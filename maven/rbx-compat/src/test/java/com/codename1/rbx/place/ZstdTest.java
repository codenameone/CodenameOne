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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/// Decodes vectors produced by the reference `zstd` command line tool. The
/// plaintext is regenerated here by the same deterministic generator that
/// produced the compressor's input, so only the compressed files are stored.
class ZstdTest {
    private static final String[] WORDS = ("the quick brown fox jumps over lazy dog place script part model"
            + " brick anchor color size vector frame part tween humanoid workspace lighting camera sound gui"
            + " label button value event wait spawn").split(" ");

    private static byte[] gen(String kind, int n, long seed) {
        if ("long".equals(kind)) {
            byte[] t = gen("text", 131072, seed);
            byte[] r = gen("rep", 6 * 1024 * 1024, 0);
            byte[] o = new byte[t.length * 2 + r.length];
            System.arraycopy(t, 0, o, 0, t.length);
            System.arraycopy(r, 0, o, t.length, r.length);
            System.arraycopy(t, 0, o, t.length + r.length, t.length);
            return o;
        }
        byte[] out = new byte[n];
        long st = seed;
        if ("rand".equals(kind)) {
            for (int i = 0; i < n; i++) {
                st = st * 6364136223846793005L + 1442695040888963407L;
                out[i] = (byte) (st >>> 33);
            }
        } else if ("text".equals(kind)) {
            int p = 0;
            while (p < n) {
                st = st * 6364136223846793005L + 1442695040888963407L;
                long r = st >>> 33;
                String w = WORDS[(int) (r % WORDS.length)];
                for (int i = 0; i < w.length() && p < n; i++) {
                    out[p++] = (byte) w.charAt(i);
                }
                if (p < n) {
                    out[p++] = (byte) ((r >>> 8) % 11 == 0 ? 10 : 32);
                }
            }
        } else if ("rep".equals(kind)) {
            for (int i = 0; i < n; i++) {
                out[i] = (byte) ((i * 7) % 253 + ((i >> 14) & 3));
            }
        }
        return out;
    }

    private static byte[] resource(String name) throws IOException {
        InputStream in = ZstdTest.class.getResourceAsStream("/zstd/" + name + ".zst");
        if (in == null) {
            throw new IOException("missing " + name);
        }
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                bo.write(buf, 0, r);
            }
            return bo.toByteArray();
        } finally {
            in.close();
        }
    }

    private static void check(String name, byte[] expected) throws IOException {
        byte[] z = resource(name);
        assertArrayEquals(expected, Zstd.decompress(z, 0, z.length, -1), name);
        assertArrayEquals(expected, Zstd.decompress(z, 0, z.length, expected.length), name + " sized");
        byte[] shifted = new byte[z.length + 5];
        System.arraycopy(z, 0, shifted, 3, z.length);
        assertArrayEquals(expected, Zstd.decompress(shifted, 3, z.length, 1), name + " offset");
    }

    @Test
    void emptyAndTiny() throws IOException {
        check("empty", new byte[0]);
        check("one", gen("text", 1, 1));
        assertArrayEquals(new byte[0], Zstd.decompress(new byte[0], 0, 0, -1));
    }

    @Test
    void textAtEveryLevel() throws IOException {
        check("text-l1", gen("text", 100000, 1));
        check("text-l3", gen("text", 100000, 2));
        check("text-l19", gen("text", 100000, 3));
        check("text-l22", gen("text", 100000, 4));
        check("text-nocheck", gen("text", 30000, 5));
        check("text-nosize", gen("text", 30000, 6));
    }

    @Test
    void randomRepetitiveAndZeros() throws IOException {
        check("rand-l3", gen("rand", 40000, 7));
        check("rep-l1", gen("rep", 4194304, 0));
        check("rep-l19", gen("rep", 4194304, 0));
        check("zero-l3", new byte[1000000]);
    }

    @Test
    void longDistanceMatches() throws IOException {
        check("long-l3", gen("long", 0, 9));
        check("long-l19", gen("long", 0, 9));
    }

    @Test
    void concatenatedFramesWithSkippableFrame() throws IOException {
        byte[] a = gen("text", 20000, 11);
        byte[] b = gen("rand", 1000, 12);
        byte[] both = new byte[a.length + b.length];
        System.arraycopy(a, 0, both, 0, a.length);
        System.arraycopy(b, 0, both, a.length, b.length);
        check("concat", both);
    }

    @Test
    void rejectsBadInput() throws IOException {
        final byte[] z = resource("text-l3");
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                Zstd.decompress(z, 0, z.length / 2, -1);
            }
        });
        final byte[] bad = {1, 2, 3, 4, 5};
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                Zstd.decompress(bad, 0, bad.length, -1);
            }
        });
        final byte[] dict = {(byte) 0x28, (byte) 0xB5, (byte) 0x2F, (byte) 0xFD, 0x21, 7, 0, 1, 0, 0};
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                Zstd.decompress(dict, 0, dict.length, -1);
            }
        });
    }
}
