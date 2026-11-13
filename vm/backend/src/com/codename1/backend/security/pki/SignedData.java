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

/// The content and the certificates of a CMS SignedData structure (RFC 5652),
/// the PKCS#7 envelope a `.mobileprovision` profile, a signed manifest or a
/// detached signature file arrives in.
///
/// ```java
/// SignedData signed = SignedData.decode(profileBytes);
/// byte[] plist = signed.getContent();
/// List<Certificate> chain = signed.getCertificates();
/// ```
///
/// It reads; it does not judge. Nothing here checks the signer's signature
/// over the content or the chain behind it: validate the certificates with
/// [CertificateValidator] and trust the content only as far as that goes.
/// Like the rest of this package it reads DER, so a BER encoding with
/// indefinite lengths is reported as such.
public final class SignedData {
    /// `id-signedData`.
    public static final String SIGNED_DATA = "1.2.840.113549.1.7.2";
    /// `id-data`, the content type of plain content.
    public static final String DATA = "1.2.840.113549.1.7.1"; //NOPMD AvoidUsingHardCodedIP - an OID

    private final String contentType;
    private final byte[] content;
    private final List<Certificate> certificates;
    private final int signerCount;

    private SignedData(String contentType, byte[] content, List<Certificate> certificates,
                       int signerCount) {
        this.contentType = contentType;
        this.content = content;
        this.certificates = certificates;
        this.signerCount = signerCount;
    }

    /// Reads a ContentInfo that holds SignedData.
    public static SignedData decode(byte[] der) throws IOException {
        if (der == null) {
            throw new CertificateException("No CMS data was given");
        }
        try {
            Asn1.Reader info = new Asn1.Reader(der).open(Asn1.SEQUENCE);
            String type = info.oid();
            if (!SIGNED_DATA.equals(type)) {
                throw new CertificateException("Not CMS SignedData: the content type is " + type);
            }
            Asn1.Reader signed = info.open(Asn1.explicit(0)).open(Asn1.SEQUENCE);
            info.finish();
            signed.smallInteger();
            signed.read(Asn1.SET);
            Asn1.Reader encap = signed.open(Asn1.SEQUENCE);
            String contentType = encap.oid();
            byte[] content = null;
            Asn1.Reader wrapped = encap.openOptional(Asn1.explicit(0));
            if (wrapped != null) {
                content = wrapped.read(Asn1.OCTET_STRING);
                wrapped.finish();
            }
            encap.finish();
            List<Certificate> certificates = new ArrayList<Certificate>();
            // [0] IMPLICIT SET OF CertificateChoices: the same constructed tag.
            Asn1.Reader certs = signed.openOptional(Asn1.explicit(0));
            if (certs != null) {
                while (certs.hasMore()) {
                    if (certs.peek() == Asn1.SEQUENCE) {
                        certificates.add(Certificate.decode(certs.raw(Asn1.SEQUENCE)));
                    } else {
                        // An attribute or other certificate format: not X.509.
                        certs.skip();
                    }
                }
            }
            if (signed.hasMore() && signed.peek() == Asn1.explicit(1)) {
                // Revocation lists: not read here.
                signed.skip();
            }
            Asn1.Reader signers = signed.open(Asn1.SET);
            int count = 0;
            while (signers.hasMore()) {
                signers.skip();
                count++;
            }
            signed.finish();
            return new SignedData(contentType, content, Collections.unmodifiableList(certificates),
                    count);
        } catch (CertificateException err) {
            throw err;
        } catch (IOException err) {
            throw new CertificateException("Not CMS SignedData: " + err.getMessage(), err);
        }
    }

    /// The type of the content, [#DATA] for plain bytes.
    public String getContentType() {
        return contentType;
    }

    /// The signed content, or null when the signature is detached.
    public byte[] getContent() {
        return content == null ? null : copy(content);
    }

    /// The certificates the sender included, in the order sent; usually the
    /// signer's and the intermediates behind it.
    public List<Certificate> getCertificates() {
        return certificates;
    }

    /// How many signers signed it.
    public int getSignerCount() {
        return signerCount;
    }

    private static byte[] copy(byte[] value) {
        byte[] out = new byte[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }
}
