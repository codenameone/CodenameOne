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

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.reel.domain.VideoKey;

/// The license endpoints a player calls for the content key.
///
/// The player sends the license token it was given with the session in the
/// `X-Reel-License` header ([com.codename1.media.DrmConfig#addLicenseHeader]),
/// and the token names the one video it unlocks. ClearKey is answered here,
/// from `video_key`; Widevine and FairPlay challenges are forwarded to the
/// configured vendor service with the key, which wraps it for the device's
/// secure hardware. A session that has ended gets no more licenses.
@RestController
public class LicenseController {
    public static final String HEADER = "X-Reel-License";
    private static final int MAX_CHALLENGE = 64 * 1024;

    private final MediaTokens tokens;
    private final Licenses licenses;
    private final Playbacks playbacks;

    public LicenseController(MediaTokens tokens, Licenses licenses, Playbacks playbacks) {
        this.tokens = tokens;
        this.licenses = licenses;
        this.playbacks = playbacks;
    }

    @PostMapping("/license/clearkey")
    public HttpServer.Response clearKey(HttpServer.Request request) throws Exception {
        MediaTokens.Grant grant = grant(request);
        if (grant == null) {
            return HttpServer.Response.text(403, "forbidden");
        }
        byte[] body = request.getBodyBytes();
        if (body == null || body.length > MAX_CHALLENGE) {
            return HttpServer.Response.text(400, "bad request");
        }
        return HttpServer.Response.json(200, licenses.clearKey(grant.videoId, new String(body, "UTF-8")));
    }

    @PostMapping("/license/widevine")
    public HttpServer.Response widevine(HttpServer.Request request) throws Exception {
        return vendor(request, Licenses.WIDEVINE);
    }

    @PostMapping("/license/fairplay")
    public HttpServer.Response fairplay(HttpServer.Request request) throws Exception {
        return vendor(request, Licenses.FAIRPLAY);
    }

    private HttpServer.Response vendor(HttpServer.Request request, String scheme) throws Exception {
        LicenseProvider provider = licenses.provider(scheme);
        if (provider == null) {
            return HttpServer.Response.text(404, scheme + " is not configured on this server");
        }
        MediaTokens.Grant grant = grant(request);
        if (grant == null) {
            return HttpServer.Response.text(403, "forbidden");
        }
        byte[] body = request.getBodyBytes();
        if (body == null || body.length == 0 || body.length > MAX_CHALLENGE) {
            return HttpServer.Response.text(400, "bad request");
        }
        VideoKey key = licenses.key(grant.videoId);
        if (key == null) {
            return HttpServer.Response.text(404, "not found");
        }
        byte[] license = provider.license(grant.videoId, key.getKeyId(), key.getKeyHex(), body);
        return new HttpServer.Response(200, "application/octet-stream", license);
    }

    private MediaTokens.Grant grant(HttpServer.Request request) {
        String token = request.getHeader(HEADER);
        MediaTokens.Grant grant = token == null ? null : tokens.verifyLicense(token.trim());
        if (grant == null || !playbacks.isLive(grant)) {
            return null;
        }
        return grant;
    }
}
