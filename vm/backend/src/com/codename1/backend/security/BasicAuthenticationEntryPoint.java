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

/// Answers 401 with the challenge that makes a client send HTTP Basic
/// credentials: `WWW-Authenticate: Basic realm="Realm"`.
public final class BasicAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private String realmName = "Realm";

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        if (realmName == null || realmName.indexOf('"') >= 0 || realmName.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("A realm name is text without quotes or "
                    + "backslashes");
        }
        for (int iter = 0 ; iter < realmName.length() ; iter++) {
            char c = realmName.charAt(iter);
            if (c < 0x20 || c > 0x7e) {
                throw new IllegalArgumentException("A realm name is printable ASCII");
            }
        }
        this.realmName = realmName;
    }

    @Override
    public HttpServer.Response commence(HttpServer.Request request,
                                        AuthenticationException authException) {
        return Responses.status(401, "Unauthorized")
                .header("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
    }
}
