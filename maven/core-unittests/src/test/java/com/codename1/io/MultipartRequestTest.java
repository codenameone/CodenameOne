package com.codename1.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultipartRequestTest {
    @BeforeEach
    void resetImplementation() {
        TestImplementationProvider.installImplementation(true);
    }

    @Test
    void buildRequestBodyIncludesArgumentsAndBinaryData() throws IOException {
        MultipartRequest request = new MultipartRequest();
        request.setBoundary("test-boundary");
        request.addArgument("text", "value with spaces");
        request.addArgumentNoEncoding("raw", "unchanged + data");
        request.addArgument("multi", new String[]{"first", "second"});
        request.addData("file", "hello".getBytes(StandardCharsets.UTF_8), "application/octet-stream");
        request.setFilename("file", "file.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        request.buildRequestBody(out);
        byte[] payload = out.toByteArray();
        String payloadString = new String(payload, StandardCharsets.ISO_8859_1);

        assertTrue(payloadString.contains("--test-boundary"));
        assertTrue(payloadString.contains("Content-Disposition: form-data; name=\"text\""));
        assertTrue(payloadString.contains("value+with+spaces"));
        assertTrue(payloadString.contains("Content-Disposition: form-data; name=\"raw\""));
        assertTrue(payloadString.contains("unchanged + data"));
        assertTrue(payloadString.contains("Content-Disposition: form-data; name=\"multi\""));
        assertTrue(payloadString.contains("first"));
        assertTrue(payloadString.contains("second"));
        assertTrue(payloadString.contains("Content-Disposition: form-data; name=\"file\"; filename=\"file.txt\""));
        assertTrue(payloadString.contains("Content-Type: application/octet-stream"));
        assertTrue(payloadString.contains("hello"));
        assertTrue(payloadString.endsWith("--test-boundary--\r\n"));

        assertEquals(payload.length, request.calculateContentLength());
    }

    @Test
    void base64ToggleControlsEncoding() throws IOException {
        MultipartRequest request = new MultipartRequest();
        request.setBoundary("boundary");
        request.addArgument("text", "encode me");
        request.setBase64Binaries(false);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        request.buildRequestBody(out);
        String payload = out.toString(StandardCharsets.ISO_8859_1.name());

        assertTrue(payload.contains("encode me"));
        assertFalse(payload.contains("encode+me"));
    }

    @Test
    void manualRedirectFlagIsHonored() {
        MultipartRequest request = new MultipartRequest();
        assertTrue(request.isManualRedirect());
        assertTrue(request.onRedirect("http://example.com"));

        request.setManualRedirect(false);
        assertFalse(request.isManualRedirect());
        assertFalse(request.onRedirect("http://example.com"));
    }

    @Test
    void calculateContentLengthMatchesStreamForInputStreamData() throws IOException {
        MultipartRequest request = new MultipartRequest();
        request.setBoundary("stream-boundary");
        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        request.addData("stream", new ByteArrayInputStream(data), data.length, "application/octet-stream");
        request.setFilename("stream", "blob.bin");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        request.buildRequestBody(out);

        assertEquals(out.size(), request.calculateContentLength());
        byte[] payload = out.toByteArray();
        assertTrue(payload.length > data.length);
        assertTrue(containsSubArray(payload, data));
    }

    private static boolean containsSubArray(byte[] outer, byte[] inner) {
        if (inner.length == 0 || outer.length < inner.length) {
            return false;
        }
        for (int i = 0; i <= outer.length - inner.length; i++) {
            boolean match = true;
            for (int j = 0; j < inner.length; j++) {
                if (outer[i + j] != inner[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }
}
