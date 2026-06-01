package com.codename1.e2e;

import com.codename1.e2e.grpc.GreeterGrpc;
import com.codename1.e2e.grpc.HelloReply;
import com.codename1.e2e.grpc.HelloRequest;
import com.codename1.io.grpc.GrpcResponse;
import com.codename1.testing.AbstractTest;
import com.codename1.util.OnComplete;

/** End-to-end gRPC-Web unary round-trip against the test server. */
public class GrpcProtocolTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        GreeterGrpc grpc = GreeterGrpc.of(E2eSupport.baseUrl() + "/grpc");
        HelloRequest req = new HelloRequest();
        req.name = "Leia";

        final HelloReply[] data = new HelloReply[1];
        final int[] status = { -99 };
        final boolean[] done = { false };
        grpc.sayHello(req, null, new OnComplete<GrpcResponse<HelloReply>>() {
            public void completed(GrpcResponse<HelloReply> r) {
                status[0] = r.getResponseCode();
                data[0] = r.getResponseData();
                done[0] = true;
            }
        });
        E2eSupport.await(done);

        assertTrue(done[0], "gRPC call timed out");
        assertEqual(0, status[0], "gRPC status should be OK (0)");
        assertNotNull(data[0], "gRPC response data was null");
        assertEqual("Hello, Leia!", data[0].message, "gRPC reply message");
        return true;
    }

    @Override
    public int getTimeoutMillis() {
        return 90000;
    }
}
