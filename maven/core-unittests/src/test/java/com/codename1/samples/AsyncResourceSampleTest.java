package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.media.Media;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.util.AsyncResource;

import static org.junit.jupiter.api.Assertions.*;

class AsyncResourceSampleTest extends UITestBase {
    private static final String SUCCESS_URI = "https://sample-videos.com/audio/mp3/crowd-cheering.mp3";
    private static final String ERROR_URI = "https://sample-videos.com/audio/mp3/crowd-cheering-not-found.mp3";

    @FormTest
    void playAsyncCompletesAndPlaysMedia() {
        AsyncResource<Media> asyncMedia = new AsyncResource<Media>();
        FakeMedia media = new FakeMedia();
        implementation.setMediaAsync(SUCCESS_URI, asyncMedia);

        AsyncResourceSample sample = new AsyncResourceSample();
        sample.start();

        Button playAsync = findButton(Display.getInstance().getCurrent(), "Play Async");
        assertNotNull(playAsync);

        playAsync.released();
        assertFalse(playAsync.isEnabled());

        implementation.completeMediaAsync(SUCCESS_URI, media);
        flushSerialCalls();

        assertTrue(playAsync.isEnabled());
        assertTrue(media.playInvoked);
    }

    @FormTest
    void playAsyncHandlesErrors() {
        AsyncResource<Media> asyncMedia = new AsyncResource<Media>();
        implementation.setMediaAsync(ERROR_URI, asyncMedia);

        AsyncResourceSample sample = new AsyncResourceSample();
        sample.start();

        Button errorButton = findButton(Display.getInstance().getCurrent(), "Play Async (Not Found)");
        assertNotNull(errorButton);

        errorButton.released();
        assertFalse(errorButton.isEnabled());

        RuntimeException failure = new RuntimeException("Resource missing");
        implementation.failMediaAsync(ERROR_URI, failure);
        flushSerialCalls();

        assertTrue(errorButton.isEnabled());
    }

    private Button findButton(Form form, String text) {
        if (form == null) {
            return null;
        }
        return findButtonRecursive(form.getContentPane(), text);
    }

    private Button findButtonRecursive(Container container, String text) {
        for (Component child : container) {
            if (child instanceof Button) {
                Button button = (Button) child;
                if (text.equals(button.getText())) {
                    return button;
                }
            }
            if (child instanceof Container) {
                Button nested = findButtonRecursive((Container) child, text);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static class FakeMedia implements Media {
        private boolean playInvoked;

        public void play() {
            playInvoked = true;
        }

        public void pause() {
        }

        public void prepare() {
        }

        public void cleanup() {
        }

        public int getTime() {
            return 0;
        }

        public void setTime(int time) {
        }

        public int getDuration() {
            return 0;
        }

        public int getVolume() {
            return 0;
        }

        public void setVolume(int vol) {
        }

        public boolean isPlaying() {
            return playInvoked;
        }

        public Component getVideoComponent() {
            return null;
        }

        public boolean isVideo() {
            return false;
        }

        public boolean isFullScreen() {
            return false;
        }

        public void setFullScreen(boolean fullScreen) {
        }

        public boolean isNativePlayerMode() {
            return false;
        }

        public void setNativePlayerMode(boolean nativePlayerMode) {
        }

        public void setVariable(String key, Object value) {
        }

        public Object getVariable(String key) {
            return null;
        }

        public boolean isTimeSupported() {
            return true;
        }

        public boolean isSeekSupported() {
            return true;
        }
    }
}
