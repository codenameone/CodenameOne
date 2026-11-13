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
/// A real-time and offline audio graph modelled on the W3C Web Audio API.
///
/// Sources ([AudioBufferSourceNode], [OscillatorNode], [ConstantSourceNode])
/// feed processing nodes ([GainNode], [DelayNode], [BiquadFilterNode],
/// [IIRFilterNode], [StereoPannerNode], [ChannelSplitterNode],
/// [ChannelMergerNode]) that end at the context's destination. Node
/// parameters are [AudioParam]s with sample-accurate automation and can be
/// modulated by other nodes.
///
/// [PannerNode] places a source in 3D around the context's [AudioListener],
/// with the specification's distance models and sound cones. Its
/// [PanningModel#HRTF] mode renders binaurally for headphones from a
/// structural head model (interaural delay, head shadow and pinna echoes), so
/// it is the same portable code, with the same result, on every platform.
///
/// [OfflineAudioContext] renders to memory on every platform. [AudioContext]
/// plays to the speaker where the platform has a real-time output; call
/// [AudioContext#isSupported()] first. Names and behaviour follow the
/// specification, so Web Audio material applies with Java naming: `gain.gain.value = 0.5`
/// becomes `gain.getGain().setValue(0.5f)`.
///
/// The graph is rendered by portable code, so a graph sounds the same on every
/// platform it runs on.
package com.codename1.media.audio;
