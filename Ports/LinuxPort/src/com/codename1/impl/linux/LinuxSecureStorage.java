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
    private static String key(String account) {
        return "cn1securestorage_" + account;
    }

    /* -------------------------------------------------- non-prompting API */

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
            return Storage.getInstance().writeObject(key(account), enc);
        } catch (Throwable t) {
            return false;
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
        Storage.getInstance().deleteStorageFile(key(account));
        return true;
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
