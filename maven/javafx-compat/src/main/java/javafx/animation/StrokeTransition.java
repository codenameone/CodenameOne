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

/// Changes the stroke of a shape from one colour to another over time.
///
/// The start is the `from` value, or the stroke the shape has when the
/// transition starts, if that is a colour. The end is the `to` value, or
/// the start. All of them are read when the transition starts. With
/// neither a `from` nor a `to` value, or with no `from` value and a
/// stroke that is not a plain colour, there is nothing to animate and
/// the shape is left as it is.
public final class StrokeTransition extends Transition {

    private final ObjectProperty<Duration> duration =
            new SimpleObjectProperty<Duration>(this, "duration", Duration.millis(400));
    private final ObjectProperty<Shape> shape = new SimpleObjectProperty<Shape>(this, "shape");
    private final ObjectProperty<Color> fromValue = new SimpleObjectProperty<Color>(this, "fromValue");
    private final ObjectProperty<Color> toValue = new SimpleObjectProperty<Color>(this, "toValue");
    private Shape target;
    private Color start;
    private Color end;

    /// Creates a transition of a length for a shape, between two colours.
    public StrokeTransition(Duration duration, Shape shape, Color fromValue, Color toValue) {
        setDuration(duration);
        setShape(shape);
        setFromValue(fromValue);
        setToValue(toValue);
    }

    /// Creates a transition of a length between two colours.
    public StrokeTransition(Duration duration, Color fromValue, Color toValue) {
        this(duration, null, fromValue, toValue);
    }

    /// Creates a transition of a length for a shape.
    public StrokeTransition(Duration duration, Shape shape) {
        this(duration, shape, null, null);
    }

    /// Creates a transition of a length.
    public StrokeTransition(Duration duration) {
        this(duration, null, null, null);
    }

    /// Creates a transition of 400 milliseconds.
    public StrokeTransition() {
        this(Duration.millis(400), null, null, null);
    }

    /// Sets the length of the transition. It is read when the
    /// transition starts.
    public final void setDuration(Duration value) {
        if (value == null) {
            throw new NullPointerException("Duration must not be null");
        }
        if (value.lessThan(Duration.ZERO) || value.isUnknown()) {
            throw new IllegalArgumentException("Cannot set duration to negative value.");
        }
        duration.set(value);
    }

    /// Returns the length of the transition.
    public final Duration getDuration() {
        return duration.get();
    }

    /// The length of the transition; 400 milliseconds unless set.
    public final ObjectProperty<Duration> durationProperty() {
        return duration;
    }

    @Override
    void syncDuration() {
        Duration d = duration.get();
        setCycleDuration(d == null || d.lessThan(Duration.ZERO) || d.isUnknown() ? Duration.ZERO : d);
    }

    /// Sets the shape that is animated. Without one the transition
    /// animates the node of the nearest parent transition that names
    /// one, if that is a shape. It is read when the transition starts.
    public final void setShape(Shape value) {
        shape.set(value);
    }

    /// Returns the shape that is animated.
    public final Shape getShape() {
        return shape.get();
    }

    /// The shape that is animated.
    public final ObjectProperty<Shape> shapeProperty() {
        return shape;
    }

    /// Sets the colour the transition starts at; `null`, the default,
    /// starts at the stroke the shape has.
    public final void setFromValue(Color value) {
        fromValue.set(value);
    }

    /// Returns the colour the transition starts at.
    public final Color getFromValue() {
        return fromValue.get();
    }

    /// The colour the transition starts at; `null`, the default, starts
    /// at the stroke the shape has.
    public final ObjectProperty<Color> fromValueProperty() {
        return fromValue;
    }

    /// Sets the colour the transition ends at; `null`, the default, ends
    /// where it starts.
    public final void setToValue(Color value) {
        toValue.set(value);
    }

    /// Returns the colour the transition ends at.
    public final Color getToValue() {
        return toValue.get();
    }

    /// The colour the transition ends at; `null`, the default, ends
    /// where it starts.
    public final ObjectProperty<Color> toValueProperty() {
        return toValue;
    }

    @Override
    void prepare() {
        target = null;
        Shape s = getShape();
        if (s == null) {
            Node inherited = getParentTargetNode();
            if (inherited instanceof Shape) {
                s = (Shape) inherited;
            }
        }
        Color from = getFromValue();
        Color to = getToValue();
        if (s == null || from == null && to == null) {
            return;
        }
        if (from == null) {
            Paint now = s.getStroke();
            if (!(now instanceof Color)) {
                return;
            }
            from = (Color) now;
        }
        start = from;
        end = to == null ? from : to;
        target = s;
    }

    @Override
    protected void interpolate(double frac) {
        if (target != null) {
            // The ends are the colours asked for, not a rounding of them.
            target.setStroke(frac <= 0 ? start : frac >= 1 ? end : start.interpolate(end, frac));
        }
    }
}
