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
package com.codename1.ads.applovin;

import com.codename1.ads.AdCallback;
import com.codename1.ads.spi.AdConsentController;

/// Wraps the AppLovin consent flow and, on iOS, the App Tracking Transparency prompt, exposing them through the Codename One
/// [AdConsentController] SPI. The native side reports completion through
/// [AppLovinCallback] with handle 0.
class AppLovinConsentController implements AdConsentController {
    private final AppLovinNative bridge;
    private AdCallback<Integer> pending;

    AppLovinConsentController(AppLovinNative bridge) {
        this.bridge = bridge;
    }

    @Override
    public void requestConsent(boolean underAgeOfConsent, AdCallback<Integer> onComplete) {
        this.pending = onComplete;
        bridge.requestConsent(underAgeOfConsent);
    }

    @Override
    public int getConsentStatus() {
        return bridge.getConsentStatus();
    }

    @Override
    public boolean canRequestAds() {
        return bridge.canRequestAds();
    }

    @Override
    public void reset() {
        bridge.resetConsent();
    }

    /// Invoked by [AppLovinProvider#dispatch] when the native consent flow finishes.
    void onConsentComplete(int status) {
        AdCallback<Integer> cb = pending;
        pending = null;
        if (cb != null) {
            cb.onResult(Integer.valueOf(status));
        }
    }
}
