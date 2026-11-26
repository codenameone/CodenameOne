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
        ByteSink out = new ByteSink(in.length * 3);
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
        return out.toArray();
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

    /// One snappy block, `in[from, end)`.
    static void block(byte[] in, int from, int end, ByteSink out) throws IOException {
        int p = from;
        long size = 0;
        for (int shift = 0;; shift += 7) {
            if (p >= end || shift > 28) {
                throw new IOException("A snappy Kafka batch has a malformed length");
            }
            int b = in[p++] & 0xff;
            size |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
        }
        if (size > ByteSink.LIMIT) {
            throw new IOException("A snappy Kafka batch claims " + size + " bytes");
        }
        int floor = out.length();
        long expected = floor + size;
        while (p < end) {
            int tag = in[p++] & 0xff;
            int kind = tag & 3;
            if (kind == 0) {
                int n = tag >>> 2;
                if (n >= 60) {
                    int bytes = n - 59;
                    n = (int) ByteSink.le(in, p, bytes, end);
                    p += bytes;
                    if (n < 0) {
                        throw new IOException("A snappy Kafka batch is malformed");
                    }
                }
                n++;
                if (n > end - p) {
                    throw new IOException("A snappy Kafka batch is truncated");
                }
                out.put(in, p, n);
                p += n;
            } else if (kind == 1) {
                if (p >= end) {
                    throw new IOException("A snappy Kafka batch is truncated");
                }
                int n = 4 + ((tag >>> 2) & 7);
                int distance = ((tag >>> 5) << 8) | (in[p++] & 0xff);
                out.copy(distance, n, floor);
            } else {
                int bytes = kind == 2 ? 2 : 4;
                long distance = ByteSink.le(in, p, bytes, end);
                p += bytes;
                out.copy(distance, (tag >>> 2) + 1, floor);
            }
            if (out.length() > expected) {
                throw new IOException("A snappy Kafka batch is longer than it claims");
            }
        }
        if (out.length() != expected) {
            throw new IOException("A snappy Kafka batch is shorter than it claims");
        }
    }
}
