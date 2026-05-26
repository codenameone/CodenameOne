package com.codename1.ai.mlkit.text;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TextRecognizerTest {

    /** Mock implementation of NativeTextRecognizer for headless JVM tests. */
    static class MockBridge implements NativeTextRecognizer {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        String response = "hello";
        public String recognize(byte[] imageBytes) {
            if (imageBytes == null) throw new NullPointerException();
            return response;
        }
    }

    @Test
    void bridge_returns_canned_string() {
        MockBridge b = new MockBridge();
        assertEquals("hello", b.recognize(new byte[]{1, 2, 3}));
    }

    @Test
    void bridge_reports_supported() {
        MockBridge b = new MockBridge();
        assertTrue(b.isSupported());
    }

    @Test
    void bridge_rejects_null_input() {
        MockBridge b = new MockBridge();
        assertThrows(NullPointerException.class, () -> b.recognize(null));
    }
}
