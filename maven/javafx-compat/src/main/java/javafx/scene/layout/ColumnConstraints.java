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
import javafx.geometry.HPos;

/// How a `GridPane` sizes one column and places the children in it. The
/// constraint at index `n` of the pane's list applies to column `n`.
///
/// A size left at `Region.USE_COMPUTED_SIZE` is computed from the
/// children of the column. A `percentWidth` of zero or more makes the
/// column that share of the pane's width less the gaps, whatever the
/// sizes say. A `hgrow` or `halignment` left `null` falls back to what the
/// children ask for.
public class ColumnConstraints extends ConstraintsBase {

    private final DoubleProperty min = new Num(this, "minWidth", Region.USE_COMPUTED_SIZE);
    private final DoubleProperty pref = new Num(this, "prefWidth", Region.USE_COMPUTED_SIZE);
    private final DoubleProperty max = new Num(this, "maxWidth", Region.USE_COMPUTED_SIZE);
    private final DoubleProperty percent = new Num(this, "percentWidth", -1);
    private final ObjectProperty<Priority> grow = new Ref<Priority>(this, "hgrow");
    private final ObjectProperty<HPos> align = new Ref<HPos>(this, "halignment");
    private final BooleanProperty fill = new Flag(this, "fillWidth", true);

    /// Creates a constraint that computes everything from the children.
    public ColumnConstraints() {
    }

    /// Creates a column of a fixed width.
    public ColumnConstraints(double width) {
        min.set(Region.USE_PREF_SIZE);
        pref.set(width);
        max.set(Region.USE_PREF_SIZE);
    }

    /// Creates a column with a range of sizes.
    public ColumnConstraints(double minWidth, double prefWidth, double maxWidth) {
        min.set(minWidth);
        pref.set(prefWidth);
        max.set(maxWidth);
    }

    /// Creates a column with a range of sizes, a grow priority, an
    /// alignment for its children and whether it stretches them.
    public ColumnConstraints(double minWidth, double prefWidth, double maxWidth, Priority hgrow, HPos halignment,
            boolean fillWidth) {
        min.set(minWidth);
        pref.set(prefWidth);
        max.set(maxWidth);
        grow.set(hgrow);
        align.set(halignment);
        fill.set(fillWidth);
    }

    /// Returns the minimum width, `Region.USE_COMPUTED_SIZE` or
    /// `Region.USE_PREF_SIZE`.
    public final double getMinWidth() {
        return min.get();
    }

    /// Sets the minimum width.
    public final void setMinWidth(double value) {
        min.set(value);
    }

    /// The minimum width of the column.
    public final DoubleProperty minWidthProperty() {
        return min;
    }

    /// Returns the preferred width, or `Region.USE_COMPUTED_SIZE`.
    public final double getPrefWidth() {
        return pref.get();
    }

    /// Sets the preferred width.
    public final void setPrefWidth(double value) {
        pref.set(value);
    }

    /// The preferred width of the column.
    public final DoubleProperty prefWidthProperty() {
        return pref;
    }

    /// Returns the maximum width, `Region.USE_COMPUTED_SIZE` or
    /// `Region.USE_PREF_SIZE`.
    public final double getMaxWidth() {
        return max.get();
    }

    /// Sets the maximum width.
    public final void setMaxWidth(double value) {
        max.set(value);
    }

    /// The maximum width of the column.
    public final DoubleProperty maxWidthProperty() {
        return max;
    }

    /// Returns the share of the pane in percent, or a negative number.
    public final double getPercentWidth() {
        return percent.get();
    }

    /// Sets the share of the pane this column takes, in percent; a negative
    /// number turns it off.
    public final void setPercentWidth(double value) {
        percent.set(value);
    }

    /// The share of the pane this column takes, in percent.
    public final DoubleProperty percentWidthProperty() {
        return percent;
    }

    /// Returns how eagerly the column takes spare space, or `null`.
    public final Priority getHgrow() {
        return grow.get();
    }

    /// Sets how eagerly the column takes spare space.
    public final void setHgrow(Priority value) {
        grow.set(value);
    }

    /// How eagerly the column takes spare space.
    public final ObjectProperty<Priority> hgrowProperty() {
        return grow;
    }

    /// Returns where the children sit in the column, or `null`.
    public final HPos getHalignment() {
        return align.get();
    }

    /// Sets where the children sit in the column.
    public final void setHalignment(HPos value) {
        align.set(value);
    }

    /// Where the children sit in the column.
    public final ObjectProperty<HPos> halignmentProperty() {
        return align;
    }

    /// Returns whether resizable children are stretched to the width of
    /// the column.
    public final boolean isFillWidth() {
        return fill.get();
    }

    /// Sets whether resizable children are stretched to the width of the
    /// column.
    public final void setFillWidth(boolean value) {
        fill.set(value);
    }

    /// Whether resizable children are stretched to the width of the column.
    public final BooleanProperty fillWidthProperty() {
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
        return "ColumnConstraints percentWidth=" + getPercentWidth() + " minWidth=" + getMinWidth() + " prefWidth="
                + getPrefWidth() + " maxWidth=" + getMaxWidth() + " hgrow=" + getHgrow() + " fillWidth="
                + isFillWidth() + " halignment=" + getHalignment();
    }
}
