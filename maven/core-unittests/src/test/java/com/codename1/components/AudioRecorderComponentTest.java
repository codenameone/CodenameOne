package com.codename1.components;

import com.codename1.media.Media;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.Button;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudioRecorderComponentTest extends ComponentTestBase {

    private Media media;

    @BeforeEach
    void prepareMediaMocks() {
        when(implementation.getAvailableRecordingMimeTypes()).thenReturn(new String[]{"audio/wav"});
        media = mock(Media.class);
        when(implementation.createMediaRecorder(any(MediaRecorderBuilder.class))).thenReturn(media);
    }

    private AudioRecorderComponent createRecorder(boolean redirect) {
        MediaRecorderBuilder builder = new MediaRecorderBuilder();
        try {
            builder.path("/tmp/record.m4a");
        } catch (java.io.IOException e) {
            fail("Unexpected IOException configuring MediaRecorderBuilder: " + e.getMessage());
        }
        builder.redirectToAudioBuffer(redirect);
        AudioRecorderComponent component = new AudioRecorderComponent(builder);
        flushSerialCalls();
        return component;
    }

    @Test
    void initializationQueuesAndAppliesPausedState() {
        AudioRecorderComponent recorder = createRecorder(false);
        assertEquals(AudioRecorderComponent.RecorderState.Paused, recorder.getState());
        assertTrue(recorder.getComponentCount() > 0, "Recorder UI should be constructed after initialization");
    }

    @Test
    void recordAndPauseActionsUpdateMediaState() throws Exception {
        AudioRecorderComponent recorder = createRecorder(false);
        Button recordButton = getPrivateButton(recorder, "record");
        fireButtonAction(recordButton);
        assertEquals(AudioRecorderComponent.RecorderState.Recording, recorder.getState());
        verify(media).play();

        Button pauseButton = getPrivateButton(recorder, "pause");
        fireButtonAction(pauseButton);
        assertEquals(AudioRecorderComponent.RecorderState.Paused, recorder.getState());
        verify(media).pause();
    }

    @Test
    void doneActionRedirectAcceptsRecordingAndNotifiesListeners() throws Exception {
        AudioRecorderComponent recorder = createRecorder(true);
        Button recordButton = getPrivateButton(recorder, "record");
        fireButtonAction(recordButton);

        AtomicInteger eventCount = new AtomicInteger();
        recorder.addActionListener(evt -> eventCount.incrementAndGet());

        Button doneButton = getPrivateButton(recorder, "done");
        fireButtonAction(doneButton);

        verify(media).cleanup();
        assertEquals(AudioRecorderComponent.RecorderState.Accepted, recorder.getState());
        assertEquals(2, eventCount.get(), "Expected paused and accepted events");
    }

    @Test
    void animateUpdatesRecordingTime() throws Exception {
        AudioRecorderComponent recorder = createRecorder(false);
        setPrivateField(recorder, "state", AudioRecorderComponent.RecorderState.Recording);
        setPrivateField(recorder, "recordingLength", 61005L);
        setPrivateField(recorder, "lastRecordingStartTime", 0L);
        Label recordingTime = getPrivateField(recorder, "recordingTime", Label.class);
        boolean animating = recorder.animate();
        assertTrue(animating);
        assertEquals("01:01.5", recordingTime.getText());
    }

    private Button getPrivateButton(AudioRecorderComponent recorder, String fieldName) throws Exception {
        return getPrivateField(recorder, fieldName, Button.class);
    }

    @SuppressWarnings("unchecked")
    private void fireButtonAction(Button button) {
        Collection<ActionListener> listeners = button.getListeners();
        for (ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(button));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private void setPrivateField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
