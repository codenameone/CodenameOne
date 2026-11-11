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

/// Where [OneTimeTokenService] keeps the tokens it has given out: never a
/// token, only its hash.
public interface OneTimeTokenRepository {
    /// Keeps a token for `username`, in place of any that user had for
    /// `purpose`: the newest link is the only one that works.
    ///
    /// #### Parameters
    ///
    /// - `tokenHash`: the token's hash
    /// - `purpose`: what the token may be used for
    /// - `username`: whose it is
    /// - `now`: the moment, epoch milliseconds; tokens that expired before it
    ///   may be dropped
    /// - `expiresAt`: when the token stops working, epoch milliseconds
    void save(String tokenHash, String purpose, String username, long now, long expiresAt);

    /// Uses a token up. Answers the user it was given to, or null when there
    /// is none for `purpose`, it has expired, or another caller used it first:
    /// of two callers with one token, one is answered.
    String consume(String tokenHash, String purpose, long now);

    /// Drops every token `username` has for `purpose`.
    void removeAll(String username, String purpose);
}
