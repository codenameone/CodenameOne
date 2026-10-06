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

import java.util.Collection;

/// Who a request is from when the user was recognized by a remember-me cookie
/// rather than signing in during this session.
///
/// It is authenticated, so `authenticated()` accepts it. `fullyAuthenticated()`
/// and `isFullyAuthenticated()` do not: what should ask for the password again
/// -- changing it, a payment -- uses those. A session that started from the
/// cookie keeps this type for as long as it lasts.
public class RememberMeAuthenticationToken extends AbstractAuthenticationToken {
    private final int keyHash;
    private final Object principal;

    /// @param key what identifies the services that made the token
    public RememberMeAuthenticationToken(String key, Object principal,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }
        if (principal == null || "".equals(principal)) {
            throw new IllegalArgumentException("principal cannot be null or empty");
        }
        this.keyHash = key.hashCode();
        this.principal = principal;
        super.setAuthenticated(true);
    }

    /// The hash of the key the token was made with.
    public int getKeyHash() {
        return keyHash;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
