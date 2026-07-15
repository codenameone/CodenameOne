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