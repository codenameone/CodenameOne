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

import com.codename1.impl.CoreJdkHostInstaller;
import com.codename1.impl.JdkHost;
import jdkparity.java.security.GeneralSecurityException;
import jdkparity.java.security.MessageDigest;
import jdkparity.java.security.NoSuchAlgorithmException;
import jdkparity.java.security.SecureRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JavaAPI's {@code java.security} against the JDK's, over the core's host,
 * whose digests are the core's own {@code Hash}: the same bytes digest to the
 * same values, an unknown algorithm is refused alike, and secure random bytes
 * come from the host.
 */
class SecurityParityTest {

    @BeforeEach
    void installCoreHost() {
        CoreJdkHostInstaller.install();
    }

    @AfterEach
    void restore() {
        JdkHost.install(null);
    }

    private static byte[][] inputs() {
        byte[] big = new byte[1000];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) (i * 31 + 7);
        }
        return new byte[][] {{}, {0}, "abc".getBytes(StandardCharsets.UTF_8), new byte[55], new byte[56],
            new byte[64], new byte[111], new byte[112], new byte[128], big};
    }

    private static String run(Op op) {
        try {
            return String.valueOf(op.run());
        } catch (java.security.NoSuchAlgorithmException | NoSuchAlgorithmException e) {
            return "NoSuchAlgorithmException";
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private interface Op {
        Object run() throws Exception;
    }

    @Test
    void digestsAreTheJdks() {
        int compared = 0;
        for (String name : new String[] {"MD5", "SHA-1", "SHA", "SHA1", "SHA-224", "SHA-256", "sha-256",
            "SHA-384", "SHA-512", "SHA-3", "CRC32", ""}) {
            assertEquals(run(() -> java.security.MessageDigest.getInstance(name).getAlgorithm()),
                    run(() -> MessageDigest.getInstance(name).getAlgorithm()), name);
            assertEquals(run(() -> java.security.MessageDigest.getInstance(name).getDigestLength()),
                    run(() -> MessageDigest.getInstance(name).getDigestLength()), name);
            for (byte[] in : inputs()) {
                assertEquals(run(() -> Arrays.toString(java.security.MessageDigest.getInstance(name).digest(in))),
                        run(() -> Arrays.toString(MessageDigest.getInstance(name).digest(in))), name + in.length);
                assertEquals(run(() -> {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance(name);
                    md.update(in, 0, in.length / 2);
                    md.update((byte) 7);
                    md.update(in);
                    byte[] first = md.digest();
                    md.update(in, in.length / 2, in.length - in.length / 2);
                    md.reset();
                    md.update(first);
                    return Arrays.toString(first) + Arrays.toString(md.digest());
                }), run(() -> {
                    MessageDigest md = MessageDigest.getInstance(name);
                    md.update(in, 0, in.length / 2);
                    md.update((byte) 7);
                    md.update(in);
                    byte[] first = md.digest();
                    md.update(in, in.length / 2, in.length - in.length / 2);
                    md.reset();
                    md.update(first);
                    return Arrays.toString(first) + Arrays.toString(md.digest());
                }), "pieces " + name + in.length);
                compared += 2;
            }
        }
        assertTrue(compared > 200);
        byte[][] pairs = {null, {}, {1}, {1, 2}, {1, 3}, {2, 1}, {1, 2, 3}};
        for (byte[] a : pairs) {
            for (byte[] b : pairs) {
                assertEquals(java.security.MessageDigest.isEqual(a, b), MessageDigest.isEqual(a, b));
            }
        }
        assertThrows(NullPointerException.class, () -> MessageDigest.getInstance(null));
        assertThrows(IllegalArgumentException.class, () -> MessageDigest.getInstance("MD5").update(null, 0, 1));
        assertTrue(new NoSuchAlgorithmException("x") instanceof GeneralSecurityException);
    }

    @Test
    void secureRandomReadsTheHost() {
        final int[] asked = new int[1];
        JdkHost.install(new JdkHost() {
            @Override
            public void randomBytes(byte[] out) {
                asked[0] += out.length;
                Arrays.fill(out, (byte) 0x5A);
            }
        });
        SecureRandom random = new SecureRandom();
        random.setSeed(1L);
        byte[] bytes = new byte[9];
        random.nextBytes(bytes);
        byte[] expected = new byte[9];
        Arrays.fill(expected, (byte) 0x5A);
        assertArrayEquals(expected, bytes);
        assertArrayEquals(new byte[] {0x5A, 0x5A}, random.generateSeed(2));
        assertEquals(0x5A5A5A5A, random.nextInt());
        assertEquals(15, asked[0]);
        assertThrows(IllegalArgumentException.class, () -> random.generateSeed(-1));
        assertEquals("NativePRNG", SecureRandom.getInstanceStrong().getAlgorithm());
        // The core's source needs an initialized Codename One, which a unit
        // test does not have; with none installed the defaults refuse.
        JdkHost.install(null);
        assertThrows(UnsupportedOperationException.class, () -> new SecureRandom().nextBytes(new byte[1]));
    }
}
