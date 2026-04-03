/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface HTMLImageElement extends HTMLElement {
    @JSProperty String getSrc();
    @JSProperty void setSrc(String src);
    @JSProperty int getWidth();
    @JSProperty int getHeight();
    @JSProperty int getNaturalWidth();
    @JSProperty int getNaturalHeight();
    @JSProperty boolean isComplete();
}