package java.util.concurrent;

import java.util.Collection;
import java.util.List;

public interface ExecutorService extends Executor {
    void shutdown();
    List<Runnable> shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
    <T> Future<T> submit(Callable<T> task);
    <T> Future<T> submit(Runnable task, T result);
    Future<?> submit(Runnable task);
    // invokeAll and invokeAny are complex to implement correctly in a minimal way without full infrastructure
    // but the test uses submit. I will skip invokeAll/Any for now or add stubs if needed.
    // The previous copied interface had them. If I omit them, I break source compatibility with full JDK,
    // but the requirement is "subset".
    // I'll add them if tests fail or if I can implement them simply.
}
