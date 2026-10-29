/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter;

import java.util.ArrayList;
import java.util.List;

/**
 * dart:ui's {@code PictureRecorder}. A {@code Canvas(recorder)} writes its calls here;
 * {@link #endRecording} hands them to a {@link Picture}, which replays them onto a
 * Codename One {@code Graphics} when it is turned into an image.
 */
public class PictureRecorder {
    private List<Object[]> ops = new ArrayList<Object[]>();
    private boolean recording;

    public PictureRecorder() {
    }

    void begin() {
        recording = true;
    }

    void record(String op, Object[] args) {
        ops.add(new Object[] {op, args});
    }

    /** {@code isRecording}: a canvas was attached and endRecording has not run. */
    public boolean isRecording() {
        return recording;
    }

    /** Ends the recording; later calls on the canvas are recorded into a fresh one. */
    public Picture endRecording() {
        Picture p = new Picture(ops);
        ops = new ArrayList<Object[]>();
        recording = false;
        return p;
    }
}
