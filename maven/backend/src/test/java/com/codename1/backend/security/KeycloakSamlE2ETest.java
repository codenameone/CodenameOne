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
import com.codename1.backend.security.saml2.RelyingPartyRegistration;
import com.codename1.backend.security.saml2.RelyingPartyRegistrations;
import com.codename1.impl.backend.security.SecuritySupport;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/// `saml2Login` and `saml2Logout` against a real Keycloak acting as the SAML
/// identity provider, driven as a browser would: redirects followed, the login
/// form filled in, the auto-posting forms posted. Keycloak signs the
/// assertion and the response and encrypts the assertion (AES-256-GCM, key
/// wrapped by RSA-OAEP with SHA-256), and checks this server's signed
/// AuthnRequest and LogoutRequest. Runs under
/// `maven/integration-tests/backend-identity/run.sh`; skipped otherwise.
class KeycloakSamlE2ETest {
    private static final String REALM = "cn1";
    private static final String SP = "http://localhost/saml2/service-provider-metadata/kc";
    private static String keycloak;
    private static KeyPair sp;
    private static Certificate spCert;

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            return HttpServer.Response.text(200, who == null ? "nobody" : who.getName());
        }
    };

    /// The identity provider's side of the browser: its own cookies, no Secure
    /// check (the container serves plain HTTP), redirects not followed.
    private static final class Browser {
        final Map<String, String> cookies = new LinkedHashMap<String, String>();
        int status;
        String location;
        String body;

        void call(String method, String url, String form, String bearer) throws Exception {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setRequestMethod(method);
            if (!cookies.isEmpty()) {
                StringBuilder b = new StringBuilder();
                for (Map.Entry<String, String> e : cookies.entrySet()) {
                    b.append(b.length() == 0 ? "" : "; ").append(e.getKey()).append('=')
                            .append(e.getValue());
                }
                c.setRequestProperty("Cookie", b.toString());
            }
            if (bearer != null) {
                c.setRequestProperty("Authorization", "Bearer " + bearer);
            }
            if (form != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", form.startsWith("{") || form.startsWith("<")
                        ? (form.startsWith("{") ? "application/json" : "application/xml")
                        : "application/x-www-form-urlencoded");
                try (OutputStream out = c.getOutputStream()) {
                    out.write(form.getBytes("UTF-8"));
                }
            }
            status = c.getResponseCode();
            location = c.getHeaderField("Location");
            for (Map.Entry<String, List<String>> h : c.getHeaderFields().entrySet()) {
                if (h.getKey() != null && "Set-Cookie".equalsIgnoreCase(h.getKey())) {
                    for (String v : h.getValue()) {
                        String pair = v.substring(0, v.indexOf(';') < 0 ? v.length()
                                : v.indexOf(';'));
                        int eq = pair.indexOf('=');
                        String name = pair.substring(0, eq).trim();
                        String value = pair.substring(eq + 1);
                        if (value.length() == 0 || v.contains("Max-Age=0")) {
                            cookies.remove(name);
                        } else {
                            cookies.put(name, value);
                        }
                    }
                }
            }
            InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (in != null) {
                byte[] buf = new byte[8192];
                for (int n; (n = in.read(buf)) > 0; ) {
                    out.write(buf, 0, n);
                }
                in.close();
            }
            body = out.toString("UTF-8");
        }
    }

    private static String admin;
    private static long gap;

    @BeforeAll
    static void identityProvider() throws Exception {
        keycloak = System.getenv("CN1_E2E_KEYCLOAK_URL");
        Assumptions.assumeTrue(keycloak != null,
                "no Keycloak; run maven/integration-tests/backend-identity/run.sh");
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        sp = g.generateKeyPair();
        spCert = Certificate.selfSigned(DistinguishedName.parse("CN=cn1 e2e sp"),
                sp.getPrivate().getEncoded(), 2);
        Browser b = new Browser();
        b.call("POST", keycloak + "/realms/master/protocol/openid-connect/token",
                "grant_type=password&client_id=admin-cli&username=admin&password=admin", null);
        assertEquals(200, b.status, b.body);
        admin = group(b.body, "\"access_token\":\"([^\"]+)\"");
        String payload = admin.substring(admin.indexOf('.') + 1, admin.lastIndexOf('.'));
        long issued = Long.parseLong(group(new String(Base64.getUrlDecoder().decode(payload),
                "UTF-8"), "\"iat\":(\\d+)")) * 1000L;
        gap = Math.abs(System.currentTimeMillis() - issued);
        b.call("DELETE", keycloak + "/admin/realms/" + REALM, null, admin);
        b.call("POST", keycloak + "/admin/realms",
                "{\"realm\":\"" + REALM + "\",\"enabled\":true}", admin);
        assertEquals(201, b.status, b.body);
        b.call("POST", keycloak + "/admin/realms/" + REALM + "/users",
                "{\"username\":\"ada\",\"enabled\":true,\"email\":\"ada@corp.example\","
                + "\"emailVerified\":true,\"firstName\":\"Ada\",\"lastName\":\"Lovelace\","
                + "\"credentials\":[{\"type\":\"password\",\"value\":\"s3cret\","
                + "\"temporary\":false}]}", admin);
        assertEquals(201, b.status, b.body);
    }

    private static String group(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        assertTrue(m.find(), regex + " in " + text);
        return m.group(1);
    }

    private static String html(String s) {
        return s.replace("&amp;", "&").replace("&#x3a;", ":").replace("&#x2f;", "/")
                .replace("&#x3d;", "=").replace("&#43;", "+").replace("&#x2b;", "+")
                .replace("&quot;", "\"");
    }

    private static String b64(Certificate c) throws Exception {
        String pem = c.toPem();
        return pem.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "").replace("\n", "").replace("\r", "");
    }

    private static SecuredServer start(Browser b) throws Exception {
        String acs = "http://localhost/login/saml2/sso/kc";
        String slo = "http://localhost/logout/saml2/slo/kc";
        // Our metadata must be something Keycloak can read: its converter
        // turns it into a client, which must name our ACS.
        b.call("GET", keycloak + "/realms/" + REALM + "/protocol/saml/descriptor", null, null);
        assertEquals(200, b.status);
        // The identity provider's clock, from its token's iat: a container in a
        // VM that slept can run minutes behind the host, and an assertion it
        // issues then reads as expired here. The registration is given that
        // gap on top of its usual minute; on a CI runner the gap is ~0.
        RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadata(b.body, "kc")
                .clockSkewMillis(60000L + gap)
                .signingCredential(sp.getPrivate().getEncoded(), spCert)
                .decryptionCredential(sp.getPrivate().getEncoded(), spCert)
                .build();
        assertTrue(registration.getAssertingPartySingleLogoutServiceLocation() != null);
        SecuredServer server = SecuredServer.start(APP, http -> http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .saml2Login(saml -> saml.relyingPartyRegistrationRepository(
                        new InMemoryRelyingPartyRegistrationRepository(registration))
                        .failureHandler((request, refused) -> HttpServer.Response.text(401,
                                refused.getMessage())))
                .saml2Logout(Customizer.withDefaults())
                .build());
        String metadata = server.get("/saml2/service-provider-metadata/kc").body;
        b.call("POST", keycloak + "/admin/realms/" + REALM + "/client-description-converter",
                metadata, admin);
        assertEquals(200, b.status, b.body);
        assertTrue(b.body.contains(acs), b.body);
        b.call("GET", keycloak + "/admin/realms/" + REALM + "/clients?clientId="
                + URLEncoder.encode(SP, "UTF-8"), null, admin);
        if (b.body.contains("\"id\":\"")) {
            b.call("DELETE", keycloak + "/admin/realms/" + REALM + "/clients/"
                    + group(b.body, "\"id\":\"([^\"]+)\""), null, admin);
        }
        String cert = b64(spCert);
        b.call("POST", keycloak + "/admin/realms/" + REALM + "/clients", "{\"clientId\":\"" + SP
                + "\",\"protocol\":\"saml\",\"enabled\":true,\"frontchannelLogout\":true,"
                + "\"redirectUris\":[\"http://localhost/*\"],\"attributes\":{"
                + "\"saml_assertion_consumer_url_post\":\"" + acs + "\","
                + "\"saml_single_logout_service_url_redirect\":\"" + slo + "\","
                + "\"saml_name_id_format\":\"email\",\"saml.force.name.id.format\":\"true\","
                + "\"saml.server.signature\":\"true\",\"saml.assertion.signature\":\"true\","
                + "\"saml.signature.algorithm\":\"RSA_SHA256\","
                + "\"saml.client.signature\":\"true\",\"saml.signing.certificate\":\"" + cert
                + "\",\"saml.encrypt\":\"true\",\"saml.encryption.certificate\":\"" + cert + "\","
                + "\"saml.encryption.algorithm\":\"http://www.w3.org/2009/xmlenc11#aes256-gcm\","
                + "\"saml.encryption.keyAlgorithm\":\"http://www.w3.org/2009/xmlenc11#rsa-oaep\","
                + "\"saml.encryption.digestMethod\":\"http://www.w3.org/2001/04/xmlenc#sha256\","
                + "\"saml.encryption.maskGenerationFunction\":"
                + "\"http://www.w3.org/2009/xmlenc11#mgf1sha256\"}}", admin);
        assertEquals(201, b.status, b.body);
        return server;
    }

    /// Signs ada in, from the first protected page to the session here.
    /// @return the SAMLResponse Keycloak posted, decoded
    private static String signIn(SecuredServer server, Browser b) throws Exception {
        SecuredServer.Reply first = server.get("/private");
        assertEquals(302, first.status);
        SecuredServer.Reply authn = server.get(first.header("Location"));
        assertEquals(302, authn.status, authn.body);
        b.call("GET", authn.header("Location"), null, null);
        String posted;
        if (b.body.contains("kc-form-login")) {
            assertEquals(200, b.status, b.body);
            String action = html(group(b.body, "id=\"kc-form-login\"[^>]*action=\"([^\"]+)\""));
            b.call("POST", action, "username=ada&password=s3cret&credentialId=", null);
        }
        assertEquals(200, b.status, b.body);
        posted = b.body;
        String response = html(group(posted, "name=\"SAMLResponse\"\\s+value=\"([^\"]+)\""));
        String form = "SAMLResponse=" + URLEncoder.encode(response, "UTF-8");
        if (posted.contains("name=\"RelayState\"")) {
            form += "&RelayState=" + URLEncoder.encode(
                    html(group(posted, "name=\"RelayState\"\\s+value=\"([^\"]*)\"")), "UTF-8");
        }
        SecuredServer.Reply acs = server.post("/login/saml2/sso/kc", form);
        assertEquals(302, acs.status, acs.body + " " + acs.header("Location"));
        assertTrue(!acs.header("Location").contains("error"), acs.header("Location") + " "
                + acs.headers("X-Debug"));
        assertEquals("ada@corp.example", server.get("/private").body);
        return new String(Base64.getDecoder().decode(response), "UTF-8");
    }

    @Test
    void signsInAndOutThroughKeycloak() throws Exception {
        Browser b = new Browser();
        try (SecuredServer server = start(b)) {
            String response = signIn(server, b);
            assertTrue(response.contains("EncryptedAssertion"), response);
            assertTrue(!response.contains("<saml:Assertion") && !response.contains(":Assertion "),
                    "the assertion travelled in the clear");

            SecuredServer.Reply out = server.post("/logout", "");
            assertEquals(302, out.status, out.body);
            String toIdp = out.header("Location");
            assertTrue(toIdp.startsWith(keycloak), toIdp);
            assertEquals(302, server.get("/private").status);
            b.call("GET", toIdp, null, null);
            String back;
            if (b.status == 302) {
                back = b.location;
            } else {
                assertEquals(200, b.status, b.body);
                back = html(group(b.body, "action=\"([^\"]+)\""));
            }
            assertTrue(back.startsWith("http://localhost/logout/saml2/slo/kc?"), back + " "
                    + b.body);
            SecuredServer.Reply done = server.get(back.substring("http://localhost".length()));
            assertEquals(302, done.status, done.body);
            assertEquals("/login?logout", done.header("Location"));

            // signed out at Keycloak too: the next sign-in asks for the password again
            SecuredServer.Reply again = server.get("/saml2/authenticate/kc");
            b.call("GET", again.header("Location"), null, null);
            assertTrue(b.body.contains("kc-form-login"), "Keycloak kept the session: " + b.body);
            assertTrue(URLDecoder.decode(back, "UTF-8").length() > 0);
        }
    }
}
