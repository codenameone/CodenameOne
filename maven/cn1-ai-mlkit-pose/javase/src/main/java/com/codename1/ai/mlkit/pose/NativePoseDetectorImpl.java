package com.codename1.ai.mlkit.pose;

public class NativePoseDetectorImpl {
    public float[] detect(byte[] imageBytes) {
        float[] out = new float[99];
        for (int i = 0; i < 33; i++) { out[i * 3] = i; out[i * 3 + 2] = 0.5f; }
        return out;
    }

    public boolean isSupported() {
        return true;
    }
}
