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

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/// Contract / behaviour tests for the platform agnostic `VideoIO` API. These run against
/// the lightweight mock implementation with an in-memory `VideoIO` double installed, so
/// they are deterministic and require no native codecs. The real ffmpeg backed encode /
/// decode round trip is covered separately in the JavaSE port module.
class VideoIOTest extends UITestBase {
    private DoubleVideoIO io;

    @BeforeEach
    void installDouble() {
        io = new DoubleVideoIO();
        implementation.setVideoIO(io);
    }

    @Test
    void isSupportedReflectsImplementation() {
        assertTrue(VideoIO.isSupported());
        assertSame(io, VideoIO.getVideoIO());

        implementation.setVideoIO(null);
        assertFalse(VideoIO.isSupported());
        assertNull(VideoIO.getVideoIO());
    }

    @Test
    void builderHasSensibleDefaults() {
        VideoWriterBuilder b = new VideoWriterBuilder();
        assertEquals(VideoIO.CONTAINER_MP4, b.getContainer());
        assertEquals(VideoIO.CODEC_H264, b.getVideoCodec());
        assertEquals(VideoIO.CODEC_AAC, b.getAudioCodec());
        assertEquals(30f, b.getFrameRate(), 0.001f);
        assertEquals(44100, b.getSampleRate());
        assertEquals(2, b.getAudioChannels());
        assertTrue(b.isHasVideo());
        assertFalse(b.isHasAudio());
        assertEquals(-1, b.getVideoBitRate());
    }

    @Test
    void builderIsFluentAndStoresValues() {
        VideoWriterBuilder b = new VideoWriterBuilder()
                .path("mem://x").container(VideoIO.CONTAINER_WEBM)
                .width(320).height(240).frameRate(24)
                .videoCodec(VideoIO.CODEC_VP9).videoBitRate(2_000_000).keyFrameInterval(1.5f)
                .hasAudio(true).audioCodec(VideoIO.CODEC_OPUS).audioBitRate(96000)
                .sampleRate(48000).audioChannels(1);
        assertEquals("mem://x", b.getPath());
        assertEquals(VideoIO.CONTAINER_WEBM, b.getContainer());
        assertEquals(320, b.getWidth());
        assertEquals(240, b.getHeight());
        assertEquals(24f, b.getFrameRate(), 0.001f);
        assertEquals(VideoIO.CODEC_VP9, b.getVideoCodec());
        assertEquals(2_000_000, b.getVideoBitRate());
        assertEquals(1.5f, b.getKeyFrameInterval(), 0.001f);
        assertTrue(b.isHasAudio());
        assertEquals(VideoIO.CODEC_OPUS, b.getAudioCodec());
        assertEquals(96000, b.getAudioBitRate());
        assertEquals(48000, b.getSampleRate());
        assertEquals(1, b.getAudioChannels());
    }

    @Test
    void buildWithoutPathThrows() {
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                new VideoWriterBuilder().build();
            }
        });
    }

    @Test
    void buildWithoutVideoIOThrows() {
        implementation.setVideoIO(null);
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                new VideoWriterBuilder().path("mem://x").build();
            }
        });
    }

    @Test
    void videoFrameExposesArgbAndRgba() {
        int[] argb = {0xff112233, 0x80445566};
        VideoFrame f = new VideoFrame(argb, 2, 1, 500);
        assertEquals(2, f.getWidth());
        assertEquals(1, f.getHeight());
        assertEquals(500, f.getTimestampMillis());
        assertArrayEquals(argb, f.getARGB());

        byte[] rgba = new byte[2 * 1 * 4];
        f.getRGBA(rgba);
        // pixel 0: A=ff R=11 G=22 B=33 -> R,G,B,A
        assertEquals(0x11, rgba[0] & 0xff);
        assertEquals(0x22, rgba[1] & 0xff);
        assertEquals(0x33, rgba[2] & 0xff);
        assertEquals(0xff, rgba[3] & 0xff);
        // pixel 1: A=80 R=44 G=55 B=66
        assertEquals(0x44, rgba[4] & 0xff);
        assertEquals(0x55, rgba[5] & 0xff);
        assertEquals(0x66, rgba[6] & 0xff);
        assertEquals(0x80, rgba[7] & 0xff);
    }

    @Test
    void videoFrameRejectsMismatchedLength() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                new VideoFrame(new int[3], 2, 2, 0);
            }
        });
    }

    @Test
    void videoFrameToImageMatchesDimensions() {
        VideoFrame f = new VideoFrame(new int[4], 2, 2, 0);
        assertEquals(2, f.toImage().getWidth());
        assertEquals(2, f.toImage().getHeight());
    }

    @Test
    void codecSupportScansAvailableLists() {
        assertTrue(io.isEncoderSupported(VideoIO.CODEC_H264));
        assertTrue(io.isEncoderSupported(VideoIO.CODEC_AAC));
        assertFalse(io.isEncoderSupported(VideoIO.CODEC_AV1));
        assertTrue(io.isDecoderSupported(VideoIO.CODEC_H264));
        assertFalse(io.isDecoderSupported("does-not-exist"));

        VideoCodec h264 = io.getAvailableEncoders()[0];
        assertEquals(VideoIO.CODEC_H264, h264.getId());
        assertTrue(h264.isVideo());
        assertTrue(h264.isEncoder());
        assertTrue(h264.isHardwareAccelerated());
    }

    @Test
    void encodeThenDecodeRoundTripsThroughPublicApi() throws IOException {
        int w = 4, h = 2;
        VideoWriter writer = new VideoWriterBuilder()
                .path("mem://clip").width(w).height(h).frameRate(10)
                .hasAudio(true).sampleRate(8000).audioChannels(1)
                .build();
        int[][] sent = new int[3][];
        for (int i = 0; i < 3; i++) {
            int[] argb = new int[w * h];
            for (int p = 0; p < argb.length; p++) {
                argb[p] = 0xff000000 | (i * 40 + p);
            }
            sent[i] = argb;
            writer.writeFrame(argb, w, h, i * 100);
        }
        short[] pcm = new short[8000]; // 1 second mono @ 8kHz
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = (short) (i % 100);
        }
        writer.writeAudio(pcm, 8000, 1, 0);
        writer.close();

        VideoReader reader = io.openReader("mem://clip");
        assertEquals(w, reader.getWidth());
        assertEquals(h, reader.getHeight());
        assertTrue(reader.hasVideo());
        assertTrue(reader.hasAudio());
        assertEquals(8000, reader.getAudioSampleRate());
        assertEquals(1, reader.getAudioChannels());

        final List<VideoFrame> got = new ArrayList<VideoFrame>();
        reader.readFrames(10, new VideoReader.FrameCallback() {
            @Override
            public boolean frame(VideoFrame frame) {
                got.add(frame);
                return true;
            }
        });
        assertEquals(3, got.size());
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(sent[i], got.get(i).getARGB());
        }

        VideoFrame at = reader.frameAt(150);
        assertNotNull(at);
        assertArrayEquals(sent[1], at.getARGB());

        AudioBuffer audio = reader.readAudio();
        assertNotNull(audio);
        assertEquals(8000, audio.getSize());
        reader.close();
    }

    @Test
    void readFramesStopsWhenCallbackReturnsFalse() throws IOException {
        VideoWriter writer = new VideoWriterBuilder().path("mem://stop").width(2).height(2).build();
        for (int i = 0; i < 5; i++) {
            writer.writeFrame(new int[4], 2, 2, i * 10);
        }
        writer.close();

        final AtomicInteger count = new AtomicInteger();
        io.openReader("mem://stop").readFrames(30, new VideoReader.FrameCallback() {
            @Override
            public boolean frame(VideoFrame frame) {
                count.incrementAndGet();
                return false;
            }
        });
        assertEquals(1, count.get());
    }

    // ------------------------------------------------------------------
    // In-memory VideoIO double
    // ------------------------------------------------------------------

    private static final class DoubleVideoIO extends VideoIO {
        private final Map<String, Recording> store = new HashMap<String, Recording>();

        @Override
        public VideoCodec[] getAvailableEncoders() {
            return new VideoCodec[]{
                new VideoCodec(CODEC_H264, "h264_test", "video/avc", true, true, false, true, 4096, 4096, new String[]{CONTAINER_MP4}),
                new VideoCodec(CODEC_AAC, "aac", "audio/mp4a-latm", false, true, false, false, -1, -1, new String[]{CONTAINER_MP4})
            };
        }

        @Override
        public VideoCodec[] getAvailableDecoders() {
            return new VideoCodec[]{
                new VideoCodec(CODEC_H264, "h264_test", "video/avc", true, false, true, true, -1, -1, new String[0])
            };
        }

        @Override
        public VideoWriter createWriter(VideoWriterBuilder cfg) {
            return new DoubleWriter(cfg, store);
        }

        @Override
        public VideoReader openReader(String filePath) throws IOException {
            Recording r = store.get(filePath);
            if (r == null) {
                throw new IOException("no such clip: " + filePath);
            }
            return new DoubleReader(r);
        }
    }

    private static final class Recording {
        int width, height;
        float frameRate;
        final List<int[]> frames = new ArrayList<int[]>();
        final List<Long> timestamps = new ArrayList<Long>();
        short[] audio;
        int sampleRate = -1, channels = -1;
    }

    private static final class DoubleWriter extends VideoWriter {
        private final Recording rec = new Recording();
        private final Map<String, Recording> store;
        private final String path;

        DoubleWriter(VideoWriterBuilder cfg, Map<String, Recording> store) {
            this.store = store;
            this.path = cfg.getPath();
            rec.width = cfg.getWidth();
            rec.height = cfg.getHeight();
            rec.frameRate = cfg.getFrameRate();
        }

        @Override
        public void writeFrame(int[] argb, int width, int height, long pts) {
            rec.frames.add(argb.clone());
            rec.timestamps.add(pts);
        }

        @Override
        public void writeAudio(short[] interleavedPcm, int sampleRate, int channels, long pts) {
            rec.audio = interleavedPcm.clone();
            rec.sampleRate = sampleRate;
            rec.channels = channels;
        }

        @Override
        public void close() {
            store.put(path, rec);
        }

        @Override
        public int getWidth() {
            return rec.width;
        }

        @Override
        public int getHeight() {
            return rec.height;
        }

        @Override
        public float getFrameRate() {
            return rec.frameRate;
        }
    }

    private static final class DoubleReader extends VideoReader {
        private final Recording rec;

        DoubleReader(Recording rec) {
            this.rec = rec;
        }

        @Override
        public int getWidth() {
            return rec.width;
        }

        @Override
        public int getHeight() {
            return rec.height;
        }

        @Override
        public long getDurationMillis() {
            return rec.timestamps.isEmpty() ? 0 : rec.timestamps.get(rec.timestamps.size() - 1);
        }

        @Override
        public float getFrameRate() {
            return rec.frameRate;
        }

        @Override
        public boolean hasVideo() {
            return !rec.frames.isEmpty();
        }

        @Override
        public boolean hasAudio() {
            return rec.audio != null;
        }

        @Override
        public int getAudioSampleRate() {
            return rec.sampleRate;
        }

        @Override
        public int getAudioChannels() {
            return rec.channels;
        }

        @Override
        public VideoFrame frameAt(long millis) {
            int idx = 0;
            for (int i = 0; i < rec.timestamps.size(); i++) {
                if (rec.timestamps.get(i) <= millis) {
                    idx = i;
                } else {
                    break;
                }
            }
            if (rec.frames.isEmpty()) {
                return null;
            }
            return new VideoFrame(rec.frames.get(idx).clone(), rec.width, rec.height, rec.timestamps.get(idx));
        }

        @Override
        public void readFrames(float fps, FrameCallback callback) {
            for (int i = 0; i < rec.frames.size(); i++) {
                VideoFrame f = new VideoFrame(rec.frames.get(i).clone(), rec.width, rec.height, rec.timestamps.get(i));
                if (!callback.frame(f)) {
                    break;
                }
            }
        }

        @Override
        public AudioBuffer readAudio() {
            if (rec.audio == null) {
                return null;
            }
            float[] f = new float[rec.audio.length];
            for (int i = 0; i < f.length; i++) {
                f[i] = rec.audio[i] / 32768f;
            }
            AudioBuffer b = new AudioBuffer(Math.max(1, f.length));
            b.copyFrom(rec.sampleRate, rec.channels, f);
            return b;
        }

        @Override
        public void close() {
        }
    }
}
