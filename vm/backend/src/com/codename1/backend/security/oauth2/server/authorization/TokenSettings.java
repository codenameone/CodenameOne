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
package com.codename1.backend.security.oauth2.server.authorization;

/// How long what is issued to one client lasts, in seconds.
///
/// | | Unless set |
/// |---|---|
/// | authorization code | 5 minutes |
/// | access token | 5 minutes |
/// | ID token | 30 minutes |
/// | refresh token | 60 minutes from its last use |
/// | device code | 5 minutes |
///
/// A refresh token is replaced every time it is used unless
/// [#isReuseRefreshTokens] says otherwise, and using one that was replaced
/// revokes the whole grant.
public final class TokenSettings {
    private final long authorizationCodeTimeToLive;
    private final long accessTokenTimeToLive;
    private final long idTokenTimeToLive;
    private final long refreshTokenTimeToLive;
    private final long deviceCodeTimeToLive;
    private final boolean reuseRefreshTokens;

    private TokenSettings(Builder b) {
        this.authorizationCodeTimeToLive = b.authorizationCodeTimeToLive;
        this.accessTokenTimeToLive = b.accessTokenTimeToLive;
        this.idTokenTimeToLive = b.idTokenTimeToLive;
        this.refreshTokenTimeToLive = b.refreshTokenTimeToLive;
        this.deviceCodeTimeToLive = b.deviceCodeTimeToLive;
        this.reuseRefreshTokens = b.reuseRefreshTokens;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getAuthorizationCodeTimeToLive() {
        return authorizationCodeTimeToLive;
    }

    public long getAccessTokenTimeToLive() {
        return accessTokenTimeToLive;
    }

    public long getIdTokenTimeToLive() {
        return idTokenTimeToLive;
    }

    public long getRefreshTokenTimeToLive() {
        return refreshTokenTimeToLive;
    }

    public long getDeviceCodeTimeToLive() {
        return deviceCodeTimeToLive;
    }

    /// Whether a refresh token stays the same when it is used. False unless
    /// set: a token that is replaced on every use lets a stolen one be noticed.
    public boolean isReuseRefreshTokens() {
        return reuseRefreshTokens;
    }

    /// Builds a [TokenSettings]. Every time is in seconds, and positive.
    public static final class Builder {
        private long authorizationCodeTimeToLive = 300;
        private long accessTokenTimeToLive = 300;
        private long idTokenTimeToLive = 1800;
        private long refreshTokenTimeToLive = 3600;
        private long deviceCodeTimeToLive = 300;
        private boolean reuseRefreshTokens;

        Builder() {
        }

        private static long positive(long seconds, String what) {
            if (seconds <= 0) {
                throw new IllegalArgumentException(what + " must be greater than 0 seconds");
            }
            return seconds;
        }

        public Builder authorizationCodeTimeToLive(long seconds) {
            this.authorizationCodeTimeToLive = positive(seconds, "authorizationCodeTimeToLive");
            return this;
        }

        public Builder accessTokenTimeToLive(long seconds) {
            this.accessTokenTimeToLive = positive(seconds, "accessTokenTimeToLive");
            return this;
        }

        public Builder idTokenTimeToLive(long seconds) {
            this.idTokenTimeToLive = positive(seconds, "idTokenTimeToLive");
            return this;
        }

        public Builder refreshTokenTimeToLive(long seconds) {
            this.refreshTokenTimeToLive = positive(seconds, "refreshTokenTimeToLive");
            return this;
        }

        public Builder deviceCodeTimeToLive(long seconds) {
            this.deviceCodeTimeToLive = positive(seconds, "deviceCodeTimeToLive");
            return this;
        }

        public Builder reuseRefreshTokens(boolean reuseRefreshTokens) {
            this.reuseRefreshTokens = reuseRefreshTokens;
            return this;
        }

        public TokenSettings build() {
            return new TokenSettings(this);
        }
    }
}
