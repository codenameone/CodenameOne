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
package com.codename1.impl.linux;

import com.codename1.io.Storage;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.SecureStorage;
import com.codename1.util.AsyncResource;

/**
 * Linux secure storage backed by the OS Data Protection API (DPAPI). Each
 * value is encrypted with {@link LinuxNative#dpapiProtect} -- a key derived
 * from the current Linux user's logon, so the ciphertext is decryptable only
 * by that user on this machine -- and the encrypted blob is persisted through
 * the normal Codename One {@link Storage}. This is the desktop analog of the iOS
 * keychain / Android EncryptedSharedPreferences non-prompting store, and is what
 * the networking layer reads on every call (LLM API keys, refresh tokens) without
 * an interactive prompt.
 *
 * <p>DPAPI is the Linux user-account authentication boundary, so there is no
 * separate biometric gate on the desktop: the biometric-prompting overloads map
 * to the same store and complete without an interactive prompt. (A Linux Hello
 * gate can layer on top once biometric support lands.)</p>
 */
public class LinuxSecureStorage extends SecureStorage {
    /// The storage name this account's token lives under.
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

    /// Moves a token an earlier build wrote into the namespaced name, and reports what it found.
    ///
    /// The token names an entry in the user's Secret Service, so moving it is a copy of the token
    /// rather than anything touching the secret. Copied before the old name is dropped: a delete
    /// that ran first and then failed to write would strand a secret in the keyring with nothing
    /// left to name it.
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
            // The token is still readable under its old name, which is what the caller is given.
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
            lock = LinuxNative.fileLockExclusive(gatePath(account));
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
                LinuxNative.fileUnlock(lock);
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
            byte[] enc = LinuxNative.dpapiProtect(value.getBytes("UTF-8"));
            if (enc == null) {
                return false;
            }
            if (!Storage.getInstance().writeObject(key(account), enc)) {
                return false;
            }
            // The old token stays where it is, and this application stops looking at it. Removing
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
    /// forgotten would come straight back from the token it was taken from.
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
        return LinuxNative.storageDir() + "/" + gateName(account);
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
            byte[] dec = LinuxNative.dpapiUnprotect((byte[]) o);
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
        // The keyring entry first, then the token that names it. What is stored here is a token;
        // the secret itself is in the Secret Service, so deleting the token alone made the value
        // unreachable through this class while leaving it in the user's keyring -- recoverable,
        // and an orphan for every key ever forgotten. For a forgotten database key that is the
        // difference between erased and merely hidden.
        //
        // In this order because the token is the only way to find the entry: dropping it first
        // would leave nothing to clear the keyring with.
        Object stored;
        try {
            stored = Storage.getInstance().readObject(key(account));
            if (!(stored instanceof byte[])) {
                // Written before the namespace existed. Adopted rather than ignored, so what
                // follows forgets the keyring entry it names instead of leaving it behind under a
                // token this call is about to stop looking at.
                stored = adoptLegacyEntry(account);
            }
        } catch (Throwable cannotRead) {
            // The token cannot be read, so the keyring entry cannot be found. Deleting the stored
            // object would throw away the only name it has, so the object stays and this reports
            // failure: there is a secret in the keyring and this call did not remove it.
            return false;
        }
        if (stored instanceof byte[]) {
            int forgotten = LinuxNative.dpapiForget((byte[]) stored);
            if (forgotten < 0) {
                // The keyring refused or could not be asked. The token is kept deliberately -- it
                // is the only way to find that entry again, so deleting it here would strand a
                // recoverable secret with nothing left to name it, which is the state this method
                // exists to avoid. Reported as a failure rather than a success that did half of
                // the work.
                return false;
            }
            // 0 means nothing was there, which is what a second forget sees; the token still goes.
        }
        // Checked for the same reason the keyring result is: deleteStorageFile returns void, so
        // a token that could not be deleted would report a clean removal. The token is not the
        // secret here -- that is out of the keyring already -- but entryState() reads exactly
        // this file, so a surviving token answers ENTRY_PRESENT for a key that is gone, and
        // ManagedKeys then refuses to generate a replacement because it believes one exists.
        // The database that key belonged to would never open again.
        Storage storage = Storage.getInstance();
        storage.deleteStorageFile(key(account));
        // The unnamespaced token as well, or a caller that was told the key was forgotten would
        // still have it under the name an earlier build used.
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
     * Linux user account, so no extra interactive prompt is shown. */

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
