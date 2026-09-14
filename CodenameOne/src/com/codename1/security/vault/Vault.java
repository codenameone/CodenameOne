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

import com.codename1.io.Storage;
import com.codename1.security.Base32;
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
/// } else if (!vault.unlockRemembered().isDone()) {
///     vault.unlockWithPassword(password).get();
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
/// A vault instance is not safe to drive from two threads at once. The state it holds is one
/// unlocked data key; the operations that change it are not designed to interleave, and racing
/// an unlock against a lock is a bug in the caller rather than something to lock against. Ports
/// where a single application runs in more than one process are handled where it matters -- the
/// device key's create-if-absent converges (see [DeviceProtection#ensureKey]) -- and the metadata
/// record carries a counter so a caller can detect that another writer moved it.
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
            options = newOptions;
        }
        return this;
    }

    // ------------------------------------------------------- capabilities

    /// What this device can actually provide, per unlock policy.
    ///
    /// Ask before offering the user a choice: a "remember this device" switch on a platform that
    /// cannot remember is worse than no switch.
    public VaultCapabilities capabilities() {
        // The BASE mechanism, not the one the current policy happens to use. deviceProtection()
        // answers the gated variant while this vault is enrolled REQUIRE_USER_VERIFICATION, so
        // seeding capabilities with it made protectionFor(REMEMBER_DEVICE) describe the passkey
        // rather than the store that policy would actually use -- in the browser, reporting
        // NON_EXTRACTABLE_KEY=NO from the passkey where the device key reports YES. A capability
        // query is about what each policy WOULD provide, and protectionFor selects the gated
        // variant itself for the one policy that needs it.
        return new VaultCapabilities(baseDeviceProtection());
    }

    /// What currently protects this vault on this device, as observed.
    ///
    /// This describes the state that exists, not the state that could: a vault enrolled
    /// [UnlockPolicy#SESSION_ONLY] reports no device key because there is none, on the same
    /// platform where [#capabilities()] says one is available.
    public ProtectionReport protection() {
        DeviceProtection device = deviceProtection();
        ProtectionReport.Builder b = ProtectionReport.builder();
        boolean enrolled = state() != NOT_ENROLLED;
        b.set(Protection.PERSISTENT, enrolled);
        if (deviceRecord() == null) {
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
                    enrolled ? deviceReport.answer(Protection.ENCRYPTED_AT_REST)
                            : ProtectionReport.NO);
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
        return state() == LOCKED || state() == UNLOCKED;
    }

    /// Whether the data key is currently available.
    public boolean isUnlocked() {
        checkAutoLock();
        return dataKey != null;
    }

    /// One of [#NOT_ENROLLED], [#LOCKED], [#UNLOCKED] or [#STATE_UNKNOWN].
    public int state() {
        checkAutoLock();
        if (dataKey != null) {
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
    public boolean passwordNeedsRewrap() {
        return passwordNeedsRewrap;
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
            options = opts;
        }
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration;
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
            metadata = verified;
            adoptKey(key);
            key = null;
            touch();
            if (options.getPolicy() != UnlockPolicy.SESSION_ONLY) {
                try {
                    rememberNow(options.getPolicy());
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
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    VaultMetadata meta = requireMetadata();
                    if (meta.passwordWrap == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "this vault has no password wrap");
                    }
                    SecureEnvelope envelope = SecureEnvelope.parse(meta.passwordWrap);
                    byte[] key = envelope.openWithPassword(password,
                            wrapBinding(meta, PURPOSE_PASSWORD));
                    if (generation != lockGeneration) {
                        Bytes.zero(key);
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while it was being unlocked");
                    }
                    requireAuthenticRecord(meta, key);
                    // Checked here too, not only at enrolment. A vault enrolled before the
                    // application started asking for anything reopened without ever meeting the
                    // requirements it was later configured with -- so require(...) governed the
                    // first launch and nothing afterwards, which is the opposite of what a
                    // requirement is for.
                    requireProtections();
                    publishKey(generation, meta, key);
                    passwordNeedsRewrap = envelope.getKdf().needsUpgrade();
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
        final int generation = lockGeneration;
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
                        Storage.getInstance().deleteStorageFile(deviceRecordKey());
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
                    DeviceProtection unlocking = deviceProtection();
                    unlocking.setDeviceBoundRequired(options.isDeviceBoundPasskeyRequired());
                    if (options.isDeviceBoundPasskeyRequired()) {
                        await(unlocking.ensureKey(deviceKeyId()),
                                "the remembered credential does not satisfy this vault's "
                                + "device-bound requirement");
                    }
                    key = awaitBytes(unlocking.unwrap(deviceKeyId(), record.wrap,
                                    wrapBinding(meta, PURPOSE_DEVICE).serialize()),
                            "the remembered device key could not be used");
                    if (generation != lockGeneration) {
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
        final int generation = lockGeneration;
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
                    rememberNow(options.getPolicy());
                    requireDeviceRecordStillWanted(generation);
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
        // Snapshotted, not re-read, for the reason rotateDataKey gives: the store below prompts,
        // so this method runs for as long as the user takes, and lock() nulls both of these. Read
        // afterwards they are null and the worker dies on a NullPointerException, which the
        // catch-all reports as "this vault operation could not complete" -- a lock described as an
        // unknown fault. With the snapshot the write finishes and the caller is told it was LOCKED,
        // by the generation check the two public entry points make after this returns.
        VaultMetadata meta = metadata;
        byte[] key = dataKey;
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
        byte[] aad = wrapBinding(meta, PURPOSE_DEVICE).serialize();
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
        writeDeviceRecord(new DeviceRecord(policy, wrapped, meta.dataKeyVersion));
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
                    Storage.getInstance().deleteStorageFile(deviceRecordKey());
                    forgetEveryMechanism();
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

    /// Locks the vault: the data key is overwritten and dropped, and every handle this vault
    /// issued stops working.
    ///
    /// An operation already in flight when this runs does not deliver its result -- it fails with
    /// [VaultError#LOCKED] instead. What locking cannot do is reach a plaintext or a key already
    /// handed to a caller, including to hostile code that got one while the vault was open.
    public void lock() {
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
                    if (entries == null) {
                        throw new VaultException(VaultError.STORAGE_UNAVAILABLE,
                                "this device's storage cannot be enumerated, so the secrets in "
                                + "this vault cannot be found to delete; nothing was removed");
                    }
                    String prefix = secretKey("");
                    try {
                        for (String entry : entries) {
                            if (entry != null && entry.startsWith(prefix)) {
                                storage.deleteStorageFile(entry);
                            }
                        }
                        storage.deleteStorageFile(deviceRecordKey());
                        storage.deleteStorageFile(metadataKey());
                        forgetEveryMechanism();
                        requireEverythingGone(storage, entries, prefix);
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
        final int generation = lockGeneration;
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
                    byte[] key = dataKey;
                    VaultMetadata meta = metadata;
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
                    String entry = secretKey(secretName);
                    // Kept so the write can be undone. The check above is before a storage write
                    // and a lock can land inside one, and this call REPLACES whatever was stored
                    // under the name -- so refusing without putting the old value back would
                    // discard a secret the caller still believes is there, on an operation that
                    // reported failure.
                    Object previous = Storage.getInstance().readObject(entry);
                    if (!Storage.getInstance().writeObject(entry, Bytes.toHex(sealed))) {
                        throw new VaultException(VaultError.QUOTA_EXCEEDED,
                                "the secret could not be written to storage");
                    }
                    if (generation != lockGeneration) {
                        // Asked again after the write, like createRecoveryCode. Completing here
                        // would let a screen that was already stale when the user touched it
                        // change what the vault holds after lock() had returned.
                        if (previous instanceof String) {
                            Storage.getInstance().writeObject(entry, previous);
                        } else {
                            Storage.getInstance().deleteStorageFile(entry);
                        }
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this secret was being stored");
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
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                byte[] plain = null;
                try {
                    requireUnlocked();
                    String stored = asString(Storage.getInstance().readObject(
                            secretKey(secretName)));
                    if (stored == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "no secret is stored under that name");
                    }
                    byte[] sealed = Bytes.fromHex(stored);
                    if (sealed == null) {
                        throw new VaultException(VaultError.CORRUPT,
                                "the stored secret is not in a format this build wrote");
                    }
                    plain = openAnyVersion(sealed, binding(metadata, secretName, PURPOSE_SECRET));
                    requireSameGeneration(generation);
                    out.complete(chars(plain));
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
        final int generation = lockGeneration;
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
                    Object previous = Storage.getInstance().readObject(entry);
                    Storage.getInstance().deleteStorageFile(entry);
                    if (generation != lockGeneration) {
                        if (previous instanceof String) {
                            Storage.getInstance().writeObject(entry, previous);
                        }
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this secret was being removed");
                    }
                    out.complete(Boolean.valueOf(!Storage.getInstance().exists(entry)));
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
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    requireUnlocked();
                    // Snapshotted for the same reason as putSecret: lock() nulls metadata, and
                    // dereferencing it here after requireUnlocked has passed is a
                    // NullPointerException rather than a refusal.
                    byte[] key = dataKey;
                    VaultMetadata meta = metadata;
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
                    out.complete(sealed);
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
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    requireUnlocked();
                    byte[] plain = openAnyVersion(sealed,
                            binding(metadata, recordId, PURPOSE_RECORD));
                    if (generation != lockGeneration) {
                        Bytes.zero(plain);
                        requireSameGeneration(generation);
                    }
                    out.complete(plain);
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
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    requireUnlocked();
                    byte[] source = dataKey;
                    VaultMetadata meta = metadata;
                    requireSameGeneration(generation);
                    if (source == null || meta == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this key was being derived");
                    }
                    byte[] derived = deriveSubkey(source, purpose);
                    if (generation != lockGeneration) {
                        Bytes.zero(derived);
                        requireSameGeneration(generation);
                    }
                    // extractedKeyProtection(), not the device report. The device key may
                    // well be non-extractable -- in the browser it is -- but what this handle
                    // carries is a derived subkey sitting in a Java byte array, which its own
                    // isExportable() correctly reports as exportable. Handing back a report
                    // saying NON_EXTRACTABLE_KEY while the object contradicts it is the kind
                    // of guarantee that gets believed.
                    // `generation`, not lockGeneration. Reading the field again here meant a
                    // lock landing between the check above and this line stamped the handle
                    // with the POST-lock value -- so the handle considered itself live and went
                    // on sealing and opening with key material the lock had invalidated.
                    out.complete(new VaultKeyHandle(Vault.this, generation, keyGeneration,
                            derived,
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
        final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    if (options.isOpaqueKeysOnly()) {
                        throw new VaultException(VaultError.POLICY_NOT_MET,
                                "this vault was configured to produce no raw key material, and an "
                                + "encrypted database cannot be keyed without it",
                                Protection.NON_EXTRACTABLE_KEY, null);
                    }
                    requireUnlocked();
                    byte[] source = dataKey;
                    requireSameGeneration(generation);
                    if (source == null) {
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while this key was being derived");
                    }
                    Hmac mac = Hmac.create(Hash.SHA256, source);
                    mac.update(Bytes.utf8("cn1.vault.dbkey.v1"));
                    mac.update(Bytes.utf8(alias == null ? "" : alias));
                    byte[] key = mac.doFinal();
                    if (generation != lockGeneration) {
                        Bytes.zero(key);
                        requireSameGeneration(generation);
                    }
                    out.complete(key);
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
                    metadata = next;
                    passwordNeedsRewrap = false;
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
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                try {
                    requireUnlocked();
                    byte[] raw = SecureRandom.bytes(20);
                    char[] code = Base32.encode(raw).toCharArray();
                    Bytes.zero(raw);
                    byte[] derived = recoveryKey(code);
                    VaultMetadata next = metadata.copy();
                    next.recoveryWrap = SecureEnvelope.seal(derived, next.dataKeyId,
                            next.dataKeyVersion,
                            wrapBinding(next, PURPOSE_RECOVERY), dataKey);
                    Bytes.zero(derived);
                    next.counter = metadata.counter + 1;
                    // Checked before the write: a recovery code refused after persisting its
                    // wrap would be a code the vault accepts and the caller never received.
                    stampMac(next, metadata, dataKey);
                    requireSameGeneration(generation);
                    VaultMetadata previous = metadata;
                    commitMetadata(previous, next);
                    if (generation != lockGeneration) {
                        // Asked again, because the check above is before a storage write and a
                        // lock can land inside one. Completing here would hand back a working
                        // recovery credential after lock() had already returned.
                        //
                        // And simply refusing is not enough, for the reason the comment above
                        // gives: this call REPLACES any previous recovery wrap, so a refusal
                        // that left the new record standing would retire a code the user still
                        // holds in favour of one they were never given. The previous record goes
                        // back, so the vault is exactly as it was before this call.
                        Bytes.zero(code);
                        commitMetadata(next, previous);
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while a recovery code was being created");
                    }
                    metadata = next;
                    out.complete(code);
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

    /// Unlocks with a recovery code from [#createRecoveryCode()].
    public AsyncResource<Boolean> unlockWithRecoveryCode(final char[] code) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        // On the calling thread; see unlockWithPassword for why not in the worker.
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                byte[] derived = null;
                try {
                    VaultMetadata meta = requireMetadata();
                    if (meta.recoveryWrap == null) {
                        throw new VaultException(VaultError.KEY_MISSING,
                                "this vault has no recovery code");
                    }
                    derived = recoveryKey(code);
                    byte[] key = SecureEnvelope.parse(meta.recoveryWrap).open(derived,
                            wrapBinding(meta, PURPOSE_RECOVERY));
                    if (generation != lockGeneration) {
                        Bytes.zero(key);
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while it was being unlocked");
                    }
                    requireAuthenticRecord(meta, key);
                    requireProtections();
                    publishKey(generation, meta, key);
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
        // Rotation derives from the password twice -- once to prove it, once to wrap the new key
        // -- so at the default profile it holds the vault open for well over a second. A lock that
        // lands in there must not be undone by the publish at the end.
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                byte[] fresh = null;
                try {
                    requireUnlocked();
                    VaultMetadata meta = metadata;
                    // Snapshotted, not re-read. lock() sets dataKey to null, and the derivation
                    // below runs for seconds -- so comparing against the field afterwards compared
                    // against null and reported AUTHENTICATION_FAILED, telling the user their
                    // password was wrong when it was fine and the vault had simply been locked.
                    // Measured: the lock-race test produced exactly that before this snapshot.
                    byte[] currentKey = dataKey;
                    // Proven before anything changes: a rotation that leaves the password unable
                    // to unwrap the new key is a vault nobody can open on another device.
                    byte[] check = SecureEnvelope.parse(meta.passwordWrap).openWithPassword(
                            password, wrapBinding(meta, PURPOSE_PASSWORD));
                    if (generation != lockGeneration) {
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
                    if (generation != lockGeneration) {
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
                    if (generation != lockGeneration) {
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
                    adoptKey(fresh);
                    fresh = null;
                    metadata = next;
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
                        try {
                            rememberNow(remembered.policy);
                        } catch (RuntimeException rewrapFailed) {
                            Storage.getInstance().deleteStorageFile(deviceRecordKey());
                        }
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
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
                try {
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
                    byte[] key = SecureEnvelope.parse(incoming.passwordWrap).openWithPassword(
                            password, wrapBinding(incoming, PURPOSE_PASSWORD));
                    // Checked against the key this record itself describes, which is the only
                    // point at which the counter it claims can be believed.
                    try {
                        requireAuthenticRecord(incoming, key);
                        if (local != null) {
                            requireMetadataAncestry(local, incoming);
                            requireKeyContinuity(local, incoming, key);
                        }
                    } catch (RuntimeException refused) {
                        Bytes.zero(key);
                        throw refused;
                    }
                    if (generation != lockGeneration) {
                        // The record is still written -- enrolling this device is the point of the
                        // call and it succeeded. What is refused is leaving the vault unlocked
                        // afterwards, because the application asked for it to be locked.
                        commitMetadata(local, incoming);
                        Bytes.zero(key);
                        throw new VaultException(VaultError.LOCKED,
                                "the vault was locked while the sync state was being imported");
                    }
                    // The configured policy governs this enrolment too. Importing sync state is
                    // how a SECOND device joins a vault, so it is an enrolment entry point in
                    // everything but name -- and it went from authentication straight to commit,
                    // so a device configured REMEMBER_DEVICE or REQUIRE_USER_VERIFICATION, or
                    // with unmet require(...), reported success and ended up session-only.
                    requireProtections();
                    // Committed against the record that was read above, so a tab that wrote
                    // between the comparison and here loses rather than being overwritten.
                    commitMetadata(local, incoming);
                    // Through the same lock-aware handoff as every other publication: the check
                    // above is before the commit, and the commit is a storage write wide enough
                    // for a lock to land inside. publishKey withdraws the key and re-locks if it
                    // did, so an overlapping lock leaves the vault closed rather than reopened.
                    // No `key = null` here, unlike unlockRemembered: that method's finally
                    // zeroes the local, so it has to be released before publication. This one's
                    // finally touches only the password, so nulling it is a dead store -- which
                    // SpotBugs says outright, and it was copied from the other path by shape.
                    publishKey(generation, incoming, key);
                    // Established after the record is committed, because remembering wraps the
                    // key this record describes. A failure here is reported rather than
                    // swallowed: the device is enrolled, but not under the policy that was asked
                    // for, and the caller has to know that.
                    if (options.getPolicy() != UnlockPolicy.SESSION_ONLY) {
                        try {
                            rememberNow(options.getPolicy());
                        } catch (RuntimeException rememberFailed) {
                            // Rolled back to whatever was here BEFORE, which is not always
                            // nothing. The first version of this deleted the record outright, on
                            // the reasoning that a device which has just joined has sealed
                            // nothing of its own -- true for a first import, and false for the
                            // refresh of a device that already had a vault. There, deleting
                            // threw away the local password and recovery wraps and made every
                            // secret already on the device unreadable, on a call that reported
                            // failure.
                            if (local == null) {
                                // Guarded exactly as enrolment's rollback is, and for the same
                                // reason: the record has been readable by every session on this
                                // origin since it was committed, the step that failed is a
                                // prompt, and another tab can unlock the vault and store secrets
                                // inside that window. Deleting then takes the only password wrap
                                // with it -- and forgetting every mechanism would also destroy a
                                // remembered unlock that other tab had just established.
                                if (vaultIsStillUntouched(incoming)) {
                                    Storage.getInstance().deleteStorageFile(metadataKey());
                                    try {
                                        forgetEveryMechanism();
                                    } catch (RuntimeException alsoFailed) {
                                        // Nothing here can reach a half-made key, and reporting
                                        // this instead of the original would name the wrong
                                        // failure.
                                    }
                                }
                            } else {
                                // Put the previous record back and leave the mechanisms alone:
                                // rememberNow writes the device record last, so a failure inside
                                // it leaves the existing record standing, and forgetting every
                                // mechanism here would destroy a remembered unlock this device
                                // already had and the caller never asked to give up.
                                //
                                // What cannot be undone is ensureKey having replaced the stored
                                // key under the same id before failing later; the device record
                                // then no longer unwraps, and a password unlock is the way back.
                                // That is narrower than losing the vault, which is what the
                                // delete did.
                                commitMetadata(incoming, local);
                            }
                            lock();
                            throw rememberFailed;
                        }
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
                    Bytes.zero(password);
                }
            }
        });
        return out;
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
        final int generation = lockGeneration;
        background(new Runnable() {
            @Override
            public void run() {
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
                        Object saved = Storage.getInstance().readObject(deviceRecordKey());
                        try {
                            rememberNow(policy);
                        } catch (VaultException establishFailed) {
                            // rememberNow writes the device record, so a failure partway can
                            // leave it describing a mechanism that was never completed. Put back
                            // exactly what was there and report the failure: nothing changed.
                            if (saved instanceof String) {
                                Storage.getInstance().writeObject(deviceRecordKey(), saved);
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
                        if (outgoing != deviceProtection(policy)) {
                            requireKeyDeleted(outgoing,
                                    "the previous device key could not be deleted");
                        }
                        requireDeviceRecordStillWanted(generation);
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
        return keyAtVersion(metadata, dataKey, wanted);
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

    private byte[] recordMac(VaultMetadata meta, byte[] key) {
        byte[] saved = meta.mac;
        meta.mac = null;
        try {
            Hmac mac = Hmac.create(Hash.SHA256, key);
            mac.update(Bytes.utf8("cn1.vault.record.v1"));
            mac.update(Bytes.utf8(meta.serializeForMac()));
            return mac.doFinal();
        } finally {
            meta.mac = saved;
        }
    }

    /// The one subkey derivation, so the live handle and a recovered older one cannot drift.
    static byte[] deriveSubkey(byte[] source, String purpose) {
        Hmac mac = Hmac.create(Hash.SHA256, source);
        mac.update(Bytes.utf8("cn1.vault.subkey.v1"));
        mac.update(Bytes.utf8(purpose == null ? "" : purpose));
        return mac.doFinal();
    }

    private void requireUnlocked() {
        checkAutoLock();
        if (dataKey == null || metadata == null) {
            throw new VaultException(VaultError.LOCKED, "the vault is locked");
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

    private void checkAutoLock() {
        long idle = options.getAutoLockMillis();
        if (idle > 0 && dataKey != null && System.currentTimeMillis() - lastActivity > idle) {
            lock();
        }
    }

    private void touch() {
        lastActivity = System.currentTimeMillis();
    }

    /// The lock generation, for a handle to notice that it has been invalidated.
    int generation() {
        return lockGeneration;
    }

    /// The key generation, for a handle to notice that the key it derives from has been replaced.
    int keyGeneration() {
        return keyGeneration;
    }

    /// Installs a data key, and tells every handle derived from the old one that it is stale.
    ///
    /// One place, because a counter that is bumped at some of the assignments is worse than none:
    /// a handle would keep working across exactly the rotation nobody remembered to stamp.
    private void adoptKey(byte[] key) {
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
        if (metadata != null) {
            return metadata;
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
            stored = Storage.getInstance().readObject(metadataKey());
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
            if (Storage.getInstance().exists(metadataKey())) {
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
    }

    private void writeMetadata(VaultMetadata meta) {
        if (!Storage.getInstance().writeObject(metadataKey(), meta.serialize())) {
            throw new VaultException(VaultError.QUOTA_EXCEEDED,
                    "the vault record could not be written to storage");
        }
    }

    private DeviceRecord deviceRecord() {
        Object stored = Storage.getInstance().readObject(deviceRecordKey());
        if (!(stored instanceof String)) {
            return null;
        }
        return DeviceRecord.parse((String) stored);
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

    /// The protection for the policy this device is actually enrolled under, which is the one to
    /// use for an operation on an existing wrap.
    private DeviceProtection deviceProtection() {
        return deviceProtection(getPolicy());
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
    /// Publishes a recovered key, and undoes it if a lock arrived while it was being published.
    ///
    /// The check and the assignment are two statements and a lock can land between them. What
    /// this does NOT do is make them atomic: that means a mutex in core, and this project's
    /// model is one thread on each side of a boundary with a plain int counter for work that
    /// outlives the EDT turn that started it -- which is exactly what `generation` is. A lock
    /// here would be the first of several, each closing the interleaving the last one opened.
    ///
    /// So the window is not closed; the OUTCOME is. If the generation moved, the key just
    /// published is withdrawn and the vault is left locked, which is the state lock() was
    /// asking for. A caller that raced is told LOCKED rather than handed an open vault, and
    /// the transient is a few statements wide and self-correcting rather than permanent.
    private void publishKey(int generation, VaultMetadata meta, byte[] key) {
        metadata = meta;
        adoptKey(key);
        if (generation != lockGeneration) {
            lock();
            throw new VaultException(VaultError.LOCKED,
                    "the vault was locked while it was being unlocked");
        }
    }

    /// Why this is a plain int and not volatile, which a review has asked for more than once.
    ///
    /// Codename One's model is one thread on each side of a boundary rather than two on the same
    /// state, and its rule for work that outlives the turn that started it is a plain counter --
    /// explicitly not a lock and not volatile. The PR quality gate enforces the same thing:
    /// `AvoidUsingVolatile` is on the forbidden PMD list, so the suggested change does not
    /// compile past CI, and `volatile` appears nowhere else in this package or in Display.
    ///
    /// What is true, and worth stating rather than hiding: the sanctioned pattern compares the
    /// counter ON the EDT, and these workers compare it off it, so the Java memory model
    /// promises nothing about when an increment becomes visible to them. Closing that properly
    /// means the rest of the prescribed answer -- workers that take what they need as parameters
    /// and return through callSerially, touching no field -- which is a redesign of this class's
    /// asynchronous API rather than a keyword, and belongs to whoever decides that trade.
    ///
    /// What is here instead bounds the damage rather than the window: every publication goes
    /// through publishKey, which re-reads the counter and locks again if it moved, so a worker
    /// that loses the race leaves the vault closed rather than open.
    ///
    /// lock() promises that nothing in flight delivers afterwards, and an operation that reads
    /// storage and decrypts is in flight for long enough to matter -- an EDT caller's lifecycle
    /// callback can land squarely inside it. The generation has to be captured on the CALLING
    /// thread: read inside the worker it can already be the post-lock value, and the check then
    /// passes for the very interleaving it exists to catch.
    /// Withdraws a device record that a lock landed on top of, and reports the lock.
    ///
    /// The device wrap is a way back into this vault without a password, and establishing one runs
    /// through a store that prompts -- a passkey, a keystore with user verification -- so the write
    /// takes as long as the user takes. `lock()` can land anywhere inside that. Every other path
    /// that publishes something reopenable already checks the generation it started on; these two
    /// did not, so a vault the application had asked to close was left with a fresh passwordless
    /// unlock on disk, and the call reported success.
    ///
    /// Deleting is the rollback, because there was nothing here to put back: this branch runs only
    /// when the record is new or its policy is changing, and the change-of-policy case saves and
    /// restores the previous record on its own failure path already.
    ///
    /// Not every path needs this. `forgetDevice` and `destroyLocalData` only ever remove, so a lock
    /// landing inside one leaves less behind rather than more, and `changePassword` is documented
    /// to work on a locked instance and publishes no key at all.
    private void requireDeviceRecordStillWanted(int generation) {
        if (generation == lockGeneration) {
            return;
        }
        Storage.getInstance().deleteStorageFile(deviceRecordKey());
        throw new VaultException(VaultError.LOCKED,
                "the vault was locked while this device was being remembered; nothing that can "
                + "reopen it without a password was left behind");
    }

    private void requireSameGeneration(int generation) {
        if (generation != lockGeneration) {
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
    private void requireEverythingGone(Storage storage, String[] entries, String prefix) {
        StringBuilder left = new StringBuilder();
        for (String entry : entries) {
            if (entry != null && entry.startsWith(prefix) && storage.exists(entry)) {
                left.append(left.length() == 0 ? "" : ", ").append("a secret");
                break;
            }
        }
        if (storage.exists(deviceRecordKey())) {
            left.append(left.length() == 0 ? "" : ", ").append("the device wrap");
        }
        if (storage.exists(metadataKey())) {
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
        for (String entry : entries) {
            if (entry != null && entry.startsWith(prefix)) {
                return false;
            }
        }
        return true;
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

    private static char[] chars(byte[] utf8) {
        String s = Bytes.fromUtf8(utf8, 0, utf8.length);
        char[] out = new char[s.length()];
        s.getChars(0, s.length(), out, 0);
        return out;
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
