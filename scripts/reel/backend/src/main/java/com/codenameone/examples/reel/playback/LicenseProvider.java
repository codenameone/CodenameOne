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
package com.codenameone.examples.reel.playback;

import java.io.IOException;

/// A hardware DRM license server: Widevine or FairPlay. Reel issues ClearKey
/// and AES-128 keys itself; the other two need a licensed vendor (a DRM
/// service, or Apple's FairPlay program), and that vendor is plugged in here.
///
/// An implementation receives the opaque challenge the device's DRM module
/// made, after Reel has checked the viewer's license token and knows the
/// video's key, and returns the vendor's response unread. None ships with the
/// sample: [Licenses] offers a scheme only when a provider for it is a bean, so
/// without one devices are simply served ClearKey or AES-128.
public interface LicenseProvider {
    /// `WIDEVINE` or `FAIRPLAY`.
    String scheme();

    /// The license response for a challenge.
    ///
    /// - `videoId`: the video being played
    /// - `keyId`, `keyHex`: its content key, as Reel packaged it
    /// - `challenge`: the device's request body
    byte[] license(String videoId, String keyId, String keyHex, byte[] challenge) throws IOException;

    /// The FairPlay application certificate URL, or null for Widevine.
    String certificateUrl();
}
