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

/// Biometric-gated secure storage backed by the platform keychain. Reading an
/// entry prompts the user for biometric authentication; writing or deleting
/// may or may not, depending on the platform.
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
public abstract class SecureStorage {

    private static SecureStorage fallback;

    /// Subclasses are constructed by the port; not for application use.
    protected SecureStorage() {
    }

    /// Returns the platform-specific singleton owned by the current port.
    /// Ports that do not implement secure storage get a no-op fallback that
    /// reports [BiometricError#NOT_AVAILABLE].
    public static SecureStorage getInstance() {
        SecureStorage s = Display.getInstance().getSecureStorage();
        if (s != null) {
            return s;
        }
        if (fallback == null) {
            fallback = new StubSecureStorage();
        }
        return fallback;
    }

    /// Retrieves a previously-stored entry, prompting for biometric
    /// authentication. The returned `AsyncResource` completes with the value,
    /// or with a [BiometricException] on failure (including
    /// [BiometricError#KEY_REVOKED] when biometrics have been re-enrolled
    /// since the entry was written).
    public abstract AsyncResource<String> get(String reason, String account);

    /// Stores or overwrites a value for the given account. On iOS the user
    /// is typically not prompted (Apple's keychain accepts writes without
    /// re-authenticating); on Android the user is prompted because the
    /// underlying cipher requires biometric authentication.
    public abstract AsyncResource<Boolean> set(String reason, String account, String value);

    /// Removes a previously-stored entry. No authentication is required since
    /// deletion does not reveal the value.
    public abstract AsyncResource<Boolean> remove(String reason, String account);

    /// Configures the iOS keychain access group for sharing entries between
    /// the main app and its extensions. The argument must include the Team
    /// ID prefix (e.g. `"ABCDE12345.group.com.example.app"`). Pass `null` or
    /// empty to clear. Ignored on non-iOS platforms.
    ///
    /// The `ios.keychainAccessGroup` build hint must declare the same group
    /// in the app's entitlements for this to work.
    public abstract void setKeychainAccessGroup(String group);
}
