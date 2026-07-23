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
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Simulator {@link BlePeripheral} bridging the core GATT client contract
 * onto {@link SimulatedBluetoothStack} operations. One canonical instance
 * exists per address (cached by {@link SimulatorBleBackend}), so
 * discovered {@link GattCharacteristic} objects stay identical across the
 * scan/connect/notify lifecycle.
 */
class SimBlePeripheral extends BlePeripheral {

    private final SimulatedBluetoothStack stack;
    private final String address;
    private final Object dbLock = new Object();
    /** service|characteristic uuid -> canonical discovered instance */
    private HashMap<String, GattCharacteristic> characteristicIndex =
            new HashMap<String, GattCharacteristic>();

    SimBlePeripheral(SimulatedBluetoothStack stack, String address) {
        this.stack = stack;
        this.address = address;
        stack.setPeripheralSink(address,
                new SimulatedBluetoothStack.PeripheralSink() {
                    public void onNotification(BluetoothUuid serviceUuid,
                            BluetoothUuid characteristicUuid, byte[] value) {
                        GattCharacteristic c = canonicalCharacteristic(
                                serviceUuid, characteristicUuid);
                        if (c != null) {
                            fireNotification(c, value);
                        }
                    }

                    public void onConnectionLost(BluetoothError error,
                            String message) {
                        fireConnectionStateChanged(
                                ConnectionState.DISCONNECTED,
                                new BluetoothException(error, message));
                    }
                });
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        VirtualPeripheral p = stack.getPeripheral(address);
        return p == null ? null : p.getName();
    }

    public DeviceType getType() {
        VirtualPeripheral p = stack.getPeripheral(address);
        if (p == null) {
            return DeviceType.UNKNOWN;
        }
        if (p.isClassic()) {
            return p.isLe() ? DeviceType.DUAL : DeviceType.CLASSIC;
        }
        return DeviceType.LE;
    }

    public BondState getBondState() {
        return stack.isBonded(address) ? BondState.BONDED : BondState.NONE;
    }

    private GattCharacteristic canonicalCharacteristic(
            BluetoothUuid serviceUuid, BluetoothUuid characteristicUuid) {
        synchronized (dbLock) {
            return characteristicIndex.get(
                    serviceUuid + "|" + characteristicUuid);
        }
    }

    // ------------------------------------------------------------------
    // BlePeripheral SPI
    // ------------------------------------------------------------------

    protected void doConnect(ConnectionOptions options,
            final AsyncResource<BlePeripheral> out) {
        stack.connect(address,
                new SimulatedBluetoothStack.Callback<Boolean>() {
                    public void onSuccess(Boolean value) {
                        if (!out.isDone()) {
                            out.complete(SimBlePeripheral.this);
                        }
                    }

                    public void onError(BluetoothError error,
                            String message) {
                        if (!out.isDone()) {
                            out.error(new BluetoothException(error, message));
                        }
                    }
                });
    }

    protected void doDisconnect() {
        stack.disconnect(address,
                new SimulatedBluetoothStack.Callback<Boolean>() {
                    public void onSuccess(Boolean value) {
                        fireConnectionStateChanged(
                                ConnectionState.DISCONNECTED, null);
                    }

                    public void onError(BluetoothError error,
                            String message) {
                        // the link is gone either way
                        fireConnectionStateChanged(
                                ConnectionState.DISCONNECTED, null);
                    }
                });
    }

    protected void doDiscoverServices(
            final AsyncResource<List<GattService>> out) {
        stack.discoverServices(address,
                new SimulatedBluetoothStack.Callback<List<VirtualService>>() {
                    public void onSuccess(List<VirtualService> value) {
                        List<GattService> services = buildGattModel(value);
                        if (!out.isDone()) {
                            out.complete(services);
                        }
                    }

                    public void onError(BluetoothError error,
                            String message) {
                        if (!out.isDone()) {
                            out.error(new BluetoothException(error, message));
                        }
                    }
                });
    }

    /** Maps the virtual database to canonical core model instances. */
    private List<GattService> buildGattModel(List<VirtualService> virtual) {
        ArrayList<GattService> services = new ArrayList<GattService>();
        HashMap<String, GattCharacteristic> index =
                new HashMap<String, GattCharacteristic>();
        int size = virtual == null ? 0 : virtual.size();
        for (int i = 0; i < size; i++) {
            VirtualService vs = virtual.get(i);
            GattService s = new GattService(this, vs.getUuid(),
                    vs.isPrimary(), i);
            List<VirtualCharacteristic> chars = vs.getCharacteristics();
            int cs = chars.size();
            for (int j = 0; j < cs; j++) {
                VirtualCharacteristic vc = chars.get(j);
                GattCharacteristic c = new GattCharacteristic(s,
                        vc.getUuid(), vc.getProperties(), j);
                List<VirtualDescriptor> descs = vc.getDescriptors();
                int ds = descs.size();
                for (int k = 0; k < ds; k++) {
                    c.addDescriptor(new GattDescriptor(c,
                            descs.get(k).getUuid()));
                }
                s.addCharacteristic(c);
                index.put(vs.getUuid() + "|" + vc.getUuid(), c);
            }
            services.add(s);
        }
        synchronized (dbLock) {
            characteristicIndex = index;
        }
        return services;
    }

    protected void doReadCharacteristic(GattCharacteristic c,
            final AsyncResource<byte[]> out) {
        stack.readCharacteristic(address, c.getService().getUuid(),
                c.getUuid(), bytesCallback(out));
    }

    protected void doWriteCharacteristic(GattCharacteristic c, byte[] value,
            boolean withResponse, final AsyncResource<Boolean> out) {
        stack.writeCharacteristic(address, c.getService().getUuid(),
                c.getUuid(), value, booleanCallback(out));
    }

    protected void doReadDescriptor(GattDescriptor d,
            final AsyncResource<byte[]> out) {
        GattCharacteristic c = d.getCharacteristic();
        stack.readDescriptor(address, c.getService().getUuid(), c.getUuid(),
                d.getUuid(), bytesCallback(out));
    }

    protected void doWriteDescriptor(GattDescriptor d, byte[] value,
            final AsyncResource<Boolean> out) {
        GattCharacteristic c = d.getCharacteristic();
        stack.writeDescriptor(address, c.getService().getUuid(), c.getUuid(),
                d.getUuid(), value, booleanCallback(out));
    }

    protected void doSetNotifications(GattCharacteristic c, boolean enable,
            boolean indication, final AsyncResource<Boolean> out) {
        stack.setNotifications(address, c.getService().getUuid(), c.getUuid(),
                enable, booleanCallback(out));
    }

    protected void doReadRssi(final AsyncResource<Integer> out) {
        stack.readRssi(address, integerCallback(out));
    }

    protected void doRequestMtu(int mtu, final AsyncResource<Integer> out) {
        stack.requestMtu(address, mtu, integerCallback(out));
    }

    protected void doRequestConnectionPriority(ConnectionPriority priority,
            AsyncResource<Boolean> out) {
        // interval preferences are a no-op in the simulator
        stack.logEvent("connectionPriority", address + " " + priority);
        if (!out.isDone()) {
            out.complete(Boolean.TRUE);
        }
    }

    protected void doCreateBond(final AsyncResource<Boolean> out) {
        stack.bond(address, booleanCallback(out));
    }

    protected void doOpenL2cap(final int psm, boolean secure,
            final AsyncResource<L2capChannel> out) {
        stack.openL2capChannel(address, psm,
                new SimulatedBluetoothStack.Callback<SimStreamChannel>() {
                    public void onSuccess(SimStreamChannel value) {
                        if (!out.isDone()) {
                            out.complete(new SimL2capChannel(psm, value));
                        }
                    }

                    public void onError(BluetoothError error,
                            String message) {
                        if (!out.isDone()) {
                            out.error(new BluetoothException(error, message));
                        }
                    }
                });
    }

    // ------------------------------------------------------------------
    // callback bridges
    // ------------------------------------------------------------------

    private SimulatedBluetoothStack.Callback<byte[]> bytesCallback(
            final AsyncResource<byte[]> out) {
        return new SimulatedBluetoothStack.Callback<byte[]>() {
            public void onSuccess(byte[] value) {
                if (!out.isDone()) {
                    out.complete(value);
                }
            }

            public void onError(BluetoothError error, String message) {
                if (!out.isDone()) {
                    out.error(new BluetoothException(error, message));
                }
            }
        };
    }

    private SimulatedBluetoothStack.Callback<Boolean> booleanCallback(
            final AsyncResource<Boolean> out) {
        return new SimulatedBluetoothStack.Callback<Boolean>() {
            public void onSuccess(Boolean value) {
                if (!out.isDone()) {
                    out.complete(value);
                }
            }

            public void onError(BluetoothError error, String message) {
                if (!out.isDone()) {
                    out.error(new BluetoothException(error, message));
                }
            }
        };
    }

    private SimulatedBluetoothStack.Callback<Integer> integerCallback(
            final AsyncResource<Integer> out) {
        return new SimulatedBluetoothStack.Callback<Integer>() {
            public void onSuccess(Integer value) {
                if (!out.isDone()) {
                    out.complete(value);
                }
            }

            public void onError(BluetoothError error, String message) {
                if (!out.isDone()) {
                    out.error(new BluetoothException(error, message));
                }
            }
        };
    }
}
