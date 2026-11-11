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
import com.codename1.backend.security.mail.EmailVerificationService;

/// Takes a mailed verification link; see [EmailVerificationConfigurer].
public final class EmailVerificationFilter implements SecurityFilter {
    private final EmailVerificationService service;
    private final String confirmUrl;
    private final String successUrl;

    EmailVerificationFilter(EmailVerificationService service, String confirmUrl,
            String successUrl) {
        this.service = service;
        this.confirmUrl = confirmUrl;
        this.successUrl = successUrl;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        boolean get = "GET".equals(request.getMethod());
        if (!(get || "POST".equals(request.getMethod()))
                || !confirmUrl.equals(SecurityExchange.path(request))) {
            return chain.doFilter(request);
        }
        String token = get ? request.queryParam("token") : Responses.param(request, "token");
        if (token == null || !PasswordResetFilter.isToken(token)) {
            return gone();
        }
        if (get) {
            // Opening the link proves nothing: a mail gateway opens every link
            // in a mail before its reader sees it, and would prove an address
            // for whoever typed it. The page asks, and the answer is a POST,
            // which no scanner sends.
            StringBuilder html = new StringBuilder(768);
            html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
                    + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                    + "<title>Confirm your email address</title>\n</head>\n<body>\n"
                    + "<form method=\"post\" action=\"").append(Responses.escape(confirmUrl))
                .append("\">\n<h2>Confirm your email address</h2>\n"
                    + "<input type=\"hidden\" name=\"token\" value=\"").append(token)
                .append("\">\n");
            CsrfToken csrf = CsrfFilter.getToken(request);
            if (csrf != null) {
                html.append("<input type=\"hidden\" name=\"")
                    .append(Responses.escape(csrf.getParameterName())).append("\" value=\"")
                    .append(Responses.escape(csrf.getToken())).append("\">\n");
            }
            html.append("<button type=\"submit\">Confirm address</button>\n</form>\n</body>\n"
                    + "</html>\n");
            // The address of this page carries the token.
            return Responses.html(200, html.toString()).header("Cache-Control", "no-store")
                    .header("Referrer-Policy", "no-referrer");
        }
        if (service.confirm(token) == null) {
            return gone();
        }
        return Responses.redirect(successUrl).header("Referrer-Policy", "no-referrer");
    }

    private static HttpServer.Response gone() {
        return Responses.html(400, "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n"
                + "<meta charset=\"utf-8\">\n<title>Link no longer good</title>\n</head>\n"
                + "<body>\n<p role=\"alert\">That link is no longer good. Ask for another"
                + "</p>\n</body>\n</html>\n").header("Cache-Control", "no-store");
    }
}
