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

import java.io.IOException;

/// Describes how a database should be opened, and in particular how it is keyed.
///
/// Pass an instance to `Database#openOrCreate(java.lang.String, com.codename1.db.DatabaseConfig)`.
/// Opening without a config, through `Database#openOrCreate(java.lang.String)`, is
/// always plaintext and always will be -- there is no implicit upgrade.
///
/// #### Security
///
/// **A passphrase written into your source code is not a secret.** String literals
/// are recoverable from a shipped .ipa or .apk in minutes, so a constant passphrase
/// buys you nothing against anyone who has the file. This is the mistake that gets
/// made most often, so it is worth being blunt about it: if your application cannot
/// ask a human for a passphrase, use `#managed()` instead. A random key held in the
/// platform key store is strictly better than a constant compiled into the binary.
///
/// Encryption here protects data **at rest** and nothing else. It does not defend
/// against a rooted or jailbroken device, a debugger attached to the running
/// process, or a memory dump: while the database is open the key is in memory.
///
/// #### Choosing a mode
///
/// ```java
/// // A human supplies the secret. Nothing is stored on the device.
/// DatabaseConfig.passphrase(passwordField.getText());
///
/// // No secret to manage. A random key is generated once and kept in the
/// // platform key store. Best default when there is nobody to prompt.
/// DatabaseConfig.managed();
///
/// // The application already has 32 bytes of key material of its own.
/// DatabaseConfig.rawKey(keyBytes);
///
/// // Explicitly plaintext.
/// DatabaseConfig.plain();
/// ```
///
/// #### On-disk format
///
/// Every platform that supports encryption reads and writes the same format, so a
/// database created on one device can be opened on another and in the simulator.
/// See the `com.codename1.db` package documentation for the pinned parameters.
public final class DatabaseConfig {

    /// No encryption. The database is a plain SQLite file.
    public static final int KEY_NONE = 0;

    /// The key is derived from an application supplied passphrase.
    public static final int KEY_PASSPHRASE = 1;

    /// The key is random, generated once, and held in the platform key store.
    public static final int KEY_MANAGED = 2;

    /// The key is 32 raw bytes supplied by the application.
    public static final int KEY_RAW = 3;

    /// The only cipher profile currently defined.
    static final String PROFILE_SQLCIPHER4 = "sqlcipher4";

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final int keyMode;
    private final String keyAlias;
    private char[] passphrase;
    private byte[] rawKey;

    private DatabaseConfig(int keyMode, String keyAlias, char[] passphrase, byte[] rawKey) {
        this.keyMode = keyMode;
        this.keyAlias = keyAlias;
        this.passphrase = passphrase;
        this.rawKey = rawKey;
    }

    /// Returns a config that opens the database unencrypted.
    ///
    /// This is identical to calling `Database#openOrCreate(java.lang.String)` and
    /// exists so that code choosing between modes at runtime has something to
    /// return for the plaintext case.
    ///
    /// #### Returns
    ///
    /// a plaintext config
    public static DatabaseConfig plain() {
        return new DatabaseConfig(KEY_NONE, null, null, null);
    }

    /// Returns a config keyed from the supplied passphrase.
    ///
    /// The passphrase is stretched into a key by the cipher's key derivation
    /// function, so a weak passphrase yields a weak database. Nothing is stored on
    /// the device: losing the passphrase means losing the data.
    ///
    /// #### Parameters
    ///
    /// - `passphrase`: the secret, which must not be null or empty, and must not contain the
    ///   character with code point zero
    ///
    /// #### Returns
    ///
    /// a passphrase-keyed config
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the passphrase is null, empty, or contains the character
    ///   with code point zero
    public static DatabaseConfig passphrase(String passphrase) {
        if (passphrase == null || passphrase.length() == 0) {
            throw new IllegalArgumentException("The database passphrase must not be null or empty");
        }
        // Rejected rather than truncated. The cipher takes a key as bytes and a length, and the
        // engines behind this API disagree about where that length comes from: some measure to the
        // first zero byte, which would silently reduce two passphrases differing only after such a
        // character to the same key, and make one passphrase open a database on one platform and
        // not on another.
        // Refusing is the one answer that is the same everywhere, and it is loud.
        if (passphrase.indexOf(0) >= 0) {
            throw new IllegalArgumentException("The database passphrase must not contain the "
                    + "character with code point zero. It cannot be carried to every platform's "
                    + "cipher without being silently cut short there, so it is refused rather "
                    + "than quietly weakening the key.");
        }
        return new DatabaseConfig(KEY_PASSPHRASE, null, passphrase.toCharArray(), null);
    }

    /// Returns a config keyed by a random key held in the platform key store,
    /// using the database name as the key alias.
    ///
    /// The first time a database is opened this way a fresh random key is
    /// generated and stored. Subsequent opens retrieve the same key. The
    /// application never sees or handles the key.
    ///
    /// #### Durability
    ///
    /// The key lives and dies with the platform key store entry. Uninstalling the
    /// application, wiping the device, or -- on Android -- restoring a backup onto a
    /// different device leaves the database permanently unreadable, because
    /// Android key store keys cannot be exported. iOS keychain entries do survive
    /// an encrypted backup and restore. If the data must outlive the device, use
    /// `#passphrase(java.lang.String)` with a secret the user or your server holds.
    ///
    /// #### Returns
    ///
    /// a config keyed from the platform key store
    public static DatabaseConfig managed() {
        return new DatabaseConfig(KEY_MANAGED, null, null, null);
    }

    /// Returns a config keyed by a random key held in the platform key store under
    /// an explicit alias.
    ///
    /// Use this when several databases should share one key, or when the database
    /// name may change but the key should not.
    ///
    /// #### Parameters
    ///
    /// - `keyAlias`: the key store alias, which must not be null or empty
    ///
    /// #### Returns
    ///
    /// a config keyed from the platform key store
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the alias is null or empty
    ///
    /// #### See also
    ///
    /// - #managed()
    public static DatabaseConfig managed(String keyAlias) {
        if (keyAlias == null || keyAlias.length() == 0) {
            throw new IllegalArgumentException("The managed key alias must not be null or empty");
        }
        return new DatabaseConfig(KEY_MANAGED, keyAlias, null, null);
    }

    /// Returns a config keyed directly by 32 raw bytes, bypassing key derivation.
    ///
    /// Use this when the application already derives key material by its own means,
    /// for instance from a server-issued secret. Because no key derivation function
    /// is applied, the bytes must already be uniformly random -- do not pass a
    /// hashed password here and expect passphrase-grade protection.
    ///
    /// #### Parameters
    ///
    /// - `key`: exactly 32 bytes of key material
    ///
    /// #### Returns
    ///
    /// a raw-keyed config
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the array is null or is not exactly 32 bytes
    public static DatabaseConfig rawKey(byte[] key) {
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException("A raw database key must be exactly 32 bytes");
        }
        byte[] copy = new byte[32];
        System.arraycopy(key, 0, copy, 0, 32);
        return new DatabaseConfig(KEY_RAW, null, null, copy);
    }

    /// Returns the key mode, one of `#KEY_NONE`, `#KEY_PASSPHRASE`, `#KEY_MANAGED`
    /// or `#KEY_RAW`.
    ///
    /// #### Returns
    ///
    /// the key mode
    public int getKeyMode() {
        return keyMode;
    }

    /// Returns whether this config asks for an encrypted database.
    ///
    /// #### Returns
    ///
    /// true unless the mode is `#KEY_NONE`
    public boolean isEncrypted() {
        return keyMode != KEY_NONE;
    }

    /// Returns the explicit managed key alias, or null when the database name is
    /// used as the alias.
    ///
    /// #### Returns
    ///
    /// the alias or null
    public String getKeyAlias() {
        return keyAlias;
    }

    /// Returns the cipher profile name that describes the on-disk format.
    ///
    /// Only one profile is currently defined. The accessor exists so that a future
    /// profile can be introduced without changing the shape of this class.
    ///
    /// #### Returns
    ///
    /// the profile identifier
    public String getCipherProfile() {
        return PROFILE_SQLCIPHER4;
    }

    /// Returns whether keys for this config are protected by dedicated key storage
    /// hardware on the current platform.
    ///
    /// This is false for `#passphrase(java.lang.String)` and `#rawKey(byte[])`,
    /// because the application, not the platform, holds that key material. For
    /// `#managed()` it reflects the platform: true where a hardware backed key
    /// store is available, and **false in the simulator**, where the key is
    /// protected only by a software derived key in the desktop user profile.
    ///
    /// Applications with a hard requirement on hardware backing should check this
    /// and refuse to store sensitive data when it returns false.
    ///
    /// #### Returns
    ///
    /// true when the key is held in hardware backed storage
    public boolean isKeyHardwareBacked() {
        if (keyMode != KEY_MANAGED) {
            return false;
        }
        return Database.isManagedKeyHardwareBacked();
    }

    /// Clears the key material held by this config.
    ///
    /// Call this once the database has been opened. The passphrase and raw key
    /// buffers are overwritten with zeroes.
    ///
    /// Note the honest limitation: the value actually handed to the database engine
    /// is a `String`, because every supported engine keys from one, and Java strings
    /// are immutable and cannot be wiped. This method reduces the window, it does
    /// not eliminate it.
    public void wipe() {
        if (passphrase != null) {
            for (int iter = 0; iter < passphrase.length; iter++) {
                passphrase[iter] = 0;
            }
            passphrase = null;
        }
        if (rawKey != null) {
            for (int iter = 0; iter < rawKey.length; iter++) {
                rawKey[iter] = 0;
            }
            rawKey = null;
        }
    }

    /// Produces the key literal handed to the underlying engine.
    ///
    /// This exists for the platform implementations; applications have no reason to call it.
    /// Passphrases are returned verbatim. Raw and managed keys are rendered as the literal `x'`
    /// followed by 64 hexadecimal characters and a closing quote, which is the one form every
    /// supported engine interprets identically as a raw key with no key derivation applied.
    ///
    /// For a managed key this is the call that generates and stores the key on first use, so it
    /// can fail even though the config itself was built successfully.
    ///
    /// #### Parameters
    ///
    /// - `databaseName`: used as the key store alias when no explicit alias was set
    ///
    /// #### Returns
    ///
    /// the key literal, or null when the config is plaintext
    ///
    /// #### Throws
    ///
    /// - `IOException`: if a managed key could not be produced or stored
    public String resolveKeyMaterial(String databaseName) throws IOException {
        switch (keyMode) {
            case KEY_NONE:
                return null;
            case KEY_PASSPHRASE:
                if (passphrase == null) {
                    throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                            "The passphrase has already been wiped from this configuration");
                }
                return new String(passphrase);
            case KEY_RAW:
                if (rawKey == null) {
                    throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                            "The raw key has already been wiped from this configuration");
                }
                return toKeyLiteral(rawKey);
            default:
                return toKeyLiteral(ManagedKeys.keyFor(keyAlias != null ? keyAlias : databaseName));
        }
    }

    /// Renders 32 bytes as the engine level raw key literal.
    static String toKeyLiteral(byte[] key) {
        StringBuilder b = new StringBuilder(68);
        b.append("x'");
        for (byte raw : key) {
            int v = raw & 0xff;
            b.append(HEX[v >>> 4]);
            b.append(HEX[v & 0x0f]);
        }
        b.append('\'');
        return b.toString();
    }
}
