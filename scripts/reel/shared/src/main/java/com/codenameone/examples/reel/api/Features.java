/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.api;

/// The feature switches the admin console turns on and off, and the presets that
/// set them together. The server is the authority: a client hides what a switch
/// turns off, and the server refuses it either way.
public final class Features {
    /// Paid plans; off makes every title free to every account.
    public static final String SUBSCRIPTIONS = "subscriptions";
    /// Per-title rentals.
    public static final String RENTALS = "rentals";
    /// Ads on ad-supported plans and free titles.
    public static final String ADS = "ads";
    /// Anyone may upload; off limits the studio to accounts an admin made creators.
    public static final String OPEN_UPLOADS = "openUploads";
    /// Quizzes, polls and branching in videos.
    public static final String INTERACTIVE = "interactive";
    /// Course certificates.
    public static final String CERTIFICATES = "certificates";
    /// Household profiles with kids profiles and PINs.
    public static final String PROFILES = "profiles";
    /// Offline downloads on plans that allow them.
    public static final String DOWNLOADS = "downloads";
    /// Password sharing detection.
    public static final String SHARING_DETECTION = "sharingDetection";
    /// Hardware DRM (Widevine, FairPlay) where the device has it; off serves AES-128 everywhere.
    public static final String HARDWARE_DRM = "hardwareDrm";
    /// A visible per-account watermark over the video.
    public static final String WATERMARK = "watermark";
    /// Viewer reports of titles.
    public static final String REPORTS = "reports";

    /// A subscription service of curated titles: Netflix.
    public static final String PRESET_STREAMING = "STREAMING";
    /// An open platform of creators' videos with ads: YouTube.
    public static final String PRESET_VIDEO_PLATFORM = "VIDEO_PLATFORM";
    /// Courses with quizzes and certificates: a training academy.
    public static final String PRESET_ACADEMY = "ACADEMY";

    /// Every switch, in the order the admin console lists them.
    public static final String[] ALL = {
        SUBSCRIPTIONS, RENTALS, ADS, OPEN_UPLOADS, INTERACTIVE, CERTIFICATES,
        PROFILES, DOWNLOADS, SHARING_DETECTION, HARDWARE_DRM, WATERMARK, REPORTS
    };

    private Features() {
    }
}
