package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

public class VideoPlaybackTest extends BaseTest {
    // A small, public domain video sample
    private static final String VIDEO_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Small.mp4";

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Video Playback", new BorderLayout(), "VideoPlayback");
        Label status = new Label("Initializing video...");
        form.add(BorderLayout.NORTH, status);
        form.show();

        try {
            Media video = MediaManager.createMedia(VIDEO_URL, true, () -> {
                // Completion callback
            });
            if (video != null) {
                video.setNativePlayerMode(false); // Try to embed if possible
                form.add(BorderLayout.CENTER, video.getVideoComponent());
                status.setText("Playing video...");
                video.play();
            } else {
                status.setText("Failed to create media (null).");
            }
        } catch (Exception e) {
            status.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
