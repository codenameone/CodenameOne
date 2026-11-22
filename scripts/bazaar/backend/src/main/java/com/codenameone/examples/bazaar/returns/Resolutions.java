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
package com.codenameone.examples.bazaar.returns;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CreditAdjustDto;
import com.codenameone.examples.bazaar.api.DraftOrderDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PayLinkRequestDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.customer.Customers;
import com.codenameone.examples.bazaar.domain.ReturnRequest;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.order.PayLinks;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/// How a received return is settled, by the resolution the shopper chose:
///
/// - `REFUND`: the goods' value, less the store's fees, back to the card.
/// - `STORE_CREDIT`: the same amount added to the customer's store credit,
///   nothing back to the card.
/// - `EXCHANGE`: a replacement order for the chosen variant, paid with the
///   goods' value less the fees. When the replacement costs less the rest
///   goes back to the card; when it costs more the shopper gets a checkout
///   link for the difference and the replacement ships once it is paid.
///
/// Every path goes through the order's own refund, which keeps the fees and
/// the exchange credit back from what it pays out, so the order's totals and
/// the card always agree.
@Component
public class Resolutions {
    public static final String REFUND = "REFUND";
    public static final String STORE_CREDIT = "STORE_CREDIT";
    public static final String EXCHANGE = "EXCHANGE";
    static final int EXCHANGE_LINK_DAYS = 14;

    private final DataSource db;
    private final Orders orders;
    private final StoreOrderRepository orderRows;
    private final Customers customers;
    private final PayLinks payLinks;

    public Resolutions(DataSource db, Orders orders, StoreOrderRepository orderRows,
            Customers customers, PayLinks payLinks) {
        this.db = db;
        this.orders = orders;
        this.orderRows = orderRows;
        this.customers = customers;
        this.payLinks = payLinks;
    }

    /// Settles `r` (claimed REFUNDED by the caller) by its resolution.
    public void settle(String username, String storeId, ReturnRequest r, RefundRequestDto req)
            throws IOException {
        Map row = db.queryOne("SELECT resolution, exchange_variant_id, fee_cents FROM "
                + "return_request WHERE id = ?", new Object[] {r.getId()});
        String resolution = row == null ? REFUND : Rows.text(row.get("resolution"));
        long fee = row == null ? 0 : Rows.number(row.get("fee_cents"));
        if (STORE_CREDIT.equals(resolution)) {
            long[] parts = orders.refundParts(username, storeId, r.getOrderId(), req, true, fee,
                    StoreAccess.MANAGERS);
            StoreOrder o = orderRows.findById(r.getOrderId());
            if (parts[0] > 0 && o.getCustomerId().length() > 0) {
                customers.adjustCredit(username, storeId, o.getCustomerId(),
                        new CreditAdjustDto(parts[0], "Return " + r.getId(), 0));
            }
            return;
        }
        if (EXCHANGE.equals(resolution)) {
            exchange(username, storeId, r, req, fee, Rows.text(row.get("exchange_variant_id")));
            return;
        }
        orders.refundParts(username, storeId, r.getOrderId(), req, false, fee,
                StoreAccess.MANAGERS);
    }

    private void exchange(String username, String storeId, ReturnRequest r,
            RefundRequestDto req, long fee, String variantId) throws IOException {
        StoreOrder original = orderRows.findById(r.getOrderId());
        int units = 0;
        for (int iter = 0; iter < req.lines().size(); iter++) {
            units += req.lines().get(iter).quantity();
        }
        long credit = Math.max(0, orders.quoteRefund(original.getId(), req) - fee);
        AddressDto to = new AddressDto(original.getShipName(), original.getShipLine1(),
                original.getShipLine2(), original.getShipCity(), original.getShipRegion(),
                original.getShipPostal(), original.getShipCountry(), original.getShipPhone(),
                0, 0);
        OrderDto draft = orders.createDraft(username, storeId, new DraftOrderDto(
                original.getEmail(), Collections.singletonList(new CartLineChangeDto(variantId,
                        units)), to, null, "Exchange for return " + r.getId()));
        StoreOrder replacement = orderRows.findById(draft.id());
        if (original.getUsername().length() > 0) {
            replacement.setUsername(original.getUsername());
            replacement.setCustomerId(original.getCustomerId());
            orderRows.save(replacement);
        }
        long applied = Math.min(credit, replacement.getTotalCents());
        orders.refundParts(username, storeId, original.getId(), req, false, fee + applied,
                StoreAccess.MANAGERS);
        orders.convert(orderRows.findById(replacement.getId()), username);
        if (applied > 0) {
            orders.partPay(replacement.getId(), EXCHANGE, applied, "return-" + r.getId(),
                    username);
        }
        db.execute("UPDATE return_request SET replacement_order_id = ? WHERE id = ?",
                new Object[] {replacement.getId(), r.getId()});
        orders.event(original.getId(), "EXCHANGED", "Exchanged for order #"
                + replacement.getNumber(), username);
        if (applied < replacement.getTotalCents()) {
            // The replacement costs more than the credit: the shopper pays
            // the difference through a link, by mail.
            payLinks.create(username, storeId, replacement.getId(),
                    new PayLinkRequestDto(EXCHANGE_LINK_DAYS));
        }
    }

    /// Ships only what is sold here: a resolution named in a request.
    static String check(String resolution) {
        String r = resolution == null || resolution.length() == 0 ? REFUND : resolution;
        if (!REFUND.equals(r) && !STORE_CREDIT.equals(r) && !EXCHANGE.equals(r)) {
            throw new ResponseStatusException(400, "A return is a refund, store credit or an "
                    + "exchange");
        }
        return r;
    }
}
