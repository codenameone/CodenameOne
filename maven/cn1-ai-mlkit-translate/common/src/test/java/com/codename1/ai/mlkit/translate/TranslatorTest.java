package com.codename1.ai.mlkit.translate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TranslatorTest {

    /** Mock implementation of NativeTranslator for headless JVM tests. */
    static class MockBridge implements NativeTranslator {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public String translate(String text, String sourceLang, String targetLang) {
            return text + "@" + sourceLang + "->" + targetLang;
        }
    }

    @Test
    void mock_translate_round_trip() {
        MockBridge b = new MockBridge();
        assertEquals("hi@en->fr", b.translate("hi", "en", "fr"));
    }
}
