/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;

public interface HTMLInputElement extends HTMLElement {
    @JSProperty String getValue();
    @JSProperty void setValue(String value);
    @JSProperty String getType();
    @JSProperty void setType(String type);
    @JSProperty boolean isChecked();
    @JSProperty void setChecked(boolean checked);
    @JSProperty String getPlaceholder();
    @JSProperty void setPlaceholder(String placeholder);
    @JSProperty boolean isDisabled();
    @JSProperty void setDisabled(boolean disabled);
    @JSProperty String getAccept();
    @JSProperty void setAccept(String accept);
    @JSProperty int getMaxLength();
    @JSProperty void setMaxLength(int maxLength);
    void select();
    void focus();
    void blur();
}