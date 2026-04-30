/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * Node list interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/NodeList
 */
public interface NodeList extends JSObject {
    int getLength();
    Object item(int index);
}