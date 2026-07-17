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
import com.codename1.impl.javase.bluetooth.NativeBleBackend.Wire;

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
 * <p>Serialization is plain JSON without external libraries:
 * {@link #toJson()} hand-rolls the writer (indented, deterministic field
 * order) and {@link #fromJson(InputStream)} parses with the core
 * {@code com.codename1.io.JSONParser} through {@link Wire}. Format
 * (version {@value #FORMAT_VERSION}):</p>
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

    /** Serializes the fixture as indented JSON (format sketched above). */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": ").append(version);
        if (platform != null) {
            sb.append(",\n  \"platform\": \"")
                    .append(Wire.escape(platform)).append('"');
        }
        sb.append(",\n  \"devices\": [");
        int size = devices.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('\n');
            writeDevice(sb, devices.get(i));
        }
        sb.append(size == 0 ? "]\n" : "\n  ]\n").append("}\n");
        return sb.toString();
    }

    private static void writeDevice(StringBuilder sb, Device d) {
        sb.append("    {\"id\": \"").append(Wire.escape(d.id)).append('"');
        if (d.name != null) {
            sb.append(",\n     \"name\": \"").append(Wire.escape(d.name))
                    .append('"');
        }
        sb.append(",\n     \"connectable\": ").append(d.connectable);
        if (d.txPower != null) {
            sb.append(",\n     \"txPower\": ").append(d.txPower);
        }
        sb.append(",\n     \"rssiTimeline\": [");
        int size = d.rssiTimeline.size();
        for (int i = 0; i < size; i++) {
            RssiSample s = d.rssiTimeline.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("{\"relTimeMs\": ").append(s.relTimeMs)
                    .append(", \"rssi\": ").append(s.rssi).append('}');
        }
        sb.append(']');
        sb.append(",\n     \"serviceUuids\": [");
        size = d.serviceUuids.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(d.serviceUuids.get(i)).append('"');
        }
        sb.append(']');
        sb.append(",\n     \"manufacturerData\": {");
        boolean first = true;
        for (Map.Entry<Integer, byte[]> e : d.manufacturerData.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append('"').append(e.getKey()).append("\": \"")
                    .append(Wire.encodeBase64(e.getValue())).append('"');
        }
        sb.append('}');
        sb.append(",\n     \"serviceData\": {");
        first = true;
        for (Map.Entry<BluetoothUuid, byte[]> e : d.serviceData.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append('"').append(e.getKey()).append("\": \"")
                    .append(Wire.encodeBase64(e.getValue())).append('"');
        }
        sb.append('}');
        if (!d.gatt.isEmpty()) {
            sb.append(",\n     \"gatt\": [");
            int gs = d.gatt.size();
            for (int i = 0; i < gs; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('\n');
                writeService(sb, d.gatt.get(i));
            }
            sb.append("\n     ]");
        }
        sb.append('}');
    }

    private static void writeService(StringBuilder sb, ServiceRecord s) {
        sb.append("       {\"uuid\": \"").append(s.uuid)
                .append("\", \"primary\": ").append(s.primary)
                .append(", \"characteristics\": [");
        int size = s.characteristics.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('\n');
            writeCharacteristic(sb, s.characteristics.get(i));
        }
        sb.append("]}");
    }

    private static void writeCharacteristic(StringBuilder sb,
            CharacteristicRecord c) {
        sb.append("         {\"uuid\": \"").append(c.uuid)
                .append("\", \"properties\": [");
        String[] names = propertyNames(c.properties);
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(names[i]).append('"');
        }
        sb.append(']');
        if (c.value != null) {
            sb.append(", \"value\": \"").append(Wire.encodeBase64(c.value))
                    .append('"');
        }
        sb.append(", \"descriptors\": [");
        int size = c.descriptors.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("{\"uuid\": \"").append(c.descriptors.get(i).uuid)
                    .append("\"}");
        }
        sb.append("]}");
    }

    /**
     * The wire names (see {@code PROTOCOL.md}) of a
     * {@link GattCharacteristic}{@code .PROPERTY_*} bitmask; the inverse
     * of {@link NativeBlePeripheral#propertiesMask(List)}.
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
        Map<String, Object> root = Wire.parse(json);
        long version = Wire.longVal(root, "version", -1);
        if (version != FORMAT_VERSION) {
            throw new IOException("Unsupported fixture version " + version
                    + " (this build reads version " + FORMAT_VERSION + ")");
        }
        BluetoothFixture fixture = new BluetoothFixture();
        fixture.platform = Wire.str(root, "platform", null);
        List<Object> deviceArr = Wire.list(root, "devices");
        int size = deviceArr.size();
        for (int i = 0; i < size; i++) {
            fixture.addDevice(parseDevice(Wire.map(deviceArr.get(i))));
        }
        return fixture;
    }

    private static Device parseDevice(Map<String, Object> obj)
            throws IOException {
        String id = Wire.str(obj, "id", "");
        if (id.length() == 0) {
            throw new IOException("Fixture device without an id");
        }
        Device d = new Device(id);
        d.name = Wire.str(obj, "name", null);
        d.connectable = Wire.boolVal(obj, "connectable", true);
        if (obj.containsKey("txPower")) {
            d.txPower = Integer.valueOf(Wire.intVal(obj, "txPower", 0));
        }
        List<Object> timeline = Wire.list(obj, "rssiTimeline");
        int size = timeline.size();
        for (int i = 0; i < size; i++) {
            Map<String, Object> s = Wire.map(timeline.get(i));
            d.rssiTimeline.add(new RssiSample(
                    Wire.longVal(s, "relTimeMs", 0),
                    Wire.intVal(s, "rssi", -127)));
        }
        List<Object> uuids = Wire.list(obj, "serviceUuids");
        size = uuids.size();
        for (int i = 0; i < size; i++) {
            d.serviceUuids.add(parseUuid(String.valueOf(uuids.get(i))));
        }
        Map<String, Object> manufacturer =
                Wire.map(obj.get("manufacturerData"));
        for (Map.Entry<String, Object> e : manufacturer.entrySet()) {
            try {
                d.manufacturerData.put(Integer.valueOf(e.getKey()),
                        Wire.decodeBase64(String.valueOf(e.getValue())));
            } catch (NumberFormatException ex) {
                throw new IOException("Bad manufacturer company id: "
                        + e.getKey());
            }
        }
        Map<String, Object> serviceData = Wire.map(obj.get("serviceData"));
        for (Map.Entry<String, Object> e : serviceData.entrySet()) {
            d.serviceData.put(parseUuid(e.getKey()),
                    Wire.decodeBase64(String.valueOf(e.getValue())));
        }
        List<Object> gatt = Wire.list(obj, "gatt");
        size = gatt.size();
        for (int i = 0; i < size; i++) {
            d.gatt.add(parseService(Wire.map(gatt.get(i))));
        }
        return d;
    }

    private static ServiceRecord parseService(Map<String, Object> obj)
            throws IOException {
        ServiceRecord s = new ServiceRecord(
                parseUuid(Wire.str(obj, "uuid", "")));
        s.primary = Wire.boolVal(obj, "primary", true);
        List<Object> chars = Wire.list(obj, "characteristics");
        int size = chars.size();
        for (int i = 0; i < size; i++) {
            Map<String, Object> ch = Wire.map(chars.get(i));
            CharacteristicRecord c = new CharacteristicRecord(
                    parseUuid(Wire.str(ch, "uuid", "")),
                    NativeBlePeripheral.propertiesMask(
                            Wire.list(ch, "properties")));
            if (ch.containsKey("value")) {
                c.value = Wire.decodeBase64(Wire.str(ch, "value", ""));
            }
            List<Object> descriptors = Wire.list(ch, "descriptors");
            int ds = descriptors.size();
            for (int j = 0; j < ds; j++) {
                c.descriptors.add(new DescriptorRecord(parseUuid(Wire.str(
                        Wire.map(descriptors.get(j)), "uuid", ""))));
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
