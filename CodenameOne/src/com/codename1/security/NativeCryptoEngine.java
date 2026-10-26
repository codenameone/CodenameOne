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

/// The [CryptoEngine] of the ParparVM targets (iOS, macOS, native Windows and
/// Linux, and the backend): secp256k1, Schnorr, Ed25519 and Keccak run in
/// the linked C kernels, everything else goes to the engine it wraps.
///
/// The kernels are linked only into a build whose classes use these APIs,
/// so a port creates this engine through [#create(CryptoEngine)], which
/// answers the wrapped engine when they are absent.
@com.codename1.impl.SharedWithBackend
public class NativeCryptoEngine extends CryptoEngine {
    private static final int PUB_COMPRESSED = 33;
    private static final int PUB_UNCOMPRESSED = 65;

    private final CryptoEngine next;

    /// Creates the engine over the one that handles what the kernels do not.
    protected NativeCryptoEngine(CryptoEngine next) {
        this.next = next == null ? new CryptoEngine() : next;
    }

    /// This engine over `next` when the kernels are linked, otherwise
    /// `next` itself. Call it only on a ParparVM target: elsewhere the
    /// native methods have no implementation.
    public static CryptoEngine create(CryptoEngine next) {
        if (NativeCryptoKernels.available()) {
            return new NativeCryptoEngine(next);
        }
        return next;
    }

    @Override
    public byte[] pbkdf2(String hashAlgorithm, byte[] password, byte[] salt, int iterations, int length) {
        return next.pbkdf2(hashAlgorithm, password, salt, iterations, length);
    }

    @Override
    public byte[] secpPublicKey(byte[] privateKey, boolean compressed) {
        if (!is(privateKey, 32)) {
            return null;
        }
        byte[] out = new byte[compressed ? PUB_COMPRESSED : PUB_UNCOMPRESSED];
        return NativeCryptoKernels.secpPublicKey(privateKey, out) ? out : null;
    }

    @Override
    public byte[] secpSign(byte[] hash, byte[] privateKey) {
        if (!is(hash, 32) || !is(privateKey, 32)) {
            return null;
        }
        byte[] out = new byte[65];
        return NativeCryptoKernels.secpSign(hash, privateKey, out) ? out : null;
    }

    @Override
    public int secpVerify(byte[] hash, byte[] compactSignature, byte[] publicKey) {
        if (!is(hash, 32) || !is(compactSignature, 64) || !isPublicKeyLength(publicKey)) {
            return -1;
        }
        return NativeCryptoKernels.secpVerify(hash, compactSignature, publicKey);
    }

    @Override
    public byte[] secpRecover(byte[] hash, byte[] compactSignature, int recoveryId, boolean compressed) {
        if (!is(hash, 32) || !is(compactSignature, 64) || recoveryId < 0 || recoveryId > 3) {
            return null;
        }
        byte[] out = new byte[compressed ? PUB_COMPRESSED : PUB_UNCOMPRESSED];
        return NativeCryptoKernels.secpRecover(hash, compactSignature, recoveryId, out) ? out : null;
    }

    @Override
    public byte[] secpPrivateKeyTweakAdd(byte[] privateKey, byte[] tweak) {
        if (!is(privateKey, 32) || !is(tweak, 32)) {
            return null;
        }
        byte[] out = new byte[32];
        return NativeCryptoKernels.secpPrivateTweakAdd(privateKey, tweak, out) ? out : null;
    }

    @Override
    public byte[] secpPublicKeyTweakAdd(byte[] publicKey, byte[] tweak, boolean compressed) {
        if (!isPublicKeyLength(publicKey) || !is(tweak, 32)) {
            return null;
        }
        byte[] out = new byte[compressed ? PUB_COMPRESSED : PUB_UNCOMPRESSED];
        return NativeCryptoKernels.secpPublicTweakAdd(publicKey, tweak, out) ? out : null;
    }

    @Override
    public byte[] schnorrPublicKey(byte[] privateKey) {
        if (!is(privateKey, 32)) {
            return null;
        }
        byte[] out = new byte[32];
        return NativeCryptoKernels.xOnlyPublicKey(privateKey, out) ? out : null;
    }

    @Override
    public byte[] schnorrSign(byte[] message, byte[] privateKey, byte[] auxRand) {
        if (message == null || !is(privateKey, 32) || (auxRand != null && auxRand.length != 32)) {
            return null;
        }
        byte[] out = new byte[64];
        return NativeCryptoKernels.schnorrSign(message, privateKey, auxRand, out) ? out : null;
    }

    @Override
    public int schnorrVerify(byte[] message, byte[] signature, byte[] publicKey) {
        if (message == null || !is(signature, 64) || !is(publicKey, 32)) {
            return -1;
        }
        return NativeCryptoKernels.schnorrVerify(message, signature, publicKey);
    }

    @Override
    public byte[] ed25519PublicKey(byte[] privateKey) {
        if (!is(privateKey, 32)) {
            return null;
        }
        byte[] out = new byte[32];
        NativeCryptoKernels.ed25519PublicKey(privateKey, out);
        return out;
    }

    @Override
    public byte[] ed25519Sign(byte[] privateKey, byte[] publicKey, byte[] message) {
        if (!is(privateKey, 32) || !is(publicKey, 32) || message == null) {
            return null;
        }
        byte[] out = new byte[64];
        NativeCryptoKernels.ed25519Sign(privateKey, publicKey, message, out);
        return out;
    }

    @Override
    public int ed25519Verify(byte[] message, byte[] signature, byte[] publicKey) {
        if (message == null || !is(signature, 64) || !is(publicKey, 32)) {
            return -1;
        }
        // ed25519-donna checks only the top bits of S and accepts a
        // non-canonical key; RFC 8032 rejects both, as the Java verifier
        // does. Refuse S >= L here, and leave the rare key encodings donna
        // reads differently (y >= p, and x = 0 with the sign bit set) to Java.
        if (!Ed25519Math.isCanonicalScalar(signature, 32)) {
            return 0;
        }
        if (!isOrdinaryEdwardsKey(publicKey)) {
            return -1;
        }
        return NativeCryptoKernels.ed25519Verify(message, signature, publicKey);
    }

    @Override
    public byte[] keccak256(byte[] data) {
        if (data == null) {
            return null;
        }
        byte[] out = new byte[32];
        NativeCryptoKernels.keccak256(data, 0, data.length, out);
        return out;
    }

    /// Whether `y` (the low 255 bits, little endian) is below
    /// `p - 1 = 2^255 - 20`. That excludes every non-canonical `y`, and the
    /// two values `y = 1` and `y = -1` aside, every point with `x = 0`;
    /// `y = 1` is checked separately.
    private static boolean isOrdinaryEdwardsKey(byte[] key) {
        if ((key[31] & 0x7f) == 0x7f) {
            for (int i = 30; i > 0; i--) {
                if ((key[i] & 0xff) != 0xff) {
                    return true;
                }
            }
            // y >= 2^255 - 256: only y < 2^255 - 20 is ordinary
            return (key[0] & 0xff) < 0xec;
        }
        if ((key[31] & 0x80) == 0) {
            return true;
        }
        // sign bit set: y = 1 would make x = 0 with a negative sign
        if (key[0] != 1) {
            return true;
        }
        for (int i = 1; i < 31; i++) {
            if (key[i] != 0) {
                return true;
            }
        }
        return (key[31] & 0x7f) != 0;
    }

    private static boolean is(byte[] a, int length) {
        return a != null && a.length == length;
    }

    private static boolean isPublicKeyLength(byte[] key) {
        return key != null && (key.length == PUB_COMPRESSED || key.length == PUB_UNCOMPRESSED);
    }
}
