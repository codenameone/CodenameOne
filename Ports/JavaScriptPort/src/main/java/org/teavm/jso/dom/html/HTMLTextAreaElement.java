/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;

public interface HTMLTextAreaElement extends HTMLElement {
    @JSProperty String getValue();
    @JSProperty void setValue(String value);
    @JSProperty int getRows();
    @JSProperty void setRows(int rows);
    @JSProperty int getCols();
    @JSProperty void setCols(int cols);
    @JSProperty String getPlaceholder();
    @JSProperty void setPlaceholder(String placeholder);
    void select();
    void focus();
    void blur();
}