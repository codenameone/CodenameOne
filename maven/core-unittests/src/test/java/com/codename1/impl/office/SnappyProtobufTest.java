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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Snappy and protocol buffer readers the Keynote reader stands on.
class SnappyProtobufTest {
    private static byte[] b(int... v) {
        byte[] r = new byte[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = (byte) v[i];
        }
        return r;
    }

    @Test
    void literalsAndCopies() throws IOException {
        // "abcd" then a copy of 4 from 4 back
        byte[] blk = b(8, 0x0c, 'a', 'b', 'c', 'd', 0x01, 0x04);
        assertEquals("abcdabcd", new String(Snappy.decompress(blk, 0, blk.length, 100), "ASCII"));
        // an overlapping copy: one byte repeated
        byte[] run = b(8, 0x00, 'a', 0x1a, 0x01, 0x00);
        assertEquals("aaaaaaaa", new String(Snappy.decompress(run, 0, run.length, 100), "ASCII"));
    }

    @Test
    void iwaChunksConcatenate() throws IOException {
        byte[] blk = b(8, 0x0c, 'a', 'b', 'c', 'd', 0x01, 0x04);
        byte[] iwa = new byte[(4 + blk.length) * 2];
        for (int k = 0; k < 2; k++) {
            int at = k * (4 + blk.length);
            iwa[at + 1] = (byte) blk.length;
            System.arraycopy(blk, 0, iwa, at + 4, blk.length);
        }
        assertEquals(16, Snappy.decompressIwa(iwa, 100).length);
    }

    @Test
    void hostileSnappyFails() {
        // a copy reaching before the start
        assertThrows(IOException.class, () -> Snappy.decompress(b(4, 0x01, 0x09), 0, 3, 100));
        // a declared size over the limit
        assertThrows(IOException.class, () -> Snappy.decompress(b(0xff, 0xff, 0xff, 0x7f), 0, 4,
                1000));
        // a chunk longer than the file
        assertThrows(IOException.class, () -> Snappy.decompressIwa(b(0, 0xff, 0, 0, 1), 100));
        // a block that ends short of its declared size
        assertThrows(IOException.class, () -> Snappy.decompress(b(8, 0x00, 'a'), 0, 3, 100));
    }

    @Test
    void protobufFields() throws IOException {
        byte[] msg = b(0x08, 0x96, 0x01, 0x12, 0x02, 'h', 'i', 0x1a, 0x02, 0x08, 0x05,
                0x25, 0, 0, 0x80, 0x3f);
        Protobuf p = new Protobuf(msg, 0, msg.length);
        assertTrue(p.next());
        assertEquals(1, p.field());
        assertEquals(150, p.varint());
        assertTrue(p.next());
        assertEquals("hi", p.string());
        assertTrue(p.next());
        Protobuf inner = p.message();
        assertTrue(inner.next());
        assertEquals(5, inner.varint());
        assertFalse(inner.next());
        assertTrue(p.next());
        assertEquals(1f, p.float32(), 0);
        assertFalse(p.next());
    }

    @Test
    void hostileProtobufFails() {
        // a string longer than the message
        assertThrows(IOException.class, () -> new Protobuf(b(0x12, 0x7f, 'x'), 0, 3).next());
        // a varint that never ends
        assertThrows(IOException.class, () -> new Protobuf(b(0x08, 0xff, 0xff), 0, 3).next());
        // a group, which no current writer emits
        assertThrows(IOException.class, () -> new Protobuf(b(0x0b), 0, 1).next());
    }
}
