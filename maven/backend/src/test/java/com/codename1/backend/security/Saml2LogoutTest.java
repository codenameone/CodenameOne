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
package com.codename1.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.pki.Certificate;
import com.codename1.backend.security.pki.DistinguishedName;
import com.codename1.backend.security.saml2.InMemoryRelyingPartyRegistrationRepository;
import com.codename1.backend.security.saml2.SamlFixtures;
import com.codename1.impl.backend.security.SecuritySupport;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/// `http.saml2Logout(...)` over the whole request path, against an identity
/// provider whose messages are signed and checked by the JDK.
class Saml2LogoutTest {
    private static final String SLO = "https://idp.example.com/slo";
    private static final String OUR_SLO = "http://localhost/logout/saml2/slo/corp";
    private static final String SIG_ALG = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static SamlFixtures idp;
    private static KeyPair sp;
    private static Certificate spCert;

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            return HttpServer.Response.text(200, who == null ? "nobody" : who.getName());
        }
    };

    @BeforeAll
    static void keys() throws Exception {
        idp = new SamlFixtures();
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        sp = g.generateKeyPair();
        spCert = Certificate.selfSigned(DistinguishedName.parse("CN=sp.example.com"),
                sp.getPrivate().getEncoded(), 30);
    }

    private static SecuredServer start() throws Exception {
        return SecuredServer.start(APP, http -> http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .saml2Login(saml -> saml.relyingPartyRegistrationRepository(
                        new InMemoryRelyingPartyRegistrationRepository(idp.registration()
                                .signingCredential(sp.getPrivate().getEncoded(), spCert)
                                .assertingPartySingleLogoutServiceLocation(SLO).build())))
                .saml2Logout(Customizer.withDefaults())
                .build());
    }

    private static String param(String url, String name) throws Exception {
        int at = url.indexOf((url.indexOf('?') < 0 ? "" : "") + name + "=");
        while (at > 0 && url.charAt(at - 1) != '?' && url.charAt(at - 1) != '&') {
            at = url.indexOf(name + "=", at + 1);
        }
        if (at < 0) {
            return null;
        }
        int end = url.indexOf('&', at);
        return url.substring(at + name.length() + 1, end < 0 ? url.length() : end);
    }

    private static String inflate(String encodedParam) throws Exception {
        byte[] deflated = Base64.getDecoder().decode(URLDecoder.decode(encodedParam, "UTF-8"));
        Inflater inflater = new Inflater(true);
        inflater.setInput(deflated);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        while (!inflater.finished()) {
            int n = inflater.inflate(buf);
            if (n == 0 && inflater.needsInput()) {
                break;
            }
            out.write(buf, 0, n);
        }
        return out.toString("UTF-8");
    }

    private static String attr(String xml, String name) {
        int at = xml.indexOf(" " + name + "=\"") + name.length() + 3;
        return xml.substring(at, xml.indexOf('"', at));
    }

    /// A message from the identity provider, by HTTP-Redirect, signed with
    /// `key` (null: unsigned).
    private static String redirect(String param, String xml, KeyPair key) throws Exception {
        Deflater d = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        d.setInput(xml.getBytes("UTF-8"));
        d.finish();
        byte[] buf = new byte[8192];
        int n = d.deflate(buf);
        String value = URLEncoder.encode(Base64.getEncoder().encodeToString(
                java.util.Arrays.copyOf(buf, n)), "UTF-8");
        String q = param + "=" + value + "&RelayState=rs1&SigAlg="
                + URLEncoder.encode(SIG_ALG, "UTF-8");
        if (key == null) {
            return q;
        }
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(key.getPrivate());
        s.update(q.getBytes("UTF-8"));
        return q + "&Signature=" + URLEncoder.encode(
                Base64.getEncoder().encodeToString(s.sign()), "UTF-8");
    }

    private static String logoutRequest(String nameId) {
        return "<samlp:LogoutRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" "
                + "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"_lr"
                + System.nanoTime() + "\" Version=\"2.0\" IssueInstant=\""
                + SamlFixtures.time(System.currentTimeMillis()) + "\" Destination=\"" + OUR_SLO
                + "\"><saml:Issuer>" + SamlFixtures.IDP + "</saml:Issuer><saml:NameID>" + nameId
                + "</saml:NameID><samlp:SessionIndex>_session1</samlp:SessionIndex>"
                + "</samlp:LogoutRequest>";
    }

    private static void signIn(SecuredServer server) throws Exception {
        String location = server.get("/saml2/authenticate/corp").header("Location");
        String xml = inflate(param(location, "SAMLRequest"));
        SamlFixtures.Spec s = new SamlFixtures.Spec();
        s.inResponseTo = attr(xml, "ID");
        s.assertionId = "_slo" + System.nanoTime();
        SecuredServer.Reply acs = server.post("/login/saml2/sso/corp", "SAMLResponse="
                + URLEncoder.encode(Base64.getEncoder().encodeToString(
                idp.xml(s).getBytes("UTF-8")), "UTF-8"));
        assertEquals(302, acs.status, acs.body);
        assertEquals("alice@corp.example", server.get("/private").body);
    }

    @Test
    void signingOutHereSignsOutAtTheIdentityProvider() throws Exception {
        try (SecuredServer server = start()) {
            signIn(server);
            SecuredServer.Reply out = server.post("/logout", "");
            assertEquals(302, out.status, out.body);
            String location = out.header("Location");
            assertTrue(location.startsWith(SLO + "?SAMLRequest="), location);
            // the JDK checks our signature over the query, as an IdP would
            String q = location.substring(location.indexOf('?') + 1);
            String signed = q.substring(0, q.indexOf("&Signature="));
            Signature v = Signature.getInstance("SHA256withRSA");
            v.initVerify(sp.getPublic());
            v.update(signed.getBytes("UTF-8"));
            assertTrue(v.verify(Base64.getDecoder().decode(URLDecoder.decode(
                    param(location, "Signature"), "UTF-8"))));
            String request = inflate(param(location, "SAMLRequest"));
            assertTrue(request.contains(">alice@corp.example</saml:NameID>"), request);
            assertTrue(request.contains("<samlp:SessionIndex>_session1</samlp:SessionIndex>"),
                    request);
            assertEquals(302, server.get("/private").status);
            String response = "<samlp:LogoutResponse xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:"
                    + "protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"_r1\" "
                    + "Version=\"2.0\" IssueInstant=\""
                    + SamlFixtures.time(System.currentTimeMillis()) + "\" Destination=\"" + OUR_SLO
                    + "\" InResponseTo=\"" + attr(request, "ID") + "\"><saml:Issuer>"
                    + SamlFixtures.IDP + "</saml:Issuer><samlp:Status><samlp:StatusCode "
                    + "Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status>"
                    + "</samlp:LogoutResponse>";
            String answer = "/logout/saml2/slo/corp?" + redirect("SAMLResponse", response, idp.keys);
            SecuredServer.Reply done = server.get(answer);
            assertEquals(302, done.status, done.body);
            assertEquals("/login?logout", done.header("Location"));
            // the request cookie is gone: the same answer again is refused
            assertEquals(400, server.get(answer).status);
        }
    }

    @Test
    void identityProviderSignsTheUserOut() throws Exception {
        try (SecuredServer server = start()) {
            signIn(server);
            SecuredServer.Reply r = server.get("/logout/saml2/slo/corp?"
                    + redirect("SAMLRequest", logoutRequest("alice@corp.example"), idp.keys));
            assertEquals(302, r.status, r.body);
            String location = r.header("Location");
            assertTrue(location.startsWith(SLO + "?SAMLResponse="), location);
            assertEquals("rs1", param(location, "RelayState"));
            assertTrue(inflate(param(location, "SAMLResponse"))
                    .contains("status:Success"));
            assertEquals(302, server.get("/private").status);
        }
    }

    @Test
    void refusesUnsignedAndForgedRequests() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair attacker = g.generateKeyPair();
        try (SecuredServer server = start()) {
            signIn(server);
            assertEquals(400, server.get("/logout/saml2/slo/corp?"
                    + redirect("SAMLRequest", logoutRequest("alice@corp.example"), null)).status);
            assertEquals(400, server.get("/logout/saml2/slo/corp?"
                    + redirect("SAMLRequest", logoutRequest("alice@corp.example"), attacker))
                    .status);
            // a signature over one message does not carry to another
            String good = redirect("SAMLRequest", logoutRequest("bob@corp.example"), idp.keys);
            String other = redirect("SAMLRequest", logoutRequest("alice@corp.example"), null);
            assertEquals(400, server.get("/logout/saml2/slo/corp?" + other
                    + good.substring(good.indexOf("&Signature="))).status);
            assertEquals("alice@corp.example", server.get("/private").body);
        }
    }

    @Test
    void requestForSomeoneElseLeavesTheSession() throws Exception {
        try (SecuredServer server = start()) {
            signIn(server);
            SecuredServer.Reply r = server.get("/logout/saml2/slo/corp?"
                    + redirect("SAMLRequest", logoutRequest("bob@corp.example"), idp.keys));
            assertEquals(302, r.status, r.body);
            assertTrue(inflate(param(r.header("Location"), "SAMLResponse"))
                    .contains("status:Requester"));
            assertEquals("alice@corp.example", server.get("/private").body);
        }
    }

    @Test
    void metadataListsTheLogoutAddress() throws Exception {
        try (SecuredServer server = start()) {
            SecuredServer.Reply md = server.get("/saml2/service-provider-metadata/corp");
            assertEquals(200, md.status);
            assertTrue(md.body.contains("SingleLogoutService"), md.body);
            assertTrue(md.body.contains("Location=\"" + OUR_SLO + "\""), md.body);
        }
    }
}
