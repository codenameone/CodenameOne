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
import java.util.List;

/// The login page of a chain whose only ways in are other providers: a
/// link to each. Shared by `oauth2Login` and `saml2Login`, and a class of its
/// own so that neither drags the other into a server that declares one.
final class LoginLinksPage implements SecurityFilter {
    private final String loginPage;
    private final List<String[]> links;

    LoginLinksPage(String loginPage, List<String[]> links) {
        this.loginPage = loginPage;
        this.links = links;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        if (!"GET".equals(request.getMethod())
                || !loginPage.equals(SecurityExchange.path(request))) {
            return chain.doFilter(request);
        }
        StringBuilder page = new StringBuilder(512);
        page.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Please sign in</title>\n</head>\n<body>\n<h2>Please sign in</h2>\n");
        if (request.queryParam("error") != null) {
            page.append("<p role=\"alert\">The sign-in did not complete</p>\n");
        }
        if (request.queryParam("logout") != null) {
            page.append("<p role=\"status\">You have been signed out</p>\n");
        }
        for (String[] link : links) {
            page.append("<p><a href=\"").append(Responses.escape(link[0]))
                .append("\">Sign in with ").append(Responses.escape(link[1]))
                .append("</a></p>\n");
        }
        page.append("</body>\n</html>\n");
        return Responses.html(200, page.toString());
    }
}
