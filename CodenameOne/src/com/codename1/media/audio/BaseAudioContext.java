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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/// What [AudioContext] (real time, to the speaker) and [OfflineAudioContext]
/// (as fast as possible, to memory) share: the clock, the destination and the
/// factory methods for every node, the Web Audio `BaseAudioContext`.
///
/// The graph is rendered in quanta of 128 frames. Building and changing it is
/// allowed from any thread; every change is queued and applied by the renderer
/// at the start of its next quantum, so the renderer never waits on a lock.
public abstract class BaseAudioContext {
    /// Frames rendered at a time, fixed by the specification.
    static final int RENDER_QUANTUM = 128;

    private static final Runnable[] NONE = new Runnable[0];

    private final float sampleRate;
    private final AtomicLong renderedFrames = new AtomicLong();
    private final AtomicReference<Runnable[]> pending = new AtomicReference<Runnable[]>(NONE);
    private final AudioDestinationNode destination;
    private final AudioListener listener;
    private final PeriodicWave[] standardWaves = new PeriodicWave[4];

    // ---- render side ----
    private final AudioBus silent = new AudioBus(1);
    private final ArrayList<AudioNode> rActive = new ArrayList<AudioNode>();
    private final ArrayList<AudioScheduledSourceNode> rEndedNow = new ArrayList<AudioScheduledSourceNode>();
    private long rQuantum;

    BaseAudioContext(float sampleRate, int destinationChannels, int maxChannels) {
        checkSampleRate(sampleRate);
        this.sampleRate = sampleRate;
        destination = new AudioDestinationNode(this, destinationChannels, maxChannels);
        listener = new AudioListener(this);
    }

    /// Times reach the renderer as `float` seconds more often than not
    /// (`200 / 48000f`), which lands a hair past the frame meant. A frame
    /// position within a thousandth of a frame of a whole frame is taken as
    /// that frame.
    static double snapFrames(double frames) {
        double r = Math.floor(frames + 0.5);
        return Math.abs(frames - r) < 1e-3 ? r : frames;
    }

    static boolean nonNegativeFinite(double v) {
        return v >= 0 && !Double.isInfinite(v);
    }

    static boolean positiveFinite(double v) {
        return v > 0 && !Double.isInfinite(v);
    }

    static void checkSampleRate(float sampleRate) {
        if (!(sampleRate >= 3000f && sampleRate <= 768000f)) {
            throw new IllegalArgumentException("sampleRate must be between 3000 and 768000: " + sampleRate);
        }
    }

    /// The sample rate every node of this context renders at.
    ///
    /// #### Returns
    ///
    /// frames per second
    public float getSampleRate() {
        return sampleRate;
    }

    /// The time of the next frame to be rendered: the clock every scheduling
    /// method is measured against. It starts at 0 and advances one render
    /// quantum at a time.
    ///
    /// #### Returns
    ///
    /// seconds
    public double getCurrentTime() {
        return renderedFrames.get() / (double) sampleRate;
    }

    /// The node whose input is heard (or recorded, offline).
    ///
    /// #### Returns
    ///
    /// the destination
    public AudioDestinationNode getDestination() {
        return destination;
    }

    /// The listener every [PannerNode] of this context is heard from.
    ///
    /// #### Returns
    ///
    /// the listener
    public AudioListener getListener() {
        return listener;
    }

    /// Creates a 3D panner, equal-power with the inverse distance model until changed.
    ///
    /// #### Returns
    ///
    /// the node
    public PannerNode createPanner() {
        return new PannerNode(this);
    }

    /// Whether [PanningModel#HRTF] renders binaurally on this platform. It is
    /// always true: the head model is portable code. The method exists so code
    /// written for platforms where HRTF might fall back to equal-power can ask.
    ///
    /// #### Returns
    ///
    /// true
    public boolean isHrtfSupported() {
        return true;
    }

    /// The life cycle state.
    ///
    /// #### Returns
    ///
    /// the state
    public abstract AudioContextState getState();

    /// Creates silent [AudioData] to fill in and play.
    ///
    /// #### Parameters
    ///
    /// - `numberOfChannels`: 1 to 32
    ///
    /// - `length`: frames
    ///
    /// - `sampleRate`: frames per second; need not match the context
    ///
    /// #### Returns
    ///
    /// the new data
    public AudioData createBuffer(int numberOfChannels, int length, float sampleRate) {
        return new AudioData(numberOfChannels, length, sampleRate);
    }

    /// Creates a node that multiplies its input by a gain.
    ///
    /// #### Returns
    ///
    /// the node
    public GainNode createGain() {
        return new GainNode(this);
    }

    /// Creates a delay line of up to one second.
    ///
    /// #### Returns
    ///
    /// the node
    public DelayNode createDelay() {
        return new DelayNode(this, 1);
    }

    /// Creates a delay line.
    ///
    /// #### Parameters
    ///
    /// - `maxDelayTime`: the longest delay in seconds, more than 0 and less than 180
    ///
    /// #### Returns
    ///
    /// the node
    public DelayNode createDelay(double maxDelayTime) {
        return new DelayNode(this, maxDelayTime);
    }

    /// Creates a node that plays [AudioData].
    ///
    /// #### Returns
    ///
    /// the node
    public AudioBufferSourceNode createBufferSource() {
        return new AudioBufferSourceNode(this);
    }

    /// Creates a node whose output is a constant, or automated, value.
    ///
    /// #### Returns
    ///
    /// the node
    public ConstantSourceNode createConstantSource() {
        return new ConstantSourceNode(this);
    }

    /// Creates a band-limited oscillator, a 440 Hz sine until changed.
    ///
    /// #### Returns
    ///
    /// the node
    public OscillatorNode createOscillator() {
        return new OscillatorNode(this);
    }

    /// Creates a second order filter, a 350 Hz low pass until changed.
    ///
    /// #### Returns
    ///
    /// the node
    public BiquadFilterNode createBiquadFilter() {
        return new BiquadFilterNode(this);
    }

    /// Creates a general IIR filter.
    ///
    /// #### Parameters
    ///
    /// - `feedforward`: the numerator coefficients, 1 to 20 of them, not all 0
    ///
    /// - `feedback`: the denominator coefficients, 1 to 20 of them, the first not 0
    ///
    /// #### Returns
    ///
    /// the node
    public IIRFilterNode createIIRFilter(double[] feedforward, double[] feedback) {
        return new IIRFilterNode(this, feedforward, feedback);
    }

    /// Creates an equal-power stereo panner.
    ///
    /// #### Returns
    ///
    /// the node
    public StereoPannerNode createStereoPanner() {
        return new StereoPannerNode(this);
    }

    /// Creates a node that splits 6 channels into 6 mono outputs.
    ///
    /// #### Returns
    ///
    /// the node
    public ChannelSplitterNode createChannelSplitter() {
        return new ChannelSplitterNode(this, 6);
    }

    /// Creates a node that splits channels into mono outputs.
    ///
    /// #### Parameters
    ///
    /// - `numberOfOutputs`: 1 to 32
    ///
    /// #### Returns
    ///
    /// the node
    public ChannelSplitterNode createChannelSplitter(int numberOfOutputs) {
        return new ChannelSplitterNode(this, numberOfOutputs);
    }

    /// Creates a node that merges 6 mono inputs into one 6 channel output.
    ///
    /// #### Returns
    ///
    /// the node
    public ChannelMergerNode createChannelMerger() {
        return new ChannelMergerNode(this, 6);
    }

    /// Creates a node that merges mono inputs into one output.
    ///
    /// #### Parameters
    ///
    /// - `numberOfInputs`: 1 to 32
    ///
    /// #### Returns
    ///
    /// the node
    public ChannelMergerNode createChannelMerger(int numberOfInputs) {
        return new ChannelMergerNode(this, numberOfInputs);
    }

    /// Creates a normalized custom waveform for an [OscillatorNode].
    ///
    /// #### Parameters
    ///
    /// - `real`: cosine coefficients; index 0, the DC term, is ignored
    ///
    /// - `imag`: sine coefficients, the same length as `real`
    ///
    /// #### Returns
    ///
    /// the waveform
    public PeriodicWave createPeriodicWave(float[] real, float[] imag) {
        return new PeriodicWave(real, imag, false);
    }

    /// Creates a custom waveform for an [OscillatorNode].
    ///
    /// #### Parameters
    ///
    /// - `real`: cosine coefficients; index 0, the DC term, is ignored
    ///
    /// - `imag`: sine coefficients, the same length as `real`
    ///
    /// - `disableNormalization`: true to keep the coefficients' own amplitude
    ///   rather than scaling the peak to 1
    ///
    /// #### Returns
    ///
    /// the waveform
    public PeriodicWave createPeriodicWave(float[] real, float[] imag, boolean disableNormalization) {
        return new PeriodicWave(real, imag, disableNormalization);
    }

    PeriodicWave standardWave(OscillatorType type) {
        int i = type.ordinal();
        PeriodicWave w = standardWaves[i];
        if (w == null) {
            w = PeriodicWave.standard(type);
            standardWaves[i] = w;
        }
        return w;
    }

    /// Queues a change for the renderer.
    void post(Runnable r) {
        for (;;) {
            Runnable[] cur = pending.get();
            Runnable[] next = new Runnable[cur.length + 1];
            System.arraycopy(cur, 0, next, 0, cur.length);
            next[cur.length] = r;
            if (pending.compareAndSet(cur, next)) {
                return;
            }
        }
    }

    // ---------------------------------------------------------------- render

    AudioBus silentBus() {
        return silent;
    }

    void rActivate(AudioNode n) {
        if (!rActive.contains(n)) {
            rActive.add(n);
        }
    }

    void rEnded(AudioScheduledSourceNode n) {
        rEndedNow.add(n);
    }

    /// Renders the next quantum and returns the destination's output. Render
    /// thread only.
    final AudioBus renderQuantum() {
        Runnable[] changes = pending.getAndSet(NONE);
        for (Runnable change : changes) {
            change.run();
        }
        long q = rQuantum;
        AudioBus out = destination.pull(0, q);
        // Sources not (yet) connected to the destination still advance in time.
        for (int i = 0; i < rActive.size(); i++) { // NOPMD ForLoopCanBeForeach - no iterator on the render thread
            AudioNode n = rActive.get(i);
            if (!n.renderedQuantum(q)) {
                n.pull(0, q);
            }
        }
        rQuantum = q + 1;
        renderedFrames.set(rQuantum * RENDER_QUANTUM);
        int ended = rEndedNow.size();
        if (ended > 0) {
            for (int i = 0; i < ended; i++) {
                AudioScheduledSourceNode n = rEndedNow.get(i);
                rActive.remove(n);
                n.dispatchEnded(dispatchesEndedInline());
            }
            rEndedNow.clear();
        }
        return out;
    }

    /// Whether `onended` callbacks run on the rendering thread (offline) or are
    /// handed to the EDT (real time).
    abstract boolean dispatchesEndedInline();
}
