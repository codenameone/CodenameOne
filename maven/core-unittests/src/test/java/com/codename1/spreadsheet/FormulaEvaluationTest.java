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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Formulas against the values Excel gives for them. The expectations were worked out from
/// the definitions of the functions, not copied from this implementation's output.
class FormulaEvaluationTest {
    private Workbook book;
    private Sheet sheet;

    @BeforeEach
    void data() {
        book = new Workbook();
        sheet = book.addSheet("Data");
        double[] a = {10, 20, 30, 40, 50};
        String[] b = {"apple", "banana", "cherry", "apple", "date"};
        for (int i = 0; i < 5; i++) {
            sheet.setCellValue(i, 0, Double.valueOf(a[i]));
            sheet.setCellValue(i, 1, b[i]);
        }
        sheet.setCellValue("C1", Double.valueOf(1.5));
        sheet.setCellValue("C2", Double.valueOf(2.5));
        sheet.setCellValue("C3", Boolean.TRUE);
        sheet.setCellValue("C5", "x");
        Sheet other = book.addSheet("Other Sheet");
        other.setCellValue("A1", Double.valueOf(7));
        other.setCellValue("A2", Double.valueOf(8));
    }

    private Object eval(String formula) {
        sheet.setCellFormula("Z100", formula);
        // a formula that makes a date gives its cell a date format, and the cell would then
        // answer with a Date: these tests compare the numbers of the days
        sheet.setCellFormat(99, 25, "General");
        return sheet.getCellValue("Z100");
    }

    private void num(double expected, String formula) {
        Object v = eval(formula);
        if (!(v instanceof Double)) {
            fail(formula + " gave " + v + " and not " + expected);
        }
        double d = ((Double) v).doubleValue();
        assertEquals(expected, d, Math.max(1e-9, Math.abs(expected) * 1e-9), formula);
    }

    private void val(Object expected, String formula) {
        assertEquals(expected, eval(formula), formula);
    }

    /// Where LibreOffice, which calculated the file fixtures, differs from Excel: it has no
    /// logical values, only the numbers 1 and 0. Excel's answers are the ones wanted.
    @Test
    void truthValuesAreNotNumbers() {
        num(4, "TYPE(TRUE)");
        val("aTRUE", "\"a\"&TRUE");
        num(2, "COUNT({1,\"a\",TRUE,2})");
        num(3, "COUNT(1,TRUE,2)");
        num(44, "DAYS360(DATE(2024,1,15),DATE(2024,2,29))");
        num(46, "DAYS360(DATE(2024,1,15),DATE(2024,3,1))");
        num(31, "DAYS360(DATE(2024,1,31),DATE(2024,3,1))");
        num(2.20462262184878, "CONVERT(1,\"kg\",\"lbm\")");
    }

    @Test
    void operators() {
        num(7, "1+2*3");
        num(9, "(1+2)*3");
        num(4, "-2^2");
        num(64, "2^3^2");
        num(0.5, "50%");
        num(2.5, "10/4");
        val("ab12", "\"ab\"&12");
        val(Boolean.TRUE, "1<2");
        val(Boolean.TRUE, "\"a\"=\"A\"");
        val(Boolean.TRUE, "\"a\"<>\"b\"");
        val(Boolean.FALSE, "2>=3");
        val(CellError.DIV0, "1/0");
        val(CellError.VALUE, "1+\"x\"");
        num(3, "1+\"2\"");
        num(2, "1+TRUE");
        val(Boolean.TRUE, "\"a\">5");
        val(Boolean.TRUE, "TRUE>\"zzz\"");
    }

    @Test
    void references() {
        num(10, "A1");
        num(150, "SUM(A1:A5)");
        num(150, "SUM(A:A)");
        num(15, "SUM('Other Sheet'!A1:A2)");
        num(30, "SUM(A1:A2 A1:A5)");
        num(80, "SUM((A1:A2,A5))");
        num(7, "'Other Sheet'!A1");
        num(0, "D9+0");
        val("", "D9&\"\"");
        val(CellError.REF, "Missing!A1");
    }

    @Test
    void math() {
        num(3.14, "ROUND(3.14159,2)");
        num(1200, "ROUND(1234,-2)");
        num(-3, "ROUND(-2.5,0)");
        num(3, "ROUNDUP(2.1,0)");
        num(2, "ROUNDDOWN(2.9,0)");
        num(-3, "INT(-2.5)");
        num(-2, "TRUNC(-2.5)");
        num(1, "MOD(10,3)");
        num(2, "MOD(-10,3)");
        num(-2, "MOD(10,-3)");
        val(CellError.DIV0, "MOD(1,0)");
        num(5, "ABS(-5)");
        num(4, "SQRT(16)");
        val(CellError.NUM, "SQRT(-1)");
        num(8, "POWER(2,3)");
        num(120, "FACT(5)");
        num(10, "COMBIN(5,2)");
        num(6, "GCD(12,18)");
        num(36, "LCM(12,18)");
        num(15, "CEILING(11,5)");
        num(10, "FLOOR(11,5)");
        num(12, "MROUND(11,3)");
        num(1, "SIGN(3)");
        num(Math.PI, "PI()");
        num(1, "SIN(PI()/2)");
        num(2, "LOG10(100)");
        num(3, "LOG(8,2)");
        num(1, "LN(EXP(1))");
        num(55, "SUMSQ(1,2,3,4,5)");
        num(400, "SUMPRODUCT(A1:A2,A3:A4)+SUMPRODUCT(A1:A1,A1:A1)*0-700");
        num(24, "PRODUCT(2,3,4)");
        num(2, "QUOTIENT(7,3)");
        num(3, "ODD(2)");
        num(4, "EVEN(3)");
        num(180, "DEGREES(PI())");
        val("XIV", "ROMAN(14)");
        num(14, "ARABIC(\"XIV\")");
        val("FF", "BASE(255,16)");
        num(255, "DECIMAL(\"FF\",16)");
    }

    @Test
    void statistics() {
        num(30, "AVERAGE(A1:A5)");
        num(5, "COUNT(A1:A5)");
        num(2, "COUNT(C1:C5)");
        num(4, "COUNTA(C1:C5)");
        num(1, "COUNTBLANK(C1:C5)");
        num(50, "MAX(A1:A5)");
        num(10, "MIN(A1:A5)");
        num(30, "MEDIAN(A1:A5)");
        num(Math.sqrt(250), "STDEV(A1:A5)");
        num(Math.sqrt(200), "STDEVP(A1:A5)");
        num(250, "VAR(A1:A5)");
        num(200, "VAR.P(A1:A5)");
        num(40, "LARGE(A1:A5,2)");
        num(20, "SMALL(A1:A5,2)");
        num(2, "RANK(40,A1:A5)");
        num(4, "RANK(40,A1:A5,1)");
        num(20, "QUARTILE(A1:A5,1)");
        num(46, "PERCENTILE(A1:A5,0.9)");
        num(3, "MODE(1,2,3,3,4)");
        num(2, "COUNTIF(B1:B5,\"apple\")");
        num(3, "COUNTIF(A1:A5,\">20\")");
        num(2, "COUNTIF(B1:B5,\"*an*\")+COUNTIF(B1:B5,\"d?te\")");
        num(50, "SUMIF(B1:B5,\"apple\",A1:A5)");
        num(120, "SUMIF(A1:A5,\">=30\")");
        num(40, "SUMIFS(A1:A5,B1:B5,\"apple\",A1:A5,\">10\")");
        num(25, "AVERAGEIF(B1:B5,\"apple\",A1:A5)");
        num(1, "COUNTIFS(B1:B5,\"apple\",A1:A5,\"<20\")");
        num(40, "MAXIFS(A1:A5,B1:B5,\"apple\")");
        num(10, "MINIFS(A1:A5,B1:B5,\"apple\")");
        num(1, "CORREL(A1:A5,A1:A5)");
        num(10, "SLOPE(A1:A5,{1;2;3;4;5})");
        num(0, "INTERCEPT(A1:A5,{1;2;3;4;5})");
        num(60, "FORECAST(6,A1:A5,{1;2;3;4;5})");
        num(150, "SUBTOTAL(9,A1:A5)");
        num(30, "SUBTOTAL(1,A1:A5)");
        num(Math.pow(12000000, 0.2), "GEOMEAN(A1:A5)");
        num(12, "AVEDEV(A1:A5)");
        num(1000, "DEVSQ(A1:A5)");
    }

    @Test
    void logic() {
        val("yes", "IF(A1>5,\"yes\",\"no\")");
        val(Boolean.FALSE, "IF(A1>50,1)");
        num(0, "IF(TRUE,)");
        val("b", "IFS(A1>50,\"a\",A1>5,\"b\")");
        val(CellError.NA, "IFS(FALSE,1)");
        num(-1, "IFERROR(1/0,-1)");
        num(-1, "IFNA(VLOOKUP(99,A1:B5,2,FALSE),-1)");
        val(CellError.DIV0, "IFNA(1/0,-1)");
        val(Boolean.TRUE, "AND(TRUE,1,A1>5)");
        val(Boolean.TRUE, "OR(FALSE,0,A1>5)");
        val(Boolean.TRUE, "XOR(TRUE,TRUE,TRUE)");
        val(Boolean.FALSE, "NOT(1)");
        val("two", "SWITCH(2,1,\"one\",2,\"two\",\"other\")");
        val("other", "SWITCH(5,1,\"one\",2,\"two\",\"other\")");
        val("b", "CHOOSE(2,\"a\",\"b\",\"c\")");
        num(1, "IF(TRUE,1,1/0)");
        num(30, "LET(x,10,y,20,x+y)");
        val(Boolean.TRUE, "ISNUMBER(A1)");
        val(Boolean.TRUE, "ISTEXT(B1)");
        val(Boolean.TRUE, "ISBLANK(C4)");
        val(Boolean.TRUE, "ISERROR(1/0)");
        val(Boolean.TRUE, "ISNA(NA())");
        val(Boolean.FALSE, "ISERR(NA())");
        val(Boolean.TRUE, "ISLOGICAL(C3)");
        val(Boolean.TRUE, "ISEVEN(4)");
        num(2, "ERROR.TYPE(1/0)");
        num(2, "TYPE(\"a\")");
        num(1, "N(TRUE)");
        val("", "T(5)");
    }

    @Test
    void text() {
        val("Hello", "LEFT(\"Hello World\",5)");
        val("World", "RIGHT(\"Hello World\",5)");
        val("lo W", "MID(\"Hello World\",4,4)");
        num(11, "LEN(\"Hello World\")");
        val("HELLO", "UPPER(\"hello\")");
        val("hello", "LOWER(\"HELLO\")");
        val("Hello World", "PROPER(\"hello WORLD\")");
        val("a b", "TRIM(\"  a   b  \")");
        val("a-b-c", "TEXTJOIN(\"-\",TRUE,\"a\",\"\",\"b\",\"c\")");
        val("ab", "CONCATENATE(\"a\",\"b\")");
        val("apple10", "CONCAT(B1,A1)");
        num(5, "FIND(\"o\",\"Hello World\")");
        val(CellError.VALUE, "FIND(\"z\",\"Hello\")");
        num(5, "SEARCH(\"O\",\"Hello World\")");
        num(2, "SEARCH(\"e?l\",\"Hello\")");
        val("Hexxo", "SUBSTITUTE(\"Hello\",\"l\",\"x\")");
        val("Helxo", "SUBSTITUTE(\"Hello\",\"l\",\"x\",2)");
        val("HeXXo", "REPLACE(\"Hello\",3,2,\"XX\")");
        val("ababab", "REPT(\"ab\",3)");
        num(12.5, "VALUE(\"12.5\")");
        num(0.5, "VALUE(\"50%\")");
        val("1,234.50", "TEXT(1234.5,\"#,##0.00\")");
        val("50%", "TEXT(0.5,\"0%\")");
        val("2024-03-05", "TEXT(DATE(2024,3,5),\"yyyy-mm-dd\")");
        val("Tue 5 Mar 24", "TEXT(DATE(2024,3,5),\"ddd d mmm yy\")");
        val("007", "TEXT(7,\"000\")");
        val("1.23E+04", "TEXT(12345,\"0.00E+00\")");
        val("1,234.57", "FIXED(1234.567,2)");
        val("$1,234.57", "DOLLAR(1234.567)");
        val(Boolean.TRUE, "EXACT(\"a\",\"a\")");
        val(Boolean.FALSE, "EXACT(\"a\",\"A\")");
        val("A", "CHAR(65)");
        num(65, "CODE(\"A\")");
        val("Hello", "TEXTBEFORE(\"Hello World\",\" \")");
        val("World", "TEXTAFTER(\"Hello World\",\" \")");
    }

    @Test
    void dates() {
        num(45356, "DATE(2024,3,5)");
        num(2024, "YEAR(45356)");
        num(3, "MONTH(45356)");
        num(5, "DAY(45356)");
        num(3, "WEEKDAY(45356)");
        num(2, "WEEKDAY(45356,2)");
        num(0.5, "TIME(12,0,0)");
        num(13, "HOUR(0.5625)");
        num(30, "MINUTE(0.5625)");
        num(45382, "EOMONTH(DATE(2024,3,5),0)");
        num(45327, "EDATE(DATE(2024,3,5),-1)");
        num(31, "DAYS(DATE(2024,2,1),DATE(2024,1,1))");
        num(1, "DATEDIF(DATE(2023,1,15),DATE(2024,3,5),\"y\")");
        num(13, "DATEDIF(DATE(2023,1,15),DATE(2024,3,5),\"m\")");
        num(45356, "DATEVALUE(\"2024-03-05\")");
        num(5, "NETWORKDAYS(DATE(2024,3,4),DATE(2024,3,10))");
        num(45359, "WORKDAY(DATE(2024,3,5),3)");
        num(10, "WEEKNUM(DATE(2024,3,5))");
        num(10, "ISOWEEKNUM(DATE(2024,3,5))");
        num(60, "DATE(1900,2,29)");
        num(60, "DATE(1900,3,0)");
        num(59, "DATE(1900,2,28)");
        num(1, "DATE(1900,1,1)");
        num(61, "DATE(1900,3,1)");
        num(29, "DAY(60)");
        num(0.5, "YEARFRAC(DATE(2024,1,1),DATE(2024,7,1))");
        num(180, "DAYS360(DATE(2024,1,1),DATE(2024,7,1))");
    }

    @Test
    void lookups() {
        val("cherry", "VLOOKUP(30,A1:B5,2,FALSE)");
        val("cherry", "VLOOKUP(35,A1:B5,2)");
        val(CellError.NA, "VLOOKUP(5,A1:B5,2)");
        val(CellError.NA, "VLOOKUP(35,A1:B5,2,FALSE)");
        num(20, "HLOOKUP(10,A1:B2,2,FALSE)");
        num(3, "MATCH(30,A1:A5,0)");
        num(3, "MATCH(35,A1:A5)");
        num(2, "MATCH(\"b*\",B1:B5,0)");
        val("banana", "INDEX(B1:B5,2)");
        val("banana", "INDEX(A1:B5,2,2)");
        num(150, "SUM(INDEX(A1:B5,0,1))");
        num(20, "XLOOKUP(\"banana\",B1:B5,A1:A5)");
        val("none", "XLOOKUP(\"zzz\",B1:B5,A1:A5,\"none\")");
        num(4, "XMATCH(\"apple\",B1:B5,0,-1)");
        num(20, "LOOKUP(25,A1:A5)");
        val("banana", "LOOKUP(25,A1:A5,B1:B5)");
        num(50, "SUM(OFFSET(A1,1,0,2,1))");
        num(30, "INDIRECT(\"A3\")");
        num(15, "SUM(INDIRECT(\"'Other Sheet'!A1:A2\"))");
        num(100, "ROW()");
        num(26, "COLUMN()");
        num(3, "ROW(A3)");
        num(5, "ROWS(A1:B5)");
        num(2, "COLUMNS(A1:B5)");
        val("$B$3", "ADDRESS(3,2)");
        val("B3", "ADDRESS(3,2,4)");
        num(2, "SHEETS()");
    }

    @Test
    void financial() {
        num(-1037.0320893591606, "PMT(0.08/12,10,10000)");
        num(-12762.815625, "FV(0.05,5,0,10000)");
        num(-7835.261664684588, "PV(0.05,5,0,10000)");
        num(1188.4434123352207, "NPV(0.1,-10000,3000,4200,6800)");
        num(0.2, "IRR({-100,120})");
        num(2000, "SLN(11000,1000,5)");
        num(0.1, "RATE(1,0,-100,110)");
        num(1, "NPER(0.1,0,-100,110)");
        num(0.21, "EFFECT(0.2,2)");
    }

    @Test
    void arrays() {
        num(6, "SUM({1,2,3})");
        num(4, "INDEX({1,2;3,4},2,2)");
        num(55, "SUM(SEQUENCE(10))");
        num(90, "SUM(FILTER(A1:A5,A1:A5>30))");
        num(10, "INDEX(SORT(A1:A5),1)");
        num(50, "INDEX(SORT(A1:A5,1,-1),1)");
        num(4, "ROWS(UNIQUE(B1:B5))");
        num(550, "SUMPRODUCT(A1:A5*{1;2;3;4;5})");
        num(32, "SUMPRODUCT({1,2,3},{4,5,6})");
        num(3, "SUM(--(B1:B5<>\"apple\"))");
        num(2, "COUNT(TRANSPOSE(C1:C2))");
        num(10, "MMULT({1,2},{2;4})");
        num(-2, "MDETERM({1,2;3,4})");
    }

    @Test
    void engineeringAndDistributions() {
        val("FF", "DEC2HEX(255)");
        num(255, "HEX2DEC(\"FF\")");
        val("1010", "DEC2BIN(10)");
        num(10, "BIN2DEC(\"1010\")");
        num(6, "BITAND(14,7)");
        num(1, "DELTA(3,3)");
        num(100, "CONVERT(1,\"m\",\"cm\")");
        num(32, "CONVERT(0,\"C\",\"F\")");
        num(0.5, "NORM.S.DIST(0,TRUE)");
        num(0.841344746068543, "NORMSDIST(1)");
        num(1.959963984540054, "NORM.S.INV(0.975)");
        num(0.3125, "BINOM.DIST(2,5,0.5,FALSE)");
        num(0.1804470443154836, "POISSON.DIST(3,2,FALSE)");
        num(24, "GAMMA(5)");
        num(0.8427007929497149, "ERF(1)");
        num(0.6321205588285577, "EXPON.DIST(1,1,TRUE)");
        num(1.6448536269514722, "NORMSINV(0.95)");
        num(0.05, "T.DIST.2T(2.2281388519649385,10)");
        num(0.95, "CHISQ.DIST(3.841458820694124,1,TRUE)");
    }

    @Test
    void namesAndErrors() {
        book.setDefinedName("Amounts", "Data!$A$1:$A$5");
        book.setDefinedName("Rate", "0.1");
        num(150, "SUM(Amounts)");
        num(15, "SUM(amounts)*Rate");
        val(CellError.NAME, "NoSuchFunction(1)");
        val(CellError.NAME, "undefined_name+1");
        val(CellError.NA, "SUM(A1:A5,NA())");
        assertTrue(Workbook.getSupportedFunctions().length > 350, "function count");
    }
}
