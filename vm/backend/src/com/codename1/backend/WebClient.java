/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// An outbound HTTP client with its own configuration: a client certificate for
/// mutual TLS, an HTTP/2 requirement, timeouts, and a pool of connections that
/// belongs to it alone.
///
/// The static methods on [Web] are a client with every default and no pool.
/// Create one of these when a request needs more than that:
///
/// ```java
/// WebClient bank = new WebClient()
///         .setClientCertificate(ClientCertificate.pkcs12(p12, password))
///         .setRequireHttp2(true)
///         .setRequestTimeoutMillis(10000);
/// Web.Result r = bank.getJson("https://api.bank.example/v1/balance", token);
/// ```
///
/// Create it once and share it -- it is safe to use from every handler at once
/// -- because the pool is what makes a second request to the same host skip the
/// TCP and TLS handshakes. [#close()] releases the pool.
///
/// **The configuration is fixed by the first request.** A setter called after
/// one throws IllegalStateException. Changing the credential of a client whose
/// pool already holds connections authenticated as somebody else is exactly how
/// two identities end up sharing a connection, so it is not possible; make a
/// second client instead. Two clients never share a connection, whatever their
/// configuration.
///
/// **Certificate and hostname verification are always on**, as they are for
/// [Web]: there is no option to turn them off, and a client certificate does not
/// change how the server's certificate is checked.
///
/// **HTTP/2.** With [#setRequireHttp2(boolean)] the request is made over HTTP/2
/// or not at all: a peer that does not speak it fails the request with an
/// IOException saying so, and the request is not sent to it as HTTP/1.1. Without
/// it the packaged binary offers HTTP/2 over TLS and falls back to HTTP/1.1 when
/// the server declines; [Web.Result#getProtocol()] says which was used. The local
/// Java SE runtime speaks HTTP/1.1 only (its client is HttpURLConnection, the
/// one every JDK this runs on has), so there a client that REQUIRES HTTP/2 fails
/// every request with an IOException that says so, rather than quietly
/// succeeding over a protocol the caller ruled out.
public final class WebClient {
    private boolean requireHttp2;
    private ClientCertificate clientCertificate;
    private int connectTimeoutMillis = 30000;
    private int readTimeoutMillis = 30000;
    private int requestTimeoutMillis;
    private boolean reuseConnections = true;
    private boolean used;
    private boolean closed;
    private int inFlight;

    /// The runtime's per-client transport (a connection pool), created by Web on
    /// first use and released by close().
    private Object transport;

    /// Requires HTTP/2: a peer that does not negotiate it fails the request.
    /// Default false.
    public synchronized WebClient setRequireHttp2(boolean requireHttp2) {
        checkUnused();
        this.requireHttp2 = requireHttp2;
        return this;
    }

    public synchronized boolean isRequireHttp2() {
        return requireHttp2;
    }

    /// The certificate presented when a server asks for one, or null for none.
    public synchronized WebClient setClientCertificate(ClientCertificate clientCertificate) {
        checkUnused();
        this.clientCertificate = clientCertificate;
        return this;
    }

    public synchronized ClientCertificate getClientCertificate() {
        return clientCertificate;
    }

    /// How long establishing the connection -- TCP and TLS -- may take. Default
    /// 30000; 0 means no limit.
    public synchronized WebClient setConnectTimeoutMillis(int millis) {
        checkUnused();
        this.connectTimeoutMillis = nonNegative(millis);
        return this;
    }

    public synchronized int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    /// How long the transfer may make NO progress before it is abandoned.
    /// Default 30000; 0 means no limit. A transfer that keeps moving is never cut
    /// off by this, however long it takes -- that is the request timeout.
    ///
    /// The packaged binary measures this in whole seconds (libcurl's
    /// low-speed limit), so a value is rounded UP to the next second there.
    public synchronized WebClient setReadTimeoutMillis(int millis) {
        checkUnused();
        this.readTimeoutMillis = nonNegative(millis);
        return this;
    }

    public synchronized int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    /// The ceiling on a whole request, connection to last byte. Default 0, no
    /// ceiling. A request still running when it is reached fails with an
    /// IOException.
    public synchronized WebClient setRequestTimeoutMillis(int millis) {
        checkUnused();
        this.requestTimeoutMillis = nonNegative(millis);
        return this;
    }

    public synchronized int getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    /// Whether a finished request's connection is kept for the next request to
    /// the same server. Default true. With false every request opens, and closes,
    /// a connection of its own.
    public synchronized WebClient setReuseConnections(boolean reuseConnections) {
        checkUnused();
        this.reuseConnections = reuseConnections;
        return this;
    }

    public synchronized boolean isReuseConnections() {
        return reuseConnections;
    }

    public Web.Result get(String url) throws IOException {
        return request("GET", url, null, null);
    }

    public Web.Result getJson(String url, String bearerToken) throws IOException {
        List headers = new ArrayList();
        headers.add("Accept: application/json");
        if (bearerToken != null) {
            headers.add("Authorization: Bearer " + bearerToken);
        }
        return request("GET", url, headers, null);
    }

    public Web.Result postJson(String url, String json, String bearerToken) throws IOException {
        List headers = new ArrayList();
        headers.add("Content-Type: application/json");
        headers.add("Accept: application/json");
        if (bearerToken != null) {
            headers.add("Authorization: Bearer " + bearerToken);
        }
        return request("POST", url, headers, json == null ? new byte[0] : json.getBytes("UTF-8"));
    }

    /// As [Web#request(String, String, List, byte[])], through this client.
    public Web.Result request(String method, String url, List headers, byte[] body)
            throws IOException {
        synchronized (this) {
            if (closed) {
                throw new IOException("This WebClient has been closed");
            }
            used = true;
            // Registered under the monitor close() takes, so a close that
            // lands before Web reaches transport() waits for this request
            // instead of pulling the transport out from under it.
            inFlight++;
        }
        try {
            return Web.request(this, method, url, headers, body);
        } finally {
            exchangeEnded();
        }
    }

    /// As [Web#open(String, String, List, long)], through this client: its
    /// certificate, pool and connect and read timeouts. The exchange counts as a
    /// request in flight until it is closed, so [#close] waits for it.
    public Web.Exchange open(String method, String url, List headers, long requestLength)
            throws IOException {
        synchronized (this) {
            if (closed) {
                throw new IOException("This WebClient has been closed");
            }
            used = true;
            inFlight++;
        }
        boolean opened = false;
        try {
            Web.Exchange exchange = Web.open(this, method, url, headers, requestLength);
            opened = true;
            return exchange;
        } finally {
            if (!opened) {
                exchangeEnded();
            }
        }
    }

    /// One request or exchange is over; the last one out after close() releases
    /// the transport.
    void exchangeEnded() {
        Object released = null;
        synchronized (this) {
            inFlight--;
            if (closed && inFlight == 0) {
                released = transport;
                transport = null;
            }
        }
        if (released != null) {
            Web.closeTransport(released);
        }
    }

    /// Releases the client's pooled connections. A request made after this
    /// fails; one already running finishes first, and the last of them
    /// releases the connections.
    public void close() {
        Object released = null;
        synchronized (this) {
            closed = true;
            if (inFlight == 0) {
                released = transport;
                transport = null;
            }
        }
        if (released != null) {
            Web.closeTransport(released);
        }
    }

    /// The transport Web keeps for this client, created by `factory` the first
    /// time. Only called by a request registered in request(), which close()
    /// waits for, so it is never asked for one after the release.
    synchronized Object transport(Web.TransportFactory factory) throws IOException {
        if (transport == null) {
            transport = factory.create(this);
        }
        return transport;
    }

    // Every setter and getter is synchronized, as checkUnused() and the `used`
    // transition in request() are: a setter checked outside the monitor could
    // pass, let the first request build its transport from the old value, and
    // then assign -- leaving a client that reports one certificate and
    // presents another. Callers hold the monitor only for a field access.
    private synchronized void checkUnused() {
        if (used) {
            throw new IllegalStateException("A WebClient's configuration is fixed by its "
                    + "first request; create another client for a different one");
        }
    }

    private static int nonNegative(int millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("A timeout cannot be negative: " + millis);
        }
        return millis;
    }
}
