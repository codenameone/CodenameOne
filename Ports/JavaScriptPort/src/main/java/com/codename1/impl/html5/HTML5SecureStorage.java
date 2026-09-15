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

    /// Where the PREVIOUS version of this port put a plaintext entry, which is the raw name.
    ///
    /// Not escaped, and that asymmetry is deliberate. Nothing here ever writes a legacy entry --
    /// they are read and deleted only -- so this has to address what is already on disk, and what
    /// is on disk was written by a version that passed the raw name to Storage. Escaping it made
    /// every existing credential and managed database key invisible on the first launch after an
    /// upgrade. Two historical accounts that normalization folded together still share one legacy
    /// entry, which is what they have always done; the escape below stops that happening to
    /// anything written from now on.
    private static String legacyKey(String account) {
        return LEGACY_PREFIX + account;
    }

    private static String encryptedKey(String account) {
        return ENCRYPTED_PREFIX + escaped(account);
    }

    /// An account name reduced to characters a storage key keeps.
    ///
    /// Storage.fixFileName rewrites '/', '\\', '%', '?', '*', ':' and '=' to '_' when
    /// normalizeNames is on, which is the default -- so `api/token` and `api_token` addressed ONE
    /// entry. The second set() overwrote the first account's ciphertext, and because the AAD
    /// still names the original account every later read of the first failed authentication,
    /// while remove() for either took out both. The vault escapes its own names for exactly this
    /// and the scheme is the same: anything outside [A-Za-z0-9] and '.' becomes `_XXXX`, which
    /// fixFileName leaves alone and which no two distinct names can collide on.
    ///
    /// Copied rather than shared: the vault's copy is private to a different module, and widening
    /// something into public API to avoid ten lines here would be the worse trade.
    private static String escaped(String account) {
        StringBuilder b = new StringBuilder(account.length());
        for (int iter = 0; iter < account.length(); iter++) {
            char c = account.charAt(iter);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.';
            if (safe) {
                b.append(c);
            } else {
                b.append('_');
                b.append(HEX.charAt((c >> 12) & 0x0f));
                b.append(HEX.charAt((c >> 8) & 0x0f));
                b.append(HEX.charAt((c >> 4) & 0x0f));
                b.append(HEX.charAt(c & 0x0f));
            }
        }
        return b.toString();
    }

    /// One entry, read past Storage's process-local cache.
    ///
    /// readObject answers that cache before it looks at storage and nothing another context
    /// writes can invalidate it -- so the re-reads this migration makes to decide whether the
    /// plaintext is still the value it set out with were answering from the copy THIS tab had
    /// already taken. Another tab replacing the value was invisible, and the deletion at the end
    /// then removed the replacement, leaving only the encrypted copy of the value it superseded.
    private static Object readUncached(String name) {
        if (!Storage.getInstance().exists(name)) {
            return null;
        }
        java.io.InputStream in = null;
        try {
            in = Storage.getInstance().createInputStream(name);
            return com.codename1.io.Util.readObject(new java.io.DataInputStream(in));
        } catch (java.io.IOException cannotRead) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                    // Nothing left to do with it.
                }
            }
        }
    }

    /// Creates an entry only if the account has none, atomically across tabs.
    ///
    /// The inherited implementation checks and then writes, which two tabs can both pass -- and
    /// in a browser that is not a narrow window: opening the same managed database in two tabs
    /// does it. Each tab then generates a key, both write, and Storage hands each tab back its
    /// OWN cached ciphertext, so each proceeds under a key only one of them persisted. The
    /// database the loser created cannot be opened after a reload.
    ///
    /// IndexedDB's `add` is the primitive that settles it: the store accepts exactly one record
    /// for an id and refuses the rest with a ConstraintError, so the loser re-reads and adopts
    /// the winner's value. The device key and the passkey record already converge this way; this
    /// is the same move for an ordinary account.
    ///
    /// Answers null when the entry could not be created and could not be read either, which is
    /// the inherited contract.
    @Override
    public String setIfAbsent(String account, String value) {
        if (account == null || value == null) {
            return null;
        }
        String existing = get(account);
        if (existing != null) {
            return existing;
        }
        try {
            // Sealed first: what the store settles on has to be the ciphertext, or two tabs
            // would converge on one record and disagree about what it decrypts to.
            String sealed = seal(account, value);
            byte[] answer = nativeSetIfAbsent(encryptedKey(account), sealed);
            if (answer == null || answer.length == 0 || answer[0] != STATUS_OK) {
                return null;
            }
            // The SETTLED record comes back, which is this tab's ciphertext only if it won.
            String settled = new String(answer, 1, answer.length - 1, "UTF-8");
            // Mirrored into ordinary storage, which is the namespace every other operation on
            // this class reads -- get, set, remove, entryState. The bridge record is a GATE and
            // not the value: leaving the ciphertext only there made a created entry invisible to
            // all of them, so Database.forgetManagedKey saw an existing managed key as absent and
            // could never delete it, and remove() reported success while the entry lived on.
            //
            // Both tabs write the same settled bytes, so this is idempotent however the race
            // went: the loser is writing the winner's record, not its own. A set() landing
            // after the re-read above still wins in the gate store and loses here, which is
            // two concurrent writes resolving last-write-wins rather than a create discarding
            // a value it was told about -- that one is closed by set() settling in the gate.
            if (!Storage.getInstance().writeObject(encryptedKey(account), settled)) {
                // The gate goes with it. The add succeeded, so the ciphertext is settled in the
                // gate store; leaving it there while reporting failure means the entry is absent
                // from every namespace a read uses AND present in the one a create consults, so
                // the NEXT setIfAbsent loses its own add, reads this record back, and makes the
                // value from a call that reported failure visible as though it had been stored.
                // Released only when this tab is the one that won -- the loser is looking at
                // somebody else's live record and must not delete it.
                if (sealed.equals(settled)) {
                    try {
                        nativeForget(encryptedKey(account));
                    } catch (RuntimeException noBridge) {
                        Log.p("SecureStorage could not release the create gate it had just "
                                + "taken: " + reasonOf(noBridge), Log.WARNING);
                    }
                }
                return null;
            }
            String plain = open(account, settled);
            if (plain != null) {
                // The plaintext entry a previous version may have left goes only once the
                // encrypted one is settled and readable, and only when it is the same value.
                Object stale = readUncached(legacyKey(account));
                if (stale instanceof String && plain.equals(stale)) {
                    Storage.getInstance().deleteStorageFile(legacyKey(account));
                }
            }
            return plain;
        } catch (RuntimeException failed) {
            Log.p("SecureStorage create refused: " + reasonOf(failed), Log.WARNING);
            return null;
        } catch (java.io.UnsupportedEncodingException noUtf8) {
            return null;
        }
    }

    /// Adds one record if its id is free, and answers the record that is there either way.
    ///
    /// Status byte first, then the settled ciphertext as UTF-8 -- the same shape every native in
    /// HTML5DeviceProtection uses, and for the same reason: a browser has several distinct ways
    /// to refuse and the Java side has to tell them apart.
    static native byte[] nativeSetIfAbsent(String entry, String sealed);

    /// Writes one record into the same store the create gate uses, replacing what was there.
    ///
    /// An ordinary set() used to write ordinary Storage only. A tab paused inside setIfAbsent --
    /// past its "nothing here" check -- could then create the gate afterwards and mirror its own
    /// candidate over the value this call had already stored, which is the lost update the
    /// atomic create exists to rule out. With both writers settling in one store the create's
    /// re-read sees the newer record and adopts it.
    static native byte[] nativeSet(String entry, String sealed);

    /// Releases the gate for one entry, so a later create can win it again.
    ///
    /// Without this, remove() would clear the value while the gate still held the old ciphertext,
    /// and the next setIfAbsent would answer with a credential the caller had forgotten.
    static native byte[] nativeForget(String entry);

    /// Whether a gate native reported success.
    ///
    /// Distinct from "it threw": a build whose bridge predates these natives has no gate store
    /// at all, so there is nothing for a caller to keep consistent and the ordinary Storage
    /// write stands on its own. A bridge that answered and refused is the opposite case -- the
    /// store is there and now disagrees with Storage -- and every caller treats that as failure.
    private static boolean gateAccepted(byte[] answer) {
        return answer != null && answer.length > 0 && answer[0] == STATUS_OK;
    }

    /// The status byte a native prefixes its payload with when it succeeded.
    private static final byte STATUS_OK = 0;

    private HTML5DeviceProtection device() {
        return HTML5DeviceProtection.getInstance();
    }

    public boolean set(String account, String value) {
        if (account == null || value == null) {
            return false;
        }
        try {
            String sealed = seal(account, value);
            // The gate store first, so a setIfAbsent running concurrently in another tab sees
            // this value when it re-reads the settled record and adopts it instead of mirroring
            // its own candidate over it. Ordinary Storage is the mirror; this is where the two
            // writers meet.
            try {
                if (!gateAccepted(nativeSet(encryptedKey(account), sealed))) {
                    // The bridge is there and refused, so the gate now holds the superseded
                    // ciphertext. Writing only the mirror would leave the next setIfAbsent able
                    // to resurrect it.
                    Log.p("SecureStorage: the create gate refused this write", Log.WARNING);
                    return false;
                }
            } catch (RuntimeException noBridge) {
                // No gate store in this build, so nothing can diverge from one. See gateAccepted.
                Log.p("SecureStorage could not settle the create gate: " + reasonOf(noBridge),
                        Log.WARNING);
            }
            if (!Storage.getInstance().writeObject(encryptedKey(account), sealed)) {
                // The gate goes with it, for the reason setIfAbsent gives. The put succeeded, so
                // the new ciphertext is settled in the gate store while this call reports failure
                // -- and a later setIfAbsent loses its own add against that record, mirrors it
                // into ordinary storage, and makes the value from a set() the caller was told did
                // not happen visible as though it had. Deleting rather than restoring, because
                // put overwrote what was there and this class does not hold the previous
                // ciphertext; what remains is the ordinary entry, which is the pre-call state
                // every read already sees.
                //
                // These two are the whole set: nativeSet here and nativeSetIfAbsent in the create
                // are the only writers of the gate store, and nativeForget is the only remover.
                try {
                    nativeForget(encryptedKey(account));
                } catch (RuntimeException noBridge) {
                    Log.p("SecureStorage could not release the create gate after a failed write: "
                            + reasonOf(noBridge), Log.WARNING);
                }
                return false;
            }
            // The legacy entry goes only after the encrypted one is in place. A store that failed
            // the write above and had already lost the plaintext would have destroyed the value.
            Storage.getInstance().deleteStorageFile(legacyKey(account));
            // Checked, because deleteStorageFile cannot report: it returns void, and this port's
            // implementation catches the IndexedDB failure and returns. Answering true there told
            // the caller its secret had been written securely while the OLD one was still sitting
            // in browser storage as plaintext. remove() already checks this way.
            if (Storage.getInstance().exists(legacyKey(account))) {
                Log.p("SecureStorage: the plaintext entry this replaced could not be removed",
                        Log.WARNING);
                return false;
            }
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
            // Only when it is the value this ciphertext holds. An older service worker or tab
            // can write a NEWER plaintext entry after this version created the encrypted one, and
            // deleting it here unconditionally threw that update away for good while get() went
            // on answering the older ciphertext. Read past the cache for the same reason migrate
            // does: the copy this tab took earlier is precisely what hides the other tab's write.
            Object stale = readUncached(legacyKey(account));
            if (stale instanceof String && plaintext.equals(stale)) {
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
            Object stillPlain = readUncached(legacyKey(account));
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
            Object beforeDelete = readUncached(legacyKey(account));
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
        // The gate as well, or the value is gone while the record that settled it remains -- and
        // the next setIfAbsent would hand back a credential this call was told to forget.
        try {
            // Checked. The bridge answers a refused delete with a status byte rather than
            // throwing, so ignoring it let remove() report success over a gate that still held
            // the old ciphertext -- and the next setIfAbsent would read that record back and
            // write the forgotten credential or managed database key into ordinary storage
            // again. Reporting failure is what makes the caller try again rather than believe
            // the secret is gone.
            if (!gateAccepted(nativeForget(encryptedKey(account)))) {
                Log.p("SecureStorage: the create gate for this entry could not be released",
                        Log.WARNING);
                return false;
            }
        } catch (RuntimeException noBridge) {
            // A build whose bridge predates this native, which therefore has no gate store and
            // never created one. The entry is still removed from the namespace every read uses,
            // which is what this method promises. See gateAccepted.
            Log.p("SecureStorage could not release the create gate: " + reasonOf(noBridge),
                    Log.WARNING);
        }
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
