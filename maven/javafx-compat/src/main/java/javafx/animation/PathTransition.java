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

import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.util.MathUtil;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

/// Moves a node along the outline of a shape over time.
///
/// The centre of the node's layout bounds follows the outline, through
/// the node's `translateX` and `translateY`: the outline is read in the
/// coordinates of the shape, as the coordinates of the node's parent.
/// With [OrientationType#ORTHOGONAL_TO_TANGENT] the node is turned as
/// well, through `rotate`, to face along the outline.
///
/// The outline is read when the transition starts, curves as short
/// lines no further than a tenth of a pixel from them. An outline of
/// several parts is travelled one part after the other, a gap between
/// two parts taking no time.
public final class PathTransition extends Transition {

    /// How the node is turned as it moves.
    public enum OrientationType {
        /// The node keeps the rotation it has.
        NONE,
        /// The node is turned to face along the outline.
        ORTHOGONAL_TO_TANGENT
    }

    private final ObjectProperty<Duration> duration =
            new SimpleObjectProperty<Duration>(this, "duration", Duration.millis(400));
    private final ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
    private final ObjectProperty<Shape> path = new SimpleObjectProperty<Shape>(this, "path");
    private final ObjectProperty<OrientationType> orientation =
            new SimpleObjectProperty<OrientationType>(this, "orientation", OrientationType.NONE);

    private Node target;
    private boolean turn;
    /// The lines of the outline, four numbers each, and the length
    /// travelled at the end of each.
    private double[] lines = new double[0];
    private double[] travelled = new double[0];
    private int lineCount;
    private double pivotX;
    private double pivotY;

    /// Creates a transition of a length along a shape for a node.
    public PathTransition(Duration duration, Shape path, Node node) {
        setDuration(duration);
        setPath(path);
        setNode(node);
    }

    /// Creates a transition of a length along a shape.
    public PathTransition(Duration duration, Shape path) {
        this(duration, path, null);
    }

    /// Creates a transition of 400 milliseconds.
    public PathTransition() {
        this(Duration.millis(400), null, null);
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

    /// Sets the node that is moved. Without one the transition moves the
    /// node of the nearest parent transition that names one. It is read
    /// when the transition starts.
    public final void setNode(Node value) {
        node.set(value);
    }

    /// Returns the node that is moved.
    public final Node getNode() {
        return node.get();
    }

    /// The node that is moved.
    public final ObjectProperty<Node> nodeProperty() {
        return node;
    }

    /// Sets the shape whose outline the node follows. It is read when
    /// the transition starts.
    public final void setPath(Shape value) {
        path.set(value);
    }

    /// Returns the shape whose outline the node follows.
    public final Shape getPath() {
        return path.get();
    }

    /// The shape whose outline the node follows.
    public final ObjectProperty<Shape> pathProperty() {
        return path;
    }

    /// Sets how the node is turned as it moves.
    public final void setOrientation(OrientationType value) {
        orientation.set(value);
    }

    /// Returns how the node is turned as it moves.
    public final OrientationType getOrientation() {
        OrientationType o = orientation.get();
        return o == null ? OrientationType.NONE : o;
    }

    /// How the node is turned as it moves; `NONE` unless set.
    public final ObjectProperty<OrientationType> orientationProperty() {
        return orientation;
    }

    @Override
    void prepare() {
        target = null;
        lineCount = 0;
        Node n = getNode() != null ? getNode() : getParentTargetNode();
        Shape shape = getPath();
        if (n == null || shape == null) {
            return;
        }
        FxPath outline = shape.cn1Outline();
        if (outline == null || outline.isEmpty()) {
            return;
        }
        FxPath flat = outline.flatten(0.1);
        int commands = flat.commandCount();
        double[] p = flat.points();
        lines = new double[commands * 4];
        travelled = new double[commands];
        int at = 0;
        double x = 0;
        double y = 0;
        double startX = 0;
        double startY = 0;
        double total = 0;
        for (int i = 0; i < commands; i++) {
            byte command = flat.command(i);
            double toX = x;
            double toY = y;
            boolean line = false;
            if (command == FxPath.MOVE) {
                x = p[at++];
                y = p[at++];
                startX = x;
                startY = y;
            } else if (command == FxPath.LINE) {
                toX = p[at++];
                toY = p[at++];
                line = true;
            } else if (command == FxPath.CLOSE) {
                toX = startX;
                toY = startY;
                line = true;
            } else if (command == FxPath.QUAD) {
                // A flattened outline has no curve left; were there one, its end is where it leads.
                at += 2;
                toX = p[at++];
                toY = p[at++];
                line = true;
            } else if (command == FxPath.CUBIC) {
                at += 4;
                toX = p[at++];
                toY = p[at++];
                line = true;
            }
            if (line) {
                double dx = toX - x;
                double dy = toY - y;
                double length = Math.sqrt(dx * dx + dy * dy);
                if (length > 0) {
                    int o = lineCount * 4;
                    lines[o] = x;
                    lines[o + 1] = y;
                    lines[o + 2] = toX;
                    lines[o + 3] = toY;
                    total += length;
                    travelled[lineCount] = total;
                    lineCount++;
                }
                x = toX;
                y = toY;
            }
        }
        if (lineCount == 0) {
            // An outline that is one point: the node goes there and stays.
            lines = new double[] {startX, startY, startX, startY};
            travelled = new double[] {0};
            lineCount = 1;
        }
        Bounds b = n.getLayoutBounds();
        pivotX = b.getMinX() + b.getWidth() / 2;
        pivotY = b.getMinY() + b.getHeight() / 2;
        turn = getOrientation() == OrientationType.ORTHOGONAL_TO_TANGENT;
        target = n;
    }

    @Override
    protected void interpolate(double frac) {
        if (target == null || lineCount == 0) {
            return;
        }
        double total = travelled[lineCount - 1];
        double f = frac < 0 ? 0 : frac > 1 ? 1 : frac;
        double want = total * f;
        // The first line that ends at or beyond the length wanted.
        int lo = 0;
        int hi = lineCount - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (travelled[mid] < want) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        int o = lo * 4;
        double before = lo == 0 ? 0 : travelled[lo - 1];
        double length = travelled[lo] - before;
        double along = length <= 0 ? 0 : (want - before) / length;
        double x1 = lines[o];
        double y1 = lines[o + 1];
        double x2 = lines[o + 2];
        double y2 = lines[o + 3];
        target.setTranslateX(x1 + (x2 - x1) * along - pivotX);
        target.setTranslateY(y1 + (y2 - y1) * along - pivotY);
        if (turn && length > 0) {
            target.setRotate(Math.toDegrees(MathUtil.atan2(y2 - y1, x2 - x1)));
        }
    }
}
