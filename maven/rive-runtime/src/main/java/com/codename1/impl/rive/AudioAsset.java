/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import com.codename1.media.Media;
import com.codename1.media.MediaManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/// Audio embedded in a Rive file. Each play is a new player, so sounds may
/// overlap the way they do in Rive's audio engine; a player releases itself
/// when it completes.
public class AudioAsset extends AudioAssetBase {
    /// Starts playing the asset's bytes; does nothing when there are none or
    /// the platform cannot play them.
    public void play() {
        byte[] bytes = getContents();
        if (bytes == null || bytes.length < 4 || !com.codename1.ui.Display.isInitialized()) {
            // No Display (a headless test, a server render): nothing can play.
            return;
        }
        Cleanup done = new Cleanup();
        try {
            Media m = MediaManager.createMedia(new ByteArrayInputStream(bytes), mimeType(bytes), done);
            if (m != null) {
                done.media = m;
                m.play();
            }
        } catch (IOException ex) {
            com.codename1.io.Log.e(ex);
        }
    }

    /// The container of an exported Rive sound, recognized by its magic bytes.
    static String mimeType(byte[] b) {
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F') {
            return "audio/wav";
        }
        if (b[0] == 'O' && b[1] == 'g' && b[2] == 'g' && b[3] == 'S') {
            return "audio/ogg";
        }
        if (b[0] == 'f' && b[1] == 'L' && b[2] == 'a' && b[3] == 'C') {
            return "audio/flac";
        }
        return "audio/mpeg";
    }

    /// Releases the player once the clip finishes.
    private static final class Cleanup implements Runnable {
        private Media media;

        @Override
        public void run() {
            if (media != null) {
                media.cleanup();
            }
        }
    }
}
