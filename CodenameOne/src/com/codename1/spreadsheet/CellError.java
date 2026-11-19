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

/// An error a cell holds instead of a value, such as `#DIV/0!`. A formula that reads a cell
/// in error answers that error, which is how errors travel through a sheet; nothing is thrown.
///
/// There is one instance per error, so two errors can be compared with `==`.
public final class CellError {
    /// Division by zero.
    public static final CellError DIV0 = new CellError("#DIV/0!", 2);
    /// A value that is not available, what a failed lookup answers.
    public static final CellError NA = new CellError("#N/A", 7);
    /// A name or a function that is not known.
    public static final CellError NAME = new CellError("#NAME?", 5);
    /// Two ranges that were expected to intersect and do not.
    public static final CellError NULL = new CellError("#NULL!", 1);
    /// A number that is out of the range a function accepts or can answer.
    public static final CellError NUM = new CellError("#NUM!", 6);
    /// A reference to cells that no longer exist.
    public static final CellError REF = new CellError("#REF!", 4);
    /// A value of the wrong type, such as text where a number is needed.
    public static final CellError VALUE = new CellError("#VALUE!", 3);
    /// A formula that depends on its own result. Spreadsheet files have no such value: XLSX
    /// writes it as `#REF!`, OpenDocument as LibreOffice's `Err:522`, and CSV as `#CIRC!`.
    public static final CellError CIRCULAR = new CellError("#CIRC!", 4);

    private static final CellError[] ALL = {DIV0, NA, NAME, NULL, NUM, REF, VALUE, CIRCULAR};

    private final String text;
    private final int type;

    private CellError(String text, int type) {
        this.text = text;
        this.type = type;
    }

    /// The error for the text a spreadsheet shows, ignoring case.
    ///
    /// #### Parameters
    ///
    /// - `text`: text such as `#N/A`
    ///
    /// #### Returns
    ///
    /// the error, or null when the text is not one
    public static CellError valueOf(String text) {
        if (text == null) {
            return null;
        }
        for (CellError known : ALL) {
            if (known.text.equalsIgnoreCase(text)) {
                return known;
            }
        }
        return null;
    }

    /// The number the `ERROR.TYPE` function answers for this error.
    ///
    /// #### Returns
    ///
    /// a number from 1 to 7
    public int getType() {
        return type;
    }

    /// The text a spreadsheet shows for this error.
    ///
    /// #### Returns
    ///
    /// text such as `#DIV/0!`
    @Override
    public String toString() {
        return text;
    }
}
