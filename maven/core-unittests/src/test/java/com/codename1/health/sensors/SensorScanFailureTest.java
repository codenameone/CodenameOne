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
package com.codename1.health.sensors;

import com.codename1.bluetooth.FakeBluetooth;
import com.codename1.health.Health;
import com.codename1.health.HealthException;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A sensor scan that cannot start has to say so.
 *
 * <p>The failure arrives on the {@code BleScan} the health layer keeps to
 * itself -- callers are handed a {@code SensorScan}, which deliberately
 * does not expose it -- so without forwarding, a scan that never started
 * looked exactly like one that started and found nothing.</p>
 */
class SensorScanFailureTest extends UITestBase {

    private FakeBluetooth fake;

    @BeforeEach
    void installFake() {
        fake = new FakeBluetooth();
        implementation.setBluetooth(fake);
    }

    @AfterEach
    void removeFake() {
        implementation.setBluetooth(null);
    }

    /** Collects whatever the discovery listener is told. */
    private static final class Recorder
            implements SensorDiscoveryListener {

        private HealthException failure;
        private int discovered;

        public void sensorDiscovered(HealthSensor sensor) {
            discovered++;
        }

        public void scanFailed(HealthException error) {
            failure = error;
        }
    }

    @Test
    void aScanThatCannotStartReportsItToTheListener() {
        fake.getFakeLE().failNextStart(
                new IllegalStateException("adapter busy"));
        Recorder recorder = new Recorder();

        SensorScan scan = Health.getInstance().getSensors()
                .startScan(new SensorScanSettings(), recorder);
        flushSerialCalls();

        assertNotNull(scan);
        assertNotNull(recorder.failure,
                "a scan that never started must reach scanFailed");
        assertFalse(scan.isScanning(),
                "and it must not report itself as running");
    }

    @Test
    void aScanThatStartsReportsNoFailure() {
        Recorder recorder = new Recorder();

        SensorScan scan = Health.getInstance().getSensors()
                .startScan(new SensorScanSettings(), recorder);
        flushSerialCalls();

        assertNull(recorder.failure);
        assertTrue(scan.isScanning());
        scan.stop();
    }
}
