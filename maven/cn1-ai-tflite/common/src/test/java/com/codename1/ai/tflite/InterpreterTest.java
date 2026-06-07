package com.codename1.ai.tflite;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InterpreterTest {

    /** Mock implementation of NativeInterpreter for headless JVM tests. */
    static class MockBridge implements NativeInterpreter {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public float[] run(byte[] modelBytes, float[] input, int outputLength) {
            float[] r = new float[outputLength];
            for (int i = 0; i < r.length; i++) r[i] = i;
            return r;
        }
    }

    @Test
    void mock_returns_increasing_vector() {
        MockBridge b = new MockBridge();
        float[] r = b.run(new byte[0], new float[]{1f, 2f}, 4);
        assertEquals(4, r.length);
        assertEquals(3.0f, r[3], 1e-6);
    }
}
