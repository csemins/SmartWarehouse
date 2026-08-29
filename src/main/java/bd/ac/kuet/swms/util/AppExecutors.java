package bd.ac.kuet.swms.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppExecutors {
    // db + http work goes here, never on the fx thread
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(4, named("worker"));
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2, named("scheduler"));

    private AppExecutors() { }

    public static ExecutorService workers() { return WORKERS; }

    public static ScheduledExecutorService scheduler() { return SCHEDULER; }

    private static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger();
        // daemon threads so the app can actually exit
        return r -> {
            Thread t = new Thread(r, prefix + "-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    public static void shutdown() {
        WORKERS.shutdown();
        SCHEDULER.shutdown();
        try {
            WORKERS.awaitTermination(2, TimeUnit.SECONDS);
            SCHEDULER.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
