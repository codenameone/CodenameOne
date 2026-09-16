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

import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.security.CryptoException;
import com.codename1.security.Hash;
import com.codename1.security.Hmac;
import com.codename1.security.SecureRandom;
import com.codename1.security.vault.spi.DeviceProtection;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/// A password-protected store an application opens once and then uses, without choosing a cipher,
/// a nonce, a KDF or a wrapping scheme.
///
/// #### What it is
///
/// One random 32 byte **data key** protects everything the vault holds. The data key itself is
/// never stored: what is stored are *wraps* of it -- a copy sealed under a key derived from the
/// user's password, optionally a copy sealed under a recovery code, and, if the user asked to be
/// remembered, a copy sealed by this device's key store or by a non-extractable browser
/// `CryptoKey`. Unlocking means unwrapping one of those copies. Changing the password rewrites
/// one wrap and touches no data; forgetting a device deletes one wrap and touches no data.
///
/// That separation is what makes cross-device synchronization work without sending anything
/// usable to a server. The record describing a vault -- the wraps included -- is ciphertext, and
/// the records the vault seals are ciphertext, so a sync server stores both and can read neither.
///
/// ```java
/// Vault vault = Vault.named("notes");
/// if (!vault.isEnrolled()) {
///     vault.enroll(password, new VaultOptions().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
/// } else {
///     try {
///         // Not isDone(): that answers whether the operation has FINISHED, and it is true
///         // for the ordinary KEY_MISSING this returns on a device that was never remembered.
///         vault.unlockRemembered().get();
///     } catch (RuntimeException notRemembered) {
///         vault.unlockWithPassword(password).get();
///     }
/// }
/// vault.putSecret("api.token", token).get();
/// ```
///
/// #### What it protects against, and what it does not
///
/// | Against | Protected |
/// | --- | --- |
/// | A stolen ciphertext file, a stolen database, a backup | Yes. Everything at rest is AEAD ciphertext under a key that is not stored beside it |
/// | A copied browser profile or device image, vault locked | Yes for [UnlockPolicy#SESSION_ONLY]. **No** for [UnlockPolicy#REMEMBER_DEVICE] in a browser: a full profile copy carries the IndexedDB the wrapping key lives in, and the key works in the copy |
/// | A copied browser profile, vault unlocked | No. The data key is in memory |
/// | Malicious script in the application's own origin, vault unlocked | **No.** It calls the same decrypt the application calls. Nothing in this package prevents that, and non-extractable keys and Web Workers do not either |
/// | Malicious script, vault locked | It cannot read the data, and it can wait, log keystrokes and read the password when the user next types it |
/// | A browser extension, or malware on the device | No |
/// | A compromised server serving new application code | No. New code runs in the origin and inherits everything the application has |
/// | Data already copied elsewhere | No. [#forgetDevice()] and [#destroyLocalData()] remove local access; they cannot reach a copy |
///
/// The row that gets misread is the second. "The database file alone is useless" and "a copy of
/// the whole profile is useless" are different claims, and only the first is true of a browser
/// with a remembered device. Say so to users rather than implying the stronger one.
///
/// #### Threads and the EDT
///
/// Every operation returns an [AsyncResource]. The expensive ones -- anything that derives a key
/// from a password, which is six hundred thousand iterations by default -- run on a background
/// thread, so calling `get()` on the result from the EDT parks the EDT through
/// [com.codename1.ui.CN#invokeAndBlock] rather than freezing it. Calls that only touch memory
/// complete before they return and `get()` on them is free.
///
/// Applications should serialize state-changing operations on a vault instance. Locking can
/// cancel work already running on a worker; generation checks share a monitor with key changes
/// so the cancellation is visible across threads. Ports where an application runs in more than
/// one process converge device-key creation (see [DeviceProtection#ensureKey]), and the metadata
/// counter detects another writer's changes.
public final class Vault {

    // ---------------------------------------------------------------- state

    private final String name;
    private final String application;
    private VaultOptions options = new VaultOptions();

    /// The unwrapped data key, non-null exactly while unlocked.
    private byte[] dataKey;

    /// Incremented by every lock. A handle or an in-flight operation compares against it so a
    /// result computed before a lock is not delivered after one.
    private int lockGeneration;

    /// Incremented whenever the data key is replaced, which is not the same event as a lock.
    ///
    /// A rotation leaves the vault open, so lockGeneration does not move -- and a KeyHandle
    /// handed out before it went on answering with the subkey derived from the SUPERSEDED data
    /// key. Sealing stayed self-consistent, because the handle stamps the version it was made at
    /// and open() walks the retired chain back to it, but mac() has no version to stamp: a tag
    /// produced by a stale handle after a rotation cannot be verified by any handle obtained
    /// afterwards, here or on another device, and nothing reports that.
    ///
    /// So a handle dies at a rotation exactly as it dies at a lock. The alternative -- resolving
    /// the live subkey on every use -- would silently change what getVersion() describes and what
    /// a tag from one handle means over its lifetime, which is a worse contract than a refusal
    /// the caller can see.
    private int keyGeneration;

    private long lastActivity;
    private boolean passwordNeedsRewrap;
    private VaultMetadata metadata;

    private static final String STORAGE_PREFIX = "cn1vault.";
    private static final String PURPOSE_PASSWORD = "wrap.password";
    private static final String PURPOSE_RECOVERY = "wrap.recovery";
    private static final String PURPOSE_DEVICE = "wrap.device";
    private static final String PURPOSE_RETIRED = "wrap.retired";
    private static final String PURPOSE_SECRET = "secret";
    private static final String PURPOSE_RECORD = "record";
    private static final String DATA_KEY_RECORD = "datakey";

    /// Vault is not set up on this device and has nothing stored.
    public static final int NOT_ENROLLED = 0;

    /// Vault exists and is locked.
    public static final int LOCKED = 1;

    /// Vault exists and is unlocked.
    public static final int UNLOCKED = 2;

    /// The store could not be read, so nothing is known. Callers must not enroll over this.
    public static final int STATE_UNKNOWN = -1;

    private Vault(String name) {
        this.name = name;
        this.application = applicationIdentity();
    }

    /// Returns the vault with this name, creating the object but not the vault.
    ///
    /// The name separates one vault from another within an application; it is part of the
    /// binding every envelope carries, so two vaults cannot open each other's records even with
    /// the same password.
    ///
    /// #### Parameters
    ///
    /// - `name`: a short stable name, e.g. `"notes"`. Must not be null or empty
    public static Vault named(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("a vault needs a name");
        }
        return new Vault(name);
    }

    /// The name this vault was opened with.
    public String getName() {
        return name;
    }

    /// Applies options -- required protections, KDF profile, unlock policy, auto lock.
    ///
    /// Call before [#enroll]. Changing the policy afterwards is [#setPolicy(UnlockPolicy)], which
    /// does the work of removing wraps the new policy forbids.
    public Vault configure(VaultOptions newOptions) {
        if (newOptions != null) {
            // Copied, not retained. VaultOptions is mutable and this class mutates it: setPolicy
            // writes the new policy through options.policy(...) and restores it in a finally. So
            // one options object configuring two vaults used to tie them together -- changing one
            // vault's policy silently changed what the other would enrol or import under next,
            // and a vault could begin persisting a device key with nothing having called
            // setPolicy on it.
            options = newOptions.copy();
        }
        return this;
    }

    // ------------------------------------------------------- capabilities

    /// What this device can actually provide, per unlock policy.
    ///
    /// Ask before offering the user a choice: a "remember this device" switch on a platform that
    /// cannot remember is worse than no switch.
    public VaultCapabilities capabilities() {
        // The base mechanism, independent of the current policy. Selecting the current policy's
        // gated variant for capabilities made protectionFor(REMEMBER_DEVICE) describe the passkey
        // rather than the store that policy would actually use -- in the browser, reporting
        // NON_EXTRACTABLE_KEY=NO from the passkey where the device key reports YES. A capability
        // query is about what each policy WOULD provide, and protectionFor selects the gated
        // variant itself for the one policy that needs it.
        return new VaultCapabilities(baseDeviceProtection(), canKeepAndSealARecord());
    }

    /// Whether this platform can do what a SESSION_ONLY vault needs: keep a record and seal it.
    ///
    /// Asked DIRECTLY rather than inferred from the device protection's report. Reading it off
    /// that report was the first attempt and it was wrong in both directions: a device store is
    /// not what session-only uses, so an application-supplied DeviceProtection that reports no
    /// persistence -- an unplugged hardware token, say -- refused a policy that never touches it,
    /// and on a port whose fallback secure store is unavailable the same thing happened while
    /// ordinary Storage and the cipher were both fine.
    ///
    /// Storage is asked through entryState, which is the tri-state added for exactly this kind
    /// of question: a browser whose IndexedDB is unusable answers UNKNOWN rather than pretending
    /// the entry is absent. The cipher is asked by doing the thing -- one small seal under a
    /// throwaway key -- because a platform either performs AES-GCM or it does not, and nothing
    /// short of trying distinguishes those on the browser.
    private boolean canKeepAndSealARecord() {
        Storage storage = Storage.getInstance();
        if (storage == null) {
            return false;
        }
        // A NAME OF ITS OWN, never this vault's record. Probing metadataKey() asked two
        // questions at once and answered the wrong one: a vault whose own entry cannot be looked
        // up -- a transient IndexedDB refusal -- is not a platform that cannot store, and
        // reporting the policy as unsupported there turned "retry, the store is busy" into
        // POLICY_NOT_MET, which an application reads as "this device will never do this".
        // A NAME PER PROBE. A fixed one collided with itself: two tabs or threads asking at once
        // both wrote it, and the first to finish read and deleted it before the second read its
        // own write -- so a perfectly healthy platform answered "session-only unsupported".
        byte[] probeKey = null;
        try {
            // Name generation and storage lookup can fail before any write, too. A capability
            // query must report the missing prerequisite rather than throw at its caller.
            String probe = STORAGE_PREFIX + "capability.probe." + Bytes.toHex(SecureRandom.bytes(8));
            if (storage.entryState(probe)
                    == com.codename1.impl.CodenameOneImplementation.STORAGE_ENTRY_UNKNOWN) {
                return false;
            }
            // A readable store may still refuse writes, so persist and read back a probe.
            try {
                if (!storage.writeObject(probe, "probe")) {
                    return false;
                }
                if (!"probe".equals(asString(readUncached(probe)))) {
                    return false;
                }
            } finally {
                // This query may run on every settings screen; do not accumulate probe entries.
                // Cleanup failures are handled by the same outer guard as write failures.
                storage.deleteStorageFile(probe);
            }
            probeKey = SecureRandom.bytes(32);
            SecureEnvelope.seal(probeKey, DATA_KEY_RECORD, 1,
                    AssociatedData.of(application, "probe", DATA_KEY_RECORD, "probe"),
                    probeKey);
            return true;
        } catch (RuntimeException cannotProbe) {
            return false;
        } finally {
            Bytes.zero(probeKey);
        }
    }

    /// What currently protects this vault on this device, as observed.
    ///
    /// This describes the state that exists, not the state that could: a vault enrolled
    /// [UnlockPolicy#SESSION_ONLY] reports no device key because there is none, on the same
    /// platform where [#capabilities()] says one is available.
    public ProtectionReport protection() {
        // ONE read of the device record, used for BOTH the provider below and the presence
        // further down. They used to be two independent uncached reads -- deviceProtection() goes
        // through getPolicy(), and deviceRecordState() reads again -- so another tab moving the
        // policy from REQUIRE_USER_VERIFICATION to REMEMBER_DEVICE between them left this
        // describing the settled record with the PASSKEY provider: USER_VERIFICATION=YES over a
        // record that now permits unattended unlock, which is the one claim a caller must be
        // able to trust.
        Object storedRecord;
        try {
            storedRecord = readUncached(deviceRecordKey());
        } catch (VaultException cannotRead) {
            // readUncached refuses when storage cannot say whether the entry is there, and this
            // method's whole contract is to DESCRIBE that condition rather than to fail on it --
            // the recordState == UNKNOWN branch below exists for exactly this. Throwing here meant
            // diagnostics and DatabaseConfig.effectiveKeyProtection got a synchronous exception
            // where they asked a question with an "unknown" answer available.
            storedRecord = null;
        }
        DeviceRecord snapshot = storedRecord instanceof String
                ? DeviceRecord.parse((String) storedRecord) : null;
        DeviceProtection device = deviceProtection(
                snapshot == null ? UnlockPolicy.SESSION_ONLY : snapshot.policy);
        ProtectionReport.Builder b = ProtectionReport.builder();
        // Three states, because there are three. `state() != NOT_ENROLLED` folded STATE_UNKNOWN --
        // a record that is present and could not be read -- into "enrolled", so an unreadable
        // vault reported PERSISTENT=YES and, with no device record, ENCRYPTED_AT_REST=YES. Those
        // were not observed: nothing here has seen what the record says. A caller asking
        // provides() then acted on protections that may not exist, which is the one thing this
        // report's three-state contract is for.
        int stored = state();
        int enrolled = stored == STATE_UNKNOWN ? ProtectionReport.UNKNOWN
                : (stored != NOT_ENROLLED ? ProtectionReport.YES : ProtectionReport.NO);
        b.set(Protection.PERSISTENT, enrolled);
        int recordState = recordStateOf(storedRecord, snapshot);
        if (recordState == ProtectionReport.UNKNOWN) {
            // A record is there and could not be read, so which mechanism protects this vault is
            // not known -- and neither branch below can be taken without claiming it is.
            b.set(Protection.ENCRYPTED_AT_REST, ProtectionReport.UNKNOWN);
            b.set(Protection.NON_EXTRACTABLE_KEY, ProtectionReport.UNKNOWN);
            b.set(Protection.OS_PROTECTED, ProtectionReport.UNKNOWN);
            b.set(Protection.HARDWARE_BACKED, ProtectionReport.UNKNOWN);
            b.set(Protection.USER_VERIFICATION, ProtectionReport.UNKNOWN);
            b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
            return b.build();
        }
        if (recordState == ProtectionReport.NO) {
            // No stored key, so the only thing at rest is ciphertext under a password-derived key.
            b.set(Protection.ENCRYPTED_AT_REST, enrolled);
            // No device wrap, so the only thing that can reopen this vault is the password. None
            // of the key-storage protections apply, because no key is stored.
            b.set(Protection.NON_EXTRACTABLE_KEY, false);
            b.set(Protection.OS_PROTECTED, false);
            b.set(Protection.HARDWARE_BACKED, false);
            b.set(Protection.USER_VERIFICATION, false);
        } else {
            ProtectionReport deviceReport = device.protection();
            // From the store: a wrapping key kept in the clear beside the ciphertext means the
            // records are encrypted and the protection is not.
            b.set(Protection.ENCRYPTED_AT_REST,
                    enrolled == ProtectionReport.YES
                            ? deviceReport.answer(Protection.ENCRYPTED_AT_REST)
                            : enrolled);
            b.set(Protection.NON_EXTRACTABLE_KEY, deviceReport.answer(Protection.NON_EXTRACTABLE_KEY));
            b.set(Protection.OS_PROTECTED, deviceReport.answer(Protection.OS_PROTECTED));
            b.set(Protection.HARDWARE_BACKED, deviceReport.answer(Protection.HARDWARE_BACKED));
            b.set(Protection.USER_VERIFICATION, deviceReport.answer(Protection.USER_VERIFICATION));
        }
        // Never yes. See the field note on Protection.
        b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
        return b.build();
    }

    // ------------------------------------------------------------- state

    /// Whether this vault has been set up on this device.
    public boolean isEnrolled() {
        // ONE call, compared twice. state() is not a pure read -- it runs checkAutoLock() and can
        // consult storage -- so two calls can disagree: an unlocked vault crossing its auto-lock
        // deadline between them answers UNLOCKED and then LOCKED, and both comparisons fail, so a
        // vault that is plainly enrolled reports that it is not. A storage read that recovers
        // between the two does the same thing through STATE_UNKNOWN.
        int now = state();
        return now == LOCKED || now == UNLOCKED;
    }

    /// Whether the data key is currently available.
    public synchronized boolean isUnlocked() {
        checkAutoLock();
        return dataKey != null;
    }

    /// One of [#NOT_ENROLLED], [#LOCKED], [#UNLOCKED] or [#STATE_UNKNOWN].
    public int state() {
        if (isUnlocked()) {
            return UNLOCKED;
        }
        try {
            return loadMetadata() == null ? NOT_ENROLLED : LOCKED;
        } catch (VaultException unreadable) {
            // A record that exists and cannot be parsed is not an absent one. Answering
            // NOT_ENROLLED here is how an application offers to enroll over a vault whose data it
            // would then be unable to open.
            return STATE_UNKNOWN;
        }
    }

    /// Whether the password wrap was written under a weaker KDF profile than [KdfProfile#current()].
    ///
    /// True only after a successful [#unlockWithPassword]. An application that sees this and
    /// still holds the password should call [#changePassword] with the same password on both
    /// sides, which rewraps under today's profile.
    public synchronized boolean passwordNeedsRewrap() {
        return passwordNeedsRewrap;
    }

    private synchronized void setPasswordNeedsRewrap(boolean value) {
        passwordNeedsRewrap = value;
    }

    // ---------------------------------------------------------- enrolment

    /// Sets up the vault for the first time.
    ///
    /// #### What happens, in this order
    ///
    /// 1. The required protections in [VaultOptions] are checked against what this device
    ///    provides. An unmet one fails here, before anything is written.
    /// 2. A random vault id and a random 32 byte data key are generated.
    /// 3. The data key is wrapped under the password and the record is written **and read back
    ///    and unwrapped**. Only a wrap that has been proven to reopen counts as written.
    /// 4. Only then, if the policy asks for it, is the device wrap created -- also written and
    ///    verified.
    ///
    /// The ordering is the recoverable one. A crash after step 3 leaves a vault the password
    /// opens; a crash between 3 and 4 leaves the same. The reverse order would leave a window in
    /// which the vault opens on this device and nowhere else, and no password can rescue it.
    ///
    /// #### Parameters
    ///
    /// - `password`: the user's password. Cleared by this method once it has been used
    ///
    /// - `opts`: options, or null for the defaults
    ///
    /// #### Returns
    ///
    /// a resource completing when the vault is set up and unlocked, or erroring with
    /// [VaultError#CONFLICT] if one already exists, [VaultError#POLICY_NOT_MET] if a required
    /// protection is missing, or [VaultError#STORAGE_UNAVAILABLE] if nothing could be written
    public AsyncResource<Boolean> enroll(final char[] password, VaultOptions opts) {
        if (opts != null) {
            // Copied, for the reason configure gives: this is the second way a caller's mutable
            // options object gets retained, and the two vaults tied together by it do not care
            // which door it came through.
            options = opts.copy();
        }
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    enrollNow(password, generation);
                    out.complete(Boolean.TRUE);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (CryptoException failed) {
                    out.error(new VaultException(VaultError.CRYPTO_UNAVAILABLE,
                            "the platform could not perform the cryptography enrolment needs",
                            failed));
                } catch (RuntimeException broke) {
                    // LAST, because CryptoException is a RuntimeException: put ahead of it and
                    // the specific handler above becomes unreachable, which javac refuses.
                    //
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(password);
                }
            }
        });
        return out;
    }

    private void enrollNow(char[] password, int generation) {
        requirePolicySupported(options.getPolicy());
        requireProtections();
        int existing = state();
        if (existing != NOT_ENROLLED) {
            throw new VaultException(VaultError.CONFLICT,
                    existing == STATE_UNKNOWN
                            ? "a vault record exists here and could not be read; refusing to "
                              + "overwrite it, because doing so would orphan whatever it protects"
                            : "this vault is already set up on this device");
        }
        VaultMetadata fresh = new VaultMetadata();
        fresh.vaultId = Bytes.toHex(SecureRandom.bytes(16));
        fresh.dataKeyId = DATA_KEY_RECORD;
        fresh.dataKeyVersion = 1;
        fresh.counter = 1;
        byte[] key = SecureRandom.bytes(32);
        try {
            fresh.passwordWrap = SecureEnvelope.sealWithPassword(password, options.getKdf(),
                    fresh.dataKeyId, fresh.dataKeyVersion,
                    wrapBinding(fresh, PURPOSE_PASSWORD), key);
            stampMac(fresh, null, key);
            // base null: this must be a creation, so a record appearing between the
            // NOT_ENROLLED check above and here is a losing race rather than something to
            // overwrite. As close to create-if-absent as Storage can express.
            commitMetadata(null, fresh);
            // Read back and open. A store that accepted a write and did not keep it -- an evicted
            // origin, a full disk, a quota refusal reported as success -- would otherwise be
            // discovered at the next launch, by a user who can no longer get in.
            VaultMetadata verified = loadMetadata();
            if (verified == null || verified.passwordWrap == null) {
                throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                        "the vault record did not survive being written");
            }
            byte[] proof = SecureEnvelope.parse(verified.passwordWrap)
                    .openWithPassword(password, wrapBinding(verified, PURPOSE_PASSWORD));
            if (!Bytes.constantTimeEquals(proof, key)) {
                Bytes.zero(proof);
                throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                        "the vault record did not read back as it was written");
            }
            Bytes.zero(proof);
            // Enrolment derives from the password and then verifies through storage, which is
            // seconds at the default profile -- ample room for an EDT caller's lifecycle
            // callback to lock in the middle. Publishing afterwards would leave the vault open
            // against an explicit request to close it. The record stays: enrolling is what the
            // call was for and it succeeded; what is refused is holding the key.
            requireSameGeneration(generation);
            // Confirmed still ours immediately before publishing. Two tabs can both pass the
            // NOT_ENROLLED check above, generate different data keys and both write: the
            // read-back proof catches the interleaving where the other tab wrote FIRST, but
            // not the one where it writes after we verified. Publishing there would leave
            // this tab unlocked under a key the stored record no longer describes, sealing
            // records that nothing can open after a restart.
            //
            // This narrows that window rather than closing it, and the honest reason is that
            // com.codename1.io.Storage has no compare-and-set: writeObject is the only
            // primitive, so an atomic create-if-absent cannot be expressed here for every
            // port. What makes the residue safe rather than silent is the wrap binding --
            // every record is sealed against this record's vaultId and dataKeyVersion, and
            // the losing tab's vaultId is a different 16 random bytes, so its records fail
            // authentication outright instead of decrypting into the wrong state.
            VaultMetadata settled = loadMetadata();
            if (settled == null || !settled.serialize().equals(verified.serialize())) {
                throw new VaultException(VaultError.CONFLICT,
                        "another session finished setting up this vault first; unlock with "
                        + "the password instead of enrolling again");
            }
            adoptSession(generation, verified, key);
            key = null;
            touch();
            // Asked AGAIN, after publishing. The check above is a read, and the publication that
            // follows it is not part of it: a second tab writing its own record in between left
            // this one unlocked and holding a key whose vaultId no persisted record names, with
            // enroll() reporting success. Every secret written afterwards seals against that
            // vaultId, reads back fine in this tab, and is permanently unrecoverable after a
            // reload -- silent data loss on a call that said it succeeded.
            //
            // This does NOT make enrolment atomic and cannot: com.codename1.io.Storage has
            // writeObject and no compare-and-set, so a create-if-absent is not expressible for
            // every port. What it does is make the LOSER find out. The window shrinks to the
            // instructions between this read and the throw, and the outcome changes from "report
            // success and lose data later" to "report CONFLICT now", which is the answer the
            // documentation already tells the caller how to handle: unlock with the password
            // instead of enrolling again.
            VaultMetadata afterPublish = loadMetadataFresh();
            if (afterPublish == null || !afterPublish.serialize().equals(verified.serialize())) {
                // Locked before reporting, so nothing holds a key the store no longer describes.
                lock();
                throw new VaultException(VaultError.CONFLICT,
                        "another session finished setting up this vault first; unlock with "
                        + "the password instead of enrolling again");
            }
            if (options.getPolicy() != UnlockPolicy.SESSION_ONLY) {
                try {
                    rememberNow(options.getPolicy());
                    // The enrolment above is committed -- the metadata is written and the key is
                    // published -- so a lock landing inside the prompt does not undo it; it
                    // invalidates only the wrap, which is withdrawn rather than left describing
                    // zeroes. requireSameGeneration ran before rememberNow and could not see a
                    // lock that arrived after it.
                    withdrawDeviceRecordIfLocked(generation);
                } catch (RuntimeException rememberFailed) {
                    // RuntimeException and not just VaultException: a port can throw rather than
                    // complete with an error, and that left the vault published exactly the same
                    // way. VaultException is one of these.
                    //
                    // Undone rather than left standing. The record and the live key were already
                    // published above, so a cancelled passkey prompt or a store that refused the
                    // key left the vault enrolled and open WITHOUT the policy that was asked for
                    // -- while enroll() reported failure. The documented answer to that failure
                    // is to try again with a weaker policy, and that attempt then hit CONFLICT
                    // against the record this call had quietly left behind.
                    //
                    // Safe to roll back only while the vault still protects nothing, and that is
                    // a claim about the DEVICE rather than about this call. The record has been
                    // readable by every session on this origin since it was committed above, and
                    // the step that failed is a passkey prompt -- seconds or minutes of it -- so
                    // another tab can unlock this vault and store secrets inside that window.
                    // Deleting the record then takes the only password wrap with it and orphans
                    // them permanently, on a call that was merely tidying up after itself.
                    //
                    // So the rollback asks first, and keeps the record when the answer is no. A
                    // retry with a weaker policy then reports CONFLICT rather than succeeding,
                    // which is the correct outcome once someone else is using the vault: unlock
                    // it, do not enrol it again.
                    if (vaultIsStillUntouched(verified)) {
                        Storage.getInstance().deleteStorageFile(metadataKey());
                        if (!definitelyGone(metadataKey())) {
                            // The delete is void-returning and both real ports can drop one
                            // silently, so rethrowing the original here told the caller its
                            // enrolment had failed over a vault that is still ENROLLED. The
                            // documented answer to that failure is to retry with a weaker
                            // policy, and that retry then hits CONFLICT for reasons the first
                            // error gave no hint of. The storage failure is the one that
                            // describes the state the device is actually in, and the original
                            // goes on as its cause.
                            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                                    "this vault's enrolment could not be undone after the setup "
                                    + "failed, so the vault is still enrolled on this device",
                                    rememberFailed);
                        }
                        try {
                            forgetEveryMechanism();
                        } catch (RuntimeException alsoFailed) {
                            // The mechanism may hold a half-made key. Nothing here can reach it,
                            // and reporting this instead of the original would name the wrong
                            // failure.
                        }
                    }
                    lock();
                    throw rememberFailed;
                }
            }
        } finally {
            Bytes.zero(key);
        }
    }

    // ------------------------------------------------------------ unlock

    /// Unlocks with the user's password.
    ///
    /// #### Parameters
    ///
    /// - `password`: the user's password, cleared by this method
    ///
    /// #### Returns
    ///
    /// a resource completing when the vault is unlocked, or erroring with
    /// [VaultError#AUTHENTICATION_FAILED] for a wrong password, [VaultError#KEY_MISSING] when
    /// there is no vault here, or [VaultError#TEMPORARILY_UNREADABLE] when the record could not
    /// be read
    public AsyncResource<Boolean> unlockWithPassword(final char[] password) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // Captured HERE, on the calling thread, and not inside the worker below.
        //
        // Reading it in the worker looks equivalent and is not: the worker may not be scheduled
        // until after a lock() has already run, and it would then read the post-lock value, agree
        // with itself, and publish the key into a vault the application had just closed. Measured
        // -- a test that locked between the call and the worker starting reopened the vault.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                // Owned here and released in the finally, exactly as unlockRemembered does. This
                // is the real data key from the moment the wrap opens, and every check after that
                // point can throw -- requireAuthenticRecord on an edited record, requireProtections
                // on a requirement the application added since enrolment. The catch cleared only
                // the password, so the vault stayed shut while the key it had just derived sat in
                // the heap until collection.
                byte[] key = null;
                try {
                    VaultMetadata meta = requireMetadata();
                    if (meta.passwordWrap == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "this vault has no password wrap");
                    }
                    SecureEnvelope envelope = SecureEnvelope.parse(meta.passwordWrap);
                    key = envelope.openWithPassword(password,
                            wrapBinding(meta, PURPOSE_PASSWORD));
                    if (generation != lockGeneration()) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while it was being unlocked");
                    }
                    requireAuthenticRecord(meta, key);
                    // Checked here too, not only at enrolment. A vault enrolled before the
                    // application started asking for anything reopened without ever meeting the
                    // requirements it was later configured with -- so require(...) governed the
                    // first launch and nothing afterwards, which is the opposite of what a
                    // requirement is for.
                    //
                    // Against the STORED policy, like unlockRemembered, not the configured one.
                    // What the requirement asks about is the protection this vault actually has,
                    // and that is a property of the mechanism it was enrolled with. Reading it
                    // from options got both directions wrong: a vault enrolled REMEMBER_DEVICE
                    // into the keychain was refused by a later require(OS_PROTECTED), because a
                    // caller that had not repeated the policy in its options left it at
                    // SESSION_ONLY, whose report says OS protection is absent -- and configuring
                    // a remembered policy on a vault that is genuinely session-only passed the
                    // same check without any such mechanism existing.
                    requireProtections(getPolicy());
                    publishKey(generation, meta, key);
                    // Ownership transferred: the vault holds this array now, so the finally must
                    // not wipe it. Load bearing, not a dead store -- see unlockRemembered.
                    key = null;
                    completeUnlock(out, generation, Boolean.valueOf(envelope.getKdf().needsUpgrade()));
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(key);
                    Bytes.zero(password);
                }
            }
        });
        return out;
    }

    /// Unlocks from this device's remembered key, without a password.
    ///
    /// Fails with [VaultError#KEY_MISSING] when the device was never remembered or has been
    /// forgotten -- which is the ordinary case an application handles by asking for the password.
    /// Under [UnlockPolicy#REQUIRE_USER_VERIFICATION] the platform prompts here and a dismissed
    /// prompt is [VaultError#CANCELLED], which is not a failure to report as an error to the
    /// user.
    public AsyncResource<Boolean> unlockRemembered() {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread. The unwrap can prompt and can take as long as the user does, and
        // a vault locked in the meantime must not be reopened by a result already in flight.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                byte[] key = null;
                try {
                    DeviceRecord record = deviceRecord();
                    if (record == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "this device is not remembered for this vault");
                    }
                    VaultMetadata meta = requireMetadata();
                    if (record.keyVersion != meta.dataKeyVersion) {
                        // The wrap holds a different data key than the vault record says is
                        // current, which happens when a rotation committed its metadata and then
                        // could not rewrite this wrap -- a cancelled prompt, storage that went
                        // away, or a crash in between. Using it would hand back the OLD key while
                        // the vault labels everything it seals with the NEW version, and those
                        // records then fail to open after a password unlock. Data loss, silently.
                        //
                        // Refused rather than repaired: repairing needs the new key, and the only
                        // thing here that has it is a password unlock. The stale record is removed
                        // so the next remembered unlock is an honest "not remembered".
                        //
                        // Only while it is still the record that was INSPECTED. Another tab can
                        // refresh the wrap between the read above and this line, and deleting
                        // then threw away a valid record that tab had already reported storing
                        // -- leaving this one reporting KEY_MISSING for a device that really is
                        // remembered. Finding something else means the problem this is reacting
                        // to has already been fixed by somebody.
                        Object stillStale = readUncached(deviceRecordKey());
                        if (stillStale instanceof String
                                && record.serialize().equals(stillStale)) {
                            Storage.getInstance().deleteStorageFile(deviceRecordKey());
                        }
                        throw new VaultException(VaultError.KEY_MISSING,
                                "this device's remembered key is from before a key rotation and "
                                + "has been discarded; unlock with the password to remember it "
                                + "again");
                    }
                    // Judged against the policy configured NOW, not the one this device was
                    // enrolled under. An application that adds requireDeviceBoundPasskey() to a
                    // vault already remembered with a syncable passkey was still reopened by
                    // that credential: enrolment validates it and this path went straight to
                    // unwrap, so the requirement governed new enrolments and nothing else.
                    //
                    // ensureKey is the validation, not a creation: with a record already stored
                    // the port checks it against the requirement and refuses rather than making
                    // anything.
                    DeviceProtection unlocking = deviceProtection(record.policy);
                    if (record.policy == UnlockPolicy.REQUIRE_USER_VERIFICATION
                            && !unlocking.requiresUserVerification()) {
                        throw new VaultException(VaultError.POLICY_NOT_MET,
                                "this device cannot provide the verification required by its "
                                + "remembered unlock policy", Protection.USER_VERIFICATION, null);
                    }
                    unlocking.setDeviceBoundRequired(options.isDeviceBoundPasskeyRequired());
                    if (options.isDeviceBoundPasskeyRequired()) {
                        await(unlocking.ensureKey(deviceKeyId()),
                                "the remembered credential does not satisfy this vault's "
                                + "device-bound requirement");
                    }
                    key = awaitBytes(unlocking.unwrap(deviceKeyId(), record.wrap,
                                    deviceWrapBinding(meta, record.policy).serialize()),
                            "the remembered device key could not be used");
                    if (generation != lockGeneration()) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while it was being unlocked");
                    }
                    if (key == null || key.length != 32) {
                        throw new VaultException(VaultError.CORRUPT,
                                "the device wrap did not contain a data key");
                    }
                    requireAuthenticRecord(meta, key);
                    // Against the policy this device is actually enrolled under, which is what
                    // a remembered unlock is using.
                    requireProtections(record.policy);
                    publishKey(generation, meta, key);
                    key = null;
                    completeUnlock(out, generation);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(key);
                }
            }
        });
        return out;
    }

    /// Remembers this device, so the vault reopens without a password.
    ///
    /// Requires the vault to be unlocked, because it is the data key that gets wrapped. Refused
    /// with [VaultError#POLICY_NOT_MET] when the policy is
    /// [UnlockPolicy#REQUIRE_USER_VERIFICATION] and this device's key store cannot gate on user
    /// verification -- an unattended wrap under a policy that promises a prompt is the failure
    /// that policy exists to prevent.
    public AsyncResource<Boolean> rememberDevice() {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    requireUnlocked();
                    DeviceRecord existing = deviceRecord();
                    if (existing != null
                            && existing.policy == UnlockPolicy.REQUIRE_USER_VERIFICATION
                            && options.getPolicy() != UnlockPolicy.REQUIRE_USER_VERIFICATION) {
                        // Weakening a policy is setPolicy's job, where it is an explicit request.
                        // Doing it here, as a side effect of "remember me", is how a user who
                        // asked for a prompt stops getting one without being told.
                        throw new VaultException(VaultError.POLICY_NOT_MET,
                                "this device is enrolled with user verification required; use "
                                + "setPolicy to change that deliberately",
                                Protection.USER_VERIFICATION, null);
                    }
                    if (options.getPolicy() == UnlockPolicy.SESSION_ONLY) {
                        // SESSION_ONLY is a promise that nothing capable of reopening this
                        // vault is written down. Honouring "remember me" here would persist a
                        // device wrap whose own record still says SESSION_ONLY: the vault
                        // reopens without a password while getPolicy() reports that it cannot.
                        // Refused rather than silently upgraded, because changing what a vault
                        // promises is setPolicy's job and is the caller's decision to make.
                        throw new VaultException(VaultError.POLICY_NOT_MET,
                                "this vault is configured session-only, which stores nothing "
                                + "that can reopen it; use setPolicy to choose a remembering "
                                + "policy first",
                                Protection.PERSISTENT, null);
                    }
                    // Snapshotted before the rewrite, because this call is not always a
                    // CREATE. A device already remembered under this same policy is rewrapped
                    // here anyway, and the rollback below then deleted a remembered unlock this
                    // call did not create -- so a redundant "remember me" that raced a lock()
                    // reported LOCKED and also silently forgot the working enrolment the user
                    // already had. Read past Storage's cache, because another tab can have
                    // replaced it since this one last looked.
                    Object restore = readUncached(deviceRecordKey());
                    rememberNow(options.getPolicy());
                    requireDeviceRecordStillWanted(generation,
                            restore instanceof String ? (String) restore : null);
                    out.complete(Boolean.TRUE);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                }
            }
        });
        return out;
    }

    /// Writes the device wrap under one policy.
    ///
    /// The policy is a parameter rather than read from [VaultOptions] because the two disagree in
    /// the case that matters. A vault enrolled under
    /// [UnlockPolicy#REQUIRE_USER_VERIFICATION] and then reopened by a caller who did not repeat
    /// that in its options has the stronger policy stored on the device and the weaker one
    /// configured, and a rewrap that read the configured one would replace the gated wrap with an
    /// unattended one -- silently turning off the prompt the user asked for.
    private void rememberNow(UnlockPolicy policy) {
        final int generation = lockGeneration();
        final int keyAt = keyGeneration();
        // Snapshotted, not re-read, for the reason rotateDataKey gives: the store below prompts,
        // so this method runs for as long as the user takes, and lock() nulls both of these. Read
        // afterwards they are null and the worker dies on a NullPointerException, which the
        // catch-all reports as "this vault operation could not complete" -- a lock described as an
        // unknown fault. With the snapshot the write finishes and the caller is told it was LOCKED,
        // by the generation check the two public entry points make after this returns.
        VaultMetadata meta = sessionMetadata();
        byte[] key = sessionKey();
        if (meta == null || key == null) {
            throw new VaultException(VaultError.LOCKED,
                    "the vault was locked before this device could be remembered");
        }
        DeviceProtection device = deviceProtection(policy);
        // Set before ensureKey, because it changes how the key is created rather than how it is
        // used. A port that cannot honour it refuses there.
        device.setDeviceBoundRequired(options.isDeviceBoundPasskeyRequired());
        if (policy == UnlockPolicy.REQUIRE_USER_VERIFICATION
                && !device.requiresUserVerification()) {
            throw new VaultException(VaultError.POLICY_NOT_MET,
                    "this device cannot gate its key store on user verification",
                    Protection.USER_VERIFICATION, null);
        }
        Boolean ready = await(device.ensureKey(deviceKeyId()),
                "the device key could not be created");
        if (ready == null || !ready.booleanValue()) {
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "the device key could not be created");
        }
        requireRememberedKeyCurrent(meta, keyAt);
        byte[] aad = deviceWrapBinding(meta, policy).serialize();
        byte[] wrapped = await(device.wrap(deviceKeyId(), key, aad),
                "the data key could not be wrapped for this device");
        // Proven before it is trusted, same reasoning as enrolment: a wrap that cannot be
        // unwrapped is a "remember me" that silently does not.
        byte[] proof = await(device.unwrap(deviceKeyId(), wrapped, aad),
                "the device wrap could not be read back");
        boolean good = Bytes.constantTimeEquals(proof, key);
        Bytes.zero(proof);
        if (!good) {
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "the device wrap did not read back as it was written");
        }
        requireRememberedKeyCurrent(meta, keyAt);
        DeviceRecord attempted = new DeviceRecord(policy, wrapped, meta.dataKeyVersion);
        writeDeviceRecord(attempted);
        byte[] persistedProof = null;
        try {
            requireRememberedKeyCurrent(meta, keyAt);
            // A concurrent forget can delete the key after the first proof but before this
            // record is published. Verify after publication as well; keyState alone cannot
            // distinguish the original key from a replacement created under the same id.
            // A lock is handled by the caller's existing rollback/degradation path.
            if (generation != lockGeneration()) {
                return;
            }
            persistedProof = awaitBytes(device.unwrap(deviceKeyId(), wrapped, aad),
                    "the published device wrap no longer has a usable device key");
            if (generation != lockGeneration()) {
                return;
            }
            requireAuthenticRecord(meta, persistedProof);
            requireRememberedKeyCurrent(meta, keyAt);
            if (!attempted.serialize().equals(readUncached(deviceRecordKey()))) {
                throw new VaultException(VaultError.CONFLICT,
                        "this device's remembered record changed while it was being published");
            }
        } catch (VaultException changed) {
            // A replacement inside the storage write must not leave this stale wrap active.
            // Preserve a later writer's record; only this attempt can be withdrawn here.
            if (attempted.serialize().equals(readUncached(deviceRecordKey()))) {
                Storage.getInstance().deleteStorageFile(deviceRecordKey());
                if (!definitelyGone(deviceRecordKey())) {
                    throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                            "a stale device wrap could not be withdrawn", changed);
                }
            }
            throw changed;
        } finally {
            Bytes.zero(persistedProof);
        }
    }

    /// Authenticates the selected mechanism as well as the vault and data-key version. Providers
    /// may share key material between their gated and unattended variants; changing the policy
    /// in ordinary Storage must never let the unattended variant open a gated wrap.
    private AssociatedData deviceWrapBinding(VaultMetadata meta, UnlockPolicy policy) {
        return wrapBinding(meta, PURPOSE_DEVICE + "." + policy.name());
    }

    /// A prompt or wrapping operation can outlive a rotation in this session or another tab.
    private void requireRememberedKeyCurrent(VaultMetadata expected, int keyAt) {
        requireSameKey(keyAt);
        VaultMetadata stored = loadMetadataFresh();
        if (stored == null || !stored.serialize().equals(expected.serialize())) {
            throw new VaultException(VaultError.CONFLICT,
                    "this vault changed while the device was being remembered; try again");
        }
        requireSameKey(keyAt);
    }

    /// Forgets this device: the local wrap is deleted and so is the device key behind it.
    ///
    /// The vault still opens with the password, here and everywhere else. What this cannot do is
    /// reach a copy of the data key that was taken while the device was remembered -- see the
    /// table on this class.
    public AsyncResource<Boolean> forgetDevice() {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    Storage storage = Storage.getInstance();
                    for (int attempt = 0; attempt < 3; attempt++) {
                        storage.deleteStorageFile(deviceRecordKey());
                        // Prove removal before destroying its key. A refused record deletion
                        // must leave the existing remembered unlock usable for a later retry.
                        if (!definitelyGone(deviceRecordKey())) {
                            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                                    "this device's remembered-unlock record could not be removed, so "
                                    + "the key it names has been left in place rather than orphaned");
                        }
                        forgetEveryMechanism();
                        if (definitelyGone(deviceRecordKey())) {
                            out.complete(Boolean.TRUE);
                            return;
                        }
                        // A remembering session published while key deletion yielded. Remove
                        // that record and its mechanism together on the next pass. A publisher
                        // arriving after this final absence check must verify its key after its
                        // own write, so it either establishes a usable new enrolment or refuses.
                    }
                    throw new VaultException(VaultError.CONFLICT,
                            "another session kept remembering this device while it was being "
                            + "forgotten; retry forgetting after that operation finishes");
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                }
            }
        });
        return out;
    }

    /// Locks the vault: the data key is overwritten and dropped, and every handle this vault
    /// issued stops working.
    ///
    /// An operation already in flight when this runs does not deliver its result -- it fails with
    /// [VaultError#LOCKED] instead. What locking cannot do is reach a plaintext or a key already
    /// handed to a caller, including to hostile code that got one while the vault was open.
    public synchronized void lock() {
        Bytes.zero(dataKey);
        dataKey = null;
        metadata = null;
        passwordNeedsRewrap = false;
        lockGeneration++;
    }

    /// Deletes everything this vault stores on this device: the record, the secrets, the device
    /// wrap and the device key.
    ///
    /// This is "delete my data from this device", not "delete my data". It does not reach a sync
    /// server, another device, a backup or an operating system snapshot, and a browser that has
    /// already written the storage to disk may leave the blocks recoverable. Say "removed from
    /// this browser", not "erased".
    public AsyncResource<Boolean> destroyLocalData() {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    Storage storage = Storage.getInstance();
                    // Enumerated BEFORE anything is removed, and a store that cannot enumerate
                    // stops the call here rather than skipping to the end of it. The branch used
                    // to treat an unavailable listing as an empty one -- every secret left on
                    // disk, the record and the device key deleted anyway, and TRUE reported for
                    // "delete everything this vault stores on this device". It is reachable:
                    // JavaSE answers getStorageDir().list(), and File.list() is null for a
                    // directory that does not exist or cannot be read.
                    String[] entries = storage.listEntries();
                    if (entries == null || (entries.length == 0
                            && !definitelyGone(metadataKey()))) {
                        // Null is JavaSE, where File.list() says so. The second half is the
                        // browser, where it does not: HTML5Implementation catches the IndexedDB
                        // IOException and answers an EMPTY array, which is indistinguishable from
                        // a store that holds nothing -- so a transient failure here skipped every
                        // secret and went on to delete the record and the device key, orphaning
                        // the ciphertexts for good. This vault's own record is still on disk at
                        // this point, so a listing that reports nothing while that record exists
                        // is contradicting something we can see, and is not to be believed.
                        throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                                "this device's storage cannot be enumerated, so the secrets in "
                                + "this vault cannot be found to delete; nothing was removed");
                    }
                    String prefix = secretKey("");
                    java.util.Vector deleted = new java.util.Vector();
                    try {
                        for (String entry : entries) {
                            if (entry != null && entry.startsWith(prefix)) {
                                // Remembered, because the confirmation cannot rely on a second
                                // enumeration: on the browser a failed listing answers an EMPTY
                                // array, so a refused secret deletion followed by a failed
                                // re-enumeration read as "nothing left" and destroyLocalData
                                // reported success over ciphertext that is still there.
                                deleted.addElement(entry);
                                storage.deleteStorageFile(entry);
                            }
                        }
                        storage.deleteStorageFile(deviceRecordKey());
                        storage.deleteStorageFile(metadataKey());
                        forgetEveryMechanism();
                        requireEverythingGone(storage, prefix, deleted);
                    } finally {
                        // In a finally, because once ANY of that has happened the live key and
                        // cached record describe a vault that is no longer on disk. A device key
                        // that refuses to be deleted makes forgetEveryMechanism throw, and the
                        // lock that used to sit after it was skipped -- leaving the vault open,
                        // so a caller that caught the error and carried on sealed records under
                        // a key nothing would hold after a restart. Failing to delete is
                        // recoverable; writing more data into a vault that no longer exists is
                        // not.
                        lock();
                    }
                    out.complete(Boolean.TRUE);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                }
            }
        });
        return out;
    }

    // ------------------------------------------------------------ secrets

    /// Stores a secret string under a name.
    ///
    /// The value is taken as characters rather than a `String` so the caller can clear it; this
    /// method clears the array it is given once the secret is sealed.
    public AsyncResource<Boolean> putSecret(final String secretName, final char[] value) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        final int keyAt = keyGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                byte[] plain = null;
                try {
                    requireUnlocked();
                    // Taken once, into locals. Reading metadata again further down is what
                    // turned a concurrent lock() into a NullPointerException -- lock() nulls
                    // the field, and this expression dereferenced it after requireUnlocked had
                    // already passed.
                    byte[] key = sessionKey();
                    VaultMetadata meta = sessionMetadata();
                    requireSameGeneration(generation);
                    if (key == null || meta == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this secret was being stored");
                    }
                    plain = Bytes.utf8(value);
                    byte[] sealed = SecureEnvelope.seal(key, meta.dataKeyId,
                            meta.dataKeyVersion,
                            binding(meta, secretName, PURPOSE_SECRET), plain);
                    // Asked again before the write. The snapshot above is a REFERENCE, and
                    // lock() zeroes the array in place before it drops it, so a lock landing
                    // during the seal leaves ciphertext made with a key of zeroes. Refusing here
                    // means that is discarded rather than stored as if it were the secret.
                    requireSameGeneration(generation);
                    // And that the key it sealed under is still this vault's. lockGeneration does
                    // not move for a replacement, so without this the seal above could have run
                    // against an array adoptKey had already zeroed.
                    requireSameKey(keyAt);
                    String entry = secretKey(secretName);
                    // Kept so the write can be undone. The check above is before a storage write
                    // and a lock can land inside one, and this call REPLACES whatever was stored
                    // under the name -- so refusing without putting the old value back would
                    // discard a secret the caller still believes is there, on an operation that
                    // reported failure.
                    // Uncached, because this is what a rollback would put BACK. Storage's
                    // process-local cache can answer with this tab's older copy, so restoring it
                    // would overwrite whatever another tab stored most recently -- on an
                    // operation that reports LOCKED and is supposed to have changed nothing.
                    Object previous = readUncached(entry);
                    if (!Storage.getInstance().writeObject(entry, Bytes.toHex(sealed))) {
                        throw new VaultException(VaultError.QUOTA_EXCEEDED,
                                "the secret could not be written to storage");
                    }
                    VaultMetadata persisted = loadMetadataFresh();
                    if (persisted == null || !persisted.serialize().equals(meta.serialize())) {
                        // A different tab can destroy or replace the vault inside writeObject.
                        // Its lock counter is independent. Withdraw only our ciphertext, without
                        // resurrecting the previous secret into a vault that no longer owns it.
                        lock();
                        requireSecretRestored(entry, null, Bytes.toHex(sealed));
                        throw new VaultException(persisted == null ? VaultError.LOCKED : VaultError.CONFLICT,
                                "another session removed or changed the vault while the secret was stored");
                    }
                    try {
                        completeUnlocked(out, generation, keyAt, Boolean.TRUE);
                    } catch (VaultException failed) {
                        requireSecretRestored(entry, previous, Bytes.toHex(sealed));
                        throw failed;
                    }
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(plain);
                    Bytes.zero(value);
                }
            }
        });
        return out;
    }

    /// Reads a secret back. The caller owns the returned characters and should clear them.
    ///
    /// Errors with [VaultError#KEY_MISSING] when there is no such secret, and with
    /// [VaultError#AUTHENTICATION_FAILED] when there is one and it does not authenticate -- which
    /// means it was altered or was written under a different vault.
    public AsyncResource<char[]> getSecret(final String secretName) {
        final AsyncResource<char[]> out = new AsyncResource<char[]>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        final int keyAt = keyGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                byte[] plain = null;
                try {
                    requireUnlocked();
                    // Uncached. readObject answers this TAB's copy and nothing another tab
                    // writes can invalidate it, so once this tab had read a secret it kept
                    // decrypting that same ciphertext however many times another tab replaced the
                    // value -- indefinitely, with the newer record sitting in storage. Every
                    // metadata and device-record read here already goes past the cache for
                    // exactly this reason; the secrets themselves did not.
                    String stored = asString(readUncached(secretKey(secretName)));
                    if (stored == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "no secret is stored under that name");
                    }
                    byte[] sealed = Bytes.fromHex(stored);
                    if (sealed == null) {
                        throw new VaultException(VaultError.CORRUPT,
                                "the stored secret is not in a format this build wrote");
                    }
                    // Snapshotted, not read off the field. lock() nulls metadata, and reading
                    // it here after requireUnlocked had already passed produced a
                    // NullPointerException that the terminal handler reported as UNKNOWN -- an
                    // ordinary lock described as an unknown fault, on a path whose documented
                    // answer is LOCKED. putSecret takes its snapshot for the same reason.
                    VaultMetadata meta = sessionMetadata();
                    if (meta == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this secret was being read");
                    }
                    plain = openAnyVersion(sealed, binding(meta, secretName, PURPOSE_SECRET));
                    completeUnlocked(out, generation, keyAt, chars(plain));
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(plain);
                }
            }
        });
        return out;
    }

    /// Removes a secret.
    public AsyncResource<Boolean> removeSecret(final String secretName) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    // The only secret operation that did none of this. Deleting needs no key, so
                    // it ran while locked -- including after the auto-lock timeout, where the
                    // session is over and a stale screen tapping "remove" still destroyed the
                    // ciphertext irreversibly. destroyLocalData is the operation that deliberately
                    // works without a key; this one is an ordinary edit and answers LOCKED like
                    // its siblings.
                    requireUnlocked();
                    String entry = secretKey(secretName);
                    // Kept so the delete can be undone, the way putSecret keeps what it
                    // overwrites. The generation is checked after the delete -- it has to be,
                    // because a lock can land inside the delete itself -- and reporting LOCKED
                    // over a secret that is irreversibly gone tells the caller nothing changed
                    // when everything did.
                    // Uncached, for the reason putSecret gives: a rollback restores this.
                    Object previous = readUncached(entry);
                    Storage.getInstance().deleteStorageFile(entry);
                    if (generation != lockGeneration()) {
                        // null: this path DELETED the entry, so "still ours" means still
                        // absent. What that cannot distinguish, and why it restores anyway, is
                        // recorded on the helper.
                        requireSecretRestored(entry, previous, null);
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this secret was being removed");
                    }
                    // definitelyGone, not !exists: a port that cannot tell answers false, which
                    // this line read as "removed" and reported as success over a secret that is
                    // still on the device.
                    out.complete(Boolean.valueOf(definitelyGone(entry)));
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                }
            }
        });
        return out;
    }

    // ------------------------------------------------------------ records

    /// Seals application data into a portable envelope, bound to a record id.
    ///
    /// Nothing is stored: the bytes come back for the caller to put wherever the data belongs --
    /// a file, a database column, a sync server. They are readable only by a vault holding this
    /// data key, which means the same user's other devices after they enroll from
    /// [#exportSyncState()].
    public AsyncResource<byte[]> seal(final String recordId, final byte[] plaintext) {
        final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        final int keyAt = keyGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    requireUnlocked();
                    // Snapshotted for the same reason as putSecret: lock() nulls metadata, and
                    // dereferencing it here after requireUnlocked has passed is a
                    // NullPointerException rather than a refusal.
                    byte[] key = sessionKey();
                    VaultMetadata meta = sessionMetadata();
                    requireSameGeneration(generation);
                    if (key == null || meta == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this record was being sealed");
                    }
                    byte[] sealed = SecureEnvelope.seal(key, meta.dataKeyId,
                            meta.dataKeyVersion,
                            binding(meta, recordId, PURPOSE_RECORD), plaintext);
                    // lock() zeroes the key array in place, so a lock during the seal above
                    // produces ciphertext under zeroes. Handing that back would look like a
                    // sealed record and open as nothing.
                    requireSameGeneration(generation);
                    // And under a key this vault still has: a replacement zeroes the array this
                    // snapshotted, and lockGeneration does not move for one.
                    completeUnlocked(out, generation, keyAt, sealed);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                }
            }
        });
        return out;
    }

    /// Opens what [#seal] produced, including envelopes written before a [#rotateDataKey()].
    ///
    /// A record sealed under a retired key version is opened by walking the key chain in the
    /// vault record. A record whose key version is newer than this vault knows about is
    /// [VaultError#UNSUPPORTED_FORMAT]: another device rotated and this one has not synced yet,
    /// which is a state to report rather than to guess through.
    public AsyncResource<byte[]> open(final String recordId, final byte[] sealed) {
        final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        final int keyAt = keyGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    requireUnlocked();
                    // Snapshotted for the reason getSecret gives: lock() nulls this field, and
                    // reading it after requireUnlocked has passed reports an ordinary lock as an
                    // unknown fault.
                    VaultMetadata meta = sessionMetadata();
                    if (meta == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this record was being opened");
                    }
                    byte[] plain = openAnyVersion(sealed,
                            binding(meta, recordId, PURPOSE_RECORD));
                    completeUnlocked(out, generation, keyAt, plain);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                }
            }
        });
        return out;
    }

    /// An opaque key for a named purpose, derived from the data key.
    ///
    /// Two purposes produce two independent keys -- the derivation is HMAC-SHA-256 over the data
    /// key, which is uniformly random, so this needs no salt and no stretching. Use it when a
    /// component wants its own key without being handed the vault's: a cache encryptor, a
    /// per-feature sealer, a MAC for an integrity check.
    ///
    /// The handle stops working when the vault locks.
    public AsyncResource<KeyHandle> operationalKey(final String purpose) {
        final AsyncResource<KeyHandle> out = new AsyncResource<KeyHandle>();
        // On the calling thread; see unlockWithPassword for why not in the worker. BOTH
        // generations, because this derives from a snapshot of the data key and must not stamp
        // the handle with a generation that moved after the snapshot was taken.
        final int generation = lockGeneration();
        final int keyAt = keyGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    requireUnlocked();
                    byte[] source = sessionKey();
                    VaultMetadata meta = sessionMetadata();
                    requireSameGeneration(generation);
                    if (source == null || meta == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this key was being derived");
                    }
                    byte[] derived = deriveSubkey(source, purpose);
                    completeUnlocked(out, generation, keyAt,
                            new VaultKeyHandle(Vault.this, generation, keyAt, derived,
                                    purpose, meta.dataKeyVersion, extractedKeyProtection()));
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                }
            }
        });
        return out;
    }

    /// Raw key bytes for an encrypted database, which is the one deliberate exposure in this
    /// package.
    ///
    /// #### Why this exists and why it is not hidden
    ///
    /// SQLCipher keys from bytes. That is true of the native builds and of the WASM build the
    /// browser uses, and no opaque handle changes it: at some point 32 bytes have to reach the
    /// engine. The choices were to pretend otherwise by quietly exporting a [KeyHandle] behind
    /// the caller's back, or to have one method, named for what it does, that says so in its
    /// documentation and can be refused by policy. This is the second.
    ///
    /// #### What it actually costs
    ///
    /// While the database is open the key is in the process: in this array until the caller
    /// clears it, inside the engine for as long as the connection lives, and in whatever the
    /// runtime copied it into. In a browser that means a heap any script in the origin shares. It
    /// is bounded by the vault being unlocked -- there is no key at all before that -- and it is
    /// not bounded by anything else.
    ///
    /// The key is derived from the vault's data key, so it changes when [#rotateDataKey] runs and
    /// the database has to be rekeyed in the same operation. It is not the data key itself, so a
    /// leak of it does not open the vault's records.
    ///
    /// #### Parameters
    ///
    /// - `alias`: the database's alias, normally its name. Two aliases get two unrelated keys
    ///
    /// #### Returns
    ///
    /// a resource completing with 32 bytes the caller owns and should clear, or erroring with
    /// [VaultError#POLICY_NOT_MET] when the vault was configured with
    /// [VaultOptions#requireOpaqueKeysOnly()], or [VaultError#LOCKED] when it is locked
    public AsyncResource<byte[]> databaseKey(final String alias) {
        return databaseKeyForVersion(alias, 0);
    }

    /// Derives a database key from a specific current or retired data-key version.
    ///
    /// Use the version stored alongside a database to open it after a local or imported rotation,
    /// then explicitly rekey it with the current version. The retired chain retains these keys
    /// across restarts. The same lock and opaque-key restrictions as [#databaseKey(String)] apply.
    ///
    /// - `alias`: the alias originally used for this database
    /// - `version`: a positive data-key version; future versions fail with
    ///   [VaultError#UNSUPPORTED_FORMAT], unavailable retired keys with [VaultError#KEY_MISSING]
    public AsyncResource<byte[]> databaseKey(final String alias, final int version) {
        if (version < 1) {
            throw new IllegalArgumentException("A database key version must be positive");
        }
        return databaseKeyForVersion(alias, version);
    }

    /// The current data-key version. Store this non-secret value alongside a database when
    /// creating or rekeying it, so an imported rotation cannot hide which key opens the file.
    /// Requires an unlocked vault.
    public int getDataKeyVersion() {
        int generation = lockGeneration();
        requireUnlocked();
        synchronized (this) {
            requireSameGeneration(generation);
            return metadata.dataKeyVersion;
        }
    }

    private AsyncResource<byte[]> databaseKeyForVersion(final String alias, final int version) {
        final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        final int keyAt = keyGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                byte[] source = null;
                try {
                    if (options.isOpaqueKeysOnly()) {
                        throw new VaultException(VaultError.POLICY_NOT_MET,
                                "this vault was configured to produce no raw key material, and an "
                                + "encrypted database cannot be keyed without it",
                                Protection.NON_EXTRACTABLE_KEY, null);
                    }
                    requireUnlocked();
                    VaultMetadata meta = sessionMetadata();
                    requireSameGeneration(generation);
                    if (meta == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this key was being derived");
                    }
                    source = dataKeyAtVersion(version == 0 ? meta.dataKeyVersion : version);
                    Hmac mac = Hmac.create(Hash.SHA256, source);
                    mac.update(Bytes.utf8("cn1.vault.dbkey.v1"));
                    mac.update(Bytes.utf8(alias == null ? "" : alias));
                    byte[] key = mac.doFinal();
                    completeUnlocked(out, generation, keyAt, key);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(source);
                }
            }
        });
        return out;
    }

    /// What protects the key [#databaseKey(String)] produces, reported honestly.
    ///
    /// It is the vault's own protection with one flag forced off: the bytes exist, so
    /// [Protection#NON_EXTRACTABLE_KEY] is `NO` here even where the vault reports `YES`. A
    /// database keyed this way is protected at rest by whatever protects the vault, and not at
    /// all from code running while it is open.
    public ProtectionReport databaseKeyProtection() {
        return extractedKeyProtection();
    }

    /// This vault's protections, restated for a key whose bytes the caller is now holding.
    ///
    /// Everything else about the vault still applies -- where the record lives, what gates an
    /// unlock -- but NON_EXTRACTABLE_KEY cannot survive handing the bytes out, whatever the
    /// underlying device key can do. Shared by the two places that do it, so the answer cannot
    /// drift between them.
    private ProtectionReport extractedKeyProtection() {
        ProtectionReport vaultReport = protection();
        ProtectionReport.Builder b = ProtectionReport.builder();
        for (Protection protection : Protection.values()) {
            b.set(protection, vaultReport.answer(protection));
        }
        b.set(Protection.NON_EXTRACTABLE_KEY, false);
        return b.build();
    }

    // -------------------------------------------- password, recovery, rotation

    /// Changes the password by rewrapping the data key. No record is re-encrypted, so this is
    /// constant time in the amount of data the vault holds.
    ///
    /// Requires the old password even when the vault is already unlocked: the alternative is that
    /// anyone who finds an unlocked application can lock the real owner out of every other device.
    public AsyncResource<Boolean> changePassword(final char[] oldPassword, final char[] newPassword) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        background(new Runnable() {
            @Override
            public void run() {
                byte[] key = null;
                try {
                    VaultMetadata meta = requireMetadata();
                    if (meta.passwordWrap == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "this vault has no password wrap to change");
                    }
                    key = SecureEnvelope.parse(meta.passwordWrap).openWithPassword(oldPassword,
                            wrapBinding(meta, PURPOSE_PASSWORD));
                    // Before anything is re-signed. This is an entry point of its own -- it works
                    // on a locked instance and never goes through an unlock path -- so a record
                    // whose counter or retired chain had been edited was opened, and then
                    // stampMac below signed the edit. That does not merely miss the check, it
                    // launders the corruption into something every later check accepts.
                    requireAuthenticRecord(meta, key);
                    byte[] rewrapped = SecureEnvelope.sealWithPassword(newPassword,
                            options.getKdf(), meta.dataKeyId, meta.dataKeyVersion,
                            wrapBinding(meta, PURPOSE_PASSWORD), key);
                    // Same discipline as rotation: a copy is written and then adopted, so a failed
                    // write leaves the vault on the record that is actually stored rather than on
                    // one holding a password wrap nobody can find.
                    VaultMetadata next = meta.copy();
                    next.passwordWrap = rewrapped;
                    next.counter = meta.counter + 1;
                    stampMac(next, meta, key);
                    commitMetadata(meta, next);
                    // No lock-generation check here, and that is deliberate rather than an
                    // omission -- a review has asked for one, so the reasoning belongs beside
                    // the code rather than in rotateDataKey's comment where it used to live.
                    //
                    // Three things make a lock landing inside this harmless. It never reads the
                    // shared data key: the key it re-wraps is derived from the password wrap
                    // into an array of its own, so lock() zeroing dataKey cannot corrupt what
                    // this produces. It publishes no key, so the vault is closed afterwards
                    // either way, which is the state lock() was asking for. And it is documented
                    // to work on an ALREADY-locked vault -- so a lock arriving in the middle
                    // cannot make the operation invalid, when the same state at the start is
                    // explicitly supported.
                    //
                    // Undoing it would also be the worse answer. Rolling the record back
                    // restores the OLD password after the user asked to change it, on a call
                    // that would then report LOCKED; leaving it stands means the change the user
                    // asked for is durable and their next unlock uses the new password. What a
                    // lock must prevent is an operation DELIVERING a key or reopening a closed
                    // vault, and this does neither.
                    //
                    // Cached ONLY by an instance that actually holds the key. changePassword is
                    // the one path documented to work on a LOCKED vault, and caching the record
                    // there left this tab warm with no key: loadMetadata() then answers the
                    // in-memory copy instead of reading storage, so a later unlockWithPassword
                    // opened the wrap THIS tab remembered. Another tab changing the password or
                    // rotating in between was invisible, and the superseded password kept
                    // working while the tab operated on stale key metadata.
                    //
                    // Every other assignment to this field adopts a key in the same breath --
                    // enrolment, publishKey, the rotation, the recovery code -- so this is the
                    // only one that can be holding nothing.
                    synchronized (Vault.this) {
                        // Never repopulate a locked session's metadata, even transiently.
                        if (dataKey != null) {
                            metadata = next;
                        }
                    }
                    setPasswordNeedsRewrap(false);
                    out.complete(Boolean.TRUE);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(key);
                    Bytes.zero(oldPassword);
                    Bytes.zero(newPassword);
                }
            }
        });
        return out;
    }

    /// Creates a recovery code and wraps the data key under it.
    ///
    /// The returned characters are the only copy: they are not stored anywhere, and a vault whose
    /// password is forgotten and whose recovery code was not written down is not recoverable by
    /// anybody, which is the property that makes the rest of this worth anything. Show them once,
    /// tell the user to keep them, and clear the array.
    ///
    /// A code is twenty bytes of randomness in Base32 -- 160 bits, which needs no stretching, so
    /// [#unlockWithRecoveryCode] is fast where [#unlockWithPassword] is deliberately slow.
    public AsyncResource<char[]> createRecoveryCode() {
        final AsyncResource<char[]> out = new AsyncResource<char[]>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        // The key generation as well. adoptKey zeroes the dataKey array IN PLACE, so an unlock of
        // an already-unlocked vault or a key import running beside this leaves the seal below
        // wrapping zeroes while stampMac -- which read the field a second time, after the
        // replacement -- stamps authentic metadata over it. The result is a recovery code the
        // vault accepts as well-formed and that can never open it, handed to the user as their
        // one way back in. seal, putSecret, databaseKey, operationalKey and rotateDataKey all
        // carry this guard; this path read the field twice and carried neither the snapshot nor
        // the check.
        final int keyAt = keyGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                char[] owned = null;
                try {
                    requireUnlocked();
                    byte[] raw = SecureRandom.bytes(20);
                    // Into a clearable array, not through Base32.encode's String. A recovery code
                    // is the one credential that reopens this vault without the password, and the
                    // String that method returns cannot be zeroed by anybody -- so the array
                    // handed back was never the only copy the javadoc says it is, and a heap dump
                    // taken long after lock() still held a working credential.
                    char[] code = Bytes.base32Chars(raw);
                    Bytes.zero(raw);
                    // Owned from here, so every way out that is not delivery wipes it. Sealing,
                    // stamping and commitMetadata can all throw after the code exists -- a
                    // storage conflict, a quota refusal -- and the catch reported the error while
                    // a credential that reopens this vault, and that the caller never received,
                    // stayed in the heap. The javadoc says the returned characters are the only
                    // copy; until this it was not even the only copy on the failure path.
                    owned = code;
                    // Snapshotted once, for the reason getSecret gives: lock() nulls the field,
                    // and four separate reads of it between here and the commit each gave a
                    // concurrent lock its own way to surface as an unknown fault. Found by the
                    // rule in VaultSourceInvariantsTest rather than by review.
                    VaultMetadata current = sessionMetadata();
                    if (current == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while a recovery code was being created");
                    }
                    // Snapshotted once and used for BOTH the wrap and the MAC, so the two
                    // cannot disagree about which key this record belongs to.
                    byte[] sealing = sessionKey();
                    requireSameKey(keyAt);
                    byte[] derived = recoveryKey(code);
                    VaultMetadata next = current.copy();
                    next.recoveryWrap = SecureEnvelope.seal(derived, next.dataKeyId,
                            next.dataKeyVersion,
                            wrapBinding(next, PURPOSE_RECOVERY), sealing);
                    Bytes.zero(derived);
                    next.counter = current.counter + 1;
                    // Checked before the write: a recovery code refused after persisting its
                    // wrap would be a code the vault accepts and the caller never received.
                    stampMac(next, current, sealing);
                    // And after the seal, which is where a replacement lands: the KDF above runs
                    // for as long as the profile asks.
                    requireSameKey(keyAt);
                    requireSameGeneration(generation);
                    VaultMetadata previous = current;
                    commitMetadata(previous, next);
                    // The completion boundary now owns cleanup on validation failure. Once
                    // published, callbacks may retain the code even if application code throws.
                    owned = null;
                    final VaultMetadata published = next;
                    try {
                        completeUnlocked(out, generation, keyAt, code, new Runnable() {
                            @Override
                            public void run() {
                                metadata = published;
                            }
                        });
                    } catch (VaultException failed) {
                        // Storage can yield; restore only a wrap whose code was not delivered.
                        if (!out.isDone()) {
                            commitMetadata(next, previous);
                        }
                        throw failed;
                    }
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(owned);
                }
            }
        });
        return out;
    }

    /// Unlocks with a recovery code from [#createRecoveryCode()].
    public AsyncResource<Boolean> unlockWithRecoveryCode(final char[] code) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                byte[] derived = null;
                // Owned and released like unlockWithPassword's, and for the same reason: the
                // checks after the wrap opens can all throw, and the finally used to release
                // everything except the key itself. The review named the password path; this one
                // is the same six lines.
                byte[] key = null;
                try {
                    VaultMetadata meta = requireMetadata();
                    if (meta.recoveryWrap == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "this vault has no recovery code");
                    }
                    derived = recoveryKey(code);
                    key = SecureEnvelope.parse(meta.recoveryWrap).open(derived,
                            wrapBinding(meta, PURPOSE_RECOVERY));
                    if (generation != lockGeneration()) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while it was being unlocked");
                    }
                    requireAuthenticRecord(meta, key);
                    // The stored policy, for the reason unlockWithPassword gives: this is an
                    // unlock, so the question is what the vault HAS, not what this caller
                    // happened to configure.
                    requireProtections(getPolicy());
                    publishKey(generation, meta, key);
                    // Ownership transferred; the finally must not wipe what the vault now holds.
                    key = null;
                    completeUnlock(out, generation);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(key);
                    Bytes.zero(derived);
                    Bytes.zero(code);
                }
            }
        });
        return out;
    }

    /// Generates a new data key and retires the current one.
    ///
    /// The outgoing key is sealed under the incoming one and kept in the vault record, so
    /// everything sealed before this call still opens through [#open]. New records use the new
    /// key. The password and recovery wraps are rewritten to wrap the new key, which is why this
    /// needs the password.
    ///
    /// What rotation does **not** do is make an already-copied record unreadable. Anyone holding
    /// the old key and the old ciphertext keeps both.
    ///
    /// **An existing recovery code stops working.** It wrapped the outgoing key, and rewrapping it
    /// would need the code, which is not stored anywhere -- by design. Call
    /// [#createRecoveryCode()] afterwards and tell the user the old one is void, or they will
    /// find out when they need it.
    public AsyncResource<Boolean> rotateDataKey(final char[] password) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // The key generation as well as the lock generation. The rotation snapshots the OUTGOING
        // key and seals it into the retired chain, and adoptKey zeroes that array in place -- so a
        // redundant unlockWithPassword completing in between left this sealing an envelope full
        // of zeroes as the retired key, reporting success, and making every secret from the old
        // version permanently unreadable. seal, putSecret, databaseKey and operationalKey were
        // given this guard; the rotation, which is the operation that MOVES the key, was not.
        final int keyAt = keyGeneration();
        // Rotation derives from the password twice -- once to prove it, once to wrap the new key
        // -- so at the default profile it holds the vault open for well over a second. A lock that
        // lands in there must not be undone by the publish at the end.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                byte[] fresh = null;
                try {
                    requireUnlocked();
                    VaultMetadata meta = sessionMetadata();
                    // Snapshotted, not re-read. lock() sets dataKey to null, and the derivation
                    // below runs for seconds -- so comparing against the field afterwards compared
                    // against null and reported AUTHENTICATION_FAILED, telling the user their
                    // password was wrong when it was fine and the vault had simply been locked.
                    // Measured: the lock-race test produced exactly that before this snapshot.
                    byte[] currentKey = sessionKey();
                    // Proven before anything changes: a rotation that leaves the password unable
                    // to unwrap the new key is a vault nobody can open on another device.
                    byte[] check = SecureEnvelope.parse(meta.passwordWrap).openWithPassword(
                            password, wrapBinding(meta, PURPOSE_PASSWORD));
                    if (generation != lockGeneration()) {
                        // Asked before the comparison, so a lock is reported as a lock rather than
                        // as whatever the comparison happens to conclude about a key that is no
                        // longer there.
                        Bytes.zero(check);
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while the key was being rotated");
                    }
                    boolean same = Bytes.constantTimeEquals(check, currentKey);
                    Bytes.zero(check);
                    if (!same) {
                        throw new VaultException(VaultError.AUTHENTICATION_FAILED,
                                "the password does not match this vault");
                    }
                    // Asked before the outgoing key is sealed into the chain, because everything
                    // after this point depends on currentKey still being this vault's.
                    requireSameKey(keyAt);
                    fresh = SecureRandom.bytes(32);
                    int newVersion = meta.dataKeyVersion + 1;
                    // Built on a copy, and swapped in only once it is safely written. Advancing
                    // the live record first would leave a failed write holding a version the key
                    // in hand does not match, and an application that caught the error and kept
                    // going would stamp that version onto records encrypted with the old key.
                    VaultMetadata next = meta.copy();
                    // The outgoing key sealed under the incoming one, which is the link that lets
                    // an old record still be opened.
                    next.retired.put(Integer.valueOf(meta.dataKeyVersion),
                            SecureEnvelope.seal(fresh, meta.dataKeyId, newVersion,
                                    binding(meta, DATA_KEY_RECORD,
                                            PURPOSE_RETIRED + "." + meta.dataKeyVersion),
                                    currentKey));
                    next.dataKeyVersion = newVersion;
                    next.passwordWrap = SecureEnvelope.sealWithPassword(password, options.getKdf(),
                            next.dataKeyId, newVersion,
                            wrapBinding(next, PURPOSE_PASSWORD), fresh);
                    next.recoveryWrap = null;
                    next.counter = meta.counter + 1;
                    if (generation != lockGeneration()) {
                        // Checked before the write, so a rotation interrupted by a lock simply did
                        // not happen: nothing is persisted and nothing is published. Refusing
                        // after the write would leave a rotated record on disk that the caller was
                        // told had failed.
                        //
                        // changePassword deliberately has no such check. It publishes metadata and
                        // never a data key, and metadata with no key in hand is exactly the locked
                        // state, so there is nothing there for a lock to undo.
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while the key was being rotated");
                    }
                    // The key check belongs HERE, with the lock check above it, because this is
                    // the last point at which nothing is durable. The seal above takes real time
                    // -- a KDF at the configured profile -- and a replacement landing inside it
                    // means the retired envelope wrapped the zeroed array. Refusing now means the
                    // rotation simply did not happen.
                    requireSameKey(keyAt);
                    stampMac(next, meta, fresh);
                    commitMetadata(meta, next);
                    // Read back and confirm the record on disk is the one just written, BEFORE
                    // the new key is adopted. Two tabs unlocking the same version-N vault both
                    // produce a version-(N+1) key and both write the same next counter, so the
                    // equal-counter check in importSyncState never sees them -- they share local
                    // storage rather than exchanging sync state. Publishing regardless left this
                    // tab sealing under a key no persisted wrap describes, and those records are
                    // unreadable after a restart.
                    //
                    // The same portable limit as first enrollment applies and is worth stating
                    // plainly: Storage has writeObject and no compare-and-set, so this is a
                    // persisted-winner check rather than a lock, and a write landing after the
                    // read below is still possible. It is strictly narrower than publishing
                    // blind, and the failure stays closed -- both tabs label the key version
                    // N+1, so a loser's records do not decrypt under the winner's key, they
                    // fail authentication.
                    VaultMetadata settled = loadMetadataFresh();
                    if (settled == null || !settled.serialize().equals(next.serialize())) {
                        Bytes.zero(fresh);
                        throw new VaultException(VaultError.CONFLICT,
                                "another session changed this vault while the key was being "
                                + "rotated; nothing was adopted here");
                    }
                    if (generation != lockGeneration()) {
                        // Asked once more, immediately before publication. The earlier check is
                        // before the write, and the write plus its read-back is long enough for a
                        // lock to land inside -- publishing after that reopens a vault the caller
                        // was told to close.
                        //
                        // The metadata is already committed and is NOT rolled back: the rotation
                        // did happen, and the record on disk describes the new key. What is
                        // refused is holding it. The vault stays locked and the next unlock uses
                        // the new password wrap, which is the same state a crash here would
                        // leave.
                        Bytes.zero(fresh);
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while the key was being rotated; the "
                                + "rotation is stored and the vault is closed");
                    }
                    // Deliberately NOT another requireSameKey. The rotation is committed by
                    // this point, and a version of this asked the question here: a redundant
                    // unlockWithPassword publishing between the read-back and this line moved
                    // keyGeneration, so the guard threw over a rotation that had already durably
                    // happened. The caller saw a failed rotation and skipped the database rekey
                    // the javadoc tells it to do, the next unlock loaded the new vault key, and
                    // the database could not be opened again -- a data loss produced by the
                    // check rather than by the race.
                    //
                    // Adopting is also the correct answer rather than merely the harmless one.
                    // Whatever a concurrent unlock published was derived from the metadata this
                    // rotation has just replaced, so it is superseded by construction; `fresh` is
                    // the key the persisted record now describes. Refusing would leave the vault
                    // OPEN holding a key its own metadata no longer names, which is worse than
                    // either outcome the check was choosing between.
                    adoptSession(generation, next, fresh);
                    fresh = null;
                    try {
                        DeviceRecord remembered = deviceRecord();
                        if (remembered != null) {
                            // The device wrap holds the old key. Re-wrapping is part of the rotation;
                            // leaving it would let a remembered unlock produce a key that no longer
                            // opens anything new. Under the policy the device is actually enrolled
                            // with, not the one this caller happens to have configured.
                            //
                            // The metadata above is already committed, so a failure here cannot be
                            // rolled back -- the rotation happened. What it can do is not leave a wrap
                            // of the superseded key lying about: that is discarded, and the vault is
                            // still openable by password. The version stamped into the record makes
                            // the same state safe after a crash, where this handler never runs.
                            //
                            // And it is NOT rethrown. The rewrap is the last step of a rotation that
                            // has already been committed, so reporting the whole call as failed
                            // describes a state that does not exist -- and the caller acts on that
                            // report. The sequence this breaks is the ordinary one: rotate the vault
                            // key, then rekey the database under it. An exception here makes the
                            // caller skip the rekey, so the database stays under the superseded key
                            // while every later unlock derives the new one, and it cannot be opened
                            // again after a restart. Losing a remembered unlock is recoverable with a
                            // password; losing the database is not.
                            //
                            // The degradation is still observable: the device record is gone, so
                            // getPolicy() answers SESSION_ONLY, which is the state the device is
                            // actually in. A caller that cares re-establishes it with remember(...).
                            rememberNow(remembered.policy);
                            // A lock landing inside that is worse than a failure, because it
                            // SUCCEEDS. rememberNow snapshots the data key it was handed and
                            // lock() zeroes that same array in place, so a store that prompts --
                            // a passkey, a keystore with user verification -- can return to find
                            // it wrapping zeroes. It then unwraps zeroes for its own read-back
                            // check, agrees with itself, and writes a device record of the right
                            // key VERSION holding nothing. Every later remembered unlock then
                            // fails metadata authentication, and nothing reports why.
                            //
                            // The record goes, on the same reasoning as the catch below: the
                            // rotation is committed and stays reported as done, and what is
                            // discarded is a wrap that cannot open anything.
                            withdrawDeviceRecordIfLocked(generation);
                        }
                    } catch (RuntimeException rewrapFailed) {
                        // The same discard as the success path above, and it has to be the
                        // same DISCARD: a re-wrap that threw partway can have written the
                        // record before it failed, and a delete that is then dropped
                        // silently -- both real ports can -- leaves getPolicy() reporting a
                        // remembering policy over a wrap that opens nothing. Routed through
                        // the helper so the record's mechanism is destroyed when the record
                        // itself will not go, rather than repeating the blind delete this
                        // used to make.
                        discardDeviceRecord();
                    }
                    out.complete(Boolean.TRUE);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(fresh);
                    Bytes.zero(password);
                }
            }
        });
        return out;
    }

    // --------------------------------------------------------------- sync

    /// The vault record, as bytes to hand to a sync server or copy to a new device.
    ///
    /// Contains the wrapped data key and nothing usable: a server storing this cannot open the
    /// vault and cannot help anybody else to. The device wrap is deliberately excluded, because
    /// it is the one piece that would let a copy of this blob open the vault without the
    /// password.
    ///
    /// Keep the credentials the user logs into the sync server with separate from the vault
    /// password. A server that can verify the login must never be able to derive the vault key,
    /// which it could if they were the same string.
    public byte[] exportSyncState() {
        // FRESH, for the same reason importSyncState reads fresh. loadMetadata answers the
        // in-memory copy once the vault is open, so an instance holding version N exported N
        // after another tab had already moved the shared vault to N+1 -- and the import side's
        // own fresh read cannot help, because the stale bytes have already left this device. A
        // sync server or a newly enrolled device accepting them restores the superseded password
        // wrap and omits the rotated key that newer records need.
        VaultMetadata meta = loadMetadataFresh();
        if (meta == null) {
            throw new VaultException(VaultError.KEY_MISSING, "there is no vault to export");
        }
        return Bytes.utf8(meta.serialize());
    }

    /// Enrolls this device from another device's [#exportSyncState()].
    ///
    /// Importing a rotation changes the current database key. Existing database files keep their
    /// previous keys: open them with [#databaseKey(String,int)] or a versioned
    /// [com.codename1.db.DatabaseConfig#vault] and rekey explicitly. Keep the database's key version
    /// with its local metadata; the retired key chain remains available after this import.
    ///
    /// A policy failure rolls back an untouched import and locks this session. If rollback is
    /// unsafe or cannot be confirmed, [VaultError#IMPORT_COMMITTED] reports that the import
    /// reached committed state; reread it and finish policy setup rather than assuming no change.
    ///
    /// #### Replay and rollback
    ///
    /// The record carries a counter that increases on every change. This refuses a record whose
    /// counter is **below** the one already stored, because accepting one is how a server that has
    /// been compromised, or that simply serves a stale replica, rolls a device back to a password
    /// the user has since changed. AEAD proves the record was not altered; it says nothing about
    /// whether it is the latest, and no amount of cryptography inside the record can establish
    /// that on its own. An application that needs stronger freshness has to get it from the
    /// server -- a monotonic version the server refuses to decrease, an authenticated timestamp --
    /// and an offline client cannot detect a rollback at all beyond what this counter catches.
    ///
    /// #### Parameters
    ///
    /// - `state`: bytes from [#exportSyncState()] on another device
    ///
    /// - `password`: the vault password, cleared by this method
    public AsyncResource<Boolean> importSyncState(final byte[] state, final char[] password) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                // Owned for the whole worker, like every other path that derives one. This is the
                // real data key from the moment the wrap opens, and the steps after that can all
                // throw -- requirePolicySupported, requireProtections, or commitMetadata losing
                // to a concurrent write. Two places wiped it by hand and the rest fell through to
                // a finally that cleared only the password, so a failed import left usable key
                // material in the heap for a vault it never adopted.
                byte[] key = null;
                try {
                    requirePlausibleSyncState(state);
                    VaultMetadata incoming = VaultMetadata.parse(
                            Bytes.fromUtf8(state, 0, state == null ? 0 : state.length));
                    if (incoming == null) {
                        throw new VaultException(VaultError.CORRUPT, "the sync state is empty");
                    }
                    // FRESH, not the cached record. loadMetadata answers the in-memory copy
                    // once the vault is open, so this compared an incoming record against
                    // whatever this instance happened to be holding: another tab that had
                    // already rotated the shared vault to N+1 was invisible, an authentic copy
                    // of N passed the equal-counter check against the stale N, and the write
                    // below then put N back over the persisted N+1 -- losing the rotation and
                    // with it every record the other tab had sealed under the new key.
                    VaultMetadata local = loadMetadataFresh();
                    if (local != null) {
                        if (!local.vaultId.equals(incoming.vaultId)) {
                            throw new VaultException(VaultError.CONFLICT,
                                    "the sync state belongs to a different vault than the one "
                                    + "already on this device");
                        }
                        if (incoming.counter < local.counter) {
                            throw new VaultException(VaultError.CONFLICT,
                                    "the sync state is older than the record already on this "
                                    + "device; refusing to roll back");
                        }
                        if (incoming.counter == local.counter
                                && !incoming.serialize().equals(local.serialize())) {
                            // Same counter, different content: two devices changed from the same
                            // base and neither is newer. Accepting either silently discards the
                            // other -- and when both rotated, the local records sealed under the
                            // local version-N key can never be opened again, because the imported
                            // key of the same version is a different key.
                            //
                            // There is no merge to perform here. Whichever the user keeps, the
                            // other device's changes since the fork are lost, and that is a
                            // decision for the application and its sync layer rather than for a
                            // comparison of two integers.
                            throw new VaultException(VaultError.CONFLICT,
                                    "this device and the sync state have both changed since they "
                                    + "last agreed; the vault cannot choose between them");
                        }
                    }
                    if (incoming.passwordWrap == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "the sync state carries no password wrap, so it cannot enroll "
                                + "this device");
                    }
                    // Opened before it is stored. A record that does not unwrap under this
                    // password would replace a working local record with one that cannot be used.
                    key = SecureEnvelope.parse(incoming.passwordWrap).openWithPassword(
                            password, wrapBinding(incoming, PURPOSE_PASSWORD));
                    // Checked against the key this record itself describes, which is the only
                    // point at which the counter it claims can be believed.
                    requireAuthenticRecord(incoming, key);
                    if (local != null) {
                        requireMetadataAncestry(local, incoming);
                        requireKeyContinuity(local, incoming, key);
                    }
                    // The configured policy governs this enrolment too. Importing sync state is
                    // how a SECOND device joins a vault, so it is an enrolment entry point in
                    // everything but name -- and it went from authentication straight to commit,
                    // so a device configured REMEMBER_DEVICE or REQUIRE_USER_VERIFICATION, or
                    // with unmet require(...), reported success and ended up session-only.
                    //
                    // Judged BEFORE the lock check, because both paths below commit. Sitting
                    // after it meant a lock landing during the key derivation took the branch
                    // that writes the record and reports LOCKED -- so an import asking for a
                    // policy this device cannot support, or for a protection it does not have,
                    // enrolled the device anyway and left it session-only, with the only error
                    // the caller saw naming the lock. Both checks are pure: they read
                    // capabilities and throw, so hoisting them changes nothing else.
                    requirePolicySupported(options.getPolicy());
                    requireProtections();
                    if (generation != lockGeneration()) {
                        // The record is still written -- enrolling this device is the point of the
                        // call and it succeeded. What is refused is leaving the vault unlocked
                        // afterwards, because the application asked for it to be locked.
                        commitMetadata(local, incoming);
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while the sync state was being imported");
                    }
                    // Committed against the record that was read above, so a tab that wrote
                    // between the comparison and here loses rather than being overwritten.
                    commitMetadata(local, incoming);
                    // Through the same lock-aware handoff as every other publication: the check
                    // above is before the commit, and the commit is a storage write wide enough
                    // for a lock to land inside. publishKey refuses to install a key if it did,
                    // so an overlapping lock leaves the vault closed rather than reopened.
                    publishKey(generation, incoming, key);
                    // Ownership transferred, so the finally must not wipe what the vault now
                    // holds. This used to be a dead store and is not any more: the finally below
                    // zeroes the key, which is the whole of the fix above.
                    key = null;
                    // Established after the record is committed, because remembering wraps the
                    // key this record describes. A failure here is reported rather than
                    // swallowed: the device is enrolled, but not under the policy that was asked
                    // for, and the caller has to know that.
                    try {
                        if (options.getPolicy() == UnlockPolicy.SESSION_ONLY) {
                            // A session-only import over a vault that already has a remembered
                            // mechanism has to REMOVE it, not merely decline to add one. Skipping
                            // straight past this left the old device record and its key in place, so
                            // the import reported success while getPolicy() still answered the
                            // previous remembering policy and unlockRemembered still opened the vault
                            // without a password -- the opposite of what the caller configured.
                            // Through the three-state read: deviceRecord() answers null both for
                            // "there is none" and for "there is one and it could not be read", and
                            // collapsing those let a transient failure skip the cleanup while the
                            // import reported success. Once storage recovers the old record makes
                            // getPolicy() report a remembering policy again, and at an unchanged key
                            // version unlockRemembered reopens the vault without a password.
                            if (deviceRecordState() == ProtectionReport.UNKNOWN) {
                                throw new VaultException(VaultError.TEMPORARILY_UNREADABLE,
                                        "a device record exists here and could not be read, so this "
                                        + "import cannot make the vault session-only");
                            }
                            DeviceRecord standing = deviceRecord();
                            if (standing != null) {
                                Storage.getInstance().deleteStorageFile(deviceRecordKey());
                                // Before the key goes, and before this reports success -- the check
                                // setPolicy already makes on the same transition. deleteStorageFile
                                // returns void and both real ports can fail one silently, and a
                                // record that survived leaves getPolicy() answering the old
                                // remembering policy while unlockRemembered follows it to a key that
                                // has since been deleted.
                                if (!definitelyGone(deviceRecordKey())) {
                                    throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                                            "the device record could not be removed, so this import "
                                            + "cannot make the vault session-only");
                                }
                                requireKeyDeleted(deviceProtection(standing.policy),
                                        "the previous device key could not be deleted");
                            }
                        } else {
                            rememberNow(options.getPolicy());
                            // The import is committed; a lock during the prompt invalidates only
                            // the device wrap, which must not remain capable of reopening it.
                            withdrawDeviceRecordIfLocked(generation);
                        }
                    } catch (RuntimeException policyFailed) {
                        failImportPolicy(local, incoming, policyFailed);
                    }
                    touch();
                    out.complete(Boolean.TRUE);
                } catch (VaultException failed) {
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    out.error(asVaultException(broke, "this vault operation could not complete"));
                } finally {
                    Bytes.zero(key);
                    Bytes.zero(password);
                }
            }
        });
        return out;
    }

    /// An import may be adopted by another session before its policy work finishes. Rollback
    /// must not orphan ciphertext that session wrote under an incoming rotated key.
    private void failImportPolicy(VaultMetadata local, VaultMetadata incoming,
            RuntimeException policyFailed) {
        try {
            if (local != null && local.serialize().equals(incoming.serialize())) {
                throw policyFailed;
            }
            boolean restored = false;
            try {
                if (vaultIsStillUntouched(incoming)) {
                    if (local != null) {
                        commitMetadata(incoming, local);
                        restored = true;
                    } else {
                        Storage.getInstance().deleteStorageFile(metadataKey());
                        restored = definitelyGone(metadataKey());
                        if (restored) {
                            try {
                                forgetEveryMechanism();
                            } catch (RuntimeException ignored) {
                                // No vault remains for a partial device key to open.
                            }
                        }
                    }
                }
            } catch (RuntimeException rollbackFailed) {
                throw new VaultException(VaultError.IMPORT_COMMITTED,
                        "the import committed, its policy setup failed, and rollback could not "
                        + "be confirmed; reread the stored state before retrying", rollbackFailed);
            }
            if (!restored) {
                throw new VaultException(VaultError.IMPORT_COMMITTED,
                        "the import committed, but its configured policy could not be established; "
                        + "the imported state was retained to protect data and this session was "
                        + "locked; unlock with the imported credentials and retry setPolicy",
                        policyFailed);
            }
            throw policyFailed;
        } finally {
            lock();
        }
    }

    // ------------------------------------------------------------ policy

    /// Changes the unlock policy, doing the work the new policy implies.
    ///
    /// Moving to [UnlockPolicy#SESSION_ONLY] or [UnlockPolicy#REQUIRE_USER_VERIFICATION] deletes
    /// the unattended device wrap. That deletion is the whole point: a policy that promises a
    /// prompt while an alternative unlock sits beside it promises nothing.
    public AsyncResource<Boolean> setPolicy(final UnlockPolicy policy) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration();
        background(new Runnable() {
            @Override
            public void run() {
                // Everything this call may disturb, captured before it disturbs any of it: the
                // configured policy and the device record as found. A transition that fails part
                // way used to leave both half-applied -- options already moved to the target the
                // transition never reached, so the vault reported one policy and every later
                // rememberDevice or import used the other, and a shared VaultOptions carried that
                // into whatever else was configured with it; and on a failure AFTER rememberNow
                // the new record stood, so a retry saw current.policy == policy, skipped the
                // cleanup for good, and reported success.
                UnlockPolicy configuredBefore = options.getPolicy();
                // Snapshotted through the three-state reader, and refused outright when it cannot
                // be taken. The cached read collapsed "there is no record" and "there is one and
                // it could not be read" into the same null -- and the rollback below deletes the
                // record whenever the snapshot is not a String, so a transient failure turned a
                // setPolicy REJECTED before it changed anything into one that removed the user's
                // remembered unlock. Nothing this method can do afterwards is safe if it does not
                // know what it is rolling back to.
                //
                // Both reads are wrapped, and deliberately NOT moved into the main try below.
                // They can throw: readUncached answers TEMPORARILY_UNREADABLE when storage
                // cannot say whether the record is there, and deviceRecordState goes through it
                // -- so an exception here escaped a worker that had not completed `out`, and a
                // caller blocked in get() waited for an answer that was never coming. A hang is
                // the one failure this class refuses to produce.
                //
                // The main try is the wrong home for them because its catches roll back first,
                // and the rollback deletes the device record whenever the snapshot is not a
                // String -- so routing a read failure through it would remove the user's
                // remembered unlock on a call that changed nothing, which is exactly what the
                // guard below exists to prevent.
                Object savedRecord;
                try {
                    if (deviceRecordState() == ProtectionReport.UNKNOWN) {
                        out.error(new VaultException(VaultError.TEMPORARILY_UNREADABLE,
                                "a device record exists here and could not be read, so this "
                                + "policy change cannot be undone if it fails; nothing was "
                                + "changed"));
                        return;
                    }
                    savedRecord = readUncached(deviceRecordKey());
                } catch (RuntimeException cannotRead) {
                    out.error(asVaultException(cannotRead,
                            "this device's remembered-unlock record could not be read, so the "
                            + "policy change was not started"));
                    return;
                }
                boolean settled = false;
                // An array because the finally below reads it and this is a Java 5 source level;
                // what it records is the one step of this transition that cannot be undone.
                final boolean[] outgoingKeyGone = {false};
                // One-shot, because the rollback now runs from the catches AND from the finally.
                final boolean[] undone = {false};
                try {
                    requirePolicySupported(policy);
                    // Judged against the policy being moved TO, and before options is mutated or
                    // any record is deleted, so a refusal leaves the vault exactly as it was.
                    // Enrollment enforces require(...) and this did not, so a vault enrolled
                    // SESSION_ONLY with require(ENCRYPTED_AT_REST) could move to REMEMBER_DEVICE
                    // and persist its wrapping key in a store that reports ENCRYPTED_AT_REST=NO
                    // -- the JavaSE simulator, or Android below API 23. Supported and permitted
                    // are different questions, and only the first was being asked.
                    requireProtections(policy);
                    options.policy(policy);
                    DeviceRecord current = deviceRecord();
                    UnlockPolicy previous = current == null
                            ? UnlockPolicy.SESSION_ONLY : current.policy;
                    if (policy == UnlockPolicy.SESSION_ONLY) {
                        Storage.getInstance().deleteStorageFile(deviceRecordKey());
                        // Checked, because deleteStorageFile reports nothing: it returns void,
                        // and both real ports can fail one silently. A record that survived
                        // leaves getPolicy() answering the old remembering policy, and a later
                        // move BACK to it then finds current.policy == policy and skips the
                        // enrolment -- a remembered unlock permanently without its key.
                        if (!definitelyGone(deviceRecordKey())) {
                            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                                    "the device record could not be removed, so this vault is "
                                    + "not session-only");
                        }
                        requireKeyDeleted(deviceProtection(previous),
                                "the device key could not be deleted");
                    } else if (current == null || current.policy != policy) {
                        requireUnlocked();
                        // The incoming mechanism is established and proven BEFORE the outgoing
                        // one is discarded. Deleting first meant that a user who dismissed the
                        // passkey prompt on the way from REMEMBER_DEVICE to
                        // REQUIRE_USER_VERIFICATION got an error AND lost the remembered unlock
                        // they already had -- the vault silently demoted to session-only by a
                        // call that failed.
                        // Uncached, like every other device-record read here: this is the
                        // record the rollback below puts back, so reading a stale copy of it
                        // would restore a record another tab had already replaced.
                        Object saved = readUncached(deviceRecordKey());
                        try {
                            rememberNow(policy);
                        } catch (VaultException establishFailed) {
                            if (establishFailed.getError() == VaultError.CONFLICT) {
                                // rememberNow never published a stale wrap, or withdrew only
                                // its own attempt. Restoring the pre-prompt record here would
                                // overwrite the wrap a concurrent rotation already refreshed.
                                options.policy(configuredBefore);
                                undone[0] = true;
                                throw establishFailed;
                            }
                            // rememberNow writes the device record, so a failure partway can
                            // leave it describing a mechanism that was never completed. Put back
                            // exactly what was there and report the failure: nothing changed.
                            // Only over a record this attempt is responsible for. Another tab
                            // completing its own policy change while rememberNow waited on a
                            // prompt leaves ITS record here, and writing `saved` over that
                            // silently reverts a call that already reported success -- and can
                            // restore a record naming a mechanism that tab has since deleted.
                            //
                            // Whose it is cannot be told from the bytes, but it can from the
                            // POLICY: this attempt was installing `policy`, so a record naming
                            // anything else was put there by somebody else and is left alone. A
                            // second tab installing the SAME policy is the benign case either
                            // way -- what gets restored then is a record equivalent to the one
                            // it wrote.
                            DeviceRecord standing = deviceRecord();
                            if (saved instanceof String
                                    && (standing == null || standing.policy == policy)) {
                                requireDeviceRecordRestored(deviceRecordKey(), (String) saved,
                                        establishFailed);
                            }
                            throw establishFailed;
                        }
                        // Removed through the OUTGOING policy's protection, because on a port
                        // where the two policies are different mechanisms -- a stored key and a
                        // passkey in the browser -- asking the incoming one to delete would leave
                        // the outgoing key in place. That is the unattended wrap a stronger policy
                        // exists to remove.
                        //
                        // Skipped when the two resolve to the SAME protection, which they do on
                        // any port with no user-verifying variant: deviceProtection() falls back
                        // to the base store, so both policies share one store and one key id, and
                        // deleting the outgoing key there would delete the incoming one that was
                        // just written.
                        DeviceProtection outgoing = deviceProtection(previous);
                        // Snapshotted, not re-derived. deviceProtection() asks the store for its
                        // user-verifying variant each time, and on the browser that answer
                        // depends on a capability probe -- so calling it again here could compare
                        // against a different object than the one this transition actually used
                        // and delete the outgoing key when the two are in fact the same store.
                        DeviceProtection incoming = deviceProtection(policy);
                        // Identity is exactly the question -- whether the two policies resolve to
                        // the SAME store object, in which case deleting the outgoing key would
                        // delete the incoming one that was just written -- so equals() would be
                        // wrong here as well as meaningless for an SPI implementation. PMD only
                        // started seeing this when the right-hand side stopped being a method
                        // call; the comparison itself has not changed.
                        if (outgoing != incoming) { //NOPMD CompareObjectsWithEquals
                            requireKeyDeleted(outgoing,
                                    "the previous device key could not be deleted");
                            outgoingKeyGone[0] = true;
                        }
                        // null: setPolicy has already deleted the OUTGOING device key by this
                        // point, so the record it replaced names a key that no longer exists and
                        // putting it back would restore a remembered unlock that cannot work.
                        requireDeviceRecordStillWanted(generation, null);
                    }
                    settled = true;
                    out.complete(Boolean.TRUE);
                } catch (VaultException failed) {
                    // Rolled back BEFORE the failure is published. An error callback runs off
                    // this completion and is free to call setPolicy again, and background()
                    // starts a new thread per call -- so a retry from that callback used to run
                    // concurrently with a finally that then wrote the old record back over what
                    // the retry had just settled, while the caller had been told the retry
                    // succeeded. Completing means the persisted state is settled.
                    if (!undoPolicyChange(configuredBefore, savedRecord, outgoingKeyGone[0],
                            undone)) {
                        // The original failure goes on as the cause. "The policy change failed"
                        // and "and this device is not back on the policy it had" are both things
                        // the caller needs, and only the second changes what they can do next.
                        out.error(new VaultException(VaultError.STORAGE_UNAVAILABLE,
                                "the policy change failed and the device record it replaced "
                                + "could not be put back, so this device is not on the policy it "
                                + "started with", failed));
                        return;
                    }
                    out.error(failed);
                } catch (RuntimeException broke) {
                    // A worker that throws anything else must still ANSWER. These run detached,
                    // so an escaping exception used to end the thread with the AsyncResource
                    // never completed -- and a caller blocked in get() waits for that forever.
                    // A hang is a worse failure than an error, and it is the one the caller
                    // cannot diagnose. Reached most easily by locking mid-operation, which nulls
                    // metadata under a worker that already passed requireUnlocked.
                    VaultException reported =
                            asVaultException(broke, "this vault operation could not complete");
                    if (!undoPolicyChange(configuredBefore, savedRecord, outgoingKeyGone[0],
                            undone)) {
                        reported = new VaultException(VaultError.STORAGE_UNAVAILABLE,
                                "the policy change failed and the device record it replaced "
                                + "could not be put back, so this device is not on the policy it "
                                + "started with", reported);
                    }
                    out.error(reported);
                } finally {
                    // Still here as a backstop for any route out that is not one of those two.
                    // Idempotent, so the common paths do not undo twice.
                    if (!settled) {
                        // Back to exactly what was found, by every route out that is not success
                        // -- which is what "nothing changed" has to mean for a call that reports
                        // failure. The inner handler around rememberNow restores the record too
                        // and that is deliberate redundancy: it is the one place that can put the
                        // record back before the OUTGOING key is deleted, and this runs after.
                        undoPolicyChange(configuredBefore, savedRecord, outgoingKeyGone[0],
                                undone);
                    }
                }
            }
        });
        return out;
    }

    /// The policy this device is operating under.
    public UnlockPolicy getPolicy() {
        DeviceRecord record = deviceRecord();
        return record == null ? UnlockPolicy.SESSION_ONLY : record.policy;
    }

    // ------------------------------------------------------------ internals

    /// Opens an envelope that may have been sealed under a retired key version, walking the chain
    /// in the vault record one link at a time.
    byte[] openAnyVersion(byte[] sealed, AssociatedData aad) {
        SecureEnvelope envelope = SecureEnvelope.parse(sealed);
        byte[] key = dataKeyAtVersion(envelope.getKeyVersion());
        try {
            return envelope.open(key, aad);
        } finally {
            Bytes.zero(key);
        }
    }

    /// The data key as it was at a given version, walking the retired chain back to it.
    ///
    /// Always a copy, even for the current version, so the caller can zero what it is given
    /// without reaching into the vault's own key.
    byte[] dataKeyAtVersion(int wanted) {
        // Both fields SNAPSHOTTED and checked here, rather than read live inside keyAtVersion.
        // lock() nulls metadata and dataKey together, and a lock landing between a caller's
        // requireUnlocked() and this read made keyAtVersion dereference a null record --
        // a NullPointerException thrown synchronously out of an API whose callers catch
        // VaultException, so VaultKeyHandle.open() never completed its AsyncResource with
        // LOCKED and the exception escaped to whatever was on the stack instead.
        //
        // Both callers are on this path: openAnyVersion, which every retired-version read goes
        // through, and subkeyAtVersion behind a post-rotation KeyHandle.
        VaultMetadata record = sessionMetadata();
        byte[] live = sessionKey();
        if (record == null || live == null) {
            throw new VaultException(VaultError.LOCKED,
                    "the vault was locked while a record sealed under an earlier key version "
                    + "was being opened");
        }
        return keyAtVersion(record, live, wanted);
    }

    /// Walks `record`'s retired chain back from its current data key to version `wanted`.
    ///
    /// Takes the record and its key as parameters rather than reading the open vault, because
    /// [#importSyncState] has to walk a record that is not this device's -- see the key
    /// continuity check there. The answer is a fresh array the caller owns and must zero;
    /// `currentKey` is left alone.
    private byte[] keyAtVersion(VaultMetadata record, byte[] currentKey, int wanted) {
        int current = record.dataKeyVersion;
        if (wanted > current) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "this record was sealed under data key version " + wanted
                    + " and this device only knows about version " + current
                    + "; synchronise before reading it");
        }
        byte[] key = new byte[32];
        System.arraycopy(currentKey, 0, key, 0, 32);
        boolean reached = false;
        try {
            for (int version = current - 1; version >= wanted; version--) {
                byte[] link = record.retired.get(Integer.valueOf(version));
                if (link == null) {
                    throw new VaultException(VaultError.KEY_MISSING,
                            "the key chain in this vault does not reach version " + wanted);
                }
                byte[] older = SecureEnvelope.parse(link).open(key,
                        binding(record, DATA_KEY_RECORD, PURPOSE_RETIRED + "." + version));
                Bytes.zero(key);
                key = older;
            }
            reached = true;
            return key;
        } finally {
            if (!reached) {
                Bytes.zero(key);
            }
        }
    }

    /// The operational subkey for a purpose as it was at a given data key version.
    ///
    /// Rotation changes the data key, and every subkey is derived from it, so a handle acquired
    /// after a rotation derives something different from the one that sealed an older record.
    /// KeyHandle#getVersion promises those records stay readable, which means the older subkey
    /// has to be reachable -- through the same retired chain the vault's own records use.
    byte[] subkeyAtVersion(String purpose, int wanted) {
        requireUnlocked();
        byte[] source = dataKeyAtVersion(wanted);
        try {
            return deriveSubkey(source, purpose);
        } finally {
            Bytes.zero(source);
        }
    }

    /// Stamps the record's authentication tag, under the key that record describes.
    ///
    /// Taken as a parameter rather than read from the field, because rotation writes the record
    /// for its NEW key while the vault is still holding the old one.
    /// Signs `meta` and records that it descends from `base`.
    ///
    /// One place, because every mutation of the record goes through here on its way to storage,
    /// and ancestry that is stamped at some of them is worse than none: an import would read a
    /// missing link as a fork and refuse a sync that was perfectly ordinary. `base` is null only
    /// for enrolment, which descends from nothing.
    private void stampMac(VaultMetadata meta, VaultMetadata base, byte[] key) {
        meta.ancestors.clear();
        if (base != null) {
            meta.ancestors.addAll(base.ancestors);
            String parent = fingerprint(base.mac);
            if (parent != null) {
                meta.ancestors.add(parent);
            }
            while (meta.ancestors.size() > VaultMetadata.MAX_ANCESTORS) {
                meta.ancestors.remove(0);
            }
        }
        meta.mac = null;
        meta.mac = recordMac(meta, key);
    }

    /// How a record is named in another record's ancestry, or null when it has no tag to name.
    private static String fingerprint(byte[] mac) {
        if (mac == null || mac.length < VaultMetadata.FINGERPRINT_BYTES) {
            return null;
        }
        byte[] head = new byte[VaultMetadata.FINGERPRINT_BYTES];
        System.arraycopy(mac, 0, head, 0, head.length);
        return Bytes.toHex(head);
    }

    /// Verifies the tag, refusing a record whose counter or contents were edited.
    ///
    /// This is what makes the rollback check mean anything. The counter is plaintext, so a server
    /// serving sync state could take an OLD record, raise its counter above the local one, and
    /// still pass the password wrap -- the wrap authenticates the vault id, the key version and
    /// the purpose, and says nothing about the counter or the retired chain. The newer key was
    /// then overwritten and every record sealed after the rotation became unreadable.
    ///
    /// A missing tag is refused too. Accepting one would leave the whole check optional for
    /// anyone able to delete a line.
    private void requireAuthenticRecord(VaultMetadata meta, byte[] key) {
        byte[] expected = recordMac(meta, key);
        try {
            if (meta.mac == null || !Bytes.constantTimeEquals(expected, meta.mac)) {
                throw new VaultException(VaultError.AUTHENTICATION_FAILED,
                        "the vault record does not authenticate under this password; its "
                        + "contents have been altered");
            }
        } finally {
            Bytes.zero(expected);
        }
    }

    /// Refuses an incoming sync record that is not a descendant of the one already here.
    ///
    /// Key continuity below is the half of this that protects readability, and it is not the whole
    /// question. A fork that never rotated keeps the same data key on both sides, so it passes
    /// that check while its metadata changes are still unrelated: one device changes its password
    /// once and reaches counter 2, the other replaces its recovery code twice and reaches counter
    /// 3, and importing the second silently restores the first device's OLD password wrap. The
    /// user's password change is gone, with no error anywhere -- and again the counters cannot
    /// say so, because counting mutations is not the same as ordering them.
    ///
    /// So each record carries the fingerprints of the records it came from, and the test is
    /// whether the incoming one names the local one among them. That is a real causal test rather
    /// than a comparison of two integers: a device that is simply ahead always names what it
    /// passed through, and a device that diverged never does, whichever way its counter went.
    ///
    /// Two records that are byte-identical are the same record, not a fork -- a refresh of a
    /// device that is already up to date arrives that way and has to keep working.
    private void requireMetadataAncestry(VaultMetadata local, VaultMetadata incoming) {
        String here = fingerprint(local.mac);
        if (here == null) {
            // No tag to be named by, so ancestry cannot be established either way. The counter and
            // key-continuity checks are what is left; both are weaker, and neither is new.
            return;
        }
        if (incoming.serialize().equals(local.serialize())) {
            return;
        }
        if (incoming.ancestors.contains(here)) {
            return;
        }
        String there = fingerprint(incoming.mac);
        if (there != null && local.ancestors.contains(there)) {
            throw new VaultException(VaultError.CONFLICT,
                    "the sync state is an earlier version of the vault already on this device; "
                    + "refusing to roll back");
        }
        throw new VaultException(VaultError.CONFLICT,
                "this device and the sync state have both changed since they last agreed; the "
                + "vault cannot choose between them. This is also what a device more than "
                + VaultMetadata.MAX_ANCESTORS + " changes behind looks like, because a record "
                + "carries no more ancestry than that");
    }

    /// Refuses an incoming sync record whose key lineage does not contain the one this device
    /// is already using.
    ///
    /// The counter alone cannot decide this. It counts mutations on whichever device made them,
    /// so it is not a causal clock: two devices that diverge from the same base can each raise it
    /// a different number of times, and the one that ends up higher is not necessarily a
    /// descendant of the other. A device that rotates once reaches counter 2 with a version-2 key;
    /// a device that changes its password twice reaches counter 3 still holding the version-1 key.
    /// Importing the second over the first passes every counter test there is, and overwrites the
    /// version-2 key and its retired chain with a record that has never heard of them -- after
    /// which every record the first device sealed since rotating is permanently unreadable.
    ///
    /// What is checked instead is continuity: walk the INCOMING record's retired chain back to the
    /// version the local record is on, and require the key that comes out to be the local one. A
    /// genuine descendant always passes, because rotation retires the key it replaces into exactly
    /// that chain. A fork does not, whichever way its counter went.
    ///
    /// The local key is identified by its MAC rather than by unwrapping it. Unwrapping would need
    /// the local password, and the record being imported is frequently the one that CHANGED the
    /// password -- so the local wrap is under a password this caller no longer has, and a
    /// legitimate password-change sync would be refused. The MAC is keyed by the record's own data
    /// key, so recomputing it under the candidate ancestor answers the same question without one.
    private void requireKeyContinuity(VaultMetadata local, VaultMetadata incoming, byte[] incomingKey) {
        if (local.mac == null) {
            // Nothing to check the ancestor against. Fall back on the one thing that is still
            // certain: a chain that ends below the local version cannot contain the local key.
            if (incoming.dataKeyVersion < local.dataKeyVersion) {
                throw new VaultException(VaultError.CONFLICT, CONTINUITY_MESSAGE);
            }
            return;
        }
        byte[] ancestor;
        try {
            ancestor = keyAtVersion(incoming, incomingKey, local.dataKeyVersion);
        } catch (VaultException noChain) {
            // Either the incoming record never reached the local version, or its chain is
            // incomplete. Both mean the same thing here, and neither is the error the walk
            // reports to a reader opening an old record. The original is kept as the cause: it
            // says WHICH of the two happened, which is the only thing that would diagnose a
            // genuinely damaged record rather than an ordinary fork.
            throw new VaultException(VaultError.CONFLICT, CONTINUITY_MESSAGE, noChain);
        }
        try {
            byte[] expected = recordMac(local, ancestor);
            try {
                if (!Bytes.constantTimeEquals(expected, local.mac)) {
                    throw new VaultException(VaultError.CONFLICT, CONTINUITY_MESSAGE);
                }
            } finally {
                Bytes.zero(expected);
            }
        } finally {
            Bytes.zero(ancestor);
        }
    }

    private static final String CONTINUITY_MESSAGE =
            "the sync state does not descend from the vault already on this device; importing it "
            + "would discard the key this device is using and everything sealed under it";

    /// The tag over one record, computed without touching it.
    ///
    /// It used to null `meta.mac` for the duration and put it back in a finally, which was both
    /// unnecessary and unsafe: serializeForMac already excludes the tag -- that is the whole
    /// reason it exists beside serialize -- and `meta` is frequently the shared `metadata`
    /// object, so the window exposed a record with no tag to any other worker. createRecoveryCode
    /// reading base.mac == null in that window stamps an update with no parent fingerprint, which
    /// later makes an ordinary sync import look like a fork; two verifications overlapping can
    /// also fail authentication against each other for no reason.
    private byte[] recordMac(VaultMetadata meta, byte[] key) {
        Hmac mac = Hmac.create(Hash.SHA256, key);
        mac.update(Bytes.utf8("cn1.vault.record.v1"));
        mac.update(Bytes.utf8(meta.serializeForMac()));
        return mac.doFinal();
    }

    /// The one subkey derivation, so the live handle and a recovered older one cannot drift.
    static byte[] deriveSubkey(byte[] source, String purpose) {
        Hmac mac = Hmac.create(Hash.SHA256, source);
        mac.update(Bytes.utf8("cn1.vault.subkey.v1"));
        mac.update(Bytes.utf8(purpose == null ? "" : purpose));
        return mac.doFinal();
    }

    /// A borrowed key reference, read under the publication monitor. Operations still check
    /// their captured generations before delivering results; lock() may wipe this array.
    private synchronized byte[] sessionKey() {
        return dataKey;
    }

    /// Reads the published cache under the same monitor as lock() and adoptSession().
    private synchronized VaultMetadata sessionMetadata() {
        return metadata;
    }

    private void requireUnlocked() {
        checkAutoLock();
        VaultMetadata session = sessionMetadata();
        if (sessionKey() == null || session == null) {
            throw new VaultException(VaultError.LOCKED, "the vault is locked");
        }
        // Other browser tabs share storage, but not this session or Storage's cache.
        // A key whose persisted wrap was deleted must never produce new ciphertext.
        VaultMetadata stored = loadMetadataFresh();
        if (stored == null || !stored.serialize().equals(session.serialize())) {
            lock();
            throw new VaultException(stored == null ? VaultError.LOCKED : VaultError.CONFLICT,
                    "another session removed or changed this vault; unlock it again before use");
        }
        touch();
    }

    private void requireProtections() {
        requireProtections(options.getPolicy());
    }

    /// The required protections, judged against the policy named here rather than the one
    /// currently configured.
    ///
    /// setPolicy needs the distinction: it is asking whether the policy it is about to move TO
    /// still satisfies what this vault was required to have, and the configured policy at that
    /// moment is the one being left behind.
    private void requireProtections(UnlockPolicy policy) {
        Protection[] required = options.getRequired();
        if (required.length == 0) {
            return;
        }
        ProtectionReport report = capabilities().protectionFor(policy);
        Protection unmet = report.firstUnmet(required);
        if (unmet != null) {
            throw new VaultException(VaultError.POLICY_NOT_MET,
                    "this device does not provide " + unmet.name()
                    + ", which this vault was required to have", unmet, null);
        }
    }

    private void requirePolicySupported(UnlockPolicy policy) {
        if (!capabilities().supports(policy)) {
            throw new VaultException(VaultError.POLICY_NOT_MET,
                    "this device cannot provide the " + policy.name() + " unlock policy",
                    policy == UnlockPolicy.REQUIRE_USER_VERIFICATION
                            ? Protection.USER_VERIFICATION : Protection.PERSISTENT, null);
        }
    }

    private synchronized void checkAutoLock() {
        long idle = options.getAutoLockMillis();
        if (idle > 0 && dataKey != null
                && (System.nanoTime() - lastActivity) / 1000000L > idle) {
            lock();
        }
    }

    /// Records activity on a MONOTONIC clock.
    ///
    /// System.currentTimeMillis() is the wall clock and it moves: a user correcting the date, or
    /// an NTP step, sends it backwards, and the subtraction in checkAutoLock then goes negative
    /// -- so the vault stays open until the clock catches up, which can turn a one-minute idle
    /// timeout into hours. nanoTime has no relationship to the date and cannot be set;
    /// ShieldToken measures its own expiry this way for the same reason.
    private synchronized void touch() {
        lastActivity = System.nanoTime();
    }

    /// The lock generation, for a handle to notice that it has been invalidated.
    ///
    /// Evaluates the idle timeout first. `autoLockAfter` is documented as checked on next use,
    /// and a [KeyHandle] IS a use -- but the handle tested liveness by reading these counters
    /// directly, and nothing on that path reached checkAutoLock. An application that worked only
    /// through handles therefore never evaluated the timeout at all: the cached subkey went on
    /// sealing and opening indefinitely, while the vault reported itself unlocked to anyone who
    /// asked it directly and locked itself the moment they did.
    int generation() {
        checkAutoLock();
        return lockGeneration();
    }

    /// Counts a handle operation as activity, exactly as requireUnlocked does for the vault's own.
    ///
    /// Necessary BECAUSE of the check above, not beside it: evaluating the timeout on a path that
    /// does not also refresh it would lock a caller out in the middle of the work the timeout
    /// exists to measure -- so making handles honour the idle lock without this would replace one
    /// defect with a worse one.
    void noteHandleUse() {
        requireUnlocked();
    }

    /// The key generation, for a handle to notice that the key it derives from has been replaced.
    synchronized int keyGeneration() {
        return keyGeneration;
    }

    /// Acquires the same monitor that publishes lock()'s state clearing and counter increment.
    private synchronized int lockGeneration() {
        return lockGeneration;
    }

    /// Installs a data key, and tells every handle derived from the old one that it is stale.
    ///
    /// One place, because a counter that is bumped at some of the assignments is worse than none:
    /// a handle would keep working across exactly the rotation nobody remembered to stamp.
    private synchronized void adoptKey(byte[] key) {
        // Unlocking an already-unlocked vault would otherwise leave the previous array in the
        // heap with nothing pointing at it, which is the one copy this class can still do
        // something about.
        Bytes.zero(dataKey);
        dataKey = key;
        keyGeneration++;
    }

    private VaultMetadata requireMetadata() {
        VaultMetadata meta = loadMetadata();
        if (meta == null) {
            throw new VaultException(VaultError.KEY_MISSING,
                    "this vault is not set up on this device");
        }
        return meta;
    }

    private VaultMetadata loadMetadata() {
        VaultMetadata cached = sessionMetadata();
        if (cached != null) {
            return cached;
        }
        return loadMetadataFresh();
    }

    /// The stored record, never the cached one.
    ///
    /// A read-back that is checking whether this session's write survived has to reach storage:
    /// loadMetadata() answers the in-memory record once the vault is open, which would make
    /// every such check compare a value against itself and pass.
    private VaultMetadata loadMetadataFresh() {
        Object stored;
        try {
            stored = readUncached(metadataKey());
        } catch (RuntimeException unreadable) {
            throw new VaultException(VaultError.TEMPORARILY_UNREADABLE,
                    "the vault record could not be read from storage", unreadable);
        }
        if (!(stored instanceof String)) {
            // An entry that IS there and did not come back as a string is unreadable, not
            // absent. Storage.readObject swallows a corrupt or failed read and answers null, so
            // returning null here made state() say NOT_ENROLLED -- and enroll() is allowed over
            // NOT_ENROLLED, which would overwrite the record and orphan everything it protects.
            // The STATE_UNKNOWN guard in enrollNow already exists for exactly this; it just
            // never got the chance to fire.
            // entryState for the same reason readUncached uses it: a port that cannot tell
            // answers false here, and false is the branch that returns null and reports
            // NOT_ENROLLED. UNKNOWN goes with PRESENT, because neither is evidence of absence
            // and this is the decision enroll() is allowed to act on.
            if (Storage.getInstance().entryState(metadataKey())
                    != com.codename1.impl.CodenameOneImplementation.STORAGE_ENTRY_ABSENT) {
                throw new VaultException(VaultError.TEMPORARILY_UNREADABLE,
                        "a vault record exists here and could not be read");
            }
            return null;
        }
        return VaultMetadata.parse((String) stored);
    }

    /// Writes a metadata mutation only if the stored record is still the one it was derived
    /// from.
    ///
    /// Every mutation here is read-modify-write over a record two browser tabs share, and each
    /// one raises the counter by one from whatever it read. Checking only the rotation writer
    /// left the others able to overwrite it: a tab changing the password from cached metadata
    /// would write its stale key version and the SAME next counter over a rotation that had
    /// already published, removing the only persisted wrap and retired chain describing the new
    /// key and making every record sealed under it unreadable.
    ///
    /// Storage has writeObject and no compare-and-set, so this is optimistic concurrency and not
    /// a lock -- a writer landing between the read and the write is still possible. What it
    /// removes is the much wider window of never looking at all, and it fails closed: the loser
    /// is told CONFLICT and has changed nothing.
    private void commitMetadata(VaultMetadata base, VaultMetadata next) {
        VaultMetadata current = loadMetadataFresh();
        String expected = base == null ? null : base.serialize();
        String found = current == null ? null : current.serialize();
        boolean same = expected == null ? found == null : expected.equals(found);
        if (!same) {
            throw new VaultException(VaultError.CONFLICT,
                    "another session changed this vault while this change was being prepared; "
                    + "nothing was written here");
        }
        writeMetadata(next);
        // Read back, because the compare above and the write below it are two steps. Two tabs
        // calling changePassword from the same base record both passed that check and both wrote,
        // and both reported success -- so one of them told the user a password was installed that
        // cannot open the record that actually survived. Concurrent recovery-code creation fails
        // the same way, and neither of those callers had the settled read-back that enrolment and
        // rotation do.
        //
        // Placed HERE rather than in those two callers because this is where every metadata
        // mutation goes through, including the rollbacks that put a previous record back.
        //
        // It does NOT make the commit atomic and cannot: com.codename1.io.Storage has writeObject
        // and no compare-and-set. What it does is stop BOTH writers reporting success in the
        // overlapping case -- the one whose record did not survive now says CONFLICT, which is
        // what its caller is documented to retry from. A writer that lands after this read still
        // wins silently, and that residue is the same one enrolment records.
        VaultMetadata settled = loadMetadataFresh();
        if (settled == null || !settled.serialize().equals(next.serialize())) {
            throw new VaultException(VaultError.CONFLICT,
                    "another session changed this vault while this change was being written; "
                    + "what is stored is not what this call wrote");
        }
    }

    private void writeMetadata(VaultMetadata meta) {
        if (!Storage.getInstance().writeObject(metadataKey(), meta.serialize())) {
            throw new VaultException(VaultError.QUOTA_EXCEEDED,
                    "the vault record could not be written to storage");
        }
    }

    private DeviceRecord deviceRecord() {
        // Uncached, like the metadata read. The device record is cross-context state -- another
        // tab forgetting the device, or moving it to a different policy, writes this entry -- and
        // Storage.readObject answers a copy this context took earlier, which nothing that tab
        // does can invalidate. Found by a test whose deliberately unreadable record was invisible
        // here, which is the same thing a real second tab would have been.
        Object stored = readUncached(deviceRecordKey());
        if (!(stored instanceof String)) {
            return null;
        }
        return DeviceRecord.parse((String) stored);
    }

    /// Whether a device record is here, absent, or present and unreadable.
    ///
    /// deviceRecord() answers null for the last two alike, which is right for a caller asking
    /// "can I unlock with it" and wrong for one asking what protects this vault: a transient
    /// read failure then reported SESSION_ONLY and, through protection(), password-only
    /// protections such as ENCRYPTED_AT_REST=YES -- none of which was observed, because nothing
    /// managed to read the record that decides them.
    private int deviceRecordState() {
        Object stored = readUncached(deviceRecordKey());
        return recordStateOf(stored, stored instanceof String
                ? DeviceRecord.parse((String) stored) : null);
    }

    /// The same answer from a record ALREADY read, so a caller that needs the record itself does
    /// not read it a second time and risk describing two different states as one.
    private int recordStateOf(Object stored, DeviceRecord parsed) {
        if (stored instanceof String) {
            return parsed == null ? ProtectionReport.UNKNOWN : ProtectionReport.YES;
        }
        // Only a definite ABSENT is NO. A port that could not tell used to answer false here
        // and this reported NO -- "this device has no remembered unlock" -- which a caller is
        // entitled to act on. UNKNOWN is what the three-state report exists to carry.
        return definitelyGone(deviceRecordKey()) ? ProtectionReport.NO : ProtectionReport.UNKNOWN;
    }

    private void writeDeviceRecord(DeviceRecord record) {
        if (!Storage.getInstance().writeObject(deviceRecordKey(), record.serialize())) {
            throw new VaultException(VaultError.QUOTA_EXCEEDED,
                    "the device record could not be written to storage");
        }
    }

    /// The device protection for a given policy.
    ///
    /// [UnlockPolicy#REQUIRE_USER_VERIFICATION] gets the port's user-verifying variant, which on
    /// the browser is a different mechanism entirely -- a passkey rather than a stored key. When
    /// the port has none this falls back to the unattended one, which then fails the
    /// `requiresUserVerification` check in [#rememberNow] and in [VaultCapabilities]: the policy
    /// is refused rather than quietly downgraded, which is the whole point of it.
    private DeviceProtection deviceProtection(UnlockPolicy policy) {
        DeviceProtection base = baseDeviceProtection();
        if (policy == UnlockPolicy.REQUIRE_USER_VERIFICATION) {
            DeviceProtection gated = base.userVerifying();
            if (gated != null) {
                return gated;
            }
        }
        return base;
    }

    private DeviceProtection baseDeviceProtection() {
        DeviceProtection supplied = options.getDeviceProtection();
        if (supplied != null) {
            return supplied;
        }
        DeviceProtection fromPort = Display.getInstance().getDeviceProtection();
        return fromPort != null ? fromPort : SHARED_FALLBACK;
    }

    private static final DeviceProtection SHARED_FALLBACK = new SecureStorageDeviceProtection();

    private byte[] recoveryKey(char[] code) {
        // No stretching: a recovery code is 160 bits of machine-generated randomness, and
        // iterating over it would cost the user seconds to defend against a search nobody can
        // run. A user-chosen code would need the password path instead.
        Hmac mac = Hmac.create(Hash.SHA256, Bytes.utf8("cn1.vault.recovery.v1"));
        byte[] codeBytes = Bytes.utf8(code);
        mac.update(codeBytes);
        Bytes.zero(codeBytes);
        return mac.doFinal();
    }

    /// Deletes this vault's device key under every mechanism the port has, not only the one the
    /// record named.
    ///
    /// "Forget this device" has to mean it. A port with two mechanisms can have a leftover from a
    /// policy this vault used earlier -- a passkey enrolled, then the policy relaxed -- and a
    /// deletion that only reached the current one would leave it usable.
    /// Deletes a device key and insists it was deleted.
    ///
    /// await() unwraps the SPI's answer, and a protection that completes normally with FALSE --
    /// the browser does exactly that when the IndexedDB delete fails -- was being discarded. So
    /// "forget this device" removed the record, reported success, and left the wrapping key in
    /// place: the one outcome that promise exists to rule out, with no signal to retry.
    private void requireKeyDeleted(DeviceProtection protection, String message) {
        Boolean gone = await(protection.deleteKey(deviceKeyId()), message);
        if (gone != null && gone.booleanValue()) {
            return;
        }
        // FALSE is overloaded, and treating it as failure on its own would break "forget this
        // device" on every port that has two mechanisms. Stores answer it for "the delete
        // failed" AND for "there was nothing here" -- the test double returns
        // `keys.remove(id) != null`, which is false for an absent key, and forgetEveryMechanism
        // asks BOTH mechanisms precisely because one of them usually has nothing. Only the state
        // afterwards separates the two, so ask.
        //
        // KEY_UNKNOWN is not good enough: a store that cannot say whether the key is there
        // cannot be the evidence that it is gone.
        if (protection.keyState(deviceKeyId()) == DeviceProtection.KEY_ABSENT) {
            return;
        }
        throw new VaultException(VaultError.STORAGE_UNAVAILABLE, message);
    }

    private void forgetEveryMechanism() {
        DeviceProtection base = baseDeviceProtection();
        requireKeyDeleted(base, "the device key could not be deleted");
        DeviceProtection gated = base.userVerifying();
        if (gated != null) {
            // No check for whether this is the same object as `base`. A port whose single key
            // store is itself user-verifying returns `this` here, and deleting a key that is
            // already gone is what every store does anyway -- so the second call is a wasted
            // round trip on those ports and the correct one on the ports that have two stores.
            requireKeyDeleted(gated, "the device key could not be deleted");
        }
    }

    /// The binding for a wrap of the data key, which includes the version that key is.
    ///
    /// Without the version in here, opening a wrap authenticates the envelope and its binding and
    /// says nothing about the record around it -- so a server that serves sync state can splice a
    /// still-valid wrap of an OLD key into a record claiming a new version. Unlocking then yields
    /// the old key while the vault labels everything it seals with the new one, and it can restore
    /// a password the user has since changed away from. Both are the same trick: the wrap was
    /// genuine, the context was not.
    ///
    /// Binding the version makes the context part of what the tag covers, so a spliced wrap fails
    /// to open instead of opening into the wrong state.
    ///
    /// What this does not fix, and cannot: replaying a whole consistent older record with the
    /// counter raised. Everything inside such a record agrees with itself, and an offline client
    /// has nothing to compare it against. That is the freshness limit documented on
    /// [#importSyncState].
    /// Publishes a recovered key only if no lock has invalidated the operation.
    ///
    /// Storage checks may yield. After they finish, the generation check and both state writes
    /// share lock()'s monitor, so a stale unlock never installs a key. No monitor is held across
    /// storage or device operations.
    private void publishKey(int generation, VaultMetadata meta, byte[] key) {
        // The stored record has to still be the one this key was derived FROM. Deriving takes as
        // long as the KDF profile asks, and a rotation committing inside that window leaves this
        // about to publish version N over a vault whose persisted record says N+1 -- both calls
        // reporting success, this instance holding the old database key and storage holding the
        // new one. A caller that then derives a database key after the rotation it was told
        // succeeded rekeys with the superseded value and cannot reopen the database after a
        // restart.
        //
        // Compared as METADATA rather than by the key generation, which would also fire for a
        // second unlock of an already-open vault publishing the very same key -- a benign
        // duplicate this must not turn into an error. A record that has not changed means
        // nothing rotated, whoever else is unlocking.
        //
        // All four publish paths go through here: the two password unlocks, the remembered one,
        // and the import. Only the first was reported.
        VaultMetadata settled = loadMetadataFresh();
        if (settled == null || !settled.serialize().equals(meta.serialize())) {
            throw new VaultException(VaultError.CONFLICT,
                    "this vault's record changed while the key was being derived, so the key this "
                    + "unlock produced is not the one the vault now describes; try again");
        }
        adoptSession(generation, meta, key);
    }

    /// Enrollment, unlock and rotation publish both fields under the lock monitor. A stale
    /// operation is rejected before it can make even a temporary unlocked session visible.
    private synchronized void adoptSession(int generation, VaultMetadata meta, byte[] key) {
        requireSameGeneration(generation);
        // Polling threads must never see a live key paired with the previous idle timestamp.
        touch();
        metadata = meta;
        adoptKey(key);
    }

    /// Reports unlock success only while the published session has not been locked again.
    private void completeUnlock(AsyncResource<Boolean> out, int generation) {
        completeUnlock(out, generation, null);
    }

    private void completeUnlock(AsyncResource<Boolean> out, final int generation,
            final Boolean needsRewrap) {
        out.complete(Boolean.TRUE, this, new Runnable() {
            @Override
            public void run() {
                requireSameGeneration(generation);
                if (needsRewrap != null) {
                    setPasswordNeedsRewrap(needsRewrap.booleanValue());
                }
            }
        });
    }

    /// Withdraws a device record that a lock landed on top of, and reports the lock.
    ///
    /// The device wrap is a way back into this vault without a password, and establishing one runs
    /// through a store that prompts -- a passkey, a keystore with user verification -- so the write
    /// takes as long as the user takes. `lock()` can land anywhere inside that. Every other path
    /// that publishes something reopenable already checks the generation it started on; these two
    /// did not, so a vault the application had asked to close was left with a fresh passwordless
    /// unlock on disk, and the call reported success.
    ///
    /// `restore` is the record this call overwrote, serialized, or null when it created one.
    /// Deleting is the right rollback only in the second case. The first version of this took no
    /// such argument and always deleted, on the claim that "there was nothing here to put back" --
    /// true of setPolicy, whose outgoing key has already been deleted by the time it gets here, and
    /// false of rememberDevice, which rewraps an existing enrolment under the same device key.
    /// That key is untouched, so the old record still works and putting it back is what "nothing
    /// changed" means for a call that reports failure.
    ///
    /// Not every path needs this. `forgetDevice` and `destroyLocalData` only ever remove, so a lock
    /// landing inside one leaves less behind rather than more, and `changePassword` is documented
    /// to work on a locked instance and publishes no key at all.
    /// Withdraws a device record written while a lock was landing, WITHOUT failing the call.
    ///
    /// The other half of `requireDeviceRecordStillWanted`, for the three paths that commit
    /// something else first. rememberNow snapshots the data key it is handed and `lock()` zeroes
    /// that same array in place, so a store that prompts -- a passkey, a keystore with user
    /// verification -- can come back to find it wrapping zeroes. It then unwraps zeroes for its
    /// own read-back check, agrees with itself, and writes a device record of the right key
    /// VERSION holding nothing: `getPolicy()` reports the device is remembered and every later
    /// `unlockRemembered()` fails metadata authentication with nothing to say why.
    ///
    /// Withdrawing rather than throwing, because the rotation or the import beside it is already
    /// committed and stays reported as done; what is discarded is a wrap that cannot open
    /// anything. The degradation is observable -- the record is gone, so `getPolicy()` answers
    /// SESSION_ONLY, which is the state the device is really in.
    ///
    /// Named rather than written out at each site, because it was written out at ONE of the three
    /// and the other two were reported separately.
    private void withdrawDeviceRecordIfLocked(int generation) {
        if (generation == lockGeneration()) {
            return;
        }
        discardDeviceRecord();
    }

    /// Removes the device record, and destroys its mechanism when the record will not go.
    ///
    /// Shared by the two paths that discard a wrap they can no longer vouch for: a lock landing
    /// inside rememberNow's prompt, and a re-wrap that threw after writing the record. Both are
    /// beside an operation that is already committed and stays reported as done, so neither can
    /// refuse -- which is exactly why the delete has to be checked rather than assumed.
    private void discardDeviceRecord() {
        try {
            Storage.getInstance().deleteStorageFile(deviceRecordKey());
            if (definitelyGone(deviceRecordKey())) {
                return;
            }
        } catch (RuntimeException unreadableStore) {
            // Cleanup is post-commit. A thrown delete or lookup needs the same mechanism
            // fallback as a silently refused delete, rather than failing the committed change.
            Log.p("Vault: device-record cleanup could not be confirmed", Log.WARNING);
        }
        // The delete is void-returning and both real ports can drop one silently, so this used to
        // report the operation as successfully degraded to session-only over a record that was
        // still there -- and that record wraps the ZEROED key, so getPolicy() answers a
        // remembering policy and every later unlockRemembered() dies on metadata authentication
        // with nothing to say why.
        //
        // The mechanism goes instead of the caller being told. The enrolment, import or rotation
        // beside this is committed and stays reported as done -- that is the whole reason this
        // withdraws rather than throws -- so what is left to do is make the surviving record
        // unable to open anything, which destroying its key does.
        try {
            forgetEveryMechanism();
        } catch (RuntimeException alsoFailed) {
            // Nothing here can reach a half-made key, and the record is already unusable in
            // practice: it wraps zeroes. Logged rather than raised, because raising it would
            // fail an operation that did happen.
            Log.p("Vault: a device record written under a lock could not be withdrawn and its "
                    + "mechanism could not be destroyed either", Log.WARNING);
        }
    }

    private void requireDeviceRecordStillWanted(int generation, String restore) {
        if (generation == lockGeneration()) {
            return;
        }
        Storage storage = Storage.getInstance();
        if (restore != null) {
            requireDeviceRecordRestored(deviceRecordKey(), restore, null);
            throw new VaultException(VaultError.LOCKED,
                    "the vault was locked while this device was being remembered; the remembered "
                    + "unlock that was already here has been put back unchanged");
        }
        storage.deleteStorageFile(deviceRecordKey());
        if (!definitelyGone(deviceRecordKey())) {
            // The delete is void-returning and both real ports can drop one silently, so this
            // message -- "nothing that can reopen it without a password was left behind" -- was a
            // claim nothing had checked. A record that survives here is a fresh passwordless
            // unlock for a vault the application has just closed, which is the exact outcome the
            // generation check exists to prevent, so the mechanism behind it goes instead: a
            // record whose device key is gone cannot open anything.
            try {
                forgetEveryMechanism();
            } catch (RuntimeException alsoFailed) {
                // Reporting this would name the wrong failure. The refusal below is the answer
                // either way, and it does not promise the record is gone.
            }
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "the vault was locked while this device was being remembered, and the record "
                    + "written for it could not be removed; its device key has been destroyed so "
                    + "that it cannot reopen the vault, but the record itself is still here");
        }
        throw new VaultException(VaultError.LOCKED,
                "the vault was locked while this device was being remembered; nothing that can "
                + "reopen it without a password was left behind");
    }

    /// Refuses when the DATA KEY has been replaced since `at`.
    ///
    /// Not the same question as the lock generation, and that is the whole of the defect this
    /// answers: adoptKey zeroes the outgoing array in place and bumps this counter, while
    /// lockGeneration does not move -- a rotation, or an unlock of an already-unlocked vault,
    /// leaves the vault open. So a worker that snapshotted dataKey and then waited on a cipher or
    /// a user-verification prompt came back holding a buffer of zeroes, encrypted under it, and
    /// reported success. rememberNow's own read-back agreed, because unwrapping zeroes with
    /// zeroes round-trips. What it wrote was a secret, a database key or a remembered record that
    /// nothing can read afterwards.
    private void requireSameKey(int at) {
        if (at != keyGeneration()) {
            throw new VaultException(VaultError.CONFLICT,
                    "this vault's data key was replaced while the operation was running, so what "
                    + "it produced was not sealed under the key this vault now has");
        }
    }

    /// Checks and result publication share lock() and adoptKey()'s monitor. Crypto, storage,
    /// device prompts and completion callbacks run outside it. A refused result belongs to us.
    private <T> void completeUnlocked(AsyncResource<T> out, int generation, int keyAt, T result) {
        completeUnlocked(out, generation, keyAt, result, null);
    }

    private <T> void completeUnlocked(AsyncResource<T> out, final int generation,
            final int keyAt, final T result, final Runnable update) {
        out.complete(result, this, new Runnable() {
            @Override
            public void run() {
                try {
                    requireSameGeneration(generation);
                    requireSameKey(keyAt);
                } catch (VaultException failed) {
                    if (result instanceof byte[]) {
                        Bytes.zero((byte[]) result);
                    } else if (result instanceof char[]) {
                        Bytes.zero((char[]) result);
                    } else if (result instanceof KeyHandle) {
                        ((KeyHandle) result).destroy();
                    }
                    throw failed;
                }
                if (update != null) {
                    update.run();
                }
            }
        });
    }

    private void requireSameGeneration(int generation) {
        if (generation != lockGeneration()) {
            throw new VaultException(VaultError.LOCKED,
                    "the vault was locked while this operation was running");
        }
    }

    private AssociatedData wrapBinding(VaultMetadata meta, String purpose) {
        return AssociatedData.of(application, meta.vaultId, DATA_KEY_RECORD,
                purpose + "." + meta.dataKeyVersion);
    }

    private AssociatedData binding(VaultMetadata meta, String record, String purpose) {
        return AssociatedData.of(application, meta.vaultId, record, purpose);
    }

    /// Refuses to report a destruction that did not remove everything.
    ///
    /// deleteStorageFile returns void, so the loop above cannot tell a delete that happened from
    /// one that did not -- and both real ports can fail one silently: JavaSE discards
    /// File.delete()'s boolean, and the browser catches and logs the IndexedDB error. Reporting
    /// TRUE there is the worst answer available: a metadata record that survived leaves a vault
    /// the password still opens, on a call whose entire contract is that it is gone. Checked
    /// rather than assumed, the same way removeSecret answers with exists() instead of with the
    /// fact that it asked.
    private void requireEverythingGone(Storage storage, String prefix,
            java.util.Vector deleted) {
        StringBuilder left = new StringBuilder();
        // Every name this call actually deleted, asked by entryState rather than by enumerating
        // again. A second listing is not evidence on the browser, where a failed enumeration
        // answers an EMPTY array: a secret whose deletion was silently refused, followed by a
        // listing that fails, read as "nothing left" and let this report success over ciphertext
        // still on disk. entryState answers UNKNOWN there instead, and UNKNOWN is not gone.
        for (int iter = 0; iter < deleted.size(); iter++) {
            if (!definitelyGone((String) deleted.elementAt(iter))) {
                left.append("a secret");
                break;
            }
        }
        // Re-enumerated, not the array the deletion loop walked. Another context can store a
        // secret after that snapshot was taken -- a second browser tab holding the same key --
        // and verifying against the snapshot would confirm the entries this call knew about
        // while the new one stayed on disk, readable by that tab, under a report that everything
        // local had been destroyed. What this cannot do is stop a write that lands after the
        // check; it closes the window between the snapshot and the verification, which is the
        // whole of the deletion, rather than an instant.
        // And a fresh listing as well, which catches an entry that APPEARED since the snapshot
        // -- a second tab storing a secret while this ran. It is an addition to the check above,
        // never a substitute: an empty array here can mean the enumeration failed.
        String[] now = storage.listEntries();
        if (now == null) {
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "this device's storage could not be enumerated to confirm the deletion");
        }
        for (String entry : now) {
            // definitelyGone and not exists: a port that cannot tell answers false to exists(),
            // and false here meant "this secret went" -- the one claim destroyLocalData must not
            // make without evidence.
            if (entry != null && entry.startsWith(prefix) && !definitelyGone(entry)) {
                left.append(left.length() == 0 ? "" : ", ").append("a secret");
                break;
            }
        }
        if (!definitelyGone(deviceRecordKey())) {
            left.append(left.length() == 0 ? "" : ", ").append("the device wrap");
        }
        if (!definitelyGone(metadataKey())) {
            left.append(left.length() == 0 ? "" : ", ").append("the vault record");
        }
        if (left.length() > 0) {
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "this device's storage did not remove everything it was asked to; "
                    + left + " is still here");
        }
    }

    /// Whether the record this call wrote is still exactly as it wrote it and nothing has been
    /// stored under it.
    ///
    /// Two questions, because a session that adopted this vault leaves two different traces. It
    /// may have changed the record -- a password change, a rotation, a recovery code, or its own
    /// enrolment winning a race -- which the byte comparison catches. Or it may have left the
    /// record alone and put secrets under it, which only the storage listing catches.
    ///
    /// What this cannot see is a session that called [#seal] and kept the ciphertext in the
    /// application's own storage. Those bytes are bound to this record's vault id, so they would
    /// be dead after the record is removed. It is a real residue and a much narrower one than
    /// deleting unconditionally, and a caller that cannot tolerate it should enrol before handing
    /// the vault to other sessions.
    private boolean vaultIsStillUntouched(VaultMetadata written) {
        Storage storage = Storage.getInstance();
        VaultMetadata current = loadMetadataFresh();
        if (current == null || !current.serialize().equals(written.serialize())) {
            return false;
        }
        String[] entries = storage.listEntries();
        if (entries == null) {
            // Cannot establish that it is untouched, and the direction to err in is keeping a
            // record that protects something rather than deleting one that protects nothing.
            return false;
        }
        String prefix = secretKey("");
        boolean sawOurOwnRecord = false;
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            if (entry.startsWith(prefix)) {
                return false;
            }
            if (entry.equals(metadataKey())) {
                sawOurOwnRecord = true;
            }
        }
        // The listing has to PROVE itself before its silence counts as absence. Null is JavaSE
        // saying it failed; the browser does not say so at all -- HTML5Implementation catches
        // the IndexedDB error and answers an EMPTY array, which reads exactly like a store that
        // holds nothing. A tab storing a secret while a remembered-policy enrolment waited on
        // its prompt, and an enumeration that then failed alongside that prompt, made this
        // answer "untouched" and the rollback deleted the metadata protecting that ciphertext.
        //
        // This vault's own record is on disk right now -- the comparison above just read it --
        // so a listing that does not contain it is contradicting something already established
        // and is not evidence of anything.
        return sawOurOwnRecord;
    }

    /// One storage entry, read past the cache.
    ///
    /// Storage.readObject answers its process-local cache first and nothing another process
    /// writes can invalidate it, so "fresh" through that method was not fresh at all -- every
    /// freshness check in this class was comparing against whatever THIS tab had read before.
    /// That is the whole mechanism those checks exist to catch: another tab rotates the shared
    /// vault, this tab's commitMetadata compares its incoming record against a cached
    /// predecessor, agrees with itself, and writes over the rotation.
    ///
    /// Composed from the public stream primitives rather than by clearing the cache, because
    /// clearCache() is all-or-nothing: it would evict everything the APPLICATION has cached, on
    /// every vault mutation, to answer a question about one entry.
    /// Whether one entry is PROVEN gone, which is not the same as `!exists(name)`.
    ///
    /// Every "did the delete happen" check in this class asked exists() and read false as yes. A
    /// port that cannot tell answers false, so a storage failure was reported as a successful
    /// removal -- a secret, a device record or a whole vault reported destroyed while it was
    /// still there. UNKNOWN is grouped with PRESENT at every one of those sites, because only a
    /// definite ABSENT is evidence that something went.
    private static boolean definitelyGone(String name) {
        return Storage.getInstance().entryState(name)
                == com.codename1.impl.CodenameOneImplementation.STORAGE_ENTRY_ABSENT;
    }

    /// The largest sync state this will look at, before it looks at any of it.
    ///
    /// Sync state arrives from wherever the application syncs, which is a server it does not
    /// control the health of. Everything inside the record is size-checked -- SecureEnvelope has
    /// MAX_CIPHERTEXT, MAX_SALT and MAX_KEY_ID, VaultMetadata has MAX_ANCESTORS -- but the record
    /// ITSELF was not, and none of those limits apply until after it has been decoded and parsed.
    /// The decode alone is the amplification: bytes become a UTF-16 String at twice the size,
    /// which is then cut into per-line substrings, appended into StringBuilders and hex-decoded
    /// back into arrays, all of it retained until parse returns and none of it authenticated by
    /// anything. A response of a few hundred megabytes is an out-of-memory in an application that
    /// merely asked to sync.
    ///
    /// Four megabytes rather than a tighter figure because a legitimate record can be large: the
    /// retired chain carries one envelope per rotation this vault has EVER done, at roughly 220
    /// characters each, so this still admits something like eighteen thousand rotations -- a
    /// daily rotation for fifty years. It is a bound on the absurd, not a quota.
    private static final int MAX_SYNC_STATE = 4 * 1024 * 1024;

    /// Puts one secret entry back the way it was, and refuses to claim it did when it did not.
    ///
    /// Both lock-race rollbacks used to write or delete and then report LOCKED regardless.
    /// writeObject returns a boolean nobody read and deleteStorageFile returns void, so a storage
    /// failure during the rollback left the caller told "the vault was locked, nothing changed"
    /// over a secret that had in fact been overwritten, created or deleted. LOCKED is a promise
    /// that nothing changed; when it cannot be kept the answer is the storage failure, which is
    /// the one the caller can do something about.
    /// `ours` is what this operation left in the entry -- the ciphertext it wrote, or null when
    /// it deleted. The restore happens ONLY while that is still what is stored.
    ///
    /// Restoring unconditionally was itself a lost update. Another tab completing putSecret for
    /// the same name between this operation's write and its post-write lock check has committed
    /// a NEWER value, and writing `previous` over it discards a call that reported success --
    /// while this one reports LOCKED, whose whole meaning is that nothing changed. Finding
    /// something other than `ours` means this operation is no longer the last writer, and the
    /// state it was going to restore is not the state to restore to.
    ///
    /// The same reasoning covers the delete: if another tab created an entry after this one
    /// removed it, putting `previous` back would bury that too.
    private void requireSecretRestored(String entry, Object previous, String ours) {
        // `ours == null` is the DELETE path, where "still ours" means still absent -- and that
        // is the one case this cannot establish, which a review has raised and which is worth
        // recording rather than re-deriving. An absent entry is equally what ANOTHER tab's
        // successful removeSecret looks like, so restoring can resurrect a secret somebody else
        // was told had gone.
        //
        // It still restores, deliberately. The alternative loses the contract lock() actually
        // has and that aVaultLockedWhileASecretIsRemovedKeepsTheSecret pins down: a removal
        // interrupted by a lock must not take the secret with it, because reporting LOCKED over
        // ciphertext that is irreversibly gone tells the caller nothing changed when everything
        // did. That is the single-instance case, which is every non-browser port and most
        // browser sessions; the resurrection needs TWO unlocked tabs removing the same secret
        // concurrently with a lock landing inside one of them.
        //
        // Closing it properly means a per-operation tombstone -- a change to what a secret entry
        // IS, so that "absent because I deleted it" and "absent because you did" stop looking
        // alike -- rather than anything this rollback can decide. That is a deliberate trade,
        // not an oversight.
        Object current = readUncached(entry);
        boolean stillOurs = ours == null
                ? !(current instanceof String)
                : (current instanceof String) && ours.equals(current);
        if (!stillOurs) {
            // Somebody else is the last writer. Leaving their value alone IS the rollback: this
            // operation's own effect is already gone.
            return;
        }
        if (previous instanceof String) {
            if (!Storage.getInstance().writeObject(entry, previous)) {
                throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                        "the vault was locked while this secret was being changed, and the value "
                        + "that was there could not be put back");
            }
            return;
        }
        Storage.getInstance().deleteStorageFile(entry);
        if (!definitelyGone(entry)) {
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "the vault was locked while this secret was being changed, and the entry "
                    + "this call created could not be removed again");
        }
    }

    /// Undoes a failed policy change: the configured policy and the device record as they were.
    ///
    /// Extracted so it can run BEFORE the AsyncResource is completed. It used to live in a
    /// finally, which runs after out.error -- and an error callback is free to call setPolicy
    /// again. background() starts a new thread per call, so that retry ran CONCURRENTLY with this
    /// cleanup: the retry settled a new record, this then wrote the old one back over it, and the
    /// caller had already been told the retry succeeded. Completing has to mean the persisted
    /// state is settled, which means the last write happens first.
    ///
    /// Idempotent, because the finally still calls it as a backstop for any route out that is not
    /// one of the two catches.
    /// Answers whether the device record really is back as it was, so the caller can publish the
    /// truth. An earlier version only logged a refused restore, on the reasoning that this ran in
    /// a finally where the failure had already been delivered and nothing thrown could reach
    /// anyone. That reasoning went stale the moment the rollback was moved AHEAD of out.error:
    /// the catches now run it before publishing, so a failed restore is something they can and
    /// must report. Leaving it at a log meant a call that reported its original error had also
    /// left the INCOMING record active while resetting options to the old policy -- so the
    /// effective policy had changed, and a retry acted on the wrong remembered mechanism.
    private boolean undoPolicyChange(UnlockPolicy configuredBefore, Object savedRecord,
            boolean outgoingKeyGone, boolean[] alreadyUndone) {
        if (alreadyUndone[0]) {
            return true;
        }
        alreadyUndone[0] = true;
        options.policy(configuredBefore);
        if (savedRecord instanceof String && !outgoingKeyGone) {
            if (!Storage.getInstance().writeObject(deviceRecordKey(), (String) savedRecord)) {
                Log.p("Vault: the device record could not be restored after a failed policy "
                        + "change", Log.WARNING);
                return false;
            }
            return true;
        }
        // Not restored once the OUTGOING key has been deleted: on a port where the two policies
        // are different mechanisms -- a stored key and a passkey in the browser -- that key is
        // gone for good, and putting its record back leaves a remembered unlock that names a key
        // nothing holds. It would fail at the next launch rather than here. Session only is the
        // honest state, and the password still opens the vault.
        Storage.getInstance().deleteStorageFile(deviceRecordKey());
        // And that deletion is checked like every other, because a record surviving here is the
        // incoming one -- a remembered unlock naming a key this call has already destroyed.
        return definitelyGone(deviceRecordKey());
    }

    /// Puts the device record back, and refuses to claim it did when it did not.
    ///
    /// The device-record twin of requireSecretRestored, and it exists for the same reason: every
    /// one of these rollbacks wrote the old record and then reported a DIFFERENT error --
    /// LOCKED, or whatever made the policy change fail -- on the strength of a boolean nobody
    /// read. A storage refusal there leaves the record this call had just written, which on the
    /// lock-race path can be a wrap of the zeroed key, while the caller is told the remembered
    /// unlock it already had was put back unchanged. It was not, and it no longer works.
    ///
    /// The original failure goes on as the cause, because "the policy change failed" and "and it
    /// could not be undone" are both things the caller needs, and only the second changes what
    /// they can do next.
    private static void requireDeviceRecordRestored(String key, String saved, Throwable because) {
        if (!Storage.getInstance().writeObject(key, saved)) {
            throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                    "the device record this call replaced could not be put back, so this device's "
                    + "remembered unlock is no longer the one it had", because);
        }
    }

    /// Refuses a sync state too large to be one, before anything decodes it.
    private static void requirePlausibleSyncState(byte[] state) {
        if (state != null && state.length > MAX_SYNC_STATE) {
            throw new VaultException(VaultError.CORRUPT,
                    "this sync state is larger than any vault record can be and was not read");
        }
    }

    private Object readUncached(String name) {
        // entryState and not exists(), because exists() answers a boolean and a port that cannot
        // tell has to pick one -- and every port that catches picks false. On the browser a
        // transient IndexedDB refusal therefore reported this vault's metadata record as ABSENT,
        // which every caller here reads as "this device is not enrolled": enroll() then writes
        // fresh metadata under a NEW data key over a vault whose secrets were all sealed under
        // the old one, and nothing in the process ever reports a failure. The read below already
        // distinguishes the two; the existence gate in front of it did not, and short-circuited
        // before it could.
        int state = Storage.getInstance().entryState(name);
        if (state == com.codename1.impl.CodenameOneImplementation.STORAGE_ENTRY_UNKNOWN) {
            throw new VaultException(VaultError.TEMPORARILY_UNREADABLE,
                    "this device's storage could not say whether the vault record is there");
        }
        if (state == com.codename1.impl.CodenameOneImplementation.STORAGE_ENTRY_ABSENT) {
            return null;
        }
        java.io.InputStream in = null;
        try {
            in = Storage.getInstance().createInputStream(name);
            return com.codename1.io.Util.readObject(new java.io.DataInputStream(in));
        } catch (java.io.IOException cannotRead) {
            throw new VaultException(VaultError.TEMPORARILY_UNREADABLE,
                    "the vault record could not be read from storage", cannotRead);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                    // Nothing left to do with it; the value was already read or the read failed.
                }
            }
        }
    }

    private String metadataKey() {
        return STORAGE_PREFIX + application + "." + safeName();
    }

    /// The vault name with everything that could be confused with a separator escaped.
    ///
    /// Without this, a vault called `notes.device` writes its record to the key a vault called
    /// `notes` writes its **device record** to -- one vault silently overwriting another's device
    /// wrap, which is a lost vault rather than a collision anyone would notice. The escape is
    /// reversible, so two names that differ keep different keys; folding to a single replacement
    /// character would reintroduce the collision a step further along.
    private String safeName() {
        // '.' is deliberately NOT in the safe set here: it separates a metadata key from the
        // device key built on it, which is the collision this method exists to prevent. Nor is
        // '_', which is now the escape character.
        return escaped(name, "-");
    }

    /// The escape every storage name here is built from: anything outside the safe set becomes
    /// `_` and four hex digits, and `_` is itself outside every safe set, so `a_0020b` and `a b`
    /// cannot encode alike. Reversible, which is the whole point -- two values that differ must
    /// keep different keys, and folding to one replacement character is how they stop doing that.
    ///
    /// The escape character is `_` and NOT `%`, which is what this used at first and which does
    /// not survive the trip. `Storage.fixFileName` rewrites `/ \\ % ? * : =` to `_` whenever
    /// `normalizeNames` is on, and it is on by default -- so a vault called `a.b` escaped to
    /// `a%002eb` and then PERSISTED as `a_002eb`, which is exactly the spelling a vault literally
    /// called `a_002eb` persisted under. The escape was reversible everywhere except the one
    /// place it had to be, and `destroyLocalData` compounded it by matching an unnormalized
    /// prefix against the normalized names `listEntries` returns, so it walked past the entries
    /// it was asked to delete. `_` is not in that rewrite list.
    ///
    /// The safe set is a parameter because the callers genuinely differ: a vault name must escape
    /// `.`, an application identity must keep it, since that is what package names are made of.
    /// None of them may include `_`.
    private static String escaped(String value, String alsoSafe) {
        StringBuilder b = new StringBuilder(value.length());
        for (int iter = 0; iter < value.length(); iter++) {
            char c = value.charAt(iter);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || alsoSafe.indexOf(c) >= 0;
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

    private static final String HEX = "0123456789abcdef";

    private String deviceRecordKey() {
        return metadataKey() + ".device";
    }

    private String secretKey(String secretName) {
        // Escaped, not embedded. Storage.fixFileName rewrites '/' and six other characters to
        // '_', so `api/token` and `api_token` addressed ONE stored object: writing the second
        // overwrote the first, whose own associated data then failed to authenticate, and
        // removing either removed both.
        return metadataKey() + ".s." + escaped(secretName == null ? "" : secretName, "-");
    }

    private String deviceKeyId() {
        return application + ":" + safeName();
    }

    /// Decoded straight into the array the caller gets, with no String in between.
    ///
    /// Every getSecret answer comes through here. Going via Bytes.fromUtf8 put the whole secret
    /// into an immutable String -- and into the StringBuilder behind it -- neither of which the
    /// finally below, or the caller's own wipe of the returned array, can reach. The plaintext
    /// stayed findable in a heap dump after the vault was locked or destroyed.
    private static char[] chars(byte[] utf8) {
        return Bytes.charsFromUtf8(utf8, 0, utf8.length);
    }

    /// Runs work off the EDT so a caller can `get()` the result from the EDT without freezing it.
    ///
    /// Already off the EDT, the work runs inline: spawning a thread to do what the caller's thread
    /// is free to do would only add a context switch, and on the JavaScript port a thread is a
    /// coroutine whose scheduling is not free.
    private static void background(Runnable work) {
        if (com.codename1.ui.CN.isEdt()) {
            Display.getInstance().startThread(work, "cn1-vault").start();
        } else {
            work.run();
        }
    }

    /// The same wait, typed, so the erased narrowing does not land in the caller.
    ///
    /// `await` is generic, so javac puts a CHECKCAST at every call site. ParparVM does not throw
    /// for a failed cast, and the terminal `catch (RuntimeException)` these workers now carry is
    /// a supertype of ClassCastException -- so the cast-semantics gate reads that pair as a
    /// handler that cannot run on iOS, and it is right to. Keeping the narrowing in a method with
    /// no handler at all removes the question rather than baselining it.
    private static byte[] awaitBytes(AsyncResource<byte[]> resource, String message) {
        return await(resource, message);
    }

    /// A storage value narrowed to a String without relying on a cast to raise anything.
    ///
    /// Returns null when it is not one, for the same reason: on ParparVM a failed cast hands the
    /// next instruction the wrong object rather than throwing, so the test has to be the
    /// instanceof and not the catch.
    private static String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    /// Waits for an SPI resource on the current thread, translating whatever it failed with.
    private static <T> T await(AsyncResource<T> resource, String message) {
        try {
            return resource.get();
        } catch (RuntimeException failed) {
            throw asVaultException(failed, message);
        }
    }

    private static VaultException asVaultException(Throwable error, String message) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof VaultException) {
                return (VaultException) cause;
            }
            cause = cause.getCause();
        }
        return new VaultException(VaultError.UNKNOWN, message, error);
    }

    /// A stable identity for this application, so two applications on a desktop -- which share a
    /// storage directory and a key store -- cannot reach each other's vaults.
    private static String applicationIdentity() {
        String id = null;
        try {
            id = Display.getInstance().getProperty("package_name", null);
            if (id == null || id.length() == 0) {
                id = Display.getInstance().getProperty("AppName", null);
            }
        } catch (RuntimeException tooEarly) {
            id = null;
        }
        if (id == null || id.length() == 0) {
            return "cn1app";
        }
        // The same reversible escape safeName() uses, rather than folding every unsupported
        // character to a single '_'. That folding made "foo$bar" and "foo_bar" -- both
        // reachable, as a package component and as a fallback AppName -- encode alike, and two
        // applications that collide here share metadata keys and device-key ids: one sees
        // CONFLICT against, overwrites, or deletes the other's same-named vault.
        return escaped(id, ".-");
    }

    /// The device-local half of a vault: which policy this device enrolled under and the wrap the
    /// port produced. Never synced.
    private static final class DeviceRecord {
        final UnlockPolicy policy;
        final byte[] wrap;
        /// The data key version this wrap contains. See [Vault#unlockRemembered()] for why a
        /// record that does not carry one is refused rather than trusted.
        final int keyVersion;

        DeviceRecord(UnlockPolicy policy, byte[] wrap, int keyVersion) {
            this.policy = policy;
            this.wrap = wrap;
            this.keyVersion = keyVersion;
        }

        String serialize() {
            return "CN1VAULTDEV1\npolicy=" + policy.name() + "\nversion=" + keyVersion
                    + "\nwrap=" + Bytes.toHex(wrap) + "\n";
        }

        static DeviceRecord parse(String text) {
            if (!text.startsWith("CN1VAULTDEV1")) {
                return null;
            }
            UnlockPolicy policy = UnlockPolicy.REMEMBER_DEVICE;
            byte[] wrap = null;
            int keyVersion = 0;
            int at = text.indexOf('\n') + 1;
            while (at > 0 && at < text.length()) {
                int end = text.indexOf('\n', at);
                if (end < 0) {
                    end = text.length();
                }
                String line = text.substring(at, end);
                at = end + 1;
                if (line.startsWith("policy=")) {
                    String value = line.substring(7);
                    if (UnlockPolicy.REQUIRE_USER_VERIFICATION.name().equals(value)) {
                        policy = UnlockPolicy.REQUIRE_USER_VERIFICATION;
                    } else if (UnlockPolicy.SESSION_ONLY.name().equals(value)) {
                        policy = UnlockPolicy.SESSION_ONLY;
                    }
                } else if (line.startsWith("version=")) {
                    try {
                        keyVersion = Integer.parseInt(line.substring(8));
                    } catch (NumberFormatException malformed) {
                        keyVersion = 0;
                    }
                } else if (line.startsWith("wrap=")) {
                    wrap = Bytes.fromHex(line.substring(5));
                }
            }
            return wrap == null ? null : new DeviceRecord(policy, wrap, keyVersion);
        }
    }
}
