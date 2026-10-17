/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package java.util.concurrent;

import com.codename1.impl.JdkHost;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/// Factories for the executor services.
///
/// #### Where a task runs
///
/// On the pool's own threads, never on the UI thread. A task that touches
/// the user interface hands that part back, the way desktop code does with
/// `SwingUtilities.invokeLater` or `Platform.runLater`.
///
/// Workers are started when a task first needs one and share one queue: a
/// task goes to whichever worker is free. A fixed pool keeps its workers
/// until it is shut down; a cached pool starts one whenever none is free and
/// lets one go after a minute without work, as the JDK's do. The overloads
/// that take a [ThreadFactory] make their workers with it.
///
/// #### Waiting
///
/// `Future.get()` and `awaitTermination` on the UI thread wait through
/// `JdkHost.blockUi`, so events keep being dispatched.
///
/// The work stealing pools are fixed pools.
public class Executors {

    /// How long a cached pool's idle worker waits for work before it ends.
    private static final long CACHED_KEEP_ALIVE_MILLIS = 60000L;

    /// How many workers a work stealing pool without a size has.
    private static final int WORK_STEALING_WORKERS = 4;

    private Executors() {
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return newFixedThreadPool(nThreads, defaultThreadFactory());
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException();
        }
        return new Pool(nThreads, 0, requireFactory(threadFactory));
    }

    public static ExecutorService newSingleThreadExecutor() {
        return newSingleThreadExecutor(defaultThreadFactory());
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new Pool(1, 0, requireFactory(threadFactory));
    }

    public static ExecutorService newCachedThreadPool() {
        return newCachedThreadPool(defaultThreadFactory());
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return new Pool(Integer.MAX_VALUE, CACHED_KEEP_ALIVE_MILLIS, requireFactory(threadFactory));
    }

    /// A pool for work that splits itself up: here, a fixed pool.
    public static ExecutorService newWorkStealingPool() {
        return newFixedThreadPool(WORK_STEALING_WORKERS);
    }

    public static ExecutorService newWorkStealingPool(int parallelism) {
        return newFixedThreadPool(parallelism);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return newScheduledThreadPool(corePoolSize, defaultThreadFactory());
    }

    /// Zero is accepted, as the JDK accepts it: a zero sized scheduled pool
    /// still starts a worker for the first task it is given.
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }
        return new ScheduledPool(new Pool(Math.max(1, corePoolSize), 0, requireFactory(threadFactory)));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return newScheduledThreadPool(1);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return newScheduledThreadPool(1, threadFactory);
    }

    /// The factory the executors use by default: non-daemon threads named
    /// `pool-N-thread-M`.
    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory(false);
    }

    /// A `Callable` that runs `task` and answers `result`.
    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        return new RunnableAdapter<T>(task, result);
    }

    public static Callable<Object> callable(Runnable task) {
        return callable(task, null);
    }

    private static ThreadFactory requireFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        return threadFactory;
    }

    static final class RunnableAdapter<T> implements Callable<T> {
        private final Runnable task;
        private final T result;

        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }

        public T call() {
            task.run();
            return result;
        }
    }

    static final class DefaultThreadFactory implements ThreadFactory {
        private static final Object COUNT_LOCK = new Object();
        private static int pools;

        private final String prefix;
        private final boolean daemon;
        private int threads;

        DefaultThreadFactory(boolean daemon) {
            int n;
            synchronized (COUNT_LOCK) {
                n = ++pools;
            }
            this.prefix = "pool-" + n + "-thread-";
            this.daemon = daemon;
        }

        public Thread newThread(Runnable r) {
            int n;
            synchronized (this) {
                n = ++threads;
            }
            Thread t = new Thread(r, prefix + n);
            if (daemon) {
                t.setDaemon(true);
            }
            return t;
        }
    }

    /// The thread pool every factory above hands out: workers started on
    /// demand, up to `maxWorkers`, sharing one queue. Everything is guarded
    /// by the queue's monitor; the backend submits from many threads at once.
    static final class Pool extends AbstractExecutorService {
        private final LinkedList<Runnable> queue = new LinkedList<Runnable>();
        private final List<Thread> workers = new ArrayList<Thread>();
        private final int maxWorkers;
        /// How long an idle worker waits for work before ending; 0 is forever.
        private final long keepAliveMillis;
        private final ThreadFactory factory;
        /// Workers waiting for work.
        private int idle;
        private boolean shutdown;
        /// Set by shutdownNow: workers stop without draining the queue.
        private boolean stop;

        Pool(int maxWorkers, long keepAliveMillis, ThreadFactory factory) {
            this.maxWorkers = maxWorkers;
            this.keepAliveMillis = keepAliveMillis;
            this.factory = factory;
        }

        public void execute(Runnable command) {
            if (command == null) {
                throw new NullPointerException();
            }
            Thread started = null;
            synchronized (queue) {
                if (shutdown) {
                    throw new RejectedExecutionException("The executor has been shut down");
                }
                queue.add(command);
                if (queue.size() > idle && workers.size() < maxWorkers) {
                    Thread t;
                    try {
                        t = factory.newThread(new Worker());
                    } catch (RuntimeException e) {
                        // The submission failed, so it must not stay queued for a
                        // later worker to run.
                        queue.removeLast();
                        throw e;
                    } catch (Error e) {
                        queue.removeLast();
                        throw e;
                    }
                    if (t == null) {
                        if (workers.isEmpty()) {
                            queue.removeLast();
                            throw new RejectedExecutionException("The thread factory made no thread");
                        }
                    } else {
                        workers.add(t);
                        started = t;
                    }
                } else {
                    queue.notify();
                }
            }
            if (started != null) {
                started.start();
            }
        }

        public void shutdown() {
            synchronized (queue) {
                shutdown = true;
                queue.notifyAll();
            }
        }

        public List<Runnable> shutdownNow() {
            List<Runnable> pending;
            List<Thread> running;
            synchronized (queue) {
                shutdown = true;
                stop = true;
                pending = new ArrayList<Runnable>(queue);
                queue.clear();
                running = new ArrayList<Thread>(workers);
                queue.notifyAll();
            }
            for (int i = 0; i < running.size(); i++) {
                running.get(i).interrupt();
            }
            return pending;
        }

        public boolean isShutdown() {
            synchronized (queue) {
                return shutdown;
            }
        }

        public boolean isTerminated() {
            synchronized (queue) {
                return terminated();
            }
        }

        /// Called holding the queue's monitor.
        boolean terminated() {
            return shutdown && workers.isEmpty() && queue.isEmpty();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            if (unit == null) {
                throw new NullPointerException();
            }
            if (!isTerminated()) {
                new Termination(this, BlockingWait.deadline(BlockingWait.millis(timeout, unit))).await();
            }
            return isTerminated();
        }

        /// The next task for the calling worker, or null when it is to end;
        /// a worker that ends is taken off the list here, under the lock.
        Runnable next(Thread self) {
            synchronized (queue) {
                while (true) {
                    if (stop || (queue.isEmpty() && shutdown)) {
                        retire(self);
                        return null;
                    }
                    if (!queue.isEmpty()) {
                        return queue.removeFirst();
                    }
                    idle++;
                    boolean timedOut = false;
                    try {
                        if (keepAliveMillis > 0) {
                            long before = BlockingWait.now();
                            queue.wait(keepAliveMillis);
                            timedOut = BlockingWait.now() - before >= keepAliveMillis;
                        } else {
                            queue.wait();
                        }
                    } catch (InterruptedException e) {
                        // shutdownNow interrupts; the flags above say what to do.
                        timedOut = false;
                    } finally {
                        idle--;
                    }
                    if (timedOut && queue.isEmpty()) {
                        retire(self);
                        return null;
                    }
                }
            }
        }

        private void retire(Thread self) {
            workers.remove(self);
            if (terminated()) {
                queue.notifyAll();
            }
        }

        /// Whether shutdownNow has been called; read by a worker between tasks.
        boolean stopping() {
            synchronized (queue) {
                return stop;
            }
        }

        final class Worker implements Runnable {
            public void run() {
                Thread self = Thread.currentThread();
                while (true) {
                    Runnable task = next(self);
                    if (task == null) {
                        return;
                    }
                    // An interrupt meant for the last task, or for an idle
                    // wait, must not reach this one -- unless the pool is
                    // stopping, which is what the interrupt then says.
                    if (!stopping()) {
                        Thread.interrupted();
                    }
                    try {
                        task.run();
                    } catch (Throwable t) {
                        JdkHost.get().log(JdkHost.LOG_ERROR,
                                "Exception in thread \"" + self.getName() + "\" " + t, t);
                    }
                }
            }
        }

        private static final class Termination extends BlockingWait {
            private final Pool pool;

            Termination(Pool pool, long deadline) {
                super(pool.queue, deadline);
                this.pool = pool;
            }

            boolean ready() {
                return pool.terminated();
            }
        }
    }
}
