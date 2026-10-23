/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.TableLayout;

/// A table of the console: a header row and rows of cells, the widths shared
/// out by weight, numbers aligned to the end of their column.
///
/// "End" follows the language: in Hebrew the layout mirrors, so the first
/// column is on the right and the amounts line up on the left edge of
/// theirs, which is how a Hebrew ledger reads.
///
/// On a narrow screen a table wider than the window scrolls sideways inside
/// itself rather than squeezing seven columns into a phone's width.
public final class Table {
    /// A table is never drawn narrower than this, in millimetres per weight.
    private static final float MM_PER_WEIGHT = 9f;

    private final String[] headers;
    private final int[] weights;
    private final boolean[] numeric;
    private final Container rows = new Container(BoxLayout.y());
    private final Container outer;

    /// @param headers the column names, translated here
    /// @param weights each column's share of the width
    /// @param numeric which columns hold numbers (aligned to the end)
    public Table(String[] headers, int[] weights, boolean[] numeric) {
        this(headers, weights, numeric, false);
    }

    /// @param fit true for a table whose columns were chosen to fit the
    ///     window: it never scrolls sideways
    public Table(String[] headers, int[] weights, boolean[] numeric, boolean fit) {
        this.headers = headers;
        this.weights = weights;
        this.numeric = numeric;
        int total = 0;
        for (int iter = 0; iter < weights.length; iter++) {
            total += weights[iter];
        }
        Container head = line("BnTableHead");
        for (int iter = 0; iter < headers.length; iter++) {
            head.add(constraint(head, iter), cell(Lang.tr(headers[iter]),
                    numeric[iter] ? "BnCellHeadNum" : "BnCellHead"));
        }
        final int least = CN.convertToPixels(MM_PER_WEIGHT * total);
        final boolean scrolls = !fit && least > CN.getDisplayWidth() - CN.convertToPixels(8f);
        // The width is set in calcPreferredSize, never with setPreferredW,
        // which would freeze the height at the header's while rows arrive.
        Container body = new Container(BoxLayout.y(), "BnTable") {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension d = super.calcPreferredSize();
                if (scrolls) {
                    d.setWidth(Math.max(d.getWidth(), least));
                }
                return d;
            }
        };
        body.add(head);
        body.add(rows);
        // A BorderLayout centre is held to the window's width, so a table
        // that scrolls sits in a row that lets it be as wide as it asks.
        outer = new Container(scrolls ? BoxLayout.x() : new BorderLayout());
        outer.setScrollableX(scrolls);
        if (scrolls) {
            outer.add(body);
        } else {
            outer.add(BorderLayout.CENTER, body);
        }
    }

    /// The table, for the page to place.
    public Container component() {
        return outer;
    }

    /// Adds a row; `tap` (may be null) runs when it is tapped.
    public Container add(String[] cells, Runnable tap) {
        return add(cells, "BnTableRow", null, tap);
    }

    /// Adds a row in `uiid` (a highlighted row is `BnTableRowBad`); `cellUiids`
    /// may name a style per cell, or be null.
    public Container add(String[] cells, String uiid, String[] cellUiids, final Runnable tap) {
        Container row = line(uiid);
        for (int iter = 0; iter < cells.length; iter++) {
            String style = cellUiids != null && cellUiids[iter] != null ? cellUiids[iter]
                    : numeric[iter] ? "BnCellNum" : "BnCell";
            row.add(constraint(row, iter), cell(cells[iter] == null ? "" : cells[iter], style));
        }
        if (tap == null) {
            rows.add(row);
            return row;
        }
        Button lead = new Button();
        lead.setUIID("BnRowLead");
        lead.addActionListener(evt -> tap.run());
        Container whole = new Container(new BorderLayout());
        whole.add(BorderLayout.CENTER, row);
        whole.add(BorderLayout.WEST, lead);
        whole.setLeadComponent(lead);
        rows.add(whole);
        return row;
    }

    /// A row of quiet text across the whole table.
    public void note(String text) {
        Container row = new Container(new BorderLayout(), "BnTableRow");
        row.add(BorderLayout.CENTER, new Label(text, "BnCellMuted"));
        rows.add(row);
    }

    public int size() {
        return rows.getComponentCount();
    }

    private Container line(String uiid) {
        TableLayout layout = new TableLayout(1, headers.length);
        layout.setGrowHorizontally(true);
        return new Container(layout, uiid);
    }

    private Object constraint(Container row, int column) {
        if (!(row.getLayout() instanceof TableLayout)) {
            return null;
        }
        TableLayout.Constraint c = ((TableLayout) row.getLayout()).createConstraint();
        int total = 0;
        for (int iter = 0; iter < weights.length; iter++) {
            total += weights[iter];
        }
        // The percentages are whole numbers; the last column takes the rest.
        int used = 0;
        for (int iter = 0; iter < column; iter++) {
            used += weights[iter] * 100 / total;
        }
        c.setWidthPercentage(column == weights.length - 1 ? 100 - used
                : weights[column] * 100 / total);
        return c;
    }

    private static Component cell(String text, String uiid) {
        Label label = new Label(text, uiid);
        label.setEndsWith3Points(true);
        return label;
    }
}
