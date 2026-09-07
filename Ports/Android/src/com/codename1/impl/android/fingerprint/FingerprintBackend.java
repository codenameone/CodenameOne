/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android.fingerprint;

import android.app.Activity;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import com.codename1.impl.android.BiometricBackend;

import javax.crypto.Cipher;

/// The API 23-28 half of [BiometricBackend].
///
/// #### Why this goes through the support library
///
/// The platform class it stands on, `android.hardware.fingerprint
/// .FingerprintManager`, was deprecated in API 28 and **removed in API 37**,
/// along with `Context.FINGERPRINT_SERVICE`. Naming it from a file that every
/// generated application compiles is what broke issue #5701: a Hello World
/// generated against an API 37 platform failed `compileDebugJavaWithJavac` on
/// port sources the developer never wrote.
///
/// `FingerprintManagerCompat` solves that without giving anything up. The
/// support library ships the type itself, so this file compiles against every
/// platform Codename One supports -- 37 included -- while still calling the
/// platform API underneath on the devices that have it. That matters because
/// an application compiled against API 37 still *runs* on API 23-28, where this
/// is the only biometric API there is; deleting the file for a modern
/// `compileSdk` would have taken fingerprint away from those devices.
///
/// The `android.support.v4` spelling is deliberate and is what the other ten
/// support-library users in this port write. The port jar compiles it against
/// cn1-binaries' `support-compat-v4` jar, and `AndroidGradleBuilder` rewrites
/// it to `androidx.core.hardware.fingerprint.FingerprintManagerCompat` for
/// every AndroidX application through `androidx-class-mapping.csv`.
///
/// Reflection would not have been an option: `FingerprintManagerCompat
/// .AuthenticationCallback` is an abstract *class*, and
/// `java.lang.reflect.Proxy` implements interfaces only. That mistake is
/// exactly what made the old `BiometricsApi29` inert on every device.
public final class FingerprintBackend implements BiometricBackend {

    /// The API level that removed `android.hardware.fingerprint`.
    ///
    /// The compat class is a *compile-time* shield, not a run-time one. Its
    /// `isHardwareDetected` and friends are guarded by
    /// `SDK_INT >= 23` with no upper bound, and behind that guard they ask for
    /// the platform `FingerprintManager` by class -- so on a device that no
    /// longer has one, the first call raises `NoClassDefFoundError` rather than
    /// returning false. Letting it run is therefore not an option; answering
    /// "no fingerprint hardware" before touching it is what keeps that off
    /// every caller's path.
    ///
    /// Nothing is lost by it. The modern backend compiles down to `compileSdk`
    /// 28 and so is present in every build the builder generates, which means
    /// an API 37 device is served by `BiometricPrompt` and never arrives here.
    private static final int FINGERPRINT_REMOVED_IN_SDK = 37;

    /// The number of soft failures -- a finger that touched the sensor and was
    /// not recognised -- tolerated before the call is failed. `FingerprintManager`
    /// draws no UI of its own, so unlike `BiometricPrompt` there is nothing on
    /// screen to give up on its own.
    private static final int MAX_SOFT_FAILURES = 5;

    /// `FINGERPRINT_ERROR_HW_UNAVAILABLE`, `FINGERPRINT_ERROR_NO_FINGERPRINTS`
    /// and `FINGERPRINT_ERROR_UNABLE_TO_PROCESS`. Inlined because the class that
    /// declares them is the one this file exists to avoid naming; the values are
    /// AOSP constants and have never moved.
    private static final int HW_UNAVAILABLE = 1;
    private static final int NOT_RECOGNIZED = 2;
    private static final int NO_FINGERPRINTS = 11;

    /// Loaded reflectively by `AndroidBiometrics.backend()`.
    public FingerprintBackend() {
    }

    private static FingerprintManagerCompat manager(Activity activity) {
        if (Build.VERSION.SDK_INT >= FINGERPRINT_REMOVED_IN_SDK) {
            return null;
        }
        return FingerprintManagerCompat.from(activity);
    }

    @Override
    public boolean canAuthenticate(Activity activity) {
        FingerprintManagerCompat fpm = manager(activity);
        return fpm != null && fpm.isHardwareDetected()
                && fpm.hasEnrolledFingerprints();
    }

    @Override
    public boolean hasEnrolledFingerprints(Activity activity) {
        FingerprintManagerCompat fpm = manager(activity);
        return fpm != null && fpm.hasEnrolledFingerprints();
    }

    @Override
    public void authenticate(Activity activity, String title, String subtitle,
                             String description, String negativeButton,
                             Cipher cipher, CancellationSignal cancel,
                             final Callback callback) {
        // title/subtitle/description/negativeButton are deliberately unused:
        // FingerprintManager has no system-drawn prompt, so the copy has
        // nowhere to go. BiometricPrompt, which does draw one, is what every
        // device from API 29 gets.
        FingerprintManagerCompat fpm = manager(activity);
        if (fpm == null || !fpm.isHardwareDetected()) {
            callback.onError(HW_UNAVAILABLE, "No fingerprint hardware");
            return;
        }
        if (!fpm.hasEnrolledFingerprints()) {
            callback.onError(NO_FINGERPRINTS, "No fingerprints enrolled");
            return;
        }
        fpm.authenticate(new FingerprintManagerCompat.CryptoObject(cipher), 0,
                bridgeCancellation(cancel),
                new FingerprintManagerCompat.AuthenticationCallback() {
                    private int failures;

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        callback.onError(errorCode,
                                errString == null ? "" : errString.toString());
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult r) {
                        FingerprintManagerCompat.CryptoObject crypto = r.getCryptoObject();
                        callback.onSuccess(crypto == null ? null : crypto.getCipher());
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        if (failures++ > MAX_SOFT_FAILURES) {
                            callback.onError(NOT_RECOGNIZED, "Authentication failed");
                        }
                    }
                }, null);
    }

    /// Follows the caller's `android.os.CancellationSignal` with the support
    /// library one this API takes.
    ///
    /// The two are separate types and the old support-library jar has no
    /// overload for the platform one, so the cancel has to be forwarded. A
    /// signal that is already cancelled invokes the listener immediately, which
    /// is what makes this safe to install after the fact.
    private static android.support.v4.os.CancellationSignal bridgeCancellation(
            CancellationSignal cancel) {
        final android.support.v4.os.CancellationSignal compat =
                new android.support.v4.os.CancellationSignal();
        if (cancel != null) {
            cancel.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                @Override
                public void onCancel() {
                    compat.cancel();
                }
            });
        }
        return compat;
    }
}
