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

/// `re.Match`, and the one call `Pattern.match`, `search` and `fullmatch`
/// make: `_cn1re.match(prog, pattern, string, pos, endpos, mode)`. sre does
/// both in C. As Python they were five frames per call (`match`, `_run`,
/// `_check`, the engine wrapper, the program lookup), a list of boxed
/// registers, and a Match whose constructor walked `groupindex` and whose
/// `group(1)` went through two isinstance checks: three to five times
/// CPython's cost for a call that matches three characters.
final class RegexMatch extends PyObject {
    static final PyType TYPE = new PyType("Match", new PyType[] {Py.OBJECT_TYPE}, null);

    private final Object pattern;
    private final Object string;
    private final String text;
    private final boolean bytes;
    private final int pos;
    private final int endpos;
    private final int groups;
    /// Two per group, group 0 first, then `lastindex` (0 for none).
    private final int[] regs;

    RegexMatch(Object pattern, Object string, String text, boolean bytes, int pos, int endpos,
            int groups, int[] regs) {
        this.pattern = pattern;
        this.string = string;
        this.text = text;
        this.bytes = bytes;
        this.pos = pos;
        this.endpos = endpos;
        this.groups = groups;
        this.regs = regs;
    }

    @Override
    public PyType type() {
        return TYPE;
    }

    /// `_cn1re.match`: None or a Match. `endpos` may be None.
    static Object match(Object progObj, Object pattern, Object string, Object posObj, Object endObj,
            int mode) {
        if (!(progObj instanceof Regex.Prog)) {
            throw Py.typeError("_cn1re.match() needs a compiled program");
        }
        Regex.Prog p = (Regex.Prog) progObj;
        return search(p, pattern, string, posObj, endObj, -1, mode, -1);
    }

    /// The shared body of a match and of one step of `finditer`: `at` is where
    /// the attempt starts (-1 for `pos`), `forbid` an end an empty match may
    /// not have.
    static Object search(Regex.Prog p, Object pattern, Object string, Object posObj, Object endObj,
            int at, int mode, int forbid) {
        boolean isBytes = checkKind(p, pattern, string);
        String text = isBytes ? latin1((PyBytes) string) : (String) string;
        // Counted once: the run below is handed the count rather than
        // taking it again (a decoded bytes text is Latin-1, without pairs).
        int pairs = isBytes ? 0 : PyStr.surrogates(text);
        int n = text.length() - pairs;
        long pos = Py.index(posObj);
        long endpos = endObj == Py.NONE ? n : Py.index(endObj); //NOPMD CompareObjectsWithEquals - None is a singleton
        if (endpos > n) {
            endpos = n;
        }
        if (endpos < 0) {
            endpos = 0;
        }
        if (pos < 0) {
            pos = 0;
        }
        if (pos > endpos) {
            return Py.NONE;
        }
        int start = at < 0 ? (int) pos : at;
        if (start > endpos) {
            return Py.NONE;
        }
        int[] regs = Regex.run(p, text, pairs, start, (int) endpos, mode, forbid);
        if (regs.length == 0) {
            return Py.NONE;
        }
        return new RegexMatch(pattern, string, text, isBytes, (int) pos, (int) endpos, p.groups, regs);
    }

    /// True for bytes. A str pattern takes only str and a bytes one only
    /// bytes, as sre's `getstring` insists; which one a program is was fixed
    /// when it was compiled, so the pattern is asked once.
    private static boolean checkKind(Regex.Prog p, Object pattern, Object string) {
        int kind = p.kind;
        if (kind == 0) {
            kind = Py.getattr(pattern, "pattern") instanceof String ? 1 : 2;
            p.kind = kind;
        }
        if (kind == 1) {
            if (!(string instanceof String)) {
                throw Py.typeError("cannot use a string pattern on a bytes-like object");
            }
            return false;
        }
        if (string instanceof String) {
            throw Py.typeError("cannot use a bytes pattern on a string-like object");
        }
        if (!(string instanceof PyBytes)) {
            throw Py.typeError("expected string or bytes-like object, got '"
                    + Py.typeOf(string).name + "'");
        }
        return true;
    }

    /// The last bytes object decoded and its text. A tokenizer (pdfminer's)
    /// searches one buffer from position after position; decoding all of it
    /// on every call made a PDF slower than the Python wrapper was. Bytes are
    /// immutable, so identity is enough. Python runs under the [Gil].
    private static PyBytes lastBytes;
    private static String lastText;

    private static String latin1(PyBytes b) {
        if (b == lastBytes) { //NOPMD CompareObjectsWithEquals - identity is meant
            return lastText;
        }
        byte[] v = b.value;
        String t = Codecs.latin1(v, 0, v.length);
        if (v.length <= 64) {
            // A short subject costs less to convert than to remember, and
            // remembering it would evict the long one: psparser alternates
            // `NONSPC.search(self.buf, i)` with subs over each hex token, and
            // every token converted the whole buffer again.
            return t;
        }
        lastBytes = b;
        lastText = t;
        return t;
    }

    // ---- groups ------------------------------------------------------------

    private int index(Object g) {
        if (g instanceof Long) {
            long i = ((Long) g).longValue();
            if (i >= 0 && i <= groups) {
                return (int) i;
            }
        } else if (g instanceof String) {
            Object idx = groupindex().getOrNull(g);
            if (idx != null) {
                return (int) Py.index(idx);
            }
        } else if (Py.isInt(g)) {
            long i = Py.asLong(g);
            if (i >= 0 && i <= groups) {
                return (int) i;
            }
        }
        throw Py.indexError("no such group");
    }

    private PyDict groupindex() {
        Object d = Py.getattr(pattern, "groupindex");
        if (!(d instanceof PyDict)) {
            throw Py.typeError("Pattern.groupindex is not a dict");
        }
        return (PyDict) d;
    }

    private Object get(int idx, Object dflt) {
        int s = regs[2 * idx];
        int e = regs[2 * idx + 1];
        if (s < 0 || e < 0) {
            return dflt;
        }
        if (bytes) {
            byte[] v = ((PyBytes) string).value;
            byte[] out = new byte[e - s];
            System.arraycopy(v, s, out, 0, e - s);
            return new PyBytes(out);
        }
        return PyStr.substr(text, s, e);
    }

    private Object group(Object[] a) {
        if (a.length == 1) {
            return get(0, Py.NONE);
        }
        if (a.length == 2) {
            return get(index(a[1]), Py.NONE);
        }
        Object[] out = new Object[a.length - 1];
        for (int i = 1; i < a.length; i++) {
            out[i - 1] = get(index(a[i]), Py.NONE);
        }
        return new PyTuple(out);
    }

    private Object groupsTuple(Object dflt) {
        Object[] out = new Object[groups];
        for (int i = 0; i < groups; i++) {
            out[i] = get(i + 1, dflt);
        }
        return new PyTuple(out);
    }

    private Object span(int i) {
        return Py.tuple(Py.of(regs[2 * i]), Py.of(regs[2 * i + 1]));
    }

    private Object lastgroup() {
        int last = regs[regs.length - 1];
        if (last <= 0) {
            return Py.NONE;
        }
        PyDict gi = groupindex();
        Object found = Py.NONE;
        for (Object k : gi.keyArray()) {
            Object v = gi.getOrNull(k);
            if (v != null && Py.index(v) == last) {
                found = k;
            }
        }
        return found;
    }

    @Override
    public Object getAttrOrNull(String name) {
        switch (name) {
            case "re":
                return pattern;
            case "string":
                return string;
            case "pos":
                return Py.of(pos);
            case "endpos":
                return Py.of(endpos);
            case "lastindex": {
                int last = regs[regs.length - 1];
                return last > 0 ? Py.of(last) : Py.NONE;
            }
            case "lastgroup":
                return lastgroup();
            case "regs": {
                Object[] out = new Object[groups + 1];
                for (int i = 0; i <= groups; i++) {
                    out[i] = span(i);
                }
                return new PyTuple(out);
            }
            default:
                return super.getAttrOrNull(name);
        }
    }

    @Override
    public Object getItem(Object key) {
        return get(index(key), Py.NONE);
    }

    @Override
    public String repr() {
        return "<re.Match object; span=(" + regs[0] + ", " + regs[1] + "), match="
                + Py.toRepr(get(0, Py.NONE)) + ">";
    }

    /// `expand` needs the template compiler of `re`, which registers it here
    /// when it is imported. Python runs under the [Gil], one thread at a time.
    static Object expandHook;
    /// `re._compile_template` and `Pattern._subn`, registered with it.
    static Object templateHook;
    static Object subHook;
    static final int TEMPLATES = 64;

    /// `Pattern.sub` (`withCount` false) and `subn`: a str template over a
    /// str runs entirely here; anything else (a callable, bytes) goes to
    /// the general loop in Python.
    static Object sub(Regex.Prog p, Object pattern, Object repl, Object string, Object countObj,
            boolean withCount) {
        if (repl instanceof String && string instanceof String && !checkKind(p, pattern, string)) {
            java.util.HashMap<String, PyList> cache = p.templates;
            if (cache == null) {
                cache = new java.util.HashMap<String, PyList>();
                p.templates = cache;
            }
            PyList template = cache.get(repl);
            if (template == null) {
                if (templateHook == null) {
                    throw Py.runtimeError("re is not initialised");
                }
                Object t = Py.obj(templateHook).call2(pattern, repl);
                if (!(t instanceof PyList)) {
                    throw Py.typeError("re._compile_template did not return a list");
                }
                template = (PyList) t;
                if (cache.size() < TEMPLATES) {
                    cache.put((String) repl, template);
                }
            }
            Object r = Regex.subn(p, (String) string, template, (int) Py.index(countObj));
            return withCount ? r : ((PyTuple) r).arr()[0];
        }
        if (string instanceof String && !(repl instanceof String) && Builtins.isCallable(repl)
                && !checkKind(p, pattern, string)) {
            Object r = Regex.subCall(p, pattern, (String) string, repl, (int) Py.index(countObj));
            return withCount ? r : ((PyTuple) r).arr()[0];
        }
        if (string instanceof PyBytes && checkKind(p, pattern, string)) {
            // A bytes subject runs here too, over its Latin-1 text, as a
            // bytes search does: pdfminer subs every hex string it reads.
            String text = latin1((PyBytes) string);
            Object r = null;
            if (repl instanceof PyBytes) {
                java.util.HashMap<String, PyList> cache = p.byteTemplates;
                if (cache == null) {
                    cache = new java.util.HashMap<String, PyList>();
                    p.byteTemplates = cache;
                }
                String key = latin1((PyBytes) repl);
                PyList template = cache.get(key);
                if (template == null && templateHook != null) {
                    template = textTemplate(Py.obj(templateHook).call2(pattern, repl));
                    if (template != null && cache.size() < TEMPLATES) {
                        cache.put(key, template);
                    }
                }
                if (template != null) {
                    r = Regex.subn(p, text, template, (int) Py.index(countObj));
                }
            } else if (Builtins.isCallable(repl)) {
                r = Regex.subCall(p, pattern, (PyBytes) string, text, repl, (int) Py.index(countObj));
            }
            if (r != null) {
                Object[] items = ((PyTuple) r).arr();
                PyBytes out = new PyBytes(latin1Encode((String) items[0]));
                return withCount ? Py.tuple(out, items[1]) : out;
            }
        }
        if (subHook == null) {
            throw Py.runtimeError("re is not initialised");
        }
        Object r = Py.obj(subHook).call4(pattern, repl, string, countObj);
        return withCount ? r : Py.obj(r).getItem(Long.valueOf(0));
    }

    /// A bytes template from `re._compile_template` with its literal parts
    /// as Latin-1 text, the form [Regex#subn] takes; null for a part of
    /// another kind, which the Python loop handles instead.
    private static PyList textTemplate(Object t) {
        if (!(t instanceof PyList)) {
            return null;
        }
        PyList src = (PyList) t;
        PyList out = Py.list();
        int n = src.size();
        for (int i = 0; i < n; i++) {
            Object part = src.get(i);
            if (part instanceof PyBytes) {
                byte[] v = ((PyBytes) part).value;
                out.append(Codecs.latin1(v, 0, v.length));
            } else if (part instanceof Long) {
                out.append(part);
            } else {
                return null;
            }
        }
        return out;
    }

    /// The bytes of a text whose chars are all below 256 (a bytes subject's
    /// Latin-1 text, with bytes put into it).
    static byte[] latin1Encode(String s) {
        int n = s.length();
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }

    private static RegexMatch self(Object o) {
        return (RegexMatch) o;
    }

    static {
        PyType t = TYPE;
        t.define("group", PyBuiltin.method(t, "group", a -> self(a[0]).group(a))
                .with1(m -> self(m).get(0, Py.NONE))
                .with2((m, g) -> self(m).get(self(m).index(g), Py.NONE)));
        t.define("__getitem__", PyBuiltin.method2(t, "__getitem__", (a0, a1) -> self(a0).getItem(a1))
                .with2((m, g) -> self(m).getItem(g)));
        t.define("groups", PyBuiltin.method(t, "groups",
                a -> self(a[0]).groupsTuple(a.length > 1 ? a[1] : Py.NONE))
                .with1(m -> self(m).groupsTuple(Py.NONE)));
        t.define("groupdict", PyBuiltin.method(t, "groupdict", a -> {
            RegexMatch m = self(a[0]);
            Object dflt = a.length > 1 ? a[1] : Py.NONE;
            PyDict gi = m.groupindex();
            PyDict out = new PyDict();
            for (Object k : gi.keyArray()) {
                out.setItem(k, m.get((int) Py.index(gi.getOrNull(k)), dflt));
            }
            return out;
        }));
        t.define("start", PyBuiltin.method(t, "start",
                a -> Py.of(self(a[0]).regs[2 * (a.length > 1 ? self(a[0]).index(a[1]) : 0)]))
                .with1(m -> Py.of(self(m).regs[0]))
                .with2((m, g) -> Py.of(self(m).regs[2 * self(m).index(g)])));
        t.define("end", PyBuiltin.method(t, "end",
                a -> Py.of(self(a[0]).regs[2 * (a.length > 1 ? self(a[0]).index(a[1]) : 0) + 1]))
                .with1(m -> Py.of(self(m).regs[1]))
                .with2((m, g) -> Py.of(self(m).regs[2 * self(m).index(g) + 1])));
        t.define("span", PyBuiltin.method(t, "span",
                a -> self(a[0]).span(a.length > 1 ? self(a[0]).index(a[1]) : 0))
                .with1(m -> self(m).span(0))
                .with2((m, g) -> self(m).span(self(m).index(g))));
        t.define("expand", PyBuiltin.method2(t, "expand", (a0, a1) -> {
            if (expandHook == null) {
                throw Py.runtimeError("re is not initialised");
            }
            return Py.obj(expandHook).call2(a0, a1);
        }));
        t.define("__copy__", PyBuiltin.method1(t, "__copy__", a0 -> a0));
        t.define("__deepcopy__", PyBuiltin.method1(t, "__deepcopy__", a0 -> a0));
        t.define("__class_getitem__", new PyClassMethod(PyBuiltin.fixed2("__class_getitem__", (a0, a1) -> new PyGenericAlias((PyType) a0, a1))));
        t.define("__module__", "re");
    }
}
