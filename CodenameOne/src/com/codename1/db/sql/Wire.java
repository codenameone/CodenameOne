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
package com.codename1.db.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


/// Buffered framing over a [SqlSocket] connection, shared by the PostgreSQL and
/// MySQL clients.
///
/// Both protocols are length-prefixed binary, and both are read a packet at a
/// time, so an unbuffered read per field would be one system call per integer. The
/// read buffer here is what makes a row decode a memory operation.
///
/// The two protocols disagree about byte order -- PostgreSQL is big endian and
/// MySQL little endian -- so both are provided rather than picking one and having
/// a client remember to swap.
@com.codename1.impl.SharedWithBackend
final class Wire {
    private final SqlSocket connection;
    private final byte[] buffer = new byte[16384];
    private int position;
    private int limit;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

    Wire(SqlSocket connection) {
        this.connection = connection;
    }

    SqlSocket getConnection() {
        return connection;
    }

    // ---------------- reading ----------------

    /// One byte, or -1 at end of stream.
    int read() throws IOException {
        if (position >= limit && !fill()) {
            return -1;
        }
        return buffer[position++] & 0xff;
    }

    /// Exactly `length` bytes, or an IOException: a short packet is a protocol error.
    byte[] readFully(int length) throws IOException {
        byte[] target = new byte[length];
        readFully(target, 0, length);
        return target;
    }

    void readFully(byte[] target, int offset, int length) throws IOException {
        int at = 0;
        while (at < length) {
            if (position >= limit && !fill()) {
                throw new IOException("The connection closed after " + at + " of "
                        + length + " bytes");
            }
            int available = limit - position;
            int take = available < length - at ? available : length - at;
            System.arraycopy(buffer, position, target, offset + at, take);
            position += take;
            at += take;
        }
    }

    /// Discards `length` bytes without allocating for them.
    void skip(int length) throws IOException {
        int remaining = length;
        while (remaining > 0) {
            if (position >= limit && !fill()) {
                throw new IOException("The connection closed while skipping");
            }
            int available = limit - position;
            int take = available < remaining ? available : remaining;
            position += take;
            remaining -= take;
        }
    }

    int readIntBE() throws IOException {
        byte[] b = readFully(4);
        return ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
    }

    int readShortBE() throws IOException {
        byte[] b = readFully(2);
        return ((b[0] & 0xff) << 8) | (b[1] & 0xff);
    }

    private boolean fill() throws IOException {
        position = 0;
        limit = 0;
        int n = connection.read(buffer, 0, buffer.length);
        if (n <= 0) {
            return false;
        }
        limit = n;
        return true;
    }

    // ---------------- writing ----------------

    void writeByte(int value) {
        out.write(value & 0xff);
    }

    void writeBytes(byte[] data) {
        if (data != null) {
            out.write(data, 0, data.length);
        }
    }

    void writeBytes(byte[] data, int offset, int length) {
        out.write(data, offset, length);
    }

    void writeShortBE(int value) {
        out.write((value >> 8) & 0xff);
        out.write(value & 0xff);
    }

    void writeIntBE(int value) {
        out.write((value >> 24) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 8) & 0xff);
        out.write(value & 0xff);
    }

    void writeShortLE(int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    void writeIntLE(int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }

    void writeLongLE(long value) {
        for (int iter = 0 ; iter < 8 ; iter++) {
            out.write((int) ((value >> (iter * 8)) & 0xff));
        }
    }

    /// A NUL-terminated string, which is how both protocols carry names.
    void writeCString(String value) {
        writeBytes(utf8(value));
        out.write(0);
    }

    /// How many bytes are staged but not yet sent.
    int pending() {
        return out.size();
    }

    byte[] take() {
        byte[] data = out.toByteArray();
        out.reset();
        return data;
    }

    /// Sends everything staged and clears the buffer.
    void flush() throws IOException {
        byte[] data = take();
        if (data.length > 0) {
            connection.write(data, 0, data.length);
        }
    }

    static byte[] utf8(String value) {
        if (value == null) {
            return new byte[0];
        }
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing", err);
        }
    }

    static String fromUtf8(byte[] data, int offset, int length) {
        try {
            return new String(data, offset, length, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing", err);
        }
    }

    static String fromUtf8(byte[] data) {
        return data == null ? null : fromUtf8(data, 0, data.length);
    }

    /// UTF-16LE, two bytes a code unit, which is how TDS carries every string.
    /// Written out by hand: a surrogate pair is two code units in a Java string
    /// already, so each char is exactly one little-endian pair.
    static byte[] utf16le(String value) {
        if (value == null) {
            return new byte[0];
        }
        int length = value.length();
        byte[] out = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            out[i * 2] = (byte) c;
            out[i * 2 + 1] = (byte) (c >> 8);
        }
        return out;
    }

    /// The reverse of [#utf16le]. `length` is in bytes; an odd trailing byte is
    /// ignored. Built on a StringBuilder, never a char array, which this
    /// runtime would widen every Latin-1 string to hold.
    static String fromUtf16le(byte[] data, int offset, int length) {
        int units = length / 2;
        StringBuilder out = new StringBuilder(units);
        for (int i = 0; i < units; i++) {
            out.append((char) ((data[offset + i * 2] & 0xff) | ((data[offset + i * 2 + 1] & 0xff) << 8)));
        }
        return out.toString();
    }
    private static final String BASE64 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /// Standard base64 with padding, as SCRAM spells its nonces and proofs.
    static String base64(byte[] data) {
        StringBuilder out = new StringBuilder((data.length + 2) / 3 * 4);
        for (int i = 0; i < data.length; i += 3) {
            int n = (data[i] & 0xff) << 16;
            if (i + 1 < data.length) {
                n |= (data[i + 1] & 0xff) << 8;
            }
            if (i + 2 < data.length) {
                n |= data[i + 2] & 0xff;
            }
            out.append(BASE64.charAt(n >> 18 & 63)).append(BASE64.charAt(n >> 12 & 63));
            out.append(i + 1 < data.length ? BASE64.charAt(n >> 6 & 63) : '=');
            out.append(i + 2 < data.length ? BASE64.charAt(n & 63) : '=');
        }
        return out.toString();
    }

    /// Decodes standard padded base64, or answers null for anything that is not.
    static byte[] unbase64(String text) {
        int length = text.length();
        if (length % 4 != 0) {
            return null;
        }
        int pad = 0;
        if (length > 0 && text.charAt(length - 1) == '=') {
            pad++;
            if (text.charAt(length - 2) == '=') {
                pad++;
            }
        }
        byte[] out = new byte[length / 4 * 3 - pad];
        int at = 0;
        for (int i = 0; i < length; i += 4) {
            int n = 0;
            for (int j = 0; j < 4; j++) {
                char c = text.charAt(i + j);
                int v;
                if (c == '=' && i + 4 == length && j >= 4 - pad) {
                    v = 0;
                } else {
                    v = BASE64.indexOf(c);
                    if (v < 0) {
                        return null;
                    }
                }
                n = n << 6 | v;
            }
            for (int j = 0; j < 3 && at < out.length; j++) {
                out[at++] = (byte) (n >> 16 - 8 * j);
            }
        }
        return out;
    }
}
