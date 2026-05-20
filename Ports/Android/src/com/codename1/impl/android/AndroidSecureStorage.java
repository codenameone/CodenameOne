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
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
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
 */
public final class AndroidSecureStorage extends SecureStorage {

    private static final String KEY_ID = "BiometricsKey";
    private static final String PREFS = "CN1BiometricSecureStorage";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private Cipher cipher;
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
        runAuthenticatedCipher(reason, account, Cipher.ENCRYPT_MODE, result, c -> {
            byte[] enc = c.doFinal(value.getBytes("UTF-8"));
            SharedPreferences sp = AndroidNativeUtil.getActivity()
                    .getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit()
                    .putString("v_" + account, Base64.encodeToString(enc, Base64.DEFAULT))
                    .putString("iv_" + account, Base64.encodeToString(c.getIV(), Base64.DEFAULT))
                    .apply();
            return Boolean.TRUE;
        });
        return result;
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
        runAuthenticatedCipher(reason, account, Cipher.DECRYPT_MODE, result, c -> {
            SharedPreferences sp2 = AndroidNativeUtil.getActivity()
                    .getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            byte[] enc = Base64.decode(sp2.getString("v_" + account, ""), Base64.DEFAULT);
            byte[] dec = c.doFinal(enc);
            return new String(dec, "UTF-8");
        });
        return result;
    }

    @Override
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        SharedPreferences sp = AndroidNativeUtil.getActivity()
                .getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().remove("v_" + account).remove("iv_" + account).apply();
        result.complete(Boolean.TRUE);
        return result;
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
        if (!initCipher(mode, account)) {
            if (mode == Cipher.ENCRYPT_MODE) {
                if (!createKey() || !initCipher(mode, account)) {
                    failResult(result, BiometricError.UNKNOWN, "Failed to initialise cipher");
                    return;
                }
            } else {
                failResult(result, BiometricError.KEY_REVOKED,
                        "Failed to initialise cipher; key must have been revoked");
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            promptBiometric29(reason, mode, account, result, work);
        } else {
            promptBiometricLegacy(mode, account, result, work);
        }
    }

    private <V> void promptBiometric29(final String reason, final int mode, final String account,
                                       final AsyncResource<V> result, final CipherWork<V> work) {
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
                        cipher,
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
                        new FingerprintManager.CryptoObject(cipher);
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
            // Samsung 8.0.0 quirk: the cipher passes init but doFinal fails
            // with a key-invalidated error. Delete the key and let the caller
            // retry the entire operation.
            // https://issuetracker.google.com/u/0/issues/65578763
            removePermanentlyInvalidatedKey();
            cipher = null;
            failResult(result, BiometricError.KEY_REVOKED,
                    "Cipher operation failed; key invalidated: " + t.getMessage());
        }
    }

    private <V> void succeedResult(final AsyncResource<V> result, final V value) {
        Display.getInstance().callSerially(() -> {
            if (!result.isDone()) {
                result.complete(value);
            }
        });
    }

    private static <V> void failResult(final AsyncResource<V> result,
                                       final BiometricError err, final String msg) {
        Display.getInstance().callSerially(() -> {
            if (!result.isDone()) {
                result.error(new BiometricException(err, msg));
            }
        });
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

    private Cipher cipher() {
        if (cipher == null) {
            try {
                cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES
                        + "/" + KeyProperties.BLOCK_MODE_CBC
                        + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Cipher init failed", e);
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException("Cipher init failed", e);
            }
        }
        return cipher;
    }

    private boolean initCipher(int mode, String account) {
        try {
            SecretKey key = getSecretKey();
            if (key == null) {
                return false;
            }
            if (mode == Cipher.ENCRYPT_MODE) {
                cipher().init(mode, key);
            } else {
                SharedPreferences sp = AndroidNativeUtil.getActivity()
                        .getApplicationContext()
                        .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                byte[] iv = Base64.decode(sp.getString("iv_" + account, ""), Base64.DEFAULT);
                cipher().init(mode, key, new IvParameterSpec(iv));
            }
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            removePermanentlyInvalidatedKey();
            return false;
        } catch (InvalidKeyException e) {
            Log.e(e);
            return false;
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(e);
            return false;
        }
    }

    private void removePermanentlyInvalidatedKey() {
        try {
            keyStore().deleteEntry(KEY_ID);
            cipher = null;
        } catch (KeyStoreException e) {
            Log.e(e);
        }
    }

    /** Lambda-stand-in for Java 5 source level: cipher op that may throw. */
    private interface CipherWork<V> {
        V run(Cipher c) throws Exception;
    }
}
