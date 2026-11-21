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
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.api.MenuLinkDto;
import com.codenameone.examples.bazaar.api.MetafieldDefDto;
import com.codenameone.examples.bazaar.api.MetafieldValueDto;
import com.codenameone.examples.bazaar.api.TranslationDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.content.Metafields;
import com.codenameone.examples.bazaar.content.Navigation;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.market.Translations;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Menu link labels and metafield names in the market's language: a de-CH
/// market reads the `de` translation, an untranslated link keeps its
/// original label, and a visitor with no market sees the originals.
@BackendTest
class ContentTranslationTest {
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
    @Autowired
    private Translations translations;
    @Autowired
    private Navigation navigation;
    @Autowired
    private Metafields metafields;

    @Test
    void menuLabelsAndSpecNamesFollowTheMarketLanguageWithFallback() throws Exception {
        Shop s = new Shop("ct-menu", 300, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        navigation.add(s.owner, s.storeId, new MenuLinkDto(null, Navigation.MAIN, "", "Sale",
                "/s/ct-menu", 0));
        List<MenuLinkDto> made = navigation.add(s.owner, s.storeId, new MenuLinkDto(null,
                Navigation.MAIN, "", "Journal", "/s/ct-menu", 0));
        String sale = made.get(0).id();
        String journal = made.get(1).id();
        translations.put(s.owner, s.storeId, new TranslationDto(Translations.MENU_LINK, sale,
                "de", "title", "Ausverkauf"));
        MetafieldDefDto material = metafields.define(s.owner, s.storeId, new MetafieldDefDto(
                null, "PRODUCT", "material", "Material", "text", "", true));
        metafields.put(s.owner, s.storeId, new MetafieldValueDto(material.id(), "PRODUCT",
                s.productId, "material", null, null, "Stoneware"));
        translations.put(s.owner, s.storeId, new TranslationDto(Translations.METAFIELD,
                material.id(), "de", "name", "Werkstoff"));
        markets.save(s.owner, s.storeId, null, new MarketDto(null, "ch", "Switzerland", "USD",
                Arrays.asList("CH"), "de-CH", Markets.NONE, 0, true));

        String page = "/s/ct-menu/products/ct-menu-mug";
        String ch = body(page + "?market=ch");
        assertTrue(ch.contains("Ausverkauf"), "translated label by the de fallback");
        assertTrue(ch.contains("Journal"), "an untranslated label stays as written");
        assertTrue(ch.contains("Werkstoff"), "the spec name in German");
        MockResponse plain = mvc.perform(get(page).header("Host", SignIn.HOST)).andReturn()
                .getResponse();
        String en = plain.getContentAsString();
        assertTrue(en.contains("Sale") && !en.contains("Ausverkauf"), "the default language");
        assertTrue(en.contains("Material") && !en.contains("Werkstoff"));
        assertTrue(plain.getHeader("Vary") != null
                && plain.getHeader("Vary").contains("Accept-Language"), "varies by language");

        // Removing the link takes its translations with it.
        navigation.remove(s.owner, s.storeId, sale);
        assertFalse(translations.has(s.storeId, Translations.MENU_LINK));
        // Another store cannot translate this store's link.
        Shop o = new Shop("ct-menu-o", 300, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        try {
            translations.put(o.owner, o.storeId, new TranslationDto(Translations.MENU_LINK,
                    journal, "de", "title", "x"));
            throw new AssertionError("translated another store's link");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
    }

    private String body(String path) throws Exception {
        MockResponse r = mvc.perform(get(path).header("Host", SignIn.HOST)).andReturn()
                .getResponse();
        assertEquals(200, r.getStatus());
        return r.getContentAsString();
    }
}
