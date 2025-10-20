package com.codename1.media;

import com.codename1.test.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaManagerTest extends UITestBase {
    @BeforeEach
    void configureImplementation() throws Exception {
        when(implementation.getAvailableRecordingMimeTypes()).thenReturn(new String[]{"audio/aac", "audio/amr"});
        when(implementation.createBackgroundMedia(anyString())).thenReturn(mock(Media.class));
        when(implementation.createBackgroundMediaAsync(anyString())).thenReturn(mock(AsyncResource.class));
        when(implementation.createMedia(anyString(), anyBoolean(), nullable(Runnable.class))).thenReturn(mock(Media.class));
        when(implementation.createMediaAsync(anyString(), anyBoolean(), nullable(Runnable.class))).thenReturn(mock(AsyncResource.class));
        when(implementation.createMedia(any(InputStream.class), anyString(), nullable(Runnable.class))).thenReturn(mock(Media.class));
        when(implementation.createMediaAsync(any(InputStream.class), anyString(), nullable(Runnable.class))).thenReturn(mock(AsyncResource.class));
        when(implementation.createMediaRecorder(anyString(), anyString())).thenReturn(mock(Media.class));
        clearAudioBuffers();
        clearRemoteControlListener();
    }

    @AfterEach
    void resetStatics() throws Exception {
        clearAudioBuffers();
        clearRemoteControlListener();
    }

    @Test
    void getAudioBufferManagesReferenceCounts() throws Exception {
        AudioBuffer first = MediaManager.getAudioBuffer("buffer", true, 4);
        AudioBuffer second = MediaManager.getAudioBuffer("buffer", true, 4);
        assertSame(first, second);

        MediaManager.releaseAudioBuffer("buffer");
        assertTrue(getAudioBufferMap().containsKey("buffer"));

        MediaManager.releaseAudioBuffer("buffer");
        assertFalse(getAudioBufferMap().containsKey("buffer"));

        AudioBuffer replacement = MediaManager.getAudioBuffer("buffer", true, 4);
        assertNotSame(first, replacement);
    }

    @Test
    void deleteAudioBufferRemovesEntryImmediately() throws Exception {
        MediaManager.getAudioBuffer("temp", true, 2);
        MediaManager.deleteAudioBuffer("temp");

        assertFalse(getAudioBufferMap().containsKey("temp"));
    }

    @Test
    void setRemoteControlListenerStartsAndStopsService() throws Exception {
        RemoteControlListener listener = new RemoteControlListener() {
            @Override
            public void play() {
            }

            @Override
            public void pause() {
            }

            @Override
            public void stop() {
            }
        };

        MediaManager.setRemoteControlListener(listener);
        MediaManager.setRemoteControlListener(listener);
        MediaManager.setRemoteControlListener(null);

        verify(implementation, times(1)).startRemoteControl();
        verify(implementation, times(1)).stopRemoteControl();
        assertNull(MediaManager.getRemoteControlListener());
    }

    @Test
    void createMediaRecorderValidatesMimeTypes() throws IOException {
        Media expected = mock(Media.class);
        when(implementation.createMediaRecorder("/file", "audio/amr")).thenReturn(expected);
        MediaRecorderBuilder builder = new MediaRecorderBuilder().path("/file").mimeType("audio/amr");

        Media result = MediaManager.createMediaRecorder(builder);

        assertSame(expected, result);
        verify(implementation).createMediaRecorder("/file", "audio/amr");
    }

    @Test
    void createMediaRecorderThrowsForUnsupportedMime() throws IOException {
        MediaRecorderBuilder builder = new MediaRecorderBuilder().path("/file").mimeType("audio/ogg");

        assertThrows(IllegalArgumentException.class, () -> MediaManager.createMediaRecorder(builder));
        verify(implementation, never()).createMediaRecorder(anyString(), anyString());
    }

    @Test
    void createMediaRecorderUsesDefaultMimeWhenNull() throws IOException {
        Media expected = mock(Media.class);
        when(implementation.createMediaRecorder("/file", "audio/aac")).thenReturn(expected);
        MediaRecorderBuilder builder = new MediaRecorderBuilder().path("/file").mimeType(null);

        Media result = MediaManager.createMediaRecorder(builder);

        assertSame(expected, result);
        verify(implementation).createMediaRecorder("/file", "audio/aac");
    }

    @Test
    void createMediaRecorderRedirectsToBuilderWhenRequested() throws IOException {
        final Media expected = mock(Media.class);
        final boolean[] built = new boolean[1];
        MediaRecorderBuilder builder = new MediaRecorderBuilder() {
            @Override
            public Media build() throws IOException {
                built[0] = true;
                return expected;
            }
        }.path("buffer").redirectToAudioBuffer(true);

        Media result = MediaManager.createMediaRecorder(builder);

        assertTrue(built[0]);
        assertSame(expected, result);
        verify(implementation, never()).createMediaRecorder(anyString(), anyString());
    }

    @Test
    void getAsyncMediaReturnsSameInstanceForAsyncMedia() {
        StubAsyncMedia async = new StubAsyncMedia();
        assertSame(async, MediaManager.getAsyncMedia(async));
    }

    @Test
    void getAsyncMediaWrapsSynchronousMedia() {
        FakeMedia media = new FakeMedia();
        media.setDuration(2000);
        AsyncMedia async = MediaManager.getAsyncMedia(media);
        List<AsyncMedia.State> events = new ArrayList<AsyncMedia.State>();
        async.addMediaStateChangeListener(evt -> events.add(evt.getNewState()));

        async.prepare();
        async.play();
        assertTrue(media.prepareCalled);
        assertTrue(media.playing);
        assertTrue(async.isPlaying());
        assertEquals(AsyncMedia.State.Playing, events.get(0));

        async.setTime(1500);
        assertEquals(1500, media.currentTime);
        assertEquals(2000, async.getDuration());

        async.setVolume(7);
        assertEquals(7, media.volume);

        async.setFullScreen(true);
        assertTrue(media.fullScreen);
        async.setNativePlayerMode(true);
        assertTrue(media.nativeMode);

        async.setVariable("key", "value");
        assertEquals("value", async.getVariable("key"));

        async.pause();
        assertFalse(media.playing);
        assertEquals(AsyncMedia.State.Paused, events.get(events.size() - 1));

        async.cleanup();
        assertTrue(media.cleanedUp);
        assertEquals(media.component, async.getVideoComponent());
        assertTrue(async.isVideo());
    }

    @Test
    void mediaCreationMethodsDelegateToDisplay() throws IOException {
        MediaManager.createBackgroundMedia("bg");
        MediaManager.createBackgroundMediaAsync("bg");
        MediaManager.createMedia("uri", true);
        MediaManager.createMedia("uri", true, null);
        try {
            MediaManager.createMedia(new ByteArrayInputStream(new byte[]{1}), "audio/mp3");
            MediaManager.createMedia(new ByteArrayInputStream(new byte[]{1}), "audio/mp3", null);
        } catch (IOException e) {
            fail(e);
        }
        MediaManager.createMediaAsync("uri", false, null);
        MediaManager.createMediaAsync(new ByteArrayInputStream(new byte[]{1}), "audio/wav", null);

        verify(implementation).createBackgroundMedia("bg");
        verify(implementation).createBackgroundMediaAsync("bg");
        verify(implementation, times(2)).createMedia(anyString(), anyBoolean(), nullable(Runnable.class));
        verify(implementation, times(2)).createMedia(any(InputStream.class), anyString(), nullable(Runnable.class));
        verify(implementation).createMediaAsync(anyString(), anyBoolean(), nullable(Runnable.class));
        verify(implementation).createMediaAsync(any(InputStream.class), anyString(), nullable(Runnable.class));
    }

    @Test
    void completionHandlersDelegateToDisplay() {
        Media media = mock(Media.class);
        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
            }
        };

        MediaManager.addCompletionHandler(media, onComplete);
        MediaManager.removeCompletionHandler(media, onComplete);

        verify(implementation).addCompletionHandler(media, onComplete);
        verify(implementation).removeCompletionHandler(media, onComplete);
    }

    private Map<String, AudioBuffer> getAudioBufferMap() throws Exception {
        Field field = MediaManager.class.getDeclaredField("audioBuffers");
        field.setAccessible(true);
        return (Map<String, AudioBuffer>) field.get(null);
    }

    private void clearAudioBuffers() throws Exception {
        getAudioBufferMap().clear();
    }

    private void clearRemoteControlListener() throws Exception {
        Field field = MediaManager.class.getDeclaredField("remoteControlListener");
        field.setAccessible(true);
        field.set(null, null);
    }

    private static class StubAsyncMedia extends AbstractMedia {
        @Override
        protected void playImpl() {
        }

        @Override
        protected void pauseImpl() {
        }

        @Override
        public void prepare() {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public int getTime() {
            return 0;
        }

        @Override
        public void setTime(int time) {
        }

        @Override
        public int getDuration() {
            return 0;
        }

        @Override
        public int getVolume() {
            return 0;
        }

        @Override
        public void setVolume(int vol) {
        }

        @Override
        public boolean isPlaying() {
            return false;
        }

        @Override
        public com.codename1.ui.Component getVideoComponent() {
            return null;
        }

        @Override
        public boolean isVideo() {
            return false;
        }

        @Override
        public boolean isFullScreen() {
            return false;
        }

        @Override
        public void setFullScreen(boolean fullScreen) {
        }

        @Override
        public boolean isNativePlayerMode() {
            return false;
        }

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {
        }

        @Override
        public void setVariable(String key, Object value) {
        }

        @Override
        public Object getVariable(String key) {
            return null;
        }
    }

    private static class FakeMedia implements Media {
        boolean playing;
        boolean prepareCalled;
        boolean cleanedUp;
        boolean video = true;
        boolean fullScreen;
        boolean nativeMode;
        int currentTime;
        int duration;
        int volume;
        final com.codename1.ui.Component component = mock(com.codename1.ui.Component.class);
        final Map<String, Object> variables = new HashMap<String, Object>();

        @Override
        public void play() {
            playing = true;
        }

        @Override
        public void pause() {
            playing = false;
        }

        @Override
        public void prepare() {
            prepareCalled = true;
        }

        @Override
        public void cleanup() {
            cleanedUp = true;
        }

        @Override
        public int getTime() {
            return currentTime;
        }

        @Override
        public void setTime(int time) {
            currentTime = time;
        }

        @Override
        public int getDuration() {
            return duration;
        }

        void setDuration(int duration) {
            this.duration = duration;
        }

        @Override
        public int getVolume() {
            return volume;
        }

        @Override
        public void setVolume(int vol) {
            volume = vol;
        }

        @Override
        public boolean isPlaying() {
            return playing;
        }

        @Override
        public com.codename1.ui.Component getVideoComponent() {
            return component;
        }

        @Override
        public boolean isVideo() {
            return video;
        }

        @Override
        public boolean isFullScreen() {
            return fullScreen;
        }

        @Override
        public void setFullScreen(boolean fullScreen) {
            this.fullScreen = fullScreen;
        }

        @Override
        public boolean isNativePlayerMode() {
            return nativeMode;
        }

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {
            nativeMode = nativePlayer;
        }

        @Override
        public void setVariable(String key, Object value) {
            variables.put(key, value);
        }

        @Override
        public Object getVariable(String key) {
            return variables.get(key);
        }
    }
}
