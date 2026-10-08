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

/// Who a request is from: a credential presented for checking, or -- once an
/// [AuthenticationManager] has accepted it -- the principal and what it may do.
///
/// A controller receives the current one by declaring a parameter of this type;
/// anywhere else it is `SecurityContextHolder.getContext().getAuthentication()`.
public interface Authentication {
    /// What the principal has been granted; empty before authentication.
    Collection<? extends GrantedAuthority> getAuthorities();

    /// What proves the principal is who it says: a password, usually, and null
    /// once it has been checked.
    Object getCredentials();

    /// Anything else the mechanism recorded about the request, or null.
    Object getDetails();

    /// The identity: a username before authentication, and afterwards usually the
    /// [com.codename1.backend.security.core.userdetails.UserDetails] it was
    /// resolved to.
    Object getPrincipal();

    /// Whether this has been accepted. A token a client merely presented is not.
    boolean isAuthenticated();

    /// Marks the token trusted or not. Implementations refuse `true` from
    /// outside: a trusted token is made by a constructor that says so.
    void setAuthenticated(boolean isAuthenticated);

    /// The principal's name.
    String getName();
}
