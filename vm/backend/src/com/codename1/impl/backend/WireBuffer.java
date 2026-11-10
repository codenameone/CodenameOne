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
package com.codename1.impl.backend;

import com.codename1.backend.Tcp;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/// Buffered framing over a [Tcp] connection, for the binary and line protocols
/// the messaging and key/value clients speak: Redis's RESP, AMQP 0-9-1 and
/// Kafka's.
///
/// Reads come out of one fixed buffer filled a socket read at a time, so
/// decoding a frame is a memory operation rather than a system call per field.
/// Writes are staged in one growable array and sent with a single write by
/// [#flush], which is what makes a pipelined batch of commands one packet. The
/// staging array is reused: steady traffic allocates nothing here.
///
/// Not thread safe; a connection belongs to one caller at a time.
public final class WireBuffer {
    private final Tcp connection;
    private final byte[] in;
    private int position;
    private int limit;
    private byte[] out = new byte[1024];
    private int outLength;

    /// A buffer over `connection` with a 16 KiB read buffer.
    public WireBuffer(Tcp connection) {
        this(connection, 16384);
    }

    /// A buffer over `connection` reading `readBuffer` bytes at a time.
    public WireBuffer(Tcp connection, int readBuffer) {
        this.connection = connection;
        this.in = new byte[readBuffer];
    }

    /// The connection underneath.
    public Tcp getConnection() {
        return connection;
    }

    // ------------------------------------------------------------- reading

    private void fill() throws IOException {
        position = 0;
        limit = 0;
        int n = connection.read(in, 0, in.length);
        if (n <= 0) {
            throw new IOException("The connection was closed by the server");
        }
        limit = n;
    }

    /// Whether a read would be answered without touching the socket.
    public boolean hasBuffered() {
        return position < limit;
    }

    /// One byte; an IOException at end of stream.
    public int read() throws IOException {
        if (position >= limit) {
            fill();
        }
        return in[position++] & 0xff;
    }

    /// Exactly `length` bytes into `target`, or an IOException.
    public void readFully(byte[] target, int offset, int length) throws IOException {
        while (length > 0) {
            if (position >= limit) {
                if (length >= in.length) {
                    // A large body goes straight into its destination.
                    int n = connection.read(target, offset, length);
                    if (n <= 0) {
                        throw new IOException("The connection was closed by the server");
                    }
                    offset += n;
                    length -= n;
                    continue;
                }
                fill();
            }
            int chunk = Math.min(length, limit - position);
            System.arraycopy(in, position, target, offset, chunk);
            position += chunk;
            offset += chunk;
            length -= chunk;
        }
    }

    /// Exactly `length` bytes, newly allocated.
    public byte[] readFully(int length) throws IOException {
        if (length < 0) {
            throw new IOException("Negative frame length " + length);
        }
        byte[] data = new byte[length];
        readFully(data, 0, length);
        return data;
    }

    /// Discards `length` bytes.
    public void skip(long length) throws IOException {
        while (length > 0) {
            if (position >= limit) {
                fill();
            }
            int chunk = (int) Math.min(length, limit - position);
            position += chunk;
            length -= chunk;
        }
    }

    /// A big-endian 16 bit unsigned value.
    public int readShortBE() throws IOException {
        return (read() << 8) | read();
    }

    /// A big-endian 32 bit value.
    public int readIntBE() throws IOException {
        return (read() << 24) | (read() << 16) | (read() << 8) | read();
    }

    /// A big-endian 64 bit value.
    public long readLongBE() throws IOException {
        return ((long) readIntBE() << 32) | (readIntBE() & 0xffffffffL);
    }

    /// The bytes up to a CR LF, without it, decoded as ASCII -- a RESP header.
    /// Refuses a line longer than the read buffer: no header is.
    public String readLine() throws IOException {
        StringBuilder sb = null;
        int start = position;
        while (true) {
            if (position >= limit) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                appendAscii(sb, start, position);
                if (sb.length() > in.length) {
                    throw new IOException("Protocol line longer than " + in.length + " bytes");
                }
                fill();
                start = 0;
            }
            byte b = in[position++];
            if (b == '\n') {
                int end = position - 1;
                String line;
                if (sb == null) {
                    line = ascii(start, end);
                } else {
                    appendAscii(sb, start, end);
                    line = sb.toString();
                }
                int len = line.length();
                return len > 0 && line.charAt(len - 1) == '\r' ? line.substring(0, len - 1) : line;
            }
        }
    }

    private String ascii(int from, int to) {
        char[] chars = new char[to - from];
        for (int i = from; i < to; i++) {
            chars[i - from] = (char) (in[i] & 0x7f);
        }
        return new String(chars);
    }

    private void appendAscii(StringBuilder sb, int from, int to) {
        for (int i = from; i < to; i++) {
            sb.append((char) (in[i] & 0x7f));
        }
    }

    // ------------------------------------------------------------- writing

    private void ensure(int more) {
        int need = outLength + more;
        if (need > out.length) {
            byte[] bigger = new byte[Math.max(need, out.length * 2)];
            System.arraycopy(out, 0, bigger, 0, outLength);
            out = bigger;
        }
    }

    /// Stages one byte.
    public void writeByte(int value) {
        ensure(1);
        out[outLength++] = (byte) value;
    }

    /// Stages `data`.
    public void writeBytes(byte[] data) {
        writeBytes(data, 0, data.length);
    }

    /// Stages part of `data`.
    public void writeBytes(byte[] data, int offset, int length) {
        ensure(length);
        System.arraycopy(data, offset, out, outLength, length);
        outLength += length;
    }

    /// Stages a big-endian 16 bit value.
    public void writeShortBE(int value) {
        ensure(2);
        out[outLength++] = (byte) (value >>> 8);
        out[outLength++] = (byte) value;
    }

    /// Stages a big-endian 32 bit value.
    public void writeIntBE(int value) {
        ensure(4);
        out[outLength++] = (byte) (value >>> 24);
        out[outLength++] = (byte) (value >>> 16);
        out[outLength++] = (byte) (value >>> 8);
        out[outLength++] = (byte) value;
    }

    /// Stages a big-endian 64 bit value.
    public void writeLongBE(long value) {
        writeIntBE((int) (value >>> 32));
        writeIntBE((int) value);
    }

    /// Stages the decimal digits of `value`, as RESP writes lengths.
    public void writeDecimal(long value) {
        if (value == 0) {
            writeByte('0');
            return;
        }
        if (value < 0) {
            writeByte('-');
            if (value == Long.MIN_VALUE) {
                writeBytes(ascii("9223372036854775808"));
                return;
            }
            value = -value;
        }
        int digits = 0;
        for (long v = value; v > 0; v /= 10) {
            digits++;
        }
        ensure(digits);
        for (int i = outLength + digits - 1; i >= outLength; i--) {
            out[i] = (byte) ('0' + (value % 10));
            value /= 10;
        }
        outLength += digits;
    }

    /// How many bytes are staged.
    public int pending() {
        return outLength;
    }

    /// Where the next staged byte goes, for a length that is patched in later.
    public int mark() {
        return outLength;
    }

    /// Overwrites four staged bytes at `at` with a big-endian value: a frame
    /// length known only once the frame is written.
    public void patchIntBE(int at, int value) {
        out[at] = (byte) (value >>> 24);
        out[at + 1] = (byte) (value >>> 16);
        out[at + 2] = (byte) (value >>> 8);
        out[at + 3] = (byte) value;
    }

    /// Copies out the staged bytes from `from` on, leaving them staged.
    public byte[] copyStaged(int from) {
        byte[] copy = new byte[outLength - from];
        System.arraycopy(out, from, copy, 0, copy.length);
        return copy;
    }

    /// Drops everything staged from `at` on.
    public void truncate(int at) {
        outLength = at;
    }

    /// Sends everything staged in one write and clears the stage.
    public void flush() throws IOException {
        if (outLength > 0) {
            int n = outLength;
            outLength = 0;
            connection.write(out, 0, n);
        }
        if (out.length > 262144) {
            // One huge value should not pin its buffer for the connection's life.
            out = new byte[1024];
        }
    }

    // ------------------------------------------------------------- text

    /// `value` as UTF-8.
    public static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing", err);
        }
    }

    /// UTF-8 `data` as a String; null for null.
    public static String fromUtf8(byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return new String(data, 0, data.length, "UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing", err);
        }
    }

    /// An ASCII string's bytes, without consulting a charset.
    public static byte[] ascii(String value) {
        byte[] b = new byte[value.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) value.charAt(i);
        }
        return b;
    }
}
