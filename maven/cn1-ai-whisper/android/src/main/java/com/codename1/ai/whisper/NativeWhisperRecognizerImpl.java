package com.codename1.ai.whisper;


public class NativeWhisperRecognizerImpl {
    // Android side uses whisper.cpp's prebuilt JNI wrapper packaged inside
    // the cn1lib's nativeand zip. The build server injects the .so into the
    // jniLibs directory via the AiDependencyTable's androidNativeDir entry.
    public String transcribe(String modelPath, String audioPath) {
        try {
            System.loadLibrary("whisper");
        } catch (UnsatisfiedLinkError ule) {
            throw new RuntimeException("whisper native library not found", ule);
        }
        return nativeTranscribe(modelPath, audioPath);
    }

    private native String nativeTranscribe(String modelPath, String audioPath);

    public boolean isSupported() {
        return true;
    }
}
