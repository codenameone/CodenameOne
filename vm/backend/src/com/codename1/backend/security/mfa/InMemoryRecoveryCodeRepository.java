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

import com.codename1.backend.security.SecuritySchema;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Recovery codes kept in this process: gone when it stops. For development
/// and tests; see [JdbcRecoveryCodeRepository] otherwise.
public final class InMemoryRecoveryCodeRepository implements RecoveryCodeRepository {
    private final Map<String, Set<String>> byUser = new HashMap<String, Set<String>>();

    @Override
    public synchronized void replace(String username, List<String> codeHashes) {
        byUser.put(SecuritySchema.usernameKey(username), new HashSet<String>(codeHashes));
    }

    @Override
    public synchronized List<String> findHashes(String username) {
        Set<String> codes = username == null ? null
                : byUser.get(SecuritySchema.usernameKey(username));
        return codes == null ? new ArrayList<String>() : new ArrayList<String>(codes);
    }

    @Override
    public synchronized boolean consume(String username, String codeHash) {
        Set<String> codes = username == null ? null
                : byUser.get(SecuritySchema.usernameKey(username));
        return codes != null && codes.remove(codeHash);
    }

    @Override
    public synchronized int count(String username) {
        Set<String> codes = username == null ? null
                : byUser.get(SecuritySchema.usernameKey(username));
        return codes == null ? 0 : codes.size();
    }
}
