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
package com.codename1.backend.security;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.mail.PasswordResetService;
import com.codename1.backend.security.ratelimit.RateLimitKeyResolver;
import com.codename1.backend.security.ratelimit.RateLimitKeys;
import com.codename1.backend.security.ratelimit.RateLimiter;

/// Serves the pages of a password reset and takes what they post; see
/// [PasswordResetConfigurer].
public final class PasswordResetFilter implements SecurityFilter {
    private final PasswordResetService service;
    private final String requestUrl;
    private final String resetUrl;
    private final String baseUrl;
    private final boolean servePages;
    private final RateLimiter attempts;
    private final String loginPage;
    private final RateLimitKeyResolver network = RateLimitKeys.clientNetwork();

    PasswordResetFilter(PasswordResetService service, String requestUrl, String resetUrl,
            String baseUrl, boolean servePages, RateLimiter attempts, String loginPage) {
        this.service = service;
        this.requestUrl = requestUrl;
        this.resetUrl = resetUrl;
        this.baseUrl = baseUrl;
        this.servePages = servePages;
        this.attempts = attempts;
        this.loginPage = loginPage;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String path = SecurityExchange.path(request);
        boolean get = "GET".equals(request.getMethod());
        boolean post = "POST".equals(request.getMethod());
        if (requestUrl.equals(path)) {
            if (post) {
                return ask(request);
            }
            if (get && servePages) {
                return askPage(request);
            }
        } else if (resetUrl.equals(path)) {
            if (post) {
                return set(request);
            }
            if (get && servePages) {
                return setPage(request);
            }
        }
        return chain.doFilter(request);
    }

    /// The address a link to this server starts with. The request's own host
    /// is believed only when it is this machine: a link is mailed to somebody
    /// who did not send the request.
    static String origin(HttpServer.Request request, String configured) {
        if (configured != null) {
            return configured;
        }
        String host = request.getHeader("Host");
        String name = host == null ? "" : host;
        int port = name.lastIndexOf(':');
        if (port > name.lastIndexOf(']')) {
            name = name.substring(0, port);
        }
        boolean digits = host != null;
        for (int iter = port + 1 ; digits && port >= 0 && iter < host.length() ; iter++) {
            digits = host.charAt(iter) >= '0' && host.charAt(iter) <= '9';
        }
        if (!digits || !("localhost".equalsIgnoreCase(name) || "127.0.0.1".equals(name) // NOPMD AvoidUsingHardCodedIP - loopback is the one host trusted without a setting
                || "[::1]".equals(name))) {
            throw new IllegalStateException("A link cannot be mailed without knowing the "
                    + "address this server is reached at. Set "
                    + PasswordResetConfigurer.BASE_URL + "=https://your.host in "
                    + "application.properties; only a request to localhost is served without "
                    + "it.");
        }
        return (request.isSecure() ? "https://" : "http://") + host;
    }

    private HttpServer.Response ask(HttpServer.Request request) {
        String username = Responses.param(request, "username");
        String origin = origin(request, baseUrl);
        if (attempts != null) {
            String from = network.resolve(request);
            String here = "password-reset:" + (from == null ? "net:none" : from);
            if (!attempts.tryAcquire(here)) {
                return tooMany(attempts.retryAfterSeconds(here));
            }
            // Per account as well, or one caller spread over many addresses
            // fills a mailbox. It lets anybody who knows a user name use up
            // that user's allowance for a quarter of an hour; a reset is
            // not how a user signs in, and mail to them without end is
            // the worse of the two.
            if (username != null && username.length() > 0 && username.length() <= 320) {
                String who = "password-reset:user:" + SecuritySchema.usernameKey(username);
                if (!attempts.tryAcquire(who)) {
                    return tooMany(attempts.retryAfterSeconds(who));
                }
            }
        }
        service.request(username, origin + resetUrl);
        return Responses.redirect(requestUrl + "?sent");
    }

    private HttpServer.Response set(HttpServer.Request request) {
        String token = Responses.param(request, "token");
        String password = Responses.param(request, "password");
        String confirm = Responses.param(request, "confirm");
        if (token == null || !isToken(token)) {
            return Responses.redirect(requestUrl + "?expired");
        }
        // Looked at before the token is, so that a slip of the keyboard does
        // not cost the user their link.
        if (password == null || (confirm != null && !confirm.equals(password))
                || !service.isAcceptable(password)) {
            return Responses.redirect(resetUrl + "?token=" + token + "&invalid");
        }
        if (!service.reset(token, password)) {
            return Responses.redirect(requestUrl + "?expired");
        }
        return Responses.redirect(loginPage + "?reset");
    }

    /// Whether `token` is written as a token is, and so can go into a URL as
    /// it stands.
    static boolean isToken(String token) {
        if (token.length() != 43) {
            return false;
        }
        for (int iter = 0 ; iter < token.length() ; iter++) {
            char c = token.charAt(iter);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }

    private static HttpServer.Response tooMany(long wait) {
        return Responses.status(429, "Too Many Requests")
                .header("Retry-After", String.valueOf(wait < 1 ? 1 : wait));
    }

    private static StringBuilder open(String title, String action) {
        StringBuilder html = new StringBuilder(1024);
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("<title>").append(title).append("</title>\n</head>\n<body>\n");
        html.append("<form method=\"post\" action=\"").append(Responses.escape(action));
        html.append("\">\n<h2>").append(title).append("</h2>\n");
        return html;
    }

    private static HttpServer.Response close(HttpServer.Request request, StringBuilder html) {
        CsrfToken token = CsrfFilter.getToken(request);
        if (token != null) {
            html.append("<input type=\"hidden\" name=\"")
                .append(Responses.escape(token.getParameterName())).append("\" value=\"")
                .append(Responses.escape(token.getToken())).append("\">\n");
        }
        html.append("<button type=\"submit\">Continue</button>\n</form>\n</body>\n</html>\n");
        // The address of the second page carries the token: it is not to be
        // kept by a cache or handed on to whatever the page links to.
        return Responses.html(200, html.toString()).header("Cache-Control", "no-store")
                .header("Referrer-Policy", "no-referrer");
    }

    private HttpServer.Response askPage(HttpServer.Request request) {
        StringBuilder html = open("Reset your password", requestUrl);
        if (request.queryParam("sent") != null) {
            html.append("<p role=\"status\">If there is an account by that name, a link to "
                    + "reset its password is on its way to its address</p>\n");
        } else if (request.queryParam("expired") != null) {
            html.append("<p role=\"alert\">That link is no longer good. Ask for another</p>\n");
        }
        html.append("<p><label for=\"username\">Your user name</label>\n<input type=\"text\" "
                + "id=\"username\" name=\"username\" required autofocus "
                + "autocomplete=\"username\"></p>\n");
        return close(request, html);
    }

    private HttpServer.Response setPage(HttpServer.Request request) {
        String token = request.queryParam("token");
        if (token == null || !isToken(token)) {
            return Responses.redirect(requestUrl + "?expired");
        }
        StringBuilder html = open("Choose a new password", resetUrl);
        if (request.queryParam("invalid") != null) {
            html.append("<p role=\"alert\">The two passwords must be the same, and at least ")
                .append(service.getMinimumLength()).append(" characters long</p>\n");
        }
        html.append("<input type=\"hidden\" name=\"token\" value=\"").append(token);
        html.append("\">\n<p><label for=\"password\">New password</label>\n<input "
                + "type=\"password\" id=\"password\" name=\"password\" required autofocus "
                + "autocomplete=\"new-password\"></p>\n<p><label for=\"confirm\">The same "
                + "again</label>\n<input type=\"password\" id=\"confirm\" name=\"confirm\" "
                + "required autocomplete=\"new-password\"></p>\n");
        return close(request, html);
    }
}
