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
import com.codenameone.examples.bazaar.store.DemoStores;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The catalog as shoppers and staff see it: what is public, smart
/// collections, facets, search and lookups by code.
@BackendTest
class CatalogTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DataSource db;

    private static List<String> titles(Map page) {
        List items = (List) page.get("items");
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (int iter = 0; iter < items.size(); iter++) {
            out.add((String) ((Map) items.get(iter)).get("title"));
        }
        return out;
    }

    private static List<String> names(String... names) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (int iter = 0; iter < names.length; iter++) {
            out.add(names[iter]);
        }
        return out;
    }

    @Test
    void draftsAndArchivedProductsAreHiddenFromShoppers() throws Exception {
        Shops shop = Shops.open(accounts, stores, "visible-shop");
        Map live = shop.product(mvc, "Live Lamp", "ACTIVE", "Lamps", "Lux", "home", 2000);
        Map draft = shop.product(mvc, "Draft Desk", "DRAFT", "Desks", "Lux", "home", 9000);
        Map gone = shop.product(mvc, "Old Chair", "ACTIVE", "Chairs", "Lux", "home", 5000);
        mvc.perform(People.asMerchant(post(shop.base() + "/products/" + gone.get("id")
                + "/archive"), shop.owner));

        // No sign-in needed, and only the active product shows.
        Map page = Rest.json(mvc, get("/api/shop/stores/" + shop.handle + "/products"));
        assertEquals(names("Live Lamp"), titles(page));
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/products/" + live.get("id"))));
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/products/" + draft.get("id"))));
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/products/" + gone.get("id"))));
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/stores/" + shop.handle
                + "/products/draft-desk")));
        // Costs are for staff.
        Map shown = Rest.json(mvc, get("/api/shop/products/" + live.get("id")));
        assertEquals(0L, Rest.number((Map) ((List) shown.get("variants")).get(0), "costCents"));

        // Staff see all three.
        Map all = Rest.json(mvc, People.asMerchant(get(shop.base() + "/products"), shop.owner));
        assertEquals(3L, Rest.number(all, "total"));

        // A suspended store shows nothing at all.
        db.execute("UPDATE store SET status = 'SUSPENDED' WHERE id = ?",
                new Object[] {shop.storeId});
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/stores/" + shop.handle
                + "/products")));
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/products/" + live.get("id"))));
    }

    @Test
    void smartCollectionsFollowTheirRules() throws Exception {
        Shops shop = Shops.open(accounts, stores, "smart-shop");
        shop.product(mvc, "Cheap Mug", "ACTIVE", "Mugs", "Clay", "gift,kitchen", 1500);
        shop.product(mvc, "Fancy Mug", "ACTIVE", "Mugs", "Clay", "kitchen", 6000);
        shop.product(mvc, "Cheap Vase", "ACTIVE", "Vases", "Kiln", "gift", 2500);
        shop.product(mvc, "Hidden Mug", "DRAFT", "Mugs", "Clay", "gift", 1000);
        Map made = Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base() + "/collections"),
                "{\"title\":\"Gift mugs\",\"ruleText\":\"tag:GIFT and type:mugs AND price<2000\","
                + "\"published\":true,\"position\":-1}"), shop.owner));
        assertEquals("gift-mugs", made.get("handle"));
        // Staff count the draft too; shoppers do not.
        assertEquals(2L, Rest.number(made, "productCount"));
        Map page = Rest.json(mvc, get("/api/shop/stores/" + shop.handle
                + "/products?collection=gift-mugs"));
        assertEquals(names("Cheap Mug"), titles(page));

        Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base() + "/collections"),
                "{\"title\":\"Over twenty\",\"ruleText\":\"price>2000\",\"published\":true,"
                + "\"position\":-1}"), shop.owner));
        page = Rest.json(mvc, get("/api/shop/stores/" + shop.handle
                + "/products?collection=over-twenty&sort=-price"));
        assertEquals(names("Fancy Mug", "Cheap Vase"), titles(page));

        // A rule that cannot be read is refused when saved.
        assertEquals(400, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(shop.base()
                + "/collections"), "{\"title\":\"Bad\",\"ruleText\":\"colour:red\"}"),
                shop.owner)));

        // A manual collection keeps the products placed in it.
        Map vase = shop.product(mvc, "Second Vase", "ACTIVE", "Vases", "Kiln", "", 3000);
        Map manual = Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base()
                + "/collections"), "{\"title\":\"Picks\",\"published\":true,\"productIds\":[\""
                + vase.get("id") + "\"]}"), shop.owner));
        assertEquals(1L, Rest.number(manual, "productCount"));
        List shopperCollections = Rest.list(mvc, get("/api/shop/stores/" + shop.handle
                + "/collections"));
        assertEquals(3, shopperCollections.size());
        mvc.perform(People.asMerchant(delete(shop.base() + "/collections/" + manual.get("id")),
                shop.owner));
        assertEquals(2, Rest.list(mvc, get("/api/shop/stores/" + shop.handle
                + "/collections")).size());
    }

    @Test
    void facetsCountEachFilterAgainstTheOthers() throws Exception {
        Shops shop = Shops.open(accounts, stores, "facet-shop");
        shop.product(mvc, "Red Kettle", "ACTIVE", "Kettles", "Acme", "red,steel", 4000);
        shop.product(mvc, "Blue Kettle", "ACTIVE", "Kettles", "Bolt", "blue,steel", 5000);
        shop.product(mvc, "Red Toaster", "ACTIVE", "Toasters", "Acme", "red", 3000, 3500);
        shop.product(mvc, "Draft Toaster", "DRAFT", "Toasters", "Acme", "red", 100);

        Map facets = Rest.json(mvc, get("/api/shop/stores/" + shop.handle + "/facets"));
        assertEquals(3L, Rest.number(facets, "total"));
        assertEquals(3000L, Rest.number(facets, "minPriceCents"));
        assertEquals(5000L, Rest.number(facets, "maxPriceCents"));
        Map firstType = (Map) ((List) facets.get("types")).get(0);
        assertEquals("Kettles", firstType.get("name"));
        assertEquals(2L, Rest.number(firstType, "count"));

        // With a type chosen the type facet still counts every type, and the
        // others count only kettles.
        facets = Rest.json(mvc, get("/api/shop/stores/" + shop.handle
                + "/facets?type=kettles"));
        assertEquals(2L, Rest.number(facets, "total"));
        assertEquals(2, ((List) facets.get("types")).size());
        List tags = (List) facets.get("tags");
        assertEquals("steel", ((Map) tags.get(0)).get("name"));
        assertEquals(2L, Rest.number((Map) tags.get(0), "count"));

        // Search words, vendor and price narrow the list.
        assertEquals(names("Red Kettle", "Red Toaster"), titles(Rest.json(mvc,
                get("/api/shop/stores/" + shop.handle + "/products?q=red"))));
        assertEquals(names("Red Toaster"), titles(Rest.json(mvc, get("/api/shop/stores/"
                + shop.handle + "/products?vendor=ACME&max=3200"))));
        Map paged = Rest.json(mvc, get("/api/shop/stores/" + shop.handle
                + "/products?sort=price&size=2&page=1"));
        assertEquals(names("Blue Kettle"), titles(paged));
        assertEquals(3L, Rest.number(paged, "total"));
    }

    @Test
    void variantsAreFoundByBarcodeOrSku() throws Exception {
        Shops shop = Shops.open(accounts, stores, "scan-shop");
        Map product = shop.product(mvc, "Scanner Socks", "ACTIVE", "Socks", "Feet", "", 1200);
        String variantId = Shops.variantId(product, 0);
        String barcode = DemoStores.ean13("200999000001");
        Map variant = (Map) ((List) product.get("variants")).get(0);
        Rest.json(mvc, People.asMerchant(Rest.body(put(shop.base() + "/products/"
                + product.get("id")), "{\"title\":\"Scanner Socks\",\"variants\":[{\"id\":\""
                + variantId + "\",\"option1\":\"V0\",\"sku\":\"" + variant.get("sku")
                + "\",\"barcode\":\"" + barcode + "\",\"priceCents\":1200,"
                + "\"trackInventory\":true}]}"), shop.owner));
        Map hit = Rest.json(mvc, People.asMerchant(get(shop.base() + "/lookup?code=" + barcode),
                shop.owner));
        assertEquals(variantId, ((Map) hit.get("variant")).get("id"));
        assertEquals("Scanner Socks", hit.get("productTitle"));
        // The SKU was made from the title as "ScannerSoc-0"; typed in lower case.
        String lower = "scannersoc-0";
        hit = Rest.json(mvc, People.asMerchant(get(shop.base() + "/lookup?code=" + lower),
                shop.owner));
        assertEquals(variantId, ((Map) hit.get("variant")).get("id"));
        assertEquals(404, Rest.statusOf(mvc, People.asMerchant(get(shop.base()
                + "/lookup?code=0000000000000"), shop.owner)));
        // Another store's staff cannot look into this one.
        Shops other = Shops.open(accounts, stores, "scan-other");
        assertEquals(404, Rest.statusOf(mvc, People.asMerchant(get(shop.base() + "/lookup?code="
                + barcode), other.owner)));
    }

    @Test
    void duplicateAndBulkEditMakeTheirChanges() throws Exception {
        Shops shop = Shops.open(accounts, stores, "bulk-shop");
        Map a = shop.product(mvc, "Alpha Cup", "DRAFT", "Cups", "Z", "", 1000);
        Map b = shop.product(mvc, "Beta Cup", "DRAFT", "Cups", "Z", "", 2000);
        Map copy = Rest.json(mvc, People.asMerchant(post(shop.base() + "/products/"
                + a.get("id") + "/duplicate"), shop.owner));
        assertEquals("DRAFT", copy.get("status"));
        assertFalse(copy.get("id").equals(a.get("id")));
        assertTrue(((String) ((Map) ((List) copy.get("variants")).get(0)).get("sku"))
                .endsWith("-COPY"));
        Map count = Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base()
                + "/products/bulk"), "{\"productIds\":[\"" + a.get("id") + "\",\"" + b.get("id")
                + "\"],\"status\":\"ACTIVE\",\"adjustBasisPoints\":1000}"), shop.owner));
        assertEquals(2L, Rest.number(count, "count"));
        Map page = Rest.json(mvc, get("/api/shop/stores/" + shop.handle + "/products?sort=price"));
        assertEquals(names("Alpha Cup", "Beta Cup"), titles(page));
        assertEquals(1100L, Rest.number((Map) ((List) page.get("items")).get(0), "minPriceCents"));
    }

    @Test
    void theDemoStoresAreSeededWithAConsistentLedger() throws Exception {
        Map northwind = Rest.json(mvc, get("/api/shop/stores/" + DemoStores.NORTHWIND));
        assertEquals("Northwind Outfitters", northwind.get("name"));
        Map page = Rest.json(mvc, get("/api/shop/stores/" + DemoStores.LITTLE_CLAY
                + "/products?size=100"));
        // Thirty products, one a draft, one archived and one for the
        // Insiders club's members only, which a guest does not see.
        assertEquals(27L, Rest.number(page, "total"));
        Map bad = db.queryOne("SELECT COUNT(*) AS n FROM inventory_level l WHERE l.on_hand <> "
                + "(SELECT COALESCE(SUM(m.on_hand_delta), 0) FROM inventory_move m "
                + "WHERE m.variant_id = l.variant_id AND m.location_id = l.location_id)",
                new Object[0]);
        assertEquals(0L, Rows.number(bad.get("n")));
        List rows = db.query("SELECT barcode FROM variant WHERE barcode <> ''", new Object[0]);
        assertTrue(rows.size() > 100);
        for (int iter = 0; iter < rows.size(); iter++) {
            String code = (String) ((Map) rows.get(iter)).get("barcode");
            assertEquals(code, DemoStores.ean13(code.substring(0, 12)));
        }
    }
}
