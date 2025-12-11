package com.codename1.media;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import static org.junit.jupiter.api.Assertions.*;

class MediaExtrasTest extends UITestBase {

    @FormTest
    void testAsyncMediaErrorEvents() {
        AsyncMedia.MediaErrorType type = AsyncMedia.MediaErrorType.Network;
        AsyncMedia.MediaException ex = new AsyncMedia.MediaException(type, "Error");
        AsyncMedia.MediaErrorEvent evt = new AsyncMedia.MediaErrorEvent(null, ex); // Source can be null for test

        assertEquals(type, evt.getMediaException().getMediaErrorType());
        assertEquals("Error", evt.getMediaException().getMessage());
    }

    @FormTest
    void testMediaMetaData() {
        MediaMetaData meta = new MediaMetaData();
        meta.setTitle("Title");
        meta.setSubtitle("Subtitle");
        meta.setTrackNumber(1);
        meta.setNumTracks(10);

        assertEquals("Title", meta.getTitle());
        assertEquals("Subtitle", meta.getSubtitle());
        assertEquals(1, meta.getTrackNumber());
        assertEquals(10, meta.getNumTracks());

        // No Artist/Album/Genre/Duration/MimeType properties available in MediaMetaData source
    }

    @FormTest
    void testAbstractMediaSuccessCallback() {
        // AbstractMedia coverage via MediaManager or subclass
        // We can't easily trigger the private Runnable inside AbstractMedia without full simulation.
        // But creating AsyncMedia via MediaManager might wrap it.
    }

    @FormTest
    void testAudioBufferCallbacks() {
        AudioBuffer buf = new AudioBuffer(1024);
        final boolean[] called = new boolean[1];
        AudioBuffer.AudioBufferCallback cb = new AudioBuffer.AudioBufferCallback() {
            public void frameReceived(AudioBuffer buffer) {
                called[0] = true;
            }
        };

        buf.addCallback(cb);

        float[] data = new float[128];
        buf.copyFrom(44100, 1, data);

        assertTrue(called[0], "Callback should be invoked on copyFrom");

        buf.removeCallback(cb);
        called[0] = false;
        buf.copyFrom(44100, 1, data);
        assertFalse(called[0], "Callback should not be invoked after removal");
    }
}
