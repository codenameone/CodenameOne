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

import java.util.ArrayList;

/// Mixes multiple PCM tracks into a single `AudioBuffer`.
///
/// `AudioMixer` is a platform-neutral helper for building a sample-accurate
/// PCM timeline.  All tracks are locked to the mixer sample rate and channel
/// count.  Track offsets are converted to sample frames on that clock, and the
/// mixed output uses interleaved float PCM samples in the same format as
/// `AudioBuffer` and `WAVWriter`.
///
/// ```java
/// AudioMixer mixer = new AudioMixer(44100, 2);
/// mixer.addTrack(musicBuffer, 0, 0.5f);
/// mixer.addTrack(effectBuffer, 250, 1.0f);
/// AudioBuffer mixed = mixer.mix();
/// float[] pcm = new float[mixed.getSize()];
/// mixed.copyTo(pcm);
/// wavWriter.write(pcm, 0, mixed.getSize());
/// ```
public class AudioMixer {
    private final ArrayList<Track> tracks = new ArrayList<Track>();
    private final int sampleRate;
    private final int numChannels;

    /// Creates a new mixer locked to a single sample clock.
    ///
    /// #### Parameters
    ///
    /// - `sampleRate`: The sample rate in frames per second.  E.g. 44100.
    /// - `numChannels`: The number of interleaved channels.  E.g. 1 or 2.
    public AudioMixer(int sampleRate, int numChannels) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be greater than zero");
        }
        if (numChannels <= 0) {
            throw new IllegalArgumentException("numChannels must be greater than zero");
        }
        this.sampleRate = sampleRate;
        this.numChannels = numChannels;
    }

    /// Gets the sample rate used by this mixer.
    ///
    /// #### Returns
    ///
    /// the sample rate in frames per second.
    public int getSampleRate() {
        return sampleRate;
    }

    /// Gets the number of interleaved channels used by this mixer.
    ///
    /// #### Returns
    ///
    /// the number of channels.
    public int getNumChannels() {
        return numChannels;
    }

    /// Gets the number of tracks currently in the mixer.
    ///
    /// #### Returns
    ///
    /// the number of tracks.
    public int getTrackCount() {
        return tracks.size();
    }

    /// Removes all tracks from this mixer.
    public void clear() {
        tracks.clear();
    }

    /// Adds an `AudioBuffer` track at the given millisecond offset.
    ///
    /// The source samples are copied when the track is added so the source
    /// buffer can be reused or modified afterwards.  The offset is rounded to
    /// the nearest sample frame on the mixer clock.
    ///
    /// #### Parameters
    ///
    /// - `source`: Source PCM buffer.  Its sample rate and channel count must
    /// match the mixer.
    /// - `offsetMs`: Offset from the start of the timeline in milliseconds.
    /// - `gain`: Gain multiplier for this track.  `1.0f` preserves level.
    ///
    /// #### Returns
    ///
    /// this mixer.
    public AudioMixer addTrack(AudioBuffer source, long offsetMs, float gain) {
        return addTrackAtFrame(source, framesFromMilliseconds(offsetMs), gain);
    }

    /// Adds an `AudioBuffer` track at an exact sample-frame offset.
    ///
    /// Use this method when the track position is already known in frames and
    /// no millisecond conversion should be applied.
    ///
    /// #### Parameters
    ///
    /// - `source`: Source PCM buffer.  Its sample rate and channel count must
    /// match the mixer.
    /// - `offsetFrame`: Offset from the start of the timeline in sample frames.
    /// - `gain`: Gain multiplier for this track.  `1.0f` preserves level.
    ///
    /// #### Returns
    ///
    /// this mixer.
    public AudioMixer addTrackAtFrame(AudioBuffer source, int offsetFrame, float gain) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (source.getSampleRate() != sampleRate) {
            throw new IllegalArgumentException("Track sampleRate " + source.getSampleRate() + " does not match mixer sampleRate " + sampleRate);
        }
        if (source.getNumChannels() != numChannels) {
            throw new IllegalArgumentException("Track numChannels " + source.getNumChannels() + " does not match mixer numChannels " + numChannels);
        }
        float[] pcm = new float[source.getSize()];
        source.copyTo(pcm);
        return addTrackAtFrame(pcm, 0, source.getSize(), offsetFrame, gain);
    }

    /// Adds an interleaved PCM track at the given millisecond offset.
    ///
    /// The source samples are copied when the track is added.  The offset is
    /// rounded to the nearest sample frame on the mixer clock.
    ///
    /// #### Parameters
    ///
    /// - `pcmData`: Interleaved PCM samples.
    /// - `offsetMs`: Offset from the start of the timeline in milliseconds.
    /// - `gain`: Gain multiplier for this track.  `1.0f` preserves level.
    ///
    /// #### Returns
    ///
    /// this mixer.
    public AudioMixer addTrack(float[] pcmData, long offsetMs, float gain) {
        if (pcmData == null) {
            throw new IllegalArgumentException("pcmData must not be null");
        }
        return addTrack(pcmData, 0, pcmData.length, offsetMs, gain);
    }

    /// Adds an interleaved PCM track range at the given millisecond offset.
    ///
    /// The source samples are copied when the track is added.  The offset is
    /// rounded to the nearest sample frame on the mixer clock.
    ///
    /// #### Parameters
    ///
    /// - `pcmData`: Interleaved PCM samples.
    /// - `offset`: Offset in `pcmData` to start copying from.
    /// - `len`: Number of samples to copy.
    /// - `offsetMs`: Offset from the start of the timeline in milliseconds.
    /// - `gain`: Gain multiplier for this track.  `1.0f` preserves level.
    ///
    /// #### Returns
    ///
    /// this mixer.
    public AudioMixer addTrack(float[] pcmData, int offset, int len, long offsetMs, float gain) {
        return addTrackAtFrame(pcmData, offset, len, framesFromMilliseconds(offsetMs), gain);
    }

    /// Adds an interleaved PCM track range at an exact sample-frame offset.
    ///
    /// #### Parameters
    ///
    /// - `pcmData`: Interleaved PCM samples.
    /// - `offset`: Offset in `pcmData` to start copying from.
    /// - `len`: Number of samples to copy.
    /// - `offsetFrame`: Offset from the start of the timeline in sample frames.
    /// - `gain`: Gain multiplier for this track.  `1.0f` preserves level.
    ///
    /// #### Returns
    ///
    /// this mixer.
    public AudioMixer addTrackAtFrame(float[] pcmData, int offset, int len, int offsetFrame, float gain) {
        validatePcmRange(pcmData, offset, len);
        validateOffsetFrame(offsetFrame);
        validateGain(gain);
        int frameCount = len / numChannels;
        if (offsetFrame > Integer.MAX_VALUE - frameCount) {
            throw new IllegalArgumentException("Track offset is too large for this mixer");
        }
        if (offsetFrame + frameCount > Integer.MAX_VALUE / numChannels) {
            throw new IllegalArgumentException("Track output is too large for this mixer");
        }
        float[] pcm = new float[len];
        System.arraycopy(pcmData, offset, pcm, 0, len);
        tracks.add(new Track(pcm, offsetFrame, frameCount, gain));
        return this;
    }

    /// Gets the number of sample frames in the mixed output.
    ///
    /// #### Returns
    ///
    /// the mixed timeline length in sample frames.
    public int getOutputFrameCount() {
        int outputFrameCount = 0;
        for (Track track : tracks) {
            outputFrameCount = Math.max(outputFrameCount, track.offsetFrame + track.frameCount);
        }
        return outputFrameCount;
    }

    /// Gets the number of interleaved samples in the mixed output.
    ///
    /// #### Returns
    ///
    /// the mixed timeline length in samples.
    public int getOutputSize() {
        return getOutputFrameCount() * numChannels;
    }

    /// Mixes all tracks into a new `AudioBuffer`.
    ///
    /// The output is clipped to the normalized PCM range `[-1, 1]` so it can
    /// be passed directly to `WAVWriter`.
    ///
    /// #### Returns
    ///
    /// a new `AudioBuffer` containing the mixed PCM timeline.
    public AudioBuffer mix() {
        int outputSize = getOutputSize();
        float[] output = new float[outputSize];
        for (Track track : tracks) {
            int destOffset = track.offsetFrame * numChannels;
            for (int i = 0; i < track.pcm.length; i++) {
                output[destOffset + i] += track.pcm[i] * track.gain;
            }
        }
        for (int i = 0; i < output.length; i++) {
            output[i] = clamp(output[i]);
        }
        AudioBuffer out = new AudioBuffer(output.length);
        out.copyFrom(sampleRate, numChannels, output);
        return out;
    }

    private int framesFromMilliseconds(long offsetMs) {
        if (offsetMs < 0) {
            throw new IllegalArgumentException("offsetMs must not be negative");
        }
        if (offsetMs > (Long.MAX_VALUE - 500L) / sampleRate) {
            throw new IllegalArgumentException("offsetMs is too large for this mixer");
        }
        long frames = (offsetMs * (long) sampleRate + 500L) / 1000L;
        if (frames > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("offsetMs is too large for this mixer");
        }
        return (int) frames;
    }

    private void validatePcmRange(float[] pcmData, int offset, int len) {
        if (pcmData == null) {
            throw new IllegalArgumentException("pcmData must not be null");
        }
        if (offset < 0 || len < 0 || offset > pcmData.length - len) {
            throw new IllegalArgumentException("Invalid pcmData range");
        }
        if (len % numChannels != 0) {
            throw new IllegalArgumentException("Track sample length must be a multiple of numChannels");
        }
    }

    private void validateOffsetFrame(int offsetFrame) {
        if (offsetFrame < 0) {
            throw new IllegalArgumentException("offsetFrame must not be negative");
        }
    }

    private void validateGain(float gain) {
        if (Float.isNaN(gain) || Float.isInfinite(gain)) {
            throw new IllegalArgumentException("gain must be finite");
        }
    }

    private float clamp(float sample) {
        if (sample > 1.0f) {
            return 1.0f;
        }
        if (sample < -1.0f) {
            return -1.0f;
        }
        return sample;
    }

    private static class Track {
        private final float[] pcm;
        private final int offsetFrame;
        private final int frameCount;
        private final float gain;

        Track(float[] pcm, int offsetFrame, int frameCount, float gain) {
            this.pcm = pcm;
            this.offsetFrame = offsetFrame;
            this.frameCount = frameCount;
            this.gain = gain;
        }
    }
}
