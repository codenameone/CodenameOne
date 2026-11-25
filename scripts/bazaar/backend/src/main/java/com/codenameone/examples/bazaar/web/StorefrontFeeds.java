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
import com.codenameone.examples.bazaar.content.Pages;
import com.codenameone.examples.bazaar.content.RichText;
import com.codenameone.examples.bazaar.domain.BlogPost;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.Stores;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A store's feeds: its products for shopping engines, per market, and its
/// blog for feed readers.
///
/// **Products** -- `/s/{handle}/feeds/{market}/products.xml` (an RSS 2.0
/// document in the Google Merchant `g:` namespace) and `products.csv` -- list
/// every variant of every active product with its availability and its price
/// in the market's currency, priced the way the storefront prices that
/// market (its rate, adjustment, rounding and fixed prices). `default` is the
/// store's own currency; a market that is unknown or inactive is 404, never
/// silently the store's prices. Both answer an `ETag` of their bytes and are
/// cacheable for a quarter of an hour, so an engine polling them gets a 304.
///
/// **Blog** -- `/s/{handle}/feeds/blog.atom` and `blog.rss`, the 50 newest
/// published posts, summaries only (plain text; the body stays on the site).
///
/// Everything here is text a merchant typed, so every value is escaped and
/// characters XML cannot carry are dropped; a CSV cell that a spreadsheet
/// would read as a formula is quoted with a leading apostrophe.
@RestController
public class StorefrontFeeds {
    static final String CACHE = "public, max-age=900";

    private final Stores stores;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final InventoryLevelRepository levels;
    private final Markets markets;
    private final Pages pages;
    private final SiteUrls urls;
    private final com.codenameone.examples.bazaar.catalog.Schedules schedules;
    private final com.codenameone.examples.bazaar.catalog.Bundles bundles;

    public StorefrontFeeds(Stores stores, ProductRepository products, VariantRepository variants,
            InventoryLevelRepository levels, Markets markets, Pages pages, SiteUrls urls,
            com.codenameone.examples.bazaar.catalog.Schedules schedules,
            com.codenameone.examples.bazaar.catalog.Bundles bundles) {
        this.schedules = schedules;
        this.bundles = bundles;
        this.stores = stores;
        this.products = products;
        this.variants = variants;
        this.levels = levels;
        this.markets = markets;
        this.pages = pages;
        this.urls = urls;
    }

    /// One row of a product feed.
    static final class Row {
        String id;
        String groupId;
        String title;
        String description;
        String link;
        String image;
        String availability;
        long priceCents;
        String currency;
        String brand;
        String gtin;
        String sku;
        /// The Google product taxonomy id, 0 for none.
        int category;
        /// `750ml` and `1l`, or empty when the variant is not sold by measure.
        String unitMeasure = "";
        String unitBase = "";
    }

    /// A unit as Google's unit pricing attributes spell it.
    static String feedUnit(String unit) {
        return "m2".equals(unit) ? "sqm" : unit;
    }

    @GetMapping("/s/{handle}/feeds/{market}/products.xml")
    @Transactional(readOnly = true)
    public HttpServer.Response productsXml(@PathVariable("handle") String handle,
            @PathVariable("market") String market, HttpServer.Request request)
            throws IOException {
        Store store = stores.activeStore(handle);
        List<Row> rows = rows(store, market, request);
        StringBuilder x = new StringBuilder(4096);
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<rss version=\"2.0\" xmlns:g=\"http://base.google.com/ns/1.0\">\n<channel>\n");
        tag(x, "title", store.getName());
        tag(x, "link", urls.absolute(request, StorefrontPages.storeUrl(store.getHandle())));
        tag(x, "description", store.getTagline());
        for (int iter = 0; iter < rows.size(); iter++) {
            Row r = rows.get(iter);
            x.append("<item>");
            tag(x, "g:id", r.id);
            tag(x, "g:item_group_id", r.groupId);
            tag(x, "g:title", r.title);
            tag(x, "g:description", r.description);
            tag(x, "g:link", r.link);
            if (r.image.length() > 0) {
                tag(x, "g:image_link", r.image);
            }
            tag(x, "g:availability", r.availability);
            tag(x, "g:price", Money.plain(r.priceCents, r.currency) + " " + r.currency);
            tag(x, "g:condition", "new");
            if (r.brand.length() > 0) {
                tag(x, "g:brand", r.brand);
            }
            if (r.gtin.length() > 0) {
                tag(x, "g:gtin", r.gtin);
            }
            if (r.sku.length() > 0) {
                tag(x, "g:mpn", r.sku);
            }
            if (r.category > 0) {
                tag(x, "g:google_product_category", Integer.toString(r.category));
            }
            if (r.unitMeasure.length() > 0) {
                tag(x, "g:unit_pricing_measure", r.unitMeasure);
                tag(x, "g:unit_pricing_base_measure", r.unitBase);
            }
            x.append("</item>\n");
        }
        x.append("</channel>\n</rss>\n");
        return Conditional.answer(request, "application/xml; charset=utf-8",
                x.toString().getBytes("UTF-8"), CACHE, 0);
    }

    @GetMapping("/s/{handle}/feeds/{market}/products.csv")
    @Transactional(readOnly = true)
    public HttpServer.Response productsCsv(@PathVariable("handle") String handle,
            @PathVariable("market") String market, HttpServer.Request request)
            throws IOException {
        Store store = stores.activeStore(handle);
        List<Row> rows = rows(store, market, request);
        StringBuilder c = new StringBuilder(4096);
        c.append("id,item_group_id,title,description,link,image_link,availability,price,"
                + "condition,brand,gtin,mpn,google_product_category,unit_pricing_measure,"
                + "unit_pricing_base_measure\r\n");
        for (int iter = 0; iter < rows.size(); iter++) {
            Row r = rows.get(iter);
            cell(c, r.id, false);
            cell(c, r.groupId, false);
            cell(c, r.title, false);
            cell(c, r.description, false);
            cell(c, r.link, false);
            cell(c, r.image, false);
            cell(c, r.availability, false);
            cell(c, Money.plain(r.priceCents, r.currency) + " " + r.currency, false);
            cell(c, "new", false);
            cell(c, r.brand, false);
            cell(c, r.gtin, false);
            cell(c, r.sku, false);
            cell(c, r.category > 0 ? Integer.toString(r.category) : "", false);
            cell(c, r.unitMeasure, false);
            cell(c, r.unitBase, true);
            c.append("\r\n");
        }
        return Conditional.answer(request, "text/csv; charset=utf-8",
                c.toString().getBytes("UTF-8"), CACHE, 0);
    }

    /// The variants of the store's active products, priced for `market`.
    List<Row> rows(Store store, String market, HttpServer.Request request) throws IOException {
        Markets.Pricer pricer = null;
        if (!"default".equals(market)) {
            pricer = market == null || market.length() > 16 ? null
                    : markets.pricer(store, market);
            if (pricer == null) {
                throw new ResponseStatusException(404, "No such market");
            }
        }
        String currency = pricer == null ? store.getCurrency() : pricer.currency();
        List<Product> ps = schedules.visible(store.getId(),
                com.codenameone.examples.bazaar.catalog.Schedules.ONLINE,
                products.findByStoreIdAndStatusOrderByTitleAsc(store.getId(), Catalog.ACTIVE));
        List<String> ids = new ArrayList<String>();
        Map<String, Product> byId = new HashMap<String, Product>();
        for (int iter = 0; iter < ps.size(); iter++) {
            ids.add(ps.get(iter).getId());
            byId.put(ps.get(iter).getId(), ps.get(iter));
        }
        List<Variant> vs = ids.isEmpty() ? new ArrayList<Variant>()
                : variants.findByProductIdIn(ids);
        List<String> vids = new ArrayList<String>();
        for (int iter = 0; iter < vs.size(); iter++) {
            vids.add(vs.get(iter).getId());
        }
        Map<String, Integer> available = new HashMap<String, Integer>();
        List<InventoryLevel> ls = vids.isEmpty() ? new ArrayList<InventoryLevel>()
                : levels.findByVariantIdIn(vids);
        for (int iter = 0; iter < ls.size(); iter++) {
            InventoryLevel l = ls.get(iter);
            Integer was = available.get(l.getVariantId());
            int n = (was == null ? 0 : was.intValue()) + Math.max(0, l.getOnHand() - l.getCommitted());
            available.put(l.getVariantId(), Integer.valueOf(n));
        }
        // A bundle has no stock of its own: it is in stock while its
        // components make one.
        Map<String, List<com.codenameone.examples.bazaar.catalog.Bundles.Part>> bundleParts =
                bundles.byVariant(store.getId());
        for (Map.Entry<String, List<com.codenameone.examples.bazaar.catalog.Bundles.Part>> e
                : bundleParts.entrySet()) {
            available.put(e.getKey(), Integer.valueOf(bundles.available(e.getValue(), null)));
        }
        List<Row> out = new ArrayList<Row>();
        // Products in title order, each product's variants in their own order.
        for (int p = 0; p < ps.size(); p++) {
            Product product = ps.get(p);
            for (int iter = 0; iter < vs.size(); iter++) {
                Variant v = vs.get(iter);
                if (!v.getProductId().equals(product.getId())) {
                    continue;
                }
                Row r = new Row();
                r.id = v.getSku().length() > 0 ? v.getSku() : v.getId();
                r.groupId = product.getId();
                String vt = v.getTitle();
                r.title = vt.length() == 0 || "Default".equals(vt) || "Default Title".equals(vt)
                        ? product.getTitle() : product.getTitle() + " - " + vt;
                r.description = clip(RichText.collapse(product.getDescription()).trim(), 5000);
                r.link = urls.absolute(request, StorefrontPages.productUrl(store.getHandle(),
                        product.getHandle()) + "?variant=" + SiteUrls.encode(v.getId())
                        + (pricer == null ? "" : "&market=" + SiteUrls.encode(market)));
                r.image = product.getPhotoId().length() > 0
                        ? urls.absolute(request, "/media/" + product.getPhotoId()) : "";
                Integer n = available.get(v.getId());
                boolean inStock = bundleParts.containsKey(v.getId()) ? n != null && n.intValue() > 0
                        : !v.isTrackInventory() || v.isAllowBackorder()
                        || n != null && n.intValue() > 0;
                r.availability = inStock ? "in_stock" : "out_of_stock";
                r.priceCents = pricer == null ? v.getPriceCents()
                        : pricer.price(v.getId(), v.getPriceCents());
                r.currency = currency;
                r.brand = product.getVendor().length() > 0 ? product.getVendor() : store.getName();
                r.gtin = v.getBarcode();
                r.sku = v.getSku();
                r.category = product.getCategoryId();
                if (com.codenameone.examples.bazaar.ops.UnitPricing.unitPriceCents(r.priceCents,
                        v.getUnitQuantity(), v.getUnitName(), v.getUnitReference(),
                        v.getUnitReferenceName()) >= 0) {
                    r.unitMeasure = com.codenameone.examples.bazaar.ops.UnitPricing.quantity(
                            v.getUnitQuantity()) + feedUnit(v.getUnitName());
                    r.unitBase = v.getUnitReference() + feedUnit(v.getUnitReferenceName());
                }
                out.add(r);
            }
        }
        return out;
    }

    @GetMapping("/s/{handle}/feeds/blog.atom")
    @Transactional(readOnly = true)
    public HttpServer.Response atom(@PathVariable("handle") String handle,
            HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        List<BlogPost> ps = pages.publishedPosts(store.getId(), null);
        String blog = urls.absolute(request, StorefrontPages.storeUrl(store.getHandle()) + "/blog");
        StringBuilder x = new StringBuilder(2048);
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");
        tag(x, "title", store.getName() + " blog");
        tag(x, "id", blog);
        x.append("<link rel=\"alternate\" href=\"");
        text(x, blog);
        x.append("\"/>\n<link rel=\"self\" href=\"");
        text(x, urls.absolute(request, StorefrontPages.storeUrl(store.getHandle())
                + "/feeds/blog.atom"));
        x.append("\"/>\n");
        Instant newest = ps.isEmpty() ? store.getUpdatedAt() : updated(ps.get(0));
        tag(x, "updated", iso(newest));
        long newestMillis = newest == null ? 0 : newest.toEpochMilli();
        for (int iter = 0; iter < ps.size() && iter < 50; iter++) {
            BlogPost p = ps.get(iter);
            String link = blog + "/" + SiteUrls.encode(p.getHandle());
            x.append("<entry>");
            tag(x, "title", p.getTitle());
            tag(x, "id", link);
            x.append("<link rel=\"alternate\" href=\"");
            text(x, link);
            x.append("\"/>");
            tag(x, "published", iso(p.getPublishedAt()));
            tag(x, "updated", iso(updated(p)));
            newestMillis = Math.max(newestMillis, updated(p) == null ? 0 : updated(p).toEpochMilli());
            x.append("<author>");
            tag(x, "name", p.getAuthor().length() > 0 ? p.getAuthor() : store.getName());
            x.append("</author>");
            List<String> tags = com.codenameone.examples.bazaar.content.Lists.split(p.getTags());
            for (int t = 0; t < tags.size(); t++) {
                x.append("<category term=\"");
                text(x, tags.get(t));
                x.append("\"/>");
            }
            tag(x, "summary", p.getExcerpt());
            x.append("</entry>\n");
        }
        x.append("</feed>\n");
        return Conditional.answer(request, "application/atom+xml; charset=utf-8",
                x.toString().getBytes("UTF-8"), CACHE, newestMillis);
    }

    @GetMapping("/s/{handle}/feeds/blog.rss")
    @Transactional(readOnly = true)
    public HttpServer.Response rss(@PathVariable("handle") String handle,
            HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        List<BlogPost> ps = pages.publishedPosts(store.getId(), null);
        String blog = urls.absolute(request, StorefrontPages.storeUrl(store.getHandle()) + "/blog");
        StringBuilder x = new StringBuilder(2048);
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rss version=\"2.0\">\n<channel>\n");
        tag(x, "title", store.getName() + " blog");
        tag(x, "link", blog);
        tag(x, "description", store.getTagline());
        long newestMillis = 0;
        for (int iter = 0; iter < ps.size() && iter < 50; iter++) {
            BlogPost p = ps.get(iter);
            String link = blog + "/" + SiteUrls.encode(p.getHandle());
            x.append("<item>");
            tag(x, "title", p.getTitle());
            tag(x, "link", link);
            tag(x, "guid", link);
            tag(x, "pubDate", rfc822(p.getPublishedAt()));
            tag(x, "description", p.getExcerpt());
            x.append("</item>\n");
            newestMillis = Math.max(newestMillis, updated(p) == null ? 0 : updated(p).toEpochMilli());
        }
        x.append("</channel>\n</rss>\n");
        return Conditional.answer(request, "application/rss+xml; charset=utf-8",
                x.toString().getBytes("UTF-8"), CACHE, newestMillis);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        if (e.getStatus() == 404) {
            return NotFoundPage.response();
        }
        return HttpServer.Response.text(e.getStatus(), e.getReason() == null ? "" : e.getReason());
    }

    private static Instant updated(BlogPost p) {
        return p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getPublishedAt();
    }

    /// An instant as RFC 3339 to the second: `2026-05-31T10:00:00Z`.
    static String iso(Instant at) {
        String s = at == null ? Instant.EPOCH.toString() : at.toString();
        // Instant.toString drops zero seconds and may carry a fraction.
        String date = s.substring(0, 10);
        String time = s.length() >= 16 ? s.substring(11, 16) : "00:00";
        String seconds = s.length() >= 19 && s.charAt(16) == ':' ? s.substring(17, 19) : "00";
        return date + "T" + time + ":" + seconds + "Z";
    }

    private static final String[] DAYS = {"Thu", "Fri", "Sat", "Sun", "Mon", "Tue", "Wed"};
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
        "Aug", "Sep", "Oct", "Nov", "Dec"};

    /// An instant as RSS wants it: `Sun, 31 May 2026 10:00:00 GMT`.
    static String rfc822(Instant at) {
        String iso = iso(at);
        int y = Integer.parseInt(iso.substring(0, 4));
        int m = Integer.parseInt(iso.substring(5, 7));
        int d = Integer.parseInt(iso.substring(8, 10));
        long day = Math.floorDiv(at == null ? 0 : at.getEpochSecond(), 86400L);
        return DAYS[(int) Math.floorMod(day, 7L)] + ", " + (d < 10 ? "0" : "") + d + " "
                + MONTHS[m - 1] + " " + y + " " + iso.substring(11, 19) + " GMT";
    }

    private static String clip(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static void tag(StringBuilder x, String name, String value) {
        x.append('<').append(name).append('>');
        text(x, value == null ? "" : value);
        x.append("</").append(name).append('>');
    }

    /// Escaped XML text, without the characters XML 1.0 cannot carry.
    static void text(StringBuilder x, String value) {
        for (int iter = 0; iter < value.length(); iter++) {
            char c = value.charAt(iter);
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r' || c == '\ufffe' || c == '\uffff') {
                continue;
            }
            if (Character.isSurrogate(c)) {
                boolean paired = Character.isHighSurrogate(c) && iter + 1 < value.length()
                        && Character.isLowSurrogate(value.charAt(iter + 1));
                if (paired) {
                    x.append(c).append(value.charAt(iter + 1));
                    iter++;
                }
                continue;
            }
            if (c == '&') {
                x.append("&amp;");
            } else if (c == '<') {
                x.append("&lt;");
            } else if (c == '>') {
                x.append("&gt;");
            } else if (c == '"') {
                x.append("&quot;");
            } else {
                x.append(c);
            }
        }
    }

    /// One CSV cell: quoted, quotes doubled, and a leading `=`, `+`, `-`, `@`
    /// (a formula to a spreadsheet) defused with an apostrophe.
    static void cell(StringBuilder c, String value, boolean last) {
        String v = value == null ? "" : value;
        c.append('"');
        if (v.length() > 0 && (v.charAt(0) == '=' || v.charAt(0) == '+' || v.charAt(0) == '-'
                || v.charAt(0) == '@' || v.charAt(0) == '\t' || v.charAt(0) == '\r')) {
            c.append('\'');
        }
        for (int iter = 0; iter < v.length(); iter++) {
            char ch = v.charAt(iter);
            if (ch == '"') {
                c.append("\"\"");
            } else if (ch == '\r' || ch == '\n') {
                c.append(' ');
            } else {
                c.append(ch);
            }
        }
        c.append('"');
        if (!last) {
            c.append(',');
        }
    }
}
