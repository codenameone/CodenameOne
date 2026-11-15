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

/// A platform audio output stream that pulls samples from an
/// [AudioRenderCallback]. Ports implement this and return it from
/// `CodenameOneImplementation.openAudioOutput`; applications use
/// [AudioContext] instead.
public interface AudioOutputPeer {
    /// Starts, or resumes, calling the render callback.
    void start();

    /// Pauses the stream; the render callback is not called until [#start()].
    void stop();

    /// Stops the stream for good and releases the device.
    void close();

    /// The sample rate the device actually runs at, which may differ from the
    /// one requested.
    ///
    /// #### Returns
    ///
    /// frames per second
    int getSampleRate();

    /// The number of interleaved channels passed to the render callback.
    ///
    /// #### Returns
    ///
    /// the channel count
    int getChannels();

    /// The time between a sample being rendered and it being heard, as well as
    /// the port can tell.
    ///
    /// #### Returns
    ///
    /// the latency in seconds, or 0 when unknown
    double getOutputLatency();
}
