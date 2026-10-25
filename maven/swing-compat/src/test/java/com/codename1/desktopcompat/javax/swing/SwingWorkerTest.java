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

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The worker's life as an application sees it from the event dispatch
/// thread, which is where every test here runs.
public class SwingWorkerTest extends KernelTestBase {

    /// Keeps the event dispatch thread dispatching until `flag` is set.
    private static void pumpUntil(final AtomicBoolean flag) {
        final long deadline = System.currentTimeMillis() + 10000;
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (!flag.get() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
        assertTrue("timed out", flag.get());
    }

    private static final class Log implements PropertyChangeListener {
        final List<String> events = new ArrayList<String>();
        final List<Boolean> onEdt = new ArrayList<Boolean>();

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            events.add(e.getPropertyName() + ":" + e.getOldValue() + ">" + e.getNewValue());
            onEdt.add(Boolean.valueOf(Display.getInstance().isEdt()));
        }
    }

    @Test
    public void stateAndProgressEventsArriveInOrderOnTheEventDispatchThread() throws Exception {
        final AtomicBoolean finished = new AtomicBoolean();
        final boolean[] backgroundOnEdt = {true};
        final boolean[] doneOnEdt = {false};
        final List<String> order = new ArrayList<String>();
        final Log log = new Log();
        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                backgroundOnEdt[0] = Display.getInstance().isEdt();
                setProgress(40);
                setProgress(40);
                setProgress(100);
                return "answer";
            }

            @Override
            protected void done() {
                doneOnEdt[0] = Display.getInstance().isEdt();
                order.addAll(log.events);
                finished.set(true);
            }
        };
        w.addPropertyChangeListener(log);
        assertSame(SwingWorker.StateValue.PENDING, w.getState());
        assertFalse(w.isDone());
        w.execute();
        w.execute();
        pumpUntil(finished);
        assertFalse(backgroundOnEdt[0]);
        assertTrue(doneOnEdt[0]);
        List<String> expected = new ArrayList<String>();
        expected.add("state:PENDING>STARTED");
        expected.add("progress:0>40");
        expected.add("progress:40>100");
        expected.add("state:STARTED>DONE");
        assertEquals(expected, log.events);
        assertEquals("done() runs after the events", expected, order);
        assertFalse(log.onEdt.contains(Boolean.FALSE));
        assertSame(SwingWorker.StateValue.DONE, w.getState());
        assertTrue(w.isDone());
        assertFalse(w.isCancelled());
        assertEquals(100, w.getProgress());
        assertEquals("answer", w.get());
        assertEquals("answer", w.get(1, TimeUnit.MILLISECONDS));
        assertFalse(w.cancel(true));
    }

    @Test
    public void progressOutsideZeroToOneHundredIsRefused() {
        SwingWorker<Void, Void> w = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                return null;
            }
        };
        try {
            w.setProgress(101);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(0, w.getProgress());
        }
    }

    @Test
    public void chunksPublishedBeforeTheEventThreadRunsArriveAsOneList() {
        final AtomicBoolean finished = new AtomicBoolean();
        final List<List<Integer>> calls = new ArrayList<List<Integer>>();
        final boolean[] processOnEdt = {true};
        SwingWorker<Void, Integer> w = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                processOnEdt[0] &= Display.getInstance().isEdt();
                calls.add(new ArrayList<Integer>(chunks));
            }

            @Override
            protected void done() {
                finished.set(true);
            }
        };
        // Published on the event dispatch thread, which cannot run the
        // flush before this method lets go of it.
        w.publish(1, 2);
        w.publish(3);
        w.publish();
        assertTrue(calls.isEmpty());
        w.execute();
        pumpUntil(finished);
        assertEquals(1, calls.size());
        List<Integer> all = new ArrayList<Integer>();
        all.add(1);
        all.add(2);
        all.add(3);
        assertEquals(all, calls.get(0));
        assertTrue(processOnEdt[0]);
    }

    @Test
    public void chunksPublishedInTheBackgroundAllArriveInOrderBeforeDone() {
        final AtomicBoolean finished = new AtomicBoolean();
        final List<Integer> seen = new ArrayList<Integer>();
        final int[] atDone = {-1};
        SwingWorker<Void, Integer> w = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() {
                for (int i = 0; i < 200; i++) {
                    publish(i);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                seen.addAll(chunks);
            }

            @Override
            protected void done() {
                atDone[0] = seen.size();
                finished.set(true);
            }
        };
        w.execute();
        pumpUntil(finished);
        assertEquals(200, atDone[0]);
        for (int i = 0; i < 200; i++) {
            assertEquals(i, seen.get(i).intValue());
        }
    }

    @Test
    public void getOnTheEventDispatchThreadWaitsWhileThatThreadKeepsDispatching() throws Exception {
        final AtomicBoolean release = new AtomicBoolean();
        SwingWorker<Integer, Void> w = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                while (!release.get()) {
                    Thread.sleep(2);
                }
                return 42;
            }
        };
        w.execute();
        try {
            w.get(20, TimeUnit.MILLISECONDS);
            fail();
        } catch (TimeoutException expected) {
            assertFalse(w.isDone());
        }
        // Only the event dispatch thread releases the task, so get() can
        // return only if that thread still runs what is posted to it.
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                release.set(true);
            }
        });
        assertEquals(42, w.get().intValue());
        assertTrue(w.isDone());
    }

    @Test
    public void anExceptionInTheBackgroundSurfacesFromGet() throws Exception {
        final AtomicBoolean finished = new AtomicBoolean();
        final IllegalStateException boom = new IllegalStateException("boom");
        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                throw boom;
            }

            @Override
            protected void done() {
                finished.set(true);
            }
        };
        w.execute();
        try {
            w.get();
            fail();
        } catch (ExecutionException e) {
            assertSame(boom, e.getCause());
        }
        pumpUntil(finished);
        assertSame(SwingWorker.StateValue.DONE, w.getState());
        assertFalse(w.isCancelled());
    }

    @Test
    public void cancelInterruptsTheTaskAndGetThrows() throws Exception {
        final AtomicBoolean started = new AtomicBoolean();
        final AtomicBoolean interrupted = new AtomicBoolean();
        final int[] doneCalls = {0};
        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                started.set(true);
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
                return "late";
            }

            @Override
            protected void done() {
                doneCalls[0]++;
            }
        };
        w.execute();
        pumpUntil(started);
        assertTrue(w.cancel(true));
        assertEquals("done() runs at once on the event dispatch thread", 1, doneCalls[0]);
        assertTrue(w.isCancelled());
        assertTrue(w.isDone());
        assertSame(SwingWorker.StateValue.DONE, w.getState());
        try {
            w.get();
            fail();
        } catch (CancellationException expected) {
            assertFalse(w.cancel(true));
        }
        pumpUntil(interrupted);
        assertEquals(1, doneCalls[0]);
    }

    @Test
    public void aWorkerCancelledBeforeItStartsNeverRuns() throws Exception {
        final boolean[] ran = {false};
        final Log log = new Log();
        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                ran[0] = true;
                return "x";
            }
        };
        w.addPropertyChangeListener(log);
        assertTrue(w.cancel(false));
        w.run();
        assertFalse(ran[0]);
        assertEquals(1, log.events.size());
        assertEquals("state:PENDING>DONE", log.events.get(0));
        try {
            w.get(1, TimeUnit.SECONDS);
            fail();
        } catch (CancellationException expected) {
            assertTrue(w.isCancelled());
        }
    }
}
