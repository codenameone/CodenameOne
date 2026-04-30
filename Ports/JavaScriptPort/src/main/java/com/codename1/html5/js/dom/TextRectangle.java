/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * TextRectangle (ClientRect/DOMRect) interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/DOMRect
 */
public interface TextRectangle extends JSObject {
    int getLeft();
    int getTop();
    int getRight();
    int getBottom();
    int getWidth();
    int getHeight();
}