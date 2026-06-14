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
package com.codename1.payment;

import com.codename1.ui.Display;

/// Publishes card data to the Apple Wallet issuer-provisioning extension so
/// users can add the issuer's cards to Apple Wallet from inside the Wallet
/// app ("From apps on your iPhone"). iOS only; on other platforms
/// [#isSupported()] returns false and all methods are no-ops.
///
/// ### How it works
///
/// The Wallet extension runs in a separate process, launched by the Wallet
/// app, usually while your app is not running. It cannot call into your Java
/// code. Instead your app pre-publishes the list of available cards (and an
/// auth token) with this class; the data is stored in the shared App Group
/// container where the generated extension reads it. The final provisioning
/// step - producing the encrypted pass payload - is performed by your issuer
/// backend: the extension POSTs Apple's certificates/nonce plus the card
/// identifier and auth token to the HTTPS endpoint configured in the
/// `ios.wallet.issuerEndpoint` build hint, which must respond with JSON
/// `{"activationData", "encryptedPassData", "ephemeralPublicKey"}` (base64).
///
/// Call [#setPassEntries(WalletPassEntry[])] whenever the user's card list
/// changes (e.g. after login) and keep a fresh auth token published with
/// [#setAuthToken(java.lang.String)]; call [#clear()] on logout.
///
/// ### Enabling the extension (no native code needed)
///
/// Add these build hints to the iOS build:
///
/// - `ios.wallet.extension=true` - generates the non-UI Wallet extension
/// - `ios.wallet.appGroup=group.com.mycompany.myapp` - shared App Group id
/// - `ios.wallet.issuerEndpoint=https://...` - issuer backend endpoint
/// - `ios.wallet.includeUI=true` + `ios.wallet.authEndpoint=https://...` -
///   optional in-Wallet login screen, only needed when you report
///   [#setRequiresAuthentication(boolean)] true instead of keeping a token
/// - `ios.wallet.*Inject` hints - inject custom Objective-C at key points
///
/// Each extension needs its own App ID and provisioning profile carrying the
/// restricted `com.apple.developer.payment-pass-provisioning` entitlement,
/// which Apple grants per-app on request. For cloud builds either place the
/// `.mobileprovision` files in `src/main/resources` and name them in the
/// `ios.wallet.nonuiProvisioningProfile` / `ios.wallet.uiProvisioningProfile`
/// hints, or host them at the URLs given in the
/// `ios.wallet.nonuiProvisioningURL` / `ios.wallet.uiProvisioningURL` hints.
/// The card network must also list the extension App IDs in the pass
/// metadata (`associatedApplicationIdentifiers`) or Wallet never invokes
/// the extension.
///
/// ### Bringing your own extension
///
/// If you already have Xcode extension targets (e.g. from an issuer SDK
/// vendor or an existing native app), skip the `ios.wallet.*` hints and copy
/// each extension into `ios/app_extensions/<Name>/` in your project instead:
/// its sources, `Info.plist`, `<Name>.entitlements`, an optional
/// `buildSettings.properties` with Xcode build settings (set
/// `IPHONEOS_DEPLOYMENT_TARGET=14.0` for Wallet extensions) and the
/// extension's `.mobileprovision` for cloud device builds.
public final class WalletExtension {

    private WalletExtension() {
    }

    /// Returns true if the platform supports publishing Wallet extension
    /// data: iOS 14 or newer with the `ios.wallet.extension` build hint
    /// enabled. Returns false on all other platforms and in the simulator.
    public static boolean isSupported() {
        return Display.getInstance().isWalletExtensionSupported();
    }

    /// Publishes the cards that the Wallet extension offers for provisioning
    /// on the iPhone itself, replacing any previously published list. Cards
    /// already present in Wallet are filtered out automatically by the
    /// extension.
    ///
    /// #### Parameters
    ///
    /// - `entries`: the available cards; an empty array or null clears the list
    public static void setPassEntries(WalletPassEntry[] entries) {
        Display.getInstance().walletExtensionSetPassEntries(false, entries);
    }

    /// Publishes the cards offered for provisioning on a paired Apple Watch,
    /// replacing any previously published list. Typically the same list as
    /// [#setPassEntries(WalletPassEntry[])].
    ///
    /// #### Parameters
    ///
    /// - `entries`: the available cards; an empty array or null clears the list
    public static void setRemotePassEntries(WalletPassEntry[] entries) {
        Display.getInstance().walletExtensionSetPassEntries(true, entries);
    }

    /// Sets whether Wallet should present the login UI extension before
    /// provisioning. Keep this false and maintain a fresh token with
    /// [#setAuthToken(java.lang.String)] to skip in-Wallet login entirely.
    public static void setRequiresAuthentication(boolean requiresAuthentication) {
        Display.getInstance().walletExtensionSetRequiresAuthentication(requiresAuthentication);
    }

    /// Publishes the auth token that the extension forwards to the issuer
    /// endpoint (JSON `authToken` field and `Authorization: Bearer` header)
    /// when the user adds a card.
    ///
    /// #### Parameters
    ///
    /// - `token`: the token, or null to remove it
    public static void setAuthToken(String token) {
        Display.getInstance().walletExtensionSetAuthToken(token);
    }

    /// Clears everything previously published: pass entries, remote pass
    /// entries, the auth token and the requires-authentication flag. Call
    /// this when the user logs out.
    public static void clear() {
        Display.getInstance().walletExtensionClear();
    }
}
