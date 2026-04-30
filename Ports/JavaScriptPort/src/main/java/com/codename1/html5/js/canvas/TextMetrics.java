/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.canvas;

import com.codename1.html5.js.JSObject;

/**
 * TextMetrics interface for canvas text measurement.
 * https://developer.mozilla.org/en-US/docs/Web/API/TextMetrics
 */
public interface TextMetrics extends JSObject {
    double getWidth();
    double getActualBoundingBoxLeft();
    double getActualBoundingBoxRight();
    double getActualBoundingBoxAscent();
    double getActualBoundingBoxDescent();
}