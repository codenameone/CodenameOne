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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import com.codename1.backend.security.oauth2.jwt.JwtException;
import com.codename1.backend.security.oauth2.jwt.JwtIssuerValidator;
import com.codename1.backend.security.oauth2.jwt.JwtValidators;
import com.codename1.backend.security.oauth2.server.resource.DefaultBearerTokenResolver;
import com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationConverter;
import com.codename1.backend.security.oauth2.server.resource.JwtGrantedAuthoritiesConverter;
import com.codename1.backend.security.oauth2.server.resource.JwtIssuerAuthenticationManagerResolver;
import com.codename1.impl.backend.security.SecuritySupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// A resource server at work in a running server: tokens accepted and refused
/// through a real chain, with the answer RFC 6750 asks for each time.
class OAuth2ResourceServerTest {
    private static final String SPEC = "https://tools.ietf.org/html/rfc6750#section-3.1";

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            String path = request.pathFrom(0);
            if (path.endsWith("/me")) {
                Authentication who = SecuritySupport.authentication();
                return HttpServer.Response.text(200, who == null ? "nobody"
                        : who.getName() + " " + who.getAuthorities() + " "
                        + who.getPrincipal().getClass().getSimpleName());
            }
            return HttpServer.Response.text(200, "ok " + path);
        }
    };

    private static Jwk rsa() throws Exception {
        return Jwk.ofPrivateKey(Base64.decode(KeyFixtures.RSA_PKCS8_DER));
    }

    private static Jwk p256() throws Exception {
        return Jwk.ofPrivateKey(Base64.decode(KeyFixtures.EC256_PKCS8_DER));
    }

    private static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    private static JwtClaimsSet.Builder claims(String scope) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder().issuer("https://id.example.com")
                .subject("ada").audience("orders-api").issuedAt(now()).expiresAt(now() + 300);
        if (scope != null) {
            builder.claim("scope", scope);
        }
        return builder;
    }

    private static String token(Jwk key, JwtClaimsSet claims) {
        return new DefaultJwtEncoder(JwkSet.of(key)).encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static JwtDecoder rsaDecoder() {
        return DefaultJwtDecoder.withPublicKey(Base64.decode(KeyFixtures.RSA_PUBLIC_DER)).build();
    }

    /// The chain most of these tests run under.
    private static SecuredServer.Chain api(final Customizer<OAuth2ResourceServerConfigurer> oauth2) {
        return new SecuredServer.Chain() {
            @Override
            public SecurityFilterChain build(HttpSecurity http) {
                http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/open/**").permitAll()
                        .requestMatchers("/api/orders/**").hasAuthority("SCOPE_orders:read")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2);
                return http.build();
            }
        };
    }

    private static SecuredServer start(Customizer<OAuth2ResourceServerConfigurer> oauth2)
            throws Exception {
        return SecuredServer.start(APP, api(oauth2));
    }

    @Test
    @DisplayName("A good token is who the request is from; none, or a bad one, is 401 with why")
    void acceptedAndRefused() throws Exception {
        try (SecuredServer server = start(o -> o.jwt(jwt -> jwt.decoder(rsaDecoder())))) {
            String good = token(rsa(), claims("orders:read orders:write").build());
            Reply me = server.get("/api/me", "Authorization", bearer(good));
            assertEquals(200, me.status, me.toString());
            assertEquals("ada [SCOPE_orders:read, SCOPE_orders:write] Jwt", me.body);
            assertEquals("ok /api/orders/7", server.get("/api/orders/7", "Authorization",
                    bearer(good)).body);
            // The scheme in any case, as HTTP has it.
            assertEquals(200, server.get("/api/me", "Authorization", "bEaReR " + good).status);
            // Nothing is kept: no session was started for it.
            assertNull(me.header("Set-Cookie"), me.toString());
            assertTrue(server.cookies.isEmpty());

            // No token: asked for one, and told nothing else.
            Reply none = server.get("/api/me");
            assertEquals(401, none.status);
            assertEquals("Bearer", none.header("WWW-Authenticate"));
            // Where the rule asks for a scope the challenge names it -- and
            // still no error: none was made (RFC 6750 3).
            Reply scoped = server.get("/api/orders/7");
            assertEquals(401, scoped.status);
            assertEquals("Bearer scope=\"orders:read\"", scoped.header("WWW-Authenticate"));
            // A role is not a scope, and is not offered as one.
            assertEquals("Bearer", server.get("/api/admin/x").header("WWW-Authenticate"));
            // An open route stays open.
            assertEquals(200, server.get("/api/open/docs").status);

            int reached = server.reached().size();
            String expired = token(rsa(), claims("orders:read").expiresAt(now() - 3600).build());
            Reply late = server.get("/api/me", "Authorization", bearer(expired));
            assertEquals(401, late.status);
            assertTrue(late.header("WWW-Authenticate").startsWith(
                    "Bearer error=\"invalid_token\", error_description=\"Jwt expired at "),
                    late.toString());
            assertTrue(late.header("WWW-Authenticate").endsWith(", error_uri=\"" + SPEC + "\""),
                    late.toString());
            // A bad token is refused even where no token was needed.
            assertEquals(401, server.get("/api/open/docs", "Authorization", bearer(expired)).status);

            String early = token(rsa(), claims(null).notBefore(now() + 3600).build());
            assertTrue(server.get("/api/me", "Authorization", bearer(early))
                    .header("WWW-Authenticate").contains("error_description=\"Jwt used before "));

            int second = good.lastIndexOf('.');
            char last = good.charAt(second - 2);
            String tampered = good.substring(0, second - 2) + (last == 'A' ? 'B' : 'A')
                    + good.substring(second - 1);
            assertEquals("Bearer error=\"invalid_token\", error_description=\"The token's signature "
                    + "does not verify\", error_uri=\"" + SPEC + "\"", server.get("/api/me",
                            "Authorization", bearer(tampered)).header("WWW-Authenticate"));

            // Signed by another key.
            String foreign = token(Jwk.ofPrivateKey(Crypto.generateRsaKey(2048))
                    .withKeyId(rsa().getKeyId()), claims("orders:read").build());
            assertEquals(401, server.get("/api/me", "Authorization", bearer(foreign)).status);

            String none64 = Base64Url.encode("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
                    + "." + Base64Url.encode(("{\"sub\":\"admin\",\"scope\":\"orders:read\",\"exp\":"
                    + (now() + 300) + "}").getBytes(StandardCharsets.UTF_8)) + ".";
            Reply unsigned = server.get("/api/orders/1", "Authorization", bearer(none64));
            assertEquals(401, unsigned.status);
            assertEquals("Bearer error=\"invalid_token\", error_description=\"The token's "
                    + "algorithm, none, is not one this decoder accepts\", error_uri=\"" + SPEC + "\"",
                    unsigned.header("WWW-Authenticate"));

            // An EC token to the RSA verifier.
            assertTrue(server.get("/api/me", "Authorization", bearer(token(p256(),
                    claims(null).build()))).header("WWW-Authenticate").contains(
                            "The token's algorithm, ES256, is not one this decoder accepts"));

            Reply malformed = server.get("/api/me", "Authorization", "Bearer two words");
            assertEquals(401, malformed.status);
            assertEquals("Bearer error=\"invalid_token\", error_description=\"Bearer token is "
                    + "malformed\", error_uri=\"" + SPEC + "\"", malformed.header("WWW-Authenticate"));
            assertEquals(401, server.get("/api/me", "Authorization", "Bearer").status);
            assertEquals(401, server.get("/api/me", "Authorization", "Bearer not-a-jwt").status);
            // What a token's author wrote does not reach the header raw.
            String hostile = Base64Url.encode("{\"alg\":\"a\\\"\\r\\nX-Injected: 1\"}"
                    .getBytes(StandardCharsets.UTF_8)) + ".e30.AAAA";
            Reply injected = server.get("/api/me", "Authorization", bearer(hostile));
            assertEquals(401, injected.status);
            assertNull(injected.header("X-Injected"));
            assertTrue(injected.header("WWW-Authenticate").contains("algorithm, a???X-Injected: 1,"),
                    injected.toString());
            assertEquals(reached, server.reached().size(), "a refused token never reaches the app");

            // A token in the address is not read unless that was asked for.
            assertEquals(401, server.get("/api/me?access_token=" + good).status);
        }
    }

    @Test
    @DisplayName("a realm, when the chain names one, is in every challenge")
    void realm() throws Exception {
        try (SecuredServer server = start(o -> o.realmName("orders").jwt(
                jwt -> jwt.decoder(rsaDecoder())))) {
            assertEquals("Bearer realm=\"orders\"", server.get("/api/me")
                    .header("WWW-Authenticate"));
            assertEquals("Bearer realm=\"orders\", scope=\"orders:read\"",
                    server.get("/api/orders/7").header("WWW-Authenticate"));
            assertTrue(server.get("/api/me", "Authorization", "Bearer x.y.z")
                    .header("WWW-Authenticate").startsWith("Bearer realm=\"orders\", "
                            + "error=\"invalid_token\""));
            assertTrue(server.get("/api/orders/7", "Authorization", bearer(token(rsa(),
                    claims("orders:write").build()))).header("WWW-Authenticate").startsWith(
                            "Bearer realm=\"orders\", error=\"insufficient_scope\""));
        }
    }

    @Test
    @DisplayName("A good token that grants too little is 403 insufficient_scope")
    void insufficientScope() throws Exception {
        try (SecuredServer server = start(o -> o.jwt(jwt -> jwt.decoder(rsaDecoder())))) {
            String writeOnly = token(rsa(), claims("orders:write").build());
            Reply denied = server.get("/api/orders/7", "Authorization", bearer(writeOnly));
            assertEquals(403, denied.status, denied.toString());
            String insufficient = "Bearer error=\"insufficient_scope\", error_description=\"The "
                    + "request requires higher privileges than provided by the access token.\", "
                    + "error_uri=\"" + SPEC + "\", scope=\"orders:read\"";
            assertEquals(insufficient, denied.header("WWW-Authenticate"));
            assertEquals(200, server.get("/api/me", "Authorization", bearer(writeOnly)).status);

            // A token accepted without being sent -- what a test's jwt() does
            // -- is refused exactly as the one that was sent.
            java.util.Map<String, Object> headers = new java.util.LinkedHashMap<String, Object>();
            headers.put("alg", "none");
            java.util.Map<String, Object> accepted = new java.util.LinkedHashMap<String, Object>();
            accepted.put("sub", "ada");
            accepted.put("scope", "orders:write");
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new com.codename1.backend.security.oauth2.server.resource
                    .JwtAuthenticationToken(new com.codename1.backend.security.oauth2.jwt.Jwt(
                            "token", headers, accepted), java.util.Collections.singletonList(
                                    new SimpleGrantedAuthority("SCOPE_orders:write"))));
            com.codename1.impl.backend.security.SecurityAccess.get().testContext(context);
            try {
                Reply simulated = server.get("/api/orders/7");
                assertEquals(403, simulated.status, simulated.toString());
                assertEquals(insufficient, simulated.header("WWW-Authenticate"));
                assertEquals(denied.body, simulated.body);
                assertEquals(200, server.get("/api/me").status);
            } finally {
                com.codename1.impl.backend.security.SecurityAccess.get().testContext(null);
            }
            // No scope claim at all: authenticated, and granted nothing.
            String scopeless = token(rsa(), claims(null).build());
            assertEquals("ada [] Jwt", server.get("/api/me", "Authorization", bearer(scopeless)).body);
            assertEquals(403, server.get("/api/orders/7", "Authorization", bearer(scopeless)).status);
            // A scope is not a role.
            assertEquals(403, server.get("/api/admin/x", "Authorization", bearer(token(rsa(),
                    claims("ADMIN admin ROLE_ADMIN").build()))).status);
            // The scp claim, as a list.
            java.util.List<String> scp = java.util.Arrays.asList("orders:read", "profile");
            Reply listed = server.get("/api/me", "Authorization", bearer(token(rsa(),
                    claims(null).claim("scp", scp).build())));
            assertEquals("ada [SCOPE_orders:read, SCOPE_profile] Jwt", listed.body);
        }
    }

    @Test
    @DisplayName("Authorities and the name from other claims, by a converter")
    void converter() throws Exception {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        converter.setPrincipalClaimName("preferred_username");
        try (SecuredServer server = start(o -> o.jwt(jwt -> jwt.decoder(rsaDecoder())
                .jwtAuthenticationConverter(converter)))) {
            String admin = token(rsa(), claims("orders:read").claim("roles",
                    java.util.Arrays.asList("ADMIN", "USER")).claim("preferred_username", "ada.l")
                    .build());
            assertEquals("ada.l [ROLE_ADMIN, ROLE_USER] Jwt",
                    server.get("/api/me", "Authorization", bearer(admin)).body);
            assertEquals(200, server.get("/api/admin/x", "Authorization", bearer(admin)).status);
            // The scope claim is no longer what grants.
            assertEquals(403, server.get("/api/orders/1", "Authorization", bearer(admin)).status);
        }
        // The same converter, found as a bean.
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "test",
                new Object[] {converter, rsaDecoder()}, APP,
                api(o -> o.jwt(Customizer.<OAuth2ResourceServerConfigurer.JwtConfigurer>withDefaults())))) {
            assertEquals("ada.l [ROLE_USER] Jwt", server.get("/api/me", "Authorization",
                    bearer(token(rsa(), claims(null).claim("roles", "USER")
                            .claim("preferred_username", "ada.l").build()))).body);
        }
    }

    @Test
    @DisplayName("CSRF is not asked of a request with a bearer token, and still is of the rest")
    void csrf() throws Exception {
        try (SecuredServer server = start(o -> o.jwt(jwt -> jwt.decoder(rsaDecoder())))) {
            String good = token(rsa(), claims("orders:read").build());
            Reply posted = server.call("POST", "/api/orders/7", "{}", "application/json",
                    "Authorization", bearer(good));
            assertEquals(200, posted.status, posted.toString());
            // Without a token the same POST needs the CSRF token it does not have.
            Reply bare = server.call("POST", "/api/open/form", "a=b",
                    "application/x-www-form-urlencoded");
            assertEquals(403, bare.status, bare.toString());
            assertNull(bare.header("WWW-Authenticate"));
            // A bad bearer token does not buy the exemption a good one has.
            assertEquals(401, server.call("POST", "/api/open/form", "a=b",
                    "application/x-www-form-urlencoded", "Authorization", "Bearer bad.token.x")
                    .status);
        }
    }

    @Test
    @DisplayName("A token in the query is read only when that is turned on, and on a GET")
    void queryParameter() throws Exception {
        final DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        resolver.setAllowUriQueryParameter(true);
        try (SecuredServer server = start(o -> o.bearerTokenResolver(resolver)
                .jwt(jwt -> jwt.decoder(rsaDecoder())))) {
            String good = token(rsa(), claims("orders:read").build());
            assertEquals(200, server.get("/api/me?access_token=" + good).status);
            assertEquals(200, server.get("/api/me", "Authorization", bearer(good)).status);
            Reply both = server.get("/api/me?access_token=" + good, "Authorization", bearer(good));
            assertEquals(400, both.status);
            assertEquals("Bearer error=\"invalid_request\", error_description=\"Found multiple "
                    + "bearer tokens in the request\", error_uri=\"" + SPEC + "\"",
                    both.header("WWW-Authenticate"));
            assertEquals(401, server.get("/api/me?access_token=a%20b").status);
            // Not on a POST: the address of a POST is as logged as any other.
            assertEquals(403, server.call("POST", "/api/me?access_token=" + good, "{}",
                    "application/json").status);
        }
        // Another header, for a server behind a proxy that keeps Authorization.
        final DefaultBearerTokenResolver forwarded = new DefaultBearerTokenResolver();
        forwarded.setBearerTokenHeaderName("X-Forwarded-Authorization");
        try (SecuredServer server = start(o -> o.bearerTokenResolver(forwarded)
                .jwt(jwt -> jwt.decoder(rsaDecoder())))) {
            String good = token(rsa(), claims(null).build());
            assertEquals(401, server.get("/api/me", "Authorization", bearer(good)).status);
            assertEquals(200, server.get("/api/me", "X-Forwarded-Authorization", bearer(good)).status);
        }
    }

    @Test
    @DisplayName("Several issuers: each token is verified by the issuer it names, and only that one")
    void multiIssuer() throws Exception {
        final Jwk acme = rsa();
        final Jwk partner = p256();
        Map<String, JwtDecoder> decoders = new LinkedHashMap<String, JwtDecoder>();
        DefaultJwtDecoder acmeDecoder = DefaultJwtDecoder.withPublicKey(acme.getPublicKey()).build();
        acmeDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("https://acme.example.com"));
        DefaultJwtDecoder partnerDecoder = DefaultJwtDecoder.withPublicKey(partner.getPublicKey())
                .build();
        partnerDecoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator("https://partner.example.com")));
        decoders.put("https://acme.example.com", acmeDecoder);
        decoders.put("https://partner.example.com", partnerDecoder);
        final JwtIssuerAuthenticationManagerResolver resolver =
                JwtIssuerAuthenticationManagerResolver.fromDecoders(decoders);
        try (SecuredServer server = start(o -> o.authenticationManagerResolver(resolver))) {
            String fromAcme = token(acme, claims("orders:read").issuer("https://acme.example.com")
                    .subject("ada@acme").build());
            String fromPartner = token(partner, claims("orders:read")
                    .issuer("https://partner.example.com").subject("bob@partner").build());
            assertEquals("ada@acme [SCOPE_orders:read] Jwt",
                    server.get("/api/me", "Authorization", bearer(fromAcme)).body);
            assertEquals("bob@partner [SCOPE_orders:read] Jwt",
                    server.get("/api/me", "Authorization", bearer(fromPartner)).body);

            // The partner's key, claiming to be acme: acme's decoder judges it,
            // and acme never signed it.
            String impostor = token(partner, claims("orders:read").issuer("https://acme.example.com")
                    .subject("root").build());
            Reply refused = server.get("/api/me", "Authorization", bearer(impostor));
            assertEquals(401, refused.status);
            assertTrue(refused.header("WWW-Authenticate").contains(
                    "The token's algorithm, ES256, is not one this decoder accepts"),
                    refused.toString());
            // The same with a key of the right kind: the signature is what fails.
            String forged = token(Jwk.ofPrivateKey(Crypto.generateRsaKey(2048)),
                    claims("orders:read").issuer("https://acme.example.com").build());
            assertTrue(server.get("/api/me", "Authorization", bearer(forged))
                    .header("WWW-Authenticate").contains("There is no RS256 key with the token's key id"));

            // An issuer nobody trusts, however well it signs.
            String stranger = token(acme, claims("orders:read").issuer("https://evil.example.com")
                    .build());
            Reply untrusted = server.get("/api/me", "Authorization", bearer(stranger));
            assertEquals(401, untrusted.status);
            assertEquals("Bearer error=\"invalid_token\", error_description=\"Invalid issuer\", "
                    + "error_uri=\"" + SPEC + "\"", untrusted.header("WWW-Authenticate"));
            String nameless = token(acme, JwtClaimsSet.builder().subject("ada")
                    .expiresAt(now() + 300).build());
            assertTrue(server.get("/api/me", "Authorization", bearer(nameless))
                    .header("WWW-Authenticate").contains("error_description=\"Missing issuer\""));
            assertTrue(server.get("/api/me", "Authorization", "Bearer abc")
                    .header("WWW-Authenticate").contains("Malformed token"));
            assertEquals("Bearer", server.get("/api/me").header("WWW-Authenticate"));
        }
    }

    @Test
    @DisplayName("An issuer's metadata and keys, fetched over HTTP by the runtime's own client")
    void issuerOverHttp() throws Exception {
        final Jwk signing = p256().withKeyId("2026-10");
        final String[] issuer = new String[1];
        final int[] metadataRequests = new int[1];
        // The JDK's own little server plays the identity provider: one process
        // runs one backend, and that one is the resource server under test.
        com.sun.net.httpserver.HttpServer provider = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/", new com.sun.net.httpserver.HttpHandler() {
            @Override
            public void handle(com.sun.net.httpserver.HttpExchange exchange)
                    throws java.io.IOException {
                String path = exchange.getRequestURI().getPath();
                String body = null;
                if ("/.well-known/openid-configuration".equals(path)) {
                    synchronized (metadataRequests) {
                        metadataRequests[0]++;
                    }
                    body = "{\"issuer\":\"" + issuer[0] + "\",\"jwks_uri\":\"" + issuer[0]
                            + "/keys\",\"id_token_signing_alg_values_supported\":[\"ES256\"]}";
                } else if ("/keys".equals(path)) {
                    body = JwkSet.of(signing).toJson();
                }
                byte[] bytes = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(body == null ? 404 : 200, bytes.length == 0 ? -1
                        : bytes.length);
                if (bytes.length > 0) {
                    exchange.getResponseBody().write(bytes);
                }
                exchange.close();
            }
        });
        provider.start();
        try {
            issuer[0] = "http://127.0.0.1:" + provider.getAddress().getPort();
            Properties settings = SecuredServer.settings();
            settings.setProperty(OAuth2ResourceServerConfigurer.ISSUER_URI, issuer[0]);
            settings.setProperty(OAuth2ResourceServerConfigurer.AUDIENCES, "billing-api, orders-api");
            try (SecuredServer server = SecuredServer.start(settings, "test", new Object[0], APP,
                    api(o -> o.jwt(Customizer.<OAuth2ResourceServerConfigurer.JwtConfigurer>
                            withDefaults())))) {
                synchronized (metadataRequests) {
                    assertEquals(0, metadataRequests[0], "nothing is fetched at start-up");
                }
                String good = token(signing, claims("orders:read").issuer(issuer[0]).build());
                Reply me = server.get("/api/me", "Authorization", bearer(good));
                assertEquals(200, me.status, me.toString());
                assertEquals("ada [SCOPE_orders:read] Jwt", me.body);
                assertEquals(200, server.get("/api/orders/1", "Authorization", bearer(good)).status);
                synchronized (metadataRequests) {
                    assertEquals(1, metadataRequests[0], "the metadata is read once");
                }
                // The issuer the properties name is required of every token.
                assertTrue(server.get("/api/me", "Authorization", bearer(token(signing,
                        claims(null).issuer("https://id.example.com").build())))
                        .header("WWW-Authenticate").contains("The iss claim is not valid"));
                // And so is one of the audiences.
                assertTrue(server.get("/api/me", "Authorization", bearer(token(signing,
                        claims(null).issuer(issuer[0]).audience("another-api").build())))
                        .header("WWW-Authenticate").contains("The aud claim is not valid"));
                // Signed by a key the issuer does not publish.
                assertEquals(401, server.get("/api/me", "Authorization", bearer(token(p256()
                        .withKeyId("stolen"), claims(null).issuer(issuer[0]).build()))).status);
            }
        } finally {
            provider.stop(0);
        }
    }

    @Test
    @DisplayName("The properties: a key file, a key set address, and what is said when none is set")
    void properties() throws Exception {
        Path dir = Files.createTempDirectory("resource-server");
        Path pem = dir.resolve("issuer.pem");
        Files.write(pem, KeyFixtures.EC256_PUBLIC_PEM.getBytes(StandardCharsets.US_ASCII));
        Properties settings = SecuredServer.settings();
        settings.setProperty(OAuth2ResourceServerConfigurer.PUBLIC_KEY_LOCATION, "file:" + pem);
        settings.setProperty(OAuth2ResourceServerConfigurer.AUDIENCES, "orders-api");
        try (SecuredServer server = SecuredServer.start(settings, "test", new Object[0], APP,
                api(o -> o.jwt(Customizer.<OAuth2ResourceServerConfigurer.JwtConfigurer>
                        withDefaults())))) {
            // The key in the file is an EC key, so ES256 is what it verifies.
            assertEquals(200, server.get("/api/me", "Authorization", bearer(token(p256(),
                    claims(null).build()))).status);
            assertEquals(401, server.get("/api/me", "Authorization", bearer(token(rsa(),
                    claims(null).build()))).status);
            assertTrue(server.get("/api/me", "Authorization", bearer(token(p256(),
                    claims(null).audience("billing-api").build()))).header("WWW-Authenticate")
                    .contains("The aud claim is not valid"));
        }

        final Properties empty = SecuredServer.settings();
        IllegalStateException none = assertThrows(IllegalStateException.class,
                () -> SecuredServer.start(empty, "test", new Object[0], APP, api(o -> o.jwt(
                        Customizer.<OAuth2ResourceServerConfigurer.JwtConfigurer>withDefaults()))));
        assertEquals("oauth2ResourceServer().jwt() needs a way to verify tokens, and this "
                + "application has none. Declare a JwtDecoder bean, call decoder(...) or "
                + "jwkSetUri(...) on the jwt() configurer, or set "
                + "cn1.security.oauth2.resourceserver.jwt.issuer-uri, "
                + "cn1.security.oauth2.resourceserver.jwt.jwk-set-uri or "
                + "cn1.security.oauth2.resourceserver.jwt.public-key-location.", none.getMessage());
        assertEquals("oauth2ResourceServer() needs to be told how tokens are verified: call "
                + "jwt(...), or authenticationManagerResolver(...)", assertThrows(
                        IllegalStateException.class, () -> SecuredServer.start(empty, "test",
                                new Object[0], APP, api(o -> { }))).getMessage());
        final Properties bad = SecuredServer.settings();
        bad.setProperty(OAuth2ResourceServerConfigurer.JWK_SET_URI, "https://id.example.com/jwks");
        bad.setProperty(OAuth2ResourceServerConfigurer.JWS_ALGORITHMS, "RS256, none");
        assertEquals("cn1.security.oauth2.resourceserver.jwt.jws-algorithms names none, which is "
                + "not one of RS256, RS384, RS512, PS256, ES256 and ES384", assertThrows(
                        IllegalStateException.class, () -> SecuredServer.start(bad, "test",
                                new Object[0], APP, api(o -> o.jwt(Customizer
                                        .<OAuth2ResourceServerConfigurer.JwtConfigurer>withDefaults()))))
                        .getMessage());
        final Properties missing = SecuredServer.settings();
        missing.setProperty(OAuth2ResourceServerConfigurer.PUBLIC_KEY_LOCATION,
                dir.resolve("absent.pem").toString());
        assertTrue(assertThrows(IllegalStateException.class, () -> SecuredServer.start(missing,
                "test", new Object[0], APP, api(o -> o.jwt(Customizer
                        .<OAuth2ResourceServerConfigurer.JwtConfigurer>withDefaults()))))
                .getMessage().startsWith("cn1.security.oauth2.resourceserver.jwt.public-key-location: "
                        + "Could not open the key file "));
    }

    @Test
    @DisplayName("A token that could not be judged is this server's failure, not a bad token")
    void keysUnavailable() throws Exception {
        final JwtDecoder blind = new JwtDecoder() {
            @Override
            public Jwt decode(String token) {
                throw new JwtException("Could not get the keys to verify the token with: down");
            }
        };
        try (SecuredServer server = start(o -> o.jwt(jwt -> jwt.decoder(blind)))) {
            Reply reply = server.get("/api/me", "Authorization", bearer(token(rsa(),
                    claims(null).build())));
            assertEquals(500, reply.status, reply.toString());
            assertNull(reply.header("WWW-Authenticate"));
            assertFalse(reply.body.contains("down"), "the reason stays in the server");
            assertTrue(server.reached().isEmpty());
        }
    }

    @Test
    @DisplayName("Beside form login: a browser is sent to the login page, a token is still a token")
    void besideFormLogin() throws Exception {
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "test",
                new Object[] {new com.codename1.backend.security.core.userdetails
                        .InMemoryUserDetailsManager(com.codename1.backend.security.core.userdetails.User
                                .withUsername("ada").password("{noop}pw").roles("USER").build())},
                APP, new SecuredServer.Chain() {
                    @Override
                    public SecurityFilterChain build(HttpSecurity http) {
                        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                            .formLogin(Customizer.<FormLoginConfigurer>withDefaults())
                            .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.decoder(rsaDecoder())));
                        return http.build();
                    }
                })) {
            assertEquals(302, server.get("/page").status);
            assertEquals(200, server.get("/page", "Authorization", bearer(token(rsa(),
                    claims(null).build()))).status);
            Reply bad = server.get("/page", "Authorization", "Bearer x.y.z");
            assertEquals(401, bad.status);
            assertTrue(bad.header("WWW-Authenticate").startsWith("Bearer error=\"invalid_token\""));
        }
    }

    @Test
    @DisplayName("A signature algorithm other than the default is accepted when it is named")
    void namedAlgorithm() throws Exception {
        final JwtDecoder pss = DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.ofPublicKey(
                Base64.decode(KeyFixtures.RSA_PUBLIC_DER)))).jwsAlgorithms(SignatureAlgorithm.PS256)
                .build();
        try (SecuredServer server = start(o -> o.jwt(jwt -> jwt.decoder(pss)))) {
            String ps256 = new DefaultJwtEncoder(JwkSet.of(rsa())).encode(JwtEncoderParameters.from(
                    com.codename1.backend.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.PS256)
                            .build(), claims(null).build())).getTokenValue();
            assertEquals(200, server.get("/api/me", "Authorization", bearer(ps256)).status);
            assertEquals(401, server.get("/api/me", "Authorization", bearer(token(rsa(),
                    claims(null).build()))).status);
        }
    }
}
