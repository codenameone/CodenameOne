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
package com.codename1.maven;

import com.codename1.build.Log;
import org.apache.commons.io.IOUtils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.prefs.Preferences;

/// Finds the Codename One signing API token the Certificate Wizard runs with:
/// the one given explicitly, else the one cached from an earlier sign-in, else
/// a browser sign-in when that is allowed. Shared by the Maven
/// `cn1:certificatewizard` goal and the Gradle `certificateWizard` task.
public final class CodenameOneLogin {
    private final Log log;

    /// A login helper logging to `log`.
    public CodenameOneLogin(Log log) {
        this.log = log;
    }

    /// The token and user to run with. Caches a fresh sign-in the same way the
    /// desktop tools do, in the `/com/codename1/ui` preferences node.
    ///
    /// @param token an explicit token, or null
    /// @param user an explicit user, or null
    /// @param login whether a browser sign-in may be started
    public LoginResult resolve(String token, String user, boolean login, int loginTimeoutSeconds, String baseUrl) {
        Preferences prefs = Preferences.userRoot().node("/com/codename1/ui");
        String effectiveToken = firstNonEmpty(token, "");
        String effectiveUser = firstNonEmpty(user, prefs.get("user", null), "");
        String cachedToken = firstNonEmpty(prefs.get("token", null), "");
        if (effectiveToken.length() == 0 && isUsableJwt(cachedToken)) {
            effectiveToken = cachedToken;
            log.debug("Using cached Codename One signing API JWT");
        }
        if (login && effectiveToken.length() == 0) {
            LoginResult result = interactiveLogin(baseUrl, loginTimeoutSeconds);
            if (result != null) {
                effectiveToken = result.token;
                effectiveUser = firstNonEmpty(user, result.user, effectiveUser);
                prefs.put("token", effectiveToken);
                if (effectiveUser.length() > 0) {
                    prefs.put("user", effectiveUser);
                }
            }
        }
        return new LoginResult(effectiveToken, effectiveUser);
    }

    private LoginResult interactiveLogin(String baseUrl, int loginTimeoutSeconds) {
        if (loginTimeoutSeconds <= 0) {
            return null;
        }
        String key = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + loginTimeoutSeconds * 1000L;
        try {
            String root = normalizeBaseUrl(baseUrl);
            String redirect = root + "/loggedIn.html";
            String loginUrl = root + "/appsec/7.0/set-user?redirect=" + enc(redirect)
                    + "&loginKey=" + enc(key);
            log.info("Opening Codename One sign-in for certificate wizard authentication");
            log.info(loginUrl);
            openBrowser(loginUrl);
            while (System.currentTimeMillis() < deadline) {
                LoginResult result = pollLogin(root, key);
                if (result != null) {
                    log.info("Received Codename One signing API token for " + result.user);
                    return result;
                }
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            log.warn("Timed out waiting for Codename One browser login");
        } catch (Exception ex) {
            log.warn("Unable to complete browser login: " + ex.getMessage());
        }
        return null;
    }

    private LoginResult pollLogin(String root, String key) throws IOException {
        URL url = new URL(root + "/poll-user?ver=2&loginKey=" + enc(key));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestMethod("GET");
            int code = con.getResponseCode();
            if (code == 404) {
                return null;
            }
            if (code != 200) {
                log.debug("Codename One login poll returned HTTP " + code);
                return null;
            }
            String body = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8).trim();
            if (body.length() == 0) {
                return null;
            }
            String[] lines = body.split("\\r?\\n", 2);
            String receivedToken = lines[0].trim();
            if (receivedToken.length() == 0) {
                return null;
            }
            String receivedUser = lines.length > 1 ? lines[1].trim() : "";
            return new LoginResult(receivedToken, receivedUser);
        } finally {
            con.disconnect();
        }
    }

    private void openBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            log.warn("Desktop browsing is not available. Open the URL above in a browser to continue.");
        }
    }

    private static String firstNonEmpty(String a, String b, String c) {
        if (a != null && a.trim().length() > 0) {
            return a.trim();
        }
        if (b != null && b.trim().length() > 0) {
            return b.trim();
        }
        return c == null ? "" : c;
    }

    private static String firstNonEmpty(String a, String b) {
        return firstNonEmpty(a, b, "");
    }

    private static String normalizeBaseUrl(String url) {
        String out = url == null || url.trim().length() == 0
                ? "https://cloud.codenameone.com" : url.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8");
    }

    public static boolean isUsableJwt(String token) {
        long expiresAt = jwtExpiresAt(token);
        return expiresAt > System.currentTimeMillis() + 120000L;
    }

    static long jwtExpiresAt(String token) {
        if (token == null) {
            return -1L;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length < 2) {
            return -1L;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            String payload = new String(decoded, StandardCharsets.UTF_8);
            long exp = numericJsonClaim(payload, "exp");
            return exp <= 0L ? -1L : exp * 1000L;
        } catch (RuntimeException ex) {
            return -1L;
        }
    }

    private static long numericJsonClaim(String json, String name) {
        String quoted = "\"" + name + "\"";
        int idx = json.indexOf(quoted);
        if (idx < 0) {
            return -1L;
        }
        int colon = json.indexOf(':', idx + quoted.length());
        if (colon < 0) {
            return -1L;
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        if (end == start) {
            return -1L;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private static String padBase64(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value);
        for (int i = remainder; i < 4; i++) {
            out.append('=');
        }
        return out.toString();
    }

    public static final class LoginResult {
        /// The bearer token.
        public final String token;
        /// The account it belongs to, or empty.
        public final String user;

        LoginResult(String token, String user) {
            this.token = token;
            this.user = user == null ? "" : user;
        }
    }
}
