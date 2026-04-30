/*
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 * Licensed under the PolyForm Noncommercial License 1.0.0
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.JSObject;

/**
 * Interface for the JavaScript HTMLCanvasElement.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLCanvasElement
 */
public interface HTMLCanvasElement extends HTMLElement {
    int getWidth();
    int getHeight();
    void setWidth(int width);
    void setHeight(int height);
    CanvasRenderingContext2D getContext(String contextId);
    String toDataURL(String type);
    String toDataURL(String type, double quality);
}
