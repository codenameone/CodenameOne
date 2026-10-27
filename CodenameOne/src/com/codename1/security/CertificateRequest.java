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
package com.codename1.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/// A PKCS#10 certificate signing request (RFC 2986): what a device sends a
/// certificate authority to get the client certificate it then presents over
/// mutual TLS.
///
/// The key pair never leaves the device. The request carries the public key and
/// the names the certificate is for, signed with the private key to prove the
/// device holds it:
///
/// ```java
/// KeyPair keys = KeyGenerator.ec(256);
/// byte[] csr = new CertificateRequest(keys.getPublicKey().getEncoded(), "EC")
///         .commonName("thermostat-42")
///         .organization("Example Devices")
///         .dnsName("thermostat-42.local")
///         .sign(data -> Signature.sign(Signature.SHA256_WITH_ECDSA, keys.getPrivateKey(), data));
/// String pem = CertificateRequest.toPem(csr);
/// ```
///
/// The class only encodes. The signature comes from whatever holds the key: a
/// hardware key store signs with its own call, and a headless device on the
/// backend with `com.codename1.backend.Crypto`:
///
/// ```java {class="cn1-no-try"}
/// byte[] key = Crypto.generateEcKey(Crypto.P256);
/// byte[] csr = new CertificateRequest(Der.publicKeyOf(key), "EC")
///         .commonName(deviceId)
///         .sign(data -> Crypto.sign(Crypto.ES256, key, data));
/// ```
///
/// Requests are signed with SHA-256: ECDSA for an `EC` key, PKCS#1
/// v1.5 for an `RSA` key.
@com.codename1.impl.SharedWithBackend
public final class CertificateRequest {
    /// Signs the encoded request body with the private key matching the request's
    /// public key, using [#getSignatureAlgorithm()].
    public interface Signer {
        /// Signs `data`.
        ///
        /// @param data the DER CertificationRequestInfo
        /// @return the signature; for ECDSA the DER `SEQUENCE { r, s }` the
        /// platform's `SHA256withECDSA` produces
        /// @throws IOException when the key cannot sign
        byte[] sign(byte[] data) throws IOException;
    }

    private static final int[] OID_COMMON_NAME = {2, 5, 4, 3};
    private static final int[] OID_COUNTRY = {2, 5, 4, 6};
    private static final int[] OID_ORGANIZATION = {2, 5, 4, 10};
    private static final int[] OID_UNIT = {2, 5, 4, 11};
    private static final int[] OID_EXTENSION_REQUEST = {1, 2, 840, 113549, 1, 9, 14};
    private static final int[] OID_SUBJECT_ALT_NAME = {2, 5, 29, 17};
    private static final int[] OID_ECDSA_SHA256 = {1, 2, 840, 10045, 4, 3, 2};
    private static final int[] OID_RSA_SHA256 = {1, 2, 840, 113549, 1, 1, 11};

    private final byte[] publicKey;
    private final boolean ec;
    private final List<byte[]> subject = new ArrayList<byte[]>();
    private final List<byte[]> altNames = new ArrayList<byte[]>();

    /// Starts a request for a public key.
    ///
    /// @param subjectPublicKeyInfo the X.509 DER public key, as
    /// `PublicKey.getEncoded()` returns it
    /// @param keyAlgorithm `EC` or `RSA`
    public CertificateRequest(byte[] subjectPublicKeyInfo, String keyAlgorithm) {
        if (subjectPublicKeyInfo == null || subjectPublicKeyInfo.length == 0) {
            throw new IllegalArgumentException("No public key");
        }
        if ("EC".equals(keyAlgorithm)) {
            ec = true;
        } else if ("RSA".equals(keyAlgorithm)) {
            ec = false;
        } else {
            throw new IllegalArgumentException("Key algorithm must be EC or RSA: " + keyAlgorithm);
        }
        publicKey = new byte[subjectPublicKeyInfo.length];
        System.arraycopy(subjectPublicKeyInfo, 0, publicKey, 0, publicKey.length);
    }

    /// The signature algorithm the [Signer] has to use: `SHA256withECDSA` or
    /// `SHA256withRSA`.
    ///
    /// @return the JCA name of the algorithm
    public String getSignatureAlgorithm() {
        return ec ? "SHA256withECDSA" : "SHA256withRSA";
    }

    /// Adds the subject's common name (CN), typically the device identity.
    ///
    /// @param name the name
    /// @return this request
    public CertificateRequest commonName(String name) {
        return attribute(OID_COMMON_NAME, utf8(name));
    }

    /// Adds the subject's organization (O).
    ///
    /// @param name the organization
    /// @return this request
    public CertificateRequest organization(String name) {
        return attribute(OID_ORGANIZATION, utf8(name));
    }

    /// Adds the subject's organizational unit (OU).
    ///
    /// @param name the unit
    /// @return this request
    public CertificateRequest organizationalUnit(String name) {
        return attribute(OID_UNIT, utf8(name));
    }

    /// Adds the subject's country (C).
    ///
    /// @param code the two-letter ISO 3166 code
    /// @return this request
    public CertificateRequest country(String code) {
        if (code == null || code.length() != 2 || !printable(code)) {
            throw new IllegalArgumentException("A country is a two-letter code: " + code);
        }
        return attribute(OID_COUNTRY, tlv(0x13, ascii(code)));
    }

    /// Asks for a DNS name in the certificate's subject alternative names, which
    /// is what a TLS client checks when the device is the server.
    ///
    /// @param name the host name, ASCII
    /// @return this request
    public CertificateRequest dnsName(String name) {
        if (name == null || name.length() == 0 || !printable(name)) {
            throw new IllegalArgumentException("Not an ASCII host name: " + name);
        }
        altNames.add(tlv(0x82, ascii(name)));
        return this;
    }

    /// Asks for an IPv4 address in the subject alternative names.
    ///
    /// @param address dotted decimal, such as `192.168.1.20`
    /// @return this request
    public CertificateRequest ipAddress(String address) {
        byte[] octets = new byte[4];
        String[] parts = address == null ? new String[0] : split(address);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Not an IPv4 address: " + address);
        }
        for (int i = 0; i < 4; i++) {
            int v = -1;
            if (parts[i].length() > 0 && parts[i].length() <= 3) {
                v = 0;
                for (int j = 0; j < parts[i].length(); j++) {
                    char c = parts[i].charAt(j);
                    v = c >= '0' && c <= '9' ? v * 10 + c - '0' : 256;
                }
            }
            if (v < 0 || v > 255) {
                throw new IllegalArgumentException("Not an IPv4 address: " + address);
            }
            octets[i] = (byte) v;
        }
        altNames.add(tlv(0x87, octets));
        return this;
    }

    /// The DER CertificationRequestInfo: the part of the request that is signed.
    ///
    /// @return the encoded bytes
    public byte[] getInfo() {
        byte[] attributes;
        if (altNames.isEmpty()) {
            attributes = tlv(0xa0, new byte[0]);
        } else {
            byte[] san = sequence(altNames);
            byte[] extension = seq(oid(OID_SUBJECT_ALT_NAME), tlv(0x04, san));
            byte[] extensions = seq(extension);
            byte[] attribute = seq(oid(OID_EXTENSION_REQUEST), tlv(0x31, extensions));
            attributes = tlv(0xa0, attribute);
        }
        List<byte[]> rdns = new ArrayList<byte[]>();
        for (byte[] atv : subject) {
            rdns.add(tlv(0x31, atv));
        }
        return seq(tlv(0x02, new byte[]{0}), sequence(rdns), publicKey, attributes);
    }

    /// Encodes the request with a signature made elsewhere over [#getInfo()].
    ///
    /// @param signature the signature bytes
    /// @return the DER request
    public byte[] encode(byte[] signature) {
        if (signature == null || signature.length == 0) {
            throw new IllegalArgumentException("No signature");
        }
        byte[] algorithm = ec ? seq(oid(OID_ECDSA_SHA256)) : seq(oid(OID_RSA_SHA256), new byte[]{0x05, 0});
        byte[] bits = new byte[signature.length + 1];
        System.arraycopy(signature, 0, bits, 1, signature.length);
        return seq(getInfo(), algorithm, tlv(0x03, bits));
    }

    /// Signs and encodes the request.
    ///
    /// @param signer signs with the private key
    /// @return the DER request
    /// @throws IOException when the signer fails
    public byte[] sign(Signer signer) throws IOException {
        return encode(signer.sign(getInfo()));
    }

    /// Wraps a DER request in the PEM form most certificate authorities take.
    ///
    /// @param der the encoded request
    /// @return `-----BEGIN CERTIFICATE REQUEST-----` and the base64 body
    public static String toPem(byte[] der) {
        String body = Base64.getEncoder().encodeToString(der);
        StringBuilder out = new StringBuilder("-----BEGIN CERTIFICATE REQUEST-----\n");
        for (int i = 0; i < body.length(); i += 64) {
            out.append(body.substring(i, Math.min(body.length(), i + 64))).append('\n');
        }
        return out.append("-----END CERTIFICATE REQUEST-----\n").toString();
    }

    private CertificateRequest attribute(int[] type, byte[] value) {
        subject.add(seq(oid(type), value));
        return this;
    }

    private static byte[] utf8(String value) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("Empty name");
        }
        try {
            return tlv(0x0c, value.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8", e);
        }
    }

    private static boolean printable(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x21 || c > 0x7e) {
                return false;
            }
        }
        return true;
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) value.charAt(i);
        }
        return out;
    }

    private static String[] split(String address) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= address.length(); i++) {
            if (i == address.length() || address.charAt(i) == '.') {
                parts.add(address.substring(start, i));
                start = i + 1;
            }
        }
        return parts.toArray(new String[parts.size()]);
    }

    static byte[] oid(int[] arcs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(arcs[0] * 40 + arcs[1]);
        for (int i = 2; i < arcs.length; i++) {
            int v = arcs[i];
            int shift = 28;
            while (shift > 0 && (v >>> shift) == 0) {
                shift -= 7;
            }
            for (; shift > 0; shift -= 7) {
                out.write(0x80 | ((v >>> shift) & 0x7f));
            }
            out.write(v & 0x7f);
        }
        return tlv(0x06, out.toByteArray());
    }

    private static byte[] seq(byte[]... parts) {
        List<byte[]> list = new ArrayList<byte[]>();
        for (byte[] p : parts) {
            list.add(p);
        }
        return sequence(list);
    }

    private static byte[] sequence(List<byte[]> parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.write(p, 0, p.length);
        }
        return tlv(0x30, out.toByteArray());
    }

    static byte[] tlv(int tag, byte[] value) {
        int n = value.length;
        int lengthBytes = n < 0x80 ? 0 : n < 0x100 ? 1 : n < 0x10000 ? 2 : n < 0x1000000 ? 3 : 4;
        byte[] out = new byte[2 + lengthBytes + n];
        out[0] = (byte) tag;
        if (lengthBytes == 0) {
            out[1] = (byte) n;
        } else {
            out[1] = (byte) (0x80 | lengthBytes);
            for (int i = 0; i < lengthBytes; i++) {
                out[2 + i] = (byte) (n >>> (8 * (lengthBytes - 1 - i)));
            }
        }
        System.arraycopy(value, 0, out, 2 + lengthBytes, n);
        return out;
    }
}
