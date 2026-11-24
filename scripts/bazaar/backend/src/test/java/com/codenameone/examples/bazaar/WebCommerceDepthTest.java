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
import com.codenameone.examples.bazaar.api.CompanyBuyerDto;
import com.codenameone.examples.bazaar.api.CompanyDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.NetTermDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PriceListDto;
import com.codenameone.examples.bazaar.api.PriceListEntryDto;
import com.codenameone.examples.bazaar.api.SubscriptionDto;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.subscribe.Subscriptions;
import com.codenameone.examples.bazaar.trade.Trade;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The subscription and B2B web pages: the storefront's selling plans and
/// net-terms checkout, the account's subscriptions and company pages, and
/// the back office's subscriptions, renewals, companies, price lists, quote
/// board and invoices -- each main action through the pages, another store
/// or another shopper answering 404, a post without its CSRF token 403, a
/// shopper kept out of the back office and a non-buyer out of the company
/// pages. Plus the account's return form, offered only once a line arrived.
@BackendTest
class WebCommerceDepthTest {
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
    private Subscriptions subscriptions;
    @Autowired
    private Trade trade;
    @Autowired
    private Fulfillments fulfillments;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 200, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private CommerceSeed seed(Shop s, String prefix) throws Exception {
        return new CommerceSeed(s, prefix, accounts, mvc, subscriptions, trade);
    }

    private Browser signedIn(String user) throws Exception {
        Browser b = new Browser(mvc);
        b.signIn(user, People.PASSWORD);
        return b;
    }

    private static void redirected(MockResponse r) throws Exception {
        assertTrue(r.getStatus() == 302 || r.getStatus() == 303,
                "answered " + r.getStatus() + " " + r.getContentAsString());
    }

    private static String location(MockResponse r) throws Exception {
        redirected(r);
        return r.getRedirectedUrl();
    }

    // ============================================================ storefront

    @Test
    void aProductOffersItsPlanAndTheSubscriptionCheckoutCarriesIt() throws Exception {
        Shop s = shop("wcd-plan");
        CommerceSeed seed = seed(s, "wcd-plan");
        String handle = "wcd-plan";
        Browser guest = new Browser(mvc);
        MockResponse page = guest.get("/s/" + handle + "/products/" + handle + "-mug");
        guest.assertPage(page, "One-time purchase");
        guest.assertPage(page, "value=\"" + seed.plan.id() + "\"");
        guest.assertPage(page, "save 10%");

        String user = People.customer(accounts, "wcd-plan-new");
        Rest.addCard(mvc, user, Rest.VISA);
        Browser b = signedIn(user);
        String to = location(b.send(Browser.form("/s/" + handle + "/cart/add", "variantId",
                s.variantId, "quantity", "2", "plan", seed.plan.id())));
        assertTrue(to.startsWith("/account/subscriptions/start?"), to);
        assertTrue(to.indexOf("plan=" + seed.plan.id()) > 0, to);
        MockResponse start = b.get(to);
        b.assertPage(start, "Subscribe and pay the first delivery");
        b.assertPage(start, "every month");
        // 2 x $10.00 at 10% off.
        b.assertPage(start, "$18.00");
        redirected(b.post("/account/subscriptions/start", "shop", handle, "plan", seed.plan.id(),
                "variant", s.variantId, "quantity", "2", "card", "", "address", ""));
        List<SubscriptionDto> mine = subscriptions.mine(user, s.storeId);
        assertEquals(1, mine.size());
        assertEquals(2, mine.get(0).quantity());
        // A one-time add still goes to the cart.
        String cart = location(b.send(Browser.form("/s/" + handle + "/cart/add", "variantId",
                s.variantId, "quantity", "1", "plan", "")));
        assertTrue(cart.endsWith("/cart"), cart);
    }

    @Test
    void theAccountSkipsPausesResumesAndCancelsASubscription() throws Exception {
        Shop s = shop("wcd-subs");
        CommerceSeed seed = seed(s, "wcd-subs");
        Browser b = signedIn(seed.subscriber);
        MockResponse list = b.get("/account/subscriptions");
        b.assertPage(list, "Your subscriptions");
        b.assertPage(list, "Skip next delivery");
        SubscriptionDto sub = subscriptions.mine(seed.subscriber, s.storeId).get(0);
        String base = "/account/subscriptions/" + s.storeId + "/" + sub.id();
        long due = sub.dueAt();
        redirected(b.post(base + "/skip"));
        assertTrue(subscriptions.mine(seed.subscriber, s.storeId).get(0).dueAt() > due);
        redirected(b.post(base + "/pause"));
        assertEquals(Subscriptions.PAUSED, subscriptions.mine(seed.subscriber, s.storeId).get(0)
                .status());
        b.assertPage(b.get("/account/subscriptions"), "Resume");
        redirected(b.post(base + "/resume"));
        assertEquals(Subscriptions.ACTIVE, subscriptions.mine(seed.subscriber, s.storeId).get(0)
                .status());
        // Pausing twice is the service's 409, told as a message.
        redirected(b.post(base + "/resume"));
        redirected(b.post(base + "/cancel"));
        assertEquals(Subscriptions.CANCELLED, subscriptions.mine(seed.subscriber, s.storeId)
                .get(0).status());

        String other = People.customer(accounts, "wcd-subs-other");
        Browser stranger = signedIn(other);
        stranger.get("/account/subscriptions");
        assertEquals(404, stranger.post(base + "/pause").getStatus());
        assertEquals(403, b.postWithoutToken(base + "/pause").getStatus());
    }

    // ================================================================= buyer

    @Test
    void aBuyerSeesTheirPricesAndOrdersOnNetTermsAtCheckout() throws Exception {
        Shop s = shop("wcd-terms");
        CommerceSeed seed = seed(s, "wcd-terms");
        Browser b = signedIn(seed.buyer);
        String co = "/account/company/" + s.storeId;
        assertEquals(co, location(b.get("/account/company")));
        MockResponse page = b.get(co);
        b.assertPage(page, seed.company.name());
        b.assertPage(page, "Net 30");
        // The list's entry: $10.00 at 10% off.
        b.assertPage(page, "$9.00");
        b.assertPage(b.get("/account"), "/account/company");

        redirected(b.send(Browser.form("/s/wcd-terms/cart/add", "variantId", s.variantId,
                "quantity", "3")));
        MockResponse checkout = b.get("/s/wcd-terms/checkout");
        b.assertPage(checkout, "Place on net terms");
        String key = WebCommerceDepthTest.key(checkout.getContentAsString());
        String order = location(b.post("/s/wcd-terms/checkout/terms", "idempotencyKey", key,
                "email", seed.buyer, "name", "Dock 4", "line1", "1 Hafen", "city", "Hamburg",
                "postal", "20457", "country", "DE"));
        assertTrue(order.indexOf("/s/wcd-terms/orders/") >= 0, order);
        List<NetTermDto> terms = trade.myNetTerms(seed.buyer, s.storeId);
        assertEquals(2, terms.size());
        boolean found = false;
        for (int iter = 0; iter < terms.size(); iter++) {
            found |= terms.get(iter).totalCents() == 2700;
        }
        assertTrue(found, "three mugs at $9.00 on terms");
    }

    @Test
    void aQuantityRuleRefusesANetTermsCheckoutWithItsReason() throws Exception {
        Shop s = shop("wcd-rule");
        CommerceSeed seed = seed(s, "wcd-rule");
        trade.writeEntry(s.storeId, seed.list.id(), new PriceListEntryDto(null, s.variantId, 0,
                6, 0, 6));
        Browser b = signedIn(seed.buyer);
        redirected(b.send(Browser.form("/s/wcd-rule/cart/add", "variantId", s.variantId,
                "quantity", "4")));
        MockResponse checkout = b.get("/s/wcd-rule/checkout");
        String key = key(checkout.getContentAsString());
        MockResponse refused = b.post("/s/wcd-rule/checkout/terms", "idempotencyKey", key,
                "email", seed.buyer, "name", "Dock", "line1", "1 Hafen", "city", "Hamburg",
                "postal", "20457", "country", "DE");
        assertEquals(200, refused.getStatus());
        b.assertPage(refused, "At least 6 must be ordered");
        assertEquals(1, trade.myNetTerms(seed.buyer, s.storeId).size());
        b.assertPage(b.get("/account/company/" + s.storeId), "min 6, in steps of 6");
    }

    @Test
    void quickOrderAndCsvMakeQuotesOrShowEveryRefusedLine() throws Exception {
        Shop s = shop("wcd-csv");
        CommerceSeed seed = seed(s, "wcd-csv");
        Browser b = signedIn(seed.buyer);
        String co = "/account/company/" + s.storeId;
        b.get(co);
        String q = location(b.post(co + "/quick-order", "sku1", s.sku, "qty1", "5"));
        assertTrue(q.startsWith(co + "/quotes/"), q);
        b.assertPage(b.get(q), "Draft");

        MockResponse bad = b.post(co + "/upload", "csv", "sku,quantity\n" + s.sku
                + ",2\nNOPE,1\n" + s.sku + ",zero\n");
        assertEquals(200, bad.getStatus());
        b.assertPage(bad, "These lines were not accepted");
        b.assertPage(bad, "Unknown SKU NOPE");
        b.assertPage(bad, "is not a quantity");
        MockResponse comma = b.post(co + "/quick-order", "sku1", "a,b", "qty1", "1");
        b.assertPage(comma, "cannot hold a comma");
        int before = trade.myQuotes(seed.buyer, s.storeId).size();
        redirected(b.post(co + "/upload", "csv", s.sku + ",3\n"));
        assertEquals(before + 1, trade.myQuotes(seed.buyer, s.storeId).size());
    }

    @Test
    void aBuyerAcceptsASentQuoteAndOpensTheirInvoice() throws Exception {
        Shop s = shop("wcd-quote");
        CommerceSeed seed = seed(s, "wcd-quote");
        TradeQuoteDto q = trade.requestQuote(seed.buyer, s.storeId,
                CommerceSeed.request(s.variantId, 2));
        trade.moveQuote(s.owner, s.storeId, q.id(), Trade.SENT);
        Browser b = signedIn(seed.buyer);
        String co = "/account/company/" + s.storeId;
        MockResponse list = b.get(co + "/quotes");
        b.assertPage(list, "Accept");
        b.assertPage(list, "Decline");
        redirected(b.post(co + "/quotes/" + q.id() + "/" + Trade.ACCEPTED));
        assertEquals(Trade.ACCEPTED, trade.quote(s.owner, s.storeId, q.id()).status());
        // Accepting twice is the pipeline's 409, told as a message.
        redirected(b.post(co + "/quotes/" + q.id() + "/" + Trade.ACCEPTED));
        redirected(b.post(co + "/quotes/" + q.id() + "/" + Trade.ORDERED));
        assertEquals(Trade.ORDERED, trade.quote(s.owner, s.storeId, q.id()).status());

        MockResponse invoices = b.get(co + "/invoices");
        b.assertPage(invoices, "Invoice (PDF)");
        String orderId = seed.ordered.orderId();
        MockResponse pdf = b.get(co + "/invoices/" + orderId + "/invoice.pdf");
        assertEquals(200, pdf.getStatus());
        assertTrue(pdf.getContentType().startsWith("application/pdf"), pdf.getContentType());

        // Another company's buyer: the quote and the invoice are 404.
        Shop o = shop("wcd-quote-o");
        CommerceSeed other = seed(o, "wcd-quote-o");
        trade.addBuyer(s.owner, s.storeId, makeCompany(s, "Rival"), 
                new CompanyBuyerDto(null, other.buyer, ""));
        Browser rival = signedIn(other.buyer);
        assertEquals(404, rival.get(co + "/quotes/" + q.id()).getStatus());
        rival.get(co);
        assertEquals(404, rival.post(co + "/quotes/" + q.id() + "/" + Trade.DECLINED)
                .getStatus());
        assertEquals(404, rival.get(co + "/invoices/" + orderId + "/invoice.pdf").getStatus());
        assertEquals(403, b.postWithoutToken(co + "/quotes/" + q.id() + "/" + Trade.DECLINED)
                .getStatus());
    }

    private String makeCompany(Shop s, String name) throws Exception {
        return trade.writeCompany(s.storeId, null, new CompanyDto(null, name, "", 30, ""))
                .id();
    }

    @Test
    void somebodyWhoBuysForNoCompanySeesNoCompanyPages() throws Exception {
        Shop s = shop("wcd-nobuyer");
        seed(s, "wcd-nobuyer");
        String shopper = People.customer(accounts, "wcd-nobuyer-shopper");
        Browser b = signedIn(shopper);
        assertFalse(b.get("/account").getContentAsString().indexOf("/account/company") >= 0);
        String co = "/account/company/" + s.storeId;
        assertEquals(403, b.get("/account/company").getStatus());
        assertEquals(403, b.get(co).getStatus());
        assertEquals(403, b.get(co + "/quotes").getStatus());
        assertEquals(403, b.get(co + "/invoices").getStatus());
        assertEquals(403, b.post(co + "/quick-order", "sku1", s.sku, "qty1", "1").getStatus());
        assertEquals(403, b.post(co + "/upload", "csv", s.sku + ",1").getStatus());
    }

    // ========================================================== back office

    // =============================================================== returns

    @Test
    void theReturnFormWaitsUntilALineIsDelivered() throws Exception {
        Shop s = shop("wcd-return");
        String user = People.customer(accounts, "wcd-return-buyer");
        OrderDto o = s.buy(user, 2);
        String line = o.lines().get(0).id();
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(line, null, null, 2));
        String fid = fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, "LOCAL", ls)).id();
        fulfillments.ship(s.owner, s.storeId, fid);
        Browser b = signedIn(user);
        MockResponse shipped = b.get("/account/orders/" + o.id());
        assertEquals(200, shipped.getStatus());
        assertFalse(shipped.getContentAsString().indexOf("Request a return") >= 0,
                "a shipped order that has not arrived offers no return form");
        fulfillments.delivered(s.owner, s.storeId, fid);
        b.assertPage(b.get("/account/orders/" + o.id()), "Request a return");
    }

    /// The checkout page's idempotency key.
    static String key(String html) {
        int at = html.indexOf("name=\"idempotencyKey\"");
        int v = html.indexOf("value=\"", at) + 7;
        return html.substring(v, html.indexOf('"', v));
    }

    @SuppressWarnings("unused")
    private static Map<String, String> none() {
        return null;
    }
}
