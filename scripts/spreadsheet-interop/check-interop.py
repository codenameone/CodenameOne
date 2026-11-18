#!/usr/bin/env python3
"""Opens the workbooks our writers produced with other implementations.

For every <name>.xlsx and <name>.ods in the directory given:

* openpyxl must load the xlsx, with its formulas and with its stored values;
* LibreOffice must open both files and save each as xlsx;
* every cell LibreOffice saved must hold the value our writer stored for it,
  and every formula must still be a formula.

Run inside the image built from the Containerfile beside this script.
"""
import datetime
import os
import subprocess
import sys
import tempfile

from openpyxl import load_workbook
from openpyxl.utils.datetime import to_excel

failures = []

# Formulas LibreOffice answers differently from Excel whatever file they come from. Each
# is held to Excel's answer in the unit tests.
LIBREOFFICE_DIFFERS = {
    # the sheet of an address is separated with LibreOffice's own "." instead of "!"
    '=ADDRESS(1,1,1,TRUE(),"Data")',
}


def fail(message):
    failures.append(message)
    print("FAIL " + message)


def norm(v):
    if isinstance(v, bool):
        return v
    if isinstance(v, (int, float)):
        return float(v)
    if isinstance(v, datetime.datetime):
        return v.replace(microsecond=0)
    if isinstance(v, datetime.date):
        return datetime.datetime(v.year, v.month, v.day)
    if isinstance(v, datetime.time):
        return v.replace(microsecond=0)
    if v == "":
        return None
    return v


def same(a, b):
    a, b = norm(a), norm(b)
    # LibreOffice gives a formula that answers a date a date format when the cell has none,
    # as a spreadsheet does when the formula is typed. The day number is what is compared.
    if isinstance(a, float) and isinstance(b, (datetime.datetime, datetime.time)):
        b = float(to_excel(b))
    if isinstance(a, float) and isinstance(b, float) and not isinstance(a, bool) and not isinstance(b, bool):
        return abs(a - b) <= 1e-9 * max(1.0, abs(a), abs(b))
    return a == b


def convert(path, kind, outdir, profile):
    subprocess.check_call(["soffice", profile, "--headless", "--convert-to", kind, "--outdir", outdir, path],
                          stdout=subprocess.DEVNULL)
    out = os.path.join(outdir, os.path.splitext(os.path.basename(path))[0] + "." + kind)
    if not os.path.exists(out):
        fail("LibreOffice did not convert " + path)
        return None
    return out


def compare(label, ours_values, ours_formulas, theirs_path):
    theirs_values = load_workbook(theirs_path, data_only=True)
    theirs_formulas = load_workbook(theirs_path)
    if ours_values.sheetnames != theirs_values.sheetnames:
        fail(f"{label}: sheets {theirs_values.sheetnames}, wrote {ours_values.sheetnames}")
        return 0
    cells = 0
    for name in ours_values.sheetnames:
        ov, of, tv, tf = ours_values[name], ours_formulas[name], theirs_values[name], theirs_formulas[name]
        for row in of.iter_rows():
            for cell in row:
                if cell.value is None:
                    continue
                cells += 1
                at = f"{label}: {name}!{cell.coordinate}"
                wrote = ov[cell.coordinate].value
                got = tv[cell.coordinate].value
                formula = isinstance(cell.value, str) and cell.value.startswith("=") and cell.data_type == "f"
                if cell.value in LIBREOFFICE_DIFFERS:
                    continue
                if not same(wrote, got):
                    fail(f"{at} {cell.value!r}: wrote {wrote!r}, LibreOffice has {got!r}")
                if formula and tf[cell.coordinate].data_type != "f":
                    fail(f"{at} {cell.value!r}: no longer a formula")
        if sorted(str(r) for r in ov.merged_cells.ranges) != sorted(str(r) for r in tv.merged_cells.ranges):
            fail(f"{label}: {name} merged {tv.merged_cells.ranges}, wrote {ov.merged_cells.ranges}")
    return cells


def main():
    src = sys.argv[1]
    work = tempfile.mkdtemp()
    profile = "-env:UserInstallation=file://" + os.path.join(work, "profile")
    names = sorted(f[:-5] for f in os.listdir(src) if f.endswith(".xlsx"))
    if not names:
        fail("nothing to check in " + src)
    for name in names:
        xlsx = os.path.join(src, name + ".xlsx")
        ods = os.path.join(src, name + ".ods")
        ours_values = load_workbook(xlsx, data_only=True)
        ours_formulas = load_workbook(xlsx)
        formulas = sum(1 for ws in ours_formulas for row in ws.iter_rows() for c in row if c.data_type == "f")
        from_xlsx = convert(xlsx, "xlsx", os.path.join(work, "from-xlsx"), profile)
        from_ods = convert(ods, "xlsx", os.path.join(work, "from-ods"), profile)
        for label, path in ((name + ".xlsx", from_xlsx), (name + ".ods", from_ods)):
            if path:
                cells = compare(label, ours_values, ours_formulas, path)
                print(f"{label}: {cells} cells, {formulas} formulas compared")
                # a comparison that found nothing to compare has not found the files equal
                if cells < 20:
                    fail(f"{label}: only {cells} cells were compared")
    if failures:
        print(f"{len(failures)} failures")
        sys.exit(1)
    print("spreadsheet interop: ok")


if __name__ == "__main__":
    main()
