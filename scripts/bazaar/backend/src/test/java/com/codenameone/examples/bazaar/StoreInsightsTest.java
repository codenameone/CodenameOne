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
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProfitLossDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.ops.Reports;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The profit and loss statement, "frequently bought together" and the
/// accessibility markers of the server-rendered product page.
@BackendTest
class StoreInsightsTest {
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
    private Reports reports;
    @Autowired
    private Catalog catalog;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 300, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    /// A second, uncosted product in `s`, stocked.
    private String saucer(Shop s, String name) throws Exception {
        String pid = Ids.next();
        db.execute("INSERT INTO product (id, store_id, handle, title, description, vendor, "
                + "product_type, tags, status, seo_title, seo_description, photo_id, rating_total, "
                + "rating_count, created_at, updated_at) VALUES (?, ?, ?, 'Saucer', '', '', '', '', "
                + "'ACTIVE', '', '', '', 0, 0, 0, 0)",
                new Object[] {pid, s.storeId, name + "-saucer"});
        String vid = Ids.next();
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position) VALUES "
                + "(?, ?, ?, 'White', '', '', '', ?, '', 500, 0, 0, 100, 1, 1, 0, 0)",
                new Object[] {vid, pid, s.storeId, name + "-SAUCER"});
        inventory.restock(s.storeId, vid, s.locationId, 50, "opening", "test", Inventory.RESTOCK);
        return pid + "\n" + vid;
    }

    @Test
    void profitAndLossSubtractsCostOfGoodsAndCountsUncostedUnits() throws Exception {
        Shop s = shop("si-pnl");
        String[] saucer = saucer(s, "si-pnl").split("\n");
        String buyer = People.customer(accounts, "si-pnl-buyer");
        carts.add(s.storeId, buyer, null, new CartLineChangeDto(saucer[1], 2));
        s.buy(buyer, 3);
        String today = Days.iso(System.currentTimeMillis());
        String before = Days.iso(System.currentTimeMillis() - 2 * Days.MILLIS);
        ProfitLossDto p = reports.profitLoss(s.owner, s.storeId, before, today);
        assertEquals(1, p.orders());
        assertEquals(1200, p.cogsCents(), "three mugs at 4.00");
        assertEquals(2, p.uncostedUnits(), "the saucers have no cost");
        assertEquals(p.grossSalesCents() - p.discountsCents() - p.refundsCents(), p.netSalesCents());
        assertEquals(p.netSalesCents() - 1200, p.grossMarginCents());
        assertTrue(p.taxCents() >= 0 && p.shippingCents() > 0, "Express shipping was charged");
        String csv = Reports.profitLossCsv(p);
        assertTrue(csv.startsWith("line,amount\r\n"), csv);
        assertTrue(csv.contains("cost_of_goods,12.00\r\n"), csv);
        assertTrue(csv.contains("uncosted_units,2\r\n"), csv);
        ProfitLossDto empty = reports.profitLoss(s.owner, s.storeId, "2001-01-01", "2001-01-31");
        assertEquals(0, empty.orders());
        assertEquals(0, empty.marginBp());
    }

    @Test
    void productsBoughtTogetherAreRecommendedOnTheWebPage() throws Exception {
        Shop s = shop("si-fbt");
        String[] saucer = saucer(s, "si-fbt").split("\n");
        assertTrue(catalog.together(s.productId, 4).isEmpty() || !catalog.together(s.productId, 4)
                .get(0).id().equals(saucer[0]), "nothing bought together yet");
        String buyer = People.customer(accounts, "si-fbt-buyer");
        carts.add(s.storeId, buyer, null, new CartLineChangeDto(saucer[1], 1));
        s.buy(buyer, 1);
        List<ProductCardDto> picks = catalog.together(s.productId, 4);
        assertFalse(picks.isEmpty());
        assertEquals(saucer[0], picks.get(0).id());
        assertEquals(s.productId, catalog.together(saucer[0], 4).get(0).id());
        String html = visit("/s/si-fbt/products/si-fbt-mug").getContentAsString();
        assertTrue(html.contains("Frequently bought together"), html);
    }

    @Test
    void theProductPageCarriesItsAccessibilityMarkers() throws Exception {
        shop("si-a11y");
        MockResponse r = visit("/s/si-a11y/products/si-a11y-mug");
        assertEquals(200, r.getStatus());
        String html = r.getContentAsString();
        assertTrue(html.contains(" lang=\""), "page language");
        assertTrue(html.contains("class=\"skip\" href=\"#main\""), "skip link");
        assertTrue(html.contains("<main id=\"main\""), "main landmark the skip link targets");
        assertTrue(html.contains("<header"), "banner landmark");
        assertTrue(html.contains("<footer"), "contentinfo landmark");
        assertTrue(html.contains("<label for=\"quantity\"") || html.contains("for=\"quantity\""),
                "quantity input labelled");
        assertTrue(html.contains("aria-label="), "search and navigation are named");
        int at = html.indexOf("<img");
        while (at >= 0) {
            int end = html.indexOf('>', at);
            assertTrue(html.substring(at, end).contains("alt="), html.substring(at, end));
            at = html.indexOf("<img", end);
        }
    }

    private MockResponse visit(String path) throws Exception {
        return mvc.perform(get(path).header("Host", SignIn.HOST)).andReturn().getResponse();
    }
}
