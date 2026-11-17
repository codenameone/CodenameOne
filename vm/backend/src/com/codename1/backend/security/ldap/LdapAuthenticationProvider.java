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

import com.codename1.backend.Config;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.AuthenticationProvider;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.UsernamePasswordAuthenticationToken;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/// Signs users in against an LDAP directory, for form login and HTTP Basic
/// alike: an [LdapAuthenticator] checks the password, an
/// [LdapAuthoritiesPopulator] answers the roles.
///
/// ```java
/// LdapContextSource directory = new LdapContextSource("ldaps://ldap.corp.example/dc=corp,dc=example");
/// BindAuthenticator bind = new BindAuthenticator(directory);
/// bind.setUserDnPatterns("uid={0},ou=people");
/// http.authenticationProvider(new LdapAuthenticationProvider(bind,
///         new DefaultLdapAuthoritiesPopulator(directory, "ou=groups")))
///     .formLogin(Customizer.withDefaults());
/// ```
///
/// or from the configuration, [#fromConfig]. The principal of the
/// authentication is an [LdapUserDetails]. For Active Directory see
/// [ActiveDirectoryLdapAuthenticationProvider].
public final class LdapAuthenticationProvider implements AuthenticationProvider {
    private final LdapAuthenticator authenticator;
    private final LdapAuthoritiesPopulator authoritiesPopulator;

    public LdapAuthenticationProvider(LdapAuthenticator authenticator) {
        this(authenticator, null);
    }

    public LdapAuthenticationProvider(LdapAuthenticator authenticator,
                                      LdapAuthoritiesPopulator authoritiesPopulator) {
        this.authenticator = authenticator;
        this.authoritiesPopulator = authoritiesPopulator;
    }

    /// The provider the configuration describes, or null when `cn1.ldap.urls`
    /// is not set. Besides [LdapContextSource]'s settings:
    ///
    /// - `cn1.ldap.user-dn-patterns`: `uid={0},ou=people`, several separated by `;`
    /// - `cn1.ldap.user-search-base` and `cn1.ldap.user-search-filter`
    ///   (`(uid={0})` by default), used when no DN pattern is set
    /// - `cn1.ldap.group-search-base`: where the groups are; none, no roles
    ///   but `ROLE_USER`
    /// - `cn1.ldap.group-search-filter`: `(member={0})` by default
    /// - `cn1.ldap.group-role-attribute`: `cn` by default
    public static AuthenticationProvider fromConfig(Config config) throws IOException {
        LdapContextSource source = LdapContextSource.fromConfig(config);
        if (source == null) {
            return null;
        }
        String p = LdapContextSource.PREFIX;
        BindAuthenticator bind = new BindAuthenticator(source);
        String patterns = config.get(p + "user-dn-patterns");
        if (patterns != null && patterns.trim().length() > 0) {
            bind.setUserDnPatterns(LdapEncoder.split(patterns, ';'));
        } else {
            bind.setUserSearch(new FilterBasedLdapUserSearch(
                    config.get(p + "user-search-base", ""),
                    config.get(p + "user-search-filter", "(uid={0})"), source));
        }
        DefaultLdapAuthoritiesPopulator groups = new DefaultLdapAuthoritiesPopulator(source,
                config.get(p + "group-search-base"));
        groups.setGroupSearchFilter(config.get(p + "group-search-filter", "(member={0})"));
        groups.setGroupRoleAttribute(config.get(p + "group-role-attribute", "cn"));
        groups.setDefaultRole("ROLE_USER");
        return new LdapAuthenticationProvider(bind, groups);
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        String password = credentials instanceof String ? (String) credentials : null;
        LdapEntry entry = authenticator.authenticate(username, password);
        Collection<GrantedAuthority> roles = authoritiesPopulator == null
                ? Collections.<GrantedAuthority>emptyList()
                : authoritiesPopulator.getGrantedAuthorities(entry, username);
        LdapUserDetails user = new LdapUserDetails(username, entry, roles);
        return UsernamePasswordAuthenticationToken.authenticated(user, null, roles);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
