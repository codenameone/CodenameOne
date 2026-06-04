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

import com.codename1.ads.spi.AdProvider;
import com.codename1.impl.CodenameOneImplementation;

/// The entry point of the Codename One advertising API. It holds the active
/// [AdProvider] and exposes the lifecycle integrations (interstitial-on-transition
/// and app open ads) that build on the Codename One form and application
/// lifecycle.
///
/// A provider is supplied by an ad library and registered once at startup. Each
/// library exposes a static `install()` method that registers its provider, so
/// wiring AdMob is a single line:
///
/// ```java
/// public void init(Object context) {
///     AdMobProvider.install();
///     AdManager.initialize(new AdConfig(), ready -> AdConsent.requestConsent(null));
/// }
/// ```
///
/// Any object implementing [AdProvider] can be registered with
/// [#registerProvider(AdProvider)] directly, so third party ad networks and
/// mediation layers plug in without changing the framework or the build.
///
/// When no provider is registered the API degrades gracefully: format support
/// reports false and loads fail with [AdError#CODE_UNSUPPORTED] instead of
/// throwing.
public final class AdManager {
    private static AdProvider provider;
    private static AdConfig config;
    private static boolean initialized;

    private AdManager() {
    }

    /// Registers the ad provider to use, replacing any previously registered
    /// provider. Normally called by a provider's static `install()` method.
    ///
    /// #### Parameters
    ///
    /// - `p`: the provider to use
    public static void registerProvider(AdProvider p) {
        provider = p;
    }

    /// The active ad provider, or null when none is registered or the registered
    /// provider is unsupported on this platform.
    public static AdProvider getProvider() {
        if (provider != null && !provider.isSupported()) {
            return null;
        }
        return provider;
    }

    /// The configuration passed to [#initialize(AdConfig, AdCallback)], or null
    /// if it has not been called.
    public static AdConfig getConfig() {
        return config;
    }

    /// True when the active provider supports the given format.
    ///
    /// #### Parameters
    ///
    /// - `format`: the format to query
    public static boolean isSupported(AdFormat format) {
        AdProvider p = getProvider();
        return p != null && p.isFormatSupported(format);
    }

    /// Initializes the active provider's SDK. The callback fires when
    /// initialization completes (immediately with `Boolean.FALSE` when no
    /// provider is registered).
    ///
    /// #### Parameters
    ///
    /// - `cfg`: the global ad configuration, must not be null
    /// - `onComplete`: invoked with the readiness flag, may be null
    public static void initialize(AdConfig cfg, final AdCallback<Boolean> onComplete) {
        config = cfg;
        AdProvider p = getProvider();
        if (p == null) {
            complete(onComplete, Boolean.FALSE);
            return;
        }
        p.initialize(cfg, new AdCallback<Boolean>() {
            @Override
            public void onResult(Boolean value) {
                initialized = value != null && value.booleanValue();
                complete(onComplete, value == null ? Boolean.FALSE : value);
            }
        });
    }

    /// True once [#initialize(AdConfig, AdCallback)] has completed successfully.
    public static boolean isInitialized() {
        return initialized;
    }

    /// Shows the supplied interstitial automatically on screen transitions, no
    /// more often than `minIntervalMillis`.
    ///
    /// The manager loads the first ad, shows it when a transition occurs and the
    /// interval has elapsed, and preloads the next one when the ad is dismissed.
    /// Any [AdListener] you set on the ad is still notified.
    ///
    /// #### Parameters
    ///
    /// - `ad`: the interstitial to show on transitions
    /// - `minIntervalMillis`: minimum time between two shown ads
    public static void bindInterstitialOnTransition(final InterstitialAd ad, final int minIntervalMillis) {
        final AdListener delegate = ad.getAdListener();
        ad.setAdListener(new AdListener() {
            @Override
            public void onLoaded() {
                if (delegate != null) {
                    delegate.onLoaded();
                }
            }

            @Override
            public void onFailedToLoad(AdError error) {
                if (delegate != null) {
                    delegate.onFailedToLoad(error);
                }
            }

            @Override
            public void onShown() {
                if (delegate != null) {
                    delegate.onShown();
                }
            }

            @Override
            public void onShowFailed(AdError error) {
                if (delegate != null) {
                    delegate.onShowFailed(error);
                }
            }

            @Override
            public void onDismissed() {
                if (delegate != null) {
                    delegate.onDismissed();
                }
                // preload the next ad once this one is gone
                ad.load();
            }

            @Override
            public void onImpression() {
                if (delegate != null) {
                    delegate.onImpression();
                }
            }

            @Override
            public void onClicked() {
                if (delegate != null) {
                    delegate.onClicked();
                }
            }
        });
        ad.load();
        CodenameOneImplementation.setOnCurrentFormChange(new TransitionShower(ad, minIntervalMillis));
    }

    /// Loads the supplied app open ad and lets the provider show it
    /// automatically whenever the application returns to the foreground.
    ///
    /// #### Parameters
    ///
    /// - `ad`: the app open ad to manage
    public static void enableAppOpenAds(AppOpenAd ad) {
        ad.load();
        ad.setAutoShowOnForeground(true);
    }

    private static void complete(final AdCallback<Boolean> onComplete, final Boolean value) {
        if (onComplete == null) {
            return;
        }
        AbstractFullScreenAd.runOnEdt(new Runnable() {
            @Override
            public void run() {
                onComplete.onResult(value);
            }
        });
    }

    /// Runnable installed as the current-form-change hook; shows the bound
    /// interstitial when the configured interval has elapsed.
    private static final class TransitionShower implements Runnable {
        private final InterstitialAd ad;
        private final int minIntervalMillis;
        private long lastShown = System.currentTimeMillis();

        TransitionShower(InterstitialAd ad, int minIntervalMillis) {
            this.ad = ad;
            this.minIntervalMillis = minIntervalMillis;
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (now - lastShown >= minIntervalMillis && ad.isLoaded()) {
                lastShown = now;
                ad.show();
            }
        }
    }
}
