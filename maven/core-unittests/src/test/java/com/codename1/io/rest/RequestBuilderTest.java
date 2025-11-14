package com.codename1.io.rest;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CN;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;
import com.codename1.util.Base64;
import com.codename1.io.rest.Response;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RequestBuilderTest extends UITestBase {
    private static final String BASE_URL = "https://example.com";

    @BeforeEach
    void clearConnections() throws Exception {
        CN.callSeriallyAndWait(new Runnable() {
            public void run() {
                implementation.clearConnections();
            }
        });
    }

    @EdtTest
    void createRequestPopulatesConnection() throws Exception {
        RequestBuilder previewBuilder = createConfiguredBuilder();
        ConnectionRequest preview = previewBuilder.fetchAsString(response -> { });
        NetworkManager.getInstance().killAndWait(preview);

        assertEquals(ConnectionRequest.CachingMode.MANUAL, preview.getCacheMode());
        assertEquals(1500, preview.getTimeout());
        assertEquals(3000, preview.getReadTimeout());
        assertFalse(preview.isCookiesEnabled());
        assertEquals(ConnectionRequest.PRIORITY_HIGH, preview.getPriority());
        assertTrue(preview.isInsecure());

        RequestBuilder builder = createConfiguredBuilder();
        preparePermutations("https://example.com/items/42", Arrays.asList(
                "search=hello%20world&filter=one&filter=two",
                "filter=one&filter=two&search=hello%20world"
        ));

        builder.getAsString();

        TestConnection connection = findSingleConnection();
        assertNotNull(connection);
        assertTrue(connection.getUrl().startsWith("https://example.com/items/42"));
        assertFalse(connection.isPostRequest());

        Map<String, List<String>> query = parseQuery(connection.getUrl());
        assertEquals("hello%20world", query.get("search").get(0));
        List<String> filterValues = query.get("filter");
        assertNotNull(filterValues);
        assertEquals(Arrays.asList("one", "two"), filterValues);

        Map<String, String> headers = connection.getHeaders();
        assertEquals("value", headers.get("X-Test"));
        assertTrue(headers.containsKey("Authorization"));
        assertTrue(headers.get("Authorization").startsWith("Basic "));
        String expectedBasic = Base64.encodeNoNewline("user:pass".getBytes(StandardCharsets.UTF_8));
        assertEquals("Basic " + expectedBasic, headers.get("Authorization"));

        String body = new String(connection.getOutputData(), StandardCharsets.UTF_8);
        assertEquals("{\"name\":\"demo\"}", body);
    }

    @EdtTest
    void stringErrorHandlerReceivesResponse() throws Exception {
        TestConnection connection = implementation.createConnection(BASE_URL);
        connection.setResponseCode(500);
        connection.setResponseMessage("Server Error");
        connection.setInputData("failure".getBytes(StandardCharsets.UTF_8));

        CountDownLatch latch = new CountDownLatch(1);
        final Response<String>[] holder = new Response[1];
        RequestBuilder builder = new RequestBuilder("GET", BASE_URL);
        builder.onErrorCodeString(response -> {
            holder[0] = response;
            latch.countDown();
        });

        builder.getAsString();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Response<String> response = holder[0];
        assertNotNull(response);
        assertEquals(500, response.getResponseCode());
        assertEquals("failure", response.getResponseData());
        assertEquals("Server Error", response.getResponseErrorMessage());
    }

    @EdtTest
    void jsonErrorHandlerRespectsParserConfiguration() throws Exception {
        TestConnection connection = implementation.createConnection(BASE_URL);
        connection.setResponseCode(400);
        connection.setResponseMessage("Bad Request");
        connection.setInputData("{\"flag\":true,\"count\":1}".getBytes(StandardCharsets.UTF_8));

        CountDownLatch latch = new CountDownLatch(1);
        final Response<Map>[] holder = new Response[1];
        RequestBuilder builder = new RequestBuilder("GET", BASE_URL);
        builder.useBoolean(true)
                .useLongs(true)
                .onErrorCodeJSON(response -> {
                    holder[0] = response;
                    latch.countDown();
                });

        builder.getAsJsonMap();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Response<Map> response = holder[0];
        assertNotNull(response);
        Map data = response.getResponseData();
        assertTrue(data.get("flag") instanceof Boolean);
        assertTrue((Boolean) data.get("flag"));
        assertTrue(data.get("count") instanceof Long);
        assertEquals(1L, ((Long) data.get("count")).longValue());
    }

    private RequestBuilder createConfiguredBuilder() {
        return new RequestBuilder("POST", "https://example.com/items/{id}")
                .contentType("application/json")
                .pathParam("id", "42")
                .queryParam("search", "hello world")
                .queryParam("filter", new String[]{"one", "two"})
                .header("X-Test", "value")
                .basicAuth("user", "pass")
                .body("{\"name\":\"demo\"}")
                .cacheMode(ConnectionRequest.CachingMode.MANUAL)
                .timeout(1500)
                .readTimeout(3000)
                .cookiesEnabled(false)
                .priority(ConnectionRequest.PRIORITY_HIGH)
                .insecure(true)
                .postParameters(Boolean.FALSE);
    }

    private void preparePermutations(String base, List<String> permutations) {
        for (String suffix : permutations) {
            TestConnection conn = implementation.createConnection(base + "?" + suffix);
            conn.setInputData(new byte[0]);
            conn.setResponseCode(200);
            conn.setResponseMessage("OK");
        }
    }

    private TestConnection findSingleConnection() {
        Collection<TestConnection> connections = implementation.getConnections();
        assertEquals(1, connections.size());
        return connections.iterator().next();
    }

    private Map<String, List<String>> parseQuery(String url) {
        int idx = url.indexOf('?');
        Map<String, List<String>> out = new HashMap<String, List<String>>();
        if (idx < 0) {
            return out;
        }
        String query = url.substring(idx + 1);
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = parts[0];
            String value = parts.length > 1 ? parts[1] : "";
            List<String> values = out.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                out.put(key, values);
            }
            values.add(value);
        }
        return out;
    }
}
