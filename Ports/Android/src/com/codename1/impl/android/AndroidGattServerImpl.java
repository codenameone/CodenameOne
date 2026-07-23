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

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattStatus;
import com.codename1.bluetooth.le.server.BleCentral;
import com.codename1.bluetooth.le.server.GattLocalCharacteristic;
import com.codename1.bluetooth.le.server.GattLocalDescriptor;
import com.codename1.bluetooth.le.server.GattLocalService;
import com.codename1.bluetooth.le.server.GattReadRequest;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.bluetooth.le.server.GattWriteRequest;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Android implementation of the local {@link GattServer} over
 * {@code BluetoothGattServer}.
 *
 * Two operations require explicit serialization on Android and get it here:
 * consecutive {@code addService} calls must wait for {@code onServiceAdded},
 * and consecutive notifications must wait for {@code onNotificationSent}.
 * Client Characteristic Configuration descriptors are attached automatically
 * to notifying characteristics and their writes surface as
 * {@code subscriptionChanged} events rather than raw descriptor writes.
 */
class AndroidGattServerImpl extends GattServer {

    private final android.bluetooth.BluetoothGattServer server;

    private final Object serverLock = new Object();

    // platform -> definition lookups for routing requests
    private final HashMap<android.bluetooth.BluetoothGattCharacteristic, GattLocalCharacteristic>
            charMap =
            new HashMap<android.bluetooth.BluetoothGattCharacteristic, GattLocalCharacteristic>();
    private final HashMap<GattLocalCharacteristic, android.bluetooth.BluetoothGattCharacteristic>
            charReverse =
            new HashMap<GattLocalCharacteristic, android.bluetooth.BluetoothGattCharacteristic>();
    private final HashMap<android.bluetooth.BluetoothGattDescriptor, GattLocalDescriptor>
            descMap =
            new HashMap<android.bluetooth.BluetoothGattDescriptor, GattLocalDescriptor>();
    private final HashMap<GattLocalService, android.bluetooth.BluetoothGattService>
            serviceMap =
            new HashMap<GattLocalService, android.bluetooth.BluetoothGattService>();
    /** the CCCDs this implementation injected for notify/indicate chars */
    private final HashSet<android.bluetooth.BluetoothGattDescriptor> autoCccds =
            new HashSet<android.bluetooth.BluetoothGattDescriptor>();

    // connected centrals by address
    private final HashMap<String, AndroidCentral> centrals =
            new HashMap<String, AndroidCentral>();
    // per-characteristic subscriber addresses (from CCCD writes)
    private final HashMap<android.bluetooth.BluetoothGattCharacteristic, HashSet<String>>
            subscribers =
            new HashMap<android.bluetooth.BluetoothGattCharacteristic, HashSet<String>>();

    // ---- addService serialization (Android requires onServiceAdded
    // between consecutive additions)
    private static final class AddOp {
        final android.bluetooth.BluetoothGattService service;
        final AsyncResource<Boolean> out;

        AddOp(android.bluetooth.BluetoothGattService service,
                AsyncResource<Boolean> out) {
            this.service = service;
            this.out = out;
        }
    }

    private AddOp currentAdd;
    private final LinkedList<AddOp> pendingAdds = new LinkedList<AddOp>();

    // ---- notification serialization (one notifyCharacteristicChanged per
    // onNotificationSent)
    private static final class NotifyOp {
        final android.bluetooth.BluetoothGattCharacteristic characteristic;
        final byte[] value;
        final boolean confirm;
        final List<android.bluetooth.BluetoothDevice> targets;
        final AsyncResource<Boolean> out;
        int index;

        NotifyOp(android.bluetooth.BluetoothGattCharacteristic characteristic,
                byte[] value, boolean confirm,
                List<android.bluetooth.BluetoothDevice> targets,
                AsyncResource<Boolean> out) {
            this.characteristic = characteristic;
            this.value = value;
            this.confirm = confirm;
            this.targets = targets;
            this.out = out;
        }
    }

    private NotifyOp currentNotify;
    private final LinkedList<NotifyOp> pendingNotifies =
            new LinkedList<NotifyOp>();

    AndroidGattServerImpl(GattServerListener listener) {
        super(listener);
        Context ctx = AndroidImplementation.getContext();
        android.bluetooth.BluetoothManager mgr = AndroidBluetooth.manager();
        if (ctx == null || mgr == null) {
            throw new RuntimeException("Bluetooth is unavailable");
        }
        android.bluetooth.BluetoothGattServer s =
                mgr.openGattServer(ctx, serverCallback);
        if (s == null) {
            throw new RuntimeException(
                    "The platform failed to open a GATT server "
                            + "(adapter off?)");
        }
        server = s;
    }

    // ------------------------------------------------------------------
    // port SPI
    // ------------------------------------------------------------------

    @Override
    protected void doAddService(GattLocalService service,
            AsyncResource<Boolean> out) {
        android.bluetooth.BluetoothGattService ps = buildPlatformService(
                service);
        AddOp op = new AddOp(ps, out);
        boolean startNow;
        synchronized (serverLock) {
            serviceMap.put(service, ps);
            if (currentAdd == null) {
                currentAdd = op;
                startNow = true;
            } else {
                pendingAdds.add(op);
                startNow = false;
            }
        }
        if (startNow) {
            startAdd(op);
        }
    }

    private void startAdd(AddOp op) {
        boolean started;
        try {
            started = server.addService(op.service);
        } catch (SecurityException se) {
            started = false;
        } catch (Throwable ex) {
            started = false;
        }
        if (!started) {
            if (!op.out.isDone()) {
                op.out.error(new BluetoothException(BluetoothError.UNKNOWN,
                        "The platform rejected the service addition"));
            }
            advanceAdd(op);
        }
    }

    private void advanceAdd(AddOp finished) {
        AddOp next;
        synchronized (serverLock) {
            if (currentAdd != finished) {
                return;
            }
            currentAdd = pendingAdds.poll();
            next = currentAdd;
        }
        if (next != null) {
            startAdd(next);
        }
    }

    private android.bluetooth.BluetoothGattService buildPlatformService(
            GattLocalService service) {
        android.bluetooth.BluetoothGattService ps =
                new android.bluetooth.BluetoothGattService(
                        AndroidBluetooth.toPlatformUuid(service.getUuid()),
                        service.isPrimary()
                                ? android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
                                : android.bluetooth.BluetoothGattService.SERVICE_TYPE_SECONDARY);
        for (GattLocalCharacteristic lc : service.getCharacteristics()) {
            // the core PROPERTY_* / PERMISSION_* bits mirror Android's values
            android.bluetooth.BluetoothGattCharacteristic pc =
                    new android.bluetooth.BluetoothGattCharacteristic(
                            AndroidBluetooth.toPlatformUuid(lc.getUuid()),
                            lc.getProperties(), lc.getPermissions());
            if (lc.getValue() != null) {
                pc.setValue(lc.getValue());
            }
            boolean hasCccd = false;
            for (GattLocalDescriptor ld : lc.getDescriptors()) {
                android.bluetooth.BluetoothGattDescriptor pd =
                        new android.bluetooth.BluetoothGattDescriptor(
                                AndroidBluetooth.toPlatformUuid(ld.getUuid()),
                                ld.getPermissions());
                if (ld.getValue() != null) {
                    pd.setValue(ld.getValue());
                }
                pc.addDescriptor(pd);
                if (AndroidBluetooth.CCCD_UUID.equals(pd.getUuid())) {
                    hasCccd = true;
                    synchronized (serverLock) {
                        autoCccds.add(pd);
                    }
                } else {
                    synchronized (serverLock) {
                        descMap.put(pd, ld);
                    }
                }
            }
            int notifyBits = GattCharacteristic.PROPERTY_NOTIFY
                    | GattCharacteristic.PROPERTY_INDICATE;
            if (!hasCccd && (lc.getProperties() & notifyBits) != 0) {
                // Android requires an explicit CCCD on the server side for
                // centrals to arm notifications -- inject one
                android.bluetooth.BluetoothGattDescriptor cccd =
                        new android.bluetooth.BluetoothGattDescriptor(
                                AndroidBluetooth.CCCD_UUID,
                                android.bluetooth.BluetoothGattDescriptor.PERMISSION_READ
                                        | android.bluetooth.BluetoothGattDescriptor.PERMISSION_WRITE);
                pc.addDescriptor(cccd);
                synchronized (serverLock) {
                    autoCccds.add(cccd);
                }
            }
            ps.addCharacteristic(pc);
            synchronized (serverLock) {
                charMap.put(pc, lc);
                charReverse.put(lc, pc);
            }
        }
        return ps;
    }

    @Override
    protected void doNotify(BleCentral central,
            GattLocalCharacteristic characteristic, byte[] value,
            boolean confirm, AsyncResource<Boolean> out) {
        android.bluetooth.BluetoothGattCharacteristic pc;
        synchronized (serverLock) {
            pc = charReverse.get(characteristic);
        }
        if (pc == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "The characteristic was not added to this server"));
            return;
        }
        ArrayList<android.bluetooth.BluetoothDevice> targets =
                new ArrayList<android.bluetooth.BluetoothDevice>();
        synchronized (serverLock) {
            if (central != null) {
                AndroidCentral ac = centrals.get(central.getAddress());
                if (ac != null) {
                    targets.add(ac.device);
                }
            } else {
                HashSet<String> subs = subscribers.get(pc);
                if (subs != null) {
                    for (String address : subs) {
                        AndroidCentral ac = centrals.get(address);
                        if (ac != null) {
                            targets.add(ac.device);
                        }
                    }
                }
            }
        }
        if (targets.isEmpty()) {
            // nothing to send -- either no subscribed central or the target
            // central disconnected
            out.complete(Boolean.TRUE);
            return;
        }
        byte[] copy;
        if (value == null) {
            copy = new byte[0];
        } else {
            copy = new byte[value.length];
            System.arraycopy(value, 0, copy, 0, value.length);
        }
        NotifyOp op = new NotifyOp(pc, copy, confirm, targets, out);
        boolean startNow;
        synchronized (serverLock) {
            if (currentNotify == null) {
                currentNotify = op;
                startNow = true;
            } else {
                pendingNotifies.add(op);
                startNow = false;
            }
        }
        if (startNow) {
            sendCurrentNotification(op);
        }
    }

    private void sendCurrentNotification(NotifyOp op) {
        android.bluetooth.BluetoothDevice target = op.targets.get(op.index);
        boolean started;
        try {
            op.characteristic.setValue(op.value);
            started = server.notifyCharacteristicChanged(target,
                    op.characteristic, op.confirm);
        } catch (SecurityException se) {
            started = false;
        } catch (Throwable ex) {
            started = false;
        }
        if (!started) {
            if (!op.out.isDone()) {
                op.out.error(new BluetoothException(BluetoothError.UNKNOWN,
                        "The platform rejected the notification"));
            }
            advanceNotify(op);
        }
    }

    private void advanceNotify(NotifyOp finished) {
        NotifyOp next;
        synchronized (serverLock) {
            if (currentNotify != finished) {
                return;
            }
            currentNotify = pendingNotifies.poll();
            next = currentNotify;
        }
        if (next != null) {
            sendCurrentNotification(next);
        }
    }

    // ------------------------------------------------------------------
    // public surface
    // ------------------------------------------------------------------

    @Override
    public void removeService(GattLocalService service) {
        android.bluetooth.BluetoothGattService ps;
        synchronized (serverLock) {
            ps = serviceMap.remove(service);
            if (ps != null) {
                for (android.bluetooth.BluetoothGattCharacteristic pc
                        : ps.getCharacteristics()) {
                    GattLocalCharacteristic lc = charMap.remove(pc);
                    if (lc != null) {
                        charReverse.remove(lc);
                    }
                    subscribers.remove(pc);
                    for (android.bluetooth.BluetoothGattDescriptor pd
                            : pc.getDescriptors()) {
                        descMap.remove(pd);
                        autoCccds.remove(pd);
                    }
                }
            }
        }
        if (ps != null) {
            try {
                server.removeService(ps);
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public void close() {
        synchronized (serverLock) {
            serviceMap.clear();
            charMap.clear();
            charReverse.clear();
            descMap.clear();
            autoCccds.clear();
            subscribers.clear();
            centrals.clear();
        }
        try {
            server.close();
        } catch (Throwable ignore) {
        }
    }

    @Override
    public List<BleCentral> getConnectedCentrals() {
        ArrayList<BleCentral> out = new ArrayList<BleCentral>();
        synchronized (serverLock) {
            out.addAll(centrals.values());
        }
        return out;
    }

    // ------------------------------------------------------------------
    // platform callback -- binder threads
    // ------------------------------------------------------------------

    private AndroidCentral centralFor(
            android.bluetooth.BluetoothDevice device) {
        synchronized (serverLock) {
            AndroidCentral c = centrals.get(device.getAddress());
            if (c == null) {
                c = new AndroidCentral(device);
                centrals.put(device.getAddress(), c);
            }
            return c;
        }
    }

    private void sendResponse(android.bluetooth.BluetoothDevice device,
            int requestId, int status, int offset, byte[] value) {
        try {
            server.sendResponse(device, requestId, status, offset, value);
        } catch (Throwable ignore) {
        }
    }

    private static byte[] slice(byte[] value, int offset) {
        if (value == null) {
            return new byte[0];
        }
        if (offset <= 0) {
            return value;
        }
        if (offset >= value.length) {
            return new byte[0];
        }
        byte[] out = new byte[value.length - offset];
        System.arraycopy(value, offset, out, 0, out.length);
        return out;
    }

    private final android.bluetooth.BluetoothGattServerCallback
            serverCallback = new android.bluetooth.BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(
                android.bluetooth.BluetoothDevice device, int status,
                int newState) {
            if (newState
                    == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                fireCentralConnected(centralFor(device));
            } else if (newState
                    == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                AndroidCentral central;
                synchronized (serverLock) {
                    central = centrals.remove(device.getAddress());
                    for (HashSet<String> subs : subscribers.values()) {
                        subs.remove(device.getAddress());
                    }
                }
                if (central != null) {
                    fireCentralDisconnected(central);
                }
            }
        }

        @Override
        public void onServiceAdded(int status,
                android.bluetooth.BluetoothGattService service) {
            AddOp op;
            synchronized (serverLock) {
                op = currentAdd;
            }
            if (op == null) {
                return;
            }
            if (!op.out.isDone()) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    op.out.complete(Boolean.TRUE);
                } else {
                    op.out.error(new BluetoothException(
                            BluetoothError.GATT_ERROR,
                            "The platform failed to register the service "
                                    + "(status " + status + ")", status));
                }
            }
            advanceAdd(op);
        }

        @Override
        public void onCharacteristicReadRequest(
                android.bluetooth.BluetoothDevice device, int requestId,
                int offset,
                android.bluetooth.BluetoothGattCharacteristic pc) {
            GattLocalCharacteristic lc;
            synchronized (serverLock) {
                lc = charMap.get(pc);
            }
            if (lc == null) {
                sendResponse(device, requestId,
                        GattStatus.INVALID_HANDLE.getAttCode(), offset, null);
                return;
            }
            if (lc.getValue() != null) {
                // static value -- serve without involving the listener
                sendResponse(device, requestId,
                        android.bluetooth.BluetoothGatt.GATT_SUCCESS, offset,
                        slice(lc.getValue(), offset));
                return;
            }
            fireCharacteristicReadRequest(new AndroidReadRequest(
                    centralFor(device), lc, null, offset, device, requestId));
        }

        @Override
        public void onCharacteristicWriteRequest(
                android.bluetooth.BluetoothDevice device, int requestId,
                android.bluetooth.BluetoothGattCharacteristic pc,
                boolean preparedWrite, boolean responseNeeded, int offset,
                byte[] value) {
            GattLocalCharacteristic lc;
            synchronized (serverLock) {
                lc = charMap.get(pc);
            }
            if (lc == null) {
                if (responseNeeded) {
                    sendResponse(device, requestId,
                            GattStatus.INVALID_HANDLE.getAttCode(), offset,
                            null);
                }
                return;
            }
            if (preparedWrite) {
                // reliable/queued writes are not part of the portable API
                if (responseNeeded) {
                    sendResponse(device, requestId,
                            GattStatus.REQUEST_NOT_SUPPORTED.getAttCode(),
                            offset, null);
                }
                return;
            }
            fireCharacteristicWriteRequest(new AndroidWriteRequest(
                    centralFor(device), lc, null, copy(value), offset,
                    responseNeeded, device, requestId));
        }

        @Override
        public void onDescriptorReadRequest(
                android.bluetooth.BluetoothDevice device, int requestId,
                int offset, android.bluetooth.BluetoothGattDescriptor pd) {
            boolean isCccd;
            GattLocalDescriptor ld;
            boolean subscribed;
            synchronized (serverLock) {
                isCccd = autoCccds.contains(pd);
                ld = descMap.get(pd);
                HashSet<String> subs = subscribers.get(pd.getCharacteristic());
                subscribed = subs != null
                        && subs.contains(device.getAddress());
            }
            if (isCccd) {
                byte[] v = subscribed
                        ? android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                sendResponse(device, requestId,
                        android.bluetooth.BluetoothGatt.GATT_SUCCESS, offset,
                        slice(v, offset));
                return;
            }
            if (ld == null) {
                sendResponse(device, requestId,
                        GattStatus.INVALID_HANDLE.getAttCode(), offset, null);
                return;
            }
            if (ld.getValue() != null) {
                sendResponse(device, requestId,
                        android.bluetooth.BluetoothGatt.GATT_SUCCESS, offset,
                        slice(ld.getValue(), offset));
                return;
            }
            fireDescriptorReadRequest(new AndroidReadRequest(
                    centralFor(device), null, ld, offset, device, requestId));
        }

        @Override
        public void onDescriptorWriteRequest(
                android.bluetooth.BluetoothDevice device, int requestId,
                android.bluetooth.BluetoothGattDescriptor pd,
                boolean preparedWrite, boolean responseNeeded, int offset,
                byte[] value) {
            boolean isCccd;
            GattLocalDescriptor ld;
            GattLocalCharacteristic lc;
            synchronized (serverLock) {
                isCccd = autoCccds.contains(pd);
                ld = descMap.get(pd);
                lc = charMap.get(pd.getCharacteristic());
            }
            if (isCccd) {
                boolean enable = value != null && value.length > 0
                        && value[0] != 0;
                boolean changed;
                synchronized (serverLock) {
                    HashSet<String> subs =
                            subscribers.get(pd.getCharacteristic());
                    if (enable) {
                        if (subs == null) {
                            subs = new HashSet<String>();
                            subscribers.put(pd.getCharacteristic(), subs);
                        }
                        changed = subs.add(device.getAddress());
                    } else {
                        changed = subs != null
                                && subs.remove(device.getAddress());
                    }
                }
                if (responseNeeded) {
                    sendResponse(device, requestId,
                            android.bluetooth.BluetoothGatt.GATT_SUCCESS,
                            offset, value);
                }
                if (changed && lc != null) {
                    fireSubscriptionChanged(centralFor(device), lc, enable);
                }
                return;
            }
            if (ld == null) {
                if (responseNeeded) {
                    sendResponse(device, requestId,
                            GattStatus.INVALID_HANDLE.getAttCode(), offset,
                            null);
                }
                return;
            }
            if (preparedWrite) {
                if (responseNeeded) {
                    sendResponse(device, requestId,
                            GattStatus.REQUEST_NOT_SUPPORTED.getAttCode(),
                            offset, null);
                }
                return;
            }
            fireDescriptorWriteRequest(new AndroidWriteRequest(
                    centralFor(device), null, ld, copy(value), offset,
                    responseNeeded, device, requestId));
        }

        @Override
        public void onExecuteWrite(android.bluetooth.BluetoothDevice device,
                int requestId, boolean execute) {
            // reliable writes are rejected at the prepare stage above
            sendResponse(device, requestId,
                    GattStatus.REQUEST_NOT_SUPPORTED.getAttCode(), 0, null);
        }

        @Override
        public void onNotificationSent(
                android.bluetooth.BluetoothDevice device, int status) {
            NotifyOp op;
            synchronized (serverLock) {
                op = currentNotify;
            }
            if (op == null) {
                return;
            }
            if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                if (!op.out.isDone()) {
                    op.out.error(new BluetoothException(
                            BluetoothError.GATT_ERROR,
                            "Notification failed (status " + status + ")",
                            status));
                }
                advanceNotify(op);
                return;
            }
            op.index++;
            if (op.index < op.targets.size()) {
                sendCurrentNotification(op);
            } else {
                if (!op.out.isDone()) {
                    op.out.complete(Boolean.TRUE);
                }
                advanceNotify(op);
            }
        }

        @Override
        public void onMtuChanged(android.bluetooth.BluetoothDevice device,
                int mtu) {
            centralFor(device).updateMtu(mtu);
        }
    };

    private static byte[] copy(byte[] value) {
        if (value == null) {
            return new byte[0];
        }
        byte[] out = new byte[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    // ------------------------------------------------------------------
    // model wrappers
    // ------------------------------------------------------------------

    /** A connected central; identity is the platform device address. */
    static final class AndroidCentral extends BleCentral {
        final android.bluetooth.BluetoothDevice device;

        AndroidCentral(android.bluetooth.BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public String getAddress() {
            return device.getAddress();
        }

        void updateMtu(int mtu) {
            setMtu(mtu);
        }
    }

    /** Read-request envelope routing respond/reject to sendResponse. */
    private final class AndroidReadRequest extends GattReadRequest {
        private final android.bluetooth.BluetoothDevice device;
        private final int requestId;

        AndroidReadRequest(BleCentral central,
                GattLocalCharacteristic characteristic,
                GattLocalDescriptor descriptor, int offset,
                android.bluetooth.BluetoothDevice device, int requestId) {
            super(central, characteristic, descriptor, offset);
            this.device = device;
            this.requestId = requestId;
        }

        @Override
        public void respond(byte[] value) {
            // Android expects the long-read slice starting at the request
            // offset -- apps respond with the full value
            sendResponse(device, requestId,
                    android.bluetooth.BluetoothGatt.GATT_SUCCESS, getOffset(),
                    slice(value == null ? new byte[0] : value, getOffset()));
        }

        @Override
        public void reject(GattStatus status) {
            sendResponse(device, requestId,
                    (status == null ? GattStatus.UNLIKELY_ERROR : status)
                            .getAttCode(), getOffset(), null);
        }
    }

    /** Write-request envelope routing respond/reject to sendResponse. */
    private final class AndroidWriteRequest extends GattWriteRequest {
        private final android.bluetooth.BluetoothDevice device;
        private final int requestId;

        AndroidWriteRequest(BleCentral central,
                GattLocalCharacteristic characteristic,
                GattLocalDescriptor descriptor, byte[] value, int offset,
                boolean responseRequired,
                android.bluetooth.BluetoothDevice device, int requestId) {
            super(central, characteristic, descriptor, value, offset,
                    responseRequired);
            this.device = device;
            this.requestId = requestId;
        }

        @Override
        public void respond() {
            sendResponse(device, requestId,
                    android.bluetooth.BluetoothGatt.GATT_SUCCESS, getOffset(),
                    getValue());
        }

        @Override
        public void reject(GattStatus status) {
            sendResponse(device, requestId,
                    (status == null ? GattStatus.UNLIKELY_ERROR : status)
                            .getAttCode(), getOffset(), null);
        }
    }
}
