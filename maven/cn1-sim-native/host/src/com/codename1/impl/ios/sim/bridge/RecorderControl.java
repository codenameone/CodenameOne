/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.impl.ios.sim.bridge;

/**
 * The Test Recorder's control surface, exposed to the simulator shell. The
 * recorder lives next to the input pipeline (it taps the relay's pointer/key
 * events on the way in and replays them with the original timing); the shell's
 * Tools menu drives it through this interface.
 */
public interface RecorderControl {
    /** Clears any prior take and begins recording input. */
    void start();

    /** Stops recording. */
    void stop();

    /** Replays the recorded input on a background thread with original timing. */
    void play();

    /** @return true while recording. */
    boolean isRecording();

    /** @return number of events captured. */
    int count();
}
