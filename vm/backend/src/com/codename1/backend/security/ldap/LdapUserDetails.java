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
package com.codename1.backend.security.ldap;

import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/// The principal of a directory sign-in: the user name, the entry's DN, the
/// entry itself and the roles. Holds no password.
public final class LdapUserDetails implements UserDetails {
    private final String username;
    private final LdapEntry entry;
    private final List<GrantedAuthority> authorities;

    public LdapUserDetails(String username, LdapEntry entry,
                           Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.entry = entry;
        this.authorities = Collections.unmodifiableList(
                new ArrayList<GrantedAuthority>(authorities));
    }

    public String getDn() {
        return entry.getDn();
    }

    /// The user's directory entry, with the attributes the authenticator read.
    public LdapEntry getEntry() {
        return entry;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
