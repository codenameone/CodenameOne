/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;

public interface TextRectangle extends JSObject {
    int getTop();
    int getLeft();
    int getWidth();
    int getHeight();
    int getBottom();
    int getRight();
}