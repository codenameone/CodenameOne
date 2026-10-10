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
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;

/// Lays its managed children out in a grid of rows and columns.
///
/// A child sits in the cell its column and row index constraints name
/// (0, 0 without them) and may span several cells; a span of
/// [#REMAINING] reaches the last row or column. Several children may
/// share a cell. The grid has as many rows and columns as the children
/// and the constraint lists need.
///
/// #### Sizes
///
/// A column is as wide as the widest child that sits in it alone, and a
/// row as tall as its tallest, unless a `ColumnConstraints` or
/// `RowConstraints` at its index says otherwise. A child that spans
/// several columns widens them when it needs more than they have: the
/// columns that grow share what is missing, or all of the spanned columns
/// that have no size of their own when none grows.
///
/// A column with a percentage takes that share of the pane's width less
/// the gaps. When the pane is wider than the rest need, the spare width
/// goes in equal shares to the columns that grow `ALWAYS`, then to those
/// that grow `SOMETIMES`, each up to its maximum; when it is narrower,
/// every column without a percentage gives up an equal share down to its
/// minimum. A column grows as its constraint says or, when that says
/// nothing, as eagerly as the most eager child in it. Without any growing
/// column the grid keeps its size and is placed by `alignment`. Rows
/// behave the same way with heights.
///
/// #### In the cell
///
/// A resizable child is stretched to its cell, up to its maximum size,
/// unless a fill constraint on the child or on the row or column says
/// no. A child that does not fill the cell is placed by its own
/// alignment constraints, then by those of the row and column, and
/// otherwise left and vertically centered.
///
/// Grid lines (`gridLinesVisible`) are drawn solid along the edges of
/// every column and row, behind the children; JavaFX dashes them.
///
/// #### Styling
///
/// Besides the names of a region: `-fx-hgap`, `-fx-vgap` (`Number`) and
/// `-fx-alignment` (`javafx.geometry.Pos`, or its CSS spelling as a
/// `String`, matched without regard to ASCII case).
public class GridPane extends Pane {

    /// A span that reaches the last row or column of the grid.
    public static final int REMAINING = Integer.MAX_VALUE;

    private static final String ROW = "gridpane-row";
    private static final String COLUMN = "gridpane-column";
    private static final String ROW_SPAN = "gridpane-row-span";
    private static final String COLUMN_SPAN = "gridpane-column-span";
    private static final String MARGIN = "gridpane-margin";
    private static final String HALIGNMENT = "gridpane-halignment";
    private static final String VALIGNMENT = "gridpane-valignment";
    private static final String HGROW = "gridpane-hgrow";
    private static final String VGROW = "gridpane-vgrow";
    private static final String FILL_WIDTH = "gridpane-fill-width";
    private static final String FILL_HEIGHT = "gridpane-fill-height";

    private final javafx.beans.property.BooleanProperty gridLinesVisible =
            new com.codename1.fxcompat.runtime.FxBoolean(this, "gridLinesVisible", false, Dirty.PAINT);
    private final DoubleProperty hgap = new FxDouble(this, "hgap", 0, Dirty.LAYOUT);
    private final DoubleProperty vgap = new FxDouble(this, "vgap", 0, Dirty.LAYOUT);
    private final ObjectProperty<Pos> alignment = new FxObject<Pos>(this, "alignment", Pos.TOP_LEFT, Dirty.LAYOUT);
    private final ObservableList<RowConstraints> rowConstraints = FXCollections.observableArrayList();
    private final ObservableList<ColumnConstraints> columnConstraints = FXCollections.observableArrayList();

    private double[] columnStarts = new double[0];
    private double[] columnWidths = new double[0];
    private double[] rowStarts = new double[0];
    private double[] rowHeights = new double[0];

    /// Creates an empty grid without gaps.
    public GridPane() {
        rowConstraints.addListener(new ListChangeListener<RowConstraints>() {
            @Override
            public void onChanged(Change<? extends RowConstraints> change) {
                constraintsChanged(change);
            }
        });
        columnConstraints.addListener(new ListChangeListener<ColumnConstraints>() {
            @Override
            public void onChanged(Change<? extends ColumnConstraints> change) {
                constraintsChanged(change);
            }
        });
    }

    private void constraintsChanged(ListChangeListener.Change<? extends ConstraintsBase> change) {
        while (change.next()) {
            List<? extends ConstraintsBase> removed = change.getRemoved();
            for (int i = 0; i < removed.size(); i++) {
                ConstraintsBase c = removed.get(i);
                if (c != null) {
                    c.remove(this);
                }
            }
            List<? extends ConstraintsBase> added = change.getAddedSubList();
            for (int i = 0; i < added.size(); i++) {
                ConstraintsBase c = added.get(i);
                if (c != null) {
                    c.add(this);
                }
            }
        }
        requestLayout();
    }

    // ------------------------------------------------------- constraints

    private static void checkIndex(Integer value, String what) {
        if (value != null && value.intValue() < 0) {
            throw new IllegalArgumentException(what + " must be greater or equal to 0, but was " + value);
        }
    }

    private static void checkSpan(Integer value, String what) {
        if (value != null && value.intValue() < 1) {
            throw new IllegalArgumentException(what + " must be greater or equal to 1, but was " + value);
        }
    }

    /// Sets the row a child sits in; `null` removes the constraint.
    public static void setRowIndex(Node child, Integer value) {
        checkIndex(value, "rowIndex");
        setConstraint(child, ROW, value);
    }

    /// Returns the row index constraint of a child, or `null`.
    public static Integer getRowIndex(Node child) {
        return LayoutSupport.integer(child, ROW);
    }

    /// Sets the column a child sits in; `null` removes the constraint.
    public static void setColumnIndex(Node child, Integer value) {
        checkIndex(value, "columnIndex");
        setConstraint(child, COLUMN, value);
    }

    /// Returns the column index constraint of a child, or `null`.
    public static Integer getColumnIndex(Node child) {
        return LayoutSupport.integer(child, COLUMN);
    }

    /// Sets how many rows a child spans, or [#REMAINING]; `null` removes
    /// the constraint.
    public static void setRowSpan(Node child, Integer value) {
        checkSpan(value, "rowSpan");
        setConstraint(child, ROW_SPAN, value);
    }

    /// Returns the row span constraint of a child, or `null`.
    public static Integer getRowSpan(Node child) {
        return LayoutSupport.integer(child, ROW_SPAN);
    }

    /// Sets how many columns a child spans, or [#REMAINING]; `null`
    /// removes the constraint.
    public static void setColumnSpan(Node child, Integer value) {
        checkSpan(value, "columnSpan");
        setConstraint(child, COLUMN_SPAN, value);
    }

    /// Returns the column span constraint of a child, or `null`.
    public static Integer getColumnSpan(Node child) {
        return LayoutSupport.integer(child, COLUMN_SPAN);
    }

    /// Sets the space kept free around a child in its cell; `null`
    /// removes the constraint.
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN, value);
    }

    /// Returns the margin of a child, or `null`.
    public static Insets getMargin(Node child) {
        return LayoutSupport.insets(child, MARGIN);
    }

    /// Sets where a child sits horizontally in its cell; `null` removes
    /// the constraint.
    public static void setHalignment(Node child, HPos value) {
        setConstraint(child, HALIGNMENT, value);
    }

    /// Returns the horizontal alignment constraint of a child, or `null`.
    public static HPos getHalignment(Node child) {
        Object v = getConstraint(child, HALIGNMENT);
        return v instanceof HPos ? (HPos) v : null;
    }

    /// Sets where a child sits vertically in its cell; `null` removes the
    /// constraint.
    public static void setValignment(Node child, VPos value) {
        setConstraint(child, VALIGNMENT, value);
    }

    /// Returns the vertical alignment constraint of a child, or `null`.
    public static VPos getValignment(Node child) {
        Object v = getConstraint(child, VALIGNMENT);
        return v instanceof VPos ? (VPos) v : null;
    }

    /// Sets how eagerly the columns of a child take spare width; `null`
    /// removes the constraint.
    public static void setHgrow(Node child, Priority value) {
        setConstraint(child, HGROW, value);
    }

    /// Returns the horizontal grow constraint of a child, or `null`.
    public static Priority getHgrow(Node child) {
        return LayoutSupport.priority(child, HGROW);
    }

    /// Sets how eagerly the rows of a child take spare height; `null`
    /// removes the constraint.
    public static void setVgrow(Node child, Priority value) {
        setConstraint(child, VGROW, value);
    }

    /// Returns the vertical grow constraint of a child, or `null`.
    public static Priority getVgrow(Node child) {
        return LayoutSupport.priority(child, VGROW);
    }

    /// Sets whether a child is stretched to the width of its cell; `null`
    /// removes the constraint.
    public static void setFillWidth(Node child, Boolean value) {
        setConstraint(child, FILL_WIDTH, value);
    }

    /// Returns the fill width constraint of a child, or `null`.
    public static Boolean isFillWidth(Node child) {
        Object v = getConstraint(child, FILL_WIDTH);
        return v instanceof Boolean ? (Boolean) v : null;
    }

    /// Sets whether a child is stretched to the height of its cell;
    /// `null` removes the constraint.
    public static void setFillHeight(Node child, Boolean value) {
        setConstraint(child, FILL_HEIGHT, value);
    }

    /// Returns the fill height constraint of a child, or `null`.
    public static Boolean isFillHeight(Node child) {
        Object v = getConstraint(child, FILL_HEIGHT);
        return v instanceof Boolean ? (Boolean) v : null;
    }

    /// Sets the cell of a child.
    public static void setConstraints(Node child, int columnIndex, int rowIndex) {
        setRowIndex(child, Integer.valueOf(rowIndex));
        setColumnIndex(child, Integer.valueOf(columnIndex));
    }

    /// Sets the cell and the spans of a child.
    public static void setConstraints(Node child, int columnIndex, int rowIndex, int columnspan, int rowspan) {
        setRowIndex(child, Integer.valueOf(rowIndex));
        setColumnIndex(child, Integer.valueOf(columnIndex));
        setRowSpan(child, Integer.valueOf(rowspan));
        setColumnSpan(child, Integer.valueOf(columnspan));
    }

    /// Sets the cell, the spans and the alignment of a child.
    public static void setConstraints(Node child, int columnIndex, int rowIndex, int columnspan, int rowspan,
            HPos halignment, VPos valignment) {
        setConstraints(child, columnIndex, rowIndex, columnspan, rowspan);
        setHalignment(child, halignment);
        setValignment(child, valignment);
    }

    /// Sets the cell, the spans, the alignment and the grow priorities of
    /// a child.
    public static void setConstraints(Node child, int columnIndex, int rowIndex, int columnspan, int rowspan,
            HPos halignment, VPos valignment, Priority hgrow, Priority vgrow) {
        setConstraints(child, columnIndex, rowIndex, columnspan, rowspan);
        setHalignment(child, halignment);
        setValignment(child, valignment);
        setHgrow(child, hgrow);
        setVgrow(child, vgrow);
    }

    /// Sets the cell, the spans, the alignment, the grow priorities and
    /// the margin of a child.
    public static void setConstraints(Node child, int columnIndex, int rowIndex, int columnspan, int rowspan,
            HPos halignment, VPos valignment, Priority hgrow, Priority vgrow, Insets margin) {
        setConstraints(child, columnIndex, rowIndex, columnspan, rowspan);
        setHalignment(child, halignment);
        setValignment(child, valignment);
        setHgrow(child, hgrow);
        setVgrow(child, vgrow);
        setMargin(child, margin);
    }

    /// Removes the grid constraints from a child.
    public static void clearConstraints(Node child) {
        setRowIndex(child, null);
        setColumnIndex(child, null);
        setRowSpan(child, null);
        setColumnSpan(child, null);
        setHalignment(child, null);
        setValignment(child, null);
        setHgrow(child, null);
        setVgrow(child, null);
        setMargin(child, null);
        setFillWidth(child, null);
        setFillHeight(child, null);
    }

    // -------------------------------------------------------- properties

    /// Returns the gap between columns.
    public final double getHgap() {
        return hgap.get();
    }

    /// Sets the gap between columns.
    public final void setHgap(double value) {
        hgap.set(value);
    }

    /// The gap between columns.
    public final DoubleProperty hgapProperty() {
        return hgap;
    }

    /// Returns the gap between rows.
    public final double getVgap() {
        return vgap.get();
    }

    /// Sets the gap between rows.
    public final void setVgap(double value) {
        vgap.set(value);
    }

    /// The gap between rows.
    public final DoubleProperty vgapProperty() {
        return vgap;
    }

    /// Returns where the grid sits when the pane has room to spare.
    public final Pos getAlignment() {
        return alignment.get();
    }

    /// Sets where the grid sits when the pane has room to spare.
    public final void setAlignment(Pos value) {
        alignment.set(value);
    }

    /// Where the grid sits when the pane has room to spare.
    public final ObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    /// Returns the constraints of the rows, by row index.
    public final ObservableList<RowConstraints> getRowConstraints() {
        return rowConstraints;
    }

    /// Returns the constraints of the columns, by column index.
    public final ObservableList<ColumnConstraints> getColumnConstraints() {
        return columnConstraints;
    }

    /// Adds a child in a cell.
    public void add(Node child, int columnIndex, int rowIndex) {
        setConstraints(child, columnIndex, rowIndex);
        getChildren().add(child);
    }

    /// Adds a child in a cell, spanning columns and rows.
    public void add(Node child, int columnIndex, int rowIndex, int colspan, int rowspan) {
        setConstraints(child, columnIndex, rowIndex, colspan, rowspan);
        getChildren().add(child);
    }

    /// Adds children to a row, each in the next column after what the row
    /// already holds.
    public void addRow(int rowIndex, Node... children) {
        int next = nextFree(false, rowIndex);
        for (int i = 0; i < children.length; i++) {
            setConstraints(children[i], next + i, rowIndex);
        }
        getChildren().addAll(children);
    }

    /// Adds children to a column, each in the next row after what the
    /// column already holds.
    public void addColumn(int columnIndex, Node... children) {
        int next = nextFree(true, columnIndex);
        for (int i = 0; i < children.length; i++) {
            setConstraints(children[i], columnIndex, next + i);
        }
        getChildren().addAll(children);
    }

    /// The first index along a column (or a row) after the children that
    /// already sit in it.
    private int nextFree(boolean column, int line) {
        List<Node> managed = getManagedChildren();
        int next = 0;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            int at = index(child, column);
            int span = span(child, column);
            if (line >= at && (span == REMAINING || line <= at + span - 1)) {
                int other = index(child, !column);
                int otherSpan = span(child, !column);
                next = Math.max(next, (otherSpan == REMAINING ? other : other + otherSpan - 1) + 1);
            }
        }
        return next;
    }

    /// Returns the number of rows of the grid.
    public final int getRowCount() {
        List<Node> managed = getManagedChildren();
        return count(false, managed);
    }

    /// Returns the number of columns of the grid.
    public final int getColumnCount() {
        List<Node> managed = getManagedChildren();
        return count(true, managed);
    }

    /// Returns the bounds of a cell as of the last layout, in the
    /// coordinates of the pane; a cell outside the grid has no size and
    /// sits after the last row or column.
    public final Bounds getCellBounds(int columnIndex, int rowIndex) {
        Insets in = getInsets();
        double x = in.getLeft();
        double w = 0;
        if (columnIndex >= 0 && columnIndex < columnWidths.length) {
            x = columnStarts[columnIndex];
            w = columnWidths[columnIndex];
        } else if (columnWidths.length > 0) {
            int last = columnWidths.length - 1;
            x = columnStarts[last] + columnWidths[last];
        }
        double y = in.getTop();
        double h = 0;
        if (rowIndex >= 0 && rowIndex < rowHeights.length) {
            y = rowStarts[rowIndex];
            h = rowHeights[rowIndex];
        } else if (rowHeights.length > 0) {
            int last = rowHeights.length - 1;
            y = rowStarts[last] + rowHeights[last];
        }
        return new BoundingBox(x, y, w, h);
    }

    // ------------------------------------------------------------ tracks

    private static int index(Node child, boolean column) {
        Integer v = column ? getColumnIndex(child) : getRowIndex(child);
        return v == null ? 0 : v.intValue();
    }

    private static int span(Node child, boolean column) {
        Integer v = column ? getColumnSpan(child) : getRowSpan(child);
        return v == null ? 1 : v.intValue();
    }

    /// The last row or column a child covers.
    private static int last(Node child, boolean column, int count) {
        int at = index(child, column);
        int span = span(child, column);
        return span == REMAINING ? Math.max(at, count - 1) : at + span - 1;
    }

    private int count(boolean column, List<Node> managed) {
        int n = column ? columnConstraints.size() : rowConstraints.size();
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            int at = index(child, column);
            int span = span(child, column);
            n = Math.max(n, span == REMAINING ? at + 1 : at + span);
        }
        return n;
    }

    private ConstraintsBase constraint(boolean column, int i) {
        if (column) {
            return i < columnConstraints.size() ? columnConstraints.get(i) : null;
        }
        return i < rowConstraints.size() ? rowConstraints.get(i) : null;
    }

    /// The rows or the columns of the grid as layout sees them.
    private static final class Tracks {
        final double[] min;
        final double[] pref;
        final double[] max;
        /// The share of the pane as a fraction, or a negative number.
        final double[] share;
        final boolean[] fixedPref;
        final boolean[] fixedMin;
        final Priority[] grow;
        double gap;

        Tracks(int n) {
            min = new double[n];
            pref = new double[n];
            max = new double[n];
            share = new double[n];
            fixedPref = new boolean[n];
            fixedMin = new boolean[n];
            grow = new Priority[n];
        }

        double gaps() {
            return min.length > 1 ? gap * (min.length - 1) : 0;
        }
    }

    private double areaSize(Node child, boolean column, boolean minimum, double given) {
        Insets m = getMargin(child);
        if (column) {
            return minimum ? computeChildMinAreaWidth(child, m, given) : computeChildPrefAreaWidth(child, m, given);
        }
        return minimum ? computeChildMinAreaHeight(child, m, given) : computeChildPrefAreaHeight(child, m, given);
    }

    private static double range(double[] sizes, int from, int to, double gap) {
        double total = 0;
        for (int i = from; i <= to && i < sizes.length; i++) {
            total += sizes[i];
        }
        return to > from ? total + gap * (to - from) : total;
    }

    /// Widens the tracks a spanning child covers until they hold it.
    private static void widen(double[] sizes, boolean[] fixed, Tracks t, int from, int to, double need) {
        double missing = need - range(sizes, from, to, t.gap);
        if (!(missing > 0)) {
            return;
        }
        for (int pass = 0; pass < 3; pass++) {
            int takers = 0;
            for (int i = from; i <= to; i++) {
                if (takes(fixed, t, i, pass)) {
                    takers++;
                }
            }
            if (takers > 0) {
                double each = missing / takers;
                for (int i = from; i <= to; i++) {
                    if (takes(fixed, t, i, pass)) {
                        sizes[i] += each;
                    }
                }
                return;
            }
        }
    }

    private static boolean takes(boolean[] fixed, Tracks t, int i, int pass) {
        if (fixed[i] || t.share[i] >= 0) {
            return false;
        }
        if (pass == 0) {
            return t.grow[i] == Priority.ALWAYS;
        } else if (pass == 1) {
            return t.grow[i] == Priority.SOMETIMES;
        }
        return true;
    }

    /// Measures the columns, or the rows. `widths` are the widths of the
    /// columns when the rows are measured for a known width, else `null`.
    private Tracks measure(boolean column, List<Node> managed, double[] widths, double widthGap) {
        int n = count(column, managed);
        int columns = column ? n : count(true, managed);
        Tracks t = new Tracks(n);
        t.gap = column ? snapSpaceX(getHgap()) : snapSpaceY(getVgap());
        double[] setMin = new double[n];
        double[] setPref = new double[n];
        double[] setMax = new double[n];
        boolean[] ownGrow = new boolean[n];
        double shares = 0;
        for (int i = 0; i < n; i++) {
            ConstraintsBase c = constraint(column, i);
            setMin[i] = c == null ? USE_COMPUTED_SIZE : c.minSize();
            setPref[i] = c == null ? USE_COMPUTED_SIZE : c.prefSize();
            setMax[i] = c == null ? USE_COMPUTED_SIZE : c.maxSize();
            double percent = c == null ? -1 : c.percent();
            t.share[i] = percent >= 0 ? percent / 100 : -1;
            if (percent >= 0) {
                shares += percent / 100;
            }
            t.grow[i] = c == null ? null : c.grow();
            ownGrow[i] = t.grow[i] != null;
            t.fixedPref[i] = setPref[i] != USE_COMPUTED_SIZE;
            t.fixedMin[i] = setMin[i] != USE_COMPUTED_SIZE;
        }
        if (shares > 1) {
            for (int i = 0; i < n; i++) {
                if (t.share[i] >= 0) {
                    t.share[i] = t.share[i] / shares;
                }
            }
        }
        List<List<Node>> onBaseline = null;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            int from = index(child, column);
            int to = last(child, column, n);
            Priority wants = column ? getHgrow(child) : getVgrow(child);
            if (wants != null) {
                for (int k = from; k <= to; k++) {
                    if (!ownGrow[k]) {
                        t.grow[k] = t.grow[k] == null ? wants : Priority.max(t.grow[k], wants);
                    }
                }
            }
            if (from != to) {
                continue;
            }
            double given = column || widths == null ? -1
                    : range(widths, index(child, true), last(child, true, columns), widthGap);
            t.min[from] = Math.max(t.min[from], areaSize(child, column, true, given));
            t.pref[from] = Math.max(t.pref[from], areaSize(child, column, false, given));
            if (!column && valignment(child, from) == VPos.BASELINE) {
                if (onBaseline == null) {
                    onBaseline = new ArrayList<List<Node>>();
                    for (int k = 0; k < n; k++) {
                        onBaseline.add(new ArrayList<Node>());
                    }
                }
                onBaseline.get(from).add(child);
            }
        }
        if (onBaseline != null) {
            for (int i = 0; i < n; i++) {
                if (!onBaseline.get(i).isEmpty()) {
                    t.pref[i] = Math.max(t.pref[i], LayoutSupport.baselineAreaHeight(this, onBaseline.get(i), MARGIN));
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (t.fixedPref[i]) {
                double low = !t.fixedMin[i] || setMin[i] == USE_PREF_SIZE ? 0 : setMin[i];
                double high = setMax[i] == USE_COMPUTED_SIZE || setMax[i] == USE_PREF_SIZE ? Double.MAX_VALUE
                        : setMax[i];
                t.pref[i] = boundedSize(low, setPref[i], high);
            }
            if (t.fixedMin[i]) {
                t.min[i] = setMin[i] == USE_PREF_SIZE ? t.pref[i] : setMin[i];
            }
        }
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            int from = index(child, column);
            int to = last(child, column, n);
            if (from == to) {
                continue;
            }
            double given = column || widths == null ? -1
                    : range(widths, index(child, true), last(child, true, columns), widthGap);
            widen(t.pref, t.fixedPref, t, from, to, areaSize(child, column, false, given));
            widen(t.min, t.fixedMin, t, from, to, areaSize(child, column, true, given));
        }
        for (int i = 0; i < n; i++) {
            if (setMax[i] == USE_COMPUTED_SIZE) {
                t.max[i] = Double.MAX_VALUE;
            } else if (setMax[i] == USE_PREF_SIZE) {
                t.max[i] = t.pref[i];
            } else {
                t.max[i] = setMax[i];
            }
            if (t.max[i] < t.min[i]) {
                t.max[i] = t.min[i];
            }
            if (t.pref[i] > t.max[i]) {
                t.pref[i] = t.max[i];
            }
            if (t.pref[i] < t.min[i]) {
                if (t.fixedPref[i] && !t.fixedMin[i]) {
                    t.min[i] = t.pref[i];
                } else {
                    t.pref[i] = t.min[i];
                }
            }
            t.pref[i] = column ? snapSizeX(t.pref[i]) : snapSizeY(t.pref[i]);
            t.min[i] = column ? snapSizeX(t.min[i]) : snapSizeY(t.min[i]);
        }
        return t;
    }

    /// The length the grid needs for its tracks at given sizes, gaps
    /// included: large enough for every percentage to hold what its track
    /// needs and for the rest to fit in what the percentages leave.
    private static double total(Tracks t, double[] sizes) {
        double rest = 0;
        double shares = 0;
        double content = 0;
        for (int i = 0; i < sizes.length; i++) {
            if (t.share[i] >= 0) {
                shares += t.share[i];
                if (t.share[i] > 0) {
                    content = Math.max(content, sizes[i] / t.share[i]);
                }
            } else {
                rest += sizes[i];
            }
        }
        if (shares > 0 && shares < 1) {
            content = Math.max(content, rest / (1 - shares));
        } else {
            content = Math.max(content, rest);
        }
        return content + t.gaps();
    }

    /// Fits the tracks to a length and answers their sizes.
    private double[] resolve(Tracks t, double length, boolean column) {
        int n = t.pref.length;
        double[] sizes = new double[n];
        double space = Math.max(0, length - t.gaps());
        double used = 0;
        for (int i = 0; i < n; i++) {
            if (t.share[i] >= 0) {
                sizes[i] = column ? snapSpaceX(space * t.share[i]) : snapSpaceY(space * t.share[i]);
            } else {
                sizes[i] = t.pref[i];
            }
            used += sizes[i];
        }
        double extra = space - used;
        if (extra < 0) {
            double[] limits = new double[n];
            for (int i = 0; i < n; i++) {
                limits[i] = t.share[i] >= 0 ? Double.NaN : t.min[i];
            }
            LayoutSupport.distribute(this, sizes, limits, extra);
        } else if (extra > 0) {
            double remaining = extra;
            for (int pass = 0; pass < 2; pass++) {
                Priority wanted = pass == 0 ? Priority.ALWAYS : Priority.SOMETIMES;
                double[] limits = new double[n];
                for (int i = 0; i < n; i++) {
                    limits[i] = t.share[i] < 0 && t.grow[i] == wanted ? t.max[i] : Double.NaN;
                }
                remaining = LayoutSupport.distribute(this, sizes, limits, remaining);
            }
        }
        return sizes;
    }

    private double[] widthsFor(List<Node> managed, double width) {
        Tracks t = measure(true, managed, null, 0);
        Insets in = getInsets();
        double length = width == -1 ? total(t, t.pref) : width - in.getLeft() - in.getRight();
        return resolve(t, length, true);
    }

    @Override
    public Orientation getContentBias() {
        List<Node> managed = getManagedChildren();
        return LayoutSupport.bias(managed);
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        Tracks t = measure(true, managed, null, 0);
        return in.getLeft() + total(t, t.min) + in.getRight();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        Tracks t = measure(true, managed, null, 0);
        return in.getLeft() + total(t, t.pref) + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        Tracks t = measure(false, managed, widthsFor(managed, width), snapSpaceX(getHgap()));
        return in.getTop() + total(t, t.min) + in.getBottom();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        Tracks t = measure(false, managed, widthsFor(managed, width), snapSpaceX(getHgap()));
        return in.getTop() + total(t, t.pref) + in.getBottom();
    }

    private static double[] starts(double[] sizes, double first, double gap) {
        double[] out = new double[sizes.length];
        double at = first;
        for (int i = 0; i < sizes.length; i++) {
            out[i] = at;
            at += sizes[i] + gap;
        }
        return out;
    }

    private HPos halignment(Node child, int column) {
        HPos own = getHalignment(child);
        if (own != null) {
            return own;
        }
        if (column < columnConstraints.size()) {
            ColumnConstraints c = columnConstraints.get(column);
            if (c != null && c.getHalignment() != null) {
                return c.getHalignment();
            }
        }
        return HPos.LEFT;
    }

    private VPos valignment(Node child, int row) {
        VPos own = getValignment(child);
        if (own != null) {
            return own;
        }
        if (row < rowConstraints.size()) {
            RowConstraints c = rowConstraints.get(row);
            if (c != null && c.getValignment() != null) {
                return c.getValignment();
            }
        }
        return VPos.CENTER;
    }

    private boolean fills(Node child, boolean column, int at) {
        Boolean own = column ? isFillWidth(child) : isFillHeight(child);
        if (own != null) {
            return own.booleanValue();
        }
        ConstraintsBase c = constraint(column, at);
        return c == null || c.fill();
    }

    @Override
    protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        Insets in = getInsets();
        double insideWidth = getWidth() - in.getLeft() - in.getRight();
        double insideHeight = getHeight() - in.getTop() - in.getBottom();
        Tracks columns = measure(true, managed, null, 0);
        double[] widths = resolve(columns, insideWidth, true);
        Tracks rows = measure(false, managed, widths, columns.gap);
        double[] heights = resolve(rows, insideHeight, false);
        Pos pos = alignment.get() == null ? Pos.TOP_LEFT : alignment.get();
        double x0 = in.getLeft()
                + LayoutSupport.xOffset(insideWidth, LayoutSupport.sum(widths, columns.gap), pos.getHpos());
        double y0 = in.getTop()
                + LayoutSupport.yOffset(insideHeight, LayoutSupport.sum(heights, rows.gap), pos.getVpos());
        double[] xs = starts(widths, x0, columns.gap);
        double[] ys = starts(heights, y0, rows.gap);
        columnStarts = xs;
        columnWidths = widths;
        rowStarts = ys;
        rowHeights = heights;
        double[] baselines = null;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            int c0 = index(child, true);
            int c1 = last(child, true, widths.length);
            int r0 = index(child, false);
            int r1 = last(child, false, heights.length);
            double w = range(widths, c0, c1, columns.gap);
            double h = range(heights, r0, r1, rows.gap);
            HPos hpos = halignment(child, c0);
            VPos vpos = valignment(child, r0);
            boolean fillWidth = fills(child, true, c0);
            if (vpos == VPos.BASELINE) {
                if (baselines == null) {
                    baselines = rowBaselines(managed, heights.length);
                }
                LayoutSupport.layoutOnBaseline(this, child, xs[c0], ys[r0], w, h, baselines[r0], getMargin(child),
                        fillWidth, hpos);
            } else {
                layoutInArea(child, xs[c0], ys[r0], w, h, 0, getMargin(child), fillWidth, fills(child, false, r0),
                        hpos, vpos);
            }
        }
    }

    /// The baseline of each row: the lowest baseline among the children
    /// aligned to it that sit in that row alone.
    private double[] rowBaselines(List<Node> managed, int rows) {
        double[] out = new double[rows];
        for (int r = 0; r < rows; r++) {
            List<Node> row = new ArrayList<Node>();
            for (int i = 0; i < managed.size(); i++) {
                Node child = managed.get(i);
                if (index(child, false) == r && valignment(child, r) == VPos.BASELINE) {
                    row.add(child);
                }
            }
            out[r] = LayoutSupport.areaBaseline(this, row, MARGIN);
        }
        return out;
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-hgap".equals(property)) {
            return Double.valueOf(getHgap());
        } else if ("-fx-vgap".equals(property)) {
            return Double.valueOf(getVgap());
        } else if ("-fx-alignment".equals(property)) {
            return getAlignment();
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
        }
        return super.cn1SetStyleValue(property, value);
    }

    /// Sets whether the edges of the columns and rows are drawn, which is
    /// meant for looking at a layout while it is made.
    public final void setGridLinesVisible(boolean value) {
        gridLinesVisible.set(value);
    }

    /// Returns whether the edges of the columns and rows are drawn.
    public final boolean isGridLinesVisible() {
        return gridLinesVisible.get();
    }

    /// Whether the edges of the columns and rows are drawn.
    public final javafx.beans.property.BooleanProperty gridLinesVisibleProperty() {
        return gridLinesVisible;
    }

    @Override
    public void cn1Paint(com.codename1.fxcompat.runtime.Renderer renderer) {
        super.cn1Paint(renderer);
        int columns = Math.min(columnStarts.length, columnWidths.length);
        int rows = Math.min(rowStarts.length, rowHeights.length);
        if (!isGridLinesVisible() || columns == 0 || rows == 0) {
            return;
        }
        javafx.scene.paint.Color line = javafx.scene.paint.Color.rgb(30, 30, 30);
        double left = columnStarts[0];
        double right = columnStarts[columns - 1] + columnWidths[columns - 1];
        double top = rowStarts[0];
        double bottom = rowStarts[rows - 1] + rowHeights[rows - 1];
        for (int i = 0; i < columns; i++) {
            renderer.fillRect(columnStarts[i], top, 1, bottom - top, line);
            renderer.fillRect(columnStarts[i] + columnWidths[i], top, 1, bottom - top + 1, line);
        }
        for (int i = 0; i < rows; i++) {
            renderer.fillRect(left, rowStarts[i], right - left, 1, line);
            renderer.fillRect(left, rowStarts[i] + rowHeights[i], right - left + 1, 1, line);
        }
    }
}
