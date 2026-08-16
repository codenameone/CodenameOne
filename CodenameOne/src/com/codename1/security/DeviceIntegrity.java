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
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;

/// Device-integrity and runtime self-protection (RASP) entry point. Groups four families of
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
/// 4. **Tapjacking / overlay defense** -- [#setTapjackingProtection(TapjackingPolicy)] detects, and
///    optionally drops, touches that arrive while another app's window is drawn over yours, and
///    [#setHideOverlayWindows(boolean)] asks Android 12+ to remove such windows outright. Where
///    family 3 asks "is a hostile accessibility service installed", this one answers "is something
///    covering the screen right now".
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
/// - `android.tapjackingGuard=true` (optionally `android.tapjackingGuard.mode=block|strict|report`
///   and `android.tapjackingGuard.hideOverlays=true|false`) -- applies a tapjacking policy at
///   startup and, by default, also asks Android 12+ to hide overlay windows (declaring the
///   `HIDE_OVERLAY_WINDOWS` permission that needs), with no app code.
/// - `android.hideOverlayWindows=true` -- declares that permission on its own, for apps that call
///   [#setHideOverlayWindows(boolean)] directly without the launch-time guard.
///
/// #### Platform support
///
/// - **Android** -- full support. Attestation via Play Integrity (requires the `android.playIntegrity`
///   build hint to bundle the SDK), RASP via the root/Frida/emulator checks, accessibility enumeration
///   via the system settings, and secure screens via `FLAG_SECURE`. Tapjacking detection reads the
///   obscured flags carried on each touch; [#setHideOverlayWindows(boolean)] needs Android 12+.
/// - **iOS** -- attestation via App Attest (requires the `ios.appAttest` build hint), RASP via the
///   jailbreak detector. Accessibility-service enumeration and [#setSecureScreen(boolean)] are
///   Android-only concepts and are no-ops on iOS. So is the tapjacking family: iOS gives no
///   application a way to draw over another app, so there is nothing to detect and
///   [#isScreenObscured()] is always false. (Screen recording and mirroring are a different threat,
///   covered by the `ios.disableScreenshots` build hint.)
/// - **JavaSE simulator / other ports** -- behave as a clean, unsupported device: attestation
///   completes with an error, [#isDeviceCompromised()] returns false and the accessibility list is
///   empty. Application code never needs platform `if` statements. The simulator can fake an
///   overlay from `Simulate > App Shield > Screen Overlay (Tapjacking)`.
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

    /// Discards the cached platform attestation state, so the next [#requestIntegrityToken(String)]
    /// attests from a fresh hardware key.
    ///
    /// Only iOS holds client-side attestation state. Apple's model is: generate a hardware key once,
    /// attest it once, then produce cheap assertions against it for every subsequent request. Your
    /// backend records the key when it accepts the attestation. If the backend later rejects a request
    /// because it does not recognise the key -- the app was reinstalled, the device was restored from a
    /// backup, or the OS invalidated the key -- call this, then request a token again. That is the only
    /// correct recovery; retrying with the same key will keep failing.
    ///
    /// Do not call this on every failure. Attestation is rate limited by Apple, and re-attesting in a
    /// loop will get the app throttled. No-op on Android, where Play Integrity keeps no client key, and
    /// where attestation is unsupported.
    public static void resetAttestation() {
        Display.getInstance().resetAttestation();
    }

    /// Tells the attestation layer that your backend has recorded the attested key, so later requests
    /// can use cheap assertions instead of attesting again.
    ///
    /// This matters on iOS. The first token of a device's life is an attestation, which carries the
    /// public key; every token after it is an assertion, which carries only the key's identifier. An
    /// assertion sent before the backend has stored that public key is unresolvable, and the natural
    /// reading of that rejection -- the key is invalid -- would throw away a key that was perfectly
    /// good and burn one of Apple's rate limited attestations replacing it. So requests made between
    /// the attestation and this acknowledgement are refused with a retry hint rather than asserted.
    ///
    /// Call it once, after the response accepting the attestation token, passing the key that
    /// response acknowledged. Not calling it is safe but slower: the client assumes registration
    /// succeeded after a short grace period. No-op on Android and where attestation is
    /// unsupported.
    /// @param keyId the key identifier your backend recorded -- the middle field of the
    ///        `cn1aa1:attest:<keyId>:<attestation>` token it accepted, base64-decoded.
    ///        Naming it matters: a response for an earlier attestation can arrive after
    ///        the key has already been replaced, and acknowledging that would mark a key
    ///        attested which the backend has never seen.
    public static void confirmAttestation(String keyId) {
        Display.getInstance().confirmAttestation(keyId);
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
        if (enabledServices == null || enabledServices.length == 0) {
            return false;
        }
        for (String service : enabledServices) {
            if (service == null || service.length() == 0) {
                continue;
            }
            String pkg = service;
            int slash = pkg.indexOf('/');
            if (slash >= 0) {
                pkg = pkg.substring(0, slash);
            }
            if (!isAllowed(pkg, allowedPackages)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowed(String pkg, String[] allowedPackages) {
        if (allowedPackages == null) {
            return false;
        }
        for (String allowed : allowedPackages) {
            if (pkg.equals(allowed)) {
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

    /// Sets what the framework does about touches that arrive while another application's window is
    /// drawn over this app -- a tapjacking attack.
    ///
    /// The default is [TapjackingPolicy#OFF]. [TapjackingPolicy#BLOCK] is the recommended setting
    /// for a sensitive app: it drops any gesture that starts on a fully obscured window, and
    /// reports it. Set this once during startup.
    ///
    /// ```java
    /// DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.BLOCK);
    /// DeviceIntegrity.addTapjackingListener(e -> {
    ///     if (DeviceIntegrity.isScreenObscured()) {
    ///         Dialog.show("Security warning",
    ///             "Another app is drawing over this screen. Close it before continuing.",
    ///             "OK", null);
    ///     }
    /// });
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `policy`: the policy to apply; null is treated as [TapjackingPolicy#OFF]
    public static void setTapjackingProtection(TapjackingPolicy policy) {
        Display.getInstance().setTapjackingProtection(policy);
    }

    /// The tapjacking policy currently in force. Never null; [TapjackingPolicy#OFF] until set.
    public static TapjackingPolicy getTapjackingPolicy() {
        return Display.getInstance().getTapjackingPolicy();
    }

    /// True when the **most recently observed touch** arrived while another application's window was
    /// drawn over this app.
    ///
    /// Read that literally. Android reports obscuring as a flag on a delivered touch event, so this
    /// is not a live query of the window stack: if an overlay appears and the user never touches
    /// the screen, nothing is observed and this stays false. It also only becomes live once a
    /// policy other than [TapjackingPolicy#OFF] is set.
    ///
    /// That reactive limitation is the reason [#setHideOverlayWindows(boolean)] exists -- it
    /// prevents the overlay instead of noticing it after the fact.
    ///
    /// #### Returns
    ///
    /// true if the last observed touch was obscured
    public static boolean isScreenObscured() {
        return Display.getInstance().isScreenObscured();
    }

    /// Registers a listener notified when the obscured state **changes** -- once when an overlay is
    /// first observed, and again when a later touch arrives clean, so an app can raise and then
    /// dismiss a warning. It does not fire per touch.
    ///
    /// This matters when the policy blocks: a blocked gesture is never delivered to your components,
    /// so this listener is the only way the app learns the tap happened at all. Callbacks arrive on
    /// the EDT, and the [com.codename1.ui.events.ActionEvent] source is a `Boolean` carrying the new
    /// state.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addTapjackingListener(ActionListener l) {
        Display.getInstance().addTapjackingListener(l);
    }

    /// Removes a listener added by [#addTapjackingListener(ActionListener)].
    public static void removeTapjackingListener(ActionListener l) {
        Display.getInstance().removeTapjackingListener(l);
    }

    /// Asks the OS to hide non-system windows drawn over this app while it is in the foreground
    /// (Android 12 / API 31 `Window.setHideOverlayWindows`).
    ///
    /// This is the strongest of the tapjacking defenses and the only one that also covers native
    /// peer components such as `BrowserComponent` and native text fields, because it removes the
    /// overlay rather than filtering the touches it enables. Pair it with [#setSecureScreen(boolean)]
    /// when entering a sensitive screen and clear both on the way out.
    ///
    /// Two things are required, and [#isHideOverlayWindowsSupported()] reports both rather than
    /// claiming a protection you are not getting: Android 12 (API 31) or newer, and the
    /// `android.permission.HIDE_OVERLAY_WINDOWS` manifest permission. Android throws without the
    /// permission, so this call is skipped and logged instead. The `android.tapjackingGuard` build
    /// hint declares it; apps that use only the runtime API should set `android.hideOverlayWindows`.
    /// It is a normal install-time permission, so the user is never prompted.
    ///
    /// On older Android releases, and on every other platform, this is a no-op and the touch level
    /// policy set by [#setTapjackingProtection(TapjackingPolicy)] is what protects the app.
    ///
    /// #### Parameters
    ///
    /// - `hide`: true to hide overlay windows, false to allow them again
    public static void setHideOverlayWindows(boolean hide) {
        Display.getInstance().setHideOverlayWindows(hide);
    }

    /// True when [#setHideOverlayWindows(boolean)] is actually enforced: Android 12 or newer **and**
    /// the app holds `android.permission.HIDE_OVERLAY_WINDOWS`. False elsewhere, including on an
    /// Android 12 device whose build never declared the permission -- the API level alone would be a
    /// claim the app could not verify.
    public static boolean isHideOverlayWindowsSupported() {
        return Display.getInstance().isHideOverlayWindowsSupported();
    }
}
