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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.EventQueue;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeSupport;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// Runs a long task off the event dispatch thread and reports back on it.
///
/// `execute()` starts a thread that runs `doInBackground()`. What that
/// publishes reaches `process` on the event dispatch thread, with every
/// chunk published before that thread got to it in one list; `done()` and
/// the `state` and `progress` property changes are delivered there too.
/// `get()` called on the event dispatch thread before the task ended keeps
/// that thread dispatching while it waits; on any other thread it waits by
/// polling.
///
/// Not supported: the class is a `Future` and a `Runnable` but not a
/// `RunnableFuture`; each worker has a thread of its own instead of the ten
/// thread pool of the desktop; every `progress` change is delivered, where
/// the desktop may merge several into one event.
public abstract class SwingWorker<T, V> implements Future<T>, Runnable {

    private static final int RUNNING = 0;
    private static final int VALUE = 1;
    private static final int FAILED = 2;
    private static final int CANCELLED = 3;
    private static final int POLL_MILLIS = 5;

    private final AtomicInteger outcome = new AtomicInteger(RUNNING);
    private final AtomicReference<StateValue> state = new AtomicReference<StateValue>(StateValue.PENDING);
    private final AtomicInteger progress = new AtomicInteger();
    private final AtomicBoolean executed = new AtomicBoolean();
    private final AtomicBoolean ran = new AtomicBoolean();
    private final AtomicReference<T> result = new AtomicReference<T>();
    private final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
    private final AtomicReference<Thread> runner = new AtomicReference<Thread>();
    private final AtomicReference<ArrayList<V>> pending = new AtomicReference<ArrayList<V>>();
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    /// The stages of a worker's life, the values of its `state` property.
    public enum StateValue {
        PENDING,
        STARTED,
        DONE
    }

    public SwingWorker() {
    }

    protected abstract T doInBackground() throws Exception;

    /// Runs the task on the calling thread, unless it ran or was cancelled
    /// already. `execute()` calls this on the worker's thread.
    @Override
    public final void run() {
        if (!ran.compareAndSet(false, true) || outcome.get() != RUNNING) {
            return;
        }
        runner.set(Thread.currentThread());
        if (state.compareAndSet(StateValue.PENDING, StateValue.STARTED)) {
            firePropertyChange("state", StateValue.PENDING, StateValue.STARTED);
        }
        try {
            result.set(doInBackground());
            complete(VALUE);
        } catch (Throwable t) {
            failure.set(t);
            complete(FAILED);
        } finally {
            runner.set(null);
        }
    }

    /// Hands chunks to `process`, which runs later on the event dispatch
    /// thread with everything published until then.
    @SafeVarargs
    protected final void publish(V... chunks) {
        if (chunks == null || chunks.length == 0) {
            return;
        }
        while (true) {
            ArrayList<V> old = pending.get();
            ArrayList<V> grown = old == null ? new ArrayList<V>() : new ArrayList<V>(old);
            for (int i = 0; i < chunks.length; i++) {
                grown.add(chunks[i]);
            }
            if (pending.compareAndSet(old, grown)) {
                if (old == null) {
                    later(new Flush<V>(this));
                }
                return;
            }
        }
    }

    protected void process(List<V> chunks) {
    }

    protected void done() {
    }

    protected final void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("the value should be from 0 to 100");
        }
        int old = this.progress.getAndSet(progress);
        if (old != progress) {
            firePropertyChange("progress", Integer.valueOf(old), Integer.valueOf(progress));
        }
    }

    public final int getProgress() {
        return progress.get();
    }

    /// Starts the task on a thread of its own. A second call does nothing.
    public final void execute() {
        if (executed.compareAndSet(false, true)) {
            new Thread(this, "SwingWorker").start();
        }
    }

    /// Cancels the task. A task that did not start never runs; one that is
    /// running has its thread interrupted when `mayInterruptIfRunning`, and
    /// its result is dropped either way.
    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        if (!outcome.compareAndSet(RUNNING, CANCELLED)) {
            return false;
        }
        if (mayInterruptIfRunning) {
            Thread t = runner.get();
            if (t != null) {
                t.interrupt();
            }
        }
        finished();
        return true;
    }

    @Override
    public final boolean isCancelled() {
        return outcome.get() == CANCELLED;
    }

    @Override
    public final boolean isDone() {
        return outcome.get() != RUNNING;
    }

    @Override
    public final T get() throws InterruptedException, ExecutionException {
        await(-1);
        return report();
    }

    @Override
    public final T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        long millis = unit.toMillis(timeout);
        await(millis < 0 ? 0 : millis);
        if (outcome.get() == RUNNING) {
            throw new TimeoutException();
        }
        return report();
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /// Notifies the listeners on the event dispatch thread: at once when
    /// called there, else after the events already queued.
    public final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (EventQueue.isDispatchThread()) {
            propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
        } else {
            later(new Fire(propertyChangeSupport,
                    new PropertyChangeEvent(this, propertyName, oldValue, newValue)));
        }
    }

    public final PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public final StateValue getState() {
        if (isDone()) {
            return StateValue.DONE;
        }
        return state.get();
    }

    private void complete(int how) {
        if (outcome.compareAndSet(RUNNING, how)) {
            finished();
        }
    }

    /// The task ended one way or another: the state becomes `DONE` and
    /// `done()` runs on the event dispatch thread.
    private void finished() {
        StateValue old = state.getAndSet(StateValue.DONE);
        if (old != StateValue.DONE) {
            firePropertyChange("state", old, StateValue.DONE);
        }
        Runnable r = new Done(this);
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            later(r);
        }
    }

    private static void later(Runnable r) {
        EventQueue.invokeLater(r);
    }

    private T report() throws ExecutionException {
        int how = outcome.get();
        if (how == CANCELLED) {
            throw new CancellationException();
        }
        if (how == FAILED) {
            throw new ExecutionException(failure.get());
        }
        return result.get();
    }

    /// Waits until the task ended, or for `millis` when that is not
    /// negative.
    private void await(long millis) throws InterruptedException {
        if (outcome.get() != RUNNING) {
            return;
        }
        long deadline = millis < 0 ? -1 : System.currentTimeMillis() + millis;
        if (Display.isInitialized() && Display.getInstance().isEdt()) {
            Display.getInstance().invokeAndBlock(new Waiter(outcome, deadline));
            return;
        }
        while (outcome.get() == RUNNING && (deadline < 0 || System.currentTimeMillis() < deadline)) {
            Thread.sleep(POLL_MILLIS);
        }
    }

    void cn1Flush() {
        ArrayList<V> chunks = pending.getAndSet(null);
        if (chunks != null && !chunks.isEmpty()) {
            process(chunks);
        }
    }

    void cn1Done() {
        done();
    }

    /// Polls for the end of the task off the event dispatch thread while
    /// that thread goes on dispatching.
    private static final class Waiter implements Runnable {
        private final AtomicInteger outcome;
        private final long deadline;

        Waiter(AtomicInteger outcome, long deadline) {
            this.outcome = outcome;
            this.deadline = deadline;
        }

        @Override
        public void run() {
            while (outcome.get() == RUNNING && (deadline < 0 || System.currentTimeMillis() < deadline)) {
                try {
                    Thread.sleep(POLL_MILLIS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private static final class Flush<V> implements Runnable {
        private final SwingWorker<?, V> worker;

        Flush(SwingWorker<?, V> worker) {
            this.worker = worker;
        }

        @Override
        public void run() {
            worker.cn1Flush();
        }
    }

    private static final class Done implements Runnable {
        private final SwingWorker<?, ?> worker;

        Done(SwingWorker<?, ?> worker) {
            this.worker = worker;
        }

        @Override
        public void run() {
            worker.cn1Done();
        }
    }

    private static final class Fire implements Runnable {
        private final PropertyChangeSupport support;
        private final PropertyChangeEvent event;

        Fire(PropertyChangeSupport support, PropertyChangeEvent event) {
            this.support = support;
            this.event = event;
        }

        @Override
        public void run() {
            support.firePropertyChange(event);
        }
    }
}
