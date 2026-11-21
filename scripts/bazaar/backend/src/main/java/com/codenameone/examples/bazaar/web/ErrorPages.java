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
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.ControllerAdvice;
import com.codename1.backend.annotations.ExceptionHandler;
import com.codename1.backend.log.Logger;
import com.codename1.backend.security.FilterChain;
import com.codename1.backend.security.SecurityFilter;

import java.io.IOException;

/// The web app's error pages: 404, 403, 429, 500 and one for any other
/// refusal.
///
/// Each is a constant. Nothing of the request or of the failure is in it -- not
/// the path, not the reason a service gave, not a stack trace -- so it cannot
/// leak what it did not get, and there is nothing in it to escape. It has no
/// inline style or script, so it passes the policy of a public page and of a
/// signed-in one alike.
///
/// They reach the browser three ways:
///
/// - **From a web controller**, by this class as advice: a
///   `ResponseStatusException` gives its status's page, anything else is
///   logged here and answers 500. A controller with its own handler (the
///   storefront, the checkout) is asked first.
/// - **From the security filters**, which answer before any controller -- the
///   rate limits (429), a missing or wrong CSRF token and a role the caller
///   lacks (403) -- through [#filter()], which swaps such a plain answer for
///   the page.
/// - The 404 of the storefront, through [NotFoundPage].
///
/// `/api/**` is never touched: the app reads its errors as JSON.
@ControllerAdvice(basePackages = "com.codenameone.examples.bazaar.web")
public class ErrorPages {
    private static final Logger LOG = Logger.getLogger(ErrorPages.class);
    private static final String CSS = "/static/shop.46baf561.css";

    private static String page(String code, String title, String text) {
        return "<!doctype html><html lang=\"en\"><head>"
                + "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,"
                + " initial-scale=1\"><meta name=\"robots\" content=\"noindex\">"
                + "<title>" + title + " - Bazaar</title>"
                + "<link rel=\"stylesheet\" href=\"" + CSS + "\"></head><body>"
                + "<header class=\"bar\"><div class=\"wrap bar-in\"><a class=\"brand\" href=\"/\">"
                + "Bazaar</a></div></header><main class=\"wrap empty-state\">"
                + "<p class=\"big\">" + code + "</p><h1>" + title + "</h1><p>" + text + "</p>"
                + "<p><a class=\"button\" href=\"/\">Browse the shops</a></p></main></body></html>";
    }

    static final String NOT_FOUND = page("404", "We could not find that page",
            "The shop, product or shipment may have moved or is no longer available.");
    static final String FORBIDDEN = page("403", "You cannot do that here",
            "Sign in with an account that may, or go back and try again.");
    static final String TOO_MANY = page("429", "Slow down a little",
            "That was a lot of requests in a short time. Wait a minute and try again.");
    static final String FAILED = page("500", "Something went wrong on our side",
            "Nothing you did caused this. Try again in a moment.");
    static final String REFUSED = page("400", "We could not do that",
            "Go back, check what was entered and try again.");

    /// The page for `status`, with the headers its kind needs.
    public static HttpServer.Response response(int status) throws IOException {
        String html;
        int code = status;
        if (status == 404) {
            html = NOT_FOUND;
        } else if (status == 403 || status == 401) {
            html = FORBIDDEN;
            code = 403;
        } else if (status == 429) {
            html = TOO_MANY;
        } else if (status >= 500) {
            html = FAILED;
            code = 500;
        } else {
            html = REFUSED;
        }
        HttpServer.Response r = new HttpServer.Response(code, "text/html; charset=utf-8",
                html.getBytes("UTF-8"));
        // A missing page is the same for everybody; anything else is about one
        // request and is never kept.
        r.header("Cache-Control", code == 404 ? "public, max-age=60" : "no-store");
        if (code == 429) {
            r.header("Retry-After", "60");
        }
        return r;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        return response(e.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public HttpServer.Response failed(Exception e) throws IOException {
        // Logged here, never shown: the page says only that it failed.
        LOG.error("A web page failed", e);
        return response(500);
    }

    /// Whether `request` is one of the web app's own (not the app's API).
    static boolean web(HttpServer.Request request) {
        String t = request.getTarget();
        return t != null && !t.startsWith("/api/") && !t.startsWith("/oauth2/");
    }

    /// A security filter that swaps the plain answers the filters behind it
    /// give -- 403 for a CSRF token or a role, 429 for a rate limit, 500 for a
    /// failure -- for the page. Added first in a web chain, so it sees what
    /// every filter behind it answered.
    public static SecurityFilter filter() {
        return new SecurityFilter() {
            @Override
            public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
                    throws Exception {
                HttpServer.Response r;
                try {
                    r = chain.doFilter(request);
                } catch (ResponseStatusException e) {
                    return response(e.getStatus());
                } catch (Exception e) {
                    LOG.error("A web request failed", e);
                    return response(500);
                }
                if (r == null || !web(request)) {
                    return r;
                }
                int s = r.getStatus();
                if (s != 403 && s != 429 && s < 500) {
                    return r;
                }
                String type = r.getContentType();
                if (type != null && type.startsWith("text/html")) {
                    return r;
                }
                return response(s);
            }
        };
    }
}
