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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Forwards a request to another HTTP server and streams its answer back: a
/// reverse proxy, an API gateway route, a pass-through to an internal service.
///
/// ```java
/// HttpProxy images = new HttpProxy("http://images.internal:8080");
/// // in a handler:
/// return images.forward(request);              // same path and query upstream
/// return images.forward(request, "/v2/thumbs"); // or a path of your choosing
/// ```
///
/// What it guarantees:
///
/// - **Bounded memory.** A binary request body is streamed up from wherever it
///   is -- a spooled upload included -- and the response body is streamed down
///   as it arrives ([Web.Exchange]). A text or compressed request body is the
///   server's in-memory one (under its 8 MB limit), forwarded decoded.
/// - **Headers survive, hop-by-hop ones do not.** Every end-to-end header goes
///   through in both directions, a repeated one (`Set-Cookie`) line by line.
///   The hop-by-hop set of RFC 9110 7.6.1 -- `Connection`, `Keep-Alive`,
///   `Proxy-*`, `TE`, `Trailer`, `Transfer-Encoding`, `Upgrade` -- and every
///   header a `Connection` field names are removed each way. `Host` and
///   `Content-Length` are the transport's. `X-Forwarded-For`, `-Proto` and
///   `-Host` are added unless [#setForwardedHeaders] turns them off.
/// - **Destinations are fixed.** Only the target's own host is reached, plus
///   hosts named with [#allowHost]; a redirect elsewhere is passed back to the
///   client, never followed. Only `http` and `https`.
/// - **Redirects are passed through** by default. [#setFollowRedirects] follows
///   up to that many hops itself, for a request with no body only -- a streamed
///   body cannot be sent twice -- and only to allowed hosts. A hop to another
///   host drops the client's `Authorization` and `Cookie`.
/// - **Cancellation goes both ways.** A client that disconnects mid-download
///   makes the next write fail, which closes the upstream exchange at once. An
///   upstream that fails before answering is a `502`; one that fails after the
///   status and headers went out cannot be turned into an error response any
///   more, so the client's response is aborted -- the HTTP/1.1 connection is
///   closed without its final chunk, the HTTP/2 stream is reset -- and the
///   client sees a truncated response, never a complete-looking one.
///
/// The response is re-framed by this server: chunked on HTTP/1.1, DATA frames on
/// HTTP/2, whatever length the upstream declared. One instance serves any number
/// of concurrent requests once configured.
public final class HttpProxy {
    private final String base;
    private final String baseHost;
    private final List allowedHosts = new ArrayList();
    private WebClient client;
    private int followRedirects;
    private boolean forwardedHeaders = true;

    /// - `target`: the upstream's origin and optional base path, such as
    ///   `https://api.internal:8443` or `http://10.0.0.5/service`
    public HttpProxy(String target) throws IOException {
        Urls.requireHttp(target);
        String trimmed = target;
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.base = trimmed;
        this.baseHost = hostOf(trimmed);
        if (baseHost == null) {
            throw new IllegalArgumentException("the proxy target names no host");
        }
    }

    /// The client upstream requests go through, for its timeouts and client
    /// certificate; a default one when unset.
    public HttpProxy setClient(WebClient client) {
        this.client = client;
        return this;
    }

    /// Lets requests -- redirects and [#forward(HttpServer.Request, String)]
    /// with an absolute URL -- reach `host` (a name, or `name:port`) as well as
    /// the target's own.
    public HttpProxy allowHost(String host) {
        if (host != null && host.length() > 0) {
            allowedHosts.add(asciiLower(host));
        }
        return this;
    }

    /// How many redirects to follow on the client's behalf; 0, the default,
    /// hands every 3xx back to the client as it came.
    public HttpProxy setFollowRedirects(int hops) {
        this.followRedirects = Math.max(0, hops);
        return this;
    }

    /// Whether to add `X-Forwarded-For`, `X-Forwarded-Proto` and
    /// `X-Forwarded-Host`; on by default.
    public HttpProxy setForwardedHeaders(boolean forwardedHeaders) {
        this.forwardedHeaders = forwardedHeaders;
        return this;
    }

    /// Forwards to the target with the request's own path and query appended.
    public HttpServer.Response forward(HttpServer.Request request) {
        return forward(request, request.getTarget());
    }

    /// Forwards to `pathOrUrl`: a path (with any query) appended to the target,
    /// or an absolute URL, which must name an allowed host.
    public HttpServer.Response forward(HttpServer.Request request, String pathOrUrl) {
        String url = pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")
                ? pathOrUrl
                : base + (pathOrUrl.startsWith("/") ? "" : "/") + pathOrUrl;
        if (!isAllowed(url)) {
            return HttpServer.Response.text(502, "the proxy may not reach that destination");
        }
        String method = request.getMethod();
        List headers = requestHeaders(request);
        long length = request.getBodyLength();
        boolean hasBody = length > 0 || request.getHeader("Content-Length") != null
                || request.getHeader("Transfer-Encoding") != null;
        Web.Exchange exchange = null;
        boolean handedOver = false;
        try {
            int hops = 0;
            int status;
            while (true) {
                exchange = open(method, url, headers, hasBody ? length : Web.NO_BODY);
                if (hasBody && length > 0) {
                    InputStream in = request.getBodyStream();
                    try {
                        OutputStream out = exchange.getRequestBody(); //NOPMD CloseResource - closed below; the exchange owns it
                        copy(in, out);
                        out.close();
                    } finally {
                        in.close();
                    }
                }
                status = exchange.getStatus();
                String location = exchange.getHeader("Location");
                // A body of no bytes can be sent again; only a real one cannot.
                String next = hops < followRedirects && length == 0 && isRedirect(status)
                        && location != null ? resolve(url, location) : null;
                if (next == null || !isAllowed(next)) {
                    break;
                }
                exchange.close();
                exchange = null;
                String nextHost = hostOf(next);
                if (nextHost == null || !nextHost.equalsIgnoreCase(hostOf(url))
                        || !url.regionMatches(true, 0, next, 0, next.indexOf(':') + 1)) {
                    // The client's credentials were meant for the host it asked;
                    // another one, allowed or not, does not get them, nor does the
                    // same host over another scheme (https to http would send them
                    // in the clear) -- as curl
                    // and browsers drop them on a redirect across hosts.
                    headers = withoutCredentials(headers);
                }
                url = next;
                hops++;
                if (status == 303) {
                    method = "HEAD".equals(method) ? "HEAD" : "GET";
                }
            }
            HttpServer.Response response = respond(request, exchange, status);
            handedOver = true;
            return response;
        } catch (IOException err) {
            // Before any of the response went out, so it can still be answered.
            return HttpServer.Response.text(502, "the upstream request failed");
        } finally {
            if (!handedOver && exchange != null) {
                exchange.close();
            }
        }
    }

    private Web.Exchange open(String method, String url, List headers, long length)
            throws IOException {
        return client != null ? client.open(method, url, headers, length)
                : Web.open(method, url, headers, length);
    }

    /// The client's response: the upstream status and end-to-end headers, and a
    /// body that streams from the exchange, which the writer then owns.
    private static HttpServer.Response respond(HttpServer.Request request,
                                               final Web.Exchange exchange, int status)
            throws IOException {
        Map values = exchange.getHeaderValueMap();
        List dropped = connectionTokens(values.get("connection"));
        Map out = new LinkedHashMap();
        String contentType = null;
        Iterator it = values.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            String name = (String) e.getKey();
            if ("content-type".equals(name)) {
                List all = (List) e.getValue();
                contentType = all.isEmpty() ? null : (String) all.get(0);
                continue;
            }
            if (isHopByHop(name) || dropped.contains(name) || "content-length".equals(name)
                    || "date".equals(name)) {
                continue;
            }
            List all = (List) e.getValue();
            out.put(name, all.size() == 1 ? all.get(0) : new ArrayList(all));
        }
        boolean bodiless = "HEAD".equals(request.getMethod()) || status == 204 || status == 304
                || (status >= 100 && status < 200);
        long described = digits(values.get("content-length"));
        if (bodiless && (described >= 0 || !"HEAD".equals(request.getMethod()))) {
            exchange.close();
            HttpServer.Response head = HttpServer.Response.empty(status, contentType, out);
            // A HEAD describes the representation it does not send: the
            // upstream's length, not the 0 of this empty body.
            head.describedLength = "HEAD".equals(request.getMethod()) ? described : -1;
            return head;
        }
        // From here a HEAD is one whose upstream gave no length, and it goes out
        // as a stream so that none is invented: the server never runs a HEAD's
        // writer, and closes it -- ending the exchange -- instead.
        HttpServer.Response response = HttpServer.Response.stream(status, contentType,
                new Relay(exchange));
        response.extraHeaders = out;
        return response;
    }

    /// Copies the upstream body to the client. Closeable because the server
    /// does not always run a writer (a client gone before the headers, an
    /// HTTP/2 stream it could not open), and the exchange must end either way.
    private static final class Relay implements HttpServer.BodyWriter, Closeable {
        private final Web.Exchange exchange;

        Relay(Web.Exchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void writeTo(OutputStream output) throws IOException {
            try {
                copy(exchange.getResponseBody(), output);
            } finally {
                // Complete, or cancelled: either way the upstream is done, and on
                // a client that went away this is what stops it sending.
                exchange.close();
            }
        }

        @Override
        public void close() {
            exchange.close();
        }
    }

    /// A Content-Length value as a number, or -1 for none or anything but
    /// plain digits (a list of several is not one length).
    private static long digits(Object value) {
        Object one = value;
        if (one instanceof List) {
            List all = (List) one;
            one = all.size() == 1 ? all.get(0) : null;
        }
        if (!(one instanceof String)) {
            return -1;
        }
        String text = ((String) one).trim();
        if (text.length() == 0 || text.length() > 18) {
            return -1;
        }
        long n = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }

    private static List withoutCredentials(List headers) {
        List out = new ArrayList();
        for (Object each : headers) {
            String line = (String) each;
            if (!line.regionMatches(true, 0, "authorization:", 0, 14)
                    && !line.regionMatches(true, 0, "cookie:", 0, 7)) {
                out.add(line);
            }
        }
        return out;
    }

    private List requestHeaders(HttpServer.Request request) {
        Map headers = request.getHeaders();
        List dropped = connectionTokens(headers.get("connection"));
        List out = new ArrayList();
        String forwardedFor = null;
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            String name = (String) e.getKey();
            String value = String.valueOf(e.getValue());
            // Content-Encoding goes too: what is forwarded is always identity. A
            // spooled body is identity by definition, and one the client sent
            // gzip encoded was decoded on arrival (getBodyBytes), so passing the
            // header on would label plain bytes as gzip.
            if (isHopByHop(name) || dropped.contains(name) || "host".equals(name)
                    || "content-length".equals(name) || "expect".equals(name)
                    || "content-encoding".equals(name)) {
                continue;
            }
            if (forwardedHeaders && "x-forwarded-for".equals(name)) {
                forwardedFor = value;
                continue;
            }
            if (forwardedHeaders && ("x-forwarded-proto".equals(name)
                    || "x-forwarded-host".equals(name))) {
                continue;
            }
            out.add(name + ": " + value);
        }
        if (forwardedHeaders) {
            String peer = request.getPeerAddress();
            if (peer != null) {
                out.add("x-forwarded-for: " + (forwardedFor == null ? peer
                        : forwardedFor + ", " + peer));
            } else if (forwardedFor != null) {
                out.add("x-forwarded-for: " + forwardedFor);
            }
            out.add("x-forwarded-proto: " + (request.isSecure() ? "https" : "http"));
            String host = request.getHeader("Host");
            if (host != null) {
                out.add("x-forwarded-host: " + host);
            }
        }
        return out;
    }

    /// RFC 9110 7.6.1, plus the Proxy-Connection some clients still send.
    static boolean isHopByHop(String lowerName) {
        return "connection".equals(lowerName) || "keep-alive".equals(lowerName)
                || "proxy-connection".equals(lowerName)
                || "proxy-authenticate".equals(lowerName)
                || "proxy-authorization".equals(lowerName) || "te".equals(lowerName)
                || "trailer".equals(lowerName) || "transfer-encoding".equals(lowerName)
                || "upgrade".equals(lowerName);
    }

    /// The header names a Connection field lists, lower-cased; a value may be a
    /// String or, from a response, a List of lines.
    static List connectionTokens(Object value) {
        List out = new ArrayList();
        if (value == null) {
            return out;
        }
        List lines = new ArrayList();
        if (value instanceof List) {
            lines.addAll((List) value);
        } else {
            lines.add(value);
        }
        for (Object line : lines) {
            String text = String.valueOf(line);
            int at = 0;
            while (at <= text.length()) {
                int comma = text.indexOf(',', at);
                int end = comma < 0 ? text.length() : comma;
                String token = text.substring(at, end).trim();
                if (token.length() > 0) {
                    out.add(asciiLower(token));
                }
                if (comma < 0) {
                    break;
                }
                at = comma + 1;
            }
        }
        return out;
    }

    private boolean isAllowed(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        String host = hostOf(url);
        if (host == null) {
            return false;
        }
        if (host.equals(baseHost) || allowedHosts.contains(host)) {
            return true;
        }
        int colon = host.lastIndexOf(':');
        return colon > 0 && host.indexOf(']') < colon
                && allowedHosts.contains(host.substring(0, colon));
    }

    /// `host[:port]` of an absolute http(s) URL, lower-cased, or null. Userinfo
    /// is refused rather than parsed, since `a@b` is how a destination check is
    /// usually walked around.
    static String hostOf(String url) {
        int start = url.indexOf("://");
        if (start < 0) {
            return null;
        }
        start += 3;
        int end = start;
        while (end < url.length() && "/?#".indexOf(url.charAt(end)) < 0) {
            end++;
        }
        String authority = url.substring(start, end);
        if (authority.length() == 0 || authority.indexOf('@') >= 0
                || authority.indexOf('\\') >= 0) {
            return null;
        }
        return asciiLower(authority);
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307
                || status == 308;
    }

    /// A Location against the URL that produced it: absolute, or rooted at its
    /// origin. Relative-path references are uncommon enough in a Location to be
    /// handed back to the client instead.
    static String resolve(String current, String location) {
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return location;
        }
        if (location.startsWith("//")) {
            int colon = current.indexOf(':');
            return current.substring(0, colon + 1) + location;
        }
        if (location.startsWith("/")) {
            int start = current.indexOf("://") + 3;
            int end = start;
            while (end < current.length() && "/?#".indexOf(current.charAt(end)) < 0) {
                end++;
            }
            return current.substring(0, end) + location;
        }
        return null;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        int n;
        while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
            if (n > 0) {
                out.write(buffer, 0, n);
            }
        }
    }

    private static String asciiLower(String value) {
        char[] chars = value.toCharArray();
        for (int iter = 0 ; iter < chars.length ; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }
}
