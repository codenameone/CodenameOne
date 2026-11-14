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

import com.codename1.util.MathUtil;

/// A band-limited periodic waveform, the Web Audio `OscillatorNode`. Mono.
///
/// ```java
/// OfflineAudioContext audio = new OfflineAudioContext(2, 44100, 44100);
/// OscillatorNode osc = audio.createOscillator();
/// osc.setType(OscillatorType.SAWTOOTH);
/// osc.getFrequency().setValue(220);
/// osc.connect(audio.getDestination());
/// osc.start();
/// osc.stop(audio.getCurrentTime() + 1);
/// ```
public final class OscillatorNode extends AudioScheduledSourceNode {
    private final AudioParam frequency;
    private final AudioParam detune;
    private OscillatorType type = OscillatorType.SINE;

    // ---- render side ----
    private PeriodicWave rWave;
    private double rPhase;

    OscillatorNode(BaseAudioContext context) {
        super(context, 1);
        float nyquist = context.getSampleRate() / 2;
        frequency = new AudioParam(context, "frequency", 440f, -nyquist, nyquist,
                AutomationRate.A_RATE, false);
        detune = new AudioParam(context, "detune", 0f, -153600f, 153600f, AutomationRate.A_RATE, false);
        rWave = context.standardWave(OscillatorType.SINE);
    }

    /// The frequency in Hz, 440 by default; an audio rate parameter.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getFrequency() {
        return frequency;
    }

    /// A detune in cents applied on top of the frequency, 0 by default; an
    /// audio rate parameter.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getDetune() {
        return detune;
    }

    /// The waveform.
    ///
    /// #### Returns
    ///
    /// the type, `CUSTOM` after [#setPeriodicWave(PeriodicWave)]
    public OscillatorType getType() {
        return type;
    }

    /// Selects a standard waveform.
    ///
    /// #### Parameters
    ///
    /// - `type`: any type but `CUSTOM`
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for `CUSTOM`; use [#setPeriodicWave(PeriodicWave)]
    public void setType(OscillatorType type) {
        if (type == null || type == OscillatorType.CUSTOM) {
            throw new IllegalArgumentException("Use setPeriodicWave for a custom waveform");
        }
        this.type = type;
        publishWave(context.standardWave(type));
    }

    /// Plays a custom waveform; the type becomes `CUSTOM`.
    ///
    /// #### Parameters
    ///
    /// - `wave`: the waveform
    public void setPeriodicWave(PeriodicWave wave) {
        if (wave == null) {
            throw new IllegalArgumentException("wave is null");
        }
        type = OscillatorType.CUSTOM;
        publishWave(wave);
    }

    private void publishWave(final PeriodicWave w) {
        context.post(new Runnable() {
            @Override
            public void run() {
                rWave = w;
            }
        });
    }

    @Override
    void render(long q, int from, int to) {
        float[] f = frequency.compute(q);
        float[] dt = detune.compute(q);
        boolean fixed = frequency.isConstant() && detune.isConstant();
        float sr = context.getSampleRate();
        double nyquist = sr / 2.0;
        float[] d = outBuses[0].data[0];
        double phase = rPhase;
        double freq = 0;
        float[] table = null;
        for (int i = from; i < to; i++) {
            if (i == from || !fixed) {
                freq = f[i];
                if (dt[i] != 0f) {
                    freq *= MathUtil.pow(2, dt[i] / 1200.0);
                }
                table = rWave.tableFor(freq, nyquist);
            }
            if (table != null) {
                double pos = phase * PeriodicWave.TABLE_SIZE;
                int idx = (int) pos;
                double frac = pos - idx;
                d[i] = (float) (table[idx] + (table[idx + 1] - table[idx]) * frac);
            }
            phase += freq / sr;
            phase -= Math.floor(phase);
        }
        rPhase = phase;
    }
}
