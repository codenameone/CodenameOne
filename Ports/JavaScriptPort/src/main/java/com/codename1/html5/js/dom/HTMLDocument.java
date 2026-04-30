/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;

/**
 * HTML Document interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/Document
 */
public interface HTMLDocument extends Document {
    HTMLElement getDocumentElement();
    HTMLElement createElement(String tagName);
    HTMLElement getElementById(String id);
    HTMLCanvasElement createCanvasElement();
    HTMLImageElement createImageElement();
    HTMLInputElement createInputElement();
    HTMLTextAreaElement createTextAreaElement();
    HTMLButtonElement createButtonElement();
    TextRectangle getBoundingClientRect();
    NodeList querySelectorAll(String selector);
    void addEventListener(String type, Object listener);
    Object createTextNode(String text);
    Object createComment(String text);
}