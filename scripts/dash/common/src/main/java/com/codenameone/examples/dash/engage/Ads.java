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
package com.codenameone.examples.dash.engage;

import com.codename1.ads.AdConfig;
import com.codename1.ads.AdError;
import com.codename1.ads.AdFormat;
import com.codename1.ads.AdListener;
import com.codename1.ads.AdManager;
import com.codename1.ads.BannerAd;
import com.codename1.ads.InterstitialAd;
import com.codename1.ads.RewardedAd;
import com.codename1.ads.ServerSideVerificationOptions;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codenameone.examples.dash.AppConfig;
import com.codenameone.examples.dash.net.Session;

/// Advertising, through `com.codename1.ads`. Three placements, each the kind
/// players accept in a free game:
///
/// - **Rewarded**: opt in, watch, get coins. The server grants the coins and
///   caps them per day, so a modified client gains nothing.
/// - **Interstitial**: between runs, at most every third one, never
///   during play and never for a Royal Pass holder.
/// - **Banner**: on the home screen only.
///
/// The ad network is a cn1lib (AdMob, AppLovin, ...) registered with
/// [AdManager]; this class names none of them. Where no network is installed
/// -- the simulator, the web build, a fork that has not added one yet -- a
/// rewarded ad is a short stand-in dialog, so the reward flow can be built and
/// tested end to end, and the other placements are simply absent.
// tag::dash-ads[]
public final class Ads {
    private static final int INTERSTITIAL_EVERY = 3;
    private static int runsSinceAd;

    private Ads() {
    }

    public static void start() {
        if (AdManager.getProvider() != null) {
            AdManager.initialize(new AdConfig().testMode(true), ready -> { });
        }
    }

    /// Plays a rewarded ad and runs `granted` once the network says it was
    /// watched to the end.
    public static void rewarded(final Runnable granted, final Runnable unavailable) {
        Telemetry.adWatched("rewarded_offer");
        if (!AdManager.isInitialized() || !AdManager.isSupported(AdFormat.REWARDED)) {
            standIn(granted);
            return;
        }
        final RewardedAd ad = new RewardedAd(AppConfig.REWARDED_AD_UNIT);
        // Server-side verification: the network calls the game's server with
        // this user id, so a reward could be granted there and nowhere else.
        String who = Session.user() == null ? "" : Session.user().username();
        ad.setServerSideVerificationOptions(new ServerSideVerificationOptions(who, "coins"));
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

    /// Called after every run; shows an interstitial every few.
    public static void betweenRuns(boolean adFree) {
        runsSinceAd++;
        if (adFree || runsSinceAd < INTERSTITIAL_EVERY || !AdManager.isInitialized()
                || !AdManager.isSupported(AdFormat.INTERSTITIAL)) {
            return;
        }
        runsSinceAd = 0;
        final InterstitialAd ad = new InterstitialAd(AppConfig.INTERSTITIAL_AD_UNIT);
        ad.setAdListener(new AdListener() {
            @Override
            public void onLoaded() {
                ad.show();
            }
        });
        ad.load();
    }

    /// A banner for the home screen, or null where there is no network.
    public static Component banner(boolean adFree) {
        if (adFree || !AdManager.isInitialized() || !AdManager.isSupported(AdFormat.BANNER)) {
            return null;
        }
        BannerAd banner = new BannerAd(AppConfig.BANNER_AD_UNIT);
        banner.load();
        return banner;
    }

    private static void standIn(final Runnable granted) {
        Dialog d = new Dialog("Sponsored", new com.codename1.ui.layouts.BorderLayout());
        d.setUIID("AdDialog");
        d.add(com.codename1.ui.layouts.BorderLayout.CENTER,
                new com.codename1.components.SpanLabel("An ad would play here. Install an ad network cn1lib "
                        + "to show real ones."));
        d.setTimeout(1500);
        d.show();
        granted.run();
    }
}
// end::dash-ads[]
