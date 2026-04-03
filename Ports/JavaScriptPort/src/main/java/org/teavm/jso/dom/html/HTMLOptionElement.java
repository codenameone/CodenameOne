/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;

public interface HTMLOptionElement extends HTMLElement {
    @JSProperty String getValue();
    @JSProperty void setValue(String value);
    @JSProperty String getText();
    @JSProperty void setText(String text);
    @JSProperty boolean isSelected();
    @JSProperty void setSelected(boolean selected);
}