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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

/// Tasks and services. The tests run on the application thread, as an
/// application's code does, so a test that waits for a background thread
/// lets the event queue run meanwhile (`await`): it never sleeps for a
/// result, it waits for a condition, and gives up after a bound.
public class ConcurrencyTest {

    private static final long BOUND_MILLIS = 20000;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> log = new ArrayList<String>();

    /// The display has to exist before the rule decides which thread a
    /// test runs on, or the first test of the JVM runs off the
    /// application thread.
    @BeforeClass
    public static void display() {
        HeadlessImplementation.install();
    }

    private interface Check {
        boolean ok();
    }

    /// Lets the event queue run until a condition holds.
    private static void await(String what, Check check) {
        long deadline = System.currentTimeMillis() + BOUND_MILLIS;
        while (!check.ok()) {
            if (System.currentTimeMillis() > deadline) {
                fail("timed out waiting for " + what);
            }
            MainThreadRule.drain();
        }
    }

    /// Holds the application thread, delivering nothing, until a
    /// background thread has raised a flag.
    private static void holdUntil(String what, AtomicBoolean flag) {
        long deadline = System.currentTimeMillis() + BOUND_MILLIS;
        while (!flag.get()) {
            if (System.currentTimeMillis() > deadline) {
                fail("timed out waiting for " + what);
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                fail("interrupted");
            }
        }
    }

    private static void pause() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            // a cancelled task is interrupted; the loop around this looks
        }
    }

    private void record(final Worker<?> worker, final String prefix) {
        worker.stateProperty().addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue<? extends State> observable, State old, State next) {
                log.add(prefix + next + (Platform.isFxApplicationThread() ? "" : " OFF THE FX THREAD"));
            }
        });
        worker.runningProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean old, Boolean next) {
                log.add(prefix + "running=" + next + (Platform.isFxApplicationThread() ? "" : " OFF THE FX THREAD"));
            }
        });
    }

    private EventHandler<WorkerStateEvent> note(final String what) {
        return new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                log.add(what + (Platform.isFxApplicationThread() ? "" : " OFF THE FX THREAD"));
            }
        };
    }

    // ---- tasks ----

    @Test
    public void aTaskSucceeds() {
        assertTrue(Platform.isFxApplicationThread());
        final AtomicBoolean calledOnFxThread = new AtomicBoolean(true);
        final Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                calledOnFxThread.set(Platform.isFxApplicationThread());
                updateTitle("copying");
                updateMessage("half way");
                updateProgress(1, 2);
                return "done";
            }

            @Override
            protected void scheduled() {
                log.add("scheduled()");
            }

            @Override
            protected void running() {
                log.add("running()");
            }

            @Override
            protected void succeeded() {
                log.add("succeeded() " + getValue());
            }
        };
        record(task, "");
        task.setOnScheduled(note("onScheduled"));
        task.setOnRunning(note("onRunning"));
        task.setOnSucceeded(note("onSucceeded"));
        task.setOnFailed(note("onFailed"));
        task.setOnCancelled(note("onCancelled"));
        final List<Object> sources = new ArrayList<Object>();
        task.addEventHandler(WorkerStateEvent.ANY, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                sources.add(event.getSource());
                sources.add(event.getEventType());
            }
        });
        assertSame(State.READY, task.getState());
        assertEquals(-1.0, task.getProgress(), 0);
        assertEquals("", task.getMessage());
        assertFalse(task.isDone());

        new Thread(task).start();
        await("the task to succeed", new Check() {
            @Override
            public boolean ok() {
                return task.getState() == State.SUCCEEDED;
            }
        });
        assertEquals(Arrays.asList("SCHEDULED", "running=true", "onScheduled", "scheduled()", "RUNNING",
                "onRunning", "running()", "SUCCEEDED", "running=false", "onSucceeded", "succeeded() done"), log);
        assertEquals(Arrays.asList(task, WorkerStateEvent.WORKER_STATE_SCHEDULED, task,
                WorkerStateEvent.WORKER_STATE_RUNNING, task, WorkerStateEvent.WORKER_STATE_SUCCEEDED), sources);
        assertFalse("call() runs off the application thread", calledOnFxThread.get());
        assertEquals("done", task.getValue());
        assertNull(task.getException());
        assertEquals("copying", task.getTitle());
        assertEquals("half way", task.getMessage());
        assertEquals(1.0, task.getWorkDone(), 0);
        assertEquals(2.0, task.getTotalWork(), 0);
        assertEquals(0.5, task.getProgress(), 0);
        assertFalse(task.isRunning());
        assertTrue(task.isDone());
        assertFalse(task.isCancelled());
        assertFalse("a finished task cannot be cancelled", task.cancel());
        assertSame(State.SUCCEEDED, task.getState());
        assertSame(task.getOnSucceeded(), task.onSucceededProperty().get());
    }

    @Test
    public void aTaskFails() {
        final IllegalStateException boom = new IllegalStateException("boom");
        final Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                throw boom;
            }

            @Override
            protected void failed() {
                log.add("failed()");
            }

            @Override
            protected void succeeded() {
                log.add("succeeded()");
            }
        };
        record(task, "");
        task.setOnFailed(note("onFailed"));
        task.setOnSucceeded(note("onSucceeded"));
        new Thread(task).start();
        await("the task to fail", new Check() {
            @Override
            public boolean ok() {
                return task.getState() == State.FAILED;
            }
        });
        assertEquals(Arrays.asList("SCHEDULED", "running=true", "RUNNING", "FAILED", "running=false", "onFailed",
                "failed()"), log);
        assertSame(boom, task.getException());
        assertNull(task.getValue());
        assertTrue(task.isDone());
        assertFalse(task.isCancelled());
    }

    /// A task that works until it is cancelled or released.
    private final class Waiting extends Task<String> {
        final AtomicBoolean entered = new AtomicBoolean();
        final AtomicBoolean left = new AtomicBoolean();
        final AtomicBoolean release = new AtomicBoolean();
        final String result;

        Waiting(String result) {
            this.result = result;
        }

        @Override
        protected String call() {
            entered.set(true);
            try {
                while (!isCancelled() && !release.get()) {
                    pause();
                }
                return result;
            } finally {
                left.set(true);
            }
        }

        @Override
        protected void cancelled() {
            log.add("task cancelled()");
        }

        @Override
        protected void succeeded() {
            log.add("task succeeded()");
        }
    }

    @Test
    public void aRunningTaskIsCancelled() {
        final Waiting task = new Waiting("never");
        record(task, "");
        task.setOnCancelled(note("onCancelled"));
        task.setOnSucceeded(note("onSucceeded"));
        new Thread(task).start();
        await("the task to run", new Check() {
            @Override
            public boolean ok() {
                return task.entered.get() && task.getState() == State.RUNNING;
            }
        });
        assertTrue(task.isRunning());
        assertTrue(task.cancel());
        // On the application thread the state changes there and then.
        assertSame(State.CANCELLED, task.getState());
        assertTrue(task.isCancelled());
        assertTrue(task.isDone());
        assertFalse(task.isRunning());
        assertFalse("cancelled once", task.cancel());
        await("call() to return", new Check() {
            @Override
            public boolean ok() {
                return task.left.get();
            }
        });
        MainThreadRule.drain();
        MainThreadRule.drain();
        // What call() returned after the cancellation is dropped.
        assertEquals(Arrays.asList("SCHEDULED", "running=true", "RUNNING", "CANCELLED", "running=false",
                "onCancelled", "task cancelled()"), log);
        assertSame(State.CANCELLED, task.getState());
        assertNull(task.getValue());
    }

    @Test
    public void aTaskCancelledBeforeItRunsNeverCalls() {
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean returned = new AtomicBoolean();
        final Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                called.set(true);
                return "x";
            }
        };
        record(task, "");
        assertTrue(task.cancel(false));
        assertSame(State.CANCELLED, task.getState());
        new Thread(new Runnable() {
            @Override
            public void run() {
                task.run();
                returned.set(true);
            }
        }).start();
        await("run() to return", new Check() {
            @Override
            public boolean ok() {
                return returned.get();
            }
        });
        MainThreadRule.drain();
        assertFalse(called.get());
        assertEquals(Arrays.asList("CANCELLED"), log);
    }

    @Test
    public void aTaskCancelledFromAnotherThreadChangesStateOnTheFxThread() {
        final Waiting task = new Waiting("never");
        record(task, "");
        new Thread(task).start();
        await("the task to run", new Check() {
            @Override
            public boolean ok() {
                return task.entered.get() && task.getState() == State.RUNNING;
            }
        });
        final AtomicReference<Boolean> answer = new AtomicReference<Boolean>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                answer.set(Boolean.valueOf(task.cancel()));
            }
        }).start();
        await("the cancellation to arrive", new Check() {
            @Override
            public boolean ok() {
                return task.getState() == State.CANCELLED;
            }
        });
        assertEquals(Boolean.TRUE, answer.get());
        assertEquals(Arrays.asList("SCHEDULED", "running=true", "RUNNING", "CANCELLED", "running=false",
                "task cancelled()"), log);
    }

    @Test
    public void rapidUpdatesAreMergedAndTheLastOneArrives() {
        final AtomicBoolean produced = new AtomicBoolean();
        final AtomicBoolean release = new AtomicBoolean();
        final Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() {
                for (int i = 1; i <= 1000; i++) {
                    updateProgress(i, 1000);
                    updateMessage("step " + i);
                    updateTitle("title " + i);
                    updateValue(Integer.valueOf(i));
                }
                produced.set(true);
                while (!release.get()) {
                    pause();
                }
                return Integer.valueOf(-1);
            }
        };
        final List<String> messages = new ArrayList<String>();
        final List<Double> progress = new ArrayList<Double>();
        final List<Integer> values = new ArrayList<Integer>();
        final AtomicInteger offThread = new AtomicInteger();
        task.messageProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String old, String next) {
                messages.add(next);
                if (!Platform.isFxApplicationThread()) {
                    offThread.incrementAndGet();
                }
            }
        });
        task.progressProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number old, Number next) {
                progress.add(Double.valueOf(next.doubleValue()));
                if (!Platform.isFxApplicationThread()) {
                    offThread.incrementAndGet();
                }
            }
        });
        task.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observable, Integer old, Integer next) {
                values.add(next);
            }
        });
        new Thread(task).start();
        // The application thread is busy here, in this test, so none of the
        // thousand updates can be delivered while they are made.
        holdUntil("the updates", produced);
        assertTrue(messages.isEmpty());
        assertTrue(progress.isEmpty());
        await("the updates to arrive", new Check() {
            @Override
            public boolean ok() {
                return !messages.isEmpty() && !progress.isEmpty() && !values.isEmpty()
                        && "title 1000".equals(task.getTitle());
            }
        });
        MainThreadRule.drain();
        assertEquals(Arrays.asList("step 1000"), messages);
        assertEquals(Arrays.asList(Double.valueOf(1.0)), progress);
        assertEquals(Arrays.asList(Integer.valueOf(1000)), values);
        assertEquals(1000.0, task.getWorkDone(), 0);
        assertEquals(1000.0, task.getTotalWork(), 0);
        assertEquals(0, offThread.get());
        release.set(true);
        await("the task to succeed", new Check() {
            @Override
            public boolean ok() {
                return task.getState() == State.SUCCEEDED;
            }
        });
        assertEquals("the result replaces the partial value", Integer.valueOf(-1), task.getValue());
    }

    @Test
    public void progressNumbersAreNormalised() {
        /// Reports on the calling thread, which is the application thread.
        final class Reporter extends Task<Void> {
            @Override
            protected Void call() {
                return null;
            }

            void report(double done, double max) {
                updateProgress(done, max);
            }

            void report(long done, long max) {
                updateProgress(done, max);
            }
        }
        Reporter task = new Reporter();
        task.report(3L, 4L);
        assertEquals(0.75, task.getProgress(), 0);
        task.report(9.0, 4.0);
        assertEquals(4.0, task.getWorkDone(), 0);
        assertEquals(1.0, task.getProgress(), 0);
        task.report(-5.0, 4.0);
        assertEquals(-1.0, task.getWorkDone(), 0);
        assertEquals(-1.0, task.getProgress(), 0);
        task.report(Double.NaN, Double.POSITIVE_INFINITY);
        assertEquals(-1.0, task.getTotalWork(), 0);
        assertEquals(-1.0, task.getProgress(), 0);
        task.report(0.0, 0.0);
        assertEquals("nothing to do is not a division by zero", -1.0, task.getProgress(), 0);
    }

    @Test
    public void aStartedTaskBelongsToTheFxThread() {
        final Waiting task = new Waiting("x");
        new Thread(task).start();
        await("the task to run", new Check() {
            @Override
            public boolean ok() {
                return task.entered.get();
            }
        });
        final AtomicReference<String> seen = new AtomicReference<String>();
        final AtomicBoolean finished = new AtomicBoolean();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    task.getState();
                    seen.set("no exception");
                } catch (IllegalStateException expected) {
                    seen.set("refused");
                }
                finished.set(true);
            }
        }).start();
        await("the other thread", new Check() {
            @Override
            public boolean ok() {
                return finished.get();
            }
        });
        assertEquals("refused", seen.get());
        task.release.set(true);
        await("the task to succeed", new Check() {
            @Override
            public boolean ok() {
                return task.getState() == State.SUCCEEDED;
            }
        });
        assertEquals("x", task.getValue());
    }

    // ---- services ----

    /// A service whose tasks answer at once, or work until released.
    private final class Counting extends Service<String> {
        int created;
        boolean waiting;
        Waiting last;

        @Override
        protected Task<String> createTask() {
            created++;
            final String result = "v" + created;
            if (waiting) {
                last = new Waiting(result);
                return last;
            }
            return new Task<String>() {
                @Override
                protected String call() {
                    updateMessage("made " + result);
                    return result;
                }
            };
        }

        @Override
        protected void ready() {
            log.add("ready()");
        }

        @Override
        protected void succeeded() {
            log.add("succeeded() " + getValue());
        }

        @Override
        protected void cancelled() {
            log.add("cancelled()");
        }
    }

    private void awaitState(final Service<?> service, final State state) {
        await("the service to be " + state, new Check() {
            @Override
            public boolean ok() {
                return service.getState() == state;
            }
        });
    }

    @Test
    public void aServiceStartsResetsAndStartsAgain() {
        final Counting service = new Counting();
        record(service, "");
        service.setOnSucceeded(note("onSucceeded"));
        service.setOnReady(note("onReady"));
        assertSame(State.READY, service.getState());
        service.start();
        assertSame("scheduled by start itself", State.SCHEDULED, service.getState());
        assertTrue(service.isRunning());
        awaitState(service, State.SUCCEEDED);
        assertEquals(Arrays.asList("SCHEDULED", "running=true", "RUNNING", "onSucceeded", "succeeded() v1",
                "SUCCEEDED", "running=false"), log);
        assertEquals("v1", service.getValue());
        assertEquals("made v1", service.getMessage());
        assertFalse(service.isRunning());
        try {
            service.start();
            fail("a service that ended has to be reset first");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertFalse("nothing left to cancel", service.cancel());

        log.clear();
        service.reset();
        assertEquals(Arrays.asList("onReady", "ready()", "READY"), log);
        assertSame(State.READY, service.getState());
        assertNull(service.getValue());
        assertEquals("", service.getMessage());
        assertEquals(-1.0, service.getProgress(), 0);

        log.clear();
        service.start();
        awaitState(service, State.SUCCEEDED);
        assertEquals("v2", service.getValue());
        assertEquals(2, service.created);

        log.clear();
        service.restart();
        awaitState(service, State.SUCCEEDED);
        assertEquals("v3", service.getValue());
        assertEquals(Arrays.asList("cancelled()", "CANCELLED", "onReady", "ready()", "READY", "SCHEDULED",
                "running=true", "RUNNING", "onSucceeded", "succeeded() v3", "SUCCEEDED", "running=false"), log);
    }

    @Test
    public void aRunningServiceIsCancelledAndRestarted() {
        final Counting service = new Counting();
        service.waiting = true;
        record(service, "");
        service.start();
        try {
            service.reset();
            fail("a scheduled service cannot be reset");
        } catch (IllegalStateException expected) {
            // expected
        }
        try {
            service.start();
            fail("a scheduled service cannot be started");
        } catch (IllegalStateException expected) {
            // expected
        }
        awaitState(service, State.RUNNING);
        try {
            service.reset();
            fail("a running service cannot be reset");
        } catch (IllegalStateException expected) {
            // expected
        }
        final Waiting first = service.last;
        assertTrue(service.cancel());
        assertSame(State.CANCELLED, service.getState());
        assertTrue(first.isCancelled());
        assertFalse(service.cancel());
        assertEquals(Arrays.asList("SCHEDULED", "running=true", "RUNNING", "cancelled()", "CANCELLED",
                "running=false", "task cancelled()"), log);
        await("the first task to leave", new Check() {
            @Override
            public boolean ok() {
                return first.left.get();
            }
        });

        // Restarting a running service cancels its task and runs a new one.
        service.reset();
        service.start();
        awaitState(service, State.RUNNING);
        final Waiting second = service.last;
        log.clear();
        service.waiting = false;
        service.restart();
        assertTrue(second.isCancelled());
        awaitState(service, State.SUCCEEDED);
        assertEquals("v3", service.getValue());
        assertEquals(Arrays.asList("cancelled()", "CANCELLED", "running=false", "task cancelled()", "ready()", "READY",
                "SCHEDULED",
                "running=true", "RUNNING", "succeeded() v3", "SUCCEEDED", "running=false"), log);
        await("the second task to leave", new Check() {
            @Override
            public boolean ok() {
                return second.left.get();
            }
        });
    }

    @Test
    public void aServiceThatNeverStartedCanBeCancelled() {
        Counting service = new Counting();
        record(service, "");
        assertTrue(service.cancel());
        assertSame(State.CANCELLED, service.getState());
        assertFalse(service.cancel());
        try {
            service.start();
            fail("cancelled is not ready");
        } catch (IllegalStateException expected) {
            // expected
        }
        service.reset();
        assertSame(State.READY, service.getState());
        assertEquals(0, service.created);
    }

    @Test
    public void aServiceBelongsToTheFxThread() {
        final Counting service = new Counting();
        final AtomicReference<String> seen = new AtomicReference<String>();
        final AtomicBoolean finished = new AtomicBoolean();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    service.start();
                    seen.set("no exception");
                } catch (IllegalStateException expected) {
                    seen.set("refused");
                }
                finished.set(true);
            }
        }).start();
        await("the other thread", new Check() {
            @Override
            public boolean ok() {
                return finished.get();
            }
        });
        assertEquals("refused", seen.get());
        assertEquals(0, service.created);
    }
}
