/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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

import java.util.HashMap;

/// The common path of `xml.parsers.expat`'s tokenizer, which CPython runs as
/// C. [#run] consumes complete tokens -- text with predefined and character
/// references, start and end tags with namespace processing, comments --
/// and calls the parser's handlers itself. It stops, without side effects,
/// before the first token it does not take on (a declaration, a processing
/// instruction, CDATA, a general entity, input that is incomplete, anything
/// that is an error) and returns its position; the Python step then handles
/// that one token exactly as before, so errors and their positions stay the
/// Python module's.
///
/// The parser's settings (`buffer_text`, `ordered_attributes`,
/// `specified_attributes`, `namespace_prefixes`, the separator) are read once
/// per call, as the C parser reads them once per parse; handlers are read at
/// every event, so a handler that replaces another takes effect at once.
final class ExpatRun {
    private ExpatRun() {
    }

    private static final AttrSite STATE = new AttrSite("_state");
    private static final StoreSite SET_STATE = new StoreSite("_state");
    private static final StoreSite SET_EV = new StoreSite("_ev");
    private static final AttrSite STACK = new AttrSite("_stack");
    private static final AttrSite NS = new AttrSite("_ns");
    private static final StoreSite SET_NS = new StoreSite("_ns");
    private static final AttrSite SEP = new AttrSite("_sep");
    private static final AttrSite TEXT = new AttrSite("_text");
    private static final AttrSite BUFFER_TEXT = new AttrSite("buffer_text");
    private static final AttrSite ORDERED = new AttrSite("ordered_attributes");
    private static final AttrSite SPECIFIED = new AttrSite("specified_attributes");
    private static final AttrSite NS_PREFIXES = new AttrSite("namespace_prefixes");
    private static final AttrSite ATTDEFAULTS = new AttrSite("_attdefaults");
    private static final AttrSite EXPANDING = new AttrSite("_expanding");
    private static final AttrSite FRAGMENT = new AttrSite("_fragment");
    private static final AttrSite H_CHARS = new AttrSite("CharacterDataHandler");
    private static final AttrSite H_START = new AttrSite("StartElementHandler");
    private static final AttrSite H_END = new AttrSite("EndElementHandler");
    private static final AttrSite H_COMMENT = new AttrSite("CommentHandler");
    private static final AttrSite H_START_NS = new AttrSite("StartNamespaceDeclHandler");
    private static final AttrSite H_END_NS = new AttrSite("EndNamespaceDeclHandler");

    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
    private static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    private static final String NL = "\n";

    /// One call's view of the parser.
    private static final class Ctx {
        final Object p;
        final PyList stack;
        final PyList text;
        final boolean buffered;
        final boolean ordered;
        final boolean prefixes;
        /// The separator; null without namespace processing.
        final String sep;
        /// Whether no start tag can be taken (an odd separator or defaults
        /// table): every one goes to the Python step.
        final boolean noTags;
        /// The DTD's attribute defaults, null when none apply.
        final PyDict defaults;
        Object ns;
        int state;
        /// Expanded names by qualified name, for the scope [#nameNs].
        final HashMap<String, String> elemNames = new HashMap<String, String>();
        final HashMap<String, String> attrNames = new HashMap<String, String>();
        Object nameNs;
        /// The start tag [#scan] read.
        String qname;
        String[] an = new String[8];
        String[] av = new String[8];
        int na;
        boolean selfClosing;

        Ctx(Object p) {
            this.p = p;
            Object st = STACK.get(p);
            Object tx = TEXT.get(p);
            this.stack = st instanceof PyList ? (PyList) st : null;
            this.text = tx instanceof PyList ? (PyList) tx : null;
            this.buffered = Py.isTrue(BUFFER_TEXT.get(p));
            this.ordered = Py.isTrue(ORDERED.get(p));
            this.prefixes = Py.isTrue(NS_PREFIXES.get(p));
            Object s = SEP.get(p);
            this.sep = s instanceof String ? (String) s : null;
            this.ns = NS.get(p);
            this.state = (int) Typed.toLong(STATE.get(p));
            boolean odd = !isNone(s) && sep == null || sep != null && !(ns instanceof PyDict);
            PyDict d = null;
            if (!Py.isTrue(SPECIFIED.get(p))) {
                Object o = ATTDEFAULTS.get(p);
                if (o instanceof PyDict) {
                    d = ((PyDict) o).size() > 0 ? (PyDict) o : null;
                } else {
                    odd = true;
                }
            }
            this.defaults = d;
            this.noTags = odd || stack == null;
        }

        void setState(int s) {
            state = s;
            SET_STATE.set(p, Long.valueOf(s));
        }

        void setNs(Object n) {
            ns = n;
            SET_NS.set(p, n);
        }

        void ev(int pos) {
            SET_EV.set(p, Long.valueOf(pos));
        }
    }

    static int run(Object p, String buf, int pos) {
        Ctx c = new Ctx(p);
        int n = buf.length();
        while (pos < n) {
            int lt = buf.indexOf('<', pos);
            if (lt != pos) {
                if (lt < 0) {
                    return pos;
                }
                int r = text(c, buf, pos, lt);
                if (r < lt) {
                    return r;
                }
                pos = lt;
                continue;
            }
            char ch = pos + 1 < n ? buf.charAt(pos + 1) : '\0';
            int r;
            if (ch == '/') {
                r = endTag(c, buf, pos);
            } else if (ch == '!') {
                r = comment(c, buf, pos);
            } else if (ch == '?') {
                return pos;
            } else {
                r = startTag(c, buf, pos);
            }
            if (r < 0) {
                return pos;
            }
            pos = r;
        }
        return pos;
    }

    private static boolean isNone(Object o) {
        return o == null || o == Py.NONE; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    // -- text --------------------------------------------------------------

    /// Text in `[s, e)`: returns `e`, or the position the Python step must
    /// take over at (nothing at or past it has been reported).
    private static int text(Ctx c, String buf, int s, int e) {
        if (c.state != 1) {
            return s;
        }
        // One pass over the run: where its first reference is, and whether
        // it holds "]]>" (an error the Python step reports). Every search is
        // bounded by the run; one reaching the end of the buffer made a
        // document's parse quadratic.
        int firstAmp = -1;
        for (int i = s; i < e; i++) {
            char ch = buf.charAt(i);
            if (ch == '&') {
                if (firstAmp < 0) {
                    firstAmp = i;
                }
            } else if (ch == '>' && i - s >= 2 && buf.charAt(i - 1) == ']' && buf.charAt(i - 2) == ']') {
                return s;
            }
        }
        c.ev(s);
        int at = s;
        while (true) {
            int amp = at == s ? firstAmp : find(buf, '&', at, e);
            if (amp < 0) {
                chars(c, buf.substring(at, e));
                return e;
            }
            if (amp > at) {
                chars(c, buf.substring(at, amp));
            }
            int semi = find(buf, ';', amp + 1, Math.min(e, amp + 12));
            if (semi < 0) {
                return amp;
            }
            String v = reference(buf, amp + 1, semi);
            if (v == null) {
                return amp;
            }
            chars(c, v);
            at = semi + 1;
        }
    }

    private static int find(String buf, char c, int s, int e) {
        for (int i = s; i < e; i++) {
            if (buf.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    /// The text of the reference between `&` and `;`, or null for one the
    /// Python step must see (a general entity, or a malformed reference).
    private static String reference(String buf, int s, int e) {
        if (s >= e) {
            return null;
        }
        if (buf.charAt(s) != '#') {
            switch (e - s) {
                case 2:
                    if (buf.startsWith("lt", s)) {
                        return "<";
                    }
                    if (buf.startsWith("gt", s)) {
                        return ">";
                    }
                    return null;
                case 3:
                    return buf.startsWith("amp", s) ? "&" : null;
                case 4:
                    if (buf.startsWith("quot", s)) {
                        return "\"";
                    }
                    if (buf.startsWith("apos", s)) {
                        return "'";
                    }
                    return null;
                default:
                    return null;
            }
        }
        int i = s + 1;
        boolean hex = i < e && buf.charAt(i) == 'x';
        if (hex) {
            i++;
        }
        if (i >= e || e - i > 8) {
            return null;
        }
        int cp = 0;
        for (; i < e; i++) {
            int d = Character.digit(buf.charAt(i), hex ? 16 : 10);
            if (d < 0 || buf.charAt(i) > 'f') {
                return null;
            }
            cp = cp * (hex ? 16 : 10) + d;
        }
        if (cp == 0 || cp > 0x10FFFF || cp >= 0xD800 && cp <= 0xDFFF || cp == 0xFFFE || cp == 0xFFFF
                || cp < 0x20 && cp != 9 && cp != 10 && cp != 13) {
            return null;
        }
        if (cp < 0x10000) {
            return String.valueOf((char) cp);
        }
        int v = cp - 0x10000;
        return new String(new char[] {(char) (0xD800 + (v >> 10)), (char) (0xDC00 + (v & 0x3FF))});
    }


    /// `_chars`: buffered, or one call per line and per line ending.
    private static void chars(Ctx c, String s) {
        if (s.length() == 0) {
            return;
        }
        if (c.buffered) {
            if (c.text != null) {
                c.text.append(s);
            } else {
                Py.callMethod(TEXT.get(c.p), "append", s);
            }
            return;
        }
        Object h = H_CHARS.get(c.p);
        if (isNone(h)) {
            return;
        }
        int start = 0;
        int len = s.length();
        while (true) {
            int nl = s.indexOf('\n', start);
            if (nl < 0) {
                if (start < len) {
                    Py.call(h, start == 0 ? s : s.substring(start));
                }
                return;
            }
            if (nl > start) {
                Py.call(h, s.substring(start, nl));
            }
            Py.call(h, NL);
            start = nl + 1;
        }
    }

    /// `_flush`: hands buffered text to the handler.
    private static void flush(Ctx c) {
        PyList l = c.text;
        if (l == null) {
            Py.callMethod(c.p, "_flush");
            return;
        }
        int k = l.size();
        if (k == 0) {
            return;
        }
        String data;
        if (k == 1 && l.get(0) instanceof String) {
            data = (String) l.get(0);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < k; i++) {
                sb.append(Typed.asStr(l.get(i)));
            }
            data = sb.toString();
        }
        l.clear();
        Object h = H_CHARS.get(c.p);
        if (!isNone(h)) {
            Py.call(h, data);
        }
    }

    // -- comments ----------------------------------------------------------

    private static int comment(Ctx c, String buf, int pos) {
        if (!buf.startsWith("<!--", pos)) {
            return -1;
        }
        int e = buf.indexOf("-->", pos + 4);
        if (e < 0) {
            return -1;
        }
        int bad = buf.indexOf("--", pos + 4);
        if (bad < e || e - 1 >= pos + 4 && buf.charAt(e - 1) == '-') {
            return -1;
        }
        c.ev(pos);
        flush(c);
        Object h = H_COMMENT.get(c.p);
        if (!isNone(h)) {
            Py.call(h, buf.substring(pos + 4, e));
        }
        return e + 3;
    }

    // -- tags --------------------------------------------------------------

    private static int endTag(Ctx c, String buf, int pos) {
        PyList st = c.stack;
        if (st == null) {
            return -1;
        }
        int n = buf.length();
        int i = pos + 2;
        int e = XmlScan.name(buf, i, n);
        if (e == i) {
            return -1;
        }
        int j = e;
        while (j < n && XmlScan.space(buf.charAt(j))) {
            j++;
        }
        if (j >= n || buf.charAt(j) != '>') {
            return -1;
        }
        int k = st.size();
        if (k == 0) {
            return -1;
        }
        Object top = st.get(k - 1);
        if (!(top instanceof PyTuple)) {
            return -1;
        }
        Object q = ((PyTuple) top).get(0);
        if (!(q instanceof String) || ((String) q).length() != e - i || !buf.startsWith((String) q, i)) {
            return -1;
        }
        c.ev(pos);
        closeTop(c);
        return j + 1;
    }

    /// `_close_top`.
    private static void closeTop(Ctx c) {
        PyList st = c.stack;
        PyTuple top = (PyTuple) st.pop(-1);
        flush(c);
        Object h = H_END.get(c.p);
        if (!isNone(h)) {
            Py.call(h, top.get(1));
        }
        Object declared = top.get(2);
        if (Py.len(declared) > 0) {
            h = H_END_NS.get(c.p);
            if (!isNone(h)) {
                if (declared instanceof PyList) {
                    PyList d = (PyList) declared;
                    for (int i = d.size() - 1; i >= 0; i--) {
                        Py.call(h, ((PyTuple) d.get(i)).get(0));
                    }
                }
            }
        }
        Object parent = top.get(3);
        if (parent != c.ns) { //NOPMD CompareObjectsWithEquals - identity is meant
            c.setNs(parent);
        }
        if (st.size() == 0 && Py.len(EXPANDING.get(c.p)) == 0) {
            Object frag = FRAGMENT.getOrMiss(c.p);
            if (frag == Py.MISS || !Py.isTrue(frag)) { //NOPMD CompareObjectsWithEquals - identity is meant
                c.setState(2);
            }
        }
    }

    /// Reads `<name attr="v" ...>` at `pos` into the context, values
    /// normalised as `_attr_value` would; -1 for anything off the common path
    /// (see [XmlScan#scanStart], which this follows).
    private static int scan(Ctx c, String s, int pos) {
        int n = s.length();
        int i = pos + 1;
        int e = XmlScan.name(s, i, n);
        if (e == i) {
            return -1;
        }
        c.qname = s.substring(i, e);
        c.na = 0;
        i = e;
        while (true) {
            int ws = i;
            while (i < n && XmlScan.space(s.charAt(i))) {
                i++;
            }
            if (i >= n) {
                return -1;
            }
            char ch = s.charAt(i);
            if (ch == '>' || ch == '/') {
                c.selfClosing = ch == '/';
                if (c.selfClosing) {
                    if (i + 1 >= n || s.charAt(i + 1) != '>') {
                        return -1;
                    }
                    i++;
                }
                return i + 1;
            }
            if (i == ws) {
                return -1;
            }
            int ne = XmlScan.name(s, i, n);
            if (ne == i) {
                return -1;
            }
            int ns = i;
            i = ne;
            while (i < n && XmlScan.space(s.charAt(i))) {
                i++;
            }
            if (i >= n || s.charAt(i) != '=') {
                return -1;
            }
            i++;
            while (i < n && XmlScan.space(s.charAt(i))) {
                i++;
            }
            if (i >= n) {
                return -1;
            }
            char q = s.charAt(i);
            if (q != '"' && q != '\'') {
                return -1;
            }
            int vs = i + 1;
            int ve = s.indexOf(q, vs);
            if (ve < 0) {
                return -1;
            }
            boolean norm = false;
            for (int k = vs; k < ve; k++) {
                char v = s.charAt(k);
                if (v == '<' || v == '&') {
                    return -1;
                }
                if (v == '\t' || v == '\n' || v == '\r') {
                    norm = true;
                }
            }
            int len = ne - ns;
            for (int k = 0; k < c.na; k++) {
                String o = c.an[k];
                if (o.length() == len && s.startsWith(o, ns)) {
                    return -1;
                }
            }
            String val = s.substring(vs, ve);
            if (norm) {
                val = val.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
            }
            if (c.na == c.an.length) {
                String[] a = new String[c.na * 2];
                String[] b = new String[c.na * 2];
                System.arraycopy(c.an, 0, a, 0, c.na);
                System.arraycopy(c.av, 0, b, 0, c.na);
                c.an = a;
                c.av = b;
            }
            c.an[c.na] = s.substring(ns, ne);
            c.av[c.na] = val;
            c.na++;
            i = ve + 1;
        }
    }

    private static int startTag(Ctx c, String buf, int pos) {
        if (c.noTags || c.state == 2) {
            return -1;
        }
        int end = scan(c, buf, pos);
        if (end < 0) {
            return -1;
        }
        String qname = c.qname;
        if (c.defaults != null && c.defaults.getOrNull(qname) != null) {
            return -1;
        }
        Object parentNs = c.ns;
        int k = c.na;
        Object declared = PyTuple.EMPTY;
        PyDict newNs = null;
        String name;
        if (c.sep != null) {
            // Declarations first: they change the scope the names expand in.
            PyList decl = null;
            for (int i = 0; i < k; i++) {
                String an = c.an[i];
                if (!an.startsWith("xmlns") || an.length() > 5 && an.charAt(5) != ':') {
                    continue;
                }
                String av = c.av[i];
                String prefix = an.length() > 5 ? an.substring(6) : null;
                if ("xmlns".equals(prefix) || "xml".equals(prefix) && !XML_NS.equals(av)
                        || !"xml".equals(prefix) && (XML_NS.equals(av) || XMLNS_NS.equals(av))
                        || prefix != null && av.length() == 0) {
                    return -1;
                }
                if (newNs == null) {
                    newNs = ((PyDict) parentNs).copy();
                    decl = Py.list();
                }
                newNs.setItem(prefix == null || prefix.length() == 0 ? "" : prefix, av);
                decl.append(Py.tuple(prefix == null ? Py.NONE : prefix, av.length() == 0 ? Py.NONE : av));
            }
            if (decl != null) {
                declared = decl;
            }
            PyDict scope = newNs != null ? newNs : (PyDict) parentNs;
            if (scope != c.nameNs) { //NOPMD CompareObjectsWithEquals - identity is meant
                c.nameNs = scope;
                c.elemNames.clear();
                c.attrNames.clear();
            }
            name = c.elemNames.get(qname);
            if (name == null) {
                name = expand(scope, qname, true, c.sep, c.prefixes);
                if (name == null) {
                    return -1;
                }
                c.elemNames.put(qname, name);
            }
            // Keep the attributes that are not declarations, expanded.
            int keep = 0;
            for (int i = 0; i < k; i++) {
                String an = c.an[i];
                if (an.startsWith("xmlns") && (an.length() == 5 || an.charAt(5) == ':')) {
                    continue;
                }
                String en = c.attrNames.get(an);
                if (en == null) {
                    en = expand(scope, an, false, c.sep, c.prefixes);
                    if (en == null) {
                        return -1;
                    }
                    c.attrNames.put(an, en);
                }
                for (int j = 0; j < keep; j++) {
                    if (c.an[j].equals(en)) {
                        return -1;
                    }
                }
                c.an[keep] = en;
                c.av[keep] = c.av[i];
                keep++;
            }
            k = keep;
        } else {
            name = qname;
        }
        // Nothing below declines: the token is ours from here.
        c.ev(pos);
        if (newNs != null) {
            c.setNs(newNs);
        }
        flush(c);
        if (declared != PyTuple.EMPTY) { //NOPMD CompareObjectsWithEquals - identity is meant
            Object h = H_START_NS.get(c.p);
            if (!isNone(h)) {
                PyList d = (PyList) declared;
                for (int i = 0; i < d.size(); i++) {
                    PyTuple t = (PyTuple) d.get(i);
                    Py.call(h, t.get(0), t.get(1));
                }
            }
        }
        if (c.state == 0) {
            c.setState(1);
        }
        Object h = H_START.get(c.p);
        if (!isNone(h)) {
            Object attrs;
            if (c.ordered) {
                PyList l = Py.list();
                for (int i = 0; i < k; i++) {
                    l.append(c.an[i]);
                    l.append(c.av[i]);
                }
                attrs = l;
            } else {
                PyDict d = Py.dict();
                for (int i = 0; i < k; i++) {
                    d.setItem(c.an[i], c.av[i]);
                }
                attrs = d;
            }
            Py.call(h, name, attrs);
        }
        c.stack.append(new PyTuple(new Object[] {qname, name, declared, parentNs}));
        if (c.selfClosing) {
            c.ev(pos);
            closeTop(c);
        }
        return end;
    }

    /// `_expand_name`, or null where it would raise.
    private static String expand(PyDict ns, String qname, boolean element, String sep, boolean prefixes) {
        int colon = qname.indexOf(':');
        if (colon < 0) {
            if (!element) {
                return qname;
            }
            Object uri = ns.getOrNull("");
            if (!(uri instanceof String) || ((String) uri).length() == 0) {
                return uri == null || uri instanceof String || isNone(uri) ? qname : null;
            }
            return (String) uri + sep + qname;
        }
        Object uri = ns.getOrNull(qname.substring(0, colon));
        if (!(uri instanceof String)) {
            return null;
        }
        String local = qname.substring(colon + 1);
        if (prefixes) {
            return (String) uri + sep + local + sep + qname.substring(0, colon);
        }
        return (String) uri + sep + local;
    }
}
