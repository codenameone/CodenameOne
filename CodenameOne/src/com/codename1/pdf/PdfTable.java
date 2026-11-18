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
package com.codename1.pdf;

import java.util.ArrayList;
import java.util.List;

/// A table for [PdfLayout]. Columns share the available width in
/// proportion to their weights; cells fill the rows left to right and wrap
/// their text. A row that does not fit continues on the next page, and the
/// header rows are repeated at the top of every page the table spans.
///
/// ```java
/// PdfDocument doc = new PdfDocument();
/// PdfLayout layout = new PdfLayout(doc, PdfPageSize.A4, 54);
/// PdfTable t = new PdfTable(3, 1, 1).setHeaderRows(1);
/// t.addCell(new PdfParagraph("Item", PdfFont.HELVETICA_BOLD, 10), 1, 0xeeeeee);
/// t.addCell(new PdfParagraph("Qty", PdfFont.HELVETICA_BOLD, 10), 1, 0xeeeeee);
/// t.addCell(new PdfParagraph("Price", PdfFont.HELVETICA_BOLD, 10), 1, 0xeeeeee);
/// t.addCell("Consulting").addCell("12").addCell("$1,200.00");
/// layout.add(t);
/// ```
@com.codename1.impl.SharedWithBackend
public final class PdfTable {
    final float[] weights;
    final List<Cell> cells = new ArrayList<Cell>();
    int headerRows;
    float padding = 4;
    float borderWidth = 0.5f;
    int borderColor = 0x999999;
    PdfFont font = PdfFont.HELVETICA;
    float fontSize = 10;
    float spaceAfter = 8;

    /// A table with one column per weight; each weight must be positive.
    public PdfTable(float... columnWeights) {
        if (columnWeights == null || columnWeights.length == 0) {
            throw new IllegalArgumentException("A table needs at least one column");
        }
        weights = new float[columnWeights.length];
        for (int i = 0; i < weights.length; i++) {
            // A zero, negative or NaN weight would place every column at NaN.
            if (!(columnWeights[i] > 0) || columnWeights[i] == Float.POSITIVE_INFINITY) { // NOPMD LogicInversion - also rejects NaN, which every comparison fails
                throw new IllegalArgumentException("Column weights must be positive and finite");
            }
            weights[i] = columnWeights[i];
        }
    }

    /// The number of leading rows that form the header; 0 by default.
    public PdfTable setHeaderRows(int rows) {
        this.headerRows = rows;
        return this;
    }

    /// The space between a cell's border and its text; 4 points by default.
    public PdfTable setCellPadding(float points) {
        this.padding = points;
        return this;
    }

    /// The width and color of the cell borders; a width of 0 draws none.
    public PdfTable setBorder(float width, int rgb) {
        this.borderWidth = width;
        this.borderColor = rgb;
        return this;
    }

    /// The font of cells added as plain strings.
    public PdfTable setFont(PdfFont font, float size) {
        this.font = font;
        this.fontSize = size;
        return this;
    }

    /// The space below the table; 8 points by default.
    public PdfTable setSpaceAfter(float points) {
        this.spaceAfter = points;
        return this;
    }

    /// Adds a cell of plain text in the table's font.
    public PdfTable addCell(String text) {
        return addCell(new PdfParagraph(text, font, fontSize), 1, -1);
    }

    /// Adds a cell holding `paragraph`.
    public PdfTable addCell(PdfParagraph paragraph) {
        return addCell(paragraph, 1, -1);
    }

    /// Adds a cell spanning `colspan` columns, filled with `backgroundRgb`,
    /// or transparent when it is negative.
    public PdfTable addCell(PdfParagraph paragraph, int colspan, int backgroundRgb) {
        cells.add(new Cell(paragraph, Math.max(1, Math.min(colspan, weights.length)), backgroundRgb));
        return this;
    }

    static final class Cell {
        final PdfParagraph paragraph;
        final int colspan;
        final int background;

        Cell(PdfParagraph paragraph, int colspan, int background) {
            this.paragraph = paragraph;
            this.colspan = colspan;
            this.background = background;
        }
    }

    /// The cells grouped into rows.
    List<List<Cell>> rows() {
        List<List<Cell>> rows = new ArrayList<List<Cell>>();
        List<Cell> row = new ArrayList<Cell>();
        int used = 0;
        for (int i = 0; i < cells.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Cell c = cells.get(i);
            if (used + c.colspan > weights.length) {
                rows.add(row);
                row = new ArrayList<Cell>();
                used = 0;
            }
            row.add(c);
            used += c.colspan;
            if (used == weights.length) {
                rows.add(row);
                row = new ArrayList<Cell>();
                used = 0;
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        return rows;
    }
}
