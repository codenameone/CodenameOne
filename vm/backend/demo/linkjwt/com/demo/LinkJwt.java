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
package com.demo;

import com.codename1.backend.Backend;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SessionCreationPolicy;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.WiringEnvironment;
import com.codename1.impl.backend.security.SecuritySupport;

/// A server that only verifies bearer tokens: one stateless chain with
/// `oauth2ResourceServer`. Its binary must hold no login form, no user store,
/// no password hashing, no API keys, no rate limiter and no sign-out.
public final class LinkJwt {
    private LinkJwt() {
    }

    public static void main(String[] args) throws Exception {
        final byte[] secret = "link-check-secret-of-32-bytes-ok".getBytes("UTF-8");
        Backend.Builder builder = Backend.builder().quiet().host("127.0.0.1").port(0);
        BackendAccess.get().application(builder, new LinkApp() {
            @Override
            void chains(WiringEnvironment environment) {
                HttpSecurity http = SecuritySupport.http(environment.getConfig(), new Object[0]);
                http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/open").permitAll()
                            .anyRequest().hasAuthority("SCOPE_orders:read"))
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                            .decoder(DefaultJwtDecoder.withSecretKey(secret).build())));
                environment.registerSecurityFilterChain(http.build(), 1);
            }
        });
        BackendAccess.get().security(builder);
        Backend backend = builder.start();
        long now = System.currentTimeMillis() / 1000L;
        DefaultJwtEncoder encoder = new DefaultJwtEncoder(JwkSet.of(Jwk.ofSecret(secret)));
        String good = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .subject("ada").issuedAt(now).expiresAt(now + 300)
                .claim("scope", "orders:read").build())).getTokenValue();
        String narrow = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .subject("ada").issuedAt(now).expiresAt(now + 300)
                .claim("scope", "other").build())).getTokenValue();
        int open = LinkApp.status(backend, "GET", "/open");
        int anonymous = LinkApp.status(backend, "GET", "/orders");
        int accepted = LinkApp.status(backend, "GET", "/orders", "Authorization", "Bearer " + good);
        int denied = LinkApp.status(backend, "GET", "/orders", "Authorization", "Bearer " + narrow);
        int forged = LinkApp.status(backend, "GET", "/orders", "Authorization",
                "Bearer " + good.substring(0, good.length() - 2) + "xx");
        // No session, so nothing for a forged request to ride on: a POST with a
        // token needs no CSRF token.
        int post = LinkApp.status(backend, "POST", "/orders", "Authorization", "Bearer " + good);
        backend.stop();
        System.out.println("LINKCHECK jwt open=" + open + " anonymous=" + anonymous + " accepted="
                + accepted + " denied=" + denied + " forged=" + forged + " post=" + post);
        System.out.println(open == 200 && anonymous == 401 && accepted == 200 && denied == 403
                && forged == 401 && post == 200 ? "LINKCHECK OK" : "LINKCHECK FAILED");
    }
}
