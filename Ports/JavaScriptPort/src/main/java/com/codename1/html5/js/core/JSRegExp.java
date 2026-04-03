/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.core;

import com.codename1.html5.js.JSObject;

/**
 * JavaScript RegExp interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp
 */
public interface JSRegExp extends JSObject {
    boolean test(String str);
    Object exec(String str);
    String getSource();
    boolean isGlobal();
    boolean isIgnoreCase();
    boolean isMultiline();
    int getLastIndex();
    void setLastIndex(int index);
}