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
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.MetafieldDefDto;
import com.codenameone.examples.bazaar.api.MetafieldValueDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.TranslationDto;
import com.codenameone.examples.bazaar.api.TranslationFieldDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.content.Metafields;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.market.Translations;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Product content in the shopper's language, end to end: the title,
/// description, SEO text, options, pictures' alternative text, text
/// metafields and collections, on every storefront surface, through the
/// shop API, in order lines and mail; and the editor's side -- the sheet with
/// each field's status, and the CSV round trip.
@BackendTest
class ProductTranslationTest {
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
    private Translations translations;
    @Autowired
    private Metafields metafields;
    @Autowired
    private Catalog catalog;
    @Autowired
    private Emails emails;
    private final MailSender mail = MailSender.current();

    /// A mug in Blue and Red with a captioned picture, a text metafield and
    /// a collection, all translated to German.
    private Shop translated(String name) throws Exception {
        Shop s = new Shop(name, 50, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
        db.execute("UPDATE product SET description = ?, seo_title = ? WHERE id = ?",
                new Object[] {"A sturdy mug", "Mug for coffee", s.productId});
        db.execute("UPDATE variant SET option1 = ? WHERE id = ?", new Object[] {"Blue",
            s.variantId});
        db.execute("INSERT INTO product_option (id, product_id, name, option_values, position) "
                + "VALUES (?, ?, 'Color', 'Blue, Red', 0)", new Object[] {Ids.next(),
                    s.productId});
        String photo = Ids.next();
        db.execute("INSERT INTO product_photo (id, product_id, photo_id, alt, position) VALUES "
                + "(?, ?, ?, 'A blue mug on a table', 0)", new Object[] {photo, s.productId,
                    Ids.next()});
        MetafieldDefDto glaze = metafields.define(s.owner, s.storeId, new MetafieldDefDto(null,
                "PRODUCT", "glaze", "Glaze", "text", "", true));
        metafields.put(s.owner, s.storeId, new MetafieldValueDto(glaze.id(), "PRODUCT",
                s.productId, "glaze", null, null, "Matte stoneware"));
        Map row = db.queryOne("SELECT id FROM metafield_value WHERE owner_id = ?",
                new Object[] {s.productId});
        String valueId = Rows.text(row.get("id"));
        CollectionDto kitchen = catalog.createCollection(s.owner, s.storeId, new CollectionDto(
                null, name + "-kitchen", "Kitchen", "For the kitchen", "", "", 0, true,
                Arrays.asList(s.productId), 0));
        put(s, Translations.PRODUCT, s.productId, "title", "Becher");
        put(s, Translations.PRODUCT, s.productId, "description", "Ein robuster Becher");
        put(s, Translations.PRODUCT, s.productId, "seoTitle", "Kaffeebecher");
        put(s, Translations.OPTION, s.productId, "name1", "Farbe");
        put(s, Translations.OPTION, s.productId, "values1", "Blau, Rot");
        put(s, Translations.IMAGE, photo, "alt", "Ein blauer Becher auf einem Tisch");
        put(s, Translations.METAFIELD_VALUE, valueId, "value", "Mattes Steinzeug");
        put(s, Translations.COLLECTION, kitchen.id(), "title", "Kueche");
        return s;
    }

    private void put(Shop s, String kind, String ref, String field, String value)
            throws Exception {
        translations.put(s.owner, s.storeId, new TranslationDto(kind, ref, "de", field, value));
    }

    @Test
    void everyStorefrontSurfaceShowsTheShoppersLanguage() throws Exception {
        Shop s = translated("pt-store");
        String page = "/s/pt-store/products/pt-store-mug";

        // `?lang=de` is the hreflang alternate: it must serve German content,
        // with no market and no Accept-Language.
        String de = body(page + "?lang=de", null);
        assertTrue(de.contains("Becher"), "the title");
        assertTrue(de.contains("Ein robuster Becher"), "the description");
        assertTrue(de.contains("Kaffeebecher"), "the SEO title");
        assertTrue(de.contains("Ein blauer Becher auf einem Tisch"), "the picture's alt text");
        assertTrue(de.contains("Mattes Steinzeug"), "the text metafield");
        assertTrue(de.contains("inLanguage&quot;:&quot;de&quot;"), "the structured data's language");
        assertFalse(de.contains("A sturdy mug"), "no original description left");

        String en = body(page, null);
        assertTrue(en.contains("A sturdy mug") && !en.contains("Ein robuster Becher"),
                "the originals by default");
        // The browser's language decides when the shopper chose none.
        assertTrue(body(page, "de-CH,de;q=0.9").contains("Ein robuster Becher"));
        // A language the store never translated into shows the originals.
        assertTrue(body(page + "?lang=fr", null).contains("A sturdy mug"));

        // Cards, the collection page and search.
        String collection = body("/s/pt-store/collections/pt-store-kitchen?lang=de", null);
        assertTrue(collection.contains("Kueche"), "the collection's title");
        assertTrue(collection.contains("Becher"), "the card's title");
        assertTrue(body("/s/pt-store?lang=de", null).contains("Becher"), "the home page cards");
        String found = body("/s/pt-store/search?q=Becher&lang=de", null);
        assertTrue(found.contains("pt-store-mug"), "found by its German title");
        assertTrue(body("/s/pt-store/search?q=Mug&lang=de", null).contains("pt-store-mug"),
                "and still by the original");
        assertFalse(body("/s/pt-store/search?q=Becher", null).contains("pt-store-mug"),
                "an English shopper's search does not read German");
        // The sitemap names the German address of each page.
        String map = body("/sitemap.xml", null);
        assertTrue(map.contains("hreflang=\"de\"") && map.contains("?lang=de"), map);
    }

    @Test
    void theShopApiAnswersInTheCallersLanguageAndTheEditorNever() throws Exception {
        Shop s = translated("pt-api");
        Map p = Rest.json(mvc, get("/api/shop/products/" + s.productId)
                .header("Accept-Language", "de-DE,de;q=0.9,en;q=0.5"));
        assertEquals("Becher", p.get("title"));
        Map option = (Map) ((List) p.get("options")).get(0);
        assertEquals("Farbe", option.get("name"));
        assertEquals(Arrays.asList("Blau", "Rot"), option.get("values"));
        Map variant = (Map) ((List) p.get("variants")).get(0);
        assertEquals("Blau", variant.get("option1"));
        Map english = Rest.json(mvc, get("/api/shop/products/" + s.productId));
        assertEquals("Mug", english.get("title"));
        Map list = Rest.json(mvc, get("/api/shop/stores/pt-api/products?q=Becher")
                .header("Accept-Language", "de"));
        assertEquals("Becher", ((Map) ((List) list.get("items")).get(0)).get("title"));
        List cs = Rest.list(mvc, get("/api/shop/stores/pt-api/collections?lang=de"));
        assertEquals("Kueche", ((Map) cs.get(0)).get("title"));

        // The merchant's editor loads the originals: saving it must never
        // turn a translation into the original.
        ProductDto edited = catalog.merchantProduct(s.owner, s.storeId, s.productId);
        assertEquals("Mug", edited.title());
        assertEquals("Color", edited.options().get(0).name());
    }

    @Test
    void orderLinesAndMailSpeakTheOrdersLanguage() throws Exception {
        Shop s = translated("pt-order");
        String buyer = People.customer(accounts, "pt-order-buyer");
        OrderDto o = s.buy(buyer, 1, buyer);
        // The line records the original; the merchant reads it so.
        assertEquals("Mug", o.lines().get(0).title());
        db.execute("UPDATE store_order SET language = ? WHERE id = ?", new Object[] {"de",
            o.id()});
        mail.clearSent();
        emails.orderConfirmation(o.id());
        Mail m = null;
        for (Mail sent : mail.sent()) {
            if (sent.getTo().contains(buyer)) {
                m = sent;
            }
        }
        if (m == null) {
            fail("no confirmation");
        }
        assertTrue(m.getText().contains("Becher"), m.getText());
    }

    @Test
    void theSheetTellsMissingOutdatedAndDone() throws Exception {
        Shop s = translated("pt-sheet");
        List<TranslationFieldDto> sheet = translations.sheet(s.owner, s.storeId,
                Translations.PRODUCT, s.productId, "de");
        assertEquals(Translations.DONE, status(sheet, Translations.PRODUCT, "title"));
        assertEquals(Translations.MISSING, status(sheet, Translations.PRODUCT, "seoDescription"));
        assertEquals(Translations.DONE, status(sheet, Translations.OPTION, "values1"));
        assertEquals(Translations.DONE, status(sheet, Translations.IMAGE, "alt"));
        assertEquals(Translations.DONE, status(sheet, Translations.METAFIELD_VALUE, "value"));
        assertEquals(null, status(sheet, Translations.OPTION, "name2"), "no second option");

        // The merchant changes the original: the translation is outdated, and
        // still shown until it is redone.
        db.execute("UPDATE product SET title = ? WHERE id = ?", new Object[] {"Big mug",
            s.productId});
        sheet = translations.sheet(s.owner, s.storeId, Translations.PRODUCT, s.productId, "de");
        assertEquals(Translations.OUTDATED, status(sheet, Translations.PRODUCT, "title"));
        put(s, Translations.PRODUCT, s.productId, "title", "Grosser Becher");
        sheet = translations.sheet(s.owner, s.storeId, Translations.PRODUCT, s.productId, "de");
        assertEquals(Translations.DONE, status(sheet, Translations.PRODUCT, "title"));

        // Option values line up one for one.
        try {
            put(s, Translations.OPTION, s.productId, "values1", "Blau");
            fail("one value for two");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
        // Another store can neither read nor write this store's text.
        Shop other = new Shop("pt-sheet-o", 0, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String photo = Rows.text(db.queryOne("SELECT id FROM product_photo WHERE product_id = ?",
                new Object[] {s.productId}).get("id"));
        try {
            translations.put(other.owner, other.storeId, new TranslationDto(Translations.IMAGE,
                    photo, "de", "alt", "x"));
            fail("translated another store's picture");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
        try {
            translations.sheet(other.owner, other.storeId, Translations.PRODUCT, s.productId,
                    "de");
            fail("read another store's product");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
    }

    @Test
    void theCsvRoundTripsAndReportsBadRows() throws Exception {
        Shop s = translated("pt-csv");
        String csv = translations.exportCsv(s.owner, s.storeId, "de");
        assertTrue(csv.startsWith("\"kind\",\"ref_id\",\"field\""), csv);
        assertTrue(csv.contains("\"Becher\""), csv);
        // Sent back untouched, nothing changes.
        Translations.ImportResult same = translations.importCsv(s.owner, s.storeId, csv);
        assertEquals(0, same.saved);
        assertTrue(same.errors.isEmpty(), same.errors.toString());
        // A translator fills the SEO description, quoting a comma, and one
        // row names a field a product does not have.
        String filled = "kind,ref_id,field,locale,translation\r\n"
                + "PRODUCT," + s.productId + ",seoDescription,de,\"Ein Becher, robust\"\r\n"
                + "PRODUCT," + s.productId + ",colour,de,Blau\r\n";
        Translations.ImportResult r = translations.importCsv(s.owner, s.storeId, filled);
        assertEquals(1, r.saved);
        assertEquals(1, r.errors.size());
        assertTrue(r.errors.get(0).startsWith("Row 3"), r.errors.toString());
        assertEquals("Ein Becher, robust", translations.fields(s.storeId, Translations.PRODUCT,
                s.productId, "de").get("seoDescription"));
        // The guard against spreadsheet formulas is undone on the way back.
        String formula = "kind,ref_id,field,locale,translation\n"
                + "PRODUCT," + s.productId + ",description,de,'=Becher\n";
        translations.importCsv(s.owner, s.storeId, formula);
        assertEquals("=Becher", translations.fields(s.storeId, Translations.PRODUCT,
                s.productId, "de").get("description"));
    }

    private static String status(List<TranslationFieldDto> sheet, String kind, String field) {
        for (int iter = 0; iter < sheet.size(); iter++) {
            if (sheet.get(iter).kind().equals(kind) && sheet.get(iter).field().equals(field)) {
                return sheet.get(iter).status();
            }
        }
        return null;
    }

    private String body(String path, String language) throws Exception {
        com.codename1.backend.test.MockRequestBuilder r = get(path).header("Host", SignIn.HOST);
        if (language != null) {
            r = r.header("Accept-Language", language);
        }
        MockResponse res = mvc.perform(r).andReturn().getResponse();
        assertEquals(200, res.getStatus(), path);
        return res.getContentAsString();
    }
}
