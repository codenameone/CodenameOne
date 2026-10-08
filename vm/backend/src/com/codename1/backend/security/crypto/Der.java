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
package com.codename1.backend.security.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// The little ASN.1 DER a server needs to move keys and signatures between the
/// shapes they travel in: a JSON Web Key's numbers and the SubjectPublicKeyInfo
/// that [com.codename1.backend.Crypto#verify] takes; a PKCS#1 or SEC 1 private
/// key out of an older PEM file and the PKCS#8 that
/// [com.codename1.backend.Crypto#sign] takes; an ECDSA signature as OpenSSL and
/// the JDK write it and as a JSON Web Signature carries it.
///
/// ```java
/// byte[] publicKey = Der.rsaPublicKey(modulus, exponent);      // from a JWK's n and e
/// byte[] jose = Der.ecdsaDerToJose(Crypto.sign(Crypto.ES256, key, data), 32);
/// ```
///
/// Nothing here computes with a key. It reads and writes the envelope, and
/// refuses one that is not exactly what it expects with an [IOException].
public final class Der {
    /// rsaEncryption, 1.2.840.113549.1.1.1.
    private static final byte[] OID_RSA = {0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d,
        0x01, 0x01, 0x01};
    /// id-ecPublicKey, 1.2.840.10045.2.1.
    private static final byte[] OID_EC = {0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x02, 0x01};
    /// prime256v1, 1.2.840.10045.3.1.7.
    private static final byte[] OID_P256 = {0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x03, 0x01,
        0x07};
    /// secp384r1, 1.3.132.0.34.
    private static final byte[] OID_P384 = {0x2b, (byte) 0x81, 0x04, 0x00, 0x22};

    private static final int INTEGER = 0x02;
    private static final int BIT_STRING = 0x03;
    private static final int OCTET_STRING = 0x04;
    private static final int NULL = 0x05;
    private static final int OID = 0x06;
    private static final int SEQUENCE = 0x30;

    /// The key type of an RSA key, as a JSON Web Key names it.
    public static final String RSA = "RSA";
    /// The key type of an elliptic curve key, as a JSON Web Key names it.
    public static final String EC = "EC";
    /// The curve of ES256, as a JSON Web Key names it.
    public static final String P256 = "P-256";
    /// The curve of ES384, as a JSON Web Key names it.
    public static final String P384 = "P-384";

    private Der() {
    }

    // ------------------------------------------------------------ public keys

    /// The SubjectPublicKeyInfo of an RSA public key.
    ///
    /// @param modulus the modulus as an unsigned big-endian number, as a JWK's
    /// `n` decodes
    /// @param exponent the public exponent, likewise: a JWK's `e`
    public static byte[] rsaPublicKey(byte[] modulus, byte[] exponent) throws IOException {
        if (modulus == null || exponent == null || isZero(modulus) || isZero(exponent)) {
            throw new IOException("An RSA public key needs a modulus and an exponent");
        }
        byte[] key = tlv(SEQUENCE, integer(modulus), integer(exponent));
        return tlv(SEQUENCE, tlv(SEQUENCE, tlv(OID, OID_RSA), tlv(NULL)), bitString(key));
    }

    /// The modulus and the public exponent of an RSA SubjectPublicKeyInfo, each
    /// an unsigned big-endian number without leading zeros.
    public static byte[][] rsaPublicKeyParts(byte[] publicKey) throws IOException {
        Reader info = spki(publicKey, OID_RSA, "an RSA public key");
        Reader key = new Reader(unwrapBits(info)).open(SEQUENCE);
        byte[] modulus = unsigned(key.take(INTEGER));
        byte[] exponent = unsigned(key.take(INTEGER));
        if (isZero(modulus) || isZero(exponent)) {
            throw new IOException("Not an RSA public key: an empty modulus or exponent");
        }
        return new byte[][] {modulus, exponent};
    }

    /// The size of an RSA public key: the bits of its modulus.
    public static int rsaModulusBits(byte[] publicKey) throws IOException {
        byte[] modulus = rsaPublicKeyParts(publicKey)[0];
        int bits = modulus.length * 8;
        for (int mask = 0x80 ; mask != 0 && (modulus[0] & mask) == 0 ; mask >>= 1) {
            bits--;
        }
        return bits;
    }

    /// The SubjectPublicKeyInfo of a public key on [#P256] or [#P384].
    ///
    /// @param curve the curve as a JWK's `crv` names it
    /// @param x the point's first coordinate, big-endian: a JWK's `x`
    /// @param y its second: a JWK's `y`
    public static byte[] ecPublicKey(String curve, byte[] x, byte[] y) throws IOException {
        int size = ecCoordinateLength(curve);
        if (x == null || y == null || x.length != size || y.length != size) {
            throw new IOException("A " + curve + " public key has two coordinates of " + size
                    + " bytes each");
        }
        byte[] point = new byte[1 + 2 * size];
        point[0] = 0x04;
        System.arraycopy(x, 0, point, 1, size);
        System.arraycopy(y, 0, point, 1 + size, size);
        return tlv(SEQUENCE, tlv(SEQUENCE, tlv(OID, OID_EC),
                tlv(OID, P256.equals(curve) ? OID_P256 : OID_P384)), bitString(point));
    }

    /// The curve of an EC SubjectPublicKeyInfo: [#P256] or [#P384].
    public static String ecCurve(byte[] publicKey) throws IOException {
        Reader info = new Reader(publicKey).open(SEQUENCE);
        Reader algorithm = info.open(SEQUENCE);
        if (!same(algorithm.take(OID), OID_EC)) {
            throw new IOException("Not an EC public key");
        }
        return curveName(algorithm.take(OID));
    }

    /// The two coordinates of an EC SubjectPublicKeyInfo's point, each as long
    /// as [#ecCoordinateLength] says.
    public static byte[][] ecPublicKeyParts(byte[] publicKey) throws IOException {
        int size = ecCoordinateLength(ecCurve(publicKey));
        Reader info = new Reader(publicKey).open(SEQUENCE);
        info.open(SEQUENCE);
        byte[] point = unwrapBits(info);
        if (point.length != 1 + 2 * size || point[0] != 0x04) {
            throw new IOException("Not an uncompressed point of the key's curve");
        }
        byte[] x = new byte[size];
        byte[] y = new byte[size];
        System.arraycopy(point, 1, x, 0, size);
        System.arraycopy(point, 1 + size, y, 0, size);
        return new byte[][] {x, y};
    }

    /// The bytes one coordinate -- and each half of a JOSE signature -- takes on
    /// a curve: 32 on [#P256], 48 on [#P384].
    public static int ecCoordinateLength(String curve) throws IOException {
        if (P256.equals(curve)) {
            return 32;
        }
        if (P384.equals(curve)) {
            return 48;
        }
        throw new IOException("Not a supported curve: " + curve + ". The curves are P-256 and P-384");
    }

    /// [#RSA] or [#EC]: what a SubjectPublicKeyInfo is the key of.
    public static String publicKeyType(byte[] publicKey) throws IOException {
        Reader algorithm = new Reader(publicKey).open(SEQUENCE).open(SEQUENCE);
        return keyType(algorithm.take(OID));
    }

    // ----------------------------------------------------------- private keys

    /// [#RSA] or [#EC]: what a PKCS#8 private key is the key of.
    public static String privateKeyType(byte[] privateKey) throws IOException {
        Reader info = new Reader(privateKey).open(SEQUENCE);
        info.take(INTEGER);
        return keyType(info.open(SEQUENCE).take(OID));
    }

    /// The public half of a PKCS#8 private key, as a SubjectPublicKeyInfo.
    ///
    /// An RSA private key always holds its public numbers. An EC private key
    /// holds its public point only when whatever wrote it put it there, which
    /// openssl does; one that does not is refused, and its public key has to be
    /// given beside it.
    public static byte[] publicKeyOf(byte[] privateKey) throws IOException {
        Reader info = new Reader(privateKey).open(SEQUENCE);
        info.take(INTEGER);
        Reader algorithm = info.open(SEQUENCE);
        byte[] type = algorithm.take(OID);
        Reader key = new Reader(info.take(OCTET_STRING)).open(SEQUENCE);
        key.take(INTEGER);
        if (same(type, OID_RSA)) {
            return rsaPublicKey(unsigned(key.take(INTEGER)), unsigned(key.take(INTEGER)));
        }
        if (!same(type, OID_EC)) {
            throw new IOException("Not an RSA or EC private key");
        }
        byte[] curve = algorithm.take(OID);
        curveName(curve);
        key.take(OCTET_STRING);
        while (!key.atEnd()) {
            int tag = key.peek();
            if (tag == 0xa1) {
                byte[] point = unwrapBits(key.open(0xa1));
                return tlv(SEQUENCE, tlv(SEQUENCE, tlv(OID, OID_EC), tlv(OID, curve)),
                        bitString(point));
            }
            key.take(tag);
        }
        throw new IOException("This EC private key does not carry its public key. Give the "
                + "public key as well, or write the private key again with "
                + "`openssl pkcs8 -topk8 -nocrypt`, which includes it");
    }

    /// Wraps a PKCS#1 `RSAPrivateKey` -- the content of a PEM file that says
    /// `RSA PRIVATE KEY` -- as PKCS#8.
    public static byte[] pkcs1ToPkcs8(byte[] rsaPrivateKey) throws IOException {
        Reader key = new Reader(rsaPrivateKey).open(SEQUENCE);
        key.take(INTEGER);
        key.take(INTEGER);
        key.take(INTEGER);
        return tlv(SEQUENCE, tlv(INTEGER, new byte[1]),
                tlv(SEQUENCE, tlv(OID, OID_RSA), tlv(NULL)), tlv(OCTET_STRING, rsaPrivateKey));
    }

    /// Wraps a SEC 1 `ECPrivateKey` -- the content of a PEM file that says
    /// `EC PRIVATE KEY` -- as PKCS#8. The key has to name its curve, as one
    /// openssl wrote does.
    public static byte[] sec1ToPkcs8(byte[] ecPrivateKey) throws IOException {
        Reader key = new Reader(ecPrivateKey).open(SEQUENCE);
        key.take(INTEGER);
        key.take(OCTET_STRING);
        byte[] curve = null;
        while (!key.atEnd()) {
            int tag = key.peek();
            if (tag == 0xa0) {
                curve = key.open(0xa0).take(OID);
            } else {
                key.take(tag);
            }
        }
        if (curve == null) {
            throw new IOException("This EC private key does not name its curve");
        }
        curveName(curve);
        return tlv(SEQUENCE, tlv(INTEGER, new byte[1]),
                tlv(SEQUENCE, tlv(OID, OID_EC), tlv(OID, curve)), tlv(OCTET_STRING, ecPrivateKey));
    }

    // ------------------------------------------------------------- signatures

    /// An ECDSA signature as a JSON Web Signature carries it -- r and s side by
    /// side, each padded to the curve's size -- from the ASN.1 DER that
    /// [com.codename1.backend.Crypto#sign] returns.
    ///
    /// @param partLength [#ecCoordinateLength] of the key's curve
    public static byte[] ecdsaDerToJose(byte[] der, int partLength) throws IOException {
        Reader signature = new Reader(der).open(SEQUENCE);
        byte[] r = unsigned(signature.take(INTEGER));
        byte[] s = unsigned(signature.take(INTEGER));
        if (!signature.atEnd() || r.length > partLength || s.length > partLength) {
            throw new IOException("Not an ECDSA signature of that curve");
        }
        byte[] out = new byte[2 * partLength];
        System.arraycopy(r, 0, out, partLength - r.length, r.length);
        System.arraycopy(s, 0, out, 2 * partLength - s.length, s.length);
        return out;
    }

    /// The ASN.1 DER [com.codename1.backend.Crypto#verify] takes, from an ECDSA
    /// signature as a JSON Web Signature carries it.
    public static byte[] ecdsaJoseToDer(byte[] jose) throws IOException {
        if (jose == null || jose.length == 0 || (jose.length & 1) != 0) {
            throw new IOException("Not an ECDSA signature: two numbers of equal length");
        }
        int half = jose.length / 2;
        byte[] r = new byte[half];
        byte[] s = new byte[half];
        System.arraycopy(jose, 0, r, 0, half);
        System.arraycopy(jose, half, s, 0, half);
        return tlv(SEQUENCE, integer(r), integer(s));
    }

    // -------------------------------------------------------------- internals

    private static String keyType(byte[] oid) throws IOException {
        if (same(oid, OID_RSA)) {
            return RSA;
        }
        if (same(oid, OID_EC)) {
            return EC;
        }
        throw new IOException("Not an RSA or EC key");
    }

    private static String curveName(byte[] oid) throws IOException {
        if (same(oid, OID_P256)) {
            return P256;
        }
        if (same(oid, OID_P384)) {
            return P384;
        }
        throw new IOException("Not a supported curve. The curves are P-256 and P-384");
    }

    /// The SubjectPublicKeyInfo of a key of one algorithm, read up to its bits.
    private static Reader spki(byte[] publicKey, byte[] oid, String what) throws IOException {
        Reader info = new Reader(publicKey).open(SEQUENCE);
        if (!same(info.open(SEQUENCE).take(OID), oid)) {
            throw new IOException("Not " + what);
        }
        return info;
    }

    /// The content of a BIT STRING that holds whole bytes.
    private static byte[] unwrapBits(Reader from) throws IOException {
        byte[] bits = from.take(BIT_STRING);
        if (bits.length < 1 || bits[0] != 0) {
            throw new IOException("Not a key: a bit string that is not whole bytes");
        }
        byte[] out = new byte[bits.length - 1];
        System.arraycopy(bits, 1, out, 0, out.length);
        return out;
    }

    private static byte[] bitString(byte[] content) {
        byte[] bits = new byte[content.length + 1];
        System.arraycopy(content, 0, bits, 1, content.length);
        return tlv(BIT_STRING, bits);
    }

    private static boolean isZero(byte[] number) {
        for (byte b : number) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /// A number without the leading zeros DER and callers put in front of it.
    private static byte[] unsigned(byte[] number) {
        int start = 0;
        while (start < number.length - 1 && number[start] == 0) {
            start++;
        }
        if (start == 0) {
            return number;
        }
        byte[] out = new byte[number.length - start];
        System.arraycopy(number, start, out, 0, out.length);
        return out;
    }

    /// An INTEGER holding an unsigned number: minimal, and with a zero in front
    /// when its top bit would otherwise make it negative.
    private static byte[] integer(byte[] number) {
        byte[] minimal = unsigned(number);
        if (minimal.length == 0) {
            return tlv(INTEGER, new byte[1]);
        }
        if ((minimal[0] & 0x80) == 0) {
            return tlv(INTEGER, minimal);
        }
        byte[] padded = new byte[minimal.length + 1];
        System.arraycopy(minimal, 0, padded, 1, minimal.length);
        return tlv(INTEGER, padded);
    }

    private static boolean same(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int iter = 0 ; iter < a.length ; iter++) {
            if (a[iter] != b[iter]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] tlv(int tag, byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(length + 6);
        out.write(tag);
        if (length < 0x80) {
            out.write(length);
        } else if (length < 0x100) {
            out.write(0x81);
            out.write(length);
        } else if (length < 0x10000) {
            out.write(0x82);
            out.write(length >> 8);
            out.write(length);
        } else {
            out.write(0x83);
            out.write(length >> 16);
            out.write(length >> 8);
            out.write(length);
        }
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    /// A cursor over DER: one tag, length and value at a time, never past the
    /// end of what it was given.
    private static final class Reader {
        private final byte[] data;
        private int pos;
        private final int end;

        Reader(byte[] data) throws IOException {
            if (data == null) {
                throw new IOException("No key or signature was given");
            }
            this.data = data;
            this.end = data.length;
        }

        private Reader(byte[] data, int pos, int end) {
            this.data = data;
            this.pos = pos;
            this.end = end;
        }

        boolean atEnd() {
            return pos >= end;
        }

        int peek() throws IOException {
            if (pos >= end) {
                throw new IOException("Truncated DER");
            }
            return data[pos] & 0xff;
        }

        /// Steps over the next element's header, which must have this tag, and
        /// answers the length of its value.
        private int header(int tag) throws IOException {
            if (peek() != tag) {
                throw new IOException("Unexpected DER: wanted tag 0x" + Integer.toHexString(tag)
                        + ", found 0x" + Integer.toHexString(peek()));
            }
            pos++;
            if (pos >= end) {
                throw new IOException("Truncated DER");
            }
            int length = data[pos++] & 0xff;
            if (length >= 0x80) {
                int count = length & 0x7f;
                if (count == 0 || count > 3 || pos + count > end) {
                    throw new IOException("Unsupported DER length");
                }
                length = 0;
                for (int iter = 0 ; iter < count ; iter++) {
                    length = (length << 8) | (data[pos++] & 0xff);
                }
            }
            if (length > end - pos) {
                throw new IOException("Truncated DER");
            }
            return length;
        }

        /// A cursor over the value of the next element.
        Reader open(int tag) throws IOException {
            int length = header(tag);
            Reader inner = new Reader(data, pos, pos + length);
            pos += length;
            return inner;
        }

        /// The value of the next element.
        byte[] take(int tag) throws IOException {
            int length = header(tag);
            byte[] out = new byte[length];
            System.arraycopy(data, pos, out, 0, length);
            pos += length;
            return out;
        }
    }
}
