# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/stringprep.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""stringprep: the RFC 3454 tables, as membership tests.

The tables that RFC 3454 defines from Unicode 3.2 properties ask
unicodedata, which answers with its current version; a character assigned
after 3.2 is therefore not reported unassigned by in_table_a1. Table B.2
and B.3 are approximated by full case folding."""

from unicodedata import ucd_3_2_0 as unicodedata

assert unicodedata.unidata_version


def in_table_a1(code):
    if unicodedata.category(code) != "Cn":
        return False
    c = ord(code)
    if 0xFDD0 <= c < 0xFDF0:
        return False
    return (c & 0xFFFF) not in (0xFFFE, 0xFFFF)


b1_set = set([173, 847, 6150, 6155, 6156, 6157, 8203, 8204, 8205, 8288, 65279] + list(range(65024, 65040)))


def in_table_b1(code):
    return ord(code) in b1_set


def map_table_b3(code):
    return code.casefold()


def map_table_b2(a):
    al = map_table_b3(a)
    b = unicodedata.normalize("NFKC", al)
    bl = "".join([map_table_b3(ch) for ch in b])
    c = unicodedata.normalize("NFKC", bl)
    if b != c:
        return c
    return al


def in_table_c11(code):
    return code == " "


def in_table_c12(code):
    return unicodedata.category(code) == "Zs" and code != " "


def in_table_c11_c12(code):
    return unicodedata.category(code) == "Zs"


def in_table_c21(code):
    return ord(code) < 128 and unicodedata.category(code) == "Cc"


c22_specials = set([1757, 1807, 6158, 8204, 8205, 8232, 8233, 65279] + list(range(8288, 8292))
                   + list(range(8298, 8304)) + list(range(65529, 65533)) + list(range(119155, 119163)))


def in_table_c22(code):
    c = ord(code)
    if c < 128:
        return False
    if unicodedata.category(code) == "Cc":
        return True
    return c in c22_specials


def in_table_c21_c22(code):
    return unicodedata.category(code) == "Cc" or ord(code) in c22_specials


def in_table_c3(code):
    return unicodedata.category(code) == "Co"


def in_table_c4(code):
    c = ord(code)
    if c < 0xFDD0:
        return False
    if c < 0xFDF0:
        return True
    return (ord(code) & 0xFFFF) in (0xFFFE, 0xFFFF)


def in_table_c5(code):
    return unicodedata.category(code) == "Cs"


c6_set = set(range(65529, 65534))


def in_table_c6(code):
    return ord(code) in c6_set


c7_set = set(range(12272, 12284))


def in_table_c7(code):
    return ord(code) in c7_set


c8_set = set([832, 833, 8206, 8207] + list(range(8234, 8239)) + list(range(8298, 8304)))


def in_table_c8(code):
    return ord(code) in c8_set


c9_set = set([917505] + list(range(917536, 917632)))


def in_table_c9(code):
    return ord(code) in c9_set


def in_table_d1(code):
    return unicodedata.bidirectional(code) in ("R", "AL")


def in_table_d2(code):
    return unicodedata.bidirectional(code) == "L"
