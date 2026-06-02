package com.codename1.e2e;

import com.codename1.e2e.graphql.Category;
import com.codename1.e2e.graphql.CatalogGraphApi;
import com.codename1.e2e.graphql.ProductData;
import com.codename1.e2e.graphql.ProductsByCategoryData;
import com.codename1.e2e.graphql.ProductsData;
import com.codename1.e2e.graphql.ProductsData_Products;
import com.codename1.io.graphql.GraphQLResponse;
import com.codename1.testing.AbstractTest;
import com.codename1.util.OnComplete;

/**
 * End-to-end GraphQL round-trips against the catalog server: a list query with
 * a nested object selection + enum + string list, a single-object query, and a
 * query with an enum-typed variable.
 */
public class GraphQlProtocolTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        CatalogGraphApi api = CatalogGraphApi.of(E2eSupport.baseUrl() + "/graphql");

        // --- list query: nested object + enum + string list ---
        final ProductsData[] data = new ProductsData[1];
        final boolean[] ok1 = { false };
        final boolean[] d1 = { false };
        api.products(null, new OnComplete<GraphQLResponse<ProductsData>>() {
            public void completed(GraphQLResponse<ProductsData> r) { ok1[0] = r.isOk(); data[0] = r.getData(); d1[0] = true; }
        });
        E2eSupport.await(d1);
        assertTrue(d1[0], "GraphQL products timed out");
        assertTrue(ok1[0], "GraphQL products reported errors");
        assertNotNull(data[0], "GraphQL products data null");
        assertEqual(4, data[0].products().size(), "GraphQL product count");
        ProductsData_Products first = byId(data[0], "1");
        assertNotNull(first, "GraphQL product id=1 missing");
        assertEqual("The Hobbit", first.name(), "GraphQL name");
        assertEqual(Category.BOOKS, first.category(), "GraphQL enum field");
        assertTrue(first.tags().contains("fantasy"), "GraphQL string-list field");
        assertNotNull(first.dimensions(), "GraphQL nested selection");
        assertRange(20.0, first.dimensions().height(), 0.001, "GraphQL nested field");

        // --- single object query ---
        final ProductData[] pd = new ProductData[1];
        final boolean[] d2 = { false };
        api.product("3", null, new OnComplete<GraphQLResponse<ProductData>>() {
            public void completed(GraphQLResponse<ProductData> r) { pd[0] = r.getData(); d2[0] = true; }
        });
        E2eSupport.await(d2);
        assertTrue(d2[0], "GraphQL product timed out");
        assertNotNull(pd[0], "GraphQL product null");
        assertNotNull(pd[0].product(), "GraphQL product.product null");
        assertEqual(Category.TOYS, pd[0].product().category(), "GraphQL single enum");

        // --- enum-typed variable ---
        final ProductsByCategoryData[] pc = new ProductsByCategoryData[1];
        final boolean[] d3 = { false };
        api.productsByCategory(Category.ELECTRONICS, null,
                new OnComplete<GraphQLResponse<ProductsByCategoryData>>() {
            public void completed(GraphQLResponse<ProductsByCategoryData> r) { pc[0] = r.getData(); d3[0] = true; }
        });
        E2eSupport.await(d3);
        assertTrue(d3[0], "GraphQL byCategory timed out");
        assertNotNull(pc[0], "GraphQL byCategory null");
        assertEqual(1, pc[0].productsByCategory().size(), "GraphQL byCategory count");
        assertEqual(Category.ELECTRONICS, pc[0].productsByCategory().get(0).category(),
                "GraphQL enum-variable filter");
        return true;
    }

    private static ProductsData_Products byId(ProductsData data, String id) {
        for (ProductsData_Products p : data.products()) {
            if (id.equals(p.id())) {
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
