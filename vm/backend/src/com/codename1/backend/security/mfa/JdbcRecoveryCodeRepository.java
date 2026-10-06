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

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.SecuritySchema;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/// Recovery codes kept in the server's database, in the
/// `cn1_mfa_recovery_code` table of [SecuritySchema]. A code is used up by one
/// `DELETE` that names it, and counted as used only when that removed exactly
/// one row.
public final class JdbcRecoveryCodeRepository implements RecoveryCodeRepository {
    private final DataSource dataSource;

    public JdbcRecoveryCodeRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    private static RuntimeException failed(Exception err) {
        if (err instanceof RuntimeException) {
            return (RuntimeException) err;
        }
        return new AuthenticationServiceException("The recovery code store could not be "
                + "reached: " + err.getMessage(), err);
    }

    @Override
    public void replace(String username, List<String> codeHashes) {
        try {
            dataSource.inTransaction(new Replace(SecuritySchema.usernameKey(username),
                    codeHashes));
        } catch (Exception err) {
            throw failed(err);
        }
    }

    /// Every code of a user swapped for new ones, as the body of a transaction.
    private static final class Replace implements DataSource.Work {
        private final String user;
        private final List<String> codeHashes;

        Replace(String user, List<String> codeHashes) {
            this.user = user;
            this.codeHashes = codeHashes;
        }

        @Override
        public Object run(Database db) throws Exception {
            db.execute("DELETE FROM cn1_mfa_recovery_code WHERE username_key = ?",
                    new Object[] {user});
            for (String hash : codeHashes) {
                db.execute("INSERT INTO cn1_mfa_recovery_code (username_key, code_hash) "
                        + "VALUES (?, ?)", new Object[] {user, hash});
            }
            return null;
        }
    }

    @Override
    public boolean consume(String username, String codeHash) {
        if (username == null || codeHash == null) {
            return false;
        }
        try {
            return dataSource.execute("DELETE FROM cn1_mfa_recovery_code WHERE username_key = ? "
                    + "AND code_hash = ?", new Object[] {SecuritySchema.usernameKey(username),
                        codeHash}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public int count(String username) {
        try {
            Map row = dataSource.queryOne("SELECT COUNT(*) AS n FROM cn1_mfa_recovery_code WHERE "
                    + "username_key = ?", new Object[] {SecuritySchema.usernameKey(username)});
            Object n = row == null ? null : row.get("n");
            return n instanceof Number ? ((Number) n).intValue() : 0;
        } catch (IOException err) {
            throw failed(err);
        }
    }
}
