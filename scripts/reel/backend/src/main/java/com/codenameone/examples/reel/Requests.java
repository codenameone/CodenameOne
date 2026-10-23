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
package com.codenameone.examples.reel;

import com.codename1.backend.security.SecurityExchange;

/// What the request being served says about where it came from.
///
/// The app names its device in two headers it sends with every call:
/// `X-Reel-Device`, an id it made once and keeps, and `X-Reel-Device-Name`.
/// The country is the one a CDN in front of the server puts in `CF-IPCountry`;
/// without such a CDN it is empty and sharing detection works on addresses
/// alone.
public final class Requests {
    public static final String DEVICE = "X-Reel-Device";
    public static final String DEVICE_NAME = "X-Reel-Device-Name";
    public static final String PLATFORM = "X-Reel-Platform";
    public static final String PROFILE_KEY = "X-Reel-Profile-Key";

    private Requests() {
    }

    public static String header(String name) {
        SecurityExchange exchange = SecurityExchange.current();
        String value = exchange == null ? null : exchange.getRequest().getHeader(name);
        return value == null ? "" : value.trim();
    }

    /// The device id, cut to what the column holds; empty when not sent.
    public static String deviceId() {
        return clean(header(DEVICE), 64);
    }

    public static String deviceName() {
        String name = clean(header(DEVICE_NAME), 80);
        return name.length() == 0 ? "Unknown device" : name;
    }

    public static String platform() {
        return clean(header(PLATFORM), 32);
    }

    public static String ip() {
        SecurityExchange exchange = SecurityExchange.current();
        String address = exchange == null ? null : exchange.getRequest().getRemoteAddress();
        return address == null ? "" : clean(address, 64);
    }

    public static String country() {
        return clean(header("CF-IPCountry"), 8);
    }

    /// Printable ASCII only, at most `max` characters: these go into columns,
    /// logs and admin screens.
    static String clean(String value, int max) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < value.length() && out.length() < max; iter++) {
            char c = value.charAt(iter);
            if (c >= ' ' && c <= '~') {
                out.append(c);
            }
        }
        return out.toString().trim();
    }
}
