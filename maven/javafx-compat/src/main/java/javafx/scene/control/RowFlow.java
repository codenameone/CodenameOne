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
package javafx.scene.control;

import java.util.ArrayList;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/// The scrolling body of a list or a table: rows of one height, of which
/// only those in view have cells.
///
/// The cells are children of this region and its peer clips them, so a
/// row scrolled half out of view is cut at the edge. There is one group
/// of cells per visible row, one cell per column; a row keeps its cells
/// while it stays in view and a row that scrolls in takes over the cells
/// of the one that left. The rows below the last item are shown as empty
/// cells.
///
/// Scrolling is vertical: by the wheel, by dragging, and by
/// [#show(int)]. A thin thumb at the trailing edge shows the position.
final class RowFlow extends Region {

    /// What a view tells its flow.
    interface Rows {
        /// The number of rows.
        int rowCount();

        /// The number of columns; a list has one.
        int columnCount();

        /// Fills the x and width of every column for a body of a width.
        void columns(double width, double[] x, double[] w);

        /// Creates a cell of a column.
        IndexedCell<?> createCell(int column);

        /// Shows a row in a cell, or nothing with a row of -1.
        void updateCell(IndexedCell<?> cell, int column, int row, boolean force);

        /// The height the application fixed for every row, or 0.
        double fixedCellSize();
    }

    private static final double DEFAULT_ROW = 24;
    private static final double DRAG_SLOP = 6;
    private static final int MAX_VISIBLE = 400;

    private final Rows rows;
    private final ArrayList<IndexedCell<?>[]> slots = new ArrayList<IndexedCell<?>[]>();
    private final Region thumb = new Region();
    private double offset;
    private double measured;
    private boolean force;
    private double pressY;
    private double pressOffset;
    private boolean dragging;
    private int pendingRow = -1;

    RowFlow(Rows rows) {
        this.rows = rows;
        thumb.setMouseTransparent(true);
        thumb.setManaged(false);
        thumb.setVisible(false);
        thumb.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.35), new CornerRadii(2),
                Insets.EMPTY)));
        cn1Children().add(thumb);
        addEventHandler(ScrollEvent.SCROLL, new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                double before = offset;
                scrollTo(offset - event.getDeltaY());
                if (Double.compare(before, offset) != 0) {
                    event.consume();
                }
            }
        });
        addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pressY = event.getSceneY();
                pressOffset = offset;
                dragging = false;
            }
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                double moved = event.getSceneY() - pressY;
                if (!dragging && Math.abs(moved) > DRAG_SLOP) {
                    dragging = true;
                }
                if (dragging) {
                    scrollTo(pressOffset - moved);
                }
            }
        });
    }

    /// Whether every second row is shaded across the whole view, and the
    /// room below the last row as rows would be, which the standard theme
    /// does in a list and a table, not a tree.
    boolean stripes;

    @Override
    public void cn1Paint(com.codename1.fxcompat.runtime.Renderer renderer) {
        super.cn1Paint(renderer);
        double rh = rowHeight();
        // A view with no rows at all is left plain: JavaFX makes no cells
        // for it, so there is nothing for the theme's odd rows to match.
        if (!stripes || !(rh > 0) || rows.rowCount() <= 0) {
            return;
        }
        // From the first row in view: a row is shaded to the edge of the
        // view, past its last cell, and so is the room below the rows.
        int first = (int) Math.floor(offset / rh);
        double y = first * rh - offset;
        Color alt = com.codename1.fxcompat.runtime.CssEngine.themeColor(this, "-fx-control-inner-background-alt");
        if (alt == null) {
            return;
        }
        for (int row = first; y < getHeight(); row++, y += rh) {
            if (row % 2 == 1 && y + rh > 0) {
                double top = Math.max(0, y);
                renderer.fillRect(0, top, getWidth(), Math.min(y + rh, getHeight()) - top, alt);
            }
        }
    }

    /// Returns the height of a row.
    double rowHeight() {
        double fixed = rows.fixedCellSize();
        if (fixed > 0) {
            return fixed;
        }
        return measured > 0 ? measured : DEFAULT_ROW;
    }

    double offset() {
        return offset;
    }

    private double maxOffset() {
        return Math.max(0, rows.rowCount() * rowHeight() - getHeight());
    }

    /// Scrolls so that the body starts `value` below the first row.
    void scrollTo(double value) {
        double max = maxOffset();
        double v = value < 0 || Double.isNaN(value) ? 0 : (value > max ? max : value);
        if (Double.compare(v, offset) != 0) {
            offset = v;
            requestLayout();
        }
    }

    /// Scrolls so that a row is the first one shown, as far as the rows
    /// reach.
    void show(int row) {
        pendingRow = row < 0 ? 0 : row;
        requestLayout();
    }

    /// Returns the row at a y of this region, -1 below the last one.
    int rowAt(double y) {
        double h = rowHeight();
        if (y < 0 || h <= 0) {
            return -1;
        }
        int row = (int) Math.floor((y + offset) / h);
        return row >= 0 && row < rows.rowCount() ? row : -1;
    }

    /// Returns the row under an event of the scene, -1 for none.
    int rowAt(MouseEvent event) {
        Point2D p = sceneToLocal(event.getSceneX(), event.getSceneY());
        return rowAt(p.getY());
    }

    /// Returns the cell that shows a row in a column, or `null` when the
    /// row is out of view.
    IndexedCell<?> cell(int row, int column) {
        for (int i = 0; i < slots.size(); i++) {
            IndexedCell<?>[] slot = slots.get(i);
            if (column < slot.length && slot[column].getIndex() == row) {
                return slot[column];
            }
        }
        return null;
    }

    /// Returns the cells of every visible row, row by row.
    ArrayList<IndexedCell<?>[]> slots() {
        return slots;
    }

    /// The rows changed: every cell is updated on the next layout.
    void rowsChanged() {
        requestLayout();
    }

    /// Shows every row again even where the item is the same.
    void refresh() {
        force = true;
        requestLayout();
    }

    /// Drops every cell; the columns or the cell factory changed.
    void rebuild() {
        for (int i = 0; i < slots.size(); i++) {
            IndexedCell<?>[] slot = slots.get(i);
            for (int c = 0; c < slot.length; c++) {
                cn1Children().remove(slot[c]);
            }
        }
        slots.clear();
        measured = 0;
        requestLayout();
    }

    private IndexedCell<?>[] newSlot(int columns) {
        IndexedCell<?>[] slot = new IndexedCell<?>[columns];
        for (int c = 0; c < columns; c++) {
            slot[c] = rows.createCell(c);
            cn1Children().add(cn1Children().size() - 1, slot[c]);
        }
        return slot;
    }

    private void measure(int columns, int count) {
        if (rows.fixedCellSize() > 0 || measured > 0 || columns == 0) {
            return;
        }
        if (slots.isEmpty()) {
            slots.add(newSlot(columns));
        }
        IndexedCell<?>[] slot = slots.get(0);
        double h = 0;
        for (int c = 0; c < slot.length; c++) {
            rows.updateCell(slot[c], c, count > 0 ? 0 : -1, true);
            h = Math.max(h, slot[c].prefHeight(-1));
        }
        if (h > 0 && count > 0) {
            measured = h;
        }
    }

    @Override
    protected void layoutChildren() {
        int columns = rows.columnCount();
        int count = rows.rowCount();
        if (!slots.isEmpty() && slots.get(0).length != columns) {
            rebuild();
        }
        measure(columns, count);
        double w = getWidth();
        double h = getHeight();
        double rh = rowHeight();
        if (pendingRow >= 0) {
            // Asked for before the height of a row was known.
            offset = pendingRow * rh;
            pendingRow = -1;
        }
        double max = maxOffset();
        if (offset > max) {
            offset = max;
        }
        int visible = rh > 0 && columns > 0 ? (int) Math.ceil(h / rh) + 1 : 0;
        if (visible > MAX_VISIBLE) {
            visible = MAX_VISIBLE;
        }
        while (slots.size() > visible) {
            IndexedCell<?>[] gone = slots.remove(slots.size() - 1);
            for (int c = 0; c < gone.length; c++) {
                cn1Children().remove(gone[c]);
            }
        }
        while (slots.size() < visible) {
            slots.add(newSlot(columns));
        }
        double[] xs = new double[columns];
        double[] ws = new double[columns];
        rows.columns(w, xs, ws);
        int first = rh > 0 ? (int) Math.floor(offset / rh) : 0;
        for (int k = 0; k < visible; k++) {
            int row = first + k;
            // A row keeps its cells while it is in view.
            IndexedCell<?>[] slot = slots.get(row % visible);
            for (int c = 0; c < columns; c++) {
                IndexedCell<?> cell = slot[c];
                rows.updateCell(cell, c, row < count ? row : -1, force);
                cell.setVisible(ws[c] > 0);
                cell.resizeRelocate(xs[c], row * rh - offset, ws[c], rh);
            }
        }
        force = false;
        if (max > 0 && h > 0) {
            double total = count * rh;
            double th = Math.max(16, total > 0 ? h * h / total : h);
            thumb.setVisible(true);
            thumb.resizeRelocate(w - 5, offset / max * (h - th), 3, th);
        } else {
            thumb.setVisible(false);
        }
    }

    @Override
    protected Node cn1PickLocal(double x, double y) {
        // Cells reach beyond the body; what is clipped cannot be hit.
        return contains(x, y) ? super.cn1PickLocal(x, y) : null;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 0;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 0;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }
}
