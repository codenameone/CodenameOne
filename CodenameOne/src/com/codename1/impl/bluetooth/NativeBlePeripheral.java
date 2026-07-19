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
package com.codename1.impl.bluetooth;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
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
import com.codename1.impl.bluetooth.NativeBleBackend.PendingOp;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// Real-radio {@link BlePeripheral} bridging the core GATT client contract
/// onto the native `libcn1ble` engine. One canonical instance exists per
/// address (cached by {@link NativeBleBackend}), so discovered
/// {@link GattCharacteristic} objects stay identical across the
/// scan/connect/notify lifecycle.
///
/// btleplug constraints surface here: no bonding ({@link #doCreateBond}), no
/// L2CAP ({@link #doOpenL2cap}), no MTU negotiation ({@link #doRequestMtu}
/// resolves with the current value), no connection-priority hints, and RSSI
/// reads answer with the last advertisement sighting.
///
/// Internal to the native-backend implementation -- not a public API.
public class NativeBlePeripheral extends BlePeripheral {

    private final NativeBleBackend backend;
    private final String address;
    private final AtomicReference<String> name = new AtomicReference<String>();
    private final AtomicInteger lastRssi = new AtomicInteger(-127);
    private final AtomicBoolean rssiSeen = new AtomicBoolean();

    private final Object dbLock = new Object();
    /// service|characteristic uuid to canonical discovered instance
    private HashMap<String, GattCharacteristic> characteristicIndex =
            new HashMap<String, GattCharacteristic>();

    NativeBlePeripheral(NativeBleBackend backend, String address) {
        this.backend = backend;
        this.address = address;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getName() {
        return name.get();
    }

    @Override
    public DeviceType getType() {
        return DeviceType.LE;
    }

    @Override
    public BondState getBondState() {
        // btleplug exposes no bond registry
        return BondState.NONE;
    }

    // ------------------------------------------------------------------
    // backend event entry points
    // ------------------------------------------------------------------

    /// A scan sighting refreshed the advertised name / RSSI.
    void updateFromScan(String advertisedName, int rssi) {
        if (advertisedName != null && advertisedName.length() > 0) {
            name.set(advertisedName);
        }
        lastRssi.set(rssi);
        rssiSeen.set(true);
    }

    /// OS-observed link establishment (solicited or not).
    void handleConnected(String reportedName) {
        if (reportedName != null && reportedName.length() > 0) {
            name.set(reportedName);
        }
        fireConnectionStateChanged(ConnectionState.CONNECTED, null);
    }

    /// OS-observed link teardown; {@code reason} is empty when benign.
    void handleDisconnected(String reason) {
        BluetoothException cause = null;
        if (reason != null && reason.length() > 0) {
            cause = new BluetoothException(BluetoothError.CONNECTION_LOST,
                    reason);
        }
        fireConnectionStateChanged(ConnectionState.DISCONNECTED, cause);
    }

    /// The native engine died with this peripheral possibly connected.
    void handleEngineDied(BluetoothException failure) {
        fireConnectionStateChanged(ConnectionState.DISCONNECTED, failure);
    }

    /// Routes an engine notification to the canonical characteristic.
    void handleNotification(String serviceUuid, String characteristicUuid,
            String valueBase64) {
        GattCharacteristic c;
        synchronized (dbLock) {
            c = characteristicIndex.get(serviceUuid + "|"
                    + characteristicUuid);
        }
        if (c != null) {
            fireNotification(c, Json.decodeBase64(valueBase64));
        }
    }

    // ------------------------------------------------------------------
    // BlePeripheral SPI
    // ------------------------------------------------------------------

    @Override
    protected void doConnect(ConnectionOptions options,
            final AsyncResource<BlePeripheral> out) {
        backend.connectPeripheral(address, new PendingOp() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                String reportedName = Json.str(payload, "name", null);
                if (reportedName != null && reportedName.length() > 0) {
                    name.set(reportedName);
                }
                if (!out.isDone()) {
                    out.complete(NativeBlePeripheral.this);
                }
            }

            @Override
            public void onFailure(BluetoothException failure) {
                if (!out.isDone()) {
                    out.error(failure);
                }
            }
        });
    }

    @Override
    protected void doDisconnect() {
        backend.disconnectPeripheral(address, new PendingOp() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                fireConnectionStateChanged(ConnectionState.DISCONNECTED, null);
            }

            @Override
            public void onFailure(BluetoothException failure) {
                // the link is gone either way
                fireConnectionStateChanged(ConnectionState.DISCONNECTED, null);
            }
        });
    }

    @Override
    protected void doDiscoverServices(
            final AsyncResource<List<GattService>> out) {
        backend.discoverServices(address, new PendingOp() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                List<GattService> services = buildGattModel(payload);
                if (!out.isDone()) {
                    out.complete(services);
                }
            }

            @Override
            public void onFailure(BluetoothException failure) {
                if (!out.isDone()) {
                    out.error(failure);
                }
            }
        });
    }

    /// Maps the engine's discovered payload to canonical core instances.
    private List<GattService> buildGattModel(Map<String, Object> payload) {
        ArrayList<GattService> services = new ArrayList<GattService>();
        HashMap<String, GattCharacteristic> index =
                new HashMap<String, GattCharacteristic>();
        List<Object> svcArr = Json.list(payload, "services");
        int size = svcArr.size();
        for (int i = 0; i < size; i++) {
            Map<String, Object> svc = Json.map(svcArr.get(i));
            String svcUuid = Json.str(svc, "uuid", "");
            GattService s = new GattService(this, parseUuid(svcUuid),
                    Json.boolVal(svc, "primary", true), i);
            List<Object> chArr = Json.list(svc, "characteristics");
            int cs = chArr.size();
            for (int j = 0; j < cs; j++) {
                Map<String, Object> ch = Json.map(chArr.get(j));
                String chUuid = Json.str(ch, "uuid", "");
                GattCharacteristic c = new GattCharacteristic(s,
                        parseUuid(chUuid),
                        propertiesMask(Json.list(ch, "properties")), j);
                List<Object> descArr = Json.list(ch, "descriptors");
                int ds = descArr.size();
                for (int k = 0; k < ds; k++) {
                    Map<String, Object> d = Json.map(descArr.get(k));
                    c.addDescriptor(new GattDescriptor(c,
                            parseUuid(Json.str(d, "uuid", ""))));
                }
                s.addCharacteristic(c);
                index.put(svcUuid + "|" + chUuid, c);
            }
            services.add(s);
        }
        synchronized (dbLock) {
            characteristicIndex = index;
        }
        return services;
    }

    private static BluetoothUuid parseUuid(String s) {
        return BluetoothUuid.fromString(s);
    }

    /// Maps the wire property names onto the GattCharacteristic bits.
    public static int propertiesMask(List<Object> names) {
        int mask = 0;
        int size = names.size();
        for (int i = 0; i < size; i++) {
            String p = String.valueOf(names.get(i));
            if ("broadcast".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_BROADCAST;
            } else if ("read".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_READ;
            } else if ("writeWithoutResponse".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_WRITE_WITHOUT_RESPONSE;
            } else if ("write".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_WRITE;
            } else if ("notify".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_NOTIFY;
            } else if ("indicate".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_INDICATE;
            } else if ("signedWrite".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_SIGNED_WRITE;
            } else if ("extendedProps".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_EXTENDED_PROPS;
            }
        }
        return mask;
    }

    @Override
    protected void doReadCharacteristic(GattCharacteristic c,
            final AsyncResource<byte[]> out) {
        backend.readCharacteristic(address,
                c.getService().getUuid().toString(), c.getUuid().toString(),
                bytesOp(out));
    }

    @Override
    protected void doWriteCharacteristic(GattCharacteristic c, byte[] value,
            boolean withResponse, final AsyncResource<Boolean> out) {
        backend.writeCharacteristic(address,
                c.getService().getUuid().toString(), c.getUuid().toString(),
                value, !withResponse, booleanOp(out));
    }

    @Override
    protected void doReadDescriptor(GattDescriptor d,
            final AsyncResource<byte[]> out) {
        GattCharacteristic c = d.getCharacteristic();
        backend.readDescriptor(address,
                c.getService().getUuid().toString(), c.getUuid().toString(),
                d.getUuid().toString(), bytesOp(out));
    }

    @Override
    protected void doWriteDescriptor(GattDescriptor d, byte[] value,
            final AsyncResource<Boolean> out) {
        GattCharacteristic c = d.getCharacteristic();
        backend.writeDescriptor(address,
                c.getService().getUuid().toString(), c.getUuid().toString(),
                d.getUuid().toString(), value, booleanOp(out));
    }

    @Override
    protected void doSetNotifications(GattCharacteristic c, boolean enable,
            boolean indication, final AsyncResource<Boolean> out) {
        // btleplug picks notify vs indicate from the characteristic's own
        // properties; the indication flag needs no separate wire field
        backend.setNotifications(address,
                c.getService().getUuid().toString(), c.getUuid().toString(),
                enable, booleanOp(out));
    }

    @Override
    protected void doReadRssi(final AsyncResource<Integer> out) {
        backend.readRssi(address, new PendingOp() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                int rssi = Json.intVal(payload, "rssi", -127);
                lastRssi.set(rssi);
                rssiSeen.set(true);
                if (!out.isDone()) {
                    out.complete(Integer.valueOf(rssi));
                }
            }

            @Override
            public void onFailure(BluetoothException failure) {
                if (out.isDone()) {
                    return;
                }
                // the platform can't read live RSSI -- fall back to the last
                // scan sighting when one exists
                if (failure.getError() == BluetoothError.NOT_SUPPORTED
                        && rssiSeen.get()) {
                    out.complete(Integer.valueOf(lastRssi.get()));
                } else {
                    out.error(failure);
                }
            }
        });
    }

    @Override
    protected void doRequestMtu(int mtu, AsyncResource<Integer> out) {
        // btleplug negotiates/does not expose MTU control -- resolve with the
        // current value, mirroring the iOS behavior of this API
        if (!out.isDone()) {
            out.complete(Integer.valueOf(getMtu()));
        }
    }

    @Override
    protected void doRequestConnectionPriority(ConnectionPriority priority,
            AsyncResource<Boolean> out) {
        // interval preferences are platform-managed -- a successful no-op
        if (!out.isDone()) {
            out.complete(Boolean.TRUE);
        }
    }

    @Override
    protected void doCreateBond(AsyncResource<Boolean> out) {
        if (backend.engineSupports("bonding")) {
            // reserved: no current engine build advertises bonding
            if (!out.isDone()) {
                out.complete(Boolean.TRUE);
            }
            return;
        }
        if (!out.isDone()) {
            out.error(new BluetoothException(BluetoothError.BOND_FAILED,
                    "Bonding is not supported by the native backend"
                            + " (btleplug has no bonding API)"));
        }
    }

    @Override
    protected void doOpenL2cap(int psm, boolean secure,
            AsyncResource<L2capChannel> out) {
        if (!out.isDone()) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "L2CAP channels are not supported by the native backend"
                            + " (btleplug is central-only GATT)"));
        }
    }

    private static PendingOp booleanOp(final AsyncResource<Boolean> out) {
        return new PendingOp() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                if (!out.isDone()) {
                    out.complete(Boolean.TRUE);
                }
            }

            @Override
            public void onFailure(BluetoothException failure) {
                if (!out.isDone()) {
                    out.error(failure);
                }
            }
        };
    }

    /// Completion that decodes a base64 "value" payload into the raw bytes of
    /// a characteristic/descriptor read. Static so it holds no reference to
    /// the enclosing peripheral.
    private static PendingOp bytesOp(final AsyncResource<byte[]> out) {
        return new PendingOp() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                if (!out.isDone()) {
                    out.complete(Json.decodeBase64(
                            Json.str(payload, "value", "")));
                }
            }

            @Override
            public void onFailure(BluetoothException failure) {
                if (!out.isDone()) {
                    out.error(failure);
                }
            }
        };
    }
}
