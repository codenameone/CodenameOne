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

import com.codename1.bluetooth.BluetoothUuid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A scriptable remote Bluetooth device in the simulated stack: identity,
 * advertisement payload, GATT database and stream endpoints. Build one
 * fluently, register it via
 * {@link SimulatedBluetoothStack#addPeripheral(VirtualPeripheral)} (or
 * {@link BluetoothSimulator#addPeripheral(VirtualPeripheral)}) and the app
 * under test can scan for it, connect and talk to it.
 *
 * <p>Everything except the address is mutable so a debug UI can edit the
 * device live.</p>
 */
public final class VirtualPeripheral {

    private final String address;
    private String name;
    private int rssi = -60;
    private boolean connectable = true;
    private boolean le = true;
    private boolean classic;
    private boolean bonded;
    private final ArrayList<VirtualService> services =
            new ArrayList<VirtualService>();
    private final ArrayList<BluetoothUuid> advertisedServiceUuids =
            new ArrayList<BluetoothUuid>();
    private final LinkedHashMap<Integer, byte[]> manufacturerData =
            new LinkedHashMap<Integer, byte[]>();
    private final LinkedHashMap<BluetoothUuid, byte[]> serviceData =
            new LinkedHashMap<BluetoothUuid, byte[]>();
    private Integer txPower;
    private final LinkedHashMap<Integer, SimStreamHandler> l2capEndpoints =
            new LinkedHashMap<Integer, SimStreamHandler>();

    public VirtualPeripheral(String address) {
        if (address == null || address.length() == 0) {
            throw new IllegalArgumentException("address is required");
        }
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    /** Sets the advertised/cached device name; fluent. */
    public synchronized VirtualPeripheral setName(String name) {
        this.name = name;
        return this;
    }

    public synchronized String getName() {
        return name;
    }

    /** Sets the signal strength in dBm reported by scans and RSSI reads. */
    public synchronized VirtualPeripheral setRssi(int rssi) {
        this.rssi = rssi;
        return this;
    }

    public synchronized int getRssi() {
        return rssi;
    }

    /** Whether the device accepts connections; defaults to {@code true}. */
    public synchronized VirtualPeripheral setConnectable(boolean connectable) {
        this.connectable = connectable;
        return this;
    }

    public synchronized boolean isConnectable() {
        return connectable;
    }

    /** Whether the device speaks BLE; defaults to {@code true}. */
    public synchronized VirtualPeripheral setLe(boolean le) {
        this.le = le;
        return this;
    }

    public synchronized boolean isLe() {
        return le;
    }

    /**
     * Whether the device speaks classic (BR/EDR) Bluetooth -- classic
     * discovery only reports flagged devices. Defaults to {@code false}.
     */
    public synchronized VirtualPeripheral setClassic(boolean classic) {
        this.classic = classic;
        return this;
    }

    public synchronized boolean isClassic() {
        return classic;
    }

    /**
     * Pre-seeds the bond state: a peripheral registered with
     * {@code bonded == true} shows up in the bonded-device listings without
     * an explicit bond operation.
     */
    public synchronized VirtualPeripheral setBonded(boolean bonded) {
        this.bonded = bonded;
        return this;
    }

    public synchronized boolean isBonded() {
        return bonded;
    }

    /** Adds a GATT service to the device's database; fluent. */
    public synchronized VirtualPeripheral withService(VirtualService service) {
        if (service != null) {
            services.add(service);
        }
        return this;
    }

    /** The GATT services in registration order (a snapshot). */
    public synchronized List<VirtualService> getServices() {
        return new ArrayList<VirtualService>(services);
    }

    /** The first service with the given UUID, or {@code null}. */
    public synchronized VirtualService getService(BluetoothUuid uuid) {
        int size = services.size();
        for (int i = 0; i < size; i++) {
            VirtualService s = services.get(i);
            if (s.getUuid().equals(uuid)) {
                return s;
            }
        }
        return null;
    }

    /** The characteristic at the given service/characteristic UUID pair. */
    public synchronized VirtualCharacteristic getCharacteristic(
            BluetoothUuid serviceUuid, BluetoothUuid characteristicUuid) {
        VirtualService s = getService(serviceUuid);
        return s == null ? null : s.getCharacteristic(characteristicUuid);
    }

    /** Adds a service UUID to the advertisement payload; fluent. */
    public synchronized VirtualPeripheral addAdvertisedServiceUuid(
            BluetoothUuid uuid) {
        if (uuid != null && !advertisedServiceUuids.contains(uuid)) {
            advertisedServiceUuids.add(uuid);
        }
        return this;
    }

    /** The advertised service UUIDs (a snapshot). */
    public synchronized List<BluetoothUuid> getAdvertisedServiceUuids() {
        return new ArrayList<BluetoothUuid>(advertisedServiceUuids);
    }

    /** Adds manufacturer data to the advertisement payload; fluent. */
    public synchronized VirtualPeripheral addManufacturerData(int companyId,
            byte[] data) {
        manufacturerData.put(Integer.valueOf(companyId), ByteArrays.copy(data));
        return this;
    }

    /** The advertised manufacturer data, keyed by company id (a snapshot). */
    public synchronized Map<Integer, byte[]> getManufacturerData() {
        return new LinkedHashMap<Integer, byte[]>(manufacturerData);
    }

    /** Adds service data to the advertisement payload; fluent. */
    public synchronized VirtualPeripheral addServiceData(BluetoothUuid uuid,
            byte[] data) {
        if (uuid != null) {
            serviceData.put(uuid, ByteArrays.copy(data));
        }
        return this;
    }

    /** The advertised service data, keyed by service UUID (a snapshot). */
    public synchronized Map<BluetoothUuid, byte[]> getServiceData() {
        return new LinkedHashMap<BluetoothUuid, byte[]>(serviceData);
    }

    /**
     * Sets the advertised TX power in dBm; {@code null} (the default)
     * omits it from the advertisement. Fluent.
     */
    public synchronized VirtualPeripheral setTxPower(Integer txPower) {
        this.txPower = txPower;
        return this;
    }

    /** The advertised TX power in dBm, or {@code null} when absent. */
    public synchronized Integer getTxPower() {
        return txPower;
    }

    /**
     * Registers a virtual L2CAP endpoint on this device: when the app
     * opens an L2CAP channel to the given PSM, the handler receives the
     * remote side of the piped channel. Fluent.
     */
    public synchronized VirtualPeripheral withL2capEndpoint(int psm,
            SimStreamHandler handler) {
        if (handler == null) {
            l2capEndpoints.remove(Integer.valueOf(psm));
        } else {
            l2capEndpoints.put(Integer.valueOf(psm), handler);
        }
        return this;
    }

    /** The L2CAP endpoint handler at the given PSM, or {@code null}. */
    public synchronized SimStreamHandler getL2capEndpoint(int psm) {
        return l2capEndpoints.get(Integer.valueOf(psm));
    }

    public String toString() {
        return "VirtualPeripheral(" + address + ", " + getName() + ")";
    }
}
