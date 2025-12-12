package com.codename1.media;

import com.codename1.ui.DisplayTest;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaCoverageTest extends UITestBase {

    private void cleanup() {
        MediaManager.setRemoteControlListener(null);
    }

    @FormTest
    public void testRemoteControlCallback() {
        cleanup();
        final AtomicBoolean pauseCalled = new AtomicBoolean(false);
        final AtomicBoolean toggleCalled = new AtomicBoolean(false);
        final AtomicBoolean seekCalled = new AtomicBoolean(false);
        final AtomicBoolean stopCalled = new AtomicBoolean(false);
        final AtomicBoolean ffCalled = new AtomicBoolean(false);
        final AtomicBoolean rwCalled = new AtomicBoolean(false);

        final long[] seekPos = new long[1];

        RemoteControlListener listener = new RemoteControlListener() {
            @Override
            public void pause() {
                pauseCalled.set(true);
            }

            @Override
            public void togglePlayPause() {
                toggleCalled.set(true);
            }

            @Override
            public void seekTo(long pos) {
                seekCalled.set(true);
                seekPos[0] = pos;
            }

            @Override
            public void stop() {
                stopCalled.set(true);
            }

            @Override
            public void fastForward() {
                ffCalled.set(true);
            }

            @Override
            public void rewind() {
                rwCalled.set(true);
            }
        };

        MediaManager.setRemoteControlListener(listener);

        RemoteControlCallback.pause();
        DisplayTest.flushEdt();
        Assertions.assertTrue(pauseCalled.get(), "RemoteControlCallback.pause() should call listener.pause()");

        RemoteControlCallback.togglePlayPause();
        DisplayTest.flushEdt();
        Assertions.assertTrue(toggleCalled.get(), "RemoteControlCallback.togglePlayPause() should call listener.togglePlayPause()");

        RemoteControlCallback.seekTo(12345L);
        DisplayTest.flushEdt();
        Assertions.assertTrue(seekCalled.get(), "RemoteControlCallback.seekTo() should call listener.seekTo()");
        Assertions.assertEquals(12345L, seekPos[0], "RemoteControlCallback.seekTo() should pass correct position");

        // Test missing methods for coverage
        RemoteControlCallback.stop();
        DisplayTest.flushEdt();
        Assertions.assertTrue(stopCalled.get(), "RemoteControlCallback.stop() should call listener.stop()");

        RemoteControlCallback.fastForward();
        DisplayTest.flushEdt();
        Assertions.assertTrue(ffCalled.get(), "RemoteControlCallback.fastForward() should call listener.fastForward()");

        RemoteControlCallback.rewind();
        DisplayTest.flushEdt();
        Assertions.assertTrue(rwCalled.get(), "RemoteControlCallback.rewind() should call listener.rewind()");
    }

    @FormTest
    public void testMediaManagerAsyncMediaTimer() throws InterruptedException {

        final AtomicBoolean playing = new AtomicBoolean(false);
        final AtomicBoolean playCalled = new AtomicBoolean(false);

        Media mockMedia = new Media() {
            @Override
            public void prepare() {}
            @Override
            public void play() {
                playCalled.set(true);
            }
            @Override
            public void pause() {
                playing.set(false);
            }
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
            public boolean isPlaying() {
                return playing.get();
            }
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
        };

        AsyncMedia asyncMedia = MediaManager.getAsyncMedia(mockMedia);

        final AtomicBoolean stateChangedToPlaying = new AtomicBoolean(false);
        asyncMedia.addMediaStateChangeListener(evt -> {
            if (evt.getNewState() == AsyncMedia.State.Playing) {
                stateChangedToPlaying.set(true);
            }
        });

        asyncMedia.play();

        Assertions.assertTrue(playCalled.get(), "Underlying media.play() should be called");
        Assertions.assertFalse(stateChangedToPlaying.get(), "State should not be Playing yet");

        // Now simulate media starting to play after a delay
        Thread.sleep(100);
        playing.set(true);

        long start = System.currentTimeMillis();
        while (!stateChangedToPlaying.get() && System.currentTimeMillis() - start < 3000) {
            DisplayTest.flushEdt();
            Thread.sleep(50);
        }
    }

    @FormTest
    public void testPauseImplTimer() throws InterruptedException {
        // Targets MediaManager$1$2: TimerTask inside pauseImpl of getAsyncMedia

        final AtomicBoolean playing = new AtomicBoolean(true);
        final AtomicBoolean pauseCalled = new AtomicBoolean(false);

        Media mockMedia = new Media() {
            @Override
            public void prepare() {}
            @Override
            public void play() {}
            @Override
            public void pause() {
                pauseCalled.set(true);
            }
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
            public boolean isPlaying() {
                return playing.get();
            }
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
        };

        AsyncMedia asyncMedia = MediaManager.getAsyncMedia(mockMedia);

        final AtomicBoolean pausedEventFired = new AtomicBoolean(false);
        asyncMedia.addMediaStateChangeListener(evt -> {
            if (evt.getNewState() == AsyncMedia.State.Paused) {
                pausedEventFired.set(true);
            }
        });

        // Ensure we are in Playing state initially (mock isPlaying=true)
        Assertions.assertEquals(AsyncMedia.State.Playing, asyncMedia.getState());

        // Call pause. This calls pauseImpl.
        asyncMedia.pause();

        Assertions.assertTrue(pauseCalled.get(), "mockMedia.pause() should be called");

        // Media is still "playing" (mock returns true).
        // So pauseImpl starts a timer (MediaManager$1$2).

        // Now simulate media stopping playback after delay
        Thread.sleep(100);
        playing.set(false);

        // Wait for timer to fire and check state
        long start = System.currentTimeMillis();
        while (!pausedEventFired.get() && System.currentTimeMillis() - start < 3000) {
            DisplayTest.flushEdt();
            Thread.sleep(50);
        }

        Assertions.assertTrue(pausedEventFired.get(), "Timer should detect paused state and fire event");
    }

}
