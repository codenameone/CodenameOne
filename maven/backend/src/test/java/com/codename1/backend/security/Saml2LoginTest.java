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
import com.codename1.backend.security.saml2.InMemoryRelyingPartyRegistrationRepository;
import com.codename1.backend.security.saml2.Saml2Authentication;
import com.codename1.backend.security.saml2.SamlFixtures;
import com.codename1.impl.backend.security.SecuritySupport;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.zip.Inflater;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/// `http.saml2Login(...)` through the whole request path: the redirect to the
/// identity provider, the posted response, the session that follows.
class Saml2LoginTest {
    private static SamlFixtures idp;

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            String groups = who instanceof Saml2Authentication
                    ? ((Saml2Authentication) who).getPrincipal().getAttribute("groups").toString()
                    : "-";
            return HttpServer.Response.text(200, who == null ? "nobody"
                    : who.getName() + " " + who.getAuthorities() + " " + groups);
        }
    };

    @BeforeAll
    static void keys() throws Exception {
        idp = new SamlFixtures();
    }

    private static SecuredServer start() throws Exception {
        return SecuredServer.start(APP, http -> http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .saml2Login(saml -> saml.relyingPartyRegistrationRepository(
                        new InMemoryRelyingPartyRegistrationRepository(
                                idp.registration().build())))
                .build());
    }

    private static String requestId(String location) throws Exception {
        String q = location.substring(location.indexOf("SAMLRequest=") + 12);
        int amp = q.indexOf('&');
        byte[] deflated = Base64.getDecoder().decode(URLDecoder.decode(
                amp < 0 ? q : q.substring(0, amp), "UTF-8"));
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
        String xml = out.toString("UTF-8");
        assertTrue(xml.contains("AssertionConsumerServiceURL=\"" + SamlFixtures.ACS + "\""), xml);
        int at = xml.indexOf(" ID=\"") + 5;
        return xml.substring(at, xml.indexOf('"', at));
    }

    private static String form(String xml) throws Exception {
        return "SAMLResponse=" + URLEncoder.encode(
                Base64.getEncoder().encodeToString(xml.getBytes("UTF-8")), "UTF-8");
    }

    @Test
    void signsInThroughTheIdentityProvider() throws Exception {
        try (SecuredServer server = start()) {
            SecuredServer.Reply first = server.get("/private");
            assertEquals(302, first.status);
            assertEquals("/saml2/authenticate/corp", first.header("Location"));
            SecuredServer.Reply authn = server.get("/saml2/authenticate/corp");
            assertEquals(302, authn.status);
            String location = authn.header("Location");
            assertTrue(location.startsWith(SamlFixtures.SSO + "?SAMLRequest="), location);
            SamlFixtures.Spec s = new SamlFixtures.Spec();
            s.inResponseTo = requestId(location);
            s.assertionId = "_e2e" + System.nanoTime();
            SecuredServer.Reply acs = server.post("/login/saml2/sso/corp", form(idp.xml(s)));
            assertEquals(302, acs.status, acs.body);
            assertEquals("/private", acs.header("Location"));
            SecuredServer.Reply page = server.get("/private");
            assertEquals(200, page.status);
            assertEquals("alice@corp.example [ROLE_USER] [admins, staff]", page.body);
        }
    }

    @Test
    void refusesResponseToAnotherRequest() throws Exception {
        try (SecuredServer server = start()) {
            server.get("/saml2/authenticate/corp");
            SamlFixtures.Spec s = new SamlFixtures.Spec();
            s.inResponseTo = "ARQnot-ours";
            s.assertionId = "_e2e" + System.nanoTime();
            SecuredServer.Reply acs = server.post("/login/saml2/sso/corp", form(idp.xml(s)));
            assertEquals(302, acs.status);
            assertEquals("/login?error", acs.header("Location"));
            assertEquals("nobody", server.get("/private").status == 302 ? "nobody" : "someone");
        }
    }

    @Test
    void servesMetadataUnauthenticated() throws Exception {
        try (SecuredServer server = start()) {
            SecuredServer.Reply md = server.get("/saml2/service-provider-metadata/corp");
            assertEquals(200, md.status);
            assertTrue(md.body.contains("entityID=\"" + SamlFixtures.SP + "\""), md.body);
            assertTrue(md.body.contains("Location=\"" + SamlFixtures.ACS + "\""), md.body);
        }
    }
}
