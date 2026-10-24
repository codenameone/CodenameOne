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

import com.codename1.backend.Web;
import com.codename1.backend.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// A [LicenseProvider] that forwards the device's challenge to a DRM vendor's
/// license proxy and returns its answer. Most DRM services (and an in-house
/// Widevine or FairPlay key server) take exactly this: the raw challenge in
/// the body, an authorization header identifying the customer, and the
/// content key id, which Reel sends as `X-Reel-Key-Id` for a vendor that
/// looks keys up by id.
///
/// Configured from `reel.drm.widevine.url` / `.header` and
/// `reel.drm.fairplay.url` / `.header` / `.certificate` (see `Services`).
/// The header is one `Name: value` line, typically the vendor's API key.
public final class HttpLicenseProxy implements LicenseProvider {
    private final String scheme;
    private final String url;
    private final String header;
    private final String certificateUrl;
    private final WebClient client = new WebClient().setRequestTimeoutMillis(10000);

    public HttpLicenseProxy(String scheme, String url, String header, String certificateUrl) {
        this.scheme = scheme;
        this.url = url;
        this.header = header == null ? "" : header.trim();
        this.certificateUrl = certificateUrl == null || certificateUrl.length() == 0 ? null : certificateUrl;
    }

    @Override
    public String scheme() {
        return scheme;
    }

    @Override
    public byte[] license(String videoId, String keyId, String keyHex, byte[] challenge) throws IOException {
        List<String> headers = new ArrayList<String>();
        headers.add("Content-Type: application/octet-stream");
        headers.add("X-Reel-Key-Id: " + keyId);
        headers.add("X-Reel-Content-Id: " + videoId);
        if (header.length() > 0) {
            headers.add(header);
        }
        Web.Result answer = client.request("POST", url, headers, challenge);
        if (!answer.isSuccess()) {
            throw new IOException(scheme + " license server answered " + answer.getStatus());
        }
        return answer.getBody();
    }

    @Override
    public String certificateUrl() {
        return certificateUrl;
    }
}
