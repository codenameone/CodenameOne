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
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.helper.BleBackend;
import com.codename1.bluetooth.le.ScanResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manual smoke test against the REAL cn1-ble-helper binary and this
 * machine's radio. Disabled by default (hardware + OS Bluetooth permission
 * required); run explicitly with:
 *
 * <pre>
 * mvn test -pl javase -Plocal-dev-javase \
 *   -Dtest=NativeBleSmokeTest -Dcn1.ble.smoke=true \
 *   -Dcn1.bluetooth.helperPath=&lt;path-to&gt;/cn1-ble-helper
 * </pre>
 *
 * Without {@code cn1.bluetooth.helperPath} the test looks for the local
 * cargo build at {@code Ports/JavaSE/native/cn1-ble-helper/target/release}.
 *
 * <p>The test never fails for environmental reasons -- an adapter that
 * reports unsupported/unauthorized (headless CI, missing TCC grant on
 * macOS) or zero sightings are reported, not asserted. Only a broken
 * protocol handshake fails it.</p>
 */
public class NativeBleSmokeTest {

    @Test
    @EnabledIfSystemProperty(named = "cn1.ble.smoke", matches = "true")
    public void scanTheRealRadioForThreeSeconds() throws Exception {
        String helperPath = System.getProperty(
                HelperBinaryResolver.HELPER_PATH_PROPERTY);
        if (helperPath == null) {
            helperPath = new File(
                    "../../Ports/JavaSE/native/cn1-ble-helper/target/"
                            + "release/cn1-ble-helper").getAbsolutePath();
        }
        File helper = new File(helperPath);
        Assumptions.assumeTrue(helper.isFile(),
                "no helper binary at " + helper.getAbsolutePath()
                        + " -- build it with cargo or pass -D"
                        + HelperBinaryResolver.HELPER_PATH_PROPERTY);

        NativeBleBackend backend = new NativeBleBackend(
                Arrays.asList(helper.getAbsolutePath()));
        try {
            backend.setAdapterStateSink(new BleBackend.AdapterStateSink() {
                public void adapterStateChanged(AdapterState newState) {
                    System.out.println("[ble-smoke] adapter state: "
                            + newState);
                }
            });
            long deadline = System.currentTimeMillis() + 10000;
            while (backend.getAdapterState() == AdapterState.UNKNOWN
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            AdapterState state = backend.getAdapterState();
            Assertions.assertNotEquals(AdapterState.UNKNOWN, state,
                    "helper never completed the stateChanged handshake");
            System.out.println("[ble-smoke] adapter: " + state
                    + ", helper descriptors="
                    + backend.helperSupports("descriptors"));
            if (state != AdapterState.POWERED_ON) {
                System.out.println("[ble-smoke] adapter not powered on -- "
                        + "skipping the scan (likely missing hardware or "
                        + "OS Bluetooth permission)");
                return;
            }
            final CopyOnWriteArrayList<ScanResult> sightings =
                    new CopyOnWriteArrayList<ScanResult>();
            backend.startScan(new BleBackend.ScanSink() {
                public void onResult(ScanResult result) {
                    sightings.add(result);
                }

                public void onFailed(BluetoothException reason) {
                    System.out.println("[ble-smoke] scan failed: "
                            + reason);
                }
            });
            Thread.sleep(3000);
            backend.stopScan();
            java.util.HashSet<String> unique = new java.util.HashSet<>();
            for (ScanResult r : sightings) {
                unique.add(r.getPeripheral().getAddress());
            }
            System.out.println("[ble-smoke] " + sightings.size()
                    + " sightings of " + unique.size()
                    + " distinct peripherals in 3s");
        } finally {
            backend.shutdown();
        }
    }
}
