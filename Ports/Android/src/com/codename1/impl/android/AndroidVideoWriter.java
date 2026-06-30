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
package com.codename1.impl.android;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.codename1.media.VideoIO;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/// Android `VideoWriter`. Video frames are encoded with a `MediaCodec` configured for
/// flexible YUV420 byte buffer input (no GL/Surface dependency); audio is encoded to AAC
/// with a second `MediaCodec`. The compressed samples are buffered and muxed into the
/// final MP4 with `MediaMuxer` on `#close()`, which keeps the streaming logic simple and
/// avoids the track-ready ordering hazards of live interleaving.
class AndroidVideoWriter extends VideoWriter {
    private static final long TIMEOUT_US = 10000;

    private final int width;
    private final int height;
    private final float frameRate;
    private final boolean hasVideo;
    private final boolean hasAudio;
    private final String outputPath;

    private MediaCodec videoCodec;
    private MediaFormat videoFormat;
    private final List<Sample> videoSamples = new ArrayList<Sample>();

    private MediaCodec audioCodec;
    private MediaFormat audioFormat;
    private final List<Sample> audioSamples = new ArrayList<Sample>();
    private int audioSampleRate;
    private int audioChannels;
    private long audioFramesFed;

    private boolean closed;

    AndroidVideoWriter(VideoWriterBuilder cfg) throws IOException {
        this.width = cfg.getWidth();
        this.height = cfg.getHeight();
        this.frameRate = cfg.getFrameRate();
        this.hasVideo = cfg.isHasVideo();
        this.hasAudio = cfg.isHasAudio();
        this.outputPath = AndroidImplementation.removeFilePrefix(cfg.getPath());
        this.audioSampleRate = cfg.getSampleRate();
        this.audioChannels = cfg.getAudioChannels();

        if (!hasVideo && !hasAudio) {
            throw new IllegalStateException("VideoWriter must have a video or audio track");
        }

        if (hasVideo) {
            String mime = AndroidVideoIO.mimeForCodec(cfg.getVideoCodec());
            if (mime == null || mime.startsWith("audio/")) {
                mime = AndroidVideoIO.MIME_H264;
            }
            int bitRate = cfg.getVideoBitRate();
            if (bitRate <= 0) {
                bitRate = (int) Math.max(800000L, Math.min((long) (width * (long) height * Math.max(1f, frameRate) * 0.10), 100000000L));
            }
            MediaFormat format = MediaFormat.createVideoFormat(mime, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, Math.max(1, Math.round(frameRate)));
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Math.max(1, Math.round(cfg.getKeyFrameInterval())));
            try {
                videoCodec = MediaCodec.createEncoderByType(mime);
                videoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                videoCodec.start();
            } catch (Exception ex) {
                throw new IOException("Failed to start video encoder for " + mime, ex);
            }
        }

        if (hasAudio) {
            String mime = AndroidVideoIO.mimeForCodec(cfg.getAudioCodec());
            if (mime == null || mime.startsWith("video/")) {
                mime = AndroidVideoIO.MIME_AAC;
            }
            MediaFormat format = MediaFormat.createAudioFormat(mime, audioSampleRate, audioChannels);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, cfg.getAudioBitRate());
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024);
            try {
                audioCodec = MediaCodec.createEncoderByType(mime);
                audioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                audioCodec.start();
            } catch (Exception ex) {
                throw new IOException("Failed to start audio encoder for " + mime, ex);
            }
        }
    }

    @Override
    public void writeFrame(int[] argb, int frameWidth, int frameHeight, long presentationTimeMillis) throws IOException {
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
        long ptsUs = Math.max(0, presentationTimeMillis) * 1000L;
        while (true) {
            int index = videoCodec.dequeueInputBuffer(TIMEOUT_US);
            if (index >= 0) {
                Image img = videoCodec.getInputImage(index);
                fillImageFromArgb(img, argb, width, height);
                videoCodec.queueInputBuffer(index, 0, width * height * 3 / 2, ptsUs, 0);
                break;
            }
            drain(videoCodec, videoSamples, false);
        }
        videoFormat = drain(videoCodec, videoSamples, false);
    }

    @Override
    public void writeAudio(short[] interleavedPcm, int sampleRate, int channels, long presentationTimeMillis) throws IOException {
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
        int offset = 0;
        while (offset < bytes.length) {
            int index = audioCodec.dequeueInputBuffer(TIMEOUT_US);
            if (index >= 0) {
                ByteBuffer in = audioCodec.getInputBuffer(index);
                in.clear();
                int chunk = Math.min(in.remaining(), bytes.length - offset);
                in.put(bytes, offset, chunk);
                long framesSoFar = audioFramesFed;
                long ptsUs = framesSoFar * 1000000L / Math.max(1, audioSampleRate);
                audioCodec.queueInputBuffer(index, 0, chunk, ptsUs, 0);
                offset += chunk;
                audioFramesFed += (chunk / 2) / Math.max(1, audioChannels);
            } else {
                audioFormat = drain(audioCodec, audioSamples, false);
            }
        }
        audioFormat = drain(audioCodec, audioSamples, false);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (hasVideo) {
                signalEndOfStream(videoCodec);
                videoFormat = drainToEnd(videoCodec, videoSamples);
            }
            if (hasAudio) {
                signalEndOfStream(audioCodec);
                audioFormat = drainToEnd(audioCodec, audioSamples);
            }
            mux();
        } catch (Exception ex) {
            throw new IOException("Failed to finalize video", ex);
        } finally {
            releaseQuietly(videoCodec);
            releaseQuietly(audioCodec);
            videoCodec = null;
            audioCodec = null;
        }
    }

    private void mux() throws IOException {
        MediaMuxer muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int videoTrack = -1;
        int audioTrack = -1;
        if (hasVideo && videoFormat != null) {
            videoTrack = muxer.addTrack(videoFormat);
        }
        if (hasAudio && audioFormat != null) {
            audioTrack = muxer.addTrack(audioFormat);
        }
        muxer.start();
        try {
            if (videoTrack >= 0) {
                writeSamples(muxer, videoTrack, videoSamples);
            }
            if (audioTrack >= 0) {
                writeSamples(muxer, audioTrack, audioSamples);
            }
        } finally {
            try {
                muxer.stop();
            } catch (Exception ignored) {
            }
            muxer.release();
        }
    }

    private static void writeSamples(MediaMuxer muxer, int track, List<Sample> samples) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        for (Sample s : samples) {
            ByteBuffer buf = ByteBuffer.wrap(s.data);
            info.set(0, s.data.length, s.ptsUs, s.flags);
            muxer.writeSampleData(track, buf, info);
        }
    }

    /// Drains all currently available output, appending samples and returning the latest
    /// output format if one was reported (else the previous one).
    private MediaFormat drain(MediaCodec codec, List<Sample> out, boolean endOfStream) {
        MediaFormat format = codec == videoCodec ? videoFormat : audioFormat;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (true) {
            int index = codec.dequeueOutputBuffer(info, endOfStream ? TIMEOUT_US : 0);
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;
                }
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                format = codec.getOutputFormat();
            } else if (index >= 0) {
                ByteBuffer buf = codec.getOutputBuffer(index);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && info.size > 0) {
                    byte[] data = new byte[info.size];
                    buf.position(info.offset);
                    buf.get(data, 0, info.size);
                    out.add(new Sample(data, info.presentationTimeUs, info.flags));
                }
                boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                codec.releaseOutputBuffer(index, false);
                if (eos) {
                    break;
                }
            }
        }
        return format;
    }

    private MediaFormat drainToEnd(MediaCodec codec, List<Sample> out) {
        return drain(codec, out, true);
    }

    private void signalEndOfStream(MediaCodec codec) {
        while (true) {
            int index = codec.dequeueInputBuffer(TIMEOUT_US);
            if (index >= 0) {
                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                return;
            }
        }
    }

    private static void releaseQuietly(MediaCodec codec) {
        if (codec != null) {
            try {
                codec.stop();
            } catch (Exception ignored) {
            }
            try {
                codec.release();
            } catch (Exception ignored) {
            }
        }
    }

    /// Converts an ARGB pixel array into the YUV420 planes of a `MediaCodec` input image
    /// (BT.601 limited range), respecting each plane's row and pixel stride.
    static void fillImageFromArgb(Image image, int[] argb, int w, int h) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuf = planes[0].getBuffer();
        ByteBuffer uBuf = planes[1].getBuffer();
        ByteBuffer vBuf = planes[2].getBuffer();
        int yRowStride = planes[0].getRowStride();
        int yPixStride = planes[0].getPixelStride();
        int uRowStride = planes[1].getRowStride();
        int uPixStride = planes[1].getPixelStride();
        int vRowStride = planes[2].getRowStride();
        int vPixStride = planes[2].getPixelStride();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = argb[y * w + x];
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;
                int yy = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                yBuf.put(y * yRowStride + x * yPixStride, (byte) clamp(yy));
                if ((x & 1) == 0 && (y & 1) == 0) {
                    int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                    int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                    int cx = x / 2;
                    int cy = y / 2;
                    uBuf.put(cy * uRowStride + cx * uPixStride, (byte) clamp(u));
                    vBuf.put(cy * vRowStride + cx * vPixStride, (byte) clamp(v));
                }
            }
        }
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public float getFrameRate() {
        return frameRate;
    }

    private static final class Sample {
        final byte[] data;
        final long ptsUs;
        final int flags;

        Sample(byte[] data, long ptsUs, int flags) {
            this.data = data;
            this.ptsUs = ptsUs;
            this.flags = flags;
        }
    }
}
