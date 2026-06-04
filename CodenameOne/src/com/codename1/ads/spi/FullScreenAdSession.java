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

import com.codename1.ads.AdRequest;
import com.codename1.ads.ServerSideVerificationOptions;

/// A provider's handle for a single full screen ad (interstitial, rewarded,
/// rewarded interstitial or app open). Created by
/// [AdProvider#createFullScreenAd(com.codename1.ads.AdFormat, String)] and
/// driven by the public format classes in [com.codename1.ads].
///
/// This is an internal SPI type.
public interface FullScreenAdSession {
    /// Registers the callback used to report lifecycle events. Called once
    /// immediately after creation.
    void setCallback(AdSessionCallback callback);

    /// Sets the server side verification options for rewarded formats. Optional;
    /// providers that do not support SSV may ignore this.
    void setServerSideVerificationOptions(ServerSideVerificationOptions options);

    /// Begins loading an ad. The request may be null for a default load.
    void load(AdRequest request);

    /// True if an ad is loaded and ready to show.
    boolean isLoaded();

    /// Presents the loaded ad. Does nothing if no ad is loaded.
    void show();

    /// For app open ads only: enables the provider managed auto-show on
    /// foreground. No-op for other formats.
    void setAutoShowOnForeground(boolean enabled);

    /// Releases native resources held by this session.
    void dispose();
}
