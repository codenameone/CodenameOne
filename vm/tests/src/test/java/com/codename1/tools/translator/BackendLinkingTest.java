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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A server carries the code of the security it declares, and of nothing else.
 *
 * The translator drops what nothing reaches, so whether a mechanism ends up in a binary is
 * decided by whether anything on a path from the entry point names it. That is a property of
 * how the security layer is put together -- a factory keyed by class, a default applied in a
 * constructor, a constant that holds one resolver of each kind -- and nothing in the JVM tests
 * can see it: there, every class is on the class path whatever refers to it. One careless
 * reference puts bcrypt, the login page and the JWT stack into every server with a chain, and
 * every test stays green.
 *
 * So this builds six whole servers, vm/backend/demo/linknone, linkjwt, linkform,
 * linkoauthlogin, linkauthserver and linkwebauthn -- no security, a JWT resource server only,
 * a form login only, a sign-in through another provider only, an authorization server only,
 * passkeys only -- runs each to prove it works, and reads the symbols of each binary. A class that is linked has a static
 * initializer symbol; the classes of what a server did not declare must have none.
 *
 * The same goes for the natives. They are compiled whole, and what leaves an uncalled one out
 * is the linker (see the dead-strip flags in vm/backend/build.sh): a server that signs nothing
 * must not hold the signature native, and the one that does sign must.
 *
 * Because the linker now drops what nothing reaches, a class the translator kept and nothing
 * calls has no symbol either. What is read here is the binary, which is the thing a server
 * pays for; the floor on the number of classes below is what keeps a stripped binary from
 * passing by holding nothing.
 */
class BackendLinkingTest {
    private static final String SECURITY = "com_codename1_backend_security_";

    /** What only a chain that checks passwords needs. */
    private static final String[] PASSWORDS = {"crypto_BCryptPasswordEncoder",
        "crypto_Pbkdf2PasswordEncoder", "crypto_Pbkdf2Sha256PasswordEncoder",
        "crypto_DelegatingPasswordEncoder", "crypto_NoOpPasswordEncoder",
        "crypto_PasswordEncoderFactories", "DaoAuthenticationProvider", "PasswordAuthentication",
        "core_userdetails_InMemoryUserDetailsManager"};

    /** The form login, and what comes with it. */
    private static final String[] FORM = {"FormLoginConfigurer", "UsernamePasswordAuthenticationFilter",
        "DefaultLoginPageGeneratingFilter", "LoginUrlAuthenticationEntryPoint", "LogoutConfigurer",
        "LogoutFilter", "RequestCacheConfigurer", "HttpSessionRequestCache",
        "SavedRequestAwareAuthenticationSuccessHandler", "SessionSignIn"};

    /** Token verification. */
    private static final String[] TOKENS = {"OAuth2ResourceServerConfigurer",
        "BearerTokenAuthenticationFilter", "oauth2_jwt_DefaultJwtDecoder", "oauth2_jwt_RemoteJwkSet",
        "oauth2_jwt_Jwt", "oauth2_jwt_Jose", "oauth2_server_resource_JwtAuthenticationProvider",
        "crypto_Jwk", "crypto_JwkSet", "crypto_Der", "crypto_KeyFiles"};

    /** What none of the three declares. */
    private static final String[] NEVER = {"HttpBasicConfigurer", "BasicAuthenticationFilter",
        "ApiKeyConfigurer", "ApiKeyAuthenticationFilter", "apikey_ApiKey",
        "apikey_ApiKeyAuthenticationToken", "apikey_InMemoryApiKeyRepository",
        "RateLimitConfigurer", "RateLimitFilter", "ratelimit_InMemoryRateLimiter",
        "ratelimit_RateLimitKeys", "ratelimit_JdbcRateLimiter", "RememberMeConfigurer",
        "RememberMeAuthenticationFilter", "rememberme_PersistentTokenBasedRememberMeServices",
        "rememberme_InMemoryTokenRepositoryImpl", "rememberme_JdbcTokenRepository",
        "MfaConfigurer", "SecondFactorAuthenticationFilter", "mfa_TotpService",
        "mfa_RecoveryCodeService", "mfa_JdbcTotpRepository", "SecuritySchema",
        "core_userdetails_JdbcUserDetailsManager", "apikey_JdbcApiKeyRepository"};

    /** Sign-in through another provider. */
    private static final String[] OAUTH_LOGIN = {"OAuth2LoginConfigurer",
        "OAuth2LoginAuthenticationFilter", "OAuth2AuthorizationRequestRedirectFilter",
        "oauth2_client_ClientRegistration", "oauth2_client_InMemoryClientRegistrationRepository",
        "oauth2_client_DefaultOAuth2AuthorizationRequestResolver",
        "oauth2_client_DefaultAuthorizationCodeTokenResponseClient",
        "oauth2_client_OidcIdTokenDecoderFactory", "oauth2_client_OidcUserService",
        "oauth2_client_OAuth2AuthorizationRequest"};
    /** The authorization server. */
    private static final String[] AUTH_SERVER = {"AuthorizationServerConfigurer",
        "OAuth2AuthorizationServerFilter", "OAuth2AuthorizationEndpointFilter",
        "oauth2_server_authorization_OAuth2AuthorizationServer",
        "oauth2_server_authorization_RegisteredClient",
        "oauth2_server_authorization_InMemoryRegisteredClientRepository",
        "oauth2_server_authorization_InMemoryOAuth2AuthorizationService",
        "oauth2_server_authorization_AuthorizationServerKeys",
        "oauth2_server_authorization_AuthorizationServerSettings",
        "oauth2_server_authorization_TokenSettings", "oauth2_jwt_DefaultJwtEncoder"};
    /** What keeps a table, or ties a provider's user to a local one: an application's to name. */
    private static final String[] OAUTH_OPTIONAL = {
        "oauth2_client_JdbcFederatedIdentityRepository",
        "oauth2_client_InMemoryFederatedIdentityRepository",
        "oauth2_client_LinkingOAuth2UserService",
        "oauth2_server_authorization_JdbcOAuth2AuthorizationService",
        "oauth2_server_authorization_JdbcRegisteredClientRepository"};
    /** The form login alone, without the sign-out and the saved request any session sign-in has. */
    private static final String[] FORM_ONLY = {"FormLoginConfigurer",
        "UsernamePasswordAuthenticationFilter", "DefaultLoginPageGeneratingFilter",
        "LoginUrlAuthenticationEntryPoint"};
    /** Passkeys: the configurer, its filters, and what reads and verifies a ceremony. */
    private static final String[] WEBAUTHN = {"WebAuthnConfigurer", "WebAuthnAuthenticationFilter",
        "WebAuthnRegistrationFilter", "WebAuthnAuthenticationCodec", "webauthn_Cbor",
        "webauthn_CoseKey", "webauthn_AuthenticatorData",
        "webauthn_WebAuthnRelyingPartyOperations", "webauthn_WebAuthnAuthentication",
        "webauthn_CredentialRecord", "webauthn_InMemoryUserCredentialRepository",
        "webauthn_InMemoryPublicKeyCredentialUserEntityRepository"};
    /** Every class of passkeys, by prefix: what a server without them must not hold. */
    private static final String[] NO_WEBAUTHN = {"WebAuthnConfigurer",
        "WebAuthnAuthenticationFilter", "WebAuthnRegistrationFilter",
        "WebAuthnAuthenticationCodec", "webauthn_"};
    /** The natives only a server that signs, verifies or encrypts calls. */
    private static final String[] SIGNING_NATIVES = {"Crypto_signImpl", "Crypto_verifyImpl",
        "Crypto_aesGcmImpl", "Crypto_generateRsaKeyImpl"};

    @Test
    @DisplayName("a server links the security it declares and no other")
    void eachServerCarriesOnlyWhatItDeclares() throws Exception {
        if (CompilerHelper.isWindows()) {
            Assumptions.abort("the server-side backend is POSIX-only for now");
        }
        BackendTestSupport.require(Files.isDirectory(BackendTestSupport.backendDir()),
                "vm/backend is not present");
        BackendTestSupport.require(BackendTestSupport.hasCommand("nm"), "nm is not available");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to build the backend");
        Path work = Files.createTempDirectory("backend-linkcheck");

        Set<String> none = build(work, "LinkNone", "demo/linknone", jdk8,
                "LINKCHECK none open=200");
        Set<String> jwt = build(work, "LinkJwt", "demo/linkjwt", jdk8,
                "LINKCHECK jwt open=200 anonymous=401 accepted=200 denied=403 forged=401 post=200");
        Set<String> form = build(work, "LinkForm", "demo/linkform", jdk8,
                "LINKCHECK form open=200 redirect=302 to=/login page=200 refused=/login?error "
                        + "signedIn=/ home=200 logout=302 after=302");
        Set<String> login = build(work, "LinkOAuthLogin", "demo/linkoauthlogin", jdk8,
                "LINKCHECK oauthlogin open=200 redirect=302 to=/oauth2/authorization/acme "
                        + "asked=true refused=/login?error page=200 home=302");
        Set<String> issuer = build(work, "LinkAuthServer", "demo/linkauthserver", jdk8,
                "LINKCHECK authserver open=200 metadata=200 jwks=200 token=200 signed=true "
                        + "wrong=401 device=200 authorize=401 elsewhere=403");
        Set<String> passkeys = build(work, "LinkWebAuthn", "demo/linkwebauthn", jdk8,
                "LINKCHECK webauthn open=200 options=200 rp=true refused=401 register=403 "
                        + "home=403 registered=true verified=ada forged=signature_invalid");

        // No chain, no layer: not one class of it, the internals included.
        List<String> stray = new ArrayList<String>();
        for (String cls : none) {
            if (cls.startsWith(SECURITY) || cls.startsWith("com_codename1_impl_backend_security_")) {
                stray.add(cls);
            }
        }
        assertTrue(stray.isEmpty(), "a server with no chain links the security layer: " + stray);

        // The instrument itself: each server does hold what it declared, so an
        // absence below is the linker's doing and not a symbol that was renamed.
        present("LinkJwt", jwt, TOKENS);
        present("LinkJwt", jwt, "HttpSecurity", "FilterChainEngine", "AuthorizationFilter");
        present("LinkForm", form, FORM);
        present("LinkForm", form, PASSWORDS);
        present("LinkForm", form, "HttpSecurity", "FilterChainEngine", "CsrfFilter");

        absent("LinkJwt", jwt, PASSWORDS);
        absent("LinkJwt", jwt, FORM);
        absent("LinkJwt", jwt, NEVER);
        absent("LinkForm", form, TOKENS);
        absent("LinkForm", form, NEVER);
        absent("LinkForm", form, "oauth2_");
        absent("LinkJwt", jwt, OAUTH_LOGIN);
        absent("LinkJwt", jwt, "oauth2_client_", "oauth2_server_authorization_");
        absent("LinkJwt", jwt, "AuthorizationServerConfigurer", "OAuth2AuthorizationServerFilter",
                "OAuth2AuthorizationEndpointFilter");
        absent("LinkForm", form, OAUTH_LOGIN);
        absent("LinkForm", form, AUTH_SERVER);

        // A sign-in through another provider: that, the session it ends in, and
        // the verification of the ID token -- and no authorization server, no
        // form, no passwords, nothing that keeps a table.
        present("LinkOAuthLogin", login, OAUTH_LOGIN);
        present("LinkOAuthLogin", login, "SessionSignIn", "LogoutFilter", "HttpSessionRequestCache",
                "oauth2_jwt_DefaultJwtDecoder", "oauth2_jwt_RemoteJwkSet", "crypto_Jwk");
        absent("LinkOAuthLogin", login, "oauth2_server_");
        absent("LinkOAuthLogin", login, "AuthorizationServerConfigurer",
                "OAuth2AuthorizationServerFilter", "OAuth2AuthorizationEndpointFilter",
                "OAuth2ResourceServerConfigurer", "BearerTokenAuthenticationFilter");
        absent("LinkOAuthLogin", login, FORM_ONLY);
        absent("LinkOAuthLogin", login, PASSWORDS);
        absent("LinkOAuthLogin", login, NEVER);
        absent("LinkOAuthLogin", login, OAUTH_OPTIONAL);
        // It verifies ID tokens and signs nothing: not even Apple's client
        // secret, which only a server that signs in with Apple makes.
        absent("LinkOAuthLogin", login, "oauth2_client_AppleClientSecret",
                "oauth2_jwt_DefaultJwtEncoder");

        // An authorization server: that, and what signs -- and no sign-in of any
        // kind, since this one declares none, no passwords, nothing that keeps a
        // table.
        present("LinkAuthServer", issuer, AUTH_SERVER);
        present("LinkAuthServer", issuer, "crypto_Jwk", "oauth2_jwt_DefaultJwtDecoder");
        absent("LinkAuthServer", issuer, "oauth2_client_", "oauth2_server_resource_");
        absent("LinkAuthServer", issuer, OAUTH_LOGIN);
        absent("LinkAuthServer", issuer, "OAuth2ResourceServerConfigurer",
                "BearerTokenAuthenticationFilter", "oauth2_jwt_RemoteJwkSet");
        absent("LinkAuthServer", issuer, FORM);
        absent("LinkAuthServer", issuer, PASSWORDS);
        absent("LinkAuthServer", issuer, NEVER);
        absent("LinkAuthServer", issuer, OAUTH_OPTIONAL);

        // Passkeys: the ceremonies, the session they end in, and the CBOR and
        // DER that read a credential's key -- and no form, no passwords, no
        // tokens, no second factor, nothing that keeps a table. The fixture
        // verifies a registration and a sign-in made elsewhere, so what is
        // present here ran, translated.
        present("LinkWebAuthn", passkeys, WEBAUTHN);
        present("LinkWebAuthn", passkeys, "SessionSignIn", "LogoutFilter",
                "HttpSessionRequestCache", "crypto_Der", "HttpSecurity", "AuthorizationFilter");
        absent("LinkWebAuthn", passkeys, FORM_ONLY);
        absent("LinkWebAuthn", passkeys, PASSWORDS);
        absent("LinkWebAuthn", passkeys, NEVER);
        absent("LinkWebAuthn", passkeys, OAUTH_LOGIN);
        absent("LinkWebAuthn", passkeys, AUTH_SERVER);
        absent("LinkWebAuthn", passkeys, OAUTH_OPTIONAL);
        absent("LinkWebAuthn", passkeys, "oauth2_", "OAuth2ResourceServerConfigurer",
                "BearerTokenAuthenticationFilter", "OAuth2AuthenticationCodec", "crypto_Jwk",
                "crypto_JwkSet", "crypto_KeyFiles", "crypto_SignedTokens",
                "webauthn_JdbcUserCredentialRepository",
                "webauthn_JdbcPublicKeyCredentialUserEntityRepository");
        // And nobody else holds a class of it, nor the codec of a sign-in
        // through another provider where there is none.
        absent("LinkJwt", jwt, NO_WEBAUTHN);
        absent("LinkForm", form, NO_WEBAUTHN);
        absent("LinkOAuthLogin", login, NO_WEBAUTHN);
        absent("LinkAuthServer", issuer, NO_WEBAUTHN);
        present("LinkOAuthLogin", login, "OAuth2AuthenticationCodec");
        absent("LinkJwt", jwt, "OAuth2AuthenticationCodec");
        absent("LinkForm", form, "OAuth2AuthenticationCodec");
        absent("LinkAuthServer", issuer, "OAuth2AuthenticationCodec");

        // The natives, which only the linker can leave out. The server that
        // signs holds the signature native -- so its absence elsewhere is the
        // linker's doing and not a symbol that was renamed.
        String signs = NATIVES.get("LinkAuthServer");
        assertTrue(signs.contains("com_codename1_backend_Crypto_signImpl")
                && signs.contains("com_codename1_backend_Crypto_generateRsaKeyImpl"),
                "LinkAuthServer signs tokens with a key it makes, and its binary holds no "
                        + "native for it");
        assertTrue(NATIVES.get("LinkNone").contains("com_codename1_backend_Crypto_sha256Impl"),
                "LinkNone hashes, and its binary holds no native for it");
        for (String symbol : SIGNING_NATIVES) {
            for (String main : new String[] {"LinkNone", "LinkForm"}) {
                assertTrue(!NATIVES.get(main).contains("com_codename1_backend_" + symbol),
                        main + " never signs, verifies or encrypts, and its binary holds the "
                                + "native " + symbol + ": the link no longer drops what "
                                + "nothing calls; see the dead-strip flags in "
                                + "vm/backend/build.sh");
            }
        }
        assertTrue(NATIVES.get("LinkOAuthLogin").contains("com_codename1_backend_Crypto_verifyImpl"),
                "LinkOAuthLogin verifies ID tokens, and its binary holds no native for it");
        assertTrue(NATIVES.get("LinkWebAuthn").contains("com_codename1_backend_Crypto_verifyImpl"),
                "LinkWebAuthn verifies assertions, and its binary holds no native for it");
        for (String symbol : new String[] {"Crypto_signImpl", "Crypto_generateRsaKeyImpl",
            "Crypto_aesGcmImpl"}) {
            assertTrue(!NATIVES.get("LinkWebAuthn").contains("com_codename1_backend_" + symbol),
                    "LinkWebAuthn signs nothing, makes no keys and encrypts nothing, and its "
                            + "binary holds the native " + symbol);
        }
        for (String symbol : new String[] {"Crypto_signImpl", "Crypto_generateRsaKeyImpl",
            "Crypto_aesGcmImpl"}) {
            assertTrue(!NATIVES.get("LinkOAuthLogin").contains("com_codename1_backend_" + symbol),
                    "LinkOAuthLogin signs nothing and makes no keys, and its binary holds the "
                            + "native " + symbol);
        }
    }

    /** The native symbols of the Crypto class in each binary built, by main class. */
    private static final java.util.Map<String, String> NATIVES =
            new java.util.HashMap<String, String>();

    /** Builds and runs one server; answers the classes its binary holds. */
    private static Set<String> build(Path work, String main, String demo, Path jdk8,
                                     String expected) throws Exception {
        Path binary = work.resolve(main);
        String failure = BackendTestSupport.build(main, demo, binary, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }
        Process p = new ProcessBuilder(binary.toString()).redirectErrorStream(true).start();
        boolean[] timedOut = new boolean[1];
        String output = BackendTestSupport.awaitOutput(p, 2, TimeUnit.MINUTES, timedOut);
        if (timedOut[0]) {
            fail(main + " did not finish:\n" + output);
        }
        assertTrue(output.contains(expected) && output.contains("LINKCHECK OK"),
                main + " does not serve what it declares:\n" + output);
        String symbols = BackendTestSupport.run(Arrays.asList("nm", binary.toString()), 120);
        assertTrue(symbols != null, "nm could not read " + binary);
        Set<String> classes = new TreeSet<String>();
        String marker = "STATIC_INITIALIZER_";
        StringBuilder natives = new StringBuilder();
        for (String line : symbols.split("\n")) {
            if (line.contains("com_codename1_backend_Crypto_") && line.contains("Impl___")) {
                natives.append(line).append('\n');
            }
            int at = line.indexOf(marker);
            if (at >= 0) {
                classes.add(line.substring(at + marker.length()).trim());
            }
        }
        // A floor, so a binary whose symbols were stripped cannot pass by
        // holding nothing at all.
        assertTrue(classes.size() > 300 && classes.contains("com_codename1_backend_Backend"),
                "could not read the classes of " + main + " from its symbols: " + classes.size());
        NATIVES.put(main, natives.toString());
        // For the log: what each kind of security costs a binary.
        System.out.println("LINKSIZE " + main + " " + Files.size(binary) + " bytes, "
                + classes.size() + " classes");
        return classes;
    }

    private static void present(String main, Set<String> classes, String... names) {
        for (String name : names) {
            assertTrue(classes.contains(SECURITY + name), main + " declares what needs "
                    + name.replace('_', '.') + ", and its binary does not hold it");
        }
    }

    /** Requires no class whose name is `name`, or starts with it when it ends in `_`. */
    private static void absent(String main, Set<String> classes, String... names) {
        List<String> found = new ArrayList<String>();
        for (String name : names) {
            for (String cls : classes) {
                if (cls.equals(SECURITY + name) || (name.endsWith("_")
                        && cls.startsWith(SECURITY + name))) {
                    found.add(cls);
                }
            }
        }
        assertTrue(found.isEmpty(), main + " links what it never declared: " + found
                + ". Something on a path every chain takes names it; see HttpSecurity for how "
                + "the optional parts are kept reachable only from their own DSL methods.");
    }
}
