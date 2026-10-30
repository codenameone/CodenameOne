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
package com.codename1.backend;

import com.codename1.crypto.rpc.RpcException;
import com.codename1.crypto.rpc.RpcTransport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The backend's [RpcTransport]: the chain clients of `com.codename1.crypto.rpc` (`EvmRpc`,
/// `EsploraClient`, `SolanaRpc`) send through a [WebClient], so they share its connection pool,
/// timeouts and certificate checks.
///
/// ```java
/// WebClient web = new WebClient().setRequestTimeoutMillis(15000);
/// EvmRpc eth = new EvmRpc(new WebRpcTransport(web).header("Authorization", "Bearer " + key), url);
/// ```
///
/// Create one per provider and share it; it holds no state per request.
public final class WebRpcTransport implements RpcTransport {
    private final WebClient client;
    private final List<String> headers = new ArrayList<String>();

    /// A transport over a client.
    public WebRpcTransport(WebClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client");
        }
        this.client = client;
    }

    /// A transport over a client with every default.
    public WebRpcTransport() {
        this(new WebClient());
    }

    /// Adds a header sent with every request, for instance an API key. Configure the
    /// transport before sharing it between threads.
    public WebRpcTransport header(String name, String value) {
        headers.add(name + ": " + value);
        return this;
    }

    @Override
    public String send(String method, String url, String contentType, String body) throws IOException {
        List<String> h = new ArrayList<String>(headers);
        if (contentType != null) {
            h.add("Content-Type: " + contentType);
        }
        Web.Result r = client.request(method, url, h, body == null ? null : body.getBytes("UTF-8"));
        String text = r.getBodyAsString();
        if (r.getStatus() < 200 || r.getStatus() > 299) {
            throw RpcException.http(r.getStatus(), text);
        }
        return text == null ? "" : text;
    }
}
