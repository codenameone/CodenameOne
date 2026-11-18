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
package com.codename1.security;

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.Collections;
import java.util.List;

/// Entry point for biometric authentication (Touch ID, Face ID, fingerprint,
/// Android `BiometricPrompt`). Obtain the platform implementation via
/// [#getInstance()]; the returned subclass is owned by the active port.
///
/// A typical unlock flow:
///
/// ```java
/// Biometrics b = Biometrics.getInstance();
/// if (!b.canAuthenticate()) {
///     // Fall back to password
///     return;
/// }
/// b.authenticate("Unlock your account").onResult((success, err) -> {
///     if (err != null) {
///         BiometricError code = ((BiometricException) err).getError();
///         // branch on code
///     } else {
///         // success
///     }
/// });
/// ```
///
/// [#authenticate(AuthenticationOptions)] returns an `AsyncResource` whose
/// failure path completes with a [BiometricException] so callers can branch
/// on the typed [BiometricError].
///
/// #### Platform support
///
/// - **iOS** -- uses `LocalAuthentication.framework` (`LAContext`). Touch ID
///   and Face ID on supported devices. Add the `ios.NSFaceIDUsageDescription`
///   build hint when targeting Face ID hardware.
/// - **Android** -- uses `BiometricPrompt` on API 29+ (Android 10) and
///   `FingerprintManagerCompat` on API 23-28. Fingerprint, face, and iris
///   modalities are reported per `PackageManager` features. The legacy path
///   goes through the support library rather than
///   `android.hardware.fingerprint` directly, which Android removed in API 37,
///   so an app compiled against any platform still authenticates on an API
///   23-28 device.
/// - **JavaSE simulator** -- behaves as a real device with no enrolled
///   biometrics by default. The `Simulate -> Biometric Simulation` submenu
///   in the simulator lets you toggle hardware availability, enrolled
///   modalities, and the outcome of the next [#authenticate(String)] call.
/// - **All other platforms (desktop deploy, JavaScript, ...)** -- this base
///   class is returned as-is and acts as a non-supporting fallback:
///   [#isSupported()] / [#canAuthenticate()] return `false` and
///   [#authenticate(String)] completes with [BiometricError#NOT_AVAILABLE].
///   Application code does not need platform `if` statements -- always
///   gate biometrics on [#canAuthenticate()] before invoking the prompt.
public class Biometrics {

    /// Subclasses are constructed by the port. Application code obtains the
    /// active instance via [#getInstance()].
    protected Biometrics() {
    }

    /// Returns the platform-specific singleton owned by the current port.
    /// On ports that do not implement biometrics this returns a base
    /// [Biometrics] instance whose methods report the device as
    /// unsupported, so calling code never needs a `null` check or a
    /// platform-specific `if`.
    public static Biometrics getInstance() {
        Biometrics b = Display.getInstance().getBiometrics();
        return b != null ? b : DEFAULT;
    }

    private static final Biometrics DEFAULT = new Biometrics();

    /// Returns `true` when biometric hardware exists on the device,
    /// regardless of whether the user has enrolled biometrics. Combine with
    /// [#canAuthenticate()] to gate UI affordances: show the "Use
    /// biometrics" toggle when `isSupported()` is true, but only invoke
    /// [#authenticate(AuthenticationOptions)] when `canAuthenticate()` is
    /// also true. Returns `false` on the fallback base class.
    public boolean isSupported() {
        return false;
    }

    /// Returns `true` when the device is ready to authenticate right now:
    /// hardware present, at least one biometric enrolled, and not in a
    /// locked-out state. Returns `false` on the fallback base class.
    public boolean canAuthenticate() {
        return false;
    }

    /// Lists the biometric modalities the device can authenticate with. On
    /// iOS this is the enrolled one, [BiometricType#FINGERPRINT] or
    /// [BiometricType#FACE]. On Android 10 and later it is every modality
    /// the device has hardware for, once any biometric is enrolled: Android
    /// does not report which one the user enrolled, and the list may
    /// contain [BiometricType#IRIS] there. Windows and Linux do not learn
    /// the modality and report [BiometricType#STRONG] alone.
    ///
    /// To choose the wording of the application's own UI use
    /// [#getPromptType()], which reduces this list to one answer.
    ///
    /// #### Returns
    ///
    /// an empty list when nothing is enrolled or the device is unsupported
    public List<BiometricType> getAvailableBiometrics() {
        return Collections.emptyList();
    }

    /// Answers what the application should call biometric authentication on
    /// this device: a fingerprint, the face, or biometrics in general when
    /// the device cannot say which. Use it to choose the wording and icon of
    /// the application's own UI; it needs no platform or OS version check.
    ///
    /// ```java
    /// String label;
    /// switch (Biometrics.getInstance().getPromptType()) {
    ///     case FINGERPRINT: label = "Sign in with fingerprint"; break;
    ///     case FACE: label = "Sign in with face"; break;
    ///     case BIOMETRICS: label = "Sign in with biometrics"; break;
    ///     default: label = null; // NONE: offer the password only
    /// }
    /// ```
    ///
    /// A single modality is named, and anything else that can authenticate
    /// is [BiometricPromptType#BIOMETRICS]:
    ///
    /// - **iOS** names Touch ID or Face ID; any other sensor is generic.
    /// - **Android** reports that some biometric is enrolled and not which,
    ///   so a device with more than one kind of biometric hardware is
    ///   generic. A device with one kind is named.
    /// - **Windows and Linux** are always generic: Windows Hello does not
    ///   say what it verifies with, and that may be a PIN.
    ///
    /// This asks the platform each time it is called, which on some ports
    /// waits for another thread or process; on Android, [#canAuthenticate()]
    /// and [#getAvailableBiometrics()] wait for the UI thread as well. Call
    /// it when building a screen and keep the answer, not once per paint.
    ///
    /// #### Returns
    ///
    /// [BiometricPromptType#NONE] when [#canAuthenticate()] is `false`
    public BiometricPromptType getPromptType() {
        if (!canAuthenticate()) {
            return BiometricPromptType.NONE;
        }
        return promptTypeOf(getAvailableBiometrics());
    }

    /// The answer of [#getPromptType()] for a device that can authenticate
    /// with the given modalities. A port that learns both facts from one
    /// platform query overrides [#getPromptType()] and calls this, so the
    /// rule stays in one place.
    ///
    /// #### Parameters
    ///
    /// - `types`: what [#getAvailableBiometrics()] reported, may be `null`
    ///
    /// #### Returns
    ///
    /// never [BiometricPromptType#NONE]
    protected static BiometricPromptType promptTypeOf(List<BiometricType> types) {
        // STRONG and WEAK are authenticator classes, not modalities, so they
        // do not count. An iris has no wording of its own and is generic.
        BiometricPromptType found = null;
        int size = types == null ? 0 : types.size();
        for (int i = 0; i < size; i++) {
            BiometricType t = types.get(i);
            BiometricPromptType p = null;
            if (t == BiometricType.FINGERPRINT) {
                p = BiometricPromptType.FINGERPRINT;
            } else if (t == BiometricType.FACE) {
                p = BiometricPromptType.FACE;
            } else if (t == BiometricType.IRIS) {
                p = BiometricPromptType.BIOMETRICS;
            }
            if (p != null) {
                if (found != null && found != p) {
                    return BiometricPromptType.BIOMETRICS;
                }
                found = p;
            }
        }
        return found == null ? BiometricPromptType.BIOMETRICS : found;
    }

    /// Prompts the user to authenticate. The returned `AsyncResource`
    /// completes with `true` on success, or with a [BiometricException] on
    /// failure (consult [BiometricException#getError()] for the typed code).
    /// On the fallback base class this completes immediately with
    /// [BiometricError#NOT_AVAILABLE] so callers don't need to platform-
    /// check before invoking.
    ///
    /// #### Parameters
    ///
    /// - `opts`: non-null configuration; [AuthenticationOptions#setReason(String)]
    ///   should be set
    public AsyncResource<Boolean> authenticate(AuthenticationOptions opts) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                "Biometric authentication is not available on this platform"));
        return r;
    }

    /// Convenience for `authenticate(new AuthenticationOptions().setReason(reason))`.
    public AsyncResource<Boolean> authenticate(String reason) {
        return authenticate(new AuthenticationOptions().setReason(reason));
    }

    /// Cancels an in-flight [#authenticate(AuthenticationOptions)] call if
    /// one is running. The pending `AsyncResource` completes with
    /// [BiometricError#USER_CANCELED].
    ///
    /// #### Returns
    ///
    /// `true` when a call was cancelled; `false` when no authentication was
    /// pending. Always `false` on the fallback base class.
    public boolean stopAuthentication() {
        return false;
    }
}
