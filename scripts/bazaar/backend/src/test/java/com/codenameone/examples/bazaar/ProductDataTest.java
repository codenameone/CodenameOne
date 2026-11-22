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
import com.codename1.pdf.PdfReader;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.MediaUploadDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.carrier.CarrierProvider;
import com.codenameone.examples.bazaar.carrier.EasyPostCarrier;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.catalog.Taxonomy;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.ops.Customs;
import com.codenameone.examples.bazaar.ops.UnitPricing;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.security.WebHeaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Product data the outside world reads: the taxonomy category, customs
/// details per variant and unit pricing -- validated, stored, and shown on the
/// storefront, in its feeds and structured data, to the carrier and on the
/// packing slip -- and the 3D model viewer with the policy it needs.
@BackendTest
class ProductDataTest {
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
    private Catalog catalog;

    private static final int BEVERAGES = 413;
    private static final int CLOTHING = 1604;

    @Test
    void unitPricesRoundHalfUpAndNeedAMeasure() {
        // $10.00 for 250 g is $4.00 per 100 g.
        assertEquals(400L, UnitPricing.unitPriceCents(1000, 250000, "g", 100, "g"));
        // $9.99 for 300 g: 333 cents, rounded down.
        assertEquals(333L, UnitPricing.unitPriceCents(999, 300000, "g", 100, "g"));
        // $5.00 for 3 kg: 166.67 per kg rounds up to 167.
        assertEquals(167L, UnitPricing.unitPriceCents(500, 3000, "kg", 1, "kg"));
        // Across units of one dimension: $3.00 for 750 ml is $4.00 a litre.
        assertEquals(400L, UnitPricing.unitPriceCents(300, 750000, "ml", 1, "l"));
        // A fractional measure: $6.00 for 1.5 kg is $0.40 per 100 g.
        assertEquals(40L, UnitPricing.unitPriceCents(600, 1500, "kg", 100, "g"));
        // No measure, a zero one, a missing unit or two dimensions: none.
        assertEquals(UnitPricing.UNKNOWN, UnitPricing.unitPriceCents(1000, 0, "g", 100, "g"));
        assertEquals(UnitPricing.UNKNOWN, UnitPricing.unitPriceCents(1000, 250000, null, 100,
                "g"));
        assertEquals(UnitPricing.UNKNOWN, UnitPricing.unitPriceCents(1000, 250000, "g", 0, "g"));
        assertEquals(UnitPricing.UNKNOWN, UnitPricing.unitPriceCents(1000, 250000, "g", 100,
                "ml"));
        assertEquals(UnitPricing.UNKNOWN, UnitPricing.unitPriceCents(1000, -5, "g", 100, "g"));
        // A price too large to scale is refused, never overflowed.
        assertEquals(UnitPricing.UNKNOWN, UnitPricing.unitPriceCents(Long.MAX_VALUE / 2, 1,
                "mg", 10000, "kg"));

        assertEquals("100 g", UnitPricing.per(100, "g"));
        assertEquals("kg", UnitPricing.per(1, "kg"));
        assertEquals(1500L, UnitPricing.parseQuantity("1,5"));
        assertEquals(250000L, UnitPricing.parseQuantity("250"));
        assertEquals(0L, UnitPricing.parseQuantity(""));
        assertEquals(-1L, UnitPricing.parseQuantity("1.2345"));
        assertEquals(-1L, UnitPricing.parseQuantity("abc"));
        assertEquals("1.5", UnitPricing.quantity(1500));
    }

    @Test
    void customsCodesAndCountriesAreValidated() {
        assertEquals("691200", Customs.hsCode("6912.00"));
        assertEquals("6912001000", Customs.hsCode("6912 00 10 00"));
        assertEquals("", Customs.hsCode(""));
        assertNull(Customs.hsCode("12345"));
        assertNull(Customs.hsCode("12345678901"));
        assertNull(Customs.hsCode("6912ab"));
        assertEquals("CA", Customs.country("ca"));
        assertEquals("", Customs.country(" "));
        assertNull(Customs.country("XX"));
        assertNull(Customs.country("CAN"));
    }

    @Test
    void theTaxonomyIsGooglesTwoLevels() {
        assertEquals("Food, Beverages & Tobacco > Beverages", Taxonomy.path(BEVERAGES));
        assertEquals("Beverages", Taxonomy.name(BEVERAGES));
        assertEquals(412, Taxonomy.top(BEVERAGES));
        assertTrue(Taxonomy.within(BEVERAGES, 412));
        assertTrue(Taxonomy.within(BEVERAGES, BEVERAGES));
        assertFalse(Taxonomy.within(CLOTHING, 412));
        assertEquals(BEVERAGES, Taxonomy.parse("Food, Beverages & Tobacco > Beverages"));
        assertEquals(BEVERAGES, Taxonomy.parse("413"));
        assertEquals(0, Taxonomy.parse(""));
        assertEquals(-1, Taxonomy.parse("Nowhere > Nothing"));
        assertFalse(Taxonomy.known(999999));
        assertTrue(Taxonomy.all().size() > 150);
        boolean found = false;
        for (com.codenameone.examples.bazaar.api.NameCountDto hit : Taxonomy.search("bever", 5)) {
            found |= hit.count() == BEVERAGES;
        }
        assertTrue(found, "searching finds the category");
    }

    @Test
    void easyPostDeclaresCustomsOnlyAcrossABorder() throws Exception {
        final List<String> bodies = new ArrayList<String>();
        EasyPostCarrier.Transport fake = (method, path, json) -> {
            bodies.add(json);
            return new EasyPostCarrier.Reply(201, "{\"id\":\"shp_1\",\"rates\":[]}");
        };
        CarrierProvider.Place origin = new CarrierProvider.Place("Shop", "1 Dock Rd",
                "New York", 0, 0, "10001", "NY", "US");
        EasyPostCarrier ep = new EasyPostCarrier(fake, "", origin, 0);
        List<CarrierProvider.CustomsItem> items = Arrays.asList(new CarrierProvider.CustomsItem(
                "Tea tin", 2, 1999, "USD", 500, "090230", "LK", "TEA-1"));
        CarrierProvider.Parcel parcel = new CarrierProvider.Parcel(500, items);
        CarrierProvider.Place shop = new CarrierProvider.Place("Shop", "", "", 0, 0);

        ep.rates(parcel, shop, new CarrierProvider.Place("Ann", "1 Rue", "Montreal", 0, 0,
                "H2X 1Y4", "QC", "CA"));
        String abroad = bodies.get(0);
        assertTrue(abroad.contains("\"customs_info\""), abroad);
        assertTrue(abroad.contains("\"hs_tariff_number\":\"090230\""), abroad);
        assertTrue(abroad.contains("\"origin_country\":\"LK\""), abroad);
        assertTrue(abroad.contains("\"value\":\"19.99\""), abroad);
        assertTrue(abroad.contains("\"customs_certify\":true"), abroad);

        ep.rates(parcel, shop, new CarrierProvider.Place("Bo", "1 Main St", "Austin", 0, 0,
                "73301", "TX", "US"));
        assertFalse(bodies.get(1).contains("customs_info"), bodies.get(1));
        assertEquals("19.99", EasyPostCarrier.decimal(1999));
        assertEquals("0.05", EasyPostCarrier.decimal(5));
        assertTrue(CarrierProvider.crossesBorder("US", "CA"));
        assertFalse(CarrierProvider.crossesBorder("US", "us"));
        assertFalse(CarrierProvider.crossesBorder("", "CA"));
    }

    /// A product with a category and one variant sold by weight, made through
    /// the merchant API.
    private Map tea(Shops shop, String title) throws Exception {
        String json = "{\"title\":\"" + title + "\",\"status\":\"ACTIVE\",\"categoryId\":"
                + BEVERAGES + ",\"options\":[{\"name\":\"Size\",\"values\":[\"250 g\"]}],"
                + "\"variants\":[{\"option1\":\"250 g\",\"sku\":\"" + title.replace(' ', '-')
                + "\",\"barcode\":\"\",\"priceCents\":1000,\"taxable\":true,"
                + "\"trackInventory\":false,\"hsCode\":\"0902.30\",\"originCountry\":\"lk\","
                + "\"unitQuantity\":250000,\"unitName\":\"g\",\"unitReference\":100,"
                + "\"unitReferenceName\":\"g\"}]}";
        return Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base() + "/products"), json),
                shop.owner));
    }

    @Test
    void theApiStoresAndRefusesProductData() throws Exception {
        Shops shop = Shops.open(accounts, stores, "pd-api");
        Map p = tea(shop, "Black tea");
        assertEquals((long) BEVERAGES, Rest.number(p, "categoryId"));
        Map v = (Map) ((List) p.get("variants")).get(0);
        assertEquals("090230", v.get("hsCode"));
        assertEquals("LK", v.get("originCountry"));
        assertEquals(250000L, ((Number) v.get("unitQuantity")).longValue());

        String bad = "{\"title\":\"Bad\",\"status\":\"ACTIVE\",\"options\":[{\"name\":\"Size\","
                + "\"values\":[\"x\"]}],\"variants\":[{\"option1\":\"x\",\"sku\":\"BAD-%s\","
                + "\"barcode\":\"\",\"priceCents\":100,\"taxable\":true,"
                + "\"trackInventory\":false,%s}]}";
        String[] wrong = {"\"hsCode\":\"123\"", "\"originCountry\":\"ZZ\"",
            "\"unitQuantity\":1000,\"unitName\":\"g\",\"unitReference\":1,"
                + "\"unitReferenceName\":\"l\"",
            "\"unitQuantity\":1000,\"unitName\":\"furlong\",\"unitReference\":1,"
                + "\"unitReferenceName\":\"furlong\""};
        for (int iter = 0; iter < wrong.length; iter++) {
            MockResponse r = mvc.perform(People.asMerchant(Rest.body(post(shop.base()
                    + "/products"), String.format(bad, iter, wrong[iter])), shop.owner))
                    .andReturn().getResponse();
            assertEquals(400, r.getStatus(), wrong[iter] + " " + r.getContentAsString());
        }
        String unknownCategory = "{\"title\":\"Odd\",\"status\":\"ACTIVE\",\"categoryId\":999999,"
                + "\"options\":[{\"name\":\"Size\",\"values\":[\"x\"]}],\"variants\":[{"
                + "\"option1\":\"x\",\"sku\":\"ODD-1\",\"barcode\":\"\",\"priceCents\":100,"
                + "\"taxable\":true,\"trackInventory\":false}]}";
        assertEquals(400, mvc.perform(People.asMerchant(Rest.body(post(shop.base()
                + "/products"), unknownCategory), shop.owner)).andReturn().getResponse()
                .getStatus());
    }

    @Test
    void theStorefrontShowsUnitPricesAndStructuredData() throws Exception {
        Shops shop = Shops.open(accounts, stores, "pd-page");
        Map p = tea(shop, "Green tea");
        MockResponse page = mvc.perform(get("/s/pd-page/products/" + p.get("handle")))
                .andReturn().getResponse();
        String html = page.getContentAsString();
        assertEquals(200, page.getStatus(), html);
        assertTrue(html.contains("$4.00 / 100 g"), html);
        // The structured data sits HTML-escaped in an attribute.
        String ld = html.replace("&quot;", "\"").replace("&gt;", ">").replace("&amp;", "&");
        assertTrue(ld.contains("\"category\":\"Food, Beverages & Tobacco > Beverages\""), ld);
        assertTrue(ld.contains("\"@type\":\"UnitPriceSpecification\",\"price\":\"4.00\""), ld);
        assertTrue(ld.contains("\"value\":100,\"unitCode\":\"GRM\""), ld);
        // No model on this page: no viewer and no blob: in the policy.
        assertFalse(html.contains("model-viewer"), "no viewer without a model");
        assertFalse(page.getHeader("Content-Security-Policy").contains("blob:"));

        String search = mvc.perform(get("/s/pd-page/search?q=green")).andReturn().getResponse()
                .getContentAsString();
        assertTrue(search.contains("$4.00 / 100 g"), search);

        String xml = mvc.perform(get("/s/pd-page/feeds/default/products.xml")).andReturn()
                .getResponse().getContentAsString();
        assertTrue(xml.contains("<g:google_product_category>413</g:google_product_category>"),
                xml);
        assertTrue(xml.contains("<g:unit_pricing_measure>250 g</g:unit_pricing_measure>"), xml);
        assertTrue(xml.contains("<g:unit_pricing_base_measure>100 g"), xml);
        String csv = mvc.perform(get("/s/pd-page/feeds/default/products.csv")).andReturn()
                .getResponse().getContentAsString();
        assertTrue(csv.contains("google_product_category"), csv);
        assertTrue(csv.contains("250 g"), csv);
    }

    @Test
    void aCollectionFiltersByCategory() throws Exception {
        Shops shop = Shops.open(accounts, stores, "pd-filter");
        tea(shop, "Oolong tea");
        String shirt = "{\"title\":\"Linen shirt\",\"status\":\"ACTIVE\",\"categoryId\":" + CLOTHING
                + ",\"options\":[{\"name\":\"Size\",\"values\":[\"M\"]}],\"variants\":[{"
                + "\"option1\":\"M\",\"sku\":\"SHIRT-M\",\"barcode\":\"\",\"priceCents\":3000,"
                + "\"taxable\":true,\"trackInventory\":false}]}";
        Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base() + "/products"), shirt),
                shop.owner));
        Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base() + "/collections"),
                "{\"title\":\"Everything\",\"ruleText\":\"price>0\",\"published\":true}"),
                shop.owner));
        String all = mvc.perform(get("/s/pd-filter/collections/everything")).andReturn()
                .getResponse().getContentAsString();
        assertTrue(all.contains("Oolong tea") && all.contains("Linen shirt"), all);
        assertTrue(all.contains("category=412"), all);
        assertTrue(all.contains("category=166"), all);
        String food = mvc.perform(get("/s/pd-filter/collections/everything?category=412"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(food.contains("Oolong tea"), food);
        assertFalse(food.contains("Linen shirt"), food);
        assertTrue(food.contains("noindex"), "a filtered page is not indexed");
        // Junk is no filter, never an error.
        String junk = mvc.perform(get("/s/pd-filter/collections/everything?category=x%3Cy"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(junk.contains("Linen shirt"), junk);
    }

    @Test
    void aModelLoadsTheLocalViewerAndWidensThePolicyForItsPageOnly() throws Exception {
        Shop s = new Shop("pd-model", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        catalog.addMedia(s.owner, s.storeId, s.productId, new MediaUploadDto("MODEL", "mug.glb",
                "model/gltf-binary", Base64.getEncoder().encodeToString(
                    new byte[] {'g', 'l', 'T', 'F', 2, 0, 0, 0, 12, 0, 0, 0}), null));
        MockResponse page = mvc.perform(get("/s/pd-model/products/pd-model-mug")).andReturn()
                .getResponse();
        String html = page.getContentAsString();
        assertTrue(html.contains("<model-viewer"), html);
        assertTrue(html.contains("<script type=\"module\" src=\"/static/model-viewer."), html);
        assertFalse(html.contains("<script src=\"http"), "no third-party viewer");
        String csp = page.getHeader("Content-Security-Policy");
        assertTrue(csp.contains("img-src 'self' data: blob:"), csp);
        assertTrue(csp.contains("connect-src 'self' blob:"), csp);
        assertFalse(csp.contains("unsafe-eval"), csp);
        assertFalse(csp.contains("unsafe-inline"), csp);
        // The viewer file itself is served from this site.
        int at = html.indexOf("/static/model-viewer.");
        String src = html.substring(at, html.indexOf('"', at));
        assertEquals(200, mvc.perform(get(src)).andReturn().getResponse().getStatus());

        String plain = WebHeaders.policy("n", null);
        assertFalse(plain.contains("blob:"), plain);
    }

    @Test
    void anInternationalPackingSlipListsCustoms() throws Exception {
        Shop s = new Shop("pd-slip", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        db.execute("UPDATE variant SET hs_code = '691200', origin_country = 'PT' WHERE id = ?",
                new Object[] {s.variantId});
        String user = People.customer(accounts, "pd-slip-buyer");
        OrderDto o = s.buy(user, 2);
        String base = "/api/merchant/stores/" + s.storeId + "/orders/" + o.id();
        String home = slip(base, s.owner);
        assertFalse(home.contains("691200"), "a domestic slip has no customs: " + home);

        db.execute("UPDATE store_order SET ship_country = 'CA' WHERE id = ?",
                new Object[] {o.id()});
        String abroad = slip(base, s.owner);
        assertTrue(abroad.contains("691200"), abroad);
        assertTrue(abroad.contains("PT"), abroad);
    }

    private String slip(String base, String owner) throws Exception {
        MockResponse r = mvc.perform(People.asMerchant(get(base + "/packing-slip.pdf"), owner))
                .andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        return PdfReader.open(r.getContentAsByteArray()).extractText();
    }
}
