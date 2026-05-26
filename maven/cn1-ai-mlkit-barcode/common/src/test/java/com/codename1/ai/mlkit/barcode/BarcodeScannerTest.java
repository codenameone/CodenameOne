package com.codename1.ai.mlkit.barcode;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BarcodeScannerTest {

    /** Mock implementation of NativeBarcodeScanner for headless JVM tests. */
    static class MockBridge implements NativeBarcodeScanner {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public String[] scan(byte[] imageBytes) {
            return new String[]{"x", "y"};
        }
    }

    @Test
    void mock_bridge_returns_two_codes() {
        MockBridge b = new MockBridge();
        String[] r = b.scan(new byte[]{1, 2, 3});
        assertEquals(2, r.length);
        assertEquals("x", r[0]);
    }

    @Test
    void bridge_reports_supported() {
        assertTrue(new MockBridge().isSupported());
    }
}
