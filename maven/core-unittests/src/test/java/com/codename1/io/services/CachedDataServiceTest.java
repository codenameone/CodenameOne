package com.codename1.io.services;

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CachedDataServiceTest extends UITestBase {

    @FormTest
    void testUpdateDataQueuesRequestAndReadsResponse() throws IOException {
        CachedDataService.register();

        CachedData data = new CachedData();
        data.setUrl("http://example.com/data");
        data.setData("old".getBytes());
        data.setModified("Sat, 01 Jan 2000 00:00:00 GMT");
        data.setEtag("etag-1");

        implementation.clearQueuedRequests();

        CachedDataService.updateData(data, null);
        assertTrue(data.isFetching());

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        CachedDataService request = (CachedDataService) requests.get(0);
        assertEquals("http://example.com/data", request.getUrl());
        assertFalse(request.isPost());

        TestCodenameOneImplementation.TestConnection connection = implementation.createConnection(request.getUrl());
        connection.setHeader("Last-Modified", "Sun, 02 Jan 2000 00:00:00 GMT");
        connection.setHeader("ETag", "etag-2");

        request.readHeaders(connection);
        assertEquals("Sun, 02 Jan 2000 00:00:00 GMT", data.getModified());
        assertEquals("etag-2", data.getEtag());

        byte[] payload = "fresh".getBytes();
        request.readResponse(new ByteArrayInputStream(payload));
        assertArrayEquals(payload, data.getData());
        assertFalse(data.isFetching());

        data.setFetching(true);
        request.handleErrorResponseCode(304, "Not Modified");
        assertFalse(data.isFetching());
    }
}
