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
package com.codename1.backend.security.oauth2.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// The parts of a sign-in through another provider that need no server: the
/// rules that tie a provider's user to a local one, registrations read from
/// settings, the claims an ID token must have right.
class OAuth2ClientTest {
    @TempDir
    File dir;

    // ------------------------------------------------------ account linking

    private final InMemoryFederatedIdentityRepository identities =
            new InMemoryFederatedIdentityRepository();
    private final InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
            User.withUsername("ada@example.com").password("{noop}pw").roles("ADMIN").build(),
            User.withUsername("locked@example.com").password("{noop}pw").roles("USER")
                    .accountLocked(true).build());

    private static Map<String, Object> attributes(Object email, Object verified) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (email != null) {
            out.put("email", email);
        }
        if (verified != null) {
            out.put("email_verified", verified);
        }
        return out;
    }

    private static String refusal(Runnable sign) {
        OAuth2AuthenticationException refused = assertThrows(
                OAuth2AuthenticationException.class, sign::run);
        return refused.getError().getErrorCode();
    }

    @Test
    @DisplayName("an identity already tied to a user signs in as that user, whatever else changed")
    void existingLinkWins() {
        LinkingOAuth2UserService service = new LinkingOAuth2UserService(identities, users);
        identities.link("google", "sub-1", "ada@example.com");
        // The provider now reports another address, and an unverified one: the
        // identity is what was tied, and the identity has not changed.
        UserDetails who = service.resolve("google", "sub-1", attributes("new@elsewhere.example",
                Boolean.FALSE));
        assertEquals("ada@example.com", who.getUsername());
        // No address at all.
        assertEquals("ada@example.com", service.resolve("google", "sub-1",
                attributes(null, null)).getUsername());
        // The same subject at another provider is another identity.
        assertEquals(LinkingOAuth2UserService.EMAIL_NOT_VERIFIED, refusal(
                () -> service.resolve("github", "sub-1", attributes(null, null))));
        // A tie to a user who may not sign in admits nobody.
        identities.link("google", "sub-9", "locked@example.com");
        assertEquals(LinkingOAuth2UserService.ACCOUNT_UNAVAILABLE, refusal(
                () -> service.resolve("google", "sub-9", attributes(null, null))));
    }

    @Test
    @DisplayName("a verified address ties the identity to the local user of that name")
    void verifiedEmailLinks() {
        LinkingOAuth2UserService service = new LinkingOAuth2UserService(identities, users);
        assertNull(identities.findUsername("google", "sub-2"));
        UserDetails who = service.resolve("google", "sub-2", attributes("ada@example.com",
                Boolean.TRUE));
        assertEquals("ada@example.com", who.getUsername());
        assertEquals("[ROLE_ADMIN]", who.getAuthorities().toString());
        assertEquals("ada@example.com", identities.findUsername("google", "sub-2"));
        // Apple writes the flag as text.
        assertEquals("ada@example.com", service.resolve("apple", "sub-3",
                attributes("ada@example.com", "true")).getUsername());
        assertEquals(2, identities.findByUsername("ADA@example.com").size());
        assertTrue(identities.unlink("apple", "sub-3"));
        assertFalse(identities.unlink("apple", "sub-3"));
    }

    @Test
    @DisplayName("an address the provider does not vouch for is refused, whoever has it")
    void unverifiedEmailRefused() {
        LinkingOAuth2UserService service = new LinkingOAuth2UserService(identities, users);
        service.setCreateUsers(true);
        for (Object verified : new Object[] {Boolean.FALSE, null, "false", "yes",
            Integer.valueOf(1)}) {
            // An address a local user has: taking the provider's word would
            // hand over ada's account.
            assertEquals(LinkingOAuth2UserService.EMAIL_NOT_VERIFIED, refusal(
                    () -> service.resolve("google", "sub-4", attributes("ada@example.com",
                            verified))), String.valueOf(verified));
            // And one nobody has: no account is made on a provider's hearsay.
            assertEquals(LinkingOAuth2UserService.EMAIL_NOT_VERIFIED, refusal(
                    () -> service.resolve("google", "sub-5", attributes("new@example.com",
                            verified))), String.valueOf(verified));
        }
        assertEquals(LinkingOAuth2UserService.EMAIL_NOT_VERIFIED, refusal(
                () -> service.resolve("google", "sub-4", attributes(null, Boolean.TRUE))));
        assertNull(identities.findUsername("google", "sub-4"));
        assertFalse(users.userExists("new@example.com"));
    }

    @Test
    @DisplayName("a verified address nobody has makes a new user only when the application allows it")
    void newUsers() {
        LinkingOAuth2UserService service = new LinkingOAuth2UserService(identities, users);
        assertEquals(LinkingOAuth2UserService.ACCOUNT_NOT_FOUND, refusal(
                () -> service.resolve("google", "sub-6", attributes("new@example.com",
                        Boolean.TRUE))));
        assertFalse(users.userExists("new@example.com"));
        assertNull(identities.findUsername("google", "sub-6"));

        service.setCreateUsers(true);
        service.setNewUserAuthorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_MEMBER")));
        UserDetails made = service.resolve("google", "sub-6", attributes("new@example.com",
                Boolean.TRUE));
        assertEquals("new@example.com", made.getUsername());
        assertEquals("[ROLE_MEMBER]", made.getAuthorities().toString());
        assertEquals("new@example.com", identities.findUsername("google", "sub-6"));
        // A password nobody knows, under an id no encoder has.
        String password = users.loadUserByUsername("new@example.com").getPassword();
        assertTrue(password.startsWith("{federated}") && password.length() == 11 + 43, password);
        // The next sign-in is the first rule's.
        assertEquals("new@example.com", service.resolve("google", "sub-6",
                attributes(null, null)).getUsername());

        // Users that cannot be added to cannot be told to be.
        LinkingOAuth2UserService fixed = new LinkingOAuth2UserService(identities,
                username -> users.loadUserByUsername(username));
        assertThrows(IllegalArgumentException.class, () -> fixed.setCreateUsers(true));
    }

    @Test
    @DisplayName("the service names the user after the local account, for both kinds of provider")
    void serviceReturnsTheLocalUser() {
        LinkingOAuth2UserService service = new LinkingOAuth2UserService(identities, users);
        final Map<String, Object> remote = attributes("ada@example.com", Boolean.TRUE);
        remote.put("id", Integer.valueOf(42));
        service.setOAuth2UserService(request -> new DefaultOAuth2User(Arrays.asList(
                new SimpleGrantedAuthority("OAUTH2_USER")), remote, "id"));
        ClientRegistration hub = ClientRegistration.withRegistrationId("hub").clientId("c")
                .authorizationUri("https://hub.example/a").tokenUri("https://hub.example/t")
                .build();
        OAuth2User user = service.loadUser(new OAuth2UserRequest(hub,
                new OAuth2AccessTokenResponse(Collections.singletonMap("access_token", "t"))));
        assertEquals("ada@example.com", user.getName());
        assertEquals("[ROLE_ADMIN, OAUTH2_USER]", user.getAuthorities().toString());
        assertEquals(Integer.valueOf(42), user.getAttributes().get("id"));
        assertEquals("ada@example.com", identities.findUsername("hub", "42"));

        // OpenID Connect: tied by the subject, whatever names the user.
        final Map<String, Object> claims = attributes("ada@example.com", Boolean.TRUE);
        claims.put("sub", "oidc-sub");
        claims.put("preferred_username", "ada-at-the-provider");
        final Jwt token = new Jwt("a.b.c", Collections.<String, Object>singletonMap("alg", "RS256"),
                claims);
        service.setOidcUserService(request -> new DefaultOidcUser(Arrays.asList(
                new SimpleGrantedAuthority("OIDC_USER")), token, claims, "preferred_username"));
        ClientRegistration oidc = ClientRegistration.withRegistrationId("acme").clientId("c")
                .issuerUri("https://id.example").scope("openid").build();
        OAuth2User viaGeneric = service.loadUser(new OidcUserRequest(oidc,
                new OAuth2AccessTokenResponse(Collections.singletonMap("access_token", "t")),
                token));
        assertEquals("ada@example.com", viaGeneric.getName());
        assertTrue(viaGeneric instanceof OidcUser);
        assertSame(token, ((OidcUser) viaGeneric).getIdToken());
        assertTrue(((OidcUser) viaGeneric).isEmailVerified());
        assertEquals("ada@example.com", identities.findUsername("acme", "oidc-sub"));
        assertNull(identities.findUsername("acme", "ada-at-the-provider"));
    }

    // -------------------------------------------------------- registrations

    @Test
    @DisplayName("registrations are read from the settings, by common provider or by a provider block")
    void fromConfig() throws Exception {
        File key = new File(dir, "AuthKey.p8");
        Files.write(key.toPath(), KeyFixtures.EC256_PKCS8_PEM.getBytes("US-ASCII"));
        Properties p = new Properties();
        String r = ClientRegistrations.REGISTRATION;
        p.setProperty(r + "google.client-id", "g-id");
        p.setProperty(r + "google.client-secret", "g-secret");
        p.setProperty(r + "work.provider", "microsoft");
        p.setProperty(r + "work.client-id", "m-id");
        p.setProperty(r + "work.client-secret", "m-secret");
        p.setProperty(r + "work.scope", "openid, email");
        p.setProperty(r + "apple.client-id", "com.example.web");
        p.setProperty(r + "apple.apple-team-id", "TEAM123456");
        p.setProperty(r + "apple.apple-key-id", "KEY1234567");
        p.setProperty(r + "apple.apple-private-key", key.getPath());
        p.setProperty(r + "acme.client-id", "web");
        p.setProperty(r + "acme.client-authentication-method", "none");
        p.setProperty(r + "acme.provider", "acme-id");
        p.setProperty(r + "acme.redirect-uri", "https://app.example/cb");
        p.setProperty(r + "acme.client-name", "Acme");
        p.setProperty(ClientRegistrations.PROVIDER + "acme-id.issuer-uri", "https://id.example");
        p.setProperty(ClientRegistrations.PROVIDER + "acme-id.user-name-attribute", "email");
        List<ClientRegistration> all = ClientRegistrations.fromConfig(Config.of(p, "prod"));
        Map<String, ClientRegistration> byId = new LinkedHashMap<String, ClientRegistration>();
        for (ClientRegistration registration : all) {
            byId.put(registration.getRegistrationId(), registration);
        }
        assertEquals("[acme, apple, google, work]", byId.keySet().toString());

        ClientRegistration google = byId.get("google");
        assertEquals("g-id", google.getClientId());
        assertEquals("g-secret", google.resolveClientSecret());
        assertEquals("https://accounts.google.com", google.getProviderDetails().getIssuerUri());
        assertEquals("[openid, profile, email]", google.getScopes().toString());
        assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                google.getClientAuthenticationMethod());
        assertEquals("{baseUrl}/login/oauth2/code/{registrationId}", google.getRedirectUri());

        ClientRegistration work = byId.get("work");
        assertEquals("[openid, email]", work.getScopes().toString());
        assertNull(work.getProviderDetails().getIssuerUri());
        assertEquals("https://login.microsoftonline.com/{tenantid}/v2.0",
                work.getProviderDetails().getIssuerTemplate());

        ClientRegistration apple = byId.get("apple");
        assertEquals(ClientRegistration.FORM_POST, apple.getResponseMode());
        assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_POST,
                apple.getClientAuthenticationMethod());
        assertNull(apple.getClientSecret());
        assertEquals(3, apple.resolveClientSecret().split("\\.").length);

        ClientRegistration acme = byId.get("acme");
        assertEquals(ClientAuthenticationMethod.NONE, acme.getClientAuthenticationMethod());
        assertEquals("https://id.example", acme.getProviderDetails().getIssuerUri());
        // The endpoints are the issuer's to say, when a user first signs in.
        assertNull(acme.getProviderDetails().getTokenUri());
        assertEquals("email", acme.getProviderDetails().getUserNameAttributeName());
        assertEquals("Acme", acme.getClientName());

        // Read from the issuer's metadata, which must be the issuer's own.
        final String metadata = "{\"issuer\":\"https://id.example\",\"authorization_endpoint\":"
                + "\"https://id.example/authorize\",\"token_endpoint\":\"https://id.example/token\","
                + "\"jwks_uri\":\"https://id.example/keys\",\"userinfo_endpoint\":"
                + "\"https://id.example/me\",\"id_token_signing_alg_values_supported\":[\"ES256\"]}";
        ClientRegistration resolved = ClientRegistrations.resolve(acme, uri -> metadata);
        assertEquals("https://id.example/token", resolved.getProviderDetails().getTokenUri());
        assertEquals("https://id.example/keys", resolved.getProviderDetails().getJwkSetUri());
        assertEquals("[ES256]", Arrays.toString(resolved.getProviderDetails()
                .getIdTokenAlgorithms()));
        assertSame(google, ClientRegistrations.resolve(google, uri -> {
            throw new java.io.IOException("a registration that names its endpoints asks nobody");
        }));
        assertThrows(IllegalArgumentException.class, () -> ClientRegistrations.resolve(acme,
                uri -> metadata.replace("\"issuer\":\"https://id.example\"",
                        "\"issuer\":\"https://evil.example\"")));

        assertThrows(IllegalArgumentException.class, () -> ClientRegistration
                .withRegistrationId("x").clientId("c").build());
        assertThrows(IllegalArgumentException.class, () -> ClientRegistration
                .withRegistrationId("has/slash"));
        assertThrows(IllegalArgumentException.class, () -> ClientRegistration
                .withRegistrationId("x").clientId("c").authorizationUri("https://a/b")
                .tokenUri("https://a/c").scope("openid").build());
        assertEquals(CommonOAuth2Provider.GITHUB, CommonOAuth2Provider.of("GitHub"));
        assertNull(CommonOAuth2Provider.of("acme-id"));
    }

    // ----------------------------------------------------------- ID tokens

    private static Jwt token(Map<String, Object> claims) {
        return new Jwt("a.b.c", Collections.<String, Object>singletonMap("alg", "RS256"), claims);
    }

    private static Map<String, Object> claims(String issuer, Object audience) {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("iss", issuer);
        claims.put("sub", "user-1");
        claims.put("aud", audience);
        claims.put("iat", Long.valueOf(now));
        claims.put("exp", Long.valueOf(now + 300));
        return claims;
    }

    @Test
    @DisplayName("a multi-tenant issuer is held to the tenant template, with the token's own tenant")
    void tenantTemplate() {
        ClientRegistration microsoft = CommonOAuth2Provider.MICROSOFT.getBuilder("work")
                .clientId("m-id").clientSecret("s").build();
        OidcIdTokenDecoderFactory.IdTokenValidator validator =
                new OidcIdTokenDecoderFactory.IdTokenValidator(microsoft);
        String tenant = "9188040d-6c67-4c5b-b112-36a304b66dad";
        Map<String, Object> good = claims("https://login.microsoftonline.com/" + tenant + "/v2.0",
                "m-id");
        good.put("tid", tenant);
        assertFalse(validator.validate(token(good)).hasErrors());
        // The literal "common" is nobody's issuer.
        Map<String, Object> common = claims("https://login.microsoftonline.com/common/v2.0",
                "m-id");
        common.put("tid", tenant);
        assertTrue(validator.validate(token(common)).hasErrors());
        // Another tenant's issuer than the one the token says it is of.
        Map<String, Object> mixed = claims("https://login.microsoftonline.com/" + tenant + "/v2.0",
                "m-id");
        mixed.put("tid", "11111111-2222-3333-4444-555555555555");
        assertTrue(validator.validate(token(mixed)).hasErrors());
        // No tenant; and a "tenant" that would write another host into the issuer.
        assertTrue(validator.validate(token(claims("https://login.microsoftonline.com/" + tenant
                + "/v2.0", "m-id"))).hasErrors());
        Map<String, Object> crafted = claims("https://login.microsoftonline.com/x/../evil/v2.0",
                "m-id");
        crafted.put("tid", "x/../evil");
        assertTrue(validator.validate(token(crafted)).hasErrors());
        // And another client's token from the right tenant.
        Map<String, Object> theirs = claims("https://login.microsoftonline.com/" + tenant
                + "/v2.0", "other-app");
        theirs.put("tid", tenant);
        assertEquals("The aud claim does not name this client",
                validator.validate(token(theirs)).getErrors().get(0).getDescription());
    }

    @Test
    @DisplayName("Apple's client secret is signed once an hour, not once a sign-in")
    void appleSecretIsCached() throws Exception {
        final long[] now = {1700000000000L};
        AppleClientSecret secret = new AppleClientSecret("TEAM123456", "KEY1234567",
                KeyFixtures.EC256_PKCS8_PEM);
        secret.setClock(new Clock() {
            @Override
            public long currentTimeMillis() {
                return now[0];
            }
        });
        ClientRegistration apple = CommonOAuth2Provider.APPLE.getBuilder("apple")
                .clientId("com.example.web").clientSecretSupplier(secret).build();
        String first = apple.resolveClientSecret();
        now[0] += 54 * 60 * 1000L;
        assertEquals(first, apple.resolveClientSecret());
        // Under five minutes left: a new one.
        now[0] += 2 * 60 * 1000L;
        String second = apple.resolveClientSecret();
        assertNotEquals(first, second);
        assertEquals(second, apple.resolveClientSecret());
        // An RSA key is not what Apple issues.
        assertThrows(java.io.IOException.class, () -> new AppleClientSecret("T", "K",
                KeyFixtures.RSA_PKCS8_PEM));
    }

    @Test
    @DisplayName("forms and queries are written and read the same way")
    void parameters() {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("redirect_uri", "https://a.example/cb?x=1&y=2");
        fields.put("scope", "openid read:user");
        fields.put("name", "caf\u00e9 + \u2603");
        fields.put("absent", null);
        String form = OAuth2Parameters.format(fields);
        assertEquals("redirect_uri=https%3A%2F%2Fa.example%2Fcb%3Fx%3D1%26y%3D2"
                + "&scope=openid%20read%3Auser&name=caf%C3%A9%20%2B%20%E2%98%83", form);
        Map<String, String> back = OAuth2Parameters.parse(form);
        assertEquals("https://a.example/cb?x=1&y=2", back.get("redirect_uri"));
        assertEquals("caf\u00e9 + \u2603", back.get("name"));
        assertEquals("a b", OAuth2Parameters.parse("v=a+b").get("v"));
        // A field given twice keeps its first value; a broken escape is no field.
        assertEquals("1", OAuth2Parameters.parse("v=1&v=2").get("v"));
        assertNull(OAuth2Parameters.parse("v=%zz").get("v"));
        assertNull(OAuth2Parameters.decode("%4"));
        assertEquals("com.acme.app:/cb?code=c&state=s", OAuth2Parameters.append(
                "com.acme.app:/cb", OAuth2Parameters.parse("code=c&state=s")));
        assertEquals("https://a/b?x=1&code=c", OAuth2Parameters.append("https://a/b?x=1",
                OAuth2Parameters.parse("code=c")));
        assertEquals("[a, b, c]", OAuth2Parameters.scopes(" a  b,c ").toString());
        // RFC 7636, appendix B.
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", OAuth2Parameters.sha256(
                "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
        assertEquals(43, OAuth2Parameters.random(32).length());
        assertNotEquals(OAuth2Parameters.random(32), OAuth2Parameters.random(32));
    }
}
