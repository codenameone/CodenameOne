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

/// Biometric-gated secure storage backed by the platform keychain. Reading
/// an entry prompts the user for biometric authentication; writing or
/// deleting may or may not, depending on the platform.
///
/// Entries are bound to the current set of enrolled biometrics. If the user
/// adds a fingerprint, enrols a new face, or disables device security, every
/// stored entry is automatically invalidated and subsequent
/// [#get(String, String)] calls fail with [BiometricError#KEY_REVOKED]. The
/// application must then re-prompt the user for the original value and
/// [#set(String, String, String)] it again.
///
/// Use this for short, secret strings (auth tokens, refresh tokens,
/// encryption keys). For larger data, encrypt with a key stored here.
///
/// #### Platform support
///
/// - **iOS** -- backed by Security.framework (`SecItemAdd` /
///   `SecItemCopyMatching` / `SecItemDelete`) with
///   `kSecAccessControlTouchIDCurrentSet`. Sharing entries with App
///   Extensions requires both the `ios.keychainAccessGroup` build hint AND
///   a call to [#setKeychainAccessGroup(String)] passing the same
///   Team-ID-prefixed group identifier.
/// - **Android** -- AES/CBC/PKCS7 ciphertext stored in `SharedPreferences`
///   with the key in the `AndroidKeyStore`, locked via
///   `setUserAuthenticationRequired(true)`. The `BiometricPrompt` (API 29+)
///   or `FingerprintManager` (API 23-28) unlocks the cipher for one
///   operation per prompt.
/// - **JavaSE simulator** -- backed by `java.util.prefs.Preferences`, gated
///   on the same Biometric Simulation menu used by [Biometrics]. Useful for
///   testing the round-trip and `KEY_REVOKED` paths without a device.
/// - **All other platforms** -- this base class is returned as-is and acts
///   as a non-supporting fallback: every method completes with
///   [BiometricError#NOT_AVAILABLE]. Application code does not need
///   platform `if` statements.
public class SecureStorage {

    /// Subclasses are constructed by the port. Application code obtains the
    /// active instance via [#getInstance()].
    protected SecureStorage() {
    }

    /// Returns the platform-specific singleton owned by the current port.
    /// On ports that do not implement secure storage this returns a base
    /// [SecureStorage] instance whose methods report
    /// [BiometricError#NOT_AVAILABLE].
    public static SecureStorage getInstance() {
        SecureStorage s = Display.getInstance().getSecureStorage();
        return s != null ? s : DEFAULT;
    }

    private static final SecureStorage DEFAULT = new SecureStorage();

    /// Retrieves a previously-stored entry, prompting for biometric
    /// authentication. The returned `AsyncResource` completes with the
    /// value, or with a [BiometricException] on failure (including
    /// [BiometricError#KEY_REVOKED] when biometrics have been re-enrolled
    /// since the entry was written). On the fallback base class this
    /// completes immediately with [BiometricError#NOT_AVAILABLE].
    public AsyncResource<String> get(String reason, String account) {
        AsyncResource<String> r = new AsyncResource<String>();
        r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                "Secure storage is not available on this platform"));
        return r;
    }

    /// Stores or overwrites a value for the given account. On iOS the user
    /// is typically not prompted (Apple's keychain accepts writes without
    /// re-authenticating); on Android the user is prompted because the
    /// underlying cipher requires biometric authentication. On the fallback
    /// base class this completes immediately with
    /// [BiometricError#NOT_AVAILABLE].
    public AsyncResource<Boolean> set(String reason, String account, String value) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                "Secure storage is not available on this platform"));
        return r;
    }

    /// Removes a previously-stored entry. No authentication is required
    /// since deletion does not reveal the value. On the fallback base class
    /// this completes immediately with [BiometricError#NOT_AVAILABLE].
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                "Secure storage is not available on this platform"));
        return r;
    }

    /// Configures the iOS keychain access group for sharing entries between
    /// the main app and its extensions. The argument must include the Team
    /// ID prefix (e.g. `"ABCDE12345.group.com.example.app"`). Pass `null`
    /// or empty to clear. Ignored on non-iOS platforms and on the fallback
    /// base class.
    ///
    /// The `ios.keychainAccessGroup` build hint must declare the same group
    /// in the app's entitlements for this to work.
    public void setKeychainAccessGroup(String group) {
        // No-op fallback.
    }
}
