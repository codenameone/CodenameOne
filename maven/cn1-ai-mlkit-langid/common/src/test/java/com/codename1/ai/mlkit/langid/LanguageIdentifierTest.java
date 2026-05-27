package com.codename1.ai.mlkit.langid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LanguageIdentifierTest {

    /** Mock implementation of NativeLanguageIdentifier for headless JVM tests. */
    static class MockBridge implements NativeLanguageIdentifier {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public String identify(String input) { return "en"; }
    }

    @Test
    void mock_identifies_english() {
        MockBridge b = new MockBridge();
        assertEquals("en", b.identify("hello"));
    }
}
