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

/// Where each user's passkey handle is kept; see
/// [PublicKeyCredentialUserEntity].
///
/// A user has one handle for as long as they have the account: it is written
/// into every authenticator they register, and a sign-in without a user name
/// finds them by it. [InMemoryPublicKeyCredentialUserEntityRepository] keeps
/// them in this process; [JdbcPublicKeyCredentialUserEntityRepository] in the
/// server's database.
public interface PublicKeyCredentialUserEntityRepository {
    /// The user with this handle, or null.
    PublicKeyCredentialUserEntity findById(byte[] id);

    /// The user of this name, or null. Names differing only in the case of
    /// `A` to `Z` are the same user.
    PublicKeyCredentialUserEntity findByUsername(String username);

    /// Stores `user`, unless a user of that name is there already.
    ///
    /// @return the user of that name as stored: `user`, or the one that was
    /// there first -- whose handle is then the one to use
    PublicKeyCredentialUserEntity save(PublicKeyCredentialUserEntity user);

    /// Forgets the user with this handle. Their credentials are another
    /// repository's to remove.
    void delete(byte[] id);
}
