/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Cross-origin resource sharing for the application's routes, from `cn1.cors.*`.
///
/// Off unless `cn1.cors.allowedOrigins` names something, because the default a
/// browser applies -- no cross-origin reads -- is the safe one and turning it off
/// is a decision. A page served from one origin calling the API on another (a
/// web app on its own port, a CDN in front of a separate API host) needs it on.
///
/// A preflight is answered only when no handler answered the OPTIONS request
/// itself, so an endpoint with an origin policy of its own -- the MCP endpoint --
/// keeps it. An ordinary response gains the headers only when its handler set no
/// `Access-Control-Allow-Origin` of its own.
final class Cors {
    static final String ALLOWED_ORIGINS = "cn1.cors.allowedOrigins";
    static final String ALLOWED_METHODS = "cn1.cors.allowedMethods";
    static final String ALLOWED_HEADERS = "cn1.cors.allowedHeaders";
    static final String EXPOSED_HEADERS = "cn1.cors.exposedHeaders";
    static final String ALLOW_CREDENTIALS = "cn1.cors.allowCredentials";
    static final String MAX_AGE = "cn1.cors.maxAgeSeconds";

    private final List origins;
    private final boolean anyOrigin;
    private final String methods;
    private final String headers;
    private final String exposed;
    private final boolean credentials;
    private final int maxAge;

    private Cors(List origins, String methods, String headers, String exposed,
                 boolean credentials, int maxAge) {
        this.origins = origins;
        this.anyOrigin = origins.contains("*");
        this.methods = methods;
        this.headers = headers;
        this.exposed = exposed;
        this.credentials = credentials;
        this.maxAge = maxAge;
    }

    /// Whether the server speaks TLS; set once, before it serves anything.
    private boolean tls;

    /// Records whether requests arrive over TLS, which decides the scheme of the
    /// server's own origin.
    void servedOverTls(boolean value) {
        this.tls = value;
    }

    /// The configured policy, or null when CORS is off.
    static Cors fromConfig(Config config) throws IOException {
        String listed = config.get(ALLOWED_ORIGINS, null);
        if (listed == null || listed.trim().length() == 0) {
            return null;
        }
        List origins = new ArrayList();
        int pos = 0;
        while (pos <= listed.length()) {
            int comma = listed.indexOf(',', pos);
            int end = comma < 0 ? listed.length() : comma;
            String origin = listed.substring(pos, end).trim();
            if (origin.length() > 0) {
                // An origin is scheme://host[:port] with nothing after it; a path,
                // query or fragment (a trailing slash included) never matches what a
                // browser sends, so it is refused here rather than leaving a policy
                // that silently allows no one.
                if (!"*".equals(origin) && !isOrigin(origin)) {
                    throw new IOException(ALLOWED_ORIGINS + " names \"" + origin + "\"; an origin "
                            + "is scheme://host[:port], with no path");
                }
                origins.add(origin);
            }
            if (comma < 0) {
                break;
            }
            pos = comma + 1;
        }
        boolean credentials = config.getBoolean(ALLOW_CREDENTIALS, false);
        if (credentials && origins.contains("*")) {
            // Browsers refuse a credentialed response that allows every origin, so
            // the combination is a policy that cannot work as written.
            throw new IOException(ALLOW_CREDENTIALS + "=true needs " + ALLOWED_ORIGINS
                    + " to list the origins; a browser refuses credentials with *");
        }
        return new Cors(origins,
                config.get(ALLOWED_METHODS, "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS"),
                config.get(ALLOWED_HEADERS, "*"),
                config.get(EXPOSED_HEADERS, ""),
                credentials,
                config.getInt(MAX_AGE, 1800));
    }

    /// Whether `origin` may read responses.
    boolean allows(String origin) {
        if (origin == null) {
            return false;
        }
        if (anyOrigin) {
            return true;
        }
        for (Object listed : origins) {
            if (origin.equalsIgnoreCase((String) listed)) {
                return true;
            }
        }
        return false;
    }

    /// A cross-origin request the policy does not admit -- its origin is not
    /// listed, or its method (the requested one, for a preflight) is not in
    /// `cn1.cors.allowedMethods` -- answered 403 before any handler runs, as
    /// Spring's CorsProcessor answers it; null otherwise. Checked only after the
    /// response used to be: a simple request (a `text/plain` POST needs no
    /// preflight) then ran its handler, side effects and all, and only the CORS
    /// headers were withheld -- and the method list was never consulted for it.
    /// A same-origin request carries an Origin too, and is not a CORS request.
    HttpServer.Response reject(HttpServer.Request request) {
        String origin = request.getHeader("Origin");
        if (origin == null || sameOrigin(origin, requestScheme(request), request.getHeader("Host"))) {
            return null;
        }
        String asked = request.getHeader("Access-Control-Request-Method");
        String method = "OPTIONS".equals(request.getMethod()) && asked != null ? asked.trim()
                : request.getMethod();
        if (!allows(origin)) {
            return varyByOrigin(HttpServer.Response.text(403, "origin not allowed"));
        }
        if (!allowsMethod(method)) {
            return varyByOrigin(HttpServer.Response.text(403, "method not allowed"));
        }
        return null;
    }

    private boolean allowsMethod(String method) {
        if ("*".equals(methods.trim())) {
            return true;
        }
        int pos = 0;
        while (pos <= methods.length()) {
            int comma = methods.indexOf(',', pos);
            int end = comma < 0 ? methods.length() : comma;
            if (methods.substring(pos, end).trim().equals(method)) {
                return true;
            }
            if (comma < 0) {
                break;
            }
            pos = comma + 1;
        }
        return false;
    }

    /// The scheme the request was sent with: X-Forwarded-Proto when a TLS
    /// terminating proxy names it -- the server itself then sees plain HTTP for a
    /// page served over https -- else whether this server speaks TLS. A page
    /// cannot put that header on a simple request, and a preflight carries only
    /// the names of the headers to come.
    private String requestScheme(HttpServer.Request request) {
        String forwarded = request.getHeader("X-Forwarded-Proto");
        if (forwarded != null) {
            int comma = forwarded.indexOf(',');
            String first = (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
            if (first.length() > 0) {
                return first;
            }
        }
        return tls ? "https" : "http";
    }

    /// Whether `origin` names the server the request was sent to: the same scheme,
    /// and a host and port equal to the Host header's, a default port written or
    /// not. Without the scheme, `http://api.example` calling the TLS server at
    /// `https://api.example` -- a cross-origin request to a browser -- skipped
    /// the policy altogether.
    private static boolean sameOrigin(String origin, String requestScheme, String host) {
        if (host == null) {
            return false;
        }
        int scheme = origin.indexOf("://");
        if (scheme < 0 || !origin.substring(0, scheme).equalsIgnoreCase(requestScheme)) {
            return false;
        }
        String authority = origin.substring(scheme + 3);
        boolean https = "https".equalsIgnoreCase(requestScheme);
        return withoutDefaultPort(authority, https).equalsIgnoreCase(withoutDefaultPort(host.trim(), https));
    }

    private static String withoutDefaultPort(String authority, boolean https) {
        String port = https ? ":443" : ":80";
        return authority.endsWith(port) ? authority.substring(0, authority.length() - port.length())
                : authority;
    }

    /// The answer to a preflight no handler took, or null when `request` is not
    /// one. A preflight from an origin the policy does not list is a 403.
    HttpServer.Response preflight(HttpServer.Request request) {
        if (!"OPTIONS".equals(request.getMethod())) {
            return null;
        }
        String origin = request.getHeader("Origin");
        String asked = request.getHeader("Access-Control-Request-Method");
        if (origin == null || asked == null) {
            return null;
        }
        if (!allows(origin)) {
            return varyByOrigin(HttpServer.Response.text(403, "origin not allowed"));
        }
        HttpServer.Response response = varyByOrigin(HttpServer.Response.empty(204, null, null));
        allowOrigin(response, origin);
        // With credentials "*" is a literal method name to a browser, as it is for
        // headers below, so the method asked for is echoed instead.
        response.header("Access-Control-Allow-Methods",
                credentials && "*".equals(methods.trim()) ? asked.trim() : methods);
        String requested = request.getHeader("Access-Control-Request-Headers");
        if ("*".equals(headers.trim())) {
            // With credentials "*" is a literal header name to a browser, so the
            // headers actually requested are echoed instead; without them it is a
            // wildcard and echoing is equivalent.
            if (requested != null && requested.length() > 0) {
                response.header("Access-Control-Allow-Headers", requested);
            }
        } else {
            response.header("Access-Control-Allow-Headers", headers);
        }
        response.header("Access-Control-Max-Age", String.valueOf(maxAge));
        return response;
    }

    /// Adds the headers a browser reads to a response for an allowed origin.
    void decorate(HttpServer.Request request, HttpServer.Response response) {
        if (response == null || response.hasHeader("Access-Control-Allow-Origin")) {
            return;
        }
        if (!anyOrigin || credentials) {
            // The answer depends on Origin whenever the policy names origins -- the
            // headers below or their absence -- so a shared cache must key on it even
            // for a request with no Origin or a denied one. Without it, a cached copy
            // with no CORS headers was served to an allowed origin, whose browser
            // then blocked a permitted response.
            response.appendToken("Vary", "Origin");
        }
        String origin = request.getHeader("Origin");
        if (origin == null || !allows(origin)) {
            return;
        }
        allowOrigin(response, origin);
        if (exposed.trim().length() > 0) {
            response.header("Access-Control-Expose-Headers", exposed);
        }
    }

    /// `response`, with `Vary: Origin` when the policy names origins.
    private HttpServer.Response varyByOrigin(HttpServer.Response response) {
        if (!anyOrigin || credentials) {
            response.appendToken("Vary", "Origin");
        }
        return response;
    }

    private void allowOrigin(HttpServer.Response response, String origin) {
        // The origin itself rather than "*" whenever the answer depends on it, and
        // Vary so a cache does not hand one origin's answer to another.
        if (anyOrigin && !credentials) {
            response.header("Access-Control-Allow-Origin", "*");
        } else {
            // Vary: Origin was added by decorate, for every answer of this policy.
            response.header("Access-Control-Allow-Origin", origin);
        }
        if (credentials) {
            response.header("Access-Control-Allow-Credentials", "true");
        }
    }

    /// Whether `value` is scheme://authority and nothing more.
    private static boolean isOrigin(String value) {
        int scheme = value.indexOf("://");
        if (scheme <= 0) {
            return false;
        }
        String authority = value.substring(scheme + 3);
        return authority.length() > 0 && authority.indexOf('/') < 0 && authority.indexOf('?') < 0
                && authority.indexOf('#') < 0;
    }
}
