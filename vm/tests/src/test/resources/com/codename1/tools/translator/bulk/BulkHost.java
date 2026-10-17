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
import com.codename1.security.Hash;

/// The host the ParparVM run of the bulk program installs before every check:
/// the digests and random bytes a built application's host offers. Never
/// loaded on HotSpot, where the JDK answers and no host is consulted.
public class BulkHost extends JdkHost {
    private static long seed = System.currentTimeMillis();

    /// Installs a fresh instance; [Bulk] calls it after every check.
    static void installDefault() {
        JdkHost.install(new BulkHost());
    }

    /// Test entropy only: a check compares shapes and lengths, never the
    /// values, and a production host draws these from the platform.
    @Override
    public void randomBytes(byte[] out) {
        java.util.Random r = new java.util.Random(seed++ ^ System.nanoTime());
        r.nextBytes(out);
    }

    /// The digests a built application's host offers, by the names
    /// `CoreJdkHost.digest` answers: the core's own `Hash`, which the bulk
    /// program compiles from `CodenameOne/src`.
    @Override
    public Digest digest(String algorithm) {
        if (!Hash.MD5.equalsIgnoreCase(algorithm) && !Hash.SHA1.equalsIgnoreCase(algorithm)
                && !Hash.SHA224.equalsIgnoreCase(algorithm) && !Hash.SHA256.equalsIgnoreCase(algorithm)
                && !Hash.SHA384.equalsIgnoreCase(algorithm) && !Hash.SHA512.equalsIgnoreCase(algorithm)) {
            return null;
        }
        final Hash hash = Hash.create(algorithm);
        return new Digest() {
            @Override
            public void update(byte[] data, int offset, int length) {
                hash.update(data, offset, length);
            }

            @Override
            public byte[] digest() {
                return hash.digest();
            }

            @Override
            public void reset() {
                hash.reset();
            }

            @Override
            public int length() {
                return hash.digestLength();
            }
        };
    }
}
