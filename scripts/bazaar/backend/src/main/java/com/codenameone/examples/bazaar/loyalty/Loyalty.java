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
package com.codenameone.examples.bazaar.loyalty;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.LedgerEntryDto;
import com.codenameone.examples.bazaar.api.LoyaltyMemberDto;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyRewardDto;
import com.codenameone.examples.bazaar.api.LoyaltyTierDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.CustomerAddress;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.LoyaltyLedger;
import com.codenameone.examples.bazaar.domain.LoyaltyMember;
import com.codenameone.examples.bazaar.domain.LoyaltyProgram;
import com.codenameone.examples.bazaar.domain.LoyaltyReward;
import com.codenameone.examples.bazaar.domain.LoyaltyTier;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.marketing.Shoppers;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.repository.CustomerAddressRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyLedgerRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyMemberRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyProgramRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyRewardRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyTierRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The points programme: earning on paid orders (online, and at a register
/// when the programme says so), reversing on refunds and cancellations,
/// redeeming for single-use codes, tiers with member prices and free
/// shipping, member cards a register scans, referrals, and expiry.
///
/// Every movement of points is a [LoyaltyLedger] row whose `ref` is unique
/// per store (`EARN:<order>`, `REVERSE:<order>:<total>`, ...), so a paid
/// event told twice, or a refund job that runs again, moves nothing the
/// second time.
///
/// Member prices and free shipping are ordinary automatic discounts whose
/// `eligibility` names the tier: checkout applies them through the
/// [DiscountEngine] like any other, and [Memberships] says who qualifies.
@Component
public class Loyalty {
    public static final String EARN = "EARN";
    public static final String REVERSE = "REVERSE";
    public static final String REDEEM = "REDEEM";
    public static final String RESTORE = "RESTORE";
    public static final String EXPIRE = "EXPIRE";
    public static final String ADJUST = "ADJUST";
    public static final String REFERRAL = "REFERRAL";
    public static final String JOURNEY = "JOURNEY";
    public static final String SPEND = "SPEND";
    public static final String POINTS = "POINTS";
    public static final String ISSUED = "ISSUED";
    public static final String USED = "USED";
    public static final String EXPIRED = "EXPIRED";
    static final long YEAR = 365L * 24 * 3600 * 1000;
    static final long DAY = 24L * 3600 * 1000;
    /// How long a reward code stays usable.
    static final long REWARD_DAYS = 90;
    /// The most points one manual change may move.
    static final long MAX_ADJUST = 1000000L;

    private final LoyaltyProgramRepository programs;
    private final LoyaltyTierRepository tiers;
    private final LoyaltyMemberRepository members;
    private final LoyaltyLedgerRepository ledger;
    private final LoyaltyRewardRepository rewards;
    private final DiscountRepository discounts;
    private final StoreOrderRepository orders;
    private final CustomerRepository customers;
    private final CustomerAddressRepository addresses;
    private final StoreRepository stores;
    private final StoreAccess access;
    private final Shoppers shoppers;

    public Loyalty(LoyaltyProgramRepository programs, LoyaltyTierRepository tiers,
            LoyaltyMemberRepository members, LoyaltyLedgerRepository ledger,
            LoyaltyRewardRepository rewards, DiscountRepository discounts,
            StoreOrderRepository orders, CustomerRepository customers,
            CustomerAddressRepository addresses, StoreRepository stores, StoreAccess access,
            Shoppers shoppers) {
        this.programs = programs;
        this.tiers = tiers;
        this.members = members;
        this.ledger = ledger;
        this.rewards = rewards;
        this.discounts = discounts;
        this.orders = orders;
        this.customers = customers;
        this.addresses = addresses;
        this.stores = stores;
        this.access = access;
        this.shoppers = shoppers;
    }

    // ------------------------------------------------------------- programme

    @Transactional(readOnly = true)
    public LoyaltyProgramDto program(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return programDto(storeId);
    }

    LoyaltyProgramDto programDto(String storeId) {
        LoyaltyProgram p = programs.findByStoreId(storeId);
        if (p == null) {
            p = defaults(storeId);
        }
        List<LoyaltyTierDto> out = new ArrayList<LoyaltyTierDto>();
        List<LoyaltyTier> rows = tiers.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(tierDto(rows.get(iter)));
        }
        return new LoyaltyProgramDto(p.isActive(), p.getPointsPerUnit(), p.getUnitCents(),
                p.getRounding(), p.getPointsPerReward(), p.getRewardCents(), p.getMinRedeemPoints(),
                p.getExpiryDays(), p.isEarnOnPos(), p.getTierBasis(), p.getReferrerPoints(),
                p.getRefereePoints(), out);
    }

    static LoyaltyProgram defaults(String storeId) {
        LoyaltyProgram p = new LoyaltyProgram();
        p.setStoreId(storeId);
        p.setPointsPerUnit(1);
        p.setUnitCents(100);
        p.setRounding(LoyaltyMath.DOWN);
        p.setPointsPerReward(100);
        p.setRewardCents(500);
        p.setMinRedeemPoints(100);
        p.setEarnOnPos(true);
        p.setTierBasis(SPEND);
        return p;
    }

    static LoyaltyTierDto tierDto(LoyaltyTier t) {
        return new LoyaltyTierDto(t.getId(), t.getName(), t.getThreshold(), t.getMemberBps(),
                t.isFreeShipping(), t.getEarlyAccessHours(), t.getColour(), t.getPosition());
    }

    /// Saves the programme and its tiers, and keeps each tier's member-price
    /// and free-shipping discounts in step with it.
    @Transactional(rollbackFor = IOException.class)
    public LoyaltyProgramDto saveProgram(String username, String storeId, LoyaltyProgramDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return save(storeId, in);
    }

    public LoyaltyProgramDto save(String storeId, LoyaltyProgramDto in) throws IOException {
        if (in == null) {
            throw new ResponseStatusException(400, "Describe the programme");
        }
        if (in.pointsPerUnit() < 0 || in.pointsPerUnit() > 1000 || in.unitCents() <= 0
                || in.unitCents() > 1000000 || in.pointsPerReward() < 0 || in.rewardCents() < 0
                || in.minRedeemPoints() < 0 || in.expiryDays() < 0 || in.expiryDays() > 3650
                || in.referrerPoints() < 0 || in.refereePoints() < 0) {
            throw new ResponseStatusException(400, "Those programme numbers are out of range");
        }
        String rounding = LoyaltyMath.UP.equals(in.rounding()) || LoyaltyMath.NEAREST.equals(in.rounding())
                ? in.rounding() : LoyaltyMath.DOWN;
        LoyaltyProgram p = programs.findByStoreId(storeId);
        boolean fresh = p == null;
        if (fresh) {
            p = defaults(storeId);
            p.setId(Ids.next());
            p.setCreatedAt(Instant.now());
        }
        p.setActive(in.active());
        p.setPointsPerUnit(in.pointsPerUnit());
        p.setUnitCents(in.unitCents());
        p.setRounding(rounding);
        p.setPointsPerReward(in.pointsPerReward());
        p.setRewardCents(in.rewardCents());
        p.setMinRedeemPoints(in.minRedeemPoints());
        p.setExpiryDays(in.expiryDays());
        p.setEarnOnPos(in.earnOnPos());
        p.setTierBasis(POINTS.equals(in.tierBasis()) ? POINTS : SPEND);
        p.setReferrerPoints(in.referrerPoints());
        p.setRefereePoints(in.refereePoints());
        p.setUpdatedAt(Instant.now());
        if (fresh) {
            programs.insert(p);
        } else {
            programs.save(p);
        }
        List<LoyaltyTier> old = tiers.findByStoreIdOrderByPositionAsc(storeId);
        List<String> kept = new ArrayList<String>();
        List<LoyaltyTierDto> wanted = in.tiers() == null ? new ArrayList<LoyaltyTierDto>() : in.tiers();
        if (wanted.size() > 10) {
            throw new ResponseStatusException(400, "A programme has at most 10 tiers");
        }
        for (int iter = 0; iter < wanted.size(); iter++) {
            LoyaltyTierDto w = wanted.get(iter);
            if (w == null || Codes.cut(w.name(), 80).length() == 0 || w.threshold() < 0
                    || w.memberBps() < 0 || w.memberBps() > 9000 || w.earlyAccessHours() < 0
                    || w.earlyAccessHours() > 720) {
                throw new ResponseStatusException(400, "Each tier needs a name and sensible numbers");
            }
            LoyaltyTier t = null;
            for (int o = 0; o < old.size() && w.id() != null; o++) {
                if (w.id().equals(old.get(o).getId())) {
                    t = old.get(o);
                }
            }
            boolean newTier = t == null;
            if (newTier) {
                t = new LoyaltyTier();
                t.setId(Ids.next());
                t.setStoreId(storeId);
            }
            t.setName(Codes.cut(w.name(), 80));
            t.setThreshold(w.threshold());
            t.setMemberBps(w.memberBps());
            t.setFreeShipping(w.freeShipping());
            t.setEarlyAccessHours(w.earlyAccessHours());
            t.setColour(Tokens.colour(w.colour(), "#8a6d3b"));
            t.setPosition(iter);
            t.setDiscountId(perk(storeId, t.getDiscountId(), t.getName() + " member price",
                    DiscountEngine.PERCENT, t.getMemberBps(), "TIER:" + t.getId(),
                    p.isActive() && t.getMemberBps() > 0));
            t.setShippingDiscountId(perk(storeId, t.getShippingDiscountId(),
                    t.getName() + " free shipping", DiscountEngine.FREE_SHIPPING, 0,
                    "TIER:" + t.getId(), p.isActive() && t.isFreeShipping()));
            if (newTier) {
                tiers.insert(t);
            } else {
                tiers.save(t);
            }
            kept.add(t.getId());
        }
        for (int o = 0; o < old.size(); o++) {
            LoyaltyTier gone = old.get(o);
            if (!kept.contains(gone.getId())) {
                perk(storeId, gone.getDiscountId(), "", DiscountEngine.PERCENT, 0, "", false);
                perk(storeId, gone.getShippingDiscountId(), "", DiscountEngine.FREE_SHIPPING, 0, "",
                        false);
                tiers.delete(gone);
            }
        }
        return programDto(storeId);
    }

    /// Creates or updates the automatic discount behind a member perk, and
    /// answers its id. An unused perk is kept, switched off.
    public String perk(String storeId, String id, String title, String kind, long amount,
            String eligibility, boolean on) throws IOException {
        Discount d = id == null || id.length() == 0 ? null : discounts.findById(id);
        if (d == null && !on) {
            return id == null ? "" : id;
        }
        boolean fresh = d == null;
        if (fresh) {
            d = new Discount();
            d.setId(Ids.next());
            d.setStoreId(storeId);
            d.setCreatedAt(Instant.now());
        }
        if (title.length() > 0) {
            d.setTitle(Codes.cut(title, 120));
        }
        d.setKind(kind);
        d.setAmount(amount);
        d.setAppliesTo(DiscountEngine.ALL);
        d.setAutomatic(true);
        d.setCombinable(true);
        if (eligibility.length() > 0) {
            d.setEligibility(eligibility);
        }
        d.setActive(on);
        if (fresh) {
            discounts.insert(d);
        } else {
            discounts.save(d);
        }
        return d.getId();
    }

    // --------------------------------------------------------------- members

    /// The shopper's own card, joining the programme on first use.
    public LoyaltyMemberDto me(String username, String storeId) throws IOException {
        shoppers.store(storeId);
        Customer c = shoppers.customer(storeId, username);
        return dto(memberFor(c));
    }

    /// The member row of a customer, made on first use.
    @Transactional(rollbackFor = IOException.class)
    public LoyaltyMember memberFor(Customer c) throws IOException {
        LoyaltyMember m = members.findByStoreIdAndCustomerId(c.getStoreId(), c.getId());
        if (m != null) {
            return m;
        }
        m = new LoyaltyMember();
        m.setId(Ids.next());
        m.setStoreId(c.getStoreId());
        m.setCustomerId(c.getId());
        m.setUsername(c.getUsername());
        m.setEmail(c.getEmail());
        m.setPhone(c.getPhone());
        m.setCardToken(Tokens.code(20));
        m.setReferralCode(Tokens.code(8));
        m.setCreatedAt(Instant.now());
        m.setLastActivityAt(Instant.now());
        members.insert(m);
        return m;
    }

    @Transactional(readOnly = true)
    public List<LoyaltyMemberDto> members(String username, String storeId, String q)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        String needle = Tokens.fold(q);
        List<LoyaltyMember> rows = members.findByStoreIdOrderByLifetimeDesc(storeId);
        List<LoyaltyMemberDto> out = new ArrayList<LoyaltyMemberDto>();
        for (int iter = 0; iter < rows.size() && out.size() < 200; iter++) {
            LoyaltyMember m = rows.get(iter);
            if (needle.length() == 0 || Tokens.fold(m.getEmail()).indexOf(needle) >= 0
                    || Tokens.fold(m.getUsername()).indexOf(needle) >= 0) {
                out.add(dto(m));
            }
        }
        return out;
    }

    /// The member a scanned card belongs to, for a register of the same store.
    @Transactional(readOnly = true)
    public LoyaltyMemberDto card(String username, String storeId, String token) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        String t = token == null ? "" : token;
        int colon = t.lastIndexOf(':');
        if (colon >= 0) {
            // The whole QR payload was scanned: bazaar:member:<store>:<token>.
            t = t.substring(colon + 1);
        }
        LoyaltyMember m = t.length() == 0 ? null : members.findByCardToken(Codes.normalize(t));
        if (m == null || !storeId.equals(m.getStoreId())) {
            throw new ResponseStatusException(404, "No such member card");
        }
        return dto(m);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryDto> ledger(String username, String storeId, String memberId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return ledgerOf(member(storeId, memberId));
    }

    public List<LedgerEntryDto> myLedger(String username, String storeId) throws IOException {
        shoppers.store(storeId);
        return ledgerOf(memberFor(shoppers.customer(storeId, username)));
    }

    @Transactional(readOnly = true)
    List<LedgerEntryDto> ledgerOf(LoyaltyMember m) {
        List<LoyaltyLedger> rows = ledger.findByMemberIdOrderByCreatedAtDesc(m.getId());
        List<LedgerEntryDto> out = new ArrayList<LedgerEntryDto>();
        for (int iter = 0; iter < rows.size() && iter < 500; iter++) {
            LoyaltyLedger l = rows.get(iter);
            out.add(new LedgerEntryDto(l.getId(), l.getKind(), l.getPoints(), l.getOrderId(),
                    l.getNote(), l.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    public List<LoyaltyRewardDto> myRewards(String username, String storeId) throws IOException {
        shoppers.store(storeId);
        LoyaltyMember m = memberFor(shoppers.customer(storeId, username));
        List<LoyaltyReward> rows = rewards.findByMemberIdOrderByCreatedAtDesc(m.getId());
        List<LoyaltyRewardDto> out = new ArrayList<LoyaltyRewardDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(rewardDto(rows.get(iter)));
        }
        return out;
    }

    private LoyaltyMember member(String storeId, String memberId) {
        LoyaltyMember m = memberId == null ? null : members.findById(memberId);
        if (m == null || !storeId.equals(m.getStoreId())) {
            throw new ResponseStatusException(404, "No such member");
        }
        return m;
    }

    @Transactional(rollbackFor = IOException.class)
    public LoyaltyMemberDto adjust(String username, String storeId, String memberId, long points,
            String note) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        LoyaltyMember m = member(storeId, memberId);
        if (points == 0 || Math.abs(points) > MAX_ADJUST) {
            throw new ResponseStatusException(400, "Adjust by a sensible number of points");
        }
        if (members.addIf(m.getId(), points) != 1) {
            throw new ResponseStatusException(409, "That would take the balance below zero");
        }
        record(storeId, m.getId(), ADJUST, points, "", "ADJUST:" + Ids.next(),
                Codes.cut(note == null || note.length() == 0 ? "Adjusted by " + username : note, 200));
        if (points > 0) {
            touch(m.getId(), points);
        }
        return dto(members.findById(m.getId()));
    }

    /// Points to a member from a journey step, at most once per `ref`.
    @Transactional(rollbackFor = IOException.class)
    public boolean award(String storeId, String customerId, long points, String ref, String note)
            throws IOException {
        Customer c = customers.findById(customerId);
        if (c == null || points <= 0 || ledger.findByStoreIdAndRef(storeId, ref) != null) {
            return false;
        }
        LoyaltyMember m = memberFor(c);
        record(storeId, m.getId(), JOURNEY, points, "", ref, Codes.cut(note, 200));
        members.add(m.getId(), points);
        touch(m.getId(), points);
        return true;
    }

    // --------------------------------------------------------------- redeem

    public LoyaltyRewardDto redeem(String username, String storeId, long points) throws IOException {
        shoppers.store(storeId);
        LoyaltyMember m = memberFor(shoppers.customer(storeId, username));
        return issue(storeId, m.getId(), points);
    }

    public LoyaltyRewardDto redeemFor(String username, String storeId, String memberId, long points)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return issue(storeId, member(storeId, memberId).getId(), points);
    }

    /// Takes `points` off the balance and issues a single-use code worth
    /// them; checkout and the register apply it as a discount line.
    @Transactional(rollbackFor = IOException.class)
    LoyaltyRewardDto issue(String storeId, String memberId, long points) throws IOException {
        LoyaltyProgram p = programs.findByStoreId(storeId);
        LoyaltyMember m = members.findById(memberId);
        String why = LoyaltyMath.redeemProblem(points, m.getBalance(), p);
        if (why != null) {
            throw new ResponseStatusException(why.startsWith("Not enough") ? 409 : 400, why);
        }
        if (members.addIf(memberId, -points) != 1) {
            throw new ResponseStatusException(409, "Not enough points");
        }
        long cents = LoyaltyMath.value(points, p);
        Instant now = Instant.now();
        Discount d = new Discount();
        d.setId(Ids.next());
        d.setStoreId(storeId);
        String code = "PTS" + Tokens.code(9);
        d.setCode(code);
        d.setTitle(points + " points");
        d.setKind(DiscountEngine.FIXED);
        d.setAmount(cents);
        d.setAppliesTo(DiscountEngine.ALL);
        d.setUsageLimit(1);
        d.setCombinable(true);
        d.setStartsAt(now);
        d.setEndsAt(now.plusMillis(REWARD_DAYS * DAY));
        d.setActive(true);
        d.setCreatedAt(now);
        discounts.insert(d);
        LoyaltyReward r = new LoyaltyReward();
        r.setId(Ids.next());
        r.setStoreId(storeId);
        r.setMemberId(memberId);
        r.setPoints(points);
        r.setCents(cents);
        r.setDiscountId(d.getId());
        r.setCode(code);
        r.setStatus(ISSUED);
        r.setCreatedAt(now);
        r.setExpiresAt(d.getEndsAt());
        rewards.insert(r);
        record(storeId, memberId, REDEEM, -points, "", "REDEEM:" + r.getId(), "Reward " + code);
        return rewardDto(r);
    }

    static LoyaltyRewardDto rewardDto(LoyaltyReward r) {
        return new LoyaltyRewardDto(r.getId(), r.getPoints(), r.getCents(), r.getCode(),
                r.getStatus(), r.getExpiresAt().toEpochMilli());
    }

    // ------------------------------------------------------- earn and reverse

    /// Earns an order's points once it is paid. Safe to call for every paid
    /// event of the order: the second call finds `EARN:<order>` and stops.
    /// Also settles a referral on the referee's first paid order.
    @Transactional(rollbackFor = IOException.class)
    public long earn(String orderId) throws IOException {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null || o.getCustomerId().length() == 0) {
            return 0;
        }
        String f = o.getFinancial();
        if (!"PAID".equals(f) && !"PARTIALLY_REFUNDED".equals(f)) {
            return 0;
        }
        LoyaltyProgram p = programs.findByStoreId(o.getStoreId());
        if (p == null || !p.isActive() || "POS".equals(o.getChannel()) && !p.isEarnOnPos()) {
            return 0;
        }
        if (ledger.findByStoreIdAndRef(o.getStoreId(), EARN + ":" + orderId) != null) {
            return 0;
        }
        Customer c = customers.findById(o.getCustomerId());
        if (c == null) {
            return 0;
        }
        LoyaltyMember m = memberFor(c);
        long eligible = o.getSubtotalCents() - o.getDiscountCents();
        long points = LoyaltyMath.points(eligible, p);
        record(o.getStoreId(), m.getId(), EARN, points, orderId, EARN + ":" + orderId,
                "Order " + o.getNumber());
        if (points > 0) {
            members.add(m.getId(), points);
        }
        touch(m.getId(), points);
        referral(p, members.findById(m.getId()), o);
        return points;
    }

    /// Takes back the share of an order's points its refunds (or its
    /// cancellation) now cover. Asked again with nothing new refunded it
    /// moves nothing.
    @Transactional(rollbackFor = IOException.class)
    public long reverse(String orderId, boolean cancelled) throws IOException {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null) {
            return 0;
        }
        LoyaltyLedger earned = ledger.findByStoreIdAndRef(o.getStoreId(), EARN + ":" + orderId);
        if (earned == null || earned.getPoints() <= 0) {
            return 0;
        }
        long paid = o.getTotalCents();
        long target = cancelled ? earned.getPoints()
                : LoyaltyMath.reversedFor(earned.getPoints(), paid, o.getRefundedCents());
        long done = 0;
        List<LoyaltyLedger> rows = ledger.findByStoreIdAndOrderId(o.getStoreId(), orderId);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (REVERSE.equals(rows.get(iter).getKind())) {
                done -= rows.get(iter).getPoints();
            }
        }
        long delta = target - done;
        if (delta <= 0) {
            return 0;
        }
        String ref = REVERSE + ":" + orderId + ":" + target;
        if (ledger.findByStoreIdAndRef(o.getStoreId(), ref) != null) {
            return 0;
        }
        record(o.getStoreId(), earned.getMemberId(), REVERSE, -delta, orderId, ref,
                cancelled ? "Order cancelled" : "Order refunded");
        members.add(earned.getMemberId(), -delta);
        return delta;
    }

    // -------------------------------------------------------------- referrals

    /// Records who referred the shopper. Only before their first order, never
    /// themselves, and only once.
    @Transactional(rollbackFor = IOException.class)
    public LoyaltyMemberDto applyReferral(String username, String storeId, String code)
            throws IOException {
        shoppers.store(storeId);
        LoyaltyMember m = memberFor(shoppers.customer(storeId, username));
        LoyaltyMember referrer = code == null ? null
                : members.findByStoreIdAndReferralCode(storeId, Codes.normalize(code));
        if (referrer == null) {
            throw new ResponseStatusException(404, "No such referral code");
        }
        if (referrer.getId().equals(m.getId())) {
            throw new ResponseStatusException(400, "That is your own code");
        }
        if (m.getReferredBy().length() > 0) {
            throw new ResponseStatusException(409, "A referral is already recorded");
        }
        Customer c = customers.findById(m.getCustomerId());
        if (c != null && c.getOrdersCount() > 0) {
            throw new ResponseStatusException(409, "Referrals are for new customers");
        }
        m.setReferredBy(referrer.getId());
        members.save(m);
        return dto(m);
    }

    /// Rewards both sides on the referee's first paid order, unless the two
    /// look like one person: the same mailbox, phone, or delivery address.
    void referral(LoyaltyProgram p, LoyaltyMember referee, StoreOrder order) throws IOException {
        if (referee.getReferredBy().length() == 0 || referee.isReferralRewarded()) {
            return;
        }
        LoyaltyMember referrer = members.findById(referee.getReferredBy());
        if (referrer == null || members.claimReferral(referee.getId()) != 1) {
            return;
        }
        String why = sameperson(referrer, referee, order);
        if (why != null) {
            record(p.getStoreId(), referee.getId(), REFERRAL, 0, order.getId(),
                    REFERRAL + ":" + referee.getId() + ":blocked", "Referral not rewarded: " + why);
            return;
        }
        if (p.getReferrerPoints() > 0) {
            record(p.getStoreId(), referrer.getId(), REFERRAL, p.getReferrerPoints(), order.getId(),
                    REFERRAL + ":" + referee.getId() + ":referrer", "Referred a friend");
            members.add(referrer.getId(), p.getReferrerPoints());
            touch(referrer.getId(), p.getReferrerPoints());
        }
        if (p.getRefereePoints() > 0) {
            record(p.getStoreId(), referee.getId(), REFERRAL, p.getRefereePoints(), order.getId(),
                    REFERRAL + ":" + referee.getId() + ":referee", "Welcome reward");
            members.add(referee.getId(), p.getRefereePoints());
            touch(referee.getId(), p.getRefereePoints());
        }
    }

    /// Why two members look like the same person, or null.
    String sameperson(LoyaltyMember a, LoyaltyMember b, StoreOrder order) {
        Customer ca = customers.findById(a.getCustomerId());
        Customer cb = customers.findById(b.getCustomerId());
        String ma = Tokens.mailbox(ca == null ? a.getEmail() : ca.getEmail());
        String mb = Tokens.mailbox(order.getEmail().length() > 0 ? order.getEmail()
                : cb == null ? b.getEmail() : cb.getEmail());
        if (ma.length() > 0 && ma.equals(mb)) {
            return "same e-mail";
        }
        String pa = Tokens.digits(ca == null ? a.getPhone() : ca.getPhone());
        String pb = Tokens.digits(order.getPhone().length() > 0 ? order.getPhone()
                : order.getShipPhone());
        if (pa.length() >= 6 && pa.equals(pb)) {
            return "same phone";
        }
        String where = place(order.getShipLine1(), order.getShipPostal());
        if (where.length() > 1 && ca != null) {
            List<CustomerAddress> saved = addresses.findByCustomerId(ca.getId());
            for (int iter = 0; iter < saved.size(); iter++) {
                if (where.equals(place(saved.get(iter).getLine1(), saved.get(iter).getPostal()))) {
                    return "same address";
                }
            }
            List<StoreOrder> past = orders.findByCustomerIdOrderByCreatedAtDesc(ca.getId());
            for (int iter = 0; iter < past.size() && iter < 50; iter++) {
                if (where.equals(place(past.get(iter).getShipLine1(), past.get(iter).getShipPostal()))) {
                    return "same address";
                }
            }
        }
        return null;
    }

    private static String place(String line1, String postal) {
        StringBuilder out = new StringBuilder();
        String s = Tokens.fold(line1) + "|" + Tokens.fold(postal);
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c != ' ' && c != '.' && c != ',') {
                out.append(c);
            }
        }
        return out.length() <= 1 ? "" : out.toString();
    }

    // ------------------------------------------------------------ the ledger

    private void record(String storeId, String memberId, String kind, long points, String orderId,
            String ref, String note) throws IOException {
        LoyaltyLedger l = new LoyaltyLedger();
        l.setId(Ids.next());
        l.setStoreId(storeId);
        l.setMemberId(memberId);
        l.setKind(kind);
        l.setPoints(points);
        l.setOrderId(orderId == null ? "" : orderId);
        l.setRef(Codes.cut(ref, 80));
        l.setNote(Codes.cut(note, 200));
        l.setCreatedAt(Instant.now());
        ledger.insert(l);
    }

    /// Notes activity and recomputes the member's lifetime points and tier.
    private void touch(String memberId, long earned) {
        LoyaltyMember m = members.findById(memberId);
        if (m == null) {
            return;
        }
        if (earned > 0) {
            m.setLifetime(m.getLifetime() + earned);
        }
        m.setLastActivityAt(Instant.now());
        LoyaltyTier t = LoyaltyMath.tierFor(qualifying(m),
                tiers.findByStoreIdOrderByPositionAsc(m.getStoreId()));
        m.setTierId(t == null ? "" : t.getId());
        members.save(m);
    }

    /// The spend (cents, net of refunds, of paid orders) or the points earned
    /// over the last 365 days, whichever the programme ranks tiers by.
    long qualifying(LoyaltyMember m) {
        LoyaltyProgram p = programs.findByStoreId(m.getStoreId());
        long since = System.currentTimeMillis() - YEAR;
        long total = 0;
        if (p != null && POINTS.equals(p.getTierBasis())) {
            List<LoyaltyLedger> rows = ledger.findByMemberIdAndCreatedAtGreaterThanEqual(m.getId(),
                    Instant.ofEpochMilli(since));
            for (int iter = 0; iter < rows.size(); iter++) {
                LoyaltyLedger l = rows.get(iter);
                if (EARN.equals(l.getKind()) || REVERSE.equals(l.getKind())) {
                    total += l.getPoints();
                }
            }
            return Math.max(0, total);
        }
        List<StoreOrder> rows = orders.findByCustomerIdOrderByCreatedAtDesc(m.getCustomerId());
        for (int iter = 0; iter < rows.size(); iter++) {
            StoreOrder o = rows.get(iter);
            if (o.getCreatedAt().toEpochMilli() < since) {
                break;
            }
            String f = o.getFinancial();
            if ("PAID".equals(f) || "PARTIALLY_REFUNDED".equals(f)) {
                total += Math.max(0, o.getTotalCents() - o.getRefundedCents());
            }
        }
        return total;
    }

    @Transactional(readOnly = true)
    public LoyaltyMemberDto dto(LoyaltyMember m) {
        List<LoyaltyTier> ts = tiers.findByStoreIdOrderByPositionAsc(m.getStoreId());
        long q = qualifying(m);
        LoyaltyTier now = LoyaltyMath.tierFor(q, ts);
        LoyaltyTier next = LoyaltyMath.nextTier(q, ts);
        Store s = stores.findById(m.getStoreId());
        return new LoyaltyMemberDto(m.getId(), m.getCustomerId(), m.getUsername(), m.getEmail(),
                m.getBalance(), m.getLifetime(), now == null ? "" : now.getId(),
                now == null ? "" : now.getName(), next == null ? "" : next.getName(),
                next == null ? 0 : next.getThreshold() - q, m.getCardToken(),
                payload(s == null ? "" : s.getHandle(), m.getCardToken()), m.getReferralCode(), q,
                m.getCreatedAt().toEpochMilli());
    }

    /// What a member card's QR code holds.
    public static String payload(String storeHandle, String token) {
        return "bazaar:member:" + storeHandle + ":" + token;
    }

    // ----------------------------------------------------------------- jobs

    @Scheduled(fixedDelay = 3600000, initialDelay = 90000, lock = "bazaar-loyalty-expiry")
    public void expireJob() {
        try {
            tick(Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("loyalty expiry: " + failed);
        }
    }

    /// Expires unused reward codes (their points go back), marks used ones,
    /// and lapses the balances of members inactive past the programme's
    /// `expiryDays`. Answers how many rows it changed.
    public int tick(Instant now) throws IOException {
        int changed = 0;
        List<LoyaltyReward> due = rewards.findByStatusAndExpiresAtLessThan(ISSUED, now);
        for (int iter = 0; iter < due.size(); iter++) {
            if (settleReward(due.get(iter).getId())) {
                changed++;
            }
        }
        List<LoyaltyProgram> active = programs.findByActive(true);
        for (int iter = 0; iter < active.size(); iter++) {
            LoyaltyProgram p = active.get(iter);
            if (p.getExpiryDays() <= 0) {
                continue;
            }
            Instant before = now.minusMillis(p.getExpiryDays() * DAY);
            List<LoyaltyMember> idle = members
                    .findByStoreIdAndBalanceGreaterThanAndLastActivityAtLessThan(p.getStoreId(), 0,
                            before);
            for (int m = 0; m < idle.size(); m++) {
                if (lapse(idle.get(m).getId())) {
                    changed++;
                }
            }
        }
        return changed;
    }

    @Transactional(rollbackFor = IOException.class)
    boolean settleReward(String rewardId) throws IOException {
        LoyaltyReward r = rewards.findById(rewardId);
        if (r == null || !ISSUED.equals(r.getStatus())) {
            return false;
        }
        Discount d = discounts.findById(r.getDiscountId());
        boolean used = d != null && d.getUsedCount() > 0;
        if (rewards.move(r.getId(), ISSUED, used ? USED : EXPIRED) != 1) {
            return false;
        }
        if (!used) {
            if (d != null) {
                d.setActive(false);
                discounts.save(d);
            }
            record(r.getStoreId(), r.getMemberId(), RESTORE, r.getPoints(), "",
                    RESTORE + ":" + r.getId(), "Reward " + r.getCode() + " expired unused");
            members.add(r.getMemberId(), r.getPoints());
        }
        return true;
    }

    @Transactional(rollbackFor = IOException.class)
    boolean lapse(String memberId) throws IOException {
        LoyaltyMember m = members.findById(memberId);
        if (m == null || m.getBalance() <= 0) {
            return false;
        }
        String ref = EXPIRE + ":" + m.getId() + ":" + m.getLastActivityAt().toEpochMilli();
        if (ledger.findByStoreIdAndRef(m.getStoreId(), ref) != null) {
            return false;
        }
        record(m.getStoreId(), m.getId(), EXPIRE, -m.getBalance(), "", ref, "Points expired");
        members.add(m.getId(), -m.getBalance());
        return true;
    }
}
