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
import com.codename1.backend.security.BadCredentialsException;
import java.io.IOException;
import java.util.List;

/// Checks a password by binding as the user: the directory itself judges it,
/// and its password policy (lockout, expiry) applies. The user's name is found
/// from DN patterns (`uid={0},ou=people`) or a [LdapUserSearch], as in Spring.
public final class BindAuthenticator implements LdapAuthenticator {
    private final LdapContextSource contextSource;
    private String[] userDnPatterns = new String[0];
    private LdapUserSearch userSearch;
    private String[] attributes = new String[0];

    public BindAuthenticator(LdapContextSource contextSource) {
        this.contextSource = contextSource;
    }

    /// Patterns relative to the base, `{0}` standing for the user name, tried in order.
    public void setUserDnPatterns(String... patterns) {
        this.userDnPatterns = patterns.clone();
    }

    public void setUserSearch(LdapUserSearch userSearch) {
        this.userSearch = userSearch;
    }

    /// The attributes read from the user's entry; all by default.
    public void setUserAttributes(String... attributes) {
        this.attributes = attributes.clone();
    }

    @Override
    public LdapEntry authenticate(String username, String password) {
        if (username == null || username.length() == 0) {
            throw new BadCredentialsException("Bad credentials");
        }
        if (password == null || password.length() == 0) {
            // Never reaches the directory: a bind with a name and no password
            // is an unauthenticated bind, which many servers accept.
            throw new BadCredentialsException("Empty password");
        }
        if (userDnPatterns.length == 0 && userSearch == null) {
            throw new AuthenticationServiceException("A BindAuthenticator needs user DN "
                    + "patterns or a user search");
        }
        try {
            for (String pattern : userDnPatterns) {
                String dn = contextSource.absolute(LdapEncoder.format(pattern,
                        LdapEncoder.nameEncode(username)));
                LdapEntry entry = bindAndRead(dn, password);
                if (entry != null) {
                    return entry;
                }
            }
            if (userSearch != null) {
                LdapEntry found = userSearch.searchForUser(username);
                if (found != null) {
                    LdapEntry entry = bindAndRead(found.getDn(), password);
                    if (entry != null) {
                        return entry;
                    }
                }
            }
        } catch (IOException err) {
            throw new AuthenticationServiceException("The directory could not be asked: "
                    + err.getMessage(), err);
        }
        throw new BadCredentialsException("Bad credentials");
    }

    /// The entry of `dn` read as that user, or null when the bind is refused.
    private LdapEntry bindAndRead(String dn, String password) throws IOException {
        try (LdapConnection c = contextSource.connect()) {
            try {
                c.bind(dn, password);
            } catch (LdapException refused) {
                if (refused.getResultCode() == LdapException.INVALID_CREDENTIALS
                        || refused.getResultCode() == LdapException.INVALID_DN_SYNTAX
                        || refused.getResultCode() == LdapException.NO_SUCH_OBJECT) {
                    return null;
                }
                throw refused;
            }
            List<LdapEntry> self = c.search(dn, LdapConnection.SCOPE_BASE, "(objectClass=*)", 1,
                    0, attributes);
            return self.isEmpty() ? new LdapEntry(dn) : self.get(0);
        }
    }
}
