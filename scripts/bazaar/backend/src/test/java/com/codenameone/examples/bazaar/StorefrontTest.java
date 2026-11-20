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
package com.codenameone.examples.bazaar;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The server-rendered storefront: pages, structured data, the sitemap, the
/// session cart and the caching headers.
@BackendTest
class StorefrontTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private MockResponse fetch(String path) throws Exception {
        return mvc.perform(get(path)).andReturn().getResponse();
    }

    private static String attribute(String html, String name) {
        int at = html.indexOf(name + "=\"");
        if (at < 0) {
            return null;
        }
        int start = at + name.length() + 2;
        return html.substring(start, html.indexOf('"', start)).replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    @Test
    void thePagesRender() throws Exception {
        Shop s = shop("sf-pages");
        String[] paths = {"/", "/s/sf-pages", "/s/sf-pages/search?q=mug",
            "/s/sf-pages/products/sf-pages-mug", "/s/sf-pages/cart"};
        for (int iter = 0; iter < paths.length; iter++) {
            MockResponse r = fetch(paths[iter]);
            assertEquals(200, r.getStatus(), paths[iter]);
            assertTrue(r.getContentType().startsWith("text/html"), paths[iter]);
            assertTrue(r.getContentAsString().contains("/static/storefront.css"), paths[iter]);
        }
        String search = fetch("/s/sf-pages/search?q=mug").getContentAsString();
        assertTrue(search.contains("sf-pages-mug"));
        assertTrue(search.contains("name=\"robots\" content=\"noindex\""));
        assertNotNull(s.productId);
    }

    @Test
    void theProductPageCarriesStructuredDataAndItsCanonical() throws Exception {
        shop("sf-ld");
        MockResponse r = fetch("/s/sf-ld/products/sf-ld-mug?variant=x");
        String html = r.getContentAsString();
        Map ld = Json.parseObject(attribute(html, "data-ld"));
        assertEquals("Product", ld.get("@type"));
        assertEquals("Mug", ld.get("name"));
        Map offer = (Map) ld.get("offers");
        assertEquals("Offer", offer.get("@type"));
        assertEquals("USD", offer.get("priceCurrency"));
        String canonical = r.getHeader("Link");
        assertTrue(canonical.endsWith("/s/sf-ld/products/sf-ld-mug>; rel=\"canonical\""),
                canonical);
        assertTrue(html.contains("property=\"og:url\""));
        assertTrue(attribute(html, "property=\"og:url\" content").endsWith("/s/sf-ld/products/sf-ld-mug"));
        assertTrue(html.contains("itemtype=\"https://schema.org/Product\""));
        // No photo: the placeholder is drawn inline.
        assertTrue(html.contains("<svg"));
    }

    @Test
    void aRatedProductHasAnAggregateRating() throws Exception {
        Shop s = shop("sf-rated");
        db.execute("UPDATE product SET rating_total = 9, rating_count = 2 WHERE id = ?",
                new Object[] {s.productId});
        Map ld = Json.parseObject(attribute(fetch("/s/sf-rated/products/sf-rated-mug")
                .getContentAsString(), "data-ld"));
        Map rating = (Map) ld.get("aggregateRating");
        assertEquals("AggregateRating", rating.get("@type"));
        assertEquals(2L, Rows.number(rating.get("reviewCount")));
    }

    @Test
    void aScriptInATitleIsText() throws Exception {
        Shop s = shop("sf-xss");
        db.execute("UPDATE product SET title = ? WHERE id = ?",
                new Object[] {"<script>alert(1)</script> Mug", s.productId});
        String html = fetch("/s/sf-xss/products/sf-xss-mug").getContentAsString();
        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt; Mug"));
        Map ld = Json.parseObject(attribute(html, "data-ld"));
        assertEquals("<script>alert(1)</script> Mug", ld.get("name"));
    }

    @Test
    void theSitemapListsOnlyWhatIsForSale() throws Exception {
        Shop open = shop("sf-map");
        Shop closed = shop("sf-map-closed");
        db.execute("UPDATE store SET status = ? WHERE id = ?",
                new Object[] {Stores.SUSPENDED, closed.storeId});
        db.execute("INSERT INTO product (id, store_id, handle, title, description, vendor, "
                + "product_type, tags, status, seo_title, seo_description, photo_id, rating_total, "
                + "rating_count, created_at, updated_at) VALUES (?, ?, 'sf-map-draft', 'Draft', "
                + "'', '', '', '', 'DRAFT', '', '', '', 0, 0, 0, 0)",
                new Object[] {Ids.next(), open.storeId});
        MockResponse r = fetch("/sitemap.xml");
        assertEquals(200, r.getStatus());
        String xml = r.getContentAsString();
        assertTrue(xml.contains("/s/sf-map/products/sf-map-mug</loc>"));
        assertTrue(xml.contains("/s/sf-map</loc>"));
        assertFalse(xml.contains("sf-map-draft"));
        assertFalse(xml.contains("sf-map-closed"));
        assertEquals(404, fetch("/s/sf-map-closed").getStatus());
        assertEquals(404, fetch("/s/sf-map/products/sf-map-draft").getStatus());
    }

    @Test
    void robotsPointsAtTheSitemap() throws Exception {
        MockResponse r = fetch("/robots.txt");
        assertEquals(200, r.getStatus());
        String text = r.getContentAsString();
        assertTrue(text.contains("User-agent: *"));
        assertTrue(text.contains("Disallow: /api/"));
        assertTrue(text.contains("Sitemap: ") && text.contains("/sitemap.xml"));
    }

    @Test
    void theCartLivesInTheSession() throws Exception {
        Shop s = shop("sf-cart");
        MockResponse empty = fetch("/s/sf-cart/cart");
        assertTrue(empty.getContentAsString().contains("Your cart is empty"));
        MockResponse added = mvc.perform(post("/s/sf-cart/cart/add")
                .header("Sec-Fetch-Site", "same-origin")
                .contentType("application/x-www-form-urlencoded")
                .content("variantId=" + s.variantId + "&quantity=2")).andReturn().getResponse();
        assertTrue(added.getStatus() == 302 || added.getStatus() == 303);
        assertTrue(added.getRedirectedUrl().endsWith("/s/sf-cart/cart"));
        String session = added.getCookie("CN1SESSION");
        assertNotNull(session);
        MockResponse cart = mvc.perform(get("/s/sf-cart/cart").cookie("CN1SESSION", session))
                .andReturn().getResponse();
        String html = cart.getContentAsString();
        assertTrue(html.contains(">Mug</a>"), html);
        assertTrue(html.contains("value=\"2\""));
        assertTrue(html.contains("store=sf-cart&amp;cart="));
        assertEquals("private, no-store", cart.getHeader("Cache-Control"));
        // Another visitor sees their own, empty cart.
        assertTrue(fetch("/s/sf-cart/cart").getContentAsString().contains("Your cart is empty"));
        int crossSite = mvc.perform(post("/s/sf-cart/cart/add")
                .header("Sec-Fetch-Site", "cross-site").cookie("CN1SESSION", session)
                .contentType("application/x-www-form-urlencoded")
                .content("variantId=" + s.variantId + "&quantity=1")).andReturn().getResponse()
                .getStatus();
        assertEquals(403, crossSite);
    }

    @Test
    void unknownThingsAre404Pages() throws Exception {
        String[] paths = {"/s/no-such-store", "/s/no-such-store/products/x", "/track/NOPE123",
            "/media/nothing-here"};
        for (int iter = 0; iter < paths.length; iter++) {
            MockResponse r = fetch(paths[iter]);
            assertEquals(404, r.getStatus(), paths[iter]);
        }
        shop("sf-404");
        MockResponse r = fetch("/s/sf-404/products/missing");
        assertEquals(404, r.getStatus());
        assertTrue(r.getContentAsString().contains("We could not find that page"));
        assertTrue(r.getContentType().startsWith("text/html"));
    }

    @Test
    void publicPagesAreCachedAndRevalidated() throws Exception {
        shop("sf-cache");
        MockResponse first = fetch("/s/sf-cache");
        assertEquals("public, max-age=60, stale-while-revalidate=600",
                first.getHeader("Cache-Control"));
        MockResponse map = fetch("/sitemap.xml");
        assertNotNull(map.getHeader("ETag"));
        assertEquals(304, mvc.perform(get("/sitemap.xml")
                .header("If-None-Match", map.getHeader("ETag"))).andReturn().getResponse()
                .getStatus());
        assertEquals("private, no-store", fetch("/s/sf-cache/cart").getHeader("Cache-Control"));
    }
}
