/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.pay;

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
                + "<title>Wayline</title><style>body{font-family:-apple-system,system-ui,"
                + "sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;"
                + "justify-content:center;background:#f6f7f9;color:#14161a}"
                + "main{max-width:22rem;padding:2rem;text-align:center}"
                + "h1{font-size:1.4rem}p{color:#5b6270;line-height:1.5}</style></head>"
                + "<body><main><h1>" + (done ? "Card added" : "No card was added") + "</h1><p>"
                + (done ? "You can close this page and go back to Wayline to finish."
                        : "You can close this page and go back to Wayline to try again.")
                + "</p></main></body></html>";
        return new HttpServer.Response(200, "text/html; charset=utf-8", html.getBytes("UTF-8"));
    }
}
