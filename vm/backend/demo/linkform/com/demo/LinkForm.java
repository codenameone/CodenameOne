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
import com.codename1.backend.security.Customizer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.WiringEnvironment;
import com.codename1.impl.backend.security.SecuritySupport;

/// A server people sign in to through a form: one chain with `formLogin` and
/// an in-memory user store. Its binary must hold no token verification -- no
/// JWT decoder, no key set, no signature check -- no API keys and no rate
/// limiter.
public final class LinkForm {
    private LinkForm() {
    }

    public static void main(String[] args) throws Exception {
        final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        final InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password(encoder.encode("ada-pw")).roles("USER").build());
        Backend.Builder builder = Backend.builder().quiet().host("127.0.0.1").port(0);
        BackendAccess.get().application(builder, new LinkApp() {
            @Override
            void chains(WiringEnvironment environment) {
                HttpSecurity http = SecuritySupport.http(environment.getConfig(),
                        new Object[] {users, encoder});
                http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/open").permitAll()
                            .anyRequest().authenticated())
                    .csrf(csrf -> csrf.disable())
                    .formLogin(Customizer.withDefaults());
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
        int page = LinkApp.status(backend, "GET", "/login");
        HttpServer.Response refused = LinkApp.send(backend, "POST", "/login",
                "username=ada&password=wrong");
        HttpServer.Response signedIn = LinkApp.send(backend, "POST", "/login",
                "username=ada&password=ada-pw");
        String cookie = LinkApp.header(signedIn, "Set-Cookie");
        if (cookie != null && cookie.indexOf(';') > 0) {
            cookie = cookie.substring(0, cookie.indexOf(';'));
        }
        int home = cookie == null ? -1 : LinkApp.status(backend, "GET", "/home", "Cookie", cookie);
        // Sign-out came with the form login.
        int logout = cookie == null ? -1 : LinkApp.status(backend, "POST", "/logout",
                "Cookie", cookie);
        int after = cookie == null ? -1 : LinkApp.status(backend, "GET", "/home", "Cookie", cookie,
                "Accept", "text/html");
        backend.stop();
        System.out.println("LINKCHECK form open=" + open + " redirect=" + redirect + " to="
                + location + " page=" + page + " refused=" + LinkApp.header(refused, "Location")
                + " signedIn=" + LinkApp.header(signedIn, "Location") + " home=" + home
                + " logout=" + logout + " after=" + after);
        System.out.println(open == 200 && redirect == 302 && "/login".equals(location)
                && page == 200 && "/login?error".equals(LinkApp.header(refused, "Location"))
                && "/".equals(LinkApp.header(signedIn, "Location")) && home == 200
                && logout == 302 && after == 302 ? "LINKCHECK OK" : "LINKCHECK FAILED");
    }
}
