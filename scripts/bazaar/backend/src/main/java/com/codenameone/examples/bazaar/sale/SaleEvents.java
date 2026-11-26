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
package com.codenameone.examples.bazaar.sale;

import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.DiscountRulesDto;
import com.codenameone.examples.bazaar.api.LiveSaleDto;
import com.codenameone.examples.bazaar.api.PromoEventDto;
import com.codenameone.examples.bazaar.api.RescheduleDto;
import com.codenameone.examples.bazaar.api.SaleEventDto;
import com.codenameone.examples.bazaar.api.SaleItemDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.cart.Quote;
import com.codenameone.examples.bazaar.domain.Campaign;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.SaleClaim;
import com.codenameone.examples.bazaar.domain.SaleEvent;
import com.codenameone.examples.bazaar.domain.SaleItem;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.marketing.Shoppers;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.Money;
import com.codenameone.examples.bazaar.repository.CampaignRepository;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.SaleClaimRepository;
import com.codenameone.examples.bazaar.repository.SaleEventRepository;
import com.codenameone.examples.bazaar.repository.SaleItemRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Scheduled price changes and flash sales, and the promotions calendar.
///
/// A sale changes the variants' own prices while it runs: each item keeps a
/// snapshot of the price and compare-at price it replaced, the variant is
/// set to the sale price with the old price as its compare-at, and the
/// snapshot is put back when the sale ends. Every step is a conditional
/// state move (sale `SCHEDULED -> RUNNING -> ENDED`, item `PENDING ->
/// APPLIED -> REVERTED`), and the job finishes any step left half done, so
/// a restart in the middle of a sale neither applies a price twice nor
/// forgets to revert one. A variant the merchant repriced during the sale is
/// left at the merchant's price.
///
/// A flash sale caps the units sold at the sale price: each order claims its
/// units inside the transaction that writes it (`SaleEvent.sellIf` is one
/// conditional update), and a cancelled order gives them back.
@Component
public class SaleEvents {
    public static final String SCHEDULED = "SCHEDULED";
    public static final String RUNNING = "RUNNING";
    public static final String ENDED = "ENDED";
    public static final String CANCELLED = "CANCELLED";
    public static final String PENDING = "PENDING";
    public static final String APPLIED = "APPLIED";
    public static final String REVERTED = "REVERTED";
    public static final String SALE = "SALE";
    public static final String FLASH = "FLASH";
    public static final String PERCENT = "PERCENT";
    public static final String AMOUNT_OFF = "AMOUNT_OFF";
    public static final String FIXED_PRICE = "FIXED_PRICE";
    static final int MAX_ITEMS = 500;
    static final long MAX_DAYS = 366;
    static final long DAY = 24L * 3600 * 1000;

    private final SaleEventRepository sales;
    private final SaleItemRepository items;
    private final SaleClaimRepository claims;
    private final VariantRepository variants;
    private final DiscountRepository discounts;
    private final CampaignRepository campaigns;
    private final StoreAccess access;
    private final Shoppers shoppers;

    public SaleEvents(SaleEventRepository sales, SaleItemRepository items, SaleClaimRepository claims,
            VariantRepository variants, DiscountRepository discounts, CampaignRepository campaigns,
            StoreAccess access, Shoppers shoppers) {
        this.sales = sales;
        this.items = items;
        this.claims = claims;
        this.variants = variants;
        this.discounts = discounts;
        this.campaigns = campaigns;
        this.access = access;
        this.shoppers = shoppers;
    }

    // ----------------------------------------------------------------- math

    /// The price `mode` and `amount` make of `cents`, never below 0 nor above
    /// the price itself. Percent is in basis points, rounded half even.
    public static long salePrice(long cents, String mode, long amount) {
        long out;
        if (PERCENT.equals(mode)) {
            out = cents - Money.basisPoints(cents, Math.min(10000, Math.max(0, amount)));
        } else if (AMOUNT_OFF.equals(mode)) {
            out = cents - amount;
        } else {
            out = amount;
        }
        return Math.max(0, Math.min(cents, out));
    }

    /// Whether two windows overlap; an end is exclusive.
    public static boolean overlaps(long aStart, long aEnd, long bStart, long bEnd) {
        return aStart < bEnd && bStart < aEnd;
    }

    // -------------------------------------------------------------- merchant

    @Transactional(readOnly = true)
    public List<SaleEventDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<SaleEvent> rows = sales.findByStoreIdOrderByStartsAtAsc(storeId);
        List<SaleEventDto> out = new ArrayList<SaleEventDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public SaleEventDto get(String username, String storeId, String saleId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(sale(storeId, saleId));
    }

    SaleEventDto dto(SaleEvent s) {
        List<SaleItem> rows = items.findBySaleId(s.getId());
        List<SaleItemDto> out = new ArrayList<SaleItemDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            SaleItem i = rows.get(iter);
            out.add(new SaleItemDto(i.getVariantId(), i.getSaleCents(), i.getOriginalCents(),
                    i.getState()));
        }
        return new SaleEventDto(s.getId(), s.getTitle(), s.getKind(), s.getMode(), s.getAmount(),
                s.getStartsAt().toEpochMilli(), s.getEndsAt().toEpochMilli(), s.getStatus(),
                s.getStockCap(), s.getSold(), s.getColour(), out);
    }

    SaleEvent sale(String storeId, String saleId) {
        SaleEvent s = saleId == null ? null : sales.findById(saleId);
        if (s == null || !storeId.equals(s.getStoreId())) {
            throw new ResponseStatusException(404, "No such sale");
        }
        return s;
    }

    @Transactional(rollbackFor = IOException.class)
    public SaleEventDto create(String username, String storeId, SaleEventDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return insert(storeId, in, Instant.now());
    }

    /// Schedules a sale over the variants `in.items` names. The sale price of
    /// each is worked out now from its current price, unless the item gives
    /// one. Refused when a variant is already in a sale that overlaps.
    public SaleEventDto insert(String storeId, SaleEventDto in, Instant now) throws IOException {
        if (in == null || Codes.cut(in.title(), 120).length() == 0) {
            throw new ResponseStatusException(400, "Name the sale");
        }
        String kind = FLASH.equals(in.kind()) ? FLASH : SALE;
        String mode = PERCENT.equals(in.mode()) || AMOUNT_OFF.equals(in.mode())
                || FIXED_PRICE.equals(in.mode()) ? in.mode() : null;
        if (mode == null || in.amount() < 0 || PERCENT.equals(mode) && in.amount() > 10000) {
            throw new ResponseStatusException(400, "Choose how much the sale takes off");
        }
        window(in.startsAt(), in.endsAt(), now);
        if (in.stockCap() < 0 || in.stockCap() > 1000000) {
            throw new ResponseStatusException(400, "That stock cap is out of range");
        }
        List<SaleItemDto> wanted = in.items() == null ? new ArrayList<SaleItemDto>() : in.items();
        if (wanted.isEmpty() || wanted.size() > MAX_ITEMS) {
            throw new ResponseStatusException(400, "Choose between 1 and " + MAX_ITEMS + " variants");
        }
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < wanted.size(); iter++) {
            if (wanted.get(iter) == null || wanted.get(iter).variantId() == null
                    || ids.contains(wanted.get(iter).variantId())) {
                throw new ResponseStatusException(400, "Each variant once");
            }
            ids.add(wanted.get(iter).variantId());
        }
        List<Variant> found = variants.findByIdIn(ids);
        Map<String, Variant> byId = new HashMap<String, Variant>();
        for (int iter = 0; iter < found.size(); iter++) {
            if (storeId.equals(found.get(iter).getStoreId())) {
                byId.put(found.get(iter).getId(), found.get(iter));
            }
        }
        if (byId.size() != ids.size()) {
            throw new ResponseStatusException(404, "No such variant");
        }
        String clash = clash(ids, null, in.startsAt(), in.endsAt());
        if (clash != null) {
            throw new ResponseStatusException(409, "A variant is already in the sale " + clash);
        }
        SaleEvent s = new SaleEvent();
        s.setId(Ids.next());
        s.setStoreId(storeId);
        s.setTitle(Codes.cut(in.title(), 120));
        s.setKind(kind);
        s.setMode(mode);
        s.setAmount(in.amount());
        s.setStartsAt(Instant.ofEpochMilli(in.startsAt()));
        s.setEndsAt(Instant.ofEpochMilli(in.endsAt()));
        s.setStatus(SCHEDULED);
        s.setStockCap(FLASH.equals(kind) ? in.stockCap() : 0);
        s.setColour(Tokens.colour(in.colour(), FLASH.equals(kind) ? "#c0392b" : "#d35400"));
        s.setCreatedAt(now);
        sales.insert(s);
        for (int iter = 0; iter < wanted.size(); iter++) {
            SaleItemDto w = wanted.get(iter);
            Variant v = byId.get(w.variantId());
            SaleItem i = new SaleItem();
            i.setId(Ids.next());
            i.setStoreId(storeId);
            i.setSaleId(s.getId());
            i.setVariantId(v.getId());
            i.setSaleCents(w.saleCents() > 0 ? Math.min(w.saleCents(), v.getPriceCents())
                    : salePrice(v.getPriceCents(), mode, in.amount()));
            i.setState(PENDING);
            items.insert(i);
        }
        return dto(s);
    }

    private static void window(long startsAt, long endsAt, Instant now) {
        if (startsAt <= 0 || endsAt <= startsAt || endsAt - startsAt > MAX_DAYS * DAY) {
            throw new ResponseStatusException(400, "A sale needs a start before its end, within a year");
        }
        if (endsAt <= now.toEpochMilli()) {
            throw new ResponseStatusException(400, "That sale would already be over");
        }
    }

    /// The title of another live or scheduled sale that has one of `ids` in
    /// an overlapping window, or null.
    String clash(List<String> ids, String except, long startsAt, long endsAt) {
        List<SaleItem> others = items.findByVariantIdIn(ids);
        for (int iter = 0; iter < others.size(); iter++) {
            SaleItem o = others.get(iter);
            if (o.getSaleId().equals(except)) {
                continue;
            }
            SaleEvent s = sales.findById(o.getSaleId());
            if (s != null && (SCHEDULED.equals(s.getStatus()) || RUNNING.equals(s.getStatus()))
                    && overlaps(startsAt, endsAt, s.getStartsAt().toEpochMilli(),
                            s.getEndsAt().toEpochMilli())) {
                return s.getTitle();
            }
        }
        return null;
    }

    /// Cancels a scheduled sale, or ends a running one now (its prices are
    /// put back by the same step the job uses).
    public SaleEventDto cancel(String username, String storeId, String saleId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        SaleEvent s = sale(storeId, saleId);
        if (sales.move(s.getId(), SCHEDULED, CANCELLED) != 1 && sales.move(s.getId(), RUNNING, ENDED) == 1) {
            revertItems(s.getId());
        }
        return dto(sales.findById(s.getId()));
    }

    // ------------------------------------------------------------- the job

    @Scheduled(fixedDelay = 30000, initialDelay = 20000, lock = "bazaar-sale-events")
    public void job() {
        try {
            tick(Instant.now());
        } catch (IOException | RuntimeException failed) {
            System.err.println("sale events: " + failed);
        }
    }

    /// Starts sales whose time came, ends those whose time passed, and
    /// finishes any item step a restart interrupted. Answers how many item
    /// prices it changed; a second call at the same instant answers 0.
    public int tick(Instant now) throws IOException {
        int changed = 0;
        List<SaleEvent> due = sales.findByStatusAndStartsAtLessThanEqual(SCHEDULED, now);
        for (int iter = 0; iter < due.size(); iter++) {
            SaleEvent s = due.get(iter);
            if (s.getEndsAt().isAfter(now)) {
                sales.move(s.getId(), SCHEDULED, RUNNING);
            } else {
                sales.move(s.getId(), SCHEDULED, ENDED);
            }
        }
        List<SaleEvent> over = sales.findByStatusAndEndsAtLessThanEqual(RUNNING, now);
        for (int iter = 0; iter < over.size(); iter++) {
            sales.move(over.get(iter).getId(), RUNNING, ENDED);
        }
        List<SaleEvent> running = sales.findByStatus(RUNNING);
        for (int iter = 0; iter < running.size(); iter++) {
            changed += applyItems(running.get(iter).getId());
        }
        List<SaleEvent> ended = sales.findByStatus(ENDED);
        for (int iter = 0; iter < ended.size(); iter++) {
            changed += revertItems(ended.get(iter).getId());
        }
        return changed;
    }

    int applyItems(String saleId) throws IOException {
        int changed = 0;
        List<SaleItem> rows = items.findBySaleId(saleId);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (PENDING.equals(rows.get(iter).getState()) && applyItem(rows.get(iter).getId())) {
                changed++;
            }
        }
        return changed;
    }

    int revertItems(String saleId) throws IOException {
        int changed = 0;
        List<SaleItem> rows = items.findBySaleId(saleId);
        for (int iter = 0; iter < rows.size(); iter++) {
            String state = rows.get(iter).getState();
            if (APPLIED.equals(state) && revertItem(rows.get(iter).getId())) {
                changed++;
            } else if (PENDING.equals(state)) {
                // Never started: nothing to put back.
                items.move(rows.get(iter).getId(), PENDING, REVERTED);
            }
        }
        return changed;
    }

    /// Snapshots the variant's prices and sets the sale price, in one
    /// transaction with the item's move; a second call finds it APPLIED.
    @Transactional(rollbackFor = IOException.class)
    boolean applyItem(String itemId) {
        SaleItem i = items.findById(itemId);
        if (i == null || items.move(i.getId(), PENDING, APPLIED) != 1) {
            return false;
        }
        Variant v = variants.findById(i.getVariantId());
        if (v == null) {
            return false;
        }
        i.setOriginalCents(v.getPriceCents());
        i.setOriginalCompareCents(v.getCompareAtCents());
        i.setState(APPLIED);
        items.save(i);
        if (i.getSaleCents() < v.getPriceCents()) {
            // The "was" price shown beside it is the higher of the price it
            // replaced and a compare-at the merchant had already set.
            v.setCompareAtCents(Math.max(v.getPriceCents(), v.getCompareAtCents()));
            v.setPriceCents(i.getSaleCents());
            variants.save(v);
        }
        return true;
    }

    /// Puts the snapshot back, unless the merchant changed the price while
    /// the sale ran, in which case their price stands.
    @Transactional(rollbackFor = IOException.class)
    boolean revertItem(String itemId) {
        SaleItem i = items.findById(itemId);
        if (i == null || items.move(i.getId(), APPLIED, REVERTED) != 1) {
            return false;
        }
        Variant v = variants.findById(i.getVariantId());
        if (v != null && v.getPriceCents() == i.getSaleCents()
                && i.getSaleCents() < i.getOriginalCents()) {
            v.setPriceCents(i.getOriginalCents());
            v.setCompareAtCents(i.getOriginalCompareCents());
            variants.save(v);
        }
        return true;
    }

    // -------------------------------------------------------------- flash

    /// Claims an order's units of every running capped sale it buys from,
    /// inside the transaction that writes the order: a cap that would be
    /// passed answers 409 and the whole order is rolled back.
    public void claim(StoreOrder order, Quote quote) throws IOException {
        Map<String, Integer> qty = new HashMap<String, Integer>();
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Quote.Line l = quote.lines.get(iter);
            Integer was = qty.get(l.variant.getId());
            qty.put(l.variant.getId(), (was == null ? 0 : was.intValue()) + l.price.quantity);
            if (was == null) {
                ids.add(l.variant.getId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<String, Integer> bySale = new HashMap<String, Integer>();
        List<SaleItem> rows = items.findByVariantIdIn(ids);
        for (int iter = 0; iter < rows.size(); iter++) {
            SaleItem i = rows.get(iter);
            if (!APPLIED.equals(i.getState())) {
                continue;
            }
            Integer was = bySale.get(i.getSaleId());
            bySale.put(i.getSaleId(), (was == null ? 0 : was.intValue())
                    + qty.get(i.getVariantId()).intValue());
        }
        for (Map.Entry<String, Integer> e : bySale.entrySet()) {
            SaleEvent s = sales.findById(e.getKey());
            if (s == null || !RUNNING.equals(s.getStatus()) || s.getStockCap() <= 0) {
                continue;
            }
            int n = e.getValue().intValue();
            if (sales.sellIf(s.getId(), n) != 1) {
                throw new ResponseStatusException(409, s.getTitle() + " has sold out");
            }
            SaleClaim c = new SaleClaim();
            c.setId(Ids.next());
            c.setStoreId(s.getStoreId());
            c.setSaleId(s.getId());
            c.setOrderId(order.getId());
            c.setQty(n);
            claims.insert(c);
        }
    }

    /// Gives a cancelled order's flash-sale units back; once per claim.
    @Transactional(rollbackFor = IOException.class)
    public int release(String orderId) {
        int back = 0;
        List<SaleClaim> rows = claims.findByOrderId(orderId);
        for (int iter = 0; iter < rows.size(); iter++) {
            SaleClaim c = rows.get(iter);
            if (claims.release(c.getId()) == 1) {
                sales.unsell(c.getSaleId(), c.getQty());
                back += c.getQty();
            }
        }
        return back;
    }

    // ---------------------------------------------------------- storefront

    /// Running sales for the storefront banner and countdown.
    @Transactional(readOnly = true)
    public List<LiveSaleDto> live(String storeId, Instant now) {
        shoppers.store(storeId);
        List<SaleEvent> rows = sales.findByStoreIdAndStatus(storeId, RUNNING);
        List<LiveSaleDto> out = new ArrayList<LiveSaleDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            SaleEvent s = rows.get(iter);
            List<String> ids = new ArrayList<String>();
            List<SaleItem> in = items.findBySaleId(s.getId());
            for (int i = 0; i < in.size(); i++) {
                if (APPLIED.equals(in.get(i).getState())) {
                    ids.add(in.get(i).getVariantId());
                }
            }
            long left = Math.max(0, (s.getEndsAt().toEpochMilli() - now.toEpochMilli()) / 1000);
            int remaining = s.getStockCap() <= 0 ? -1 : Math.max(0, s.getStockCap() - s.getSold());
            out.add(new LiveSaleDto(s.getId(), s.getTitle(), s.getKind(), s.getEndsAt().toEpochMilli(),
                    left, remaining, ids));
        }
        return out;
    }

    // ------------------------------------------------------------ calendar

    /// Sales, dated discounts and scheduled campaigns that touch the window.
    @Transactional(readOnly = true)
    public List<PromoEventDto> calendar(String username, String storeId, String from, String to)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        long a = millis(from, 0);
        long b = millis(to, Long.MAX_VALUE);
        List<PromoEventDto> out = new ArrayList<PromoEventDto>();
        List<SaleEvent> rows = sales.findByStoreIdOrderByStartsAtAsc(storeId);
        for (int iter = 0; iter < rows.size(); iter++) {
            SaleEvent s = rows.get(iter);
            if (!CANCELLED.equals(s.getStatus()) && overlaps(a, b, s.getStartsAt().toEpochMilli(),
                    s.getEndsAt().toEpochMilli())) {
                out.add(new PromoEventDto(SALE, s.getId(), s.getTitle(), s.getStartsAt().toEpochMilli(),
                        s.getEndsAt().toEpochMilli(), s.getColour(), s.getStatus()));
            }
        }
        List<Discount> ds = discounts.findByStoreIdOrderByCreatedAtDesc(storeId);
        for (int iter = 0; iter < ds.size(); iter++) {
            Discount d = ds.get(iter);
            long s = d.getStartsAt() == null ? 0 : d.getStartsAt().toEpochMilli();
            long e = d.getEndsAt() == null ? 0 : d.getEndsAt().toEpochMilli();
            if (!d.isActive() || s <= 0 || e <= 0 || d.getEligibility().length() > 0
                    || d.getUsageLimit() == 1) {
                // Undated, switched off, a member perk or a points reward:
                // not a promotion anyone plans on a calendar.
                continue;
            }
            if (overlaps(a, b, s, e)) {
                out.add(new PromoEventDto("DISCOUNT", d.getId(), d.getTitle(), s, e, "#2e7d32",
                        "ACTIVE"));
            }
        }
        List<Campaign> cs = campaigns.findByStoreIdOrderByCreatedAtDesc(storeId);
        for (int iter = 0; iter < cs.size(); iter++) {
            Campaign c = cs.get(iter);
            long at = c.getScheduledAt().toEpochMilli();
            if (at > 0 && at >= a && at < b && !"CANCELLED".equals(c.getStatus())) {
                out.add(new PromoEventDto("CAMPAIGN", c.getId(), c.getName(), at, at + 3600000L,
                        c.getColour(), c.getStatus()));
            }
        }
        return out;
    }

    private static long millis(String text, long fallback) {
        if (text == null || text.length() == 0) {
            return fallback;
        }
        long out = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9' || iter > 15) {
                throw new ResponseStatusException(400, "Dates are milliseconds");
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }

    /// Moves a sale, a discount or a campaign on the calendar. A sale moves
    /// only while scheduled, and not onto another sale of one of its variants.
    @Transactional(rollbackFor = IOException.class)
    public PromoEventDto reschedule(String username, String storeId, RescheduleDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null || in.id() == null) {
            throw new ResponseStatusException(400, "Choose what to move");
        }
        Instant now = Instant.now();
        if (SALE.equals(in.kind())) {
            SaleEvent s = sale(storeId, in.id());
            if (!SCHEDULED.equals(s.getStatus())) {
                throw new ResponseStatusException(409, "Only a sale that has not started can move");
            }
            window(in.startsAt(), in.endsAt(), now);
            List<SaleItem> its = items.findBySaleId(s.getId());
            List<String> ids = new ArrayList<String>();
            for (int iter = 0; iter < its.size(); iter++) {
                ids.add(its.get(iter).getVariantId());
            }
            String clash = clash(ids, s.getId(), in.startsAt(), in.endsAt());
            if (clash != null) {
                throw new ResponseStatusException(409, "A variant is already in the sale " + clash);
            }
            s.setStartsAt(Instant.ofEpochMilli(in.startsAt()));
            s.setEndsAt(Instant.ofEpochMilli(in.endsAt()));
            sales.save(s);
            return new PromoEventDto(SALE, s.getId(), s.getTitle(), in.startsAt(), in.endsAt(),
                    s.getColour(), s.getStatus());
        }
        if ("DISCOUNT".equals(in.kind())) {
            Discount d = discounts.findById(in.id());
            if (d == null || !storeId.equals(d.getStoreId())) {
                throw new ResponseStatusException(404, "No such discount");
            }
            if (in.startsAt() <= 0 || in.endsAt() <= in.startsAt()) {
                throw new ResponseStatusException(400, "A start before the end");
            }
            d.setStartsAt(Instant.ofEpochMilli(in.startsAt()));
            d.setEndsAt(Instant.ofEpochMilli(in.endsAt()));
            discounts.save(d);
            return new PromoEventDto("DISCOUNT", d.getId(), d.getTitle(), in.startsAt(), in.endsAt(),
                    "#2e7d32", "ACTIVE");
        }
        if ("CAMPAIGN".equals(in.kind())) {
            Campaign c = campaigns.findById(in.id());
            if (c == null || !storeId.equals(c.getStoreId())) {
                throw new ResponseStatusException(404, "No such campaign");
            }
            if (!"SCHEDULED".equals(c.getStatus()) && !"DRAFT".equals(c.getStatus())) {
                throw new ResponseStatusException(409, "That campaign has already gone out");
            }
            if (in.startsAt() <= now.toEpochMilli()) {
                throw new ResponseStatusException(400, "Schedule it in the future");
            }
            c.setScheduledAt(Instant.ofEpochMilli(in.startsAt()));
            c.setStatus("SCHEDULED");
            campaigns.save(c);
            return new PromoEventDto("CAMPAIGN", c.getId(), c.getName(), in.startsAt(),
                    in.startsAt() + 3600000L, c.getColour(), c.getStatus());
        }
        throw new ResponseStatusException(400, "Unknown kind");
    }

    // ------------------------------------------------------ discount rules

    @Transactional(readOnly = true)
    public DiscountRulesDto rules(String username, String storeId, String discountId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return rulesDto(discount(storeId, discountId));
    }

    private Discount discount(String storeId, String discountId) {
        Discount d = discountId == null ? null : discounts.findById(discountId);
        if (d == null || !storeId.equals(d.getStoreId())) {
            throw new ResponseStatusException(404, "No such discount");
        }
        return d;
    }

    static DiscountRulesDto rulesDto(Discount d) {
        return new DiscountRulesDto(d.getDiscountClass(), d.isCombinesProduct(), d.isCombinesOrder(),
                d.isCombinesShipping(), d.getEligibility(), d.getRules());
    }

    /// Sets a discount's combination class and the rules of a tiered, volume
    /// or bundle discount. The rules must parse as JSON the engine reads.
    @Transactional(rollbackFor = IOException.class)
    public DiscountRulesDto saveRules(String username, String storeId, String discountId,
            DiscountRulesDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Discount d = discount(storeId, discountId);
        if (in == null) {
            throw new ResponseStatusException(400, "Describe the rules");
        }
        String k = in.discountClass() == null ? "" : in.discountClass();
        if (k.length() > 0 && !DiscountEngine.CLASS_PRODUCT.equals(k)
                && !DiscountEngine.CLASS_ORDER.equals(k) && !DiscountEngine.CLASS_SHIPPING.equals(k)) {
            throw new ResponseStatusException(400, "The class is PRODUCT, ORDER or SHIPPING");
        }
        String rules = in.rules() == null ? "" : in.rules().trim();
        if (rules.length() > 2000) {
            throw new ResponseStatusException(400, "Those rules are too long");
        }
        if (rules.length() > 0) {
            try {
                Json.parseObject(rules);
            } catch (IOException | RuntimeException bad) {
                throw new ResponseStatusException(400, "The rules are not valid JSON");
            }
        }
        String e = in.eligibility() == null ? "" : in.eligibility();
        if (e.length() > 0 && !e.startsWith("TIER:") && !e.startsWith("CLUB:") || e.length() > 80) {
            throw new ResponseStatusException(400, "Eligibility is TIER:<id> or CLUB:<id>");
        }
        d.setDiscountClass(k);
        d.setCombinesProduct(in.combinesProduct());
        d.setCombinesOrder(in.combinesOrder());
        d.setCombinesShipping(in.combinesShipping());
        d.setEligibility(e);
        d.setRules(rules);
        discounts.save(d);
        return rulesDto(d);
    }
}
