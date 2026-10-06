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
package com.codename1.backend.security.ratelimit;

import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.security.AnonymousAuthenticationToken;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.apikey.ApiKeyAuthenticationToken;

/// The keys a request is usually counted under.
public final class RateLimitKeys {

    // Each resolver is made by the method that hands it out, not held in a
    // static: a constant would put every one of them -- and with the API key
    // one, the API key classes -- into any server that limits by address.
    private RateLimitKeys() {
    }

    /// The address of the client: [HttpServer.Request#getRemoteAddress]. The
    /// one key a request has before anybody has signed in, and so the one for
    /// a login form.
    ///
    /// Behind a load balancer this is the load balancer's address -- one key
    /// for every client there is -- until the server is told to believe the
    /// forwarding headers: `cn1.server.forwardHeaders`.
    public static RateLimitKeyResolver clientAddress() {
        return new ClientAddress();
    }

    /// The name of who is signed in; no key for a request nobody signed in for.
    public static RateLimitKeyResolver principal() {
        return new Principal();
    }

    /// The id of the request's session; no key for a request without one.
    public static RateLimitKeyResolver sessionId() {
        return new Session();
    }

    /// The id of the API key the request presented; no key for a request that
    /// signed in another way.
    public static RateLimitKeyResolver apiKeyId() {
        return new Key();
    }

    /// The first of `resolvers` that has a key for the request: who is signed
    /// in, or else the client's address.
    public static RateLimitKeyResolver firstOf(RateLimitKeyResolver... resolvers) {
        if (resolvers == null || resolvers.length == 0) {
            throw new IllegalArgumentException("At least one resolver is required");
        }
        return new FirstOf(resolvers.clone());
    }

    private static Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private static final class ClientAddress implements RateLimitKeyResolver {
        @Override
        public String resolve(HttpServer.Request request) {
            String address = request.getRemoteAddress();
            return address == null ? null : "ip:" + address;
        }
    }

    private static final class Principal implements RateLimitKeyResolver {
        @Override
        public String resolve(HttpServer.Request request) {
            Authentication authentication = authentication();
            return authentication == null ? null : "user:" + authentication.getName();
        }

        @Override
        public boolean needsAuthentication() {
            return true;
        }
    }

    private static final class Session implements RateLimitKeyResolver {
        @Override
        public String resolve(HttpServer.Request request) {
            HttpSession session = request.getSession(false);
            return session == null ? null : "session:" + session.getId();
        }
    }

    private static final class Key implements RateLimitKeyResolver {
        @Override
        public String resolve(HttpServer.Request request) {
            Authentication authentication = authentication();
            return authentication instanceof ApiKeyAuthenticationToken
                    ? "key:" + ((ApiKeyAuthenticationToken) authentication).getApiKey().getId()
                    : null;
        }

        @Override
        public boolean needsAuthentication() {
            return true;
        }
    }

    private static final class FirstOf implements RateLimitKeyResolver {
        private final RateLimitKeyResolver[] resolvers;

        FirstOf(RateLimitKeyResolver[] resolvers) {
            this.resolvers = resolvers;
        }

        @Override
        public String resolve(HttpServer.Request request) {
            for (RateLimitKeyResolver resolver : resolvers) {
                String key = resolver.resolve(request);
                if (key != null) {
                    return key;
                }
            }
            return null;
        }

        @Override
        public boolean needsAuthentication() {
            for (RateLimitKeyResolver resolver : resolvers) {
                if (resolver.needsAuthentication()) {
                    return true;
                }
            }
            return false;
        }
    }
}
