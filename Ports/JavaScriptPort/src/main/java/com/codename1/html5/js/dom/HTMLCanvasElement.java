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
    /// Generic context accessor returning the raw context object. Used for WebGL,
    /// where the context is not a 2D context and a context-attributes object
    /// (e.g. `{preserveDrawingBuffer:true}`) must be supplied. Maps to the
    /// standard `HTMLCanvasElement.getContext(contextType, attributes)`.
    JSObject getContext(String contextId, JSObject options);
    String toDataURL(String type);
    String toDataURL(String type, double quality);
}
