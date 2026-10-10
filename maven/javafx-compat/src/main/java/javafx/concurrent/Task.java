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
package javafx.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// One piece of work for a background thread, done once: [#call()]
/// runs there, and everything it reports reaches the JavaFX application
/// thread, where the properties of the task change and its handlers
/// run.
///
/// A task is a `Runnable`: hand it to a thread, or let a [Service]
/// start it. Run directly it goes from `READY` through `SCHEDULED` and
/// `RUNNING` to `SUCCEEDED`, `FAILED` (an exception left `call()`, see
/// [#getException()]) or `CANCELLED`. A task runs once; a second `run`
/// does nothing.
///
/// `call()` reports with [#updateProgress(double, double)],
/// [#updateMessage(String)], [#updateTitle(String)] and
/// [#updateValue(Object)]. They may be called as often as the work
/// likes: updates that arrive faster than the application thread takes
/// them are merged, and it sees the latest.
///
/// Cancelling asks the work to stop, it cannot make it:
/// `call()` should look at [#isCancelled()] now and then. The state
/// becomes `CANCELLED` at once all the same, and whatever `call()`
/// returns later is dropped.
///
/// The properties belong to the application thread; reading them from
/// another thread throws `IllegalStateException`. [#isCancelled()],
/// [#isDone()], [#cancel()] and the `update` methods are for any
/// thread.
///
/// Differences from JavaFX: there a task is a
/// `java.util.concurrent.FutureTask`. Codename One has no
/// `java.util.concurrent` beyond the atomic classes, so the task is a
/// `Runnable` with the `FutureTask` methods that need none of it
/// (`run`, `cancel(boolean)`, `isCancelled`, `isDone`), and the blocking
/// `get()` does not exist: take the result from [#getValue()] in a
/// succeeded handler.
public abstract class Task<V> implements Worker<V>, EventTarget, Runnable {

    private static final int PENDING = 0;
    private static final int COMPLETED = 1;
    private static final int CANCELLED = 2;

    /// The pair an update of the progress carries across threads.
    private static final class ProgressUpdate {
        final double workDone;
        final double totalWork;

        ProgressUpdate(double workDone, double totalWork) {
            this.workDone = workDone;
            this.totalWork = totalWork;
        }
    }

    // The hand-over between the thread that works and the application
    // thread. Nothing else of a task is touched by two threads.
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicInteger outcome = new AtomicInteger(PENDING);
    private final AtomicReference<Thread> runner = new AtomicReference<Thread>();
    private final AtomicReference<ProgressUpdate> progressUpdate = new AtomicReference<ProgressUpdate>();
    private final AtomicReference<String> messageUpdate = new AtomicReference<String>();
    private final AtomicReference<String> titleUpdate = new AtomicReference<String>();
    private final AtomicReference<V> valueUpdate = new AtomicReference<V>();

    private final WorkerEvents events = new WorkerEvents(this);
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<State>(this, "state", State.READY);
    private final ReadOnlyObjectWrapper<V> value = new ReadOnlyObjectWrapper<V>(this, "value");
    private final ReadOnlyObjectWrapper<Throwable> exception =
            new ReadOnlyObjectWrapper<Throwable>(this, "exception");
    private final ReadOnlyDoubleWrapper workDone = new ReadOnlyDoubleWrapper(this, "workDone", -1);
    private final ReadOnlyDoubleWrapper totalWork = new ReadOnlyDoubleWrapper(this, "totalWork", -1);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(this, "progress", -1);
    private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper(this, "running", false);
    private final ReadOnlyStringWrapper message = new ReadOnlyStringWrapper(this, "message", "");
    private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper(this, "title", "");

    /// Creates a task.
    public Task() {
    }

    /// Does the work, on a background thread, and returns its result.
    /// An exception it throws fails the task.
    protected abstract V call() throws Exception;

    // ---- running ----

    /// Runs the task on the calling thread, which should not be the
    /// JavaFX application thread: calls [#call()] and reports the
    /// outcome to the application thread. Only the first call does
    /// anything, and none does once the task was cancelled.
    @Override
    public void run() {
        if (!started.compareAndSet(false, true) || outcome.get() != PENDING) {
            return;
        }
        runner.set(Thread.currentThread());
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                setState(State.SCHEDULED);
                setState(State.RUNNING);
            }
        });
        try {
            final V result = call();
            if (outcome.compareAndSet(PENDING, COMPLETED)) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        value.set(result);
                        setState(State.SUCCEEDED);
                    }
                });
            }
        } catch (final Throwable failure) {
            if (outcome.compareAndSet(PENDING, COMPLETED)) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        exception.set(failure);
                        setState(State.FAILED);
                    }
                });
            }
        } finally {
            runner.set(null);
        }
    }

    /// Cancels the task, interrupting the thread that runs it.
    @Override
    public final boolean cancel() {
        return cancel(true);
    }

    /// Cancels the task. Returns `false` when it already ended or was
    /// cancelled before. `call()` is never started on a cancelled task;
    /// one that is running is interrupted if asked, and otherwise finds
    /// out through [#isCancelled()].
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!outcome.compareAndSet(PENDING, CANCELLED)) {
            return false;
        }
        if (mayInterruptIfRunning) {
            Thread thread = runner.get();
            if (thread != null) {
                thread.interrupt();
            }
        }
        if (Platform.isFxApplicationThread()) {
            setState(State.CANCELLED);
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setState(State.CANCELLED);
                }
            });
        }
        return true;
    }

    /// Returns whether the task was cancelled. For any thread.
    public boolean isCancelled() {
        return outcome.get() == CANCELLED;
    }

    /// Returns whether the task has ended: by returning, by throwing or
    /// by being cancelled. For any thread; the state property follows
    /// on the application thread a moment later.
    public boolean isDone() {
        return outcome.get() != PENDING;
    }

    // ---- state ----

    private static boolean ended(State s) {
        return s == State.SUCCEEDED || s == State.FAILED || s == State.CANCELLED;
    }

    /// Moves the task to a state, on the application thread: sets the
    /// properties, delivers the event and calls the method of the
    /// state. A task that ended stays as it is.
    final void setState(State next) {
        State current = state.get();
        if (current == next || ended(current)) {
            return;
        }
        if (isCancelled() && next != State.CANCELLED) {
            return;
        }
        state.set(next);
        running.set(next == State.SCHEDULED || next == State.RUNNING);
        events.fire(this, next);
        switch (next) {
            case SCHEDULED:
                scheduled();
                break;
            case RUNNING:
                running();
                break;
            case SUCCEEDED:
                succeeded();
                break;
            case CANCELLED:
                cancelled();
                break;
            case FAILED:
                failed();
                break;
            default:
                break;
        }
    }

    private void checkThread() {
        if (started.get() && !Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Task must only be used from the FX Application Thread");
        }
    }

    /// Called on the application thread when the task is scheduled.
    protected void scheduled() {
    }

    /// Called on the application thread when the task starts running.
    protected void running() {
    }

    /// Called on the application thread when the task has finished
    /// with a result.
    protected void succeeded() {
    }

    /// Called on the application thread when the task was cancelled.
    protected void cancelled() {
    }

    /// Called on the application thread when an exception ended the
    /// task.
    protected void failed() {
    }

    // ---- properties ----

    @Override
    public final State getState() {
        checkThread();
        return state.get();
    }

    @Override
    public final ReadOnlyObjectProperty<State> stateProperty() {
        checkThread();
        return state.getReadOnlyProperty();
    }

    @Override
    public final V getValue() {
        checkThread();
        return value.get();
    }

    @Override
    public final ReadOnlyObjectProperty<V> valueProperty() {
        checkThread();
        return value.getReadOnlyProperty();
    }

    @Override
    public final Throwable getException() {
        checkThread();
        return exception.get();
    }

    @Override
    public final ReadOnlyObjectProperty<Throwable> exceptionProperty() {
        checkThread();
        return exception.getReadOnlyProperty();
    }

    @Override
    public final double getWorkDone() {
        checkThread();
        return workDone.get();
    }

    @Override
    public final ReadOnlyDoubleProperty workDoneProperty() {
        checkThread();
        return workDone.getReadOnlyProperty();
    }

    @Override
    public final double getTotalWork() {
        checkThread();
        return totalWork.get();
    }

    @Override
    public final ReadOnlyDoubleProperty totalWorkProperty() {
        checkThread();
        return totalWork.getReadOnlyProperty();
    }

    @Override
    public final double getProgress() {
        checkThread();
        return progress.get();
    }

    @Override
    public final ReadOnlyDoubleProperty progressProperty() {
        checkThread();
        return progress.getReadOnlyProperty();
    }

    @Override
    public final boolean isRunning() {
        checkThread();
        return running.get();
    }

    @Override
    public final ReadOnlyBooleanProperty runningProperty() {
        checkThread();
        return running.getReadOnlyProperty();
    }

    @Override
    public final String getMessage() {
        checkThread();
        return message.get();
    }

    @Override
    public final ReadOnlyStringProperty messageProperty() {
        checkThread();
        return message.getReadOnlyProperty();
    }

    @Override
    public final String getTitle() {
        checkThread();
        return title.get();
    }

    @Override
    public final ReadOnlyStringProperty titleProperty() {
        checkThread();
        return title.getReadOnlyProperty();
    }

    // ---- reporting from the work ----

    /// Reports how much of the work is done; see
    /// [#updateProgress(double, double)].
    protected void updateProgress(long workDone, long max) {
        updateProgress((double) workDone, (double) max);
    }

    /// Reports how much of the work is done, from any thread. A
    /// negative or unusable number means unknown and is reported as -1;
    /// more done than there is counts as all of it. The progress
    /// property becomes the share that is done, or -1.
    protected void updateProgress(double workDone, double max) {
        double total = Double.isInfinite(max) || Double.isNaN(max) || max < 0 ? -1 : max;
        double done = Double.isInfinite(workDone) || Double.isNaN(workDone) || workDone < 0 ? -1 : workDone;
        if (done > total) {
            done = total;
        }
        if (Platform.isFxApplicationThread()) {
            applyProgress(done, total);
        } else if (progressUpdate.getAndSet(new ProgressUpdate(done, total)) == null) {
            // Nothing was waiting, so nothing is on its way to collect this
            // one. Later updates replace it until it is collected.
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ProgressUpdate update = progressUpdate.getAndSet(null);
                    if (update != null) {
                        applyProgress(update.workDone, update.totalWork);
                    }
                }
            });
        }
    }

    private void applyProgress(double done, double total) {
        totalWork.set(total);
        workDone.set(done);
        progress.set(done < 0 || total <= 0 ? -1 : done / total);
    }

    /// Reports what the work is doing, from any thread.
    protected void updateMessage(String message) {
        if (Platform.isFxApplicationThread()) {
            this.message.set(message);
        } else if (messageUpdate.getAndSet(message) == null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Task.this.message.set(messageUpdate.getAndSet(null));
                }
            });
        }
    }

    /// Reports the title of the work, from any thread.
    protected void updateTitle(String title) {
        if (Platform.isFxApplicationThread()) {
            this.title.set(title);
        } else if (titleUpdate.getAndSet(title) == null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Task.this.title.set(titleUpdate.getAndSet(null));
                }
            });
        }
    }

    /// Publishes a partial result, from any thread; the result
    /// `call()` returns replaces it in the end.
    protected void updateValue(V value) {
        if (Platform.isFxApplicationThread()) {
            this.value.set(value);
        } else if (valueUpdate.getAndSet(value) == null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Task.this.value.set(valueUpdate.getAndSet(null));
                }
            });
        }
    }

    // ---- handlers ----

    /// The handler of the task being scheduled.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onScheduledProperty() {
        checkThread();
        return events.slot(State.SCHEDULED);
    }

    /// Returns the handler of the task being scheduled.
    public final EventHandler<WorkerStateEvent> getOnScheduled() {
        checkThread();
        return events.get(State.SCHEDULED);
    }

    /// Sets the handler of the task being scheduled.
    public final void setOnScheduled(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.SCHEDULED).set(value);
    }

    /// The handler of the task starting to run.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onRunningProperty() {
        checkThread();
        return events.slot(State.RUNNING);
    }

    /// Returns the handler of the task starting to run.
    public final EventHandler<WorkerStateEvent> getOnRunning() {
        checkThread();
        return events.get(State.RUNNING);
    }

    /// Sets the handler of the task starting to run.
    public final void setOnRunning(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.RUNNING).set(value);
    }

    /// The handler of the task finishing with a result.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onSucceededProperty() {
        checkThread();
        return events.slot(State.SUCCEEDED);
    }

    /// Returns the handler of the task finishing with a result.
    public final EventHandler<WorkerStateEvent> getOnSucceeded() {
        checkThread();
        return events.get(State.SUCCEEDED);
    }

    /// Sets the handler of the task finishing with a result.
    public final void setOnSucceeded(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.SUCCEEDED).set(value);
    }

    /// The handler of the task being cancelled.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onCancelledProperty() {
        checkThread();
        return events.slot(State.CANCELLED);
    }

    /// Returns the handler of the task being cancelled.
    public final EventHandler<WorkerStateEvent> getOnCancelled() {
        checkThread();
        return events.get(State.CANCELLED);
    }

    /// Sets the handler of the task being cancelled.
    public final void setOnCancelled(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.CANCELLED).set(value);
    }

    /// The handler of the task being ended by an exception.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onFailedProperty() {
        checkThread();
        return events.slot(State.FAILED);
    }

    /// Returns the handler of the task being ended by an exception.
    public final EventHandler<WorkerStateEvent> getOnFailed() {
        checkThread();
        return events.get(State.FAILED);
    }

    /// Sets the handler of the task being ended by an exception.
    public final void setOnFailed(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.FAILED).set(value);
    }

    /// Adds a handler for a type of event and its sub types.
    public final <T extends Event> void addEventHandler(final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        checkThread();
        events.manager.addEventHandler(eventType, eventHandler);
    }

    /// Removes a handler.
    public final <T extends Event> void removeEventHandler(final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        checkThread();
        events.manager.removeEventHandler(eventType, eventHandler);
    }

    /// Adds a filter, which sees an event before the handlers do.
    public final <T extends Event> void addEventFilter(final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        checkThread();
        events.manager.addEventFilter(eventType, eventFilter);
    }

    /// Removes a filter.
    public final <T extends Event> void removeEventFilter(final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        checkThread();
        events.manager.removeEventFilter(eventType, eventFilter);
    }

    /// Delivers an event to the handlers of the task.
    public final void fireEvent(Event event) {
        checkThread();
        Event.fireEvent(this, event);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        checkThread();
        return tail.prepend(events.manager);
    }
}
