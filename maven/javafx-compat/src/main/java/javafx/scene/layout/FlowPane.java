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

import java.util.ArrayList;
import java.util.List;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;

/// Lays its managed children out one after the other and starts a new
/// row, or a new column, when the next child does not fit.
///
/// A horizontal flow pane fills rows from left to right and wraps at its
/// own width; a vertical one fills columns from top to bottom and wraps
/// at its height. Every child takes its preferred size along the flow. A
/// row is as tall as its tallest child (a column as wide as its widest),
/// and a resizable child is stretched to that, up to its maximum size;
/// `rowValignment` and `columnHalignment` place the children that do not
/// fill it. `alignment` places the rows and columns in the pane.
///
/// The preferred size along the flow is `prefWrapLength`, or the longest
/// child when that is longer; the preferred size across it is what the
/// content needs when wrapped at the size given, or at `prefWrapLength`
/// when none is given.
///
/// #### Styling
///
/// Besides the names of a region: `-fx-hgap`, `-fx-vgap` (`Number`),
/// `-fx-alignment` (`javafx.geometry.Pos` or its CSS spelling as a
/// `String`) and `-fx-orientation` (`javafx.geometry.Orientation` or its
/// name as a `String`); strings are matched without regard to ASCII case.
public class FlowPane extends Pane {

    private static final String MARGIN = "flowpane-margin";

    private final ObjectProperty<Orientation> orientation = new FxObject<Orientation>(this, "orientation",
            Orientation.HORIZONTAL, Dirty.LAYOUT);
    private final DoubleProperty hgap = new FxDouble(this, "hgap", 0, Dirty.LAYOUT);
    private final DoubleProperty vgap = new FxDouble(this, "vgap", 0, Dirty.LAYOUT);
    private final DoubleProperty prefWrapLength = new FxDouble(this, "prefWrapLength", 400, Dirty.LAYOUT);
    private final ObjectProperty<Pos> alignment = new FxObject<Pos>(this, "alignment", Pos.TOP_LEFT, Dirty.LAYOUT);
    private final ObjectProperty<HPos> columnHalignment = new FxObject<HPos>(this, "columnHalignment", HPos.LEFT,
            Dirty.LAYOUT);
    private final ObjectProperty<VPos> rowValignment = new FxObject<VPos>(this, "rowValignment", VPos.CENTER,
            Dirty.LAYOUT);

    /// Creates an empty horizontal flow pane without gaps.
    public FlowPane() {
    }

    /// Creates an empty flow pane without gaps.
    public FlowPane(Orientation orientation) {
        this.orientation.set(orientation);
    }

    /// Creates an empty horizontal flow pane with gaps.
    public FlowPane(double hgap, double vgap) {
        this.hgap.set(hgap);
        this.vgap.set(vgap);
    }

    /// Creates an empty flow pane with gaps.
    public FlowPane(Orientation orientation, double hgap, double vgap) {
        this.orientation.set(orientation);
        this.hgap.set(hgap);
        this.vgap.set(vgap);
    }

    /// Creates a horizontal flow pane with children.
    public FlowPane(Node... children) {
        cn1Children().addAll(children);
    }

    /// Creates a flow pane with children.
    public FlowPane(Orientation orientation, Node... children) {
        this.orientation.set(orientation);
        cn1Children().addAll(children);
    }

    /// Creates a horizontal flow pane with gaps and children.
    public FlowPane(double hgap, double vgap, Node... children) {
        this.hgap.set(hgap);
        this.vgap.set(vgap);
        cn1Children().addAll(children);
    }

    /// Creates a flow pane with gaps and children.
    public FlowPane(Orientation orientation, double hgap, double vgap, Node... children) {
        this.orientation.set(orientation);
        this.hgap.set(hgap);
        this.vgap.set(vgap);
        cn1Children().addAll(children);
    }

    /// Sets the space kept free around a child in a flow pane; `null`
    /// removes the constraint.
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN, value);
    }

    /// Returns the margin of a child, or `null`.
    public static Insets getMargin(Node child) {
        return LayoutSupport.insets(child, MARGIN);
    }

    /// Removes the flow pane constraints from a child.
    public static void clearConstraints(Node child) {
        setMargin(child, null);
    }

    /// Returns the direction of the flow.
    public final Orientation getOrientation() {
        Orientation o = orientation.get();
        return o == null ? Orientation.HORIZONTAL : o;
    }

    /// Sets the direction of the flow.
    public final void setOrientation(Orientation value) {
        orientation.set(value);
    }

    /// The direction of the flow.
    public final ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    /// Returns the horizontal gap between children or columns.
    public final double getHgap() {
        return hgap.get();
    }

    /// Sets the horizontal gap between children or columns.
    public final void setHgap(double value) {
        hgap.set(value);
    }

    /// The horizontal gap between children or columns.
    public final DoubleProperty hgapProperty() {
        return hgap;
    }

    /// Returns the vertical gap between children or rows.
    public final double getVgap() {
        return vgap.get();
    }

    /// Sets the vertical gap between children or rows.
    public final void setVgap(double value) {
        vgap.set(value);
    }

    /// The vertical gap between children or rows.
    public final DoubleProperty vgapProperty() {
        return vgap;
    }

    /// Returns the length at which the content wraps when the preferred
    /// size is computed.
    public final double getPrefWrapLength() {
        return prefWrapLength.get();
    }

    /// Sets the length at which the content wraps when the preferred size
    /// is computed.
    public final void setPrefWrapLength(double value) {
        prefWrapLength.set(value);
    }

    /// The length at which the content wraps when the preferred size is
    /// computed.
    public final DoubleProperty prefWrapLengthProperty() {
        return prefWrapLength;
    }

    /// Returns where the rows or columns sit in the pane.
    public final Pos getAlignment() {
        return alignment.get();
    }

    /// Sets where the rows or columns sit in the pane.
    public final void setAlignment(Pos value) {
        alignment.set(value);
    }

    /// Where the rows or columns sit in the pane.
    public final ObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    /// Returns where a child sits across its column in a vertical flow.
    public final HPos getColumnHalignment() {
        return columnHalignment.get();
    }

    /// Sets where a child sits across its column in a vertical flow.
    public final void setColumnHalignment(HPos value) {
        columnHalignment.set(value);
    }

    /// Where a child sits across its column in a vertical flow.
    public final ObjectProperty<HPos> columnHalignmentProperty() {
        return columnHalignment;
    }

    /// Returns where a child sits across its row in a horizontal flow.
    public final VPos getRowValignment() {
        return rowValignment.get();
    }

    /// Sets where a child sits across its row in a horizontal flow.
    public final void setRowValignment(VPos value) {
        rowValignment.set(value);
    }

    /// Where a child sits across its row in a horizontal flow.
    public final ObjectProperty<VPos> rowValignmentProperty() {
        return rowValignment;
    }

    private Pos align() {
        Pos p = alignment.get();
        return p == null ? Pos.TOP_LEFT : p;
    }

    @Override
    public Orientation getContentBias() {
        return getOrientation();
    }

    /// One row of a horizontal flow, or one column of a vertical one.
    private static final class Run {
        final List<Node> nodes = new ArrayList<Node>();
        final List<double[]> areas = new ArrayList<double[]>();
        double offset;
        double length;
        double breadth;
        double baseline;
    }

    private boolean rowsOnBaseline() {
        return getOrientation() == Orientation.HORIZONTAL && getRowValignment() == VPos.BASELINE;
    }

    private void close(Run run, double offset, double gap) {
        int n = run.areas.size();
        double length = 0;
        double breadth = 0;
        for (int i = 0; i < n; i++) {
            double[] a = run.areas.get(i);
            length += a[1];
            breadth = Math.max(breadth, a[2]);
        }
        run.length = n > 1 ? length + gap * (n - 1) : length;
        run.breadth = breadth;
        run.offset = offset;
        if (rowsOnBaseline()) {
            run.baseline = LayoutSupport.areaBaseline(this, run.nodes, MARGIN);
            run.breadth = LayoutSupport.baselineAreaHeight(this, run.nodes, MARGIN);
        }
    }

    /// Breaks the children into runs no longer than a limit. An area is
    /// position along the run, length along it and breadth across it.
    private List<Run> runs(double limit) {
        boolean horizontal = getOrientation() == Orientation.HORIZONTAL;
        double along = horizontal ? snapSpaceX(getHgap()) : snapSpaceY(getVgap());
        double across = horizontal ? snapSpaceY(getVgap()) : snapSpaceX(getHgap());
        List<Node> managed = getManagedChildren();
        List<Run> runs = new ArrayList<Run>();
        Run run = new Run();
        double offset = 0;
        double length = 0;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            Insets m = getMargin(child);
            double w = computeChildPrefAreaWidth(child, m, -1);
            double h = computeChildPrefAreaHeight(child, m, -1);
            double size = horizontal ? w : h;
            if (length + size > limit && length > 0) {
                close(run, offset, along);
                offset += run.breadth + across;
                runs.add(run);
                run = new Run();
                length = 0;
            }
            run.nodes.add(child);
            run.areas.add(new double[] {length, size, horizontal ? h : w});
            length += size + along;
        }
        close(run, offset, along);
        runs.add(run);
        return runs;
    }

    private static double longest(List<Run> runs) {
        double max = 0;
        for (int i = 0; i < runs.size(); i++) {
            max = Math.max(max, runs.get(i).length);
        }
        return max;
    }

    private static double breadth(List<Run> runs) {
        Run last = runs.get(runs.size() - 1);
        return last.offset + last.breadth;
    }

    private double largestChild(boolean width) {
        List<Node> managed = getManagedChildren();
        double max = 0;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            Insets m = getMargin(child);
            max = Math.max(max, width ? computeChildPrefAreaWidth(child, m, -1)
                    : computeChildPrefAreaHeight(child, m, -1));
        }
        return max;
    }

    @Override
    protected double computeMinWidth(double height) {
        if (getOrientation() == Orientation.HORIZONTAL) {
            Insets in = getInsets();
            return in.getLeft() + snapSizeX(largestChild(true)) + in.getRight();
        }
        return computePrefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
        if (getOrientation() == Orientation.VERTICAL) {
            Insets in = getInsets();
            return in.getTop() + snapSizeY(largestChild(false)) + in.getBottom();
        }
        return computePrefHeight(width);
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        if (getOrientation() == Orientation.HORIZONTAL) {
            double wrap = getPrefWrapLength();
            double content = longest(runs(wrap));
            return in.getLeft() + snapSizeX(Math.max(wrap, content)) + in.getRight();
        }
        double limit = height != -1 ? height - in.getTop() - in.getBottom() : getPrefWrapLength();
        return in.getLeft() + breadth(runs(limit)) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        if (getOrientation() == Orientation.HORIZONTAL) {
            double limit = width != -1 ? width - in.getLeft() - in.getRight() : getPrefWrapLength();
            return in.getTop() + breadth(runs(limit)) + in.getBottom();
        }
        double wrap = getPrefWrapLength();
        double content = longest(runs(wrap));
        return in.getTop() + snapSizeY(Math.max(wrap, content)) + in.getBottom();
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double insideWidth = getWidth() - in.getLeft() - in.getRight();
        double insideHeight = getHeight() - in.getTop() - in.getBottom();
        boolean horizontal = getOrientation() == Orientation.HORIZONTAL;
        List<Run> runs = runs(horizontal ? insideWidth : insideHeight);
        Pos pos = align();
        HPos hpos = getColumnHalignment() == null ? HPos.LEFT : getColumnHalignment();
        VPos vpos = getRowValignment() == null ? VPos.CENTER : getRowValignment();
        boolean baseline = rowsOnBaseline();
        double across = breadth(runs);
        for (int r = 0; r < runs.size(); r++) {
            Run run = runs.get(r);
            double x0 = in.getLeft() + LayoutSupport.xOffset(insideWidth, horizontal ? run.length : across,
                    pos.getHpos());
            double y0 = in.getTop() + LayoutSupport.yOffset(insideHeight, horizontal ? across : run.length,
                    pos.getVpos());
            for (int i = 0; i < run.nodes.size(); i++) {
                Node child = run.nodes.get(i);
                double[] a = run.areas.get(i);
                if (!horizontal) {
                    layoutInArea(child, x0 + run.offset, y0 + a[0], run.breadth, a[1], 0, getMargin(child), hpos,
                            vpos == VPos.BASELINE ? VPos.TOP : vpos);
                } else if (baseline) {
                    LayoutSupport.layoutOnBaseline(this, child, x0 + a[0], y0 + run.offset, a[1], run.breadth,
                            run.baseline, getMargin(child), true, hpos);
                } else {
                    layoutInArea(child, x0 + a[0], y0 + run.offset, a[1], run.breadth, 0, getMargin(child), hpos,
                            vpos);
                }
            }
        }
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-hgap".equals(property)) {
            return Double.valueOf(getHgap());
        } else if ("-fx-vgap".equals(property)) {
            return Double.valueOf(getVgap());
        } else if ("-fx-alignment".equals(property)) {
            return getAlignment();
        } else if ("-fx-orientation".equals(property)) {
            return getOrientation();
        }
        return super.cn1StyleValue(property);
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-hgap".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            setHgap(((Number) value).doubleValue());
            return true;
        } else if ("-fx-vgap".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            setVgap(((Number) value).doubleValue());
            return true;
        } else if ("-fx-alignment".equals(property)) {
            Pos p = LayoutSupport.toPos(value);
            if (value != null && p == null) {
                return false;
            }
            setAlignment(p);
            return true;
        } else if ("-fx-orientation".equals(property)) {
            Orientation o = LayoutSupport.toOrientation(value);
            if (o == null) {
                return false;
            }
            setOrientation(o);
            return true;
        }
        return super.cn1SetStyleValue(property, value);
    }
}
