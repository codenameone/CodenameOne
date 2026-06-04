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

/// An opt-in full screen ad that grants the user a reward for watching it to
/// completion. Pass the [OnUserEarnedRewardListener] to [#show(OnUserEarnedRewardListener)]
/// to be notified when the reward is earned.
///
/// ```java
/// RewardedAd ad = new RewardedAd("ca-app-pub-xxx/yyy");
/// ad.setAdListener(new AdListener() {
///     public void onLoaded() {
///         ad.show(new OnUserEarnedRewardListener() {
///             public void onUserEarnedReward(RewardItem r) {
///                 grantCoins(r.getAmount());
///             }
///         });
///     }
/// });
/// ad.load();
/// ```
///
/// For valuable rewards, verify server side with
/// [#setServerSideVerificationOptions(ServerSideVerificationOptions)] rather
/// than trusting the client callback.
public class RewardedAd extends AbstractFullScreenAd {
    /// Creates a rewarded ad for the given ad unit id.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    public RewardedAd(String adUnitId) {
        super(AdFormat.REWARDED, adUnitId);
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
