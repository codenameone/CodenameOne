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
package com.codename1.health;

import com.codename1.impl.health.LocalHealthStore;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every caller-facing store result arrives on the EDT, whatever the backend.
 *
 * <p>This used to depend on which store answered. The mobile ports complete on
 * the EDT and the result was handed back there; a local-backed store -- the
 * simulator, the desktop, the JavaScript port -- completed on whichever thread
 * had called, so the result arrived on that thread. The same app code updating
 * a label from a read callback therefore worked on a phone and produced a
 * repaint glitch on the desktop, and it was written down as a known asymmetry
 * rather than fixed.</p>
 *
 * <p>Each operation is started from a worker thread on purpose: with the caller
 * already on the EDT the old code delivered there by accident, so a test that
 * called from the EDT would have passed against the bug.</p>
 */
class HealthEdtDeliveryTest extends UITestBase {

    /** Where one delivery landed. */
    private static final class Landing<T> implements AsyncResult<T> {
        private final AtomicBoolean onEdt = new AtomicBoolean();
        private final AtomicBoolean arrived = new AtomicBoolean();
        private final AtomicBoolean failed = new AtomicBoolean();

        public void onReady(T value, Throwable err) {
            onEdt.set(CN.isEdt());
            failed.set(err != null);
            arrived.set(true);
        }
    }

    /**
     * Runs {@code op} off the EDT and waits for its delivery without blocking
     * the EDT.
     *
     * <p>Worth spelling out, because the obvious harness fails in a way that
     * looks like a product bug. These tests run on the EDT; the delivery being
     * asserted is a {@code callSerially}; so waiting on a latch from the test
     * thread stops the event loop and the runnable carrying the result can
     * never run. {@code invokeAndBlock} is the CN1 answer -- it moves the
     * waiting off the EDT and keeps the loop pumping. My first version of this
     * file used a plain latch and reported the aggregate path as broken when
     * it was not.</p>
     */
    private <T> void assertDeliveredOnEdt(final Landing<T> landing,
            final Runnable op) {
        assertDeliveredOnEdt(landing, op, false);
    }

    /// `mayFail` for the operations whose fallback answer is an error: what
    /// is under test is the thread it arrives on, not the outcome.
    private <T> void assertDeliveredOnEdt(final Landing<T> landing,
            final Runnable op, final boolean mayFail) {
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                assertFalse(CN.isEdt(), "the operation must start off the EDT");
                op.run();
                long deadline = System.currentTimeMillis() + 10_000L;
                while (!landing.arrived.get()
                        && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        });
        assertTrue(landing.arrived.get(), "the callback must arrive");
        if (!mayFail) {
            assertFalse(landing.failed.get(), "and must not be an error");
        }
        assertTrue(landing.onEdt.get(),
                "a result must arrive on the EDT on every backend");
    }

    @Test
    void aReadDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdRead = true;
        final Landing<List<HealthSample>> landing =
                new Landing<List<HealthSample>>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.readSamples(new SampleQuery()
                        .addType(HealthDataType.STEPS)
                        .setTimeRange(HealthTimeRange.between(0L, 1000L)))
                        .onResult(landing);
                store.heldRead.complete(new SamplePage(
                        new ArrayList<HealthSample>(), null, false));
            }
        });
    }

    @Test
    void aPagedReadDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdRead = true;
        final Landing<SamplePage> landing = new Landing<SamplePage>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.readSamplePage(new SampleQuery()
                        .addType(HealthDataType.STEPS)
                        .setTimeRange(HealthTimeRange.between(0L, 1000L)))
                        .onResult(landing);
                store.heldRead.complete(new SamplePage(
                        new ArrayList<HealthSample>(), null, false));
            }
        });
    }

    @Test
    void aWriteDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdWrite = true;
        final Landing<HealthWriteResult> landing =
                new Landing<HealthWriteResult>();
        final List<HealthSample> samples = new ArrayList<HealthSample>();
        samples.add(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(120, HealthUnit.COUNT), 0L, 60_000L));
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.write(samples).onResult(landing);
                store.lateWrite.complete(new HealthWriteResult());
            }
        });
    }

    /**
     * A port that answers off the EDT still reaches the caller on it.
     *
     * <p>Held open deliberately. The other tests here start an operation and
     * attach the listener afterwards, which is fine while the store is slower
     * than the attach -- but it is a race, and it is the race that made this
     * very test fail on CI and pass locally: when the EDT delivered first, the
     * resource was already settled and {@code onResult} then fired inline on
     * the attaching thread, recording "not the EDT" for a delivery that had in
     * fact happened there. Holding the port's completion until the listener is
     * attached removes the ordering question entirely.</p>
     */
    @Test
    void anAggregateDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdAggregate = true;
        final Landing<List<AggregateResult>> landing =
                new Landing<List<AggregateResult>>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.aggregate(new AggregateQuery()
                        .addType(HealthDataType.STEPS)
                        .addMetric(AggregateMetric.TOTAL)
                        .setTimeRange(HealthTimeRange.between(0L, 1000L)))
                        .onResult(landing);
                // Attached; now let the port answer, from this worker thread.
                store.heldAggregate.complete(
                        new ArrayList<AggregateResult>());
            }
        });
    }

    /**
     * The facade's own actions deliver on the EDT too.
     *
     * <p>These were missed when the store's results were moved onto
     * {@code EdtResult}: {@code openHealthSettings} and
     * {@code openProviderSetup} settle synchronously before the method
     * returns, so a callback attached afterwards ran immediately on whatever
     * thread called -- and a caller doing UI work in it, which is the whole
     * point of "did the settings screen open?", raced rendering.</p>
     *
     * <p>The fallback facade is the one under test here because it is the one
     * that settles inline; the port facades do the same thing through the
     * same resource type.</p>
     */
    /**
     * Every public health resource is an EDT-delivering one.
     *
     * <p>Written as a source scan rather than as one test per operation
     * because the defect kept coming back in a new place: the store's nine
     * resources moved to {@link com.codename1.impl.health.EdtResult} first,
     * then the facade's two openers were found still inline, then the
     * workout and sensor operations. Each round fixed the sites that had been
     * pointed at. This asserts the property over the whole surface, so the
     * next public resource that settles inline fails here rather than in
     * review.</p>
     *
     * <p>The internal resources stay plain on purpose and are listed by the
     * method that owns them: the base class does its own threading around
     * those, and hopping them would put the paging loop and the write
     * chunking back on the event loop.</p>
     */
    @Test
    void everyPublicHealthResourceDeliversOnTheEdt() throws Exception {
        // Owner method -> how many plain OneShots it is allowed to create.
        Map<String, Integer> allowedInternal = new HashMap<String, Integer>();
        allowedInternal.put("startAuthorization", 1);
        allowedInternal.put("readPageInto", 1);
        allowedInternal.put("readPage", 1);
        allowedInternal.put("writeChunk", 1);
        allowedInternal.put("drainChanges", 1);

        List<String> offenders = new ArrayList<String>();
        for (File f : healthSources(new File("../../CodenameOne/src/com/"
                + "codename1/health"))) {
            String src = read(f);
            String[] lines = src.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].indexOf("new OneShot<") < 0) {
                    continue;
                }
                String owner = enclosingMethod(lines, i);
                if (!allowedInternal.containsKey(owner)) {
                    offenders.add(f.getName() + ":" + (i + 1)
                            + " in " + owner);
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "these create a plain OneShot outside the methods allowed to"
                        + " own an internal one; a caller-facing result must"
                        + " be an EdtResult: " + offenders);
    }

    private static List<File> healthSources(File dir) {
        List<File> out = new ArrayList<File>();
        File[] kids = dir.listFiles();
        if (kids == null) {
            return out;
        }
        for (File k : kids) {
            if (k.isDirectory()) {
                out.addAll(healthSources(k));
            } else if (k.getName().endsWith(".java")) {
                out.add(k);
            }
        }
        return out;
    }

    private static String read(File f) throws Exception {
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream bo =
                    new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                bo.write(buf, 0, r);
            }
            return new String(bo.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /** The nearest declaration above `at`, by name. */
    private static String enclosingMethod(String[] lines, int at) {
        for (int i = at; i >= 0; i--) {
            String l = lines[i];
            if (l.startsWith("    public ") || l.startsWith("    private ")
                    || l.startsWith("    protected ")
                    || l.startsWith("    final ")) {
                int paren = l.indexOf('(');
                if (paren > 0) {
                    String head = l.substring(0, paren);
                    int sp = head.lastIndexOf(' ');
                    return sp > 0 ? head.substring(sp + 1) : head.trim();
                }
            }
        }
        return "<unknown>";
    }

    @Test
    void aFacadeActionDeliversOnTheEdt() {
        final Landing<Boolean> landing = new Landing<Boolean>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                Health.getInstance().openHealthSettings().onResult(landing);
            }
        }, true);
    }

    /// A listener attached AFTER the result settled still arrives on the EDT.
    ///
    /// This is the same guarantee as the tests above, at the one moment it used not to
    /// hold. `AsyncResource.ready` invokes the callback immediately, on the registering
    /// thread, when the resource is already done -- so EdtResult's contract covered only
    /// listeners attached before completion. The facade actions complete before
    /// returning, so a caller cannot attach in time, and which thread the callback ran
    /// on came down to whether the EDT had drained the hop yet: on the EDT on an idle
    /// machine, off it on a loaded one. `aFacadeActionDeliversOnTheEdt` therefore passed
    /// or failed by timing. This one settles the resource and waits for the hop to drain
    /// before attaching, so the late path is the only one it can take.
    @Test
    void aListenerAttachedAfterCompletionStillArrivesOnTheEdt() {
        final Landing<Boolean> landing = new Landing<Boolean>();
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                assertFalse(CN.isEdt(), "the operation must start off the EDT");
                AsyncResource<Boolean> settled = Health.getInstance().openHealthSettings();
                // Wait for the completion hop itself, so the registration below is
                // unambiguously late rather than merely probably late.
                long deadline = System.currentTimeMillis() + 10_000L;
                while (!settled.isDone() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
                assertTrue(settled.isDone(), "the fixture needs a settled resource");
                settled.onResult(landing);
                deadline = System.currentTimeMillis() + 10_000L;
                while (!landing.arrived.get() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        });
        assertTrue(landing.arrived.get(), "the callback must arrive");
        assertTrue(landing.onEdt.get(),
                "a result must arrive on the EDT even when the listener was attached "
                + "after it settled");
    }

    /// And a listener attached after a FAILURE arrives on the EDT too.
    ///
    /// `onResult` is `ready` followed by `except`, and only the first was marshalled --
    /// so this exact sequence, a worker registering on a resource that had already
    /// failed, ran the error half synchronously on that worker. An app handling a health
    /// error by showing a dialog or updating a label was then touching the UI off the
    /// EDT, which is what this class exists to prevent, reached through the other half of
    /// the same method.
    @Test
    void aListenerAttachedAfterAFailureAlsoArrivesOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdRead = true;
        final Landing<List<HealthSample>> landing =
                new Landing<List<HealthSample>>();
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                assertFalse(CN.isEdt(), "the operation must start off the EDT");
                AsyncResource<List<HealthSample>> failed = store.readSamples(
                        new SampleQuery()
                                .addType(HealthDataType.STEPS)
                                .setTimeRange(HealthTimeRange.between(0L, 1000L)));
                store.heldRead.error(new IllegalStateException("the backend said no"));
                long deadline = System.currentTimeMillis() + 10_000L;
                while (!failed.isDone() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
                assertTrue(failed.isDone(), "the fixture needs a settled failure");
                failed.onResult(landing);
                deadline = System.currentTimeMillis() + 10_000L;
                while (!landing.arrived.get() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        });
        assertTrue(landing.arrived.get(), "the callback must arrive");
        assertTrue(landing.failed.get(), "and it must be the error half");
        assertTrue(landing.onEdt.get(),
                "an error delivered to a late listener is still a callback an app "
                + "handles by touching the UI, so it belongs on the EDT");
    }

    @Test
    void aDeleteDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdDelete = true;
        final Landing<Integer> landing = new Landing<Integer>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.delete(HealthDeleteRequest.byRange(
                        HealthDataType.STEPS,
                        HealthTimeRange.between(0L, 1000L)))
                        .onResult(landing);
                store.heldDelete.complete(Integer.valueOf(0));
            }
        });
    }
}
