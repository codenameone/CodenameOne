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
package com.codename1.backend.security.mfa;

/// What the server keeps of one user's authenticator app.
public final class TotpCredential {
    private final String username;
    private final byte[] secret;
    private final boolean confirmed;
    private final long lastUsedStep;

    /// @param confirmed whether the user has proved the app works, by entering
    /// a code from it; until then the secret is not a second factor
    /// @param lastUsedStep the time step of the last code accepted, or -1
    public TotpCredential(String username, byte[] secret, boolean confirmed, long lastUsedStep) {
        if (username == null || secret == null || secret.length == 0) {
            throw new IllegalArgumentException("A credential needs a user and a secret");
        }
        this.username = username;
        this.secret = secret.clone();
        this.confirmed = confirmed;
        this.lastUsedStep = lastUsedStep;
    }

    public String getUsername() {
        return username;
    }

    /// The shared secret.
    public byte[] getSecret() {
        return secret.clone();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public long getLastUsedStep() {
        return lastUsedStep;
    }
}
