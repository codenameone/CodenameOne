package com.codename1.io.rest;

import com.codename1.junit.UITestBase;
import com.codename1.properties.IntProperty;
import com.codename1.properties.Property;
import com.codename1.properties.PropertyBusinessObject;
import com.codename1.properties.PropertyIndex;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;
import com.codename1.util.Callback;
import com.codename1.util.OnComplete;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestBuilderActionListenerTest extends UITestBase {
    private static final String BASE_URL = "https://example.com";

    @BeforeEach
    void clearConnections() {
        implementation.clearConnections();
        implementation.clearQueuedRequests();
    }

    @Test
    void fetchAsJsonMapInvokesCallback() {
        prepareJsonConnection(BASE_URL + "/json", "{\"status\":\"ok\",\"count\":2}");

        RequestBuilder builder = new RequestBuilder("GET", BASE_URL + "/json");
        final AtomicReference<Response<Map>> holder = new AtomicReference<Response<Map>>();
        CountDownLatch latch = new CountDownLatch(1);

        builder.fetchAsJsonMap(new OnComplete<Response<Map>>() {
            public void completed(Response<Map> value) {
                holder.set(value);
                latch.countDown();
            }
        });

        waitFor(latch, 2000);

        Response<Map> response = holder.get();
        assertNotNull(response);
        assertEquals(200, response.getResponseCode());
        assertEquals("ok", response.getResponseData().get("status"));
    }

    @Test
    void getAsJsonMapInvokesCallback() {
        prepareJsonConnection(BASE_URL + "/legacy-json", "{\"status\":\"legacy\"}");

        RequestBuilder builder = new RequestBuilder("GET", BASE_URL + "/legacy-json");
        final AtomicReference<Response<Map>> holder = new AtomicReference<Response<Map>>();
        CountDownLatch latch = new CountDownLatch(1);

        builder.getAsJsonMap(new SuccessCallback<Response<Map>>() {
            public void onSucess(Response<Map> value) {
                holder.set(value);
                latch.countDown();
            }
        });

        waitFor(latch, 2000);

        Response<Map> response = holder.get();
        assertNotNull(response);
        assertEquals("legacy", response.getResponseData().get("status"));
    }

    @Test
    void fetchAsPropertiesParsesBusinessObject() {
        prepareJsonConnection(BASE_URL + "/item", "{\"name\":\"Widget\",\"count\":4}");

        RequestBuilder builder = new RequestBuilder("GET", BASE_URL + "/item");
        final AtomicReference<Response<PropertyBusinessObject>> holder = new AtomicReference<Response<PropertyBusinessObject>>();
        CountDownLatch latch = new CountDownLatch(1);

        builder.fetchAsProperties(new OnComplete<Response<PropertyBusinessObject>>() {
            public void completed(Response<PropertyBusinessObject> value) {
                holder.set(value);
                latch.countDown();
            }
        }, SampleItem.class);

        waitFor(latch, 2000);

        Response<PropertyBusinessObject> response = holder.get();
        assertNotNull(response);
        SampleItem item = (SampleItem) response.getResponseData();
        assertEquals("Widget", item.name.get());
        assertEquals(4, item.count.get());
    }

    @Test
    void fetchAsPropertyListParsesBusinessObjectList() {
        prepareJsonConnection(BASE_URL + "/items", "{\"items\":[{\"name\":\"A\",\"count\":1},{\"name\":\"B\",\"count\":2}]}");

        RequestBuilder builder = new RequestBuilder("GET", BASE_URL + "/items");
        final AtomicReference<Response<List<PropertyBusinessObject>>> holder = new AtomicReference<Response<List<PropertyBusinessObject>>>();
        CountDownLatch latch = new CountDownLatch(1);

        builder.fetchAsPropertyList(new OnComplete<Response<List<PropertyBusinessObject>>>() {
            public void completed(Response<List<PropertyBusinessObject>> value) {
                holder.set(value);
                latch.countDown();
            }
        }, SampleItem.class, "items");

        waitFor(latch, 2000);

        Response<List<PropertyBusinessObject>> response = holder.get();
        assertNotNull(response);
        List<PropertyBusinessObject> items = response.getResponseData();
        assertEquals(2, items.size());
        SampleItem first = (SampleItem) items.get(0);
        SampleItem second = (SampleItem) items.get(1);
        assertEquals("A", first.name.get());
        assertEquals(2, second.count.get());
    }

    @Test
    void getAsBytesAsyncSupportsCallbackAndOnComplete() {
        byte[] payload = "bytes".getBytes(StandardCharsets.UTF_8);
        prepareBytesConnection(BASE_URL + "/bytes-legacy", payload);
        prepareBytesConnection(BASE_URL + "/bytes", payload);

        final AtomicReference<Response<byte[]>> callbackHolder = new AtomicReference<Response<byte[]>>();
        final AtomicReference<Response<byte[]>> onCompleteHolder = new AtomicReference<Response<byte[]>>();
        CountDownLatch latch = new CountDownLatch(2);

        RequestBuilder legacyBuilder = new RequestBuilder("GET", BASE_URL + "/bytes-legacy");
        legacyBuilder.getAsBytesAsync(new Callback<Response<byte[]>>() {
            public void onSucess(Response<byte[]> value) {
                callbackHolder.set(value);
                latch.countDown();
            }

            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
            }
        });

        RequestBuilder builder = new RequestBuilder("GET", BASE_URL + "/bytes");
        builder.fetchAsBytes(new OnComplete<Response<byte[]>>() {
            public void completed(Response<byte[]> value) {
                onCompleteHolder.set(value);
                latch.countDown();
            }
        });

        waitFor(latch, 2000);

        assertTrue(Arrays.equals(payload, callbackHolder.get().getResponseData()));
        assertTrue(Arrays.equals(payload, onCompleteHolder.get().getResponseData()));
    }

    @Test
    void getAsStringAsyncSupportsCallbackAndOnComplete() {
        prepareBytesConnection(BASE_URL + "/string-legacy", "hello".getBytes(StandardCharsets.UTF_8));
        prepareBytesConnection(BASE_URL + "/string", "world".getBytes(StandardCharsets.UTF_8));

        final AtomicReference<Response<String>> callbackHolder = new AtomicReference<Response<String>>();
        final AtomicReference<Response<String>> onCompleteHolder = new AtomicReference<Response<String>>();
        CountDownLatch latch = new CountDownLatch(2);

        RequestBuilder legacyBuilder = new RequestBuilder("GET", BASE_URL + "/string-legacy");
        legacyBuilder.getAsStringAsync(new Callback<Response<String>>() {
            public void onSucess(Response<String> value) {
                callbackHolder.set(value);
                latch.countDown();
            }

            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
            }
        });

        RequestBuilder builder = new RequestBuilder("GET", BASE_URL + "/string");
        builder.fetchAsString(new OnComplete<Response<String>>() {
            public void completed(Response<String> value) {
                onCompleteHolder.set(value);
                latch.countDown();
            }
        });

        waitFor(latch, 2000);

        assertEquals("hello", callbackHolder.get().getResponseData());
        assertEquals("world", onCompleteHolder.get().getResponseData());
    }

    private void prepareJsonConnection(String url, String json) {
        prepareBytesConnection(url, json.getBytes(StandardCharsets.UTF_8));
    }

    private void prepareBytesConnection(String url, byte[] payload) {
        TestConnection connection = implementation.createConnection(url);
        connection.setInputData(payload);
        connection.setContentLength(payload.length);
        connection.setResponseCode(200);
        connection.setResponseMessage("OK");
    }

    static class SampleItem implements PropertyBusinessObject {
        final Property<String, SampleItem> name = new Property<String, SampleItem>("name");
        final IntProperty<SampleItem> count = new IntProperty<SampleItem>("count");
        private final PropertyIndex index = new PropertyIndex(this, "SampleItem", name, count);

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }
}
