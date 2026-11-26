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

package com.codenameone.examples.bazaar.ai;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codenameone.examples.bazaar.cart.Codes;

/// `POST /api/merchant/stores/{storeId}/ai/v1/chat/completions`: the merchant
/// AI ([MerchantAi]) in the shape `LlmClient.localOpenAiCompatible` speaks.
/// Signed-in merchants only (`/api/merchant/**`), and the store's staff only.
@Controller
public class MerchantAiController {
    /// The route, as the tests name it.
    public static final String PATH = "/api/merchant/stores/{storeId}/ai/v1/chat/completions";

    private final MerchantAi ai;

    public MerchantAiController(MerchantAi ai) {
        this.ai = ai;
    }

    @PostMapping(PATH)
    public HttpServer.Response complete(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        String who = Codes.callerOrNull();
        if (who == null) {
            return HttpServer.Response.json(401, error("Sign in first"));
        }
        try {
            return HttpServer.Response.json(200, ai.complete(who, storeId,
                    request.getBodyBytes()));
        } catch (ResponseStatusException e) {
            return HttpServer.Response.json(e.getStatus(), error(e.getReason() == null
                    ? "refused" : e.getReason()));
        }
    }

    private static String error(String message) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        java.util.Map<String, Object> e = new java.util.LinkedHashMap<String, Object>();
        e.put("message", message);
        m.put("error", e);
        return com.codename1.backend.Json.write(m);
    }
}
