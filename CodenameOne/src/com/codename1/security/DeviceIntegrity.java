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

/// Device-integrity and runtime self-protection (RASP) entry point. Groups three families of
/// security primitives that an app -- a banking app in particular -- can use to react to a hostile
/// runtime environment:
///
/// 1. **Platform attestation** -- [#requestIntegrityToken(String)] returns a signed Google Play
///    Integrity token (Android) or Apple App Attest assertion (iOS). The token is opaque and must be
///    sent to and **verified by your backend**; it is the only trustworthy way to gate a high value
///    action (such as a transfer to a newly added beneficiary) on device/app integrity, because a
///    decision made on a compromised device can itself be tampered with.
/// 2. **RASP reporting** -- [#isDeviceCompromised()] / [#getCompromiseReasons()] expose a non-exiting
///    aggregate of the root/jailbreak/instrumentation checks so the app can degrade gracefully
///    (warn, disable a feature, require step-up auth) instead of being hard-killed at launch.
/// 3. **Accessibility-abuse defense** -- [#getEnabledAccessibilityServices()] /
///    [#hasUntrustedAccessibilityService(String...)] detect malware that abuses Android accessibility
///    services for overlays, remote control and on-screen text extraction, and [#setSecureScreen(boolean)]
///    blocks screenshots, screen recording and accessibility screen scraping on sensitive screens.
///
/// #### Zero-code build hints
///
/// Each capability also has a build hint that wires an automatic launch-time guard, so a project can
/// adopt it without writing code:
///
/// - `android.playIntegrity=true` (optionally `android.playIntegrity.verifyUrl=<backend>`) -- bundles
///   the Play Integrity SDK, enables [#requestIntegrityToken(String)], and -- when a verify URL is set
///   -- attests at launch and exits if the backend rejects the token.
/// - `ios.appAttest=true` -- enables App Attest and [#requestIntegrityToken(String)] on iOS.
/// - `android.rootCheck` / `android.fridaDetection` / `ios.detectJailbreak` -- existing hard launch
///   gates that exit on a compromised device. [#isDeviceCompromised()] reports the same signals
///   without exiting.
/// - `android.accessibilityGuard=true` (optionally `android.accessibilityGuard.allow=<csv packages>`
///   and `android.accessibilityGuard.mode=exit|warn`) -- checks the enabled accessibility services at
///   launch and exits (or logs) when an untrusted one is active.
///
/// #### Platform support
///
/// - **Android** -- full support. Attestation via Play Integrity (requires the `android.playIntegrity`
///   build hint to bundle the SDK), RASP via the root/Frida/emulator checks, accessibility enumeration
///   via the system settings, and secure screens via `FLAG_SECURE`.
/// - **iOS** -- attestation via App Attest (requires the `ios.appAttest` build hint), RASP via the
///   jailbreak detector. Accessibility-service enumeration and [#setSecureScreen(boolean)] are
///   Android-only concepts and are no-ops on iOS.
/// - **JavaSE simulator / other ports** -- behave as a clean, unsupported device: attestation
///   completes with an error, [#isDeviceCompromised()] returns false and the accessibility list is
///   empty. Application code never needs platform `if` statements.
///
/// #### Since
///
/// 8.0
public final class DeviceIntegrity {

    private DeviceIntegrity() {
    }

    /// Requests a signed platform-attestation token bound to the supplied server nonce.
    ///
    /// On Android this drives the Google Play Integrity API (bundle it with the `android.playIntegrity`
    /// build hint); on iOS it drives Apple App Attest (enable it with the `ios.appAttest` build hint).
    /// The resulting token is opaque and **must be verified server-side** -- POST it to your backend,
    /// which decrypts/validates the verdict with Google/Apple and decides whether to permit the action.
    ///
    /// ```java
    /// DeviceIntegrity.requestIntegrityToken(serverNonce).onResult((token, err) -> {
    ///     if (err != null) {
    ///         // attestation unavailable -- treat as untrusted / require step-up
    ///         return;
    ///     }
    ///     // POST token to the bank backend; the backend allows or blocks the transfer
    /// });
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `serverNonce`: a fresh, server-generated nonce/challenge to bind into the attestation, used by
    ///   the backend to prevent replay
    ///
    /// #### Returns
    ///
    /// an `AsyncResource` that completes with the opaque attestation token, or completes with an error
    /// when attestation is unsupported or the platform request fails
    public static AsyncResource<String> requestIntegrityToken(String serverNonce) {
        return Display.getInstance().requestIntegrityToken(serverNonce);
    }

    /// Returns true when platform attestation (Play Integrity / App Attest) is available on this device
    /// and was bundled into the build via the relevant build hint.
    public static boolean isAttestationSupported() {
        return Display.getInstance().isAttestationSupported();
    }

    /// Non-exiting RASP check. Returns true when the device shows signs of being rooted, jailbroken,
    /// running under dynamic instrumentation (e.g. Frida) or otherwise tampered. Unlike the
    /// `android.rootCheck` / `ios.detectJailbreak` launch gates this never terminates the app, so it is
    /// safe to call from runtime logic (for example before authorizing a transfer).
    public static boolean isDeviceCompromised() {
        return Display.getInstance().isDeviceCompromised();
    }

    /// Returns the reason codes behind [#isDeviceCompromised()], e.g. `"root"`, `"frida"`, `"emulator"`,
    /// `"jailbreak"`. Empty when the device appears clean.
    public static String[] getCompromiseReasons() {
        return Display.getInstance().getCompromiseReasons();
    }

    /// Returns the component identifiers (`package/.ServiceClass`) of the accessibility services
    /// currently enabled on the device. Android only; returns an empty array on iOS and other ports.
    public static String[] getEnabledAccessibilityServices() {
        return Display.getInstance().getEnabledAccessibilityServices();
    }

    /// Returns true when an accessibility service that is **not** in the supplied allow-list is
    /// currently enabled -- a strong indicator of accessibility-abusing malware on Android. Pass the
    /// package names your app explicitly trusts (for example a known screen reader the user relies on);
    /// any enabled service whose package is not listed makes this return true. With no arguments, this
    /// returns true whenever any accessibility service is enabled.
    ///
    /// #### Parameters
    ///
    /// - `allowedPackages`: package names of accessibility services considered safe
    ///
    /// #### Returns
    ///
    /// true if at least one enabled accessibility service is not in `allowedPackages`
    public static boolean hasUntrustedAccessibilityService(String... allowedPackages) {
        return containsUntrustedService(getEnabledAccessibilityServices(), allowedPackages);
    }

    /// Pure allow-list evaluation, separated from the platform lookup so it can be unit tested.
    /// Returns true if any entry in `enabledServices` (a `package/.ServiceClass` component id) has a
    /// package that is not present in `allowedPackages`.
    static boolean containsUntrustedService(String[] enabledServices, String[] allowedPackages) {
        if(enabledServices == null || enabledServices.length == 0) {
            return false;
        }
        for(String service : enabledServices) {
            if(service == null || service.length() == 0) {
                continue;
            }
            String pkg = service;
            int slash = pkg.indexOf('/');
            if(slash >= 0) {
                pkg = pkg.substring(0, slash);
            }
            if(!isAllowed(pkg, allowedPackages)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowed(String pkg, String[] allowedPackages) {
        if(allowedPackages == null) {
            return false;
        }
        for(String allowed : allowedPackages) {
            if(pkg.equals(allowed)) {
                return true;
            }
        }
        return false;
    }

    /// Marks the currently displayed screen as secure, blocking OS screenshots, screen recording and
    /// accessibility screen scraping while it is showing (Android `FLAG_SECURE`). Call with `true`
    /// when entering a sensitive screen (PIN entry, transfer confirmation) and `false` when leaving it.
    /// No-op on iOS and other ports.
    ///
    /// #### Parameters
    ///
    /// - `secure`: true to protect the screen, false to clear the protection
    public static void setSecureScreen(boolean secure) {
        Display.getInstance().setSecureScreen(secure);
    }
}
