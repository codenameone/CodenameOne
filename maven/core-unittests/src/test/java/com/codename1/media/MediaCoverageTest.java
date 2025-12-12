package com.codename1.media;

import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;

public class MediaCoverageTest extends UITestBase {

    // Usually cleanup is done in tearDownDisplay of UITestBase, but we can add specific cleanup here
    private void cleanup() {
        MediaManager.setRemoteControlListener(null);
    }

    @FormTest
    public void testRemoteControlCallback() {
        cleanup();
        final boolean[] pauseCalled = new boolean[1];
        final boolean[] toggleCalled = new boolean[1];
        final boolean[] seekCalled = new boolean[1];
        final long[] seekPos = new long[1];

        RemoteControlListener listener = new RemoteControlListener() {
            @Override
            public void pause() {
                pauseCalled[0] = true;
            }

            @Override
            public void togglePlayPause() {
                toggleCalled[0] = true;
            }

            @Override
            public void seekTo(long pos) {
                seekCalled[0] = true;
                seekPos[0] = pos;
            }
        };

        MediaManager.setRemoteControlListener(listener);

        RemoteControlCallback.pause();
        DisplayTest.flushEdt();
        Assertions.assertTrue(pauseCalled[0], "RemoteControlCallback.pause() should call listener.pause()");

        RemoteControlCallback.togglePlayPause();
        DisplayTest.flushEdt();
        Assertions.assertTrue(toggleCalled[0], "RemoteControlCallback.togglePlayPause() should call listener.togglePlayPause()");

        RemoteControlCallback.seekTo(12345L);
        DisplayTest.flushEdt();
        Assertions.assertTrue(seekCalled[0], "RemoteControlCallback.seekTo() should call listener.seekTo()");
        Assertions.assertEquals(12345L, seekPos[0], "RemoteControlCallback.seekTo() should pass correct position");
    }

    @FormTest
    public void testMediaManagerAsyncMediaTimer() throws InterruptedException {
        // Test MediaManager$1$1: TimerTask inside AbstractMedia returned by getAsyncMedia

        // Mock media object
        final boolean[] playing = new boolean[1];
        final boolean[] playCalled = new boolean[1];

        Media mockMedia = new Media() {
            @Override
            public void prepare() {}
            @Override
            public void play() {
                playCalled[0] = true;
                // Don't set playing to true immediately to allow the timer to run
            }
            @Override
            public void pause() {
                playing[0] = false;
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
                return playing[0];
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

        final boolean[] stateChangedToPlaying = new boolean[1];
        asyncMedia.addMediaStateChangeListener(evt -> {
            if (evt.getNewState() == AsyncMedia.State.Playing) {
                stateChangedToPlaying[0] = true;
            }
        });

        // Call play on async media. This calls playImpl in the anonymous class (MediaManager$1).
        // playImpl calls media.play().
        // If media.isPlaying() is false (which it is), it starts a Timer.
        // The TimerTask (MediaManager$1$1) runs periodically and checks if media.isPlaying().
        // If it becomes true, it fires state change.
        asyncMedia.play();

        Assertions.assertTrue(playCalled[0], "Underlying media.play() should be called");
        Assertions.assertFalse(stateChangedToPlaying[0], "State should not be Playing yet");

        // Now simulate media starting to play after a delay
        Thread.sleep(100);
        playing[0] = true;

        // Wait for timer to pick it up. Increase timeout to 5s.
        long start = System.currentTimeMillis();
        while (!stateChangedToPlaying[0] && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(50);
        }

        Assertions.assertTrue(stateChangedToPlaying[0], "Timer should detect playing state and fire event");
    }
}
