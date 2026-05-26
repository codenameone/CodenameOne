package com.codename1.ai.mlkit.docscan;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentScannerTest {

    /** Mock implementation of NativeDocumentScanner for headless JVM tests. */
    static class MockBridge implements NativeDocumentScanner {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public String scanToFile(byte[] imageBytes) { return "/tmp/x.jpg"; }
    }

    @Test
    void mock_returns_path() {
        MockBridge b = new MockBridge();
        assertEquals("/tmp/x.jpg", b.scanToFile(new byte[]{1}));
    }
}
