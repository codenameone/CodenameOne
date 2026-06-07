package com.codename1.ai.mlkit.pose;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PoseDetectorTest {

    /** Mock implementation of NativePoseDetector for headless JVM tests. */
    static class MockBridge implements NativePoseDetector {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public float[] detect(byte[] imageBytes) { return new float[99]; }
    }

    @Test
    void mock_returns_33_landmarks() {
        MockBridge b = new MockBridge();
        assertEquals(99, b.detect(new byte[]{1}).length);
    }
}
