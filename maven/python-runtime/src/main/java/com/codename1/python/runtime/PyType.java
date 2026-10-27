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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A Python type: a builtin (`int`, `list`, `ValueError`) or a class the
/// application defines.
///
/// Attribute lookup walks the C3 method resolution order, which is computed
/// once when the type is created. Calling a type constructs an instance: a
/// builtin type through its [Factory], a class through `__new__`/`__init__`.
public final class PyType extends PyObject {

    /// How a builtin type builds its instances from call arguments.
    public interface Factory {
        Object create(PyType type, Object[] args, String[] kwNames);
    }

    public String name;
    public final PyType[] bases;
    final PyType[] mro;
    final Map<String, Object> dict = new LinkedHashMap<String, Object>();

    /// Bumped by every change to any class namespace. Inline caches
    /// (`AttrSite`) and the lookup cache below compare against it, so nothing
    /// cached outlives the class state it was read from. Changing a class
    /// after it is built is rare, so one counter for all of them costs
    /// nothing in practice and needs no list of subclasses.
    static int version;

    private static final Object MISSING = new PyObject() {
        @Override
        public PyType type() {
            return Py.OBJECT_TYPE;
        }
    };
    private Map<String, Object> lookupCache;
    private int lookupVersion = -1;
    Factory factory;
    /// The builtin type whose storage instances of this class extend, or `null`.
    final PyType solidBase;
    /// The class's metaclass: `type`, or a subclass of it (null while `type`
    /// itself is being built, which is its own metaclass).
    PyType meta;

    /// The direct subclasses, for `type.__subclasses__()`; null until one exists.
    private List<PyType> subclasses;

    PyType(String name, PyType[] bases, Factory factory) {
        this.name = name;
        this.bases = bases;
        this.factory = factory;
        this.mro = computeMro(this, bases);
        for (int i = 0; i < bases.length; i++) {
            if (bases[i].subclasses == null) {
                bases[i].subclasses = new ArrayList<PyType>();
            }
            bases[i].subclasses.add(this);
        }
        PyType solid = null;
        for (int i = 1; i < mro.length && solid == null; i++) {
            if (mro[i].factory != null && mro[i] != Py.OBJECT_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                solid = mro[i];
            }
        }
        this.solidBase = solid;
        this.meta = Py.TYPE_TYPE;
    }

    /// Builds the Java object behind an instance of a class the transpiler
    /// compiled, whose attributes are fields of a generated subclass of
    /// `PyInstance` (see `PyInstance.fieldIndex`).
    public interface Layout {
        PyInstance create(PyType type);
    }

    private Layout layout;
    private String[] compiledMethods;
    private String[] layoutFields;
    /// Set once a method the generated code calls directly may no longer be
    /// what the class resolves that name to: a class in its MRO was given,
    /// or lost, an attribute of that name after it was built. The generated
    /// dispatch then goes the generic way for good. Kept per class, so
    /// patching one class leaves the others on the fast path.
    public boolean patched;
    private static final List<PyType> LAYOUTS = new ArrayList<PyType>();

    /// Gives this class a compiled layout. `methods` are the names whose
    /// functions generated code calls without a lookup; `fields` the
    /// attributes that live in fields. Only this exact class uses the layout:
    /// a subclass built at run time gets ordinary instances, so a compiled
    /// method can never run for a class that overrides it unseen.
    public PyType layout(Layout l, String[] methods, String[] fields) {
        layout = l;
        compiledMethods = methods;
        layoutFields = fields;
        LAYOUTS.add(this);
        return this;
    }

    /// The compiled class this one's layout extends, or null.
    private PyType layoutParent() {
        return bases.length == 1 && bases[0].layout != null ? bases[0] : null;
    }

    /// Whether the class has a compiled layout.
    public boolean hasLayout() {
        return layout != null;
    }

    /// A new, uninitialised instance.
    public PyInstance newInstance() {
        PyInstance o = layout != null ? layout.create(this) : new PyInstance(this);
        if (solidBase != null && BuiltinSubclass.mutable(solidBase)) {
            // Filled by the builtin's __init__ (or the subclass's, through super()).
            o.base = solidBase.factory.create(solidBase, Py.NO_ARGS, Py.NO_KW);
        }
        return o;
    }

    /// Invalidates direct calls to `attr` and refuses what a compiled field
    /// cannot honour, once the class namespace of `this` changes.
    private void changed(String attr, Object value) {
        for (int i = 0; i < LAYOUTS.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
            PyType t = LAYOUTS.get(i);
            if (!t.isSubtype(this)) {
                continue;
            }
            if ("__setattr__".equals(attr) || "__getattribute__".equals(attr) || "__delattr__".equals(attr)
                    || value instanceof PyProperty && contains(t.layoutFields, attr)) {
                // Generated code stores and reads the fields of these
                // instances directly. The transpiler leaves a field to the
                // generic path when the module itself assigns that name on
                // the class (Layouts.plan); this is the case it cannot see.
                throw Py.typeError("cannot add '" + attr + "' to class '" + t.name
                        + "' after it is built: its attributes are compiled");
            }
            // A compiled method t inherits counts as much as its own.
            for (PyType u = t; u != null && !t.patched; u = u.layoutParent()) {
                if (contains(u.compiledMethods, attr)) {
                    t.patched = true;
                }
            }
        }
    }

    private static boolean contains(String[] a, String s) {
        if (a != null) {
            for (String e : a) {
                if (e.equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Creates a class the application defines, `class name(bases): ...`.
    /// The generated code fills the namespace afterwards with [#define].
    public static PyType newClass(String name, Object[] bases) {
        Object[] original = bases;
        bases = mroEntries(bases);
        PyType[] b;
        if (bases.length == 0) {
            b = new PyType[] {Py.OBJECT_TYPE};
        } else {
            b = new PyType[bases.length];
            for (int i = 0; i < bases.length; i++) {
                if (!(bases[i] instanceof PyType)) {
                    throw Py.typeError("bases must be types");
                }
                b[i] = (PyType) bases[i];
                PyType solid = b[i].factory != null && b[i] != Py.OBJECT_TYPE ? b[i] : b[i].solidBase; //NOPMD CompareObjectsWithEquals - identity is meant
                if (solid != null && solid != Py.TYPE_TYPE && !BuiltinSubclass.subclassable(solid)) { //NOPMD CompareObjectsWithEquals - identity is meant
                    throw Py.typeError("type '" + solid.name + "' is not an acceptable base type");
                }
            }
        }
        PyType t = new PyType(name, b, null);
        if (original != bases) { //NOPMD CompareObjectsWithEquals - identity is meant
            t.dict.put("__orig_bases__", PyTuple.of(original));
        }
        for (PyType e : b) {
            PyType s = e.factory != null && e != Py.OBJECT_TYPE ? e : e.solidBase; //NOPMD CompareObjectsWithEquals - identity is meant
            if (s != null && s != t.solidBase && !t.solidBase.isSubtype(s)) { //NOPMD CompareObjectsWithEquals - identity is meant
                throw Py.typeError("multiple bases have instance lay-out conflict");
            }
        }
        return t;
    }

    /// `bases` with each non-class base replaced by what its
    /// `__mro_entries__` answers (`typing.Generic[T]`, `NamedTuple`), or
    /// `bases` itself when every entry is a class.
    static Object[] mroEntries(Object[] bases) {
        List<Object> out = null;
        for (int i = 0; i < bases.length; i++) {
            Object base = bases[i];
            Object hook = base instanceof PyType ? null : Py.obj(base).getAttrOrNull("__mro_entries__");
            if (hook == null) {
                if (out != null) {
                    out.add(base);
                }
                continue;
            }
            if (out == null) {
                out = new ArrayList<Object>();
                for (int j = 0; j < i; j++) {
                    out.add(bases[j]);
                }
            }
            Object entries = Py.obj(hook).call(new Object[] {PyTuple.of(bases)}, Py.NO_KW);
            if (!(entries instanceof PyTuple)) {
                throw Py.typeError("__mro_entries__ must return a tuple");
            }
            Object[] items = ((PyTuple) entries).items;
            for (Object item : items) {
                if (!out.contains(item)) {
                    out.add(item);
                }
            }
        }
        return out == null ? bases : out.toArray();
    }

    /// Stores an attribute in the class namespace.
    /// The class namespace's own binding of `attr`, without the MRO: what a
    /// name read inside a class body sees.
    private Set<String> slotNames;
    private boolean slotsKnown;

    /// The only attributes an instance may hold, when every class in the
    /// MRO declares `__slots__` and none of them names `__dict__`; null when
    /// instances have a `__dict__`.
    Set<String> slotNames() {
        if (!slotsKnown) {
            slotNames = computeSlots();
            slotsKnown = true;
        }
        return slotNames;
    }

    private Set<String> computeSlots() {
        Set<String> r = new HashSet<String>();
        for (PyType t : mro) {
            if (t == Py.OBJECT_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                continue;
            }
            Object s = t.dict.get("__slots__");
            if (s == null) {
                return null;
            }
            Object[] names = s instanceof String ? new Object[] {s} : Py.toArray(s);
            for (Object n : names) {
                String name = Py.toStr(n);
                if ("__dict__".equals(name)) {
                    return null;
                }
                r.add(name);
            }
        }
        return r;
    }

    public Object own(String attr) {
        return dict.get(attr);
    }

    public PyType define(String attr, Object value) {
        version++;
        dict.put(attr, value);
        return this;
    }

    @Override
    public PyType type() {
        return meta != null ? meta : Py.TYPE_TYPE;
    }

    /// Whether the metaclass is a class the application defines.
    private boolean customMeta() {
        return meta != null && meta != Py.TYPE_TYPE; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// The metaclass's own dunder `name`, or null when the metaclass is
    /// `type` or does not override it.
    private Object metaDunder(String name) {
        if (!customMeta()) {
            return null;
        }
        Object m = meta.lookup(name);
        return m == null || m instanceof PyBuiltin ? null : m;
    }

    /// `isinstance(o, this)`, through the metaclass's `__instancecheck__`
    /// when it defines one (what `abc.ABCMeta` registration needs).
    boolean instanceCheck(Object o) {
        if (Py.typeOf(o).isSubtype(this)) {
            return true;
        }
        Object m = metaDunder("__instancecheck__");
        return m != null && Py.isTrue(Py.obj(m).call(new Object[] {this, o}, Py.NO_KW));
    }

    /// `issubclass(c, this)`, through the metaclass's `__subclasscheck__`.
    boolean subclassCheck(PyType c) {
        Object m = metaDunder("__subclasscheck__");
        if (m != null) {
            return Py.isTrue(Py.obj(m).call(new Object[] {this, c}, Py.NO_KW));
        }
        return c.isSubtype(this);
    }

    // ---- class creation ----------------------------------------------------

    /// Class namespaces handed to a metaclass whose class object already
    /// exists: the generated code builds a class before its metaclass runs,
    /// so `type.__new__` adopts that object instead of making another.
    /// Pairs of namespace and class, innermost last.
    private static final List<Object> PROVISIONAL = new ArrayList<Object>();

    /// The most derived of `explicit` and the metaclasses of `bases`.
    static PyType winner(PyType explicit, PyType[] bases) {
        PyType w = explicit;
        for (PyType base : bases) {
            PyType m = base.type();
            if (w.isSubtype(m)) {
                continue;
            }
            if (m.isSubtype(w)) {
                w = m;
                continue;
            }
            throw Py.typeError("metaclass conflict: the metaclass of a derived class must be a (non-strict) "
                    + "subclass of the metaclasses of all its bases");
        }
        return w;
    }

    /// Completes `class k(..., **kw)` once its body has filled the namespace:
    /// runs the metaclass, `__set_name__` and `__init_subclass__`. Returns
    /// what the class name is bound to, which a metaclass may replace.
    /// `kwNames` has null for a `**mapping` entry.
    public static Object finish(PyType k, String[] kwNames, Object[] kwValues) {
        List<String> names = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();
        Object explicit = null;
        for (int i = 0; i < kwNames.length; i++) {
            if (kwNames[i] == null) {
                PyDict d = Py.toDict(kwValues[i]);
                Object[] keys = d.keyArray();
                for (Object key : keys) {
                    addKw(names, values, Py.toStr(key), d.getItem(key));
                }
            } else {
                addKw(names, values, kwNames[i], kwValues[i]);
            }
        }
        int mi = names.indexOf("metaclass");
        if (mi >= 0) {
            explicit = values.remove(mi);
            names.remove(mi);
        }
        String[] kn = names.toArray(new String[names.size()]);
        Object[] kv = values.toArray(new Object[values.size()]);
        k.implicitClassMethods();
        if (explicit != null && !(explicit instanceof PyType)) {
            // A plain callable as the metaclass is simply called.
            return callMeta(Py.obj(explicit), k, kn, kv);
        }
        PyType w = winner(explicit == null ? Py.TYPE_TYPE : (PyType) explicit, k.bases);
        if (w == Py.TYPE_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
            k.created(kn, kv);
            return k;
        }
        return callMeta(w, k, kn, kv);
    }

    private static void addKw(List<String> names, List<Object> values, String n, Object v) {
        if (names.contains(n)) {
            throw Py.typeError("__build_class__() got multiple values for keyword argument '" + n + "'");
        }
        names.add(n);
        values.add(v);
    }

    private static Object callMeta(PyObject meta, PyType k, String[] kn, Object[] kv) {
        PyDict ns = k.namespace();
        Object[] bs = new Object[k.bases.length];
        System.arraycopy(k.bases, 0, bs, 0, bs.length);
        Object[] args = new Object[3 + kv.length];
        args[0] = k.name;
        args[1] = new PyTuple(bs);
        args[2] = ns;
        System.arraycopy(kv, 0, args, 3, kv.length);
        PROVISIONAL.add(ns);
        PROVISIONAL.add(k);
        try {
            return meta.call(args, kn);
        } finally {
            PROVISIONAL.remove(PROVISIONAL.size() - 1);
            PROVISIONAL.remove(PROVISIONAL.size() - 1);
        }
    }

    /// The class namespace as a dict, as a metaclass receives it.
    private PyDict namespace() {
        PyDict ns = new PyDict();
        for (Map.Entry<String, Object> e : dict.entrySet()) {
            ns.setItem(Py.str(e.getKey()), e.getValue());
        }
        return ns;
    }

    /// `type.__new__(mcs, name, bases, ns, **kw)`.
    private static boolean sameBases(PyType k, Object[] bases) {
        Object[] want = mroEntries(bases);
        if (want.length == 0) {
            return k.bases.length == 1 && k.bases[0] == Py.OBJECT_TYPE; //NOPMD CompareObjectsWithEquals - identity is meant
        }
        if (want.length != k.bases.length) {
            return false;
        }
        for (int i = 0; i < want.length; i++) {
            if (want[i] != k.bases[i]) { //NOPMD CompareObjectsWithEquals - identity is meant
                return false;
            }
        }
        return true;
    }

    static Object typeNew(Object[] args, String[] kw) {
        int npos = args.length - kw.length;
        if (npos < 1 || !(args[0] instanceof PyType)) {
            throw Py.notATypeObject("type", args);
        }
        PyType mcs = (PyType) args[0];
        if (!mcs.isSubtype(Py.TYPE_TYPE)) {
            throw Py.typeError("type.__new__(" + mcs.name + "): " + mcs.name + " is not a subtype of type");
        }
        if (npos == 2 && kw.length == 0 && mcs == Py.TYPE_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return Py.typeOf(args[1]);
        }
        if (npos != 4) {
            throw Py.typeError("type.__new__() takes exactly 3 arguments (" + (npos - 1) + " given)");
        }
        PyDict ns = Py.toDict(args[3]);
        String[] kn = kw;
        Object[] kv = new Object[kw.length];
        System.arraycopy(args, npos, kv, 0, kw.length);
        PyType k = null;
        for (int i = PROVISIONAL.size() - 2; i >= 0 && k == null; i -= 2) {
            if (PROVISIONAL.get(i) == ns) { //NOPMD CompareObjectsWithEquals - identity is meant
                k = (PyType) PROVISIONAL.get(i + 1);
            }
        }
        if (k != null && !sameBases(k, Py.toArray(args[2]))) {
            // The metaclass asked for other bases (TypedDict makes a dict):
            // that is a different class, not the one the code built.
            k = null;
        }
        Object[] keys = ns.keyArray();
        if (k != null) {
            // The class the generated code built: adopt the namespace the
            // metaclass may have changed.
            List<String> keep = new ArrayList<String>();
            for (Object key : keys) {
                keep.add(Py.toStr(key));
            }
            Object[] old = k.dict.keySet().toArray();
            for (Object x : old) {
                if (!keep.contains(x)) {
                    k.dict.remove(x);
                }
            }
        } else {
            k = newClass(Py.toStr(args[1]), Py.toArray(args[2]));
        }
        for (Object key : keys) {
            k.dict.put(Py.toStr(key), ns.getItem(key));
        }
        version++;
        k.meta = winner(mcs, k.bases);
        k.implicitClassMethods();
        k.created(kn, kv);
        return k;
    }

    /// `__init_subclass__` and `__class_getitem__` are class methods even
    /// when written as plain functions.
    private void implicitClassMethods() {
        String[] names = {"__init_subclass__", "__class_getitem__"};
        for (String e : names) {
            Object f = dict.get(e);
            if (f instanceof PyFunction) {
                dict.put(e, new PyClassMethod(f));
            }
        }
    }

    /// `__set_name__` on the namespace, then the parent's `__init_subclass__`.
    private void created(String[] kn, Object[] kv) {
        Object[] entries = dict.entrySet().toArray();
        for (Object entry : entries) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, Object> e = (Map.Entry<String, Object>) entry;
            Object v = e.getValue();
            if (v instanceof PyInstance) {
                Object sn = ((PyInstance) v).cls.lookup("__set_name__");
                if (sn != null) {
                    Py.obj(sn).call(new Object[] {v, this, e.getKey()}, Py.NO_KW);
                }
            }
        }
        for (int i = 1; i < mro.length; i++) {
            Object is = mro[i].dict.get("__init_subclass__");
            if (is != null) {
                Object f = is instanceof PyClassMethod ? ((PyClassMethod) is).func : is;
                Py.obj(f).call(Py.prepend(this, kv), kn);
                return;
            }
        }
        if (kn.length > 0) {
            throw Py.typeError(name + ".__init_subclass__() takes no keyword arguments");
        }
    }

    // ---- what a metaclass can override -------------------------------------

    @Override
    public Object getItem(Object key) {
        Object m = metaDunder("__getitem__");
        if (m != null) {
            return Py.obj(m).call(new Object[] {this, key}, Py.NO_KW);
        }
        Object cg = lookup("__class_getitem__");
        if (cg != null) {
            Object f = cg instanceof PyClassMethod ? ((PyClassMethod) cg).func : cg;
            return Py.obj(f).call(new Object[] {this, key}, Py.NO_KW);
        }
        if (factory != null) {
            return new PyGenericAlias(this, key);
        }
        throw Py.typeError("type '" + name + "' is not subscriptable");
    }

    @Override
    public Object iter() {
        Object m = metaDunder("__iter__");
        if (m != null) {
            return Py.obj(m).call(new Object[] {this}, Py.NO_KW);
        }
        return super.iter();
    }

    @Override
    public int len() {
        Object m = metaDunder("__len__");
        if (m != null) {
            return (int) Py.asLong(Py.obj(m).call(new Object[] {this}, Py.NO_KW));
        }
        return super.len();
    }

    @Override
    public boolean contains(Object x) {
        Object m = metaDunder("__contains__");
        if (m != null) {
            return Py.isTrue(Py.obj(m).call(new Object[] {this, x}, Py.NO_KW));
        }
        return super.contains(x);
    }

    @Override
    public boolean isTrue() {
        Object m = metaDunder("__bool__");
        if (m != null) {
            return Py.isTrue(Py.obj(m).call(new Object[] {this}, Py.NO_KW));
        }
        m = metaDunder("__len__");
        return m == null || len() != 0;
    }

    @Override
    public String str() {
        Object m = metaDunder("__str__");
        if (m != null) {
            return Py.strValue(Py.obj(m).call(new Object[] {this}, Py.NO_KW), "__str__");
        }
        return repr();
    }

    /// Looks `attr` up along the MRO, without binding.
    public Object lookup(String attr) {
        if (lookupVersion != version || lookupCache == null) {
            lookupCache = new java.util.HashMap<String, Object>();
            lookupVersion = version;
        }
        Object c = lookupCache.get(attr);
        if (c != null) {
            return c == MISSING ? null : c;
        }
        Object found = null;
        for (int i = 0; i < mro.length && found == null; i++) {
            found = mro[i].dict.get(attr);
        }
        lookupCache.put(attr, found == null ? MISSING : found);
        return found;
    }

    /// Whether the class overrides `__getattribute__`, which takes every
    /// attribute read out of the inline caches' hands.
    boolean hasCustomGetattribute() {
        Object g = lookup("__getattribute__");
        return g != null && !(g instanceof PyBuiltin);
    }

    /// `issubclass(self, other)`.
    public boolean isSubtype(PyType other) {
        for (PyType e : mro) {
            if (e == other) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getAttrOrNull(String attr) {
        if ("__name__".equals(attr)) {
            return name;
        }
        if ("__qualname__".equals(attr)) {
            Object q = dict.get(attr);
            return q != null ? q : name;
        }
        if ("__mro__".equals(attr)) {
            Object[] items = new Object[mro.length];
            System.arraycopy(mro, 0, items, 0, mro.length);
            return new PyTuple(items);
        }
        if ("__dict__".equals(attr)) {
            return namespace();
        }
        if ("__subclasses__".equals(attr)) {
            return PyBuiltin.positional("__subclasses__", a -> {
                PyList out = new PyList(subclasses == null ? 0 : subclasses.size());
                if (subclasses != null) {
                    for (int i = 0; i < subclasses.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
                        out.append(subclasses.get(i));
                    }
                }
                return out;
            });
        }
        if ("__bases__".equals(attr)) {
            Object[] items = new Object[bases.length];
            System.arraycopy(bases, 0, items, 0, bases.length);
            return new PyTuple(items);
        }
        Object v = lookup(attr);
        if (v == null) {
            if (customMeta()) {
                Object mv = metaAttr(attr);
                if (mv != null) {
                    return mv;
                }
            }
            if ("__class__".equals(attr)) {
                return type();
            }
            // Built-in types carry no namespace entries for these.
            if ("__module__".equals(attr)) {
                return Py.str("builtins");
            }
            if ("__doc__".equals(attr)) {
                return Py.NONE;
            }
            if ("__hash__".equals(attr)) {
                // Instances hash by identity without an entry for it; a class
                // that defines __eq__ alone is unhashable (PyInstance.hash).
                return lookup("__eq__") != null ? Py.NONE : Builtins.OBJECT_HASH;
            }
            return null;
        }
        // Reading a function through the class gives the plain function;
        // classmethod and staticmethod know what they become.
        if (v instanceof PyClassMethod) {
            return new PyMethod(this, ((PyClassMethod) v).func);
        }
        if (v instanceof PyStaticMethod) {
            return ((PyStaticMethod) v).func;
        }
        if (v instanceof PyInstance) {
            Object get = ((PyInstance) v).cls.lookup("__get__");
            if (get != null) {
                return Py.obj(get).call(new Object[] {v, Py.NONE, this}, Py.NO_KW);
            }
        }
        return v;
    }

    /// An attribute of the metaclass, read through the class: a method
    /// bound to the class, a property computed on it.
    private Object metaAttr(String attr) {
        Object m = meta.lookup(attr);
        if (m == null) {
            Object ga = metaDunder("__getattr__");
            return ga == null ? null : Py.obj(ga).call(new Object[] {this, attr}, Py.NO_KW);
        }
        if (m instanceof PyProperty) {
            return ((PyProperty) m).get(this);
        }
        if (m instanceof PyClassMethod) {
            return new PyMethod(meta, ((PyClassMethod) m).func);
        }
        if (m instanceof PyStaticMethod) {
            return ((PyStaticMethod) m).func;
        }
        return Py.bindAttr(m, this);
    }

    @Override
    public void setAttr(String attr, Object value) {
        Object sa = metaDunder("__setattr__");
        if (sa != null) {
            Py.obj(sa).call(new Object[] {this, attr, value}, Py.NO_KW);
            return;
        }
        setAttrDirect(attr, value);
    }

    /// `type.__setattr__(cls, attr, value)`.
    public void setAttrDirect(String attr, Object value) {
        if ("__name__".equals(attr)) {
            if (!(value instanceof String) && !(value instanceof PyStr)) {
                throw Py.typeError("can only assign string to " + name + ".__name__, not '"
                        + Py.typeOf(value).name + "'");
            }
            name = Py.toStr(value);
            version++;
            return;
        }
        changed(attr, value);
        version++;
        dict.put(attr, value);
    }

    @Override
    public void delAttr(String attr) {
        Object da = metaDunder("__delattr__");
        if (da != null) {
            Py.obj(da).call(new Object[] {this, attr}, Py.NO_KW);
            return;
        }
        delAttrDirect(attr);
    }

    /// `type.__delattr__(cls, attr)`.
    public void delAttrDirect(String attr) {
        changed(attr, null);
        version++;
        if (dict.remove(attr) == null) {
            throw Py.attributeError("type object '" + name + "' has no attribute '" + attr + "'");
        }
    }

    @Override
    public Object call(Object[] args, String[] kwNames) {
        if (this == Py.TYPE_TYPE && args.length == 1 && kwNames.length == 0) { //NOPMD CompareObjectsWithEquals - identity is meant
            return Py.typeOf(args[0]);
        }
        Object mc = metaDunder("__call__");
        if (mc != null) {
            return Py.obj(mc).call(Py.prepend(this, args), kwNames);
        }
        return construct(args, kwNames);
    }

    /// `type.__call__(cls, *args)`: `__new__`, then `__init__`.
    Object construct(Object[] args, String[] kwNames) {
        if (factory != null) {
            // A builtin type builds and initialises its own instances.
            return factory.create(this, args, kwNames);
        }
        Object newFn = lookup("__new__");
        Object obj;
        if (newFn != null && newFn != Builtins.OBJECT_NEW) { //NOPMD CompareObjectsWithEquals - identity is meant
            Object fn = newFn instanceof PyStaticMethod ? ((PyStaticMethod) newFn).func : newFn;
            obj = Py.obj(fn).call(Py.prepend(this, args), kwNames);
            if (!Py.typeOf(obj).isSubtype(this)) {
                return obj;
            }
        } else {
            obj = newInstance();
        }
        Object init = lookup("__init__");
        if (init != null && init != Builtins.OBJECT_INIT) { //NOPMD CompareObjectsWithEquals - identity is meant
            Object r = Py.obj(init).call(Py.prepend(obj, args), kwNames);
            if (r != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                throw Py.typeError("__init__() should return None, not '" + Py.typeOf(r).name + "'");
            }
        } else if (args.length > 0 && newFn == Builtins.OBJECT_NEW) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.typeError(name + "() takes no arguments");
        }
        return obj;
    }

    /// The `__init__` of a plain class (no `__new__`, no builtin base), the
    /// case a positional constructor call takes without an argument array.
    private Object plainInit() {
        if (factory != null || this == Py.TYPE_TYPE || customMeta()) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        Object newFn = lookup("__new__");
        if (newFn != null && newFn != Builtins.OBJECT_NEW) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        Object init = lookup("__init__");
        return init instanceof PyFunction ? init : null;
    }

    private static void initReturned(Object r) {
        if (r != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.typeError("__init__() should return None, not '" + Py.typeOf(r).name + "'");
        }
    }

    @Override
    public Object call0() {
        Object init = plainInit();
        if (init == null) {
            return super.call0();
        }
        PyInstance o = newInstance();
        initReturned(((PyFunction) init).call1(o));
        return o;
    }

    @Override
    public Object call1(Object a) {
        Object init = plainInit();
        if (init == null) {
            return super.call1(a);
        }
        PyInstance o = newInstance();
        initReturned(((PyFunction) init).call2(o, a));
        return o;
    }

    @Override
    public Object call2(Object a, Object b) {
        Object init = plainInit();
        if (init == null) {
            return super.call2(a, b);
        }
        PyInstance o = newInstance();
        initReturned(((PyFunction) init).call3(o, a, b));
        return o;
    }

    @Override
    public Object call3(Object a, Object b, Object c) {
        Object init = plainInit();
        if (init == null) {
            return super.call3(a, b, c);
        }
        PyInstance o = newInstance();
        initReturned(((PyFunction) init).call4(o, a, b, c));
        return o;
    }

    @Override
    public String repr() {
        Object m = metaDunder("__repr__");
        if (m != null) {
            return Py.strValue(Py.obj(m).call(new Object[] {this}, Py.NO_KW), "__repr__");
        }
        Object q = dict.get("__qualname__");
        String qual = q instanceof String ? (String) q : name;
        Object mod = dict.get("__module__");
        if (mod instanceof String && !"builtins".equals(mod)) {
            return "<class '" + mod + "." + qual + "'>";
        }
        return "<class '" + qual + "'>";
    }

    @Override
    public long hash() {
        return System.identityHashCode(this);
    }

    @Override
    public Object or(Object o) {
        return PyUnion.of(this, o);
    }

    @Override
    public Object ror(Object o) {
        return PyUnion.of(o, this);
    }

    private static PyType[] computeMro(PyType self, PyType[] bases) {
        List<List<PyType>> seqs = new ArrayList<List<PyType>>();
        for (PyType e : bases) {
            List<PyType> l = new ArrayList<PyType>();
            for (int j = 0; j < e.mro.length; j++) {
                l.add(e.mro[j]);
            }
            seqs.add(l);
        }
        List<PyType> direct = new ArrayList<PyType>();
        for (PyType base : bases) {
            direct.add(base);
        }
        seqs.add(direct);
        List<PyType> out = new ArrayList<PyType>();
        out.add(self);
        while (true) {
            boolean empty = true;
            for (int i = 0; i < seqs.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
                if (!seqs.get(i).isEmpty()) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                return out.toArray(new PyType[out.size()]);
            }
            PyType candidate = null;
            for (int i = 0; i < seqs.size() && candidate == null; i++) {
                List<PyType> s = seqs.get(i);
                if (s.isEmpty()) {
                    continue;
                }
                PyType head = s.get(0);
                boolean inTail = false;
                for (int j = 0; j < seqs.size() && !inTail; j++) {
                    List<PyType> t = seqs.get(j);
                    inTail = t.indexOf(head) > 0;
                }
                if (!inTail) {
                    candidate = head;
                }
            }
            if (candidate == null) {
                throw Py.typeError("Cannot create a consistent method resolution order (MRO)");
            }
            out.add(candidate);
            for (int i = 0; i < seqs.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
                List<PyType> s = seqs.get(i);
                if (!s.isEmpty() && s.get(0) == candidate) { //NOPMD CompareObjectsWithEquals - identity is meant
                    s.remove(0);
                }
            }
        }
    }
}
