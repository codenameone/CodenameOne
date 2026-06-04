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
package com.codename1.ads;

/// Receives the lifecycle events of an ad. This is an adapter class with empty
/// implementations so you only override the events you care about:
///
/// ```java
/// interstitial.setAdListener(new AdListener() {
///     public void onDismissed() {
///         // resume gameplay
///     }
/// });
/// ```
///
/// Every callback is delivered on the Codename One EDT, so it is safe to touch
/// the UI directly. The model is fully event driven; there is never a need to
/// poll [InterstitialAd#isLoaded()] in a loop.
public class AdListener {
    /// Invoked when the ad finished loading and is ready to be shown.
    public void onLoaded() {
    }

    /// Invoked when the ad failed to load.
    ///
    /// #### Parameters
    ///
    /// - `error`: the failure description
    public void onFailedToLoad(AdError error) {
    }

    /// Invoked when the ad was presented full screen (or rendered, for banners).
    public void onShown() {
    }

    /// Invoked when the ad failed to present.
    ///
    /// #### Parameters
    ///
    /// - `error`: the failure description
    public void onShowFailed(AdError error) {
    }

    /// Invoked when the full screen ad was dismissed and the app regained focus.
    /// This is the right place to load the next ad and resume the app.
    public void onDismissed() {
    }

    /// Invoked when an impression was recorded for the ad.
    public void onImpression() {
    }

    /// Invoked when the user clicked the ad.
    public void onClicked() {
    }
}
