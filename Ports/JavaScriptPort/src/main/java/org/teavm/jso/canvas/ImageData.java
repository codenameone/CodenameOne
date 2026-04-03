/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.canvas;

import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

public interface ImageData extends JSObject {
    int getWidth();
    int getHeight();
    Uint8ClampedArray getData();
}