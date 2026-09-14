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

import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.security.SecureStorage;
import com.codename1.security.vault.Protection;
import com.codename1.security.vault.ProtectionReport;
import com.codename1.security.vault.VaultError;
import com.codename1.security.vault.VaultException;

/// The browser's tier of the non-prompting secure store, encrypted.
///
/// #### What changed, and why the old note was not enough
///
/// This used to write the value straight into ordinary CN1 `Storage` and say plainly that the
/// page could read it. The honesty was right and the behaviour was not: a managed database key is
/// exactly the sort of secret somebody assumes is protected, and "it is documented" is not a
/// control. What lands on disk is now AES-GCM ciphertext under a `CryptoKey` created with
/// `extractable: false` and kept in IndexedDB -- a key the page can use and cannot read out. See
/// [HTML5DeviceProtection] for what that does and does not buy.
///
/// The part that has not changed: **script running in this origin can still read every value
/// here**, by calling `get` exactly as the application does. The key being non-extractable stops
/// it being carried away, not used. There is no arrangement of Web Workers, non-extractable keys
/// or Content-Security-Policy that makes a browser store unreadable to code running in its own
/// origin, and anything that claims otherwise is describing a delay rather than a boundary.
///
/// What it does buy is the threat model SQLCipher answers on every other platform, which is the
/// one this store exists for: the storage pool, a profile backup or a copied database file is
/// ciphertext on its own.
///
/// #### An insecure origin is refused, not downgraded
///
/// Web Crypto does not exist outside a secure context, so on plain `http:` (other than
/// `localhost`) there is nothing to encrypt with. This **refuses the write** rather than falling
/// back to the plaintext entry it used to write. A caller sees `false` from
/// [#set(String, String)] and [com.codename1.db.DatabaseConfig] managed mode reports a key it
/// could not store, which is a failure the developer can act on. A silent plaintext write is one
/// they cannot.
///
/// Reads of pre-existing plaintext entries still work, in an insecure context included. Refusing
/// those would not protect anything and would lock an application out of data it already has.
///
/// #### Migration
///
/// Entries written by the previous version live under `cn1secure.<account>` as plaintext. The
/// first read of one rewrites it: the encrypted entry is written under `cn1secure.v2.<account>`,
/// **read back and decrypted**, and only then is the plaintext entry deleted. A crash anywhere in
/// that sequence is recoverable, because the next read prefers the encrypted entry and cleans up
/// whatever plaintext is still beside it.
///
/// Two things migration cannot do, both worth telling a user rather than implying away. The old
/// plaintext bytes may survive in a browser profile backup, an operating system snapshot or the
/// free blocks of the storage file; deleting the entry is not erasing it. And a service worker
/// serving an older version of the application can still write plaintext entries after the
/// migration -- see the cache and update rules in the JavaScript port's deployment guide.
public final class HTML5SecureStorage extends SecureStorage {

    /// Where the previous version wrote, and what migration reads from.
    private static final String LEGACY_PREFIX = "cn1secure.";

    /// Where encrypted entries live. A separate namespace rather than the same one, so a partly
    /// migrated store is a state that can be read rather than a guess about what a given blob is.
    ///
    /// **Disjoint from the legacy prefix, not nested inside it.** The obvious spelling was
    /// `cn1secure.v2.`, and it collides: the encrypted entry for account `foo` and the plaintext
    /// entry for an account literally named `v2.foo` are then the same storage key, so writing one
    /// destroys the other and a read cannot tell ciphertext from plaintext. `cn1secure2.` cannot
    /// be produced by `legacyKey` for any account, because that always has a `.` where this has a
    /// `2`.
    private static final String ENCRYPTED_PREFIX = "cn1secure2.";

    /// The device key every entry here is encrypted under. One key for the whole store: a key per
    /// entry would multiply the IndexedDB round trips and protect nothing extra, since anything
    /// that can reach one can reach them all.
    private static final String KEY_ID = "cn1.securestorage";

    private static String legacyKey(String account) {
        return LEGACY_PREFIX + account;
    }

    private static String encryptedKey(String account) {
        return ENCRYPTED_PREFIX + account;
    }

    private HTML5DeviceProtection device() {
        return HTML5DeviceProtection.getInstance();
    }

    public boolean set(String account, String value) {
        if (account == null || value == null) {
            return false;
        }
        try {
            String sealed = seal(account, value);
            if (!Storage.getInstance().writeObject(encryptedKey(account), sealed)) {
                return false;
            }
            // The legacy entry goes only after the encrypted one is in place. A store that failed
            // the write above and had already lost the plaintext would have destroyed the value.
            Storage.getInstance().deleteStorageFile(legacyKey(account));
            return true;
        } catch (RuntimeException failed) {
            // Not logged with the value, the account or any part of the ciphertext -- this line
            // reaches the browser console, which is the last place a secret should end up.
            Log.p("SecureStorage write refused: " + reasonOf(failed), Log.WARNING);
            return false;
        }
    }

    public String get(String account) {
        if (account == null) {
            return null;
        }
        Object sealed = Storage.getInstance().readObject(encryptedKey(account));
        if (sealed instanceof String) {
            // The cast is taken before the try rather than inside it: ParparVM does not throw
            // for a failed cast, so a cast under a catch is a handler that cannot run.
            String ciphertext = (String) sealed;
            String plaintext;
            try {
                plaintext = open(account, ciphertext);
            } catch (RuntimeException failed) {
                Log.p("SecureStorage read failed: " + reasonOf(failed), Log.WARNING);
                // Fall through to the legacy entry rather than answering null. Reaching here
                // means the encrypted copy did not open -- the IndexedDB key was cleared or is
                // momentarily unavailable, or the ciphertext is damaged -- and if an
                // interrupted migration left the plaintext beside it, that copy is the same
                // value and is the only one still readable. This ordering is the whole point:
                // the delete below used to run BEFORE this open, so a failure here had already
                // destroyed the only recoverable copy.
                return legacyValue(account);
            }
            // Proven readable, so the interrupted migration can be finished now and not sooner.
            if (Storage.getInstance().exists(legacyKey(account))) {
                Storage.getInstance().deleteStorageFile(legacyKey(account));
            }
            return plaintext;
        }
        Object legacy = Storage.getInstance().readObject(legacyKey(account));
        if (!(legacy instanceof String)) {
            // Read back through instanceof rather than a cast: a failed cast raises nothing
            // catchable on this runtime, and a storage entry that is not a string is a corrupt
            // one, not a crash.
            return null;
        }
        migrate(account, (String) legacy);
        return (String) legacy;
    }

    /// The plaintext entry alone, read without migrating it.
    ///
    /// Only reached when an encrypted entry exists and would not open, so migrating here would
    /// rewrite the ciphertext that just failed -- and the entry has to stay exactly where it is
    /// until something can actually read the encrypted copy again.
    private String legacyValue(String account) {
        Object legacy = Storage.getInstance().readObject(legacyKey(account));
        if (!(legacy instanceof String)) {
            return null;
        }
        return (String) legacy;
    }

    /// Rewrites one plaintext entry as an encrypted one, verifying before it deletes.
    ///
    /// Failure here is not failure of the read that triggered it: the caller still gets its
    /// value. An entry that could not be migrated is tried again on the next read, which is the
    /// right behaviour for a browser whose IndexedDB is briefly unavailable.
    private void migrate(String account, String value) {
        try {
            String sealed = seal(account, value);
            // Re-read before writing, because seal() is where this pauses and a browser runs
            // many tabs against one store. A tab that read the plaintext and then waited here
            // would write it over whatever arrived meanwhile: set() puts the new value in the
            // encrypted entry and removes the plaintext, so a token another tab had just
            // refreshed was silently replaced by the stale one this migration set out with.
            //
            // Two questions, both of them "is this still the entry I read?". The plaintext has
            // to still be the value being migrated, and no encrypted entry may have appeared --
            // get() only reaches here when there was none, so one now is newer than this by
            // construction. Neither is a compare-and-set: com.codename1.io.Storage has no such
            // primitive, so this narrows the window from the whole of seal() to the instructions
            // between the check and the write rather than closing it.
            Object stillPlain = Storage.getInstance().readObject(legacyKey(account));
            if (!(stillPlain instanceof String) || !value.equals(stillPlain)) {
                return;
            }
            if (Storage.getInstance().exists(encryptedKey(account))) {
                return;
            }
            if (!Storage.getInstance().writeObject(encryptedKey(account), sealed)) {
                return;
            }
            Object verify = Storage.getInstance().readObject(encryptedKey(account));
            if (!(verify instanceof String) || !value.equals(open(account, asString(verify)))) {
                // The encrypted copy does not read back as the original. Leave the plaintext
                // alone: it is the only correct copy there is.
                Storage.getInstance().deleteStorageFile(encryptedKey(account));
                return;
            }
            // Asked once more before the plaintext goes. It is the only copy of anything that
            // arrived after the check above, and deleting it there would lose that value
            // outright rather than merely deferring a migration.
            Object beforeDelete = Storage.getInstance().readObject(legacyKey(account));
            if (beforeDelete instanceof String && value.equals(beforeDelete)) {
                Storage.getInstance().deleteStorageFile(legacyKey(account));
            }
        } catch (RuntimeException failed) {
            Log.p("SecureStorage migration deferred: " + reasonOf(failed), Log.WARNING);
        }
    }

    public boolean remove(String account) {
        if (account == null) {
            return false;
        }
        Storage storage = Storage.getInstance();
        storage.deleteStorageFile(encryptedKey(account));
        storage.deleteStorageFile(legacyKey(account));
        // Checked, because deleteStorageFile cannot report anything: it returns void. A forgotten
        // key that is still there would leave entryState answering PRESENT for a key the caller
        // believes is gone, and ManagedKeys then refuses to generate a replacement.
        return !storage.exists(encryptedKey(account)) && !storage.exists(legacyKey(account));
    }

    public int entryState(String account) {
        if (account == null) {
            return ENTRY_UNKNOWN;
        }
        Storage storage = Storage.getInstance();
        // A definite answer either way, which is what lets ManagedKeys generate a first key at
        // all: it refuses unless the store can say the entry is genuinely absent. Both namespaces
        // are consulted, so a half-migrated entry never reads as absent.
        boolean present = storage.exists(encryptedKey(account)) || storage.exists(legacyKey(account));
        return present ? ENTRY_PRESENT : ENTRY_ABSENT;
    }

    public ProtectionReport protection() {
        ProtectionReport.Builder b = ProtectionReport.builder();
        ProtectionReport deviceReport = device().protection();
        b.set(Protection.PERSISTENT, deviceReport.answer(Protection.PERSISTENT));
        b.set(Protection.ENCRYPTED_AT_REST, deviceReport.answer(Protection.ENCRYPTED_AT_REST));
        b.set(Protection.NON_EXTRACTABLE_KEY, deviceReport.answer(Protection.NON_EXTRACTABLE_KEY));
        b.set(Protection.OS_PROTECTED, false);
        b.set(Protection.HARDWARE_BACKED, deviceReport.answer(Protection.HARDWARE_BACKED));
        b.set(Protection.USER_VERIFICATION, false);
        b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
        return b.build();
    }

    public ProtectionReport protectionOf(String account) {
        if (account != null && Storage.getInstance().exists(legacyKey(account))) {
            // A plaintext entry that has not been migrated yet. Reporting the store's capability
            // here would say this value is encrypted when it is sitting in the open.
            //
            // The legacy copy alone decides this, whether or not an encrypted one exists beside
            // it. An interrupted migration leaves both, and get() deliberately falls back to the
            // plaintext when the encrypted copy will not open -- so answering "encrypted"
            // because the encrypted entry is present would let a caller's ENCRYPTED_AT_REST
            // requirement be satisfied by a value that is then served from the open one. It
            // corrects itself: the first get() that opens the ciphertext deletes the plaintext,
            // and this answers encrypted from then on.
            ProtectionReport.Builder b = ProtectionReport.builder();
            b.set(Protection.PERSISTENT, true);
            b.set(Protection.ENCRYPTED_AT_REST, false);
            b.set(Protection.NON_EXTRACTABLE_KEY, false);
            b.set(Protection.OS_PROTECTED, false);
            b.set(Protection.HARDWARE_BACKED, false);
            b.set(Protection.USER_VERIFICATION, false);
            b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
            return b.build();
        }
        return protection();
    }

    /// A checked narrowing that does not rely on a cast raising anything.
    ///
    /// ParparVM's `CHECKCAST` is unchecked, so a failed cast hands the next instruction the wrong
    /// object rather than throwing -- which inside a `catch` means the handler never runs and the
    /// bad value is used. Anywhere a cast would otherwise sit under a catch here, it goes through
    /// this instead.
    private static String asString(Object value) {
        return value instanceof String ? (String) value : "";
    }

    /// The [VaultError] behind a failure, for a log line that names the cause and never the value.
    ///
    /// Every failure here arrives through [com.codename1.util.AsyncResource#get()], which wraps
    /// whatever went wrong in an `AsyncExecutionException` -- so catching [VaultException]
    /// directly catches nothing, and the exception escapes a method whose contract is to return
    /// `null` or `false`. The cause chain is walked instead.
    private static String reasonOf(Throwable failed) {
        Throwable cause = failed;
        while (cause != null) {
            if (cause instanceof VaultException) {
                return ((VaultException) cause).getError().name();
            }
            cause = cause.getCause();
        }
        return VaultError.UNKNOWN.name();
    }

    /// Encrypts one entry, binding the ciphertext to the account it belongs to.
    ///
    /// The binding is what stops an attacker with write access to the storage pool moving an
    /// entry: the ciphertext stored under `api.token`, copied over `db.key`, fails to
    /// authenticate rather than decrypting into the wrong slot.
    private String seal(String account, String value) {
        HTML5DeviceProtection dev = device();
        Boolean ready = dev.ensureKey(KEY_ID).get();
        if (ready == null || !ready.booleanValue()) {
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "the browser could not establish the key this store encrypts with");
        }
        byte[] plain = utf8(value);
        try {
            byte[] wrapped = dev.wrap(KEY_ID, plain, utf8(account)).get();
            return toHex(wrapped);
        } finally {
            for (int iter = 0; iter < plain.length; iter++) {
                plain[iter] = 0;
            }
        }
    }

    private String open(String account, String sealed) {
        byte[] bytes = fromHex(sealed);
        if (bytes == null) {
            throw new VaultException(VaultError.CORRUPT,
                    "the stored entry is not in a format this build wrote");
        }
        byte[] plain = device().unwrap(KEY_ID, bytes, utf8(account)).get();
        return fromUtf8(plain);
    }

    // The port compiles against the CLDC class library, so the encoding helpers are written out
    // here rather than taken from a charset. They only ever see an account name and a value the
    // application supplied, both of which are ordinary strings.

    private static byte[] utf8(String value) {
        int length = value.length();
        byte[] out = new byte[length * 3];
        int at = 0;
        for (int iter = 0; iter < length; iter++) {
            int c = value.charAt(iter);
            if (c < 0x80) {
                out[at++] = (byte) c;
            } else if (c < 0x800) {
                out[at++] = (byte) (0xc0 | (c >> 6));
                out[at++] = (byte) (0x80 | (c & 0x3f));
            } else {
                out[at++] = (byte) (0xe0 | (c >> 12));
                out[at++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                out[at++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        byte[] exact = new byte[at];
        System.arraycopy(out, 0, exact, 0, at);
        return exact;
    }

    private static String fromUtf8(byte[] data) {
        StringBuilder b = new StringBuilder(data.length);
        int at = 0;
        while (at < data.length) {
            int first = data[at++] & 0xff;
            if (first < 0x80) {
                b.append((char) first);
            } else if ((first & 0xe0) == 0xc0 && at < data.length) {
                b.append((char) (((first & 0x1f) << 6) | (data[at++] & 0x3f)));
            } else if (at + 1 < data.length) {
                int second = data[at++] & 0x3f;
                int third = data[at++] & 0x3f;
                b.append((char) (((first & 0x0f) << 12) | (second << 6) | third));
            }
        }
        return b.toString();
    }

    private static String toHex(byte[] data) {
        StringBuilder b = new StringBuilder(data.length * 2);
        for (int iter = 0; iter < data.length; iter++) {
            int v = data[iter] & 0xff;
            b.append(HEX.charAt(v >>> 4));
            b.append(HEX.charAt(v & 0x0f));
        }
        return b.toString();
    }

    private static byte[] fromHex(String hex) {
        if ((hex.length() & 1) != 0) {
            return null;
        }
        byte[] out = new byte[hex.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            int hi = HEX.indexOf(Character.toLowerCase(hex.charAt(iter * 2)));
            int lo = HEX.indexOf(Character.toLowerCase(hex.charAt(iter * 2 + 1)));
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[iter] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static final String HEX = "0123456789abcdef";
}
