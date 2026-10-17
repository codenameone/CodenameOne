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
package com.codenameone.examples.savor;

import com.codename1.io.CharArrayReader;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.RequestAuthorizer;
import com.codename1.ui.CN;
import com.codenameone.examples.savor.net.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// The other person in a test. The app on screen is one user; a delivery needs
/// two, so the second one acts through the API directly, with their own token.
///
/// Their requests opt out of the app's authorizer, which would otherwise put
/// the on-screen user's token on anything sent to `/api`.
final class Actor {
    private final String token;
    int status;

    /// One sign-in for each account for the whole run. The server limits how
    /// often an address may sign in, as it should, and a suite that signed in
    /// again for every step would be turned away by it.
    private static final Map<String, Actor> SIGNED_IN = new HashMap<String, Actor>();

    private Actor(String token) {
        this.token = token;
    }

    /// Signs `email` in the way the app does, and keeps the token to itself.
    static Actor signIn(String email) {
        Actor known = SIGNED_IN.get(email);
        if (known != null) {
            return new Actor(known.token);
        }
        Actor fresh = authenticate(email);
        SIGNED_IN.put(email, fresh);
        return fresh;
    }

    /// Whether `email` can sign in at this moment, asked of the server and
    /// kept by nobody: this is how a test sees a blocked account refused.
    static boolean canSignIn(String email) {
        try {
            authenticate(email);
            return true;
        } catch (RuntimeException refused) {
            return false;
        }
    }

    private static Actor authenticate(String email) {
        final String[] token = new String[1];
        final String[] problem = new String[1];
        CN.callSerially(() -> Session.authenticate(email, E2e.PASSWORD,
                tokens -> token[0] = tokens.getAccessToken(),
                (status, message) -> problem[0] = status + " " + message));
        long deadline = System.currentTimeMillis() + 30000;
        while (token[0] == null && problem[0] == null && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException stop) {
                break;
            }
        }
        if (token[0] == null) {
            throw new RuntimeException("Could not sign " + email + " in: " + problem[0]);
        }
        return new Actor(token[0]);
    }

    Map<String, Object> get(String path) {
        return send("GET", path, null);
    }

    Map<String, Object> put(String path, String json) {
        return send("PUT", path, json);
    }

    Map<String, Object> post(String path, String json) {
        return send("POST", path, json == null ? "{}" : json);
    }

    private Map<String, Object> send(String method, String path, String json) {
        ConnectionRequest request = new ConnectionRequest();
        request.setUrl(AppConfig.serverUrl() + path);
        request.setHttpMethod(method);
        request.setPost(json != null);
        if (json != null) {
            request.setContentType("application/json");
            request.setRequestBody(json);
        }
        request.addRequestHeader("Authorization", "Bearer " + token);
        request.setAuthorizer(RequestAuthorizer.NONE);
        request.setReadResponseForErrors(true);
        request.setFailSilently(true);
        request.setDuplicateSupported(true);
        NetworkManager.getInstance().addToQueueAndWait(request);
        status = request.getResponseCode();
        byte[] body = request.getResponseData();
        // A list comes back as the parser gives it: under "root".
        if (body == null || body.length == 0 || body[0] != '{' && body[0] != '[') {
            return new HashMap<String, Object>();
        }
        try {
            return new JSONParser().parseJSON(new CharArrayReader(
                    new String(body, "UTF-8").toCharArray()));
        } catch (IOException malformed) {
            return new HashMap<String, Object>();
        }
    }

    /// What `path` answers when it answers a list.
    java.util.List<Object> list(String path) {
        Object root = get(path).get("root");
        return root instanceof java.util.List ? (java.util.List<Object>) root
                : new java.util.ArrayList<Object>();
    }

    /// A courier's position report.
    Map<String, Object> online(boolean online, double lat, double lng) {
        return post("/api/courier/status", "{\"online\":" + online + ",\"lat\":" + lat
                + ",\"lng\":" + lng + ",\"heading\":0}");
    }

    static String text(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value instanceof String ? (String) value : "";
    }
}
