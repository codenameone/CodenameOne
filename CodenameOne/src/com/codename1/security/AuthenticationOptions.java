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

/// Configures a single call to [Biometrics#authenticate(AuthenticationOptions)].
/// Setters return `this` for fluent chaining; only [#setReason(String)] is
/// required (it maps to the iOS `localizedReason` and the Android
/// `BiometricPrompt` title fallback).
///
/// Not every option is honoured on every platform -- the docs on each
/// setter note where the value is consulted. Unrecognised options are
/// silently ignored, so callers can set the union without
/// platform-checking. On platforms that don't support biometrics at all
/// (desktop deploy, JavaScript), the entire prompt collapses into a
/// [BiometricError#NOT_AVAILABLE] failure and none of these options matter.
public final class AuthenticationOptions {

    private String reason;
    private String title;
    private String subtitle;
    private String description;
    private String negativeButtonText = "Cancel";
    private boolean biometricOnly;
    private boolean sensitiveTransaction;
    private boolean stickyAuth;
    private boolean showDialogOnAndroid = true;

    /// The current prompt reason, or `null` if unset. See
    /// [#setReason(String)] for how it is rendered.
    public String getReason() {
        return reason;
    }

    /// The user-facing reason for prompting. On iOS this is the
    /// `localizedReason` passed to `LAContext.evaluatePolicy`; on Android
    /// it is used as the `BiometricPrompt` title if [#setTitle(String)] is
    /// unset. On JavaSE simulator it is shown in the modal prompt body.
    /// Required for production use -- platforms typically reject an empty
    /// reason.
    public AuthenticationOptions setReason(String reason) {
        this.reason = reason;
        return this;
    }

    /// The current Android `BiometricPrompt` title, or `null` to fall back
    /// to [#getReason()]. Always `null` on iOS where this property has no
    /// effect.
    public String getTitle() {
        return title;
    }

    /// Android `BiometricPrompt` title. Ignored on iOS (use [#setReason(String)]
    /// there). Ignored on the fallback base class.
    public AuthenticationOptions setTitle(String title) {
        this.title = title;
        return this;
    }

    /// The current Android `BiometricPrompt` subtitle, or `null` if unset.
    /// Always `null` on iOS where this property has no effect.
    public String getSubtitle() {
        return subtitle;
    }

    /// Android `BiometricPrompt` subtitle. Ignored on iOS and on the
    /// fallback base class.
    public AuthenticationOptions setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /// The current Android `BiometricPrompt` description body, or `null`
    /// if unset. Always `null` on iOS where this property has no effect.
    public String getDescription() {
        return description;
    }

    /// Android `BiometricPrompt` description body. Ignored on iOS and on
    /// the fallback base class.
    public AuthenticationOptions setDescription(String description) {
        this.description = description;
        return this;
    }

    /// The current Android `BiometricPrompt` negative button label.
    /// Defaults to `"Cancel"`. Ignored on iOS (Apple's prompt has its own
    /// system-defined cancel button).
    public String getNegativeButtonText() {
        return negativeButtonText;
    }

    /// Android `BiometricPrompt` negative button label (defaults to
    /// `"Cancel"`). Ignored on iOS and on the fallback base class.
    public AuthenticationOptions setNegativeButtonText(String text) {
        this.negativeButtonText = text == null ? "Cancel" : text;
        return this;
    }

    /// `true` when the OS prompt is configured to reject device-credential
    /// fallback. See [#setBiometricOnly(boolean)].
    public boolean isBiometricOnly() {
        return biometricOnly;
    }

    /// If `true`, the OS prompt rejects device-credential fallback (PIN /
    /// pattern / passcode). Honoured on both platforms; on Android this
    /// maps to `setAllowedAuthenticators(BIOMETRIC_STRONG)` or its legacy
    /// equivalent. Ignored on the fallback base class.
    public AuthenticationOptions setBiometricOnly(boolean biometricOnly) {
        this.biometricOnly = biometricOnly;
        return this;
    }

    /// `true` when the caller has asked for a class-3 ("strong") biometric.
    /// See [#setSensitiveTransaction(boolean)].
    public boolean isSensitiveTransaction() {
        return sensitiveTransaction;
    }

    /// Hints that the operation guards a sensitive action and a class-3
    /// ("strong") biometric should be required where the platform exposes
    /// the distinction. Affects Android API 30+; advisory on iOS (where
    /// Face ID / Touch ID are both considered strong). Ignored on the
    /// fallback base class.
    public AuthenticationOptions setSensitiveTransaction(boolean sensitive) {
        this.sensitiveTransaction = sensitive;
        return this;
    }

    /// `true` when the Android sticky-auth flag is set. See
    /// [#setStickyAuth(boolean)].
    public boolean isStickyAuth() {
        return stickyAuth;
    }

    /// If `true`, the in-progress authentication survives the app being
    /// backgrounded and resumes on foreground (Android sticky-auth
    /// semantics). No effect on iOS or on the fallback base class.
    public AuthenticationOptions setStickyAuth(boolean stickyAuth) {
        this.stickyAuth = stickyAuth;
        return this;
    }

    /// `true` (the default) when Codename One should overlay its own dialog
    /// on the legacy Android `FingerprintManager` path. See
    /// [#setShowDialogOnAndroid(boolean)].
    public boolean isShowDialogOnAndroid() {
        return showDialogOnAndroid;
    }

    /// Controls whether the legacy `FingerprintManager` path (Android 6-9)
    /// draws a Codename One Dialog over the system prompt. The modern
    /// `BiometricPrompt` path (Android 10+) provides its own UI and ignores
    /// this flag. Ignored on iOS and on the fallback base class.
    public AuthenticationOptions setShowDialogOnAndroid(boolean show) {
        this.showDialogOnAndroid = show;
        return this;
    }
}
