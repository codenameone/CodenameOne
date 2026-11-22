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
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Schedules;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Review;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
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

/// What a shopper keeps besides orders: a wishlist, saved addresses and
/// reviews; and what a guest may read without signing in.
@BackendTest
class ShopperGapsTest {
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
    private ProductRepository products;
    @Autowired
    private ReviewRepository reviewRows;
    @Autowired
    private Schedules schedules;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    @Test
    void aWishlistKeepsEachProductOnce() throws Exception {
        Shop s = shop("wish-shop");
        String user = People.customer(accounts, "wisher");
        Map item = Rest.json(mvc, People.asCustomer(put("/api/wishlist/" + s.productId), user));
        assertEquals("Mug", item.get("title"));
        assertEquals(1000, Rest.number(item, "priceCents"));
        Rest.json(mvc, People.asCustomer(put("/api/wishlist/" + s.productId), user));
        assertEquals(1, Rest.list(mvc, People.asCustomer(get("/api/wishlist"), user)).size());
        assertEquals(404, Rest.statusOf(mvc, People.asCustomer(put("/api/wishlist/nope"), user)));
        assertTrue(Rest.list(mvc, People.asCustomer(delete("/api/wishlist/" + s.productId),
                user)).isEmpty());
        assertEquals(401, Rest.statusOf(mvc, get("/api/wishlist")));
    }

    @Test
    void aScheduledProductIsNotWishedNorReviewedBeforeItsWindow() throws Exception {
        Shop s = shop("wish-sched");
        String user = People.customer(accounts, "wish-sched-buyer");
        Rest.json(mvc, People.asCustomer(put("/api/wishlist/" + s.productId), user));
        assertEquals(1, Rest.list(mvc, People.asCustomer(get("/api/wishlist"), user)).size());
        String reviews = "/api/shop/products/" + s.productId + "/reviews?page=0&size=10";
        assertEquals(200, Rest.statusOf(mvc, get(reviews)));
        long now = System.currentTimeMillis();
        schedules.set(s.storeId, s.productId, Schedules.ONLINE, now + 3600000L, 0, now);
        // Before its launch the product shows nowhere: not in a wishlist that
        // saved it, not as a new wish, not through its reviews.
        assertTrue(Rest.list(mvc, People.asCustomer(get("/api/wishlist"), user)).isEmpty());
        assertEquals(404, Rest.statusOf(mvc, People.asCustomer(put("/api/wishlist/"
                + s.productId), user)));
        assertEquals(404, Rest.statusOf(mvc, get(reviews)));
        assertEquals(404, Rest.statusOf(mvc, People.asCustomer(Rest.body(post(
                "/api/products/" + s.productId + "/reviews"),
                "{\"rating\":5,\"title\":\"Early\",\"body\":\"\"}"), user)));
        // Once the window opens the saved wish is back.
        schedules.set(s.storeId, s.productId, Schedules.ONLINE, now - 3600000L, 0, now);
        assertEquals(1, Rest.list(mvc, People.asCustomer(get("/api/wishlist"), user)).size());
        assertEquals(200, Rest.statusOf(mvc, get(reviews)));
    }

    @Test
    void aProductsRatingIsTheAverageOfItsReviewsRoundedAsTheyAre() throws Exception {
        Shop s = shop("rating-shop");
        int[] stars = {4, 4, 3};
        for (int iter = 0; iter < stars.length; iter++) {
            Review r = new Review();
            r.setId(Ids.next());
            r.setStoreId(s.storeId);
            r.setProductId(s.productId);
            r.setUsername("rater-" + iter + "@bazaar.example");
            r.setAuthorName("Rater");
            r.setRating(stars[iter]);
            r.setTitle("");
            r.setBody("");
            r.setStatus("PUBLISHED");
            r.setCreatedAt(java.time.Instant.ofEpochMilli(1000L + iter));
            reviewRows.insert(r);
        }
        products.addRating(s.productId, 11, 3);
        // 11 / 3 is 3.67: both say 3.7, where truncating made the card say 3.6.
        Map product = Rest.json(mvc, get("/api/shop/products/" + s.productId));
        Map page = Rest.json(mvc, get("/api/shop/products/" + s.productId
                + "/reviews?page=0&size=10"));
        assertEquals(37, Rest.number(product, "rating"));
        assertEquals(37, Rest.number(page, "averageTenths"));
    }

    @Test
    void aGuestBrowsesTheFeedAndAShopperFollowsStores() throws Exception {
        Shop s = shop("follow-shop");
        Map feed = Rest.json(mvc, get("/api/shop/discover?size=60"));
        boolean found = false;
        for (Object o : (List) feed.get("items")) {
            found |= s.productId.equals(((Map) o).get("id"));
        }
        assertTrue(found, "the feed reads every active store, with no token");
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/discover?q=mug&sort=popular")));
        assertEquals(400, Rest.statusOf(mvc, get("/api/shop/discover?size=x")));
        assertEquals(401, Rest.statusOf(mvc, get("/api/follows")));
        assertEquals(401, Rest.statusOf(mvc, put("/api/follows/" + s.storeId)));
        assertEquals(401, Rest.statusOf(mvc, get("/api/follows/feed")));

        String user = People.customer(accounts, "follower");
        String other = People.customer(accounts, "follower-other");
        assertEquals(1, Rest.list(mvc, People.asCustomer(put("/api/follows/" + s.storeId), user))
                .size());
        assertEquals(1, Rest.list(mvc, People.asCustomer(put("/api/follows/" + s.storeId), user))
                .size());
        assertEquals(404, Rest.statusOf(mvc, People.asCustomer(put("/api/follows/nope"), user)));
        assertTrue(Rest.list(mvc, People.asCustomer(get("/api/follows"), other)).isEmpty());
        Map mine = Rest.json(mvc, People.asCustomer(get("/api/follows/feed"), user));
        assertEquals(s.productId, ((Map) ((List) mine.get("items")).get(0)).get("id"));
        assertTrue(Rest.list(mvc, People.asCustomer(delete("/api/follows/" + s.storeId), user))
                .isEmpty());
    }

    private static String address(String city, boolean isDefault) {
        return "{\"id\":null,\"address\":{\"name\":\"Ada\",\"line1\":\"1 Main St\",\"line2\":\"\","
                + "\"city\":\"" + city + "\",\"region\":\"NY\",\"postal\":\"12207\","
                + "\"country\":\"us\",\"phone\":\"\",\"lat\":0,\"lng\":0},\"isDefault\":"
                + isDefault + "}";
    }

    @Test
    void savedAddressesKeepExactlyOneDefault() throws Exception {
        String user = People.customer(accounts, "addresser");
        String other = People.customer(accounts, "addresser-other");
        Map first = Rest.json(mvc, People.asCustomer(Rest.body(post("/api/account/addresses"),
                address("Albany", false)), user));
        assertEquals(Boolean.TRUE, first.get("isDefault"));
        assertEquals("US", ((Map) first.get("address")).get("country"));
        Map second = Rest.json(mvc, People.asCustomer(Rest.body(post("/api/account/addresses"),
                address("Troy", false)), user));
        assertEquals(Boolean.FALSE, second.get("isDefault"));
        List both = Rest.list(mvc, People.asCustomer(post("/api/account/addresses/"
                + second.get("id") + "/default"), user));
        assertEquals(1, defaults(both));
        Map edited = Rest.json(mvc, People.asCustomer(Rest.body(put("/api/account/addresses/"
                + first.get("id")), address("Schenectady", false)), user));
        assertEquals("Schenectady", ((Map) edited.get("address")).get("city"));
        assertEquals(404, Rest.statusOf(mvc, People.asCustomer(delete("/api/account/addresses/"
                + first.get("id")), other)));
        List left = Rest.list(mvc, People.asCustomer(delete("/api/account/addresses/"
                + second.get("id")), user));
        assertEquals(1, left.size());
        assertEquals(Boolean.TRUE, ((Map) left.get(0)).get("isDefault"));
        assertTrue(Rest.list(mvc, People.asCustomer(get("/api/account/addresses"), other))
                .isEmpty());
    }

    private static int defaults(List all) {
        int n = 0;
        for (Object o : all) {
            n += Boolean.TRUE.equals(((Map) o).get("isDefault")) ? 1 : 0;
        }
        return n;
    }

    private static String review(int stars) {
        return "{\"rating\":" + stars + ",\"title\":\"Fine mug\",\"body\":\"Holds coffee.\"}";
    }

    @Test
    void reviewsAreModeratedAndMoveTheRating() throws Exception {
        Shop s = shop("review-shop");
        String buyer = People.customer(accounts, "review-buyer");
        String browser = People.customer(accounts, "review-browser");
        s.buy(buyer, 1);
        String url = "/api/products/" + s.productId + "/reviews";
        Map mine = Rest.json(mvc, People.asCustomer(Rest.body(post(url), review(5)), buyer));
        assertEquals(Boolean.TRUE, mine.get("verified"));
        assertEquals("PENDING", mine.get("status"));
        assertEquals(409, Rest.statusOf(mvc, People.asCustomer(Rest.body(post(url), review(4)),
                buyer)));
        assertEquals(400, Rest.statusOf(mvc, People.asCustomer(Rest.body(post(url), review(6)),
                browser)));
        Map theirs = Rest.json(mvc, People.asCustomer(Rest.body(post(url), review(2)), browser));
        assertEquals(Boolean.FALSE, theirs.get("verified"));

        String publicUrl = "/api/shop/products/" + s.productId + "/reviews?page=0&size=10";
        assertEquals(0, Rest.number(Rest.json(mvc, get(publicUrl)), "count"));
        String mod = "/api/merchant/stores/" + s.storeId + "/reviews/";
        assertEquals(2, Rest.list(mvc, People.asMerchant(get("/api/merchant/stores/" + s.storeId
                + "/reviews?status=PENDING"), s.owner)).size());
        Rest.json(mvc, People.asMerchant(post(mod + mine.get("id") + "/publish"), s.owner));
        Rest.json(mvc, People.asMerchant(post(mod + theirs.get("id") + "/publish"), s.owner));
        Product p = products.findById(s.productId);
        assertEquals(7, p.getRatingTotal());
        assertEquals(2, p.getRatingCount());
        Map page = Rest.json(mvc, get(publicUrl));
        assertEquals(2, Rest.number(page, "count"));
        assertEquals(35, Rest.number(page, "averageTenths"));
        List histogram = (List) page.get("histogram");
        assertEquals(1, ((Number) histogram.get(1)).intValue());
        assertEquals(1, ((Number) histogram.get(4)).intValue());

        Rest.json(mvc, People.asMerchant(post(mod + theirs.get("id") + "/reject"), s.owner));
        p = products.findById(s.productId);
        assertEquals(5, p.getRatingTotal());
        assertEquals(1, p.getRatingCount());
        assertEquals(1, Rest.number(Rest.json(mvc, get(publicUrl)), "count"));
        assertEquals(403 / 100, Rest.statusOf(mvc, People.asMerchant(post(mod + mine.get("id")
                + "/reject"), browser)) / 100);
    }

    @Test
    void aGuestReadsTheShopWithoutSigningIn() throws Exception {
        Shop s = shop("guest-shop");
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/products/" + s.productId)));
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/products/" + s.productId
                + "/reviews?page=0&size=5")));
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/stores/guest-shop/products")));
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/cart/" + s.storeId)));
        assertEquals(401, Rest.statusOf(mvc, Rest.body(post("/api/products/" + s.productId
                + "/reviews"), review(5))));
        assertFalse(Rest.statusOf(mvc, get("/api/account/addresses")) == 200);
    }
}
