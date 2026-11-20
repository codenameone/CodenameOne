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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.ExceptionHandler;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.Collection;
import com.codenameone.examples.bazaar.domain.Photo;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.CollectionRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.Stores;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// What crawlers read beside the pages -- `/robots.txt` and `/sitemap.xml` --
/// and the pictures the pages show, at `/media/{photoId}`.
@RestController
public class StorefrontFiles {
    private final StoreRepository stores;
    private final ProductRepository products;
    private final CollectionRepository collections;
    private final Catalog catalog;
    private final SiteUrls urls;

    public StorefrontFiles(StoreRepository stores, ProductRepository products,
            CollectionRepository collections, Catalog catalog, SiteUrls urls) {
        this.stores = stores;
        this.products = products;
        this.collections = collections;
        this.catalog = catalog;
        this.urls = urls;
    }

    /// The API, the sign-in pages and the per-visitor pages stay out of
    /// indexes; everything else is welcome, and the sitemap says what there is.
    @GetMapping("/robots.txt")
    public HttpServer.Response robots(HttpServer.Request request) throws IOException {
        String body = "User-agent: *\n"
                + "Disallow: /api/\n"
                + "Disallow: /oauth2/\n"
                + "Disallow: /login\n"
                + "Disallow: /pay/\n"
                + "Disallow: /track/\n"
                + "Disallow: /s/*/cart\n"
                + "Disallow: /s/*/search\n"
                + "Allow: /\n"
                + "\n"
                + "Sitemap: " + urls.absolute(request, "/sitemap.xml") + "\n";
        return Conditional.answer(request, "text/plain; charset=utf-8", body.getBytes("UTF-8"),
                "public, max-age=3600", 0);
    }

    /// Every active store, its published collections and its active
    /// products, with when each last changed. Drafts, archived products and
    /// suspended or pending stores are not listed.
    @GetMapping("/sitemap.xml")
    @Transactional(readOnly = true)
    public HttpServer.Response sitemap(HttpServer.Request request) throws IOException {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        long newest = 0;
        List<Store> active = stores.findByStatusOrderByNameAsc(Stores.ACTIVE);
        for (int s = 0; s < active.size(); s++) {
            Store store = active.get(s);
            List<Product> ps = products.findByStoreIdAndStatusOrderByTitleAsc(store.getId(),
                    Catalog.ACTIVE);
            Instant storeChanged = store.getUpdatedAt();
            for (int p = 0; p < ps.size(); p++) {
                storeChanged = later(storeChanged, ps.get(p).getUpdatedAt());
            }
            newest = Math.max(newest, millis(storeChanged));
            url(xml, urls.absolute(request, StorefrontPages.storeUrl(store.getHandle())),
                    storeChanged);
            List<Collection> cs = collections.findByStoreIdOrderByPositionAsc(store.getId());
            for (int c = 0; c < cs.size(); c++) {
                if (cs.get(c).isPublished()) {
                    url(xml, urls.absolute(request, StorefrontPages.collectionUrl(
                            store.getHandle(), cs.get(c).getHandle())), storeChanged);
                }
            }
            for (int p = 0; p < ps.size(); p++) {
                url(xml, urls.absolute(request, StorefrontPages.productUrl(store.getHandle(),
                        ps.get(p).getHandle())), ps.get(p).getUpdatedAt());
            }
        }
        xml.append("</urlset>\n");
        return Conditional.answer(request, "application/xml; charset=utf-8",
                xml.toString().getBytes("UTF-8"), "public, max-age=900", newest);
    }

    /// A stored picture of an active store. Photos are never changed in
    /// place -- a new picture is a new id -- so a browser keeps one for a year.
    @GetMapping("/media/{photoId}")
    public HttpServer.Response media(@PathVariable("photoId") String photoId,
            HttpServer.Request request) throws IOException {
        Photo photo = catalog.publicPhoto(photoId);
        String type = photo.getContentType();
        if (type == null || !type.startsWith("image/")) {
            type = "application/octet-stream";
        }
        String tag = "\"" + photo.getId() + "\"";
        if (tag.equals(request.getHeader("If-None-Match"))) {
            return new HttpServer.Response(304, type, new byte[0])
                    .header("ETag", tag)
                    .header("Cache-Control", "public, max-age=31536000, immutable");
        }
        return new HttpServer.Response(200, type, photo.getData())
                .header("ETag", tag)
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        if (e.getStatus() == 404) {
            return NotFoundPage.response();
        }
        return HttpServer.Response.text(e.getStatus(), e.getReason() == null ? "" : e.getReason());
    }

    private static void url(StringBuilder xml, String loc, Instant changed) {
        xml.append("  <url><loc>");
        escape(xml, loc);
        xml.append("</loc>");
        if (changed != null && changed.toEpochMilli() > 0) {
            xml.append("<lastmod>").append(StorefrontPages.day(changed)).append("</lastmod>");
        }
        xml.append("</url>\n");
    }

    private static void escape(StringBuilder xml, String text) {
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c == '&') {
                xml.append("&amp;");
            } else if (c == '<') {
                xml.append("&lt;");
            } else if (c == '>') {
                xml.append("&gt;");
            } else if (c == '"') {
                xml.append("&quot;");
            } else if (c == '\'') {
                xml.append("&apos;");
            } else {
                xml.append(c);
            }
        }
    }

    private static Instant later(Instant a, Instant b) {
        return a == null ? b : b == null || !b.isAfter(a) ? a : b;
    }

    private static long millis(Instant at) {
        return at == null ? 0 : at.toEpochMilli();
    }
}
