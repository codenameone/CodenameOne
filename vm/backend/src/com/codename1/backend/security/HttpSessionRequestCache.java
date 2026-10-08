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
import com.codename1.backend.HttpSession;

/// Remembers the address in the HTTP session, under [#SAVED_REQUEST].
///
/// A request is worth returning to when it is a GET a person's browser made:
/// not one a script sent (`X-Requested-With: XMLHttpRequest`), not one that asks
/// for JSON only, and not a request for the site's icon.
public final class HttpSessionRequestCache implements RequestCache {
    /// The session attribute the address is stored under.
    public static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST";

    private boolean createSessionAllowed = true;

    /// Whether remembering may start a session; true unless changed.
    public void setCreateSessionAllowed(boolean createSessionAllowed) {
        this.createSessionAllowed = createSessionAllowed;
    }

    @Override
    public void saveRequest(HttpServer.Request request) {
        if (!"GET".equals(request.getMethod())) {
            return;
        }
        String target = request.getTarget();
        if (!localTarget(target)) {
            return;
        }
        String path = SecurityExchange.path(request);
        if (path.startsWith("/favicon.") || "XMLHttpRequest".equals(
                request.getHeader("X-Requested-With"))) {
            return;
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.regionMatches(true, 0, "application/json", 0, 16)
                && accept.indexOf(',') < 0) {
            return;
        }
        HttpSession session = request.getSession(createSessionAllowed);
        if (session != null) {
            session.setAttribute(SAVED_REQUEST, target);
        }
    }

    @Override
    public String getRequest(HttpServer.Request request) {
        HttpSession session = request.getSession(false);
        Object saved = session == null ? null : session.getAttribute(SAVED_REQUEST);
        if (!(saved instanceof String)) {
            return null;
        }
        String target = (String) saved;
        // What was stored is this server's own path; anything else is not ours.
        return localTarget(target) ? target : null;
    }

    private static boolean localTarget(String target) {
        if (target == null || !target.startsWith("/") || target.startsWith("//")) {
            return false;
        }
        for (int i = 0; i < target.length(); i++) {
            char c = target.charAt(i);
            if (c == '\\' || c <= 32 || c == 127) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void removeRequest(HttpServer.Request request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SAVED_REQUEST) != null) {
            session.removeAttribute(SAVED_REQUEST);
        }
    }
}
