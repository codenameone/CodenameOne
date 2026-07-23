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
import com.codename1.bluetooth.gatt.GattStatus;
import com.codename1.bluetooth.le.server.BleCentral;
import com.codename1.bluetooth.le.server.GattLocalCharacteristic;
import com.codename1.bluetooth.le.server.GattLocalService;
import com.codename1.bluetooth.le.server.GattReadRequest;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.bluetooth.le.server.GattWriteRequest;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Simulator {@link GattServer} over the virtual stack's app-side GATT
 * registry. Virtual centrals created via
 * {@link SimulatedBluetoothStack#connectVirtualCentral()} appear here as
 * {@link BleCentral}s and their requests flow through the core
 * {@code fire*} helpers to the application listener.
 */
class SimGattServer extends GattServer {

    private final SimulatedBluetoothStack stack;
    private final HashMap<String, SimBleCentral> centrals =
            new HashMap<String, SimBleCentral>();

    private static final class SimBleCentral extends BleCentral {
        private final String address;

        SimBleCentral(String address) {
            this.address = address;
        }

        public String getAddress() {
            return address;
        }
    }

    SimGattServer(GattServerListener listener,
            SimulatedBluetoothStack stack) {
        super(listener);
        this.stack = stack;
    }

    /** Opens the stack-side server and resolves {@code out} with this. */
    void open(final AsyncResource<GattServer> out) {
        stack.openAppGattServer(new SimulatedBluetoothStack.AppServerSink() {
            public void centralConnected(String centralAddress) {
                fireCentralConnected(central(centralAddress));
            }

            public void centralDisconnected(String centralAddress) {
                SimBleCentral central;
                synchronized (centrals) {
                    central = centrals.remove(centralAddress);
                }
                if (central != null) {
                    fireCentralDisconnected(central);
                }
            }

            public void subscriptionChanged(String centralAddress,
                    GattLocalCharacteristic characteristic,
                    boolean subscribed) {
                fireSubscriptionChanged(central(centralAddress),
                        characteristic, subscribed);
            }

            public void characteristicReadRequest(
                    final SimulatedBluetoothStack.AppReadRequest request) {
                fireCharacteristicReadRequest(new GattReadRequest(
                        central(request.getCentralAddress()),
                        request.getCharacteristic(), null, 0) {
                    public void respond(byte[] value) {
                        request.respond(value);
                    }

                    public void reject(GattStatus status) {
                        request.reject(status);
                    }
                });
            }

            public void characteristicWriteRequest(
                    final SimulatedBluetoothStack.AppWriteRequest request) {
                fireCharacteristicWriteRequest(new GattWriteRequest(
                        central(request.getCentralAddress()),
                        request.getCharacteristic(), null,
                        request.getValue(), 0, true) {
                    public void respond() {
                        request.respond();
                    }

                    public void reject(GattStatus status) {
                        request.reject(status);
                    }
                });
            }
        }, new SimulatedBluetoothStack.Callback<Boolean>() {
            public void onSuccess(Boolean value) {
                if (!out.isDone()) {
                    out.complete(SimGattServer.this);
                }
            }

            public void onError(BluetoothError error, String message) {
                if (!out.isDone()) {
                    out.error(new BluetoothException(error, message));
                }
            }
        });
    }

    private SimBleCentral central(String address) {
        synchronized (centrals) {
            SimBleCentral central = centrals.get(address);
            if (central == null) {
                central = new SimBleCentral(address);
                centrals.put(address, central);
            }
            return central;
        }
    }

    public void removeService(GattLocalService service) {
        stack.removeAppService(service);
    }

    public void close() {
        stack.closeAppGattServer();
    }

    public List<BleCentral> getConnectedCentrals() {
        ArrayList<BleCentral> out = new ArrayList<BleCentral>();
        List<String> addresses = stack.getConnectedCentralAddresses();
        int size = addresses.size();
        for (int i = 0; i < size; i++) {
            out.add(central(addresses.get(i)));
        }
        return out;
    }

    protected void doAddService(GattLocalService service,
            final AsyncResource<Boolean> out) {
        stack.addAppService(service,
                new SimulatedBluetoothStack.Callback<Boolean>() {
                    public void onSuccess(Boolean value) {
                        if (!out.isDone()) {
                            out.complete(value);
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

    protected void doNotify(BleCentral central,
            GattLocalCharacteristic characteristic, byte[] value,
            boolean confirm, final AsyncResource<Boolean> out) {
        stack.notifyAppValue(characteristic, value,
                central == null ? null : central.getAddress(),
                new SimulatedBluetoothStack.Callback<Boolean>() {
                    public void onSuccess(Boolean value) {
                        if (!out.isDone()) {
                            out.complete(value);
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
}
