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
package com.codename1.backend.security;

import com.codename1.backend.HttpServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What a chain keeps about the request the calling thread is serving, beyond
/// the request itself: attributes filters hand one another, and headers for the
/// response that is yet to come back.
///
/// A [SecurityFilter] does not write into the response it gets from the rest of
/// the chain, because a handler may answer every request with one shared
/// object. It records the header here instead, and the server writes it into
/// its own copy of the response:
///
/// ```java
/// SecurityExchange.current().setResponseHeader("X-Request-Trace", id);
/// ```
public final class SecurityExchange {
    private static final ThreadLocal<SecurityExchange> CURRENT = new ThreadLocal<SecurityExchange>();

    private final HttpServer.Request request;
    private final boolean secure;
    private SecurityFilterChain chain;
    private String path;
    private Map<String, Object> attributes;
    /// {name, value, "add" or "set"}, in the order they were recorded.
    private List<String[]> headers;

    SecurityExchange(HttpServer.Request request, boolean secure) {
        this.request = request;
        this.secure = secure;
    }

    /// The exchange of the request this thread is serving under a chain, or null
    /// when it serves none.
    public static SecurityExchange current() {
        return CURRENT.get();
    }

    static void enter(SecurityExchange exchange) {
        CURRENT.set(exchange);
    }

    static void leave() {
        CURRENT.set(null);
    }

    /// The exchange of `request`, when it is the one this thread is serving.
    static SecurityExchange of(HttpServer.Request request) {
        SecurityExchange exchange = CURRENT.get();
        return exchange != null && exchange.request == request ? exchange : null; //NOPMD CompareObjectsWithEquals - the request object itself
    }

    /// The path of `request` as the routers compare it, computed once for the
    /// request this thread is serving: every rule of a chain asks for it.
    static String path(HttpServer.Request request) {
        SecurityExchange exchange = of(request);
        if (exchange == null) {
            return canonicalPath(request);
        }
        if (exchange.path == null) {
            exchange.path = canonicalPath(request);
        }
        return exchange.path;
    }

    private static String canonicalPath(HttpServer.Request request) {
        return request.getTarget() == null ? "" : request.pathFrom(0);
    }

    public HttpServer.Request getRequest() {
        return request;
    }

    /// Whether the request arrived over TLS: terminated by this server, or by
    /// a proxy it trusts; see [HttpServer.Request#isSecure].
    public boolean isSecure() {
        return secure;
    }

    /// The chain guarding the request.
    public SecurityFilterChain getFilterChain() {
        return chain;
    }

    void setFilterChain(SecurityFilterChain chain) {
        this.chain = chain;
    }

    /// Something an earlier filter recorded under `name`, or null.
    public Object getAttribute(String name) {
        return attributes == null ? null : attributes.get(name);
    }

    /// Records `value` for later filters and for the handler; null removes it.
    public void setAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new HashMap<String, Object>();
        }
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    /// Sets a header on the response this request will get, replacing one the
    /// handler set under the same name.
    public void setResponseHeader(String name, String value) {
        record(name, value, "set");
    }

    /// Adds a header to the response this request will get, beside any other of
    /// that name: one more `Set-Cookie`.
    public void addResponseHeader(String name, String value) {
        record(name, value, "add");
    }

    private void record(String name, String value, String how) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("A header needs a name and a value");
        }
        if (headers == null) {
            headers = new ArrayList<String[]>(2);
        }
        headers.add(new String[] {name, value, how});
    }

    List<String[]> responseHeaders() {
        return headers;
    }
}
