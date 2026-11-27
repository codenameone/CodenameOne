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
package com.codenameone.examples.bazaar.channel;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The marketplace sync engine. Each [#tick] runs every connected channel
/// whose backoff has passed, in three steps:
///
/// 1. **Pull orders** when the pull interval passed or a signed notification
///    asked ([#notification]). Each marketplace order becomes one Bazaar order in a
///    transaction of its own, channel `MARKETPLACE`, its `client_id` derived
///    from the channel and the external id (unique, so a replay finds the
///    first order and writes nothing). Stock is reserved inside that
///    transaction. A marketplace sale is never refused for want of stock --
///    the buyer already paid the marketplace -- so, as a register's offline
///    sale does (`PosSales`), it commits the units anyway, the order and an
///    audit event say it was oversold, and every listing of the variant is
///    pushed at once.
/// 2. **Push listings.** A listing is looked at when the inventory ledger
///    moved its variant since the channel's cursor, its price differs from
///    the one last pushed, it never was pushed, or it is retrying. Its state
///    (available units at the locations that fulfil online, and price) is
///    pushed only when it differs from the state last accepted, and only once
///    it has held still for [#DEBOUNCE_MS] (an order or an oversell skips the
///    wait). The listing's `version` is claimed with a conditional update
///    before the call and is the idempotency key the marketplace sees.
/// 3. **Push shipments** of marketplace orders, with carrier and tracking.
///
/// No transaction is open during a marketplace call, and nothing in the
/// checkout ever waits for a channel: the job runs on its own schedule.
/// Failures back off per listing, shipment and channel, from 30 seconds
/// doubling to an hour, and each lands in the channel's sync log.
@Component
public class ChannelSync {
    public static final String MARKETPLACE = "MARKETPLACE";
    public static final String ACTIVE = "ACTIVE";
    public static final String ERROR = "ERROR";
    public static final String PAUSED = "PAUSED";
    public static final String OK = "OK";
    public static final String SKIPPED = "SKIPPED";
    public static final String PENDING = "PENDING";
    static final long DEBOUNCE_MS = 20000L;
    static final long PULL_EVERY_MS = 120000L;
    /// Orders are asked for from this long before the last pull: clocks
    /// differ, and an order indexed late is still found. Replays are free.
    static final long PULL_OVERLAP_MS = 600000L;
    static final long FIRST_BACKOFF_MS = 30000L;
    static final long MAX_BACKOFF_MS = 3600000L;
    static final long LOG_KEEP_MS = 30L * 24 * 3600000L;
    /// Units offered for a variant that does not track inventory.
    static final int UNTRACKED_QTY = 100;
    static final String SYNC_USER = "marketplace-sync";

    private final DataSource db;
    private final Connectors connectors;
    private final Orders orders;
    private final OrderLineRepository lines;
    private final Inventory inventory;
    private final Markets markets;
    private final com.codenameone.examples.bazaar.catalog.Bundles bundles;
    private final com.codenameone.examples.bazaar.catalog.Schedules schedules;

    private final com.codenameone.examples.bazaar.routing.OrderRouting routing;

    public ChannelSync(DataSource db, Connectors connectors, Orders orders,
            OrderLineRepository lines, Inventory inventory, Markets markets,
            com.codenameone.examples.bazaar.catalog.Bundles bundles,
            com.codenameone.examples.bazaar.catalog.Schedules schedules,
            com.codenameone.examples.bazaar.routing.OrderRouting routing) {
        this.routing = routing;
        this.bundles = bundles;
        this.schedules = schedules;
        this.markets = markets;
        this.db = db;
        this.connectors = connectors;
        this.orders = orders;
        this.lines = lines;
        this.inventory = inventory;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 40000, lock = "bazaar-channel-sync")
    public void job() {
        try {
            tick(System.currentTimeMillis());
        } catch (IOException | RuntimeException failed) {
            System.err.println("channel sync: " + failed);
        }
    }

    /// One pass over every channel, as at `now`.
    public void tick(long now) throws IOException {
        List rows = db.query("SELECT id FROM sales_channel WHERE status <> ? AND next_attempt_at <= ?",
                new Object[] {PAUSED, Long.valueOf(now)});
        for (int iter = 0; iter < rows.size(); iter++) {
            try {
                run(Rows.text(((Map) rows.get(iter)).get("id")), now, false);
            } catch (IOException | RuntimeException failed) {
                System.err.println("channel sync: " + failed);
            }
        }
        db.execute("DELETE FROM channel_sync_log WHERE created_at < ?",
                new Object[] {Long.valueOf(now - LOG_KEEP_MS)});
    }

    /// Runs one channel now. `force` ignores the pull interval, the debounce
    /// and every backoff: the back office's "Sync now".
    public void run(String channelId, long now, boolean force) throws IOException {
        Map ch = channel(channelId);
        if (ch == null || PAUSED.equals(Rows.text(ch.get("status")))) {
            return;
        }
        MarketplaceConnector.Session session;
        try {
            session = session(ch, now, false);
        } catch (ChannelException e) {
            channelFailed(ch, now, "AUTH", e);
            return;
        }
        boolean pull = force || Rows.number(ch.get("pull_requested")) == 1
                || now - Rows.number(ch.get("last_pull_at")) >= PULL_EVERY_MS;
        try {
            if (pull) {
                pull(ch, session, now);
            }
            push(ch, session, now, force);
            ship(ch, session, now, force);
        } catch (ChannelException e) {
            channelFailed(ch, now, pull ? "PULL" : "PUSH", e);
            return;
        }
        if (!ACTIVE.equals(Rows.text(ch.get("status"))) || Rows.number(ch.get("fail_count")) > 0) {
            db.execute("UPDATE sales_channel SET status = ?, fail_count = 0, last_error = '' "
                    + "WHERE id = ?", new Object[] {ACTIVE, channelId});
        }
    }

    // ------------------------------------------------------------- auth

    private MarketplaceConnector.Session session(Map ch, long now, boolean renew)
            throws ChannelException, IOException {
        String id = Rows.text(ch.get("id"));
        String access = Rows.text(ch.get("access_token"));
        if (!renew && access.length() > 0 && Rows.number(ch.get("access_expires_at")) > now + 60000L) {
            return new MarketplaceConnector.Session(id, access);
        }
        MarketplaceConnector.Token t = connector(ch).refresh(Rows.text(ch.get("refresh_token")));
        db.execute("UPDATE sales_channel SET access_token = ?, access_expires_at = ? WHERE id = ?",
                new Object[] {t.accessToken, Long.valueOf(t.expiresAt), id});
        ch.put("access_token", t.accessToken);
        ch.put("access_expires_at", Long.valueOf(t.expiresAt));
        log(ch, "AUTH", "", OK, "Access token refreshed");
        return new MarketplaceConnector.Session(id, t.accessToken);
    }

    private MarketplaceConnector connector(Map ch) {
        return connectors.of(Rows.text(ch.get("kind")));
    }

    private void channelFailed(Map ch, long now, String action, ChannelException e)
            throws IOException {
        int fails = (int) Rows.number(ch.get("fail_count")) + 1;
        db.execute("UPDATE sales_channel SET status = ?, fail_count = ?, next_attempt_at = ?, "
                + "last_error = ? WHERE id = ?", new Object[] {ERROR, Integer.valueOf(fails),
                    Long.valueOf(now + backoff(fails, e.isRetryable())), cut(e.getMessage()),
                    Rows.text(ch.get("id"))});
        log(ch, action, "", ERROR, e.getMessage());
    }

    /// 30 s, 60 s, 2 min ... capped at an hour; a failure that will not fix
    /// itself starts at ten minutes.
    static long backoff(int fails, boolean retryable) {
        long wait = retryable ? FIRST_BACKOFF_MS : FIRST_BACKOFF_MS * 20;
        for (int iter = 1; iter < fails && wait < MAX_BACKOFF_MS; iter++) {
            wait *= 2;
        }
        return Math.min(wait, MAX_BACKOFF_MS);
    }

    /// Runs `call`, refreshing the token once if the marketplace refused it.
    private interface Call<T> {
        T run(MarketplaceConnector.Session s) throws ChannelException;
    }

    private <T> T withAuth(Map ch, MarketplaceConnector.Session[] s, long now, Call<T> call)
            throws ChannelException, IOException {
        try {
            return call.run(s[0]);
        } catch (ChannelException e) {
            if (!e.isAuth()) {
                throw e;
            }
            s[0] = session(ch, now, true);
            return call.run(s[0]);
        }
    }

    // ------------------------------------------------------------- pull

    private void pull(Map ch, MarketplaceConnector.Session session, final long now)
            throws ChannelException, IOException {
        final long since = Math.max(0, Rows.number(ch.get("last_pull_at")) - PULL_OVERLAP_MS);
        final MarketplaceConnector c = connector(ch);
        MarketplaceConnector.Session[] s = {session};
        List<MarketplaceConnector.ExternalOrder> found = withAuth(ch, s, now,
                x -> c.orders(x, since));
        int made = 0;
        for (int iter = 0; iter < found.size(); iter++) {
            MarketplaceConnector.ExternalOrder o = found.get(iter);
            try {
                if (importOrder(ch, o)) {
                    made++;
                }
            } catch (ResponseStatusException | IOException refused) {
                log(ch, "ORDER", o.id, ERROR, refused instanceof ResponseStatusException
                        ? ((ResponseStatusException) refused).getReason() : refused.getMessage());
            }
        }
        db.execute("UPDATE sales_channel SET last_pull_at = ?, pull_requested = 0 WHERE id = ?",
                new Object[] {Long.valueOf(now), Rows.text(ch.get("id"))});
        ch.put("last_pull_at", Long.valueOf(now));
        log(ch, "PULL", "", OK, found.size() + " orders read, " + made + " new");
    }

    /// Makes `o` a Bazaar order unless it already is one. Answers whether it
    /// made one.
    boolean importOrder(Map ch, MarketplaceConnector.ExternalOrder o) throws IOException {
        final String storeId = Rows.text(ch.get("store_id"));
        final String channelId = Rows.text(ch.get("id"));
        if (o.id == null || o.id.length() == 0 || o.id.length() > 64) {
            throw new ResponseStatusException(400, "An order without a usable id");
        }
        if (db.queryOne("SELECT id FROM channel_order WHERE channel_id = ? AND external_id = ?",
                new Object[] {channelId, o.id}) != null) {
            return false;
        }
        Map store = db.queryOne("SELECT currency FROM store WHERE id = ?", new Object[] {storeId});
        String currency = store == null ? "" : Rows.text(store.get("currency"));
        // An order in another currency is imported at the store's rate for
        // it (the market's chosen source, else ECB, else manual), locked now:
        // the books keep store currency, the order keeps what the buyer paid
        // in theirs, and refunds go back in that currency (Orders.refund).
        Markets.LockedRate fx = null;
        if (!currency.equals(o.currency)) {
            fx = markets.rateFor(storeId, o.currency);
            if (fx == null) {
                throw new ResponseStatusException(400, "Order " + o.id + " is in " + o.currency
                        + " and the store has no exchange rate for it");
            }
        }
        if (o.lines.isEmpty()) {
            throw new ResponseStatusException(400, "Order " + o.id + " has no lines");
        }
        final List<Map> resolved = new ArrayList<Map>();
        long subtotal = 0;
        for (int iter = 0; iter < o.lines.size(); iter++) {
            MarketplaceConnector.ExternalLine l = o.lines.get(iter);
            Map v = variantOf(channelId, storeId, l.sku);
            if (v == null) {
                throw new ResponseStatusException(400, "Order " + o.id + " sells SKU " + cut(l.sku)
                        + ", which no variant of the store has");
            }
            if (l.quantity <= 0 || l.quantity > 10000 || l.unitCents < 0) {
                throw new ResponseStatusException(400, "Order " + o.id + " has a line that cannot be");
            }
            resolved.add(v);
            subtotal = addCents(subtotal, lineCents(storeCents(l.unitCents, fx), l.quantity));
        }
        long foreignTotal = 0;
        if (fx != null) {
            long foreignSubtotal = 0;
            for (int iter = 0; iter < o.lines.size(); iter++) {
                MarketplaceConnector.ExternalLine l = o.lines.get(iter);
                foreignSubtotal = addCents(foreignSubtotal, lineCents(l.unitCents, l.quantity));
            }
            foreignTotal = o.totalCents > 0 ? o.totalCents : addCents(addCents(foreignSubtotal,
                    Math.max(0, o.shippingCents)), Math.max(0, o.taxCents));
        }
        StoreOrder order = new StoreOrder();
        order.setId(Ids.next());
        order.setStoreId(storeId);
        order.setChannel(MARKETPLACE);
        order.setStatus(Orders.OPEN);
        // The marketplace collected the money; refunds go back through it.
        order.setFinancial(Orders.PAID);
        order.setFulfilment(Orders.UNFULFILLED);
        order.setSubtotalCents(subtotal);
        order.setShippingCents(storeCents(Math.max(0, o.shippingCents), fx));
        order.setTaxCents(storeCents(Math.max(0, o.taxCents), fx));
        order.setTotalCents(o.totalCents > 0 ? storeCents(o.totalCents, fx)
                : addCents(addCents(subtotal, order.getShippingCents()), order.getTaxCents()));
        order.setCurrency(currency);
        if (fx != null) {
            order.setPresentmentCurrency(cut(o.currency, 3));
            order.setPresentmentTotalCents(foreignTotal);
            order.setFxRateMicros(fx.micros);
            order.setFxSource(fx.source);
        }
        order.setEmail(cut(o.buyerEmail, 190));
        order.setShipName(cut(o.shipName, 120));
        order.setShipLine1(cut(o.line1, 200));
        order.setShipLine2(cut(o.line2, 200));
        order.setShipCity(cut(o.city, 120));
        order.setShipRegion(cut(o.region, 80));
        order.setShipPostal(cut(o.postal, 20));
        order.setShipCountry(cut(o.country, 2));
        order.setShippingTitle(cut(Rows.text(ch.get("name")) + " shipping", 120));
        order.setNote(cut(Rows.text(ch.get("name")) + " order " + o.id, 1000));
        order.setClientId(clientId(channelId, o.id));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        final ConvertedOrder ext = new ConvertedOrder(o, fx);
        final List<String> oversold = new ArrayList<String>();
        final Set<String> variants = new HashSet<String>();
        StoreOrder saved = orders.insertNumbered(order, placed -> {
            oversold.clear();
            List<Map> refs = new ArrayList<Map>();
            for (int iter = 0; iter < ext.lines.size(); iter++) {
                MarketplaceConnector.ExternalLine l = ext.lines.get(iter);
                Map v = resolved.get(iter);
                String variantId = Rows.text(v.get("id"));
                variants.add(variantId);
                String loc = routing.plan(storeId, new String[] {variantId},
                        new int[] {l.quantity}, false, 0, 0).locations[0];
                if (loc == null) {
                    throw new ResponseStatusException(409, "The store has no location that "
                            + "fulfils online orders");
                }
                if (placed.getLocationId().length() == 0) {
                    placed.setLocationId(loc);
                }
                OrderLine line = new OrderLine();
                line.setId(Ids.next());
                line.setOrderId(placed.getId());
                line.setProductId(Rows.text(v.get("product_id")));
                line.setVariantId(variantId);
                line.setTitle(cut(Rows.text(v.get("ptitle")), 200));
                line.setVariantTitle(cut(Rows.text(v.get("title")), 200));
                line.setSku(cut(Rows.text(v.get("sku")), 64));
                line.setQuantity(l.quantity);
                line.setPriceCents(storeCents(l.unitCents, ext.fx));
                line.setLocationId(loc);
                lines.insert(line);
                String ref = "order:" + placed.getId();
                if (!inventory.reserve(storeId, variantId, loc, l.quantity, ref, SYNC_USER)) {
                    // Sold on the marketplace already: commit it anyway and
                    // say so, as an offline register sale does.
                    inventory.forceCommit(storeId, variantId, loc, l.quantity, ref, SYNC_USER);
                    oversold.add(line.getSku().length() > 0 ? line.getSku() : variantId);
                }
                Map r = new HashMap();
                r.put("lineId", l.lineId);
                r.put("variantId", variantId);
                r.put("quantity", Integer.valueOf(l.quantity));
                refs.add(r);
            }
            db.execute("UPDATE store_order SET location_id = ? WHERE id = ?",
                    new Object[] {placed.getLocationId(), placed.getId()});
            db.execute("INSERT INTO channel_order (id, store_id, channel_id, external_id, order_id, "
                    + "lines_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(),
                        storeId, channelId, ext.id, placed.getId(), Json.write(refs),
                        Long.valueOf(System.currentTimeMillis())});
            orders.event(placed.getId(), "PLACED", "Imported from " + Rows.text(ch.get("name"))
                    + " order " + ext.id, SYNC_USER);
            if (!oversold.isEmpty()) {
                String what = "Sold more than on hand on " + Rows.text(ch.get("name")) + ": "
                        + join(oversold);
                orders.event(placed.getId(), "OVERSOLD", what, SYNC_USER);
                db.execute("INSERT INTO audit_event (id, store_id, username, action, subject, detail, "
                        + "created_at) VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), storeId,
                            SYNC_USER, "CHANNEL_OVERSOLD", placed.getId(), cut("Order #"
                            + placed.getNumber() + " " + what), Long.valueOf(System.currentTimeMillis())});
            }
        });
        if (saved == null) {
            return false;
        }
        // Every channel of the store hears about the units this order took
        // at once, skipping the debounce, so the next sale elsewhere cannot
        // take units that are gone.
        List<String> vs = new ArrayList<String>(variants);
        for (int iter = 0; iter < vs.size(); iter++) {
            db.execute("UPDATE channel_listing SET urgent = 1 WHERE store_id = ? AND variant_id = ?",
                    new Object[] {storeId, vs.get(iter)});
        }
        log(ch, "ORDER", o.id, oversold.isEmpty() ? OK : ERROR, "Order #" + saved.getNumber()
                + (oversold.isEmpty() ? "" : ", oversold " + join(oversold)));
        return true;
    }

    /// An external order with the rate its money converts at (null for the
    /// store's own currency).
    private static final class ConvertedOrder {
        final String id;
        final List<MarketplaceConnector.ExternalLine> lines;
        final Markets.LockedRate fx;

        ConvertedOrder(MarketplaceConnector.ExternalOrder o, Markets.LockedRate fx) {
            this.id = o.id;
            this.lines = o.lines;
            this.fx = fx;
        }
    }

    /// `cents` of the order's currency in the store's: `cents / rate`, half
    /// up, at the locked rate in millionths. Unchanged without a rate; an
    /// amount too large to convert is a 400.
    static long storeCents(long cents, Markets.LockedRate fx) {
        if (fx == null) {
            return cents;
        }
        if (fx.micros <= 0) {
            throw new ResponseStatusException(400, "The exchange rate is not usable");
        }
        try {
            return Math.addExact(Math.multiplyExact(cents, 1000000L), fx.micros / 2) / fx.micros;
        } catch (ArithmeticException overflow) {
            throw new ResponseStatusException(400, "That order is too large to convert");
        }
    }

    private static long lineCents(long unitCents, int quantity) {
        try {
            return Math.multiplyExact(unitCents, (long) quantity);
        } catch (ArithmeticException overflow) {
            throw new ResponseStatusException(400, "That order is too large");
        }
    }

    private static long addCents(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException overflow) {
            throw new ResponseStatusException(400, "That order is too large");
        }
    }

    /// The store's `client_id` for an external order: unique per channel and
    /// external id, at most 64 characters.
    static String clientId(String channelId, String externalId) throws IOException {
        String tail = externalId.length() <= 30 ? externalId
                : Tokens.sign(channelId, externalId).substring(0, 30);
        return "m" + channelId + ":" + tail;
    }

    private Map variantOf(String channelId, String storeId, String sku) throws IOException {
        if (sku == null || sku.length() == 0 || sku.length() > 64) {
            return null;
        }
        String select = "SELECT v.id, v.product_id, v.title, v.sku, p.title AS ptitle FROM variant v "
                + "JOIN product p ON p.id = v.product_id ";
        Map v = db.queryOne(select + "JOIN channel_listing l ON l.variant_id = v.id WHERE "
                + "l.channel_id = ? AND l.external_sku = ? AND p.store_id = ?",
                new Object[] {channelId, sku, storeId});
        if (v == null) {
            v = db.queryOne(select + "WHERE p.store_id = ? AND v.sku = ?", new Object[] {storeId, sku});
        }
        return v;
    }

    // ------------------------------------------------------------- push

    private void push(Map ch, MarketplaceConnector.Session session, long now, boolean force)
            throws ChannelException, IOException {
        String channelId = Rows.text(ch.get("id"));
        String storeId = Rows.text(ch.get("store_id"));
        long cursor = Rows.number(ch.get("ledger_cursor"));
        Set<String> moved = new HashSet<String>();
        long newest = cursor;
        List ms = db.query("SELECT variant_id, created_at FROM inventory_move WHERE store_id = ? "
                + "AND created_at > ?", new Object[] {storeId, Long.valueOf(cursor)});
        for (int iter = 0; iter < ms.size(); iter++) {
            Map m = (Map) ms.get(iter);
            moved.add(Rows.text(m.get("variant_id")));
            newest = Math.max(newest, Rows.millis(m.get("created_at")));
        }
        Map store = db.queryOne("SELECT currency FROM store WHERE id = ?", new Object[] {storeId});
        String currency = store == null ? "" : Rows.text(store.get("currency"));
        List ls = db.query("SELECT l.*, v.price_cents AS vprice, v.track_inventory AS tracked, "
                + "v.product_id AS vproduct FROM channel_listing l JOIN variant v ON v.id = "
                + "l.variant_id WHERE l.channel_id = ?", new Object[] {channelId});
        // A product outside its window on this channel is pushed as none
        // available, which takes it off the marketplace; a bundle is what its
        // components make, and moves when any of them does.
        Set<String> hidden = schedules.hidden(storeId, channelId);
        Map<String, List<com.codenameone.examples.bazaar.catalog.Bundles.Part>> bundleParts =
                bundles.byVariant(storeId);
        for (Map.Entry<String, List<com.codenameone.examples.bazaar.catalog.Bundles.Part>> e
                : bundleParts.entrySet()) {
            for (int p = 0; p < e.getValue().size(); p++) {
                if (moved.contains(e.getValue().get(p).variantId)) {
                    moved.add(e.getKey());
                }
            }
        }
        final MarketplaceConnector c = connector(ch);
        MarketplaceConnector.Session[] s = {session};
        for (int iter = 0; iter < ls.size(); iter++) {
            Map l = (Map) ls.get(iter);
            String variantId = Rows.text(l.get("variant_id"));
            long price = Rows.number(l.get("vprice"));
            String pushed = Rows.text(l.get("pushed_state"));
            boolean urgent = Rows.number(l.get("urgent")) == 1;
            boolean retrying = ERROR.equals(Rows.text(l.get("status")));
            boolean look = force || urgent || retrying || pushed.length() == 0
                    || moved.contains(variantId) || price != Rows.number(l.get("pushed_price_cents"))
                    || !pushed.equals(Rows.text(l.get("seen_state")));
            if (!look || !force && Rows.number(l.get("next_attempt_at")) > now) {
                continue;
            }
            List<com.codenameone.examples.bazaar.catalog.Bundles.Part> parts =
                    bundleParts.get(variantId);
            int qty = hidden.contains(Rows.text(l.get("vproduct"))) ? 0
                    : parts != null ? Math.min(UNTRACKED_QTY, bundles.available(parts,
                        onlineLocations(storeId)))
                    : Rows.number(l.get("tracked")) == 1 ? available(variantId) : UNTRACKED_QTY;
            String state = qty + "@" + price;
            String id = Rows.text(l.get("id"));
            if (state.equals(pushed)) {
                db.execute("UPDATE channel_listing SET status = ?, urgent = 0, seen_state = ?, "
                        + "fail_count = 0, last_error = '' WHERE id = ?", new Object[] {OK, state, id});
                continue;
            }
            if (!force && !urgent) {
                if (!state.equals(Rows.text(l.get("seen_state")))) {
                    db.execute("UPDATE channel_listing SET seen_state = ?, seen_at = ? WHERE id = ?",
                            new Object[] {state, Long.valueOf(now), id});
                    continue;
                }
                if (now - Rows.number(l.get("seen_at")) < DEBOUNCE_MS) {
                    continue;
                }
            }
            long version = Rows.number(l.get("version"));
            if (db.execute("UPDATE channel_listing SET version = ? WHERE id = ? AND version = ?",
                    new Object[] {Long.valueOf(version + 1), id, Long.valueOf(version)}) != 1) {
                continue;
            }
            final MarketplaceConnector.ListingPush p = new MarketplaceConnector.ListingPush(
                    Rows.text(l.get("external_sku")), Rows.text(l.get("external_id")), qty, price,
                    currency, id + ":" + (version + 1));
            try {
                MarketplaceConnector.PushResult r = withAuth(ch, s, now, x -> c.upsert(x, p));
                db.execute("UPDATE channel_listing SET pushed_state = ?, seen_state = ?, pushed_qty = ?, "
                        + "pushed_price_cents = ?, external_id = ?, etag = ?, status = ?, fail_count = 0, "
                        + "urgent = 0, last_error = '', next_attempt_at = 0, updated_at = ? WHERE id = ?",
                        new Object[] {state, state, Integer.valueOf(qty), Long.valueOf(price),
                            r.externalId.length() > 0 ? cut(r.externalId, 64)
                                : Rows.text(l.get("external_id")), cut(r.etag, 128), OK,
                            Long.valueOf(now), id});
                log(ch, "PUSH", p.sku, OK, qty + " available at " + Cents.format(price, currency) + " "
                        + currency);
            } catch (ChannelException e) {
                if (e.isAuth()) {
                    throw e;
                }
                int fails = (int) Rows.number(l.get("fail_count")) + 1;
                db.execute("UPDATE channel_listing SET status = ?, fail_count = ?, next_attempt_at = ?, "
                        + "last_error = ?, seen_state = ?, updated_at = ? WHERE id = ?", new Object[] {ERROR,
                            Integer.valueOf(fails), Long.valueOf(now + backoff(fails, e.isRetryable())),
                            cut(e.getMessage()), state, Long.valueOf(now), id});
                log(ch, "PUSH", p.sku, ERROR, e.getMessage());
            }
        }
        if (newest > cursor) {
            db.execute("UPDATE sales_channel SET ledger_cursor = ? WHERE id = ?",
                    new Object[] {Long.valueOf(newest), channelId});
        }
    }

    /// The active locations of a store that fulfil online orders.
    private Set<String> onlineLocations(String storeId) throws IOException {
        Set<String> out = new HashSet<String>();
        List rs = db.query("SELECT id FROM location WHERE store_id = ? AND active = 1 AND "
                + "fulfils_online = 1", new Object[] {storeId});
        for (int iter = 0; iter < rs.size(); iter++) {
            out.add(Rows.text(((Map) rs.get(iter)).get("id")));
        }
        return out;
    }

    /// Units of a variant free to sell online: on hand less committed, summed
    /// over the active locations that fulfil online orders, never below zero.
    int available(String variantId) throws IOException {
        List rs = db.query("SELECT il.on_hand, il.committed FROM inventory_level il JOIN location lo "
                + "ON lo.id = il.location_id WHERE il.variant_id = ? AND lo.active = 1 AND "
                + "lo.fulfils_online = 1", new Object[] {variantId});
        long total = 0;
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            total += Rows.number(r.get("on_hand")) - Rows.number(r.get("committed"));
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, total));
    }

    // ------------------------------------------------------------- ship

    private void ship(Map ch, MarketplaceConnector.Session session, long now, boolean force)
            throws ChannelException, IOException {
        String channelId = Rows.text(ch.get("id"));
        List fs = db.query("SELECT f.id, f.carrier, f.tracking_number, f.updated_at, co.external_id, "
                + "co.lines_json, cf.id AS cfid, cf.fail_count, cf.next_attempt_at, cf.status AS cfstatus "
                + "FROM fulfillment f JOIN channel_order co ON co.order_id = f.order_id LEFT JOIN "
                + "channel_fulfillment cf ON cf.fulfillment_id = f.id WHERE co.channel_id = ? AND "
                + "f.status IN ('SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED')",
                new Object[] {channelId});
        final MarketplaceConnector c = connector(ch);
        MarketplaceConnector.Session[] s = {session};
        for (int iter = 0; iter < fs.size(); iter++) {
            Map f = (Map) fs.get(iter);
            String cfid = Rows.text(f.get("cfid"));
            if (cfid.length() > 0 && (OK.equals(Rows.text(f.get("cfstatus")))
                    || !force && Rows.number(f.get("next_attempt_at")) > now)) {
                continue;
            }
            String fid = Rows.text(f.get("id"));
            if (cfid.length() == 0) {
                cfid = Ids.next();
                db.execute("INSERT INTO channel_fulfillment (id, store_id, channel_id, fulfillment_id, "
                        + "status, fail_count, next_attempt_at, last_error, updated_at) VALUES "
                        + "(?, ?, ?, ?, ?, 0, 0, '', ?)", new Object[] {cfid, Rows.text(ch.get("store_id")),
                            channelId, fid, PENDING, Long.valueOf(now)});
            }
            final MarketplaceConnector.Shipment shipment = new MarketplaceConnector.Shipment(
                    Rows.text(f.get("external_id")), Rows.text(f.get("carrier")),
                    Rows.text(f.get("tracking_number")), now, shipLines(fid,
                    Rows.text(f.get("lines_json"))));
            try {
                withAuth(ch, s, now, x -> {
                    c.ship(x, shipment);
                    return Boolean.TRUE;
                });
                db.execute("UPDATE channel_fulfillment SET status = ?, fail_count = 0, last_error = '', "
                        + "updated_at = ? WHERE id = ?", new Object[] {OK, Long.valueOf(now), cfid});
                log(ch, "SHIP", shipment.orderId, OK, shipment.carrier + " " + shipment.tracking);
            } catch (ChannelException e) {
                if (e.isAuth()) {
                    throw e;
                }
                int fails = (int) Rows.number(f.get("fail_count")) + 1;
                db.execute("UPDATE channel_fulfillment SET status = ?, fail_count = ?, next_attempt_at = ?, "
                        + "last_error = ?, updated_at = ? WHERE id = ?", new Object[] {ERROR,
                            Integer.valueOf(fails), Long.valueOf(now + backoff(fails, e.isRetryable())),
                            cut(e.getMessage()), Long.valueOf(now), cfid});
                log(ch, "SHIP", shipment.orderId, ERROR, e.getMessage());
            }
        }
    }

    /// The marketplace line ids and quantities of one fulfilment.
    private List<MarketplaceConnector.ExternalLine> shipLines(String fulfillmentId, String json)
            throws IOException {
        Map<String, String> lineOf = new HashMap<String, String>();
        Object parsed = json.length() == 0 ? null : Json.parse(json);
        List refs = parsed instanceof List ? (List) parsed : new ArrayList();
        for (int iter = 0; iter < refs.size(); iter++) {
            if (refs.get(iter) instanceof Map) {
                Map r = (Map) refs.get(iter);
                lineOf.put(Rows.text(r.get("variantId")), Rows.text(r.get("lineId")));
            }
        }
        List rs = db.query("SELECT fl.quantity, ol.variant_id, ol.sku FROM fulfillment_line fl JOIN "
                + "order_line ol ON ol.id = fl.order_line_id WHERE fl.fulfillment_id = ?",
                new Object[] {fulfillmentId});
        List<MarketplaceConnector.ExternalLine> out = new ArrayList<MarketplaceConnector.ExternalLine>();
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            String lineId = lineOf.get(Rows.text(r.get("variant_id")));
            out.add(new MarketplaceConnector.ExternalLine(lineId, Rows.text(r.get("sku")),
                    (int) Rows.number(r.get("quantity")), 0));
        }
        return out;
    }

    // ------------------------------------------------------- notifications

    /// A marketplace notification for a channel: refused (401, the same for an
    /// unknown channel as for a bad signature) unless signed with the
    /// channel's secret; otherwise the channel pulls its orders now. A
    /// notification sent twice pulls twice and makes each order once.
    public void notification(String channelId, byte[] body, String signature, long now)
            throws IOException {
        Map ch = channelId == null || channelId.length() > 32 ? null : channel(channelId);
        if (ch == null || !connector(ch).verify(Rows.text(ch.get("webhook_secret")), body, signature)) {
            if (ch != null) {
                log(ch, "NOTIFY", "", ERROR, "A notification with a bad signature was refused");
            }
            throw new ResponseStatusException(401, "refused");
        }
        db.execute("UPDATE sales_channel SET pull_requested = 1 WHERE id = ?", new Object[] {channelId});
        log(ch, "NOTIFY", "", OK, "Notification received");
        if (Rows.number(ch.get("next_attempt_at")) <= now) {
            run(channelId, now, false);
        }
    }

    // ------------------------------------------------------------ helpers

    Map channel(String channelId) throws IOException {
        return db.queryOne("SELECT * FROM sales_channel WHERE id = ?", new Object[] {channelId});
    }

    void log(Map ch, String action, String ref, String outcome, String message) throws IOException {
        db.execute("INSERT INTO channel_sync_log (id, store_id, channel_id, action, ref, outcome, "
                + "message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(),
                    Rows.text(ch.get("store_id")), Rows.text(ch.get("id")), action, cut(ref, 64),
                    outcome, cut(message), Long.valueOf(System.currentTimeMillis())});
    }

    static String cut(String s) {
        return cut(s, 500);
    }

    static String cut(String s, int max) {
        String v = s == null ? "" : s;
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static String join(List<String> items) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < items.size(); iter++) {
            if (iter > 0) {
                b.append(", ");
            }
            b.append(items.get(iter));
        }
        return b.toString();
    }
}
