/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;

public interface NodeList<T extends JSObject> extends JSObject {
    int getLength();
    T get(int index);
    T item(int index);
}