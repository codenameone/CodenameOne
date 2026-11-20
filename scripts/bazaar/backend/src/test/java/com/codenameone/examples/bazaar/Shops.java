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

import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.store.Stores;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;

/// A store made fresh for a test, and products put in it through the API.
final class Shops {
    final String owner;
    final String storeId;
    final String handle;
    final String locationId;

    private Shops(String owner, String storeId, String handle, String locationId) {
        this.owner = owner;
        this.storeId = storeId;
        this.handle = handle;
        this.locationId = locationId;
    }

    /// A store named `name` (also its handle) with one location.
    static Shops open(Accounts accounts, Stores stores, String name) throws Exception {
        String owner = People.customer(accounts, name + "-owner");
        Store store = stores.create(owner, name, name, "USD");
        LocationDto location = stores.addLocation(owner, store.getId(),
                new LocationDto(null, "Shop floor", "RETAIL", "", true, true, 0));
        return new Shops(owner, store.getId(), store.getHandle(), location.id());
    }

    String base() {
        return "/api/merchant/stores/" + storeId;
    }

    /// Creates a product with one variant per price, and answers it.
    Map product(MockMvc mvc, String title, String status, String type, String vendor,
            String tags, long... prices) throws Exception {
        StringBuilder variants = new StringBuilder();
        for (int iter = 0; iter < prices.length; iter++) {
            if (iter > 0) {
                variants.append(',');
            }
            variants.append("{\"option1\":\"V").append(iter).append("\",\"sku\":\"")
                    .append(Catalog2.sku(title, iter)).append("\",\"barcode\":\"\",\"priceCents\":")
                    .append(prices[iter]).append(",\"taxable\":true,\"trackInventory\":true}");
        }
        StringBuilder tagList = new StringBuilder();
        int start = 0;
        for (int iter = 0; iter <= tags.length() && tags.length() > 0; iter++) {
            if (iter == tags.length() || tags.charAt(iter) == ',') {
                tagList.append(start > 0 ? "," : "").append('"')
                        .append(tags.substring(start, iter)).append('"');
                start = iter + 1;
            }
        }
        String json = "{\"title\":\"" + title + "\",\"status\":\"" + status
                + "\",\"productType\":\"" + type + "\",\"vendor\":\"" + vendor + "\",\"tags\":["
                + tagList + "],\"options\":[{\"name\":\"Kind\",\"values\":[\"V0\"]}],"
                + "\"variants\":[" + variants + "]}";
        return Rest.json(mvc, People.asMerchant(Rest.body(post(base() + "/products"), json),
                owner));
    }

    static String variantId(Map product, int index) {
        return (String) ((Map) ((List) product.get("variants")).get(index)).get("id");
    }

    /// Small helpers kept apart from the catalog code under test.
    static final class Catalog2 {
        private Catalog2() {
        }

        static String sku(String title, int index) {
            StringBuilder out = new StringBuilder();
            for (int iter = 0; iter < title.length() && out.length() < 10; iter++) {
                char c = title.charAt(iter);
                if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') {
                    out.append(c);
                }
            }
            return out + "-" + index;
        }
    }
}
