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
package com.codename1.flutter.plugins.dio;

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
import dart.convert.Convert;
import dart.core.DartMap;
import dart.io.SocketException;
import dart.typed_data.Uint8List;

/// dio's `Dio`, over Codename One's `ConnectionRequest`.
///
/// A request runs as dio runs it: the request interceptors in order, the HTTP
/// exchange, a status check (`validateStatus`, 2xx by default), then the response
/// interceptors -- or, on any failure, the error interceptors -- and the future
/// completes with what comes out of the last of them. A JSON response (by content
/// type, for the default `ResponseType.json`) is decoded into Dart maps and lists.
/// With no Display (headless) there is no network, and every request fails with a
/// `connectionError`, so an app's offline path is what runs.
public class Dio {

    private BaseOptions options;
    private final Interceptors interceptors = new Interceptors();

    public Dio() {
        this(null);
    }

    public Dio(BaseOptions options) {
        this.options = options == null ? new BaseOptions() : options;
    }

    public BaseOptions options() {
        return options;
    }

    public void options(BaseOptions v) {
        options = v == null ? new BaseOptions() : v;
    }

    public Interceptors interceptors() {
        return interceptors;
    }

    public void close() {
    }

    public void close(Boolean force) {
    }

    public <T> Future<Response<T>> get(String path, Object data, Map<String, Object> queryParameters,
            Options options, CancelToken cancelToken, Object onReceiveProgress) {
        return send("GET", path, data, queryParameters, options, cancelToken);
    }

    public <T> Future<Response<T>> post(String path, Object data, Map<String, Object> queryParameters,
            Options options, CancelToken cancelToken, Object onSendProgress, Object onReceiveProgress) {
        return send("POST", path, data, queryParameters, options, cancelToken);
    }

    public <T> Future<Response<T>> put(String path, Object data, Map<String, Object> queryParameters,
            Options options, CancelToken cancelToken, Object onSendProgress, Object onReceiveProgress) {
        return send("PUT", path, data, queryParameters, options, cancelToken);
    }

    public <T> Future<Response<T>> patch(String path, Object data, Map<String, Object> queryParameters,
            Options options, CancelToken cancelToken, Object onSendProgress, Object onReceiveProgress) {
        return send("PATCH", path, data, queryParameters, options, cancelToken);
    }

    public <T> Future<Response<T>> delete(String path, Object data, Map<String, Object> queryParameters,
            Options options, CancelToken cancelToken) {
        return send("DELETE", path, data, queryParameters, options, cancelToken);
    }

    public <T> Future<Response<T>> head(String path, Object data, Map<String, Object> queryParameters,
            Options options, CancelToken cancelToken) {
        return send("HEAD", path, data, queryParameters, options, cancelToken);
    }

    public <T> Future<Response<T>> request(String path, Object data, Map<String, Object> queryParameters,
            CancelToken cancelToken, Options options, Object onSendProgress, Object onReceiveProgress) {
        String method = options != null && options.method != null ? options.method : null;
        return send(method, path, data, queryParameters, options, cancelToken);
    }

    /// `dio.fetch(requestOptions)`: re-runs a fully built request -- the usual way
    /// an error interceptor retries after refreshing a token.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Future<Response<T>> fetch(RequestOptions ro) {
        Call call = new Call(ro);
        call.request(0, ro);
        return (Future) call.done.future();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Future<Response<T>> send(String method, String path, Object data,
            Map<String, Object> query, Options opts, CancelToken cancelToken) {
        RequestOptions ro = compose(method, path, data, query, opts);
        ro.cancelToken = cancelToken;
        return fetch(ro);
    }

    private RequestOptions compose(String method, String path, Object data, Map<String, Object> query,
            Options opts) {
        BaseOptions b = options;
        RequestOptions ro = new RequestOptions();
        ro.path(path);
        ro.baseUrl(b.baseUrl);
        ro.data = data;
        ro.connectTimeout = b.connectTimeout;
        DartMap<String, Object> q = new DartMap<String, Object>();
        if (b.queryParameters != null) {
            q.putAll(b.queryParameters);
        }
        if (query != null) {
            q.putAll(query);
        }
        ro.queryParameters = q;
        DartMap<String, Object> h = new DartMap<String, Object>();
        if (b.headers != null) {
            h.putAll(b.headers);
        }
        DartMap<String, Object> x = new DartMap<String, Object>();
        if (b.extra != null) {
            x.putAll(b.extra);
        }
        ro.method = pick(method, opts == null ? null : opts.method, b.method, "GET");
        ro.contentType = b.contentType;
        ro.responseType = b.responseType;
        ro.sendTimeout = b.sendTimeout;
        ro.receiveTimeout = b.receiveTimeout;
        ro.validateStatus = b.validateStatus;
        ro.followRedirects = b.followRedirects;
        if (opts != null) {
            if (opts.headers != null) {
                h.putAll(opts.headers);
            }
            if (opts.extra != null) {
                x.putAll(opts.extra);
            }
            if (opts.contentType != null) {
                ro.contentType = opts.contentType;
            }
            if (opts.responseType != null) {
                ro.responseType = opts.responseType;
            }
            if (opts.sendTimeout != null) {
                ro.sendTimeout = opts.sendTimeout;
            }
            if (opts.receiveTimeout != null) {
                ro.receiveTimeout = opts.receiveTimeout;
            }
            if (opts.validateStatus != null) {
                ro.validateStatus = opts.validateStatus;
            }
            if (opts.followRedirects != null) {
                ro.followRedirects = opts.followRedirects;
            }
        }
        ro.headers = h;
        ro.extra = x;
        if (ro.responseType == null) {
            ro.responseType = ResponseType.json;
        }
        return ro;
    }

    private static String pick(String a, String b, String c, String d) {
        return a != null ? a : b != null ? b : c != null ? c : d;
    }

    /// One request travelling through the interceptors.
    final class Call {
        final Completer<Response<Object>> done = new Completer<Response<Object>>();
        private final RequestOptions original;

        Call(RequestOptions original) {
            this.original = original;
        }

        void request(int index, RequestOptions ro) {
            if (done.isCompleted()) {
                return;
            }
            if (index < interceptors.size()) {
                try {
                    interceptors.get(index).onRequest(ro, new RequestInterceptorHandler(this, index));
                } catch (RuntimeException e) {
                    error(0, wrap(ro, e));
                }
                return;
            }
            transmit(ro, this);
        }

        void response(int index, Response<Object> r) {
            if (done.isCompleted()) {
                return;
            }
            if (index < interceptors.size()) {
                try {
                    interceptors.get(index).onResponse(r, new ResponseInterceptorHandler(this, index));
                } catch (RuntimeException e) {
                    error(0, wrap(r.requestOptions, e));
                }
                return;
            }
            succeed(r);
        }

        void error(int index, DioException e) {
            if (done.isCompleted()) {
                return;
            }
            if (index < interceptors.size()) {
                try {
                    interceptors.get(index).onError(e, new ErrorInterceptorHandler(this, index));
                } catch (RuntimeException ex) {
                    fail(wrap(e.requestOptions, ex));
                }
                return;
            }
            fail(e);
        }

        void succeed(Response<Object> r) {
            if (!done.isCompleted()) {
                done.complete(r);
            }
        }

        void fail(DioException e) {
            if (!done.isCompleted()) {
                done.completeError(e);
            }
        }

        private DioException wrap(RequestOptions ro, RuntimeException e) {
            if (e instanceof DioException) {
                return (DioException) e;
            }
            return DioException.of(ro != null ? ro : original, DioExceptionType.unknown, e.getMessage(), e, null);
        }
    }

    private static boolean valid(RequestOptions ro, long status) {
        if (ro.validateStatus != null) {
            return Boolean.TRUE.equals(ro.validateStatus.call(Long.valueOf(status)));
        }
        return status >= 200 && status < 300;
    }

    private void transmit(final RequestOptions ro, final Call call) {
        final CancelToken token = ro.cancelToken;
        if (token != null && token.isCancelled()) {
            call.error(0, DioException.of(ro, DioExceptionType.cancel, "The request was cancelled.",
                    token.cancelError(), null));
            return;
        }
        if (!Display.isInitialized()) {
            call.error(0, DioException.of(ro, DioExceptionType.connectionError,
                    "The connection errored: no network, Codename One is not initialized",
                    new SocketException("No network: Codename One is not initialized"), null));
            return;
        }
        final String[] contentType = {headerValue(ro, "content-type")};
        if (contentType[0] == null) {
            contentType[0] = ro.contentType;
        }
        final byte[] payload = encodeBody(ro, contentType);
        final Headers responseHeaders = new Headers();
        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        ConnectionRequest req = new ConnectionRequest() {
            private Exception failure;

            @Override
            protected void readHeaders(Object connection) throws IOException {
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
            protected void buildRequestBody(OutputStream os) throws IOException {
                if (payload != null) {
                    os.write(payload);
                }
            }

            @Override
            protected void readResponse(InputStream input) throws IOException {
                byte[] buf = new byte[8192];
                int n;
                while ((n = input.read(buf)) > 0) {
                    received.write(buf, 0, n);
                }
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // The status reaches validateStatus through the response, as in dio.
            }

            @Override
            protected void handleException(Exception err) {
                failure = err;
            }

            @Override
            protected void postResponse() {
                if (failure != null) {
                    String m = String.valueOf(failure.getMessage());
                    boolean timeout = failure.getClass().getName().indexOf("Timeout") >= 0
                            || Headers.fold(m).indexOf("timed out") >= 0;
                    call.error(0, DioException.of(ro, timeout ? DioExceptionType.connectionTimeout
                            : DioExceptionType.connectionError, m, new SocketException(m), null));
                    return;
                }
                Response<Object> r = new Response<Object>();
                r.requestOptions = ro;
                r.statusCode = Long.valueOf(getResponseCode());
                r.statusMessage = getResponseErrorMessage();
                r.headers = responseHeaders;
                try {
                    r.data = decode(ro, responseHeaders, received.toByteArray());
                } catch (RuntimeException e) {
                    call.error(0, DioException.of(ro, DioExceptionType.unknown, e.getMessage(), e, r));
                    return;
                }
                if (!valid(ro, getResponseCode())) {
                    call.error(0, DioException.of(ro, DioExceptionType.badResponse,
                            "This exception was thrown because the response has a status code of "
                                    + getResponseCode() + " and RequestOptions.validateStatus was configured "
                                    + "to throw for this status code.", null, r));
                    return;
                }
                call.response(0, r);
            }
        };
        req.setUrl(ro.url());
        req.setHttpMethod(ro.method);
        req.setPost(payload != null);
        req.setFailSilently(true);
        req.setReadResponseForErrors(true);
        req.setDuplicateSupported(true);
        if (ro.followRedirects != null) {
            req.setFollowRedirects(ro.followRedirects.booleanValue());
        }
        if (ro.connectTimeout != null) {
            req.setTimeout((int) ro.connectTimeout.inMilliseconds());
        }
        if (ro.receiveTimeout != null) {
            req.setReadTimeout((int) ro.receiveTimeout.inMilliseconds());
        }
        for (Map.Entry<String, Object> e : ro.headers().entrySet()) {
            if (e.getValue() != null && !"content-type".equals(Headers.fold(e.getKey()))) {
                req.addRequestHeader(e.getKey(), String.valueOf(e.getValue()));
            }
        }
        if (contentType[0] != null && payload != null) {
            req.setContentType(contentType[0]);
        }
        if (token != null) {
            token.attach(req, new Runnable() {
                @Override
                public void run() {
                    call.error(0, DioException.of(ro, DioExceptionType.cancel, "The request was cancelled.",
                            token.cancelError(), null));
                }
            });
        }
        NetworkManager.getInstance().addToQueue(req);
    }

    private static String headerValue(RequestOptions ro, String folded) {
        for (Map.Entry<String, Object> e : ro.headers().entrySet()) {
            if (folded.equals(Headers.fold(e.getKey())) && e.getValue() != null) {
                return String.valueOf(e.getValue());
            }
        }
        return null;
    }

    /// The request body: FormData as multipart, a String as is, bytes raw, and a
    /// map or list as JSON -- or as a form when the content type says so.
    private static byte[] encodeBody(RequestOptions ro, String[] contentType) {
        Object data = ro.data;
        if (data == null) {
            return null;
        }
        try {
            if (data instanceof FormData) {
                contentType[0] = ((FormData) data).contentType();
                return ((FormData) data).encode();
            }
            if (data instanceof Uint8List) {
                return ((Uint8List) data).toBytes();
            }
            if (data instanceof String) {
                if (contentType[0] == null) {
                    contentType[0] = Headers.textPlainContentType;
                }
                return ((String) data).getBytes("UTF-8");
            }
            if (data instanceof Map && contentType[0] != null
                    && contentType[0].startsWith(Headers.formUrlEncodedContentType)) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<?, ?> e : ((Map<?, ?>) data).entrySet()) {
                    if (sb.length() > 0) {
                        sb.append('&');
                    }
                    sb.append(com.codename1.io.Util.encodeUrl(String.valueOf(e.getKey()))).append('=')
                            .append(com.codename1.io.Util.encodeUrl(dart.runtime.DartRuntime.str(e.getValue())));
                }
                return sb.toString().getBytes("UTF-8");
            }
            if (data instanceof List && contentType[0] == null && isByteList((List<?>) data)) {
                List<?> l = (List<?>) data;
                byte[] out = new byte[l.size()];
                for (int i = 0; i < out.length; i++) {
                    out[i] = (byte) ((Number) l.get(i)).intValue();
                }
                return out;
            }
            if (contentType[0] == null) {
                contentType[0] = Headers.jsonContentType;
            }
            return Convert.jsonEncode(data).getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private static boolean isByteList(List<?> l) {
        if (l.isEmpty()) {
            return false;
        }
        for (Object o : l) {
            if (!(o instanceof Long) && !(o instanceof Integer)) {
                return false;
            }
        }
        return true;
    }

    private static Object decode(RequestOptions ro, Headers headers, byte[] body) {
        ResponseType type = ro.responseType == null ? ResponseType.json : ro.responseType;
        if (type == ResponseType.bytes || type == ResponseType.stream) {
            return Uint8List.fromBytes(body);
        }
        String text;
        try {
            text = new String(body, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            text = new String(body);
        }
        if (type == ResponseType.json) {
            String ct = headers.value("content-type");
            boolean json = ct != null && Headers.fold(ct).indexOf("json") >= 0;
            if (json) {
                return text.trim().isEmpty() ? null : Convert.jsonDecode(text);
            }
        }
        return text;
    }

}
