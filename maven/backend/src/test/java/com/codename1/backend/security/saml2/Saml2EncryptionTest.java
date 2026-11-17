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

import com.codename1.backend.security.pki.Certificate;
import com.codename1.backend.security.pki.DistinguishedName;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/// Encrypted assertions, encrypted by the JDK's own ciphers, against the
/// decryption under test.
class Saml2EncryptionTest {
    private static SamlFixtures idp;
    private static KeyPair sp;
    private static KeyPair other;
    private static Certificate spCert;

    @BeforeAll
    static void keys() throws Exception {
        idp = new SamlFixtures();
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        sp = g.generateKeyPair();
        other = g.generateKeyPair();
        spCert = Certificate.selfSigned(DistinguishedName.parse("CN=sp.example.com"),
                sp.getPrivate().getEncoded(), 30);
    }

    private static RelyingPartyRegistration registration() {
        return idp.registration().decryptionCredential(sp.getPrivate().getEncoded(), spCert)
                .build();
    }

    private static Saml2AuthenticatedPrincipal validate(String xml,
            RelyingPartyRegistration r) throws Exception {
        return new Saml2ResponseValidator().validate(xml.getBytes("UTF-8"), r,
                SamlFixtures.ACS, SamlFixtures.SP, null, System.currentTimeMillis());
    }

    private static String encrypted(String data, String transport, boolean inside)
            throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.assertionId = "_enc" + System.nanoTime();
        return SamlFixtures.encrypt(idp.xml(s), sp.getPublic(), data, transport, inside);
    }

    private static String code(String xml, RelyingPartyRegistration r) {
        return assertThrows(Saml2AuthenticationException.class, () -> validate(xml, r))
                .getErrorCode();
    }

    @Test
    void decryptsEveryCombinationIdentityProvidersSend() throws Exception {
        String[] data = {"aes128-gcm", "aes256-gcm", "aes128-cbc", "aes256-cbc"};
        String[] transport = {"rsa-oaep-mgf1p", "rsa-oaep"};
        for (String d : data) {
            for (String t : transport) {
                for (boolean inside : new boolean[] {true, false}) {
                    Saml2AuthenticatedPrincipal p = validate(encrypted(d, t, inside),
                            registration());
                    assertEquals("alice@corp.example", p.getName(), d + " " + t + " " + inside);
                    assertEquals("admins", p.getFirstAttribute("groups"));
                }
            }
        }
    }

    @Test
    void refusesPkcs1KeyTransport() throws Exception {
        assertEquals(Saml2ErrorCodes.DECRYPTION_ERROR,
                code(encrypted("aes128-gcm", "rsa-1_5", true), registration()));
    }

    @Test
    void wrongKeyIsOneUniformError() throws Exception {
        RelyingPartyRegistration wrong = idp.registration()
                .decryptionCredential(other.getPrivate().getEncoded(), spCert).build();
        Saml2AuthenticationException gcm = assertThrows(Saml2AuthenticationException.class,
                () -> validate(encrypted("aes128-gcm", "rsa-oaep-mgf1p", true), wrong));
        Saml2AuthenticationException cbc = assertThrows(Saml2AuthenticationException.class,
                () -> validate(encrypted("aes128-cbc", "rsa-oaep-mgf1p", true), wrong));
        assertEquals(Saml2ErrorCodes.DECRYPTION_ERROR, gcm.getErrorCode());
        assertEquals(gcm.getMessage(), cbc.getMessage());
    }

    @Test
    void tamperedCiphertextIsRefused() throws Exception {
        for (String d : new String[] {"aes128-gcm", "aes128-cbc"}) {
            String xml = encrypted(d, "rsa-oaep-mgf1p", true);
            int at = xml.lastIndexOf("<xenc:CipherValue>") + "<xenc:CipherValue>".length() + 30;
            char c = xml.charAt(at);
            String bad = xml.substring(0, at) + (c == 'A' ? 'B' : 'A') + xml.substring(at + 1);
            String error = code(bad, registration());
            // CBC has no tag: a flipped byte decrypts to garbage, which then
            // fails to parse or to verify. Either way it never signs anyone in.
            assertEquals(true, Saml2ErrorCodes.DECRYPTION_ERROR.equals(error)
                    || Saml2ErrorCodes.INVALID_SIGNATURE.equals(error)
                    || Saml2ErrorCodes.MALFORMED_RESPONSE_DATA.equals(error), d + " " + error);
        }
    }

    @Test
    void noDecryptionKeyIsADecryptionError() throws Exception {
        assertEquals(Saml2ErrorCodes.DECRYPTION_ERROR,
                code(encrypted("aes128-gcm", "rsa-oaep-mgf1p", true), idp.registration().build()));
    }

    @Test
    void unsignedEncryptedAssertionIsRefused() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.signAssertion = false;
        String xml = SamlFixtures.encrypt(idp.xml(s), sp.getPublic(), "aes128-gcm",
                "rsa-oaep-mgf1p", true);
        assertEquals(Saml2ErrorCodes.INVALID_SIGNATURE, code(xml, registration()));
    }

    @Test
    void signedResponseOverEncryptedAssertion() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.signAssertion = false;
        String xml = SamlFixtures.encrypt(idp.xml(s), sp.getPublic(), "aes256-gcm",
                "rsa-oaep", true);
        Document doc = SamlFixtures.parse(xml);
        Element response = doc.getDocumentElement();
        response.setIdAttribute("ID", true);
        idp.sign(response, s.responseId);
        String signed = SamlFixtures.serialize(doc);
        assertEquals("alice@corp.example", validate(signed, registration()).getName());
        // and the response signature covers the ciphertext
        int at = signed.lastIndexOf("<xenc:CipherValue>") + "<xenc:CipherValue>".length() + 10;
        char c = signed.charAt(at);
        String bad = signed.substring(0, at) + (c == 'A' ? 'B' : 'A') + signed.substring(at + 1);
        assertEquals(Saml2ErrorCodes.INVALID_SIGNATURE, code(bad, registration()));
    }

    @Test
    void twoAssertionsHiddenInsideOneEncryptionAreRefused() throws Exception {
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        String xml = idp.xml(s);
        int start = xml.indexOf("<saml:Assertion");
        int end = xml.indexOf("</saml:Assertion>") + "</saml:Assertion>".length();
        String one = xml.substring(start, end);
        String doubled = xml.substring(0, start) + "<saml:Assertion" + one.substring(15)
                .replace("</saml:Assertion>", "</saml:Assertion>" + one.replace(s.assertionId,
                        "_evil").replace("alice@", "admin@"))
                + xml.substring(end);
        // the encryptor takes the first Assertion through the last close tag
        String enc = SamlFixtures.encrypt(doubled, sp.getPublic(), "aes128-gcm",
                "rsa-oaep-mgf1p", true);
        String error = code(enc, registration());
        assertEquals(true, !"".equals(error), error);
    }
}
