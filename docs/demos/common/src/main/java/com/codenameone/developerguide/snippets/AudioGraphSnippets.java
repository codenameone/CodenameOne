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
package com.codenameone.developerguide.snippets;

import com.codename1.media.AudioBuffer;
import com.codename1.media.MediaManager;
import com.codename1.media.audio.AnalyserNode;
import com.codename1.media.audio.AudioContext;
import com.codename1.media.audio.AudioData;
import com.codename1.media.audio.AudioProcessor;
import com.codename1.media.audio.AudioProcessorNode;
import com.codename1.media.audio.BiquadFilterNode;
import com.codename1.media.audio.BiquadFilterType;
import com.codename1.media.audio.GainNode;
import com.codename1.media.audio.MicrophoneSourceNode;
import com.codename1.media.audio.OfflineAudioContext;
import com.codename1.media.audio.OscillatorNode;
import com.codename1.media.audio.PannerNode;
import com.codename1.media.audio.PanningModel;

/// Snippets for the audio graph, included by the developer guide's media
/// chapter.
public class AudioGraphSnippets {
    void beep() {
        // tag::audio-graph-beep[]
        AudioContext ctx = AudioContext.create();
        if (ctx == null) {
            return; // no real-time output on this platform
        }
        OscillatorNode osc = ctx.createOscillator();
        osc.getFrequency().setValue(440);
        GainNode env = ctx.createGain();
        double now = ctx.getCurrentTime();
        env.getGain().setValueAtTime(0, now);
        env.getGain().linearRampToValueAtTime(0.5f, now + 0.02);
        env.getGain().exponentialRampToValueAtTime(0.001f, now + 1);
        BiquadFilterNode filter = ctx.createBiquadFilter();
        filter.setType(BiquadFilterType.LOWPASS);
        filter.getFrequency().setValue(1200);
        osc.connect(filter).connect(env).connect(ctx.getDestination());
        osc.start(now);
        osc.stop(now + 1);
        // end::audio-graph-beep[]
    }

    void spatial(AudioContext ctx, AudioData footsteps) {
        // tag::audio-graph-spatial[]
        PannerNode panner = ctx.createPanner();
        panner.setPanningModel(PanningModel.HRTF);
        panner.getPositionX().setValue(3);
        panner.getPositionZ().setValue(-1);
        ctx.getListener().getPositionX().setValue(0);
        com.codename1.media.audio.AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(footsteps);
        src.setLoop(true);
        src.connect(panner).connect(ctx.getDestination());
        src.start();
        // end::audio-graph-spatial[]
    }

    float level(AnalyserNode analyser) {
        // tag::audio-graph-analyser[]
        float[] spectrum = new float[analyser.getFrequencyBinCount()];
        analyser.getFloatFrequencyData(spectrum);
        float loudest = -1000;
        for (float db : spectrum) {
            loudest = Math.max(loudest, db);
        }
        // end::audio-graph-analyser[]
        return loudest;
    }

    AudioProcessorNode bitCrusher(AudioContext ctx) {
        // tag::audio-graph-processor[]
        AudioProcessorNode crusher = ctx.createAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(float[][][] in, float[][][] out, float[][] params) {
                for (int c = 0; c < out[0].length && c < in[0].length; c++) {
                    for (int i = 0; i < 128; i++) {
                        out[0][c][i] = (float) Math.floor(in[0][c][i] * 8 + 0.5) / 8f;
                    }
                }
                return true;
            }
        }, 2);
        // end::audio-graph-processor[]
        return crusher;
    }

    void microphone(AudioContext ctx) {
        // tag::audio-graph-microphone[]
        if (!ctx.isMicrophoneSourceSupported()) {
            return;
        }
        AudioBuffer capture = MediaManager.getAudioBuffer("mic", true, 4096);
        MicrophoneSourceNode mic = ctx.createMicrophoneSource(capture);
        AnalyserNode meter = ctx.createAnalyser();
        mic.connect(meter);
        // start a recorder built with redirectToAudioBuffer(true) and path "mic"
        // end::audio-graph-microphone[]
    }

    AudioData offline(byte[] wav) {
        // tag::audio-graph-offline[]
        OfflineAudioContext offline = new OfflineAudioContext(2, 48000 * 5, 48000);
        AudioData clip = offline.decodeAudioData(wav);
        if (clip == null) {
            return null; // not a WAV file
        }
        com.codename1.media.audio.AudioBufferSourceNode src = offline.createBufferSource();
        src.setBuffer(clip);
        com.codename1.media.audio.ConvolverNode reverb = offline.createConvolver();
        reverb.setBuffer(clip);
        src.connect(reverb).connect(offline.getDestination());
        src.start();
        AudioData rendered = offline.startRendering();
        // end::audio-graph-offline[]
        return rendered;
    }
}
