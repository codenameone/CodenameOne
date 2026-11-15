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
package com.codenameone.examples.brickyard;

import com.codename1.io.Preferences;
import com.codename1.ui.CN;

/// Everything the app needs to know about where it runs: the server, the OIDC
/// client, the store SKUs, the ad units and the deep links it answers.
public final class AppConfig {
    public static final String DEFAULT_SERVER = "http://localhost:8080";
    /// Must match `SecurityConfig.CLIENT_ID` on the server.
    public static final String CLIENT_ID = "brickyard-app";
    public static final String REDIRECT_URI = "http://127.0.0.1/callback";
    public static final String WEB_REDIRECT_PATH = "/signin/code";
    public static final String SCHEME = "brickyard://";
    public static final String PUSH_APP_KEY = "";
    // Google's public test units: they always fill and never pay.
    public static final String REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917";
    public static final String SKU_BRICKS_400 = "brickyard.bricks_400";
    public static final String SKU_BRICKS_1000 = "brickyard.bricks_1000";
    public static final String SKU_PLUS = "brickyard.plus_monthly";
    public static final String ENTITLEMENT_PLUS = "plus";

    private static final String SERVER_KEY = "brickyard.server";

    private AppConfig() {
    }

    public static String serverUrl() {
        String url = System.getProperty("brickyard.server.url");
        if (url == null || url.length() == 0) {
            String origin = pageOrigin();
            url = origin != null ? origin : Preferences.get(SERVER_KEY, DEFAULT_SERVER);
        }
        return clean(url);
    }

    public static boolean serverSwitchable() {
        return pageOrigin() == null;
    }

    public static String pageOrigin() {
        String origin = CN.getProperty("browser.window.location.origin", null);
        if (origin == null || !(origin.regionMatches(true, 0, "http://", 0, 7)
                || origin.regionMatches(true, 0, "https://", 0, 8))) {
            return null;
        }
        return clean(origin);
    }

    public static String redirectUri() {
        String origin = pageOrigin();
        return origin == null ? REDIRECT_URI : origin + WEB_REDIRECT_PATH;
    }

    public static void setServerUrl(String url) {
        Preferences.set(SERVER_KEY, clean(url));
    }

    /// The link that opens an experience: `brickyard://experience/<id>`.
    public static String experienceLink(String id) {
        return SCHEME + "experience/" + id;
    }

    /// The experience a link names, or null. Accepts the app scheme and a web
    /// link carrying `?experience=<id>`.
    public static String experienceFromLink(String link) {
        if (link == null) {
            return null;
        }
        String prefix = SCHEME + "experience/";
        String id = null;
        if (link.regionMatches(true, 0, prefix, 0, prefix.length())) {
            id = link.substring(prefix.length());
        } else {
            int at = link.indexOf("experience=");
            if (at >= 0) {
                id = link.substring(at + "experience=".length());
            }
        }
        if (id == null) {
            return null;
        }
        int end = 0;
        while (end < id.length() && id.charAt(end) != '&' && id.charAt(end) != '#'
                && id.charAt(end) != '/' && id.charAt(end) != '?') {
            end++;
        }
        id = id.substring(0, end);
        return id.length() == 0 ? null : id;
    }

    /// The experience the app was opened with, if any.
    public static String launchExperience() {
        String id = experienceFromLink(CN.getProperty("browser.window.location.href", null));
        return id != null ? id : experienceFromLink(CN.getProperty("AppArg", null));
    }

    static String clean(String url) {
        String text = url == null ? "" : url.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.length() == 0) {
            return DEFAULT_SERVER;
        }
        if (!text.regionMatches(true, 0, "http://", 0, 7)
                && !text.regionMatches(true, 0, "https://", 0, 8)) {
            text = "http://" + text;
        }
        return text;
    }
}
