/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.core;

import com.codename1.html5.js.JSObject;

/**
 * JavaScript Array interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array
 */
public interface JSArray<T> extends JSObject {
    static <T> JSArray<T> create() {
        return null; // Native implementation
    }
    
    static <T> JSArray<T> create(int length) {
        return null; // Native implementation  
    }
    
    int getLength();
    T get(int index);
    void set(int index, T value);
    int push(T element);
    T pop();
    T shift();
    int unshift(T element);
    JSArray<T> slice(int start);
    JSArray<T> slice(int start, int end);
    JSArray<T> splice(int start);
    JSArray<T> splice(int start, int deleteCount);
    int indexOf(T element);
    int indexOf(T element, int fromIndex);
    boolean includes(T element);
    void forEach(Object callback);
    JSArray<T> filter(Object callback);
    JSArray<T> map(Object callback);
    T find(Object callback);
    int findIndex(Object callback);
    Object join();
    Object join(String separator);
    JSArray<T> concat(JSArray<T> other);
    void sort(Object compareFunction);
    void reverse();
}