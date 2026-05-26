package com.codename1.ai.mlkit.text;

public class NativeTextRecognizerImpl implements NativeTextRecognizer {
    public String recognize(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return "";
        return "[mlkit-text simulator stub] " + imageBytes.length + " bytes";
    }

    public boolean isSupported() {
        return true;
    }
}
