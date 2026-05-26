package com.codename1.ai.mlkit.face;

public class NativeFaceDetectorImpl implements NativeFaceDetector {

    private static boolean hintsEnsured;
    private static synchronized void ensureSimulatorHints() {
        if (hintsEnsured) return;
        hintsEnsured = true;
        java.util.Map<String, String> hints =
            com.codename1.ui.Display.getInstance().getProjectBuildHints();
        if (hints == null) return;  // not running in the simulator
            if (!hints.containsKey("ios.NSCameraUsageDescription")) {
                com.codename1.ui.Display.getInstance()
                    .setProjectBuildHint("ios.NSCameraUsageDescription", "This app uses the camera to detect faces.");
            }
    }

    public NativeFaceDetectorImpl() {
        ensureSimulatorHints();
    }
    public int[] detect(byte[] imageBytes) {
        // Deterministic 1-face stub for simulator runs.
        if (imageBytes == null || imageBytes.length == 0) return new int[0];
        return new int[]{10, 20, 100, 120};
    }

    public boolean isSupported() {
        return true;
    }
}
