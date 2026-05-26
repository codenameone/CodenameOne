package com.codename1.ai.imagegen;

public class NativeStableDiffusionImpl {
    public byte[] generate(String prompt, int width, int height, int steps) {
        // Simulator stub: returns a 1x1 PNG so callers can exercise pipelines.
        return new byte[]{
            (byte)0x89, (byte)'P', (byte)'N', (byte)'G',
            (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A
        };
    }

    public boolean isSupported() {
        return true;
    }
}
