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

import java.util.List;

/// Where recovery codes are kept: as hashes, never as the codes.
public interface RecoveryCodeRepository {
    /// Replaces every code of `username` with these hashes.
    void replace(String username, List<String> codeHashes);

    /// A snapshot of the user's salted password hashes, for checking a presented code.
    /// An unknown user has an empty list. Consumption still goes through [#consume].
    List<String> findHashes(String username);

    /// Uses one code up.
    ///
    /// The test and the removal are one step: a code is good once, however
    /// many requests present it at the same moment.
    ///
    /// @return whether this call removed it
    boolean consume(String username, String codeHash);

    /// How many codes `username` has left.
    int count(String username);
}
