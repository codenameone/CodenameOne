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

/// A [CsrfToken] that is its three values.
public final class DefaultCsrfToken implements CsrfToken {
    private final String headerName;
    private final String parameterName;
    private final String token;

    public DefaultCsrfToken(String headerName, String parameterName, String token) {
        if (headerName == null || headerName.length() == 0 || parameterName == null
                || parameterName.length() == 0 || token == null || token.length() == 0) {
            throw new IllegalArgumentException("A CSRF token needs a header name, a parameter "
                    + "name and a value");
        }
        this.headerName = headerName;
        this.parameterName = parameterName;
        this.token = token;
    }

    @Override
    public String getHeaderName() {
        return headerName;
    }

    @Override
    public String getParameterName() {
        return parameterName;
    }

    @Override
    public String getToken() {
        return token;
    }
}
