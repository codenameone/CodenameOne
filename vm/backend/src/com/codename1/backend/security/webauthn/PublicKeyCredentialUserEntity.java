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

/// A user as passkeys know them: the name they sign in with, and the user
/// handle -- random bytes that stand for the account inside an authenticator,
/// and that a sign-in without a user name hands back.
///
/// The handle is not the name and says nothing of it: an authenticator shows
/// it to nobody, but stores it where anyone holding the device may read it.
/// It is at most 64 bytes; this layer makes 32 random ones for a user who has
/// none.
///
/// It is the principal of a [WebAuthnAuthentication], so a controller may
/// take it:
///
/// ```java
/// @GetMapping("/me")
/// String me(@AuthenticationPrincipal PublicKeyCredentialUserEntity user) {
///     return user.getName();
/// }
/// ```
public final class PublicKeyCredentialUserEntity {
    private final String name;
    private final byte[] id;
    private final String displayName;

    /// @param name the user's name, as the application's user store has it
    /// @param id the user handle: 1 to 64 bytes
    /// @param displayName what an authenticator shows; the name when null
    public PublicKeyCredentialUserEntity(String name, byte[] id, String displayName) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A user needs a name");
        }
        if (id == null || id.length == 0 || id.length > 64) {
            throw new IllegalArgumentException("A user handle is 1 to 64 bytes");
        }
        this.name = name;
        this.id = id.clone();
        this.displayName = displayName == null || displayName.length() == 0 ? name : displayName;
    }

    public String getName() {
        return name;
    }

    /// The user handle.
    public byte[] getId() {
        return id.clone();
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return name;
    }
}
