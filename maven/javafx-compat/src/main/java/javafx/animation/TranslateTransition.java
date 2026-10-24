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

/// Moves a node over time by animating its `translateX` and
/// `translateY`.
///
/// The start is the `from` value, or what the node has when the
/// transition starts. The end is the `to` value, or the start moved by
/// the `by` value. All of them are read when the transition starts.
/// There is no third axis.
public final class TranslateTransition extends Transition {

    private final ObjectProperty<Duration> duration =
            new SimpleObjectProperty<Duration>(this, "duration", Duration.millis(400));
    private final ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
    private Node target;
    private final DoubleProperty fromX = new SimpleDoubleProperty(this, "fromX", Double.NaN);
    private final DoubleProperty toX = new SimpleDoubleProperty(this, "toX", Double.NaN);
    private final DoubleProperty byX = new SimpleDoubleProperty(this, "byX", 0.0);
    private double startX;
    private double deltaX;
    private final DoubleProperty fromY = new SimpleDoubleProperty(this, "fromY", Double.NaN);
    private final DoubleProperty toY = new SimpleDoubleProperty(this, "toY", Double.NaN);
    private final DoubleProperty byY = new SimpleDoubleProperty(this, "byY", 0.0);
    private double startY;
    private double deltaY;

    /// Creates a transition of a length for a node.
    public TranslateTransition(Duration duration, Node node) {
        setDuration(duration);
        setNode(node);
    }

    /// Creates a transition of a length.
    public TranslateTransition(Duration duration) {
        this(duration, null);
    }

    /// Creates a transition of 400 milliseconds.
    public TranslateTransition() {
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

    /// Sets the X translation the transition starts at; not a number, the default, starts at what the node has.
    public final void setFromX(double value) {
        fromX.set(value);
    }

    /// Returns the X translation the transition starts at; not a number, the default, starts at what the node has.
    public final double getFromX() {
        return fromX.get();
    }

    /// The X translation the transition starts at; not a number, the default, starts at what the node has.
    public final DoubleProperty fromXProperty() {
        return fromX;
    }

    /// Sets the X translation the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final void setToX(double value) {
        toX.set(value);
    }

    /// Returns the X translation the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final double getToX() {
        return toX.get();
    }

    /// The X translation the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final DoubleProperty toXProperty() {
        return toX;
    }

    /// Sets how far the X translation moves from its start; used when no `to` value is set.
    public final void setByX(double value) {
        byX.set(value);
    }

    /// Returns how far the X translation moves from its start; used when no `to` value is set.
    public final double getByX() {
        return byX.get();
    }

    /// How far the X translation moves from its start; used when no `to` value is set.
    public final DoubleProperty byXProperty() {
        return byX;
    }

    /// Sets the Y translation the transition starts at; not a number, the default, starts at what the node has.
    public final void setFromY(double value) {
        fromY.set(value);
    }

    /// Returns the Y translation the transition starts at; not a number, the default, starts at what the node has.
    public final double getFromY() {
        return fromY.get();
    }

    /// The Y translation the transition starts at; not a number, the default, starts at what the node has.
    public final DoubleProperty fromYProperty() {
        return fromY;
    }

    /// Sets the Y translation the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final void setToY(double value) {
        toY.set(value);
    }

    /// Returns the Y translation the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final double getToY() {
        return toY.get();
    }

    /// The Y translation the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final DoubleProperty toYProperty() {
        return toY;
    }

    /// Sets how far the Y translation moves from its start; used when no `to` value is set.
    public final void setByY(double value) {
        byY.set(value);
    }

    /// Returns how far the Y translation moves from its start; used when no `to` value is set.
    public final double getByY() {
        return byY.get();
    }

    /// How far the Y translation moves from its start; used when no `to` value is set.
    public final DoubleProperty byYProperty() {
        return byY;
    }

    @Override
    void prepare() {
        target = getNode() != null ? getNode() : getParentTargetNode();
        if (target == null) {
            return;
        }
        double fromX = getFromX();
        double toX = getToX();
        startX = Double.isNaN(fromX) ? target.getTranslateX() : fromX;
        deltaX = Double.isNaN(toX) ? getByX() : toX - startX;
        double fromY = getFromY();
        double toY = getToY();
        startY = Double.isNaN(fromY) ? target.getTranslateY() : fromY;
        deltaY = Double.isNaN(toY) ? getByY() : toY - startY;
    }

    @Override
    protected void interpolate(double frac) {
        if (target != null) {
            target.setTranslateX(startX + frac * deltaX);
            target.setTranslateY(startY + frac * deltaY);
        }
    }
}
