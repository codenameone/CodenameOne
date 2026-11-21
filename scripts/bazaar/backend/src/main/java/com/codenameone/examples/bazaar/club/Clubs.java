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
package com.codenameone.examples.bazaar.club;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.ClubDto;
import com.codenameone.examples.bazaar.api.ClubItemDto;
import com.codenameone.examples.bazaar.api.ClubMembershipDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Club;
import com.codenameone.examples.bazaar.domain.ClubItem;
import com.codenameone.examples.bazaar.domain.ClubMember;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.marketing.Shoppers;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.repository.ClubItemRepository;
import com.codenameone.examples.bazaar.repository.ClubMemberRepository;
import com.codenameone.examples.bazaar.repository.ClubRepository;
import com.codenameone.examples.bazaar.repository.CollectionRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Paid memberships. Joining charges the fee through [Payments] and sets the
/// renewal date; a job renews members when it falls due. Membership opens
/// member-only products and collections, early access before a public
/// launch, and the club's member price, an automatic discount whose
/// eligibility is `CLUB:<id>` (see `Memberships`).
///
/// A charge id is derived from the member and the period it pays for, and
/// [Payments#authorize] answers the hold already made for an id it has seen,
/// so a renewal run twice for one period charges once.
@Component
public class Clubs {
    public static final String ACTIVE = "ACTIVE";
    /// Paid up to the renewal date and not renewing.
    public static final String CANCELLED = "CANCELLED";
    public static final String LAPSED = "LAPSED";
    static final long DAY = 24L * 3600 * 1000;
    /// Failed renewals before a membership lapses; one attempt per job pass.
    static final int MAX_FAILURES = 3;

    private final ClubRepository clubs;
    private final ClubMemberRepository members;
    private final ClubItemRepository items;
    private final ProductRepository products;
    private final CollectionRepository collections;
    private final Payments payments;
    private final StoreAccess access;
    private final Shoppers shoppers;
    private final Loyalty loyalty;

    public Clubs(ClubRepository clubs, ClubMemberRepository members, ClubItemRepository items,
            ProductRepository products, CollectionRepository collections, Payments payments,
            StoreAccess access, Shoppers shoppers, Loyalty loyalty) {
        this.clubs = clubs;
        this.members = members;
        this.items = items;
        this.products = products;
        this.collections = collections;
        this.payments = payments;
        this.access = access;
        this.shoppers = shoppers;
        this.loyalty = loyalty;
    }

    // -------------------------------------------------------------- merchant

    @Transactional(readOnly = true)
    public List<ClubDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dtos(storeId, false);
    }

    @Transactional(readOnly = true)
    public List<ClubDto> storeClubs(String storeId) {
        shoppers.store(storeId);
        return dtos(storeId, true);
    }

    private List<ClubDto> dtos(String storeId, boolean activeOnly) {
        List<Club> rows = clubs.findByStoreIdOrderByCreatedAtAsc(storeId);
        List<ClubDto> out = new ArrayList<ClubDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            if (!activeOnly || rows.get(iter).isActive()) {
                out.add(dto(rows.get(iter)));
            }
        }
        return out;
    }

    ClubDto dto(Club c) {
        return new ClubDto(c.getId(), c.getName(), c.getDescription(), c.getFeeCents(),
                c.getPeriodDays(), c.getMemberBps(), c.getEarlyAccessHours(), c.getColour(),
                c.isActive(), members.countByClubIdAndStatus(c.getId(), ACTIVE));
    }

    @Transactional(rollbackFor = IOException.class)
    public ClubDto save(String username, String storeId, String clubId, ClubDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return write(storeId, clubId, in);
    }

    /// Creates the club (`clubId` null) or updates it, and its member price.
    public ClubDto write(String storeId, String clubId, ClubDto in) throws IOException {
        if (in == null || Codes.cut(in.name(), 80).length() == 0) {
            throw new ResponseStatusException(400, "Name the club");
        }
        if (in.feeCents() < 0 || in.feeCents() > 10000000 || in.periodDays() < 1
                || in.periodDays() > 366 || in.memberBps() < 0 || in.memberBps() > 9000
                || in.earlyAccessHours() < 0 || in.earlyAccessHours() > 720) {
            throw new ResponseStatusException(400, "Those club numbers are out of range");
        }
        Club c = clubId == null ? null : club(storeId, clubId);
        boolean fresh = c == null;
        if (fresh) {
            c = new Club();
            c.setId(Ids.next());
            c.setStoreId(storeId);
            c.setCreatedAt(Instant.now());
        }
        c.setName(Codes.cut(in.name(), 80));
        c.setDescription(Codes.cut(in.description(), 500));
        c.setFeeCents(in.feeCents());
        c.setPeriodDays(in.periodDays());
        c.setMemberBps(in.memberBps());
        c.setEarlyAccessHours(in.earlyAccessHours());
        c.setColour(Tokens.colour(in.colour(), "#5b4b8a"));
        c.setActive(in.active());
        c.setDiscountId(loyalty.perk(storeId, c.getDiscountId(), c.getName() + " member price",
                DiscountEngine.PERCENT, c.getMemberBps(), "CLUB:" + c.getId(),
                c.isActive() && c.getMemberBps() > 0));
        if (fresh) {
            clubs.insert(c);
        } else {
            clubs.save(c);
        }
        return dto(c);
    }

    Club club(String storeId, String clubId) {
        Club c = clubId == null ? null : clubs.findById(clubId);
        if (c == null || !storeId.equals(c.getStoreId())) {
            throw new ResponseStatusException(404, "No such club");
        }
        return c;
    }

    @Transactional(readOnly = true)
    public List<ClubMembershipDto> members(String username, String storeId, String clubId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Club c = club(storeId, clubId);
        List<ClubMember> rows = members.findByClubIdOrderByJoinedAtDesc(c.getId());
        List<ClubMembershipDto> out = new ArrayList<ClubMembershipDto>();
        for (int iter = 0; iter < rows.size() && iter < 500; iter++) {
            out.add(dto(rows.get(iter), c));
        }
        return out;
    }

    static ClubMembershipDto dto(ClubMember m, Club c) {
        return new ClubMembershipDto(m.getId(), m.getClubId(), c == null ? "" : c.getName(),
                m.getUsername(), m.getStatus(), m.getJoinedAt().toEpochMilli(),
                m.getRenewsAt().toEpochMilli());
    }

    @Transactional(readOnly = true)
    public List<ClubItemDto> items(String username, String storeId, String clubId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return itemDtos(club(storeId, clubId).getId());
    }

    private List<ClubItemDto> itemDtos(String clubId) {
        List<ClubItem> rows = items.findByClubId(clubId);
        List<ClubItemDto> out = new ArrayList<ClubItemDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ClubItem i = rows.get(iter);
            out.add(new ClubItemDto(i.getId(), i.getProductId(), i.getCollectionId(),
                    i.isMembersOnly(), i.getMembersFrom().toEpochMilli(),
                    i.getPublicFrom().toEpochMilli()));
        }
        return out;
    }

    /// Puts a product or a collection behind the club: members only, or on
    /// sale to members from `membersFrom` and to everyone from `publicFrom`.
    @Transactional(rollbackFor = IOException.class)
    public List<ClubItemDto> addItem(String username, String storeId, String clubId, ClubItemDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Club c = club(storeId, clubId);
        return add(storeId, c.getId(), in);
    }

    public List<ClubItemDto> add(String storeId, String clubId, ClubItemDto in) throws IOException {
        if (in == null) {
            throw new ResponseStatusException(400, "Choose a product or a collection");
        }
        String productId = in.productId() == null ? "" : in.productId();
        String collectionId = in.collectionId() == null ? "" : in.collectionId();
        if (productId.length() > 0 == collectionId.length() > 0) {
            throw new ResponseStatusException(400, "Choose a product or a collection, not both");
        }
        if (productId.length() > 0) {
            com.codenameone.examples.bazaar.domain.Product p = products.findById(productId);
            if (p == null || !storeId.equals(p.getStoreId())) {
                throw new ResponseStatusException(404, "No such product");
            }
        } else {
            com.codenameone.examples.bazaar.domain.Collection k = collections.findById(collectionId);
            if (k == null || !storeId.equals(k.getStoreId())) {
                throw new ResponseStatusException(404, "No such collection");
            }
        }
        if (in.publicFrom() > 0 && in.membersFrom() > in.publicFrom()) {
            throw new ResponseStatusException(400, "Members go first, before the public launch");
        }
        if (!in.membersOnly() && in.publicFrom() <= 0) {
            throw new ResponseStatusException(400, "Make it members only or set a launch time");
        }
        ClubItem i = new ClubItem();
        i.setId(Ids.next());
        i.setStoreId(storeId);
        i.setClubId(clubId);
        i.setProductId(productId);
        i.setCollectionId(collectionId);
        i.setMembersOnly(in.membersOnly());
        i.setMembersFrom(Tokens.at(in.membersFrom()));
        i.setPublicFrom(Tokens.at(in.publicFrom()));
        items.insert(i);
        return itemDtos(clubId);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<ClubItemDto> removeItem(String username, String storeId, String clubId,
            String itemId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Club c = club(storeId, clubId);
        ClubItem i = itemId == null ? null : items.findById(itemId);
        if (i == null || !c.getId().equals(i.getClubId())) {
            throw new ResponseStatusException(404, "No such club item");
        }
        items.delete(i);
        return itemDtos(c.getId());
    }

    // --------------------------------------------------------------- shopper

    public List<ClubMembershipDto> mine(String username, String storeId) throws IOException {
        shoppers.store(storeId);
        if (username == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        List<ClubMember> rows = members.findByStoreIdAndUsername(storeId, username);
        List<ClubMembershipDto> out = new ArrayList<ClubMembershipDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter), clubs.findById(rows.get(iter).getClubId())));
        }
        return out;
    }

    /// Joins a club and pays the first period. The charge happens with no
    /// transaction open, as every card call does; the membership is written
    /// once it went through. Joining again while a membership is current
    /// only turns renewal back on.
    public ClubMembershipDto join(String username, String storeId, String clubId, String methodId)
            throws IOException {
        Store s = shoppers.store(storeId);
        Club c = club(storeId, clubId);
        if (!c.isActive()) {
            throw new ResponseStatusException(409, "The club is not taking members");
        }
        Customer customer = shoppers.customer(storeId, username);
        ClubMember m = members.findByClubIdAndUsername(c.getId(), username);
        Instant now = Instant.now();
        if (m != null && !LAPSED.equals(m.getStatus()) && m.getRenewsAt().isAfter(now)) {
            return resume(m.getId(), c);
        }
        String method = payments.choose(username, methodId)[0];
        if (Payments.CASH.equals(method)) {
            throw new ResponseStatusException(400, "A membership needs a card");
        }
        String charge = Ids.next();
        if (c.getFeeCents() > 0) {
            payments.authorize(charge, username, method, c.getFeeCents(), s.getCurrency());
            String status = payments.capture(charge);
            if (!Payments.PAID.equals(status)) {
                throw new ResponseStatusException(402, "The card was refused");
            }
        }
        return enrol(m == null ? null : m.getId(), c, customer, method, charge, now);
    }

    @Transactional(rollbackFor = IOException.class)
    ClubMembershipDto resume(String memberId, Club c) {
        ClubMember m = members.findById(memberId);
        m.setStatus(ACTIVE);
        members.save(m);
        return dto(m, c);
    }

    @Transactional(rollbackFor = IOException.class)
    ClubMembershipDto enrol(String memberId, Club c, Customer customer, String method, String charge,
            Instant now) throws IOException {
        ClubMember m = memberId == null ? null : members.findById(memberId);
        boolean fresh = m == null;
        if (fresh) {
            m = new ClubMember();
            m.setId(Ids.next());
            m.setStoreId(c.getStoreId());
            m.setClubId(c.getId());
            m.setCustomerId(customer.getId());
            m.setUsername(customer.getUsername());
        }
        m.setStatus(ACTIVE);
        m.setPaymentMethodId(method);
        m.setJoinedAt(now);
        m.setRenewsAt(now.plusMillis(c.getPeriodDays() * DAY));
        m.setLastChargeId(charge);
        m.setFailures(0);
        if (fresh) {
            members.insert(m);
        } else {
            members.save(m);
        }
        return dto(m, c);
    }

    /// Stops renewal; the member keeps the benefits until the renewal date.
    @Transactional(rollbackFor = IOException.class)
    public ClubMembershipDto leave(String username, String storeId, String clubId) {
        shoppers.store(storeId);
        Club c = club(storeId, clubId);
        ClubMember m = username == null ? null : members.findByClubIdAndUsername(c.getId(), username);
        if (m == null) {
            throw new ResponseStatusException(404, "You are not a member");
        }
        if (ACTIVE.equals(m.getStatus())) {
            m.setStatus(CANCELLED);
            members.save(m);
        }
        return dto(m, c);
    }

    // ----------------------------------------------------------------- jobs

    @Scheduled(fixedDelay = 900000, initialDelay = 120000, lock = "bazaar-club-renewals")
    public void renewJob() {
        try {
            tick(Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("club renewals: " + failed);
        }
    }

    /// Renews every membership whose date has come, and lapses cancelled
    /// ones. Answers how many it renewed.
    public int tick(Instant now) throws IOException {
        int renewed = 0;
        List<ClubMember> due = members.findByStatusAndRenewsAtLessThanEqual(ACTIVE, now);
        for (int iter = 0; iter < due.size(); iter++) {
            if (renew(due.get(iter).getId(), now)) {
                renewed++;
            }
        }
        List<ClubMember> ended = members.findByStatusAndRenewsAtLessThanEqual(CANCELLED, now);
        for (int iter = 0; iter < ended.size(); iter++) {
            finish(ended.get(iter).getId(), LAPSED);
        }
        return renewed;
    }

    boolean renew(String memberId, Instant now) throws IOException {
        ClubMember m = members.findById(memberId);
        Club c = m == null ? null : clubs.findById(m.getClubId());
        if (m == null || c == null || !ACTIVE.equals(m.getStatus()) || m.getRenewsAt().isAfter(now)) {
            return false;
        }
        if (!c.isActive()) {
            finish(m.getId(), LAPSED);
            return false;
        }
        Instant due = m.getRenewsAt();
        // One id per member, period and attempt: a pass run twice charges
        // once, and a refused card is tried afresh on the next pass.
        String charge = Ids.sha256(m.getId() + ":" + due.toEpochMilli() + ":" + m.getFailures()).substring(0, 32);
        if (members.claimRenewal(m.getId(), due, charge) != 1) {
            return false;
        }
        String status;
        try {
            if (c.getFeeCents() > 0) {
                payments.authorize(charge, m.getUsername(), m.getPaymentMethodId(), c.getFeeCents(),
                        shoppers.store(c.getStoreId()).getCurrency());
                status = payments.capture(charge);
            } else {
                status = Payments.PAID;
            }
        } catch (ResponseStatusException refused) {
            status = Payments.FAILED;
        }
        return settle(m.getId(), due, c.getPeriodDays(), Payments.PAID.equals(status));
    }

    @Transactional(rollbackFor = IOException.class)
    boolean settle(String memberId, Instant due, int periodDays, boolean paid) {
        ClubMember m = members.findById(memberId);
        if (m == null || !due.equals(m.getRenewsAt())) {
            return false;
        }
        if (paid) {
            m.setRenewsAt(due.plusMillis(periodDays * DAY));
            m.setFailures(0);
        } else {
            m.setFailures(m.getFailures() + 1);
            if (m.getFailures() >= MAX_FAILURES) {
                m.setStatus(LAPSED);
            }
        }
        members.save(m);
        return paid;
    }

    @Transactional(rollbackFor = IOException.class)
    void finish(String memberId, String status) {
        ClubMember m = members.findById(memberId);
        if (m != null) {
            m.setStatus(status);
            members.save(m);
        }
    }
}
