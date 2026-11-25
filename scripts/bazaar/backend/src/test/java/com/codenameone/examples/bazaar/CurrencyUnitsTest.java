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
package com.codenameone.examples.bazaar;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.admin.Platform;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlatformStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatsDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.market.EcbExchangeRates;
import com.codenameone.examples.bazaar.market.FixedExchangeRates;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.market.ReportingRates;
import com.codenameone.examples.bazaar.ops.Amounts;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.DemoStores;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Amounts in each currency's own minor units, and the platform's totals in
/// one reporting currency at the rates recorded at sale.
@BackendTest
class CurrencyUnitsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private StoreOrderRepository orderRows;
    @Autowired
    private StoreRepository storeRows;
    @Autowired
    private VariantRepository variantRows;
    @Autowired
    private Platform platform;
    @Autowired
    private ReportingRates reporting;
    @Autowired
    private DemoStores demoStores;

    @Test
    void digitsComeFromTheCurrency() {
        assertEquals(2, Amounts.digits("USD"));
        assertEquals(0, Amounts.digits("JPY"));
        assertEquals(3, Amounts.digits("BHD"));
        assertEquals(2, Amounts.digits(""));
        assertEquals(2, Amounts.digits("ZZZ"));
        assertEquals(0, Amounts.digits("XAU"));
    }

    @Test
    void yenHasNoDecimals() {
        assertEquals("1250", Amounts.plain(1250, "JPY"));
        assertEquals("-7", Amounts.plain(-7, "JPY"));
        String shown = Money.format(1250, "JPY");
        assertTrue(shown.endsWith("1250") && shown.indexOf('.') < 0, shown);
        assertEquals(1250, Amounts.parse("1250", "JPY"));
        assertEquals(1250, Amounts.parse("1250.9", "JPY"));
        assertEquals("$12.50", Money.format(1250, "USD"));
        assertEquals("12.50", Money.plain(1250, "USD"));
    }

    @Test
    void dinarsHaveThreeDecimals() {
        assertEquals("1.250", Amounts.plain(1250, "BHD"));
        assertEquals("0.005", Amounts.plain(5, "BHD"));
        assertEquals("-0.050", Amounts.plain(-50, "BHD"));
        assertEquals("BHD 1.250", Money.format(1250, "BHD"));
        assertEquals(1234, Amounts.parse("1.2345", "BHD"));
        assertEquals(1200, Amounts.parse("1.2", "BHD"));
        // Ten dollars at 0.376 dinars each: 3.760 dinars, not 0.376.
        assertEquals(3760, Markets.convert(1000, "USD", "BHD", 376000, 0, Markets.NONE));
        // A dollar at 150 yen is 150 yen, not 15000.
        assertEquals(150, Markets.convert(100, "USD", "JPY", 150000000, 0, Markets.NONE));
        // Rounding rules work in the target's own unit.
        assertEquals(4000, Markets.convert(1000, "USD", "BHD", 376000, 0, Markets.WHOLE));
        assertEquals(3999, Markets.convert(1000, "USD", "BHD", 376000, 0, Markets.NINETY_NINE));
        assertEquals(1500, Markets.convert(1000, "USD", "JPY", 150000000, 0, Markets.NINETY_NINE));
        assertEquals(1500, Checkouts.atRate(1000, "USD", "JPY", 150000000));
        // The two-decimal forms answer as before.
        assertEquals(920, Markets.convert(1000, 920000, 0, Markets.NONE));
        assertEquals(999, Markets.convert(1000, 920000, 0, Markets.NINETY_NINE));
    }

    @Test
    void roundingIsHalfToEven() {
        assertEquals(2, Amounts.divideHalfEven(5, 2));
        assertEquals(4, Amounts.divideHalfEven(7, 2));
        assertEquals(-2, Amounts.divideHalfEven(-5, 2));
        assertEquals(12, Amounts.rescale(1250, 2, 0));
        assertEquals(14, Amounts.rescale(1350, 2, 0));
        assertEquals(-12, Amounts.rescale(-1250, 2, 0));
        assertEquals(12500, Amounts.rescale(1250, 2, 3));
        // 15000 yen at 0.006667 dollars: 100.005 dollars, which is 10000.5
        // cents, rounds to the even 10000.
        assertEquals(10000, Amounts.convert(15000, "JPY", "USD", 6667));
    }

    @Test
    void mixedCurrenciesAddInTheReportingCurrency() {
        ReportingRates usd = new ReportingRates("USD", new FixedExchangeRates());
        assertEquals(6667, usd.rates().rateMicros("JPY", "USD"));
        StoreOrder dollars = order("USD", 10000);
        StoreOrder yen = order("JPY", 15000);
        StoreOrder dinars = order("BHD", 3760);
        usd.stamp(dollars);
        usd.stamp(yen);
        usd.stamp(dinars);
        assertEquals("USD", yen.getReportingCurrency());
        assertEquals(6667, yen.getReportingRateMicros());
        assertEquals(1000000, dollars.getReportingRateMicros());
        long total = usd.toReporting(dollars, dollars.getTotalCents())
                + usd.toReporting(yen, yen.getTotalCents())
                + usd.toReporting(dinars, dinars.getTotalCents());
        // 100.00 + 100.00 + 10.00 dollars.
        assertEquals(21000, total);
        // The recorded rate wins over today's.
        yen.setReportingRateMicros(10000);
        assertEquals(15000, usd.toReporting(yen, 15000));
        // A rate recorded against another reporting currency is not used.
        ReportingRates eur = new ReportingRates("EUR", new FixedExchangeRates());
        assertEquals(9200, eur.toReporting(dollars, 10000));
        // A stamped order keeps its rate.
        eur.stamp(yen);
        assertEquals("USD", yen.getReportingCurrency());
    }

    @Test
    void ecbRatesCrossThroughTheEuro() {
        String xml = "<gesmes:Envelope><Cube><Cube time='2026-11-24'>"
                + "<Cube currency='USD' rate='1.0870'/><Cube currency='JPY' rate='163.05'/>"
                + "</Cube></Cube></gesmes:Envelope>";
        int[] fetches = new int[1];
        EcbExchangeRates ecb = new EcbExchangeRates(() -> {
            fetches[0]++;
            return xml;
        }, new FixedExchangeRates());
        assertEquals(150000000, ecb.rateMicros("USD", "JPY"));
        assertEquals(1000000, ecb.rateMicros("JPY", "JPY"));
        // Not published by the bank: the fixed table answers.
        assertEquals(376000, ecb.rateMicros("USD", "BHD"));
        assertEquals(1, fetches[0]);
    }

    @Test
    void aYenOrderEndToEnd() throws Exception {
        Shop shop = new Shop("yen-shop", 10, "JPY", 1500, accounts, stores, pricing, db,
                inventory, carts, checkouts, mvc);
        OrderDto placed = shop.buy(People.customer(accounts, "yen-buyer"), 2);
        StoreOrder o = orderRows.findById(placed.id());
        assertEquals("JPY", o.getCurrency());
        assertEquals(3000, o.getSubtotalCents());
        // Express is $19.99 in the demo table: 2998.5 yen, to the even 2998.
        assertEquals(2998, o.getShippingCents());
        assertEquals(o.getSubtotalCents() + o.getShippingCents() + o.getTaxCents()
                - o.getDiscountCents(), o.getTotalCents());
        assertEquals("USD", o.getReportingCurrency());
        assertEquals(6667, o.getReportingRateMicros());
        PlatformStatsDto stats = platform.stats();
        assertEquals(reporting.currency(), stats.reportingCurrency());
        StoreStatsDto mine = null;
        for (StoreStatsDto s : stats.perStore()) {
            if (s.storeId().equals(shop.storeId)) {
                mine = s;
            }
        }
        assertEquals(o.getTotalCents(), mine.gmvCents());
        assertEquals(Amounts.convert(o.getTotalCents(), "JPY", "USD", 6667),
                mine.reportingGmvCents());
    }

    @Test
    void fennelAndFigSellsInYen() throws Exception {
        String owner = People.customer(accounts, "fennel-yen-owner");
        demoStores.fennelFig(owner);
        Store ff = storeRows.findByHandle(DemoStores.FENNEL_FIG);
        assertEquals("JPY", ff.getCurrency());
        List<Variant> stock = variantRows.findByStoreId(ff.getId());
        boolean cruet = false;
        for (Variant v : stock) {
            // The olive oil cruet is $24.00 in the table: 3600 yen.
            cruet |= v.getPriceCents() == 3600;
        }
        assertTrue(cruet, "a yen price for the cruet");
        List<StoreOrder> sold = orderRows.findByStoreIdAndCreatedAtGreaterThanEqual(ff.getId(),
                java.time.Instant.EPOCH);
        assertTrue(!sold.isEmpty());
        assertEquals(6667, sold.get(0).getReportingRateMicros());
    }

    private static StoreOrder order(String currency, long total) {
        StoreOrder o = new StoreOrder();
        o.setCurrency(currency);
        o.setTotalCents(total);
        return o;
    }
}
