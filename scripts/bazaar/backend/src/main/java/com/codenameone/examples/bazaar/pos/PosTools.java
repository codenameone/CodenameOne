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

package com.codenameone.examples.bazaar.pos;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.ApprovalDto;
import com.codenameone.examples.bazaar.api.ApprovalRequestDto;
import com.codenameone.examples.bazaar.api.CommissionDto;
import com.codenameone.examples.bazaar.api.GridTileDto;
import com.codenameone.examples.bazaar.api.ParkedCartDto;
import com.codenameone.examples.bazaar.api.PosGridDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosPolicyDto;
import com.codenameone.examples.bazaar.api.StaffRateDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.AuditEvent;
import com.codenameone.examples.bazaar.domain.Collection;
import com.codenameone.examples.bazaar.domain.GridTile;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.ParkedCart;
import com.codenameone.examples.bazaar.domain.ParkedLine;
import com.codenameone.examples.bazaar.domain.PosApproval;
import com.codenameone.examples.bazaar.domain.PosPolicy;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.StaffRate;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.Money;
import com.codenameone.examples.bazaar.repository.AuditEventRepository;
import com.codenameone.examples.bazaar.repository.CollectionRepository;
import com.codenameone.examples.bazaar.repository.GridTileRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.ParkedCartRepository;
import com.codenameone.examples.bazaar.repository.ParkedLineRepository;
import com.codenameone.examples.bazaar.repository.PosApprovalRepository;
import com.codenameone.examples.bazaar.repository.PosPolicyRepository;
import com.codenameone.examples.bazaar.repository.StaffRateRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The point of sale's tools around the sale: the store's policy, commission
/// rates and their report, manager approvals, parked carts and the smart grid.
///
/// A manager approval is a manager typing their own PIN at the register. The
/// PIN goes through the same lockout as a sign-in, and what comes back is an
/// approval id the sale, return or edit carries -- never the PIN, since a sale
/// may wait offline in the register's outbox. An approval is used once, by
/// the first thing that names it, and both the grant and the use are audited.
@Component
public class PosTools {
    public static final String DISCOUNT = "DISCOUNT";
    public static final String PRICE_OVERRIDE = "PRICE_OVERRIDE";
    public static final String REFUND = "REFUND";
    public static final String ORDER_EDIT = "ORDER_EDIT";
    public static final String PARKED = "PARKED";
    public static final String RESUMED = "RESUMED";
    public static final String DISCARDED = "DISCARDED";
    static final int MAX_TILES = 60;
    static final int MAX_PARKED_LINES = 100;
    static final long MAX_BP = 10000L;
    /// The actions a grid tile may start, each a button the sell screen knows.
    static final String[] ACTIONS = {"CUSTOM_ITEM", "PARK", "PARKED", "LAYAWAY", "CUSTOMER",
        "DISCOUNT"};
    static final long DAY = 24L * 60L * 60L * 1000L;

    private final Pos pos;
    private final StoreAccess access;
    private final StoreStaffRepository staff;
    private final PosPolicyRepository policies;
    private final StaffRateRepository rates;
    private final PosApprovalRepository approvals;
    private final ParkedCartRepository parked;
    private final ParkedLineRepository parkedLines;
    private final GridTileRepository tiles;
    private final LocationRepository locations;
    private final CollectionRepository collections;
    private final VariantRepository variants;
    private final StoreOrderRepository orders;
    private final AuditEventRepository audits;
    private final PasswordEncoder encoder;

    public PosTools(Pos pos, StoreAccess access, StoreStaffRepository staff,
            PosPolicyRepository policies, StaffRateRepository rates,
            PosApprovalRepository approvals, ParkedCartRepository parked,
            ParkedLineRepository parkedLines, GridTileRepository tiles,
            LocationRepository locations, CollectionRepository collections,
            VariantRepository variants, StoreOrderRepository orders,
            AuditEventRepository audits, PasswordEncoder encoder) {
        this.pos = pos;
        this.access = access;
        this.staff = staff;
        this.policies = policies;
        this.rates = rates;
        this.approvals = approvals;
        this.parked = parked;
        this.parkedLines = parkedLines;
        this.tiles = tiles;
        this.locations = locations;
        this.collections = collections;
        this.variants = variants;
        this.orders = orders;
        this.audits = audits;
        this.encoder = encoder;
    }

    // --------------------------------------------------------------- policy

    /// The store's policy row, or one with nothing switched on when the
    /// owner never set one: until then nothing needs an approval.
    @Transactional(readOnly = true)
    public PosPolicy policyOf(String storeId) {
        PosPolicy p = policies.findByStoreId(storeId);
        if (p != null) {
            return p;
        }
        p = new PosPolicy();
        p.setStoreId(storeId);
        p.setTipPresets("");
        p.setLayawayDepositBp(2000);
        p.setLayawayCancelFeeBp(0);
        return p;
    }

    public PosPolicyDto policy(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(policyOf(storeId));
    }

    @Transactional(rollbackFor = IOException.class)
    public PosPolicyDto savePolicy(String username, String storeId, PosPolicyDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Send the policy");
        }
        long[] bps = {in.discountApprovalBp(), in.tip1Bp(), in.tip2Bp(), in.tip3Bp(),
            in.layawayDepositBp(), in.layawayCancelFeeBp()};
        for (int iter = 0; iter < bps.length; iter++) {
            if (bps[iter] < 0 || bps[iter] > MAX_BP) {
                throw new ResponseStatusException(400, "A percentage is between 0 and 100");
            }
        }
        PosPolicy p = policies.findByStoreId(storeId);
        boolean fresh = p == null;
        if (fresh) {
            p = new PosPolicy();
            p.setId(Ids.next());
            p.setStoreId(storeId);
        }
        p.setDiscountApprovalBp(in.discountApprovalBp());
        p.setApprovePriceOverrides(in.approvePriceOverrides());
        p.setApproveRefunds(in.approveRefunds());
        p.setApproveOrderEdits(in.approveOrderEdits());
        p.setTipPresets(in.tip1Bp() + "," + in.tip2Bp() + "," + in.tip3Bp());
        p.setLayawayDepositBp(in.layawayDepositBp());
        p.setLayawayCancelFeeBp(in.layawayCancelFeeBp());
        p.setUpdatedAt(Instant.now());
        if (fresh) {
            policies.insert(p);
        } else {
            policies.save(p);
        }
        audit(storeId, username, "POS_POLICY", p.getId(), "Point-of-sale policy changed");
        return dto(p);
    }

    static PosPolicyDto dto(PosPolicy p) {
        long[] tips = tips(p.getTipPresets());
        return new PosPolicyDto(p.getDiscountApprovalBp(), p.isApprovePriceOverrides(),
                p.isApproveRefunds(), p.isApproveOrderEdits(), tips[0], tips[1], tips[2],
                p.getLayawayDepositBp(), p.getLayawayCancelFeeBp());
    }

    /// Three basis-point values out of "a,b,c"; anything unreadable is 0.
    static long[] tips(String text) {
        long[] out = new long[3];
        if (text == null) {
            return out;
        }
        int slot = 0;
        long value = 0;
        for (int iter = 0; iter <= text.length() && slot < 3; iter++) {
            char ch = iter == text.length() ? ',' : text.charAt(iter);
            if (ch == ',') {
                out[slot++] = Math.min(MAX_BP, value);
                value = 0;
            } else if (ch >= '0' && ch <= '9' && value < MAX_BP) {
                value = value * 10 + (ch - '0');
            }
        }
        return out;
    }

    // ----------------------------------------------------------- commission

    @Transactional(readOnly = true)
    public List<StaffRateDto> staffRates(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<StoreStaff> team = staff.findByStoreIdOrderByDisplayNameAsc(storeId);
        Map<String, Long> byUser = rateMap(storeId);
        List<StaffRateDto> out = new ArrayList<StaffRateDto>();
        for (int iter = 0; iter < team.size(); iter++) {
            StoreStaff m = team.get(iter);
            Long r = byUser.get(m.getUsername());
            out.add(new StaffRateDto(m.getUsername(), m.getDisplayName(), m.getRole(),
                    r == null ? 0 : r.longValue()));
        }
        return out;
    }

    /// `username`'s commission rate now, in basis points (0 when none is set):
    /// what a counter sale they make is attributed at.
    @Transactional(readOnly = true)
    public long rateFor(String storeId, String username) {
        StaffRate r = username == null ? null : rates.findByStoreIdAndUsername(storeId, username);
        return r == null ? 0 : Math.max(0, r.getRateBp());
    }

    private Map<String, Long> rateMap(String storeId) {
        Map<String, Long> out = new HashMap<String, Long>();
        List<StaffRate> rows = rates.findByStoreId(storeId);
        for (int iter = 0; iter < rows.size(); iter++) {
            out.put(rows.get(iter).getUsername(), Long.valueOf(rows.get(iter).getRateBp()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public StaffRateDto saveStaffRate(String username, String storeId, StaffRateDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null || in.username() == null || in.rateBp() < 0 || in.rateBp() > MAX_BP) {
            throw new ResponseStatusException(400, "A rate names a staff member and is 0 to 100%");
        }
        StoreStaff m = staff.findByStoreIdAndUsername(storeId, Accounts.email(in.username()));
        if (m == null) {
            throw new ResponseStatusException(404, "Not on this store's staff");
        }
        StaffRate r = rates.findByStoreIdAndUsername(storeId, m.getUsername());
        if (r == null) {
            r = new StaffRate();
            r.setId(Ids.next());
            r.setStoreId(storeId);
            r.setUsername(m.getUsername());
            r.setRateBp(in.rateBp());
            r.setUpdatedAt(Instant.now());
            rates.insert(r);
        } else {
            r.setRateBp(in.rateBp());
            r.setUpdatedAt(Instant.now());
            rates.save(r);
        }
        audit(storeId, username, "COMMISSION_RATE", m.getUsername(),
                "Commission for " + m.getUsername() + " set to " + in.rateBp() + " bp");
        return new StaffRateDto(m.getUsername(), m.getDisplayName(), m.getRole(), in.rateBp());
    }

    /// Counter sales per staff member who made them, closed between `from`
    /// and `to`. Net is the total less tax and refunds (the refund's share of
    /// tax taken out pro rata); a tip is reported apart and earns nothing.
    /// The commission is each sale's net at the rate kept on the sale when it
    /// was made, so a rate changed later earns from then on; `rateBp` is the
    /// rate now.
    @Transactional(readOnly = true)
    public List<CommissionDto> commissions(String username, String storeId, String fromText,
            String toText) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        long now = System.currentTimeMillis();
        long to = millis(toText, now + 1);
        long from = millis(fromText, to - 30L * DAY);
        if (from >= to) {
            throw new ResponseStatusException(400, "The period ends before it starts");
        }
        Map<String, Long> rateOf = rateMap(storeId);
        Map<String, long[]> sums = new HashMap<String, long[]>();
        List<String> order = new ArrayList<String>();
        List<StoreOrder> rows = orders.findByStoreIdAndCreatedAtGreaterThanEqual(storeId,
                Instant.ofEpochMilli(from));
        for (int iter = 0; iter < rows.size(); iter++) {
            StoreOrder o = rows.get(iter);
            if (o.getCreatedAt().toEpochMilli() >= to || !Orders.POS.equals(o.getChannel())
                    || !(Orders.CLOSED.equals(o.getStatus()) || shipped(o))
                    || o.getStaffUsername().length() == 0) {
                continue;
            }
            long[] s = sums.get(o.getStaffUsername());
            if (s == null) {
                s = new long[4];
                sums.put(o.getStaffUsername(), s);
                order.add(o.getStaffUsername());
            }
            long net = net(o);
            s[0]++;
            s[1] += net;
            s[2] += o.getTipCents();
            // Each sale earns at the rate it was made at; one made before
            // sales kept their rate earns at the current one.
            Long current = rateOf.get(o.getStaffUsername());
            long bp = o.getCommissionBp() >= 0 ? o.getCommissionBp()
                    : current == null ? 0 : current.longValue();
            s[3] += Money.basisPoints(net, bp);
        }
        List<CommissionDto> out = new ArrayList<CommissionDto>();
        for (int iter = 0; iter < order.size(); iter++) {
            String who = order.get(iter);
            long[] s = sums.get(who);
            Long r = rateOf.get(who);
            long bp = r == null ? 0 : r.longValue();
            StoreStaff m = staff.findByStoreIdAndUsername(storeId, who);
            out.add(new CommissionDto(who, m == null ? who : m.getDisplayName(), (int) s[0], s[1],
                    s[2], bp, s[3]));
        }
        return out;
    }

    /// A sale at the counter shipped to the customer: open until it ships,
    /// but paid in full and earned when it was made.
    static boolean shipped(StoreOrder o) {
        return Orders.OPEN.equals(o.getStatus()) && Orders.PAID.equals(o.getFinancial())
                && o.getShipLine1().length() > 0;
    }

    /// An order's sales net of tax and of what was refunded.
    static long net(StoreOrder o) {
        long total = o.getTotalCents();
        long goods = total - o.getTaxCents();
        long refundedGoods = total <= 0 ? 0
                : Money.divideHalfEven(Math.min(o.getRefundedCents(), total) * goods, total);
        return Math.max(0, goods - refundedGoods);
    }

    static long millis(String text, long fallback) {
        if (text == null || text.trim().length() == 0) {
            return fallback;
        }
        String t = text.trim();
        long v = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char ch = t.charAt(iter);
            if (ch < '0' || ch > '9' || iter > 15) {
                throw new ResponseStatusException(400, "A time is milliseconds since 1970");
            }
            v = v * 10 + (ch - '0');
        }
        return v;
    }

    // ------------------------------------------------------------- approval

    /// Checks a manager's PIN and grants an approval of `kind`.
    public ApprovalDto approve(String username, String storeId, String registerId,
            ApprovalRequestDto req) throws IOException {
        StoreStaff manager = approver(username, storeId, registerId, req);
        if (manager.getPinLockedUntil() != null
                && manager.getPinLockedUntil().toEpochMilli() > System.currentTimeMillis()) {
            throw new ResponseStatusException(423, "Too many wrong PINs; try again later");
        }
        if (manager.getPinHash().length() == 0) {
            throw new ResponseStatusException(409, "No PIN is set for " + manager.getUsername());
        }
        String typed = req.pin() == null ? "" : req.pin().trim();
        if (typed.length() == 0 || !encoder.matches(typed, manager.getPinHash())) {
            if (pos.wrongPin(manager.getId())) {
                throw new ResponseStatusException(423, "Too many wrong PINs; try again later");
            }
            audit(storeId, username, "POS_APPROVAL_REFUSED", registerId,
                    "Wrong manager PIN for " + req.kind());
            throw new ResponseStatusException(401, "Wrong PIN");
        }
        if (manager.getPinFailures() > 0) {
            pos.rightPin(manager.getId());
        }
        return grant(username, storeId, registerId, manager, req);
    }

    @Transactional(readOnly = true)
    private StoreStaff approver(String username, String storeId, String registerId,
            ApprovalRequestDto req) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        pos.register(storeId, registerId);
        if (req == null || req.kind() == null || !kind(req.kind())) {
            throw new ResponseStatusException(400,
                    "An approval is a DISCOUNT, PRICE_OVERRIDE, REFUND or ORDER_EDIT");
        }
        if (req.amountCents() < 0) {
            throw new ResponseStatusException(400, "That is not an amount");
        }
        String who = req.managerUsername() == null ? "" : Accounts.email(req.managerUsername());
        StoreStaff m = who.length() == 0 ? null : staff.findByStoreIdAndUsername(storeId, who);
        if (m == null || !StoreAccess.isRole(m.getRole(), StoreAccess.MANAGERS)) {
            throw new ResponseStatusException(403, "Only an owner or a manager can approve");
        }
        return m;
    }

    static boolean kind(String k) {
        return DISCOUNT.equals(k) || PRICE_OVERRIDE.equals(k) || REFUND.equals(k)
                || ORDER_EDIT.equals(k);
    }

    @Transactional(rollbackFor = IOException.class)
    private ApprovalDto grant(String username, String storeId, String registerId,
            StoreStaff manager, ApprovalRequestDto req) throws IOException {
        PosApproval a = new PosApproval();
        a.setId(Ids.next());
        a.setStoreId(storeId);
        a.setRegisterId(registerId);
        a.setKind(req.kind());
        a.setApprover(manager.getUsername());
        a.setRequestedBy(username);
        a.setAmountCents(req.amountCents());
        a.setReason(Codes.cut(req.reason() == null ? "" : req.reason(), 200));
        a.setUsedBy("");
        a.setCreatedAt(Instant.now());
        approvals.insert(a);
        audit(storeId, manager.getUsername(), "POS_APPROVAL", a.getId(), req.kind() + " of "
                + req.amountCents() + " cents approved for " + username
                + (a.getReason().length() > 0 ? ": " + a.getReason() : ""));
        return new ApprovalDto(a.getId(), a.getKind(), a.getApprover(), a.getRequestedBy(),
                a.getAmountCents(), a.getCreatedAt().toEpochMilli());
    }

    /// Whether `member` may do what needs an approval without one: owners
    /// and managers approve themselves.
    public static boolean approvesItself(StoreStaff member) {
        return member != null && StoreAccess.isRole(member.getRole(), StoreAccess.MANAGERS);
    }

    /// Answers 403 unless `approvalId` is an unused approval of the store of
    /// one of `kinds`. Reads only: [#use] spends it.
    @Transactional(readOnly = true)
    public PosApproval requireApproval(String storeId, String approvalId, String... kinds) {
        PosApproval a = approvalId == null || approvalId.length() == 0 ? null
                : approvals.findById(approvalId);
        if (a == null || !storeId.equals(a.getStoreId())) {
            throw new ResponseStatusException(403, "A manager's approval is needed");
        }
        if (a.getUsedBy().length() > 0) {
            throw new ResponseStatusException(409, "That approval was already used");
        }
        if (!StoreAccess.isRole(a.getKind(), kinds)) {
            throw new ResponseStatusException(403, "That approval is for something else");
        }
        return a;
    }

    /// Spends an approval on `subject`, in the caller's transaction; 409 when
    /// something else spent it first.
    public void use(String storeId, String approvalId, String subject, String actor)
            throws IOException {
        if (approvals.use(approvalId, Codes.cut(subject, 64)) != 1) {
            throw new ResponseStatusException(409, "That approval was already used");
        }
        audit(storeId, actor, "POS_APPROVAL_USED", approvalId, "Approval used by " + subject);
    }

    /// The approval a refund at the counter needs, or null: one is needed
    /// when the store's policy asks for it and the caller is not an owner or
    /// a manager.
    @Transactional(readOnly = true)
    public PosApproval refundApproval(String username, String storeId, String approvalId)
            throws IOException {
        StoreStaff me = access.requireStaff(username, storeId, StoreAccess.ANYONE);
        if (approvesItself(me) || !policyOf(storeId).isApproveRefunds()) {
            return null;
        }
        return requireApproval(storeId, approvalId, REFUND);
    }

    /// [#use] in a transaction of its own.
    @Transactional(rollbackFor = IOException.class)
    public void spend(String storeId, String approvalId, String subject, String actor)
            throws IOException {
        use(storeId, approvalId, subject, actor);
    }

    /// Who made a sale: the PIN-signed-in member it names, or the device's
    /// account when it names none. 400 for anyone not on the store's staff.
    @Transactional(readOnly = true)
    public StoreStaff seller(String storeId, String named, String caller) {
        String who = named == null || named.trim().length() == 0 ? caller
                : Accounts.email(named);
        StoreStaff m = staff.findByStoreIdAndUsername(storeId, who);
        if (m == null) {
            throw new ResponseStatusException(400, "The sale names someone not on the staff");
        }
        return m;
    }

    // --------------------------------------------------------------- parked

    @Transactional(readOnly = true)
    public List<ParkedCartDto> parked(String username, String storeId, String registerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Register r = pos.register(storeId, registerId);
        List<ParkedCart> rows = parked.findByRegisterIdAndStatusOrderByCreatedAtAsc(r.getId(),
                PARKED);
        List<ParkedCartDto> out = new ArrayList<ParkedCartDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    /// Parks a cart. The register's `clientId` names it for good: sent again
    /// -- even after it was resumed -- the cart kept the first time is the
    /// answer, so a replayed offline queue never brings a resumed cart back.
    public ParkedCartDto park(String username, String storeId, String registerId,
            ParkedCartDto in) throws IOException {
        Register r = checkPark(username, storeId, registerId, in);
        String clientId = in.clientId().trim();
        ParkedCart again = byClientId(r.getId(), clientId);
        if (again != null) {
            return dto(again);
        }
        try {
            return dto(insertParked(username, storeId, r, clientId, in));
        } catch (RuntimeException collided) {
            // The same hold arriving twice at once: the unique index let one in.
            ParkedCart first = byClientId(r.getId(), clientId);
            if (first != null) {
                return dto(first);
            }
            throw collided;
        }
    }

    @Transactional(readOnly = true)
    private Register checkPark(String username, String storeId, String registerId,
            ParkedCartDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Register r = pos.register(storeId, registerId);
        if (in == null || in.clientId() == null || in.clientId().trim().length() == 0
                || in.clientId().trim().length() > 64) {
            throw new ResponseStatusException(400, "A parked cart needs its register's id for it");
        }
        if (in.lines() == null || in.lines().isEmpty() || in.lines().size() > MAX_PARKED_LINES) {
            throw new ResponseStatusException(400, "A parked cart has 1 to 100 lines");
        }
        for (int iter = 0; iter < in.lines().size(); iter++) {
            PosLineDto l = in.lines().get(iter);
            if (l == null || l.quantity() <= 0 || l.quantity() > 10000 || l.unitCents() < 0) {
                throw new ResponseStatusException(400, "A line is an item and a quantity");
            }
            boolean custom = l.variantId() == null || l.variantId().length() == 0;
            if (custom && (l.title() == null || l.title().trim().length() == 0)) {
                throw new ResponseStatusException(400, "A custom item needs a title");
            }
            if (!custom) {
                Variant v = variants.findById(l.variantId());
                if (v == null || !storeId.equals(v.getStoreId())) {
                    throw new ResponseStatusException(400, "An item of the cart is not the store's");
                }
            }
        }
        return r;
    }

    @Transactional(readOnly = true)
    private ParkedCart byClientId(String registerId, String clientId) {
        return parked.findByRegisterIdAndClientId(registerId, clientId);
    }

    @Transactional(rollbackFor = IOException.class)
    private ParkedCart insertParked(String username, String storeId, Register r, String clientId,
            ParkedCartDto in) throws IOException {
        ParkedCart c = new ParkedCart();
        c.setId(Ids.next());
        c.setStoreId(storeId);
        c.setRegisterId(r.getId());
        c.setClientId(clientId);
        c.setLabel(Codes.cut(in.label() == null ? "" : in.label(), 80));
        c.setCustomerId(Codes.cut(in.customerId() == null ? "" : in.customerId(), 32));
        c.setStaffUsername(Codes.cut(in.staffUsername() == null
                || in.staffUsername().length() == 0 ? username : in.staffUsername(), 190));
        c.setNote(Codes.cut(in.note() == null ? "" : in.note(), 500));
        c.setStatus(PARKED);
        long when = in.createdAt() > 0 && in.createdAt() <= System.currentTimeMillis() + 300000L
                ? in.createdAt() : System.currentTimeMillis();
        c.setCreatedAt(Instant.ofEpochMilli(when));
        parked.insert(c);
        for (int iter = 0; iter < in.lines().size(); iter++) {
            PosLineDto l = in.lines().get(iter);
            ParkedLine pl = new ParkedLine();
            pl.setId(Ids.next());
            pl.setStoreId(storeId);
            pl.setParkedId(c.getId());
            pl.setVariantId(l.variantId() == null ? "" : l.variantId());
            pl.setTitle(Codes.cut(l.title() == null ? "" : l.title(), 200));
            pl.setQuantity(l.quantity());
            pl.setUnitCents(l.unitCents());
            pl.setTaxable(l.taxable());
            pl.setOverridden(l.overridden());
            pl.setPosition(iter);
            parkedLines.insert(pl);
        }
        return c;
    }

    public ParkedCartDto resume(String username, String storeId, String registerId,
            String parkedId) throws IOException {
        return move(username, storeId, registerId, parkedId, RESUMED);
    }

    public ParkedCartDto discard(String username, String storeId, String registerId,
            String parkedId) throws IOException {
        return move(username, storeId, registerId, parkedId, DISCARDED);
    }

    /// Takes a parked cart out of the list, as RESUMED or DISCARDED. Asking
    /// for what already happened answers the cart; asking for the other
    /// outcome is 409.
    @Transactional(rollbackFor = IOException.class)
    private ParkedCartDto move(String username, String storeId, String registerId,
            String parkedId, String to) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Register r = pos.register(storeId, registerId);
        ParkedCart c = parkedId == null ? null : parked.findById(parkedId);
        if (c == null || !r.getId().equals(c.getRegisterId())) {
            c = parkedId == null ? null : parked.findByRegisterIdAndClientId(r.getId(), parkedId);
        }
        if (c == null || !storeId.equals(c.getStoreId())) {
            throw new ResponseStatusException(404, "No such parked cart");
        }
        if (PARKED.equals(c.getStatus())) {
            if (parked.move(c.getId(), PARKED, to, Instant.now()) == 1) {
                c.setStatus(to);
            } else {
                c = parked.findById(c.getId());
            }
        }
        if (!to.equals(c.getStatus())) {
            throw new ResponseStatusException(409, "That cart was " + c.getStatus());
        }
        return dto(c);
    }

    private ParkedCartDto dto(ParkedCart c) {
        List<ParkedLine> rows = parkedLines.findByParkedIdOrderByPositionAsc(c.getId());
        List<PosLineDto> lines = new ArrayList<PosLineDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ParkedLine l = rows.get(iter);
            lines.add(new PosLineDto(l.getVariantId(), l.getQuantity(), l.getUnitCents(),
                    l.getTitle(), l.isTaxable(), l.isOverridden()));
        }
        return new ParkedCartDto(c.getId(), c.getClientId(), c.getLabel(), c.getCustomerId(),
                c.getStaffUsername(), c.getNote(), lines, c.getStatus(),
                c.getCreatedAt().toEpochMilli());
    }

    // ----------------------------------------------------------------- grid

    @Transactional(readOnly = true)
    public PosGridDto grid(String username, String storeId, String locationId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        location(storeId, locationId);
        List<GridTile> rows = tiles.findByStoreIdAndLocationIdOrderByPositionAsc(storeId,
                locationId);
        List<GridTileDto> out = new ArrayList<GridTileDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            GridTile t = rows.get(iter);
            out.add(new GridTileDto(t.getPosition(), t.getKind(), t.getRef(), t.getLabel(),
                    t.getColor()));
        }
        return new PosGridDto(locationId, out);
    }

    /// Replaces the grid of a location: each tile is a product of the store,
    /// a collection of the store or one of [#ACTIONS].
    @Transactional(rollbackFor = IOException.class)
    public PosGridDto saveGrid(String username, String storeId, String locationId,
            PosGridDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        location(storeId, locationId);
        List<GridTileDto> wanted = in == null || in.tiles() == null
                ? new ArrayList<GridTileDto>() : in.tiles();
        if (wanted.size() > MAX_TILES) {
            throw new ResponseStatusException(400, "At most " + MAX_TILES + " tiles");
        }
        for (int iter = 0; iter < wanted.size(); iter++) {
            checkTile(storeId, wanted.get(iter));
        }
        tiles.deleteByStoreIdAndLocationId(storeId, locationId);
        for (int iter = 0; iter < wanted.size(); iter++) {
            GridTileDto w = wanted.get(iter);
            GridTile t = new GridTile();
            t.setId(Ids.next());
            t.setStoreId(storeId);
            t.setLocationId(locationId);
            t.setPosition(iter);
            t.setKind(w.kind());
            t.setRef(w.ref());
            t.setLabel(Codes.cut(w.label() == null ? "" : w.label(), 80));
            t.setColor(w.color() & 0xffffff);
            tiles.insert(t);
        }
        audit(storeId, username, "POS_GRID", locationId, wanted.size() + " tiles laid out");
        return grid(username, storeId, locationId);
    }

    private void checkTile(String storeId, GridTileDto t) {
        if (t == null || t.kind() == null || t.ref() == null || t.ref().length() > 64) {
            throw new ResponseStatusException(400, "A tile is a kind and what it opens");
        }
        if ("PRODUCT".equals(t.kind())) {
            Variant v = variants.findById(t.ref());
            if (v == null || !storeId.equals(v.getStoreId())) {
                throw new ResponseStatusException(400, "A tile names an item not the store's");
            }
        } else if ("COLLECTION".equals(t.kind())) {
            Collection c = collections.findById(t.ref());
            if (c == null || !storeId.equals(c.getStoreId())) {
                throw new ResponseStatusException(400, "A tile names a collection not the store's");
            }
        } else if ("ACTION".equals(t.kind())) {
            if (!StoreAccess.isRole(t.ref(), ACTIONS)) {
                throw new ResponseStatusException(400, "A tile names an action the register lacks");
            }
        } else {
            throw new ResponseStatusException(400, "A tile is a PRODUCT, COLLECTION or ACTION");
        }
    }

    private Location location(String storeId, String locationId) {
        Location l = locationId == null ? null : locations.findById(locationId);
        if (l == null || !storeId.equals(l.getStoreId())) {
            throw new ResponseStatusException(404, "No such location");
        }
        return l;
    }

    // ---------------------------------------------------------------- audit

    void audit(String storeId, String username, String action, String subject, String detail)
            throws IOException {
        AuditEvent e = new AuditEvent();
        e.setId(Ids.next());
        e.setStoreId(storeId);
        e.setUsername(username == null ? "" : username);
        e.setAction(action);
        e.setSubject(Codes.cut(subject == null ? "" : subject, 64));
        e.setDetail(Codes.cut(detail, 500));
        e.setCreatedAt(Instant.now());
        audits.insert(e);
    }
}
