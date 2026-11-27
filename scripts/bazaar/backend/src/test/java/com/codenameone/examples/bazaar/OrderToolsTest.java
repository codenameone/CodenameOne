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
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.BulkActionDto;
import com.codenameone.examples.bazaar.api.BulkActionResultDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.DraftOrderDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderSettingsDto;
import com.codenameone.examples.bazaar.api.PayLinkDto;
import com.codenameone.examples.bazaar.api.PayLinkRequestDto;
import com.codenameone.examples.bazaar.api.TemplateDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.docs.Documents;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.notify.Templates;
import com.codenameone.examples.bazaar.order.BulkOrders;
import com.codenameone.examples.bazaar.order.OrderSettings;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.order.PayLinks;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Archiving, bulk actions, pay links, notification templates, return rules
/// and label sheets.
@BackendTest
class OrderToolsTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    @Autowired
    private Orders orders;
    @Autowired
    private BulkOrders bulk;
    @Autowired
    private PayLinks payLinks;
    @Autowired
    private Templates templates;
    @Autowired
    private OrderSettings settings;
    @Autowired
    private Documents documents;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
    }

    private static int refused(Executable call) {
        return assertThrows(ResponseStatusException.class, call).getStatus();
    }

    private static boolean listed(List<OrderDto> list, String id) {
        for (int iter = 0; iter < list.size(); iter++) {
            if (id.equals(list.get(iter).id())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void archivingMovesAnOrderOutOfTheList() throws Exception {
        Shop s = shop("archive-shop", 5);
        OrderDto o = s.buy(People.customer(accounts, "archive-buyer"), 1);
        orders.archive(s.owner, s.storeId, o.id(), true);
        assertFalse(listed(orders.list(s.owner, s.storeId, null, null, null, null, null, null, null), o.id()));
        assertTrue(listed(orders.archived(s.owner, s.storeId), o.id()));
        orders.archive(s.owner, s.storeId, o.id(), false);
        assertTrue(listed(orders.list(s.owner, s.storeId, null, null, null, null, null, null, null), o.id()));
        String stranger = People.customer(accounts, "archive-stranger");
        assertEquals(404, refused(() -> orders.archive(stranger, s.storeId, o.id(), true)));
    }

    @Test
    void bulkActionsAnswerPerOrder() throws Exception {
        Shop s = shop("bulk-shop", 10);
        OrderDto a = s.buy(People.customer(accounts, "bulk-a"), 1);
        OrderDto b = s.buy(People.customer(accounts, "bulk-b"), 1);
        List<BulkActionResultDto> r = bulk.run(s.owner, s.storeId, new BulkActionDto(
                Arrays.asList(a.id(), b.id(), "nosuchorder"), BulkOrders.TAG, "vip"));
        assertEquals(3, r.size());
        assertTrue(r.get(0).ok());
        assertTrue(r.get(1).ok());
        assertFalse(r.get(2).ok());
        assertTrue(BulkOrders.hasTag(orders.detail(s.owner, s.storeId, a.id()).tags(), "VIP"));
        // Tagging twice does not add the tag twice.
        bulk.run(s.owner, s.storeId, new BulkActionDto(Collections.singletonList(a.id()),
                BulkOrders.TAG, "vip"));
        assertEquals("vip", orders.detail(s.owner, s.storeId, a.id()).tags().trim());
        bulk.run(s.owner, s.storeId, new BulkActionDto(Arrays.asList(a.id(), b.id()),
                BulkOrders.ARCHIVE, null));
        assertEquals(2, orders.archived(s.owner, s.storeId).size());
        assertEquals(400, refused(() -> bulk.run(s.owner, s.storeId, new BulkActionDto(
                Collections.singletonList(a.id()), "EXPLODE", null))));
    }

    @Test
    void aPayLinkResolvesOnlyUntampered() throws Exception {
        Shop s = shop("paylink-shop", 5);
        OrderDto draft = orders.createDraft(s.owner, s.storeId, new DraftOrderDto(
                "buyer@example.com", Collections.singletonList(new CartLineChangeDto(
                        s.variantId, 1)), null, null, "phone sale"));
        PayLinkDto link = payLinks.create(s.owner, s.storeId, draft.id(),
                new PayLinkRequestDto(3));
        String token = link.url().substring(link.url().lastIndexOf('/') + 1);
        assertEquals(draft.id(), payLinks.resolve(token).getId());
        String forged = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");
        assertEquals(404, refused(() -> payLinks.resolve(forged)));
        // A second link replaces the first.
        PayLinkDto again = payLinks.create(s.owner, s.storeId, draft.id(),
                new PayLinkRequestDto(3));
        assertEquals(404, refused(() -> payLinks.resolve(token)));
        assertEquals(again.url(), payLinks.activeUrl(draft.id()));
    }

    @Test
    void templatesKeepTheirPlaceholders() throws Exception {
        Shop s = shop("template-shop", 1);
        TemplateDto saved = templates.save(s.owner, s.storeId, new TemplateDto(
                "mail.paylink.title", null, null, "en", "Pay for it here", null, null, false));
        assertTrue(saved.custom());
        assertEquals("Pay for it here", templates.overrides(s.storeId, "en")
                .get("mail.paylink.title"));
        assertEquals(400, refused(() -> templates.save(s.owner, s.storeId, new TemplateDto(
                "mail.paylink.title", null, null, "en", "Order {7}", null, null, false))));
        TemplateDto shown = templates.preview(s.owner, s.storeId, new TemplateDto(
                "mail.paylink.body", null, null, "en", "{0} made #{1}", null, null, false));
        assertTrue(shown.body().indexOf("{0}") < 0);
        templates.reset(s.owner, s.storeId, new TemplateDto("mail.paylink.title", null, null,
                "en", null, null, null, false));
        assertFalse(templates.overrides(s.storeId, "en").containsKey("mail.paylink.title"));
    }

    @Test
    void returnRulesAreSavedAndFeesCapped() throws Exception {
        Shop s = shop("rules-shop", 1);
        settings.save(s.owner, s.storeId, new OrderSettingsDto(true, 14, 1500, 500,
                s.productId, ""));
        OrderSettingsDto r = settings.of(s.storeId);
        assertEquals(14, r.returnWindowDays());
        assertTrue(r.autoArchive());
        assertTrue(settings.finalSale(r, s.productId));
        assertEquals(150, OrderSettings.restockingFee(1000, 1500));
        assertEquals(0, OrderSettings.restockingFee(1000, 0));
    }

    @Test
    void labelSheetsArePdfs() throws Exception {
        Shop s = shop("label-shop", 1);
        byte[] avery = documents.labelSheet(s.storeId, Collections.singletonList(s.variantId),
                Collections.singletonList(Integer.valueOf(31)), "AVERY_30");
        assertEquals("%PDF", new String(avery, 0, 4, "US-ASCII"));
        byte[] thermal = documents.labelSheet(s.storeId,
                Collections.singletonList(s.variantId),
                Collections.singletonList(Integer.valueOf(2)), "THERMAL_2X1");
        assertEquals("%PDF", new String(thermal, 0, 4, "US-ASCII"));
    }
}
