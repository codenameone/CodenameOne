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

import com.codename1.security.CryptoException;
import com.codename1.security.SecureRandom;
import com.codename1.security.SecureStorage;

import java.io.IOException;

/// Generates and retrieves the random keys behind `DatabaseConfig#managed()`.
///
/// This lives in the core rather than in each port on purpose: every platform must
/// derive byte-identical key material from the same alias, or a database written on
/// one device could not be opened on another. The only per-platform part is where
/// `com.codename1.security.SecureStorage` physically puts the bytes.
final class ManagedKeys {

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

        // Nothing came back, which is not the same as nothing being there. Every platform's
        // get() answers null for an entry it could not read as well as for one that does not
        // exist, and generating on the strength of that overwrites a key that was there all
        // along -- after which the database encrypted under the old one cannot be opened by
        // anyone, ever. So the store is asked the question it can actually answer, and a key is
        // generated only when it says the entry is absent.
        if (storage.entryState(account) != SecureStorage.ENTRY_ABSENT) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                    "The managed database key for '" + alias + "' could not be read, and this "
                    + "platform cannot say whether one is stored. Refusing to generate a "
                    + "replacement, because doing so would overwrite an existing key and leave "
                    + "the database it encrypted unreadable. Try again when the key store is "
                    + "available, or supply a passphrase with DatabaseConfig.passphrase().");
        }

        byte[] generated;
        try {
            generated = SecureRandom.bytes(KEY_LENGTH);
        } catch (CryptoException noRandomness) {
            // Same failure as a store that will not hold the key, and it has to arrive the same
            // way. SecureRandom reports an unavailable or failing platform RNG by throwing an
            // unchecked CryptoException, which walked straight out of this method past its
            // declared contract: on Android the reflective open handler read it as
            // NOT_SUPPORTED -- "this platform has no cipher", which is the wrong thing to tell
            // anyone, since the cipher is fine and the randomness is not -- and the other ports
            // let it out unchecked at a caller that is only prepared for an IOException. There
            // is nothing to fall back to: a key from a broken RNG is the one outcome worse than
            // no key at all.
            throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                    "This platform could not produce the randomness a managed database key needs."
                    + " Supply a passphrase with DatabaseConfig.passphrase() instead.",
                    noRandomness);
        }
        // Created rather than written, and the answer is what the store ended up holding. The
        // synchronized above covers threads in this VM and nothing between two: an application
        // can run in more than one process -- Android components with their own android:process,
        // or two runs of a desktop build -- and both can find nothing stored and generate. Whoever
        // lost the race takes the winner's key here instead of overwriting it, so the database is
        // opened with the key the store actually has rather than the one this call made up.
        String stored = storage.setIfAbsent(account, toHex(generated));
        if (stored == null) {
            // Deliberately fatal. Carrying on with a key we cannot persist would
            // produce a database that can never be reopened, and falling back to a
            // derived or constant key would silently downgrade the protection the
            // caller asked for. Both are worse than failing here.
            throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                    "This platform cannot store a managed database key. Supply a passphrase "
                    + "with DatabaseConfig.passphrase() instead.");
        }
        if (stored.length() != KEY_LENGTH * 2) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                    "The managed database key for '" + alias + "' was stored by something else "
                    + "and is not a key this can read. Refusing to overwrite it, because a "
                    + "replacement would leave any database it encrypted unreadable.");
        }
        return fromHex(stored);
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

    /// Whether a key is stored under this alias.
    ///
    /// `#forget(String)` cannot answer it: every platform's remove reports success for an entry
    /// that was not there -- Android returns the commit result, the keychain treats
    /// errSecItemNotFound as success -- so a caller choosing between two aliases has to look
    /// before it deletes rather than infer from what the delete returned.
    static boolean has(String alias) {
        return state(alias) == SecureStorage.ENTRY_PRESENT;
    }

    /// What the key store says about an alias: present, absent, or unknown.
    ///
    /// Separate from `#has(String)` because a caller that acts on absence has to tell absence from
    /// a store that could not be asked. Deleting on the strength of a failed lookup is how an
    /// unrelated database loses its key.
    ///
    /// #### Parameters
    ///
    /// - `alias`: the logical key name
    ///
    /// #### Returns
    ///
    /// one of the `SecureStorage` entry states
    static int state(String alias) {
        SecureStorage storage = SecureStorage.getInstance();
        String account = accountName(alias);
        if (storage.get(account) != null) {
            // Readable, so it is certainly there, whatever the store would say about it.
            return SecureStorage.ENTRY_PRESENT;
        }
        return storage.entryState(account);
    }

    /// Maps an alias to the `SecureStorage` account name.
    ///
    /// A platform key store may not accept every character an application can put in a database
    /// name, so unsafe ones are escaped -- but escaped reversibly, as `%` followed by two hex
    /// digits. Folding them all to `_` would make `customer/db` and `customer_db` the same
    /// account: two databases would silently share a key, and forgetting one would destroy the
    /// other. `%` itself is escaped so the encoding stays unambiguous.
    private static String accountName(String alias) {
        StringBuilder b = new StringBuilder(ACCOUNT_PREFIX.length() + alias.length() + 8);
        b.append(ACCOUNT_PREFIX);
        for (int iter = 0; iter < alias.length(); iter++) {
            char c = alias.charAt(iter);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_';
            if (safe) {
                b.append(c);
            } else {
                b.append('%');
                b.append("0123456789abcdef".charAt((c >> 12) & 0x0f));
                b.append("0123456789abcdef".charAt((c >> 8) & 0x0f));
                b.append("0123456789abcdef".charAt((c >> 4) & 0x0f));
                b.append("0123456789abcdef".charAt(c & 0x0f));
            }
        }
        return b.toString();
    }

    private static String toHex(byte[] data) {
        StringBuilder b = new StringBuilder(data.length * 2);
        for (byte raw : data) {
            int v = raw & 0xff;
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
