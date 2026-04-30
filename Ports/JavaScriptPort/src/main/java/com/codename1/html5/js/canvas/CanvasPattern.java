/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.canvas;

import com.codename1.html5.js.JSObject;

/**
 * Interface for CanvasPattern.
 * https://developer.mozilla.org/en-US/docs/Web/API/CanvasPattern
 */
public interface CanvasPattern extends JSObject {
    void setTransform(double a, double b, double c, double d, double e, double f);
}
