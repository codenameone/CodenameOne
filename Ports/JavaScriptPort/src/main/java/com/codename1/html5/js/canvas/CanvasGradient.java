/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.canvas;

import com.codename1.html5.js.JSObject;

/**
 * Canvas gradient interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/CanvasGradient
 */
public interface CanvasGradient extends JSObject {
    void addColorStop(double offset, String color);
}