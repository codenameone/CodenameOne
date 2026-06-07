package com.codename1.ai.whisper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WhisperRecognizerTest {

    /** Mock implementation of NativeWhisperRecognizer for headless JVM tests. */
    static class MockBridge implements NativeWhisperRecognizer {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public String transcribe(String m, String a) { return "hello world"; }
    }

    @Test
    void mock_returns_transcript() {
        MockBridge b = new MockBridge();
        assertEquals("hello world", b.transcribe("m.bin", "a.wav"));
    }
}
