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
package com.codename1.impl.windows;

import com.codename1.io.Storage;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.SecureStorage;
import com.codename1.util.AsyncResource;

/**
 * Windows secure storage backed by the OS Data Protection API (DPAPI). Each
 * value is encrypted with {@link WindowsNative#dpapiProtect} -- a key derived
 * from the current Windows user's logon, so the ciphertext is decryptable only
 * by that user on this machine -- and the encrypted blob is persisted through
 * the normal Codename One {@link Storage}. This is the desktop analog of the iOS
 * keychain / Android EncryptedSharedPreferences non-prompting store, and is what
 * the networking layer reads on every call (LLM API keys, refresh tokens) without
 * an interactive prompt.
 *
 * <p>DPAPI is the Windows user-account authentication boundary, so there is no
 * separate biometric gate on the desktop: the biometric-prompting overloads map
 * to the same store and complete without an interactive prompt. (A Windows Hello
 * gate can layer on top once biometric support lands.)</p>
 */
public class WindowsSecureStorage extends SecureStorage {
    /// The storage name this account's ciphertext lives under.
    ///
    /// Namespaced by the application, because Storage on this platform is not. Entries land in
    /// the Codename One directory shared by every native desktop application under this user
    /// account, so two applications that both asked for a managed key under the same alias
    /// derived the same account name and read each other's key -- and forgetting it in either one
    /// removed the other's only copy, leaving a database that nothing can open. The application
    /// home directory is keyed by package for exactly this reason; this is the same boundary, in
    /// the one namespace that did not have it.
    private static String key(String account) {
        return "cn1securestorage_" + applicationNamespace() + "_" + account;
    }

    /// The name an entry was written under before the namespace existed.
    ///
    /// Read from, never written to. What is sitting here is an earlier build's data, and for a
    /// managed database key it is the only copy there is.
    private static String legacyKey(String account) {
        return "cn1securestorage_" + account;
    }

    /// Moves an entry an earlier build wrote into the namespaced name, and reports what it found.
    ///
    /// The stored bytes are DPAPI ciphertext bound to this Windows user, so moving them is a copy
    /// rather than a re-encryption. Copied before the old name is dropped: a delete that ran first
    /// and then failed to write would destroy a key that cannot be regenerated.
    private static Object adoptLegacyEntry(String account) {
        Storage storage = Storage.getInstance();
        Object stored;
        try {
            if (!storage.exists(legacyKey(account))) {
                return null;
            }
            stored = storage.readObject(legacyKey(account));
        } catch (Throwable cannotRead) {
            return null;
        }
        if (!(stored instanceof byte[])) {
            return null;
        }
        try {
            if (storage.writeObject(key(account), stored)) {
                storage.deleteStorageFile(legacyKey(account));
            }
        } catch (Throwable cannotMove) {
            // The value is still readable under its old name, which is what the caller is given.
        }
        return stored;
    }

    /* -------------------------------------------------- non-prompting API */

    @Override
    public boolean set(String account, String value) {
        if (account == null || value == null) {
            return false;
        }
        try {
            byte[] enc = WindowsNative.dpapiProtect(value.getBytes("UTF-8"));
            if (enc == null) {
                return false;
            }
            if (!Storage.getInstance().writeObject(key(account), enc)) {
                return false;
            }
            // Only after the new one is stored. A stale entry left under the old name would be
            // read back by adoptLegacyEntry the moment the namespaced one went missing.
            Storage.getInstance().deleteStorageFile(legacyKey(account));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public int entryState(String account) {
        if (account == null) {
            return ENTRY_UNKNOWN;
        }
        try {
            // Whether the token is stored, not whether the secret behind it can be fetched. A
            // keyring that is briefly unavailable leaves the token exactly where it was, and
            // reporting the entry absent then is what would let a caller overwrite the key.
            Storage storage = Storage.getInstance();
            if (storage.exists(key(account))) {
                return ENTRY_PRESENT;
            }
            // Absent under the namespaced name is not absent: an earlier build wrote it without
            // one, and reporting nothing here is what would have ManagedKeys generate a second
            // key over a database the first one encrypted.
            return storage.exists(legacyKey(account)) ? ENTRY_PRESENT : ENTRY_ABSENT;
        } catch (Throwable cannotAsk) {
            return ENTRY_UNKNOWN;
        }
    }

    @Override
    public String get(String account) {
        if (account == null) {
            return null;
        }
        try {
            Object o = Storage.getInstance().readObject(key(account));
            if (!(o instanceof byte[])) {
                o = adoptLegacyEntry(account);
            }
            if (!(o instanceof byte[])) {
                return null;
            }
            byte[] dec = WindowsNative.dpapiUnprotect((byte[]) o);
            if (dec == null) {
                return null;
            }
            return new String(dec, "UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean remove(String account) {
        if (account == null) {
            return false;
        }
        // Checked, because deleteStorageFile cannot report anything: it returns void and the
        // DeleteFileW under it has its result dropped. The file is the DPAPI ciphertext itself,
        // so a delete that quietly failed -- the file open without delete sharing, or carrying
        // the read-only attribute -- left a secret on disk that the same Windows user can still
        // decrypt, while forgetManagedKey() reported the key forgotten. Reading the entry back
        // is the only answer available here, and an entry that is still there is a failure.
        Storage storage = Storage.getInstance();
        storage.deleteStorageFile(key(account));
        // The unnamespaced entry as well, or a caller that was told the key was forgotten would
        // still have it on disk under the name an earlier build used -- and get() would read it
        // back.
        storage.deleteStorageFile(legacyKey(account));
        return !storage.exists(key(account)) && !storage.exists(legacyKey(account));
    }

    /* ----------------------------------------- prompting (AsyncResource) API
     * Mapped to the same DPAPI store; DPAPI already binds the secret to the
     * Windows user account, so no extra interactive prompt is shown. */

    @Override
    public AsyncResource<Boolean> set(String reason, String account, String value) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(Boolean.valueOf(set(account, value)));
        return r;
    }

    @Override
    public AsyncResource<String> get(String reason, String account) {
        AsyncResource<String> r = new AsyncResource<String>();
        String v = get(account);
        if (v != null) {
            r.complete(v);
        } else {
            r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "No secure-storage entry for " + account));
        }
        return r;
    }

    @Override
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(Boolean.valueOf(remove(account)));
        return r;
    }
}
