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
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.oauth2.client.ClientRegistration;
import com.codename1.backend.security.oauth2.client.InMemoryClientRegistrationRepository;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.WiringEnvironment;
import com.codename1.impl.backend.security.SecuritySupport;

/// A server people sign in to through another provider: one chain with
/// `oauth2Login` and one OpenID Connect registration. Its binary must hold no
/// authorization server, no login form, no password hashing, no user store,
/// no API keys, no rate limiter, no second factor and nothing that keeps a
/// table.
public final class LinkOAuthLogin {
    private LinkOAuthLogin() {
    }

    public static void main(String[] args) throws Exception {
        final ClientRegistration provider = ClientRegistration.withRegistrationId("acme")
                .clientId("link-check").clientSecret("link-check-secret")
                .issuerUri("https://id.example.com")
                .authorizationUri("https://id.example.com/oauth2/authorize")
                .tokenUri("https://id.example.com/oauth2/token")
                .jwkSetUri("https://id.example.com/oauth2/jwks")
                .redirectUri("https://app.example.com/login/oauth2/code/{registrationId}")
                .scope("openid", "profile").build();
        Backend.Builder builder = Backend.builder().quiet().host("127.0.0.1").port(0);
        BackendAccess.get().application(builder, new LinkApp() {
            @Override
            void chains(WiringEnvironment environment) {
                HttpSecurity http = SecuritySupport.http(environment.getConfig(), new Object[0]);
                http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/open").permitAll()
                            .anyRequest().authenticated())
                    .oauth2Login(oauth2 -> oauth2.clientRegistrationRepository(
                            new InMemoryClientRegistrationRepository(provider)));
                environment.registerSecurityFilterChain(http.build(), 1);
            }
        });
        BackendAccess.get().security(builder);
        Backend backend = builder.start();
        int open = LinkApp.status(backend, "GET", "/open");
        HttpServer.Response anonymous = LinkApp.send(backend, "GET", "/home", null,
                "Accept", "text/html");
        int redirect = BackendAccess.get().status(anonymous);
        String location = LinkApp.header(anonymous, "Location");
        // The trip to the provider: a state, a nonce and a PKCE challenge.
        HttpServer.Response start = LinkApp.send(backend, "GET", "/oauth2/authorization/acme",
                null);
        String sentTo = LinkApp.header(start, "Location");
        boolean asked = sentTo != null
                && sentTo.startsWith("https://id.example.com/oauth2/authorize?response_type=code")
                && sentTo.indexOf("&state=") > 0 && sentTo.indexOf("&nonce=") > 0
                && sentTo.indexOf("&code_challenge_method=S256") > 0;
        String cookie = LinkApp.header(start, "Set-Cookie");
        if (cookie != null && cookie.indexOf(';') > 0) {
            cookie = cookie.substring(0, cookie.indexOf(';'));
        }
        // An answer with another state than the one this browser was given.
        HttpServer.Response forged = cookie == null ? null : LinkApp.send(backend, "GET",
                "/login/oauth2/code/acme?code=abc&state=not-the-state", null, "Cookie", cookie);
        String refused = forged == null ? null : LinkApp.header(forged, "Location");
        int page = LinkApp.status(backend, "GET", "/login");
        int home = cookie == null ? -1 : LinkApp.status(backend, "GET", "/home", "Cookie", cookie,
                "Accept", "text/html");
        backend.stop();
        System.out.println("LINKCHECK oauthlogin open=" + open + " redirect=" + redirect + " to="
                + location + " asked=" + asked + " refused=" + refused + " page=" + page
                + " home=" + home);
        System.out.println(open == 200 && redirect == 302
                && "/oauth2/authorization/acme".equals(location) && asked
                && "/login?error".equals(refused) && page == 200 && home == 302
                ? "LINKCHECK OK" : "LINKCHECK FAILED");
    }
}
