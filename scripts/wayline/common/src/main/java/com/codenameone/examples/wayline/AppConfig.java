/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.wayline;

import com.codename1.io.Preferences;
import com.codename1.ui.CN;

/// The few things about the app that are settings and not code.
public final class AppConfig {
    /// Where the server is when nothing says otherwise: a server started on
    /// this machine with `cn1:backend`. Change this to your deployment's address
    /// before you ship; until then the settings screen can point a build at
    /// another server, which is how a phone reaches a laptop on the same network.
    public static final String DEFAULT_SERVER = "http://localhost:8080";
    /// The id the server knows this app by. The same constant is in the
    /// server's `SecurityConfig`.
    public static final String CLIENT_ID = "wayline-app";
    /// The address the server hands the authorization code back to. The app
    /// reads the code out of the redirect itself and never opens this address,
    /// so it only has to be one the server has on its list.
    public static final String REDIRECT_URI = "http://127.0.0.1/callback";
    /// The path the server hands the code back on when the app runs in a
    /// browser. The same constant is in the server's `SecurityConfig`.
    public static final String WEB_REDIRECT_PATH = "/signin/code";
    /// Where the map opens when the device will not say where it is.
    public static final double DEFAULT_LAT = 37.8080;
    public static final double DEFAULT_LNG = -122.4120;

    private static final String SERVER_KEY = "wayline.server";
    private static final String MODE_KEY = "wayline.mode";
    private static final String MODE_OWNER_KEY = "wayline.mode.owner";

    private AppConfig() {
    }

    /// The server's address, with no trailing slash. A `wayline.server.url`
    /// system property wins, which is what the tests use; then the address saved
    /// from the settings screen; then, in a browser, the address the page came
    /// from, since the server that hosts the app is the server it talks to;
    /// then [#DEFAULT_SERVER].
    public static String serverUrl() {
        String url = System.getProperty("wayline.server.url");
        if (url == null || url.length() == 0) {
            String origin = pageOrigin();
            url = Preferences.get(SERVER_KEY, origin == null ? DEFAULT_SERVER : origin);
        }
        return clean(url);
    }

    /// The address of the page the app is running in -- `https://host`, with
    /// no path -- or null when it is not running in a browser.
    public static String pageOrigin() {
        String origin = CN.getProperty("browser.window.location.origin", null);
        if (origin == null || !(origin.regionMatches(true, 0, "http://", 0, 7)
                || origin.regionMatches(true, 0, "https://", 0, 8))) {
            return null;
        }
        return clean(origin);
    }

    /// Where the server is asked to send the authorization code.
    ///
    /// The installed app names the loopback address and reads the code off the
    /// redirect without following it. A browser follows a redirect itself and
    /// will not show the page an answer from another origin, so there the code
    /// is sent to the server the page came from, which hands it back.
    public static String redirectUri() {
        String origin = pageOrigin();
        return origin == null ? REDIRECT_URI : origin + WEB_REDIRECT_PATH;
    }

    public static void setServerUrl(String url) {
        Preferences.set(SERVER_KEY, clean(url));
    }

    /// The mode `username` chose last -- `rider`, `driver` or `admin` -- or
    /// empty when they have chosen none, or the choice on record is someone
    /// else's. A choice recorded with no name is anyone's: that is how a test
    /// puts the app in a mode before it signs in.
    public static String mode(String username) {
        String owner = Preferences.get(MODE_OWNER_KEY, "");
        if (owner.length() > 0 && !owner.equals(username)) {
            return "";
        }
        return Preferences.get(MODE_KEY, "");
    }

    /// Records `mode` as chosen by whoever signs in next.
    public static void setMode(String mode) {
        setMode(mode, "");
    }

    public static void setMode(String mode, String username) {
        Preferences.set(MODE_KEY, mode == null ? "" : mode);
        Preferences.set(MODE_OWNER_KEY, username == null ? "" : username);
    }

    private static String clean(String url) {
        String text = url == null ? "" : url.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.length() == 0) {
            return DEFAULT_SERVER;
        }
        // Compared without folding the case: a scheme is ASCII, and
        // toLowerCase() follows the device's language.
        if (!text.regionMatches(true, 0, "http://", 0, 7)
                && !text.regionMatches(true, 0, "https://", 0, 8)) {
            text = "http://" + text;
        }
        return text;
    }
}
