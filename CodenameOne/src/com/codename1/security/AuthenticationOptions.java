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

/**
 * Configures a single call to {@link Biometrics#authenticate(AuthenticationOptions)}.
 * Setters return {@code this} for fluent chaining; only {@link #setReason(String)}
 * is required (it maps to the iOS {@code localizedReason} and the Android
 * BiometricPrompt title fallback).
 *
 * <p>Not every option is honored on every platform &mdash; the JavaDoc on each
 * setter notes the platforms where the value is consulted. Unrecognized
 * options are silently ignored, so callers can set the union without
 * platform-checking.</p>
 */
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

    public AuthenticationOptions() {
    }

    public String getReason() {
        return reason;
    }

    /**
     * The user-facing reason for prompting. On iOS this is the
     * {@code localizedReason} passed to {@code LAContext.evaluatePolicy};
     * on Android it is used as the BiometricPrompt title if
     * {@link #setTitle(String)} is unset.
     */
    public AuthenticationOptions setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getTitle() {
        return title;
    }

    /** Android BiometricPrompt title. Ignored on iOS. */
    public AuthenticationOptions setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSubtitle() {
        return subtitle;
    }

    /** Android BiometricPrompt subtitle. Ignored on iOS. */
    public AuthenticationOptions setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public String getDescription() {
        return description;
    }

    /** Android BiometricPrompt description body. Ignored on iOS. */
    public AuthenticationOptions setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getNegativeButtonText() {
        return negativeButtonText;
    }

    /** Android BiometricPrompt negative button label (defaults to "Cancel"). */
    public AuthenticationOptions setNegativeButtonText(String text) {
        this.negativeButtonText = text == null ? "Cancel" : text;
        return this;
    }

    public boolean isBiometricOnly() {
        return biometricOnly;
    }

    /**
     * If {@code true}, the OS prompt rejects device-credential fallback (PIN
     * / pattern / passcode). Honored on both platforms; on Android this maps
     * to {@code setAllowedAuthenticators(BIOMETRIC_STRONG)} or its legacy
     * equivalent.
     */
    public AuthenticationOptions setBiometricOnly(boolean biometricOnly) {
        this.biometricOnly = biometricOnly;
        return this;
    }

    public boolean isSensitiveTransaction() {
        return sensitiveTransaction;
    }

    /**
     * Hints that the operation guards a sensitive action and a class-3
     * ("strong") biometric should be required where the platform exposes the
     * distinction. Affects Android API 30+; advisory on iOS.
     */
    public AuthenticationOptions setSensitiveTransaction(boolean sensitive) {
        this.sensitiveTransaction = sensitive;
        return this;
    }

    public boolean isStickyAuth() {
        return stickyAuth;
    }

    /**
     * If {@code true}, the in-progress authentication survives the app being
     * backgrounded and resumes on foreground (Android sticky-auth semantics).
     * No effect on iOS.
     */
    public AuthenticationOptions setStickyAuth(boolean stickyAuth) {
        this.stickyAuth = stickyAuth;
        return this;
    }

    public boolean isShowDialogOnAndroid() {
        return showDialogOnAndroid;
    }

    /**
     * Controls whether the legacy {@code FingerprintManager} path (Android
     * 6-9) draws a Codename One Dialog over the system prompt. The modern
     * BiometricPrompt path (Android 10+) provides its own UI and ignores
     * this flag.
     */
    public AuthenticationOptions setShowDialogOnAndroid(boolean show) {
        this.showDialogOnAndroid = show;
        return this;
    }
}
