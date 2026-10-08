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
import java.util.Collections;
import java.util.List;

/// A [SecurityFilterChain] that is a matcher and a list of filters: what
/// [HttpSecurity#build] returns.
public final class DefaultSecurityFilterChain implements SecurityFilterChain {
    private final RequestMatcher requestMatcher;
    private final List<SecurityFilter> filters;

    public DefaultSecurityFilterChain(RequestMatcher requestMatcher, List<SecurityFilter> filters) {
        if (requestMatcher == null || filters == null) {
            throw new IllegalArgumentException("A chain needs a matcher and its filters");
        }
        this.requestMatcher = requestMatcher;
        this.filters = Collections.unmodifiableList(new ArrayList<SecurityFilter>(filters));
    }

    public RequestMatcher getRequestMatcher() {
        return requestMatcher;
    }

    @Override
    public List<SecurityFilter> getFilters() {
        return filters;
    }

    @Override
    public boolean matches(HttpServer.Request request) {
        return requestMatcher.matches(request);
    }

    @Override
    public String toString() {
        return "DefaultSecurityFilterChain [RequestMatcher=" + requestMatcher + ", Filters="
                + filters.size() + "]";
    }
}
