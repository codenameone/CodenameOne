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
package com.codename1.ui.table;

import com.codename1.ui.Component;

import java.util.Comparator;

/// One column of a `DataGrid`: which column of the model it shows, its
/// title and width, and how its values are painted, compared and edited.
///
/// A column is a description, not a component. Changing one after it was
/// added to a grid takes effect on the grid's next layout; call
/// `DataGrid#refresh()` to ask for it.
///
/// ```java
/// GridColumn price = new GridColumn(2, "Price")
///         .setKind(GridColumn.Kind.NUMBER)
///         .setWidthMM(22)
///         .setAggregate(GridAggregate.SUM);
/// ```
public class GridColumn {

    /// What a column holds, which decides how it is aligned, compared and
    /// edited. `AUTO` decides from each value.
    public enum Kind {
        /// Decided from the value: a `Boolean` is a check box, a `Number`
        /// is right aligned, anything else is text.
        AUTO,
        /// Text, edited in a text field.
        TEXT,
        /// A number, right aligned and edited in a numeric text field.
        NUMBER,
        /// A check box, toggled in place with no editor.
        BOOLEAN,
        /// A `java.util.Date`, edited in a date picker.
        DATE,
        /// One of `getChoices()`, edited in a picker.
        CHOICE
    }

    private final int modelColumn;
    private String title;
    private Kind kind = Kind.AUTO;
    private float widthMM;
    private float minWidthMM = 8;
    private float maxWidthMM = 120;
    private int flex;
    private int alignment = -1;
    private boolean sortable = true;
    private boolean resizable = true;
    private boolean movable = true;
    private boolean editable = true;
    private boolean visible = true;
    private GridCellRenderer renderer;
    private GridCellEditor editor;
    private GridFormatter formatter;
    private Comparator<Object> comparator;
    private GridAggregate aggregate;
    private Object[] choices;
    private int width = -1;
    private boolean resized;

    /// A column showing column `modelColumn` of the model, titled `title`.
    public GridColumn(int modelColumn, String title) {
        this.modelColumn = modelColumn;
        this.title = title;
    }

    /// The column of the model this column shows.
    public int getModelColumn() {
        return modelColumn;
    }

    /// The text in the header.
    public String getTitle() {
        return title;
    }

    /// Sets the text in the header.
    public GridColumn setTitle(String title) {
        this.title = title;
        return this;
    }

    /// What the column holds.
    public Kind getKind() {
        return kind;
    }

    /// Sets what the column holds; `AUTO` by default.
    public GridColumn setKind(Kind kind) {
        this.kind = kind == null ? Kind.AUTO : kind;
        return this;
    }

    /// The width asked for, in millimetres; 0 sizes the column to its
    /// title and the first rows.
    public float getWidthMM() {
        return widthMM;
    }

    /// Sets the width, in millimetres; 0, the default, sizes the column to
    /// what it shows.
    public GridColumn setWidthMM(float widthMM) {
        this.widthMM = Math.max(0, widthMM);
        width = -1;
        resized = false;
        return this;
    }

    /// The narrowest a resize may make the column, in millimetres.
    public float getMinWidthMM() {
        return minWidthMM;
    }

    /// Sets the narrowest a resize may make the column, in millimetres.
    public GridColumn setMinWidthMM(float minWidthMM) {
        this.minWidthMM = Math.max(1, minWidthMM);
        return this;
    }

    /// The widest the column sizes itself to what it shows, in millimetres.
    public float getMaxWidthMM() {
        return maxWidthMM;
    }

    /// Sets the widest the column sizes itself to what it shows, in
    /// millimetres. A resize by the user may go past it.
    public GridColumn setMaxWidthMM(float maxWidthMM) {
        this.maxWidthMM = Math.max(1, maxWidthMM);
        return this;
    }

    /// The share of the width left over when the columns are narrower than
    /// the grid; 0, the default, takes none.
    public int getFlex() {
        return flex;
    }

    /// Sets the share of the left over width this column takes.
    public GridColumn setFlex(int flex) {
        this.flex = Math.max(0, flex);
        return this;
    }

    /// The alignment of the values: `Component.LEFT`, `CENTER` or `RIGHT`,
    /// or -1 to align by kind.
    public int getAlignment() {
        return alignment;
    }

    /// Sets the alignment of the values; -1, the default, right aligns
    /// numbers, centres check boxes and left aligns the rest.
    public GridColumn setAlignment(int alignment) {
        this.alignment = alignment;
        return this;
    }

    /// The alignment a value is painted with, from `getAlignment()` or the
    /// value's kind.
    public int resolveAlignment(Object value) {
        if (alignment >= 0) {
            return alignment;
        }
        Kind k = resolveKind(value);
        if (k == Kind.NUMBER) {
            return Component.RIGHT;
        }
        if (k == Kind.BOOLEAN) {
            return Component.CENTER;
        }
        return Component.LEFT;
    }

    /// The kind of one value: the column's kind, or the value's own when the
    /// column's is `AUTO`.
    public Kind resolveKind(Object value) {
        if (kind != Kind.AUTO) {
            return kind;
        }
        if (value instanceof Boolean) {
            return Kind.BOOLEAN;
        }
        if (value instanceof Number) {
            return Kind.NUMBER;
        }
        if (value instanceof java.util.Date) {
            return Kind.DATE;
        }
        return Kind.TEXT;
    }

    /// Whether a tap on the header sorts by this column.
    public boolean isSortable() {
        return sortable;
    }

    /// Sets whether a tap on the header sorts by this column.
    public GridColumn setSortable(boolean sortable) {
        this.sortable = sortable;
        return this;
    }

    /// Whether the edge of the header can be dragged to resize the column.
    public boolean isResizable() {
        return resizable;
    }

    /// Sets whether the column can be resized by dragging its header edge.
    public GridColumn setResizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    /// Whether the header can be dragged to move the column.
    public boolean isMovable() {
        return movable;
    }

    /// Sets whether the header can be dragged to move the column.
    public GridColumn setMovable(boolean movable) {
        this.movable = movable;
        return this;
    }

    /// Whether the column's cells may be edited. A cell is edited only when
    /// the column and the model both allow it.
    public boolean isEditable() {
        return editable;
    }

    /// Sets whether the column's cells may be edited.
    public GridColumn setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    /// Whether the column is shown.
    public boolean isVisible() {
        return visible;
    }

    /// Shows or hides the column. A hidden column keeps its place.
    public GridColumn setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /// The renderer of the cells, or null for the grid's default.
    public GridCellRenderer getRenderer() {
        return renderer;
    }

    /// Sets the renderer of the cells; null uses the grid's default.
    public GridColumn setRenderer(GridCellRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    /// The editor of the cells, or null for the grid's default.
    public GridCellEditor getEditor() {
        return editor;
    }

    /// Sets the editor of the cells; null uses the grid's default.
    public GridColumn setEditor(GridCellEditor editor) {
        this.editor = editor;
        return this;
    }

    /// How values are turned into text, or null for the default.
    public GridFormatter getFormatter() {
        return formatter;
    }

    /// Sets how values are turned into text and read back from it.
    public GridColumn setFormatter(GridFormatter formatter) {
        this.formatter = formatter;
        width = -1;
        return this;
    }

    /// The text a value is shown as.
    public String format(Object value) {
        if (formatter != null) {
            return formatter.format(value);
        }
        if (value == null) {
            return "";
        }
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return Long.toString((long) d);
            }
        }
        return value.toString();
    }

    /// How two values of the column are ordered, or null for the default
    /// order: numbers by value, dates by time, booleans false first, and
    /// everything else by its text, ignoring case.
    public Comparator<Object> getComparator() {
        return comparator;
    }

    /// Sets how two values of the column are ordered. It is never given a
    /// null: nulls sort first.
    public GridColumn setComparator(Comparator<Object> comparator) {
        this.comparator = comparator;
        return this;
    }

    /// Compares two values with `getComparator()` or the default order.
    /// Null comes first.
    public int compare(Object a, Object b) {
        return GridSorter.compare(comparator, a, b);
    }

    /// What a group row shows in this column, or null for nothing.
    public GridAggregate getAggregate() {
        return aggregate;
    }

    /// Sets what a group row shows in this column, such as
    /// `GridAggregate.SUM`.
    public GridColumn setAggregate(GridAggregate aggregate) {
        this.aggregate = aggregate;
        return this;
    }

    /// The values a `CHOICE` column picks from.
    public Object[] getChoices() {
        if (choices == null) {
            return null;
        }
        Object[] out = new Object[choices.length];
        System.arraycopy(choices, 0, out, 0, choices.length);
        return out;
    }

    /// Sets the values a `CHOICE` column picks from, and makes it one.
    public GridColumn setChoices(Object... choices) {
        if (choices == null) {
            this.choices = null;
        } else {
            this.choices = new Object[choices.length];
            System.arraycopy(choices, 0, this.choices, 0, choices.length);
            kind = Kind.CHOICE;
        }
        return this;
    }

    // The width in pixels the grid laid the column out at, -1 until then.
    int width() {
        return width;
    }

    void width(int w) {
        width = w;
        resized = false;
    }

    // A width the user dragged the column to, which no re-measuring replaces.
    void resizedTo(int w) {
        width = w;
        resized = true;
    }

    boolean measured() {
        return widthMM <= 0 && !resized;
    }
}
