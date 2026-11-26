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
package com.codenameone.examples.bazaar.taxes;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.TaxExemptionDto;
import com.codenameone.examples.bazaar.api.TaxReportRowDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.OrderExemption;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.TaxExemption;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.repository.OrderExemptionRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.TaxExemptionRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Tax exemptions and the tax report.
///
/// **Exemptions.** A customer (by account) may hold an exemption certificate
/// covering regions: `US-NY` is one state, `DE` a whole country. A quote to an
/// address it covers, before the certificate expires, carries no tax, and the
/// order remembers the certificate ([OrderExemption]) for the report and an
/// audit. A store whose prices include tax does not apply exemptions at
/// checkout: the tax is inside every price, and taking it out would make the
/// order's lines and totals disagree. Such a store refunds the tax portion
/// instead, and the quote says nothing was exempted.
///
/// **The report** sums the store's orders placed in a period by destination
/// country and region: the orders, their sales after discounts, the tax
/// collected and how many were exempt. Cancelled orders are left out.
@Component
public class TaxExemptions {
    static final int MAX_REGIONS = 40;

    private final TaxExemptionRepository exemptions;
    private final OrderExemptionRepository uses;
    private final StoreOrderRepository orders;
    private final StoreAccess access;
    private final ReverseCharge reverseCharge;

    public TaxExemptions(TaxExemptionRepository exemptions, OrderExemptionRepository uses,
            StoreOrderRepository orders, StoreAccess access, ReverseCharge reverseCharge) {
        this.reverseCharge = reverseCharge;
        this.exemptions = exemptions;
        this.uses = uses;
        this.orders = orders;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<TaxExemptionDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<TaxExemption> rows = exemptions.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<TaxExemptionDto> out = new ArrayList<TaxExemptionDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    static TaxExemptionDto dto(TaxExemption t) {
        return new TaxExemptionDto(t.getId(), t.getUsername(), t.getCertificateId(),
                split(t.getRegions()), t.getExpiresAt().toEpochMilli());
    }

    static List<String> split(String text) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        while (start < text.length()) {
            int comma = text.indexOf(',', start);
            int end = comma < 0 ? text.length() : comma;
            String r = text.substring(start, end).trim();
            if (r.length() > 0) {
                out.add(r);
            }
            start = end + 1;
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public TaxExemptionDto add(String username, String storeId, TaxExemptionDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null || Codes.cut(in.username(), 200).length() == 0) {
            throw new ResponseStatusException(400, "Name the customer's account");
        }
        String cert = Codes.cut(in.certificateId(), 64);
        if (cert.length() == 0) {
            throw new ResponseStatusException(400, "Enter the certificate number");
        }
        List<String> given = in.regions() == null ? new ArrayList<String>() : in.regions();
        if (given.isEmpty() || given.size() > MAX_REGIONS) {
            throw new ResponseStatusException(400, "Enter 1 to " + MAX_REGIONS + " regions");
        }
        StringBuilder rs = new StringBuilder();
        for (int iter = 0; iter < given.size(); iter++) {
            if (rs.length() > 0) {
                rs.append(',');
            }
            rs.append(region(given.get(iter)));
        }
        TaxExemption t = new TaxExemption();
        t.setId(Ids.next());
        t.setStoreId(storeId);
        t.setUsername(in.username().trim());
        t.setCertificateId(cert);
        t.setRegions(rs.toString());
        t.setExpiresAt(Instant.ofEpochMilli(Math.max(0, in.expiresAt())));
        t.setCreatedAt(Instant.now());
        exemptions.insert(t);
        return dto(t);
    }

    @Transactional(rollbackFor = IOException.class)
    public void remove(String username, String storeId, String exemptionId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        TaxExemption t = exemptionId == null ? null : exemptions.findById(exemptionId);
        if (t == null || !storeId.equals(t.getStoreId())) {
            throw new ResponseStatusException(404, "No such exemption");
        }
        exemptions.delete(t);
    }

    /// `US-NY` or `DE`, upper case; the part after the dash is 1 to 3
    /// letters or digits.
    static String region(String text) {
        String t = text == null ? "" : text.trim();
        String country = Markets.country(t.length() >= 2 ? t.substring(0, 2) : t);
        if (t.length() == 2) {
            return country;
        }
        if (t.length() < 4 || t.length() > 6 || t.charAt(2) != '-') {
            throw new ResponseStatusException(400, "A region is like US-NY or DE");
        }
        StringBuilder out = new StringBuilder(country).append('-');
        for (int iter = 3; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - 'a' + 'A');
            }
            if (!(c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')) {
                throw new ResponseStatusException(400, "A region is like US-NY or DE");
            }
            out.append(c);
        }
        return out.toString();
    }

    /// The certificate exempting `username` at an address in `country` and
    /// `region`, or null. Only the checkout asks, for a signed-in shopper.
    /// Without a certificate, a company buyer's EU reverse charge
    /// ([ReverseCharge]) exempts the same way.
    @Transactional(readOnly = true)
    public String certificateFor(String storeId, String username, String country, String region,
            Instant now) throws IOException {
        if (username == null || country == null || country.trim().length() != 2) {
            return null;
        }
        List<TaxExemption> mine = exemptions.findByStoreIdAndUsername(storeId, username);
        String c = country.trim();
        String r = region == null ? "" : region.trim();
        for (int iter = 0; iter < mine.size(); iter++) {
            TaxExemption t = mine.get(iter);
            long expires = t.getExpiresAt().toEpochMilli();
            if (expires > 0 && expires <= now.toEpochMilli()) {
                continue;
            }
            List<String> rs = split(t.getRegions());
            for (int k = 0; k < rs.size(); k++) {
                if (covers(rs.get(k), c, r)) {
                    return t.getCertificateId();
                }
            }
        }
        return reverseCharge.certificateFor(storeId, username, c);
    }

    /// Whether `entry` (`US`, `US-NY`) covers an address in `country`/`region`.
    /// The region may be written `NY` or `US-NY`.
    static boolean covers(String entry, String country, String region) {
        if (!entry.regionMatches(true, 0, country, 0, 2)) {
            return false;
        }
        if (entry.length() == 2) {
            return true;
        }
        String want = entry.substring(3);
        String r = region.length() > 3 && region.charAt(2) == '-' ? region.substring(3) : region;
        return want.equalsIgnoreCase(r);
    }

    /// Records that `orderId` was exempted by `certificateId`.
    @Transactional(rollbackFor = IOException.class)
    public void recordUse(String storeId, String orderId, String certificateId)
            throws IOException {
        if (uses.findByOrderId(orderId) != null) {
            return;
        }
        OrderExemption u = new OrderExemption();
        u.setId(Ids.next());
        u.setStoreId(storeId);
        u.setOrderId(orderId);
        u.setCertificateId(certificateId);
        uses.insert(u);
    }

    /// The certificate that exempted an order, or null.
    @Transactional(readOnly = true)
    public String certificateOf(String orderId) throws IOException {
        OrderExemption u = uses.findByOrderId(orderId);
        return u == null ? null : u.getCertificateId();
    }

    /// The tax report over `[from, to)` (`yyyy-MM-dd`, `to` inclusive as a
    /// day), one row per country and region, sorted.
    @Transactional(readOnly = true)
    public List<TaxReportRowDto> report(String username, String storeId, String from, String to)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (!Days.isIso(from) || !Days.isIso(to) || from.compareTo(to) > 0) {
            throw new ResponseStatusException(400, "Give the period as two dates, the first first");
        }
        long start = dayStart(from);
        long end = dayStart(to) + Days.MILLIS;
        Set<String> exempt = new HashSet<String>();
        List<OrderExemption> us = uses.findByStoreId(storeId);
        for (int iter = 0; iter < us.size(); iter++) {
            exempt.add(us.get(iter).getOrderId());
        }
        List<StoreOrder> rows = orders.findByStoreIdAndCreatedAtGreaterThanEqual(storeId,
                Instant.ofEpochMilli(start));
        Map<String, long[]> sums = new LinkedHashMap<String, long[]>();
        for (int iter = 0; iter < rows.size(); iter++) {
            StoreOrder o = rows.get(iter);
            long at = o.getCreatedAt().toEpochMilli();
            if (at >= end || "CANCELLED".equals(o.getStatus())) {
                continue;
            }
            String key = (o.getShipCountry() == null ? "" : o.getShipCountry()) + "\n"
                    + (o.getShipRegion() == null ? "" : o.getShipRegion());
            long[] s = sums.get(key);
            if (s == null) {
                s = new long[4];
                sums.put(key, s);
            }
            s[0]++;
            s[1] += o.getSubtotalCents() - o.getDiscountCents();
            s[2] += o.getTaxCents();
            if (exempt.contains(o.getId())) {
                s[3]++;
            }
        }
        List<String> keys = new ArrayList<String>(sums.keySet());
        java.util.Collections.sort(keys);
        List<TaxReportRowDto> out = new ArrayList<TaxReportRowDto>();
        for (int iter = 0; iter < keys.size(); iter++) {
            String k = keys.get(iter);
            int nl = k.indexOf('\n');
            long[] s = sums.get(k);
            out.add(new TaxReportRowDto(k.substring(0, nl), k.substring(nl + 1), (int) s[0], s[1],
                    s[2], (int) s[3]));
        }
        return out;
    }

    /// Midnight UTC of a `yyyy-MM-dd` day, by the inverse of [Days#iso].
    public static long dayStart(String iso) {
        int y = Integer.parseInt(iso.substring(0, 4));
        int m = Integer.parseInt(iso.substring(5, 7));
        int d = Integer.parseInt(iso.substring(8, 10));
        long yy = m <= 2 ? y - 1 : y;
        long era = yy / 400;
        long yoe = yy - era * 400;
        long mp = m > 2 ? m - 3 : m + 9;
        long doy = (153 * mp + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long days = era * 146097 + doe - 719468;
        return days * Days.MILLIS;
    }
}
