# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/dom/domreg.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.dom.domreg: the DOM implementation registry. minidom is the only
implementation; one registered by name is called, never imported."""

well_known_implementations = {"minidom": "xml.dom.minidom"}

registered = {}


def registerDOMImplementation(name, factory):
    registered[name] = factory


def _good_enough(dom, features):
    for f, v in features:
        if not dom.hasFeature(f, v):
            return 0
    return 1


def _minidom():
    from xml.dom import minidom
    return minidom.getDOMImplementation()


def getDOMImplementation(name=None, features=()):
    if name:
        if name in registered:
            return registered[name]()
        if name == "minidom":
            return _minidom()
        raise ImportError("no registered DOM implementation named " + name)
    if isinstance(features, str):
        features = _parse_feature_string(features)
    for creator in registered.values():
        dom = creator()
        if _good_enough(dom, features):
            return dom
    dom = _minidom()
    if _good_enough(dom, features):
        return dom
    raise ImportError("no suitable DOM implementation found")


def _parse_feature_string(s):
    features = []
    parts = s.split()
    i = 0
    length = len(parts)
    while i < length:
        feature = parts[i]
        if feature[0] in "0123456789":
            raise ValueError("bad feature name: %r" % (feature,))
        i = i + 1
        version = None
        if i < length:
            v = parts[i]
            if v[0] in "0123456789":
                i = i + 1
                version = v
        features.append((feature, version))
    return tuple(features)
