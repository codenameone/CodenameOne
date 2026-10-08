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

/// What happens to the session, and to the CSRF token, when a user signs in.
final class SessionAuthentication {
    static final int CHANGE_SESSION_ID = 0;
    static final int NEW_SESSION = 1;
    static final int NONE = 2;

    private final int fixation;
    private final boolean stateless;

    SessionAuthentication(int fixation, boolean stateless) {
        this.fixation = fixation;
        this.stateless = stateless;
    }

    void onAuthentication(HttpServer.Request request) {
        if (!stateless) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                if (fixation == CHANGE_SESSION_ID) {
                    // An id the client held before it signed in is one somebody
                    // else may have planted on it.
                    session.changeSessionId();
                } else if (fixation == NEW_SESSION) {
                    session.invalidate();
                    request.getSession(true);
                }
            }
        }
        CsrfFilter.rotate(request);
    }
}
