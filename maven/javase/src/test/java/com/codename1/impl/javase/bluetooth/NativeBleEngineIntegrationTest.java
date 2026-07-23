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
package com.codename1.impl.javase.bluetooth;

import com.codename1.bluetooth.AdapterState;
import com.codename1.impl.bluetooth.BleBackend;
import com.codename1.impl.bluetooth.NativeBleBackend;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end exercise of the REAL native Bluetooth stack: the bundled
 * {@code libcn1ble} shared library is loaded through JNI and driven via
 * {@link NativeBleBackend} over the actual {@link JniBleBridge} -- no mock,
 * no fake bridge. It verifies the whole native round-trip
 * (JNI -&gt; Rust engine -&gt; event pump -&gt; JSON decode -&gt; backend): the
 * engine loads and its startup {@code capabilities} handshake round-trips
 * back through the native boundary.
 *
 * <p>This is deliberately hardware-independent. A CI runner has no paired
 * peripheral -- and often no radio at all -- but the engine always emits its
 * {@code capabilities} handshake first, so that real round-trip is exercised
 * regardless of the host; a concrete adapter state (POWERED_ON with a radio,
 * otherwise UNSUPPORTED) is additionally checked when it arrives. The library
 * MUST be present and loadable -- if it is not, that is a packaging failure
 * and the test fails rather than skipping. The full command/GATT flow is
 * covered deterministically by {@link NativeBleScriptedEngineTest}.</p>
 */
public class NativeBleEngineIntegrationTest {

    private static final long TIMEOUT_MILLIS = 15000;

    @Test
    public void nativeEngineLoadsAndHandshakeRoundTrips()
            throws InterruptedException {
        // 1. the real shared library must load through JNI -- no skip
        assertTrue(JniBleBridge.isLibraryAvailable(),
                "libcn1ble must be bundled and loadable for the native "
                        + "backend; resolution trace: "
                        + JniBleBridge.describeResolution());

        NativeBleBackend backend = new NativeBleBackend(new JniBleBridge());
        final AtomicReference<AdapterState> state =
                new AtomicReference<AdapterState>();
        final CountDownLatch stateLatch = new CountDownLatch(1);
        try {
            // 2. installing the sink boots the real engine. It emits its
            //    capabilities handshake first (always), then an adapter-state
            //    event -- POWERED_ON with a radio, otherwise UNSUPPORTED. A CI
            //    runner has no radio, so assert the deterministic part: the
            //    capabilities handshake round-tripped through the real native
            //    boundary (JNI -> Rust engine -> JSON -> backend), which is
            //    what populates engineSupports(). The adapter state is
            //    captured best-effort and, when it arrives, must be concrete.
            //    (The full command/GATT flow is covered deterministically by
            //    NativeBleScriptedEngineTest.)
            backend.setAdapterStateSink(new BleBackend.AdapterStateSink() {
                @Override
                public void adapterStateChanged(AdapterState newState) {
                    state.set(newState);
                    stateLatch.countDown();
                }
            });
            long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
            while (System.currentTimeMillis() < deadline
                    && !backend.engineSupports("descriptors")
                    && state.get() == null) {
                Thread.sleep(30);
            }
            assertTrue(
                    backend.engineSupports("descriptors") || state.get() != null,
                    "the native engine's handshake did not round-trip through "
                            + "JNI within " + TIMEOUT_MILLIS + "ms");
            AdapterState reported = state.get();
            if (reported != null) {
                assertNotEquals(AdapterState.UNKNOWN, reported,
                        "engine reported a non-concrete adapter state");
            }
            assertTrue(backend.isLeSupported());
        } finally {
            backend.shutdown();
        }
    }
}
