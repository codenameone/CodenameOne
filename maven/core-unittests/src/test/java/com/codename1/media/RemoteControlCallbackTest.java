package com.codename1.media;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class RemoteControlCallbackTest extends UITestBase {

    @FormTest
    public void testRemoteControlCallback() {
        // Register a listener
        MyRemoteControlListener listener = new MyRemoteControlListener();
        MediaManager.setRemoteControlListener(listener);

        // Call callbacks
        RemoteControlCallback.skipToNext();
        flushSerialCalls();
        assertTrue(listener.nextCalled, "skipToNext should be called");

        RemoteControlCallback.skipToPrevious();
        flushSerialCalls();
        assertTrue(listener.prevCalled, "skipToPrevious should be called");

        RemoteControlCallback.play();
        flushSerialCalls();
        assertTrue(listener.playCalled, "play should be called");
    }

    static class MyRemoteControlListener extends RemoteControlListener {
        boolean nextCalled;
        boolean prevCalled;
        boolean playCalled;

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
    }
}
