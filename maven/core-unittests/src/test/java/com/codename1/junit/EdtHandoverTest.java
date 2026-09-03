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
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code Display.init()} that lands while the previous dispatch thread is tearing down must
 * start a dispatch thread of its own.
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
 * <p>Reproduced here rather than raced for. The window is only open while a loaded machine
 * happens to be descheduling the old thread, which is why this never failed locally; an
 * implementation that blocks inside {@code deinitialize()} holds it open instead, so the test
 * either passes or fails for the reason it names.</p>
 */
class EdtHandoverTest {

    /** How long a working dispatch thread is given to run one runnable. */
    private static final long DISPATCH_TIMEOUT = 5000L;

    /**
     * An implementation that parks the departing dispatch thread inside its teardown.
     *
     * <p>{@code mainEDTLoop} calls this after leaving the loop and before it stops being alive,
     * so blocking here holds the thread in exactly the state the race produces, for as long as
     * the test needs it.</p>
     */
    private static final class BlockingDeinitImplementation extends TestCodenameOneImplementation {
        private final Object gate = new Object();
        private boolean entered;
        private boolean released;

        @Override
        public void deinitialize() {
            synchronized (gate) {
                entered = true;
                gate.notifyAll();
                long deadline = System.currentTimeMillis() + DISPATCH_TIMEOUT;
                // Bounded on purpose. A blocked EDT that is never released would hang the whole
                // suite rather than fail this one test, and the failure would then be reported
                // against whichever class the runner happened to reach.
                while (!released && System.currentTimeMillis() < deadline) {
                    try {
                        gate.wait(deadline - System.currentTimeMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            super.deinitialize();
        }

        boolean awaitEntered() {
            synchronized (gate) {
                long deadline = System.currentTimeMillis() + DISPATCH_TIMEOUT;
                while (!entered && System.currentTimeMillis() < deadline) {
                    try {
                        gate.wait(deadline - System.currentTimeMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return entered;
                    }
                }
                return entered;
            }
        }

        void release() {
            synchronized (gate) {
                released = true;
                gate.notifyAll();
            }
        }
    }

    @Test
    void anInitDuringTheOldEdtsTeardownStartsItsOwnDispatchThread() {
        BlockingDeinitImplementation blocking = new BlockingDeinitImplementation();
        BlockingDeinitImplementation parked = startGeneration(blocking);

        // Send the old generation into its teardown and wait until it is parked there: alive,
        // and no longer dispatching. This is the whole point of the fixture -- from here the
        // ordering is fixed rather than hoped for.
        Display.deinitialize();
        assertTrue(parked.awaitEntered(),
                "the dispatch thread never reached the teardown, so the window this test is "
                        + "about was never opened and what follows would prove nothing");

        try {
            install(new TestCodenameOneImplementation());
            Display.init(null);

            assertTrue(dispatchWorks(),
                    "init() adopted a thread that had already left the dispatch loop, so this "
                            + "generation has no event dispatch: everything it queues waits for "
                            + "ever, which is the display-not-initialized timeout CI reports");
        } finally {
            blocking.release();
        }
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
        assertTrue(blocking.awaitEntered(), "the fixture never parked the departing thread");

        install(new TestCodenameOneImplementation());
        Display.init(null);
        assertTrue(dispatchThreadKnowsItself(),
                "the successor has to be a recognised dispatch thread before it can be lost");

        // Let the old thread run the rest of its teardown, which is where it used to clear the
        // field the successor is recorded in.
        blocking.release();
        settle();

        assertTrue(dispatchThreadKnowsItself(),
                "the departing thread cleared the recorded dispatch thread on its way out, so "
                        + "isEdt() answers false ON the live dispatch thread and work meant for "
                        + "it is queued behind itself");
    }

    /**
     * Brings up a generation on {@code impl} and gets its dispatch thread into the dispatch loop.
     *
     * <p>The showing of a form is not decoration. {@code mainEDTLoop} spends its first phase in a
     * separate loop that runs until there is a current form, and only the loop AFTER that one has
     * a teardown -- so a thread with no form never reaches the state this test is about, and the
     * fixture silently proved nothing until it did.</p>
     */
    private static BlockingDeinitImplementation startGeneration(BlockingDeinitImplementation impl) {
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
        return impl;
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
        synchronized (done) {
            long deadline = System.currentTimeMillis() + DISPATCH_TIMEOUT;
            while (!ran[0] && System.currentTimeMillis() < deadline) {
                try {
                    done.wait(deadline - System.currentTimeMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return ran[0];
        }
    }

    /** Gives the released thread a moment to finish dying before the state is read again. */
    private static void settle() {
        long deadline = System.currentTimeMillis() + 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(25L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
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
