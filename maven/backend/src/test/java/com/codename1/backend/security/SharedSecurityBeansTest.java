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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import java.util.Collections;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class SharedSecurityBeansTest {
    private HttpSecurity http(Object[] beans, boolean... primary) {
        Properties settings = new Properties();
        settings.setProperty(HttpSecurity.USER_PASSWORD, "fallback");
        return new HttpSecurity(Config.of(settings, "dev"), beans,
                new String[] {"first", "second"}, primary);
    }

    private UserDetailsService users(String name) {
        return new InMemoryUserDetailsManager(User.withUsername(name).password("encoded")
                .roles("USER").build());
    }

    private void ambiguous(Runnable action, String type) {
        String message = assertThrows(IllegalStateException.class, action::run).getMessage();
        assertTrue(message.contains(type), message);
        assertTrue(message.contains("@Primary"), message);
        assertTrue(message.contains("first") && message.contains("second"), message);
    }

    @Test
    void userStoresCannotSilentlyBecomeAConfiguredUser() {
        UserDetailsService first = users("first");
        UserDetailsService second = users("second");
        Object[] beans = {first, second};
        ambiguous(() -> PasswordAuthentication.users(http(beans)), "UserDetailsService");
        ambiguous(() -> PasswordAuthentication.require(http(beans), "Login"), "UserDetailsService");
        assertSame(second, PasswordAuthentication.users(http(beans, false, true)));
        HttpSecurity explicit = http(beans).userDetailsService(first);
        assertSame(first, PasswordAuthentication.users(explicit));
    }

    private static final class Encoder implements PasswordEncoder {
        int checks;

        @Override
        public String encode(CharSequence password) {
            return "encoded";
        }

        @Override
        public boolean matches(CharSequence password, String encoded) {
            checks++;
            return "secret".contentEquals(password) && "encoded".equals(encoded);
        }
    }

    @Test
    void passwordEncoderSelectionUsesPrimaryAndExplicitChoice() {
        Encoder first = new Encoder();
        Encoder second = new Encoder();
        Object[] beans = {first, second};
        ambiguous(() -> PasswordAuthentication.require(http(beans).userDetailsService(users("ada")),
                "Login"), "PasswordEncoder");
        HttpSecurity primary = http(beans, false, true).userDetailsService(users("ada"));
        assertEquals("ada", PasswordAuthentication.require(primary, "Login").authenticate(
                new UsernamePasswordAuthenticationToken("ada", "secret")).getName());
        assertEquals(0, first.checks);
        assertTrue(second.checks > 0);
        HttpSecurity explicit = http(beans).userDetailsService(users("ada"));
        explicit.setSharedObject(PasswordEncoder.class, first);
        assertTrue(PasswordAuthentication.require(explicit, "Login").authenticate(
                new UsernamePasswordAuthenticationToken("ada", "secret")).isAuthenticated());
        assertTrue(first.checks > 0);
    }

    @Test
    void jwtDecoderSelectionUsesPrimaryAndExplicitChoice() {
        JwtDecoder first = token -> new Jwt(token, Collections.singletonMap("alg", "RS256"),
                Collections.singletonMap("sub", "first"));
        JwtDecoder second = token -> new Jwt(token, Collections.singletonMap("alg", "RS256"),
                Collections.singletonMap("sub", "second"));
        Object[] beans = {first, second};
        ambiguous(() -> new OAuth2ResourceServerConfigurer.JwtConfigurer().manager(http(beans)),
                "JwtDecoder");
        assertEquals("second", new OAuth2ResourceServerConfigurer.JwtConfigurer()
                .manager(http(beans, false, true)).authenticate(
                        new BearerTokenAuthenticationToken("token")).getName());
        assertEquals("first", new OAuth2ResourceServerConfigurer.JwtConfigurer().decoder(first)
                .manager(http(beans)).authenticate(new BearerTokenAuthenticationToken("token")).getName());
    }

    @Test
    void webAuthnRejectsAmbiguousUsersAndHonorsPrimary() {
        Object[] beans = {users("first"), users("second")};
        ambiguous(() -> http(beans).webAuthn(w -> w.rpId("example.com")
                .allowedOrigins("https://example.com")).build(), "UserDetailsService");
        http(beans, false, true).webAuthn(w -> w.rpId("example.com")
                .allowedOrigins("https://example.com")).build();
    }

    @Test
    void authorizationServerRejectsAmbiguousSecretEncodersAndHonorsPrimary() {
        Object[] beans = {new Encoder(), new Encoder()};
        ambiguous(() -> http(beans).authorizationServer(a -> a
                .registeredClientRepository(new InMemoryRegisteredClientRepository())
                .jwkSource(Collections::emptyList)).build(), "PasswordEncoder");
        http(beans, false, true).authorizationServer(a -> a
                .registeredClientRepository(new InMemoryRegisteredClientRepository())
                .jwkSource(Collections::emptyList)).build();
    }
}
