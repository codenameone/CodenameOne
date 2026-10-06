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
package com.codename1.backend.security.rememberme;

/// One remembered sign-in, as the server keeps it: whose it is, the series the
/// cookie names, the hash of the token the cookie must carry next, and when it
/// was last used.
public final class PersistentRememberMeToken {
    private final String username;
    private final String series;
    private final String tokenHash;
    private final long lastUsed;

    /// @param tokenHash the SHA-256 of the token, in hex: the token itself is
    /// only ever in the cookie
    /// @param lastUsed epoch milliseconds
    public PersistentRememberMeToken(String username, String series, String tokenHash,
                                     long lastUsed) {
        if (username == null || series == null || tokenHash == null) {
            throw new IllegalArgumentException("A token needs a user, a series and a hash");
        }
        this.username = username;
        this.series = series;
        this.tokenHash = tokenHash;
        this.lastUsed = lastUsed;
    }

    public String getUsername() {
        return username;
    }

    public String getSeries() {
        return series;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public long getLastUsed() {
        return lastUsed;
    }
}
