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

/// The ad formats supported by the modern advertising API. Not every
/// [com.codename1.ads.spi.AdProvider] supports every format; query
/// [AdManager#isSupported(AdFormat)] before requesting one.
public enum AdFormat {
    /// A small rectangular ad that lives inside the component hierarchy. See [BannerAd].
    BANNER,

    /// A full screen ad shown at a natural break (e.g. between game levels). See [InterstitialAd].
    INTERSTITIAL,

    /// An opt-in full screen ad that grants the user a reward for watching. See [RewardedAd].
    REWARDED,

    /// A full screen ad shown on a transition that may grant a reward but is not strictly opt-in.
    /// See [RewardedInterstitialAd].
    REWARDED_INTERSTITIAL,

    /// A full screen ad shown when the application is brought to the foreground. See [AppOpenAd].
    APP_OPEN,

    /// An ad whose assets are rendered by the application using its own components. See [NativeAd].
    NATIVE
}
