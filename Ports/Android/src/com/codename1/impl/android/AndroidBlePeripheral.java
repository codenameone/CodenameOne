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

import android.content.Context;
import android.os.Build;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BondState;
import com.codename1.bluetooth.DeviceType;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattDescriptor;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ConnectionOptions;
import com.codename1.bluetooth.le.ConnectionPriority;
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.bluetooth.le.L2capChannel;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Android implementation of {@link BlePeripheral} on top of
 * {@code BluetoothGatt}. The core class serializes the {@code do*} SPI
 * calls one-at-a-time per peripheral, so each operation kind keeps a single
 * pending {@code AsyncResource} slot that the matching
 * {@code BluetoothGattCallback} method completes. All platform callbacks
 * arrive on binder threads; the core fire methods and AsyncResource
 * plumbing perform the EDT dispatch.
 */
class AndroidBlePeripheral extends BlePeripheral {

    private final android.bluetooth.BluetoothDevice device;
    private volatile android.bluetooth.BluetoothGatt gatt;

    private final Object lock = new Object();
    private AsyncResource<BlePeripheral> pendingConnect;
    private AsyncResource<List<GattService>> pendingDiscover;
    private AsyncResource<byte[]> pendingCharRead;
    private AsyncResource<Boolean> pendingCharWrite;
    private AsyncResource<byte[]> pendingDescRead;
    private AsyncResource<Boolean> pendingDescWrite;
    private AsyncResource<Integer> pendingRssi;
    private AsyncResource<Integer> pendingMtu;

    /**
     * Maps between the platform GATT database and the canonical core model
     * built during the last service discovery. fireNotification must be
     * handed the canonical GattCharacteristic instance, and the do* methods
     * must resolve the platform object backing a canonical one. Neither
     * class overrides equals, so plain HashMaps give identity semantics.
     */
    private final Object mapLock = new Object();
    private final HashMap<android.bluetooth.BluetoothGattCharacteristic, GattCharacteristic>
            charToCore =
            new HashMap<android.bluetooth.BluetoothGattCharacteristic, GattCharacteristic>();
    private final HashMap<GattCharacteristic, android.bluetooth.BluetoothGattCharacteristic>
            charToPlatform =
            new HashMap<GattCharacteristic, android.bluetooth.BluetoothGattCharacteristic>();
    private final HashMap<GattDescriptor, android.bluetooth.BluetoothGattDescriptor>
            descToPlatform =
            new HashMap<GattDescriptor, android.bluetooth.BluetoothGattDescriptor>();

    AndroidBlePeripheral(android.bluetooth.BluetoothDevice device) {
        this.device = device;
    }

    android.bluetooth.BluetoothDevice getPlatformDevice() {
        return device;
    }

    // ------------------------------------------------------------------
    // identity
    // ------------------------------------------------------------------

    @Override
    public String getAddress() {
        return device.getAddress();
    }

    @Override
    public String getName() {
        try {
            return device.getName();
        } catch (SecurityException se) {
            return null;
        }
    }

    @Override
    public DeviceType getType() {
        try {
            return AndroidBluetooth.mapDeviceType(device.getType());
        } catch (SecurityException se) {
            return DeviceType.UNKNOWN;
        }
    }

    @Override
    public BondState getBondState() {
        try {
            return AndroidBluetooth.mapBondState(device.getBondState());
        } catch (SecurityException se) {
            return BondState.NONE;
        }
    }

    // ------------------------------------------------------------------
    // connection lifecycle
    // ------------------------------------------------------------------

    @Override
    protected void doConnect(ConnectionOptions options,
            AsyncResource<BlePeripheral> out) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) {
            throw new RuntimeException("No Android context available");
        }
        synchronized (lock) {
            pendingConnect = out;
        }
        android.bluetooth.BluetoothGatt g;
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                g = device.connectGatt(ctx, options.isAutoConnect(), callback,
                        android.bluetooth.BluetoothDevice.TRANSPORT_LE);
            } else {
                g = device.connectGatt(ctx, options.isAutoConnect(), callback);
            }
        } catch (SecurityException se) {
            synchronized (lock) {
                pendingConnect = null;
            }
            out.error(new BluetoothException(BluetoothError.UNAUTHORIZED,
                    "Missing BLUETOOTH_CONNECT permission", se));
            return;
        }
        if (g == null) {
            synchronized (lock) {
                pendingConnect = null;
            }
            throw new RuntimeException("connectGatt returned null");
        }
        gatt = g;
    }

    @Override
    protected void doDisconnect() {
        android.bluetooth.BluetoothGatt g = gatt;
        if (g != null) {
            try {
                g.disconnect();
            } catch (Throwable ignore) {
            }
        }
    }

    /** Always close the BluetoothGatt after the disconnect callback -- a
     * leaked client eventually exhausts the per-device GATT interfaces. */
    private void closeGatt() {
        android.bluetooth.BluetoothGatt g;
        synchronized (lock) {
            g = gatt;
            gatt = null;
        }
        if (g != null) {
            try {
                g.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private android.bluetooth.BluetoothGatt requireGatt(AsyncResource<?> out) {
        android.bluetooth.BluetoothGatt g = gatt;
        if (g == null && !out.isDone()) {
            out.error(new BluetoothException(BluetoothError.NOT_CONNECTED,
                    "Peripheral is not connected"));
        }
        return g;
    }

    // ------------------------------------------------------------------
    // GATT client SPI
    // ------------------------------------------------------------------

    @Override
    protected void doDiscoverServices(AsyncResource<List<GattService>> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        synchronized (lock) {
            pendingDiscover = out;
        }
        boolean started;
        try {
            started = g.discoverServices();
        } catch (SecurityException se) {
            started = false;
        }
        if (!started) {
            synchronized (lock) {
                pendingDiscover = null;
            }
            out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                    "Failed to start service discovery"));
        }
    }

    @Override
    protected void doReadCharacteristic(GattCharacteristic c,
            AsyncResource<byte[]> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        android.bluetooth.BluetoothGattCharacteristic pc = platformChar(c);
        if (pc == null) {
            out.error(staleCharacteristic());
            return;
        }
        synchronized (lock) {
            pendingCharRead = out;
        }
        boolean started;
        try {
            started = g.readCharacteristic(pc);
        } catch (SecurityException se) {
            started = false;
        }
        if (!started) {
            synchronized (lock) {
                pendingCharRead = null;
            }
            out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                    "Failed to start characteristic read"));
        }
    }

    @Override
    protected void doWriteCharacteristic(GattCharacteristic c, byte[] value,
            boolean withResponse, AsyncResource<Boolean> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        android.bluetooth.BluetoothGattCharacteristic pc = platformChar(c);
        if (pc == null) {
            out.error(staleCharacteristic());
            return;
        }
        pc.setWriteType(withResponse
                ? android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                : android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        pc.setValue(value == null ? new byte[0] : value);
        synchronized (lock) {
            pendingCharWrite = out;
        }
        boolean started;
        try {
            started = g.writeCharacteristic(pc);
        } catch (SecurityException se) {
            started = false;
        }
        if (!started) {
            synchronized (lock) {
                pendingCharWrite = null;
            }
            out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                    "Failed to start characteristic write"));
        }
    }

    @Override
    protected void doReadDescriptor(GattDescriptor d,
            AsyncResource<byte[]> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        android.bluetooth.BluetoothGattDescriptor pd = platformDesc(d);
        if (pd == null) {
            out.error(staleCharacteristic());
            return;
        }
        synchronized (lock) {
            pendingDescRead = out;
        }
        boolean started;
        try {
            started = g.readDescriptor(pd);
        } catch (SecurityException se) {
            started = false;
        }
        if (!started) {
            synchronized (lock) {
                pendingDescRead = null;
            }
            out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                    "Failed to start descriptor read"));
        }
    }

    @Override
    protected void doWriteDescriptor(GattDescriptor d, byte[] value,
            AsyncResource<Boolean> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        android.bluetooth.BluetoothGattDescriptor pd = platformDesc(d);
        if (pd == null) {
            out.error(staleCharacteristic());
            return;
        }
        pd.setValue(value == null ? new byte[0] : value);
        startDescriptorWrite(g, pd, out);
    }

    @Override
    protected void doSetNotifications(GattCharacteristic c, boolean enable,
            boolean indication, AsyncResource<Boolean> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        android.bluetooth.BluetoothGattCharacteristic pc = platformChar(c);
        if (pc == null) {
            out.error(staleCharacteristic());
            return;
        }
        boolean armed;
        try {
            armed = g.setCharacteristicNotification(pc, enable);
        } catch (SecurityException se) {
            armed = false;
        }
        if (!armed) {
            out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                    "setCharacteristicNotification failed"));
            return;
        }
        android.bluetooth.BluetoothGattDescriptor cccd =
                pc.getDescriptor(AndroidBluetooth.CCCD_UUID);
        if (cccd == null) {
            // no CCCD on this characteristic -- the local arm is all there is
            out.complete(Boolean.TRUE);
            return;
        }
        byte[] v;
        if (!enable) {
            v = android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        } else if (indication) {
            v = android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        } else {
            v = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        }
        cccd.setValue(v);
        startDescriptorWrite(g, cccd, out);
    }

    /** Shared by doWriteDescriptor and the CCCD write of
     * doSetNotifications -- only one queued operation is in flight, so a
     * single pending slot serves both. The AsyncResource completes in
     * onDescriptorWrite. */
    private void startDescriptorWrite(android.bluetooth.BluetoothGatt g,
            android.bluetooth.BluetoothGattDescriptor pd,
            AsyncResource<Boolean> out) {
        synchronized (lock) {
            pendingDescWrite = out;
        }
        boolean started;
        try {
            started = g.writeDescriptor(pd);
        } catch (SecurityException se) {
            started = false;
        }
        if (!started) {
            synchronized (lock) {
                pendingDescWrite = null;
            }
            out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                    "Failed to start descriptor write"));
        }
    }

    @Override
    protected void doReadRssi(AsyncResource<Integer> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        synchronized (lock) {
            pendingRssi = out;
        }
        boolean started;
        try {
            started = g.readRemoteRssi();
        } catch (SecurityException se) {
            started = false;
        }
        if (!started) {
            synchronized (lock) {
                pendingRssi = null;
            }
            out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                    "Failed to start RSSI read"));
        }
    }

    @Override
    protected void doRequestMtu(int mtu, AsyncResource<Integer> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < 21) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "MTU negotiation requires Android 5 (API 21)"));
            return;
        }
        synchronized (lock) {
            pendingMtu = out;
        }
        boolean started;
        try {
            started = g.requestMtu(mtu);
        } catch (SecurityException se) {
            started = false;
        }
        if (!started) {
            synchronized (lock) {
                pendingMtu = null;
            }
            out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                    "Failed to start MTU negotiation"));
        }
    }

    @Override
    protected void doRequestConnectionPriority(ConnectionPriority priority,
            AsyncResource<Boolean> out) {
        android.bluetooth.BluetoothGatt g = requireGatt(out);
        if (g == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < 21) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "Connection priority requires Android 5 (API 21)"));
            return;
        }
        int v;
        if (priority == ConnectionPriority.HIGH) {
            v = android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH;
        } else if (priority == ConnectionPriority.LOW_POWER) {
            v = android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER;
        } else {
            v = android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED;
        }
        boolean ok;
        try {
            // Android has no public completion callback for this request;
            // resolve with the submission result.
            ok = g.requestConnectionPriority(v);
        } catch (SecurityException se) {
            ok = false;
        }
        out.complete(ok ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    protected void doCreateBond(AsyncResource<Boolean> out) {
        AndroidBluetooth.createBondImpl(device, out);
    }

    @Override
    protected void doOpenL2cap(final int psm, final boolean secure,
            final AsyncResource<L2capChannel> out) {
        if (!AndroidL2capCompat.isSupported()) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "L2CAP channels require Android 10 (API 29) or newer"));
            return;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                android.bluetooth.BluetoothSocket socket = null;
                try {
                    socket = AndroidL2capCompat.openChannel(device, psm,
                            secure);
                    socket.connect();
                    out.complete(new AndroidL2capChannel(psm, socket));
                } catch (SecurityException se) {
                    closeQuietly(socket);
                    out.error(new BluetoothException(
                            BluetoothError.UNAUTHORIZED,
                            "Missing BLUETOOTH_CONNECT permission", se));
                } catch (IOException ioe) {
                    closeQuietly(socket);
                    out.error(new BluetoothException(BluetoothError.IO_ERROR,
                            "L2CAP open failed: " + ioe.getMessage(), ioe));
                } catch (Throwable ex) {
                    closeQuietly(socket);
                    out.error(new BluetoothException(BluetoothError.UNKNOWN,
                            "L2CAP open failed: " + ex, ex));
                }
            }
        }, "CN1-L2CAP-connect");
        t.setDaemon(true);
        t.start();
    }

    private static void closeQuietly(android.bluetooth.BluetoothSocket s) {
        if (s != null) {
            try {
                s.close();
            } catch (Throwable ignore) {
            }
        }
    }

    // ------------------------------------------------------------------
    // model mapping
    // ------------------------------------------------------------------

    private android.bluetooth.BluetoothGattCharacteristic platformChar(
            GattCharacteristic c) {
        synchronized (mapLock) {
            return charToPlatform.get(c);
        }
    }

    private android.bluetooth.BluetoothGattDescriptor platformDesc(
            GattDescriptor d) {
        synchronized (mapLock) {
            return descToPlatform.get(d);
        }
    }

    private static BluetoothException staleCharacteristic() {
        return new BluetoothException(BluetoothError.GATT_ERROR,
                "Unknown attribute -- use instances from the last "
                        + "discoverServices() result");
    }

    /** Rebuilds the canonical core model from the platform GATT database
     * after a successful discovery. */
    private List<GattService> buildServiceModel(
            android.bluetooth.BluetoothGatt g) {
        ArrayList<GattService> result = new ArrayList<GattService>();
        List<android.bluetooth.BluetoothGattService> platformServices =
                g.getServices();
        HashMap<android.bluetooth.BluetoothGattService, GattService> svcMap =
                new HashMap<android.bluetooth.BluetoothGattService, GattService>();
        synchronized (mapLock) {
            charToCore.clear();
            charToPlatform.clear();
            descToPlatform.clear();
            for (android.bluetooth.BluetoothGattService ps : platformServices) {
                GattService s = new GattService(this,
                        AndroidBluetooth.toCn1Uuid(ps.getUuid()),
                        ps.getType() == android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY,
                        ps.getInstanceId());
                for (android.bluetooth.BluetoothGattCharacteristic pc
                        : ps.getCharacteristics()) {
                    // the core PROPERTY_* bits mirror the Bluetooth spec and
                    // therefore Android's values -- pass through verbatim
                    GattCharacteristic c = new GattCharacteristic(s,
                            AndroidBluetooth.toCn1Uuid(pc.getUuid()),
                            pc.getProperties(), pc.getInstanceId());
                    for (android.bluetooth.BluetoothGattDescriptor pd
                            : pc.getDescriptors()) {
                        GattDescriptor d = new GattDescriptor(c,
                                AndroidBluetooth.toCn1Uuid(pd.getUuid()));
                        c.addDescriptor(d);
                        descToPlatform.put(d, pd);
                    }
                    s.addCharacteristic(c);
                    charToCore.put(pc, c);
                    charToPlatform.put(c, pc);
                }
                svcMap.put(ps, s);
                result.add(s);
            }
            // second pass: link included services to the canonical instances
            for (android.bluetooth.BluetoothGattService ps : platformServices) {
                GattService s = svcMap.get(ps);
                for (android.bluetooth.BluetoothGattService inc
                        : ps.getIncludedServices()) {
                    GattService coreInc = svcMap.get(inc);
                    if (coreInc != null) {
                        s.addIncludedService(coreInc);
                    }
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // platform callback -- binder threads
    // ------------------------------------------------------------------

    private final android.bluetooth.BluetoothGattCallback callback =
            new android.bluetooth.BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(
                android.bluetooth.BluetoothGatt g, int status, int newState) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                AsyncResource<BlePeripheral> pc;
                synchronized (lock) {
                    pc = pendingConnect;
                    pendingConnect = null;
                }
                if (pc != null) {
                    if (pc.isDone()) {
                        // the core already timed out / cancelled this
                        // attempt -- tear the late connection down
                        try {
                            g.disconnect();
                        } catch (Throwable ignore) {
                        }
                        return;
                    }
                    pc.complete(AndroidBlePeripheral.this);
                } else {
                    // autoConnect reconnection outside a pending connect()
                    fireConnectionStateChanged(ConnectionState.CONNECTED,
                            null);
                }
            } else if (newState
                    == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                closeGatt();
                AsyncResource<BlePeripheral> pc;
                synchronized (lock) {
                    pc = pendingConnect;
                    pendingConnect = null;
                }
                if (pc != null && !pc.isDone()) {
                    pc.error(new BluetoothException(
                            BluetoothError.CONNECTION_FAILED,
                            "Connection attempt failed (status " + status
                                    + ")", status));
                } else {
                    fireConnectionStateChanged(ConnectionState.DISCONNECTED,
                            status == android.bluetooth.BluetoothGatt.GATT_SUCCESS
                                    ? null
                                    : new BluetoothException(
                                            BluetoothError.CONNECTION_LOST,
                                            "Connection lost (status "
                                                    + status + ")", status));
                }
            }
        }

        @Override
        public void onServicesDiscovered(android.bluetooth.BluetoothGatt g,
                int status) {
            AsyncResource<List<GattService>> out;
            synchronized (lock) {
                out = pendingDiscover;
                pendingDiscover = null;
            }
            if (out == null || out.isDone()) {
                return;
            }
            if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                        "Service discovery failed (status " + status + ")",
                        status));
                return;
            }
            out.complete(buildServiceModel(g));
        }

        @Override
        public void onCharacteristicRead(android.bluetooth.BluetoothGatt g,
                android.bluetooth.BluetoothGattCharacteristic pc,
                int status) {
            // copy before anything else -- Android reuses the buffer
            byte[] copied = copyValue(pc.getValue());
            AsyncResource<byte[]> out;
            synchronized (lock) {
                out = pendingCharRead;
                pendingCharRead = null;
            }
            if (out == null || out.isDone()) {
                return;
            }
            if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                        "Characteristic read failed (status " + status + ")",
                        status));
                return;
            }
            out.complete(copied);
        }

        @Override
        public void onCharacteristicWrite(android.bluetooth.BluetoothGatt g,
                android.bluetooth.BluetoothGattCharacteristic pc,
                int status) {
            AsyncResource<Boolean> out;
            synchronized (lock) {
                out = pendingCharWrite;
                pendingCharWrite = null;
            }
            if (out == null || out.isDone()) {
                return;
            }
            if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                        "Characteristic write failed (status " + status + ")",
                        status));
                return;
            }
            out.complete(Boolean.TRUE);
        }

        @Override
        public void onDescriptorRead(android.bluetooth.BluetoothGatt g,
                android.bluetooth.BluetoothGattDescriptor pd, int status) {
            byte[] copied = copyValue(pd.getValue());
            AsyncResource<byte[]> out;
            synchronized (lock) {
                out = pendingDescRead;
                pendingDescRead = null;
            }
            if (out == null || out.isDone()) {
                return;
            }
            if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                        "Descriptor read failed (status " + status + ")",
                        status));
                return;
            }
            out.complete(copied);
        }

        @Override
        public void onDescriptorWrite(android.bluetooth.BluetoothGatt g,
                android.bluetooth.BluetoothGattDescriptor pd, int status) {
            AsyncResource<Boolean> out;
            synchronized (lock) {
                out = pendingDescWrite;
                pendingDescWrite = null;
            }
            if (out == null || out.isDone()) {
                return;
            }
            if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                        "Descriptor write failed (status " + status + ")",
                        status));
                return;
            }
            out.complete(Boolean.TRUE);
        }

        @Override
        public void onCharacteristicChanged(
                android.bluetooth.BluetoothGatt g,
                android.bluetooth.BluetoothGattCharacteristic pc) {
            // Android reuses the value buffer -- copy IMMEDIATELY, before
            // any dispatch hop
            byte[] copied = copyValue(pc.getValue());
            GattCharacteristic core;
            synchronized (mapLock) {
                core = charToCore.get(pc);
            }
            if (core != null) {
                fireNotification(core, copied);
            }
        }

        @Override
        public void onReadRemoteRssi(android.bluetooth.BluetoothGatt g,
                int rssi, int status) {
            AsyncResource<Integer> out;
            synchronized (lock) {
                out = pendingRssi;
                pendingRssi = null;
            }
            if (out == null || out.isDone()) {
                return;
            }
            if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                        "RSSI read failed (status " + status + ")", status));
                return;
            }
            out.complete(Integer.valueOf(rssi));
        }

        @Override
        public void onMtuChanged(android.bluetooth.BluetoothGatt g, int mtu,
                int status) {
            AsyncResource<Integer> out;
            synchronized (lock) {
                out = pendingMtu;
                pendingMtu = null;
            }
            if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                if (out != null && !out.isDone()) {
                    // the core records the granted value from the result
                    out.complete(Integer.valueOf(mtu));
                } else {
                    // peer-initiated MTU change
                    setMtu(mtu);
                }
            } else if (out != null && !out.isDone()) {
                out.error(new BluetoothException(BluetoothError.GATT_ERROR,
                        "MTU negotiation failed (status " + status + ")",
                        status));
            }
        }
    };

    private static byte[] copyValue(byte[] raw) {
        if (raw == null) {
            return new byte[0];
        }
        byte[] copy = new byte[raw.length];
        System.arraycopy(raw, 0, copy, 0, raw.length);
        return copy;
    }
}
