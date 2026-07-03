/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.media;

import com.codename1.ui.Image;

import java.io.IOException;

/// Encodes application supplied frames and audio into a video file using the platform's
/// native codecs. Obtain a `VideoWriter` from a `VideoWriterBuilder`, then push video
/// frames (each as an `Image` you fully control, with an explicit presentation
/// timestamp) and, optionally, interleaved PCM audio. Call `#close()` to finalize and
/// mux the file.
///
/// This is the encode counterpart of `VideoReader`. Because every frame is just an
/// `Image`, you have complete control over the pixels: render charts, overlays,
/// transformed camera frames, generated animation, etc. into the image's `Graphics`
/// before handing it to the writer.
///
/// Example:
///
/// ```java
/// VideoWriter w = new VideoWriterBuilder().path(out).width(640).height(480).frameRate(30).build();
/// for (int i = 0; i < 90; i++) {
///     Image frame = Image.createImage(640, 480, 0xff000000);
///     Graphics g = frame.getGraphics();
///     g.setColor(0xffffff);
///     g.fillRect(i * 6 % 640, 0, 40, 480);
///     w.writeFrame(frame, Math.round(i * 1000f / 30f));
/// }
/// w.close();
/// ```
public abstract class VideoWriter {
    /// Writes a single video frame at the given presentation timestamp. The frame may be
    /// any `Image`; its pixels are read via `Image#getRGB()`. Frames should be supplied
    /// in non decreasing timestamp order.
    ///
    /// #### Parameters
    ///
    /// - `frame`: the frame to encode, expected to match the configured width and height
    ///
    /// - `presentationTimeMillis`: the timestamp of this frame in milliseconds
    ///
    /// #### Throws
    ///
    /// - `IOException`: if encoding fails
    public void writeFrame(Image frame, long presentationTimeMillis) throws IOException {
        writeFrame(frame.getRGB(), frame.getWidth(), frame.getHeight(), presentationTimeMillis);
    }

    /// Writes a single video frame supplied as a raw ARGB pixel array. This is the method
    /// platform implementations provide; `#writeFrame(Image, long)` funnels into it.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the frame pixels in ARGB order, length `width * height`
    ///
    /// - `width`: the frame width in pixels
    ///
    /// - `height`: the frame height in pixels
    ///
    /// - `presentationTimeMillis`: the timestamp of this frame in milliseconds
    ///
    /// #### Throws
    ///
    /// - `IOException`: if encoding fails
    public abstract void writeFrame(int[] argb, int width, int height, long presentationTimeMillis) throws IOException;

    /// Writes a block of interleaved PCM audio samples at the given timestamp. Samples
    /// are signed 16 bit, interleaved by channel. The audio track must have been enabled
    /// with `VideoWriterBuilder#hasAudio(boolean)`.
    ///
    /// #### Parameters
    ///
    /// - `interleavedPcm`: signed 16 bit interleaved samples
    ///
    /// - `sampleRate`: the sample rate of the supplied data in Hz
    ///
    /// - `channels`: the number of interleaved channels
    ///
    /// - `presentationTimeMillis`: the timestamp of the first sample in milliseconds
    ///
    /// #### Throws
    ///
    /// - `IOException`: if encoding fails
    public abstract void writeAudio(short[] interleavedPcm, int sampleRate, int channels, long presentationTimeMillis) throws IOException;

    /// Convenience method that writes the current contents of an `AudioBuffer` (which
    /// stores floating point samples) as 16 bit PCM. The buffer's own sample rate and
    /// channel count are used.
    ///
    /// #### Parameters
    ///
    /// - `buffer`: the audio buffer to write
    ///
    /// - `presentationTimeMillis`: the timestamp of the first sample in milliseconds
    ///
    /// #### Throws
    ///
    /// - `IOException`: if encoding fails
    public void writeAudio(AudioBuffer buffer, long presentationTimeMillis) throws IOException {
        int size = buffer.getSize();
        float[] f = new float[size];
        buffer.copyTo(f);
        short[] s = new short[size];
        for (int i = 0; i < size; i++) {
            float v = f[i];
            if (v > 1f) {
                v = 1f;
            } else if (v < -1f) {
                v = -1f;
            }
            s[i] = (short) Math.round(v * 32767f);
        }
        writeAudio(s, buffer.getSampleRate(), buffer.getNumChannels(), presentationTimeMillis);
    }

    /// Finalizes the file: flushes any pending frames, writes the container index and
    /// releases native resources. The writer cannot be used after this call.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if finalization fails
    public abstract void close() throws IOException;

    /// The configured frame width in pixels.
    public abstract int getWidth();

    /// The configured frame height in pixels.
    public abstract int getHeight();

    /// The configured nominal frame rate.
    public abstract float getFrameRate();
}
