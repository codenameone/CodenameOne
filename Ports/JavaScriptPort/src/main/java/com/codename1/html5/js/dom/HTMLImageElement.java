/*
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 * Licensed under the PolyForm Noncommercial License 1.0.0
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * Interface for the JavaScript HTMLImageElement.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLImageElement
 */
public interface HTMLImageElement extends HTMLElement {
    String getSrc();
    void setSrc(String src);
    int getNaturalWidth();
    int getNaturalHeight();
    boolean isComplete();
    String getCrossOrigin();
    void setCrossOrigin(String crossOrigin);
}
