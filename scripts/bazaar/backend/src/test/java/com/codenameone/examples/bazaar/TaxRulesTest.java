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
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.taxes.SimulatedSalesTaxProvider;
import com.codenameone.examples.bazaar.taxes.TaxRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tax rules beyond the store's own rates: the EU VAT table by product
/// category, overrides per product and collection, the US provider and the
/// OSS report.
@BackendTest
class TaxRulesTest {
    private static final AddressDto BERLIN = new AddressDto("Ada Lovelace", "Hauptstr 1", "",
            "Berlin", "", "10115", "DE", "", 52.52, 13.40);

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
        return new Shop(name, 50, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String m(Shop s) {
        return "/api/merchant/stores/" + s.storeId;
    }

    private void send(Shop s, String path, String json) throws Exception {
        Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + path), s.owner), json));
    }

    private long tax(Shop s, String user, AddressDto to) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(to, null));
        carts.update(s.storeId, user, null, c.lines().get(0).id(), 0);
        return c.taxCents();
    }

    @Test
    void euVatFollowsTheProductCategoryWithTheNextHigherRateAsFallback() throws Exception {
        Shop s = shop("tx-eu");
        String buyer = People.customer(accounts, "tx-eu-buyer");
        long before = tax(s, buyer, BERLIN);
        send(s, "/tax-settings", "{\"euVat\":true,\"homeCountry\":\"FR\",\"usProvider\":\"\"}");
        long standard = tax(s, buyer, BERLIN);
        assertEquals(before, standard, "a standard-rated good pays the 19% the store had");
        send(s, "/products/" + s.productId + "/tax-category", "{\"category\":\"reduced\"}");
        long reduced = tax(s, buyer, BERLIN);
        assertEquals(190 - 70, standard - reduced, "7% instead of 19% on a 10.00 mug");
        // Germany has no super-reduced rate: the next higher one applies.
        send(s, "/products/" + s.productId + "/tax-category", "{\"category\":\"super-reduced\"}");
        assertEquals(reduced, tax(s, buyer, BERLIN));
        send(s, "/products/" + s.productId + "/tax-category", "{\"category\":\"exempt\"}");
        assertEquals(190, standard - tax(s, buyer, BERLIN));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(s) + "/products/"
                + s.productId + "/tax-category"), s.owner), "{\"category\":\"luxury\"}")));
        List rates = Rest.list(mvc, People.asMerchant(get(m(s) + "/vat-rates"), s.owner));
        assertTrue(rates.size() > 27);
    }

    @Test
    void overridesBeatTheTableAndARegionBeatsItsCountry() throws Exception {
        Shop s = shop("tx-ovr");
        String buyer = People.customer(accounts, "tx-ovr-buyer");
        long ny = tax(s, buyer, Shop.NY);
        String collection = Ids.next();
        db.execute("INSERT INTO collection (id, store_id, handle, title, description, photo_id, "
                + "rule_text, position, published) VALUES (?, ?, 'cups', 'Cups', '', '', '', 0, 1)",
                new Object[] {collection, s.storeId});
        db.execute("INSERT INTO collection_product (id, collection_id, product_id, position) "
                + "VALUES (?, ?, ?, 0)", new Object[] {Ids.next(), collection, s.productId});
        send(s, "/tax-overrides", "{\"targetKind\":\"collection\",\"targetId\":\"" + collection
                + "\",\"country\":\"US\",\"region\":\"\",\"basisPoints\":100}");
        assertEquals(40 - 10, ny - tax(s, buyer, Shop.NY), "1% for the collection, US-wide");
        send(s, "/tax-overrides", "{\"targetKind\":\"product\",\"targetId\":\"" + s.productId
                + "\",\"country\":\"US\",\"region\":\"\",\"basisPoints\":200}");
        assertEquals(40 - 20, ny - tax(s, buyer, Shop.NY), "the product's own rule wins");
        send(s, "/tax-overrides", "{\"targetKind\":\"product\",\"targetId\":\"" + s.productId
                + "\",\"country\":\"US\",\"region\":\"NY\",\"basisPoints\":0}");
        assertEquals(40, ny - tax(s, buyer, Shop.NY), "New York's own rule beats the US one");
        List all = Rest.list(mvc, People.asMerchant(get(m(s) + "/tax-overrides"), s.owner));
        assertEquals(3, all.size());
        for (Object o : all) {
            Rest.list(mvc, People.asMerchant(delete(m(s) + "/tax-overrides/" + ((Map) o).get("id")),
                    s.owner));
        }
        assertEquals(ny, tax(s, buyer, Shop.NY));
        // Another store's product cannot be the target.
        Shop other = shop("tx-ovr-b");
        assertEquals(404, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(s)
                + "/tax-overrides"), s.owner), "{\"targetKind\":\"product\",\"targetId\":\""
                + other.productId + "\",\"country\":\"US\",\"region\":\"\",\"basisPoints\":0}")));
    }

    @Test
    void theSimulatedUsProviderRatesByStateAndCategory() throws Exception {
        Shop s = shop("tx-us");
        String buyer = People.customer(accounts, "tx-us-buyer");
        long ny = tax(s, buyer, Shop.NY);
        send(s, "/tax-settings", "{\"euVat\":false,\"homeCountry\":\"US\",\"usProvider\":\""
                + SimulatedSalesTaxProvider.NAME + "\"}");
        assertEquals(ny, tax(s, buyer, Shop.NY), "New York's 4% either way");
        send(s, "/products/" + s.productId + "/tax-category", "{\"category\":\"reduced\"}");
        assertEquals(20, ny - tax(s, buyer, Shop.NY), "reduced goods at half the state rate");
        SimulatedSalesTaxProvider p = new SimulatedSalesTaxProvider();
        assertEquals(0, p.basisPoints("OR", TaxRules.STANDARD));
        assertEquals(-1, p.basisPoints("ZZ", TaxRules.STANDARD));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(s)
                + "/tax-settings"), s.owner), "{\"euVat\":false,\"homeCountry\":\"US\","
                + "\"usProvider\":\"nobody\"}")));
    }

    @Test
    void theOssReportSumsCrossBorderEuSalesByCountryAndRate() throws Exception {
        Shop s = shop("tx-oss");
        send(s, "/tax-settings", "{\"euVat\":true,\"homeCountry\":\"FR\",\"usProvider\":\"\"}");
        send(s, "/products/" + s.productId + "/tax-category", "{\"category\":\"reduced\"}");
        String buyer = People.customer(accounts, "tx-oss-buyer");
        carts.add(s.storeId, buyer, null, new CartLineChangeDto(s.variantId, 3));
        CartDto c = carts.estimate(s.storeId, buyer, null, new EstimateDto(BERLIN, null));
        Map card = Rest.addCard(mvc, buyer, Rest.VISA);
        OrderDto o = checkouts.place(buyer, s.storeId, Ids.next(), new PlaceOrderDto(BERLIN,
                c.shippingOptions().get(0).rateId(), (String) card.get("id"),
                buyer + "@example.com", "", null, 0, null));
        assertTrue(o.number() > 0);
        // A US sale is not in it.
        s.buy(buyer, 1);

        List rows = Rest.list(mvc, People.asMerchant(get(m(s) + "/oss-report"), s.owner));
        assertEquals(1, rows.size(), String.valueOf(db.query("SELECT o.financial, o.status, o.ship_country, l.tax_bp, l.tax_cents FROM store_order o JOIN order_line l ON l.order_id = o.id WHERE o.store_id = ?", new Object[] {s.storeId})));
        Map de = (Map) rows.get(0);
        assertEquals("DE", de.get("country"));
        assertEquals(700L, Rest.number(de, "basisPoints"));
        assertEquals(1L, Rest.number(de, "orders"));
        assertEquals(3000L, Rest.number(de, "netCents"));
        assertEquals(210L, Rest.number(de, "taxCents"));
        MockResponse csv = mvc.perform(People.asMerchant(get(m(s) + "/oss-report.csv"), s.owner))
                .andReturn().getResponse();
        assertEquals(200, csv.getStatus());
        assertTrue(csv.getContentType().startsWith("text/csv"));
        String body = csv.getContentAsString();
        assertTrue(body.startsWith("country,vat_rate_percent,currency,orders,net_amount,vat_amount"), body);
        assertTrue(body.contains("DE,7.00,USD,1,30.00,2.10"), body);
        // Outside the dates asked for, nothing.
        assertEquals(0, Rest.list(mvc, People.asMerchant(get(m(s)
                + "/oss-report?from=2001-01-01&to=2001-12-31"), s.owner)).size());
    }
}
