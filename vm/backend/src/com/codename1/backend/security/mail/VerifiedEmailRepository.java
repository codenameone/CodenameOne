/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.security.mail;

/// Where [EmailVerificationService] keeps each user's address and whether it
/// has been proved.
public interface VerifiedEmailRepository {
    /// Records `email` as the address `username` claims, not yet proved, in
    /// place of whatever was recorded -- a proved address included: a user
    /// has one address, and it is the one they gave last. An application
    /// that wants the old one to stand until the new one is proved keeps it
    /// in its own table and calls this when it lets go of it. `proof` is what [#markVerified] has to
    /// be given for this address: it stands for the one link that was mailed
    /// there, so a link mailed to an earlier address proves nothing about
    /// this one.
    void savePending(String username, String email, String proof);

    /// Marks the address `username` claims as proved at `now`, when `proof`
    /// is the one it was recorded with. False when the user claims none, or
    /// claims one recorded with another proof.
    boolean markVerified(String username, String proof, long now);

    /// The address `username` claims, proved or not, or null.
    String findEmail(String username);

    /// The address `username` has proved, or null when they claim none or
    /// have not proved the one they claim. One read: an address replaced
    /// between two reads would be answered as proved when it is not.
    String findVerifiedEmail(String username);

    /// Whether the address `username` claims has been proved.
    boolean isVerified(String username);

    /// Forgets `username`'s address.
    void delete(String username);
}
