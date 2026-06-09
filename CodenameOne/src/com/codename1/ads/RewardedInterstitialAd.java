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

/// A rewarded interstitial: a full screen ad shown at a transition that can
/// grant a reward but, unlike [RewardedAd], is not strictly opt-in. The ad
/// network requires an intro screen with an opt-out, which the underlying SDK
/// presents automatically.
///
/// The API mirrors [RewardedAd]: register an [OnUserEarnedRewardListener] and
/// optionally [#setServerSideVerificationOptions(ServerSideVerificationOptions)].
public class RewardedInterstitialAd extends AbstractFullScreenAd {
    /// Creates a rewarded interstitial for the given ad unit id.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    public RewardedInterstitialAd(String adUnitId) {
        super(AdFormat.REWARDED_INTERSTITIAL, adUnitId);
    }

    /// Sets the reward listener used by [#show()].
    public void setOnUserEarnedRewardListener(OnUserEarnedRewardListener listener) {
        this.rewardListener = listener;
    }

    /// Configures server side verification (SSV) of the reward. Must be set
    /// before [#show()].
    @Override
    public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
        super.setServerSideVerificationOptions(options);
    }

    /// Presents the ad and registers the reward listener in one call.
    ///
    /// #### Parameters
    ///
    /// - `listener`: notified when the reward is earned
    public void show(OnUserEarnedRewardListener listener) {
        this.rewardListener = listener;
        show();
    }
}
