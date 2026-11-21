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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.StoreText;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The storefront's interface text follows the market language (Spanish,
/// Hebrew right to left), or the visitor's Accept-Language when no market
/// names one -- and then the page says it varies by that header -- with
/// English for a key a translation lacks.
@BackendTest
class StoreTextTest {
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
    private Markets markets;

    @Test
    void storefrontLabelsFollowTheMarketLanguage() throws Exception {
        Shop s = new Shop("st-text", 300, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        markets.save(s.owner, s.storeId, null, new MarketDto(null, "mx", "Mexico", "USD",
                Arrays.asList("MX"), "es-MX", Markets.NONE, 0, true, Markets.MANUAL));
        markets.save(s.owner, s.storeId, null, new MarketDto(null, "il", "Israel", "USD",
                Arrays.asList("IL"), "he", Markets.NONE, 0, true, Markets.MANUAL));
        String page = "/s/st-text/products/st-text-mug";

        String es = body(mvc.perform(get(page + "?market=mx").header("Host", SignIn.HOST))
                .andReturn().getResponse());
        assertTrue(es.contains("lang=\"es\""), "lang follows the market");
        assertTrue(es.contains("dir=\"ltr\""));
        assertTrue(es.contains("A\u00f1adir al carrito"), "the button in Spanish");
        assertTrue(es.contains(">Carrito<"), "the header in Spanish");
        assertFalse(es.contains("Add to cart"));

        String he = body(mvc.perform(get(page + "?market=il").header("Host", SignIn.HOST))
                .andReturn().getResponse());
        assertTrue(he.contains("lang=\"he\"") && he.contains("dir=\"rtl\""), "Hebrew is rtl");
        assertTrue(he.contains("\u05dc\u05d4\u05d5\u05e1\u05d9\u05e3 \u05dc\u05e2\u05d2\u05dc\u05d4"),
                "the button in Hebrew");

        // No market: the visitor's header picks, and the page varies by it.
        MockResponse fr = mvc.perform(get(page).header("Host", SignIn.HOST)
                .header("Accept-Language", "fr-CA,fr;q=0.8")).andReturn().getResponse();
        String frBody = body(fr);
        assertTrue(frBody.contains("Ajouter au panier") && frBody.contains("lang=\"fr\""));
        assertTrue(fr.getHeader("Vary") != null && fr.getHeader("Vary").contains("Accept-Language"),
                "a header-picked language varies the cache by that header");
        MockResponse en = mvc.perform(get(page).header("Host", SignIn.HOST)).andReturn()
                .getResponse();
        String enBody = body(en);
        assertTrue(enBody.contains("Add to cart") && enBody.contains("lang=\"en\""));
        assertNotEquals(en.getHeader("ETag"), fr.getHeader("ETag"),
                "each language is its own validator");
        // A market's copy is its own address, so its language needs no Vary.
        MockResponse mx = mvc.perform(get(page + "?market=mx").header("Host", SignIn.HOST)
                .header("Accept-Language", "de")).andReturn().getResponse();
        assertTrue(body(mx).contains("A\u00f1adir al carrito"), "the market wins over the header");
    }

    @Test
    void signInFollowsTheVisitorsLanguage() throws Exception {
        String he = body(mvc.perform(get("/auth/signin").header("Host", SignIn.HOST)
                .header("Accept-Language", "he-IL")).andReturn().getResponse());
        assertTrue(he.contains("dir=\"rtl\"") && he.contains("lang=\"he\""));
        assertTrue(he.contains("\u05e1\u05d9\u05e1\u05de\u05d4"), "the password label in Hebrew");
        String es = body(mvc.perform(get("/auth/signin?error=1").header("Host", SignIn.HOST)
                .header("Accept-Language", "es")).andReturn().getResponse());
        assertTrue(es.contains("no corresponden a ninguna cuenta"), "the problem in Spanish");
    }

    @Test
    void aMissingTranslationFallsBackToEnglishPerKey() {
        Map<String, String> t = StoreText.merge(
                new String[] {"a", "Apple", "b", "Bread"}, new String[] {"a", "Manzana"});
        assertEquals("Manzana", t.get("a"));
        assertEquals("Bread", t.get("b"), "the key the translation lacks reads in English");
        for (String lang : new String[] {"es", "fr", "de", "he"}) {
            Map<String, String> full = StoreText.table(lang);
            for (String key : StoreText.keys()) {
                assertTrue(full.get(key) != null && full.get(key).length() > 0, lang + " " + key);
            }
        }
        assertEquals(StoreText.table("en"), StoreText.table("xx"), "unknown is English");
    }

    @Test
    void everyTemplateKeyHasEnglishText() throws Exception {
        Set<String> keys = new HashSet<String>(Arrays.asList(StoreText.keys()));
        File[] files = new File("src/main/resources/templates").listFiles();
        int seen = 0;
        for (int iter = 0; files != null && iter < files.length; iter++) {
            String s = new String(Files.readAllBytes(files[iter].toPath()), StandardCharsets.UTF_8);
            int at = s.indexOf(".t['");
            while (at >= 0) {
                int end = s.indexOf("']", at);
                String key = s.substring(at + 4, end);
                assertTrue(keys.contains(key), files[iter].getName() + " names " + key);
                seen++;
                at = s.indexOf(".t['", end);
            }
        }
        assertTrue(seen > 100, "the templates use the table");
    }

    private static String body(MockResponse r) throws Exception {
        assertEquals(200, r.getStatus());
        return r.getContentAsString();
    }
}
