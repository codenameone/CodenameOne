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
}
