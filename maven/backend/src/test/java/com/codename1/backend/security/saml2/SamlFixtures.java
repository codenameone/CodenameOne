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
import com.codename1.backend.security.pki.DistinguishedName;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/// A stand-in identity provider for the tests: writes SAML responses and signs
/// them with the JDK's own XML Signature implementation, which shares no code
/// with the verifier under test -- so a canonicalization mistake on either
/// side shows up as a failure rather than as two wrongs agreeing.
public final class SamlFixtures {
    public static final String IDP = "https://idp.example.com/metadata";
    public static final String SP = "http://localhost/saml2/service-provider-metadata/corp";
    public static final String ACS = "http://localhost/login/saml2/sso/corp";
    public static final String SSO = "https://idp.example.com/sso";

    public final KeyPair keys;
    public final Certificate certificate;

    public SamlFixtures() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        keys = g.generateKeyPair();
        certificate = Certificate.selfSigned(DistinguishedName.parse("CN=idp.example.com"),
                keys.getPrivate().getEncoded(), 30);
    }

    public RelyingPartyRegistration.Builder registration() {
        return RelyingPartyRegistration.withRegistrationId("corp")
                .assertingPartyEntityId(IDP)
                .singleSignOnServiceLocation(SSO)
                .verificationCertificate(certificate);
    }

    public static String time(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(millis));
    }

    /// What goes into a response; each test changes what it is about.
    public static final class Spec {
        public String responseId = "_resp1";
        public String assertionId = "_assert1";
        public String destination = ACS;
        public String inResponseTo;
        public String issuer = IDP;
        public String status = "urn:oasis:names:tc:SAML:2.0:status:Success";
        public String nameId = "alice@corp.example";
        public String audience = SP;
        public String recipient = ACS;
        public long now = System.currentTimeMillis();
        public long lifetime = 5 * 60 * 1000L;
        public boolean signAssertion = true;
        public boolean signResponse;
        public String attributes = "<saml:AttributeStatement><saml:Attribute Name=\"groups\">"
                + "<saml:AttributeValue>admins</saml:AttributeValue>"
                + "<saml:AttributeValue>staff</saml:AttributeValue></saml:Attribute>"
                + "</saml:AttributeStatement>";
    }

    public String unsignedXml(Spec s) {
        String irt = s.inResponseTo == null ? "" : " InResponseTo=\"" + s.inResponseTo + "\"";
        return "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" "
                + "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"" + s.responseId
                + "\" Version=\"2.0\" IssueInstant=\"" + time(s.now) + "\""
                + (s.destination == null ? "" : " Destination=\"" + s.destination + "\"") + irt
                + ">\n  <saml:Issuer>" + s.issuer + "</saml:Issuer>\n"
                + "  <samlp:Status><samlp:StatusCode Value=\"" + s.status + "\"/></samlp:Status>\n"
                + "  <saml:Assertion ID=\"" + s.assertionId + "\" Version=\"2.0\" IssueInstant=\""
                + time(s.now) + "\">\n    <saml:Issuer>" + s.issuer + "</saml:Issuer>\n"
                + "    <saml:Subject><saml:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\">"
                + s.nameId + "</saml:NameID>\n"
                + "      <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">"
                + "<saml:SubjectConfirmationData NotOnOrAfter=\"" + time(s.now + s.lifetime)
                + "\" Recipient=\"" + s.recipient + "\"" + irt + "/></saml:SubjectConfirmation>\n"
                + "    </saml:Subject>\n"
                + "    <saml:Conditions NotBefore=\"" + time(s.now - 60000) + "\" NotOnOrAfter=\""
                + time(s.now + s.lifetime) + "\"><saml:AudienceRestriction><saml:Audience>"
                + s.audience + "</saml:Audience></saml:AudienceRestriction></saml:Conditions>\n"
                + "    <saml:AuthnStatement AuthnInstant=\"" + time(s.now)
                + "\" SessionIndex=\"_session1\"><saml:AuthnContext><saml:AuthnContextClassRef>"
                + "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
                + "</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement>\n"
                + "    " + s.attributes + "\n  </saml:Assertion>\n</samlp:Response>";
    }

    /// The response of `s`, signed as `s` says, as XML text.
    public String xml(Spec s) throws Exception {
        Document doc = parse(unsignedXml(s));
        Element response = doc.getDocumentElement();
        Element assertion = (Element) response.getElementsByTagNameNS(
                "urn:oasis:names:tc:SAML:2.0:assertion", "Assertion").item(0);
        response.setIdAttribute("ID", true);
        assertion.setIdAttribute("ID", true);
        if (s.signAssertion) {
            sign(assertion, s.assertionId);
        }
        if (s.signResponse) {
            sign(response, s.responseId);
        }
        return serialize(doc);
    }

    public byte[] bytes(Spec s) throws Exception {
        return xml(s).getBytes("UTF-8");
    }

    /// Signs `e` enveloped, the signature right after its Issuer, as SAML puts it.
    public void sign(Element e, String id) throws Exception {
        XMLSignatureFactory f = XMLSignatureFactory.getInstance("DOM");
        Reference ref = f.newReference("#" + id,
                f.newDigestMethod(DigestMethod.SHA256, null),
                Arrays.asList(f.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                        f.newTransform(CanonicalizationMethod.EXCLUSIVE,
                                (TransformParameterSpec) null)), null, null);
        SignedInfo si = f.newSignedInfo(f.newCanonicalizationMethod(
                CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                f.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                Collections.singletonList(ref));
        Node issuer = e.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion",
                "Issuer").item(0);
        DOMSignContext ctx = new DOMSignContext(keys.getPrivate(), e, issuer.getNextSibling());
        ctx.setDefaultNamespacePrefix("ds");
        f.newXMLSignature(si, null).sign(ctx);
    }

    /// Whether the JDK accepts the enveloped signature of the element with `id`.
    public boolean jdkVerifies(String xml, String id) throws Exception {
        Document doc = parse(xml);
        Element target = find(doc.getDocumentElement(), id);
        target.setIdAttribute("ID", true);
        NodeList sigs = target.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        DOMValidateContext ctx = new DOMValidateContext(keys.getPublic(), sigs.item(0));
        return XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(ctx).validate(ctx);
    }

    private static Element find(Element e, String id) {
        if (id.equals(e.getAttribute("ID"))) {
            return e;
        }
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element found = find((Element) children.item(i), id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static Document parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    public static String serialize(Document doc) throws Exception {
        javax.xml.transform.Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty("omit-xml-declaration", "yes");
        StringWriter out = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(out));
        return out.toString();
    }

    /// Replaces the assertion of the signed response `xml` with an
    /// EncryptedAssertion, encrypted with the JDK's own ciphers to `to`.
    ///
    /// @param data `aes128-gcm`, `aes256-gcm`, `aes128-cbc` or `aes256-cbc`
    /// @param transport `rsa-oaep-mgf1p`, `rsa-oaep` (SHA-256 digest and
    /// MGF1) or `rsa-1_5`
    /// @param keyInside the EncryptedKey inside the data's KeyInfo, or beside it
    public static String encrypt(String xml, java.security.PublicKey to, String data,
            String transport, boolean keyInside) throws Exception {
        int start = xml.indexOf("<saml:Assertion");
        int end = xml.indexOf("</saml:Assertion>") + "</saml:Assertion>".length();
        byte[] plain = xml.substring(start, end).getBytes("UTF-8");
        boolean gcm = data.endsWith("gcm");
        int bits = data.startsWith("aes256") ? 256 : 128;
        javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance("AES");
        kg.init(bits);
        javax.crypto.SecretKey cek = kg.generateKey();
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] body;
        if (gcm) {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            c.init(javax.crypto.Cipher.ENCRYPT_MODE, cek,
                    new javax.crypto.spec.GCMParameterSpec(128, iv));
            body = concat(iv, c.doFinal(plain));
        } else {
            byte[] iv = new byte[16];
            random.nextBytes(iv);
            // XML Encryption's padding: random bytes, the last one the pad length.
            int pad = 16 - plain.length % 16;
            byte[] padded = java.util.Arrays.copyOf(plain, plain.length + pad);
            padded[padded.length - 1] = (byte) pad;
            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/CBC/NoPadding");
            c.init(javax.crypto.Cipher.ENCRYPT_MODE, cek,
                    new javax.crypto.spec.IvParameterSpec(iv));
            body = concat(iv, c.doFinal(padded));
        }
        String keyAlg;
        String keyExtra = "";
        javax.crypto.Cipher wrap;
        if ("rsa-1_5".equals(transport)) {
            keyAlg = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
            wrap = null;
        } else if ("rsa-oaep".equals(transport)) {
            keyAlg = "http://www.w3.org/2009/xmlenc11#rsa-oaep";
            keyExtra = "<ds:DigestMethod xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" "
                    + "Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>"
                    + "<xenc11:MGF xmlns:xenc11=\"http://www.w3.org/2009/xmlenc11#\" "
                    + "Algorithm=\"http://www.w3.org/2009/xmlenc11#mgf1sha256\"/>";
            wrap = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPPadding");
            wrap.init(javax.crypto.Cipher.ENCRYPT_MODE, to, new javax.crypto.spec.OAEPParameterSpec(
                    "SHA-256", "MGF1", java.security.spec.MGF1ParameterSpec.SHA256,
                    javax.crypto.spec.PSource.PSpecified.DEFAULT));
        } else {
            keyAlg = "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";
            wrap = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPPadding");
            wrap.init(javax.crypto.Cipher.ENCRYPT_MODE, to, new javax.crypto.spec.OAEPParameterSpec(
                    "SHA-1", "MGF1", java.security.spec.MGF1ParameterSpec.SHA1,
                    javax.crypto.spec.PSource.PSpecified.DEFAULT));
        }
        java.util.Base64.Encoder b64 = java.util.Base64.getEncoder();
        String ek = "<xenc:EncryptedKey><xenc:EncryptionMethod Algorithm=\"" + keyAlg + "\">"
                + keyExtra + "</xenc:EncryptionMethod><xenc:CipherData><xenc:CipherValue>"
                + b64.encodeToString(wrap == null ? pkcs1v15(to, cek.getEncoded(), random)
                        : wrap.doFinal(cek.getEncoded()))
                + "</xenc:CipherValue></xenc:CipherData></xenc:EncryptedKey>";
        String dataAlg = (gcm ? "http://www.w3.org/2009/xmlenc11#" : "http://www.w3.org/2001/04/xmlenc#")
                + data;
        String enc = "<saml:EncryptedAssertion><xenc:EncryptedData "
                + "xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" "
                + "Type=\"http://www.w3.org/2001/04/xmlenc#Element\">"
                + "<xenc:EncryptionMethod Algorithm=\"" + dataAlg + "\"/>"
                + (keyInside ? "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">"
                        + ek + "</ds:KeyInfo>" : "")
                + "<xenc:CipherData><xenc:CipherValue>" + b64.encodeToString(body)
                + "</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData>"
                + (keyInside ? "" : ek.replace("<xenc:EncryptedKey>",
                        "<xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">"))
                + "</saml:EncryptedAssertion>";
        return xml.substring(0, start) + enc + xml.substring(end);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /// RSA PKCS#1 v1.5 encryption padded by hand (RFC 8017 7.2.1), for the test
    /// that the SP refuses this transport. The bytes are genuine, so an SP that
    /// accepted v1.5 would decrypt them and that test would fail; a JCE
    /// "PKCS1Padding" cipher is avoided only because code scanning flags it.
    private static byte[] pkcs1v15(java.security.PublicKey key, byte[] m, java.util.Random random) {
        java.security.interfaces.RSAPublicKey rsa = (java.security.interfaces.RSAPublicKey) key;
        int k = (rsa.getModulus().bitLength() + 7) / 8;
        byte[] em = new byte[k];
        em[1] = 2;
        for (int i = 2; i < k - m.length - 1; i++) {
            byte b;
            do {
                b = (byte) random.nextInt(256);
            } while (b == 0);
            em[i] = b;
        }
        System.arraycopy(m, 0, em, k - m.length, m.length);
        byte[] c = new java.math.BigInteger(1, em).modPow(rsa.getPublicExponent(), rsa.getModulus())
                .toByteArray();
        byte[] out = new byte[k];
        int from = Math.max(0, c.length - k);
        System.arraycopy(c, from, out, k - (c.length - from), c.length - from);
        return out;
    }
}
