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


# LibreOffice keeps the values a file stores unless it is told to calculate on load, and
# its default for both formats is not to. Without this every value compared would be our
# own value read back, and the check would prove nothing about our formulas.
RECALCULATE_ON_LOAD = """<?xml version="1.0" encoding="UTF-8"?>
<oor:items xmlns:oor="http://openoffice.org/2001/registry" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<item oor:path="/org.openoffice.Office.Calc/Formula/Load"><prop oor:name="OOXMLRecalcMode" oor:op="fuse"><value>0</value></prop></item>
<item oor:path="/org.openoffice.Office.Calc/Formula/Load"><prop oor:name="ODFRecalcMode" oor:op="fuse"><value>0</value></prop></item>
</oor:items>
"""


def recalculating_profile(work):
    user = os.path.join(work, "profile", "user")
    os.makedirs(user, exist_ok=True)
    with open(os.path.join(user, "registrymodifications.xcu"), "w") as f:
        f.write(RECALCULATE_ON_LOAD)
    return "-env:UserInstallation=file://" + os.path.join(work, "profile")


PLANTED_PARTS = {
    "[Content_Types].xml": '<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
    '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
    '<Default Extension="xml" ContentType="application/xml"/>'
    '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
    '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
    '</Types>',
    "_rels/.rels": '<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
    '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>'
    '</Relationships>',
    "xl/workbook.xml": '<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"'
    ' xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="S" sheetId="1" r:id="rId1"/></sheets></workbook>',
    "xl/_rels/workbook.xml.rels": '<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
    '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>'
    '</Relationships>',
    "xl/worksheets/sheet1.xml": '<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
    '<sheetData><row r="1"><c r="A1"><f>1+1</f><v>3</v></c></row></sheetData></worksheet>',
}


def check_recalculates(work, profile):
    """A file whose stored value is wrong must come back right, or nothing here is evidence."""
    import zipfile
    planted = os.path.join(work, "planted.xlsx")
    with zipfile.ZipFile(planted, "w") as z:
        for name, text in PLANTED_PARTS.items():
            z.writestr(name, text)
    out = convert(planted, "xlsx", os.path.join(work, "planted-out"), profile)
    if out:
        got = load_workbook(out, data_only=True)["S"]["A1"].value
        if norm(got) != 2.0:
            fail(f"LibreOffice kept the stored value of =1+1 ({got!r}): it is not recalculating on load")


def main():
    src = sys.argv[1]
    work = tempfile.mkdtemp()
    profile = recalculating_profile(work)
    check_recalculates(work, profile)
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
