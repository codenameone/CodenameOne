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

import java.util.function.Supplier;

/// Grants access to an authenticated caller holding one of a set of authorities.
public final class AuthorityAuthorizationManager<T> implements AuthorizationManager<T> {
    private static final String ROLE_PREFIX = "ROLE_";

    private final String[] authorities;

    private AuthorityAuthorizationManager(String[] authorities) {
        this.authorities = authorities;
    }

    /// The caller has the role: `hasRole("ADMIN")` asks for `ROLE_ADMIN`.
    public static <T> AuthorityAuthorizationManager<T> hasRole(String role) {
        return hasAnyRole(role);
    }

    /// The caller has the authority, as it is written.
    public static <T> AuthorityAuthorizationManager<T> hasAuthority(String authority) {
        return hasAnyAuthority(authority);
    }

    /// The caller has any one of the roles.
    public static <T> AuthorityAuthorizationManager<T> hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            throw new IllegalArgumentException("roles cannot be empty");
        }
        String[] authorities = new String[roles.length];
        for (int iter = 0 ; iter < roles.length ; iter++) {
            String role = roles[iter];
            if (role == null || role.length() == 0) {
                throw new IllegalArgumentException("roles cannot contain null values");
            }
            if (role.startsWith(ROLE_PREFIX)) {
                throw new IllegalArgumentException(role + " should not start with " + ROLE_PREFIX
                        + " since " + ROLE_PREFIX + " is automatically prepended when using "
                        + "hasRole. Consider using hasAuthority instead.");
            }
            authorities[iter] = ROLE_PREFIX + role;
        }
        return new AuthorityAuthorizationManager<T>(authorities);
    }

    /// The caller has any one of the authorities.
    public static <T> AuthorityAuthorizationManager<T> hasAnyAuthority(String... authorities) {
        if (authorities == null || authorities.length == 0) {
            throw new IllegalArgumentException("authorities cannot be empty");
        }
        for (String authority : authorities) {
            if (authority == null || authority.length() == 0) {
                throw new IllegalArgumentException("authorities cannot contain null values");
            }
        }
        return new AuthorityAuthorizationManager<T>(authorities.clone());
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, T object) {
        Authentication current = authentication.get();
        if (current == null || !current.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        for (GrantedAuthority granted : current.getAuthorities()) {
            String name = granted.getAuthority();
            for (String wanted : authorities) {
                if (wanted.equals(name)) {
                    return new AuthorizationDecision(true);
                }
            }
        }
        return new AuthorizationDecision(false);
    }

    @Override
    public String toString() {
        return "AuthorityAuthorizationManager[authorities=" + java.util.Arrays.asList(authorities)
                + "]";
    }
}
