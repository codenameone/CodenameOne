/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * Interface for the JavaScript Element object.
 * https://developer.mozilla.org/en-US/docs/Web/API/Element
 */
public interface Element extends JSObject {
    String getTagName();
    String getId();
    void setId(String id);
    String getAttribute(String name);
    void setAttribute(String name, String value);
    void removeAttribute(String name);
    boolean hasAttribute(String name);
    void addEventListener(String type, Object listener);
    void addEventListener(String type, Object listener, boolean capture);
    void removeEventListener(String type, Object listener);
    void removeEventListener(String type, Object listener, boolean capture);
    Element getParentNode();
    Object getFirstChild();
    Object getLastChild();
    Object getNextSibling();
    Object getPreviousSibling();
    void appendChild(Object child);
    Element insertBefore(Object newChild, Object refChild);
    void removeChild(Object child);
    void setInnerHTML(String html);
    String getInnerHTML();
    void setTextContent(String text);
    String getTextContent();
    CSSStyleDeclaration getStyle();
    TextRectangle getBoundingClientRect();
    boolean dispatchEvent(Event evt);
    HTMLDocument getOwnerDocument();
}