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

/// The assets of a native ad, loaded by [NativeAdLoader]. A native ad lets you
/// render the advertiser's content with your own components so it blends with
/// the surrounding UI. Not every field is populated for every ad; check for
/// null before use.
///
/// Native ad support is an optional provider capability. When the active
/// provider does not support it, [NativeAdLoader] reports the format as
/// unsupported.
///
/// @author Shai Almog
public class NativeAd {
    private final String headline;
    private final String body;
    private final String callToAction;
    private final String advertiser;
    private final String iconUrl;
    private final String imageUrl;
    private final double starRating;

    /// Creates a native ad asset bundle. Intended for providers; applications
    /// receive instances through [NativeAdLoader].
    public NativeAd(String headline, String body, String callToAction, String advertiser,
                    String iconUrl, String imageUrl, double starRating) {
        this.headline = headline;
        this.body = body;
        this.callToAction = callToAction;
        this.advertiser = advertiser;
        this.iconUrl = iconUrl;
        this.imageUrl = imageUrl;
        this.starRating = starRating;
    }

    /// The ad headline, may be null.
    public String getHeadline() {
        return headline;
    }

    /// The ad body text, may be null.
    public String getBody() {
        return body;
    }

    /// The call to action label (e.g. "Install"), may be null.
    public String getCallToAction() {
        return callToAction;
    }

    /// The advertiser name, may be null.
    public String getAdvertiser() {
        return advertiser;
    }

    /// The URL of the ad icon, may be null.
    public String getIconUrl() {
        return iconUrl;
    }

    /// The URL of the main ad image, may be null.
    public String getImageUrl() {
        return imageUrl;
    }

    /// The star rating (0-5), or 0 when not provided.
    public double getStarRating() {
        return starRating;
    }
}
