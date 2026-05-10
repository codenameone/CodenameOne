package com.codename1.ant;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AntExecutorTest {

    @Test
    public void returnsNullWhenNoServerErrorMarkers() {
        assertNull(AntExecutor.extractServerErrorDetail(null));
        assertNull(AntExecutor.extractServerErrorDetail(""));
        assertNull(AntExecutor.extractServerErrorDetail("Just some build output\nNothing interesting here"));
    }

    @Test
    public void capturesServerJsonBodyAndStatus() {
        String log = "Sending build request to the server, notice that the build might take a while to complete!\n"
                + "Sending build to account: shai@codenameone.com\n"
                + "Response message from server is: Internal Server Error\n"
                + "Server Detailed Error Message: {\"timestamp\":\"2026-05-10T03:43:19.633+00:00\",\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/appsec/7.0/build/upload\"}\n"
                + "java.io.IOException: Server returned HTTP response code: 500 for URL: https://cloud.codenameone.com/appsec/7.0/build/upload\n"
                + "    at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1700)";

        String detail = AntExecutor.extractServerErrorDetail(log);

        assertTrue("expected response message line, got: " + detail,
                detail.contains("Response message from server is: Internal Server Error"));
        assertTrue("expected JSON body, got: " + detail,
                detail.contains("Server Detailed Error Message: {\"timestamp\""));
        assertTrue("expected HTTP status line, got: " + detail,
                detail.contains("Server returned HTTP response code: 500"));
    }

    @Test
    public void ignoresUnrelatedLines() {
        String log = "Building project\nCompiling sources\nResponse message from server is: OK\nDone";
        String detail = AntExecutor.extractServerErrorDetail(log);
        assertEquals("Response message from server is: OK", detail);
    }
}
