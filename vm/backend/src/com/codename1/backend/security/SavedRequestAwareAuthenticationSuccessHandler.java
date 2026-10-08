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

/// After sign-in, sends the user to the page that asked them to sign in, or to
/// a default when they came to the login page on their own.
public final class SavedRequestAwareAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {
    private String defaultTargetUrl = "/";
    private boolean alwaysUseDefaultTargetUrl;
    private RequestCache requestCache = new HttpSessionRequestCache();

    /// Where to go when there is nowhere to return to; `/` unless set.
    public void setDefaultTargetUrl(String defaultTargetUrl) {
        if (defaultTargetUrl == null || !defaultTargetUrl.startsWith("/")
                || defaultTargetUrl.startsWith("//")) {
            throw new IllegalArgumentException("defaultTargetUrl must be a path on this "
                    + "server, starting with one /");
        }
        this.defaultTargetUrl = defaultTargetUrl;
    }

    /// Whether to go to the default even when a page asked for the sign-in.
    public void setAlwaysUseDefaultTargetUrl(boolean alwaysUseDefaultTargetUrl) {
        this.alwaysUseDefaultTargetUrl = alwaysUseDefaultTargetUrl;
    }

    public void setRequestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
    }

    @Override
    public HttpServer.Response onAuthenticationSuccess(HttpServer.Request request,
                                                       Authentication authentication) {
        String saved = requestCache.getRequest(request);
        if (saved != null) {
            requestCache.removeRequest(request);
        }
        return Responses.redirect(saved == null || alwaysUseDefaultTargetUrl ? defaultTargetUrl
                : saved);
    }
}
