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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An NDEF message -- the payload of an NDEF-formatted tag. A message
/// contains one or more [NdefRecord]s in order.
///
/// Construct messages directly:
///
/// ```java
/// NdefMessage msg = new NdefMessage(
///     NdefRecord.createUri("https://codenameone.com"),
///     NdefRecord.createText("en", "Codename One"));
/// nfc.writeNdef(tag, msg);
/// ```
///
/// or parse a raw byte stream via [#parse(byte[])] (the format ports use to
/// hand a discovered tag back to your code).
public final class NdefMessage {

    private final List<NdefRecord> records;

    public NdefMessage(NdefRecord... records) {
        if (records == null || records.length == 0) {
            throw new IllegalArgumentException("at least one record required");
        }
        List<NdefRecord> rs = new ArrayList<NdefRecord>(records.length);
        for (int i = 0; i < records.length; i++) {
            if (records[i] == null) {
                throw new IllegalArgumentException("record " + i + " is null");
            }
            rs.add(records[i]);
        }
        this.records = Collections.unmodifiableList(rs);
    }

    public NdefMessage(List<NdefRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("at least one record required");
        }
        List<NdefRecord> rs = new ArrayList<NdefRecord>(records.size());
        for (int i = 0; i < records.size(); i++) {
            NdefRecord r = records.get(i);
            if (r == null) {
                throw new IllegalArgumentException("record " + i + " is null");
            }
            rs.add(r);
        }
        this.records = Collections.unmodifiableList(rs);
    }

    /// The records carried by this message in tag order. Immutable.
    public List<NdefRecord> getRecords() {
        return records;
    }

    /// Convenience -- returns the first record, since most NDEF messages
    /// carry a single payload.
    public NdefRecord getFirstRecord() {
        return records.get(0);
    }

    /// Serialises this message to a flat byte array using the NDEF wire
    /// format. Ports call this to hand a message to the OS for writing.
    public byte[] toByteArray() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int n = records.size();
        for (int i = 0; i < n; i++) {
            NdefRecord r = records.get(i);
            byte[] type = r.getType();
            byte[] id = r.getId();
            byte[] payload = r.getPayload();
            boolean shortRecord = payload.length < 256;
            boolean hasId = id.length > 0;
            int header = r.getTnf() & 0x07;
            if (i == 0) {
                header |= 0x80; // MB
            }
            if (i == n - 1) {
                header |= 0x40; // ME
            }
            if (shortRecord) {
                header |= 0x10; // SR
            }
            if (hasId) {
                header |= 0x08; // IL
            }
            bos.write(header);
            bos.write(type.length & 0xFF);
            if (shortRecord) {
                bos.write(payload.length & 0xFF);
            } else {
                bos.write((payload.length >>> 24) & 0xFF);
                bos.write((payload.length >>> 16) & 0xFF);
                bos.write((payload.length >>> 8) & 0xFF);
                bos.write(payload.length & 0xFF);
            }
            if (hasId) {
                bos.write(id.length & 0xFF);
            }
            bos.write(type, 0, type.length);
            if (hasId) {
                bos.write(id, 0, id.length);
            }
            bos.write(payload, 0, payload.length);
        }
        return bos.toByteArray();
    }

    /// Parses an NDEF byte stream into a message. Tolerates the
    /// short-record (SR) flag and the optional id-length (IL) flag.
    /// Concatenated messages (multiple MB/ME-bracketed groups in the same
    /// stream) are not supported -- pass a single message.
    ///
    /// #### Throws
    ///
    /// - [NfcException] with [NfcError#INVALID_NDEF] when the input is
    ///   malformed
    public static NdefMessage parse(byte[] raw) throws NfcException {
        if (raw == null) {
            throw new NfcException(NfcError.INVALID_NDEF, "null NDEF payload");
        }
        List<NdefRecord> out = new ArrayList<NdefRecord>();
        int p = 0;
        boolean sawMb = false;
        boolean sawMe = false;
        while (p < raw.length) {
            int header = raw[p++] & 0xFF;
            boolean mb = (header & 0x80) != 0;
            boolean me = (header & 0x40) != 0;
            boolean sr = (header & 0x10) != 0;
            boolean il = (header & 0x08) != 0;
            byte tnf = (byte) (header & 0x07);
            if (out.isEmpty()) {
                if (!mb) {
                    throw new NfcException(NfcError.INVALID_NDEF,
                            "missing MB on first record");
                }
                sawMb = true;
            }
            if (p >= raw.length) {
                throw new NfcException(NfcError.INVALID_NDEF, "truncated");
            }
            int typeLen = raw[p++] & 0xFF;
            int payloadLen;
            if (sr) {
                if (p >= raw.length) {
                    throw new NfcException(NfcError.INVALID_NDEF,
                            "truncated SR length");
                }
                payloadLen = raw[p++] & 0xFF;
            } else {
                if (p + 4 > raw.length) {
                    throw new NfcException(NfcError.INVALID_NDEF,
                            "truncated payload length");
                }
                payloadLen = ((raw[p] & 0xFF) << 24)
                        | ((raw[p + 1] & 0xFF) << 16)
                        | ((raw[p + 2] & 0xFF) << 8)
                        | (raw[p + 3] & 0xFF);
                p += 4;
            }
            int idLen = 0;
            if (il) {
                if (p >= raw.length) {
                    throw new NfcException(NfcError.INVALID_NDEF,
                            "truncated id length");
                }
                idLen = raw[p++] & 0xFF;
            }
            if (p + typeLen + idLen + payloadLen > raw.length || payloadLen < 0) {
                throw new NfcException(NfcError.INVALID_NDEF, "truncated fields");
            }
            byte[] type = new byte[typeLen];
            System.arraycopy(raw, p, type, 0, typeLen);
            p += typeLen;
            byte[] id = new byte[idLen];
            System.arraycopy(raw, p, id, 0, idLen);
            p += idLen;
            byte[] payload = new byte[payloadLen];
            System.arraycopy(raw, p, payload, 0, payloadLen);
            p += payloadLen;
            out.add(new NdefRecord(tnf, type, id, payload));
            if (me) {
                sawMe = true;
                break;
            }
        }
        if (!sawMb || !sawMe || out.isEmpty()) {
            throw new NfcException(NfcError.INVALID_NDEF,
                    "incomplete NDEF message");
        }
        return new NdefMessage(out);
    }
}
