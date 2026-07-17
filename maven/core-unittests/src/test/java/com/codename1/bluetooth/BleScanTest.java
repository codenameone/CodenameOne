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
package com.codename1.bluetooth;

import com.codename1.bluetooth.le.AdvertisementData;
import com.codename1.bluetooth.le.BleScan;
import com.codename1.bluetooth.le.ScanFilter;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.ScanSettings;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.codename1.bluetooth.BtTestUtil.assertFailedWith;
import static com.codename1.bluetooth.BtTestUtil.bytes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The multi-handle scan registry of {@link com.codename1.bluetooth.le.BluetoothLE}:
 * several concurrent scans multiplexed over one platform scan, per-handle
 * filtering and duplicate suppression, last-stop-stops-platform-scan, scan
 * failure fan-out, and the {@link ScanFilter} matching matrix. All results
 * are injected synchronously through the fake and only listener dispatch
 * crosses onto the EDT, drained via {@code flushSerialCalls()}.
 */
class BleScanTest extends UITestBase {

    private static final BluetoothUuid HEART_RATE =
            BluetoothUuid.fromShort(0x180D);
    private static final BluetoothUuid BATTERY =
            BluetoothUuid.fromShort(0x180F);

    private FakeBluetooth fake;
    private FakeBluetoothLE le;

    @BeforeEach
    void installFake() {
        fake = new FakeBluetooth();
        implementation.setBluetooth(fake);
        le = fake.getFakeLE();
    }

    private static ScanResult sighting(String address, String name,
            BluetoothUuid advertisedService) {
        AdvertisementData ad = new AdvertisementData();
        if (name != null) {
            ad.setLocalName(name);
        }
        if (advertisedService != null) {
            ad.addServiceUuid(advertisedService);
        }
        return new ScanResult(new FakeBlePeripheral(address, name), -42, ad,
                true, 1000L);
    }

    @Test
    void installedFakeIsReturnedByTheFacade() {
        assertSame(fake, Bluetooth.getInstance());
        assertSame(le, Bluetooth.getInstance().getLE());
    }

    @Test
    void twoScansWithDifferentFiltersShareOnePlatformScanAndOnlySeeMatches() {
        List<ScanResult> seen1 = new ArrayList<ScanResult>();
        List<ScanResult> seen2 = new ArrayList<ScanResult>();
        BleScan s1 = le.startScan(new ScanSettings().addFilter(
                new ScanFilter().setServiceUuid(HEART_RATE)), seen1::add);
        BleScan s2 = le.startScan(new ScanSettings().addFilter(
                new ScanFilter().setServiceUuid(BATTERY)), seen2::add);
        assertTrue(s1.isActive());
        assertTrue(s2.isActive());
        assertEquals(1, le.getStartCount(),
                "both handles must share a single platform scan");
        assertTrue(le.isPlatformScanActive());

        le.injectScanResult(sighting("AA:00", "hrm", HEART_RATE));
        le.injectScanResult(sighting("BB:00", "batt", BATTERY));
        flushSerialCalls();

        assertEquals(1, seen1.size());
        assertEquals("AA:00", seen1.get(0).getPeripheral().getAddress());
        assertEquals(1, seen2.size());
        assertEquals("BB:00", seen2.get(0).getPeripheral().getAddress());

        s1.stop();
        s2.stop();
    }

    @Test
    void duplicateSuppressionIsPerHandle() {
        List<ScanResult> dedup = new ArrayList<ScanResult>();
        List<ScanResult> all = new ArrayList<ScanResult>();
        BleScan s1 = le.startScan(new ScanSettings(), dedup::add);
        BleScan s2 = le.startScan(
                new ScanSettings().setAllowDuplicates(true), all::add);

        le.injectScanResult(sighting("AA:00", "hrm", HEART_RATE));
        le.injectScanResult(sighting("AA:00", "hrm", HEART_RATE));
        flushSerialCalls();

        assertEquals(1, dedup.size(),
                "allowDuplicates=false must report each device once");
        assertEquals(2, all.size(),
                "allowDuplicates=true must report every sighting");

        s1.stop();
        s2.stop();
    }

    @Test
    void stoppingOneHandleKeepsThePlatformScanRunningUntilTheLastStops() {
        BleScan s1 = le.startScan(new ScanSettings(), result -> { });
        BleScan s2 = le.startScan(new ScanSettings(), result -> { });
        assertEquals(1, le.getStartCount());

        s1.stop();
        assertTrue(le.isPlatformScanActive(),
                "platform scan must survive while another handle is active");
        assertEquals(0, le.getStopCount());
        assertFalse(s1.isActive());
        assertTrue(s1.isDone());
        assertEquals(Boolean.TRUE, s1.get(),
                "a stopped handle resolves with true");

        s2.stop();
        assertFalse(le.isPlatformScanActive());
        assertEquals(1, le.getStopCount());
        assertEquals(Boolean.TRUE, s2.get());

        // stop on an ended handle is a no-op
        s2.stop();
        assertEquals(1, le.getStopCount());
    }

    @Test
    void stoppedHandleNoLongerReceivesResults() {
        List<ScanResult> seen1 = new ArrayList<ScanResult>();
        List<ScanResult> seen2 = new ArrayList<ScanResult>();
        BleScan s1 = le.startScan(
                new ScanSettings().setAllowDuplicates(true), seen1::add);
        BleScan s2 = le.startScan(
                new ScanSettings().setAllowDuplicates(true), seen2::add);

        le.injectScanResult(sighting("AA:00", "a", null));
        s1.stop();
        le.injectScanResult(sighting("AA:00", "a", null));
        flushSerialCalls();

        assertEquals(1, seen1.size());
        assertEquals(2, seen2.size());
        s2.stop();
    }

    @Test
    void scanFailureErrorsEveryActiveHandleAndClearsTheRegistry() {
        BleScan s1 = le.startScan(new ScanSettings(), result -> { });
        BleScan s2 = le.startScan(new ScanSettings(), result -> { });

        le.failScan(new BluetoothException(BluetoothError.SCAN_FAILED,
                "OS aborted"));

        assertFailedWith(s1, BluetoothError.SCAN_FAILED);
        assertFailedWith(s2, BluetoothError.SCAN_FAILED);

        // the registry was cleared: the next scan starts a fresh platform scan
        BleScan s3 = le.startScan(new ScanSettings(), result -> { });
        assertEquals(2, le.getStartCount());
        s3.stop();
    }

    @Test
    void scanFailureWithNullReasonSubstitutesScanFailed() {
        BleScan s1 = le.startScan(new ScanSettings(), result -> { });
        le.failScan(null);
        assertFailedWith(s1, BluetoothError.SCAN_FAILED);
    }

    @Test
    void startScanWithoutScanSupportFailsNotSupported() {
        le.setScanSupported(false);
        BleScan s = le.startScan(new ScanSettings(), result -> { });
        assertFailedWith(s, BluetoothError.NOT_SUPPORTED);
        assertEquals(0, le.getStartCount());
    }

    @Test
    void platformStartFailureFailsTheInitiatingHandle() {
        le.failNextStart(new IllegalStateException("adapter busy"));
        BleScan s = le.startScan(new ScanSettings(), result -> { });
        assertFailedWith(s, BluetoothError.SCAN_FAILED);
        // the failed registration was removed: a new scan starts cleanly
        BleScan s2 = le.startScan(new ScanSettings(), result -> { });
        assertTrue(s2.isActive());
        assertEquals(1, le.getStartCount());
        s2.stop();
    }

    // ------------------------------------------------------------------
    // ScanFilter matching matrix
    // ------------------------------------------------------------------

    @Test
    void filterMatchesExactName() {
        ScanResult r = sighting("AA:00", "Polar H10", null);
        assertTrue(new ScanFilter().setName("Polar H10").matches(r));
        assertFalse(new ScanFilter().setName("Polar H9").matches(r));
    }

    @Test
    void filterNameFallsBackToPeripheralNameWhenAdvertisementHasNone() {
        ScanResult r = new ScanResult(
                new FakeBlePeripheral("AA:00", "Polar H10"), -40,
                new AdvertisementData(), true, 0);
        assertTrue(new ScanFilter().setName("Polar H10").matches(r));
        assertTrue(new ScanFilter().setNamePrefix("Polar").matches(r));
    }

    @Test
    void filterMatchesNamePrefix() {
        ScanResult r = sighting("AA:00", "Polar H10", null);
        assertTrue(new ScanFilter().setNamePrefix("Polar").matches(r));
        assertFalse(new ScanFilter().setNamePrefix("Garmin").matches(r));
        assertFalse(new ScanFilter().setNamePrefix("Polar").matches(
                sighting("BB:00", null, null)));
    }

    @Test
    void filterMatchesAddress() {
        ScanResult r = sighting("AA:00", "x", null);
        assertTrue(new ScanFilter().setAddress("AA:00").matches(r));
        assertFalse(new ScanFilter().setAddress("BB:00").matches(r));
    }

    @Test
    void filterMatchesServiceUuid() {
        ScanResult r = sighting("AA:00", "x", HEART_RATE);
        assertTrue(new ScanFilter().setServiceUuid(HEART_RATE).matches(r));
        assertFalse(new ScanFilter().setServiceUuid(BATTERY).matches(r));
    }

    @Test
    void filterMatchesManufacturerDataUnderMask() {
        AdvertisementData ad = new AdvertisementData();
        ad.addManufacturerData(0x004C, bytes(0x01, 0x99, 0x55));
        ScanResult r = new ScanResult(new FakeBlePeripheral("AA:00", "x"),
                -40, ad, true, 0);

        // masked compare: second byte ignored
        assertTrue(new ScanFilter().setManufacturerData(0x004C,
                bytes(0x01, 0x02), bytes(0xFF, 0x00)).matches(r));
        // exact compare (null mask) fails on the second byte
        assertFalse(new ScanFilter().setManufacturerData(0x004C,
                bytes(0x01, 0x02), null).matches(r));
        // exact compare succeeds on the true prefix
        assertTrue(new ScanFilter().setManufacturerData(0x004C,
                bytes(0x01, 0x99), null).matches(r));
        // null data matches any payload for the company
        assertTrue(new ScanFilter().setManufacturerData(0x004C, null, null)
                .matches(r));
        // company absent entirely
        assertFalse(new ScanFilter().setManufacturerData(0x0059, null, null)
                .matches(r));
        // pattern longer than the payload
        assertFalse(new ScanFilter().setManufacturerData(0x004C,
                bytes(0x01, 0x99, 0x55, 0x77), null).matches(r));
    }

    @Test
    void criteriaOnOneFilterAreAndCombined() {
        ScanFilter f = new ScanFilter().setNamePrefix("Polar")
                .setServiceUuid(HEART_RATE);
        assertTrue(f.matches(sighting("AA:00", "Polar H10", HEART_RATE)));
        assertFalse(f.matches(sighting("AA:00", "Polar H10", BATTERY)));
        assertFalse(f.matches(sighting("AA:00", "Garmin", HEART_RATE)));
    }

    @Test
    void multipleFiltersOnSettingsAreOrCombined() {
        ScanSettings settings = new ScanSettings()
                .addFilter(new ScanFilter().setServiceUuid(HEART_RATE))
                .addFilter(new ScanFilter().setServiceUuid(BATTERY));
        assertTrue(settings.matches(sighting("AA:00", "a", HEART_RATE)));
        assertTrue(settings.matches(sighting("BB:00", "b", BATTERY)));
        assertFalse(settings.matches(sighting("CC:00", "c",
                BluetoothUuid.fromShort(0x1800))));
    }

    @Test
    void settingsWithoutFiltersMatchEverything() {
        assertTrue(new ScanSettings().matches(
                sighting("AA:00", null, null)));
    }
}
