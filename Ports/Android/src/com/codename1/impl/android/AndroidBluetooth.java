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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothPermission;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.BondState;
import com.codename1.bluetooth.DeviceType;
import com.codename1.bluetooth.le.AdvertisementData;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.BluetoothLE;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.bluetooth.le.ScanMode;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.server.AdvertiseData;
import com.codename1.bluetooth.le.server.AdvertiseMode;
import com.codename1.bluetooth.le.server.AdvertiseSettings;
import com.codename1.bluetooth.le.server.BleAdvertisement;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.bluetooth.le.server.TxPowerLevel;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Android implementation of {@link com.codename1.bluetooth.Bluetooth}:
 * capability queries, adapter state (with an ACTION_STATE_CHANGED
 * broadcast receiver feeding fireAdapterStateChanged), the runtime
 * permission mapping and the enable-request system dialog. The LE and
 * classic role objects live in {@link AndroidBluetoothLE} and
 * {@link AndroidRfcomm}.
 *
 * Android Bluetooth classes are referenced fully qualified throughout the
 * Bluetooth port files because their simple names collide with the core
 * API's (BluetoothDevice, ScanResult, AdvertiseData, ...).
 */
class AndroidBluetooth extends com.codename1.bluetooth.Bluetooth {

    /** Android 12 runtime permissions -- string literals because the port
     * compiles against the API 27 android.jar (same precedent as
     * POST_NOTIFICATIONS in AndroidImplementation). */
    static final String PERMISSION_SCAN = "android.permission.BLUETOOTH_SCAN";
    static final String PERMISSION_CONNECT =
            "android.permission.BLUETOOTH_CONNECT";
    static final String PERMISSION_ADVERTISE =
            "android.permission.BLUETOOTH_ADVERTISE";

    /** The Client Characteristic Configuration descriptor. */
    static final java.util.UUID CCCD_UUID =
            toPlatformUuid(BluetoothUuid.CCCD);

    private AndroidBluetoothLE le;
    private AndroidRfcomm classic;

    AndroidBluetooth() {
        registerAdapterStateReceiver();
    }

    // ------------------------------------------------------------------
    // shared plumbing used by the other Bluetooth port classes
    // ------------------------------------------------------------------

    static android.bluetooth.BluetoothAdapter adapter() {
        try {
            return android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        } catch (Throwable t) {
            return null;
        }
    }

    static android.bluetooth.BluetoothManager manager() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) {
            return null;
        }
        try {
            return (android.bluetooth.BluetoothManager) ctx
                    .getSystemService(Context.BLUETOOTH_SERVICE);
        } catch (Throwable t) {
            return null;
        }
    }

    static BluetoothUuid toCn1Uuid(java.util.UUID uuid) {
        return new BluetoothUuid(uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits());
    }

    static java.util.UUID toPlatformUuid(BluetoothUuid uuid) {
        return new java.util.UUID(uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits());
    }

    static DeviceType mapDeviceType(int platformType) {
        switch (platformType) {
            case android.bluetooth.BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return DeviceType.CLASSIC;
            case android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE:
                return DeviceType.LE;
            case android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL:
                return DeviceType.DUAL;
            default:
                return DeviceType.UNKNOWN;
        }
    }

    static BondState mapBondState(int platformState) {
        switch (platformState) {
            case android.bluetooth.BluetoothDevice.BOND_BONDING:
                return BondState.BONDING;
            case android.bluetooth.BluetoothDevice.BOND_BONDED:
                return BondState.BONDED;
            default:
                return BondState.NONE;
        }
    }

    /**
     * Registers a receiver for a protected system broadcast. On API 33+
     * the 3-argument registerReceiver overload is invoked reflectively
     * with RECEIVER_EXPORTED (0x2) because the constant/overload is absent
     * from the API 27 android.jar the port compiles against.
     */
    static void registerSystemReceiver(Context appCtx,
            BroadcastReceiver receiver, IntentFilter filter) {
        boolean registered = false;
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                java.lang.reflect.Method m = Context.class.getMethod(
                        "registerReceiver", BroadcastReceiver.class,
                        IntentFilter.class, int.class);
                m.invoke(appCtx, receiver, filter, Integer.valueOf(0x2));
                registered = true;
            } catch (Throwable ignore) {
            }
        }
        if (!registered) {
            appCtx.registerReceiver(receiver, filter);
        }
    }

    /**
     * Shared bonding flow: watches ACTION_BOND_STATE_CHANGED for the given
     * device and resolves the resource when the bond state settles. Used by
     * both the LE peripheral and the classic role.
     */
    static void createBondImpl(final android.bluetooth.BluetoothDevice device,
            final AsyncResource<Boolean> out) {
        if (device == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "createBond requires a device"));
            return;
        }
        try {
            if (device.getBondState()
                    == android.bluetooth.BluetoothDevice.BOND_BONDED) {
                out.complete(Boolean.TRUE);
                return;
            }
        } catch (SecurityException se) {
            out.error(new BluetoothException(BluetoothError.UNAUTHORIZED,
                    "Missing BLUETOOTH_CONNECT permission", se));
            return;
        }
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "No Android context available"));
            return;
        }
        final Context appCtx = ctx.getApplicationContext() != null
                ? ctx.getApplicationContext() : ctx;
        final String address = device.getAddress();
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                android.bluetooth.BluetoothDevice d =
                        (android.bluetooth.BluetoothDevice) intent
                                .getParcelableExtra(
                                        android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                if (d == null || !address.equals(d.getAddress())) {
                    return;
                }
                int state = intent.getIntExtra(
                        android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE,
                        -1);
                if (state
                        == android.bluetooth.BluetoothDevice.BOND_BONDED) {
                    try {
                        appCtx.unregisterReceiver(this);
                    } catch (Throwable ignore) {
                    }
                    if (!out.isDone()) {
                        out.complete(Boolean.TRUE);
                    }
                } else if (state
                        == android.bluetooth.BluetoothDevice.BOND_NONE) {
                    try {
                        appCtx.unregisterReceiver(this);
                    } catch (Throwable ignore) {
                    }
                    if (!out.isDone()) {
                        out.error(new BluetoothException(
                                BluetoothError.BOND_FAILED,
                                "Bonding failed or was rejected"));
                    }
                }
            }
        };
        registerSystemReceiver(appCtx, receiver, new IntentFilter(
                android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        boolean started;
        try {
            started = device.createBond();
        } catch (SecurityException se) {
            try {
                appCtx.unregisterReceiver(receiver);
            } catch (Throwable ignore) {
            }
            out.error(new BluetoothException(BluetoothError.UNAUTHORIZED,
                    "Missing BLUETOOTH_CONNECT permission", se));
            return;
        }
        if (!started) {
            try {
                appCtx.unregisterReceiver(receiver);
            } catch (Throwable ignore) {
            }
            out.error(new BluetoothException(BluetoothError.BOND_FAILED,
                    "The platform could not start bonding"));
        }
    }

    // ------------------------------------------------------------------
    // capabilities
    // ------------------------------------------------------------------

    @Override
    public boolean isSupported() {
        return adapter() != null;
    }

    @Override
    public boolean isLeSupported() {
        if (adapter() == null || Build.VERSION.SDK_INT < 21) {
            return false;
        }
        Context ctx = AndroidImplementation.getContext();
        return ctx != null && ctx.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @Override
    public boolean isClassicSupported() {
        return isSupported();
    }

    @Override
    public boolean isPeripheralModeSupported() {
        return isLeSupported();
    }

    @Override
    public boolean isL2capSupported() {
        return isLeSupported() && AndroidL2capCompat.isSupported();
    }

    // ------------------------------------------------------------------
    // adapter state
    // ------------------------------------------------------------------

    @Override
    public AdapterState getAdapterState() {
        android.bluetooth.BluetoothAdapter a = adapter();
        if (a == null) {
            return AdapterState.UNSUPPORTED;
        }
        try {
            return mapAdapterState(a.getState());
        } catch (SecurityException se) {
            return AdapterState.UNAUTHORIZED;
        }
    }

    static AdapterState mapAdapterState(int platformState) {
        switch (platformState) {
            case android.bluetooth.BluetoothAdapter.STATE_ON:
                return AdapterState.POWERED_ON;
            case android.bluetooth.BluetoothAdapter.STATE_OFF:
                return AdapterState.POWERED_OFF;
            case android.bluetooth.BluetoothAdapter.STATE_TURNING_ON:
                return AdapterState.TURNING_ON;
            case android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF:
                return AdapterState.TURNING_OFF;
            default:
                return AdapterState.UNKNOWN;
        }
    }

    private void registerAdapterStateReceiver() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null || adapter() == null) {
            return;
        }
        Context appCtx = ctx.getApplicationContext() != null
                ? ctx.getApplicationContext() : ctx;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                int state = intent.getIntExtra(
                        android.bluetooth.BluetoothAdapter.EXTRA_STATE, -1);
                fireAdapterStateChanged(mapAdapterState(state));
            }
        };
        try {
            registerSystemReceiver(appCtx, receiver, new IntentFilter(
                    android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED));
        } catch (Throwable ignore) {
        }
    }

    @Override
    public AsyncResource<Boolean> requestEnable() {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        android.bluetooth.BluetoothAdapter a = adapter();
        if (a == null) {
            out.complete(Boolean.FALSE);
            return out;
        }
        if (a.isEnabled()) {
            out.complete(Boolean.TRUE);
            return out;
        }
        if (AndroidImplementation.getActivity() == null) {
            // the system dialog needs a foreground activity
            out.complete(Boolean.FALSE);
            return out;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (Build.VERSION.SDK_INT >= 31
                        && !AndroidImplementation.checkForPermission(
                                PERMISSION_CONNECT,
                                "This is required to turn on Bluetooth")) {
                    out.complete(Boolean.FALSE);
                    return;
                }
                try {
                    AndroidNativeUtil.startActivityForResult(new Intent(
                            android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE),
                            new IntentResultListener() {
                        public void onActivityResult(int requestCode,
                                int resultCode, Intent data) {
                            out.complete(resultCode == Activity.RESULT_OK
                                    ? Boolean.TRUE : Boolean.FALSE);
                        }
                    });
                } catch (RuntimeException ex) {
                    out.complete(Boolean.FALSE);
                }
            }
        });
        return out;
    }

    // ------------------------------------------------------------------
    // permissions
    // ------------------------------------------------------------------

    /**
     * Maps a portable permission to the runtime permissions the current
     * Android version actually requires: the BLUETOOTH_* runtime trio on
     * 12+ (API 31), fine location for scanning on 6-11 (API 23-30), and
     * nothing below that (install-time manifest permissions only).
     */
    static String[] runtimePermissions(BluetoothPermission p) {
        if (Build.VERSION.SDK_INT >= 31) {
            switch (p) {
                case SCAN:
                    return new String[]{PERMISSION_SCAN};
                case CONNECT:
                    return new String[]{PERMISSION_CONNECT};
                case ADVERTISE:
                    return new String[]{PERMISSION_ADVERTISE};
                default:
                    return new String[0];
            }
        }
        if (Build.VERSION.SDK_INT >= 23 && p == BluetoothPermission.SCAN) {
            return new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION};
        }
        return new String[0];
    }

    @Override
    public boolean hasPermission(BluetoothPermission permission) {
        if (!isSupported() || permission == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) {
            return false;
        }
        String[] perms = runtimePermissions(permission);
        for (int i = 0; i < perms.length; i++) {
            if (android.support.v4.content.ContextCompat.checkSelfPermission(
                    ctx, perms[i]) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public AsyncResource<Boolean> requestPermissions(
            BluetoothPermission... permissions) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (!isSupported()) {
            out.complete(Boolean.FALSE);
            return out;
        }
        final ArrayList<String> perms = new ArrayList<String>();
        if (permissions != null) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i] == null) {
                    continue;
                }
                String[] mapped = runtimePermissions(permissions[i]);
                for (int j = 0; j < mapped.length; j++) {
                    if (!perms.contains(mapped[j])) {
                        perms.add(mapped[j]);
                    }
                }
            }
        }
        if (perms.isEmpty()) {
            out.complete(Boolean.TRUE);
            return out;
        }
        // checkForPermission blocks via invokeAndBlock and must run on the
        // EDT
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                boolean all = true;
                int size = perms.size();
                for (int i = 0; i < size; i++) {
                    all = AndroidImplementation.checkForPermission(
                            perms.get(i),
                            "This is required to use Bluetooth") && all;
                }
                out.complete(all ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return out;
    }

    // ------------------------------------------------------------------
    // role entry points
    // ------------------------------------------------------------------

    @Override
    public synchronized BluetoothLE getLE() {
        if (le == null) {
            le = new AndroidBluetoothLE(this);
        }
        return le;
    }

    @Override
    public synchronized com.codename1.bluetooth.classic.BluetoothClassic
            getClassic() {
        if (classic == null) {
            classic = new AndroidRfcomm();
        }
        return classic;
    }
}

/**
 * Android implementation of the BLE central + peripheral role entry point
 * over BluetoothLeScanner / BluetoothLeAdvertiser / BluetoothGattServer.
 * The core class multiplexes any number of app-level scans onto the single
 * platform scan managed here.
 */
class AndroidBluetoothLE extends BluetoothLE {

    private final AndroidBluetooth bluetooth;

    /** Peripheral identity cache so repeated sightings of one device share
     * a single stateful BlePeripheral instance. */
    private final Object peripheralLock = new Object();
    private final HashMap<String, AndroidBlePeripheral> peripherals =
            new HashMap<String, AndroidBlePeripheral>();

    private final Object scanLock = new Object();
    private android.bluetooth.le.BluetoothLeScanner activeScanner;
    private android.bluetooth.le.ScanCallback activeCallback;

    AndroidBluetoothLE(AndroidBluetooth bluetooth) {
        this.bluetooth = bluetooth;
    }

    AndroidBlePeripheral getOrCreatePeripheral(
            android.bluetooth.BluetoothDevice device) {
        synchronized (peripheralLock) {
            AndroidBlePeripheral p = peripherals.get(device.getAddress());
            if (p == null) {
                p = new AndroidBlePeripheral(device);
                peripherals.put(device.getAddress(), p);
            }
            return p;
        }
    }

    // ------------------------------------------------------------------
    // scanning SPI
    // ------------------------------------------------------------------

    @Override
    protected boolean isScanSupported() {
        return bluetooth.isLeSupported();
    }

    @Override
    protected void startPlatformScan() {
        android.bluetooth.BluetoothAdapter a = AndroidBluetooth.adapter();
        if (a == null) {
            throw new RuntimeException(
                    "Bluetooth is not available on this device");
        }
        if (!a.isEnabled()) {
            throw new RuntimeException("The Bluetooth adapter is powered off");
        }
        android.bluetooth.le.BluetoothLeScanner scanner =
                a.getBluetoothLeScanner();
        if (scanner == null) {
            throw new RuntimeException(
                    "BLE scanning is unavailable (adapter off?)");
        }
        android.bluetooth.le.ScanSettings settings =
                new android.bluetooth.le.ScanSettings.Builder()
                        .setScanMode(mapScanMode(getAggregateScanMode()))
                        .build();
        android.bluetooth.le.ScanCallback cb =
                new android.bluetooth.le.ScanCallback() {
            @Override
            public void onScanResult(int callbackType,
                    android.bluetooth.le.ScanResult result) {
                deliver(result);
            }

            @Override
            public void onBatchScanResults(
                    List<android.bluetooth.le.ScanResult> results) {
                if (results != null) {
                    int size = results.size();
                    for (int i = 0; i < size; i++) {
                        deliver(results.get(i));
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                clearPlatformScan();
                fireScanFailed(new BluetoothException(
                        BluetoothError.SCAN_FAILED,
                        "The OS aborted the scan (code " + errorCode + ")"));
            }
        };
        // a SecurityException (missing BLUETOOTH_SCAN / location) propagates
        // as a RuntimeException and fails the initiating handle
        scanner.startScan(null, settings, cb);
        synchronized (scanLock) {
            activeScanner = scanner;
            activeCallback = cb;
        }
    }

    @Override
    protected void stopPlatformScan() {
        clearPlatformScan();
    }

    private void clearPlatformScan() {
        android.bluetooth.le.BluetoothLeScanner scanner;
        android.bluetooth.le.ScanCallback cb;
        synchronized (scanLock) {
            scanner = activeScanner;
            cb = activeCallback;
            activeScanner = null;
            activeCallback = null;
        }
        if (scanner != null && cb != null) {
            try {
                scanner.stopScan(cb);
            } catch (Throwable ignore) {
            }
        }
    }

    private void deliver(android.bluetooth.le.ScanResult platformResult) {
        if (platformResult == null || platformResult.getDevice() == null) {
            return;
        }
        AndroidBlePeripheral p = getOrCreatePeripheral(
                platformResult.getDevice());
        android.bluetooth.le.ScanRecord record =
                platformResult.getScanRecord();
        AdvertisementData ad = record != null
                ? AdvertisementData.parse(record.getBytes())
                : new AdvertisementData();
        boolean connectable = true;
        if (Build.VERSION.SDK_INT >= 26) {
            connectable = platformResult.isConnectable();
        }
        fireScanResult(new ScanResult(p, platformResult.getRssi(), ad,
                connectable, System.currentTimeMillis()));
    }

    private static int mapScanMode(ScanMode mode) {
        if (mode == ScanMode.OPPORTUNISTIC && Build.VERSION.SDK_INT >= 23) {
            return android.bluetooth.le.ScanSettings.SCAN_MODE_OPPORTUNISTIC;
        }
        if (mode == ScanMode.LOW_POWER || mode == ScanMode.OPPORTUNISTIC) {
            return android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER;
        }
        if (mode == ScanMode.LOW_LATENCY) {
            return android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;
        }
        return android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED;
    }

    // ------------------------------------------------------------------
    // peripheral lookup
    // ------------------------------------------------------------------

    @Override
    public BlePeripheral getPeripheral(String address) {
        android.bluetooth.BluetoothAdapter a = AndroidBluetooth.adapter();
        if (a == null || address == null
                || !android.bluetooth.BluetoothAdapter
                        .checkBluetoothAddress(address)) {
            return null;
        }
        return getOrCreatePeripheral(a.getRemoteDevice(address));
    }

    @Override
    public List<BlePeripheral> getConnectedPeripherals(
            BluetoothUuid serviceFilter) {
        ArrayList<BlePeripheral> out = new ArrayList<BlePeripheral>();
        android.bluetooth.BluetoothManager mgr = AndroidBluetooth.manager();
        if (mgr == null) {
            return out;
        }
        List<android.bluetooth.BluetoothDevice> devices;
        try {
            devices = mgr.getConnectedDevices(
                    android.bluetooth.BluetoothProfile.GATT);
        } catch (SecurityException se) {
            return out;
        }
        if (devices == null) {
            return out;
        }
        java.util.UUID want = serviceFilter == null
                ? null : AndroidBluetooth.toPlatformUuid(serviceFilter);
        int size = devices.size();
        for (int i = 0; i < size; i++) {
            android.bluetooth.BluetoothDevice d = devices.get(i);
            if (want != null) {
                // best effort: the SDP/GATT cache is only populated for
                // some devices; when it is unavailable the device is kept
                android.os.ParcelUuid[] uuids = null;
                try {
                    uuids = d.getUuids();
                } catch (Throwable ignore) {
                }
                if (uuids != null && uuids.length > 0) {
                    boolean match = false;
                    for (int j = 0; j < uuids.length; j++) {
                        if (want.equals(uuids[j].getUuid())) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        continue;
                    }
                }
            }
            out.add(getOrCreatePeripheral(d));
        }
        return out;
    }

    @Override
    public List<BlePeripheral> getBondedPeripherals() {
        ArrayList<BlePeripheral> out = new ArrayList<BlePeripheral>();
        android.bluetooth.BluetoothAdapter a = AndroidBluetooth.adapter();
        if (a == null) {
            return out;
        }
        java.util.Set<android.bluetooth.BluetoothDevice> bonded;
        try {
            bonded = a.getBondedDevices();
        } catch (SecurityException se) {
            return out;
        }
        if (bonded == null) {
            return out;
        }
        for (android.bluetooth.BluetoothDevice d : bonded) {
            int type;
            try {
                type = d.getType();
            } catch (SecurityException se) {
                type = android.bluetooth.BluetoothDevice.DEVICE_TYPE_UNKNOWN;
            }
            if (type == android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE
                    || type == android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL) {
                out.add(getOrCreatePeripheral(d));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // peripheral role
    // ------------------------------------------------------------------

    @Override
    public AsyncResource<GattServer> openGattServer(
            GattServerListener listener) {
        AsyncResource<GattServer> out = new AsyncResource<GattServer>();
        if (!bluetooth.isPeripheralModeSupported()) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "BLE peripheral mode is not supported on this device"));
            return out;
        }
        if (listener == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "openGattServer requires a listener"));
            return out;
        }
        try {
            out.complete(new AndroidGattServerImpl(listener));
        } catch (SecurityException se) {
            out.error(new BluetoothException(BluetoothError.UNAUTHORIZED,
                    "Missing BLUETOOTH_CONNECT permission", se));
        } catch (RuntimeException ex) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "Failed to open the GATT server: " + ex.getMessage(),
                    ex));
        }
        return out;
    }

    @Override
    public AsyncResource<BleAdvertisement> startAdvertising(
            AdvertiseSettings settings, AdvertiseData data,
            AdvertiseData scanResponse) {
        final AsyncResource<BleAdvertisement> out =
                new AsyncResource<BleAdvertisement>();
        android.bluetooth.BluetoothAdapter a = AndroidBluetooth.adapter();
        if (!bluetooth.isPeripheralModeSupported() || a == null) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "BLE peripheral mode is not supported on this device"));
            return out;
        }
        if (!a.isEnabled()) {
            out.error(new BluetoothException(BluetoothError.POWERED_OFF,
                    "The Bluetooth adapter is powered off"));
            return out;
        }
        android.bluetooth.le.BluetoothLeAdvertiser advertiser =
                a.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            out.error(new BluetoothException(BluetoothError.ADVERTISE_FAILED,
                    "This device cannot advertise"));
            return out;
        }
        AdvertiseSettings s = settings == null
                ? new AdvertiseSettings() : settings;
        android.bluetooth.le.AdvertiseSettings platformSettings =
                new android.bluetooth.le.AdvertiseSettings.Builder()
                        .setAdvertiseMode(mapAdvertiseMode(s.getMode()))
                        .setTxPowerLevel(mapTxPower(s.getTxPower()))
                        .setConnectable(s.isConnectable())
                        .setTimeout(s.getTimeout())
                        .build();
        android.bluetooth.le.AdvertiseData platformData =
                buildAdvertiseData(data);
        android.bluetooth.le.AdvertiseData platformScanResponse =
                scanResponse == null ? null : buildAdvertiseData(scanResponse);
        final AndroidAdvertisementHandle handle =
                new AndroidAdvertisementHandle(advertiser);
        android.bluetooth.le.AdvertiseCallback cb =
                new android.bluetooth.le.AdvertiseCallback() {
            @Override
            public void onStartSuccess(
                    android.bluetooth.le.AdvertiseSettings settingsInEffect) {
                handle.markActive();
                if (!out.isDone()) {
                    out.complete(handle);
                }
            }

            @Override
            public void onStartFailure(int errorCode) {
                if (!out.isDone()) {
                    out.error(new BluetoothException(
                            BluetoothError.ADVERTISE_FAILED,
                            "Advertising failed to start (code " + errorCode
                                    + ")"));
                }
            }
        };
        handle.callback = cb;
        try {
            advertiser.startAdvertising(platformSettings, platformData,
                    platformScanResponse, cb);
        } catch (SecurityException se) {
            out.error(new BluetoothException(BluetoothError.UNAUTHORIZED,
                    "Missing BLUETOOTH_ADVERTISE permission", se));
        } catch (RuntimeException ex) {
            out.error(new BluetoothException(BluetoothError.ADVERTISE_FAILED,
                    "Advertising failed to start: " + ex.getMessage(), ex));
        }
        return out;
    }

    private static android.bluetooth.le.AdvertiseData buildAdvertiseData(
            AdvertiseData data) {
        android.bluetooth.le.AdvertiseData.Builder b =
                new android.bluetooth.le.AdvertiseData.Builder();
        if (data != null) {
            b.setIncludeDeviceName(data.isIncludeDeviceName());
            b.setIncludeTxPowerLevel(data.isIncludeTxPower());
            List<BluetoothUuid> uuids = data.getServiceUuids();
            int size = uuids.size();
            for (int i = 0; i < size; i++) {
                b.addServiceUuid(new android.os.ParcelUuid(
                        AndroidBluetooth.toPlatformUuid(uuids.get(i))));
            }
            for (Map.Entry<Integer, byte[]> e
                    : data.getManufacturerData().entrySet()) {
                b.addManufacturerData(e.getKey().intValue(), e.getValue());
            }
            for (Map.Entry<BluetoothUuid, byte[]> e
                    : data.getServiceData().entrySet()) {
                b.addServiceData(new android.os.ParcelUuid(
                        AndroidBluetooth.toPlatformUuid(e.getKey())),
                        e.getValue());
            }
        }
        return b.build();
    }

    private static int mapAdvertiseMode(AdvertiseMode mode) {
        if (mode == AdvertiseMode.LOW_POWER) {
            return android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
        }
        if (mode == AdvertiseMode.LOW_LATENCY) {
            return android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
        }
        return android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    }

    private static int mapTxPower(TxPowerLevel level) {
        if (level == TxPowerLevel.ULTRA_LOW) {
            return android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW;
        }
        if (level == TxPowerLevel.LOW) {
            return android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_LOW;
        }
        if (level == TxPowerLevel.HIGH) {
            return android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;
        }
        return android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM;
    }

    @Override
    public AsyncResource<L2capServer> openL2capServer(final boolean secure) {
        final AsyncResource<L2capServer> out = new AsyncResource<L2capServer>();
        final android.bluetooth.BluetoothAdapter a =
                AndroidBluetooth.adapter();
        if (a == null || !AndroidL2capCompat.isSupported()) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "L2CAP channels require Android 10 (API 29) or newer"));
            return out;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    android.bluetooth.BluetoothServerSocket serverSocket =
                            AndroidL2capCompat.listen(a, secure);
                    int psm = AndroidL2capCompat.psmOf(serverSocket);
                    out.complete(new AndroidL2capServer(psm, serverSocket));
                } catch (SecurityException se) {
                    out.error(new BluetoothException(
                            BluetoothError.UNAUTHORIZED,
                            "Missing BLUETOOTH_CONNECT permission", se));
                } catch (IOException ioe) {
                    out.error(new BluetoothException(BluetoothError.IO_ERROR,
                            "L2CAP listen failed: " + ioe.getMessage(), ioe));
                } catch (Throwable ex) {
                    out.error(new BluetoothException(BluetoothError.UNKNOWN,
                            "L2CAP listen failed: " + ex, ex));
                }
            }
        }, "CN1-L2CAP-listen");
        t.setDaemon(true);
        t.start();
        return out;
    }

    /** Live advertisement handle; stop() detaches the platform callback. */
    private static final class AndroidAdvertisementHandle
            extends BleAdvertisement {

        private final android.bluetooth.le.BluetoothLeAdvertiser advertiser;
        volatile android.bluetooth.le.AdvertiseCallback callback;
        private volatile boolean active;

        AndroidAdvertisementHandle(
                android.bluetooth.le.BluetoothLeAdvertiser advertiser) {
            this.advertiser = advertiser;
        }

        void markActive() {
            active = true;
        }

        @Override
        public void stop() {
            if (!active) {
                return;
            }
            active = false;
            android.bluetooth.le.AdvertiseCallback cb = callback;
            if (cb != null) {
                try {
                    advertiser.stopAdvertising(cb);
                } catch (Throwable ignore) {
                }
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
