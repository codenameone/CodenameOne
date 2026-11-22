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
package com.codenameone.examples.bazaar.pos;

/// Where the receipt printer listens: `host` or `host:port`, the port 9100
/// (the raw printing port every network receipt printer opens) unless
/// given. An IPv6 address goes in brackets, `[fe80::1]:9100`.
public final class PrinterAddress {
    public static final int RAW_PORT = 9100;

    public final String host;
    public final int port;

    private PrinterAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /// The address typed, or null when it is not one.
    public static PrinterAddress parse(String typed) {
        if (typed == null) {
            return null;
        }
        String t = typed.trim();
        if (t.length() == 0 || t.indexOf(' ') >= 0 || t.indexOf('/') >= 0) {
            return null;
        }
        String host;
        String port = "";
        if (t.charAt(0) == '[') {
            int close = t.indexOf(']');
            if (close < 2) {
                return null;
            }
            host = t.substring(1, close);
            String rest = t.substring(close + 1);
            if (rest.length() > 0) {
                if (rest.charAt(0) != ':') {
                    return null;
                }
                port = rest.substring(1);
            }
        } else {
            int colon = t.indexOf(':');
            if (colon >= 0 && t.indexOf(':', colon + 1) >= 0) {
                // A bare IPv6 address: no room for a port without brackets.
                host = t;
            } else if (colon >= 0) {
                host = t.substring(0, colon);
                port = t.substring(colon + 1);
            } else {
                host = t;
            }
        }
        if (host.length() == 0 || host.length() > 253) {
            return null;
        }
        int p = RAW_PORT;
        if (port.length() > 0 || t.endsWith(":")) {
            p = digits(port);
            if (p < 1 || p > 65535) {
                return null;
            }
        }
        return new PrinterAddress(host, p);
    }

    /// `text` as a number of at most five digits, or -1.
    private static int digits(String text) {
        if (text.length() == 0 || text.length() > 5) {
            return -1;
        }
        int out = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }

    @Override
    public String toString() {
        return (host.indexOf(':') >= 0 ? "[" + host + "]" : host) + ":" + port;
    }
}
