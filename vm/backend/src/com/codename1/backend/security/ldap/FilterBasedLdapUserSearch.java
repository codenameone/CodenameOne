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

import java.io.IOException;
import java.util.List;

/// Finds a user by a filter under a search base, with the user name in place
/// of `{0}` -- escaped, so a name of `*` or `x)(uid=*` matches only itself.
///
/// ```java
/// new FilterBasedLdapUserSearch("ou=people", "(uid={0})", contextSource)
/// ```
public final class FilterBasedLdapUserSearch implements LdapUserSearch {
    private final String searchBase;
    private final String searchFilter;
    private final LdapContextSource contextSource;
    private boolean searchSubtree = true;
    private String[] attributes = new String[0];
    private int timeLimitSeconds;

    /// @param searchBase relative to the context source's base; empty for the base itself
    public FilterBasedLdapUserSearch(String searchBase, String searchFilter,
                                     LdapContextSource contextSource) {
        this.searchBase = searchBase == null ? "" : searchBase;
        this.searchFilter = searchFilter;
        this.contextSource = contextSource;
    }

    /// Whether to look below the search base too; true by default, as Spring's.
    public void setSearchSubtree(boolean searchSubtree) {
        this.searchSubtree = searchSubtree;
    }

    /// The attributes to read; all by default.
    public void setReturningAttributes(String... attributes) {
        this.attributes = attributes.clone();
    }

    public void setSearchTimeLimit(int seconds) {
        this.timeLimitSeconds = seconds;
    }

    @Override
    public LdapEntry searchForUser(String username) throws IOException {
        try (LdapConnection c = contextSource.readOnly()) {
            return search(c, username);
        }
    }

    LdapEntry search(LdapConnection c, String username) throws IOException {
        List<LdapEntry> found;
        try {
            found = c.search(contextSource.absolute(searchBase),
                    searchSubtree ? LdapConnection.SCOPE_SUBTREE : LdapConnection.SCOPE_ONE_LEVEL,
                    LdapEncoder.format(searchFilter, LdapEncoder.filterEncode(username)), 2,
                    timeLimitSeconds, attributes);
        } catch (LdapException e) {
            if (e.getResultCode() == LdapException.SIZE_LIMIT_EXCEEDED) {
                throw new IOException("More than one directory entry matches " + searchFilter
                        + " for " + username, e);
            }
            if (e.getResultCode() == LdapException.NO_SUCH_OBJECT) {
                return null;
            }
            throw e;
        }
        if (found.size() > 1) {
            throw new IOException("More than one directory entry matches " + searchFilter
                    + " for " + username);
        }
        return found.isEmpty() ? null : found.get(0);
    }
}
