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
package com.codename1.backend.push;

/// One device to deliver a push to: the provider that owns the device and the
/// token that provider issued for it.
///
/// The push service names five providers: `fcm`, `apns`, `huawei`, `wns` and
/// `web`. A device reports its provider in older spellings too -- the app's
/// `PushSubscription.getTransportId()` can answer `gcm`, `ios` or `hms` -- so
/// [#of(String, String)] accepts those and stores the service's name. A device
/// key from the classic `Push.getPushKey()`, such as `cn1-fcm-TOKEN`, is read by
/// [#fromDeviceKey(String)].
///
/// [#toString()] names the provider only. A token identifies one person's
/// device, so it stays out of anything that might reach a log.
public final class PushTarget {
    /// Firebase Cloud Messaging, for Android.
    public static final String FCM = "fcm";

    /// Apple Push Notification service, for iOS.
    public static final String APNS = "apns";

    /// Huawei Push Kit, for Huawei devices without Google services.
    public static final String HUAWEI = "huawei";

    /// Windows Push Notification Services.
    public static final String WNS = "wns";

    /// Web Push, for browsers.
    public static final String WEB = "web";

    private static final String DEVICE_KEY_PREFIX = "cn1-";

    private final String provider;
    private final String token;

    private PushTarget(String provider, String token) {
        this.provider = provider;
        this.token = token;
    }

    /// A target for `token` on `provider`.
    ///
    /// @param provider the provider id: `fcm`, `apns`, `huawei`, `wns` or `web`,
    /// or one of the aliases `gcm`, `ios` and `hms`, in any case
    /// @param token the provider's token for the device
    /// @return the target, with the provider in the service's spelling
    /// @throws IllegalArgumentException if the provider is unknown or the token is
    /// null or blank
    public static PushTarget of(String provider, String token) {
        String canonical = canonicalProvider(provider);
        if (canonical == null) {
            throw new IllegalArgumentException("Unknown push provider \"" + provider
                    + "\"; expected fcm, apns, huawei, wns or web");
        }
        if (token == null || token.trim().length() == 0) {
            throw new IllegalArgumentException("A push target needs a device token");
        }
        return new PushTarget(canonical, token);
    }

    /// A target from a classic device key, `cn1-<provider>-<token>`, as the app's
    /// `Push.getPushKey()` reports it: `cn1-fcm-...`, `cn1-gcm-...`,
    /// `cn1-ios-...` or `cn1-hms-...`.
    ///
    /// The prefix is removed and the provider normalized, so the result carries
    /// the bare token the provider issued.
    ///
    /// @param deviceKey the key the device registered
    /// @return the target
    /// @throws IllegalArgumentException if the key doesn't have that form, names
    /// an unknown provider, or has no token
    public static PushTarget fromDeviceKey(String deviceKey) {
        if (deviceKey == null
                || !deviceKey.regionMatches(true, 0, DEVICE_KEY_PREFIX, 0, DEVICE_KEY_PREFIX.length())) {
            throw new IllegalArgumentException("A device key has the form cn1-<provider>-<token>");
        }
        int start = DEVICE_KEY_PREFIX.length();
        int dash = deviceKey.indexOf('-', start);
        if (dash <= start) {
            throw new IllegalArgumentException("A device key has the form cn1-<provider>-<token>");
        }
        return of(deviceKey.substring(start, dash), deviceKey.substring(dash + 1));
    }

    /// The service's name for a provider id or alias, or null when it names none.
    ///
    /// Compared with equalsIgnoreCase, never by folding with toLowerCase(): that
    /// is locale sensitive, and on a server whose default locale is Turkish the
    /// `I` of `IOS` folds to a dotless i and matches nothing.
    static String canonicalProvider(String id) {
        if (id == null) {
            return null;
        }
        if (FCM.equalsIgnoreCase(id) || "gcm".equalsIgnoreCase(id)) {
            return FCM;
        }
        if (APNS.equalsIgnoreCase(id) || "ios".equalsIgnoreCase(id)) {
            return APNS;
        }
        if (HUAWEI.equalsIgnoreCase(id) || "hms".equalsIgnoreCase(id)) {
            return HUAWEI;
        }
        if (WNS.equalsIgnoreCase(id)) {
            return WNS;
        }
        if (WEB.equalsIgnoreCase(id)) {
            return WEB;
        }
        return null;
    }

    /// @return the provider, always one of `fcm`, `apns`, `huawei`, `wns` and `web`
    public String getProvider() {
        return provider;
    }

    /// @return the provider's token for the device
    public String getToken() {
        return token;
    }

    /// The provider only; the token is left out so a logged target identifies no
    /// device.
    @Override
    public String toString() {
        return "PushTarget[" + provider + "]";
    }
}
