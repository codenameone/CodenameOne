/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.core;

import com.codename1.html5.js.JSObject;

/**
 * JavaScript String interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String
 */
public interface JSString extends JSObject {
    static JSString valueOf(String str) {
        return null; // Native implementation
    }
    
    int getLength();
    JSString charAt(int index);
    int charCodeAt(int index);
    int codePointAt(int index);
    JSString concat(JSString other);
    boolean contains(JSString search);
    boolean endsWith(JSString search);
    int indexOf(JSString search);
    int indexOf(JSString search, int fromIndex);
    int lastIndexOf(JSString search);
    int lastIndexOf(JSString search, int fromIndex);
    JSString slice(int start);
    JSString slice(int start, int end);
    JSString substring(int start);
    JSString substring(int start, int end);
    JSString toLowerCase();
    JSString toUpperCase();
    JSString trim();
    JSString[] split(JSString separator);
    boolean startsWith(JSString search);
    String stringValue();
}