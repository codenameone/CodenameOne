/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.rpc;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/// The application's [RpcTransport]: each request goes through the `NetworkManager` queue as
/// a `ConnectionRequest`, waiting with `addToQueueAndWait`, so a call from the EDT keeps the
/// UI responsive. Headers added with [#header] go on every request, which is how a provider's
/// API key is passed.
///
/// Not shared with the backend, which has `com.codename1.backend.WebRpcTransport` instead.
public class NetworkTransport implements RpcTransport {
    private final List<String[]> headers = new ArrayList<String[]>();
    private int timeout = 30000;

    /// Adds a header sent with every request, for instance an API key.
    public NetworkTransport header(String name, String value) {
        headers.add(new String[] {name, value});
        return this;
    }

    /// The request timeout in milliseconds; 30 seconds by default.
    public NetworkTransport timeout(int millis) {
        timeout = millis;
        return this;
    }

    @Override
    public String send(String method, String url, String contentType, String body) throws IOException {
        Request r = new Request();
        r.setUrl(url);
        r.setHttpMethod(method);
        r.setPost("POST".equals(method));
        r.setFailSilently(true);
        r.setReadResponseForErrors(true);
        r.setDuplicateSupported(true);
        r.setTimeout(timeout);
        for (int i = 0; i < headers.size(); i++) {
            r.addRequestHeader(headers.get(i)[0], headers.get(i)[1]);
        }
        if (contentType != null) {
            r.setContentType(contentType);
        }
        if (body != null) {
            r.setRequestBody(body);
        }
        NetworkManager.getInstance().addToQueueAndWait(r);
        if (r.failure != null) {
            if (r.failure instanceof IOException) {
                throw (IOException) r.failure;
            }
            throw new IOException(String.valueOf(r.failure.getMessage()));
        }
        byte[] data = r.getResponseData();
        String text = data == null ? "" : utf8(data);
        int code = r.errorCode != 0 ? r.errorCode : r.getResponseCode();
        if (code < 200 || code > 299) {
            throw RpcException.http(code, text);
        }
        return text;
    }

    private static String utf8(byte[] data) throws IOException {
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IOException("UTF-8 is unavailable");
        }
    }

    // Records what ConnectionRequest would otherwise show in a dialog.
    private static final class Request extends ConnectionRequest {
        Exception failure;
        int errorCode;

        @Override
        protected void handleException(Exception err) {
            failure = err;
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            errorCode = code;
        }
    }
}
