/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.engage;

import com.codename1.ads.AdConfig;
import com.codename1.ads.AdError;
import com.codename1.ads.AdFormat;
import com.codename1.ads.AdListener;
import com.codename1.ads.AdManager;
import com.codename1.ads.RewardedAd;
import com.codename1.ads.ServerSideVerificationOptions;
import com.codename1.ui.CN;
import com.codenameone.examples.brickyard.AppConfig;
import com.codenameone.examples.brickyard.net.Session;

/// A rewarded ad for free Bricks, through `com.codename1.ads`. Offered only
/// when an ad network cn1lib is installed and supports the format; the shop
/// hides the offer otherwise.
public final class Ads {
    private Ads() {
    }

    public static void start() {
        if (AdManager.getProvider() != null) {
            AdManager.initialize(new AdConfig().testMode(true), ready -> { });
        }
    }

    public static boolean rewardedAvailable() {
        return AdManager.isInitialized() && AdManager.isSupported(AdFormat.REWARDED);
    }

    /// Plays a rewarded ad. The reward itself is credited by the ad network's
    /// server-side verification callback, keyed by the account name.
    public static void rewarded(final Runnable granted, final Runnable unavailable) {
        if (!rewardedAvailable()) {
            unavailable.run();
            return;
        }
        Telemetry.adWatched("free_bricks");
        final RewardedAd ad = new RewardedAd(AppConfig.REWARDED_AD_UNIT);
        String who = Session.user() == null ? "" : Session.user().username();
        ad.setServerSideVerificationOptions(new ServerSideVerificationOptions(who, "bricks"));
        ad.setAdListener(new AdListener() {
            @Override
            public void onLoaded() {
                ad.show(reward -> CN.callSerially(granted));
            }

            @Override
            public void onFailedToLoad(AdError error) {
                CN.callSerially(unavailable);
            }
        });
        ad.load();
    }
}
