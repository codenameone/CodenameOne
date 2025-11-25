package com.codename1.samples;

import com.codename1.components.AudioRecorderComponent;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.media.Media;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Sheet;
import com.codename1.io.FileSystemStorage;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AudioRecorderComponentSampleTest extends UITestBase {

    @FormTest
    void acceptingRecordingCompletesResourceAndPlaysMedia() throws Exception {
        implementation.setAvailableRecordingMimeTypes(new String[]{"audio/wav"});
        FakeMedia recorder = new FakeMedia();
        FakeMedia playback = new FakeMedia();
        implementation.setMediaRecorder(recorder);
        implementation.setMedia(playback);

        AudioRecorderComponentSample sample = new AudioRecorderComponentSample();
        AsyncResource<String> recording = sample.recordAudio();
        ResultHolder<String> result = new ResultHolder<String>();
        recording.onResult(new AsyncResult<String>() {
            public void onReady(String value, Throwable error) {
                result.value.set(value);
                result.error.set(error);
                result.completed.set(true);
            }
        });

        flushSerialCalls();
        AudioRecorderComponent recorderComponent = findRecorderComponent();
        assertNotNull(recorderComponent);
        assertEquals(AudioRecorderComponent.RecorderState.Paused, recorderComponent.getState());

        Button recordButton = findVisibleButton(recorderComponent);
        assertNotNull(recordButton);
        implementation.tapComponent(recordButton);
        flushSerialCalls();

        assertTrue(recorder.playInvoked);
        assertEquals(AudioRecorderComponent.RecorderState.Recording, recorderComponent.getState());

        Button doneButton = findButtonWithText(Display.getInstance().getCurrent(), "Done");
        assertNotNull(doneButton);
        implementation.tapComponent(doneButton);
        flushSerialCalls();

        Button acceptButton = findButtonWithText(Display.getInstance().getCurrent(), "Accept");
        assertNotNull(acceptButton);
        implementation.tapComponent(acceptButton);
        flushSerialCalls();

        assertTrue(result.completed.get());
        assertNull(result.error.get());
        assertNotNull(result.value.get());
        assertTrue(recorder.cleanupInvoked);
        assertTrue(playback.playInvoked);
    }

    @FormTest
    void closingSheetWithoutAcceptingCancelsAndDeletesRecording() throws Exception {
        implementation.setAvailableRecordingMimeTypes(new String[]{"audio/wav"});
        FakeMedia recorder = new FakeMedia();
        implementation.setMediaRecorder(recorder);

        AudioRecorderComponentSample sample = new AudioRecorderComponentSample();
        AsyncResource<String> recording = sample.recordAudio();
        ResultHolder<String> result = new ResultHolder<String>();
        recording.onResult(new AsyncResult<String>() {
            public void onReady(String value, Throwable error) {
                result.value.set(value);
                result.error.set(error);
                result.completed.set(true);
            }
        });

        flushSerialCalls();
        String path = sample.getLastRecordingPath();
        createPlaceholderFile(path);

        Sheet sheet = Sheet.getCurrentSheet();
        assertNotNull(sheet);
        sheet.back();
        flushSerialCalls();

        assertTrue(result.completed.get());
        assertNull(result.error.get());
        assertNull(result.value.get());
        assertFalse(FileSystemStorage.getInstance().exists(path));
    }

    private AudioRecorderComponent findRecorderComponent() {
        Form current = Display.getInstance().getCurrent();
        if (current == null) {
            return null;
        }
        return findComponent(current.getContentPane(), AudioRecorderComponent.class);
    }

    private Button findVisibleButton(Container root) {
        for (Component child : root) {
            if (child instanceof Button) {
                Button button = (Button) child;
                String text = button.getText();
                if ((text == null || text.length() == 0) && button.isVisible() && button.isEnabled()) {
                    return button;
                }
            }
            if (child instanceof Container) {
                Button nested = findVisibleButton((Container) child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private Button findButtonWithText(Form root, String text) {
        if (root == null) {
            return null;
        }
        return findButtonWithText(root.getContentPane(), text);
    }

    private Button findButtonWithText(Container container, String text) {
        for (Component child : container) {
            if (child instanceof Button) {
                Button button = (Button) child;
                if (text.equals(button.getText())) {
                    return button;
                }
            }
            if (child instanceof Container) {
                Button nested = findButtonWithText((Container) child, text);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private <T extends Component> T findComponent(Container root, Class<T> type) {
        for (Component child : root) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container) {
                T nested = findComponent((Container) child, type);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private void createPlaceholderFile(String path) throws IOException {
        OutputStream os = FileSystemStorage.getInstance().openOutputStream(path);
        try {
            os.write("recording".getBytes("UTF-8"));
        } finally {
            os.close();
        }
    }

    private static class ResultHolder<T> {
        private final AtomicReference<T> value = new AtomicReference<T>();
        private final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        private final AtomicBoolean completed = new AtomicBoolean();
    }

    private static class FakeMedia implements Media {
        private boolean playInvoked;
        private boolean pauseInvoked;
        private boolean cleanupInvoked;

        public void play() {
            playInvoked = true;
        }

        public void pause() {
            pauseInvoked = true;
        }

        public void prepare() {
        }

        public void cleanup() {
            cleanupInvoked = true;
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
