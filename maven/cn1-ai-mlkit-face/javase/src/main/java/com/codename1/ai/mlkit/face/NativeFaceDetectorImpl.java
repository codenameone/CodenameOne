package com.codename1.ai.mlkit.face;

public class NativeFaceDetectorImpl implements NativeFaceDetector {
    public int[] detect(byte[] imageBytes) {
        // Deterministic 1-face stub for simulator runs.
        if (imageBytes == null || imageBytes.length == 0) return new int[0];
        return new int[]{10, 20, 100, 120};
    }

    public boolean isSupported() {
        return true;
    }
}
