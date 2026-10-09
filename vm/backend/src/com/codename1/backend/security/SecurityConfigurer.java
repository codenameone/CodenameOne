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

/// One configurable part of an [HttpSecurity]: form login, CSRF protection, the
/// authorization rules. The built-in parts are these, and so is anything an
/// application or a library adds with [HttpSecurity#with].
///
/// When the chain is built every configurer's [#init] runs, then every one's
/// [#configure]: `init` is where a part tells the others about itself -- an
/// entry point, a path that must stay open -- and `configure` is where it adds
/// its filters.
public abstract class SecurityConfigurer {
    private HttpSecurity builder;

    void attach(HttpSecurity http) {
        this.builder = http;
    }

    /// The [HttpSecurity] this configurer was applied to.
    protected final HttpSecurity getBuilder() {
        if (builder == null) {
            throw new IllegalStateException("This configurer has not been applied to an "
                    + "HttpSecurity");
        }
        return builder;
    }

    /// Shares what other parts need to know; nothing by default.
    public void init(HttpSecurity http) {
        // nothing to share
    }

    /// Adds this part's filters; nothing by default.
    public void configure(HttpSecurity http) {
        // nothing to add
    }

    /// Takes this part out of the chain: `http.csrf(csrf -> csrf.disable())`.
    public HttpSecurity disable() {
        HttpSecurity http = getBuilder();
        http.removeConfigurer(getClass());
        return http;
    }
}
