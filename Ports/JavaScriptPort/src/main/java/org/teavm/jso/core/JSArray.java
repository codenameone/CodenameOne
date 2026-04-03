/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.core;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSBody;

public interface JSArray<T extends JSObject> extends JSObject {
    int getLength();
    T get(int index);
    void set(int index, T value);
    T push(T value);
    T pop();
    JSArray<T> concat(JSArray<T> other);
    JSArray<T> slice(int start);
    JSArray<T> slice(int start, int end);
    void splice(int start, int deleteCount);
    
    @JSBody(params = {}, script = "return []")
    static <T extends JSObject> JSArray<T> create() { return null; }
    
    @JSBody(params = {"size"}, script = "return new Array(size)")
    static <T extends JSObject> JSArray<T> create(int size) { return null; }
}