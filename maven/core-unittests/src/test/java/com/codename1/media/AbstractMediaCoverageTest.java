package com.codename1.media;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Component;
import com.codename1.media.AsyncMedia.MediaException;
import com.codename1.media.AsyncMedia.MediaErrorType;

public class AbstractMediaCoverageTest extends UITestBase {

    static class TestMedia extends AbstractMedia {
        boolean playImplCalled = false;
        boolean pauseImplCalled = false;
        boolean playing = false;
        int time = 0;
        int duration = 10000;
        int volume = 100;

        @Override
        protected void playImpl() {
            playImplCalled = true;
        }

        @Override
        protected void pauseImpl() {
            pauseImplCalled = true;
        }

        public void triggerStateChange(State newState) {
            if (newState == State.Playing) playing = true;
            if (newState == State.Paused) playing = false;
            fireMediaStateChange(newState);
        }

        public void triggerError(MediaException ex) {
            fireMediaError(ex);
        }

        @Override public void setTime(int time) { this.time = time; }
        @Override public int getTime() { return time; }
        @Override public int getDuration() { return duration; }
        @Override public void setVolume(int vol) { this.volume = vol; }
        @Override public int getVolume() { return volume; }
        @Override public boolean isPlaying() { return playing; }
        @Override public void cleanup() {}
        @Override public Component getVideoComponent() { return null; }
        @Override public boolean isVideo() { return false; }
        @Override public boolean isFullScreen() { return false; }
        @Override public void setFullScreen(boolean fullScreen) {}
        @Override public boolean isNativePlayerMode() { return false; }
        @Override public void setNativePlayerMode(boolean nativePlayer) {}
        @Override public void setVariable(String key, Object value) {}
        @Override public Object getVariable(String key) { return null; }
        @Override public void prepare() {}
    }

    @FormTest
    public void testAsyncInterleaving() {
        TestMedia media = new TestMedia();
        media.playing = false; // Initially paused

        // 1. playAsync() called. Sets pendingPlayRequest.
        media.playAsync();
        Assertions.assertTrue(media.playImplCalled, "Play should be called");
        media.playImplCalled = false;

        // 2. pauseAsync() called while play is pending (state not yet Playing).
        // This should attach listeners to the pending play request.
        AsyncMedia.PauseRequest pauseReq = media.pauseAsync();

        // 3. Fail the play request.
        // This should trigger the except callback ($9) attached by pauseAsync.
        // The callback calls pauseAsync(out).
        media.triggerError(new MediaException(MediaErrorType.Unknown, "Simulated error"));

        DisplayTest.flushEdt();

        // When play fails, pendingPlayRequest becomes null.
        // The callback calls pauseAsync(out).
        // Since pendingPlayRequest is null, it proceeds to check pendingPauseRequest.
        // It proceeds to check state. State is Paused (playing=false).
        // So it completes immediately without calling pauseImpl.

        Assertions.assertTrue(pauseReq.isDone(), "Pause request should complete");
    }

    @FormTest
    public void testPlayAfterPause() {
        TestMedia media = new TestMedia();
        media.playing = true; // Initially playing so pauseAsync works
        media.pauseImplCalled = false;
        media.playImplCalled = false;

        // 1. pauseAsync
        media.pauseAsync();
        Assertions.assertTrue(media.pauseImplCalled, "Pause should be called");
        media.pauseImplCalled = false;

        // 2. playAsync while pause is pending (state not yet Paused)
        media.playAsync();

        // 3. Succeed pause
        media.triggerStateChange(AsyncMedia.State.Paused);
        DisplayTest.flushEdt();

        // Pause completed. Listener calls playAsync().
        // playAsync sees state Paused. Calls playImpl.

        Assertions.assertTrue(media.playImplCalled, "Play should be called after pause success");
    }

    @FormTest
    public void testPlayAfterPlay() {
        TestMedia media = new TestMedia();
        media.playing = false;
        media.playAsync();
        // pendingPlayRequest is set.

        AsyncMedia.PlayRequest req2 = media.playAsync();
        // This attaches ready/except to pendingPlayRequest ($4, $5).

        media.triggerStateChange(AsyncMedia.State.Playing);
        DisplayTest.flushEdt();

        // Both requests should complete.
        Assertions.assertTrue(req2.isDone(), "Second play request should be done");
    }
}
