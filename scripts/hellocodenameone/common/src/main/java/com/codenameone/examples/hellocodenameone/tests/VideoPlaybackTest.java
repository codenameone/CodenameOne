package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.layouts.BorderLayout;

public class VideoPlaybackTest extends BaseTest {
    // A small, public domain video sample (HTTPS to avoid cleartext issues)
    private static final String VIDEO_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Small.mp4";

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

        // Use createMediaAsync to avoid blocking the EDT on network access.
        try {
            MediaManager.createMediaAsync(VIDEO_URL, true, () -> {
                // On completion of playback
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
            }).except(err -> {
                 status.setText("Error creating media: " + err.getMessage());
                 form.revalidate();
            });
        } catch (Exception e) {
            status.setText("Error starting media creation: " + e.getMessage());
            e.printStackTrace();
        }

        // Safety timeout: Ensure the test finishes even if callbacks are never invoked (e.g. network hang)
        // We allow plenty of time (10s) for buffering, but ensure we eventually call done().
        UITimer.timer(10000, false, form, () -> {
            if (!isDone()) {
                if (status.getText().equals("Initializing video...")) {
                     status.setText("Video initialization timed out.");
                     form.revalidate();
                }
                done();
            }
        });

        return true;
    }
}
