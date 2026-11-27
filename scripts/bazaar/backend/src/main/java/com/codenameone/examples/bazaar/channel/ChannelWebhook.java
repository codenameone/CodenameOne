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
package com.codenameone.examples.bazaar.channel;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.PostMapping;

/// A marketplace's order notification:
/// `POST /api/channels/notifications?channel={id}`, the raw body signed in
/// [MarketplaceConnector#SIGNATURE_HEADER]. Open to anyone -- the signature
/// is the authentication -- and rate limited by address in `SecurityConfig`.
/// An unknown channel and a bad signature get the same answer.
@Controller
public class ChannelWebhook {
    public static final String PATH = "/api/channels/notifications";

    private final ChannelSync sync;

    public ChannelWebhook(ChannelSync sync) {
        this.sync = sync;
    }

    @PostMapping(PATH)
    public HttpServer.Response receive(HttpServer.Request request) throws Exception {
        String channel;
        try {
            channel = request.param("channel");
        } catch (RuntimeException malformed) {
            channel = null;
        }
        try {
            sync.notification(channel, request.getBodyBytes(),
                    request.getHeader(MarketplaceConnector.SIGNATURE_HEADER),
                    System.currentTimeMillis());
            return HttpServer.Response.text(200, "ok");
        } catch (ResponseStatusException e) {
            return HttpServer.Response.text(e.getStatus(), e.getReason() == null ? "refused"
                    : e.getReason());
        }
    }
}
