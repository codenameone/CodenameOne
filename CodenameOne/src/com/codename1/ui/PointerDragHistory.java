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
package com.codename1.ui;

import com.codename1.impl.CodenameOneImplementation;

/// The recent path of one pointer gesture, which is what a fling's speed is computed
/// from.
///
/// This is the only piece of gesture bookkeeping involved enough to be worth sharing:
/// a ring of positions and timestamps, plus the wrap arithmetic around it. Everything
/// else a top level tracks during a gesture -- the pressed component, whether a drag
/// happened, the long press timer -- is a field, and a field on the right object needs
/// no sharing.
///
/// One of these belongs to each top level that dispatches pointer events: `Display`
/// owns the main surface's, and every `Window` owns its own. That is deliberately not
/// a table keyed by window: a per-window slot table has to be leased, reclaimed and
/// bounded, and getting any of that wrong loses a gesture or leaks a slot. An object
/// the window holds is created when the window is and collected with it.
///
/// @author Shai Almog
final class PointerDragHistory {

    private final float[] pathX;
    private final float[] pathY;
    private final long[] pathTime;
    private int offset;
    private int length;

    /// The moment the display started, which recorded timestamps are relative to.
    private final long baseTime;

    PointerDragHistory(int pathLength, long baseTime) {
        int len = pathLength > 0 ? pathLength : 1;
        pathX = new float[len];
        pathY = new float[len];
        pathTime = new long[len];
        this.baseTime = baseTime;
    }

    /// Records one position in the gesture.
    void record(int x, int y, int timestamp) {
        pathX[offset] = x;
        pathY[offset] = y;
        pathTime[offset] = baseTime + (long) timestamp;
        if (length < pathX.length) {
            length++;
        }
        offset++;
        if (offset >= pathX.length) {
            offset = 0;
        }
    }

    /// Forgets the gesture, so the next one starts from nothing rather than flinging
    /// with the previous gesture's speed.
    void reset() {
        offset = 0;
        length = 0;
    }

    /// The speed of the recorded gesture along one axis, as the implementation
    /// computes it.
    float speed(CodenameOneImplementation impl, boolean yAxis) {
        if (yAxis) {
            return impl.getDragSpeed(pathY, pathTime, offset, length);
        }
        return impl.getDragSpeed(pathX, pathTime, offset, length);
    }

    /// How many positions are recorded. Used to tell a real gesture from one that
    /// never moved.
    int recordedLength() {
        return length;
    }
}
