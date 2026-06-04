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

/// A full screen interstitial ad shown at natural break points such as between
/// game levels or activities.
///
/// ```java
/// InterstitialAd ad = new InterstitialAd("ca-app-pub-xxx/yyy");
/// ad.setAdListener(new AdListener() {
///     public void onLoaded() { ad.show(); }
///     public void onDismissed() { ad.load(); } // preload the next one
/// });
/// ad.load();
/// ```
///
/// All callbacks are delivered on the EDT. You can also let Codename One show an
/// interstitial automatically on screen transitions with
/// [AdManager#bindInterstitialOnTransition(InterstitialAd, int)].
///
/// @author Shai Almog
public class InterstitialAd extends AbstractFullScreenAd {
    /// Creates an interstitial for the given ad unit id.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    public InterstitialAd(String adUnitId) {
        super(AdFormat.INTERSTITIAL, adUnitId);
    }
}
