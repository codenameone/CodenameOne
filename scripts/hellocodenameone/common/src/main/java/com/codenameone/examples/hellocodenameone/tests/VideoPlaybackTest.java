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

        // Use createMediaAsync to avoid blocking the EDT (which causes timeouts)
        try {
            MediaManager.createMediaAsync(VIDEO_URL, true, () -> {
                // On completion of playback (optional, we just want to start it)
            }).ready(media -> {
                if (media != null) {
                    media.setNativePlayerMode(false); // Try to embed if possible
                    form.add(BorderLayout.CENTER, media.getVideoComponent());
                    status.setText("Playing video...");
                    form.revalidate();
                    media.play();
                } else {
                    status.setText("Failed to create media (null).");
                    form.revalidate();
                }
                // Wait a bit for playback to start, then finish.
                UITimer.timer(2000, false, form, () -> done());
            }).except(err -> {
                 status.setText("Error creating media: " + err.getMessage());
                 form.revalidate();
                 done();
            });
        } catch (Exception e) {
            status.setText("Error: " + e.getMessage());
            e.printStackTrace();
            done();
        }

        return true;
    }
}
