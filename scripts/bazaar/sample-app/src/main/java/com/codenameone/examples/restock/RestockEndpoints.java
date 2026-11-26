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
package com.codenameone.examples.restock;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// The app's web surface. Every route only unpacks the request and hands it
/// to [RestockAlerts]; Bazaar is told which of these addresses to call when the
/// app sets a store up.
///
/// | Route | Called by |
/// |---|---|
/// | `GET /restock/install?store=` | the merchant, from the app directory |
/// | `GET /restock/callback` | the merchant's browser, back from the consent page |
/// | `POST /restock/webhooks` | Bazaar, signed |
/// | `POST /restock/blocks/data`, `/restock/blocks/action` | Bazaar, signed |
/// | `GET /restock/proxy/{store}[/{page}]` | Bazaar's app proxy, signed query |
/// | `GET /restock/admin?session=` | the admin, embedding the app's page |
@RestController
public class RestockEndpoints {
    private final RestockAlerts app;

    public RestockEndpoints(RestockAlerts app) {
        this.app = app;
    }

    private static HttpServer.Response page(int status, String html) throws IOException {
        return new HttpServer.Response(status, "text/html; charset=utf-8",
                html.getBytes("UTF-8")).header("Cache-Control", "no-store");
    }

    private static HttpServer.Response json(String json) throws IOException {
        return new HttpServer.Response(200, "application/json", json.getBytes("UTF-8"));
    }

    private static HttpServer.Response refused() throws IOException {
        return new HttpServer.Response(401, "text/plain", "Bad signature".getBytes("UTF-8"));
    }

    @GetMapping("/restock/install")
    public HttpServer.Response install(@RequestParam("store") String store) throws IOException {
        return new HttpServer.Response(302, "text/plain", new byte[0])
                .header("Location", app.installUrl(store));
    }

    @GetMapping("/restock/callback")
    public HttpServer.Response callback(@RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state) throws IOException {
        try {
            String store = app.completeInstall(code, state);
            return page(200, RestockAlerts.html("<p>Restock Alerts is installed in "
                    + RestockAlerts.escape(store) + ".</p>"));
        } catch (IOException failed) {
            // The reason goes to the log, never to the browser.
            System.err.println("Restock Alerts: install failed: " + failed.getMessage());
            return page(400, RestockAlerts.html("<p>The install did not complete.</p>"));
        }
    }

    @PostMapping("/restock/webhooks")
    public HttpServer.Response webhook(HttpServer.Request request) throws IOException {
        int status = app.webhook(request.getHeader("X-Bazaar-Topic"), body(request),
                request.getHeader("X-Bazaar-Signature"), request.getHeader("X-Bazaar-Timestamp"));
        return new HttpServer.Response(status, "text/plain", new byte[0]);
    }

    @PostMapping("/restock/blocks/data")
    public HttpServer.Response blockData(HttpServer.Request request) throws IOException {
        String answer = app.blockData(body(request), request.getHeader("X-Bazaar-Signature"),
                request.getHeader("X-Bazaar-Timestamp"));
        return answer == null ? refused() : json(answer);
    }

    @PostMapping("/restock/blocks/action")
    public HttpServer.Response blockAction(HttpServer.Request request) throws IOException {
        String answer = app.blockAction(body(request), request.getHeader("X-Bazaar-Signature"),
                request.getHeader("X-Bazaar-Timestamp"));
        return answer == null ? refused() : json(answer);
    }

    @GetMapping("/restock/proxy/{store}")
    public HttpServer.Response proxyRoot(@PathVariable("store") String store,
            HttpServer.Request request) throws IOException {
        return proxy(store, "", request);
    }

    @GetMapping("/restock/proxy/{store}/{page}")
    public HttpServer.Response proxyPage(@PathVariable("store") String store,
            @PathVariable("page") String page, HttpServer.Request request) throws IOException {
        return proxy(store, page, request);
    }

    private HttpServer.Response proxy(String store, String page, HttpServer.Request request)
            throws IOException {
        String html = app.proxyPage(store, page, query(request.getTarget()));
        return html == null ? refused() : page(200, html);
    }

    @GetMapping("/restock/admin")
    public HttpServer.Response admin(@RequestParam(value = "session", required = false)
            String session) throws IOException {
        String html = app.adminPage(session);
        return html == null ? refused() : page(200, html);
    }

    private static byte[] body(HttpServer.Request request) {
        byte[] b = request.getBodyBytes();
        return b == null ? new byte[0] : b;
    }

    /// The decoded query of `target`; the proxy signature is over decoded
    /// values, so every parameter is needed, not only the ones the page reads.
    static Map<String, String> query(String target) {
        int q = target == null ? -1 : target.indexOf('?');
        if (q < 0) {
            return new LinkedHashMap<String, String>();
        }
        return new LinkedHashMap<String, String>(OAuth2Parameters.parse(target.substring(q + 1)));
    }
}
