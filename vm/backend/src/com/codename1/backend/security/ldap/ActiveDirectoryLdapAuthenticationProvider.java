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

import com.codename1.backend.security.AccountExpiredException;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.AuthenticationProvider;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.BadCredentialsException;
import com.codename1.backend.security.CredentialsExpiredException;
import com.codename1.backend.security.DisabledException;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.LockedException;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.UsernamePasswordAuthenticationToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Signs users in against Active Directory, as Spring's provider of the same
/// name: binds as `user@domain`, reads the user's entry by
/// `userPrincipalName`, and makes each group in `memberOf` an authority named
/// by the group's CN (`Domain Admins`, no prefix).
///
/// A refused bind is told apart by Active Directory's sub-error code: a locked
/// account is a [LockedException], an expired password a
/// [CredentialsExpiredException], a disabled account a [DisabledException],
/// an expired account an [AccountExpiredException]; the rest is a
/// [BadCredentialsException].
///
/// ```java
/// http.authenticationProvider(new ActiveDirectoryLdapAuthenticationProvider(
///         "corp.example", "ldaps://dc1.corp.example"));
/// ```
public final class ActiveDirectoryLdapAuthenticationProvider implements AuthenticationProvider {
    private final String domain;
    private final String rootDn;
    private final LdapContextSource contextSource;
    private String searchFilter = "(&(objectClass=user)(userPrincipalName={0}))";
    private boolean nestedGroups;

    public ActiveDirectoryLdapAuthenticationProvider(String domain, String url) {
        this(domain, url, null);
    }

    /// @param rootDn where users are searched; null for the domain's root
    /// (`corp.example` is `dc=corp,dc=example`)
    public ActiveDirectoryLdapAuthenticationProvider(String domain, String url, String rootDn) {
        this.domain = domain == null || domain.length() == 0 ? null : domain;
        this.rootDn = rootDn != null ? rootDn : this.domain == null ? "" : rootOf(this.domain);
        this.contextSource = new LdapContextSource(url);
        this.contextSource.setBase("");
    }

    /// The context source, for its timeouts and TLS settings.
    public LdapContextSource getContextSource() {
        return contextSource;
    }

    /// The user filter; `{0}` is `user@domain`, `{1}` the name as typed.
    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    /// Whether a user's authorities include the groups their groups are in.
    /// Active Directory's `memberOf` lists direct groups only; with this on,
    /// one more search asks the directory for every group that contains the
    /// user through any chain (`LDAP_MATCHING_RULE_IN_CHAIN`). Off by default,
    /// as in Spring, since on a large forest that search is not cheap.
    public void setSearchNestedGroups(boolean nestedGroups) {
        this.nestedGroups = nestedGroups;
    }

    private static String rootOf(String domain) {
        StringBuilder out = new StringBuilder();
        for (String part : LdapEncoder.split(domain, '.')) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append("dc=").append(LdapEncoder.nameEncode(part));
        }
        return out.toString();
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        String password = credentials instanceof String ? (String) credentials : null;
        if (username == null || username.length() == 0 || password == null
                || password.length() == 0) {
            throw new BadCredentialsException("Bad credentials");
        }
        String principal = username;
        if (domain != null) {
            int at = username.indexOf('@');
            if (at < 0) {
                principal = username + "@" + domain;
            } else if (!username.substring(at + 1).equalsIgnoreCase(domain)) {
                // A name in another domain would bind there through a forest
                // trust, signing in someone this provider was never told about.
                throw new BadCredentialsException("Bad credentials");
            }
        }
        LdapEntry entry;
        List<LdapEntry> nested = null;
        try (LdapConnection c = contextSource.connect()) {
            try {
                c.bind(principal, password);
            } catch (LdapException refused) {
                throw translate(refused);
            }
            List<LdapEntry> found = c.search(rootDn, LdapConnection.SCOPE_SUBTREE,
                    LdapEncoder.format(searchFilter, LdapEncoder.filterEncode(principal),
                            LdapEncoder.filterEncode(username)), 2, 0);
            if (found.size() != 1) {
                throw new BadCredentialsException(found.isEmpty()
                        ? "No directory entry for " + principal
                        : "More than one directory entry for " + principal);
            }
            entry = found.get(0);
            if (nestedGroups) {
                nested = c.search(rootDn, LdapConnection.SCOPE_SUBTREE,
                        "(&(objectClass=group)(member:1.2.840.113556.1.4.1941:="
                        + LdapEncoder.filterEncode(entry.getDn()) + "))", 0, 0, "cn");
            }
        } catch (IOException err) {
            throw new AuthenticationServiceException("Active Directory could not be asked: "
                    + err.getMessage(), err);
        }
        List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
        java.util.Set<String> names = new java.util.LinkedHashSet<String>();
        for (String group : entry.getValues("memberOf")) {
            String cn = firstValue(group);
            if (cn != null) {
                names.add(cn);
            }
        }
        if (nested != null) {
            for (LdapEntry group : nested) {
                String cn = firstValue(group.getDn());
                if (cn != null) {
                    names.add(cn);
                }
            }
        }
        for (String name : names) {
            roles.add(new SimpleGrantedAuthority(name));
        }
        return UsernamePasswordAuthenticationToken.authenticated(
                new LdapUserDetails(username, entry, roles), null, roles);
    }

    /// The value of a DN's first RDN, unescaped: `CN=Domain Admins,CN=Users,...`
    /// is `Domain Admins`.
    static String firstValue(String dn) {
        int eq = dn.indexOf('=');
        if (eq < 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (int i = eq + 1; i < dn.length(); i++) {
            char c = dn.charAt(i);
            if (c == '\\' && i + 1 < dn.length()) {
                out.append(dn.charAt(++i));
            } else if (c == ',' || c == '+') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// Active Directory's reason for a refused bind, from the `data XXX` in
    /// its diagnostic message.
    static RuntimeException translate(LdapException refused) {
        if (refused.getResultCode() != LdapException.INVALID_CREDENTIALS) {
            return new AuthenticationServiceException("Active Directory refused the bind: "
                    + refused.getMessage(), refused);
        }
        String d = refused.getDiagnosticMessage();
        int at = d.indexOf("data ");
        String code = at < 0 ? "" : d.substring(at + 5, Math.min(d.length(), at + 8));
        if ("775".equals(code)) {
            return new LockedException("The account is locked");
        }
        if ("532".equals(code) || "773".equals(code)) {
            return new CredentialsExpiredException("The password has expired");
        }
        if ("533".equals(code)) {
            return new DisabledException("The account is disabled");
        }
        if ("701".equals(code)) {
            return new AccountExpiredException("The account has expired");
        }
        return new BadCredentialsException("Bad credentials");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
