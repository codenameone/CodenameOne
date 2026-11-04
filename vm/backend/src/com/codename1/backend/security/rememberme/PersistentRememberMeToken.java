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
    private final String previousTokenHash;
    private final String userAgent;
    private final String ipAddress;

    /// @param tokenHash the SHA-256 of the token, in hex: the token itself is
    /// only ever in the cookie
    /// @param lastUsed epoch milliseconds
    public PersistentRememberMeToken(String username, String series, String tokenHash,
                                     long lastUsed) {
        this(username, series, tokenHash, lastUsed, null);
    }

    /// Includes the token replaced at `lastUsed`, for a bounded concurrent-use grace.
    public PersistentRememberMeToken(String username, String series, String tokenHash,
                                     long lastUsed, String previousTokenHash) {
        if (username == null || series == null || tokenHash == null) {
            throw new IllegalArgumentException("A token needs a user, a series and a hash");
        }
        this.username = username;
        this.series = series;
        this.tokenHash = tokenHash;
        this.lastUsed = lastUsed;
        this.previousTokenHash = previousTokenHash;
        this.userAgent = null;
        this.ipAddress = null;
    }

    /// A token with the device it was issued to, for a "signed-in devices" page.
    ///
    /// #### Parameters
    ///
    /// - `userAgent`: the `User-Agent` of the sign-in, or null
    /// - `ipAddress`: the client address of the sign-in, or null
    public PersistentRememberMeToken(String username, String series, String tokenHash,
                                     long lastUsed, String previousTokenHash, String userAgent,
                                     String ipAddress) {
        if (username == null || series == null || tokenHash == null) {
            throw new IllegalArgumentException("A token needs a user, a series and a hash");
        }
        this.username = username;
        this.series = series;
        this.tokenHash = tokenHash;
        this.lastUsed = lastUsed;
        this.previousTokenHash = previousTokenHash;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
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

    /// The hash replaced at [#getLastUsed()], or null before the first rotation.
    public String getPreviousTokenHash() {
        return previousTokenHash;
    }

    /// The `User-Agent` the series was issued to, or null when it was not recorded.
    public String getUserAgent() {
        return userAgent;
    }

    /// The client address the series was issued to, or null when it was not
    /// recorded. It is the address at sign-in; the series may since be used from
    /// another.
    public String getIpAddress() {
        return ipAddress;
    }
}
