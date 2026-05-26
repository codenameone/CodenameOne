package com.codename1.ai.mlkit.segmentation;

public class NativeSelfieSegmenterImpl implements NativeSelfieSegmenter {
    public byte[] segment(byte[] imageBytes) {
        // 8x8 checkerboard stub.
        byte[] out = new byte[64];
        for (int i = 0; i < 64; i++) out[i] = (byte)(((i / 8) + (i % 8)) % 2 == 0 ? 255 : 0);
        return out;
    }

    public boolean isSupported() {
        return true;
    }
}
