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
package com.codename1.security.vault;

import com.codename1.security.CryptoException;
import com.codename1.security.SecureRandom;
import com.codename1.security.SecureStorage;
import com.codename1.security.vault.spi.DeviceProtection;
import com.codename1.util.AsyncResource;

/// The [DeviceProtection] every port gets when it does not supply one of its own: a random
/// wrapping key in [SecureStorage], and AES-GCM performed in shared code.
///
/// This is the right implementation wherever `SecureStorage` is the operating system's secret
/// store -- the iOS keychain, the Android keystore, the desktop credential store. The wrapping
/// key is bytes, and it is bytes in the one place on the device that is built to hold bytes
/// nobody else should reach.
///
/// It is the **wrong** implementation in a browser, where `SecureStorage` is ordinary
/// origin-private storage: the wrapping key would sit beside the ciphertext it protects, which is
/// no protection at all. The JavaScript port therefore overrides
/// [com.codename1.impl.CodenameOneImplementation#getDeviceProtection] with one built on a
/// non-extractable `CryptoKey`, and this class never runs there.
///
/// #### Racing callers
///
/// Key creation goes through [SecureStorage#setIfAbsent(String, String)], which returns what the
/// store ended up holding rather than what this call wrote. Two processes that both find nothing
/// therefore agree on one key instead of each overwriting the other -- which matters more here
/// than almost anywhere, because the loser's key is what a device's remembered vault was wrapped
/// under.
public class SecureStorageDeviceProtection extends DeviceProtection {

    /// Namespace for the wrapping keys, kept away from whatever else an application stores.
    private static final String ACCOUNT_PREFIX = "cn1.vault.dk.";

    private static final int KEY_LENGTH = 32;

    /// The store backing this. Overridable so a port can supply a store other than the one
    /// [SecureStorage#getInstance()] returns, which the simulator's test harness uses.
    protected SecureStorage storage() {
        return SecureStorage.getInstance();
    }

    private static String account(String keyId) {
        return ACCOUNT_PREFIX + (keyId == null ? "" : keyId);
    }

    @Override
    public ProtectionReport protection() {
        SecureStorage store = storage();
        ProtectionReport.Builder b = ProtectionReport.builder();
        // Asked of the store rather than assumed: the base SecureStorage returns ENTRY_UNKNOWN
        // for everything, which is exactly the platform that must not be described as protected.
        boolean hasStore = store.entryState(account("cn1.probe")) != SecureStorage.ENTRY_UNKNOWN;
        b.set(Protection.PERSISTENT, hasStore);
        b.set(Protection.ENCRYPTED_AT_REST, hasStore);
        // The key is bytes this class can read back, by construction. Saying otherwise would be
        // the single most misleading thing this file could do.
        b.set(Protection.NON_EXTRACTABLE_KEY, false);
        // Whether the store is the OS key store is a question about the port, and the port is
        // what overrides this class when the answer is no. From here it cannot be verified.
        b.set(Protection.OS_PROTECTED, hasStore ? ProtectionReport.UNKNOWN : ProtectionReport.NO);
        b.set(Protection.HARDWARE_BACKED, ProtectionReport.UNKNOWN);
        b.set(Protection.USER_VERIFICATION, false);
        b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
        return b.build();
    }

    @Override
    public int keyState(String keyId) {
        switch (storage().entryState(account(keyId))) {
            case SecureStorage.ENTRY_PRESENT:
                return KEY_PRESENT;
            case SecureStorage.ENTRY_ABSENT:
                return KEY_ABSENT;
            default:
                return KEY_UNKNOWN;
        }
    }

    @Override
    public AsyncResource<Boolean> ensureKey(String keyId) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        try {
            byte[] key = loadOrCreate(keyId);
            Bytes.zero(key);
            out.complete(Boolean.TRUE);
        } catch (VaultException failed) {
            out.error(failed);
        }
        return out;
    }

    @Override
    public AsyncResource<byte[]> wrap(String keyId, byte[] plaintext, byte[] aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] key = null;
        try {
            key = loadOrCreate(keyId);
            out.complete(SecureEnvelope.seal(key, keyId, 1, aad, plaintext));
        } catch (VaultException failed) {
            out.error(failed);
        } catch (CryptoException failed) {
            out.error(new VaultException(VaultError.CRYPTO_UNAVAILABLE,
                    "the platform could not wrap under the device key", failed));
        } finally {
            Bytes.zero(key);
        }
        return out;
    }

    @Override
    public AsyncResource<byte[]> unwrap(String keyId, byte[] wrapped, byte[] aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] key = null;
        try {
            key = load(keyId);
            if (key == null) {
                // Absence and unreadability are answered differently, because the caller is about
                // to decide whether to create a replacement.
                out.error(new VaultException(
                        keyState(keyId) == KEY_ABSENT ? VaultError.KEY_MISSING
                                : VaultError.TEMPORARILY_UNREADABLE,
                        "no device key is available for this vault"));
                return out;
            }
            out.complete(SecureEnvelope.parse(wrapped).open(key, aad));
        } catch (VaultException failed) {
            out.error(failed);
        } catch (CryptoException failed) {
            out.error(new VaultException(VaultError.CRYPTO_UNAVAILABLE,
                    "the platform could not unwrap under the device key", failed));
        } finally {
            Bytes.zero(key);
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteKey(String keyId) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        out.complete(Boolean.valueOf(storage().remove(account(keyId))));
        return out;
    }

    private byte[] load(String keyId) {
        String stored = storage().get(account(keyId));
        if (stored == null) {
            return null;
        }
        byte[] key = Bytes.fromHex(stored);
        if (key == null || key.length != KEY_LENGTH) {
            throw new VaultException(VaultError.CORRUPT,
                    "the stored device key is not a key this build wrote; refusing to replace it, "
                    + "because a replacement would orphan everything the original protected");
        }
        return key;
    }

    private byte[] loadOrCreate(String keyId) {
        byte[] existing = load(keyId);
        if (existing != null) {
            return existing;
        }
        if (keyState(keyId) != KEY_ABSENT) {
            // The store has no key it can hand over and will not say the entry is absent. Writing
            // here is how a device key that was there all along gets overwritten.
            throw new VaultException(VaultError.TEMPORARILY_UNREADABLE,
                    "the device key could not be read and the store cannot say whether one "
                    + "exists; refusing to create a replacement");
        }
        byte[] generated;
        try {
            generated = SecureRandom.bytes(KEY_LENGTH);
        } catch (CryptoException noRandomness) {
            throw new VaultException(VaultError.CRYPTO_UNAVAILABLE,
                    "this platform could not produce the randomness a device key needs",
                    noRandomness);
        }
        String hex = Bytes.toHex(generated);
        Bytes.zero(generated);
        String settled = storage().setIfAbsent(account(keyId), hex);
        if (settled == null) {
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "this platform cannot store a device key, so a vault cannot be remembered "
                    + "across restarts here");
        }
        byte[] key = Bytes.fromHex(settled);
        if (key == null || key.length != KEY_LENGTH) {
            throw new VaultException(VaultError.CORRUPT,
                    "the device key entry was written by something else; refusing to overwrite it");
        }
        return key;
    }
}
