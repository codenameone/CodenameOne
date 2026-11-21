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
package com.codenameone.examples.bazaar.dispute;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.PostMapping;

/// The payment processor's dispute events: `POST /api/payments/webhooks/disputes`
/// with the body signed in `X-Bazaar-Signature` ([Disputes#webhook]). Open to
/// anyone -- the signature is the authentication -- and rate limited by
/// address in `SecurityConfig`.
@Controller
public class DisputeWebhook {
    public static final String PATH = "/api/payments/webhooks/disputes";

    private final Disputes disputes;

    public DisputeWebhook(Disputes disputes) {
        this.disputes = disputes;
    }

    @PostMapping(PATH)
    public HttpServer.Response receive(HttpServer.Request request) throws Exception {
        try {
            disputes.webhook(request.getBodyBytes(), request.getHeader("X-Bazaar-Signature"));
            return HttpServer.Response.text(200, "ok");
        } catch (ResponseStatusException e) {
            return HttpServer.Response.text(e.getStatus(), e.getReason() == null ? "refused"
                    : e.getReason());
        }
    }
}
