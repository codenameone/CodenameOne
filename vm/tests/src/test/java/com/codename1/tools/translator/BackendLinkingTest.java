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
 * So this builds three whole servers, vm/backend/demo/linknone, linkjwt and linkform -- no
 * security, a JWT resource server only, a form login only -- runs each to prove it works, and
 * reads the symbols of each binary. A class that is linked has a static initializer symbol;
 * the classes of what a server did not declare must have none.
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
        "SavedRequestAwareAuthenticationSuccessHandler"};

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
    }

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
        for (String line : symbols.split("\n")) {
            int at = line.indexOf(marker);
            if (at >= 0) {
                classes.add(line.substring(at + marker.length()).trim());
            }
        }
        // A floor, so a binary whose symbols were stripped cannot pass by
        // holding nothing at all.
        assertTrue(classes.size() > 300 && classes.contains("com_codename1_backend_Backend"),
                "could not read the classes of " + main + " from its symbols: " + classes.size());
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
