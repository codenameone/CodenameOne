/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

import dart.runtime.Funcs;

/**
 * Render element for {@link GestureDetector} (and material InkWell). Owns no
 * component itself: it mounts the child at slot 0 and a synthesized
 * {@link GestureOverlay} at slot 1 whose transparent component covers the
 * child's bounds. Slot order puts the overlay's component AFTER the child
 * subtree in the flat container, so it sits on top for pointer dispatch.
 */
public class GestureRenderElement extends RenderElement {

    private Element childElement;
    private Element overlayElement;

    public GestureRenderElement(GestureDetector widget) {
        super(widget);
    }

    GestureDetector gesture() {
        return (GestureDetector) widget();
    }

    /** The wrapped content (slot 0) — the subtree the overlay must not shadow. */
    Element contentElement() {
        return childElement;
    }

    @Override
    protected void syncChildren() {
        childElement = updateChild(childElement, gesture().getChild(), 0);
        overlayElement = updateChild(overlayElement, new GestureOverlay(), 1);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (childElement != null) {
            visitor.call(childElement);
        }
        if (overlayElement != null) {
            visitor.call(overlayElement);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement child = findRenderElement(childElement);
        Size cs;
        if (child != null) {
            cs = child.layout(constraints);
            setChildOffset(child, 0, 0);
        } else {
            cs = constraints.smallest();
        }
        RenderElement overlay = findRenderElement(overlayElement);
        if (overlay != null) {
            overlay.layout(BoxConstraints.tight(cs.width(), cs.height()));
            setChildOffset(overlay, 0, 0);
        }
        return constraints.constrain(cs);
    }
}
