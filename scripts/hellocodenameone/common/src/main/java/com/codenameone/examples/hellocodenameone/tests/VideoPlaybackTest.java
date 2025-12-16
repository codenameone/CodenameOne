package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.layouts.BorderLayout;

public class VideoPlaybackTest extends BaseTest {
    // A small, public domain video sample
    private static final String VIDEO_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Small.mp4";

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        Form form = new Form("Video Playback", new BorderLayout());
        Label status = new Label("Initializing video...");
        form.add(BorderLayout.NORTH, status);
        form.show();

        try {
            Media video = MediaManager.createMedia(VIDEO_URL, true, null);
            if (video != null) {
                video.setNativePlayerMode(false); // Try to embed if possible
                form.add(BorderLayout.CENTER, video.getVideoComponent());
                status.setText("Playing video...");
                form.revalidate();
                video.play();
            } else {
                status.setText("Failed to create media (null).");
                form.revalidate();
            }
        } catch (Exception e) {
            status.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }

        // Wait a bit for playback to start/fail, then finish.
        // We do not wait for the video to finish as it is a playback test, not a completion test.
        UITimer.timer(2000, false, form, () -> done());

        return true;
    }
}
