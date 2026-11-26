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
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The HTML checkout: a guest buys from the session cart, with every refusal
/// shown on the form and the confirmation behind its link token.
@BackendTest
class WebCheckoutTest {
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

    private final MailSender mail = MailSender.current();

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    /// A guest browser with `qty` mugs in its cart, on the checkout page.
    private Browser guest(Shop s, String handle, int qty) throws Exception {
        Browser b = new Browser(mvc);
        MockResponse added = b.send(Browser.form("/s/" + handle + "/cart/add", "variantId",
                s.variantId, "quantity", Integer.toString(qty)));
        assertEquals(303 == added.getStatus() || 302 == added.getStatus(), true,
                "add answered " + added.getStatus());
        MockResponse page = b.get("/s/" + handle + "/checkout");
        b.assertPage(page, "Place order");
        assertEquals("private, no-store", page.getHeader("Cache-Control"));
        String csp = page.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        assertTrue(csp.contains("nonce-"), csp);
        assertNotNull(b.csrf, "the form carries a CSRF token");
        return b;
    }

    private static String key(String html) {
        String marker = "name=\"idempotencyKey\" value=\"";
        int at = html.indexOf(marker);
        return html.substring(at + marker.length(), html.indexOf('"', at + marker.length()));
    }

    private static String[] address(String key, String... more) {
        String[] base = {"email", "ada@example.com", "name", "Ada Lovelace", "line1", "1 Main St",
            "city", "Albany", "region", "NY", "postal", "12207", "country", "US",
            "cardNumber", "4242 4242 4242 4242", "cardExpiry", "12/39", "cardCvc", "123",
            "idempotencyKey", key};
        java.util.List<String> all = new java.util.ArrayList<String>(java.util.Arrays.asList(base));
        for (int i = 0; i < more.length; i += 2) {
            int at = all.indexOf(more[i]);
            if (at >= 0 && at % 2 == 0) {
                all.set(at + 1, more[i + 1]);
            } else {
                all.add(more[i]);
                all.add(more[i + 1]);
            }
        }
        return all.toArray(new String[0]);
    }

    private int committed(Shop s) throws Exception {
        return (int) Rows.number(db.queryOne("SELECT committed FROM inventory_level "
                + "WHERE variant_id = ? AND location_id = ?",
                new Object[] {s.variantId, s.locationId}).get("committed"));
    }

    private long orders(Shop s) throws Exception {
        return Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM store_order WHERE store_id = ?",
                new Object[] {s.storeId}).get("n"));
    }

    @Test
    void aGuestBuysThroughTheHtmlCheckout() throws Exception {
        Shop s = shop("wc-guest");
        Browser b = guest(s, "wc-guest", 2);
        String key = key(b.last.getContentAsString());
        // htmx settles a swap by copying attributes with setAttribute; a
        // `style` copied that way is an inline style attribute the policy
        // refuses (anything that touched an input's style -- an extension,
        // an automation tool hiding the caret -- made it fail at random).
        String settle = b.last.getContentAsString().replace("&quot;", "\"");
        int settleAt = settle.indexOf("\"attributesToSettle\":[");
        assertTrue(settleAt > 0, "the checkout names what htmx settles");
        assertTrue(settle.substring(settleAt, settle.indexOf(']', settleAt)).indexOf("style") < 0, "never style");
        mail.clearSent();
        MockResponse placed = b.post("/s/wc-guest/checkout/place", address(key));
        assertEquals(303, placed.getStatus(), placed.getContentAsString());
        String location = placed.getRedirectedUrl();
        // No String.matches: the compiled run uses the device class library, which lacks it.
        int at = location.indexOf("/s/wc-guest/orders/");
        int q = location.indexOf("?t=");
        assertTrue(at >= 0 && q > at + 19, location);
        for (int i = at + 19; i < q; i++) {
            assertTrue(Character.isDigit(location.charAt(i)), location);
        }
        assertEquals(32, location.length() - q - 3, location);
        assertEquals(2, committed(s), "the mugs are reserved");

        MockResponse confirmation = b.get(location.substring(location.indexOf("/s/")));
        b.assertPage(confirmation, "Thank you! Order #");
        String html = confirmation.getContentAsString();
        assertTrue(html.contains("ada@example.com"));
        assertTrue(html.contains("invoice.pdf?t="));
        assertEquals("private, no-store", confirmation.getHeader("Cache-Control"));

        String invoice = location.substring(location.indexOf("/s/")).replace("?t=",
                "/invoice.pdf?t=");
        MockResponse pdf = b.get(invoice);
        assertEquals(200, pdf.getStatus());
        assertEquals("application/pdf", pdf.getContentType());

        boolean confirmed = false;
        for (Mail m : mail.sent()) {
            confirmed |= m.getTo().toString().contains("ada@example.com")
                    && m.getSubject().contains("Order #");
        }
        assertTrue(confirmed, "the confirmation mail went to the guest");
        // The cart was emptied.
        assertTrue(b.get("/s/wc-guest/cart").getContentAsString().contains("Your cart is empty"));
    }

    @Test
    void aSecondSubmitIsTheSameOrder() throws Exception {
        Shop s = shop("wc-twice");
        Browser b = guest(s, "wc-twice", 1);
        String key = key(b.last.getContentAsString());
        String csrf = b.csrf;
        MockResponse first = b.post("/s/wc-twice/checkout/place", address(key));
        assertEquals(303, first.getStatus(), first.getContentAsString());
        b.csrf = csrf;
        MockResponse second = b.post("/s/wc-twice/checkout/place", address(key));
        assertEquals(303, second.getStatus(), second.getContentAsString());
        assertEquals(first.getRedirectedUrl(), second.getRedirectedUrl());
        assertEquals(1L, orders(s));
        assertEquals(1, committed(s));
    }

    @Test
    void aDiscountCodeIsApplied() throws Exception {
        Shop s = shop("wc-code");
        Browser b = guest(s, "wc-code", 2);
        String key = key(b.last.getContentAsString());
        MockResponse wrong = b.post("/s/wc-code/checkout/codes",
                address(key, "codeAction", "apply-discount", "discountCode", "NOPE"));
        assertEquals(200, wrong.getStatus());
        assertTrue(wrong.getContentAsString().contains("class=\"err\" role=\"alert\""),
                "an unknown code is refused on the form");
        MockResponse applied = b.post("/s/wc-code/checkout/codes",
                address(key, "codeAction", "apply-discount", "discountCode", "WELCOME10"));
        b.assertPage(applied, "WELCOME10");
        assertTrue(applied.getContentAsString().contains("-$2.00"), "10% of $20.00 off");
        MockResponse placed = b.post("/s/wc-code/checkout/place", address(key));
        assertEquals(303, placed.getStatus(), placed.getContentAsString());
        assertEquals(200L, Rows.number(db.queryOne("SELECT discount_cents FROM store_order "
                + "WHERE store_id = ?", new Object[] {s.storeId}).get("discount_cents")));
    }

    @Test
    void anIncompleteAddressIsExplainedFieldByField() throws Exception {
        Shop s = shop("wc-bad");
        Browser b = guest(s, "wc-bad", 1);
        String key = key(b.last.getContentAsString());
        MockResponse refused = b.post("/s/wc-bad/checkout/place", "email", "not-an-email",
                "name", "", "line1", "", "city", "", "country", "US", "idempotencyKey", key);
        assertEquals(200, refused.getStatus());
        String html = refused.getContentAsString();
        assertTrue(html.contains("Enter the email address the receipt goes to"));
        assertTrue(html.contains("Enter the street address"));
        assertTrue(html.contains("Enter the city"));
        assertTrue(html.contains("Enter the card number"));
        assertTrue(html.contains("aria-invalid=\"true\""));
        assertTrue(html.contains("role=\"alert\""));
        assertEquals(0L, orders(s));
        assertEquals(0, committed(s));
        // A declined card is refused before anything is reserved.
        MockResponse declined = b.post("/s/wc-bad/checkout/place",
                address(key, "cardNumber", "4000000000000002"));
        assertEquals(200, declined.getStatus());
        assertTrue(declined.getContentAsString().contains("Your card was declined"));
        assertEquals(0L, orders(s));
        assertEquals(0, committed(s));
    }

    @Test
    void theRatesFollowTheCountry() throws Exception {
        Shop s = shop("wc-rates");
        Browser b = guest(s, "wc-rates", 1);
        String key = key(b.last.getContentAsString());
        MockResponse us = b.post("/s/wc-rates/checkout", address(key));
        b.assertPage(us, "Standard");
        assertTrue(us.getContentAsString().contains("name=\"rateId\""));
        assertEquals("private, no-store", us.getHeader("Cache-Control"));
    }

    @Test
    void aPostWithoutTheCsrfTokenIsRefused() throws Exception {
        Shop s = shop("wc-csrf");
        Browser b = guest(s, "wc-csrf", 1);
        String key = key(b.last.getContentAsString());
        MockResponse forged = b.postWithoutToken("/s/wc-csrf/checkout/place", address(key));
        assertEquals(403, forged.getStatus());
        assertEquals(0L, orders(s));
    }

    @Test
    void anotherGuestsTokenOpensNothing() throws Exception {
        Shop s = shop("wc-idor");
        Browser one = guest(s, "wc-idor", 1);
        MockResponse a = one.post("/s/wc-idor/checkout/place",
                address(key(one.last.getContentAsString())));
        Browser two = guest(s, "wc-idor", 1);
        MockResponse c = two.post("/s/wc-idor/checkout/place",
                address(key(two.last.getContentAsString())));
        String first = a.getRedirectedUrl();
        String second = c.getRedirectedUrl();
        assertFalse(first.equals(second));
        String firstToken = first.substring(first.indexOf("?t=") + 3);
        String secondPath = second.substring(second.indexOf("/s/"), second.indexOf("?t="));
        // The second order with the first order's token, and with none.
        assertEquals(404, two.get(secondPath + "?t=" + firstToken).getStatus());
        assertEquals(404, two.get(secondPath).getStatus());
        assertEquals(404, two.get(secondPath + "/invoice.pdf?t=" + firstToken).getStatus());
        assertEquals(404, two.get("/s/wc-idor/orders/99?t=" + firstToken).getStatus());
        assertEquals(200, two.get(second.substring(second.indexOf("/s/"))).getStatus());
    }

    private void checkoutSettings(Shop s, String methods, String tips, String fieldKind,
            boolean required) throws Exception {
        db.execute("INSERT INTO checkout_settings (store_id, methods, tips_enabled, tip_presets, "
                + "updated_at) VALUES (?, ?, ?, ?, 0)",
                new Object[] {s.storeId, methods, tips.length() > 0 ? 1 : 0, tips});
        if (fieldKind != null) {
            db.execute("INSERT INTO checkout_field (id, store_id, sort_order, label, kind, "
                    + "choices, required, created_at) VALUES (?, ?, 0, 'Gate code', ?, '', ?, 0)",
                    new Object[] {Ids.next(), s.storeId, fieldKind, required ? 1 : 0});
        }
    }

    private String orderColumn(Shop s, String column) throws Exception {
        return String.valueOf(db.queryOne("SELECT " + column + " FROM store_order "
                + "WHERE store_id = ?", new Object[] {s.storeId}).get(column));
    }

    @Test
    void theTestWalletIsOfferedWhenNothingMustBeAsked() throws Exception {
        Shop s = shop("wc-wallet");
        checkoutSettings(s, "", "", null, false);
        Browser b = guest(s, "wc-wallet", 1);
        String html = b.last.getContentAsString();
        assertTrue(html.contains("Pay with the test wallet"), "the simulated wallet is offered");
        assertFalse(b.last.getHeader("Content-Security-Policy").contains("stripe.com"));
    }

    @Test
    void aTipAndTheStoresQuestionGoThroughTheHtmlCheckout() throws Exception {
        Shop s = shop("wc-tip");
        checkoutSettings(s, "", "p1500", "text", true);
        Browser b = guest(s, "wc-tip", 2);
        String html = b.last.getContentAsString();
        assertTrue(html.contains("Add a tip"), "the tip presets are offered");
        assertTrue(html.contains("Gate code *"), "the required question is asked");
        // A wallet sheet cannot ask the store's required question, so the
        // express button stays off; and the simulated processor's policy never
        // names Stripe.
        assertFalse(html.contains("Pay with the test wallet"),
                "no express wallet while a required question must be answered");
        assertFalse(b.last.getHeader("Content-Security-Policy").contains("stripe.com"));
        String key = key(html);
        MockResponse missing = b.post("/s/wc-tip/checkout/place", address(key, "tip", "1"));
        assertFalse(missing.getStatus() == 303, "an unanswered required question is refused");
        assertEquals(0L, orders(s));
        MockResponse placed = b.post("/s/wc-tip/checkout/place",
                address(key, "tip", "1", "f0", "4321#"));
        assertEquals(303, placed.getStatus(), placed.getContentAsString());
        assertTrue(Long.parseLong(orderColumn(s, "tip_cents")) > 0,
                "the tip is kept beside the total");
        String location = placed.getRedirectedUrl();
        String confirmation = b.get(location.substring(location.indexOf("/s/")))
                .getContentAsString();
        assertTrue(confirmation.contains("Gate code"), confirmation);
        assertTrue(confirmation.contains("4321#"), "the answer shows on the order");
    }

    @Test
    void aPayLaterOrderWaitsUntilTheBankAnswers() throws Exception {
        Shop s = shop("wc-later");
        checkoutSettings(s, "klarna", "", null, false);
        Browser b = guest(s, "wc-later", 3);
        assertTrue(b.last.getContentAsString().contains("Klarna"), "Klarna is offered in USD");
        MockResponse placed = b.post("/s/wc-later/checkout/place",
                address(key(b.last.getContentAsString()), "payType", "klarna",
                    "cardNumber", "", "cardExpiry", "", "cardCvc", ""));
        assertEquals(303, placed.getStatus(), placed.getContentAsString());
        assertEquals("PENDING", orderColumn(s, "financial"));
        assertEquals(3, committed(s), "the stock is held while the payment is pending");
        String location = placed.getRedirectedUrl();
        String page = b.get(location.substring(location.indexOf("/s/"))).getContentAsString();
        assertTrue(page.contains("Finish paying"), page);
        String marker = "action=\"";
        int at = page.indexOf(marker, page.indexOf("pay-pending"));
        String action = page.substring(at + marker.length(),
                page.indexOf('"', at + marker.length())).replace("&amp;", "&");
        MockResponse declined = b.post(action, "outcome", "decline");
        assertEquals(303, declined.getStatus(), declined.getContentAsString());
        assertEquals("CANCELLED", orderColumn(s, "status"));
        assertEquals(0, committed(s), "a failed payment releases the stock");
    }
}
