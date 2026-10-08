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

import com.codename1.backend.Base64Url;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntity;
import com.codename1.backend.security.webauthn.WebAuthnAuthentication;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Keeps a sign-in with a passkey in the session as what it is: on a later
/// request the authentication is a [WebAuthnAuthentication] again, its
/// principal the user's [PublicKeyCredentialUserEntity], and it still says
/// which credential signed and whether the user was verified.
///
/// Installed by `http.webAuthn(...)`, and by nothing else.
final class WebAuthnAuthenticationCodec implements AuthenticationCodec {
    static final String KIND = "webauthn";

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public Map<String, Object> encode(Authentication authentication) {
        if (!(authentication instanceof WebAuthnAuthentication)) {
            return null;
        }
        WebAuthnAuthentication passkey = (WebAuthnAuthentication) authentication;
        PublicKeyCredentialUserEntity user = passkey.getPrincipal();
        Map<String, Object> kept = new LinkedHashMap<String, Object>();
        kept.put("userId", Base64Url.encode(user.getId()));
        kept.put("displayName", user.getDisplayName());
        byte[] credentialId = passkey.getCredentialId();
        if (credentialId != null) {
            kept.put("credentialId", Base64Url.encode(credentialId));
        }
        kept.put("userVerified", Boolean.valueOf(passkey.isUserVerified()));
        return kept;
    }

    @Override
    public Authentication decode(String name, List<GrantedAuthority> authorities, Map stored) {
        Object userId = stored.get("userId");
        Object displayName = stored.get("displayName");
        Object credentialId = stored.get("credentialId");
        byte[] handle = userId instanceof String ? Base64Url.decode((String) userId) : null;
        if (handle == null || handle.length == 0 || handle.length > 64) {
            return null;
        }
        return new WebAuthnAuthentication(new PublicKeyCredentialUserEntity(name, handle,
                displayName instanceof String ? (String) displayName : null), authorities,
                credentialId instanceof String ? Base64Url.decode((String) credentialId) : null,
                Boolean.TRUE.equals(stored.get("userVerified")));
    }
}
