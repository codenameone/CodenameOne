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
/// byte[] rlp = Hash.fromHex("02c0");                       // your encoded transaction
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
/// field arithmetic has no data-dependent branches, `k G` is a signed-digit
/// comb whose every digit is odd (so no table entry is the point at
/// infinity), each comb step uses the complete addition formula of Renes,
/// Costello and Batina and reads every entry of its table, and inversion is
/// the branch-free safegcd of Bernstein and Yang. Verification and recovery
/// handle public data only and take the fast variable-time path: a
/// Strauss-Shamir double multiplication over wNAF expansions, with both
/// scalars split in half by the curve's endomorphism. The arithmetic lives in
/// `Secp256k1Math`; it depends on nothing but this package and the core JDK
/// subset, so it runs identically on every port and on the backend.
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
    // Scalars modulo n are int[16] of 16-bit limbs, little endian: a product
    // of two limbs is 32 bits and a column of sixteen of them 36, so a whole
    // schoolbook product accumulates in a long and the reduction is a few
    // short, obviously bounded loops (see Modulus.reduce). A signature needs
    // only a handful of scalar products, so this is not where the time goes;
    // the field and the curve are in Secp256k1Math.

    private static final int LIMBS = 16;
    private static final int LIMB_MASK = 0xffff;
    private static final int LIMB_BITS = 16;

    private static final Modulus FN = new Modulus(limbs(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"));

    private static final int[] ZERO = new int[LIMBS];
    private static final int[] N_HALF = shiftRight(FN.m, 1);

    // p, for the recovery id's "R.x was reduced by n" case
    private static final int[] P_LIMBS = limbs(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F");

    // the GLV decomposition k = k1 + k2 lambda (mod n) with |k1|, |k2| < 2^128:
    // c1 = round(k g1 / 2^384), c2 = round(k g2 / 2^384),
    // k2 = c1 (-b1) + c2 (-b2), k1 = k - k2 lambda
    private static final int[] GLV_G1 = limbs(
            "3086D221A7D46BCDE86C90E49284EB153DAA8A1471E8CA7FE893209A45DBB031");
    private static final int[] GLV_G2 = limbs(
            "E4437ED6010E88286F547FA90ABFE4C4221208AC9DF506C61571B4AE8AC47F71");
    private static final int[] GLV_MINUS_B1 = limbs(
            "00000000000000000000000000000000E4437ED6010E88286F547FA90ABFE4C3");
    private static final int[] GLV_MINUS_B2 = limbs(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE8A280AC50774346DD765CDA83DB1562C");
    private static final int[] GLV_MINUS_LAMBDA = limbs(
            "AC9C52B33FA3CF1F5AD9E3FD77ED9BA4A880B9FC8EC739C2E0CFC810B51283CF");

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
        byte[] fast = CryptoEngine.get().secpPublicKey(privateKey, compressed);
        if (fast != null) {
            return fast;
        }
        int[] d = privateScalar(privateKey);
        int[][] q = mulGen(d);
        wipe(d);
        return serialize(q[0], q[1], compressed);
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
        int[][] q = parseOrThrow(publicKey);
        return serialize(q[0], q[1], compressed);
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
        byte[] fast = CryptoEngine.get().secpPrivateKeyTweakAdd(privateKey, tweak);
        if (fast != null) {
            return fast;
        }
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
        byte[] fast = CryptoEngine.get().secpPublicKeyTweakAdd(publicKey, tweak, compressed);
        if (fast != null) {
            return fast;
        }
        int[][] q = parseOrThrow(publicKey);
        int[] t = tweakScalar(tweak);
        if (isZero(t)) {
            return serialize(q[0], q[1], compressed);
        }
        int[][] tg = mulGen(t);
        int[][] sum = addPoints(q, tg);
        if (sum == null) {
            throw new CryptoException("tweak produces the point at infinity");
        }
        return serialize(sum[0], sum[1], compressed);
    }

    /// Adds two public keys as curve points.
    ///
    /// #### Throws
    ///
    /// - `CryptoException`: if either key is not on the curve or the sum is
    ///   the point at infinity
    public static byte[] publicKeyAdd(byte[] a, byte[] b, boolean compressed) {
        int[][] sum = addPoints(parseOrThrow(a), parseOrThrow(b));
        if (sum == null) {
            throw new CryptoException("sum is the point at infinity");
        }
        return serialize(sum[0], sum[1], compressed);
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
        byte[] fast = CryptoEngine.get().secpSign(hash, privateKey);
        if (fast != null) {
            return EcdsaSignature.fromCompact(slice(fast, 0, 64), fast[64]);
        }
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
        int fast = CryptoEngine.get().secpVerify(hash, signature.toCompact(), publicKey);
        if (fast >= 0) {
            return fast == 1;
        }
        int[][] q = parseOrThrow(publicKey);
        int[] r = toLimbs(signature.r, 0);
        int[] s = toLimbs(signature.s, 0);
        if (!isValidScalar(r) || !isValidScalar(s) || lessThan(N_HALF, s)) {
            return false;
        }
        int[] z = FN.reduce(toLimbs(hash, 0));
        int[] w = scalarInverse(s);
        int[] u1 = new int[LIMBS];
        int[] u2 = new int[LIMBS];
        FN.mul(z, w, u1);
        FN.mul(r, w, u2);
        Secp256k1Math.Gej sum = new Secp256k1Math.Gej();
        doubleMul(sum, q, u2, u1);
        if (sum.inf) {
            return false;
        }
        // R.x == r without leaving Jacobian coordinates: x = X / Z^2, so
        // compare r Z^2 with X, and (r + n) Z^2 when r + n is still below p
        int[] z2 = new int[10];
        Secp256k1Math.sqr(z2, sum.z);
        int[] xr = fieldOf(r);
        int[] t = new int[10];
        Secp256k1Math.mul(t, xr, z2);
        if (Secp256k1Math.equal(t, sum.x)) {
            return true;
        }
        int[] rn = new int[LIMBS];
        long carry = 0;
        for (int i = 0; i < LIMBS; i++) {
            long v = (long) r[i] + FN.m[i] + carry;
            rn[i] = (int) (v & LIMB_MASK);
            carry = v >> LIMB_BITS;
        }
        if (carry != 0 || !lessThan(rn, P_LIMBS)) {
            return false;
        }
        Secp256k1Math.mul(t, fieldOf(rn), z2);
        return Secp256k1Math.equal(t, sum.x);
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
        byte[] fast = CryptoEngine.get().secpRecover(hash, signature.toCompact(), recId, compressed);
        if (fast != null) {
            return fast;
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
            if (carry != 0 || !lessThan(x, P_LIMBS)) {
                return null;
            }
        }
        int[] fx = fieldOf(x);
        int[] fy = Secp256k1Math.liftY(fx, (recId & 1) != 0);
        if (fy == null) {
            return null;
        }
        int[] z = FN.reduce(toLimbs(hash, 0));
        int[] rInv = scalarInverse(r);
        int[] u1 = new int[LIMBS];
        int[] u2 = new int[LIMBS];
        FN.mul(z, rInv, u1);
        FN.sub(ZERO, u1, u1);
        FN.mul(s, rInv, u2);
        Secp256k1Math.Gej q = new Secp256k1Math.Gej();
        doubleMul(q, new int[][] {fx, fy}, u2, u1);
        int[] qx = new int[10];
        int[] qy = new int[10];
        if (!Secp256k1Math.toAffine(new Secp256k1Math.Ctx(), q, qx, qy)) {
            return null;
        }
        return serialize(qx, qy, compressed);
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
        int[][] rp = mulGen(nonce);
        byte[] rxBytes = new byte[32];
        Secp256k1Math.toBytes(rp[0], rxBytes, 0);
        int[] rx = toLimbs(rxBytes, 0);
        int[] r = FN.reduce(rx);
        if (isZero(r)) {
            return null;
        }
        int[] kInv = scalarInverse(nonce);
        int[] s = new int[LIMBS];
        FN.mul(r, d, s);
        FN.add(s, z, s);
        FN.mul(s, kInv, s);
        wipe(kInv);
        if (isZero(s)) {
            return null;
        }
        int recId = (rp[1][0] & 1) | (lessThan(rx, FN.m) ? 0 : 2);
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

    private static int[][] parseOrThrow(byte[] publicKey) {
        int[][] p = parse(publicKey);
        if (p == null) {
            throw new CryptoException("not a secp256k1 public key");
        }
        return p;
    }

    // {x, y} normalized, or null when the bytes are not a point on the curve
    // in either SEC 1 form
    private static int[][] parse(byte[] key) {
        if (key == null) {
            return null;
        }
        if (key.length == COMPRESSED_PUBLIC_KEY_LENGTH && (key[0] == 2 || key[0] == 3)) {
            if (!Secp256k1Math.isCanonical(key, 1)) {
                return null;
            }
            int[] x = new int[10];
            Secp256k1Math.fromBytes(x, key, 1);
            int[] y = Secp256k1Math.liftY(x, key[0] == 3);
            if (y == null) {
                return null;
            }
            return new int[][] {x, y};
        }
        if (key.length == UNCOMPRESSED_PUBLIC_KEY_LENGTH && key[0] == 4) {
            if (!Secp256k1Math.isCanonical(key, 1) || !Secp256k1Math.isCanonical(key, 33)) {
                return null;
            }
            int[] x = new int[10];
            int[] y = new int[10];
            Secp256k1Math.fromBytes(x, key, 1);
            Secp256k1Math.fromBytes(y, key, 33);
            if (!Secp256k1Math.onCurve(x, y)) {
                return null;
            }
            return new int[][] {x, y};
        }
        return null;
    }

    private static byte[] serialize(int[] x, int[] y, boolean compressed) {
        byte[] out;
        if (compressed) {
            out = new byte[COMPRESSED_PUBLIC_KEY_LENGTH];
            out[0] = (byte) (2 + (y[0] & 1));
        } else {
            out = new byte[UNCOMPRESSED_PUBLIC_KEY_LENGTH];
            out[0] = 4;
            Secp256k1Math.toBytes(y, out, 33);
        }
        Secp256k1Math.toBytes(x, out, 1);
        return out;
    }

    // ----------------------------------------------------------------
    // BIP-340 Schnorr signatures, reached through Schnorr

    static byte[] xOnlyPublicKey(byte[] privateKey) {
        byte[] fast = CryptoEngine.get().schnorrPublicKey(privateKey);
        if (fast != null) {
            return fast;
        }
        int[] d = privateScalar(privateKey);
        int[][] p = mulGen(d);
        wipe(d);
        byte[] out = new byte[32];
        Secp256k1Math.toBytes(p[0], out, 0);
        return out;
    }

    static byte[] schnorrSign(byte[] message, byte[] privateKey, byte[] auxRand) {
        if (message == null) {
            throw new CryptoException("message must not be null");
        }
        if (auxRand != null) {
            checkLength(auxRand, 32, "auxiliary randomness");
        }
        byte[] fast = CryptoEngine.get().schnorrSign(message, privateKey, auxRand);
        if (fast != null) {
            return fast;
        }
        int[] d = privateScalar(privateKey);
        int[][] p = mulGen(d);
        if ((p[1][0] & 1) != 0) {
            FN.sub(ZERO, d, d);
        }
        byte[] px = new byte[32];
        Secp256k1Math.toBytes(p[0], px, 0);
        byte[] dBytes = new byte[32];
        toBytes(d, dBytes, 0);
        byte[] t = taggedHash(TAG_AUX, auxRand == null ? new byte[32] : auxRand, null, null);
        for (int i = 0; i < 32; i++) {
            t[i] ^= dBytes[i];
        }
        byte[] rand = taggedHash(TAG_NONCE, t, px, message);
        wipe(t);
        wipe(dBytes);
        int[] k = FN.reduce(toLimbs(rand, 0));
        wipe(rand);
        try {
            if (isZero(k)) {
                throw new CryptoException("nonce generation failed");
            }
            int[][] r = mulGen(k);
            if ((r[1][0] & 1) != 0) {
                FN.sub(ZERO, k, k);
            }
            byte[] sig = new byte[64];
            Secp256k1Math.toBytes(r[0], sig, 0);
            int[] e = FN.reduce(toLimbs(taggedHash(TAG_CHALLENGE, slice(sig, 0, 32), px, message), 0));
            int[] s = new int[LIMBS];
            FN.mul(e, d, s);
            FN.add(s, k, s);
            toBytes(s, sig, 32);
            wipe(s);
            return sig;
        } finally {
            wipe(k);
            wipe(d);
        }
    }

    static boolean schnorrVerify(byte[] message, byte[] signature, byte[] xOnlyKey) {
        if (message == null) {
            throw new CryptoException("message must not be null");
        }
        checkLength(xOnlyKey, 32, "x-only public key");
        if (signature == null || signature.length != 64) {
            return false;
        }
        int fast = CryptoEngine.get().schnorrVerify(message, signature, xOnlyKey);
        if (fast >= 0) {
            return fast == 1;
        }
        if (!Secp256k1Math.isCanonical(xOnlyKey, 0) || !Secp256k1Math.isCanonical(signature, 0)) {
            return false;
        }
        int[] px = new int[10];
        Secp256k1Math.fromBytes(px, xOnlyKey, 0);
        int[] py = Secp256k1Math.liftY(px, false);
        if (py == null) {
            return false;
        }
        int[] s = toLimbs(signature, 32);
        if (!lessThan(s, FN.m)) {
            return false;
        }
        int[] e = FN.reduce(toLimbs(taggedHash(TAG_CHALLENGE, slice(signature, 0, 32), xOnlyKey, message), 0));
        FN.sub(ZERO, e, e);
        Secp256k1Math.Gej sum = new Secp256k1Math.Gej();
        doubleMul(sum, new int[][] {px, py}, e, s);
        int[] rx = new int[10];
        int[] ry = new int[10];
        if (!Secp256k1Math.toAffine(new Secp256k1Math.Ctx(), sum, rx, ry) || (ry[0] & 1) != 0) {
            return false;
        }
        int[] r = new int[10];
        Secp256k1Math.fromBytes(r, signature, 0);
        return Secp256k1Math.equal(r, rx);
    }

    private static final byte[] TAG_AUX = tag("BIP0340/aux");
    private static final byte[] TAG_NONCE = tag("BIP0340/nonce");
    private static final byte[] TAG_CHALLENGE = tag("BIP0340/challenge");

    private static byte[] tag(String name) {
        byte[] n = new byte[name.length()];
        for (int i = 0; i < n.length; i++) {
            n[i] = (byte) name.charAt(i);
        }
        return Hash.sha256(n);
    }

    // SHA256(SHA256(tag) || SHA256(tag) || a || b || c), BIP-340's tagged hash
    private static byte[] taggedHash(byte[] tagHash, byte[] a, byte[] b, byte[] c) {
        Hash h = Hash.create(Hash.SHA256);
        h.update(tagHash);
        h.update(tagHash);
        h.update(a);
        if (b != null) {
            h.update(b);
        }
        if (c != null) {
            h.update(c);
        }
        return h.digest();
    }

    private static byte[] slice(byte[] a, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(a, off, out, 0, len);
        return out;
    }

    // ----------------------------------------------------------------
    // bridges to Secp256k1Math

    // k G for 0 < k < n, as normalized {x, y}; constant time
    private static int[][] mulGen(int[] k) {
        int[] w = words(k);
        int[] x = new int[10];
        int[] y = new int[10];
        Secp256k1Math.mulGen(w, x, y);
        wipe(w);
        return new int[][] {x, y};
    }

    // a + b for public points, or null for the point at infinity
    private static int[][] addPoints(int[][] a, int[][] b) {
        Secp256k1Math.Ctx c = new Secp256k1Math.Ctx();
        Secp256k1Math.Gej r = Secp256k1Math.Gej.affine(a[0], a[1]);
        Secp256k1Math.addGe(c, r, r, b[0], b[1]);
        int[] x = new int[10];
        int[] y = new int[10];
        if (!Secp256k1Math.toAffine(c, r, x, y)) {
            return null;
        }
        return new int[][] {x, y};
    }

    // r = u q + v G for public data, splitting both scalars by the curve's
    // endomorphism so the doubling chain is 129 steps long, not 256
    private static void doubleMul(Secp256k1Math.Gej r, int[][] q, int[] u, int[] v) {
        int[][] su = split(u);
        int[][] sv = split(v);
        Secp256k1Math.ecmult(r, q[0], q[1], su[0], su[2][0] != 0, su[1], su[2][1] != 0,
                sv[0], sv[2][0] != 0, sv[1], sv[2][1] != 0);
    }

    // {k1 words, k2 words, {k1 negative, k2 negative}} with
    // k == k1 + k2 lambda (mod n) and |k1|, |k2| < 2^128
    private static int[][] split(int[] k) {
        int[] c1 = mulShift384(k, GLV_G1);
        int[] c2 = mulShift384(k, GLV_G2);
        int[] t1 = new int[LIMBS];
        int[] t2 = new int[LIMBS];
        FN.mul(c1, GLV_MINUS_B1, t1);
        FN.mul(c2, GLV_MINUS_B2, t2);
        int[] k2 = new int[LIMBS];
        FN.add(t1, t2, k2);
        int[] k1 = new int[LIMBS];
        FN.mul(k2, GLV_MINUS_LAMBDA, k1);
        FN.add(k1, k, k1);
        int n1 = 0;
        int n2 = 0;
        if (lessThan(N_HALF, k1)) {
            FN.sub(ZERO, k1, k1);
            n1 = 1;
        }
        if (lessThan(N_HALF, k2)) {
            FN.sub(ZERO, k2, k2);
            n2 = 1;
        }
        return new int[][] {words(k1), words(k2), {n1, n2}};
    }

    // round(a b / 2^384) for 256-bit a and b
    private static int[] mulShift384(int[] a, int[] b) {
        long[] c = new long[2 * LIMBS + 1];
        for (int i = 0; i < LIMBS; i++) {
            long ai = a[i];
            for (int j = 0; j < LIMBS; j++) {
                c[i + j] += ai * b[j];
            }
        }
        for (int i = 0; i < 2 * LIMBS; i++) {
            c[i + 1] += c[i] >> LIMB_BITS;
            c[i] &= LIMB_MASK;
        }
        int[] out = new int[LIMBS];
        long carry = (c[23] >> 15) & 1;
        for (int i = 0; i < 8; i++) {
            long v = c[24 + i] + carry;
            out[i] = (int) (v & LIMB_MASK);
            carry = v >> LIMB_BITS;
        }
        out[8] = (int) carry;
        return out;
    }

    // 16-bit limbs to eight 32-bit words
    private static int[] words(int[] limbs) {
        int[] w = new int[8];
        for (int i = 0; i < 8; i++) {
            w[i] = limbs[2 * i] | (limbs[2 * i + 1] << 16);
        }
        return w;
    }

    // a reduced 256-bit value as a field element
    private static int[] fieldOf(int[] limbs) {
        byte[] b = new byte[32];
        toBytes(limbs, b, 0);
        int[] f = new int[10];
        Secp256k1Math.fromBytes(f, b, 0);
        return f;
    }

    // 1 / s mod n by safegcd; constant time
    private static int[] scalarInverse(int[] s) {
        int[] s30 = new int[9];
        long acc = 0;
        int bits = 0;
        int o = 0;
        for (int i = 0; i < LIMBS; i++) {
            acc |= ((long) s[i]) << bits;
            bits += LIMB_BITS;
            if (bits >= 30) {
                s30[o++] = (int) (acc & 0x3FFFFFFF);
                acc >>>= 30;
                bits -= 30;
            }
        }
        s30[o] = (int) acc;
        int[] inv = Secp256k1Math.modInverse(s30, Secp256k1Math.N30, Secp256k1Math.N_INV30);
        int[] out = new int[LIMBS];
        acc = 0;
        bits = 0;
        o = 0;
        for (int i = 0; i < 9; i++) {
            acc |= ((long) inv[i]) << bits;
            bits += 30;
            while (bits >= LIMB_BITS && o < LIMBS) {
                out[o++] = (int) (acc & LIMB_MASK);
                acc >>>= LIMB_BITS;
                bits -= LIMB_BITS;
            }
        }
        wipe(s30);
        wipe(inv);
        return out;
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
}
