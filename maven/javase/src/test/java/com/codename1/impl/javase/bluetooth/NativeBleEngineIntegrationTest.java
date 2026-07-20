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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end exercise of the REAL native Bluetooth stack: the bundled
 * {@code libcn1ble} shared library is loaded through JNI and driven via
 * {@link NativeBleBackend} over the actual {@link JniBleBridge} -- no mock,
 * no fake bridge. It verifies the whole native round-trip
 * (JNI -&gt; Rust engine -&gt; event pump -&gt; JSON decode -&gt; backend):
 * the engine loads, its startup handshake produces a real adapter state, and
 * a scan command round-trips back through the native boundary.
 *
 * <p>This is deliberately hardware-independent. A CI runner has no paired
 * peripheral -- and often no radio at all -- but the engine always emits its
 * {@code capabilities} handshake and a terminal adapter state (POWERED_ON
 * with a radio and permission, otherwise UNSUPPORTED / UNAUTHORIZED /
 * POWERED_OFF), so the native path is exercised for real regardless of the
 * host. The library MUST be present and loadable -- if it is not, that is a
 * packaging failure and the test fails rather than skipping.</p>
 */
public class NativeBleEngineIntegrationTest {

    private static final long TIMEOUT_MILLIS = 15000;

    @Test
    public void nativeEngineLoadsAndReportsAdapterStateAndScanRoundTrips()
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
            // 2. installing the sink boots the engine; a real adapter-state
            //    event must come back through the native round-trip
            backend.setAdapterStateSink(new BleBackend.AdapterStateSink() {
                @Override
                public void adapterStateChanged(AdapterState newState) {
                    state.set(newState);
                    stateLatch.countDown();
                }
            });
            assertTrue(stateLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS),
                    "the native engine did not report an adapter state "
                            + "within " + TIMEOUT_MILLIS + "ms");
            AdapterState reported = state.get();
            assertNotNull(reported);
            // any concrete state proves the JNI -> Rust -> event -> JSON ->
            // backend chain ran; UNKNOWN would mean the handshake never
            // decoded
            assertNotEquals(AdapterState.UNKNOWN, reported,
                    "engine reported no concrete adapter state -- the native "
                            + "handshake did not round-trip");
            assertTrue(backend.isLeSupported());

            // 3. a scan command must round-trip through the native boundary:
            //    it either starts (scanStarted) or fails typed (e.g.
            //    scanFailed on a radioless CI host) -- never hangs
            final CountDownLatch scanLatch = new CountDownLatch(1);
            backend.startScan(new BleBackend.ScanSink() {
                @Override
                public void onResult(
                        com.codename1.bluetooth.le.ScanResult result) {
                    scanLatch.countDown(); // a real sighting: also fine
                }

                @Override
                public void onFailed(
                        com.codename1.bluetooth.BluetoothException reason) {
                    scanLatch.countDown(); // radioless host: expected path
                }
            });
            // On a host with a powered-on radio the scan simply runs (no
            // terminal callback), so only assert a bounded outcome when the
            // adapter is not usable; either way the command reached native.
            if (reported != AdapterState.POWERED_ON) {
                assertTrue(
                        scanLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS),
                        "scan command did not round-trip through the native "
                                + "engine within " + TIMEOUT_MILLIS + "ms");
            }
            backend.stopScan();
        } finally {
            backend.shutdown();
        }
    }
}
