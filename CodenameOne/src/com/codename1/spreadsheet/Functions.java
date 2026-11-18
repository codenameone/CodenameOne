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

import java.util.ArrayList;
import java.util.HashMap;

/// The table of worksheet functions: which exist, which class implements each, and how each
/// is called.
///
/// A function registered as scalar takes single values and is applied element by element to
/// an array; it never sees an error, which is its result without a call. Every other function
/// receives its arguments as they evaluated, references and arrays included.
final class Functions {
    static final int CATEGORY = 0xff;
    static final int SPECIAL = 0;
    static final int MATH = 1;
    static final int STAT = 2;
    static final int LOGIC = 3;
    static final int TEXT = 4;
    static final int DATE = 5;
    static final int LOOKUP = 6;
    static final int FINANCIAL = 7;
    static final int ENGINEERING = 8;
    static final int DATABASE = 9;
    static final int DISTRIBUTION = 10;
    static final int ARRAY = 11;

    static final int SCALAR = 0x100;
    /// The result changes without any cell changing, or depends on cells the formula does
    /// not name.
    static final int VOLATILE = 0x200;

    private static final String SPECIAL_NAMES = "IF IFERROR IFNA IFS SWITCH CHOOSE LET ROW COLUMN ISFORMULA FORMULATEXT";

    private static final String VOLATILE_NAMES = " NOW TODAY RAND RANDBETWEEN RANDARRAY OFFSET INDIRECT ";

    /// Functions newer than the Excel file format. A file names them with a prefix, and Excel
    /// shows `#NAME?` for one written without it.
    private static final String XLFN = " ACOT ACOTH AGGREGATE ARABIC BASE BETA.DIST BETA.INV BINOM.DIST BINOM.DIST.RANGE"
            + " BINOM.INV BITAND BITLSHIFT BITOR BITRSHIFT BITXOR CEILING.MATH CEILING.PRECISE CHISQ.DIST CHISQ.DIST.RT"
            + " CHISQ.INV CHISQ.INV.RT CHISQ.TEST COMBINA CONCAT CONFIDENCE.NORM CONFIDENCE.T COT COTH COVARIANCE.P"
            + " COVARIANCE.S CSC CSCH DAYS DECIMAL ERF.PRECISE ERFC.PRECISE EXPON.DIST F.DIST F.DIST.RT F.INV F.INV.RT"
            + " F.TEST FLOOR.MATH FLOOR.PRECISE FORECAST.LINEAR FORMULATEXT GAMMA GAMMA.DIST GAMMA.INV GAMMALN.PRECISE"
            + " GAUSS HYPGEOM.DIST IFNA IFS ISFORMULA ISO.CEILING ISOWEEKNUM LET LOGNORM.DIST LOGNORM.INV MAXIFS MINIFS"
            + " MODE.MULT MODE.SNGL MUNIT NEGBINOM.DIST NETWORKDAYS.INTL NORM.DIST NORM.INV NORM.S.DIST NORM.S.INV"
            + " NUMBERVALUE PERCENTILE.EXC PERCENTILE.INC PERCENTRANK.EXC PERCENTRANK.INC PERMUTATIONA PHI POISSON.DIST"
            + " QUARTILE.EXC QUARTILE.INC RANDARRAY RANK.AVG RANK.EQ SEC SECH SEQUENCE SHEET SHEETS SKEW.P STDEV.P"
            + " STDEV.S SWITCH T.DIST T.DIST.2T T.DIST.RT T.INV T.INV.2T T.TEST TEXTAFTER TEXTBEFORE TEXTJOIN TEXTSPLIT"
            + " PDURATION RRI UNICHAR UNICODE UNIQUE VAR.P VAR.S WEIBULL.DIST WORKDAY.INTL XLOOKUP XMATCH XOR Z.TEST TOCOL TOROW"
            + " VSTACK HSTACK TAKE DROP CHOOSEROWS CHOOSECOLS EXPAND WRAPROWS WRAPCOLS ARRAYTOTEXT VALUETOTEXT SORTBY ";

    /// The newer functions that take a second prefix in an Excel file.
    private static final String XLWS = " FILTER SORT ";

    /// Functions an OpenDocument file names with the `COM.MICROSOFT.` prefix: the ones Excel
    /// added that the OpenFormula standard does not define.
    private static final String ODS_MICROSOFT = " CEILING FLOOR IFS SWITCH CONCAT TEXTJOIN MAXIFS MINIFS XLOOKUP XMATCH FILTER SORT"
            + " SORTBY UNIQUE SEQUENCE RANDARRAY LET BETA.DIST BETA.INV BINOM.DIST BINOM.INV CHISQ.DIST CHISQ.DIST.RT"
            + " CHISQ.INV CHISQ.INV.RT CHISQ.TEST CONFIDENCE.NORM CONFIDENCE.T COVARIANCE.P COVARIANCE.S ERF.PRECISE"
            + " ERFC.PRECISE EXPON.DIST F.DIST F.DIST.RT F.INV F.INV.RT F.TEST GAMMA.DIST GAMMA.INV GAMMALN.PRECISE"
            + " HYPGEOM.DIST LOGNORM.DIST LOGNORM.INV MODE.MULT MODE.SNGL NEGBINOM.DIST NORM.DIST NORM.INV NORM.S.DIST"
            + " NORM.S.INV PERCENTILE.EXC PERCENTILE.INC PERCENTRANK.EXC PERCENTRANK.INC POISSON.DIST QUARTILE.EXC"
            + " QUARTILE.INC RANK.AVG RANK.EQ STDEV.P STDEV.S T.DIST T.DIST.2T T.DIST.RT T.INV T.INV.2T T.TEST VAR.P"
            + " VAR.S WEIBULL.DIST Z.TEST NETWORKDAYS.INTL WORKDAY.INTL CEILING.MATH CEILING.PRECISE FLOOR.MATH"
            + " FLOOR.PRECISE FORECAST.LINEAR AGGREGATE ";

    private Functions() {
    }

    /// The function table, built when the first formula is read: a class of its own so
    /// that the class loader, and no lock, is what builds it once.
    private static final class Table {
        static final HashMap<String, Integer> BY_NAME;
        static final String[] NAMES;

        static {
            HashMap<String, Integer> t = new HashMap<String, Integer>();
            ArrayList<String> all = new ArrayList<String>();
            register(t, all, SPECIAL_NAMES, SPECIAL);
            register(t, all, MathFunctions.SCALAR_NAMES, MATH | SCALAR);
            register(t, all, MathFunctions.RANGE_NAMES, MATH);
            register(t, all, StatFunctions.SCALAR_NAMES, STAT | SCALAR);
            register(t, all, StatFunctions.RANGE_NAMES, STAT);
            register(t, all, LogicFunctions.SCALAR_NAMES, LOGIC | SCALAR);
            register(t, all, LogicFunctions.RANGE_NAMES, LOGIC);
            register(t, all, TextFunctions.SCALAR_NAMES, TEXT | SCALAR);
            register(t, all, TextFunctions.RANGE_NAMES, TEXT);
            register(t, all, DateFunctions.SCALAR_NAMES, DATE | SCALAR);
            register(t, all, DateFunctions.RANGE_NAMES, DATE);
            register(t, all, LookupFunctions.RANGE_NAMES, LOOKUP);
            register(t, all, FinancialFunctions.SCALAR_NAMES, FINANCIAL | SCALAR);
            register(t, all, FinancialFunctions.RANGE_NAMES, FINANCIAL);
            register(t, all, EngineeringFunctions.SCALAR_NAMES, ENGINEERING | SCALAR);
            register(t, all, EngineeringFunctions.RANGE_NAMES, ENGINEERING);
            register(t, all, DatabaseFunctions.RANGE_NAMES, DATABASE);
            register(t, all, DistributionFunctions.SCALAR_NAMES, DISTRIBUTION | SCALAR);
            register(t, all, DistributionFunctions.RANGE_NAMES, DISTRIBUTION);
            register(t, all, ArrayFunctions.RANGE_NAMES, ARRAY);
            String[] sorted = new String[all.size()];
            all.toArray(sorted);
            java.util.Arrays.sort(sorted);
            BY_NAME = t;
            NAMES = sorted;
        }
    }

    private static void register(HashMap<String, Integer> t, ArrayList<String> all, String list, int info) {
        int n = list.length();
        int start = 0;
        while (start < n) {
            int end = list.indexOf(' ', start);
            if (end < 0) {
                end = n;
            }
            if (end > start) {
                String name = list.substring(start, end);
                int flags = info;
                if (VOLATILE_NAMES.indexOf(" " + name + " ") >= 0) {
                    flags |= VOLATILE;
                }
                t.put(name, Integer.valueOf(flags));
                all.add(name);
            }
            start = end + 1;
        }
    }

    /// The flags of a function, or -1 when there is no function of that name. The name is
    /// upper case, as the parser leaves it.
    /// A table for a switch on text: the position of each word is the label of its case.
    /// The core is compiled at a source level that has no switch on strings.
    static HashMap<String, Integer> switchTable(String... words) {
        HashMap<String, Integer> t = new HashMap<String, Integer>(words.length * 2);
        for (int i = 0; i < words.length; i++) {
            t.put(words[i], Integer.valueOf(i));
        }
        return t;
    }

    /// The case of a word in a table made by [#switchTable(String...)], -1 for a word that
    /// has none.
    static int index(HashMap<String, Integer> table, String word) {
        Integer i = word == null ? null : table.get(word);
        return i == null ? -1 : i.intValue();
    }

    static int info(String name) {
        Integer i = Table.BY_NAME.get(name);
        return i == null ? -1 : i.intValue();
    }

    static String[] names() {
        String[] names = Table.NAMES;
        String[] copy = new String[names.length];
        System.arraycopy(names, 0, copy, 0, copy.length);
        return copy;
    }

    static Object call(int info, String name, Evaluator ev, Object[] args) {
        switch (info & CATEGORY) {
            case MATH:
                return MathFunctions.call(name, ev, args);
            case STAT:
                return StatFunctions.call(name, ev, args);
            case LOGIC:
                return LogicFunctions.call(name, ev, args);
            case TEXT:
                return TextFunctions.call(name, ev, args);
            case DATE:
                return DateFunctions.call(name, ev, args);
            case LOOKUP:
                return LookupFunctions.call(name, ev, args);
            case FINANCIAL:
                return FinancialFunctions.call(name, ev, args);
            case ENGINEERING:
                return EngineeringFunctions.call(name, ev, args);
            case DATABASE:
                return DatabaseFunctions.call(name, ev, args);
            case DISTRIBUTION:
                return DistributionFunctions.call(name, ev, args);
            case ARRAY:
                return ArrayFunctions.call(name, ev, args);
            default:
                return CellError.NAME;
        }
    }

    /// What a file writes in front of a function name.
    /// Functions OpenDocument renamed with `LEGACY.` when it gave their plain name to the
    /// newer function, where Excel kept the old name and gave the new function a dotted one.
    private static final String ODS_LEGACY = " CHIDIST CHIINV FDIST FINV NORMSDIST NORMSINV TDIST ";
    /// The newer functions that took those names, as OpenDocument and as Excel call them.
    private static final String[] ODS_RENAMED = {
        "FDIST", "F.DIST", "FINV", "F.INV", "CHISQDIST", "CHISQ.DIST", "CHISQINV", "CHISQ.INV"
    };

    /// The name used in the trees, Excel's, of a function as an OpenDocument formula names it.
    static String fromOds(String name) {
        if (name.startsWith("LEGACY.")) {
            return name.substring(7);
        }
        for (int i = 0; i < ODS_RENAMED.length; i += 2) {
            if (ODS_RENAMED[i].equals(name)) {
                return ODS_RENAMED[i + 1];
            }
        }
        return name;
    }

    /// The name, prefix included, an OpenDocument formula gives a function.
    static String toOds(String name) {
        if (ODS_LEGACY.indexOf(" " + name + " ") >= 0) {
            return "LEGACY." + name;
        }
        for (int i = 0; i < ODS_RENAMED.length; i += 2) {
            if (ODS_RENAMED[i + 1].equals(name)) {
                return ODS_RENAMED[i];
            }
        }
        return filePrefix(name, true) + name;
    }

    static String filePrefix(String name, boolean ods) {
        String key = " " + name + " ";
        if (ods) {
            return ODS_MICROSOFT.indexOf(key) >= 0 ? "COM.MICROSOFT." : "";
        }
        if (XLWS.indexOf(key) >= 0) {
            return "_xlfn._xlws.";
        }
        return XLFN.indexOf(key) >= 0 ? "_xlfn." : "";
    }
}
