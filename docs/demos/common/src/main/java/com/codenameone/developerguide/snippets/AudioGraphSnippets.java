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
import com.codename1.media.audio.AudioBufferSourceNode;
import com.codename1.media.audio.AudioContext;
import com.codename1.media.audio.AudioData;
import com.codename1.media.audio.AudioProcessor;
import com.codename1.media.audio.AudioProcessorNode;
import com.codename1.media.audio.BiquadFilterNode;
import com.codename1.media.audio.BiquadFilterType;
import com.codename1.media.audio.ConvolverNode;
import com.codename1.media.audio.GainNode;
import com.codename1.media.audio.MicrophoneSourceNode;
import com.codename1.media.audio.OfflineAudioContext;
import com.codename1.media.audio.OscillatorNode;
import com.codename1.media.audio.PannerNode;
import com.codename1.media.audio.PanningModel;

/// Snippets for the audio graph, included by the developer guide's media
/// chapter. Each tagged block declares what it uses, so it also runs as it
/// stands in the Playground, whose script already has a variable named `ctx`.
public class AudioGraphSnippets {
    void beep() {
        // tag::audio-graph-beep[]
        AudioContext audio = AudioContext.create();
        if (audio != null) { // null where the platform has no real-time output
            OscillatorNode osc = audio.createOscillator();
            osc.getFrequency().setValue(440);
            GainNode env = audio.createGain();
            double now = audio.getCurrentTime();
            env.getGain().setValueAtTime(0, now);
            env.getGain().linearRampToValueAtTime(0.5f, now + 0.02);
            env.getGain().exponentialRampToValueAtTime(0.001f, now + 1);
            BiquadFilterNode filter = audio.createBiquadFilter();
            filter.setType(BiquadFilterType.LOWPASS);
            filter.getFrequency().setValue(1200);
            osc.connect(filter).connect(env).connect(audio.getDestination());
            osc.start(now);
            osc.stop(now + 1);
        }
        // end::audio-graph-beep[]
    }

    void spatial() {
        // tag::audio-graph-spatial[]
        AudioContext audio = AudioContext.create();
        if (audio != null) {
            PannerNode panner = audio.createPanner();
            panner.setPanningModel(PanningModel.HRTF);
            panner.getPositionX().setValue(3);
            panner.getPositionZ().setValue(-1);
            audio.getListener().getPositionX().setValue(0);
            OscillatorNode tone = audio.createOscillator();
            tone.connect(panner).connect(audio.getDestination());
            tone.start();
            tone.stop(audio.getCurrentTime() + 2);
        }
        // end::audio-graph-spatial[]
    }

    float level() {
        // tag::audio-graph-analyser[]
        OfflineAudioContext audio = new OfflineAudioContext(1, 4096, 44100);
        OscillatorNode osc = audio.createOscillator();
        AnalyserNode analyser = audio.createAnalyser();
        osc.connect(analyser).connect(audio.getDestination());
        osc.start();
        audio.startRendering();
        float[] spectrum = new float[analyser.getFrequencyBinCount()];
        analyser.getFloatFrequencyData(spectrum);
        float loudest = -1000;
        for (float db : spectrum) {
            loudest = Math.max(loudest, db);
        }
        // end::audio-graph-analyser[]
        return loudest;
    }

    AudioProcessorNode bitCrusher() {
        // tag::audio-graph-processor[]
        OfflineAudioContext audio = new OfflineAudioContext(2, 44100, 44100);
        AudioProcessorNode crusher = audio.createAudioProcessor(new AudioProcessor() {
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

    void microphone() {
        // tag::audio-graph-microphone[]
        AudioContext audio = AudioContext.create();
        if (audio != null && audio.isMicrophoneSourceSupported()) {
            AudioBuffer capture = MediaManager.getAudioBuffer("mic", true, 4096);
            MicrophoneSourceNode mic = audio.createMicrophoneSource(capture);
            AnalyserNode meter = audio.createAnalyser();
            mic.connect(meter);
            // start a recorder built with redirectToAudioBuffer(true) and path "mic"
        }
        // end::audio-graph-microphone[]
    }

    void offline() {
        // tag::audio-graph-offline[]
        byte[] wav = new byte[0]; // the bytes of a .wav file, from storage or a resource
        AudioData rendered = null;
        OfflineAudioContext offline = new OfflineAudioContext(2, 48000 * 5, 48000);
        AudioData clip = offline.decodeAudioData(wav); // null if not a WAV file
        if (clip != null) {
            AudioBufferSourceNode src = offline.createBufferSource();
            src.setBuffer(clip);
            ConvolverNode reverb = offline.createConvolver();
            reverb.setBuffer(clip);
            src.connect(reverb).connect(offline.getDestination());
            src.start();
            rendered = offline.startRendering();
        }
        // end::audio-graph-offline[]
    }
}
