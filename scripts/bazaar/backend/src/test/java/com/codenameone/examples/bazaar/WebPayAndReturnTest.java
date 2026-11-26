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
import com.codenameone.examples.bazaar.api.DraftOrderDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderSettingsDto;
import com.codenameone.examples.bazaar.api.PayLinkDto;
import com.codenameone.examples.bazaar.api.PayLinkRequestDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.OrderSettings;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.order.PayLinks;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.returns.Returns;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The storefront pages for a pay link and for a self-serve return.
@BackendTest
class WebPayAndReturnTest {
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
    private Orders orders;
    @Autowired
    private PayLinks payLinks;
    @Autowired
    private Fulfillments fulfillments;
    @Autowired
    private Returns returns;
    @Autowired
    private OrderSettings settings;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static void redirected(MockResponse r) {
        assertTrue(r.getStatus() == 302 || r.getStatus() == 303, "answered " + r.getStatus());
    }

    @Test
    void aGuestPaysADraftThroughItsLink() throws Exception {
        Shop s = shop("wp-pay");
        OrderDto draft = orders.createDraft(s.owner, s.storeId, new DraftOrderDto(
                "buyer@example.com", Collections.singletonList(new CartLineChangeDto(
                        s.variantId, 2)), null, null, "phone sale"));
        PayLinkDto link = payLinks.create(s.owner, s.storeId, draft.id(),
                new PayLinkRequestDto(3));
        String token = link.url().substring(link.url().lastIndexOf('/') + 1);
        Browser b = new Browser(mvc);
        redirected(b.get("/pay/" + token));
        String page = "/s/wp-pay/checkout/pay/" + token;
        b.assertPage(b.get(page), "Prepared for you by");
        assertEquals(404, new Browser(mvc).get("/s/other-shop/checkout/pay/" + token).getStatus());
        assertEquals(404, new Browser(mvc).get("/pay/" + token + "x").getStatus());
        assertEquals(403, b.postWithoutToken(page, "cardNumber", "4242424242424242",
                "cardExpiry", "12/39", "cardCvc", "123").getStatus());
        b.get(page);
        MockResponse paid = b.post(page, "name", "Ada", "cardNumber", "4242424242424242",
                "cardExpiry", "12/39", "cardCvc", "123");
        assertEquals(200, paid.getStatus(), paid.getContentAsString());
        assertTrue(paid.getContentAsString().contains("Paid. Thank you"), paid.getContentAsString());
        assertEquals(Orders.PAID, orders.detailOf(draft.id()).financial());
        // A used link is gone.
        assertEquals(404, new Browser(mvc).get(page).getStatus());
    }

    @Test
    void aShopperQuotesAndRequestsAStoreCreditReturn() throws Exception {
        Shop s = shop("wp-return");
        settings.save(s.owner, s.storeId, new OrderSettingsDto(false, 30, 1000, 200, "", ""));
        String user = People.customer(accounts, "wp-return-buyer");
        OrderDto o = s.buy(user, 2);
        String line = o.lines().get(0).id();
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(line, null, null, 2));
        String fid = fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, "LOCAL", ls)).id();
        fulfillments.ship(s.owner, s.storeId, fid);
        fulfillments.delivered(s.owner, s.storeId, fid);
        Browser b = new Browser(mvc);
        b.signIn(user, People.PASSWORD);
        String path = "/account/orders/" + o.id();
        b.assertPage(b.get(path), "Restocking fee");
        MockResponse quote = b.post(path + "/return/quote", "qty-" + line, "1",
                "reason", "CHANGED_MIND", "resolution", "STORE_CREDIT");
        assertEquals(200, quote.getStatus(), quote.getContentAsString());
        String html = quote.getContentAsString();
        assertTrue(html.contains("Store credit you get"), html);
        b.get(path);
        redirected(b.post(path + "/return", "qty-" + line, "1",
                "reason", "CHANGED_MIND", "resolution", "STORE_CREDIT"));
        assertEquals(1, returns.mine(user, o.id()).size());
    }
}
