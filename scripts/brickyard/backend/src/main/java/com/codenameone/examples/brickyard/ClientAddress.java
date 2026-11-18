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
package com.codenameone.examples.brickyard;

import com.codename1.backend.security.SecurityFilter;
import java.io.IOException;

/// The address of the client the current request came from, for checks
/// that compare where two requests came from without keeping the address.
///
/// It is the runtime's [com.codename1.backend.HttpServer.Request#getRemoteAddress]:
/// the connection's peer, or -- when the server runs behind a proxy and is
/// told so with `cn1.server.forwardHeaders=true` and
/// `cn1.server.trustedProxies` -- the client `X-Forwarded-For` names past
/// the trusted proxies. [#filter] records it for the request's thread.
public final class ClientAddress {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<String>();

    private ClientAddress() {
    }

    /// Records the request's address while the rest of the chain runs.
    public static SecurityFilter filter() {
        return (request, chain) -> {
            String previous = CURRENT.get();
            CURRENT.set(request.getRemoteAddress());
            try {
                return chain.doFilter(request);
            } finally {
                if (previous == null) {
                    CURRENT.remove();
                } else {
                    CURRENT.set(previous);
                }
            }
        };
    }

    /// The current request's address, or null when unknown.
    public static String current() {
        return CURRENT.get();
    }

    /// Sets the address for code run outside a request, such as a test.
    public static void set(String address) {
        if (address == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(address);
        }
    }

    /// A one-way hash of `address`, or "" when there is none; addresses are
    /// compared, never stored.
    public static String hash(String address) throws IOException {
        if (address == null || address.length() == 0) {
            return "";
        }
        return Ids.sha256("brickyard-ip:" + address).substring(0, 64);
    }
}
