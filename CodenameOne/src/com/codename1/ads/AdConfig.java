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

import java.util.ArrayList;
import java.util.List;

/// Configuration passed once to [AdManager#initialize(AdConfig, AdCallback)].
/// Controls test mode and the global compliance flags every modern ad network
/// requires (child directed treatment, under-age-of-consent treatment and a
/// maximum ad content rating).
public class AdConfig {
    /// Unspecified child directed treatment (let the network decide).
    public static final int TAG_UNSPECIFIED = 0;
    /// Tag requests as directed to children (COPPA).
    public static final int TAG_TRUE = 1;
    /// Tag requests as not directed to children.
    public static final int TAG_FALSE = 2;

    /// Content rating: general audiences.
    public static final int RATING_G = 1;
    /// Content rating: parental guidance.
    public static final int RATING_PG = 2;
    /// Content rating: teen.
    public static final int RATING_T = 3;
    /// Content rating: mature audiences.
    public static final int RATING_MA = 4;
    /// Content rating: unspecified (network default).
    public static final int RATING_UNSPECIFIED = 0;

    private boolean testMode;
    private final List<String> testDeviceIds = new ArrayList<String>();
    private int tagForChildDirectedTreatment = TAG_UNSPECIFIED;
    private int tagForUnderAgeOfConsent = TAG_UNSPECIFIED;
    private int maxAdContentRating = RATING_UNSPECIFIED;

    /// When true the provider serves test ads on every device (in addition to
    /// any explicit [#getTestDeviceIds()]). Never ship a release build with
    /// test mode on or you will violate the ad network's program policies, and
    /// never click live ads during development.
    public boolean isTestMode() {
        return testMode;
    }

    /// Enables or disables global test mode.
    public AdConfig testMode(boolean testMode) {
        this.testMode = testMode;
        return this;
    }

    /// Registers a device hash that should always receive test ads. The hash is
    /// printed to the device log by the underlying SDK the first time an ad
    /// loads.
    public AdConfig addTestDevice(String deviceId) {
        if (deviceId != null) {
            testDeviceIds.add(deviceId);
        }
        return this;
    }

    /// The registered test device hashes, never null.
    public List<String> getTestDeviceIds() {
        return testDeviceIds;
    }

    /// The test device hashes as a comma separated string for native bridges.
    public String getTestDeviceIdString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < testDeviceIds.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(testDeviceIds.get(i));
        }
        return b.toString();
    }

    /// Child directed treatment flag, one of [#TAG_UNSPECIFIED], [#TAG_TRUE], [#TAG_FALSE].
    public int getTagForChildDirectedTreatment() {
        return tagForChildDirectedTreatment;
    }

    /// Sets the child directed treatment flag (COPPA).
    public AdConfig tagForChildDirectedTreatment(int tag) {
        this.tagForChildDirectedTreatment = tag;
        return this;
    }

    /// Under age of consent flag, one of [#TAG_UNSPECIFIED], [#TAG_TRUE], [#TAG_FALSE].
    public int getTagForUnderAgeOfConsent() {
        return tagForUnderAgeOfConsent;
    }

    /// Sets the under-age-of-consent flag, used by [AdConsent] when gathering GDPR consent.
    public AdConfig tagForUnderAgeOfConsent(int tag) {
        this.tagForUnderAgeOfConsent = tag;
        return this;
    }

    /// The maximum ad content rating, one of the `RATING_*` constants.
    public int getMaxAdContentRating() {
        return maxAdContentRating;
    }

    /// Caps the content rating of served ads.
    public AdConfig maxAdContentRating(int rating) {
        this.maxAdContentRating = rating;
        return this;
    }
}
