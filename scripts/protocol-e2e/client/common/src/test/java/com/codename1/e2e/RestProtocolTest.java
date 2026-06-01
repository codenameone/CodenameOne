package com.codename1.e2e;

import com.codename1.e2e.rest.Greeting;
import com.codename1.e2e.rest.GreetingApi;
import com.codename1.io.rest.Response;
import com.codename1.testing.AbstractTest;
import com.codename1.util.OnComplete;

/** End-to-end REST (OpenAPI) round-trip against the Spring Boot test server. */
public class RestProtocolTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        GreetingApi api = GreetingApi.of(E2eSupport.baseUrl());
        final Greeting[] data = new Greeting[1];
        final int[] code = { -1 };
        final boolean[] done = { false };
        api.greeting("Shai", null, new OnComplete<Response<Greeting>>() {
            public void completed(Response<Greeting> r) {
                code[0] = r.getResponseCode();
                data[0] = r.getResponseData();
                done[0] = true;
            }
        });
        E2eSupport.await(done);

        assertTrue(done[0], "REST call timed out");
        assertEqual(200, code[0], "REST HTTP status");
        assertNotNull(data[0], "REST response data was null");
        assertEqual("Hello, Shai!", data[0].message, "REST greeting message");
        assertEqual("rest", data[0].transport, "REST transport tag");
        return true;
    }

    @Override
    public int getTimeoutMillis() {
        return 90000;
    }
}
