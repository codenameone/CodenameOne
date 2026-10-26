/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security;

/// The C kernels ParparVM links into an application that uses this
/// package's elliptic-curve or Keccak APIs (libsecp256k1 and
/// ed25519-donna, see `cn1_crypto_kernels.c`). Only [NativeCryptoEngine]
/// calls these, and only after [#available()] answered `true`; every other
/// platform has no implementation of them.
///
/// Every array has the exact length the C side reads or writes; the engine
/// checks them first.
@com.codename1.impl.SharedWithBackend
final class NativeCryptoKernels {
    private NativeCryptoKernels() {
    }

    static native boolean available();

    static native boolean secpPublicKey(byte[] privateKey, byte[] out);

    static native boolean secpSign(byte[] hash, byte[] privateKey, byte[] out65);

    static native int secpVerify(byte[] hash, byte[] compact, byte[] publicKey);

    static native boolean secpRecover(byte[] hash, byte[] compact, int recoveryId, byte[] out);

    static native boolean secpPrivateTweakAdd(byte[] privateKey, byte[] tweak, byte[] out);

    static native boolean secpPublicTweakAdd(byte[] publicKey, byte[] tweak, byte[] out);

    static native boolean xOnlyPublicKey(byte[] privateKey, byte[] out);

    static native boolean schnorrSign(byte[] message, byte[] privateKey, byte[] auxRand, byte[] out);

    static native int schnorrVerify(byte[] message, byte[] signature, byte[] publicKey);

    static native void ed25519PublicKey(byte[] seed, byte[] out);

    static native void ed25519Sign(byte[] seed, byte[] publicKey, byte[] message, byte[] out);

    static native int ed25519Verify(byte[] message, byte[] signature, byte[] publicKey);

    static native void keccak256(byte[] data, int offset, int length, byte[] out);

    static native void sha256(byte[] data, int offset, int length, byte[] out);

    static native void sha512(byte[] data, int offset, int length, byte[] out);

    static native void ripemd160(byte[] data, int offset, int length, byte[] out);

    /// HMAC-SHA-256 (`bits` 256) or HMAC-SHA-512 (`bits` 512).
    static native void hmac(int bits, byte[] key, byte[] data, int offset, int length, byte[] out);

    /// PBKDF2 with HMAC-SHA-256 or -512, filling all of `out`.
    static native void pbkdf2(int bits, byte[] password, byte[] salt, int iterations, byte[] out);

    /// A batch over the cores: `algorithm` is a [CryptoEngine] `BATCH_`
    /// constant, or 11 for SHA-256 and 12 for Keccak-256 (null signatures and
    /// keys). Messages and ECDSA keys are slices given by offset arrays one
    /// longer than `count`; see `cn1_crypto_kernels.c` for the result bytes.
    static native void batch(int algorithm, byte[] messages, int[] messageOffsets, byte[] signatures,
            byte[] keys, int[] keyOffsets, byte[] results, int count);
}
