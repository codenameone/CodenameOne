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
package com.codename1.backend.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.codename1.backend.Tcp;

/**
 * Buffered framing over a {@link Tcp} connection, shared by the PostgreSQL and
 * MySQL clients.
 *
 * Both protocols are length-prefixed binary, and both are read a packet at a
 * time, so an unbuffered read per field would be one system call per integer. The
 * read buffer here is what makes a row decode a memory operation.
 *
 * The two protocols disagree about byte order -- PostgreSQL is big endian and
 * MySQL little endian -- so both are provided rather than picking one and having
 * a client remember to swap.
 */
final class Wire {
    private final Tcp connection;
    private final byte[] buffer = new byte[16384];
    private int position;
    private int limit;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

    Wire(Tcp connection) {
        this.connection = connection;
    }

    Tcp getConnection() {
        return connection;
    }

    // ---------------- reading ----------------

    /** One byte, or -1 at end of stream. */
    int read() throws IOException {
        if(position >= limit && !fill()) {
            return -1;
        }
        return buffer[position++] & 0xff;
    }

    /** Exactly `length` bytes, or an IOException: a short packet is a protocol error. */
    byte[] readFully(int length) throws IOException {
        byte[] target = new byte[length];
        readFully(target, 0, length);
        return target;
    }

    void readFully(byte[] target, int offset, int length) throws IOException {
        int at = 0;
        while(at < length) {
            if(position >= limit && !fill()) {
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

    /** Discards `length` bytes without allocating for them. */
    void skip(int length) throws IOException {
        int remaining = length;
        while(remaining > 0) {
            if(position >= limit && !fill()) {
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
        if(n <= 0) {
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
        if(data != null) {
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
        for(int iter = 0 ; iter < 8 ; iter++) {
            out.write((int)((value >> (iter * 8)) & 0xff));
        }
    }

    /** A NUL-terminated string, which is how both protocols carry names. */
    void writeCString(String value) {
        writeBytes(utf8(value));
        out.write(0);
    }

    /** How many bytes are staged but not yet sent. */
    int pending() {
        return out.size();
    }

    byte[] take() {
        byte[] data = out.toByteArray();
        out.reset();
        return data;
    }

    /** Sends everything staged and clears the buffer. */
    void flush() throws IOException {
        byte[] data = take();
        if(data.length > 0) {
            connection.write(data, 0, data.length);
        }
    }

    static byte[] utf8(String value) {
        if(value == null) {
            return new byte[0];
        }
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing");
        }
    }

    static String fromUtf8(byte[] data, int offset, int length) {
        try {
            return new String(data, offset, length, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing");
        }
    }

    static String fromUtf8(byte[] data) {
        return data == null ? null : fromUtf8(data, 0, data.length);
    }
}
