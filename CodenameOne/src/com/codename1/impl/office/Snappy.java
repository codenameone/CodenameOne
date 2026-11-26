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
package com.codename1.impl.office;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// Decompresses Snappy blocks and the chunked stream Keynote, Pages and Numbers store their
/// `.iwa` archives in.
///
/// An `.iwa` file is a run of chunks, each a type byte (0 for compressed data), a 24-bit
/// little-endian length and a raw Snappy block -- the framing of the Snappy stream format,
/// without its stream identifier or checksums. Every length is checked against the input and
/// the output is capped, so a damaged or hostile file fails with an `IOException` instead of
/// exhausting memory.
public final class Snappy {
    /// The largest output `decompress` produces unless told otherwise: 64 MB.
    public static final int DEFAULT_LIMIT = 64 * 1024 * 1024;

    private Snappy() {
    }

    /// Decompresses an `.iwa` file: every chunk, concatenated.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file
    ///
    /// - `limit`: the most bytes to produce
    ///
    /// #### Returns
    ///
    /// the decompressed archive
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the data is damaged or the output passes the limit
    public static byte[] decompressIwa(byte[] data, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(limit,
                Math.max(16, data.length * 3)));
        int pos = 0;
        int total = 0;
        while (pos < data.length) {
            if (pos + 4 > data.length) {
                throw new IOException("Truncated chunk header");
            }
            int type = data[pos] & 0xff;
            int len = (data[pos + 1] & 0xff) | (data[pos + 2] & 0xff) << 8
                    | (data[pos + 3] & 0xff) << 16;
            pos += 4;
            if (len > data.length - pos) {
                throw new IOException("Truncated chunk");
            }
            if (type == 0) {
                byte[] block = decompress(data, pos, len, limit - total);
                total += block.length;
                out.write(block, 0, block.length);
            } else if (type == 1) {
                // an uncompressed chunk
                if (len > limit - total) {
                    throw new IOException("Output limit passed");
                }
                total += len;
                out.write(data, pos, len);
            }
            // any other type is padding or metadata and is skipped
            pos += len;
        }
        return out.toByteArray();
    }

    /// Decompresses one raw Snappy block.
    ///
    /// #### Parameters
    ///
    /// - `in`: the input
    ///
    /// - `off`: where the block starts
    ///
    /// - `len`: its length
    ///
    /// - `limit`: the most bytes to produce
    ///
    /// #### Returns
    ///
    /// the decompressed bytes
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the block is damaged or larger than the limit
    public static byte[] decompress(byte[] in, int off, int len, int limit) throws IOException {
        int end = off + len;
        int pos = off;
        // the uncompressed length, a varint
        long size = 0;
        int shift = 0;
        while (true) {
            if (pos >= end || shift > 28) {
                throw new IOException("Bad Snappy length");
            }
            int b = in[pos++] & 0xff;
            size |= (long) (b & 0x7f) << shift;
            if (b < 0x80) {
                break;
            }
            shift += 7;
        }
        if (size > limit || size < 0) {
            throw new IOException("Output limit passed");
        }
        byte[] out = new byte[(int) size];
        int o = 0;
        while (pos < end) {
            int tag = in[pos++] & 0xff;
            int kind = tag & 3;
            if (kind == 0) {
                int n = tag >>> 2;
                if (n >= 60) {
                    int bytes = n - 59;
                    if (pos + bytes > end) {
                        throw new IOException("Truncated literal");
                    }
                    n = 0;
                    for (int i = 0; i < bytes; i++) {
                        n |= (in[pos++] & 0xff) << (8 * i);
                    }
                }
                long count = (n & 0xffffffffL) + 1;
                if (count > end - pos || count > out.length - o) {
                    throw new IOException("Bad literal");
                }
                System.arraycopy(in, pos, out, o, (int) count);
                pos += (int) count;
                o += (int) count;
                continue;
            }
            int length;
            int offset;
            if (kind == 1) {
                if (pos >= end) {
                    throw new IOException("Truncated copy");
                }
                length = 4 + ((tag >>> 2) & 7);
                offset = (tag & 0xe0) << 3 | (in[pos++] & 0xff);
            } else if (kind == 2) {
                if (pos + 2 > end) {
                    throw new IOException("Truncated copy");
                }
                length = 1 + (tag >>> 2);
                offset = (in[pos] & 0xff) | (in[pos + 1] & 0xff) << 8;
                pos += 2;
            } else {
                if (pos + 4 > end) {
                    throw new IOException("Truncated copy");
                }
                length = 1 + (tag >>> 2);
                offset = (in[pos] & 0xff) | (in[pos + 1] & 0xff) << 8
                        | (in[pos + 2] & 0xff) << 16 | (in[pos + 3] & 0xff) << 24;
                pos += 4;
            }
            if (offset <= 0 || offset > o || length > out.length - o) {
                throw new IOException("Bad copy");
            }
            // byte by byte: a copy may overlap what it is writing
            for (int i = 0; i < length; i++) {
                out[o] = out[o - offset];
                o++;
            }
        }
        if (o != out.length) {
            throw new IOException("Snappy block is short");
        }
        return out;
    }
}
