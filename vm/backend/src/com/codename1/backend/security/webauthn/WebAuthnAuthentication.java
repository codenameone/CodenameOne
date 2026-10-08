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
package com.codename1.backend.security.webauthn;

import com.codename1.backend.security.AbstractAuthenticationToken;
import com.codename1.backend.security.GrantedAuthority;
import java.util.Collection;

/// Who signed in with a passkey.
///
/// The principal is the [PublicKeyCredentialUserEntity] of the user; the name
/// is the user's name, the one the application's user store knows them by,
/// and the authorities are the ones that store gives them. Beside those it
/// says which credential signed, and whether the authenticator verified the
/// user for it.
///
/// It stays one on the requests that follow: the session keeps what is here.
public final class WebAuthnAuthentication extends AbstractAuthenticationToken {
    private final PublicKeyCredentialUserEntity principal;
    private final byte[] credentialId;
    private final boolean userVerified;

    /// @param credentialId the credential that signed; may be null when it is
    /// not known
    public WebAuthnAuthentication(PublicKeyCredentialUserEntity principal,
                                  Collection<? extends GrantedAuthority> authorities,
                                  byte[] credentialId, boolean userVerified) {
        super(authorities);
        if (principal == null) {
            throw new IllegalArgumentException("principal cannot be null");
        }
        this.principal = principal;
        this.credentialId = credentialId == null ? null : credentialId.clone();
        this.userVerified = userVerified;
        setAuthenticated(true);
    }

    @Override
    public PublicKeyCredentialUserEntity getPrincipal() {
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

    /// The id of the credential that signed, or null.
    public byte[] getCredentialId() {
        return credentialId == null ? null : credentialId.clone();
    }

    /// Whether the authenticator verified the user -- a PIN, a fingerprint, a
    /// face -- so that the sign-in is two factors by itself.
    public boolean isUserVerified() {
        return userVerified;
    }
}
