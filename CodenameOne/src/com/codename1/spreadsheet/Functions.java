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
    private static final String ODS_LEGACY = " CHIDIST CHIINV CHITEST FDIST FINV NORMSDIST NORMSINV TDIST ";
    /// The newer functions that took those names, as OpenDocument and as Excel call them.
    private static final String[] ODS_RENAMED = {
        "FDIST", "F.DIST", "FINV", "F.INV", "CHISQDIST", "CHISQ.DIST", "CHISQINV", "CHISQ.INV",
        // and two OpenDocument names apart from Excel's
        "FORMULA", "FORMULATEXT", "SKEWP", "SKEW.P"
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

    /// How many arguments each function takes, as Excel accepts them when a formula is typed:
    /// `NAME:least:most`. A formula that gives a function too few or too many cannot be
    /// entered in Excel, so it is refused here too, rather than calculated with defaults that
    /// Excel never applies and then written to a file Excel rejects.
    private static final String ARITY = " "
            + "ABS:1:1 ACOS:1:1 ACOSH:1:1 ACOT:1:1 ACOTH:1:1 ADDRESS:2:5 AGGREGATE:3:255 AND:1:255 "
            + "ARABIC:1:1 AREAS:1:1 ASIN:1:1 ASINH:1:1 ATAN:1:1 ATAN2:2:2 ATANH:1:1 AVEDEV:1:255 "
            + "AVERAGE:1:255 AVERAGEA:1:255 AVERAGEIF:2:3 AVERAGEIFS:3:255 BASE:2:3 BETA.DIST:4:6 "
            + "BETA.INV:3:5 BETADIST:3:5 BETAINV:3:5 BIN2DEC:1:1 BIN2HEX:1:2 BIN2OCT:1:2 "
            + "BINOM.DIST:4:4 BINOM.DIST.RANGE:3:4 BINOM.INV:3:3 BINOMDIST:4:4 BITAND:2:2 "
            + "BITLSHIFT:2:2 BITOR:2:2 BITRSHIFT:2:2 BITXOR:2:2 CEILING:2:2 CEILING.MATH:1:3 "
            + "CEILING.PRECISE:1:2 CHAR:1:1 CHIDIST:2:2 CHIINV:2:2 CHISQ.DIST:3:3 CHISQ.DIST.RT:2:2 "
            + "CHISQ.INV:2:2 CHISQ.INV.RT:2:2 CHISQ.TEST:2:2 CHITEST:2:2 CHOOSE:2:255 CHOOSECOLS:2:255 "
            + "CHOOSEROWS:2:255 CLEAN:1:1 CODE:1:1 COLUMN:0:1 COLUMNS:1:1 COMBIN:2:2 COMBINA:2:2 "
            + "CONCAT:1:254 CONCATENATE:1:255 CONFIDENCE:3:3 CONFIDENCE.NORM:3:3 CONFIDENCE.T:3:3 "
            + "CONVERT:3:3 CORREL:2:2 COS:1:1 COSH:1:1 COT:1:1 COTH:1:1 COUNT:1:255 COUNTA:1:255 "
            + "COUNTBLANK:1:1 COUNTIF:2:2 COUNTIFS:2:254 COVAR:2:2 COVARIANCE.P:2:2 COVARIANCE.S:2:2 "
            + "CRITBINOM:3:3 CSC:1:1 CSCH:1:1 CUMIPMT:6:6 CUMPRINC:6:6 DATE:3:3 DATEDIF:3:3 "
            + "DATEVALUE:1:1 DAVERAGE:3:3 DAY:1:1 DAYS:2:2 DAYS360:2:3 DB:4:5 DCOUNT:3:3 DCOUNTA:3:3 "
            + "DDB:4:5 DEC2BIN:1:2 DEC2HEX:1:2 DEC2OCT:1:2 DECIMAL:2:2 DEGREES:1:1 DELTA:1:2 "
            + "DEVSQ:1:255 DGET:3:3 DMAX:3:3 DMIN:3:3 DOLLAR:1:2 DOLLARDE:2:2 DOLLARFR:2:2 "
            + "DPRODUCT:3:3 DROP:2:3 DSTDEV:3:3 DSTDEVP:3:3 DSUM:3:3 DVAR:3:3 DVARP:3:3 EDATE:2:2 "
            + "EFFECT:2:2 EOMONTH:2:2 ERF:1:2 ERF.PRECISE:1:1 ERFC:1:1 ERFC.PRECISE:1:1 ERROR.TYPE:1:1 "
            + "EVEN:1:1 EXACT:2:2 EXP:1:1 EXPAND:2:4 EXPON.DIST:3:3 EXPONDIST:3:3 F.DIST:4:4 "
            + "F.DIST.RT:3:3 F.INV:3:3 F.INV.RT:3:3 F.TEST:2:2 FACT:1:1 FACTDOUBLE:1:1 FALSE:0:0 "
            + "FDIST:3:3 FILTER:2:3 FIND:2:3 FINV:3:3 FISHER:1:1 FISHERINV:1:1 FIXED:1:3 FLOOR:2:2 "
            + "FLOOR.MATH:1:3 FLOOR.PRECISE:1:2 FORECAST:3:3 FORECAST.LINEAR:3:3 FORMULATEXT:1:1 "
            + "FREQUENCY:2:2 FTEST:2:2 FV:3:5 FVSCHEDULE:2:2 GAMMA:1:1 GAMMA.DIST:4:4 GAMMA.INV:3:3 "
            + "GAMMADIST:4:4 GAMMAINV:3:3 GAMMALN:1:1 GAMMALN.PRECISE:1:1 GAUSS:1:1 GCD:1:255 "
            + "GEOMEAN:1:255 GESTEP:1:2 HARMEAN:1:255 HEX2BIN:1:2 HEX2DEC:1:1 HEX2OCT:1:2 HLOOKUP:3:4 "
            + "HOUR:1:1 HSTACK:1:254 HYPGEOM.DIST:5:5 HYPGEOMDIST:4:4 IF:2:3 IFERROR:2:2 IFNA:2:2 "
            + "IFS:2:254 INDEX:2:4 INDIRECT:1:2 INT:1:1 INTERCEPT:2:2 IPMT:4:6 IRR:1:2 ISBLANK:1:1 "
            + "ISERR:1:1 ISERROR:1:1 ISEVEN:1:1 ISFORMULA:1:1 ISLOGICAL:1:1 ISNA:1:1 ISNONTEXT:1:1 "
            + "ISNUMBER:1:1 ISO.CEILING:1:2 ISODD:1:1 ISOWEEKNUM:1:1 ISPMT:4:4 ISREF:1:1 ISTEXT:1:1 "
            + "KURT:1:255 LARGE:2:2 LCM:1:255 LEFT:1:2 LEN:1:1 LET:3:253 LN:1:1 LOG:1:2 LOG10:1:1 "
            + "LOGINV:3:3 LOGNORM.DIST:4:4 LOGNORM.INV:3:3 LOGNORMDIST:3:3 LOOKUP:2:3 LOWER:1:1 "
            + "MATCH:2:3 MAX:1:255 MAXA:1:255 MAXIFS:3:255 MDETERM:1:1 MEDIAN:1:255 MID:3:3 MIN:1:255 "
            + "MINA:1:255 MINIFS:3:255 MINUTE:1:1 MINVERSE:1:1 MIRR:3:3 MMULT:2:2 MOD:2:2 MODE:1:255 "
            + "MODE.MULT:1:255 MODE.SNGL:1:255 MONTH:1:1 MROUND:2:2 MULTINOMIAL:1:255 MUNIT:1:1 N:1:1 "
            + "NA:0:0 NEGBINOM.DIST:4:4 NEGBINOMDIST:3:3 NETWORKDAYS:2:3 NETWORKDAYS.INTL:2:4 "
            + "NOMINAL:2:2 NORM.DIST:4:4 NORM.INV:3:3 NORM.S.DIST:2:2 NORM.S.INV:1:1 NORMDIST:4:4 "
            + "NORMINV:3:3 NORMSDIST:1:1 NORMSINV:1:1 NOT:1:1 NOW:0:0 NPER:3:5 NPV:2:255 "
            + "NUMBERVALUE:1:3 OCT2BIN:1:2 OCT2DEC:1:1 OCT2HEX:1:2 ODD:1:1 OFFSET:3:5 OR:1:255 "
            + "PDURATION:3:3 PEARSON:2:2 PERCENTILE:2:2 PERCENTILE.EXC:2:2 PERCENTILE.INC:2:2 "
            + "PERCENTRANK:2:3 PERCENTRANK.EXC:2:3 PERCENTRANK.INC:2:3 PERMUT:2:2 PERMUTATIONA:2:2 "
            + "PHI:1:1 PI:0:0 PMT:3:5 POISSON:3:3 POISSON.DIST:3:3 POWER:2:2 PPMT:4:6 PRODUCT:1:255 "
            + "PROPER:1:1 PV:3:5 QUARTILE:2:2 QUARTILE.EXC:2:2 QUARTILE.INC:2:2 QUOTIENT:2:2 "
            + "RADIANS:1:1 RAND:0:0 RANDARRAY:0:5 RANDBETWEEN:2:2 RANK:2:3 RANK.AVG:2:3 RANK.EQ:2:3 "
            + "RATE:3:6 REPLACE:4:4 REPT:2:2 RIGHT:1:2 ROMAN:1:2 ROUND:2:2 ROUNDDOWN:2:2 ROUNDUP:2:2 "
            + "ROW:0:1 ROWS:1:1 RRI:3:3 RSQ:2:2 SEARCH:2:3 SEC:1:1 SECH:1:1 SECOND:1:1 SEQUENCE:1:4 "
            + "SERIESSUM:4:4 SHEET:0:1 SHEETS:0:1 SIGN:1:1 SIN:1:1 SINH:1:1 SKEW:1:255 SKEW.P:1:255 "
            + "SLN:3:3 SLOPE:2:2 SMALL:2:2 SORT:1:4 SORTBY:2:255 SQRT:1:1 SQRTPI:1:1 STANDARDIZE:3:3 "
            + "STDEV:1:255 STDEV.P:1:255 STDEV.S:1:255 STDEVA:1:255 STDEVP:1:255 STDEVPA:1:255 "
            + "STEYX:2:2 SUBSTITUTE:3:4 SUBTOTAL:2:255 SUM:1:255 SUMIF:2:3 SUMIFS:3:255 "
            + "SUMPRODUCT:1:255 SUMSQ:1:255 SUMX2MY2:2:2 SUMX2PY2:2:2 SUMXMY2:2:2 SWITCH:3:254 SYD:4:4 "
            + "T:1:1 T.DIST:3:3 T.DIST.2T:2:2 T.DIST.RT:2:2 T.INV:2:2 T.INV.2T:2:2 T.TEST:4:4 TAKE:2:3 "
            + "TAN:1:1 TANH:1:1 TDIST:3:3 TEXT:2:2 TEXTAFTER:2:6 TEXTBEFORE:2:6 TEXTJOIN:3:255 "
            + "TIME:3:3 TIMEVALUE:1:1 TINV:2:2 TOCOL:1:3 TODAY:0:0 TOROW:1:3 TRANSPOSE:1:1 TRIM:1:1 "
            + "TRIMMEAN:2:2 TRUE:0:0 TRUNC:1:2 TTEST:4:4 TYPE:1:1 UNICHAR:1:1 UNICODE:1:1 UNIQUE:1:3 "
            + "UPPER:1:1 VALUE:1:1 VAR:1:255 VAR.P:1:255 VAR.S:1:255 VARA:1:255 VARP:1:255 VARPA:1:255 "
            + "VDB:5:7 VLOOKUP:3:4 VSTACK:1:254 WEEKDAY:1:2 WEEKNUM:1:2 WEIBULL:4:4 WEIBULL.DIST:4:4 "
            + "WORKDAY:2:3 WORKDAY.INTL:2:4 WRAPCOLS:2:3 WRAPROWS:2:3 XIRR:2:3 XLOOKUP:3:6 XMATCH:2:4 "
            + "XNPV:3:3 XOR:1:254 YEAR:1:1 YEARFRAC:2:3 Z.TEST:2:3 ZTEST:2:3 ";

    private static final HashMap<String, int[]> ARITIES = arities();

    /// The least and the most arguments a function takes, or null for a name this
    /// implementation does not know.
    static int[] arity(String name) {
        return ARITIES.get(name);
    }

    private static HashMap<String, int[]> arities() {
        HashMap<String, int[]> t = new HashMap<String, int[]>();
        int at = 0;
        while (at < ARITY.length()) {
            int end = ARITY.indexOf(' ', at);
            if (end < 0) {
                end = ARITY.length();
            }
            if (end > at) {
                String entry = ARITY.substring(at, end);
                int a = entry.indexOf(':');
                int b = entry.indexOf(':', a + 1);
                t.put(entry.substring(0, a), new int[] {Integer.parseInt(entry.substring(a + 1, b)),
                        Integer.parseInt(entry.substring(b + 1))});
            }
            at = end + 1;
        }
        return t;
    }

}
