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

/// Enumerates the biometric authentication modalities that may be available
/// on a device. Returned from
/// [Biometrics#getAvailableBiometrics()].
///
/// The set returned at runtime depends on the platform and the user's
/// enrolled credentials -- e.g. an iPhone with Face ID enrolled returns
/// `[FACE]`; an Android Pixel with a fingerprint and a face enrolled
/// returns `[FINGERPRINT, FACE]`. Use the list to drive UI affordances
/// (icon, prompt copy) but never to gate the actual `authenticate()` call:
/// always rely on [Biometrics#canAuthenticate()] for that decision.
public enum BiometricType {

    /// Fingerprint sensor (iOS Touch ID, Android `FEATURE_FINGERPRINT`).
    /// Populated on both platforms when the device has fingerprint hardware
    /// AND at least one fingerprint is enrolled.
    FINGERPRINT,

    /// Face recognition (iOS Face ID, Android `FEATURE_FACE`). Populated on
    /// both platforms; on Android 9-10 the OS does not expose face
    /// enrolment via the BiometricPrompt API even on devices that have it,
    /// so this value only appears on Android API 29+.
    FACE,

    /// Iris recognition (Android `FEATURE_IRIS`). Used by a handful of
    /// Samsung devices and is not exposed on iOS. Practically rare in 2026
    /// -- code that targets it should still treat the absence as the
    /// expected case.
    IRIS,

    /// Android **class-3 / "strong"** authenticator tier. Indicates the
    /// device's available biometric meets Android's stricter cryptographic
    /// requirements (false-acceptance rate < 1/100,000) and can therefore
    /// gate Keystore-backed keys created with `setUserAuthenticationRequired`
    /// + a strong-only authenticator policy. Populated only on Android API
    /// 30+; iOS has no analogous concept and this value is never returned
    /// there. Combine with [AuthenticationOptions#setSensitiveTransaction(boolean)]
    /// to require this tier when guarding sensitive operations.
    STRONG,

    /// Android **class-2 / "weak"** authenticator tier. Indicates a
    /// biometric whose false-acceptance rate is between 1/10,000 and
    /// 1/100,000 (typically older fingerprint sensors). It can authenticate
    /// the user for UI flows but is NOT permitted to unlock Keystore-bound
    /// keys, so weak-only devices cannot use [SecureStorage]. Populated
    /// only on Android API 30+; not returned on iOS.
    WEAK
}
