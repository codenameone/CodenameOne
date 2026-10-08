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
package javafx.scene.canvas;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;

/// A rectangle that is drawn on through a [GraphicsContext].
///
/// JavaFX keeps the pixels of a canvas in an image. This layer keeps the
/// drawing calls instead and replays them whenever the canvas is painted,
/// through the same renderer the shapes use, so what was drawn persists
/// and stays sharp at any display scale. [GraphicsContext] explains how
/// the list of calls is kept from growing without bound.
///
/// A canvas is transparent where nothing was drawn, is not resizable by a
/// layout and is hit anywhere inside its bounds.
public class Canvas extends Node {

    private final DoubleProperty width = new FxDouble(this, "width", 0, Dirty.GEOMETRY);
    private final DoubleProperty height = new FxDouble(this, "height", 0, Dirty.GEOMETRY);
    private GraphicsContext context;

    /// Creates a canvas of no size.
    public Canvas() {
    }

    /// Creates a canvas of a size.
    public Canvas(double width, double height) {
        this.width.set(width);
        this.height.set(height);
    }

    /// Returns the one context that draws on this canvas.
    public GraphicsContext getGraphicsContext2D() {
        if (context == null) {
            context = new GraphicsContext(this);
        }
        return context;
    }

    /// Returns the width.
    public final double getWidth() {
        return width.get();
    }

    /// Sets the width. What was drawn is kept.
    public final void setWidth(double value) {
        width.set(value);
    }

    /// The width.
    public final DoubleProperty widthProperty() {
        return width;
    }

    /// Returns the height.
    public final double getHeight() {
        return height.get();
    }

    /// Sets the height. What was drawn is kept.
    public final void setHeight(double value) {
        height.set(value);
    }

    /// The height.
    public final DoubleProperty heightProperty() {
        return height;
    }

    @Override
    protected Bounds cn1ComputeLayoutBounds() {
        return new BoundingBox(0, 0, Math.max(0, getWidth()), Math.max(0, getHeight()));
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        if (context != null) {
            context.replay(renderer);
        }
    }
}
