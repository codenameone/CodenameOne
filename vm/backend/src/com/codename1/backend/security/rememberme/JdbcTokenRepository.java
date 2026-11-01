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

import com.codename1.backend.DataSource;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.SecuritySchema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Remembered sign-ins kept in the server's database, in the
/// `cn1_persistent_logins` table of [SecuritySchema], so that any process of a
/// deployment recognizes a cookie any other issued.
///
/// ```java
/// http.rememberMe(remember -> remember.tokenRepository(new JdbcTokenRepository(dataSource)));
/// ```
///
/// A token is replaced by one `UPDATE` that names the hash it expects to find,
/// and counted as replaced only when that changed exactly one row.
public final class JdbcTokenRepository implements PersistentTokenRepository {
    private final DataSource dataSource;

    public JdbcTokenRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    private static RuntimeException failed(IOException err) {
        return new AuthenticationServiceException("The remember-me store could not be reached: "
                + err.getMessage(), err);
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        try {
            dataSource.execute("INSERT INTO " + SecuritySchema.table("cn1_persistent_logins") + " (series, username_key, "
                    + "username, token_hash, last_used, previous_token_hash, user_agent, ip_address) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {
                        token.getSeries(), SecuritySchema.usernameKey(token.getUsername()),
                        token.getUsername(), token.getTokenHash(),
                        Long.valueOf(token.getLastUsed()), token.getPreviousTokenHash(),
                        token.getUserAgent(), token.getIpAddress()});
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public boolean updateToken(String series, String expectedTokenHash, String newTokenHash,
                               long lastUsed) {
        try {
            return dataSource.execute("UPDATE " + SecuritySchema.table("cn1_persistent_logins") + " SET previous_token_hash = ?, "
                    + "token_hash = ?, last_used = ? WHERE series = ? AND token_hash = ?", new Object[] {
                        expectedTokenHash, newTokenHash, Long.valueOf(lastUsed), series, expectedTokenHash}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String series) {
        if (series == null) {
            return null;
        }
        try {
            Map row = dataSource.queryOne("SELECT username, token_hash, last_used, previous_token_hash, user_agent, "
                    + "ip_address FROM "
                    + SecuritySchema.table("cn1_persistent_logins") + " WHERE series = ?", new Object[] {series});
            if (row == null) {
                return null;
            }
            return token(row, series);
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public void removeToken(String series) {
        try {
            dataSource.execute("DELETE FROM " + SecuritySchema.table("cn1_persistent_logins") + " WHERE series = ?",
                    new Object[] {series});
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public void removeUserTokens(String username) {
        try {
            dataSource.execute("DELETE FROM " + SecuritySchema.table("cn1_persistent_logins") + " WHERE username_key = ?",
                    new Object[] {SecuritySchema.usernameKey(username)});
        } catch (IOException err) {
            throw failed(err);
        }
    }

    /// Deletes the series last used more than `olderThanSeconds` ago: for a
    /// scheduled job, since a cookie nobody presents again leaves its row behind.
    ///
    /// @param now epoch milliseconds
    /// @return how many were deleted
    private static PersistentRememberMeToken token(Map row, String series) {
        Object used = row.get("last_used");
        return new PersistentRememberMeToken(String.valueOf(row.get("username")), series,
                String.valueOf(row.get("token_hash")),
                used instanceof Number ? ((Number) used).longValue() : 0L,
                text(row.get("previous_token_hash")), text(row.get("user_agent")),
                text(row.get("ip_address")));
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : null;
    }

    /// Every series of a user, most recently used first.
    @Override
    public List<PersistentRememberMeToken> getTokensForUser(String username) {
        List<PersistentRememberMeToken> out = new ArrayList<PersistentRememberMeToken>();
        if (username == null) {
            return out;
        }
        try {
            for (Object row : dataSource.query("SELECT series, username, token_hash, last_used, "
                    + "previous_token_hash, user_agent, ip_address FROM "
                    + SecuritySchema.table("cn1_persistent_logins") + " WHERE username_key = ? "
                    + "ORDER BY last_used DESC", new Object[] {SecuritySchema.usernameKey(username)})) {
                Map map = (Map) row;
                out.add(token(map, String.valueOf(map.get("series"))));
            }
            return out;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    /// Removes the series in one statement that names its owner, so it cannot
    /// remove another user's.
    @Override
    public boolean removeUserToken(String username, String series) {
        if (username == null || series == null) {
            return false;
        }
        try {
            return dataSource.execute("DELETE FROM " + SecuritySchema.table("cn1_persistent_logins")
                    + " WHERE series = ? AND username_key = ?", new Object[] {series,
                        SecuritySchema.usernameKey(username)}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    public int deleteExpired(long now, long olderThanSeconds) throws IOException {
        return dataSource.execute("DELETE FROM " + SecuritySchema.table("cn1_persistent_logins") + " WHERE last_used < ?",
                new Object[] {Long.valueOf(now - olderThanSeconds * 1000L)});
    }
}
