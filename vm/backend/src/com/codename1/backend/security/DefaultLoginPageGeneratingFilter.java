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

import com.codename1.backend.HttpServer;

/// Serves a plain login page at `GET /login` for a chain that uses form login
/// and names no page of its own.
public final class DefaultLoginPageGeneratingFilter implements SecurityFilter {
    private final String loginPage;
    private final String processingUrl;
    private final String usernameParameter;
    private final String passwordParameter;
    private final String rememberMeParameter;

    DefaultLoginPageGeneratingFilter(String loginPage, String processingUrl,
                                     String usernameParameter, String passwordParameter,
                                     String rememberMeParameter) {
        this.rememberMeParameter = rememberMeParameter;
        this.loginPage = loginPage;
        this.processingUrl = processingUrl;
        this.usernameParameter = usernameParameter;
        this.passwordParameter = passwordParameter;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        if (!"GET".equals(request.getMethod())
                || !loginPage.equals(SecurityExchange.path(request))) {
            return chain.doFilter(request);
        }
        boolean error = request.queryParam("error") != null;
        boolean loggedOut = request.queryParam("logout") != null;
        CsrfToken token = CsrfFilter.getToken(request);
        StringBuilder page = new StringBuilder(1024);
        page.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            .append("<title>Please sign in</title>\n</head>\n<body>\n")
            .append("<form method=\"post\" action=\"").append(Responses.escape(processingUrl))
            .append("\">\n<h2>Please sign in</h2>\n");
        if (error) {
            page.append("<p role=\"alert\">Bad credentials</p>\n");
        }
        if (loggedOut) {
            page.append("<p role=\"status\">You have been signed out</p>\n");
        }
        page.append("<p><label for=\"username\">Username</label>\n<input type=\"text\" id=\"username\" "
                + "name=\"").append(Responses.escape(usernameParameter))
            .append("\" required autofocus autocomplete=\"username\"></p>\n")
            .append("<p><label for=\"password\">Password</label>\n<input type=\"password\" "
                + "id=\"password\" name=\"").append(Responses.escape(passwordParameter))
            .append("\" required autocomplete=\"current-password\"></p>\n");
        if (rememberMeParameter != null) {
            page.append("<p><input type=\"checkbox\" id=\"remember-me\" name=\"")
                .append(Responses.escape(rememberMeParameter))
                .append("\"> <label for=\"remember-me\">Remember me on this computer</label></p>\n");
        }
        if (token != null) {
            page.append("<input type=\"hidden\" name=\"")
                .append(Responses.escape(token.getParameterName())).append("\" value=\"")
                .append(Responses.escape(token.getToken())).append("\">\n");
        }
        page.append("<button type=\"submit\">Sign in</button>\n</form>\n</body>\n</html>\n");
        return Responses.html(200, page.toString());
    }
}
