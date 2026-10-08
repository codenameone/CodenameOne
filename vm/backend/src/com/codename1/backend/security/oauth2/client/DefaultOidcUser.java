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
package com.codename1.backend.security.oauth2.client;

import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.util.Collection;
import java.util.Map;

/// An [OidcUser] that holds what it is given.
public class DefaultOidcUser extends DefaultOAuth2User implements OidcUser {
    private final Jwt idToken;

    /// @param claims the ID token's claims and the user info's, merged
    /// @param nameAttributeKey the claim that names the user; `sub` for null
    public DefaultOidcUser(Collection<? extends GrantedAuthority> authorities, Jwt idToken,
                           Map<String, Object> claims, String nameAttributeKey) {
        super(authorities, claims, nameAttributeKey == null ? "sub" : nameAttributeKey);
        this.idToken = idToken;
    }

    /// A user called `name`; see
    /// [DefaultOAuth2User#DefaultOAuth2User(String, Collection, Map)].
    public DefaultOidcUser(String name, Collection<? extends GrantedAuthority> authorities,
                           Jwt idToken, Map<String, Object> claims) {
        super(name, authorities, claims);
        this.idToken = idToken;
    }

    @Override
    public Jwt getIdToken() {
        return idToken;
    }

    @Override
    public Map<String, Object> getClaims() {
        return getAttributes();
    }

    @Override
    public String getSubject() {
        Object sub = getAttributes().get("sub");
        return sub instanceof String ? (String) sub : null;
    }

    @Override
    public String getEmail() {
        Object email = getAttributes().get("email");
        return email instanceof String ? (String) email : null;
    }

    @Override
    public boolean isEmailVerified() {
        Object verified = getAttributes().get("email_verified");
        // Apple writes it as text.
        return Boolean.TRUE.equals(verified) || "true".equals(verified);
    }
}
