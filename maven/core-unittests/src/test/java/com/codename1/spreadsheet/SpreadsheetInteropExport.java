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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Writes workbooks with our writers for other programs to open:
/// `scripts/check-spreadsheet-interop.sh` runs this and then has LibreOffice and openpyxl
/// read what it wrote. That our own reader reads our own writer is tested elsewhere and
/// proves little; this is the half that says a file is one a spreadsheet accepts.
public final class SpreadsheetInteropExport {
    private SpreadsheetInteropExport() {
    }

    private static void write(Workbook book, File dir, String name, Workbook.Format format) throws IOException {
        OutputStream out = new FileOutputStream(new File(dir, name));
        try {
            book.write(out, format);
        } finally {
            out.close();
        }
    }

    /// A workbook with one of everything the writers keep.
    static Workbook sample() {
        Workbook book = new Workbook();
        Sheet s = book.addSheet("Orders & <Returns>");
        s.addRow("Item", "Count", "Price", "Shipped", "Total");
        s.addRow("a \"quoted\" <name> & more", Double.valueOf(3), Double.valueOf(2.5), Boolean.TRUE);
        s.addRow("  two  spaces\nand a line", Double.valueOf(-4), Double.valueOf(1234.5), Boolean.FALSE);
        s.addRow("00420", Double.valueOf(1e-7), Double.valueOf(0), Boolean.TRUE);
        s.setCellFormula("E2", "=B2*C2");
        s.setCellFormula("E3", "=B3*C3");
        s.setCellFormula("E4", "=1/0");
        s.setCellFormula("E5", "=SUM(E2:E3)+Tax");
        s.setCellValue("A6", DateUtil.toDate(45000.75, false));
        s.setCellFormat(5, 0, "yyyy-mm-dd hh:mm");
        s.setCellFormat(2, 2, "#,##0.00");
        s.setCellFormat(1, 1, "0.0%");
        s.setHeaderRow(true);
        s.setFrozenPanes(1, 2);
        s.setColumnWidth(0, 30);
        s.setRowOutlineLevel(2, 1);
        s.setRowOutlineLevel(3, 2);
        s.addMergedRegion(6, 0, 7, 2);
        book.setDefinedName("Tax", "0.5");
        Sheet other = book.addSheet("Other");
        other.setCellFormula("A1", "='Orders & <Returns>'!E5*2");
        return book;
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        InputStream in = SpreadsheetInteropExport.class.getResourceAsStream("/spreadsheet-fixtures/calc.xlsx");
        Workbook calc;
        try {
            calc = Workbook.read(in, Workbook.Format.XLSX);
        } finally {
            in.close();
        }
        // every value written is one this evaluator calculated, not one read from the file
        calc.recalculate();
        write(calc, dir, "calc.xlsx", Workbook.Format.XLSX);
        write(calc, dir, "calc.ods", Workbook.Format.ODS);
        Workbook sample = sample();
        write(sample, dir, "sample.xlsx", Workbook.Format.XLSX);
        write(sample, dir, "sample.ods", Workbook.Format.ODS);
    }
}
