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

/// `_cn1functools`: the parts of `functools` CPython implements in C. The
/// `lru_cache` decorator stays Python, as in CPython, and wraps the function
/// in [LruWrapper] -- the equivalent of C's `_lru_cache_wrapper`, which was a
/// Python closure here: an argument tuple, a `_make_key` call and a pop and
/// re-insert per hit, on functions such as charset_normalizer's per-character
/// `_char_info`.
final class Functools {

    private Functools() {
    }

    static boolean provide(String name) {
        if (!"_cn1functools".equals(name)) {
            return false;
        }
        PyModule m = new PyModule("_cn1functools", new String[0]);
        m.setAttr("_lru_cache_wrapper", PyBuiltin.positional("_lru_cache_wrapper", a -> {
            if (a.length != 4) {
                throw Py.typeError("_lru_cache_wrapper expected 4 arguments, got " + a.length);
            }
            if (!Builtins.isCallable(a[0])) {
                throw Py.typeError("the first argument must be callable");
            }
            long max;
            if (a[1] == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                max = -1;
            } else {
                max = Math.max(0, Py.index(a[1]));
            }
            return new LruWrapper(Py.obj(a[0]), max, Py.isTrue(a[2]), a[3]);
        }));
        PyModule.registerModule(m);
        return true;
    }

    /// The marker between positional and keyword arguments in a key.
    private static final Object KW_MARK = new PyObject() {
        @Override
        public PyType type() {
            return Py.OBJECT_TYPE;
        }
    };

    static final PyType LRU_TYPE = lruType();

    private static PyType lruType() {
        PyType t = new PyType("_lru_cache_wrapper", new PyType[] {Py.OBJECT_TYPE}, (ty, a, k) -> {
            throw Py.typeError("cannot create 'functools._lru_cache_wrapper' instances");
        });
        t.define("__module__", "functools");
        t.define("cache_info", PyBuiltin.method1(t, "cache_info", s -> ((LruWrapper) s).info()));
        t.define("cache_clear", PyBuiltin.method1(t, "cache_clear", s -> {
            ((LruWrapper) s).clear();
            return Py.NONE;
        }));
        t.define("__copy__", PyBuiltin.method1(t, "__copy__", s -> s));
        t.define("__deepcopy__", PyBuiltin.method2(t, "__deepcopy__", (s, memo) -> s));
        return t;
    }

    /// A node of the bounded cache's recency list; the root's `next` is the
    /// least recently used.
    private static final class Link {
        Link prev;
        Link next;
        Object key;
        Object result;
    }

    static final class LruWrapper extends PyObject {
        private final PyObject fn;
        /// -1 when unbounded.
        private final long maxsize;
        private final boolean typed;
        private final Object infoType;
        /// key to result when unbounded, key to [Link] when bounded.
        private final PyDict cache = new PyDict();
        private final Link root = new Link();
        private long hits;
        private long misses;
        private PyDict attrs;

        LruWrapper(PyObject fn, long maxsize, boolean typed, Object infoType) {
            this.fn = fn;
            this.maxsize = maxsize;
            this.typed = typed;
            this.infoType = infoType;
            root.prev = root;
            root.next = root;
        }

        @Override
        public PyType type() {
            return LRU_TYPE;
        }

        Object info() {
            return Py.obj(infoType).call(new Object[] {PyInt.of(hits), PyInt.of(misses),
                maxsize < 0 ? Py.NONE : PyInt.of(maxsize), PyInt.of(cache.len())}, Py.NO_KW);
        }

        void clear() {
            cache.clear();
            root.prev = root;
            root.next = root;
            hits = 0;
            misses = 0;
        }

        /// CPython's `lru_cache_make_key`: a lone `int` or `str` argument is
        /// its own key; otherwise the positional arguments, then a marker and
        /// the keyword pairs, then (typed) every argument's type.
        private Object key(Object[] a, String[] kw) {
            int npos = a.length - kw.length;
            if (kw.length == 0 && !typed) {
                if (npos == 1 && (a[0] instanceof Long || a[0] instanceof String)) {
                    return a[0];
                }
                return PyTuple.of(a.clone());
            }
            int n = npos + (kw.length == 0 ? 0 : 1 + 2 * kw.length) + (typed ? a.length : 0);
            Object[] k = new Object[n];
            System.arraycopy(a, 0, k, 0, npos);
            int j = npos;
            if (kw.length != 0) {
                k[j++] = KW_MARK;
                for (int i = 0; i < kw.length; i++) {
                    k[j++] = kw[i];
                    k[j++] = a[npos + i];
                }
            }
            if (typed) {
                for (Object v : a) {
                    k[j++] = Py.typeOf(v);
                }
            }
            return PyTuple.of(k);
        }

        private Object key1(Object a) {
            if (!typed && (a instanceof Long || a instanceof String)) {
                return a;
            }
            return typed ? PyTuple.of(a, Py.typeOf(a)) : PyTuple.of(a);
        }

        @Override
        public Object call(Object[] args, String[] kwNames) {
            if (maxsize == 0) {
                misses++;
                return fn.call(args, kwNames);
            }
            Object k = key(args, kwNames);
            Object hit = lookup(k);
            if (hit != null) {
                return hit;
            }
            misses++;
            return store(k, fn.call(args, kwNames));
        }

        @Override
        public Object call0() {
            return call(Py.NO_ARGS, Py.NO_KW);
        }

        @Override
        public Object call1(Object a) {
            if (maxsize == 0) {
                misses++;
                return fn.call1(a);
            }
            Object k = key1(a);
            Object hit = lookup(k);
            if (hit != null) {
                return hit;
            }
            misses++;
            return store(k, fn.call1(a));
        }

        @Override
        public Object call2(Object a, Object b) {
            if (maxsize == 0 || typed) {
                return call(new Object[] {a, b}, Py.NO_KW);
            }
            Object k = Py.tuple(a, b);
            Object hit = lookup(k);
            if (hit != null) {
                return hit;
            }
            misses++;
            return store(k, fn.call2(a, b));
        }

        /// The cached result, counted as a hit, or null.
        private Object lookup(Object k) {
            // Hashed here rather than by the dict, whose message would name
            // the key tuple: CPython reports only the unhashable argument.
            long h = k instanceof Long || k instanceof String ? Py.keyHash(k, "") : Py.obj(k).hash();
            Object v = cache.getOrNullHashed(k, h);
            if (v == null) {
                return null;
            }
            hits++;
            if (maxsize < 0) {
                return v;
            }
            Link l = (Link) v;
            if (root.prev != l) { //NOPMD CompareObjectsWithEquals - identity is meant
                l.prev.next = l.next;
                l.next.prev = l.prev;
                l.prev = root.prev;
                l.next = root;
                root.prev.next = l;
                root.prev = l;
            }
            return l.result;
        }

        /// CPython's insertion: a call that cached its own key while it ran
        /// keeps that entry; a full cache gives its oldest link to the new key.
        private Object store(Object k, Object result) {
            if (maxsize < 0) {
                cache.setItem(k, result);
                return result;
            }
            if (cache.getOrNull(k) != null) {
                return result;
            }
            Link l;
            if (cache.len() >= maxsize) {
                l = root.next;
                l.prev.next = l.next;
                l.next.prev = l.prev;
                cache.delItem(l.key);
            } else {
                l = new Link();
            }
            l.key = k;
            l.result = result;
            l.prev = root.prev;
            l.next = root;
            root.prev.next = l;
            root.prev = l;
            cache.setItem(k, l);
            return result;
        }

        @Override
        public Object bind(Object self) {
            return new PyMethod(self, this);
        }

        @Override
        public Object getAttrOrNull(String name) {
            if (attrs != null) {
                Object v = attrs.getOrNull(name);
                if (v != null) {
                    return v;
                }
            }
            if ("__dict__".equals(name)) {
                if (attrs == null) {
                    attrs = new PyDict();
                }
                return attrs;
            }
            if ("__wrapped__".equals(name)) {
                return fn;
            }
            return super.getAttrOrNull(name);
        }

        @Override
        public void setAttr(String name, Object value) {
            if (attrs == null) {
                attrs = new PyDict();
            }
            attrs.setItem(name, value);
        }

        @Override
        public void delAttr(String name) {
            if (attrs == null || !attrs.contains(name)) {
                throw Py.attributeError("'functools._lru_cache_wrapper' object has no attribute '" + name + "'");
            }
            attrs.delItem(name);
        }
    }
}
