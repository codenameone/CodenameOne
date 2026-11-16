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

import com.codename1.backend.security.pki.Certificate;

/// This server's SAML metadata, for the identity provider's administrator to
/// load: who it is, where responses go, the key its requests are signed with,
/// the keys assertions may be encrypted to, and where logout messages go.
public final class Saml2Metadata {
    /// The algorithms this server decrypts, advertised beside its encryption key.
    private static final String[] ENCRYPTION_METHODS = {XmlEncryption.XENC11 + "aes256-gcm",
        XmlEncryption.XENC11 + "aes128-gcm", XmlEncryption.XENC + "aes256-cbc",
        XmlEncryption.XENC + "aes128-cbc", XmlEncryption.XENC11 + "rsa-oaep",
        XmlEncryption.XENC + "rsa-oaep-mgf1p"};
    /// The bindings its logout address answers.
    private static final String[] SLO_BINDINGS = {RelyingPartyRegistration.REDIRECT,
        RelyingPartyRegistration.POST};

    private Saml2Metadata() {
    }

    /// The `EntityDescriptor` of this server as `registration`'s service provider.
    public static String forServiceProvider(RelyingPartyRegistration registration,
                                            String entityId, String acsLocation) {
        return forServiceProvider(registration, entityId, acsLocation, null);
    }

    /// The `EntityDescriptor`, with a `SingleLogoutService` at `sloLocation`
    /// for both bindings when it is not null.
    public static String forServiceProvider(RelyingPartyRegistration registration,
                                            String entityId, String acsLocation,
                                            String sloLocation) {
        StringBuilder xml = new StringBuilder(2048);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<md:EntityDescriptor xmlns:md=\"").append(RelyingPartyRegistrations.MD)
                .append("\" entityID=\"").append(Xml.escape(entityId)).append("\">\n")
                .append("<md:SPSSODescriptor AuthnRequestsSigned=\"")
                .append(registration.getSigningKey() != null)
                .append("\" WantAssertionsSigned=\"true\" protocolSupportEnumeration=\"")
                .append(Saml2ResponseValidator.PROTOCOL).append("\">\n");
        Certificate cert = registration.getSigningCertificate();
        if (cert != null) {
            xml.append("<md:KeyDescriptor use=\"signing\"><ds:KeyInfo xmlns:ds=\"")
                    .append(XmlSignature.DSIG).append("\"><ds:X509Data><ds:X509Certificate>")
                    .append(com.codename1.backend.Base64.encode(cert.getEncoded()))
                    .append("</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>\n");
        }
        for (Certificate c : registration.getDecryptionCertificates()) {
            xml.append("<md:KeyDescriptor use=\"encryption\"><ds:KeyInfo xmlns:ds=\"")
                    .append(XmlSignature.DSIG).append("\"><ds:X509Data><ds:X509Certificate>")
                    .append(com.codename1.backend.Base64.encode(c.getEncoded()))
                    .append("</ds:X509Certificate></ds:X509Data></ds:KeyInfo>");
            for (String alg : ENCRYPTION_METHODS) {
                xml.append("<md:EncryptionMethod Algorithm=\"").append(alg).append("\"/>");
            }
            xml.append("</md:KeyDescriptor>\n");
        }
        if (sloLocation != null) {
            for (String binding : SLO_BINDINGS) {
                xml.append("<md:SingleLogoutService Binding=\"").append(binding)
                        .append("\" Location=\"").append(Xml.escape(sloLocation))
                        .append("\"/>\n");
            }
        }
        if (registration.getNameIdFormat() != null) {
            xml.append("<md:NameIDFormat>").append(Xml.escape(registration.getNameIdFormat()))
                    .append("</md:NameIDFormat>\n");
        }
        xml.append("<md:AssertionConsumerService Binding=\"").append(RelyingPartyRegistration.POST)
                .append("\" Location=\"").append(Xml.escape(acsLocation))
                .append("\" index=\"0\" isDefault=\"true\"/>\n")
                .append("</md:SPSSODescriptor>\n</md:EntityDescriptor>\n");
        return xml.toString();
    }
}
