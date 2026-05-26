package com.codename1.ai.mlkit.smartreply;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SmartReplyTest {

    /** Mock implementation of NativeSmartReply for headless JVM tests. */
    static class MockBridge implements NativeSmartReply {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public String[] suggest(String c) { return new String[]{"ok"}; }
    }

    @Test
    void mock_returns_single_suggestion() {
        MockBridge b = new MockBridge();
        assertEquals(1, b.suggest("[]").length);
    }
}
