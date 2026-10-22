/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley.ui;

import com.codename1.components.ImageViewer;
import com.codename1.components.InfiniteProgress;
import com.codename1.components.MediaPlayer;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.io.IOException;
import java.util.Map;

/// Opens a received photo, video or voice message. Each is fetched,
/// checked against the digest its message carried, and decrypted on this
/// device; only a video is written out, to a private temporary file the
/// player can read, and that file is deleted when the app next starts.
public final class MediaViewer {
    private static Media playing;

    private MediaViewer() {
    }

    public static void image(final Form previous, Map<String, Object> media) {
        final Form form = Ui.form("Photo", "Photo");
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, new InfiniteProgress());
        form.show();
        com.codenameone.examples.parley.chat.Media.bytes(media, plain -> {
            ImageViewer viewer = new ImageViewer(EncodedImage.create(plain));
            viewer.setName("photo");
            form.removeAll();
            form.add(BorderLayout.CENTER, viewer);
            form.revalidate();
        }, Ui::fail);
    }

    public static void video(final Form previous, Map<String, Object> media) {
        final Form form = Ui.form("Video", "Video");
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, new InfiniteProgress());
        form.show();
        com.codenameone.examples.parley.chat.Media.file(media, path -> {
            try {
                Media video = MediaManager.createMedia(path, true);
                MediaPlayer player = new MediaPlayer(video);
                player.setName("video");
                form.removeAll();
                form.add(BorderLayout.CENTER, player);
                form.revalidate();
                video.play();
            } catch (IOException cannotPlay) {
                Ui.say("This video cannot be played here");
            }
        }, Ui::fail);
    }

    /// Plays a voice message where it is; a second one stops the first.
    public static void voice(Map<String, Object> media) {
        stop();
        com.codenameone.examples.parley.chat.Media.file(media, path -> {
            try {
                playing = MediaManager.createMedia(path, false, MediaViewer::stop);
                playing.play();
            } catch (IOException cannotPlay) {
                Ui.say("This voice message cannot be played here");
            }
        }, Ui::fail);
    }

    private static void stop() {
        if (playing != null) {
            Media was = playing;
            playing = null;
            was.cleanup();
        }
    }
}
