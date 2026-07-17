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
package com.codename1.impl.ios;

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.BluetoothLE;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.ScanSettings;
import com.codename1.bluetooth.le.server.AdvertiseData;
import com.codename1.bluetooth.le.server.AdvertiseSettings;
import com.codename1.bluetooth.le.server.BleAdvertisement;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.io.Util;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/**
 * iOS BLE central / peripheral role entry point over CoreBluetooth.
 *
 * <p>Scanning: the ScanFilter fields are not readable by ports (setters
 * only), so the platform scan always runs unfiltered and the core
 * demultiplexer applies the per-handle filters and duplicate suppression.
 * CBCentralManagerScanOptionAllowDuplicatesKey is enabled only while at
 * least one active handle asked for duplicates.</p>
 */
class IOSBluetoothLE extends BluetoothLE {

    private final IOSBluetooth bt;
    private final Object scanStateLock = new Object();
    private boolean scanning;
    private boolean scanDuplicates;
    private IOSGattServer activeServer;

    IOSBluetoothLE(IOSBluetooth bt) {
        this.bt = bt;
    }

    // ------------------------------------------------------------------
    // scanning SPI
    // ------------------------------------------------------------------

    @Override
    protected boolean isScanSupported() {
        return true;
    }

    @Override
    protected void startPlatformScan() {
        bt.ensureMonitor();
        synchronized (scanStateLock) {
            scanning = true;
            scanDuplicates = anyDuplicates();
            bt.nativeInstance.btStartScan(null, scanDuplicates);
        }
    }

    @Override
    protected void stopPlatformScan() {
        synchronized (scanStateLock) {
            if (scanning) {
                scanning = false;
                bt.nativeInstance.btStopScan();
            }
        }
    }

    @Override
    protected void onScanRegistrationsChanged() {
        synchronized (scanStateLock) {
            if (!scanning) {
                return;
            }
            if (getActiveScanSettings().isEmpty()) {
                // fireScanFailed clears the registry without calling
                // stopPlatformScan -- stop the native scan here
                scanning = false;
                bt.nativeInstance.btStopScan();
                return;
            }
            boolean dup = anyDuplicates();
            if (dup != scanDuplicates) {
                scanDuplicates = dup;
                // re-issuing scanForPeripherals replaces the options
                bt.nativeInstance.btStartScan(null, dup);
            }
        }
    }

    private boolean anyDuplicates() {
        List<ScanSettings> settings = getActiveScanSettings();
        int size = settings.size();
        for (int i = 0; i < size; i++) {
            if (settings.get(i).isAllowDuplicates()) {
                return true;
            }
        }
        return false;
    }

    void scanResultFromNative(ScanResult result) {
        fireScanResult(result);
    }

    /** Adapter left POWERED_ON while a platform scan was running. */
    void platformScanLostFromNative(AdapterState state) {
        synchronized (scanStateLock) {
            if (!scanning) {
                return;
            }
            scanning = false;
        }
        BluetoothError err;
        if (state == AdapterState.POWERED_OFF) {
            err = BluetoothError.POWERED_OFF;
        } else if (state == AdapterState.UNAUTHORIZED) {
            err = BluetoothError.UNAUTHORIZED;
        } else {
            err = BluetoothError.SCAN_FAILED;
        }
        fireScanFailed(new BluetoothException(err,
                "Scan aborted: Bluetooth adapter is " + state));
    }

    // ------------------------------------------------------------------
    // known peripherals
    // ------------------------------------------------------------------

    @Override
    public BlePeripheral getPeripheral(String address) {
        if (address == null || address.length() == 0) {
            return null;
        }
        bt.ensureMonitor();
        String id = address.trim().toUpperCase();
        String name = bt.nativeInstance.btRetrievePeripheral(id);
        if (name == null) {
            return null;
        }
        return IOSBluetooth.peripheral(id,
                name.length() == 0 ? null : name);
    }

    @Override
    public List<BlePeripheral> getConnectedPeripherals(
            BluetoothUuid serviceFilter) {
        ArrayList<BlePeripheral> out = new ArrayList<BlePeripheral>();
        if (serviceFilter == null) {
            // retrieveConnectedPeripheralsWithServices requires a service
            // list -- iOS cannot enumerate all system connections
            return out;
        }
        bt.ensureMonitor();
        String s = bt.nativeInstance.btGetKnownPeripherals(
                serviceFilter.toString());
        if (s == null || s.length() == 0) {
            return out;
        }
        String[] lines = Util.split(s, "\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() == 0) {
                continue;
            }
            String[] fields = Util.split(lines[i], "\t");
            String name = fields.length > 1 && fields[1].length() > 0
                    ? fields[1] : null;
            out.add(IOSBluetooth.peripheral(fields[0], name));
        }
        return out;
    }

    // getBondedPeripherals(): iOS has no bonded-device registry; the base
    // class empty list is the correct answer.

    // ------------------------------------------------------------------
    // peripheral role
    // ------------------------------------------------------------------

    @Override
    public AsyncResource<GattServer> openGattServer(
            GattServerListener listener) {
        AsyncResource<GattServer> r = new AsyncResource<GattServer>();
        if (listener == null) {
            r.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "openGattServer requires a listener"));
            return r;
        }
        if (!bt.isPeripheralModeSupported()) {
            r.error(peripheralUnsupported());
            return r;
        }
        synchronized (this) {
            if (activeServer != null) {
                r.error(new BluetoothException(BluetoothError.BUSY,
                        "A GATT server is already open; close it first"));
                return r;
            }
        }
        IOSGattServer server = new IOSGattServer(bt, this, listener);
        int rid = IOSBluetooth.takeId(
                new IOSBluetooth.PendingServer(r, server));
        bt.nativeInstance.btOpenGattServer(rid);
        return r;
    }

    @Override
    public AsyncResource<BleAdvertisement> startAdvertising(
            AdvertiseSettings settings, AdvertiseData data,
            AdvertiseData scanResponse) {
        AsyncResource<BleAdvertisement> r =
                new AsyncResource<BleAdvertisement>();
        if (!bt.isPeripheralModeSupported()) {
            r.error(peripheralUnsupported());
            return r;
        }
        AdvertiseSettings st = settings == null
                ? new AdvertiseSettings() : settings;
        AdvertiseData d = data == null ? new AdvertiseData() : data;
        // iOS only advertises the local name and service UUIDs
        // (CBAdvertisementDataLocalNameKey / ServiceUUIDsKey); manufacturer
        // and service data are silently dropped by CoreBluetooth, and
        // advertising is always connectable while CBPeripheralManager runs.
        ArrayList<String> uuids = new ArrayList<String>();
        appendUuids(uuids, d);
        if (scanResponse != null) {
            appendUuids(uuids, scanResponse);
        }
        boolean includeName = d.isIncludeDeviceName()
                || (scanResponse != null && scanResponse.isIncludeDeviceName());
        // empty string asks the native side for the device name
        String localName = includeName ? "" : null;
        IOSBleAdvertisement adv = new IOSBleAdvertisement(bt,
                st.getTimeout());
        int rid = IOSBluetooth.takeId(
                new IOSBluetooth.PendingAdvertise(r, adv));
        bt.nativeInstance.btStartAdvertising(rid, localName,
                uuids.toArray(new String[uuids.size()]));
        return r;
    }

    private static void appendUuids(ArrayList<String> out, AdvertiseData d) {
        List<BluetoothUuid> uuids = d.getServiceUuids();
        int size = uuids.size();
        for (int i = 0; i < size; i++) {
            String s = uuids.get(i).toString();
            if (!out.contains(s)) {
                out.add(s);
            }
        }
    }

    @Override
    public AsyncResource<L2capServer> openL2capServer(boolean secure) {
        AsyncResource<L2capServer> r = new AsyncResource<L2capServer>();
        if (!bt.isPeripheralModeSupported()) {
            r.error(peripheralUnsupported());
            return r;
        }
        int rid = IOSBluetooth.takeId(
                new IOSBluetooth.PendingL2capServer(r));
        bt.nativeInstance.btPublishL2cap(rid, secure);
        return r;
    }

    private static BluetoothException peripheralUnsupported() {
        return new BluetoothException(BluetoothError.NOT_SUPPORTED,
                "BLE peripheral mode is not supported on this device");
    }

    // ------------------------------------------------------------------
    // package plumbing
    // ------------------------------------------------------------------

    synchronized IOSGattServer getActiveServer() {
        return activeServer;
    }

    synchronized void serverOpenedFromNative(IOSGattServer server) {
        activeServer = server;
    }

    synchronized void clearActiveServer(IOSGattServer server) {
        if (activeServer == server) {
            activeServer = null;
        }
    }
}
