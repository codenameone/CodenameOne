package com.codename1.ai.whisper;

import android.media.MediaMetadataRetriever;

import com.codename1.media.TranscriptionSegment;

import java.util.Collections;


public class NativeWhisperRecognizerImpl {
    private static volatile boolean loaded;
    private static volatile boolean loadAttempted;
    private static volatile Throwable loadError;

    public String transcribe(String modelPath, String audioPath) {
        requireWhisper();
        return nativeTranscribe(modelPath, audioPath);
    }

    public String transcribeSegments(String modelPath, String audioPath) {
        requireWhisper();
        try {
            return nativeTranscribeSegments(modelPath, audioPath);
        } catch (UnsatisfiedLinkError ule) {
            String text = nativeTranscribe(modelPath, audioPath);
            long duration = audioDurationMs(audioPath);
            return WhisperRecognizer.encodeSegmentsPayload(Collections.singletonList(
                    new TranscriptionSegment(0, duration, text == null ? "" : text)));
        }
    }

    private native String nativeTranscribe(String modelPath, String audioPath);

    private native String nativeTranscribeSegments(String modelPath, String audioPath);

    public boolean isSupported() {
        return loadWhisper();
    }

    private static void requireWhisper() {
        if (!loadWhisper()) {
            throw new RuntimeException("Whisper JNI library not found", loadError);
        }
    }

    private static boolean loadWhisper() {
        if (loaded || loadAttempted) {
            return loaded;
        }
        synchronized (NativeWhisperRecognizerImpl.class) {
            if (loaded || loadAttempted) {
                return loaded;
            }
            loadAttempted = true;
            try {
                System.loadLibrary("cn1aiwhisper");
                loaded = true;
                return true;
            } catch (UnsatisfiedLinkError cn1BridgeError) {
                try {
                    System.loadLibrary("whisper");
                    loaded = true;
                    return true;
                } catch (UnsatisfiedLinkError legacyError) {
                    loadError = legacyError;
                    if (loadError.getCause() == null) {
                        try {
                            loadError.initCause(cn1BridgeError);
                        } catch (Throwable ignored) {
                        }
                    }
                    return false;
                }
            }
        }
    }

    private static long audioDurationMs(String audioPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(audioPath);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration != null && duration.length() > 0) {
                return Long.parseLong(duration);
            }
        } catch (Throwable ignored) {
            // Fall through to 0ms; this is only used by the text-only JNI fallback.
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }
}
