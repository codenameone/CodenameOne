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

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GATT server over CBPeripheralManager. Every local attribute added
 * through {@link #doAddService} is assigned an integer local id shared
 * with the native side, so the CBATTRequest / subscription callbacks can
 * address the exact GattLocal* instance without UUID ambiguity.
 *
 * <p>iOS specifics:</p>
 * <ul>
 * <li>CoreBluetooth serves descriptor reads/writes itself from the static
 * values supplied at service creation -- descriptor requests never reach
 * the listener.</li>
 * <li>The CCCD (0x2902) is managed by iOS; a user-supplied CCCD descriptor
 * is skipped when building the native service.</li>
 * <li>Centrals are visible to a CBPeripheralManager only through their
 * subscriptions, so {@code getConnectedCentrals()} lists subscribed
 * centrals and centralConnected / centralDisconnected fire on the first /
 * last subscription of a central.</li>
 * </ul>
 */
class IOSGattServer extends GattServer {

    private static int nextLocalId = 1;

    private final IOSBluetooth bt;
    private final IOSBluetoothLE le;

    private final Map<Integer, GattLocalCharacteristic> charsById =
            new HashMap<Integer, GattLocalCharacteristic>();
    private final Map<GattLocalCharacteristic, Integer> charIds =
            new HashMap<GattLocalCharacteristic, Integer>();
    private final Map<Integer, GattLocalDescriptor> descsById =
            new HashMap<Integer, GattLocalDescriptor>();
    private final Map<GattLocalService, Integer> serviceIds =
            new HashMap<GattLocalService, Integer>();

    /** Subscribed centrals by identifier, insertion ordered. */
    private final LinkedHashMap<String, IOSBleCentral> centrals =
            new LinkedHashMap<String, IOSBleCentral>();
    /** Characteristic local ids each central is subscribed to. */
    private final Map<String, HashSet<Integer>> subscriptions =
            new HashMap<String, HashSet<Integer>>();

    IOSGattServer(IOSBluetooth bt, IOSBluetoothLE le,
            GattServerListener listener) {
        super(listener);
        this.bt = bt;
        this.le = le;
    }

    private static synchronized int allocId() {
        return nextLocalId++;
    }

    // ------------------------------------------------------------------
    // GattServer SPI
    // ------------------------------------------------------------------

    @Override
    protected void doAddService(GattLocalService service,
            AsyncResource<Boolean> out) {
        StringBuilder sb = new StringBuilder();
        synchronized (charsById) {
            if (serviceIds.containsKey(service)) {
                out.error(new BluetoothException(BluetoothError.UNKNOWN,
                        "Service was already added to this server"));
                return;
            }
            int sid = allocId();
            serviceIds.put(service, Integer.valueOf(sid));
            sb.append("S|").append(sid).append('|')
                    .append(service.getUuid().toString()).append('|')
                    .append(service.isPrimary() ? '1' : '0').append('\n');
            List<GattLocalCharacteristic> chars =
                    service.getCharacteristics();
            int charCount = chars.size();
            for (int i = 0; i < charCount; i++) {
                GattLocalCharacteristic c = chars.get(i);
                int cid = allocId();
                charsById.put(Integer.valueOf(cid), c);
                charIds.put(c, Integer.valueOf(cid));
                sb.append("C|").append(cid).append('|')
                        .append(c.getUuid().toString()).append('|')
                        .append(c.getProperties()).append('|')
                        .append(c.getPermissions()).append('|')
                        .append(IOSBluetooth.bytesToHex(c.getValue()))
                        .append('\n');
                List<GattLocalDescriptor> descs = c.getDescriptors();
                int descCount = descs.size();
                for (int j = 0; j < descCount; j++) {
                    GattLocalDescriptor d = descs.get(j);
                    if (BluetoothUuid.CCCD.equals(d.getUuid())) {
                        // iOS owns the CCCD; adding it explicitly throws
                        continue;
                    }
                    int did = allocId();
                    descsById.put(Integer.valueOf(did), d);
                    sb.append("D|").append(did).append('|')
                            .append(d.getUuid().toString()).append('|')
                            .append(d.getPermissions()).append('|')
                            .append(IOSBluetooth.bytesToHex(d.getValue()))
                            .append('\n');
                }
            }
        }
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btAddService(rid, sb.toString());
    }

    @Override
    public void removeService(GattLocalService service) {
        Integer sid;
        synchronized (charsById) {
            sid = serviceIds.remove(service);
            if (sid != null) {
                List<GattLocalCharacteristic> chars =
                        service.getCharacteristics();
                int charCount = chars.size();
                for (int i = 0; i < charCount; i++) {
                    GattLocalCharacteristic c = chars.get(i);
                    Integer cid = charIds.remove(c);
                    if (cid != null) {
                        charsById.remove(cid);
                    }
                }
            }
        }
        if (sid != null) {
            bt.nativeInstance.btRemoveService(sid.intValue());
        }
    }

    @Override
    public void close() {
        synchronized (charsById) {
            charsById.clear();
            charIds.clear();
            descsById.clear();
            serviceIds.clear();
        }
        synchronized (centrals) {
            centrals.clear();
            subscriptions.clear();
        }
        bt.nativeInstance.btCloseGattServer();
        le.clearActiveServer(this);
    }

    @Override
    public List<BleCentral> getConnectedCentrals() {
        synchronized (centrals) {
            return new ArrayList<BleCentral>(centrals.values());
        }
    }

    @Override
    protected void doNotify(BleCentral central,
            GattLocalCharacteristic characteristic, byte[] value,
            boolean confirm, AsyncResource<Boolean> out) {
        Integer cid;
        synchronized (charsById) {
            cid = charIds.get(characteristic);
        }
        if (cid == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "Characteristic was not added to this server"));
            return;
        }
        // iOS picks notification vs indication from the characteristic's
        // properties; `confirm` has no per-update switch.
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btNotifyValue(rid, cid.intValue(),
                value == null ? new byte[0] : value,
                central == null ? null : central.getAddress());
    }

    // ------------------------------------------------------------------
    // native event plumbing
    // ------------------------------------------------------------------

    void readRequestFromNative(long handle, String centralId, int charLocalId,
            int descLocalId, int offset) {
        GattLocalCharacteristic c;
        GattLocalDescriptor d;
        synchronized (charsById) {
            c = charsById.get(Integer.valueOf(charLocalId));
            d = descLocalId >= 0
                    ? descsById.get(Integer.valueOf(descLocalId)) : null;
        }
        if (c == null && d == null) {
            bt.nativeInstance.btRespondToReadRequest(handle, null,
                    GattStatus.INVALID_HANDLE.getAttCode());
            return;
        }
        IOSGattReadRequest rq = new IOSGattReadRequest(bt, handle,
                centralFor(centralId, 0), c, d, offset);
        if (d != null) {
            fireDescriptorReadRequest(rq);
        } else {
            fireCharacteristicReadRequest(rq);
        }
    }

    void writeRequestFromNative(long handle, String centralId,
            int charLocalId, int descLocalId, byte[] value, int offset,
            boolean responseRequired) {
        GattLocalCharacteristic c;
        GattLocalDescriptor d;
        synchronized (charsById) {
            c = charsById.get(Integer.valueOf(charLocalId));
            d = descLocalId >= 0
                    ? descsById.get(Integer.valueOf(descLocalId)) : null;
        }
        if (c == null && d == null) {
            if (responseRequired) {
                bt.nativeInstance.btRespondToWriteRequest(handle,
                        GattStatus.INVALID_HANDLE.getAttCode());
            }
            return;
        }
        IOSGattWriteRequest rq = new IOSGattWriteRequest(bt, handle,
                centralFor(centralId, 0), c, d, value, offset,
                responseRequired);
        if (d != null) {
            fireDescriptorWriteRequest(rq);
        } else {
            fireCharacteristicWriteRequest(rq);
        }
    }

    void subscriptionFromNative(String centralId, int centralMtu,
            int charLocalId, boolean subscribed) {
        GattLocalCharacteristic c;
        synchronized (charsById) {
            c = charsById.get(Integer.valueOf(charLocalId));
        }
        if (c == null) {
            return;
        }
        IOSBleCentral central;
        boolean firstSubscription = false;
        boolean lastSubscription = false;
        synchronized (centrals) {
            central = centrals.get(centralId);
            HashSet<Integer> subs = subscriptions.get(centralId);
            if (subscribed) {
                if (central == null) {
                    central = new IOSBleCentral(centralId);
                    centrals.put(centralId, central);
                    firstSubscription = true;
                }
                if (subs == null) {
                    subs = new HashSet<Integer>();
                    subscriptions.put(centralId, subs);
                }
                subs.add(Integer.valueOf(charLocalId));
            } else {
                if (central == null) {
                    central = new IOSBleCentral(centralId);
                }
                if (subs != null) {
                    subs.remove(Integer.valueOf(charLocalId));
                    if (subs.isEmpty()) {
                        subscriptions.remove(centralId);
                        centrals.remove(centralId);
                        lastSubscription = true;
                    }
                }
            }
            if (centralMtu > 0) {
                central.mtuFromNative(centralMtu);
            }
        }
        if (firstSubscription) {
            fireCentralConnected(central);
        }
        fireSubscriptionChanged(central, c, subscribed);
        if (lastSubscription) {
            fireCentralDisconnected(central);
        }
    }

    private IOSBleCentral centralFor(String centralId, int mtu) {
        synchronized (centrals) {
            IOSBleCentral central = centrals.get(centralId);
            if (central == null) {
                // reads/writes can arrive from centrals that never
                // subscribed; represent them without registering
                central = new IOSBleCentral(centralId);
            }
            if (mtu > 0) {
                central.mtuFromNative(mtu);
            }
            return central;
        }
    }

    // ------------------------------------------------------------------
    // helper types
    // ------------------------------------------------------------------

    static final class IOSBleCentral extends BleCentral {
        private final String id;

        IOSBleCentral(String id) {
            this.id = id;
        }

        @Override
        public String getAddress() {
            return id;
        }

        void mtuFromNative(int mtu) {
            setMtu(mtu);
        }
    }

    static final class IOSGattReadRequest extends GattReadRequest {
        private final IOSBluetooth bt;
        private final long handle;

        IOSGattReadRequest(IOSBluetooth bt, long handle, BleCentral central,
                GattLocalCharacteristic characteristic,
                GattLocalDescriptor descriptor, int offset) {
            super(central, characteristic, descriptor, offset);
            this.bt = bt;
            this.handle = handle;
        }

        @Override
        public void respond(byte[] value) {
            bt.nativeInstance.btRespondToReadRequest(handle,
                    value == null ? new byte[0] : value, 0);
        }

        @Override
        public void reject(GattStatus status) {
            bt.nativeInstance.btRespondToReadRequest(handle, null,
                    status == null
                            ? GattStatus.UNLIKELY_ERROR.getAttCode()
                            : status.getAttCode());
        }
    }

    static final class IOSGattWriteRequest extends GattWriteRequest {
        private final IOSBluetooth bt;
        private final long handle;
        private final boolean needsResponse;

        IOSGattWriteRequest(IOSBluetooth bt, long handle, BleCentral central,
                GattLocalCharacteristic characteristic,
                GattLocalDescriptor descriptor, byte[] value, int offset,
                boolean responseRequired) {
            super(central, characteristic, descriptor, value, offset,
                    responseRequired);
            this.bt = bt;
            this.handle = handle;
            this.needsResponse = responseRequired;
        }

        @Override
        public void respond() {
            if (needsResponse) {
                bt.nativeInstance.btRespondToWriteRequest(handle, 0);
            }
        }

        @Override
        public void reject(GattStatus status) {
            if (needsResponse) {
                bt.nativeInstance.btRespondToWriteRequest(handle,
                        status == null
                                ? GattStatus.UNLIKELY_ERROR.getAttCode()
                                : status.getAttCode());
            }
        }
    }
}
