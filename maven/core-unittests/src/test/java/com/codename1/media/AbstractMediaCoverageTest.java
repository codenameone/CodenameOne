package com.codename1.media;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.atomic.AtomicBoolean;

public class AbstractMediaCoverageTest extends UITestBase {

    static class MockAsyncMedia extends AbstractMedia {
        private final AtomicBoolean playing = new AtomicBoolean(false);

        @Override
        protected void playImpl() {
            // Simulate async completion
            new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
                playing.set(true);
                // Fire on EDT to be safe with EventDispatcher?
                // AbstractMedia doesn't require it but it's good practice.
                // But let's keep it on thread to simulate native callbacks.
                fireMediaStateChange(State.Playing);
            }).start();
        }

        @Override
        protected void pauseImpl() {
            // Simulate async completion
            new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
                playing.set(false);
                fireMediaStateChange(State.Paused);
            }).start();
        }

        @Override
        public void prepare() {}
        @Override
        public void cleanup() {}
        @Override
        public int getTime() { return 0; }
        @Override
        public void setTime(int time) {}
        @Override
        public int getDuration() { return 0; }
        @Override
        public int getVolume() { return 0; }
        @Override
        public void setVolume(int vol) {}
        @Override
        public boolean isPlaying() { return playing.get(); }
        @Override
        public Component getVideoComponent() { return null; }
        @Override
        public boolean isVideo() { return false; }
        @Override
        public boolean isFullScreen() { return false; }
        @Override
        public void setFullScreen(boolean fullScreen) {}
        @Override
        public boolean isNativePlayerMode() { return false; }
        @Override
        public void setNativePlayerMode(boolean nativePlayer) {}
        @Override
        public void setVariable(String key, Object value) {}
        @Override
        public Object getVariable(String key) { return null; }
    }

    @FormTest
    public void testChainedPauseRequests() throws InterruptedException {
        // This test targets AbstractMedia$10 and AbstractMedia$11
        // which are created when pauseAsync is called while a pause request is pending.

        MockAsyncMedia media = new MockAsyncMedia();
        // Start playing first
        AsyncMedia.PlayRequest playReq = media.playAsync();
        long start = System.currentTimeMillis();
        while (!playReq.isDone() && System.currentTimeMillis() - start < 2000) {
            DisplayTest.flushEdt();
            Thread.sleep(10);
        }
        Assertions.assertTrue(playReq.isDone(), "Play request should complete");

        // Now trigger pause
        AsyncMedia.PauseRequest pauseReq1 = media.pauseAsync();

        // While pauseReq1 is pending, trigger pauseAsync again
        AsyncMedia.PauseRequest pauseReq2 = media.pauseAsync();

        Assertions.assertNotSame(pauseReq1, pauseReq2, "Should create new request object");

        // Wait for pauseReq1
        start = System.currentTimeMillis();
        while (!pauseReq1.isDone() && System.currentTimeMillis() - start < 2000) {
            DisplayTest.flushEdt();
            Thread.sleep(10);
        }
        Assertions.assertTrue(pauseReq1.isDone(), "First pause request should complete");

        // pauseReq2 should also be done (chained)
        start = System.currentTimeMillis();
        while (!pauseReq2.isDone() && System.currentTimeMillis() - start < 2000) {
            DisplayTest.flushEdt();
            Thread.sleep(10);
        }

        Assertions.assertTrue(pauseReq2.isDone(), "Chained pause request should complete");
    }
}
