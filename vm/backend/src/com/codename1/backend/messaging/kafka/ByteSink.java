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

/// The output of a decompressor: a growing array with a ceiling, and the
/// back-reference copy the LZ77 family of codecs share.
///
/// Every bound is checked here rather than left to an index exception: on
/// ParparVM an array access out of range is not reliably an exception, so a
/// corrupt batch must be refused before it is read past.
final class ByteSink {
    /// The most one batch may decompress to. A fetch is capped at 4 MiB, so
    /// anything near this is a decompression bomb rather than a batch.
    static final int LIMIT = 256 * 1024 * 1024;

    private byte[] data;
    private int length;

    ByteSink(int expected) {
        data = new byte[Math.max(64, Math.min(expected, 16 * 1024 * 1024))];
    }

    int length() {
        return length;
    }

    private void room(int n) throws IOException {
        if (n < 0 || n > LIMIT - length) {
            throw new IOException("A Kafka batch decompresses to more than "
                    + (LIMIT >> 20) + " MiB");
        }
        if (length + n > data.length) {
            long want = Math.max((long) data.length * 2, (long) length + n);
            byte[] bigger = new byte[(int) Math.min(want, LIMIT)];
            System.arraycopy(data, 0, bigger, 0, length);
            data = bigger;
        }
    }

    void put(int b) throws IOException {
        room(1);
        data[length++] = (byte) b;
    }

    /// `n` bytes of `src` from `at`, which the caller has bounds checked.
    void put(byte[] src, int at, int n) throws IOException {
        room(n);
        System.arraycopy(src, at, data, length, n);
        length += n;
    }

    /// `n` copies of `b`.
    void fill(int b, int n) throws IOException {
        room(n);
        byte v = (byte) b;
        for (int i = 0; i < n; i++) {
            data[length + i] = v;
        }
        length += n;
    }

    /// Copies `n` bytes from `distance` back, which may overlap what it
    /// writes; `floor` is where back-references may reach to.
    void copy(long distance, int n, int floor) throws IOException {
        if (distance <= 0 || distance > length - floor) {
            throw new IOException("A compressed Kafka batch refers back past its start");
        }
        room(n);
        int from = length - (int) distance;
        if (distance >= n) {
            System.arraycopy(data, from, data, length, n);
        } else {
            for (int i = 0; i < n; i++) {
                data[length + i] = data[from + i];
            }
        }
        length += n;
    }

    byte[] toArray() {
        byte[] out = new byte[length];
        System.arraycopy(data, 0, out, 0, length);
        return out;
    }

    /// The little-endian `n`-byte integer at `at` of `in`, checked against `end`.
    static long le(byte[] in, int at, int n, int end) throws IOException {
        if (at < 0 || n > end - at) {
            throw new IOException("A compressed Kafka batch is truncated");
        }
        long v = 0;
        for (int i = n - 1; i >= 0; i--) {
            v = (v << 8) | (in[at + i] & 0xff);
        }
        return v;
    }
}
