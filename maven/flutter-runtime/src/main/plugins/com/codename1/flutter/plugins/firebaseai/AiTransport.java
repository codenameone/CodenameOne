/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.firebaseai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Display;

import dart.async.Completer;
import dart.async.Future;

/** One JSON POST over ConnectionRequest; the reply is decoded, an error reply becomes a FirebaseAIException. */
final class AiTransport {
    private AiTransport() {
    }

    static Future<Map<String, Object>> post(String url, final List<String[]> headers, String json) {
        final Completer<Map<String, Object>> done = new Completer<Map<String, Object>>();
        if (!Display.isInitialized()) {
            done.completeError(new FirebaseAIException(
                    "A network error has occurred: Codename One is not initialized"));
            return done.future();
        }
        final byte[] payload;
        try {
            payload = json.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            done.completeError(new FirebaseAIException(String.valueOf(e)));
            return done.future();
        }
        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        ConnectionRequest req = new ConnectionRequest() {
            private Exception failure;
            private boolean settled;

            @Override
            protected void buildRequestBody(OutputStream os) throws IOException {
                os.write(payload);
            }

            @Override
            protected void readResponse(InputStream input) throws IOException {
                byte[] buf = new byte[4096];
                int n;
                while ((n = input.read(buf)) > 0) {
                    received.write(buf, 0, n);
                }
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // the error body carries the server's message; read in postResponse
            }

            /// Transport failures reach only this, so they settle here, on the EDT.
            @Override
            protected void handleException(Exception err) {
                failure = err;
                if (Display.isInitialized() && !Display.getInstance().isEdt()) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            postResponse();
                        }
                    });
                } else {
                    postResponse();
                }
            }

            @Override
            protected void postResponse() {
                if (settled) {
                    return;
                }
                settled = true;
                if (failure != null) {
                    done.completeError(new FirebaseAIException(
                            "A network error has occurred: " + failure.getMessage()));
                    return;
                }
                String body;
                try {
                    body = new String(received.toByteArray(), "UTF-8");
                } catch (java.io.UnsupportedEncodingException e) {
                    done.completeError(new FirebaseAIException(String.valueOf(e)));
                    return;
                }
                try {
                    done.complete(GenerateContentResponse.parse(body));
                } catch (FirebaseAIException e) {
                    if (getResponseCode() >= 400 && body.length() == 0) {
                        done.completeError(new FirebaseAIException("HTTP " + getResponseCode()));
                    } else {
                        done.completeError(e);
                    }
                }
            }
        };
        req.setUrl(url);
        req.setPost(true);
        req.setHttpMethod("POST");
        req.setContentType("application/json");
        for (String[] h : headers) {
            req.addRequestHeader(h[0], h[1]);
        }
        req.setReadResponseForErrors(true);
        req.setDuplicateSupported(true);
        NetworkManager.getInstance().addToQueue(req);
        return done.future();
    }
}
