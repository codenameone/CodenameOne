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
package com.codename1.impl;

/// Converts P-256 keys between the DER encodings the portable security API
/// carries (X.509 SubjectPublicKeyInfo, PKCS#8 PrivateKeyInfo) and the raw
/// forms native providers take: the 65-byte uncompressed point `04||X||Y` and
/// the 32-byte private scalar.
///
/// The ports whose providers import raw keys -- Security.framework's X9.63
/// representation on iOS, the BCrypt ECC blobs on Windows -- convert here, in
/// Java, so the ASN.1 lives in one place instead of once per native layer.
///
/// This is port plumbing, not application API.
public final class EcP256Codec {
    /// Length of an uncompressed point: one tag byte and two 32-byte coordinates.
    public static final int POINT_LENGTH = 65;
    /// Length of a private scalar and of each coordinate.
    public static final int SCALAR_LENGTH = 32;

    // SEQUENCE { SEQUENCE { OID ecPublicKey, OID prime256v1 }, BIT STRING (66) }
    private static final byte[] SPKI_PREFIX = {
        0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x02, 0x01,
        0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00
    };

    // The AlgorithmIdentifier alone, as it appears inside a PKCS#8 wrapper.
    private static final byte[] ALGORITHM_ID = {
        0x30, 0x13, 0x06, 0x07, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x02, 0x01,
        0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x03, 0x01, 0x07
    };

    private static final byte[] CURVE_OID = {
        0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x03, 0x01, 0x07
    };

    private EcP256Codec() {}

    /// Wraps an uncompressed point as X.509 SubjectPublicKeyInfo DER.
    public static byte[] spkiFromPoint(byte[] point) {
        checkPoint(point);
        byte[] out = new byte[SPKI_PREFIX.length + POINT_LENGTH];
        System.arraycopy(SPKI_PREFIX, 0, out, 0, SPKI_PREFIX.length);
        System.arraycopy(point, 0, out, SPKI_PREFIX.length, POINT_LENGTH);
        return out;
    }

    /// Extracts the uncompressed point from P-256 SubjectPublicKeyInfo DER.
    /// Every provider encodes a named-curve P-256 key identically, so anything
    /// that is not exactly this structure is refused rather than guessed at.
    public static byte[] pointFromSpki(byte[] spki) {
        if (spki == null || spki.length != SPKI_PREFIX.length + POINT_LENGTH) {
            throw new RuntimeException("not a P-256 SubjectPublicKeyInfo");
        }
        for (int i = 0; i < SPKI_PREFIX.length; i++) {
            if (spki[i] != SPKI_PREFIX[i]) {
                throw new RuntimeException("not a P-256 SubjectPublicKeyInfo");
            }
        }
        byte[] point = new byte[POINT_LENGTH];
        System.arraycopy(spki, SPKI_PREFIX.length, point, 0, POINT_LENGTH);
        checkPoint(point);
        return point;
    }

    /// Builds PKCS#8 PrivateKeyInfo DER for a scalar and its public point. The
    /// point is included (the optional `publicKey` field of SEC 1's
    /// ECPrivateKey) because Security.framework and Web Crypto in some browsers
    /// will not import a private key without it.
    public static byte[] pkcs8FromScalar(byte[] scalar, byte[] point) {
        if (scalar == null || scalar.length != SCALAR_LENGTH) {
            throw new RuntimeException("P-256 private scalar must be 32 bytes");
        }
        checkPoint(point);
        byte[] out = new byte[138];
        int p = 0;
        out[p++] = 0x30;
        out[p++] = (byte) 0x81;
        out[p++] = (byte) 0x87;
        out[p++] = 0x02;
        out[p++] = 0x01;
        out[p++] = 0x00;
        System.arraycopy(ALGORITHM_ID, 0, out, p, ALGORITHM_ID.length);
        p += ALGORITHM_ID.length;
        out[p++] = 0x04;
        out[p++] = 0x6d;
        out[p++] = 0x30;
        out[p++] = 0x6b;
        out[p++] = 0x02;
        out[p++] = 0x01;
        out[p++] = 0x01;
        out[p++] = 0x04;
        out[p++] = 0x20;
        System.arraycopy(scalar, 0, out, p, SCALAR_LENGTH);
        p += SCALAR_LENGTH;
        out[p++] = (byte) 0xa1;
        out[p++] = 0x44;
        out[p++] = 0x03;
        out[p++] = 0x42;
        out[p++] = 0x00;
        System.arraycopy(point, 0, out, p, POINT_LENGTH);
        return out;
    }

    /// Returns `{scalar, point}` from P-256 PKCS#8 DER. `point` is null when
    /// the encoding omits it, which SEC 1 allows and the JDK sometimes does.
    public static byte[][] fromPkcs8(byte[] der) {
        Der outer = new Der(der, 0, der == null ? 0 : der.length);
        Der info = outer.enter(0x30);
        outer.end();
        info.skip(0x02);
        Der algorithm = info.enter(0x30);
        algorithm.skip(0x06);
        if (!algorithm.matches(CURVE_OID)) {
            throw new RuntimeException("PKCS#8 key is not on P-256");
        }
        Der body = info.enter(0x04);
        Der ec = body.enter(0x30);
        ec.skip(0x02);
        byte[] raw = ec.read(0x04);
        if (raw.length > SCALAR_LENGTH) {
            throw new RuntimeException("P-256 private scalar too long");
        }
        byte[] scalar = new byte[SCALAR_LENGTH];
        System.arraycopy(raw, 0, scalar, SCALAR_LENGTH - raw.length, raw.length);
        byte[] point = null;
        while (ec.hasMore()) {
            int tag = ec.peek();
            if (tag == 0xa1) {
                Der wrapped = ec.enter(0xa1);
                byte[] bits = wrapped.read(0x03);
                if (bits.length != POINT_LENGTH + 1 || bits[0] != 0) {
                    throw new RuntimeException("malformed P-256 public key in PKCS#8");
                }
                point = new byte[POINT_LENGTH];
                System.arraycopy(bits, 1, point, 0, POINT_LENGTH);
                checkPoint(point);
            } else {
                ec.skip(tag);
            }
        }
        return new byte[][] {scalar, point};
    }

    /// Converts a DER ECDSA signature (`SEQUENCE { INTEGER r, INTEGER s }`, the
    /// JCA form) to the fixed 64-byte `r||s` form Web Crypto and BCrypt use.
    public static byte[] signatureDerToRaw(byte[] der) {
        Der outer = new Der(der, 0, der == null ? 0 : der.length);
        Der seq = outer.enter(0x30);
        byte[] r = seq.read(0x02);
        byte[] s = seq.read(0x02);
        byte[] out = new byte[64];
        copyInteger(r, out, 0);
        copyInteger(s, out, 32);
        return out;
    }

    /// Converts a 64-byte `r||s` signature to DER.
    public static byte[] signatureRawToDer(byte[] raw) {
        if (raw == null || raw.length != 64) {
            throw new RuntimeException("raw P-256 signature must be 64 bytes");
        }
        byte[] r = integer(raw, 0);
        byte[] s = integer(raw, 32);
        int body = 2 + r.length + 2 + s.length;
        byte[] out = new byte[2 + body];
        out[0] = 0x30;
        out[1] = (byte) body;
        out[2] = 0x02;
        out[3] = (byte) r.length;
        System.arraycopy(r, 0, out, 4, r.length);
        out[4 + r.length] = 0x02;
        out[5 + r.length] = (byte) s.length;
        System.arraycopy(s, 0, out, 6 + r.length, s.length);
        return out;
    }

    private static void copyInteger(byte[] value, byte[] out, int offset) {
        int start = 0;
        while (start < value.length - 1 && value[start] == 0) {
            start++;
        }
        int len = value.length - start;
        if (len > 32) {
            throw new RuntimeException("ECDSA signature component too long");
        }
        System.arraycopy(value, start, out, offset + 32 - len, len);
    }

    private static byte[] integer(byte[] raw, int offset) {
        int start = offset;
        int end = offset + 32;
        while (start < end - 1 && raw[start] == 0) {
            start++;
        }
        boolean pad = (raw[start] & 0x80) != 0;
        byte[] out = new byte[end - start + (pad ? 1 : 0)];
        System.arraycopy(raw, start, out, pad ? 1 : 0, end - start);
        return out;
    }

    private static void checkPoint(byte[] point) {
        if (point == null || point.length != POINT_LENGTH || point[0] != 0x04) {
            throw new RuntimeException("P-256 public point must be 65 bytes starting with 0x04");
        }
    }

    /// A cursor over one level of DER. Lengths are definite (DER forbids
    /// anything else) and at most two bytes, which bounds every key here.
    private static final class Der {
        private final byte[] data;
        private int pos;
        private final int end;

        Der(byte[] data, int pos, int end) {
            if (data == null) {
                throw new RuntimeException("DER input must not be null");
            }
            this.data = data;
            this.pos = pos;
            this.end = end;
        }

        boolean hasMore() {
            return pos < end;
        }

        int peek() {
            if (pos >= end) {
                throw new RuntimeException("truncated DER");
            }
            return data[pos] & 0xff;
        }

        void end() {
            if (pos != end) {
                throw new RuntimeException("trailing data after DER key");
            }
        }

        private int header(int tag) {
            if (peek() != tag) {
                throw new RuntimeException("unexpected DER tag " + peek() + ", wanted " + tag);
            }
            pos++;
            if (pos >= end) {
                throw new RuntimeException("truncated DER");
            }
            int len = data[pos++] & 0xff;
            if (len == 0x81) {
                if (pos >= end) {
                    throw new RuntimeException("truncated DER");
                }
                len = data[pos++] & 0xff;
            } else if (len == 0x82) {
                if (pos + 1 >= end) {
                    throw new RuntimeException("truncated DER");
                }
                len = ((data[pos] & 0xff) << 8) | (data[pos + 1] & 0xff);
                pos += 2;
            } else if (len > 0x7f) {
                throw new RuntimeException("unsupported DER length");
            }
            if (pos + len > end) {
                throw new RuntimeException("truncated DER");
            }
            return len;
        }

        Der enter(int tag) {
            int len = header(tag);
            Der inner = new Der(data, pos, pos + len);
            pos += len;
            return inner;
        }

        byte[] read(int tag) {
            int len = header(tag);
            byte[] out = new byte[len];
            System.arraycopy(data, pos, out, 0, len);
            pos += len;
            return out;
        }

        void skip(int tag) {
            // Not "pos += header(tag)": Java reads pos before header() moves it.
            int len = header(tag);
            pos += len;
        }

        boolean matches(byte[] expected) {
            if (end - pos != expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if (data[pos + i] != expected[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
