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
package com.codename1.backend.security.webauthn;

import com.codename1.backend.Base64Url;
import com.codename1.backend.DataSource;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.SecuritySchema;
import java.io.IOException;
import java.util.Map;

/// User handles kept in the server's database, in the `cn1_webauthn_user`
/// table of [SecuritySchema].
///
/// The table is keyed by the user's name and the handle is unique in it, so a
/// user has one handle and a handle one user, whatever arrives at once: the
/// first to store a name wins, and [#save] answers what the first stored.
public final class JdbcPublicKeyCredentialUserEntityRepository
        implements PublicKeyCredentialUserEntityRepository {
    private final DataSource dataSource;
    private Clock clock = Clock.SYSTEM;

    public JdbcPublicKeyCredentialUserEntityRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    /// The clock a row's creation time is read from; for tests.
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    private static RuntimeException failed(IOException err) {
        return new AuthenticationServiceException("The passkey store could not be reached: "
                + err.getMessage(), err);
    }

    private static PublicKeyCredentialUserEntity entity(Map row) throws IOException {
        if (row == null) {
            return null;
        }
        Object name = row.get("username");
        Object id = row.get("user_id");
        Object displayName = row.get("display_name");
        byte[] handle = id instanceof String ? Base64Url.decode((String) id) : null;
        if (!(name instanceof String) || handle == null || handle.length == 0
                || handle.length > 64) {
            throw new IOException("A stored passkey user has no name or no handle");
        }
        return new PublicKeyCredentialUserEntity((String) name, handle,
                displayName instanceof String ? (String) displayName : null);
    }

    private PublicKeyCredentialUserEntity byName(String username) throws IOException {
        return entity(dataSource.queryOne("SELECT username, user_id, display_name FROM "
                + "cn1_webauthn_user WHERE username_key = ?",
                new Object[] {SecuritySchema.usernameKey(username)}));
    }

    @Override
    public PublicKeyCredentialUserEntity findById(byte[] id) {
        if (id == null || id.length == 0) {
            return null;
        }
        try {
            return entity(dataSource.queryOne("SELECT username, user_id, display_name FROM "
                    + "cn1_webauthn_user WHERE user_id = ?",
                    new Object[] {Base64Url.encode(id)}));
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        if (username == null || username.length() == 0) {
            return null;
        }
        try {
            return byName(username);
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public PublicKeyCredentialUserEntity save(PublicKeyCredentialUserEntity user) {
        try {
            PublicKeyCredentialUserEntity first = byName(user.getName());
            if (first != null) {
                return first;
            }
            try {
                dataSource.execute("INSERT INTO cn1_webauthn_user (username_key, username, "
                        + "user_id, display_name, created_at) VALUES (?, ?, ?, ?, ?)",
                        new Object[] {SecuritySchema.usernameKey(user.getName()), user.getName(),
                            Base64Url.encode(user.getId()), user.getDisplayName(),
                            Long.valueOf(clock.currentTimeMillis())});
                return user;
            } catch (IOException raced) {
                // The key refused the row: another request stored this name
                // between the read and the insert, and its handle stands.
                first = byName(user.getName());
                if (first == null) {
                    throw raced;
                }
                return first;
            }
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public void delete(byte[] id) {
        if (id == null || id.length == 0) {
            return;
        }
        try {
            dataSource.execute("DELETE FROM cn1_webauthn_user WHERE user_id = ?",
                    new Object[] {Base64Url.encode(id)});
        } catch (IOException err) {
            throw failed(err);
        }
    }
}
