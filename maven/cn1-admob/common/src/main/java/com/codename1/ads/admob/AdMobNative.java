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
package com.codename1.ads.admob;

import com.codename1.system.NativeInterface;
import com.codename1.ui.PeerComponent;

/// The native bridge to the Google Mobile Ads (GMA) SDK, implemented per
/// platform (Android, iOS, and a JavaSE placeholder). Each ad is addressed by
/// an integer handle allocated by [AdMobProvider]; lifecycle events flow back
/// asynchronously through [AdMobCallback] keyed by the same handle.
public interface AdMobNative extends NativeInterface {
    /// Initializes the GMA SDK. The flags mirror [com.codename1.ads.AdConfig].
    void initialize(String testDeviceIds, boolean testMode, int tagForChildDirected,
                    int tagForUnderAge, int maxAdContentRating);

    /// Creates a full screen ad of the given format (see the `FORMAT_*`
    /// constants in [AdMobProvider]) bound to `handle`. Returns false if the
    /// format is unsupported on this platform.
    boolean createFullScreen(int handle, int format, String adUnitId);

    /// Sets the server side verification options for a rewarded ad.
    void setServerSideVerification(int handle, String userId, String customData);

    /// Begins loading the full screen ad bound to `handle`.
    void loadFullScreen(int handle, String keywords, String contentUrl, boolean nonPersonalized);

    /// True if the full screen ad bound to `handle` is loaded and ready.
    boolean isFullScreenLoaded(int handle);

    /// Presents the full screen ad bound to `handle`.
    void showFullScreen(int handle);

    /// Enables provider managed auto-show on foreground for an app open ad.
    void setAppOpenAutoShow(int handle, boolean enabled);

    /// Releases the full screen ad bound to `handle`.
    void disposeFullScreen(int handle);

    /// Creates a native banner view bound to `handle` and returns it as a peer
    /// component. `sizeType` matches the `SIZE_*` constants in
    /// [com.codename1.ads.BannerAd]; `widthDp` is the adaptive width, or 0 for
    /// the available width.
    PeerComponent createBanner(int handle, String adUnitId, int sizeType, int widthDp);

    /// Begins loading the banner bound to `handle`.
    void loadBanner(int handle, String keywords, String contentUrl, boolean nonPersonalized);

    /// Releases the banner bound to `handle`.
    void disposeBanner(int handle);

    /// Gathers GDPR consent (and, on iOS, presents the App Tracking
    /// Transparency prompt). Completion is reported through [AdMobCallback]
    /// with handle 0 and event [AdMobCallback#CONSENT_COMPLETE].
    void requestConsent(boolean underAgeOfConsent);

    /// The current consent status, mirroring the `STATUS_*` constants in
    /// [com.codename1.ads.AdConsent].
    int getConsentStatus();

    /// True when there is enough consent to request ads.
    boolean canRequestAds();

    /// Clears stored consent (testing only).
    void resetConsent();
}
