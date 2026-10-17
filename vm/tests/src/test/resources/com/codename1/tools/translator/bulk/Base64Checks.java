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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;

/// java.util.Base64: every encoder and decoder on the same input, every
/// outcome compared as text (the value, or the exception's class name).
public final class Base64Checks {
    private Base64Checks() {
    }

    /// One call whose outcome is compared.
    private interface Call {
        Object call() throws Exception;
    }

    private static String outcome(Call call) {
        try {
            Object o = call.call();
            if (o instanceof byte[]) {
                return Arrays.toString((byte[]) o);
            }
            return String.valueOf(o);
        } catch (Exception e) {
            return e.getClass().getName();
        }
    }

    /// `Random.nextBytes` as the JDK documents it: four bytes from each
    /// `nextInt`, low byte first. Written out rather than called because
    /// JavaAPI's takes one `next(8)` per byte, so the two runs would encode
    /// different inputs; that difference is not what this group compares.
    private static void fill(Random random, byte[] bytes) {
        for (int i = 0; i < bytes.length;) {
            int rnd = random.nextInt();
            for (int n = Math.min(bytes.length - i, 4); n-- > 0; rnd >>= 8) {
                bytes[i++] = (byte) rnd;
            }
        }
    }

    private static List<byte[]> inputs() {
        List<byte[]> out = new ArrayList<byte[]>();
        Random random = new Random(42);
        for (int n = 0; n < 70; n++) {
            byte[] b = new byte[n];
            fill(random, b);
            out.add(b);
        }
        for (int n : new int[] {113, 114, 115, 171, 1000}) {
            byte[] b = new byte[n];
            fill(random, b);
            out.add(b);
        }
        out.add(new byte[] {-1, -1, -1, -5, -17, -65});
        return out;
    }

    /// Runs the group.
    public static void run() {
        // The original filled its inputs with Random.nextBytes; this group
        // fills them by hand (see fill), and this check holds nextBytes
        // itself to the JDK's documented algorithm.
        Bulk.check("Base64.randomNextBytesAsTheInputsWereMade", () -> {
            Random random = new Random(42);
            for (int n : new int[] {0, 1, 3, 4, 6, 9}) {
                byte[] b = new byte[n];
                random.nextBytes(b);
                Bulk.p("nextBytes" + n, b);
            }
            Random byHand = new Random(42);
            for (int n : new int[] {0, 1, 3, 4, 6, 9}) {
                byte[] b = new byte[n];
                fill(byHand, b);
                Bulk.p("fill" + n, b);
            }
        });
        Bulk.check("Base64.thePaddingCharacterIsNoLineSeparator", () -> {
            byte[][] separators = {{'='}, {'\r', '='}, {'A'}};
            for (int i = 0; i < separators.length; i++) {
                final byte[] separator = separators[i];
                Bulk.pThrown("separator" + i, IllegalArgumentException.class,
                        () -> Base64.getMimeEncoder(4, separator));
            }
        });
        Bulk.check("Base64.everyEncoderAndDecoderIsTheJdks", () -> {
            final Base64.Encoder[] encoders = {Base64.getEncoder(), Base64.getUrlEncoder(), Base64.getMimeEncoder(),
                Base64.getEncoder().withoutPadding(), Base64.getUrlEncoder().withoutPadding(),
                Base64.getMimeEncoder(10, new byte[] {'!', '\n'}), Base64.getMimeEncoder(0, new byte[] {'!'}),
                Base64.getMimeEncoder(7, new byte[0]).withoutPadding()};
            final Base64.Decoder[] decoders = {Base64.getDecoder(), Base64.getUrlDecoder(), Base64.getMimeDecoder()};
            int compared = 0;
            List<String> texts = new ArrayList<String>();
            List<byte[]> inputs = inputs();
            for (int k = 0; k < inputs.size(); k++) {
                final byte[] in = inputs.get(k);
                // Every outcome of one input folded into a hash, one line per
                // input: the encodings of 1000 bytes are too long to print.
                int hash = 0;
                for (int e = 0; e < encoders.length; e++) {
                    final Base64.Encoder enc = encoders[e];
                    hash = 31 * hash + outcome(() -> enc.encodeToString(in)).hashCode();
                    hash = 31 * hash + outcome(() -> enc.encode(in)).hashCode();
                    hash = 31 * hash + outcome(() -> enc.encode(in, new byte[in.length * 2 + 8])).hashCode();
                    hash = 31 * hash + outcome(() -> enc.encode(in, new byte[in.length])).hashCode();
                    compared += 4;
                    texts.add(enc.encodeToString(in));
                }
                Bulk.p("encode" + k + " length " + in.length, hash);
            }
            Bulk.p("encodingOf6", encoders[0].encodeToString(inputs.get(6)));
            Bulk.p("mimeOf171", outcome(() -> encoders[2].encodeToString(inputs.get(73))));
            String[] literal = {"=", "==", "A", "A=", "A==", "AA", "AA=", "AA==", "AAA", "AAA=", "AAA==", "AAAA=",
                "AAAA", "AA==AA", "AA=A", "A A A A", "AAAA\nAAAA", "AA\n==", "AA=\n=", "AAA\n=", "****", "AAA*",
                "AAAA====", "-_-_", "+/+/", "QUJD" + (char) 0xE9, "QUJD" + (char) 0x142, "AAA=A", "AQ==\n", "AQ=="};
            int generated = texts.size();
            texts.addAll(Arrays.asList(literal));
            int hash = 0;
            for (int t = 0; t < texts.size(); t++) {
                final String text = texts.get(t);
                StringBuilder line = new StringBuilder();
                for (int d = 0; d < decoders.length; d++) {
                    final Base64.Decoder dec = decoders[d];
                    String fromString = outcome(() -> dec.decode(text));
                    String fromBytes = outcome(() -> dec.decode(text.getBytes(StandardCharsets.ISO_8859_1)));
                    compared += 2;
                    if (t < generated) {
                        hash = 31 * hash + fromString.hashCode();
                        hash = 31 * hash + fromBytes.hashCode();
                    } else {
                        line.append(" d").append(d).append('=').append(fromString).append(" b").append(d).append('=')
                                .append(fromBytes);
                    }
                }
                if (t < generated) {
                    if (t % 100 == 99 || t == generated - 1) {
                        Bulk.p("decodeGenerated upTo" + t, hash);
                    }
                } else {
                    Bulk.p("decode [" + Bulk.esc(text) + "]", line.toString());
                }
            }
            Bulk.pThrown("separatorA", IllegalArgumentException.class,
                    () -> Base64.getMimeEncoder(8, new byte[] {'A'}));
            Bulk.p("compared", compared);
        });
    }
}
