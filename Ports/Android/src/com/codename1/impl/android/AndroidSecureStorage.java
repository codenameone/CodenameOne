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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
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
        boolean created;
        try {
            created = gate.createNewFile();
        } catch (java.io.IOException cannotCreate) {
            Log.e(cannotCreate);
            return super.setIfAbsent(account, value);
        }
        if (created) {
            if (set(account, value)) {
                return value;
            }
            // Nothing was stored, so the gate must not stay shut: the next caller has to be able
            // to create the entry rather than be turned away from one that was never written.
            if (!gate.delete()) {
                // Worth saying out loud rather than dropping: while that file is there, every
                // later attempt to create this key is refused, and the only thing that clears it
                // is removing the file.
                Log.p("Secure storage could not remove " + gate.getAbsolutePath()
                        + " after a failed write, so creating '" + account
                        + "' will be refused until that file is gone");
            }
            return null;
        }
        String stored = get(account);
        if (stored != null) {
            return stored;
        }
        // Another process holds the gate and this one cannot see its write. Reported rather than
        // written over, which is the whole point of the gate.
        return null;
    }

    /** The file whose creation decides which caller stores this account. */
    private java.io.File gateFile(String account) {
        try {
            java.io.File dir = new java.io.File(AndroidNativeUtil.getActivity()
                    .getApplicationContext().getFilesDir(), "cn1securestorage");
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return null;
            }
            return new java.io.File(dir, "gate-" + Integer.toHexString(account.hashCode()));
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
        synchronized (PLAIN_KEY_LOCK) {
            return prefs.edit().remove(account).commit();
        }
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
        // Deleting the key and dropping the ciphertexts it protected are one step, under
        // the same lock readers and writers hold. Clearing outside it left a window
        // where a writer had already encrypted under the old key and was about to store
        // a value this was about to wipe -- or worse, stored it just after.
        synchronized (PLAIN_KEY_LOCK) {
            try {
                // Same reasoning as plainKey(): this tier does not touch the shared
                // KeyStore instance.
                KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
                ks.load(null);
                ks.deleteEntry(PLAIN_KEY_ID);
            } catch (Exception e) {
                Log.e(e);
            }
            SharedPreferences prefs = plainPrefs();
            if (prefs != null) {
                // Also commit(), for the same reason: this method's whole purpose is to
                // make the key deletion and the ciphertext deletion one step, and an
                // asynchronous clear can be reordered after a writer's pending write.
                prefs.edit().clear().commit();
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
     * prompts the user via {@code BiometricPrompt} (or legacy
     * {@code FingerprintManager}), and on success runs the supplied
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
        if (Build.VERSION.SDK_INT >= 29) {
            promptBiometric29(reason, mode, account, result, work, operationCipher);
        } else {
            promptBiometricLegacy(mode, account, operationCipher, result, work);
        }
    }

    private <V> void promptBiometric29(final String reason, final int mode, final String account,
                                       final AsyncResource<V> result, final CipherWork<V> work,
                                       final Cipher operationCipher) {
        AndroidBiometrics.runOnUi(new Runnable() {
            @Override
            public void run() {
                if (cancellationSignal != null) {
                    cancellationSignal.cancel();
                }
                final CancellationSignal cs = new CancellationSignal();
                cancellationSignal = cs;
                BiometricsApi29.authenticateWithCipher(
                        AndroidNativeUtil.getActivity(),
                        reason == null ? "Authenticate" : reason,
                        null, null, "Cancel",
                        operationCipher,
                        cs,
                        new BiometricsApi29.CipherAuthCallback() {
                            @Override
                            public void onSuccess(Object authedCipher) {
                                cs.cancel();
                                runCipherWork((Cipher) authedCipher, work, result, mode, account);
                            }

                            @Override
                            public void onError(int errorCode, String errString) {
                                failResult(result,
                                        AndroidBiometrics.mapBiometricPromptError(errorCode),
                                        errString == null ? "" : errString);
                            }
                        });
            }
        });
    }

    private <V> void promptBiometricLegacy(final int mode, final String account,
                                           final Cipher operationCipher,
                                           final AsyncResource<V> result, final CipherWork<V> work) {
        AndroidBiometrics.runOnUi(new Runnable() {
            @Override
            public void run() {
                FingerprintManager fpm = (FingerprintManager)
                        AndroidNativeUtil.getActivity()
                                .getSystemService(Activity.FINGERPRINT_SERVICE);
                if (fpm == null) {
                    failResult(result, BiometricError.NOT_AVAILABLE, "No fingerprint hardware");
                    return;
                }
                if (cancellationSignal != null) {
                    cancellationSignal.cancel();
                }
                final CancellationSignal cs = new CancellationSignal();
                cancellationSignal = cs;
                FingerprintManager.CryptoObject crypto =
                        new FingerprintManager.CryptoObject(operationCipher);
                fpm.authenticate(crypto, cs, 0, new FingerprintManager.AuthenticationCallback() {
                    int failures;

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        failResult(result, AndroidBiometrics.mapFingerprintManagerError(errorCode),
                                errString == null ? "" : errString.toString());
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult r) {
                        cs.cancel();
                        runCipherWork(r.getCryptoObject().getCipher(), work, result, mode, account);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        if (failures++ > 5) {
                            cs.cancel();
                            failResult(result, BiometricError.AUTHENTICATION_FAILED,
                                    "Authentication failed");
                        }
                    }
                }, null);
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
