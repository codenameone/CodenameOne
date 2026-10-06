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
package com.codename1.backend.security.oauth2.server.authorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Grants kept in this process: gone when it stops, and unknown to any other.
/// For development, tests and a server that runs as one process and can lose
/// its refresh tokens on a restart; see [JdbcOAuth2AuthorizationService].
public final class InMemoryOAuth2AuthorizationService implements OAuth2AuthorizationService {
    private final Map<String, OAuth2Authorization> authorizations =
            new HashMap<String, OAuth2Authorization>();
    /// kind, NUL, hash to {authorizationId, used, expiresAt, polledAt}.
    private final Map<String, Object[]> tokens = new LinkedHashMap<String, Object[]>();

    private static String key(String kind, String hash) {
        return kind + '\0' + hash;
    }

    @Override
    public synchronized void save(OAuth2Authorization authorization) {
        authorizations.put(authorization.getId(), authorization);
    }

    @Override
    public synchronized OAuth2Authorization findById(String id) {
        return id == null ? null : authorizations.get(id);
    }

    @Override
    public synchronized void remove(String id) {
        authorizations.remove(id);
        for (Iterator<Object[]> it = tokens.values().iterator() ; it.hasNext() ; ) {
            if (it.next()[0].equals(id)) {
                it.remove();
            }
        }
    }

    @Override
    public synchronized void addToken(String authorizationId, String kind, String tokenHash,
                                      long expiresAt) {
        tokens.put(key(kind, tokenHash), new Object[] {authorizationId, Boolean.FALSE,
                Long.valueOf(expiresAt), Long.valueOf(0)});
    }

    @Override
    public synchronized StoredToken findToken(String kind, String tokenHash) {
        Object[] row = tokenHash == null ? null : tokens.get(key(kind, tokenHash));
        return row == null ? null : new StoredToken((String) row[0],
                Boolean.TRUE.equals(row[1]), ((Long) row[2]).longValue(),
                ((Long) row[3]).longValue());
    }

    @Override
    public synchronized boolean consumeToken(String kind, String tokenHash, long now) {
        Object[] row = tokenHash == null ? null : tokens.get(key(kind, tokenHash));
        if (row == null || Boolean.TRUE.equals(row[1]) || ((Long) row[2]).longValue() <= now) {
            return false;
        }
        row[1] = Boolean.TRUE;
        row[3] = Long.valueOf(now);
        return true;
    }

    @Override
    public synchronized void touchToken(String kind, String tokenHash, long now) {
        Object[] row = tokenHash == null ? null : tokens.get(key(kind, tokenHash));
        if (row != null) {
            row[3] = Long.valueOf(now);
        }
    }

    @Override
    public synchronized boolean decide(String id, boolean approved, String principalName,
                                       Map<String, Object> attributes) {
        OAuth2Authorization a = authorizations.get(id);
        if (a == null || !OAuth2Authorization.PENDING.equals(a.getStatus())) {
            return false;
        }
        Map<String, Object> merged = new LinkedHashMap<String, Object>(a.getAttributes());
        if (attributes != null) {
            merged.putAll(attributes);
        }
        authorizations.put(id, new OAuth2Authorization(id, a.getRegisteredClientId(),
                approved ? principalName : "", a.getAuthorizationGrantType(), a.getScopes(),
                approved ? OAuth2Authorization.ACTIVE : OAuth2Authorization.DENIED, merged,
                a.getCreatedAt(), a.getExpiresAt()));
        return true;
    }

    @Override
    public synchronized int purgeExpired(long now, int limit) {
        int purged = 0;
        for (Iterator<Object[]> it = tokens.values().iterator() ; it.hasNext() && purged < limit ; ) {
            if (((Long) it.next()[2]).longValue() < now) {
                it.remove();
                purged++;
            }
        }
        List<String> expired = new ArrayList<String>();
        for (OAuth2Authorization a : authorizations.values()) {
            if (a.getExpiresAt() < now && purged + expired.size() < limit) {
                expired.add(a.getId());
            }
        }
        for (String id : expired) {
            remove(id);
            purged++;
        }
        return purged;
    }
}
