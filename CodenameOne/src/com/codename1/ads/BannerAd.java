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
import com.codename1.ads.spi.AdSessionCallback;
import com.codename1.ads.spi.BannerAdSession;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

/// A banner ad that lives inside the Codename One component hierarchy. Add it to
/// a form like any other component (typically anchored at the top or bottom)
/// and call [#load()]:
///
/// ```java
/// BannerAd banner = new BannerAd("ca-app-pub-xxx/yyy");
/// form.add(BorderLayout.SOUTH, banner);
/// banner.load();
/// ```
///
/// On a device the banner wraps the network's native ad view through a peer
/// component; in the simulator it renders a labelled placeholder so layouts can
/// be designed and tested without a device. The default [#SIZE_ADAPTIVE] size
/// asks the network for an anchored adaptive banner sized to the available
/// width, which is the recommended modern banner type.
public class BannerAd extends Container {
    /// Anchored adaptive banner sized to the available width (recommended).
    public static final int SIZE_ADAPTIVE = 0;
    /// Standard banner, 320x50.
    public static final int SIZE_BANNER = 1;
    /// Large banner, 320x100.
    public static final int SIZE_LARGE_BANNER = 2;
    /// Medium rectangle, 300x250.
    public static final int SIZE_MEDIUM_RECTANGLE = 3;
    /// Leaderboard, 728x90 (tablets).
    public static final int SIZE_LEADERBOARD = 4;

    private final String adUnitId;
    private final int size;
    private BannerAdSession session;
    private AdListener adListener;

    /// Creates an adaptive banner for the given ad unit id.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    public BannerAd(String adUnitId) {
        this(adUnitId, SIZE_ADAPTIVE);
    }

    /// Creates a banner of a specific size.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    /// - `size`: one of the `SIZE_*` constants
    public BannerAd(String adUnitId, int size) {
        super(new BorderLayout());
        this.adUnitId = adUnitId;
        this.size = size;
    }

    /// Sets the listener notified of this banner's lifecycle events.
    public void setAdListener(AdListener listener) {
        this.adListener = listener;
    }

    /// Loads a banner with default targeting.
    public void load() {
        load(null);
    }

    /// Loads a banner using the supplied targeting metadata.
    ///
    /// #### Parameters
    ///
    /// - `request`: optional targeting metadata, may be null
    public void load(AdRequest request) {
        AdProvider provider = AdManager.getProvider();
        if (provider == null || !provider.isFormatSupported(AdFormat.BANNER)) {
            fireFailedToLoad(new AdError(AdError.CODE_UNSUPPORTED, null,
                    "No ad provider supports banners on this platform"));
            return;
        }
        if (session == null) {
            session = provider.createBanner(adUnitId, size, 0);
            if (session == null) {
                fireFailedToLoad(new AdError(AdError.CODE_UNSUPPORTED, null,
                        "Provider " + provider.getName() + " returned no banner session"));
                return;
            }
            Component view = session.getView();
            if (view != null) {
                addComponent(BorderLayout.CENTER, view);
                refreshBannerLayout();
            }
            session.setCallback(new Dispatcher());
        }
        session.load(request);
    }

    /// Releases the resources held by this banner. Call from your form's cleanup.
    public void dispose() {
        if (session != null) {
            session.dispose();
            session = null;
        }
    }

    private void refreshBannerLayout() {
        if (getComponentForm() != null) {
            revalidate();
        }
    }

    private void fireFailedToLoad(final AdError error) {
        AbstractFullScreenAd.runOnEdt(new Runnable() {
            @Override
            public void run() {
                if (adListener != null) {
                    adListener.onFailedToLoad(error);
                }
            }
        });
    }

    private class Dispatcher implements AdSessionCallback {
        @Override
        public void onLoaded() {
            AbstractFullScreenAd.runOnEdt(new Runnable() {
                @Override
                public void run() {
                    refreshBannerLayout();
                    if (adListener != null) {
                        adListener.onLoaded();
                    }
                }
            });
        }

        @Override
        public void onFailedToLoad(AdError error) {
            fireFailedToLoad(error);
        }

        @Override
        public void onShown() {
            AbstractFullScreenAd.runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (adListener != null) {
                        adListener.onShown();
                    }
                }
            });
        }

        @Override
        public void onShowFailed(AdError error) {
        }

        @Override
        public void onDismissed() {
        }

        @Override
        public void onImpression() {
            AbstractFullScreenAd.runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (adListener != null) {
                        adListener.onImpression();
                    }
                }
            });
        }

        @Override
        public void onClicked() {
            AbstractFullScreenAd.runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (adListener != null) {
                        adListener.onClicked();
                    }
                }
            });
        }

        @Override
        public void onUserEarnedReward(RewardItem reward) {
        }
    }
}
