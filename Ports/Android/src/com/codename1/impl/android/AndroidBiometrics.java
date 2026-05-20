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
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Looper;

import com.codename1.io.Log;
import com.codename1.security.AuthenticationOptions;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.BiometricType;
import com.codename1.security.Biometrics;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Android backing for {@link Biometrics}. Uses
 * {@code BiometricPrompt} on API 29+ (via reflection &mdash; the cn1-binaries
 * android.jar predates API 28 so direct calls would not compile) and the
 * legacy {@code FingerprintManager} on API 23-28. Mirrors the dual-path
 * behaviour of the historical {@code FingerprintScanner} cn1lib but completes
 * per-call {@link AsyncResource} instances instead of a shared static
 * callback.
 *
 * <p>FingerprintManager error codes documented at
 * <a href="https://developer.android.com/reference/android/hardware/fingerprint/FingerprintManager">developer.android.com</a>;
 * the constants missing from the compile-time android.jar are inlined below.</p>
 */
public final class AndroidBiometrics extends Biometrics {

    // FingerprintManager constants not in the cn1-binaries android.jar.
    private static final int FINGERPRINT_ERROR_NO_FINGERPRINTS = 11;
    private static final int FINGERPRINT_ERROR_HW_NOT_PRESENT = 12;

    // BiometricPrompt error codes (API 28+) -- values are stable per AOSP.
    static final int BIOMETRIC_ERROR_HW_UNAVAILABLE = 1;
    static final int BIOMETRIC_ERROR_HW_NOT_PRESENT = 12;
    static final int BIOMETRIC_ERROR_LOCKOUT = 7;
    static final int BIOMETRIC_ERROR_LOCKOUT_PERMANENT = 9;
    static final int BIOMETRIC_ERROR_NO_BIOMETRICS = 11;
    static final int BIOMETRIC_ERROR_USER_CANCELED = 10;
    static final int BIOMETRIC_ERROR_NEGATIVE_BUTTON = 13;
    static final int BIOMETRIC_ERROR_CANCELED = 5;
    static final int BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL = 14;

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
        runOnUi(new Runnable() {
            @Override
            public void run() {
                try {
                    Activity act = AndroidNativeUtil.getActivity();
                    PackageManager pm = act.getPackageManager();
                    boolean okBio = false;
                    if (Build.VERSION.SDK_INT >= 29) {
                        if (!AndroidNativeUtil.checkForPermission("android.permission.USE_BIOMETRIC",
                                "Authorize using biometrics")) {
                            return;
                        }
                        okBio = BiometricsApi29.canAuthenticate(act);
                    } else {
                        if (!AndroidNativeUtil.checkForPermission(Manifest.permission.USE_FINGERPRINT,
                                "Authorize using fingerprint")) {
                            return;
                        }
                        FingerprintManager fpm = (FingerprintManager)
                                act.getSystemService(Activity.FINGERPRINT_SERVICE);
                        okBio = fpm != null && fpm.isHardwareDetected()
                                && fpm.hasEnrolledFingerprints();
                    }
                    if (!okBio) {
                        return;
                    }
                    if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
                        FingerprintManager fpm = (FingerprintManager)
                                act.getSystemService(Activity.FINGERPRINT_SERVICE);
                        if (fpm != null && fpm.hasEnrolledFingerprints()) {
                            out.add(BiometricType.FINGERPRINT);
                        }
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
        });
        return out;
    }

    @Override
    public AsyncResource<Boolean> authenticate(final AuthenticationOptions opts) {
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        if (Build.VERSION.SDK_INT < 23) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Android API 23 (Marshmallow) required for biometric authentication"));
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
        if (Build.VERSION.SDK_INT >= 29) {
            runOnUi(new Runnable() {
                @Override
                public void run() {
                    if (cancellationSignal != null) {
                        cancellationSignal.cancel();
                    }
                    cancellationSignal = new CancellationSignal();
                    BiometricsApi29.authenticate(AndroidNativeUtil.getActivity(),
                            title, subtitle, description, negative,
                            cancellationSignal,
                            new BiometricsApi29.AuthCallback() {
                                @Override
                                public void onSuccess() {
                                    completeSuccess(result);
                                }

                                @Override
                                public void onError(int code, String msg) {
                                    completeError(result, mapBiometricPromptError(code), msg);
                                }
                            });
                }
            });
        } else {
            authenticateLegacy(result);
        }
        return result;
    }

    private void authenticateLegacy(final AsyncResource<Boolean> result) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                if (!AndroidNativeUtil.checkForPermission(Manifest.permission.USE_FINGERPRINT,
                        "Authorize using fingerprint")) {
                    completeError(result, BiometricError.NOT_AVAILABLE,
                            "USE_FINGERPRINT permission denied");
                    return;
                }
                FingerprintManager fpm = (FingerprintManager)
                        AndroidNativeUtil.getActivity()
                                .getSystemService(Activity.FINGERPRINT_SERVICE);
                if (fpm == null || !fpm.isHardwareDetected()) {
                    completeError(result, BiometricError.NOT_AVAILABLE,
                            "No fingerprint hardware");
                    return;
                }
                if (!fpm.hasEnrolledFingerprints()) {
                    completeError(result, BiometricError.NOT_ENROLLED, "No fingerprints enrolled");
                    return;
                }
                if (cancellationSignal != null) {
                    cancellationSignal.cancel();
                }
                final CancellationSignal cs = new CancellationSignal();
                cancellationSignal = cs;
                FingerprintManager.AuthenticationCallback cb = new FingerprintManager.AuthenticationCallback() {
                    int failures;

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        completeError(result, mapFingerprintManagerError(errorCode),
                                errString == null ? "" : errString.toString());
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult r) {
                        cs.cancel();
                        completeSuccess(result);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        if (failures++ > 5) {
                            cs.cancel();
                            completeError(result, BiometricError.AUTHENTICATION_FAILED,
                                    "Authentication failed");
                        }
                    }
                };
                fpm.authenticate(null, cs, 0, cb, null);
            }
        });
    }

    void completeSuccess(final AsyncResource<Boolean> result) {
        if (pending != result) {
            return;
        }
        pending = null;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!result.isDone()) {
                    result.complete(Boolean.TRUE);
                }
            }
        });
    }

    void completeError(final AsyncResource<Boolean> result,
                       final BiometricError err, final String msg) {
        if (pending != result) {
            return;
        }
        pending = null;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!result.isDone()) {
                    result.error(new BiometricException(err, msg));
                }
            }
        });
    }

    static BiometricError mapBiometricPromptError(int code) {
        switch (code) {
            case BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BIOMETRIC_ERROR_HW_NOT_PRESENT:
                return BiometricError.NOT_AVAILABLE;
            case BIOMETRIC_ERROR_LOCKOUT:
                return BiometricError.LOCKED_OUT;
            case BIOMETRIC_ERROR_LOCKOUT_PERMANENT:
                return BiometricError.PERMANENTLY_LOCKED_OUT;
            case BIOMETRIC_ERROR_NO_BIOMETRICS:
                return BiometricError.NOT_ENROLLED;
            case BIOMETRIC_ERROR_USER_CANCELED:
            case BIOMETRIC_ERROR_NEGATIVE_BUTTON:
                return BiometricError.USER_CANCELED;
            case BIOMETRIC_ERROR_CANCELED:
                return BiometricError.SYSTEM_CANCELED;
            case BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL:
                return BiometricError.PASSCODE_NOT_SET;
            default:
                return BiometricError.UNKNOWN;
        }
    }

    static BiometricError mapFingerprintManagerError(int code) {
        switch (code) {
            case FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE:
            case FINGERPRINT_ERROR_HW_NOT_PRESENT:
                return BiometricError.NOT_AVAILABLE;
            case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                return BiometricError.LOCKED_OUT;
            case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT:
                return BiometricError.PERMANENTLY_LOCKED_OUT;
            case FINGERPRINT_ERROR_NO_FINGERPRINTS:
                return BiometricError.NOT_ENROLLED;
            case FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED:
                return BiometricError.USER_CANCELED;
            case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                return BiometricError.SYSTEM_CANCELED;
            default:
                return BiometricError.UNKNOWN;
        }
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

    static void runOnUi(Runnable r) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            r.run();
        } else {
            AndroidNativeUtil.getActivity().runOnUiThread(r);
        }
    }
}
