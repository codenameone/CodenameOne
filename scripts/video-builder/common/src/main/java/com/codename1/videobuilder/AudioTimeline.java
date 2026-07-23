/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.videobuilder;

import com.codename1.media.AudioBuffer;
import com.codename1.media.AudioMixer;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Sample-accurate, bounded-memory audio timeline. */
final class AudioTimeline {
    static final int SAMPLE_RATE = 48_000;
    static final int CHANNELS = 2;
    static final int CHUNK_MS = 10_000;
    /** One decibel of encode headroom to protect AAC from full-scale PCM peaks. */
    static final float OUTPUT_GAIN = 0.8912509f;
    private final List<Clip> clips = new ArrayList<>();
    private final long durationMs;

    AudioTimeline(VideoScript script, List<NarrationService.PreparedNarration> narration) throws Exception {
        long effectiveDurationMs = script.getDurationMs();
        for (NarrationService.PreparedNarration item : narration) {
            clips.add(new Clip(item.atMs(), item.audio().samples(), item.gain(), false));
            effectiveDurationMs = Math.max(effectiveDurationMs, item.atMs() + item.durationMs());
        }
        VideoIO io = null;
        for (VideoScript.AudioTrack track : script.getAudio()) {
            if (io == null) io = VideoIO.getVideoIO();
            if (io == null) throw new IOException("VideoIO is required to decode audio track " + track.path());
            try (ReaderClose close = new ReaderClose(io.openReader(script.getProjectDirectory().resolve(track.path()).toString()))) {
                AudioBuffer source = close.reader.readAudio();
                clips.add(new Clip(track.atMs(), normalize(source), track.gain(), track.loop()));
            }
        }
        durationMs = effectiveDurationMs;
    }

    boolean isEmpty() { return clips.isEmpty(); }
    long durationMs() { return durationMs; }

    void writeTo(VideoWriter writer) throws IOException {
        for (long chunkStartMs = 0; chunkStartMs < durationMs; chunkStartMs += CHUNK_MS) {
            writer.writeAudio(mixChunk(chunkStartMs), chunkStartMs);
        }
    }

    void writeRaw(Path path) throws IOException {
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(path))) {
            for (long chunkStartMs = 0; chunkStartMs < durationMs; chunkStartMs += CHUNK_MS) {
                AudioBuffer mixed = mixChunk(chunkStartMs);
                float[] samples = new float[mixed.getSize()];
                mixed.copyTo(samples);
                byte[] pcm = new byte[samples.length * 2];
                int offset = 0;
                for (float sample : samples) {
                    short value = pcmSample(sample);
                    pcm[offset++] = (byte) (value & 0xff);
                    pcm[offset++] = (byte) ((value >> 8) & 0xff);
                }
                output.write(pcm);
            }
        }
    }

    static short pcmSample(float sample) {
        float clipped = Math.max(-1f, Math.min(1f, sample * OUTPUT_GAIN));
        return (short) Math.round(clipped * 32767f);
    }

    private AudioBuffer mixChunk(long chunkStartMs) {
        long chunkEndMs = Math.min(durationMs, chunkStartMs + CHUNK_MS);
        int frames = (int) ((chunkEndMs - chunkStartMs) * SAMPLE_RATE / 1000L);
        AudioMixer mixer = new AudioMixer(SAMPLE_RATE, CHANNELS);
        for (Clip clip : clips) addOverlap(mixer, clip, chunkStartMs, chunkEndMs);
        if (mixer.getTrackCount() == 0) mixer.addTrack(new float[frames * CHANNELS], 0, 1f);
        else mixer.addTrackAtFrame(new float[CHANNELS], 0, CHANNELS, Math.max(0, frames - 1), 0f);
        return mixer.mix();
    }

    private void addOverlap(AudioMixer mixer, Clip clip, long chunkStartMs, long chunkEndMs) {
        long clipDurationMs = clip.samples.length / CHANNELS * 1000L / SAMPLE_RATE;
        if (clipDurationMs <= 0) return;
        long occurrence = clip.atMs;
        if (clip.loop && occurrence + clipDurationMs <= chunkStartMs) {
            long skipped = (chunkStartMs - occurrence) / clipDurationMs;
            occurrence += skipped * clipDurationMs;
            if (occurrence + clipDurationMs <= chunkStartMs) occurrence += clipDurationMs;
        }
        do {
            long occurrenceEnd = occurrence + clipDurationMs;
            long start = Math.max(chunkStartMs, occurrence);
            long end = Math.min(chunkEndMs, occurrenceEnd);
            if (start < end) {
                int sourceFrame = (int) ((start - occurrence) * SAMPLE_RATE / 1000L);
                int frames = (int) ((end - start) * SAMPLE_RATE / 1000L);
                int destinationFrame = (int) ((start - chunkStartMs) * SAMPLE_RATE / 1000L);
                mixer.addTrackAtFrame(clip.samples, sourceFrame * CHANNELS,
                        Math.min(frames * CHANNELS, clip.samples.length - sourceFrame * CHANNELS),
                        destinationFrame, clip.gain);
            }
            occurrence += clipDurationMs;
        } while (clip.loop && occurrence < chunkEndMs);
    }

    private static float[] normalize(AudioBuffer source) {
        float[] samples = new float[source.getSize()];
        source.copyTo(samples);
        return new WavPcm.AudioData(source.getSampleRate(), source.getNumChannels(), samples)
                .normalized(SAMPLE_RATE, CHANNELS).samples();
    }

    private record Clip(long atMs, float[] samples, float gain, boolean loop) { }
    private static final class ReaderClose implements AutoCloseable {
        final VideoReader reader;
        ReaderClose(VideoReader reader) { this.reader = reader; }
        public void close() throws IOException { reader.close(); }
    }
}
