package com.codename1.ai.mlkit.barcode;

public class NativeBarcodeScannerImpl {
    public String[] scan(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return new String[0];
        // Deterministic stub for simulator runs.
        return new String[]{"SIMULATOR_BARCODE_" + imageBytes.length};
    }

    public boolean isSupported() {
        return true;
    }
}
