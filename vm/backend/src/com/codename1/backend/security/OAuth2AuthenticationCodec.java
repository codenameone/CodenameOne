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
package com.codename1.backend.security;

import com.codename1.backend.security.oauth2.client.DefaultOAuth2User;
import com.codename1.backend.security.oauth2.client.DefaultOidcUser;
import com.codename1.backend.security.oauth2.client.OAuth2AuthenticationToken;
import com.codename1.backend.security.oauth2.client.OAuth2User;
import com.codename1.backend.security.oauth2.client.OidcUser;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Keeps a sign-in through another identity provider in the session as what
/// it is: on a later request the authentication is an
/// [OAuth2AuthenticationToken] again, with the registration it came through,
/// and its principal an [OAuth2User] with what the provider said of the user
/// -- an [OidcUser], with its ID token, when the provider spoke OpenID
/// Connect.
///
/// Installed by `http.oauth2Login(...)`, and by nothing else: a server that
/// signs nobody in elsewhere does not carry it.
///
/// Only what a session can store is kept of an attribute: text, numbers,
/// truth values, and lists and maps of them. A number may come back wider than
/// it went in.
final class OAuth2AuthenticationCodec implements AuthenticationCodec {
    static final String KIND = "oauth2";

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public Map<String, Object> encode(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return null;
        }
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User user = token.getPrincipal();
        Map<String, Object> kept = new LinkedHashMap<String, Object>();
        kept.put("registrationId", token.getAuthorizedClientRegistrationId());
        kept.put("attributes", plainMap(user.getAttributes(), 0));
        if (user instanceof OidcUser) {
            Jwt idToken = ((OidcUser) user).getIdToken();
            if (idToken != null) {
                Map<String, Object> jwt = new LinkedHashMap<String, Object>();
                jwt.put("value", idToken.getTokenValue());
                jwt.put("headers", plainMap(idToken.getHeaders(), 0));
                jwt.put("claims", plainMap(idToken.getClaims(), 0));
                kept.put("idToken", jwt);
            }
        }
        return kept;
    }

    @Override
    public Authentication decode(String name, List<GrantedAuthority> authorities, Map stored) {
        Object registrationId = stored.get("registrationId");
        Object attributes = stored.get("attributes");
        if (!(registrationId instanceof String) || ((String) registrationId).length() == 0
                || !(attributes instanceof Map)) {
            return null;
        }
        Map<String, Object> said = plainMap((Map) attributes, 0);
        OAuth2User user = null;
        Object idToken = stored.get("idToken");
        if (idToken instanceof Map) {
            Object value = ((Map) idToken).get("value");
            Object headers = ((Map) idToken).get("headers");
            Object claims = ((Map) idToken).get("claims");
            if (value instanceof String && ((String) value).length() > 0
                    && headers instanceof Map && !((Map) headers).isEmpty()
                    && claims instanceof Map) {
                user = new DefaultOidcUser(name, authorities, new Jwt((String) value,
                        plainMap((Map) headers, 0), plainMap((Map) claims, 0)), said);
            }
        }
        if (user == null) {
            user = new DefaultOAuth2User(name, authorities, said);
        }
        return new OAuth2AuthenticationToken(user, authorities, (String) registrationId);
    }

    private static Map<String, Object> plainMap(Map value, int depth) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Object entry : value.entrySet()) {
            Map.Entry e = (Map.Entry) entry;
            Object kept = plain(e.getValue(), depth + 1);
            if (e.getKey() instanceof String && kept != null) {
                out.put((String) e.getKey(), kept);
            }
        }
        return out;
    }

    /// `value` reduced to what a session stores, or null for what it cannot.
    private static Object plain(Object value, int depth) {
        if (value instanceof String || value instanceof Boolean || value instanceof Long
                || value instanceof Integer || value instanceof Double) {
            return value;
        }
        if (depth > 6) {
            return null;
        }
        if (value instanceof Map) {
            return plainMap((Map) value, depth);
        }
        if (value instanceof List) {
            List<Object> out = new ArrayList<Object>();
            for (Object element : (List) value) {
                Object kept = plain(element, depth + 1);
                if (kept != null) {
                    out.add(kept);
                }
            }
            return out;
        }
        return null;
    }
}
