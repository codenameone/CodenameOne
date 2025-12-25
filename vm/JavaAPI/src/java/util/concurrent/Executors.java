package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

public class Executors {

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new SimpleThreadPool(nThreads);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return new SimpleThreadPool(1);
    }

    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        return new RunnableAdapter<T>(task, result);
    }

    public static Callable<Object> callable(Runnable task) {
        if (task == null) throw new NullPointerException();
        return new RunnableAdapter<Object>(task, null);
    }

    static final class RunnableAdapter<T> implements Callable<T> {
        final Runnable task;
        final T result;
        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }
        public T call() {
            task.run();
            return result;
        }
    }

    static class SimpleThreadPool extends AbstractExecutorService {
        private final List<Runnable> workQueue = new ArrayList<Runnable>();
        private final List<Worker> workers = new ArrayList<Worker>();
        private boolean isShutdown;

        SimpleThreadPool(int nThreads) {
            for (int i = 0; i < nThreads; i++) {
                Worker w = new Worker();
                workers.add(w);
                w.start();
            }
        }

        public void execute(Runnable command) {
            synchronized(workQueue) {
                if (isShutdown) throw new RejectedExecutionException();
                workQueue.add(command);
                workQueue.notify();
            }
        }

        public void shutdown() {
            synchronized(workQueue) {
                isShutdown = true;
                workQueue.notifyAll();
            }
        }

        public List<Runnable> shutdownNow() {
            List<Runnable> pending;
            synchronized(workQueue) {
                isShutdown = true;
                pending = new ArrayList<Runnable>(workQueue);
                workQueue.clear();
                workQueue.notifyAll();
            }
            return pending;
        }

        public boolean isShutdown() {
            synchronized(workQueue) {
                return isShutdown;
            }
        }

        public boolean isTerminated() {
            synchronized(workQueue) {
                if (!isShutdown || !workQueue.isEmpty()) return false;
                for (Worker w : workers) {
                    if (w.isAlive()) return false;
                }
                return true;
            }
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long millis = unit.toMillis(timeout);
            long end = System.currentTimeMillis() + millis;
            while (!isTerminated()) {
                long delay = end - System.currentTimeMillis();
                if (delay <= 0) return false;
                Thread.sleep(Math.min(delay, 100));
            }
            return true;
        }

        class Worker extends Thread {
            public void run() {
                while (true) {
                    Runnable task = null;
                    synchronized(workQueue) {
                        while (workQueue.isEmpty() && !isShutdown) {
                            try {
                                workQueue.wait();
                            } catch (InterruptedException e) {}
                        }
                        if (workQueue.isEmpty() && isShutdown) {
                            return;
                        }
                        task = workQueue.remove(0);
                    }
                    if (task != null) {
                        try {
                            task.run();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
