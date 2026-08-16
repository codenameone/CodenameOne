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
package com.codename1.home.commissioning;

import com.codename1.home.HomeError;
import com.codename1.home.HomeException;
import com.codename1.impl.home.CommissioningGateway;
import com.codename1.util.AsyncResource;

/// Adds a new Matter accessory to the user's home.
///
/// Obtain it from `com.codename1.home.SmartHome#getCommissioner()`, which
/// never returns `null`.
///
/// ```java
/// Commissioner c = SmartHome.getInstance().getCommissioner();
/// if (!c.isSupported()) {
///     c.openEcosystemApp();
///     return;
/// }
/// c.commission(new CommissioningRequest().setSetupPayload(scanned))
///  .onResult((result, err) -> {
///      if (err != null) {
///          return;
///      }
///      if (result.wasCommissionedToThisApp()) {
///          SmartHome.getInstance().refresh();
///      }
///  });
/// ```
///
/// #### The flow is not yours
///
/// Both mobile backends hand the whole interaction to an operating-system
/// sheet: iOS presents `MatterSupport`'s add-device UI and Android presents
/// Play services' commissioning activity. There is no progress reporting, and
/// the user may be several minutes -- they have to power the accessory on,
/// hold a button, sometimes join it to Wi-Fi. Do not put a short timeout or a
/// determinate progress bar behind this.
public final class Commissioner {

    private final CommissioningGateway gateway;

    /// Created by `com.codename1.home.SmartHome`; not part of the public
    /// surface.
    ///
    /// #### Parameters
    ///
    /// - `gateway`: the machinery to delegate to, or `null` for an inert
    ///   commissioner
    public Commissioner(CommissioningGateway gateway) {
        this.gateway = gateway;
    }

    /// How this backend adds an accessory.
    ///
    /// #### Returns
    ///
    /// the style, never `null`
    public CommissioningStyle getStyle() {
        if (gateway == null) {
            return CommissioningStyle.NONE;
        }
        int ordinal = gateway.getCommissioningStyle();
        CommissioningStyle[] all = CommissioningStyle.values();
        if (ordinal < 0 || ordinal >= all.length) {
            return CommissioningStyle.NONE;
        }
        return all[ordinal];
    }

    /// Whether [#commission(CommissioningRequest)] can do anything here.
    ///
    /// `false` on watchOS, tvOS and macOS, and on an Android device with no
    /// Play services. Check it before offering an "add a device" button;
    /// [#openEcosystemApp()] is usually the honest alternative.
    ///
    /// #### Returns
    ///
    /// `true` when this platform can commission
    public boolean isSupported() {
        return getStyle() == CommissioningStyle.OS_OWNED_UI;
    }

    /// Adds an accessory.
    ///
    /// The result resolves when the platform's flow finishes, however it
    /// finished. A user who backed out produces a failure carrying
    /// [HomeError#USER_CANCELED], which is a normal outcome rather than
    /// something to report as an error.
    ///
    /// **Read [CommissioningResult#wasCommissionedToThisApp()]** before doing
    /// anything with the returned accessory id.
    ///
    /// #### Parameters
    ///
    /// - `request`: what to add and where; an empty request means "open the
    ///   platform UI and let the user do everything there"
    ///
    /// #### Returns
    ///
    /// the outcome, delivered on the EDT
    public AsyncResource<CommissioningResult> commission(
            CommissioningRequest request) {
        if (gateway == null) {
            return failed(HomeError.COMMISSIONING_UNAVAILABLE,
                    "this platform cannot add smart-home accessories");
        }
        if (!isSupported()) {
            return failed(HomeError.COMMISSIONING_UNAVAILABLE,
                    "this platform cannot add smart-home accessories; send"
                            + " the user to the ecosystem app with"
                            + " openEcosystemApp() instead");
        }
        return gateway.commission(
                request == null ? new CommissioningRequest() : request);
    }

    /// Opens the platform's ecosystem app -- Apple Home, Google Home -- so the
    /// user can add an accessory there instead.
    ///
    /// The answer on every platform where [#isSupported()] is `false`, and a
    /// reasonable fallback when commissioning fails for reasons your app
    /// cannot fix.
    ///
    /// #### Returns
    ///
    /// `true` when the app was opened; `false` when it is not installed, which
    /// is worth telling the user, since installing it is something they can do
    public boolean openEcosystemApp() {
        return gateway != null && gateway.openEcosystemApp();
    }

    private static AsyncResource<CommissioningResult> failed(HomeError error,
            String message) {
        AsyncResource<CommissioningResult> r =
                new AsyncResource<CommissioningResult>();
        r.error(new HomeException(error, message));
        return r;
    }
}
