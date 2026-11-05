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
package com.codenameone.examples.slate.sync;

import com.codename1.backend.Json;
import com.codename1.backend.Web;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// What the Google and Microsoft calendars have in common: an OAuth client
/// that trades the app's authorization code for a refresh token once, and the
/// refresh token for an access token on every read.
///
/// The app runs the consent screen itself, with PKCE and the platform's
/// browser (`com.codename1.io.Oauth2` on the device), and sends the code it
/// got back. The code is exchanged here, with the client secret only the
/// server knows, so the refresh token never sits on a phone.
abstract class OAuthCalendar implements ExternalCalendar {
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    OAuthCalendar(String tokenUrl, String clientId, String clientSecret) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String connect(String code, String redirectUri) throws IOException {
        Map answer = token("grant_type=authorization_code&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri));
        Object refresh = answer.get("refresh_token");
        if (!(refresh instanceof String) || ((String) refresh).length() == 0) {
            throw new IOException("The calendar did not grant offline access. Connect it again"
                    + " and allow Slate to see when you are busy");
        }
        return (String) refresh;
    }

    /// An access token for one read.
    String access(String refreshToken) throws IOException {
        Map answer = token("grant_type=refresh_token&refresh_token=" + enc(refreshToken));
        Object access = answer.get("access_token");
        if (!(access instanceof String)) {
            throw new IOException("The calendar no longer accepts Slate. Connect it again");
        }
        return (String) access;
    }

    /// Sends JSON with a bearer token and answers the parsed reply.
    static Map call(String method, String url, String bearer, String json) throws IOException {
        List headers = new ArrayList();
        headers.add("Authorization: Bearer " + bearer);
        headers.add("Content-Type: application/json");
        headers.add("Prefer: outlook.timezone=\"UTC\"");
        Web.Result answer = Web.request(method, url, headers,
                json == null ? null : json.getBytes("UTF-8"));
        if (answer.getStatus() / 100 != 2) {
            throw new IOException("The calendar answered " + answer.getStatus());
        }
        return Json.parseObject(answer.getBodyAsString());
    }

    private Map token(String form) throws IOException {
        List headers = new ArrayList();
        headers.add("Content-Type: application/x-www-form-urlencoded");
        String body = form + "&client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret);
        Web.Result answer = Web.request("POST", tokenUrl, headers, body.getBytes("UTF-8"));
        if (answer.getStatus() / 100 != 2) {
            throw new IOException("The calendar refused the sign-in (" + answer.getStatus() + ")");
        }
        return Json.parseObject(answer.getBodyAsString());
    }

    static String enc(String value) throws IOException {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    /// The calendars a link names, comma separated, or `primary` when it names
    /// none.
    static List<String> calendars(String csv) {
        List<String> out = new ArrayList<String>();
        if (csv != null) {
            String[] parts = csv.split(",");
            for (int iter = 0; iter < parts.length; iter++) {
                if (parts[iter].trim().length() > 0) {
                    out.add(parts[iter].trim());
                }
            }
        }
        if (out.isEmpty()) {
            out.add("primary");
        }
        return out;
    }
}
