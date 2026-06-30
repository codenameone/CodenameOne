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

import java.io.IOException;

/// Decodes an existing video clip into individual RGBA frames and PCM audio. This is the
/// read side that `com.codename1.media.Media` deliberately does not provide: `Media` is
/// a player whose `Media#setTime(int)` snaps to key frames and gives no exact frame
/// guarantee, whereas `VideoReader` decodes to precise frames.
///
/// Two access patterns are offered:
///
/// - `#frameAt(long)` returns the single frame at (or nearest before) a given millisecond
///   offset. This is frame accurate, unlike seeking a `Media` player.
/// - `#readFrames(float, FrameCallback)` walks the whole clip emitting frames at a fixed
///   target frame rate. Source clips are frequently variable frame rate (VFR), for
///   example screen recordings; this resamples them to a constant frame rate (CFR) so the
///   caller receives an evenly spaced sequence it can re-encode or process.
///
/// The audio track, when present, is exposed as interleaved PCM through `#readAudio()`.
///
/// Obtain an instance from `VideoIO#openReader(String)`. Always `#close()` it when done.
///
/// @author Shai Almog
///
/// #### Since
///
/// 8.0
public abstract class VideoReader {
    /// The width of the video track in pixels, or -1 if there is no video.
    public abstract int getWidth();

    /// The height of the video track in pixels, or -1 if there is no video.
    public abstract int getHeight();

    /// The total duration of the clip in milliseconds, or -1 if unknown.
    public abstract long getDurationMillis();

    /// The average/nominal frame rate of the source video track. For variable frame rate
    /// sources this is an average; use `#readFrames(float, FrameCallback)` to obtain an
    /// evenly spaced sequence.
    public abstract float getFrameRate();

    /// True if the clip has a decodable video track.
    public abstract boolean hasVideo();

    /// True if the clip has a decodable audio track.
    public abstract boolean hasAudio();

    /// The sample rate of the audio track in Hz, or -1 if there is no audio.
    public abstract int getAudioSampleRate();

    /// The number of channels in the audio track, or -1 if there is no audio.
    public abstract int getAudioChannels();

    /// Returns the frame at (or nearest at/before) the given timestamp, decoded to RGBA.
    /// Unlike seeking a `Media` player, the returned frame corresponds exactly to the
    /// requested position rather than the closest key frame.
    ///
    /// #### Parameters
    ///
    /// - `millis`: the timestamp to seek to in milliseconds
    ///
    /// #### Returns
    ///
    /// the decoded `VideoFrame`, or null if the position is past the end of the clip
    ///
    /// #### Throws
    ///
    /// - `IOException`: if decoding fails
    public abstract VideoFrame frameAt(long millis) throws IOException;

    /// Walks the entire clip, decoding and resampling it to the requested constant frame
    /// rate, invoking the callback once per output frame in increasing timestamp order.
    /// This converts a variable frame rate source to a constant frame rate sequence by
    /// duplicating or dropping source frames as needed.
    ///
    /// #### Parameters
    ///
    /// - `fps`: the target constant output frame rate in frames per second
    ///
    /// - `callback`: receives each decoded frame; return false from it to stop early
    ///
    /// #### Throws
    ///
    /// - `IOException`: if decoding fails
    public abstract void readFrames(float fps, FrameCallback callback) throws IOException;

    /// Reads the entire audio track and returns it as a single `AudioBuffer` of
    /// interleaved floating point samples. For very long clips consider using the audio
    /// only at a reduced rate or processing the clip in segments, as the whole track is
    /// held in memory.
    ///
    /// #### Returns
    ///
    /// the decoded audio, or null if the clip has no audio track
    ///
    /// #### Throws
    ///
    /// - `IOException`: if decoding fails
    public abstract AudioBuffer readAudio() throws IOException;

    /// Releases the decoder and any native resources. The reader cannot be used after
    /// this call.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if releasing resources fails
    public abstract void close() throws IOException;

    /// Callback used by `VideoReader#readFrames(float, FrameCallback)` to receive frames
    /// as they are decoded.
    public interface FrameCallback {
        /// Called once per decoded output frame.
        ///
        /// #### Parameters
        ///
        /// - `frame`: the decoded frame
        ///
        /// #### Returns
        ///
        /// true to continue decoding, false to stop early
        boolean frame(VideoFrame frame);
    }
}
