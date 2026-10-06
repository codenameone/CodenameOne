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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/// Remembered sign-ins kept in this process: gone when it stops, and unknown to
/// any other process of the same deployment. For development and for a server
/// that runs alone; see [JdbcTokenRepository] otherwise.
public final class InMemoryTokenRepositoryImpl implements PersistentTokenRepository {
    private final Map<String, PersistentRememberMeToken> bySeries =
            new HashMap<String, PersistentRememberMeToken>();

    @Override
    public synchronized void createNewToken(PersistentRememberMeToken token) {
        if (bySeries.containsKey(token.getSeries())) {
            throw new IllegalStateException("Series Id '" + token.getSeries()
                    + "' already exists!");
        }
        bySeries.put(token.getSeries(), token);
    }

    @Override
    public synchronized boolean updateToken(String series, String expectedTokenHash,
                                            String newTokenHash, long lastUsed) {
        PersistentRememberMeToken current = bySeries.get(series);
        if (current == null || !current.getTokenHash().equals(expectedTokenHash)) {
            return false;
        }
        bySeries.put(series, new PersistentRememberMeToken(current.getUsername(), series,
                newTokenHash, lastUsed));
        return true;
    }

    @Override
    public synchronized PersistentRememberMeToken getTokenForSeries(String series) {
        return series == null ? null : bySeries.get(series);
    }

    @Override
    public synchronized void removeToken(String series) {
        bySeries.remove(series);
    }

    @Override
    public synchronized void removeUserTokens(String username) {
        Iterator<PersistentRememberMeToken> all = bySeries.values().iterator();
        while (all.hasNext()) {
            if (all.next().getUsername().equalsIgnoreCase(username)) {
                all.remove();
            }
        }
    }

    /// How many series are stored.
    public synchronized int size() {
        return bySeries.size();
    }
}
