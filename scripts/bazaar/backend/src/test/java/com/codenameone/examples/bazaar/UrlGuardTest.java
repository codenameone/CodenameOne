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
package com.codenameone.examples.bazaar;

import com.codenameone.examples.bazaar.web.security.UrlGuard;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Server-side request forgery: webhook URLs that point at this machine, the
/// private network or the metadata service are refused at save and delivery.
class UrlGuardTest {
    @Test
    void refusesInwardAddressesByName() {
        String[] bad = {"http://localhost/x", "http://127.0.0.1/", "http://10.1.2.3/",
            "http://169.254.169.254/latest/meta-data", "http://192.168.0.1:8080/",
            "http://172.20.0.1/", "http://[::1]/", "http://[fd00::1]/", "http://[fe80::1]/",
            "http://[::ffff:127.0.0.1]/", "http://2130706433/", "http://0x7f.1/",
            "http://metadata.google.internal/", "http://printer.local/", "http://intranet/",
            "http://user@evil.example@127.0.0.1/", "ftp://example.com/", "http://0.0.0.0/"};
        for (int iter = 0; iter < bad.length; iter++) {
            assertNotNull(UrlGuard.refuseByName(bad[iter]), bad[iter]);
        }
        assertNull(UrlGuard.refuseByName("https://hooks.example.com/bazaar"));
        assertNull(UrlGuard.refuseByName("https://93.184.216.34/x"));
        assertNull(UrlGuard.refuseByName("https://cafe.example.com/x"));
    }

    @Test
    void refusesANameThatResolvesInward() {
        UrlGuard.Resolver inward = host -> new byte[][] {{8, 8, 8, 8}, {10, 0, 0, 5}};
        UrlGuard.Resolver outward = host -> new byte[][] {{93, (byte) 184, (byte) 216, 34}};
        UrlGuard.Resolver none = host -> {
            throw new UnknownHostException(host);
        };
        assertNotNull(UrlGuard.refuse("https://rebind.example.com/", inward));
        assertNull(UrlGuard.refuse("https://hooks.example.com/", outward));
        assertNotNull(UrlGuard.refuse("https://nowhere.example.com/", none));
    }
}
