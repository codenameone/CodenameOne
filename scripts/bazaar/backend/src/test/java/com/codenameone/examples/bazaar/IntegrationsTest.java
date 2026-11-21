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
import com.codenameone.examples.bazaar.api.FxRateDto;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.api.VatCheckDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.market.FxFeed;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.trade.VatIds;
import com.codenameone.examples.bazaar.trade.VatRegistry;
import com.codenameone.examples.bazaar.trade.VatVerification;
import com.codenameone.examples.bazaar.trade.ViesRegistry;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The outside services: the ECB rate feed, VIES, geolocation and a real
/// carrier -- each through its interface with a local fake, never the network.
@BackendTest
class IntegrationsTest {
    static final String ECB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<gesmes:Envelope xmlns:gesmes=\"http://www.gesmes.org/xml/2002-08-01\">\n"
            + "<Cube>\n<Cube time='2026-11-20'>\n"
            + "<Cube currency='USD' rate='1.0842'/>\n"
            + "<Cube currency=\"GBP\" rate=\"0.83915\"/>\n"
            + "<Cube currency='JPY' rate='162.31'/>\n"
            + "<Cube currency='BAD' rate='1e3'/>\n"
            + "</Cube>\n</Cube>\n</gesmes:Envelope>\n";

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
    private Markets markets;
    @Autowired
    private FxFeed feed;
    @Autowired
    private StoreRepository storeRows;
    @Autowired
    private VatVerification vat;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 300, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    // ================================================================== FX

    @Test
    void ecbFileParsesToMillionthsAndCrossRatesAreInteger() throws Exception {
        FxFeed.Daily d = FxFeed.parse(ECB_XML);
        assertEquals(1000000L, d.perEuro.get("EUR").longValue());
        assertEquals(1084200L, d.perEuro.get("USD").longValue());
        assertEquals(839150L, d.perEuro.get("GBP").longValue());
        assertEquals(162310000L, d.perEuro.get("JPY").longValue());
        assertNull(d.perEuro.get("BAD"));
        assertEquals(20777L * 86400000L, d.publishedAt);
        // 1 USD in EUR: 1 / 1.0842 = 0.922339...
        assertEquals(922339L, FxFeed.cross(d, "USD", "EUR"));
        // 1 USD in GBP: 0.83915 / 1.0842 = 0.773981...
        assertEquals(773981L, FxFeed.cross(d, "USD", "GBP"));
        assertEquals(1084200L, FxFeed.cross(d, "EUR", "USD"));
        assertEquals(0L, FxFeed.cross(d, "USD", "CHF"));
        assertThrows(IOException.class, () -> FxFeed.parse("<html>maintenance</html>"));
    }

    @Test
    void aMarketOnTheFeedIsPricedAtItAndTheManualRateIsKept() throws Exception {
        Shop s = shop("fxfeed");
        String currency = storeRows.findById(s.storeId).getCurrency();
        String other = "USD".equals(currency) ? "EUR" : "USD";
        markets.putRate(s.owner, s.storeId, new FxRateDto(other, 500000, 0, "", 0));
        MarketDto m = markets.save(s.owner, s.storeId, null, new MarketDto(null, "fx", "Feed",
                other, Arrays.asList("DE"), "", Markets.NONE, 0, true, Markets.MANUAL));
        assertEquals(500000L, markets.pricer(storeRows.findById(s.storeId), "fx").rateMicros());
        markets.save(s.owner, s.storeId, m.id(), new MarketDto(m.id(), "fx", "Feed", other,
                Arrays.asList("DE"), "", Markets.NONE, 0, true, Markets.ECB));
        assertTrue(feed.refresh(() -> ECB_XML) >= 1);
        Markets.Pricer p = markets.pricer(storeRows.findById(s.storeId), "fx");
        FxFeed.Daily d = FxFeed.parse(ECB_XML);
        assertEquals(FxFeed.cross(d, currency, other), p.rateMicros());
        assertEquals(Markets.ECB, p.source());
        List<FxRateDto> rows = markets.rates(s.owner, s.storeId);
        assertEquals(500000L, rows.get(0).rateMicros());
        assertEquals(Markets.ECB, rows.get(0).feedSource());
        assertEquals(d.publishedAt, rows.get(0).feedAt());
        // A feed that is down changes nothing.
        assertThrows(IOException.class, () -> feed.refresh(() -> {
            throw new IOException("down");
        }));
        assertEquals(p.rateMicros(), markets.pricer(storeRows.findById(s.storeId), "fx")
                .rateMicros());
    }

    // ================================================================= VAT

    @Test
    void viesAnswersAreCachedAndAnOutageFallsBackToUnverified() throws Exception {
        final int[] asked = {0};
        ViesRegistry vies = new ViesRegistry(path -> {
            asked[0]++;
            if (path.equals("/ms/DE/vat/123456789")) {
                return new ViesRegistry.Reply(200, "{\"isValid\":true,\"name\":\"Acme GmbH\","
                        + "\"userError\":\"VALID\",\"requestIdentifier\":\"WAPIAAAA1\"}");
            }
            if (path.equals("/ms/FR/vat/12345678901")) {
                return new ViesRegistry.Reply(200, "{\"isValid\":false,\"userError\":"
                        + "\"MS_UNAVAILABLE\"}");
            }
            if (path.equals("/ms/AT/vat/U12345678")) {
                return new ViesRegistry.Reply(200, "{\"isValid\":false,\"name\":\"---\","
                        + "\"userError\":\"INVALID\"}");
            }
            return new ViesRegistry.Reply(503, "down");
        });
        long now = 1800000000000L;
        VatCheckDto ok = vat.verify("de 123 456 789", vies, now);
        assertEquals(VatIds.VERIFIED, ok.status());
        assertTrue(ok.valid());
        assertEquals("Acme GmbH", ok.name());
        assertEquals(1, asked[0]);
        assertEquals("WAPIAAAA1", vat.reference("DE123456789"));
        // Cached: asked again within the week, VIES is not consulted.
        assertEquals(VatIds.VERIFIED, vat.verify("DE123456789", vies, now + 1000).status());
        assertEquals(1, asked[0]);
        // A member state that is down: valid on format, unverified, not cached.
        VatCheckDto down = vat.verify("FR12345678901", vies, now);
        assertEquals(VatIds.UNVERIFIED, down.status());
        assertTrue(down.valid());
        vat.verify("FR12345678901", vies, now);
        assertEquals(3, asked[0]);
        // VIES itself down (503) or unreachable: the same.
        assertEquals(VatIds.UNVERIFIED, vat.verify("BE0123456789", vies, now).status());
        assertEquals(VatIds.UNVERIFIED, vat.verify("BE0123456789", (c, n) -> {
            throw new IOException("no route");
        }, now).status());
        // Not issued.
        VatCheckDto no = vat.verify("ATU12345678", vies, now);
        assertEquals(VatIds.INVALID, no.status());
        assertTrue(!no.valid());
        assertEquals("", no.name());
        // A bad format never reaches VIES; no registry checks the format only.
        int before = asked[0];
        assertEquals(VatIds.FORMAT, vat.verify("DE12", vies, now).status());
        assertEquals(before, asked[0]);
        assertEquals(VatIds.FORMAT, vat.verify("NL123456789B01", VatRegistry.NONE, now)
                .status());
    }
}
