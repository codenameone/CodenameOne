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

import com.codename1.io.Log;
import com.codename1.ui.CN;
import java.util.concurrent.atomic.AtomicBoolean;

/// Runs an [AudioProcessor] in the graph, the Web Audio `AudioWorkletNode`.
/// Created by [BaseAudioContext#createAudioProcessor]. Parameters are real
/// [AudioParam]s, automated and modulated like any other.
///
/// A processor that throws is stopped: the node outputs silence from then on
/// and [#setOnProcessorError(Runnable)] runs on the EDT, as the worklet's
/// `processorerror` event does.
public final class AudioProcessorNode extends AudioNode {
    private final AudioProcessor processor;
    private final AudioParam[] params;
    private final String[] paramNames;
    private final int[] outputChannels;
    private final float[][][] inViews;
    private final float[][][] outViews;
    private final float[][] paramValues;
    private final float[][] constantValues;
    private final AtomicBoolean failed = new AtomicBoolean();
    private Runnable onError;
    private final Runnable errorTask = new Runnable() {
        @Override
        public void run() {
            Runnable r = onError;
            if (r != null) {
                r.run();
            }
        }
    };
    private boolean rStopped;

    AudioProcessorNode(BaseAudioContext context, AudioProcessor processor, int inputs, int outputs,
            int[] outputChannelCount, AudioParamDescriptor[] descriptors) {
        super(context, inputs, outputs, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        if (processor == null) {
            throw new IllegalArgumentException("processor");
        }
        if (inputs == 0 && outputs == 0) {
            throw new IllegalArgumentException("A processor needs an input or an output");
        }
        this.processor = processor;
        outputChannels = new int[outputs];
        for (int i = 0; i < outputs; i++) {
            int n = outputChannelCount == null || i >= outputChannelCount.length ? 1 : outputChannelCount[i];
            if (n < 1 || n > AudioBus.MAX_CHANNELS) {
                throw new IllegalArgumentException("output channel count must be between 1 and "
                        + AudioBus.MAX_CHANNELS);
            }
            outputChannels[i] = n;
        }
        int pc = descriptors == null ? 0 : descriptors.length;
        params = new AudioParam[pc];
        paramNames = new String[pc];
        paramValues = new float[pc][];
        constantValues = new float[pc][1];
        for (int i = 0; i < pc; i++) {
            AudioParamDescriptor d = descriptors[i];
            for (int j = 0; j < i; j++) {
                if (paramNames[j].equals(d.getName())) {
                    throw new IllegalArgumentException("Duplicate parameter " + d.getName());
                }
            }
            paramNames[i] = d.getName();
            params[i] = new AudioParam(context, d.getName(), d.getDefaultValue(), d.getMinValue(),
                    d.getMaxValue(), d.getAutomationRate(), false);
        }
        inViews = new float[inputs][][];
        outViews = new float[outputs][][];
    }

    /// A declared parameter by name.
    ///
    /// #### Parameters
    ///
    /// - `name`: the descriptor's name
    ///
    /// #### Returns
    ///
    /// the parameter, or null when no descriptor has that name
    public AudioParam getParameter(String name) {
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(name)) {
                return params[i];
            }
        }
        return null;
    }

    /// The names of the declared parameters, in declaration order.
    ///
    /// #### Returns
    ///
    /// a new array of names
    public String[] getParameterNames() {
        String[] out = new String[paramNames.length];
        System.arraycopy(paramNames, 0, out, 0, out.length);
        return out;
    }

    /// Whether the processor threw and was stopped.
    ///
    /// #### Returns
    ///
    /// true after a failure
    public boolean isFailed() {
        return failed.get();
    }

    /// Sets what runs on the EDT when the processor throws.
    ///
    /// #### Parameters
    ///
    /// - `r`: the handler, or null
    public void setOnProcessorError(Runnable r) {
        onError = r;
    }

    private static float[][] view(float[][] cached, AudioBus bus, int channels) {
        float[][] v = cached;
        if (v == null || v.length != channels) {
            v = new float[channels][];
        }
        for (int c = 0; c < channels; c++) {
            v[c] = bus.data[c];
        }
        return v;
    }

    @Override
    void process(long q) {
        for (int o = 0; o < outBuses.length; o++) {
            outBuses[o].setChannels(outputChannels[o]);
        }
        if (rStopped) {
            return;
        }
        for (int i = 0; i < inBuses.length; i++) {
            inViews[i] = view(inViews[i], inBuses[i], inBuses[i].channels);
        }
        for (int o = 0; o < outBuses.length; o++) {
            outViews[o] = view(outViews[o], outBuses[o], outputChannels[o]);
        }
        for (int p = 0; p < params.length; p++) {
            float[] v = params[p].compute(q);
            if (params[p].isConstant()) {
                constantValues[p][0] = v[0];
                paramValues[p] = constantValues[p];
            } else {
                paramValues[p] = v;
            }
        }
        boolean keep;
        try {
            keep = processor.process(inViews, outViews, paramValues);
        } catch (RuntimeException e) {
            rStopped = true;
            for (int o = 0; o < outBuses.length; o++) {
                outBuses[o].setChannels(outputChannels[o]);
            }
            if (failed.compareAndSet(false, true)) {
                Log.e(e);
                CN.callSerially(errorTask);
            }
            return;
        }
        if (!keep) {
            rStopped = true;
        }
    }
}
