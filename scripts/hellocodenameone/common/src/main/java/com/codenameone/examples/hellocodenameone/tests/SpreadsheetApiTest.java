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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.spreadsheet.CellError;
import com.codename1.spreadsheet.Sheet;
import com.codename1.spreadsheet.Workbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// The spreadsheet package on the device: formulas, number formats and both file formats.
///
/// The unit tests hold the evaluator to several hundred values LibreOffice calculated, on a
/// desktop JVM. This holds a sample of them to the same values where the arithmetic, the
/// text conversions and the zip streams are the device's own.
public class SpreadsheetApiTest extends BaseTest {
    private static final String[] FORMULAS = {
        "-2^2", "2^3^2", "10/4", "MOD(-10,3)", "ROUND(2.5,0)", "ROUND(-2.5,0)", "ROUND(1234.567,-2)",
        "SQRT(2)", "EXP(1)", "LN(10)", "SIN(1)", "ATAN2(1,2)", "POWER(2,10)", "FACT(10)", "COMBIN(10,3)",
        "SUM(A1:A5)", "AVERAGE(A1:A5)", "STDEV(A1:A5)", "MEDIAN(A1:A5)", "SUMPRODUCT(A1:A5,B1:B5)",
        "COUNTIF(A1:A5,\">2\")", "SUMIF(C1:C5,\"b*\",A1:A5)", "LARGE(A1:A5,2)", "PERCENTILE(A1:A5,0.9)",
        "IF(A1>0,\"y\",\"n\")", "IFERROR(1/0,\"err\")", "AND(TRUE,A1)", "1/0", "NA()",
        "UPPER(C1)", "MID(C2,2,3)", "TEXT(1234.5678,\"#,##0.00\")", "TEXT(0.256,\"0.0%\")",
        "SUBSTITUTE(\"a-b-c\",\"-\",\"+\",2)", "VALUE(\"1,234\")", "FIXED(1234.567,1)", "\"x\"&1.5",
        "DAYS(DATE(2024,2,29),0)", "YEAR(45351)", "WEEKDAY(45351)", "EDATE(DATE(2024,1,31),1)", "DAYS360(45306,45351)",
        "TEXT(45351,\"yyyy-mm-dd\")", "TEXT(45351,\"ddd d mmm\")", "TEXT(0.5,\"hh:mm\")",
        "VLOOKUP(\"beta\",C1:D5,2,FALSE)", "INDEX(A1:B5,3,2)", "MATCH(4,A1:A5,0)",
        "PMT(0.05/12,360,200000)", "NPV(0.1,-1000,300,400,500)", "IRR({-1000,300,400,500})",
        "NORMSDIST(1)", "NORMSINV(0.975)", "DEC2HEX(255)", "HEX2DEC(\"FF\")",
        "SUM({1,2;3,4})", "MDETERM({1,2;3,4})",
    };

    private static final Object[] EXPECTED = {
        4.0, 64.0, 2.5, 2.0, 3.0, -3.0, 1200.0,
        1.4142135623730951, 2.718281828459045, 2.302585092994046, 0.8414709848078965, 1.1071487177940904,
        1024.0, 3628800.0, 120.0,
        15.0, 3.0, 1.5811388300841898, 3.0, 7.75,
        3.0, 6.0, 4.0, 4.6,
        "y", "err", Boolean.TRUE, CellError.DIV0, CellError.NA,
        "ALPHA", "eta", "1,234.57", "25.6%",
        "a-b+c", 1234.0, "1,234.6", "x1.5",
        45351.0, 2024.0, 5.0, 45351.0, 44.0,
        "2024-02-29", "Thu 29 Feb", "12:00",
        20.0, 0.75, 4.0,
        -1073.6432460242781, -19.124376750222154, 0.08896339469334993,
        0.8413447460685429, 1.959963984540054, "FF", 255.0,
        10.0, -2.0,
    };

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            Workbook book = build();
            check(book, "built");
            check(copy(book, Workbook.Format.XLSX), "xlsx");
            check(copy(book, Workbook.Format.ODS), "ods");
            edits(copy(book, Workbook.Format.XLSX));
        } catch (Throwable t) {
            fail("spreadsheet test failed: " + t);
            return false;
        }
        done();
        return true;
    }

    private static Workbook build() {
        Workbook book = new Workbook();
        Sheet data = book.addSheet("Data");
        String[] words = {"alpha", "beta", "gamma", "bravo", "delta"};
        double[] second = {0.5, 0.25, 0.75, 1, 0.1};
        for (int i = 0; i < 5; i++) {
            data.setCellValue(i, 0, Double.valueOf(i + 1));
            data.setCellValue(i, 1, Double.valueOf(second[i]));
            data.setCellValue(i, 2, words[i]);
            data.setCellValue(i, 3, Double.valueOf((i + 1) * 10));
        }
        for (int i = 0; i < FORMULAS.length; i++) {
            data.setCellFormula(i, 5, "=" + FORMULAS[i]);
        }
        return book;
    }

    private static Workbook copy(Workbook book, Workbook.Format format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        book.write(out, format);
        Workbook read = Workbook.read(new ByteArrayInputStream(out.toByteArray()), format);
        // what is compared must be calculated here, not carried through the file
        read.recalculate();
        return read;
    }

    private void check(Workbook book, String what) {
        Sheet data = book.getSheet("Data");
        assertNotNull(data, what + ": no Data sheet");
        for (int i = 0; i < FORMULAS.length; i++) {
            Object value = data.getCellValue(i, 5);
            Object expected = EXPECTED[i];
            boolean same;
            if (expected instanceof Double && value instanceof Double) {
                double e = ((Double) expected).doubleValue();
                double v = ((Double) value).doubleValue();
                same = Math.abs(e - v) <= 1e-9 * Math.max(1, Math.abs(e));
            } else {
                same = expected.equals(value);
            }
            assertBool(same, what + ": =" + FORMULAS[i] + " is " + value + ", expected " + expected);
        }
    }

    private void edits(Workbook book) {
        Sheet data = book.getSheet("Data");
        data.setValueAt(0, 0, "101");
        assertEqual(Double.valueOf(115), data.getCellValue(15, 5), "an edit did not reach SUM(A1:A5)");
        data.insertRows(0, 1);
        assertEqual("=SUM(A2:A6)", data.getCellFormula(16, 5), "inserting a row did not move the range");
        assertEqual(Double.valueOf(115), data.getCellValue(16, 5), "the sum changed when a row was inserted");
    }
}
