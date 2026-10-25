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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Outbound HTTP and HTTPS for server-side binaries. Backed by libcurl, so TLS
/// verification, redirects, chunked decoding and the system certificate store all
/// come from a library that is maintained for the purpose.
///
/// Distinct from [Http], which is a raw-socket plaintext client for the host
/// runtime's loopback control protocol. Use this one for anything real.
///
/// **Certificate store.** A dynamically linked build finds the system CA bundle.
/// A fully static build has whatever the image provides, which for a `scratch`
/// container is nothing - and TLS then fails with "unable to get local issuer
/// certificate". Ship a `ca-certificates.crt` and point curl at it with the
/// `CURL_CA_BUNDLE` or `SSL_CERT_FILE` environment variable (the first one set
/// wins). The native client reads them and hands the file to libcurl, which
/// would not read either on its own.
public final class Web {

    /// ASCII lower case, because String.toLowerCase() is LOCALE SENSITIVE and this
    /// platform has no Locale to ask for the root one. On a device set to Turkish
    /// the I of an ASCII token folds to a dotless i, so a header stored under one
    /// spelling is looked up under another and getHeader answers null: nothing is
    /// thrown, nothing is logged, and the caller reads a header that is there as
    /// absent. A header name is ASCII by specification. Copied rather than shared;
    /// see CLAUDE.md. Both arms of Web carry it, because both index headers.
    private static String asciiLower(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }

    private Web() {
    }

    /// An outbound response: status, body, and libcurl's message when it failed.
    public static final class Result {
        private final int status;
        private final byte[] body;
        private final String error;
        private final Map headers;
        private final Map values;
        private final String protocol;

        /// @param values name (lower case) to the List of every value the response
        /// carried for it, in arrival order
        Result(int status, byte[] body, String error, Map values) {
            this(status, body, error, values, null);
        }

        Result(int status, byte[] body, String error, Map values, String protocol) {
            this.protocol = protocol;
            this.status = status;
            this.body = body;
            this.error = error;
            this.values = values == null ? new LinkedHashMap() : values;
            // The single-valued view getHeaders() has always returned, kept so a
            // caller that iterates it still gets a Map of Strings.
            this.headers = new LinkedHashMap();
            java.util.Iterator it = this.values.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List all = (List) entry.getValue();
                if (all != null && !all.isEmpty()) {
                    this.headers.put(entry.getKey(), all.get(0));
                }
            }
        }

        /// The HTTP status, or -1 when the transfer itself failed.
        public int getStatus() {
            return status;
        }

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }

        public byte[] getBody() {
            return body;
        }

        public String getBodyAsString() {
            if (body == null) {
                return null;
            }
            try {
                return new String(body, "UTF-8");
            } catch (IOException err) {
                return new String(body);
            }
        }

        /// The protocol the response came over -- "HTTP/2", "HTTP/1.1" or
        /// "HTTP/1.0" -- or null when the runtime could not tell.
        public String getProtocol() {
            return protocol;
        }

        /// Non-null only when the transfer failed before producing a status.
        public String getError() {
            return error;
        }

        /// The response headers, lower-cased names to values.
        ///
        /// A response's headers are half of what an API says -- the ETag S3 returns for
        /// a PUT, the content type of an object, the rate-limit budget a service
        /// publishes -- and a client that can only read the body cannot see any of it.
        /// Names are lower-cased because HTTP header names are case insensitive and a
        /// caller should not have to guess which case this server chose.
        public Map getHeaders() {
            return headers;
        }

        /// The FIRST value for a name, matched case-insensitively. Null when absent.
        ///
        /// First rather than last, which is what this used to answer -- and the two
        /// differ only for a field the response repeated, which is the case that was
        /// losing data. Use getHeaderValues for a field that is legitimately repeated:
        /// Set-Cookie is the one every response has, and its values cannot be joined
        /// back together because an Expires attribute contains a comma of its own.
        public String getHeader(String name) {
            List all = getHeaderValues(name);
            return all.isEmpty() ? null : (String) all.get(0);
        }

        /// Every value the response carried for a name, in arrival order.
        ///
        /// Empty, never null, when the response had none. A repeated field used to
        /// collapse to one value here, so a response setting three cookies delivered
        /// one and nothing said the others had been dropped.
        public List getHeaderValues(String name) {
            if (name == null) {
                return new ArrayList();
            }
            List all = (List) values.get(asciiLower(name));
            return all == null ? new ArrayList() : all;
        }
    }

    public static Result get(String url) throws IOException {
        return request("GET", url, null, null);
    }

    public static Result getJson(String url, String bearerToken) throws IOException {
        List headers = new ArrayList();
        headers.add("Accept: application/json");
        if (bearerToken != null) {
            headers.add("Authorization: Bearer " + bearerToken);
        }
        return request("GET", url, headers, null);
    }

    public static Result postJson(String url, String json, String bearerToken) throws IOException {
        List headers = new ArrayList();
        headers.add("Content-Type: application/json");
        headers.add("Accept: application/json");
        if (bearerToken != null) {
            headers.add("Authorization: Bearer " + bearerToken);
        }
        byte[] payload;
        try {
            payload = json == null ? new byte[0] : json.getBytes("UTF-8");
        } catch (IOException err) {
            throw new IOException("Could not encode the request body", err);
        }
        return request("POST", url, headers, payload);
    }

    /// - `headers`: a list of "Name: value" strings, or null
    ///
    /// Every default and no connection pool; a [WebClient] is the way to set a
    /// client certificate, require HTTP/2, change a timeout or reuse connections.
    public static Result request(String method, String url, List headers, byte[] body) throws IOException {
        return request(null, method, url, headers, body);
    }

    /// Creates the per-client transport the first time a WebClient is used.
    interface TransportFactory {
        Object create(WebClient client) throws IOException;
    }

    /// A WebClient's native connection pool. The pool itself is freed only when
    /// the client is closed AND no request is still inside it, so close() from
    /// one handler cannot pull the pool out from under a request on another.
    private static final class Pool {
        private long handle;
        private int active;
        private boolean closed;

        synchronized long enter() throws IOException {
            if (closed || handle == 0) {
                throw new IOException("This WebClient has been closed");
            }
            active++;
            return handle;
        }

        void exit() {
            long release = 0;
            synchronized (this) {
                active--;
                if (closed && active == 0) {
                    release = handle;
                    handle = 0;
                }
            }
            if (release != 0) {
                poolFreeImpl(release);
            }
        }

        void close() {
            long release = 0;
            synchronized (this) {
                closed = true;
                if (active == 0) {
                    release = handle;
                    handle = 0;
                }
            }
            if (release != 0) {
                poolFreeImpl(release);
            }
        }
    }

    private static final TransportFactory POOLS = new TransportFactory() {
        @Override
        public Object create(WebClient client) throws IOException {
            initialiseCurlOnce();
            Pool pool = new Pool();
            pool.handle = poolCreateImpl();
            if (pool.handle == 0) {
                throw new IOException("Could not allocate the WebClient's connection pool");
            }
            return pool;
        }
    };

    static void closeTransport(Object transport) {
        if (transport instanceof Pool) {
            ((Pool) transport).close();
        }
    }

    static Result request(WebClient client, String method, String url, List headers,
                          byte[] body) throws IOException {
        Urls.requireHttp(url);
        // The request line's other field; see HeaderLines.requireMethod.
        HeaderLines.requireMethod(method);
        HeaderLines.validate(headers);
        // Null unless a tracer is installed; see Tracing.startHttpClient.
        Span span = Tracing.startHttpClient(method, url, headers);
        int status = -1;
        Throwable failure = null;
        try {
            Result result = perform(client, method, url, headers, body,
                    Tracing.propagationHeaders(span, headers));
            status = result.getStatus();
            return result;
        } catch (IOException err) {
            failure = err;
            throw err;
        } catch (RuntimeException err) {
            failure = err;
            throw err;
        } finally {
            Tracing.endHttpClient(span, status, failure);
        }
    }

    /// @param trace the trace-context lines, or null. Sent like any other header,
    /// but NOT counted as the caller's: whether a redirect is followed
    /// freely depends on whether the caller handed over something that
    /// could leak (see CURLOPT_FOLLOWLOCATION in cn1_backend_web.c),
    /// and a trace id is not that. Counting it would have silently
    /// stopped every plain GET following redirects the moment tracing
    /// was turned on.
    private static Result perform(WebClient client, String method, String url, List headers,
                                  byte[] body, List trace) throws IOException {
        StringBuilder joined = new StringBuilder();
        boolean callerHeaders = false;
        if (headers != null) {
            for (int iter = 0 ; iter < headers.size() ; iter++) {
                if (iter > 0) {
                    joined.append('\n');
                }
                String line = String.valueOf(headers.get(iter));
                // What the native side would turn into a list entry: a line with
                // something in it. An empty one never reached libcurl before either.
                if (line.length() > 0) {
                    callerHeaders = true;
                }
                joined.append(line);
            }
        }
        if (trace != null) {
            for (Object line : trace) {
                if (joined.length() > 0) {
                    joined.append('\n');
                }
                joined.append(String.valueOf(line));
            }
        }
        initialiseCurlOnce();
        Pool pool = null;
        long poolHandle = 0;
        if (client != null && client.isReuseConnections()) {
            pool = (Pool) client.transport(POOLS);
            poolHandle = pool.enter();
        }
        long handle;
        try {
            handle = performClient(client, poolHandle, method, url,
                    HeaderLines.narrowed(joined.toString()), body, callerHeaders);
        } finally {
            if (pool != null) {
                pool.exit();
            }
        }
        if (handle == 0) {
            // REDACTED, because this message goes wherever the caller logs it and
            // the URL may be a presigned one whose signature is the credential.
            throw new IOException("Could not start a request to "
                    + Urls.forMessage(url));
        }
        try {
            int status = statusImpl(handle);
            String error = errorImpl(handle);
            if (status < 0) {
                throw new IOException("Request to " + Urls.forMessage(url) + " failed: "
                        + (error == null ? "unknown error" : error));
            }
            return new Result(status, bodyImpl(handle), error,
                    parseHeaders(headersImpl(handle)), protocolName(protocolImpl(handle)));
        } finally {
            freeImpl(handle);
        }
    }

    /// Bit 0 of performImpl's flags: HTTP/2 or nothing.
    private static final int FLAG_REQUIRE_HTTP2 = 1;
    /// Bit 1: a WebClient, which offers HTTP/2 over TLS even where this libcurl's
    /// own default would not, and takes the timeouts it was given.
    private static final int FLAG_CLIENT = 2;
    /// Bit 2: never keep or reuse a connection.
    private static final int FLAG_NO_REUSE = 4;
    /// Bit 3: a streamed exchange, stepped from Java; see Exchange.
    private static final int FLAG_STREAM = 8;

    private static final int WANT_HEADERS = 1;
    private static final int WANT_DATA = 2;
    private static final int WANT_SPACE = 3;

    /// The request has no body.
    public static final long NO_BODY = -2;
    /// The request has a body whose length is not known in advance: chunked on
    /// HTTP/1.1, DATA frames until the end on HTTP/2.
    public static final long UNKNOWN_LENGTH = -1;

    /// Opens a streamed exchange; see [Exchange]. Every default, no pool.
    public static Exchange open(String method, String url, List headers, long requestLength)
            throws IOException {
        return open(null, method, url, headers, requestLength);
    }

    static Exchange open(WebClient client, String method, String url, List headers,
                         long requestLength) throws IOException {
        Urls.requireHttp(url);
        HeaderLines.requireMethod(method);
        HeaderLines.validate(headers);
        if (requestLength < NO_BODY) {
            throw new IllegalArgumentException("requestLength " + requestLength);
        }
        StringBuilder joined = new StringBuilder();
        if (headers != null) {
            for (int iter = 0 ; iter < headers.size() ; iter++) {
                if (iter > 0) {
                    joined.append('\n');
                }
                joined.append(String.valueOf(headers.get(iter)));
            }
        }
        initialiseCurlOnce();
        // callerHeaders true: redirects are never followed in a stream anyway,
        // and it keeps the follow decision out of the transfer entirely.
        long handle = performClient(client, 0, method, url,
                HeaderLines.narrowed(joined.toString()), null, true, FLAG_STREAM, requestLength);
        if (handle == 0) {
            throw new IOException("Could not start a request to " + Urls.forMessage(url));
        }
        if (statusImpl(handle) == -1 && errorImpl(handle) != null) {
            String error = errorImpl(handle);
            freeImpl(handle);
            throw new IOException("Request to " + Urls.forMessage(url) + " failed: " + error);
        }
        return new Exchange(client, handle, url, requestLength != NO_BODY);
    }

    /// One streamed HTTP exchange: the request body written as it is produced,
    /// the response read as it arrives, neither held in memory whole. The way to
    /// move a large upload or download, or to forward one (see HttpProxy).
    ///
    /// Use it from one thread, in order: write and close [#getRequestBody] when
    /// the request has one, then [#getStatus] and the headers, then read
    /// [#getResponseBody]. Writes block until the transport has taken the bytes
    /// and reads until some have arrived, so a slow peer at either end slows the
    /// producer rather than filling memory -- each direction buffers a fixed
    /// 256 KiB window. [#close] ends the exchange whatever state it is in;
    /// before the response has been read to its end that CANCELS it, closing the
    /// upstream connection rather than draining it. Always close it, in a finally.
    ///
    /// Redirects are never followed here: a streamed body cannot be replayed.
    /// The request timeout of a WebClient does not apply either -- a stream may
    /// legitimately run for as long as it moves -- while its connect and read
    /// timeouts do, the read timeout bounding each wait for the peer. A stream
    /// has a connection of its own rather than one from the client's pool.
    public static final class Exchange {
        private final WebClient client;
        private final String url;
        private final boolean hasBody;
        private long handle;
        private boolean requestDone;
        private boolean headersRead;
        private boolean ended;
        private int status = -1;
        private String protocol;
        private Map values;
        private Map firstValues;
        private OutputStream requestBody;
        private InputStream responseBody;

        Exchange(WebClient client, long handle, String url, boolean hasBody) {
            this.client = client;
            this.handle = handle;
            this.url = url;
            this.hasBody = hasBody;
            this.requestDone = !hasBody;
        }

        /// Where the request body goes. Closing it ends the request body; the
        /// stream is the exchange's and closing it does not end the exchange.
        public OutputStream getRequestBody() throws IOException {
            if (!hasBody) {
                throw new IllegalStateException("this exchange was opened with NO_BODY");
            }
            requireOpen();
            if (requestBody == null) {
                requestBody = new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        write(new byte[] {(byte) b}, 0, 1);
                    }

                    @Override
                    public void write(byte[] data, int offset, int length) throws IOException {
                        if (requestDone) {
                            throw new IOException("the request body has been closed");
                        }
                        if (offset < 0 || length < 0 || length > data.length - offset) {
                            throw new IndexOutOfBoundsException();
                        }
                        int at = offset;
                        int left = length;
                        while (left > 0) {
                            requireOpen();
                            int taken = streamWriteImpl(handle, data, at, left);
                            if (taken < 0) {
                                throw new IOException("Out of memory for the request body to "
                                        + Urls.forMessage(url));
                            }
                            // Returns once libcurl has taken the whole block: the
                            // backpressure, and what makes reusing `data` safe.
                            int state = streamStepImpl(handle, WANT_SPACE);
                            if ((state & 2) != 0) {
                                ended = true;
                                if (statusImpl(handle) < 0) {
                                    throw failure();
                                }
                                throw new IOException("Request to " + Urls.forMessage(url)
                                        + " ended before its body was sent");
                            }
                            at += taken;
                            left -= taken;
                        }
                    }

                    @Override
                    public void close() throws IOException {
                        if (!requestDone) {
                            requestDone = true;
                            requireOpen();
                            streamEndUploadImpl(handle);
                        }
                    }
                };
            }
            return requestBody;
        }

        /// The response status, waiting for the response headers. Ends a request
        /// body that is still open first.
        public int getStatus() throws IOException {
            awaitResponse();
            return status;
        }

        /// "HTTP/1.1" or "HTTP/2", as negotiated.
        public String getProtocol() throws IOException {
            awaitResponse();
            return protocol;
        }

        /// The first value of every response header, by lower-case name.
        public Map getHeaders() throws IOException {
            awaitResponse();
            return firstValues;
        }

        /// Every response header, by lower-case name, each a List of values in
        /// arrival order -- Set-Cookie and the like keep every line.
        public Map getHeaderValueMap() throws IOException {
            awaitResponse();
            return values;
        }

        public String getHeader(String name) throws IOException {
            List all = getHeaderValues(name);
            return all.isEmpty() ? null : (String) all.get(0);
        }

        public List getHeaderValues(String name) throws IOException {
            awaitResponse();
            List all = name == null ? null : (List) values.get(asciiLower(name));
            return all == null ? new ArrayList() : all;
        }

        /// The response body as it arrives. Reading it to the end completes the
        /// exchange; [#close] before that cancels it.
        public InputStream getResponseBody() throws IOException {
            awaitResponse();
            if (responseBody == null) {
                responseBody = new InputStream() {
                    @Override
                    public int read() throws IOException {
                        byte[] one = new byte[1];
                        int n = read(one, 0, 1);
                        return n <= 0 ? -1 : one[0] & 0xff;
                    }

                    @Override
                    public int read(byte[] target, int offset, int length) throws IOException {
                        if (offset < 0 || length < 0 || length > target.length - offset) {
                            throw new IndexOutOfBoundsException();
                        }
                        if (length == 0) {
                            return 0;
                        }
                        while (true) {
                            requireOpen();
                            int n = streamReadImpl(handle, target, offset, length);
                            if (n > 0) {
                                return n;
                            }
                            if (ended) {
                                return -1;
                            }
                            int state = streamStepImpl(handle, WANT_DATA);
                            if ((state & 2) != 0) {
                                ended = true;
                                if (statusImpl(handle) < 0) {
                                    throw failure();
                                }
                            }
                        }
                    }

                    @Override
                    public void close() {
                        Exchange.this.close();
                    }
                };
            }
            return responseBody;
        }

        /// Ends the exchange. Before the response body has been read to its end
        /// this cancels it: the transfer is aborted and its connection closed.
        public void close() {
            if (handle == 0) {
                return;
            }
            long release = handle;
            handle = 0;
            freeImpl(release);
            if (client != null) {
                client.exchangeEnded();
            }
        }

        private void awaitResponse() throws IOException {
            requireOpen();
            if (headersRead) {
                return;
            }
            if (!requestDone) {
                getRequestBody().close();
            }
            int state = streamStepImpl(handle, WANT_HEADERS);
            if ((state & 2) != 0) {
                ended = true;
            }
            status = statusImpl(handle);
            if (status < 0) {
                throw failure();
            }
            values = parseHeaders(headersImpl(handle));
            firstValues = new LinkedHashMap();
            java.util.Iterator it = values.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List all = (List) entry.getValue();
                if (all != null && !all.isEmpty()) {
                    firstValues.put(entry.getKey(), all.get(0));
                }
            }
            protocol = protocolName(protocolImpl(handle));
            headersRead = true;
        }

        private void requireOpen() throws IOException {
            if (handle == 0) {
                throw new IOException("this exchange has been closed");
            }
        }

        private IOException failure() {
            String error = errorImpl(handle);
            return new IOException("Request to " + Urls.forMessage(url) + " failed: "
                    + (error == null ? "unknown error" : error));
        }
    }

    private static long performClient(WebClient client, long pool, String method, String url,
                                      byte[] headerLines, byte[] body, boolean callerHeaders) {
        return performClient(client, pool, method, url, headerLines, body, callerHeaders, 0,
                NO_BODY);
    }

    private static long performClient(WebClient client, long pool, String method, String url,
                                      byte[] headerLines, byte[] body, boolean callerHeaders,
                                      int extraFlags, long uploadLength) {
        boolean streamed = (extraFlags & FLAG_STREAM) != 0;
        if (client == null) {
            return performImpl(0, method, url, headerLines, body, callerHeaders, extraFlags,
                    30000, 30000, 0, null, null, -1, null, uploadLength);
        }
        int flags = FLAG_CLIENT | extraFlags;
        if (client.isRequireHttp2()) {
            flags |= FLAG_REQUIRE_HTTP2;
        }
        if (!client.isReuseConnections()) {
            flags |= FLAG_NO_REUSE;
        }
        ClientCertificate credential = client.getClientCertificate();
        byte[] certificate = null;
        byte[] key = null;
        int type = -1;
        String password = null;
        if (credential != null) {
            certificate = credential.getCertificate();
            key = credential.getKey();
            type = credential.getType();
            password = credential.getPassword();
        }
        // A stream has no whole-request deadline: it may run for as long as it
        // moves, which the read timeout (LOW_SPEED_TIME) still bounds.
        return performImpl(pool, method, url, headerLines, body, callerHeaders, flags,
                client.getConnectTimeoutMillis(), client.getReadTimeoutMillis(),
                streamed ? 0 : client.getRequestTimeoutMillis(), certificate, key, type,
                password, uploadLength);
    }

    /// CURLINFO_HTTP_VERSION's answer as a protocol name.
    private static String protocolName(int version) {
        switch (version) {
            case 1:
                return "HTTP/1.0";
            case 2:
                return "HTTP/1.1";
            case 3:
                return "HTTP/2";
            case 30:
                return "HTTP/3";
            default:
                return null;
        }
    }

    /// libcurl hands back the raw header block, status lines and all. Redirects
    /// mean there can be several blocks; the LAST one describes the response the
    /// caller got, so a later block replaces an earlier one rather than merging
    /// with it.
    static Map parseHeaders(String raw) {
        Map out = new LinkedHashMap();
        if (raw == null) {
            return out;
        }
        int at = 0;
        while (at < raw.length()) {
            int end = raw.indexOf('\n', at);
            if (end < 0) {
                end = raw.length();
            }
            String line = raw.substring(at, end).trim();
            at = end + 1;
            if (line.length() == 0) {
                continue;
            }
            if (line.regionMatches(true, 0, "HTTP/", 0, 5)) {
                // A new status line: everything before it belonged to a redirect.
                out.clear();
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            // EVERY value, in arrival order. Overwriting meant a response with
            // three Set-Cookie lines delivered one.
            String name = asciiLower(line.substring(0, colon).trim());
            List all = (List) out.get(name);
            if (all == null) {
                all = new ArrayList();
                out.put(name, all);
            }
            all.add(line.substring(colon + 1).trim());
        }
        return out;
    }

    /// Whether libcurl's global initialisation has been done successfully.
    private static boolean curlInitialised;

    /// Initialises libcurl once, before any thread can be inside the library.
    ///
    /// curl_easy_init does this implicitly on first use, and the implicit path is
    /// NOT thread safe below libcurl 7.84 or in a build whose curl_version_info
    /// does not report CURL_VERSION_THREADSAFE. Two workers whose first outbound
    /// request overlaps would both enter it. The link is against whatever -lcurl
    /// the system provides, with no version floor, so this cannot be assumed away.
    ///
    /// Every path into libcurl here is request(), and request() is the only
    /// caller of performImpl, so holding the monitor across the initialisation is
    /// enough: no other thread can be inside curl while it runs.
    ///
    /// Under the monitor rather than behind a double-checked volatile read.
    /// Every caller is about to make a network request, beside which an
    /// uncontended monitor costs nothing, and it keeps the guarantee off the
    /// question of how the translated runtime orders a volatile.
    ///
    /// The flag is set only when the initialisation SUCCEEDED, so a failure is
    /// retried rather than remembered as done. There is no matching
    /// curl_global_cleanup: the process is exiting by the time one would apply,
    /// and calling it while another thread might still be in libcurl is the very
    /// thing this avoids.
    private static synchronized void initialiseCurlOnce() throws IOException {
        if (curlInitialised) {
            return;
        }
        int rc = globalInitImpl();
        if (rc != 0) {
            // THROWN, not left for the request to carry on past. Returning here
            // set out to retry later and meanwhile let THIS worker walk into
            // curl_easy_init, whose implicit initialisation is the very thing
            // this monitor exists to keep one thread at a time -- so a failure
            // put one worker inside the implicit path while the next was inside
            // curl_global_init under the lock, which is the race, restored.
            throw new IOException("libcurl could not be initialised (CURLcode "
                    + rc + "), so no outbound request can be made");
        }
        curlInitialised = true;
    }

    /// curl_global_init's CURLcode -- 0 is CURLE_OK.
    private static native int globalInitImpl();

    /// @param pool a poolCreateImpl handle, or 0 for a connection of the request's own
    /// @param certificate the client certificate (PEM chain or PKCS#12), or null
    /// @param key the PEM private key, or null
    /// @param certificateType ClientCertificate.PEM or PKCS12, -1 for none
    private static native long performImpl(long pool, String method, String url,
                                           byte[] headerLines, byte[] body,
                                           boolean callerHeaders, int flags,
                                           int connectTimeoutMillis, int readTimeoutMillis,
                                           int requestTimeoutMillis, byte[] certificate,
                                           byte[] key, int certificateType, String password,
                                           long uploadLength);
    private static native int streamStepImpl(long handle, int want);
    private static native int streamReadImpl(long handle, byte[] buffer, int offset, int length);
    private static native int streamWriteImpl(long handle, byte[] buffer, int offset, int length);
    private static native void streamEndUploadImpl(long handle);
    private static native long poolCreateImpl();
    private static native void poolFreeImpl(long pool);
    private static native int protocolImpl(long handle);
    private static native String headersImpl(long handle);
    private static native int statusImpl(long handle);
    private static native String errorImpl(long handle);
    private static native byte[] bodyImpl(long handle);
    private static native void freeImpl(long handle);
}
