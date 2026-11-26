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
package com.codenameone.examples.bazaar.rates;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.ShippingProfileDto;
import com.codenameone.examples.bazaar.api.ShippingRateDto;
import com.codenameone.examples.bazaar.api.ShippingZoneDto;
import com.codenameone.examples.bazaar.api.TaxRateDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.ShippingRate;
import com.codenameone.examples.bazaar.domain.ShippingZone;
import com.codenameone.examples.bazaar.domain.TaxRate;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.ShippingRateRepository;
import com.codenameone.examples.bazaar.repository.ShippingZoneRepository;
import com.codenameone.examples.bazaar.repository.TaxRateRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A store's discounts, tax rates and shipping zones and rates, written by
/// its OWNER and MANAGERs. The arithmetic that uses them is in `pricing`.
@Component
public class RateSettings {
    private static final String[] KINDS = {DiscountEngine.PERCENT, DiscountEngine.FIXED,
        DiscountEngine.FREE_SHIPPING, DiscountEngine.BUY_X_GET_Y};
    private static final String[] TARGETS = {DiscountEngine.ALL, DiscountEngine.COLLECTION,
        DiscountEngine.PRODUCT};
    private static final String[] RATE_KINDS = {ShippingCalculator.FLAT, ShippingCalculator.WEIGHT,
        ShippingCalculator.PRICE, ShippingCalculator.FREE_OVER, ShippingCalculator.PICKUP,
        ShippingCalculator.LOCAL, ShippingCalculator.CARRIER};
    /// A carrier markup above 1000% is a typo.
    private static final int MAX_MARKUP_BPS = 100000;

    private final DiscountRepository discounts;
    private final TaxRateRepository taxes;
    private final ShippingZoneRepository zones;
    private final ShippingRateRepository rates;
    private final LocationRepository locations;
    private final StoreAccess access;
    private final DataSource db;

    public RateSettings(DiscountRepository discounts, TaxRateRepository taxes,
            ShippingZoneRepository zones, ShippingRateRepository rates,
            LocationRepository locations, StoreAccess access, DataSource db) {
        this.db = db;
        this.discounts = discounts;
        this.taxes = taxes;
        this.zones = zones;
        this.rates = rates;
        this.locations = locations;
        this.access = access;
    }

    private static String oneOf(String value, String[] allowed, String what) {
        for (int iter = 0; iter < allowed.length; iter++) {
            if (allowed[iter].equals(value)) {
                return value;
            }
        }
        throw new ResponseStatusException(400, "That is not a " + what);
    }

    // ------------------------------------------------------------ discounts

    @Transactional(readOnly = true)
    public List<DiscountDto> discounts(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<Discount> rows = discounts.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<DiscountDto> out = new ArrayList<DiscountDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            // Bulk-generated codes are listed by DiscountCodes, not here.
            if (rows.get(iter).getBatchId().length() == 0) {
                out.add(dto(rows.get(iter)));
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public DiscountDto saveDiscount(String username, String storeId, DiscountDto w)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (w == null) {
            throw new ResponseStatusException(400, "Describe the discount");
        }
        Discount d;
        boolean fresh = w.id() == null || w.id().length() == 0;
        if (fresh) {
            d = new Discount();
            d.setId(Ids.next());
            d.setStoreId(storeId);
            d.setCreatedAt(Instant.now());
        } else {
            d = discounts.findById(w.id());
            if (d == null || !storeId.equals(d.getStoreId())) {
                throw new ResponseStatusException(404, "No such discount");
            }
        }
        String code = Codes.cut(Codes.normalize(w.code()), 64);
        if (!w.automatic() && code.length() == 0) {
            throw new ResponseStatusException(400, "A discount that is not automatic needs a code");
        }
        if (code.length() > 0) {
            List<Discount> same = discounts.findByStoreIdAndCode(storeId, code);
            for (int iter = 0; iter < same.size(); iter++) {
                if (!same.get(iter).getId().equals(d.getId())) {
                    throw new ResponseStatusException(409, "Another discount has that code");
                }
            }
        }
        String kind = oneOf(w.kind(), KINDS, "kind of discount");
        if ((DiscountEngine.PERCENT.equals(kind) && (w.amount() <= 0 || w.amount() > 10000))
                || (DiscountEngine.FIXED.equals(kind) && w.amount() <= 0)
                || (DiscountEngine.BUY_X_GET_Y.equals(kind)
                    && (w.buyQty() <= 0 || w.getQty() <= 0 || w.amount() < 0 || w.amount() > 10000))
                || w.minSubtotalCents() < 0 || w.usageLimit() < 0) {
            throw new ResponseStatusException(400, "Those amounts do not make a discount");
        }
        String target = w.appliesTo() == null || w.appliesTo().length() == 0 ? DiscountEngine.ALL
                : oneOf(w.appliesTo(), TARGETS, "target");
        d.setCode(code);
        d.setTitle(Codes.cut(w.title(), 120));
        d.setKind(kind);
        d.setAmount(w.amount());
        d.setMinSubtotalCents(w.minSubtotalCents());
        d.setBuyQty(Math.max(0, w.buyQty()));
        d.setGetQty(Math.max(0, w.getQty()));
        d.setAppliesTo(target);
        d.setTargetId(DiscountEngine.ALL.equals(target) ? "" : Codes.cut(w.targetId(), 32));
        d.setAutomatic(w.automatic());
        d.setUsageLimit(w.usageLimit());
        d.setOncePerCustomer(w.oncePerCustomer());
        d.setCombinable(w.combinable());
        // Both columns are NOT NULL: "no start" and "no end" are the epoch
        // (the entity's own default, read back as 0 and treated as unset by
        // DiscountEngine), never null, which failed the flush for every
        // discount saved without dates.
        d.setStartsAt(w.startsAt() > 0 ? Instant.ofEpochMilli(w.startsAt()) : Instant.EPOCH);
        d.setEndsAt(w.endsAt() > 0 ? Instant.ofEpochMilli(w.endsAt()) : Instant.EPOCH);
        d.setActive(w.active());
        if (fresh) {
            discounts.insert(d);
        } else {
            discounts.save(d);
        }
        return dto(d);
    }

    @Transactional(rollbackFor = IOException.class)
    public DiscountDto deleteDiscount(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Discount d = id == null ? null : discounts.findById(id);
        if (d == null || !storeId.equals(d.getStoreId())) {
            throw new ResponseStatusException(404, "No such discount");
        }
        // The codes generated from it go with it.
        List<Discount> codes = discounts.findByBatchIdOrderByCreatedAtAsc(d.getId());
        for (int iter = 0; iter < codes.size(); iter++) {
            discounts.delete(codes.get(iter));
        }
        discounts.delete(d);
        return dto(d);
    }

    public static DiscountDto dto(Discount d) {
        return new DiscountDto(d.getId(), d.getCode(), d.getTitle(), d.getKind(), d.getAmount(),
                d.getMinSubtotalCents(), d.getBuyQty(), d.getGetQty(), d.getAppliesTo(),
                d.getTargetId(), d.isAutomatic(), d.getUsageLimit(), d.getUsedCount(),
                d.isOncePerCustomer(), d.isCombinable(),
                d.getStartsAt() == null ? 0 : d.getStartsAt().toEpochMilli(),
                d.getEndsAt() == null ? 0 : d.getEndsAt().toEpochMilli(), d.isActive());
    }

    // ------------------------------------------------------------ tax rates

    @Transactional(readOnly = true)
    public List<TaxRateDto> taxRates(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<TaxRate> rows = taxes.findByStoreId(storeId);
        List<TaxRateDto> out = new ArrayList<TaxRateDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public TaxRateDto saveTaxRate(String username, String storeId, TaxRateDto w)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (w == null || w.country() == null || w.country().trim().length() != 2
                || w.basisPoints() < 0 || w.basisPoints() > 10000) {
            throw new ResponseStatusException(400, "A tax rate needs a two letter country and a rate");
        }
        TaxRate r;
        boolean fresh = w.id() == null || w.id().length() == 0;
        if (fresh) {
            r = new TaxRate();
            r.setId(Ids.next());
            r.setStoreId(storeId);
        } else {
            r = taxes.findById(w.id());
            if (r == null || !storeId.equals(r.getStoreId())) {
                throw new ResponseStatusException(404, "No such tax rate");
            }
        }
        r.setCountry(Codes.normalize(w.country()));
        r.setRegion(Codes.cut(Codes.normalize(w.region()), 80));
        r.setName(Codes.cut(w.name(), 80));
        r.setBasisPoints(w.basisPoints());
        r.setAppliesToShipping(w.appliesToShipping());
        if (fresh) {
            taxes.insert(r);
        } else {
            taxes.save(r);
        }
        return dto(r);
    }

    @Transactional(rollbackFor = IOException.class)
    public TaxRateDto deleteTaxRate(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        TaxRate r = id == null ? null : taxes.findById(id);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No such tax rate");
        }
        taxes.delete(r);
        return dto(r);
    }

    public static TaxRateDto dto(TaxRate r) {
        return new TaxRateDto(r.getId(), r.getCountry(), r.getRegion(), r.getName(),
                r.getBasisPoints(), r.isAppliesToShipping());
    }

    // ------------------------------------------------------------- shipping

    @Transactional(readOnly = true)
    public List<ShippingZoneDto> zones(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<ShippingZone> rows = zones.findByStoreIdOrderByPositionAsc(storeId);
        List<ShippingRate> all = rates.findByStoreIdOrderByPositionAsc(storeId);
        List<ShippingZoneDto> out = new ArrayList<ShippingZoneDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            ShippingZone z = rows.get(iter);
            List<ShippingRateDto> mine = new ArrayList<ShippingRateDto>();
            for (int r = 0; r < all.size(); r++) {
                if (z.getId().equals(all.get(r).getZoneId())) {
                    mine.add(dto(all.get(r)));
                }
            }
            out.add(new ShippingZoneDto(z.getId(), z.getName(), z.getCountries(), z.getPosition(),
                    mine, z.getProfileId()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public ShippingZoneDto saveZone(String username, String storeId, ShippingZoneDto w)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (w == null || w.countries() == null || w.countries().trim().length() == 0) {
            throw new ResponseStatusException(400, "A zone needs its countries");
        }
        ShippingZone z;
        boolean fresh = w.id() == null || w.id().length() == 0;
        if (fresh) {
            z = new ShippingZone();
            z.setId(Ids.next());
            z.setStoreId(storeId);
        } else {
            z = zones.findById(w.id());
            if (z == null || !storeId.equals(z.getStoreId())) {
                throw new ResponseStatusException(404, "No such zone");
            }
        }
        z.setName(Codes.cut(w.name(), 120));
        z.setCountries(Codes.cut(w.countries().replace(" ", ""), 2000));
        z.setPosition(w.position());
        z.setProfileId(profileIn(storeId, w.profileId()));
        if (fresh) {
            zones.insert(z);
        } else {
            zones.save(z);
        }
        return new ShippingZoneDto(z.getId(), z.getName(), z.getCountries(), z.getPosition(),
                new ArrayList<ShippingRateDto>(), z.getProfileId());
    }

    @Transactional(rollbackFor = IOException.class)
    public ShippingZoneDto deleteZone(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        ShippingZone z = id == null ? null : zones.findById(id);
        if (z == null || !storeId.equals(z.getStoreId())) {
            throw new ResponseStatusException(404, "No such zone");
        }
        rates.deleteByZoneId(z.getId());
        zones.delete(z);
        return new ShippingZoneDto(z.getId(), z.getName(), z.getCountries(), z.getPosition(),
                new ArrayList<ShippingRateDto>(), z.getProfileId());
    }

    // ---------------------------------------------------- shipping profiles

    /// `profileId` when it is a profile of the store, empty for the general
    /// profile; 404 for anything else.
    private String profileIn(String storeId, String profileId) throws IOException {
        if (profileId == null || profileId.length() == 0) {
            return "";
        }
        Map row = db.queryOne("SELECT id FROM shipping_profile WHERE id = ? AND store_id = ?",
                new Object[] {profileId, storeId});
        if (row == null) {
            throw new ResponseStatusException(404, "No such shipping profile");
        }
        return profileId;
    }

    @Transactional(readOnly = true)
    public List<ShippingProfileDto> profiles(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List rows = db.query("SELECT id, name, position FROM shipping_profile WHERE store_id = ? "
                + "ORDER BY position, id", new Object[] {storeId});
        List<ShippingProfileDto> out = new ArrayList<ShippingProfileDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String id = Rows.text(r.get("id"));
            out.add(new ShippingProfileDto(id, Rows.text(r.get("name")),
                    (int) Rows.number(r.get("position")), productsOf(storeId, id)));
        }
        return out;
    }

    /// The store's products as `{id, title}`, by title, at most 500: what
    /// the back office offers to put in a profile.
    @Transactional(readOnly = true)
    public List<String[]> productChoices(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List rows = db.query("SELECT id, title FROM product WHERE store_id = ? ORDER BY title, id",
                new Object[] {storeId});
        List<String[]> out = new ArrayList<String[]>();
        for (int iter = 0; iter < rows.size() && iter < 500; iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new String[] {Rows.text(r.get("id")), Rows.text(r.get("title"))});
        }
        return out;
    }

    private List<String> productsOf(String storeId, String profileId) throws IOException {
        List rows = db.query("SELECT product_id FROM shipping_profile_product WHERE store_id = ? "
                + "AND profile_id = ? ORDER BY product_id", new Object[] {storeId, profileId});
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(Rows.text(((Map) rows.get(iter)).get("product_id")));
        }
        return out;
    }

    /// Saves a profile; its `productIds` become exactly its products, each
    /// taken out of whatever profile it was in.
    @Transactional(rollbackFor = IOException.class)
    public ShippingProfileDto saveProfile(String username, String storeId, ShippingProfileDto w)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        String name = w == null ? "" : Codes.cut(w.name(), 120).trim();
        if (name.length() == 0) {
            throw new ResponseStatusException(400, "A shipping profile needs a name");
        }
        List<String> wanted = new ArrayList<String>();
        for (int iter = 0; w.productIds() != null && iter < w.productIds().size(); iter++) {
            String p = w.productIds().get(iter);
            if (p == null || wanted.contains(p)) {
                continue;
            }
            if (db.queryOne("SELECT id FROM product WHERE id = ? AND store_id = ?",
                    new Object[] {p, storeId}) == null) {
                throw new ResponseStatusException(404, "No such product");
            }
            wanted.add(p);
        }
        String id = w.id() == null ? "" : w.id();
        if (id.length() == 0) {
            id = Ids.next();
            db.execute("INSERT INTO shipping_profile (id, store_id, name, position) VALUES "
                    + "(?, ?, ?, ?)", new Object[] {id, storeId, name,
                        Integer.valueOf(Math.max(0, w.position()))});
        } else {
            profileIn(storeId, id);
            db.execute("UPDATE shipping_profile SET name = ?, position = ? WHERE id = ?",
                    new Object[] {name, Integer.valueOf(Math.max(0, w.position())), id});
        }
        db.execute("DELETE FROM shipping_profile_product WHERE store_id = ? AND profile_id = ?",
                new Object[] {storeId, id});
        for (int iter = 0; iter < wanted.size(); iter++) {
            db.execute("DELETE FROM shipping_profile_product WHERE product_id = ?",
                    new Object[] {wanted.get(iter)});
            db.execute("INSERT INTO shipping_profile_product (product_id, store_id, profile_id) "
                    + "VALUES (?, ?, ?)", new Object[] {wanted.get(iter), storeId, id});
        }
        return new ShippingProfileDto(id, name, Math.max(0, w.position()), wanted);
    }

    /// Deletes a profile with its zones and their rates; its products ship
    /// under the general profile again.
    @Transactional(rollbackFor = IOException.class)
    public ShippingProfileDto deleteProfile(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Map row = id == null ? null : db.queryOne("SELECT id, name, position FROM shipping_profile "
                + "WHERE id = ? AND store_id = ?", new Object[] {id, storeId});
        if (row == null) {
            throw new ResponseStatusException(404, "No such shipping profile");
        }
        List<String> had = productsOf(storeId, id);
        List<ShippingZone> all = zones.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (id.equals(all.get(iter).getProfileId())) {
                rates.deleteByZoneId(all.get(iter).getId());
                zones.delete(all.get(iter));
            }
        }
        db.execute("DELETE FROM shipping_profile_product WHERE store_id = ? AND profile_id = ?",
                new Object[] {storeId, id});
        db.execute("DELETE FROM shipping_profile WHERE id = ?", new Object[] {id});
        return new ShippingProfileDto(id, Rows.text(row.get("name")),
                (int) Rows.number(row.get("position")), had);
    }

    @Transactional(rollbackFor = IOException.class)
    public ShippingRateDto saveRate(String username, String storeId, ShippingRateDto w)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (w == null || w.priceCents() < 0 || w.minValue() < 0 || w.maxValue() < 0) {
            throw new ResponseStatusException(400, "Those amounts do not make a rate");
        }
        String kind = oneOf(w.kind(), RATE_KINDS, "kind of rate");
        boolean carrier = ShippingCalculator.CARRIER.equals(kind);
        if (w.markupBps() < 0 || w.markupBps() > MAX_MARKUP_BPS || w.fallbackCents() < 0) {
            throw new ResponseStatusException(400, "A carrier markup is 0 to 1000%, and a "
                    + "fallback price is not negative");
        }
        boolean located = ShippingCalculator.PICKUP.equals(kind)
                || ShippingCalculator.LOCAL.equals(kind);
        String zoneId = "";
        if (!ShippingCalculator.PICKUP.equals(kind) || (w.zoneId() != null && w.zoneId().length() > 0)) {
            ShippingZone z = w.zoneId() == null ? null : zones.findById(w.zoneId());
            if (z == null || !storeId.equals(z.getStoreId())) {
                throw new ResponseStatusException(404, "No such zone");
            }
            zoneId = z.getId();
        }
        String locationId = "";
        if (located) {
            Location at = w.locationId() == null ? null : locations.findById(w.locationId());
            if (at == null || !storeId.equals(at.getStoreId())) {
                throw new ResponseStatusException(404, "No such location");
            }
            locationId = at.getId();
            if (ShippingCalculator.LOCAL.equals(kind) && w.radiusMeters() <= 0) {
                throw new ResponseStatusException(400, "Local delivery needs a radius");
            }
        }
        ShippingRate r;
        boolean fresh = w.id() == null || w.id().length() == 0;
        if (fresh) {
            r = new ShippingRate();
            r.setId(Ids.next());
            r.setStoreId(storeId);
        } else {
            r = rates.findById(w.id());
            if (r == null || !storeId.equals(r.getStoreId())) {
                throw new ResponseStatusException(404, "No such rate");
            }
        }
        r.setZoneId(zoneId);
        r.setName(Codes.cut(w.name(), 120));
        r.setKind(kind);
        r.setPriceCents(w.priceCents());
        r.setMinValue(w.minValue());
        r.setMaxValue(w.maxValue());
        r.setCarrier(Codes.cut(w.carrier(), 64));
        r.setService(Codes.cut(w.service(), 64));
        r.setEtaDaysMin(Math.max(0, w.etaDaysMin()));
        r.setEtaDaysMax(Math.max(w.etaDaysMin(), w.etaDaysMax()));
        r.setLocationId(locationId);
        r.setRadiusMeters(Math.max(0, w.radiusMeters()));
        r.setPosition(w.position());
        r.setMarkupBps(carrier ? w.markupBps() : 0);
        r.setFallbackCents(carrier ? w.fallbackCents() : 0);
        if (fresh) {
            rates.insert(r);
        } else {
            rates.save(r);
        }
        return dto(r);
    }

    @Transactional(rollbackFor = IOException.class)
    public ShippingRateDto deleteRate(String username, String storeId, String id)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        ShippingRate r = id == null ? null : rates.findById(id);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No such rate");
        }
        rates.delete(r);
        return dto(r);
    }

    static ShippingRateDto dto(ShippingRate r) {
        return new ShippingRateDto(r.getId(), r.getZoneId(), r.getName(), r.getKind(),
                r.getPriceCents(), r.getMinValue(), r.getMaxValue(), r.getCarrier(),
                r.getService(), r.getEtaDaysMin(), r.getEtaDaysMax(), r.getLocationId(),
                r.getRadiusMeters(), r.getPosition(), r.getMarkupBps(), r.getFallbackCents());
    }
}
