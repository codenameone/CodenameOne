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

/// Supplies the samples an [AudioOutputPeer] plays. Implemented by
/// [AudioContext]; ports call it from their audio thread.
public interface AudioRenderCallback {
    /// Fills `interleaved` with `frames` frames of `channels` interleaved float
    /// samples in the range -1 to 1. Called on the port's audio thread; it must
    /// return quickly and never block.
    ///
    /// #### Parameters
    ///
    /// - `interleaved`: the buffer to fill, at least `frames * channels` long
    ///
    /// - `frames`: the number of frames wanted
    ///
    /// - `channels`: the number of interleaved channels
    void render(float[] interleaved, int frames, int channels);
}
