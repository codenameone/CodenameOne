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

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.OssRowDto;
import com.codenameone.examples.bazaar.api.TaxCategoryDto;
import com.codenameone.examples.bazaar.api.TaxOverrideDto;
import com.codenameone.examples.bazaar.api.TaxSettingsDto;
import com.codenameone.examples.bazaar.api.VatRateDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.TaxRate;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import com.codenameone.examples.bazaar.pricing.TaxCalculator;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The rate each line of a sale is taxed at, beyond the store's rates per
/// region ([TaxCalculator]). For each line, the first of these that applies:
///
/// 1. **An override for the product** in the destination -- the region's
///    before the country's.
/// 2. **An override for a collection the product is in**, the same way; of
///    several collections, the lowest rate.
/// 3. **The EU VAT table**, when the store has turned it on and the
///    destination is a member state: the rate of the product's tax category
///    there (`standard`, `reduced`, `super-reduced`, `zero`), falling back to
///    the next higher category the country has; `exempt` is untaxed.
/// 4. **The store's US sales-tax provider**, for a US destination.
/// 5. **The store's own rates** for the region, as before.
///
/// A variant that is not taxable, and a buyer's exemption certificate, still
/// win over all of these: the calculator never taxes the first, and the
/// callers recompute without rules for the second.
///
/// This is also where the settings, the overrides, the EU table and the
/// One-Stop Shop report live.
@Component
public class TaxRules {
    public static final String STANDARD = "standard";
    public static final String REDUCED = "reduced";
    public static final String SUPER_REDUCED = "super-reduced";
    public static final String ZERO = "zero";
    public static final String EXEMPT = "exempt";
    /// Every product tax category.
    public static final List<String> CATEGORIES = Arrays.asList(STANDARD, REDUCED,
            SUPER_REDUCED, ZERO, EXEMPT);
    public static final String PRODUCT = "product";
    public static final String COLLECTION = "collection";
    /// The member states, by ISO code (Greece as `GR`).
    public static final List<String> EU = Arrays.asList("AT", "BE", "BG", "HR", "CY", "CZ",
            "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE");
    static final int MAX_OVERRIDES = 500;

    private final DataSource db;
    private final StoreAccess access;
    private final List<SalesTaxProvider> providers = new ArrayList<SalesTaxProvider>();

    public TaxRules(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
        providers.add(new SimulatedSalesTaxProvider());
    }

    /// The provider called `name`, or null.
    public SalesTaxProvider provider(String name) {
        for (int iter = 0; name != null && iter < providers.size(); iter++) {
            if (providers.get(iter).name().equals(name)) {
                return providers.get(iter);
            }
        }
        return null;
    }

    // --------------------------------------------------------- applying

    /// [TaxCalculator#apply] with this store's rules for each line.
    public TaxCalculator.Result apply(String storeId, List<PriceLine> lines, long shippingCents,
            List<TaxRate> rates, String country, String region, boolean inclusive)
            throws IOException {
        TaxCalculator.LineRates perLine = country == null || country.length() == 0 ? null
                : lineRates(storeId, lines, country, region);
        return TaxCalculator.apply(lines, shippingCents, rates, country, region, inclusive,
                perLine);
    }

    /// The rule for a sale to `country`/`region`, with what it needs read up
    /// front: null when the store has nothing beyond its own rates.
    public TaxCalculator.LineRates lineRates(String storeId, List<PriceLine> lines,
            String country, String region) throws IOException {
        Map settings = db.queryOne("SELECT eu_vat AS eu_vat, us_provider AS us_provider FROM "
                + "tax_settings WHERE store_id = ?", new Object[] {storeId});
        final boolean euVat = settings != null && Rows.number(settings.get("eu_vat")) != 0
                && EU.contains(upper(country));
        final SalesTaxProvider us = settings != null && "US".equalsIgnoreCase(country)
                ? provider(Rows.text(settings.get("us_provider"))) : null;
        List overrides = db.query("SELECT target_kind AS target_kind, target_id AS target_id, "
                + "region AS region, basis_points AS basis_points FROM tax_override WHERE "
                + "store_id = ? AND country = ?", new Object[] {storeId, upper(country)});
        if (!euVat && us == null && (overrides == null || overrides.isEmpty())) {
            return null;
        }
        final Map<String, Long> byProduct = new HashMap<String, Long>();
        final Map<String, Long> byCollection = new HashMap<String, Long>();
        collect(overrides, region, byProduct, byCollection);
        final Map<String, String> categories = new HashMap<String, String>();
        for (int iter = 0; iter < lines.size(); iter++) {
            String pid = lines.get(iter).productId;
            if (pid != null && !categories.containsKey(pid)) {
                Map r = db.queryOne("SELECT tax_category AS c FROM product WHERE id = ?",
                        new Object[] {pid});
                categories.put(pid, r == null ? STANDARD : Rows.text(r.get("c")));
            }
        }
        final Map<String, Long> vat = new HashMap<String, Long>();
        if (euVat) {
            List rows = db.query("SELECT category AS category, basis_points AS bp FROM "
                    + "eu_vat_rate WHERE country = ?", new Object[] {upper(country)});
            for (int iter = 0; rows != null && iter < rows.size(); iter++) {
                Map r = (Map) rows.get(iter);
                vat.put(Rows.text(r.get("category")), Long.valueOf(Rows.number(r.get("bp"))));
            }
        }
        final String state = region;
        return (line, cartRate) -> {
            Long own = byProduct.get(line.productId);
            if (own != null) {
                return own.longValue();
            }
            long lowest = -1;
            for (int iter = 0; iter < line.collectionIds.size(); iter++) {
                Long c = byCollection.get(line.collectionIds.get(iter));
                if (c != null && (lowest < 0 || c.longValue() < lowest)) {
                    lowest = c.longValue();
                }
            }
            if (lowest >= 0) {
                return lowest;
            }
            String category = categories.get(line.productId);
            if (euVat && !vat.isEmpty()) {
                return vatRate(vat, category);
            }
            if (us != null) {
                long bp = us.basisPoints(state, category);
                if (bp >= 0) {
                    return bp;
                }
            }
            return cartRate;
        };
    }

    /// The EU rate of `category` from one country's table: the category's
    /// own, else the next higher one the country has; exempt is nothing.
    static long vatRate(Map<String, Long> table, String category) {
        String c = category == null || !CATEGORIES.contains(category) ? STANDARD : category;
        if (EXEMPT.equals(c)) {
            return 0;
        }
        String[] chain = ZERO.equals(c) ? new String[] {ZERO, SUPER_REDUCED, REDUCED, STANDARD}
                : SUPER_REDUCED.equals(c) ? new String[] {SUPER_REDUCED, REDUCED, STANDARD}
                : REDUCED.equals(c) ? new String[] {REDUCED, STANDARD} : new String[] {STANDARD};
        for (int iter = 0; iter < chain.length; iter++) {
            Long bp = table.get(chain[iter]);
            if (bp != null) {
                return bp.longValue();
            }
        }
        return 0;
    }

    /// The overrides that apply in `region`, by target: a region's own rule
    /// beats a country-wide one.
    static void collect(List overrides, String region, Map<String, Long> byProduct,
            Map<String, Long> byCollection) {
        Map<String, Boolean> fromRegion = new HashMap<String, Boolean>();
        for (int iter = 0; overrides != null && iter < overrides.size(); iter++) {
            Map r = (Map) overrides.get(iter);
            String rr = Rows.text(r.get("region"));
            boolean regional = rr.length() > 0;
            if (regional && (region == null || !rr.equalsIgnoreCase(region.trim()))) {
                continue;
            }
            String kind = Rows.text(r.get("target_kind"));
            String key = kind + ":" + Rows.text(r.get("target_id"));
            if (!regional && Boolean.TRUE.equals(fromRegion.get(key))) {
                continue;
            }
            fromRegion.put(key, Boolean.valueOf(regional));
            Long bp = Long.valueOf(Rows.number(r.get("basis_points")));
            (PRODUCT.equals(kind) ? byProduct : byCollection).put(Rows.text(r.get("target_id")),
                    bp);
        }
    }

    // ---------------------------------------------------------- settings

    @Transactional(readOnly = true)
    public TaxSettingsDto settings(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return settingsOf(storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    public TaxSettingsDto saveSettings(String username, String storeId, TaxSettingsDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String home = upper(wanted.homeCountry() == null ? "" : wanted.homeCountry().trim());
        if (home.length() != 0 && home.length() != 2) {
            throw new ResponseStatusException(400, "The home country is a two-letter code");
        }
        if (wanted.euVat() && !EU.contains(home)) {
            throw new ResponseStatusException(400, "The EU VAT table needs an EU home country");
        }
        String us = wanted.usProvider() == null ? "" : wanted.usProvider().trim();
        if (us.length() > 0 && provider(us) == null) {
            throw new ResponseStatusException(400, "There is no sales-tax provider "
                    + Codes.cut(us, 40));
        }
        long now = System.currentTimeMillis();
        Integer eu = Integer.valueOf(wanted.euVat() ? 1 : 0);
        int changed = db.execute("UPDATE tax_settings SET eu_vat = ?, home_country = ?, "
                + "us_provider = ?, updated_at = ? WHERE store_id = ?", new Object[] {eu, home, us,
                    Long.valueOf(now), storeId});
        if (changed == 0) {
            db.execute("INSERT INTO tax_settings (store_id, eu_vat, home_country, us_provider, "
                    + "updated_at) VALUES (?, ?, ?, ?, ?)", new Object[] {storeId, eu, home, us,
                        Long.valueOf(now)});
        }
        return settingsOf(storeId);
    }

    private TaxSettingsDto settingsOf(String storeId) throws IOException {
        Map r = db.queryOne("SELECT eu_vat AS eu_vat, home_country AS home_country, "
                + "us_provider AS us_provider FROM tax_settings WHERE store_id = ?",
                new Object[] {storeId});
        if (r == null) {
            return new TaxSettingsDto(false, "", "");
        }
        return new TaxSettingsDto(Rows.number(r.get("eu_vat")) != 0,
                Rows.text(r.get("home_country")), Rows.text(r.get("us_provider")));
    }

    // --------------------------------------------------------- overrides

    @Transactional(readOnly = true)
    public List<TaxOverrideDto> overrides(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return overridesOf(storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    public TaxOverrideDto saveOverride(String username, String storeId, TaxOverrideDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String kind = wanted.targetKind() == null ? "" : wanted.targetKind().trim();
        if (!PRODUCT.equals(kind) && !COLLECTION.equals(kind)) {
            throw new ResponseStatusException(400, "An override is for a product or a collection");
        }
        String table = PRODUCT.equals(kind) ? "product" : "collection";
        Map target = wanted.targetId() == null ? null : db.queryOne("SELECT store_id AS store_id "
                + "FROM " + table + " WHERE id = ?", new Object[] {wanted.targetId()});
        StoreAccess.requireIn(target == null ? null : Rows.text(target.get("store_id")), storeId,
                kind);
        String country = upper(wanted.country() == null ? "" : wanted.country().trim());
        if (country.length() != 2) {
            throw new ResponseStatusException(400, "The country is a two-letter code");
        }
        String region = wanted.region() == null ? "" : wanted.region().trim();
        if (region.length() > 80) {
            throw new ResponseStatusException(400, "The region is too long");
        }
        if (wanted.basisPoints() < 0 || wanted.basisPoints() > 5000) {
            throw new ResponseStatusException(400, "The rate is between 0% and 50%");
        }
        Map count = db.queryOne("SELECT COUNT(*) AS n FROM tax_override WHERE store_id = ?",
                new Object[] {storeId});
        if (count != null && Rows.number(count.get("n")) >= MAX_OVERRIDES) {
            throw new ResponseStatusException(400, "Remove an override before adding another");
        }
        // One rule per target and place: saving again replaces it.
        db.execute("DELETE FROM tax_override WHERE store_id = ? AND target_kind = ? AND "
                + "target_id = ? AND country = ? AND region = ?", new Object[] {storeId, kind,
                    wanted.targetId(), country, region});
        String id = Ids.next();
        db.execute("INSERT INTO tax_override (id, store_id, target_kind, target_id, country, "
                + "region, basis_points, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[] {id, storeId, kind, wanted.targetId(), country, region,
                    Integer.valueOf(wanted.basisPoints()), Long.valueOf(System.currentTimeMillis())});
        List<TaxOverrideDto> all = overridesOf(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (id.equals(all.get(iter).id())) {
                return all.get(iter);
            }
        }
        throw new IllegalStateException("The override was not saved");
    }

    @Transactional(rollbackFor = IOException.class)
    public List<TaxOverrideDto> removeOverride(String username, String storeId, String overrideId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Map r = overrideId == null ? null : db.queryOne("SELECT store_id AS store_id FROM "
                + "tax_override WHERE id = ?", new Object[] {overrideId});
        StoreAccess.requireIn(r == null ? null : Rows.text(r.get("store_id")), storeId,
                "override");
        db.execute("DELETE FROM tax_override WHERE id = ?", new Object[] {overrideId});
        return overridesOf(storeId);
    }

    private List<TaxOverrideDto> overridesOf(String storeId) throws IOException {
        List rows = db.query("SELECT o.id AS id, o.target_kind AS target_kind, "
                + "o.target_id AS target_id, o.country AS country, o.region AS region, "
                + "o.basis_points AS basis_points, p.title AS ptitle, c.title AS ctitle "
                + "FROM tax_override o LEFT JOIN product p ON p.id = o.target_id "
                + "LEFT JOIN collection c ON c.id = o.target_id WHERE o.store_id = ? "
                + "ORDER BY o.country, o.region, o.created_at", new Object[] {storeId});
        List<TaxOverrideDto> out = new ArrayList<TaxOverrideDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String kind = Rows.text(r.get("target_kind"));
            out.add(new TaxOverrideDto(Rows.text(r.get("id")), kind,
                    Rows.text(r.get("target_id")),
                    Rows.text(r.get(PRODUCT.equals(kind) ? "ptitle" : "ctitle")),
                    Rows.text(r.get("country")), Rows.text(r.get("region")),
                    (int) Rows.number(r.get("basis_points"))));
        }
        return out;
    }

    // -------------------------------------------------------- categories

    @Transactional(rollbackFor = IOException.class)
    public TaxCategoryDto setCategory(String username, String storeId, String productId,
            TaxCategoryDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Map p = productId == null ? null : db.queryOne("SELECT store_id AS store_id FROM product "
                + "WHERE id = ?", new Object[] {productId});
        StoreAccess.requireIn(p == null ? null : Rows.text(p.get("store_id")), storeId, "product");
        String c = wanted == null || wanted.category() == null ? "" : wanted.category().trim();
        if (!CATEGORIES.contains(c)) {
            throw new ResponseStatusException(400, "The category is one of " + CATEGORIES);
        }
        db.execute("UPDATE product SET tax_category = ? WHERE id = ?", new Object[] {c, productId});
        return new TaxCategoryDto(c);
    }

    // ------------------------------------------------------------ EU table

    @Transactional(readOnly = true)
    public List<VatRateDto> vatRates(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return vatTable();
    }

    public List<VatRateDto> vatTable() throws IOException {
        List rows = db.query("SELECT country AS country, category AS category, "
                + "basis_points AS bp FROM eu_vat_rate ORDER BY country, basis_points DESC",
                new Object[0]);
        List<VatRateDto> out = new ArrayList<VatRateDto>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.add(new VatRateDto(Rows.text(r.get("country")), Rows.text(r.get("category")),
                    (int) Rows.number(r.get("bp"))));
        }
        return out;
    }

    /// Sets one rate of the EU table, for the platform's administrators
    /// (the route is under `/api/admin`); a negative rate removes it.
    @Transactional(rollbackFor = IOException.class)
    public VatRateDto saveVatRate(VatRateDto wanted) throws IOException {
        String country = upper(wanted == null || wanted.country() == null ? ""
                : wanted.country().trim());
        if (!EU.contains(country)) {
            throw new ResponseStatusException(400, "That is not an EU member state");
        }
        String c = wanted.category() == null ? "" : wanted.category().trim();
        if (!CATEGORIES.contains(c) || EXEMPT.equals(c)) {
            throw new ResponseStatusException(400, "The category is standard, reduced, "
                    + "super-reduced or zero");
        }
        if (wanted.basisPoints() > 5000) {
            throw new ResponseStatusException(400, "The rate is at most 50%");
        }
        db.execute("DELETE FROM eu_vat_rate WHERE country = ? AND category = ?",
                new Object[] {country, c});
        if (wanted.basisPoints() >= 0) {
            db.execute("INSERT INTO eu_vat_rate (country, category, basis_points, updated_at) "
                    + "VALUES (?, ?, ?, ?)", new Object[] {country, c,
                        Integer.valueOf(wanted.basisPoints()),
                        Long.valueOf(System.currentTimeMillis())});
        }
        return new VatRateDto(country, c, wanted.basisPoints());
    }

    // ---------------------------------------------------------- OSS report

    /// The One-Stop Shop return's figures: sales to consumers in other EU
    /// countries than the store's home, per country, rate and currency, net
    /// of refunds, from `from` to `to` (epoch milliseconds, `to` exclusive).
    /// Lines from before rates were kept per line are left out.
    @Transactional(readOnly = true)
    public List<OssRowDto> ossReport(String username, String storeId, long from, long to)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        String home = settingsOf(storeId).homeCountry();
        Map store = db.queryOne("SELECT tax_inclusive AS inc FROM store WHERE id = ?",
                new Object[] {storeId});
        boolean inclusive = store != null && Rows.number(store.get("inc")) != 0;
        List rows = db.query("SELECT o.id AS oid, o.ship_country AS country, o.currency AS currency, "
                + "l.tax_bp AS bp, l.quantity AS qty, l.refunded_qty AS refunded, "
                + "l.price_cents AS price, l.discount_cents AS discount, l.tax_cents AS tax "
                + "FROM store_order o JOIN order_line l ON l.order_id = o.id "
                + "WHERE o.store_id = ? AND o.created_at >= ? AND o.created_at < ? "
                + "AND o.status <> 'CANCELLED' AND o.financial IN ('AUTHORIZED', 'PAID', 'PARTIALLY_REFUNDED', "
                + "'REFUNDED') AND l.tax_bp >= 0 ORDER BY o.ship_country",
                new Object[] {storeId, Long.valueOf(from), Long.valueOf(to)});
        Map<String, long[]> sums = new LinkedHashMap<String, long[]>();
        Map<String, Map<String, Boolean>> ordersOf = new HashMap<String, Map<String, Boolean>>();
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            String country = upper(Rows.text(r.get("country")));
            if (!EU.contains(country) || country.equals(home)) {
                continue;
            }
            long qty = Rows.number(r.get("qty"));
            long kept = qty - Rows.number(r.get("refunded"));
            if (qty <= 0 || kept <= 0) {
                continue;
            }
            long gross = Rows.number(r.get("price")) * qty - Rows.number(r.get("discount"));
            long tax = Rows.number(r.get("tax")) * kept / qty;
            long net = gross * kept / qty - (inclusive ? tax : 0);
            String key = country + "|" + Rows.number(r.get("bp")) + "|"
                    + Rows.text(r.get("currency"));
            long[] s = sums.get(key);
            if (s == null) {
                s = new long[2];
                sums.put(key, s);
                ordersOf.put(key, new HashMap<String, Boolean>());
            }
            s[0] += net;
            s[1] += tax;
            ordersOf.get(key).put(Rows.text(r.get("oid")), Boolean.TRUE);
        }
        List<OssRowDto> out = new ArrayList<OssRowDto>();
        for (Map.Entry<String, long[]> e : sums.entrySet()) {
            String[] parts = splitKey(e.getKey());
            out.add(new OssRowDto(parts[0], Integer.parseInt(parts[1]), parts[2],
                    ordersOf.get(e.getKey()).size(), e.getValue()[0], e.getValue()[1]));
        }
        return out;
    }

    /// The report as CSV, one row per country, rate and currency.
    public static String ossCsv(List<OssRowDto> rows) {
        StringBuilder b = new StringBuilder("country,vat_rate_percent,currency,orders,"
                + "net_amount,vat_amount\n");
        for (int iter = 0; iter < rows.size(); iter++) {
            OssRowDto r = rows.get(iter);
            b.append(r.country()).append(',').append(percent(r.basisPoints())).append(',')
                    .append(r.currency()).append(',').append(r.orders()).append(',')
                    .append(money(r.netCents(), r.currency())).append(',')
                    .append(money(r.taxCents(), r.currency()))
                    .append('\n');
        }
        return b.toString();
    }

    static String percent(int bp) {
        String frac = Integer.toString(bp % 100 + 100).substring(1);
        return (bp / 100) + "." + frac;
    }

    /// Minor units in `currency`'s own decimals.
    static String money(long cents, String currency) {
        return com.codenameone.examples.bazaar.ops.Amounts.plain(cents, currency);
    }

    private static String[] splitKey(String key) {
        int a = key.indexOf('|');
        int b = key.indexOf('|', a + 1);
        return new String[] {key.substring(0, a), key.substring(a + 1, b), key.substring(b + 1)};
    }

    /// ASCII upper case, for country codes; see the project's rule on case.
    static String upper(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            b.append(c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c);
        }
        return b.toString();
    }
}
