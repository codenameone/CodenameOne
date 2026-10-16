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

import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.CustomPainter;
import com.codename1.flutter.rendering.Size;

/**
 * Provides a canvas for a {@link CustomPainter} to paint on, behind and/or in
 * front of an optional {@code child} — Flutter's {@code CustomPaint}. The
 * painters run against a Codename One {@code Graphics} through
 * {@link com.codename1.flutter.rendering.GraphicsCanvas}; see
 * {@link CustomPaintRenderElement}.
 */
public class CustomPaint extends Widget {

    private CustomPainter painter;
    private CustomPainter foregroundPainter;
    private Size size;
    private Widget child;

    public void painter(CustomPainter v) {
        this.painter = v;
    }

    public void foregroundPainter(CustomPainter v) {
        this.foregroundPainter = v;
    }

    public void size(Size v) {
        this.size = v;
    }

    public void isComplex(boolean v) {
    }

    public void willChange(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public CustomPainter getPainter() {
        return painter;
    }

    /** The painter drawn OVER the child — Flutter's {@code foregroundPainter}. */
    public CustomPainter getForegroundPainter() {
        return foregroundPainter;
    }

    /** The box the painter asks for when there is no child, in logical pixels. */
    public Size getSize() {
        return size;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new CustomPaintRenderElement(this);
    }
}
