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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Background listeners are bound by a build-generated factory rather than by
 * resolving a class name reflectively. Reflection would be invisible to the
 * iOS and JavaScript translators' dead-code elimination and would break under
 * obfuscation -- on exactly the platform where background delivery is the
 * point. These cases pin the resulting contract.
 */
class BackgroundListenerFactoryTest extends UITestBase {

    /** Records deliveries so a test can see the listener actually ran. */
    public static class RecordingListener
            implements HealthBackgroundListener {
        static final List<HealthChangeBatch> DELIVERED =
                new ArrayList<HealthChangeBatch>();
        static CountDownLatch latch = new CountDownLatch(1);

        public void healthDataChanged(HealthChangeBatch batch) {
            DELIVERED.add(batch);
            latch.countDown();
        }
    }

    /** A store that lets a test drive fireChanges directly. */
    private static class DrivableStore extends LocalHealthStore {
        void deliver(HealthChangeBatch batch) {
            fireChanges(batch);
        }
    }

    @AfterEach
    void clearFactory() {
        HealthStore.setBackgroundListenerFactory(null);
        RecordingListener.DELIVERED.clear();
        RecordingListener.latch = new CountDownLatch(1);
    }

    private static HealthChangeBatch batchFor(String subscriptionId) {
        return new HealthChangeBatch(subscriptionId,
                new ArrayList<HealthDataType>(),
                new ArrayList<HealthSample>(), new ArrayList<String>(),
                false, HealthAnchor.of("anchor-1"), 5000L, false);
    }

    private static SubscriptionRequest request(String id) {
        return new SubscriptionRequest(id).addType(HealthDataType.STEPS);
    }

    /**
     * The generated factory is what binds a persisted subscription to code.
     * This is the shape the build server emits: a direct construction, keyed
     * on the source class name.
     */
    @Test
    void generatedFactoryResolvesTheListener() throws Exception {
        DrivableStore store = new DrivableStore();
        store.subscribe(request("steps-v1"), RecordingListener.class);

        final String expected = RecordingListener.class.getName();
        HealthStore.setBackgroundListenerFactory(
                className -> expected.equals(className)
                        ? new RecordingListener() : null);

        store.deliver(batchFor("steps-v1"));
        waitFor(RecordingListener.latch, 2000);

        assertEquals(1, RecordingListener.DELIVERED.size(),
                "the generated binding should have been used");
        store.unsubscribe("steps-v1");
    }

    /**
     * With no generated bindings -- the simulator, unit tests, or a build
     * predating the generator -- delivery is skipped rather than throwing.
     * Crucially the anchor is not advanced, so the data is redelivered later
     * rather than lost.
     */
    @Test
    void missingFactoryDefersDeliveryWithoutLosingData() throws Exception {
        DrivableStore store = new DrivableStore();
        store.subscribe(request("steps-v2"), RecordingListener.class);
        HealthStore.setBackgroundListenerFactory(null);

        store.deliver(batchFor("steps-v2"));
        drainEdt();

        assertEquals(0, RecordingListener.DELIVERED.size());
        assertNull(com.codename1.io.Preferences.get(
                        "cn1$health$anchor$steps-v2", null),
                "the cursor must not advance past data nobody received");
        store.unsubscribe("steps-v2");
    }

    /** A factory that does not know the class behaves the same way. */
    @Test
    void unknownClassDefersRatherThanFailing() throws Exception {
        DrivableStore store = new DrivableStore();
        store.subscribe(request("steps-v3"), RecordingListener.class);
        HealthStore.setBackgroundListenerFactory(className -> null);

        store.deliver(batchFor("steps-v3"));
        drainEdt();

        assertEquals(0, RecordingListener.DELIVERED.size());
        assertNull(com.codename1.io.Preferences.get(
                "cn1$health$anchor$steps-v3", null));
        store.unsubscribe("steps-v3");
    }

    /**
     * An in-memory listener needs no factory at all -- it is only the
     * after-process-death path that requires a generated binding.
     */
    @Test
    void inMemoryListenerNeedsNoFactory() throws Exception {
        DrivableStore store = new DrivableStore();
        final int[] delivered = new int[1];
        final CountDownLatch latch = new CountDownLatch(1);
        store.subscribe(request("steps-v4"),
                (HealthChangeListener) batch -> {
                    delivered[0]++;
                    latch.countDown();
                });

        store.deliver(batchFor("steps-v4"));
        waitFor(latch, 2000);

        assertEquals(1, delivered[0]);
        store.unsubscribe("steps-v4");
    }

    @Test
    void subscribingWithANonListenerClassIsRejectedEagerly() {
        DrivableStore store = new DrivableStore();
        assertThrows(IllegalArgumentException.class,
                () -> store.subscribe(request("bad"), String.class),
                "a class that cannot receive callbacks must be rejected at"
                        + " registration, not discovered weeks later when a"
                        + " background delivery silently does nothing");
    }

    /**
     * Pushes a task through the EDT and waits for it, so anything the store
     * queued before it has certainly run. Used by the negative cases, which
     * have no delivery of their own to wait on.
     */
    private void drainEdt() {
        CountDownLatch latch = new CountDownLatch(1);
        com.codename1.ui.Display.getInstance().callSerially(latch::countDown);
        waitFor(latch, 2000);
    }
}
