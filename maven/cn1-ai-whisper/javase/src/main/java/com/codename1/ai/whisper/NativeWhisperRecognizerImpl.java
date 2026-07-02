package com.codename1.ai.whisper;

import com.codename1.media.TranscriptionSegment;

import java.util.ArrayList;
import java.util.List;

public class NativeWhisperRecognizerImpl implements NativeWhisperRecognizer {
    public String transcribe(String modelPath, String audioPath) {
        // Simulator stub. Real whisper.cpp JNA backend is opt-in.
        return "[whisper simulator stub] model=" + modelPath + " audio=" + audioPath;
    }

    public String transcribeSegments(String modelPath, String audioPath) {
        List<TranscriptionSegment> segments = new ArrayList<TranscriptionSegment>();
        segments.add(new TranscriptionSegment(0, 0, transcribe(modelPath, audioPath)));
        return WhisperRecognizer.encodeSegmentsPayload(segments);
    }

    public boolean isSupported() {
        return true;
    }
}
