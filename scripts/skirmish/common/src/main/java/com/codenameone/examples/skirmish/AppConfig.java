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
package com.codenameone.examples.skirmish;

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
    public static final String CLIENT_ID = "skirmish-app";
    /// The address the server hands the authorization code back to. The app
    /// reads the code out of the redirect itself and never opens this address,
    /// so it only has to be one the server has on its list.
    public static final String REDIRECT_URI = "http://127.0.0.1/callback";
    /// The path the server hands the code back on when the app runs in a
    /// browser. The same constant is in the server's `SecurityConfig`.
    public static final String WEB_REDIRECT_PATH = "/signin/code";
    // tag::skirmish-push-key[]
    /// The app's push key, from the Push section of the Codename One console.
    /// Empty, the app registers for no notifications: everything still works,
    /// and a phone in a pocket simply hears nothing until it is opened.
    public static final String PUSH_APP_KEY = "";
    // end::skirmish-push-key[]

    // tag::skirmish-monetization-ids[]
    /// Google's published test ad units. They always fill, never pay, and are
    /// safe to click; replace them with your own AdMob units before release.
    public static final String REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917";
    public static final String INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712";
    public static final String BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111";
    /// The store products, as configured in App Store Connect and the Play
    /// Console. The server's `Catalog` names the same ones.
    public static final String SKU_GEMS_80 = "skirmish.gems_80";
    public static final String SKU_GEMS_500 = "skirmish.gems_500";
    public static final String SKU_ROYAL_PASS = "skirmish.royal_pass";
    // end::skirmish-monetization-ids[]

    private static final String SERVER_KEY = "skirmish.server";
    private static final String MODE_KEY = "skirmish.mode";
    private static final String MODE_OWNER_KEY = "skirmish.mode.owner";

    private AppConfig() {
    }

    /// The server's address, with no trailing slash. A `skirmish.server.url`
    /// system property wins, which is what the tests use; then the address saved
    /// from the settings screen; then, in a browser, the address the page came
    /// from, since the server that hosts the app is the server it talks to;
    /// then [#DEFAULT_SERVER].
    public static String serverUrl() {
        String url = System.getProperty("skirmish.server.url");
        if (url == null || url.length() == 0) {
            String origin = pageOrigin();
            // In a browser it is always the page's own server: see
            // serverSwitchable(). An address saved before that is ignored.
            url = origin != null ? origin : Preferences.get(SERVER_KEY, DEFAULT_SERVER);
        }
        return clean(url);
    }

    /// Whether the app may be pointed at another server. Not in a browser:
    /// there the authorization code is sent back to the page's own server
    /// ([#redirectUri]), and another server has not registered that address
    /// as one of its own, so every sign-in after a switch would be refused.
    /// The hosted app is the server's own, and talks to that server.
    public static boolean serverSwitchable() {
        return pageOrigin() == null;
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

    /// The mode the account `owner` (see
    /// [com.codenameone.examples.skirmish.net.Session#owner(String)]) chose last
    /// -- `player` or `admin` -- or
    /// empty when they have chosen none, or the choice on record is someone
    /// else's. A choice recorded with no name is anyone's: that is how a test
    /// puts the app in a mode before it signs in.
    public static String mode(String owner) {
        String chosenBy = Preferences.get(MODE_OWNER_KEY, "");
        if (chosenBy.length() > 0 && !chosenBy.equals(owner)) {
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

    /// Whether `url` names the server `current` does, as [#setServerUrl]
    /// would store it.
    public static boolean sameServer(String url, String current) {
        return clean(url).equals(current);
    }

    /// Whether the app was opened with `skirmish://tour`, the self-driving
    /// walkthrough the promotional video is recorded from. A `server=`
    /// parameter in the link points the app at that server first.
    /// A `skirmish-tour.txt` in the app's home directory, holding the server
    /// address, asks for the same thing: a simulator can be given that file
    /// with no hands on the screen (`xcrun simctl get_app_container` names the
    /// directory), where a link would stop at the system's "Open in" prompt.
    /// The file is consumed, so the next launch is an ordinary one.
    public static boolean tourRequested() {
        String file = tourFile();
        if (file != null) {
            if (file.length() > 0) {
                setServerUrl(file);
            }
            return true;
        }
        String link = CN.getProperty("AppArg", null);
        if (link == null || !link.startsWith("skirmish://tour")) {
            return false;
        }
        int at = link.indexOf("server=");
        if (at >= 0) {
            int end = link.indexOf('&', at);
            setServerUrl(link.substring(at + 7, end < 0 ? link.length() : end));
        }
        return true;
    }

    private static String tourFile() {
        com.codename1.io.FileSystemStorage fs = com.codename1.io.FileSystemStorage.getInstance();
        String path = fs.getAppHomePath() + "skirmish-tour.txt";
        if (!fs.exists(path)) {
            return null;
        }
        try {
            java.io.InputStream in = fs.openInputStream(path);
            try {
                String text = com.codename1.io.Util.readToString(in).trim();
                return text;
            } finally {
                in.close();
            }
        } catch (java.io.IOException err) {
            return "";
        } finally {
            fs.delete(path);
        }
    }

    /// The challenge code the app was opened with -- a challenge link opened in
    /// a browser, or handed to the installed app -- or null.
    public static String challengeCode() {
        return linkParameter("challenge");
    }

    /// The Forge pack a shared link opened the app with, or null.
    public static String packId() {
        return linkParameter("pack");
    }

    /// The link that opens a Forge pack, for a player to send a friend.
    public static String packLink(String id) {
        String origin = pageOrigin();
        return (origin == null ? serverUrl() : origin) + "/?pack=" + id;
    }

    private static String linkParameter(String name) {
        String key = name + "=";
        String link = CN.getProperty("browser.window.location.href", null);
        if (link == null || link.indexOf(key) < 0) {
            link = CN.getProperty("AppArg", null);
        }
        if (link == null) {
            return null;
        }
        int at = link.indexOf(key);
        if (at < 0) {
            return null;
        }
        int end = at + key.length();
        while (end < link.length() && link.charAt(end) != '&' && link.charAt(end) != '#') {
            end++;
        }
        String code = link.substring(at + key.length(), end);
        return code.length() == 0 ? null : code;
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
