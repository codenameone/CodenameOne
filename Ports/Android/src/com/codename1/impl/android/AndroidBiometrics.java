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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import com.codename1.io.Log;
import com.codename1.security.AuthenticationOptions;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.BiometricType;
import com.codename1.security.Biometrics;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/// Android backing for [Biometrics]. Mirrors the dual-path behaviour of the
/// historical `FingerprintScanner` cn1lib but completes per-call
/// [AsyncResource] instances instead of a shared static callback.
///
/// Neither Android biometric API can be named from this file. `BiometricPrompt`
/// is newer than the `android.jar` the port jar compiles against, and
/// `FingerprintManager` was removed in API 37, so naming it broke every
/// generated application built against a 37 platform (issue #5701). Both live
/// behind [BiometricBackend], one implementation per package, resolved once by
/// [AndroidBiometrics#backend]; the legacy one reaches its API through
/// `FingerprintManagerCompat` so that it keeps working on an API 23-28 device
/// no matter which platform the application was compiled against.
public final class AndroidBiometrics extends Biometrics {

    // Android biometric error codes. BiometricPrompt and FingerprintManager
    // number these identically -- 1 through 12 are the same constants under two
    // names in AOSP -- which is why one mapping serves both backends. Inlined
    // because neither class can be named here, and because the two above 12
    // exist on androidx's BiometricPrompt rather than the framework's.
    private static final int ERROR_HW_UNAVAILABLE = 1;
    private static final int ERROR_UNABLE_TO_PROCESS = 2;
    private static final int ERROR_CANCELED = 5;
    private static final int ERROR_LOCKOUT = 7;
    private static final int ERROR_LOCKOUT_PERMANENT = 9;
    private static final int ERROR_USER_CANCELED = 10;
    private static final int ERROR_NO_BIOMETRICS = 11;
    private static final int ERROR_HW_NOT_PRESENT = 12;
    private static final int ERROR_NEGATIVE_BUTTON = 13;
    private static final int ERROR_NO_DEVICE_CREDENTIAL = 14;

    // Probe key tying biometric success to a real KeyStore unlock so the
    // success callback cannot be bypassed by app hooking tools (Frida etc.).
    // See CodeQL alert "Insecure local authentication" (java/android/insecure-local-authentication).
    private static final String PROBE_KEY_ID = "CN1BiometricsAuthProbeKey";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final byte[] PROBE_PLAINTEXT = new byte[]{0x42};

    /// The backend this device and this build have, or `null` when there is
    /// none. Resolved once at class initialisation: the answer cannot change
    /// while the process lives, and a lazily initialised static would be a
    /// SpotBugs finding in a tree that gates on zero of them.
    private static final BiometricBackend BACKEND = resolveBackend();

    private CancellationSignal cancellationSignal;
    private AsyncResource<Boolean> pending;

    AndroidBiometrics() {
    }

    @Override
    public boolean isSupported() {
        if (Build.VERSION.SDK_INT < 23) {
            return false;
        }
        PackageManager pm = AndroidNativeUtil.getActivity().getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            if (pm.hasSystemFeature("android.hardware.biometrics.face")
                    || pm.hasSystemFeature("android.hardware.biometrics.iris")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canAuthenticate() {
        if (Build.VERSION.SDK_INT < 23) {
            return false;
        }
        return !getAvailableBiometrics().isEmpty();
    }

    @Override
    public List<BiometricType> getAvailableBiometrics() {
        final List<BiometricType> out = new ArrayList<BiometricType>();
        if (Build.VERSION.SDK_INT < 23) {
            return out;
        }
        runOnUi(new CollectAvailableBiometricsRunnable(out));
        return out;
    }

    private static final class CollectAvailableBiometricsRunnable implements Runnable {
        private final List<BiometricType> out;
        CollectAvailableBiometricsRunnable(List<BiometricType> out) { this.out = out; }
        @Override
        public void run() { collectAvailableBiometrics(out); }
    }

    private static void collectAvailableBiometrics(List<BiometricType> out) {
        if (BACKEND == null) {
            return;
        }
        try {
            Activity act = AndroidNativeUtil.getActivity();
            PackageManager pm = act.getPackageManager();
            if (Build.VERSION.SDK_INT >= 29) {
                if (!AndroidNativeUtil.checkForPermission("android.permission.USE_BIOMETRIC",
                        "Authorize using biometrics")) {
                    return;
                }
            } else if (!AndroidNativeUtil.checkForPermission(
                    Manifest.permission.USE_FINGERPRINT,
                    "Authorize using fingerprint")) {
                return;
            }
            if (!BACKEND.canAuthenticate(act)) {
                return;
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                    && BACKEND.hasEnrolledFingerprints(act)) {
                out.add(BiometricType.FINGERPRINT);
            }
            if (Build.VERSION.SDK_INT >= 29) {
                if (pm.hasSystemFeature("android.hardware.biometrics.face")) {
                    out.add(BiometricType.FACE);
                }
                if (pm.hasSystemFeature("android.hardware.biometrics.iris")) {
                    out.add(BiometricType.IRIS);
                }
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    @Override
    public AsyncResource<Boolean> authenticate(final AuthenticationOptions opts) {
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        if (Build.VERSION.SDK_INT < 23) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Android API 23 (Marshmallow) required for biometric authentication"));
            return result;
        }
        if (BACKEND == null) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "No biometric API is available to this build"));
            return result;
        }
        final String reason = opts == null || opts.getReason() == null
                ? "Authenticate" : opts.getReason();
        final String title = opts == null || opts.getTitle() == null
                ? reason : opts.getTitle();
        final String negative = opts == null || opts.getNegativeButtonText() == null
                ? "Cancel" : opts.getNegativeButtonText();
        final String subtitle = opts == null ? null : opts.getSubtitle();
        final String description = opts == null ? null : opts.getDescription();

        pending = result;
        // Initialise a CryptoObject-backed Cipher that the OS will unlock
        // ONLY if real biometric authentication succeeds. The success callback
        // then performs a doFinal() against the unlocked cipher; if a hooking
        // tool bypasses the callback, doFinal() throws and the result fails.
        final Cipher probeCipher = initProbeCipher(result);
        if (probeCipher == null) {
            // initProbeCipher already errored the result.
            return result;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < 29
                        && !AndroidNativeUtil.checkForPermission(
                                Manifest.permission.USE_FINGERPRINT,
                                "Authorize using fingerprint")) {
                    completeError(result, BiometricError.NOT_AVAILABLE,
                            "USE_FINGERPRINT permission denied");
                    return;
                }
                if (cancellationSignal != null) {
                    cancellationSignal.cancel();
                }
                final CancellationSignal cs = new CancellationSignal();
                cancellationSignal = cs;
                BACKEND.authenticate(AndroidNativeUtil.getActivity(),
                        title, subtitle, description, negative,
                        probeCipher, cs, new BiometricBackend.Callback() {
                            @Override
                            public void onSuccess(Cipher authedCipher) {
                                cs.cancel();
                                // Confirm the unlock by running an actual
                                // crypto operation on the cipher the OS handed
                                // back. A hooked or spoofed success callback
                                // either has no CryptoObject at all or reaches
                                // a still-locked cipher, and doFinal() throws.
                                if (verifyProbeCipher(authedCipher)) {
                                    completeSuccess(result);
                                } else {
                                    completeError(result, BiometricError.AUTHENTICATION_FAILED,
                                            "Probe cipher rejected -- biometric success may have been spoofed");
                                }
                            }

                            @Override
                            public void onError(int code, String msg) {
                                // The legacy backend gives up after a run of
                                // unrecognised touches while the sensor is
                                // still listening, so the signal has to be
                                // cancelled here rather than left to the OS.
                                // Cancelling one the OS has already finished
                                // with is a no-op.
                                cs.cancel();
                                completeError(result, mapBiometricError(code), msg);
                            }
                        });
            }
        });
        return result;
    }

    /// Initialises an AES/CBC/PKCS7 Cipher under the Keystore probe key in
    /// ENCRYPT_MODE. The Keystore enforces user-authentication-required, so
    /// the Cipher only finalises after a real biometric prompt completes
    /// (see [CodeQL "Insecure local authentication"](https://github.com/codenameone/CodenameOne/security/code-scanning)).
    /// Returns `null` and errors the result if the key cannot be created or
    /// initialised.
    private Cipher initProbeCipher(AsyncResource<Boolean> result) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
                ks.load(null);
                SecretKey key = (SecretKey) ks.getKey(PROBE_KEY_ID, null);
                if (key == null) {
                    createProbeKey();
                    key = (SecretKey) ks.getKey(PROBE_KEY_ID, null);
                    if (key == null) {
                        completeError(result, BiometricError.UNKNOWN,
                                "Failed to create biometric probe key");
                        return null;
                    }
                }
                Cipher c = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES
                        + "/" + KeyProperties.BLOCK_MODE_CBC
                        + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
                c.init(Cipher.ENCRYPT_MODE, key);
                return c;
            } catch (KeyPermanentlyInvalidatedException e) {
                // User enrolled new biometrics since the probe key was created.
                // Delete it and retry once -- the next iteration recreates it.
                deleteProbeKey();
            } catch (Throwable t) {
                Log.e(t);
                completeError(result, BiometricError.UNKNOWN,
                        "Failed to initialise biometric probe cipher: " + t.getMessage());
                return null;
            }
        }
        completeError(result, BiometricError.KEY_REVOKED,
                "Biometric probe key permanently invalidated");
        return null;
    }

    private void createProbeKey() {
        try {
            KeyGenParameterSpec.Builder b = new KeyGenParameterSpec.Builder(PROBE_KEY_ID,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true);
            // Invalidate the probe key whenever the user enrols a new biometric;
            // this is the security property CodeQL is asking us to enforce.
            if (Build.VERSION.SDK_INT >= 24) {
                b.setInvalidatedByBiometricEnrollment(true);
            }
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
            kg.init(b.build());
            kg.generateKey();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private void deleteProbeKey() {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
            ks.load(null);
            ks.deleteEntry(PROBE_KEY_ID);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Performs the actual cryptographic operation on the biometric-unlocked
    /// Cipher. Returning `true` proves the user really authenticated; throws
    /// indicate the success callback was reached without a valid biometric
    /// unlock and the call must be rejected.
    private boolean verifyProbeCipher(Cipher authedCipher) {
        if (authedCipher == null) {
            return false;
        }
        try {
            authedCipher.doFinal(PROBE_PLAINTEXT);
            return true;
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    void completeSuccess(final AsyncResource<Boolean> result) {
        if (pending != result) {
            return;
        }
        pending = null;
        Display.getInstance().callSerially(new CompleteSuccessRunnable(result));
    }

    private static final class CompleteSuccessRunnable implements Runnable {
        private final AsyncResource<Boolean> result;
        CompleteSuccessRunnable(AsyncResource<Boolean> result) { this.result = result; }
        @Override
        public void run() {
            if (!result.isDone()) {
                result.complete(Boolean.TRUE);
            }
        }
    }

    void completeError(final AsyncResource<Boolean> result,
                       final BiometricError err, final String msg) {
        if (pending != result) {
            return;
        }
        pending = null;
        Display.getInstance().callSerially(new CompleteErrorRunnable(result, err, msg));
    }

    private static final class CompleteErrorRunnable implements Runnable {
        private final AsyncResource<Boolean> result;
        private final BiometricError err;
        private final String msg;
        CompleteErrorRunnable(AsyncResource<Boolean> result, BiometricError err, String msg) {
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

    /// Translates an Android biometric error code into the portable
    /// [BiometricError]. One mapping for both backends: the `FINGERPRINT_ERROR_`
    /// and `BIOMETRIC_ERROR_` constants are the same numbers in AOSP.
    static BiometricError mapBiometricError(int code) {
        switch (code) {
            case ERROR_HW_UNAVAILABLE:
            case ERROR_HW_NOT_PRESENT:
                return BiometricError.NOT_AVAILABLE;
            case ERROR_UNABLE_TO_PROCESS:
                // Also what the legacy backend reports once a finger has
                // touched the sensor too many times without being recognised;
                // FingerprintManager has no dedicated code for that.
                return BiometricError.AUTHENTICATION_FAILED;
            case ERROR_LOCKOUT:
                return BiometricError.LOCKED_OUT;
            case ERROR_LOCKOUT_PERMANENT:
                return BiometricError.PERMANENTLY_LOCKED_OUT;
            case ERROR_NO_BIOMETRICS:
                return BiometricError.NOT_ENROLLED;
            case ERROR_USER_CANCELED:
            case ERROR_NEGATIVE_BUTTON:
                return BiometricError.USER_CANCELED;
            case ERROR_CANCELED:
                return BiometricError.SYSTEM_CANCELED;
            case ERROR_NO_DEVICE_CREDENTIAL:
                return BiometricError.PASSCODE_NOT_SET;
            default:
                return BiometricError.UNKNOWN;
        }
    }

    /// The backend for this device, or `null` when neither package survived
    /// into this build.
    ///
    /// Both are loaded by name because the modern one cannot be linked: it is
    /// excluded from the port jar compile, and deleted from a generated
    /// application whose `compileSdk` is below 30. The fallback to the legacy
    /// backend is what covers such a build, and the legacy backend is loaded
    /// the same way for symmetry rather than necessity.
    private static BiometricBackend resolveBackend() {
        Object instance = null;
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                // The class name is a literal AT the Class.forName call, not a
                // parameter threaded into a shared helper. R8 keeps a class
                // named by a constant string there and cannot see one that
                // arrives through a variable, and a release build renaming
                // these would leave the lookup failing with no biometrics and
                // no error -- the same shape as the absence this catch treats
                // as normal.
                instance = Class.forName(
                        "com.codename1.impl.android.biometrics.BiometricPromptBackend")
                        .newInstance();
            } catch (Throwable absent) {
                // Only reachable on a build compiled below API 28, where
                // BiometricPrompt does not exist and the builder deleted the
                // package. Nothing the builder generates goes there, so in
                // practice every device from API 29 up -- 37 included -- is
                // served by BiometricPrompt.
                instance = null;
            }
        }
        if (!(instance instanceof BiometricBackend) && Build.VERSION.SDK_INT >= 23) {
            try {
                instance = Class.forName(
                        "com.codename1.impl.android.fingerprint.FingerprintBackend")
                        .newInstance();
            } catch (Throwable absent) {
                // Not expected: the legacy backend goes through
                // FingerprintManagerCompat and so compiles against every
                // platform, and nothing deletes it. Kept because the load is
                // by name and a rename or a stripped package must degrade
                // rather than throw out of a static initialiser.
                instance = null;
            }
        }
        // Tested rather than cast inside the catch. A failed cast does not
        // throw under ParparVM, so a catch around one is a handler that never
        // runs, and scripts/check-cast-semantics.sh holds the whole tree --
        // Android sources included -- to the guarded shape.
        return instance instanceof BiometricBackend
                ? (BiometricBackend) instance : null;
    }

    @Override
    public boolean stopAuthentication() {
        final AsyncResource<Boolean> p = pending;
        if (p == null) {
            return false;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (cancellationSignal != null) {
                    cancellationSignal.cancel();
                    cancellationSignal = null;
                }
            }
        });
        completeError(p, BiometricError.USER_CANCELED, "Authentication cancelled by app");
        return true;
    }

    /// The resolved backend, or `null` when this device and this build have
    /// none. Shared with [AndroidSecureStorage], which prompts with the same
    /// API for a different cipher.
    static BiometricBackend backend() {
        return BACKEND;
    }

    static void runOnUi(Runnable r) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            r.run();
        } else {
            AndroidNativeUtil.getActivity().runOnUiThread(r);
        }
    }
}
