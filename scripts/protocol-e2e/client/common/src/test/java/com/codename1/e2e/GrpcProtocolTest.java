package com.codename1.e2e;

import com.codename1.e2e.grpc.CatalogGrpc;
import com.codename1.e2e.grpc.Category;
import com.codename1.e2e.grpc.GetProductRequest;
import com.codename1.e2e.grpc.ListProductsRequest;
import com.codename1.e2e.grpc.Product;
import com.codename1.e2e.grpc.ProductList;
import com.codename1.io.grpc.GrpcResponse;
import com.codename1.testing.AbstractTest;
import com.codename1.util.OnComplete;

/**
 * End-to-end gRPC-Web round-trips against the catalog server, exercising the
 * generated protobuf codecs for an enum field, a repeated scalar, a double,
 * and nested/repeated messages.
 */
public class GrpcProtocolTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        CatalogGrpc grpc = CatalogGrpc.of(E2eSupport.baseUrl() + "/grpc");

        // --- unary: message with enum + repeated string + double ---
        final Product[] product = new Product[1];
        final int[] s1 = { -99 };
        final boolean[] d1 = { false };
        grpc.getProduct(new GetProductRequest(1L), null, new OnComplete<GrpcResponse<Product>>() {
            public void completed(GrpcResponse<Product> r) { s1[0] = r.getResponseCode(); product[0] = r.getResponseData(); d1[0] = true; }
        });
        E2eSupport.await(d1);
        assertTrue(d1[0], "gRPC getProduct timed out");
        assertEqual(0, s1[0], "gRPC status OK");
        assertNotNull(product[0], "gRPC product null");
        assertEqual(1L, product[0].id(), "gRPC int64 field");
        assertEqual("The Hobbit", product[0].name(), "gRPC string field");
        assertEqual(Category.BOOKS, product[0].category(), "gRPC enum field");
        assertTrue(product[0].tags().contains("classic"), "gRPC repeated string field");
        assertRange(4.8, product[0].rating(), 0.001, "gRPC double field");

        // --- nested/repeated message + enum request field ---
        final ProductList[] list = new ProductList[1];
        final boolean[] d2 = { false };
        grpc.listProducts(new ListProductsRequest(Category.BOOKS), null, new OnComplete<GrpcResponse<ProductList>>() {
            public void completed(GrpcResponse<ProductList> r) { list[0] = r.getResponseData(); d2[0] = true; }
        });
        E2eSupport.await(d2);
        assertTrue(d2[0], "gRPC listProducts timed out");
        assertNotNull(list[0], "gRPC list null");
        assertNotNull(list[0].products(), "gRPC repeated message null");
        assertEqual(2, list[0].products().size(), "gRPC repeated message count");
        for (Product p : list[0].products()) {
            assertEqual(Category.BOOKS, p.category(), "gRPC nested enum");
        }
        return true;
    }

    @Override
    public int getTimeoutMillis() {
        return 90000;
    }
}
