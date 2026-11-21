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
package com.codenameone.examples.bazaar.marketing;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/// The links a marketing message carries, all plain GETs a mail client can
/// follow without signing in (`/api/shop` GETs are open): the open pixel,
/// the click redirect, the unsubscribe link and the signup confirmation.
/// None of them says whether a token was known.
@RestController
public class MarketingLinks {
    /// A transparent 1x1 GIF.
    static final byte[] PIXEL = {71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, 0, 0, 0, -1, -1, -1,
        33, -7, 4, 1, 0, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59};

    private final Campaigns campaigns;
    private final SignupForms forms;

    public MarketingLinks(Campaigns campaigns, SignupForms forms) {
        this.campaigns = campaigns;
        this.forms = forms;
    }

    @GetMapping("/api/shop/m/o/{file}")
    public HttpServer.Response open(@PathVariable("file") String file) {
        String token = file != null && file.endsWith(".gif") ? file.substring(0, file.length() - 4) : file;
        try {
            campaigns.opened(token);
        } catch (RuntimeException failed) {
            // The image is answered whatever happened.
        }
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cache-Control", "no-store");
        return new HttpServer.Response(200, "image/gif", PIXEL, headers);
    }

    @GetMapping("/api/shop/m/c/{token}/{position}")
    public HttpServer.Response click(@PathVariable("token") String token,
            @PathVariable("position") String position) {
        int at = -1;
        if (position != null && position.length() == 1 && position.charAt(0) >= '0'
                && position.charAt(0) <= '9') {
            at = position.charAt(0) - '0';
        }
        String to = at < 0 ? null : campaigns.clicked(token, at);
        if (to == null) {
            return page(404, "That link has expired.");
        }
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Location", to);
        return new HttpServer.Response(302, "text/plain", new byte[0], headers);
    }

    @GetMapping("/api/shop/m/u/{token}")
    public HttpServer.Response unsubscribe(@PathVariable("token") String token,
            @RequestParam(value = "s", required = false) String signature) throws Exception {
        boolean done = campaigns.unsubscribe(token, signature);
        return page(done ? 200 : 400, done ? "You are unsubscribed. You will not hear from us again."
                : "That unsubscribe link is not valid.");
    }

    @GetMapping("/api/shop/m/confirm/{token}")
    public HttpServer.Response confirm(@PathVariable("token") String token) throws Exception {
        String code = forms.confirm(token, Instant.now());
        if (code == null) {
            return page(400, "That link was already used or is not valid.");
        }
        return page(200, code.length() == 0 ? "You are subscribed. Thank you."
                : "You are subscribed. Your welcome code is " + code + ".");
    }

    static HttpServer.Response page(int status, String message) {
        String html = "<!doctype html><meta charset=utf-8><meta name=viewport "
                + "content=\"width=device-width\"><title>Bazaar</title><body style=\"font:16px "
                + "system-ui;max-width:32em;margin:4em auto;padding:0 16px\"><p>" + message
                + "</p></body>";
        try {
            return new HttpServer.Response(status, "text/html; charset=utf-8", html.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException never) {
            return new HttpServer.Response(status, "text/plain", new byte[0]);
        }
    }
}
