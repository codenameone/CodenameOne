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
import com.codename1.ads.spi.FullScreenAdSession;
import com.codename1.ui.Display;

/// Shared plumbing for the full screen ad formats ([InterstitialAd],
/// [RewardedAd], [RewardedInterstitialAd], [AppOpenAd]). Handles lazy session
/// creation against the registered [AdProvider] and marshals every provider
/// callback onto the EDT before forwarding it to the public [AdListener].
///
/// @author Shai Almog
abstract class AbstractFullScreenAd {
    private final AdFormat format;
    /// The ad unit identifier from the network console.
    final String adUnitId;
    private FullScreenAdSession session;
    private AdListener adListener;
    OnUserEarnedRewardListener rewardListener;
    private ServerSideVerificationOptions ssv;

    AbstractFullScreenAd(AdFormat format, String adUnitId) {
        this.format = format;
        this.adUnitId = adUnitId;
    }

    /// Sets the listener notified of this ad's lifecycle events.
    public void setAdListener(AdListener listener) {
        this.adListener = listener;
    }

    /// The listener notified of this ad's lifecycle events, may be null.
    public AdListener getAdListener() {
        return adListener;
    }

    void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
        this.ssv = options;
        if (session != null) {
            session.setServerSideVerificationOptions(options);
        }
    }

    /// Loads an ad with default targeting.
    public void load() {
        load(null);
    }

    /// Loads an ad using the supplied targeting metadata.
    ///
    /// #### Parameters
    ///
    /// - `request`: optional targeting metadata, may be null
    public void load(final AdRequest request) {
        AdProvider provider = AdManager.getProvider();
        if (provider == null || !provider.isFormatSupported(format)) {
            fireFailedToLoad(new AdError(AdError.CODE_UNSUPPORTED, null,
                    "No ad provider supports " + format + " on this platform"));
            return;
        }
        if (session == null) {
            session = provider.createFullScreenAd(format, adUnitId);
            if (session == null) {
                fireFailedToLoad(new AdError(AdError.CODE_UNSUPPORTED, null,
                        "Provider " + provider.getName() + " returned no session for " + format));
                return;
            }
            session.setCallback(new Dispatcher());
            if (ssv != null) {
                session.setServerSideVerificationOptions(ssv);
            }
        }
        session.load(request);
    }

    /// True when an ad is loaded and ready to [#show()].
    public boolean isLoaded() {
        return session != null && session.isLoaded();
    }

    /// Presents the loaded ad. Does nothing if no ad is loaded.
    public void show() {
        if (session != null) {
            session.show();
        }
    }

    /// Releases the resources held by this ad.
    public void dispose() {
        if (session != null) {
            session.dispose();
            session = null;
        }
    }

    FullScreenAdSession getSession() {
        return session;
    }

    private void fireFailedToLoad(final AdError error) {
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                if (adListener != null) {
                    adListener.onFailedToLoad(error);
                }
            }
        });
    }

    static void runOnEdt(Runnable r) {
        Display d = Display.getInstance();
        if (d.isEdt()) {
            r.run();
        } else {
            d.callSerially(r);
        }
    }

    /// Bridges provider callbacks (which may arrive on any thread) onto the EDT
    /// and forwards them to the public listeners.
    private class Dispatcher implements AdSessionCallback {
        @Override
        public void onLoaded() {
            runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (adListener != null) {
                        adListener.onLoaded();
                    }
                }
            });
        }

        @Override
        public void onFailedToLoad(final AdError error) {
            fireFailedToLoad(error);
        }

        @Override
        public void onShown() {
            runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (adListener != null) {
                        adListener.onShown();
                    }
                }
            });
        }

        @Override
        public void onShowFailed(final AdError error) {
            runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (adListener != null) {
                        adListener.onShowFailed(error);
                    }
                }
            });
        }

        @Override
        public void onDismissed() {
            runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (adListener != null) {
                        adListener.onDismissed();
                    }
                }
            });
        }

        @Override
        public void onImpression() {
            runOnEdt(new Runnable() {
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
            runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (adListener != null) {
                        adListener.onClicked();
                    }
                }
            });
        }

        @Override
        public void onUserEarnedReward(final RewardItem reward) {
            runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (rewardListener != null) {
                        rewardListener.onUserEarnedReward(reward);
                    }
                }
            });
        }
    }
}
