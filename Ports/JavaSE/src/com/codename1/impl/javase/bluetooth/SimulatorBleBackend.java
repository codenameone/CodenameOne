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

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.AdvertisementData;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.server.AdvertiseData;
import com.codename1.bluetooth.le.server.AdvertiseSettings;
import com.codename1.bluetooth.le.server.BleAdvertisement;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link BleBackend} over {@link SimulatedBluetoothStack} -- the
 * simulator's default BLE engine. Owns the canonical
 * {@link SimBlePeripheral} per address and translates
 * {@link VirtualPeripheral} sightings into core {@link ScanResult}s.
 */
class SimulatorBleBackend implements BleBackend {

    /** The backend name reported by {@link #getName()}. */
    static final String NAME = "simulator";

    private final SimulatedBluetoothStack stack;
    private final HashMap<String, SimBlePeripheral> peripheralCache =
            new HashMap<String, SimBlePeripheral>();
    private Object scanToken;

    SimulatorBleBackend(SimulatedBluetoothStack stack) {
        this.stack = stack;
    }

    SimulatedBluetoothStack getStack() {
        return stack;
    }

    public String getName() {
        return NAME;
    }

    public boolean isLeSupported() {
        return true;
    }

    public boolean isPeripheralModeSupported() {
        return true;
    }

    public boolean isClassicSupported() {
        return true;
    }

    public boolean isL2capSupported() {
        return true;
    }

    public AdapterState getAdapterState() {
        return stack.isAdapterEnabled() ? AdapterState.POWERED_ON
                : AdapterState.POWERED_OFF;
    }

    public void setAdapterStateSink(final AdapterStateSink sink) {
        stack.setAdapterListener(sink == null ? null
                : new SimulatedBluetoothStack.AdapterListener() {
                    public void adapterEnabledChanged(boolean enabled) {
                        sink.adapterStateChanged(enabled
                                ? AdapterState.POWERED_ON
                                : AdapterState.POWERED_OFF);
                    }
                });
    }

    /** The canonical peripheral wrapper for the address. */
    SimBlePeripheral peripheral(String address) {
        synchronized (peripheralCache) {
            SimBlePeripheral p = peripheralCache.get(address);
            if (p == null) {
                p = new SimBlePeripheral(stack, address);
                peripheralCache.put(address, p);
            }
            return p;
        }
    }

    public synchronized void startScan(final ScanSink sink) {
        stopScan();
        scanToken = stack.startScanFeed(
                new SimulatedBluetoothStack.ScanFeedSink() {
                    public void onSighting(VirtualPeripheral p,
                            long timestamp) {
                        sink.onResult(buildScanResult(p, timestamp));
                    }

                    public void onScanFailed(BluetoothError error,
                            String message) {
                        sink.onFailed(new BluetoothException(error, message));
                    }
                });
    }

    public synchronized void stopScan() {
        if (scanToken != null) {
            stack.stopScanFeed(scanToken);
            scanToken = null;
        }
    }

    private ScanResult buildScanResult(VirtualPeripheral p, long timestamp) {
        AdvertisementData ad = new AdvertisementData();
        ad.setLocalName(p.getName());
        List<BluetoothUuid> uuids = p.getAdvertisedServiceUuids();
        int size = uuids.size();
        for (int i = 0; i < size; i++) {
            ad.addServiceUuid(uuids.get(i));
        }
        for (Map.Entry<Integer, byte[]> e
                : p.getManufacturerData().entrySet()) {
            ad.addManufacturerData(e.getKey().intValue(), e.getValue());
        }
        for (Map.Entry<BluetoothUuid, byte[]> e
                : p.getServiceData().entrySet()) {
            ad.addServiceData(e.getKey(), e.getValue());
        }
        ad.setTxPowerLevel(p.getTxPower());
        return new ScanResult(peripheral(p.getAddress()), p.getRssi(), ad,
                p.isConnectable(), timestamp);
    }

    public BlePeripheral getPeripheral(String address) {
        return stack.isPeripheralRegistered(address)
                ? peripheral(address) : null;
    }

    public List<BlePeripheral> getConnectedPeripherals(
            BluetoothUuid serviceFilter) {
        ArrayList<BlePeripheral> out = new ArrayList<BlePeripheral>();
        List<String> addresses = stack.getConnectedAddresses();
        int size = addresses.size();
        for (int i = 0; i < size; i++) {
            String address = addresses.get(i);
            if (serviceFilter != null && !offersService(address,
                    serviceFilter)) {
                continue;
            }
            out.add(peripheral(address));
        }
        return out;
    }

    private boolean offersService(String address, BluetoothUuid serviceUuid) {
        VirtualPeripheral p = stack.getPeripheral(address);
        if (p == null) {
            return false;
        }
        return p.getService(serviceUuid) != null
                || p.getAdvertisedServiceUuids().contains(serviceUuid);
    }

    public List<BlePeripheral> getBondedPeripherals() {
        ArrayList<BlePeripheral> out = new ArrayList<BlePeripheral>();
        List<String> addresses = stack.getBondedAddresses();
        int size = addresses.size();
        for (int i = 0; i < size; i++) {
            String address = addresses.get(i);
            VirtualPeripheral p = stack.getPeripheral(address);
            if (p != null && p.isLe()) {
                out.add(peripheral(address));
            }
        }
        return out;
    }

    public AsyncResource<GattServer> openGattServer(
            GattServerListener listener) {
        AsyncResource<GattServer> out = new AsyncResource<GattServer>();
        new SimGattServer(listener, stack).open(out);
        return out;
    }

    public AsyncResource<BleAdvertisement> startAdvertising(
            AdvertiseSettings settings, AdvertiseData data,
            AdvertiseData scanResponse) {
        final AsyncResource<BleAdvertisement> out =
                new AsyncResource<BleAdvertisement>();
        final AdvertiseSettings s = settings == null
                ? new AdvertiseSettings() : settings;
        stack.startAppAdvertising(describeAdvertisement(s, data),
                new SimulatedBluetoothStack.Callback<Object>() {
                    public void onSuccess(Object token) {
                        if (s.getTimeout() > 0) {
                            stack.stopAppAdvertisingAfter(token,
                                    s.getTimeout());
                        }
                        if (!out.isDone()) {
                            out.complete(new SimBleAdvertisement(stack,
                                    token));
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

    private static String describeAdvertisement(AdvertiseSettings settings,
            AdvertiseData data) {
        StringBuilder sb = new StringBuilder("mode=");
        sb.append(settings.getMode());
        sb.append(" connectable=").append(settings.isConnectable());
        if (data != null) {
            List<BluetoothUuid> uuids = data.getServiceUuids();
            if (!uuids.isEmpty()) {
                sb.append(" services=").append(uuids);
            }
        }
        return sb.toString();
    }

    public AsyncResource<L2capServer> openL2capServer(boolean secure) {
        final AsyncResource<L2capServer> out =
                new AsyncResource<L2capServer>();
        stack.publishAppL2capServer(
                new SimulatedBluetoothStack.Callback<Integer>() {
                    public void onSuccess(Integer psm) {
                        if (!out.isDone()) {
                            out.complete(new SimL2capServer(stack,
                                    psm.intValue()));
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

    public void shutdown() {
        stopScan();
    }
}
