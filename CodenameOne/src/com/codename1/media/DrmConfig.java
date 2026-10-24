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

import java.util.HashMap;
import java.util.Map;

/// How a [StreamRequest] obtains the keys of a protected stream: the scheme, the
/// license server URL, and whatever that server needs to authorize the request.
///
/// The license exchange is made by the platform player, or by the port on the
/// player's behalf. The request body is the opaque challenge the scheme defines and
/// the response goes back to the player unread, so the license server is the only
/// party that interprets either:
///
/// ```java
/// String playbackToken = "token-from-your-server";
/// DrmConfig drm = new DrmConfig(DrmScheme.WIDEVINE, "https://example.com/license/widevine")
///         .addLicenseHeader("Authorization", "Bearer " + playbackToken);
/// ```
///
/// [DrmScheme#AES_128] needs no license URL: the key URL is in the playlist, and any
/// token it needs belongs in that URL.
public final class DrmConfig {
    private final DrmScheme scheme;
    private final String licenseUrl;
    private String certificateUrl;
    private String contentId;
    private final Map<String, String> licenseHeaders = new HashMap<String, String>();

    /// Creates a configuration for the given scheme.
    ///
    /// #### Parameters
    ///
    /// - `scheme`: the protection scheme the stream is packaged with
    ///
    /// - `licenseUrl`: where the player posts its license challenge; may be null only
    ///   for [DrmScheme#AES_128], whose key URL is part of the playlist
    public DrmConfig(DrmScheme scheme, String licenseUrl) {
        if (scheme == null) {
            throw new IllegalArgumentException("scheme is required");
        }
        if (licenseUrl == null && scheme != DrmScheme.AES_128) {
            throw new IllegalArgumentException(scheme + " needs a license URL");
        }
        this.scheme = scheme;
        this.licenseUrl = licenseUrl;
    }

    /// The protection scheme.
    ///
    /// #### Returns
    ///
    /// the scheme this configuration was created with
    public DrmScheme getScheme() {
        return scheme;
    }

    /// Where the player posts its license challenge.
    ///
    /// #### Returns
    ///
    /// the license URL, or null for [DrmScheme#AES_128]
    public String getLicenseUrl() {
        return licenseUrl;
    }

    /// Sets the URL of the FairPlay application certificate, the DER file Apple
    /// issues to the content owner. Required for [DrmScheme#FAIRPLAY] and ignored by
    /// every other scheme.
    ///
    /// #### Parameters
    ///
    /// - `certificateUrl`: the certificate URL
    ///
    /// #### Returns
    ///
    /// this configuration, for chaining
    public DrmConfig setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
        return this;
    }

    /// The FairPlay application certificate URL.
    ///
    /// #### Returns
    ///
    /// the certificate URL, or null when none was set
    public String getCertificateUrl() {
        return certificateUrl;
    }

    /// Sets the FairPlay content identifier passed to the key request. When none is
    /// set, the host of the `skd://` key URL in the playlist is used, which is the
    /// common convention.
    ///
    /// #### Parameters
    ///
    /// - `contentId`: the content identifier
    ///
    /// #### Returns
    ///
    /// this configuration, for chaining
    public DrmConfig setContentId(String contentId) {
        this.contentId = contentId;
        return this;
    }

    /// The FairPlay content identifier.
    ///
    /// #### Returns
    ///
    /// the content identifier, or null to derive it from the key URL
    public String getContentId() {
        return contentId;
    }

    /// Adds an HTTP header sent with every license request, typically the
    /// authorization the license server checks the viewer's entitlement with.
    ///
    /// #### Parameters
    ///
    /// - `name`: the header name
    ///
    /// - `value`: the header value
    ///
    /// #### Returns
    ///
    /// this configuration, for chaining
    public DrmConfig addLicenseHeader(String name, String value) {
        if (name == null || value == null || hasLineBreak(name) || hasLineBreak(value)) {
            // Ports pass headers to native code as line separated pairs, so a line
            // break would silently shift every header after it.
            throw new IllegalArgumentException("Header names and values must be non-null and free of line breaks");
        }
        licenseHeaders.put(name, value);
        return this;
    }

    /// The headers sent with every license request.
    ///
    /// #### Returns
    ///
    /// a copy of the headers, keyed by name
    public Map<String, String> getLicenseHeaders() {
        return new HashMap<String, String>(licenseHeaders);
    }
    private static boolean hasLineBreak(String s) {
        return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
    }
}
