package com.codename1.ai.mlkit.barcode;

public class NativeBarcodeScannerImpl implements NativeBarcodeScanner {

    private static boolean hintsEnsured;
    private static synchronized void ensureSimulatorHints() {
        if (hintsEnsured) return;
        hintsEnsured = true;
        java.util.Map<String, String> hints =
            com.codename1.ui.Display.getInstance().getProjectBuildHints();
        if (hints == null) return;  // not running in the simulator
            if (!hints.containsKey("ios.NSCameraUsageDescription")) {
                com.codename1.ui.Display.getInstance()
                    .setProjectBuildHint("ios.NSCameraUsageDescription", "This app uses the camera to scan barcodes.");
            }
    }

    public NativeBarcodeScannerImpl() {
        ensureSimulatorHints();
    }
    public String[] scan(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return new String[0];
        // Deterministic stub for simulator runs.
        return new String[]{"SIMULATOR_BARCODE_" + imageBytes.length};
    }

    public boolean isSupported() {
        return true;
    }
}
