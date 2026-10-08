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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.VPos;

/// How a `GridPane` sizes one row and places the children in it. The
/// constraint at index `n` of the pane's list applies to row `n`.
///
/// A size left at `Region.USE_COMPUTED_SIZE` is computed from the
/// children of the row. A `percentHeight` of zero or more makes the
/// row that share of the pane's height less the gaps, whatever the
/// sizes say. A `vgrow` or `valignment` left `null` falls back to what the
/// children ask for.
public class RowConstraints extends ConstraintsBase {

    private final DoubleProperty min = new Num(this, "minHeight", Region.USE_COMPUTED_SIZE);
    private final DoubleProperty pref = new Num(this, "prefHeight", Region.USE_COMPUTED_SIZE);
    private final DoubleProperty max = new Num(this, "maxHeight", Region.USE_COMPUTED_SIZE);
    private final DoubleProperty percent = new Num(this, "percentHeight", -1);
    private final ObjectProperty<Priority> grow = new Ref<Priority>(this, "vgrow");
    private final ObjectProperty<VPos> align = new Ref<VPos>(this, "valignment");
    private final BooleanProperty fill = new Flag(this, "fillHeight", true);

    /// Creates a constraint that computes everything from the children.
    public RowConstraints() {
    }

    /// Creates a row of a fixed height.
    public RowConstraints(double height) {
        min.set(Region.USE_PREF_SIZE);
        pref.set(height);
        max.set(Region.USE_PREF_SIZE);
    }

    /// Creates a row with a range of sizes.
    public RowConstraints(double minHeight, double prefHeight, double maxHeight) {
        min.set(minHeight);
        pref.set(prefHeight);
        max.set(maxHeight);
    }

    /// Creates a row with a range of sizes, a grow priority, an
    /// alignment for its children and whether it stretches them.
    public RowConstraints(double minHeight, double prefHeight, double maxHeight, Priority vgrow, VPos valignment,
            boolean fillHeight) {
        min.set(minHeight);
        pref.set(prefHeight);
        max.set(maxHeight);
        grow.set(vgrow);
        align.set(valignment);
        fill.set(fillHeight);
    }

    /// Returns the minimum height, `Region.USE_COMPUTED_SIZE` or
    /// `Region.USE_PREF_SIZE`.
    public final double getMinHeight() {
        return min.get();
    }

    /// Sets the minimum height.
    public final void setMinHeight(double value) {
        min.set(value);
    }

    /// The minimum height of the row.
    public final DoubleProperty minHeightProperty() {
        return min;
    }

    /// Returns the preferred height, or `Region.USE_COMPUTED_SIZE`.
    public final double getPrefHeight() {
        return pref.get();
    }

    /// Sets the preferred height.
    public final void setPrefHeight(double value) {
        pref.set(value);
    }

    /// The preferred height of the row.
    public final DoubleProperty prefHeightProperty() {
        return pref;
    }

    /// Returns the maximum height, `Region.USE_COMPUTED_SIZE` or
    /// `Region.USE_PREF_SIZE`.
    public final double getMaxHeight() {
        return max.get();
    }

    /// Sets the maximum height.
    public final void setMaxHeight(double value) {
        max.set(value);
    }

    /// The maximum height of the row.
    public final DoubleProperty maxHeightProperty() {
        return max;
    }

    /// Returns the share of the pane in percent, or a negative number.
    public final double getPercentHeight() {
        return percent.get();
    }

    /// Sets the share of the pane this row takes, in percent; a negative
    /// number turns it off.
    public final void setPercentHeight(double value) {
        percent.set(value);
    }

    /// The share of the pane this row takes, in percent.
    public final DoubleProperty percentHeightProperty() {
        return percent;
    }

    /// Returns how eagerly the row takes spare space, or `null`.
    public final Priority getVgrow() {
        return grow.get();
    }

    /// Sets how eagerly the row takes spare space.
    public final void setVgrow(Priority value) {
        grow.set(value);
    }

    /// How eagerly the row takes spare space.
    public final ObjectProperty<Priority> vgrowProperty() {
        return grow;
    }

    /// Returns where the children sit in the row, or `null`.
    public final VPos getValignment() {
        return align.get();
    }

    /// Sets where the children sit in the row.
    public final void setValignment(VPos value) {
        align.set(value);
    }

    /// Where the children sit in the row.
    public final ObjectProperty<VPos> valignmentProperty() {
        return align;
    }

    /// Returns whether resizable children are stretched to the height of
    /// the row.
    public final boolean isFillHeight() {
        return fill.get();
    }

    /// Sets whether resizable children are stretched to the height of the
    /// row.
    public final void setFillHeight(boolean value) {
        fill.set(value);
    }

    /// Whether resizable children are stretched to the height of the row.
    public final BooleanProperty fillHeightProperty() {
        return fill;
    }

    @Override
    double minSize() {
        return min.get();
    }

    @Override
    double prefSize() {
        return pref.get();
    }

    @Override
    double maxSize() {
        return max.get();
    }

    @Override
    double percent() {
        return percent.get();
    }

    @Override
    Priority grow() {
        return grow.get();
    }

    @Override
    boolean fill() {
        return fill.get();
    }

    /// Returns a description for debugging.
    @Override
    public String toString() {
        return "RowConstraints percentHeight=" + getPercentHeight() + " minHeight=" + getMinHeight() + " prefHeight="
                + getPrefHeight() + " maxHeight=" + getMaxHeight() + " vgrow=" + getVgrow() + " fillHeight="
                + isFillHeight() + " valignment=" + getValignment();
    }
}
