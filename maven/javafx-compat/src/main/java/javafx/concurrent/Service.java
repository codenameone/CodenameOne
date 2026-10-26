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

import com.codename1.ui.Display;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// Work that can be done again and again: every start creates a fresh
/// [Task] with [#createTask()] and runs it on a new background thread,
/// and the properties of the service show that task's state, progress
/// and result.
///
/// A service is used from the JavaFX application thread only. It can
/// be started in the `READY` state alone: [#reset()] takes a service
/// that ended back there, and [#restart()] cancels, resets and starts
/// in one step.
///
/// Differences from JavaFX: Codename One has no
/// `java.util.concurrent.Executor`, so there is no executor property;
/// a subclass that wants its tasks run differently overrides
/// [#executeTask(Task)].
public abstract class Service<V> implements Worker<V>, EventTarget {

    private final WorkerEvents events = new WorkerEvents(this);
    private final ObjectProperty<State> state = new SimpleObjectProperty<State>(this, "state", State.READY);
    private final ObjectProperty<V> value = new SimpleObjectProperty<V>(this, "value");
    private final ObjectProperty<Throwable> exception = new SimpleObjectProperty<Throwable>(this, "exception");
    private final DoubleProperty workDone = new SimpleDoubleProperty(this, "workDone", -1);
    private final DoubleProperty totalWork = new SimpleDoubleProperty(this, "totalWork", -1);
    private final DoubleProperty progress = new SimpleDoubleProperty(this, "progress", -1);
    private final BooleanProperty running = new SimpleBooleanProperty(this, "running", false);
    private final StringProperty message = new SimpleStringProperty(this, "message", "");
    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private Task<V> task;
    private int threadNumber;

    /// Creates a service.
    protected Service() {
        state.addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue<? extends State> observable, State old, State next) {
                entered(next);
            }
        });
    }

    /// Creates the task of one run. Called on the application thread
    /// by every start.
    protected abstract Task<V> createTask();

    /// Runs a task the service has just created. This implementation
    /// starts a new thread for it.
    protected void executeTask(final Task<V> task) {
        threadNumber++;
        String name = "fx-service-" + threadNumber;
        if (Display.isInitialized()) {
            // A Codename One thread, which the platform's crash reporting knows.
            Display.getInstance().createThread(task, name).start();
        } else {
            new Thread(task, name).start();
        }
    }

    // ---- controls ----

    private void checkThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Service must only be used from the FX Application Thread");
        }
    }

    /// Starts the service, which must be `READY`: creates a task,
    /// shows it through the properties and runs it.
    public void start() {
        checkThread();
        if (getState() != State.READY) {
            throw new IllegalStateException("Can only start a Service in the READY state. Was in state "
                    + getState());
        }
        task = createTask();
        state.bind(task.stateProperty());
        value.bind(task.valueProperty());
        exception.bind(task.exceptionProperty());
        workDone.bind(task.workDoneProperty());
        totalWork.bind(task.totalWorkProperty());
        progress.bind(task.progressProperty());
        running.bind(task.runningProperty());
        message.bind(task.messageProperty());
        title.bind(task.titleProperty());
        task.setState(State.SCHEDULED);
        executeTask(task);
    }

    /// Cancels the task that is running, if any. The service then
    /// stays `CANCELLED` until it is reset.
    @Override
    public boolean cancel() {
        checkThread();
        if (task == null) {
            if (state.get() == State.CANCELLED || state.get() == State.SUCCEEDED || state.get() == State.FAILED) {
                return false;
            }
            state.unbind();
            state.set(State.CANCELLED);
            return true;
        }
        return task.cancel(true);
    }

    /// Cancels whatever is running and starts afresh.
    public void restart() {
        checkThread();
        if (task != null) {
            task.cancel();
            task = null;
            state.unbind();
            state.set(State.CANCELLED);
        }
        reset();
        start();
    }

    /// Takes a service that ended back to `READY` with its properties
    /// at their defaults. A service that is scheduled or running cannot
    /// be reset.
    public void reset() {
        checkThread();
        State current = getState();
        if (current == State.SCHEDULED || current == State.RUNNING) {
            throw new IllegalStateException();
        }
        task = null;
        state.unbind();
        state.set(State.READY);
        value.unbind();
        value.set(null);
        exception.unbind();
        exception.set(null);
        workDone.unbind();
        workDone.set(-1);
        totalWork.unbind();
        totalWork.set(-1);
        progress.unbind();
        progress.set(-1);
        running.unbind();
        running.set(false);
        message.unbind();
        message.set("");
        title.unbind();
        title.set("");
    }

    private void entered(State next) {
        if (next == null) {
            return;
        }
        events.fire(this, next);
        switch (next) {
            case READY:
                ready();
                break;
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

    /// Called when the service becomes ready again.
    protected void ready() {
    }

    /// Called when the service is scheduled.
    protected void scheduled() {
    }

    /// Called when the service starts running.
    protected void running() {
    }

    /// Called when the service has finished with a result.
    protected void succeeded() {
    }

    /// Called when the service was cancelled.
    protected void cancelled() {
    }

    /// Called when an exception ended the service.
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
        return state;
    }

    @Override
    public final V getValue() {
        checkThread();
        return value.get();
    }

    @Override
    public final ReadOnlyObjectProperty<V> valueProperty() {
        checkThread();
        return value;
    }

    @Override
    public final Throwable getException() {
        checkThread();
        return exception.get();
    }

    @Override
    public final ReadOnlyObjectProperty<Throwable> exceptionProperty() {
        checkThread();
        return exception;
    }

    @Override
    public final double getWorkDone() {
        checkThread();
        return workDone.get();
    }

    @Override
    public final ReadOnlyDoubleProperty workDoneProperty() {
        checkThread();
        return workDone;
    }

    @Override
    public final double getTotalWork() {
        checkThread();
        return totalWork.get();
    }

    @Override
    public final ReadOnlyDoubleProperty totalWorkProperty() {
        checkThread();
        return totalWork;
    }

    @Override
    public final double getProgress() {
        checkThread();
        return progress.get();
    }

    @Override
    public final ReadOnlyDoubleProperty progressProperty() {
        checkThread();
        return progress;
    }

    @Override
    public final boolean isRunning() {
        checkThread();
        return running.get();
    }

    @Override
    public final ReadOnlyBooleanProperty runningProperty() {
        checkThread();
        return running;
    }

    @Override
    public final String getMessage() {
        checkThread();
        return message.get();
    }

    @Override
    public final ReadOnlyStringProperty messageProperty() {
        checkThread();
        return message;
    }

    @Override
    public final String getTitle() {
        checkThread();
        return title.get();
    }

    @Override
    public final ReadOnlyStringProperty titleProperty() {
        checkThread();
        return title;
    }

    // ---- handlers ----

    /// The handler of the service becoming ready again.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onReadyProperty() {
        checkThread();
        return events.slot(State.READY);
    }

    /// Returns the handler of the service becoming ready again.
    public final EventHandler<WorkerStateEvent> getOnReady() {
        checkThread();
        return events.get(State.READY);
    }

    /// Sets the handler of the service becoming ready again.
    public final void setOnReady(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.READY).set(value);
    }

    /// The handler of the service being scheduled.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onScheduledProperty() {
        checkThread();
        return events.slot(State.SCHEDULED);
    }

    /// Returns the handler of the service being scheduled.
    public final EventHandler<WorkerStateEvent> getOnScheduled() {
        checkThread();
        return events.get(State.SCHEDULED);
    }

    /// Sets the handler of the service being scheduled.
    public final void setOnScheduled(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.SCHEDULED).set(value);
    }

    /// The handler of the service starting to run.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onRunningProperty() {
        checkThread();
        return events.slot(State.RUNNING);
    }

    /// Returns the handler of the service starting to run.
    public final EventHandler<WorkerStateEvent> getOnRunning() {
        checkThread();
        return events.get(State.RUNNING);
    }

    /// Sets the handler of the service starting to run.
    public final void setOnRunning(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.RUNNING).set(value);
    }

    /// The handler of the service finishing with a result.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onSucceededProperty() {
        checkThread();
        return events.slot(State.SUCCEEDED);
    }

    /// Returns the handler of the service finishing with a result.
    public final EventHandler<WorkerStateEvent> getOnSucceeded() {
        checkThread();
        return events.get(State.SUCCEEDED);
    }

    /// Sets the handler of the service finishing with a result.
    public final void setOnSucceeded(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.SUCCEEDED).set(value);
    }

    /// The handler of the service being cancelled.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onCancelledProperty() {
        checkThread();
        return events.slot(State.CANCELLED);
    }

    /// Returns the handler of the service being cancelled.
    public final EventHandler<WorkerStateEvent> getOnCancelled() {
        checkThread();
        return events.get(State.CANCELLED);
    }

    /// Sets the handler of the service being cancelled.
    public final void setOnCancelled(EventHandler<WorkerStateEvent> value) {
        checkThread();
        events.slot(State.CANCELLED).set(value);
    }

    /// The handler of the service being ended by an exception.
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onFailedProperty() {
        checkThread();
        return events.slot(State.FAILED);
    }

    /// Returns the handler of the service being ended by an exception.
    public final EventHandler<WorkerStateEvent> getOnFailed() {
        checkThread();
        return events.get(State.FAILED);
    }

    /// Sets the handler of the service being ended by an exception.
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

    /// Delivers an event to the handlers of the service.
    protected final void fireEvent(Event event) {
        checkThread();
        Event.fireEvent(this, event);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        checkThread();
        return tail.prepend(events.manager);
    }
}
