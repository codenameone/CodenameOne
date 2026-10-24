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
package javafx.scene.layout;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;

/// Places up to five children: one along each edge and one in the middle.
///
/// The top and bottom children span the whole width and take their
/// preferred height. The left and right children take their preferred
/// width and the height between top and bottom. The center child gets
/// what is left and is stretched to it, up to its maximum size. When the
/// pane is too small the edges keep their size and the center gives way.
///
/// Setting one of the five properties adds the node to the children and
/// removes the node it replaces. A node that does not fill its area is
/// placed by its alignment constraint; without one the top and left sit
/// top left, the bottom sits bottom left, the right top right and the
/// center in the middle.
public class BorderPane extends Pane {

    private static final String MARGIN = "borderpane-margin";
    private static final String ALIGNMENT = "borderpane-alignment";

    private final ObjectProperty<Node> center = new Slot("center");
    private final ObjectProperty<Node> top = new Slot("top");
    private final ObjectProperty<Node> bottom = new Slot("bottom");
    private final ObjectProperty<Node> left = new Slot("left");
    private final ObjectProperty<Node> right = new Slot("right");

    /// Creates an empty border pane.
    public BorderPane() {
    }

    /// Creates a border pane with a center.
    public BorderPane(Node center) {
        this.center.set(center);
    }

    /// Creates a border pane with a node for each position; any of them
    /// may be `null`.
    public BorderPane(Node center, Node top, Node right, Node bottom, Node left) {
        this.center.set(center);
        this.top.set(top);
        this.right.set(right);
        this.bottom.set(bottom);
        this.left.set(left);
    }

    /// One of the five positions: keeps the list of children in step.
    private final class Slot extends SimpleObjectProperty<Node> {
        private Node shown;

        Slot(String name) {
            super(BorderPane.this, name);
        }

        @Override
        protected void invalidated() {
            Node now = get();
            if (now == shown) {
                return;
            }
            ObservableList<Node> children = cn1Children();
            Node before = shown;
            shown = now;
            if (before != null) {
                children.remove(before);
            }
            if (now != null && !children.contains(now)) {
                children.add(now);
            }
            requestLayout();
        }
    }

    /// Sets where a child sits in its area of a border pane; `null`
    /// removes the constraint.
    public static void setAlignment(Node child, Pos value) {
        setConstraint(child, ALIGNMENT, value);
    }

    /// Returns the alignment constraint of a child, or `null`.
    public static Pos getAlignment(Node child) {
        return LayoutSupport.pos(child, ALIGNMENT);
    }

    /// Sets the space kept free around a child in a border pane; `null`
    /// removes the constraint.
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN, value);
    }

    /// Returns the margin of a child, or `null`.
    public static Insets getMargin(Node child) {
        return LayoutSupport.insets(child, MARGIN);
    }

    /// Removes the border pane constraints from a child.
    public static void clearConstraints(Node child) {
        setAlignment(child, null);
        setMargin(child, null);
    }

    /// Returns the node in the middle, or `null`.
    public final Node getCenter() {
        return center.get();
    }

    /// Sets the node in the middle.
    public final void setCenter(Node value) {
        center.set(value);
    }

    /// The node in the middle.
    public final ObjectProperty<Node> centerProperty() {
        return center;
    }

    /// Returns the node along the top edge, or `null`.
    public final Node getTop() {
        return top.get();
    }

    /// Sets the node along the top edge.
    public final void setTop(Node value) {
        top.set(value);
    }

    /// The node along the top edge.
    public final ObjectProperty<Node> topProperty() {
        return top;
    }

    /// Returns the node along the bottom edge, or `null`.
    public final Node getBottom() {
        return bottom.get();
    }

    /// Sets the node along the bottom edge.
    public final void setBottom(Node value) {
        bottom.set(value);
    }

    /// The node along the bottom edge.
    public final ObjectProperty<Node> bottomProperty() {
        return bottom;
    }

    /// Returns the node along the left edge, or `null`.
    public final Node getLeft() {
        return left.get();
    }

    /// Sets the node along the left edge.
    public final void setLeft(Node value) {
        left.set(value);
    }

    /// The node along the left edge.
    public final ObjectProperty<Node> leftProperty() {
        return left;
    }

    /// Returns the node along the right edge, or `null`.
    public final Node getRight() {
        return right.get();
    }

    /// Sets the node along the right edge.
    public final void setRight(Node value) {
        right.set(value);
    }

    /// The node along the right edge.
    public final ObjectProperty<Node> rightProperty() {
        return right;
    }

    /// A position's node when layout places it: managed and still a child.
    private Node placed(ObjectProperty<Node> slot) {
        Node n = slot.get();
        return n != null && n.isManaged() && n.getParent() == this ? n : null;
    }

    private static boolean biased(Node n, Orientation bias) {
        return n != null && n.getContentBias() == bias;
    }

    @Override
    public Orientation getContentBias() {
        Node c = placed(center);
        if (c != null && c.getContentBias() != null) {
            return c.getContentBias();
        }
        if (biased(placed(right), Orientation.VERTICAL) || biased(placed(left), Orientation.VERTICAL)) {
            return Orientation.VERTICAL;
        }
        if (biased(placed(bottom), Orientation.HORIZONTAL) || biased(placed(top), Orientation.HORIZONTAL)) {
            return Orientation.HORIZONTAL;
        }
        return null;
    }

    private double areaWidth(Node child, double height, boolean minimum) {
        if (child == null) {
            return 0;
        }
        Insets m = getMargin(child);
        return minimum ? computeChildMinAreaWidth(child, m, height) : computeChildPrefAreaWidth(child, m, height);
    }

    private double areaHeight(Node child, double width, boolean minimum) {
        if (child == null) {
            return 0;
        }
        Insets m = getMargin(child);
        return minimum ? computeChildMinAreaHeight(child, m, width) : computeChildPrefAreaHeight(child, m, width);
    }

    private double contentWidth(double height, boolean minimum) {
        Node t = placed(top);
        Node b = placed(bottom);
        Node l = placed(left);
        Node r = placed(right);
        Node c = placed(center);
        double middle = -1;
        if (height != -1 && (biased(l, Orientation.VERTICAL) || biased(r, Orientation.VERTICAL)
                || biased(c, Orientation.VERTICAL))) {
            Insets in = getInsets();
            middle = Math.max(0, height - in.getTop() - in.getBottom() - areaHeight(t, -1, false)
                    - areaHeight(b, -1, false));
        }
        double row = areaWidth(l, middle, minimum) + areaWidth(c, middle, minimum) + areaWidth(r, middle, minimum);
        return Math.max(row, Math.max(areaWidth(t, -1, minimum), areaWidth(b, -1, minimum)));
    }

    private double contentHeight(double width, boolean minimum) {
        Node t = placed(top);
        Node b = placed(bottom);
        Node l = placed(left);
        Node r = placed(right);
        Node c = placed(center);
        double inside = -1;
        double middle = -1;
        if (width != -1) {
            Insets in = getInsets();
            inside = Math.max(0, width - in.getLeft() - in.getRight());
            if (biased(c, Orientation.HORIZONTAL)) {
                middle = Math.max(0, inside - areaWidth(l, -1, false) - areaWidth(r, -1, false));
            }
        }
        double row = Math.max(areaHeight(c, middle, minimum),
                Math.max(areaHeight(l, -1, minimum), areaHeight(r, -1, minimum)));
        return areaHeight(t, inside, minimum) + row + areaHeight(b, inside, minimum);
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + contentWidth(height, true) + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + contentHeight(width, true) + in.getBottom();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + contentWidth(height, false) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + contentHeight(width, false) + in.getBottom();
    }

    private static double horizontal(Insets m) {
        return m == null ? 0 : m.getLeft() + m.getRight();
    }

    private static double vertical(Insets m) {
        return m == null ? 0 : m.getTop() + m.getBottom();
    }

    /// Sizes an edge child to an area less its margin and answers the
    /// size it took: a resizable child fills the area within its minimum
    /// and maximum.
    private double[] sizeEdge(Node child, double areaWidth, double areaHeight, Insets margin) {
        double w = Math.max(0, areaWidth - horizontal(margin));
        double h = Math.max(0, areaHeight - vertical(margin));
        if (child.isResizable()) {
            Orientation bias = child.getContentBias();
            if (bias == Orientation.VERTICAL) {
                h = boundedSize(child.minHeight(-1), h, child.maxHeight(-1));
                w = boundedSize(child.minWidth(h), w, child.maxWidth(h));
            } else if (bias == Orientation.HORIZONTAL) {
                w = boundedSize(child.minWidth(-1), w, child.maxWidth(-1));
                h = boundedSize(child.minHeight(w), h, child.maxHeight(w));
            } else {
                w = boundedSize(child.minWidth(-1), w, child.maxWidth(-1));
                h = boundedSize(child.minHeight(-1), h, child.maxHeight(-1));
            }
            w = snapSizeX(w);
            h = snapSizeY(h);
            child.resize(w, h);
        } else {
            w = child.getLayoutBounds().getWidth();
            h = child.getLayoutBounds().getHeight();
        }
        return new double[] {w, h};
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double width = Math.max(getWidth(), minWidth(-1));
        double height = Math.max(getHeight(), minHeight(-1));
        double insideX = in.getLeft();
        double insideY = in.getTop();
        double insideWidth = width - insideX - in.getRight();
        double insideHeight = height - insideY - in.getBottom();
        Node t = placed(top);
        Node b = placed(bottom);
        Node l = placed(left);
        Node r = placed(right);
        Node c = placed(center);

        double topHeight = 0;
        if (t != null) {
            Insets m = getMargin(t);
            double inner = Math.max(0, insideWidth - horizontal(m));
            double wanted = Math.min(snapSizeY(t.prefHeight(inner)), Math.max(0, insideHeight - vertical(m)));
            double[] size = sizeEdge(t, insideWidth, wanted + vertical(m), m);
            topHeight = snapSizeY(size[1] + vertical(m));
            Pos pos = getAlignment(t);
            positionInMargin(t, insideX, insideY, insideWidth, topHeight, m, pos == null ? HPos.LEFT : pos.getHpos(),
                    pos == null ? VPos.TOP : pos.getVpos());
        }
        double bottomHeight = 0;
        if (b != null) {
            Insets m = getMargin(b);
            double inner = Math.max(0, insideWidth - horizontal(m));
            double wanted = Math.min(snapSizeY(b.prefHeight(inner)),
                    Math.max(0, insideHeight - topHeight - vertical(m)));
            double[] size = sizeEdge(b, insideWidth, wanted + vertical(m), m);
            bottomHeight = snapSizeY(size[1] + vertical(m));
            Pos pos = getAlignment(b);
            positionInMargin(b, insideX, insideY + insideHeight - bottomHeight, insideWidth, bottomHeight, m,
                    pos == null ? HPos.LEFT : pos.getHpos(), pos == null ? VPos.BOTTOM : pos.getVpos());
        }
        double middleHeight = Math.max(0, insideHeight - topHeight - bottomHeight);
        double leftWidth = 0;
        if (l != null) {
            Insets m = getMargin(l);
            double inner = Math.max(0, middleHeight - vertical(m));
            double wanted = Math.min(snapSizeX(l.prefWidth(inner)), Math.max(0, insideWidth - horizontal(m)));
            double[] size = sizeEdge(l, wanted + horizontal(m), middleHeight, m);
            leftWidth = snapSizeX(size[0] + horizontal(m));
            Pos pos = getAlignment(l);
            positionInMargin(l, insideX, insideY + topHeight, leftWidth, middleHeight, m,
                    pos == null ? HPos.LEFT : pos.getHpos(), pos == null ? VPos.TOP : pos.getVpos());
        }
        double rightWidth = 0;
        if (r != null) {
            Insets m = getMargin(r);
            double inner = Math.max(0, middleHeight - vertical(m));
            double wanted = Math.min(snapSizeX(r.prefWidth(inner)),
                    Math.max(0, insideWidth - leftWidth - horizontal(m)));
            double[] size = sizeEdge(r, wanted + horizontal(m), middleHeight, m);
            rightWidth = snapSizeX(size[0] + horizontal(m));
            Pos pos = getAlignment(r);
            positionInMargin(r, insideX + insideWidth - rightWidth, insideY + topHeight, rightWidth, middleHeight, m,
                    pos == null ? HPos.RIGHT : pos.getHpos(), pos == null ? VPos.TOP : pos.getVpos());
        }
        if (c != null) {
            Pos pos = getAlignment(c);
            layoutInArea(c, insideX + leftWidth, insideY + topHeight,
                    Math.max(0, insideWidth - leftWidth - rightWidth), middleHeight, 0, getMargin(c),
                    pos == null ? HPos.CENTER : pos.getHpos(), pos == null ? VPos.CENTER : pos.getVpos());
        }
    }
}
