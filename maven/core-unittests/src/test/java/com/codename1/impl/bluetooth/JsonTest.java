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
package com.codename1.impl.bluetooth;

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Deterministic codec tests of the native BLE engine's event decoder: JSON
 * event-line parsing (including nested scan-result structures), the base64
 * round trip and the typed error / adapter-state / property-mask mappings. No
 * native engine, no hardware.
 */
public class JsonTest {

    private static Map<String, Object> parse(String line) {
        try {
            return Json.parse(line);
        } catch (Exception ex) {
            throw new AssertionError("parse failed: " + line, ex);
        }
    }

    @Test
    public void eventParsingExtractsTypedFields() {
        Map<String, Object> ev = parse("{\"event\":\"readResult\","
                + "\"requestId\":42,\"address\":\"aa:01\","
                + "\"rssi\":-63,\"value\":\"AQI=\",\"primary\":true}");
        Assertions.assertEquals("readResult", Json.str(ev, "event", null));
        Assertions.assertEquals(42, Json.longVal(ev, "requestId", -1));
        Assertions.assertEquals(-63, Json.intVal(ev, "rssi", 0));
        Assertions.assertTrue(Json.boolVal(ev, "primary", false));
        Assertions.assertEquals(-1, Json.longVal(ev, "missing", -1));
        Assertions.assertArrayEquals(new byte[] {1, 2},
                Json.decodeBase64(Json.str(ev, "value", "")));
    }

    @Test
    public void nestedScanResultStructuresParse() {
        Map<String, Object> ev = parse("{\"event\":\"scanResult\","
                + "\"address\":\"aa:01\",\"name\":\"HR\",\"rssi\":-45,"
                + "\"serviceUuids\":[\"0000180d-0000-1000-8000-00805f9b34fb\"],"
                + "\"manufacturerData\":{\"76\":\"AQI=\"},"
                + "\"serviceData\":{}}");
        List<Object> uuids = Json.list(ev, "serviceUuids");
        Assertions.assertEquals(1, uuids.size());
        Assertions.assertEquals("0000180d-0000-1000-8000-00805f9b34fb",
                String.valueOf(uuids.get(0)));
        Map<String, Object> manufacturer =
                Json.map(ev.get("manufacturerData"));
        Assertions.assertArrayEquals(new byte[] {1, 2},
                Json.decodeBase64(String.valueOf(manufacturer.get("76"))));
        Assertions.assertTrue(Json.map(ev.get("serviceData")).isEmpty());
        Assertions.assertTrue(Json.list(ev, "absent").isEmpty());
    }

    @Test
    public void base64RoundTrips() {
        byte[] data = new byte[] {0, 1, 2, (byte) 0xFF, 42};
        Assertions.assertArrayEquals(data,
                Json.decodeBase64(Json.encodeBase64(data)));
        Assertions.assertArrayEquals(new byte[0], Json.decodeBase64(""));
        Assertions.assertArrayEquals(new byte[0], Json.decodeBase64(null));
        Assertions.assertEquals("", Json.encodeBase64(null));
        Assertions.assertEquals("", Json.encodeBase64(new byte[0]));
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
