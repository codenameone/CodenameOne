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

/// Answers with a status and nothing else: `new HttpStatusEntryPoint(401)` for
/// an API whose clients know how to authenticate without being told.
public final class HttpStatusEntryPoint implements AuthenticationEntryPoint {
    private final int status;

    public HttpStatusEntryPoint(int status) {
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("An entry point answers with an error status");
        }
        this.status = status;
    }

    @Override
    public HttpServer.Response commence(HttpServer.Request request,
                                        AuthenticationException authException) {
        return HttpServer.Response.empty(status, "text/plain; charset=utf-8", null);
    }
}
