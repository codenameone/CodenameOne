package com.codename1.e2e;

import com.codename1.e2e.rest.CatalogApi;
import com.codename1.e2e.rest.model.Category;
import com.codename1.e2e.rest.model.Product;
import com.codename1.io.rest.Response;
import com.codename1.testing.AbstractTest;
import com.codename1.util.OnComplete;

import java.util.List;

/**
 * End-to-end REST (OpenAPI) round-trips against the catalog server: a list
 * response with nested objects + enums + string lists, a single object by id,
 * and an enum-typed path parameter.
 */
public class RestProtocolTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        CatalogApi api = CatalogApi.of(E2eSupport.baseUrl());

        // --- list: nested object + enum + string list ---
        final List<Product>[] all = new List[1];
        final boolean[] d1 = { false };
        api.listProducts(null, new OnComplete<Response<List<Product>>>() {
            public void completed(Response<List<Product>> r) { all[0] = r.getResponseData(); d1[0] = true; }
        });
        E2eSupport.await(d1);
        assertTrue(d1[0], "REST list timed out");
        assertNotNull(all[0], "REST list null");
        assertEqual(4, all[0].size(), "REST product count");
        Product hobbit = byId(all[0], 1L);
        assertNotNull(hobbit, "product id=1 missing");
        assertEqual("The Hobbit", hobbit.name(), "REST product name");
        assertEqual(Category.BOOKS, hobbit.category(), "REST enum field");
        assertTrue(hobbit.tags().contains("fantasy"), "REST string-list field");
        assertNotNull(hobbit.dimensions(), "REST nested object");
        assertRange(12.0, hobbit.dimensions().width(), 0.001, "REST nested object field");

        // --- single object by id ---
        final Product[] one = new Product[1];
        final boolean[] d2 = { false };
        api.getProduct(2L, null, new OnComplete<Response<Product>>() {
            public void completed(Response<Product> r) { one[0] = r.getResponseData(); d2[0] = true; }
        });
        E2eSupport.await(d2);
        assertTrue(d2[0], "REST getProduct timed out");
        assertNotNull(one[0], "REST getProduct null");
        assertEqual(Category.ELECTRONICS, one[0].category(), "REST getProduct enum");

        // --- enum-typed path parameter ---
        final List<Product>[] books = new List[1];
        final boolean[] d3 = { false };
        api.getProductsByCategory(Category.BOOKS, null, new OnComplete<Response<List<Product>>>() {
            public void completed(Response<List<Product>> r) { books[0] = r.getResponseData(); d3[0] = true; }
        });
        E2eSupport.await(d3);
        assertTrue(d3[0], "REST byCategory timed out");
        assertNotNull(books[0], "REST byCategory null");
        assertEqual(2, books[0].size(), "REST byCategory count");
        for (Product p : books[0]) {
            assertEqual(Category.BOOKS, p.category(), "REST byCategory filtered enum");
        }
        return true;
    }

    private static Product byId(List<Product> list, long id) {
        for (Product p : list) {
            if (p.id() != null && p.id().longValue() == id) {
                return p;
            }
        }
        return null;
    }

    @Override
    public int getTimeoutMillis() {
        return 90000;
    }
}
