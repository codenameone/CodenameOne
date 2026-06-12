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

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

class NdefRecordTest {

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rtdAccessorsReturnExpectedBytes() {
        assertArrayEquals(new byte[]{'T'}, NdefRecord.rtdText());
        assertArrayEquals(new byte[]{'U'}, NdefRecord.rtdUri());
        assertArrayEquals(new byte[]{'S', 'p'}, NdefRecord.rtdSmartPoster());
        assertArrayEquals("android.com:pkg".getBytes(), NdefRecord.rtdAndroidApp());
    }

    @Test
    void rtdAccessorsReturnDefensiveCopies() {
        byte[] first = NdefRecord.rtdText();
        first[0] = 'Z';
        // Mutating the returned array must not corrupt the shared constant.
        assertArrayEquals(new byte[]{'T'}, NdefRecord.rtdText());
    }

    @Test
    void constructorNormalizesNullFieldsToEmptyArrays() {
        NdefRecord r = new NdefRecord(NdefRecord.TNF_UNKNOWN, null, null, null);
        assertEquals(NdefRecord.TNF_UNKNOWN, r.getTnf());
        assertEquals(0, r.getType().length);
        assertEquals(0, r.getId().length);
        assertEquals(0, r.getPayload().length);
    }

    @Test
    void accessorsReturnDefensiveCopies() {
        byte[] type = {'A'};
        byte[] id = {'i'};
        byte[] payload = {1, 2, 3};
        NdefRecord r = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, type, id, payload);

        // Mutating the source arrays after construction must not leak in.
        type[0] = 'B';
        id[0] = 'j';
        payload[0] = 9;
        assertArrayEquals(new byte[]{'A'}, r.getType());
        assertArrayEquals(new byte[]{'i'}, r.getId());
        assertArrayEquals(new byte[]{1, 2, 3}, r.getPayload());

        // Mutating the returned arrays must not leak back into the record.
        r.getType()[0] = 'C';
        r.getPayload()[0] = 7;
        assertArrayEquals(new byte[]{'A'}, r.getType());
        assertArrayEquals(new byte[]{1, 2, 3}, r.getPayload());
    }

    @Test
    void createTextRoundTrips() {
        NdefRecord r = NdefRecord.createText("en", "Codename One");
        assertEquals(NdefRecord.TNF_WELL_KNOWN, r.getTnf());
        assertArrayEquals(new byte[]{'T'}, r.getType());
        assertEquals("Codename One", r.getTextPayload());
    }

    @Test
    void createTextDefaultsNullOrEmptyLanguageToEnglish() {
        NdefRecord r = NdefRecord.createText(null, "hi");
        // First payload byte holds the language-code length: "en" -> 2.
        assertEquals(2, r.getPayload()[0] & 0x3F);
        assertEquals("hi", r.getTextPayload());

        NdefRecord r2 = NdefRecord.createText("", "hi");
        assertEquals(2, r2.getPayload()[0] & 0x3F);
    }

    @Test
    void createTextTreatsNullTextAsEmpty() {
        NdefRecord r = NdefRecord.createText("en", null);
        assertEquals("", r.getTextPayload());
    }

    @Test
    void createTextEncodesUnicodeAsUtf8() {
        NdefRecord r = NdefRecord.createText("ja", "こん");
        assertEquals("こん", r.getTextPayload());
    }

    @Test
    void createTextRejectsOverlongLanguageCode() {
        StringBuilder lang = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            lang.append('a');
        }
        assertThrows(IllegalArgumentException.class,
                () -> NdefRecord.createText(lang.toString(), "x"));
    }

    @Test
    void createUriCompressesKnownPrefix() {
        NdefRecord r = NdefRecord.createUri("https://www.codenameone.com");
        assertEquals(NdefRecord.TNF_WELL_KNOWN, r.getTnf());
        assertArrayEquals(new byte[]{'U'}, r.getType());
        // Prefix code 2 == "https://www." per the RTD-URI abbreviation table.
        assertEquals(2, r.getPayload()[0] & 0xFF);
        assertEquals("https://www.codenameone.com", r.getUriPayload());
    }

    @Test
    void createUriWithoutKnownPrefixUsesZeroCode() {
        NdefRecord r = NdefRecord.createUri("custom-scheme:payload");
        assertEquals(0, r.getPayload()[0] & 0xFF);
        assertEquals("custom-scheme:payload", r.getUriPayload());
    }

    @Test
    void createUriRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> NdefRecord.createUri(null));
    }

    @Test
    void createMimeStoresTypeAndPayload() {
        byte[] payload = {10, 20, 30};
        NdefRecord r = NdefRecord.createMime("application/octet-stream", payload);
        assertEquals(NdefRecord.TNF_MIME_MEDIA, r.getTnf());
        assertArrayEquals(utf8("application/octet-stream"), r.getType());
        assertArrayEquals(payload, r.getPayload());
    }

    @Test
    void createMimeRejectsEmptyType() {
        assertThrows(IllegalArgumentException.class,
                () -> NdefRecord.createMime("", new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> NdefRecord.createMime(null, new byte[0]));
    }

    @Test
    void createExternalLowercasesDomainAndType() {
        byte[] payload = {1};
        NdefRecord r = NdefRecord.createExternal("Example.COM", "MyType", payload);
        assertEquals(NdefRecord.TNF_EXTERNAL_TYPE, r.getTnf());
        assertArrayEquals(utf8("example.com:mytype"), r.getType());
        assertArrayEquals(payload, r.getPayload());
    }

    @Test
    void createExternalRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> NdefRecord.createExternal(null, "t", new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> NdefRecord.createExternal("d", null, new byte[0]));
    }

    @Test
    void createApplicationRecordUsesAndroidAarType() {
        NdefRecord r = NdefRecord.createApplicationRecord("com.example.app");
        assertEquals(NdefRecord.TNF_EXTERNAL_TYPE, r.getTnf());
        assertArrayEquals(NdefRecord.rtdAndroidApp(), r.getType());
        assertArrayEquals(utf8("com.example.app"), r.getPayload());
    }

    @Test
    void createApplicationRecordRejectsEmptyPackage() {
        assertThrows(IllegalArgumentException.class,
                () -> NdefRecord.createApplicationRecord(""));
        assertThrows(IllegalArgumentException.class,
                () -> NdefRecord.createApplicationRecord(null));
    }

    @Test
    void getTextPayloadReturnsNullForNonTextRecords() {
        // Wrong TNF.
        assertNull(NdefRecord.createMime("text/plain", new byte[]{1}).getTextPayload());
        // Well-known but URI type, not text.
        assertNull(NdefRecord.createUri("http://x").getTextPayload());
    }

    @Test
    void getTextPayloadReturnsNullForEmptyPayload() {
        NdefRecord r = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.rtdText(), null, new byte[0]);
        assertNull(r.getTextPayload());
    }

    @Test
    void getTextPayloadReturnsNullWhenLanguageLengthOverrunsPayload() {
        // Status byte claims a 5-byte language code but payload only has 1 byte.
        NdefRecord r = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.rtdText(), null, new byte[]{0x05});
        assertNull(r.getTextPayload());
    }

    @Test
    void getUriPayloadDecodesAbsoluteUriRecord() {
        // For an absolute-URI record the URI lives in the type field.
        NdefRecord r = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI,
                utf8("https://abs.example.com"), null, new byte[]{0});
        assertEquals("https://abs.example.com", r.getUriPayload());
    }

    @Test
    void getUriPayloadDecodesAbsoluteUriWithEmptyPayload() {
        // Regression: an absolute-URI record's URI is held in the type field
        // and the payload is commonly empty. The decoder must not let the
        // payload-length guard short-circuit this case to null.
        NdefRecord r = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI,
                utf8("https://empty.example.com"), null, new byte[0]);
        assertEquals("https://empty.example.com", r.getUriPayload());
    }

    @Test
    void getUriPayloadReturnsNullForUnrelatedRecords() {
        assertNull(NdefRecord.createText("en", "x").getUriPayload());
        NdefRecord empty = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.rtdUri(), null, new byte[0]);
        assertNull(empty.getUriPayload());
    }

    @Test
    void getUriPayloadTreatsOutOfRangePrefixCodeAsNoPrefix() {
        // Prefix byte 0xFF is beyond the abbreviation table -> empty prefix.
        NdefRecord r = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.rtdUri(), null,
                new byte[]{(byte) 0xFF, 'a', 'b'});
        assertEquals("ab", r.getUriPayload());
    }
}
