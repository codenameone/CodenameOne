/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface HTMLFormElement extends HTMLElement {
    @JSProperty String getAction();
    @JSProperty void setAction(String action);
    @JSProperty String getMethod();
    @JSProperty void setMethod(String method);
    @JSProperty String getEnctype();
    @JSProperty void setEnctype(String enctype);
    @JSProperty String getTarget();
    @JSProperty void setTarget(String target);
    void submit();
    void reset();
    Object getElements();
    int getLength();
}