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
package com.codenameone.examples.bazaar.platform;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.api.AppListingDto;
import com.codenameone.examples.bazaar.api.AppReviewDto;
import com.codenameone.examples.bazaar.ops.AppStoreText;
import com.codenameone.examples.bazaar.web.SiteUrls;

import java.util.List;

/// The public app directory, server-rendered for search engines and for
/// anyone without the client: `GET /apps?q=&category=&page=` and
/// `GET /apps/{handle}`. Approved listings only; everything a developer
/// wrote is escaped, and each page has its own title and description.
@RestController
public class AppDirectoryPages {
    private final AppDirectory directory;

    public AppDirectoryPages(AppDirectory directory) {
        this.directory = directory;
    }

    @GetMapping("/apps")
    public HttpServer.Response index(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", required = false) String page) throws Exception {
        int p = 0;
        try {
            p = page == null || page.length() == 0 ? 0 : Math.max(0, Integer.parseInt(page));
        } catch (NumberFormatException wrong) {
            p = 0;
        }
        String cat = category != null && AppStoreText.CATEGORIES.contains(category)
                ? category : "";
        List<AppListingDto> found = directory.search(q, cat, p);
        StringBuilder b = new StringBuilder();
        b.append("<form method=\"get\" action=\"/apps\"><input type=\"search\" name=\"q\" value=\"")
                .append(AppConsentPage.esc(q)).append("\" aria-label=\"Search apps\"> ")
                .append("<select name=\"category\" aria-label=\"Category\"><option value=\"\">")
                .append("All categories</option>");
        for (int iter = 0; iter < AppStoreText.CATEGORIES.size(); iter++) {
            String c = AppStoreText.CATEGORIES.get(iter);
            b.append("<option value=\"").append(c).append('"')
                    .append(c.equals(cat) ? " selected" : "").append('>').append(c)
                    .append("</option>");
        }
        b.append("</select> <button type=\"submit\">Search</button></form><ul class=\"apps\">");
        for (int iter = 0; iter < found.size(); iter++) {
            AppListingDto l = found.get(iter);
            b.append("<li><a href=\"/apps/").append(SiteUrls.encode(l.handle())).append("\">")
                    .append(AppConsentPage.esc(l.name())).append("</a> ")
                    .append(AppConsentPage.esc(l.tagline()));
            if (l.reviews() > 0) {
                b.append(" <span>").append(AppStoreText.rating(l.ratingTenths(), l.reviews()))
                        .append(" (").append(l.reviews()).append(")</span>");
            }
            b.append("</li>");
        }
        if (found.isEmpty()) {
            b.append("<li>No apps match.</li>");
        }
        b.append("</ul>");
        if (found.size() == AppDirectory.PAGE) {
            b.append("<p><a href=\"/apps?page=").append(p + 1).append("&amp;category=")
                    .append(SiteUrls.encode(cat)).append("&amp;q=")
                    .append(SiteUrls.encode(q == null ? "" : q)).append("\">More apps</a></p>");
        }
        return html(200, "App directory", "Apps that add to a Bazaar store: marketing, "
                + "shipping, inventory and more.", b.toString(), true);
    }

    @GetMapping("/apps/{handle}")
    public HttpServer.Response listing(@PathVariable("handle") String handle) throws Exception {
        AppListingDto l;
        List<AppReviewDto> reviews;
        try {
            l = directory.listing(handle);
            reviews = directory.reviews(handle);
        } catch (ResponseStatusException missing) {
            return html(404, "No such app", "", "<p>There is no such app in the directory.</p>"
                    + "<p><a href=\"/apps\">All apps</a></p>", false);
        }
        StringBuilder b = new StringBuilder();
        if (l.iconUrl().length() > 0) {
            b.append("<img src=\"").append(AppConsentPage.esc(l.iconUrl()))
                    .append("\" alt=\"\" width=\"64\" height=\"64\">");
        }
        b.append("<p><strong>").append(AppConsentPage.esc(l.tagline())).append("</strong></p>")
                .append("<p>").append(AppConsentPage.esc(l.description())).append("</p>")
                .append("<p>Category: ").append(AppConsentPage.esc(l.category())).append("</p>");
        if (l.pricing().length() > 0) {
            b.append("<p>Pricing: ").append(AppConsentPage.esc(l.pricing())).append("</p>");
        }
        if (l.reviews() > 0) {
            b.append("<p>Rated ").append(AppStoreText.rating(l.ratingTenths(), l.reviews()))
                    .append(" of 5 by ").append(l.reviews())
                    .append(l.reviews() == 1 ? " store" : " stores").append("</p>");
        }
        for (int iter = 0; iter < l.screenshots().size(); iter++) {
            b.append("<img src=\"").append(AppConsentPage.esc(l.screenshots().get(iter)))
                    .append("\" alt=\"Screenshot ").append(iter + 1)
                    .append("\" loading=\"lazy\">");
        }
        b.append("<p>");
        link(b, l.supportUrl(), "Support");
        link(b, l.privacyUrl(), "Privacy policy");
        b.append("</p><p>Install it from the Apps screen of your store.</p>");
        if (!reviews.isEmpty()) {
            b.append("<h2>Reviews</h2><ul>");
            for (int iter = 0; iter < reviews.size(); iter++) {
                AppReviewDto r = reviews.get(iter);
                b.append("<li>").append(AppStoreText.stars(r.rating() * 10)).append(' ')
                        .append(AppConsentPage.esc(r.storeName())).append(": ")
                        .append(AppConsentPage.esc(r.body())).append("</li>");
            }
            b.append("</ul>");
        }
        return html(200, l.name(), l.tagline(), b.toString(), true);
    }

    private static void link(StringBuilder b, String url, String text) {
        if (url.length() > 0) {
            b.append("<a rel=\"nofollow noopener\" href=\"").append(AppConsentPage.esc(url))
                    .append("\">").append(text).append("</a> ");
        }
    }

    /// A whole page; `index` lets search engines list it.
    static HttpServer.Response html(int status, String title, String description, String body,
            boolean index) throws java.io.IOException {
        String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + (index ? "" : "<meta name=\"robots\" content=\"noindex\">")
                + (description.length() > 0 ? "<meta name=\"description\" content=\""
                        + AppConsentPage.esc(description) + "\">" : "")
                + "<title>" + AppConsentPage.esc(title) + (index ? " - Bazaar apps" : "")
                + "</title><link rel=\"stylesheet\" href=\""
                + com.codenameone.examples.bazaar.web.ErrorPages.CSS
                + "\"></head><body><main class=\"narrow\"><h1>" + AppConsentPage.esc(title)
                + "</h1>" + body + "</main></body></html>";
        return new HttpServer.Response(status, "text/html; charset=utf-8",
                html.getBytes("UTF-8")).header("Cache-Control", index ? "public, max-age=300"
                        : "no-store").header("X-Frame-Options", "DENY");
    }
}
