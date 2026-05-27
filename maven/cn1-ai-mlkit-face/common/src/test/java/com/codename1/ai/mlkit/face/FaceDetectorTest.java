package com.codename1.ai.mlkit.face;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FaceDetectorTest {

    /** Mock implementation of NativeFaceDetector for headless JVM tests. */
    static class MockBridge implements NativeFaceDetector {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public int[] detect(byte[] imageBytes) {
            return new int[]{1, 2, 3, 4, 5, 6, 7, 8};
        }
    }

    @Test
    void mock_bridge_returns_two_faces() {
        MockBridge b = new MockBridge();
        int[] r = b.detect(new byte[]{1});
        assertEquals(8, r.length);
    }
}
