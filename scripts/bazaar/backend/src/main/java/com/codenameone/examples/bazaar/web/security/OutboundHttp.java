/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.web.security;

import com.codename1.backend.Web;
import com.codename1.backend.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// Every request the server makes to a URL a merchant or an app chose:
/// webhooks, app extensions and proxies, checkout functions and the custom
/// domain probe. Each one is
///
/// - checked with [UrlGuard#refuse(String, UrlGuard.Resolver)] against what
///   the host resolves to now;
/// - bounded in time, the whole exchange and not only each wait for the peer;
/// - bounded in size while it is read: the answer is never buffered past
///   `maxBytes`, the exchange is cancelled at that point;
/// - never redirected. A streamed exchange does not follow a redirect, so a
///   3xx is answered to the caller as it is, and every caller here takes it
///   as a failure. A redirect would otherwise be a way to reach an address
///   the guard never saw.
///
/// **What this does not close: DNS rebinding.** The guard resolves the host,
/// and the HTTP client resolves it again when it connects. A name with a
/// short TTL can answer a public address to the first lookup and a private
/// one to the second. The backend's client has no way to connect to an
/// address chosen in advance while still sending the name for the `Host`
/// header and TLS (the JDK drops a caller's `Host` header, and certificate
/// checks follow the URL's host), nor to report the address it connected
/// to. A deployment that must rule rebinding out sends these requests
/// through an egress proxy that applies the same rules to the address it
/// actually connects to, or keeps the server on a network that cannot reach
/// anything private.
public final class OutboundHttp {
    /// The answer of one request.
    public static final class Reply {
        public final int status;
        public final String contentType;
        public final byte[] body;

        public Reply(int status, String contentType, byte[] body) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
        }

        /// The body as UTF-8 text.
        public String text() throws IOException {
            return body == null ? "" : new String(body, "UTF-8");
        }
    }

    private OutboundHttp() {
    }

    /// Sends one request to a URL someone else chose; see the class.
    ///
    /// - `IOException`: when the URL is refused, nothing answered in time, or
    ///   the answer is longer than `maxBytes`
    public static Reply send(String method, String url, List<String> headers, byte[] body,
            int timeoutMillis, int maxBytes) throws IOException {
        String unsafe = UrlGuard.refuse(url, UrlGuard.DNS);
        if (unsafe != null) {
            throw new IOException(unsafe);
        }
        return sendUnguarded(method, url, headers, body, timeoutMillis, maxBytes);
    }

    /// [#send] without the address check: for the tests, which talk to a
    /// server on this machine, and nothing else.
    static Reply sendUnguarded(String method, String url, List<String> headers, byte[] body,
            int timeoutMillis, int maxBytes) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        WebClient client = new WebClient().setConnectTimeoutMillis(timeoutMillis)
                .setReadTimeoutMillis(timeoutMillis).setRequestTimeoutMillis(timeoutMillis)
                .setReuseConnections(false);
        Web.Exchange exchange = null;
        try {
            exchange = client.open(method, url, headers == null ? new ArrayList<String>()
                    : new ArrayList<String>(headers), body == null ? Web.NO_BODY : body.length);
            if (body != null) {
                OutputStream out = exchange.getRequestBody();
                out.write(body);
                out.close();
            }
            int status = exchange.getStatus();
            String type = exchange.getHeader("Content-Type");
            InputStream in = exchange.getResponseBody();
            ByteArrayOutputStream kept = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            while (true) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IOException("The answer did not arrive within " + timeoutMillis
                            + " ms");
                }
                int n = in.read(chunk, 0, chunk.length);
                if (n < 0) {
                    break;
                }
                if (kept.size() + n > maxBytes) {
                    throw new IOException("The answer is longer than " + maxBytes + " bytes");
                }
                kept.write(chunk, 0, n);
            }
            return new Reply(status, type, kept.toByteArray());
        } finally {
            // The exchange first: before its body is read to the end, closing
            // it cancels the transfer rather than draining it, and the client
            // counts it as in flight until then.
            if (exchange != null) {
                exchange.close();
            }
            client.close();
        }
    }
}
