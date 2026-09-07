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
package com.codename1.impl.android;

import android.app.Activity;
import android.os.CancellationSignal;

import javax.crypto.Cipher;

/// The one biometric operation set Codename One needs from Android, expressed
/// so that neither of its two implementations has to be visible to the code
/// that calls it.
///
/// #### Why this exists
///
/// Android has shipped two mutually exclusive biometric APIs and the port has
/// to compile against a range of SDKs that contains neither of them in full:
///
/// - `android.hardware.fingerprint.FingerprintManager` arrived in API 23 and
///   was **removed in API 37**. An app generated against a 37 platform does
///   not compile if any source names it -- which is exactly what issue #5701
///   reported, in an unmodified Hello World. The legacy backend therefore goes
///   through the support library's `FingerprintManagerCompat`, which keeps the
///   API 23-28 devices an API 37 build still runs on.
/// - `android.hardware.biometrics.BiometricPrompt` arrived in API 28, which is
///   newer than the `android.jar` the port jar itself is compiled against.
///
/// Neither can be named from a file that has to compile everywhere, and
/// neither can be reached by `java.lang.reflect.Proxy` either, because the
/// callback both of them take is an abstract *class* and `Proxy` implements
/// interfaces only. So each lives in its own package -- `biometrics` for the
/// modern one, `fingerprint` for the legacy one -- with an implementation that
/// names its API at compile time, and `AndroidBiometrics.backend()` loads
/// whichever one this device and this build actually have. See
/// [AndroidNearbyBridge][com.codename1.impl.android.AndroidNearbyBridge] for
/// the same arrangement applied to a different missing dependency.
///
/// Error codes are the raw Android ones. The two APIs happen to number them
/// identically (`HW_UNAVAILABLE` 1 through `HW_NOT_PRESENT` 12), so
/// [AndroidBiometrics#mapBiometricError] is one mapping rather than two.
public interface BiometricBackend {

    /// Whether the device has usable, enrolled biometric hardware right now.
    boolean canAuthenticate(Activity activity);

    /// Whether at least one **fingerprint** is enrolled, as opposed to any
    /// other biometric modality. Used to answer
    /// [com.codename1.security.Biometrics#getAvailableBiometrics], which
    /// reports modalities separately.
    boolean hasEnrolledFingerprints(Activity activity);

    /// Prompts the user and, on success, hands back the same `cipher` with the
    /// keystore unlock applied to it.
    ///
    /// The cipher is not optional: every caller in the port passes a
    /// `CryptoObject`-backed one so that success can be proven by a real
    /// crypto operation rather than by the callback having fired. A hooked
    /// callback reaches a still-locked cipher and `doFinal` throws.
    ///
    /// #### Parameters
    ///
    /// - `title`, `subtitle`, `description`, `negativeButton`: prompt copy.
    ///   The legacy backend has no system-drawn prompt and ignores them.
    /// - `cancel`: cancels the prompt; the caller owns it.
    void authenticate(Activity activity, String title, String subtitle,
                      String description, String negativeButton, Cipher cipher,
                      CancellationSignal cancel, Callback callback);

    /// Completion of a single [BiometricBackend#authenticate] call. Exactly one
    /// method is invoked, on the UI thread.
    public interface Callback {

        /// The user authenticated and `authenticatedCipher` is unlocked.
        void onSuccess(Cipher authenticatedCipher);

        /// Authentication ended without success.
        ///
        /// #### Parameters
        ///
        /// - `code`: an Android biometric error code, mapped by
        ///   [AndroidBiometrics#mapBiometricError]
        void onError(int code, String message);
    }
}
