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
package com.codename1.spreadsheet;

/// What a sheet stores for one cell.
final class Cell {
    static final byte CLEAN = 0;
    static final byte DIRTY = 1;
    /// On the recalculation stack, waiting for the cells it reads.
    static final byte VISITING = 2;

    /// A Double, String, Boolean or CellError, or null for an empty cell. For a formula this
    /// is its last result: the one the file stored, until the formula is calculated here.
    Object value;
    /// The parsed formula, null for a plain value and for a formula that could not be parsed.
    Node formula;
    /// The formula as text in the Excel dialect without the equals sign. Kept verbatim for a
    /// formula that could not be parsed, so that it is written back as it was read.
    String formulaText;
    /// Index into the workbook's number formats, 0 being General.
    int format;
    byte state;
    /// For a formula that could not be read: whether its text is OpenDocument syntax.
    boolean odsSyntax;
    /// The formula reads cells its text does not name (INDIRECT, OFFSET) or changes by itself
    /// (NOW, RAND), so it is calculated again after every edit.
    boolean alwaysDirty;
    /// Written in the file as an array formula: evaluated without implicit intersection.
    boolean arrayFormula;
    /// The last calculation took an array where one value is wanted; written as an array
    /// formula so Excel calculates it the same way.
    boolean spreads;
    /// Whether spreads has been worked out: a formula read from a file keeps its cached value
    /// and is not calculated until something it reads changes.
    boolean spreadsKnown;
    // the format came from a typed formula such as =TODAY(), and goes when the formula does
    boolean autoFormat;
    // calls a function this package does not have: the value its file stored stands until
    // something it reads changes
    boolean keepsCached;
    // found to read itself, directly or through others, while it was being calculated
    boolean circular;
    /// The references the formula names, found when first needed.
    Ref[] refs;

    // where a formula cell is; kept up to date when rows and columns move
    Sheet sheet;
    int row;
    int col;

    Cell(Sheet sheet, int row, int col) {
        this.sheet = sheet;
        this.row = row;
        this.col = col;
    }

    boolean isFormula() {
        return formulaText != null;
    }
}
