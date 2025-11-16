package com.codename1.capture;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.capture.VideoCaptureConstraints;

import static org.junit.jupiter.api.Assertions.*;

class CapturePackageTest extends UITestBase {

    @FormTest
    void captureSynchronousMethodsReturnConfiguredPaths() {
        implementation.setNextCapturePhotoPath("file://photo.jpg");
        assertEquals("file://photo.jpg", Capture.capturePhoto());

        implementation.setNextCaptureAudioPath("file://audio.wav");
        assertEquals("file://audio.wav", Capture.captureAudio());

        implementation.setNextCaptureVideoPath("file://video.mp4");
        assertEquals("file://video.mp4", Capture.captureVideo());

        VideoCaptureConstraints constraints = new VideoCaptureConstraints();
        Capture.captureVideo(constraints);
        assertSame(constraints, implementation.getLastVideoConstraints());

        MediaRecorderBuilder builder = new MediaRecorderBuilder();
        Capture.captureAudio(builder);
        assertSame(builder, implementation.getLastMediaRecorderBuilder());
    }

    @FormTest
    void asynchronousCaptureInvokesListenersImmediately() {
        implementation.setNextCapturePhotoPath("file://async-photo.jpg");
        RecordingListener listener = new RecordingListener();
        Capture.capturePhoto(listener);
        assertEquals("file://async-photo.jpg", listener.lastPath);

        implementation.setNextCaptureAudioPath("file://async-audio.wav");
        Capture.captureAudio(listener);
        assertEquals("file://async-audio.wav", listener.lastPath);

        implementation.setNextCaptureVideoPath("file://async-video.mp4");
        Capture.captureVideo(listener);
        assertEquals("file://async-video.mp4", listener.lastPath);
    }

    private static class RecordingListener implements ActionListener {
        private String lastPath;

        public void actionPerformed(ActionEvent evt) {
            lastPath = (String) evt.getSource();
        }
    }
}
