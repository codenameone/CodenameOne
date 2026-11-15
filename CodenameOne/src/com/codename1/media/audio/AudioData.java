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
package com.codename1.media.audio;

/// In-memory PCM audio: one float array per channel, samples nominally in the
/// range -1 to 1. This is the Web Audio `AudioBuffer`, named differently so it
/// does not clash with the microphone capture class
/// `com.codename1.media.AudioBuffer`.
///
/// Create one with [BaseAudioContext#createBuffer(int, int, float)] or
/// [#fromInterleaved(float[], int, int, int, float)], play it with an
/// [AudioBufferSourceNode], and get one back from
/// [OfflineAudioContext#startRendering()].
public final class AudioData {
    private final float[][] channels;
    private final int length;
    private final float sampleRate;

    /// Creates silent audio.
    ///
    /// #### Parameters
    ///
    /// - `numberOfChannels`: 1 to 32
    ///
    /// - `length`: the length in frames, at least 1
    ///
    /// - `sampleRate`: frames per second, 3000 to 768000
    public AudioData(int numberOfChannels, int length, float sampleRate) {
        if (numberOfChannels < 1 || numberOfChannels > AudioBus.MAX_CHANNELS) {
            throw new IllegalArgumentException("numberOfChannels must be between 1 and "
                    + AudioBus.MAX_CHANNELS + ": " + numberOfChannels);
        }
        if (length < 1) {
            throw new IllegalArgumentException("length must be at least 1: " + length);
        }
        BaseAudioContext.checkSampleRate(sampleRate);
        this.length = length;
        this.sampleRate = sampleRate;
        channels = new float[numberOfChannels][];
        for (int i = 0; i < numberOfChannels; i++) {
            channels[i] = new float[length];
        }
    }

    /// Creates audio from interleaved samples, the layout `WAVWriter`, the
    /// microphone `AudioBuffer` and most decoders use.
    ///
    /// #### Parameters
    ///
    /// - `interleaved`: the samples
    ///
    /// - `offset`: the index of the first sample
    ///
    /// - `frames`: the number of frames to copy
    ///
    /// - `numberOfChannels`: the number of interleaved channels
    ///
    /// - `sampleRate`: frames per second
    ///
    /// #### Returns
    ///
    /// a new `AudioData` holding a copy of the samples
    public static AudioData fromInterleaved(float[] interleaved, int offset, int frames,
                                            int numberOfChannels, float sampleRate) {
        if (offset < 0 || frames < 1 || offset + frames * numberOfChannels > interleaved.length) {
            throw new IllegalArgumentException("offset/frames out of range");
        }
        AudioData d = new AudioData(numberOfChannels, frames, sampleRate);
        for (int c = 0; c < numberOfChannels; c++) {
            float[] dst = d.channels[c];
            int src = offset + c;
            for (int i = 0; i < frames; i++) {
                dst[i] = interleaved[src];
                src += numberOfChannels;
            }
        }
        return d;
    }

    /// Copies the samples out interleaved, for `WAVWriter` or a port API.
    ///
    /// #### Returns
    ///
    /// a new array of `getLength() * getNumberOfChannels()` samples
    public float[] toInterleaved() {
        int n = channels.length;
        float[] out = new float[length * n];
        for (int c = 0; c < n; c++) {
            float[] src = channels[c];
            int dst = c;
            for (int i = 0; i < length; i++) {
                out[dst] = src[i];
                dst += n;
            }
        }
        return out;
    }

    /// The sample rate.
    ///
    /// #### Returns
    ///
    /// frames per second
    public float getSampleRate() {
        return sampleRate;
    }

    /// The length.
    ///
    /// #### Returns
    ///
    /// the number of frames
    public int getLength() {
        return length;
    }

    /// The duration.
    ///
    /// #### Returns
    ///
    /// the length in seconds
    public double getDuration() {
        return length / (double) sampleRate;
    }

    /// The number of channels.
    ///
    /// #### Returns
    ///
    /// the channel count
    public int getNumberOfChannels() {
        return channels.length;
    }

    /// The live sample array of a channel. Writes to it are heard by any
    /// [AudioBufferSourceNode] that is playing this data.
    ///
    /// #### Parameters
    ///
    /// - `channel`: the channel index
    ///
    /// #### Returns
    ///
    /// the channel's samples, not a copy
    public float[] getChannelData(int channel) {
        checkChannel(channel);
        return channels[channel];
    }

    /// Copies samples of one channel into `destination`.
    ///
    /// #### Parameters
    ///
    /// - `destination`: the array to fill
    ///
    /// - `channel`: the channel index
    ///
    /// - `startInChannel`: the first frame to copy
    public void copyFromChannel(float[] destination, int channel, int startInChannel) {
        checkChannel(channel);
        if (startInChannel < 0 || startInChannel > length) {
            throw new IllegalArgumentException("startInChannel out of range: " + startInChannel);
        }
        int n = Math.min(destination.length, length - startInChannel);
        System.arraycopy(channels[channel], startInChannel, destination, 0, n);
    }

    /// Copies `source` into one channel.
    ///
    /// #### Parameters
    ///
    /// - `source`: the samples
    ///
    /// - `channel`: the channel index
    ///
    /// - `startInChannel`: the first frame to write
    public void copyToChannel(float[] source, int channel, int startInChannel) {
        checkChannel(channel);
        if (startInChannel < 0 || startInChannel > length) {
            throw new IllegalArgumentException("startInChannel out of range: " + startInChannel);
        }
        int n = Math.min(source.length, length - startInChannel);
        System.arraycopy(source, 0, channels[channel], startInChannel, n);
    }

    float[][] channels() {
        return channels;
    }

    private void checkChannel(int channel) {
        if (channel < 0 || channel >= channels.length) {
            throw new IllegalArgumentException("No channel " + channel + " in "
                    + channels.length + " channel audio");
        }
    }
}
