/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;

public interface HTMLLinkElement extends HTMLElement {
    @JSProperty String getHref();
    @JSProperty void setHref(String href);
    @JSProperty String getRel();
    @JSProperty void setRel(String rel);
    @JSProperty String getType();
    @JSProperty void setType(String type);
}