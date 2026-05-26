package com.codename1.ai.mlkit.text;

public class NativeTextRecognizerImpl implements NativeTextRecognizer {

    private static boolean hintsEnsured;
    private static synchronized void ensureSimulatorHints() {
        if (hintsEnsured) return;
        hintsEnsured = true;
        java.util.Map<String, String> hints =
            com.codename1.ui.Display.getInstance().getProjectBuildHints();
        if (hints == null) return;  // not running in the simulator
            if (!hints.containsKey("ios.NSCameraUsageDescription")) {
                com.codename1.ui.Display.getInstance()
                    .setProjectBuildHint("ios.NSCameraUsageDescription", "This app uses the camera to recognise text.");
            }
    }

    public NativeTextRecognizerImpl() {
        ensureSimulatorHints();
    }
    public String recognize(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return "";
        return "[mlkit-text simulator stub] " + imageBytes.length + " bytes";
    }

    public boolean isSupported() {
        return true;
    }
}
