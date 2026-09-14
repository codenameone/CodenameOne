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

    /// The key is derived from an unlocked [com.codename1.security.vault.Vault].
    ///
    /// Different from `#KEY_MANAGED` in where the protection comes from. A managed key is held by
    /// the platform key store and is available whenever the application runs; a vault key exists
    /// only while the user has unlocked the vault, which is what makes it usable in a browser --
    /// there is no key store in a page, and a key that is always available there is a key sitting
    /// in the open.
    public static final int KEY_VAULT = 4;

    /// The only cipher profile currently defined.
    static final String PROFILE_SQLCIPHER4 = "sqlcipher4";

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final int keyMode;
    private final String keyAlias;
    private char[] passphrase;
    private byte[] rawKey;
    private final com.codename1.security.vault.Vault vault;

    private DatabaseConfig(int keyMode, String keyAlias, char[] passphrase, byte[] rawKey) {
        this(keyMode, keyAlias, passphrase, rawKey, null);
    }

    private DatabaseConfig(int keyMode, String keyAlias, char[] passphrase, byte[] rawKey,
                           com.codename1.security.vault.Vault vault) {
        this.keyMode = keyMode;
        this.keyAlias = keyAlias;
        this.passphrase = passphrase;
        this.rawKey = rawKey;
        this.vault = vault;
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
        // Refused for the same reason and in the same spirit: the engines read a key of exactly
        // this shape as 32 raw bytes written in hexadecimal rather than as text to run the key
        // derivation over. A passphrase that happens to look like one would therefore be used as a
        // raw key, silently skipping the derivation this method promises -- weaker in a way
        // nothing would report. Add any character to it, or use rawKey if raw bytes were meant.
        if (looksLikeRawKeyLiteral(passphrase)) {
            throw new IllegalArgumentException("The database passphrase must not have the form of "
                    + "a raw key literal, x' followed by 64 hexadecimal digits and a closing "
                    + "quote. The engines read that as raw bytes rather than as a passphrase, so "
                    + "it would silently skip the key derivation. Use rawKey(byte[]) for raw bytes.");
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

    /// Keys the database from an unlocked vault.
    ///
    /// The key is derived from the vault's data key -- see
    /// [com.codename1.security.vault.Vault#databaseKey(String)], including its honest account of
    /// what handing raw bytes to a database engine costs. The vault must be unlocked when the
    /// database is opened; it need not stay unlocked afterwards, because the engine has the key
    /// by then, and locking the vault does **not** close an open connection.
    ///
    /// #### What this buys over `#managed()`
    ///
    /// In a browser, everything. `managed()` keeps its key wherever the port's secure storage is,
    /// which in a page is origin-private storage -- now encrypted under a non-extractable
    /// `CryptoKey`, and still readable by anything running in the origin at any time, because
    /// nothing gates it. A vault key does not exist until the user unlocks, so a page loaded and
    /// left alone has no database key in it at all.
    ///
    /// On Android and iOS the two are closer: a managed key already sits in the OS key store.
    /// The vault still adds the user's password to the chain, and adds cross-device portability,
    /// which a key generated per device does not have.
    ///
    /// #### Rotation
    ///
    /// [com.codename1.security.vault.Vault#rotateDataKey] changes this key. A database opened
    /// under the old one has to be rekeyed in the same operation or it can no longer be opened;
    /// there is no automatic rekey, because an interrupted one leaves a database encrypted under
    /// neither key and that is not a thing to do behind a caller's back.
    ///
    /// #### Parameters
    ///
    /// - `vault`: an unlocked vault
    ///
    /// - `alias`: the key alias within the vault, normally the database name
    ///
    /// #### Returns
    ///
    /// a vault-keyed config
    public static DatabaseConfig vault(com.codename1.security.vault.Vault vault, String alias) {
        if (vault == null) {
            throw new IllegalArgumentException("A vault-keyed database needs a vault");
        }
        return new DatabaseConfig(KEY_VAULT, alias, null, null, vault);
    }

    /// Returns the key mode, one of `#KEY_NONE`, `#KEY_PASSPHRASE`, `#KEY_MANAGED`,
    /// `#KEY_RAW` or `#KEY_VAULT`.
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
            case KEY_VAULT:
                return resolveVaultKey(databaseName);
            default:
                return toKeyLiteral(ManagedKeys.keyFor(keyAlias != null ? keyAlias : databaseName));
        }
    }

    /// Derives the engine literal from the vault, and clears the bytes as soon as it has.
    ///
    /// The literal is still a `String` and still cannot be wiped -- that limitation is the same
    /// one `#wipe()` documents for every other mode -- but the array the vault produced does not
    /// outlive this method.
    private String resolveVaultKey(String databaseName) throws IOException {
        // Taken as Object and then tested, rather than assigned straight to byte[] inside the
        // try. Erasure puts a checkcast at this call site, and ParparVM does not throw for a
        // failed one -- so a cast inside a catch(RuntimeException) is a handler that cannot run
        // on iOS, and the wrong object would go on to be read as key material.
        Object resolved;
        try {
            resolved = vault.databaseKey(keyAlias != null ? keyAlias : databaseName).get();
        } catch (RuntimeException failed) {
            com.codename1.security.vault.VaultError error = errorOf(failed);
            throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                    error == com.codename1.security.vault.VaultError.LOCKED
                            ? "The vault is locked, so this database cannot be opened. Unlock it "
                              + "and open the database again."
                            : "The vault could not produce a key for this database (" + error
                              + ").", failed);
        }
        if (!(resolved instanceof byte[])) {
            throw new DatabaseEncryptionException(DatabaseEncryptionException.KEY_UNAVAILABLE,
                    "The vault produced no key material for this database.");
        }
        byte[] key = (byte[]) resolved;
        try {
            return toKeyLiteral(key);
        } finally {
            for (int iter = 0; iter < key.length; iter++) {
                key[iter] = 0;
            }
        }
    }

    private static com.codename1.security.vault.VaultError errorOf(Throwable failed) {
        Throwable cause = failed;
        while (cause != null) {
            if (cause instanceof com.codename1.security.vault.VaultException) {
                return ((com.codename1.security.vault.VaultException) cause).getError();
            }
            cause = cause.getCause();
        }
        return com.codename1.security.vault.VaultError.UNKNOWN;
    }

    /// What actually protects this database's key on this device.
    ///
    /// `#isKeyHardwareBacked()` answers one bit of this and has to answer `false` wherever it
    /// cannot verify, which reads the same as "definitely not". This reports each protection
    /// separately and distinguishes "no" from "cannot say" -- see
    /// [com.codename1.security.vault.ProtectionReport].
    ///
    /// #### Returns
    ///
    /// the effective protection, never null
    public com.codename1.security.vault.ProtectionReport effectiveKeyProtection() {
        com.codename1.security.vault.ProtectionReport.Builder b =
                com.codename1.security.vault.ProtectionReport.builder();
        if (keyMode == KEY_NONE) {
            return com.codename1.security.vault.ProtectionReport.none();
        }
        if (keyMode == KEY_VAULT) {
            return vault.databaseKeyProtection();
        }
        if (keyMode == KEY_PASSPHRASE || keyMode == KEY_RAW) {
            // The application holds this key, wherever it got it. Nothing here knows what
            // protects it, and guessing would be the one answer worse than saying so.
            b.set(com.codename1.security.vault.Protection.ENCRYPTED_AT_REST, true);
            b.set(com.codename1.security.vault.Protection.PERSISTENT,
                    com.codename1.security.vault.ProtectionReport.UNKNOWN);
            b.set(com.codename1.security.vault.Protection.NON_EXTRACTABLE_KEY, false);
            b.set(com.codename1.security.vault.Protection.OS_PROTECTED,
                    com.codename1.security.vault.ProtectionReport.UNKNOWN);
            b.set(com.codename1.security.vault.Protection.HARDWARE_BACKED,
                    com.codename1.security.vault.ProtectionReport.UNKNOWN);
            b.set(com.codename1.security.vault.Protection.USER_VERIFICATION, false);
            b.set(com.codename1.security.vault.Protection.ISOLATED_FROM_APPLICATION_CODE, false);
            return b.build();
        }
        com.codename1.security.vault.ProtectionReport store =
                com.codename1.security.SecureStorage.getInstance().protection();
        // Propagated, not asserted. The managed key is only as encrypted as the store holding
        // it, and two stores answer NO on purpose: the JavaSE simulator, where this is
        // reproducible obfuscation rather than encryption, and Android below API 23, which has
        // no keystore to wrap with. Answering YES here told policy and diagnostics the key was
        // encrypted in exactly the two places it is not.
        b.set(com.codename1.security.vault.Protection.ENCRYPTED_AT_REST,
                store.answer(com.codename1.security.vault.Protection.ENCRYPTED_AT_REST));
        b.set(com.codename1.security.vault.Protection.PERSISTENT,
                store.answer(com.codename1.security.vault.Protection.PERSISTENT));
        b.set(com.codename1.security.vault.Protection.NON_EXTRACTABLE_KEY, false);
        b.set(com.codename1.security.vault.Protection.OS_PROTECTED,
                store.answer(com.codename1.security.vault.Protection.OS_PROTECTED));
        b.set(com.codename1.security.vault.Protection.HARDWARE_BACKED,
                isKeyHardwareBacked() ? com.codename1.security.vault.ProtectionReport.YES
                        : store.answer(com.codename1.security.vault.Protection.HARDWARE_BACKED));
        b.set(com.codename1.security.vault.Protection.USER_VERIFICATION, false);
        b.set(com.codename1.security.vault.Protection.ISOLATED_FROM_APPLICATION_CODE, false);
        return b.build();
    }

    /// The length of the literal `#toKeyLiteral(byte[])` writes: `x'`, 64 hex digits, `'`.
    private static final int RAW_KEY_LITERAL_LENGTH = 67;

    /// Whether a string is exactly the literal an engine reads as 32 raw bytes.
    ///
    /// `x'` then 64 hexadecimal digits then `'`, which is what `#toKeyLiteral(byte[])` writes.
    static boolean looksLikeRawKeyLiteral(String value) {
        // 67: "x'" is two, the digits are sixty-four, the closing quote is one. Requiring 68
        // meant the literal toKeyLiteral actually writes was never recognized, so the rejection
        // this method exists for never fired and such a passphrase reached the engines as a raw
        // key -- exactly the silent skip of the derivation it was written to prevent.
        if (value == null || value.length() != RAW_KEY_LITERAL_LENGTH) {
            return false;
        }
        if (value.charAt(0) != 'x' && value.charAt(0) != 'X') {
            return false;
        }
        if (value.charAt(1) != '\'' || value.charAt(RAW_KEY_LITERAL_LENGTH - 1) != '\'') {
            return false;
        }
        for (int iter = 2; iter < RAW_KEY_LITERAL_LENGTH - 1; iter++) {
            char c = value.charAt(iter);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
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
