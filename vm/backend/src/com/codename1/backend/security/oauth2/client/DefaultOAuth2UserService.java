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

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The user of a provider that is OAuth2 without OpenID Connect -- GitHub: its
/// attributes are what the provider's user info address answers the access
/// token with, and its name is the registration's name attribute among them.
///
/// The user has the authority `OAUTH2_USER`, and `SCOPE_x` for every scope
/// granted.
public final class DefaultOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        ClientRegistration registration = userRequest.getClientRegistration();
        ClientRegistration.ProviderDetails p = registration.getProviderDetails();
        if (p.getUserInfoUri() == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_user_info_uri"),
                    "The registration " + registration.getRegistrationId() + " has no "
                    + "userInfoUri to read the user from");
        }
        if (p.getUserNameAttributeName() == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "missing_user_name_attribute"), "The registration "
                    + registration.getRegistrationId() + " has no userNameAttributeName");
        }
        Map<String, Object> attributes = userInfo(registration, userRequest.getAccessToken());
        if (attributes.get(p.getUserNameAttributeName()) == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_USER_INFO_RESPONSE), "The user info of "
                    + registration.getRegistrationId() + " has no "
                    + p.getUserNameAttributeName());
        }
        return new DefaultOAuth2User(authorities("OAUTH2_USER", granted(userRequest)), attributes,
                p.getUserNameAttributeName());
    }

    /// The scopes the provider granted: what it said, or what was asked for
    /// when it said nothing.
    static Set<String> granted(OAuth2UserRequest request) {
        Set<String> scopes = request.getTokenResponse().getScopes();
        return scopes.isEmpty() ? request.getClientRegistration().getScopes() : scopes;
    }

    static List<GrantedAuthority> authorities(String kind, Set<String> scopes) {
        List<GrantedAuthority> out = new ArrayList<GrantedAuthority>();
        out.add(new SimpleGrantedAuthority(kind));
        for (String scope : scopes) {
            out.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        return out;
    }

    /// What the provider's user info address says of the bearer of
    /// `accessToken`.
    static Map<String, Object> userInfo(ClientRegistration registration, String accessToken) {
        Web.Result result;
        try {
            result = Web.getJson(registration.getProviderDetails().getUserInfoUri(), accessToken);
        } catch (IOException err) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_USER_INFO_RESPONSE), "The user info of "
                    + registration.getRegistrationId() + " could not be reached", err);
        }
        Map parsed = null;
        if (result.isSuccess() && result.getBodyAsString() != null) {
            try {
                parsed = Json.parseObject(result.getBodyAsString());
            } catch (IOException malformed) {
                parsed = null;
            } catch (RuntimeException malformed) {
                parsed = null;
            }
        }
        if (parsed == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_USER_INFO_RESPONSE), "The user info of "
                    + registration.getRegistrationId() + " answered " + result.getStatus());
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Object entry : parsed.entrySet()) {
            Map.Entry e = (Map.Entry) entry;
            if (e.getValue() != null) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        return out;
    }
}
