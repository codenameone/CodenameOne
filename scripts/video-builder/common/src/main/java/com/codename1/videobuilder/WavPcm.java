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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** WAV reader and deterministic linear PCM normalizer. */
final class WavPcm {
    private WavPcm() { }

    static AudioData read(Path path) throws Exception {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat original = source.getFormat();
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, original.getSampleRate(), 16,
                    original.getChannels(), original.getChannels() * 2, original.getSampleRate(), false);
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, source);
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = decoded.read(buffer)) >= 0) if (count > 0) bytes.write(buffer, 0, count);
                byte[] raw = bytes.toByteArray();
                float[] samples = new float[raw.length / 2];
                for (int i = 0, s = 0; s < samples.length; i += 2, s++) {
                    short value = (short) ((raw[i] & 0xff) | (raw[i + 1] << 8));
                    samples[s] = value / 32768f;
                }
                return new AudioData(Math.round(pcm.getSampleRate()), pcm.getChannels(), samples);
            }
        } catch (IllegalArgumentException ex) {
            throw new IOException("Unsupported WAV encoding: " + path, ex);
        }
    }

    record AudioData(int sampleRate, int channels, float[] samples) {
        int frameCount() { return samples.length / channels; }

        AudioData normalized(int targetRate, int targetChannels) {
            if (sampleRate == targetRate && channels == targetChannels) return this;
            int sourceFrames = frameCount();
            int targetFrames = Math.max(1, Math.round(sourceFrames * (float) targetRate / sampleRate));
            float[] out = new float[targetFrames * targetChannels];
            for (int frame = 0; frame < targetFrames; frame++) {
                float sourcePosition = frame * (float) sampleRate / targetRate;
                int first = Math.min(sourceFrames - 1, (int) sourcePosition);
                int second = Math.min(sourceFrames - 1, first + 1);
                float fraction = sourcePosition - first;
                for (int channel = 0; channel < targetChannels; channel++) {
                    int sourceChannel = channels == 1 ? 0 : Math.min(channel, channels - 1);
                    float a = samples[first * channels + sourceChannel];
                    float b = samples[second * channels + sourceChannel];
                    out[frame * targetChannels + channel] = a + (b - a) * fraction;
                }
            }
            return new AudioData(targetRate, targetChannels, out);
        }
    }
}
