/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.canvas.CanvasRenderingContext2D;

public interface HTMLCanvasElement extends HTMLElement {
    @JSProperty int getWidth();
    @JSProperty void setWidth(int width);
    @JSProperty int getHeight();
    @JSProperty void setHeight(int height);
    CanvasRenderingContext2D getContext(String contextId);
    CanvasRenderingContext2D getContext2d();
    String toDataURL(String type);
    String toDataURL(String type, double quality);
}