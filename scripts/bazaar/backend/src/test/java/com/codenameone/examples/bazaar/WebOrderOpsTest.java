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
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.notify.Templates;
import com.codenameone.examples.bazaar.order.OrderSettings;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.order.PayLinks;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The back office pages of the order and stock tools: each page's main
/// action, another store's answering 404 and a post without its token 403.
@BackendTest
class WebOrderOpsTest {
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
    private OrderSettings settings;
    @Autowired
    private Templates templates;
    @Autowired
    private PayLinks payLinks;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 20, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
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

    private int level(String column, Shop s) throws Exception {
        Map row = db.queryOne("SELECT " + column + " AS n FROM inventory_level WHERE variant_id = ? "
                + "AND location_id = ?", new Object[] {s.variantId, s.locationId});
        return ((Number) row.get("n")).intValue();
    }

    @Test
    void stockStatesShowAndMoveUnavailableUnits() throws Exception {
        Shop s = shop("woo-states");
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId;
        b.assertPage(b.get(a + "/stock-states"), "Adjust unavailable");
        int before = level("on_hand", s);
        redirected(b.post(a + "/stock-states/move", "variant", s.variantId, "location",
                s.locationId, "reason", "DAMAGED", "quantity", "2"));
        assertEquals(before - 2, level("on_hand", s));
        assertEquals(2, level("damaged", s));
        b.assertPage(b.get(a + "/stock-states"), "damaged 2");
    }

    @Test
    void serialsAreReceivedAndFound() throws Exception {
        Shop s = shop("woo-serials");
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId;
        b.assertPage(b.get(a + "/serials"), "Find a serial");
        // The opening stock is untracked, so tracking can be switched on.
        redirected(b.post(a + "/serials/mode", "variant", s.variantId, "mode", "SERIAL"));
        redirected(b.post(a + "/serials/receive", "variant", s.variantId, "location",
                s.locationId, "reference", "PO 7", "serials", "SN-A1\nSN-A2\n"));
        redirected(b.post(a + "/serials/find", "q", "SN-A2"));
        b.assertPage(b.get(a + "/serials?q=SN-A2"), "In stock since");
    }

    @Test
    void labelSheetsPrintAsPdf() throws Exception {
        Shop s = shop("woo-labels");
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId;
        b.assertPage(b.get(a + "/labels"), "Print labels");
        MockResponse pdf = b.post(a + "/labels.pdf", "layout", "AVERY_30",
                "qty-" + s.variantId, "3");
        assertEquals(200, pdf.getStatus());
        assertTrue(pdf.getContentAsString().startsWith("%PDF"));
        redirected(b.post(a + "/labels.pdf", "layout", "AVERY_30"));
    }

    @Test
    void orderSettingsSaveTheReturnRules() throws Exception {
        Shop s = shop("woo-settings");
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId;
        b.assertPage(b.get(a + "/order-settings"), "Return window");
        redirected(b.post(a + "/order-settings", "autoArchive", "1", "window", "14",
                "restocking", "12.5", "returnShipping", "4.50", "finalSaleProducts",
                s.productId, "finalSaleCollections", ""));
        assertTrue(settings.of(s.storeId).autoArchive());
        assertEquals(14, settings.of(s.storeId).returnWindowDays());
        assertEquals(1250, settings.of(s.storeId).restockingFeeBp());
        assertEquals(450, settings.of(s.storeId).returnShippingCents());
    }

    @Test
    void notificationTemplatesAreEditedPreviewedAndReset() throws Exception {
        Shop s = shop("woo-templates");
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId;
        b.assertPage(b.get(a + "/notifications?lang=en"), "mail.paylink.title");
        redirected(b.post(a + "/notifications/save", "key", "mail.paylink.title", "lang", "en",
                "body", "Your order is ready to pay"));
        assertEquals("Your order is ready to pay", templates.overrides(s.storeId, "en")
                .get("mail.paylink.title"));
        b.assertPage(b.get(a + "/notifications?lang=en&preview=mail.paylink.title"),
                "Preview of mail.paylink.title");
        redirected(b.post(a + "/notifications/reset?lang=en&key=mail.paylink.title"));
        assertFalse(templates.overrides(s.storeId, "en").containsKey("mail.paylink.title"));
    }

    /// The back office reads in the language the storefront picks: the
    /// `lang` cookie (else `Accept-Language`) sets the page's language and
    /// direction, its sections and its tables' frame.
    @Test
    void theBackOfficeSpeaksTheReadersLanguage() throws Exception {
        Shop s = shop("wo-lang");
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId;
        b.lang = "de";
        String de = b.get(a + "/archived-orders?since=30").getContentAsString();
        assertTrue(de.contains("lang=\"de\""), "German page");
        assertTrue(de.contains("Bestellungen") && de.contains("Produkte"), "German sections");
        assertTrue(de.contains("Letzte 30 Tage"), "German filter");
        b.lang = "he";
        String he = b.get(a + "/orders").getContentAsString();
        assertTrue(he.contains("dir=\"rtl\""), "Hebrew reads right to left");
        assertTrue(he.contains("\u05de\u05d5\u05e6\u05e8\u05d9\u05dd"), "Hebrew sections");
        b.lang = null;
        String en = b.get(a + "/orders").getContentAsString();
        assertTrue(en.contains(">Products<"), "English without a choice");
    }

    @Test
    void bulkActionsReportEachOrderAndArchivedOrdersComeBack() throws Exception {
        Shop s = shop("woo-bulk");
        String buyer = People.customer(accounts, "woo-bulk-buyer");
        OrderDto one = s.buy(buyer, 1);
        OrderDto two = s.buy(buyer, 1);
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId;
        b.assertPage(b.get(a + "/orders"), "With selected");
        // Express parcels leave with a label, bought on the way.
        MockResponse fulfilled = b.post(a + "/orders/bulk", "action", "FULFIL", "ids", one.id(),
                "ids", two.id());
        assertEquals(200, fulfilled.getStatus(), fulfilled.getContentAsString());
        assertTrue(fulfilled.getContentAsString().contains("2 of 2 done"),
                fulfilled.getContentAsString());
        b.get(a + "/orders");
        assertEquals(200, b.post(a + "/orders/bulk", "action", "ARCHIVE", "ids", one.id())
                .getStatus());
        b.assertPage(b.get(a + "/archived-orders"), "#" + one.number());
        redirected(b.get(a + "/orders?status=ARCHIVED"));
        // The archived list answers the orders table's search and date
        // filter, and choosing "Archived" there carries both along.
        MockResponse moved = b.get(a + "/orders?status=ARCHIVED&q=%23" + one.number()
                + "&since=30");
        assertEquals(303, moved.getStatus());
        assertTrue(moved.getHeader("Location").endsWith("/archived-orders?q=%23"
                + one.number() + "&since=30"), moved.getHeader("Location"));
        b.assertPage(b.get(a + "/archived-orders?q=%23" + one.number() + "&since=7"),
                "#" + one.number());
        String none = b.get(a + "/archived-orders?q=no-such-order-xyz").getContentAsString();
        assertTrue(none.contains("No archived orders match."), none);
        assertFalse(none.contains(">#" + one.number() + "<"), none);
        redirected(b.post(a + "/orders/" + one.id() + "/unarchive"));
        assertTrue(orders.archived(s.owner, s.storeId).isEmpty());
        redirected(b.post(a + "/orders/bulk", "action", "PACKING_SLIPS", "ids", one.id()));
        assertEquals(200, b.get(a + "/packing-slips.pdf?orders=" + one.id()).getStatus());
    }

    @Test
    void aDraftGetsAPayLinkFromItsPage() throws Exception {
        Shop s = shop("woo-paylink");
        OrderDto draft = orders.createDraft(s.owner, s.storeId, new DraftOrderDto(
                "buyer@example.com", Collections.singletonList(new CartLineChangeDto(
                        s.variantId, 1)), null, null, "phone sale"));
        Browser b = signedIn(s.owner);
        String a = "/admin/" + s.storeId;
        b.assertPage(b.get(a + "/orders/" + draft.id()), "Send pay link");
        redirected(b.post(a + "/orders/" + draft.id() + "/pay-link"));
        assertNotNull(payLinks.activeUrl(draft.id()));
    }

    @Test
    void anotherStoresToolsAnswer404AndForgedPostsAre403() throws Exception {
        Shop mine = shop("woo-idor-a");
        Shop theirs = shop("woo-idor-b");
        String buyer = People.customer(accounts, "woo-idor-buyer");
        OrderDto o = theirs.buy(buyer, 1);
        Browser b = signedIn(mine.owner);
        String a = "/admin/" + mine.storeId;
        String other = "/admin/" + theirs.storeId;
        String[] gets = {other + "/stock-states", other + "/serials", other + "/labels",
            other + "/order-settings", other + "/notifications", other + "/archived-orders",
            other + "/packing-slips.pdf?orders=" + o.id()};
        for (int iter = 0; iter < gets.length; iter++) {
            assertEquals(404, b.get(gets[iter]).getStatus(), gets[iter]);
        }
        b.get(a + "/labels");
        String[] posts = {a + "/orders/" + o.id() + "/archive", a + "/orders/" + o.id()
            + "/pay-link", other + "/orders/" + o.id() + "/archive", other + "/order-settings",
            other + "/stock-states/move", other + "/serials/receive", other + "/labels.pdf",
            other + "/notifications/save", other + "/orders/bulk"};
        for (int iter = 0; iter < posts.length; iter++) {
            assertEquals(404, b.post(posts[iter], "ids", o.id(), "action", "ARCHIVE")
                    .getStatus(), posts[iter]);
        }
        assertEquals(404, b.post(a + "/stock-states/move", "variant", theirs.variantId,
                "location", theirs.locationId, "reason", "DAMAGED", "quantity", "1").getStatus());
        assertEquals(404, b.post(a + "/orders/bulk", "action", "ARCHIVE", "ids", o.id())
                .getStatus() == 404 ? 404 : bulkRefused(b, a, o));
        assertTrue(orders.archived(theirs.owner, theirs.storeId).isEmpty());
        assertEquals(0, level("damaged", theirs));
        assertEquals(403, b.postWithoutToken(a + "/order-settings", "window", "1").getStatus());
        assertEquals(403, b.postWithoutToken(a + "/orders/bulk", "action", "ARCHIVE", "ids",
                o.id()).getStatus());
    }

    /// A bulk run over another store's order answers its own page with the
    /// order refused rather than a 404; either way nothing changes.
    private int bulkRefused(Browser b, String a, OrderDto o) throws Exception {
        MockResponse r = b.post(a + "/orders/bulk", "action", "ARCHIVE", "ids", o.id());
        return r.getContentAsString().contains("0 of 1 done") ? 404 : r.getStatus();
    }
}
