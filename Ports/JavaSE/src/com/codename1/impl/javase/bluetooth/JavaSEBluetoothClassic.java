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

import com.codename1.bluetooth.BluetoothDevice;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Simulator classic-Bluetooth role: discovery emits the classic-flagged
 * {@link VirtualPeripheral}s, RFCOMM connections and listeners ride the
 * virtual stack's piped-stream endpoints, and the bonded-device listing
 * mirrors the stack's bond registry. Classic Bluetooth is simulator-only:
 * when the owner runs the native backend every operation reports
 * unsupported.
 */
class JavaSEBluetoothClassic extends BluetoothClassic {

    private final JavaSEBluetooth owner;

    JavaSEBluetoothClassic(JavaSEBluetooth owner) {
        this.owner = owner;
    }

    private boolean isSimulatorActive() {
        return SimulatorBleBackend.NAME.equals(owner.activeBackendName());
    }

    private SimulatedBluetoothStack stack() {
        return BluetoothSimulator.getStack();
    }

    private static BluetoothException notSupported() {
        return new BluetoothException(BluetoothError.NOT_SUPPORTED,
                "Classic Bluetooth is only available on the simulator "
                        + "backend");
    }

    /**
     * Listener callbacks are documented as EDT-delivered; when the
     * simulated stack is exercised headlessly (no initialized Display)
     * they run inline on the stack's scheduler thread instead.
     */
    private static void dispatch(Runnable r) {
        if (Display.isInitialized()) {
            Display.getInstance().callSerially(r);
        } else {
            r.run();
        }
    }

    public ClassicDiscovery startDiscovery(
            final ClassicDiscoveryListener listener) {
        if (!isSimulatorActive()) {
            ClassicDiscovery d = new SimClassicDiscovery(null);
            d.error(notSupported());
            return d;
        }
        if (listener == null) {
            ClassicDiscovery d = new SimClassicDiscovery(null);
            d.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "startDiscovery requires a listener"));
            return d;
        }
        final SimulatedBluetoothStack stack = stack();
        final SimClassicDiscovery handle = new SimClassicDiscovery(stack);
        Object token = stack.startClassicDiscovery(
                new SimulatedBluetoothStack.ClassicDiscoverySink() {
                    public void onSighting(final VirtualPeripheral p) {
                        final ClassicScanResult result =
                                new ClassicScanResult(
                                        new SimClassicDevice(stack,
                                                p.getAddress()),
                                        p.getRssi(), 0, 0);
                        dispatch(new Runnable() {
                            public void run() {
                                listener.deviceDiscovered(result);
                            }
                        });
                    }

                    public void onComplete() {
                        if (!handle.isDone()) {
                            handle.complete(Boolean.TRUE);
                        }
                    }

                    public void onFailed(BluetoothError error,
                            String message) {
                        if (!handle.isDone()) {
                            handle.error(new BluetoothException(error,
                                    message));
                        }
                    }
                });
        handle.setToken(token);
        return handle;
    }

    private static final class SimClassicDiscovery extends ClassicDiscovery {
        private final SimulatedBluetoothStack stack;
        private volatile Object token;

        SimClassicDiscovery(SimulatedBluetoothStack stack) {
            this.stack = stack;
        }

        void setToken(Object token) {
            this.token = token;
        }

        protected void onStop() {
            Object t = token;
            if (stack != null && t != null) {
                stack.stopClassicDiscovery(t);
            }
        }
    }

    public List<BluetoothDevice> getBondedDevices() {
        ArrayList<BluetoothDevice> out = new ArrayList<BluetoothDevice>();
        if (!isSimulatorActive()) {
            return out;
        }
        SimulatedBluetoothStack stack = stack();
        List<String> addresses = stack.getBondedAddresses();
        int size = addresses.size();
        for (int i = 0; i < size; i++) {
            VirtualPeripheral p = stack.getPeripheral(addresses.get(i));
            if (p != null && p.isClassic()) {
                out.add(new SimClassicDevice(stack, p.getAddress()));
            }
        }
        return out;
    }

    public AsyncResource<Boolean> createBond(BluetoothDevice device) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (!isSimulatorActive()) {
            out.error(notSupported());
            return out;
        }
        if (device == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "createBond requires a device"));
            return out;
        }
        stack().bond(device.getAddress(), booleanCallback(out));
        return out;
    }

    public AsyncResource<Boolean> requestDiscoverable(int durationSeconds) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (!isSimulatorActive()) {
            out.complete(Boolean.FALSE);
            return out;
        }
        stack().logEvent("discoverable",
                "requested for " + durationSeconds + "s (granted)");
        out.complete(Boolean.TRUE);
        return out;
    }

    public AsyncResource<RfcommConnection> connect(BluetoothDevice device,
            BluetoothUuid serviceUuid, boolean secure) {
        AsyncResource<RfcommConnection> out =
                new AsyncResource<RfcommConnection>();
        if (device == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "connect requires a device"));
            return out;
        }
        connectImpl(device, serviceUuid, out);
        return out;
    }

    public AsyncResource<RfcommConnection> connect(String address,
            BluetoothUuid serviceUuid, boolean secure) {
        AsyncResource<RfcommConnection> out =
                new AsyncResource<RfcommConnection>();
        if (address == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "connect requires an address"));
            return out;
        }
        connectImpl(new SimClassicDevice(
                        isSimulatorActive() ? stack() : null, address),
                serviceUuid, out);
        return out;
    }

    private void connectImpl(final BluetoothDevice device,
            BluetoothUuid serviceUuid,
            final AsyncResource<RfcommConnection> out) {
        if (!isSimulatorActive()) {
            out.error(notSupported());
            return;
        }
        BluetoothUuid uuid = serviceUuid == null
                ? BluetoothUuid.SPP : serviceUuid;
        stack().connectRfcomm(uuid,
                new SimulatedBluetoothStack.Callback<SimStreamChannel>() {
                    public void onSuccess(SimStreamChannel value) {
                        if (!out.isDone()) {
                            out.complete(new JavaSERfcommConnection(device,
                                    value));
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

    public AsyncResource<RfcommServer> listen(String serviceName,
            BluetoothUuid serviceUuid, boolean secure) {
        final AsyncResource<RfcommServer> out =
                new AsyncResource<RfcommServer>();
        if (!isSimulatorActive()) {
            out.error(notSupported());
            return out;
        }
        final SimulatedBluetoothStack stack = stack();
        final BluetoothUuid uuid = serviceUuid == null
                ? BluetoothUuid.SPP : serviceUuid;
        stack.listenRfcomm(uuid,
                new SimulatedBluetoothStack.Callback<Boolean>() {
                    public void onSuccess(Boolean value) {
                        if (!out.isDone()) {
                            out.complete(new JavaSERfcommServer(uuid, stack));
                        }
                    }

                    public void onError(BluetoothError error,
                            String message) {
                        if (!out.isDone()) {
                            out.error(new BluetoothException(error, message));
                        }
                    }
                });
        return out;
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

    /** A classic device backed by the stack's peripheral registry. */
    private static final class SimClassicDevice extends BluetoothDevice {
        private final SimulatedBluetoothStack stack;
        private final String address;

        SimClassicDevice(SimulatedBluetoothStack stack, String address) {
            this.stack = stack;
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

        public String getName() {
            VirtualPeripheral p = stack == null
                    ? null : stack.getPeripheral(address);
            return p == null ? null : p.getName();
        }

        public DeviceType getType() {
            VirtualPeripheral p = stack == null
                    ? null : stack.getPeripheral(address);
            if (p == null) {
                return DeviceType.UNKNOWN;
            }
            if (p.isClassic()) {
                return p.isLe() ? DeviceType.DUAL : DeviceType.CLASSIC;
            }
            return DeviceType.LE;
        }

        public BondState getBondState() {
            return stack != null && stack.isBonded(address)
                    ? BondState.BONDED : BondState.NONE;
        }
    }
}
