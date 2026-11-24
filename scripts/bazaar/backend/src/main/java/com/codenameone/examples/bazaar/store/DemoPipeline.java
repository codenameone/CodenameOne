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
package com.codenameone.examples.bazaar.store;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.CartLine;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.CartLineRepository;
import com.codenameone.examples.bazaar.repository.CartRepository;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// A sales pipeline that moves by itself during a walkthrough.
///
/// The demo stores' seeded history is closed and delivered, so the merchant's
/// pipeline board was empty and only moved when someone dragged a card. With
/// this switched on, the first tick seeds Northwind with open orders at every
/// stage (paid, packed, labelled, shipped, delivered) and with carts that are
/// browsing or at checkout, and every tick after it does one real thing to one
/// of them -- a new order arrives, a paid one is packed, a packed one gets its
/// label, a labelled one ships. The simulated carrier's tracking sweep
/// ([Fulfillments#poll]) then takes a shipped parcel through "out for
/// delivery" to delivered. Each step goes through the same service calls a
/// person would make, so every one publishes the ordinary `order` frame on the
/// live socket and the board reloads and animates.
///
/// Off unless the profile is a development one **and**
/// `bazaar.demo.pipeline` is true: production never runs it, and neither do
/// the backend tests, whose counts it would change. It touches only the orders
/// and carts it created itself (client id and token prefixed [#PREFIX]).
@Component
public class DemoPipeline {
    /// The client id (orders) and token (carts) prefix of what this made.
    public static final String PREFIX = "demo-live-";

    /// The kinds of step [#step] takes, for the tests.
    public static final String PLACED = "PLACED";
    public static final String PACKED = "PACKED";
    public static final String LABELLED = "LABELLED";
    public static final String SHIPPED = "SHIPPED";
    public static final String IDLE = "IDLE";

    private static final String[] NAMES = {"Ava Brooks", "Noah Patel", "Mia Chen",
        "Liam Garcia", "Zoe Martin", "Ethan Kim", "Ivy Rossi", "Leo Novak", "Nora Silva",
        "Owen Hughes", "Ruby Okafor", "Finn Larsen"};
    private static final String[][] CITIES = {{"Portland", "OR", "97205"},
        {"Austin", "TX", "78701"}, {"Denver", "CO", "80202"}, {"Boston", "MA", "02108"},
        {"Seattle", "WA", "98101"}, {"Chicago", "IL", "60601"}};

    private final Config config;
    private final StoreRepository stores;
    private final LocationRepository locations;
    private final VariantRepository variants;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final FulfillmentRepository fulfillments;
    private final CartRepository carts;
    private final CartLineRepository cartLines;
    private final Orders orderService;
    private final Fulfillments fulfil;
    private int ticks;
    private int seq;

    public DemoPipeline(Config config, StoreRepository stores, LocationRepository locations,
            VariantRepository variants, StoreOrderRepository orders, OrderLineRepository lines,
            FulfillmentRepository fulfillments, CartRepository carts,
            CartLineRepository cartLines, Orders orderService, Fulfillments fulfil) {
        this.config = config;
        this.stores = stores;
        this.locations = locations;
        this.variants = variants;
        this.orders = orders;
        this.lines = lines;
        this.fulfillments = fulfillments;
        this.carts = carts;
        this.cartLines = cartLines;
        this.orderService = orderService;
        this.fulfil = fulfil;
    }

    /// Whether the simulator runs on this server.
    public boolean enabled() throws IOException {
        return config.isDevelopmentProfile() && config.getBoolean("bazaar.demo.pipeline", false);
    }

    /// Every few seconds: seeds once, then takes one step.
    @Scheduled(fixedDelay = 5000, initialDelay = 20000, lock = "bazaar-demo-pipeline")
    public void tick() throws IOException {
        if (!enabled()) {
            return;
        }
        Store store = stores.findByHandle(DemoStores.NORTHWIND);
        if (store == null) {
            return;
        }
        seed(store.getId());
        step(store.getId());
    }

    /// Seeds the board of `storeId` unless an earlier call did: orders at every
    /// stage and carts at checkout and browsing. Answers whether it seeded.
    public boolean seed(String storeId) throws IOException {
        if (!mine(storeId).isEmpty()) {
            return false;
        }
        String owner = DemoAccounts.MERCHANT;
        // Oldest first, so the later stages hold the older orders.
        String[] stages = {"DELIVERED", "DELIVERED", SHIPPED, SHIPPED, SHIPPED, LABELLED,
            LABELLED, PACKED, PACKED, PLACED, PLACED, PLACED};
        for (int iter = 0; iter < stages.length; iter++) {
            StoreOrder o = place(storeId, (stages.length - iter) * 47L * 60000L);
            if (o == null) {
                return false;
            }
            String stage = stages[iter];
            try {
                if (PLACED.equals(stage)) {
                    continue;
                }
                String f = packed(owner, storeId, o.getId());
                if (PACKED.equals(stage)) {
                    continue;
                }
                fulfil.buyLabel(owner, storeId, f, new LabelRequestDto(null));
                if (LABELLED.equals(stage)) {
                    continue;
                }
                fulfil.ship(owner, storeId, f);
                if ("DELIVERED".equals(stage)) {
                    // Only the carrier says a parcel arrived: run its tracking a
                    // month ahead. These are the first parcels shipped, so no
                    // other seeded card moves with them.
                    fulfil.advance(System.currentTimeMillis() + 30L * 24 * 3600000L);
                }
            } catch (IOException | RuntimeException err) {
                System.err.println("[demo-pipeline] seeding " + stage + ": " + err);
            }
        }
        seedCarts(storeId);
        return true;
    }

    /// One step on the board of `storeId`; answers which kind it took. Every
    /// third step is a new order, the rest move the oldest card of the
    /// earliest stage that has one, latest stage first, so the board flows.
    public String step(String storeId) throws IOException {
        ticks++;
        String owner = DemoAccounts.MERCHANT;
        if (ticks % 3 == 1) {
            StoreOrder o = place(storeId, 0);
            return o == null ? IDLE : PLACED;
        }
        List<StoreOrder> open = mine(storeId);
        // Labelled parcels ship first, then packed ones get labels, then paid
        // ones are packed: each tick moves the card nearest the door.
        String[] wanted = {Fulfillments.LABEL_CREATED, Fulfillments.PACKED, null};
        for (int w = 0; w < wanted.length; w++) {
            for (int iter = open.size() - 1; iter >= 0; iter--) {
                StoreOrder o = open.get(iter);
                List<Fulfillment> fs = fulfillments.findByOrderId(o.getId());
                Fulfillment f = fs.isEmpty() ? null : fs.get(0);
                try {
                    if (wanted[w] == null && f == null && Orders.OPEN.equals(o.getStatus())) {
                        packed(owner, storeId, o.getId());
                        return PACKED;
                    }
                    // A parcel with a tracking number has its label whatever
                    // scan its status last recorded, so it ships next.
                    String st = f == null ? null : f.getTrackingNumber().length() > 0
                            && Fulfillments.PACKED.equals(f.getStatus())
                            ? Fulfillments.LABEL_CREATED : f.getStatus();
                    if (f != null && wanted[w] != null && wanted[w].equals(st)) {
                        if (Fulfillments.LABEL_CREATED.equals(st)) {
                            fulfil.ship(owner, storeId, f.getId());
                            return SHIPPED;
                        }
                        fulfil.buyLabel(owner, storeId, f.getId(), new LabelRequestDto(null));
                        return LABELLED;
                    }
                } catch (IOException | RuntimeException err) {
                    System.err.println("[demo-pipeline] step: " + err);
                }
            }
        }
        return IDLE;
    }

    /// The open orders this made, newest first.
    public List<StoreOrder> mine(String storeId) {
        List<StoreOrder> all = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<StoreOrder> out = new ArrayList<StoreOrder>();
        for (int iter = 0; iter < all.size(); iter++) {
            StoreOrder o = all.get(iter);
            if (o.getClientId() != null && o.getClientId().startsWith(PREFIX)) {
                out.add(o);
            }
        }
        return out;
    }

    private String packed(String owner, String storeId, String orderId) throws IOException {
        Fulfillment f = null;
        List<Fulfillment> fs = fulfillments.findByOrderId(orderId);
        if (fs.isEmpty()) {
            Location at = shipsFrom(storeId);
            List<com.codenameone.examples.bazaar.api.FulfilLineDto> send =
                    new ArrayList<com.codenameone.examples.bazaar.api.FulfilLineDto>();
            List<OrderLine> ls = lines.findByOrderId(orderId);
            for (int iter = 0; iter < ls.size(); iter++) {
                OrderLine l = ls.get(iter);
                send.add(new com.codenameone.examples.bazaar.api.FulfilLineDto(l.getId(),
                        l.getTitle(), l.getSku(), l.getQuantity()));
            }
            String id = fulfil.create(owner, storeId, orderId,
                    new com.codenameone.examples.bazaar.api.NewFulfillmentDto(
                            at == null ? "" : at.getId(), null, send)).id();
            fulfil.pack(owner, storeId, id, new PackDto(600));
            return id;
        }
        f = fs.get(0);
        return f.getId();
    }

    private Location shipsFrom(String storeId) {
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).isFulfilsOnline()) {
                return all.get(iter);
            }
        }
        return all.isEmpty() ? null : all.get(0);
    }

    /// A paid online order placed `agoMillis` ago, published as a new order.
    private StoreOrder place(String storeId, long agoMillis) throws IOException {
        Store store = stores.findById(storeId);
        List<Variant> stock = variants.findByStoreId(storeId);
        Location at = shipsFrom(storeId);
        if (store == null || stock.isEmpty()) {
            return null;
        }
        int n = seq++;
        Instant when = Instant.now().minusMillis(agoMillis);
        StoreOrder o = new StoreOrder();
        o.setId(Ids.next());
        o.setStoreId(storeId);
        o.setNumber(nextNumber(storeId));
        o.setClientId(PREFIX + o.getId());
        o.setChannel(Orders.ONLINE);
        o.setStatus(Orders.OPEN);
        o.setFinancial(Orders.PAID);
        o.setFulfilment(Orders.UNFULFILLED);
        o.setCurrency(store.getCurrency());
        String name = NAMES[n % NAMES.length];
        o.setEmail(email(name));
        String[] city = CITIES[n % CITIES.length];
        o.setShipName(name);
        o.setShipLine1((100 + n * 17) + " Market Street");
        o.setShipCity(city[0]);
        o.setShipRegion(city[1]);
        o.setShipPostal(city[2]);
        o.setShipCountry("US");
        o.setLocationId(at == null ? "" : at.getId());
        List<OrderLine> made = new ArrayList<OrderLine>();
        long subtotal = 0;
        for (int l = 0; l < 1 + n % 3; l++) {
            Variant v = stock.get((n * 5 + l * 3) % stock.size());
            OrderLine line = new OrderLine();
            line.setId(Ids.next());
            line.setOrderId(o.getId());
            line.setProductId(v.getProductId());
            line.setVariantId(v.getId());
            line.setTitle(v.getTitle());
            line.setVariantTitle(v.getTitle());
            line.setSku(v.getSku());
            line.setQuantity(1 + (n + l) % 2);
            line.setPriceCents(v.getPriceCents());
            line.setLocationId(o.getLocationId());
            subtotal += v.getPriceCents() * line.getQuantity();
            made.add(line);
        }
        o.setSubtotalCents(subtotal);
        o.setShippingCents(subtotal > 7500 ? 0 : 595);
        o.setTotalCents(subtotal + o.getShippingCents());
        o.setCreatedAt(when);
        o.setUpdatedAt(when);
        orders.insert(o);
        lines.saveAll(made);
        orderService.publish(o);
        return o;
    }

    /// "Ava Brooks" as "ava.brooks@example.com", folded by hand: the names are
    /// ASCII and a locale must not fold them.
    static String email(String name) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < name.length(); iter++) {
            char c = name.charAt(iter);
            b.append(c == ' ' ? '.' : c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return b.append("@example.com").toString();
    }

    private int nextNumber(String storeId) {
        List<StoreOrder> all = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        int top = 1000;
        for (int iter = 0; iter < all.size(); iter++) {
            top = Math.max(top, all.get(iter).getNumber());
        }
        return top + 1;
    }

    /// Five carts with items: three browsing (no e-mail yet), two that reached
    /// checkout and gave one.
    private void seedCarts(String storeId) throws IOException {
        List<Variant> stock = variants.findByStoreId(storeId);
        if (stock.isEmpty()) {
            return;
        }
        for (int iter = 0; iter < 5; iter++) {
            Instant when = Instant.now().minusMillis((iter + 1) * 9L * 60000L);
            Cart c = new Cart();
            c.setId(Ids.next());
            c.setStoreId(storeId);
            c.setToken(PREFIX + c.getId());
            c.setUsername("");
            c.setEmail(iter < 2 ? email(NAMES[(iter + 5) % NAMES.length]) : "");
            c.setCreatedAt(when);
            c.setUpdatedAt(when);
            carts.insert(c);
            List<CartLine> ls = new ArrayList<CartLine>();
            for (int l = 0; l <= iter % 2; l++) {
                CartLine line = new CartLine();
                line.setId(Ids.next());
                line.setCartId(c.getId());
                line.setVariantId(stock.get((iter * 3 + l * 7) % stock.size()).getId());
                line.setQuantity(1);
                line.setAddedAt(when);
                ls.add(line);
            }
            cartLines.saveAll(ls);
        }
    }
}
