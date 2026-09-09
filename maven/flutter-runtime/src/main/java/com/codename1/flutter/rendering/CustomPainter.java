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

import com.codename1.flutter.Canvas;
import com.codename1.flutter.semantics.SemanticsBuilderCallback;

/**
 * Base class an application implements to paint directly onto a {@link Canvas}
 * — Flutter's {@code CustomPainter}. Subclasses override {@link #paint} to draw
 * and {@link #shouldRepaint} to decide when a re-paint is required. The
 * optional {@code repaint} listenable (a Listenable that triggers repaints) is
 * captured for API shape.
 */
public abstract class CustomPainter {

    private Object repaint;

    public CustomPainter() {
    }

    public void repaint(Object v) {
        this.repaint = v;
    }

    /**
     * Draws this painter's content within a box of the given {@code size}.
     */
    public abstract void paint(Canvas canvas, Size size);

    /**
     * Whether a repaint is needed when the delegate is replaced by
     * {@code oldDelegate}. Dart subclasses narrow the parameter type
     * ({@code covariant}), which becomes an overload rather than an override in
     * Java; this default keeps the base concrete so those subclasses compile.
     */
    public boolean shouldRepaint(CustomPainter oldDelegate) {
        return true;
    }

    /**
     * Returns the callback that produces this painter's accessibility nodes, or
     * {@code null} when the painter contributes no custom semantics — Flutter's
     * {@code CustomPainter.semanticsBuilder}. Painters that annotate their
     * drawing for screen readers (e.g. the Rally line chart) override this to
     * return a {@link SemanticsBuilderCallback}.
     */
    public SemanticsBuilderCallback semanticsBuilder() {
        return null;
    }
}
