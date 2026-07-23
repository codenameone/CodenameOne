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
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.io.JSONWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A recorded Bluetooth trace: the versioned, in-memory model behind the
 * record-and-replay fixture files under
 * {@code maven/javase/src/test/resources/bluetooth-fixtures/}. A fixture
 * captures what a scan (and optional GATT walks) observed on real
 * hardware -- device identities, RSSI timelines, advertisement payloads
 * and GATT databases with readable values -- so the exact same trace can
 * be replayed deterministically into a {@link SimulatedBluetoothStack}
 * via {@link SimulatedBluetoothStack#loadFixture(BluetoothFixture)}.
 *
 * <p>Fixtures produced by {@link FixtureRecorder} are scrambled with
 * {@link FixtureScrambler} before they are written to disk, so committed
 * traces never carry real device identities.</p>
 *
 * <p>Serialization uses the core JSON codec: {@link #toJson()} builds an
 * ordered {@code Map}/{@code List} tree and hands it to
 * {@code com.codename1.io.JSONWriter}, and {@link #fromJson(InputStream)}
 * parses with the core {@code com.codename1.io.JSONParser} through
 * {@link FixtureJson}. Field order is deterministic (backed by
 * {@link LinkedHashMap}). Format (version {@value #FORMAT_VERSION}):</p>
 *
 * <pre>
 * {"version": 1,
 *  "platform": "Mac OS X 15.x (cn1-ble-helper)",
 *  "devices": [
 *    {"id": "SC:RA:MB:4E:11:83",
 *     "name": "Device-2F41",
 *     "connectable": true,
 *     "txPower": 4,
 *     "rssiTimeline": [{"relTimeMs": 120, "rssi": -58}, ...],
 *     "serviceUuids": ["0000180d-0000-1000-8000-00805f9b34fb"],
 *     "manufacturerData": {"76": "&lt;base64&gt;"},
 *     "serviceData": {"&lt;uuid&gt;": "&lt;base64&gt;"},
 *     "gatt": [
 *       {"uuid": "...", "primary": true, "characteristics": [
 *         {"uuid": "...", "properties": ["read","notify"],
 *          "value": "&lt;base64&gt;",
 *          "descriptors": [{"uuid": "..."}]}]}]}]}
 * </pre>
 *
 * <p>Optional fields ({@code name}, {@code txPower}, {@code value}) are
 * omitted when absent; {@code gatt} is omitted when the device carries no
 * captured GATT database.</p>
 */
public final class BluetoothFixture {

    /** The fixture file format version written by {@link #toJson()}. */
    public static final int FORMAT_VERSION = 1;

    /** One RSSI sighting at a time relative to the start of the capture. */
    public static final class RssiSample {
        /** Milliseconds since the capture started. */
        public final long relTimeMs;
        /** Signal strength in dBm. */
        public final int rssi;

        public RssiSample(long relTimeMs, int rssi) {
            this.relTimeMs = relTimeMs;
            this.rssi = rssi;
        }
    }

    /** A captured GATT descriptor (identity only). */
    public static final class DescriptorRecord {
        public final BluetoothUuid uuid;

        public DescriptorRecord(BluetoothUuid uuid) {
            if (uuid == null) {
                throw new IllegalArgumentException("uuid is required");
            }
            this.uuid = uuid;
        }
    }

    /**
     * A captured GATT characteristic: identity, the
     * {@link GattCharacteristic}{@code .PROPERTY_*} bitmask, descriptors
     * and -- when the characteristic was readable during capture -- the
     * value that was read.
     */
    public static final class CharacteristicRecord {
        public final BluetoothUuid uuid;
        public final int properties;
        /** The captured readable value, or {@code null} when none. */
        public byte[] value;
        public final ArrayList<DescriptorRecord> descriptors =
                new ArrayList<DescriptorRecord>();

        public CharacteristicRecord(BluetoothUuid uuid, int properties) {
            if (uuid == null) {
                throw new IllegalArgumentException("uuid is required");
            }
            this.uuid = uuid;
            this.properties = properties;
        }
    }

    /** A captured GATT service. */
    public static final class ServiceRecord {
        public final BluetoothUuid uuid;
        public boolean primary = true;
        public final ArrayList<CharacteristicRecord> characteristics =
                new ArrayList<CharacteristicRecord>();

        public ServiceRecord(BluetoothUuid uuid) {
            if (uuid == null) {
                throw new IllegalArgumentException("uuid is required");
            }
            this.uuid = uuid;
        }
    }

    /** One observed device with its advertisement history and GATT DB. */
    public static final class Device {
        /** The device identifier (scrambled in committed fixtures). */
        public final String id;
        /** The advertised local name, or {@code null}. */
        public String name;
        public boolean connectable = true;
        /** The advertised TX power in dBm, or {@code null} when absent. */
        public Integer txPower;
        /** RSSI sightings in capture order. */
        public final ArrayList<RssiSample> rssiTimeline =
                new ArrayList<RssiSample>();
        public final ArrayList<BluetoothUuid> serviceUuids =
                new ArrayList<BluetoothUuid>();
        /** Manufacturer payloads keyed by SIG company identifier. */
        public final LinkedHashMap<Integer, byte[]> manufacturerData =
                new LinkedHashMap<Integer, byte[]>();
        /** Service-data payloads keyed by service UUID. */
        public final LinkedHashMap<BluetoothUuid, byte[]> serviceData =
                new LinkedHashMap<BluetoothUuid, byte[]>();
        /** The captured GATT database; empty when never connected. */
        public final ArrayList<ServiceRecord> gatt =
                new ArrayList<ServiceRecord>();

        public Device(String id) {
            if (id == null || id.length() == 0) {
                throw new IllegalArgumentException("id is required");
            }
            this.id = id;
        }

        /** {@code true} when a GATT database was captured. */
        public boolean hasGatt() {
            return !gatt.isEmpty();
        }

        /**
         * Materializes this record as a {@link VirtualPeripheral} ready
         * for {@link SimulatedBluetoothStack#addPeripheral}: the initial
         * RSSI is the first timeline sample and the GATT records map onto
         * the {@code Virtual*} model.
         */
        public VirtualPeripheral toVirtualPeripheral() {
            VirtualPeripheral p = new VirtualPeripheral(id)
                    .setName(name)
                    .setConnectable(connectable)
                    .setTxPower(txPower);
            if (!rssiTimeline.isEmpty()) {
                p.setRssi(rssiTimeline.get(0).rssi);
            }
            int size = serviceUuids.size();
            for (int i = 0; i < size; i++) {
                p.addAdvertisedServiceUuid(serviceUuids.get(i));
            }
            for (Map.Entry<Integer, byte[]> e : manufacturerData.entrySet()) {
                p.addManufacturerData(e.getKey().intValue(), e.getValue());
            }
            for (Map.Entry<BluetoothUuid, byte[]> e
                    : serviceData.entrySet()) {
                p.addServiceData(e.getKey(), e.getValue());
            }
            size = gatt.size();
            for (int i = 0; i < size; i++) {
                ServiceRecord sr = gatt.get(i);
                VirtualService service = new VirtualService(sr.uuid)
                        .setPrimary(sr.primary);
                int cs = sr.characteristics.size();
                for (int j = 0; j < cs; j++) {
                    CharacteristicRecord cr = sr.characteristics.get(j);
                    VirtualCharacteristic c = new VirtualCharacteristic(
                            cr.uuid, cr.properties, cr.value);
                    int ds = cr.descriptors.size();
                    for (int k = 0; k < ds; k++) {
                        c.withDescriptor(new VirtualDescriptor(
                                cr.descriptors.get(k).uuid));
                    }
                    service.withCharacteristic(c);
                }
                p.withService(service);
            }
            return p;
        }
    }

    private int version = FORMAT_VERSION;
    private String platform;
    private final ArrayList<Device> devices = new ArrayList<Device>();

    public int getVersion() {
        return version;
    }

    /** A note about the capture platform, e.g. the host OS; fluent. */
    public BluetoothFixture setPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    /** Appends a device record; fluent. */
    public BluetoothFixture addDevice(Device device) {
        if (device != null) {
            devices.add(device);
        }
        return this;
    }

    /** The device records in capture order (the live list). */
    public List<Device> getDevices() {
        return devices;
    }

    /** The device record with the given id, or {@code null}. */
    public Device getDevice(String id) {
        int size = devices.size();
        for (int i = 0; i < size; i++) {
            if (devices.get(i).id.equals(id)) {
                return devices.get(i);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // JSON writing
    // ------------------------------------------------------------------

    /**
     * Serializes the fixture as JSON via the core
     * {@code com.codename1.io.JSONWriter}, building an ordered
     * {@code Map}/{@code List} tree (format sketched above).
     */
    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("version", Integer.valueOf(version));
        if (platform != null) {
            root.put("platform", platform);
        }
        List<Object> deviceList = new ArrayList<Object>();
        int size = devices.size();
        for (int i = 0; i < size; i++) {
            deviceList.add(deviceToMap(devices.get(i)));
        }
        root.put("devices", deviceList);
        return JSONWriter.toJson(root);
    }

    private static Map<String, Object> deviceToMap(Device d) {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("id", d.id);
        if (d.name != null) {
            obj.put("name", d.name);
        }
        obj.put("connectable", Boolean.valueOf(d.connectable));
        if (d.txPower != null) {
            obj.put("txPower", d.txPower);
        }
        List<Object> timeline = new ArrayList<Object>();
        int size = d.rssiTimeline.size();
        for (int i = 0; i < size; i++) {
            RssiSample s = d.rssiTimeline.get(i);
            Map<String, Object> sample = new LinkedHashMap<String, Object>();
            sample.put("relTimeMs", Long.valueOf(s.relTimeMs));
            sample.put("rssi", Integer.valueOf(s.rssi));
            timeline.add(sample);
        }
        obj.put("rssiTimeline", timeline);
        List<Object> uuids = new ArrayList<Object>();
        size = d.serviceUuids.size();
        for (int i = 0; i < size; i++) {
            uuids.add(d.serviceUuids.get(i).toString());
        }
        obj.put("serviceUuids", uuids);
        Map<String, Object> manufacturer = new LinkedHashMap<String, Object>();
        for (Map.Entry<Integer, byte[]> e : d.manufacturerData.entrySet()) {
            manufacturer.put(String.valueOf(e.getKey()),
                    FixtureJson.encodeBase64(e.getValue()));
        }
        obj.put("manufacturerData", manufacturer);
        Map<String, Object> serviceData = new LinkedHashMap<String, Object>();
        for (Map.Entry<BluetoothUuid, byte[]> e : d.serviceData.entrySet()) {
            serviceData.put(e.getKey().toString(),
                    FixtureJson.encodeBase64(e.getValue()));
        }
        obj.put("serviceData", serviceData);
        if (!d.gatt.isEmpty()) {
            List<Object> gatt = new ArrayList<Object>();
            int gs = d.gatt.size();
            for (int i = 0; i < gs; i++) {
                gatt.add(serviceToMap(d.gatt.get(i)));
            }
            obj.put("gatt", gatt);
        }
        return obj;
    }

    private static Map<String, Object> serviceToMap(ServiceRecord s) {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("uuid", s.uuid.toString());
        obj.put("primary", Boolean.valueOf(s.primary));
        List<Object> chars = new ArrayList<Object>();
        int size = s.characteristics.size();
        for (int i = 0; i < size; i++) {
            chars.add(characteristicToMap(s.characteristics.get(i)));
        }
        obj.put("characteristics", chars);
        return obj;
    }

    private static Map<String, Object> characteristicToMap(
            CharacteristicRecord c) {
        Map<String, Object> obj = new LinkedHashMap<String, Object>();
        obj.put("uuid", c.uuid.toString());
        List<Object> props = new ArrayList<Object>();
        String[] names = propertyNames(c.properties);
        for (int i = 0; i < names.length; i++) {
            props.add(names[i]);
        }
        obj.put("properties", props);
        if (c.value != null) {
            obj.put("value", FixtureJson.encodeBase64(c.value));
        }
        List<Object> descriptors = new ArrayList<Object>();
        int size = c.descriptors.size();
        for (int i = 0; i < size; i++) {
            Map<String, Object> desc = new LinkedHashMap<String, Object>();
            desc.put("uuid", c.descriptors.get(i).uuid.toString());
            descriptors.add(desc);
        }
        obj.put("descriptors", descriptors);
        return obj;
    }

    /**
     * The wire names (see {@code PROTOCOL.md}) of a
     * {@link GattCharacteristic}{@code .PROPERTY_*} bitmask; the inverse
     * of {@link FixtureJson#propertiesMask(List)}.
     */
    static String[] propertyNames(int mask) {
        ArrayList<String> out = new ArrayList<String>();
        if ((mask & GattCharacteristic.PROPERTY_BROADCAST) != 0) {
            out.add("broadcast");
        }
        if ((mask & GattCharacteristic.PROPERTY_READ) != 0) {
            out.add("read");
        }
        if ((mask & GattCharacteristic.PROPERTY_WRITE_WITHOUT_RESPONSE)
                != 0) {
            out.add("writeWithoutResponse");
        }
        if ((mask & GattCharacteristic.PROPERTY_WRITE) != 0) {
            out.add("write");
        }
        if ((mask & GattCharacteristic.PROPERTY_NOTIFY) != 0) {
            out.add("notify");
        }
        if ((mask & GattCharacteristic.PROPERTY_INDICATE) != 0) {
            out.add("indicate");
        }
        if ((mask & GattCharacteristic.PROPERTY_SIGNED_WRITE) != 0) {
            out.add("signedWrite");
        }
        if ((mask & GattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0) {
            out.add("extendedProps");
        }
        return out.toArray(new String[out.size()]);
    }

    // ------------------------------------------------------------------
    // JSON parsing
    // ------------------------------------------------------------------

    /**
     * Parses a fixture from its JSON form. Throws {@link IOException} on
     * malformed input or an unsupported format version.
     */
    public static BluetoothFixture fromJson(InputStream in)
            throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) >= 0) {
            buffer.write(chunk, 0, n);
        }
        return fromJson(new String(buffer.toByteArray(), "UTF-8"));
    }

    /** Parses a fixture from its JSON form. */
    public static BluetoothFixture fromJson(String json) throws IOException {
        Map<String, Object> root = FixtureJson.parse(json);
        long version = FixtureJson.longVal(root, "version", -1);
        if (version != FORMAT_VERSION) {
            throw new IOException("Unsupported fixture version " + version
                    + " (this build reads version " + FORMAT_VERSION + ")");
        }
        BluetoothFixture fixture = new BluetoothFixture();
        fixture.platform = FixtureJson.str(root, "platform", null);
        List<Object> deviceArr = FixtureJson.list(root, "devices");
        int size = deviceArr.size();
        for (int i = 0; i < size; i++) {
            fixture.addDevice(parseDevice(FixtureJson.map(deviceArr.get(i))));
        }
        return fixture;
    }

    private static Device parseDevice(Map<String, Object> obj)
            throws IOException {
        String id = FixtureJson.str(obj, "id", "");
        if (id.length() == 0) {
            throw new IOException("Fixture device without an id");
        }
        Device d = new Device(id);
        d.name = FixtureJson.str(obj, "name", null);
        d.connectable = FixtureJson.boolVal(obj, "connectable", true);
        if (obj.containsKey("txPower")) {
            d.txPower = Integer.valueOf(FixtureJson.intVal(obj, "txPower", 0));
        }
        List<Object> timeline = FixtureJson.list(obj, "rssiTimeline");
        int size = timeline.size();
        for (int i = 0; i < size; i++) {
            Map<String, Object> s = FixtureJson.map(timeline.get(i));
            d.rssiTimeline.add(new RssiSample(
                    FixtureJson.longVal(s, "relTimeMs", 0),
                    FixtureJson.intVal(s, "rssi", -127)));
        }
        List<Object> uuids = FixtureJson.list(obj, "serviceUuids");
        size = uuids.size();
        for (int i = 0; i < size; i++) {
            d.serviceUuids.add(parseUuid(String.valueOf(uuids.get(i))));
        }
        Map<String, Object> manufacturer =
                FixtureJson.map(obj.get("manufacturerData"));
        for (Map.Entry<String, Object> e : manufacturer.entrySet()) {
            try {
                d.manufacturerData.put(Integer.valueOf(e.getKey()),
                        FixtureJson.decodeBase64(String.valueOf(e.getValue())));
            } catch (NumberFormatException ex) {
                throw new IOException("Bad manufacturer company id: "
                        + e.getKey());
            }
        }
        Map<String, Object> serviceData = FixtureJson.map(obj.get("serviceData"));
        for (Map.Entry<String, Object> e : serviceData.entrySet()) {
            d.serviceData.put(parseUuid(e.getKey()),
                    FixtureJson.decodeBase64(String.valueOf(e.getValue())));
        }
        List<Object> gatt = FixtureJson.list(obj, "gatt");
        size = gatt.size();
        for (int i = 0; i < size; i++) {
            d.gatt.add(parseService(FixtureJson.map(gatt.get(i))));
        }
        return d;
    }

    private static ServiceRecord parseService(Map<String, Object> obj)
            throws IOException {
        ServiceRecord s = new ServiceRecord(
                parseUuid(FixtureJson.str(obj, "uuid", "")));
        s.primary = FixtureJson.boolVal(obj, "primary", true);
        List<Object> chars = FixtureJson.list(obj, "characteristics");
        int size = chars.size();
        for (int i = 0; i < size; i++) {
            Map<String, Object> ch = FixtureJson.map(chars.get(i));
            CharacteristicRecord c = new CharacteristicRecord(
                    parseUuid(FixtureJson.str(ch, "uuid", "")),
                    FixtureJson.propertiesMask(
                            FixtureJson.list(ch, "properties")));
            if (ch.containsKey("value")) {
                c.value = FixtureJson.decodeBase64(FixtureJson.str(ch, "value", ""));
            }
            List<Object> descriptors = FixtureJson.list(ch, "descriptors");
            int ds = descriptors.size();
            for (int j = 0; j < ds; j++) {
                c.descriptors.add(new DescriptorRecord(parseUuid(FixtureJson.str(
                        FixtureJson.map(descriptors.get(j)), "uuid", ""))));
            }
            s.characteristics.add(c);
        }
        return s;
    }

    private static BluetoothUuid parseUuid(String s) throws IOException {
        try {
            return BluetoothUuid.fromString(s);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Bad UUID in fixture: " + s);
        }
    }
}
