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

/// ECDSA over the secp256k1 curve, the signature scheme of Bitcoin and
/// Ethereum, written in portable Java so a wallet can sign on the device and
/// a server can verify or sign with the same class.
///
/// Keys are raw bytes: a private key is 32 big-endian bytes in `[1, n-1]`, a
/// public key is the SEC 1 encoding of the point, either compressed (33
/// bytes, `02`/`03` then `x`) or uncompressed (65 bytes, `04` then `x` then
/// `y`). Every method accepts a public key in either form.
///
/// #### Signing an Ethereum transaction
///
/// ```java
/// byte[] priv = Hash.fromHex("4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318");
///
/// // the address is the last 20 bytes of keccak256(x || y)
/// byte[] pub = Secp256k1.publicKey(priv, false);          // 04 || x || y
/// byte[] xy = new byte[64];
/// System.arraycopy(pub, 1, xy, 0, 64);
/// byte[] digest = Hash.keccak256(xy);
/// byte[] address = new byte[20];
/// System.arraycopy(digest, 12, address, 0, 20);           // 2c7536e3605d9c16a7a3d7b1898e529396a65c23
///
/// // sign the keccak256 of the RLP-encoded transaction
/// Secp256k1.EcdsaSignature sig = Secp256k1.sign(Hash.keccak256(rlp), priv);
/// byte[] r = sig.r();
/// byte[] s = sig.s();
/// int v = 27 + sig.recoveryId();                           // or chainId * 2 + 35 + recoveryId
/// ```
///
/// Signatures are deterministic (RFC 6979, HMAC-SHA256) and always low-S, as
/// Bitcoin's standardness rules and Ethereum's EIP-2 require, and they carry
/// the recovery id Ethereum derives the signer's address from. [#verify]
/// rejects a high-S signature the way libsecp256k1 does; [#recoverPublicKey]
/// accepts either, the way the EVM's `ecrecover` does. A signature from
/// another library can be lowered with [EcdsaSignature#normalize].
///
/// #### Constant time
///
/// Everything that touches a private key or a nonce -- key derivation,
/// signing, the tweak used by BIP32 derivation -- runs in constant time: the
/// field and scalar arithmetic uses fixed 16-bit limbs with no data-dependent
/// branches, the curve arithmetic uses complete addition formulas that have
/// no special case for doubling or the point at infinity, and the scalar
/// multiplication is a fixed-window ladder whose table lookup reads every
/// entry. Verification and recovery handle public data only and share the
/// same code. The implementation is independent of libsecp256k1 and
/// depends on nothing but this package and the core JDK subset, so it runs
/// identically on every port and on the backend.
@com.codename1.impl.SharedWithBackend
public final class Secp256k1 {

    /// Length in bytes of a private key.
    public static final int PRIVATE_KEY_LENGTH = 32;

    /// Length in bytes of the message hash a signature covers.
    public static final int HASH_LENGTH = 32;

    /// Length in bytes of a compressed public key (`02`/`03` then `x`).
    public static final int COMPRESSED_PUBLIC_KEY_LENGTH = 33;

    /// Length in bytes of an uncompressed public key (`04` then `x` then `y`).
    public static final int UNCOMPRESSED_PUBLIC_KEY_LENGTH = 65;

    // ----------------------------------------------------------------
    // Representation. A 256-bit integer is int[16] of 16-bit limbs, little
    // endian. The limb is deliberately narrow: a product of two limbs is 32
    // bits and a column of sixteen of them 36, so a whole schoolbook product
    // accumulates in a long with room to spare and the reduction never has
    // to interleave carries with multiplications. That costs more multiplies
    // than libsecp256k1's 26-bit limbs and buys a reduction that is a few
    // short, obviously bounded loops -- see Modulus.reduce.

    private static final int LIMBS = 16;
    private static final int LIMB_MASK = 0xffff;
    private static final int LIMB_BITS = 16;

    private static final Modulus FP = new Modulus(limbs(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F"));
    private static final Modulus FN = new Modulus(limbs(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"));

    private static final int[] ZERO = new int[LIMBS];
    private static final int[] SEVEN = small(7);
    // 3 * b, the constant the complete addition formulas multiply by
    private static final int[] B3 = small(21);
    // (p + 1) / 4 == (p >> 2) + 1 since p == 3 (mod 4): the square root exponent
    private static final int[] SQRT_EXP = addSmall(shiftRight(FP.m, 2), 1);
    private static final int[] N_HALF = shiftRight(FN.m, 1);

    private static final Pt G = Pt.affine(
            limbs("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798"),
            limbs("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8"));
    private static final Pt[] G_TABLE = table(G);

    private Secp256k1() {
    }

    // ----------------------------------------------------------------
    // keys

    /// Tells whether the bytes are a usable private key: exactly 32 bytes
    /// holding a big-endian integer in `[1, n-1]`.
    public static boolean isValidPrivateKey(byte[] privateKey) {
        if (privateKey == null || privateKey.length != PRIVATE_KEY_LENGTH) {
            return false;
        }
        return isValidScalar(toLimbs(privateKey, 0));
    }

    /// Derives the public key of a private key.
    ///
    /// #### Parameters
    ///
    /// - `privateKey`: 32 bytes in `[1, n-1]`
    ///
    /// - `compressed`: `true` for the 33-byte SEC 1 form, `false` for the
    ///   65-byte form Ethereum derives an address from
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the private key is not valid
    public static byte[] publicKey(byte[] privateKey, boolean compressed) {
        int[] d = privateScalar(privateKey);
        Pt q = mul(G_TABLE, d);
        wipe(d);
        return serialize(q, compressed);
    }

    /// Tells whether the bytes encode a point on the curve, in either SEC 1
    /// form.
    public static boolean isValidPublicKey(byte[] publicKey) {
        return parse(publicKey) != null;
    }

    /// Re-encodes a public key in the requested form.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the key is not a point on the curve
    public static byte[] convertPublicKey(byte[] publicKey, boolean compressed) {
        return serialize(parseOrThrow(publicKey), compressed);
    }

    /// Adds a tweak to a private key modulo the curve order: the private
    /// half of a BIP32 child derivation. Constant time.
    ///
    /// #### Parameters
    ///
    /// - `privateKey`: 32 bytes in `[1, n-1]`
    ///
    /// - `tweak`: 32 bytes, less than `n`
    ///
    /// #### Returns
    ///
    /// `(privateKey + tweak) mod n`, 32 bytes
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if either input is out of range or the sum is zero
    public static byte[] privateKeyTweakAdd(byte[] privateKey, byte[] tweak) {
        int[] d = privateScalar(privateKey);
        int[] t = tweakScalar(tweak);
        int[] sum = new int[LIMBS];
        FN.add(d, t, sum);
        wipe(d);
        wipe(t);
        if (isZero(sum)) {
            throw new CryptoException("tweak produces a zero key");
        }
        byte[] out = new byte[PRIVATE_KEY_LENGTH];
        toBytes(sum, out, 0);
        wipe(sum);
        return out;
    }

    /// Adds `tweak * G` to a public key: the public half of a BIP32 child
    /// derivation, matching [#privateKeyTweakAdd] on the private key.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the key is not on the curve, the tweak is not
    ///   less than `n`, or the result is the point at infinity
    public static byte[] publicKeyTweakAdd(byte[] publicKey, byte[] tweak, boolean compressed) {
        Pt q = parseOrThrow(publicKey);
        int[] t = tweakScalar(tweak);
        Pt tg = mul(G_TABLE, t);
        Pt sum = new Pt();
        add(q, tg, sum);
        if (sum.isInfinity()) {
            throw new CryptoException("tweak produces the point at infinity");
        }
        return serialize(sum, compressed);
    }

    /// Adds two public keys as curve points.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if either key is not on the curve or the sum is
    ///   the point at infinity
    public static byte[] publicKeyAdd(byte[] a, byte[] b, boolean compressed) {
        Pt sum = new Pt();
        add(parseOrThrow(a), parseOrThrow(b), sum);
        if (sum.isInfinity()) {
            throw new CryptoException("sum is the point at infinity");
        }
        return serialize(sum, compressed);
    }

    // ----------------------------------------------------------------
    // ECDSA

    /// Signs a 32-byte message hash. The nonce is derived from the key and
    /// the hash as RFC 6979 specifies with HMAC-SHA256, so signing the same
    /// hash twice gives the same signature and no random number generator is
    /// involved. The result is always low-S and carries its recovery id.
    ///
    /// #### Parameters
    ///
    /// - `hash`: 32 bytes, the digest of the message (Keccak-256 of the
    ///   RLP-encoded transaction on Ethereum, double SHA-256 on Bitcoin)
    ///
    /// - `privateKey`: 32 bytes in `[1, n-1]`
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the hash is not 32 bytes or the key is not valid
    public static EcdsaSignature sign(byte[] hash, byte[] privateKey) {
        checkLength(hash, HASH_LENGTH, "hash");
        int[] d = privateScalar(privateKey);
        int[] z = FN.reduce(toLimbs(hash, 0));
        byte[] zBytes = new byte[HASH_LENGTH];
        toBytes(z, zBytes, 0);

        // RFC 6979 section 3.2, steps b to h
        byte[] v = new byte[32];
        for (int i = 0; i < v.length; i++) {
            v[i] = 1;
        }
        byte[] k = new byte[32];
        k = hmac(k, v, (byte) 0, privateKey, zBytes);
        v = hmac(k, v);
        k = hmac(k, v, (byte) 1, privateKey, zBytes);
        v = hmac(k, v);
        try {
            // Each attempt fails with probability about 2^-128; the bound only
            // turns an impossible infinite loop into an exception.
            for (int attempt = 0; attempt < 128; attempt++) {
                v = hmac(k, v);
                int[] nonce = toLimbs(v, 0);
                if (isValidScalar(nonce)) {
                    EcdsaSignature sig = signWithNonce(z, d, nonce);
                    wipe(nonce);
                    if (sig != null) {
                        return sig;
                    }
                }
                k = hmac(k, v, (byte) 0);
                v = hmac(k, v);
            }
        } finally {
            wipe(d);
            wipe(k);
            wipe(v);
        }
        throw new CryptoException("nonce generation failed");
    }

    /// Verifies a signature over a 32-byte hash. A signature whose `s` is in
    /// the upper half of the range is rejected, as libsecp256k1 and the
    /// Bitcoin and Ethereum networks reject it; see
    /// [EcdsaSignature#normalize].
    ///
    /// #### Returns
    ///
    /// `true` if the signature was made over `hash` by the holder of the
    /// private key behind `publicKey`
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the hash is not 32 bytes or the public key is
    ///   not a point on the curve
    public static boolean verify(byte[] hash, EcdsaSignature signature, byte[] publicKey) {
        checkLength(hash, HASH_LENGTH, "hash");
        if (signature == null) {
            throw new CryptoException("signature must not be null");
        }
        Pt q = parseOrThrow(publicKey);
        int[] r = toLimbs(signature.r, 0);
        int[] s = toLimbs(signature.s, 0);
        if (!isValidScalar(r) || !isValidScalar(s) || lessThan(N_HALF, s)) {
            return false;
        }
        int[] z = FN.reduce(toLimbs(hash, 0));
        int[] w = new int[LIMBS];
        FN.inv(s, w);
        int[] u1 = new int[LIMBS];
        int[] u2 = new int[LIMBS];
        FN.mul(z, w, u1);
        FN.mul(r, w, u2);
        Pt sum = new Pt();
        add(mul(G_TABLE, u1), mul(table(q), u2), sum);
        int[] x = new int[LIMBS];
        int[] y = new int[LIMBS];
        if (!toAffine(sum, x, y)) {
            return false;
        }
        return equal(FN.reduce(x), r);
    }

    /// Recovers the public key that made a signature, from the signature's
    /// recovery id: what Ethereum does instead of carrying the sender's key
    /// in a transaction. Unlike [#verify] this accepts a high-S signature,
    /// as the EVM's `ecrecover` does.
    ///
    /// #### Parameters
    ///
    /// - `hash`: the 32 bytes that were signed
    ///
    /// - `signature`: a signature whose recovery id is known (0 to 3)
    ///
    /// - `compressed`: the form to return the key in
    ///
    /// #### Returns
    ///
    /// the public key, or `null` if no key could have produced the signature
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if the hash is not 32 bytes or the signature has
    ///   no recovery id
    public static byte[] recoverPublicKey(byte[] hash, EcdsaSignature signature, boolean compressed) {
        checkLength(hash, HASH_LENGTH, "hash");
        if (signature == null) {
            throw new CryptoException("signature must not be null");
        }
        int recId = signature.recoveryId;
        if (recId < 0) {
            throw new CryptoException("signature carries no recovery id");
        }
        int[] r = toLimbs(signature.r, 0);
        int[] s = toLimbs(signature.s, 0);
        if (!isValidScalar(r) || !isValidScalar(s)) {
            return null;
        }
        int[] x = r;
        if ((recId & 2) != 0) {
            // R.x was reduced by n once; undo it, and give up if that leaves
            // the field (an event with probability about 2^-128)
            x = new int[LIMBS];
            long carry = 0;
            for (int i = 0; i < LIMBS; i++) {
                long sum = (long) r[i] + FN.m[i] + carry;
                x[i] = (int) (sum & LIMB_MASK);
                carry = sum >> LIMB_BITS;
            }
            if (carry != 0 || !lessThan(x, FP.m)) {
                return null;
            }
        }
        int[] y = new int[LIMBS];
        if (!solveY(x, (recId & 1) != 0, y)) {
            return null;
        }
        Pt rp = Pt.affine(x, y);
        int[] z = FN.reduce(toLimbs(hash, 0));
        int[] rInv = new int[LIMBS];
        FN.inv(r, rInv);
        int[] u1 = new int[LIMBS];
        int[] u2 = new int[LIMBS];
        FN.mul(z, rInv, u1);
        FN.sub(ZERO, u1, u1);
        FN.mul(s, rInv, u2);
        Pt q = new Pt();
        add(mul(G_TABLE, u1), mul(table(rp), u2), q);
        if (q.isInfinity()) {
            return null;
        }
        return serialize(q, compressed);
    }

    /// An ECDSA signature: `r` and `s` as 32-byte big-endian integers, and
    /// the recovery id a signer knows and a verifier does not need.
    public static final class EcdsaSignature {
        private final byte[] r;
        private final byte[] s;
        private final int recoveryId;

        /// Creates a signature from its parts.
        ///
        /// #### Parameters
        ///
        /// - `r`: 32 bytes
        ///
        /// - `s`: 32 bytes
        ///
        /// - `recoveryId`: 0 to 3, or -1 when it is not known
        ///
        /// #### Throws
        ///
        /// - `CryptoException`: if a part has the wrong length or the
        ///   recovery id is out of range
        public EcdsaSignature(byte[] r, byte[] s, int recoveryId) {
            checkLength(r, 32, "r");
            checkLength(s, 32, "s");
            if (recoveryId < -1 || recoveryId > 3) {
                throw new CryptoException("recovery id must be 0 to 3, or -1 when unknown");
            }
            this.r = copy(r);
            this.s = copy(s);
            this.recoveryId = recoveryId;
        }

        /// Creates a signature from the 64-byte `r || s` encoding.
        ///
        /// #### Parameters
        ///
        /// - `compact`: 64 bytes, `r` then `s`
        ///
        /// - `recoveryId`: 0 to 3, or -1 when it is not known
        public static EcdsaSignature fromCompact(byte[] compact, int recoveryId) {
            checkLength(compact, 64, "compact signature");
            byte[] r = new byte[32];
            byte[] s = new byte[32];
            System.arraycopy(compact, 0, r, 0, 32);
            System.arraycopy(compact, 32, s, 0, 32);
            return new EcdsaSignature(r, s, recoveryId);
        }

        /// The `r` component, 32 big-endian bytes.
        public byte[] r() {
            return copy(r);
        }

        /// The `s` component, 32 big-endian bytes.
        public byte[] s() {
            return copy(s);
        }

        /// The recovery id, 0 to 3, or -1 when the signature was built
        /// without one. Ethereum's `v` is this plus 27, or plus
        /// `chainId * 2 + 35` under EIP-155.
        public int recoveryId() {
            return recoveryId;
        }

        /// The 64-byte `r || s` encoding.
        public byte[] toCompact() {
            byte[] out = new byte[64];
            System.arraycopy(r, 0, out, 0, 32);
            System.arraycopy(s, 0, out, 32, 32);
            return out;
        }

        /// Tells whether `s` is in the lower half of the range, the form
        /// [Secp256k1#sign] produces and [Secp256k1#verify] accepts.
        public boolean isLowS() {
            return !lessThan(N_HALF, toLimbs(s, 0));
        }

        /// Returns the signature with `s` lowered to the other half of the
        /// range when it is high, flipping the recovery id to match; a
        /// signature that is already low-S is returned as it is. Both forms
        /// are valid for the same key and hash, which is why networks accept
        /// only one of them.
        public EcdsaSignature normalize() {
            if (isLowS()) {
                return this;
            }
            int[] neg = new int[LIMBS];
            FN.sub(ZERO, toLimbs(s, 0), neg);
            byte[] lowS = new byte[32];
            toBytes(neg, lowS, 0);
            return new EcdsaSignature(r, lowS, recoveryId < 0 ? -1 : recoveryId ^ 1);
        }
    }

    // ----------------------------------------------------------------
    // signing internals

    // One RFC 6979 candidate. Returns null when r or s came out zero, which
    // the caller answers with the next candidate.
    private static EcdsaSignature signWithNonce(int[] z, int[] d, int[] nonce) {
        Pt rp = mul(G_TABLE, nonce);
        int[] rx = new int[LIMBS];
        int[] ry = new int[LIMBS];
        if (!toAffine(rp, rx, ry)) {
            return null;
        }
        int[] r = FN.reduce(rx);
        if (isZero(r)) {
            return null;
        }
        int[] kInv = new int[LIMBS];
        FN.inv(nonce, kInv);
        int[] s = new int[LIMBS];
        FN.mul(r, d, s);
        FN.add(s, z, s);
        FN.mul(s, kInv, s);
        wipe(kInv);
        if (isZero(s)) {
            return null;
        }
        int recId = (ry[0] & 1) | (lessThan(rx, FN.m) ? 0 : 2);
        if (lessThan(N_HALF, s)) {
            FN.sub(ZERO, s, s);
            recId ^= 1;
        }
        byte[] rBytes = new byte[32];
        byte[] sBytes = new byte[32];
        toBytes(r, rBytes, 0);
        toBytes(s, sBytes, 0);
        return new EcdsaSignature(rBytes, sBytes, recId);
    }

    private static byte[] hmac(byte[] key, byte[] v) {
        return Hmac.sha256(key, v);
    }

    private static byte[] hmac(byte[] key, byte[] v, byte separator) {
        Hmac h = Hmac.create(Hash.SHA256, key);
        h.update(v);
        h.update(new byte[] {separator});
        return h.doFinal();
    }

    private static byte[] hmac(byte[] key, byte[] v, byte separator, byte[] x, byte[] h1) {
        Hmac h = Hmac.create(Hash.SHA256, key);
        h.update(v);
        h.update(new byte[] {separator});
        h.update(x);
        h.update(h1);
        return h.doFinal();
    }

    // ----------------------------------------------------------------
    // encoding

    private static int[] privateScalar(byte[] privateKey) {
        checkLength(privateKey, PRIVATE_KEY_LENGTH, "private key");
        int[] d = toLimbs(privateKey, 0);
        if (!isValidScalar(d)) {
            wipe(d);
            throw new CryptoException("private key must be in [1, n-1]");
        }
        return d;
    }

    private static int[] tweakScalar(byte[] tweak) {
        checkLength(tweak, PRIVATE_KEY_LENGTH, "tweak");
        int[] t = toLimbs(tweak, 0);
        if (!lessThan(t, FN.m)) {
            wipe(t);
            throw new CryptoException("tweak must be less than n");
        }
        return t;
    }

    private static Pt parseOrThrow(byte[] publicKey) {
        Pt p = parse(publicKey);
        if (p == null) {
            throw new CryptoException("not a secp256k1 public key");
        }
        return p;
    }

    // null when the bytes are not a point on the curve in either SEC 1 form
    private static Pt parse(byte[] key) {
        if (key == null) {
            return null;
        }
        if (key.length == COMPRESSED_PUBLIC_KEY_LENGTH && (key[0] == 2 || key[0] == 3)) {
            int[] x = toLimbs(key, 1);
            if (!lessThan(x, FP.m)) {
                return null;
            }
            int[] y = new int[LIMBS];
            if (!solveY(x, key[0] == 3, y)) {
                return null;
            }
            return Pt.affine(x, y);
        }
        if (key.length == UNCOMPRESSED_PUBLIC_KEY_LENGTH && key[0] == 4) {
            int[] x = toLimbs(key, 1);
            int[] y = toLimbs(key, 33);
            if (!lessThan(x, FP.m) || !lessThan(y, FP.m)) {
                return null;
            }
            int[] rhs = new int[LIMBS];
            curveRhs(x, rhs);
            int[] lhs = new int[LIMBS];
            FP.mul(y, y, lhs);
            if (!equal(lhs, rhs)) {
                return null;
            }
            return Pt.affine(x, y);
        }
        return null;
    }

    private static byte[] serialize(Pt p, boolean compressed) {
        int[] x = new int[LIMBS];
        int[] y = new int[LIMBS];
        if (!toAffine(p, x, y)) {
            throw new CryptoException("point at infinity has no encoding");
        }
        byte[] out;
        if (compressed) {
            out = new byte[COMPRESSED_PUBLIC_KEY_LENGTH];
            out[0] = (byte) (2 + (y[0] & 1));
        } else {
            out = new byte[UNCOMPRESSED_PUBLIC_KEY_LENGTH];
            out[0] = 4;
            toBytes(y, out, 33);
        }
        toBytes(x, out, 1);
        return out;
    }

    // x^3 + 7
    private static void curveRhs(int[] x, int[] out) {
        int[] t = new int[LIMBS];
        FP.mul(x, x, t);
        FP.mul(t, x, t);
        FP.add(t, SEVEN, out);
    }

    // y with the requested parity such that y^2 == x^3 + 7; false when x is
    // not the abscissa of a curve point
    private static boolean solveY(int[] x, boolean odd, int[] yOut) {
        int[] rhs = new int[LIMBS];
        curveRhs(x, rhs);
        int[] y = new int[LIMBS];
        FP.pow(rhs, SQRT_EXP, y);
        int[] check = new int[LIMBS];
        FP.mul(y, y, check);
        if (!equal(check, rhs)) {
            return false;
        }
        if (((y[0] & 1) == 1) != odd) {
            FP.sub(ZERO, y, y);
        }
        System.arraycopy(y, 0, yOut, 0, LIMBS);
        return true;
    }

    private static void checkLength(byte[] data, int length, String what) {
        if (data == null) {
            throw new CryptoException(what + " must not be null");
        }
        if (data.length != length) {
            throw new CryptoException(what + " must be " + length + " bytes, not " + data.length);
        }
    }

    private static byte[] copy(byte[] a) {
        byte[] out = new byte[a.length];
        System.arraycopy(a, 0, out, 0, a.length);
        return out;
    }

    private static void wipe(int[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = 0;
        }
    }

    private static void wipe(byte[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = 0;
        }
    }

    // ----------------------------------------------------------------
    // 256-bit integers as limbs

    private static int[] limbs(String hex) {
        return toLimbs(Hash.fromHex(hex), 0);
    }

    private static int[] small(int v) {
        int[] out = new int[LIMBS];
        out[0] = v & LIMB_MASK;
        out[1] = v >>> LIMB_BITS;
        return out;
    }

    // 32 big-endian bytes to limbs
    private static int[] toLimbs(byte[] in, int off) {
        int[] out = new int[LIMBS];
        for (int i = 0; i < LIMBS; i++) {
            int o = off + 30 - 2 * i;
            out[i] = ((in[o] & 0xff) << 8) | (in[o + 1] & 0xff);
        }
        return out;
    }

    private static void toBytes(int[] a, byte[] out, int off) {
        for (int i = 0; i < LIMBS; i++) {
            int o = off + 30 - 2 * i;
            out[o] = (byte) (a[i] >>> 8);
            out[o + 1] = (byte) a[i];
        }
    }

    private static int[] addSmall(int[] a, int c) {
        int[] out = new int[LIMBS];
        int carry = c;
        for (int i = 0; i < LIMBS; i++) {
            int v = a[i] + carry;
            out[i] = v & LIMB_MASK;
            carry = v >>> LIMB_BITS;
        }
        return out;
    }

    private static int[] subSmall(int[] a, int c) {
        int[] out = new int[LIMBS];
        int borrow = c;
        for (int i = 0; i < LIMBS; i++) {
            int v = a[i] - borrow;
            out[i] = v & LIMB_MASK;
            borrow = (v >> 31) & 1;
        }
        return out;
    }

    // a >> bits, for 0 < bits < 16
    private static int[] shiftRight(int[] a, int bits) {
        int[] out = new int[LIMBS];
        for (int i = 0; i < LIMBS - 1; i++) {
            out[i] = ((a[i] >>> bits) | (a[i + 1] << (LIMB_BITS - bits))) & LIMB_MASK;
        }
        out[LIMBS - 1] = a[LIMBS - 1] >>> bits;
        return out;
    }

    // a < b, computed without a data-dependent branch
    private static boolean lessThan(int[] a, int[] b) {
        long borrow = 0;
        for (int i = 0; i < LIMBS; i++) {
            long d = (long) a[i] - b[i] - borrow;
            borrow = (d >> 63) & 1;
        }
        return borrow != 0;
    }

    private static boolean isZero(int[] a) {
        int acc = 0;
        for (int i = 0; i < LIMBS; i++) {
            acc |= a[i];
        }
        return acc == 0;
    }

    private static boolean equal(int[] a, int[] b) {
        int acc = 0;
        for (int i = 0; i < LIMBS; i++) {
            acc |= a[i] ^ b[i];
        }
        return acc == 0;
    }

    // 0 < v < n
    private static boolean isValidScalar(int[] v) {
        return !isZero(v) && lessThan(v, FN.m);
    }

    // ----------------------------------------------------------------
    // arithmetic modulo a 256-bit prime of the form 2^256 - k with k < 2^129
    // (both secp256k1 primes are): the field p and the group order n

    private static final class Modulus {
        // the modulus
        final int[] m;
        // 2^256 - m, little endian, klen limbs long (3 for p, 9 for n)
        private final int[] k = new int[LIMBS];
        private final int klen;
        // m - 2, the exponent that inverts by Fermat
        private final int[] minusTwo;

        Modulus(int[] m) {
            this.m = m;
            int carry = 1;
            for (int i = 0; i < LIMBS; i++) {
                int v = (~m[i] & LIMB_MASK) + carry;
                k[i] = v & LIMB_MASK;
                carry = v >>> LIMB_BITS;
            }
            int len = 0;
            for (int i = 0; i < LIMBS; i++) {
                if (k[i] != 0) {
                    len = i + 1;
                }
            }
            klen = len;
            minusTwo = subSmall(m, 2);
        }

        // out = a * b mod m. out may be a or b.
        void mul(int[] a, int[] b, int[] out) {
            long[] c = new long[2 * LIMBS];
            for (int i = 0; i < LIMBS; i++) {
                long ai = a[i];
                for (int j = 0; j < LIMBS; j++) {
                    c[i + j] += ai * b[j];
                }
            }
            reduce(c, out);
        }

        // out = a + b mod m, for a, b < m. out may be a or b.
        void add(int[] a, int[] b, int[] out) {
            long[] c = new long[2 * LIMBS];
            long carry = 0;
            for (int i = 0; i < LIMBS; i++) {
                long s = (long) a[i] + b[i] + carry;
                c[i] = s & LIMB_MASK;
                carry = s >> LIMB_BITS;
            }
            c[LIMBS] = carry;
            conditionalSubtract(c, out);
        }

        // out = a - b mod m, for a, b < m. out may be a or b.
        void sub(int[] a, int[] b, int[] out) {
            int[] d = new int[LIMBS];
            long borrow = 0;
            for (int i = 0; i < LIMBS; i++) {
                long v = (long) a[i] - b[i] - borrow;
                d[i] = (int) (v & LIMB_MASK);
                borrow = (v >> 63) & 1;
            }
            // a < b: the difference wrapped past zero, so add m back (the
            // carry out of the top limb undoes the wrap)
            int mask = (int) -borrow;
            long carry = 0;
            for (int i = 0; i < LIMBS; i++) {
                long s = (long) d[i] + (m[i] & mask) + carry;
                out[i] = (int) (s & LIMB_MASK);
                carry = s >> LIMB_BITS;
            }
        }

        // out = a^(m-2) == 1/a mod m (0 maps to 0). out may be a.
        void inv(int[] a, int[] out) {
            pow(a, minusTwo, out);
        }

        // out = base^exp mod m by square-and-multiply over the bits of exp.
        // Every exponent used here is a public constant, so the branch on
        // its bits leaks nothing about base. out may be base.
        void pow(int[] base, int[] exp, int[] out) {
            int[] b = new int[LIMBS];
            System.arraycopy(base, 0, b, 0, LIMBS);
            int[] r = new int[LIMBS];
            r[0] = 1;
            for (int bit = 255; bit >= 0; bit--) {
                mul(r, r, r);
                if (((exp[bit >> 4] >> (bit & 15)) & 1) != 0) {
                    mul(r, b, r);
                }
            }
            System.arraycopy(r, 0, out, 0, LIMBS);
        }

        // v mod m for any v < 2^256 (one conditional subtraction, since
        // m > 2^255)
        int[] reduce(int[] v) {
            long[] c = new long[2 * LIMBS];
            for (int i = 0; i < LIMBS; i++) {
                c[i] = v[i];
            }
            int[] out = new int[LIMBS];
            conditionalSubtract(c, out);
            return out;
        }

        // Reduces a 32-column product (each column below 2^36) to out < m.
        //
        // 2^256 == k (mod m), so a column j >= 16 is worth c[j] * k at column
        // j - 16. Each fold pass below moves whole columns down that way and
        // each carry pass renormalises every column to 16 bits; the passes
        // are ordered so that no column ever exceeds about 2^56:
        //
        //  1. columns 24..31 fold into 8..23 (k has at most 9 limbs, so
        //     nothing lands at or above 24 again); the value is now < 2^386
        //  2. columns 16..24 fold into 0..16; the value is now < 2^260
        //  3. column 16 (at most 4 bits) folds; the value is now < 2^256 + 2^134
        //  4. column 16 (at most 1 bit) folds; the value is now < 2^256
        //  5. one conditional subtraction of m
        //
        // Every loop has a fixed trip count, so this is constant time.
        private void reduce(long[] c, int[] out) {
            carry(c);
            fold(c, 2 * LIMBS - 1, 24);
            carry(c);
            fold(c, 24, LIMBS);
            carry(c);
            fold(c, LIMBS, LIMBS);
            carry(c);
            fold(c, LIMBS, LIMBS);
            carry(c);
            conditionalSubtract(c, out);
        }

        // folds columns hi down to lo, each into the klen columns 16 below it
        private void fold(long[] c, int hi, int lo) {
            for (int j = hi; j >= lo; j--) {
                long v = c[j];
                c[j] = 0;
                int base = j - LIMBS;
                for (int i = 0; i < klen; i++) {
                    c[base + i] += v * k[i];
                }
            }
        }

        private static void carry(long[] c) {
            for (int i = 0; i < 2 * LIMBS - 1; i++) {
                c[i + 1] += c[i] >> LIMB_BITS;
                c[i] &= LIMB_MASK;
            }
        }

        // out = c mod m for a normalised c < 2m whose bit 256 is c[16]
        private void conditionalSubtract(long[] c, int[] out) {
            int[] t = new int[LIMBS];
            long borrow = 0;
            for (int i = 0; i < LIMBS; i++) {
                long d = c[i] - m[i] - borrow;
                t[i] = (int) (d & LIMB_MASK);
                borrow = (d >> 63) & 1;
            }
            // c >= m when the bit above the top limb is set or no borrow came out
            int mask = (int) -(c[LIMBS] | (borrow ^ 1));
            for (int i = 0; i < LIMBS; i++) {
                out[i] = (t[i] & mask) | ((int) c[i] & ~mask);
            }
        }
    }

    // ----------------------------------------------------------------
    // the curve, in homogeneous projective coordinates (X : Y : Z) with
    // x = X / Z and y = Y / Z; the point at infinity is (0 : 1 : 0)

    private static final class Pt {
        final int[] x = new int[LIMBS];
        final int[] y = new int[LIMBS];
        final int[] z = new int[LIMBS];

        static Pt infinity() {
            Pt p = new Pt();
            p.y[0] = 1;
            return p;
        }

        static Pt affine(int[] x, int[] y) {
            Pt p = new Pt();
            System.arraycopy(x, 0, p.x, 0, LIMBS);
            System.arraycopy(y, 0, p.y, 0, LIMBS);
            p.z[0] = 1;
            return p;
        }

        void set(Pt o) {
            System.arraycopy(o.x, 0, x, 0, LIMBS);
            System.arraycopy(o.y, 0, y, 0, LIMBS);
            System.arraycopy(o.z, 0, z, 0, LIMBS);
        }

        boolean isInfinity() {
            return isZero(z);
        }
    }

    // out = p + q by the complete addition law for y^2 = x^3 + b (Renes,
    // Costello and Batina, 2016, with a = 0 and b3 = 3b):
    //
    //   X3 = (X1 Y2 + X2 Y1)(Y1 Y2 - b3 Z1 Z2) - b3 (Y1 Z2 + Y2 Z1)(X1 Z2 + X2 Z1)
    //   Y3 = (Y1 Y2 + b3 Z1 Z2)(Y1 Y2 - b3 Z1 Z2) + 3 b3 X1 X2 (X1 Z2 + X2 Z1)
    //   Z3 = (Y1 Z2 + Y2 Z1)(Y1 Y2 + b3 Z1 Z2) + 3 X1 X2 (X1 Y2 + X2 Y1)
    //
    // It is correct for every pair of inputs -- p == q, p == -q, either one
    // the point at infinity -- so it doubles as the doubling and there is no
    // branch for the ladder to leak through. A dedicated doubling formula
    // would save about a third of the multiplications; this keeps one
    // formula. out may be p or q.
    private static void add(Pt p, Pt q, Pt out) {
        int[] t0 = new int[LIMBS];
        int[] t1 = new int[LIMBS];
        int[] t2 = new int[LIMBS];
        int[] t3 = new int[LIMBS];
        int[] t4 = new int[LIMBS];
        int[] t5 = new int[LIMBS];
        int[] t6 = new int[LIMBS];
        int[] t7 = new int[LIMBS];
        FP.mul(p.x, q.x, t0);         // X1 X2
        FP.mul(p.y, q.y, t1);         // Y1 Y2
        FP.mul(p.z, q.z, t2);         // Z1 Z2
        FP.add(p.x, p.y, t3);
        FP.add(q.x, q.y, t4);
        FP.mul(t3, t4, t3);
        FP.add(t0, t1, t4);
        FP.sub(t3, t4, t3);           // X1 Y2 + X2 Y1
        FP.add(p.y, p.z, t4);
        FP.add(q.y, q.z, t5);
        FP.mul(t4, t5, t4);
        FP.add(t1, t2, t5);
        FP.sub(t4, t5, t4);           // Y1 Z2 + Y2 Z1
        FP.add(p.x, p.z, t5);
        FP.add(q.x, q.z, t6);
        FP.mul(t5, t6, t5);
        FP.add(t0, t2, t6);
        FP.sub(t5, t6, t5);           // X1 Z2 + X2 Z1
        FP.add(t0, t0, t6);
        FP.add(t6, t0, t0);           // 3 X1 X2
        FP.mul(B3, t2, t2);           // b3 Z1 Z2
        FP.add(t1, t2, t6);           // Y1 Y2 + b3 Z1 Z2
        FP.sub(t1, t2, t1);           // Y1 Y2 - b3 Z1 Z2
        FP.mul(B3, t5, t5);           // b3 (X1 Z2 + X2 Z1)
        FP.mul(t3, t1, t2);
        FP.mul(t4, t5, t7);
        FP.sub(t2, t7, out.x);
        FP.mul(t6, t1, t1);
        FP.mul(t0, t5, t7);
        FP.add(t1, t7, out.y);
        FP.mul(t4, t6, t6);
        FP.mul(t0, t3, t7);
        FP.add(t6, t7, out.z);
    }

    // the multiples 0..15 of p, for mul
    private static Pt[] table(Pt p) {
        Pt[] t = new Pt[16];
        t[0] = Pt.infinity();
        t[1] = new Pt();
        t[1].set(p);
        for (int i = 2; i < t.length; i++) {
            t[i] = new Pt();
            add(t[i - 1], p, t[i]);
        }
        return t;
    }

    // scalar * p, where table is table(p) and 0 <= scalar < 2^256, by a
    // fixed window of four bits: 64 windows, each four doublings and one
    // addition of the table entry the window selects. The entry is read with
    // a constant-time scan of the whole table, so neither the sequence of
    // operations nor the memory addresses touched depend on the scalar.
    private static Pt mul(Pt[] table, int[] scalar) {
        Pt r = Pt.infinity();
        Pt t = new Pt();
        for (int w = 63; w >= 0; w--) {
            add(r, r, r);
            add(r, r, r);
            add(r, r, r);
            add(r, r, r);
            int digit = (scalar[w >> 2] >> ((w & 3) << 2)) & 0xf;
            select(table, digit, t);
            add(r, t, r);
        }
        return r;
    }

    // out = table[index] without indexing by index
    private static void select(Pt[] table, int index, Pt out) {
        wipe(out.x);
        wipe(out.y);
        wipe(out.z);
        for (int e = 0; e < table.length; e++) {
            // -1 when e == index, else 0
            int mask = ((e ^ index) - 1) >> 31;
            Pt entry = table[e];
            for (int i = 0; i < LIMBS; i++) {
                out.x[i] |= entry.x[i] & mask;
                out.y[i] |= entry.y[i] & mask;
                out.z[i] |= entry.z[i] & mask;
            }
        }
    }

    // false for the point at infinity
    private static boolean toAffine(Pt p, int[] xOut, int[] yOut) {
        if (p.isInfinity()) {
            return false;
        }
        int[] zInv = new int[LIMBS];
        FP.inv(p.z, zInv);
        FP.mul(p.x, zInv, xOut);
        FP.mul(p.y, zInv, yOut);
        return true;
    }
}
