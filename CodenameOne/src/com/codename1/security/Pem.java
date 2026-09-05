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
/// Validation stops at structure. This class checks what it walks, classifies
/// on, or rebuilds -- container shape, mandatory fields, element bounds -- so a
/// blob it cannot honestly convert is refused here, naming the problem rather
/// than leaving the platform to answer "invalid key format". It deliberately
/// does not check that the key material is usable. "The platform bridge rejects
/// this encoding" is not a workable line to draw, because the platform also
/// rejects a structurally perfect SubjectPublicKeyInfo whose modulus is one
/// byte; deciding whether a well-formed key is a valid RSA or EC key belongs to
/// the provider and cannot be reproduced portably here.
///
/// Nothing here is exposed publicly; the entry points are the `fromPem`
/// factories on [PublicKey] and [PrivateKey].
final class Pem {
    private static final int SHAPE_SPKI = 0;
    private static final int SHAPE_PKCS8 = 1;
    private static final int SHAPE_PKCS1_PUBLIC = 2;
    private static final int SHAPE_PKCS1_PRIVATE = 3;
    private static final int SHAPE_SEC1 = 4;
    private static final int SHAPE_UNKNOWN = 5;

    private static final String[] PUBLIC_LABELS = {"PUBLIC KEY", "RSA PUBLIC KEY"};
    private static final String[] PRIVATE_LABELS = {"PRIVATE KEY", "RSA PRIVATE KEY", "EC PRIVATE KEY"};

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
        byte[] der = select(pem, PUBLIC_LABELS, false);
        // The container is settled by the DER's own shape rather than by the
        // label, so unarmored input accepts exactly what armored input does.
        int shape = shapeOf(der);
        if (shape == SHAPE_SPKI) {
            return der;
        }
        if (shape == SHAPE_PKCS1_PUBLIC) {
            // PKCS#1 RSAPublicKey -> SubjectPublicKeyInfo. The BIT STRING needs
            // a leading zero byte for "no unused bits in the final octet".
            byte[] bitString = new byte[der.length + 1];
            System.arraycopy(der, 0, bitString, 1, der.length);
            return tlv(0x30, concat(rsaAlgorithmIdentifier(), tlv(0x03, bitString)));
        }
        if (shape == SHAPE_UNKNOWN) {
            throw new CryptoException("unrecognized public key container");
        }
        throw new CryptoException("this is a private key, not a public key");
    }

    /// Decodes `pem` to PKCS#8 PrivateKeyInfo DER.
    ///
    /// Accepts a `PRIVATE KEY` block, the older PKCS#1 `RSA PRIVATE KEY` and
    /// SEC1 `EC PRIVATE KEY` blocks (rewrapped here), or bare base64 with no
    /// armor at all.
    static byte[] toPkcs8(String pem) {
        byte[] der = select(pem, PRIVATE_LABELS, true);
        int shape = shapeOf(der);
        if (shape == SHAPE_PKCS8) {
            return normalizePkcs8(der);
        }
        if (shape == SHAPE_PKCS1_PRIVATE) {
            return wrapPkcs8(rsaAlgorithmIdentifier(), der);
        }
        if (shape == SHAPE_SEC1) {
            return sec1ToPkcs8(der);
        }
        if (shape == SHAPE_UNKNOWN) {
            throw new CryptoException("unrecognized private key container");
        }
        throw new CryptoException("this is a public key, not a private key");
    }

    /// Reads the algorithm OID out of an SPKI or PKCS#8 blob and maps it to the
    /// name the crypto bridge expects ([PublicKey#RSA] or [PublicKey#EC]).
    ///
    /// Both containers reach the AlgorithmIdentifier the same way once an
    /// optional leading version INTEGER is skipped, so one walk covers both.
    static String algorithm(byte[] der) {
        byte[] oid = algorithmIdentifier(der);
        if (equal(oid, OID_RSA)) {
            return PublicKey.RSA;
        }
        if (equal(oid, OID_EC)) {
            return PublicKey.EC;
        }
        throw new CryptoException("unsupported key algorithm OID " + oidToString(oid)
                + "; only RSA and EC keys are supported");
    }

    /// Runs the same AlgorithmIdentifier checks as [#algorithm] without
    /// insisting the OID be one this class recognizes.
    ///
    /// The `fromPem` overloads that take an algorithm name never call
    /// [#algorithm], so without this they skipped every structural check it
    /// performs: an SPKI whose AlgorithmIdentifier was an empty `30 00` came
    /// back as a usable key.
    static void requireAlgorithmIdentifier(byte[] der) {
        algorithmIdentifier(der);
    }

    /// Walks to the AlgorithmIdentifier, checks it, and returns its OID.
    ///
    /// SPKI and PKCS#8 reach it the same way once an optional leading version
    /// INTEGER is skipped, so one walk covers both.
    private static byte[] algorithmIdentifier(byte[] der) {
        Cursor c = new Cursor(der);
        c.enter(0x30);
        if (c.peek() == 0x02) {
            c.skip();
        }
        c.enter(0x30);
        byte[] oid = c.read(0x06);
        requireOid(oid);
        // AlgorithmIdentifier ::= SEQUENCE { algorithm OID, parameters ANY
        // DEFINED BY algorithm OPTIONAL } -- at most one parameters element,
        // and nothing after it. Stopping at the OID accepted { OID, NULL, NULL }.
        boolean hasParameters = c.hasMore();
        if (hasParameters) {
            c.skip();
        }
        if (c.hasMore()) {
            throw new CryptoException("malformed key: AlgorithmIdentifier carries more than one "
                    + "parameters field");
        }
        // RFC 4055 says rsaEncryption carries NULL parameters, but a key that
        // omits them is accepted by the platform, so refusing it here would
        // reject keys that work. Only the EC requirement is enforced, because
        // that one the platform does enforce.
        if (equal(oid, OID_EC) && !hasParameters) {
            throw new CryptoException("EC key names no curve: id-ecPublicKey requires "
                    + "ECParameters in the AlgorithmIdentifier");
        }
        return oid;
    }

    /// Classifies a key blob by walking the outer SEQUENCE's children, which
    /// is the only thing available for input that arrived as bare base64 and so
    /// carries no label.
    ///
    /// Every mandatory field is checked, not just the first: a blob holding a
    /// well-formed AlgorithmIdentifier and nothing else looks exactly like the
    /// start of an SPKI, and classifying on that alone returned a key with no
    /// public value in it, which then failed in the platform bridge with the
    /// opaque error this class exists to replace.
    private static int shapeOf(byte[] der) {
        Cursor c = new Cursor(der);
        c.enter(0x30);
        if (!c.hasMore()) {
            return SHAPE_UNKNOWN;
        }
        if (c.peek() == 0x30) {
            // SubjectPublicKeyInfo ::= SEQUENCE { AlgorithmIdentifier, BIT STRING }
            // -- exactly two fields, so the BIT STRING is consumed (which
            // bounds-checks its length) and nothing may follow it. Peeking at
            // the tag alone accepted a lone 0x03 byte and an empty 03 00.
            c.skip();
            if (!c.hasMore() || c.peek() != 0x03) {
                return SHAPE_UNKNOWN;
            }
            // A BIT STRING's first content octet counts unused bits, so a
            // length of one is metadata and no key material at all.
            byte[] bits = c.read(0x03);
            if (bits.length < 2 || c.hasMore()) {
                return SHAPE_UNKNOWN;
            }
            // That octet is a count of 0..7, and the bits it declares unused
            // must be zero. Checking only the length accepted "03 02 08 00"
            // and "03 02 07 FF", which both decoders refuse.
            int unused = bits[0] & 0xFF;
            if (unused > 7) {
                return SHAPE_UNKNOWN;
            }
            if (unused != 0 && (bits[bits.length - 1] & ((1 << unused) - 1)) != 0) {
                return SHAPE_UNKNOWN;
            }
            return SHAPE_SPKI;
        }
        if (c.peek() != 0x02) {
            return SHAPE_UNKNOWN;
        }
        // Kept because PKCS#1 reads it as a version below. It is the modulus in
        // an RSAPublicKey, so it can only be interpreted once the field count
        // has said which container this is.
        byte[] firstInteger = c.element();
        if (!c.hasMore()) {
            return SHAPE_UNKNOWN;
        }
        if (c.peek() == 0x30) {
            // PrivateKeyInfo ::= SEQUENCE { INTEGER, AlgorithmIdentifier, OCTET STRING }
            // RFC 5958 allows optional [0] attributes and [1] publicKey after
            // the privateKey, so trailing fields are not an error here.
            c.skip();
            if (!c.hasMore() || c.peek() != 0x04) {
                return SHAPE_UNKNOWN;
            }
            if (c.consume(0x04) == 0) {
                return SHAPE_UNKNOWN;
            }
            // Only [0] attributes and [1] publicKey may follow the privateKey,
            // in that order. Letting anything through carried the junk into the
            // key that was handed to the platform.
            if (c.hasMore() && c.peek() == 0xA0) {
                requireAttributes(c.element());
            }
            // RFC 5958's module is IMPLICIT TAGS, so [1] PublicKey -- a BIT
            // STRING -- keeps the primitive form and arrives as 0x81, not the
            // constructed 0xA1. Checking only for 0xA1 rejected the encoding
            // the RFC actually specifies while the platform accepted it. Both
            // spellings are taken; the field is dropped by the version-0
            // rewrite either way.
            boolean hasPublicKey = false;
            if (c.hasMore() && (c.peek() == 0x81 || c.peek() == 0xA1)) {
                c.skip();
                hasPublicKey = true;
            }
            if (c.hasMore()) {
                return SHAPE_UNKNOWN;
            }
            // RFC 5958 section 2 is deliberately asymmetric here: "If publicKey
            // is present, then version MUST be v2 (1). Otherwise version SHOULD
            // be v1 (0)." So this check is one-way on purpose.
            //
            // A version-0 container carrying a publicKey breaks the MUST, and
            // the version-0 path returns the bytes untouched, so it would reach
            // the platform with a field its own version forbids. That is
            // refused.
            //
            // Version 1 without a publicKey only breaks the SHOULD, and
            // refusing it would cost a key that works: measured, such a key is
            // accepted raw by JDK 17 and later, and normalizePkcs8 below
            // turns it into the canonical version-0 encoding -- byte for byte
            // the same key -- which JDK 11 accepts as well. Rejecting it would
            // be a regression on every supported runtime in exchange for
            // enforcing a SHOULD, so it is normalized instead.
            if (hasPublicKey && !isVersion(firstInteger, 1)) {
                return SHAPE_UNKNOWN;
            }
            return SHAPE_PKCS8;
        }
        if (c.peek() == 0x04) {
            // ECPrivateKey ::= SEQUENCE { INTEGER, OCTET STRING, [0], [1] }
            return c.consume(0x04) > 0 ? SHAPE_SEC1 : SHAPE_UNKNOWN;
        }
        if (c.peek() != 0x02) {
            return SHAPE_UNKNOWN;
        }
        // PKCS#1. RSAPublicKey is exactly { modulus, publicExponent };
        // RSAPrivateKey's nine INTEGER fields are all mandatory, and a
        // multi-prime key adds an otherPrimeInfos SEQUENCE after them.
        // A DER INTEGER always carries at least one content octet, so an empty
        // one is malformed however many of them there are.
        if (firstInteger.length <= 2) {
            return SHAPE_UNKNOWN;
        }
        int integers = 1;
        while (c.hasMore() && c.peek() == 0x02) {
            if (c.consume(0x02) == 0) {
                return SHAPE_UNKNOWN;
            }
            integers++;
        }
        if (integers == 2 && !c.hasMore()) {
            return SHAPE_PKCS1_PUBLIC;
        }
        if (integers != 9) {
            return SHAPE_UNKNOWN;
        }
        boolean multiPrime = false;
        if (c.hasMore()) {
            // The only thing that may follow the nine fields is a single
            // otherPrimeInfos SEQUENCE, for a multi-prime key. Accepting
            // whatever was there wrapped the junk into the PKCS#8 output.
            if (c.peek() != 0x30) {
                return SHAPE_UNKNOWN;
            }
            c.skip();
            if (c.hasMore()) {
                return SHAPE_UNKNOWN;
            }
            multiPrime = true;
        }
        // RFC 3447: version 0 is a two-prime key and version 1 a multi-prime
        // one, so the version and the presence of otherPrimeInfos have to
        // agree. Counting the field and never reading it let any version
        // through to be rewrapped as PKCS#8.
        if (!isVersion(firstInteger, multiPrime ? 1 : 0)) {
            return SHAPE_UNKNOWN;
        }
        return SHAPE_PKCS1_PRIVATE;
    }

    /// Decodes the first armored block whose label is one of `wanted`.
    ///
    /// The first block is not necessarily the key: `openssl ecparam -genkey`
    /// writes an `EC PARAMETERS` block ahead of the `EC PRIVATE KEY` one, and
    /// taking whatever came first rejected that file even though it holds
    /// exactly the key that was asked for. Input carrying no armor at all is
    /// decoded whole.
    private static byte[] select(String pem, String[] wanted, boolean privateKey) {
        if (pem == null) {
            throw new CryptoException("pem must not be null");
        }
        if (pem.indexOf(BEGIN) < 0) {
            return body(pem, null);
        }
        StringBuilder seen = new StringBuilder();
        int at = 0;
        while (true) {
            int begin = pem.indexOf(BEGIN, at);
            if (begin < 0) {
                break;
            }
            int labelStart = begin + BEGIN.length();
            int labelEnd = pem.indexOf(DASHES, labelStart);
            if (labelEnd < 0) {
                throw new CryptoException("malformed PEM: -----BEGIN line is not terminated");
            }
            String label = pem.substring(labelStart, labelEnd).trim();
            for (int i = 0; i < wanted.length; i++) {
                if (wanted[i].equals(label)) {
                    return body(pem.substring(begin), label);
                }
            }
            if (seen.length() > 0) {
                seen.append(", ");
            }
            seen.append(label);
            at = labelEnd + DASHES.length();
        }
        String labels = seen.toString();
        if (privateKey && labels.indexOf("ENCRYPTED PRIVATE KEY") >= 0) {
            throw new CryptoException("this private key is passphrase-encrypted; decrypt it first with: "
                    + "openssl pkcs8 -topk8 -nocrypt -in key.pem -out key_pkcs8.pem");
        }
        if (!privateKey && labels.indexOf("CERTIFICATE") >= 0) {
            throw new CryptoException("this is a certificate, not a public key; "
                    + "extract the key with: openssl x509 -in cert.pem -pubkey -noout");
        }
        throw new CryptoException("no " + (privateKey ? "private" : "public")
                + " key block in this PEM; it holds: " + labels);
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
        requireStrictBase64(base64);
        // Base64.decode tolerates the line breaks but nothing else, and answers
        // null rather than throwing when it meets a character it cannot map.
        byte[] der = Base64.decode(StringUtil.getBytes(base64));
        if (der == null || der.length == 0) {
            throw new CryptoException("malformed PEM: the body is not valid base64");
        }
        requireWholeElement(der);
        return der;
    }

    /// Checks the body is well-formed base64 before it is decoded.
    ///
    /// [Base64#decode] stops at the first `=` and never looks at what follows,
    /// so a body of `<valid base64>=<anything>` decodes to the intact prefix and
    /// is accepted. A spliced or corrupted file would load silently on the
    /// strength of its first half, so padding is required to be the last thing
    /// in the body.
    private static void requireStrictBase64(String base64) {
        int symbols = 0;
        int padding = 0;
        for (int i = 0; i < base64.length(); i++) {
            char c = base64.charAt(i);
            if (c == '\n' || c == '\r' || c == ' ' || c == '\t') {
                continue;
            }
            if (c == '=') {
                padding++;
                continue;
            }
            if (padding > 0) {
                throw new CryptoException("malformed PEM: content follows the base64 padding");
            }
            if (!isBase64(c)) {
                throw new CryptoException("malformed PEM: '" + c + "' is not a base64 character");
            }
            symbols++;
        }
        if (padding > 2) {
            throw new CryptoException("malformed PEM: too much base64 padding");
        }
        if (symbols == 0 || ((symbols + padding) & 3) != 0) {
            throw new CryptoException("malformed PEM: the base64 body is truncated");
        }
    }

    private static boolean isBase64(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9') || c == '+' || c == '/';
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

    /// `SEQUENCE { OID id-ecPublicKey, ECParameters }`.
    ///
    /// `ecParameters` is passed through whole rather than rebuilt, because
    /// ECParameters is a CHOICE: usually a named-curve OID, but a complete
    /// SEQUENCE describing the curve when the key was written with explicit
    /// parameters. Both belong in this field verbatim.
    private static byte[] ecAlgorithmIdentifier(byte[] ecParameters) {
        return tlv(0x30, concat(tlv(0x06, OID_EC), ecParameters));
    }

    /// SEQUENCE { INTEGER 0, AlgorithmIdentifier, OCTET STRING privateKey }.
    private static byte[] wrapPkcs8(byte[] algorithmIdentifier, byte[] privateKey) {
        byte[] version = tlv(0x02, new byte[] {0});
        return tlv(0x30, concat(concat(version, algorithmIdentifier), tlv(0x04, privateKey)));
    }

    /// Rewrites an RFC 5958 version-1 `OneAsymmetricKey` as the version-0
    /// `PrivateKeyInfo` of RFC 5208, dropping the `[1] publicKey` that only the
    /// later version allows.
    ///
    /// JDK 11 refuses a version-1 key outright ("version mismatch: supported 00,
    /// parsed 01") while 17 and later accept it, so passing one straight through
    /// works on some supported runtimes and not others. The private key itself
    /// is untouched and the public half is derivable from it, so the version-0
    /// form every provider understands loses nothing. A key that is already
    /// version 0 is returned byte for byte.
    private static byte[] normalizePkcs8(byte[] der) {
        Cursor c = new Cursor(der);
        c.enter(0x30);
        byte[] version = c.element();
        if (isVersion(version, 0)) {
            return der;
        }
        if (!isVersion(version, 1)) {
            // Rewriting anything that merely is not version 0 would launder a
            // corrupt header into a well-formed key: a version of 2, or an
            // empty INTEGER, came back as the canonical version-0 encoding and
            // was accepted.
            throw new CryptoException("unsupported PKCS#8 version; only 0 (RFC 5208) and "
                    + "1 (RFC 5958) are defined");
        }
        byte[] algorithm = c.element();
        byte[] privateKey = c.element();
        byte[] attributes = new byte[0];
        if (c.hasMore() && c.peek() == 0xA0) {
            // [0] attributes is legal in version 0 as well, so it is kept
            attributes = c.element();
        }
        if (c.hasMore() && (c.peek() == 0x81 || c.peek() == 0xA1)) {
            // the [1] publicKey that version 0 has no room for
            c.skip();
        }
        if (c.hasMore()) {
            // shapeOf() has already refused anything else, but this method
            // rebuilds a key from what it reads, so it does not lean on that:
            // silently dropping a leftover would launder a spliced container.
            throw new CryptoException("malformed PKCS#8 key: unexpected field 0x"
                    + Integer.toHexString(c.peek()));
        }
        return tlv(0x30, concat(concat(tlv(0x02, new byte[] {0}), algorithm),
                concat(privateKey, attributes)));
    }

    /// Checks an OBJECT IDENTIFIER's contents.
    ///
    /// [Cursor#read] verifies the tag and the bounds, nothing more, so an empty
    /// or unterminated OID reached the explicit-algorithm overloads intact --
    /// the auto-detecting path only caught them by accident, because such an
    /// OID matches neither of the two it knows.
    private static void requireOid(byte[] oid) {
        if (oid.length == 0) {
            throw new CryptoException("malformed key: the algorithm OID is empty");
        }
        if ((oid[oid.length - 1] & 0x80) != 0) {
            throw new CryptoException("malformed key: the algorithm OID ends mid-value");
        }
        // Each sub-identifier is base-128, most significant group first, and a
        // leading 0x80 group would be a redundant zero.
        boolean startOfValue = true;
        for (int i = 0; i < oid.length; i++) {
            if (startOfValue && oid[i] == (byte) 0x80) {
                throw new CryptoException("malformed key: the algorithm OID is not minimally encoded");
            }
            startOfValue = (oid[i] & 0x80) == 0;
        }
    }

    /// Checks a PKCS#8 `[0] attributes` wrapper.
    ///
    /// `Attributes ::= SET OF Attribute`, and `Attribute ::= SEQUENCE { type
    /// OBJECT IDENTIFIER, values SET OF AttributeValue }`. Skipping the wrapper
    /// whole let `A0 02 05 00` through -- which the JDK tolerates and OpenSSL
    /// does not, so the key worked on JavaSE and Android and failed on the Linux
    /// port. Checking only that each child is a SEQUENCE is not enough either:
    /// OpenSSL also refuses `A0 04 30 02 05 00`, whose child is a SEQUENCE but
    /// holds a NULL rather than the type and values it must.
    private static void requireAttributes(byte[] element) {
        Cursor c = new Cursor(element);
        c.enter(0xA0);
        while (c.hasMore()) {
            if (c.peek() != 0x30) {
                throw new CryptoException("malformed PKCS#8 key: attributes hold a 0x"
                        + Integer.toHexString(c.peek()) + " where an Attribute SEQUENCE belongs");
            }
            Cursor attribute = new Cursor(c.element());
            attribute.enter(0x30);
            attribute.read(0x06);
            if (!attribute.hasMore() || attribute.peek() != 0x31) {
                throw new CryptoException("malformed PKCS#8 key: an Attribute has no values SET");
            }
            attribute.skip();
            if (attribute.hasMore()) {
                throw new CryptoException("malformed PKCS#8 key: an Attribute has more than "
                        + "a type and a values SET");
            }
        }
    }

    /// True when `element` is the DER for `INTEGER value` as a version field.
    private static boolean isVersion(byte[] element, int value) {
        return element.length == 3 && element[0] == 0x02 && element[1] == 0x01
                && element[2] == (byte) value;
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
        if (!isVersion(version, 1)) {
            // RFC 5915 defines only version 1; copying any other value into the
            // rewrapped key left the provider to reject it on first use.
            throw new CryptoException("SEC1 EC private key must be version 1");
        }
        byte[] privateKey = c.element();
        byte[] ecParameters = null;
        byte[] publicKey = new byte[0];
        // ECPrivateKey ends with at most one [0] parameters and one [1]
        // publicKey, in that order. Accepting anything else here meant a
        // malformed key could carry thousands of junk children, and appending
        // each one to a growing array copied the whole accumulation every time.
        if (c.hasMore() && c.peek() == 0xA0) {
            // [0] wraps ECParameters, which is a CHOICE -- a named-curve OID
            // or, for a key written with explicit parameters, a whole SEQUENCE
            // describing the curve. Take it verbatim: reading an OID out of it
            // would reject the explicit form, which is a valid SEC1 key.
            Cursor parameters = new Cursor(c.element());
            parameters.enter(0xA0);
            ecParameters = parameters.element();
            if (parameters.hasMore()) {
                // ECParameters is a CHOICE, so the wrapper holds exactly one
                // value. Reading the first and ignoring the rest dropped the
                // extra bytes and returned a well-formed key built from a
                // spliced container.
                throw new CryptoException("malformed SEC1 EC private key: "
                        + "more than one value in the parameters field");
            }
        }
        if (c.hasMore() && c.peek() == 0xA1) {
            publicKey = c.element();
            // The wrapper is copied into the rewrapped key, so what is inside
            // it has to be checked here or it is never checked at all.
            Cursor wrapper = new Cursor(publicKey);
            wrapper.enter(0xA1);
            if (wrapper.consume(0x03) == 0 || wrapper.hasMore()) {
                throw new CryptoException("malformed SEC1 EC private key: the public key field "
                        + "is not a single BIT STRING");
            }
        }
        if (c.hasMore()) {
            throw new CryptoException("malformed SEC1 EC private key: unexpected field 0x"
                    + Integer.toHexString(c.peek()));
        }
        if (ecParameters == null) {
            throw new CryptoException("SEC1 EC private key names no curve; re-export it as PKCS#8 with: "
                    + "openssl pkcs8 -topk8 -nocrypt -in key.pem -out key_pkcs8.pem");
        }
        // RFC 5915: the parameters field is not repeated inside PKCS#8, the
        // AlgorithmIdentifier carries the curve instead.
        byte[] inner = tlv(0x30, concat(concat(version, privateKey), publicKey));
        return wrapPkcs8(ecAlgorithmIdentifier(ecParameters), inner);
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
    ///
    /// Every read is bounded by `end`, the end of the element the cursor is
    /// currently inside, which [#enter] narrows as it descends. Bounding
    /// against the whole blob instead would let a read escape its parent: an
    /// AlgorithmIdentifier declared as `30 00` followed by an OID that really
    /// belongs to the enclosing SEQUENCE would be walked as though the OID were
    /// its own, and the malformed key would be reported as valid RSA. The walk
    /// only ever descends, so `end` never has to be restored.
    private static final class Cursor {
        private final byte[] der;
        private int pos;
        private int end;

        Cursor(byte[] der) {
            this.der = der;
            this.end = der.length;
        }

        /// Tag of the element at the cursor, without consuming it.
        int peek() {
            if (pos >= end) {
                throw new CryptoException("malformed key: truncated DER");
            }
            return der[pos] & 0xFF;
        }

        /// Consumes a constructed element's header, leaving the cursor on its
        /// first child and narrowing the walk to that element's contents.
        void enter(int expectedTag) {
            expect(expectedTag);
            int length = length();
            requireRoom(length);
            end = pos + length;
        }

        /// Consumes an element whole, contents included.
        void skip() {
            peek();
            pos++;
            // length() advances pos past the length bytes, so its result has to
            // be read into a local first -- "pos += length()" would capture the
            // old pos and throw that advance away.
            int length = length();
            requireRoom(length);
            pos += length;
        }

        /// Offset of the cursor within the blob.
        int position() {
            return pos;
        }

        /// True while the cursor has not reached the end of the element it is
        /// inside.
        boolean hasMore() {
            return pos < end;
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

        /// Consumes a primitive element and returns the length of its
        /// contents, without copying them -- for validating a field is present
        /// and well formed when its value is not otherwise needed.
        int consume(int expectedTag) {
            expect(expectedTag);
            int length = length();
            requireRoom(length);
            pos += length;
            return length;
        }

        /// Consumes a primitive element and returns its contents.
        byte[] read(int expectedTag) {
            expect(expectedTag);
            int length = length();
            requireRoom(length);
            byte[] out = new byte[length];
            System.arraycopy(der, pos, out, 0, length);
            pos += length;
            return out;
        }

        /// Checks `length` bytes are available before the end of the enclosing
        /// element. Phrased as a subtraction rather than "pos + length > end":
        /// a declared length near Integer.MAX_VALUE makes that sum overflow to
        /// a negative number, which slips past the check and reaches an
        /// allocation.
        private void requireRoom(int length) {
            if (length > end - pos) {
                throw new CryptoException("malformed key: truncated DER");
            }
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
            if (pos >= end) {
                throw new CryptoException("malformed key: truncated DER");
            }
            int first = der[pos++] & 0xFF;
            if (first < 0x80) {
                return first;
            }
            int count = first & 0x7F;
            if (count == 0 || count > 4 || count > end - pos) {
                throw new CryptoException("malformed key: bad DER length");
            }
            // DER requires the shortest possible length encoding. BER does not,
            // and the difference is not academic: JDK 11 and 17 refuse a
            // redundant length outright while 21 and later accept it, so a key
            // encoded this way works on some supported runtimes and not others.
            if (der[pos] == 0) {
                throw new CryptoException("malformed key: non-minimal DER length, leading zero octet");
            }
            int value = 0;
            for (int i = 0; i < count; i++) {
                value = (value << 8) | (der[pos++] & 0xFF);
            }
            if (value < 0) {
                throw new CryptoException("malformed key: bad DER length");
            }
            if (value < 0x80) {
                throw new CryptoException("malformed key: non-minimal DER length, long form used for "
                        + value);
            }
            return value;
        }
    }
}
