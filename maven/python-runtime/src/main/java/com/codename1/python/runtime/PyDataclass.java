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

/// `_cn1dataclass`: the `__init__` of a dataclass.
///
/// CPython writes that method as source, one assignment a field, and compiles
/// it. Here `dataclasses` has a plan of the fields instead, and walking it in
/// Python for every instance cost several times what the assignments did: a
/// library that makes a small dataclass per token of what it parses spent
/// more on these than on parsing.
///
/// [#init] answers a function that binds the arguments and stores the fields.
/// It only handles a call that is right. One with too many arguments, a
/// keyword nothing takes, a field given twice or a required field left out
/// goes, before anything is stored or any default factory called, to the
/// Python `__init__` it was given, which raises what CPython raises.
final class PyDataclass {

    private PyDataclass() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1dataclass", new String[0]);
        // init(qualname, names, positions, takes, kinds, fallbacks, frozen, slow): one entry
        // a field in the five tuples -- its name, its place among the positional
        // arguments or -1, whether a caller may pass it, and what it is when the
        // caller did not (2 a factory to call, 1 a default, 0 nothing).
        m.setAttr("init", PyBuiltin.positional("init", a -> {
            Object[] names = Py.toArray(a[1]);
            Object[] positions = Py.toArray(a[2]);
            Object[] takes = Py.toArray(a[3]);
            Object[] kinds = Py.toArray(a[4]);
            int n = names.length;
            String[] name = new String[n];
            int[] position = new int[n];
            boolean[] taken = new boolean[n];
            int[] kind = new int[n];
            int positional = 0;
            for (int i = 0; i < n; i++) {
                name[i] = Py.toStr(names[i]);
                position[i] = (int) Py.asLong(positions[i]);
                taken[i] = Py.isTrue(takes[i]);
                kind[i] = (int) Py.asLong(kinds[i]);
                if (position[i] >= 0) {
                    positional++;
                }
            }
            return new PyBuiltin("__init__", new Init(name, position, taken, kind, Py.toArray(a[5]), positional,
                    Py.isTrue(a[6]), Py.obj(a[7]))).qual(Py.toStr(a[0]));
        }));
        return m;
    }

    /// The generated `__init__` behind `init`, or null when `init` is anything else.
    static Init initOf(Object init) {
        if (init instanceof PyBuiltin && ((PyBuiltin) init).impl instanceof Init) {
            return (Init) ((PyBuiltin) init).impl;
        }
        return null;
    }

    static final class Init implements PyBuiltin.Impl {
        private final String[] name;
        private final int[] position;
        private final boolean[] taken;
        private final int[] kind;
        private final Object[] fallback;
        private final int positional;
        private final boolean frozen;
        private final PyObject slow;

        Init(String[] name, int[] position, boolean[] taken, int[] kind, Object[] fallback, int positional,
                boolean frozen, PyObject slow) {
            this.name = name;
            this.position = position;
            this.taken = taken;
            this.kind = kind;
            this.fallback = fallback;
            this.positional = positional;
            this.frozen = frozen;
            this.slow = slow;
        }

        @Override
        public Object call(Object[] args, String[] kwNames) {
            int given = args.length - 1 - kwNames.length;
            if (given < 0 || given > positional || !(args[0] instanceof PyInstance)) {
                return slow.call(args, kwNames);
            }
            int n = name.length;
            if (kwNames.length == 0) {
                // Nothing to match by name: a field's value is where its position says.
                if (!positional(given)) {
                    return slow.call(args, kwNames);
                }
                PyInstance to = (PyInstance) args[0];
                for (int i = 0; i < n; i++) {
                    if (position[i] >= 0 && position[i] < given) {
                        store(to, i, args[1 + position[i]]);
                    } else {
                        storeDefault(to, i);
                    }
                }
                return Py.NONE;
            }
            // Where each field's value is in args: 0 for a field the caller left out.
            int[] at = new int[n];
            int matched = 0;
            for (int i = 0; i < n; i++) {
                if (position[i] >= 0 && position[i] < given) {
                    at[i] = 1 + position[i];
                    for (String k : kwNames) {
                        if (k.equals(name[i])) {
                            return slow.call(args, kwNames);
                        }
                    }
                    continue;
                }
                if (taken[i]) {
                    for (int k = 0; k < kwNames.length; k++) {
                        if (kwNames[k].equals(name[i])) {
                            at[i] = 1 + given + k;
                            matched++;
                            break;
                        }
                    }
                }
                if (at[i] == 0 && kind[i] == 0 && taken[i]) {
                    return slow.call(args, kwNames);
                }
            }
            if (matched != kwNames.length) {
                return slow.call(args, kwNames);
            }
            PyInstance self = (PyInstance) args[0];
            for (int i = 0; i < n; i++) {
                Object v;
                if (at[i] != 0) {
                    v = args[at[i]];
                } else if (kind[i] == 2) {
                    v = Py.obj(fallback[i]).call0();
                } else if (kind[i] == 1) {
                    v = fallback[i];
                } else {
                    continue;
                }
                if (frozen) {
                    self.setAttrObject(name[i], v);
                } else {
                    self.setAttr(name[i], v);
                }
            }
            return Py.NONE;
        }

        /// Whether `given` positional arguments and no keywords are a call this
        /// method answers itself: no more than it takes, and every field they do
        /// not reach has a default.
        boolean positional(int given) {
            if (given < 0 || given > positional) {
                return false;
            }
            for (int i = 0; i < name.length; i++) {
                if (!(position[i] >= 0 && position[i] < given) && kind[i] == 0 && taken[i]) {
                    return false;
                }
            }
            return true;
        }

        /// The fields of `self` from at most five positional arguments, for a
        /// constructor call that has no argument array. The caller has asked
        /// [#positional(int)] first.
        void fill(PyInstance self, int given, Object a, Object b, Object c, Object d, Object e) {
            for (int i = 0; i < name.length; i++) {
                int p = position[i];
                if (p >= 0 && p < given) {
                    store(self, i, p == 0 ? a : p == 1 ? b : p == 2 ? c : p == 3 ? d : e);
                } else {
                    storeDefault(self, i);
                }
            }
        }

        private void storeDefault(PyInstance self, int i) {
            if (kind[i] == 2) {
                store(self, i, Py.obj(fallback[i]).call0());
            } else if (kind[i] == 1) {
                store(self, i, fallback[i]);
            }
        }

        private void store(PyInstance self, int i, Object v) {
            if (frozen) {
                self.setAttrObject(name[i], v);
            } else {
                self.setAttr(name[i], v);
            }
        }
    }
}
