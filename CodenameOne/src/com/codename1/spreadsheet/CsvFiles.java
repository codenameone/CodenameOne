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

import com.codename1.io.CSVParser;
import com.codename1.io.CSVWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Reads separated text into a sheet and writes a sheet as separated text.
///
/// Text has no types, so reading guesses them: a cell that reads as a number, a date or a
/// logical value becomes one. The guess is made a column at a time, because the usual way
/// for it to go wrong is a column of codes -- postal codes, part numbers, phone numbers --
/// of which `00420` is one: where any cell of a column would lose digits by becoming a
/// number, the whole column stays text.
final class CsvFiles {
    private CsvFiles() {
    }

    static void read(Workbook book, InputStream in, char separator, boolean detect) throws IOException {
        CSVParser parser = new CSVParser(separator);
        parser.setDetectSeparator(detect);
        String[][] rows = parser.parse(in);
        Sheet sheet = book.addSheet("Sheet1");
        int columns = 0;
        for (String[] line : rows) {
            columns = Math.max(columns, line.length);
        }
        boolean[] text = new boolean[columns];
        for (String[] line : rows) {
            for (int c = 0; c < line.length; c++) {
                text[c] |= isCode(line[c]);
            }
        }
        book.silent = true;
        try {
            for (int r = 0; r < rows.length; r++) {
                String[] row = rows[r];
                for (int c = 0; c < row.length; c++) {
                    String v = row[c];
                    if (v == null || v.length() == 0) {
                        continue;
                    }
                    char first = v.charAt(0);
                    // a formula in a text file is text: calculating what a stranger's file
                    // says to calculate is not what opening it should do
                    if (text[c] || first == '=' || first == '\'') {
                        sheet.store(r, c, v);
                    } else {
                        sheet.setValueAt(r, c, v);
                    }
                }
            }
        } finally {
            book.silent = false;
        }
    }

    /// Whether text is digits that a number would not keep: a leading zero, or more digits
    /// than a number holds.
    private static boolean isCode(String v) {
        if (v == null) {
            return false;
        }
        int n = v.length();
        if (n < 2) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            char c = v.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return v.charAt(0) == '0' || n > 15;
    }

    static void write(Workbook book, OutputStream out, char separator) throws IOException {
        CSVWriter writer = new CSVWriter(separator);
        if (book.getSheetCount() == 0) {
            writer.write(out, new String[0][]);
            return;
        }
        Sheet sheet = book.getSheet(0);
        int rows = sheet.usedRows();
        int columns = sheet.usedColumns();
        String[][] data = new String[rows][columns];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                data[r][c] = sheet.getCellText(r, c);
            }
        }
        writer.write(out, data);
    }
}
