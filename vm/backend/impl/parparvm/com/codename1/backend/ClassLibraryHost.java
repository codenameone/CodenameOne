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
package com.codename1.backend;

import com.codename1.impl.JdkHost;
import com.codename1.security.Hash;

/// The class library's [JdkHost] on a server compiled by ParparVM.
///
/// `java.security.MessageDigest` and `SecureRandom` in `vm/JavaAPI` ask the
/// installed host for their digests and random bytes. A client installs the
/// core's `CoreJdkHost`; nothing did on a server, so the default host answered
/// null and `MessageDigest.getInstance("SHA-256")` threw. Digests come from the
/// shared core `Hash` (the same pure-Java implementation the client uses,
/// incremental as `MessageDigest` needs) and random bytes from [Crypto].
///
/// The Java SE twin installs nothing: there the JDK is the class library.
final class ClassLibraryHost extends JdkHost {
    private ClassLibraryHost() {
    }

    /// Installs the host. Idempotent; called before anything in the server runs.
    static void install() {
        if (!(JdkHost.get() instanceof ClassLibraryHost)) {
            JdkHost.install(new ClassLibraryHost());
        }
    }

    @Override
    public void randomBytes(byte[] out) {
        byte[] bytes;
        try {
            bytes = Crypto.randomBytes(out.length);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No secure random source", e);
        }
        System.arraycopy(bytes, 0, out, 0, out.length);
    }

    @Override
    public Digest digest(String algorithm) {
        if (!Hash.MD5.equalsIgnoreCase(algorithm) && !Hash.SHA1.equalsIgnoreCase(algorithm)
                && !Hash.SHA224.equalsIgnoreCase(algorithm) && !Hash.SHA256.equalsIgnoreCase(algorithm)
                && !Hash.SHA384.equalsIgnoreCase(algorithm) && !Hash.SHA512.equalsIgnoreCase(algorithm)) {
            return null;
        }
        return new HashDigest(Hash.create(algorithm));
    }

    /// A shared core `Hash` as a [JdkHost.Digest].
    private static final class HashDigest implements Digest {
        private final Hash hash;

        HashDigest(Hash hash) {
            this.hash = hash;
        }

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
    }
}
