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

/// A mono source whose output is [#getOffset()], the Web Audio
/// `ConstantSourceNode`. Connected to several parameters it drives them all
/// from one automation timeline.
public final class ConstantSourceNode extends AudioScheduledSourceNode {
    private final AudioParam offset;

    ConstantSourceNode(BaseAudioContext context) {
        super(context, 1);
        offset = new AudioParam(context, "offset", 1f, -Float.MAX_VALUE, Float.MAX_VALUE,
                AutomationRate.A_RATE, false);
    }

    /// The output value, 1 by default; an audio rate parameter.
    ///
    /// #### Returns
    ///
    /// the parameter
    public AudioParam getOffset() {
        return offset;
    }

    @Override
    void render(long q, int from, int to) {
        float[] v = offset.compute(q);
        float[] d = outBuses[0].data[0];
        for (int i = from; i < to; i++) {
            d[i] = v[i];
        }
    }
}
