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
package com.codename1.media;

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Describes an adaptive (HLS or DASH) or progressive video stream, optionally
/// protected by DRM, and creates the platform player for it.
///
/// ```java
/// StreamRequest req = new StreamRequest(manifestUrl)
///         .setDrm(new DrmConfig(DrmScheme.WIDEVINE, licenseUrl)
///                 .addLicenseHeader("Authorization", "Bearer " + token))
///         .setStartPosition(resumeAtMillis);
/// if (!req.isSupported()) {
///     // ask the server for a scheme this device can play
/// }
/// Media video = req.create();
/// form.add(BorderLayout.CENTER, video.getVideoComponent());
/// ```
///
/// The player is the platform's own (AVPlayer on Apple platforms, the framework
/// media player on Android, ffmpeg in the simulator), so the protocols, codecs and
/// DRM schemes it plays are the platform's. [#isSupported()] answers for the scheme
/// and the headers; a codec the device cannot decode surfaces as a playback error.
///
/// To hide a protected video from screenshots and screen recording while it is on
/// screen, call `Display.getInstance().setProperty("DisableScreenshots", "true")`
/// and set it back to `"false"` when the player closes.
public final class StreamRequest {
    private final String url;
    private DrmConfig drm;
    private int startPosition;
    private Runnable onCompletion;
    private final Map<String, String> headers = new HashMap<String, String>();

    /// Creates a request for the stream at the given URL.
    ///
    /// #### Parameters
    ///
    /// - `url`: the HLS (`.m3u8`) or DASH (`.mpd`) manifest, or a progressive file
    public StreamRequest(String url) {
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("url is required");
        }
        this.url = url;
    }

    /// The stream URL.
    ///
    /// #### Returns
    ///
    /// the manifest or file URL
    public String getUrl() {
        return url;
    }

    /// Sets the content protection of the stream.
    ///
    /// #### Parameters
    ///
    /// - `drm`: the DRM configuration, or null for an unprotected stream
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public StreamRequest setDrm(DrmConfig drm) {
        this.drm = drm;
        return this;
    }

    /// The content protection of the stream.
    ///
    /// #### Returns
    ///
    /// the DRM configuration, or null for an unprotected stream
    public DrmConfig getDrm() {
        return drm;
    }

    /// Sets where playback starts, for resuming a title the viewer left part way.
    ///
    /// #### Parameters
    ///
    /// - `millis`: the start position in milliseconds
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public StreamRequest setStartPosition(int millis) {
        this.startPosition = Math.max(0, millis);
        return this;
    }

    /// Where playback starts.
    ///
    /// #### Returns
    ///
    /// the start position in milliseconds
    public int getStartPosition() {
        return startPosition;
    }

    /// Sets a callback invoked when playback reaches the end of the stream.
    ///
    /// #### Parameters
    ///
    /// - `onCompletion`: the callback, or null
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public StreamRequest setOnCompletion(Runnable onCompletion) {
        this.onCompletion = onCompletion;
        return this;
    }

    /// The completion callback.
    ///
    /// #### Returns
    ///
    /// the callback, or null
    public Runnable getOnCompletion() {
        return onCompletion;
    }

    /// Adds an HTTP header to the manifest and segment requests. Only some platforms
    /// can attach headers to the requests their player makes (see
    /// [MediaManager#isStreamHeadersSupported()]); a URL that carries its own signed
    /// token works everywhere and is the portable choice.
    ///
    /// #### Parameters
    ///
    /// - `name`: the header name
    ///
    /// - `value`: the header value
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public StreamRequest addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /// The headers for the manifest and segment requests.
    ///
    /// #### Returns
    ///
    /// a copy of the headers, keyed by name
    public Map<String, String> getHeaders() {
        return new HashMap<String, String>(headers);
    }

    /// Answers whether this platform can play the request: its DRM scheme, and its
    /// headers if it has any.
    ///
    /// #### Returns
    ///
    /// true when [#create()] can be expected to return a player
    public boolean isSupported() {
        if (drm != null && !MediaManager.isDrmSupported(drm.getScheme())) {
            return false;
        }
        return headers.isEmpty() || MediaManager.isStreamHeadersSupported();
    }

    /// Creates the player. Blocks until the platform has created it, so call it off
    /// the EDT or use [#createAsync()].
    ///
    /// #### Returns
    ///
    /// the media, whose `getVideoComponent()` is the view to place in a form
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the platform cannot play the request, which
    ///   [#isSupported()] predicts, or the stream cannot be opened
    public Media create() throws IOException {
        if (drm != null && !MediaManager.isDrmSupported(drm.getScheme())) {
            throw new IOException(drm.getScheme() + " is not supported on this platform");
        }
        if (!headers.isEmpty() && !MediaManager.isStreamHeadersSupported()) {
            throw new IOException("Stream request headers are not supported on this platform;"
                    + " carry the credential in the URL instead");
        }
        Media m = Display.getInstance().createStreamingMedia(this);
        if (m == null) {
            throw new IOException("The platform could not create a player for " + url);
        }
        return m;
    }

    /// Creates the player without blocking the calling thread.
    ///
    /// #### Returns
    ///
    /// a resource completed with the media, or with the error [#create()] would throw
    public AsyncResource<Media> createAsync() {
        final AsyncResource<Media> out = new AsyncResource<Media>();
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                try {
                    out.complete(create());
                } catch (Throwable t) {
                    out.error(t);
                }
            }
        }, "StreamRequest").start();
        return out;
    }
}
