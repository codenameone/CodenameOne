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
package com.codename1.bluetooth.le;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.server.AdvertiseData;
import com.codename1.bluetooth.le.server.AdvertiseSettings;
import com.codename1.bluetooth.le.server.BleAdvertisement;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/// The BLE central role: scanning for peripherals and re-obtaining known
/// ones. Obtain via `Bluetooth.getInstance().getLE()`; the instance is
/// owned by the active port and never `null` -- on ports without BLE it
/// reports every operation as unsupported.
///
/// Any number of scans may run concurrently. This base class multiplexes
/// them over a single platform scan: each [BleScan] handle only receives
/// advertisements matching its own [ScanSettings] filters, and the
/// platform scan stops when the last handle stops.
public class BluetoothLE {

    private final Object scanLock = new Object();
    private final ArrayList<ScanRegistration> activeScans =
            new ArrayList<ScanRegistration>();

    private static final class ScanRegistration {
        BleScan handle;
        ScanSettings settings;
        ScanListener listener;
        HashSet<String> seen;
    }

    /// Ports construct subclasses. Application code obtains the active
    /// instance via `Bluetooth.getInstance().getLE()`.
    protected BluetoothLE() {
    }

    /// Starts scanning; the listener fires on the EDT for every
    /// advertisement passing the settings' filters. Returns a live
    /// [BleScan] handle -- keep a reference and call [BleScan#stop()]
    /// when done. On ports without BLE the returned handle is already
    /// failed with [BluetoothError#NOT_SUPPORTED].
    public final BleScan startScan(ScanSettings settings, ScanListener listener) {
        final ScanRegistration reg = new ScanRegistration();
        reg.settings = settings == null ? new ScanSettings() : settings;
        reg.listener = listener;
        reg.handle = new BleScan() {
            protected void onStop() {
                unregisterScan(reg);
            }
        };
        if (listener == null) {
            reg.handle.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "startScan requires a listener"));
            return reg.handle;
        }
        if (!isScanSupported()) {
            reg.handle.error(new BluetoothException(
                    BluetoothError.NOT_SUPPORTED,
                    "BLE scanning is not supported on this platform"));
            return reg.handle;
        }
        if (!reg.settings.isAllowDuplicates()) {
            reg.seen = new HashSet<String>();
        }
        boolean first;
        synchronized (scanLock) {
            activeScans.add(reg);
            first = activeScans.size() == 1;
        }
        if (first) {
            try {
                startPlatformScan();
            } catch (RuntimeException ex) {
                unregisterScan(reg);
                if (!reg.handle.isDone()) {
                    reg.handle.error(new BluetoothException(
                            BluetoothError.SCAN_FAILED,
                            "Failed to start scan: " + ex, ex));
                }
                return reg.handle;
            }
        }
        onScanRegistrationsChanged();
        return reg.handle;
    }

    private void unregisterScan(ScanRegistration reg) {
        boolean last;
        synchronized (scanLock) {
            boolean removed = activeScans.remove(reg);
            last = removed && activeScans.isEmpty();
        }
        if (last) {
            try {
                stopPlatformScan();
            } catch (RuntimeException ignored) {
            }
        }
        onScanRegistrationsChanged();
    }

    /// Re-obtains a peripheral from an address persisted earlier (see
    /// [com.codename1.bluetooth.BluetoothDevice#getAddress()] for the
    /// address semantics) without scanning. Returns `null` when the
    /// platform cannot resolve the address.
    public BlePeripheral getPeripheral(String address) {
        return null;
    }

    /// The peripherals the *system* currently holds connected (e.g.
    /// paired wearables), optionally filtered to those offering the given
    /// service. Empty on ports without BLE.
    ///
    /// iOS filters exactly (CoreBluetooth resolves services); Android can
    /// only consult its cached SDP/GATT UUIDs, so the filter is
    /// best-effort there and devices with an empty cache are retained.
    public List<BlePeripheral> getConnectedPeripherals(
            BluetoothUuid serviceFilter) {
        return new ArrayList<BlePeripheral>();
    }

    /// The LE peripherals bonded with this device. Empty on ports without
    /// BLE or without a bonded-device registry (iOS).
    public List<BlePeripheral> getBondedPeripherals() {
        return new ArrayList<BlePeripheral>();
    }

    // ------------------------------------------------------------------
    // peripheral role
    // ------------------------------------------------------------------

    /// Opens the local GATT server for the peripheral role; the
    /// listener's methods fire on the EDT. Fails with
    /// [BluetoothError#NOT_SUPPORTED] on ports without peripheral mode --
    /// branch via `Bluetooth.getInstance().isPeripheralModeSupported()`.
    public AsyncResource<GattServer> openGattServer(
            GattServerListener listener) {
        AsyncResource<GattServer> r = new AsyncResource<GattServer>();
        r.error(peripheralNotSupported());
        return r;
    }

    /// Starts advertising. Resolves with a live [BleAdvertisement] handle
    /// once the platform actually started broadcasting, or fails with
    /// [BluetoothError#ADVERTISE_FAILED] /
    /// [BluetoothError#NOT_SUPPORTED]. `scanResponse` may be `null`.
    public AsyncResource<BleAdvertisement> startAdvertising(
            AdvertiseSettings settings, AdvertiseData data,
            AdvertiseData scanResponse) {
        AsyncResource<BleAdvertisement> r =
                new AsyncResource<BleAdvertisement>();
        r.error(peripheralNotSupported());
        return r;
    }

    /// Opens a listening L2CAP endpoint for the peripheral role; publish
    /// [L2capServer#getPsm()] to centrals (typically via a GATT
    /// characteristic). Fails with [BluetoothError#NOT_SUPPORTED] where
    /// L2CAP or peripheral mode is unavailable.
    public AsyncResource<L2capServer> openL2capServer(boolean secure) {
        AsyncResource<L2capServer> r = new AsyncResource<L2capServer>();
        r.error(peripheralNotSupported());
        return r;
    }

    private static BluetoothException peripheralNotSupported() {
        return new BluetoothException(BluetoothError.NOT_SUPPORTED,
                "BLE peripheral mode is not supported on this platform");
    }

    // ------------------------------------------------------------------
    // port SPI
    // ------------------------------------------------------------------

    /// `true` when this port can scan; the base class returns `false`,
    /// making [#startScan(ScanSettings, ScanListener)] fail fast.
    protected boolean isScanSupported() {
        return false;
    }

    /// Starts the single underlying platform scan; called when the first
    /// [BleScan] handle registers. Sightings are reported via
    /// [#fireScanResult(ScanResult)]. May throw a `RuntimeException` to
    /// fail the initiating handle.
    protected void startPlatformScan() {
    }

    /// Stops the underlying platform scan; called when the last handle
    /// stops.
    protected void stopPlatformScan() {
    }

    /// Called after every scan registration change. Ports may retune the
    /// platform scan (e.g. push down filters or raise the scan mode) based
    /// on [#getActiveScanSettings()]. Default does nothing.
    protected void onScanRegistrationsChanged() {
    }

    /// The settings of all currently active scan handles -- for ports
    /// that optimize the platform scan.
    protected final List<ScanSettings> getActiveScanSettings() {
        ArrayList<ScanSettings> out = new ArrayList<ScanSettings>();
        synchronized (scanLock) {
            int size = activeScans.size();
            for (int i = 0; i < size; i++) {
                out.add(activeScans.get(i).settings);
            }
        }
        return out;
    }

    /// The most aggressive [ScanMode] among the active handles --
    /// convenience for ports mapping the merged scan onto a platform scan
    /// mode.
    protected final ScanMode getAggregateScanMode() {
        ScanMode mode = ScanMode.OPPORTUNISTIC;
        synchronized (scanLock) {
            int size = activeScans.size();
            for (int i = 0; i < size; i++) {
                ScanMode m = activeScans.get(i).settings.getScanMode();
                if (m.ordinal() > mode.ordinal()) {
                    mode = m;
                }
            }
        }
        return mode;
    }

    /// Reports one advertisement sighting from the platform scan; safe to
    /// call from any thread. The base class demultiplexes it to every
    /// active handle whose filters match, applying per-handle duplicate
    /// suppression, and dispatches the listeners on the EDT.
    protected final void fireScanResult(final ScanResult result) {
        if (result == null) {
            return;
        }
        final ArrayList<ScanListener> targets = new ArrayList<ScanListener>();
        synchronized (scanLock) {
            int size = activeScans.size();
            for (int i = 0; i < size; i++) {
                ScanRegistration reg = activeScans.get(i);
                if (!reg.settings.matches(result)) {
                    continue;
                }
                if (reg.seen != null && !reg.seen.add(
                        result.getPeripheral().getAddress())) {
                    continue;
                }
                targets.add(reg.listener);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        dispatchScanResult(targets, result);
    }

    // Static so the Runnable doesn't carry a synthetic outer-BluetoothLE
    // reference (SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static void dispatchScanResult(
            final ArrayList<ScanListener> targets, final ScanResult result) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                int size = targets.size();
                for (int i = 0; i < size; i++) {
                    targets.get(i).peripheralDiscovered(result);
                }
            }
        });
    }

    /// Reports that the OS aborted the platform scan; every active handle
    /// fails with the given reason and the registry is cleared. The base
    /// class does NOT call [#stopPlatformScan()] on this path -- the
    /// platform scan is presumed dead, so ports must clean up their own
    /// scanner state before (or when) calling this.
    protected final void fireScanFailed(BluetoothException reason) {
        final ArrayList<ScanRegistration> failed;
        synchronized (scanLock) {
            failed = new ArrayList<ScanRegistration>(activeScans);
            activeScans.clear();
        }
        BluetoothException r = reason != null ? reason
                : new BluetoothException(BluetoothError.SCAN_FAILED,
                        "Scan aborted by the OS");
        int size = failed.size();
        for (int i = 0; i < size; i++) {
            BleScan handle = failed.get(i).handle;
            if (!handle.isDone()) {
                handle.error(r);
            }
        }
        onScanRegistrationsChanged();
    }
}
