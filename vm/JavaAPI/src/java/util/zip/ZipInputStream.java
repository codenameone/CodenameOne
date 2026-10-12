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
import java.nio.charset.Charset;
import java.util.Calendar;

/** Sequential ZIP reader for STORED and DEFLATED entries. ZIP64 and encryption are refused. */
public class ZipInputStream extends InflaterInputStream {
    private final CRC32 crc = new CRC32();
    private final Charset encoding;
    private byte[] drainBuffer;
    private ZipEntry entry;
    private long remaining, count;
    private int flags;
    private boolean end;
    public ZipInputStream(InputStream in) {
        this(in, Charset.forName("UTF-8"));
    }
    public ZipInputStream(InputStream in, Charset charset) {
        super(new PushbackInputStream(in, 8192), new Inflater(true), 8192);
        if (charset == null)
            throw new NullPointerException();
        encoding = charset;
    }
    public ZipEntry getNextEntry() throws IOException {
        ensureOpen();
        closeEntry();
        if (end)
            return null;
        int first = in.read();
        if (first < 0) {
            end = true;
            return null;
        }
        long signature = first | (word(3) << 8);
        if (signature == 0x02014b50L || signature == 0x06054b50L) {
            end = true;
            return null;
        }
        if (signature != 0x04034b50L)
            throw new ZipException("Invalid ZIP local header");
        word(2);
        flags = (int) word(2);
        int method = (int) word(2);
        long time = word(4);
        long expectedCrc = word(4), compressed = word(4), size = word(4);
        int nameLength = (int) word(2), extraLength = (int) word(2);
        if ((flags & (1 | 0x40 | 0x2000)) != 0)
            throw new ZipException("Encrypted ZIP unsupported");
        if (method != ZipEntry.STORED && method != ZipEntry.DEFLATED)
            throw new ZipException("Unsupported ZIP method");
        if (method == ZipEntry.STORED && (flags & 8) != 0)
            throw new ZipException("STORED descriptor unsupported");
        if (size == 0xffffffffL || compressed == 0xffffffffL)
            throw new ZipException("ZIP64 unsupported");
        byte[] name = bytes(nameLength), extra = bytes(extraLength);
        int at = 0;
        while (at < extra.length) {
            if (extra.length - at < 4)
                throw new ZipException("Malformed ZIP extra");
            int id = (extra[at] & 255) | ((extra[at + 1] & 255) << 8);
            int n = (extra[at + 2] & 255) | ((extra[at + 3] & 255) << 8);
            if (id == 1)
                throw new ZipException("ZIP64 unsupported");
            if (n > extra.length - at - 4)
                throw new ZipException("Malformed ZIP extra");
            at += 4 + n;
        }
        Charset nameEncoding = (flags & 0x800) != 0 ? Charset.forName("UTF-8") : encoding;
        String decodedName = new String(name, nameEncoding);
        if (!java.util.Arrays.equals(name, decodedName.getBytes(nameEncoding)))
            throw new ZipException("Invalid ZIP entry name encoding: " + nameEncoding.name());
        entry = createZipEntry(decodedName);
        entry.setMethod(method);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 1980 + (int) (time >>> 25));
        c.set(Calendar.MONTH, (int) ((time >>> 21) & 15) - 1);
        c.set(Calendar.DAY_OF_MONTH, (int) ((time >>> 16) & 31));
        c.set(Calendar.HOUR_OF_DAY, (int) ((time >>> 11) & 31));
        c.set(Calendar.MINUTE, (int) ((time >>> 5) & 63));
        c.set(Calendar.SECOND, (int) ((time & 31) * 2));
        c.set(Calendar.MILLISECOND, 0);
        entry.setTime(c.getTime().getTime());
        entry.setExtra(extra);
        if ((flags & 8) == 0) {
            entry.setCrc(expectedCrc);
            entry.setSize(size);
            entry.setCompressedSize(compressed);
        }
        if (method == ZipEntry.STORED && size != compressed)
            throw new ZipException("Invalid STORED sizes");
        remaining = size;
        count = 0;
        crc.reset();
        inf.reset();
        return entry;
    }
    protected ZipEntry createZipEntry(String name) {
        return new ZipEntry(name);
    }
    public int read(byte[] b, int off, int length) throws IOException {
        ensureOpen();
        Deflater.bounds(b, off, length);
        if (length == 0)
            return 0;
        if (entry == null)
            return -1;
        int n;
        if (entry.getMethod() == ZipEntry.STORED) {
            n = remaining == 0 ? -1 : in.read(b, off, (int) Math.min(remaining, length));
            if (n < 0 && remaining != 0)
                throw new EOFException("Truncated STORED entry");
            if (n > 0)
                remaining -= n;
        } else
            n = super.read(b, off, length);
        if (n > 0) {
            crc.update(b, off, n);
            count += n;
            if (count >= 0xffffffffL)
                throw new ZipException("ZIP64 unsupported");
            return n;
        }
        if (n == 0)
            return 0;
        long compressed = count;
        if (entry.getMethod() == ZipEntry.DEFLATED) {
            if (!inf.finished())
                throw new ZipException("ZIP entry requires a dictionary");
            compressed = inf.getBytesRead();
            int left = inf.getRemaining();
            if (left > 0)
                ((PushbackInputStream) in).unread(buf, len - left, left);
        }
        if ((flags & 8) != 0) {
            long value = word(4);
            if (value == 0x08074b50L)
                value = word(4);
            entry.setCrc(value);
            entry.setCompressedSize(word(4));
            entry.setSize(word(4));
        }
        if (entry.getCrc() != crc.getValue() || entry.getSize() != count || entry.getCompressedSize() != compressed)
            throw new ZipException("ZIP entry CRC or size mismatch");
        entry = null;
        return -1;
    }
    public void closeEntry() throws IOException {
        ensureOpen();
        if (entry == null)
            return;
        if (drainBuffer == null)
            drainBuffer = new byte[8192];
        while (entry != null && read(drainBuffer, 0, drainBuffer.length) >= 0) {
        }
    }
    public int available() throws IOException {
        ensureOpen();
        return entry == null ? 0 : 1;
    }
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            inf.end();
        }
    }
    private long word(int n) throws IOException {
        long value = 0;
        for (int i = 0; i < n; i++) {
            int b = in.read();
            if (b < 0)
                throw new EOFException("Truncated ZIP");
            value |= (long) b << (8 * i);
        }
        return value;
    }
    private byte[] bytes(int n) throws IOException {
        byte[] b = new byte[n];
        int off = 0;
        while (off < n) {
            int read = in.read(b, off, n - off);
            if (read < 0)
                throw new EOFException("Truncated ZIP");
            if (read == 0) {
                b[off++] = (byte) word(1);
            } else
                off += read;
        }
        return b;
    }
}
