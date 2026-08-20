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
        if (isAdopted(account)) {
            return null;
        }
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
                markAdopted(account);
            }
        } catch (Throwable cannotMove) {
            // The value is still readable under its old name, which is what the caller is given.
        }
        return stored;
    }

    /* -------------------------------------------------- non-prompting API */

    /**
     * Creates an entry through a gate the operating system decides, so two processes cannot both
     * create one.
     *
     * <p>The inherited implementation checks and then writes, which is two operations: two runs of
     * this application doing their first managed open can both find nothing stored, generate
     * different keys and each overwrite the other, after which the database is encrypted with a
     * key that no longer exists. Reading back afterwards does not close that -- both callers read
     * their own write.</p>
     *
     * <p>{@code fileCreateExclusive} is the one operation here that cannot be won twice. The
     * caller that creates the gate file stores the value; the caller that finds it already there
     * reads what the winner stored, waiting briefly for it to land, and never writes.</p>
     *
     * @param account the account to create
     * @param value the value to store when there is none
     * @return the value now stored, which may be another process's, or null if it could not be
     *   stored
     */
    @Override
    public String setIfAbsent(String account, String value) {
        if (account == null || value == null) {
            return null;
        }
        String existing = get(account);
        if (existing != null) {
            return existing;
        }
        long lock;
        try {
            lock = WindowsNative.fileLockExclusive(gatePath(account));
        } catch (Throwable cannotAsk) {
            return super.setIfAbsent(account, value);
        }
        if (lock == 0) {
            // The lock could not be taken, which says nothing about the entry -- so this falls
            // back rather than reporting an absence it has not established.
            return super.setIfAbsent(account, value);
        }
        try {
            // Asked again inside the lock. Whoever held it before may have created the key, and
            // the answer to that is theirs rather than a second one of ours.
            String stored = get(account);
            if (stored != null) {
                return stored;
            }
            return set(account, value) ? value : null;
        } finally {
            try {
                WindowsNative.fileUnlock(lock);
            } catch (Throwable cannotRelease) {
                // The operating system drops it when this process ends, whatever happens here.
            }
        }
    }


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
            // The old entry stays where it is, and this application stops looking at it. Removing
            // it would take it from every other application that shared it: one account name under
            // one OS user is exactly what the namespace separates, so applications that have not
            // upgraded yet still need to find their key there.
            markAdopted(account);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /// Records that this application has taken its own copy of the shared entry.
    ///
    /// The shared entry is never removed -- other applications may still need it -- so something
    /// has to stop this one reading it again after it has its own, or a key that was just
    /// forgotten would come straight back from the copy it was taken from.
    private static void markAdopted(String account) {
        try {
            Storage.getInstance().writeObject(adoptedKey(account), Boolean.TRUE);
        } catch (Throwable cannotMark) {
            // Worst case this application adopts again, which is the same value.
        }
    }

    /// Whether this application has already taken its copy.
    private static boolean isAdopted(String account) {
        try {
            return Storage.getInstance().exists(adoptedKey(account));
        } catch (Throwable cannotAsk) {
            return false;
        }
    }

    /// The mark's own storage name, inside this application's namespace.
    private static String adoptedKey(String account) {
        return key(account) + "-adopted";
    }

    /// The file whose creation decides which caller stores this account.
    private static String gatePath(String account) {
        return WindowsNative.storageDir() + "\\\\" + gateName(account);
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
            // key over a database the first one encrypted. Once this application has taken its own
            // copy, though, the shared entry belongs to whoever else still needs it and is not
            // consulted again.
            if (isAdopted(account)) {
                return ENTRY_ABSENT;
            }
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
        // Marked, not deleted: another application may still be waiting to adopt it. The mark is
        // what stops this one reading it back and resurrecting a key the caller just forgot.
        markAdopted(account);
        // The lock file is deliberately left alone. It gates nothing by existing -- what excludes
        // a second writer is the lock held on it, which the system drops when the process ends --
        // and removing it while another process holds that lock would have the next caller create
        // a different file and lock that instead, which is two writers again.
        return !storage.exists(key(account));
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
