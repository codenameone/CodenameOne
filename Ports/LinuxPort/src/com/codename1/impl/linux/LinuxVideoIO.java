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
package com.codename1.impl.linux;

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

/// Linux `com.codename1.media.VideoIO` implementation backed by GStreamer
/// (`filesrc ! decodebin ! videoconvert ! appsink` for frame accurate decoding,
/// `appsrc ! videoconvert ! x264enc ! mp4mux ! filesink` for encoding). The native peers
/// are opaque handles into `LinuxNative`.
class LinuxVideoIO extends VideoIO {
    @Override
    public VideoCodec[] getAvailableEncoders() {
        return codecs(true);
    }

    @Override
    public VideoCodec[] getAvailableDecoders() {
        return codecs(false);
    }

    /// Decoding runs through `decodebin`, which auto-plugs whatever decoder the host
    /// has, so decodability is a capability question and is asked of the registry by
    /// caps -- exactly what decodebin itself asks. A name list cannot answer it: the
    /// VA plugin spells H.264 `vah264dec`, stateless V4L2 spells it `v4l2slh264dec`,
    /// and AAC arrives as `avdec_aac`, `avdec_aac_fixed`, `faad` or `fdkaacdec`
    /// depending on the build.
    ///
    /// Encoding has no such freedom. The writer builds one fixed pipeline, so an
    /// encoder is offered only when the exact factory that pipeline names is present:
    /// answering "H.264 encodes" because some other H.264 encoder exists would send
    /// the caller into a pipeline that still cannot be built.
    private static final String H264_CAPS = "video/x-h264";
    private static final String HEVC_CAPS = "video/x-h265";
    private static final String AAC_CAPS = "audio/mpeg, mpegversion=(int)4";

    /// The codecs this host can really handle. Reporting H.264, HEVC and AAC
    /// unconditionally described the pipelines this port *would like* to build rather
    /// than the plugins that are installed: a distribution carrying only
    /// gstreamer plugins-base and plugins-good has no x264enc, x265enc or avenc_aac,
    /// so callers picked an encoder that could not exist and the failure only showed
    /// up as a broken write much later.
    private VideoCodec[] codecs(boolean encoder) {
        List<VideoCodec> out = new ArrayList<VideoCodec>();
        String[] mp4 = new String[]{CONTAINER_MP4};
        if (!LinuxNative.videoBackendAvailable()) {
            return new VideoCodec[0];
        }
        // Every encoder pipeline muxes into MP4 through a parser, so a missing muxer
        // or parser disqualifies the codec just as surely as a missing encoder.
        boolean mux = LinuxNative.videoFactoryAvailable("mp4mux");
        if (encoder ? (mux && LinuxNative.videoFactoryAvailable("x264enc") && LinuxNative.videoFactoryAvailable("h264parse"))
                : LinuxNative.videoDecoderAvailable(H264_CAPS)) {
            out.add(new VideoCodec(CODEC_H264, "H.264 (GStreamer)", "video/avc", true, encoder, !encoder, false, -1, -1, mp4));
        }
        if (encoder ? (mux && LinuxNative.videoFactoryAvailable("x265enc") && LinuxNative.videoFactoryAvailable("h265parse"))
                : LinuxNative.videoDecoderAvailable(HEVC_CAPS)) {
            out.add(new VideoCodec(CODEC_HEVC, "HEVC (GStreamer)", "video/hevc", true, encoder, !encoder, false, -1, -1, mp4));
        }
        if (encoder ? (mux && LinuxNative.videoFactoryAvailable("avenc_aac") && LinuxNative.videoFactoryAvailable("aacparse"))
                : LinuxNative.videoDecoderAvailable(AAC_CAPS)) {
            out.add(new VideoCodec(CODEC_AAC, "AAC (GStreamer)", "audio/mp4a-latm", false, encoder, !encoder, false, -1, -1, mp4));
        }
        return out.toArray(new VideoCodec[out.size()]);
    }

    @Override
    public VideoWriter createWriter(VideoWriterBuilder cfg) throws IOException {
        return new Writer(cfg);
    }

    @Override
    public VideoReader openReader(String filePath) throws IOException {
        long peer = LinuxNative.videoReaderOpen(filePath);
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
            this.width = LinuxNative.videoReaderWidth(peer);
            this.height = LinuxNative.videoReaderHeight(peer);
            this.duration = LinuxNative.videoReaderDuration(peer);
            this.frameRate = LinuxNative.videoReaderFrameRate(peer);
            this.hasVideo = LinuxNative.videoReaderHasVideo(peer);
            this.hasAudio = LinuxNative.videoReaderHasAudio(peer);
            this.audioSampleRate = LinuxNative.videoReaderAudioSampleRate(peer);
            this.audioChannels = LinuxNative.videoReaderAudioChannels(peer);
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
            byte[] rgba = LinuxNative.videoReaderFrameAt(peer, Math.max(0, millis));
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
            byte[] pcm = LinuxNative.videoReaderReadAudio(peer);
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
            LinuxNative.videoReaderClose(peer);
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
            this.peer = LinuxNative.videoWriterOpen(cfg.getPath(), hevc, width, height, frameRate, br, gop,
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
            LinuxNative.videoWriterFrame(peer, argbToRgba(argb, width * height), width, height, Math.max(0, pts));
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
            LinuxNative.videoWriterAudio(peer, bytes, sampleRate, channels, Math.max(0, pts));
        }

        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            if (!LinuxNative.videoWriterClose(peer)) {
                throw new IOException("Failed to finalize video file");
            }
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public float getFrameRate() { return frameRate; }
    }
}
