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
package com.codename1.art;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Fetches a file from Figma's REST API and reads it into an `ArtDocument`.
///
/// The token is a personal access token (or OAuth token) the user supplies. It is kept only
/// in this object, sent only to `api.figma.com` in the `X-Figma-Token` header, and never
/// stored or logged. Requests block until they finish, so call from a background thread or
/// through `invokeAndBlock`.
///
/// ```java
/// FigmaClient figma = new FigmaClient(tokenTheUserTyped);
/// ArtDocument doc = figma.fetch("FILE_KEY_FROM_THE_URL");
/// ```
public final class FigmaClient {
    /// The API this client talks to.
    public static final String API = "https://api.figma.com/v1/";
    /// The most image fills downloaded for one file.
    public static final int MAX_IMAGES = 1000;

    private final String token;
    private boolean bearer;

    /// Creates a client.
    ///
    /// #### Parameters
    ///
    /// - `token`: a personal access token
    public FigmaClient(String token) {
        if (token == null || token.length() == 0) {
            throw new IllegalArgumentException("A Figma token is required");
        }
        this.token = token;
    }

    /// Sends the token as an OAuth bearer token instead of a personal access token.
    ///
    /// #### Parameters
    ///
    /// - `oauth`: true for an OAuth token
    public FigmaClient setOAuth(boolean oauth) {
        this.bearer = oauth;
        return this;
    }

    /// Fetches a file with its geometry and image fills and reads it.
    ///
    /// #### Parameters
    ///
    /// - `fileKey`: the key in the file's URL, `figma.com/design/KEY/...`
    public ArtDocument fetch(String fileKey) throws IOException {
        checkKey(fileKey);
        byte[] json = get(API + "files/" + fileKey + "?geometry=paths");
        Map<String, Object> parsed = JSONParser.parseJSON(json);
        Map<String, byte[]> images = new HashMap<String, byte[]>();
        List<String> refs = FigmaReader.imageRefs(parsed);
        if (!refs.isEmpty()) {
            Map<String, Object> fills = JSONParser.parseJSON(get(API + "files/" + fileKey + "/images"));
            Map<String, Object> urls = Json.obj(Json.obj(fills, "meta"), "images");
            if (urls != null) {
                int count = 0;
                Iterator<Map.Entry<String, Object>> it = urls.entrySet().iterator();
                while (it.hasNext() && count < MAX_IMAGES) {
                    Map.Entry<String, Object> en = it.next();
                    Object url = en.getValue();
                    if (url instanceof String && refs.contains(en.getKey()) && ((String) url).startsWith("https://")) {
                        images.put(en.getKey(), download((String) url));
                        count++;
                    }
                }
            }
        }
        return FigmaReader.read(json, images);
    }

    /// The raw JSON of any API path, such as `files/KEY/nodes?ids=1:2&geometry=paths`.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path after `/v1/`
    public byte[] get(String path) throws IOException {
        String url = path.startsWith(API) ? path : API + path;
        return request(url, true);
    }

    private byte[] download(String url) throws IOException {
        // Image URLs point at Figma's storage, not the API; they are signed and need no token.
        return request(url, false);
    }

    private static void checkKey(String key) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("A file key is required");
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_')) {
                throw new IllegalArgumentException("Not a Figma file key: " + key);
            }
        }
    }

    private byte[] request(String url, boolean auth) throws IOException {
        Request r = new Request();
        r.setUrl(url);
        r.setPost(false);
        r.setFailSilently(true);
        r.setReadResponseForErrors(true);
        r.setDuplicateSupported(true);
        if (auth) {
            if (bearer) {
                r.addRequestHeader("Authorization", "Bearer " + token);
            } else {
                r.addRequestHeader("X-Figma-Token", token);
            }
        }
        NetworkManager.getInstance().addToQueueAndWait(r);
        if (r.failure != null) {
            IOException e = new IOException("Figma request failed: " + r.failure.getMessage());
            e.initCause(r.failure);
            throw e;
        }
        int code = r.getResponseCode();
        byte[] data = r.getResponseData();
        if (code == 403) {
            throw new IOException("Figma refused the token (403)");
        }
        if (code == 404) {
            throw new IOException("Figma file not found (404)");
        }
        if (code == 429) {
            throw new IOException("Figma rate limit reached (429); try again later");
        }
        if (code < 200 || code >= 300 || data == null) {
            throw new IOException("Figma request failed with HTTP " + code);
        }
        return data;
    }

    private static final class Request extends ConnectionRequest {
        private Exception failure;

        @Override
        protected void handleException(Exception err) {
            failure = err;
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            // Reported by the caller from the response code.
        }
    }
}
