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
package com.codename1.flutter.rendering;

import com.codename1.flutter.Rect;

/**
 * The base of the render tree — Flutter's {@code RenderObject}. new_gallery
 * reaches one via {@code BuildContext.findRenderObject()} and casts it to
 * {@link RenderBox}. This is a structural stub: the Codename One runtime lays
 * out with its own {@link com.codename1.flutter.RenderElement} tree, so the
 * geometry accessors return neutral values until a later rendering milestone
 * wires them to the live layout.
 */
public class RenderObject {

    /** Whether this render object is attached to the render tree. */
    public boolean attached() {
        return false;
    }

    /** The bounds painted by this object, in its own coordinate space. */
    public Rect paintBounds() {
        return Rect.zero;
    }

    /** The bounds used for semantics, in its own coordinate space. */
    public Rect semanticBounds() {
        return Rect.zero;
    }

    /** Marks this object as needing a repaint (no-op in this milestone). */
    public void markNeedsPaint() {
    }

    /** Marks this object as needing layout (no-op in this milestone). */
    public void markNeedsLayout() {
    }
}
