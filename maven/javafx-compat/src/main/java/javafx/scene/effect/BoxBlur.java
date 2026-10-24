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
package javafx.scene.effect;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/// A box blur of a node. **Recorded and not drawn**: a node with one is
/// painted sharp, see [Effect].
public class BoxBlur extends Effect {

    private final ObjectProperty<Effect> input = new SimpleObjectProperty<Effect>(this, "input");
    private final DoubleProperty width = new SimpleDoubleProperty(this, "width", 5);
    private final DoubleProperty height = new SimpleDoubleProperty(this, "height", 5);
    private final IntegerProperty iterations = new SimpleIntegerProperty(this, "iterations", 1);

    /// Creates a blur five wide, five high, run once.
    public BoxBlur() {
    }

    /// Creates a blur of a width, a height and a number of passes.
    public BoxBlur(double width, double height, int iterations) {
        setWidth(width);
        setHeight(height);
        setIterations(iterations);
    }

    /// Returns the effect this one is applied on top of.
    public final Effect getInput() {
        return input.get();
    }

    /// Sets the effect this one is applied on top of.
    public final void setInput(Effect value) {
        input.set(value);
    }

    /// The effect this one is applied on top of.
    public final ObjectProperty<Effect> inputProperty() {
        return input;
    }

    /// Returns the width of the blur, from 0 to 255.
    public final double getWidth() {
        return width.get();
    }

    /// Sets the width of the blur.
    public final void setWidth(double value) {
        width.set(DropShadow.clamp(value, 0, 255));
    }

    /// The width of the blur.
    public final DoubleProperty widthProperty() {
        return width;
    }

    /// Returns the height of the blur, from 0 to 255.
    public final double getHeight() {
        return height.get();
    }

    /// Sets the height of the blur.
    public final void setHeight(double value) {
        height.set(DropShadow.clamp(value, 0, 255));
    }

    /// The height of the blur.
    public final DoubleProperty heightProperty() {
        return height;
    }

    /// Returns the number of passes, from 0 to 3.
    public final int getIterations() {
        return iterations.get();
    }

    /// Sets the number of passes.
    public final void setIterations(int value) {
        iterations.set(value < 0 ? 0 : (value > 3 ? 3 : value));
    }

    /// The number of passes.
    public final IntegerProperty iterationsProperty() {
        return iterations;
    }
}
