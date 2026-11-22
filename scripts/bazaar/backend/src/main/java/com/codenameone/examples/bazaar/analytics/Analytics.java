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
package com.codenameone.examples.bazaar.analytics;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.BreakdownDto;
import com.codenameone.examples.bazaar.api.FunnelStepDto;
import com.codenameone.examples.bazaar.api.InventoryValueDto;
import com.codenameone.examples.bazaar.api.KpiDto;
import com.codenameone.examples.bazaar.api.InsightsDto;
import com.codenameone.examples.bazaar.api.SeriesPointDto;
import com.codenameone.examples.bazaar.api.TopItemDto;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Refund;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.CartRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.RefundRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A store's sales dashboard for a period, each figure beside the same
/// figure for the period of equal length just before it.
///
/// Definitions, so the numbers on the screen can be checked by hand:
///
/// - An order counts unless it is cancelled or an unconverted draft.
/// - Gross is the counted orders' subtotals; discounts their discounts; net
///   is gross less discounts less the refunds made in the period (a refund
///   is dated by when it was made, not by its order).
/// - Average order is (gross - discounts) / orders.
/// - Returning-customer rate: of the customers who ordered in the period,
///   the share who had a counted order before it, in basis points.
/// - Conversion: online orders / carts created in the period, in basis points.
/// - Funnel: carts, carts that reached checkout (they carry an email or a
///   signed-in shopper), online orders, online orders fulfilled.
///
/// Days and hours are UTC. Everything is added up in memory from the rows,
/// as the admin's statistics are.
@Component
public class Analytics {
    static final long DEFAULT_DAYS = 30;
    static final long MAX_DAYS = 400;
    static final int TOP = 10;

    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final RefundRepository refunds;
    private final CartRepository carts;
    private final CustomerRepository customers;
    private final InventoryLevelRepository levels;
    private final VariantRepository variants;
    private final LocationRepository locations;
    private final StoreAccess access;

    public Analytics(StoreOrderRepository orders, OrderLineRepository lines,
            RefundRepository refunds, CartRepository carts, CustomerRepository customers,
            InventoryLevelRepository levels, VariantRepository variants,
            LocationRepository locations, StoreAccess access) {
        this.orders = orders;
        this.lines = lines;
        this.refunds = refunds;
        this.carts = carts;
        this.customers = customers;
        this.levels = levels;
        this.variants = variants;
        this.locations = locations;
        this.access = access;
    }

    /// `[from, to)`, defaulting to the last 30 days.
    static long[] period(long from, long to, long now) {
        long end = to > 0 ? to : now;
        long start = from > 0 ? from : end - DEFAULT_DAYS * Days.MILLIS;
        if (end <= start) {
            throw new ResponseStatusException(400, "The period ends before it starts");
        }
        if (end - start > MAX_DAYS * Days.MILLIS) {
            throw new ResponseStatusException(400, "A period is at most " + MAX_DAYS + " days");
        }
        return new long[] {start, end, start - (end - start)};
    }

    @Transactional(readOnly = true)
    public AnalyticsDto dashboard(String username, String storeId, long from, long to)
            throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.MANAGERS);
        Store store = access.requireStore(username, storeId);
        long[] p = period(from, to, System.currentTimeMillis());
        Data d = load(storeId, p[2]);
        List<StoreOrder> now = d.between(p[0], p[1]);
        List<StoreOrder> before = d.between(p[2], p[0]);
        KpiDto current = kpis(d, now, p[0], p[1]);
        KpiDto previous = kpis(d, before, p[2], p[0]);
        Map<String, List<OrderLine>> byOrder = lines(now);
        return new AnalyticsDto(p[0], p[1], p[2], current, previous, daily(now, p[0], p[1]),
                hourly(now), byChannel(now), byLocation(storeId, now), topProducts(now, byOrder),
                topVariants(now, byOrder), topCustomers(now), discounts(now),
                inventory(storeId, byOrder), funnel(d, now, p[0], p[1]), store.getCurrency());
    }

    @Transactional(readOnly = true)
    public List<FunnelStepDto> funnel(String username, String storeId, long from, long to)
            throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.MANAGERS);
        long[] p = period(from, to, System.currentTimeMillis());
        Data d = load(storeId, p[0]);
        return funnel(d, d.between(p[0], p[1]), p[0], p[1]);
    }

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.repository.ProductViewEventRepository views;

    /// Sales by channel and the views-to-orders funnel of a period, each
    /// counted by the database rather than added up here:
    ///
    /// - channels: `web` (online orders placed on the storefront, and those
    ///   from before the source was kept), `app` (the app and the API), `pos`,
    ///   `marketplace`, and `draft` (draft orders turned into orders); orders
    ///   and sales (subtotal less discounts) as the dashboard counts them.
    /// - funnel: product pages viewed, carts created, carts that reached
    ///   checkout (an email or an account, as [#funnel] counts them), and
    ///   online orders.
    @Transactional(readOnly = true)
    public InsightsDto insights(String username, String storeId, long from, long to)
            throws IOException {
        access.requireArea(username, storeId, Perms.REPORTS, false, StoreAccess.MANAGERS);
        Store store = access.requireStore(username, storeId);
        long[] p = period(from, to, System.currentTimeMillis());
        Instant start = Instant.ofEpochMilli(p[0]);
        Instant end = Instant.ofEpochMilli(p[1]);
        long[][] sums = new long[CHANNELS.length][2];
        List<Object[]> mix = orders.channelMix(storeId, start, end, Orders.CANCELLED,
                Orders.DRAFT);
        for (int iter = 0; iter < mix.size(); iter++) {
            Object[] row = mix.get(iter);
            int at = channelIndex(text(row[0]), text(row[1]));
            if (at >= 0) {
                sums[at][0] += number(row[2]);
                sums[at][1] += number(row[3]) - number(row[4]);
            }
        }
        List<Object[]> drafts = orders.convertedDrafts(storeId, start, end, Orders.CANCELLED,
                Orders.DRAFT, "");
        if (!drafts.isEmpty()) {
            Object[] row = drafts.get(0);
            sums[4][0] += number(row[0]);
            sums[4][1] += number(row[1]) - number(row[2]);
        }
        List<BreakdownDto> channels = new ArrayList<BreakdownDto>();
        for (int iter = 0; iter < CHANNELS.length; iter++) {
            channels.add(new BreakdownDto(CHANNELS[iter], CHANNEL_TITLES[iter],
                    (int) sums[iter][0], sums[iter][1]));
        }
        long cartCount = number(carts.countCreated(storeId, start, end));
        long anonymous = number(carts.countAnonymous(storeId, start, end, ""));
        List<FunnelStepDto> funnel = new ArrayList<FunnelStepDto>();
        funnel.add(new FunnelStepDto("views", "Product views",
                (int) number(views.countViews(storeId, start, end))));
        funnel.add(new FunnelStepDto("carts", "Carts", (int) cartCount));
        funnel.add(new FunnelStepDto("checkouts", "Reached checkout",
                (int) (cartCount - anonymous)));
        funnel.add(new FunnelStepDto("orders", "Ordered", (int) number(orders.countOnline(storeId,
                start, end, Orders.CANCELLED, Orders.ONLINE))));
        return new InsightsDto(p[0], p[1], channels, funnel, store.getCurrency());
    }

    static final String[] CHANNELS = {"web", "app", "pos", "marketplace", "draft"};
    static final String[] CHANNEL_TITLES = {"Online store", "App", "Point of sale",
        "Marketplaces", "Draft orders"};

    /// The [#CHANNELS] slot of an order's channel and source, or -1.
    static int channelIndex(String channel, String source) {
        if (Orders.ONLINE.equals(channel)) {
            return Orders.SOURCE_APP.equals(source) ? 1 : 0;
        }
        if (Orders.POS.equals(channel)) {
            return 2;
        }
        if ("MARKETPLACE".equals(channel)) {
            return 3;
        }
        return -1;
    }

    private static String text(Object o) {
        return o == null ? "" : o.toString();
    }

    /// A count or sum the database answered: a Long, an Integer or a decimal
    /// depending on the engine, and null for a sum of no rows.
    static long number(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0;
    }

    /// The rows a dashboard reads, from `since` on; every counted order of
    /// the store is kept apart to know who was a customer before.
    static final class Data {
        final List<StoreOrder> counted = new ArrayList<StoreOrder>();
        final List<Refund> refunds = new ArrayList<Refund>();
        final List<Cart> carts = new ArrayList<Cart>();

        List<StoreOrder> between(long from, long to) {
            List<StoreOrder> out = new ArrayList<StoreOrder>();
            for (int iter = 0; iter < counted.size(); iter++) {
                long at = counted.get(iter).getCreatedAt().toEpochMilli();
                if (at >= from && at < to) {
                    out.add(counted.get(iter));
                }
            }
            return out;
        }
    }

    private Data load(String storeId, long since) {
        Data d = new Data();
        List<StoreOrder> all = orders.findByStoreIdAndCreatedAtGreaterThanEqual(storeId,
                Instant.EPOCH);
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < all.size(); iter++) {
            StoreOrder o = all.get(iter);
            if (counts(o)) {
                d.counted.add(o);
            }
            ids.add(o.getId());
        }
        if (!ids.isEmpty()) {
            d.refunds.addAll(refunds.findByOrderIdIn(ids));
        }
        d.carts.addAll(carts.findByStoreIdAndCreatedAtGreaterThanEqual(storeId,
                Instant.ofEpochMilli(since)));
        return d;
    }

    static boolean counts(StoreOrder o) {
        return !"CANCELLED".equals(o.getStatus()) && !Orders.isUnconvertedDraft(o);
    }

    /// The headline figures of `period` (the orders placed in `[from, to)`).
    static KpiDto kpis(Data d, List<StoreOrder> period, long from, long to) {
        long gross = 0;
        long disc = 0;
        long tax = 0;
        long ship = 0;
        int online = 0;
        Set<String> who = new HashSet<String>();
        for (int iter = 0; iter < period.size(); iter++) {
            StoreOrder o = period.get(iter);
            gross += o.getSubtotalCents();
            disc += o.getDiscountCents();
            tax += o.getTaxCents();
            ship += o.getShippingCents();
            if (Orders.ONLINE.equals(o.getChannel())) {
                online++;
            }
            if (o.getCustomerId().length() > 0) {
                who.add(o.getCustomerId());
            }
        }
        Set<String> earlier = new HashSet<String>();
        for (int iter = 0; iter < d.counted.size(); iter++) {
            StoreOrder o = d.counted.get(iter);
            if (o.getCreatedAt().toEpochMilli() < from && o.getCustomerId().length() > 0) {
                earlier.add(o.getCustomerId());
            }
        }
        int returning = 0;
        for (String id : who) {
            if (earlier.contains(id)) {
                returning++;
            }
        }
        long refunded = 0;
        for (int iter = 0; iter < d.refunds.size(); iter++) {
            long at = d.refunds.get(iter).getCreatedAt().toEpochMilli();
            if (at >= from && at < to) {
                refunded += d.refunds.get(iter).getAmountCents();
            }
        }
        int cartCount = 0;
        for (int iter = 0; iter < d.carts.size(); iter++) {
            long at = d.carts.get(iter).getCreatedAt().toEpochMilli();
            if (at >= from && at < to) {
                cartCount++;
            }
        }
        int n = period.size();
        return new KpiDto(gross, disc, refunded, gross - disc - refunded, tax, ship, n,
                n == 0 ? 0 : (gross - disc) / n, who.size(), bp(returning, who.size()), cartCount,
                bp(online, cartCount));
    }

    /// `part / whole` in basis points, 0 for an empty whole.
    static int bp(long part, long whole) {
        return whole <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, part * 10000L / whole);
    }

    private static long sales(StoreOrder o) {
        return o.getSubtotalCents() - o.getDiscountCents();
    }

    static List<SeriesPointDto> daily(List<StoreOrder> period, long from, long to) {
        long first = Days.number(from);
        int days = (int) (Days.number(to - 1) - first + 1);
        int[] count = new int[days];
        long[] cents = new long[days];
        for (int iter = 0; iter < period.size(); iter++) {
            int day = (int) (Days.number(period.get(iter).getCreatedAt().toEpochMilli()) - first);
            if (day >= 0 && day < days) {
                count[day]++;
                cents[day] += sales(period.get(iter));
            }
        }
        List<SeriesPointDto> out = new ArrayList<SeriesPointDto>();
        for (int iter = 0; iter < days; iter++) {
            out.add(new SeriesPointDto(Days.iso((first + iter) * Days.MILLIS), count[iter],
                    cents[iter]));
        }
        return out;
    }

    static List<SeriesPointDto> hourly(List<StoreOrder> period) {
        int[] count = new int[24];
        long[] cents = new long[24];
        for (int iter = 0; iter < period.size(); iter++) {
            int h = (int) (period.get(iter).getCreatedAt().toEpochMilli() % Days.MILLIS / 3600000L);
            count[h]++;
            cents[h] += sales(period.get(iter));
        }
        List<SeriesPointDto> out = new ArrayList<SeriesPointDto>();
        for (int iter = 0; iter < 24; iter++) {
            out.add(new SeriesPointDto((iter < 10 ? "0" : "") + iter + ":00", count[iter],
                    cents[iter]));
        }
        return out;
    }

    private static final class Sum {
        String id;
        String title;
        String subtitle;
        int orders;
        int quantity;
        long cents;
    }

    private static Sum sum(Map<String, Sum> sums, String key, String title, String subtitle) {
        Sum s = sums.get(key);
        if (s == null) {
            s = new Sum();
            s.id = key;
            s.title = title;
            s.subtitle = subtitle;
            sums.put(key, s);
        }
        return s;
    }

    private static List<BreakdownDto> breakdown(Map<String, Sum> sums) {
        List<Sum> all = sorted(sums);
        List<BreakdownDto> out = new ArrayList<BreakdownDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Sum s = all.get(iter);
            out.add(new BreakdownDto(s.id, s.title, s.orders, s.cents));
        }
        return out;
    }

    private static List<TopItemDto> top(Map<String, Sum> sums) {
        List<Sum> all = sorted(sums);
        List<TopItemDto> out = new ArrayList<TopItemDto>();
        for (int iter = 0; iter < all.size() && iter < TOP; iter++) {
            Sum s = all.get(iter);
            out.add(new TopItemDto(s.id, s.title, s.subtitle, s.quantity, s.cents));
        }
        return out;
    }

    private static List<Sum> sorted(Map<String, Sum> sums) {
        List<Sum> all = new ArrayList<Sum>(sums.values());
        Collections.sort(all, new Comparator<Sum>() {
            @Override
            public int compare(Sum a, Sum b) {
                int c = Long.compare(b.cents, a.cents);
                return c != 0 ? c : a.id.compareTo(b.id);
            }
        });
        return all;
    }

    static List<BreakdownDto> byChannel(List<StoreOrder> period) {
        Map<String, Sum> sums = new LinkedHashMap<String, Sum>();
        for (int iter = 0; iter < period.size(); iter++) {
            StoreOrder o = period.get(iter);
            Sum s = sum(sums, o.getChannel(), Orders.POS.equals(o.getChannel()) ? "Point of sale"
                    : "Online store", "");
            s.orders++;
            s.cents += sales(o);
        }
        return breakdown(sums);
    }

    private List<BreakdownDto> byLocation(String storeId, List<StoreOrder> period) {
        Map<String, String> names = new HashMap<String, String>();
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            names.put(all.get(iter).getId(), all.get(iter).getName());
        }
        Map<String, Sum> sums = new LinkedHashMap<String, Sum>();
        for (int iter = 0; iter < period.size(); iter++) {
            StoreOrder o = period.get(iter);
            String id = o.getLocationId();
            String name = names.containsKey(id) ? names.get(id) : "Online";
            Sum s = sum(sums, id.length() == 0 ? "online" : id, name, "");
            s.orders++;
            s.cents += sales(o);
        }
        return breakdown(sums);
    }

    private Map<String, List<OrderLine>> lines(List<StoreOrder> period) {
        Map<String, List<OrderLine>> out = new HashMap<String, List<OrderLine>>();
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < period.size(); iter++) {
            ids.add(period.get(iter).getId());
            out.put(period.get(iter).getId(), new ArrayList<OrderLine>());
        }
        if (!ids.isEmpty()) {
            List<OrderLine> all = lines.findByOrderIdIn(ids);
            for (int iter = 0; iter < all.size(); iter++) {
                out.get(all.get(iter).getOrderId()).add(all.get(iter));
            }
        }
        return out;
    }

    private static long lineCents(OrderLine l) {
        return l.getPriceCents() * l.getQuantity() - l.getDiscountCents();
    }

    static List<TopItemDto> topProducts(List<StoreOrder> period,
            Map<String, List<OrderLine>> byOrder) {
        Map<String, Sum> sums = new HashMap<String, Sum>();
        for (int iter = 0; iter < period.size(); iter++) {
            List<OrderLine> ls = byOrder.get(period.get(iter).getId());
            for (int l = 0; ls != null && l < ls.size(); l++) {
                OrderLine line = ls.get(l);
                Sum s = sum(sums, line.getProductId(), line.getTitle(), "");
                s.quantity += line.getQuantity();
                s.cents += lineCents(line);
            }
        }
        return top(sums);
    }

    static List<TopItemDto> topVariants(List<StoreOrder> period,
            Map<String, List<OrderLine>> byOrder) {
        Map<String, Sum> sums = new HashMap<String, Sum>();
        for (int iter = 0; iter < period.size(); iter++) {
            List<OrderLine> ls = byOrder.get(period.get(iter).getId());
            for (int l = 0; ls != null && l < ls.size(); l++) {
                OrderLine line = ls.get(l);
                Sum s = sum(sums, line.getVariantId(), line.getTitle(),
                        line.getVariantTitle().length() > 0 ? line.getVariantTitle() : line.getSku());
                s.quantity += line.getQuantity();
                s.cents += lineCents(line);
            }
        }
        return top(sums);
    }

    private List<TopItemDto> topCustomers(List<StoreOrder> period) {
        Map<String, Sum> sums = new HashMap<String, Sum>();
        for (int iter = 0; iter < period.size(); iter++) {
            StoreOrder o = period.get(iter);
            if (o.getCustomerId().length() == 0) {
                continue;
            }
            Sum s = sum(sums, o.getCustomerId(), o.getEmail(), "");
            s.quantity++;
            s.cents += sales(o);
        }
        List<TopItemDto> out = top(sums);
        List<TopItemDto> named = new ArrayList<TopItemDto>();
        for (int iter = 0; iter < out.size(); iter++) {
            TopItemDto t = out.get(iter);
            Customer c = customers.findById(t.id());
            String name = c == null ? "" : (c.getFirstName() + " " + c.getLastName()).trim();
            named.add(new TopItemDto(t.id(), name.length() > 0 ? name : t.title(),
                    c == null ? t.title() : c.getEmail(), t.quantity(), t.salesCents()));
        }
        return named;
    }

    /// Discounts by code: how many orders used each and how much it took off.
    static List<BreakdownDto> discounts(List<StoreOrder> period) {
        Map<String, Sum> sums = new LinkedHashMap<String, Sum>();
        for (int iter = 0; iter < period.size(); iter++) {
            StoreOrder o = period.get(iter);
            if (o.getDiscountCents() <= 0) {
                continue;
            }
            String code = o.getDiscountCode().length() == 0 ? "automatic" : o.getDiscountCode();
            Sum s = sum(sums, code, code, "");
            s.orders++;
            s.cents += o.getDiscountCents();
        }
        return breakdown(sums);
    }

    /// Stock on hand at cost and at price, and sell-through: units sold in
    /// the period over units sold plus units left.
    private InventoryValueDto inventory(String storeId, Map<String, List<OrderLine>> byOrder) {
        Map<String, Variant> byId = new HashMap<String, Variant>();
        List<Variant> all = variants.findByStoreId(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            byId.put(all.get(iter).getId(), all.get(iter));
        }
        List<InventoryLevel> stock = levels.findByStoreId(storeId);
        int units = 0;
        long cost = 0;
        long retail = 0;
        int low = 0;
        for (int iter = 0; iter < stock.size(); iter++) {
            InventoryLevel l = stock.get(iter);
            Variant v = byId.get(l.getVariantId());
            int onHand = Math.max(0, l.getOnHand());
            units += onHand;
            if (v != null) {
                cost += v.getCostCents() * onHand;
                retail += v.getPriceCents() * onHand;
            }
            if (l.getReorderPoint() > 0 && l.getOnHand() - l.getCommitted() <= l.getReorderPoint()) {
                low++;
            }
        }
        int sold = 0;
        for (List<OrderLine> ls : byOrder.values()) {
            for (int iter = 0; iter < ls.size(); iter++) {
                sold += ls.get(iter).getQuantity();
            }
        }
        return new InventoryValueDto(units, cost, retail, low, sold, bp(sold, (long) sold + units));
    }

    static List<FunnelStepDto> funnel(Data d, List<StoreOrder> period, long from, long to) {
        int cartCount = 0;
        int checkout = 0;
        for (int iter = 0; iter < d.carts.size(); iter++) {
            Cart c = d.carts.get(iter);
            long at = c.getCreatedAt().toEpochMilli();
            if (at < from || at >= to) {
                continue;
            }
            cartCount++;
            if (c.getEmail().length() > 0 || c.getUsername().length() > 0) {
                checkout++;
            }
        }
        int online = 0;
        int fulfilled = 0;
        for (int iter = 0; iter < period.size(); iter++) {
            StoreOrder o = period.get(iter);
            if (Orders.ONLINE.equals(o.getChannel())) {
                online++;
                if (Orders.FULFILLED.equals(o.getFulfilment())) {
                    fulfilled++;
                }
            }
        }
        List<FunnelStepDto> out = new ArrayList<FunnelStepDto>();
        out.add(new FunnelStepDto("carts", "Carts", cartCount));
        out.add(new FunnelStepDto("checkouts", "Reached checkout", checkout));
        out.add(new FunnelStepDto("orders", "Ordered", online));
        out.add(new FunnelStepDto("fulfilled", "Fulfilled", fulfilled));
        return out;
    }
}
