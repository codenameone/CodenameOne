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

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Low level video encoding and decoding using the platform's native codecs. `VideoIO`
/// is to video what `com.codename1.ui.util.ImageIO` is to still images, but goes deeper:
/// it can enumerate the codecs the device actually supports, encode application rendered
/// frames and audio into a standard container (`VideoWriter`), and decode an existing
/// clip into frame accurate RGBA frames and PCM audio (`VideoReader`).
///
/// It is supported on iOS, macOS, Android, Windows, Linux, JavaScript and the desktop
/// simulator. It is not available on the TV, Watch or Car targets. As with other
/// optional platform features, always gate usage with `#isSupported()`:
///
/// ```java
/// if (VideoIO.isSupported()) {
///     VideoIO io = VideoIO.getVideoIO();
///     // ... encode / decode
/// }
/// ```
///
/// ### Encoding
///
/// Configure a `VideoWriterBuilder`, build a `VideoWriter`, push frames and audio, then
/// close it. Every frame is an ordinary `com.codename1.ui.Image`, so the application has
/// full control over the pixels.
///
/// ### Decoding
///
/// `#openReader(String)` returns a `VideoReader` exposing duration, dimensions and frame
/// rate, frame accurate `VideoReader#frameAt(long)`, a variable to constant frame rate
/// resampler `VideoReader#readFrames(float, com.codename1.media.VideoReader.FrameCallback)`
/// and the audio track as PCM via `VideoReader#readAudio()`.
///
/// @author Shai Almog
///
/// #### Since
///
/// 8.0
public abstract class VideoIO {
    /// Codec id for H.264 / AVC video, the most broadly compatible video codec.
    public static final String CODEC_H264 = "h264";
    /// Codec id for H.265 / HEVC video.
    public static final String CODEC_HEVC = "hevc";
    /// Codec id for VP8 video.
    public static final String CODEC_VP8 = "vp8";
    /// Codec id for VP9 video.
    public static final String CODEC_VP9 = "vp9";
    /// Codec id for AV1 video.
    public static final String CODEC_AV1 = "av1";
    /// Codec id for AAC audio, the most broadly compatible audio codec.
    public static final String CODEC_AAC = "aac";
    /// Codec id for Opus audio.
    public static final String CODEC_OPUS = "opus";
    /// Codec id for uncompressed 16 bit PCM audio.
    public static final String CODEC_PCM = "pcm";

    /// Container id for the MPEG 4 (.mp4) container.
    public static final String CONTAINER_MP4 = "mp4";
    /// Container id for the WebM (.webm) container.
    public static final String CONTAINER_WEBM = "webm";
    /// Container id for the QuickTime (.mov) container.
    public static final String CONTAINER_MOV = "mov";
    /// Container id for the Matroska (.mkv) container.
    public static final String CONTAINER_MKV = "mkv";

    /// Returns the `VideoIO` instance for the current platform, or null when video
    /// encoding/decoding is not supported. Prefer `#isSupported()` for a simple boolean
    /// check.
    ///
    /// #### Returns
    ///
    /// the platform `VideoIO`, or null if unsupported
    public static VideoIO getVideoIO() {
        return Display.getInstance().getVideoIO();
    }

    /// Convenience test for whether this platform supports the `VideoIO` API.
    ///
    /// #### Returns
    ///
    /// true if video encoding/decoding is available
    public static boolean isSupported() {
        return Display.getInstance().getVideoIO() != null;
    }

    /// Lists the codecs that can be used for encoding on this device. The set is device
    /// dependent and may include hardware accelerated entries (see
    /// `VideoCodec#isHardwareAccelerated()`).
    ///
    /// #### Returns
    ///
    /// the available encoder codecs (never null, possibly empty)
    public abstract VideoCodec[] getAvailableEncoders();

    /// Lists the codecs that can be used for decoding on this device.
    ///
    /// #### Returns
    ///
    /// the available decoder codecs (never null, possibly empty)
    public abstract VideoCodec[] getAvailableDecoders();

    /// Tests whether a given codec id can be used for encoding on this device.
    ///
    /// #### Parameters
    ///
    /// - `codecId`: one of the `#CODEC_H264` style constants
    ///
    /// #### Returns
    ///
    /// true if an encoder for the codec exists
    public boolean isEncoderSupported(String codecId) {
        return containsCodec(getAvailableEncoders(), codecId);
    }

    /// Tests whether a given codec id can be used for decoding on this device.
    ///
    /// #### Parameters
    ///
    /// - `codecId`: one of the `#CODEC_H264` style constants
    ///
    /// #### Returns
    ///
    /// true if a decoder for the codec exists
    public boolean isDecoderSupported(String codecId) {
        return containsCodec(getAvailableDecoders(), codecId);
    }

    private static boolean containsCodec(VideoCodec[] codecs, String codecId) {
        if (codecs == null || codecId == null) {
            return false;
        }
        for (VideoCodec c : codecs) {
            if (codecId.equals(c.getId())) {
                return true;
            }
        }
        return false;
    }

    /// Creates a `VideoWriter` for the supplied configuration. Application code normally
    /// uses `VideoWriterBuilder#build()` rather than calling this directly.
    ///
    /// #### Parameters
    ///
    /// - `cfg`: the encoder configuration
    ///
    /// #### Returns
    ///
    /// a ready to use writer
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the writer cannot be created
    public abstract VideoWriter createWriter(VideoWriterBuilder cfg) throws IOException;

    /// Opens a `VideoReader` for the clip at the supplied
    /// `com.codename1.io.FileSystemStorage` path.
    ///
    /// #### Parameters
    ///
    /// - `filePath`: the FileSystemStorage path of the clip to decode
    ///
    /// #### Returns
    ///
    /// a reader positioned at the start of the clip
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the clip cannot be opened
    public abstract VideoReader openReader(String filePath) throws IOException;

    /// Opens a `VideoReader` for a clip supplied as a stream. The default implementation
    /// spools the stream to a temporary file and opens that; platforms that can decode
    /// directly from a stream may override it. The stream is fully consumed and closed.
    ///
    /// #### Parameters
    ///
    /// - `stream`: the clip data
    ///
    /// - `mimeType`: the clip mime type (e.g. "video/mp4"), used to pick a file suffix; may be null
    ///
    /// #### Returns
    ///
    /// a reader for the clip
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the clip cannot be opened
    public VideoReader openReader(InputStream stream, String mimeType) throws IOException {
        String suffix = ".mp4";
        if (mimeType != null) {
            int slash = mimeType.indexOf('/');
            if (slash > -1 && slash < mimeType.length() - 1) {
                suffix = "." + mimeType.substring(slash + 1);
            }
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath() + "/cn1-videoio-" + System.currentTimeMillis() + suffix;
        OutputStream os = null; //NOPMD CloseResource
        try {
            os = fs.openOutputStream(path);
            Util.copy(stream, os);
        } finally {
            Util.cleanup(os);
            // Util.copy does not close its input; the contract says the stream is
            // fully consumed and closed, so close it here.
            Util.cleanup(stream);
        }
        // Wrap so closing the reader also deletes the spool file we created.
        return new SpooledVideoReader(openReader(path), path);
    }

    /// A `VideoReader` opened from a temporary spool file (see
    /// {@link #openReader(InputStream, String)}) that deletes the spool file when
    /// it is closed, delegating everything else to the underlying reader.
    private static final class SpooledVideoReader extends VideoReader {
        private final VideoReader delegate;
        private final String spoolPath;

        SpooledVideoReader(VideoReader delegate, String spoolPath) {
            this.delegate = delegate;
            this.spoolPath = spoolPath;
        }

        public int getWidth() { return delegate.getWidth(); }
        public int getHeight() { return delegate.getHeight(); }
        public long getDurationMillis() { return delegate.getDurationMillis(); }
        public float getFrameRate() { return delegate.getFrameRate(); }
        public boolean hasVideo() { return delegate.hasVideo(); }
        public boolean hasAudio() { return delegate.hasAudio(); }
        public int getAudioSampleRate() { return delegate.getAudioSampleRate(); }
        public int getAudioChannels() { return delegate.getAudioChannels(); }
        public VideoFrame frameAt(long millis) throws IOException { return delegate.frameAt(millis); }
        public void readFrames(float fps, FrameCallback callback) throws IOException { delegate.readFrames(fps, callback); }
        public AudioBuffer readAudio() throws IOException { return delegate.readAudio(); }

        public void close() throws IOException {
            try {
                delegate.close();
            } finally {
                try {
                    FileSystemStorage.getInstance().delete(spoolPath);
                } catch (Throwable ignored) {
                    // best-effort spool cleanup
                }
            }
        }
    }
}
