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

import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.pdf.PdfReader;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.docs.Code128;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every printable document is a real PDF from a real address, with the
/// order's own numbers in it, and only for those allowed to see it.
@BackendTest
class DocumentsTest {
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
    private Orders orders;
    @Autowired
    private Fulfillments fulfillments;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;

    /// The text of the PDF a request answers, after checking it is one.
    private String pdf(MockRequestBuilder request) throws Exception {
        MockResponse r = mvc.perform(request).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        assertTrue(r.getContentType().startsWith("application/pdf"), r.getContentType());
        byte[] bytes = r.getContentAsByteArray();
        assertEquals("%PDF-", new String(bytes, 0, 5, "ISO-8859-1"));
        return PdfReader.open(bytes).extractText();
    }

    private int statusOf(MockRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    @Test
    void orderDocuments() throws Exception {
        Shop s = new Shop("docs-shop", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String user = People.customer(accounts, "docs-buyer");
        OrderDto o = s.buy(user, 2);
        String base = "/api/merchant/stores/" + s.storeId + "/orders/" + o.id();

        String invoice = pdf(People.asMerchant(get(base + "/invoice.pdf"), s.owner));
        assertTrue(invoice.contains("#" + o.number()), invoice);
        assertTrue(invoice.contains("$41.59"), invoice);
        assertTrue(invoice.contains("$19.99"), invoice);
        assertTrue(invoice.contains("Ada Lovelace"), invoice);
        assertTrue(invoice.contains("Authorized"), invoice);

        String mine = pdf(People.asCustomer(get("/api/orders/" + o.id() + "/invoice.pdf"), user));
        assertTrue(mine.contains("$41.59"), mine);
        String other = People.customer(accounts, "docs-other");
        assertEquals(404, statusOf(People.asCustomer(get("/api/orders/" + o.id()
                + "/invoice.pdf"), other)));
        assertEquals(404, statusOf(People.asMerchant(get(base + "/invoice.pdf"), other)));

        String slip = pdf(People.asMerchant(get(base + "/packing-slip.pdf"), s.owner));
        assertTrue(slip.contains(s.sku), slip);
        assertTrue(slip.contains("0123456789012"), slip);
        assertTrue(!slip.contains("$"), "no prices on a packing slip: " + slip);

        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(orders.detailOf(o.id()).lines().get(0).id(), null, null, 2));
        FulfillmentDto f = fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, "SHIP", ls));
        String labelPath = "/api/merchant/stores/" + s.storeId + "/fulfillments/" + f.id()
                + "/label.pdf";
        assertEquals(409, statusOf(People.asMerchant(get(labelPath), s.owner)));
        f = fulfillments.buyLabel(s.owner, s.storeId, f.id(), new LabelRequestDto("GROUND"));
        String label = pdf(People.asMerchant(get(labelPath), s.owner));
        assertTrue(label.contains(f.trackingNumber()), label);
        assertTrue(label.contains("GROUND"), label);
        assertTrue(label.contains("Albany"), label);

        fulfillments.ship(s.owner, s.storeId, f.id());
        List<RefundLineDto> back = new ArrayList<RefundLineDto>();
        back.add(new RefundLineDto(ls.get(0).orderLineId(), 1));
        orders.refund(s.owner, s.storeId, o.id(), new RefundRequestDto(back, 0, true, "Chipped"));
        Map refund = db.queryOne("SELECT id FROM refund WHERE order_id = ?",
                new Object[] {o.id()});
        String note = pdf(People.asMerchant(get("/api/merchant/stores/" + s.storeId + "/refunds/"
                + refund.get("id") + "/credit-note.pdf"), s.owner));
        assertTrue(note.contains("$10.40"), note);
        assertTrue(note.contains("Chipped"), note);
        assertTrue(note.contains("#" + o.number()), note);

        String invoiceAfter = pdf(People.asMerchant(get(base + "/invoice.pdf"), s.owner));
        assertTrue(invoiceAfter.contains("Refunded"), invoiceAfter);
    }

    @Test
    void purchaseOrderAndShiftReport() throws Exception {
        Shop s = new Shop("docs-po-shop", 0, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String supplier = Ids.next();
        db.execute("INSERT INTO supplier (id, store_id, name, contact, email, phone, address, "
                + "note) VALUES (?, ?, 'Clayworks Ltd', 'Mo', 'mo@clay.example', '', "
                + "'4 Kiln Lane', '')", new Object[] {supplier, s.storeId});
        String po = Ids.next();
        db.execute("INSERT INTO purchase_order (id, store_id, number, supplier_id, location_id, "
                + "status, expected_at, note, total_cents, created_at, updated_at) VALUES "
                + "(?, ?, 17, ?, ?, 'ORDERED', 0, 'Fragile', 4800, 0, 0)",
                new Object[] {po, s.storeId, supplier, s.locationId});
        db.execute("INSERT INTO purchase_line (id, purchase_order_id, variant_id, quantity, "
                + "received, cost_cents) VALUES (?, ?, ?, 12, 0, 400)",
                new Object[] {Ids.next(), po, s.variantId});
        String text = pdf(People.asMerchant(get("/api/merchant/stores/" + s.storeId
                + "/purchase-orders/" + po + "/po.pdf"), s.owner));
        assertTrue(text.contains("PO-17"), text);
        assertTrue(text.contains("Clayworks Ltd"), text);
        assertTrue(text.contains("$48.00"), text);

        String register = Ids.next();
        db.execute("INSERT INTO register (id, store_id, location_id, name, active) VALUES "
                + "(?, ?, ?, 'Till 1', 1)", new Object[] {register, s.storeId, s.locationId});
        String shift = Ids.next();
        db.execute("INSERT INTO shift (id, register_id, store_id, username, status, "
                + "opening_cents, cash_sales_cents, card_sales_cents, other_sales_cents, "
                + "cash_moves_cents, expected_cents, counted_cents, sales_count, note, opened_at, "
                + "closed_at) VALUES (?, ?, ?, ?, 'CLOSED', 10000, 12345, 67800, 0, -2000, "
                + "20345, 20300, 9, '', 1763640000000, 1763668800000)",
                new Object[] {shift, register, s.storeId, s.owner});
        String z = pdf(People.asMerchant(get("/api/merchant/stores/" + s.storeId + "/shifts/"
                + shift + "/report.pdf"), s.owner));
        assertTrue(z.contains("Z REPORT"), z);
        assertTrue(z.contains("Till 1"), z);
        assertTrue(z.contains("$801.45"), z);
        assertTrue(z.contains("Short"), z);
        assertTrue(z.contains("$0.45"), z);
        String stranger = People.customer(accounts, "docs-po-stranger");
        assertEquals(404, statusOf(People.asMerchant(get("/api/merchant/stores/" + s.storeId
                + "/shifts/" + shift + "/report.pdf"), stranger)));
    }

    @Test
    void code128Checksum() {
        // Start B (104) + P*1 + J*2 + J*3 + 1*4 + 2*5 + 3*6 + C*7 = 879; 879 % 103 = 55.
        assertEquals(55, Code128.checksum("PJJ123C"));
        assertEquals(104 % 103, Code128.checksum(""));
        int[] values = Code128.encode("AB");
        assertEquals(5, values.length);
        assertEquals(Code128.START_B, values[0]);
        assertEquals(33, values[1]);
        assertEquals(34, values[2]);
        assertEquals((104 + 33 + 2 * 34) % 103, values[3]);
        assertEquals(Code128.STOP, values[4]);
        // Each character is 11 modules; the stop is 13.
        assertEquals(11 * 4 + 13, Code128.modules("AB").length());
        assertTrue(Code128.modules("AB").startsWith("11010010000"), "start B is 211214");
        assertTrue(Code128.modules("AB").endsWith("1100011101011"), "the stop is 2331112");
    }
}
