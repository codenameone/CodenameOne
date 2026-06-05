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

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.ui.PeerComponent;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;

import java.util.HashMap;
import java.util.Map;

/// Android implementation of the Unity LevelPlay (ironSource) native bridge.
/// Shipped as source and compiled by the Codename One Android build; validated
/// on device.
///
/// LevelPlay's interstitial and rewarded placements are singletons (not per ad
/// unit), so the bridge routes the active full screen handle to the singleton
/// callbacks; banners are per instance.
public class LevelPlayNativeImpl {
    private static final int FORMAT_INTERSTITIAL = 1;
    private static final int FORMAT_REWARDED = 2;
    private static final int FORMAT_APP_OPEN = 4;

    private final Map<Integer, Integer> formats = new HashMap<Integer, Integer>();
    private final Map<Integer, IronSourceBannerLayout> banners = new HashMap<Integer, IronSourceBannerLayout>();
    private int activeInterstitial = -1;
    private int activeRewarded = -1;
    private boolean listenersBound;

    public void initialize(final String testDeviceIds, final boolean testMode,
                           final int tagForChildDirected, final int tagForUnderAge,
                           final int maxAdContentRating) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                bindListeners();
                IronSource.init(activity, readAppKey(activity));
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

    private void bindListeners() {
        if (listenersBound) {
            return;
        }
        listenersBound = true;
        IronSource.setLevelPlayInterstitialListener(new LevelPlayInterstitialListener() {
            public void onAdReady(AdInfo adInfo) { fire(activeInterstitial, LevelPlayCallback.LOADED, 0, null, null, 0); }
            public void onAdLoadFailed(IronSourceError e) { fire(activeInterstitial, LevelPlayCallback.FAILED, e.getErrorCode(), e.getErrorMessage(), null, 0); }
            public void onAdOpened(AdInfo adInfo) { fire(activeInterstitial, LevelPlayCallback.SHOWN, 0, null, null, 0); fire(activeInterstitial, LevelPlayCallback.IMPRESSION, 0, null, null, 0); }
            public void onAdShowFailed(IronSourceError e, AdInfo adInfo) { fire(activeInterstitial, LevelPlayCallback.SHOW_FAILED, e.getErrorCode(), e.getErrorMessage(), null, 0); }
            public void onAdClicked(AdInfo adInfo) { fire(activeInterstitial, LevelPlayCallback.CLICKED, 0, null, null, 0); }
            public void onAdClosed(AdInfo adInfo) { fire(activeInterstitial, LevelPlayCallback.DISMISSED, 0, null, null, 0); }
        });
        IronSource.setLevelPlayRewardedVideoListener(new LevelPlayRewardedVideoListener() {
            public void onAdAvailable(AdInfo adInfo) { fire(activeRewarded, LevelPlayCallback.LOADED, 0, null, null, 0); }
            public void onAdUnavailable() { fire(activeRewarded, LevelPlayCallback.FAILED, LevelPlayErrorCodes.NOT_READY, "No fill", null, 0); }
            public void onAdOpened(AdInfo adInfo) { fire(activeRewarded, LevelPlayCallback.SHOWN, 0, null, null, 0); fire(activeRewarded, LevelPlayCallback.IMPRESSION, 0, null, null, 0); }
            public void onAdShowFailed(IronSourceError e, AdInfo adInfo) { fire(activeRewarded, LevelPlayCallback.SHOW_FAILED, e.getErrorCode(), e.getErrorMessage(), null, 0); }
            public void onAdClicked(Placement placement, AdInfo adInfo) { fire(activeRewarded, LevelPlayCallback.CLICKED, 0, null, null, 0); }
            public void onAdRewarded(Placement placement, AdInfo adInfo) { fire(activeRewarded, LevelPlayCallback.REWARD, 0, null, placement.getRewardName(), placement.getRewardAmount()); }
            public void onAdClosed(AdInfo adInfo) { fire(activeRewarded, LevelPlayCallback.DISMISSED, 0, null, null, 0); }
        });
    }

    private static void fire(int handle, int event, int code, String message, String rewardType, int rewardAmount) {
        if (handle >= 0) {
            LevelPlayCallback.fire(handle, event, code, message, rewardType, rewardAmount);
        }
    }

    public boolean createFullScreen(int handle, int format, String adUnitId) {
        if (format == FORMAT_APP_OPEN) {
            return false; // LevelPlay has no dedicated app-open format
        }
        formats.put(handle, format);
        return true;
    }

    public void setServerSideVerification(int handle, String userId, String customData) {
        if (userId != null) {
            IronSource.setUserId(userId);
        }
    }

    public void loadFullScreen(final int handle, String keywords, String contentUrl, boolean nonPersonalized) {
        final Integer format = formats.get(handle);
        final Activity activity = AndroidNativeUtil.getActivity();
        if (format == null || activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (format == FORMAT_INTERSTITIAL) {
                    activeInterstitial = handle;
                    IronSource.loadInterstitial();
                } else if (format == FORMAT_REWARDED) {
                    activeRewarded = handle;
                    if (IronSource.isRewardedVideoAvailable()) {
                        LevelPlayCallback.fire(handle, LevelPlayCallback.LOADED, 0, null, null, 0);
                    }
                }
            }
        });
    }

    public boolean isFullScreenLoaded(int handle) {
        Integer format = formats.get(handle);
        if (format == null) {
            return false;
        }
        if (format == FORMAT_INTERSTITIAL) {
            return IronSource.isInterstitialReady();
        }
        if (format == FORMAT_REWARDED) {
            return IronSource.isRewardedVideoAvailable();
        }
        return false;
    }

    public void showFullScreen(final int handle) {
        final Integer format = formats.get(handle);
        final Activity activity = AndroidNativeUtil.getActivity();
        if (format == null || activity == null) {
            LevelPlayCallback.fire(handle, LevelPlayCallback.SHOW_FAILED, LevelPlayErrorCodes.NOT_READY, "No ad loaded", null, 0);
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (format == FORMAT_INTERSTITIAL) {
                    activeInterstitial = handle;
                    IronSource.showInterstitial();
                } else if (format == FORMAT_REWARDED) {
                    activeRewarded = handle;
                    IronSource.showRewardedVideo();
                }
            }
        });
    }

    public void setAppOpenAutoShow(int handle, boolean enabled) {
    }

    public void disposeFullScreen(int handle) {
        formats.remove(handle);
    }

    public PeerComponent createBanner(final int handle, final String adUnitId, final int sizeType, final int widthDp) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return null;
        }
        final IronSourceBannerLayout[] out = new IronSourceBannerLayout[1];
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                IronSourceBannerLayout banner = IronSource.createBanner(activity, ISBannerSize.BANNER);
                banner.setLevelPlayBannerListener(new LevelPlayBannerListener() {
                    public void onAdLoaded(AdInfo adInfo) { LevelPlayCallback.fire(handle, LevelPlayCallback.LOADED, 0, null, null, 0); LevelPlayCallback.fire(handle, LevelPlayCallback.IMPRESSION, 0, null, null, 0); }
                    public void onAdLoadFailed(IronSourceError e) { LevelPlayCallback.fire(handle, LevelPlayCallback.FAILED, e.getErrorCode(), e.getErrorMessage(), null, 0); }
                    public void onAdClicked(AdInfo adInfo) { LevelPlayCallback.fire(handle, LevelPlayCallback.CLICKED, 0, null, null, 0); }
                    public void onAdScreenPresented(AdInfo adInfo) { }
                    public void onAdScreenDismissed(AdInfo adInfo) { }
                    public void onAdLeftApplication(AdInfo adInfo) { }
                });
                banners.put(handle, banner);
                out[0] = banner;
            }
        });
        return out[0] == null ? null : PeerComponent.create(out[0]);
    }

    public void loadBanner(final int handle, String keywords, String contentUrl, boolean nonPersonalized) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                IronSourceBannerLayout banner = banners.get(handle);
                if (banner != null) {
                    IronSource.loadBanner(banner);
                }
            }
        });
    }

    public void disposeBanner(final int handle) {
        final IronSourceBannerLayout banner = banners.remove(handle);
        if (banner == null) {
            return;
        }
        Activity activity = AndroidNativeUtil.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    IronSource.destroyBanner(banner);
                }
            });
        }
    }

    public void requestConsent(boolean underAgeOfConsent) {
        // LevelPlay consent is set via IronSource.setConsent(...) from your CMP;
        // report "not required" so the cross-platform flow can proceed.
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
