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
package com.codename1.backend.security.oauth2.client;

import com.codename1.backend.DataSource;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.SecuritySchema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Identities kept in the server's database, in the `cn1_federated_identity`
/// table of [SecuritySchema].
///
/// ```java
/// @Bean
/// FederatedIdentityRepository identities(DataSource dataSource) {
///     return new JdbcFederatedIdentityRepository(dataSource);
/// }
/// ```
///
/// The pair of provider and subject is the table's key, so of two processes
/// that tie the same identity at the same moment one inserts the row and the
/// other reads it: both answer the same user.
public final class JdbcFederatedIdentityRepository implements FederatedIdentityRepository {
    private final DataSource dataSource;
    private Clock clock = Clock.SYSTEM;

    public JdbcFederatedIdentityRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    /// The clock a link's creation time is read from; for tests.
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String findUsername(String provider, String subject) {
        try {
            return read(provider, subject);
        } catch (IOException err) {
            // A store that cannot be asked ties nobody to anybody: refusing the
            // sign-in is the safe answer, and making a second account is not.
            throw new IllegalStateException("The federated identities could not be read: "
                    + err.getMessage(), err);
        }
    }

    private String read(String provider, String subject) throws IOException {
        Map row = dataSource.queryOne("SELECT username FROM cn1_federated_identity WHERE "
                + "provider = ? AND subject = ?", new Object[] {provider, subject});
        return row == null ? null : String.valueOf(row.get("username"));
    }

    @Override
    public String link(String provider, String subject, String username) {
        try {
            String existing = read(provider, subject);
            if (existing != null) {
                return existing;
            }
            try {
                dataSource.execute("INSERT INTO cn1_federated_identity (provider, subject, "
                        + "username_key, username, created_at) VALUES (?, ?, ?, ?, ?)",
                        new Object[] {provider, subject, SecuritySchema.usernameKey(username),
                            username, Long.valueOf(clock.currentTimeMillis())});
                return username;
            } catch (IOException raced) {
                // The key refused the row: another request tied the identity
                // between the read and the insert, and its answer stands.
                existing = read(provider, subject);
                if (existing == null) {
                    throw raced;
                }
                return existing;
            }
        } catch (IOException err) {
            throw new IllegalStateException("The federated identity could not be stored: "
                    + err.getMessage(), err);
        }
    }

    @Override
    public boolean unlink(String provider, String subject) {
        try {
            return dataSource.execute("DELETE FROM cn1_federated_identity WHERE provider = ? "
                    + "AND subject = ?", new Object[] {provider, subject}) == 1;
        } catch (IOException err) {
            throw new IllegalStateException("The federated identity could not be removed: "
                    + err.getMessage(), err);
        }
    }

    @Override
    public List<String[]> findByUsername(String username) {
        List<String[]> out = new ArrayList<String[]>();
        try {
            for (Object row : dataSource.query("SELECT provider, subject FROM "
                    + "cn1_federated_identity WHERE username_key = ? ORDER BY created_at, "
                    + "provider, subject", new Object[] {SecuritySchema.usernameKey(username)})) {
                Map r = (Map) row;
                out.add(new String[] {String.valueOf(r.get("provider")),
                        String.valueOf(r.get("subject"))});
            }
        } catch (IOException err) {
            throw new IllegalStateException("The federated identities could not be read: "
                    + err.getMessage(), err);
        }
        return out;
    }
}
