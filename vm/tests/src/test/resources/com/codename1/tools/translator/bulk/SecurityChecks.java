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

import com.codename1.impl.JdkHost;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/// java.security: digests and secure random bytes. On ParparVM the digests
/// come from the host, as they do in an application: [BulkHost] answers
/// them with the core's own `Hash`.
public final class SecurityChecks {
    private SecurityChecks() {
    }

    private interface Op {
        Object run() throws Exception;
    }

    private static String run(Op op) {
        try {
            return String.valueOf(op.run());
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private static byte[][] inputs() {
        byte[] big = new byte[1000];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) (i * 31 + 7);
        }
        return new byte[][] {{}, {0}, "abc".getBytes(StandardCharsets.UTF_8), new byte[55], new byte[56],
            new byte[64], new byte[111], new byte[112], new byte[128], big};
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("Security.digestsAreTheJdks", () -> {
            for (String name : new String[] {"MD5", "SHA-1", "SHA", "SHA1", "SHA-224", "SHA-256", "sha-256",
                "SHA-384", "SHA-512", "SHA-3", "CRC32", ""}) {
                Bulk.p("algorithm[" + name + "]", run(() -> MessageDigest.getInstance(name).getAlgorithm()));
                Bulk.p("length[" + name + "]", run(() -> MessageDigest.getInstance(name).getDigestLength()));
                for (byte[] in : inputs()) {
                    Bulk.p("digest[" + name + "," + in.length + "]",
                            run(() -> Arrays.toString(MessageDigest.getInstance(name).digest(in))));
                    Bulk.p("pieces[" + name + "," + in.length + "]", run(() -> {
                        MessageDigest md = MessageDigest.getInstance(name);
                        md.update(in, 0, in.length / 2);
                        md.update((byte) 7);
                        md.update(in);
                        byte[] first = md.digest();
                        md.update(in, in.length / 2, in.length - in.length / 2);
                        md.reset();
                        md.update(first);
                        return Arrays.toString(first) + Arrays.toString(md.digest());
                    }));
                }
            }
            byte[][] pairs = {null, {}, {1}, {1, 2}, {1, 3}, {2, 1}, {1, 2, 3}};
            StringBuilder eq = new StringBuilder();
            for (byte[] a : pairs) {
                for (byte[] b : pairs) {
                    eq.append(MessageDigest.isEqual(a, b) ? '1' : '0');
                }
            }
            Bulk.p("isEqual", eq.toString());
            Bulk.pThrown("getInstanceNull", NullPointerException.class, () -> MessageDigest.getInstance(null));
            Bulk.pThrown("updateNull", IllegalArgumentException.class,
                    () -> MessageDigest.getInstance("MD5").update(null, 0, 1));
            Object e = new NoSuchAlgorithmException("x");
            Bulk.p("isGeneralSecurityException", e instanceof GeneralSecurityException);
        });
        // A host the check installs answers every secure random byte.
        Bulk.cn1Only("Security.secureRandomReadsTheHost", () -> {
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
            Bulk.expect(Arrays.equals(expected, bytes), "nextBytes is the host's: " + Arrays.toString(bytes));
            Bulk.expect(Arrays.equals(new byte[] {0x5A, 0x5A}, random.generateSeed(2)), "generateSeed");
            Bulk.expectEquals(Integer.valueOf(0x5A5A5A5A), Integer.valueOf(random.nextInt()), "nextInt");
            Bulk.expectEquals(Integer.valueOf(15), Integer.valueOf(asked[0]), "bytes asked of the host");
            Bulk.expectEquals("IllegalArgumentException",
                    Bulk.thrown(IllegalArgumentException.class, () -> random.generateSeed(-1)), "generateSeed(-1)");
            Bulk.expectEquals("NativePRNG", SecureRandom.getInstanceStrong().getAlgorithm(), "getInstanceStrong");
            // With no host installed the defaults have no source and refuse.
            JdkHost.install(null);
            Bulk.expectEquals("UnsupportedOperationException",
                    Bulk.thrown(UnsupportedOperationException.class, () -> new SecureRandom().nextBytes(new byte[1])),
                    "the defaults refuse");
        });
    }
}
