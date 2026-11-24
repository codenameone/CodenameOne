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
package com.codenameone.examples.bazaar.web.slots;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bazaar.web.security.CspSources;

import java.io.IOException;

/// The storefront routes of app slots ([Slots]). A merchant manages their
/// apps in the Bazaar app (`/app/`), through the REST API.
///
/// - `/w/{id}`: a widget's HTML, for the sandboxed frame a page prints. Its
///   own chain keeps it stateless (no session is read or made, no cookie is
///   set), and its policy carries `sandbox allow-scripts`, so even opened as
///   a page of its own it runs in an opaque origin with no access to the
///   storefront's cookies or storage.
/// - `POST /consent`: records the visitor's cookie choice and goes back to
///   the page the banner was on. Works with no script at all.
@Controller
public class SlotPages {
    private final Slots slots;
    private final boolean secureCookies;

    public SlotPages(Slots slots, @Value("${bazaar.web.hsts:true}") boolean secureCookies) {
        this.slots = slots;
        this.secureCookies = secureCookies;
    }

    private static String param(HttpServer.Request request, String name) {
        String v = request.param(name);
        return v == null ? "" : v;
    }


    // --------------------------------------------------------------- widget

    /// A widget's HTML as a document of its own, for `<iframe sandbox>`.
    @GetMapping("/w/{id}")
    public HttpServer.Response widget(@PathVariable("id") String id) throws IOException {
        AppSlot s = slots.widget(id);
        if (s == null) {
            return new HttpServer.Response(404, "text/plain; charset=utf-8",
                    "Not found".getBytes("UTF-8")).header("Cache-Control", "no-store");
        }
        String doc = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<meta name=\"robots\" content=\"noindex\"></head><body>" + s.widgetHtml
                + "</body></html>";
        return new HttpServer.Response(200, "text/html; charset=utf-8", doc.getBytes("UTF-8"))
                .header("Content-Security-Policy", widgetPolicy(s.origins))
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .header("Cache-Control", "public, max-age=60");
    }

    /// The policy of a widget document. The `sandbox` directive is what makes
    /// it safe: the document runs in an opaque origin whether it is framed
    /// with the `sandbox` attribute or opened directly, so its inline script
    /// (the point of a widget) can reach neither the storefront's cookies nor
    /// its storage nor its pages. Beyond its inline code it may load only from
    /// the hosts its owner allowed, may be framed only by this site's pages,
    /// and may not submit forms or set a `<base>`.
    static String widgetPolicy(String origins) {
        StringBuilder hosts = new StringBuilder();
        int start = 0;
        while (start < origins.length()) {
            int sp = origins.indexOf(' ', start);
            int end = sp < 0 ? origins.length() : sp;
            String one = CspSources.clean(origins.substring(start, end));
            if (one != null) {
                hosts.append(' ').append(one);
            }
            start = end + 1;
        }
        String h = hosts.toString();
        return "sandbox allow-scripts; default-src 'none'; script-src 'unsafe-inline'" + h
                + "; style-src 'unsafe-inline'" + h + "; img-src data:" + h + "; connect-src"
                + (h.length() == 0 ? " 'none'" : h) + "; font-src" + (h.length() == 0 ? " 'none'" : h)
                + "; frame-ancestors 'self'; base-uri 'none'; form-action 'none'";
    }

    // -------------------------------------------------------------- consent

    /// Records the visitor's choice: `choice` is `all`, `none` or `custom`
    /// (with `analytics` and `marketing` checkboxes), and `back` the page to
    /// return to -- a path on this site, or the front page.
    @PostMapping("/consent")
    public HttpServer.Response consent(HttpServer.Request request) {
        com.codenameone.examples.bazaar.web.Storefront.sameSite(request);
        String choice = param(request, "choice");
        boolean a;
        boolean m;
        if ("all".equals(choice)) {
            a = true;
            m = true;
        } else if ("custom".equals(choice)) {
            a = "1".equals(param(request, "analytics"));
            m = "1".equals(param(request, "marketing"));
        } else {
            a = false;
            m = false;
        }
        Consent c = new Consent(true, a, m);
        return HttpServer.Response.text(303, "")
                .header("Location", back(param(request, "back")))
                .header("Set-Cookie", c.setCookie(secureCookies))
                .header("Cache-Control", "no-store");
    }

    /// `back` when it is a path on this site; `/` for anything else, so the
    /// form is not an open redirect.
    static String back(String back) {
        if (back == null || back.length() == 0 || back.length() > 500 || back.charAt(0) != '/'
                || (back.length() > 1 && (back.charAt(1) == '/' || back.charAt(1) == '\\'))) {
            return "/";
        }
        for (int iter = 0; iter < back.length(); iter++) {
            char c = back.charAt(iter);
            if (c <= ' ' || c >= 127 || c == '\\') {
                return "/";
            }
        }
        return back;
    }
}
