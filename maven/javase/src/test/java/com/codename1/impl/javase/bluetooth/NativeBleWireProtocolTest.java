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
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.impl.javase.bluetooth.NativeBleBackend.Wire;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Deterministic codec tests of the cn1-ble-helper wire protocol: command
 * serialization (including escaping), event-line parsing and the typed
 * error / adapter-state mappings. No subprocess, no hardware.
 */
public class NativeBleWireProtocolTest {

    @Test
    public void commandSerializationIsStable() {
        String line = Wire.obj().put("cmd", "connect").put("id", 7L)
                .put("address", "aa:bb:cc").line();
        Assertions.assertEquals(
                "{\"cmd\":\"connect\",\"id\":7,\"address\":\"aa:bb:cc\"}",
                line);
        String write = Wire.obj().put("cmd", "write").put("id", 12L)
                .put("value", "AQI=").put("noResponse", true).line();
        Assertions.assertEquals("{\"cmd\":\"write\",\"id\":12,"
                + "\"value\":\"AQI=\",\"noResponse\":true}", write);
    }

    @Test
    public void escapingCoversQuotesBackslashesAndControlChars() {
        Assertions.assertEquals("a\\\"b\\\\c\\nd\\re\\tf",
                Wire.escape("a\"b\\c\nd\re\tf"));
        Assertions.assertEquals("x\\u0001y", Wire.escape("x" + (char) 1 + "y"));
        Assertions.assertEquals("", Wire.escape(null));
        // escaped output must survive a JSON parse round trip
        String line = Wire.obj().put("cmd", "write")
                .put("value", "we\"ird\\pay\nload").line();
        Map<String, Object> parsed = parse(line);
        Assertions.assertEquals("we\"ird\\pay\nload",
                Wire.str(parsed, "value", null));
    }

    private static Map<String, Object> parse(String line) {
        try {
            return Wire.parse(line);
        } catch (Exception ex) {
            throw new AssertionError("parse failed: " + line, ex);
        }
    }

    @Test
    public void eventParsingExtractsTypedFields() {
        Map<String, Object> ev = parse("{\"event\":\"readResult\","
                + "\"requestId\":42,\"address\":\"aa:01\","
                + "\"rssi\":-63,\"value\":\"AQI=\",\"primary\":true}");
        Assertions.assertEquals("readResult", Wire.str(ev, "event", null));
        Assertions.assertEquals(42, Wire.longVal(ev, "requestId", -1));
        Assertions.assertEquals(-63, Wire.intVal(ev, "rssi", 0));
        Assertions.assertTrue(Wire.boolVal(ev, "primary", false));
        Assertions.assertEquals(-1, Wire.longVal(ev, "missing", -1));
        Assertions.assertArrayEquals(new byte[] {1, 2},
                Wire.decodeBase64(Wire.str(ev, "value", "")));
    }

    @Test
    public void nestedScanResultStructuresParse() {
        Map<String, Object> ev = parse("{\"event\":\"scanResult\","
                + "\"address\":\"aa:01\",\"name\":\"HR\",\"rssi\":-45,"
                + "\"serviceUuids\":[\"0000180d-0000-1000-8000-00805f9b34fb\"],"
                + "\"manufacturerData\":{\"76\":\"AQI=\"},"
                + "\"serviceData\":{}}");
        List<Object> uuids = Wire.list(ev, "serviceUuids");
        Assertions.assertEquals(1, uuids.size());
        Assertions.assertEquals("0000180d-0000-1000-8000-00805f9b34fb",
                String.valueOf(uuids.get(0)));
        Map<String, Object> manufacturer =
                Wire.map(ev.get("manufacturerData"));
        Assertions.assertArrayEquals(new byte[] {1, 2},
                Wire.decodeBase64(String.valueOf(manufacturer.get("76"))));
        Assertions.assertTrue(Wire.map(ev.get("serviceData")).isEmpty());
        Assertions.assertTrue(Wire.list(ev, "absent").isEmpty());
    }

    @Test
    public void base64RoundTrips() {
        byte[] data = new byte[] {0, 1, 2, (byte) 0xFF, 42};
        Assertions.assertArrayEquals(data,
                Wire.decodeBase64(Wire.encodeBase64(data)));
        Assertions.assertArrayEquals(new byte[0], Wire.decodeBase64(""));
        Assertions.assertArrayEquals(new byte[0], Wire.decodeBase64(null));
        Assertions.assertEquals("", Wire.encodeBase64(null));
    }

    @Test
    public void errorCodesMapToTypedBluetoothErrors() {
        Assertions.assertEquals(BluetoothError.NOT_SUPPORTED,
                NativeBleBackend.mapErrorCode("notSupported"));
        Assertions.assertEquals(BluetoothError.UNAUTHORIZED,
                NativeBleBackend.mapErrorCode("unauthorized"));
        Assertions.assertEquals(BluetoothError.POWERED_OFF,
                NativeBleBackend.mapErrorCode("poweredOff"));
        Assertions.assertEquals(BluetoothError.SCAN_FAILED,
                NativeBleBackend.mapErrorCode("scanFailed"));
        Assertions.assertEquals(BluetoothError.CONNECTION_FAILED,
                NativeBleBackend.mapErrorCode("connectFailed"));
        Assertions.assertEquals(BluetoothError.CONNECTION_FAILED,
                NativeBleBackend.mapErrorCode("unknownPeripheral"));
        Assertions.assertEquals(BluetoothError.NOT_CONNECTED,
                NativeBleBackend.mapErrorCode("notConnected"));
        Assertions.assertEquals(BluetoothError.GATT_ERROR,
                NativeBleBackend.mapErrorCode("unknownCharacteristic"));
        Assertions.assertEquals(BluetoothError.GATT_ERROR,
                NativeBleBackend.mapErrorCode("unknownDescriptor"));
        Assertions.assertEquals(BluetoothError.TIMEOUT,
                NativeBleBackend.mapErrorCode("timeout"));
        Assertions.assertEquals(BluetoothError.IO_ERROR,
                NativeBleBackend.mapErrorCode("ioError"));
        Assertions.assertEquals(BluetoothError.UNKNOWN,
                NativeBleBackend.mapErrorCode("badRequest"));
        Assertions.assertEquals(BluetoothError.UNKNOWN,
                NativeBleBackend.mapErrorCode("somethingNew"));
    }

    @Test
    public void adapterStatesMap() {
        Assertions.assertEquals(AdapterState.POWERED_ON,
                NativeBleBackend.mapAdapterState("poweredOn"));
        Assertions.assertEquals(AdapterState.POWERED_OFF,
                NativeBleBackend.mapAdapterState("poweredOff"));
        Assertions.assertEquals(AdapterState.UNSUPPORTED,
                NativeBleBackend.mapAdapterState("unsupported"));
        Assertions.assertEquals(AdapterState.UNAUTHORIZED,
                NativeBleBackend.mapAdapterState("unauthorized"));
        Assertions.assertEquals(AdapterState.UNKNOWN,
                NativeBleBackend.mapAdapterState("whatever"));
    }

    @Test
    public void characteristicPropertyNamesMapToBits() {
        int mask = NativeBlePeripheral.propertiesMask(Arrays.asList(
                (Object) "read", "notify"));
        Assertions.assertEquals(GattCharacteristic.PROPERTY_READ
                | GattCharacteristic.PROPERTY_NOTIFY, mask);
        int all = NativeBlePeripheral.propertiesMask(Arrays.asList(
                (Object) "broadcast", "read", "writeWithoutResponse",
                "write", "notify", "indicate", "signedWrite",
                "extendedProps", "unknownFutureFlag"));
        Assertions.assertEquals(GattCharacteristic.PROPERTY_BROADCAST
                | GattCharacteristic.PROPERTY_READ
                | GattCharacteristic.PROPERTY_WRITE_WITHOUT_RESPONSE
                | GattCharacteristic.PROPERTY_WRITE
                | GattCharacteristic.PROPERTY_NOTIFY
                | GattCharacteristic.PROPERTY_INDICATE
                | GattCharacteristic.PROPERTY_SIGNED_WRITE
                | GattCharacteristic.PROPERTY_EXTENDED_PROPS, all);
    }
}
