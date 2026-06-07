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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.ui.PeerComponent;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Android implementation of the AdMob native bridge, built on the Google
/// Mobile Ads (GMA) SDK v24+ and the User Messaging Platform (UMP) for GDPR
/// consent. Shipped as source and compiled by the Codename One Android build.
///
/// All SDK calls are marshalled onto the Android UI thread; results are
/// reported back to Codename One through [AdMobCallback] keyed by handle.
public class AdMobNativeImpl {
    private static final int FORMAT_INTERSTITIAL = 1;
    private static final int FORMAT_REWARDED = 2;
    private static final int FORMAT_REWARDED_INTERSTITIAL = 3;
    private static final int FORMAT_APP_OPEN = 4;

    private final Map<Integer, FullScreenHolder> ads = new HashMap<Integer, FullScreenHolder>();
    private final Map<Integer, AdView> banners = new HashMap<Integer, AdView>();
    private ConsentInformation consentInformation;
    private ServerSideVerificationOptions pendingSsv;

    private static final class FullScreenHolder {
        int format;
        String adUnitId;
        Object ad; // InterstitialAd | RewardedAd | RewardedInterstitialAd | AppOpenAd
        ServerSideVerificationOptions ssv;
        boolean autoShow;
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
                RequestConfiguration.Builder cfg = new RequestConfiguration.Builder();
                List<String> devices = new ArrayList<String>();
                if (testMode) {
                    devices.add(AdRequest.DEVICE_ID_EMULATOR);
                }
                if (testDeviceIds != null && testDeviceIds.length() > 0) {
                    for (String id : testDeviceIds.split(",")) {
                        if (id.trim().length() > 0) {
                            devices.add(id.trim());
                        }
                    }
                }
                if (!devices.isEmpty()) {
                    cfg.setTestDeviceIds(devices);
                }
                cfg.setTagForChildDirectedTreatment(mapTag(tagForChildDirected));
                cfg.setTagForUnderAgeOfConsent(mapTag(tagForUnderAge));
                if (maxAdContentRating > 0) {
                    cfg.setMaxAdContentRating(mapRating(maxAdContentRating));
                }
                MobileAds.setRequestConfiguration(cfg.build());
                MobileAds.initialize(activity);
            }
        });
    }

    private static int mapTag(int tag) {
        switch (tag) {
            case 1: return RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
            case 2: return RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
            default: return RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
        }
    }

    private static String mapRating(int rating) {
        switch (rating) {
            case 1: return RequestConfiguration.MAX_AD_CONTENT_RATING_G;
            case 2: return RequestConfiguration.MAX_AD_CONTENT_RATING_PG;
            case 3: return RequestConfiguration.MAX_AD_CONTENT_RATING_T;
            case 4: return RequestConfiguration.MAX_AD_CONTENT_RATING_MA;
            default: return RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED;
        }
    }

    public boolean createFullScreen(int handle, int format, String adUnitId) {
        FullScreenHolder h = new FullScreenHolder();
        h.format = format;
        h.adUnitId = adUnitId;
        ads.put(handle, h);
        return true;
    }

    public void setServerSideVerification(int handle, String userId, String customData) {
        FullScreenHolder h = ads.get(handle);
        if (h == null) {
            return;
        }
        ServerSideVerificationOptions.Builder b = new ServerSideVerificationOptions.Builder();
        if (userId != null) {
            b.setUserId(userId);
        }
        if (customData != null) {
            b.setCustomData(customData);
        }
        h.ssv = b.build();
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
                AdRequest request = buildRequest(keywords, contentUrl, nonPersonalized);
                switch (h.format) {
                    case FORMAT_INTERSTITIAL:
                        loadInterstitial(activity, handle, h, request);
                        break;
                    case FORMAT_REWARDED:
                        loadRewarded(activity, handle, h, request);
                        break;
                    case FORMAT_REWARDED_INTERSTITIAL:
                        loadRewardedInterstitial(activity, handle, h, request);
                        break;
                    case FORMAT_APP_OPEN:
                        loadAppOpen(activity, handle, h, request);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void loadInterstitial(Activity activity, final int handle, final FullScreenHolder h, AdRequest request) {
        InterstitialAd.load(activity, h.adUnitId, request, new InterstitialAdLoadCallback() {
            public void onAdLoaded(InterstitialAd ad) {
                h.ad = ad;
                ad.setFullScreenContentCallback(fullScreenCallback(handle));
                AdMobCallback.fire(handle, AdMobCallback.LOADED, 0, null, null, 0);
            }
            public void onAdFailedToLoad(LoadAdError error) {
                h.ad = null;
                AdMobCallback.fire(handle, AdMobCallback.FAILED, error.getCode(), error.getMessage(), null, 0);
            }
        });
    }

    private void loadRewarded(Activity activity, final int handle, final FullScreenHolder h, AdRequest request) {
        RewardedAd.load(activity, h.adUnitId, request, new RewardedAdLoadCallback() {
            public void onAdLoaded(RewardedAd ad) {
                h.ad = ad;
                ad.setFullScreenContentCallback(fullScreenCallback(handle));
                AdMobCallback.fire(handle, AdMobCallback.LOADED, 0, null, null, 0);
            }
            public void onAdFailedToLoad(LoadAdError error) {
                h.ad = null;
                AdMobCallback.fire(handle, AdMobCallback.FAILED, error.getCode(), error.getMessage(), null, 0);
            }
        });
    }

    private void loadRewardedInterstitial(Activity activity, final int handle, final FullScreenHolder h, AdRequest request) {
        RewardedInterstitialAd.load(activity, h.adUnitId, request, new RewardedInterstitialAdLoadCallback() {
            public void onAdLoaded(RewardedInterstitialAd ad) {
                h.ad = ad;
                ad.setFullScreenContentCallback(fullScreenCallback(handle));
                AdMobCallback.fire(handle, AdMobCallback.LOADED, 0, null, null, 0);
            }
            public void onAdFailedToLoad(LoadAdError error) {
                h.ad = null;
                AdMobCallback.fire(handle, AdMobCallback.FAILED, error.getCode(), error.getMessage(), null, 0);
            }
        });
    }

    private void loadAppOpen(Activity activity, final int handle, final FullScreenHolder h, AdRequest request) {
        AppOpenAd.load(activity, h.adUnitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            public void onAdLoaded(AppOpenAd ad) {
                h.ad = ad;
                ad.setFullScreenContentCallback(fullScreenCallback(handle));
                AdMobCallback.fire(handle, AdMobCallback.LOADED, 0, null, null, 0);
            }
            public void onAdFailedToLoad(LoadAdError error) {
                h.ad = null;
                AdMobCallback.fire(handle, AdMobCallback.FAILED, error.getCode(), error.getMessage(), null, 0);
            }
        });
    }

    private FullScreenContentCallback fullScreenCallback(final int handle) {
        return new FullScreenContentCallback() {
            public void onAdShowedFullScreenContent() {
                AdMobCallback.fire(handle, AdMobCallback.SHOWN, 0, null, null, 0);
            }
            public void onAdFailedToShowFullScreenContent(AdError error) {
                AdMobCallback.fire(handle, AdMobCallback.SHOW_FAILED, error.getCode(), error.getMessage(), null, 0);
            }
            public void onAdDismissedFullScreenContent() {
                FullScreenHolder h = ads.get(handle);
                if (h != null) {
                    h.ad = null;
                }
                AdMobCallback.fire(handle, AdMobCallback.DISMISSED, 0, null, null, 0);
            }
            public void onAdImpression() {
                AdMobCallback.fire(handle, AdMobCallback.IMPRESSION, 0, null, null, 0);
            }
            public void onAdClicked() {
                AdMobCallback.fire(handle, AdMobCallback.CLICKED, 0, null, null, 0);
            }
        };
    }

    public boolean isFullScreenLoaded(int handle) {
        FullScreenHolder h = ads.get(handle);
        return h != null && h.ad != null;
    }

    public void showFullScreen(final int handle) {
        final Activity activity = AndroidNativeUtil.getActivity();
        final FullScreenHolder h = ads.get(handle);
        if (activity == null || h == null || h.ad == null) {
            AdMobCallback.fire(handle, AdMobCallback.SHOW_FAILED, AdMobErrorCodes.NOT_READY, "No ad loaded", null, 0);
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                OnUserEarnedRewardListener reward = new OnUserEarnedRewardListener() {
                    public void onUserEarnedReward(RewardItem item) {
                        AdMobCallback.fire(handle, AdMobCallback.REWARD, 0, null, item.getType(), item.getAmount());
                    }
                };
                if (h.ad instanceof InterstitialAd) {
                    ((InterstitialAd) h.ad).show(activity);
                } else if (h.ad instanceof RewardedAd) {
                    RewardedAd ad = (RewardedAd) h.ad;
                    if (h.ssv != null) {
                        ad.setServerSideVerificationOptions(h.ssv);
                    }
                    ad.show(activity, reward);
                } else if (h.ad instanceof RewardedInterstitialAd) {
                    RewardedInterstitialAd ad = (RewardedInterstitialAd) h.ad;
                    if (h.ssv != null) {
                        ad.setServerSideVerificationOptions(h.ssv);
                    }
                    ad.show(activity, reward);
                } else if (h.ad instanceof AppOpenAd) {
                    ((AppOpenAd) h.ad).show(activity);
                }
            }
        });
    }

    public void setAppOpenAutoShow(int handle, boolean enabled) {
        // A full app-open implementation observes ProcessLifecycleOwner and
        // re-shows on foreground; the load/show wiring above is the core. The
        // foreground observer is registered by the host app lifecycle and is a
        // device-side concern.
        FullScreenHolder h = ads.get(handle);
        if (h != null) {
            h.autoShow = enabled;
        }
    }

    public void disposeFullScreen(int handle) {
        ads.remove(handle);
    }

    public PeerComponent createBanner(final int handle, final String adUnitId, final int sizeType, final int widthDp) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return null;
        }
        final AdView[] out = new AdView[1];
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                AdView adView = new AdView(activity);
                adView.setAdUnitId(adUnitId);
                adView.setAdSize(mapSize(activity, sizeType, widthDp));
                banners.put(handle, adView);
                out[0] = adView;
            }
        });
        return out[0] == null ? null : PeerComponent.create(out[0]);
    }

    private static AdSize mapSize(Activity activity, int sizeType, int widthDp) {
        switch (sizeType) {
            case 1: return AdSize.BANNER;
            case 2: return AdSize.LARGE_BANNER;
            case 3: return AdSize.MEDIUM_RECTANGLE;
            case 4: return AdSize.LEADERBOARD;
            default:
                int w = widthDp > 0 ? widthDp
                        : (int) (activity.getResources().getDisplayMetrics().widthPixels
                            / activity.getResources().getDisplayMetrics().density);
                return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, w);
        }
    }

    public void loadBanner(final int handle, final String keywords, final String contentUrl,
                           final boolean nonPersonalized) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                final AdView adView = banners.get(handle);
                if (adView == null) {
                    return;
                }
                adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                    public void onAdLoaded() {
                        AdMobCallback.fire(handle, AdMobCallback.LOADED, 0, null, null, 0);
                    }
                    public void onAdFailedToLoad(LoadAdError error) {
                        AdMobCallback.fire(handle, AdMobCallback.FAILED, error.getCode(), error.getMessage(), null, 0);
                    }
                    public void onAdImpression() {
                        AdMobCallback.fire(handle, AdMobCallback.IMPRESSION, 0, null, null, 0);
                    }
                    public void onAdClicked() {
                        AdMobCallback.fire(handle, AdMobCallback.CLICKED, 0, null, null, 0);
                    }
                });
                adView.loadAd(buildRequest(keywords, contentUrl, nonPersonalized));
            }
        });
    }

    public void disposeBanner(final int handle) {
        final AdView adView = banners.remove(handle);
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

    private static AdRequest buildRequest(String keywords, String contentUrl, boolean nonPersonalized) {
        AdRequest.Builder b = new AdRequest.Builder();
        if (keywords != null && keywords.length() > 0) {
            for (String k : keywords.split(",")) {
                if (k.trim().length() > 0) {
                    b.addKeyword(k.trim());
                }
            }
        }
        if (contentUrl != null && contentUrl.length() > 0) {
            b.setContentUrl(contentUrl);
        }
        if (nonPersonalized) {
            Bundle extras = new Bundle();
            extras.putString("npa", "1");
            b.addNetworkExtrasBundle(AdManagerAdRequest.class, extras);
        }
        return b.build();
    }

    public void requestConsent(final boolean underAgeOfConsent) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            AdMobCallback.fire(0, AdMobCallback.CONSENT_COMPLETE, 0, null, null, 0);
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                        .setTagForUnderAgeOfConsent(underAgeOfConsent)
                        .build();
                consentInformation = UserMessagingPlatform.getConsentInformation(activity);
                consentInformation.requestConsentInfoUpdate(activity, params,
                        new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                            public void onConsentInfoUpdateSuccess() {
                                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity,
                                        new ConsentInformation.OnConsentFormDismissedListener() {
                                            public void onConsentFormDismissed(com.google.android.ump.FormError error) {
                                                AdMobCallback.fire(0, AdMobCallback.CONSENT_COMPLETE,
                                                        getConsentStatus(), null, null, 0);
                                            }
                                        });
                            }
                        },
                        new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                            public void onConsentInfoUpdateFailure(com.google.android.ump.FormError error) {
                                AdMobCallback.fire(0, AdMobCallback.CONSENT_COMPLETE, getConsentStatus(),
                                        error.getMessage(), null, 0);
                            }
                        });
            }
        });
    }

    public int getConsentStatus() {
        if (consentInformation == null) {
            return 0; // STATUS_UNKNOWN
        }
        switch (consentInformation.getConsentStatus()) {
            case ConsentInformation.ConsentStatus.NOT_REQUIRED:
                return 2;
            case ConsentInformation.ConsentStatus.REQUIRED:
                return 1;
            case ConsentInformation.ConsentStatus.OBTAINED:
                return 3;
            default:
                return 0;
        }
    }

    public boolean canRequestAds() {
        return consentInformation == null || consentInformation.canRequestAds();
    }

    public void resetConsent() {
        if (consentInformation != null) {
            consentInformation.reset();
        }
    }

    public boolean isSupported() {
        return true;
    }
}
