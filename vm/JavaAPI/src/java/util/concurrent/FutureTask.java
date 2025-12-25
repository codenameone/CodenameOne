package java.util.concurrent;

public class FutureTask<V> implements RunnableFuture<V> {
    private Callable<V> callable;
    private V result;
    private Throwable exception;
    private boolean done;
    private boolean cancelled;

    public FutureTask(Callable<V> callable) {
        if (callable == null) throw new NullPointerException();
        this.callable = callable;
    }

    public FutureTask(Runnable runnable, V result) {
        if (runnable == null) throw new NullPointerException();
        this.callable = Executors.callable(runnable, result);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized(this) {
            if (done) return false;
            cancelled = true;
            done = true;
            notifyAll();
        }
        return true;
    }

    public boolean isCancelled() {
        synchronized(this) {
            return cancelled;
        }
    }

    public boolean isDone() {
        synchronized(this) {
            return done;
        }
    }

    public V get() throws InterruptedException, ExecutionException {
        synchronized(this) {
            while (!done) {
                wait();
            }
            if (cancelled) throw new CancellationException();
            if (exception != null) throw new ExecutionException(exception);
            return result;
        }
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) throw new NullPointerException();
        long millis = unit.toMillis(timeout);
        long end = System.currentTimeMillis() + millis;
        synchronized(this) {
            while (!done) {
                long delay = end - System.currentTimeMillis();
                if (delay <= 0) throw new TimeoutException();
                wait(delay);
            }
            if (cancelled) throw new CancellationException();
            if (exception != null) throw new ExecutionException(exception);
            return result;
        }
    }

    public void run() {
        Callable<V> c;
        synchronized(this) {
            if (done) return;
            c = callable;
        }
        try {
            V v = c.call();
            synchronized(this) {
                if (!done) {
                    result = v;
                    done = true;
                    notifyAll();
                }
            }
        } catch (Throwable t) {
            synchronized(this) {
                if (!done) {
                    exception = t;
                    done = true;
                    notifyAll();
                }
            }
        }
    }
}
