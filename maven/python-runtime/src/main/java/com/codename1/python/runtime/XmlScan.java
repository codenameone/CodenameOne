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
package com.codename1.python.runtime;

/// `_cn1xml`: the fast path of the bundled expat's tokenizer.
///
/// The tokenizer matches every tag with a regular expression. These scan the
/// shapes nearly every document consists of -- ASCII names, quoted attribute
/// values without references -- by hand, and answer `None` for anything else,
/// which the tokenizer then hands to its expressions exactly as before. So a
/// scan never decides a malformed or unusual tag: it either agrees with the
/// expression or declines.
final class XmlScan {
    private XmlScan() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1xml", new String[0]);
        m.setAttr("scan_start", PyBuiltin.fixed2("scan_start", (a0, a1) -> scanStart(Typed.asStr(a0), (int) Typed.toLong(a1))));
        m.setAttr("run", PyBuiltin.fixed3("run", (a0, a1, a2) -> Long.valueOf(ExpatRun.run(a0, Typed.asStr(a1), (int) Typed.toLong(a2)))));
        m.setAttr("iter", PyBuiltin.fixed2("iter", (a0, a1) -> new ElementIter(a0, a1)));
        m.setAttr("find", PyBuiltin.fixed2("find", (a0, a1) -> children(a0, a1, false)));
        m.setAttr("findall", PyBuiltin.fixed2("findall", (a0, a1) -> children(a0, a1, true)));
        m.setAttr("build", PyBuiltin.positional("build",
                a -> new Builder(Typed.asStr(a[0]), a[1], Py.isTrue(a[2]), a.length > 3 ? a[3] : null).build()));
        m.setAttr("lxml_build", PyBuiltin.positional("lxml_build",
                a -> new Builder(Typed.asStr(a[0]), Py.isTrue(a[1]), new Lxml(a)).build()));
        m.setAttr("scan_end", PyBuiltin.fixed2("scan_end", (a0, a1) -> scanEnd(Typed.asStr(a0), (int) Typed.toLong(a1))));
        return m;
    }

    /// A character the expat module's `_NAME` class accepts, restricted to
    /// ASCII: beyond it the class depends on what `\s` means, so decline.
    private static boolean nameChar(char c) {
        if (c <= ' ' || c >= 0x7f) {
            return false;
        }
        switch (c) {
            case '<': case '>': case '/': case '=': case '"': case '\'': case '!': case '?':
            case '&': case ';': case '%': case '(': case ')': case '[': case ']': case ',': case '|':
                return false;
            default:
                return true;
        }
    }

    /// Whitespace both scans skip: the ASCII members of `\s`. Its other
    /// members (\x0b, \x0c, \x1c-\x1f and non-ASCII spaces) make a scan
    /// decline, through [#nameChar] answering false for them.
    static boolean space(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    static int name(String s, int i, int n) {
        while (i < n && nameChar(s.charAt(i))) {
            i++;
        }
        return i;
    }

    /// `<name attr="v" ...>` at `pos`: `(end, name, [n0, v0, n1, v1 ...],
    /// selfClosing)`, values already normalised as `_attr_value` would (tab,
    /// newline and CR to a space). `None` for a duplicate attribute, a value
    /// with `<` or `&`, or any shape off the common path.
    static Object scanStart(String s, int pos) {
        int n = s.length();
        int i = pos + 1;
        int e = name(s, i, n);
        if (e == i) {
            return Py.NONE;
        }
        String qname = s.substring(i, e);
        PyList attrs = Py.list();
        i = e;
        while (true) {
            int ws = i;
            while (i < n && space(s.charAt(i))) {
                i++;
            }
            if (i >= n) {
                return Py.NONE;
            }
            char c = s.charAt(i);
            if (c == '>' || c == '/') {
                boolean self = c == '/';
                if (self) {
                    if (i + 1 >= n || s.charAt(i + 1) != '>') {
                        return Py.NONE;
                    }
                    i++;
                }
                return Py.tuple(Long.valueOf(i + 1), qname, attrs, Py.bool(self));
            }
            if (i == ws) {
                return Py.NONE;
            }
            int ne = name(s, i, n);
            if (ne == i) {
                return Py.NONE;
            }
            String an = s.substring(i, ne);
            i = ne;
            while (i < n && space(s.charAt(i))) {
                i++;
            }
            if (i >= n || s.charAt(i) != '=') {
                return Py.NONE;
            }
            i++;
            while (i < n && space(s.charAt(i))) {
                i++;
            }
            if (i >= n) {
                return Py.NONE;
            }
            char q = s.charAt(i);
            if (q != '"' && q != '\'') {
                return Py.NONE;
            }
            int vs = i + 1;
            int ve = s.indexOf(q, vs);
            if (ve < 0) {
                return Py.NONE;
            }
            boolean norm = false;
            for (int k = vs; k < ve; k++) {
                char v = s.charAt(k);
                if (v == '<' || v == '&') {
                    return Py.NONE;
                }
                if (v == '\t' || v == '\n' || v == '\r') {
                    norm = true;
                }
            }
            for (int k = 0; k < attrs.size(); k += 2) {
                if (an.equals(attrs.get(k))) {
                    return Py.NONE;
                }
            }
            String val = s.substring(vs, ve);
            if (norm) {
                val = val.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
            }
            attrs.append(an);
            attrs.append(val);
            i = ve + 1;
        }
    }

    /// `</name>` at `pos`: `(end, name)`, or `None` off the common path.
    static Object scanEnd(String s, int pos) {
        int n = s.length();
        int i = pos + 2;
        int e = name(s, i, n);
        if (e == i) {
            return Py.NONE;
        }
        int j = e;
        while (j < n && space(s.charAt(j))) {
            j++;
        }
        if (j >= n || s.charAt(j) != '>') {
            return Py.NONE;
        }
        return Py.tuple(Long.valueOf(j + 1), s.substring(i, e));
    }

    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
    private static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";

    /// Thrown inside [Builder] to decline: the document is not one of the
    /// shapes the fast build reproduces exactly, so the Python parser runs.
    private static final class Decline extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Decline() {
            super();
        }
    }

    private static final Decline DECLINE = new Decline();

    /// `ElementTree.XML(text)` with the default parser and tree builder,
    /// done in one pass in Java, as CPython does it in C: per element the
    /// Python path ran the tokenizer, `_start_tail`, `XMLParser._start`,
    /// `_fixname`, `TreeBuilder.start` and `Element.__init__`. Elements are
    /// still made by calling the factory, so they are the module's own
    /// `Element`. It accepts elements, attributes, namespaces, the five
    /// predefined and character references, comments and an XML
    /// declaration, and answers `None` for anything else (a DOCTYPE, CDATA,
    /// a processing instruction, any error), for the Python parser to
    /// handle -- and to report exactly as it does.
    private static final AttrSite CHILDREN = new AttrSite("_children");
    private static final StoreSite SET_CHILDREN = new StoreSite("_children");
    private static final StoreSite SET_ATTRIB = new StoreSite("attrib");
    /// Slots in a builder's tag cache, a power of two.
    private static final int TAG_CACHE = 64;

    private static final AttrSite TAG = new AttrSite("tag");
    private static final StoreSite SET_TAG = new StoreSite("tag");

    /// Whether ElementPath would read `path` as one child tag: `tag` or
    /// `{uri}tag`, with none of the characters a path expression uses.
    static boolean plainTag(String path) {
        int n = path.length();
        int start = 0;
        if (n > 0 && path.charAt(0) == '{') {
            int close = path.indexOf('}');
            if (close <= 1) {
                return false;
            }
            start = close + 1;
        }
        if (start >= n || path.charAt(start) == '{') {
            return false;
        }
        for (int k = start; k < n; k++) {
            char c = path.charAt(k);
            if (c < 128 && PATH_SPECIAL[c]) {
                return false;
            }
        }
        return true;
    }

    private static final boolean[] PATH_SPECIAL = new boolean[128];

    static {
        String special = "/[]*.@()=:'\" \t\n\r";
        for (int k = 0; k < special.length(); k++) {
            PATH_SPECIAL[special.charAt(k)] = true;
        }
    }

    /// `find` (the first) or `findall` (a list) of the children of `self`
    /// whose tag equals `path`, NotImplemented when `path` is not one tag.
    static Object children(Object self, Object path, boolean all) {
        if (!(path instanceof String) || !plainTag((String) path)) {
            return Py.NOT_IMPLEMENTED;
        }
        Object kids = CHILDREN.get(self);
        PyList out = all ? new PyList() : null;
        PyList ls = kids instanceof PyList ? (PyList) kids : null;
        PyObject it = ls != null ? null : Py.iter(kids);
        int k = 0;
        for (Object e; (e = it != null ? it.next() : k < ls.size ? ls.get(k++) : null) != null;) { //NOPMD AssignmentInOperand - the list or iterator walk in one loop
            if (Py.eqBool(TAG.get(e), path)) {
                if (!all) {
                    return e;
                }
                out.append(e);
            }
        }
        return all ? out : Py.NONE;
    }

    /// `Element.iter(tag)`: the element and its descendants in document
    /// order, those whose tag equals `tag` (every one for None), walked over a
    /// stack of `(children, next index)` as the Python version it replaces
    /// walks -- and as CPython's C accelerator does, in its own language.
    /// Children are read when the walk reaches them, so a tree changed
    /// between two steps is seen as the Python generator saw it.
    static final class ElementIter extends PyObject {
        private final Object root;
        private final Object tag;
        private int state;
        private Object children;
        private int i;
        /// The element last yielded, whose children the next step enters.
        private Object pending;
        private Object[] lists = new Object[8];
        private int[] marks = new int[8];
        private int depth;

        ElementIter(Object root, Object tag) {
            this.root = root;
            this.tag = tag == Py.NONE ? null : tag; //NOPMD CompareObjectsWithEquals - None is a singleton
        }

        @Override
        public PyType type() {
            return Py.ITERATOR_TYPE;
        }

        @Override
        public Object iter() {
            return this;
        }

        private boolean matches(Object e) {
            return tag == null || Py.eqBool(TAG.get(e), tag);
        }

        @Override
        public Object next() {
            if (state == 0) {
                state = 1;
                if (matches(root)) {
                    return root;
                }
            }
            if (state == 1) {
                state = 2;
                children = CHILDREN.get(root);
                i = 0;
            } else if (state == 3) {
                return null;
            }
            if (pending != null) {
                Object kids = CHILDREN.get(pending);
                pending = null;
                if (Py.isTrue(kids)) {
                    if (depth == lists.length) {
                        lists = java.util.Arrays.copyOf(lists, depth * 2);
                        marks = java.util.Arrays.copyOf(marks, depth * 2);
                    }
                    lists[depth] = children;
                    marks[depth] = i;
                    depth++;
                    children = kids;
                    i = 0;
                }
            }
            while (true) {
                Object c = children;
                int n = c instanceof PyList ? ((PyList) c).size : Py.len(c);
                if (i < n) {
                    Object e = c instanceof PyList ? ((PyList) c).get(i) : Py.getitem(c, PyInt.of(i));
                    i++;
                    if (matches(e)) {
                        pending = e;
                        return e;
                    }
                    Object kids = CHILDREN.get(e);
                    if (Py.isTrue(kids)) {
                        if (depth == lists.length) {
                            lists = java.util.Arrays.copyOf(lists, depth * 2);
                            marks = java.util.Arrays.copyOf(marks, depth * 2);
                        }
                        lists[depth] = children;
                        marks[depth] = i;
                        depth++;
                        children = kids;
                        i = 0;
                    }
                } else if (depth > 0) {
                    depth--;
                    children = lists[depth];
                    i = marks[depth];
                    lists[depth] = null;
                } else {
                    state = 3;
                    children = null;
                    return null;
                }
            }
        }
    }

    /// What `_cn1xml.lxml_build` needs of the lxml shim to build a tree as
    /// its `_XMLDriver` feeding a `_TreeSink` does: `(text, from_bytes,
    /// class_for, lookup, nohint, cnt, remove_blank_text, cache)`.
    /// `class_for(tag)` answers the element class; the shim passes it only
    /// for a lookup whose answer depends on nothing but the tag, so it is
    /// asked once per tag, and `cache` is the shim's tag -> class dict for
    /// the lookup, kept across parses while no registration changed it.
    static final class Lxml {
        final Object classFor;
        final Object lookup;
        final Object noHint;
        final Object cnt;
        final boolean removeBlankText;
        final PyDict classes;

        Lxml(Object[] a) {
            classFor = a[2];
            lookup = a[3];
            noHint = a[4];
            cnt = a[5];
            removeBlankText = Py.isTrue(a[6]);
            classes = a.length > 7 && a[7] instanceof PyDict ? (PyDict) a[7] : new PyDict();
        }

        Object classFor(String tag) {
            Object c = classes.getOrNull(tag);
            if (c == null) {
                c = Py.call(classFor, tag);
                classes.setItem(tag, c);
            }
            return c;
        }
    }

    private static final AttrSite NEW = new AttrSite("__new__");
    private static final AttrSite INIT = new AttrSite("_init");
    private static final AttrSite KIDS = new AttrSite("_xkids");
    private static final StoreSite X_KIND = new StoreSite("_xkind");
    private static final StoreSite X_TAG = new StoreSite("_xtag");
    private static final StoreSite X_ATTRIB = new StoreSite("_xattrib");
    private static final StoreSite X_NS = new StoreSite("_xns");
    private static final StoreSite X_KIDS = new StoreSite("_xkids");
    private static final StoreSite X_PARENT = new StoreSite("_xparent");
    private static final StoreSite X_TEXT = new StoreSite("_xtext");
    private static final StoreSite X_TAIL = new StoreSite("_xtail");
    private static final StoreSite X_PFX = new StoreSite("_xpfx");
    private static final StoreSite X_LOOKUP = new StoreSite("_xlookup");
    private static final StoreSite X_DOC = new StoreSite("_xdoc");
    private static final StoreSite X_TARGET = new StoreSite("_xtarget");
    private static final StoreSite X_CNT = new StoreSite("_xcnt");

    private static final class Builder {
        private final String s;
        private final int n;
        private final Object factory;
        private final boolean fromBytes;
        /// Whether the factory is a class whose `__init__` is ElementTree's own
        /// `Element.__init__`: the builder then makes its elements as that
        /// method does -- `tag`, a dict of its own as `attrib`, an empty
        /// `_children`, in that order -- giving each the dict it built.
        private final boolean direct;
        private int i;
        private final StringBuilder data = new StringBuilder(); //NOPMD AvoidStringBufferField - one parse's text, dropped with the builder
        private Object last;
        private boolean tail;
        private Object root;
        private final java.util.ArrayList<Object> open = new java.util.ArrayList<Object>();
        private final java.util.ArrayList<String> openNames = new java.util.ArrayList<String>();
        /// Namespace scopes: a map per open element that declared any, with
        /// the depth it was declared at.
        private final java.util.ArrayList<java.util.HashMap<String, String>> scopes =
                new java.util.ArrayList<java.util.HashMap<String, String>>();
        private final java.util.ArrayList<Integer> scopeDepth = new java.util.ArrayList<Integer>();
        /// Expanded element names by their qualified name, as expat's
        /// ElementTree builder keeps them: one string per distinct tag, so
        /// equal tags are one object and compare by identity. Emptied
        /// whenever a namespace scope opens or closes.
        private final String[] rawNames = new String[TAG_CACHE];
        private final String[] tagNames = new String[TAG_CACHE];
        /// Attribute names as written, one string per distinct name.
        private final String[] attrRaw = new String[TAG_CACHE];
        /// The hash of the name [#nameEnd] last scanned.
        private int nameHash;
        private final java.util.ArrayList<String> attrNames = new java.util.ArrayList<String>();
        private final java.util.ArrayList<String> attrValues = new java.util.ArrayList<String>();
        /// The lxml shim's tree, or null for ElementTree's.
        private final Lxml lx;
        /// Per open element, as the shim's driver keeps them: whether it has
        /// had a child, and whether it has kept text (bit 0 and bit 1).
        private final java.util.ArrayList<Integer> state = new java.util.ArrayList<Integer>();

        Builder(String text, boolean fromBytes, Lxml lx) {
            this(text, null, fromBytes, null, lx);
        }

        Builder(String text, Object factory, boolean fromBytes, Object elementInit) {
            this(text, factory, fromBytes, elementInit, null);
        }

        private Builder(String text, Object factory, boolean fromBytes, Object elementInit, Lxml lx) {
            this.lx = lx;
            // Nothing the build runs can change the class: no Python code runs
            // while elements are made directly.
            this.direct = elementInit != null && factory instanceof PyType
                    && ((PyType) factory).plainInit() == elementInit; //NOPMD CompareObjectsWithEquals - identity is meant
            String t = text;
            if (t.indexOf('\r') >= 0) {
                t = t.replace("\r\n", "\n").replace('\r', '\n');
            }
            if (t.length() > 0 && t.charAt(0) == '\uFEFF') {
                t = t.substring(1);
            }
            this.s = t;
            this.n = t.length();
            this.factory = factory;
            this.fromBytes = fromBytes;
            java.util.HashMap<String, String> base = new java.util.HashMap<String, String>();
            base.put("xml", XML_NS);
            scopes.add(base);
            scopeDepth.add(Integer.valueOf(-1));
        }

        Object build() {
            try {
                return run();
            } catch (Decline d) {
                return Py.NONE;
            }
        }

        private Object run() {
            if (s.startsWith("<?xml") && n > 5 && XmlScan.space(s.charAt(5))) {
                decl();
            }
            while (i < n) {
                int lt = s.indexOf('<', i);
                int end = lt < 0 ? n : lt;
                if (end > i) {
                    text(i, end);
                    i = end;
                }
                if (lt < 0) {
                    break;
                }
                char c = lt + 1 < n ? s.charAt(lt + 1) : '\0';
                if (c == '/') {
                    endTag();
                } else if (c == '!') {
                    comment();
                } else if (c == '?') {
                    throw DECLINE;
                } else {
                    startTag();
                }
            }
            if (root == null || !open.isEmpty()) {
                throw DECLINE;
            }
            return root;
        }

        private void decl() {
            int e = s.indexOf("?>");
            if (e < 0) {
                throw DECLINE;
            }
            int j = 5;
            String[] keys = {"version", "encoding", "standalone"};
            int k = 0;
            String enc = null;
            boolean first = true;
            while (true) {
                int ws = j;
                while (j < e && XmlScan.space(s.charAt(j))) {
                    j++;
                }
                if (j == e) {
                    break;
                }
                if (j == ws) {
                    throw DECLINE;
                }
                int ne = XmlScan.name(s, j, e);
                String key = s.substring(j, ne);
                while (k < keys.length && !keys[k].equals(key)) {
                    if (first) {
                        throw DECLINE;
                    }
                    k++;
                }
                if (k == keys.length) {
                    throw DECLINE;
                }
                first = false;
                j = ne;
                while (j < e && XmlScan.space(s.charAt(j))) {
                    j++;
                }
                if (j >= e || s.charAt(j) != '=') {
                    throw DECLINE;
                }
                j++;
                while (j < e && XmlScan.space(s.charAt(j))) {
                    j++;
                }
                if (j >= e) {
                    throw DECLINE;
                }
                char q = s.charAt(j);
                int ve = q == '"' || q == '\'' ? s.indexOf(q, j + 1) : -1;
                if (ve < 0 || ve > e) {
                    throw DECLINE;
                }
                String v = s.substring(j + 1, ve);
                if (k == 1) {
                    enc = v;
                } else if (k == 2 && !"yes".equals(v) && !"no".equals(v)) {
                    throw DECLINE;
                }
                k++;
                j = ve + 1;
            }
            if (first) {
                throw DECLINE;
            }
            if (fromBytes && enc != null && !"utf-8".equalsIgnoreCase(enc) && !"utf8".equalsIgnoreCase(enc)) {
                throw DECLINE;
            }
            i = e + 2;
        }

        private void comment() {
            if (lx != null || !s.startsWith("<!--", i)) {
                // A comment is an element of the lxml tree, made by the shim.
                throw DECLINE;
            }
            int e = s.indexOf("-->", i + 4);
            if (e < 0) {
                throw DECLINE;
            }
            int bad = s.indexOf("--", i + 4);
            if (bad < e || s.charAt(e - 1) == '-' && e - 1 >= i + 4) {
                throw DECLINE;
            }
            i = e + 3;
        }

        /// Character data in `[a, b)`. Outside the root only whitespace is
        /// allowed, and it is dropped.
        private void text(int a, int b) {
            if (open.isEmpty()) {
                for (int k = a; k < b; k++) {
                    char c = s.charAt(k);
                    if (c != ' ' && c != '\t' && c != '\n') {
                        throw DECLINE;
                    }
                }
                return;
            }
            // Appended a run at a time: each append to a builder takes its lock.
            int k = a;
            while (k < b) {
                int run = k;
                while (run < b && s.charAt(run) != '&') {
                    if (s.charAt(run) == ']' && run + 3 <= b && s.startsWith("]]>", run)) {
                        throw DECLINE;
                    }
                    run++;
                }
                data.append(s, k, run);
                if (run < b) {
                    k = ref(run, b, data);
                } else {
                    k = run;
                }
            }
        }

        /// The reference at `k` (an `&`) appended to `out`; answers the
        /// offset after it.
        private int ref(int k, int b, StringBuilder out) {
            int semi = s.indexOf(';', k + 1);
            if (semi < 0 || semi >= b) {
                throw DECLINE;
            }
            String body = s.substring(k + 1, semi);
            if (body.length() > 1 && body.charAt(0) == '#') {
                long v = 0;
                boolean hex = body.charAt(1) == 'x';
                int from = hex ? 2 : 1;
                if (from >= body.length() || body.length() - from > 8) {
                    throw DECLINE;
                }
                for (int d = from; d < body.length(); d++) {
                    int dv = Character.digit(body.charAt(d), hex ? 16 : 10);
                    if (dv < 0 || body.charAt(d) > 0x7f) {
                        throw DECLINE;
                    }
                    v = v * (hex ? 16 : 10) + dv;
                }
                boolean ok = v == 0x9 || v == 0xA || v == 0xD || v >= 0x20 && v <= 0xD7FF
                        || v >= 0xE000 && v <= 0xFFFD || v >= 0x10000 && v <= 0x10FFFF;
                if (!ok) {
                    throw DECLINE;
                }
                if (v >= 0x10000) {
                    out.append(Character.toChars((int) v));
                } else {
                    out.append((char) v);
                }
            } else if ("amp".equals(body)) {
                out.append('&');
            } else if ("lt".equals(body)) {
                out.append('<');
            } else if ("gt".equals(body)) {
                out.append('>');
            } else if ("quot".equals(body)) {
                out.append('"');
            } else if ("apos".equals(body)) {
                out.append('\'');
            } else {
                throw DECLINE;
            }
            return semi + 1;
        }

        private void flush() {
            if (lx != null) {
                return;
            }
            if (data.length() > 0) {
                if (last != null) {
                    Py.obj(last).setAttr(tail ? "tail" : "text", data.toString());
                }
                data.setLength(0);
            }
        }

        /// Scans the name at `a`, leaving `i` after it and its hash in
        /// `nameHash`, without making a string of it.
        private int nameEnd(int a) {
            int e = XmlScan.name(s, a, n);
            if (e == a) {
                throw DECLINE;
            }
            int h = 0;
            for (int k = a; k < e; k++) {
                char c = s.charAt(k);
                if (c == '{' || c == '}') {
                    throw DECLINE;
                }
                h = 31 * h + c;
            }
            i = e;
            nameHash = h;
            return e;
        }

        private boolean named(int a, int e, String name) {
            return name != null && name.length() == e - a && s.regionMatches(a, name, 0, e - a);
        }

        private String uri(String prefix) {
            for (int k = scopes.size() - 1; k >= 0; k--) {
                java.util.HashMap<String, String> m = scopes.get(k);
                if (m.containsKey(prefix)) {
                    return m.get(prefix);
                }
            }
            return null;
        }

        /// ElementTree's name for `q`: `{uri}local` when it has a namespace.
        private String expand(String q, boolean element) {
            int colon = q.indexOf(':');
            if (colon < 0) {
                if (!element) {
                    return q;
                }
                String u = uri("");
                return u == null || u.length() == 0 ? q : "{" + u + "}" + q;
            }
            if (colon == 0 || colon == q.length() - 1 || q.indexOf(':', colon + 1) >= 0) {
                throw DECLINE;
            }
            String u = uri(q.substring(0, colon));
            if (u == null || u.length() == 0) {
                throw DECLINE;
            }
            return "{" + u + "}" + q.substring(colon + 1);
        }

        private void skipSpace() {
            while (i < n && XmlScan.space(s.charAt(i))) {
                i++;
            }
        }

        private void startTag() {
            if (root != null && open.isEmpty()) {
                throw DECLINE;
            }
            int qa = i + 1;
            int qe = nameEnd(qa);
            int slot = (nameHash ^ nameHash >>> 16) & (TAG_CACHE - 1);
            String q = rawNames[slot];
            if (!named(qa, qe, q)) {
                q = s.substring(qa, qe);
            }
            java.util.ArrayList<String> names = attrNames;
            java.util.ArrayList<String> values = attrValues;
            names.clear();
            values.clear();
            java.util.HashMap<String, String> declared = null;
            boolean self;
            while (true) {
                int ws = i;
                skipSpace();
                if (i >= n) {
                    throw DECLINE;
                }
                char c = s.charAt(i);
                if (c == '>' || c == '/') {
                    self = c == '/';
                    if (self && (i + 1 >= n || s.charAt(i + 1) != '>')) {
                        throw DECLINE;
                    }
                    i += self ? 2 : 1;
                    break;
                }
                if (i == ws) {
                    throw DECLINE;
                }
                int na = i;
                int ne = nameEnd(na);
                int as = (nameHash ^ nameHash >>> 16) & (TAG_CACHE - 1);
                String an = attrRaw[as];
                if (!named(na, ne, an)) {
                    an = s.substring(na, ne);
                    attrRaw[as] = an;
                }
                skipSpace();
                if (i >= n || s.charAt(i) != '=') {
                    throw DECLINE;
                }
                i++;
                skipSpace();
                char qc = i < n ? s.charAt(i) : '\0';
                if (qc != '"' && qc != '\'') {
                    throw DECLINE;
                }
                int ve = s.indexOf(qc, i + 1);
                if (ve < 0) {
                    throw DECLINE;
                }
                String av0 = s.substring(i + 1, ve);
                boolean plain = av0.indexOf('<') < 0 && av0.indexOf('&') < 0 && av0.indexOf('\t') < 0
                        && av0.indexOf('\n') < 0;
                StringBuilder v = plain ? null : new StringBuilder(ve - i);
                for (int k = plain ? ve : i + 1; k < ve;) {
                    char ch = s.charAt(k);
                    if (ch == '<') {
                        throw DECLINE;
                    }
                    if (ch == '&') {
                        k = ref(k, ve, v);
                        continue;
                    }
                    v.append(ch == '\t' || ch == '\n' ? ' ' : ch);
                    k++;
                }
                i = ve + 1;
                if (names.contains(an) || declared != null && ("xmlns".equals(an) ? declared.containsKey("")
                        : an.startsWith("xmlns:") && declared.containsKey(an.substring(6)))) {
                    throw DECLINE;
                }
                String av = plain ? av0 : v.toString();
                if ("xmlns".equals(an) || an.startsWith("xmlns:")) {
                    String prefix = an.length() > 5 ? an.substring(6) : "";
                    if (an.length() > 5 && prefix.length() == 0 || "xmlns".equals(prefix) || "xml".equals(prefix)
                            || XML_NS.equals(av) || XMLNS_NS.equals(av)
                            || prefix.length() > 0 && av.length() == 0 || prefix.indexOf(':') >= 0) {
                        throw DECLINE;
                    }
                    if (declared == null) {
                        // In document order: it becomes the element's nsmap.
                        declared = new java.util.LinkedHashMap<String, String>();
                    }
                    declared.put(prefix, av);
                } else {
                    names.add(an);
                    values.add(av);
                }
            }
            if (declared != null) {
                scopes.add(declared);
                scopeDepth.add(Integer.valueOf(open.size()));
                java.util.Arrays.fill(rawNames, null);
            }
            String tag = rawNames[slot] == q ? tagNames[slot] : null; //NOPMD CompareObjectsWithEquals - the cached string itself
            if (tag == null) {
                tag = expand(q, true);
                rawNames[slot] = q;
                tagNames[slot] = tag;
            }
            PyDict attrib = new PyDict();
            for (int k = 0; k < names.size(); k++) {
                String en = expand(names.get(k), false);
                if (attrib.getOrNull(en) != null) {
                    throw DECLINE;
                }
                attrib.setItem(en, values.get(k));
            }
            if (lx != null) {
                lxStart(tag, attrib, q, declared);
                if (self) {
                    close();
                }
                return;
            }
            flush();
            Object el;
            if (direct) {
                el = ((PyType) factory).newInstance();
                SET_TAG.set(el, tag);
                SET_ATTRIB.set(el, attrib);
                SET_CHILDREN.set(el, new PyList());
            } else {
                el = Py.call(factory, tag, attrib);
            }
            if (!open.isEmpty()) {
                Object kids = CHILDREN.get(open.get(open.size() - 1));
                if (!(kids instanceof PyList)) {
                    throw DECLINE;
                }
                ((PyList) kids).append(el);
            } else {
                root = el;
            }
            last = el;
            tail = false;
            open.add(el);
            openNames.add(q);
            if (self) {
                close();
            }
        }

        private void endTag() {
            int qa = i + 2;
            int qe = nameEnd(qa);
            skipSpace();
            if (i >= n || s.charAt(i) != '>') {
                throw DECLINE;
            }
            i++;
            if (open.isEmpty() || !named(qa, qe, openNames.get(openNames.size() - 1))) {
                throw DECLINE;
            }
            close();
        }

        /// `_XMLDriver.flush`: the text pending before a tag, given to the open
        /// element as `_TreeSink.data` does, unless `remove_blank_text` drops
        /// it. `end` is whether the tag that ends it is an end tag.
        private void lxFlush(boolean end) {
            if (data.length() == 0) {
                return;
            }
            String text = data.toString();
            data.setLength(0);
            if (open.isEmpty()) {
                return;
            }
            int top = state.size() - 1;
            int st = state.get(top).intValue();
            boolean hadChild = (st & 1) != 0;
            if (lx.removeBlankText && blank(text)) {
                if (!hadChild) {
                    if (!end) {
                        return;
                    }
                } else if ((st & 2) == 0) {
                    return;
                }
            }
            if (!hadChild) {
                state.set(top, Integer.valueOf(st | 2));
            }
            Object parent = open.get(open.size() - 1);
            Object kids = KIDS.get(parent);
            if (!(kids instanceof PyList)) {
                throw DECLINE;
            }
            PyList k = (PyList) kids;
            if (k.size() > 0) {
                X_TAIL.set(k.get(k.size() - 1), text);
            } else {
                X_TEXT.set(parent, text);
            }
        }

        private boolean blank(String t) {
            for (int k = 0; k < t.length(); k++) {
                char c = t.charAt(k);
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    return false;
                }
            }
            return true;
        }

        /// `_XMLDriver.on_start` and `_TreeSink.start`, with `_blank`'s
        /// fields stored in its order, so an element has the layout the
        /// Python path gives it.
        private void lxStart(String tag, PyDict attrib, String q, java.util.HashMap<String, String> declared) {
            lxFlush(false);
            if (!state.isEmpty()) {
                int top = state.size() - 1;
                state.set(top, Integer.valueOf(state.get(top).intValue() | 1));
            }
            Object hint;
            if (tag.length() > 0 && tag.charAt(0) == '{') {
                int colon = q.indexOf(':');
                hint = colon < 0 ? Py.NONE : q.substring(0, colon);
            } else {
                hint = lx.noHint;
            }
            PyDict ns = new PyDict();
            if (declared != null) {
                for (java.util.Map.Entry<String, String> e : declared.entrySet()) {
                    if (e.getValue().length() > 0) {
                        ns.setItem(e.getKey().length() == 0 ? Py.NONE : e.getKey(), e.getValue());
                    }
                }
            }
            Object cls = lx.classFor(tag);
            Object el = NEW.call1(cls, cls);
            Object parent = open.isEmpty() ? null : open.get(open.size() - 1);
            PyList kids = new PyList();
            if (!fastElement(el, tag, attrib, ns, kids, parent == null ? Py.NONE : parent, hint)) {
                slowElement(el, tag, attrib, ns, kids, hint);
                if (parent != null) {
                    X_PARENT.set(el, parent);
                }
            }
            if (parent != null) {
                Object pk = KIDS.get(parent);
                if (!(pk instanceof PyList)) {
                    throw DECLINE;
                }
                ((PyList) pk).append(el);
            } else {
                root = el;
            }
            open.add(el);
            openNames.add(q);
            state.add(Integer.valueOf(0));
            Object init = Py.orDefault(INIT.getOrMiss(el), Py.NONE);
            if (init != null && init != Py.NONE) { //NOPMD CompareObjectsWithEquals - None is a singleton
                Py.call(init);
            }
        }

        /// The names `slowElement` stores, in its order.
        private static final String[] ELEMENT_NAMES = {"_xkind", "_xtag", "_xattrib", "_xns", "_xkids", "_xparent",
            "_xtext", "_xtail", "_xpfx", "_xlookup", "_xdoc", "_xtarget", "_xcnt"};

        /// Element class -> the shape its instances have after
        /// `slowElement`, learnt from the first one and good while no class
        /// changed (`PyType.version`). Shared by every parse: shapes and
        /// the version are global, and the program's threads parse one at a
        /// time under the interpreter lock.
        private static final java.util.HashMap<PyType, Shape> elementShapes = new java.util.HashMap<PyType, Shape>();
        private static int elementVersion = -1;

        /// Installs all of an element's attributes at once: the same values
        /// `slowElement` stores, as the one shape every element of the class
        /// ends with. Only for a plain instance with nothing set yet, of a
        /// class whose first element was built the slow way.
        private boolean fastElement(Object el, String tag, PyDict attrib, PyDict ns, PyList kids, Object parent,
                Object hint) {
            if (el == null || el.getClass() != PyInstance.class) { //NOPMD CompareObjectsWithEquals - exact class is meant
                return false;
            }
            PyInstance i = (PyInstance) el;
            if (i.shape().size() != 0 || i.base != null || i.nonStrKeys != null) {
                return false;
            }
            Shape s = elementShape(i.cls);
            if (s == null) {
                slowElement(el, tag, attrib, ns, kids, hint);
                X_PARENT.set(el, parent);
                Shape got = i.shape();
                if (java.util.Arrays.equals(got.names, ELEMENT_NAMES)) {
                    learnShape(i.cls, got);
                }
                return true;
            }
            Object[] v = new Object[Math.max(ELEMENT_NAMES.length, i.cls.slotHint)];
            v[0] = "e";
            v[1] = tag;
            v[2] = attrib;
            v[3] = ns;
            v[4] = kids;
            v[5] = parent;
            v[6] = Py.NONE;
            v[7] = Py.NONE;
            v[8] = hint;
            v[9] = lx.lookup;
            v[10] = Py.NONE;
            v[11] = Py.NONE;
            v[12] = lx.cnt;
            i.slots = v;
            i.shape = s;
            return true;
        }

        private static Shape elementShape(PyType t) {
            if (elementVersion != PyType.version) {
                elementVersion = PyType.version;
                elementShapes.clear();
            }
            return elementShapes.get(t);
        }

        private static void learnShape(PyType t, Shape s) {
            if (elementVersion == PyType.version) {
                elementShapes.put(t, s);
            }
        }

        private void slowElement(Object el, String tag, PyDict attrib, PyDict ns, PyList kids, Object hint) {
            // The attributes in _Element.__init__'s order, which fixes the
            // instance dict's order, each stored once with its final value:
            // the defaults it then overwrote cost a store and, for the
            // containers, an allocation each per element.
            X_KIND.set(el, "e");
            X_TAG.set(el, tag);
            X_ATTRIB.set(el, attrib);
            X_NS.set(el, ns);
            X_KIDS.set(el, kids);
            X_PARENT.set(el, Py.NONE);
            X_TEXT.set(el, Py.NONE);
            X_TAIL.set(el, Py.NONE);
            X_PFX.set(el, hint);
            X_LOOKUP.set(el, lx.lookup);
            X_DOC.set(el, Py.NONE);
            X_TARGET.set(el, Py.NONE);
            X_CNT.set(el, lx.cnt);
        }

        private void close() {
            if (lx != null) {
                lxFlush(true);
                state.remove(state.size() - 1);
            } else {
                flush();
            }
            int depth = open.size() - 1;
            last = open.remove(depth);
            openNames.remove(depth);
            tail = true;
            if (scopeDepth.get(scopeDepth.size() - 1).intValue() == depth) {
                scopes.remove(scopes.size() - 1);
                scopeDepth.remove(scopeDepth.size() - 1);
                java.util.Arrays.fill(rawNames, null);
            }
        }
    }
}
