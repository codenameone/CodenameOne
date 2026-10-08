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

/// Sign-in with HTTP Basic credentials on each request.
///
/// ```java
/// http.httpBasic(basic -> basic.realmName("Orders API"));
/// ```
///
/// On a chain that also declares [HttpSecurity#mfa], a user who has a second
/// factor cannot sign in this way; see [#secondFactorExempt].
public final class HttpBasicConfigurer extends SecurityConfigurer {
    private final BasicAuthenticationEntryPoint basicEntryPoint = new BasicAuthenticationEntryPoint();
    private AuthenticationEntryPoint authenticationEntryPoint = basicEntryPoint;
    private boolean secondFactorExempt;

    HttpBasicConfigurer() {
    }

    /// The realm named in the challenge; `Realm` unless set.
    public HttpBasicConfigurer realmName(String realmName) {
        basicEntryPoint.setRealmName(realmName);
        return this;
    }

    /// What answers a request whose credentials are refused, and -- when this is
    /// the chain's only way in -- one that sent none.
    public HttpBasicConfigurer authenticationEntryPoint(AuthenticationEntryPoint entryPoint) {
        if (entryPoint == null) {
            throw new IllegalArgumentException("authenticationEntryPoint cannot be null");
        }
        this.authenticationEntryPoint = entryPoint;
        return this;
    }

    /// Lets a user who has a second factor in with their password alone.
    ///
    /// On a chain with [HttpSecurity#mfa], Basic credentials of a user who has
    /// a second factor are refused: they are a first factor sent with every
    /// request, with no step to present a code in, and accepting them would
    /// make the code optional for anyone who knows the password. Call this
    /// only when that is what is meant -- a chain whose Basic callers are
    /// scripts the user set up, on a network that is trusted.
    public HttpBasicConfigurer secondFactorExempt() {
        this.secondFactorExempt = true;
        return this;
    }

    @Override
    public void init(HttpSecurity http) {
        ExceptionHandlingConfigurer handling = http.getConfigurer(ExceptionHandlingConfigurer.class);
        if (handling != null) {
            handling.defaultAuthenticationEntryPointFor(authenticationEntryPoint,
                    RequestMatchers.header("X-Requested-With", "XMLHttpRequest"));
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilter(new BasicAuthenticationFilter(
                PasswordAuthentication.require(http, "httpBasic()"), authenticationEntryPoint,
                secondFactorExempt ? null : http.secondFactorPolicy()),
                HttpSecurity.ORDER_BASIC);
    }
}
