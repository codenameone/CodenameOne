#!/usr/bin/env python3
"""Writes the spreadsheet test fixtures.

openpyxl authors a workbook of data and formulas with no values in the formula
cells; LibreOffice then opens it, calculates every formula and saves it as xlsx
and as ods. The values LibreOffice stored are what the tests hold the formula
evaluator to, so the evaluator is never its own oracle.

Run inside the image built from the Containerfile beside this script:

    make-fixtures.py <output directory>

LibreOffice is the oracle only where it agrees with Excel. It has no logical
cells -- TRUE is the number 1 -- so formulas that tell a truth value from a
number (TYPE(TRUE), "a"&TRUE, a sum over cells holding TRUE) are left out here
and held to Excel's answer in FormulaEvaluationTest instead.
"""
import datetime
import os
import subprocess
import sys
import tempfile

from openpyxl import Workbook
from openpyxl.styles import Font
from openpyxl.workbook.defined_name import DefinedName

ROWS = [
    ("North", "Widget", 12, 4.5, datetime.date(2024, 1, 15), True, 0.82),
    ("South", "Gadget", 7, 19.99, datetime.date(2024, 2, 29), False, 0.4),
    ("East", "Widget", 30, 4.25, datetime.date(2024, 3, 3), True, 0.91),
    ("West", "Sprocket", 0, 120.0, datetime.date(2023, 12, 31), True, 0.05),
    ("North", "Gadget", 18, 21.5, datetime.date(2024, 5, 20), False, 0.67),
    ("South", "Widget", 44, 4.1, datetime.date(2024, 6, 1), True, 0.73),
    ("East", "Sprocket", 3, 115.75, datetime.date(2024, 7, 4), True, 0.12),
    ("West", "Gadget", 25, 18.0, datetime.date(2024, 8, 19), False, 0.55),
    ("North", "Sprocket", 5, 130.0, datetime.date(2024, 9, 9), True, 0.99),
    ("South", "Gadget", 16, 20.25, datetime.date(2024, 10, 31), True, 0.31),
    ("East", "Widget", 52, 3.95, datetime.date(2024, 11, 11), False, 0.48),
    ("West", "Widget", 9, 4.75, datetime.date(2024, 12, 25), True, 0.6),
]

U = "Data!C2:C13"
P = "Data!D2:D13"
D = "Data!E2:E13"
R = "Data!A2:A13"
K = "Data!B2:B13"
G = "Data!G2:G13"

FORMULAS = [
    # operators and coercion
    "1+2*3", "(1+2)*3", "-2^2", "2^3^2", "10/4", "7-10", "50%", "\"a\"&\"b\"&1", "1=1", "\"a\"<\"B\"",
    "1<>2", "3>=3", "\"10\"+5", "TRUE+1", "1/0", "-(3)", "+5", "2*-3", "1E3+1", "\"x\"=\"X\"",
    # references
    "Data!C2", "Data!C2+Data!D2", f"SUM({U})", "SUM(Data!C2:D13)", "SUM(Data!C:C)", "SUM(Data!15:15)",
    "Data!A2&\" \"&Data!B2", "TotalUnits", "TotalUnits*2", "SUM(Data!C2:C4,Data!C10:C13)",
    # math
    "ABS(-3.5)", "SIGN(-2)", "INT(-2.5)", "TRUNC(-2.57,1)", "ROUND(2.5,0)", "ROUND(-2.5,0)", "ROUND(1234.567,-2)",
    "ROUNDUP(2.01,1)", "ROUNDDOWN(-2.09,1)", "MOD(10,3)", "MOD(-10,3)", "MOD(10,-3)", "QUOTIENT(17,5)",
    "POWER(2,10)", "SQRT(2)", "EXP(1)", "LN(10)", "LOG10(1000)", "LOG(8,2)", "PI()", "SIN(1)", "COS(1)", "TAN(1)",
    "ASIN(0.5)", "ACOS(0.5)", "ATAN(1)", "ATAN2(1,2)", "SINH(1)", "COSH(1)", "TANH(1)", "DEGREES(PI())",
    "RADIANS(180)", "FACT(10)", "COMBIN(10,3)", "PERMUT(10,3)", "GCD(48,18)", "LCM(4,6)", "CEILING(2.1,1)",
    "FLOOR(2.9,1)", "CEILING(7,5)", "FLOOR(7,5)", "MROUND(10,3)", "EVEN(3)", "ODD(4)", "PRODUCT(1,2,3,4)",
    f"SUMSQ({U})", f"SUMPRODUCT({U},{P})", f"SUMIF({R},\"North\",{U})", f"SUMIF({U},\">10\")",
    f"SUMIFS({U},{R},\"East\",{K},\"Widget\")", f"SUBTOTAL(9,{U})", f"SUBTOTAL(1,{U})", "SQRTPI(2)",
    "FACTDOUBLE(7)", "MULTINOMIAL(2,3,4)", "SERIESSUM(2,0,1,{1,2,3})", "ROMAN(1994)", "_xlfn.BASE(255,16)",
    "_xlfn.DECIMAL(\"FF\",16)", "_xlfn.ARABIC(\"MCMXCIV\")",
    # statistics
    f"AVERAGE({U})", f"COUNT({U})", "COUNTA(Data!A1:G13)", "COUNTBLANK(Data!A1:H14)", f"MIN({U})", f"MAX({U})",
    f"MEDIAN({U})", f"MODE({U},12)", f"STDEV({U})", f"STDEVP({U})", f"VAR({U})", f"VARP({U})",
    f"_xlfn.STDEV.S({U})", f"_xlfn.VAR.P({U})", f"LARGE({U},2)", f"SMALL({U},3)", f"RANK(30,{U})",
    f"RANK(30,{U},1)", f"PERCENTILE({U},0.9)", f"QUARTILE({U},1)", f"COUNTIF({R},\"North\")",
    f"COUNTIF({U},\">=16\")", f"COUNTIF({K},\"*et\")", f"COUNTIF({K},\"?adget\")",
    f"COUNTIFS({R},\"West\",{U},\"<20\")", f"AVERAGEIF({R},\"South\",{U})",
    f"AVERAGEIFS({P},{K},\"Gadget\",{U},\">10\")", f"_xlfn.MAXIFS({U},{K},\"Widget\")",
    f"_xlfn.MINIFS({P},{R},\"North\")", f"GEOMEAN({P})", f"HARMEAN({P})", f"AVEDEV({U})", f"DEVSQ({U})",
    f"CORREL({U},{P})", f"SLOPE({U},{P})", f"INTERCEPT({U},{P})", f"RSQ({U},{P})", f"FORECAST(10,{U},{P})",
    f"COVAR({U},{P})", f"SKEW({U})", f"KURT({U})", f"TRIMMEAN({U},0.2)", f"PERCENTRANK({U},18)",
    f"STANDARDIZE(20,AVERAGE({U}),STDEV({U}))", f"AVERAGEA({U})", f"MAXA({G})", f"STEYX({U},{P})",
    f"PEARSON({U},{P})", f"_xlfn.PERCENTILE.EXC({U},0.25)", f"_xlfn.QUARTILE.INC({U},3)",
    f"_xlfn.RANK.AVG(12,{U})", f"_xlfn.COVARIANCE.S({U},{P})", f"_xlfn.MODE.SNGL({U},7,7)",
    # logic and information
    "IF(Data!C2>10,\"big\",\"small\")", "IF(Data!C5,\"y\",\"n\")", "IF(1,2)", "IF(0,2)", "AND(TRUE,1,Data!F2)",
    "OR(FALSE,0)", "NOT(TRUE)", "_xlfn.XOR(TRUE,TRUE,TRUE)", "IFERROR(1/0,\"err\")", "_xlfn.IFNA(NA(),\"none\")",
    "_xlfn.IFS(Data!C2>100,\"a\",Data!C2>10,\"b\",TRUE,\"c\")", "_xlfn.SWITCH(Data!B2,\"Gadget\",1,\"Widget\",2,0)",
    "CHOOSE(2,\"a\",\"b\",\"c\")", "ISNUMBER(Data!C2)", "ISTEXT(Data!A2)", "ISBLANK(Data!H2)", "ISERROR(1/0)",
    "ISNA(NA())", "ISERR(NA())", "ISLOGICAL(Data!F2)", "ISNONTEXT(1)", "ISEVEN(4)", "ISODD(4)", "N(TRUE)",
    "N(\"a\")", "T(\"a\")", "T(1)", "TYPE(1)", "TYPE(\"a\")", "ERROR.TYPE(1/0)", "NA()",
    "ISREF(Data!A1)", "TRUE()", "FALSE()", "ERROR.TYPE(NA())",
    # text
    "LEN(Data!A2)", "LEFT(Data!B2,3)", "RIGHT(Data!B2,2)", "MID(Data!B2,2,3)", "UPPER(Data!A2)", "LOWER(Data!A2)",
    "PROPER(\"hello wORLD-foo\")", "TRIM(\"  a   b  \")", "CONCATENATE(Data!A2,\"-\",Data!C2)",
    "_xlfn.CONCAT(Data!A2:B3)", "_xlfn.TEXTJOIN(\",\",TRUE,Data!A2:A5)", "FIND(\"d\",\"Widget\")",
    "SEARCH(\"G\",\"Widget\")", "SEARCH(\"g?t\",\"Widget\")", "SUBSTITUTE(\"a-b-c\",\"-\",\"+\")",
    "SUBSTITUTE(\"a-b-c\",\"-\",\"+\",2)", "REPLACE(\"abcdef\",2,3,\"XY\")", "REPT(\"ab\",3)", "EXACT(\"a\",\"A\")",
    "VALUE(\"12.5\")", "VALUE(\"1,234\")", "VALUE(\"50%\")", "TEXT(1234.5678,\"#,##0.00\")", "TEXT(0.256,\"0.0%\")",
    "TEXT(Data!E2,\"yyyy-mm-dd\")", "TEXT(Data!E3,\"d mmm yyyy\")", "TEXT(5,\"000\")", "TEXT(0.5,\"hh:mm\")",
    "TEXT(1234567,\"0.00E+00\")", "FIXED(1234.567,1)", "FIXED(1234.567,1,TRUE)", "DOLLAR(1234.567,2)",
    "CHAR(65)", "CODE(\"A\")", "CLEAN(\"a\"&CHAR(7)&\"b\")", "_xlfn.UNICHAR(8364)", "_xlfn.UNICODE(\"A\")",
    "_xlfn.NUMBERVALUE(\"1.234,5\",\",\",\".\")", "FIND(\"x\",\"abc\")", "LEFT(\"abc\")", "MID(\"abc\",5,2)",
    "LEN(123.5)", "TEXT(-5,\"0;(0)\")", "TEXT(0,\"0;-0;\\z\")",
    # dates
    "DATE(2024,2,29)", "DATE(2024,14,1)", "DATE(2024,3,0)", "YEAR(Data!E2)", "MONTH(Data!E2)", "DAY(Data!E2)",
    "WEEKDAY(Data!E2)", "WEEKDAY(Data!E2,2)", "WEEKNUM(Data!E2)", "_xlfn.ISOWEEKNUM(Data!E5)",
    "EDATE(Data!E2,1)", "EDATE(DATE(2024,1,31),1)", "EOMONTH(Data!E2,0)", "EOMONTH(Data!E2,-2)",
    "DAYS360(Data!E2,Data!E3)", "DAYS360(Data!E2,Data!E3,TRUE)", "_xlfn.DAYS(Data!E3,Data!E2)",
    "DATEDIF(Data!E2,Data!E7,\"m\")", "DATEDIF(Data!E2,Data!E7,\"d\")", "DATEDIF(Data!E5,Data!E7,\"y\")",
    "DATEDIF(Data!E2,Data!E7,\"md\")", "NETWORKDAYS(Data!E2,Data!E3)", "WORKDAY(Data!E2,10)",
    "YEARFRAC(Data!E2,Data!E7)", "YEARFRAC(Data!E2,Data!E7,1)", "YEARFRAC(Data!E2,Data!E7,3)",
    "TIME(13,45,10)", "HOUR(0.75)", "MINUTE(0.76)", "SECOND(0.7601)", "DATEVALUE(\"2024-03-15\")",
    "TIMEVALUE(\"13:30\")", "Data!E3-Data!E2", f"MAX({D})", f"MIN({D})", "DATE(1900,3,1)", "DATE(2000,1,1)",
    "NETWORKDAYS(Data!E2,Data!E7,Data!E3:E4)", "WORKDAY(Data!E2,-5)",
    # lookups
    "VLOOKUP(\"Gadget\",Data!B2:D13,2,FALSE)", "VLOOKUP(20,Data!C2:D13,2,FALSE)", "VLOOKUP(\"nope\",Data!B2:D13,2,FALSE)",
    "VLOOKUP(3,{1,\"a\";2,\"b\";4,\"c\"},2,TRUE)", "HLOOKUP(\"Price\",Data!A1:G13,3,FALSE)",
    f"MATCH(\"East\",{R},0)", "MATCH(3,{1,2,4,8},1)", "MATCH(3,{8,4,2,1},-1)", f"INDEX({U},4)",
    "INDEX(Data!A2:G13,3,2)", f"INDEX({P},MATCH(\"Sprocket\",{K},0))", "LOOKUP(5,{1,4,9},{\"a\",\"b\",\"c\"})",
    "ROW(Data!C7)", "COLUMN(Data!C7)", f"ROWS({U})", "COLUMNS(Data!A1:G1)", "ADDRESS(3,4)", "ADDRESS(3,4,4)",
    "ADDRESS(1,1,1,TRUE,\"Data\")", "INDIRECT(\"Data!C3\")", f"SUM(OFFSET(Data!C2,1,0,3,1))",
    "SUM(INDIRECT(\"Data!C2:C4\"))", "OFFSET(Data!A1,2,1)", "CHOOSE(3,Data!C2,Data!C3,Data!C4)",
    "SUM(INDEX(Data!C2:D13,0,2))", "AREAS(Data!A1:B2)", "MATCH(\"w*\",Data!B2:B13,0)",
    # financial
    "PMT(0.05/12,360,200000)", "PV(0.05,10,-1000)", "FV(0.05,10,-1000)", "FV(0.05,10,-1000,0,1)", "NPER(0.05,-1000,8000)",
    "RATE(60,-200,10000)", "NPV(0.1,-1000,300,400,500)", "IRR({-1000,300,400,500})", "IPMT(0.05/12,1,360,200000)",
    "PPMT(0.05/12,1,360,200000)", "SLN(10000,1000,5)", "SYD(10000,1000,5,2)", "DB(10000,1000,5,2)",
    "DDB(10000,1000,5,2)", "MIRR({-1000,300,400,500},0.1,0.12)", "EFFECT(0.05,12)", "NOMINAL(0.05,12)",
    "CUMIPMT(0.05/12,360,200000,1,12,0)", "CUMPRINC(0.05/12,360,200000,1,12,0)",
    "XNPV(0.1,{-1000,300,400,500},{45292,45382,45473,45565})", "XIRR({-1000,300,400,500},{45292,45382,45473,45565})",
    "_xlfn.RRI(10,100,200)", "_xlfn.PDURATION(0.05,100,200)", "DOLLARDE(1.02,16)", "DOLLARFR(1.125,16)",
    "ISPMT(0.05,1,10,1000)", "FVSCHEDULE(100,{0.05,0.06})",
    # engineering and distributions
    "DEC2BIN(10)", "DEC2HEX(255)", "DEC2OCT(64)", "BIN2DEC(\"1010\")", "HEX2DEC(\"FF\")", "OCT2DEC(\"100\")",
    "HEX2BIN(\"F\")", "BIN2HEX(\"1111\")", "DEC2BIN(-1)", "HEX2DEC(\"FFFFFFFFFF\")", "_xlfn.BITAND(12,10)",
    "_xlfn.BITOR(12,10)", "_xlfn.BITXOR(12,10)", "_xlfn.BITLSHIFT(1,4)", "_xlfn.BITRSHIFT(16,2)", "DELTA(1,1)",
    "GESTEP(3,2)", "ERF(0.5)", "ERFC(0.5)", "CONVERT(1,\"m\",\"ft\")", "CONVERT(100,\"C\",\"F\")",
    "CONVERT(1,\"mi\",\"km\")", "NORMSDIST(1)", "NORMSINV(0.975)", "NORMDIST(1,0,2,TRUE)", "NORMDIST(1,0,2,FALSE)",
    "NORMINV(0.9,10,2)", "_xlfn.NORM.S.DIST(0.5,FALSE)", "BINOMDIST(3,10,0.5,FALSE)", "BINOMDIST(3,10,0.5,TRUE)",
    "POISSON(2,3,FALSE)", "POISSON(2,3,TRUE)", "EXPONDIST(1,2,TRUE)", "TDIST(2,10,2)", "TINV(0.05,10)",
    "CHIDIST(3,2)", "CHIINV(0.05,2)", "FDIST(2,3,10)", "FINV(0.05,3,10)", "GAMMALN(5.5)", "_xlfn.GAMMA(5.5)",
    "CONFIDENCE(0.05,2,30)", "FISHER(0.5)", "FISHERINV(0.5)", "LOGNORMDIST(2,0,1)", "WEIBULL(1,2,3,TRUE)",
    "_xlfn.T.DIST(1,10,TRUE)", "_xlfn.T.INV(0.9,10)", "_xlfn.CHISQ.DIST(3,2,TRUE)", "_xlfn.F.DIST(2,3,10,TRUE)",
    "BETADIST(0.3,2,3)", "BETAINV(0.5,2,3)", "GAMMADIST(2,3,2,TRUE)", "GAMMAINV(0.5,3,2)", "HYPGEOMDIST(1,4,8,20)",
    "NEGBINOMDIST(3,2,0.5)", "CRITBINOM(10,0.5,0.9)", "_xlfn.PHI(0.5)", "_xlfn.GAUSS(1)",
    # database
    "DSUM(Data!A1:G13,\"Units\",Criteria!A1:A2)", "DAVERAGE(Data!A1:G13,3,Criteria!A1:A2)",
    "DCOUNT(Data!A1:G13,\"Units\",Criteria!A1:B2)", "DMAX(Data!A1:G13,\"Price\",Criteria!A1:A2)",
    "DMIN(Data!A1:G13,\"Price\",Criteria!A1:B2)", "DGET(Data!A1:G13,\"Units\",Criteria!D1:E2)",
    "DCOUNTA(Data!A1:G13,\"Product\",Criteria!A1:A2)", "DPRODUCT(Data!A1:G13,\"Units\",Criteria!A1:B2)",
    # arrays
    "SUM({1,2;3,4})", "SUMPRODUCT({1,2,3},{4,5,6})", "SUM(MMULT({1,2;3,4},{5;6}))", "MDETERM({1,2;3,4})",
    "SUM(TRANSPOSE({1,2,3}))", "INDEX(MINVERSE({1,2;3,4}),1,1)", f"SUMPRODUCT(({R}=\"North\")*{U})",
    f"SUMPRODUCT(--({U}>10))", "COUNT({1,\"a\",2})", f"SUMPRODUCT({U}*{P})/SUM({U})",
    "SUMX2MY2({1,2,3},{4,5,6})", "SUMXMY2({1,2,3},{4,5,6})", "SUMX2PY2({1,2,3},{4,5,6})",
    f"INDEX(FREQUENCY({U},{{10,20,40}}),2)",
]


def build(path):
    wb = Workbook()
    data = wb.active
    data.title = "Data"
    data.append(["Region", "Product", "Units", "Price", "Date", "Active", "Rating"])
    for row in ROWS:
        data.append(list(row))
    for cell in data[1]:
        cell.font = Font(bold=True)
    for r in range(2, len(ROWS) + 2):
        data.cell(r, 4).number_format = "#,##0.00"
        data.cell(r, 5).number_format = "yyyy-mm-dd"
        data.cell(r, 7).number_format = "0.0%"
    data.column_dimensions["A"].width = 14
    data.column_dimensions["E"].width = 12.5
    data.freeze_panes = "B2"
    data["A15"] = "Totals for the year"
    data.merge_cells("A15:B15")
    data["C15"] = f"=SUM(C2:C13)"
    data["D15"] = "=SUMPRODUCT(C2:C13,D2:D13)"
    data["D15"].number_format = "#,##0.00"
    # a chain: each cell reads the one above, so a wrong order shows
    data["I2"] = 1
    for r in range(3, 13):
        data.cell(r, 9).value = f"=I{r - 1}*2+C{r}"

    crit = wb.create_sheet("Criteria")
    crit.append(["Region", "Units", None, "Region", "Product"])
    crit.append(["North", ">10", None, "West", "Sprocket"])

    sheet = wb.create_sheet("Formulas")
    for i, f in enumerate(FORMULAS):
        sheet.cell(i + 1, 1).value = "=" + f
        sheet.cell(i + 1, 2).value = f
    sheet.column_dimensions["A"].width = 18

    quoted = wb.create_sheet("Year's Plan")
    quoted["A1"] = 5
    quoted["A2"] = "=A1*Data!C2"
    sheet.cell(len(FORMULAS) + 1, 1).value = "='Year''s Plan'!A2+1"
    sheet.cell(len(FORMULAS) + 1, 2).value = "'Year''s Plan'!A2+1"

    name = DefinedName("TotalUnits", attr_text="SUM(Data!$C$2:$C$13)")
    if hasattr(wb.defined_names, "append"):
        wb.defined_names.append(name)
    else:
        wb.defined_names["TotalUnits"] = name
    wb.save(path)


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    work = tempfile.mkdtemp()
    src = os.path.join(work, "calc.xlsx")
    build(src)
    profile = "-env:UserInstallation=file://" + os.path.join(work, "profile")
    for kind in ("xlsx", "ods"):
        subprocess.check_call(["soffice", profile, "--headless", "--convert-to", kind, "--outdir", out, src])
    print("wrote", sorted(os.listdir(out)))


if __name__ == "__main__":
    main()
