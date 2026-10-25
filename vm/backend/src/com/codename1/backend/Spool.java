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
package com.codename1.backend;

import java.io.IOException;
import java.io.InputStream;

/// Bytes held in memory up to a threshold and in an unlinked temporary file past
/// it: a large request body or multipart part, kept to a fixed amount of heap.
///
/// The file has no name from the moment it exists (FileIo.createTemp), so
/// [#close] is the whole of the cleanup and a crash leaves nothing behind. The
/// owner -- the request that read the body -- closes it when its exchange ends,
/// however it ends. Any number of streams from [#open] may read it at once: they
/// read by position and share nothing but the descriptor.
final class Spool {
    private final int threshold;
    private final long limit;
    private final String directory;
    private ByteSink memory;
    private int fd = -1;
    private long length;
    private boolean closed;

    /// @param threshold bytes kept in memory before the content moves to disk
    /// @param limit the most this spool accepts; [#write] answers false past it
    /// @param directory where the file goes, or null for the system default
    Spool(int threshold, long limit, String directory) {
        this.threshold = threshold;
        this.limit = limit;
        this.directory = directory;
        this.memory = new ByteSink(Math.max(1, Math.min(threshold, 8192)));
    }

    /// A spool over a file someone else already wrote -- the HTTP/2 receiver,
    /// which spools natively -- taking ownership of its descriptor.
    static Spool adopt(int fd, long length) {
        Spool spool = new Spool(0, length, null);
        spool.memory = null;
        spool.fd = fd;
        spool.length = length;
        return spool;
    }

    /// Appends a slice. False when it would take the content past the limit,
    /// with nothing written; IOException when the disk refused it.
    boolean write(byte[] data, int offset, int count) throws IOException {
        if (closed) {
            throw new IOException("spool is closed");
        }
        if (count > limit - length) {
            return false;
        }
        if (fd < 0 && length + count > threshold) {
            fd = FileIo.createTemp(directory);
            if (fd < 0) {
                throw new IOException("could not create a spool file in "
                        + (directory == null ? "the temporary directory" : directory));
            }
            if (memory.length() > 0 && !FileIo.write(fd, memory.bytes(), 0, memory.length())) {
                throw new IOException("could not write the spool file");
            }
            memory = null;
        }
        if (fd >= 0) {
            if (count > 0 && !FileIo.write(fd, data, offset, count)) {
                throw new IOException("could not write the spool file");
            }
        } else {
            memory.put(data, offset, count);
        }
        length += count;
        return true;
    }

    long length() {
        return length;
    }

    boolean isOnDisk() {
        return fd >= 0;
    }

    /// The whole content as one array. The caller decides whether that is
    /// affordable; a spool past [Integer#MAX_VALUE] cannot be one.
    byte[] toBytes() throws IOException {
        if (length > Integer.MAX_VALUE - 8) {
            throw new IOException("content of " + length + " bytes cannot be one array");
        }
        return toBytes(0, (int) length);
    }

    /// A new stream over the content from its start. Reading it after [#close]
    /// throws.
    InputStream open() {
        return new Reader(0, length);
    }

    /// A stream over `count` bytes from `offset`: one multipart part.
    InputStream open(long offset, long count) {
        if (offset < 0 || count < 0 || offset > length - count) {
            throw new IndexOutOfBoundsException(offset + "+" + count + " of " + length);
        }
        return new Reader(offset, offset + count);
    }

    /// Fills as much of `target` as there is from `position`; the count.
    int read(long position, byte[] target) throws IOException {
        int want = (int) Math.max(0, Math.min(target.length, length - position));
        int at = 0;
        while (at < want) {
            int n = readAt(position + at, target, at, want - at);
            at += n;
        }
        return want;
    }

    /// One read of at most `count` bytes at `position`, which must be inside.
    private int readAt(long position, byte[] target, int offset, int count) throws IOException {
        if (closed) {
            throw new IOException("the request that owned this content has ended");
        }
        if (fd < 0) {
            System.arraycopy(memory.bytes(), (int) position, target, offset, count);
            return count;
        }
        int n = FileIo.pread(fd, target, offset, count, position);
        if (n <= 0) {
            throw new IOException("could not read the spool file");
        }
        return n;
    }

    /// `count` bytes from `offset` as one array.
    byte[] toBytes(long offset, int count) throws IOException {
        byte[] out = new byte[count];
        int at = 0;
        while (at < count) {
            at += readAt(offset + at, out, at, count - at);
        }
        return out;
    }

    void close() {
        closed = true;
        memory = null;
        if (fd >= 0) {
            FileIo.close(fd);
            fd = -1;
        }
    }

    private final class Reader extends InputStream {
        private long position;
        private final long end;

        Reader(long start, long end) {
            this.position = start;
            this.end = end;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n <= 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] target, int offset, int count) throws IOException {
            if (count == 0) {
                return 0;
            }
            long left = end - position;
            if (left <= 0) {
                if (closed) {
                    throw new IOException("the request that owned this content has ended");
                }
                return -1;
            }
            int n = readAt(position, target, offset, (int) Math.min(count, left));
            position += n;
            return n;
        }

        @Override
        public long skip(long count) {
            long step = Math.max(0, Math.min(count, end - position));
            position += step;
            return step;
        }

        @Override
        public int available() {
            long left = end - position;
            return left > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, left);
        }
    }
}
