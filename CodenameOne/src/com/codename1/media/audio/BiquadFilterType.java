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

/// The response of a [BiquadFilterNode]. The coefficient formulas are the ones
/// the Web Audio specification gives, derived from the Audio EQ Cookbook.
public enum BiquadFilterType {
    /// Second order low pass; `Q` is the resonance in dB.
    LOWPASS,
    /// Second order high pass; `Q` is the resonance in dB.
    HIGHPASS,
    /// Second order band pass around `frequency`; `Q` sets the width.
    BANDPASS,
    /// Boosts or cuts frequencies below `frequency` by `gain` dB.
    LOWSHELF,
    /// Boosts or cuts frequencies above `frequency` by `gain` dB.
    HIGHSHELF,
    /// Boosts or cuts a band around `frequency` by `gain` dB; `Q` sets the width.
    PEAKING,
    /// Rejects a band around `frequency`; `Q` sets the width.
    NOTCH,
    /// Passes every frequency and shifts phase around `frequency`.
    ALLPASS
}
