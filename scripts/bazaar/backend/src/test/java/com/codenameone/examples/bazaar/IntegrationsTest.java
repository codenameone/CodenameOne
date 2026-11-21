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
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.web.GeoCountry;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.carrier.CarrierProvider;
import com.codenameone.examples.bazaar.carrier.CarrierWebhook;
import com.codenameone.examples.bazaar.carrier.EasyPostCarrier;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
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
    @Autowired
    private GeoCountry geo;

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

    // ================================================================= Geo

    @Test
    void geoSuggestsAMarketOnlyFromTrustedHeadersAndNeverOverAChoice() throws Exception {
        Shop s = shop("geo");
        String currency = storeRows.findById(s.storeId).getCurrency();
        String other = "USD".equals(currency) ? "EUR" : "USD";
        markets.putRate(s.owner, s.storeId, new FxRateDto(other, 900000, 0, "", 0));
        markets.save(s.owner, s.storeId, null, new MarketDto(null, "de", "Germany", other,
                Arrays.asList("DE"), "", Markets.NONE, 0, true, Markets.MANUAL));
        String page = "/s/geo/products/geo-mug";
        String agent = "Mozilla/5.0 (Macintosh) Safari/605";
        try {
            // Untrusted by default: the header alone suggests nothing.
            geo.setTrustedHeaders("");
            assertTrue(!body(get(page).header("CF-IPCountry", "DE").header("User-Agent", agent))
                    .contains("Shopping from DE"));
            geo.setTrustedHeaders("CF-IPCountry,X-Country-Code");
            String shown = body(get(page).header("CF-IPCountry", "de")
                    .header("User-Agent", agent));
            assertTrue(shown.contains("Shopping from DE"), "the banner");
            assertTrue(shown.contains("action=\"/market\"") && shown.contains("value=\"de\""),
                    "a button that chooses the market");
            // The suggestion never prices the page: still the store's own prices.
            assertTrue(shown.contains(currency + " (store prices)"));
            // Unknown and Tor count as none; the second trusted header is read.
            assertTrue(!body(get(page).header("CF-IPCountry", "XX").header("User-Agent", agent))
                    .contains("Shopping from"));
            assertTrue(!body(get(page).header("CF-IPCountry", "T1").header("User-Agent", agent))
                    .contains("Shopping from"));
            assertTrue(body(get(page).header("X-Country-Code", "DE").header("User-Agent", agent))
                    .contains("Shopping from DE"));
            // Crawlers see the shared page.
            assertTrue(!body(get(page).header("CF-IPCountry", "DE")
                    .header("User-Agent", "Googlebot/2.1")).contains("Shopping from"));
            // An explicit choice -- the cookie or the query -- always wins.
            assertTrue(!body(get(page).header("CF-IPCountry", "DE").header("User-Agent", agent)
                    .cookie("market", "de")).contains("Shopping from"));
            assertTrue(!body(get(page + "?market=de").header("CF-IPCountry", "DE")
                    .header("User-Agent", agent)).contains("Shopping from"));
            // The pluggable lookup: a CSV range table by client address.
            geo.setTrustedHeaders("");
            GeoCountry.RangeTable table = new GeoCountry.RangeTable(Arrays.asList(
                    "\"10.0.0.0\",\"10.255.255.255\",\"FR\"", "1.0.0.0,1.0.0.255,AU",
                    "garbage", "5.0.0.0,4.0.0.0,US"));
            assertEquals(2, table.size());
            assertEquals("FR", table.country("10.1.2.3"));
            assertEquals("AU", table.country("1.0.0.7"));
            assertEquals(null, table.country("1.0.1.0"));
            assertEquals(null, table.country("not-an-ip"));
            geo.setLookup(address -> "DE");
            assertTrue(body(get(page).header("User-Agent", agent)).contains("Shopping from DE"));
        } finally {
            geo.setTrustedHeaders("");
            geo.setLookup(null);
        }
    }

    private String body(com.codename1.backend.test.MockRequestBuilder r)
            throws Exception {
        MockResponse res = mvc.perform(r.header("Host", SignIn.HOST)).andReturn().getResponse();
        assertEquals(200, res.getStatus());
        return res.getContentAsString();
    }

    // ============================================================= Carrier

    @Test
    @SuppressWarnings("unchecked")
    void easyPostRatesLabelsSignaturesAndTrackingAgainstCannedAnswers() throws Exception {
        final List<String> calls = new java.util.ArrayList<String>();
        String shipment = "{\"id\":\"shp_1\",\"rates\":["
                + "{\"id\":\"rate_a\",\"carrier\":\"USPS\",\"service\":\"Priority\","
                + "\"rate\":\"7.58\",\"delivery_days\":2},"
                + "{\"id\":\"rate_b\",\"carrier\":\"UPS\",\"service\":\"Ground\",\"rate\":\"12\"},"
                + "{\"id\":\"rate_c\",\"carrier\":\"X\",\"service\":\"Y\",\"rate\":\"1e3\"}]}";
        EasyPostCarrier.Transport fake = (method, path, json) -> {
            calls.add(method + " " + path);
            if (path.equals("/shipments")) {
                assertTrue(json.contains("\"zip\":\"10001\""), json);
                return new EasyPostCarrier.Reply(201, shipment);
            }
            if (path.equals("/shipments/shp_1/buy")) {
                assertTrue(json.contains("rate_a"), json);
                return new EasyPostCarrier.Reply(200, "{\"tracking_code\":\"EZ100\","
                        + "\"postage_label\":{\"label_url\":\"https://labels.example/1.png\"},"
                        + "\"selected_rate\":{\"rate\":\"7.58\"}}");
            }
            if (path.startsWith("/trackers")) {
                return new EasyPostCarrier.Reply(200, "{\"trackers\":[{\"tracking_code\":"
                        + "\"EZ100\",\"tracking_details\":[{\"status\":\"pre_transit\"},"
                        + "{\"status\":\"in_transit\",\"message\":\"Departed\",\"datetime\":"
                        + "\"2026-11-20T10:00:00Z\",\"tracking_location\":{\"city\":\"Reno\"}}]}]}");
            }
            return new EasyPostCarrier.Reply(500, "");
        };
        CarrierProvider.Place origin = new CarrierProvider.Place("", "", "New York", 0, 0,
                "10001", "NY", "US");
        EasyPostCarrier ep = new EasyPostCarrier(fake, "whsec", origin, 60000L);
        CarrierProvider.Place to = new CarrierProvider.Place("Ann", "1 Main St", "Austin", 0, 0,
                "73301", "TX", "US");
        CarrierProvider.Parcel parcel = new CarrierProvider.Parcel(500);
        List<RateQuoteDto> rates = ep.rates(parcel, new CarrierProvider.Place("Shop", "", "",
                0, 0), to);
        assertEquals(2, rates.size());
        assertEquals("USPS Priority", rates.get(0).service());
        assertEquals(758L, rates.get(0).cents());
        assertEquals(2, rates.get(0).days());
        assertEquals(1200L, rates.get(1).cents());
        CarrierProvider.Label label = ep.buyLabel(parcel, new CarrierProvider.Place("Shop", "",
                "", 0, 0), to, "USPS Priority", "f1");
        assertEquals("EZ100", label.trackingNumber);
        assertEquals(758L, label.costCents);
        assertEquals("https://labels.example/1.png", label.labelUrl);
        assertThrows(IOException.class, () -> ep.buyLabel(parcel, origin, to, "FedEx Air", "f2"));
        // Polling maps the statuses it knows and is throttled per parcel.
        List<CarrierProvider.Scan> scans = ep.track("EZ100", 0, 1000000L, origin, to);
        assertEquals(1, scans.size());
        assertEquals("IN_TRANSIT", scans.get(0).status);
        assertEquals("Reno", scans.get(0).place);
        int before = calls.size();
        assertTrue(ep.track("EZ100", 0, 1030000L, origin, to).isEmpty());
        assertEquals(before, calls.size());
        // Webhook signatures: HMAC-SHA256 of the raw body under the secret.
        byte[] body = ("{\"description\":\"tracker.updated\",\"result\":{\"tracking_code\":"
                + "\"EZ100\",\"tracking_details\":[{\"status\":\"delivered\",\"message\":"
                + "\"Left at door\",\"datetime\":\"2026-11-21T09:00:00Z\"}]}}").getBytes("UTF-8");
        String good = "hmac-sha256-hex=" + com.codenameone.examples.bazaar.Ids.hex(
                com.codename1.backend.Crypto.hmacSha256("whsec".getBytes("UTF-8"), body));
        assertTrue(ep.verify(body, good));
        assertTrue(!ep.verify(body, good.substring(0, good.length() - 1) + "0"));
        assertTrue(!ep.verify(body, null));
        assertTrue(!new EasyPostCarrier(fake, "", origin, 0).verify(body, good));
        Object[] event = EasyPostCarrier.event(new String(body, "UTF-8"));
        assertEquals("EZ100", event[0]);
        assertEquals("DELIVERED", ((List<CarrierProvider.Scan>) event[1]).get(0).status);
        assertNull(EasyPostCarrier.event("{\"description\":\"batch.created\"}"));
        // The webhook route refuses everything while EasyPost is not configured.
        assertEquals(404, mvc.perform(post(CarrierWebhook.PATH).header("Host", SignIn.HOST)
                .header(EasyPostCarrier.SIGNATURE_HEADER, good).content(new String(body,
                "UTF-8"))).andReturn().getResponse().getStatus());
        // Money is parsed by hand into cents.
        assertEquals(758L, EasyPostCarrier.cents("7.58"));
        assertEquals(50L, EasyPostCarrier.cents("0.5"));
        assertEquals(1200L, EasyPostCarrier.cents("12"));
        assertEquals(-1L, EasyPostCarrier.cents("7.581"));
        assertEquals(-1L, EasyPostCarrier.cents("-3"));
        assertEquals(-1L, EasyPostCarrier.cents("."));
    }
}
