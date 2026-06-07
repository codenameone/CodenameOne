package com.codename1.ai.mlkit.translate;

public class NativeTranslatorImpl implements NativeTranslator {
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null) return "";
        return "[" + sourceLang + "->" + targetLang + "] " + text;
    }

    public boolean isSupported() {
        return true;
    }
}
