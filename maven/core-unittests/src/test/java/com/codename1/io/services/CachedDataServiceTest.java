package com.codename1.io.services;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

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
        boolean previousAutoProcessConnections = implementation.isAutoProcessConnections();
        implementation.setAutoProcessConnections(false);

        final boolean[] callbackInvoked = new boolean[1];
        try {
            TestCodenameOneImplementation.TestConnection connection = implementation.createConnection("http://example.com/data");
            connection.setHeader("Last-Modified", "Sun, 02 Jan 2000 00:00:00 GMT");
            connection.setHeader("ETag", "etag-2");
            byte[] payload = "fresh".getBytes();
            connection.setInputData(payload);
            connection.setContentLength(payload.length);

            CachedDataService.updateData(data, new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    callbackInvoked[0] = true;
                    NetworkEvent ne = (NetworkEvent) evt;
                    CachedData updated = (CachedData) ne.getMetaData();
                    data.setModified(updated.getModified());
                    data.setEtag(updated.getEtag());
                    data.setData(updated.getData());
                    data.setFetching(false);
                }
            });
            assertTrue(data.isFetching());

            List<ConnectionRequest> requests = implementation.getQueuedRequests();
            assertEquals(1, requests.size());
            CachedDataService request = (CachedDataService) requests.get(0);
            assertEquals("http://example.com/data", request.getUrl());
            assertFalse(request.isPost());

            request.readHeaders(connection);

            request.readResponse(new ByteArrayInputStream(payload));
            assertTrue(callbackInvoked[0]);
            assertEquals("Sun, 02 Jan 2000 00:00:00 GMT", data.getModified());
            assertEquals("etag-2", data.getEtag());
            assertArrayEquals(payload, data.getData());
            assertFalse(data.isFetching());

            data.setFetching(true);
            request.handleErrorResponseCode(304, "Not Modified");
            assertTrue(data.isFetching());
        } finally {
            implementation.setAutoProcessConnections(previousAutoProcessConnections);
        }
    }
}
