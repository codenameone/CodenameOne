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

import java.util.Map;

/// Where an authorization server keeps the grants it has made and the secrets
/// it issued under them.
///
/// A secret -- an authorization code, a refresh token, a device code, a user
/// code -- is never stored: its SHA-256 is, see
/// [com.codename1.backend.security.oauth2.core.OAuth2Parameters#sha256]. And a
/// secret that works once is used up by [#consumeToken], which an
/// implementation must make one conditional statement on one row, never a read
/// followed by a write: several processes may share the store, and of two that
/// present the same code at the same moment exactly one may be told yes.
///
/// This is not the interface of Spring Authorization Server's service of the
/// same name, whose `save` and `findByToken` leave that atomicity to the
/// caller.
public interface OAuth2AuthorizationService {
    /// The kind of an authorization code.
    String CODE = "code";
    /// The kind of a refresh token.
    String REFRESH_TOKEN = "refresh_token";
    /// The kind of an RFC 8628 device code.
    String DEVICE_CODE = "device_code";
    /// The kind of an RFC 8628 user code.
    String USER_CODE = "user_code";

    /// One stored secret, as [OAuth2AuthorizationService#findToken] reports it.
    final class StoredToken {
        private final String authorizationId;
        private final boolean used;
        private final long expiresAt;
        private final long polledAt;

        public StoredToken(String authorizationId, boolean used, long expiresAt, long polledAt) {
            this.authorizationId = authorizationId;
            this.used = used;
            this.expiresAt = expiresAt;
            this.polledAt = polledAt;
        }

        /// The grant it was issued under.
        public String getAuthorizationId() {
            return authorizationId;
        }

        /// Whether it has been used up.
        public boolean isUsed() {
            return used;
        }

        /// Epoch milliseconds.
        public long getExpiresAt() {
            return expiresAt;
        }

        /// When it was used up, or [OAuth2AuthorizationService#touchToken]
        /// was last called for it; 0 when neither has happened.
        public long getPolledAt() {
            return polledAt;
        }
    }

    /// Stores `authorization`, in place of the one with its id if there is one.
    void save(OAuth2Authorization authorization);

    /// The grant with this id, or null: one that was removed is revoked.
    OAuth2Authorization findById(String id);

    /// Removes the grant and every secret issued under it.
    void remove(String id);

    /// Records a secret issued under a grant.
    ///
    /// @param kind [#CODE], [#REFRESH_TOKEN], [#DEVICE_CODE] or [#USER_CODE]
    /// @param tokenHash the SHA-256 of the secret
    /// @param expiresAt epoch milliseconds
    void addToken(String authorizationId, String kind, String tokenHash, long expiresAt);

    /// Issues tokens only while an active, unexpired grant still exists. Extending
    /// the grant and adding or extending its refresh token must be one atomic
    /// operation with respect to [#remove], including across server processes.
    /// A removed grant must never be recreated. Custom stores must implement this
    /// operation before issuing user tokens; the default fails closed.
    ///
    /// @param refreshTokenHash null when no refresh token is issued
    /// @param reuse whether the hash names an existing unused, unexpired refresh token
    /// @return false when the grant or reused refresh token is no longer valid
    default boolean issueTokens(String authorizationId, long now, long expiresAt,
                                String refreshTokenHash, boolean reuse) {
        throw new UnsupportedOperationException("Atomic token issuance is not supported by this store");
    }

    /// What is stored for a secret, whatever its state; null when nothing is.
    StoredToken findToken(String kind, String tokenHash);

    /// Uses a secret up, if it is there, unused and has not expired at `now`,
    /// and records `now` as when.
    ///
    /// @return whether THIS call used it up
    boolean consumeToken(String kind, String tokenHash, long now);

    /// Extends an unused, unexpired token to at least `expiresAt`, atomically.
    /// Returns false if it is missing, used or expired at `now`. Custom stores
    /// must implement this before enabling refresh token reuse.
    default boolean extendToken(String kind, String tokenHash, long now, long expiresAt) {
        throw new UnsupportedOperationException("Token expiry extension is not supported by this store");
    }

    /// Records that a secret was presented at `now`, without checking its
    /// previous timestamp. Use [#pollToken] to enforce a polling interval.
    void touchToken(String kind, String tokenHash, long now);

    /// Claims a polling interval on an unused, unexpired token. Checking the
    /// previous poll and recording `now` must be one atomic operation, including
    /// across processes sharing a database. Rejected polls do not claim an interval.
    /// Custom stores must implement this before enabling the device grant.
    ///
    /// @param intervalMillis the minimum time between accepted polls, greater than zero
    /// @return whether this call claimed the interval
    default boolean pollToken(String kind, String tokenHash, long now, long intervalMillis) {
        throw new UnsupportedOperationException("Atomic token polling is not supported by this store");
    }

    /// Answers a device grant that is [OAuth2Authorization#PENDING]: makes it
    /// [OAuth2Authorization#ACTIVE] for `principalName`, or
    /// [OAuth2Authorization#DENIED].
    ///
    /// @param attributes what to remember of the user who approved, merged
    /// into the grant's own
    /// @return whether THIS call answered it
    boolean decide(String id, boolean approved, String principalName,
                   Map<String, Object> attributes);

    /// Forgets up to `limit` grants and secrets that expired before `now`.
    ///
    /// @return how many were forgotten
    int purgeExpired(long now, int limit);
}
