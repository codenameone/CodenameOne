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

import com.codename1.backend.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/// The parts every [Authentication] shares: its authorities, its details and
/// the name read off its principal.
public abstract class AbstractAuthenticationToken implements Authentication {
    private final List<GrantedAuthority> authorities;
    private Object details;
    private boolean authenticated;

    /// A token with these authorities; null for none.
    protected AbstractAuthenticationToken(Collection<? extends GrantedAuthority> authorities) {
        List<GrantedAuthority> copy = new ArrayList<GrantedAuthority>();
        if (authorities != null) {
            for (GrantedAuthority authority : authorities) {
                if (authority == null) {
                    throw new IllegalArgumentException("Authorities collection cannot contain "
                            + "any null elements");
                }
                copy.add(authority);
            }
        }
        this.authorities = Collections.unmodifiableList(copy);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        Object principal = getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        if (principal instanceof Authentication) {
            return ((Authentication) principal).getName();
        }
        return principal == null ? "" : principal.toString();
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.authenticated = isAuthenticated;
    }

    @Override
    public Object getDetails() {
        return details;
    }

    /// Records something more about the request this token came from.
    public void setDetails(Object details) {
        this.details = details;
    }

    /// Drops the secret this token carried, once it has been checked. Called by
    /// [ProviderManager] on a token it accepted.
    public void eraseCredentials() {
        Object principal = getPrincipal();
        if (principal instanceof com.codename1.backend.security.core.userdetails.User) {
            ((com.codename1.backend.security.core.userdetails.User) principal).eraseCredentials();
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + " [Principal=" + getName() + ", Authenticated="
                + authenticated + ", Granted Authorities=" + authorities + "]";
    }
}
