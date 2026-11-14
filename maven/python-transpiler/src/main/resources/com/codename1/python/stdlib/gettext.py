# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/gettext.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""Message catalogs. A translated program ships no .mo files, so every
lookup answers the message it was given, as CPython does when no catalog is
found; an application localizes through Codename One's resource bundles."""


class NullTranslations:
    def __init__(self, fp=None):
        self._fallback = None

    def add_fallback(self, fallback):
        self._fallback = fallback

    def gettext(self, message):
        return message

    def ngettext(self, msgid1, msgid2, n):
        return msgid1 if n == 1 else msgid2

    def pgettext(self, context, message):
        return message

    def npgettext(self, context, msgid1, msgid2, n):
        return msgid1 if n == 1 else msgid2

    def info(self):
        return {}

    def charset(self):
        return None

    def install(self, names=None):
        import builtins
        builtins._ = self.gettext


GNUTranslations = NullTranslations


def translation(domain, localedir=None, languages=None, fallback=False):
    if fallback:
        return NullTranslations()
    raise FileNotFoundError(2, "No translation file found for domain", domain)


def install(domain, localedir=None, names=None):
    NullTranslations().install(names)


def textdomain(domain=None):
    return domain or "messages"


def bindtextdomain(domain, localedir=None):
    return localedir


def gettext(message):
    return message


def dgettext(domain, message):
    return message


def ngettext(msgid1, msgid2, n):
    return msgid1 if n == 1 else msgid2


def dngettext(domain, msgid1, msgid2, n):
    return msgid1 if n == 1 else msgid2


def pgettext(context, message):
    return message


def npgettext(context, msgid1, msgid2, n):
    return msgid1 if n == 1 else msgid2
