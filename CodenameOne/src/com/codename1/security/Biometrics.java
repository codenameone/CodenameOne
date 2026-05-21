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
/// - **Android** -- uses `BiometricPrompt` on API 29+ (Android 10) and the
///   legacy `FingerprintManager` on API 23-28. Fingerprint, face, and iris
///   modalities are reported per `PackageManager` features.
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

    /// Lists the biometric modalities currently enrolled. On iOS this is
    /// [BiometricType#FINGERPRINT] or [BiometricType#FACE]; on Android the
    /// list may contain [BiometricType#IRIS] as well, and Android API 30+
    /// adds [BiometricType#STRONG] / [BiometricType#WEAK] authenticator
    /// class tags.
    ///
    /// #### Returns
    ///
    /// an empty list when nothing is enrolled or the device is unsupported
    public List<BiometricType> getAvailableBiometrics() {
        return Collections.emptyList();
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
