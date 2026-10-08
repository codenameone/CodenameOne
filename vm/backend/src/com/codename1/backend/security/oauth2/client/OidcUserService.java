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

import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// The user of an OpenID Connect provider: the claims of the verified ID
/// token, and -- when the registration names a user info address and a scope
/// that has claims there was granted -- what that address adds to them.
///
/// The user info is believed only when its `sub` is the ID token's: without
/// that check a provider's answer about one user could be attached to another.
/// The user has the authority `OIDC_USER`, and `SCOPE_x` for every scope
/// granted.
public final class OidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        ClientRegistration registration = userRequest.getClientRegistration();
        Jwt idToken = userRequest.getIdToken();
        Map<String, Object> claims = new LinkedHashMap<String, Object>(idToken.getClaims());
        Set<String> granted = DefaultOAuth2UserService.granted(userRequest);
        if (registration.getProviderDetails().getUserInfoUri() != null
                && (granted.contains("profile") || granted.contains("email")
                        || granted.contains("address") || granted.contains("phone"))) {
            Map<String, Object> info = DefaultOAuth2UserService.userInfo(registration,
                    userRequest.getAccessToken());
            Object subject = info.get("sub");
            if (subject == null || !subject.equals(idToken.getSubject())) {
                throw new OAuth2AuthenticationException(new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_USER_INFO_RESPONSE), "The user info of "
                        + registration.getRegistrationId() + " is about another subject than "
                        + "the ID token");
            }
            // The ID token is signed and the user info is not: what both say,
            // the token says.
            for (Map.Entry<String, Object> entry : info.entrySet()) {
                if (!claims.containsKey(entry.getKey())) {
                    claims.put(entry.getKey(), entry.getValue());
                }
            }
        }
        String nameAttribute = registration.getProviderDetails().getUserNameAttributeName();
        if (nameAttribute == null || claims.get(nameAttribute) == null) {
            nameAttribute = "sub";
        }
        return new DefaultOidcUser(DefaultOAuth2UserService.authorities("OIDC_USER", granted),
                idToken, claims, nameAttribute);
    }
}
