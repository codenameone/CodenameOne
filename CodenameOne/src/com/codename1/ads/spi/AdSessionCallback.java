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

import com.codename1.ads.AdError;
import com.codename1.ads.RewardItem;

/// The callback a [AdProvider] uses to report ad lifecycle events back to the
/// Codename One ads layer. Providers may invoke these methods from any thread;
/// the ads layer marshals them onto the EDT before forwarding to the public
/// [com.codename1.ads.AdListener].
///
/// This is an internal SPI type; applications use [com.codename1.ads.AdListener].
public interface AdSessionCallback {
    /// The ad finished loading.
    void onLoaded();

    /// The ad failed to load.
    void onFailedToLoad(AdError error);

    /// The ad was presented.
    void onShown();

    /// The ad failed to present.
    void onShowFailed(AdError error);

    /// The ad was dismissed.
    void onDismissed();

    /// An impression was recorded.
    void onImpression();

    /// The ad was clicked.
    void onClicked();

    /// The user earned a reward (rewarded formats only).
    void onUserEarnedReward(RewardItem reward);
}
