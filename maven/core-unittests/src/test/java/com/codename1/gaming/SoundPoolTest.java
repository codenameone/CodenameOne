package com.codename1.gaming;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Minimal unit tests for {@link SoundPool} construction and the native-vs-fallback
/// selection. Actual playback/mixing needs a real audio backend and is covered on
/// device, not here.
class SoundPoolTest extends UITestBase {

    @Test
    void createReturnsAUsablePool() {
        SoundPool pool = SoundPool.create(8);
        assertNotNull(pool);
        assertTrue(pool.getMaxStreams() > 0);
        // querying the backend must not throw, whichever was selected
        assertDoesNotThrow(pool::isNativeAccelerated);
        pool.release();
    }

    @Test
    void voiceControlsTolerateUnknownVoiceIds() {
        SoundPool pool = SoundPool.create(4);
        // operating on a voice that was never started must be a safe no-op
        pool.setVolume(-1, 0.5f);
        pool.setRate(-1, 1.0f);
        pool.setPan(-1, 0f);
        pool.pause(-1);
        pool.resume(-1);
        pool.stop(-1);
        pool.stopAll();
        pool.release();
    }
}
