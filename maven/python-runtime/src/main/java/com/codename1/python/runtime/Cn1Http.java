/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Data;
import com.codename1.io.NetworkManager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// HTTP through Codename One's `ConnectionRequest`: the platform's own
/// client on every target, with its proxy, certificate and cookie handling.
final class Cn1Http extends PyHttp {
    @Override
    public Response send(String method, String url, String[] headers, final byte[] body, int timeoutMillis,
            boolean followRedirects) throws IOException {
        Request r = new Request();
        r.setUrl(url);
        r.setHttpMethod(method);
        r.setPost(body != null);
        r.setFailSilently(true);
        r.setReadResponseForErrors(true);
        r.setDuplicateSupported(true);
        r.setFollowRedirects(followRedirects);
        if (timeoutMillis > 0) {
            r.setTimeout(timeoutMillis);
            r.setReadTimeout(timeoutMillis);
        }
        for (int i = 0; i + 1 < headers.length; i += 2) {
            r.addRequestHeader(headers[i], headers[i + 1]);
        }
        if (body != null) {
            r.setRequestBody(new Data() {
                @Override
                public void appendTo(OutputStream output) throws IOException {
                    output.write(body);
                }

                @Override
                public long getSize() {
                    return body.length;
                }
            });
        }
        NetworkManager.getInstance().addToQueueAndWait(r);
        if (r.failure != null && r.getResponseCode() <= 0) {
            throw r.failure instanceof IOException ? (IOException) r.failure
                    : new IOException(String.valueOf(r.failure.getMessage()));
        }
        int status = r.getResponseCode();
        if (status <= 0) {
            throw new IOException("no response from " + url);
        }
        String reason = r.getResponseErrorMessage();
        byte[] data = r.getResponseData();
        return new Response(status, reason == null ? "" : reason, url, r.headers.toArray(new String[0]),
                data == null ? new byte[0] : data);
    }

    /// Keeps what Python needs: every header as received, and the failure
    /// instead of the default error dialog.
    private static final class Request extends ConnectionRequest {
        final List<String> headers = new ArrayList<String>();
        Exception failure;

        @Override
        protected void readHeaders(Object connection) throws IOException {
            String[] names = getHeaderFieldNames(connection);
            if (names == null) {
                return;
            }
            for (String n : names) {
                if (n == null) {
                    continue;
                }
                String[] values = getHeaders(connection, n);
                if (values == null) {
                    continue;
                }
                for (String v : values) {
                    headers.add(n);
                    headers.add(v);
                }
            }
        }

        @Override
        protected void handleException(Exception err) {
            failure = err;
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            // The response, body included, is the caller's to read.
        }
    }
}
