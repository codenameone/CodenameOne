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

import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SimpleGrantedAuthority;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/// Roles from the groups a user is a member of, as Spring's: the groups under
/// a search base whose `member` names the user, each group's `cn` becoming
/// `ROLE_` and its name in upper case. `{0}` in the filter is the user's DN,
/// `{1}` the user name.
public final class DefaultLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
    private final LdapContextSource contextSource;
    private final String groupSearchBase;
    private String groupSearchFilter = "(member={0})";
    private String groupRoleAttribute = "cn";
    private String rolePrefix = "ROLE_";
    private boolean convertToUpperCase = true;
    private boolean searchSubtree;
    private GrantedAuthority defaultRole;

    /// @param groupSearchBase relative to the base; null for no group search
    public DefaultLdapAuthoritiesPopulator(LdapContextSource contextSource,
                                           String groupSearchBase) {
        this.contextSource = contextSource;
        this.groupSearchBase = groupSearchBase;
    }

    public void setGroupSearchFilter(String filter) {
        this.groupSearchFilter = filter;
    }

    public void setGroupRoleAttribute(String attribute) {
        this.groupRoleAttribute = attribute;
    }

    public void setRolePrefix(String prefix) {
        this.rolePrefix = prefix == null ? "" : prefix;
    }

    public void setConvertToUpperCase(boolean convert) {
        this.convertToUpperCase = convert;
    }

    /// Whether to look below the group search base; false by default, as Spring's.
    public void setSearchSubtree(boolean searchSubtree) {
        this.searchSubtree = searchSubtree;
    }

    /// A role every user signed in this way has.
    public void setDefaultRole(String role) {
        this.defaultRole = role == null ? null : new SimpleGrantedAuthority(role);
    }

    @Override
    public Collection<GrantedAuthority> getGrantedAuthorities(LdapEntry user, String username) {
        Set<GrantedAuthority> roles = new LinkedHashSet<GrantedAuthority>();
        if (defaultRole != null) {
            roles.add(defaultRole);
        }
        if (groupSearchBase == null) {
            return new ArrayList<GrantedAuthority>(roles);
        }
        String filter = LdapEncoder.format(groupSearchFilter,
                LdapEncoder.filterEncode(user.getDn()), LdapEncoder.filterEncode(username));
        try (LdapConnection c = contextSource.readOnly()) {
            for (LdapEntry group : c.search(contextSource.absolute(groupSearchBase),
                    searchSubtree ? LdapConnection.SCOPE_SUBTREE : LdapConnection.SCOPE_ONE_LEVEL,
                    filter, 0, 0, groupRoleAttribute)) {
                for (String name : group.getValues(groupRoleAttribute)) {
                    roles.add(new SimpleGrantedAuthority(rolePrefix
                            + (convertToUpperCase ? LdapEncoder.asciiUpper(name) : name)));
                }
            }
        } catch (IOException err) {
            throw new AuthenticationServiceException("The groups of " + username
                    + " could not be read: " + err.getMessage(), err);
        }
        return new ArrayList<GrantedAuthority>(roles);
    }
}
