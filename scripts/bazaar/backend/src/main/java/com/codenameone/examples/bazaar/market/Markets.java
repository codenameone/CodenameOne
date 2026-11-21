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
package com.codenameone.examples.bazaar.market;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.CustomsCodeDto;
import com.codenameone.examples.bazaar.api.DutyRateDto;
import com.codenameone.examples.bazaar.api.FxRateDto;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.api.MarketPriceDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.CustomsCode;
import com.codenameone.examples.bazaar.domain.DutyRate;
import com.codenameone.examples.bazaar.domain.FxRate;
import com.codenameone.examples.bazaar.domain.Market;
import com.codenameone.examples.bazaar.domain.MarketPrice;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.repository.CustomsCodeRepository;
import com.codenameone.examples.bazaar.repository.DutyRateRepository;
import com.codenameone.examples.bazaar.repository.FxRateRepository;
import com.codenameone.examples.bazaar.repository.MarketPriceRepository;
import com.codenameone.examples.bazaar.repository.MarketRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Markets: the countries a store sells to grouped under one currency and
/// language, with the prices shown there.
///
/// **Prices.** A variant's price in a market is its fixed price there when
/// the merchant set one ([MarketPrice]); otherwise the store price converted
/// at the merchant's own rate ([FxRate], millionths of the market currency
/// per unit of the store's -- there is no live feed), plus the market's
/// adjustment in basis points, then rounded: `NONE`, `WHOLE` (up to the next
/// whole unit) or `NINETY_NINE` (up to the next `.99`). Everything is integer
/// arithmetic on cents, rounded half up, with every product checked for
/// overflow.
///
/// **What is charged.** Market prices are what the storefront shows, and the
/// checkout charges them in the market currency: the order keeps the
/// presentment currency, total and rate, and refunds go back in that
/// currency (see `Checkouts`).
///
/// **Duties.** A product may carry an HS code ([CustomsCode]) and the
/// merchant keeps import rates per destination country and code prefix
/// ([DutyRate]); the longest prefix wins. [#dutyEstimate] is what the
/// checkout shows as the duties a buyer may pay on delivery -- an estimate,
/// not a charge.
///
/// Currencies are two-decimal ones only ([#CURRENCIES]): the storefront's
/// money formatting has no zero- or three-decimal case.
@Component
public class Markets {
    public static final String NONE = "NONE";
    public static final String WHOLE = "WHOLE";
    /// A market priced at the merchant's own rate.
    public static final String MANUAL = "manual";
    /// A market priced at the European Central Bank's daily rate ([FxFeed]).
    public static final String ECB = "ecb";
    public static final String NINETY_NINE = "NINETY_NINE";
    static final long MICROS = 1000000L;
    /// The highest rate accepted: a million units per unit of the store's.
    static final long MAX_RATE = MICROS * MICROS;
    static final int MAX_MARKETS = 50;
    /// The currencies a market may use: all with two decimals.
    static final String[] CURRENCIES = {
        "USD", "EUR", "GBP", "CAD", "AUD", "NZD", "CHF", "SEK", "NOK", "DKK", "PLN", "CZK",
        "ILS", "MXN", "BRL", "INR", "SGD", "HKD", "ZAR",
    };

    private final MarketRepository markets;
    private final MarketPriceRepository prices;
    private final FxRateRepository rates;
    private final CustomsCodeRepository customs;
    private final DutyRateRepository duties;
    private final VariantRepository variants;
    private final ProductRepository products;
    private final StoreAccess access;

    public Markets(MarketRepository markets, MarketPriceRepository prices, FxRateRepository rates,
            CustomsCodeRepository customs, DutyRateRepository duties, VariantRepository variants,
            ProductRepository products, StoreAccess access) {
        this.markets = markets;
        this.prices = prices;
        this.rates = rates;
        this.customs = customs;
        this.duties = duties;
        this.variants = variants;
        this.products = products;
        this.access = access;
    }

    // -------------------------------------------------------------- markets

    @Transactional(readOnly = true)
    public List<MarketDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dtos(markets.findByStoreIdOrderByPositionAsc(storeId));
    }

    /// The active markets of a store, for the storefront's picker.
    @Transactional(readOnly = true)
    public List<MarketDto> active(String storeId) throws IOException {
        List<Market> all = markets.findByStoreIdOrderByPositionAsc(storeId);
        List<Market> on = new ArrayList<Market>();
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).isActive()) {
                on.add(all.get(iter));
            }
        }
        return dtos(on);
    }

    private static List<MarketDto> dtos(List<Market> rows) {
        List<MarketDto> out = new ArrayList<MarketDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    public static MarketDto dto(Market m) {
        return new MarketDto(m.getId(), m.getCode(), m.getName(), m.getCurrency(),
                countries(m.getCountries()), m.getLocale(), m.getRounding(), m.getAdjustBps(),
                m.isActive(), fxSource(m.getFxSource()));
    }

    /// A stored choice as [#MANUAL] or [#ECB]: rows from before there was a
    /// choice say nothing, and are manual.
    static String fxSource(String stored) {
        return ECB.equals(stored) ? ECB : MANUAL;
    }

    /// "DE,FR" as a list, without a regular expression.
    static List<String> countries(String text) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        while (start < text.length()) {
            int comma = text.indexOf(',', start);
            int end = comma < 0 ? text.length() : comma;
            String c = text.substring(start, end).trim();
            if (c.length() > 0) {
                out.add(c);
            }
            start = end + 1;
        }
        return out;
    }

    @Transactional(readOnly = true)
    public MarketDto detail(String username, String storeId, String marketId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(market(storeId, marketId));
    }

    Market market(String storeId, String marketId) throws IOException {
        Market m = marketId == null ? null : markets.findById(marketId);
        if (m == null || !storeId.equals(m.getStoreId())) {
            throw new ResponseStatusException(404, "No such market");
        }
        return m;
    }

    /// Creates (`marketId` null) or updates a market.
    @Transactional(rollbackFor = IOException.class)
    public MarketDto save(String username, String storeId, String marketId, MarketDto in)
            throws IOException {
        Store store = access.requireStore(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Describe the market");
        }
        String code = code(in.code());
        String name = Codes.cut(in.name(), 80);
        if (name.length() == 0) {
            throw new ResponseStatusException(400, "Name the market");
        }
        String currency = currency(in.currency());
        String fxSource = in.fxSource() == null || in.fxSource().trim().length() == 0 ? MANUAL
                : in.fxSource().trim();
        if (!MANUAL.equals(fxSource) && !ECB.equals(fxSource)) {
            throw new ResponseStatusException(400, "The exchange rate is manual or ecb");
        }
        // A market on the feed needs no rate of the merchant's: until the feed has
        // one it shows the store's own prices, as a market whose rate is gone does.
        if (MANUAL.equals(fxSource) && !currency.equals(store.getCurrency()) && rates.findByStoreIdAndCurrency(storeId,
                currency) == null) {
            throw new ResponseStatusException(400, "Add an exchange rate for " + currency
                    + " first");
        }
        String rounding = in.rounding() == null || in.rounding().length() == 0 ? NONE
                : in.rounding().trim();
        if (!NONE.equals(rounding) && !WHOLE.equals(rounding) && !NINETY_NINE.equals(rounding)) {
            throw new ResponseStatusException(400, "Rounding is NONE, WHOLE or NINETY_NINE");
        }
        if (in.adjustBps() < -5000 || in.adjustBps() > 10000) {
            throw new ResponseStatusException(400, "An adjustment is between -50% and 100%");
        }
        StringBuilder cs = new StringBuilder();
        List<String> given = in.countries() == null ? new ArrayList<String>() : in.countries();
        for (int iter = 0; iter < given.size(); iter++) {
            String c = country(given.get(iter));
            if (cs.indexOf(c) < 0) {
                if (cs.length() > 0) {
                    cs.append(',');
                }
                cs.append(c);
            }
        }
        if (cs.length() == 0) {
            throw new ResponseStatusException(400, "A market serves at least one country");
        }
        if (cs.length() > 400) {
            throw new ResponseStatusException(400, "Too many countries");
        }
        String locale = in.locale() == null || in.locale().trim().length() == 0 ? ""
                : Locales.normalize(in.locale());
        Market other = markets.findByStoreIdAndCode(storeId, code);
        Market m = marketId == null ? null : market(storeId, marketId);
        if (other != null && (m == null || !other.getId().equals(m.getId()))) {
            throw new ResponseStatusException(409, "Another market already has the code " + code);
        }
        boolean fresh = m == null;
        if (fresh) {
            List<Market> all = markets.findByStoreIdOrderByPositionAsc(storeId);
            if (all.size() >= MAX_MARKETS) {
                throw new ResponseStatusException(409, "A store has at most " + MAX_MARKETS
                        + " markets");
            }
            m = new Market();
            m.setId(Ids.next());
            m.setStoreId(storeId);
            m.setPosition(all.size());
            m.setCreatedAt(Instant.now());
        } else if (!m.getCurrency().equals(currency)) {
            // Fixed prices were written in the old currency.
            prices.deleteByMarketId(m.getId());
        }
        m.setCode(code);
        m.setName(name);
        m.setCurrency(currency);
        m.setCountries(cs.toString());
        m.setLocale(locale);
        m.setRounding(rounding);
        m.setAdjustBps(in.adjustBps());
        m.setActive(in.active());
        m.setFxSource(fxSource);
        if (fresh) {
            markets.insert(m);
        } else {
            markets.save(m);
        }
        return dto(m);
    }

    @Transactional(rollbackFor = IOException.class)
    public void remove(String username, String storeId, String marketId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Market m = market(storeId, marketId);
        prices.deleteByMarketId(m.getId());
        markets.delete(m);
    }

    /// A market code: 2 to 16 lower-case letters, digits or dashes; it is
    /// what `?market=` carries.
    static String code(String text) {
        String c = text == null ? "" : text.trim();
        if (c.length() < 2 || c.length() > 16) {
            throw new ResponseStatusException(400, "A market code is 2 to 16 characters");
        }
        StringBuilder out = new StringBuilder(c.length());
        for (int iter = 0; iter < c.length(); iter++) {
            char ch = c.charAt(iter);
            if (ch >= 'A' && ch <= 'Z') {
                ch = (char) (ch - 'A' + 'a');
            }
            if (!(ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '-')) {
                throw new ResponseStatusException(400, "A market code is letters, digits and "
                        + "dashes");
            }
            out.append(ch);
        }
        return out.toString();
    }

    /// A two-letter country code, upper case.
    public static String country(String text) {
        String c = text == null ? "" : text.trim();
        if (c.length() != 2) {
            throw new ResponseStatusException(400, "Countries are two-letter codes");
        }
        StringBuilder out = new StringBuilder(2);
        for (int iter = 0; iter < 2; iter++) {
            char ch = c.charAt(iter);
            if (ch >= 'a' && ch <= 'z') {
                ch = (char) (ch - 'a' + 'A');
            }
            if (ch < 'A' || ch > 'Z') {
                throw new ResponseStatusException(400, "Countries are two-letter codes");
            }
            out.append(ch);
        }
        return out.toString();
    }

    static String currency(String text) {
        String c = text == null ? "" : text.trim();
        for (int iter = 0; iter < CURRENCIES.length; iter++) {
            if (CURRENCIES[iter].equalsIgnoreCase(c)) {
                return CURRENCIES[iter];
            }
        }
        throw new ResponseStatusException(400, "Choose a two-decimal currency such as EUR or GBP");
    }

    // --------------------------------------------------------------- prices

    @Transactional(readOnly = true)
    public List<MarketPriceDto> prices(String username, String storeId, String marketId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<MarketPrice> rows = prices.findByMarketId(market(storeId, marketId).getId());
        List<MarketPriceDto> out = new ArrayList<MarketPriceDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            MarketPrice p = rows.get(iter);
            out.add(new MarketPriceDto(p.getId(), p.getVariantId(), p.getPriceCents()));
        }
        return out;
    }

    /// Sets a variant's fixed price in a market; zero or less removes it.
    @Transactional(rollbackFor = IOException.class)
    public List<MarketPriceDto> putPrice(String username, String storeId, String marketId,
            MarketPriceDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Market m = market(storeId, marketId);
        Variant v = in == null || in.variantId() == null ? null : variants.findById(in.variantId());
        if (v == null || !storeId.equals(v.getStoreId())) {
            throw new ResponseStatusException(404, "No such variant");
        }
        if (in.priceCents() > 100000000000L) {
            throw new ResponseStatusException(400, "That price is too large");
        }
        MarketPrice p = prices.findByMarketIdAndVariantId(m.getId(), v.getId());
        if (in.priceCents() <= 0) {
            if (p != null) {
                prices.delete(p);
            }
        } else if (p == null) {
            p = new MarketPrice();
            p.setId(Ids.next());
            p.setStoreId(storeId);
            p.setMarketId(m.getId());
            p.setVariantId(v.getId());
            p.setPriceCents(in.priceCents());
            prices.insert(p);
        } else {
            p.setPriceCents(in.priceCents());
            prices.save(p);
        }
        return prices(username, storeId, marketId);
    }

    // ------------------------------------------------------------- FX rates

    @Transactional(readOnly = true)
    public List<FxRateDto> rates(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<FxRate> rows = rates.findByStoreIdOrderByCurrencyAsc(storeId);
        List<FxRateDto> out = new ArrayList<FxRateDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            FxRate r = rows.get(iter);
            out.add(new FxRateDto(r.getCurrency(), r.getRateMicros(), r.getFeedMicros(),
                    r.getFeedSource(), r.getFeedAt()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<FxRateDto> putRate(String username, String storeId, FxRateDto in)
            throws IOException {
        Store store = access.requireStore(username, storeId, StoreAccess.MANAGERS);
        String currency = currency(in == null ? null : in.currency());
        if (currency.equals(store.getCurrency())) {
            throw new ResponseStatusException(400, "The store currency needs no rate");
        }
        if (in.rateMicros() <= 0 || in.rateMicros() > MAX_RATE) {
            throw new ResponseStatusException(400, "A rate is more than zero");
        }
        FxRate r = rates.findByStoreIdAndCurrency(storeId, currency);
        boolean fresh = r == null;
        if (fresh) {
            r = new FxRate();
            r.setId(Ids.next());
            r.setStoreId(storeId);
            r.setCurrency(currency);
        }
        r.setRateMicros(in.rateMicros());
        r.setUpdatedAt(Instant.now());
        if (fresh) {
            rates.insert(r);
        } else {
            rates.save(r);
        }
        return rates(username, storeId);
    }

    // --------------------------------------------------------------- duties

    @Transactional(readOnly = true)
    public List<DutyRateDto> duties(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<DutyRate> rows = duties.findByStoreIdOrderByCountryAsc(storeId);
        List<DutyRateDto> out = new ArrayList<DutyRateDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            DutyRate d = rows.get(iter);
            out.add(new DutyRateDto(d.getId(), d.getCountry(), d.getHsPrefix(), d.getRateBps()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<DutyRateDto> addDuty(String username, String storeId, DutyRateDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Describe the duty");
        }
        if (in.rateBps() < 0 || in.rateBps() > 10000) {
            throw new ResponseStatusException(400, "A duty rate is between 0 and 100%");
        }
        DutyRate d = new DutyRate();
        d.setId(Ids.next());
        d.setStoreId(storeId);
        d.setCountry(country(in.country()));
        d.setHsPrefix(hs(in.hsPrefix(), true));
        d.setRateBps(in.rateBps());
        duties.insert(d);
        return duties(username, storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<DutyRateDto> removeDuty(String username, String storeId, String dutyId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        DutyRate d = dutyId == null ? null : duties.findById(dutyId);
        if (d == null || !storeId.equals(d.getStoreId())) {
            throw new ResponseStatusException(404, "No such duty rate");
        }
        duties.delete(d);
        return duties(username, storeId);
    }

    @Transactional(readOnly = true)
    public List<CustomsCodeDto> customsCodes(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<CustomsCode> rows = customs.findByStoreId(storeId);
        List<CustomsCodeDto> out = new ArrayList<CustomsCodeDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(new CustomsCodeDto(rows.get(iter).getProductId(), rows.get(iter).getHsCode()));
        }
        return out;
    }

    /// Sets a product's HS code; an empty one removes it.
    @Transactional(rollbackFor = IOException.class)
    public List<CustomsCodeDto> putCustomsCode(String username, String storeId, CustomsCodeDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product p = in == null || in.productId() == null ? null : products.findById(in.productId());
        if (p == null || !storeId.equals(p.getStoreId())) {
            throw new ResponseStatusException(404, "No such product");
        }
        String hs = hs(in.hsCode(), true);
        CustomsCode c = customs.findByProductId(p.getId());
        if (hs.length() == 0) {
            if (c != null) {
                customs.delete(c);
            }
        } else if (c == null) {
            c = new CustomsCode();
            c.setId(Ids.next());
            c.setStoreId(storeId);
            c.setProductId(p.getId());
            c.setHsCode(hs);
            customs.insert(c);
        } else {
            c.setHsCode(hs);
            customs.save(c);
        }
        return customsCodes(username, storeId);
    }

    /// Digits only (dots and spaces dropped), up to 12.
    static String hs(String text, boolean emptyOk) {
        String t = text == null ? "" : text;
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c >= '0' && c <= '9') {
                out.append(c);
            } else if (c != '.' && c != ' ') {
                throw new ResponseStatusException(400, "An HS code is digits");
            }
        }
        if (out.length() > 12 || out.length() == 0 && !emptyOk) {
            throw new ResponseStatusException(400, "An HS code is up to 12 digits");
        }
        return out.toString();
    }

    // ------------------------------------------------------------ the maths

    /// What a shopper sees a variant cost in a market.
    public static final class Pricer {
        final Market market;
        final long rateMicros;
        final String source;
        final Map<String, Long> fixed = new HashMap<String, Long>();

        Pricer(Market market, long rateMicros, String source) {
            this.market = market;
            this.rateMicros = rateMicros;
            this.source = source;
        }

        /// Where [#rateMicros] came from: [#MANUAL] or [#ECB]; kept on the order.
        public String source() {
            return source;
        }

        public String currency() {
            return market.getCurrency();
        }

        public Market market() {
            return market;
        }

        /// Store currency to market currency, in millionths, locked when the pricer was made.
        public long rateMicros() {
            return rateMicros;
        }

        /// The price of `variantId` whose store price is `storeCents`.
        public long price(String variantId, long storeCents) {
            Long f = fixed.get(variantId);
            if (f != null) {
                return f.longValue();
            }
            return convert(storeCents, rateMicros, market.getAdjustBps(), market.getRounding());
        }
    }

    /// The pricer of market `code` at a store, or null for no such active
    /// market (or one whose rate is gone): the store's own prices then.
    @Transactional(readOnly = true)
    public Pricer pricer(Store store, String code) throws IOException {
        if (code == null || code.length() == 0) {
            return null;
        }
        Market m = markets.findByStoreIdAndCode(store.getId(), code);
        if (m == null || !m.isActive()) {
            return null;
        }
        long rate = MICROS;
        String source = fxSource(m.getFxSource());
        if (!m.getCurrency().equals(store.getCurrency())) {
            FxRate r = rates.findByStoreIdAndCurrency(store.getId(), m.getCurrency());
            rate = r == null ? 0 : ECB.equals(source) ? r.getFeedMicros() : r.getRateMicros();
            if (rate <= 0) {
                return null;
            }
        }
        Pricer p = new Pricer(m, rate, source);
        List<MarketPrice> rows = prices.findByMarketId(m.getId());
        for (int iter = 0; iter < rows.size(); iter++) {
            p.fixed.put(rows.get(iter).getVariantId(), Long.valueOf(rows.get(iter).getPriceCents()));
        }
        return p;
    }

    /// A rate locked for an order taken in another currency: units of that
    /// currency per store unit, in millionths, and where it came from
    /// ([#MANUAL] or [#ECB]).
    public static final class LockedRate {
        public final long micros;
        public final String source;

        LockedRate(long micros, String source) {
            this.micros = micros;
            this.source = source;
        }
    }

    /// The rate a store converts `currency` at, for an order that arrived
    /// already priced in it (a marketplace order): the source an active
    /// market selling in `currency` chose, else the ECB feed when it has a
    /// rate, else the manual rate. Null when the store has no rate for it.
    @Transactional(readOnly = true)
    public LockedRate rateFor(String storeId, String currency) throws IOException {
        if (currency == null || currency.length() != 3) {
            return null;
        }
        FxRate r = rates.findByStoreIdAndCurrency(storeId, currency);
        if (r == null) {
            return null;
        }
        String wanted = null;
        List<Market> all = markets.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size() && wanted == null; iter++) {
            Market m = all.get(iter);
            if (m.isActive() && currency.equals(m.getCurrency())) {
                wanted = fxSource(m.getFxSource());
            }
        }
        if (!MANUAL.equals(wanted) && r.getFeedMicros() > 0) {
            return new LockedRate(r.getFeedMicros(), ECB);
        }
        if (r.getRateMicros() > 0) {
            return new LockedRate(r.getRateMicros(), MANUAL);
        }
        return r.getFeedMicros() > 0 ? new LockedRate(r.getFeedMicros(), ECB) : null;
    }

    /// The active market that serves `country`, or null.
    @Transactional(readOnly = true)
    public Market forCountry(String storeId, String country) throws IOException {
        if (country == null || country.trim().length() != 2) {
            return null;
        }
        List<Market> all = markets.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            Market m = all.get(iter);
            List<String> cs = countries(m.getCountries());
            for (int c = 0; c < cs.size(); c++) {
                if (m.isActive() && cs.get(c).equalsIgnoreCase(country.trim())) {
                    return m;
                }
            }
        }
        return null;
    }

    /// `cents` at `rateMicros`, adjusted by `adjustBps` and rounded by `rounding`.
    /// Half up at each step; an amount whose product overflows answers 400.
    public static long convert(long cents, long rateMicros, int adjustBps, String rounding) {
        if (rateMicros <= 0) {
            throw new ResponseStatusException(400, "A rate is more than zero");
        }
        if (cents <= 0) {
            return 0;
        }
        try {
            long converted = Math.addExact(Math.multiplyExact(cents, rateMicros), MICROS / 2)
                    / MICROS;
            long adjusted = converted + half(Math.multiplyExact(converted, (long) adjustBps),
                    10000L);
            if (adjusted < 0) {
                adjusted = 0;
            }
            if (WHOLE.equals(rounding)) {
                return Math.multiplyExact(Math.addExact(adjusted, 99L) / 100L, 100L);
            }
            if (NINETY_NINE.equals(rounding)) {
                // Up to the next x.99: 12.00 -> 12.99, 12.99 stays, 13.00 -> 13.99.
                return Math.multiplyExact(adjusted / 100L, 100L) + 99L;
            }
            return adjusted;
        } catch (ArithmeticException overflow) {
            throw new ResponseStatusException(400, "That amount is too large to convert");
        }
    }

    /// `a / b` rounded half away from zero; `b` is positive.
    static long half(long a, long b) {
        return a >= 0 ? (a + b / 2) / b : -((-a + b / 2) / b);
    }

    /// The import duties a buyer in `country` may pay on `lines` (product id
    /// and amount in store cents), by each product's HS code and the longest
    /// matching prefix among the country's rates. Zero when the store keeps
    /// no rates for the country.
    @Transactional(readOnly = true)
    public long dutyEstimate(String storeId, String country, List<String[]> lines)
            throws IOException {
        if (country == null || country.trim().length() != 2 || lines.isEmpty()) {
            return 0;
        }
        String to;
        try {
            to = country(country);
        } catch (ResponseStatusException notACountry) {
            return 0;
        }
        List<DutyRate> rs = duties.findByStoreIdAndCountry(storeId, to);
        if (rs.isEmpty()) {
            return 0;
        }
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < lines.size(); iter++) {
            ids.add(lines.get(iter)[0]);
        }
        Map<String, String> hsOf = new HashMap<String, String>();
        List<CustomsCode> codes = customs.findByProductIdIn(ids);
        for (int iter = 0; iter < codes.size(); iter++) {
            hsOf.put(codes.get(iter).getProductId(), codes.get(iter).getHsCode());
        }
        long total = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            String hs = hsOf.get(lines.get(iter)[0]);
            int bps = rate(rs, hs == null ? "" : hs);
            long amount = Long.parseLong(lines.get(iter)[1]);
            total += half(amount * bps, 10000L);
        }
        return total;
    }

    /// The rate of the longest prefix of `hs` among `rates`, or 0.
    static int rate(List<DutyRate> rates, String hs) {
        int best = -1;
        int bps = 0;
        for (int iter = 0; iter < rates.size(); iter++) {
            DutyRate d = rates.get(iter);
            String p = d.getHsPrefix();
            if (hs.startsWith(p) && p.length() > best) {
                best = p.length();
                bps = d.getRateBps();
            }
        }
        return bps;
    }
}
