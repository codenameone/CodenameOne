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
package dart.io;

import dart.async.Future;
import dart.core.DartUri;
import dart.core.Duration;

/**
 * Dart's {@code HttpClient}, over Codename One's {@code ConnectionRequest}. A request is
 * sent when the app closes it ({@code await request.close()}), and the response body
 * arrives whole, as one chunk of its stream -- what {@code package:http} and dio's
 * {@code IOHttpClientAdapter} read it as.
 */
public class HttpClient {

    private Duration connectionTimeout;
    private Duration idleTimeout = Duration.of(0, 0, 0, 15, 0, 0);
    private String userAgent = "Dart/3.0 (dart:io)";
    private boolean autoUncompress = true;
    private boolean closed;
    private Object badCertificateCallback;

    public HttpClient() {
    }

    /** {@code HttpClient(context: ...)}: the security context is the platform's. */
    public HttpClient(Object context) {
    }

    public Duration connectionTimeout() {
        return connectionTimeout;
    }

    public void connectionTimeout(Duration d) {
        connectionTimeout = d;
    }

    public Duration idleTimeout() {
        return idleTimeout;
    }

    public void idleTimeout(Duration d) {
        idleTimeout = d;
    }

    public String userAgent() {
        return userAgent;
    }

    public void userAgent(String ua) {
        userAgent = ua;
    }

    public boolean autoUncompress() {
        return autoUncompress;
    }

    public void autoUncompress(boolean v) {
        autoUncompress = v;
    }

    /** Accepted and kept; the platform's own certificate checks still apply. */
    public void badCertificateCallback(Object callback) {
        badCertificateCallback = callback;
    }

    public Object badCertificateCallback() {
        return badCertificateCallback;
    }

    public void maxConnectionsPerHost(Long n) {
    }

    public void findProxy(Object f) {
    }

    public Future<HttpClientRequest> openUrl(String method, DartUri url) {
        if (closed) {
            return Future.error(new StateErrorClosed());
        }
        HttpClientRequest r = new HttpClientRequest(this, method, url);
        if (userAgent != null) {
            r.headers().set(HttpHeaders.userAgentHeader, userAgent);
        }
        if (autoUncompress) {
            r.headers().set(HttpHeaders.acceptEncodingHeader, "gzip");
        }
        return Future.value(r);
    }

    public Future<HttpClientRequest> getUrl(DartUri url) {
        return openUrl("GET", url);
    }

    public Future<HttpClientRequest> postUrl(DartUri url) {
        return openUrl("POST", url);
    }

    public Future<HttpClientRequest> putUrl(DartUri url) {
        return openUrl("PUT", url);
    }

    public Future<HttpClientRequest> patchUrl(DartUri url) {
        return openUrl("PATCH", url);
    }

    public Future<HttpClientRequest> deleteUrl(DartUri url) {
        return openUrl("DELETE", url);
    }

    public Future<HttpClientRequest> headUrl(DartUri url) {
        return openUrl("HEAD", url);
    }

    public Future<HttpClientRequest> open(String method, String host, long port, String path) {
        return openUrl(method, DartUri.parse("http://" + host + ":" + port + path));
    }

    public Future<HttpClientRequest> get(String host, long port, String path) {
        return open("GET", host, port, path);
    }

    public Future<HttpClientRequest> post(String host, long port, String path) {
        return open("POST", host, port, path);
    }

    public void close() {
        close(null);
    }

    public void close(Boolean force) {
        closed = true;
    }

    /** Dart's StateError for a request on a closed client. */
    private static final class StateErrorClosed extends dart.core.StateError {
        private static final long serialVersionUID = 1L;

        StateErrorClosed() {
            super("Client is closed");
        }
    }
}
