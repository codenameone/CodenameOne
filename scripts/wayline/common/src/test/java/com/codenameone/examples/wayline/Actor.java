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

import com.codename1.io.CharArrayReader;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.RequestAuthorizer;
import com.codename1.ui.CN;
import com.codenameone.examples.wayline.net.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// The other person in a test. The app on screen is one user; a ride needs
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
        if (body == null || body.length == 0 || body[0] != '{') {
            return new HashMap<String, Object>();
        }
        try {
            return new JSONParser().parseJSON(new CharArrayReader(
                    new String(body, "UTF-8").toCharArray()));
        } catch (IOException malformed) {
            return new HashMap<String, Object>();
        }
    }

    /// A driver's position report.
    Map<String, Object> online(boolean online, double lat, double lng) {
        return post("/api/driver/status", "{\"online\":" + online + ",\"lat\":" + lat
                + ",\"lng\":" + lng + ",\"heading\":0}");
    }

    static String text(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value instanceof String ? (String) value : "";
    }
}
