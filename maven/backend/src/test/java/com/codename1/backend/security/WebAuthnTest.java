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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.mfa.InMemoryTotpRepository;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.rememberme.InMemoryTokenRepositoryImpl;
import com.codename1.backend.security.rememberme.PersistentTokenBasedRememberMeServices;
import com.codename1.backend.security.webauthn.CredentialRecord;
import com.codename1.backend.security.webauthn.InMemoryPublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.InMemoryUserCredentialRepository;
import com.codename1.backend.security.webauthn.Passkeys;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntity;
import com.codename1.backend.security.webauthn.WebAuthnAuthentication;
import com.codename1.backend.security.webauthn.WebAuthnException;
import com.codename1.impl.backend.security.SecuritySupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// `http.webAuthn(...)` through a whole chain: a user registers a passkey
/// while signed in, signs out, and signs in with it; and what the endpoints
/// do with a challenge that is old, used, or another session's.
class WebAuthnTest {
    private static final String RP = "example.org";
    private static final String ORIGIN = "https://example.org";
    private static final String JSON = "application/json";

    /// Says who the request is from, as a controller's parameters would see.
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            Object principal = SecuritySupport.principal();
            StringBuilder out = new StringBuilder(request.pathFrom(0)).append(' ');
            out.append(who == null ? "nobody" : who.getName() + " " + who.getAuthorities() + " "
                    + who.getClass().getSimpleName());
            if (who instanceof WebAuthnAuthentication) {
                WebAuthnAuthentication passkey = (WebAuthnAuthentication) who;
                out.append(" uv=").append(passkey.isUserVerified()).append(" credential=")
                        .append(Base64Url.encode(passkey.getCredentialId()));
            }
            if (principal instanceof PublicKeyCredentialUserEntity) {
                PublicKeyCredentialUserEntity user = (PublicKeyCredentialUserEntity) principal;
                out.append(" principal=").append(user.getName()).append('/')
                        .append(user.getId().length);
            }
            return HttpServer.Response.text(200, out.toString());
        }
    };

    private static final class Ticking implements Clock {
        long now = 1790000000000L;

        @Override
        public long currentTimeMillis() {
            return now;
        }
    }

    private final Ticking clock = new Ticking();
    private final InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
            User.withUsername("ada").password("{noop}ada-pw").roles("USER", "ADMIN").build(),
            User.withUsername("eve").password("{noop}eve-pw").roles("USER").build());
    private final InMemoryUserCredentialRepository credentials =
            new InMemoryUserCredentialRepository();
    private final InMemoryPublicKeyCredentialUserEntityRepository handles =
            new InMemoryPublicKeyCredentialUserEntityRepository();
    /// Why each refused sign-in was refused, in order.
    private final List<String> refusals = new ArrayList<String>();

    private final AuthenticationFailureHandler recording = new AuthenticationFailureHandler() {
        @Override
        public HttpServer.Response onAuthenticationFailure(HttpServer.Request request,
                                                           AuthenticationException exception) {
            refusals.add(exception instanceof WebAuthnException
                    ? ((WebAuthnException) exception).getReason() : exception.toString());
            Map<String, Object> answer = new LinkedHashMap<String, Object>();
            answer.put("authenticated", Boolean.FALSE);
            return HttpServer.Response.json(401, Json.write(answer));
        }
    };

    private Customizer<WebAuthnConfigurer> passkeys(final Customizer<WebAuthnConfigurer> more) {
        return w -> {
            w.rpId(RP).rpName("Example").allowedOrigins(ORIGIN)
                    .userCredentialRepository(credentials).userEntityRepository(handles)
                    .failureHandler(recording).clock(clock);
            more.customize(w);
        };
    }

    private SecuredServer start(final Customizer<WebAuthnConfigurer> more) throws Exception {
        return start(SecuredServer.settings(), more);
    }

    private SecuredServer start(Properties settings, final Customizer<WebAuthnConfigurer> more)
            throws Exception {
        return SecuredServer.start(settings, "dev", new Object[] {users}, APP,
                http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/open").permitAll()
                                .anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .webAuthn(passkeys(more)).build());
    }

    private static Map json(Reply reply) throws Exception {
        return Json.parseObject(reply.body);
    }

    private static Reply postJson(SecuredServer server, String target, Object body)
            throws Exception {
        return server.call("POST", target, body == null ? "" : Json.write(body), JSON);
    }

    /// Registers `passkey` for whoever the server's cookies are signed in as.
    private static Map register(SecuredServer server, Passkeys passkey, String label)
            throws Exception {
        Reply options = postJson(server, "/webauthn/register/options", null);
        assertEquals(200, options.status, options.toString());
        Reply done = postJson(server, "/webauthn/register" + (label == null ? ""
                : "?label=" + label), passkey.create(json(options)));
        return json(done);
    }

    private static Reply signIn(SecuredServer server, Passkeys passkey) throws Exception {
        Reply options = postJson(server, "/webauthn/authenticate/options", null);
        assertEquals(200, options.status, options.toString());
        return postJson(server, "/login/webauthn", passkey.get(json(options)));
    }

    @Test
    @DisplayName("register while signed in, sign out, sign in with the passkey, reach what is protected")
    void endToEnd() throws Exception {
        Passkeys phone = new Passkeys(false, RP, ORIGIN);
        Passkeys key = new Passkeys(true, RP, ORIGIN).format("packed");
        try (SecuredServer server = start(w -> { })) {
            // Nobody is signed in: the registration endpoints are the chain's
            // to refuse, and it asks for a sign-in.
            Reply anonymous = postJson(server, "/webauthn/register/options", null);
            assertEquals(302, anonymous.status, anonymous.toString());
            assertEquals("/login", anonymous.header("Location"));
            assertEquals(302, postJson(server, "/webauthn/register", new LinkedHashMap()).status);
            assertEquals(302, server.call("DELETE", "/webauthn/register/abc", null, null).status);
            assertTrue(server.reached().isEmpty(), server.reached().toString());

            assertEquals(302, server.post("/login", "username=ada&password=ada-pw").status);
            assertEquals("/me ada [ROLE_USER, ROLE_ADMIN] UsernamePasswordAuthenticationToken",
                    server.get("/me").body);

            // The options are the WebAuthn JSON, and are not to be kept by anything.
            Reply options = postJson(server, "/webauthn/register/options", null);
            assertEquals(200, options.status, options.toString());
            assertEquals("no-store", options.header("Cache-Control"));
            Map asked = json(options);
            assertEquals("{id=example.org, name=Example}", String.valueOf(asked.get("rp")));
            assertEquals("ada", ((Map) asked.get("user")).get("name"));
            assertEquals("[]", String.valueOf(asked.get("excludeCredentials")));
            // The credential as a client's toJSON() gives it, with a label.
            Reply made = postJson(server, "/webauthn/register?label=phone", phone.create(asked));
            assertEquals(200, made.status, made.toString());
            assertEquals("{success=true, credentialId=" + Base64Url.encode(phone.credentialId())
                    + "}", String.valueOf(json(made)));
            // And as Spring Security wraps it.
            options = postJson(server, "/webauthn/register/options", null);
            assertEquals("[{type=public-key, id=" + Base64Url.encode(phone.credentialId())
                    + ", transports=[internal, hybrid]}]",
                    String.valueOf(json(options).get("excludeCredentials")));
            Map<String, Object> wrapped = new LinkedHashMap<String, Object>();
            Map<String, Object> publicKey = new LinkedHashMap<String, Object>();
            publicKey.put("credential", key.create(json(options)));
            publicKey.put("label", "security key");
            wrapped.put("publicKey", publicKey);
            assertEquals(Boolean.TRUE, json(postJson(server, "/webauthn/register", wrapped))
                    .get("success"));
            List<CredentialRecord> stored = credentials.findByUserId(
                    handles.findByUsername("ada").getId());
            assertEquals("phone, security key", stored.get(0).getLabel() + ", "
                    + stored.get(1).getLabel());
            // None of that went to the application.
            assertEquals(Arrays.asList("GET /me"), server.reached());

            // Sign out; what is protected is protected again.
            assertEquals(302, server.post("/logout", "").status);
            Reply out = server.get("/private?x=1", "Accept", "text/html");
            assertEquals("302 /login", out.status + " " + out.header("Location"));

            // Sign in with the passkey, naming nobody.
            Reply requested = postJson(server, "/webauthn/authenticate/options", null);
            assertEquals(200, requested.status, requested.toString());
            assertEquals("no-store", requested.header("Cache-Control"));
            Map request = json(requested);
            assertEquals("example.org [] preferred", request.get("rpId") + " "
                    + request.get("allowCredentials") + " " + request.get("userVerification"));
            String before = server.cookies.toString();
            Reply signedIn = postJson(server, "/login/webauthn", phone.get(request));
            assertEquals(200, signedIn.status, signedIn.toString() + refusals);
            // To where the user was going when they were asked to sign in.
            assertEquals("{authenticated=true, redirectUrl=/private?x=1}",
                    String.valueOf(json(signedIn)));
            assertEquals("no-store", signedIn.header("Cache-Control"));
            // A new session id, as after any sign-in.
            assertFalse(before.equals(server.cookies.toString()));
            String seen = "/private ada [ROLE_USER, ROLE_ADMIN] WebAuthnAuthentication uv=true "
                    + "credential=" + Base64Url.encode(phone.credentialId()) + " principal=ada/32";
            assertEquals(seen, server.get("/private?x=1").body);
            assertEquals(seen.replace("/private", "/again"), server.get("/again").body);

            // The other one too -- RS256 -- and the answer then names the default.
            assertEquals(302, server.post("/logout", "").status);
            Reply second = signIn(server, key);
            assertEquals("{authenticated=true, redirectUrl=/}", String.valueOf(json(second)));
            assertTrue(server.get("/me").body.contains("credential="
                    + Base64Url.encode(key.credentialId())), server.get("/me").body);
            assertTrue(refusals.isEmpty(), refusals.toString());

            // A passkey is its owner's to remove. To eve, ada's does not exist.
            Map<String, String> adas = new LinkedHashMap<String, String>(server.cookies);
            server.cookies.clear();
            assertEquals(302, server.post("/login", "username=eve&password=eve-pw").status);
            String phoneId = Base64Url.encode(phone.credentialId());
            assertEquals(404, server.call("DELETE", "/webauthn/register/" + phoneId, null,
                    null).status);
            assertNotNull(credentials.findByCredentialId(phone.credentialId()));
            server.cookies.clear();
            server.cookies.putAll(adas);
            assertEquals(404, server.call("DELETE", "/webauthn/register/AAAA", null, null).status);
            assertEquals(404, server.call("DELETE", "/webauthn/register/not*base64", null,
                    null).status);
            Reply removed = server.call("DELETE", "/webauthn/register/" + phoneId, null, null);
            assertEquals(204, removed.status, removed.toString());
            assertNull(credentials.findByCredentialId(phone.credentialId()));
            assertEquals(404, server.call("DELETE", "/webauthn/register/" + phoneId, null,
                    null).status);
            // And it signs nobody in any more.
            server.cookies.clear();
            Reply gone = signIn(server, phone);
            assertEquals("401 {authenticated=false}", gone.status + " " + json(gone));
            assertEquals(Arrays.asList("unknown_credential"), refusals);
            assertEquals(302, server.get("/private", "Accept", "text/html").status);
        }
    }

    @Test
    @DisplayName("a challenge is answered once, in time, in the session it was given to")
    void challenges() throws Exception {
        Passkeys phone = new Passkeys(false, RP, ORIGIN).counter(0, false);
        try (SecuredServer server = start(w -> { })) {
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals(Boolean.TRUE, register(server, phone, null).get("success"));
            server.post("/logout", "");

            // The instrument: an answer in time signs in.
            Reply options = postJson(server, "/webauthn/authenticate/options", null);
            Map<String, Object> answer = phone.get(json(options));
            clock.now += 299000;
            assertEquals(200, postJson(server, "/login/webauthn", answer).status,
                    refusals.toString());
            server.post("/logout", "");

            // The same answer again: its challenge was used.
            options = postJson(server, "/webauthn/authenticate/options", null);
            answer = phone.get(json(options));
            assertEquals(200, postJson(server, "/login/webauthn", answer).status);
            server.post("/logout", "");
            assertEquals(401, postJson(server, "/login/webauthn", answer).status);
            // Even a refused answer uses its challenge up: the right one after
            // a wrong one finds nothing waiting.
            options = postJson(server, "/webauthn/authenticate/options", null);
            answer = phone.get(json(options));
            assertEquals(401, postJson(server, "/login/webauthn",
                    new Passkeys(false, RP, ORIGIN).get(json(options))).status);
            assertEquals(401, postJson(server, "/login/webauthn", answer).status);
            // Too late by a second.
            options = postJson(server, "/webauthn/authenticate/options", null);
            answer = phone.get(json(options));
            clock.now += 301000;
            assertEquals(401, postJson(server, "/login/webauthn", answer).status);
            // Asked for in one session, answered in another: with no challenge
            // of its own, and with one of its own.
            options = postJson(server, "/webauthn/authenticate/options", null);
            answer = phone.get(json(options));
            Map<String, String> first = new LinkedHashMap<String, String>(server.cookies);
            server.cookies.clear();
            assertEquals(401, postJson(server, "/login/webauthn", answer).status);
            postJson(server, "/webauthn/authenticate/options", null);
            assertEquals(401, postJson(server, "/login/webauthn", answer).status);
            // Never asked for at all, and a body that is no credential.
            server.cookies.clear();
            assertEquals(401, postJson(server, "/login/webauthn", answer).status);
            postJson(server, "/webauthn/authenticate/options", null);
            assertEquals(401, server.call("POST", "/login/webauthn", "nonsense", JSON).status);
            postJson(server, "/webauthn/authenticate/options", null);
            assertEquals(401, server.call("POST", "/login/webauthn", "[1]", JSON).status);
            assertEquals(Arrays.asList("no_challenge", "unknown_credential", "no_challenge",
                    "challenge_expired", "no_challenge", "challenge_mismatch", "no_challenge",
                    "malformed", "malformed"), refusals);
            // Nobody was signed in by any of it, and the first session still can be.
            assertEquals(302, server.get("/private", "Accept", "text/html").status);
            server.cookies.clear();
            server.cookies.putAll(first);
            assertEquals(200, postJson(server, "/login/webauthn", answer).status);

            // Registration: the same rules, told to the user who is signed in.
            Passkeys second = new Passkeys(false, RP, ORIGIN);
            options = postJson(server, "/webauthn/register/options", null);
            Map<String, Object> made = second.create(json(options));
            clock.now += 301000;
            Reply late = postJson(server, "/webauthn/register", made);
            assertEquals("400 {success=false, error=challenge_expired}", late.status + " "
                    + json(late));
            assertEquals("{success=false, error=no_challenge}",
                    String.valueOf(json(postJson(server, "/webauthn/register", made))));
            options = postJson(server, "/webauthn/register/options", null);
            made = second.create(json(options));
            assertEquals(Boolean.TRUE, json(postJson(server, "/webauthn/register", made))
                    .get("success"));
            assertEquals("{success=false, error=no_challenge}",
                    String.valueOf(json(postJson(server, "/webauthn/register", made))));
            // The same credential again, with a challenge of its own.
            options = postJson(server, "/webauthn/register/options", null);
            assertEquals("{success=false, error=credential_exists}", String.valueOf(json(
                    postJson(server, "/webauthn/register", second.create(json(options))))));
            // An origin that is not allowed, and a body that is no credential.
            options = postJson(server, "/webauthn/register/options", null);
            assertEquals("{success=false, error=origin_mismatch}", String.valueOf(json(postJson(
                    server, "/webauthn/register", new Passkeys(false, RP,
                            "https://evil.example").create(json(options))))));
            postJson(server, "/webauthn/register/options", null);
            assertEquals("{success=false, error=malformed}", String.valueOf(json(
                    server.call("POST", "/webauthn/register", "nonsense", JSON))));
            postJson(server, "/webauthn/register/options", null);
            Map<String, Object> hollow = new LinkedHashMap<String, Object>();
            hollow.put("publicKey", new LinkedHashMap<String, Object>());
            assertEquals("{success=false, error=malformed}", String.valueOf(json(
                    postJson(server, "/webauthn/register", hollow))));
            // What a refusal says is the reason's name, and nothing that was sent.
            assertFalse(late.body.contains("evil") || late.body.contains("too long ago"));
        }
    }

    @Test
    @DisplayName("options started by one user are not another's to answer")
    void registrationBelongsToWhoStartedIt() throws Exception {
        // A chain whose sign-in keeps the session: the same session, another user.
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users}, APP, http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .sessionManagement(session -> session.sessionFixation(fixation -> fixation.none()))
                        .formLogin(Customizer.withDefaults())
                        .webAuthn(passkeys(w -> { })).build())) {
            server.post("/login", "username=ada&password=ada-pw");
            Reply options = postJson(server, "/webauthn/register/options", null);
            assertEquals("ada", ((Map) json(options).get("user")).get("name"));
            // Eve signs in over ada, in the session that holds ada's options.
            server.post("/login", "username=eve&password=eve-pw");
            assertTrue(server.get("/me").body.startsWith("/me eve "), server.get("/me").body);
            Passkeys eves = new Passkeys(false, RP, ORIGIN);
            assertEquals("{success=false, error=no_challenge}", String.valueOf(json(postJson(
                    server, "/webauthn/register", eves.create(json(options))))));
            assertNull(credentials.findByCredentialId(eves.credentialId()));
        }
    }

    @Test
    @DisplayName("a passkey that verified the user needs no second factor; one that did not is asked for it")
    void secondFactor() throws Exception {
        final TotpService totp = new TotpService(new InMemoryTotpRepository(), "Acme");
        totp.setClock(clock);
        totp.beginEnrollment("ada");
        assertTrue(totp.confirmEnrollment("ada", totp.currentCode("ada")));
        clock.now += 30000;
        Passkeys verifying = new Passkeys(false, RP, ORIGIN);
        Passkeys touchOnly = new Passkeys(false, RP, ORIGIN).userVerified(false);
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users, totp}, APP, http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .mfa(mfa -> mfa.clock(clock))
                        .webAuthn(passkeys(w -> { })).build())) {
            // The instrument: ada's password alone does not sign her in.
            Reply held = server.post("/login", "username=ada&password=ada-pw");
            assertEquals("/login/mfa", held.header("Location"));
            assertEquals(302, server.post("/login/mfa", "code=" + totp.currentCode("ada")).status);
            clock.now += 30000;
            assertEquals(Boolean.TRUE, register(server, verifying, null).get("success"));
            assertEquals(Boolean.TRUE, register(server, touchOnly, null).get("success"));
            server.post("/logout", "");

            // Verified by the authenticator: signed in, no code asked.
            Reply direct = signIn(server, verifying);
            assertEquals("200 {authenticated=true, redirectUrl=/}", direct.status + " "
                    + json(direct));
            assertTrue(server.get("/me").body.contains("WebAuthnAuthentication uv=true"),
                    server.get("/me").body);
            server.post("/logout", "");

            // Touched and no more: one factor. The answer is the second
            // factor's own, and nobody is signed in until the code.
            Reply pending = signIn(server, touchOnly);
            assertEquals("302 /login/mfa", pending.status + " " + pending.header("Location"));
            assertEquals(302, server.get("/me", "Accept", "text/html").status);
            Reply finished = server.post("/login/mfa", "code=" + totp.currentCode("ada"));
            assertEquals(302, finished.status, finished.toString());
            // Still the passkey's sign-in, and still one that did not verify.
            assertTrue(server.get("/me").body.contains("WebAuthnAuthentication uv=false "
                    + "credential=" + Base64Url.encode(touchOnly.credentialId())),
                    server.get("/me").body);
            assertTrue(refusals.isEmpty(), refusals.toString());
        }
        // A chain that requires verification refuses the second kind outright.
        try (SecuredServer server = start(w -> w.userVerification("required"))) {
            assertEquals(401, signIn(server, touchOnly).status);
            assertEquals(200, signIn(server, verifying).status);
            assertEquals(Arrays.asList("user_not_verified"), refusals);
        }
    }

    @Test
    @DisplayName("a passkey signs the user in when the attempts at a code are used up, and clears them")
    void aPasskeyClearsTheCountOfWrongCodes() throws Exception {
        final TotpService totp = new TotpService(new InMemoryTotpRepository(), "Acme");
        totp.setClock(clock);
        totp.beginEnrollment("ada");
        assertTrue(totp.confirmEnrollment("ada", totp.currentCode("ada")));
        clock.now += 30000;
        Passkeys verifying = new Passkeys(false, RP, ORIGIN);
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users, totp}, APP, http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .mfa(mfa -> mfa.clock(clock))
                        .webAuthn(passkeys(w -> { })).build())) {
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals(302, server.post("/login/mfa", "code=" + totp.currentCode("ada")).status);
            clock.now += 30000;
            assertEquals(Boolean.TRUE, register(server, verifying, null).get("success"));
            server.post("/logout", "");

            // Somebody with the password uses up what this network has.
            server.cookies.clear();
            server.post("/login", "username=ada&password=ada-pw");
            String right = totp.currentCode("ada");
            String wrong = "000000".equals(right) ? "000001" : "000000";
            for (int attempt = 0; attempt < 3; attempt++) {
                assertEquals("/login/mfa?error", server.post("/login/mfa", "code=" + wrong)
                        .header("Location"));
            }
            assertEquals(429, server.post("/login/mfa", "code=" + right).status);

            // The passkey is not counted with the codes: it signs in.
            server.cookies.clear();
            Reply direct = signIn(server, verifying);
            assertEquals("200 {authenticated=true, redirectUrl=/}", direct.status + " "
                    + json(direct));
            server.post("/logout", "");

            // And what was counted is gone: the code is taken again, which it
            // was not a moment ago.
            server.cookies.clear();
            server.post("/login", "username=ada&password=ada-pw");
            Reply code = server.post("/login/mfa", "code=" + right);
            assertEquals("302 /", code.status + " " + code.header("Location"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"memory", "db"})
    @DisplayName("a later request sees the passkey's sign-in, in memory and through the database session store")
    void theSessionKeepsThePasskey(String store, @TempDir File dir) throws Exception {
        Properties settings = SecuredServer.settings();
        settings.setProperty("cn1.session.store", store);
        if ("db".equals(store)) {
            settings.setProperty(Config.DATASOURCE_URL, new File(dir, "s.db").getPath());
        }
        Passkeys phone = new Passkeys(false, RP, ORIGIN);
        try (SecuredServer server = start(settings, w -> { })) {
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals(Boolean.TRUE, register(server, phone, null).get("success"));
            server.post("/logout", "");
            assertEquals(200, signIn(server, phone).status, refusals.toString());
            String seen = "/me ada [ROLE_USER, ROLE_ADMIN] WebAuthnAuthentication uv=true "
                    + "credential=" + Base64Url.encode(phone.credentialId()) + " principal=ada/32";
            assertEquals(seen, server.get("/me").body);
            assertEquals(seen, server.get("/me").body);
            if ("db".equals(store)) {
                com.codename1.backend.DataSource pool = com.codename1.backend.DataSource.open(
                        new File(dir, "s.db").getPath(), 1, 5000, 10000);
                try {
                    String rows = String.valueOf(pool.query("SELECT * FROM cn1_http_session",
                            null));
                    assertTrue(rows.contains("\"kind\":\"webauthn\"") && rows.contains(
                            "\"credentialId\":\"" + Base64Url.encode(phone.credentialId()) + "\""),
                            rows);
                } finally {
                    pool.close();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bearerCredentialsCannotManagePasskeys(boolean useApiKey) throws Exception {
        com.codename1.backend.security.apikey.GeneratedApiKey key =
                new com.codename1.backend.security.apikey.ApiKeyGenerator().generate("ada");
        com.codename1.backend.security.apikey.InMemoryApiKeyRepository keys =
                new com.codename1.backend.security.apikey.InMemoryApiKeyRepository(key.getApiKey());
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users}, APP, http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable()).formLogin(Customizer.withDefaults())
                        .apiKey(api -> api.repository(keys))
                        .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.decoder(token ->
                                new com.codename1.backend.security.oauth2.jwt.Jwt(token,
                                        java.util.Collections.<String, Object>singletonMap("alg", "RS256"),
                                        java.util.Collections.<String, Object>singletonMap("sub", "ada")))))
                        .webAuthn(passkeys(w -> { })).build())) {
            server.post("/login", "username=ada&password=ada-pw");
            Passkeys phone = new Passkeys(false, RP, ORIGIN);
            assertEquals(Boolean.TRUE, register(server, phone, null).get("success"));
            String bearer = "Bearer " + (useApiKey ? key.getPlaintext() : "verified-token");
            // Even a valid session for the same user must not bless a bearer override.
            for (boolean withSession : new boolean[] {true, false}) {
                if (!withSession) {
                    server.cookies.clear();
                }
                assertEquals(200, server.get("/me", "Authorization", bearer).status);
                assertEquals(403, server.call("POST", "/webauthn/register/options", "", JSON,
                        "Authorization", bearer).status);
                assertEquals(403, server.call("POST", "/webauthn/register", "{}", JSON,
                        "Authorization", bearer).status);
                assertEquals(403, server.call("DELETE", "/webauthn/register/"
                        + Base64Url.encode(phone.credentialId()), null, null,
                        "Authorization", bearer).status);
                assertNotNull(credentials.findByCredentialId(phone.credentialId()));
            }
        }
    }

    @Test
    @DisplayName("somebody a remember-me cookie brought back signs in again before touching passkeys")
    void rememberedIsNotEnough() throws Exception {
        final PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices("key", users,
                        new InMemoryTokenRepositoryImpl());
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users}, APP, http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .rememberMe(remember -> remember.rememberMeServices(services))
                        .webAuthn(passkeys(w -> { })).build())) {
            server.post("/login", "username=ada&password=ada-pw&remember-me=on");
            Passkeys phone = new Passkeys(false, RP, ORIGIN);
            assertEquals(Boolean.TRUE, register(server, phone, null).get("success"));
            // The session is gone; the cookie brings ada back.
            String remembered = server.cookies.get("remember-me");
            server.cookies.clear();
            server.cookies.put("remember-me", remembered);
            assertTrue(server.get("/me").body.endsWith("RememberMeAuthenticationToken"),
                    server.get("/me").body);
            // She passes the rules, and is still not who may add or remove a passkey.
            Reply options = postJson(server, "/webauthn/register/options", null);
            assertEquals("302 /login", options.status + " " + options.header("Location"));
            Reply removed = server.call("DELETE", "/webauthn/register/"
                    + Base64Url.encode(phone.credentialId()), null, null);
            assertEquals(302, removed.status);
            assertNotNull(credentials.findByCredentialId(phone.credentialId()));
            assertEquals(1, credentials.findByUserId(handles.findByUsername("ada").getId()).size());
        }
    }

    @Test
    @DisplayName("a user is named first only where the chain says so")
    void usernameFirst() throws Exception {
        Passkeys phone = new Passkeys(false, RP, ORIGIN);
        String listed = "[{type=public-key, id=" + Base64Url.encode(phone.credentialId())
                + ", transports=[internal, hybrid]}]";
        Map<String, Object> named = new LinkedHashMap<String, Object>();
        named.put("username", "ada");
        try (SecuredServer server = start(w -> { })) {
            server.post("/login", "username=ada&password=ada-pw");
            register(server, phone, null);
            server.post("/logout", "");
            // Off: a name tells whoever asks nothing.
            assertEquals("[]", String.valueOf(json(postJson(server,
                    "/webauthn/authenticate/options", named)).get("allowCredentials")));
            assertEquals("[]", String.valueOf(json(server.post(
                    "/webauthn/authenticate/options?username=ada", "")).get("allowCredentials")));
        }
        try (SecuredServer server = start(w -> w.usernameFirst(true))) {
            Reply options = postJson(server, "/webauthn/authenticate/options", named);
            assertEquals(listed, String.valueOf(json(options).get("allowCredentials")));
            assertEquals(200, postJson(server, "/login/webauthn", phone.get(json(options))).status,
                    refusals.toString());
            server.post("/logout", "");
            assertEquals(listed, String.valueOf(json(server.post(
                    "/webauthn/authenticate/options", "username=ADA")).get("allowCredentials")));
            // A user without one, and nobody of that name, look the same.
            named.put("username", "eve");
            String eve = String.valueOf(json(postJson(server, "/webauthn/authenticate/options",
                    named)).get("allowCredentials"));
            named.put("username", "nobody");
            assertEquals(eve, String.valueOf(json(postJson(server,
                    "/webauthn/authenticate/options", named)).get("allowCredentials")));
            assertEquals("[]", eve);
            // Started for ada, and answered with a passkey that is not one of
            // those listed for her.
            named.put("username", "ada");
            Reply forAda = postJson(server, "/webauthn/authenticate/options", named);
            assertEquals(401, postJson(server, "/login/webauthn",
                    new Passkeys(false, RP, ORIGIN).get(json(forAda))).status);
            assertEquals(Arrays.asList("credential_not_allowed"), refusals);
        }
    }

    @Test
    @DisplayName("a user who may not sign in is not signed in by their passkey")
    void accountStatus() throws Exception {
        Passkeys phone = new Passkeys(false, RP, ORIGIN);
        try (SecuredServer server = start(w -> { })) {
            server.post("/login", "username=eve&password=eve-pw");
            register(server, phone, null);
            server.post("/logout", "");
            assertEquals(200, signIn(server, phone).status);
            server.post("/logout", "");
            users.updateUser(User.withUsername("eve").password("{noop}eve-pw").roles("USER")
                    .accountLocked(true).build());
            assertEquals(401, signIn(server, phone).status);
            users.updateUser(User.withUsername("eve").password("{noop}eve-pw").roles("USER")
                    .disabled(true).build());
            assertEquals(401, signIn(server, phone).status);
            users.deleteUser("eve");
            assertEquals(401, signIn(server, phone).status);
            assertEquals(Arrays.asList("account_unavailable", "account_unavailable",
                    "account_unavailable"), refusals);
            assertEquals(302, server.get("/me", "Accept", "text/html").status);
        }
    }

    @Test
    @DisplayName("an attestation this server does not verify is refused by default, and the origin of an app is allowed when listed")
    void attestationAndAppOrigins() throws Exception {
        String app = "android:apk-key-hash:" + Base64Url.encode(new byte[32]);
        try (SecuredServer server = start(w -> { })) {
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals("{success=false, error=unsupported_attestation}", String.valueOf(
                    register(server, new Passkeys(false, RP, ORIGIN).format("android-key"),
                            null)));
            assertEquals("{success=false, error=origin_mismatch}", String.valueOf(
                    register(server, new Passkeys(false, RP, app), null)));
        }
        try (SecuredServer server = start(w -> w.allowUnverifiedAttestation(true)
                .allowedOrigins(ORIGIN, app))) {
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals(Boolean.TRUE, register(server,
                    new Passkeys(false, RP, ORIGIN).format("android-key"), null).get("success"));
            Passkeys phone = new Passkeys(false, RP, app);
            assertEquals(Boolean.TRUE, register(server, phone, null).get("success"));
            server.post("/logout", "");
            assertEquals(200, signIn(server, phone).status, refusals.toString());
        }
    }

    @Test
    @DisplayName("with CSRF on the ceremonies need the token, unless the chain leaves them out of it")
    void csrf() throws Exception {
        Passkeys phone = new Passkeys(false, RP, ORIGIN);
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users}, APP, http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .webAuthn(passkeys(w -> { })).build())) {
            // As for any POST of a chain that keeps a session.
            assertEquals(403, postJson(server, "/webauthn/authenticate/options", null).status);
            assertEquals(403, postJson(server, "/login/webauthn", new LinkedHashMap()).status);
            Reply page = server.get("/login");
            String token = OAuth2Testing.field(page.body, "_csrf");
            Reply options = server.call("POST", "/webauthn/authenticate/options", "", JSON,
                    "X-CSRF-TOKEN", token);
            assertEquals(200, options.status, options.toString());
        }
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users}, APP, http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.ignoringRequestMatchers("/webauthn/**",
                                "/login/webauthn"))
                        .formLogin(Customizer.withDefaults())
                        .webAuthn(passkeys(w -> { })).build())) {
            Reply page = server.get("/login");
            server.post("/login", OAuth2Testing.form("username", "ada", "password", "ada-pw",
                    "_csrf", OAuth2Testing.field(page.body, "_csrf")));
            // An app's whole exchange, with no token anywhere.
            assertEquals(Boolean.TRUE, register(server, phone, null).get("success"));
            server.cookies.clear();
            assertEquals(200, signIn(server, phone).status, refusals.toString());
            assertTrue(server.get("/me").body.contains("WebAuthnAuthentication"));
        }
    }

    @Test
    @DisplayName("what the chain was not told is said when it is built")
    void configuration() {
        Object[] none = new Object[0];
        String noId = assertThrows(IllegalStateException.class, () -> SecuritySupport.http(
                Config.of(new Properties(), "dev"), new Object[] {users})
                .webAuthn(w -> w.allowedOrigins(ORIGIN)).build()).getMessage();
        assertTrue(noId.contains("rpId(\"example.com\")"), noId);
        String noOrigin = assertThrows(IllegalStateException.class, () -> SecuritySupport.http(
                Config.of(new Properties(), "dev"), new Object[] {users})
                .webAuthn(w -> w.rpId(RP)).build()).getMessage();
        assertTrue(noOrigin.contains("allowedOrigins(\"https://example.org\")"), noOrigin);
        String noUsers = assertThrows(IllegalStateException.class, () -> SecuritySupport.http(
                Config.of(new Properties(), "dev"), none)
                .webAuthn(w -> w.rpId(RP).allowedOrigins(ORIGIN)).build()).getMessage();
        assertTrue(noUsers.contains("UserDetailsService"), noUsers);
        assertThrows(IllegalArgumentException.class, () -> SecuritySupport.http(
                Config.of(new Properties(), "dev"), new Object[] {users})
                .webAuthn(w -> w.rpId(RP).allowedOrigins(ORIGIN + "/")).build());
        assertThrows(IllegalArgumentException.class, () -> SecuritySupport.http(
                Config.of(new Properties(), "dev"), new Object[] {users})
                .webAuthn(w -> w.rpId("https://example.org").allowedOrigins(ORIGIN)).build());
        // The repositories are found among the application's beans.
        SecuritySupport.http(Config.of(new Properties(), "dev"), new Object[] {users, credentials,
            handles}).webAuthn(w -> w.rpId(RP).allowedOrigins(ORIGIN)).build();
    }
}
