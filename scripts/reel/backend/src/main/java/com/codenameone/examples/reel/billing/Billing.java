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
package com.codenameone.examples.reel.billing;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.api.CouponCheckDto;
import com.codenameone.examples.reel.api.CouponDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.InvoiceDto;
import com.codenameone.examples.reel.api.PlanDto;
import com.codenameone.examples.reel.api.PurchaseReceiptDto;
import com.codenameone.examples.reel.api.RentDto;
import com.codenameone.examples.reel.api.RentalDto;
import com.codenameone.examples.reel.api.SubscribeDto;
import com.codenameone.examples.reel.api.SubscriptionDto;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.domain.Coupon;
import com.codenameone.examples.reel.domain.CouponRedemption;
import com.codenameone.examples.reel.domain.Invoice;
import com.codenameone.examples.reel.domain.Plan;
import com.codenameone.examples.reel.domain.Rental;
import com.codenameone.examples.reel.domain.Subscription;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.pay.PaymentDeclinedException;
import com.codenameone.examples.reel.pay.Payments;
import com.codenameone.examples.reel.repository.CouponRedemptionRepository;
import com.codenameone.examples.reel.repository.CouponRepository;
import com.codenameone.examples.reel.repository.InvoiceRepository;
import com.codenameone.examples.reel.repository.PlanRepository;
import com.codenameone.examples.reel.repository.RentalRepository;
import com.codenameone.examples.reel.repository.SubscriptionRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Subscriptions, coupons, rentals and the invoices they leave.
///
/// A subscription is one row per account. Subscribing charges the first
/// period at once -- or starts the plan's trial, once per account -- and the
/// renewal sweep charges each period after that. A charge that fails puts the
/// subscription PAST_DUE; [Entitlements] keeps it playing for a grace period
/// while the sweep retries, and it is canceled after [#MAX_ATTEMPTS] failures.
/// Changing plan takes effect at the end of the period already paid for,
/// so nobody is charged twice in one period and there is no proration to get
/// wrong. Canceling also runs to the end of the period.
///
/// Every charge carries an idempotency key derived from what it pays for
/// (the account and the period, or the rental), so a retried request or two
/// servers running the sweep at once charge a card once.
@Component
public class Billing {
    public static final String MONTH = "MONTH";
    public static final String YEAR = "YEAR";
    public static final int MAX_ATTEMPTS = 3;
    /// How long after a declined renewal the card is tried again. Three
    /// tries a day apart fit inside [Entitlements#GRACE_MILLIS].
    public static final long RETRY_MILLIS = 86400000L;
    public static final long DAY = 86400000L;
    /// How long a rental plays once paid for.
    public static final long RENTAL_MILLIS = 2L * DAY;
    public static final String PAID = "PAID";
    public static final String FAILED = "FAILED";
    public static final String KIND_SUBSCRIPTION = "SUBSCRIPTION";
    public static final String KIND_RENTAL = "RENTAL";
    public static final String KIND_STORE = "STORE";

    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final InvoiceRepository invoices;
    private final RentalRepository rentals;
    private final TitleRepository titles;
    private final Payments payments;
    private final Settings settings;
    private final boolean trustReceipts;

    public Billing(SubscriptionRepository subscriptions, PlanRepository plans, CouponRepository coupons,
            CouponRedemptionRepository redemptions,
            InvoiceRepository invoices, RentalRepository rentals, TitleRepository titles, Payments payments,
            Settings settings, @Value("${reel.billing.trust-store-receipts:false}") String trustReceipts) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.invoices = invoices;
        this.rentals = rentals;
        this.titles = titles;
        this.payments = payments;
        this.settings = settings;
        this.trustReceipts = "true".equalsIgnoreCase(trustReceipts.trim());
    }

    // ------------------------------------------------------------ plans

    @Transactional(readOnly = true)
    public List<PlanDto> plans(boolean includeRetired) {
        List<Plan> all = sortedPlans();
        List<PlanDto> out = new ArrayList<PlanDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            if (includeRetired || all.get(iter).isActive()) {
                out.add(plan(all.get(iter)));
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public PlanDto savePlan(String code, PlanDto wanted) throws IOException {
        if (wanted == null || !code(code)) {
            throw new ResponseStatusException(400, "A plan code is 2-32 letters, digits or dashes");
        }
        Plan row = plans.findById(code);
        boolean fresh = row == null;
        if (fresh) {
            row = new Plan();
            row.setCode(code);
            row.setPosition(sortedPlans().size());
        }
        String name = wanted.name() == null ? "" : wanted.name().trim();
        if (name.length() == 0 || name.length() > 60) {
            throw new ResponseStatusException(400, "Name the plan (at most 60 characters)");
        }
        row.setName(name);
        row.setDescription(cut(wanted.description(), 255));
        row.setPriceCents(Math.max(0L, wanted.priceCents()));
        row.setCurrency(settings.currency());
        row.setBillingInterval(YEAR.equals(wanted.interval()) ? YEAR : MONTH);
        row.setMaxStreams(Math.max(1, Math.min(10, wanted.maxStreams())));
        row.setMaxQuality(String.valueOf(Entitlements.maxHeight(wanted.maxQuality())));
        row.setAdSupported(wanted.adSupported());
        row.setDownloads(wanted.downloads());
        row.setTrialDays(Math.max(0, Math.min(90, wanted.trialDays())));
        row.setActive(wanted.active());
        if (fresh) {
            plans.insert(row);
        } else {
            plans.save(row);
        }
        return plan(row);
    }

    // ------------------------------------------------------ subscriptions

    @Transactional(readOnly = true)
    public SubscriptionDto subscription(String username) {
        return dto(subscriptions.findById(username));
    }

    @Transactional(rollbackFor = IOException.class)
    public SubscriptionDto subscribe(String username, SubscribeDto wanted) throws IOException {
        settings.require(Features.SUBSCRIPTIONS);
        if (wanted == null || wanted.planCode() == null) {
            throw new ResponseStatusException(400, "Choose a plan");
        }
        Plan plan = plans.findById(wanted.planCode());
        if (plan == null || !plan.isActive()) {
            throw new ResponseStatusException(404, "No such plan");
        }
        long now = System.currentTimeMillis();
        Subscription sub = subscriptions.findById(username);
        if (sub != null && Entitlements.isLive(sub, now) && sub.getStore().length() == 0) {
            // Already paying: a change of plan waits for the period's end.
            if (sub.getPlanCode().equals(plan.getCode())) {
                sub.setPendingPlanCode("");
            } else {
                sub.setPendingPlanCode(plan.getCode());
            }
            sub.setCancelAtPeriodEnd(false);
            sub.setUpdatedAt(now);
            subscriptions.save(sub);
            return dto(sub);
        }
        Coupon coupon = null;
        if (wanted.coupon() != null && wanted.coupon().trim().length() > 0) {
            coupon = coupons.findById(normalize(wanted.coupon()));
            String why = couponProblem(coupon, now);
            if (why != null) {
                throw new ResponseStatusException(400, why);
            }
            if (redemptions.findById(CouponRedemption.key(coupon.getCode(), username)) != null) {
                throw new ResponseStatusException(400, "You have already used this coupon");
            }
        }
        String method = payments.choose(username, wanted.paymentMethodId())[0];
        if (Payments.CASH.equals(method) && plan.getPriceCents() > 0) {
            throw new ResponseStatusException(400, "Add a card to subscribe");
        }
        boolean fresh = sub == null;
        if (fresh) {
            sub = new Subscription();
            sub.setUsername(username);
        }
        boolean trial = plan.getTrialDays() > 0 && (fresh || sub.getStartedAt() == 0);
        sub.setPlanCode(plan.getCode());
        sub.setPendingPlanCode("");
        sub.setCancelAtPeriodEnd(false);
        sub.setPaymentMethodId(method);
        sub.setFailedAttempts(0);
        sub.setStore("");
        sub.setStoreRef("");
        sub.setStartedAt(now);
        sub.setUpdatedAt(now);
        sub.setCouponCode(coupon == null ? "" : coupon.getCode());
        sub.setCouponPeriodsLeft(coupon == null ? 0 : Math.max(1, coupon.getPeriods()));
        if (coupon != null) {
            coupon.setRedemptions(coupon.getRedemptions() + 1);
            coupons.save(coupon);
            CouponRedemption used = new CouponRedemption();
            used.setId(CouponRedemption.key(coupon.getCode(), username));
            used.setRedeemedAt(now);
            redemptions.insert(used);
        }
        if (trial) {
            sub.setStatus(Entitlements.TRIALING);
            sub.setPeriodEndsAt(now + plan.getTrialDays() * DAY);
        } else {
            long amount = price(plan, sub);
            charge(username, method, amount, plan, "start:" + now, now);
            sub.setStatus(Entitlements.ACTIVE);
            sub.setPeriodEndsAt(periodEnd(plan, now));
            usedCouponPeriod(sub);
        }
        if (fresh) {
            subscriptions.insert(sub);
        } else {
            subscriptions.save(sub);
        }
        return dto(sub);
    }

    @Transactional(rollbackFor = IOException.class)
    public SubscriptionDto cancel(String username) throws IOException {
        Subscription sub = subscriptions.findById(username);
        if (sub == null || !Entitlements.isLive(sub, System.currentTimeMillis())) {
            throw new ResponseStatusException(404, "No subscription to cancel");
        }
        sub.setCancelAtPeriodEnd(true);
        sub.setPendingPlanCode("");
        sub.setUpdatedAt(System.currentTimeMillis());
        subscriptions.save(sub);
        return dto(sub);
    }

    @Transactional(rollbackFor = IOException.class)
    public SubscriptionDto resume(String username) throws IOException {
        Subscription sub = subscriptions.findById(username);
        if (sub == null || !Entitlements.isLive(sub, System.currentTimeMillis())) {
            throw new ResponseStatusException(404, "No subscription to resume; subscribe again");
        }
        sub.setCancelAtPeriodEnd(false);
        if (Entitlements.CANCELED.equals(sub.getStatus())) {
            sub.setStatus(Entitlements.ACTIVE);
        }
        sub.setUpdatedAt(System.currentTimeMillis());
        subscriptions.save(sub);
        return dto(sub);
    }

    @Transactional(readOnly = true)
    public CouponCheckDto checkCoupon(String code, String planCode) {
        Plan plan = planCode == null ? null : plans.findById(planCode);
        Coupon coupon = code == null ? null : coupons.findById(normalize(code));
        String why = couponProblem(coupon, System.currentTimeMillis());
        long base = plan == null ? 0L : plan.getPriceCents();
        if (why != null) {
            return new CouponCheckDto(code, false, why, base, 0);
        }
        return new CouponCheckDto(coupon.getCode(), true, "", base * (100 - coupon.getPercentOff()) / 100,
                coupon.getPercentOff());
    }

    /// An App Store or Google Play purchase. A production server must verify
    /// the receipt with the store (the App Store Server API, the Play
    /// Developer API) before trusting it; that needs store credentials this
    /// sample cannot ship, so receipts are refused unless
    /// `reel.billing.trust-store-receipts` is on, which only the development
    /// and test profiles set.
    @Transactional(rollbackFor = IOException.class)
    public SubscriptionDto storePurchase(String username, PurchaseReceiptDto receipt) throws IOException {
        settings.require(Features.SUBSCRIPTIONS);
        if (!trustReceipts) {
            throw new ResponseStatusException(501, "Store purchases are not verified on this server");
        }
        if (receipt == null || receipt.productId() == null || receipt.transactionId() == null) {
            throw new ResponseStatusException(400, "Not a receipt");
        }
        Plan plan = plans.findById(receipt.productId());
        if (plan == null) {
            throw new ResponseStatusException(404, "No plan matches " + receipt.productId());
        }
        long now = System.currentTimeMillis();
        Subscription sub = subscriptions.findById(username);
        boolean fresh = sub == null;
        if (fresh) {
            sub = new Subscription();
            sub.setUsername(username);
        }
        sub.setPlanCode(plan.getCode());
        sub.setStatus(Entitlements.ACTIVE);
        sub.setStartedAt(now);
        sub.setPeriodEndsAt(periodEnd(plan, now));
        sub.setCancelAtPeriodEnd(false);
        sub.setPendingPlanCode("");
        sub.setStore(cut(receipt.store(), 16));
        sub.setStoreRef(cut(receipt.transactionId(), 128));
        sub.setUpdatedAt(now);
        if (fresh) {
            subscriptions.insert(sub);
        } else {
            subscriptions.save(sub);
        }
        invoice(username, KIND_STORE, plan.getName() + " (" + sub.getStore() + ")", plan.getPriceCents(),
                PAID, sub.getStoreRef(), "", now);
        return dto(sub);
    }

    /// Charges the subscriptions whose period has ended, and ends those that
    /// were canceled or ran out of retries.
    @Scheduled(fixedDelay = 300000, initialDelay = 30000, lock = "reel-renewals")
    @Transactional
    public void renew() {
        long now = System.currentTimeMillis();
        List<Subscription> due = subscriptions.findByPeriodEndsAtLessThan(now);
        for (int iter = 0; iter < due.size(); iter++) {
            renewOne(due.get(iter), now);
        }
    }

    void renewOne(Subscription sub, long now) {
        String status = sub.getStatus();
        if (Entitlements.CANCELED.equals(status) || Entitlements.NONE.equals(status)) {
            return;
        }
        if (Entitlements.PAST_DUE.equals(status) && now < sub.getNextRetryAt()
                && now <= sub.getPeriodEndsAt() + Entitlements.GRACE_MILLIS) {
            return;
        }
        if (sub.isCancelAtPeriodEnd() || sub.getStore().length() > 0) {
            // A store subscription renews in the store, which tells us with a
            // new receipt; without one the period simply ends.
            end(sub, now);
            return;
        }
        if (sub.getPendingPlanCode().length() > 0) {
            sub.setPlanCode(sub.getPendingPlanCode());
            sub.setPendingPlanCode("");
        }
        Plan plan = plans.findById(sub.getPlanCode());
        if (plan == null || !plan.isActive()) {
            end(sub, now);
            return;
        }
        // The period being paid for names the charge, so a retry of the same
        // period reuses the key and the card is charged once.
        String period = "renew:" + sub.getPeriodEndsAt();
        try {
            charge(sub.getUsername(), sub.getPaymentMethodId(), price(plan, sub), plan, period, now);
            sub.setStatus(Entitlements.ACTIVE);
            sub.setFailedAttempts(0);
            sub.setPeriodEndsAt(periodEnd(plan, sub.getPeriodEndsAt()));
            usedCouponPeriod(sub);
        } catch (IOException declined) {
            sub.setFailedAttempts(sub.getFailedAttempts() + 1);
            if (sub.getFailedAttempts() >= MAX_ATTEMPTS
                    || now > sub.getPeriodEndsAt() + Entitlements.GRACE_MILLIS) {
                end(sub, now);
                return;
            }
            sub.setStatus(Entitlements.PAST_DUE);
            sub.setNextRetryAt(now + RETRY_MILLIS);
        }
        sub.setUpdatedAt(now);
        subscriptions.save(sub);
    }

    private void end(Subscription sub, long now) {
        sub.setStatus(Entitlements.CANCELED);
        sub.setCancelAtPeriodEnd(false);
        sub.setUpdatedAt(now);
        subscriptions.save(sub);
    }

    // ------------------------------------------------------------ rentals

    @Transactional(rollbackFor = IOException.class)
    public RentalDto rent(String username, RentDto wanted) throws IOException {
        settings.require(Features.RENTALS);
        Title title = wanted == null || wanted.titleId() == null ? null : titles.findById(wanted.titleId());
        if (title == null || !Entitlements.RENTAL.equals(title.getAccess())
                || !Kinds.PUBLISHED.equals(title.getStatus())) {
            throw new ResponseStatusException(404, "This title is not for rent");
        }
        long now = System.currentTimeMillis();
        List<Rental> mine = rentals.findByUsernameAndTitleId(username, title.getId());
        for (int iter = 0; iter < mine.size(); iter++) {
            if (mine.get(iter).getExpiresAt() > now) {
                return rental(mine.get(iter), title);
            }
        }
        String method = payments.choose(username, wanted.paymentMethodId())[0];
        if (Payments.CASH.equals(method)) {
            throw new ResponseStatusException(400, "Add a card to rent");
        }
        Rental row = new Rental();
        row.setId(Ids.next());
        row.setUsername(username);
        row.setTitleId(title.getId());
        row.setPriceCents(title.getRentalPriceCents());
        row.setCurrency(settings.currency());
        row.setCreatedAt(now);
        row.setExpiresAt(now + RENTAL_MILLIS);
        String ref = payments.chargeCard("rent:" + row.getId(), username, method, row.getPriceCents(),
                row.getCurrency(), "Rental: " + title.getName());
        row.setInvoiceId(invoice(username, KIND_RENTAL, "Rental: " + title.getName(), row.getPriceCents(), PAID,
                ref, title.getId(), now));
        rentals.insert(row);
        return rental(row, title);
    }

    @Transactional(readOnly = true)
    public List<RentalDto> rentals(String username) {
        List<Rental> all = rentals.findByUsernameOrderByCreatedAtDesc(username);
        List<RentalDto> out = new ArrayList<RentalDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(rental(all.get(iter), titles.findById(all.get(iter).getTitleId())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> invoices(String username) {
        List<Invoice> all = invoices.findByUsernameOrderByCreatedAtDesc(username);
        List<InvoiceDto> out = new ArrayList<InvoiceDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Invoice i = all.get(iter);
            out.add(new InvoiceDto(i.getId(), i.getKind(), i.getDescription(), i.getAmountCents(), i.getCurrency(),
                    i.getStatus(), i.getCreatedAt(), i.getTitleId()));
        }
        return out;
    }

    // ------------------------------------------------------------ coupons

    @Transactional(readOnly = true)
    public List<CouponDto> coupons() {
        List<Coupon> all = coupons.findAll();
        List<CouponDto> out = new ArrayList<CouponDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(coupon(all.get(iter)));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public CouponDto saveCoupon(String code, CouponDto wanted) throws IOException {
        String key = normalize(code);
        if (wanted == null || !code(key)) {
            throw new ResponseStatusException(400, "A coupon code is 2-32 letters, digits or dashes");
        }
        Coupon row = coupons.findById(key);
        boolean fresh = row == null;
        if (fresh) {
            row = new Coupon();
            row.setCode(key);
            row.setCreatedAt(System.currentTimeMillis());
        }
        row.setPercentOff(Math.max(1, Math.min(100, wanted.percentOff())));
        row.setPeriods(Math.max(1, Math.min(36, wanted.periods())));
        row.setMaxRedemptions(Math.max(0, wanted.maxRedemptions()));
        row.setExpiresAt(Math.max(0L, wanted.expiresAt()));
        row.setActive(wanted.active());
        if (fresh) {
            coupons.insert(row);
        } else {
            coupons.save(row);
        }
        return coupon(row);
    }

    // ------------------------------------------------------------ helpers

    private void charge(String username, String method, long amount, Plan plan, String period, long now)
            throws IOException {
        String description = plan.getName() + " plan";
        if (amount <= 0) {
            invoice(username, KIND_SUBSCRIPTION, description, 0L, PAID, "", "", now);
            return;
        }
        try {
            String ref = payments.chargeCard("sub:" + username + ":" + period, username, method, amount,
                    plan.getCurrency().length() == 0 ? settings.currency() : plan.getCurrency(), description);
            invoice(username, KIND_SUBSCRIPTION, description, amount, PAID, ref, "", now);
        } catch (PaymentDeclinedException declined) {
            invoice(username, KIND_SUBSCRIPTION, description + " (declined)", amount, FAILED, "", "", now);
            throw declined;
        }
    }

    private String invoice(String username, String kind, String description, long amount, String status,
            String ref, String titleId, long now) throws IOException {
        Invoice row = new Invoice();
        row.setId(Ids.next());
        row.setUsername(username);
        row.setKind(kind);
        row.setDescription(cut(description, 255));
        row.setAmountCents(amount);
        row.setCurrency(settings.currency());
        row.setStatus(status);
        row.setProviderRef(ref == null ? "" : ref);
        row.setTitleId(titleId == null ? "" : titleId);
        row.setCreatedAt(now);
        invoices.insert(row);
        return row.getId();
    }

    private long price(Plan plan, Subscription sub) {
        if (sub.getCouponPeriodsLeft() > 0 && sub.getCouponCode().length() > 0) {
            Coupon c = coupons.findById(sub.getCouponCode());
            if (c != null) {
                return plan.getPriceCents() * (100 - c.getPercentOff()) / 100;
            }
        }
        return plan.getPriceCents();
    }

    private static void usedCouponPeriod(Subscription sub) {
        if (sub.getCouponPeriodsLeft() > 0) {
            sub.setCouponPeriodsLeft(sub.getCouponPeriodsLeft() - 1);
            if (sub.getCouponPeriodsLeft() == 0) {
                sub.setCouponCode("");
            }
        }
    }

    private static long periodEnd(Plan plan, long from) {
        return from + (YEAR.equals(plan.getBillingInterval()) ? 365L : 30L) * DAY;
    }

    private static String couponProblem(Coupon c, long now) {
        if (c == null || !c.isActive()) {
            return "That code is not valid";
        }
        if (c.getExpiresAt() > 0 && c.getExpiresAt() < now) {
            return "That code has expired";
        }
        if (c.getMaxRedemptions() > 0 && c.getRedemptions() >= c.getMaxRedemptions()) {
            return "That code has been used up";
        }
        return null;
    }

    private List<Plan> sortedPlans() {
        List<Plan> all = new ArrayList<Plan>(plans.findAll());
        // Insertion sort by position: a handful of plans.
        for (int i = 1; i < all.size(); i++) {
            Plan p = all.get(i);
            int j = i - 1;
            while (j >= 0 && all.get(j).getPosition() > p.getPosition()) {
                all.set(j + 1, all.get(j));
                j--;
            }
            all.set(j + 1, p);
        }
        return all;
    }

    private SubscriptionDto dto(Subscription sub) {
        if (sub == null) {
            return new SubscriptionDto("", "", Entitlements.NONE, 0L, 0L, false, 0L, settings.currency(), "", 0, "");
        }
        Plan plan = plans.findById(sub.getPlanCode());
        int percent = 0;
        if (sub.getCouponCode().length() > 0) {
            Coupon c = coupons.findById(sub.getCouponCode());
            percent = c == null ? 0 : c.getPercentOff();
        }
        return new SubscriptionDto(sub.getPlanCode(), plan == null ? "" : plan.getName(), sub.getStatus(),
                sub.getStartedAt(), sub.getPeriodEndsAt(), sub.isCancelAtPeriodEnd(),
                plan == null ? 0L : plan.getPriceCents(), settings.currency(), sub.getCouponCode(), percent,
                sub.getPendingPlanCode());
    }

    public static PlanDto plan(Plan p) {
        return new PlanDto(p.getCode(), p.getName(), p.getDescription(), p.getPriceCents(), p.getCurrency(),
                p.getBillingInterval(), p.getMaxStreams(), p.getMaxQuality(), p.isAdSupported(), p.isDownloads(),
                p.getTrialDays(), p.isActive());
    }

    static CouponDto coupon(Coupon c) {
        return new CouponDto(c.getCode(), c.getPercentOff(), c.getPeriods(), c.getMaxRedemptions(),
                c.getRedemptions(), c.getExpiresAt(), c.isActive());
    }

    private RentalDto rental(Rental r, Title t) {
        return new RentalDto(r.getId(), r.getTitleId(), t == null ? "" : t.getName(), r.getPriceCents(),
                r.getCurrency(), r.getExpiresAt(), r.getStartedAt() > 0);
    }

    /// Coupon and plan codes are compared in upper case, folded by hand
    /// because they are ASCII by construction (see [#code]).
    static String normalize(String code) {
        if (code == null) {
            return "";
        }
        String t = code.trim();
        StringBuilder out = new StringBuilder(t.length());
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            out.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return out.toString();
    }

    static boolean code(String code) {
        if (code == null || code.length() < 2 || code.length() > 32) {
            return false;
        }
        for (int iter = 0; iter < code.length(); iter++) {
            char c = code.charAt(iter);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '-') {
                return false;
            }
        }
        return true;
    }

    private static String cut(String value, int max) {
        String t = value == null ? "" : value.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
