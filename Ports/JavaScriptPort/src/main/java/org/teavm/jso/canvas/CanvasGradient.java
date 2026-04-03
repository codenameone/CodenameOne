/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.canvas;

import org.teavm.jso.JSObject;

public interface CanvasGradient extends JSObject {
    void addColorStop(double offset, String color);
}

public interface CanvasPattern extends JSObject {}

public interface CanvasImageSource extends JSObject {}