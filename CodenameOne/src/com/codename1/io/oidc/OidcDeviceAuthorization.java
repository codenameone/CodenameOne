/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import java.util.Date;
import java.util.Map;

/// What an authorization server answers when a device starts the device authorization grant
/// (RFC 8628): the code the user types, where they type it, and how the device waits.
///
/// Obtained from [OidcClient#requestDeviceAuthorization()]. Show [#getUserCode()] and
/// [#getVerificationUri()] to the user -- or [#getVerificationUriComplete()] as a QR code, which
/// carries the code with it -- and hand this object to
/// [OidcClient#pollDeviceToken(OidcDeviceAuthorization)].
public final class OidcDeviceAuthorization {

    /// The interval RFC 8628 prescribes when the server names none.
    static final int DEFAULT_INTERVAL = 5;

    private final String deviceCode;
    private final String userCode;
    private final String verificationUri;
    private final String verificationUriComplete;
    private final Date expiresAt;
    private final int interval;

    OidcDeviceAuthorization(String deviceCode, String userCode, String verificationUri,
            String verificationUriComplete, Date expiresAt, int interval) {
        this.deviceCode = deviceCode;
        this.userCode = userCode;
        this.verificationUri = verificationUri;
        this.verificationUriComplete = verificationUriComplete;
        this.expiresAt = expiresAt == null ? null : new Date(expiresAt.getTime());
        this.interval = interval;
    }

    /// Builds one from a parsed device authorization response.
    ///
    /// #### Parameters
    ///
    /// - `json`: the response, parsed
    ///
    /// #### Returns
    ///
    /// the authorization
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the response has no `device_code`, no `user_code`
    ///   or no verification address
    public static OidcDeviceAuthorization fromJson(Map<String, Object> json) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        String deviceCode = text(json.get("device_code"));
        String userCode = text(json.get("user_code"));
        if (deviceCode == null || userCode == null) {
            throw new IllegalArgumentException(
                    "A device authorization response needs device_code and user_code");
        }
        String uri = text(json.get("verification_uri"));
        if (uri == null) {
            // Google's endpoint predates the RFC and still answers with this name.
            uri = text(json.get("verification_url"));
        }
        if (uri == null) {
            // The address is as required as the codes (RFC 8628 section 3.2): a code with
            // no page to type it on is a sign-in the user has no way to finish.
            throw new IllegalArgumentException(
                    "A device authorization response needs verification_uri");
        }
        long expiresIn = number(json.get("expires_in"), -1);
        Date expiresAt = expiresIn < 0 ? null
                : new Date(System.currentTimeMillis() + expiresIn * 1000L);
        long interval = number(json.get("interval"), DEFAULT_INTERVAL);
        if (interval < 1) {
            interval = DEFAULT_INTERVAL;
        }
        return new OidcDeviceAuthorization(deviceCode, userCode, uri,
                text(json.get("verification_uri_complete")), expiresAt, (int) interval);
    }

    /// The code the device polls with. Not for the user's eyes.
    public String getDeviceCode() {
        return deviceCode;
    }

    /// The short code the user types on the verification page.
    public String getUserCode() {
        return userCode;
    }

    /// The page the user opens on another device to type the code.
    public String getVerificationUri() {
        return verificationUri;
    }

    /// The same page with the code already in it, for a QR code.
    ///
    /// #### Returns
    ///
    /// the URI, or null when the server did not supply one
    public String getVerificationUriComplete() {
        return verificationUriComplete;
    }

    /// When the codes stop being valid.
    ///
    /// #### Returns
    ///
    /// the moment, or null when the server did not say
    public Date getExpiresAt() {
        return expiresAt == null ? null : new Date(expiresAt.getTime());
    }

    /// Whether the codes have run out.
    public boolean isExpired() {
        return expiresAt != null && expiresAt.getTime() <= System.currentTimeMillis();
    }

    /// The least number of seconds the device waits between two polls.
    public int getInterval() {
        return interval;
    }

    private static String text(Object o) {
        return o instanceof String ? (String) o : null;
    }

    private static long number(Object o, long fallback) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (NumberFormatException notANumber) {
                return fallback;
            }
        }
        return fallback;
    }
}
