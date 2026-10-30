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
package com.codenameone.examples.bastion;

import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.util.Hashtable;
import java.util.Vector;

/// The few things about the app that are settings and not code -- most of
/// them security settings, and each one read by the Security Center screen so
/// the user sees what is in force.
public final class AppConfig {
    /// Where a release talks to when nothing says otherwise. Replace with the
    /// deployment's API address; its host must be in [#ALLOWED_HOSTS].
    public static final String RELEASE_SERVER = "https://api.bastion.example";
    /// Where the simulator talks to: a server started on this machine with
    /// `cn1:backend`. Only a development run uses it ([#developmentAllowed]).
    public static final String DEVELOPMENT_SERVER = "http://localhost:8080";
    /// The id the server knows this app by (`SecurityConfig.CLIENT_ID`).
    public static final String CLIENT_ID = "bastion-app";
    /// The redirect the server hands the authorization code back on. The app
    /// reads the code off the redirect and never opens it
    /// (`SecurityConfig.LOOPBACK_REDIRECT`).
    public static final String REDIRECT_URI = "http://127.0.0.1/callback";
    /// The path the server hands the code back on when the app runs in a
    /// browser. The same constant is in the server's `SecurityConfig`.
    public static final String WEB_REDIRECT_PATH = "/signin/code";

    /// The hosts the app may talk to at all. Anything else is refused before a
    /// connection is opened (see `net/Guard`), so a link, a redirect or a
    /// mistyped server cannot carry a token or a signed request elsewhere.
    /// Replace with the deployment's API host(s).
    public static final String[] ALLOWED_HOSTS = {"api.bastion.example"};

    /// Certificate pins: host to the base64 SHA-256 digests of the subject
    /// public key infos its chain may present -- the server's own key and a
    /// backup (an issuing CA, or a second key held offline), so a renewal or
    /// a rotation never locks the app out. The placeholder values pin nothing
    /// real; put the deployment's in before release. Pins arriving over the
    /// air from the attestation service (`AppShield.getPinSet()`) are checked
    /// too, on top of these.
    ///
    /// Compute one with:
    /// `openssl s_client -connect host:443 | openssl x509 -pubkey -noout |
    /// openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64`
    public static final String[][] PINS = {
        {"api.bastion.example",
            "REPLACE+WITH+THE+SERVERS+OWN+SPKI+SHA256+DIGEST=",
            "REPLACE+WITH+A+BACKUP+KEY+OR+ISSUING+CA+DIGEST="},
    };

    /// How long the app may sit untouched before it locks.
    public static final long IDLE_LOCK_MILLIS = 2 * 60 * 1000L;
    /// How long the app may be in the background before it asks again.
    public static final long BACKGROUND_LOCK_MILLIS = 30 * 1000L;
    /// How long a copied account number or crypto address stays on the
    /// clipboard.
    public static final long CLIPBOARD_CLEAR_MILLIS = 60 * 1000L;

    private AppConfig() {
    }

    /// The server's address with no trailing slash: a `bastion.server.url`
    /// system property (the tests), then -- in a browser -- the address the
    /// page came from, since the server that hosts the app is the server it
    /// talks to, then [#DEVELOPMENT_SERVER] in a development run, else
    /// [#RELEASE_SERVER].
    public static String serverUrl() {
        String url = System.getProperty("bastion.server.url");
        if (url == null || url.length() == 0) {
            String origin = pageOrigin();
            url = origin != null ? origin
                    : developmentAllowed() ? DEVELOPMENT_SERVER : RELEASE_SERVER;
        }
        return clean(url);
    }

    /// Whether this run may use a development server at all: in the
    /// simulator, under the tests (which name their server), or in a page
    /// served from this machine -- which is itself cleartext from loopback,
    /// so talking back to it adds nothing. An installed app on a phone is
    /// none of these, so a release build never has the exemption, whatever
    /// address it is handed.
    public static boolean developmentAllowed() {
        String testing = System.getProperty("bastion.server.url");
        if (testing != null && testing.length() > 0) {
            return true;
        }
        if (!Display.isInitialized()) {
            return false;
        }
        if (CN.isSimulator()) {
            return true;
        }
        String origin = pageOrigin();
        return origin != null && isLoopback(hostOf(origin));
    }

    /// The address of the page the app is running in -- `https://host`, with
    /// no path -- or null when it is not running in a browser.
    public static String pageOrigin() {
        if (!Display.isInitialized()) {
            return null;
        }
        String origin = CN.getProperty("browser.window.location.origin", null);
        if (origin == null || !(origin.regionMatches(true, 0, "http://", 0, 7)
                || origin.regionMatches(true, 0, "https://", 0, 8))) {
            return null;
        }
        return clean(origin);
    }

    /// Where the server is asked to send the authorization code. The
    /// installed app names the loopback address and reads the code off the
    /// redirect without following it. A browser follows a redirect itself and
    /// will not show the page an answer from another origin, so there the code
    /// is sent to the server the page came from, which hands it back
    /// (`SignInCodePage` on the server).
    public static String redirectUri() {
        String origin = pageOrigin();
        return origin == null ? REDIRECT_URI : origin + WEB_REDIRECT_PATH;
    }

    /// Whether `url` is a development server this run may use: one on this
    /// machine (`localhost` or a loopback address), in a run where
    /// [#developmentAllowed]. The ONE exemption from "no cleartext": a server
    /// started with `cn1:backend` has no certificate, and traffic to it never
    /// leaves the machine. It is also exempt from the host allow-list and the
    /// pins, and the Security Center says so in red while it is in use.
    ///
    /// The Android emulator's alias for the host machine (`10.0.2.2`) is
    /// deliberately not one: that traffic crosses a virtual network into
    /// another machine, and an app on an emulator is a release-shaped build.
    /// Point an emulator at an `https` address instead.
    public static boolean isDevelopmentServer(String url) {
        return isLoopback(hostOf(url)) && developmentAllowed();
    }

    /// Whether `host` (lower case) is this machine.
    static boolean isLoopback(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
    }

    /// Whether `host` (lower case) is one of [#ALLOWED_HOSTS].
    public static boolean isAllowedHost(String host) {
        for (String allowed : ALLOWED_HOSTS) {
            if (allowed.equals(host)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHttps(String url) {
        return url != null && url.regionMatches(true, 0, "https://", 0, 8);
    }

    /// The host of an `http(s)` URL, in lower case, without the port; null when
    /// it is not one.
    public static String hostOf(String url) {
        if (url == null) {
            return null;
        }
        int start;
        if (url.regionMatches(true, 0, "https://", 0, 8)) {
            start = 8;
        } else if (url.regionMatches(true, 0, "http://", 0, 7)) {
            start = 7;
        } else {
            return null;
        }
        // The authority runs to the first '/', '?' or '#'. Inside it the host
        // follows the LAST '@' (user info may hold ':' and '@' of its own:
        // `http://localhost:1@evil.example/` is a request to evil.example),
        // and ends at the port's ':' -- after the ']' of an IPv6 literal.
        int end = start;
        while (end < url.length()) {
            char c = url.charAt(end);
            if (c == '/' || c == '?' || c == '#' || c == '\\') {
                break;
            }
            end++;
        }
        String authority = url.substring(start, end);
        int at = authority.lastIndexOf('@');
        String host = at >= 0 ? authority.substring(at + 1) : authority;
        if (host.startsWith("[")) {
            int close = host.indexOf(']');
            if (close < 0 || (close + 1 < host.length() && host.charAt(close + 1) != ':')) {
                return null;
            }
            host = host.substring(0, close + 1);
        } else {
            int colon = host.indexOf(':');
            if (colon >= 0) {
                host = host.substring(0, colon);
            }
        }
        if (host.length() == 0) {
            return null;
        }
        return asciiLower(host);
    }

    /// [#PINS] as a host-to-`Vector` table, the shape `PinSet` takes.
    public static Hashtable pinTable() {
        Hashtable table = new Hashtable();
        for (String[] row : PINS) {
            Vector pins = new Vector();
            for (int iter = 1; iter < row.length; iter++) {
                pins.addElement(row[iter]);
            }
            table.put(asciiLower(row[0]), pins);
        }
        return table;
    }

    /// Folded by hand: a host name is ASCII, and `toLowerCase()` follows the
    /// device's language (under Turkish, `I` does not fold to `i`).
    public static String asciiLower(String text) {
        char[] chars = text.toCharArray();
        for (int iter = 0; iter < chars.length; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }

    private static String clean(String url) {
        String text = url == null ? "" : url.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.length() == 0) {
            return developmentAllowed() ? DEVELOPMENT_SERVER : RELEASE_SERVER;
        }
        if (!text.regionMatches(true, 0, "http://", 0, 7)
                && !text.regionMatches(true, 0, "https://", 0, 8)) {
            text = "https://" + text;
        }
        return text;
    }
}
