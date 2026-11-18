#!/usr/bin/env python3
"""Writes the two workbooks the developer guide opens in its figures.

    make-guide-workbooks.py <output directory>

openpyxl authors each one and LibreOffice saves it, so the files the guide's
figures read were written by a spreadsheet application and carry the values
it calculated: regional-sales.xlsx, three sheets that read one another, and
loan-schedule.ods, a schedule of financial functions over defined names.

Run inside the image built from the Containerfile beside this script, then
copy the two files to docs/demos/common/src/main/resources.
"""
import datetime
import os
import shutil
import subprocess
import sys
import tempfile

from openpyxl import Workbook
from openpyxl.styles import Font
from openpyxl.workbook.defined_name import DefinedName

ORDERS = [
    (1041, datetime.date(2026, 1, 12), "North", "Compressor", 14, 1250.0, 0.05),
    (1042, datetime.date(2026, 1, 19), "South", "Valve kit", 220, 18.4, 0.0),
    (1043, datetime.date(2026, 2, 2), "East", "Control unit", 9, 2380.0, 0.1),
    (1044, datetime.date(2026, 2, 9), "North", "Valve kit", 310, 18.4, 0.02),
    (1045, datetime.date(2026, 2, 23), "West", "Compressor", 6, 1250.0, 0.0),
    (1046, datetime.date(2026, 3, 3), "South", "Control unit", 11, 2380.0, 0.075),
    (1047, datetime.date(2026, 3, 16), "East", "Sensor pack", 96, 74.5, 0.0),
    (1048, datetime.date(2026, 3, 30), "West", "Sensor pack", 140, 74.5, 0.03),
    (1049, datetime.date(2026, 4, 6), "North", "Control unit", 5, 2380.0, 0.0),
    (1050, datetime.date(2026, 4, 20), "South", "Compressor", 12, 1250.0, 0.04),
]

RATES = [("North", 0.04, 60000.0), ("South", 0.035, 40000.0), ("East", 0.05, 35000.0), ("West", 0.045, 25000.0)]


def name(wb, text, target):
    defined = DefinedName(text, attr_text=target)
    if hasattr(wb.defined_names, "append"):
        wb.defined_names.append(defined)
    else:
        wb.defined_names[text] = defined


def header(sheet, titles, widths):
    for c, title in enumerate(titles, 1):
        sheet.cell(1, c).value = title
        sheet.cell(1, c).font = Font(bold=True)
        sheet.column_dimensions[sheet.cell(1, c).column_letter].width = widths[c - 1]
    sheet.freeze_panes = "A2"


def sales(path):
    wb = Workbook()
    orders = wb.active
    orders.title = "Orders"
    header(orders, ["Order", "Placed", "Region", "Product", "Units", "Price", "Discount", "Net"],
           [7, 12, 8, 13, 7, 9, 9, 10])
    for r, row in enumerate(ORDERS, 2):
        for c, value in enumerate(row, 1):
            orders.cell(r, c).value = value
        orders.cell(r, 2).number_format = "d mmm yyyy"
        orders.cell(r, 6).number_format = "#,##0.00"
        orders.cell(r, 7).number_format = "0.0%"
        orders.cell(r, 8).value = "=E%d*F%d*(1-G%d)" % (r, r, r)
        orders.cell(r, 8).number_format = "#,##0.00"

    rates = wb.create_sheet("Rates")
    header(rates, ["Region", "Commission", "Target"], [10, 12, 12])
    for r, row in enumerate(RATES, 2):
        for c, value in enumerate(row, 1):
            rates.cell(r, c).value = value
        rates.cell(r, 2).number_format = "0.0%"
        rates.cell(r, 3).number_format = "#,##0"

    summary = wb.create_sheet("Summary")
    header(summary, ["Region", "Orders", "Units", "Revenue", "Share", "Commission", "Of target", "Standing"],
           [10, 8, 7, 11, 8, 12, 10, 11])
    last = len(RATES) + 1
    for r, row in enumerate(RATES, 2):
        summary.cell(r, 1).value = row[0]
        summary.cell(r, 2).value = "=COUNTIF(Orders!$C$2:$C$11,A%d)" % r
        summary.cell(r, 3).value = "=SUMIF(Orders!$C$2:$C$11,A%d,Orders!$E$2:$E$11)" % r
        summary.cell(r, 4).value = "=SUMIFS(Orders!$H$2:$H$11,Orders!$C$2:$C$11,A%d)" % r
        summary.cell(r, 5).value = "=D%d/SUM(D$2:D$%d)" % (r, last)
        summary.cell(r, 6).value = "=ROUND(D%d*VLOOKUP(A%d,Rates!$A$2:$C$5,2,FALSE),2)" % (r, r)
        summary.cell(r, 7).value = "=D%d/INDEX(Rates!$C$2:$C$5,MATCH(A%d,Rates!$A$2:$A$5,0))" % (r, r)
        summary.cell(r, 8).value = '=IF(G%d>=1,"Ahead",IF(G%d>=0.75,"On track","Behind"))' % (r, r)
    total = last + 1
    summary.cell(total, 1).value = "All regions"
    for c, letter in ((2, "B"), (3, "C"), (4, "D"), (5, "E"), (6, "F")):
        summary.cell(total, c).value = "=SUM(%s2:%s%d)" % (letter, letter, last)
    summary.cell(total, 7).value = "=D%d/SUM(Rates!$C$2:$C$5)" % total
    summary.cell(total, 8).value = '=INDEX(A2:A%d,MATCH(MAX(D2:D%d),D2:D%d,0))&" leads"' % (last, last, last)
    for r in range(2, total + 1):
        summary.cell(r, 3).number_format = "#,##0"
        summary.cell(r, 4).number_format = "#,##0.00"
        summary.cell(r, 5).number_format = "0.0%"
        summary.cell(r, 6).number_format = "#,##0.00"
        summary.cell(r, 7).number_format = "0%"
    wb.save(path)


def loan(path):
    wb = Workbook()
    terms = wb.active
    terms.title = "Terms"
    terms["A1"] = "Amount"
    terms["B1"] = 18000.0
    terms["B1"].number_format = "#,##0.00"
    terms["A2"] = "Yearly rate"
    terms["B2"] = 0.069
    terms["B2"].number_format = "0.00%"
    terms["A3"] = "Months"
    terms["B3"] = 12
    terms["A4"] = "First payment"
    terms["B4"] = datetime.date(2026, 2, 1)
    terms["B4"].number_format = "yyyy-mm-dd"
    name(wb, "Amount", "Terms!$B$1")
    name(wb, "Rate", "Terms!$B$2")
    name(wb, "Months", "Terms!$B$3")
    name(wb, "FirstPayment", "Terms!$B$4")

    plan = wb.create_sheet("Schedule")
    header(plan, ["Month", "Due", "Payment", "Interest", "Principal", "Balance", "Repaid"],
           [8, 13, 12, 11, 12, 13, 9])
    for r in range(2, 14):
        plan.cell(r, 1).value = r - 1
        plan.cell(r, 2).value = "=EDATE(FirstPayment,A%d-1)" % r
        plan.cell(r, 2).number_format = "d mmm yyyy"
        plan.cell(r, 3).value = "=-PMT(Rate/12,Months,Amount)"
        plan.cell(r, 4).value = "=-IPMT(Rate/12,A%d,Months,Amount)" % r
        plan.cell(r, 5).value = "=-PPMT(Rate/12,A%d,Months,Amount)" % r
        plan.cell(r, 6).value = "=Amount-SUM(E$2:E%d)" % r
        plan.cell(r, 7).value = "=1-F%d/Amount" % r
        for c in (3, 4, 5, 6):
            plan.cell(r, c).number_format = "#,##0.00"
        plan.cell(r, 7).number_format = "0.0%"
    wb.active = 1
    wb.save(path)


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    work = tempfile.mkdtemp()
    profile = "-env:UserInstallation=file://" + os.path.join(work, "profile")
    for build, stem, kind in ((sales, "regional-sales", "xlsx"), (loan, "loan-schedule", "ods")):
        src = os.path.join(work, stem + ".xlsx")
        build(src)
        staged = os.path.join(work, "saved")
        subprocess.check_call(["soffice", profile, "--headless", "--convert-to", kind, "--outdir", staged, src])
        shutil.copyfile(os.path.join(staged, stem + "." + kind), os.path.join(out, stem + "." + kind))
    print("wrote", sorted(os.listdir(out)))


if __name__ == "__main__":
    main()
