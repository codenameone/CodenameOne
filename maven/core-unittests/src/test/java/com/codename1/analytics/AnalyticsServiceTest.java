package com.codename1.analytics;

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsServiceTest extends UITestBase {

    @FormTest
    void testVisitQueuesAnalyticsRequest() {
        implementation.clearQueuedRequests();
        AnalyticsService.init("UA-1", "app.example.com");
        AnalyticsService.setAppsMode(true);

        AnalyticsService.visit("Home", "/");

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        ConnectionRequest request = requests.get(0);
        assertTrue(request.getUrl().contains("google-analytics"));
        assertTrue(request.isPost());
    }

    @FormTest
    void testCrashReportQueued() {
        implementation.clearQueuedRequests();
        AnalyticsService.init("UA-2", "app.example.com");
        AnalyticsService.setAppsMode(true);

        AnalyticsService.sendCrashReport(new RuntimeException("boom"), "failure", true);
        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        assertTrue(requests.get(0).getUrl().contains("google-analytics"));
    }
}
