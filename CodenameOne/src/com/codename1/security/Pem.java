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

import com.codename1.util.Base64;
import com.codename1.util.StringUtil;

/// PEM (RFC 7468) decoding for [PublicKey] and [PrivateKey].
///
/// The platform crypto bridge only accepts X.509/SubjectPublicKeyInfo and
/// PKCS#8 DER, but the files people actually have on disk are PEM: base64
/// wrapped in `-----BEGIN ...-----` armor, and often in the older PKCS#1 or
/// SEC1 container rather than the one the bridge wants. This class absorbs
/// both differences so callers do not have to run `openssl` first.
///
/// Nothing here is exposed publicly; the entry points are the `fromPem`
/// factories on [PublicKey] and [PrivateKey].
final class Pem {
    private static final String BEGIN = "-----BEGIN ";
    private static final String END = "-----END ";
    private static final String DASHES = "-----";

    /// OID 1.2.840.113549.1.1.1 -- rsaEncryption.
    private static final byte[] OID_RSA = {
        (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
        (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x01
    };

    /// OID 1.2.840.10045.2.1 -- id-ecPublicKey.
    private static final byte[] OID_EC = {
        (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0xCE, (byte) 0x3D,
        (byte) 0x02, (byte) 0x01
    };

    private Pem() {
    }

    /// Decodes `pem` to X.509/SubjectPublicKeyInfo DER.
    ///
    /// Accepts a `PUBLIC KEY` block, a PKCS#1 `RSA PUBLIC KEY` block (rewrapped
    /// here), or bare base64 with no armor at all.
    static byte[] toSpki(String pem) {
        String label = label(pem);
        byte[] der = body(pem, label);
        if (label == null) {
            if (looksLikePrivateKey(der)) {
                throw new CryptoException("this is a private key, not a public key");
            }
            return der;
        }
        if ("PUBLIC KEY".equals(label)) {
            return der;
        }
        if ("RSA PUBLIC KEY".equals(label)) {
            // PKCS#1 RSAPublicKey -> SubjectPublicKeyInfo. The BIT STRING needs
            // a leading zero byte for "no unused bits in the final octet".
            byte[] bitString = new byte[der.length + 1];
            System.arraycopy(der, 0, bitString, 1, der.length);
            return tlv(0x30, concat(rsaAlgorithmIdentifier(), tlv(0x03, bitString)));
        }
        if ("CERTIFICATE".equals(label)) {
            throw new CryptoException("this is a certificate, not a public key; "
                    + "extract the key with: openssl x509 -in cert.pem -pubkey -noout");
        }
        throw new CryptoException("not a public key PEM block: -----BEGIN " + label + "-----");
    }

    /// Decodes `pem` to PKCS#8 PrivateKeyInfo DER.
    ///
    /// Accepts a `PRIVATE KEY` block, the older PKCS#1 `RSA PRIVATE KEY` and
    /// SEC1 `EC PRIVATE KEY` blocks (rewrapped here), or bare base64 with no
    /// armor at all.
    static byte[] toPkcs8(String pem) {
        String label = label(pem);
        byte[] der = body(pem, label);
        if (label == null) {
            if (!looksLikePrivateKey(der)) {
                throw new CryptoException("this is a public key, not a private key");
            }
            return der;
        }
        if ("PRIVATE KEY".equals(label)) {
            return der;
        }
        if ("RSA PRIVATE KEY".equals(label)) {
            // PKCS#1 RSAPrivateKey -> PrivateKeyInfo.
            return wrapPkcs8(rsaAlgorithmIdentifier(), der);
        }
        if ("EC PRIVATE KEY".equals(label)) {
            return sec1ToPkcs8(der);
        }
        if ("ENCRYPTED PRIVATE KEY".equals(label)) {
            throw new CryptoException("this private key is passphrase-encrypted; decrypt it first with: "
                    + "openssl pkcs8 -topk8 -nocrypt -in key.pem -out key_pkcs8.pem");
        }
        throw new CryptoException("not a private key PEM block: -----BEGIN " + label + "-----");
    }

    /// Reads the algorithm OID out of an SPKI or PKCS#8 blob and maps it to the
    /// name the crypto bridge expects ([PublicKey#RSA] or [PublicKey#EC]).
    ///
    /// Both containers reach the AlgorithmIdentifier the same way once an
    /// optional leading version INTEGER is skipped, so one walk covers both.
    static String algorithm(byte[] der) {
        Cursor c = new Cursor(der);
        c.enter(0x30);
        if (c.peek() == 0x02) {
            c.skip();
        }
        c.enter(0x30);
        byte[] oid = c.read(0x06);
        if (equal(oid, OID_RSA)) {
            return PublicKey.RSA;
        }
        if (equal(oid, OID_EC)) {
            return PublicKey.EC;
        }
        throw new CryptoException("unsupported key algorithm OID " + oidToString(oid)
                + "; only RSA and EC keys are supported");
    }

    /// Tells a PKCS#8 blob from an SPKI one by shape alone, for input that
    /// arrived as bare base64 and so carries no label to go on: PKCS#8 opens
    /// with a version INTEGER, SPKI opens straight into the AlgorithmIdentifier
    /// SEQUENCE.
    private static boolean looksLikePrivateKey(byte[] der) {
        Cursor c = new Cursor(der);
        c.enter(0x30);
        return c.peek() == 0x02;
    }

    /// Returns the label of the first armored block, or `null` when the input
    /// carries no armor and is therefore treated as bare base64.
    private static String label(String pem) {
        if (pem == null) {
            throw new CryptoException("pem must not be null");
        }
        int begin = pem.indexOf(BEGIN);
        if (begin < 0) {
            return null;
        }
        int labelStart = begin + BEGIN.length();
        int labelEnd = pem.indexOf(DASHES, labelStart);
        if (labelEnd < 0) {
            throw new CryptoException("malformed PEM: -----BEGIN line is not terminated");
        }
        return pem.substring(labelStart, labelEnd).trim();
    }

    /// Base64-decodes the payload between the armor lines (or the whole input
    /// when `label` is null).
    private static byte[] body(String pem, String label) {
        String base64;
        if (label == null) {
            base64 = pem;
        } else {
            int labelEnd = pem.indexOf(DASHES, pem.indexOf(BEGIN) + BEGIN.length());
            int bodyStart = labelEnd + DASHES.length();
            // RFC 7468 requires the footer to repeat the header's label. Stopping
            // at any "-----END" would accept a file whose blocks have been
            // spliced together (BEGIN PUBLIC KEY closed by END PRIVATE KEY), and
            // running to end-of-input would accept one that was cut off before
            // its footer ever arrived.
            String footer = END + label + DASHES;
            int bodyEnd = pem.indexOf(footer, bodyStart);
            if (bodyEnd < 0) {
                throw new CryptoException("malformed PEM: no matching " + footer + " footer");
            }
            base64 = pem.substring(bodyStart, bodyEnd);
        }
        // Base64.decode tolerates the line breaks but nothing else, and answers
        // null rather than throwing when it meets a character it cannot map.
        byte[] der = Base64.decode(StringUtil.getBytes(base64));
        if (der == null || der.length == 0) {
            throw new CryptoException("malformed PEM: the body is not valid base64");
        }
        requireWholeElement(der);
        return der;
    }

    /// Checks that the decoded body is exactly one complete DER element.
    ///
    /// Without this a truncated or padded key still parses far enough to name
    /// its algorithm, and the damage only surfaces later as the platform's
    /// opaque "invalid key format" -- which is the failure this class exists to
    /// stop callers from having to diagnose.
    private static void requireWholeElement(byte[] der) {
        Cursor c = new Cursor(der);
        c.skip();
        if (c.hasMore()) {
            throw new CryptoException("malformed key: " + (der.length - c.position())
                    + " trailing bytes after the key");
        }
    }

    /// SEQUENCE { OID rsaEncryption, NULL } -- PKCS#1 keys always carry NULL
    /// parameters.
    private static byte[] rsaAlgorithmIdentifier() {
        return tlv(0x30, concat(tlv(0x06, OID_RSA), tlv(0x05, new byte[0])));
    }

    /// SEQUENCE { OID id-ecPublicKey, OID namedCurve }.
    private static byte[] ecAlgorithmIdentifier(byte[] curveOid) {
        return tlv(0x30, concat(tlv(0x06, OID_EC), tlv(0x06, curveOid)));
    }

    /// SEQUENCE { INTEGER 0, AlgorithmIdentifier, OCTET STRING privateKey }.
    private static byte[] wrapPkcs8(byte[] algorithmIdentifier, byte[] privateKey) {
        byte[] version = tlv(0x02, new byte[] {0});
        return tlv(0x30, concat(concat(version, algorithmIdentifier), tlv(0x04, privateKey)));
    }

    /// Converts a SEC1 `ECPrivateKey` --
    /// `SEQUENCE { INTEGER 1, OCTET STRING privateKey, [0] parameters, [1] publicKey }`
    /// -- into a PKCS#8 PrivateKeyInfo.
    ///
    /// The curve is named only in the `[0] parameters` field here, so it has to
    /// be lifted out into the PKCS#8 AlgorithmIdentifier. RFC 5915 section 3
    /// then says the field should not be repeated inside the wrapped key, so it
    /// is dropped rather than carried along -- which is also what
    /// `openssl pkcs8 -topk8` emits, byte for byte.
    private static byte[] sec1ToPkcs8(byte[] sec1) {
        Cursor c = new Cursor(sec1);
        c.enter(0x30);
        byte[] version = c.element();
        byte[] privateKey = c.element();
        byte[] curveOid = null;
        byte[] tail = new byte[0];
        while (c.hasMore()) {
            int tag = c.peek();
            byte[] element = c.element();
            if (tag == 0xA0) {
                Cursor parameters = new Cursor(element);
                parameters.enter(0xA0);
                curveOid = parameters.read(0x06);
            } else {
                tail = concat(tail, element);
            }
        }
        if (curveOid == null) {
            throw new CryptoException("SEC1 EC private key names no curve; re-export it as PKCS#8 with: "
                    + "openssl pkcs8 -topk8 -nocrypt -in key.pem -out key_pkcs8.pem");
        }
        byte[] inner = tlv(0x30, concat(concat(version, privateKey), tail));
        return wrapPkcs8(ecAlgorithmIdentifier(curveOid), inner);
    }

    private static byte[] tlv(int tag, byte[] content) {
        byte[] length = encodeLength(content.length);
        byte[] out = new byte[1 + length.length + content.length];
        out[0] = (byte) tag;
        System.arraycopy(length, 0, out, 1, length.length);
        System.arraycopy(content, 0, out, 1 + length.length, content.length);
        return out;
    }

    private static byte[] encodeLength(int length) {
        if (length < 0x80) {
            return new byte[] {(byte) length};
        }
        int bytes = 1;
        for (int v = length; v > 0xFF; v >>= 8) {
            bytes++;
        }
        byte[] out = new byte[bytes + 1];
        out[0] = (byte) (0x80 | bytes);
        for (int i = 0; i < bytes; i++) {
            out[out.length - 1 - i] = (byte) ((length >> (8 * i)) & 0xFF);
        }
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static boolean equal(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /// Renders an OID's content bytes as dotted decimal, so an unsupported
    /// algorithm names itself in the exception rather than being "invalid".
    private static String oidToString(byte[] oid) {
        if (oid.length == 0) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        int first = oid[0] & 0xFF;
        sb.append(first / 40).append('.').append(first % 40);
        long value = 0;
        for (int i = 1; i < oid.length; i++) {
            int b = oid[i] & 0xFF;
            value = (value << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                sb.append('.').append(value);
                value = 0;
            }
        }
        return sb.toString();
    }

    /// A read-only walk over a DER blob. Deliberately minimal: it only needs to
    /// step into SEQUENCE/context tags and read an OID, never to decode values.
    private static final class Cursor {
        private final byte[] der;
        private int pos;

        Cursor(byte[] der) {
            this.der = der;
        }

        /// Tag of the element at the cursor, without consuming it.
        int peek() {
            if (pos >= der.length) {
                throw new CryptoException("malformed key: truncated DER");
            }
            return der[pos] & 0xFF;
        }

        /// Consumes a constructed element's header, leaving the cursor on its
        /// first child.
        void enter(int expectedTag) {
            expect(expectedTag);
            length();
        }

        /// Consumes an element whole, contents included.
        void skip() {
            pos++;
            // length() advances pos past the length bytes, so its result has to
            // be read into a local first -- "pos += length()" would capture the
            // old pos and throw that advance away.
            int length = length();
            pos += length;
            if (pos > der.length) {
                throw new CryptoException("malformed key: truncated DER");
            }
        }

        /// Offset of the cursor within the blob.
        int position() {
            return pos;
        }

        /// True while the cursor has not reached the end of the blob.
        boolean hasMore() {
            return pos < der.length;
        }

        /// Consumes an element whole and returns its bytes, tag and length
        /// included, so it can be re-emitted verbatim.
        byte[] element() {
            int start = pos;
            skip();
            byte[] out = new byte[pos - start];
            System.arraycopy(der, start, out, 0, out.length);
            return out;
        }

        /// Consumes a primitive element and returns its contents.
        byte[] read(int expectedTag) {
            expect(expectedTag);
            int length = length();
            if (pos + length > der.length) {
                throw new CryptoException("malformed key: truncated DER");
            }
            byte[] out = new byte[length];
            System.arraycopy(der, pos, out, 0, length);
            pos += length;
            return out;
        }

        private void expect(int expectedTag) {
            if (peek() != expectedTag) {
                throw new CryptoException("malformed key: expected DER tag 0x"
                        + Integer.toHexString(expectedTag) + " but found 0x"
                        + Integer.toHexString(peek()));
            }
            pos++;
        }

        private int length() {
            if (pos >= der.length) {
                throw new CryptoException("malformed key: truncated DER");
            }
            int first = der[pos++] & 0xFF;
            if (first < 0x80) {
                return first;
            }
            int count = first & 0x7F;
            if (count == 0 || count > 4 || pos + count > der.length) {
                throw new CryptoException("malformed key: bad DER length");
            }
            int value = 0;
            for (int i = 0; i < count; i++) {
                value = (value << 8) | (der[pos++] & 0xFF);
            }
            if (value < 0) {
                throw new CryptoException("malformed key: bad DER length");
            }
            return value;
        }
    }
}
