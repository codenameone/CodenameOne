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

/// A fluent builder describing how a video file should be encoded. It mirrors the
/// style of `MediaRecorderBuilder` but covers the full set of knobs needed for video:
/// dimensions, frame rate, codec, bitrate and key frame (GOP) interval, plus the audio
/// track parameters. Build a `VideoWriter` with `#build()` and then push frames and
/// audio into it.
///
/// Example:
///
/// ```java
/// VideoWriter w = new VideoWriterBuilder()
///         .path(FileSystemStorage.getInstance().getAppHomePath() + "/out.mp4")
///         .width(720).height(1280).frameRate(30)
///         .videoCodec(VideoIO.CODEC_H264).videoBitRate(4_000_000)
///         .hasAudio(true).audioCodec(VideoIO.CODEC_AAC).sampleRate(44100).audioChannels(2)
///         .build();
/// ```
public class VideoWriterBuilder {
    private String path;
    private String container = VideoIO.CONTAINER_MP4;

    private boolean hasVideo = true;
    private int width = 1280;
    private int height = 720;
    private float frameRate = 30f;
    private String videoCodec = VideoIO.CODEC_H264;
    private int videoBitRate = -1;
    private float keyFrameInterval = 2f;

    private boolean hasAudio;
    private String audioCodec = VideoIO.CODEC_AAC;
    private int audioBitRate = 128000;
    private int sampleRate = 44100;
    private int audioChannels = 2;

    /// Sets the output path where the encoded video will be written, as a
    /// `com.codename1.io.FileSystemStorage` path. Required.
    ///
    /// #### Parameters
    ///
    /// - `path`: the output FileSystemStorage path
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder path(String path) {
        this.path = path;
        return this;
    }

    /// Sets the container format, e.g. `VideoIO#CONTAINER_MP4` (default) or
    /// `VideoIO#CONTAINER_WEBM`.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder container(String container) {
        this.container = container;
        return this;
    }

    /// Enables or disables the video track. Defaults to true.
    ///
    /// Audio-only output (`hasVideo(false)`) is not currently implemented by the
    /// platform backends, so {@link #build()} rejects it rather than silently
    /// producing a file with an empty or unexpected video track.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder hasVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
        return this;
    }

    /// Sets the frame width in pixels. Default 1280.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder width(int width) {
        this.width = width;
        return this;
    }

    /// Sets the frame height in pixels. Default 720.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder height(int height) {
        this.height = height;
        return this;
    }

    /// Sets the constant target frame rate in frames per second. Default 30. Frames are
    /// timestamped by the application when calling `VideoWriter#writeFrame`, this value
    /// is the nominal rate written into the file metadata and used by encoders that need
    /// it.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder frameRate(float frameRate) {
        this.frameRate = frameRate;
        return this;
    }

    /// Sets the video codec by id, e.g. `VideoIO#CODEC_H264` (default). Use
    /// `VideoIO#getAvailableEncoders()` to discover what the platform supports.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder videoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
        return this;
    }

    /// Sets the target video bitrate in bits per second. When left at the default (-1)
    /// the implementation picks a reasonable value derived from the resolution and frame
    /// rate.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder videoBitRate(int videoBitRate) {
        this.videoBitRate = videoBitRate;
        return this;
    }

    /// Sets the key frame (GOP) interval in seconds. A smaller value produces files that
    /// seek more accurately at the cost of size. Default 2 seconds.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder keyFrameInterval(float seconds) {
        this.keyFrameInterval = seconds;
        return this;
    }

    /// Enables or disables the audio track. Defaults to false. When enabled, push PCM
    /// samples with `VideoWriter#writeAudio`.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder hasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
        return this;
    }

    /// Sets the audio codec by id, e.g. `VideoIO#CODEC_AAC` (default).
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder audioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
        return this;
    }

    /// Sets the target audio bitrate in bits per second. Default 128000.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder audioBitRate(int audioBitRate) {
        this.audioBitRate = audioBitRate;
        return this;
    }

    /// Sets the audio sample rate in Hz. Default 44100.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder sampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    /// Sets the number of audio channels. Default 2.
    ///
    /// #### Returns
    ///
    /// Self for chaining.
    public VideoWriterBuilder audioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
        return this;
    }

    /// Builds the `VideoWriter` with the current settings.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the writer could not be created
    ///
    /// - `IllegalStateException`: if `#path(String)` was not set, video encoding is not
    /// supported on this platform, or audio-only output (`#hasVideo(boolean)` set to false)
    /// was requested
    public VideoWriter build() throws IOException {
        if (path == null) {
            throw new IllegalStateException("Must set path for VideoWriterBuilder");
        }
        VideoIO io = VideoIO.getVideoIO();
        if (io == null) {
            throw new IllegalStateException("VideoIO is not supported on this platform");
        }
        if (!hasVideo) {
            // Audio-only output is not implemented by the platform backends
            // (they always create a video encoder/pipeline); reject it here so
            // the behaviour is consistent across platforms rather than producing
            // a bogus video track.
            throw new IllegalStateException(
                    "audio-only output (hasVideo(false)) is not supported by VideoIO");
        }
        return io.createWriter(this);
    }

    /// The configured output path.
    public String getPath() {
        return path;
    }

    /// The configured container format.
    public String getContainer() {
        return container;
    }

    /// True if the video track is enabled.
    public boolean isHasVideo() {
        return hasVideo;
    }

    /// The configured frame width.
    public int getWidth() {
        return width;
    }

    /// The configured frame height.
    public int getHeight() {
        return height;
    }

    /// The configured frame rate.
    public float getFrameRate() {
        return frameRate;
    }

    /// The configured video codec id.
    public String getVideoCodec() {
        return videoCodec;
    }

    /// The configured video bitrate, or -1 to let the implementation choose.
    public int getVideoBitRate() {
        return videoBitRate;
    }

    /// The configured key frame interval in seconds.
    public float getKeyFrameInterval() {
        return keyFrameInterval;
    }

    /// True if the audio track is enabled.
    public boolean isHasAudio() {
        return hasAudio;
    }

    /// The configured audio codec id.
    public String getAudioCodec() {
        return audioCodec;
    }

    /// The configured audio bitrate.
    public int getAudioBitRate() {
        return audioBitRate;
    }

    /// The configured audio sample rate.
    public int getSampleRate() {
        return sampleRate;
    }

    /// The configured audio channel count.
    public int getAudioChannels() {
        return audioChannels;
    }
}
