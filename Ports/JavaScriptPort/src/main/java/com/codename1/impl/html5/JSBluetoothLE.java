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
package com.codename1.impl.html5;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.AdvertisementData;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.BluetoothLE;
import com.codename1.bluetooth.le.ScanFilter;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.ScanSettings;
import com.codename1.io.JSONParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BLE central role over Web Bluetooth.
 *
 * <p><b>Scanning divergence (important):</b> Web Bluetooth has no
 * free-running scan -- {@code navigator.bluetooth.requestLEScan} is
 * flag-gated/experimental, so it is not used. The only production entry
 * point is the chooser-based {@code requestDevice()}. This port therefore
 * maps {@code startScan} onto the chooser:</p>
 *
 * <ul>
 *   <li>Starting the first scan handle opens the browser's device chooser,
 *       built from the active {@link ScanSettings}' filters (serviceUuid
 *       filters become {@code services} entries, name/namePrefix map
 *       directly, manufacturerData maps to the {@code manufacturerData}
 *       filter; an address-only filter cannot be expressed and is
 *       dropped). With no usable filters the chooser runs with
 *       {@code acceptAllDevices}.</li>
 *   <li>The chooser returns <b>at most one</b> user-picked device. It is
 *       delivered as a single {@link ScanResult} with RSSI {@code 0}
 *       (unknown) and an {@link AdvertisementData} synthesized from the
 *       device name plus the filter criteria (Web Bluetooth does not
 *       expose the raw advertisement). After that the scan handle stays
 *       active but silent -- no further devices will ever arrive; call
 *       {@code stop()} when done.</li>
 *   <li>Cancelling the chooser fails the active handles with
 *       {@link BluetoothError#USER_CANCELED}.</li>
 *   <li>{@code requestDevice} requires a user gesture. Calling
 *       {@code startScan} from a button handler works out of the box (the
 *       forwarded click's transient activation usually still holds);
 *       otherwise the host defers the chooser to the next real user
 *       gesture and fails with {@code USER_CANCELED} when none arrives
 *       within 30 seconds -- it never hangs.</li>
 * </ul>
 *
 * <p><b>GATT access requires declared services:</b> Web Bluetooth only
 * allows GATT access to services listed in the chooser's filters or
 * {@code optionalServices}. This port collects the service UUIDs of ALL
 * active filters into {@code optionalServices} automatically -- JS apps
 * should always put a {@code serviceUuid} filter (or several) in their
 * {@link ScanSettings} or later {@code discoverServices()} will come back
 * empty.</p>
 */
public class JSBluetoothLE extends BluetoothLE {

    private final JSBluetooth bt;
    private final Object genLock = new Object();
    private int scanGeneration;

    JSBluetoothLE(JSBluetooth bt) {
        this.bt = bt;
    }

    /**
     * Re-obtains a peripheral seen earlier in this session (Web Bluetooth
     * device ids are per-origin and only resolvable after a chooser
     * grant); returns {@code null} for addresses this session never saw.
     */
    public BlePeripheral getPeripheral(String address) {
        return bt.peripheralOrNull(address);
    }

    // ------------------------------------------------------------------
    // scan SPI -- chooser-based, see class javadoc
    // ------------------------------------------------------------------

    protected boolean isScanSupported() {
        return bt.isLeSupported();
    }

    protected void startPlatformScan() {
        final int gen;
        synchronized (genLock) {
            gen = ++scanGeneration;
        }
        // The chooser round-trip blocks its green thread (potentially for
        // as long as the user stares at the dialog); never on the EDT.
        JSBluetooth.async(new Runnable() {
            public void run() {
                runChooser(gen);
            }
        });
    }

    protected void stopPlatformScan() {
        // A visible chooser cannot be programmatically dismissed; bump the
        // generation so a late result is dropped instead of delivered to a
        // scan session that already ended.
        synchronized (genLock) {
            scanGeneration++;
        }
    }

    private boolean isCurrentGeneration(int gen) {
        synchronized (genLock) {
            return gen == scanGeneration;
        }
    }

    private void runChooser(int gen) {
        List<Map<String, Object>> filters = extractActiveFilters();
        String optionsJson = buildRequestOptionsJson(filters);
        Map<String, Object> res = JSBluetooth.parseResult(
                JSBluetooth.nativeBtRequestDevice(optionsJson));
        if (!isCurrentGeneration(gen)) {
            return;
        }
        if (!JSBluetooth.isOk(res)) {
            fireScanFailed(JSBluetooth.toException(res,
                    BluetoothError.SCAN_FAILED, "requestDevice"));
            return;
        }
        String id = JSONParser.getString(res, "id");
        if (id == null) {
            fireScanFailed(new BluetoothException(BluetoothError.UNKNOWN,
                    "requestDevice returned no device id"));
            return;
        }
        String name = JSONParser.getString(res, "name");
        JSBlePeripheral p = bt.obtainPeripheral(id, name);
        fireScanResult(new ScanResult(p, 0, synthesizeAdvertisement(name,
                filters), true, System.currentTimeMillis()));
    }

    /**
     * Reads the criteria of every filter of every active scan handle via
     * the port-private field extractor (the core ScanFilter has no
     * getters).
     */
    private List<Map<String, Object>> extractActiveFilters() {
        ArrayList<Map<String, Object>> out =
                new ArrayList<Map<String, Object>>();
        List<ScanSettings> active = getActiveScanSettings();
        int n = active.size();
        for (int i = 0; i < n; i++) {
            List<ScanFilter> filters = active.get(i).getFilters();
            int fn = filters.size();
            for (int j = 0; j < fn; j++) {
                Map<String, Object> extracted = JSBluetooth.parseResult(
                        JSBluetooth.nativeBtExtractScanFilter(filters.get(j)));
                if (extracted != null) {
                    out.add(extracted);
                }
            }
        }
        return out;
    }

    /**
     * Builds the requestDevice options dictionary as a JSON string:
     * per-filter {@code services}/{@code name}/{@code namePrefix}/
     * {@code manufacturerData} entries, plus every filter service UUID in
     * {@code optionalServices} (required by Web Bluetooth for later GATT
     * access). No usable filters &rarr; {@code acceptAllDevices:true}.
     */
    private String buildRequestOptionsJson(List<Map<String, Object>> filters) {
        StringBuilder entries = new StringBuilder();
        ArrayList<String> optionalServices = new ArrayList<String>();
        int count = 0;
        int n = filters.size();
        for (int i = 0; i < n; i++) {
            Map<String, Object> f = filters.get(i);
            StringBuilder entry = new StringBuilder();
            String service = JSONParser.getString(f, "service");
            if (service != null) {
                entry.append("\"services\":[");
                JSBluetooth.appendJsonString(entry, service);
                entry.append(']');
                if (!optionalServices.contains(service)) {
                    optionalServices.add(service);
                }
            }
            String name = JSONParser.getString(f, "name");
            if (name != null) {
                if (entry.length() > 0) {
                    entry.append(',');
                }
                entry.append("\"name\":");
                JSBluetooth.appendJsonString(entry, name);
            }
            String namePrefix = JSONParser.getString(f, "namePrefix");
            if (namePrefix != null) {
                if (entry.length() > 0) {
                    entry.append(',');
                }
                entry.append("\"namePrefix\":");
                JSBluetooth.appendJsonString(entry, namePrefix);
            }
            int manufacturerId = JSONParser.getInt(f, "manufacturerId", -1);
            if (manufacturerId >= 0) {
                if (entry.length() > 0) {
                    entry.append(',');
                }
                entry.append("\"manufacturerData\":[{\"companyIdentifier\":")
                        .append(manufacturerId);
                appendIntArray(entry, "dataPrefix",
                        JSONParser.asList(f.get("manufacturerData")));
                appendIntArray(entry, "mask",
                        JSONParser.asList(f.get("manufacturerDataMask")));
                entry.append("}]");
            }
            // an address-only filter cannot be expressed in Web Bluetooth
            // and is dropped (the entry stays empty)
            if (entry.length() > 0) {
                if (count > 0) {
                    entries.append(',');
                }
                entries.append('{').append(entry).append('}');
                count++;
            }
        }
        StringBuilder out = new StringBuilder();
        out.append('{');
        if (count > 0) {
            out.append("\"filters\":[").append(entries).append(']');
        } else {
            out.append("\"acceptAllDevices\":true");
        }
        int os = optionalServices.size();
        if (os > 0) {
            out.append(",\"optionalServices\":[");
            for (int i = 0; i < os; i++) {
                if (i > 0) {
                    out.append(',');
                }
                JSBluetooth.appendJsonString(out, optionalServices.get(i));
            }
            out.append(']');
        }
        out.append('}');
        return out.toString();
    }

    private static void appendIntArray(StringBuilder sb, String key,
            List<Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        sb.append(",\"").append(key).append("\":[");
        int n = values.size();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object v = values.get(i);
            sb.append(v instanceof Number ? ((Number) v).intValue() & 0xFF : 0);
        }
        sb.append(']');
    }

    /**
     * Web Bluetooth exposes no advertisement payload from the chooser, so
     * the delivered ScanResult carries a synthesized AdvertisementData:
     * the device name plus the criteria of the active filters (service
     * UUIDs and manufacturer prefixes) -- exactly enough for the core
     * demultiplexer's filter matching to accept the result the app just
     * filtered for.
     */
    private AdvertisementData synthesizeAdvertisement(String name,
            List<Map<String, Object>> filters) {
        AdvertisementData ad = new AdvertisementData();
        if (name != null) {
            ad.setLocalName(name);
        }
        int n = filters.size();
        for (int i = 0; i < n; i++) {
            Map<String, Object> f = filters.get(i);
            String service = JSONParser.getString(f, "service");
            if (service != null) {
                try {
                    ad.addServiceUuid(BluetoothUuid.fromString(service));
                } catch (IllegalArgumentException ignored) {
                }
            }
            int manufacturerId = JSONParser.getInt(f, "manufacturerId", -1);
            if (manufacturerId >= 0) {
                ad.addManufacturerData(manufacturerId,
                        toBytes(JSONParser.asList(f.get("manufacturerData"))));
            }
        }
        return ad;
    }

    private static byte[] toBytes(List<Object> values) {
        if (values == null) {
            return new byte[0];
        }
        int n = values.size();
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            Object v = values.get(i);
            out[i] = v instanceof Number ? (byte) ((Number) v).intValue() : 0;
        }
        return out;
    }
}
