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

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;

import com.codename1.media.AudioBuffer;
import com.codename1.media.VideoFrame;
import com.codename1.media.VideoReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/// Android `VideoReader`. Frame accurate seeking and constant frame rate resampling are
/// implemented with `MediaMetadataRetriever#getFrameAtTime(long, int)` using
/// `OPTION_CLOSEST`, which returns the exact frame at a requested time (rotation already
/// applied). The audio track is decoded to PCM with `MediaExtractor` + `MediaCodec`.
class AndroidVideoReader extends VideoReader {
    private final String path;
    private final MediaMetadataRetriever retriever;
    private int width = -1;
    private int height = -1;
    private long durationMillis = -1;
    private float frameRate = 30f;
    private boolean hasVideo;
    private boolean hasAudio;
    private int audioSampleRate = -1;
    private int audioChannels = -1;

    AndroidVideoReader(String filePath) throws IOException {
        this.path = AndroidImplementation.removeFilePrefix(filePath);
        this.retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            String hasAud = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            if (w != null && h != null) {
                hasVideo = true;
                int vw = Integer.parseInt(w);
                int vh = Integer.parseInt(h);
                int rotation = rot != null ? Integer.parseInt(rot) : 0;
                if (rotation == 90 || rotation == 270) {
                    width = vh;
                    height = vw;
                } else {
                    width = vw;
                    height = vh;
                }
            }
            if (dur != null) {
                durationMillis = Long.parseLong(dur);
            }
            hasAudio = "yes".equalsIgnoreCase(hasAud);
        } catch (Exception ex) {
            throw new IOException("Failed to open video: " + filePath, ex);
        }
        probeStreams();
    }

    private void probeStreams() {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(path);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat fmt = extractor.getTrackFormat(i);
                String mime = fmt.getString(MediaFormat.KEY_MIME);
                if (mime == null) {
                    continue;
                }
                if (mime.startsWith("video/") && fmt.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    try {
                        frameRate = (float) fmt.getInteger(MediaFormat.KEY_FRAME_RATE);
                    } catch (Exception ex) {
                        try {
                            frameRate = fmt.getFloat(MediaFormat.KEY_FRAME_RATE);
                        } catch (Exception ignored) {
                        }
                    }
                } else if (mime.startsWith("audio/")) {
                    hasAudio = true;
                    if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        audioSampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    }
                    if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        audioChannels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            extractor.release();
        }
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
    public long getDurationMillis() {
        return durationMillis;
    }

    @Override
    public float getFrameRate() {
        return frameRate;
    }

    @Override
    public boolean hasVideo() {
        return hasVideo;
    }

    @Override
    public boolean hasAudio() {
        return hasAudio;
    }

    @Override
    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    @Override
    public int getAudioChannels() {
        return audioChannels;
    }

    @Override
    public VideoFrame frameAt(long millis) throws IOException {
        if (!hasVideo) {
            return null;
        }
        Bitmap bmp = retriever.getFrameAtTime(Math.max(0, millis) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST);
        if (bmp == null) {
            return null;
        }
        try {
            return toFrame(bmp, millis);
        } finally {
            bmp.recycle();
        }
    }

    @Override
    public void readFrames(float fps, FrameCallback callback) throws IOException {
        if (!hasVideo) {
            return;
        }
        if (fps <= 0f) {
            throw new IllegalArgumentException("fps must be positive");
        }
        long duration = durationMillis > 0 ? durationMillis : 0;
        long step = Math.max(1, Math.round(1000.0 / fps));
        for (long t = 0; duration == 0 || t < duration; t += step) {
            Bitmap bmp = retriever.getFrameAtTime(t * 1000L, MediaMetadataRetriever.OPTION_CLOSEST);
            if (bmp == null) {
                break;
            }
            VideoFrame frame;
            try {
                frame = toFrame(bmp, t);
            } finally {
                bmp.recycle();
            }
            if (!callback.frame(frame)) {
                break;
            }
            if (duration == 0) {
                break;
            }
        }
    }

    private static VideoFrame toFrame(Bitmap bmp, long millis) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] argb = new int[w * h];
        bmp.getPixels(argb, 0, w, 0, 0, w, h);
        return new VideoFrame(argb, w, h, millis);
    }

    @Override
    public AudioBuffer readAudio() throws IOException {
        if (!hasAudio) {
            return null;
        }
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try {
            extractor.setDataSource(path);
            int track = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat fmt = extractor.getTrackFormat(i);
                String mime = fmt.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i;
                    format = fmt;
                    break;
                }
            }
            if (track < 0) {
                return null;
            }
            extractor.selectTrack(track);
            int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;
            audioSampleRate = sampleRate;
            audioChannels = channels;

            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();

            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;
            while (!outputDone) {
                if (!inputDone) {
                    int inIndex = codec.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer in = codec.getInputBuffer(inIndex);
                        int size = extractor.readSampleData(in, 0);
                        if (size < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long pts = extractor.getSampleTime();
                            codec.queueInputBuffer(inIndex, 0, size, pts, 0);
                            extractor.advance();
                        }
                    }
                }
                int outIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outIndex >= 0) {
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && info.size > 0) {
                        ByteBuffer out = codec.getOutputBuffer(outIndex);
                        byte[] chunk = new byte[info.size];
                        out.position(info.offset);
                        out.get(chunk, 0, info.size);
                        pcm.write(chunk);
                    }
                    codec.releaseOutputBuffer(outIndex, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true;
                    }
                }
            }

            byte[] bytes = pcm.toByteArray();
            int sampleCount = bytes.length / 2;
            float[] samples = new float[sampleCount];
            int o = 0;
            for (int i = 0; i < sampleCount; i++) {
                int lo = bytes[o] & 0xff;
                int hi = bytes[o + 1];
                short s = (short) ((hi << 8) | lo);
                samples[i] = s / 32768f;
                o += 2;
            }
            AudioBuffer buffer = new AudioBuffer(Math.max(1, sampleCount));
            buffer.copyFrom(sampleRate, channels, samples);
            return buffer;
        } catch (Exception ex) {
            throw new IOException("Failed to decode audio", ex);
        } finally {
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
            extractor.release();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            retriever.release();
        } catch (Exception ignored) {
        }
    }
}
