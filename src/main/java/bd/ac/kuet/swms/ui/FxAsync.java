package bd.ac.kuet.swms.ui;

import bd.ac.kuet.swms.util.AppExecutors;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class FxAsync {
    private FxAsync() { }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        run(work, onSuccess, Dialogs::error);
    }

    // run on worker, callbacks come back on the fx thread (Task does that)
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        AppExecutors.workers().submit(task);
    }
}
