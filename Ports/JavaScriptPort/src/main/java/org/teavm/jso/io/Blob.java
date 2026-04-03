/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.typedarrays;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSBody;

public interface Blob extends JSObject {
    @JSProperty int getSize();
    @JSProperty String getType();
    ArrayBuffer slice(long start, long end, String contentType);
}