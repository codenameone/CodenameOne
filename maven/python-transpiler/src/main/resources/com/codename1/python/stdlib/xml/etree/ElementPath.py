# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/etree/ElementPath.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
#
#
# ElementTree
# $Id: ElementPath.py 3375 2008-02-13 08:05:08Z fredrik $
#
# limited xpath support for element trees
#
# history:
# 2003-05-23 fl   created
# 2003-05-28 fl   added support for // etc
# 2003-08-27 fl   fixed parsing of periods in element names
# 2007-09-10 fl   new selection engine
# 2007-09-12 fl   fixed parent selector
# 2007-09-13 fl   added iterfind; changed findall to return a list
# 2007-11-30 fl   added namespaces support
# 2009-10-30 fl   added child element value filter
#
# Copyright (c) 2003-2009 by Fredrik Lundh.  All rights reserved.
#
# fredrik@pythonware.com
# http://www.pythonware.com
#
# --------------------------------------------------------------------
# The ElementTree toolkit is
#
# Copyright (c) 1999-2009 by Fredrik Lundh
#
# By obtaining, using, and/or copying this software and/or its
# associated documentation, you agree that you have read, understood,
# and will comply with the following terms and conditions:
#
# Permission to use, copy, modify, and distribute this software and
# its associated documentation for any purpose and without fee is
# hereby granted, provided that the above copyright notice appears in
# all copies, and that both that copyright notice and this permission
# notice appear in supporting documentation, and that the name of
# Secret Labs AB or the author not be used in advertising or publicity
# pertaining to distribution of the software without specific, written
# prior permission.
#
# SECRET LABS AB AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD
# TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANT-
# ABILITY AND FITNESS.  IN NO EVENT SHALL SECRET LABS AB OR THE AUTHOR
# BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY
# DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,

"""xml.etree.ElementPath: the XPath subset of Element.find and friends.

Steps are tags (with {uri}, {*}, {} and prefix:tag forms), *, ., .., //,
and the predicates [@a], [@a='v'], [@a!='v'], [tag], [tag='text'],
[tag!='text'], [.='text'], [.!='text'], [n], [last()] and [last()-n].
"""

import re as _re

_TOKENS = _re.compile(r"('[^']*'|\"[^\"]*\"|::|//?|\.\.|\(\)|!=|[/.*:\[\]\(\)@=])|"
                      r"((?:\{[^}]+\})?[^/\[\]\(\)@!=\s]+)|\s+")


def xpath_tokenizer(pattern, namespaces=None):
    default = namespaces.get("") if namespaces else None
    in_attribute = False
    for m in _TOKENS.finditer(pattern):
        op = m.group(1) or ""
        tag = m.group(2) or ""
        if tag and tag[0] != "{":
            if ":" in tag:
                prefix, local = tag.split(":", 1)
                try:
                    if not namespaces:
                        raise KeyError(prefix)
                    yield op, "{%s}%s" % (namespaces[prefix], local)
                except KeyError:
                    raise SyntaxError("prefix %r not found in prefix map" % prefix) from None
            elif default and not in_attribute:
                yield op, "{%s}%s" % (default, tag)
            else:
                yield op, tag
            in_attribute = False
        else:
            yield op, tag
            in_attribute = op == "@"


def _matcher(tag):
    """A test for one element's tag: exact, or one of the {*}/{}/{uri}*
    wildcards."""
    if tag == "{*}*":
        return lambda t: isinstance(t, str)
    if tag == "{}*":
        return lambda t: isinstance(t, str) and t[:1] != "{"
    if tag[:3] == "{*}":
        local = tag[3:]
        suffix = "}" + local
        return lambda t: isinstance(t, str) and (t == local or t.endswith(suffix))
    if tag[-2:] == "}*":
        ns = tag[:-1]
        return lambda t: isinstance(t, str) and t.startswith(ns)
    if tag[:2] == "{}":
        local = tag[2:]
        return lambda t: t == local
    return None


def _parent_map(context):
    pm = context.parent_map
    if pm is None:
        pm = {}
        for p in context.root.iter():
            for c in p:
                pm[c] = p
        context.parent_map = pm
    return pm


def _child(nxt, token):
    tag = token[1]
    test = _matcher(tag)
    if test is not None:
        def select(context, result):
            for elem in result:
                for e in elem:
                    if test(e.tag):
                        yield e
        return select

    def select(context, result):
        for elem in result:
            for e in elem:
                if e.tag == tag:
                    yield e
    return select


def _star(nxt, token):
    def select(context, result):
        for elem in result:
            yield from elem
    return select


def _self(nxt, token):
    def select(context, result):
        yield from result
    return select


def _descendant(nxt, token):
    try:
        token = nxt()
    except StopIteration:
        return None
    if token[0] == "*":
        tag = "*"
    elif not token[0]:
        tag = token[1]
    else:
        raise SyntaxError("invalid descendant")
    test = _matcher(tag)
    if test is not None:
        def select(context, result):
            for elem in result:
                for e in elem.iter():
                    if e is not elem and test(e.tag):
                        yield e
        return select

    def select(context, result):
        for elem in result:
            for e in elem.iter(tag):
                if e is not elem:
                    yield e
    return select


def _parent(nxt, token):
    def select(context, result):
        pm = _parent_map(context)
        seen = set()
        for elem in result:
            p = pm.get(elem)
            if p is not None and id(p) not in seen:
                seen.add(id(p))
                yield p
    return select


def _predicate(nxt, token):
    signature = []
    values = []
    while True:
        try:
            token = nxt()
        except StopIteration:
            return None
        if token[0] == "]":
            break
        if token == ("", ""):
            continue
        if token[0] and token[0][:1] in "'\"":
            token = ("'", token[0][1:-1])
        signature.append(token[0] or "-")
        values.append(token[1])
    sig = "".join(signature)
    if sig == "@-":
        key = values[1]

        def select(context, result):
            for elem in result:
                if elem.get(key) is not None:
                    yield elem
        return select
    if sig == "@-='" or sig == "@-!='":
        key = values[1]
        value = values[-1]
        equal = sig == "@-='"

        def select(context, result):
            for elem in result:
                if (elem.get(key) == value) == equal:
                    yield elem
        return select
    if sig == "-" and not _re.match(r"\-?\d+$", values[0]):
        tag = values[0]

        def select(context, result):
            for elem in result:
                if elem.find(tag) is not None:
                    yield elem
        return select
    if sig in ("-='", "-!='", ".='", ".!='"):
        tag = values[0]
        value = values[-1]
        equal = "!" not in sig
        if sig[0] == ".":
            def select(context, result):
                for elem in result:
                    if ("".join(elem.itertext()) == value) == equal:
                        yield elem
            return select

        def select(context, result):
            for elem in result:
                for e in elem.findall(tag):
                    if ("".join(e.itertext()) == value) == equal:
                        yield elem
                        break
        return select
    if sig in ("-", "-()", "-()-"):
        if sig == "-":
            index = int(values[0]) - 1
            if index < 0:
                raise SyntaxError("XPath position >= 1 expected")
        else:
            if values[0] != "last":
                raise SyntaxError("unsupported function")
            if sig == "-()-":
                try:
                    index = int(values[2]) - 1
                except ValueError:
                    raise SyntaxError("unsupported expression")
                if index > -2:
                    raise SyntaxError("XPath offset from last() must be negative")
            else:
                index = -1

        def select(context, result):
            pm = _parent_map(context)
            for elem in result:
                p = pm.get(elem)
                if p is None:
                    continue
                elems = list(p.findall(elem.tag))
                try:
                    if elems[index] is elem:
                        yield elem
                except IndexError:
                    pass
        return select
    raise SyntaxError("invalid predicate")


_OPS = {"": _child, "*": _star, ".": _self, "..": _parent, "//": _descendant, "[": _predicate}

_cache = {}


class _SelectorContext:
    def __init__(self, root):
        self.root = root
        self.parent_map = None


def _compile(path, namespaces):
    key = (path, None if namespaces is None else tuple(sorted(namespaces.items())))
    sel = _cache.get(key)
    if sel is not None:
        return sel
    if len(_cache) > 100:
        _cache.clear()
    if path[:1] == "/":
        raise SyntaxError("cannot use absolute path on element")
    it = iter(xpath_tokenizer(path, namespaces))

    def nxt():
        return next(it)

    sel = []
    try:
        token = nxt()
    except StopIteration:
        raise SyntaxError("invalid path")
    while True:
        op = _OPS.get(token[0])
        if op is None:
            raise SyntaxError("invalid path")
        s = op(nxt, token)
        if s is None:
            raise SyntaxError("invalid path")
        sel.append(s)
        try:
            token = nxt()
            if token[0] == "/":
                token = nxt()
        except StopIteration:
            break
    _cache[key] = sel
    return sel


def iterfind(elem, path, namespaces=None):
    if path[-1:] == "/":
        path = path + "*"
    selector = _compile(path, namespaces)
    result = iter([elem])
    context = _SelectorContext(elem)
    for s in selector:
        result = s(context, result)
    return result


def find(elem, path, namespaces=None):
    return next(iterfind(elem, path, namespaces), None)


def findall(elem, path, namespaces=None):
    return list(iterfind(elem, path, namespaces))


def findtext(elem, path, default=None, namespaces=None):
    e = find(elem, path, namespaces)
    if e is None:
        return default
    return e.text or ""
