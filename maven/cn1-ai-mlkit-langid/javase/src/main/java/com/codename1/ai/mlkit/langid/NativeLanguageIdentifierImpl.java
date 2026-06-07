package com.codename1.ai.mlkit.langid;

public class NativeLanguageIdentifierImpl implements NativeLanguageIdentifier {
    public String identify(String input) {
        // Crude language ID stub for simulator.
        if (input == null || input.isEmpty()) return "und";
        return "en";
    }

    public boolean isSupported() {
        return true;
    }
}
