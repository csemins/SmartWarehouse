package bd.ac.kuet.swms.plc;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FactoryIoController {
    // like a plc cycle: read inputs -> logic -> write outputs
    public static final long SCAN_MS = 100;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ScheduledExecutorService scheduler;
    private final JobListener listener;
    private final Queue<Runnable> commands = new ConcurrentLinkedQueue<>();
    private final Deque<WarehouseJob> queue = new ConcurrentLinkedDeque<>();
    private final List<Consumer<Snapshot>> subscribers = new CopyOnWriteArrayList<>();
    private final Deque<String> log = new ConcurrentLinkedDeque<>();

    private volatile PlcDriver driver;
    private volatile CraneConfig config = CraneConfig.defaults();
    private volatile Snapshot last;
    private ScheduledFuture<?> task;

    private ControllerState state = ControllerState.DISCONNECTED;
    private volatile boolean autoMode;
    private final Outputs out = new Outputs();
    private boolean[] written;
    private int writtenTarget = -1;
    private WarehouseJob current;
    private boolean stepStarted;
    private long stepStartedAt;
    private boolean prevStart, prevReset;
    private long lastConnectTry;
    private long scans;
    private IoImage in;
    private String message = "";

    public FactoryIoController(ScheduledExecutorService scheduler, JobListener listener) {
        this.scheduler = scheduler;
        this.listener = listener;
    }

    public synchronized void connect(PlcDriver newDriver, CraneConfig cfg) {
        disconnect();
        driver = newDriver;
        config = cfg;
        lastConnectTry = 0;
        written = null;
        writtenTarget = -1;
        log("Connecting to Factory IO at " + newDriver.describe());
        task = scheduler.scheduleWithFixedDelay(this::safeScan, 0, SCAN_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void disconnect() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        PlcDriver d = driver;
        if (d != null) {
            try {
                if (d.isConnected()) d.writeCoils(0, new boolean[Actuator.COUNT]);
            } catch (Exception ignored) {
            }
            d.disconnect();
            log("Disconnected");
        }
        driver = null;
        autoMode = false;
        state = ControllerState.DISCONNECTED;
        publish();
    }

    public void setConfig(CraneConfig cfg) { commands.add(() -> config = cfg); }

    public CraneConfig config() { return config; }

    public void enqueue(WarehouseJob job) {
        queue.add(job);
        log("Queued: " + job);
    }

    public boolean cancel(WarehouseJob job) {
        if (queue.remove(job)) {
            job.setStatus(WarehouseJob.Status.CANCELLED);
            listener.jobAborted(job);
            log("Cancelled: " + job);
            return true;
        }
        return false;
    }

    public void start() { commands.add(this::doStart); }

    public void startWhenConnected() { autoMode = true; }

    public void stop() { commands.add(this::doStop); }

    public void reset() { commands.add(this::doReset); }

    public void setCoilManually(Actuator a, boolean on) {
        commands.add(() -> {
            if (current == null && state == ControllerState.IDLE) out.set(a, on);
            else log("Manual control is only possible while the crane is idle");
        });
    }

    public void setTargetManually(int position) {
        commands.add(() -> {
            if (current == null && state == ControllerState.IDLE) out.setTargetPosition(position);
            else log("Manual control is only possible while the crane is idle");
        });
    }

    public void subscribe(Consumer<Snapshot> s) { subscribers.add(s); }

    public void unsubscribe(Consumer<Snapshot> s) { subscribers.remove(s); }

    public Snapshot snapshot() { return last; }

    public List<String> log() { return new ArrayList<>(log); }

    private void safeScan() {
        try {
            scan();
        } catch (RuntimeException e) {
            log("Scan error: " + e);
        }
    }

    void scan() {
        scans++;
        PlcDriver d = driver;
        if (d == null) return;

        if (!d.isConnected()) {
            if (state != ControllerState.DISCONNECTED) log("Connection to Factory IO lost");
            state = ControllerState.DISCONNECTED;
            if (lastConnectTry == 0) {
                lastConnectTry = System.currentTimeMillis();
                try {
                    d.connect();
                    written = null;
                    writtenTarget = -1;
                    state = ControllerState.IDLE;
                    stepStarted = false;
                    log("Connected to Factory IO " + d.describe());
                } catch (Exception e) {
                    message = "Cannot reach Factory IO: " + e.getMessage();
                }
            }
            publish();
            return;
        }

        try {
            in = new IoImage(d.readDiscreteInputs(0, Sensor.COUNT), config.conveyorSensorsInverted());
            Runnable cmd;
            while ((cmd = commands.poll()) != null) cmd.run();
            panel();
            if (state == ControllerState.RUNNING) runJobs();
            lamps();
            write(d);
        } catch (Exception e) {
            log("Communication error: " + e.getMessage());
            d.disconnect();
            state = ControllerState.DISCONNECTED;
        }
        publish();
    }

    private void panel() {
        // e-stop and stop are NC buttons, true = ok
        boolean emergencyOk = in.get(Sensor.EMERGENCY_STOP);
        boolean stopOk = in.get(Sensor.STOP);
        boolean start = in.get(Sensor.START);
        boolean reset = in.get(Sensor.RESET);

        if (!stopOk && state == ControllerState.RUNNING) doStop();
        if (start && !prevStart) doStart();
        if (reset && !prevReset) doReset();
        prevStart = start;
        prevReset = reset;
    }

    private void doStart() {
        if (state == ControllerState.IDLE || state == ControllerState.PAUSED) {
            state = ControllerState.RUNNING;
            autoMode = true;
            stepStarted = false;
            log(current != null ? "Resumed" : "Started");
        } else if (state == ControllerState.FAULT) {
            log("Press Reset first");
        }
    }

    private void doStop() {
        if (state == ControllerState.RUNNING) {
            autoMode = false;
            state = current != null ? ControllerState.PAUSED : ControllerState.IDLE;
            out.conveyorsOff();
            out.setTargetPosition(0);
            log("Stopped");
        }
    }

    private void doReset() {
        if (!in.get(Sensor.EMERGENCY_STOP)) {
            log("Release the emergency stop before reset");
            return;
        }
        if (current != null) {
            current.setStatus(WarehouseJob.Status.FAILED);
            current.setMessage("Aborted by reset");
            listener.jobAborted(current);
            log("Job aborted: " + current);
            current = null;
        }
        out.stopMotion();
        out.setTargetPosition(config.restPosition());
        autoMode = false;
        state = ControllerState.IDLE;
        message = "";
        log("Reset - crane returns to rest position");
    }

    private void runJobs() {
        if (current == null) {
            current = queue.poll();
            if (current == null) {
                return;
            }
            current.setStatus(WarehouseJob.Status.RUNNING);
            stepStarted = false;
            log("Started job: " + current);
        }
        Step step = current.currentStep();
        long now = System.currentTimeMillis();
        if (!stepStarted) {
            step.start(out);
            stepStarted = true;
            stepStartedAt = now;
        }
        long elapsed = now - stepStartedAt;
        if (step.isDone(in, elapsed)) {
            step.finish(out);
            current.advance();
            stepStarted = false;
            if (!current.hasMoreSteps()) {
                current.setStatus(WarehouseJob.Status.DONE);
                log("Finished: " + current);
                listener.jobFinished(current);
                current = null;
            }
        } else if (elapsed > config.stepTimeoutSeconds() * 1000L) {
            state = ControllerState.FAULT;
            autoMode = false;
            out.stopMotion();
            message = "Timeout in step '" + step + "' - check Factory IO, then press Reset";
            current.setMessage(message);
            log(message);
        }
    }

    private void lamps() {
        out.set(Actuator.START_LIGHT, state == ControllerState.RUNNING);
        out.set(Actuator.STOP_LIGHT, state == ControllerState.IDLE || state == ControllerState.PAUSED);
        out.set(Actuator.RESET_LIGHT, state == ControllerState.FAULT && (scans / 5) % 2 == 0);
    }

    // only write when something changed, keeps the traffic low
    private void write(PlcDriver d) throws Exception {
        boolean[] coils = out.coils();
        if (!Arrays.equals(coils, written)) {
            d.writeCoils(0, coils);
            written = coils;
        }
        if (out.getTargetPosition() != writtenTarget) {
            d.writeRegister(Actuator.TARGET_POSITION_REGISTER, out.getTargetPosition());
            writtenTarget = out.getTargetPosition();
        }
    }

    private void publish() {
        PlcDriver d = driver;
        Snapshot s = new Snapshot(state, d == null ? "-" : d.describe(), in == null ? null : in.inputs(),
                out.coils(), out.getTargetPosition(), current, List.copyOf(queue), message, scans);
        last = s;
        for (Consumer<Snapshot> c : subscribers) {
            try {
                c.accept(s);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void log(String text) {
        message = text;
        log.addFirst(TIME.format(LocalTime.now()) + "  " + text);
        while (log.size() > 100) log.removeLast();
    }
}
