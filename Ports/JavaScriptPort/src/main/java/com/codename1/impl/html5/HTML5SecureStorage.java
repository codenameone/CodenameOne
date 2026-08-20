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
package com.codename1.impl.html5;

import com.codename1.io.Storage;
import com.codename1.security.SecureStorage;

/// The browser's tier of the non-prompting secure store.
///
/// There is no key store in a browser. What this offers is persistence, not protection from the
/// page: the value is written to the same origin-private storage the rest of the port uses, and
/// any script running on this origin can read it -- including anything an XSS gets to run. That
/// is weaker than every other port, where the secret sits in the platform key store, and it is
/// stated here rather than implied because a managed database key is exactly the sort of secret
/// somebody would assume was protected.
///
/// What it is still worth: the database file in the storage pool is unreadable on its own. A copy
/// of the profile directory, a backup, or anything that reads the pool without running in the
/// page finds ciphertext. That is the threat model SQLCipher answers on any platform, and it is
/// the one this preserves for the browser.
///
/// Without this the browser had no store at all, which meant `DatabaseConfig.managed()` -- the
/// mode documented as the one to use when an application cannot prompt for a passphrase --
/// failed at every open with KEY_UNAVAILABLE, on a port that reports encryption as supported.
/// The alternatives were to leave that contradiction in place or to make the port report no
/// encryption at all, and neither is what an application asking for a managed key needs.
public final class HTML5SecureStorage extends SecureStorage {

    /// Namespaced so an application storing its own values cannot collide with a key.
    private static final String PREFIX = "cn1secure.";

    private static String key(String account) {
        return PREFIX + account;
    }

    @Override
    public boolean set(String account, String value) {
        if (account == null || value == null) {
            return false;
        }
        // The answer of the write itself. A managed key that was not stored is a database that
        // can never be opened again, so a store that quietly failed would be worse than one that
        // refuses.
        return Storage.getInstance().writeObject(key(account), value);
    }

    @Override
    public String get(String account) {
        if (account == null) {
            return null;
        }
        Object stored = Storage.getInstance().readObject(key(account));
        // Read back through instanceof rather than a cast: a failed cast raises nothing catchable
        // on this runtime, and a storage entry that is not a string is a corrupt one, not a crash.
        return stored instanceof String ? (String) stored : null;
    }

    @Override
    public boolean remove(String account) {
        if (account == null) {
            return false;
        }
        Storage storage = Storage.getInstance();
        storage.deleteStorageFile(key(account));
        // Checked, because deleteStorageFile cannot report anything: it returns void. A forgotten
        // key that is still there would leave entryState answering PRESENT for a key the caller
        // believes is gone, and ManagedKeys then refuses to generate a replacement.
        return !storage.exists(key(account));
    }

    @Override
    public int entryState(String account) {
        if (account == null) {
            return ENTRY_UNKNOWN;
        }
        // A definite answer either way, which is what lets ManagedKeys generate a first key at
        // all: it refuses unless the store can say the entry is genuinely absent.
        return Storage.getInstance().exists(key(account)) ? ENTRY_PRESENT : ENTRY_ABSENT;
    }
}
