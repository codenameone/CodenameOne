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
package com.codename1.compat.jdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// Base64, URL coding, digests and the stream and text members, each
/// beside the JDK's own on the same input.
public class CodingDifferentialTest {
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

    private void same(String what, Callable<Object> jdk, Callable<Object> shim) {
        assertEquals(what, outcome(jdk), outcome(shim));
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
    public void base64IsTheJdks() {
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
        assertTrue("" + compared, compared > 5000);
    }

    @Test
    public void urlCodingIsTheJdks() {
        final String[] texts = {"", "plain", "a b+c", "a%20b", "caf\u00e9", "\u0142\u00f3d\u017a", "\ud83d\ude00 smile",
            "\ud83d alone", "a=1&b=2", "~!@#$%^&*()_+-=[]{}|;':\",./<>?", "100%", "%", "%4", "%zz", "%41%42",
            "%C3%A9t%C3%A9", "%E2%82%AC", "%e2%82%ac+%2B", "a%", "a%4", "%C3", "x%41", "%4g", "\u0000\u007f\u0080"};
        for (final String text : texts) {
            for (final String enc : new String[] {"UTF-8", "utf8", "ISO-8859-1", "US-ASCII", "nonsense"}) {
                same("encode " + text + enc, () -> java.net.URLEncoder.encode(text, enc),
                        () -> URLEncoder.encode(text, enc));
                // Which of a bad escape and an unknown encoding is reported
                // first has changed between releases of the JDK.
                if (!"nonsense".equals(enc) || "%41%42".equals(text)) {
                    same("decode " + text + enc, () -> java.net.URLDecoder.decode(text, enc),
                            () -> URLDecoder.decode(text, enc));
                }
            }
        }
        assertTrue("" + compared, compared > 200);
    }

    @Test
    public void digestsAreTheJdks() {
        for (final String name : new String[] {"MD5", "SHA-1", "SHA", "SHA1", "SHA-224", "SHA-256", "sha-256",
            "SHA-384", "SHA-512", "SHA-3", "CRC32", ""}) {
            same("known " + name, () -> java.security.MessageDigest.getInstance(name).getAlgorithm(),
                    () -> MessageDigest.getInstance(name).getAlgorithm());
            same("length " + name, () -> java.security.MessageDigest.getInstance(name).getDigestLength(),
                    () -> MessageDigest.getInstance(name).getDigestLength());
            for (final byte[] in : inputs()) {
                same("digest " + name + in.length, () -> java.security.MessageDigest.getInstance(name).digest(in),
                        () -> MessageDigest.getInstance(name).digest(in));
                same("pieces " + name + in.length, () -> {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance(name);
                    md.update(in, 0, in.length / 2);
                    md.update((byte) 7);
                    md.update(in);
                    byte[] first = md.digest();
                    md.update(in, in.length / 2, in.length - in.length / 2);
                    md.reset();
                    md.update(first);
                    return Arrays.toString(first) + Arrays.toString(md.digest());
                }, () -> {
                    MessageDigest md = MessageDigest.getInstance(name);
                    md.update(in, 0, in.length / 2);
                    md.update((byte) 7);
                    md.update(in);
                    byte[] first = md.digest();
                    md.update(in, in.length / 2, in.length - in.length / 2);
                    md.reset();
                    md.update(first);
                    return Arrays.toString(first) + Arrays.toString(md.digest());
                });
            }
        }
        final byte[][] pairs = {null, {}, {1}, {1, 2}, {1, 3}, {2, 1}, {1, 2, 3}};
        for (final byte[] a : pairs) {
            for (final byte[] b : pairs) {
                same("equal", () -> java.security.MessageDigest.isEqual(a, b), () -> MessageDigest.isEqual(a, b));
            }
        }
        assertTrue("" + compared, compared > 1500);
    }

    @Test
    public void streamsAreReadAsTheJdkReadsThem() throws Exception {
        for (byte[] in : inputs()) {
            assertArrayEquals(in, JdkIo.readAllBytes(new ByteArrayInputStream(in)));
            for (int n : new int[] {0, 1, in.length / 2, in.length, in.length + 5}) {
                assertArrayEquals(Arrays.copyOf(in, Math.min(n, in.length)),
                        JdkIo.readNBytes(new ByteArrayInputStream(in), n));
                byte[] into = new byte[in.length + 10];
                assertEquals(Math.min(n, in.length), JdkIo.readNBytes(new ByteArrayInputStream(in), into, 3, n));
                assertArrayEquals(Arrays.copyOf(in, Math.min(n, in.length)),
                        Arrays.copyOfRange(into, 3, 3 + Math.min(n, in.length)));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertEquals(in.length, JdkIo.transferTo(new ByteArrayInputStream(in), out));
            assertArrayEquals(in, out.toByteArray());
        }
        String text = "one\ntwo\r\nthree\rfour\n\nlast";
        StringWriter w = new StringWriter();
        assertEquals(text.length(), JdkIo.transferTo(new StringReader(text), w));
        assertEquals(text, w.toString());
        List<String> expected = new ArrayList<String>();
        java.io.BufferedReader jdk = new java.io.BufferedReader(new StringReader(text));
        for (String line = jdk.readLine(); line != null; line = jdk.readLine()) {
            expected.add(line);
        }
        assertEquals(expected, JdkIo.lines(new BufferedReader(new StringReader(text))).collect(Collectors.toList()));
    }

    @Test
    public void textMembersAreTheJdks() {
        for (final String s : new String[] {"", "abc", "a\ud83d\ude00b", "\ud83d", "\ude00\ud83d", "\ud83d\ud83d\ude00",
            "\u0142\u00f3d\u017a"}) {
            same("code points " + s, () -> Arrays.toString(s.codePoints().toArray()),
                    () -> Arrays.toString(JdkText.codePoints(s).toArray()));
            same("empty " + s, () -> s.isEmpty(), () -> JdkText.isEmpty(new StringBuilder(s)));
        }
        for (final int cp : new int[] {-1, 0, 65, 0xFFFF, 0x10000, 0x1F600, 0x10FFFF, 0x110000}) {
            same("append " + cp, () -> new StringBuilder("x").appendCodePoint(cp).toString(),
                    () -> JdkText.appendCodePoint(new StringBuilder("x"), cp).toString());
            same("append buffer " + cp, () -> new StringBuffer("x").appendCodePoint(cp).toString(),
                    () -> JdkText.appendCodePoint(new StringBuffer("x"), cp).toString());
        }
        List<String> list = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        assertEquals("a", JdkText.getFirst(list));
        assertEquals("c", JdkText.getLast(list));
        List<String> back = JdkText.reversed(list);
        assertEquals(Arrays.asList("c", "b", "a"), back);
        back.add("z");
        back.add(0, "y");
        assertEquals(Arrays.asList("z", "a", "b", "c", "y"), list);
        assertEquals("y", back.remove(0));
        assertEquals("z", JdkText.removeFirst(list));
        assertEquals("c", JdkText.removeLast(list));
        JdkText.addFirst(list, "0");
        JdkText.addLast(list, "9");
        assertEquals(Arrays.asList("0", "a", "b", "9"), list);
        assertEquals(Arrays.asList("9", "b", "a", "0"), back);
        assertTrue("" + compared, compared > 20);
    }
}
