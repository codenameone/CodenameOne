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
package com.codenameone.examples.bazaar.staff;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.SecurityConfig;

/// Where the app's sign-in lands when the account has a second factor. The
/// app has no page to show, so this answers what the app needs to know: a
/// code is wanted, and whether the last one was wrong. The installed app
/// sees the redirect here; the app in a browser follows it and reads this.
@RestController
public class SecondFactorPage {
    @GetMapping(SecurityConfig.SECOND_FACTOR)
    public HttpServer.Response page(HttpServer.Request request) throws Exception {
        String target = request.getTarget();
        boolean wrong = target != null && target.indexOf("?error") >= 0;
        return new HttpServer.Response(401, "application/json", ("{\"signedIn\":false,"
                + "\"secondFactor\":\"required\"" + (wrong ? ",\"error\":\"code\"" : "") + "}")
                .getBytes("UTF-8")).header("Cache-Control", "no-store");
    }
}
