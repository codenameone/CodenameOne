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
package com.codenameone.examples.reel.learning;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.reel.api.CertificateDto;
import com.codenameone.examples.reel.settings.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;

/// The printable certificate page at `/verify/{code}`: what the code on a
/// certificate leads to, and what the app opens to print or share one. No
/// sign-in: the code is the credential, and it proves only that the named
/// person passed the named course.
@RestController
public class CertificatePage {
    private final Learning learning;
    private final Settings settings;

    public CertificatePage(Learning learning, Settings settings) {
        this.learning = learning;
        this.settings = settings;
    }

    @GetMapping("/verify/{code}")
    public HttpServer.Response page(@PathVariable("code") String code) throws Exception {
        CertificateDto c;
        try {
            c = learning.verify(code);
        } catch (ResponseStatusException missing) {
            return html(404, "<h1>Not found</h1><p>No certificate has the code " + escape(code) + ".</p>");
        }
        String service = escape(settings.branding().serviceName());
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(c.issuedAt()));
        return html(200, "<div class=\"cert\"><p class=\"by\">" + service + " certifies that</p><h1>"
                + escape(c.holderName()) + "</h1><p>completed</p><h2>" + escape(c.titleName()) + "</h2><p>with a score of "
                + c.scorePercent() + "% on " + date + "</p><p class=\"code\">Verification code "
                + escape(c.verifyCode()) + "</p></div>");
    }

    private static HttpServer.Response html(int status, String body) throws Exception {
        String page = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Certificate</title>"
                + "<style>body{font-family:Georgia,serif;margin:0;background:#f4f1ea;color:#1d1b16;display:flex;"
                + "min-height:100vh;align-items:center;justify-content:center}.cert{background:#fff;max-width:40rem;"
                + "margin:1rem;padding:3rem 2rem;text-align:center;border:6px double #b08d3c}h1{font-size:2.2rem;"
                + "margin:.5rem 0}h2{font-size:1.4rem;margin:.5rem 0}.by,.code{color:#6b6250}.code{font-family:"
                + "monospace;margin-top:2rem}@media print{body{background:#fff}}</style></head><body>" + body
                + "</body></html>";
        return new HttpServer.Response(status, "text/html; charset=utf-8", page.getBytes("UTF-8"));
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char ch = s.charAt(iter);
            switch (ch) {
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '&': out.append("&amp;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(ch);
            }
        }
        return out.toString();
    }
}
