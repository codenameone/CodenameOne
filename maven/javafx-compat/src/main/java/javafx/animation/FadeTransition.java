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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.util.Duration;

/// Changes the opacity of a node over time.
///
/// The start is the `from` value, or what the node has when the
/// transition starts. The end is the `to` value, or the start moved by
/// the `by` value. All of them are read when the transition starts.
/// Opacity stays between 0 and 1.
public final class FadeTransition extends Transition {

    private final ObjectProperty<Duration> duration =
            new SimpleObjectProperty<Duration>(this, "duration", Duration.millis(400));
    private final ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
    private Node target;
    private final DoubleProperty fromValue = new SimpleDoubleProperty(this, "fromValue", Double.NaN);
    private final DoubleProperty toValue = new SimpleDoubleProperty(this, "toValue", Double.NaN);
    private final DoubleProperty byValue = new SimpleDoubleProperty(this, "byValue", 0.0);
    private double startValue;
    private double deltaValue;

    /// Creates a transition of a length for a node.
    public FadeTransition(Duration duration, Node node) {
        setDuration(duration);
        setNode(node);
    }

    /// Creates a transition of a length.
    public FadeTransition(Duration duration) {
        this(duration, null);
    }

    /// Creates a transition of 400 milliseconds.
    public FadeTransition() {
        this(Duration.millis(400), null);
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

    /// Sets the node that is animated. Without one the transition
    /// animates the node of the nearest parent transition that names
    /// one. It is read when the transition starts.
    public final void setNode(Node value) {
        node.set(value);
    }

    /// Returns the node that is animated.
    public final Node getNode() {
        return node.get();
    }

    /// The node that is animated.
    public final ObjectProperty<Node> nodeProperty() {
        return node;
    }

    /// Sets the opacity the transition starts at; not a number, the default, starts at what the node has.
    public final void setFromValue(double value) {
        fromValue.set(value);
    }

    /// Returns the opacity the transition starts at; not a number, the default, starts at what the node has.
    public final double getFromValue() {
        return fromValue.get();
    }

    /// The opacity the transition starts at; not a number, the default, starts at what the node has.
    public final DoubleProperty fromValueProperty() {
        return fromValue;
    }

    /// Sets the opacity the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final void setToValue(double value) {
        toValue.set(value);
    }

    /// Returns the opacity the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final double getToValue() {
        return toValue.get();
    }

    /// The opacity the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final DoubleProperty toValueProperty() {
        return toValue;
    }

    /// Sets how far the opacity moves from its start; used when no `to` value is set.
    public final void setByValue(double value) {
        byValue.set(value);
    }

    /// Returns how far the opacity moves from its start; used when no `to` value is set.
    public final double getByValue() {
        return byValue.get();
    }

    /// How far the opacity moves from its start; used when no `to` value is set.
    public final DoubleProperty byValueProperty() {
        return byValue;
    }

    @Override
    void prepare() {
        target = getNode() != null ? getNode() : getParentTargetNode();
        if (target == null) {
            return;
        }
        double fromValue = getFromValue();
        double toValue = getToValue();
        startValue = Double.isNaN(fromValue) ? target.getOpacity() : clamp(fromValue);
        deltaValue = Double.isNaN(toValue) ? getByValue() : clamp(toValue) - startValue;
        startValue = clamp(startValue);
        deltaValue = clamp(startValue + deltaValue) - startValue;
    }

    @Override
    protected void interpolate(double frac) {
        if (target != null) {
            target.setOpacity(clamp(startValue + frac * deltaValue));
        }
    }

    private static double clamp(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }
}
