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
import com.codenameone.examples.bazaar.api.ExchangeChoiceDto;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.OrderSettingsDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.ReturnOptionsDto;
import com.codenameone.examples.bazaar.api.ReturnQuoteDto;
import com.codenameone.examples.bazaar.api.ReturnRequestDto;
import com.codenameone.examples.bazaar.api.ReturnableLineDto;
import com.codenameone.examples.bazaar.api.SelfReturnDto;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.OrderSettings;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Self-serve returns from the shopper's account, under the store's rules
/// ([OrderSettings]): what an order still allows back, what a return would
/// come to, and the return itself, as a refund, store credit or an exchange
/// for another variant of the same product.
///
/// A quote is what the merchant will settle when the goods arrive
/// ([Resolutions]): the goods' value as the order's refund prices it, less a
/// restocking fee (a share of that value) and a flat return shipping fee,
/// neither ever more than the value. An exchange's difference is the
/// replacement's price less that credit: above zero the shopper pays it,
/// below zero it comes back.
@Component
public class SelfReturns {
    private final DataSource db;
    private final Returns returns;
    private final Orders orders;
    private final OrderSettings settings;
    private final Fulfillments fulfillments;
    private final Inventory inventory;
    private final StoreOrderRepository orderRows;
    private final OrderLineRepository lines;
    private final VariantRepository variants;
    private final ProductRepository products;

    public SelfReturns(DataSource db, Returns returns, Orders orders, OrderSettings settings,
            Fulfillments fulfillments, Inventory inventory, StoreOrderRepository orderRows,
            OrderLineRepository lines, VariantRepository variants, ProductRepository products) {
        this.db = db;
        this.returns = returns;
        this.orders = orders;
        this.settings = settings;
        this.fulfillments = fulfillments;
        this.inventory = inventory;
        this.orderRows = orderRows;
        this.lines = lines;
        this.variants = variants;
        this.products = products;
    }

    /// What the caller's order allows back, line by line.
    public ReturnOptionsDto options(String username, String orderId) throws IOException {
        orders.mineDetail(username, orderId);
        StoreOrder o = orderRows.findById(orderId);
        OrderSettingsDto rules = settings.of(o.getStoreId());
        long delivered = fulfillments.deliveredAt(orderId);
        String why = delivered == 0 ? "Nothing of the order has arrived yet"
                : !Returns.withinWindow(delivered, rules) ? "The " + rules.returnWindowDays()
                    + " day return window has closed" : "";
        Map<String, Integer> open = returns.returnable(orderId);
        List<ReturnableLineDto> out = new ArrayList<ReturnableLineDto>();
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine l = ls.get(iter);
            boolean finalSale = settings.finalSale(rules, l.getProductId());
            Integer max = open.get(l.getId());
            if (max == null && !finalSale) {
                continue;
            }
            out.add(new ReturnableLineDto(l.getId(), l.getTitle(), max == null ? 0
                    : max.intValue(), finalSale, unit(orderId, l), finalSale
                        ? new ArrayList<ExchangeChoiceDto>() : choices(o.getStoreId(), l)));
        }
        return new ReturnOptionsDto(why.length() == 0 && !open.isEmpty(), why,
                rules.returnWindowDays(), rules.restockingFeeBp(), rules.returnShippingCents(),
                out);
    }

    /// What a return would come to, without asking for it.
    public ReturnQuoteDto quote(String username, String orderId, SelfReturnDto req)
            throws IOException {
        orders.mineDetail(username, orderId);
        return priced(orderId, req, "", "", "");
    }

    /// Asks for the return, with the resolution and fees of its quote.
    public ReturnQuoteDto request(String username, String orderId, SelfReturnDto req)
            throws IOException {
        orders.mineDetail(username, orderId);
        ReturnQuoteDto q = priced(orderId, req, "", "", "");
        ReturnDto made = returns.request(username, orderId, new ReturnRequestDto(req.reason(),
                req.note(), req.lines()));
        db.execute("UPDATE return_request SET resolution = ?, exchange_variant_id = ?, "
                + "fee_cents = ? WHERE id = ?", new Object[] {q.resolution(),
                    Resolutions.EXCHANGE.equals(q.resolution()) ? req.exchangeVariantId() : "",
                    Long.valueOf(q.feeCents()), made.id()});
        return new ReturnQuoteDto(made.id(), q.valueCents(), q.feeCents(), q.exchangeCents(),
                q.differenceCents(), q.resolution(), made.status(), "");
    }

    private ReturnQuoteDto priced(String orderId, SelfReturnDto req, String id, String status,
            String replacement) throws IOException {
        if (req == null || req.lines() == null || req.lines().isEmpty()) {
            throw new ResponseStatusException(400, "Choose what to send back");
        }
        String resolution = Resolutions.check(req.resolution());
        StoreOrder o = orderRows.findById(orderId);
        OrderSettingsDto rules = settings.of(o.getStoreId());
        Map<String, Integer> open = returns.returnable(orderId);
        List<OrderLine> ls = lines.findByOrderId(orderId);
        List<RefundLineDto> wanted = new ArrayList<RefundLineDto>();
        int units = 0;
        String productId = null;
        for (int iter = 0; iter < req.lines().size(); iter++) {
            FulfilLineDto w = req.lines().get(iter);
            OrderLine line = w == null ? null : find(ls, w.orderLineId());
            if (line == null || w.quantity() <= 0) {
                throw new ResponseStatusException(400, "That line is not on the order");
            }
            if (settings.finalSale(rules, line.getProductId())) {
                throw new ResponseStatusException(409, line.getTitle()
                        + " is final sale and cannot be returned");
            }
            Integer max = open.get(line.getId());
            if (max == null || w.quantity() > max.intValue()) {
                throw new ResponseStatusException(409, "More of " + line.getTitle()
                        + " than can still be returned");
            }
            if (productId != null && !productId.equals(line.getProductId())) {
                productId = "";
            } else if (productId == null) {
                productId = line.getProductId();
            }
            units += w.quantity();
            wanted.add(new RefundLineDto(line.getId(), w.quantity()));
        }
        long value = orders.quoteRefund(orderId, new RefundRequestDto(wanted, 0, false, ""));
        long fee = Math.min(value, OrderSettings.restockingFee(value, rules.restockingFeeBp())
                + rules.returnShippingCents());
        long exchange = 0;
        long difference = fee - value;
        if (Resolutions.EXCHANGE.equals(resolution)) {
            if (productId == null || productId.length() == 0) {
                throw new ResponseStatusException(400, "An exchange swaps one product for "
                        + "another variant of it");
            }
            Variant v = req.exchangeVariantId() == null || req.exchangeVariantId().length() > 32
                    ? null : variants.findById(req.exchangeVariantId());
            if (v == null || !productId.equals(v.getProductId())) {
                throw new ResponseStatusException(400, "Choose another variant of the same "
                        + "product");
            }
            if (inventory.pickLocation(o.getStoreId(), v.getId(), units) == null) {
                throw new ResponseStatusException(409, "That variant is out of stock");
            }
            try {
                exchange = Math.multiplyExact(v.getPriceCents(), (long) units);
                difference = Math.subtractExact(exchange, value - fee);
            } catch (ArithmeticException tooBig) {
                throw new ResponseStatusException(400, "That is too much to exchange");
            }
        }
        return new ReturnQuoteDto(id, value, fee, exchange, difference, resolution, status,
                replacement);
    }

    /// The other variants of the line's product that are in stock.
    private List<ExchangeChoiceDto> choices(String storeId, OrderLine l) throws IOException {
        List<ExchangeChoiceDto> out = new ArrayList<ExchangeChoiceDto>();
        Product p = l.getProductId().length() == 0 ? null : products.findById(l.getProductId());
        if (p == null || !storeId.equals(p.getStoreId())) {
            return out;
        }
        List<Variant> vs = variants.findByProductIdOrderByPositionAsc(p.getId());
        for (int iter = 0; iter < vs.size(); iter++) {
            Variant v = vs.get(iter);
            if (v.getId().equals(l.getVariantId())) {
                continue;
            }
            if (inventory.pickLocation(storeId, v.getId(), 1) != null) {
                out.add(new ExchangeChoiceDto(v.getId(), p.getTitle() + (v.getTitle().length() > 0
                        ? " - " + v.getTitle() : ""), v.getPriceCents(), 1));
            }
        }
        return out;
    }

    /// One unit of the line at its refund price.
    private long unit(String orderId, OrderLine l) throws IOException {
        List<RefundLineDto> one = new ArrayList<RefundLineDto>();
        one.add(new RefundLineDto(l.getId(), 1));
        try {
            return orders.quoteRefund(orderId, new RefundRequestDto(one, 0, false, ""));
        } catch (ResponseStatusException nothingLeft) {
            return 0;
        }
    }

    private static OrderLine find(List<OrderLine> ls, String id) {
        for (int iter = 0; id != null && iter < ls.size(); iter++) {
            if (id.equals(ls.get(iter).getId())) {
                return ls.get(iter);
            }
        }
        return null;
    }
}
