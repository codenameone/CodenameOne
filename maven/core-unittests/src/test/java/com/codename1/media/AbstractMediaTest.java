package com.codename1.media;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

public class AbstractMediaTest extends UITestBase {

    class TestMedia extends AbstractMedia {
        boolean playing;
        Runnable onPlay;
        Runnable onPause;

        @Override
        protected void playImpl() {
            playing = true;
            if (onPlay != null) onPlay.run();
        }

        @Override
        protected void pauseImpl() {
            playing = false;
            if (onPause != null) onPause.run();
        }

        @Override
        public boolean isPlaying() {
            return playing;
        }

        @Override
        public int getTime() { return 0; }

        @Override
        public void setTime(int time) {}

        @Override
        public int getDuration() { return 0; }

        @Override
        public void setVolume(int vol) {}

        @Override
        public int getVolume() { return 100; }

        @Override
        public boolean isVideo() { return false; }

        @Override
        public Object getVariable(String key) { return null; }

        @Override
        public void setVariable(String key, Object value) {}

        @Override
        public void prepare() {}

        @Override
        public void cleanup() {}

        @Override
        public com.codename1.ui.Component getVideoComponent() { return null; }

        @Override
        public boolean isFullScreen() { return false; }

        @Override
        public void setFullScreen(boolean fullScreen) {}

        @Override
        public boolean isNativePlayerMode() { return false; }

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {}
    }

    @FormTest
    public void testConcurrentPauseAsync() {
        TestMedia media = new TestMedia();
        media.playing = true;

        AtomicBoolean pauseCalled = new AtomicBoolean(false);
        media.onPause = () -> {
            pauseCalled.set(true);
        };

        AsyncMedia.PauseRequest req1 = media.pauseAsync();
        AsyncMedia.PauseRequest req2 = media.pauseAsync();

        assertFalse(req1.isDone());
        assertFalse(req2.isDone());

        media.fireMediaStateChange(AsyncMedia.State.Paused);

        assertTrue(req1.isDone());
        assertTrue(req2.isDone());
    }

    @FormTest
    public void testConcurrentPauseAsyncError() {
        TestMedia media = new TestMedia();
        media.playing = true;

        AsyncMedia.PauseRequest req1 = media.pauseAsync();
        AsyncMedia.PauseRequest req2 = media.pauseAsync();

        Exception ex = new RuntimeException("Fail");
        media.fireMediaError(new AsyncMedia.MediaException(AsyncMedia.MediaErrorType.Unknown, ex));

        assertTrue(req1.isDone());
        assertTrue(req2.isDone());

        assertThrows(AsyncResource.AsyncExecutionException.class, () -> req1.get());
        assertThrows(AsyncResource.AsyncExecutionException.class, () -> req2.get());
    }
}
