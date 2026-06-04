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

import com.codename1.ads.AdCallback;

/// A provider's consent and privacy controller, backing the public
/// [com.codename1.ads.AdConsent] facade. Implementations wrap the GDPR consent
/// flow (e.g. Google's User Messaging Platform) and, on iOS, the App Tracking
/// Transparency prompt.
///
/// This is an internal SPI type.
public interface AdConsentController {
    /// Gathers consent if required, presenting the consent form when necessary,
    /// and (on iOS) the App Tracking Transparency prompt. The callback receives
    /// the resulting status (one of the `STATUS_*` constants in
    /// [com.codename1.ads.AdConsent]) on completion.
    ///
    /// #### Parameters
    ///
    /// - `underAgeOfConsent`: whether the user is tagged as under the age of consent
    /// - `onComplete`: invoked with the resulting consent status
    void requestConsent(boolean underAgeOfConsent, AdCallback<Integer> onComplete);

    /// The current consent status without triggering a new request.
    int getConsentStatus();

    /// True when the provider has enough consent to request ads.
    boolean canRequestAds();

    /// Clears stored consent. Intended for testing only.
    void reset();
}
