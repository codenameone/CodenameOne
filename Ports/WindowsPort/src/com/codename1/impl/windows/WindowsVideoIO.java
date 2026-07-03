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
package com.codename1.impl.windows;

import com.codename1.media.AudioBuffer;
import com.codename1.media.VideoCodec;
import com.codename1.media.VideoFrame;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Windows `com.codename1.media.VideoIO` implementation backed by Media Foundation
/// (`IMFSourceReader` for frame accurate decoding, `IMFSinkWriter` for encoding). The
/// native peers are opaque handles into `WindowsNative`; the Java layer marshals pixels
/// and performs the variable-to-constant frame rate resample.
class WindowsVideoIO extends VideoIO {
    @Override
    public VideoCodec[] getAvailableEncoders() {
        return codecs(true);
    }

    @Override
    public VideoCodec[] getAvailableDecoders() {
        return codecs(false);
    }

    private VideoCodec[] codecs(boolean encoder) {
        List<VideoCodec> out = new ArrayList<VideoCodec>();
        String[] mp4 = new String[]{CONTAINER_MP4};
        out.add(new VideoCodec(CODEC_H264, "H.264 (Media Foundation)", "video/avc", true, encoder, !encoder, true, -1, -1, mp4));
        if (WindowsNative.videoSupportsHEVC()) {
            out.add(new VideoCodec(CODEC_HEVC, "HEVC (Media Foundation)", "video/hevc", true, encoder, !encoder, true, -1, -1, mp4));
        }
        out.add(new VideoCodec(CODEC_AAC, "AAC (Media Foundation)", "audio/mp4a-latm", false, encoder, !encoder, false, -1, -1, mp4));
        return out.toArray(new VideoCodec[out.size()]);
    }

    @Override
    public VideoWriter createWriter(VideoWriterBuilder cfg) throws IOException {
        return new Writer(cfg);
    }

    @Override
    public VideoReader openReader(String filePath) throws IOException {
        long peer = WindowsNative.videoReaderOpen(filePath);
        if (peer == 0) {
            throw new IOException("Failed to open video: " + filePath);
        }
        return new Reader(peer);
    }

    static int[] rgbaToArgb(byte[] rgba, int pixels) {
        int[] argb = new int[pixels];
        int o = 0;
        for (int i = 0; i < pixels; i++) {
            int r = rgba[o] & 0xff;
            int g = rgba[o + 1] & 0xff;
            int b = rgba[o + 2] & 0xff;
            int a = rgba[o + 3] & 0xff;
            argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
            o += 4;
        }
        return argb;
    }

    static byte[] argbToRgba(int[] argb, int pixels) {
        byte[] rgba = new byte[pixels * 4];
        int o = 0;
        for (int i = 0; i < pixels; i++) {
            int p = argb[i];
            rgba[o++] = (byte) ((p >> 16) & 0xff);
            rgba[o++] = (byte) ((p >> 8) & 0xff);
            rgba[o++] = (byte) (p & 0xff);
            rgba[o++] = (byte) ((p >> 24) & 0xff);
        }
        return rgba;
    }

    static final class Reader extends VideoReader {
        private final long peer;
        private final int width, height, audioSampleRate, audioChannels;
        private final long duration;
        private final float frameRate;
        private final boolean hasVideo, hasAudio;

        Reader(long peer) {
            this.peer = peer;
            this.width = WindowsNative.videoReaderWidth(peer);
            this.height = WindowsNative.videoReaderHeight(peer);
            this.duration = WindowsNative.videoReaderDuration(peer);
            this.frameRate = WindowsNative.videoReaderFrameRate(peer);
            this.hasVideo = WindowsNative.videoReaderHasVideo(peer);
            this.hasAudio = WindowsNative.videoReaderHasAudio(peer);
            this.audioSampleRate = WindowsNative.videoReaderAudioSampleRate(peer);
            this.audioChannels = WindowsNative.videoReaderAudioChannels(peer);
        }

        public int getWidth() { return hasVideo ? width : -1; }
        public int getHeight() { return hasVideo ? height : -1; }
        public long getDurationMillis() { return duration; }
        public float getFrameRate() { return frameRate; }
        public boolean hasVideo() { return hasVideo; }
        public boolean hasAudio() { return hasAudio; }
        public int getAudioSampleRate() { return hasAudio ? audioSampleRate : -1; }
        public int getAudioChannels() { return hasAudio ? audioChannels : -1; }

        public VideoFrame frameAt(long millis) throws IOException {
            if (!hasVideo) {
                return null;
            }
            byte[] rgba = WindowsNative.videoReaderFrameAt(peer, Math.max(0, millis));
            if (rgba == null) {
                return null;
            }
            return new VideoFrame(rgbaToArgb(rgba, width * height), width, height, millis);
        }

        public void readFrames(float fps, FrameCallback callback) throws IOException {
            if (!hasVideo) {
                return;
            }
            if (fps <= 0f) {
                throw new IllegalArgumentException("fps must be positive");
            }
            long step = Math.max(1, Math.round(1000.0 / fps));
            for (long t = 0; duration <= 0 || t < duration; t += step) {
                VideoFrame f = frameAt(t);
                if (f == null || !callback.frame(f) || duration <= 0) {
                    break;
                }
            }
        }

        public AudioBuffer readAudio() throws IOException {
            if (!hasAudio) {
                return null;
            }
            byte[] pcm = WindowsNative.videoReaderReadAudio(peer);
            if (pcm == null) {
                return null;
            }
            int rate = audioSampleRate > 0 ? audioSampleRate : 44100;
            int ch = audioChannels > 0 ? audioChannels : 2;
            int sampleCount = pcm.length / 2;
            float[] samples = new float[sampleCount];
            int o = 0;
            for (int i = 0; i < sampleCount; i++) {
                int lo = pcm[o] & 0xff;
                int hi = pcm[o + 1];
                short s = (short) ((hi << 8) | lo);
                samples[i] = s / 32768f;
                o += 2;
            }
            AudioBuffer buffer = new AudioBuffer(Math.max(1, sampleCount));
            buffer.copyFrom(rate, ch, samples);
            return buffer;
        }

        public void close() throws IOException {
            WindowsNative.videoReaderClose(peer);
        }
    }

    static final class Writer extends VideoWriter {
        private final long peer;
        private final int width, height;
        private final float frameRate;
        private final boolean hasVideo, hasAudio;
        private boolean closed;

        Writer(VideoWriterBuilder cfg) throws IOException {
            this.width = cfg.getWidth();
            this.height = cfg.getHeight();
            this.frameRate = cfg.getFrameRate();
            this.hasVideo = cfg.isHasVideo();
            this.hasAudio = cfg.isHasAudio();
            boolean hevc = CODEC_HEVC.equals(cfg.getVideoCodec());
            int br = cfg.getVideoBitRate();
            if (br <= 0) {
                br = (int) Math.max(800000L, Math.min((long) (width * (long) height * Math.max(1f, frameRate) * 0.10), 100000000L));
            }
            int gop = Math.max(1, Math.round(cfg.getKeyFrameInterval() * Math.max(1f, frameRate)));
            this.peer = WindowsNative.videoWriterOpen(cfg.getPath(), hevc, width, height, frameRate, br, gop,
                    hasAudio, cfg.getAudioBitRate(), cfg.getSampleRate(), cfg.getAudioChannels());
            if (peer == 0) {
                throw new IOException("Failed to create video writer for " + cfg.getPath());
            }
        }

        public void writeFrame(int[] argb, int frameWidth, int frameHeight, long pts) throws IOException {
            if (closed) {
                throw new IOException("writer is closed");
            }
            if (!hasVideo) {
                throw new IOException("video track is not enabled for this writer");
            }
            if (frameWidth != width || frameHeight != height) {
                throw new IllegalArgumentException("frame is " + frameWidth + "x" + frameHeight
                        + " but writer was configured for " + width + "x" + height);
            }
            WindowsNative.videoWriterFrame(peer, argbToRgba(argb, width * height), width, height, Math.max(0, pts));
        }

        public void writeAudio(short[] interleavedPcm, int sampleRate, int channels, long pts) throws IOException {
            if (closed) {
                throw new IOException("writer is closed");
            }
            if (!hasAudio) {
                throw new IOException("audio track is not enabled for this writer");
            }
            byte[] bytes = new byte[interleavedPcm.length * 2];
            int o = 0;
            for (int i = 0; i < interleavedPcm.length; i++) {
                short s = interleavedPcm[i];
                bytes[o++] = (byte) (s & 0xff);
                bytes[o++] = (byte) ((s >> 8) & 0xff);
            }
            WindowsNative.videoWriterAudio(peer, bytes, sampleRate, channels, Math.max(0, pts));
        }

        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            if (!WindowsNative.videoWriterClose(peer)) {
                throw new IOException("Failed to finalize video file");
            }
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public float getFrameRate() { return frameRate; }
    }
}
