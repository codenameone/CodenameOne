package com.codename1.ai.whisper;

public class NativeWhisperRecognizerImpl implements NativeWhisperRecognizer {
    public String transcribe(String modelPath, String audioPath) {
        // Simulator stub. Real whisper.cpp JNA backend is opt-in.
        return "[whisper simulator stub] model=" + modelPath + " audio=" + audioPath;
    }

    public boolean isSupported() {
        return true;
    }
}
