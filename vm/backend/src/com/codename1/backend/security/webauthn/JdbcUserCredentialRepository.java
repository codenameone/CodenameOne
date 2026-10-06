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
import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.SecuritySchema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Credentials kept in the server's database, in the
/// `cn1_webauthn_credential` table of [SecuritySchema].
///
/// A row holds nothing secret: a public key verifies and cannot sign.
///
/// A credential's id is the table's key through its SHA-256, so that storing
/// one whose id is taken is refused by the key itself, whichever user has it
/// and however many requests try at once. The signature counter is moved by
/// one statement that names the condition it moves under, so the database
/// decides, and of two requests carrying the same assertion one changes no
/// row.
public final class JdbcUserCredentialRepository implements UserCredentialRepository {
    private static final String COLUMNS = "credential_id, user_id, algorithm, public_key, "
            + "sign_count, uv_initialized, backup_eligible, backup_state, transports, label, "
            + "created_at, last_used";

    private final DataSource dataSource;

    public JdbcUserCredentialRepository(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    private static RuntimeException failed(IOException err) {
        return new AuthenticationServiceException("The passkey store could not be reached: "
                + err.getMessage(), err);
    }

    /// The key of a credential: the SHA-256 of its id.
    private static String key(byte[] credentialId) {
        return Base64Url.encode(Crypto.sha256(credentialId));
    }

    private static Long flag(boolean value) {
        return Long.valueOf(value ? 1 : 0);
    }

    @Override
    public boolean save(CredentialRecord record) {
        StringBuilder transports = new StringBuilder();
        for (String transport : record.getTransports()) {
            transports.append(transports.length() == 0 ? "" : ",").append(transport);
        }
        Object[] values = {key(record.getCredentialId()),
            Base64Url.encode(record.getCredentialId()),
            Base64Url.encode(record.getUserEntityUserId()), Long.valueOf(record.getAlgorithm()),
            Base64Url.encode(record.getPublicKey()), Long.valueOf(record.getSignatureCount()),
            flag(record.isUvInitialized()), flag(record.isBackupEligible()),
            flag(record.isBackupState()), transports.toString(), record.getLabel(),
            Long.valueOf(record.getCreated()), Long.valueOf(record.getLastUsed())};
        try {
            dataSource.execute("INSERT INTO cn1_webauthn_credential (credential_key, " + COLUMNS
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", values);
            return true;
        } catch (IOException refused) {
            // The key refused the row, when a credential with this id is there:
            // that is the answer. Anything else is the store failing.
            try {
                if (read(record.getCredentialId()) != null) {
                    return false;
                }
            } catch (IOException err) {
                throw failed(err);
            }
            throw failed(refused);
        }
    }

    private CredentialRecord read(byte[] credentialId) throws IOException {
        Map row = dataSource.queryOne("SELECT " + COLUMNS + " FROM cn1_webauthn_credential "
                + "WHERE credential_key = ?", new Object[] {key(credentialId)});
        return row == null ? null : record(row);
    }

    @Override
    public CredentialRecord findByCredentialId(byte[] credentialId) {
        if (credentialId == null || credentialId.length == 0) {
            return null;
        }
        try {
            return read(credentialId);
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public List<CredentialRecord> findByUserId(byte[] userEntityUserId) {
        List<CredentialRecord> out = new ArrayList<CredentialRecord>();
        if (userEntityUserId == null || userEntityUserId.length == 0) {
            return out;
        }
        try {
            for (Object row : dataSource.query("SELECT " + COLUMNS + " FROM "
                    + "cn1_webauthn_credential WHERE user_id = ? ORDER BY created_at, "
                    + "credential_key", new Object[] {Base64Url.encode(userEntityUserId)})) {
                if (row instanceof Map) {
                    out.add(record((Map) row));
                }
            }
        } catch (IOException err) {
            throw failed(err);
        }
        return out;
    }

    @Override
    public boolean advance(byte[] credentialId, long signatureCount, boolean uvInitialized,
                           boolean backupState, long lastUsed) {
        Long count = Long.valueOf(signatureCount);
        try {
            // Forward; or an authenticator that keeps no counter, for which
            // both are zero. Which of the two is asked is the caller's count
            // to say, and the database decides either in the one statement.
            // (Written as two statements rather than one with "? = 0" in it:
            // PostgreSQL types a parameter compared with a literal as a 32-bit
            // integer, and a counter goes up to 4294967295.)
            String moved = signatureCount == 0 ? "sign_count = 0" : "sign_count < ?";
            Object[] values = signatureCount == 0
                    ? new Object[] {count, flag(uvInitialized), flag(backupState),
                        Long.valueOf(lastUsed), key(credentialId)}
                    : new Object[] {count, flag(uvInitialized), flag(backupState),
                        Long.valueOf(lastUsed), key(credentialId), count};
            return dataSource.execute("UPDATE cn1_webauthn_credential SET sign_count = ?, "
                    + "uv_initialized = ?, backup_state = ?, last_used = ? WHERE "
                    + "credential_key = ? AND " + moved, values) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public boolean delete(byte[] credentialId) {
        if (credentialId == null || credentialId.length == 0) {
            return false;
        }
        try {
            return dataSource.execute("DELETE FROM cn1_webauthn_credential WHERE "
                    + "credential_key = ?", new Object[] {key(credentialId)}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    private static CredentialRecord record(Map row) throws IOException {
        byte[] id = bytes(row, "credential_id");
        byte[] user = bytes(row, "user_id");
        byte[] publicKey = bytes(row, "public_key");
        List<String> transports = new ArrayList<String>();
        Object listed = row.get("transports");
        if (listed instanceof String) {
            String text = (String) listed;
            int from = 0;
            while (from < text.length()) {
                int comma = text.indexOf(',', from);
                int to = comma < 0 ? text.length() : comma;
                if (to > from) {
                    transports.add(text.substring(from, to));
                }
                from = to + 1;
            }
        }
        Object label = row.get("label");
        return CredentialRecord.builder().credentialId(id).userEntityUserId(user)
                .algorithm(number(row, "algorithm")).publicKey(publicKey)
                .signatureCount(number(row, "sign_count"))
                .uvInitialized(number(row, "uv_initialized") != 0)
                .backupEligible(number(row, "backup_eligible") != 0)
                .backupState(number(row, "backup_state") != 0).transports(transports)
                .label(label instanceof String ? (String) label : "")
                .created(number(row, "created_at")).lastUsed(number(row, "last_used")).build();
    }

    private static byte[] bytes(Map row, String column) throws IOException {
        Object value = row.get(column);
        byte[] decoded = value instanceof String ? Base64Url.decode((String) value) : null;
        if (decoded == null || decoded.length == 0) {
            throw new IOException("The " + column + " of a stored credential is not base64url");
        }
        return decoded;
    }

    private static long number(Map row, String column) throws IOException {
        Object value = row.get(column);
        if (!(value instanceof Number)) {
            throw new IOException("The " + column + " of a stored credential is not a number");
        }
        return ((Number) value).longValue();
    }
}
