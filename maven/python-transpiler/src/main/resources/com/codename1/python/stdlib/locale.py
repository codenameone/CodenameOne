# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/locale.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""locale: Codename One text is UTF-8 and formatting is the C locale's;
the device's language reaches an application through its L10N resources."""

LC_CTYPE = 0
LC_NUMERIC = 1
LC_TIME = 2
LC_COLLATE = 3
LC_MONETARY = 4
LC_MESSAGES = 5
LC_ALL = 6
CHAR_MAX = 127


class Error(Exception):
    pass


def setlocale(category, locale=None):
    if locale not in (None, "", "C", "POSIX", "C.UTF-8", "en_US.UTF-8"):
        raise Error("unsupported locale setting")
    return "C.UTF-8"


def getlocale(category=LC_CTYPE):
    return (None, None)


def getdefaultlocale(envvars=("LC_ALL", "LC_CTYPE", "LANG", "LANGUAGE")):
    return (None, "UTF-8")


def getpreferredencoding(do_setlocale=True):
    return "UTF-8"


def getencoding():
    return "UTF-8"


def localeconv():
    return {"decimal_point": ".", "thousands_sep": "", "grouping": [], "int_curr_symbol": "",
            "currency_symbol": "", "mon_decimal_point": "", "mon_thousands_sep": "", "mon_grouping": [],
            "positive_sign": "", "negative_sign": "", "int_frac_digits": CHAR_MAX, "frac_digits": CHAR_MAX,
            "p_cs_precedes": CHAR_MAX, "p_sep_by_space": CHAR_MAX, "n_cs_precedes": CHAR_MAX,
            "n_sep_by_space": CHAR_MAX, "p_sign_posn": CHAR_MAX, "n_sign_posn": CHAR_MAX}


def strcoll(a, b):
    return (a > b) - (a < b)


def strxfrm(s):
    return s


def normalize(localename):
    return localename


def format_string(f, val, grouping=False, monetary=False):
    return f % val


def str(val):
    return "%.12g" % val


def atof(s, func=float):
    return func(s)


def atoi(s):
    return int(s)
