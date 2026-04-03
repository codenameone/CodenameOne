/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;

public interface File extends Blob {
    String getName();
    long getLastModified();
}