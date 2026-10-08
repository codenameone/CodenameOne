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
package com.codename1.backend.security.oauth2.client;

import java.util.List;

/// Which local user each identity at a provider is: the pairs of a
/// registration's id and the provider's subject, each tied to one user name.
///
/// A provider's subject, not an email address, is what identifies a returning
/// user: an address can change hands, and a subject does not.
public interface FederatedIdentityRepository {
    /// The local user this identity is tied to, or null.
    String findUsername(String provider, String subject);

    /// Ties the identity to `username`, unless it is tied already.
    ///
    /// @return the user the identity is tied to once this returns: `username`,
    /// or the one another request tied it to first
    String link(String provider, String subject, String username);

    /// Unties the identity.
    ///
    /// @return whether it was tied
    boolean unlink(String provider, String subject);

    /// The identities tied to `username`, each as `{provider, subject}`.
    List<String[]> findByUsername(String username);
}
