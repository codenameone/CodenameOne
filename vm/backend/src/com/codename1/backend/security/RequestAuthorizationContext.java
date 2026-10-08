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
import java.util.Collections;
import java.util.Map;

/// What a request rule is asked about: the request, and the variables its
/// pattern bound.
public final class RequestAuthorizationContext {
    private final HttpServer.Request request;
    private final Map<String, String> variables;

    public RequestAuthorizationContext(HttpServer.Request request) {
        this(request, Collections.<String, String>emptyMap());
    }

    public RequestAuthorizationContext(HttpServer.Request request, Map<String, String> variables) {
        this.request = request;
        this.variables = variables;
    }

    public HttpServer.Request getRequest() {
        return request;
    }

    /// `{name}` of the matched pattern, by name.
    public Map<String, String> getVariables() {
        return variables;
    }
}
