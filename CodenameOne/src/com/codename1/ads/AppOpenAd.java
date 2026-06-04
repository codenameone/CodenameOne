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

import com.codename1.ads.spi.FullScreenAdSession;

/// A full screen ad shown while the application is being brought to the
/// foreground, overlaying the launch/splash experience.
///
/// App open ads are loaded ahead of time and shown when the user returns to the
/// app. The recommended way to use them is to let Codename One and the provider
/// manage the foreground hook for you:
///
/// ```java
/// AppOpenAd ad = new AppOpenAd("ca-app-pub-xxx/yyy");
/// AdManager.enableAppOpenAds(ad); // loads and auto-shows on foreground
/// ```
///
/// Foreground detection is performed natively by the provider (Android process
/// lifecycle / iOS `applicationDidBecomeActive`), which also honours the ad's
/// freshness window (an app open ad expires a few hours after loading).
///
/// @author Shai Almog
public class AppOpenAd extends AbstractFullScreenAd {
    /// Creates an app open ad for the given ad unit id.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    public AppOpenAd(String adUnitId) {
        super(AdFormat.APP_OPEN, adUnitId);
    }

    /// Enables or disables the provider managed auto-show when the app returns
    /// to the foreground. Normally driven by
    /// [AdManager#enableAppOpenAds(AppOpenAd)].
    ///
    /// #### Parameters
    ///
    /// - `enabled`: true to auto-show on foreground
    public void setAutoShowOnForeground(boolean enabled) {
        FullScreenAdSession session = getSession();
        if (session != null) {
            session.setAutoShowOnForeground(enabled);
        }
    }
}
