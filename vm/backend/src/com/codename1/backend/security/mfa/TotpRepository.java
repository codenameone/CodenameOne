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

/// Where the secrets of users' authenticator apps are kept. A user has one;
/// names are compared without regard to the case of `A` to `Z`.
public interface TotpRepository {
    /// Stores a new, unconfirmed secret for `username`, replacing any other.
    void save(String username, byte[] secret);

    /// The credential of `username`, or null.
    TotpCredential find(String username);

    /// Marks the credential confirmed, if it is there and was not.
    ///
    /// @return whether this call confirmed it
    boolean confirm(String username);

    /// Records that a code of time step `step` was accepted, if no code of
    /// that step or a later one has been.
    ///
    /// The test and the change are one step, which is what makes a code good
    /// once: of two requests presenting the same code at the same moment, on
    /// one server or two, exactly one is told true.
    ///
    /// @return whether this call recorded it
    boolean advance(String username, long step);

    /// Forgets the credential.
    ///
    /// @return whether there was one
    boolean delete(String username);
}
