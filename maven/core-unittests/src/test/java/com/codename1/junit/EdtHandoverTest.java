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
package com.codename1.junit;

import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.WindowManager;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestWindowManager;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Window;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code Display.init()} that lands while the previous dispatch thread is tearing down must
 * start a dispatch thread of its own, and that thread's teardown must stay inside its own
 * generation.
 *
 * <p>This is the bug behind the intermittent {@code FormTest timed out after 5000ms;
 * edt=display-not-initialized} that has failed a different test class each time it appeared, and
 * that the harness has been patched for twice ({@link UITestBase} and {@link FormTestInterceptor}
 * both carry a recovery for it). Those patches took ValidatorTest from twelve failures to one --
 * neither can close the window, because it is not the harness that is wrong.</p>
 *
 * <p>A thread that has left the dispatch loop stays {@code isAlive()} for the whole of its
 * teardown, and {@code init()} decided whether to start a dispatch thread on exactly that
 * evidence. Adopting a departing thread leaves the new generation with no dispatch at all:
 * everything it queues waits for ever, and {@code isInitialized()} stays false while
 * {@code codenameOneRunning} stays true -- a state {@code init()} cannot repair, because it
 * guards on that flag.</p>
 *
 * <p>Not starting a dispatch thread is only one of the two ways that ends in
 * display-not-initialized, which is why the teardown is tested here as well. Once the adoption
 * stops, the two generations overlap by design, and a teardown that reads the process-wide
 * {@code impl} slot rather than its own generation deinitializes the SUCCESSOR -- the same
 * symptom through the other door.</p>
 *
 * <p>Reproduced rather than raced for. The window is only open while a loaded machine happens to
 * be descheduling the old thread, which is why this never failed locally; an implementation that
 * parks the departing thread inside its teardown holds it open instead, so each test either
 * passes or fails for the reason it names. Adjacency in the source is worth nothing here: every
 * failure in this class is the departing thread being descheduled between two statements.</p>
 */
class EdtHandoverTest {

    /** How long a working dispatch thread is given to run one runnable. */
    private static final long DISPATCH_TIMEOUT = 5000L;

    /**
     * A one shot gate: a thread parks in it, the test observes that it arrived and lets it go.
     *
     * <p>The point of every fixture here is to hold the departing dispatch thread at a chosen
     * point of its teardown for as long as the test needs, so the ordering under test is fixed
     * rather than hoped for.</p>
     */
    private static final class Gate {
        private boolean entered;
        private boolean released;
        private Thread parked;

        /** Parks the calling thread until {@link #release()}, recording that it arrived. */
        synchronized void park() {
            entered = true;
            parked = Thread.currentThread();
            notifyAll();
            long deadline = System.currentTimeMillis() + DISPATCH_TIMEOUT;
            // Bounded on purpose. A blocked EDT that is never released would hang the whole
            // suite rather than fail this one test, and the failure would then be reported
            // against whichever class the runner happened to reach.
            while (!released && System.currentTimeMillis() < deadline) {
                try {
                    wait(deadline - System.currentTimeMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        /** Whether a thread reached {@link #park()} within the timeout. */
        synchronized boolean awaitEntered() {
            long deadline = System.currentTimeMillis() + DISPATCH_TIMEOUT;
            while (!entered && System.currentTimeMillis() < deadline) {
                try {
                    wait(deadline - System.currentTimeMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return entered;
                }
            }
            return entered;
        }

        synchronized void release() {
            released = true;
            notifyAll();
        }

        /** The parked thread, so a test can wait for it to finish dying rather than sleep. */
        synchronized Thread parkedThread() {
            return parked;
        }
    }

    /**
     * An implementation that parks the departing dispatch thread inside its teardown.
     *
     * <p>{@code mainEDTLoop} calls this after leaving the loop and before it stops being alive,
     * so blocking here holds the thread in exactly the state the race produces.</p>
     */
    private static class BlockingDeinitImplementation extends TestCodenameOneImplementation {
        private final Gate gate = new Gate();

        @Override
        public void deinitialize() {
            gate.park();
            super.deinitialize();
        }

        Gate gate() {
            return gate;
        }
    }

    /**
     * An implementation that parks the departing thread EARLIER in the same teardown: inside
     * {@code Desktop.disposeAll()}, which runs before the implementation to deinitialize is
     * read.
     *
     * <p>That difference is the whole point of the fixture. Parking inside {@code deinitialize()}
     * cannot exercise a teardown that reads the wrong implementation, because the receiver of
     * that call has already been resolved by the time the thread parks -- a test built on it
     * passes whether the teardown is generation scoped or not.</p>
     */
    private static final class BlockingWindowDisposeImplementation extends TestCodenameOneImplementation {
        private final Gate gate = new Gate();
        private final WindowManager manager = new TestWindowManager() {
            @Override
            public void dispose(Object peer) {
                gate.park();
                super.dispose(peer);
            }
        };

        @Override
        public WindowManager getWindowManager() {
            return manager;
        }

        Gate gate() {
            return gate;
        }
    }

    @Test
    void anInitDuringTheOldEdtsTeardownWaitsForItAndThenStartsItsOwnDispatchThread() {
        BlockingDeinitImplementation blocking = new BlockingDeinitImplementation();
        startGeneration(blocking);

        // Send the old generation into its teardown and wait until it is parked there: alive,
        // and no longer dispatching. This is the whole point of the fixture -- from here the
        // ordering is fixed rather than hoped for.
        Display.deinitialize();
        assertTrue(blocking.gate().awaitEntered(),
                "the dispatch thread never reached the teardown, so the window this test is "
                        + "about was never opened and what follows would prove nothing");

        install(new TestCodenameOneImplementation());
        Thread init = initOnAnotherThread();
        assertFalse(finishedWithin(init, 500L),
                "init() ran to completion alongside a teardown that had not finished. The two "
                        + "act on the same process wide state -- the impl slot, the "
                        + "implementation's initialized flag, Desktop's window registry -- so "
                        + "they have to be ordered, not interleaved");

        blocking.gate().release();
        assertTrue(finishedWithin(init, DISPATCH_TIMEOUT),
                "init() never came back after the teardown it was waiting for finished");

        assertTrue(dispatchWorks(),
                "init() adopted a thread that had already left the dispatch loop, so this "
                        + "generation has no event dispatch: everything it queues waits for "
                        + "ever, which is the display-not-initialized timeout CI reports");
    }

    /**
     * The departing thread must not disown a dispatch thread that is not its own.
     *
     * <p>Once the fix above stops the adoption, the two generations overlap by design: the old
     * thread finishes its teardown after the new one is running. It used to clear the recorded
     * EDT unconditionally at that point, so {@code isEdt()} answered false ON the live dispatch
     * thread -- and work meant for that thread was queued behind itself.</p>
     */
    @Test
    void aDepartingEdtDoesNotDisownItsSuccessor() {
        BlockingDeinitImplementation blocking = new BlockingDeinitImplementation();
        startGeneration(blocking);

        Display.deinitialize();
        assertTrue(blocking.gate().awaitEntered(), "the fixture never parked the departing thread");

        install(new TestCodenameOneImplementation());
        Thread init = initOnAnotherThread();

        // Let the old thread finish, which is where it used to clear the field the successor is
        // recorded in, and only then let init() through.
        blocking.gate().release();
        assertTrue(finishedWithin(init, DISPATCH_TIMEOUT), "init() never came back");
        assertTrue(awaitDeath(blocking.gate().parkedThread()),
                "the departing thread never finished its teardown");

        assertTrue(dispatchThreadKnowsItself(),
                "the departing thread cleared the recorded dispatch thread on its way out, so "
                        + "isEdt() answers false ON the live dispatch thread and work meant for "
                        + "it is queued behind itself");
    }

    /**
     * The departing thread must deinitialize the implementation IT served, not whichever one the
     * static slot names by the time it gets there.
     *
     * <p>{@code impl} is a single slot that {@code init()} overwrites, and the teardown runs long
     * after the successor may have replaced it. Reading it at the call site deinitialized the
     * successor's implementation, which leaves {@code isInitialized()} false while
     * {@code codenameOneRunning} stays true -- the same unrepairable state, and the same
     * display-not-initialized timeout, as failing to start a dispatch thread at all. The harness
     * already describes this failure in {@link UITestBase}: "the previous class's EDT calls
     * impl.deinitialize() on its way out, on whatever implementation is current BY THEN".</p>
     */
    @Test
    void aDepartingEdtDeinitializesItsOwnImplementationNotItsSuccessors() {
        BlockingWindowDisposeImplementation blocking = new BlockingWindowDisposeImplementation();
        startGeneration(blocking);
        assertTrue(openWindow(),
                "a window is what gives Desktop.disposeAll() something to park in, and without "
                        + "the park the thread runs the whole teardown before the successor "
                        + "exists -- which is the ordering that proves nothing");

        Display.deinitialize();
        assertTrue(blocking.gate().awaitEntered(),
                "the departing thread never reached disposeAll(), so it never parked BEFORE the "
                        + "implementation to tear down is read");

        TestCodenameOneImplementation successor = new TestCodenameOneImplementation();
        install(successor);
        Thread init = initOnAnotherThread();

        blocking.gate().release();
        assertTrue(finishedWithin(init, DISPATCH_TIMEOUT), "init() never came back");
        assertTrue(awaitDeath(blocking.gate().parkedThread()),
                "the departing thread never finished its teardown");
        assertTrue(dispatchWorks(), "the successor generation never came up");

        assertFalse(blocking.isInitialized(),
                "the departing generation's own implementation was left initialized, so the "
                        + "teardown tore down something else");
        assertTrue(successor.isInitialized(),
                "the departing thread deinitialized the SUCCESSOR's implementation: "
                        + "Display.isInitialized() is now false with codenameOneRunning true, "
                        + "which init() cannot repair because it guards on that flag");
        assertTrue(Display.isInitialized(),
                "the display is in the half torn down state every test in the next class "
                        + "reports as display-not-initialized");
    }

    /**
     * A departing thread must leave an implementation the successor is RUNNING ON alone, even
     * though it is the very one it served itself.
     *
     * <p>Tearing down "the implementation this generation served" is not enough when the host
     * hands out one implementation for every generation, which is exactly what the unit test
     * harness does: {@link UITestBase} reuses {@code TestCodenameOneImplementation.getInstance()}
     * from class to class. The departing thread and the successor then hold the same object, so
     * a teardown scoped by identity still deinitializes the live one -- and the successor is
     * permanently half up, because {@code isInitialized()} is the implementation's flag AND
     * {@code codenameOneRunning}, and {@code init()} guards on the one that is still true.</p>
     *
     * <p>This is the shape the whole suite hits between classes, and it is why the question the
     * teardown asks has to be "is it in service" rather than "is it mine".</p>
     */
    @Test
    void aDepartingEdtLeavesAnImplementationItsSuccessorIsRunningOnAlone() {
        BlockingWindowDisposeImplementation shared = new BlockingWindowDisposeImplementation();
        startGeneration(shared);
        assertTrue(openWindow(), "the fixture needs an open window to park the teardown in");

        Display.deinitialize();
        assertTrue(shared.gate().awaitEntered(),
                "the departing thread never parked before the implementation is read");

        // The SAME instance, which is what the harness does between test classes.
        install(shared);
        Thread init = initOnAnotherThread();

        shared.gate().release();
        assertTrue(finishedWithin(init, DISPATCH_TIMEOUT), "init() never came back");
        assertTrue(awaitDeath(shared.gate().parkedThread()),
                "the departing thread never finished its teardown");
        assertTrue(dispatchWorks(), "the successor generation never came up");

        assertTrue(shared.isInitialized(),
                "the departing thread deinitialized the implementation its successor is running "
                        + "on, because it was also the one it served itself");
        assertTrue(Display.isInitialized(),
                "the display is in the half torn down state every test in the next class "
                        + "reports as display-not-initialized");
    }

    /**
     * Calls {@code Display.init(null)} on a thread of its own and hands the thread back.
     *
     * <p>Off the test thread because an init() that lands during a teardown WAITS for it. The
     * blocking is the property under test, so the test needs to observe it rather than sit in
     * it.</p>
     */
    private static Thread initOnAnotherThread() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Display.init(null);
            }
        }, "edt-handover-init");
        t.start();
        return t;
    }

    /** Whether the thread finished within the given time. */
    private static boolean finishedWithin(Thread t, long millis) {
        try {
            t.join(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return !t.isAlive();
    }

    /**
     * Brings up a generation on {@code impl} and gets its dispatch thread into the dispatch loop.
     *
     * <p>The showing of a form is not decoration. {@code mainEDTLoop} spends its first phase in a
     * separate loop that runs until there is a current form, and only the loop AFTER that one has
     * a teardown -- so a thread with no form never reaches the state this test is about, and the
     * fixture silently proved nothing until it did.</p>
     */
    private static void startGeneration(TestCodenameOneImplementation impl) {
        install(impl);
        Display.deinitialize();
        Display.init(null);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                new Form("edt-handover").show();
            }
        });
        long deadline = System.currentTimeMillis() + DISPATCH_TIMEOUT;
        while (Display.getInstance().getCurrent() == null
                && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Opens one window on the EDT and reports whether it reached the desktop registry. */
    private static boolean openWindow() {
        final boolean[] opened = new boolean[1];
        final Object done = new Object();
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                synchronized (done) {
                    Window w = new Window("edt-handover-window");
                    w.setWindowSize(320, 240);
                    w.show();
                    opened[0] = true;
                    done.notifyAll();
                }
            }
        });
        return await(done, opened);
    }

    /**
     * Whether a dispatch thread runs queued work AND is recognised as the dispatch thread while
     * it does.
     *
     * <p>Running the runnable is not enough on its own: a thread that is dispatching while
     * {@code isEdt()} denies it still drains the queue, so an assertion that only watched the
     * runnable run would pass either way.</p>
     */
    private static boolean dispatchThreadKnowsItself() {
        final boolean[] answer = new boolean[1];
        final Object done = new Object();
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                synchronized (done) {
                    answer[0] = Display.getInstance().isEdt();
                    done.notifyAll();
                }
            }
        });
        return await(done, answer);
    }

    /** Queues one runnable and reports whether a dispatch thread actually ran it. */
    private static boolean dispatchWorks() {
        final boolean[] ran = new boolean[1];
        final Object done = new Object();
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                synchronized (done) {
                    ran[0] = true;
                    done.notifyAll();
                }
            }
        });
        return await(done, ran);
    }

    /** Waits for a runnable queued on the EDT to report its answer. */
    private static boolean await(Object done, boolean[] answer) {
        synchronized (done) {
            long deadline = System.currentTimeMillis() + DISPATCH_TIMEOUT;
            while (!answer[0] && System.currentTimeMillis() < deadline) {
                try {
                    done.wait(deadline - System.currentTimeMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return answer[0];
        }
    }

    /**
     * Waits for the released thread to finish dying.
     *
     * <p>Joined rather than slept on. The state these tests read is written by the last few
     * statements that thread runs, so a fixed pause is either slower than it needs to be or
     * short enough to read the state half written on a loaded machine -- which is the very
     * failure mode the whole class is about.</p>
     */
    private static boolean awaitDeath(Thread t) {
        if (t == null) {
            return false;
        }
        try {
            t.join(DISPATCH_TIMEOUT);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return !t.isAlive();
    }

    private static void install(final TestCodenameOneImplementation impl) {
        ImplementationFactory.setInstance(new ImplementationFactory() {
            @Override
            public Object createImplementation() {
                return impl;
            }
        });
    }
}
