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
    /// The modules whose bodies are running, innermost last; the main module
    /// is not on it, since `load` never runs it.
    private static final java.util.ArrayList<String> EXECUTING = new java.util.ArrayList<String>();

    public final String name;
    public final Object[] slots;
    private final String[] slotNames;
    private Map<String, Object> extra;

    /// The loaded module whose translated class is `javaClass` or encloses
    /// it (a function body may be a nested or a lambda class), or null. The
    /// longest matching module class wins, so `a.b` is never taken for `a`.
    static PyModule ofClass(String javaClass) {
        int dot = javaClass.lastIndexOf('.');
        String simple = javaClass.substring(dot + 1);
        PyModule best = null;
        int bestLength = -1;
        for (PyModule m : LOADED.values()) {
            String c = className(m.name);
            if (c.length() > bestLength && (simple.equals(c) || simple.startsWith(c + "$"))) {
                best = m;
                bestLength = c.length();
            }
        }
        return best;
    }

    /// The module whose body is running innermost -- the main module when no
    /// import is under way -- or null.
    static PyModule executing() {
        String n = EXECUTING.isEmpty() ? "__main__" : EXECUTING.get(EXECUTING.size() - 1);
        return LOADED.get(n);
    }

    /// The loaded module whose class, or a class nested in it, a native
    /// ParparVM stack frame names. Such a frame carries the translated C name,
    /// in which every `.` and `$` of the Java name is a `_`
    /// (`com_..._py_anyio_2e_core` for `py_anyio$2ecore`), so the module
    /// classes are compared in that form, the longest match winning.
    static PyModule ofNativeClass(String cName) {
        PyModule best = null;
        int bestLength = -1;
        for (PyModule m : LOADED.values()) {
            String c = "_" + className(m.name).replace('$', '_');
            int at = cName.lastIndexOf(c);
            while (at >= 0) {
                int end = at + c.length();
                if (end == cName.length() || cName.charAt(end) == '_') {
                    if (c.length() > bestLength) {
                        best = m;
                        bestLength = c.length();
                    }
                    break;
                }
                at = cName.lastIndexOf(c, at - 1);
            }
        }
        return best;
    }

    /// `PythonTranspiler.className`, which names a module's class.
    private static String className(String module) {
        StringBuilder sb = new StringBuilder("py_");
        for (int i = 0; i < module.length(); i++) {
            char c = module.charAt(i);
            if (c < 128 && (Character.isLetterOrDigit(c) || c == '_')) {
                sb.append(c);
            } else {
                sb.append('$').append(Integer.toHexString(c));
            }
        }
        return sb.toString();
    }

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

    /// Whether `name` can be imported: compiled into the program, already
    /// loaded, or provided by the runtime itself.
    public static boolean available(String name) {
        if (LOADED.containsKey(name) || LOADERS.containsKey(name)) {
            return true;
        }
        return StdModules.provide(name) && LOADED.containsKey(name);
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
        EXECUTING.add(name);
        try {
            l.exec(null);
        } finally {
            EXECUTING.remove(EXECUTING.size() - 1);
        }
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
        Object v = getGlobal(n);
        return v != null ? v : moduleGetattr(n);
    }

    /// A name the module defines, as its own code reads a global: never
    /// through the module's `__getattr__`, which serves only attribute
    /// access from outside.
    public Object getGlobal(String n) {
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
        if ("__dict__".equals(n)) {
            // As globals(): writes through it reach the module.
            return globals();
        }
        return null;
    }

    /// PEP 562: a name the module does not define goes to the module's own
    /// `__getattr__`, when it has one -- how packages load submodules and
    /// exports lazily. Its AttributeError means "no such name".
    private Object moduleGetattr(String n) {
        if ("__getattr__".equals(n)) {
            return null;
        }
        int s = slotOf("__getattr__");
        Object hook = s >= 0 ? slots[s] : extra == null ? null : extra.get("__getattr__");
        if (hook == null) {
            return null;
        }
        try {
            return Py.call(hook, Py.str(n));
        } catch (PyException e) {
            if (e.matches(Py.ATTRIBUTE_ERROR)) {
                return null;
            }
            throw e;
        }
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
    /// `globals()` (and a module's `__dict__`, and `locals()` at module
    /// level): a dict of the module's names whose writes reach the module,
    /// so `globals()[name] = value` defines `name` -- the idiom libraries
    /// export generated names with. Reads see the names as they were when
    /// it was made, plus what was written through it.
    public PyDict globals() {
        PyDict d = new Globals(this);
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
        ((Globals) d).filled = true;
        return d;
    }

    /// A module's globals dict, writing through to the module.
    private static final class Globals extends PyDict {
        private final PyModule module;
        private boolean filled;

        Globals(PyModule module) {
            this.module = module;
        }

        @Override
        public void setItem(Object key, Object value) {
            super.setItem(key, value);
            if (filled && key instanceof String) {
                module.setAttr(Py.toStr(key), value);
            }
        }

        @Override
        public Object remove(Object key) {
            Object v = super.remove(key);
            if (v != null && key instanceof String && module.getAttrOrNull(Py.toStr(key)) != null) {
                module.delAttr(Py.toStr(key));
            }
            return v;
        }
    }

    /// The names `from m import *` copies: `__all__` when defined, else every
    /// public global.
    public String[] exportedNames() {
        Object all = getGlobal("__all__");
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
