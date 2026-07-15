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
package com.codename1.html5.js.canvas;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.typedarrays.Uint8ClampedArray;

/**
 * Interface for ImageData from canvas.
 * https://developer.mozilla.org/en-US/docs/Web/API/ImageData
 */
public interface ImageData extends JSObject {
    int getWidth();
    int getHeight();
    Uint8ClampedArray getData();
    /// Writes ARGB pixel data into ``imageData.data`` host-side, in one round
    /// trip. The host bridge clones ``imageData.data`` when the worker reads
    /// it (a perf optimization for ``get(index)`` loops, see ``hostResult``
    /// in browser_bridge.js), so the natural-looking
    /// ``((Uint8ClampedArraySetter)d.getData()).set(arr)`` writes from the
    /// worker land in the *clone* — the live ``imageData.data`` stays
    /// zero-initialised, ``putImageData`` then renders transparent black,
    /// and any code that relies on the data round-trip
    /// (``CommonTransitions``' rgbBuffer fade path, anything else that goes
    /// through ``HTML5Implementation.createImage(int[], int, int)``) paints
    /// nothing. ``writeArgbBuffer`` skips the round-trip: the int[] is
    /// structured-cloned to host (one ``postMessage``), and a host-side
    /// prototype extension in browser_bridge.js unpacks ARGB → RGBA into
    /// the live ``this.data`` buffer there.
    void writeArgbBuffer(int[] argb, int offset, int width, int height);
}
