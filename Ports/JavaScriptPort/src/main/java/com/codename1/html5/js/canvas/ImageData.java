/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
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
