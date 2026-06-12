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
package com.codename1.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link Base64}. Deliberately a plain test (it does NOT
 * extend {@code UITestBase} and never initializes {@code Display}) so that the
 * encode/decode paths run with no platform present.
 *
 * <p>This is also the regression guard for the pre-init crash: {@code Base64}
 * routes its output allocation through {@code allocByteMaybeSimd}, which calls
 * {@code Simd.get()} -> {@code Display.getInstance().getSimd()}. Before the fix
 * that raised a raw {@code NullPointerException} when {@code Display} was not
 * yet initialized; the allocation now falls back to a plain array (mirroring
 * {@code VertexBuffer.allocAligned}). The SIMD allocation branch only triggers
 * when the output buffer is &gt;= 16 bytes, so the larger inputs below exercise
 * exactly that path. Run this class in isolation to be sure no other test has
 * initialized {@code Display} first.</p>
 */
class Base64Test {

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void encodeNoNewlineMatchesKnownVectors() {
        // Classic RFC 4648 examples (full triple, and the two padding cases).
        assertEquals("TWFu", Base64.encodeNoNewline(ascii("Man")));
        assertEquals("TWE=", Base64.encodeNoNewline(ascii("Ma")));
        assertEquals("TQ==", Base64.encodeNoNewline(ascii("M")));
    }

    @Test
    void encodeNoNewlineHandlesEmptyInput() {
        assertEquals("", Base64.encodeNoNewline(new byte[0]));
    }

    @Test
    void encodeAndDecodeRoundTripAcrossLengths() {
        // Covers every length-mod-3 case and, crucially, inputs whose encoded
        // output is >= 16 bytes -- the SIMD-allocation branch that previously
        // crashed before Display was initialized.
        for (int len = 0; len <= 64; len++) {
            byte[] in = new byte[len];
            for (int i = 0; i < len; i++) {
                in[i] = (byte) ((i * 31 + 7) & 0xff);
            }
            String encoded = Base64.encodeNoNewline(in);
            byte[] decoded = Base64.decode(encoded.getBytes(StandardCharsets.US_ASCII));
            assertArrayEquals(in, decoded, "round-trip failed for length " + len);
        }
    }

    @Test
    void decodeReversesKnownVector() {
        // A 24-byte payload -> 32-char base64, exercising the >= 16 output path.
        byte[] in = ascii("abcdefghijklmnopqrstuvwx");
        String encoded = Base64.encodeNoNewline(in);
        assertEquals(32, encoded.length());
        assertArrayEquals(in, Base64.decode(encoded.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void encodeUrlSafeDropsPaddingAndUsesUrlAlphabet() {
        // 0xFB 0xEF 0xFF -> standard "++//" territory; url-safe maps + -> -,
        // / -> _ and strips '=' padding.
        byte[] in = {(byte) 0xfb, (byte) 0xef, (byte) 0xff};
        String standard = Base64.encodeNoNewline(in);
        String urlSafe = Base64.encodeUrlSafe(in);
        assertTrue(standard.indexOf('+') >= 0 || standard.indexOf('/') >= 0,
                "fixture should produce + or / in the standard alphabet: " + standard);
        assertFalse(urlSafe.contains("+"));
        assertFalse(urlSafe.contains("/"));
        assertFalse(urlSafe.contains("="));
    }

    @Test
    void encodeUrlSafeRoundTripsViaStandardDecodeForLargeInput() {
        // >= 16-byte output path again, via the url-safe entry point.
        byte[] in = new byte[40];
        for (int i = 0; i < in.length; i++) {
            in[i] = (byte) (255 - i);
        }
        String urlSafe = Base64.encodeUrlSafe(in);
        // Re-pad and translate back to the standard alphabet so decode() can read it.
        StringBuilder std = new StringBuilder(urlSafe.replace('-', '+').replace('_', '/'));
        while (std.length() % 4 != 0) {
            std.append('=');
        }
        byte[] decoded = Base64.decode(std.toString().getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(in, decoded);
    }

    @Test
    void encodeWithNewlinesRoundTripsForLargeInput() {
        // encode() (line-wrapped variant) over a long buffer; decode() ignores
        // the inserted newlines.
        byte[] in = new byte[200];
        for (int i = 0; i < in.length; i++) {
            in[i] = (byte) (i & 0xff);
        }
        String encoded = Base64.encode(in);
        byte[] decoded = Base64.decode(encoded.getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(in, decoded);
    }
}
