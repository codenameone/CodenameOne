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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.Json;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

/// What the OAuth2 tests share: a browser's and a client's small motions.
final class OAuth2Testing {
    private OAuth2Testing() {
    }

    /// Stores a secret as `plain:` and itself, so a test reads what it stored.
    static final PasswordEncoder PLAIN = new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return "plain:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword.equals("plain:" + rawPassword);
        }
    };

    static final class Ticking implements Clock {
        long now = System.currentTimeMillis();

        @Override
        public long currentTimeMillis() {
            return now;
        }
    }

    /// A port nothing listens on, so a server can be told its own address
    /// before it starts.
    static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /// The parameters in the query of an address.
    static Map<String, String> query(String location) {
        int q = location.indexOf('?');
        return OAuth2Parameters.parse(q < 0 ? "" : location.substring(q + 1));
    }

    static Map json(Reply reply) throws IOException {
        return Json.parseObject(reply.body);
    }

    static String form(String... pairs) {
        StringBuilder sb = new StringBuilder();
        for (int iter = 0 ; iter + 1 < pairs.length ; iter += 2) {
            if (pairs[iter + 1] == null) {
                continue;
            }
            sb.append(sb.length() == 0 ? "" : "&").append(OAuth2Parameters.encode(pairs[iter]))
                    .append('=').append(OAuth2Parameters.encode(pairs[iter + 1]));
        }
        return sb.toString();
    }

    static String basic(String id, String secret) throws IOException {
        return "Basic " + Base64.encode((id + ":" + secret).getBytes("UTF-8"));
    }

    static String field(String html, String name) {
        String marker = "name=\"" + name + "\" value=\"";
        int at = html.indexOf(marker);
        assertTrue(at >= 0, "no " + name + " field in " + html);
        return html.substring(at + marker.length(), html.indexOf('"', at + marker.length()));
    }

    /// Signs in through the generated login form; answers the reply to the
    /// post, which redirects to where the user was going.
    static Reply signIn(SecuredServer server, String username, String password) throws Exception {
        Reply page = server.get("/login");
        assertEquals(200, page.status, page.toString());
        return server.post("/login", form("username", username, "password", password, "_csrf",
                field(page.body, "_csrf")));
    }

    /// The token, verified against the keys a server publishes.
    static Jwt verify(String token, String jwks) throws IOException {
        final JwkSet set = JwkSet.parse(jwks);
        return DefaultJwtDecoder.withJwkSource(set::getKeys).jwsAlgorithms(
                new JwsAlgorithm[] {SignatureAlgorithm.RS256, SignatureAlgorithm.ES256}).build()
                .decode(token);
    }
}
