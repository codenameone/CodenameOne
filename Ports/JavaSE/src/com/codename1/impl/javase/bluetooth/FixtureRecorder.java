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
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattDescriptor;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.helper.BleBackend;
import com.codename1.bluetooth.le.AdvertisementData;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.CharacteristicRecord;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.DescriptorRecord;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.Device;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.RssiSample;
import com.codename1.impl.javase.bluetooth.BluetoothFixture.ServiceRecord;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Records a {@link BluetoothFixture} from a live {@link BleBackend} --
 * normally the {@link NativeBleBackend} driving this machine's real radio
 * through the {@code cn1-ble-helper} subprocess. Capture is entirely
 * Java-side over the existing helper protocol; nothing native is needed
 * beyond the helper binary itself.
 *
 * <p>{@link #record(long, List, boolean)} scans for the requested
 * duration, folding every sighting into per-device records (RSSI
 * timeline with relative timestamps, advertisement payloads), then
 * optionally connects to selected devices and captures their GATT
 * database plus every readable characteristic value. GATT failures on a
 * device (random-address peripherals routinely refuse connects) degrade
 * to a scan-only record for that device instead of failing the capture.
 * </p>
 *
 * <p>Adapter problems surface as typed {@link BluetoothException}s --
 * {@code UNAUTHORIZED} when the OS denied Bluetooth permission,
 * {@code POWERED_OFF}, {@code NOT_SUPPORTED} -- and every wait is
 * bounded, so a capture never hangs.</p>
 *
 * <p>Committed fixtures must be scrambled:
 * {@link #recordScrambled(long, List, boolean, long)} chains into
 * {@link FixtureScrambler}.</p>
 */
public final class FixtureRecorder {

    /** How long to wait for the adapter handshake before failing. */
    public static final long ADAPTER_TIMEOUT_MILLIS = 10000;

    /** Per-operation GATT timeout (connect/discover/read). */
    public static final long GATT_TIMEOUT_MILLIS = 15000;

    private final BleBackend backend;
    private final boolean ownsBackend;

    /** Records from an externally managed backend (not shut down here). */
    public FixtureRecorder(BleBackend backend) {
        this(backend, false);
    }

    private FixtureRecorder(BleBackend backend, boolean ownsBackend) {
        if (backend == null) {
            throw new IllegalArgumentException("backend is required");
        }
        this.backend = backend;
        this.ownsBackend = ownsBackend;
    }

    /**
     * Creates a recorder over a fresh {@link NativeBleBackend} (the real
     * radio); {@link #close()} shuts it down. Throws a typed exception
     * with the resolution trace when no helper binary exists for this
     * host.
     */
    public static FixtureRecorder forNativeBackend()
            throws BluetoothException {
        NativeBleBackend nativeBackend = new NativeBleBackend();
        if (!nativeBackend.isAvailable()) {
            throw new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "The cn1-ble-helper binary was not found; tried: "
                            + nativeBackend.describeResolution());
        }
        return new FixtureRecorder(nativeBackend, true);
    }

    /** Releases the backend when this recorder created it. */
    public void close() {
        if (ownsBackend) {
            backend.shutdown();
        }
    }

    /**
     * Records and scrambles in one step -- the form used for fixtures
     * that are going to be committed.
     */
    public BluetoothFixture recordScrambled(long scanMillis,
            List<String> gattAddresses, boolean gattStrongest, long seed)
            throws BluetoothException {
        return FixtureScrambler.scramble(
                record(scanMillis, gattAddresses, gattStrongest), seed);
    }

    /**
     * Scans for {@code scanMillis}, then attempts a GATT capture on every
     * sighted device in {@code gattAddresses} (may be {@code null}) plus
     * -- when {@code gattStrongest} -- the connectable device with the
     * strongest sighting. The returned fixture is UNSCRAMBLED: it carries
     * real identities and must go through {@link FixtureScrambler} before
     * leaving this machine.
     */
    public BluetoothFixture record(long scanMillis,
            List<String> gattAddresses, boolean gattStrongest)
            throws BluetoothException {
        awaitAdapterReady();

        final LinkedHashMap<String, Device> drafts =
                new LinkedHashMap<String, Device>();
        final AtomicReference<BluetoothException> scanFailure =
                new AtomicReference<BluetoothException>();
        final long start = System.currentTimeMillis();
        backend.startScan(new BleBackend.ScanSink() {
            public void onResult(ScanResult result) {
                recordSighting(drafts, result,
                        System.currentTimeMillis() - start);
            }

            public void onFailed(BluetoothException reason) {
                scanFailure.set(reason);
            }
        });
        try {
            long deadline = start + Math.max(0, scanMillis);
            while (System.currentTimeMillis() < deadline) {
                BluetoothException failure = scanFailure.get();
                if (failure != null) {
                    throw failure;
                }
                sleep(50);
            }
        } finally {
            backend.stopScan();
        }
        BluetoothException failure = scanFailure.get();
        if (failure != null) {
            throw failure;
        }

        BluetoothFixture fixture = new BluetoothFixture()
                .setPlatform(System.getProperty("os.name", "unknown") + " "
                        + System.getProperty("os.version", "")
                        + " (cn1-ble-helper)");
        ArrayList<Device> devices;
        synchronized (drafts) {
            devices = new ArrayList<Device>(drafts.values());
        }
        int size = devices.size();
        for (int i = 0; i < size; i++) {
            fixture.addDevice(devices.get(i));
        }

        ArrayList<String> targets = new ArrayList<String>();
        if (gattAddresses != null) {
            targets.addAll(gattAddresses);
        }
        if (gattStrongest) {
            String strongest = strongestConnectable(devices);
            if (strongest != null && !targets.contains(strongest)) {
                targets.add(strongest);
            }
        }
        int ts = targets.size();
        for (int i = 0; i < ts; i++) {
            String address = targets.get(i);
            Device device = fixture.getDevice(address);
            if (device == null) {
                System.err.println("FixtureRecorder: GATT capture skipped, "
                        + "device was never sighted: " + address);
                continue;
            }
            captureGatt(device);
        }
        return fixture;
    }

    /** Folds one scan sighting into the per-device draft. */
    private static void recordSighting(Map<String, Device> drafts,
            ScanResult result, long relTimeMs) {
        String address = result.getPeripheral().getAddress();
        AdvertisementData ad = result.getAdvertisementData();
        synchronized (drafts) {
            Device d = drafts.get(address);
            if (d == null) {
                d = new Device(address);
                drafts.put(address, d);
            }
            d.rssiTimeline.add(new RssiSample(Math.max(0, relTimeMs),
                    result.getRssi()));
            d.connectable = result.isConnectable();
            if (ad == null) {
                return;
            }
            if (ad.getLocalName() != null
                    && ad.getLocalName().length() > 0) {
                d.name = ad.getLocalName();
            }
            List<BluetoothUuid> uuids = ad.getServiceUuids();
            int size = uuids.size();
            for (int i = 0; i < size; i++) {
                if (!d.serviceUuids.contains(uuids.get(i))) {
                    d.serviceUuids.add(uuids.get(i));
                }
            }
            int[] companyIds = ad.getManufacturerIds();
            for (int i = 0; i < companyIds.length; i++) {
                byte[] payload = ad.getManufacturerData(companyIds[i]);
                if (payload != null) {
                    d.manufacturerData.put(Integer.valueOf(companyIds[i]),
                            payload);
                }
            }
            List<BluetoothUuid> dataUuids = ad.getServiceDataUuids();
            size = dataUuids.size();
            for (int i = 0; i < size; i++) {
                byte[] payload = ad.getServiceData(dataUuids.get(i));
                if (payload != null) {
                    d.serviceData.put(dataUuids.get(i), payload);
                }
            }
            if (ad.getTxPowerLevel() != null) {
                d.txPower = ad.getTxPowerLevel();
            }
        }
    }

    /** The connectable device with the strongest sighting, or null. */
    static String strongestConnectable(List<Device> devices) {
        String best = null;
        int bestRssi = Integer.MIN_VALUE;
        int size = devices.size();
        for (int i = 0; i < size; i++) {
            Device d = devices.get(i);
            if (!d.connectable) {
                continue;
            }
            int rs = d.rssiTimeline.size();
            for (int j = 0; j < rs; j++) {
                int rssi = d.rssiTimeline.get(j).rssi;
                if (rssi > bestRssi) {
                    bestRssi = rssi;
                    best = d.id;
                }
            }
        }
        return best;
    }

    /**
     * Connects, discovers and reads every readable characteristic of the
     * device, filling {@code device.gatt}. Returns whether the capture
     * succeeded; failures leave the device scan-only.
     */
    public boolean captureGatt(Device device) {
        BlePeripheral p = backend.getPeripheral(device.id);
        if (p == null) {
            System.err.println("FixtureRecorder: no peripheral handle for "
                    + device.id);
            return false;
        }
        try {
            await("connect " + device.id, p.connect());
            List<GattService> services = await("discover " + device.id,
                    p.discoverServices());
            ArrayList<ServiceRecord> records =
                    new ArrayList<ServiceRecord>();
            int size = services.size();
            for (int i = 0; i < size; i++) {
                GattService s = services.get(i);
                ServiceRecord sr = new ServiceRecord(s.getUuid());
                sr.primary = s.isPrimary();
                List<GattCharacteristic> chars = s.getCharacteristics();
                int cs = chars.size();
                for (int j = 0; j < cs; j++) {
                    GattCharacteristic c = chars.get(j);
                    CharacteristicRecord cr = new CharacteristicRecord(
                            c.getUuid(), c.getProperties());
                    List<GattDescriptor> descriptors = c.getDescriptors();
                    int ds = descriptors.size();
                    for (int k = 0; k < ds; k++) {
                        cr.descriptors.add(new DescriptorRecord(
                                descriptors.get(k).getUuid()));
                    }
                    if (c.canRead()) {
                        try {
                            cr.value = await("read " + c.getUuid(),
                                    c.read());
                        } catch (BluetoothException ex) {
                            // some readable characteristics reject reads
                            // (encryption required etc.) -- keep going
                            System.err.println("FixtureRecorder: read of "
                                    + c.getUuid() + " failed: "
                                    + ex.getMessage());
                        }
                    }
                    sr.characteristics.add(cr);
                }
                records.add(sr);
            }
            device.gatt.clear();
            device.gatt.addAll(records);
            return true;
        } catch (BluetoothException ex) {
            System.err.println("FixtureRecorder: GATT capture of "
                    + device.id + " failed: " + ex.getMessage());
            return false;
        } finally {
            try {
                p.disconnect();
            } catch (RuntimeException ignored) {
            }
        }
    }

    // ------------------------------------------------------------------
    // plumbing
    // ------------------------------------------------------------------

    /**
     * Boots the backend (installing an adapter sink is its activation
     * point) and waits -- bounded -- until the adapter reports a usable
     * state. Anything but {@code POWERED_ON} becomes a typed failure.
     */
    private void awaitAdapterReady() throws BluetoothException {
        backend.setAdapterStateSink(new BleBackend.AdapterStateSink() {
            public void adapterStateChanged(AdapterState newState) {
                // polling below reads getAdapterState(); the sink only
                // exists to activate the backend
            }
        });
        long deadline = System.currentTimeMillis() + ADAPTER_TIMEOUT_MILLIS;
        while (backend.getAdapterState() == AdapterState.UNKNOWN
                && System.currentTimeMillis() < deadline) {
            sleep(20);
        }
        AdapterState state = backend.getAdapterState();
        if (state == AdapterState.POWERED_ON) {
            return;
        }
        if (state == AdapterState.UNAUTHORIZED) {
            throw new BluetoothException(BluetoothError.UNAUTHORIZED,
                    "The OS denied Bluetooth access to this process -- on "
                            + "macOS grant it under System Settings > "
                            + "Privacy & Security > Bluetooth");
        }
        if (state == AdapterState.POWERED_OFF) {
            throw new BluetoothException(BluetoothError.POWERED_OFF,
                    "The Bluetooth adapter is powered off");
        }
        if (state == AdapterState.UNSUPPORTED) {
            throw new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "No usable Bluetooth adapter (or the helper could not "
                            + "access it)");
        }
        throw new BluetoothException(BluetoothError.IO_ERROR,
                "The Bluetooth helper never completed its adapter "
                        + "handshake within " + ADAPTER_TIMEOUT_MILLIS
                        + "ms");
    }

    /** Bounded blocking wait on an {@link AsyncResource}. */
    private static <T> T await(String what, AsyncResource<T> op)
            throws BluetoothException {
        long deadline = System.currentTimeMillis() + GATT_TIMEOUT_MILLIS;
        while (!op.isDone() && System.currentTimeMillis() < deadline) {
            sleep(20);
        }
        if (!op.isDone()) {
            throw new BluetoothException(BluetoothError.TIMEOUT,
                    "Timed out waiting for " + what);
        }
        final AtomicReference<Throwable> error =
                new AtomicReference<Throwable>();
        op.except(new com.codename1.util.SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                error.set(t);
            }
        });
        Throwable t = error.get();
        if (t instanceof BluetoothException) {
            throw (BluetoothException) t;
        }
        if (t != null) {
            throw new BluetoothException(BluetoothError.UNKNOWN,
                    what + " failed: " + t, t);
        }
        return op.get(null);
    }

    private static void sleep(long millis) throws BluetoothException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BluetoothException(BluetoothError.UNKNOWN,
                    "Fixture capture was interrupted");
        }
    }
}
