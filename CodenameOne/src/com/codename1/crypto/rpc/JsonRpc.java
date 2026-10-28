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

import com.codename1.io.JSONParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// JSON-RPC 2.0 over an [RpcTransport]: what Ethereum and Solana nodes speak. Parameters and
/// results are the values [com.codename1.io.JSONParser#parseStrict(String, boolean)] reads and
/// [com.codename1.io.JSONParser#toJson(Object)] writes: `Map`, `List`, `String`, `Boolean`, `Long`,
/// `Double`, and an integer past a `long` as a [com.codename1.io.JSONParser.RawJson] holding its
/// exact digits.
@com.codename1.impl.SharedWithBackend
public class JsonRpc {
    private final RpcTransport transport;
    private final String url;
    private long nextId = 1;

    /// A client for an endpoint.
    public JsonRpc(RpcTransport transport, String url) {
        if (transport == null || url == null) {
            throw new IllegalArgumentException("transport and url are required");
        }
        this.transport = transport;
        this.url = url;
    }

    /// The endpoint.
    public String getUrl() {
        return url;
    }

    /// The transport.
    public RpcTransport getTransport() {
        return transport;
    }

    /// Calls a method and answers its `result`, which may be `null`.
    ///
    /// #### Throws
    ///
    /// - `RpcException`: for an `error` reply or an HTTP failure
    /// - `IOException`: when the endpoint could not be reached, or answered something that is
    ///   not a JSON-RPC response
    public Object call(String method, Object... params) throws IOException {
        return callWith(method, Arrays.asList(params));
    }

    /// Calls a method with a parameter list, or a parameter object when `params` is a `Map`.
    /// It is not an overload of [#call(String, Object...)], which would take a single
    /// parameter for the whole list.
    public Object callWith(String method, Object params) throws IOException {
        Map<String, Object> req = new LinkedHashMap<String, Object>();
        long id = nextId++;
        req.put("jsonrpc", "2.0");
        req.put("id", Long.valueOf(id));
        req.put("method", method);
        req.put("params", params == null ? new ArrayList<Object>() : params);
        String reply = transport.send("POST", url, "application/json", JSONParser.toJson(req));
        Object parsed;
        try {
            parsed = JSONParser.parseStrict(reply, true);
        } catch (IOException e) {
            throw new IOException("not a JSON-RPC response from " + url + ": " + e.getMessage(), e);
        }
        if (!(parsed instanceof Map)) {
            throw new IOException("not a JSON-RPC response from " + url);
        }
        Map<?, ?> m = (Map<?, ?>) parsed;
        Object error = m.get("error");
        if (error instanceof Map) {
            Map<?, ?> e = (Map<?, ?>) error;
            Object code = e.get("code");
            Object msg = e.get("message");
            throw new RpcException(code instanceof Number ? ((Number) code).intValue() : 0,
                    msg instanceof String ? (String) msg : "JSON-RPC error", e.get("data"), false);
        }
        if (!m.containsKey("result")) {
            throw new IOException("JSON-RPC response with neither result nor error");
        }
        return m.get("result");
    }

    /// A result that must be a JSON object.
    protected static Map<?, ?> asMap(Object o, String what) throws IOException {
        if (!(o instanceof Map)) {
            throw new IOException(what + ": expected an object");
        }
        return (Map<?, ?>) o;
    }

    /// A result that must be a JSON array.
    protected static List<?> asList(Object o, String what) throws IOException {
        if (!(o instanceof List)) {
            throw new IOException(what + ": expected an array");
        }
        return (List<?>) o;
    }

    /// A result that must be a JSON string.
    protected static String asString(Object o, String what) throws IOException {
        if (!(o instanceof String)) {
            throw new IOException(what + ": expected a string");
        }
        return (String) o;
    }

    /// A result that must be a JSON integer within `long` range.
    protected static long asLong(Object o, String what) throws IOException {
        if (o instanceof Long || o instanceof Integer) {
            return ((Number) o).longValue();
        }
        throw new IOException(what + ": expected an integer");
    }
}
