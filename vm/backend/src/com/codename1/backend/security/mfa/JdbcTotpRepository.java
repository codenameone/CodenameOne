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

import com.codename1.backend.Base64;
import com.codename1.backend.Config;
import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.SecuritySchema;
import java.io.IOException;
import java.util.Map;

/// Secrets kept in the server's database, in the `cn1_mfa_totp` table of
/// [SecuritySchema].
///
/// A secret is what an attacker needs to produce a user's codes for good, so
/// it is not stored as it is: each is sealed with AES-GCM under a key the
/// database does not hold, with a nonce of its own stored beside it and the
/// user's name bound in, so a row copied onto another user does not open. Give
/// the key in the configuration, as 32 bytes in base64:
///
/// ```
/// cn1.security.mfa.encryptionKey=...     # openssl rand -base64 32
/// ```
///
/// and build the repository with [#fromConfig]. Losing the key loses every
/// enrolment; changing it does the same.
public final class JdbcTotpRepository implements TotpRepository {
    /// The setting that holds the key.
    public static final String ENCRYPTION_KEY = "cn1.security.mfa.encryptionKey";

    private final DataSource dataSource;
    private final byte[] key;
    private Clock clock = Clock.SYSTEM;

    /// @param encryptionKey 32 bytes
    public JdbcTotpRepository(DataSource dataSource, byte[] encryptionKey) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        if (encryptionKey == null || encryptionKey.length != 32) {
            throw new IllegalArgumentException("The encryption key is 32 bytes");
        }
        this.dataSource = dataSource;
        this.key = encryptionKey.clone();
    }

    /// A repository whose key is the configuration's [#ENCRYPTION_KEY].
    ///
    /// On a development profile a server without the setting gets a fixed key
    /// and says so, so that a laptop needs no secret; anywhere else the setting
    /// is required and the server does not start without it.
    public static JdbcTotpRepository fromConfig(DataSource dataSource, Config config) {
        String encoded;
        try {
            encoded = config.get(ENCRYPTION_KEY);
        } catch (IOException err) {
            throw new IllegalStateException(ENCRYPTION_KEY + ": " + err.getMessage(), err);
        }
        if (encoded == null || encoded.trim().length() == 0) {
            if (!config.isDevelopmentProfile()) {
                throw new IllegalStateException(ENCRYPTION_KEY + " is not set. The secrets of "
                        + "users' authenticator apps are encrypted under it before they are "
                        + "stored; set it to 32 random bytes in base64 (openssl rand -base64 32) "
                        + "and keep it out of the database.");
            }
            System.err.println("cn1: " + ENCRYPTION_KEY + " is not set; on this development "
                    + "profile authenticator secrets are sealed under a fixed, public key");
            return new JdbcTotpRepository(dataSource, Crypto.sha256(utf8(
                    "cn1 development profile mfa key")));
        }
        byte[] decoded;
        try {
            decoded = Base64.decode(encoded.trim());
        } catch (RuntimeException malformed) {
            decoded = null;
        }
        if (decoded == null || decoded.length != 32) {
            throw new IllegalStateException(ENCRYPTION_KEY + " must be 32 bytes in base64, and "
                    + "is " + (decoded == null ? "not base64" : decoded.length + " bytes"));
        }
        return new JdbcTotpRepository(dataSource, decoded);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }

    private static RuntimeException failed(Exception err) {
        if (err instanceof RuntimeException) {
            return (RuntimeException) err;
        }
        return new AuthenticationServiceException("The second-factor store could not be "
                + "reached: " + err.getMessage(), err);
    }

    @Override
    public void save(String username, byte[] secret) {
        String user = SecuritySchema.usernameKey(username);
        try {
            byte[] nonce = Crypto.randomBytes(12);
            String sealed = Base64.encode(Crypto.aesGcmEncrypt(key, nonce, utf8(user), secret));
            dataSource.inTransaction(new Store(user, sealed, Base64.encode(nonce),
                    clock.currentTimeMillis()));
        } catch (Exception err) {
            throw failed(err);
        }
    }

    /// A user's secret replaced by a new, unconfirmed one, as the body of a
    /// transaction.
    private static final class Store implements DataSource.Work {
        private final String user;
        private final String sealed;
        private final String nonce;
        private final long now;

        Store(String user, String sealed, String nonce, long now) {
            this.user = user;
            this.sealed = sealed;
            this.nonce = nonce;
            this.now = now;
        }

        @Override
        public Object run(Database db) throws Exception {
            db.execute("DELETE FROM cn1_mfa_totp WHERE username_key = ?", new Object[] {user});
            db.execute("INSERT INTO cn1_mfa_totp (username_key, secret, nonce, confirmed, "
                    + "last_used_step, created_at) VALUES (?, ?, ?, 0, -1, ?)",
                    new Object[] {user, sealed, nonce, Long.valueOf(now)});
            return null;
        }
    }

    @Override
    public TotpCredential find(String username) {
        if (username == null) {
            return null;
        }
        String user = SecuritySchema.usernameKey(username);
        try {
            Map row = dataSource.queryOne("SELECT secret, nonce, confirmed, last_used_step FROM "
                    + "cn1_mfa_totp WHERE username_key = ?", new Object[] {user});
            if (row == null) {
                return null;
            }
            byte[] secret = Crypto.aesGcmDecrypt(key, Base64.decode(String.valueOf(
                    row.get("nonce"))), utf8(user), Base64.decode(String.valueOf(
                            row.get("secret"))));
            if (secret == null) {
                // Sealed under another key, or for another user: not a secret
                // this server can use, and not one it should guess around.
                throw new AuthenticationServiceException("The stored second factor of "
                        + username + " does not open with " + ENCRYPTION_KEY + ": the key was "
                        + "changed, or the row was altered");
            }
            Object confirmed = row.get("confirmed");
            Object step = row.get("last_used_step");
            return new TotpCredential(username, secret,
                    confirmed instanceof Number && ((Number) confirmed).longValue() != 0,
                    step instanceof Number ? ((Number) step).longValue() : -1);
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public boolean confirm(String username) {
        try {
            return dataSource.execute("UPDATE cn1_mfa_totp SET confirmed = 1 WHERE "
                    + "username_key = ? AND confirmed = 0",
                    new Object[] {SecuritySchema.usernameKey(username)}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public boolean advance(String username, long step) {
        try {
            return dataSource.execute("UPDATE cn1_mfa_totp SET last_used_step = ? WHERE "
                    + "username_key = ? AND last_used_step < ?", new Object[] {Long.valueOf(step),
                        SecuritySchema.usernameKey(username), Long.valueOf(step)}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public boolean confirm(String username, byte[] expectedSecret, long step) {
        return accept(username, expectedSecret, step, true);
    }

    @Override
    public boolean advance(String username, byte[] expectedSecret, long step) {
        return accept(username, expectedSecret, step, false);
    }

    private boolean accept(String username, byte[] expectedSecret, long step, boolean confirming) {
        if (username == null || expectedSecret == null) {
            return false;
        }
        String user = SecuritySchema.usernameKey(username);
        try {
            Map row = dataSource.queryOne("SELECT secret, nonce FROM cn1_mfa_totp "
                    + "WHERE username_key = ?", new Object[] {user});
            if (row == null) {
                return false;
            }
            String sealed = String.valueOf(row.get("secret"));
            String nonce = String.valueOf(row.get("nonce"));
            byte[] secret = Crypto.aesGcmDecrypt(key, Base64.decode(nonce), utf8(user),
                    Base64.decode(sealed));
            if (secret == null || !Crypto.equalsConstantTime(secret, expectedSecret)) {
                return false;
            }
            // A concurrent save changes the sealed secret and nonce. The predicate
            // binds this update to the row just checked, even on another server.
            return dataSource.execute("UPDATE cn1_mfa_totp SET confirmed = 1, last_used_step = ? "
                    + "WHERE username_key = ? AND secret = ? AND nonce = ? AND confirmed = ? "
                    + "AND last_used_step < ?", new Object[] {Long.valueOf(step), user, sealed,
                        nonce, Integer.valueOf(confirming ? 0 : 1), Long.valueOf(step)}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }

    @Override
    public boolean delete(String username) {
        try {
            return dataSource.execute("DELETE FROM cn1_mfa_totp WHERE username_key = ?",
                    new Object[] {SecuritySchema.usernameKey(username)}) == 1;
        } catch (IOException err) {
            throw failed(err);
        }
    }
}
