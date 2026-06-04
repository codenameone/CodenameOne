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

import com.codename1.ads.spi.AdConsentController;
import com.codename1.ads.spi.AdProvider;

/// Manages user privacy consent for advertising. On modern platforms collecting
/// consent is mandatory before personalized ads can be served: in the EEA/UK the
/// GDPR consent form must be shown (the provider wraps Google's User Messaging
/// Platform or an equivalent), and on iOS the App Tracking Transparency prompt
/// must be presented to access the advertising identifier.
///
/// The recommended flow is: initialize the [AdManager], request consent, then
/// load ads only once consent has been resolved:
///
/// ```java
/// AdManager.initialize(new AdConfig(), ok ->
///     AdConsent.requestConsent(status -> {
///         if (AdConsent.canRequestAds()) {
///             interstitial.load();
///         }
///     }));
/// ```
///
/// When no provider is registered consent calls resolve immediately to
/// [#STATUS_NOT_REQUIRED].
public final class AdConsent {
    /// Consent status is unknown (consent has not been requested yet).
    public static final int STATUS_UNKNOWN = 0;
    /// Consent is required and has not yet been obtained.
    public static final int STATUS_REQUIRED = 1;
    /// Consent is not required for this user (e.g. outside the EEA).
    public static final int STATUS_NOT_REQUIRED = 2;
    /// Consent was obtained.
    public static final int STATUS_OBTAINED = 3;

    private AdConsent() {
    }

    /// Gathers consent if required, presenting the consent form and (on iOS) the
    /// App Tracking Transparency prompt as needed. The callback receives one of
    /// the `STATUS_*` constants on the EDT when the flow completes.
    ///
    /// #### Parameters
    ///
    /// - `onComplete`: invoked with the resulting consent status, may be null
    public static void requestConsent(final AdCallback<Integer> onComplete) {
        AdConsentController controller = controller();
        if (controller == null) {
            complete(onComplete, STATUS_NOT_REQUIRED);
            return;
        }
        boolean underAge = AdManager.getConfig() != null
                && AdManager.getConfig().getTagForUnderAgeOfConsent() == AdConfig.TAG_TRUE;
        controller.requestConsent(underAge, new AdCallback<Integer>() {
            @Override
            public void onResult(final Integer value) {
                complete(onComplete, value == null ? STATUS_UNKNOWN : value.intValue());
            }
        });
    }

    /// True when there is enough consent to request ads. When no provider is
    /// installed this returns true so the simulator placeholder flow works.
    public static boolean canRequestAds() {
        AdConsentController controller = controller();
        return controller == null || controller.canRequestAds();
    }

    /// The current consent status without triggering a new request.
    public static int getConsentStatus() {
        AdConsentController controller = controller();
        return controller == null ? STATUS_NOT_REQUIRED : controller.getConsentStatus();
    }

    /// Clears stored consent. Intended for testing the consent flow only.
    public static void reset() {
        AdConsentController controller = controller();
        if (controller != null) {
            controller.reset();
        }
    }

    private static AdConsentController controller() {
        AdProvider provider = AdManager.getProvider();
        return provider == null ? null : provider.getConsentController();
    }

    private static void complete(final AdCallback<Integer> onComplete, final int status) {
        AbstractFullScreenAd.runOnEdt(new Runnable() {
            @Override
            public void run() {
                if (onComplete != null) {
                    onComplete.onResult(Integer.valueOf(status));
                }
            }
        });
    }
}
