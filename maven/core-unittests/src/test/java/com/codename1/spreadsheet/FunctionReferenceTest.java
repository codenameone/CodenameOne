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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The functions with a published worked example: the finance, distribution, engineering
/// and calendar functions whose answers nobody works out in their head. Every expected
/// value is the one in the function's documentation, to the digits it is printed with,
/// so the tolerance beside each is the rounding of that print and nothing looser.
///
/// A test collects every formula that disagrees before it fails, so one run names them
/// all.
class FunctionReferenceTest {
    private Sheet sheet;
    private StringBuilder wrong;

    @BeforeEach
    void book() {
        Workbook book = new Workbook();
        sheet = book.addSheet("Data");
        wrong = new StringBuilder();
    }

    @AfterEach
    void report() {
        assertEquals("", wrong.toString());
    }

    private Object eval(String formula) {
        sheet.setCellFormula("Z100", formula);
        // a formula that makes a date gives its cell a date format, and the cell would then
        // answer with a Date: these tests compare the numbers of the days
        sheet.setCellFormat(99, 25, "General");
        return sheet.getCellValue("Z100");
    }

    private void num(double expected, double tolerance, String formula) {
        Object v = eval(formula);
        if (!(v instanceof Double) || Math.abs(((Double) v).doubleValue() - expected) > tolerance) {
            wrong.append('\n').append(formula).append(" gave ").append(v).append(", not ").append(expected);
        }
    }

    private void val(Object expected, String formula) {
        Object v = eval(formula);
        if (!expected.equals(v)) {
            wrong.append('\n').append(formula).append(" gave ").append(v).append(", not ").append(expected);
        }
    }

    @Test
    void loansAndDepreciation() {
        num(480, 0.005, "VDB(2400,300,10,0,1)");
        num(396.31, 0.005, "VDB(2400,300,120,6,18)");
        num(311.81, 0.005, "VDB(2400,300,120,6,18,1.5)");
        num(315, 0.005, "VDB(2400,300,10,0,0.875,1.5)");
        num(480, 0.005, "VDB(2400,300,10,0,1,2,TRUE)");
        num(0.0077014725, 1e-9, "RATE(48,-200,8000)");
        num(-100, 1e-9, "PMT(0,10,1000)");
        num(1000, 1e-9, "FV(0,10,-100)");
        num(1000, 1e-9, "PV(0,10,-100)");
        num(10, 1e-9, "NPER(0,-100,1000)");
        num(-1030.16, 0.005, "PMT(0.08/12,10,10000,0,1)");
        num(-1037.03, 0.005, "PMT(0.08/12,10,10000)");
        num(2581.40, 0.005, "FV(0.06/12,10,-200,-500,1)");
        num(-59777.15, 0.005, "PV(0.08/12,12*20,500,,0)");
        num(59.6738657, 1e-6, "NPER(0.12/12,-100,-1000,10000,1)");
        num(60.0821229, 1e-6, "NPER(0.12/12,-100,-1000,10000)");
        num(-66.67, 0.005, "IPMT(0.1/12,1,3*12,8000)");
        num(-75.62, 0.005, "PPMT(0.1/12,1,2*12,2000)");
        num(186083.33, 0.005, "DB(1000000,100000,6,1,7)");
        num(15845.10, 0.005, "DB(1000000,100000,6,7,7)");
        num(1.32, 0.005, "DDB(2400,300,3650,1)");
        num(40, 0.005, "DDB(2400,300,120,1,2)");
        num(480, 0.005, "DDB(2400,300,10,1,2)");
        num(306, 0.005, "DDB(2400,300,10,2,1.5)");
        num(22.12, 0.005, "DDB(2400,300,10,10)");
        num(2250, 1e-9, "SLN(30000,7500,10)");
        num(4090.91, 0.005, "SYD(30000,7500,10,1)");
        num(409.09, 0.005, "SYD(30000,7500,10,10)");
        num(0.0535427, 1e-7, "EFFECT(0.0525,4)");
        num(0.05250032, 1e-8, "NOMINAL(0.053543,4)");
        num(-11135.23213, 1e-4, "CUMIPMT(0.09/12,30*12,125000,13,24,0)");
        num(-937.5, 1e-9, "CUMIPMT(0.09/12,30*12,125000,1,1,0)");
        num(-934.1071234, 1e-6, "CUMPRINC(0.09/12,30*12,125000,13,24,0)");
        num(-68.27827118, 1e-6, "CUMPRINC(0.09/12,30*12,125000,1,1,0)");
        num(0.126094, 1e-6, "MIRR({-120000,39000,30000,21000,37000,46000},0.1,0.12)");
        num(1.33089, 1e-9, "FVSCHEDULE(1,{0.09,0.11,0.1})");
        num(3.86, 0.005, "PDURATION(0.025,2000,2200)");
        num(0.0009933, 1e-7, "RRI(96,10000,11000)");
        num(1.125, 1e-9, "DOLLARDE(1.02,16)");
        num(1.02, 1e-9, "DOLLARFR(1.125,16)");
        num(1188.44, 0.005, "NPV(0.1,-10000,3000,4200,6800)");
        num(-0.021244848, 1e-8, "IRR({-70000,12000,15000,18000,21000})");
        num(0.086630948, 1e-8, "IRR({-70000,12000,15000,18000,21000,26000})");
        num(-0.443506941, 1e-7, "IRR({-70000,12000,15000},-0.1)");
        sheet.setCellValue("A1", Double.valueOf(-10000));
        sheet.setCellValue("A2", Double.valueOf(2750));
        sheet.setCellValue("A3", Double.valueOf(4250));
        sheet.setCellValue("A4", Double.valueOf(3250));
        sheet.setCellValue("A5", Double.valueOf(2750));
        sheet.setCellFormula("B1", "=DATE(2008,1,1)");
        sheet.setCellFormula("B2", "=DATE(2008,3,1)");
        sheet.setCellFormula("B3", "=DATE(2008,10,30)");
        sheet.setCellFormula("B4", "=DATE(2009,2,15)");
        sheet.setCellFormula("B5", "=DATE(2009,4,1)");
        num(2086.65, 0.005, "XNPV(0.09,A1:A5,B1:B5)");
        num(0.373362535, 1e-8, "XIRR(A1:A5,B1:B5,0.1)");
        // no payment can pay a loan off at a rate of minus one, and no rate repays nothing
        val(CellError.NUM, "RATE(10,0,1000)");
        val(CellError.NUM, "IRR({1,2,3})");
        val(CellError.NUM, "NPER(0,0,1000)");
        val(CellError.NUM, "DB(1000,100,0,1)");
        val(CellError.NUM, "EFFECT(0.05,0)");
    }

    @Test
    void distributions() {
        num(1.3333347, 1e-7, "NORM.S.INV(0.908789)");
        num(42.000002, 1e-6, "NORM.INV(0.908789,40,1.5)");
        num(0.9087888, 1e-7, "NORM.DIST(42,40,1.5,TRUE)");
        num(0.10934, 1e-5, "NORM.DIST(42,40,1.5,FALSE)");
        num(0.90878878, 1e-7, "NORM.S.DIST(1.333333,TRUE)");
        num(0.99469533, 1e-8, "T.DIST(60,1,TRUE)");
        num(0.00073691, 1e-8, "T.DIST(8,3,FALSE)");
        num(0.054644930, 1e-8, "T.DIST.2T(1.959999998,60)");
        num(0.027322465, 1e-8, "T.DIST.RT(1.959999998,60)");
        num(0.8164966, 1e-7, "T.INV(0.75,2)");
        num(0.606533, 1e-6, "T.INV.2T(0.546449,60)");
        num(0.52049988, 1e-8, "CHISQ.DIST(0.5,1,TRUE)");
        num(0.20755375, 1e-8, "CHISQ.DIST(2,3,FALSE)");
        num(3.283020286, 1e-8, "CHISQ.INV(0.93,1)");
        num(0.0500006, 1e-7, "CHISQ.DIST.RT(18.307,10)");
        num(18.306973, 1e-6, "CHISQ.INV.RT(0.050001,10)");
        num(0.99, 1e-6, "F.DIST(15.2068649,6,4,TRUE)");
        num(0.0012238, 1e-7, "F.DIST(15.2068649,6,4,FALSE)");
        num(0.01, 1e-8, "F.DIST.RT(15.2068649,6,4)");
        num(0.10930991, 1e-8, "F.INV(0.01,6,4)");
        num(15.20686, 1e-5, "F.INV.RT(0.01,6,4)");
        num(0.6854706, 1e-7, "BETA.DIST(2,8,10,TRUE,1,3)");
        num(1.4837646, 1e-7, "BETA.DIST(2,8,10,FALSE,1,3)");
        num(2, 1e-7, "BETA.INV(0.685470581,8,10,1,3)");
        num(0.068094, 1e-6, "GAMMA.DIST(10.00001131,9,2,TRUE)");
        num(0.032639, 1e-6, "GAMMA.DIST(10.00001131,9,2,FALSE)");
        num(10.0000112, 1e-6, "GAMMA.INV(0.068094,9,2)");
        num(1.7917595, 1e-7, "GAMMALN(4)");
        num(1.329340, 1e-6, "GAMMA(2.5)");
        num(0.268, 0.0005, "GAMMA(-3.75)");
        num(0.2050781, 1e-7, "BINOM.DIST(6,10,0.5,FALSE)");
        num(0.828125, 1e-9, "BINOM.DIST(6,10,0.5,TRUE)");
        num(4, 1e-9, "BINOM.INV(6,0.5,0.75)");
        num(0.084, 0.0005, "BINOM.DIST.RANGE(60,0.75,48)");
        num(0.524, 0.0005, "BINOM.DIST.RANGE(60,0.75,45,50)");
        num(0.3135141, 1e-7, "NEGBINOM.DIST(10,5,0.25,TRUE)");
        num(0.0550487, 1e-7, "NEGBINOM.DIST(10,5,0.25,FALSE)");
        num(0.4654, 0.00005, "HYPGEOM.DIST(1,4,8,20,TRUE)");
        num(0.3633, 0.00005, "HYPGEOM.DIST(1,4,8,20,FALSE)");
        num(0.124652, 1e-6, "POISSON.DIST(2,5,TRUE)");
        num(0.084224, 1e-6, "POISSON.DIST(2,5,FALSE)");
        num(0.86466472, 1e-8, "EXPON.DIST(0.2,10,TRUE)");
        num(1.35335283, 1e-8, "EXPON.DIST(0.2,10,FALSE)");
        num(0.929581, 1e-6, "WEIBULL.DIST(105,20,100,TRUE)");
        num(0.035589, 1e-6, "WEIBULL.DIST(105,20,100,FALSE)");
        num(0.0390836, 1e-7, "LOGNORM.DIST(4,3.5,1.2,TRUE)");
        num(0.0176176, 1e-7, "LOGNORM.DIST(4,3.5,1.2,FALSE)");
        num(4.0000252, 1e-6, "LOGNORM.INV(0.039084,3.5,1.2)");
        num(0.692952, 1e-6, "CONFIDENCE.NORM(0.05,2.5,50)");
        num(0.284196855, 1e-8, "CONFIDENCE.T(0.05,1,50)");
        num(1.33333333, 1e-8, "STANDARDIZE(42,40,1.5)");
        num(0.9729551, 1e-7, "FISHER(0.75)");
        num(0.75, 1e-6, "FISHERINV(0.972955)");
        num(0.301137432, 1e-9, "PHI(0.75)");
        num(0.47725, 1e-5, "GAUSS(2)");
        num(0.70792892, 1e-8, "ERF(0.745)");
        num(0.15729921, 1e-8, "ERFC(1)");
        // outside what each one is defined on
        val(CellError.NUM, "NORM.S.INV(0)");
        val(CellError.NUM, "NORM.S.INV(1)");
        val(CellError.NUM, "NORM.DIST(1,0,0,TRUE)");
        val(CellError.NUM, "T.INV(1.5,2)");
        val(CellError.NUM, "CHISQ.DIST(-1,1,TRUE)");
        val(CellError.NUM, "BINOM.DIST(11,10,0.5,FALSE)");
        val(CellError.NUM, "POISSON.DIST(-1,5,TRUE)");
        val(CellError.NUM, "GAMMA(0)");
        val(CellError.NUM, "GAMMALN(0)");
        val(CellError.NUM, "HYPGEOM.DIST(5,4,8,20,TRUE)");
    }

    @Test
    void numberBasesBitsAndUnits() {
        num(100, 0, "BIN2DEC(\"1100100\")");
        num(-1, 0, "BIN2DEC(\"1111111111\")");
        val("1001", "DEC2BIN(9,4)");
        val("1110011100", "DEC2BIN(-100)");
        val("FFFFFFFFCA", "DEC2HEX(-54)");
        val("0064", "DEC2HEX(100,4)");
        num(-165, 0, "HEX2DEC(\"FFFFFFFF5B\")");
        num(165, 0, "HEX2DEC(\"A5\")");
        num(-165, 0, "OCT2DEC(\"7777777533\")");
        num(44, 0, "OCT2DEC(54)");
        val("00001111", "HEX2BIN(\"F\",8)");
        val("1111111111", "HEX2BIN(\"FFFFFFFFFF\")");
        val("00FB", "BIN2HEX(\"11111011\",4)");
        val("FFFFFFFFFF", "BIN2HEX(\"1111111111\")");
        val("011", "BIN2OCT(\"1001\",3)");
        val("7777777777", "BIN2OCT(\"1111111111\")");
        val("072", "DEC2OCT(58,3)");
        val("7777777634", "DEC2OCT(-100)");
        val("35516", "HEX2OCT(\"3B4E\")");
        val("1000000000", "OCT2BIN(\"7777777000\")");
        val("0040", "OCT2HEX(\"100\",4)");
        val("FFFFFFFF5B", "OCT2HEX(\"7777777533\")");
        // more digits than asked for, a digit of another base, a number out of range
        val(CellError.NUM, "DEC2BIN(9,2)");
        val(CellError.NUM, "DEC2BIN(512)");
        val(CellError.NUM, "BIN2DEC(\"102\")");
        val(CellError.NUM, "HEX2DEC(\"G\")");
        val(CellError.NUM, "BIN2DEC(\"11111111111\")");
        num(9, 0, "BITAND(13,25)");
        num(23, 0, "BITOR(23,5)");
        num(6, 0, "BITXOR(5,3)");
        num(16, 0, "BITLSHIFT(4,2)");
        num(3, 0, "BITRSHIFT(13,2)");
        num(1, 0, "BITLSHIFT(4,-2)");
        val(CellError.NUM, "BITAND(-1,1)");
        val(CellError.NUM, "BITAND(1.5,1)");
        num(0, 0, "DELTA(5,4)");
        num(1, 0, "DELTA(5,5)");
        num(1, 0, "DELTA(0)");
        num(1, 0, "GESTEP(5,4)");
        num(0, 0, "GESTEP(-1)");
        num(0.4535924, 1e-7, "CONVERT(1,\"lbm\",\"kg\")");
        num(20, 1e-9, "CONVERT(68,\"F\",\"C\")");
        num(273.15, 1e-9, "CONVERT(0,\"C\",\"K\")");
        num(1000, 1e-9, "CONVERT(1,\"km\",\"m\")");
        num(1.609344, 1e-9, "CONVERT(1,\"mi\",\"km\")");
        num(3600, 1e-9, "CONVERT(1,\"hr\",\"sec\")");
        num(1024, 1e-9, "CONVERT(1,\"kibyte\",\"byte\")");
        val(CellError.NA, "CONVERT(2.5,\"ft\",\"sec\")");
        val(CellError.NA, "CONVERT(1,\"nothing\",\"m\")");
    }

    @Test
    void calendars() {
        num(1, 0, "DAYS360(DATE(2011,1,30),DATE(2011,2,1))");
        num(360, 0, "DAYS360(DATE(2011,1,1),DATE(2011,12,31))");
        num(359, 0, "DAYS360(DATE(2011,1,1),DATE(2011,12,31),TRUE)");
        num(30, 0, "DAYS360(DATE(2011,2,28),DATE(2011,3,31))");
        num(32, 0, "DAYS360(DATE(2011,2,28),DATE(2011,3,31),TRUE)");
        num(-32, 0, "DAYS360(DATE(2011,3,31),DATE(2011,2,28))");
        num(0.58055556, 1e-8, "YEARFRAC(DATE(2012,1,1),DATE(2012,7,30))");
        num(0.57650273, 1e-8, "YEARFRAC(DATE(2012,1,1),DATE(2012,7,30),1)");
        num(211d / 360, 1e-12, "YEARFRAC(DATE(2012,1,1),DATE(2012,7,30),2)");
        num(0.57808219, 1e-8, "YEARFRAC(DATE(2012,1,1),DATE(2012,7,30),3)");
        num(0.58055556, 1e-8, "YEARFRAC(DATE(2012,1,1),DATE(2012,7,30),4)");
        val(CellError.NUM, "YEARFRAC(DATE(2012,1,1),DATE(2012,7,30),5)");
        num(10, 0, "WEEKNUM(DATE(2012,3,9))");
        num(11, 0, "WEEKNUM(DATE(2012,3,9),2)");
        num(10, 0, "WEEKNUM(DATE(2012,3,9),21)");
        val(CellError.NUM, "WEEKNUM(DATE(2012,3,9),5)");
        num(10, 0, "ISOWEEKNUM(DATE(2012,3,9))");
        num(53, 0, "ISOWEEKNUM(DATE(2021,1,1))");
        num(1, 0, "ISOWEEKNUM(DATE(2019,12,30))");
        num(5, 0, "WEEKDAY(DATE(2008,2,14))");
        num(4, 0, "WEEKDAY(DATE(2008,2,14),2)");
        num(3, 0, "WEEKDAY(DATE(2008,2,14),3)");
        num(4, 0, "WEEKDAY(DATE(2008,2,14),11)");
        num(5, 0, "WEEKDAY(DATE(2008,2,14),17)");
        val(CellError.NUM, "WEEKDAY(DATE(2008,2,14),4)");
        num(110, 0, "NETWORKDAYS(DATE(2012,10,1),DATE(2013,3,1))");
        num(109, 0, "NETWORKDAYS(DATE(2012,10,1),DATE(2013,3,1),DATE(2012,11,22))");
        num(-110, 0, "NETWORKDAYS(DATE(2013,3,1),DATE(2012,10,1))");
        num(22, 0, "NETWORKDAYS.INTL(DATE(2006,1,1),DATE(2006,1,31))");
        num(-21, 0, "NETWORKDAYS.INTL(DATE(2006,2,28),DATE(2006,1,31))");
        num(22, 0, "NETWORKDAYS.INTL(DATE(2006,1,1),DATE(2006,2,1),7,{\"2006-1-2\",\"2006-1-16\"})");
        num(20, 0, "NETWORKDAYS.INTL(DATE(2006,1,1),DATE(2006,2,1),\"0010001\",{\"2006-1-2\",\"2006-1-16\"})");
        val(CellError.VALUE, "NETWORKDAYS.INTL(DATE(2006,1,1),DATE(2006,2,1),\"1111111\")");
        val(CellError.NUM, "NETWORKDAYS.INTL(DATE(2006,1,1),DATE(2006,2,1),8)");
        num(39933, 0, "WORKDAY(DATE(2008,10,1),151)");
        num(39938, 0, "WORKDAY(DATE(2008,10,1),151,{39778,39786,39834})");
        num(39720, 0, "WORKDAY(DATE(2008,10,1),-2)");
        val(CellError.NUM, "WORKDAY.INTL(DATE(2012,1,1),30,0)");
        num(41013, 0, "WORKDAY.INTL(DATE(2012,1,1),90,11)");
        num(40944, 0, "WORKDAY.INTL(DATE(2012,1,1),30,17)");
        num(2, 0, "DATEDIF(DATE(2001,1,1),DATE(2003,1,1),\"Y\")");
        num(440, 0, "DATEDIF(DATE(2001,6,1),DATE(2002,8,15),\"D\")");
        num(75, 0, "DATEDIF(DATE(2001,6,1),DATE(2002,8,15),\"YD\")");
        num(14, 0, "DATEDIF(DATE(2001,6,1),DATE(2002,8,15),\"M\")");
        num(2, 0, "DATEDIF(DATE(2001,6,1),DATE(2002,8,15),\"ym\")");
        num(14, 0, "DATEDIF(DATE(2001,6,1),DATE(2002,8,15),\"MD\")");
        val(CellError.NUM, "DATEDIF(DATE(2003,1,1),DATE(2001,1,1),\"Y\")");
        val(CellError.NUM, "DATEDIF(DATE(2001,1,1),DATE(2003,1,1),\"W\")");
        num(40527, 0, "EDATE(DATE(2011,1,15),-1)");
        num(40589, 0, "EDATE(DATE(2011,1,15),1)");
        num(40602, 0, "EDATE(DATE(2011,1,31),1)");
        num(40602, 0, "EOMONTH(DATE(2011,1,1),1)");
        num(40482, 0, "EOMONTH(DATE(2011,1,1),-3)");
        num(0.5, 1e-12, "TIME(12,0,0)");
        num(0.2743, 0.00005, "TIMEVALUE(\"6:35 AM\")");
        num(0.774306, 1e-6, "TIMEVALUE(\"6:35pm\")");
        num(0.0243056, 1e-7, "TIMEVALUE(\"12:35 a\")");
        num(0.5, 1e-12, "TIMEVALUE(\"12:00 PM\")");
        val(CellError.VALUE, "TIMEVALUE(\"13:00 PM\")");
        val(CellError.VALUE, "TIMEVALUE(\"6:35 XM\")");
        num(40777, 0, "DATEVALUE(\"2011-08-22\")");
        num(18, 0, "HOUR(0.75)");
        num(45, 0, "MINUTE(0.78125)");
        num(42, 0, "DAYS(DATE(2021,3,15),DATE(2021,2,1))");
        // the day that never was is day 60, and the first of March 1900 the day after it
        num(60, 0, "DATE(1900,2,29)");
        num(61, 0, "DATE(1900,3,1)");
        num(29, 0, "DAY(60)");
        num(2, 0, "MONTH(60)");
        num(39448, 0, "DATE(2008,1,1)");
        num(39829, 0, "DATE(2008,14,-15)");
        val(CellError.NUM, "DATE(-1,1,1)");
        val(CellError.NUM, "YEAR(-1)");
    }
}
