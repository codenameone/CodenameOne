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
package com.codename1.backend.security.apikey;

import com.codename1.backend.DataSource;
import com.codename1.backend.security.Clock;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// API keys kept in the server's database, in the `cn1_api_key` table of
/// [com.codename1.backend.security.SecuritySchema]. Only a key's hash is
/// stored; the key itself is shown once, by [ApiKeyGenerator], and never again.
///
/// ```java
/// @Bean
/// ApiKeyRepository apiKeys(DataSource dataSource) {
///     return new JdbcApiKeyRepository(dataSource);
/// }
/// ```
public final class JdbcApiKeyRepository implements ApiKeyRepository {
    private static final String COLUMNS = "key_hash, id, owner, scopes, prefix, last_four, revoked";
    private final DataSource dataSource;
    private Clock clock = Clock.SYSTEM;

    public JdbcApiKeyRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    /// The clock a key's creation time is read from; for tests.
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    private static ApiKey read(Map row) {
        List<String> scopes = new ArrayList<String>();
        String joined = String.valueOf(row.get("scopes"));
        int start = 0;
        while (start < joined.length()) {
            int space = joined.indexOf(' ', start);
            int end = space < 0 ? joined.length() : space;
            if (end > start) {
                scopes.add(joined.substring(start, end));
            }
            start = end + 1;
        }
        Object revoked = row.get("revoked");
        return new ApiKey(String.valueOf(row.get("id")), String.valueOf(row.get("owner")), scopes,
                String.valueOf(row.get("key_hash")), String.valueOf(row.get("prefix")),
                String.valueOf(row.get("last_four")),
                revoked instanceof Number && ((Number) revoked).longValue() != 0);
    }

    @Override
    public ApiKey findByHash(String hash) {
        if (hash == null) {
            return null;
        }
        try {
            Map row = dataSource.queryOne("SELECT " + COLUMNS + " FROM cn1_api_key WHERE "
                    + "key_hash = ?", new Object[] {hash});
            return row == null ? null : read(row);
        } catch (IOException err) {
            // A store that cannot be asked admits nobody; the request is refused
            // as one with an unknown key is.
            System.err.println("cn1: the API key store could not be read: " + err.getMessage());
            return null;
        }
    }

    /// Stores a new key. A scope may not contain a space.
    public void save(ApiKey key) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        StringBuilder scopes = new StringBuilder();
        for (String scope : key.getScopes()) {
            if (scope.indexOf(' ') >= 0 || scope.length() == 0) {
                throw new IllegalArgumentException("A scope is a word without spaces: " + scope);
            }
            scopes.append(scopes.length() == 0 ? "" : " ").append(scope);
        }
        dataSource.execute("INSERT INTO cn1_api_key (key_hash, id, owner, scopes, prefix, "
                + "last_four, revoked, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {
                    key.getHash(), key.getId(), key.getOwner(), scopes.toString(), key.getPrefix(),
                    key.getLastFour(), Integer.valueOf(key.isRevoked() ? 1 : 0),
                    Long.valueOf(clock.currentTimeMillis())});
    }

    /// Revokes the key with this id.
    ///
    /// @return whether this call revoked it: false when there is no such key, or
    /// it was revoked already
    public boolean revoke(String id) throws IOException {
        return dataSource.execute("UPDATE cn1_api_key SET revoked = 1 WHERE id = ? AND "
                + "revoked = 0", new Object[] {id}) == 1;
    }

    /// Forgets the key with this id altogether.
    public boolean delete(String id) throws IOException {
        return dataSource.execute("DELETE FROM cn1_api_key WHERE id = ?", new Object[] {id}) == 1;
    }

    /// The keys of one owner, oldest first.
    public List<ApiKey> findByOwner(String owner) throws IOException {
        List<ApiKey> out = new ArrayList<ApiKey>();
        for (Object row : dataSource.query("SELECT " + COLUMNS + " FROM cn1_api_key WHERE "
                + "owner = ? ORDER BY created_at, id", new Object[] {owner})) {
            out.add(read((Map) row));
        }
        return out;
    }
}
