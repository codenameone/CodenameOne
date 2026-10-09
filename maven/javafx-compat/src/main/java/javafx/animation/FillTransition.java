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
package javafx.animation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

/// Changes the fill of a shape from one colour to another over a
/// duration.
///
/// Without a start colour the transition starts from the fill the shape
/// has when it is played, which has to be a colour; without an end colour
/// it ends where it started. The shape is the one set here, or the node
/// of the transition this one is a child of when that node is a shape.
public final class FillTransition extends Transition {

    private final ObjectProperty<Duration> duration =
            new SimpleObjectProperty<Duration>(this, "duration", Duration.millis(400));
    private final ObjectProperty<Shape> shape = new SimpleObjectProperty<Shape>(this, "shape");
    private final ObjectProperty<Color> fromValue = new SimpleObjectProperty<Color>(this, "fromValue");
    private final ObjectProperty<Color> toValue = new SimpleObjectProperty<Color>(this, "toValue");
    private Shape target;
    private Color start;
    private Color end;

    /// Creates a transition of a shape between two colours.
    public FillTransition(Duration duration, Shape shape, Color fromValue, Color toValue) {
        setDuration(duration);
        setShape(shape);
        setFromValue(fromValue);
        setToValue(toValue);
    }

    /// Creates a transition between two colours.
    public FillTransition(Duration duration, Color fromValue, Color toValue) {
        this(duration, null, fromValue, toValue);
    }

    /// Creates a transition of a shape.
    public FillTransition(Duration duration, Shape shape) {
        this(duration, shape, null, null);
    }

    /// Creates a transition of a duration.
    public FillTransition(Duration duration) {
        this(duration, null, null, null);
    }

    /// Creates a transition of 400 milliseconds.
    public FillTransition() {
        this(Duration.millis(400), null, null, null);
    }

    /// Sets how long the change takes.
    public final void setDuration(Duration value) {
        if (value == null) {
            throw new NullPointerException("Duration must not be null");
        }
        if (value.lessThan(Duration.ZERO) || value.isUnknown()) {
            throw new IllegalArgumentException("Cannot set duration to negative value.");
        }
        duration.set(value);
    }

    /// Returns how long the change takes.
    public final Duration getDuration() {
        return duration.get();
    }

    /// How long the change takes.
    public final ObjectProperty<Duration> durationProperty() {
        return duration;
    }

    @Override
    void syncDuration() {
        Duration d = duration.get();
        setCycleDuration(d == null || d.lessThan(Duration.ZERO) || d.isUnknown() ? Duration.ZERO : d);
    }

    /// Sets the shape whose fill changes.
    public final void setShape(Shape value) {
        shape.set(value);
    }

    /// Returns the shape whose fill changes.
    public final Shape getShape() {
        return shape.get();
    }

    /// The shape whose fill changes.
    public final ObjectProperty<Shape> shapeProperty() {
        return shape;
    }

    /// Sets the colour the change starts from.
    public final void setFromValue(Color value) {
        fromValue.set(value);
    }

    /// Returns the colour the change starts from.
    public final Color getFromValue() {
        return fromValue.get();
    }

    /// The colour the change starts from.
    public final ObjectProperty<Color> fromValueProperty() {
        return fromValue;
    }

    /// Sets the colour the change ends at.
    public final void setToValue(Color value) {
        toValue.set(value);
    }

    /// Returns the colour the change ends at.
    public final Color getToValue() {
        return toValue.get();
    }

    /// The colour the change ends at.
    public final ObjectProperty<Color> toValueProperty() {
        return toValue;
    }

    @Override
    void prepare() {
        target = getShape();
        if (target == null) {
            Node parent = getParentTargetNode();
            target = parent instanceof Shape ? (Shape) parent : null;
        }
        start = null;
        if (target == null) {
            return;
        }
        Paint now = target.getFill();
        start = getFromValue() != null ? getFromValue() : (now instanceof Color ? (Color) now : null);
        end = getToValue() != null ? getToValue() : start;
    }

    @Override
    protected void interpolate(double frac) {
        if (target != null && start != null && end != null) {
            target.setFill(start.interpolate(end, frac));
        }
    }
}
