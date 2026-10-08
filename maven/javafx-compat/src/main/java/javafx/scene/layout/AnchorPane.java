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

import java.util.List;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;

/// Keeps the edges of its managed children at fixed distances from its
/// own edges.
///
/// An anchor is the distance from an edge of the pane's content area (the
/// area inside the padding) to the same edge of the child. A resizable
/// child anchored on two opposite sides is stretched between them,
/// whatever its minimum and maximum size; any other child keeps its
/// preferred size. A child with no anchor in a direction stays at its
/// `layoutX` or `layoutY`.
///
/// The preferred size of the pane is the smallest that shows every child
/// at its preferred size with its anchors; the minimum size uses the
/// minimum size of the children that are anchored on both sides.
public class AnchorPane extends Pane {

    private static final String TOP = "pane-top-anchor";
    private static final String LEFT = "pane-left-anchor";
    private static final String BOTTOM = "pane-bottom-anchor";
    private static final String RIGHT = "pane-right-anchor";

    /// Creates an empty anchor pane.
    public AnchorPane() {
    }

    /// Creates an anchor pane with children.
    public AnchorPane(Node... children) {
        cn1Children().addAll(children);
    }

    /// Sets the distance from the top of the content area to the top of a
    /// child; `null` removes the anchor.
    public static void setTopAnchor(Node child, Double value) {
        setConstraint(child, TOP, value);
    }

    /// Returns the top anchor of a child, or `null`.
    public static Double getTopAnchor(Node child) {
        return LayoutSupport.number(child, TOP);
    }

    /// Sets the distance from the left of the content area to the left of
    /// a child; `null` removes the anchor.
    public static void setLeftAnchor(Node child, Double value) {
        setConstraint(child, LEFT, value);
    }

    /// Returns the left anchor of a child, or `null`.
    public static Double getLeftAnchor(Node child) {
        return LayoutSupport.number(child, LEFT);
    }

    /// Sets the distance from the bottom of the content area to the bottom
    /// of a child; `null` removes the anchor.
    public static void setBottomAnchor(Node child, Double value) {
        setConstraint(child, BOTTOM, value);
    }

    /// Returns the bottom anchor of a child, or `null`.
    public static Double getBottomAnchor(Node child) {
        return LayoutSupport.number(child, BOTTOM);
    }

    /// Sets the distance from the right of the content area to the right
    /// of a child; `null` removes the anchor.
    public static void setRightAnchor(Node child, Double value) {
        setConstraint(child, RIGHT, value);
    }

    /// Returns the right anchor of a child, or `null`.
    public static Double getRightAnchor(Node child) {
        return LayoutSupport.number(child, RIGHT);
    }

    /// Removes the anchors from a child.
    public static void clearConstraints(Node child) {
        setTopAnchor(child, null);
        setRightAnchor(child, null);
        setBottomAnchor(child, null);
        setLeftAnchor(child, null);
    }

    private static double value(Double anchor) {
        return anchor == null ? 0 : anchor.doubleValue();
    }

    private double extent(boolean horizontal, boolean minimum, double across) {
        List<Node> managed = getManagedChildren();
        double max = 0;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            Double near = horizontal ? getLeftAnchor(child) : getTopAnchor(child);
            Double far = horizontal ? getRightAnchor(child) : getBottomAnchor(child);
            Bounds lb = child.getLayoutBounds();
            double start;
            if (near != null) {
                start = near.doubleValue();
            } else {
                start = horizontal ? lb.getMinX() + child.getLayoutX() : lb.getMinY() + child.getLayoutY();
            }
            double other = -1;
            Orientation bias = child.getContentBias();
            if (across != -1 && bias == (horizontal ? Orientation.VERTICAL : Orientation.HORIZONTAL)) {
                other = horizontal ? childHeight(child, across, -1) : childWidth(child, across, -1);
            }
            double size;
            if (minimum && near != null && far != null) {
                size = horizontal ? child.minWidth(other) : child.minHeight(other);
            } else if (horizontal) {
                size = computeChildPrefAreaWidth(child, null, other);
            } else {
                size = computeChildPrefAreaHeight(child, null, other);
            }
            max = Math.max(max, start + size + value(far));
        }
        return max;
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + extent(true, true, height == -1 ? -1 : height - in.getTop() - in.getBottom())
                + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + extent(false, true, width == -1 ? -1 : width - in.getLeft() - in.getRight())
                + in.getBottom();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + extent(true, false, height == -1 ? -1 : height - in.getTop() - in.getBottom())
                + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + extent(false, false, width == -1 ? -1 : width - in.getLeft() - in.getRight())
                + in.getBottom();
    }

    /// The width of a child in a content area of a given width.
    private double childWidth(Node child, double insideWidth, double height) {
        Double left = getLeftAnchor(child);
        Double right = getRightAnchor(child);
        if (left != null && right != null && child.isResizable()) {
            return Math.max(0, insideWidth - left.doubleValue() - right.doubleValue());
        }
        return computeChildPrefAreaWidth(child, null, height);
    }

    private double childHeight(Node child, double insideHeight, double width) {
        Double top = getTopAnchor(child);
        Double bottom = getBottomAnchor(child);
        if (top != null && bottom != null && child.isResizable()) {
            return Math.max(0, insideHeight - top.doubleValue() - bottom.doubleValue());
        }
        return computeChildPrefAreaHeight(child, null, width);
    }

    @Override
    protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        Insets in = getInsets();
        double insideWidth = getWidth() - in.getLeft() - in.getRight();
        double insideHeight = getHeight() - in.getTop() - in.getBottom();
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            Bounds lb = child.getLayoutBounds();
            Orientation bias = child.getContentBias();
            double w;
            double h;
            if (bias == Orientation.VERTICAL) {
                h = childHeight(child, insideHeight, -1);
                w = childWidth(child, insideWidth, h);
            } else if (bias == Orientation.HORIZONTAL) {
                w = childWidth(child, insideWidth, -1);
                h = childHeight(child, insideHeight, w);
            } else {
                w = childWidth(child, insideWidth, -1);
                h = childHeight(child, insideHeight, -1);
            }
            double x = child.getLayoutX() + lb.getMinX();
            double y = child.getLayoutY() + lb.getMinY();
            Double left = getLeftAnchor(child);
            Double right = getRightAnchor(child);
            Double top = getTopAnchor(child);
            Double bottom = getBottomAnchor(child);
            if (left != null) {
                x = in.getLeft() + left.doubleValue();
            } else if (right != null) {
                x = getWidth() - in.getRight() - right.doubleValue() - w;
            }
            if (top != null) {
                y = in.getTop() + top.doubleValue();
            } else if (bottom != null) {
                y = getHeight() - in.getBottom() - bottom.doubleValue() - h;
            }
            child.resizeRelocate(snapPositionX(x), snapPositionY(y), w, h);
        }
    }
}
