package com.codename1.ai.mlkit.labeling;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ImageLabelerTest {

    /** Mock implementation of NativeImageLabeler for headless JVM tests. */
    static class MockBridge implements NativeImageLabeler {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public String[] label(byte[] imageBytes) { return new String[]{"a", "b"}; }
    }

    @Test
    void mock_bridge_returns_labels() {
        MockBridge b = new MockBridge();
        assertArrayEquals(new String[]{"a", "b"}, b.label(new byte[]{1}));
    }
}
