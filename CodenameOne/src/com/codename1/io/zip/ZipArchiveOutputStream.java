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
package com.codename1.io.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/// Streaming ZIP writer with optional Unix permissions. Works on the JVM and
/// ParparVM. Payloads are never buffered in full. Central-directory metadata is
/// limited to 16 MiB and 65534 entries; ZIP64 is explicitly refused.
/// finish() leaves the destination open; close() closes it even after a failure.
/// Input streams are owned by the caller. This stream is not thread safe.
@com.codename1.impl.SharedWithBackend
public class ZipArchiveOutputStream extends DeflaterOutputStream {
    public static final int STORED = 0;
    public static final int DEFLATED = 8;
    private static final long LIMIT = 0xffffffffL;
    private static final int METADATA_LIMIT = 16 * 1024 * 1024;
    private final ArrayList<Record> entries = new ArrayList<Record>();
    private final HashSet<String> names = new HashSet<String>();
    private final CRC32 crc = new CRC32();
    private final Charset encoding;
    private final int encodingFlag;
    private Record current;
    private ZipEntry original;
    private int method = DEFLATED;
    private int metadataSize;
    private long written;
    private long entryStart;
    private long entrySize;
    private boolean finished;
    private boolean closed;
    private IOException failure;
    private byte[] comment = new byte[0];
    private final byte[] writeBuffer = new byte[4];

    private static final class Record {
        ZipEntry entry;
        byte[] name;
        byte[] extra;
        byte[] comment;
        long offset;
        long dosTime;
        long attributes;
        int flags;
        int platform;
    }

    public ZipArchiveOutputStream(OutputStream out) {
        this(out, StandardCharsets.UTF_8);
    }
    public ZipArchiveOutputStream(OutputStream out, Charset charset) {
        super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true), 8192);
        if (charset == null) {
            throw new NullPointerException();
        }
        encoding = charset;
        encodingFlag = "UTF-8".equalsIgnoreCase(encoding.name()) ? 0x800 : 0;
    }

    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) {
            throw new IllegalArgumentException("Invalid method");
        }
        this.method = method;
    }
    public void setLevel(int level) {
        def.setLevel(level);
    }
    public void setComment(String comment) {
        try {
            this.comment = encode(comment == null ? "" : comment);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
    private byte[] encode(String value) throws IOException {
        byte[] bytes = value.getBytes(encoding);
        if (!value.equals(new String(bytes, encoding))) {
            throw new ZipException("ZIP field cannot be represented in " + encoding.name());
        }
        if (bytes.length > 65535) {
            throw new ZipException("Encoded ZIP field exceeds 65535 bytes");
        }
        return bytes;
    }
    private void writable() throws IOException {
        if (closed) {
            throw new IOException("ZIP stream closed");
        }
        if (failure != null) {
            throw failure;
        }
        if (finished) {
            throw new IOException("ZIP stream finished");
        }
    }
    /// Writes an entry without Unix attributes, like java.util.zip.ZipOutputStream.
    public void putNextEntry(ZipEntry entry) throws IOException {
        putEntry(entry, 0, 0);
    }

    /// Writes an entry with Unix permission bits (for example 0755 or 0644).
    /// File/directory type bits are inferred from the trailing slash. Symlinks
    /// and special files are not created. Values outside 0000..0777 are refused.
    public void putNextEntry(ZipEntry entry, int permissions) throws IOException {
        if (permissions < 0 || permissions > 0x1ff) {
            throw new IllegalArgumentException("Invalid permissions");
        }
        long mode = (entry.isDirectory() ? 0x4000 : 0x8000) | permissions;
        putEntry(entry, 3, (mode << 16) | (entry.isDirectory() ? 0x10 : 0));
    }
    private void putEntry(ZipEntry entry, int platform, long attributes) throws IOException {
        writable();
        if (entry == null) {
            throw new NullPointerException();
        }
        closeEntry();
        if (entries.size() >= 65534) {
            throw new ZipException("ZIP64 entry count unsupported");
        }
        if (names.contains(entry.getName())) {
            throw new ZipException("Duplicate entry: " + entry.getName());
        }
        Record r = new Record();
        r.entry = new ZipEntry(entry);
        if (r.entry.getMethod() == -1) {
            r.entry.setMethod(method);
        }
        if (r.entry.getTime() == -1) {
            r.entry.setTime(System.currentTimeMillis());
        }
        r.name = encode(r.entry.getName());
        r.comment = encode(r.entry.getComment() == null ? "" : r.entry.getComment());
        byte[] extra = r.entry.getExtra();
        r.extra = extra == null ? new byte[0] : extra.clone();
        validateExtra(r.extra);
        r.extra = timestampExtra(r.extra, r.entry.getTime());
        if (r.name.length == 0) {
            throw new ZipException("Empty entry name");
        }
        checkSize(r.entry.getSize());
        checkSize(r.entry.getCompressedSize());
        if (r.entry.getMethod() == STORED) {
            if (r.entry.getSize() < 0 && r.entry.getCompressedSize() >= 0) {
                r.entry.setSize(r.entry.getCompressedSize());
            }
            if (r.entry.getCompressedSize() < 0) {
                r.entry.setCompressedSize(r.entry.getSize());
            }
            if (r.entry.getSize() < 0 || r.entry.getCrc() < 0 || r.entry.getSize() != r.entry.getCompressedSize()) {
                throw new ZipException("STORED entries require size and CRC before writing");
            }
        }
        int metadata = 46 + r.name.length + r.extra.length + r.comment.length;
        if (metadataSize > METADATA_LIMIT - metadata) {
            throw new ZipException("ZIP central directory exceeds 16 MiB");
        }
        r.flags = encodingFlag | (r.entry.getMethod() == DEFLATED ? 8 : 0);
        r.platform = platform;
        r.attributes = attributes;
        r.offset = written;
        r.dosTime = dosTime(r.entry.getTime());
        current = r;
        original = entry;
        entrySize = 0;
        crc.reset();
        if (r.entry.getMethod() == DEFLATED) {
            def.reset();
        }
        try {
            word(0x04034b50L, 4);
            word(20, 2);
            word(r.flags, 2);
            word(r.entry.getMethod(), 2);
            word(r.dosTime, 4);
            word(r.entry.getMethod() == STORED ? r.entry.getCrc() : 0, 4);
            word(r.entry.getMethod() == STORED ? r.entry.getSize() : 0, 4);
            word(r.entry.getMethod() == STORED ? r.entry.getSize() : 0, 4);
            word(r.name.length, 2);
            word(r.extra.length, 2);
            raw(r.name);
            raw(r.extra);
            entryStart = written;
            entries.add(r);
            names.add(r.entry.getName());
            metadataSize += metadata;
        } catch (IOException ex) {
            failure = ex;
            throw ex;
        }
    }
    private static void validateExtra(byte[] extra) throws ZipException {
        int at = 0;
        while (at < extra.length) {
            if (extra.length - at < 4) {
                throw new ZipException("Malformed ZIP extra field");
            }
            int id = (extra[at] & 255) | ((extra[at + 1] & 255) << 8);
            int size = (extra[at + 2] & 255) | ((extra[at + 3] & 255) << 8);
            if (id == 1) {
                throw new ZipException("ZIP64 extra field unsupported");
            }
            if (size > extra.length - at - 4) {
                throw new ZipException("Malformed ZIP extra field");
            }
            at += 4 + size;
        }
    }
    private static void checkSize(long size) throws ZipException {
        if (size >= LIMIT) {
            throw new ZipException("ZIP64 size unsupported");
        }
    }
    private static byte[] timestampExtra(byte[] extra, long millis) throws ZipException {
        long seconds = millis / 1000;
        if (millis < 0 && millis % 1000 != 0) {
            seconds--;
        }
        boolean timestamp = seconds >= Integer.MIN_VALUE && seconds <= Integer.MAX_VALUE;
        int added = timestamp ? 9 : 0;
        // Replace any caller-provided extended timestamp with the entry's time.
        int kept = 0;
        for (int at = 0; at < extra.length;) {
            int size = 4 + (extra[at + 2] & 255) + ((extra[at + 3] & 255) << 8);
            if ((extra[at] & 255) != 0x55 || (extra[at + 1] & 255) != 0x54) {
                kept += size;
            }
            at += size;
        }
        if (kept > 65535 - added) {
            throw new ZipException("No room for ZIP timestamp extra field");
        }
        byte[] result = new byte[kept + added];
        int pos = 0;
        for (int at = 0; at < extra.length;) {
            int size = 4 + (extra[at + 2] & 255) + ((extra[at + 3] & 255) << 8);
            if ((extra[at] & 255) != 0x55 || (extra[at + 1] & 255) != 0x54) {
                System.arraycopy(extra, at, result, pos, size);
                pos += size;
            }
            at += size;
        }
        if (!timestamp) {
            return result;
        }
        result[pos] = 0x55;
        result[pos + 1] = 0x54;
        result[pos + 2] = 5;
        result[pos + 4] = 1;
        for (int i = 0; i < 4; i++) {
            result[pos + 5 + i] = (byte) (seconds >>> (8 * i));
        }
        return result;
    }
    private static long dosTime(long millis) {
        // The deterministic epoch timestamp predates the DOS range in every
        // timezone. Avoid calendar/timezone lookups for each such entry.
        if (millis == 0) {
            return 0x00210000L;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(new java.util.Date(millis));
        int year = c.get(Calendar.YEAR);
        if (year < 1980) {
            return 0x00210000L;
        }
        if (year > 2107) {
            return 0xff9fbf7dL;
        }
        return ((long) (year - 1980) << 25) | ((c.get(Calendar.MONTH) + 1) << 21) | (c.get(Calendar.DAY_OF_MONTH) << 16)
                | (c.get(Calendar.HOUR_OF_DAY) << 11) | (c.get(Calendar.MINUTE) << 5) | (c.get(Calendar.SECOND) >> 1);
    }
    @Override
    public void write(int value) throws IOException {
        writeBuffer[0] = (byte) value;
        write(writeBuffer, 0, 1);
    }
    @Override
    public void write(byte[] data, int off, int len) throws IOException {
        writable();
        if (data == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > data.length - len) {
            throw new IndexOutOfBoundsException();
        }
        if (current == null) {
            throw new ZipException("No current ZIP entry");
        }
        try {
            checkSize(entrySize + len);
            if (current.entry.getMethod() == STORED && entrySize + len > current.entry.getSize()) {
                throw new ZipException("STORED entry exceeds declared size");
            }
            crc.update(data, off, len);
            entrySize += len;
            if (current.entry.getMethod() == STORED) {
                raw(data, off, len);
            } else {
                def.setInput(data, off, len);
                while (!def.needsInput()) {
                    deflate();
                }
            }
        } catch (IOException ex) {
            failure = ex;
            throw ex;
        }
    }
    @Override
    protected void deflate() throws IOException {
        int n = def.deflate(buf, 0, buf.length);
        if (n > 0) {
            raw(buf, 0, n);
        }
    }
    public void closeEntry() throws IOException {
        writable();
        if (current == null) {
            return;
        }
        try {
            ZipEntry e = current.entry;
            if (e.getMethod() == DEFLATED) {
                def.finish();
                while (!def.finished()) {
                    deflate();
                }
            }
            long compressed = written - entryStart;
            checkSize(compressed);
            if ((e.getSize() >= 0 && e.getSize() != entrySize)
                    || (e.getCompressedSize() >= 0 && e.getCompressedSize() != compressed)
                    || (e.getCrc() >= 0 && e.getCrc() != crc.getValue())) {
                throw new ZipException("ZIP entry size or CRC mismatch");
            }
            e.setSize(entrySize);
            e.setCompressedSize(compressed);
            e.setCrc(crc.getValue());
            original.setSize(entrySize);
            original.setCompressedSize(compressed);
            original.setCrc(crc.getValue());
            if (e.getMethod() == DEFLATED) {
                word(0x08074b50L, 4);
                word(e.getCrc(), 4);
                word(compressed, 4);
                word(entrySize, 4);
            }
            current = null;
            original = null;
        } catch (IOException ex) {
            failure = ex;
            throw ex;
        }
    }
    @Override
    public void finish() throws IOException {
        if (closed) {
            throw new IOException("ZIP stream closed");
        }
        if (failure != null) {
            throw failure;
        }
        if (finished) {
            return;
        }
        closeEntry();
        try {
            long start = written;
            for (Record r : entries) {
                ZipEntry e = r.entry;
                word(0x02014b50L, 4);
                word((r.platform << 8) | 20, 2);
                word(20, 2);
                word(r.flags, 2);
                word(e.getMethod(), 2);
                word(r.dosTime, 4);
                word(e.getCrc(), 4);
                word(e.getCompressedSize(), 4);
                word(e.getSize(), 4);
                word(r.name.length, 2);
                word(r.extra.length, 2);
                word(r.comment.length, 2);
                word(0, 2);
                word(0, 2);
                word(r.attributes, 4);
                word(r.offset, 4);
                raw(r.name);
                raw(r.extra);
                raw(r.comment);
            }
            long length = written - start;
            word(0x06054b50L, 4);
            word(0, 2);
            word(0, 2);
            word(entries.size(), 2);
            word(entries.size(), 2);
            word(length, 4);
            word(start, 4);
            word(comment.length, 2);
            raw(comment);
            finished = true;
            entries.clear();
            names.clear();
        } catch (IOException ex) {
            failure = ex;
            throw ex;
        }
    }
    @Override
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("ZIP stream closed");
        }
        if (failure != null) {
            throw failure;
        }
        try {
            out.flush();
        } catch (IOException ex) {
            failure = ex;
            throw ex;
        }
    }
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        // An earlier write already delivered failure to the caller. Throwing
        // that same object again from close breaks try-with-resources through
        // Throwable's self-suppression guard.
        IOException error = null;
        try {
            if (failure == null) {
                finish();
            }
        } catch (IOException ex) {
            error = ex;
        } finally {
            closed = true;
            def.end();
            entries.clear();
            names.clear();
            current = null;
            original = null;
            try {
                out.close();
            } catch (IOException ex) {
                if (error == null) {
                    error = ex;
                }
            }
        }
        if (error != null) {
            throw error;
        }
    }
    private void word(long value, int bytes) throws IOException {
        for (int i = 0; i < bytes; i++) {
            writeBuffer[i] = (byte) value;
            value >>>= 8;
        }
        raw(writeBuffer, 0, bytes);
    }
    private void raw(byte[] data) throws IOException {
        raw(data, 0, data.length);
    }
    private void raw(byte[] data, int off, int len) throws IOException {
        checkSize(written + len);
        out.write(data, off, len);
        written += len;
    }
}
