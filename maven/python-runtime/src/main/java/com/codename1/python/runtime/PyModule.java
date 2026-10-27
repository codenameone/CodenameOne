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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/// A Python module.
///
/// Every global the transpiler can see being assigned gets a fixed slot, and
/// generated code reads and writes `slots[i]` directly instead of hashing the
/// name. Names created at run time (`setattr(module, ...)`, `globals()[k]`)
/// go to an overflow map. Modules register a loader by name, so `import`
/// never looks a class up by reflection.
public final class PyModule extends PyObject {

    /// Runs a module body, filling its globals.
    public interface Loader {
        void exec(PyModule m);
    }

    private static final Map<String, Loader> LOADERS = new HashMap<String, Loader>();
    private static final Map<String, PyModule> LOADED = new HashMap<String, PyModule>();
    /// `sys.modules`: every module as it starts loading. A program may read
    /// and change it, but importing goes by the loaded table alone.
    static final PyDict SYS_MODULES = new PyDict();

    public final String name;
    public final Object[] slots;
    private final String[] slotNames;
    private Map<String, Object> extra;

    public PyModule(String name, String[] slotNames) {
        this.name = name;
        this.slotNames = slotNames;
        this.slots = new Object[slotNames.length];
    }

    /// Registers `loader` as the implementation of module `name`.
    public static void register(String name, Loader loader) {
        LOADERS.put(name, loader);
    }

    /// Registers an already populated module (runtime-provided modules).
    public static void registerModule(PyModule m) {
        LOADED.put(m.name, m);
        SYS_MODULES.setItem(m.name, m);
    }

    static PyModule find(String name) {
        PyModule m = LOADED.get(name);
        if (m == null && LOADERS.containsKey(name)) {
            m = load(name);
        }
        return m;
    }

    /// `import name`: runs the module body once, parents first.
    public static PyModule load(String name) {
        PyModule m = LOADED.get(name);
        if (m != null) {
            return m;
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            load(name.substring(0, dot));
        }
        m = LOADED.get(name);
        if (m != null) {
            return m;
        }
        Loader l = LOADERS.get(name);
        if (l == null) {
            if (StdModules.provide(name)) {
                m = LOADED.get(name);
                if (m != null) {
                    return m;
                }
            }
            throw Py.error(Py.MODULE_NOT_FOUND_ERROR, "No module named '" + name + "'");
        }
        l.exec(null);
        m = LOADED.get(name);
        if (m == null) {
            throw Py.error(Py.IMPORT_ERROR, "module '" + name + "' did not initialise");
        }
        return m;
    }

    /// Called by a generated module body before it runs, so a circular import
    /// sees the partially initialised module as CPython does.
    public static PyModule begin(String name, String[] slotNames) {
        PyModule m = new PyModule(name, slotNames);
        LOADED.put(name, m);
        SYS_MODULES.setItem(name, m);
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            PyModule parent = LOADED.get(name.substring(0, dot));
            if (parent != null) {
                parent.setAttr(name.substring(dot + 1), m);
            }
        }
        return m;
    }

    @Override
    public PyType type() {
        return Py.MODULE_TYPE;
    }

    private int slotOf(String n) {
        for (int i = 0; i < slotNames.length; i++) {
            if (slotNames[i].equals(n)) {
                return i;
            }
        }
        return -1;
    }

    /// A global the generated code reads that was never assigned: the
    /// builtin of that name when there is one (a module may shadow `property`
    /// and still read the builtin before its own assignment), else NameError.
    public Object unbound(int slot) {
        Object v = Builtins.lookup(slotNames[slot]);
        if (v == null) {
            throw Py.nameError(slotNames[slot]);
        }
        return v;
    }

    @Override
    public Object getAttrOrNull(String n) {
        int s = slotOf(n);
        if (s >= 0 && slots[s] != null) {
            return slots[s];
        }
        if (extra != null) {
            Object v = extra.get(n);
            if (v != null) {
                return v;
            }
        }
        if ("__name__".equals(n)) {
            return Py.str(name);
        }
        return null;
    }

    @Override
    public void setAttr(String n, Object value) {
        int s = slotOf(n);
        if (s >= 0) {
            slots[s] = value;
            return;
        }
        if (extra == null) {
            extra = new LinkedHashMap<String, Object>();
        }
        extra.put(n, value);
    }

    @Override
    public void delAttr(String n) {
        int s = slotOf(n);
        if (s >= 0 && slots[s] != null) {
            slots[s] = null;
            return;
        }
        if (extra == null || extra.remove(n) == null) {
            throw Py.attributeError("module '" + name + "' has no attribute '" + n + "'");
        }
    }

    /// `globals()`: a snapshot, since the slots are not a dict.
    public PyDict globals() {
        PyDict d = new PyDict();
        d.setItem(Py.str("__name__"), Py.str(name));
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                d.setItem(Py.str(slotNames[i]), slots[i]);
            }
        }
        if (extra != null) {
            for (Map.Entry<String, Object> e : extra.entrySet()) {
                d.setItem(Py.str(e.getKey()), e.getValue());
            }
        }
        return d;
    }

    /// The names `from m import *` copies: `__all__` when defined, else every
    /// public global.
    public String[] exportedNames() {
        Object all = getAttrOrNull("__all__");
        if (all != null) {
            Object[] items = Py.toArray(all);
            String[] r = new String[items.length];
            for (int i = 0; i < items.length; i++) {
                r[i] = Py.toStr(items[i]);
            }
            return r;
        }
        java.util.List<String> r = new java.util.ArrayList<String>();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && !slotNames[i].startsWith("_")) {
                r.add(slotNames[i]);
            }
        }
        if (extra != null) {
            for (String k : extra.keySet()) {
                if (!k.startsWith("_")) {
                    r.add(k);
                }
            }
        }
        return r.toArray(new String[r.size()]);
    }

    @Override
    public String repr() {
        return "<module '" + name + "'>";
    }
}
