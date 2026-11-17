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
package com.codename1.backend.security.saml2;

import com.codename1.backend.Crypto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Decrypts an `EncryptedAssertion`: XML Encryption 1.1, with the key wrapped
/// by RSA-OAEP and the assertion by AES-GCM or AES-CBC.
///
/// What is accepted:
///
/// - Key transport `rsa-oaep-mgf1p` and `xmlenc11#rsa-oaep`, with any SHA-1 or
///   SHA-2 digest and MGF1. RSA PKCS#1 v1.5 (`rsa-1_5`) is refused: it is open
///   to Bleichenbacher's attack, and every identity provider in use offers OAEP.
/// - Content `aes128-gcm`, `aes192-gcm`, `aes256-gcm`, `aes128-cbc`,
///   `aes192-cbc` and `aes256-cbc`. CBC is still the default of ADFS, Okta and
///   Shibboleth, so it cannot be refused; GCM is the one to choose.
/// - The `EncryptedKey` inside the `EncryptedData`'s `KeyInfo`, or beside the
///   `EncryptedData` in the `EncryptedAssertion` (where Okta and ADFS put it).
///
/// Every failure -- no key fits, the key is the wrong size, the padding is
/// wrong, the tag does not verify, the plaintext is not an assertion -- is one
/// exception with one message. CBC carries no integrity of its own, and an
/// answer that told those apart would be the oracle of Jager and Somorovsky's
/// attack on XML Encryption. The page the browser sees is in any case the same
/// redirect to the login page for every refusal.
final class XmlEncryption {
    static final String XENC = "http://www.w3.org/2001/04/xmlenc#";
    static final String XENC11 = "http://www.w3.org/2009/xmlenc11#";

    private static final String FAILED = "The encrypted assertion could not be decrypted";

    private XmlEncryption() {
    }

    /// The assertion `encrypted` holds, parsed as if it sat where the
    /// `EncryptedAssertion` does: the namespaces in scope there are in scope
    /// for it.
    ///
    /// @param keys the PKCS#8 keys to try, in order
    static Xml.Element decrypt(Xml.Element encrypted, List<byte[]> keys) {
        if (keys.isEmpty()) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.DECRYPTION_ERROR,
                    "The assertion is encrypted and this registration has no decryption key; "
                    + "configure decryption.private-key-location and "
                    + "decryption.certificate-location");
        }
        Xml.Element data = encrypted.child(XENC, "EncryptedData");
        if (data == null) {
            throw failed();
        }
        Xml.Element method = data.child(XENC, "EncryptionMethod");
        String algorithm = method == null ? null : method.attribute("Algorithm");
        if (algorithm == null) {
            throw failed();
        }
        int keyLength = contentKeyLength(algorithm);
        boolean gcm = algorithm.startsWith(XENC11);
        List<Xml.Element> wrapped = new ArrayList<Xml.Element>();
        Xml.Element info = data.child(XmlSignature.DSIG, "KeyInfo");
        if (info != null) {
            wrapped.addAll(info.children(XENC, "EncryptedKey"));
        }
        wrapped.addAll(encrypted.children(XENC, "EncryptedKey"));
        if (wrapped.isEmpty()) {
            throw failed();
        }
        byte[] cipher = cipherValue(data);
        byte[] plain = null;
        for (Xml.Element ek : wrapped) {
            for (byte[] key : keys) {
                byte[] cek = unwrap(ek, key);
                if (cek != null && cek.length == keyLength) {
                    plain = gcm ? gcm(cek, cipher) : cbc(cek, cipher);
                    if (plain != null) {
                        break;
                    }
                }
            }
            if (plain != null) {
                break;
            }
        }
        if (plain == null) {
            throw failed();
        }
        return parseInContext(encrypted, plain);
    }

    private static int contentKeyLength(String algorithm) {
        if (algorithm != null) {
            if (algorithm.equals(XENC + "aes128-cbc") || algorithm.equals(XENC11 + "aes128-gcm")) {
                return 16;
            }
            if (algorithm.equals(XENC + "aes192-cbc") || algorithm.equals(XENC11 + "aes192-gcm")) {
                return 24;
            }
            if (algorithm.equals(XENC + "aes256-cbc") || algorithm.equals(XENC11 + "aes256-gcm")) {
                return 32;
            }
        }
        throw new Saml2AuthenticationException(Saml2ErrorCodes.DECRYPTION_ERROR,
                "The assertion is encrypted with " + algorithm + "; AES-GCM or AES-CBC is "
                + "required");
    }

    /// The content key, or null when this key does not open it.
    private static byte[] unwrap(Xml.Element ek, byte[] privateKey) {
        Xml.Element method = ek.child(XENC, "EncryptionMethod");
        String algorithm = method == null ? null : method.attribute("Algorithm");
        String digest;
        String mgf;
        if ((XENC + "rsa-oaep-mgf1p").equals(algorithm)) {
            digest = oaepDigest(method);
            mgf = Crypto.SHA1;
        } else if ((XENC11 + "rsa-oaep").equals(algorithm)) {
            digest = oaepDigest(method);
            Xml.Element m = method.child(XENC11, "MGF");
            mgf = m == null ? Crypto.SHA1 : mgf(m.attribute("Algorithm"));
        } else {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.DECRYPTION_ERROR,
                    "The assertion's key is wrapped with " + algorithm + "; RSA-OAEP is "
                    + "required, and RSA PKCS#1 v1.5 is refused");
        }
        Xml.Element params = method.child(XENC, "OAEPparams");
        if (params != null && params.textOnly().trim().length() > 0) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.DECRYPTION_ERROR,
                    "OAEP parameters (a label) are not supported");
        }
        try {
            return Crypto.rsaOaepDecrypt(digest, mgf, privateKey, cipherValue(ek));
        } catch (IOException wrongKey) {
            return null;
        }
    }

    private static String oaepDigest(Xml.Element method) {
        Xml.Element d = method.child(XmlSignature.DSIG, "DigestMethod");
        if (d == null) {
            return Crypto.SHA1;
        }
        String uri = d.attribute("Algorithm");
        if ("http://www.w3.org/2000/09/xmldsig#sha1".equals(uri)) {
            return Crypto.SHA1;
        }
        if ("http://www.w3.org/2001/04/xmlenc#sha256".equals(uri)) {
            return Crypto.SHA256;
        }
        if ("http://www.w3.org/2001/04/xmldsig-more#sha384".equals(uri)) {
            return Crypto.SHA384;
        }
        if ("http://www.w3.org/2001/04/xmlenc#sha512".equals(uri)) {
            return Crypto.SHA512;
        }
        throw new Saml2AuthenticationException(Saml2ErrorCodes.DECRYPTION_ERROR,
                "Unsupported OAEP digest " + uri);
    }

    private static String mgf(String uri) {
        if ((XENC11 + "mgf1sha1").equals(uri)) {
            return Crypto.SHA1;
        }
        if ((XENC11 + "mgf1sha256").equals(uri)) {
            return Crypto.SHA256;
        }
        if ((XENC11 + "mgf1sha384").equals(uri)) {
            return Crypto.SHA384;
        }
        if ((XENC11 + "mgf1sha512").equals(uri)) {
            return Crypto.SHA512;
        }
        throw new Saml2AuthenticationException(Saml2ErrorCodes.DECRYPTION_ERROR,
                "Unsupported OAEP mask generation " + uri);
    }

    private static byte[] cipherValue(Xml.Element holder) {
        Xml.Element cd = holder.child(XENC, "CipherData");
        Xml.Element cv = cd == null ? null : cd.child(XENC, "CipherValue");
        byte[] value = cv == null ? null : XmlSignature.base64(cv.textOnly());
        if (value == null) {
            throw failed();
        }
        return value;
    }

    /// AES-GCM: a 12 byte IV, then the ciphertext and its 16 byte tag.
    private static byte[] gcm(byte[] key, byte[] data) {
        if (data.length < 12 + 16) {
            return null;
        }
        byte[] iv = new byte[12];
        byte[] sealed = new byte[data.length - 12];
        System.arraycopy(data, 0, iv, 0, 12);
        System.arraycopy(data, 12, sealed, 0, sealed.length);
        try {
            return Crypto.aesGcmDecrypt(key, iv, null, sealed);
        } catch (IOException tampered) {
            return null;
        }
    }

    /// AES-CBC: a 16 byte IV, then whole blocks, padded as XML Encryption pads
    /// (the last byte is the count, the bytes before it are arbitrary).
    private static byte[] cbc(byte[] key, byte[] data) {
        if (data.length < 32 || data.length % 16 != 0) {
            return null;
        }
        byte[] iv = new byte[16];
        byte[] blocks = new byte[data.length - 16];
        System.arraycopy(data, 0, iv, 0, 16);
        System.arraycopy(data, 16, blocks, 0, blocks.length);
        byte[] padded;
        try {
            padded = Crypto.aesCbcDecryptBlocks(key, iv, blocks);
        } catch (IOException err) {
            return null;
        }
        int pad = padded[padded.length - 1] & 0xff;
        if (pad < 1 || pad > 16) {
            return null;
        }
        byte[] out = new byte[padded.length - pad];
        System.arraycopy(padded, 0, out, 0, out.length);
        return out;
    }

    /// Parses `plain` as the content of an element that declares every
    /// namespace in scope at `at`, and answers the one element it must be.
    private static Xml.Element parseInContext(Xml.Element at, byte[] plain) {
        Map<String, String> scope = new LinkedHashMap<String, String>();
        for (Xml.Element e = at; e != null; e = e.parent) {
            for (Map.Entry<String, String> d : e.declared.entrySet()) {
                if (!scope.containsKey(d.getKey())) {
                    scope.put(d.getKey(), d.getValue());
                }
            }
        }
        String text;
        try {
            text = new String(plain, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw failed();
        }
        if (text.startsWith("<?xml")) {
            int end = text.indexOf("?>");
            if (end < 0) {
                throw failed();
            }
            text = text.substring(end + 2);
        }
        StringBuilder doc = new StringBuilder(text.length() + 256);
        doc.append("<cn1-decrypted");
        for (Map.Entry<String, String> d : scope.entrySet()) {
            doc.append(d.getKey().length() == 0 ? " xmlns" : " xmlns:" + d.getKey())
                    .append("=\"").append(Xml.escape(d.getValue())).append('"');
        }
        doc.append('>').append(text).append("</cn1-decrypted>");
        Xml.Element wrapper;
        try {
            wrapper = Xml.parse(doc.toString());
        } catch (Saml2AuthenticationException err) {
            throw failed();
        }
        List<Xml.Element> inside = wrapper.elements();
        if (inside.size() != 1 || !inside.get(0).is(Saml2ResponseValidator.ASSERTION, "Assertion")
                || wrapper.text().trim().length() > 0) {
            throw failed();
        }
        return inside.get(0);
    }

    private static Saml2AuthenticationException failed() {
        return new Saml2AuthenticationException(Saml2ErrorCodes.DECRYPTION_ERROR, FAILED);
    }
}
