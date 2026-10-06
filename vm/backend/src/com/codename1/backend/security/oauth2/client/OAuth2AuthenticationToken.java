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

import com.codename1.backend.security.AbstractAuthenticationToken;
import com.codename1.backend.security.GrantedAuthority;
import java.util.Collection;

/// A user who signed in through an identity provider.
///
/// This is the authentication of the request that completed the sign-in. The
/// session keeps who signed in -- the name and the authorities -- and the
/// requests that follow see those, with the provider's id and the user's
/// attributes in `getDetails()` as a map.
public final class OAuth2AuthenticationToken extends AbstractAuthenticationToken {
    private final OAuth2User principal;
    private final String authorizedClientRegistrationId;

    public OAuth2AuthenticationToken(OAuth2User principal,
                                     Collection<? extends GrantedAuthority> authorities,
                                     String authorizedClientRegistrationId) {
        super(authorities);
        if (principal == null || authorizedClientRegistrationId == null) {
            throw new IllegalArgumentException("A principal and a registration id are required");
        }
        this.principal = principal;
        this.authorizedClientRegistrationId = authorizedClientRegistrationId;
        setAuthenticated(true);
    }

    @Override
    public OAuth2User getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public String getName() {
        return principal.getName();
    }

    /// The registration the user signed in through.
    public String getAuthorizedClientRegistrationId() {
        return authorizedClientRegistrationId;
    }
}
