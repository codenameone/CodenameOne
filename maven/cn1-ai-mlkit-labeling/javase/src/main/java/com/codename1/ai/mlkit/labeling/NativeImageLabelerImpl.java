package com.codename1.ai.mlkit.labeling;

public class NativeImageLabelerImpl {
    public String[] label(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return new String[0];
        return new String[]{"object", "stub", "simulator"};
    }

    public boolean isSupported() {
        return true;
    }
}
