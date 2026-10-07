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
import static com.codename1.backend.security.OAuth2Testing.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.mfa.InMemoryTotpRepository;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.oauth2.client.AppleClientSecret;
import com.codename1.backend.security.oauth2.client.ClientRegistration;
import com.codename1.backend.security.oauth2.client.CommonOAuth2Provider;
import com.codename1.backend.security.oauth2.client.CookieOAuth2AuthorizationRequestRepository;
import com.codename1.backend.security.oauth2.client.DefaultOAuth2AuthorizationRequestResolver;
import com.codename1.backend.security.oauth2.client.InMemoryClientRegistrationRepository;
import com.codename1.backend.security.oauth2.client.InMemoryFederatedIdentityRepository;
import com.codename1.backend.security.oauth2.client.LinkingOAuth2UserService;
import com.codename1.backend.security.oauth2.client.OAuth2AuthenticationToken;
import com.codename1.backend.security.oauth2.client.OAuth2User;
import com.codename1.backend.security.oauth2.client.OidcUser;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jose.jws.MacAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwsHeader;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.impl.backend.security.SecuritySupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Sign-in through another provider: against this layer's own authorization
/// server, running on a port of its own and reached over real HTTP; and
/// against a stub provider that answers whatever a test tells it to, for the
/// answers no honest provider gives.
class OAuth2LoginTest {
    private static final String REDIRECT = "https://web.example.com/login/oauth2/code/";

    /// Says who the request is from, and how.
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            Object registration = who instanceof OAuth2AuthenticationToken
                    ? ((OAuth2AuthenticationToken) who).getAuthorizedClientRegistrationId() : null;
            return HttpServer.Response.text(200, request.pathFrom(0) + " " + (who == null
                    ? "nobody" : who.getName() + " " + who.getAuthorities() + " via "
                            + registration));
        }
    };

    /// The error codes of the sign-ins the application refused, in order.
    private final List<String> failures = new ArrayList<String>();

    private final AuthenticationFailureHandler recording = new AuthenticationFailureHandler() {
        @Override
        public HttpServer.Response onAuthenticationFailure(HttpServer.Request request,
                                                           AuthenticationException exception) {
            failures.add(exception instanceof OAuth2AuthenticationException
                    ? ((OAuth2AuthenticationException) exception).getError().getErrorCode()
                    : exception.getClass().getName());
            return new SimpleUrlAuthenticationFailureHandler("/login?error")
                    .onAuthenticationFailure(request, exception);
        }
    };

    // ------------------------------------------------- the provider: our own

    private static final String[] PROVIDER_PATHS = {"/oauth2/authorize", "/oauth2/token",
        "/oauth2/jwks", "/oauth2/revoke", "/userinfo", "/.well-known/**", "/idp/**"};

    private String issuer;

    /// One browser's view of the server: its own cookies. One process runs one
    /// backend, so the provider and the application that signs in through it
    /// are two chains of the same server -- as they are for an application that
    /// is its own identity provider -- and what keeps them apart here is what
    /// keeps them apart on two host names: each has its own cookie jar.
    private static final class Side {
        final SecuredServer server;
        final Map<String, String> cookies = new LinkedHashMap<String, String>();

        Side(SecuredServer server) {
            this.server = server;
        }

        Reply call(String method, String target, String body, String type, String... headers)
                throws Exception {
            server.cookies.clear();
            server.cookies.putAll(cookies);
            try {
                return server.call(method, target, body, type, headers);
            } finally {
                cookies.clear();
                cookies.putAll(server.cookies);
            }
        }

        Reply get(String target, String... headers) throws Exception {
            return call("GET", target, null, null, headers);
        }

        Reply post(String target, String form, String... headers) throws Exception {
            return call("POST", target, form, "application/x-www-form-urlencoded", headers);
        }
    }

    /// The provider's chain: this layer's authorization server, with a login
    /// form of its own, one client and two users.
    private SecuredServer.Chain providerChain() {
        return http -> http.securityMatcher(PROVIDER_PATHS)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.loginPage("/idp/login").permitAll())
                .authorizationServer(as -> as.registeredClientRepository(
                        new InMemoryRegisteredClientRepository(
                                RegisteredClient.withId("1").clientId("web")
                                        .clientSecret(OAuth2Testing.PLAIN.encode("web-secret"))
                                        .authorizationGrantType(
                                                AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .redirectUri(REDIRECT + "own")
                                        .scope("openid").scope("profile").scope("email")
                                        .build()))
                        .clientSecretEncoder(OAuth2Testing.PLAIN)
                        .userInfoMapper((username, scopes) -> {
                            Map<String, Object> claims = new LinkedHashMap<String, Object>();
                            if (scopes.contains("email")) {
                                claims.put("email", username + "@example.com");
                                // The provider vouches for ada's address, not eve's.
                                claims.put("email_verified",
                                        Boolean.valueOf("ada".equals(username)));
                            }
                            return claims;
                        })).build();
    }

    private ClientRegistration own() {
        return ClientRegistration.withRegistrationId("own").clientId("web")
                .clientSecret("web-secret").issuerUri(issuer).clientName("Acme ID")
                .redirectUri(REDIRECT + "{registrationId}")
                .scope("openid", "profile", "email").build();
    }

    /// The server: the provider's chain first, then the application's.
    private SecuredServer start(Object[] more, SecuredServer.Chain application) throws Exception {
        int port = OAuth2Testing.freePort();
        issuer = "http://127.0.0.1:" + port;
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(AuthorizationServerSettings.ISSUER, issuer);
        List<Object> beans = new ArrayList<Object>(Arrays.asList(more));
        beans.add(new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build(),
                User.withUsername("eve").password("{noop}eve-pw").roles("USER").build()));
        return SecuredServer.start(settings, "test", beans.toArray(), APP, providerChain(),
                application);
    }

    private SecuredServer start(final Customizer<OAuth2LoginConfigurer> more) throws Exception {
        return start(new Object[0], http -> http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/open").permitAll().anyRequest().authenticated())
                .oauth2Login(oauth2 -> {
                    oauth2.clientRegistrationRepository(
                            new InMemoryClientRegistrationRepository(own()))
                            .failureHandler(recording);
                    more.customize(oauth2);
                }).build());
    }

    /// A server with no provider in it, for the tests whose provider is the stub.
    private SecuredServer app(Object[] beans, final SecuredServer.Chain chain) throws Exception {
        return SecuredServer.start(SecuredServer.settings(), "test", beans, APP, chain);
    }

    /// The path and query of an address on the provider.
    private String onProvider(String location) {
        assertTrue(location.startsWith(issuer + "/"), location);
        return location.substring(issuer.length());
    }

    /// Takes a browser from the application to the provider, signs in there as
    /// `user`, and answers the address the provider sends it back to.
    private String throughProvider(Side app, Side provider, String user)
            throws Exception {
        Reply start = app.get("/oauth2/authorization/own");
        assertEquals(302, start.status, start.toString());
        String url = onProvider(start.header("Location"));
        Reply atProvider = provider.get(url, "Accept", "text/html");
        if ("/idp/login".equals(atProvider.header("Location"))) {
            Reply signedIn = provider.post("/idp/login", form("username", user, "password",
                    user + "-pw"));
            assertEquals(url, signedIn.header("Location"), signedIn.toString());
            atProvider = provider.get(url, "Accept", "text/html");
        }
        assertEquals(302, atProvider.status, atProvider.toString());
        String back = atProvider.header("Location");
        assertTrue(back.startsWith(REDIRECT + "own?code="), back);
        return back.substring("https://web.example.com".length());
    }

    @Test
    @DisplayName("a server whose settings declare Sign in with Apple does not start, and says why")
    void appleInTheSettingsStopsTheStart() {
        Properties settings = SecuredServer.settings();
        settings.setProperty("cn1.security.oauth2.client.registration.apple.client-id",
                "com.example.web");
        settings.setProperty("cn1.security.oauth2.client.registration.apple.client-secret",
                "a-static-secret");
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> SecuredServer.start(settings, "prod", new Object[0], APP,
                        http -> http.authorizeHttpRequests(auth -> auth.anyRequest()
                                .authenticated()).oauth2Login(Customizer.withDefaults()).build()));
        assertTrue(refused.getMessage().startsWith("Sign in with Apple cannot be declared in "
                + "the configuration"), refused.getMessage());
        assertTrue(refused.getMessage().contains("AppleClientSecret.fromFile("),
                refused.getMessage());
    }

    @Test
    @DisplayName("sign-in through this layer's own authorization server, end to end")
    void endToEnd() throws Exception {
        try (SecuredServer server = start(o -> { })) {
            Side provider = new Side(server);
            Side app = new Side(server);
            // One provider and no login form: straight to it, remembering where
            // the user was going.
            Reply first = app.get("/private/report?page=2", "Accept", "text/html");
            assertEquals(302, first.status);
            assertEquals("/oauth2/authorization/own", first.header("Location"));

            Reply start = app.get("/oauth2/authorization/own");
            assertEquals(302, start.status, start.toString());
            assertEquals("no-store", start.header("Cache-Control"));
            String location = start.header("Location");
            assertTrue(location.startsWith(issuer + "/oauth2/authorize?"), location);
            Map<String, String> sent = query(location);
            assertEquals("code", sent.get("response_type"));
            assertEquals("web", sent.get("client_id"));
            assertEquals("openid profile email", sent.get("scope"));
            assertEquals(REDIRECT + "own", sent.get("redirect_uri"));
            assertEquals("S256", sent.get("code_challenge_method"));
            // State, nonce and the PKCE challenge: 256 bits each, and each its own.
            assertEquals(43, sent.get("state").length());
            assertEquals(43, sent.get("nonce").length());
            assertEquals(43, sent.get("code_challenge").length());
            assertNotEquals(sent.get("state"), sent.get("nonce"));
            // Nothing secret goes with the browser.
            assertFalse(location.contains("web-secret") || location.contains("code_verifier"));

            String callback = throughProvider(app, provider, "ada");
            Reply done = app.get(callback);
            assertEquals(302, done.status, done.toString() + failures);
            assertEquals("/private/report?page=2", done.header("Location"));
            assertEquals("/private/report ada [OIDC_USER, SCOPE_openid, SCOPE_profile, "
                    + "SCOPE_email] via own", app.get("/private/report").body);

            // The answer worked once: the same address again finds nothing
            // waiting, and changes nobody's sign-in.
            Reply replay = app.get(callback);
            assertEquals("/login?error", replay.header("Location"));
            assertEquals(Arrays.asList("authorization_request_not_found"), failures);
            assertTrue(app.get("/private/x").body.startsWith("/private/x ada "));

            // Sign-out came with it.
            Reply page = app.get("/open");
            assertEquals(200, page.status);
        }
    }

    @Test
    @DisplayName("only the saved request or the configured default is ever the target")
    void noOpenRedirect() throws Exception {
        try (SecuredServer server = start(o -> o.defaultSuccessUrl("/home"))) {
            Side provider = new Side(server);
            Side app = new Side(server);
            String callback = throughProvider(app, provider, "ada");
            // Whatever else the answer carries chooses nothing.
            Reply done = app.get(callback + "&redirect_uri=https://evil.example/"
                    + "&continue=//evil.example&target=https://evil.example");
            assertEquals(302, done.status, done.toString() + failures);
            assertEquals("/home", done.header("Location"));
        }
    }

    @Test
    @DisplayName("an answer is held to the state this browser was given")
    void state() throws Exception {
        try (SecuredServer server = start(o -> { })) {
            Side provider = new Side(server);
            Side app = new Side(server);
            // No sign-in was started at all.
            Reply cold = app.get("/login/oauth2/code/own?code=abc&state=xyz");
            assertEquals("/login?error", cold.header("Location"));
            // Another state than the one sent; and the attempt used the request up.
            String callback = throughProvider(app, provider, "ada");
            String code = query(callback).get("code");
            Reply forged = app.get("/login/oauth2/code/own?code=" + code + "&state="
                    + OAuth2Parameters.random(32));
            assertEquals("/login?error", forged.header("Location"));
            assertEquals("/login?error", app.get(callback).header("Location"));
            // An answer for another registration than the one that waits.
            callback = throughProvider(app, provider, "ada");
            Reply other = app.get(callback.replace("/code/own?", "/code/other?"));
            assertEquals("/login?error", other.header("Location"));
            // A posted answer, for a provider that is not asked to post.
            callback = throughProvider(app, provider, "ada");
            Reply posted = app.post("/login/oauth2/code/own", form("code",
                    query(callback).get("code"), "state", query(callback).get("state")));
            assertEquals(403, posted.status, "a post there is not excused from CSRF protection "
                    + "when no provider posts");
            assertEquals(Arrays.asList("authorization_request_not_found",
                    "invalid_state_parameter", "authorization_request_not_found",
                    "authorization_request_not_found"), failures);
            assertTrue(app.get("/private/x", "Accept", "text/html").status == 302,
                    "a refused sign-in signed in");
            // The page a refused user lands on says nothing of why.
            Reply page = app.get("/login?error");
            assertEquals(200, page.status);
            assertTrue(page.body.contains("The sign-in did not complete"), page.body);
            assertTrue(page.body.contains("href=\"/oauth2/authorization/own\">Sign in with Acme ID"),
                    page.body);
        }
    }

    @Test
    @DisplayName("a provider's refusal is passed on as its error code and nothing else")
    void providerError() throws Exception {
        try (SecuredServer server = start(o -> { })) {
            Side provider = new Side(server);
            Side app = new Side(server);
            String state = query(app.get("/oauth2/authorization/own").header("Location"))
                    .get("state");
            // This provider names itself in every answer, a refusal included.
            String iss = "&iss=" + OAuth2Parameters.encode(issuer);
            Reply denied = app.get("/login/oauth2/code/own?error=access_denied&error_description="
                    + "%3Cscript%3Ealert(1)%3C/script%3E&state=" + state + iss);
            assertEquals("/login?error", denied.header("Location"));
            state = query(app.get("/oauth2/authorization/own").header("Location")).get("state");
            // Not a code at all: what arrives is not repeated anywhere.
            app.get("/login/oauth2/code/own?error=%3Cscript%3E&state=" + state + iss);
            assertEquals(Arrays.asList("access_denied", "invalid_request"), failures);
            assertFalse(app.get("/login?error").body.contains("script"));

            // A code the provider never issued: its token endpoint refuses, and
            // its reason stays on this side.
            state = query(app.get("/oauth2/authorization/own").header("Location")).get("state");
            app.get("/login/oauth2/code/own?code=" + OAuth2Parameters.random(32) + "&state="
                    + state + iss);
            assertEquals("invalid_grant", failures.get(2));
        }
    }

    // ------------------------------------------------------ account linking

    @Test
    @DisplayName("a provider's user signs in as the local user their verified address names")
    void linking() throws Exception {
        final InMemoryFederatedIdentityRepository identities =
                new InMemoryFederatedIdentityRepository();
        final InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada@example.com").password("{noop}x").roles("ADMIN").build(),
                User.withUsername("eve@example.com").password("{noop}x").roles("USER").build());
        final LinkingOAuth2UserService service = new LinkingOAuth2UserService(identities, users);
        try (SecuredServer server = start(o -> o.userService(service).oidcUserService(service.oidc()))) {
            Side provider = new Side(server);
            Side app = new Side(server);
            Reply done = app.get(throughProvider(app, provider, "ada"));
            assertEquals(302, done.status, done.toString() + failures);
            // Named after the local account, with its authorities first.
            assertEquals("/me ada@example.com [ROLE_ADMIN, OIDC_USER, SCOPE_openid, "
                    + "SCOPE_profile, SCOPE_email] via own", app.get("/me").body);
            assertEquals("ada@example.com", identities.findUsername("own", "ada"));
        }
        // eve's address is one the provider does not vouch for: refused, though
        // a local user has it -- because a local user has it.
        try (SecuredServer server = start(o -> o.userService(service).oidcUserService(service.oidc()))) {
            Side provider = new Side(server);
            Side app = new Side(server);
            Reply done = app.get(throughProvider(app, provider, "eve"));
            assertEquals("/login?error", done.header("Location"));
            assertEquals(Arrays.asList("email_not_verified"), failures);
            assertNull(identities.findUsername("own", "eve"));
        }
    }

    @Test
    void generatedFederatedUsersGetNormalPasswordLoginFailures() throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        LinkingOAuth2UserService service = new LinkingOAuth2UserService(
                new InMemoryFederatedIdentityRepository(), users);
        service.setCreateUsers(true);
        try (SecuredServer server = start(new Object[0], http -> http
                .authenticationProvider(new DaoAuthenticationProvider(users))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .formLogin(Customizer.withDefaults()).httpBasic(Customizer.withDefaults())
                .oauth2Login(o -> o.clientRegistrationRepository(new InMemoryClientRegistrationRepository(own()))
                        .userService(service).oidcUserService(service.oidc())).build())) {
            Side application = new Side(server);
            Side provider = new Side(server);
            assertEquals(302, application.get(throughProvider(application, provider, "ada")).status);
            assertTrue(users.userExists("ada@example.com"));
            for (String name : new String[] {"ada@example.com", "absent@example.com"}) {
                Side passwordClient = new Side(server);
                Reply basic = passwordClient.get("/private", "Authorization", OAuth2Testing.basic(name, "wrong"));
                assertEquals(401, basic.status, basic.toString());
                Reply formReply = passwordClient.post("/login", form("username", name, "password", "wrong"));
                assertEquals(302, formReply.status, formReply.toString());
                assertEquals("/login?error", formReply.header("Location"));
            }
        }
    }

    // ------------------------------------------------------- a second factor

    @Test
    @DisplayName("a second factor is asked for after the provider, as after a password")
    void secondFactor() throws Exception {
        final TotpService totp = new TotpService(new InMemoryTotpRepository(), "Acme");
        final OAuth2Testing.Ticking ticking = new OAuth2Testing.Ticking();
        totp.setClock(ticking);
        totp.beginEnrollment("ada");
        assertTrue(totp.confirmEnrollment("ada", totp.currentCode("ada")));
        // The code that confirmed is spent; the next step's is the one to type.
        ticking.now += 30000;
        try (SecuredServer server = start(new Object[] {totp}, http -> http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .oauth2Login(o -> o.clientRegistrationRepository(
                        new InMemoryClientRegistrationRepository(own())))
                .mfa(Customizer.withDefaults()).build())) {
            Side provider = new Side(server);
            Side app = new Side(server);
            Reply held = app.get(throughProvider(app, provider, "ada"));
            assertEquals("302 /login/mfa", held.status + " " + held.header("Location"));
            // The provider's word alone signed nobody in.
            assertEquals(302, app.get("/private", "Accept", "text/html").status);
            Reply signedIn = app.post("/login/mfa", "code=" + totp.currentCode("ada"));
            assertEquals(302, signedIn.status, signedIn.toString());
            // And on to where they were going when they were asked to sign in.
            assertEquals("/private", signedIn.header("Location"));
            assertTrue(app.get("/private").body.startsWith("/private ada "));
            // Still the provider's user, though the code is what finished it.
            assertTrue(app.get("/private").body.endsWith(" via own"), app.get("/private").body);
        }
    }

    // -------------------------------------------- the provider: a stub

    /// A provider that answers what it is told to, and remembers what it was
    /// asked. The JDK's own small HTTP server: a second backend cannot run in
    /// this process, and this one has nothing of the layer under test in it.
    private static final class Stub implements AutoCloseable {
        volatile String tokenAnswer = "{}";
        volatile int tokenStatus = 200;
        volatile String userAnswer = "{}";
        volatile String emailsAnswer = "[]";
        volatile int emailsStatus = 200;
        final java.util.concurrent.atomic.AtomicInteger emailsRequests =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger userRequests =
                new java.util.concurrent.atomic.AtomicInteger();
        final List<String> tokenRequests = new ArrayList<String>();
        final List<String> authorizations = new ArrayList<String>();
        final Jwk key;
        final com.sun.net.httpserver.HttpServer http;

        Stub() throws Exception {
            key = AuthorizationServerKeys.usable(Jwk.ofPrivateKey(KeyFiles.privateKey(
                    KeyFixtures.RSA_PKCS8_PEM)), "the stub's key");
            http = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(
                    java.net.InetAddress.getByName("127.0.0.1"), 0), 0);
            http.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                for (int n = exchange.getRequestBody().read(buffer) ; n >= 0 ;
                        n = exchange.getRequestBody().read(buffer)) {
                    body.write(buffer, 0, n);
                }
                String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                int status = 404;
                String type = "text/plain";
                String answer = "nothing";
                if ("/token".equals(path)) {
                    synchronized (tokenRequests) {
                        tokenRequests.add(new String(body.toByteArray(), "UTF-8"));
                        authorizations.add(String.valueOf(authorization));
                    }
                    status = tokenStatus;
                    answer = tokenAnswer;
                    type = answer.startsWith("{") ? "application/json"
                            : "application/x-www-form-urlencoded";
                } else if ("/jwks".equals(path)) {
                    status = 200;
                    type = "application/json";
                    answer = JwkSet.of(key).toJson();
                } else if ("/user".equals(path)) {
                    userRequests.incrementAndGet();
                    boolean ours = "Bearer stub-access".equals(authorization);
                    status = ours ? 200 : 401;
                    type = "application/json";
                    answer = ours ? userAnswer : "{}";
                } else if ("/user/emails".equals(path)) {
                    emailsRequests.incrementAndGet();
                    boolean ours = "Bearer stub-access".equals(authorization);
                    status = ours ? emailsStatus : 401;
                    type = "application/json";
                    answer = ours ? emailsAnswer : "{}";
                }
                byte[] bytes = answer.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", type);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            http.start();
        }

        @Override
        public void close() {
            http.stop(0);
        }
    }

    private Stub stub;
    private String stubUrl;

    private Stub stubServer() throws Exception {
        stub = new Stub();
        stubUrl = "http://127.0.0.1:" + stub.http.getAddress().getPort();
        return stub;
    }

    private ClientRegistration.Builder stubRegistration(String id) {
        return ClientRegistration.withRegistrationId(id).clientId("stub-client")
                .clientSecret("stub-secret").authorizationUri(stubUrl + "/authorize")
                .tokenUri(stubUrl + "/token").jwkSetUri(stubUrl + "/jwks")
                .redirectUri(REDIRECT + "{registrationId}");
    }

    private SecuredServer stubApp(final ClientRegistration... registrations) throws Exception {
        return app(new Object[0], http -> http.authorizeHttpRequests(auth -> auth.anyRequest()
                        .authenticated())
                .oauth2Login(o -> o.clientRegistrationRepository(
                        new InMemoryClientRegistrationRepository(registrations))
                        .failureHandler(recording)).build());
    }

    private JwtClaimsSet.Builder idClaims(String nonce) {
        long now = System.currentTimeMillis() / 1000L;
        return JwtClaimsSet.builder().issuer("https://stub.example").subject("user-1")
                .audience("stub-client").issuedAt(now).expiresAt(now + 300).claim("nonce", nonce);
    }

    private String sign(JwtClaimsSet claims) {
        return new DefaultJwtEncoder(AuthorizationServerKeys.of(Arrays.asList(stub.key)))
                .encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private void answerWith(String idToken) {
        Map<String, Object> answer = new LinkedHashMap<String, Object>();
        answer.put("access_token", "stub-access");
        answer.put("token_type", "Bearer");
        answer.put("expires_in", Long.valueOf(300));
        if (idToken != null) {
            answer.put("id_token", idToken);
        }
        stub.tokenAnswer = Json.write(answer);
    }

    /// Starts a sign-in through `registration`; answers what was sent.
    private static Map<String, String> begin(SecuredServer app, String registration)
            throws Exception {
        Reply start = app.get("/oauth2/authorization/" + registration);
        assertEquals(302, start.status, start.toString());
        return query(start.header("Location"));
    }

    @Test
    @DisplayName("an ID token is refused for a wrong audience, issuer, time, signature, algorithm or nonce")
    void idTokenRefusals() throws Exception {
        try (Stub provider = stubServer() ; SecuredServer app = stubApp(
                stubRegistration("oidc").issuerUri("https://stub.example").scope("openid")
                        .build())) {
            // The instrument first: a right token signs in.
            Map<String, String> sent = begin(app, "oidc");
            answerWith(sign(idClaims(sent.get("nonce")).build()));
            Reply good = app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            assertEquals("/", good.header("Location"), good.toString() + failures);
            assertEquals("/who user-1 [OIDC_USER, SCOPE_openid] via oidc", app.get("/who").body);
            // The exchange carried the verifier whose hash went with the browser.
            Map<String, String> exchange = OAuth2Parameters.parse(stub.tokenRequests.get(0));
            assertEquals(sent.get("code_challenge"), OAuth2Parameters.sha256(
                    exchange.get("code_verifier")));
            assertEquals("authorization_code", exchange.get("grant_type"));
            assertEquals(REDIRECT + "oidc", exchange.get("redirect_uri"));
            assertNull(exchange.get("client_secret"), "the secret was sent twice");
            assertEquals(OAuth2Testing.basic("stub-client", "stub-secret"),
                    stub.authorizations.get(0));

            long now = System.currentTimeMillis() / 1000L;
            String publicKey = KeyFixtures.RSA_PUBLIC_PEM;
            List<Object[]> cases = new ArrayList<Object[]>();
            cases.add(new Object[] {"another audience", "invalid_id_token",
                idClaims("N").audience("somebody-else")});
            cases.add(new Object[] {"another issuer", "invalid_id_token",
                idClaims("N").issuer("https://evil.example")});
            cases.add(new Object[] {"expired", "invalid_id_token",
                idClaims("N").issuedAt(now - 7200).expiresAt(now - 3600)});
            cases.add(new Object[] {"two audiences and no azp", "invalid_id_token",
                idClaims("N").audience("stub-client", "other")});
            cases.add(new Object[] {"two audiences and another's azp", "invalid_id_token",
                idClaims("N").audience("stub-client", "other").claim("azp", "other")});
            cases.add(new Object[] {"no subject", "invalid_id_token",
                JwtClaimsSet.builder().issuer("https://stub.example").audience("stub-client")
                        .issuedAt(now).expiresAt(now + 300).claim("nonce", "N")});
            cases.add(new Object[] {"another nonce", "invalid_nonce",
                idClaims("N").claim("nonce", OAuth2Parameters.random(32))});
            cases.add(new Object[] {"no nonce", "invalid_nonce", JwtClaimsSet.builder()
                    .issuer("https://stub.example").subject("user-1").audience("stub-client")
                    .issuedAt(now).expiresAt(now + 300)});
            for (Object[] one : cases) {
                failures.clear();
                sent = begin(app, "oidc");
                // "N" stands for the nonce this very request was given.
                JwtClaimsSet claims = ((JwtClaimsSet.Builder) one[2]).build();
                if ("N".equals(claims.getClaim("nonce"))) {
                    claims = JwtClaimsSet.from(claims).claim("nonce", sent.get("nonce")).build();
                }
                answerWith(sign(claims));
                Reply reply = app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
                assertEquals("/login?error", reply.header("Location"), (String) one[0]);
                assertEquals(Arrays.asList(one[1]), failures, (String) one[0]);
            }

            // Two audiences with this client as the authorized party is fine.
            sent = begin(app, "oidc");
            answerWith(sign(idClaims(sent.get("nonce")).audience("stub-client", "other")
                    .claim("azp", "stub-client").build()));
            assertEquals("/", app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"))
                    .header("Location"));

            // A signature that does not verify: one character of a good token.
            failures.clear();
            sent = begin(app, "oidc");
            String good2 = sign(idClaims(sent.get("nonce")).build());
            char last = good2.charAt(good2.length() - 2);
            answerWith(good2.substring(0, good2.length() - 2) + (last == 'A' ? 'B' : 'A')
                    + good2.charAt(good2.length() - 1));
            app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            // Signed by somebody else's key, under this provider's key id.
            sent = begin(app, "oidc");
            Jwk stranger = Jwk.ofPrivateKey(com.codename1.backend.Crypto.generateRsaKey(2048))
                    .withKeyId(stub.key.getKeyId());
            answerWith(new DefaultJwtEncoder(AuthorizationServerKeys.of(Arrays.asList(stranger)))
                    .encode(JwtEncoderParameters.from(idClaims(sent.get("nonce")).build()))
                    .getTokenValue());
            app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            // Algorithm confusion: HS256, keyed with the provider's PUBLIC key,
            // which anybody has.
            sent = begin(app, "oidc");
            Jwk asSecret = Jwk.ofSecret(publicKey.getBytes("US-ASCII"))
                    .withKeyId(stub.key.getKeyId());
            answerWith(new DefaultJwtEncoder(AuthorizationServerKeys.of(Arrays.asList(asSecret)))
                    .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256)
                            .keyId(stub.key.getKeyId()).build(), idClaims(sent.get("nonce"))
                            .build())).getTokenValue());
            app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            // And no signature at all.
            sent = begin(app, "oidc");
            answerWith(Base64Url.encode("{\"alg\":\"none\"}".getBytes("US-ASCII")) + "."
                    + Base64Url.encode(Json.write(idClaims(sent.get("nonce")).build().getClaims())
                            .getBytes("UTF-8")) + ".");
            app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            // No ID token, from a provider asked for one.
            sent = begin(app, "oidc");
            answerWith(null);
            app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            assertEquals(Arrays.asList("invalid_id_token", "invalid_id_token", "invalid_id_token",
                    "invalid_id_token", "invalid_id_token"), failures);

            // An answer that is not a token response, and a refusal.
            failures.clear();
            sent = begin(app, "oidc");
            stub.tokenAnswer = "<html>busy</html>";
            app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            sent = begin(app, "oidc");
            stub.tokenAnswer = "{\"error\":\"invalid_client\",\"error_description\":\"secret!\"}";
            stub.tokenStatus = 400;
            app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            assertEquals(Arrays.asList("invalid_token_response", "invalid_client"), failures);
        }
    }

    @Test
    void tokenExchangeRequiresBearerBeforeCallingUserInfo() throws Exception {
        try (Stub provider = stubServer(); SecuredServer app = stubApp(
                stubRegistration("hub").userInfoUri(stubUrl + "/user")
                        .userNameAttributeName("id").scope("read:user").build())) {
            stub.userAnswer = "{\"id\":\"user-1\"}";
            for (String response : new String[] {
                    "{\"access_token\":\"stub-access\"}",
                    "{\"access_token\":\"stub-access\",\"token_type\":\"DPoP\"}",
                    "{\"access_token\":\"stub-access\",\"token_type\":true}",
                    "access_token=stub-access&token_type=", "access_token=stub-access&token_type=MAC"}) {
                failures.clear();
                Map<String, String> sent = begin(app, "hub");
                stub.tokenAnswer = response;
                Reply reply = app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state"));
                assertEquals("/login?error", reply.header("Location"), response);
                assertEquals(Arrays.asList("invalid_token_response"), failures, response);
                assertEquals(0, stub.userRequests.get(), response);
            }
            Map<String, String> sent = begin(app, "hub");
            stub.tokenAnswer = "access_token=stub-access&token_type=bEaReR";
            assertEquals("/", app.get("/login/oauth2/code/hub?code=c&state="
                    + sent.get("state")).header("Location"));
            assertEquals(1, stub.userRequests.get());
        }
    }

    @Test
    void omittedResponseScopesUseTheCustomizedAuthorizationRequest() throws Exception {
        try (Stub provider = stubServer()) {
            for (boolean oidc : new boolean[] {false, true}) {
                ClientRegistration registration = stubRegistration("narrow")
                        .issuerUri("https://stub.example").userInfoUri(stubUrl + "/user")
                        .userNameAttributeName("id")
                        .scope(oidc ? "openid" : "read:user", "admin").build();
                InMemoryClientRegistrationRepository registrations =
                        new InMemoryClientRegistrationRepository(registration);
                DefaultOAuth2AuthorizationRequestResolver resolver =
                        new DefaultOAuth2AuthorizationRequestResolver(registrations);
                resolver.setAuthorizationRequestCustomizer(builder -> builder.scopes(
                        Arrays.asList(oidc ? "openid" : "read:user")));
                try (SecuredServer app = app(new Object[0], http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .oauth2Login(o -> o.clientRegistrationRepository(registrations)
                                .authorizationRequestResolver(resolver).failureHandler(recording))
                        .build())) {
                    Map<String, String> sent = begin(app, "narrow");
                    assertEquals(oidc ? "openid" : "read:user", sent.get("scope"));
                    if (oidc) {
                        answerWith(sign(idClaims(sent.get("nonce")).build()));
                        stub.userAnswer = "{\"sub\":\"user-1\"}";
                    } else {
                        stub.tokenAnswer = "access_token=stub-access&token_type=bearer";
                        stub.userAnswer = "{\"id\":\"user-1\"}";
                    }
                    Reply done = app.get("/login/oauth2/code/narrow?code=c&state=" + sent.get("state"));
                    assertEquals("/", done.header("Location"), done.toString() + failures);
                    assertEquals(oidc ? "/who user-1 [OIDC_USER, SCOPE_openid] via narrow"
                            : "/who user-1 [OAUTH2_USER, SCOPE_read:user] via narrow",
                            app.get("/who").body);
                }
            }
        }
    }

    @Test
    @DisplayName("a provider without OpenID Connect: the user is read from its API")
    void gitHubStyle() throws Exception {
        try (Stub provider = stubServer() ; SecuredServer app = stubApp(
                stubRegistration("hub").userInfoUri(stubUrl + "/user").userNameAttributeName("id")
                        .scope("read:user").build())) {
            Map<String, String> sent = begin(app, "hub");
            // No ID token is asked for, so there is no nonce; PKCE and state stay.
            assertNull(sent.get("nonce"));
            assertEquals(43, sent.get("code_challenge").length());
            // GitHub answers with a form, and names the user by a number.
            stub.tokenAnswer = "access_token=stub-access&scope=read%3Auser&token_type=bearer";
            stub.userAnswer = "{\"id\":583231,\"login\":\"octocat\",\"email\":null}";
            Reply done = app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state"));
            assertEquals("/", done.header("Location"), done.toString() + failures);
            assertEquals("/who 583231 [OAUTH2_USER, SCOPE_read:user] via hub",
                    app.get("/who").body);

            // A user info answer without the name attribute is no user.
            sent = begin(app, "hub");
            stub.userAnswer = "{\"login\":\"octocat\"}";
            app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state"));
            assertEquals(Arrays.asList("invalid_user_info_response"), failures);
        }
    }

    @Test
    @DisplayName("Sign in with Apple: a posted answer, a signed cookie, and a client secret that is a JWT")
    void appleStyle() throws Exception {
        try (Stub provider = stubServer()) {
            ClientRegistration apple = stubRegistration("apple").clientSecret(null)
                    .clientSecretSupplier(new AppleClientSecret("TEAM123456", "KEY1234567",
                            KeyFixtures.EC256_PKCS8_PEM))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .responseMode(ClientRegistration.FORM_POST)
                    .issuerUri("https://stub.example").scope("openid", "email").build();
            try (SecuredServer app = stubApp(apple)) {
                Reply start = app.get("/oauth2/authorization/apple");
                Map<String, String> sent = query(start.header("Location"));
                assertEquals("form_post", sent.get("response_mode"));
                String cookie = null;
                for (String set : start.headers("Set-Cookie")) {
                    if (set.startsWith(CookieOAuth2AuthorizationRequestRepository.COOKIE + "=")) {
                        cookie = set;
                    }
                }
                assertNotNull(cookie, start.toString());
                // Sent with a cross-site post, to the callback only, for five minutes.
                assertTrue(cookie.endsWith("; Path=/login/oauth2/code; Max-Age=300; Secure; "
                        + "HttpOnly; SameSite=None"), cookie);
                // The post arrives from Apple's page: no session cookie, no
                // CSRF token. The signed cookie is the whole of what ties it here.
                app.cookies.remove("CN1SESSION");
                String value = app.cookies.get(CookieOAuth2AuthorizationRequestRepository.COOKIE);
                answerWith(sign(idClaims(sent.get("nonce")).build()));

                // A cookie somebody edited, and one they made up.
                app.cookies.put(CookieOAuth2AuthorizationRequestRepository.COOKIE,
                        value.substring(0, value.length() - 2) + "AA");
                Reply edited = app.post("/login/oauth2/code/apple", form("code", "c", "state",
                        sent.get("state")));
                assertEquals("/login?error", edited.header("Location"), edited.toString());
                // The state must be the cookie's own.
                app.cookies.put(CookieOAuth2AuthorizationRequestRepository.COOKIE, value);
                Reply otherState = app.post("/login/oauth2/code/apple", form("code", "c", "state",
                        OAuth2Parameters.random(32)));
                assertEquals("/login?error", otherState.header("Location"));
                // An answer by redirect, for the provider that posts.
                app.cookies.put(CookieOAuth2AuthorizationRequestRepository.COOKIE, value);
                app.get("/login/oauth2/code/apple?code=c&state=" + sent.get("state"));
                assertEquals(Arrays.asList("authorization_request_not_found",
                        "invalid_state_parameter", "authorization_request_not_found"), failures);

                app.cookies.remove("CN1SESSION");
                app.cookies.put(CookieOAuth2AuthorizationRequestRepository.COOKIE, value);
                Reply done = app.post("/login/oauth2/code/apple", form("code", "c", "state",
                        sent.get("state")));
                assertEquals(302, done.status, done.toString() + failures);
                assertEquals("/", done.header("Location"));
                // The cookie is taken back with the answer.
                assertNull(app.cookies.get(CookieOAuth2AuthorizationRequestRepository.COOKIE));
                assertTrue(app.get("/who").body.startsWith("/who user-1 "));

                // The secret that went to the token endpoint: an ES256 JWT in
                // the form, naming the team, the client and Apple.
                Map<String, String> exchange = OAuth2Parameters.parse(
                        stub.tokenRequests.get(stub.tokenRequests.size() - 1));
                assertEquals("stub-client", exchange.get("client_id"));
                final Jwk apples = Jwk.ofPublicKey(KeyFiles.publicKey(
                        KeyFixtures.EC256_PUBLIC_PEM)).withKeyId("KEY1234567");
                Jwt secret = DefaultJwtDecoder.withJwkSource(() -> Arrays.asList(apples))
                        .jwsAlgorithm(
                        com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm.ES256)
                        .build().decode(exchange.get("client_secret"));
                assertEquals("TEAM123456", secret.getIssuer());
                assertEquals("stub-client", secret.getSubject());
                assertEquals(Arrays.asList("https://appleid.apple.com"), secret.getAudience());
                assertEquals("KEY1234567", secret.getHeaders().get("kid"));
                assertEquals(3600L, secret.getExpiresAt().longValue()
                        - secret.getIssuedAt().longValue());
            }
        }
    }

    @Test
    @DisplayName("a chain with a login form offers the providers on it, and declares what it needs")
    void declaration() throws Exception {
        IllegalStateException none = assertThrows(IllegalStateException.class,
                () -> app(new Object[0], http -> http.oauth2Login(Customizer.withDefaults())
                        .build()));
        assertTrue(none.getMessage().contains("ClientRegistrationRepository"), none.getMessage());
        // From the configuration, and beside a form: the form's page links to it.
        Properties settings = SecuredServer.settings();
        settings.setProperty("cn1.security.oauth2.client.registration.github.client-id", "gh-id");
        settings.setProperty("cn1.security.oauth2.client.registration.github.client-secret", "s");
        Object[] beans = {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build())};
        try (SecuredServer app = SecuredServer.start(settings, "test", beans, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .oauth2Login(Customizer.withDefaults())
                        .formLogin(Customizer.withDefaults()).build())) {
            Reply first = app.get("/private", "Accept", "text/html");
            assertEquals("/login", first.header("Location"));
            Reply page = app.get("/login");
            assertTrue(page.body.contains("name=\"password\""), page.body);
            assertTrue(page.body.contains("<a href=\"/oauth2/authorization/github\">Sign in with "
                    + "GitHub</a>"), page.body);
            Reply start = app.get("/oauth2/authorization/github");
            assertTrue(start.header("Location").startsWith(
                    "https://github.com/login/oauth/authorize?response_type=code&client_id=gh-id"),
                    start.header("Location"));
            assertEquals("http://localhost/login/oauth2/code/github",
                    query(start.header("Location")).get("redirect_uri"));
            // A registration nobody declared starts nothing.
            assertEquals(302, app.get("/oauth2/authorization/nobody", "Accept", "text/html").status);
            assertEquals("/login", app.get("/oauth2/authorization/nobody", "Accept", "text/html")
                    .header("Location"));
        }
        assertNotNull(Collections.emptyList());
        assertNotNull(new DefaultOAuth2AuthorizationRequestResolver(
                new InMemoryClientRegistrationRepository(ClientRegistration
                        .withRegistrationId("x").clientId("c").authorizationUri("https://a/b")
                        .tokenUri("https://a/c").build())));
    }

    // ------------------------------------ what a later request sees

    /// What a controller's `@AuthenticationPrincipal OAuth2User` parameter is
    /// bound from -- the generated router asks [SecuritySupport#principal] and
    /// checks the type -- and what the authentication is beside it.
    private static final HttpServer.Handler PRINCIPAL = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            Object principal = SecuritySupport.principal();
            StringBuilder out = new StringBuilder();
            out.append(who == null ? "nobody" : who.getClass().getSimpleName());
            if (who instanceof OAuth2AuthenticationToken) {
                out.append(" via ").append(
                        ((OAuth2AuthenticationToken) who).getAuthorizedClientRegistrationId());
            }
            if (principal instanceof OAuth2User) {
                OAuth2User user = (OAuth2User) principal;
                out.append(" OAuth2User ").append(user.getName()).append(' ')
                        .append(user.getAuthorities()).append(' ')
                        .append(new java.util.TreeMap<String, Object>(user.getAttributes()));
            }
            if (principal instanceof OidcUser) {
                OidcUser user = (OidcUser) principal;
                out.append(" OidcUser sub=").append(user.getSubject()).append(" idToken.aud=")
                        .append(user.getIdToken().getAudience()).append(" alg=")
                        .append(user.getIdToken().getHeaders().get("alg")).append(" value=")
                        .append(user.getIdToken().getTokenValue().length() > 100);
            }
            return HttpServer.Response.text(200, out.toString());
        }
    };

    private SecuredServer principalApp(Properties settings, ClientRegistration... registrations)
            throws Exception {
        return SecuredServer.start(settings, "test", new Object[0], PRINCIPAL, http -> http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2Login(o -> o.clientRegistrationRepository(
                        new InMemoryClientRegistrationRepository(registrations))
                        .failureHandler(recording)).build());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"memory", "db"})
    @DisplayName("a later request sees the provider's user, in memory and through the database session store")
    void theSessionKeepsTheKindOfSignIn(String store,
            @org.junit.jupiter.api.io.TempDir java.io.File dir) throws Exception {
        Properties settings = SecuredServer.settings();
        settings.setProperty("cn1.session.store", store);
        if ("db".equals(store)) {
            settings.setProperty(Config.DATASOURCE_URL, new java.io.File(dir, "s.db").getPath());
        }
        try (Stub provider = stubServer()) {
            try (SecuredServer app = principalApp(settings,
                    stubRegistration("oidc").issuerUri("https://stub.example").scope("openid")
                            .build(),
                    stubRegistration("hub").userInfoUri(stubUrl + "/user")
                            .userNameAttributeName("id").scope("read:user").build())) {
                // OpenID Connect: the claims, a nested one among them, and the
                // ID token itself.
                Map<String, String> sent = begin(app, "oidc");
                Map<String, Object> address = new LinkedHashMap<String, Object>();
                address.put("country", "NO");
                answerWith(sign(idClaims(sent.get("nonce")).claim("email", "ada@example.com")
                        .claim("email_verified", Boolean.TRUE).claim("address", address)
                        .claim("groups", Arrays.asList("staff", "ops")).build()));
                Reply done = app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
                assertEquals("/", done.header("Location"), done.toString() + failures);
                String seen = app.get("/me").body;
                assertTrue(seen.startsWith("OAuth2AuthenticationToken via oidc OAuth2User user-1 "
                        + "[OIDC_USER, SCOPE_openid] {address={country=NO}, aud=stub-client, "
                        + "email=ada@example.com, email_verified=true, "), seen);
                assertTrue(seen.contains("groups=[staff, ops]"), seen);
                assertTrue(seen.endsWith(" OidcUser sub=user-1 idToken.aud=[stub-client] alg=RS256 "
                        + "value=true"), seen);
                // And again: the same on every later request, not on the first alone.
                assertEquals(seen, app.get("/me").body);

                if ("db".equals(store)) {
                    // The instrument: the session really went through the database,
                    // as JSON, and what it holds says which kind it is.
                    com.codename1.backend.DataSource pool = com.codename1.backend.DataSource
                            .open(new java.io.File(dir, "s.db").getPath(), 1, 5000, 10000);
                    try {
                        String rows = String.valueOf(pool.query(
                                "SELECT * FROM cn1_http_session", null));
                        assertTrue(rows.contains("\"kind\":\"oauth2\"")
                                && rows.contains("\"registrationId\":\"oidc\""), rows);
                    } finally {
                        pool.close();
                    }
                }
            }
            // Without OpenID Connect: GitHub's user, whose id is a number.
            settings.setProperty(Config.DATASOURCE_URL, new java.io.File(dir, "s2.db").getPath());
            if (!"db".equals(store)) {
                settings.remove(Config.DATASOURCE_URL);
            }
            try (SecuredServer app = principalApp(settings,
                    stubRegistration("hub").userInfoUri(stubUrl + "/user")
                            .userNameAttributeName("id").scope("read:user").build())) {
                Map<String, String> sent = begin(app, "hub");
                stub.tokenAnswer = "access_token=stub-access&scope=read%3Auser&token_type=bearer";
                stub.userAnswer = "{\"id\":583231,\"login\":\"octocat\",\"site_admin\":false,"
                        + "\"plan\":{\"name\":\"pro\",\"seats\":3}}";
                Reply done = app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state"));
                assertEquals("/", done.header("Location"), done.toString() + failures);
                assertEquals("OAuth2AuthenticationToken via hub OAuth2User 583231 [OAUTH2_USER, "
                        + "SCOPE_read:user] {id=583231, login=octocat, plan={name=pro, seats=3}, "
                        + "site_admin=false}", app.get("/me").body);
            }
        }
    }

    @Test
    @DisplayName("a session of a kind the chain has no codec for is still its user, by name")
    void aKindNobodyReads() {
        HttpSessionSecurityContextRepository with = new HttpSessionSecurityContextRepository();
        with.addAuthenticationCodec(new OAuth2AuthenticationCodec());
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(
                new com.codename1.backend.security.oauth2.client.DefaultOAuth2User("ada",
                        Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")),
                        Collections.<String, Object>singletonMap("login", "ada")),
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")), "hub");
        Map<String, Object> stored = with.toMap(token);
        assertEquals("oauth2", stored.get("kind"));
        assertTrue(with.fromMap(stored) instanceof OAuth2AuthenticationToken);
        // Another chain of the same server, without oauth2Login().
        Authentication plain = new HttpSessionSecurityContextRepository().fromMap(stored);
        assertTrue(plain instanceof UsernamePasswordAuthenticationToken, String.valueOf(plain));
        assertEquals("ada [ROLE_USER]", plain.getName() + " " + plain.getAuthorities());
        // What a codec cannot read back -- a registration id that is not text,
        // attributes that are not a map -- is the name and authorities alone.
        for (Object[] broken : new Object[][] {{"registrationId", Long.valueOf(7)},
            {"registrationId", ""}, {"attributes", "x"}, {"attributes", null}}) {
            Map<String, Object> copy = new LinkedHashMap<String, Object>(stored);
            Map<String, Object> data = new LinkedHashMap<String, Object>(
                    (Map<String, Object>) stored.get("data"));
            data.put((String) broken[0], broken[1]);
            copy.put("data", data);
            assertTrue(with.fromMap(copy) instanceof UsernamePasswordAuthenticationToken,
                    String.valueOf(broken[1]));
        }
        Map<String, Object> notAMap = new LinkedHashMap<String, Object>(stored);
        notAMap.put("data", "x");
        assertTrue(with.fromMap(notAMap) instanceof UsernamePasswordAuthenticationToken);
    }

    // ------------------------------------ a provider that lists addresses

    @Test
    @DisplayName("GitHub's user is tied to the primary verified address of its list, and to no other")
    void linkingThroughTheEmailsEndpoint() throws Exception {
        final InMemoryFederatedIdentityRepository identities =
                new InMemoryFederatedIdentityRepository();
        final InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada@example.com").password("{noop}x").roles("ADMIN").build(),
                User.withUsername("eve@example.com").password("{noop}x").roles("USER").build());
        final LinkingOAuth2UserService service = new LinkingOAuth2UserService(identities, users);
        // The preset is what a real registration starts from.
        ClientRegistration preset = CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId("c").build();
        assertEquals("https://api.github.com/user/emails",
                preset.getProviderDetails().getUserEmailsUri());
        assertTrue(preset.getScopes().contains("user:email"), preset.getScopes().toString());

        try (Stub provider = stubServer() ; SecuredServer app = app(new Object[0], http -> http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2Login(o -> o.clientRegistrationRepository(
                        new InMemoryClientRegistrationRepository(stubRegistration("hub")
                                .userInfoUri(stubUrl + "/user")
                                .userEmailsUri(stubUrl + "/user/emails")
                                .userNameAttributeName("id").scope("read:user", "user:email")
                                .build()))
                        .userService(service).failureHandler(recording)).build())) {
            stub.tokenAnswer = "access_token=stub-access&scope=read%3Auser&token_type=bearer";
            // What a list must NOT tie anybody with. In every one the user
            // info itself shows eve's address, as anybody may set theirs to.
            stub.userAnswer = "{\"id\":583231,\"login\":\"octocat\","
                    + "\"email\":\"eve@example.com\",\"email_verified\":true}";
            String[] refusedLists = {
                "[]",
                // Verified and not primary; primary and not verified.
                "[{\"email\":\"ada@example.com\",\"primary\":false,\"verified\":true},"
                        + "{\"email\":\"eve@example.com\",\"primary\":true,\"verified\":false}]",
                // Flags that are not the truth value.
                "[{\"email\":\"ada@example.com\",\"primary\":\"true\",\"verified\":\"true\"}]",
                "[{\"email\":\"ada@example.com\",\"primary\":1,\"verified\":1}]",
                // Not a list of entries, and not JSON.
                "{\"email\":\"ada@example.com\",\"primary\":true,\"verified\":true}",
                "[\"ada@example.com\", 7, null, [true]]",
                "[{\"email\":7,\"primary\":true,\"verified\":true}]",
                "<html>rate limited</html>"};
            for (String list : refusedLists) {
                failures.clear();
                stub.emailsAnswer = list;
                Map<String, String> sent = begin(app, "hub");
                Reply reply = app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state"));
                assertEquals("/login?error", reply.header("Location"), list);
                assertEquals(Arrays.asList("email_not_verified"), failures, list);
            }
            // The scope was not granted: GitHub answers 404 there.
            failures.clear();
            stub.emailsStatus = 404;
            stub.emailsAnswer = "[{\"email\":\"ada@example.com\",\"primary\":true,"
                    + "\"verified\":true}]";
            Map<String, String> sent = begin(app, "hub");
            app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state"));
            assertEquals(Arrays.asList("email_not_verified"), failures);
            assertNull(identities.findUsername("hub", "583231"));

            // The primary verified address is ada's: signed in as her, whatever
            // the user info shows.
            failures.clear();
            stub.emailsStatus = 200;
            stub.emailsAnswer = "[{\"email\":\"old@example.com\",\"primary\":false,"
                    + "\"verified\":true},{\"email\":\"ada@example.com\",\"primary\":true,"
                    + "\"verified\":true,\"visibility\":\"private\"}]";
            sent = begin(app, "hub");
            Reply done = app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state"));
            assertEquals("/", done.header("Location"), done.toString() + failures);
            assertEquals("/who ada@example.com [ROLE_ADMIN, OAUTH2_USER, SCOPE_read:user] via hub",
                    app.get("/who").body);
            assertEquals("ada@example.com", identities.findUsername("hub", "583231"));

            // Tied now: the list is not asked again, and what it would say
            // changes nothing.
            int asked = stub.emailsRequests.get();
            stub.emailsAnswer = "[]";
            sent = begin(app, "hub");
            done = app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state"));
            assertEquals("/", done.header("Location"), done.toString() + failures);
            assertEquals(asked, stub.emailsRequests.get());
        }
    }

    // ------------------------------- the issuer of an answer, and at_hash

    @Test
    @DisplayName("an answer is held to the issuer the browser was sent to (RFC 9207)")
    void authorizationResponseIssuer() throws Exception {
        try (Stub provider = stubServer() ; SecuredServer app = stubApp(
                stubRegistration("says").issuerUri("https://stub.example").scope("openid")
                        .authorizationResponseIssParameterSupported(true).build(),
                stubRegistration("silent").issuerUri("https://stub.example").scope("openid")
                        .build(),
                stubRegistration("hub").userInfoUri(stubUrl + "/user")
                        .userNameAttributeName("id").scope("read:user").build())) {
            String right = "&iss=" + OAuth2Parameters.encode("https://stub.example");
            String wrong = "&iss=" + OAuth2Parameters.encode("https://evil.example");

            // A provider that says it names itself: the right name signs in.
            Map<String, String> sent = begin(app, "says");
            answerWith(sign(idClaims(sent.get("nonce")).build()));
            Reply good = app.get("/login/oauth2/code/says?code=c&state=" + sent.get("state")
                    + right);
            assertEquals("/", good.header("Location"), good.toString() + failures);
            int exchanges = stub.tokenRequests.size();

            // No name, another name, and a name that only starts like it.
            for (String iss : new String[] {"", wrong,
                "&iss=" + OAuth2Parameters.encode("https://stub.example/"),
                "&iss=" + OAuth2Parameters.encode("https://stub.example.evil.example")}) {
                failures.clear();
                sent = begin(app, "says");
                answerWith(sign(idClaims(sent.get("nonce")).build()));
                Reply reply = app.get("/login/oauth2/code/says?code=c&state="
                        + sent.get("state") + iss);
                assertEquals("/login?error", reply.header("Location"), iss);
                assertEquals(Arrays.asList("invalid_issuer"), failures, iss);
            }
            // Refused before the code went anywhere.
            assertEquals(exchanges, stub.tokenRequests.size());

            // An error from another issuer is not this provider's refusal.
            failures.clear();
            sent = begin(app, "says");
            app.get("/login/oauth2/code/says?error=access_denied&state=" + sent.get("state")
                    + wrong);
            sent = begin(app, "says");
            app.get("/login/oauth2/code/says?error=access_denied&state=" + sent.get("state")
                    + right);
            assertEquals(Arrays.asList("invalid_issuer", "access_denied"), failures);

            // A provider that does not say so may leave the name out, and is
            // held to it when it sends one.
            failures.clear();
            sent = begin(app, "silent");
            answerWith(sign(idClaims(sent.get("nonce")).build()));
            assertEquals("/", app.get("/login/oauth2/code/silent?code=c&state="
                    + sent.get("state")).header("Location"), failures.toString());
            sent = begin(app, "silent");
            answerWith(sign(idClaims(sent.get("nonce")).build()));
            assertEquals("/", app.get("/login/oauth2/code/silent?code=c&state="
                    + sent.get("state") + right).header("Location"), failures.toString());
            sent = begin(app, "silent");
            answerWith(sign(idClaims(sent.get("nonce")).build()));
            assertEquals("/login?error", app.get("/login/oauth2/code/silent?code=c&state="
                    + sent.get("state") + wrong).header("Location"));
            assertEquals(Arrays.asList("invalid_issuer"), failures);

            // A registration that names no issuer has nothing to hold one to.
            failures.clear();
            sent = begin(app, "hub");
            stub.tokenAnswer = "access_token=stub-access&scope=read%3Auser&token_type=bearer";
            stub.userAnswer = "{\"id\":583231}";
            assertEquals("/", app.get("/login/oauth2/code/hub?code=c&state=" + sent.get("state")
                    + wrong).header("Location"), failures.toString());
        }
    }

    @Test
    @DisplayName("our own provider says it names itself, and an answer without the name is refused")
    void ownProviderNamesItself() throws Exception {
        try (SecuredServer server = start(o -> { })) {
            Side provider = new Side(server);
            Side app = new Side(server);
            String callback = throughProvider(app, provider, "ada");
            assertEquals(issuer, query(callback).get("iss"), callback);
            // The same answer with the name taken off: read from the metadata,
            // this provider always sends one.
            int at = callback.indexOf("&iss=");
            int end = callback.indexOf('&', at + 1);
            String stripped = callback.substring(0, at) + (end < 0 ? "" : callback.substring(end));
            assertEquals("/login?error", app.get(stripped).header("Location"));
            assertEquals(Arrays.asList("invalid_issuer"), failures);

            failures.clear();
            callback = throughProvider(app, provider, "ada");
            Reply done = app.get(callback);
            assertEquals(302, done.status, done.toString() + failures);
            assertTrue(app.get("/private/x").body.startsWith("/private/x ada "), failures.toString());
        }
    }

    @Test
    @DisplayName("an issuer per tenant matches the template with a tenant id in it, and nothing else")
    void issuerTemplate() {
        ClientRegistration.ProviderDetails p = ClientRegistration.withRegistrationId("ms")
                .clientId("c").authorizationUri("https://a/b").tokenUri("https://a/c")
                .issuerTemplate("https://login.example/{tenantid}/v2.0").build()
                .getProviderDetails();
        assertTrue(p.isIssuer("https://login.example/9188040d-6c67-4c5b-b112-36a304b66dad/v2.0"));
        assertFalse(p.isIssuer("https://login.example//v2.0"));
        assertFalse(p.isIssuer("https://login.example/a/b/v2.0"));
        assertFalse(p.isIssuer("https://login.example/evil.example%2f/v2.0"));
        assertFalse(p.isIssuer("https://evil.example/tenant/v2.0"));
        assertFalse(p.isIssuer("https://login.example/tenant/v2.0/more"));
        assertFalse(p.isIssuer(null));
        ClientRegistration.ProviderDetails none = ClientRegistration.withRegistrationId("x")
                .clientId("c").authorizationUri("https://a/b").tokenUri("https://a/c").build()
                .getProviderDetails();
        assertFalse(none.isIssuer("https://a"));
        assertFalse(none.isAuthorizationResponseIssParameterSupported());
    }

    private static String leftHalf(String accessToken) throws Exception {
        byte[] digest = com.codename1.backend.Crypto.sha256(accessToken.getBytes("US-ASCII"));
        return Base64Url.encode(Arrays.copyOf(digest, digest.length / 2));
    }

    @Test
    @DisplayName("an ID token's at_hash is held to the access token it came with")
    void accessTokenHash() throws Exception {
        try (Stub provider = stubServer() ; SecuredServer app = stubApp(
                stubRegistration("oidc").issuerUri("https://stub.example").scope("openid")
                        .build())) {
            // The hash of the token that came with it.
            Map<String, String> sent = begin(app, "oidc");
            answerWith(sign(idClaims(sent.get("nonce")).claim("at_hash",
                    leftHalf("stub-access")).build()));
            Reply good = app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
            assertEquals("/", good.header("Location"), good.toString() + failures);

            List<Object> wrong = new ArrayList<Object>();
            // The hash of another token, the whole hash rather than half, the
            // right one in another encoding's padding, and not text at all.
            wrong.add(leftHalf("somebody-elses-access"));
            wrong.add(Base64Url.encode(com.codename1.backend.Crypto.sha256(
                    "stub-access".getBytes("US-ASCII"))));
            wrong.add(leftHalf("stub-access") + "==");
            wrong.add("");
            wrong.add(Long.valueOf(7));
            for (Object hash : wrong) {
                failures.clear();
                sent = begin(app, "oidc");
                answerWith(sign(idClaims(sent.get("nonce")).claim("at_hash", hash).build()));
                Reply reply = app.get("/login/oauth2/code/oidc?code=c&state=" + sent.get("state"));
                assertEquals("/login?error", reply.header("Location"), String.valueOf(hash));
                assertEquals(Arrays.asList("invalid_id_token"), failures, String.valueOf(hash));
            }
        }
    }
}
