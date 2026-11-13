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

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.SecuritySchema;
import java.io.IOException;
import java.util.Map;

/// Tokens kept in the server's database, in the `cn1_one_time_token` table of
/// [SecuritySchema]. A token is used up by one `DELETE` that names it, and
/// counted as used only when that removed exactly one row, so two servers
/// handed the same link let one of them through.
public final class JdbcOneTimeTokenRepository implements OneTimeTokenRepository {
    private static final String TABLE = "cn1_one_time_token";
    private final DataSource dataSource;

    public JdbcOneTimeTokenRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    private static RuntimeException failed(Exception err) {
        if (err instanceof RuntimeException) {
            return (RuntimeException) err;
        }
        return new AuthenticationServiceException("The one-time token store could not be "
                + "reached: " + err.getMessage(), err);
    }

    @Override
    public void save(String tokenHash, String purpose, String username, long now,
            long expiresAt) {
        try {
            dataSource.inTransaction(new Store(tokenHash, purpose, username, now, expiresAt));
        } catch (Exception err) {
            throw failed(err);
        }
    }

    /// A user's token for a purpose replaced by a new one, as the body of a
    /// transaction.
    private static final class Store implements DataSource.Work {
        private final String tokenHash;
        private final String purpose;
        private final String username;
        private final long now;
        private final long expiresAt;

        Store(String tokenHash, String purpose, String username, long now, long expiresAt) {
            this.tokenHash = tokenHash;
            this.purpose = purpose;
            this.username = username;
            this.now = now;
            this.expiresAt = expiresAt;
        }

        @Override
        public Object run(Database db) throws Exception {
            String table = SecuritySchema.table(TABLE);
            String user = SecuritySchema.usernameKey(username);
            db.execute("DELETE FROM " + table + " WHERE expires_at < ?",
                    new Object[] {Long.valueOf(now)});
            db.execute("DELETE FROM " + table + " WHERE username_key = ? AND purpose = ?",
                    new Object[] {user, purpose});
            db.execute("INSERT INTO " + table + " (token_hash, purpose, username_key, username, "
                    + "expires_at) VALUES (?, ?, ?, ?, ?)", new Object[] {tokenHash, purpose,
                        user, username, Long.valueOf(expiresAt)});
            return null;
        }
    }

    @Override
    public String consume(String tokenHash, String purpose, long now) {
        if (tokenHash == null || purpose == null) {
            return null;
        }
        try {
            String table = SecuritySchema.table(TABLE);
            Map row = dataSource.queryOne("SELECT username, expires_at FROM " + table
                    + " WHERE token_hash = ? AND purpose = ?", new Object[] {tokenHash, purpose});
            if (row == null) {
                return null;
            }
            // Only the removal decides, across server processes.
            if (dataSource.execute("DELETE FROM " + table + " WHERE token_hash = ? AND "
                    + "purpose = ?", new Object[] {tokenHash, purpose}) != 1) {
                return null;
            }
            Object expires = row.get("expires_at");
            Object username = row.get("username");
            if (!(expires instanceof Number) || ((Number) expires).longValue() < now
                    || !(username instanceof String)) {
                return null;
            }
            return (String) username;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public void removeAll(String username, String purpose) {
        try {
            dataSource.execute("DELETE FROM " + SecuritySchema.table(TABLE)
                    + " WHERE username_key = ? AND purpose = ?",
                    new Object[] {SecuritySchema.usernameKey(username), purpose});
        } catch (IOException err) {
            throw failed(err);
        }
    }
}
