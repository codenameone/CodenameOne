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

/// Turns a node over time by animating its `rotate` angle, in degrees.
///
/// The start is the `from` value, or what the node has when the
/// transition starts. The end is the `to` value, or the start moved by
/// the `by` value. All of them are read when the transition starts.
/// The node turns in its own plane; there is no rotation axis.
public final class RotateTransition extends Transition {

    private final ObjectProperty<Duration> duration =
            new SimpleObjectProperty<Duration>(this, "duration", Duration.millis(400));
    private final ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
    private Node target;
    private final DoubleProperty fromAngle = new SimpleDoubleProperty(this, "fromAngle", Double.NaN);
    private final DoubleProperty toAngle = new SimpleDoubleProperty(this, "toAngle", Double.NaN);
    private final DoubleProperty byAngle = new SimpleDoubleProperty(this, "byAngle", 0.0);
    private double startAngle;
    private double deltaAngle;

    /// Creates a transition of a length for a node.
    public RotateTransition(Duration duration, Node node) {
        setDuration(duration);
        setNode(node);
    }

    /// Creates a transition of a length.
    public RotateTransition(Duration duration) {
        this(duration, null);
    }

    /// Creates a transition of 400 milliseconds.
    public RotateTransition() {
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

    /// Sets the angle the transition starts at; not a number, the default, starts at what the node has.
    public final void setFromAngle(double value) {
        fromAngle.set(value);
    }

    /// Returns the angle the transition starts at; not a number, the default, starts at what the node has.
    public final double getFromAngle() {
        return fromAngle.get();
    }

    /// The angle the transition starts at; not a number, the default, starts at what the node has.
    public final DoubleProperty fromAngleProperty() {
        return fromAngle;
    }

    /// Sets the angle the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final void setToAngle(double value) {
        toAngle.set(value);
    }

    /// Returns the angle the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final double getToAngle() {
        return toAngle.get();
    }

    /// The angle the transition ends at; not a number, the default, leaves the end to the `by` value.
    public final DoubleProperty toAngleProperty() {
        return toAngle;
    }

    /// Sets how far the angle moves from its start; used when no `to` value is set.
    public final void setByAngle(double value) {
        byAngle.set(value);
    }

    /// Returns how far the angle moves from its start; used when no `to` value is set.
    public final double getByAngle() {
        return byAngle.get();
    }

    /// How far the angle moves from its start; used when no `to` value is set.
    public final DoubleProperty byAngleProperty() {
        return byAngle;
    }

    @Override
    void prepare() {
        target = getNode() != null ? getNode() : getParentTargetNode();
        if (target == null) {
            return;
        }
        double fromAngle = getFromAngle();
        double toAngle = getToAngle();
        startAngle = Double.isNaN(fromAngle) ? target.getRotate() : fromAngle;
        deltaAngle = Double.isNaN(toAngle) ? getByAngle() : toAngle - startAngle;
    }

    @Override
    protected void interpolate(double frac) {
        if (target != null) {
            target.setRotate(startAngle + frac * deltaAngle);
        }
    }
}
