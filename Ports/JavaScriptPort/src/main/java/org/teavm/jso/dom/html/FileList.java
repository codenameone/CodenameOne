/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;

public interface FileList extends JSObject {
    int getLength();
    JSObject item(int index);
    Object getFile(int index);
}