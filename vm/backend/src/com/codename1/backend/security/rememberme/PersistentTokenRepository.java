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

/// Where remembered sign-ins are kept.
public interface PersistentTokenRepository {
    /// Stores a new series.
    void createNewToken(PersistentRememberMeToken token);

    /// Replaces the token of `series`, if it is still `expectedTokenHash`.
    ///
    /// The test and the change are one step: of two requests presenting the
    /// same cookie at the same moment, one replaces the token and the other is
    /// told it did not.
    ///
    /// @return whether this call replaced it
    boolean updateToken(String series, String expectedTokenHash, String newTokenHash,
                        long lastUsed);

    /// The stored token of `series`, or null.
    PersistentRememberMeToken getTokenForSeries(String series);

    /// Forgets one series: one browser.
    void removeToken(String series);

    /// Forgets every series of a user: every browser they were remembered in.
    void removeUserTokens(String username);
}
