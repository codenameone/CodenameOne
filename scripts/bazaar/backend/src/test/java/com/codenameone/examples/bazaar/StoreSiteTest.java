/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.bazaar;

import com.codenameone.examples.bazaar.web.SiteUrls;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Predictive search, synonyms and boosts, the pre-launch password and the
/// embeddable buy button.
@BackendTest
class StoreSiteTest {
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
    @Autowired
    private Catalog catalog;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private MockResponse fetch(String path) throws Exception {
        return mvc.perform(get(path)).andReturn().getResponse();
    }

    @Test
    void suggestMatchesTitlePrefixes() throws Exception {
        shop("ss-sug");
        String json = fetch("/s/ss-sug/suggest?q=mu").getContentAsString();
        assertTrue(json.contains("\"title\":\"Mug\""), json);
        assertTrue(json.contains("/s/ss-sug/products/ss-sug-mug"), json);
        assertEquals("[]", fetch("/s/ss-sug/suggest?q=zebra").getContentAsString());
        String app = fetch("/api/shop/stores/ss-sug/suggest?q=mu").getContentAsString();
        assertTrue(app.contains("\"handle\":\"ss-sug-mug\""), app);
        assertTrue(fetch("/s/ss-sug/search?q=mu").getContentAsString()
                .contains("data-suggest=\"/s/ss-sug/suggest\""));
        assertTrue(fetch("/suggest.js").getContentAsString().contains("q-suggest"));
    }

    @Test
    void synonymsWidenSearchAndBoostsRank() throws Exception {
        Shop s = shop("ss-syn");
        assertEquals(0, catalog.browse("ss-syn", null, "cup", null, 0, 24, null).items().size());
        db.execute("UPDATE store SET search_synonyms = ? WHERE id = ?", new Object[] {"cup, mug\nsofa, couch",
                s.storeId});
        assertEquals(1, catalog.browse("ss-syn", null, "cup", null, 0, 24, null).items().size());
        db.execute("UPDATE product SET search_boost = 5 WHERE id = ?",
                new Object[] {s.productId});
        assertEquals(1, catalog.browse("ss-syn", null, "mug", null, 0, 24, null).items().size());
    }

    @Test
    void aPasswordHidesTheStorefrontUntilTyped() throws Exception {
        Shop s = shop("ss-pw");
        db.execute("UPDATE store SET storefront_password = ? WHERE id = ?", new Object[] {"open-sesame",
                s.storeId});
        MockResponse home = fetch("/s/ss-pw");
        assertTrue(home.getRedirectedUrl().endsWith("/s/ss-pw/password"));
        assertTrue(fetch("/s/ss-pw/products/ss-pw-mug").getRedirectedUrl()
                .endsWith("/password"));
        assertEquals("[]", fetch("/s/ss-pw/suggest?q=mu").getContentAsString());
        assertTrue(fetch("/s/ss-pw/password").getContentAsString().contains("type=\"password\""));
        assertFalse(fetch("/sitemap.xml").getContentAsString().contains("/s/ss-pw"));
        assertTrue(fetch("/robots.txt").getContentAsString().contains("Disallow: /s/ss-pw\n"));
        MockResponse wrong = mvc.perform(post("/s/ss-pw/password")
                .header("Sec-Fetch-Site", "same-origin")
                .contentType("application/x-www-form-urlencoded").content("password=nope"))
                .andReturn().getResponse();
        assertTrue(wrong.getRedirectedUrl().endsWith("wrong=1"));
        MockResponse right = mvc.perform(post("/s/ss-pw/password")
                .header("Sec-Fetch-Site", "same-origin")
                .contentType("application/x-www-form-urlencoded").content("password=open-sesame"))
                .andReturn().getResponse();
        String session = right.getCookie("CN1SESSION");
        assertNotNull(session);
        MockResponse in = mvc.perform(get("/s/ss-pw").cookie("CN1SESSION", session))
                .andReturn().getResponse();
        assertEquals(200, in.getStatus());
    }

    @Test
    void theBuyButtonCanBeFramedAnywhere() throws Exception {
        shop("ss-emb");
        MockResponse r = fetch("/s/ss-emb/embed/ss-emb-mug");
        assertEquals(200, r.getStatus());
        String html = r.getContentAsString();
        assertTrue(html.contains("target=\"_top\""), html);
        assertTrue(html.contains("/s/ss-emb/products/ss-emb-mug"), html);
        assertTrue(r.getHeader("Content-Security-Policy").contains("frame-ancestors *"));
        assertEquals(null, r.getHeader("X-Frame-Options"));
        assertEquals(404, fetch("/s/ss-emb/embed/nothing").getStatus());
    }

    private MockResponse onHost(String host, String path) throws Exception {
        return mvc.perform(get(path).header("Host", host)).andReturn().getResponse();
    }

    @Test
    void aCustomDomainServesItsStoreAtTheRoot() throws Exception {
        Shop s = shop("ss-cd");
        db.execute("UPDATE store SET custom_domain = ?, domain_verified_at = 1 WHERE id = ?",
                new Object[] {"shop.ss-cd.example", s.storeId});
        String host = "Shop.SS-CD.example:443";
        MockResponse product = onHost(host, "/products/ss-cd-mug");
        assertEquals(200, product.getStatus());
        String html = product.getContentAsString();
        assertTrue(html.contains("https://shop.ss-cd.example/products/ss-cd-mug"), html);
        assertTrue(onHost(host, "/").getContentAsString().contains("ss-cd-mug"));
        assertEquals(200, onHost(host, "/search?q=mu").getStatus());
        assertEquals(200, onHost(host, "/cart").getStatus());
        assertTrue(onHost(host, "/sitemap.xml").getContentAsString()
                .contains("<loc>https://shop.ss-cd.example/products/ss-cd-mug</loc>"));
        assertTrue(onHost(host, "/robots.txt").getContentAsString()
                .contains("Sitemap: https://shop.ss-cd.example/sitemap.xml"));
        // Unknown hosts fall through to the platform as before.
        assertEquals(404, onHost("elsewhere.example", "/products/ss-cd-mug").getStatus());
        assertFalse(fetch("/sitemap.xml").getContentAsString().contains("ss-cd-mug"));
    }

    @Test
    void customDomainsAreFoldedAndValidated() {
        assertEquals("shop.example.com", SiteUrls.domain(" Shop.Example.COM. "));
        assertEquals("", SiteUrls.domain(""));
        assertEquals(null, SiteUrls.domain("localhost"));
        assertEquals(null, SiteUrls.domain("10.0.0.1"));
        assertEquals(null, SiteUrls.domain("-bad.example.com"));
        assertEquals(null, SiteUrls.domain("a b.example.com"));
        assertEquals(null, SiteUrls.domain("evil.example/path"));
    }
}
