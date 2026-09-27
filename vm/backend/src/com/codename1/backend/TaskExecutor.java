/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import java.util.LinkedList;

/**
 * A named place background work runs: a pool of platform threads, or virtual
 * threads on the server's hosts.
 *
 * <p>Configured as {@code cn1.task.executor.<name>.threads} and
 * {@code cn1.task.executor.<name>.kind} ({@code platform} or {@code virtual}),
 * and obtained through {@link Tasks#executor}. The threads of a pool start on the
 * first task, so an executor the configuration names and nothing uses costs a
 * map entry.
 *
 * <p>A virtual executor hands each task to a virtual thread of its own; on a
 * runtime or a server that has none -- the Java SE arm, a TLS server, Windows --
 * it runs the task on its pool instead, which is why it has a size too.
 */
public final class TaskExecutor {
    private final String name;
    private final boolean virtual;
    private final int size;
    private final LinkedList queue = new LinkedList();
    private Thread[] workers;
    private int active;
    private long completed;
    private long failed;
    private boolean shutdown;
    /** Tasks dropped unstarted because the shutdown deadline passed. */
    private long dropped;
    /** Whether shutdown ran out of time; nothing may start after that. */
    private boolean timedOut;
    /** The server these executors belong to, which the task threads carry. */
    private final Tasks.Registry registry;

    TaskExecutor(String name, boolean virtual, int size, Tasks.Registry registry) {
        this.name = name;
        this.virtual = virtual;
        this.size = size < 1 ? 1 : size;
        this.registry = registry;
    }

    public String getName() {
        return name;
    }

    /** Whether this executor asks for virtual threads. */
    public boolean isVirtual() {
        return virtual;
    }

    public int getSize() {
        return size;
    }

    /** Tasks waiting for a thread. */
    public synchronized int getQueueDepth() {
        return queue.size();
    }

    /** Tasks running now. */
    public synchronized int getActiveCount() {
        return active;
    }

    public synchronized long getCompletedCount() {
        return completed;
    }

    /** Tasks that ended with an exception nobody received. */
    public synchronized long getFailedCount() {
        return failed;
    }

    /**
     * Runs {@code task} on this executor.
     *
     * @throws IllegalStateException once the server has stopped it
     */
    public void execute(Runnable task) {
        if(task == null) {
            return;
        }
        if(virtual) {
            // Refused and counted BEFORE the hand-off, exactly like a pool task:
            // a submission after shutdown must fail rather than run on a server
            // that is stopping, and a task already handed to a host must hold
            // shutdown()'s drain open from the moment it is accepted -- counting
            // it only once it starts let shutdown() return while it still sat in
            // a host's inbox.
            synchronized(this) {
                if(shutdown) {
                    throw new IllegalStateException("Executor " + name + " has been shut down");
                }
                active++;
            }
            HttpServer host = registry == null ? HttpServer.activeServer()
                    : registry.virtualHost();
            if(HttpServer.submitVirtualTask(new Counted(this, task), host)) {
                return;
            }
            synchronized(this) {
                active--;
            }
        }
        synchronized(this) {
            if(shutdown) {
                throw new IllegalStateException("Executor " + name + " has been shut down");
            }
            queue.addLast(task);
            if(workers == null) {
                startWorkers();
            }
            notify();
        }
    }

    private void startWorkers() {
        workers = new Thread[size];
        for(int iter = 0 ; iter < size ; iter++) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    work();
                }
            }, "cn1-task-" + name + "-" + iter);
            t.setDaemon(true);
            workers[iter] = t;
            t.start();
        }
    }

    private void work() {
        while(true) {
            Runnable task;
            synchronized(this) {
                while(queue.isEmpty() && !shutdown) {
                    try {
                        wait();
                    } catch (InterruptedException err) {
                        return;
                    }
                }
                if(queue.isEmpty()) {
                    notifyAll();
                    return;
                }
                task = (Runnable)queue.removeFirst();
                active++;
            }
            runCounted(task);
        }
    }

    void runCounted(Runnable task) {
        boolean ok = false;
        // The task works for this executor's server: an @Async call it makes, or
        // an executor it asks for, is that server's too.
        Object previous = Tasks.enter(registry);
        try {
            task.run();
            ok = true;
        } catch (Throwable err) {
            System.err.println("Task " + task + " on executor " + name + " failed: " + err);
        } finally {
            Tasks.leave(previous);
            synchronized(this) {
                active--;
                completed++;
                if(!ok) {
                    failed++;
                }
                if(shutdown && active == 0 && queue.isEmpty()) {
                    notifyAll();
                }
            }
        }
    }

    synchronized void recordFailure() {
        failed++;
    }

    /** Whether {@link #shutdown(long)} has been called; it then refuses tasks. */
    public synchronized boolean isShutdown() {
        return shutdown;
    }

    /**
     * Stops taking tasks and waits up to {@code waitMillis} for the queued and
     * running ones to finish.
     */
    public synchronized void shutdown(long waitMillis) {
        shutdown = true;
        notifyAll();
        long deadline = System.currentTimeMillis() + Math.max(0, waitMillis);
        while((active > 0 || !queue.isEmpty()) && waitMillis > 0) {
            long left = deadline - System.currentTimeMillis();
            if(left <= 0) {
                break;
            }
            try {
                wait(left);
            } catch (InterruptedException err) {
                break;
            }
        }
        if(queue.isEmpty() && active == 0) {
            return;
        }
        // Out of time. The server destroys its beans and closes its database
        // next, so a task still WAITING must never start: it would run against
        // both. Those are dropped and counted. A task already running cannot be
        // stopped from outside -- Java has no safe way to -- so its thread is
        // interrupted, which ends any wait, sleep or interruptible I/O it is in,
        // and it is reported by name so the overrun is visible.
        timedOut = true;
        int waiting = queue.size();
        dropped += waiting;
        // A dropped @Async call still has a Future someone may be waiting on;
        // it fails rather than never finishing.
        for(int iter = 0 ; iter < queue.size() ; iter++) {
            Object queued = queue.get(iter);
            if(queued instanceof AsyncTask) {
                ((AsyncTask)queued).abandon("dropped when the server stopped before it "
                        + "could start");
            }
        }
        queue.clear();
        if(workers != null) {
            for(int iter = 0 ; iter < workers.length ; iter++) {
                workers[iter].interrupt();
            }
        }
        System.err.println("Executor " + name + " did not finish within the shutdown "
                + "timeout: " + waiting + " queued task(s) dropped, " + active
                + " still running");
    }

    /** Tasks the shutdown deadline dropped before they could start. */
    public synchronized long getDroppedCount() {
        return dropped;
    }

    /**
     * Runs a task a host accepted for a virtual thread and then could not give
     * one -- no stack, or the server stopping -- on this executor's own platform
     * workers instead. It was accepted before any shutdown and is already counted
     * active, so it is queued even now, and shutdown() waits for it.
     */
    static void fallBack(Runnable task) {
        if(task instanceof Counted) {
            Counted c = (Counted)task;
            c.owner.requeue(c.task);
            return;
        }
        Tasks.platform(task);
    }

    /**
     * A task whose virtual thread was freed at shutdown before it finished. Its
     * own finally never runs, so what it would have done happens here: the
     * executor stops counting it active -- or shutdown() waits for it until the
     * deadline -- and its Future, if it has one, fails instead of never ending.
     */
    static void abandoned(Runnable task) {
        Runnable inner = task;
        if(task instanceof Counted) {
            Counted c = (Counted)task;
            inner = c.task;
            synchronized(c.owner) {
                c.owner.active--;
                c.owner.completed++;
                c.owner.failed++;
                c.owner.notifyAll();
            }
        }
        if(inner instanceof AsyncTask) {
            ((AsyncTask)inner).abandon("abandoned when the server stopped before it finished");
        }
    }

    private synchronized void requeue(Runnable task) {
        active--;
        if(timedOut) {
            dropped++;
            if(task instanceof AsyncTask) {
                ((AsyncTask)task).abandon("dropped when the server stopped before it could "
                        + "start");
            }
            notifyAll();
            return;
        }
        queue.addLast(task);
        if(workers == null) {
            startWorkers();
        } else if(shutdown) {
            // The pool's threads may already have drained and ended; one more
            // runs this and ends in turn, and shutdown() is still waiting on it.
            Thread t = new Thread(new Runnable() {
                public void run() {
                    work();
                }
            }, "cn1-task-" + name + "-late");
            t.setDaemon(true);
            t.start();
        }
        notify();
    }

    public String toString() {
        return name + (virtual ? " (virtual)" : " (" + size + " threads)");
    }

    /**
     * A task on a virtual thread, counted like one on the pool. execute() has
     * already counted it active; runCounted's finally is the matching decrement.
     */
    private static final class Counted implements Runnable {
        private final TaskExecutor owner;
        private final Runnable task;

        Counted(TaskExecutor owner, Runnable task) {
            this.owner = owner;
            this.task = task;
        }

        public void run() {
            owner.runCounted(task);
        }
    }
}
