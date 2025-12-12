package com.codename1.media;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.DisplayTest;
import static org.junit.jupiter.api.Assertions.*;

public class RemoteControlCallbackTest extends UITestBase {

    @FormTest
    public void testRemoteControlCallback() {
        // Register a listener
        MyRemoteControlListener listener = new MyRemoteControlListener();
        MediaManager.setRemoteControlListener(listener);

        // Call callbacks
        RemoteControlCallback.skipToNext();
        DisplayTest.flushEdt();
        assertTrue(listener.nextCalled, "skipToNext should be called");

        RemoteControlCallback.skipToPrevious();
        DisplayTest.flushEdt();
        assertTrue(listener.prevCalled, "skipToPrevious should be called");

        RemoteControlCallback.play();
        DisplayTest.flushEdt();
        assertTrue(listener.playCalled, "play should be called");

        // Add test for setVolume (RemoteControlCallback$10)
        RemoteControlCallback.setVolume(0.5f, 0.8f);
        DisplayTest.flushEdt();
        assertEquals(0.5f, listener.leftVol, 0.01f);
        assertEquals(0.8f, listener.rightVol, 0.01f);
    }

    static class MyRemoteControlListener extends RemoteControlListener {
        boolean nextCalled;
        boolean prevCalled;
        boolean playCalled;
        float leftVol = -1f;
        float rightVol = -1f;

        @Override
        public void play() {
            playCalled = true;
        }

        @Override
        public void skipToNext() {
            nextCalled = true;
        }

        @Override
        public void skipToPrevious() {
            prevCalled = true;
        }

        @Override
        public void setVolume(float left, float right) {
            leftVol = left;
            rightVol = right;
        }
    }
}
