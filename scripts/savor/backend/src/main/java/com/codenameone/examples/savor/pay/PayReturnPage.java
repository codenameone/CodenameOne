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
package com.codenameone.examples.savor.pay;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;

/// The page a hosted card setup ends on.
///
/// The payment processor's page sends the browser here when the user has added
/// a card or given up. There is nothing to do here but say so: the app is what
/// finishes the setup, by asking the server to read it back, once the user
/// returns to it. That is also why the page needs no sign-in and takes nothing
/// from its address but which of two sentences to show -- being sent here
/// proves nothing and changes nothing.
@RestController
public class PayReturnPage {
    @GetMapping("/pay/return")
    public HttpServer.Response page(
            @RequestParam(value = "result", required = false) String result) throws Exception {
        boolean done = "done".equals(result);
        String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>Savor</title><style>body{font-family:-apple-system,system-ui,"
                + "sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;"
                + "justify-content:center;background:#f6f7f9;color:#14161a}"
                + "main{max-width:22rem;padding:2rem;text-align:center}"
                + "h1{font-size:1.4rem}p{color:#5b6270;line-height:1.5}</style></head>"
                + "<body><main><h1>" + (done ? "Card added" : "No card was added") + "</h1><p>"
                + (done ? "You can close this page and go back to Savor to finish."
                        : "You can close this page and go back to Savor to try again.")
                + "</p></main></body></html>";
        return new HttpServer.Response(200, "text/html; charset=utf-8", html.getBytes("UTF-8"));
    }
}
