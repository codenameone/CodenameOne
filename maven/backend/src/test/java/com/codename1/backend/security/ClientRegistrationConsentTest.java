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

import static com.codename1.backend.security.OAuth2Testing.form;
import static com.codename1.backend.security.OAuth2Testing.json;
import static com.codename1.backend.security.OAuth2Testing.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationConsentPolicy;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.ClientRegistrationException;
import com.codename1.backend.security.oauth2.server.authorization.ClientRegistrationPolicy;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClientRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

/// #6310: clients registering themselves (RFC 7591), and the consent page
/// that grants exactly the scopes the user approved.
class ClientRegistrationConsentTest {
    private static final String JSON = "application/json";
    private static final String FORM = "application/x-www-form-urlencoded";
    private static final String CALLBACK = "http://127.0.0.1:43123/callback";

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return HttpServer.Response.text(200, "app " + request.pathFrom(0));
        }
    };

    private final InMemoryOAuth2AuthorizationService grants =
            new InMemoryOAuth2AuthorizationService();
    private final OAuth2Testing.Ticking clock = new OAuth2Testing.Ticking();
    private final InMemoryRegisteredClientRepository stored = AuthorizationServerTest.clients()
            instanceof InMemoryRegisteredClientRepository
            ? (InMemoryRegisteredClientRepository) AuthorizationServerTest.clients() : null;
    /// What the policy was last shown.
    private Map<String, Object> lastMetadata;

    /// A repository kept "somewhere else": it cannot list its clients.
    private RegisteredClientRepository table() {
        return new RegisteredClientRepository() {
            @Override
            public void save(RegisteredClient registeredClient) {
                stored.save(registeredClient);
            }

            @Override
            public RegisteredClient findById(String id) {
                return stored.findById(id);
            }

            @Override
            public RegisteredClient findByClientId(String clientId) {
                return stored.findByClientId(clientId);
            }
        };
    }

    private ClientRegistrationPolicy policy() {
        return new ClientRegistrationPolicy() {
            @Override
            public Set<String> allowedScopes() {
                return new LinkedHashSet<String>(Arrays.asList("read", "write"));
            }

            @Override
            public RegisteredClient validate(HttpServer.Request request,
                    Map<String, Object> metadata, RegisteredClient client) {
                lastMetadata = metadata;
                if ("Blocked".equals(client.getClientName())) {
                    throw new ClientRegistrationException(
                            ClientRegistrationException.INVALID_CLIENT_METADATA, "Not that one");
                }
                return client;
            }
        };
    }

    private SecuredServer start(RegisteredClientRepository clients,
            Customizer<AuthorizationServerConfigurer> more) throws Exception {
        int port = OAuth2Testing.freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(AuthorizationServerSettings.ISSUER, "http://127.0.0.1:" + port);
        return SecuredServer.start(settings, "test", new Object[] {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build())}, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .authorizationServer(as -> {
                            as.registeredClientRepository(clients).authorizationService(grants)
                                    .clientSecretEncoder(OAuth2Testing.PLAIN).clock(clock);
                            more.customize(as);
                        }).build());
    }

    private SecuredServer startRegistering() throws Exception {
        return start(table(), as -> as.clientRegistration(policy()));
    }

    private static Reply register(SecuredServer server, String body) throws Exception {
        return server.call("POST", "/oauth2/register", body, JSON);
    }

    private static String registered(SecuredServer server) throws Exception {
        Reply reply = register(server, "{\"client_name\":\"Agent\",\"redirect_uris\":[\""
                + "http://127.0.0.1/callback\"],\"grant_types\":[\"authorization_code\","
                + "\"refresh_token\"]}");
        assertEquals(201, reply.status, reply.toString());
        return (String) json(reply).get("client_id");
    }

    private static String authorizeUrl(String client, String scope, String verifier,
            String state, String prompt) {
        return "/oauth2/authorize?" + form("response_type", "code", "client_id", client,
                "redirect_uri", CALLBACK, "scope", scope, "state", state, "prompt", prompt,
                "code_challenge", OAuth2Parameters.sha256(verifier),
                "code_challenge_method", "S256");
    }

    /// Signs ada in on the way to `url`; answers what `url` then answers.
    private static Reply signedIn(SecuredServer server, String url) throws Exception {
        Reply reply = server.get(url, "Accept", "text/html");
        if (reply.status == 302 && "/login".equals(reply.header("Location"))) {
            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            reply = server.get(url, "Accept", "text/html");
        }
        return reply;
    }

    private static String action(String html) {
        String marker = "action=\"";
        int at = html.indexOf(marker) + marker.length();
        return html.substring(at, html.indexOf('"', at)).replace("&amp;", "&");
    }

    // ----------------------------------------------------------- registration

    @Test
    void registrationIsOffUnlessTurnedOn() throws Exception {
        try (SecuredServer server = start(table(), as -> { })) {
            Map metadata = json(server.get("/.well-known/oauth-authorization-server"));
            assertFalse(metadata.containsKey("registration_endpoint"), metadata.toString());
            assertTrue(register(server, "{\"redirect_uris\":[\"" + CALLBACK + "\"]}").status
                    != 201);
        }
    }

    @Test
    void registrationRefusesAnInMemoryRepository() {
        Exception failed = assertThrows(Exception.class, () -> start(
                AuthorizationServerTest.clients(), as -> as.clientRegistration(policy())).close());
        assertTrue(String.valueOf(failed.getMessage()).contains("in memory")
                || String.valueOf(failed.getCause()).contains("in memory"), failed.toString());
    }

    @Test
    void aClientRegistersAndIsPersistedAsAPublicPkceClientThatAsksForConsent()
            throws Exception {
        try (SecuredServer server = startRegistering()) {
            Map metadata = json(server.get("/.well-known/oauth-authorization-server"));
            assertEquals(metadata.get("issuer") + "/oauth2/register",
                    metadata.get("registration_endpoint"));
            // No CSRF token: a client calls this, not a browser session.
            Reply reply = register(server, "{\"client_name\":\"Agent\",\"redirect_uris\":[\""
                    + CALLBACK + "\",\"https://agent.example/cb\"]}");
            assertEquals(201, reply.status, reply.toString());
            assertEquals("no-store", reply.header("Cache-Control"));
            Map answer = json(reply);
            assertEquals("none", answer.get("token_endpoint_auth_method"));
            assertEquals("read write", answer.get("scope"));
            assertEquals(Arrays.asList("authorization_code"), answer.get("grant_types"));
            assertEquals("Agent", lastMetadata.get("client_name"));
            RegisteredClient saved = stored.findByClientId((String) answer.get("client_id"));
            assertNotNull(saved);
            assertTrue(saved.isPublic());
            assertTrue(saved.getClientSettings().isRequireProofKey());
            assertTrue(saved.getClientSettings().isRequireAuthorizationConsent());
            assertTrue(saved.getResources().isEmpty());
        }
    }

    @Test
    void registrationsOutsideThePolicyAreRefused() throws Exception {
        try (SecuredServer server = startRegistering()) {
            String[][] cases = {
                {"{\"redirect_uris\":[\"http://evil.example/cb\"]}", "invalid_redirect_uri"},
                {"{\"redirect_uris\":[\"https://a.example/cb#frag\"]}", "invalid_redirect_uri"},
                {"{\"redirect_uris\":[\"https://user@a.example/cb\"]}", "invalid_redirect_uri"},
                {"{\"redirect_uris\":[]}", "invalid_redirect_uri"},
                {"{\"redirect_uris\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"]}",
                    "invalid_redirect_uri"},
                {"{\"redirect_uris\":[\"" + CALLBACK + "\"],\"token_endpoint_auth_method\":"
                    + "\"client_secret_basic\"}", "invalid_client_metadata"},
                {"{\"redirect_uris\":[\"" + CALLBACK + "\"],\"scope\":\"read admin\"}",
                    "invalid_client_metadata"},
                {"{\"redirect_uris\":[\"" + CALLBACK + "\"],\"grant_types\":"
                    + "[\"client_credentials\"]}", "invalid_client_metadata"},
                {"{\"redirect_uris\":[\"" + CALLBACK + "\"],\"response_types\":[\"token\"]}",
                    "invalid_client_metadata"},
                {"{\"redirect_uris\":[\"" + CALLBACK + "\"],\"client_name\":\"Blocked\"}",
                    "invalid_client_metadata"},
                {"[1,2]", "invalid_client_metadata"},
                {"not json", "invalid_client_metadata"},
            };
            for (String[] c : cases) {
                Reply reply = register(server, c[0]);
                assertEquals(400, reply.status, c[0] + " -> " + reply);
                assertEquals(c[1], json(reply).get("error"), c[0] + " -> " + reply);
            }
            assertEquals(400, server.call("POST", "/oauth2/register", "redirect_uris=x", FORM)
                    .status);
        }
    }

    // ---------------------------------------------------------------- consent

    @Test
    void consentGrantsExactlyTheApprovedScopes() throws Exception {
        try (SecuredServer server = startRegistering()) {
            String client = registered(server);
            String verifier = OAuth2Parameters.random(32);
            Reply page = signedIn(server, authorizeUrl(client, "read write", verifier, "st-1",
                    null));
            assertEquals(200, page.status, page.toString());
            assertEquals("frame-ancestors 'none'", page.header("Content-Security-Policy"));
            assertEquals("no-store", page.header("Cache-Control"));
            assertTrue(page.body.contains("Agent"), page.body);
            assertTrue(page.body.contains("an application on this computer"), page.body);
            String ticket = OAuth2Testing.field(page.body, "ticket");
            String csrf = OAuth2Testing.field(page.body, "_csrf");
            String action = action(page.body);

            // Without the CSRF token, the chain refuses the post.
            assertEquals(403, server.call("POST", action, form("ticket", ticket, "decision",
                    "approve", "scope", "read"), FORM).status);
            // The same question again: the refusal above did not reach it.
            page = signedIn(server, authorizeUrl(client, "read write", verifier, "st-1", null));
            ticket = OAuth2Testing.field(page.body, "ticket");
            csrf = OAuth2Testing.field(page.body, "_csrf");

            // Only "read" ticked, plus a scope that was never asked for.
            Reply back = server.call("POST", action, form("_csrf", csrf, "ticket", ticket,
                    "decision", "approve", "scope", "read", "scope", "admin"), FORM);
            assertEquals(302, back.status, back.toString());
            Map<String, String> answer = query(back.header("Location"));
            assertTrue(back.header("Location").startsWith(CALLBACK + "?"));
            assertEquals("st-1", answer.get("state"));
            Reply tokens = server.call("POST", "/oauth2/token", form("grant_type",
                    "authorization_code", "client_id", client, "code", answer.get("code"),
                    "redirect_uri", CALLBACK, "code_verifier", verifier), FORM);
            assertEquals(200, tokens.status, tokens.toString());
            assertEquals("read", json(tokens).get("scope"));

            // The ticket was used: posting it again is refused.
            Reply replay = server.call("POST", action, form("_csrf", csrf, "ticket", ticket,
                    "decision", "approve", "scope", "read"), FORM);
            assertEquals(400, replay.status, replay.toString());
        }
    }

    @Test
    void twoPendingQuestionsInOneSessionAreEachAnswerable() throws Exception {
        try (SecuredServer server = startRegistering()) {
            String client = registered(server);
            String verifier = OAuth2Parameters.random(32);
            // Two tabs: the second question must not overwrite the first.
            Reply first = signedIn(server, authorizeUrl(client, "read", verifier, "tab-1", null));
            Reply second = signedIn(server, authorizeUrl(client, "read", verifier, "tab-2", null));
            for (Reply page : new Reply[] {first, second}) {
                Reply back = server.call("POST", action(page.body), form("_csrf",
                        OAuth2Testing.field(second.body, "_csrf"), "ticket",
                        OAuth2Testing.field(page.body, "ticket"), "decision", "approve", "scope", "read"), FORM);
                assertEquals(302, back.status, back.toString());
                assertTrue(back.header("Location").contains("code="), back.header("Location"));
            }
        }
    }

    @Test
    void anAnswerCountsOnlyForTheRequestItWasAskedAbout() throws Exception {
        try (SecuredServer server = startRegistering()) {
            String client = registered(server);
            String verifier = OAuth2Parameters.random(32);
            Reply page = signedIn(server, authorizeUrl(client, "read", verifier, "st-1", null));
            String ticket = OAuth2Testing.field(page.body, "ticket");
            String csrf = OAuth2Testing.field(page.body, "_csrf");
            // The same ticket, posted to a request that asks for more.
            Reply other = server.call("POST", authorizeUrl(client, "read write", verifier,
                    "st-1", null), form("_csrf", csrf, "ticket", ticket, "decision", "approve",
                    "scope", "read write"), FORM);
            assertEquals(400, other.status, other.toString());
            // A post with no question asked at all.
            Reply none = server.call("POST", authorizeUrl(client, "read", verifier, "st-2",
                    null), form("_csrf", csrf, "ticket", "made-up", "decision", "approve"), FORM);
            assertEquals(400, none.status, none.toString());
        }
    }

    @Test
    void deniedOrNothingApprovedIsAccessDenied() throws Exception {
        try (SecuredServer server = startRegistering()) {
            String client = registered(server);
            String verifier = OAuth2Parameters.random(32);
            for (String[] answer : new String[][] {{"deny", "read"}, {"approve", null}}) {
                Reply page = signedIn(server, authorizeUrl(client, "read", verifier, "st-3",
                        null));
                Reply back = server.call("POST", action(page.body), form("_csrf",
                        OAuth2Testing.field(page.body, "_csrf"), "ticket",
                        OAuth2Testing.field(page.body, "ticket"), "decision", answer[0],
                        "scope", answer[1]), FORM);
                assertEquals(302, back.status, back.toString());
                assertEquals("access_denied", query(back.header("Location")).get("error"));
            }
        }
    }

    @Test
    void promptNoneIsConsentRequired() throws Exception {
        try (SecuredServer server = startRegistering()) {
            String client = registered(server);
            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            Reply back = server.get(authorizeUrl(client, "read", OAuth2Parameters.random(32),
                    "st-4", "none"), "Accept", "text/html");
            assertEquals(302, back.status, back.toString());
            assertEquals("consent_required", query(back.header("Location")).get("error"));
        }
    }

    @Test
    void aPolicyAsksForAnyClientAndTheApplicationCanShowItsOwnPage() throws Exception {
        final List<String> asked = new java.util.ArrayList<String>();
        AuthorizationConsentPolicy policy = (client, scopes, user) -> {
            asked.add(client.getClientId() + " " + scopes + " " + user.getName());
            return scopes.contains("orders:read");
        };
        try (SecuredServer server = start(AuthorizationServerTest.clients(),
                as -> as.authorizationConsent(policy, "/my-consent"))) {
            String verifier = OAuth2Parameters.random(32);
            String url = "/oauth2/authorize?" + form("response_type", "code", "client_id", "app",
                    "redirect_uri", "http://127.0.0.1/cb", "scope", "openid orders:read",
                    "state", "st-5", "code_challenge", OAuth2Parameters.sha256(verifier),
                    "code_challenge_method", "S256");
            Reply sent = signedIn(server, url);
            assertEquals(302, sent.status, sent.toString());
            String location = sent.header("Location");
            assertTrue(location.startsWith("/my-consent?"), location);
            Map<String, String> q = query(location);
            assertEquals("app", q.get("client_id"));
            assertEquals("openid orders:read", q.get("scope"));
            assertEquals(url, q.get("action"));
            assertEquals(Arrays.asList("app [openid, orders:read] ada"), asked);
            // The application's page posts back; openid comes with the sign-in.
            Reply login = server.get("/login");
            String csrf = OAuth2Testing.field(login.body, "_csrf");
            Reply back = server.call("POST", q.get("action"), form("_csrf", csrf, "ticket",
                    q.get("ticket"), "decision", "approve", "scope", "orders:read"), FORM);
            assertEquals(302, back.status, back.toString());
            Reply tokens = server.call("POST", "/oauth2/token", form("grant_type",
                    "authorization_code", "client_id", "app", "code",
                    query(back.header("Location")).get("code"), "redirect_uri",
                    "http://127.0.0.1/cb", "code_verifier", verifier), FORM);
            assertEquals("openid orders:read", json(tokens).get("scope"));
            // Not asked when the policy says no.
            Reply quiet = server.get("/oauth2/authorize?" + form("response_type", "code",
                    "client_id", "app", "redirect_uri", "http://127.0.0.1/cb", "scope", "openid",
                    "state", "st-6", "code_challenge", OAuth2Parameters.sha256(verifier),
                    "code_challenge_method", "S256"), "Accept", "text/html");
            assertTrue(quiet.header("Location").startsWith("http://127.0.0.1/cb?code="),
                    quiet.toString());
        }
    }
}
