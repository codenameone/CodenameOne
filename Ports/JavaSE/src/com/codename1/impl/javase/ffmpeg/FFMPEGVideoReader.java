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
import com.codename1.media.VideoFrame;
import com.codename1.media.VideoReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/// ffmpeg backed `com.codename1.media.VideoReader`. Each operation spawns a short lived
/// ffmpeg process that emits raw RGBA frames or 16 bit PCM on stdout, which we parse
/// into `VideoFrame`s / an `AudioBuffer`. Frame accurate seeking uses ffmpeg input
/// seeking; constant frame rate resampling uses ffmpeg's {@code fps} filter.
class FFMPEGVideoReader extends VideoReader {
    private final String source;
    private final FFMPEGSupport.ProbeInfo probe;

    FFMPEGVideoReader(String filePath) throws IOException {
        this.source = FFMPEGSupport.normalizeSource(filePath);
        this.probe = FFMPEGSupport.probe(source);
    }

    @Override
    public int getWidth() {
        return probe.hasVideo ? probe.width : -1;
    }

    @Override
    public int getHeight() {
        return probe.hasVideo ? probe.height : -1;
    }

    @Override
    public long getDurationMillis() {
        return probe.durationMillis;
    }

    @Override
    public float getFrameRate() {
        return (float) probe.frameRate;
    }

    @Override
    public boolean hasVideo() {
        return probe.hasVideo;
    }

    @Override
    public boolean hasAudio() {
        return probe.hasAudio;
    }

    @Override
    public int getAudioSampleRate() {
        return probe.hasAudio ? probe.audioSampleRate : -1;
    }

    @Override
    public int getAudioChannels() {
        return probe.hasAudio ? probe.audioChannels : -1;
    }

    @Override
    public VideoFrame frameAt(long millis) throws IOException {
        if (!probe.hasVideo) {
            return null;
        }
        int w = probe.width;
        int h = probe.height;
        if (w <= 0 || h <= 0) {
            throw new IOException("video dimensions are unknown");
        }
        List<String> command = new ArrayList<String>();
        command.add(FFMPEGSupport.ffmpeg());
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("error");
        command.add("-ss");
        command.add(String.valueOf(Math.max(0, millis) / 1000.0));
        command.add("-i");
        command.add(source);
        command.add("-frames:v");
        command.add("1");
        command.add("-an");
        command.add("-f");
        command.add("rawvideo");
        command.add("-pix_fmt");
        command.add("rgba");
        command.add("-");
        Process process = FFMPEGSupport.startWithDrain(command);
        try {
            InputStream in = process.getInputStream();
            byte[] frame = new byte[w * h * 4];
            if (!FFMPEGSupport.readFully(in, frame)) {
                return null;
            }
            return new VideoFrame(FFMPEGSupport.rgbaToArgb(frame, w * h), w, h, millis);
        } finally {
            FFMPEGSupport.destroyQuietly(process);
        }
    }

    @Override
    public void readFrames(float fps, FrameCallback callback) throws IOException {
        if (!probe.hasVideo) {
            return;
        }
        if (fps <= 0f) {
            throw new IllegalArgumentException("fps must be positive");
        }
        int w = probe.width;
        int h = probe.height;
        if (w <= 0 || h <= 0) {
            throw new IOException("video dimensions are unknown");
        }
        List<String> command = new ArrayList<String>();
        command.add(FFMPEGSupport.ffmpeg());
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("error");
        command.add("-i");
        command.add(source);
        command.add("-an");
        command.add("-vf");
        command.add("fps=" + fps);
        command.add("-f");
        command.add("rawvideo");
        command.add("-pix_fmt");
        command.add("rgba");
        command.add("-");
        Process process = FFMPEGSupport.startWithDrain(command);
        try {
            InputStream in = process.getInputStream();
            byte[] frame = new byte[w * h * 4];
            long index = 0;
            while (FFMPEGSupport.readFully(in, frame)) {
                long pts = Math.round(index * 1000.0 / fps);
                VideoFrame vf = new VideoFrame(FFMPEGSupport.rgbaToArgb(frame, w * h), w, h, pts);
                index++;
                if (!callback.frame(vf)) {
                    break;
                }
            }
        } finally {
            FFMPEGSupport.destroyQuietly(process);
        }
    }

    @Override
    public AudioBuffer readAudio() throws IOException {
        if (!probe.hasAudio) {
            return null;
        }
        int channels = probe.audioChannels > 0 ? probe.audioChannels : 2;
        int sampleRate = probe.audioSampleRate > 0 ? probe.audioSampleRate : 44100;
        List<String> command = new ArrayList<String>();
        command.add(FFMPEGSupport.ffmpeg());
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("error");
        command.add("-i");
        command.add(source);
        command.add("-vn");
        command.add("-f");
        command.add("s16le");
        command.add("-acodec");
        command.add("pcm_s16le");
        command.add("-ac");
        command.add(String.valueOf(channels));
        command.add("-ar");
        command.add(String.valueOf(sampleRate));
        command.add("-");
        Process process = FFMPEGSupport.startWithDrain(command);
        byte[] pcm;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (InputStream in = process.getInputStream()) {
                FFMPEGSupport.copyStream(in, bos);
            }
            pcm = bos.toByteArray();
        } finally {
            FFMPEGSupport.destroyQuietly(process);
        }
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
        buffer.copyFrom(sampleRate, channels, samples);
        return buffer;
    }

    @Override
    public void close() throws IOException {
        // Each operation spawns and tears down its own process; nothing persistent to release.
    }
}
