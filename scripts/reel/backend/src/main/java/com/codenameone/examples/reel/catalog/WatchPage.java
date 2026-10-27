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
package com.codenameone.examples.reel.catalog;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.api.Advisories;
import com.codenameone.examples.reel.api.BrandingDto;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.playback.MediaOrigin;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.settings.Settings;

import java.util.List;

/// What a shared link opens: `/watch/{id}`, a public page with the title's
/// poster, synopsis and classification, and a button that opens it in the app.
///
/// The app shares this address and not an `/api` one because whoever receives
/// it may have no account and no app; a page is what every messenger can
/// preview (the `og:` tags) and every browser can open. Only a published title
/// has one -- a draft, a title under review and a removed one answer 404, so a
/// link shared before a takedown stops showing it.
@RestController
public class WatchPage {
    private final TitleRepository titles;
    private final Settings settings;

    public WatchPage(TitleRepository titles, Settings settings) {
        this.titles = titles;
        this.settings = settings;
    }

    /// The path of a title's page, which the app resolves against its server.
    public static String path(String titleId) {
        return "/watch/" + titleId;
    }

    @Transactional(readOnly = true)
    @GetMapping("/watch/{id}")
    public HttpServer.Response page(@PathVariable("id") String id) throws Exception {
        Title t = MediaOrigin.safeId(id) ? titles.findById(id) : null;
        BrandingDto brand = settings.branding();
        String service = escape(brand.serviceName());
        if (t == null || !Kinds.PUBLISHED.equals(t.getStatus())) {
            return html(404, service, color(brand.primaryColor()), service, "",
                    "<main><h1>Not available</h1><p>This title is not on " + service + ".</p></main>");
        }
        String name = escape(t.getName());
        String synopsis = escape(t.getSynopsis());
        String poster = escape(t.getPosterUrl());
        StringBuilder facts = new StringBuilder();
        facts.append("<span class=\"rating\">").append(escape(t.getRating())).append("</span>");
        if (t.getYear() > 0) {
            facts.append("<span>").append(t.getYear()).append("</span>");
        }
        List<String> advisories = Rows.split(t.getAdvisories());
        StringBuilder why = new StringBuilder();
        for (int iter = 0; iter < advisories.size(); iter++) {
            String label = Advisories.label(advisories.get(iter));
            if (label != null) {
                why.append(why.length() == 0 ? "" : ", ").append(label);
            }
        }
        String body = "<main><img src=\"" + poster + "\" alt=\"\"><div><p class=\"by\">" + service + "</p><h1>"
                + name + "</h1><p class=\"facts\">" + facts + "</p>"
                + (why.length() == 0 ? "" : "<p class=\"why\">" + escape(why.toString()) + "</p>")
                + "<p>" + synopsis + "</p><a class=\"open\" href=\"/?title=" + escape(t.getId()) + "\">Watch on "
                + service + "</a></div></main>";
        return html(200, name + " - " + service, color(brand.primaryColor()), synopsis, poster, body);
    }

    /// `value` when it is a `#rrggbb` colour, else the default red: branding is
    /// typed by an admin and lands inside a style sheet.
    static String color(String value) {
        if (value == null || value.length() != 7 || value.charAt(0) != '#') {
            return "#e50914";
        }
        for (int iter = 1; iter < 7; iter++) {
            char c = value.charAt(iter);
            if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {
                return "#e50914";
            }
        }
        return value;
    }

    private static HttpServer.Response html(int status, String title, String brand, String description,
            String image, String body) throws Exception {
        String page = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>" + title
                + "</title><meta property=\"og:title\" content=\"" + title + "\">"
                + "<meta property=\"og:description\" content=\"" + description + "\">"
                + (image.length() == 0 ? "" : "<meta property=\"og:image\" content=\"" + image + "\">")
                + "<style>body{font-family:system-ui,sans-serif;margin:0;background:#0b0b0f;color:#f2f2f5;"
                + "min-height:100vh;display:flex;align-items:center;justify-content:center}main{display:flex;"
                + "gap:2rem;max-width:56rem;margin:1rem;padding:2rem;align-items:flex-start;flex-wrap:wrap}"
                + "img{width:16rem;max-width:100%;border-radius:12px}main div{flex:1;min-width:16rem}"
                + "h1{font-size:2.2rem;margin:.3rem 0}.by{color:" + brand + ";font-weight:700;letter-spacing:.1em;"
                + "text-transform:uppercase;margin:0}.facts span{margin-right:.8rem;color:#b9b9c4}"
                + ".facts .rating{border:1px solid #b9b9c4;padding:0 .4rem;border-radius:4px}.why{color:#b9b9c4}"
                + "p{line-height:1.5}.open{display:inline-block;margin-top:1rem;background:" + brand + ";color:#fff;"
                + "padding:.8rem 1.6rem;border-radius:8px;text-decoration:none;font-weight:700}</style></head><body>"
                + body + "</body></html>";
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
