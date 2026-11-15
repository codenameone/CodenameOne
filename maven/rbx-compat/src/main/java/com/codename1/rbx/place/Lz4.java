/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.rbx.place;

/// An LZ4 *block* decompressor (no frame header): the form binary place
/// chunks are stored in. A block is a run of sequences, each a token whose
/// high nibble is a literal length and low nibble a match length minus four,
/// both extended by 255-valued bytes, followed by the literals and a two byte
/// little-endian back offset. The last sequence has literals only.
final class Lz4 {
    private Lz4() {
    }

    static byte[] decompress(byte[] src, int off, int len, int size) {
        byte[] out = new byte[size];
        int ip = off;
        int end = off + len;
        int op = 0;
        while (ip < end) {
            int token = src[ip++] & 0xff;
            int lit = token >>> 4;
            if (lit == 15) {
                int b;
                do {
                    b = src[ip++] & 0xff;
                    lit += b;
                } while (b == 255);
            }
            if (op + lit > size || ip + lit > end) {
                throw new IllegalArgumentException("Corrupt LZ4 block: literals overrun");
            }
            System.arraycopy(src, ip, out, op, lit);
            ip += lit;
            op += lit;
            if (ip >= end) {
                break;
            }
            int offset = (src[ip] & 0xff) | (src[ip + 1] & 0xff) << 8;
            ip += 2;
            int match = token & 15;
            if (match == 15) {
                int b;
                do {
                    b = src[ip++] & 0xff;
                    match += b;
                } while (b == 255);
            }
            match += 4;
            int from = op - offset;
            if (offset == 0 || from < 0 || op + match > size) {
                throw new IllegalArgumentException("Corrupt LZ4 block: bad match");
            }
            if (offset >= match) {
                System.arraycopy(out, from, out, op, match);
                op += match;
            } else {
                // Overlapping copy: the match repeats bytes it is writing.
                for (int i = 0; i < match; i++) {
                    out[op++] = out[from++];
                }
            }
        }
        if (op != size) {
            throw new IllegalArgumentException("Corrupt LZ4 block: " + op + " of " + size + " bytes");
        }
        return out;
    }
}
