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
package com.codename1.bluetooth.le;

import com.codename1.bluetooth.BluetoothUuid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/// The parsed payload of a BLE advertisement: local name, advertised
/// service UUIDs, manufacturer data, service data and TX power. Obtained
/// from [ScanResult#getAdvertisementData()].
///
/// Ports that receive raw advertisement bytes (Android, the simulator)
/// build instances via [#parse(byte[])]; ports that only get pre-parsed
/// dictionaries (iOS) populate the fields directly through the `set*` /
/// `add*` methods, which are not application API.
public class AdvertisementData {

    private static final int TYPE_FLAGS = 0x01;
    private static final int TYPE_UUID16_INCOMPLETE = 0x02;
    private static final int TYPE_UUID16_COMPLETE = 0x03;
    private static final int TYPE_UUID32_INCOMPLETE = 0x04;
    private static final int TYPE_UUID32_COMPLETE = 0x05;
    private static final int TYPE_UUID128_INCOMPLETE = 0x06;
    private static final int TYPE_UUID128_COMPLETE = 0x07;
    private static final int TYPE_NAME_SHORT = 0x08;
    private static final int TYPE_NAME_COMPLETE = 0x09;
    private static final int TYPE_TX_POWER = 0x0A;
    private static final int TYPE_SERVICE_DATA_16 = 0x16;
    private static final int TYPE_SERVICE_DATA_32 = 0x20;
    private static final int TYPE_SERVICE_DATA_128 = 0x21;
    private static final int TYPE_MANUFACTURER = 0xFF;

    private String localName;
    private final ArrayList<BluetoothUuid> serviceUuids =
            new ArrayList<BluetoothUuid>();
    private final HashMap<Integer, byte[]> manufacturerData =
            new HashMap<Integer, byte[]>();
    private final HashMap<BluetoothUuid, byte[]> serviceData =
            new HashMap<BluetoothUuid, byte[]>();
    private Integer txPowerLevel;
    private byte[] rawBytes;

    /// Creates an empty instance for ports to populate; not application
    /// API.
    public AdvertisementData() {
    }

    /// Parses raw advertisement bytes -- a sequence of
    /// `length, type, payload` AD structures -- into a populated instance.
    /// Unknown AD types are skipped; a malformed trailing structure ends
    /// parsing without failing.
    public static AdvertisementData parse(byte[] raw) {
        AdvertisementData ad = new AdvertisementData();
        ad.rawBytes = raw;
        if (raw == null) {
            return ad;
        }
        int i = 0;
        while (i < raw.length) {
            int len = raw[i] & 0xFF;
            // a structure spans len + 1 bytes: the length byte itself,
            // the type byte and len - 1 payload bytes
            if (len == 0 || i + 1 + len > raw.length) {
                break;
            }
            int type = raw[i + 1] & 0xFF;
            ad.parseStructure(type, raw, i + 2, len - 1);
            i += len + 1;
        }
        return ad;
    }

    private void parseStructure(int type, byte[] raw, int off, int len) {
        switch (type) {
            case TYPE_NAME_SHORT:
            case TYPE_NAME_COMPLETE:
                localName = utf8(raw, off, len);
                break;
            case TYPE_UUID16_INCOMPLETE:
            case TYPE_UUID16_COMPLETE:
                for (int i = 0; i + 1 < len; i += 2) {
                    addServiceUuid(BluetoothUuid.fromShort(
                            readLeInt(raw, off + i, 2)));
                }
                break;
            case TYPE_UUID32_INCOMPLETE:
            case TYPE_UUID32_COMPLETE:
                for (int i = 0; i + 3 < len; i += 4) {
                    addServiceUuid(BluetoothUuid.fromShort(
                            readLeInt(raw, off + i, 4)));
                }
                break;
            case TYPE_UUID128_INCOMPLETE:
            case TYPE_UUID128_COMPLETE:
                for (int i = 0; i + 15 < len; i += 16) {
                    addServiceUuid(readLeUuid(raw, off + i));
                }
                break;
            case TYPE_TX_POWER:
                if (len >= 1) {
                    txPowerLevel = Integer.valueOf(raw[off]);
                }
                break;
            case TYPE_MANUFACTURER:
                if (len >= 2) {
                    int companyId = readLeInt(raw, off, 2);
                    byte[] payload = new byte[len - 2];
                    System.arraycopy(raw, off + 2, payload, 0, len - 2);
                    addManufacturerData(companyId, payload);
                }
                break;
            case TYPE_SERVICE_DATA_16:
                if (len >= 2) {
                    addServiceData(
                            BluetoothUuid.fromShort(readLeInt(raw, off, 2)),
                            copy(raw, off + 2, len - 2));
                }
                break;
            case TYPE_SERVICE_DATA_32:
                if (len >= 4) {
                    addServiceData(
                            BluetoothUuid.fromShort(readLeInt(raw, off, 4)),
                            copy(raw, off + 4, len - 4));
                }
                break;
            case TYPE_SERVICE_DATA_128:
                if (len >= 16) {
                    addServiceData(readLeUuid(raw, off),
                            copy(raw, off + 16, len - 16));
                }
                break;
            case TYPE_FLAGS:
            default:
                break;
        }
    }

    private static String utf8(byte[] raw, int off, int len) {
        try {
            return new String(raw, off, len, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(raw, off, len);
        }
    }

    private static byte[] copy(byte[] raw, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(raw, off, out, 0, len);
        return out;
    }

    private static int readLeInt(byte[] raw, int off, int bytes) {
        int v = 0;
        for (int i = bytes - 1; i >= 0; i--) {
            v = (v << 8) | (raw[off + i] & 0xFF);
        }
        return v;
    }

    /// 128-bit UUIDs travel little-endian in advertisement structures.
    private static BluetoothUuid readLeUuid(byte[] raw, int off) {
        long lsb = 0;
        for (int i = 7; i >= 0; i--) {
            lsb = (lsb << 8) | (raw[off + i] & 0xFF);
        }
        long msb = 0;
        for (int i = 15; i >= 8; i--) {
            msb = (msb << 8) | (raw[off + i] & 0xFF);
        }
        return new BluetoothUuid(msb, lsb);
    }

    /// The advertised local name, or `null` when the advertisement carries
    /// none.
    public String getLocalName() {
        return localName;
    }

    /// The advertised service UUIDs; empty when none were advertised.
    public List<BluetoothUuid> getServiceUuids() {
        return new ArrayList<BluetoothUuid>(serviceUuids);
    }

    /// The manufacturer-specific payload advertised for the given company
    /// identifier, or `null`.
    public byte[] getManufacturerData(int companyId) {
        return manufacturerData.get(Integer.valueOf(companyId));
    }

    /// The company identifiers for which manufacturer data is present.
    public int[] getManufacturerIds() {
        int[] out = new int[manufacturerData.size()];
        int i = 0;
        for (Integer id : manufacturerData.keySet()) {
            out[i++] = id.intValue();
        }
        return out;
    }

    /// The service-data payload advertised for the given service UUID, or
    /// `null`.
    public byte[] getServiceData(BluetoothUuid serviceUuid) {
        return serviceData.get(serviceUuid);
    }

    /// The service UUIDs for which service data is present.
    public List<BluetoothUuid> getServiceDataUuids() {
        return new ArrayList<BluetoothUuid>(serviceData.keySet());
    }

    /// The advertised TX power level in dBm, or `null` when absent.
    public Integer getTxPowerLevel() {
        return txPowerLevel;
    }

    /// The raw advertisement bytes when the platform exposes them
    /// (Android, simulator); `null` on iOS, which only provides parsed
    /// fields.
    public byte[] getRawBytes() {
        return rawBytes;
    }

    /// Populated by ports; not application API.
    public void setLocalName(String localName) {
        this.localName = localName;
    }

    /// Populated by ports; not application API.
    public void addServiceUuid(BluetoothUuid uuid) {
        if (!serviceUuids.contains(uuid)) {
            serviceUuids.add(uuid);
        }
    }

    /// Populated by ports; not application API.
    public void addManufacturerData(int companyId, byte[] data) {
        manufacturerData.put(Integer.valueOf(companyId), data);
    }

    /// Populated by ports; not application API.
    public void addServiceData(BluetoothUuid uuid, byte[] data) {
        serviceData.put(uuid, data);
    }

    /// Populated by ports; not application API.
    public void setTxPowerLevel(Integer txPowerLevel) {
        this.txPowerLevel = txPowerLevel;
    }
}
