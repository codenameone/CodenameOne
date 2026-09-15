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
package com.codename1.impl.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.codename1.io.Log;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.SecureStorage;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import com.codename1.security.vault.Protection;
import com.codename1.security.vault.ProtectionReport;

/**
 * Android backing for {@link SecureStorage}. Values are AES/CBC/PKCS7-encrypted
 * with a key stored in the AndroidKeyStore (alias {@code BiometricsKey}), then
 * persisted to a private {@code SharedPreferences} file along with the
 * randomly-generated IV. The keystore key is created with
 * {@code setUserAuthenticationRequired(true)} so a write or read forces a
 * biometric prompt; if the user re-enrols biometrics the key becomes
 * permanently invalidated and reads fail with
 * {@link BiometricError#KEY_REVOKED}.
 *
 * <p>Carries forward two non-obvious workarounds from the original cn1lib that
 * must NOT be reverted without re-testing:</p>
 * <ul>
 *   <li>On API 33+ the {@code setUserAuthenticationRequired} call is skipped
 *   to side-step <a href="https://github.com/codenameone/FingerprintScanner/issues/8">FingerprintScanner #8</a>.
 *   <li>On Samsung devices running 8.0.0 the cipher init can succeed but
 *   final decryption then fails with a key-invalidated error; we delete the
 *   key and recreate it on first failure to recover.
 *   See <a href="https://issuetracker.google.com/u/0/issues/65578763">Google issue 65578763</a>.
 * </ul>
 *
 * <p>The non-prompting tier ({@code set(account, value)} and friends) uses a
 * <em>separate</em> keystore key ({@code CN1PlainKey}) and preferences file,
 * created without {@code setUserAuthenticationRequired}, with AES/GCM. Keeping
 * it separate matters: the biometric key is invalidated whenever the user
 * re-enrols biometrics, and secrets read on every network call must survive
 * that.</p>
 */
public final class AndroidSecureStorage extends SecureStorage {

    private static final String KEY_ID = "BiometricsKey";
    private static final String PREFS = "CN1BiometricSecureStorage";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * Deliberately distinct from {@link #KEY_ID}: the biometric key is created
     * with {@code setUserAuthenticationRequired(true)} and is invalidated when
     * the user re-enrols biometrics. The non-prompting tier must survive that,
     * so it gets its own key and its own preferences file.
     */
    private static final String PLAIN_KEY_ID = "CN1PlainKey";
    private static final String PLAIN_PREFS = "CN1PlainSecureStorage";

    /**
     * Serializes load-check-generate on the non-prompting key, and the shared
     * AndroidKeyStore handle with it. Static because the keystore alias is
     * process-wide, so two instances would race just as two threads would.
     */
    private static final Object PLAIN_KEY_LOCK = new Object();
    private static final int GCM_TAG_BITS = 128;

    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private boolean keyRevoked;
    private CancellationSignal cancellationSignal;

    AndroidSecureStorage() {
    }

    @Override
    public void setKeychainAccessGroup(String group) {
        // iOS-only; no-op on Android.
    }

    @Override
    public AsyncResource<Boolean> set(final String reason, final String account, final String value) {
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        if (Build.VERSION.SDK_INT < 23) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Android API 23 required for biometric secure storage"));
            return result;
        }
        runAuthenticatedCipher(reason, account, Cipher.ENCRYPT_MODE, result,
                new EncryptCipherWork(account, value));
        return result;
    }

    private static final class EncryptCipherWork implements CipherWork<Boolean> {
        private final String account;
        private final String value;
        EncryptCipherWork(String account, String value) {
            this.account = account;
            this.value = value;
        }
        @Override
        public Boolean run(Cipher c) throws Exception {
            byte[] enc = c.doFinal(value.getBytes("UTF-8"));
            SharedPreferences sp = AndroidNativeUtil.getActivity()
                    .getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            // commit(), so the Boolean this hands back is a statement about the disk. The
            // pair of entries is also all-or-nothing that way: apply() could persist a
            // ciphertext whose IV had not landed, which decrypts to nothing on the next
            // launch and looks to the caller like a value it successfully stored.
            return Boolean.valueOf(sp.edit()
                    .putString("v_" + account, Base64.encodeToString(enc, Base64.DEFAULT))
                    .putString("iv_" + account, Base64.encodeToString(c.getIV(), Base64.DEFAULT))
                    .commit());
        }
    }

    @Override
    public AsyncResource<String> get(final String reason, final String account) {
        final AsyncResource<String> result = new AsyncResource<String>();
        if (Build.VERSION.SDK_INT < 23) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Android API 23 required for biometric secure storage"));
            return result;
        }
        SharedPreferences sp = AndroidNativeUtil.getActivity()
                .getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (sp.getString("iv_" + account, null) == null) {
            result.error(new BiometricException(BiometricError.UNKNOWN,
                    "No secure storage entry for account: " + account));
            return result;
        }
        runAuthenticatedCipher(reason, account, Cipher.DECRYPT_MODE, result,
                new DecryptCipherWork(account));
        return result;
    }

    private static final class DecryptCipherWork implements CipherWork<String> {
        private final String account;
        DecryptCipherWork(String account) { this.account = account; }
        @Override
        public String run(Cipher c) throws Exception {
            SharedPreferences sp2 = AndroidNativeUtil.getActivity()
                    .getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            byte[] enc = Base64.decode(sp2.getString("v_" + account, ""), Base64.DEFAULT);
            byte[] dec = c.doFinal(enc);
            return new String(dec, "UTF-8");
        }
    }

    @Override
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        SharedPreferences sp = AndroidNativeUtil.getActivity()
                .getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // And the prompting tier deletes durably too. This is the credential a logout
        // clears; reporting it gone while the removal sits in memory means it comes back
        // if the process is killed before the write lands, which on Android is how a
        // process usually ends.
        result.complete(Boolean.valueOf(
                sp.edit().remove("v_" + account).remove("iv_" + account).commit()));
        return result;
    }

    // --- Non-prompting tier ------------------------------------------------
    //
    // AES/GCM under a dedicated AndroidKeyStore key created *without*
    // setUserAuthenticationRequired, so reads never raise a biometric prompt.
    // Deliberately not androidx.security EncryptedSharedPreferences: that
    // would force a transitive dependency on every Android build and it is
    // itself deprecated. The value is stored as
    // base64(iv) + ":" + base64(ciphertext) in a private preferences file.

    /// What the Android store provides for the non-prompting tier.
    ///
    /// Two different things wear this name. From API 23 the value is AES-GCM ciphertext under an
    /// `AndroidKeyStore` key the application cannot export, which is a real protection. Below 23
    /// there is no keystore to use and the value is Base64 in preferences -- obfuscation, and
    /// reported as such rather than rounded up.
    ///
    /// `HARDWARE_BACKED` stays `UNKNOWN` even on a modern device: whether the keystore key lives
    /// in a TEE or StrongBox is a property of the hardware, and this class does not query the key
    /// attestation that would establish it.
    /// What protects one entry, which on an upgraded device is not what the store can provide.
    ///
    /// The store-wide answer is about the API level: from 23 there is a keystore and entries
    /// written since are encrypted with it. An entry written by `legacyPlainSet` on API 22 and
    /// left behind by an OS upgrade is still Base64 in preferences, and the store-wide report
    /// called it encrypted -- so `SecureStorage.get(account, required)` accepted an
    /// ENCRYPTED_AT_REST requirement and then handed back the plaintext, including when the
    /// rewrite that was supposed to fix it failed.
    ///
    /// Recognised the same way `get` recognises it: no IV separator. That is the format itself
    /// rather than a flag beside it, so an entry cannot be described as migrated while it is not.
    @Override
    public ProtectionReport protectionOf(String account) {
        if (account != null && isLegacyPlaintext(account)) {
            return ProtectionReport.builder()
                    .set(Protection.PERSISTENT, true)
                    .set(Protection.ENCRYPTED_AT_REST, false)
                    .set(Protection.NON_EXTRACTABLE_KEY, false)
                    .set(Protection.OS_PROTECTED, false)
                    .set(Protection.HARDWARE_BACKED, false)
                    .set(Protection.USER_VERIFICATION, false)
                    .set(Protection.ISOLATED_FROM_APPLICATION_CODE, false)
                    .build();
        }
        return protection();
    }

    /// Whether this entry is still in the pre-keystore format, read without decrypting anything.
    ///
    /// Through plainPrefs(), which is the file a legacy value is actually in. The first version of
    /// this opened PREFS -- the prompting, biometric tier -- where a value written by
    /// legacyPlainSet has never been, so it saw nothing, fell through to the store-wide report,
    /// and let a required ENCRYPTED_AT_REST read hand back exactly the plaintext it was meant to
    /// catch. It is also the accessor that works without an Activity, which matters for the same
    /// reason get() uses it: this tier exists so a background caller can read a cached secret.
    private boolean isLegacyPlaintext(String account) {
        try {
            SharedPreferences prefs = plainPrefs();
            if (prefs == null) {
                return false;
            }
            String stored = prefs.getString(account, null);
            return stored != null && stored.indexOf(':') < 0;
        } catch (Throwable cannotAsk) {
            // Cannot establish that it is legacy, and guessing either way is worse than the
            // store-wide answer the caller would otherwise have had.
            return false;
        }
    }

    @Override
    public ProtectionReport protection() {
        boolean keystore = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;
        return ProtectionReport.builder()
                .set(Protection.PERSISTENT, true)
                .set(Protection.ENCRYPTED_AT_REST, keystore)
                // The keystore key itself cannot be exported, which is exactly what this flag is
                // about -- and below API 23 there is no such key.
                .set(Protection.NON_EXTRACTABLE_KEY, keystore)
                .set(Protection.OS_PROTECTED, keystore)
                .set(Protection.HARDWARE_BACKED, ProtectionReport.UNKNOWN)
                .set(Protection.USER_VERIFICATION, false)
                .set(Protection.ISOLATED_FROM_APPLICATION_CODE, false)
                .build();
    }

    @Override
    public boolean set(String account, String value) {
        if (account == null || value == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 23) {
            return legacyPlainSet(account, value);
        }
        try {
            // The whole use-and-persist runs under the same lock a reset takes.
            // Releasing it after the lookup let a concurrent resetPlainKey() delete the
            // alias and clear the preferences between here and the write, so this
            // reported success while storing ciphertext under a key that no longer
            // exists -- unreadable forever, and silently so.
            synchronized (PLAIN_KEY_LOCK) {
                // The invalid-key DECISION is taken in here too, not in a catch outside
                // the lock. Deciding out there let a delayed caller reset a key that was
                // no longer the one that failed it: another caller had already reset, a
                // writer had created a fresh key and committed ciphertext under it, and
                // this one then deleted that new key and wiped every stored value --
                // destroying data written after the failure it was reacting to.
                try {
                    SecretKey key = plainKey(true);
                    if (key == null) {
                        return false;
                    }
                    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                    c.init(Cipher.ENCRYPT_MODE, key);
                    byte[] enc = c.doFinal(value.getBytes("UTF-8"));
                    SharedPreferences prefs = plainPrefs();
                    if (prefs == null) {
                        return false;
                    }
                    // commit(), not apply(): apply() is asynchronous, so the write could
                    // land on disk after a reset that ran once this lock was released --
                    // storing ciphertext under a key that had already been deleted.
                    // Holding the lock is only atomic if the persist finishes inside it.
                    return prefs.edit()
                            .putString(account, Base64.encodeToString(c.getIV(), Base64.NO_WRAP)
                                    + ":" + Base64.encodeToString(enc, Base64.NO_WRAP))
                            .commit();
                } catch (InvalidKeyException e) {
                    // Includes KeyPermanentlyInvalidatedException.
                    resetPlainKey();
                    return false;
                } catch (UnrecoverableKeyException e) {
                    // Handled like an invalid key rather than falling into the generic
                    // catch: leaving the unusable alias installed made every later write
                    // return false for good, and only a read happened to clear it -- so
                    // an app that only ever writes could never store anything again.
                    resetPlainKey();
                    return false;
                }
            }
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /**
     * Creates an entry behind a gate the filesystem decides, so two processes cannot both create
     * one.
     *
     * <p>An application can declare components with their own {@code android:process}, and the
     * inherited implementation checks and then writes: both processes can find nothing stored,
     * generate different managed database keys and each overwrite the other, after which the
     * database is encrypted with a key that no longer exists. {@code createNewFile()} is decided
     * by the filesystem and cannot be won twice, so exactly one caller writes.</p>
     *
     * <p>The caller that loses reports nothing rather than writing. It cannot read the winner's
     * value either: {@code SharedPreferences} caches per process and offers no way to reload, so
     * a process that had already opened the file will not see a write made by another one. That
     * turns a permanent, silent corruption into a transient failure -- {@code ManagedKeys} raises
     * KEY_UNAVAILABLE and the next launch, whose process reads the file fresh, finds the key.</p>
     *
     * <p>That last paragraph described what this was supposed to do and not what it did. The
     * loser blocked on the lock, re-read through {@code get(account)} -- its own stale cache --
     * saw nothing, and wrote its own value over the winner's. The first process meanwhile kept
     * using the value it had cached and may already have encrypted data under it, so the outcome
     * was exactly the permanent corruption the gate exists to prevent, just narrowed to the
     * window where both processes had opened the preferences before either wrote.</p>
     *
     * <p>So the recheck under the lock cannot be the preferences: whatever decides it has to be
     * visible across processes, and the gate file already is. The winner marks it, and a caller
     * that finds the mark but reads nothing reports nothing -- which is the transient failure
     * above, now actually delivered.</p>
     *
     * @param account the account to create
     * @param value the value to store when there is none
     * @return the value now stored, or null when this caller did not store it and cannot read what
     *   did
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
        java.io.File gate = gateFile(account);
        if (gate == null) {
            return super.setIfAbsent(account, value);
        }
        java.io.RandomAccessFile handle = null;
        java.nio.channels.FileLock lock = null;
        try {
            handle = new java.io.RandomAccessFile(gate, "rw");
            // A lock rather than the file's existence: the system releases it when the process
            // ends however it ends, so a process that dies here cannot leave the alias
            // permanently uncreatable. Blocking, so a second caller waits for the first rather
            // than proceeding as though the entry were absent.
            lock = handle.getChannel().lock();
            String stored = get(account);
            if (stored != null) {
                return stored;
            }
            if (handle.length() > 0) {
                // Marked, so some process has already stored this account -- and this one cannot
                // see it, because the read above went through a SharedPreferences instance that
                // was cached before that write. Reporting nothing is the honest answer and the
                // documented one; writing here is what overwrote a key the winner was already
                // encrypting under.
                return null;
            }
            if (!set(account, value)) {
                return null;
            }
            try {
                // After the write, never before: a mark left by a store that then failed would
                // make the account permanently uncreatable, which is worse than the race.
                handle.seek(0);
                handle.write(1);
                handle.getChannel().force(true);
            } catch (java.io.IOException cannotMark) {
                // Fails CLOSED. An earlier version logged this and answered with the value on
                // the reasoning that it was stored either way -- which gives away the entire
                // mechanism, because the mark is the ONLY thing that stops the stale-cache case
                // two branches above. A second process whose SharedPreferences was cached before
                // this write reads no value AND no mark, takes the gate, and stores a different
                // managed database or vault key over this one -- while this caller has been told
                // it owns the first and is already encrypting under it. That data is then
                // orphaned for good.
                //
                // So the candidate is withdrawn and this reports nothing. Even if the removal
                // itself fails, answering null is what makes it safe: the caller never uses this
                // value, so nothing is encrypted under it and a later winner overwriting it
                // costs nothing. The retry is the caller's, and it is a retry rather than a loss.
                Log.e(cannotMark);
                if (!remove(account)) {
                    // Already covered by the paragraph above: this leaves a value stored under
                    // no mark, and answering null is what keeps that harmless, because the caller
                    // never uses it and so nothing is encrypted under it.
                    Log.p("SecureStorage: the unmarked candidate could not be withdrawn",
                            Log.WARNING);
                }
                return null;
            }
            return value;
        } catch (java.io.IOException cannotLock) {
            Log.e(cannotLock);
            return super.setIfAbsent(account, value);
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (java.io.IOException ignored) {
                    Log.e(ignored);
                }
            }
            if (handle != null) {
                try {
                    handle.close();
                } catch (java.io.IOException ignored) {
                    Log.e(ignored);
                }
            }
        }
    }

    /// The context the non-prompting tier resolves its files from.
    ///
    /// Never an Activity: this tier is the one a background service uses. The prompting tier
    /// above genuinely needs an Activity and keeps asking for one.
    private static Context context() {
        Context ctx = AndroidNativeUtil.getContext();
        if (ctx == null) {
            throw new IllegalStateException("no Android context");
        }
        return ctx;
    }

    /** The file whose creation decides which caller stores this account. */
    private java.io.File gateFile(String account) {
        try {
            // getContext(), not getActivity(), for the reason plainPrefs() gives: a port
            // initialized from a background service has no Activity and does have a context, and
            // this tier exists precisely so a background caller can work. Through getActivity()
            // this threw, the catch answered null, and setIfAbsent fell back to the inherited
            // check-then-write -- losing the cross-process gate in exactly the configuration
            // (a component with its own android:process) the gate was written for.
            java.io.File dir = new java.io.File(context()
                    .getApplicationContext().getFilesDir(), "cn1securestorage");
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return null;
            }
            return new java.io.File(dir, gateName(account));
        } catch (Throwable noContext) {
            return null;
        }
    }

    @Override
    public int entryState(String account) {
        if (account == null) {
            return ENTRY_UNKNOWN;
        }
        if (Build.VERSION.SDK_INT < 23) {
            return legacyPlainGet(account) != null ? ENTRY_PRESENT : ENTRY_ABSENT;
        }
        SharedPreferences prefs = plainPrefs();
        if (prefs == null) {
            // The store itself could not be opened, so nothing can be said about what is in it.
            return ENTRY_UNKNOWN;
        }
        // contains(), not get(): the question is whether an entry exists, and an entry that is
        // there but cannot be decrypted still exists. Answering absent for it is what would let a
        // caller overwrite a key it could not read.
        return prefs.contains(account) ? ENTRY_PRESENT : ENTRY_ABSENT;
    }

    @Override
    public String get(String account) {
        if (account == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT < 23) {
            return legacyPlainGet(account);
        }
        SharedPreferences prefs = plainPrefs();
        if (prefs == null) {
            return null;
        }
        String stored = prefs.getString(account, null);
        if (stored == null) {
            return null;
        }
        int sep = stored.indexOf(':');
        if (sep < 0) {
            // No IV separator, so this was written by legacyPlainSet on API 22 or below
            // and the device has since been upgraded to 23+. Reporting it missing would
            // silently discard a cached credential across an OS upgrade the user did not
            // choose to lose anything by. Decode it and re-store it encrypted, so this
            // only happens once.
            String legacy = decodeLegacyPlain(stored);
            if (legacy != null) {
                set(account, legacy);
            }
            return legacy;
        }
        try {
            // Same reasoning as set(): a reset landing mid-read would otherwise
            // invalidate the key between the lookup and the decrypt.
            synchronized (PLAIN_KEY_LOCK) {
                // The invalid-key DECISION is taken in here too, not in a catch
                // outside the lock. Deciding out there let a delayed caller reset a key
                // that was no longer the one that failed it: another caller had already
                // reset, a writer had created a fresh key and committed ciphertext under
                // it, and this one then deleted that new key and wiped every stored
                // value -- destroying data written after the failure it was reacting to.
                try {
                    SecretKey key = plainKey(false);
                    if (key == null) {
                        return null;
                    }
                    byte[] iv = Base64.decode(stored.substring(0, sep), Base64.NO_WRAP);
                    byte[] enc = Base64.decode(stored.substring(sep + 1), Base64.NO_WRAP);
                    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                    c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
                    return new String(c.doFinal(enc), "UTF-8");
                } catch (InvalidKeyException e) {
                    // The key was invalidated out from under us (device-wide credential
                    // change, or the Samsung 8.0.0 quirk documented on the biometric
                    // tier). Everything encrypted under it is unrecoverable, so drop
                    // the key and the ciphertexts rather than failing forever.
                    resetPlainKey();
                    return null;
                } catch (UnrecoverableKeyException e) {
                    resetPlainKey();
                    return null;
                }
            }
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    @Override
    public boolean remove(String account) {
        if (account == null) {
            return false;
        }
        SharedPreferences prefs = plainPrefs();
        if (prefs == null) {
            return false;
        }
        // commit(), and its answer is this method's answer. apply() persists on a
        // background thread, so returning true said the credential was gone while the
        // deletion was still in memory: an app that removes a token on logout and is then
        // killed -- which is the ordinary way an Android process ends -- finds it back on
        // the next launch. A removal that reports success has to have happened, and this
        // is the one operation where the caller cannot verify it later by reading.
        //
        // Under the same lock as the write and the reset, so a removal cannot be
        // interleaved with a set that recreates the entry it was clearing.
        // ONE critical section over both halves, not two. Clearing the mark under the gate lock
        // and then releasing it before deleting the value left a window in between: a
        // setIfAbsent in another process could take the freed lock, find no mark and the value
        // still present, hand that value back as the one now stored -- and then have it deleted
        // by the removal still in progress here. For a vault device key that means a device wrap
        // written under a key that no longer exists, and the next remembered unlock fails.
        //
        // Inside the section the mark goes first and the value only if that succeeded, because
        // only one of the two orders can be recovered from: a cleared mark with the value still
        // present is read back by the next setIfAbsent and returned, while a removed value under
        // a surviving mark refuses that account forever -- forgetDevice() followed by
        // rememberDevice() could never establish a device key again without clearing application
        // data.
        //
        // What this does NOT close is setIfAbsent's unlocked fast path: a caller that reads a
        // value just before it is removed is using something that was true when it read it, and
        // no lock here can change that. What it closes is the LOCKED read seeing a state this
        // method is halfway through producing.
        java.io.File gate = gateFile(account);
        if (gate == null) {
            // No gate to coordinate through, so setIfAbsent never wrote a mark either.
            synchronized (PLAIN_KEY_LOCK) {
                return prefs.edit().remove(account).commit();
            }
        }
        return removeUnderGate(gate, prefs, account);
    }

    /**
     * The preferences file, resolved from the application context rather than an
     * Activity.
     *
     * <p>A port initialized from a background service has no Activity but does
     * have a context, and this tier exists precisely so a background caller can
     * read a cached secret without prompting. Requiring an Activity would make
     * {@code get()} throw there -- outside its try/catch, so the caller crashes
     * rather than reading the value it asked for.</p>
     */
    private SharedPreferences plainPrefs() {
        Context ctx = AndroidNativeUtil.getContext();
        if (ctx == null) {
            return null;
        }
        return ctx.getApplicationContext()
                .getSharedPreferences(PLAIN_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Loads the non-prompting keystore key, optionally creating it. Returns
     * null when the key is absent and {@code create} is false, or when
     * generation fails.
     */
    /**
     * Whether the non-prompting tier's key is held in dedicated security hardware.
     *
     * An API level only says the AndroidKeyStore API exists; emulators and plenty of real devices
     * back its keys in software. Callers use this to decide whether the platform is good enough
     * for genuinely sensitive data, so it asks the key what it actually is.
     *
     * @return true when a TEE or StrongBox holds the key
     */
    static boolean isPlainKeyInsideSecureHardware() {
        java.security.spec.KeySpec spec;
        Object level = null;
        try {
            AndroidSecureStorage storage = new AndroidSecureStorage();
            SecretKey key = storage.plainKey(true);
            if (key == null) {
                return false;
            }
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance(
                    key.getAlgorithm(), ANDROID_KEY_STORE);
            spec = factory.getKeySpec(key,
                    Class.forName("android.security.keystore.KeyInfo")
                            .asSubclass(java.security.spec.KeySpec.class));
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                // getSecurityLevel() replaced the deprecated isInsideSecureHardware() in API 31.
                // Reflective because the port compiles against an older SDK than it runs on.
                level = spec.getClass().getMethod("getSecurityLevel").invoke(spec);
            }
        } catch (Throwable cannotTell) {
            // Unable to determine, so report the weaker answer rather than overstating it.
            return false;
        }
        // Both results are typed here rather than inside the try, and by instanceof rather than by
        // a bare cast: a failed cast is not an exception everywhere this framework runs, so a cast
        // in a block that catches Throwable is one whose failure nothing would catch.
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            // Anything above SECURITY_LEVEL_SOFTWARE (0) is a TEE or StrongBox.
            return level instanceof Integer && ((Integer) level).intValue() > 0;
        }
        return spec instanceof android.security.keystore.KeyInfo
                && ((android.security.keystore.KeyInfo) spec).isInsideSecureHardware();
    }

    private SecretKey plainKey(boolean create) throws Exception {
        // The whole load-check-generate sequence is serialized, not just the
        // generation. Two first writers that each saw the alias absent would each
        // generate under it, and the second generation replaces the key the first
        // one had already encrypted with -- leaving that ciphertext permanently
        // undecryptable. The shared KeyStore is not thread safe either.
        synchronized (PLAIN_KEY_LOCK) {
            // A KeyStore instance of this tier's own. The biometric tier touches the
            // shared one without PLAIN_KEY_LOCK, and KeyStore is not thread safe, so
            // sharing it here would trade a race inside this tier for a race across the
            // two -- surfacing as intermittent keystore errors that neither tier's code
            // would explain. Widening this lock into the biometric path would be worse.
            KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
            ks.load(null);
            SecretKey existing = (SecretKey) ks.getKey(PLAIN_KEY_ID, null);
            if (existing != null || !create) {
                return existing;
            }
            KeyGenerator gen = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
            gen.init(new KeyGenParameterSpec.Builder(PLAIN_KEY_ID,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build());
            gen.generateKey();
            return (SecretKey) ks.getKey(PLAIN_KEY_ID, null);
        }
    }

    private void resetPlainKey() {
        // The accounts this process can see that the failed key protected. Read first, because
        // this is only the WORK LIST -- an account only another process has written is not one
        // this reset knows about, and its gate is therefore never touched.
        //
        // Only what the failed key protected. A device upgraded from API 22 can hold unmigrated
        // Base64 legacy entries in this same file alongside iv:ciphertext ones, and a legacy
        // value was never encrypted under the keystore key that has just become unusable -- it is
        // still perfectly readable. The blanket clear took those with it, which can permanently
        // orphan a managed database whose key had not been migrated yet. Recognised by the IV
        // separator, the same way get() recognises one.
        java.util.List<String> candidates = new java.util.ArrayList<String>();
        synchronized (PLAIN_KEY_LOCK) {
            SharedPreferences prefs = plainPrefs();
            if (prefs != null) {
                // entrySet rather than keySet plus get: SpotBugs flags the second as
                // WMI_WRONG_MAP_ITERATOR, and the gate is zero-findings.
                for (java.util.Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                    Object value = entry.getValue();
                    if (!(value instanceof String) || ((String) value).indexOf(':') >= 0) {
                        candidates.add(entry.getKey());
                    }
                }
            }
        }

        // EVERY gate lock first, and only then the monitor. That is the order setIfAbsent takes
        // -- gate, then PLAIN_KEY_LOCK through set() -- so the two cannot invert, and this holds
        // no monitor while it is acquiring gates, so a setIfAbsent already holding one runs to
        // completion rather than deadlocking against this.
        //
        // Holding them across the whole reset is what makes it CORRECT rather than merely narrow.
        // The previous version cleared the values under the monitor and swept the marks
        // afterwards, re-checking each account under its gate before clearing its mark -- and
        // that re-check was a SharedPreferences lookup, which this class documents elsewhere as
        // unable to see another process's write. A replacement created in the window therefore
        // read as absent, its mark was truncated, and a third process with its own stale cache
        // could then pass setIfAbsent and overwrite a managed database or vault key already in
        // use. With the gates held there is no window and nothing to re-check: no other process
        // can create a replacement for any of these accounts while this runs.
        java.util.List<java.io.RandomAccessFile> handles =
                new java.util.ArrayList<java.io.RandomAccessFile>();
        java.util.List<java.nio.channels.FileLock> locks =
                new java.util.ArrayList<java.nio.channels.FileLock>();
        java.util.List<String> held = new java.util.ArrayList<String>();
        try {
            for (String account : candidates) {
                java.io.File gate = gateFile(account);
                if (gate == null) {
                    continue;
                }
                try {
                    java.io.RandomAccessFile handle = new java.io.RandomAccessFile(gate, "rw");
                    handles.add(handle);
                    locks.add(handle.getChannel().lock());
                    held.add(account);
                } catch (java.io.IOException cannotLock) {
                    // This account keeps its mark, which is better than clearing one this reset
                    // cannot hold -- that is the whole defect above.
                    Log.e(cannotLock);
                } catch (RuntimeException cannotLock) {
                    // OverlappingFileLockException among them, which would mean this process
                    // already holds that gate. Refusing to clear is the safe answer either way.
                    Log.e(cannotLock);
                }
            }

            synchronized (PLAIN_KEY_LOCK) {
                try {
                    // Same reasoning as plainKey(): this tier does not touch the shared KeyStore
                    // instance.
                    KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
                    ks.load(null);
                    ks.deleteEntry(PLAIN_KEY_ID);
                } catch (Exception e) {
                    Log.e(e);
                }
                SharedPreferences prefs = plainPrefs();
                if (prefs != null) {
                    // commit(), for the reason it always was: deleting the key and dropping the
                    // ciphertexts it protected is one step, and an asynchronous clear can be
                    // reordered after a writer's pending write.
                    SharedPreferences.Editor editor = prefs.edit();
                    for (String account : candidates) {
                        editor.remove(account);
                    }
                    editor.commit();
                }
            }

            // The marks, while the gates are still held. An account whose gate could not be
            // locked above is not in this list and keeps its mark.
            for (int iter = 0; iter < held.size(); iter++) {
                try {
                    java.io.RandomAccessFile handle = handles.get(iter);
                    handle.setLength(0);
                    handle.getChannel().force(true);
                } catch (java.io.IOException cannotClear) {
                    // Best effort and logged rather than fatal: this path is already the recovery
                    // from a key that can no longer decrypt anything, and a mark left standing
                    // refuses a later create rather than corrupting one.
                    Log.e(cannotClear);
                    Log.p("SecureStorage could not clear the gate mark for " + held.get(iter),
                            Log.WARNING);
                }
            }
        } finally {
            for (java.nio.channels.FileLock lock : locks) {
                try {
                    lock.release();
                } catch (java.io.IOException ignored) {
                    Log.e(ignored);
                }
            }
            for (java.io.RandomAccessFile handle : handles) {
                try {
                    handle.close();
                } catch (java.io.IOException ignored) {
                    Log.e(ignored);
                }
            }
        }
    }


    /// Clears the mark and deletes the value as one step, under the lock setIfAbsent takes.
    private boolean removeUnderGate(java.io.File gate, SharedPreferences prefs, String account) {
        java.io.RandomAccessFile handle = null;
        java.nio.channels.FileLock lock = null;
        try {
            handle = new java.io.RandomAccessFile(gate, "rw");
            lock = handle.getChannel().lock();
            handle.setLength(0);
            handle.getChannel().force(true);
            boolean removed;
            synchronized (PLAIN_KEY_LOCK) {
                removed = prefs.edit().remove(account).commit();
            }
            if (!removed) {
                // The mark goes back. Clearing it first is right while the removal SUCCEEDS --
                // a cleared mark beside a surviving value is read back by the next setIfAbsent
                // and returned -- but a removal that failed leaves exactly the pair this gate
                // exists to prevent: a value still in use and a gate saying nobody owns it, so a
                // later process with a stale preferences cache generates a replacement key and
                // overwrites it. Best effort, because the alternative to a failed rewrite is
                // nothing at all.
                try {
                    handle.seek(0);
                    handle.write(1);
                    handle.getChannel().force(true);
                } catch (java.io.IOException cannotRemark) {
                    Log.e(cannotRemark);
                }
            }
            return removed;
        } catch (java.io.IOException cannotRemove) {
            Log.e(cannotRemove);
            return false;
        } catch (RuntimeException cannotRemove) {
            // OverlappingFileLockException among them. The lock ordering above is what
            // prevents it; this is here so that being wrong about that is a refused clear
            // rather than an exception thrown out of a cleanup path.
            Log.e(cannotRemove);
            return false;
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (java.io.IOException ignored) {
                    Log.e(ignored);
                }
            }
            if (handle != null) {
                try {
                    handle.close();
                } catch (java.io.IOException ignored) {
                    Log.e(ignored);
                }
            }
        }
    }

    // API 22 and below have no KeyGenParameterSpec. The preferences file is
    // still app-private, but the value is only obfuscated, not encrypted --
    // it is extractable from a rooted device or a backup.
    private boolean legacyPlainSet(String account, String value) {
        warnLegacyPlainStorage();
        try {
            SharedPreferences prefs = plainPrefs();
            if (prefs == null) {
                return false;
            }
            // Same reason the encrypted tier commits: this returns whether the value was
            // stored, and with apply() it returned that before it was true. The legacy
            // path is weaker on confidentiality by construction; it does not get to be
            // weaker on the one thing the API actually promises.
            return prefs.edit()
                    .putString(account, Base64.encodeToString(
                            value.getBytes("UTF-8"), Base64.NO_WRAP))
                    .commit();
        } catch (IOException e) {
            Log.e(e);
            return false;
        }
    }

    /** The obfuscated-only form written on API 22 and below, or null if unreadable. */
    private String decodeLegacyPlain(String stored) {
        try {
            return new String(Base64.decode(stored, Base64.NO_WRAP), "UTF-8");
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    private String legacyPlainGet(String account) {
        warnLegacyPlainStorage();
        SharedPreferences prefs = plainPrefs();
        if (prefs == null) {
            return null;
        }
        String stored = prefs.getString(account, null);
        if (stored == null) {
            return null;
        }
        try {
            return new String(Base64.decode(stored, Base64.NO_WRAP), "UTF-8");
        } catch (IOException e) {
            Log.e(e);
            return null;
        }
    }

    private boolean legacyPlainWarned;

    private void warnLegacyPlainStorage() {
        if (!legacyPlainWarned) {
            legacyPlainWarned = true;
            Log.p("SecureStorage: this device predates Android API 23, so the "
                    + "non-prompting tier stores values obfuscated rather than "
                    + "encrypted. Do not use it for high-value secrets here.");
        }
    }

    /**
     * Generic helper that initialises the cipher under the keystore key,
     * prompts the user through {@link BiometricBackend} -- whichever of
     * {@code BiometricPrompt} and the legacy {@code FingerprintManager} this
     * device and this build have -- and on success runs the supplied
     * {@link CipherWork} against the authenticated cipher.
     */
    private <V> void runAuthenticatedCipher(final String reason, final String account,
                                            final int mode, final AsyncResource<V> result,
                                            final CipherWork<V> work) {
        SecretKey secret = getSecretKey();
        if (secret == null) {
            if (mode == Cipher.ENCRYPT_MODE) {
                if (!createKey()) {
                    failResult(result, BiometricError.UNKNOWN, "Failed to create keystore key");
                    return;
                }
            } else {
                if (keyRevoked) {
                    failResult(result, BiometricError.KEY_REVOKED, "Key has been invalidated");
                } else {
                    failResult(result, BiometricError.UNKNOWN, "No keystore key for account");
                }
                return;
            }
        }
        Cipher operationCipher = initCipher(mode, account);
        if (operationCipher == null) {
            if (mode == Cipher.ENCRYPT_MODE) {
                if (createKey()) {
                    operationCipher = initCipher(mode, account);
                }
                if (operationCipher == null) {
                    failResult(result, BiometricError.UNKNOWN, "Failed to initialise cipher");
                    return;
                }
            } else {
                failResult(result, BiometricError.KEY_REVOKED,
                        "Failed to initialise cipher; key must have been revoked");
                return;
            }
        }
        // Carried as a parameter from here on. It belongs to this operation and to no
        // other, which is what stops a concurrent call from handing its cipher to this
        // prompt.
        promptBiometric(reason, mode, account, result, work, operationCipher);
    }

    private <V> void promptBiometric(final String reason, final int mode, final String account,
                                     final AsyncResource<V> result, final CipherWork<V> work,
                                     final Cipher operationCipher) {
        final BiometricBackend backend = AndroidBiometrics.backend();
        if (backend == null) {
            failResult(result, BiometricError.NOT_AVAILABLE, "No biometric hardware");
            return;
        }
        AndroidBiometrics.runOnUi(new Runnable() {
            @Override
            public void run() {
                if (cancellationSignal != null) {
                    cancellationSignal.cancel();
                }
                final CancellationSignal cs = new CancellationSignal();
                cancellationSignal = cs;
                backend.authenticate(AndroidNativeUtil.getActivity(),
                        reason == null ? "Authenticate" : reason,
                        null, null, "Cancel",
                        operationCipher, cs, new BiometricBackend.Callback() {
                            @Override
                            public void onSuccess(Cipher authedCipher) {
                                cs.cancel();
                                if (authedCipher == null) {
                                    // The OS reported success without handing
                                    // back the CryptoObject we passed in, so
                                    // nothing proves a real unlock happened.
                                    failResult(result, BiometricError.AUTHENTICATION_FAILED,
                                            "Authenticated cipher missing -- "
                                                    + "biometric success may have been spoofed");
                                    return;
                                }
                                runCipherWork(authedCipher, work, result, mode, account);
                            }

                            @Override
                            public void onError(int errorCode, String errString) {
                                // See AndroidBiometrics: the legacy backend can
                                // report failure with the sensor still armed.
                                cs.cancel();
                                failResult(result,
                                        AndroidBiometrics.mapBiometricError(errorCode),
                                        errString == null ? "" : errString);
                            }
                        });
            }
        });
    }

    private <V> void runCipherWork(Cipher authedCipher, CipherWork<V> work,
                                   final AsyncResource<V> result, int mode, String account) {
        try {
            V v = work.run(authedCipher);
            succeedResult(result, v);
        } catch (Throwable t) {
            // Only a failure that says the KEY is finished deletes the key.
            //
            // There is one keystore key behind every biometric account, so this catch
            // used to answer a malformed stored value, or an Activity that went away
            // mid-prompt, by destroying every other entry in the store -- permanently,
            // and while telling the caller its key had been revoked when it had not.
            // The Samsung 8.0.0 quirk this was written for is still handled: a cipher
            // that initialises and then fails inside doFinal with a keystore error
            // underneath is that case, and isKeyInvalidation recognises it.
            if (isKeyInvalidation(t)) {
                removePermanentlyInvalidatedKey();
                failResult(result, BiometricError.KEY_REVOKED,
                        "Cipher operation failed; key invalidated: " + t.getMessage());
            } else {
                Log.e(t);
                failResult(result, BiometricError.UNKNOWN,
                        "Cipher operation failed: " + t.getMessage());
            }
        }
    }

    private <V> void succeedResult(final AsyncResource<V> result, final V value) {
        Display.getInstance().callSerially(new SucceedResultRunnable<V>(result, value));
    }

    private static final class SucceedResultRunnable<V> implements Runnable {
        private final AsyncResource<V> result;
        private final V value;
        SucceedResultRunnable(AsyncResource<V> result, V value) {
            this.result = result;
            this.value = value;
        }
        @Override
        public void run() {
            if (!result.isDone()) {
                result.complete(value);
            }
        }
    }

    private static <V> void failResult(final AsyncResource<V> result,
                                       final BiometricError err, final String msg) {
        Display.getInstance().callSerially(new FailResultRunnable<V>(result, err, msg));
    }

    private static final class FailResultRunnable<V> implements Runnable {
        private final AsyncResource<V> result;
        private final BiometricError err;
        private final String msg;
        FailResultRunnable(AsyncResource<V> result, BiometricError err, String msg) {
            this.result = result;
            this.err = err;
            this.msg = msg;
        }
        @Override
        public void run() {
            if (!result.isDone()) {
                result.error(new BiometricException(err, msg));
            }
        }
    }

    // --- Keystore / cipher helpers (faithful port of the cn1lib idioms) -----

    private KeyStore keyStore() {
        if (keyStore == null) {
            try {
                keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("KeyGenerator init failed", e);
            } catch (NoSuchProviderException e) {
                throw new RuntimeException("KeyGenerator init failed", e);
            } catch (KeyStoreException e) {
                throw new RuntimeException("KeyStore init failed", e);
            }
        }
        return keyStore;
    }

    private boolean createKey() {
        try {
            keyStore().load(null);
            KeyGenParameterSpec.Builder b = new KeyGenParameterSpec.Builder(KEY_ID,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            // Skip setUserAuthenticationRequired on API 33+ per
            // FingerprintScanner #8; the BiometricPrompt still authenticates
            // the user, but the keystore no longer ties the key lifetime to
            // biometric enrolment (which caused recovery failures).
            if (Build.VERSION.SDK_INT < 33) {
                b.setUserAuthenticationRequired(true);
            }
            keyGenerator.init(b.build());
            keyGenerator.generateKey();
            return true;
        } catch (NoSuchAlgorithmException e) {
            Log.e(e);
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(e);
        } catch (CertificateException e) {
            Log.e(e);
        } catch (IOException e) {
            Log.e(e);
        }
        return false;
    }

    private SecretKey getSecretKey() {
        keyRevoked = false;
        try {
            keyStore().load(null);
            return (SecretKey) keyStore.getKey(KEY_ID, null);
        } catch (UnrecoverableKeyException e) {
            keyRevoked = true;
        } catch (KeyStoreException e) {
            Log.e(e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(e);
        } catch (CertificateException e) {
            Log.e(e);
        } catch (IOException e) {
            Log.e(e);
        }
        return null;
    }

    /**
     * A NEW cipher every time, never a shared field.
     *
     * <p>The prompt is raised from a UI runnable, so an operation is in flight from the
     * moment it initialises its cipher until that runnable runs. With one instance field,
     * a second {@code set()} or {@code get()} starting in that window re-initialised the
     * same object and the first prompt was handed the second operation's cipher -- wrong
     * mode, or the wrong account's IV. The work then failed, and the failure handler read
     * that as an invalidated key and deleted the one key every biometric entry shares.</p>
     */
    private Cipher newCipher() {
        try {
            return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES
                    + "/" + KeyProperties.BLOCK_MODE_CBC
                    + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cipher init failed", e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("Cipher init failed", e);
        }
    }

    /** The initialised cipher for this one operation, or null if it could not be made. */
    private Cipher initCipher(int mode, String account) {
        try {
            SecretKey key = getSecretKey();
            if (key == null) {
                return null;
            }
            Cipher c = newCipher();
            if (mode == Cipher.ENCRYPT_MODE) {
                c.init(mode, key);
            } else {
                SharedPreferences sp = AndroidNativeUtil.getActivity()
                        .getApplicationContext()
                        .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                byte[] iv = Base64.decode(sp.getString("iv_" + account, ""), Base64.DEFAULT);
                c.init(mode, key, new IvParameterSpec(iv));
            }
            return c;
        } catch (KeyPermanentlyInvalidatedException e) {
            removePermanentlyInvalidatedKey();
            return null;
        } catch (InvalidKeyException e) {
            Log.e(e);
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(e);
            return null;
        }
    }

    private void removePermanentlyInvalidatedKey() {
        try {
            keyStore().deleteEntry(KEY_ID);
        } catch (KeyStoreException e) {
            Log.e(e);
        }
    }

    /**
     * Whether a failure means the keystore key is gone, as opposed to this one operation
     * having failed.
     *
     * <p>The distinction is the whole point. There is ONE key behind every biometric
     * account, so deleting it on any failure -- a malformed stored value, an Activity that
     * went away mid-prompt, a null passed into the work -- made every other entry
     * permanently unreadable, and told the caller its key had been revoked when it had
     * not. Only two shapes say the key itself is finished: the exception Android raises
     * for it, and the Samsung 8.0.0 quirk where a cipher initialises and then fails inside
     * doFinal with a keystore error underneath.</p>
     *
     * <p>https://issuetracker.google.com/u/0/issues/65578763</p>
     */
    private static boolean isKeyInvalidation(Throwable t) {
        // Bounded rather than while(cause != null): a self-referential cause is rare and
        // a hang inside a failure handler is worse than a missed classification.
        Throwable c = t;
        for (int depth = 0; c != null && depth < 8; depth++) {
            if (c instanceof KeyPermanentlyInvalidatedException) {
                return true;
            }
            if (c instanceof IllegalBlockSizeException
                    && c.getCause() instanceof KeyStoreException) {
                return true;
            }
            Throwable next = c.getCause();
            if (next == c) {
                break;
            }
            c = next;
        }
        return false;
    }

    /** Lambda-stand-in for Java 5 source level: cipher op that may throw. */
    private interface CipherWork<V> {
        V run(Cipher c) throws Exception;
    }
}
