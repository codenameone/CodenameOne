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
package com.codename1.backend.security.oauth2.server.resource;

import com.codename1.backend.Base64Url;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.AuthenticationManager;
import com.codename1.backend.security.AuthenticationManagerResolver;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.ProviderManager;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtDecoders;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/// For a server that takes tokens from several issuers: each token is verified
/// by the issuer it says it is from, and only by that one.
///
/// ```java
/// http.oauth2ResourceServer(o -> o.authenticationManagerResolver(
///         JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(
///                 "https://login.example.com", "https://partners.example.com")));
/// ```
///
/// The `iss` claim is read out of the token before anything has verified it,
/// and used for one thing: choosing which trusted issuer's keys to verify
/// with. A token that names an issuer not on the list is refused without a
/// request being made anywhere -- the list is what stops a token from pointing
/// the server at keys of its author's choosing. A token that names a trusted
/// issuer it did not come from fails that issuer's signature check.
///
/// An issuer's metadata is fetched when its first token arrives, and kept.
public final class JwtIssuerAuthenticationManagerResolver implements AuthenticationManagerResolver {
    private final Map<String, AuthenticationManager> managers =
            new HashMap<String, AuthenticationManager>();
    /// Issuers whose manager is made from their metadata on first use.
    private final Map<String, Boolean> discovered = new HashMap<String, Boolean>();
    private final AuthenticationManager routing = new Routing();

    /// @param managers what authenticates the tokens of each issuer, by the
    /// issuer's `iss`
    public JwtIssuerAuthenticationManagerResolver(Map<String, AuthenticationManager> managers) {
        if (managers == null || managers.isEmpty()) {
            throw new IllegalArgumentException("At least one trusted issuer is required");
        }
        this.managers.putAll(managers);
    }

    private JwtIssuerAuthenticationManagerResolver(String[] issuers) {
        if (issuers == null || issuers.length == 0) {
            throw new IllegalArgumentException("At least one trusted issuer is required");
        }
        for (String issuer : issuers) {
            if (issuer == null || issuer.length() == 0) {
                throw new IllegalArgumentException("An issuer cannot be empty");
            }
            discovered.put(issuer, Boolean.TRUE);
        }
    }

    /// Trusts these issuers, each verified with the keys its own metadata
    /// names; see [JwtDecoders#fromIssuerLocation].
    public static JwtIssuerAuthenticationManagerResolver fromTrustedIssuers(String... issuers) {
        return new JwtIssuerAuthenticationManagerResolver(issuers);
    }

    /// Trusts these issuers, each verified by the decoder given for it.
    public static JwtIssuerAuthenticationManagerResolver fromDecoders(
            Map<String, JwtDecoder> decoders) {
        Map<String, AuthenticationManager> managers =
                new LinkedHashMap<String, AuthenticationManager>();
        for (Map.Entry<String, JwtDecoder> entry : decoders.entrySet()) {
            managers.put(entry.getKey(),
                    new ProviderManager(new JwtAuthenticationProvider(entry.getValue())));
        }
        return new JwtIssuerAuthenticationManagerResolver(managers);
    }

    /// One manager for every request: it routes each token by its issuer.
    @Override
    public AuthenticationManager resolve(HttpServer.Request request) {
        return routing;
    }

    private AuthenticationManager managerFor(String issuer) {
        boolean discover;
        synchronized (this) {
            AuthenticationManager known = managers.get(issuer);
            if (known != null) {
                return known;
            }
            discover = discovered.containsKey(issuer);
        }
        if (!discover) {
            return null;
        }
        // Outside the lock: this is a request to the issuer. Two first tokens
        // at once each ask, and the second answer replaces the first.
        JwtDecoder decoder;
        try {
            decoder = JwtDecoders.fromIssuerLocation(issuer);
        } catch (RuntimeException err) {
            throw new AuthenticationServiceException("Could not read the metadata of the trusted "
                    + "issuer " + issuer + ": " + err.getMessage(), err);
        }
        AuthenticationManager made = new ProviderManager(new JwtAuthenticationProvider(decoder));
        synchronized (this) {
            managers.put(issuer, made);
        }
        return made;
    }

    /// The `iss` of a token nobody has verified.
    private static String issuerOf(String token) {
        int first = token.indexOf('.');
        int second = first < 0 ? -1 : token.indexOf('.', first + 1);
        if (first <= 0 || second <= first + 1) {
            throw new InvalidBearerTokenException("Malformed token: not three parts joined by dots");
        }
        byte[] payload = Base64Url.decode(token.substring(first + 1, second));
        Object issuer;
        try {
            if (payload == null) {
                throw new InvalidBearerTokenException("Malformed token: the payload is not base64url");
            }
            issuer = Json.parseObject(new String(payload, "UTF-8")).get("iss");
        } catch (java.io.IOException err) {
            throw new InvalidBearerTokenException("Malformed token: the payload is not a JSON "
                    + "object", err);
        } catch (InvalidBearerTokenException err) {
            throw err;
        } catch (RuntimeException err) {
            throw new InvalidBearerTokenException("Malformed token: the payload is not a JSON "
                    + "object", err);
        }
        if (!(issuer instanceof String) || ((String) issuer).length() == 0) {
            throw new InvalidBearerTokenException("Missing issuer");
        }
        return (String) issuer;
    }

    private final class Routing implements AuthenticationManager {
        @Override
        public Authentication authenticate(Authentication authentication) {
            if (!(authentication instanceof BearerTokenAuthenticationToken)) {
                return null;
            }
            String issuer = issuerOf(((BearerTokenAuthenticationToken) authentication).getToken());
            AuthenticationManager manager = managerFor(issuer);
            if (manager == null) {
                // Not echoed: the issuer is whatever the token's author wrote.
                throw new InvalidBearerTokenException("Invalid issuer");
            }
            return manager.authenticate(authentication);
        }
    }
}
