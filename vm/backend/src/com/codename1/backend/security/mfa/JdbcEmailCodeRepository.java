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
package com.codename1.backend.security.mfa;

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.SecuritySchema;
import java.io.IOException;
import java.util.Map;

/// Mailed codes kept in the server's database, in the `cn1_mfa_email` table
/// of [SecuritySchema]. An attempt is counted, and a code used up, by one
/// `UPDATE` whose condition is the limit or the code itself, so servers
/// sharing the table cannot between them allow one attempt too many or take
/// one code twice.
public final class JdbcEmailCodeRepository implements EmailCodeRepository {
    private static final String TABLE = "cn1_mfa_email";
    private final DataSource dataSource;

    public JdbcEmailCodeRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    private static RuntimeException failed(Exception err) {
        if (err instanceof RuntimeException) {
            return (RuntimeException) err;
        }
        return new AuthenticationServiceException("The mailed code store could not be "
                + "reached: " + err.getMessage(), err);
    }

    private static EmailCode read(Map row) {
        if (row == null) {
            return null;
        }
        Object enabled = row.get("enabled");
        Object hash = row.get("code_hash");
        return new EmailCode(truth(enabled), hash instanceof String ? (String) hash : "",
                number(row.get("expires_at")), (int) number(row.get("attempts")),
                number(row.get("sent_at")));
    }

    private static boolean truth(Object value) {
        return value instanceof Boolean ? ((Boolean) value).booleanValue()
                : value instanceof Number && ((Number) value).longValue() != 0;
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    @Override
    public EmailCode find(String username) {
        if (username == null) {
            return null;
        }
        try {
            return read(dataSource.queryOne("SELECT enabled, code_hash, expires_at, attempts, "
                    + "sent_at FROM " + SecuritySchema.table(TABLE) + " WHERE username_key = ?",
                    new Object[] {SecuritySchema.usernameKey(username)}));
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public void setEnabled(String username, boolean enabled) {
        try {
            dataSource.inTransaction(new Store(SecuritySchema.usernameKey(username), enabled));
        } catch (Exception err) {
            throw failed(err);
        }
    }

    @Override
    public boolean saveCode(String username, String codeHash, long expiresAt, long sentAt,
            long sentBefore, int max) {
        try {
            // One statement, so of several callers one is answered. `attempts`
            // is set before `expires_at`: MySQL reads a column a SET has
            // already written as its new value, the others as its old one.
            return dataSource.execute("UPDATE " + SecuritySchema.table(TABLE)
                    + " SET attempts = CASE WHEN expires_at < ? THEN 0 ELSE attempts END,"
                    + " code_hash = ?, expires_at = ?, sent_at = ? WHERE username_key = ?"
                    + " AND enabled = ? AND (sent_at <= ? OR sent_at > ?)"
                    + " AND (attempts < ? OR expires_at < ?)",
                    new Object[] {Long.valueOf(sentAt), codeHash, Long.valueOf(expiresAt),
                        Long.valueOf(sentAt), SecuritySchema.usernameKey(username),
                        Integer.valueOf(1), Long.valueOf(sentBefore), Long.valueOf(sentAt),
                        Integer.valueOf(max), Long.valueOf(sentAt)}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    /// A user's row replaced, as the body of a transaction. Removed and
    /// written again rather than updated: an update that changes nothing
    /// counts no row on MySQL, which reads as a user with no row.
    private static final class Store implements DataSource.Work {
        private final String user;
        private final boolean enabled;

        Store(String user, boolean enabled) {
            this.user = user;
            this.enabled = enabled;
        }

        @Override
        public Object run(Database db) throws Exception {
            String table = SecuritySchema.table(TABLE);
            db.execute("DELETE FROM " + table + " WHERE username_key = ?", new Object[] {user});
            db.execute("INSERT INTO " + table + " (username_key, enabled, code_hash, "
                    + "expires_at, attempts, sent_at) VALUES (?, ?, '', 0, 0, 0)",
                    new Object[] {user, Integer.valueOf(enabled ? 1 : 0)});
            return null;
        }
    }

    @Override
    public boolean countAttempt(String username, int max) {
        try {
            return dataSource.execute("UPDATE " + SecuritySchema.table(TABLE)
                    + " SET attempts = attempts + 1 WHERE username_key = ? AND attempts < ?",
                    new Object[] {SecuritySchema.usernameKey(username), Integer.valueOf(max)})
                    == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public boolean clear(String username, String codeHash, boolean signedIn) {
        if (username == null || codeHash == null || codeHash.length() == 0) {
            return false;
        }
        try {
            return dataSource.execute("UPDATE " + SecuritySchema.table(TABLE)
                    + " SET code_hash = '', expires_at = 0"
                    + (signedIn ? ", sent_at = 0" : "") + " WHERE username_key = ? AND "
                    + "code_hash = ?", new Object[] {SecuritySchema.usernameKey(username),
                        codeHash}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }
}
