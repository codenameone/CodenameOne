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

import java.util.List;

/// Where the passkeys users have registered are kept.
///
/// [InMemoryUserCredentialRepository] keeps them in this process, for a test
/// or a demonstration; [JdbcUserCredentialRepository] keeps them in the
/// server's database. Both do the two things the ceremonies depend on as one
/// step each, so that two requests at once cannot both pass:
///
/// - [#save] stores a credential only when no credential has its id, whoever
///   it belongs to;
/// - [#advance] moves the signature counter forward and never back.
public interface UserCredentialRepository {
    /// Stores a new credential.
    ///
    /// @return false, and nothing stored, when a credential with this id is
    /// there already -- this user's or another's
    boolean save(CredentialRecord record);

    /// The credential with this id, or null.
    CredentialRecord findByCredentialId(byte[] credentialId);

    /// The credentials of the user with this handle, oldest first; empty when
    /// they have none.
    List<CredentialRecord> findByUserId(byte[] userEntityUserId);

    /// Records a sign-in: the counter the authenticator sent, the flags it
    /// sent, and when.
    ///
    /// The counter is taken only when it is greater than the one stored -- or
    /// when both are zero, which is an authenticator that keeps no counter.
    /// One statement decides and stores, so of two requests with the same
    /// assertion one is refused.
    ///
    /// @return false, and nothing changed, when the counter did not advance or
    /// the credential is gone
    boolean advance(byte[] credentialId, long signatureCount, boolean uvInitialized,
                    boolean backupState, long lastUsed);

    /// Removes a credential.
    ///
    /// @return whether there was one
    boolean delete(byte[] credentialId);
}
