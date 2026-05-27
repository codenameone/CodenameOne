package com.codename1.ai.imagegen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StableDiffusionTest {

    /** Mock implementation of NativeStableDiffusion for headless JVM tests. */
    static class MockBridge implements NativeStableDiffusion {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public byte[] generate(String p, int w, int h, int s) { return new byte[]{1,2,3}; }
    }

    @Test
    void mock_generates_three_bytes() {
        MockBridge b = new MockBridge();
        assertEquals(3, b.generate("p", 64, 64, 10).length);
    }
}
