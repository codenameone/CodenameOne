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
package com.codenameone.examples.bazaar.events;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.automation.Automations;
import com.codenameone.examples.bazaar.automation.NodeCatalog;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.webhook.Webhooks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What happened in a store, told to its webhooks ([Webhooks#emit]), its
/// automations ([Automations#fire]) and, by mail, the people it concerns ([Emails]).
///
/// Every method is called after the change it reports committed, beside the
/// live update the change already publishes, and never throws: a webhook or
/// an automation that fails must not undo an order.
@Component
public class StoreEvents {
    private final Webhooks webhooks;
    private final Automations automations;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final CustomerRepository customers;
    private final InventoryLevelRepository levels;
    private final ProductRepository products;
    private final Emails emails;
    private final GrowthEvents growth;

    /// Field-injected: downloads reach the order service, which tells these
    /// events, so a constructor dependency would be a cycle.
    @Autowired
    private com.codenameone.examples.bazaar.catalog.Downloads downloads;

    /// Reads the order's checkout answers (the store's fields and apps'),
    /// which the order webhooks carry as `attributes`.
    @Autowired
    private com.codename1.backend.DataSource db;

    /// The order whose placing is under way on this thread, so the payment
    /// its own digital delivery takes does not mail the links a second time.
    private static final ThreadLocal<String> PLACING = new ThreadLocal<String>();

    public StoreEvents(Webhooks webhooks, Automations automations, StoreOrderRepository orders,
            OrderLineRepository lines, CustomerRepository customers,
            InventoryLevelRepository levels, ProductRepository products, Emails emails,
            GrowthEvents growth) {
        this.growth = growth;
        this.emails = emails;
        this.webhooks = webhooks;
        this.automations = automations;
        this.orders = orders;
        this.lines = lines;
        this.customers = customers;
        this.levels = levels;
        this.products = products;
    }

    /// An order was placed, online, at a register or from a draft: orders/create,
    /// orders/paid when it is paid already, customers/create for a customer's
    /// first order, and inventory/low for every line that took its variant to
    /// the reorder point.
    public void orderPlaced(String orderId) {
        try {
            Snapshot s = snapshot(orderId);
            if (s == null) {
                return;
            }
            order(s, Webhooks.ORDERS_CREATE, NodeCatalog.ORDER_CREATED);
            if ("PAID".equals(s.order.getFinancial())) {
                order(s, Webhooks.ORDERS_PAID, NodeCatalog.ORDER_PAID);
            }
            if (s.customer != null && s.customer.getOrdersCount() == 1
                    && System.currentTimeMillis() - s.customer.getCreatedAt().toEpochMilli()
                        < 120000L) {
                Map<String, Object> payload = customerPayload(s.customer);
                webhooks.emit(s.order.getStoreId(), Webhooks.CUSTOMERS_CREATE, Json.write(payload));
                automations.fire(s.order.getStoreId(), NodeCatalog.CUSTOMER_CREATED,
                        context(s));
            }
            lowStock(s);
            deliverDigital(orderId);
            if (!"POS".equals(s.order.getChannel())) {
                // A register sale gets its receipt when the shopper asks for one.
                emails.orderConfirmation(orderId);
            } else if (!downloads.forOrder(orderId).isEmpty()) {
                // ...but its downloads go out at once.
                emails.downloads(orderId);
            }
            growth.placed(orderId, "PAID".equals(s.order.getFinancial()));
        } catch (IOException | RuntimeException failed) {
            log("orders/create", orderId, failed);
        }
    }

    /// An order's payment was taken.
    public void orderPaid(String orderId) {
        orderEvent(orderId, Webhooks.ORDERS_PAID, NodeCatalog.ORDER_PAID);
        if (!orderId.equals(PLACING.get())) {
            try {
                if (downloads.deliver(orderId) > 0) {
                    emails.downloads(orderId);
                }
            } catch (IOException | RuntimeException failed) {
                log("downloads", orderId, failed);
            }
        }
        growth.paid(orderId);
    }

    /// Delivers a new order's digital lines: takes a held payment for them
    /// and issues their links, which the confirmation then carries.
    private void deliverDigital(String orderId) {
        PLACING.set(orderId);
        try {
            downloads.deliver(orderId);
        } catch (IOException | RuntimeException failed) {
            log("downloads", orderId, failed);
        } finally {
            PLACING.remove();
        }
    }

    /// The last of an order shipped or was handed over.
    public void orderFulfilled(String orderId) {
        orderEvent(orderId, Webhooks.ORDERS_FULFILLED, NodeCatalog.ORDER_FULFILLED);
    }

    /// An order was cancelled. Webhooks only; no automation triggers on it.
    public void orderCancelled(String orderId) {
        orderEvent(orderId, Webhooks.ORDERS_CANCELLED, null);
        try {
            downloads.revokeAll(orderId);
        } catch (IOException | RuntimeException failed) {
            log("downloads", orderId, failed);
        }
        growth.cancelled(orderId);
    }

    /// Money went back on an order.
    public void refunded(String orderId, long amountCents) {
        try {
            Snapshot s = snapshot(orderId);
            if (s != null) {
                Map<String, Object> payload = orderPayload(s);
                payload.put("refundCents", Long.valueOf(amountCents));
                webhooks.emit(s.order.getStoreId(), Webhooks.REFUNDS_CREATE, Json.write(payload));
                emails.refundIssued(orderId);
            }
            downloads.revokeRefunded(orderId);
            growth.refunded(orderId);
        } catch (IOException | RuntimeException failed) {
            log("refunds/create", orderId, failed);
        }
    }

    /// A merchant changed a product.
    public void productUpdated(String storeId, String productId) {
        try {
            Product p = productRead(productId);
            if (p == null) {
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("id", p.getId());
            payload.put("storeId", p.getStoreId());
            payload.put("handle", p.getHandle());
            payload.put("title", p.getTitle());
            payload.put("status", p.getStatus());
            payload.put("tags", p.getTags());
            payload.put("updatedAt", Long.valueOf(p.getUpdatedAt().toEpochMilli()));
            webhooks.emit(storeId, Webhooks.PRODUCTS_UPDATE, Json.write(payload));
        } catch (IOException | RuntimeException failed) {
            log("products/update", productId, failed);
        }
    }

    /// Stock was adjusted by hand or through the API: `inventory/low`, with
    /// the payload an order's sale sends, when `level` is at or under its
    /// reorder point. No automation or mail: those follow sales.
    public void stockAdjusted(String storeId,
            com.codenameone.examples.bazaar.api.LevelDto level) {
        if (level == null || level.reorderPoint() <= 0
                || level.available() > level.reorderPoint()) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("storeId", storeId);
            payload.put("variantId", level.variantId());
            payload.put("locationId", level.locationId());
            payload.put("available", Long.valueOf(level.available()));
            payload.put("reorderPoint", Long.valueOf(level.reorderPoint()));
            payload.put("sku", level.sku());
            webhooks.emit(storeId, Webhooks.INVENTORY_LOW, Json.write(payload));
        } catch (IOException | RuntimeException failed) {
            log("inventory/low", level.variantId(), failed);
        }
    }

    /// A cart sat untouched with items in it. Automations only.
    public void cartAbandoned(Cart cart, Customer customer) {
        Map<String, String> ctx = new HashMap<String, String>();
        ctx.put("cartId", cart.getId());
        ctx.put("username", cart.getUsername());
        ctx.put("email", cart.getEmail().length() > 0 ? cart.getEmail()
                : customer == null ? "" : customer.getEmail());
        if (customer != null) {
            ctx.put("customerId", customer.getId());
        }
        try {
            automations.fire(cart.getStoreId(), NodeCatalog.CART_ABANDONED, ctx);
        } catch (RuntimeException failed) {
            log("cart.abandoned", cart.getId(), failed);
        }
        try {
            emails.abandonedCart(cart, ctx.get("email"));
        } catch (RuntimeException failed) {
            log("cart.abandoned", cart.getId(), failed);
        }
    }

    // ---------------------------------------------------------------- inside

    /// An order with its lines and customer, read in one transaction.
    static final class Snapshot {
        StoreOrder order;
        List<OrderLine> lines;
        Customer customer;
        final List<String[]> low = new ArrayList<String[]>();
        final List<String[]> attributes = new ArrayList<String[]>();
    }

    @Transactional(readOnly = true)
    private Snapshot snapshot(String orderId) {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null) {
            return null;
        }
        Snapshot s = new Snapshot();
        s.order = o;
        s.lines = lines.findByOrderId(orderId);
        s.customer = o.getCustomerId().length() == 0 ? null : customers.findById(o.getCustomerId());
        if (db != null) {
            try {
                List<Map> rows = db.query("SELECT label, answer FROM order_field WHERE "
                        + "order_id = ? ORDER BY sort_order", new Object[] {orderId});
                for (int iter = 0; iter < rows.size(); iter++) {
                    Map r = rows.get(iter);
                    s.attributes.add(new String[] {String.valueOf(r.get("label")),
                        String.valueOf(r.get("answer"))});
                }
            } catch (java.io.IOException unreadable) {
                // The event still goes out, without its attributes.
            }
        }
        for (int iter = 0; iter < s.lines.size(); iter++) {
            OrderLine line = s.lines.get(iter);
            if (line.getLocationId().length() == 0) {
                continue;
            }
            InventoryLevel level = levels.findByVariantIdAndLocationId(line.getVariantId(),
                    line.getLocationId());
            if (level == null) {
                continue;
            }
            int available = level.getOnHand() - level.getCommitted();
            // Low as the stock screen's list has it (available at or below the
            // reorder point), and only on crossing it, so a variant already
            // low does not fire again on every sale.
            if (available <= level.getReorderPoint()
                    && available + line.getQuantity() > level.getReorderPoint()) {
                s.low.add(new String[] {line.getVariantId(), line.getLocationId(),
                    Integer.toString(available), Integer.toString(level.getReorderPoint()),
                    line.getSku(), line.getProductId()});
            }
        }
        return s;
    }

    @Transactional(readOnly = true)
    private Product productRead(String productId) {
        return productId == null ? null : products.findById(productId);
    }

    private void orderEvent(String orderId, String topic, String trigger) {
        try {
            Snapshot s = snapshot(orderId);
            if (s != null) {
                order(s, topic, trigger);
            }
        } catch (IOException | RuntimeException failed) {
            log(topic, orderId, failed);
        }
    }

    private void order(Snapshot s, String topic, String trigger) throws IOException {
        webhooks.emit(s.order.getStoreId(), topic, Json.write(orderPayload(s)));
        if (trigger != null) {
            automations.fire(s.order.getStoreId(), trigger, context(s));
        }
    }

    private void lowStock(Snapshot s) throws IOException {
        for (int iter = 0; iter < s.low.size(); iter++) {
            String[] l = s.low.get(iter);
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("storeId", s.order.getStoreId());
            payload.put("variantId", l[0]);
            payload.put("locationId", l[1]);
            payload.put("available", Long.valueOf(Long.parseLong(l[2])));
            payload.put("reorderPoint", Long.valueOf(Long.parseLong(l[3])));
            payload.put("sku", l[4]);
            payload.put("orderId", s.order.getId());
            webhooks.emit(s.order.getStoreId(), Webhooks.INVENTORY_LOW, Json.write(payload));
            Map<String, String> ctx = new HashMap<String, String>();
            ctx.put("variantId", l[0]);
            ctx.put("locationId", l[1]);
            ctx.put("productIds", l[5]);
            ctx.put("orderId", s.order.getId());
            automations.fire(s.order.getStoreId(), NodeCatalog.INVENTORY_LOW, ctx);
            emails.lowStock(s.order.getStoreId(), l[4], Integer.parseInt(l[2]),
                    Integer.parseInt(l[3]));
        }
    }

    private static Map<String, String> context(Snapshot s) {
        StoreOrder o = s.order;
        Map<String, String> ctx = new HashMap<String, String>();
        ctx.put("orderId", o.getId());
        ctx.put("customerId", o.getCustomerId());
        ctx.put("username", o.getUsername());
        ctx.put("email", o.getEmail());
        ctx.put("totalCents", Long.toString(o.getTotalCents()));
        ctx.put("channel", o.getChannel());
        ctx.put("country", o.getShipCountry());
        StringBuilder ids = new StringBuilder();
        for (int iter = 0; iter < s.lines.size(); iter++) {
            ids.append(iter == 0 ? "" : ",").append(s.lines.get(iter).getProductId());
        }
        ctx.put("productIds", ids.toString());
        return ctx;
    }

    private static Map<String, Object> orderPayload(Snapshot s) {
        StoreOrder o = s.order;
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", o.getId());
        m.put("storeId", o.getStoreId());
        m.put("number", Long.valueOf(o.getNumber()));
        m.put("channel", o.getChannel());
        m.put("status", o.getStatus());
        m.put("financial", o.getFinancial());
        m.put("fulfilment", o.getFulfilment());
        m.put("email", o.getEmail());
        List<Map<String, Object>> attributes = new ArrayList<Map<String, Object>>();
        for (int iter = 0; iter < s.attributes.size(); iter++) {
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("label", s.attributes.get(iter)[0]);
            one.put("value", s.attributes.get(iter)[1]);
            attributes.add(one);
        }
        m.put("attributes", attributes);
        m.put("customerId", o.getCustomerId());
        m.put("subtotalCents", Long.valueOf(o.getSubtotalCents()));
        m.put("discountCents", Long.valueOf(o.getDiscountCents()));
        m.put("shippingCents", Long.valueOf(o.getShippingCents()));
        m.put("taxCents", Long.valueOf(o.getTaxCents()));
        m.put("totalCents", Long.valueOf(o.getTotalCents()));
        m.put("refundedCents", Long.valueOf(o.getRefundedCents()));
        m.put("currency", o.getCurrency());
        m.put("tags", o.getTags());
        m.put("createdAt", Long.valueOf(o.getCreatedAt().toEpochMilli()));
        List<Object> ls = new ArrayList<Object>();
        for (int iter = 0; iter < s.lines.size(); iter++) {
            OrderLine l = s.lines.get(iter);
            Map<String, Object> lm = new LinkedHashMap<String, Object>();
            lm.put("productId", l.getProductId());
            lm.put("variantId", l.getVariantId());
            lm.put("sku", l.getSku());
            lm.put("title", l.getTitle());
            lm.put("quantity", Long.valueOf(l.getQuantity()));
            lm.put("priceCents", Long.valueOf(l.getPriceCents()));
            ls.add(lm);
        }
        m.put("lines", ls);
        return m;
    }

    private static Map<String, Object> customerPayload(Customer c) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", c.getId());
        m.put("storeId", c.getStoreId());
        m.put("email", c.getEmail());
        m.put("firstName", c.getFirstName());
        m.put("lastName", c.getLastName());
        m.put("acceptsMarketing", Boolean.valueOf(c.isAcceptsMarketing()));
        m.put("createdAt", Long.valueOf(c.getCreatedAt().toEpochMilli()));
        return m;
    }

    private static void log(String what, String id, Throwable failed) {
        System.err.println("[events] " + what + " for " + id + " was not told: " + failed);
    }
}
