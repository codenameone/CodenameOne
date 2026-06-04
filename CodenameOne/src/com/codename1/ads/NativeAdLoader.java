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
import com.codename1.ads.spi.NativeAdProvider;
import com.codename1.util.SuccessCallback;

/// Loads [NativeAd] assets that you render with your own components. Native ads
/// are an optional provider capability; if the active provider does not support
/// them the error callback receives an [AdError] with code
/// [AdError#CODE_UNSUPPORTED].
///
/// ```java
/// new NativeAdLoader("ca-app-pub-xxx/yyy").load(null,
///     new SuccessCallback<NativeAd>() {
///         public void onSucess(NativeAd ad) { renderMyAd(ad); }
///     },
///     new SuccessCallback<AdError>() {
///         public void onSucess(AdError e) { Log.p(e.toString()); }
///     });
/// ```
///
/// @author Shai Almog
public class NativeAdLoader {
    private final String adUnitId;

    /// Creates a loader for the given ad unit id.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    public NativeAdLoader(String adUnitId) {
        this.adUnitId = adUnitId;
    }

    /// True when the active provider supports native ads.
    public static boolean isSupported() {
        AdProvider p = AdManager.getProvider();
        return p != null && p.isFormatSupported(AdFormat.NATIVE) && (p instanceof NativeAdProvider);
    }

    /// Loads a native ad. Exactly one callback is invoked, on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `request`: optional targeting metadata, may be null
    /// - `onSuccess`: invoked with the loaded ad
    /// - `onError`: invoked on failure
    public void load(AdRequest request, final SuccessCallback<NativeAd> onSuccess,
                     final SuccessCallback<AdError> onError) {
        AdProvider provider = AdManager.getProvider();
        if (!(provider instanceof NativeAdProvider) || !provider.isFormatSupported(AdFormat.NATIVE)) {
            AbstractFullScreenAd.runOnEdt(new Runnable() {
                @Override
                public void run() {
                    if (onError != null) {
                        onError.onSucess(new AdError(AdError.CODE_UNSUPPORTED, null,
                                "Native ads are not supported by the active provider"));
                    }
                }
            });
            return;
        }
        ((NativeAdProvider) provider).loadNativeAd(adUnitId, request,
                new SuccessCallback<NativeAd>() {
                    @Override
                    public void onSucess(final NativeAd value) {
                        AbstractFullScreenAd.runOnEdt(new Runnable() {
                            @Override
                            public void run() {
                                if (onSuccess != null) {
                                    onSuccess.onSucess(value);
                                }
                            }
                        });
                    }
                },
                new SuccessCallback<AdError>() {
                    @Override
                    public void onSucess(final AdError value) {
                        AbstractFullScreenAd.runOnEdt(new Runnable() {
                            @Override
                            public void run() {
                                if (onError != null) {
                                    onError.onSucess(value);
                                }
                            }
                        });
                    }
                });
    }
}
