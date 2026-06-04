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
package com.codename1.ads.spi;

import com.codename1.ads.AdConfig;
import com.codename1.ads.AdFormat;
import com.codename1.util.SuccessCallback;

/// The service provider interface implemented by an ad network plugin (such as
/// the Google AdMob cn1lib). A provider is the seam that lets the
/// [com.codename1.ads] API stay network agnostic: AdMob, AppLovin MAX, Unity
/// LevelPlay or a custom mediation layer can each be plugged in by implementing
/// this interface and registering with
/// [com.codename1.ads.AdManager#registerProvider(AdProvider)].
///
/// Most providers are thin bridges that delegate to a
/// [com.codename1.system.NativeInterface] for the per-platform SDK while
/// keeping the cross platform plumbing here. Apps never reference this type
/// directly; they use [com.codename1.ads.AdManager] and the format classes.
///
/// A plugin auto-registers its provider by shipping an
/// [AdProviderInstaller] implementation; see that interface for details.
///
/// @author Shai Almog
public interface AdProvider {
    /// A short human readable provider name, e.g. "AdMob".
    String getName();

    /// True when this provider can run on the current platform.
    boolean isSupported();

    /// True when this provider supports the given ad format.
    boolean isFormatSupported(AdFormat format);

    /// Initializes the underlying SDK. Must invoke the callback (with
    /// `Boolean.TRUE` on success) when initialization completes. Implementations
    /// should be idempotent.
    ///
    /// #### Parameters
    ///
    /// - `config`: the global ad configuration
    /// - `onComplete`: invoked when initialization finishes
    void initialize(AdConfig config, SuccessCallback<Boolean> onComplete);

    /// Creates a session for a full screen ad of the given format. Returns null
    /// if the format is unsupported.
    ///
    /// #### Parameters
    ///
    /// - `format`: one of the full screen formats
    /// - `adUnitId`: the ad unit identifier from the network console
    FullScreenAdSession createFullScreenAd(AdFormat format, String adUnitId);

    /// Creates a banner session of the requested size. Returns null if banners
    /// are unsupported.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    /// - `bannerSize`: one of the size constants in [com.codename1.ads.BannerAd]
    /// - `widthDp`: requested width in display-independent pixels for adaptive
    ///   banners, or 0 to use the available width
    BannerAdSession createBanner(String adUnitId, int bannerSize, int widthDp);

    /// The consent controller for this provider. Returns null if the provider
    /// has no consent management of its own.
    AdConsentController getConsentController();
}
