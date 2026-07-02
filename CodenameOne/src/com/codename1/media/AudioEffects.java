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

import com.codename1.util.Simd;

/// Small PCM effects that operate on `AudioBuffer` data.
///
/// The methods in this class are platform-neutral and work on interleaved
/// normalized float PCM samples in the `[-1, 1]` range.  They return new
/// `AudioBuffer` instances and leave the source buffer unchanged.
public final class AudioEffects {
    private static final float DEFAULT_LOW_CUTOFF_HZ = 250.0f;
    private static final float DEFAULT_HIGH_CUTOFF_HZ = 4000.0f;
    private static boolean simdOptimizationsEnabled = Simd.get().isSupported();

    private AudioEffects() {
    }

    /// Indicates whether `AudioEffects` should use SIMD-backed kernels when
    /// the current platform supports them. Scalar fallbacks are always used for
    /// effects whose stateful filters don't map to the current SIMD API.
    ///
    /// #### Returns
    ///
    /// true when SIMD optimizations are enabled.
    public static boolean isSimdOptimizationsEnabled() {
        return simdOptimizationsEnabled;
    }

    /// Enables or disables SIMD-backed `AudioEffects` optimizations.
    ///
    /// This only changes internal implementation choices. It doesn't change
    /// output format or API behavior.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: true to use SIMD where supported, false to force scalar loops.
    public static void setSimdOptimizationsEnabled(boolean enabled) {
        simdOptimizationsEnabled = enabled;
    }

    /// Restores the default SIMD optimization setting for this platform.
    public static void resetSimdOptimizationsEnabled() {
        simdOptimizationsEnabled = Simd.get().isSupported();
    }

    /// Applies gain to a PCM buffer.
    ///
    /// #### Parameters
    ///
    /// - `source`: The source buffer.
    /// - `gain`: Gain multiplier.  `1.0f` preserves level.
    ///
    /// #### Returns
    ///
    /// a new buffer with gain applied and samples clipped to `[-1, 1]`.
    public static AudioBuffer gain(AudioBuffer source, float gain) {
        validateBuffer(source);
        validateFinite(gain, "gain");
        float[] pcm = copySamples(source);
        applyGainInPlace(pcm, gain);
        return buffer(source.getSampleRate(), source.getNumChannels(), pcm);
    }

    /// Normalizes a PCM buffer so its largest absolute sample reaches `targetPeak`.
    ///
    /// #### Parameters
    ///
    /// - `source`: The source buffer.
    /// - `targetPeak`: Desired peak amplitude in the range `[0, 1]`.
    ///
    /// #### Returns
    ///
    /// a new normalized buffer.  Silent buffers are returned unchanged.
    public static AudioBuffer normalize(AudioBuffer source, float targetPeak) {
        validateBuffer(source);
        validatePeak(targetPeak);
        float[] pcm = copySamples(source);
        float peak = 0.0f;
        for (float sample : pcm) {
            peak = Math.max(peak, Math.abs(sample));
        }
        if (peak == 0.0f || peak == targetPeak) {
            return buffer(source.getSampleRate(), source.getNumChannels(), pcm);
        }
        float gain = targetPeak / peak;
        applyGainInPlace(pcm, gain);
        return buffer(source.getSampleRate(), source.getNumChannels(), pcm);
    }

    /// Applies a simple three-band equalizer using default crossover points.
    ///
    /// Low frequencies are shaped below roughly 250 Hz, high frequencies above
    /// roughly 4 kHz, and the middle band contains the remaining signal.
    ///
    /// #### Parameters
    ///
    /// - `source`: The source buffer.
    /// - `lowGain`: Gain for the low band.
    /// - `midGain`: Gain for the middle band.
    /// - `highGain`: Gain for the high band.
    ///
    /// #### Returns
    ///
    /// a new equalized buffer clipped to `[-1, 1]`.
    public static AudioBuffer equalize(AudioBuffer source, float lowGain, float midGain, float highGain) {
        return equalize(source, lowGain, midGain, highGain, DEFAULT_LOW_CUTOFF_HZ, DEFAULT_HIGH_CUTOFF_HZ);
    }

    /// Applies a simple three-band equalizer.
    ///
    /// This is intended for lightweight tone shaping and demos, not as a
    /// mastering-grade parametric equalizer.
    ///
    /// #### Parameters
    ///
    /// - `source`: The source buffer.
    /// - `lowGain`: Gain for samples below `lowCutoffHz`.
    /// - `midGain`: Gain for samples between the crossover bands.
    /// - `highGain`: Gain for samples above `highCutoffHz`.
    /// - `lowCutoffHz`: Low/mid crossover frequency.
    /// - `highCutoffHz`: Mid/high crossover frequency.
    ///
    /// #### Returns
    ///
    /// a new equalized buffer clipped to `[-1, 1]`.
    public static AudioBuffer equalize(AudioBuffer source, float lowGain, float midGain, float highGain,
                                       float lowCutoffHz, float highCutoffHz) {
        validateBuffer(source);
        validateFinite(lowGain, "lowGain");
        validateFinite(midGain, "midGain");
        validateFinite(highGain, "highGain");
        validateCutoffs(source.getSampleRate(), lowCutoffHz, highCutoffHz);
        float[] input = copySamples(source);
        float[] output = new float[input.length];
        int channels = source.getNumChannels();
        float lowAlpha = lowPassAlpha(lowCutoffHz, source.getSampleRate());
        float highAlpha = highPassAlpha(highCutoffHz, source.getSampleRate());
        float[] lowState = new float[channels];
        float[] highState = new float[channels];
        float[] highPrevInput = new float[channels];
        for (int frameOffset = 0; frameOffset < input.length; frameOffset += channels) {
            for (int channel = 0; channel < channels; channel++) {
                int index = frameOffset + channel;
                float sample = input[index];
                lowState[channel] += lowAlpha * (sample - lowState[channel]);
                float high = highAlpha * (highState[channel] + sample - highPrevInput[channel]);
                highState[channel] = high;
                highPrevInput[channel] = sample;
                float low = lowState[channel];
                float mid = sample - low - high;
                output[index] = clamp(low * lowGain + mid * midGain + high * highGain);
            }
        }
        return buffer(source.getSampleRate(), channels, output);
    }

    /// Reduces center-panned content in a stereo buffer.
    ///
    /// This is the classic mid/side "vocal remover" technique.  It only works
    /// when the content to remove is centered and similar in both channels.
    ///
    /// #### Parameters
    ///
    /// - `source`: A stereo source buffer.
    ///
    /// #### Returns
    ///
    /// a new stereo buffer with the center channel reduced.
    public static AudioBuffer removeCenter(AudioBuffer source) {
        return midSide(source, 0.0f, 1.0f);
    }

    /// Isolates center-panned content in a stereo buffer.
    ///
    /// This can be useful for centered dialog or vocals, but it isn't true
    /// speech separation. It preserves only content common to the left and
    /// right channels, so it requires a stereo source where the target content
    /// is actually centered.
    ///
    /// #### Parameters
    ///
    /// - `source`: A stereo source buffer.
    ///
    /// #### Returns
    ///
    /// a new stereo buffer containing the center component.
    public static AudioBuffer isolateCenter(AudioBuffer source) {
        return midSide(source, 1.0f, 0.0f);
    }

    /// Applies mid/side processing to a stereo buffer.
    ///
    /// Use a larger `midGain` and smaller `sideGain` for simple voice/dialog
    /// enhancement, or `midGain=0` and `sideGain=1` for center removal.
    ///
    /// #### Parameters
    ///
    /// - `source`: A stereo source buffer.
    /// - `midGain`: Gain for content common to left and right channels.
    /// - `sideGain`: Gain for content that differs between channels.
    ///
    /// #### Returns
    ///
    /// a new stereo buffer.
    public static AudioBuffer midSide(AudioBuffer source, float midGain, float sideGain) {
        validateBuffer(source);
        if (source.getNumChannels() != 2) {
            throw new IllegalArgumentException("midSide() requires a stereo buffer");
        }
        validateFinite(midGain, "midGain");
        validateFinite(sideGain, "sideGain");
        float[] input = copySamples(source);
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i += 2) {
            float left = input[i];
            float right = input[i + 1];
            float mid = (left + right) * 0.5f;
            float side = (left - right) * 0.5f;
            output[i] = clamp(mid * midGain + side * sideGain);
            output[i + 1] = clamp(mid * midGain - side * sideGain);
        }
        return buffer(source.getSampleRate(), 2, output);
    }

    private static float[] copySamples(AudioBuffer source) {
        float[] pcm = allocFloat(source.getSize());
        source.copyTo(pcm);
        return pcm;
    }

    private static float[] allocFloat(int size) {
        if (size >= 16) {
            return Simd.get().allocFloat(size);
        }
        return new float[size];
    }

    private static boolean shouldUseSimd(int size) {
        return simdOptimizationsEnabled && Simd.get().isSupported() && size >= 16;
    }

    private static void applyGainInPlace(float[] pcm, float gain) {
        if (shouldUseSimd(pcm.length)) {
            Simd simd = Simd.get();
            float[] gains = simd.allocFloat(pcm.length);
            float[] multiplied = simd.allocFloat(pcm.length);
            for (int i = 0; i < gains.length; i++) {
                gains[i] = gain;
            }
            simd.mul(pcm, gains, multiplied, 0, pcm.length);
            simd.clamp(multiplied, pcm, -1.0f, 1.0f, 0, pcm.length);
            return;
        }
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = clamp(pcm[i] * gain);
        }
    }

    private static AudioBuffer buffer(int sampleRate, int channels, float[] pcm) {
        AudioBuffer out = new AudioBuffer(pcm.length);
        out.copyFrom(sampleRate, channels, pcm);
        return out;
    }

    private static void validateBuffer(AudioBuffer source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (source.getSampleRate() <= 0) {
            throw new IllegalArgumentException("source sampleRate must be greater than zero");
        }
        if (source.getNumChannels() <= 0) {
            throw new IllegalArgumentException("source numChannels must be greater than zero");
        }
        if (source.getSize() % source.getNumChannels() != 0) {
            throw new IllegalArgumentException("source sample count must be a multiple of numChannels");
        }
    }

    private static void validateFinite(float value, String name) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void validatePeak(float targetPeak) {
        validateFinite(targetPeak, "targetPeak");
        if (targetPeak < 0.0f || targetPeak > 1.0f) {
            throw new IllegalArgumentException("targetPeak must be between 0 and 1");
        }
    }

    private static void validateCutoffs(int sampleRate, float lowCutoffHz, float highCutoffHz) {
        validateFinite(lowCutoffHz, "lowCutoffHz");
        validateFinite(highCutoffHz, "highCutoffHz");
        float nyquist = sampleRate * 0.5f;
        if (lowCutoffHz <= 0.0f || highCutoffHz <= lowCutoffHz || highCutoffHz >= nyquist) {
            throw new IllegalArgumentException("Cutoffs must satisfy 0 < lowCutoffHz < highCutoffHz < Nyquist");
        }
    }

    private static float lowPassAlpha(float cutoffHz, int sampleRate) {
        float dt = 1.0f / sampleRate;
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
        return dt / (rc + dt);
    }

    private static float highPassAlpha(float cutoffHz, int sampleRate) {
        float dt = 1.0f / sampleRate;
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
        return rc / (rc + dt);
    }

    private static float clamp(float sample) {
        if (sample > 1.0f) {
            return 1.0f;
        }
        if (sample < -1.0f) {
            return -1.0f;
        }
        return sample;
    }
}
