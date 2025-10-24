package com.codename1.io.rest;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Data;
import com.codename1.io.TestImplementationProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestBuilderTest {
    @BeforeEach
    void setUp() {
        TestImplementationProvider.installImplementation(true);
    }

    @AfterEach
    void tearDown() {
        TestImplementationProvider.resetImplementation();
    }

    @Test
    void createRequestPopulatesConnection() throws Exception {
        RequestBuilder builder = new RequestBuilder("POST", "https://example.com/items/{id}");
        builder.contentType("application/json")
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

        ConnectionRequest request = invokeCreateRequest(builder, false);

        assertEquals("https://example.com/items/42", request.getUrl());
        assertEquals("POST", request.getHttpMethod());
        assertFalse(request.isPost());
        assertEquals("application/json", request.getContentType());
        assertEquals(ConnectionRequest.CachingMode.MANUAL, request.getCacheMode());
        assertEquals(1500, request.getTimeout());
        assertEquals(3000, request.getReadTimeout());
        assertFalse(request.isCookiesEnabled());
        assertEquals(ConnectionRequest.PRIORITY_HIGH, request.getPriority());
        assertTrue(request.isInsecure());

        LinkedHashMap args = getArguments(request);
        assertEquals("hello%20world", args.get("search"));
        assertArrayEquals(new String[]{"one", "two"}, (String[]) args.get("filter"));

        Map headers = getHeaders(request);
        assertEquals("value", headers.get("X-Test"));
        assertTrue(headers.containsKey("Authorization"));
        assertTrue(headers.get("Authorization").toString().startsWith("Basic "));

        Data body = request.getRequestBodyData();
        assertNotNull(body);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.appendTo(out);
        assertEquals("{\"name\":\"demo\"}", out.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    void stringErrorHandlerReceivesResponse() throws Exception {
        RequestBuilder builder = new RequestBuilder("GET", "https://example.com");
        AtomicReference<Response<String>> captured = new AtomicReference<>();
        builder.onErrorCodeString(captured::set);
        RequestBuilder.Connection connection = (RequestBuilder.Connection) invokeCreateRequest(builder, false);

        setField(ConnectionRequest.class, connection, "responseCode", 500);
        setField(ConnectionRequest.class, connection, "responseErrorMessge", "Server Error");

        connection.handleErrorResponseCode(500, "Server Error");
        connection.readUnzipedResponse(new ByteArrayInputStream("failure".getBytes(StandardCharsets.UTF_8)));
        connection.postResponse();

        Response<String> response = captured.get();
        assertNotNull(response);
        assertEquals(500, response.getResponseCode());
        assertEquals("failure", response.getResponseData());
        assertEquals("Server Error", response.getResponseErrorMessage());
    }

    @Test
    void jsonErrorHandlerRespectsParserConfiguration() throws Exception {
        RequestBuilder builder = new RequestBuilder("GET", "https://example.com");
        AtomicReference<Response<Map>> captured = new AtomicReference<>();
        builder.useBoolean(true)
                .useLongs(true)
                .onErrorCodeJSON(captured::set);
        RequestBuilder.Connection connection = (RequestBuilder.Connection) invokeCreateRequest(builder, true);

        setField(ConnectionRequest.class, connection, "responseCode", 400);
        setField(ConnectionRequest.class, connection, "responseErrorMessge", "Bad Request");

        connection.handleErrorResponseCode(400, "Bad Request");
        connection.readUnzipedResponse(new ByteArrayInputStream("{\"flag\":true,\"count\":1}".getBytes(StandardCharsets.UTF_8)));
        connection.postResponse();

        Response<Map> response = captured.get();
        assertNotNull(response);
        Map data = response.getResponseData();
        assertTrue(data.get("flag") instanceof Boolean);
        assertTrue((Boolean) data.get("flag"));
        assertTrue(data.get("count") instanceof Long);
        assertEquals(1L, ((Long) data.get("count")).longValue());
    }

    @Test
    void checkFetchedPreventsFurtherMutation() throws Exception {
        RequestBuilder builder = new RequestBuilder("GET", "https://example.com");
        Field fetchedField = RequestBuilder.class.getDeclaredField("fetched");
        fetchedField.setAccessible(true);
        fetchedField.setBoolean(builder, true);
        assertThrows(RuntimeException.class, () -> builder.header("After", "value"));
    }

    private ConnectionRequest invokeCreateRequest(RequestBuilder builder, boolean parseJson) throws Exception {
        Method create = RequestBuilder.class.getDeclaredMethod("createRequest", boolean.class);
        create.setAccessible(true);
        return (ConnectionRequest) create.invoke(builder, parseJson);
    }

    private LinkedHashMap getArguments(ConnectionRequest request) throws Exception {
        Field argsField = ConnectionRequest.class.getDeclaredField("requestArguments");
        argsField.setAccessible(true);
        LinkedHashMap args = (LinkedHashMap) argsField.get(request);
        assertNotNull(args);
        return args;
    }

    private Map getHeaders(ConnectionRequest request) throws Exception {
        Field headersField = ConnectionRequest.class.getDeclaredField("userHeaders");
        headersField.setAccessible(true);
        Map headers = (Map) headersField.get(request);
        assertNotNull(headers);
        return headers;
    }

    private void setField(Class<?> type, Object instance, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
