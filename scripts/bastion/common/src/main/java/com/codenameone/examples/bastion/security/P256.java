/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.security;

import com.codename1.security.Hash;
import com.codename1.security.Hmac;
import com.codename1.util.BigInteger;

/// ECDSA over NIST P-256 with SHA-256 (ES256), in portable Java.
///
/// The device key is P-256 because that is what the server accepts and what
/// every phone's secure hardware can hold. The framework's
/// `com.codename1.security.KeyGenerator` makes RSA keys only, and the iOS
/// port's `Signature` signs with RSA keys only, so the sample does the curve
/// arithmetic itself: identical on every port, and checked against the JDK
/// and the RFC 6979 vectors by `P256Test`.
///
/// Signing uses RFC 6979 deterministic nonces (HMAC-SHA-256), so a weak
/// random generator cannot leak the key through a repeated `k`. This is not
/// constant time: a sample's key on a phone is not exposed to a timing
/// attacker the way a server's is, but a production app should move the key
/// into the platform keystore (non-exportable, hardware backed) once the
/// framework can generate EC keys there.
public final class P256 {
    static final BigInteger P = hex(
            "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff");
    static final BigInteger N = hex(
            "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551");
    static final BigInteger B = hex(
            "5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b");
    static final BigInteger GX = hex(
            "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296");
    static final BigInteger GY = hex(
            "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5");
    private static final BigInteger THREE = BigInteger.valueOf(3);

    /// The DER of a P-256 SubjectPublicKeyInfo up to the point.
    private static final byte[] SPKI_PREFIX = Hash.fromHex(
            "3059301306072a8648ce3d020106082a8648ce3d030107034200");

    private P256() {
    }

    /// A fresh private key: 32 bytes, a scalar in `[1, n-1]`.
    public static byte[] newPrivateKey(byte[] seed) {
        // Reduced from 48 bytes so the bias of the modulo is negligible.
        if (seed == null || seed.length < 48) {
            throw new IllegalArgumentException("48 random bytes are needed");
        }
        BigInteger d = new BigInteger(1, seed).mod(N.subtract(BigInteger.ONE)).add(BigInteger.ONE);
        return fixed(d);
    }

    /// The public key of `privateKey` as X.509 SubjectPublicKeyInfo DER, the
    /// form the server takes (`DeviceRegistrationDto.publicKey`, base64url).
    public static byte[] publicKey(byte[] privateKey) {
        BigInteger[] q = toAffine(multiply(scalar(privateKey), GX, GY));
        byte[] out = new byte[SPKI_PREFIX.length + 65];
        System.arraycopy(SPKI_PREFIX, 0, out, 0, SPKI_PREFIX.length);
        out[SPKI_PREFIX.length] = 4;
        System.arraycopy(fixed(q[0]), 0, out, SPKI_PREFIX.length + 1, 32);
        System.arraycopy(fixed(q[1]), 0, out, SPKI_PREFIX.length + 33, 32);
        return out;
    }

    /// Signs `message` (hashed here with SHA-256) and answers the 64-byte
    /// `r || s`.
    public static byte[] sign(byte[] privateKey, byte[] message) {
        BigInteger d = scalar(privateKey);
        byte[] digest = Hash.sha256(message);
        BigInteger e = new BigInteger(1, digest);
        Rfc6979 nonces = new Rfc6979(fixed(d), fixed(e.mod(N)));
        while (true) {
            BigInteger k = nonces.next();
            BigInteger[] point = toAffine(multiply(k, GX, GY));
            BigInteger r = point[0].mod(N);
            if (r.signum() == 0) {
                continue;
            }
            BigInteger s = inverse(k, N).multiply(e.add(r.multiply(d))).mod(N);
            if (s.signum() == 0) {
                continue;
            }
            byte[] out = new byte[64];
            System.arraycopy(fixed(r), 0, out, 0, 32);
            System.arraycopy(fixed(s), 0, out, 32, 32);
            return out;
        }
    }

    private static BigInteger scalar(byte[] privateKey) {
        if (privateKey == null || privateKey.length != 32) {
            throw new IllegalArgumentException("A P-256 private key is 32 bytes");
        }
        BigInteger d = new BigInteger(1, privateKey);
        if (d.signum() == 0 || d.compareTo(N) >= 0) {
            throw new IllegalArgumentException("Not a P-256 private key");
        }
        return d;
    }

    // ---------------------------------------------------- curve arithmetic
    // Jacobian coordinates {X, Y, Z}: x = X/Z^2, y = Y/Z^3; null is infinity.

    static BigInteger[] multiply(BigInteger k, BigInteger x, BigInteger y) {
        BigInteger[] base = {x, y, BigInteger.ONE};
        BigInteger[] acc = null;
        for (int bit = k.bitLength() - 1; bit >= 0; bit--) {
            acc = twice(acc);
            if (k.testBit(bit)) {
                acc = add(acc, base);
            }
        }
        return acc;
    }

    private static BigInteger[] twice(BigInteger[] p) {
        if (p == null || p[1].signum() == 0) {
            return null;
        }
        BigInteger delta = p[2].multiply(p[2]).mod(P);
        BigInteger gamma = p[1].multiply(p[1]).mod(P);
        BigInteger beta = p[0].multiply(gamma).mod(P);
        BigInteger alpha = THREE.multiply(p[0].subtract(delta)).multiply(p[0].add(delta)).mod(P);
        BigInteger x3 = alpha.multiply(alpha).subtract(beta.shiftLeft(3)).mod(P);
        BigInteger z3 = p[1].add(p[2]).pow(2).subtract(gamma).subtract(delta).mod(P);
        BigInteger y3 = alpha.multiply(beta.shiftLeft(2).subtract(x3))
                .subtract(gamma.multiply(gamma).shiftLeft(3)).mod(P);
        return new BigInteger[] {x3, y3, z3};
    }

    private static BigInteger[] add(BigInteger[] p, BigInteger[] q) {
        if (p == null) {
            return q;
        }
        if (q == null) {
            return p;
        }
        BigInteger z1z1 = p[2].multiply(p[2]).mod(P);
        BigInteger z2z2 = q[2].multiply(q[2]).mod(P);
        BigInteger u1 = p[0].multiply(z2z2).mod(P);
        BigInteger u2 = q[0].multiply(z1z1).mod(P);
        BigInteger s1 = p[1].multiply(q[2]).multiply(z2z2).mod(P);
        BigInteger s2 = q[1].multiply(p[2]).multiply(z1z1).mod(P);
        if (u1.equals(u2)) {
            return s1.equals(s2) ? twice(p) : null;
        }
        BigInteger h = u2.subtract(u1).mod(P);
        BigInteger i = h.shiftLeft(1).pow(2).mod(P);
        BigInteger j = h.multiply(i).mod(P);
        BigInteger r = s2.subtract(s1).shiftLeft(1).mod(P);
        BigInteger v = u1.multiply(i).mod(P);
        BigInteger x3 = r.multiply(r).subtract(j).subtract(v.shiftLeft(1)).mod(P);
        BigInteger y3 = r.multiply(v.subtract(x3)).subtract(s1.multiply(j).shiftLeft(1)).mod(P);
        BigInteger z3 = p[2].add(q[2]).pow(2).subtract(z1z1).subtract(z2z2).multiply(h).mod(P);
        return new BigInteger[] {x3, y3, z3};
    }

    static BigInteger[] toAffine(BigInteger[] p) {
        if (p == null) {
            throw new IllegalStateException("point at infinity");
        }
        BigInteger zi = inverse(p[2], P);
        BigInteger zi2 = zi.multiply(zi).mod(P);
        BigInteger x = p[0].multiply(zi2).mod(P);
        BigInteger y = p[1].multiply(zi2).multiply(zi).mod(P);
        // y^2 = x^3 - 3x + b: a wrong result never leaves this class.
        BigInteger left = y.multiply(y).mod(P);
        BigInteger right = x.pow(3).subtract(THREE.multiply(x)).add(B).mod(P);
        if (!left.equals(right)) {
            throw new IllegalStateException("point is not on the curve");
        }
        return new BigInteger[] {x, y};
    }

    /// `value` to the power -1 modulo the prime `m`, by Fermat: value^(m-2).
    ///
    /// Not `BigInteger.modInverse`: the framework's answers wrong for 256-bit
    /// operands (it agrees with `java.math` on small numbers only), which
    /// `P256Test` would catch at once.
    static BigInteger inverse(BigInteger value, BigInteger m) {
        return value.mod(m).modPow(m.subtract(BigInteger.valueOf(2)), m);
    }

    /// `value` as exactly 32 unsigned big-endian bytes.
    static byte[] fixed(BigInteger value) {
        byte[] raw = value.toByteArray();
        byte[] out = new byte[32];
        int copy = Math.min(raw.length, 32);
        System.arraycopy(raw, raw.length - copy, out, 32 - copy, copy);
        return out;
    }

    private static BigInteger hex(String text) {
        return new BigInteger(text, 16);
    }

    /// RFC 6979 section 3.2 with HMAC-SHA-256, for a 256-bit order.
    private static final class Rfc6979 {
        private byte[] v = new byte[32];
        private byte[] k = new byte[32];

        Rfc6979(byte[] x, byte[] h1) {
            for (int iter = 0; iter < 32; iter++) {
                v[iter] = 1;
            }
            k = Hmac.sha256(k, concat(v, new byte[] {0}, x, h1));
            v = Hmac.sha256(k, v);
            k = Hmac.sha256(k, concat(v, new byte[] {1}, x, h1));
            v = Hmac.sha256(k, v);
        }

        BigInteger next() {
            while (true) {
                v = Hmac.sha256(k, v);
                BigInteger candidate = new BigInteger(1, v);
                if (candidate.signum() > 0 && candidate.compareTo(N) < 0) {
                    // Moved on now, so a retry (r or s zero) gets a new k.
                    k = Hmac.sha256(k, concat(v, new byte[] {0}, new byte[0], new byte[0]));
                    v = Hmac.sha256(k, v);
                    return candidate;
                }
                k = Hmac.sha256(k, concat(v, new byte[] {0}, new byte[0], new byte[0]));
                v = Hmac.sha256(k, v);
            }
        }

        private static byte[] concat(byte[] a, byte[] b, byte[] c, byte[] d) {
            byte[] out = new byte[a.length + b.length + c.length + d.length];
            System.arraycopy(a, 0, out, 0, a.length);
            System.arraycopy(b, 0, out, a.length, b.length);
            System.arraycopy(c, 0, out, a.length + b.length, c.length);
            System.arraycopy(d, 0, out, a.length + b.length + c.length, d.length);
            return out;
        }
    }
}
