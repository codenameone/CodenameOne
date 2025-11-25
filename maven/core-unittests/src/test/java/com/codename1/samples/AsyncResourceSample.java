package com.codename1.samples;

import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.media.MediaManager;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Test-local copy of the AsyncResource sample used by {@link AsyncResourceSampleTest}.
 */
public class AsyncResourceSample {
    private static final String SUCCESS_URI = "https://sample-videos.com/audio/mp3/crowd-cheering.mp3";
    private static final String ERROR_URI = "https://sample-videos.com/audio/mp3/crowd-cheering-not-found.mp3";

    private Form current;

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        final Form hi = new Form("Hi World", BoxLayout.y());
        hi.add(new Label("Hi World"));
        final Button playAsync = new Button("Play Async");
        playAsync.addActionListener(e -> handleAsyncMedia(playAsync, SUCCESS_URI));
        hi.add(playAsync);

        final Button playAsyncErr = new Button("Play Async (Not Found)");
        playAsyncErr.addActionListener(e -> handleAsyncMedia(playAsyncErr, ERROR_URI));
        hi.add(playAsyncErr);
        hi.show();
    }

    private void handleAsyncMedia(final Button source, String uri) {
        source.setEnabled(false);
        final ToastBar.Status status = ToastBar.getInstance().createStatus();
        status.setMessage("Loading Audio...");
        status.setShowProgressIndicator(true);
        status.show();
        source.repaint();
        MediaManager.createMediaAsync(uri, false, null)
                .ready(media -> {
                    status.clear();
                    source.setEnabled(true);
                    source.repaint();
                    media.play();
                })
                .except(ex -> {
                    status.clear();
                    source.setEnabled(true);
                    source.repaint();
                    Log.e(ex);
                    ToastBar.showErrorMessage(ex.getMessage());
                });
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
        if (current instanceof Dialog) {
            ((Dialog) current).dispose();
            current = Display.getInstance().getCurrent();
        }
    }

    public void destroy() {
    }
}
