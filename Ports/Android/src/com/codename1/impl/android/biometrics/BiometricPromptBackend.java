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
package com.codename1.impl.android.biometrics;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.CancellationSignal;

import com.codename1.impl.android.BiometricBackend;
import com.codename1.io.Log;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;

/// The API 29+ half of [BiometricBackend], written against
/// `android.hardware.biometrics` with no reflection at all.
///
/// #### Why the package is separate
///
/// `BiometricPrompt` is API 28 and the `android.jar` the Android port jar
/// compiles against is API 27, so this file is excluded from that compile (see
/// `maven/android/pom.xml`, next to the `ar`, `ai`, `cipher` and `nearby`
/// exclusions) and is compiled inside the generated application instead.
///
/// API 28 is the whole of its floor, which is also the lowest `compileSdk` the
/// builder generates. Everything newer that it needs -- `BiometricManager` is
/// API 29, `canAuthenticate(int)` and `Authenticators` are API 30 -- is reached
/// by name in [#canAuthenticate] rather than compiled against, so no realistic
/// build has to do without this backend. That matters more than it looks:
/// while the floor was 30, a project pinned to 28 or 29 lost the file, and its
/// APK then had no biometrics at all on an API 37 device, where the legacy
/// backend's platform API no longer exists.
///
/// It used to be reached by reflection from a single always-compiled file, and
/// that did not work: `BiometricPrompt.AuthenticationCallback` is an abstract
/// **class**, and `java.lang.reflect.Proxy` refuses anything that is not an
/// interface, so every call ended in `IllegalArgumentException` and the caller
/// was told the hardware was unavailable. A real subclass is the only way to
/// pass that callback, and a real subclass needs the type at compile time.
public final class BiometricPromptBackend implements BiometricBackend {

    /// `BiometricManager.BIOMETRIC_SUCCESS`, which is API 29 and so cannot be
    /// named from anything compiled against the port's own `android.jar`. It is
    /// zero and has always been zero.
    private static final int BIOMETRIC_SUCCESS = 0;

    /// `Context.BIOMETRIC_SERVICE`, which is API 29 and so cannot be named
    /// here. It is "biometric" and has always been "biometric".
    private static final String BIOMETRIC_SERVICE = "biometric";

    /// `BiometricManager.Authenticators.BIOMETRIC_STRONG`, the Class 3 tier.
    /// API 30, so inlined like the rest; 15 is the AOSP value.
    private static final int BIOMETRIC_STRONG = 15;

    /// `BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED` and
    /// `BIOMETRIC_ERROR_HW_UNAVAILABLE`. Inlined rather than named: they became
    /// public fields on `BiometricPrompt` only in API 29, so naming them would
    /// tie this file to a floor it does not otherwise need. The values are AOSP
    /// constants and have never moved.
    private static final int ERROR_USER_CANCELED = 10;
    private static final int ERROR_HW_UNAVAILABLE = 1;

    /// Loaded reflectively by `AndroidBiometrics.backend()`.
    public BiometricPromptBackend() {
    }

    /// Whether a **Class 3** biometric can authenticate right now.
    ///
    /// Class 3 and not merely "any biometric", because every caller in the port
    /// authenticates with a `CryptoObject`, and `BiometricPrompt.authenticate`
    /// defaults a crypto prompt to `Authenticators.BIOMETRIC_STRONG` and
    /// rejects anything weaker outright ("Only Strong biometrics supported with
    /// crypto"). The deprecated no-argument `canAuthenticate()` is defined by
    /// AOSP as `canAuthenticate(Authenticators.BIOMETRIC_WEAK)`, so on an API
    /// 30+ device with only a Class 2 face enrolled it answers success and the
    /// prompt then cannot possibly succeed --
    /// [com.codename1.security.Biometrics#canAuthenticate] would promise
    /// something [#authenticate] always fails.
    ///
    /// API 29 falls back to the no-argument call: the tiers do not exist
    /// there, so it is the only query that platform has. The fallback is driven
    /// by the method being absent rather than by a version check, which is the
    /// same question asked directly.
    ///
    /// `BiometricManager` is reached by name because it is API 29 and this file
    /// is held to 28. That is safe here and was not for the callback below:
    /// this is a plain method on a concrete class, whereas
    /// `BiometricPrompt.AuthenticationCallback` is an abstract *class* that
    /// `java.lang.reflect.Proxy` cannot implement -- the mistake that made the
    /// old `BiometricsApi29` inert on every device.
    @Override
    public boolean canAuthenticate(Activity activity) {
        Object manager = activity.getSystemService(BIOMETRIC_SERVICE);
        if (manager == null) {
            return false;
        }
        Object result;
        try {
            Method strong = manager.getClass()
                    .getMethod("canAuthenticate", int.class);
            result = strong.invoke(manager, Integer.valueOf(BIOMETRIC_STRONG));
        } catch (NoSuchMethodException api29) {
            result = canAuthenticateAny(manager);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
        // Tested rather than cast inside the catch: a failed cast does not
        // throw under ParparVM, and scripts/check-cast-semantics.sh holds the
        // whole tree -- Android sources included -- to the guarded shape.
        return result instanceof Integer
                && ((Integer) result).intValue() == BIOMETRIC_SUCCESS;
    }

    /// `BiometricManager.canAuthenticate()`, the API 29 query, for the one
    /// platform that has it and not the authenticator-aware overload.
    private static Object canAuthenticateAny(Object manager) {
        try {
            return manager.getClass().getMethod("canAuthenticate")
                    .invoke(manager);
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    /// Android exposes no per-modality enrolment query, so this answers
    /// "fingerprint hardware exists and a usable biometric is enrolled", with
    /// [#canAuthenticate] deciding what usable means.
    ///
    /// The legacy backend could be exact, because `FingerprintManager` only
    /// ever knew about fingerprints. From API 29 the honest options are this or
    /// nothing, and reporting nothing would make
    /// [com.codename1.security.Biometrics#getAvailableBiometrics] empty on
    /// every fingerprint-only phone.
    @Override
    public boolean hasEnrolledFingerprints(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                && canAuthenticate(activity);
    }

    @Override
    public void authenticate(Activity activity, String title, String subtitle,
                             String description, String negativeButton,
                             Cipher cipher, CancellationSignal cancel,
                             final Callback callback) {
        try {
            BiometricPrompt.Builder b = new BiometricPrompt.Builder(activity);
            b.setTitle(title);
            if (subtitle != null) {
                b.setSubtitle(subtitle);
            }
            if (description != null) {
                b.setDescription(description);
            }
            Executor exec = activity.getMainExecutor();
            b.setNegativeButton(negativeButton, exec,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            callback.onError(ERROR_USER_CANCELED,
                                    "Cancelled");
                        }
                    });
            b.build().authenticate(new BiometricPrompt.CryptoObject(cipher),
                    cancel, exec, new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                            BiometricPrompt.CryptoObject crypto = r.getCryptoObject();
                            callback.onSuccess(crypto == null ? null : crypto.getCipher());
                        }

                        @Override
                        public void onAuthenticationError(int code, CharSequence err) {
                            callback.onError(code, err == null ? "" : err.toString());
                        }

                        // onAuthenticationFailed and onAuthenticationHelp are
                        // the soft-failure stream: the prompt stays up and
                        // retries, and ends in onAuthenticationError when it
                        // gives up. Completing the AsyncResource on them would
                        // fail the call while the user is still trying.
                    });
        } catch (Throwable t) {
            Log.e(t);
            callback.onError(ERROR_HW_UNAVAILABLE,
                    "Failed to show the biometric prompt: " + t.getMessage());
        }
    }
}
