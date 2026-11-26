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

/// Snappy decompression, for batches a producer sent with
/// `compression.type=snappy`.
///
/// The Java client writes the framing of snappy-java (xerial): an eight byte
/// magic, two version words, then blocks each preceded by its big-endian
/// length. Some other clients write one bare snappy block instead, so both are
/// read, told apart by the magic. Each block is the format
/// <https://github.com/google/snappy/blob/main/format_description.txt>
/// describes: a varint length, then literals and copies.
final class Snappy {
    private static final byte[] XERIAL = {(byte) 0x82, 'S', 'N', 'A', 'P', 'P', 'Y', 0};

    private Snappy() {
    }

    static byte[] decompress(byte[] in) throws IOException {
        ByteSink out = Inflate.sink(in.length * 3);
        if (xerial(in)) {
            int p = 16;
            while (p < in.length) {
                int n = (int) be32(in, p);
                p += 4;
                if (n < 0 || n > in.length - p) {
                    throw new IOException("A snappy Kafka batch is truncated");
                }
                block(in, p, p + n, out);
                p += n;
            }
        } else {
            block(in, 0, in.length, out);
        }
        return Inflate.toArray(out);
    }

    private static boolean xerial(byte[] in) {
        if (in.length < 16) {
            return false;
        }
        for (int i = 0; i < XERIAL.length; i++) {
            if (in[i] != XERIAL[i]) {
                return false;
            }
        }
        return true;
    }

    private static long be32(byte[] in, int at) throws IOException {
        if (in.length - at < 4) {
            throw new IOException("A snappy Kafka batch is truncated");
        }
        return ((in[at] & 0xffL) << 24) | ((in[at + 1] & 0xff) << 16)
                | ((in[at + 2] & 0xff) << 8) | (in[at + 3] & 0xff);
    }

    /// One raw block, appended to `out`. The block decoder is the core's, shared
    /// with the Keynote reader; what is Kafka's is the framing and the ceiling,
    /// which counts what the batch's earlier blocks produced.
    static void block(byte[] in, int from, int end, ByteSink out) throws IOException {
        byte[] block = com.codename1.impl.office.Snappy.decompress(in, from, end - from,
                Inflate.LIMIT - out.length());
        Inflate.put(out, block, 0, block.length);
    }
}
