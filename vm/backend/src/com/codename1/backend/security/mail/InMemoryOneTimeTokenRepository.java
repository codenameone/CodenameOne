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

import com.codename1.backend.security.SecuritySchema;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// Tokens kept in this process: gone when it stops, and unknown to any other
/// server. For development and tests; see [JdbcOneTimeTokenRepository]
/// otherwise.
public final class InMemoryOneTimeTokenRepository implements OneTimeTokenRepository {
    private final Map<String, Entry> byHash = new HashMap<String, Entry>();

    private static final class Entry {
        final String purpose;
        final String userKey;
        final String username;
        final long expiresAt;

        Entry(String purpose, String userKey, String username, long expiresAt) {
            this.purpose = purpose;
            this.userKey = userKey;
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }

    @Override
    public synchronized void save(String tokenHash, String purpose, String username, long now,
            long expiresAt) {
        String user = SecuritySchema.usernameKey(username);
        for (Iterator<Entry> it = byHash.values().iterator(); it.hasNext();) {
            Entry e = it.next();
            if (e.expiresAt < now || e.userKey.equals(user) && e.purpose.equals(purpose)) {
                it.remove();
            }
        }
        byHash.put(tokenHash, new Entry(purpose, user, username, expiresAt));
    }

    @Override
    public synchronized String consume(String tokenHash, String purpose, long now) {
        Entry e = tokenHash == null ? null : byHash.get(tokenHash);
        if (e == null || !e.purpose.equals(purpose)) {
            return null;
        }
        byHash.remove(tokenHash);
        return e.expiresAt < now ? null : e.username;
    }

    @Override
    public synchronized void removeAll(String username, String purpose) {
        String user = SecuritySchema.usernameKey(username);
        for (Iterator<Entry> it = byHash.values().iterator(); it.hasNext();) {
            Entry e = it.next();
            if (e.userKey.equals(user) && e.purpose.equals(purpose)) {
                it.remove();
            }
        }
    }
}
