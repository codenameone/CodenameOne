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
package com.codename1.impl.backend.security;

import com.codename1.backend.security.AccessDeniedException;
import com.codename1.backend.security.AnonymousAuthenticationToken;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.InsufficientAuthenticationException;
import com.codename1.backend.security.RememberMeAuthenticationToken;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.impl.backend.BackendAccess;

/// What the woven check of a `@PreAuthorize`, `@Secured`, `@RolesAllowed` or
/// `@DenyAll` method calls. The build compiles the annotation into a Java
/// expression over these; nothing here reads an expression.
public final class MethodSecurity {
    private MethodSecurity() {
    }

    /// Who the calling thread's request is from: null when nothing put a
    /// context in place, the anonymous token when a chain did and nobody signed
    /// in.
    public static Authentication authentication() {
        return SecurityAccess.get().current();
    }

    /// Whether `authentication` signed in and holds any of `authorities`.
    public static boolean hasAnyAuthority(Authentication authentication, String[] authorities) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            String name = granted.getAuthority();
            if (name == null) {
                continue;
            }
            for (String wanted : authorities) {
                if (name.equals(wanted)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Nobody signed in.
    public static boolean isAnonymous(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    /// Somebody signed in, during this session or an earlier one.
    public static boolean isAuthenticated(Authentication authentication) {
        return !isAnonymous(authentication) && authentication.isAuthenticated();
    }

    /// The user was recognized by a remember-me cookie.
    public static boolean isRememberMe(Authentication authentication) {
        return authentication instanceof RememberMeAuthenticationToken;
    }

    /// Somebody signed in during this session.
    public static boolean isFullyAuthenticated(Authentication authentication) {
        return isAuthenticated(authentication) && !isRememberMe(authentication);
    }

    /// `authentication.name`; null when there is no authentication.
    public static String name(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    /// `principal`; null when there is no authentication.
    public static Object principal(Authentication authentication) {
        return authentication == null ? null : authentication.getPrincipal();
    }

    /// `principal.username`; null when the principal is not a
    /// [UserDetails], as the anonymous one is not.
    public static String username(Authentication authentication) {
        Object principal = principal(authentication);
        return principal instanceof UserDetails ? ((UserDetails) principal).getUsername() : null;
    }

    /// `a == b` in an expression. A null is equal to nothing, itself included:
    /// an absent argument must never match an absent name.
    public static boolean same(Object a, Object b) {
        return a != null && b != null && a.equals(b);
    }

    /// The refusal of a call to `method`: a caller who has not signed in is
    /// told to, anybody else is denied.
    public static RuntimeException denied(Authentication authentication, String method) {
        if (isAnonymous(authentication)) {
            return new InsufficientAuthenticationException("Full authentication is required to "
                    + "call " + method);
        }
        return new AccessDeniedException("Access Denied: " + method);
    }

    /// The bean the wiring of the calling thread's server registered under
    /// `name`, or null.
    public static Object bean(String name) {
        return BackendAccess.get().namedBean(name);
    }

    /// The failure of an expression whose bean is not there, or is not what the
    /// build compiled the call against.
    public static RuntimeException noBean(String name, String type, Object found) {
        if (found == null) {
            return new IllegalStateException("An authorization expression calls the bean \""
                    + name + "\" and this server has none: it is conditional and off, or the "
                    + "calling thread works for no server");
        }
        return new IllegalStateException("An authorization expression calls the bean \"" + name
                + "\" as a " + type + " and it is a " + found.getClass().getName());
    }
}
