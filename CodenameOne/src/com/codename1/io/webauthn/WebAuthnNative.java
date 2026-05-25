/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.webauthn;

/// Service-provider interface that [WebAuthnClient] uses to dispatch a passkey
/// ceremony through the OS's public-key credential API
/// (`ASAuthorizationPlatformPublicKeyCredentialProvider` on iOS 16+,
/// `androidx.credentials.CredentialManager` on Android API 28+).
///
/// The platform port supplies an implementation named
/// `com.codename1.io.webauthn.WebAuthnNativeImpl`; [WebAuthnClient] loads it
/// via [WebAuthnClient#setProvider(WebAuthnNative)] which the port calls at
/// app startup. Cn1lib authors who want to plug in their own implementation
/// (for example, a USB-HID security-key driver) can declare a subtype and
/// register it the same way.
///
/// The data interchange is intentionally **JSON in, JSON out** -- both sides
/// of the W3C `navigator.credentials.create()` / `.get()` call have a
/// well-defined JSON serialisation (PublicKeyCredentialCreationOptionsJSON /
/// PublicKeyCredentialRequestOptionsJSON, RegistrationResponseJSON /
/// AuthenticationResponseJSON). Passing strings keeps the native border narrow
/// and lets the implementation forward the JSON straight to the OS API (both
/// platforms accept JSON-shaped inputs in their modern APIs).
///
/// @since 7.0.246
public interface WebAuthnNative {

    /// `true` if this implementation can actually call the OS authenticator
    /// on the current device / OS version. When `false`, [WebAuthnClient]
    /// fails the ceremony with [WebAuthnException#NOT_IMPLEMENTED] so the
    /// caller can present a fallback UI (e.g. password sign-in).
    boolean isSupported();

    /// Runs a `navigator.credentials.create()` ceremony.
    /// `creationOptionsJson` must be a PublicKeyCredentialCreationOptionsJSON
    /// document as defined by W3C Credential Management Level 1. Returns a
    /// RegistrationResponseJSON string on success, or `null` if the user
    /// dismissed the sheet.
    ///
    /// Implementations are expected to be blocking: the caller is on a worker
    /// thread and waits for the result.
    ///
    /// On error, the implementation should throw a [WebAuthnException] with
    /// a code from the constants on that class.
    String createPasskey(String creationOptionsJson) throws WebAuthnException;

    /// Runs a `navigator.credentials.get()` ceremony.
    /// `requestOptionsJson` must be a PublicKeyCredentialRequestOptionsJSON
    /// document. Returns an AuthenticationResponseJSON string on success, or
    /// `null` if the user dismissed the sheet.
    ///
    /// On error, the implementation should throw a [WebAuthnException] with
    /// a code from the constants on that class.
    String getPasskey(String requestOptionsJson) throws WebAuthnException;
}
