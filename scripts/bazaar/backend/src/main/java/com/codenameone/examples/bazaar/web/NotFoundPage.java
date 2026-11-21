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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;

import java.io.IOException;

/// The storefront's 404 page. A compiled view answers 200, and an exception
/// handler answers with a response rather than a view, so this one page is a
/// constant: it shows nothing of the request, so there is nothing to escape.
final class NotFoundPage {
    private static final String HTML = "<!doctype html><html lang=\"en\"><head>"
            + "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,"
            + " initial-scale=1\"><meta name=\"robots\" content=\"noindex\">"
            + "<title>Page not found - Bazaar</title>"
            + "<link rel=\"stylesheet\" href=\"/static/shop.18c86fc7.css\"></head><body>"
            + "<header class=\"bar\"><div class=\"wrap bar-in\"><a class=\"brand\" href=\"/\">"
            + "Bazaar</a></div></header><main class=\"wrap empty-state\">"
            + "<p class=\"big\">404</p><h1>We could not find that page</h1>"
            + "<p>The shop, product or shipment may have moved or is no longer available.</p>"
            + "<p><a class=\"button\" href=\"/\">Browse the shops</a></p></main></body></html>";

    private NotFoundPage() {
    }

    static HttpServer.Response response() throws IOException {
        return new HttpServer.Response(404, "text/html; charset=utf-8", HTML.getBytes("UTF-8"))
                .header("Cache-Control", "public, max-age=60");
    }
}
