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
package com.codename1.ads.levelplay;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;

import com.unity3d.mediation.LevelPlay;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdSize;
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayInitError;
import com.unity3d.mediation.LevelPlayInitListener;
import com.unity3d.mediation.LevelPlayInitRequest;
import com.unity3d.mediation.LevelPlayPrivacySettings;
import com.unity3d.mediation.banner.LevelPlayBannerAdView;
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

import java.util.HashMap;
import java.util.Map;

/// Android implementation of the Unity LevelPlay (ironSource) native bridge.
/// Shipped as source and compiled by the Codename One Android build, and by
/// scripts/check-cn1lib-android-api.py against the SDK pinned in
/// codenameone_library_required.properties.
///
/// Written against the unified LevelPlay API (LevelPlayInterstitialAd,
/// LevelPlayRewardedAd, LevelPlayBannerAdView), where every ad is an object
/// bound to one ad unit. The older IronSource entry points this bridge used
/// were singletons per format, which forced it to route callbacks through a
/// "currently active handle" field and lose events whenever two ads of the same
/// format were in flight; one ad object per handle removes that.
public class LevelPlayNativeImpl {
    private static final int FORMAT_INTERSTITIAL = 1;
    private static final int FORMAT_REWARDED = 2;

    private final Map<Integer, FullScreenHolder> ads = new HashMap<Integer, FullScreenHolder>();
    private final Map<Integer, LevelPlayBannerAdView> banners = new HashMap<Integer, LevelPlayBannerAdView>();

    private static final class FullScreenHolder {
        int format;
        LevelPlayInterstitialAd interstitial;
        LevelPlayRewardedAd rewarded;
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
                // LevelPlay has no test device list: test ads are switched on
                // per ad unit in the dashboard, and the test suite is opened
                // from the app rather than by a flag here. So testMode and
                // testDeviceIds have no counterpart on this platform.
                if (tagForChildDirected == 1) {
                    LevelPlayPrivacySettings.setCOPPA(true);
                } else if (tagForChildDirected == 2) {
                    LevelPlayPrivacySettings.setCOPPA(false);
                }
                LevelPlayInitRequest request =
                        new LevelPlayInitRequest.Builder(readAppKey(activity)).build();
                LevelPlay.init(activity, request, new LevelPlayInitListener() {
                    public void onInitSuccess(LevelPlayConfiguration configuration) {
                    }

                    public void onInitFailed(LevelPlayInitError error) {
                        LevelPlayCallback.fire(0, LevelPlayCallback.FAILED,
                                error.getErrorCode(), error.getErrorMessage(), null, 0);
                    }
                });
            }
        });
    }

    private static String readAppKey(Activity activity) {
        try {
            ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(
                    activity.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                return ai.metaData.getString("levelplay.app.key");
            }
        } catch (Throwable t) {
            // fall through
        }
        return "";
    }

    public boolean createFullScreen(final int handle, final int format, final String adUnitId) {
        if (format != FORMAT_INTERSTITIAL && format != FORMAT_REWARDED) {
            // LevelPlay has no dedicated app-open or rewarded-interstitial
            // format, and the provider expects false rather than an ad object
            // that never loads.
            return false;
        }
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return false;
        }
        final FullScreenHolder holder = new FullScreenHolder();
        holder.format = format;
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                if (format == FORMAT_REWARDED) {
                    LevelPlayRewardedAd ad = new LevelPlayRewardedAd(adUnitId);
                    ad.setListener(new LevelPlayRewardedAdListener() {
                        public void onAdLoaded(LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.LOADED, 0, null, null, 0);
                        }

                        public void onAdLoadFailed(LevelPlayAdError error) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.FAILED,
                                    error.getErrorCode(), error.getErrorMessage(), null, 0);
                        }

                        public void onAdDisplayed(LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.SHOWN, 0, null, null, 0);
                            LevelPlayCallback.fire(handle, LevelPlayCallback.IMPRESSION, 0, null, null, 0);
                        }

                        public void onAdDisplayFailed(LevelPlayAdError error, LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.SHOW_FAILED,
                                    error.getErrorCode(), error.getErrorMessage(), null, 0);
                        }

                        public void onAdRewarded(LevelPlayReward reward, LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.REWARD, 0, null,
                                    reward.getName(), reward.getAmount());
                        }

                        public void onAdClicked(LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.CLICKED, 0, null, null, 0);
                        }

                        public void onAdClosed(LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.DISMISSED, 0, null, null, 0);
                        }
                    });
                    holder.rewarded = ad;
                } else {
                    LevelPlayInterstitialAd ad = new LevelPlayInterstitialAd(adUnitId);
                    ad.setListener(new LevelPlayInterstitialAdListener() {
                        public void onAdLoaded(LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.LOADED, 0, null, null, 0);
                        }

                        public void onAdLoadFailed(LevelPlayAdError error) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.FAILED,
                                    error.getErrorCode(), error.getErrorMessage(), null, 0);
                        }

                        public void onAdDisplayed(LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.SHOWN, 0, null, null, 0);
                            LevelPlayCallback.fire(handle, LevelPlayCallback.IMPRESSION, 0, null, null, 0);
                        }

                        public void onAdDisplayFailed(LevelPlayAdError error, LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.SHOW_FAILED,
                                    error.getErrorCode(), error.getErrorMessage(), null, 0);
                        }

                        public void onAdClicked(LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.CLICKED, 0, null, null, 0);
                        }

                        public void onAdClosed(LevelPlayAdInfo adInfo) {
                            LevelPlayCallback.fire(handle, LevelPlayCallback.DISMISSED, 0, null, null, 0);
                        }
                    });
                    holder.interstitial = ad;
                }
            }
        });
        ads.put(handle, holder);
        return true;
    }

    public void setServerSideVerification(int handle, String userId, String customData) {
        if (userId != null) {
            LevelPlay.setDynamicUserId(userId);
        }
    }

    public void loadFullScreen(final int handle, String keywords, String contentUrl,
                               boolean nonPersonalized) {
        final FullScreenHolder holder = ads.get(handle);
        final Activity activity = AndroidNativeUtil.getActivity();
        if (holder == null || activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (holder.rewarded != null) {
                    holder.rewarded.loadAd();
                } else if (holder.interstitial != null) {
                    holder.interstitial.loadAd();
                }
            }
        });
    }

    public boolean isFullScreenLoaded(int handle) {
        FullScreenHolder holder = ads.get(handle);
        if (holder == null) {
            return false;
        }
        if (holder.rewarded != null) {
            return holder.rewarded.isAdReady();
        }
        return holder.interstitial != null && holder.interstitial.isAdReady();
    }

    public void showFullScreen(final int handle) {
        final FullScreenHolder holder = ads.get(handle);
        final Activity activity = AndroidNativeUtil.getActivity();
        if (holder == null || activity == null) {
            LevelPlayCallback.fire(handle, LevelPlayCallback.SHOW_FAILED,
                    LevelPlayErrorCodes.NOT_READY, "No ad loaded", null, 0);
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (holder.rewarded != null) {
                    holder.rewarded.showAd(activity);
                } else if (holder.interstitial != null) {
                    holder.interstitial.showAd(activity);
                }
            }
        });
    }

    public void setAppOpenAutoShow(int handle, boolean enabled) {
    }

    public void disposeFullScreen(int handle) {
        ads.remove(handle);
    }

    /// Returns the raw Android view rather than a peer component. The
    /// generated LevelPlayNativeStub wraps whatever this method returns in
    /// PeerComponent.create(), so returning a peer here makes
    /// AndroidImplementation.createNativePeer reject its own AndroidPeer with
    /// an IllegalArgumentException the first time a banner is shown.
    public View createBanner(final int handle, final String adUnitId, final int sizeType,
                             final int widthDp) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return null;
        }
        final LevelPlayBannerAdView[] out = new LevelPlayBannerAdView[1];
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                LevelPlayBannerAdView.Config config = new LevelPlayBannerAdView.Config.Builder()
                        .setAdSize(mapSize(activity, sizeType, widthDp))
                        .build();
                LevelPlayBannerAdView banner =
                        new LevelPlayBannerAdView(activity, adUnitId, config);
                banner.setBannerListener(new LevelPlayBannerAdViewListener() {
                    public void onAdLoaded(LevelPlayAdInfo adInfo) {
                        LevelPlayCallback.fire(handle, LevelPlayCallback.LOADED, 0, null, null, 0);
                        LevelPlayCallback.fire(handle, LevelPlayCallback.IMPRESSION, 0, null, null, 0);
                    }

                    public void onAdLoadFailed(LevelPlayAdError error) {
                        LevelPlayCallback.fire(handle, LevelPlayCallback.FAILED,
                                error.getErrorCode(), error.getErrorMessage(), null, 0);
                    }

                    public void onAdClicked(LevelPlayAdInfo adInfo) {
                        LevelPlayCallback.fire(handle, LevelPlayCallback.CLICKED, 0, null, null, 0);
                    }
                });
                banners.put(handle, banner);
                out[0] = banner;
            }
        });
        return out[0];
    }

    private static LevelPlayAdSize mapSize(Activity activity, int sizeType, int widthDp) {
        switch (sizeType) {
            case 1: return LevelPlayAdSize.BANNER;
            case 2: return LevelPlayAdSize.LARGE;
            case 3: return LevelPlayAdSize.MEDIUM_RECTANGLE;
            case 4: return LevelPlayAdSize.LEADERBOARD;
            default: {
                LevelPlayAdSize adaptive = widthDp > 0
                        ? LevelPlayAdSize.createAdaptiveAdSize(activity, Integer.valueOf(widthDp))
                        : LevelPlayAdSize.createAdaptiveAdSize(activity);
                // createAdaptiveAdSize is documented to return null when the
                // container is too small to hold any adaptive size.
                return adaptive == null ? LevelPlayAdSize.BANNER : adaptive;
            }
        }
    }

    public void loadBanner(final int handle, String keywords, String contentUrl,
                           boolean nonPersonalized) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                LevelPlayBannerAdView banner = banners.get(handle);
                if (banner != null) {
                    banner.loadAd();
                }
            }
        });
    }

    public void disposeBanner(final int handle) {
        final LevelPlayBannerAdView banner = banners.remove(handle);
        if (banner == null) {
            return;
        }
        Activity activity = AndroidNativeUtil.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    banner.destroy();
                }
            });
        }
    }

    public void requestConsent(boolean underAgeOfConsent) {
        // LevelPlay takes consent from your CMP through
        // LevelPlayPrivacySettings rather than presenting a form of its own, so
        // report "not required" and let the cross-platform flow proceed.
        LevelPlayCallback.fire(0, LevelPlayCallback.CONSENT_COMPLETE, 2, null, null, 0);
    }

    public int getConsentStatus() {
        return 2;
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
