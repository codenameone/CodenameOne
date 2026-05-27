package com.codename1.ai.mlkit.segmentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SelfieSegmenterTest {

    /** Mock implementation of NativeSelfieSegmenter for headless JVM tests. */
    static class MockBridge implements NativeSelfieSegmenter {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public byte[] segment(byte[] imageBytes) { return new byte[16]; }
    }

    @Test
    void mock_returns_mask_bytes() {
        MockBridge b = new MockBridge();
        assertEquals(16, b.segment(new byte[]{1}).length);
    }
}
