package java.util.concurrent;

public class ExecutorCompletionService<V> {
    private final Executor executor;
    private final BlockingQueue<Future<V>> completionQueue;

    public ExecutorCompletionService(Executor executor) {
        this(executor, new LinkedBlockingQueue<Future<V>>());
    }

    public ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue) {
        if (executor == null || completionQueue == null) throw new NullPointerException();
        this.executor = executor;
        this.completionQueue = completionQueue;
    }

    public Future<V> submit(Callable<V> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = new QueueingFuture(task);
        executor.execute(f);
        return f;
    }

    public Future<V> submit(Runnable task, V result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = new QueueingFuture(task, result);
        executor.execute(f);
        return f;
    }

    public Future<V> take() throws InterruptedException {
        return completionQueue.take();
    }

    public Future<V> poll() {
        return completionQueue.poll();
    }

    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return completionQueue.poll(timeout, unit);
    }

    private class QueueingFuture extends FutureTask<V> {
        QueueingFuture(Callable<V> c) { super(c); }
        QueueingFuture(Runnable t, V r) { super(t, r); }
        protected void done() { completionQueue.add(this); }
    }
}
