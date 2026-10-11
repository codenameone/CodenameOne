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
package java.util.zip;
import java.io.*;

/** RFC 1952 decoder, including concatenated members and header/trailer CRC checks. */
public class GZIPInputStream extends InflaterInputStream {
    public static final int GZIP_MAGIC = 0x8b1f;
    protected CRC32 crc = new CRC32();
    protected boolean eos;
    public GZIPInputStream(InputStream in) throws IOException {
        this(in, 512);
    }
    public GZIPInputStream(InputStream in, int size) throws IOException {
        super(new PushbackInputStream(in, Math.max(size, 2)), new Inflater(true), size);
        try {
            header();
        } catch (IOException ex) {
            inf.end();
            throw ex;
        }
    }
    private int byteValue() throws IOException {
        int b = in.read();
        if (b < 0)
            throw new EOFException("Truncated GZIP");
        return b;
    }
    private int headerByte() throws IOException {
        int b = byteValue();
        crc.update(b);
        return b;
    }
    private void header() throws IOException {
        crc.reset();
        if (headerByte() != 31 || headerByte() != 139 || headerByte() != 8)
            throw new ZipException("Invalid GZIP header");
        int flags = headerByte();
        if ((flags & 224) != 0)
            throw new ZipException("Reserved GZIP flags");
        for (int i = 0; i < 6; i++) headerByte();
        if ((flags & 4) != 0) {
            int n = headerByte() | (headerByte() << 8);
            while (n-- > 0) headerByte();
        }
        if ((flags & 8) != 0)
            while (headerByte() != 0) {
            }
        if ((flags & 16) != 0)
            while (headerByte() != 0) {
            }
        if ((flags & 2) != 0) {
            int expected = (int) crc.getValue() & 65535;
            if ((byteValue() | (byteValue() << 8)) != expected)
                throw new ZipException("Invalid GZIP header CRC");
        }
        crc.reset();
    }
    private long word() throws IOException {
        long result = 0;
        for (int i = 0; i < 4; i++) result |= (long) byteValue() << (8 * i);
        return result;
    }
    public int read(byte[] b, int off, int length) throws IOException {
        ensureOpen();
        Deflater.bounds(b, off, length);
        if (length == 0)
            return 0;
        while (!eos) {
            int n = super.read(b, off, length);
            if (n > 0) {
                crc.update(b, off, n);
                return n;
            }
            if (!inf.finished())
                throw new ZipException("GZIP dictionary unsupported");
            int left = inf.getRemaining();
            if (left > 0)
                ((PushbackInputStream) in).unread(buf, len - left, left);
            if (word() != crc.getValue() || word() != (inf.getBytesWritten() & 0xffffffffL))
                throw new ZipException("Invalid GZIP trailer");
            int first = in.read();
            if (first < 0) {
                eos = true;
                break;
            }
            ((PushbackInputStream) in).unread(first);
            try {
                header();
            } catch (IOException trailingData) {
                // Like the JDK, ignore bytes that are not another complete header.
                // Payload and trailer errors after a valid header remain errors.
                eos = true;
                break;
            }
            inf.reset();
        }
        return -1;
    }
    public int available() throws IOException {
        ensureOpen();
        return eos ? 0 : 1;
    }
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            inf.end();
        }
    }
}
