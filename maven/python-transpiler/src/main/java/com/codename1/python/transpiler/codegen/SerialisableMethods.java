/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.transpiler.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The Python [DeclarativeMap] writes into one openpyxl model class: the
/// metaclass's tuples as literals, and `Serialisable`'s generic methods
/// unrolled over them.
///
/// Each method is `openpyxl/descriptors/serialisable.py`'s own, statement
/// for statement, with its loop over `__attrs__` or `__elements__`
/// replaced by one block per name. A field is then read as `self.name`,
/// which the inference can type, where the generic method's
/// `getattr(self, attr)` names no field at all. What the descriptor kind
/// settles is decided here: whether a child is a `NestedSequence`, a
/// `Sequence` or nested.
public final class SerialisableMethods {
    /// The module holding what the generated methods share, once: how a
    /// child element is read through the descriptor that names it. The
    /// descriptor is handed in, read by name in the class's own
    /// `from_tree` (one branch per descriptor the class and its bases
    /// declare), and `__copy__` names each instance key it carries over, so
    /// nothing here looks a name up.
    public static final String MODULE_PATH = "openpyxl/_dm_serialisable.py";
    private static final String MODULE = "from openpyxl.descriptors.serialisable import KEYWORDS, localname, Sequence, "
            + "NestedSequence, MultiSequencePart, safe_string, Element, namespaced, copy, seq_types, Descriptor\n"
            + "def child(desc, tag, el, attrib):\n"
            + "    if hasattr(desc, 'from_tree'):\n        obj = desc.from_tree(el)\n    else:\n"
            + "        if hasattr(desc.expected_type, \"from_tree\"):\n            obj = desc.expected_type.from_tree(el)\n"
            + "        else:\n            obj = el.text\n    if isinstance(desc, NestedSequence):\n"
            + "        attrib[tag] = obj\n    elif isinstance(desc, Sequence):\n        attrib.setdefault(tag, [])\n"
            + "        attrib[tag].append(obj)\n    elif isinstance(desc, MultiSequencePart):\n"
            + "        attrib.setdefault(desc.store, [])\n        attrib[desc.store].append(obj)\n    else:\n"
            + "        attrib[tag] = obj\n"
            + "def text(v):\n    if isinstance(v, str):\n        return v\n    return None\n";
    /// The source of the module at [#MODULE_PATH].
    public static String moduleSource() {
        return MODULE;
    }

    /// The names the generated methods use, imported under names no model
    /// module binds.
    static final String HELPERS = "from openpyxl._dm_serialisable import KEYWORDS as _dm_KEYWORDS, "
            + "localname as _dm_localname, Sequence as _dm_Sequence, NestedSequence as _dm_NestedSequence, "
            + "safe_string as _dm_safe_string, Element as _dm_Element, namespaced as _dm_namespaced, "
            + "seq_types as _dm_seq_types, child as _dm_child, copy as _dm_copy, text as _dm_text, "
            + "Descriptor as _dm_Descriptor\n";
    /// Each generic method of `Serialisable` and the method that calls the
    /// per-class `_dm_` version in its place. A class whose lineage binds a
    /// method of its own gets the generated one under the `_dm_` name, and
    /// `super().name(...)` from that lineage reaches it through these.
    static final Map<String, String> DISPATCH = new LinkedHashMap<String, String>();

    static {
        DISPATCH.put("from_tree", "@classmethod\ndef from_tree(cls, node):\n    return cls._dm_from_tree(node)\n");
        DISPATCH.put("to_tree", "def to_tree(self, tagname=None, idx=None, namespace=None):\n"
                + "    return self._dm_to_tree(tagname, idx, namespace)\n");
        DISPATCH.put("__iter__", "def __iter__(self):\n    return self._dm___iter__()\n");
        DISPATCH.put("__eq__", "def __eq__(self, other):\n    return self._dm___eq__(other)\n");
        DISPATCH.put("__repr__", "def __repr__(self):\n    return self._dm___repr__()\n");
        DISPATCH.put("__hash__", "def __hash__(self):\n    return self._dm___hash__()\n");
        DISPATCH.put("__add__", "def __add__(self, other):\n    return self._dm___add__(other)\n");
        DISPATCH.put("__copy__", "def __copy__(self):\n    return self._dm___copy__()\n");
    }

    /// What replaces `Serialisable`'s generic methods: the dispatchers,
    /// and the `_dm_` methods every model class overrides.
    static String baseSource() {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : DISPATCH.entrySet()) {
            b.append(e.getValue());
            String d = e.getValue().replaceFirst("def " + e.getKey() + "\\(", "def _dm_" + e.getKey() + "(");
            b.append(d.substring(0, d.indexOf("    return ")))
                    .append("    raise NotImplementedError(\"generated per class\")\n");
        }
        return b.toString();
    }

    /// `src`, one generated method, under the name `name` takes in a class
    /// whose lineage binds `custom` and whose subclasses bind `below`; with
    /// the dispatcher when only a subclass binds it.
    private static String place(String name, String src, Set<String> custom, Set<String> below) {
        if (!custom.contains(name) && !below.contains(name)) {
            return src;
        }
        String renamed = src.replaceFirst("(?m)^def " + name + "\\(", "def _dm_" + name + "(");
        return custom.contains(name) ? renamed : renamed + DISPATCH.get(name);
    }
    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList("False", "None", "True", "and",
            "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except",
            "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or",
            "pass", "raise", "return", "try", "while", "with", "yield"));

    private SerialisableMethods() {
    }

    /// `obj.name`, or a constant-name `getattr` where `name` is not one.
    private static String read(String obj, String name) {
        boolean ident = !name.isEmpty() && !KEYWORDS.contains(name) && !name.startsWith("__")
                && (Character.isLetter(name.charAt(0)) || name.charAt(0) == '_');
        for (int i = 0; i < name.length() && ident; i++) {
            char ch = name.charAt(i);
            ident = ch < 0x80 && (Character.isLetterOrDigit(ch) || ch == '_');
        }
        return ident ? obj + "." + name : "getattr(" + obj + ", " + DeclarativeMap.quote(name) + ")";
    }

    private static String tuple(List<String> names) {
        StringBuilder b = new StringBuilder("(");
        for (String n : names) {
            b.append(DeclarativeMap.quote(n)).append(", ");
        }
        return b.append(')').toString();
    }

    /// The class-body source; `custom` names what the class or a program base
    /// below `Serialisable` binds, `below` what a program subclass binds.
    @SuppressWarnings("checkstyle:ParameterNumber")
    static String methods(List<String> attrs, List<String> elements, List<String> nested, List<String[]> namespaced,
            Map<String, String> kinds, Set<String> written, Set<String> lowered, List<String> aliases,
            Set<String> custom, Set<String> below, List<String[]> init, Map<String, Boolean> descs,
            List<String> copied) {
        StringBuilder b = new StringBuilder();
        if (written.contains("__attrs__")) {
            b.append("__attrs__ = ").append(tuple(attrs)).append('\n');
        }
        if (written.contains("__elements__")) {
            b.append("__elements__ = ").append(tuple(elements)).append('\n');
        }
        if (written.contains("__nested__")) {
            b.append("__nested__ = ").append(tuple(nested)).append('\n');
        }
        StringBuilder ns = new StringBuilder("(");
        for (String[] p : namespaced) {
            ns.append('(').append(DeclarativeMap.quote(p[0])).append(", ").append(DeclarativeMap.quote(p[1]))
                    .append("), ");
        }
        ns.append(')');
        b.append("__namespaced__ = ").append(ns).append('\n');
        StringBuilder m = new StringBuilder();
        iter(m, attrs, lowered);
        b.append(place("__iter__", m.toString(), custom, below));
        m.setLength(0);
        toTree(m, attrs, elements, nested, namespaced, kinds);
        b.append(place("to_tree", m.toString(), custom, below));
        m.setLength(0);
        fromTree(m, attrs, namespaced, lowered, aliases, init, descs);
        b.append(place("from_tree", m.toString(), custom, below));
        m.setLength(0);
        {
            m.append("def __eq__(self, other):\n    if not self.__class__ == other.__class__:\n        return False\n"
                    + "    elif not dict(self) == dict(other):\n        return False\n");
            for (String e : elements) {
                m.append("    if ").append(read("self", e)).append(" != ").append(read("other", e))
                        .append(":\n        return False\n");
            }
            m.append("    return True\n");
            b.append(place("__eq__", m.toString(), custom, below));
            m.setLength(0);
        }
        {
            m.append("def __repr__(self):\n    s = u\"<{0}.{1} object>\\nParameters:\".format(self.__module__, "
                    + "self.__class__.__name__)\n    args = []\n");
            List<String> all = new ArrayList<String>(attrs);
            all.addAll(elements);
            for (String k : all) {
                m.append("    v = ").append(read("self", k)).append("\n    if isinstance(v, _dm_Descriptor):\n"
                        + "        v = None\n    args.append(u\"{0}={1}\".format(").append(DeclarativeMap.quote(k))
                        .append(", repr(v)))\n");
            }
            m.append("    return u\"\\n\".join([s, u\", \".join(args)])\n");
            b.append(place("__repr__", m.toString(), custom, below));
            m.setLength(0);
        }
        // A class that defines __eq__ alone has its __hash__ set to None by
        // Python; adding one would make it hashable. Such a lineage gets
        // the generated one under the `_dm_` name, which nothing calls.
        if (custom.contains("__eq__") && !custom.contains("__hash__")) {
            custom = new HashSet<String>(custom);
            custom.add("__hash__");
        }
        {
            List<String> all = new ArrayList<String>(attrs);
            all.addAll(elements);
            m.append("def __hash__(self):\n");
            for (int i = 0; i < all.size(); i++) {
                m.append("    f").append(i).append(" = ").append(read("self", all.get(i))).append("\n    if isinstance(f")
                        .append(i).append(", list):\n        f").append(i).append(" = tuple(f").append(i).append(")\n");
            }
            m.append("    return hash((");
            for (int i = 0; i < all.size(); i++) {
                m.append('f').append(i).append(", ");
            }
            m.append("))\n");
            b.append(place("__hash__", m.toString(), custom, below));
            m.setLength(0);
        }
        {
            m.append("def __add__(self, other):\n    if type(self) != type(other):\n"
                    + "        raise TypeError(\"Cannot combine instances of different types\")\n    vals = {}\n");
            for (String a : attrs) {
                m.append("    vals[").append(DeclarativeMap.quote(a)).append("] = ").append(read("self", a))
                        .append(" or ").append(read("other", a)).append('\n');
            }
            for (String e : elements) {
                String q = DeclarativeMap.quote(e);
                m.append("    a = ").append(read("self", e)).append("\n    b = ").append(read("other", e))
                        .append("\n    if a and b:\n        vals[").append(q).append("] = a + b\n    else:\n        vals[")
                        .append(q).append("] = a or b\n");
            }
            m.append("    return self.__class__(**vals)\n");
            b.append(place("__add__", m.toString(), custom, below));
            m.setLength(0);
        }
        {
            // The persisted names, plus the slots a lowered field keeps its
            // value in, which the descriptor kept under the field's name.
            List<String> skip = new ArrayList<String>(attrs);
            skip.addAll(elements);
            for (String a : new ArrayList<String>(skip)) {
                if (lowered.contains(a)) {
                    skip.add("_dm_" + a);
                }
            }
            // Each key the instance dictionary may hold, by name; the
            // generic method walked the dictionary with getattr/setattr.
            m.append("def __copy__(self):\n    xml = self.to_tree(tagname=\"dummy\")\n"
                    + "    cp = self.__class__.from_tree(xml)\n    d = self.__dict__\n");
            for (String k : copied) {
                if (!skip.contains(k)) {
                    String q = DeclarativeMap.quote(k);
                    String target = read("cp", k);
                    String v = "_dm_copy(" + read("self", k) + ")";
                    m.append("    if ").append(q).append(" in d:\n        ")
                            .append(target.startsWith("getattr(") ? "setattr(cp, " + q + ", " + v + ")"
                                    : target + " = " + v).append('\n');
                }
            }
            m.append("    return cp\n");
            b.append(place("__copy__", m.toString(), custom, below));
        }
        return b.toString();
    }

    private static void iter(StringBuilder b, List<String> attrs, Set<String> lowered) {
        b.append("def __iter__(self):\n");
        if (attrs.isEmpty()) {
            b.append("    return\n    yield\n");
            return;
        }
        for (String attr : attrs) {
            b.append("    value = ").append(read("self", attr)).append('\n');
            if ("attr_text".equals(attr)) {
                continue;
            }
            String key = DeclarativeMap.quote(attr.startsWith("_") ? attr.substring(1) : attr);
            if (!attr.startsWith("_") && attr.indexOf('_') >= 0) {
                if (lowered.contains(attr)) {
                    // A lowered descriptor took no `hyphenated` keyword.
                    key = DeclarativeMap.quote(attr);
                } else {
                    b.append("    key = ").append(DeclarativeMap.quote(attr)).append("\n    if getattr(getattr(self.__class__, ")
                            .append(DeclarativeMap.quote(attr)).append("), \"hyphenated\", False):\n        key = ")
                            .append(DeclarativeMap.quote(attr.replace('_', '-'))).append('\n');
                    key = "key";
                }
            }
            b.append("    if value is not None:\n        yield ").append(key).append(", _dm_safe_string(value)\n");
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void toTree(StringBuilder b, List<String> attrs, List<String> elements, List<String> nested,
            List<String[]> namespaced, Map<String, String> kinds) {
        b.append("def to_tree(self, tagname=None, idx=None, namespace=None):\n    if tagname is None:\n"
                + "        tagname = self.tagname\n    if tagname.startswith(\"_\"):\n        tagname = tagname[1:]\n"
                + "    tagname = _dm_namespaced(self, tagname, namespace)\n"
                + "    namespace = getattr(self, \"namespace\", namespace)\n    attrs = dict(self)\n");
        for (String[] p : namespaced) {
            String k = DeclarativeMap.quote(p[0]);
            b.append("    if ").append(k).append(" in attrs:\n        attrs[").append(DeclarativeMap.quote(p[1]))
                    .append("] = attrs[").append(k).append("]\n        del attrs[").append(k).append("]\n");
        }
        b.append("    el = _dm_Element(tagname, attrs)\n");
        if (attrs.contains("attr_text")) {
            b.append("    el.text = _dm_safe_string(self.attr_text)\n");
        }
        for (String e : elements) {
            String q = DeclarativeMap.quote(e);
            String kind = kinds.get(e);
            b.append("    desc = getattr(self.__class__, ").append(q).append(", None)\n    obj = ").append(read("self", e))
                    .append("\n    if hasattr(desc, \"namespace\") and hasattr(obj, 'namespace'):\n"
                    + "        obj.namespace = desc.namespace\n    if isinstance(obj, _dm_seq_types):\n");
            String nestedSeq = "        if obj:\n            el.append(desc.to_tree(" + q + ", obj, namespace))\n";
            String seq = "        desc.idx_base = self.idx_base\n        for node in desc.to_tree(" + q
                    + ", obj, namespace):\n            el.append(node)\n";
            String other = "        for v in obj:\n            el.append(v.to_tree(" + q + ", namespace))\n";
            if ("nestedseq".equals(kind)) {
                b.append(nestedSeq);
            } else if ("seq".equals(kind)) {
                b.append(seq);
            } else if ("other".equals(kind)) {
                b.append(other);
            } else {
                // A name the class dictionary does not declare: decide at
                // run time, as the generic method does.
                b.append("        if isinstance(desc, _dm_NestedSequence):\n").append(indent(nestedSeq))
                        .append("        elif isinstance(desc, _dm_Sequence):\n").append(indent(seq))
                        .append("        else:\n").append(indent(other));
            }
            b.append("    else:\n");
            if (nested.contains(e)) {
                b.append("        node = desc.to_tree(").append(q).append(", obj, namespace)\n"
                        + "        if node is not None:\n            el.append(node)\n");
            } else {
                b.append("        if obj is not None:\n            node = obj.to_tree(").append(q)
                        .append(")\n            if node is not None:\n                el.append(node)\n");
            }
        }
        b.append("    return el\n");
    }

    private static String indent(String s) {
        return s.replaceAll("(?m)^(?=.)", "    ");
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void fromTree(StringBuilder b, List<String> attrs, List<String[]> namespaced, Set<String> lowered,
            List<String> aliases, List<String[]> init, Map<String, Boolean> descs) {
        b.append("@classmethod\ndef from_tree(cls, node):\n    attrib = dict(node.attrib)\n");
        for (String[] p : namespaced) {
            String ns = DeclarativeMap.quote(p[1]);
            b.append("    if ").append(ns).append(" in attrib:\n        attrib[").append(DeclarativeMap.quote(p[0]))
                    .append("] = attrib[").append(ns).append("]\n        del attrib[").append(ns).append("]\n");
        }
        b.append("    for key in list(attrib):\n        if key.startswith('{'):\n            del attrib[key]\n"
                + "        elif key in _dm_KEYWORDS:\n            attrib[\"_\" + key] = attrib[key]\n"
                + "            del attrib[key]\n        elif \"-\" in key:\n            n = key.replace(\"-\", \"_\")\n"
                + "            attrib[n] = attrib[key]\n            del attrib[key]\n");
        if (attrs.contains("attr_text")) {
            b.append("    if node.text:\n        attrib[\"attr_text\"] = node.text\n");
        }
        b.append("    for el in node:\n        tag = _dm_localname(el)\n        if tag in _dm_KEYWORDS:\n"
                + "            tag = \"_\" + tag\n");
        // A lowered field was a descriptor with no from_tree whose type has
        // none either: the generic method stored the child's text.
        List<String> plain = new ArrayList<String>(lowered);
        java.util.Collections.sort(plain);
        for (String f : plain) {
            b.append("        ").append("if tag == ").append(DeclarativeMap.quote(f))
                    .append(":\n            attrib[tag] = el.text\n            continue\n");
        }
        for (String a : aliases) {
            // An Alias has no expected_type: the generic method failed here.
            b.append("        if tag == ").append(DeclarativeMap.quote(a))
                    .append(":\n            raise AttributeError(\"'Alias' object has no attribute 'expected_type'\")\n");
        }
        // The descriptor a child tag names, read by name: an Alias (whose
        // class read fails, which the generic getattr answered with None)
        // and any other tag are skipped, as the generic method skipped them.
        String kw = "if";
        for (Map.Entry<String, Boolean> e : descs.entrySet()) {
            if (e.getValue() || lowered.contains(e.getKey()) || aliases.contains(e.getKey())) {
                continue;
            }
            b.append("        ").append(kw).append(" tag == ").append(DeclarativeMap.quote(e.getKey()))
                    .append(":\n            _dm_child(").append(read("cls", e.getKey())).append(", tag, el, attrib)\n");
            kw = "elif";
        }
        if (init == null) {
            b.append("    return cls(**attrib)\n");
            return;
        }
        // The call by keywords, one per parameter, wherever it binds what
        // cls(**attrib) would; any other dictionary (an unknown name, a
        // missing required one) still takes that call, and its TypeError.
        List<String> names = new ArrayList<String>();
        for (String[] p : init) {
            names.add(p[0]);
            if (p[1] == null) {
                b.append("    if ").append(DeclarativeMap.quote(p[0])).append(" not in attrib:\n        return cls(**attrib)\n");
            }
        }
        b.append("    for k in attrib:\n        if k not in ").append(tuple(names)).append(":\n")
                .append("            return cls(**attrib)\n    return cls(");
        for (String[] p : init) {
            String q = DeclarativeMap.quote(p[0]);
            // A lowered field's entry is attribute or child text: str or None.
            String v = lowered.contains(p[0]) ? "_dm_text(attrib[" + q + "])" : "attrib[" + q + "]";
            b.append(p[0]).append('=').append(p[1] == null ? v : "(" + v + " if " + q + " in attrib else " + p[1] + ")")
                    .append(", ");
        }
        b.append(")\n");
    }
}
