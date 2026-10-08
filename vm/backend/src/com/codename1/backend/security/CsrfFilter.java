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

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import java.io.IOException;

/// Refuses a state-changing request that does not carry the chain's CSRF token.
///
/// GET, HEAD, TRACE and OPTIONS pass: they are not supposed to change anything.
/// Every other request must send the token back, in the header or the form
/// field the token names, and is answered 403 by the chain's
/// [AccessDeniedHandler] when it does not.
///
/// The token a page is given is masked: the stored value XORed with random
/// bytes that are sent along with it. Every response therefore carries a
/// different string for the same token, which is what keeps a compressed
/// response from leaking it to an attacker who can influence part of the body.
public final class CsrfFilter implements SecurityFilter {
    private static final String ATTRIBUTE = "com.codename1.backend.security.CsrfToken";
    /// The token a test's requests from this thread are checked against.
    private static final ThreadLocal<String> TEST = new ThreadLocal<String>();

    private final CsrfTokenRepository repository;
    private final RequestMatcher requireProtection;
    private final AccessDeniedHandler accessDeniedHandler;
    /// Whether the stored value itself is accepted in the header: the cookie
    /// repository hands a script exactly that.
    private final boolean acceptUnmasked;

    CsrfFilter(CsrfTokenRepository repository, RequestMatcher requireProtection,
               AccessDeniedHandler accessDeniedHandler) {
        this.repository = repository;
        this.requireProtection = requireProtection;
        this.accessDeniedHandler = accessDeniedHandler;
        this.acceptUnmasked = repository instanceof CookieCsrfTokenRepository;
    }

    /// The CSRF token of `request`, for a page or a script to send back, or null
    /// when the request is under no chain or its chain has CSRF protection off.
    /// Asking makes the token exist: with the session repository that starts a
    /// session.
    public static CsrfToken getToken(HttpServer.Request request) {
        SecurityExchange exchange = SecurityExchange.of(request);
        Object deferred = exchange == null ? null : exchange.getAttribute(ATTRIBUTE);
        return deferred instanceof Deferred ? ((Deferred) deferred).get() : null;
    }

    /// Replaces the token when a user signs in, as the session id is replaced:
    /// a token an attacker saw before sign-in is worthless after it.
    static void rotate(HttpServer.Request request) {
        SecurityExchange exchange = SecurityExchange.of(request);
        Object deferred = exchange == null ? null : exchange.getAttribute(ATTRIBUTE);
        if (deferred instanceof Deferred) {
            ((Deferred) deferred).rotate();
        }
    }

    /// Forgets the token when a user signs out.
    static void clear(HttpServer.Request request) {
        SecurityExchange exchange = SecurityExchange.of(request);
        Object deferred = exchange == null ? null : exchange.getAttribute(ATTRIBUTE);
        if (deferred instanceof Deferred) {
            ((Deferred) deferred).forget();
        }
    }

    static void testToken(String value) {
        TEST.set(value);
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        SecurityExchange exchange = SecurityExchange.of(request);
        Deferred deferred = new Deferred(request);
        if (exchange != null) {
            exchange.setAttribute(ATTRIBUTE, deferred);
        }
        if (acceptUnmasked && exchange != null) {
            // A script can only send back a cookie it has been given, so with
            // the cookie repository every response makes sure the client has one.
            deferred.get();
        }
        if (!requireProtection.matches(request)) {
            return chain.doFilter(request);
        }
        CsrfToken stored = deferred.stored();
        if (stored == null) {
            return accessDeniedHandler.handle(request, new MissingCsrfTokenException(null));
        }
        String actual = request.getHeader(stored.getHeaderName());
        boolean fromHeader = actual != null;
        if (actual == null) {
            actual = Responses.param(request, stored.getParameterName());
        }
        if (!valid(stored.getToken(), actual, fromHeader && acceptUnmasked)) {
            return accessDeniedHandler.handle(request, new InvalidCsrfTokenException(stored, actual));
        }
        return chain.doFilter(request);
    }

    /// The token of one request, loaded when something first asks for it: a
    /// request that never does -- most GETs -- touches no session.
    private final class Deferred {
        private final HttpServer.Request request;
        private CsrfToken stored;
        private boolean loaded;
        private CsrfToken masked;

        Deferred(HttpServer.Request request) {
            this.request = request;
        }

        /// The token the client is expected to send, or null when it has none.
        CsrfToken stored() {
            if (!loaded) {
                loaded = true;
                String test = TEST.get();
                if (test != null) {
                    // Under the default names whichever repository the chain
                    // uses: a test sends its token without knowing which.
                    stored = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", test);
                } else {
                    stored = repository.loadToken(request);
                }
            }
            return stored;
        }

        /// The token to hand a page, making and storing one when there is none.
        CsrfToken get() {
            if (stored() == null) {
                stored = repository.generateToken(request);
                repository.saveToken(stored, request);
            }
            if (masked == null) {
                masked = new DefaultCsrfToken(stored.getHeaderName(), stored.getParameterName(),
                        mask(stored.getToken()));
            }
            return masked;
        }

        void rotate() {
            if (stored() != null && TEST.get() == null) {
                repository.saveToken(null, request);
                stored = repository.generateToken(request);
                repository.saveToken(stored, request);
                masked = null;
            }
        }

        void forget() {
            if (stored() != null && TEST.get() == null) {
                repository.saveToken(null, request);
            }
            stored = null;
            masked = null;
        }
    }

    /// The methods CSRF protection leaves alone.
    static final RequestMatcher DEFAULT_REQUIRE_PROTECTION = new RequestMatcher() {
        @Override
        public boolean matches(HttpServer.Request request) {
            String method = request.getMethod();
            return !("GET".equals(method) || "HEAD".equals(method) || "TRACE".equals(method)
                    || "OPTIONS".equals(method));
        }

        @Override
        public String toString() {
            return "CsrfNotRequired [TRACE, HEAD, GET, OPTIONS]";
        }
    };

    /// A new token value: 24 random bytes, as 32 characters.
    static String newTokenValue() {
        try {
            return Base64Url.encode(Crypto.randomBytes(24));
        } catch (IOException err) {
            throw new IllegalStateException("No secure randomness available", err);
        }
    }

    /// `token` XORed with as many random bytes, which go in front of it.
    static String mask(String token) {
        byte[] bytes = Responses.utf8(token);
        byte[] random;
        try {
            random = Crypto.randomBytes(bytes.length);
        } catch (IOException err) {
            throw new IllegalStateException("No secure randomness available", err);
        }
        byte[] out = new byte[bytes.length * 2];
        System.arraycopy(random, 0, out, 0, random.length);
        for (int iter = 0 ; iter < bytes.length ; iter++) {
            out[bytes.length + iter] = (byte) (bytes[iter] ^ random[iter]);
        }
        return Base64Url.encode(out);
    }

    /// Whether `actual` is `token`: masked, or -- when allowed -- as it is.
    static boolean valid(String token, String actual, boolean unmaskedAllowed) {
        if (actual == null || actual.length() == 0) {
            return false;
        }
        byte[] expected = Responses.utf8(token);
        if (unmaskedAllowed && Crypto.equalsConstantTime(expected, Responses.utf8(actual))) {
            return true;
        }
        byte[] decoded = Base64Url.decode(actual);
        if (decoded == null || decoded.length != expected.length * 2) {
            return false;
        }
        byte[] unmasked = new byte[expected.length];
        for (int iter = 0 ; iter < expected.length ; iter++) {
            unmasked[iter] = (byte) (decoded[iter] ^ decoded[expected.length + iter]);
        }
        return Crypto.equalsConstantTime(expected, unmasked);
    }

    /// A header, parameter or cookie name: letters, digits, `-` and `_`.
    static String requireName(String name, String what) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException(what + " cannot be null or empty");
        }
        for (int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_')) {
                throw new IllegalArgumentException(what + " \"" + name + "\" may hold letters, "
                        + "digits, - and _ only");
            }
        }
        return name;
    }
}
