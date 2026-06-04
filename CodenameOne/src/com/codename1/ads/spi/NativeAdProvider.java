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
package com.codename1.ads.spi;

import com.codename1.ads.AdError;
import com.codename1.ads.AdRequest;
import com.codename1.ads.NativeAd;
import com.codename1.util.SuccessCallback;

/// Optional capability interface implemented by an [AdProvider] that supports
/// native ads. The base [AdProvider] does not require native ad support, so a
/// provider opts in by additionally implementing this interface;
/// [com.codename1.ads.NativeAdLoader] checks for it at runtime.
///
/// @author Shai Almog
public interface NativeAdProvider {
    /// Loads a native ad. Exactly one of the callbacks is invoked.
    ///
    /// #### Parameters
    ///
    /// - `adUnitId`: the ad unit identifier from the network console
    /// - `request`: optional targeting metadata, may be null
    /// - `onSuccess`: invoked with the loaded native ad
    /// - `onError`: invoked when loading fails
    void loadNativeAd(String adUnitId, AdRequest request,
                      SuccessCallback<NativeAd> onSuccess, SuccessCallback<AdError> onError);
}
