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

/// Lays its managed children out in one row, left to right, in the order of the list.
///
/// Each child gets its preferred width. When the box is wider than
/// its children need, the spare space goes in equal shares to the
/// children whose hgrow priority is `ALWAYS`, each up to its maximum
/// width, and only if space is still left to those with `SOMETIMES`.
/// When the box is narrower, every child gives up an equal share down to
/// its minimum width. Shares are whole device pixels, so up to one
/// logical pixel may stay unused. Spare space nobody takes is placed
/// according to the horizontal part of `alignment`.
///
/// Across the box a resizable child is stretched to the height of the
/// box, up to its own maximum, while `fillHeight` is set; otherwise it keeps
/// its preferred height. The vertical part of `alignment` places a child
/// that does not fill the height.
///
/// With a baseline alignment the children keep their preferred height and
/// are moved down until their baselines are level. A child without text
/// has its baseline at its bottom edge.
///
/// #### Styling
///
/// Besides the names of a region: `-fx-spacing` (`Number`),
/// `-fx-alignment` (`javafx.geometry.Pos`, or its CSS spelling such as
/// `"center-left"` as a `String`, matched without regard to ASCII case)
/// and `-fx-fill-height` (`Boolean`).
public class HBox extends Pane {

    private static final String MARGIN = "hbox-margin";
    private static final String GROW = "hbox-hgrow";

    private final DoubleProperty spacing = new FxDouble(this, "spacing", 0, Dirty.LAYOUT);
    private final ObjectProperty<Pos> alignment = new FxObject<Pos>(this, "alignment", Pos.TOP_LEFT, Dirty.LAYOUT);
    private final BooleanProperty fill = new FxBoolean(this, "fillHeight", true, Dirty.LAYOUT);

    /// Creates an empty box without spacing.
    public HBox() {
    }

    /// Creates an empty box with a gap between its children.
    public HBox(double spacing) {
        this.spacing.set(spacing);
    }

    /// Creates a box with children and no spacing.
    public HBox(Node... children) {
        cn1Children().addAll(children);
    }

    /// Creates a box with children and a gap between them.
    public HBox(double spacing, Node... children) {
        this.spacing.set(spacing);
        cn1Children().addAll(children);
    }

    /// Sets how eagerly a child takes spare width in an HBox; `null`
    /// removes the constraint.
    public static void setHgrow(Node child, Priority value) {
        setConstraint(child, GROW, value);
    }

    /// Returns the hgrow priority of a child, or `null`.
    public static Priority getHgrow(Node child) {
        return LayoutSupport.priority(child, GROW);
    }

    /// Sets the space kept free around a child in an HBox; `null`
    /// removes the constraint.
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN, value);
    }

    /// Returns the margin of a child, or `null`.
    public static Insets getMargin(Node child) {
        return LayoutSupport.insets(child, MARGIN);
    }

    /// Removes the HBox constraints from a child.
    public static void clearConstraints(Node child) {
        setHgrow(child, null);
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

    /// Returns whether resizable children are stretched to the height of
    /// the box.
    public final boolean isFillHeight() {
        return fill.get();
    }

    /// Sets whether resizable children are stretched to the height of the
    /// box.
    public final void setFillHeight(boolean value) {
        fill.set(value);
    }

    /// Whether resizable children are stretched to the height of the box.
    public final BooleanProperty fillHeightProperty() {
        return fill;
    }

    private Pos align() {
        Pos p = alignment.get();
        return p == null ? Pos.TOP_LEFT : p;
    }

    private double gap() {
        return snapSpaceX(getSpacing());
    }

    @Override
    public Orientation getContentBias() {
        List<Node> managed = getManagedChildren();
        return LayoutSupport.bias(managed);
    }

    private boolean onBaseline() {
        return align().getVpos() == VPos.BASELINE;
    }

    private double[] areaWidths(List<Node> managed, double height, boolean minimum) {
        Insets in = getInsets();
        double inside = height == -1 || !isFillHeight() || onBaseline() ? -1 : height - in.getTop() - in.getBottom();
        return LayoutSupport.areaSizes(this, managed, true, MARGIN, inside, minimum);
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        return in.getLeft() + LayoutSupport.sum(areaWidths(managed, height, true), gap()) + in.getRight();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        return in.getLeft() + LayoutSupport.sum(areaWidths(managed, height, false), gap()) + in.getRight();
    }

    private double contentHeight(double width, boolean minimum) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        if (onBaseline()) {
            return LayoutSupport.baselineAreaHeight(this, managed, MARGIN);
        }
        double[] widths = null;
        if (width != -1 && getContentBias() == Orientation.HORIZONTAL) {
            widths = areaWidths(managed, -1, false);
            LayoutSupport.fit(this, managed, true, MARGIN, GROW, widths, gap(),
                    width - in.getLeft() - in.getRight(), -1);
        }
        return LayoutSupport.maxCross(this, managed, true, MARGIN, widths, minimum);
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + contentHeight(width, true) + in.getBottom();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + contentHeight(width, false) + in.getBottom();
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
        boolean baseline = onBaseline();
        boolean fillHeight = isFillHeight() && !baseline;
        double[] widths = areaWidths(managed, height, false);
        double used = LayoutSupport.fit(this, managed, true, MARGIN, GROW, widths, gap, insideWidth,
                fillHeight ? insideHeight : -1);
        double x = in.getLeft() + LayoutSupport.xOffset(insideWidth, used, pos.getHpos());
        double y = in.getTop();
        double areaBaseline = baseline ? LayoutSupport.areaBaseline(this, managed, MARGIN) : 0;
        for (int i = 0; i < widths.length; i++) {
            Node child = managed.get(i);
            if (baseline) {
                LayoutSupport.layoutOnBaseline(this, child, x, y, widths[i], insideHeight, areaBaseline,
                        getMargin(child), true, HPos.CENTER);
            } else {
                layoutInArea(child, x, y, widths[i], insideHeight, 0, getMargin(child), true, fillHeight,
                        HPos.CENTER, pos.getVpos());
            }
            x += widths[i] + gap;
        }
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-spacing".equals(property)) {
            return Double.valueOf(getSpacing());
        } else if ("-fx-alignment".equals(property)) {
            return getAlignment();
        } else if ("-fx-fill-height".equals(property)) {
            return Boolean.valueOf(isFillHeight());
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
        } else if ("-fx-fill-height".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            setFillHeight(((Boolean) value).booleanValue());
            return true;
        }
        return super.cn1SetStyleValue(property, value);
    }
}
