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
package com.codename1.ads.applovin;

import android.app.Activity;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.ui.PeerComponent;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.sdk.AppLovinSdk;

import java.util.HashMap;
import java.util.Map;

/// Android implementation of the AppLovin MAX native bridge. Shipped as source
/// and compiled by the Codename One Android build; validated on device.
public class AppLovinNativeImpl {
    private static final int FORMAT_INTERSTITIAL = 1;
    private static final int FORMAT_REWARDED = 2;
    private static final int FORMAT_REWARDED_INTERSTITIAL = 3;
    private static final int FORMAT_APP_OPEN = 4;

    private final Map<Integer, FullScreenHolder> ads = new HashMap<Integer, FullScreenHolder>();
    private final Map<Integer, MaxAdView> banners = new HashMap<Integer, MaxAdView>();

    private static final class FullScreenHolder {
        int format;
        String adUnitId;
        Object ad; // MaxInterstitialAd | MaxRewardedAd | MaxAppOpenAd
        boolean loaded;
    }

    public void initialize(final String testDeviceIds, final boolean testMode,
                           final int tagForChildDirected, final int tagForUnderAge,
                           final int maxAdContentRating) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                AppLovinSdk sdk = AppLovinSdk.getInstance(activity);
                sdk.setMediationProvider("max");
                sdk.initializeSdk(config -> { });
            }
        });
    }

    public boolean createFullScreen(int handle, int format, String adUnitId) {
        FullScreenHolder h = new FullScreenHolder();
        h.format = format;
        h.adUnitId = adUnitId;
        ads.put(handle, h);
        return format != FORMAT_REWARDED_INTERSTITIAL; // MAX has no rewarded-interstitial
    }

    public void setServerSideVerification(int handle, String userId, String customData) {
        // MAX exposes SSV via per-network extra params; not wired in this bridge.
    }

    public void loadFullScreen(final int handle, final String keywords, final String contentUrl,
                               final boolean nonPersonalized) {
        final Activity activity = AndroidNativeUtil.getActivity();
        final FullScreenHolder h = ads.get(handle);
        if (activity == null || h == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                switch (h.format) {
                    case FORMAT_INTERSTITIAL: {
                        MaxInterstitialAd ad = new MaxInterstitialAd(h.adUnitId, activity);
                        ad.setListener(interstitialListener(handle, h));
                        h.ad = ad;
                        ad.loadAd();
                        break;
                    }
                    case FORMAT_REWARDED: {
                        MaxRewardedAd ad = MaxRewardedAd.getInstance(h.adUnitId, activity);
                        ad.setListener(rewardedListener(handle, h));
                        h.ad = ad;
                        ad.loadAd();
                        break;
                    }
                    case FORMAT_APP_OPEN: {
                        MaxAppOpenAd ad = new MaxAppOpenAd(h.adUnitId, activity);
                        ad.setListener(interstitialListener(handle, h));
                        h.ad = ad;
                        ad.loadAd();
                        break;
                    }
                    default:
                        break;
                }
            }
        });
    }

    private MaxAdListener interstitialListener(final int handle, final FullScreenHolder h) {
        return new MaxAdListener() {
            public void onAdLoaded(MaxAd ad) { h.loaded = true; AppLovinCallback.fire(handle, AppLovinCallback.LOADED, 0, null, null, 0); }
            public void onAdLoadFailed(String unit, MaxError e) { AppLovinCallback.fire(handle, AppLovinCallback.FAILED, e.getCode(), e.getMessage(), null, 0); }
            public void onAdDisplayed(MaxAd ad) { AppLovinCallback.fire(handle, AppLovinCallback.SHOWN, 0, null, null, 0); AppLovinCallback.fire(handle, AppLovinCallback.IMPRESSION, 0, null, null, 0); }
            public void onAdDisplayFailed(MaxAd ad, MaxError e) { AppLovinCallback.fire(handle, AppLovinCallback.SHOW_FAILED, e.getCode(), e.getMessage(), null, 0); }
            public void onAdClicked(MaxAd ad) { AppLovinCallback.fire(handle, AppLovinCallback.CLICKED, 0, null, null, 0); }
            public void onAdHidden(MaxAd ad) { h.loaded = false; AppLovinCallback.fire(handle, AppLovinCallback.DISMISSED, 0, null, null, 0); }
        };
    }

    private MaxRewardedAdListener rewardedListener(final int handle, final FullScreenHolder h) {
        return new MaxRewardedAdListener() {
            public void onAdLoaded(MaxAd ad) { h.loaded = true; AppLovinCallback.fire(handle, AppLovinCallback.LOADED, 0, null, null, 0); }
            public void onAdLoadFailed(String unit, MaxError e) { AppLovinCallback.fire(handle, AppLovinCallback.FAILED, e.getCode(), e.getMessage(), null, 0); }
            public void onAdDisplayed(MaxAd ad) { AppLovinCallback.fire(handle, AppLovinCallback.SHOWN, 0, null, null, 0); AppLovinCallback.fire(handle, AppLovinCallback.IMPRESSION, 0, null, null, 0); }
            public void onAdDisplayFailed(MaxAd ad, MaxError e) { AppLovinCallback.fire(handle, AppLovinCallback.SHOW_FAILED, e.getCode(), e.getMessage(), null, 0); }
            public void onAdClicked(MaxAd ad) { AppLovinCallback.fire(handle, AppLovinCallback.CLICKED, 0, null, null, 0); }
            public void onAdHidden(MaxAd ad) { h.loaded = false; AppLovinCallback.fire(handle, AppLovinCallback.DISMISSED, 0, null, null, 0); }
            public void onUserRewarded(MaxAd ad, MaxReward reward) { AppLovinCallback.fire(handle, AppLovinCallback.REWARD, 0, null, reward.getLabel(), reward.getAmount()); }
        };
    }

    public boolean isFullScreenLoaded(int handle) {
        FullScreenHolder h = ads.get(handle);
        return h != null && h.loaded;
    }

    public void showFullScreen(final int handle) {
        final Activity activity = AndroidNativeUtil.getActivity();
        final FullScreenHolder h = ads.get(handle);
        if (activity == null || h == null || h.ad == null) {
            AppLovinCallback.fire(handle, AppLovinCallback.SHOW_FAILED, AppLovinErrorCodes.NOT_READY, "No ad loaded", null, 0);
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (h.ad instanceof MaxInterstitialAd) {
                    ((MaxInterstitialAd) h.ad).showAd();
                } else if (h.ad instanceof MaxRewardedAd) {
                    ((MaxRewardedAd) h.ad).showAd();
                } else if (h.ad instanceof MaxAppOpenAd) {
                    ((MaxAppOpenAd) h.ad).showAd();
                }
            }
        });
    }

    public void setAppOpenAutoShow(int handle, boolean enabled) {
        // The reload-on-foreground observer is a device-side lifecycle concern.
    }

    public void disposeFullScreen(int handle) {
        ads.remove(handle);
    }

    public PeerComponent createBanner(final int handle, final String adUnitId, final int sizeType, final int widthDp) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return null;
        }
        final MaxAdView[] out = new MaxAdView[1];
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                MaxAdView adView = new MaxAdView(adUnitId, activity);
                banners.put(handle, adView);
                out[0] = adView;
            }
        });
        return out[0] == null ? null : PeerComponent.create(out[0]);
    }

    public void loadBanner(final int handle, final String keywords, final String contentUrl, final boolean nonPersonalized) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                final MaxAdView adView = banners.get(handle);
                if (adView == null) {
                    return;
                }
                adView.setListener(new MaxAdViewAdListener() {
                    public void onAdLoaded(MaxAd ad) { AppLovinCallback.fire(handle, AppLovinCallback.LOADED, 0, null, null, 0); }
                    public void onAdLoadFailed(String unit, MaxError e) { AppLovinCallback.fire(handle, AppLovinCallback.FAILED, e.getCode(), e.getMessage(), null, 0); }
                    public void onAdDisplayed(MaxAd ad) { AppLovinCallback.fire(handle, AppLovinCallback.IMPRESSION, 0, null, null, 0); }
                    public void onAdDisplayFailed(MaxAd ad, MaxError e) { }
                    public void onAdClicked(MaxAd ad) { AppLovinCallback.fire(handle, AppLovinCallback.CLICKED, 0, null, null, 0); }
                    public void onAdHidden(MaxAd ad) { }
                    public void onAdExpanded(MaxAd ad) { }
                    public void onAdCollapsed(MaxAd ad) { }
                });
                adView.loadAd();
            }
        });
    }

    public void disposeBanner(final int handle) {
        final MaxAdView adView = banners.remove(handle);
        if (adView == null) {
            return;
        }
        Activity activity = AndroidNativeUtil.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adView.destroy();
                }
            });
        }
    }

    public void requestConsent(boolean underAgeOfConsent) {
        // AppLovin reads consent from its CMP / the IAB TCF string; report
        // "not required" so the cross-platform flow can proceed.
        AppLovinCallback.fire(0, AppLovinCallback.CONSENT_COMPLETE, 2, null, null, 0);
    }

    public int getConsentStatus() {
        return 2; // STATUS_NOT_REQUIRED
    }

    public boolean canRequestAds() {
        return true;
    }

    public void resetConsent() {
    }

    public boolean isSupported() {
        return true;
    }
}
