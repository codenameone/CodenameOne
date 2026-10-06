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

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.crypto.NoOpPasswordEncoder;
import com.codename1.impl.backend.RequestSecurity;
import com.codename1.impl.backend.security.SecurityAccess;
import java.util.List;

/// This package's side of [SecurityAccess]: what the generated wiring, the
/// server and the test support reach that an application does not.
final class Access extends SecurityAccess {
    @Override
    public HttpSecurity httpSecurity(Config config, Object[] beans) {
        NoOpPasswordEncoder.setDevelopmentProfile(config != null && config.isDevelopmentProfile());
        return new HttpSecurity(config, beans);
    }

    @Override
    public RequestSecurity runtime(Config config, List chains, boolean tls) {
        return new FilterChainEngine(config, chains, tls);
    }

    @Override
    public Authentication authentication() {
        SecurityContext context = SecurityContextHolder.peek();
        Authentication authentication = context == null ? null : context.getAuthentication();
        return authentication instanceof AnonymousAuthenticationToken ? null : authentication;
    }

    @Override
    public Object principal() {
        SecurityContext context = SecurityContextHolder.peek();
        Authentication authentication = context == null ? null : context.getAuthentication();
        return authentication == null ? null : authentication.getPrincipal();
    }

    @Override
    public CsrfToken csrfToken(HttpServer.Request request) {
        return CsrfFilter.getToken(request);
    }

    @Override
    public SecurityContext testContext() {
        return SecurityContextHolder.testContext();
    }

    @Override
    public void testContext(SecurityContext context) {
        SecurityContextHolder.testContext(context);
    }

    @Override
    public String testCsrf(boolean valid) {
        String token = CsrfFilter.newTokenValue();
        CsrfFilter.testToken(token);
        return CsrfFilter.mask(valid ? token : CsrfFilter.newTokenValue());
    }

    @Override
    public void clearTestCsrf() {
        CsrfFilter.testToken(null);
    }
}
