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
import android.os.Build;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.BondState;
import com.codename1.bluetooth.DeviceType;
import com.codename1.bluetooth.classic.BluetoothClassic;
import com.codename1.bluetooth.classic.ClassicDiscovery;
import com.codename1.bluetooth.classic.ClassicDiscoveryListener;
import com.codename1.bluetooth.classic.ClassicScanResult;
import com.codename1.bluetooth.classic.RfcommConnection;
import com.codename1.bluetooth.classic.RfcommServer;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Android implementation of classic Bluetooth (BR/EDR): inquiry discovery
 * via {@code BluetoothAdapter.startDiscovery} broadcasts, bonding, the
 * discoverability system dialog and RFCOMM stream connections. All blocking
 * socket work (connect/accept) runs on dedicated daemon threads -- never on
 * the EDT.
 */
class AndroidRfcomm extends BluetoothClassic {

    AndroidRfcomm() {
    }

    // ------------------------------------------------------------------
    // discovery
    // ------------------------------------------------------------------

    @Override
    public ClassicDiscovery startDiscovery(
            final ClassicDiscoveryListener listener) {
        final android.bluetooth.BluetoothAdapter adapter =
                AndroidBluetooth.adapter();
        final Context ctx = AndroidImplementation.getContext();
        final boolean[] stopped = new boolean[1];
        final BroadcastReceiver[] receiverRef = new BroadcastReceiver[1];
        final ClassicDiscovery handle =
                makeClassicDiscovery(adapter, ctx, stopped, receiverRef);
        if (listener == null) {
            handle.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "startDiscovery requires a listener"));
            return handle;
        }
        if (adapter == null || ctx == null) {
            handle.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "Classic Bluetooth is not available on this device"));
            return handle;
        }
        if (!adapter.isEnabled()) {
            handle.error(new BluetoothException(BluetoothError.POWERED_OFF,
                    "The Bluetooth adapter is powered off"));
            return handle;
        }
        final Context appCtx = ctx.getApplicationContext() != null
                ? ctx.getApplicationContext() : ctx;
        BroadcastReceiver receiver =
                makeDiscoveryReceiver(listener, stopped, appCtx, handle);
        receiverRef[0] = receiver;
        IntentFilter filter = new IntentFilter();
        filter.addAction(android.bluetooth.BluetoothDevice.ACTION_FOUND);
        filter.addAction(
                android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        AndroidBluetooth.registerSystemReceiver(appCtx, receiver, filter);
        boolean started;
        try {
            started = adapter.startDiscovery();
        } catch (SecurityException se) {
            unregisterQuietly(appCtx, receiver);
            handle.error(new BluetoothException(BluetoothError.UNAUTHORIZED,
                    "Missing Bluetooth scan permission", se));
            return handle;
        }
        if (!started) {
            unregisterQuietly(appCtx, receiver);
            handle.error(new BluetoothException(BluetoothError.SCAN_FAILED,
                    "The platform failed to start the inquiry scan"));
        }
        return handle;
    }

    // Static so the ClassicDiscovery doesn't carry a synthetic
    // outer-AndroidRfcomm reference (SpotBugs
    // SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static ClassicDiscovery makeClassicDiscovery(
            final android.bluetooth.BluetoothAdapter adapter,
            final Context ctx, final boolean[] stopped,
            final BroadcastReceiver[] receiverRef) {
        return new ClassicDiscovery() {
            protected void onStop() {
                synchronized (stopped) {
                    if (stopped[0]) {
                        return;
                    }
                    stopped[0] = true;
                }
                try {
                    adapter.cancelDiscovery();
                } catch (Throwable ignore) {
                }
                unregisterQuietly(ctx, receiverRef[0]);
            }
        };
    }

    // Static so the BroadcastReceiver doesn't carry a synthetic
    // outer-AndroidRfcomm reference (SpotBugs
    // SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static BroadcastReceiver makeDiscoveryReceiver(
            final ClassicDiscoveryListener listener, final boolean[] stopped,
            final Context appCtx, final ClassicDiscovery handle) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                String action = intent.getAction();
                if (android.bluetooth.BluetoothDevice.ACTION_FOUND
                        .equals(action)) {
                    android.bluetooth.BluetoothDevice d =
                            (android.bluetooth.BluetoothDevice) intent
                                    .getParcelableExtra(
                                            android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                    if (d == null) {
                        return;
                    }
                    int rssi = intent.getShortExtra(
                            android.bluetooth.BluetoothDevice.EXTRA_RSSI,
                            Short.MIN_VALUE);
                    int major = 0;
                    int deviceClass = 0;
                    android.bluetooth.BluetoothClass cls =
                            (android.bluetooth.BluetoothClass) intent
                                    .getParcelableExtra(
                                            android.bluetooth.BluetoothDevice.EXTRA_CLASS);
                    if (cls != null) {
                        major = cls.getMajorDeviceClass();
                        deviceClass = cls.getDeviceClass();
                    }
                    final ClassicScanResult result = new ClassicScanResult(
                            new ClassicDevice(d), rssi, major, deviceClass);
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            listener.deviceDiscovered(result);
                        }
                    });
                } else if (android.bluetooth.BluetoothAdapter
                        .ACTION_DISCOVERY_FINISHED.equals(action)) {
                    synchronized (stopped) {
                        if (stopped[0]) {
                            return;
                        }
                        stopped[0] = true;
                    }
                    unregisterQuietly(appCtx, this);
                    if (!handle.isDone()) {
                        handle.complete(Boolean.TRUE);
                    }
                }
            }
        };
    }

    private static void unregisterQuietly(Context ctx,
            BroadcastReceiver receiver) {
        if (ctx == null || receiver == null) {
            return;
        }
        Context appCtx = ctx.getApplicationContext() != null
                ? ctx.getApplicationContext() : ctx;
        try {
            appCtx.unregisterReceiver(receiver);
        } catch (Throwable ignore) {
        }
    }

    // ------------------------------------------------------------------
    // bonded devices / bonding / discoverability
    // ------------------------------------------------------------------

    @Override
    public List<com.codename1.bluetooth.BluetoothDevice> getBondedDevices() {
        ArrayList<com.codename1.bluetooth.BluetoothDevice> out =
                new ArrayList<com.codename1.bluetooth.BluetoothDevice>();
        android.bluetooth.BluetoothAdapter adapter =
                AndroidBluetooth.adapter();
        if (adapter == null) {
            return out;
        }
        Set<android.bluetooth.BluetoothDevice> bonded;
        try {
            bonded = adapter.getBondedDevices();
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
            if (type != android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE) {
                out.add(new ClassicDevice(d));
            }
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> createBond(
            com.codename1.bluetooth.BluetoothDevice device) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        android.bluetooth.BluetoothAdapter adapter =
                AndroidBluetooth.adapter();
        if (adapter == null || device == null) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "Classic Bluetooth is not available on this device"));
            return out;
        }
        android.bluetooth.BluetoothDevice platformDevice;
        if (device instanceof ClassicDevice) {
            platformDevice = ((ClassicDevice) device).device;
        } else if (device instanceof AndroidBlePeripheral) {
            platformDevice = ((AndroidBlePeripheral) device)
                    .getPlatformDevice();
        } else {
            try {
                platformDevice = adapter.getRemoteDevice(device.getAddress());
            } catch (IllegalArgumentException iae) {
                out.error(new BluetoothException(BluetoothError.UNKNOWN,
                        "Invalid Bluetooth address: " + device.getAddress()));
                return out;
            }
        }
        AndroidBluetooth.createBondImpl(platformDevice, out);
        return out;
    }

    @Override
    public AsyncResource<Boolean> requestDiscoverable(
            final int durationSeconds) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (AndroidBluetooth.adapter() == null
                || AndroidImplementation.getActivity() == null) {
            out.complete(Boolean.FALSE);
            return out;
        }
        Display.getInstance().callSerially(
                makeRequestDiscoverableRunnable(durationSeconds, out));
        return out;
    }

    // Static so the Runnable doesn't carry a synthetic outer-AndroidRfcomm
    // reference (SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static Runnable makeRequestDiscoverableRunnable(
            final int durationSeconds, final AsyncResource<Boolean> out) {
        return new Runnable() {
            public void run() {
                if (Build.VERSION.SDK_INT >= 31
                        && !AndroidImplementation.checkForPermission(
                                AndroidBluetooth.PERMISSION_ADVERTISE,
                                "This is required to make the device "
                                        + "discoverable")) {
                    out.complete(Boolean.FALSE);
                    return;
                }
                try {
                    Intent intent = new Intent(
                            android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    if (durationSeconds > 0) {
                        intent.putExtra(
                                android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                                durationSeconds);
                    }
                    AndroidNativeUtil.startActivityForResult(intent,
                            new IntentResultListener() {
                        public void onActivityResult(int requestCode,
                                int resultCode, Intent data) {
                            // the result code is the granted duration, or
                            // RESULT_CANCELED when the user declined
                            out.complete(
                                    resultCode != Activity.RESULT_CANCELED
                                            ? Boolean.TRUE : Boolean.FALSE);
                        }
                    });
                } catch (RuntimeException ex) {
                    out.complete(Boolean.FALSE);
                }
            }
        };
    }

    // ------------------------------------------------------------------
    // RFCOMM
    // ------------------------------------------------------------------

    @Override
    public AsyncResource<RfcommConnection> connect(
            com.codename1.bluetooth.BluetoothDevice device,
            BluetoothUuid serviceUuid, boolean secure) {
        AsyncResource<RfcommConnection> out =
                new AsyncResource<RfcommConnection>();
        if (device == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "connect requires a device"));
            return out;
        }
        return connect(device.getAddress(), serviceUuid, secure);
    }

    @Override
    public AsyncResource<RfcommConnection> connect(String address,
            BluetoothUuid serviceUuid, final boolean secure) {
        final AsyncResource<RfcommConnection> out =
                new AsyncResource<RfcommConnection>();
        final android.bluetooth.BluetoothAdapter adapter =
                AndroidBluetooth.adapter();
        if (adapter == null) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "Classic Bluetooth is not available on this device"));
            return out;
        }
        if (!adapter.isEnabled()) {
            out.error(new BluetoothException(BluetoothError.POWERED_OFF,
                    "The Bluetooth adapter is powered off"));
            return out;
        }
        final android.bluetooth.BluetoothDevice platformDevice;
        try {
            platformDevice = adapter.getRemoteDevice(address);
        } catch (IllegalArgumentException iae) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "Invalid Bluetooth address: " + address));
            return out;
        }
        final java.util.UUID uuid = AndroidBluetooth.toPlatformUuid(
                serviceUuid == null ? BluetoothUuid.SPP : serviceUuid);
        Thread t = new Thread(makeConnectRunnable(adapter, platformDevice,
                uuid, secure, out), "CN1-RFCOMM-connect");
        t.setDaemon(true);
        t.start();
        return out;
    }

    // Static so the Runnable doesn't carry a synthetic outer-AndroidRfcomm
    // reference (SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static Runnable makeConnectRunnable(
            final android.bluetooth.BluetoothAdapter adapter,
            final android.bluetooth.BluetoothDevice platformDevice,
            final java.util.UUID uuid, final boolean secure,
            final AsyncResource<RfcommConnection> out) {
        return new Runnable() {
            public void run() {
                android.bluetooth.BluetoothSocket socket = null;
                try {
                    // an active inquiry scan badly degrades RFCOMM connects
                    try {
                        adapter.cancelDiscovery();
                    } catch (Throwable ignore) {
                    }
                    if (secure) {
                        socket = platformDevice
                                .createRfcommSocketToServiceRecord(uuid);
                    } else {
                        socket = platformDevice
                                .createInsecureRfcommSocketToServiceRecord(
                                        uuid);
                    }
                    socket.connect();
                    out.complete(new AndroidRfcommConnection(
                            new ClassicDevice(platformDevice), socket));
                } catch (SecurityException se) {
                    closeQuietly(socket);
                    out.error(new BluetoothException(
                            BluetoothError.UNAUTHORIZED,
                            "Missing BLUETOOTH_CONNECT permission", se));
                } catch (IOException ioe) {
                    closeQuietly(socket);
                    out.error(new BluetoothException(BluetoothError.IO_ERROR,
                            "RFCOMM connect failed: " + ioe.getMessage(),
                            ioe));
                } catch (Throwable ex) {
                    closeQuietly(socket);
                    out.error(new BluetoothException(BluetoothError.UNKNOWN,
                            "RFCOMM connect failed: " + ex, ex));
                }
            }
        };
    }

    @Override
    public AsyncResource<RfcommServer> listen(final String serviceName,
            BluetoothUuid serviceUuid, final boolean secure) {
        final AsyncResource<RfcommServer> out =
                new AsyncResource<RfcommServer>();
        final android.bluetooth.BluetoothAdapter adapter =
                AndroidBluetooth.adapter();
        if (adapter == null) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "Classic Bluetooth is not available on this device"));
            return out;
        }
        final BluetoothUuid effectiveUuid =
                serviceUuid == null ? BluetoothUuid.SPP : serviceUuid;
        final java.util.UUID uuid =
                AndroidBluetooth.toPlatformUuid(effectiveUuid);
        Thread t = new Thread(makeListenRunnable(adapter, serviceName,
                effectiveUuid, uuid, secure, out), "CN1-RFCOMM-listen");
        t.setDaemon(true);
        t.start();
        return out;
    }

    // Static so the Runnable doesn't carry a synthetic outer-AndroidRfcomm
    // reference (SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static Runnable makeListenRunnable(
            final android.bluetooth.BluetoothAdapter adapter,
            final String serviceName, final BluetoothUuid effectiveUuid,
            final java.util.UUID uuid, final boolean secure,
            final AsyncResource<RfcommServer> out) {
        return new Runnable() {
            public void run() {
                try {
                    android.bluetooth.BluetoothServerSocket serverSocket;
                    String name = serviceName == null
                            ? "CodenameOne" : serviceName;
                    if (secure) {
                        serverSocket = adapter
                                .listenUsingRfcommWithServiceRecord(name,
                                        uuid);
                    } else {
                        serverSocket = adapter
                                .listenUsingInsecureRfcommWithServiceRecord(
                                        name, uuid);
                    }
                    out.complete(new AndroidRfcommServer(effectiveUuid,
                            serverSocket));
                } catch (SecurityException se) {
                    out.error(new BluetoothException(
                            BluetoothError.UNAUTHORIZED,
                            "Missing BLUETOOTH_CONNECT permission", se));
                } catch (IOException ioe) {
                    out.error(new BluetoothException(BluetoothError.IO_ERROR,
                            "RFCOMM listen failed: " + ioe.getMessage(),
                            ioe));
                } catch (Throwable ex) {
                    out.error(new BluetoothException(BluetoothError.UNKNOWN,
                            "RFCOMM listen failed: " + ex, ex));
                }
            }
        };
    }

    static void closeQuietly(android.bluetooth.BluetoothSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Throwable ignore) {
            }
        }
    }

    // ------------------------------------------------------------------
    // wrappers
    // ------------------------------------------------------------------

    /** Identity wrapper for a classic (or unknown transport) device. */
    static final class ClassicDevice
            extends com.codename1.bluetooth.BluetoothDevice {
        final android.bluetooth.BluetoothDevice device;

        ClassicDevice(android.bluetooth.BluetoothDevice device) {
            this.device = device;
        }

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
    }
}

/** RFCOMM stream connection over a platform BluetoothSocket. */
class AndroidRfcommConnection extends RfcommConnection {

    private final android.bluetooth.BluetoothSocket socket;

    AndroidRfcommConnection(com.codename1.bluetooth.BluetoothDevice device,
            android.bluetooth.BluetoothSocket socket) {
        super(device);
        this.socket = socket;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public boolean isOpen() {
        return socket.isConnected();
    }
}

/**
 * Listening RFCOMM endpoint over a platform BluetoothServerSocket. The
 * blocking {@code accept()} runs on a dedicated daemon thread.
 */
class AndroidRfcommServer extends RfcommServer {

    private final android.bluetooth.BluetoothServerSocket serverSocket;
    private volatile boolean closed;

    AndroidRfcommServer(BluetoothUuid serviceUuid,
            android.bluetooth.BluetoothServerSocket serverSocket) {
        super(serviceUuid);
        this.serverSocket = serverSocket;
    }

    @Override
    public AsyncResource<RfcommConnection> accept() {
        final AsyncResource<RfcommConnection> out =
                new AsyncResource<RfcommConnection>();
        if (closed) {
            out.error(new BluetoothException(BluetoothError.IO_ERROR,
                    "The RFCOMM server is closed"));
            return out;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    android.bluetooth.BluetoothSocket socket =
                            serverSocket.accept();
                    out.complete(new AndroidRfcommConnection(
                            new AndroidRfcomm.ClassicDevice(
                                    socket.getRemoteDevice()), socket));
                } catch (IOException ioe) {
                    out.error(new BluetoothException(BluetoothError.IO_ERROR,
                            "RFCOMM accept failed: " + ioe.getMessage(),
                            ioe));
                } catch (Throwable ex) {
                    out.error(new BluetoothException(BluetoothError.UNKNOWN,
                            "RFCOMM accept failed: " + ex, ex));
                }
            }
        }, "CN1-RFCOMM-accept");
        t.setDaemon(true);
        t.start();
        return out;
    }

    @Override
    public void close() {
        closed = true;
        try {
            serverSocket.close();
        } catch (Throwable ignore) {
        }
    }
}
