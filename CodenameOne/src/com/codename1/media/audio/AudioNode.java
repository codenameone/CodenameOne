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

/// A processing block of an audio graph, the Web Audio `AudioNode`.
///
/// Nodes are created by a context (`context.createGain()` and so on) and wired
/// with [#connect(AudioNode)]; audio is heard once a path reaches
/// [BaseAudioContext#getDestination()]. Each input mixes everything connected
/// to it to a channel count decided by [#getChannelCountMode()],
/// [#getChannelCount()] and [#getChannelInterpretation()], following the
/// specification's mixing rules.
///
/// A cycle in the graph must contain a [DelayNode]; a cycle without one is
/// rendered as silence, as the specification requires. Every method may be
/// called from any thread; changes reach the renderer at its next render
/// quantum.
public abstract class AudioNode {
    final BaseAudioContext context;
    private final int numberOfInputs;
    private final int numberOfOutputs;
    private int channelCount;
    private ChannelCountMode channelCountMode;
    private ChannelInterpretation channelInterpretation;

    // ---- render side ----
    int rChannelCount;
    ChannelCountMode rMode;
    ChannelInterpretation rInterp;
    final ArrayList<ArrayList<Connection>> rInputs;
    final ArrayList<Connection> rOutgoing = new ArrayList<Connection>();
    final AudioBus[] inBuses;
    final AudioBus[] outBuses;
    private long rQuantum = -1;
    private boolean rBusy;

    AudioNode(BaseAudioContext context, int numberOfInputs, int numberOfOutputs, int channelCount,
              ChannelCountMode mode, ChannelInterpretation interp) {
        this.context = context;
        this.numberOfInputs = numberOfInputs;
        this.numberOfOutputs = numberOfOutputs;
        this.channelCount = channelCount;
        this.channelCountMode = mode;
        this.channelInterpretation = interp;
        rChannelCount = channelCount;
        rMode = mode;
        rInterp = interp;
        rInputs = new ArrayList<ArrayList<Connection>>(numberOfInputs);
        inBuses = new AudioBus[numberOfInputs];
        for (int i = 0; i < numberOfInputs; i++) {
            rInputs.add(new ArrayList<Connection>());
            inBuses[i] = new AudioBus(1);
        }
        int outs = Math.max(1, numberOfOutputs);
        outBuses = new AudioBus[outs];
        for (int i = 0; i < outs; i++) {
            outBuses[i] = new AudioBus(1);
        }
    }

    /// The context that created this node.
    ///
    /// #### Returns
    ///
    /// the context
    public BaseAudioContext getContext() {
        return context;
    }

    /// The number of inputs.
    ///
    /// #### Returns
    ///
    /// 0 for a source node
    public int getNumberOfInputs() {
        return numberOfInputs;
    }

    /// The number of outputs.
    ///
    /// #### Returns
    ///
    /// 0 for the destination
    public int getNumberOfOutputs() {
        return numberOfOutputs;
    }

    /// The channel count inputs are mixed to, subject to the channel count mode.
    ///
    /// #### Returns
    ///
    /// the channel count
    public int getChannelCount() {
        return channelCount;
    }

    /// Sets the channel count.
    ///
    /// #### Parameters
    ///
    /// - `count`: 1 to 32, or less for nodes that restrict it
    public void setChannelCount(int count) {
        checkChannelCount(count);
        channelCount = count;
        final int c = count;
        context.post(new Runnable() {
            @Override
            public void run() {
                rChannelCount = c;
            }
        });
    }

    /// How the channel count of each input is decided.
    ///
    /// #### Returns
    ///
    /// the mode
    public ChannelCountMode getChannelCountMode() {
        return channelCountMode;
    }

    /// Sets how the channel count of each input is decided.
    ///
    /// #### Parameters
    ///
    /// - `mode`: the mode
    public void setChannelCountMode(ChannelCountMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode is null");
        }
        checkChannelCountMode(mode);
        channelCountMode = mode;
        final ChannelCountMode m = mode;
        context.post(new Runnable() {
            @Override
            public void run() {
                rMode = m;
            }
        });
    }

    /// How channels are up-mixed and down-mixed.
    ///
    /// #### Returns
    ///
    /// the interpretation
    public ChannelInterpretation getChannelInterpretation() {
        return channelInterpretation;
    }

    /// Sets how channels are up-mixed and down-mixed.
    ///
    /// #### Parameters
    ///
    /// - `interpretation`: the interpretation
    public void setChannelInterpretation(ChannelInterpretation interpretation) {
        if (interpretation == null) {
            throw new IllegalArgumentException("interpretation is null");
        }
        checkChannelInterpretation(interpretation);
        channelInterpretation = interpretation;
        final ChannelInterpretation ci = interpretation;
        context.post(new Runnable() {
            @Override
            public void run() {
                rInterp = ci;
            }
        });
    }

    /// Connects output 0 to input 0 of `destination`.
    ///
    /// #### Parameters
    ///
    /// - `destination`: the node to feed
    ///
    /// #### Returns
    ///
    /// `destination`, so connections can be chained
    public AudioNode connect(AudioNode destination) {
        return connect(destination, 0, 0);
    }

    /// Connects an output of this node to an input of `destination`.
    /// Connecting the same pair twice has no further effect.
    ///
    /// #### Parameters
    ///
    /// - `destination`: the node to feed
    ///
    /// - `output`: the output of this node
    ///
    /// - `input`: the input of `destination`
    ///
    /// #### Returns
    ///
    /// `destination`
    public AudioNode connect(AudioNode destination, int output, int input) {
        if (destination == null) {
            throw new IllegalArgumentException("destination is null");
        }
        if (destination.context != context) { // NOPMD CompareObjectsWithEquals - one context, by identity
            throw new IllegalArgumentException("Nodes belong to different contexts");
        }
        checkOutput(output);
        if (input < 0 || input >= destination.numberOfInputs) {
            throw new IllegalArgumentException("No input " + input);
        }
        final Connection c = new Connection(this, output, destination, null, input);
        context.post(new Runnable() {
            @Override
            public void run() {
                ArrayList<Connection> list = c.destNode.rInputs.get(c.input);
                for (int i = 0; i < list.size(); i++) { // NOPMD ForLoopCanBeForeach - no iterator on the render thread
                    Connection o = list.get(i);
                    if (o.source == c.source && o.output == c.output) { // NOPMD CompareObjectsWithEquals - graph edges are identities
                        return;
                    }
                }
                list.add(c);
                rOutgoing.add(c);
            }
        });
        return destination;
    }

    /// Connects output 0 to an [AudioParam], whose value then has this output,
    /// mixed down to mono, added to it.
    ///
    /// #### Parameters
    ///
    /// - `param`: the parameter to modulate
    public void connect(AudioParam param) {
        connect(param, 0);
    }

    /// Connects an output to an [AudioParam].
    ///
    /// #### Parameters
    ///
    /// - `param`: the parameter to modulate
    ///
    /// - `output`: the output of this node
    public void connect(AudioParam param, int output) {
        if (param == null) {
            throw new IllegalArgumentException("param is null");
        }
        checkOutput(output);
        final Connection c = new Connection(this, output, null, param, 0);
        context.post(new Runnable() {
            @Override
            public void run() {
                ArrayList<Connection> list = c.destParam.rInputs;
                for (int i = 0; i < list.size(); i++) { // NOPMD ForLoopCanBeForeach - no iterator on the render thread
                    Connection o = list.get(i);
                    if (o.source == c.source && o.output == c.output) { // NOPMD CompareObjectsWithEquals - graph edges are identities
                        return;
                    }
                }
                list.add(c);
                rOutgoing.add(c);
            }
        });
    }

    /// Removes every outgoing connection.
    public void disconnect() {
        disconnectMatching(-1, null, null, -1);
    }

    /// Removes every connection from one output.
    ///
    /// #### Parameters
    ///
    /// - `output`: the output of this node
    public void disconnect(int output) {
        checkOutput(output);
        disconnectMatching(output, null, null, -1);
    }

    /// Removes every connection to `destination`.
    ///
    /// #### Parameters
    ///
    /// - `destination`: the node to stop feeding
    public void disconnect(AudioNode destination) {
        if (destination == null) {
            throw new IllegalArgumentException("destination is null");
        }
        disconnectMatching(-1, destination, null, -1);
    }

    /// Removes one connection.
    ///
    /// #### Parameters
    ///
    /// - `destination`: the node fed
    ///
    /// - `output`: the output of this node
    ///
    /// - `input`: the input of `destination`
    public void disconnect(AudioNode destination, int output, int input) {
        if (destination == null) {
            throw new IllegalArgumentException("destination is null");
        }
        checkOutput(output);
        disconnectMatching(output, destination, null, input);
    }

    /// Removes every connection to `param`.
    ///
    /// #### Parameters
    ///
    /// - `param`: the parameter to stop modulating
    public void disconnect(AudioParam param) {
        if (param == null) {
            throw new IllegalArgumentException("param is null");
        }
        disconnectMatching(-1, null, param, -1);
    }

    private void disconnectMatching(final int output, final AudioNode node, final AudioParam param,
                                    final int input) {
        context.post(new Runnable() {
            @Override
            public void run() {
                for (int i = rOutgoing.size() - 1; i >= 0; i--) {
                    Connection c = rOutgoing.get(i);
                    if (output >= 0 && c.output != output) {
                        continue;
                    }
                    if (node != null && c.destNode != node) { // NOPMD CompareObjectsWithEquals - graph edges are identities
                        continue;
                    }
                    if (param != null && c.destParam != param) { // NOPMD CompareObjectsWithEquals - graph edges are identities
                        continue;
                    }
                    if (input >= 0 && c.input != input) {
                        continue;
                    }
                    if (c.destNode != null) {
                        c.destNode.rInputs.get(c.input).remove(c);
                    } else {
                        c.destParam.rInputs.remove(c);
                    }
                    rOutgoing.remove(i);
                }
            }
        });
    }

    private void checkOutput(int output) {
        if (output < 0 || output >= numberOfOutputs) {
            throw new IllegalArgumentException("No output " + output);
        }
    }

    void checkChannelCount(int count) {
        if (count < 1 || count > AudioBus.MAX_CHANNELS) {
            throw new IllegalArgumentException("channelCount must be between 1 and "
                    + AudioBus.MAX_CHANNELS + ": " + count);
        }
    }

    void checkChannelCountMode(ChannelCountMode mode) {
    }

    void checkChannelInterpretation(ChannelInterpretation interpretation) {
    }

    // ---------------------------------------------------------------- render

    /// The output of this node for render quantum `q`, rendering it first if
    /// needed. Render thread only.
    final AudioBus pull(int output, long q) {
        if (rQuantum == q) {
            return outBuses[output];
        }
        if (rBusy) {
            return reentered(output, q);
        }
        rBusy = true;
        try {
            for (int i = 0; i < numberOfInputs; i++) {
                mixInput(i, q);
            }
            process(q);
        } finally {
            rBusy = false;
            rQuantum = q;
        }
        return outBuses[output];
    }

    /// Whether this node already rendered quantum `q`.
    final boolean renderedQuantum(long q) {
        return rQuantum == q;
    }

    /// Called when a cycle reaches this node while it is rendering. Only a
    /// delay can break a cycle; every other node answers silence.
    AudioBus reentered(int output, long q) {
        return context.silentBus();
    }

    private void mixInput(int index, long q) {
        ArrayList<Connection> list = rInputs.get(index);
        int n = list.size();
        int max = 1;
        for (int i = 0; i < n; i++) {
            Connection c = list.get(i);
            AudioBus b = c.source.pull(c.output, q);
            if (b.channels > max) {
                max = b.channels;
            }
        }
        int computed;
        if (rMode == ChannelCountMode.EXPLICIT) {
            computed = rChannelCount;
        } else if (rMode == ChannelCountMode.CLAMPED_MAX) {
            computed = Math.min(max, rChannelCount);
        } else {
            computed = max;
        }
        AudioBus in = inBuses[index];
        in.setChannels(computed);
        for (int i = 0; i < n; i++) {
            Connection c = list.get(i);
            AudioBus.sumInto(c.source.pull(c.output, q), in, rInterp);
        }
    }

    /// Renders one quantum from `inBuses` (already mixed) into `outBuses`.
    abstract void process(long q);
}
