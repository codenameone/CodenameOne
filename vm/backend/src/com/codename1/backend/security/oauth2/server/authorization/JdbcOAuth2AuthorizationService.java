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

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.Json;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Grants kept in the server's database, in the `cn1_oauth2_authorization` and
/// `cn1_oauth2_token` tables of
/// [com.codename1.backend.security.SecuritySchema]: what lets tokens outlive a
/// restart, and several processes be one authorization server.
///
/// ```java
/// @Bean
/// OAuth2AuthorizationService authorizations(DataSource dataSource) {
///     return new JdbcOAuth2AuthorizationService(dataSource);
/// }
/// ```
///
/// A secret is one row, keyed by its SHA-256. Using one up is
///
/// ```sql
/// UPDATE cn1_oauth2_token SET used = 1, polled_at = ?
///  WHERE token_hash = ? AND kind = ? AND used = 0 AND expires_at > ?
/// ```
///
/// and the call that sees one row changed is the one that used it: the
/// database decides between two processes, and no lock is held here. The row
/// stays, marked, until it expires -- that is how a code or a refresh token
/// presented a second time is told from one that never existed.
public final class JdbcOAuth2AuthorizationService implements OAuth2AuthorizationService {
    private final DataSource dataSource;

    public JdbcOAuth2AuthorizationService(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    private static IllegalStateException failed(String what, Exception err) {
        return new IllegalStateException("The authorization store could not " + what + ": "
                + err.getMessage(), err);
    }

    @Override
    public void save(OAuth2Authorization a) {
        Object[] values = {a.getRegisteredClientId(), a.getPrincipalName(),
            a.getAuthorizationGrantType(), OAuth2Parameters.scopes(a.getScopes()), a.getStatus(),
            Json.write(a.getAttributes()), Long.valueOf(a.getExpiresAt()), a.getId()};
        try {
            // An update that changes nothing counts no row on MySQL, so "no row"
            // alone does not say the grant is new: it is asked for.
            if (dataSource.execute("UPDATE cn1_oauth2_authorization SET registered_client_id = ?, "
                    + "principal_name = ?, grant_type = ?, scopes = ?, status = ?, "
                    + "attributes = ?, expires_at = ? WHERE id = ?", values) == 0
                    && dataSource.queryOne("SELECT id FROM cn1_oauth2_authorization WHERE id = ?",
                            new Object[] {a.getId()}) == null) {
                dataSource.execute("INSERT INTO cn1_oauth2_authorization (registered_client_id, "
                        + "principal_name, grant_type, scopes, status, attributes, expires_at, "
                        + "id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {
                            values[0], values[1], values[2], values[3], values[4], values[5],
                            values[6], values[7], Long.valueOf(a.getCreatedAt())});
            }
        } catch (IOException err) {
            throw failed("store a grant", err);
        }
    }

    @Override
    public OAuth2Authorization findById(String id) {
        if (id == null) {
            return null;
        }
        try {
            Map row = dataSource.queryOne("SELECT id, registered_client_id, principal_name, "
                    + "grant_type, scopes, status, attributes, created_at, expires_at FROM "
                    + "cn1_oauth2_authorization WHERE id = ?", new Object[] {id});
            if (row == null) {
                return null;
            }
            Map<String, Object> attributes = new LinkedHashMap<String, Object>();
            Map parsed = Json.parseObject(String.valueOf(row.get("attributes")));
            for (Object entry : parsed.entrySet()) {
                Map.Entry e = (Map.Entry) entry;
                attributes.put(String.valueOf(e.getKey()), e.getValue());
            }
            return new OAuth2Authorization(String.valueOf(row.get("id")),
                    String.valueOf(row.get("registered_client_id")),
                    String.valueOf(row.get("principal_name")),
                    String.valueOf(row.get("grant_type")),
                    OAuth2Parameters.scopes(String.valueOf(row.get("scopes"))),
                    String.valueOf(row.get("status")), attributes, number(row.get("created_at")),
                    number(row.get("expires_at")));
        } catch (IOException err) {
            throw failed("read a grant", err);
        }
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    @Override
    public void remove(String id) {
        try {
            // The grant first: a secret whose grant is gone is worth nothing,
            // whichever of these another process sees half done.
            dataSource.execute("DELETE FROM cn1_oauth2_authorization WHERE id = ?",
                    new Object[] {id});
            dataSource.execute("DELETE FROM cn1_oauth2_token WHERE authorization_id = ?",
                    new Object[] {id});
        } catch (IOException err) {
            throw failed("remove a grant", err);
        }
    }

    @Override
    public void addToken(String authorizationId, String kind, String tokenHash, long expiresAt) {
        try {
            dataSource.execute("INSERT INTO cn1_oauth2_token (token_hash, authorization_id, "
                    + "kind, used, expires_at, polled_at) VALUES (?, ?, ?, 0, ?, 0)",
                    new Object[] {tokenHash, authorizationId, kind, Long.valueOf(expiresAt)});
        } catch (IOException err) {
            throw failed("store a token", err);
        }
    }

    @Override
    public boolean issueTokens(String authorizationId, long now, long expiresAt,
                               String refreshTokenHash, boolean reuse) {
        try {
            return Boolean.TRUE.equals(dataSource.inTransaction(
                    new Issuance(authorizationId, now, expiresAt, refreshTokenHash, reuse)));
        } catch (Exception err) {
            throw failed("issue tokens", err);
        }
    }

    /// The first write locks the grant until both token and grant updates commit.
    /// remove() deletes that row first, so either issuance finishes first or it
    /// sees no grant. No upsert can recreate a revoked grant.
    private static final class Issuance implements DataSource.Work {
        private final String id;
        private final long now;
        private final long expiresAt;
        private final String hash;
        private final boolean reuse;

        Issuance(String id, long now, long expiresAt, String hash, boolean reuse) {
            this.id = id;
            this.now = now;
            this.expiresAt = expiresAt;
            this.hash = hash;
            this.reuse = reuse;
        }

        @Override
        public Object run(Database db) throws Exception {
            Object[] grant = {id, OAuth2Authorization.ACTIVE, Long.valueOf(now)};
            // A portable row lock, including SQLite. MySQL may report zero for
            // this unchanged value, so existence is checked while holding it.
            db.execute("UPDATE cn1_oauth2_authorization SET expires_at = expires_at "
                    + "WHERE id = ? AND status = ? AND expires_at > ?", grant);
            if (db.queryOne("SELECT id FROM cn1_oauth2_authorization WHERE id = ? "
                    + "AND status = ? AND expires_at > ?", grant) == null) {
                return Boolean.FALSE;
            }
            if (hash != null) {
                if (reuse) {
                    Object[] token = {hash, id, REFRESH_TOKEN, Long.valueOf(now)};
                    db.execute("UPDATE cn1_oauth2_token SET expires_at = CASE WHEN expires_at < ? "
                            + "THEN ? ELSE expires_at END WHERE token_hash = ? AND authorization_id = ? "
                            + "AND kind = ? AND used = 0 AND expires_at > ?", new Object[] {
                                Long.valueOf(expiresAt), Long.valueOf(expiresAt), hash, id,
                                REFRESH_TOKEN, Long.valueOf(now)});
                    if (db.queryOne("SELECT token_hash FROM cn1_oauth2_token WHERE token_hash = ? "
                            + "AND authorization_id = ? AND kind = ? AND used = 0 AND expires_at > ?",
                            token) == null) {
                        return Boolean.FALSE;
                    }
                } else {
                    db.execute("INSERT INTO cn1_oauth2_token (token_hash, authorization_id, "
                            + "kind, used, expires_at, polled_at) VALUES (?, ?, ?, 0, ?, 0)",
                            new Object[] {hash, id, REFRESH_TOKEN, Long.valueOf(expiresAt)});
                }
            }
            db.execute("UPDATE cn1_oauth2_authorization SET expires_at = CASE WHEN expires_at < ? "
                    + "THEN ? ELSE expires_at END WHERE id = ?",
                    new Object[] {Long.valueOf(expiresAt), Long.valueOf(expiresAt), id});
            return Boolean.TRUE;
        }
    }

    @Override
    public StoredToken findToken(String kind, String tokenHash) {
        if (tokenHash == null) {
            return null;
        }
        try {
            Map row = dataSource.queryOne("SELECT authorization_id, used, expires_at, polled_at "
                    + "FROM cn1_oauth2_token WHERE token_hash = ? AND kind = ?",
                    new Object[] {tokenHash, kind});
            return row == null ? null : new StoredToken(String.valueOf(row.get("authorization_id")),
                    number(row.get("used")) != 0, number(row.get("expires_at")),
                    number(row.get("polled_at")));
        } catch (IOException err) {
            throw failed("read a token", err);
        }
    }

    @Override
    public boolean consumeToken(String kind, String tokenHash, long now) {
        if (tokenHash == null) {
            return false;
        }
        try {
            return dataSource.execute("UPDATE cn1_oauth2_token SET used = 1, polled_at = ? "
                    + "WHERE token_hash = ? AND kind = ? AND used = 0 AND expires_at > ?",
                    new Object[] {Long.valueOf(now), tokenHash, kind, Long.valueOf(now)}) == 1;
        } catch (IOException err) {
            throw failed("use a token", err);
        }
    }

    @Override
    public void touchToken(String kind, String tokenHash, long now) {
        try {
            dataSource.execute("UPDATE cn1_oauth2_token SET polled_at = ? WHERE token_hash = ? "
                    + "AND kind = ?", new Object[] {Long.valueOf(now), tokenHash, kind});
        } catch (IOException err) {
            throw failed("record a poll", err);
        }
    }

    @Override
    public boolean extendToken(String kind, String tokenHash, long now, long expiresAt) {
        try {
            return dataSource.execute("UPDATE cn1_oauth2_token SET expires_at = CASE WHEN expires_at < ? "
                    + "THEN ? ELSE expires_at END WHERE token_hash = ? AND kind = ? AND used = 0 "
                    + "AND expires_at > ?", new Object[] {Long.valueOf(expiresAt), Long.valueOf(expiresAt),
                        tokenHash, kind, Long.valueOf(now)}) == 1;
        } catch (IOException err) {
            throw failed("extend a token", err);
        }
    }

    @Override
    public boolean decide(String id, boolean approved, String principalName,
                          Map<String, Object> attributes) {
        OAuth2Authorization a = findById(id);
        if (a == null) {
            return false;
        }
        Map<String, Object> merged = new LinkedHashMap<String, Object>(a.getAttributes());
        if (attributes != null) {
            merged.putAll(attributes);
        }
        try {
            // The status in the condition is what makes one answer win; the
            // attributes written are this caller's reading with its own added,
            // and nothing else writes a pending grant's.
            return dataSource.execute("UPDATE cn1_oauth2_authorization SET status = ?, "
                    + "principal_name = ?, attributes = ? WHERE id = ? AND status = ?",
                    new Object[] {approved ? OAuth2Authorization.ACTIVE
                            : OAuth2Authorization.DENIED, approved ? principalName : "",
                        Json.write(merged), id, OAuth2Authorization.PENDING}) == 1;
        } catch (IOException err) {
            throw failed("answer a device grant", err);
        }
    }

    @Override
    public int purgeExpired(long now, int limit) {
        int bound = limit < 1 ? 1 : limit > 1000 ? 1000 : limit;
        int purged = 0;
        try {
            // Found by the index on expires_at, a bounded number at a time, and
            // deleted by key: a purge never holds more than one row.
            List tokens = dataSource.query("SELECT token_hash FROM cn1_oauth2_token WHERE "
                    + "expires_at < ? ORDER BY expires_at LIMIT " + bound,
                    new Object[] {Long.valueOf(now)});
            for (Object row : tokens) {
                purged += dataSource.execute("DELETE FROM cn1_oauth2_token WHERE token_hash = ? "
                        + "AND expires_at < ?", new Object[] {((Map) row).get("token_hash"),
                            Long.valueOf(now)});
            }
            List grants = dataSource.query("SELECT id FROM cn1_oauth2_authorization WHERE "
                    + "expires_at < ? ORDER BY expires_at LIMIT " + bound,
                    new Object[] {Long.valueOf(now)});
            for (Object row : grants) {
                Object id = ((Map) row).get("id");
                // Conditional again: a refresh may have moved the expiry since.
                if (dataSource.execute("DELETE FROM cn1_oauth2_authorization WHERE id = ? AND "
                        + "expires_at < ?", new Object[] {id, Long.valueOf(now)}) == 1) {
                    dataSource.execute("DELETE FROM cn1_oauth2_token WHERE authorization_id = ?",
                            new Object[] {id});
                    purged++;
                }
            }
        } catch (IOException err) {
            // Housekeeping: what could not be forgotten now is forgotten later.
            System.err.println("cn1: expired authorizations could not be purged: "
                    + err.getMessage());
        }
        return purged;
    }
}
