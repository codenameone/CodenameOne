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
package com.codenameone.developerguide.screenshots;

import com.codename1.components.AudioWaveform;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/// A voice message waveform, partly played, for the developer guide.
class ComponentsAudioWaveformFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-audio-waveform";
    }

    @Override
    public Form build() {
        Form hi = new Form("Voice message", BoxLayout.y());
        float[] levels = new float[40];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = 0.2f + 0.8f * (float) Math.abs(Math.sin(i * 0.45) * Math.cos(i * 0.13));
        }
        AudioWaveform wave = new AudioWaveform(levels);
        wave.setProgress(0.4f);
        hi.add(wave);
        hi.show();
        return hi;
    }
}
