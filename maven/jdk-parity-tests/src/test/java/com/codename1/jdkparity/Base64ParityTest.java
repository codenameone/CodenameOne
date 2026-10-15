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
package com.codename1.jdkparity;

import jdkparity.java.util.Base64;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JavaAPI's Base64 beside the JDK's, on the same input: every encoder and
 * decoder, every outcome compared as text (the value, or the exception's
 * simple name).
 */
class Base64ParityTest {
    private int compared;

    private static String outcome(Callable<Object> call) {
        try {
            Object o = call.call();
            if (o instanceof byte[]) {
                return Arrays.toString((byte[]) o);
            }
            return String.valueOf(o);
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private void same(String what, Callable<Object> jdk, Callable<Object> api) {
        assertEquals(outcome(jdk), outcome(api), what);
        compared++;
    }

    private static List<byte[]> inputs() {
        List<byte[]> out = new ArrayList<byte[]>();
        Random random = new Random(42);
        for (int n = 0; n < 70; n++) {
            byte[] b = new byte[n];
            random.nextBytes(b);
            out.add(b);
        }
        for (int n : new int[] {113, 114, 115, 171, 1000}) {
            byte[] b = new byte[n];
            random.nextBytes(b);
            out.add(b);
        }
        out.add(new byte[] {-1, -1, -1, -5, -17, -65});
        return out;
    }

    @Test
    void thePaddingCharacterIsNoLineSeparator() {
        for (byte[] separator : new byte[][] {{'='}, {'\r', '='}, {'A'}}) {
            assertThrows(IllegalArgumentException.class, () -> java.util.Base64.getMimeEncoder(4, separator));
            assertThrows(IllegalArgumentException.class, () -> Base64.getMimeEncoder(4, separator));
        }
    }

    @Test
    void everyEncoderAndDecoderIsTheJdks() {
        final java.util.Base64.Encoder[] jdkEncoders = {java.util.Base64.getEncoder(),
            java.util.Base64.getUrlEncoder(), java.util.Base64.getMimeEncoder(),
            java.util.Base64.getEncoder().withoutPadding(), java.util.Base64.getUrlEncoder().withoutPadding(),
            java.util.Base64.getMimeEncoder(10, new byte[] {'!', '\n'}),
            java.util.Base64.getMimeEncoder(0, new byte[] {'!'}),
            java.util.Base64.getMimeEncoder(7, new byte[0]).withoutPadding()};
        final Base64.Encoder[] encoders = {Base64.getEncoder(), Base64.getUrlEncoder(), Base64.getMimeEncoder(),
            Base64.getEncoder().withoutPadding(), Base64.getUrlEncoder().withoutPadding(),
            Base64.getMimeEncoder(10, new byte[] {'!', '\n'}), Base64.getMimeEncoder(0, new byte[] {'!'}),
            Base64.getMimeEncoder(7, new byte[0]).withoutPadding()};
        final java.util.Base64.Decoder[] jdkDecoders = {java.util.Base64.getDecoder(),
            java.util.Base64.getUrlDecoder(), java.util.Base64.getMimeDecoder()};
        final Base64.Decoder[] decoders = {Base64.getDecoder(), Base64.getUrlDecoder(), Base64.getMimeDecoder()};
        List<String> texts = new ArrayList<String>();
        for (final byte[] in : inputs()) {
            for (int e = 0; e < encoders.length; e++) {
                final int at = e;
                same("string " + e + "/" + in.length, () -> jdkEncoders[at].encodeToString(in),
                        () -> encoders[at].encodeToString(in));
                same("bytes " + e + "/" + in.length, () -> jdkEncoders[at].encode(in), () -> encoders[at].encode(in));
                same("into " + e + "/" + in.length, () -> jdkEncoders[at].encode(in, new byte[in.length * 2 + 8]),
                        () -> encoders[at].encode(in, new byte[in.length * 2 + 8]));
                same("small " + e + "/" + in.length, () -> jdkEncoders[at].encode(in, new byte[in.length]),
                        () -> encoders[at].encode(in, new byte[in.length]));
                texts.add(jdkEncoders[e].encodeToString(in));
            }
        }
        texts.addAll(Arrays.asList("=", "==", "A", "A=", "A==", "AA", "AA=", "AA==", "AAA", "AAA=", "AAA==", "AAAA=",
                "AAAA", "AA==AA", "AA=A", "A A A A", "AAAA\nAAAA", "AA\n==", "AA=\n=", "AAA\n=", "****", "AAA*",
                "AAAA====", "-_-_", "+/+/", "QUJD\u00e9", "QUJD\u0142", "AAA=A", "AQ==\n", "AQ=="));
        for (final String text : texts) {
            for (int d = 0; d < decoders.length; d++) {
                final int at = d;
                same("decode " + d + " " + text, () -> jdkDecoders[at].decode(text), () -> decoders[at].decode(text));
                same("decode bytes " + d + " " + text,
                        () -> jdkDecoders[at].decode(text.getBytes(StandardCharsets.ISO_8859_1)),
                        () -> decoders[at].decode(text.getBytes(StandardCharsets.ISO_8859_1)));
            }
        }
        same("separator", () -> java.util.Base64.getMimeEncoder(8, new byte[] {'A'}),
                () -> Base64.getMimeEncoder(8, new byte[] {'A'}));
        assertTrue(compared > 5000, "" + compared);
    }
}
