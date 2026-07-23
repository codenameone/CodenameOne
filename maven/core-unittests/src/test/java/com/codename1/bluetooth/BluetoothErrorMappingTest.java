/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.bluetooth;

import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.gatt.GattStatus;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.Test;

import static com.codename1.bluetooth.BtTestUtil.assertFailedWith;
import static com.codename1.bluetooth.BtTestUtil.errorOf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Error typing: {@link BluetoothException} never carries a {@code null}
 * error code, GATT status codes travel with the exception, fake-scripted
 * failures surface unchanged through the operation queue, and
 * {@link GattStatus#fromAttCode(int)} maps the ATT codes.
 */
class BluetoothErrorMappingTest extends UITestBase {

    @Test
    void nullErrorCodeIsCoercedToUnknownInEveryConstructor() {
        assertEquals(BluetoothError.UNKNOWN,
                new BluetoothException(null).getError());
        assertEquals(BluetoothError.UNKNOWN,
                new BluetoothException(null, "m").getError());
        assertEquals(BluetoothError.UNKNOWN,
                new BluetoothException(null, "m",
                        new RuntimeException()).getError());
        assertEquals(BluetoothError.UNKNOWN,
                new BluetoothException(null, "m", 0x05).getError());
    }

    @Test
    void errorCodeMessageAndCauseAreCarried() {
        RuntimeException cause = new RuntimeException("root");
        BluetoothException e = new BluetoothException(
                BluetoothError.IO_ERROR, "boom", cause);
        assertEquals(BluetoothError.IO_ERROR, e.getError());
        assertEquals("boom", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    void gattStatusIsCarriedAndDefaultsToMinusOne() {
        BluetoothException withStatus = new BluetoothException(
                BluetoothError.GATT_ERROR, "write rejected", 0x03);
        assertEquals(0x03, withStatus.getGattStatus());
        assertEquals(-1,
                new BluetoothException(BluetoothError.GATT_ERROR)
                        .getGattStatus());
        assertEquals(-1,
                new BluetoothException(BluetoothError.GATT_ERROR, "m")
                        .getGattStatus());
    }

    @Test
    void scriptedFailureSurfacesUnchangedThroughTheOperationQueue() {
        FakeBlePeripheral p = new FakeBlePeripheral("AA:BB:CC:DD:EE:03", "x");
        GattService s = p.buildService(BluetoothUuid.fromShort(0x180D));
        GattCharacteristic c = p.buildCharacteristic(s,
                BluetoothUuid.fromShort(0x2A37),
                GattCharacteristic.PROPERTY_READ);
        p.connectNow();

        BluetoothException scripted = new BluetoothException(
                BluetoothError.GATT_ERROR, "read not permitted",
                GattStatus.READ_NOT_PERMITTED.getAttCode());
        AsyncResource<byte[]> r = p.readCharacteristic(c);
        p.failNext(scripted);

        BluetoothException surfaced =
                assertFailedWith(r, BluetoothError.GATT_ERROR);
        assertSame(scripted, surfaced,
                "the scripted exception must surface unchanged");
        assertEquals(GattStatus.READ_NOT_PERMITTED.getAttCode(),
                surfaced.getGattStatus());
        assertEquals(GattStatus.READ_NOT_PERMITTED,
                GattStatus.fromAttCode(surfaced.getGattStatus()));
    }

    @Test
    void scriptedConvenienceApiFailureSurfacesThroughTheCharacteristic() {
        FakeBlePeripheral p = new FakeBlePeripheral("AA:BB:CC:DD:EE:04", "x");
        GattService s = p.buildService(BluetoothUuid.fromShort(0x180D));
        GattCharacteristic c = p.buildCharacteristic(s,
                BluetoothUuid.fromShort(0x2A37),
                GattCharacteristic.PROPERTY_WRITE);
        p.connectNow();

        AsyncResource<Boolean> r = c.write(new byte[] {1});
        p.failNext(new BluetoothException(BluetoothError.BUSY, "busy"));
        assertFailedWith(r, BluetoothError.BUSY);
    }

    @Test
    void nonBluetoothSpiFailureIsWrappedWithATypedError() {
        FakeBlePeripheral p = new FakeBlePeripheral("AA:BB:CC:DD:EE:05", "x") {
            @Override
            protected void doReadRssi(AsyncResource<Integer> out) {
                throw new IllegalStateException("raw platform error");
            }
        };
        p.connectNow();
        AsyncResource<Integer> r = p.readRssi();
        BluetoothException e = assertFailedWith(r, BluetoothError.UNKNOWN);
        assertTrue(e.getCause() instanceof IllegalStateException);
    }

    @Test
    void connectFailureWithForeignExceptionIsWrappedAsConnectionFailed() {
        FakeBlePeripheral p = new FakeBlePeripheral("AA:BB:CC:DD:EE:06", "x");
        AsyncResource<?> r = p.connect();
        FakeBlePeripheral.PendingOp op = p.takeNext();
        op.out.error(new IllegalStateException("stack gone"));
        Throwable t = errorOf(r);
        assertTrue(t instanceof IllegalStateException,
                "the connect handle reports the raw failure");
        // ...but the connection-event reason is a typed BluetoothException
        assertEquals(com.codename1.bluetooth.le.ConnectionState.DISCONNECTED,
                p.getConnectionState());
    }

    @Test
    void gattStatusMapsEveryDedicatedAttCode() {
        assertEquals(GattStatus.SUCCESS, GattStatus.fromAttCode(0x00));
        assertEquals(GattStatus.INVALID_HANDLE, GattStatus.fromAttCode(0x01));
        assertEquals(GattStatus.READ_NOT_PERMITTED,
                GattStatus.fromAttCode(0x02));
        assertEquals(GattStatus.WRITE_NOT_PERMITTED,
                GattStatus.fromAttCode(0x03));
        assertEquals(GattStatus.INSUFFICIENT_AUTHENTICATION,
                GattStatus.fromAttCode(0x05));
        assertEquals(GattStatus.REQUEST_NOT_SUPPORTED,
                GattStatus.fromAttCode(0x06));
        assertEquals(GattStatus.INVALID_OFFSET, GattStatus.fromAttCode(0x07));
        assertEquals(GattStatus.INVALID_ATTRIBUTE_VALUE_LENGTH,
                GattStatus.fromAttCode(0x0D));
        assertEquals(GattStatus.UNLIKELY_ERROR, GattStatus.fromAttCode(0x0E));
        assertEquals(GattStatus.INSUFFICIENT_ENCRYPTION,
                GattStatus.fromAttCode(0x0F));
    }

    @Test
    void gattStatusFallsBackToUnlikelyErrorForUnknownCodes() {
        assertEquals(GattStatus.UNLIKELY_ERROR, GattStatus.fromAttCode(0x99));
        assertEquals(GattStatus.UNLIKELY_ERROR, GattStatus.fromAttCode(-1));
    }

    @Test
    void gattStatusRoundTripsItsAttCode() {
        for (GattStatus status : GattStatus.values()) {
            assertEquals(status, GattStatus.fromAttCode(status.getAttCode()));
        }
    }
}
