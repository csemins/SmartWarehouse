package bd.ac.kuet.swms.plc;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WarehouseJob {
    public enum Kind { STORE, RETRIEVE }

    public enum Status { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

    private static final AtomicInteger IDS = new AtomicInteger();

    private final int id = IDS.incrementAndGet();
    private final Kind kind;
    private final int cell;
    private final String label;
    private final List<Step> steps;
    private volatile Status status = Status.QUEUED;
    private volatile int stepIndex;
    private volatile String message = "";
    private String username;
    private Integer orderId;

    private WarehouseJob(Kind kind, int cell, String label, List<Step> steps) {
        this.kind = kind;
        this.cell = cell;
        this.label = label;
        this.steps = steps;
    }

    // order measured on the real scene: load = LEFT, rack + unload = RIGHT, 55 = rest
    public static WarehouseJob store(CraneConfig cfg, int cell, String label) {
        int rest = cfg.restPosition();
        return new WarehouseJob(Kind.STORE, cell, label, List.of(
                new Step.Lift(false),
                new Step.Forks(Side.MIDDLE),
                new Step.MoveCrane(rest),
                new Step.Conveyor("Bring box to load position", Sensor.AT_LOAD, true, 0,
                        Actuator.ENTRY_CONVEYOR, Actuator.LOAD_CONVEYOR),
                new Step.Forks(cfg.loadSide()),
                new Step.Lift(true),
                new Step.Forks(Side.MIDDLE),
                new Step.Lift(false),
                new Step.MoveCrane(cell),
                new Step.Forks(cfg.rackSide()),
                new Step.Lift(false),
                new Step.Forks(Side.MIDDLE),
                new Step.MoveCrane(rest)));
    }

    public static WarehouseJob retrieve(CraneConfig cfg, int cell, String label) {
        int rest = cfg.restPosition();
        return new WarehouseJob(Kind.RETRIEVE, cell, label, List.of(
                new Step.Lift(false),
                new Step.Forks(Side.MIDDLE),
                new Step.MoveCrane(cell),
                new Step.Forks(cfg.rackSide()),
                new Step.Lift(true),
                new Step.Forks(Side.MIDDLE),
                new Step.Lift(false),
                new Step.MoveCrane(rest),
                new Step.Lift(true),
                new Step.Forks(cfg.unloadSide()),
                new Step.Lift(false),
                new Step.Forks(Side.MIDDLE),
                new Step.ConveyorThrough("Send box out through the exit", Sensor.AT_EXIT, 15_000,
                        Actuator.UNLOAD_CONVEYOR, Actuator.EXIT_CONVEYOR)));
    }

    public Step currentStep() { return stepIndex < steps.size() ? steps.get(stepIndex) : null; }

    void advance() { stepIndex++; }

    boolean hasMoreSteps() { return stepIndex < steps.size(); }

    public int getId() { return id; }
    public Kind getKind() { return kind; }
    public int getCell() { return cell; }
    public String getLabel() { return label; }
    public Status getStatus() { return status; }
    void setStatus(Status status) { this.status = status; }
    public String getMessage() { return message; }
    void setMessage(String message) { this.message = message; }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getStepIndex() { return stepIndex; }
    public int getStepCount() { return steps.size(); }

    public String progress() {
        Step s = currentStep();
        return status == Status.RUNNING && s != null ? "Step " + (stepIndex + 1) + "/" + steps.size() + ": " + s : status.name();
    }

    @Override public String toString() { return kind + " cell " + cell + " - " + label; }
}
