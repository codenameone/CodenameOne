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
import java.util.ArrayList;
import java.util.List;

/// Signs the user out when a request asks for it -- `POST /logout` unless
/// configured otherwise -- and answers with the chain's [LogoutSuccessHandler].
public final class LogoutFilter implements SecurityFilter {
    private final RequestMatcher logoutRequestMatcher;
    private final List<LogoutHandler> handlers;
    private final LogoutSuccessHandler successHandler;

    LogoutFilter(RequestMatcher logoutRequestMatcher, List<LogoutHandler> handlers,
                 LogoutSuccessHandler successHandler) {
        this.logoutRequestMatcher = logoutRequestMatcher;
        this.handlers = new ArrayList<LogoutHandler>(handlers);
        this.successHandler = successHandler;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        if (!logoutRequestMatcher.matches(request)) {
            return chain.doFilter(request);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        for (LogoutHandler handler : handlers) {
            handler.logout(request, authentication);
        }
        return successHandler.onLogoutSuccess(request, authentication);
    }
}
