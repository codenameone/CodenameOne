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

import com.codename1.bluetooth.le.AdvertisementData;
import org.junit.jupiter.api.Test;

import static com.codename1.bluetooth.BtTestUtil.bytes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AdvertisementData#parse(byte[])} over raw AD structures
 * (length, type, payload sequences): names, the three service-UUID widths
 * (little-endian!), manufacturer and service data, TX power, skipped flags
 * and the malformed-input guarantees. Pure parsing -- no Display required.
 */
class AdvertisementDataParseTest {

    @Test
    void parsesCompleteLocalName() {
        AdvertisementData ad = AdvertisementData.parse(
                bytes(0x04, 0x09, 'H', 'R', 'M'));
        assertEquals("HRM", ad.getLocalName());
    }

    @Test
    void parsesShortenedLocalName() {
        AdvertisementData ad = AdvertisementData.parse(
                bytes(0x03, 0x08, 'H', 'R'));
        assertEquals("HR", ad.getLocalName());
    }

    @Test
    void parsesSixteenBitServiceUuidsLittleEndian() {
        // two 16-bit UUIDs in one complete-list structure: 0x180D, 0x180F
        AdvertisementData ad = AdvertisementData.parse(
                bytes(0x05, 0x03, 0x0D, 0x18, 0x0F, 0x18));
        assertEquals(2, ad.getServiceUuids().size());
        assertTrue(ad.getServiceUuids().contains(
                BluetoothUuid.fromShort(0x180D)));
        assertTrue(ad.getServiceUuids().contains(
                BluetoothUuid.fromShort(0x180F)));
    }

    @Test
    void parsesIncompleteSixteenBitServiceUuidList() {
        AdvertisementData ad = AdvertisementData.parse(
                bytes(0x03, 0x02, 0x0D, 0x18));
        assertTrue(ad.getServiceUuids().contains(
                BluetoothUuid.fromShort(0x180D)));
    }

    @Test
    void parsesThirtyTwoBitServiceUuidsLittleEndian() {
        AdvertisementData ad = AdvertisementData.parse(
                bytes(0x05, 0x05, 0x78, 0x56, 0x34, 0x12));
        assertEquals(1, ad.getServiceUuids().size());
        assertTrue(ad.getServiceUuids().contains(
                BluetoothUuid.fromShort(0x12345678)));
    }

    @Test
    void parsesOneHundredTwentyEightBitServiceUuidLittleEndian() {
        // 0000180d-0000-1000-8000-00805f9b34fb transmitted little-endian
        AdvertisementData ad = AdvertisementData.parse(bytes(
                0x11, 0x07,
                0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80,
                0x00, 0x10, 0x00, 0x00, 0x0D, 0x18, 0x00, 0x00));
        assertEquals(1, ad.getServiceUuids().size());
        assertEquals(BluetoothUuid.fromShort(0x180D),
                ad.getServiceUuids().get(0));
    }

    @Test
    void parsesManufacturerDataWithLittleEndianCompanyId() {
        AdvertisementData ad = AdvertisementData.parse(
                bytes(0x05, 0xFF, 0x4C, 0x00, 0x01, 0x02));
        assertArrayEquals(bytes(0x01, 0x02), ad.getManufacturerData(0x004C));
        assertNull(ad.getManufacturerData(0x4C00));
        assertArrayEquals(new int[] {0x004C}, ad.getManufacturerIds());
    }

    @Test
    void parsesSixteenBitServiceData() {
        AdvertisementData ad = AdvertisementData.parse(
                bytes(0x05, 0x16, 0x0D, 0x18, 0xAA, 0xBB));
        assertArrayEquals(bytes(0xAA, 0xBB),
                ad.getServiceData(BluetoothUuid.fromShort(0x180D)));
        assertNull(ad.getServiceData(BluetoothUuid.fromShort(0x180F)));
    }

    @Test
    void parsesOneHundredTwentyEightBitServiceData() {
        AdvertisementData ad = AdvertisementData.parse(bytes(
                0x13, 0x21,
                0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80,
                0x00, 0x10, 0x00, 0x00, 0x0D, 0x18, 0x00, 0x00,
                0xCC, 0xDD));
        assertArrayEquals(bytes(0xCC, 0xDD),
                ad.getServiceData(BluetoothUuid.fromShort(0x180D)));
    }

    @Test
    void parsesTxPowerAsSignedByte() {
        AdvertisementData ad = AdvertisementData.parse(
                bytes(0x02, 0x0A, 0xF8));
        assertEquals(Integer.valueOf(-8), ad.getTxPowerLevel());
    }

    @Test
    void flagsStructureIsSkippedWithoutBreakingLaterStructures() {
        AdvertisementData ad = AdvertisementData.parse(bytes(
                0x02, 0x01, 0x06,
                0x04, 0x09, 'H', 'R', 'M'));
        assertEquals("HRM", ad.getLocalName());
    }

    @Test
    void unknownTypeIsSkippedWithoutBreakingLaterStructures() {
        AdvertisementData ad = AdvertisementData.parse(bytes(
                0x03, 0x77, 0x01, 0x02,
                0x02, 0x0A, 0x04));
        assertEquals(Integer.valueOf(4), ad.getTxPowerLevel());
    }

    @Test
    void truncatedTrailingStructureEndsParsingWithoutThrowing() {
        // valid TX power, then a structure claiming 5 payload bytes with
        // only 1 present
        AdvertisementData ad = AdvertisementData.parse(bytes(
                0x02, 0x0A, 0xF4,
                0x05, 0x09, 'A'));
        assertEquals(Integer.valueOf(-12), ad.getTxPowerLevel());
        assertNull(ad.getLocalName());
    }

    @Test
    void zeroLengthStructureEndsParsing() {
        AdvertisementData ad = AdvertisementData.parse(bytes(
                0x00, 0x09, 'A'));
        assertNull(ad.getLocalName());
        assertTrue(ad.getServiceUuids().isEmpty());
    }

    @Test
    void emptyInputYieldsEmptyData() {
        AdvertisementData ad = AdvertisementData.parse(new byte[0]);
        assertNull(ad.getLocalName());
        assertTrue(ad.getServiceUuids().isEmpty());
        assertEquals(0, ad.getManufacturerIds().length);
        assertNull(ad.getTxPowerLevel());
        assertEquals(0, ad.getRawBytes().length);
    }

    @Test
    void nullInputYieldsEmptyDataWithoutThrowing() {
        AdvertisementData ad = AdvertisementData.parse(null);
        assertNotNull(ad);
        assertNull(ad.getLocalName());
        assertTrue(ad.getServiceUuids().isEmpty());
        assertNull(ad.getRawBytes());
    }

    @Test
    void rawBytesAreRetained() {
        byte[] raw = bytes(0x02, 0x0A, 0x00);
        assertSame(raw, AdvertisementData.parse(raw).getRawBytes());
    }

    @Test
    void duplicateServiceUuidsAreCollapsed() {
        AdvertisementData ad = AdvertisementData.parse(bytes(
                0x05, 0x03, 0x0D, 0x18, 0x0D, 0x18));
        assertEquals(1, ad.getServiceUuids().size());
    }
}
