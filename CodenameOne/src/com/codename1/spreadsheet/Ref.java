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

/// A reference a formula evaluated to: a rectangle of cells on one sheet or through a span of
/// sheets. `next` chains the areas of a union.
final class Ref {
    final Sheet sheet;
    /// The last sheet of a reference through several sheets, null otherwise.
    final Sheet lastSheet;
    final int r1;
    final int c1;
    final int r2;
    final int c2;
    Ref next;

    Ref(Sheet sheet, Sheet lastSheet, int r1, int c1, int r2, int c2) {
        this.sheet = sheet;
        this.lastSheet = lastSheet == sheet ? null : lastSheet; //NOPMD CompareObjectsWithEquals - a sheet is an identity
        this.r1 = r1;
        this.c1 = c1;
        this.r2 = r2;
        this.c2 = c2;
    }

    int rows() {
        return r2 - r1 + 1;
    }

    int cols() {
        return c2 - c1 + 1;
    }

    boolean isSingleCell() {
        return r1 == r2 && c1 == c2 && next == null && lastSheet == null;
    }

    /// The same rectangle cut down to the rows and columns the sheet has cells in. `A:A`
    /// spans a million rows; walking the ones past the data would only visit empty cells.
    Ref clip() {
        int maxRow = sheet.usedRows() - 1;
        int maxCol = sheet.usedColumns() - 1;
        if (r2 <= maxRow && c2 <= maxCol) {
            return this;
        }
        Ref r = new Ref(sheet, lastSheet, r1, c1, Math.min(r2, Math.max(maxRow, r1)), Math.min(c2, Math.max(maxCol, c1)));
        r.next = next;
        return r;
    }
}
