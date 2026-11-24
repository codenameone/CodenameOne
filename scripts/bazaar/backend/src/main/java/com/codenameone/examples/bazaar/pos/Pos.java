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

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CashMoveDto;
import com.codenameone.examples.bazaar.api.CloseShiftDto;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.OpenShiftDto;
import com.codenameone.examples.bazaar.api.PinSignInDto;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosCustomerDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosReceiptDto;
import com.codenameone.examples.bazaar.api.PosRegisterDto;
import com.codenameone.examples.bazaar.api.PosReturnDto;
import com.codenameone.examples.bazaar.api.PosReturnResultDto;
import com.codenameone.examples.bazaar.api.PosSessionDto;
import com.codenameone.examples.bazaar.api.ReceiptSendDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.ShiftDto;
import com.codenameone.examples.bazaar.api.TaxRateDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.docs.Documents;
import com.codenameone.examples.bazaar.domain.CashMove;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.Shift;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.TaxRate;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.notify.Templates;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.rates.RateSettings;
import com.codenameone.examples.bazaar.repository.CashMoveRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.RegisterRepository;
import com.codenameone.examples.bazaar.repository.ShiftRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.repository.TaxRateRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.sms.SmsSender;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The point of sale around the sales themselves ([PosSales]): registers,
/// cashier PINs, shifts and their cash, the offline catalog, customer lookup,
/// counter returns and receipts.
///
/// A register is a device signed in with a staff account; the PIN says which
/// member of the staff is at it now. Five wrong PINs in a row lock that PIN
/// for [#LOCK_MILLIS], whoever typed them.
@Component
public class Pos {
    /// The store's own opening line of a texted receipt.
    @com.codename1.backend.annotations.Autowired
    private Templates templates;

    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";
    /// Wrong PINs in a row before the PIN is locked.
    public static final int PIN_TRIES = 5;
    public static final long LOCK_MILLIS = 15L * 60L * 1000L;

    private final StoreAccess access;
    private final StoreRepository stores;
    private final RegisterRepository registers;
    private final ShiftRepository shifts;
    private final CashMoveRepository cashMoves;
    private final StoreStaffRepository staff;
    private final LocationRepository locations;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final InventoryLevelRepository levels;
    private final TaxRateRepository taxes;
    private final DiscountRepository discounts;
    private final CustomerRepository customers;
    private final StoreOrderRepository orderRows;
    private final Orders orders;
    private final Documents documents;
    private final PasswordEncoder encoder;
    private final SmsSender sms;
    private final Emails emails;
    private final com.codenameone.examples.bazaar.notify.ShopperLanguage language;

    private final com.codenameone.examples.bazaar.catalog.Bundles bundles;
    private final com.codenameone.examples.bazaar.catalog.Schedules schedules;

    public Pos(StoreAccess access, StoreRepository stores, RegisterRepository registers,
            ShiftRepository shifts, CashMoveRepository cashMoves, StoreStaffRepository staff,
            LocationRepository locations, ProductRepository products, VariantRepository variants,
            InventoryLevelRepository levels, TaxRateRepository taxes,
            DiscountRepository discounts, CustomerRepository customers,
            StoreOrderRepository orderRows, Orders orders, Documents documents,
            PasswordEncoder encoder, SmsSender sms, Emails emails,
            com.codenameone.examples.bazaar.notify.ShopperLanguage language,
            com.codenameone.examples.bazaar.catalog.Bundles bundles,
            com.codenameone.examples.bazaar.catalog.Schedules schedules) {
        this.bundles = bundles;
        this.schedules = schedules;
        this.language = language;
        this.emails = emails;
        this.access = access;
        this.stores = stores;
        this.registers = registers;
        this.shifts = shifts;
        this.cashMoves = cashMoves;
        this.staff = staff;
        this.locations = locations;
        this.products = products;
        this.variants = variants;
        this.levels = levels;
        this.taxes = taxes;
        this.discounts = discounts;
        this.customers = customers;
        this.orderRows = orderRows;
        this.orders = orders;
        this.documents = documents;
        this.encoder = encoder;
        this.sms = sms;
    }

    // ------------------------------------------------------------ registers

    @Transactional(readOnly = true)
    public List<PosRegisterDto> registers(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<Register> rows = registers.findByStoreId(storeId);
        List<PosRegisterDto> out = new ArrayList<PosRegisterDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    /// Adds a register. A register named no tax country takes the store's
    /// first tax rate's.
    @Transactional(rollbackFor = IOException.class)
    public PosRegisterDto addRegister(String username, String storeId, PosRegisterDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Describe the register");
        }
        Register r = new Register();
        r.setId(Ids.next());
        r.setStoreId(storeId);
        r.setActive(true);
        fill(r, wanted);
        if (r.getTaxCountry().length() == 0) {
            List<TaxRate> rates = taxes.findByStoreId(storeId);
            if (!rates.isEmpty()) {
                r.setTaxCountry(rates.get(0).getCountry());
                r.setTaxRegion(rates.get(0).getRegion());
            }
        }
        registers.insert(r);
        return dto(r);
    }

    @Transactional(rollbackFor = IOException.class)
    public PosRegisterDto saveRegister(String username, String storeId, String registerId,
            PosRegisterDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Register r = register(storeId, registerId);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Describe the register");
        }
        fill(r, wanted);
        r.setActive(wanted.active());
        registers.save(r);
        return dto(r);
    }

    /// Takes a register out of use. It is kept, since its shifts and sales
    /// name it, and a register taken out of use may still upload sales it
    /// made before.
    @Transactional(rollbackFor = IOException.class)
    public PosRegisterDto removeRegister(String username, String storeId, String registerId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Register r = register(storeId, registerId);
        if (openShift(r.getId()) != null) {
            throw new ResponseStatusException(409, "Close the register's shift first");
        }
        r.setActive(false);
        registers.save(r);
        return dto(r);
    }

    private void fill(Register r, PosRegisterDto wanted) {
        r.setName(Text.required(wanted.name(), 80, "the name"));
        String locationId = wanted.locationId() == null ? "" : wanted.locationId();
        Location place = locationId.length() == 0 ? null : locations.findById(locationId);
        if (place == null || !r.getStoreId().equals(place.getStoreId())) {
            throw new ResponseStatusException(400, "Choose one of the store's locations");
        }
        r.setLocationId(locationId);
        String country = Codes.normalize(wanted.taxCountry());
        if (country.length() != 0 && country.length() != 2) {
            throw new ResponseStatusException(400, "The tax country is two letters");
        }
        r.setTaxCountry(country);
        r.setTaxRegion(Codes.cut(wanted.taxRegion() == null ? "" : wanted.taxRegion().trim(), 80));
    }

    /// The store's register `registerId`, or 404.
    Register register(String storeId, String registerId) {
        Register r = registerId == null ? null : registers.findById(registerId);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No such register");
        }
        return r;
    }

    private Shift openShift(String registerId) {
        List<Shift> rows = shifts.findByRegisterIdOrderByOpenedAtDesc(registerId);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (OPEN.equals(rows.get(iter).getStatus())) {
                return rows.get(iter);
            }
        }
        return null;
    }

    private PosRegisterDto dto(Register r) {
        Shift open = openShift(r.getId());
        return new PosRegisterDto(r.getId(), r.getLocationId(), r.getName(), r.isActive(),
                r.getTaxCountry(), r.getTaxRegion(), open == null ? "" : open.getId());
    }

    // ------------------------------------------------------------------ PIN

    /// Checks the PIN of the staff member named (the caller when none is) at
    /// a register. 401 for a wrong PIN, 423 while the PIN is locked.
    public PosSessionDto signIn(String username, String storeId, String registerId,
            PinSignInDto pin) throws IOException {
        Seat seat = seat(username, storeId, registerId, pin);
        if (seat.member.getPinLockedUntil() != null
                && seat.member.getPinLockedUntil().toEpochMilli() > System.currentTimeMillis()) {
            throw new ResponseStatusException(423, "Too many wrong PINs; try again later");
        }
        if (seat.member.getPinHash().length() == 0) {
            throw new ResponseStatusException(409, "No PIN is set for " + seat.member.getUsername());
        }
        String typed = pin.pin() == null ? "" : pin.pin().trim();
        if (typed.length() == 0 || !encoder.matches(typed, seat.member.getPinHash())) {
            if (wrongPin(seat.member.getId())) {
                throw new ResponseStatusException(423, "Too many wrong PINs; try again later");
            }
            throw new ResponseStatusException(401, "Wrong PIN");
        }
        if (seat.member.getPinFailures() > 0) {
            rightPin(seat.member.getId());
        }
        Shift open = openShift(seat.register.getId());
        return new PosSessionDto(seat.register.getId(), storeId, seat.register.getLocationId(),
                seat.member.getUsername(), seat.member.getDisplayName(), seat.member.getRole(),
                open == null ? null : dto(open));
    }

    private static final class Seat {
        Register register;
        StoreStaff member;
    }

    @Transactional(readOnly = true)
    private Seat seat(String username, String storeId, String registerId, PinSignInDto pin)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Seat seat = new Seat();
        seat.register = register(storeId, registerId);
        if (!seat.register.isActive()) {
            throw new ResponseStatusException(409, "That register is not in use");
        }
        if (pin == null) {
            throw new ResponseStatusException(400, "Type the PIN");
        }
        String who = pin.username() == null || pin.username().trim().length() == 0 ? username
                : Accounts.email(pin.username());
        seat.member = staff.findByStoreIdAndUsername(storeId, who);
        if (seat.member == null) {
            throw new ResponseStatusException(403, "Not on this store's staff");
        }
        return seat;
    }

    /// Counts a wrong PIN; answers true when that was the last try and the
    /// PIN is now locked.
    @Transactional
    boolean wrongPin(String staffId) {
        staff.countPinFailure(staffId);
        StoreStaff member = staff.findById(staffId);
        if (member != null && member.getPinFailures() >= PIN_TRIES) {
            staff.lockPin(staffId, Instant.ofEpochMilli(System.currentTimeMillis() + LOCK_MILLIS));
            return true;
        }
        return false;
    }

    @Transactional
    void rightPin(String staffId) {
        staff.clearPinFailures(staffId);
    }

    // --------------------------------------------------------------- shifts

    @Transactional(rollbackFor = IOException.class)
    public ShiftDto openShift(String username, String storeId, String registerId,
            OpenShiftDto open) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Register r = register(storeId, registerId);
        if (!r.isActive()) {
            throw new ResponseStatusException(409, "That register is not in use");
        }
        if (openShift(r.getId()) != null) {
            throw new ResponseStatusException(409, "A shift is already open at this register");
        }
        long opening = open == null ? 0 : open.openingCents();
        if (opening < 0) {
            throw new ResponseStatusException(400, "The float cannot be negative");
        }
        Shift s = new Shift();
        s.setId(Ids.next());
        s.setRegisterId(r.getId());
        s.setStoreId(storeId);
        s.setUsername(username);
        s.setStatus(OPEN);
        s.setOpeningCents(opening);
        s.setExpectedCents(opening);
        s.setNote(Codes.cut(open == null || open.note() == null ? "" : open.note(), 500));
        s.setOpenedAt(Instant.now());
        shifts.insert(s);
        return dto(s);
    }

    @Transactional(readOnly = true)
    public ShiftDto shift(String username, String storeId, String shiftId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(shiftOf(storeId, shiftId));
    }

    @Transactional(readOnly = true)
    public List<CashMoveDto> cashMoves(String username, String storeId, String shiftId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        shiftOf(storeId, shiftId);
        List<CashMove> rows = cashMoves.findByShiftIdOrderByCreatedAtAsc(shiftId);
        List<CashMoveDto> out = new ArrayList<CashMoveDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            CashMove m = rows.get(iter);
            out.add(new CashMoveDto(m.getId(), m.getDeltaCents(), m.getReason(), m.getUsername(),
                    m.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    /// Cash put into the drawer (positive) or taken out (negative).
    @Transactional(rollbackFor = IOException.class)
    public ShiftDto moveCash(String username, String storeId, String shiftId, CashMoveDto move)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Shift s = shiftOf(storeId, shiftId);
        if (!OPEN.equals(s.getStatus())) {
            throw new ResponseStatusException(409, "The shift is closed");
        }
        if (move == null || move.deltaCents() == 0) {
            throw new ResponseStatusException(400, "Say how much cash went in or out");
        }
        CashMove m = new CashMove();
        m.setId(Ids.next());
        m.setShiftId(shiftId);
        m.setDeltaCents(move.deltaCents());
        m.setReason(Text.required(move.reason(), 200, "the reason"));
        m.setUsername(username);
        m.setCreatedAt(Instant.now());
        cashMoves.insert(m);
        shifts.addTotals(shiftId, 0, 0, 0, move.deltaCents(), move.deltaCents(), 0);
        return dto(shifts.findById(shiftId));
    }

    /// Closes a shift with the cash counted. Expected cash is the float plus
    /// cash sales (net of change and cash refunds) plus cash in and out; the
    /// difference is the shift's variance, over when positive.
    @Transactional(rollbackFor = IOException.class)
    public ShiftDto closeShift(String username, String storeId, String shiftId,
            CloseShiftDto close) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Shift s = shiftOf(storeId, shiftId);
        if (!OPEN.equals(s.getStatus())) {
            throw new ResponseStatusException(409, "The shift is already closed");
        }
        if (close == null || close.countedCents() < 0) {
            throw new ResponseStatusException(400, "Count the cash in the drawer");
        }
        s.setCountedCents(close.countedCents());
        if (close.note() != null && close.note().trim().length() > 0) {
            s.setNote(Codes.cut(close.note().trim(), 500));
        }
        s.setStatus(CLOSED);
        s.setClosedAt(Instant.now());
        shifts.save(s);
        return dto(s);
    }

    /// The store's shift `shiftId`, or 404.
    Shift shiftOf(String storeId, String shiftId) {
        Shift s = shiftId == null ? null : shifts.findById(shiftId);
        if (s == null || !storeId.equals(s.getStoreId())) {
            throw new ResponseStatusException(404, "No such shift");
        }
        return s;
    }

    static ShiftDto dto(Shift s) {
        boolean closed = CLOSED.equals(s.getStatus());
        return new ShiftDto(s.getId(), s.getRegisterId(), s.getUsername(), s.getStatus(),
                s.getOpeningCents(), s.getCashSalesCents(), s.getCardSalesCents(),
                s.getOtherSalesCents(), s.getCashMovesCents(), s.getExpectedCents(),
                s.getCountedCents(), closed ? s.getCountedCents() - s.getExpectedCents() : 0,
                s.getSalesCount(), s.getNote(), millis(s.getOpenedAt()),
                closed ? millis(s.getClosedAt()) : 0);
    }

    private static long millis(Instant at) {
        return at == null ? 0 : at.toEpochMilli();
    }

    // -------------------------------------------------------------- catalog

    /// Everything a register sells, priced and with what is available at its
    /// location, to sell from while offline. `version` is a digest of the
    /// whole answer; asked with the version the register already holds, the
    /// answer is marked unchanged and carries no lists. A discount's use
    /// count is left out of the digest, so every sale with a code does not
    /// make every register download the catalog again.
    @Transactional(readOnly = true)
    public PosCatalogDto catalog(String username, String storeId, String registerId,
            String version) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Register r = register(storeId, registerId);
        Store store = stores.findById(storeId);
        List<Product> ps = products.findByStoreIdAndStatusOrderByTitleAsc(storeId, "ACTIVE");
        Map<String, List<Variant>> byProduct = new HashMap<String, List<Variant>>();
        List<Variant> vs = variants.findByStoreId(storeId);
        for (int iter = 0; iter < vs.size(); iter++) {
            Variant v = vs.get(iter);
            List<Variant> list = byProduct.get(v.getProductId());
            if (list == null) {
                list = new ArrayList<Variant>();
                byProduct.put(v.getProductId(), list);
            }
            int at = list.size();
            while (at > 0 && list.get(at - 1).getPosition() > v.getPosition()) {
                at--;
            }
            list.add(at, v);
        }
        Map<String, Integer> here = new HashMap<String, Integer>();
        List<InventoryLevel> ls = levels.findByLocationId(r.getLocationId());
        for (int iter = 0; iter < ls.size(); iter++) {
            InventoryLevel l = ls.get(iter);
            here.put(l.getVariantId(), Integer.valueOf(l.getOnHand() - l.getCommitted()));
        }
        // A bundle sells what its components make here; a product outside
        // its point of sale window is not on the register at all.
        Map<String, List<com.codenameone.examples.bazaar.catalog.Bundles.Part>> bundleParts =
                bundles.byVariant(storeId);
        java.util.Set<String> at = new java.util.HashSet<String>();
        at.add(r.getLocationId());
        for (Map.Entry<String, List<com.codenameone.examples.bazaar.catalog.Bundles.Part>> e
                : bundleParts.entrySet()) {
            here.put(e.getKey(), Integer.valueOf(bundles.available(e.getValue(), at)));
        }
        java.util.Set<String> hidden = schedules.hidden(storeId,
                com.codenameone.examples.bazaar.catalog.Schedules.POS);
        StringBuilder digest = new StringBuilder();
        digest.append(store.getCurrency()).append('|').append(store.isTaxInclusive()).append('|')
                .append(r.getLocationId()).append('|').append(r.getTaxCountry()).append('|')
                .append(r.getTaxRegion()).append('\n');
        List<PosItemDto> items = new ArrayList<PosItemDto>();
        for (int iter = 0; iter < ps.size(); iter++) {
            Product p = ps.get(iter);
            if (hidden.contains(p.getId())) {
                continue;
            }
            List<Variant> list = byProduct.get(p.getId());
            for (int v = 0; list != null && v < list.size(); v++) {
                Variant x = list.get(v);
                Integer a = here.get(x.getId());
                int available = a == null ? 0 : a.intValue();
                boolean tracked = x.isTrackInventory() || bundleParts.containsKey(x.getId());
                PosItemDto item = new PosItemDto(x.getId(), p.getId(), p.getTitle(), x.getTitle(),
                        x.getSku(), x.getBarcode(), x.getPriceCents(), x.getCompareAtCents(),
                        x.isTaxable(), tracked, available, p.getPhotoId());
                items.add(item);
                digest.append(x.getId()).append('|').append(p.getTitle()).append('|')
                        .append(x.getTitle()).append('|').append(x.getSku()).append('|')
                        .append(x.getBarcode()).append('|').append(x.getPriceCents()).append('|')
                        .append(x.getCompareAtCents()).append('|').append(x.isTaxable())
                        .append('|').append(tracked).append('|').append(available)
                        .append('|').append(p.getPhotoId()).append('\n');
            }
        }
        List<TaxRate> rates = taxes.findByStoreId(storeId);
        List<TaxRateDto> rateDtos = new ArrayList<TaxRateDto>();
        for (int iter = 0; iter < rates.size(); iter++) {
            TaxRateDto t = RateSettings.dto(rates.get(iter));
            rateDtos.add(t);
            digest.append(t.id()).append('|').append(t.country()).append('|')
                    .append(t.region()).append('|').append(t.name()).append('|')
                    .append(t.basisPoints()).append('|').append(t.appliesToShipping())
                    .append('\n');
        }
        List<Discount> ds = discounts.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<DiscountDto> discountDtos = new ArrayList<DiscountDto>();
        for (int iter = 0; iter < ds.size(); iter++) {
            // A members-only perk is left out: a sale at the counter is priced
            // with no memberships (`PosSales.price`), and the register cannot
            // read the eligibility off a DiscountDto, so it used to take the
            // perk off every sale and the server refused each one as underpaid.
            String members = ds.get(iter).getEligibility();
            if (!ds.get(iter).isActive() || members != null && members.length() > 0) {
                continue;
            }
            DiscountDto d = RateSettings.dto(ds.get(iter));
            discountDtos.add(d);
            digest.append(d.id()).append('|').append(d.code()).append('|').append(d.title())
                    .append('|').append(d.kind()).append('|').append(d.amount()).append('|')
                    .append(d.minSubtotalCents()).append('|').append(d.buyQty()).append('|')
                    .append(d.getQty()).append('|').append(d.appliesTo()).append('|')
                    .append(d.targetId()).append('|').append(d.automatic()).append('|')
                    .append(d.usageLimit()).append('|')
                    .append(d.oncePerCustomer()).append('|').append(d.combinable()).append('|')
                    .append(d.startsAt()).append('|').append(d.endsAt()).append('\n');
        }
        String current = Ids.sha256(digest.toString()).substring(0, 24);
        if (current.equals(version)) {
            return new PosCatalogDto(current, true, storeId, r.getId(), r.getLocationId(),
                    store.getCurrency(), store.isTaxInclusive(), r.getTaxCountry(),
                    r.getTaxRegion(), new ArrayList<PosItemDto>(), new ArrayList<TaxRateDto>(),
                    new ArrayList<DiscountDto>());
        }
        return new PosCatalogDto(current, false, storeId, r.getId(), r.getLocationId(),
                store.getCurrency(), store.isTaxInclusive(), r.getTaxCountry(), r.getTaxRegion(),
                items, rateDtos, discountDtos);
    }

    // ------------------------------------------------------------ customers

    /// Up to 20 of the store's customers whose e-mail or name contains `q`,
    /// ignoring ASCII case, or whose phone number contains its digits.
    @Transactional(readOnly = true)
    public List<PosCustomerDto> customers(String username, String storeId, String q)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        String wanted = q == null ? "" : q.trim();
        List<PosCustomerDto> out = new ArrayList<PosCustomerDto>();
        if (wanted.length() < 2) {
            return out;
        }
        String digits = digits(wanted);
        List<Customer> all = customers.findByStoreIdOrderByLastNameAsc(storeId);
        for (int iter = 0; iter < all.size() && out.size() < 20; iter++) {
            Customer c = all.get(iter);
            boolean hit = contains(c.getEmail(), wanted)
                    || contains(c.getFirstName() + " " + c.getLastName(), wanted)
                    || digits.length() >= 3 && digits(c.getPhone()).indexOf(digits) >= 0;
            if (hit) {
                out.add(dto(c));
            }
        }
        return out;
    }

    static PosCustomerDto dto(Customer c) {
        return new PosCustomerDto(c.getId(), c.getUsername(), c.getEmail(), c.getPhone(),
                c.getFirstName(), c.getLastName(), c.getStoreCreditCents(), c.getOrdersCount(),
                c.getSpentCents());
    }

    private static String digits(String text) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; text != null && iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c >= '0' && c <= '9') {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// Whether `text` contains `part`, ignoring ASCII case.
    static boolean contains(String text, String part) {
        if (text == null) {
            return false;
        }
        for (int iter = 0; iter + part.length() <= text.length(); iter++) {
            if (text.regionMatches(true, iter, part, 0, part.length())) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------- returns

    /// A return at the counter: the units go back on hand at the line's
    /// location when `restock`, and the money goes back either from the
    /// drawer (CASH) or the way the sale was paid (ORIGINAL: card first, then
    /// gift card, the rest from the drawer). Money paid out of the drawer
    /// comes off the open shift's cash; a card refund off its card sales.
    public PosReturnResultDto counterReturn(String username, String storeId, String registerId,
            String orderId, PosReturnDto req) throws IOException {
        if (req == null) {
            throw new ResponseStatusException(400, "Say what is being returned");
        }
        boolean byHand = !"ORIGINAL".equalsIgnoreCase(req.refundTo());
        Shift shift = checkReturn(username, storeId, registerId, orderId, req);
        long[] parts = orders.refundParts(username, storeId, orderId,
                new RefundRequestDto(req.lines(), 0, req.restock(),
                        req.reason() == null ? "Counter return" : req.reason()),
                byHand, StoreAccess.ANYONE);
        long cash = parts[3];
        shifts.addTotals(shift.getId(), -cash, -parts[1], -parts[2], 0, -cash, 0);
        orders.event(orderId, "RETURNED", "Returned at the counter", username);
        return new PosReturnResultDto(orders.detailOf(orderId), parts[0], cash, parts[1],
                parts[2]);
    }

    /// What the counter return `req` of `orderId` would refund, refunding
    /// nothing: what a manager's approval is checked against.
    @Transactional(readOnly = true)
    public long quoteReturn(String storeId, String orderId, PosReturnDto req)
            throws IOException {
        StoreOrder order = orderId == null ? null : orderRows.findById(orderId);
        if (order == null || !storeId.equals(order.getStoreId()) || req == null) {
            throw new ResponseStatusException(404, "No such order");
        }
        return orders.quoteRefund(orderId, new RefundRequestDto(req.lines(), 0, req.restock(),
                ""));
    }

    @Transactional(readOnly = true)
    private Shift checkReturn(String username, String storeId, String registerId, String orderId,
            PosReturnDto req) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Register r = register(storeId, registerId);
        StoreOrder order = orderId == null ? null : orderRows.findById(orderId);
        if (order == null || !storeId.equals(order.getStoreId())) {
            throw new ResponseStatusException(404, "No such order");
        }
        Shift shift = req.shiftId() == null ? openShift(r.getId()) : shiftOf(storeId, req.shiftId());
        if (shift == null || !OPEN.equals(shift.getStatus()) || !r.getId().equals(shift.getRegisterId())) {
            throw new ResponseStatusException(409, "Open a shift at this register first");
        }
        return shift;
    }

    // ------------------------------------------------------------- receipts

    @Transactional(readOnly = true)
    public PosReceiptDto receipt(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return receiptOf(storeId, orderId, false, false);
    }

    /// Texts the receipt to a phone and/or e-mails it with the receipt PDF
    /// ([Emails#posReceipt]), recording each on the order.
    public PosReceiptDto sendReceipt(String username, String storeId, String orderId,
            ReceiptSendDto to) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        if (to == null || empty(to.email()) && empty(to.phone())) {
            throw new ResponseStatusException(400, "Give an e-mail address or a phone number");
        }
        PosReceiptDto r = receiptOf(storeId, orderId, false, false);
        boolean texted = false;
        if (!empty(to.phone())) {
            // The till shows the receipt in English (the staff's working
            // text); the shopper's copy is in the shopper's language.
            String lang = language.of(orderRows.findById(orderId));
            String intro = templates.overrides(storeId, lang).get(Templates.SMS_RECEIPT);
            String body = documents.receiptText(storeId, orderId, lang);
            sms.send(to.phone().trim(), intro == null || intro.length() == 0 ? body
                    : intro + "\n" + body);
            texted = true;
            orders.event(orderId, "RECEIPT_TEXTED", "Receipt texted", username);
        }
        boolean emailed = false;
        if (!empty(to.email())) {
            emails.posReceipt(storeId, orderId, to.email().trim());
            emailed = true;
            orders.event(orderId, "RECEIPT_EMAILED", "Receipt e-mailed to "
                    + Codes.cut(to.email().trim(), 190), username);
        }
        return new PosReceiptDto(r.orderId(), r.number(), r.text(), r.pdfPath(), texted, emailed);
    }

    private PosReceiptDto receiptOf(String storeId, String orderId, boolean texted,
            boolean emailed) throws IOException {
        StoreOrder o = documents.order(storeId, orderId);
        return new PosReceiptDto(orderId, o.getNumber(), documents.receiptText(storeId, orderId),
                "/api/merchant/stores/" + storeId + "/orders/" + orderId + "/receipt.pdf",
                texted, emailed);
    }

    private static boolean empty(String text) {
        return text == null || text.trim().length() == 0;
    }
}
