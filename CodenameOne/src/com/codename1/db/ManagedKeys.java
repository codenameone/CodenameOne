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
package com.codename1.db;

import com.codename1.security.SecureRandom;
import com.codename1.security.SecureStorage;

import java.io.IOException;

/// Generates and retrieves the random keys behind `DatabaseConfig#managed()`.
///
/// This lives in the core rather than in each port on purpose: every platform must
/// derive byte-identical key material from the same alias, or a database written on
/// one device could not be opened on another. The only per-platform part is where
/// `com.codename1.security.SecureStorage` physically puts the bytes.
class ManagedKeys {

    /// Prefix applied to every alias so database keys cannot collide with whatever
    /// else the application stores through `SecureStorage`.
    private static final String ACCOUNT_PREFIX = "cn1.db.key.";

    private static final int KEY_LENGTH = 32;

    private ManagedKeys() {
    }

    /// Returns the managed key for an alias, generating and storing one on first use.
    ///
    /// #### Parameters
    ///
    /// - `alias`: the logical key name, normally the database name
    ///
    /// #### Returns
    ///
    /// 32 bytes of key material
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the platform cannot store or retrieve the key
    static synchronized byte[] keyFor(String alias) throws IOException {
        // Synchronized because the read-generate-store sequence below is not atomic. Two threads
        // opening the same managed database for the first time could otherwise both see nothing
        // stored, generate different keys, and each overwrite the other: whichever key lost the
        // race would still have written a database nobody can ever read again.
        String account = accountName(alias);
        SecureStorage storage = SecureStorage.getInstance();

        String existing = storage.get(account);
        if (existing != null && existing.length() == KEY_LENGTH * 2) {
            return fromHex(existing);
        }
        if (existing != null) {
            // Something is stored under our account but it is not a key we wrote.
            // Overwriting it would destroy whatever it is and silently re-key the
            // database, so refuse instead.
            throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                    "The stored managed database key for '" + alias + "' is malformed. "
                    + "Refusing to overwrite it, because generating a new key would make "
                    + "any existing database unreadable.");
        }

        byte[] generated = SecureRandom.bytes(KEY_LENGTH);
        if (!storage.set(account, toHex(generated))) {
            // Deliberately fatal. Carrying on with a key we cannot persist would
            // produce a database that can never be reopened, and falling back to a
            // derived or constant key would silently downgrade the protection the
            // caller asked for. Both are worse than failing here.
            throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                    "This platform cannot store a managed database key. Supply a passphrase "
                    + "with DatabaseConfig.passphrase() instead.");
        }
        return generated;
    }

    /// Removes the managed key for an alias.
    ///
    /// #### Parameters
    ///
    /// - `alias`: the logical key name
    ///
    /// #### Returns
    ///
    /// true if an entry was removed
    static boolean forget(String alias) {
        return SecureStorage.getInstance().remove(accountName(alias));
    }

    /// Maps an alias to the `SecureStorage` account name, folding away characters
    /// that a platform key store may not accept in an identifier.
    private static String accountName(String alias) {
        StringBuilder b = new StringBuilder(ACCOUNT_PREFIX.length() + alias.length());
        b.append(ACCOUNT_PREFIX);
        for (int iter = 0; iter < alias.length(); iter++) {
            char c = alias.charAt(iter);
            if (c == '/' || c == '\\' || c == ':' || c == ' ') {
                b.append('_');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String toHex(byte[] data) {
        StringBuilder b = new StringBuilder(data.length * 2);
        for (int iter = 0; iter < data.length; iter++) {
            int v = data[iter] & 0xff;
            b.append("0123456789abcdef".charAt(v >>> 4));
            b.append("0123456789abcdef".charAt(v & 0x0f));
        }
        return b.toString();
    }

    private static byte[] fromHex(String hex) throws IOException {
        byte[] out = new byte[hex.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            int hi = digit(hex.charAt(iter * 2));
            int lo = digit(hex.charAt(iter * 2 + 1));
            if (hi < 0 || lo < 0) {
                throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                        "The stored managed database key is not valid hexadecimal");
            }
            out[iter] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static int digit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }
}
