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
package com.codename1.nfc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NdefMessageTest {

    @Test
    void varargsConstructorPreservesRecordOrder() {
        NdefRecord a = NdefRecord.createUri("https://codenameone.com");
        NdefRecord b = NdefRecord.createText("en", "hi");
        NdefMessage msg = new NdefMessage(a, b);

        assertEquals(2, msg.getRecords().size());
        assertSame(a, msg.getFirstRecord());
        assertSame(a, msg.getRecords().get(0));
        assertSame(b, msg.getRecords().get(1));
    }

    @Test
    void listConstructorPreservesRecords() {
        List<NdefRecord> records = new ArrayList<>();
        records.add(NdefRecord.createText("en", "one"));
        records.add(NdefRecord.createText("en", "two"));
        NdefMessage msg = new NdefMessage(records);
        assertEquals(2, msg.getRecords().size());
    }

    @Test
    void getRecordsIsImmutable() {
        NdefMessage msg = new NdefMessage(NdefRecord.createText("en", "x"));
        assertThrows(UnsupportedOperationException.class,
                () -> msg.getRecords().add(NdefRecord.createText("en", "y")));
    }

    @Test
    void varargsConstructorRejectsEmptyOrNull() {
        assertThrows(IllegalArgumentException.class, () -> new NdefMessage(new NdefRecord[0]));
        assertThrows(IllegalArgumentException.class, () -> new NdefMessage((NdefRecord[]) null));
    }

    @Test
    void varargsConstructorRejectsNullElement() {
        NdefRecord good = NdefRecord.createText("en", "x");
        assertThrows(IllegalArgumentException.class, () -> new NdefMessage(good, null));
    }

    @Test
    void listConstructorRejectsEmptyOrNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new NdefMessage(new ArrayList<NdefRecord>()));
        assertThrows(IllegalArgumentException.class,
                () -> new NdefMessage((List<NdefRecord>) null));
    }

    @Test
    void listConstructorRejectsNullElement() {
        List<NdefRecord> records = new ArrayList<>();
        records.add(NdefRecord.createText("en", "x"));
        records.add(null);
        assertThrows(IllegalArgumentException.class, () -> new NdefMessage(records));
    }

    @Test
    void singleRecordRoundTripsThroughBytes() throws NfcException {
        NdefMessage original = new NdefMessage(NdefRecord.createUri("https://codenameone.com"));
        NdefMessage parsed = NdefMessage.parse(original.toByteArray());

        assertEquals(1, parsed.getRecords().size());
        assertEquals("https://codenameone.com", parsed.getFirstRecord().getUriPayload());
    }

    @Test
    void multipleRecordsRoundTripPreservingOrderAndContent() throws NfcException {
        NdefMessage original = new NdefMessage(
                NdefRecord.createText("en", "first"),
                NdefRecord.createUri("https://codenameone.com"),
                NdefRecord.createMime("application/octet-stream", new byte[]{1, 2, 3}));
        NdefMessage parsed = NdefMessage.parse(original.toByteArray());

        assertEquals(3, parsed.getRecords().size());
        assertEquals("first", parsed.getRecords().get(0).getTextPayload());
        assertEquals("https://codenameone.com", parsed.getRecords().get(1).getUriPayload());
        assertArrayEquals(new byte[]{1, 2, 3}, parsed.getRecords().get(2).getPayload());
        assertEquals(NdefRecord.TNF_MIME_MEDIA, parsed.getRecords().get(2).getTnf());
    }

    @Test
    void recordWithIdRoundTrips() throws NfcException {
        NdefRecord withId = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                "text/plain".getBytes(), new byte[]{'i', 'd'}, new byte[]{7, 8});
        NdefMessage parsed = NdefMessage.parse(new NdefMessage(withId).toByteArray());

        NdefRecord r = parsed.getFirstRecord();
        assertArrayEquals(new byte[]{'i', 'd'}, r.getId());
        assertArrayEquals(new byte[]{7, 8}, r.getPayload());
    }

    @Test
    void longPayloadUsesNonShortRecordEncodingAndRoundTrips() throws NfcException {
        // Payloads >= 256 bytes force the 4-byte (non-SR) length encoding.
        byte[] big = new byte[300];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) (i & 0xFF);
        }
        NdefRecord r = NdefRecord.createMime("application/octet-stream", big);
        NdefMessage parsed = NdefMessage.parse(new NdefMessage(r).toByteArray());

        assertArrayEquals(big, parsed.getFirstRecord().getPayload());
    }

    @Test
    void parseRejectsNullInput() {
        NfcException ex = assertThrows(NfcException.class, () -> NdefMessage.parse(null));
        assertEquals(NfcError.INVALID_NDEF, ex.getError());
    }

    @Test
    void parseRejectsMissingMessageBeginFlag() {
        // First header byte lacks the MB (0x80) bit.
        byte[] raw = {0x11, 0x01, 0x00, 'T'};
        NfcException ex = assertThrows(NfcException.class, () -> NdefMessage.parse(raw));
        assertEquals(NfcError.INVALID_NDEF, ex.getError());
    }

    @Test
    void parseRejectsTruncatedStream() {
        // MB|ME|SR header but the stream ends before the type-length byte.
        byte[] raw = {(byte) 0xD1};
        NfcException ex = assertThrows(NfcException.class, () -> NdefMessage.parse(raw));
        assertEquals(NfcError.INVALID_NDEF, ex.getError());
    }

    @Test
    void parseRejectsFieldsExceedingBuffer() {
        // SR header, type len 1, payload len 10, but no bytes follow.
        byte[] raw = {(byte) 0xD1, 0x01, 0x0A};
        NfcException ex = assertThrows(NfcException.class, () -> NdefMessage.parse(raw));
        assertEquals(NfcError.INVALID_NDEF, ex.getError());
    }

    @Test
    void parseRejectsStreamWithoutMessageEndFlag() {
        // Valid MB record but ME flag never set and no further records.
        // Header: MB|SR (0x91), type len 1, payload len 1, type 'T', payload 'x'.
        byte[] raw = {(byte) 0x91, 0x01, 0x01, 'T', 'x'};
        NfcException ex = assertThrows(NfcException.class, () -> NdefMessage.parse(raw));
        assertEquals(NfcError.INVALID_NDEF, ex.getError());
    }

    @Test
    void toByteArraySetsMessageBeginAndEndFlagsCorrectly() {
        byte[] raw = new NdefMessage(
                NdefRecord.createText("en", "a"),
                NdefRecord.createText("en", "b")).toByteArray();

        // First record header must carry MB (0x80) and not ME (0x40).
        assertTrue((raw[0] & 0x80) != 0, "first record should set MB");
        assertEquals(0, raw[0] & 0x40, "first record of a multi-record message must not set ME");
    }
}
