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
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.carrier.CarrierProvider;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.domain.Wishlist;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.marketing.Shoppers;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.domain.CustomerAddress;
import com.codenameone.examples.bazaar.repository.CustomerAddressRepository;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.repository.WishlistRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The demo shopper's side of a walkthrough: a history to look at, and the
/// order placed during the tour followed all the way to the door.
///
/// [#seed] gives the demo shopper (`DemoAccounts.CUSTOMER`) past orders in
/// both demo stores in the states a shopper meets -- delivered, on its way,
/// packed, cancelled -- each earning its loyalty points as a paid order does,
/// and a wishlist. [#advance] then takes every order the shopper placed after
/// the server started one stage on per tick, through the same service calls
/// the merchant's fulfilment screens make: packed, labelled, shipped, and the
/// carrier's scans (in transit, out for delivery, delivered) fed one at a time,
/// so the tracking screen hears each step on the live socket instead of two in
/// one carrier poll.
///
/// Off unless the profile is a development one **and** `bazaar.demo.shopper`
/// is true: production never runs it, and neither do the backend tests, which
/// expect the demo shopper to be a new account. The merchant walkthrough
/// leaves it off too, since it would pack the orders that tour seeds for the
/// merchant to pack. It never touches DemoPipeline's orders, and what it seeds
/// has a client id starting with [#PREFIX].
@Component
public class ShopperDemo {
    /// The client id prefix of the seeded history.
    public static final String PREFIX = "demo-shopper-";

    /// The kinds of step [#advance] takes, for the tests.
    public static final String PACKED = "PACKED";
    public static final String LABELLED = "LABELLED";
    public static final String SHIPPED = "SHIPPED";
    public static final String SCANNED = "SCANNED";
    public static final String IDLE = "IDLE";

    /// The carrier's scans after "shipped".
    private static final String[][] SCANS = {
        {Fulfillments.IN_TRANSIT, "Departed origin facility", "Sort center"},
        {Fulfillments.IN_TRANSIT, "Arrived at regional hub", "Regional hub"},
        {Fulfillments.OUT_FOR_DELIVERY, "Out for delivery", "San Francisco"},
        {Fulfillments.DELIVERED, "Delivered", "San Francisco"}
    };

    /// `{store handle, stage, days ago}` of the seeded history, oldest first.
    private static final String[][] HISTORY = {
        {DemoStores.NORTHWIND, "DELIVERED", "41"},
        {DemoStores.LITTLE_CLAY, "DELIVERED", "26"},
        {DemoStores.NORTHWIND, "DELIVERED", "12"},
        {DemoStores.LITTLE_CLAY, "CANCELLED", "9"},
        {DemoStores.NORTHWIND, "IN_TRANSIT", "3"},
        {DemoStores.LITTLE_CLAY, "PACKED", "1"}
    };

    private final Config config;
    private final StoreRepository stores;
    private final LocationRepository locations;
    private final VariantRepository variants;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final FulfillmentRepository fulfillments;
    private final WishlistRepository wishlists;
    private final Orders orderService;
    private final Fulfillments fulfil;
    private final Loyalty loyalty;
    private final Shoppers shoppers;
    private final DemoStores demoStores;
    private final CustomerAddressRepository addresses;
    private final long started = System.currentTimeMillis();
    private boolean seeded;

    private final com.codenameone.examples.bazaar.market.ReportingRates reporting;

    public ShopperDemo(Config config, StoreRepository stores, LocationRepository locations,
            VariantRepository variants, StoreOrderRepository orders, OrderLineRepository lines,
            FulfillmentRepository fulfillments, WishlistRepository wishlists,
            Orders orderService, Fulfillments fulfil, Loyalty loyalty, Shoppers shoppers,
            DemoStores demoStores, CustomerAddressRepository addresses,
            com.codenameone.examples.bazaar.market.ReportingRates reporting) {
        this.reporting = reporting;
        this.config = config;
        this.stores = stores;
        this.locations = locations;
        this.variants = variants;
        this.orders = orders;
        this.lines = lines;
        this.fulfillments = fulfillments;
        this.wishlists = wishlists;
        this.orderService = orderService;
        this.fulfil = fulfil;
        this.loyalty = loyalty;
        this.shoppers = shoppers;
        this.demoStores = demoStores;
        this.addresses = addresses;
    }

    /// Whether this runs on this server.
    public boolean enabled() throws IOException {
        return config.isDevelopmentProfile() && config.getBoolean("bazaar.demo.shopper", false);
    }

    /// Every few seconds: seeds once, then moves the shopper's new orders on.
    @Scheduled(fixedDelay = 3500, initialDelay = 8000, lock = "bazaar-demo-shopper")
    public void tick() throws IOException {
        if (!enabled()) {
            return;
        }
        if (!seeded) {
            seeded = true;
            try {
                seed(DemoAccounts.CUSTOMER);
            } catch (IOException | RuntimeException err) {
                System.err.println("[demo-shopper] seed: " + err);
            }
        }
        advance(DemoAccounts.CUSTOMER, started);
    }

    /// Seeds the demo shopper's history unless an earlier call did; answers
    /// whether it seeded.
    public boolean seed(String username) throws IOException {
        List<StoreOrder> had = orders.findByUsernameOrderByCreatedAtDesc(username);
        for (int iter = 0; iter < had.size(); iter++) {
            String cid = had.get(iter).getClientId();
            if (cid != null && cid.startsWith(PREFIX)) {
                return false;
            }
        }
        for (int iter = 0; iter < HISTORY.length; iter++) {
            Store store = stores.findByHandle(HISTORY[iter][0]);
            if (store == null) {
                continue;
            }
            long ago = Long.parseLong(HISTORY[iter][2]) * 86400000L;
            StoreOrder o = place(username, store, iter, ago);
            if (o != null) {
                stage(store, o, HISTORY[iter][1], System.currentTimeMillis() - ago);
            }
        }
        wishlist(username);
        addresses(username);
        // The third store, which the history leaves out: the walkthrough's
        // order there is the one that earns the Explorer badge.
        demoStores.fennelFig(DemoAccounts.MERCHANT);
        return true;
    }

    /// Home and work, home the default, as a shopper with six orders has.
    private void addresses(String username) throws IOException {
        if (!addresses.findByUsername(username).isEmpty()) {
            return;
        }
        String[][] rows = {
            {"Riley Shopper", "1 Market St", "Apt 12", "San Francisco", "CA", "94105",
                "37.7941", "-122.3951"},
            {"Riley Shopper", "500 Howard St", "Floor 4", "San Francisco", "CA", "94105",
                "37.7877", "-122.3967"}
        };
        for (int iter = 0; iter < rows.length; iter++) {
            CustomerAddress a = new CustomerAddress();
            a.setId(Ids.next());
            a.setUsername(username);
            a.setName(rows[iter][0]);
            a.setLine1(rows[iter][1]);
            a.setLine2(rows[iter][2]);
            a.setCity(rows[iter][3]);
            a.setRegion(rows[iter][4]);
            a.setPostal(rows[iter][5]);
            a.setCountry("US");
            a.setPhone("+14155550123");
            a.setLat(Double.parseDouble(rows[iter][6]));
            a.setLng(Double.parseDouble(rows[iter][7]));
            a.setIsDefault(iter == 0);
            addresses.insert(a);
        }
    }

    /// One step for every open order the shopper placed at or after `since`;
    /// answers the kind of the last step taken.
    public String advance(String username, long since) throws IOException {
        List<StoreOrder> mine = orders.findByUsernameOrderByCreatedAtDesc(username);
        String did = IDLE;
        for (int iter = 0; iter < mine.size(); iter++) {
            StoreOrder o = mine.get(iter);
            String cid = o.getClientId() == null ? "" : o.getClientId();
            if (o.getCreatedAt() == null || o.getCreatedAt().toEpochMilli() < since
                    || !Orders.OPEN.equals(o.getStatus()) || cid.startsWith(PREFIX)
                    || cid.startsWith(DemoPipeline.PREFIX)) {
                continue;
            }
            try {
                String kind = step(o);
                if (!IDLE.equals(kind)) {
                    did = kind;
                }
            } catch (IOException | RuntimeException err) {
                System.err.println("[demo-shopper] step " + o.getNumber() + ": " + err);
            }
        }
        return did;
    }

    /// Takes `o` one stage on.
    private String step(StoreOrder o) throws IOException {
        String owner = DemoAccounts.MERCHANT;
        List<Fulfillment> fs = fulfillments.findByOrderId(o.getId());
        if (fs.isEmpty()) {
            if (Orders.PENDING.equals(o.getFinancial())) {
                // Cash on delivery and the like: the store records it paid
                // first, so the Paid step is reached before Packed.
                orderService.markPaid(owner, o.getStoreId(), o.getId());
            }
            packed(owner, o.getStoreId(), o.getId());
            return PACKED;
        }
        Fulfillment f = fs.get(0);
        String st = f.getStatus();
        if (Fulfillments.PACKED.equals(st) && f.getTrackingNumber().length() == 0) {
            fulfil.buyLabel(owner, o.getStoreId(), f.getId(), new LabelRequestDto(null));
            return LABELLED;
        }
        if (Fulfillments.PACKED.equals(st) || Fulfillments.LABEL_CREATED.equals(st)) {
            fulfil.ship(owner, o.getStoreId(), f.getId());
            return SHIPPED;
        }
        if (Fulfillments.SHIPPED.equals(st) || Fulfillments.IN_TRANSIT.equals(st)
                || Fulfillments.OUT_FOR_DELIVERY.equals(st)) {
            if (f.getTrackingNumber().length() == 0) {
                // A local delivery has no carrier: the store's driver arrives.
                fulfil.delivered(owner, o.getStoreId(), f.getId());
                return SCANNED;
            }
            fulfil.serviceScans(f.getId(), scansUpTo(next(st), System.currentTimeMillis()));
            return SCANNED;
        }
        return IDLE;
    }

    /// How many of [#SCANS] a parcel with status `st` gets next: one when it
    /// has just shipped, then the hub scan with out for delivery, then all.
    static int next(String st) {
        if (Fulfillments.SHIPPED.equals(st)) {
            return 1;
        }
        return Fulfillments.IN_TRANSIT.equals(st) ? 3 : 4;
    }

    /// The first `count` scans, a second apart, the last at `last`.
    private static List<CarrierProvider.Scan> scansUpTo(int count, long last) {
        List<CarrierProvider.Scan> out = new ArrayList<CarrierProvider.Scan>();
        for (int iter = 0; iter < count && iter < SCANS.length; iter++) {
            out.add(new CarrierProvider.Scan(SCANS[iter][0], SCANS[iter][1], SCANS[iter][2], 0, 0,
                    last - (count - 1 - iter) * 1000L));
        }
        return out;
    }

    private void stage(Store store, StoreOrder o, String stage, long at) {
        String owner = DemoAccounts.MERCHANT;
        try {
            if ("CANCELLED".equals(stage)) {
                orderService.cancel(owner, store.getId(), o.getId());
                return;
            }
            String f = packed(owner, store.getId(), o.getId());
            if (PACKED.equals(stage)) {
                return;
            }
            fulfil.buyLabel(owner, store.getId(), f, new LabelRequestDto(null));
            fulfil.ship(owner, store.getId(), f);
            fulfil.serviceScans(f, scansUpTo("DELIVERED".equals(stage) ? 4 : 2,
                    Math.min(System.currentTimeMillis(), at + 3L * 86400000L)));
        } catch (IOException | RuntimeException err) {
            System.err.println("[demo-shopper] seeding " + stage + ": " + err);
        } finally {
            try {
                loyalty.earn(o.getId());
            } catch (IOException | RuntimeException err) {
                System.err.println("[demo-shopper] points: " + err);
            }
        }
    }

    private String packed(String owner, String storeId, String orderId) throws IOException {
        List<Fulfillment> fs = fulfillments.findByOrderId(orderId);
        if (!fs.isEmpty()) {
            return fs.get(0).getId();
        }
        Location at = shipsFrom(storeId);
        List<FulfilLineDto> send = new ArrayList<FulfilLineDto>();
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine l = ls.get(iter);
            send.add(new FulfilLineDto(l.getId(), l.getTitle(), l.getSku(), l.getQuantity()));
        }
        String id = fulfil.create(owner, storeId, orderId,
                new NewFulfillmentDto(at == null ? "" : at.getId(), null, send)).id();
        fulfil.pack(owner, storeId, id, new PackDto(600));
        return id;
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

    /// A paid online order of the demo shopper's, placed `agoMillis` ago.
    private StoreOrder place(String username, Store store, int n, long agoMillis)
            throws IOException {
        List<Variant> stock = variants.findByStoreId(store.getId());
        if (stock.isEmpty()) {
            return null;
        }
        Customer c = shoppers.customer(store.getId(), username);
        Location at = shipsFrom(store.getId());
        Instant when = Instant.now().minusMillis(agoMillis);
        StoreOrder o = new StoreOrder();
        o.setId(Ids.next());
        o.setStoreId(store.getId());
        o.setNumber(nextNumber(store.getId()));
        o.setClientId(PREFIX + o.getId());
        o.setCustomerId(c.getId());
        o.setUsername(username);
        o.setEmail(username);
        o.setChannel(Orders.ONLINE);
        o.setStatus(Orders.OPEN);
        o.setFinancial(Orders.PAID);
        o.setFulfilment(Orders.UNFULFILLED);
        o.setCurrency(store.getCurrency());
        o.setShipName("Riley Shopper");
        o.setShipLine1("1 Market St");
        o.setShipCity("San Francisco");
        o.setShipRegion("CA");
        o.setShipPostal("94105");
        o.setShipCountry("US");
        o.setShippingTitle("Standard");
        o.setLocationId(at == null ? "" : at.getId());
        List<OrderLine> made = new ArrayList<OrderLine>();
        long subtotal = 0;
        for (int l = 0; l < 1 + n % 3; l++) {
            Variant v = stock.get((n * 7 + l * 5 + 2) % stock.size());
            OrderLine line = new OrderLine();
            line.setId(Ids.next());
            line.setOrderId(o.getId());
            line.setProductId(v.getProductId());
            line.setVariantId(v.getId());
            line.setTitle(v.getTitle());
            line.setVariantTitle(v.getTitle());
            line.setSku(v.getSku());
            line.setQuantity(1 + l % 2);
            line.setPriceCents(v.getPriceCents());
            line.setLocationId(o.getLocationId());
            subtotal += v.getPriceCents() * line.getQuantity();
            made.add(line);
        }
        o.setSubtotalCents(subtotal);
        // Free over $75, else $5.95, in the store's own currency.
        o.setShippingCents(subtotal > com.codenameone.examples.bazaar.pricing.DemoMoney.local(7500,
                o.getCurrency()) ? 0 : com.codenameone.examples.bazaar.pricing.DemoMoney.local(595,
                o.getCurrency()));
        o.setTotalCents(subtotal + o.getShippingCents());
        o.setCreatedAt(when);
        o.setUpdatedAt(when);
        reporting.stamp(o);
        orders.insert(o);
        lines.saveAll(made);
        return o;
    }

    /// Five products the shopper saved, from both stores.
    private void wishlist(String username) throws IOException {
        String[] handles = {DemoStores.NORTHWIND, DemoStores.LITTLE_CLAY};
        int added = 0;
        for (int s = 0; s < handles.length; s++) {
            Store store = stores.findByHandle(handles[s]);
            if (store == null) {
                continue;
            }
            List<Variant> stock = variants.findByStoreId(store.getId());
            List<String> taken = new ArrayList<String>();
            for (int iter = 1; iter < stock.size() && taken.size() < 3 - s; iter += 4) {
                String product = stock.get(iter).getProductId();
                if (taken.contains(product) || wishlists.findByUsernameAndProductId(
                        username, product) != null) {
                    continue;
                }
                taken.add(product);
                added++;
                Wishlist w = new Wishlist();
                w.setId(Ids.next());
                w.setUsername(username);
                w.setProductId(product);
                w.setStoreId(store.getId());
                w.setCreatedAt(Instant.now().minusMillis(added * 86400000L));
                wishlists.insert(w);
            }
        }
    }

    private int nextNumber(String storeId) {
        List<StoreOrder> all = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        int top = 1000;
        for (int iter = 0; iter < all.size(); iter++) {
            top = Math.max(top, all.get(iter).getNumber());
        }
        return top + 1;
    }
}
