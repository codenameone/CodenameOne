package com.codename1.ai.whisper;

import com.codename1.media.TranscriptionResult;
import com.codename1.media.TranscriptionSegment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WhisperRecognizerTest {

    /** Mock implementation of NativeWhisperRecognizer for headless JVM tests. */
    static class MockBridge implements NativeWhisperRecognizer {
        boolean supported = true;
        public boolean isSupported() { return supported; }
        public String transcribe(String m, String a) { return "hello world"; }
        public String transcribeSegments(String m, String a) {
            List<TranscriptionSegment> segments = new ArrayList<TranscriptionSegment>();
            segments.add(new TranscriptionSegment(0, 1250, "hello "));
            segments.add(new TranscriptionSegment(1250, 2500, "world"));
            return WhisperRecognizer.encodeSegmentsPayload(segments);
        }
    }

    @Test
    void mock_returns_transcript() {
        MockBridge b = new MockBridge();
        assertEquals("hello world", b.transcribe("m.bin", "a.wav"));
    }

    @Test
    void native_payload_round_trips_segments_and_captions() {
        MockBridge b = new MockBridge();
        TranscriptionResult result = WhisperRecognizer.parseSegments(b.transcribeSegments("m.bin", "a.wav"));
        assertEquals("hello world", result.getText());
        assertEquals(2, result.getSegments().size());
        assertEquals(0, result.getSegments().get(0).getStartTimeMs());
        assertEquals(1250, result.getSegments().get(0).getEndTimeMs());
        assertTrue(result.toSrt().contains("00:00:00,000 --> 00:00:01,250"));
        assertTrue(result.toVtt().startsWith("WEBVTT"));
        assertTrue(result.toVtt().contains("00:00:01.250 --> 00:00:02.500"));
    }
}
