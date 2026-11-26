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
package com.codenameone.examples.bazaar.subscribe;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.RenewalDto;
import com.codenameone.examples.bazaar.api.SellingPlanDto;
import com.codenameone.examples.bazaar.api.SubscribeDto;
import com.codenameone.examples.bazaar.api.SubscriptionDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.SellingPlan;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Subscription;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.marketing.Shoppers;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.SellingPlanRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.SubscriptionRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.trade.LineOrders;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Subscriptions: a product delivered every so many days, weeks or months
/// on a selling plan, at the plan's discount.
///
/// Subscribing orders and charges the first delivery at once, through the
/// same path every renewal takes ([#charge]): an order priced by
/// [LineOrders], the card held with [Payments#authorize] and captured with
/// [Orders#capturePayment]. A refused card erases the order (or cancels it,
/// once a hold was taken) so nothing ships unpaid.
///
/// **Dunning.** A renewal that fails is retried a day later, then three days
/// after that; the third failure pauses the subscription. Resuming it starts
/// a fresh schedule from that moment, so a long pause is not billed in a
/// burst of catch-up renewals.
///
/// **Dates.** Renewal `n` is `anchor + n` periods, worked out from the
/// anchor every time: a monthly plan started on the 31st renews on the 30th
/// in April and on the 31st again in May, instead of drifting to the 28th
/// for good after February.
@Component
public class Subscriptions {
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    public static final String CANCELLED = "CANCELLED";
    public static final String DAY = "DAY";
    public static final String WEEK = "WEEK";
    public static final String MONTH = "MONTH";
    /// The order channel renewals are placed on.
    public static final String CHANNEL = "SUBSCRIPTION";
    static final long DAY_MILLIS = 24L * 3600 * 1000;
    /// Failed renewals before a subscription pauses.
    static final int MAX_FAILURES = 3;
    /// How long a claimed renewal is held off from another job run.
    static final long LEASE_MILLIS = 3600L * 1000;
    /// The longest stretch the renewal calendar answers in one go.
    static final long MAX_CALENDAR_MILLIS = 92 * DAY_MILLIS;
    private static final String ACTOR = "subscriptions";

    private final SellingPlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final StoreOrderRepository orderRows;
    private final StoreAccess access;
    private final Shoppers shoppers;
    private final Payments payments;
    private final Orders orders;
    private final LineOrders lineOrders;

    public Subscriptions(SellingPlanRepository plans, SubscriptionRepository subscriptions,
            ProductRepository products, VariantRepository variants,
            StoreOrderRepository orderRows, StoreAccess access, Shoppers shoppers,
            Payments payments, Orders orders, LineOrders lineOrders) {
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.products = products;
        this.variants = variants;
        this.orderRows = orderRows;
        this.access = access;
        this.shoppers = shoppers;
        this.payments = payments;
        this.orders = orders;
        this.lineOrders = lineOrders;
    }

    // ---------------------------------------------------------------- plans

    @Transactional(readOnly = true)
    public List<SellingPlanDto> plans(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<SellingPlan> rows = plans.findByStoreIdOrderByCreatedAtAsc(storeId);
        List<SellingPlanDto> out = new ArrayList<SellingPlanDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    /// The active plans a shopper can subscribe to a product on.
    @Transactional(readOnly = true)
    public List<SellingPlanDto> productPlans(String storeId, String productId) {
        shoppers.store(storeId);
        List<SellingPlan> rows = productId == null ? new ArrayList<SellingPlan>()
                : plans.findByProductId(productId);
        List<SellingPlanDto> out = new ArrayList<SellingPlanDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            SellingPlan p = rows.get(iter);
            if (p.isActive() && storeId.equals(p.getStoreId())) {
                out.add(dto(p));
            }
        }
        return out;
    }

    static SellingPlanDto dto(SellingPlan p) {
        return new SellingPlanDto(p.getId(), p.getProductId(), p.getName(), p.getUnit(),
                p.getCadence(), p.getDiscountBps(), p.isActive());
    }

    @Transactional(rollbackFor = IOException.class)
    public SellingPlanDto savePlan(String username, String storeId, String planId,
            SellingPlanDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return writePlan(storeId, planId, in);
    }

    /// Creates the plan (`planId` null) or updates it. A change reaches new
    /// subscriptions only: each subscription keeps the terms it was sold on.
    public SellingPlanDto writePlan(String storeId, String planId, SellingPlanDto in)
            throws IOException {
        if (in == null || Codes.cut(in.name(), 80).length() == 0) {
            throw new ResponseStatusException(400, "Name the plan");
        }
        String unit = unit(in.unit());
        if (in.cadence() < 1 || in.cadence() > 365 || in.discountBps() < 0
                || in.discountBps() > 9000) {
            throw new ResponseStatusException(400, "Those plan numbers are out of range");
        }
        SellingPlan p = planId == null ? null : plan(storeId, planId);
        boolean fresh = p == null;
        if (fresh) {
            Product product = in.productId() == null ? null : products.findById(in.productId());
            if (product == null || !storeId.equals(product.getStoreId())) {
                throw new ResponseStatusException(404, "No such product");
            }
            p = new SellingPlan();
            p.setId(Ids.next());
            p.setStoreId(storeId);
            p.setProductId(product.getId());
            p.setCreatedAt(Instant.now());
        }
        p.setName(Codes.cut(in.name(), 80));
        p.setUnit(unit);
        p.setCadence(in.cadence());
        p.setDiscountBps(in.discountBps());
        p.setActive(in.active());
        if (fresh) {
            plans.insert(p);
        } else {
            plans.save(p);
        }
        return dto(p);
    }

    private static String unit(String typed) {
        String u = Codes.normalize(typed);
        if (u.endsWith("S")) {
            u = u.substring(0, u.length() - 1);
        }
        if (DAY.equals(u) || WEEK.equals(u) || MONTH.equals(u)) {
            return u;
        }
        throw new ResponseStatusException(400, "Renew by DAY, WEEK or MONTH");
    }

    SellingPlan plan(String storeId, String planId) {
        SellingPlan p = planId == null ? null : plans.findById(planId);
        if (p == null || !storeId.equals(p.getStoreId())) {
            throw new ResponseStatusException(404, "No such plan");
        }
        return p;
    }

    // ------------------------------------------------------------- merchant

    @Transactional(readOnly = true)
    public List<SubscriptionDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<Subscription> rows = subscriptions.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<SubscriptionDto> out = new ArrayList<SubscriptionDto>();
        for (int iter = 0; iter < rows.size() && iter < 500; iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public SubscriptionDto detail(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(inStore(storeId, id));
    }

    /// Changes a subscription for the store's managers: `action` is one of
    /// pause, resume, skip and cancel.
    public SubscriptionDto change(String username, String storeId, String id, String action)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return apply(inStore(storeId, id).getId(), action, Instant.now());
    }

    Subscription inStore(String storeId, String id) {
        Subscription s = id == null ? null : subscriptions.findById(id);
        if (s == null || !storeId.equals(s.getStoreId())) {
            throw new ResponseStatusException(404, "No such subscription");
        }
        return s;
    }

    /// Renewals due from `from` up to `to` (epoch millis), every cycle of
    /// every active subscription that falls in the range, soonest first.
    @Transactional(readOnly = true)
    public List<RenewalDto> calendar(String username, String storeId, String from, String to)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        long start = millis(from);
        long end = millis(to);
        if (end <= start || end - start > MAX_CALENDAR_MILLIS) {
            throw new ResponseStatusException(400, "Choose a range of up to 92 days");
        }
        List<Subscription> rows = subscriptions.due(storeId, ACTIVE, Instant.EPOCH,
                Instant.ofEpochMilli(end));
        List<RenewalDto> out = new ArrayList<RenewalDto>();
        for (int iter = 0; iter < rows.size() && out.size() < 1000; iter++) {
            Subscription s = rows.get(iter);
            Variant v = variants.findById(s.getVariantId());
            long amount = v == null ? 0 : amount(v.getPriceCents(), s.getDiscountBps())
                    * s.getQuantity();
            for (int k = 0; k < 400 && out.size() < 1000; k++) {
                long at = k == 0 ? s.getDueAt().toEpochMilli()
                        : advance(s.getAnchorAt().toEpochMilli(), s.getCycles() + k, s.getUnit(),
                            s.getCadence());
                if (at >= end) {
                    break;
                }
                if (at >= start) {
                    out.add(new RenewalDto(s.getId(), s.getUsername(), s.getVariantId(),
                            s.getQuantity(), at, amount));
                }
            }
        }
        sortByDue(out);
        return out;
    }

    private static long millis(String text) {
        if (text == null || text.trim().length() == 0) {
            throw new ResponseStatusException(400, "Choose a range of up to 92 days");
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException notANumber) {
            throw new ResponseStatusException(400, "Dates are epoch milliseconds");
        }
    }

    private static void sortByDue(List<RenewalDto> rows) {
        // Insertion sort: the calendar is short, and this keeps equal dates
        // in the order the subscriptions were read.
        for (int iter = 1; iter < rows.size(); iter++) {
            RenewalDto r = rows.get(iter);
            int at = iter - 1;
            while (at >= 0 && rows.get(at).dueAt() > r.dueAt()) {
                rows.set(at + 1, rows.get(at));
                at--;
            }
            rows.set(at + 1, r);
        }
    }

    // -------------------------------------------------------------- shopper

    @Transactional(readOnly = true)
    public List<SubscriptionDto> mine(String username, String storeId) {
        shoppers.store(storeId);
        if (username == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        List<Subscription> rows = subscriptions.findByStoreIdAndUsername(storeId, username);
        List<SubscriptionDto> out = new ArrayList<SubscriptionDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    /// The shopper's own pause, resume, skip or cancel; 404 for anybody
    /// else's subscription.
    public SubscriptionDto changeMine(String username, String storeId, String id, String action)
            throws IOException {
        shoppers.store(storeId);
        if (username == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        Subscription s = inStore(storeId, id);
        if (!username.equals(s.getUsername())) {
            throw new ResponseStatusException(404, "No such subscription");
        }
        return apply(s.getId(), action, Instant.now());
    }

    /// Subscribes `username` to a variant of the plan's product: the first
    /// delivery is ordered and charged now, and the subscription is kept
    /// only once that went through.
    public SubscriptionDto subscribe(String username, String storeId, SubscribeDto in)
            throws IOException {
        shoppers.store(storeId);
        if (username == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        if (in == null) {
            throw new ResponseStatusException(400, "Choose a plan");
        }
        SellingPlan p = plan(storeId, in.planId());
        if (!p.isActive()) {
            throw new ResponseStatusException(409, "That plan is not offered any more");
        }
        Variant v = in.variantId() == null ? null : variants.findById(in.variantId());
        if (v == null || !p.getProductId().equals(v.getProductId())) {
            throw new ResponseStatusException(404, "No such item");
        }
        if (in.quantity() < 1 || in.quantity() > 100) {
            throw new ResponseStatusException(400, "Choose from 1 to 100");
        }
        String method = payments.choose(username, in.paymentMethodId())[0];
        if (Payments.CASH.equals(method)) {
            throw new ResponseStatusException(400, "A subscription needs a card");
        }
        Customer customer = shoppers.customer(storeId, username);
        Instant now = Instant.now();
        Subscription s = new Subscription();
        s.setId(Ids.next());
        s.setStoreId(storeId);
        s.setPlanId(p.getId());
        s.setProductId(p.getProductId());
        s.setVariantId(v.getId());
        s.setQuantity(in.quantity());
        s.setDiscountBps(p.getDiscountBps());
        s.setUnit(p.getUnit());
        s.setCadence(p.getCadence());
        s.setCustomerId(customer.getId());
        s.setUsername(username);
        s.setEmail(Codes.cut(customer.getEmail().length() > 0 ? customer.getEmail() : username,
                190));
        s.setPaymentMethodId(method);
        s.setStatus(ACTIVE);
        s.setAnchorAt(now);
        s.setDueAt(now);
        s.setNextAttemptAt(now);
        AddressDto a = in.address();
        if (a != null) {
            s.setShipName(Codes.cut(a.name(), 120));
            s.setShipLine1(Codes.cut(a.line1(), 200));
            s.setShipCity(Codes.cut(a.city(), 120));
            s.setShipRegion(Codes.cut(a.region(), 80));
            s.setShipPostal(Codes.cut(a.postal(), 20));
            s.setShipCountry(Codes.cut(Codes.normalize(a.country()), 2));
        }
        String orderId = charge(s, Ids.next());
        s.setCycles(1);
        s.setDueAt(Instant.ofEpochMilli(advance(now.toEpochMilli(), 1, s.getUnit(),
                s.getCadence())));
        s.setNextAttemptAt(s.getDueAt());
        s.setLastOrderId(orderId);
        insert(s);
        return dto(s);
    }

    @Transactional(rollbackFor = IOException.class)
    void insert(Subscription s) {
        subscriptions.insert(s);
    }

    // ---------------------------------------------------------- transitions

    @Transactional(rollbackFor = IOException.class)
    SubscriptionDto apply(String id, String action, Instant now) {
        Subscription s = subscriptions.findById(id);
        String a = Codes.normalize(action);
        String st = s.getStatus();
        if ("PAUSE".equals(a)) {
            if (!ACTIVE.equals(st)) {
                throw new ResponseStatusException(409, "Only an active subscription can pause");
            }
            s.setStatus(PAUSED);
        } else if ("RESUME".equals(a)) {
            if (!PAUSED.equals(st)) {
                throw new ResponseStatusException(409, "Only a paused subscription can resume");
            }
            s.setStatus(ACTIVE);
            s.setFailures(0);
            s.setLastError("");
            if (s.getDueAt().isBefore(now)) {
                // A fresh schedule from today rather than a burst of the
                // renewals missed while paused.
                s.setAnchorAt(now);
                s.setCycles(0);
                s.setDueAt(now);
            }
            s.setNextAttemptAt(s.getDueAt());
        } else if ("SKIP".equals(a)) {
            if (CANCELLED.equals(st)) {
                throw new ResponseStatusException(409, "The subscription was cancelled");
            }
            s.setCycles(s.getCycles() + 1);
            s.setDueAt(Instant.ofEpochMilli(advance(s.getAnchorAt().toEpochMilli(),
                    s.getCycles(), s.getUnit(), s.getCadence())));
            s.setNextAttemptAt(s.getDueAt());
            s.setFailures(0);
            s.setLastError("");
        } else if ("CANCEL".equals(a)) {
            if (CANCELLED.equals(st)) {
                throw new ResponseStatusException(409, "The subscription was cancelled");
            }
            s.setStatus(CANCELLED);
        } else {
            throw new ResponseStatusException(400, "Pause, resume, skip or cancel");
        }
        subscriptions.save(s);
        return dto(s);
    }

    // ----------------------------------------------------------------- jobs

    @Scheduled(fixedDelay = 900000, initialDelay = 150000, lock = "bazaar-subscription-renewals")
    public void renewJob() {
        try {
            tick(Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("subscription renewals: " + failed);
        }
    }

    /// Renews every active subscription whose attempt time has come.
    /// Answers how many renewals were paid.
    public int tick(Instant now) throws IOException {
        int renewed = 0;
        List<Subscription> due = subscriptions.findByStatusAndNextAttemptAtLessThanEqual(ACTIVE,
                now);
        for (int iter = 0; iter < due.size(); iter++) {
            if (renew(due.get(iter).getId(), now)) {
                renewed++;
            }
        }
        return renewed;
    }

    boolean renew(String id, Instant now) throws IOException {
        Subscription s = subscriptions.findById(id);
        if (s == null || !ACTIVE.equals(s.getStatus()) || s.getNextAttemptAt().isAfter(now)) {
            return false;
        }
        Instant at = s.getNextAttemptAt();
        if (subscriptions.claim(s.getId(), at, now.plusMillis(LEASE_MILLIS), ACTIVE) != 1) {
            return false;
        }
        // One client id per cycle and attempt: the order a run placed is
        // never placed twice, and a refused card is tried afresh next time.
        String clientId = Ids.sha256("sub:" + s.getId() + ":" + s.getCycles() + ":"
                + s.getFailures()).substring(0, 32);
        String orderId;
        try {
            orderId = charge(s, clientId);
        } catch (ResponseStatusException refused) {
            settle(s.getId(), null, refused.getMessage(), now);
            return false;
        }
        settle(s.getId(), orderId, "", now);
        return true;
    }

    @Transactional(rollbackFor = IOException.class)
    void settle(String id, String orderId, String error, Instant now) {
        Subscription s = subscriptions.findById(id);
        if (s == null) {
            return;
        }
        if (orderId != null) {
            s.setCycles(s.getCycles() + 1);
            s.setDueAt(Instant.ofEpochMilli(advance(s.getAnchorAt().toEpochMilli(),
                    s.getCycles(), s.getUnit(), s.getCadence())));
            s.setNextAttemptAt(s.getDueAt());
            s.setFailures(0);
            s.setLastOrderId(orderId);
            s.setLastError("");
        } else {
            s.setFailures(s.getFailures() + 1);
            s.setLastError(Codes.cut(error == null ? "The card was refused" : error, 200));
            if (s.getFailures() >= MAX_FAILURES) {
                s.setStatus(PAUSED);
            }
            s.setNextAttemptAt(now.plusMillis(retryDelay(s.getFailures())));
        }
        subscriptions.save(s);
    }

    /// How long after failure `failures` the renewal is tried again.
    static long retryDelay(int failures) {
        return failures <= 1 ? DAY_MILLIS : 3 * DAY_MILLIS;
    }

    /// Orders and charges one delivery of `s`. Answers the order id, or
    /// throws: 402 for a refused card, 409 when it is out of stock.
    String charge(Subscription s, String clientId) throws IOException {
        Variant v = variants.findById(s.getVariantId());
        if (v == null) {
            throw new ResponseStatusException(409, "The item is no longer sold");
        }
        Store store = shoppers.store(s.getStoreId());
        long unit = amount(v.getPriceCents(), s.getDiscountBps());
        List<LineOrders.Item> items = new ArrayList<LineOrders.Item>();
        items.add(new LineOrders.Item(v.getId(), s.getQuantity(), v.getPriceCents(), unit));
        LineOrders.Buyer b = new LineOrders.Buyer();
        b.username = s.getUsername();
        b.email = s.getEmail();
        b.customerId = s.getCustomerId();
        b.shipName = s.getShipName();
        b.shipLine1 = s.getShipLine1();
        b.shipCity = s.getShipCity();
        b.shipRegion = s.getShipRegion();
        b.shipPostal = s.getShipPostal();
        b.shipCountry = s.getShipCountry();
        StoreOrder order = lineOrders.place(s.getStoreId(), CHANNEL, b, items, clientId, ACTOR,
                "Subscription renewal");
        String orderId = order.getId();
        long total = order.getTotalCents();
        if (total <= 0) {
            return orderId;
        }
        String reference;
        try {
            reference = payments.authorize(orderId, s.getUsername(), s.getPaymentMethodId(), total,
                    store.getCurrency());
        } catch (IOException | RuntimeException failed) {
            lineOrders.undo(orderId, ACTOR);
            throw failed;
        }
        lineOrders.authorized(orderId, total, reference, ACTOR);
        String financial = orders.capturePayment(orderId, ACTOR);
        if (!Orders.PAID.equals(financial)) {
            orders.cancelOrder(orderRows.findById(orderId), ACTOR);
            throw new ResponseStatusException(402, "The card was refused");
        }
        return orderId;
    }

    /// The price of one unit at `bps` off, rounded down to the cent.
    static long amount(long priceCents, int bps) {
        return priceCents - priceCents * bps / 10000;
    }

    /// Renewal `n` of a schedule anchored at `anchor`: `n * cadence` days,
    /// weeks or calendar months later, in UTC, with the day of the month
    /// clamped to the month's length.
    public static long advance(long anchor, int n, String unit, int cadence) {
        long steps = (long) n * cadence;
        if (DAY.equals(unit)) {
            return anchor + steps * DAY_MILLIS;
        }
        if (WEEK.equals(unit)) {
            return anchor + steps * 7 * DAY_MILLIS;
        }
        long day = floorDiv(anchor, DAY_MILLIS);
        long timeOfDay = anchor - day * DAY_MILLIS;
        long[] ymd = civil(day);
        long months = ymd[0] * 12 + (ymd[1] - 1) + steps;
        long year = floorDiv(months, 12);
        int month = (int) (months - year * 12) + 1;
        long d = Math.min(ymd[2], monthLength(year, month));
        return days(year, month, d) * DAY_MILLIS + timeOfDay;
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        return (a % b != 0 && (a < 0) != (b < 0)) ? q - 1 : q;
    }

    static int monthLength(long year, int month) {
        if (month == 2) {
            boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
            return leap ? 29 : 28;
        }
        return month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31;
    }

    /// Days since 1970-01-01 of a civil date (proleptic Gregorian).
    static long days(long y, int m, long d) {
        long yy = m <= 2 ? y - 1 : y;
        long era = floorDiv(yy, 400);
        long yoe = yy - era * 400;
        long mp = (m + 9) % 12;
        long doy = (153 * mp + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /// The civil date `{year, month, day}` of a day number.
    static long[] civil(long z) {
        long zz = z + 719468;
        long era = floorDiv(zz, 146097);
        long doe = zz - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        long y = yoe + era * 400 + (m <= 2 ? 1 : 0);
        return new long[] {y, m, d};
    }

    static SubscriptionDto dto(Subscription s) {
        return new SubscriptionDto(s.getId(), s.getPlanId(), s.getProductId(), s.getVariantId(),
                s.getQuantity(), s.getDiscountBps(), s.getUnit(), s.getCadence(), s.getUsername(),
                s.getStatus(), s.getDueAt().toEpochMilli(), s.getFailures(), s.getLastOrderId(),
                s.getLastError());
    }
}
