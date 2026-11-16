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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/// The response validator against responses the JDK signed, and against the
/// known ways of forging one.
class Saml2ResponseValidatorTest {
    private static SamlFixtures idp;

    @BeforeAll
    static void keys() throws Exception {
        idp = new SamlFixtures();
    }

    private static Saml2AuthenticatedPrincipal validate(byte[] xml) {
        return new Saml2ResponseValidator().validate(xml, idp.registration().build(),
                SamlFixtures.ACS, SamlFixtures.SP, null, System.currentTimeMillis());
    }

    private static String code(byte[] xml) {
        return assertThrows(Saml2AuthenticationException.class, () -> validate(xml))
                .getErrorCode();
    }

    @Test
    void acceptsSignedAssertion() throws Exception {
        Saml2AuthenticatedPrincipal p = validate(idp.bytes(new SamlFixtures.Spec()));
        assertEquals("alice@corp.example", p.getName());
        assertEquals("admins", p.getFirstAttribute("groups"));
        assertEquals(2, p.getAttribute("groups").size());
        assertEquals("_session1", p.getSessionIndexes().get(0));
    }

    @Test
    void acceptsSignedResponseOnly() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.signAssertion = false;
        s.signResponse = true;
        assertEquals("alice@corp.example", validate(idp.bytes(s)).getName());
    }

    @Test
    void ourSignerIsAcceptedByTheJdk() throws Exception {
        String unsigned = "<a:Doc xmlns:a=\"urn:x\" ID=\"_d1\"><a:Issuer>me</a:Issuer>"
                + XmlSignature.SIGNATURE_MARK + "<a:Body x=\"1 &amp; 2\">t&lt;</a:Body></a:Doc>";
        String signed = XmlSignature.sign(unsigned, "_d1", idp.keys.getPrivate().getEncoded());
        assertTrue(idp.jdkVerifies(signed, "_d1"));
    }

    @Test
    void refusesUnsigned() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.signAssertion = false;
        assertEquals(Saml2ErrorCodes.INVALID_SIGNATURE, code(idp.bytes(s)));
    }

    @Test
    void refusesOtherKey() throws Exception {
        SamlFixtures other = new SamlFixtures();
        assertEquals(Saml2ErrorCodes.INVALID_SIGNATURE, code(other.bytes(new SamlFixtures.Spec())));
    }

    @Test
    void refusesTamperedName() throws Exception {
        String xml = idp.xml(new SamlFixtures.Spec()).replace("alice@corp.example",
                "admin@corp.example");
        assertEquals(Saml2ErrorCodes.INVALID_SIGNATURE, code(xml.getBytes("UTF-8")));
    }

    @Test
    void refusesCommentTruncation() throws Exception {
        // The classic: a comment splits the NameID; a reader that keeps the
        // first text node only sees "admin@corp.example" while the signature
        // covers the whole text. Ours merges around comments.
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.nameId = "admin@corp.example.evil";
        String xml = idp.xml(s).replace("admin@corp.example.evil",
                "admin@corp.example<!---->.evil");
        Saml2AuthenticatedPrincipal p = validate(xml.getBytes("UTF-8"));
        assertEquals("admin@corp.example.evil", p.getName());
    }

    @Test
    void refusesWrappedAssertion() throws Exception {
        // XSW: the signed assertion is kept, a forged one is placed first.
        String good = idp.xml(new SamlFixtures.Spec());
        int at = good.indexOf("<saml:Assertion");
        String forged = good.substring(at, good.indexOf("</saml:Assertion>") + 17)
                .replace("_assert1", "_evil").replace("alice@corp.example", "admin@corp.example");
        String xml = good.substring(0, at) + forged + good.substring(at);
        code(xml.getBytes("UTF-8"));
    }

    @Test
    void refusesDoctype() throws Exception {
        String xml = "<!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + idp.xml(new SamlFixtures.Spec());
        assertEquals(Saml2ErrorCodes.MALFORMED_RESPONSE_DATA, code(xml.getBytes("UTF-8")));
    }

    @Test
    void refusesWrongAudience() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.audience = "https://other.example/sp";
        assertEquals(Saml2ErrorCodes.INVALID_ASSERTION, code(idp.bytes(s)));
    }

    @Test
    void refusesExpired() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.now -= 60 * 60 * 1000L;
        assertEquals(Saml2ErrorCodes.INVALID_ASSERTION, code(idp.bytes(s)));
    }

    @Test
    void refusesWrongDestination() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.destination = "https://other.example/acs";
        s.signResponse = true;
        assertEquals(Saml2ErrorCodes.INVALID_DESTINATION, code(idp.bytes(s)));
    }

    @Test
    void refusesWrongIssuer() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.issuer = "https://evil.example";
        assertEquals(Saml2ErrorCodes.INVALID_ISSUER, code(idp.bytes(s)));
    }

    @Test
    void refusesReplay() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.assertionId = "_replay" + System.nanoTime();
        byte[] xml = idp.bytes(s);
        Saml2ResponseValidator v = new Saml2ResponseValidator();
        RelyingPartyRegistration r = idp.registration().build();
        long now = System.currentTimeMillis();
        v.validate(xml, r, SamlFixtures.ACS, SamlFixtures.SP, null, now);
        assertThrows(Saml2AuthenticationException.class,
                () -> v.validate(xml, r, SamlFixtures.ACS, SamlFixtures.SP, null, now));
    }

    @Test
    void holdsInResponseTo() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.inResponseTo = "ARQabc";
        byte[] xml = idp.bytes(s);
        RelyingPartyRegistration r = idp.registration().build();
        long now = System.currentTimeMillis();
        assertThrows(Saml2AuthenticationException.class, () -> new Saml2ResponseValidator()
                .validate(xml, r, SamlFixtures.ACS, SamlFixtures.SP, "ARQother", now));
        assertEquals("alice@corp.example", new Saml2ResponseValidator()
                .validate(xml, r, SamlFixtures.ACS, SamlFixtures.SP, "ARQabc", now).getName());
    }

    @Test
    void parsesMetadata() throws Exception {
        String md = "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" "
                + "entityID=\"" + SamlFixtures.IDP + "\"><md:IDPSSODescriptor "
                + "protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<md:KeyDescriptor use=\"signing\"><ds:KeyInfo xmlns:ds=\""
                + XmlSignature.DSIG + "\"><ds:X509Data><ds:X509Certificate>"
                + com.codename1.backend.Base64.encode(idp.certificate.getEncoded())
                + "</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>"
                + "<md:SingleSignOnService Binding=\"" + RelyingPartyRegistration.REDIRECT
                + "\" Location=\"" + SamlFixtures.SSO + "\"/></md:IDPSSODescriptor>"
                + "</md:EntityDescriptor>";
        RelyingPartyRegistration r = RelyingPartyRegistrations.fromMetadata(md, "corp").build();
        assertEquals(SamlFixtures.SSO, r.getSingleSignOnServiceLocation());
        assertEquals("alice@corp.example", new Saml2ResponseValidator().validate(
                idp.bytes(new SamlFixtures.Spec()), r, SamlFixtures.ACS, SamlFixtures.SP, null,
                System.currentTimeMillis()).getName());
    }
}
