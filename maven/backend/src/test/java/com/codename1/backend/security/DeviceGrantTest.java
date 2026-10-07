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

import static com.codename1.backend.security.OAuth2Testing.field;
import static com.codename1.backend.security.OAuth2Testing.form;
import static com.codename1.backend.security.OAuth2Testing.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimiter;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The device grant (RFC 8628): a device with no browser asks, a user with one
/// answers, and the device's polling is told each state in turn.
class DeviceGrantTest {
    private static final String FORM = "application/x-www-form-urlencoded";
    private static final String GRANT = "urn:ietf:params:oauth:grant-type:device_code";
    private static final String PAGE = "/oauth2/device_verification";

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return HttpServer.Response.text(200, "app");
        }
    };

    private final OAuth2Testing.Ticking clock = new OAuth2Testing.Ticking();
    private final InMemoryOAuth2AuthorizationService grants = new InMemoryOAuth2AuthorizationService();
    private String issuer;

    private SecuredServer start(Customizer<AuthorizationServerConfigurer> more) throws Exception {
        return start(more, grants);
    }

    private SecuredServer start(Customizer<AuthorizationServerConfigurer> more,
            OAuth2AuthorizationService store) throws Exception {
        int port = OAuth2Testing.freePort();
        issuer = "http://127.0.0.1:" + port;
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(AuthorizationServerSettings.ISSUER, issuer);
        Object[] beans = {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build())};
        return SecuredServer.start(settings, "test", beans, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .authorizationServer(as -> {
                            as.registeredClientRepository(AuthorizationServerTest.clients())
                                    .authorizationService(store)
                                    .clientSecretEncoder(OAuth2Testing.PLAIN).clock(clock);
                            more.customize(as);
                        }).build());
    }

    private static Map begin(SecuredServer server, String scope) throws Exception {
        Reply reply = server.call("POST", "/oauth2/device_authorization", form("client_id", "app",
                "scope", scope), FORM);
        assertEquals(200, reply.status, reply.toString());
        assertEquals("no-store", reply.header("Cache-Control"));
        return json(reply);
    }

    private static final String JSON = "application/json";

    private static Reply postJson(SecuredServer server, String csrf, String... pairs)
            throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<String, Object>();
        for (int iter = 0 ; iter + 1 < pairs.length ; iter += 2) {
            body.put(pairs[iter], pairs[iter + 1]);
        }
        String text = com.codename1.backend.Json.write(body);
        return csrf == null ? server.call("POST", PAGE, text, JSON, "Accept", JSON)
                : server.call("POST", PAGE, text, JSON, "Accept", JSON, "X-CSRF-TOKEN", csrf);
    }

    @Test
    void simultaneousDevicePollsOnlyAdmitOneRequest() throws Exception {
        final int count = 8;
        final java.util.concurrent.CountDownLatch read = new java.util.concurrent.CountDownLatch(count);
        OAuth2AuthorizationService racing = (OAuth2AuthorizationService) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {OAuth2AuthorizationService.class},
                (proxy, method, args) -> {
                    Object result = method.invoke(grants, args);
                    if ("findToken".equals(method.getName())
                            && OAuth2AuthorizationService.DEVICE_CODE.equals(args[0])) {
                        read.countDown();
                        assertTrue(read.await(10, java.util.concurrent.TimeUnit.SECONDS));
                    }
                    return result;
                });
        try (SecuredServer server = start(as -> { }, racing)) {
            Map device = begin(server, "openid");
            java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(count);
            try {
                List<java.util.concurrent.Future<String>> replies = new ArrayList<java.util.concurrent.Future<String>>();
                for (int i = 0; i < count; i++) {
                    replies.add(workers.submit(() -> (String) json(poll(server, device.get("device_code"))).get("error")));
                }
                int pending = 0;
                int slowed = 0;
                for (java.util.concurrent.Future<String> reply : replies) {
                    String error = reply.get(15, java.util.concurrent.TimeUnit.SECONDS);
                    if ("authorization_pending".equals(error)) { pending++; }
                    if ("slow_down".equals(error)) { slowed++; }
                }
                assertEquals(1, pending, "only one concurrent poll may claim the interval");
                assertEquals(count - 1, slowed);
            } finally {
                workers.shutdownNow();
            }
        }
    }

    @Test
    void invalidDecisionDoesNotSpendTheTicketOrDenyTheGrant() throws Exception {
        try (SecuredServer server = start(as -> { })) {
            Map device = begin(server, "openid");
            String code = (String) device.get("user_code");
            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            String csrf = (String) ((Map) json(server.get(PAGE, "Accept", JSON)).get("csrf")).get("token");
            String ticket = (String) json(postJson(server, csrf, "user_code", code)).get("ticket");
            for (String invalid : new String[] {"approved", "", "DENY", " approve"}) {
                Reply response = postJson(server, csrf, "user_code", code,
                        "ticket", ticket, "decision", invalid);
                assertEquals(400, response.status, response.toString());
                assertEquals("invalid_request", json(response).get("error"));
                refused(pollLater(server, device), "authorization_pending");
            }
            assertEquals(200, postJson(server, csrf, "user_code", code,
                    "ticket", ticket, "decision", "approve").status);
            assertEquals(200, pollLater(server, device).status);
        }
    }

    @Test
    @DisplayName("the verification endpoint as JSON: the same steps and the same protections, for an app's own screen")
    void theJsonForm() throws Exception {
        try (SecuredServer server = start(as -> { })) {
            Map device = begin(server, "openid profile");
            String userCode = (String) device.get("user_code");

            // Signed out, a program is told so; it is not sent to a login page.
            Reply signedOut = server.get(PAGE, "Accept", JSON);
            assertEquals(401, signedOut.status, signedOut.toString());
            assertEquals("login_required", json(signedOut).get("error"));

            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            Reply first = server.get(PAGE, "Accept", JSON);
            assertEquals(200, first.status, first.toString());
            assertEquals("no-store", first.header("Cache-Control"));
            assertEquals("frame-ancestors 'none'", first.header("Content-Security-Policy"));
            Map csrfInfo = (Map) json(first).get("csrf");
            assertEquals("X-CSRF-TOKEN", csrfInfo.get("headerName"));
            assertEquals("_csrf", csrfInfo.get("parameterName"));
            String csrf = (String) csrfInfo.get("token");
            // The same token the page would have carried.
            assertNotNull(csrf);

            // CSRF protection covers the JSON posts as it covers the form's.
            assertEquals(403, postJson(server, null, "user_code", userCode).status);
            refused(pollLater(server, device), "authorization_pending");

            // A wrong code, and what a body that is not JSON is called.
            Reply wrong = postJson(server, csrf, "user_code", "BBBB-BBBB");
            assertEquals(400, wrong.status, wrong.toString());
            assertEquals("invalid_grant", json(wrong).get("error"));
            Reply garbage = server.call("POST", PAGE, "user_code=" + userCode, JSON, "Accept",
                    JSON, "X-CSRF-TOKEN", csrf);
            assertEquals(400, garbage.status, garbage.toString());
            assertEquals("invalid_request", json(garbage).get("error"));

            // The lookup: who is asking, for what, and the ticket of the question.
            Reply looked = postJson(server, csrf, "user_code", userCode.toLowerCase());
            assertEquals(200, looked.status, looked.toString());
            Map question = json(looked);
            assertEquals(userCode, question.get("user_code"));
            assertEquals("app", question.get("client_id"));
            assertEquals("Acme App", question.get("client_name"));
            assertEquals("openid profile", question.get("scope"));
            assertEquals("ada", question.get("principal"));
            String ticket = (String) question.get("ticket");
            assertEquals(43, ticket.length());
            refused(pollLater(server, device), "authorization_pending");

            // An answer without the ticket, or with another's, decides nothing
            // -- and spends the question it was not the answer to.
            Reply forged = postJson(server, csrf, "user_code", userCode, "decision", "approve",
                    "ticket", OAuth2Parameters.random(32));
            assertEquals(400, forged.status, forged.toString());
            assertEquals("invalid_request", json(forged).get("error"));
            assertTrue(forged.body.contains("does not carry the ticket"), forged.body);
            Reply late = postJson(server, csrf, "user_code", userCode, "decision", "approve",
                    "ticket", ticket);
            assertEquals(400, late.status, "the ticket is good once: " + late);
            assertEquals(400, postJson(server, csrf, "user_code", userCode, "decision",
                    "approve").status);
            refused(pollLater(server, device), "authorization_pending");

            // Asked again and answered with that question's ticket.
            ticket = (String) json(postJson(server, csrf, "user_code", userCode)).get("ticket");
            Reply approved = postJson(server, csrf, "user_code", userCode, "decision", "approve",
                    "ticket", ticket);
            assertEquals(200, approved.status, approved.toString());
            assertEquals("{\"status\":\"approved\"}", approved.body);
            Reply reply = pollLater(server, device);
            assertEquals(200, reply.status, reply.toString());
            assertEquals("ada", OAuth2Testing.verify((String) json(reply).get("access_token"),
                    server.get("/oauth2/jwks").body).getSubject());
            // The code worked once.
            assertEquals("invalid_grant", json(postJson(server, csrf, "user_code", userCode))
                    .get("error"));

            // Refusing, the same way.
            Map second = begin(server, "openid");
            String code = (String) second.get("user_code");
            ticket = (String) json(postJson(server, csrf, "user_code", code)).get("ticket");
            Reply denied = postJson(server, csrf, "user_code", code, "decision", "deny",
                    "ticket", ticket);
            assertEquals("{\"status\":\"denied\"}", denied.body);
            refused(pollLater(server, second), "access_denied");

            // A browser on the same endpoint still gets the page.
            assertTrue(server.get(PAGE, "Accept", "text/html,application/json;q=0.9").body
                    .contains("<form method=\"post\""));
        }
    }

    @Test
    @DisplayName("tries through the JSON form are counted with the page's, and limited as slow_down")
    void theJsonFormIsLimited() throws Exception {
        try (SecuredServer server = startConfigured("3", "90", null, as -> { })) {
            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            Map device = begin(server, "openid");
            String csrf = (String) ((Map) json(server.get(PAGE, "Accept", JSON)).get("csrf"))
                    .get("token");
            // One through the page and two through JSON: one count.
            assertEquals(200, server.post(PAGE, form("user_code", "BBBBBBBB", "_csrf", csrf))
                    .status);
            assertEquals(400, postJson(server, csrf, "user_code", "BBBBBBBB").status);
            assertEquals(400, postJson(server, csrf, "user_code", "BBBBBBBB").status);
            Reply limited = postJson(server, csrf, "user_code", (String) device.get("user_code"));
            assertEquals(429, limited.status, limited.toString());
            assertEquals("slow_down", json(limited).get("error"));
            assertEquals("90", limited.header("Retry-After"));
        }
    }

    @Test
    @DisplayName("a device names the resource server its tokens are for when it asks for its codes")
    void theAudienceOfADeviceGrant() throws Exception {
        try (SecuredServer server = start(as -> { })) {
            Reply begun = server.call("POST", "/oauth2/device_authorization", form("client_id",
                    "app", "scope", "profile", "resource", "https://tv.example.com/api"), FORM);
            assertEquals(200, begun.status, begun.toString());
            Map device = json(begun);
            OAuth2Testing.signIn(server, "ada", "ada-pw");
            Reply page = server.get(PAGE, "Accept", "text/html");
            Reply question = server.post(PAGE, form("user_code", (String) device.get("user_code"),
                    "_csrf", field(page.body, "_csrf")));
            Reply approved = server.post(PAGE, form("user_code", field(question.body, "user_code"),
                    "decision", "approve", "ticket", field(question.body, "ticket"), "_csrf",
                    field(question.body, "_csrf")));
            assertTrue(approved.body.contains("Device approved"), approved.body);
            Reply reply = poll(server, device.get("device_code"));
            assertEquals(200, reply.status, reply.toString());
            Jwt access = OAuth2Testing.verify((String) json(reply).get("access_token"),
                    server.get("/oauth2/jwks").body);
            // Kept through the user's answer, which replaces what the grant holds.
            assertEquals(java.util.Arrays.asList("https://tv.example.com/api"),
                    access.getAudience());
            assertEquals("app", access.getClaimAsString("client_id"));

            Reply bad = server.call("POST", "/oauth2/device_authorization", form("client_id",
                    "app", "resource", "tv"), FORM);
            refused(bad, "invalid_target");
        }
    }

    private static Reply poll(SecuredServer server, Object deviceCode) throws Exception {
        return server.call("POST", "/oauth2/token", form("grant_type", GRANT, "client_id", "app",
                "device_code", (String) deviceCode), FORM);
    }

    private static void refused(Reply reply, String error) throws Exception {
        assertEquals(400, reply.status, reply.toString());
        assertEquals(error, json(reply).get("error"), reply.toString());
    }

    @Test
    @DisplayName("a device is approved by a signed-in user, and its polling is told every state")
    void approved() throws Exception {
        try (SecuredServer server = start(as -> { })) {
            Map device = begin(server, "openid profile");
            String userCode = (String) device.get("user_code");
            // Eight consonants, shown in two groups; nothing that reads as a digit.
            assertTrue(userCode.matches("[BCDFGHJKLMNPQRSTVWXZ]{4}-[BCDFGHJKLMNPQRSTVWXZ]{4}"),
                    userCode);
            assertEquals(43, ((String) device.get("device_code")).length());
            assertEquals(issuer + PAGE, device.get("verification_uri"));
            assertEquals(issuer + PAGE + "?user_code=" + userCode,
                    device.get("verification_uri_complete"));
            assertEquals(300L, ((Number) device.get("expires_in")).longValue());
            assertEquals(5L, ((Number) device.get("interval")).longValue());
            // Neither code is kept as it was issued.
            assertNull(grants.findToken(OAuth2AuthorizationService.DEVICE_CODE,
                    (String) device.get("device_code")));
            assertNull(grants.findToken(OAuth2AuthorizationService.USER_CODE,
                    userCode.replace("-", "")));

            refused(poll(server, device.get("device_code")), "authorization_pending");
            // Again at once: too soon.
            refused(poll(server, device.get("device_code")), "slow_down");
            clock.now += 5000;
            refused(poll(server, device.get("device_code")), "authorization_pending");

            // The user, on another machine: sent to sign in first, and back.
            String complete = PAGE + "?user_code=" + userCode;
            Reply signedOut = server.get(complete, "Accept", "text/html");
            assertEquals(302, signedOut.status);
            assertEquals("/login", signedOut.header("Location"));
            Reply signedIn = OAuth2Testing.signIn(server, "ada", "ada-pw");
            assertEquals(complete, signedIn.header("Location"));
            Reply page = server.get(complete, "Accept", "text/html");
            assertEquals(200, page.status, page.toString());
            // The code from the link is in the form, and nothing is decided by a GET.
            assertEquals(userCode.replace("-", ""), field(page.body, "user_code"));
            assertEquals("DENY", page.header("X-Frame-Options"));
            assertEquals("frame-ancestors 'none'", page.header("Content-Security-Policy"));
            assertEquals("no-store", page.header("Cache-Control"));
            String csrf = field(page.body, "_csrf");

            // The form is protected: a post without the token is refused.
            assertEquals(403, server.post(PAGE, form("user_code", userCode, "decision",
                    "approve")).status);
            refused(pollLater(server, device), "authorization_pending");

            // Typed in lower case with the dash: the same code. The page asks,
            // naming the client and what it wants.
            Reply question = server.post(PAGE, form("user_code", userCode.toLowerCase(), "_csrf",
                    csrf));
            assertEquals(200, question.status, question.toString());
            assertTrue(question.body.contains("<strong>Acme App</strong>"), question.body);
            assertTrue(question.body.contains("openid profile"), question.body);
            assertTrue(question.body.contains(userCode), question.body);
            refused(pollLater(server, device), "authorization_pending");

            // An answer that does not come from that question decides nothing,
            // whatever else it carries: not without the ticket, not with another's.
            for (String ticket : new String[] {null, OAuth2Parameters.random(32)}) {
                Reply forged = server.post(PAGE, form("user_code", userCode, "decision",
                        "approve", "ticket", ticket, "_csrf", csrf));
                assertTrue(forged.body.contains("is asking to act as"), forged.body);
                question = forged;
            }
            refused(pollLater(server, device), "authorization_pending");

            Reply approved = server.post(PAGE, form("user_code", field(question.body, "user_code"),
                    "decision", "approve", "ticket", field(question.body, "ticket"), "_csrf",
                    field(question.body, "_csrf")));
            assertEquals(200, approved.status, approved.toString());
            assertTrue(approved.body.contains("Device approved"), approved.body);

            clock.now += 5000;
            Reply reply = poll(server, device.get("device_code"));
            assertEquals(200, reply.status, reply.toString());
            assertEquals("no-store", reply.header("Cache-Control"));
            Map tokens = json(reply);
            String jwks = server.get("/oauth2/jwks").body;
            Jwt access = OAuth2Testing.verify((String) tokens.get("access_token"), jwks);
            assertEquals("ada", access.getSubject());
            assertEquals("openid profile", access.getClaimAsString("scope"));
            assertEquals("ada", OAuth2Testing.verify((String) tokens.get("id_token"), jwks)
                    .getSubject());
            assertNotNull(tokens.get("refresh_token"));
            assertEquals(200, server.get("/userinfo", "Authorization", "Bearer "
                    + tokens.get("access_token")).status);

            // The device code worked once; the user code too.
            clock.now += 5000;
            refused(poll(server, device.get("device_code")), "invalid_grant");
            Reply spent = server.post(PAGE, form("user_code", userCode, "_csrf", csrf));
            assertTrue(spent.body.contains("That code is not valid"), spent.body);
            // And the refresh token it came with refreshes.
            assertEquals(200, server.call("POST", "/oauth2/token", form("grant_type",
                    "refresh_token", "client_id", "app", "refresh_token",
                    (String) tokens.get("refresh_token")), FORM).status);
        }
    }

    /// Types the code, and answers the question that follows.
    private static Reply answer(SecuredServer server, Object userCode, String decision,
                                String csrf) throws Exception {
        Reply question = server.post(PAGE, form("user_code", (String) userCode, "_csrf", csrf));
        if (!question.body.contains("name=\"ticket\"")) {
            return question;
        }
        return server.post(PAGE, form("user_code", (String) userCode, "decision", decision,
                "ticket", field(question.body, "ticket"), "_csrf", csrf));
    }

    private Reply pollLater(SecuredServer server, Map device) throws Exception {
        clock.now += 5000;
        return poll(server, device.get("device_code"));
    }

    @Test
    @DisplayName("a refused device is told access_denied, and an unanswered one expired_token")
    void deniedAndExpired() throws Exception {
        try (SecuredServer server = start(as -> { })) {
            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            Map device = begin(server, "openid");
            String csrf = field(server.get(PAGE).body, "_csrf");
            Reply denied = answer(server, device.get("user_code"), "deny", csrf);
            assertTrue(denied.body.contains("Device refused"), denied.body);
            refused(poll(server, device.get("device_code")), "access_denied");
            // And that is the end of it.
            refused(pollLater(server, device), "invalid_grant");

            Map late = begin(server, "openid");
            clock.now += 301000;
            refused(poll(server, late.get("device_code")), "expired_token");
            Reply tooLate = answer(server, late.get("user_code"), "approve", csrf);
            assertTrue(tooLate.body.contains("That code is not valid"), tooLate.body);

            // Another client's device code, a made-up one, a client without the
            // grant, a scope outside the registration.
            Map mine = begin(server, "openid");
            Reply other = server.call("POST", "/oauth2/token", form("grant_type", GRANT,
                    "device_code", (String) mine.get("device_code")), FORM, "Authorization",
                    OAuth2Testing.basic("web", "web-secret"));
            assertEquals(400, other.status);
            assertEquals("unauthorized_client", json(other).get("error"));
            refused(poll(server, OAuth2Parameters.random(32)), "invalid_grant");
            refused(server.call("POST", "/oauth2/token", form("grant_type", GRANT, "client_id",
                    "app"), FORM), "invalid_request");
            Reply noGrant = server.call("POST", "/oauth2/device_authorization", "", FORM,
                    "Authorization", OAuth2Testing.basic("web", "web-secret"));
            assertEquals("unauthorized_client", json(noGrant).get("error"));
            Reply scope = server.call("POST", "/oauth2/device_authorization", form("client_id",
                    "app", "scope", "admin"), FORM);
            assertEquals("invalid_scope", json(scope).get("error"));
            assertEquals(401, server.call("POST", "/oauth2/device_authorization", form("client_id",
                    "nobody"), FORM).status);
        }
    }

    @Test
    @DisplayName("device approval requires a session and a ticket from the question")
    void anAnswerWithNoSession() throws Exception {
        int port = OAuth2Testing.freePort();
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(AuthorizationServerSettings.ISSUER, "http://127.0.0.1:" + port);
        Object[] beans = {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build())};
        // HTTP Basic authenticates a request, but does not establish the
        // browser session required to approve a durable user grant.
        try (SecuredServer server = SecuredServer.start(settings, "test", beans, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .formLogin(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable())
                        .authorizationServer(as -> as
                                .registeredClientRepository(AuthorizationServerTest.clients())
                                .authorizationService(grants)
                                .clientSecretEncoder(OAuth2Testing.PLAIN).clock(clock))
                        .build())) {
            String ada = OAuth2Testing.basic("ada", "ada-pw");
            Map device = begin(server, "openid");
            String userCode = (String) device.get("user_code");
            // A stateless sign-in cannot approve the code or start a question.
            Reply early = server.post(PAGE, form("user_code", userCode, "decision", "approve",
                    "ticket", OAuth2Parameters.random(32)), "Authorization", ada,
                    "Accept", "application/json");
            assertEquals(401, early.status, early.toString());
            refused(poll(server, device.get("device_code")), "authorization_pending");
            clock.now += 5000;

            // A real session with a ticket nobody issued is asked the question
            // again; it still cannot approve an unseen request.
            assertEquals(302, server.post("/login", "username=ada&password=ada-pw").status);
            early = server.post(PAGE, form("user_code", userCode, "decision", "approve",
                    "ticket", OAuth2Parameters.random(32)));
            assertEquals(200, early.status, early.toString());
            assertTrue(early.body.contains("is asking to act as"), early.body);
            refused(poll(server, device.get("device_code")), "authorization_pending");

            // The question it was asked instead is a real one: its ticket answers.
            Reply approved = server.post(PAGE, form("user_code", field(early.body, "user_code"),
                    "decision", "approve", "ticket", field(early.body, "ticket")));
            assertEquals(200, approved.status, approved.toString());
            assertTrue(approved.body.contains("Device approved"), approved.body);
            clock.now += 5000;
            assertEquals(200, poll(server, device.get("device_code")).status);
        }
    }

    @Test
    @DisplayName("user codes are unguessable in the tries a user is allowed")
    void wrongCodesAreBounded() throws Exception {
        try (SecuredServer server = start(as -> { })) {
            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            Map device = begin(server, "openid");
            String csrf = field(server.get(PAGE).body, "_csrf");
            for (int attempt = 1 ; attempt <= 10 ; attempt++) {
                Reply wrong = server.post(PAGE, form("user_code", "BBBB-BBBB", "_csrf", csrf));
                assertEquals(200, wrong.status, "attempt " + attempt);
                assertTrue(wrong.body.contains("That code is not valid"), wrong.body);
            }
            // The eleventh is not looked at, right or wrong.
            Reply limited = server.post(PAGE, form("user_code", (String) device.get("user_code"),
                    "decision", "approve", "_csrf", csrf));
            assertEquals(429, limited.status, limited.toString());
            assertEquals("300", limited.header("Retry-After"));
            refused(poll(server, device.get("device_code")), "authorization_pending");
            // Five minutes later the user may try again.
            clock.now += 300000;
            Map again = begin(server, "openid");
            assertTrue(answer(server, again.get("user_code"), "approve", csrf).body.contains(
                    "Device approved"));
        }
        // Codes do not repeat, and are spread over the alphabet.
        Set<String> seen = new HashSet<String>();
        try (SecuredServer server = start(as -> as.deviceVerificationRateLimiter(
                new InMemoryRateLimiter(2, 60)))) {
            Set<Character> letters = new HashSet<Character>();
            for (int iter = 0 ; iter < 40 ; iter++) {
                String code = (String) begin(server, null).get("user_code");
                assertTrue(seen.add(code), "a user code repeated");
                for (char c : code.replace("-", "").toCharArray()) {
                    letters.add(Character.valueOf(c));
                }
            }
            assertEquals(20, letters.size(), "320 letters did not cover an alphabet of 20");
            // A limiter of the application's is used in place of the built-in one.
            assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
            String csrf = field(server.get(PAGE).body, "_csrf");
            assertEquals(200, server.post(PAGE, form("user_code", "CCCCCCCC", "_csrf", csrf)).status);
            assertEquals(200, server.post(PAGE, form("user_code", "CCCCCCCC", "_csrf", csrf)).status);
            assertEquals(429, server.post(PAGE, form("user_code", "CCCCCCCC", "_csrf", csrf)).status);
        }
        assertFalse(seen.isEmpty());
        assertNotEquals("", issuer);
    }

    @Test
    @DisplayName("two RateLimiter beans and none chosen: the authorization server is refused, not left counting alone")
    void twoLimiterBeansAreRefusedForTheVerificationPage() throws Exception {
        Object users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build());
        final Object[] beans = {users, new InMemoryRateLimiter(600, 60), new InMemoryRateLimiter(5, 60)};
        final String[] names = {"users", "apiLimiter", "loginLimiter"};
        int port = OAuth2Testing.freePort();
        issuer = "http://127.0.0.1:" + port;
        final Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(AuthorizationServerSettings.ISSUER, issuer);
        final boolean[][] primary = {new boolean[3]};
        final Customizer<AuthorizationServerConfigurer>[] more = new Customizer[] {as -> { }};
        SecuredServer.Chain chain = unused -> com.codename1.impl.backend.security.SecuritySupport
                .http(unused.getConfig(), beans, names, primary[0])
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .authorizationServer(as -> {
                    as.registeredClientRepository(AuthorizationServerTest.clients())
                            .authorizationService(grants)
                            .clientSecretEncoder(OAuth2Testing.PLAIN).clock(clock);
                    more[0].customize(as);
                }).build();

        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> SecuredServer.start(settings, "test", new Object[0], APP, chain));
        assertEquals("The device verification page's limit on codes tried has 2 RateLimiter "
                + "beans to choose from and nothing says which: \"apiLimiter\" ("
                + InMemoryRateLimiter.class.getName() + "), \"loginLimiter\" ("
                + InMemoryRateLimiter.class.getName() + "). Mark one of them @Primary, or pass "
                + "the one to use to authorizationServer().deviceVerificationRateLimiter(...).",
                refused.getMessage());

        // Either way out builds the chain.
        primary[0] = new boolean[] {false, false, true};
        SecuredServer.start(settings, "test", new Object[0], APP, chain).close();
        primary[0] = new boolean[3];
        more[0] = as -> as.deviceVerificationRateLimiter(new InMemoryRateLimiter(2, 60));
        SecuredServer.start(settings, "test", new Object[0], APP, chain).close();
    }

    private SecuredServer startConfigured(String attempts, String window, Object bean,
            Customizer<AuthorizationServerConfigurer> more) throws Exception {
        int port = OAuth2Testing.freePort();
        issuer = "http://127.0.0.1:" + port;
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(AuthorizationServerSettings.ISSUER, issuer);
        if (attempts != null) {
            settings.setProperty(AuthorizationServerConfigurer.DEVICE_VERIFICATION_ATTEMPTS,
                    attempts);
        }
        if (window != null) {
            settings.setProperty(AuthorizationServerConfigurer.DEVICE_VERIFICATION_WINDOW, window);
        }
        Object users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build());
        Object[] beans = bean == null ? new Object[] {users} : new Object[] {users, bean};
        return SecuredServer.start(settings, "test", beans, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .authorizationServer(as -> {
                            as.registeredClientRepository(AuthorizationServerTest.clients())
                                    .authorizationService(grants)
                                    .clientSecretEncoder(OAuth2Testing.PLAIN).clock(clock);
                            more.customize(as);
                        }).build());
    }

    @Test
    void aSingleVerificationPermitAllowsTheTicketBackedApproval() throws Exception {
        for (boolean custom : new boolean[] {false, true}) {
            try (SecuredServer server = startConfigured(custom ? null : "1", custom ? null : "300", null, as -> {
                if (custom) {
                    as.deviceVerificationRateLimiter(new InMemoryRateLimiter(1, 300));
                }
            })) {
                Map device = begin(server, "openid");
                assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
                String csrf = field(server.get(PAGE).body, "_csrf");
                String ticket = (String) json(postJson(server, csrf,
                        "user_code", (String) device.get("user_code"))).get("ticket");
                Reply approved = postJson(server, csrf, "user_code", (String) device.get("user_code"),
                        "decision", "approve", "ticket", ticket);
                assertEquals(200, approved.status, approved.toString());
                assertEquals(200, poll(server, device.get("device_code")).status);
                assertEquals(429, postJson(server, csrf, "user_code", "CCCCCCCC").status);
            }
        }
    }

    /// How many wrong user codes are looked at before a 429, and what the 429
    /// says to wait.
    private static String triesAllowed(SecuredServer server) throws Exception {
        assertEquals(302, OAuth2Testing.signIn(server, "ada", "ada-pw").status);
        String csrf = field(server.get(PAGE).body, "_csrf");
        for (int tries = 0 ; tries < 50 ; tries++) {
            Reply reply = server.post(PAGE, form("user_code", "BBBBBBBB", "_csrf", csrf));
            if (reply.status == 429) {
                return tries + " then wait " + reply.header("Retry-After");
            }
        }
        return "50";
    }

    @Test
    @DisplayName("the tries at a user code come from the configuration, and count where the limiter bean counts")
    void theVerificationLimitIsConfigured() throws Exception {
        try (SecuredServer server = startConfigured("3", "90", null, as -> { })) {
            assertEquals("3 then wait 90", triesAllowed(server));
        }
        final List<String> derivedAs = new ArrayList<String>();
        RateLimiter bean = new RateLimiter() {
            private final InMemoryRateLimiter api = new InMemoryRateLimiter(600, 60);

            @Override
            public boolean tryAcquire(String key) {
                return api.tryAcquire(key);
            }

            @Override
            public RateLimiter derive(String name, int permits, long periodSeconds) {
                derivedAs.add(name + " " + permits + "/" + periodSeconds);
                return api.derive(name, permits, periodSeconds);
            }
        };
        try (SecuredServer server = startConfigured("3", "90", bean, as -> { })) {
            assertEquals("[device-verification 3/90]", derivedAs.toString());
            assertTrue(triesAllowed(server).startsWith("3 then wait "), "the bean's 600 applied");
        }
        derivedAs.clear();
        try (SecuredServer server = startConfigured(null, null, bean, as -> { })) {
            assertEquals("[device-verification 10/300]", derivedAs.toString());
        }
        // A limiter given by name counts by its own limit, and the settings
        // beside it would decide nothing.
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> startConfigured("3", null, null, as -> as.deviceVerificationRateLimiter(
                        new InMemoryRateLimiter(2, 60))));
        assertEquals("cn1.security.authorizationserver.device.verificationAttempts and "
                + "cn1.security.authorizationserver.device.verificationWindowSeconds would decide "
                + "nothing: tries are counted by the limiter given to "
                + "deviceVerificationRateLimiter(...), by a limit of its own. Remove the "
                + "settings, or give that limiter the numbers.", refused.getMessage());
        assertEquals("cn1.security.authorizationserver.device.verificationAttempts and "
                + "cn1.security.authorizationserver.device.verificationWindowSeconds must each "
                + "be at least 1", assertThrows(IllegalStateException.class,
                        () -> startConfigured("0", null, null, as -> { })).getMessage());
    }
}
