/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

public interface HTMLSelectElement extends HTMLElement {
    int getSelectedIndex();
    void setSelectedIndex(int index);
    HTMLOptionElement getOptions();
}