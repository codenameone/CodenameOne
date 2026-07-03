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
package com.codename1.impl.javase.ffmpeg;

import com.codename1.media.AudioBuffer;
import com.codename1.media.VideoCodec;
import com.codename1.media.VideoFrame;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// End-to-end test of the ffmpeg backed `VideoIO` implementation: it encodes a short
/// synthetic clip (video frames + audio), decodes it back and asserts the metadata,
/// frame stream and audio survive the round trip. It is skipped automatically when no
/// ffmpeg/ffprobe binary is available (see `FFMPEGVideoIO#isAvailable()`).
public class FFMPEGVideoIORoundTripTest {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 48;
    private static final float FPS = 10f;
    private static final int FRAMES = 20;          // 2 seconds at 10 fps
    private static final int SAMPLE_RATE = 8000;
    private static final int AUDIO_SAMPLES = SAMPLE_RATE * 2; // ~2 seconds mono

    @Test
    public void enumeratesEncoders() {
        assumeTrue(FFMPEGVideoIO.isAvailable(), "ffmpeg not available");
        FFMPEGVideoIO io = new FFMPEGVideoIO();
        VideoCodec[] encoders = io.getAvailableEncoders();
        assertTrue(encoders.length > 0, "expected at least one encoder");
        boolean hasVideoEncoder = false;
        for (VideoCodec c : encoders) {
            if (c.isVideo() && c.isEncoder()) {
                hasVideoEncoder = true;
                break;
            }
        }
        assertTrue(hasVideoEncoder, "expected at least one video encoder");
        assertTrue(io.getAvailableDecoders().length > 0, "expected at least one decoder");
    }

    @Test
    public void encodeDecodeRoundTrip() throws Exception {
        assumeTrue(FFMPEGVideoIO.isAvailable(), "ffmpeg not available");
        FFMPEGVideoIO io = new FFMPEGVideoIO();

        String videoCodec = pickVideoCodec(io);
        assumeTrue(videoCodec != null, "no usable video encoder");

        File out = File.createTempFile("cn1-videoio-roundtrip", ".mp4");
        out.delete();
        try {
            VideoWriter writer = io.createWriter(new VideoWriterBuilder()
                    .path(out.getAbsolutePath())
                    .container(VideoIO.CONTAINER_MP4)
                    .width(WIDTH).height(HEIGHT).frameRate(FPS)
                    .videoCodec(videoCodec)
                    .hasAudio(true).audioCodec(VideoIO.CODEC_AAC)
                    .sampleRate(SAMPLE_RATE).audioChannels(1));

            for (int i = 0; i < FRAMES; i++) {
                writer.writeFrame(makeFrame(i), WIDTH, HEIGHT, Math.round(i * 1000f / FPS));
            }
            writer.writeAudio(makeTone(), SAMPLE_RATE, 1, 0);
            writer.close();

            assertTrue(out.exists() && out.length() > 0, "encoder produced no output");

            VideoReader reader = io.openReader(out.getAbsolutePath());
            try {
                assertEquals(WIDTH, reader.getWidth());
                assertEquals(HEIGHT, reader.getHeight());
                assertTrue(reader.hasVideo());
                assertTrue(reader.hasAudio());

                long duration = reader.getDurationMillis();
                assertTrue(duration > 1500 && duration < 2600, "unexpected duration " + duration);

                final List<Integer> frameSizes = new ArrayList<Integer>();
                reader.readFrames(FPS, new VideoReader.FrameCallback() {
                    @Override
                    public boolean frame(VideoFrame frame) {
                        assertEquals(WIDTH, frame.getWidth());
                        assertEquals(HEIGHT, frame.getHeight());
                        frameSizes.add(frame.getARGB().length);
                        return true;
                    }
                });
                assertTrue(frameSizes.size() >= FRAMES - 2 && frameSizes.size() <= FRAMES + 3,
                        "unexpected resampled frame count " + frameSizes.size());

                VideoFrame mid = reader.frameAt(1000);
                assertNotNull(mid);
                assertEquals(WIDTH, mid.getWidth());
                assertEquals(HEIGHT, mid.getHeight());

                assertEquals(SAMPLE_RATE, reader.getAudioSampleRate());
                int size = reader.readAudio().getSize();
                assertTrue(size > AUDIO_SAMPLES - 4000 && size < AUDIO_SAMPLES + 4000,
                        "unexpected audio sample count " + size);
            } finally {
                reader.close();
            }
        } finally {
            out.delete();
        }
    }

    /**
     * Mirrors the on-device VideoIORoundTripTest: encodes a 6-frame counting animation
     * (grey ramp 30..210 + a 440Hz tone), decodes the frames back and verifies the count
     * order survived the lossy codec (brightness strictly increasing + a wide spread) and
     * the audio PCM level (RMS) is in the expected band. Validates the device test's
     * tolerances against a real H.264/AAC round trip.
     */
    @Test
    public void countingAnimationRoundTrip() throws Exception {
        assumeTrue(FFMPEGVideoIO.isAvailable(), "ffmpeg not available");
        FFMPEGVideoIO io = new FFMPEGVideoIO();
        String videoCodec = pickVideoCodec(io);
        assumeTrue(videoCodec != null, "no usable video encoder");
        final int w = 128, h = 96, frames = 6, sr = 8000;
        final float fps = 6f;
        File out = File.createTempFile("cn1-videoio-counting", ".mp4");
        out.delete();
        try {
            VideoWriter writer = io.createWriter(new VideoWriterBuilder()
                    .path(out.getAbsolutePath()).container(VideoIO.CONTAINER_MP4)
                    .width(w).height(h).frameRate(fps).videoCodec(videoCodec)
                    .hasAudio(true).audioCodec(VideoIO.CODEC_AAC).sampleRate(sr).audioChannels(1));
            int spf = sr / frames;
            for (int i = 0; i < frames; i++) {
                int grey = 30 + i * 36;
                int fill = 0xff000000 | (grey << 16) | (grey << 8) | grey;
                int[] argb = new int[w * h];
                for (int p = 0; p < argb.length; p++) {
                    argb[p] = fill;
                }
                writer.writeFrame(argb, w, h, Math.round(i * 1000f / fps));
                short[] pcm = new short[spf];
                long base = (long) i * spf;
                for (int n = 0; n < spf; n++) {
                    double t = (base + n) / (double) sr;
                    pcm[n] = (short) (Math.sin(2 * Math.PI * 440 * t) * 0.5 * 32767);
                }
                writer.writeAudio(pcm, sr, 1, Math.round(i * 1000f / fps));
            }
            writer.close();

            VideoReader reader = io.openReader(out.getAbsolutePath());
            try {
                final List<Double> brightness = new ArrayList<Double>();
                reader.readFrames(fps, new VideoReader.FrameCallback() {
                    @Override
                    public boolean frame(VideoFrame f) {
                        brightness.add(avgBrightness(f));
                        return brightness.size() < 32;
                    }
                });
                assertTrue(brightness.size() >= 4 && brightness.size() <= 12, "frame count " + brightness.size());
                double firstB = brightness.get(0);
                double lastB = brightness.get(brightness.size() - 1);
                double mn = firstB, mx = firstB;
                for (double b : brightness) {
                    mn = Math.min(mn, b);
                    mx = Math.max(mx, b);
                }
                assertTrue(lastB - firstB > 70, "counting frames not increasing: first=" + firstB + " last=" + lastB);
                assertTrue(mx - mn > 70, "frames not distinct, spread=" + (mx - mn));

                assertTrue(reader.hasAudio());
                AudioBuffer audio = reader.readAudio();
                assertNotNull(audio);
                double rms = rms(audio);
                assertTrue(rms > 0.05 && rms < 0.7, "audio PCM rms out of range: " + rms);
            } finally {
                reader.close();
            }
        } finally {
            out.delete();
        }
    }

    private static double avgBrightness(VideoFrame frame) {
        int[] argb = frame.getARGB();
        long sum = 0;
        int count = 0;
        for (int i = 0; i < argb.length; i += 16) {
            int p = argb[i];
            sum += (((p >> 16) & 0xff) + ((p >> 8) & 0xff) + (p & 0xff)) / 3;
            count++;
        }
        return count == 0 ? 0 : (double) sum / count;
    }

    private static double rms(AudioBuffer audio) {
        int size = audio.getSize();
        float[] f = new float[size];
        audio.copyTo(f);
        double acc = 0;
        for (int i = 0; i < size; i++) {
            acc += (double) f[i] * f[i];
        }
        return size == 0 ? 0 : Math.sqrt(acc / size);
    }

    private static String pickVideoCodec(FFMPEGVideoIO io) {
        String first = null;
        for (VideoCodec c : io.getAvailableEncoders()) {
            if (c.isVideo() && c.isEncoder()) {
                if (VideoIO.CODEC_H264.equals(c.getId())) {
                    return VideoIO.CODEC_H264;
                }
                if (first == null) {
                    first = c.getId();
                }
            }
        }
        return first;
    }

    private static int[] makeFrame(int index) {
        int[] argb = new int[WIDTH * HEIGHT];
        int bar = (index * 6) % WIDTH;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int color = (x >= bar && x < bar + 8) ? 0xffffffff : 0xff202020;
                argb[y * WIDTH + x] = color;
            }
        }
        return argb;
    }

    private static short[] makeTone() {
        short[] pcm = new short[AUDIO_SAMPLES];
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = (short) (Math.sin(2 * Math.PI * 440 * i / SAMPLE_RATE) * 12000);
        }
        return pcm;
    }
}
