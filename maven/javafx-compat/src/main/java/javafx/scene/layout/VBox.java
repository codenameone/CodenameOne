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

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;

/// Lays its managed children out in one column, top to bottom, in the order of the list.
///
/// Each child gets its preferred height. When the box is taller than
/// its children need, the spare space goes in equal shares to the
/// children whose vgrow priority is `ALWAYS`, each up to its maximum
/// height, and only if space is still left to those with `SOMETIMES`.
/// When the box is shorter, every child gives up an equal share down to
/// its minimum height. Shares are whole device pixels, so up to one
/// logical pixel may stay unused. Spare space nobody takes is placed
/// according to the vertical part of `alignment`.
///
/// Across the box a resizable child is stretched to the width of the
/// box, up to its own maximum, while `fillWidth` is set; otherwise it keeps
/// its preferred width. The horizontal part of `alignment` places a child
/// that does not fill the width.
///
/// A column has no shared baseline: a baseline alignment places the
/// children like the matching top alignment.
///
/// #### Styling
///
/// Besides the names of a region: `-fx-spacing` (`Number`),
/// `-fx-alignment` (`javafx.geometry.Pos`, or its CSS spelling such as
/// `"center-left"` as a `String`, matched without regard to ASCII case)
/// and `-fx-fill-width` (`Boolean`).
public class VBox extends Pane {

    private static final String MARGIN = "vbox-margin";
    private static final String GROW = "vbox-vgrow";

    private final DoubleProperty spacing = new FxDouble(this, "spacing", 0, Dirty.LAYOUT);
    private final ObjectProperty<Pos> alignment = new FxObject<Pos>(this, "alignment", Pos.TOP_LEFT, Dirty.LAYOUT);
    private final BooleanProperty fill = new FxBoolean(this, "fillWidth", true, Dirty.LAYOUT);

    /// Creates an empty box without spacing.
    public VBox() {
    }

    /// Creates an empty box with a gap between its children.
    public VBox(double spacing) {
        this.spacing.set(spacing);
    }

    /// Creates a box with children and no spacing.
    public VBox(Node... children) {
        cn1Children().addAll(children);
    }

    /// Creates a box with children and a gap between them.
    public VBox(double spacing, Node... children) {
        this.spacing.set(spacing);
        cn1Children().addAll(children);
    }

    /// Sets how eagerly a child takes spare height in a VBox; `null`
    /// removes the constraint.
    public static void setVgrow(Node child, Priority value) {
        setConstraint(child, GROW, value);
    }

    /// Returns the vgrow priority of a child, or `null`.
    public static Priority getVgrow(Node child) {
        return LayoutSupport.priority(child, GROW);
    }

    /// Sets the space kept free around a child in a VBox; `null`
    /// removes the constraint.
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN, value);
    }

    /// Returns the margin of a child, or `null`.
    public static Insets getMargin(Node child) {
        return LayoutSupport.insets(child, MARGIN);
    }

    /// Removes the VBox constraints from a child.
    public static void clearConstraints(Node child) {
        setVgrow(child, null);
        setMargin(child, null);
    }

    /// Returns the gap between neighbouring children.
    public final double getSpacing() {
        return spacing.get();
    }

    /// Sets the gap between neighbouring children.
    public final void setSpacing(double value) {
        spacing.set(value);
    }

    /// The gap between neighbouring children.
    public final DoubleProperty spacingProperty() {
        return spacing;
    }

    /// Returns where the children sit when the box has room to spare.
    public final Pos getAlignment() {
        return alignment.get();
    }

    /// Sets where the children sit when the box has room to spare.
    public final void setAlignment(Pos value) {
        alignment.set(value);
    }

    /// Where the children sit when the box has room to spare.
    public final ObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    /// Returns whether resizable children are stretched to the width of
    /// the box.
    public final boolean isFillWidth() {
        return fill.get();
    }

    /// Sets whether resizable children are stretched to the width of the
    /// box.
    public final void setFillWidth(boolean value) {
        fill.set(value);
    }

    /// Whether resizable children are stretched to the width of the box.
    public final BooleanProperty fillWidthProperty() {
        return fill;
    }

    private Pos align() {
        Pos p = alignment.get();
        return p == null ? Pos.TOP_LEFT : p;
    }

    private double gap() {
        return snapSpaceY(getSpacing());
    }

    @Override
    public Orientation getContentBias() {
        List<Node> managed = getManagedChildren();
        return LayoutSupport.bias(managed);
    }

    private double[] areaHeights(List<Node> managed, double width, boolean minimum) {
        Insets in = getInsets();
        double inside = width == -1 || !isFillWidth() ? -1 : width - in.getLeft() - in.getRight();
        return LayoutSupport.areaSizes(this, managed, false, MARGIN, inside, minimum);
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        return in.getTop() + LayoutSupport.sum(areaHeights(managed, width, true), gap()) + in.getBottom();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        return in.getTop() + LayoutSupport.sum(areaHeights(managed, width, false), gap()) + in.getBottom();
    }

    private double contentWidth(double height, boolean minimum) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        double[] heights = null;
        if (height != -1 && getContentBias() == Orientation.VERTICAL) {
            heights = areaHeights(managed, -1, false);
            LayoutSupport.fit(this, managed, false, MARGIN, GROW, heights, gap(),
                    height - in.getTop() - in.getBottom(), -1);
        }
        return LayoutSupport.maxCross(this, managed, false, MARGIN, heights, minimum);
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + contentWidth(height, true) + in.getRight();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + contentWidth(height, false) + in.getRight();
    }

    @Override
    protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        Insets in = getInsets();
        double width = getWidth();
        double height = getHeight();
        double insideWidth = width - in.getLeft() - in.getRight();
        double insideHeight = height - in.getTop() - in.getBottom();
        double gap = gap();
        Pos pos = align();
        boolean fillWidth = isFillWidth();
        // A column has no shared baseline: a baseline alignment places
        // the children like a top alignment.
        VPos vpos = pos.getVpos() == VPos.BASELINE ? VPos.TOP : pos.getVpos();
        double[] heights = areaHeights(managed, width, false);
        double used = LayoutSupport.fit(this, managed, false, MARGIN, GROW, heights, gap, insideHeight,
                fillWidth ? insideWidth : -1);
        double x = in.getLeft();
        double y = in.getTop() + LayoutSupport.yOffset(insideHeight, used, vpos);
        for (int i = 0; i < heights.length; i++) {
            Node child = managed.get(i);
            layoutInArea(child, x, y, insideWidth, heights[i], 0, getMargin(child), fillWidth, true, pos.getHpos(),
                    vpos);
            y += heights[i] + gap;
        }
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-spacing".equals(property)) {
            return Double.valueOf(getSpacing());
        } else if ("-fx-alignment".equals(property)) {
            return getAlignment();
        } else if ("-fx-fill-width".equals(property)) {
            return Boolean.valueOf(isFillWidth());
        }
        return super.cn1StyleValue(property);
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-spacing".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            setSpacing(((Number) value).doubleValue());
            return true;
        } else if ("-fx-alignment".equals(property)) {
            Pos p = LayoutSupport.toPos(value);
            if (value != null && p == null) {
                return false;
            }
            setAlignment(p);
            return true;
        } else if ("-fx-fill-width".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            setFillWidth(((Boolean) value).booleanValue());
            return true;
        }
        return super.cn1SetStyleValue(property, value);
    }
}
