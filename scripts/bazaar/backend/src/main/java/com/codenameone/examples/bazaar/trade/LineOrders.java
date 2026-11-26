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
package com.codenameone.examples.bazaar.trade;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.events.StoreEvents;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Orders priced by the caller rather than by the cart: a subscription
/// renewal at its plan's discount, a B2B quote at the company's prices.
///
/// Stock is reserved line by line, all or nothing, exactly as checkout and
/// `Orders.convertDraft` do, and the order is numbered by
/// [Orders#insertNumbered]. The order is left OPEN and PENDING; taking the
/// money (a card, or net terms) is the caller's. Tax and shipping are not
/// added: a renewal ships free, and a B2B quote is priced net of tax --
/// working tax out per company (exemptions, reverse charge) is beyond this
/// sample.
@Component
public class LineOrders {
    private final Orders orders;
    private final StoreOrderRepository orderRows;
    private final OrderLineRepository lines;
    private final VariantRepository variants;
    private final ProductRepository products;
    private final StoreRepository stores;
    private final Inventory inventory;
    private final StoreEvents storeEvents;

    private final com.codenameone.examples.bazaar.routing.OrderRouting routing;

    public LineOrders(Orders orders, StoreOrderRepository orderRows, OrderLineRepository lines,
            VariantRepository variants, ProductRepository products, StoreRepository stores,
            Inventory inventory, StoreEvents storeEvents, com.codenameone.examples.bazaar.routing.OrderRouting routing) {
        this.routing = routing;
        this.orders = orders;
        this.orderRows = orderRows;
        this.lines = lines;
        this.variants = variants;
        this.products = products;
        this.stores = stores;
        this.inventory = inventory;
        this.storeEvents = storeEvents;
    }

    /// One line of an order: the variant, how many, its catalog price and
    /// the price actually charged per unit.
    public static final class Item {
        public final String variantId;
        public final int quantity;
        public final long listCents;
        public final long unitCents;

        public Item(String variantId, int quantity, long listCents, long unitCents) {
            this.variantId = variantId;
            this.quantity = quantity;
            this.listCents = listCents;
            this.unitCents = unitCents;
        }
    }

    /// Who the order is for and where it goes.
    public static final class Buyer {
        public String username = "";
        public String email = "";
        public String customerId = "";
        public String shipName = "";
        public String shipLine1 = "";
        public String shipCity = "";
        public String shipRegion = "";
        public String shipPostal = "";
        public String shipCountry = "";
    }

    /// Reserves the stock and writes the order. Answers 409 when an item is
    /// out of stock, with nothing reserved.
    public StoreOrder place(String storeId, String channel, Buyer buyer, List<Item> items,
            String clientId, String actor, String eventMessage) throws IOException {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(400, "The order has no lines");
        }
        Store store = stores.findById(storeId);
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < items.size(); iter++) {
            ids.add(items.get(iter).variantId);
        }
        List<Variant> found = variants.findByIdIn(ids);
        List<String> productIds = new ArrayList<String>();
        for (int iter = 0; iter < found.size(); iter++) {
            productIds.add(found.get(iter).getProductId());
        }
        List<Product> ps = products.findByIdIn(productIds);
        final String orderId = Ids.next();
        final List<String[]> held = new ArrayList<String[]>();
        final List<Variant> vs = new ArrayList<Variant>();
        final List<Product> prs = new ArrayList<Product>();
        long subtotal = 0;
        long discount = 0;
        for (int iter = 0; iter < items.size(); iter++) {
            Item it = items.get(iter);
            Variant v = variant(found, it.variantId);
            Product p = v == null ? null : product(ps, v.getProductId());
            if (v == null || p == null || !storeId.equals(v.getStoreId())) {
                orders.releaseAll(storeId, held, orderId, actor);
                throw new ResponseStatusException(404, "No such item");
            }
            String at = routing.plan(storeId, new String[] {v.getId()},
                    new int[] {it.quantity}, false, 0, 0).locations[0];
            if (at == null || !inventory.reserve(storeId, v.getId(), at, it.quantity, orderId,
                    actor)) {
                orders.releaseAll(storeId, held, orderId, actor);
                throw new ResponseStatusException(409, "Not enough " + p.getTitle() + " in stock");
            }
            held.add(new String[] {Integer.toString(iter), v.getId(), at,
                Integer.toString(it.quantity)});
            vs.add(v);
            prs.add(p);
            subtotal += it.listCents * it.quantity;
            discount += (it.listCents - it.unitCents) * it.quantity;
        }
        StoreOrder order = new StoreOrder();
        order.setId(orderId);
        order.setStoreId(storeId);
        order.setUsername(buyer.username);
        order.setEmail(Codes.cut(buyer.email, 190));
        order.setCustomerId(buyer.customerId);
        order.setChannel(channel);
        order.setStatus(Orders.OPEN);
        order.setFinancial(Orders.PENDING);
        order.setFulfilment(Orders.UNFULFILLED);
        order.setSubtotalCents(subtotal);
        order.setDiscountCents(discount);
        order.setTotalCents(subtotal - discount);
        order.setCurrency(store.getCurrency());
        order.setShipName(Codes.cut(buyer.shipName, 120));
        order.setShipLine1(Codes.cut(buyer.shipLine1, 200));
        order.setShipCity(Codes.cut(buyer.shipCity, 120));
        order.setShipRegion(Codes.cut(buyer.shipRegion, 80));
        order.setShipPostal(Codes.cut(buyer.shipPostal, 20));
        order.setShipCountry(Codes.cut(buyer.shipCountry, 2));
        order.setLocationId(held.get(0)[2]);
        order.setClientId(clientId);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        final List<Item> priced = items;
        final String who = actor;
        final String message = eventMessage;
        StoreOrder saved;
        try {
            saved = orders.insertNumbered(order, new Orders.Writer() {
                @Override
                public void write(StoreOrder o) throws IOException {
                    for (int iter = 0; iter < priced.size(); iter++) {
                        Item it = priced.get(iter);
                        OrderLine line = new OrderLine();
                        line.setId(Ids.next());
                        line.setOrderId(o.getId());
                        line.setProductId(prs.get(iter).getId());
                        line.setVariantId(it.variantId);
                        line.setTitle(Codes.cut(prs.get(iter).getTitle(), 200));
                        line.setVariantTitle(Codes.cut(vs.get(iter).getTitle(), 200));
                        line.setSku(Codes.cut(vs.get(iter).getSku(), 64));
                        line.setPhotoId(prs.get(iter).getPhotoId() == null ? ""
                                : prs.get(iter).getPhotoId());
                        line.setQuantity(it.quantity);
                        line.setPriceCents(it.listCents);
                        line.setDiscountCents((it.listCents - it.unitCents) * it.quantity);
                        line.setWeightGrams(vs.get(iter).getWeightGrams());
                        line.setLocationId(held.get(iter)[2]);
                        lines.insert(line);
                    }
                    orders.event(o.getId(), "PLACED", message, who);
                }
            });
        } catch (IOException | RuntimeException failed) {
            orders.releaseAll(storeId, held, orderId, actor);
            throw failed;
        }
        if (saved == null) {
            orders.releaseAll(storeId, held, orderId, actor);
            throw new ResponseStatusException(409, "The order is being placed; try again");
        }
        orders.publish(saved);
        storeEvents.orderPlaced(saved.getId());
        return saved;
    }

    /// Takes back an order whose payment never went through: its stock is
    /// released and the order erased.
    public void undo(String orderId, String actor) throws IOException {
        StoreOrder order = orderRows.findById(orderId);
        if (order == null) {
            return;
        }
        List<OrderLine> rows = lines.findByOrderId(orderId);
        List<String[]> held = new ArrayList<String[]>();
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderLine l = rows.get(iter);
            if (l.getLocationId().length() > 0) {
                held.add(new String[] {l.getId(), l.getVariantId(), l.getLocationId(),
                    Integer.toString(l.getQuantity())});
            }
        }
        orders.releaseAll(order.getStoreId(), held, orderId, actor);
        orders.erase(orderId);
    }

    /// Records a card hold taken for `orderId` (PENDING to AUTHORIZED, a
    /// CARD tender), as checkout does once the processor agreed.
    @Transactional(rollbackFor = IOException.class)
    public void authorized(String orderId, long amountCents, String reference, String actor)
            throws IOException {
        if (orderRows.claimFinancial(orderId, Orders.PENDING, Orders.AUTHORIZED) == 1) {
            orders.tender(orderId, "CARD", amountCents, reference);
            orders.event(orderId, "AUTHORIZED", "Payment held on the card", actor);
        }
    }

    private static Variant variant(List<Variant> found, String id) {
        for (int iter = 0; iter < found.size(); iter++) {
            if (found.get(iter).getId().equals(id)) {
                return found.get(iter);
            }
        }
        return null;
    }

    private static Product product(List<Product> found, String id) {
        for (int iter = 0; iter < found.size(); iter++) {
            if (found.get(iter).getId().equals(id)) {
                return found.get(iter);
            }
        }
        return null;
    }
}
