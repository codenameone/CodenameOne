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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Display;
import dart.async.Completer;
import dart.async.Future;
import dart.convert.ByteCodec;
import dart.core.DartUri;
import dart.core.Duration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/** Dart's {@code HttpClientRequest}: headers and body collected until {@link #close}. */
public class HttpClientRequest {

    private final HttpClient client;
    private final String method;
    private final DartUri uri;
    private final HttpHeaders headers = new HttpHeaders();
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private boolean followRedirects = true;
    private long maxRedirects = 5;
    private long contentLength = -1;
    private Object encoding;
    private Completer<HttpClientResponse> done;
    /// The request in flight once close() queued it, so abort() can kill it.
    private ConnectionRequest sent;

    HttpClientRequest(HttpClient client, String method, DartUri uri) {
        this.client = client;
        this.method = method;
        this.uri = uri;
    }

    public String method() {
        return method;
    }

    public DartUri uri() {
        return uri;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public boolean followRedirects() {
        return followRedirects;
    }

    public void followRedirects(boolean v) {
        followRedirects = v;
    }

    public long maxRedirects() {
        return maxRedirects;
    }

    public void maxRedirects(long v) {
        maxRedirects = v;
    }

    public long contentLength() {
        return contentLength;
    }

    public void contentLength(long v) {
        contentLength = v;
    }

    public boolean persistentConnection() {
        return true;
    }

    public void persistentConnection(boolean v) {
    }

    public Object encoding() {
        return encoding == null ? dart.convert.Convert.utf8 : encoding;
    }

    public void encoding(Object e) {
        encoding = e;
    }

    /** Dart's {@code add(List<int>)}. */
    public void add(Object bytes) {
        byte[] b = ByteCodec.bytesOf(bytes);
        body.write(b, 0, b.length);
    }

    /** Dart's {@code write(Object)}: the value's text, in the request's encoding. */
    public void write(Object obj) {
        Object enc = encoding();
        String s = dart.runtime.DartRuntime.str(obj);
        add(enc instanceof ByteCodec ? ((ByteCodec) enc).encode(s) : dart.convert.Convert.utf8.encode(s));
    }

    public void writeln(Object obj) {
        write(obj);
        write("\n");
    }

    public void writeln() {
        write("\n");
    }

    /** Dart's {@code addStream}: every chunk is appended. */
    public Future<Object> addStream(dart.async.Stream<Object> stream) {
        return stream.forEach(new dart.runtime.Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object chunk) {
                add(chunk);
            }
        });
    }

    public Future<Object> flush() {
        return Future.value(null);
    }

    public void abort() {
        abort(null, null);
    }

    public void abort(Object exception, Object stackTrace) {
        // Terminal either way: before close() it settles `done` so close() never sends;
        // after, it also kills the queued request so it neither runs on nor mutates the
        // server behind a cancelled call.
        if (done == null) {
            done = new Completer<HttpClientResponse>();
        }
        if (!done.isCompleted()) {
            done.completeError(exception != null ? exception : new HttpException("Request aborted", uri));
        }
        if (sent != null) {
            sent.kill();
            sent = null;
        }
    }

    public Future<HttpClientResponse> done() {
        return done == null ? close() : done.future();
    }

    /** Sends the request; the future completes with the response, or a SocketException. */
    public Future<HttpClientResponse> close() {
        if (done != null) {
            return done.future();
        }
        done = new Completer<HttpClientResponse>();
        if (!Display.isInitialized()) {
            done.completeError(new SocketException("No network: Codename One is not initialized"));
            return done.future();
        }
        final byte[] payload = body.toByteArray();
        final Completer<HttpClientResponse> result = done;
        final HttpHeaders responseHeaders = new HttpHeaders();
        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        ConnectionRequest req = new ConnectionRequest() {
            private Exception failure;
            private int redirects;
            private boolean tooManyRedirects;

            /// maxRedirects, counted per hop. Past the limit Dart fails the request
            /// with "Redirect limit exceeded" rather than following on, and so does
            /// this; returning true tells ConnectionRequest not to follow. (iOS
            /// follows redirects natively and never calls this.)
            @Override
            public boolean onRedirect(String url) {
                redirects++;
                if (redirects > maxRedirects) {
                    tooManyRedirects = true;
                    return true;
                }
                return false;
            }

            @Override
            protected void readHeaders(Object connection) throws java.io.IOException {
                String[] names = getHeaderFieldNames(connection);
                if (names != null) {
                    for (String n : names) {
                        if (n == null) {
                            continue;
                        }
                        String[] vs = getHeaders(connection, n);
                        if (vs != null) {
                            for (String v : vs) {
                                responseHeaders.add(n, v);
                            }
                        }
                    }
                }
            }

            @Override
            protected void buildRequestBody(OutputStream os) throws java.io.IOException {
                os.write(payload);
            }

            @Override
            protected void readResponse(InputStream input) throws java.io.IOException {
                byte[] buf = new byte[8192];
                int n;
                while ((n = input.read(buf)) > 0) {
                    received.write(buf, 0, n);
                }
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // The status reaches the app through the response, as in dart:io.
            }

            @Override
            protected void handleException(Exception err) {
                failure = err;
            }

            @Override
            protected void postResponse() {
                finish();
            }

            private void finish() {
                if (result.isCompleted()) {
                    return;
                }
                if (tooManyRedirects) {
                    result.completeError(new HttpException("Redirect limit exceeded"));
                    return;
                }
                if (failure != null) {
                    result.completeError(new SocketException(String.valueOf(failure.getMessage())));
                    return;
                }
                result.complete(new HttpClientResponse(getResponseCode(), getResponseErrorMessage(),
                        responseHeaders, received.toByteArray()));
            }
        };
        req.setUrl(uri.toString());
        req.setHttpMethod(method);
        boolean hasBody = payload.length > 0;
        req.setPost(hasBody);
        req.setFailSilently(true);
        req.setReadResponseForErrors(true);
        req.setFollowRedirects(followRedirects);
        req.setDuplicateSupported(true);
        Duration timeout = client.connectionTimeout();
        if (timeout != null) {
            req.setTimeout((int) timeout.inMilliseconds());
        }
        for (Map.Entry<String, List<String>> e : headers.all().entrySet()) {
            StringBuilder v = new StringBuilder();
            for (String s : e.getValue()) {
                if (v.length() > 0) {
                    v.append(", ");
                }
                v.append(s);
            }
            req.addRequestHeader(e.getKey(), v.toString());
        }
        if (hasBody && headers.value(HttpHeaders.contentTypeHeader) != null) {
            req.setContentType(headers.value(HttpHeaders.contentTypeHeader));
        }
        sent = req;
        NetworkManager.getInstance().addToQueue(req);
        return done.future();
    }
}
