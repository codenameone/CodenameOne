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
package com.codename1.backend.security.pki;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;

/// A PKCS#10 certification request: what a CA (an ACME server, a corporate CA,
/// `openssl x509 -req`) is sent to issue a certificate for a key.
///
/// ```java
/// byte[] key = Crypto.generateEcKey(Crypto.P256);
/// CertificationRequest csr = CertificationRequest.create(
///         DistinguishedName.parse("CN=api.example.com"), key,
///         Arrays.asList("api.example.com", "www.example.com"));
/// String pem = csr.toPem();
/// ```
///
/// The request is signed with SHA-256 for an RSA key or a P-256 key, and with
/// SHA-384 for a P-384 key. DNS names go in a subject alternative name
/// extension request, which is where every CA reads them from.
public final class CertificationRequest {
    private static final String EXTENSION_REQUEST = "1.2.840.113549.1.9.14";

    private final byte[] der;
    private final byte[] info;
    private final byte[] signatureAlgorithm;
    private final byte[] signature;
    private final DistinguishedName subject;
    private final byte[] publicKey;
    private final List<String> dnsNames = new ArrayList<String>();

    private CertificationRequest(byte[] der) throws IOException {
        this.der = der.clone();
        try {
            Asn1.Reader csr = new Asn1.Reader(this.der).open(Asn1.SEQUENCE);
            info = csr.raw(Asn1.SEQUENCE);
            signatureAlgorithm = csr.raw(Asn1.SEQUENCE);
            signature = csr.bits();
            csr.finish();
            Asn1.Reader body = new Asn1.Reader(info).open(Asn1.SEQUENCE);
            if (body.smallInteger() != 0) {
                throw new CertificateException("Not a version 1 certification request");
            }
            subject = DistinguishedName.decode(body.raw(Asn1.SEQUENCE));
            publicKey = body.raw(Asn1.SEQUENCE);
            Asn1.Reader attributes = body.openOptional(Asn1.explicit(0));
            while (attributes != null && attributes.hasMore()) {
                Asn1.Reader attribute = attributes.open(Asn1.SEQUENCE);
                String oid = attribute.oid();
                Asn1.Reader values = attribute.open(Asn1.SET);
                if (EXTENSION_REQUEST.equals(oid)) {
                    readExtensions(values.open(Asn1.SEQUENCE));
                }
            }
        } catch (CertificateException err) {
            throw err;
        } catch (IOException err) {
            throw new CertificateException("Not a PKCS#10 certification request: "
                    + err.getMessage(), err);
        }
    }

    private void readExtensions(Asn1.Reader list) throws IOException {
        while (list.hasMore()) {
            Asn1.Reader ext = list.open(Asn1.SEQUENCE);
            String oid = ext.oid();
            if (ext.peek() == Asn1.BOOLEAN) {
                ext.bool();
            }
            byte[] value = ext.read(Asn1.OCTET_STRING);
            if (Certificate.SUBJECT_ALT_NAME.equals(oid)) {
                Asn1.Reader names = new Asn1.Reader(value).open(Asn1.SEQUENCE);
                while (names.hasMore()) {
                    if (names.peek() == Asn1.implicit(2)) {
                        dnsNames.add(Asn1.ascii(names.read(Asn1.implicit(2))));
                    } else {
                        names.skip();
                    }
                }
            }
        }
    }

    /// Creates and signs a request.
    ///
    /// @param subject the subject; may be empty when `dnsNames` says who it is for
    /// @param privateKey the PKCS#8 key the certificate will be for, RSA, P-256 or
    /// P-384. It must carry its public key, as every key [Crypto] generates and
    /// `openssl` writes does.
    /// @param dnsNames the DNS names to request; null or empty for none
    public static CertificationRequest create(DistinguishedName subject, byte[] privateKey,
                                              List<String> dnsNames) throws IOException {
        if (subject == null || privateKey == null) {
            throw new CertificateException("A subject and a private key are required");
        }
        byte[] publicKey = Der.publicKeyOf(privateKey);
        String algorithm = SignatureAlgorithms.forKey(publicKey);
        byte[] attributes;
        if (dnsNames == null || dnsNames.isEmpty()) {
            attributes = Asn1.tlv(Asn1.explicit(0));
        } else {
            List<byte[]> names = new ArrayList<byte[]>();
            for (String name : dnsNames) {
                names.add(Asn1.tlv(Asn1.implicit(2), Asn1.asciiBytes(name)));
            }
            byte[] san = Asn1.sequence(names.toArray(new byte[names.size()][]));
            byte[] extensions = Asn1.sequence(Asn1.sequence(Asn1.oid(Certificate.SUBJECT_ALT_NAME),
                    Asn1.octets(san)));
            attributes = Asn1.tlv(Asn1.explicit(0), Asn1.sequence(Asn1.oid(EXTENSION_REQUEST),
                    Asn1.set(extensions)));
        }
        byte[] info = Asn1.sequence(Asn1.integer(0), subject.der(), publicKey, attributes);
        byte[] signature = Crypto.sign(algorithm, privateKey, info);
        return new CertificationRequest(Asn1.sequence(info, SignatureAlgorithms.identifier(algorithm),
                Asn1.bitString(signature)));
    }

    /// Reads a request from its DER.
    public static CertificationRequest decode(byte[] der) throws IOException {
        if (der == null) {
            throw new CertificateException("No certification request was given");
        }
        return new CertificationRequest(der);
    }

    /// The first `CERTIFICATE REQUEST` block of `pem`. `NEW CERTIFICATE REQUEST`,
    /// which some older tools write, is read too.
    public static CertificationRequest fromPem(String pem) throws IOException {
        for (Pem.Block block : Pem.decode(pem)) {
            if (Pem.CERTIFICATE_REQUEST.equals(block.getLabel())
                    || "NEW CERTIFICATE REQUEST".equals(block.getLabel())) {
                return new CertificationRequest(block.getDer());
            }
        }
        throw new CertificateException("The PEM text holds no CERTIFICATE REQUEST block");
    }

    /// The DER of this request.
    public byte[] getEncoded() {
        return der.clone();
    }

    /// The request as one PEM block.
    public String toPem() {
        return Pem.encode(Pem.CERTIFICATE_REQUEST, der);
    }

    /// The subject the request asks for.
    public DistinguishedName getSubject() {
        return subject;
    }

    /// The public key, as SubjectPublicKeyInfo DER.
    public byte[] getPublicKey() {
        return publicKey.clone();
    }

    /// The DNS names of the subject alternative name extension request.
    public List<String> getDnsNames() {
        return Collections.unmodifiableList(dnsNames);
    }

    /// Whether the request is signed by the key it carries, which proves the
    /// requester holds the private key.
    ///
    /// @throws IOException when the signature algorithm is outside the profile
    public boolean verify() throws IOException {
        return Crypto.verify(SignatureAlgorithms.forIdentifier(signatureAlgorithm), publicKey, info,
                signature);
    }
}
