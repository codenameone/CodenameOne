/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * Base Node interface for DOM.
 * https://developer.mozilla.org/en-US/docs/Web/API/Node
 */
public interface Node extends JSObject {
    Node getParentNode();
    Node getFirstChild();
    Node getLastChild();
    Node getNextSibling();
    Node getPreviousSibling();
    void appendChild(Node child);
    Node insertBefore(Node newChild, Node refChild);
    Node removeChild(Node child);
    Node cloneNode(boolean deep);
}