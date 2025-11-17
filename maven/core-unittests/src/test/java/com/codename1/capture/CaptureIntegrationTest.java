package com.codename1.capture;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CaptureIntegrationTest extends UITestBase {

    @FormTest
    void blockingCaptureMethodsUseImplementationCallbacks() {
        TestCodenameOneImplementation impl = implementation;
        impl.setNextCapturePhotoPath("file://sync-photo.jpg");
        impl.setNextCaptureAudioPath("file://sync-audio.wav");
        impl.setNextCaptureVideoPath("file://sync-video.mp4");

        assertEquals("file://sync-photo.jpg", Capture.capturePhoto());
        assertEquals("file://sync-audio.wav", Capture.captureAudio());
        assertEquals("file://sync-video.mp4", Capture.captureVideo());
    }

    @FormTest
    void asynchronousCaptureInvokesCallbacksWithConstraints() {
        final AtomicReference<String> photo = new AtomicReference<String>();
        final AtomicReference<String> video = new AtomicReference<String>();
        implementation.setNextCapturePhotoPath("file://async-photo.jpg");
        implementation.setNextCaptureVideoPath("file://async-video.mp4");
        VideoCaptureConstraints constraints = new VideoCaptureConstraints();

        Capture.capturePhoto(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                photo.set((String) evt.getSource());
            }
        });
        Capture.captureVideo(constraints, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                video.set((String) evt.getSource());
            }
        });

        assertEquals("file://async-photo.jpg", photo.get());
        assertEquals("file://async-video.mp4", video.get());
        assertEquals(constraints, implementation.getLastVideoConstraints());
    }
}
