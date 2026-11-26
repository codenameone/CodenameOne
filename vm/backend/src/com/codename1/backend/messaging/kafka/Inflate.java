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

/// What the decompressors add to the backend's [ByteSink]: a ceiling on what one
/// batch may inflate to, the back-reference copy the LZ77 family of codecs share,
/// and bounds-checked little-endian reads.
///
/// Every bound is checked here rather than left to an index exception: on
/// ParparVM an array access out of range is not reliably an exception, so a
/// corrupt batch must be refused before it is read past.
final class Inflate {
    /// The most one batch may decompress to. A fetch is capped at 4 MiB, so
    /// anything near this is a decompression bomb rather than a batch.
    static final int LIMIT = 64 * 1024 * 1024;

    private Inflate() {
    }

    /// A sink sized for `expected` bytes, without trusting a huge estimate.
    static ByteSink sink(int expected) {
        return new ByteSink(Math.max(64, Math.min(expected, 16 * 1024 * 1024)));
    }

    private static void room(ByteSink out, int n) throws IOException {
        if (n < 0 || n > LIMIT - out.length()) {
            throw new IOException("A Kafka batch decompresses to more than "
                    + (LIMIT >> 20) + " MiB");
        }
    }

    /// `n` bytes of `src` from `at`, which the caller has bounds checked.
    static void put(ByteSink out, byte[] src, int at, int n) throws IOException {
        room(out, n);
        out.put(src, at, n);
    }

    /// `n` copies of `b`.
    static void fill(ByteSink out, int b, int n) throws IOException {
        room(out, n);
        if (n == 0) {
            return;
        }
        out.put(b);
        // Each pass copies the run written so far, so it doubles: log n calls.
        int run = 1;
        while (run < n) {
            int c = Math.min(run, n - run);
            out.put(out.bytes(), out.length() - c, c);
            run += c;
        }
    }

    /// Copies `n` bytes from `distance` back, which may overlap what it
    /// writes; `floor` is where back-references may reach to.
    static void copy(ByteSink out, long distance, int n, int floor) throws IOException {
        if (distance <= 0 || distance > out.length() - floor) {
            throw new IOException("A compressed Kafka batch refers back past its start");
        }
        room(out, n);
        int d = (int) distance;
        // A chunk no longer than the distance never reads what it writes, so
        // an overlapping copy is a run of plain ones. ByteSink.put may grow the
        // array mid-call; it copies from the array it was handed, whose first
        // length bytes are unchanged.
        int left = n;
        while (left > 0) {
            int c = Math.min(left, d);
            out.put(out.bytes(), out.length() - d, c);
            left -= c;
        }
    }

    /// The sink's contents as an array of their own.
    static byte[] toArray(ByteSink out) {
        byte[] copy = new byte[out.length()];
        System.arraycopy(out.bytes(), 0, copy, 0, out.length());
        return copy;
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
