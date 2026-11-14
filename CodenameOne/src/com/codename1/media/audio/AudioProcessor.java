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

/// Custom DSP in Java, the Codename One counterpart of the Web Audio
/// `AudioWorkletProcessor`. An [AudioProcessorNode] calls [#process] once per
/// render quantum of 128 frames, on the render thread.
///
/// The arrays follow the worklet's layout: `inputs[input][channel][frame]`,
/// `outputs[output][channel][frame]` and `parameters[param][frame]`. An input
/// with nothing connected has one silent channel. A parameter whose value is
/// constant for the quantum is an array of length 1; otherwise it has 128
/// values. Outputs arrive zeroed.
///
/// The method runs on the audio thread: it must not block, wait on the EDT or
/// allocate in steady state, or the output glitches. Keep the arrays only for
/// the duration of the call; they are reused.
public interface AudioProcessor {
    /// Renders one quantum.
    ///
    /// #### Parameters
    ///
    /// - `inputs`: the mixed input of each input, per channel
    ///
    /// - `outputs`: the output to fill, per output and channel
    ///
    /// - `parameters`: the value of each declared parameter, in declaration order
    ///
    /// #### Returns
    ///
    /// true to keep processing; false to have the node output silence from
    /// now on, as a worklet does once its processor stops answering true
    boolean process(float[][][] inputs, float[][][] outputs, float[][] parameters);
}
