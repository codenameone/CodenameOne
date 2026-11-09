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
        m.setAttr("scan_start", PyBuiltin.positional("scan_start",
                a -> scanStart(Typed.asStr(a[0]), (int) Typed.toLong(a[1]))));
        m.setAttr("iter", PyBuiltin.positional("iter", a -> new ElementIter(a[0], a[1])));
        m.setAttr("find", PyBuiltin.positional("find", a -> children(a[0], a[1], false)));
        m.setAttr("findall", PyBuiltin.positional("findall", a -> children(a[0], a[1], true)));
        m.setAttr("build", PyBuiltin.positional("build",
                a -> new Builder(Typed.asStr(a[0]), a[1], Py.isTrue(a[2]), a.length > 3 ? a[3] : null).build()));
        m.setAttr("scan_end", PyBuiltin.positional("scan_end",
                a -> scanEnd(Typed.asStr(a[0]), (int) Typed.toLong(a[1]))));
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
    private static final AttrSite ATTRIB = new AttrSite("attrib");
    /// Slots in a builder's tag cache, a power of two.
    private static final int TAG_CACHE = 64;

    private static final AttrSite TAG = new AttrSite("tag");

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

        Builder(String text, Object factory, boolean fromBytes, Object elementInit) {
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
            if (!s.startsWith("<!--", i)) {
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
                        declared = new java.util.HashMap<String, String>();
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
            flush();
            Object el;
            if (direct) {
                el = ((PyType) factory).newInstance();
                TAG.set(el, tag);
                ATTRIB.set(el, attrib);
                CHILDREN.set(el, new PyList());
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

        private void close() {
            flush();
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
