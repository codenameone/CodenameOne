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

import com.codename1.ads.AdCallback;
import com.codename1.ads.AdConfig;
import com.codename1.ads.AdError;
import com.codename1.ads.AdFormat;
import com.codename1.ads.AdManager;
import com.codename1.ads.AdRequest;
import com.codename1.ads.RewardItem;
import com.codename1.ads.ServerSideVerificationOptions;
import com.codename1.ads.spi.AdConsentController;
import com.codename1.ads.spi.AdProvider;
import com.codename1.ads.spi.AdSessionCallback;
import com.codename1.ads.spi.BannerAdSession;
import com.codename1.ads.spi.FullScreenAdSession;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Component;
import com.codename1.ui.PeerComponent;

import java.util.HashMap;
import java.util.Map;

/// The Google AdMob implementation of [AdProvider], built on the Google Mobile
/// Ads (GMA) SDK. Enable it once at startup:
///
/// ```java
/// AdMobProvider.install();
/// ```
///
/// AdMob's own mediation runs transparently behind this provider, so adding
/// mediation adapters in the AdMob console requires no code changes.
public class AdMobProvider implements AdProvider {
    static final int FORMAT_INTERSTITIAL = 1;
    static final int FORMAT_REWARDED = 2;
    static final int FORMAT_REWARDED_INTERSTITIAL = 3;
    static final int FORMAT_APP_OPEN = 4;

    private static final Map<Integer, AdSessionCallback> CALLBACKS = new HashMap<Integer, AdSessionCallback>();
    private static int nextHandle = 1;
    private static AdMobConsentController consentController;

    private final AdMobNative bridge;

    public AdMobProvider() {
        bridge = NativeLookup.create(AdMobNative.class);
    }

    /// Registers an AdMob provider as the active provider. Call once at startup.
    public static void install() {
        AdManager.registerProvider(new AdMobProvider());
    }

    @Override
    public String getName() {
        return "AdMob";
    }

    @Override
    public boolean isSupported() {
        return bridge != null && bridge.isSupported();
    }

    @Override
    public boolean isFormatSupported(AdFormat format) {
        // Banner plus the four full screen formats; native ads are not yet wired.
        return format != AdFormat.NATIVE;
    }

    @Override
    public void initialize(AdConfig config, AdCallback<Boolean> onComplete) {
        if (bridge == null) {
            if (onComplete != null) {
                onComplete.onResult(Boolean.FALSE);
            }
            return;
        }
        AdConfig cfg = config == null ? new AdConfig() : config;
        bridge.initialize(cfg.getTestDeviceIdString(), cfg.isTestMode(),
                cfg.getTagForChildDirectedTreatment(), cfg.getTagForUnderAgeOfConsent(),
                cfg.getMaxAdContentRating());
        if (onComplete != null) {
            onComplete.onResult(Boolean.TRUE);
        }
    }

    @Override
    public FullScreenAdSession createFullScreenAd(AdFormat format, String adUnitId) {
        if (bridge == null) {
            return null;
        }
        int code = formatCode(format);
        if (code < 0) {
            return null;
        }
        int handle = allocateHandle();
        if (!bridge.createFullScreen(handle, code, adUnitId)) {
            return null;
        }
        return new FullScreenSession(bridge, handle);
    }

    @Override
    public BannerAdSession createBanner(String adUnitId, int bannerSize, int widthDp) {
        if (bridge == null) {
            return null;
        }
        int handle = allocateHandle();
        PeerComponent peer = bridge.createBanner(handle, adUnitId, bannerSize, widthDp);
        return new BannerSession(bridge, handle, peer);
    }

    @Override
    public AdConsentController getConsentController() {
        if (bridge == null) {
            return null;
        }
        if (consentController == null) {
            consentController = new AdMobConsentController(bridge);
        }
        return consentController;
    }

    private static int formatCode(AdFormat format) {
        if (format == AdFormat.INTERSTITIAL) {
            return FORMAT_INTERSTITIAL;
        }
        if (format == AdFormat.REWARDED) {
            return FORMAT_REWARDED;
        }
        if (format == AdFormat.REWARDED_INTERSTITIAL) {
            return FORMAT_REWARDED_INTERSTITIAL;
        }
        if (format == AdFormat.APP_OPEN) {
            return FORMAT_APP_OPEN;
        }
        return -1;
    }

    private static synchronized int allocateHandle() {
        return nextHandle++;
    }

    private static synchronized void register(int handle, AdSessionCallback cb) {
        CALLBACKS.put(Integer.valueOf(handle), cb);
    }

    private static synchronized void unregister(int handle) {
        CALLBACKS.remove(Integer.valueOf(handle));
    }

    private static synchronized AdSessionCallback lookup(int handle) {
        return CALLBACKS.get(Integer.valueOf(handle));
    }

    /// Routes a native event to the matching session callback. Called by
    /// [AdMobCallback]; may run on any thread (the core layer marshals to the EDT).
    static void dispatch(int handle, int event, int code, String message,
                         String rewardType, int rewardAmount) {
        if (event == AdMobCallback.CONSENT_COMPLETE) {
            if (consentController != null) {
                consentController.onConsentComplete(code);
            }
            return;
        }
        AdSessionCallback cb = lookup(handle);
        if (cb == null) {
            return;
        }
        switch (event) {
            case AdMobCallback.LOADED:
                cb.onLoaded();
                break;
            case AdMobCallback.FAILED:
                cb.onFailedToLoad(new AdError(code, "admob", message));
                break;
            case AdMobCallback.SHOWN:
                cb.onShown();
                break;
            case AdMobCallback.SHOW_FAILED:
                cb.onShowFailed(new AdError(code, "admob", message));
                break;
            case AdMobCallback.DISMISSED:
                cb.onDismissed();
                break;
            case AdMobCallback.IMPRESSION:
                cb.onImpression();
                break;
            case AdMobCallback.CLICKED:
                cb.onClicked();
                break;
            case AdMobCallback.REWARD:
                cb.onUserEarnedReward(new RewardItem(rewardType, rewardAmount));
                break;
            default:
                break;
        }
    }

    /// Handle backed implementation of a full screen ad session.
    private static final class FullScreenSession implements FullScreenAdSession {
        private final AdMobNative bridge;
        private final int handle;

        FullScreenSession(AdMobNative bridge, int handle) {
            this.bridge = bridge;
            this.handle = handle;
        }

        @Override
        public void setCallback(AdSessionCallback callback) {
            register(handle, callback);
        }

        @Override
        public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
            if (options != null) {
                bridge.setServerSideVerification(handle, options.getUserId(), options.getCustomData());
            }
        }

        @Override
        public void load(AdRequest request) {
            bridge.loadFullScreen(handle, keywords(request), contentUrl(request), nonPersonalized(request));
        }

        @Override
        public boolean isLoaded() {
            return bridge.isFullScreenLoaded(handle);
        }

        @Override
        public void show() {
            bridge.showFullScreen(handle);
        }

        @Override
        public void setAutoShowOnForeground(boolean enabled) {
            bridge.setAppOpenAutoShow(handle, enabled);
        }

        @Override
        public void dispose() {
            bridge.disposeFullScreen(handle);
            unregister(handle);
        }
    }

    /// Handle backed implementation of a banner ad session.
    private static final class BannerSession implements BannerAdSession {
        private final AdMobNative bridge;
        private final int handle;
        private final PeerComponent peer;

        BannerSession(AdMobNative bridge, int handle, PeerComponent peer) {
            this.bridge = bridge;
            this.handle = handle;
            this.peer = peer;
        }

        @Override
        public Component getView() {
            return peer;
        }

        @Override
        public void setCallback(AdSessionCallback callback) {
            register(handle, callback);
        }

        @Override
        public void load(AdRequest request) {
            bridge.loadBanner(handle, keywords(request), contentUrl(request), nonPersonalized(request));
        }

        @Override
        public void dispose() {
            bridge.disposeBanner(handle);
            unregister(handle);
        }
    }

    private static String keywords(AdRequest request) {
        return request == null ? "" : request.getKeywordString();
    }

    private static String contentUrl(AdRequest request) {
        return request == null ? null : request.getContentUrl();
    }

    private static boolean nonPersonalized(AdRequest request) {
        return request != null && request.isNonPersonalized();
    }
}
