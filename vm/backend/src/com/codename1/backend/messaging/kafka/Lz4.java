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
package com.codename1.backend.messaging.kafka;

import com.codename1.backend.ByteSink;
import java.io.IOException;

/// LZ4 decompression, for batches a producer sent with
/// `compression.type=lz4`: the LZ4 frame format
/// (<https://github.com/lz4/lz4/blob/dev/doc/lz4_Frame_format.md>), whose
/// blocks are LZ4 blocks. Blocks of one frame may refer back into the blocks
/// before them, so the whole frame is decoded into one buffer. Checksums are
/// skipped: the record batch already carries a CRC over these bytes.
final class Lz4 {
    private static final int MAGIC = 0x184D2204;

    private Lz4() {
    }

    static byte[] decompress(byte[] in) throws IOException {
        ByteSink out = Inflate.sink(in.length * 3);
        int p = 0;
        int end = in.length;
        while (p < end) {
            int magic = (int) Inflate.le(in, p, 4, end);
            p += 4;
            if ((magic & 0xfffffff0) == 0x184D2A50) {
                long skip = Inflate.le(in, p, 4, end);
                p += 4;
                if (skip > end - p) {
                    throw new IOException("An lz4 Kafka batch is truncated");
                }
                p += (int) skip;
                continue;
            }
            if (magic != MAGIC) {
                throw new IOException("An lz4 Kafka batch is not an LZ4 frame");
            }
            p = frame(in, p, end, out);
        }
        return Inflate.toArray(out);
    }

    private static int frame(byte[] in, int at, int end, ByteSink out) throws IOException {
        int p = at;
        if (end - p < 3) {
            throw new IOException("An lz4 Kafka batch is truncated");
        }
        int flags = in[p] & 0xff;
        p += 2;
        if ((flags >>> 6) != 1) {
            throw new IOException("An lz4 Kafka batch has frame version " + (flags >>> 6));
        }
        if ((flags & 1) != 0) {
            throw new IOException("An lz4 Kafka batch needs a dictionary");
        }
        boolean blockChecksum = (flags & 0x10) != 0;
        if ((flags & 0x08) != 0) {
            p += 8;
        }
        p++;
        int floor = out.length();
        while (true) {
            long word = Inflate.le(in, p, 4, end);
            p += 4;
            if (word == 0) {
                break;
            }
            boolean stored = (word & 0x80000000L) != 0;
            int size = (int) (word & 0x7fffffff);
            if (size > end - p) {
                throw new IOException("An lz4 Kafka batch is truncated");
            }
            if (stored) {
                Inflate.put(out, in, p, size);
            } else {
                block(in, p, p + size, out, floor);
            }
            p += size + (blockChecksum ? 4 : 0);
        }
        if ((flags & 0x04) != 0) {
            p += 4;
        }
        if (p > end) {
            throw new IOException("An lz4 Kafka batch is truncated");
        }
        return p;
    }

    /// One LZ4 block, `in[from, end)`.
    static void block(byte[] in, int from, int end, ByteSink out, int floor) throws IOException {
        int p = from;
        while (p < end) {
            int token = in[p++] & 0xff;
            int literals = token >>> 4;
            if (literals == 15) {
                int[] at = {p};
                literals += extra(in, at, end);
                p = at[0];
            }
            if (literals > end - p) {
                throw new IOException("An lz4 Kafka batch is truncated");
            }
            Inflate.put(out, in, p, literals);
            p += literals;
            if (p == end) {
                return;
            }
            int distance = (int) Inflate.le(in, p, 2, end);
            p += 2;
            int length = token & 15;
            if (length == 15) {
                int[] at = {p};
                length += extra(in, at, end);
                p = at[0];
            }
            Inflate.copy(out, distance, length + 4, floor);
        }
    }

    /// An LZ4 length continuation: bytes added until one is not 255.
    private static int extra(byte[] in, int[] at, int end) throws IOException {
        int n = 0;
        int p = at[0];
        while (true) {
            if (p >= end) {
                throw new IOException("An lz4 Kafka batch is truncated");
            }
            int b = in[p++] & 0xff;
            n += b;
            if (n > Inflate.LIMIT) {
                throw new IOException("An lz4 Kafka batch is malformed");
            }
            if (b != 255) {
                at[0] = p;
                return n;
            }
        }
    }
}
