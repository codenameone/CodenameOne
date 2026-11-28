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
import java.util.Arrays;
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

    /// A builtin type's one-positional-argument constructor (`str(x)`,
    /// `int(x)`): what its [Factory] answers for that call, without the
    /// argument array the factory takes.
    interface Unary {
        Object create(Object x);
    }

    /// [Unary] for two positional arguments: `int(s, 16)`, `str(b, "utf-8")`.
    interface Binary {
        Object create(Object x, Object y);
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

    /// [#hasCustomGetattribute()]'s answer, valid while [#version] equals
    /// [#customGetattributeVersion].
    private boolean customGetattribute;
    private int customGetattributeVersion = -1;

    private static final Object MISSING = new PyObject() {
        @Override
        public PyType type() {
            return Py.OBJECT_TYPE;
        }
    };
    private boolean plainException;
    private int plainExceptionVersion = -1;

    /// Whether constructing this exception type from a message only makes an
    /// instance and sets `args`: BaseException's `__new__` and `__init__`
    /// both apply. `Py.error` builds those without the call protocol.
    boolean plainException() {
        if (plainExceptionVersion != version) {
            plainException = factory == null && isSubtype(Py.BASE_EXCEPTION)
                    && lookup("__new__") == Builtins.BASE_EXCEPTION_NEW //NOPMD CompareObjectsWithEquals - identity is meant
                    && lookup("__init__") == Builtins.BASE_EXCEPTION_INIT; //NOPMD CompareObjectsWithEquals - identity is meant
            plainExceptionVersion = version;
        }
        return plainException;
    }

    private Map<String, Object> lookupCache;
    private int lookupVersion = -1;
    Factory factory;
    /// Set only on the builtin type itself, never inherited: a subclass of
    /// `str` constructs through `__new__`/`__init__` as usual.
    Unary unary;
    Binary binary;
    /// The builtin type whose storage instances of this class extend, or `null`.
    final PyType solidBase;
    /// The class's metaclass: `type`, or a subclass of it (null while `type`
    /// itself is being built, which is its own metaclass).
    PyType meta;

    /// The direct subclasses, for `type.__subclasses__()`; null until one exists.
    private List<PyType> subclasses;

    /// `abc` state of a class whose metaclass is [Abc#META]: the classes
    /// `register` made virtual subclasses, and the abstract names that make
    /// constructing it a TypeError (null when there are none).
    List<PyType> abcRegistry;
    String[] abstractNames;

    /// The direct subclasses, never null.
    List<PyType> subclassList() {
        return subclasses == null ? java.util.Collections.<PyType>emptyList() : subclasses;
    }

    /// A number of its own, for the hashed caches of the store sites
    /// ([StoreSite]). Python code runs on one thread at a time.
    final int id = nextTypeId();

    private static int typeIds;

    private static int nextTypeId() {
        return ++typeIds;
    }

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
    /// The compiled class whose layout a class built at run time borrows
    /// (see [#borrowLayout]), or null.
    private PyType layoutFrom;
    private boolean layoutDecided;
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

    /// Set for a frozen dataclass: generated code stores none of its fields
    /// directly (the transpiler gives it a layout only then), so the
    /// `__setattr__` and `__delattr__` the decorator adds after the class is
    /// built decide every store, as in CPython.
    private boolean storesThroughSetattr;

    public PyType storesThroughSetattr() {
        storesThroughSetattr = true;
        return this;
    }

    /// The compiled class after the builtin base of a class confirmed by
    /// [#builtinBased], or null.
    private PyType compiledParent;

    /// Whether a compiled class whose first base is the builtin type
    /// `builtin` (`class NavigableString(str, PageElement)`) may take its
    /// layout, with `parent` (or null) its one other base and its compiled
    /// parent: the MRO is this class, the builtin, then the parent's chain,
    /// so a name the builtin binds comes before the chain's. Typed code
    /// resolves every name in the chain, so none of its names may be one
    /// the builtin binds.
    public boolean builtinBased(PyType builtin, PyType parent) {
        if (bases.length == 0 || bases[0] != builtin || solidBase != builtin //NOPMD CompareObjectsWithEquals - identity is meant
                || customMeta()) {
            return false;
        }
        int seen = 0;
        for (int i = 1; i < bases.length; i++) {
            if (bases[i] == parent) { //NOPMD CompareObjectsWithEquals - identity is meant
                seen++;
            } else if (!fieldlessGeneric(bases[i])) {
                return false;
            }
        }
        if (parent == null) {
            return true;
        }
        if (seen != 1 || !parent.hasLayout()) {
            return false;
        }
        // Below an enum, EnumType copies these from the nearest enum (or the
        // mixin, for __hash__) onto the class when it is made (enum._finish),
        // so the builtin's own never answers for them. The compiler exempts
        // the same names (Layouts.ENUM_COPIED); the metaclass is not set on
        // this class yet, so the parent says whether EnumType makes it.
        boolean enumCopies = EnumMeta.governs(parent);
        for (PyType m : parent.mro) {
            if (m == Py.OBJECT_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
                continue;
            }
            for (String k : m.dict.keySet()) {
                if (builtin.dict.containsKey(k) && !CLASS_METADATA.contains(k)
                        && !(enumCopies && ENUM_COPIED.contains(k))) {
                    return false;
                }
            }
        }
        compiledParent = parent;
        return true;
    }

    /// Whether a compiled class deriving from builtin exceptions may take
    /// its layout: its bases are still `parent` (when not null, a compiled
    /// class with its layout) followed by the builtin exception types named
    /// `builtins`, in that order. An exception instance is a plain
    /// [PyInstance] whose `args` and the rest live in its attribute table,
    /// so the layout needs nothing of the builtins but that they are the
    /// builtins: `except` and `isinstance` walk the type's own MRO, which
    /// keeps every one of them.
    public boolean exceptionBased(PyType parent, String[] builtins) {
        int off = parent != null ? 1 : 0;
        if (customMeta() || bases.length != off + builtins.length) {
            return false;
        }
        if (parent != null && (bases[0] != parent || !parent.hasLayout())) { //NOPMD CompareObjectsWithEquals - identity is meant
            return false;
        }
        for (int i = 0; i < builtins.length; i++) {
            PyType b = bases[off + i];
            if (!Py.EXCEPTION_TYPES.contains(b) || !b.name.equals(builtins[i])) {
                return false;
            }
        }
        return true;
    }

    /// Whether `t` is `typing.Generic`, compiled with no field: as a base
    /// it gives an instance nothing.
    private static boolean fieldlessGeneric(PyType t) {
        return "Generic".equals(t.name) && t.layout != null && (t.layoutFields == null || t.layoutFields.length == 0);
    }

    /// Names every class body binds, which say nothing of its instances.
    private static final Set<String> CLASS_METADATA = new HashSet<String>(Arrays.asList(
            "__module__", "__qualname__", "__doc__", "__dict__", "__weakref__", "__annotations__",
            "__firstlineno__", "__static_attributes__", "__slots__", "__orig_bases__", "__parameters__"));

    /// The names `enum._finish` sets on every class EnumType makes.
    private static final Set<String> ENUM_COPIED = new HashSet<String>(Arrays.asList(
            "__repr__", "__str__", "__format__", "__hash__"));

    /// The compiled class this one's layout extends, or null.
    private PyType layoutParent() {
        if (layoutFrom != null) {
            return layoutFrom;
        }
        if (compiledParent != null) {
            return compiledParent;
        }
        // neutralExtraBases recorded the compiled parent when it was not
        // the first base; otherwise that is the first base.
        return bases.length >= 1 && bases[0].layout != null ? bases[0] : null;
    }

    /// Whether a compiled class with bases besides its compiled parent
    /// (`parent`, or null when it has none) may take its layout: the MRO
    /// is this class, then `parent`'s MRO but `object`, then the rest, and
    /// nothing in the rest -- `typing.Generic`, a method-only mixin --
    /// has a field or a builtin base, hooks attribute access, declares
    /// slots, binds `__init__` or `__new__` or a special method typed code
    /// acts on, or binds a name that is one of the layout's `fields`. Every
    /// field and compiled method then resolves in the compiled chain first,
    /// as typed code assumes. pdfminer's `LTContainer(LTComponent,
    /// Generic[T])` and `LTChar(LTComponent, LTText)`, and every class
    /// below them, had plain instances without this.
    public boolean neutralExtraBases(PyType parent, String[] fields) {
        PyType[] chain = parent != null ? parent.mro : new PyType[0];
        // The compiled parent may be any base, not only the first: a
        // method-only mixin listed before it (pptx's
        // `LayoutPlaceholder(_InheritsDimensions, Shape)`) is judged below
        // like every other class outside the compiled chain, and the
        // compiler resolved its methods through this same MRO.
        if (parent != null && (!isBase(parent) || !parent.hasLayout())) {
            return false;
        }
        // The compiled chain keeps its order in this MRO, though C3 may put
        // a class of an extra base between two of its classes (a diamond).
        int prev = 0;
        for (PyType c : chain) {
            int at = indexIn(mro, c);
            if (at <= prev) {
                return false;
            }
            prev = at;
        }
        for (int i = 1; i < mro.length; i++) {
            PyType m = mro[i];
            if (m == Py.OBJECT_TYPE || indexIn(chain, m) >= 0) { //NOPMD CompareObjectsWithEquals - identity is meant
                continue;
            }
            if (Py.EXCEPTION_TYPES.contains(m) && exceptionChain(chain)) {
                // A builtin exception beside a chain over one: an exception
                // instance keeps its state in its attribute table whatever
                // its builtin, so it adds nothing to the layout
                // (Layouts.mergeExtrasOrdered admits the same).
                continue;
            }
            if (m.solidBase != null) {
                return false;
            }
            // A compiled extra base's fields are copied into this layout.
            if (m.layout != null && m.layoutFields != null) {
                for (String f : m.layoutFields) {
                    if (!contains(fields, f)) {
                        return false;
                    }
                }
            }
            for (Map.Entry<String, Object> e : m.dict.entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                if ("__setattr__".equals(k) || "__getattribute__".equals(k) || "__delattr__".equals(k)
                        || "__getattr__".equals(k) || "__slots__".equals(k) && !emptySlots(v) || contains(fields, k)) {
                    return false;
                }
                if (CLASS_METADATA.contains(k)) {
                    continue;
                }
                // A name a class before this one in the MRO binds never
                // reaches it. Otherwise construction must not run it, and
                // a special method typed code acts on is taken only from a
                // compiled class, whose names the subclass's typed code
                // already resolves through the MRO (Layouts.mergeExtras).
                boolean shadowed = false;
                for (int j = 0; j < i && !shadowed; j++) {
                    shadowed = mro[j].dict.containsKey(k);
                }
                // A compiled extra base's __init__ is allowed: the compiler
                // (Layouts.mergeExtrasOrdered) binds no def of its own for
                // it, so construction runs the MRO's, as here.
                if (!shadowed && ("__init__".equals(k) && v instanceof PyFunction && !Abc.isAbstract(v) && m.layout == null
                        || "__new__".equals(k) || specialMethod(k, v) && m.layout == null && !Abc.isAbstract(v))) {
                    return false;
                }
            }
        }
        if (parent != null) {
            compiledParent = parent;
        }
        return true;
    }

    /// Whether a builtin exception type is in `chain`.
    private static boolean exceptionChain(PyType[] chain) {
        for (PyType c : chain) {
            if (Py.EXCEPTION_TYPES.contains(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBase(PyType t) {
        for (PyType b : bases) {
            if (b == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    private static int indexIn(PyType[] a, PyType t) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                return i;
            }
        }
        return -1;
    }

    /// Whether the class has a compiled layout.
    public boolean hasLayout() {
        return layout != null && layoutFrom == null;
    }

    /// A new, uninitialised instance.
    public PyInstance newInstance() {
        if (!layoutDecided && layout == null) {
            borrowLayout();
        }
        PyInstance o = layout != null ? layout.create(this) : new PyInstance(this);
        if (solidBase != null && BuiltinSubclass.mutable(solidBase)) {
            // Filled by the builtin's __init__ (or the subclass's, through super()).
            o.base = solidBase.factory.create(solidBase, Py.NO_ARGS, Py.NO_KW);
        }
        return o;
    }

    /// Lets a class built at run time -- a subclass in another module of a
    /// class the transpiler compiled -- make its instances with that class's
    /// layout, so the compiled methods it inherits read their fields as
    /// fields. Without it every one of pdfminer's parsers, all subclasses of
    /// `psparser.PSBaseParser` defined in other modules, ran the compiled
    /// tokenizer on a plain instance, every field through a megamorphic
    /// site. Decided at the first instance, when the class namespace is
    /// complete, and only when nothing in the MRO outside the compiled
    /// chain can change what a field read or store means: no attribute
    /// hooks or `__slots__`, and nothing but a function or a constant bound
    /// to a field's name, and no method that overrides a compiled one or is
    /// a special method (see the loop); later changes go through
    /// [#changed] as for a compiled class.
    private void borrowLayout() {
        layoutDecided = true;
        if (solidBase != null || customMeta() && !EnumMeta.governs(this)) {
            return;
        }
        PyType a = null;
        for (int i = 1; i < mro.length && a == null; i++) {
            if (mro[i].layout != null && mro[i].layoutFrom == null) {
                a = mro[i];
            }
        }
        if (a == null) {
            return;
        }
        boolean beforeA = true;
        boolean overrides = false;
        for (PyType m : mro) {
            if (m == a) { //NOPMD CompareObjectsWithEquals - identity is meant
                beforeA = false;
            }
            if (m == Py.OBJECT_TYPE || m.layout != null && m.layoutFrom == null && a.isSubtype(m)) { //NOPMD CompareObjectsWithEquals - identity is meant
                continue;
            }
            for (Map.Entry<String, Object> e : m.dict.entrySet()) {
                String k = e.getKey();
                if ("__setattr__".equals(k) || "__getattribute__".equals(k) || "__delattr__".equals(k)
                        || "__slots__".equals(k) && !emptySlots(e.getValue())) {
                    return;
                }
                if (fieldOf(a, k) && !plainClassValue(e.getValue())) {
                    return;
                }
                if (beforeA && specialMethod(k, e.getValue())) {
                    // Typed code assumes the compiled class's comparison and
                    // truth; a special method the compiled class lacks, or
                    // overrides, would be skipped without a word. Such a
                    // class keeps ordinary instances, which no typed entry
                    // admits, so every operation goes the generic way.
                    return;
                }
                // An override of a compiled method is honoured instead: the
                // class is marked patched, so every compiled call of a method
                // -- the generic dispatch (`m_`), a typed method's bridge
                // (`pt_`) and its virtual slot (`tv_`) -- asks the class
                // first and goes the generic way, which finds the override.
                // `label()` of the base calling `self.kind()` must answer the
                // subclass's kind. The fields stay compiled: bs4's
                // BeautifulSoupHTMLParser overrides the handle_* hooks of
                // HTMLParser, whose tokenizer it runs millions of times.
                overrides |= beforeA && methodOf(a, k);
            }
        }
        layout = a.layout;
        layoutFrom = a;
        patched |= overrides;
        LAYOUTS.add(this);
    }

    /// A special method other than `__init__` that typed code may act on
    /// in place of calling it: comparison, truth, hashing, length,
    /// iteration, operators. The ones it never acts on -- it reaches them
    /// through the generic dispatch, which finds an override -- are left
    /// out (`NEUTRAL_SPECIALS`): `__repr__` on pdfminer's LTChar and
    /// `__getstate__` on bs4's BeautifulSoup kept both on plain instances.
    private static boolean specialMethod(String name, Object v) {
        return v instanceof PyFunction && name.startsWith("__") && name.endsWith("__") && !"__init__".equals(name)
                && !NEUTRAL_SPECIALS.contains(name);
    }

    private static final Set<String> NEUTRAL_SPECIALS = new HashSet<String>();

    static {
        String[] n = {"__repr__", "__str__", "__format__", "__getstate__", "__setstate__", "__reduce__",
            "__reduce_ex__", "__copy__", "__deepcopy__", "__init_subclass__", "__class_getitem__", "__sizeof__",
            "__dir__", "__enter__", "__exit__"};
        for (String s : n) {
            NEUTRAL_SPECIALS.add(s);
        }
    }

    /// `__slots__ = ()`, as `typing.Generic` declares: no slot descriptor.
    private static boolean emptySlots(Object v) {
        return v instanceof PyTuple && ((PyTuple) v).size() == 0;
    }

    private static boolean fieldOf(PyType a, String name) {
        for (PyType u = a; u != null; u = u.layoutParent()) {
            if (contains(u.layoutFields, name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean methodOf(PyType a, String name) {
        for (PyType u = a; u != null; u = u.layoutParent()) {
            if (contains(u.compiledMethods, name)) {
                return true;
            }
        }
        return false;
    }

    /// A class attribute that is not a descriptor: an instance attribute of
    /// the same name, which a field holds, takes precedence over it.
    private static boolean plainClassValue(Object v) {
        return v == null || v == Py.NONE || v instanceof PyFunction || v instanceof String || v instanceof Long //NOPMD CompareObjectsWithEquals - identity is meant
                || v instanceof Double || v instanceof Boolean || v instanceof PyTuple || v instanceof PyStr;
    }

    /// Invalidates direct calls to `attr` and refuses what a compiled field
    /// cannot honour, once the class namespace of `this` changes.
    /// Set for good once anything writes or deletes an attribute of this
    /// compiled class, or of a class above it, after the class was built.
    /// Generated code folds a class constant only while this is clear: the
    /// transpiler proves over the whole program that no instance ever holds
    /// the name, and this answers for the class objects, which a computed
    /// `setattr(cls, k, v)` can reach without the build telling which.
    public boolean classWritten;

    private void changed(String attr, Object value) {
        for (int i = 0; i < LAYOUTS.size(); i++) { //NOPMD ForLoopCanBeForeach - indexed: a for-each allocates an iterator
            PyType t = LAYOUTS.get(i);
            if (!t.isSubtype(this)) {
                continue;
            }
            t.classWritten = true;
            boolean hooks = "__setattr__".equals(attr) || "__delattr__".equals(attr);
            if (hooks && t.storesThroughSetattr) {
                continue;
            }
            if (hooks || "__getattribute__".equals(attr)
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
            Object entries = Py.obj(hook).call1(PyTuple.of(bases));
            if (!(entries instanceof PyTuple)) {
                throw Py.typeError("__mro_entries__ must return a tuple");
            }
            Object[] items = ((PyTuple) entries).arr();
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
    private Object newAttrHook;
    private boolean newAttrHookKnown;

    /// `__cn1_new_attr__(self, name, value)` of the class, asked before an
    /// instance gains an attribute it has never had; it answers true when it
    /// kept the value itself. What a bundled library binds in place of a
    /// `__setattr__` that only polices new names (pydantic's BaseModel): a
    /// class binding `__setattr__` has no layout. Read once per class.
    Object newAttrHook() {
        if (!newAttrHookKnown) {
            newAttrHook = lookup("__cn1_new_attr__");
            newAttrHookKnown = true;
        }
        return newAttrHook;
    }

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
        // abc's ABCMeta is done by the runtime (Abc), so its classes keep the
        // fast construction paths, which check abstractNames themselves.
        return meta != null && meta != Py.TYPE_TYPE && meta != Abc.META; //NOPMD CompareObjectsWithEquals - identity is meant
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
        if (protocolState() != PROTO_NONE) {
            return protocolCheck(o, Py.typeOf(o).isSubtype(this));
        }
        if (Py.typeOf(o).isSubtype(this)) {
            return true;
        }
        if (Abc.governs(this)) {
            return Abc.subclassCheck(this, Py.typeOf(o));
        }
        Object m = metaDunder("__instancecheck__");
        return m != null && Py.isTrue(Py.obj(m).call2(this, o));
    }

    /// `issubclass(c, this)`, through the metaclass's `__subclasscheck__`.
    boolean subclassCheck(PyType c) {
        if (protocolState() != PROTO_NONE) {
            String data = protocolState() == PROTO_RUNTIME && !c.isSubtype(this) ? protocolData() : null;
            if (data != null) {
                throw Py.typeError("Protocols with non-method members don't support issubclass(). "
                        + "Non-method members: " + data + ".");
            }
            return protocolCheck(c, c.isSubtype(this));
        }
        if (Abc.governs(this)) {
            return Abc.subclassCheck(this, c);
        }
        Object m = metaDunder("__subclasscheck__");
        if (m != null) {
            return Py.isTrue(Py.obj(m).call2(this, c));
        }
        return c.isSubtype(this);
    }

    private static final int PROTO_NONE = 0;
    private static final int PROTO_RUNTIME = 1;
    private static final int PROTO_STATIC = 2;

    /// The protocol state of this class and the class version it was read
    /// at: `typing.runtime_checkable` marks a class after it is built.
    private int protoState;
    private int protoAt = -1;

    /// Whether this class is a `typing.Protocol` of its own (`_is_protocol`
    /// in its own namespace, as CPython keeps it), and whether it is
    /// runtime checkable. A protocol the transpiler wrote no structural
    /// check for (ProtocolChecks) is held nominally.
    private int protocolState() {
        if (protoAt != version) {
            Object p = dict.get("_is_protocol");
            int st = PROTO_NONE;
            if (p != null && Py.isTrue(p) && dict.get("__cn1_protocol_check__") != null) {
                Object r = dict.get("_is_runtime_protocol");
                st = r != null && Py.isTrue(r) ? PROTO_RUNTIME : PROTO_STATIC;
            }
            protoState = st;
            protoAt = version;
        }
        return protoState;
    }

    /// `isinstance`/`issubclass` against a protocol: CPython refuses one
    /// that is not runtime checkable before anything else, then accepts a
    /// nominal subclass, then checks the members by their literal names
    /// through the check the transpiler generated for the class body.
    private boolean protocolCheck(Object o, boolean nominal) {
        if (protocolState() == PROTO_STATIC) {
            throw Py.typeError("Instance and class checks can only be used with @runtime_checkable protocols");
        }
        if (nominal) {
            return true;
        }
        return Py.isTrue(Py.call(getAttr("__cn1_protocol_check__"), o));
    }

    /// The quoted names of the non-method members this protocol and those
    /// above it declare, or null when there are none.
    private String protocolData() {
        StringBuilder b = null;
        for (PyType t : mro) {
            Object d = t.dict.get("__cn1_protocol_data__");
            if (d != null) {
                b = b == null ? new StringBuilder() : b.append(", ");
                b.append(Py.toStr(d));
            }
        }
        return b == null ? null : b.toString();
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
        if (w == Abc.META) { //NOPMD CompareObjectsWithEquals - identity is meant
            // What ABCMeta.__new__ did, without a reflective metaclass call:
            // type.__new__ (created), then the abstract names.
            k.meta = w;
            k.created(kn, kv);
            Abc.init(k);
            return k;
        }
        if (w == EnumMeta.META) { //NOPMD CompareObjectsWithEquals - identity is meant
            // What EnumType.__new__ did, without a reflective metaclass
            // call: type.__new__ (created), then the members.
            k.meta = w;
            Object boundary = boundaryOf(kn, kv);
            k.created(withoutBoundary(kn), withoutBoundary(kn, kv));
            EnumMeta.init(k, boundary);
            return k;
        }
        if (!k.hasLayout()) {
            return callMeta(w, k, kn, kv);
        }
        // A compiled class: its fields and methods were laid out from the
        // body as written, and generated code names the class object it
        // built. A metaclass `__new__`/`__init__` may add to the namespace,
        // but replacing the class, or rebinding or adding a name the layout
        // compiled, would leave compiled code reading another class than the
        // one Python sees; that is refused rather than run unsoundly.
        Object[] keys = k.dict.keySet().toArray();
        Object[] before = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            before[i] = k.dict.get(keys[i]);
        }
        Object made = callMeta(w, k, kn, kv);
        if (made != k) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.typeError("metaclass " + w.name + " replaced the compiled class " + k.name);
        }
        for (int i = 0; i < keys.length; i++) {
            if (k.dict.get(keys[i]) != before[i]) { //NOPMD CompareObjectsWithEquals - identity is meant
                throw Py.typeError("metaclass " + w.name + " rebound " + keys[i] + " of the compiled class " + k.name);
            }
        }
        for (Object key : k.dict.keySet().toArray()) {
            if (k.layoutNames(key) && !containsKey(keys, key)) {
                throw Py.typeError("metaclass " + w.name + " added " + key + ", a compiled name of " + k.name);
            }
        }
        return k;
    }

    private static boolean containsKey(Object[] keys, Object key) {
        for (Object x : keys) {
            if (x.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /// Whether `name` is one of the compiled methods or fields of the
    /// layout.
    private boolean layoutNames(Object name) {
        for (String[] a : new String[][] {compiledMethods, layoutFields}) {
            if (a != null) {
                for (String x : a) {
                    if (x.equals(name)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        if (k.meta.isSubtype(EnumMeta.META)) {
            // EnumType(name, bases, ns, boundary=...): the functional API.
            Object boundary = boundaryOf(kn, kv);
            k.created(withoutBoundary(kn), withoutBoundary(kn, kv));
            EnumMeta.init(k, boundary);
            return k;
        }
        k.created(kn, kv);
        if (k.meta.isSubtype(Abc.META)) {
            // A Python metaclass deriving from abc.ABCMeta calling
            // super().__new__.
            Abc.init(k);
        }
        return k;
    }

    /// The `boundary=` keyword of an enum class statement, or None.
    private static Object boundaryOf(String[] kn, Object[] kv) {
        for (int i = 0; i < kn.length; i++) {
            if ("boundary".equals(kn[i])) {
                return kv[i];
            }
        }
        return Py.NONE;
    }

    /// The keyword names without `boundary`, which EnumType consumes.
    private static String[] withoutBoundary(String[] kn) {
        List<String> out = new ArrayList<String>();
        for (String n : kn) {
            if (!"boundary".equals(n)) {
                out.add(n);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    /// The keyword values matching [#withoutBoundary].
    private static Object[] withoutBoundary(String[] kn, Object[] kv) {
        List<Object> out = new ArrayList<Object>();
        for (int i = 0; i < kn.length; i++) {
            if (!"boundary".equals(kn[i])) {
                out.add(kv[i]);
            }
        }
        return out.toArray();
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
                    Py.obj(sn).call3(v, this, e.getKey());
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
            return Py.obj(m).call2(this, key);
        }
        if (EnumMeta.governs(this)) {
            return EnumMeta.getItem(this, key);
        }
        Object cg = lookup("__class_getitem__");
        if (cg != null) {
            Object f = cg instanceof PyClassMethod ? ((PyClassMethod) cg).func : cg;
            return Py.obj(f).call2(this, key);
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
            return Py.obj(m).call1(this);
        }
        if (EnumMeta.governs(this)) {
            return EnumMeta.iterOf(this);
        }
        return super.iter();
    }

    @Override
    public int len() {
        Object m = metaDunder("__len__");
        if (m != null) {
            return (int) Py.asLong(Py.obj(m).call1(this));
        }
        if (EnumMeta.governs(this)) {
            return EnumMeta.len(this);
        }
        return super.len();
    }

    @Override
    public boolean contains(Object x) {
        Object m = metaDunder("__contains__");
        if (m != null) {
            return Py.isTrue(Py.obj(m).call2(this, x));
        }
        if (EnumMeta.governs(this)) {
            return EnumMeta.contains(this, x);
        }
        return super.contains(x);
    }

    @Override
    public boolean isTrue() {
        Object m = metaDunder("__bool__");
        if (m != null) {
            return Py.isTrue(Py.obj(m).call1(this));
        }
        if (EnumMeta.governs(this)) {
            return true;
        }
        m = metaDunder("__len__");
        return m == null || len() != 0;
    }

    @Override
    public String str() {
        Object m = metaDunder("__str__");
        if (m != null) {
            return Py.strValue(Py.obj(m).call1(this), "__str__");
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
    ///
    /// Asked on every plain attribute read, so the answer is kept per class
    /// against [#version]: a read costs two field compares until some class
    /// changes. The version stays one global counter rather than one per
    /// class with propagation to subclasses: writes to a class after it is
    /// built are rare (and mostly at import), a type does not track its
    /// subclasses, and a per-class version would need that list kept for
    /// every class just to make the rare write cheaper.
    boolean hasCustomGetattribute() {
        if (customGetattributeVersion != version) {
            Object g = lookup("__getattribute__");
            customGetattribute = g != null && !(g instanceof PyBuiltin);
            customGetattributeVersion = version;
        }
        return customGetattribute;
    }

    /// Whether `__getitem__` is the builtin one (a dict subclass that does
    /// not override it), and the class's `__missing__` (null for none), kept
    /// against [#version] as [#hasCustomGetattribute()] is: a subscript of a
    /// `defaultdict` asked both of the whole MRO on every read.
    private boolean builtinGetitem;
    private Object missing;
    private int getitemVersion = -1;

    boolean hasBuiltinGetitem() {
        if (getitemVersion != version) {
            builtinGetitem = lookup("__getitem__") instanceof PyBuiltin;
            missing = lookup("__missing__");
            getitemVersion = version;
        }
        return builtinGetitem;
    }

    /// `__missing__`, valid after [#hasBuiltinGetitem()] answered.
    Object missingMethod() {
        return missing;
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
            // ABCMeta's register is found here too, though Abc.META keeps
            // the fast construction paths customMeta() guards.
            if (meta != null && meta != Py.TYPE_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
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
                return Py.obj(get).call3(v, Py.NONE, this);
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
            return ga == null ? null : Py.obj(ga).call2(this, attr);
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
            Py.obj(sa).call3(this, attr, value);
            return;
        }
        if (EnumMeta.governs(this)) {
            EnumMeta.setAttr(this, attr, value);
            return;
        }
        setAttrDirect(attr, value);
    }

    /// `type.__setattr__(cls, attr, value)`.
    public void setAttrDirect(String attr, Object value) {
        if (customMeta()) {
            // A data descriptor of the metaclass (a property with a setter)
            // decides the store, as type.__setattr__ does in CPython.
            Object ma = meta.lookup(attr);
            if (ma instanceof PyProperty) {
                ((PyProperty) ma).set(this, value);
                return;
            }
            if (ma instanceof PyInstance) {
                Object set = ((PyInstance) ma).cls.lookup("__set__");
                if (set != null) {
                    Py.obj(set).call3(ma, this, value);
                    return;
                }
            }
        }
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
            Py.obj(da).call2(this, attr);
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
        if (EnumMeta.governs(this)) {
            return EnumMeta.call(this, args, kwNames);
        }
        return construct(args, kwNames);
    }

    /// `type.__call__(cls, *args)`: `__new__`, then `__init__`.
    Object construct(Object[] args, String[] kwNames) {
        if (abstractNames != null) {
            throw Abc.instantiationError(this);
        }
        if (factory != null) {
            // A builtin type builds and initialises its own instances.
            return factory.create(this, args, kwNames);
        }
        if (kwNames.length == 0 && this != Py.OS_ERROR && plainException()) { //NOPMD CompareObjectsWithEquals - identity is meant
            // An exception class that overrides neither: what __new__ and
            // __init__ would do between them, once, without the receiver
            // prepended to two argument arrays (raise ValueError(...) in a
            // parser's loop).
            PyInstance e = newInstance();
            Object[] rest = new Object[args.length];
            System.arraycopy(args, 0, rest, 0, rest.length);
            e.setAttrDirect("args", rest.length == 0 ? PyTuple.EMPTY : new PyTuple(rest));
            return e;
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
    Object plainInit() {
        Object init = plainInitAny();
        return init instanceof PyFunction ? init : null;
    }

    /// The `__init__` of a plain class whatever it is, or null for a class that
    /// is not plain.
    private Object plainInitAny() {
        if (plainInitVersion == version) {
            return plainInitCache;
        }
        Object r = resolvePlainInit();
        plainInitCache = r;
        plainInitVersion = version;
        return r;
    }

    /// [#plainInitAny()]'s answer, valid while [#plainInitVersion] equals
    /// [#version]; any class change re-resolves it.
    private Object plainInitCache;
    private int plainInitVersion = -1;

    /// `__bool__` of this type, else its `__len__` (one of them null), for
    /// truth tests of instances; good while [#truthVersion] is [#version].
    Object truthBool;
    /// [#truthBool] is a builtin subclass's own `__bool__`: an instance's
    /// truth is its base value's.
    boolean truthBase;
    /// `__len__` / `__hash__` are the builtin's own (a subclass of str,
    /// tuple, int... that does not override them): an instance's answer is
    /// its base value's, asked without the builtin call and the view of the
    /// value it made. Kept with the truth methods, against the version.
    boolean lenBase;
    boolean hashBase;

    /// How many extra attribute slots this class's instances have grown to,
    /// the most seen: a new instance's first slot array is that long, so an
    /// `__init__` that sets a dozen names allocates once rather than at 4, 8
    /// and 16. Only a size hint -- any value is correct.
    /// The last `__dict__` an instance of this class built from scratch:
    /// the shape and set fields it was built for, the shape slots holding a
    /// field marker, and copies of its keys, hashes, index table and value
    /// sources (see `PyInstance.InstanceDict`).
    Shape dictTplShape;
    long dictTplMask;
    boolean[] dictTplMarks;
    Object[] dictTplKeys;
    long[] dictTplHashes;
    int[] dictTplIndex;
    int[] dictTplMap;

    int slotHint = 4;
    Object truthLen;
    /// What [#truthBool] always answers, when it is a function whose body
    /// only returns a bool (PyFunction#constant); null otherwise.
    Object truthConst;
    int truthVersion = -1;

    /// Refreshes [#truthBool] and [#truthLen] after any type changed.
    void truthMethods() {
        truthBool = lookup("__bool__");
        truthLen = truthBool == null ? lookup("__len__") : null;
        truthConst = truthBool instanceof PyFunction ? ((PyFunction) truthBool).constant : null;
        // A builtin's own __bool__, or (str, list, dict: no __bool__) its
        // own __len__, both answer the truth of the value an instance wraps.
        // A Python override is a function, never a builtin.
        truthBase = truthBool instanceof PyBuiltin ? ((PyBuiltin) truthBool).baseTruth
                : truthBool == null && truthLen instanceof PyBuiltin;
        lenBase = lookup("__len__") instanceof PyBuiltin;
        hashBase = lookup("__hash__") instanceof PyBuiltin;
        truthVersion = version;
    }

    private Object resolvePlainInit() {
        if (factory != null || this == Py.TYPE_TYPE || customMeta() || abstractNames != null) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        Object newFn = lookup("__new__");
        if (newFn != null && newFn != Builtins.OBJECT_NEW) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        return lookup("__init__");
    }

    // Keyword constructor calls of a plain class (`Tag(parser, builder,
    // name, attrs=a)`): `__init__` gets the new instance and the arguments as
    // parameters, with no argument array and none with the instance
    // prepended. Anything else -- a builtin, a `__new__`, a metaclass -- goes
    // the generic way.

    @Override
    public Object callKwF(String[] kwNames, Object a) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a));
            return o;
        }
        return super.callKwF(kwNames, a);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b));
            return o;
        }
        return super.callKwF(kwNames, a, b);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c));
            return o;
        }
        return super.callKwF(kwNames, a, b, c);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e, f));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e, f);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e, f, g));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e, f, g);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e, f, g, h));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e, f, g, h);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e, f, g, h, i);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i, j));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e, f, g, h, i, j);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i, j, k));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e, f, g, h, i, j, k);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i, j, k, l));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e, f, g, h, i, j, k, l);
    }

    @Override
    public Object callKwF(String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyInstance o = newInstance();
            initReturned(((PyFunction) init).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i, j, k, l, m));
            return o;
        }
        return super.callKwF(kwNames, a, b, c, d, e, f, g, h, i, j, k, l, m);
    }

    /// A positional constructor call of a plain class without an argument
    /// array: `__init__` is a function, or the one a dataclass generated. Null
    /// when the class is neither, and the caller builds the array after all.
    private Object positionalNew(int n, Object a, Object b, Object c, Object d, Object e, Object f6,
            Object g7) {
        Object init = plainInitAny();
        if (init instanceof PyFunction) {
            PyFunction f = (PyFunction) init;
            PyInstance o = newInstance();
            switch (n) {
                case 0:
                    initReturned(f.call1(o));
                    break;
                case 1:
                    initReturned(f.call2(o, a));
                    break;
                case 2:
                    initReturned(f.call3(o, a, b));
                    break;
                case 3:
                    initReturned(f.call4(o, a, b, c));
                    break;
                case 4:
                    initReturned(f.call5(o, a, b, c, d));
                    break;
                case 5:
                    initReturned(f.call6(o, a, b, c, d, e));
                    break;
                case 6:
                    initReturned(f.call7(o, a, b, c, d, e, f6));
                    break;
                default:
                    initReturned(f.call8(o, a, b, c, d, e, f6, g7));
                    break;
            }
            return o;
        }
        return constructFixed(n, a, b, c, d, e, f6, g7);
    }

    /// [#construct] for a call of `n` positional arguments: `__new__` and
    /// `__init__` called at their arity, where construct prepends the
    /// receiver to an argument array for each. A namedtuple (`urlparse`), a
    /// str subclass (bs4's NavigableString) and a list subclass are built
    /// this way per element. Null for a call this does not cover, which
    /// then goes through construct exactly as before.
    private Object constructFixed(int n, Object a, Object b, Object c, Object d, Object e, Object f6,
            Object g7) {
        if (factory != null || this == Py.TYPE_TYPE || this == Py.OS_ERROR || plainException() //NOPMD CompareObjectsWithEquals - identity is meant
                || abstractNames != null || metaDunder("__call__") != null || EnumMeta.governs(this)) {
            return null;
        }
        Object newFn = lookup("__new__");
        Object obj;
        if (newFn != null && newFn != Builtins.OBJECT_NEW) { //NOPMD CompareObjectsWithEquals - identity is meant
            Object fn = newFn instanceof PyStaticMethod ? ((PyStaticMethod) newFn).func : newFn;
            obj = callWith(Py.obj(fn), this, n, a, b, c, d, e, f6, g7);
            if (!Py.typeOf(obj).isSubtype(this)) {
                return obj;
            }
        } else {
            obj = newInstance();
        }
        Object init = lookup("__init__");
        if (init != null && init != Builtins.OBJECT_INIT) { //NOPMD CompareObjectsWithEquals - identity is meant
            initReturned(callWith(Py.obj(init), obj, n, a, b, c, d, e, f6, g7));
        } else if (n > 0 && newFn == Builtins.OBJECT_NEW) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.typeError(name + "() takes no arguments");
        }
        return obj;
    }

    /// `f(first, a, ...)` with the `n` arguments after `first`.
    private static Object callWith(PyObject f, Object first, int n, Object a, Object b, Object c, Object d, Object e,
            Object f6, Object g7) {
        switch (n) {
            case 0:
                return f.call1(first);
            case 1:
                return f.call2(first, a);
            case 2:
                return f.call3(first, a, b);
            case 3:
                return f.call4(first, a, b, c);
            case 4:
                return f.call5(first, a, b, c, d);
            case 5:
                return f.call6(first, a, b, c, d, e);
            case 6:
                return f.call7(first, a, b, c, d, e, f6);
            default:
                return f.call8(first, a, b, c, d, e, f6, g7);
        }
    }

    /// What a call of a class does with its `__init__`'s result; public for
    /// the constructor calls the transpiler binds at build time.
    public static void initReturned(Object r) {
        if (r != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.typeError("__init__() should return None, not '" + Py.typeOf(r).name + "'");
        }
    }

    @Override
    public Object call0() {
        Object o = positionalNew(0, null, null, null, null, null, null, null);
        return o != null ? o : super.call0();
    }

    @Override
    public Object call1(Object a) {
        if (unary != null) {
            return unary.create(a);
        }
        Object o = positionalNew(1, a, null, null, null, null, null, null);
        return o != null ? o : super.call1(a);
    }

    @Override
    public Object call2(Object a, Object b) {
        if (binary != null) {
            return binary.create(a, b);
        }
        Object o = positionalNew(2, a, b, null, null, null, null, null);
        return o != null ? o : super.call2(a, b);
    }

    @Override
    public Object call3(Object a, Object b, Object c) {
        Object o = positionalNew(3, a, b, c, null, null, null, null);
        return o != null ? o : super.call3(a, b, c);
    }

    @Override
    public Object call4(Object a, Object b, Object c, Object d) {
        Object o = positionalNew(4, a, b, c, d, null, null, null);
        return o != null ? o : super.call4(a, b, c, d);
    }

    @Override
    public Object call5(Object a, Object b, Object c, Object d, Object e) {
        Object o = positionalNew(5, a, b, c, d, e, null, null);
        return o != null ? o : super.call5(a, b, c, d, e);
    }

    @Override
    public Object call6(Object a, Object b, Object c, Object d, Object e, Object f) {
        Object o = positionalNew(6, a, b, c, d, e, f, null);
        return o != null ? o : super.call6(a, b, c, d, e, f);
    }

    @Override
    public Object call7(Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
        Object o = positionalNew(7, a, b, c, d, e, f, g);
        return o != null ? o : super.call7(a, b, c, d, e, f, g);
    }

    @Override
    public String repr() {
        Object m = metaDunder("__repr__");
        if (m != null) {
            return Py.strValue(Py.obj(m).call1(this), "__repr__");
        }
        if (EnumMeta.governs(this)) {
            return EnumMeta.repr(this);
        }
        return "<class '" + fullName() + "'>";
    }

    /// `module.qualname` as `repr` shows a class and its instances; a
    /// builtin type's module is left out.
    String fullName() {
        Object q = dict.get("__qualname__");
        String qual = q instanceof String ? (String) q : name;
        Object mod = dict.get("__module__");
        if (mod instanceof String && !"builtins".equals(mod)) {
            return mod + "." + qual;
        }
        return qual;
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
