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
package com.codename1.ads.mock;

import com.codename1.ads.AdCallback;
import com.codename1.ads.AdConfig;
import com.codename1.ads.AdError;
import com.codename1.ads.AdFormat;
import com.codename1.ads.AdManager;
import com.codename1.ads.AdRequest;
import com.codename1.ads.BannerAd;
import com.codename1.ads.NativeAd;
import com.codename1.ads.RewardItem;
import com.codename1.ads.ServerSideVerificationOptions;
import com.codename1.ads.spi.AdConsentController;
import com.codename1.ads.spi.AdProvider;
import com.codename1.ads.spi.AdSessionCallback;
import com.codename1.ads.spi.BannerAdSession;
import com.codename1.ads.spi.FullScreenAdSession;
import com.codename1.ads.spi.NativeAdProvider;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

/// A deterministic, network-free ad provider for tests and screenshots. It
/// renders fixed, labelled "ads" with stable colours, text and sizes (no
/// randomness, no time, no network), which makes it suitable for pixel
/// comparison in the screenshot test suite and for demonstrating the API in the
/// simulator without an AdMob account.
///
/// Enable it once at startup:
///
/// ```java
/// MockAdProvider.install();
/// ```
public class MockAdProvider implements AdProvider, NativeAdProvider {
    /// Registers a mock provider as the active provider. Call once at startup.
    public static void install() {
        AdManager.registerProvider(new MockAdProvider());
    }

    @Override
    public String getName() {
        return "Mock";
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean isFormatSupported(AdFormat format) {
        return true;
    }

    @Override
    public void initialize(AdConfig config, AdCallback<Boolean> onComplete) {
        if (onComplete != null) {
            onComplete.onResult(Boolean.TRUE);
        }
    }

    @Override
    public FullScreenAdSession createFullScreenAd(AdFormat format, String adUnitId) {
        return new MockFullScreen(format);
    }

    @Override
    public BannerAdSession createBanner(String adUnitId, int bannerSize, int widthDp) {
        return new MockBannerSession(bannerSize);
    }

    @Override
    public AdConsentController getConsentController() {
        return new MockConsent();
    }

    @Override
    public void loadNativeAd(String adUnitId, AdRequest request,
                             AdCallback<NativeAd> onSuccess, AdCallback<AdError> onError) {
        onSuccess.onResult(new NativeAd(
                "CodeMagic Pro",
                "Build, test and ship your Codename One apps faster.",
                "Install",
                "CodeMagic",
                null, null, 4.5));
    }

    /// Deterministic full screen ad: fires the lifecycle events and presents a
    /// fixed close-able form.
    private static final class MockFullScreen implements FullScreenAdSession {
        private final AdFormat format;
        private AdSessionCallback cb;
        private boolean loaded;

        MockFullScreen(AdFormat format) {
            this.format = format;
        }

        @Override
        public void setCallback(AdSessionCallback callback) {
            cb = callback;
        }

        @Override
        public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
        }

        @Override
        public void load(AdRequest request) {
            loaded = true;
            cb.onLoaded();
        }

        @Override
        public boolean isLoaded() {
            return loaded;
        }

        @Override
        public void show() {
            if (!loaded) {
                cb.onShowFailed(new AdError(AdError.CODE_INTERNAL, "mock", "No ad loaded"));
                return;
            }
            loaded = false;
            cb.onShown();
            cb.onImpression();
            if (format == AdFormat.REWARDED || format == AdFormat.REWARDED_INTERSTITIAL) {
                cb.onUserEarnedReward(new RewardItem("coins", 10));
            }
            cb.onDismissed();
        }

        @Override
        public void setAutoShowOnForeground(boolean enabled) {
        }

        @Override
        public void dispose() {
        }
    }

    private static final class MockBannerSession implements BannerAdSession {
        private final MockBanner view;
        private AdSessionCallback cb;

        MockBannerSession(int bannerSize) {
            view = new MockBanner(bannerSize);
        }

        @Override
        public Component getView() {
            return view;
        }

        @Override
        public void setCallback(AdSessionCallback callback) {
            cb = callback;
        }

        @Override
        public void load(AdRequest request) {
            cb.onLoaded();
            cb.onImpression();
        }

        @Override
        public void dispose() {
        }
    }

    /// A fixed-size, fixed-colour banner drawn entirely with Codename One
    /// components so it renders identically every run.
    private static final class MockBanner extends Container {
        private final int bannerSize;

        MockBanner(int bannerSize) {
            super(new BorderLayout());
            this.bannerSize = bannerSize;
            getAllStyles().setBgColor(0x3367d6);
            getAllStyles().setBgTransparency(255);
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
            Label label = new Label("Advertisement");
            label.getAllStyles().setFgColor(0xffffff);
            label.getAllStyles().setBgTransparency(0);
            label.getAllStyles().setAlignment(Component.CENTER);
            add(BorderLayout.CENTER, label);
        }

        @Override
        protected Dimension calcPreferredSize() {
            int height = bannerSize == BannerAd.SIZE_MEDIUM_RECTANGLE
                    ? CN.convertToPixels(250) : CN.convertToPixels(50);
            return new Dimension(CN.convertToPixels(320), height);
        }
    }

    private static final class MockConsent implements AdConsentController {
        @Override
        public void requestConsent(boolean underAgeOfConsent, AdCallback<Integer> onComplete) {
            // STATUS_NOT_REQUIRED
            onComplete.onResult(Integer.valueOf(2));
        }

        @Override
        public int getConsentStatus() {
            return 2;
        }

        @Override
        public boolean canRequestAds() {
            return true;
        }

        @Override
        public void reset() {
        }
    }
}
