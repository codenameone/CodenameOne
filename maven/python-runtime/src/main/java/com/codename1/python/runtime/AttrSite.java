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

/// An inline cache for one attribute access in generated code.
///
/// The transpiler gives every `obj.name` read, `obj.name = v` store and
/// `obj.name(...)` call its own site. The site remembers what it found the
/// last time -- the receiver's class, the class version, the instance's shape
/// and where the answer lives -- and as long as the next receiver matches, the
/// lookup is one comparison and an array access instead of a walk of the
/// method resolution order and a hash lookup per class. A site remembers up
/// to [#WAYS] receiver layouts, because one call written against a base
/// class routinely sees several subclasses (`task.run()` over every kind of
/// task); past that it replaces entries in turn.
///
/// Correctness rests on two identities. A class's version changes whenever
/// any class namespace changes (`PyType.version`), so a cached method,
/// property check or `__setattr__` check can never outlive the class it was
/// taken from. And a shape fixes which attribute names an instance holds, so
/// the same shape means the same slot -- and, for a class attribute, that the
/// instance still does not shadow it. Anything the cache does not model goes
/// the generic way, which is always correct.
public final class AttrSite {
    /// How many receiver layouts a site remembers.
    static final int WAYS = 4;
    /// Overflow entries a megamorphic read site keeps past its four ways.
    static final int HASHED = 256;

    private static final int NONE = 0;
    private static final int SLOT = 1;
    private static final int CLASS_ATTR = 2;
    /// A compiled field (`PyInstance.fieldIndex`), read by index; an unset
    /// one still takes the generic read, which finds the class's value.
    private static final int FIELD = 3;
    private static final int BUILTIN = 3;
    /// The instance lacks the attribute: no slot, no field, nothing in the
    /// class, and no `__getattribute__` or `__getattr__` that could answer
    /// it. Only [#getOrMiss] answers from it; a plain read still raises.
    private static final int MISSING = 5;
    /// The layout [#getOrMiss] last found missing, checked before the ways:
    /// a probe that misses usually misses on one class.
    private PyType missType;
    private Shape missShape;
    private int missVersion;

    private final String name;
    /// A builtin method this site calls directly when the receiver is that
    /// exact builtin type: Python forbids replacing a builtin type's methods,
    /// so the lookup, the argument array and the bound call can all be skipped.
    private final int intrinsic;
    private static final int I_NONE = 0;
    private static final int I_GET = 1;
    private static final int I_APPEND = 2;
    private static final int I_UPPER = 3;
    private static final int I_LOWER = 4;
    private static final int I_SPLIT = 5;
    private static final int I_JOIN = 6;
    private static final int I_CLEAR = 7;
    private static final int I_STARTSWITH = 8;
    private static final int I_ENDSWITH = 9;
    private static final int I_STRIP = 10;
    private static final int I_LSTRIP = 11;
    private static final int I_RSTRIP = 12;
    private static final int I_FIND = 13;
    private static final int I_RFIND = 14;
    private static final int I_REPLACE = 15;

    // Reads and calls. An entry with a null shape is a non-instance receiver
    // type (BUILTIN or NONE). All entries are valid only while getVersion is
    // the current class version.
    private int getVersion = -1;
    private int getUsed;
    private PyType[] getTypes;
    private Shape[] getShapes;
    private int[] getKinds;
    private int[] getSlots;
    private Object[] getValues;

    // The same entries again as plain fields, which the hit paths compare:
    // a field is a load, an array element a bounds-checked call in
    // translated code, and these sites run for every attribute access.
    // gfN is the function a call takes the receiver for (CLASS_ATTR holding
    // a PyFunction), else null; gkN is SLOT for a slot read, else NONE.
    private PyType gt0;
    private PyType gt1;
    private PyType gt2;
    private PyType gt3;
    private Shape gs0;
    private Shape gs1;
    private Shape gs2;
    private Shape gs3;
    private int gk0;
    private int gk1;
    private int gk2;
    private int gk3;
    private int gi0;
    private int gi1;
    private int gi2;
    private int gi3;
    private Object gf0;
    private Object gf1;
    private Object gf2;
    private Object gf3;

    /// A site for each name of `names`, one a line: the sites of a module,
    /// which generated code holds as one table.
    public static AttrSite[] table(String names) {
        String[] each = Py.names(names);
        AttrSite[] out = new AttrSite[each.length];
        for (int i = 0; i < each.length; i++) {
            out[i] = new AttrSite(each[i]);
        }
        return out;
    }

    public AttrSite(String name) {
        this.name = name.intern();
        this.intrinsic = intrinsicOf(name);
    }

    private static int intrinsicOf(String name) {
        switch (name) {
            case "get":
                return I_GET;
            case "append":
                return I_APPEND;
            case "upper":
                return I_UPPER;
            case "lower":
                return I_LOWER;
            case "split":
                return I_SPLIT;
            case "join":
                return I_JOIN;
            case "clear":
                return I_CLEAR;
            case "startswith":
                return I_STARTSWITH;
            case "endswith":
                return I_ENDSWITH;
            case "strip":
                return I_STRIP;
            case "lstrip":
                return I_LSTRIP;
            case "rstrip":
                return I_RSTRIP;
            case "find":
                return I_FIND;
            case "rfind":
                return I_RFIND;
            case "replace":
                return I_REPLACE;
            default:
                return I_NONE;
        }
    }

    /// The read entry for this receiver layout, or -1.
    private int findGet(PyType t, Shape shape) {
        if (getVersion != PyType.version) {
            getVersion = PyType.version;
            getUsed = 0;
            gt0 = gt1 = gt2 = gt3 = null;
            if (getTypes != null && getTypes.length > WAYS) {
                // The overflow entries name layouts of the old version.
                getTypes = null;
            }
            return -1;
        }
        for (int k = 0; k < getUsed; k++) {
            if (getTypes[k] == t && getShapes[k] == shape) { //NOPMD CompareObjectsWithEquals - identity is meant
                return k;
            }
        }
        if (getTypes != null && getTypes.length > WAYS) {
            int k = WAYS + hash(t, shape);
            if (getTypes[k] == t && getShapes[k] == shape) { //NOPMD CompareObjectsWithEquals - identity is meant
                return k;
            }
        }
        return -1;
    }

    /// The overflow slot of a layout. A megamorphic site -- one attribute
    /// read on instances of dozens of classes, as an XML binding that defines
    /// a class per tag does -- used to evict round robin past four layouts,
    /// so nearly every read missed and went back to the class dictionaries.
    private static int hash(PyType t, Shape shape) {
        int h = t.id * 0x9E3779B1 + (shape == null ? 0 : shape.id * 0x85EBCA6B);
        return h >>> (32 - 8);
    }

    private int addGet(PyType t, Shape shape, int kind, int slot, Object value) {
        int k;
        if (getUsed < WAYS) {
            k = getUsed++;
        } else {
            k = WAYS + hash(t, shape);
            if (getTypes.length == WAYS) {
                getTypes = java.util.Arrays.copyOf(getTypes, WAYS + HASHED);
                getShapes = java.util.Arrays.copyOf(getShapes, WAYS + HASHED);
                getKinds = java.util.Arrays.copyOf(getKinds, WAYS + HASHED);
                getSlots = java.util.Arrays.copyOf(getSlots, WAYS + HASHED);
                getValues = java.util.Arrays.copyOf(getValues, WAYS + HASHED);
            }
        }
        if (getTypes == null) {
            // Made on the first entry: most sites of a large program are in
            // code that never runs, and five arrays each was most of what a
            // site weighed.
            getTypes = new PyType[WAYS];
            getShapes = new Shape[WAYS];
            getKinds = new int[WAYS];
            getSlots = new int[WAYS];
            getValues = new Object[WAYS];
        }
        getTypes[k] = t;
        getShapes[k] = shape;
        getKinds[k] = kind;
        getSlots[k] = slot;
        getValues[k] = value;
        int fast = kind == SLOT || kind == FIELD ? kind : NONE;
        Object f = kind == CLASS_ATTR && value instanceof PyFunction ? value : null;
        if (k == 0) {
            gt0 = t; gs0 = shape; gk0 = fast; gi0 = slot; gf0 = f;
        } else if (k == 1) {
            gt1 = t; gs1 = shape; gk1 = fast; gi1 = slot; gf1 = f;
        } else if (k == 2) {
            gt2 = t; gs2 = shape; gk2 = fast; gi2 = slot; gf2 = f;
        } else if (k == 3) {
            gt3 = t; gs3 = shape; gk3 = fast; gi3 = slot; gf3 = f;
        }
        return k;
    }

    // ---- reads ---------------------------------------------------------------

    public Object get(Object o) {
        if (o instanceof PyInstance) {
            PyInstance i = (PyInstance) o;
            if (getVersion == PyType.version) {
                PyType t = i.cls;
                Shape s = i.shape;
                if (gs0 == s && gt0 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (gk0 == SLOT) {
                        return i.slots[gi0];
                    }
                    if (gk0 == FIELD) {
                        Object v = i.getField(gi0);
                        if (v != null) {
                            return v;
                        }
                    }
                }
                if (gs1 == s && gt1 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (gk1 == SLOT) {
                        return i.slots[gi1];
                    }
                    if (gk1 == FIELD) {
                        Object v = i.getField(gi1);
                        if (v != null) {
                            return v;
                        }
                    }
                }
                if (gs2 == s && gt2 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (gk2 == SLOT) {
                        return i.slots[gi2];
                    }
                    if (gk2 == FIELD) {
                        Object v = i.getField(gi2);
                        if (v != null) {
                            return v;
                        }
                    }
                }
                if (gs3 == s && gt3 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (gk3 == SLOT) {
                        return i.slots[gi3];
                    }
                    if (gk3 == FIELD) {
                        Object v = i.getField(gi3);
                        if (v != null) {
                            return v;
                        }
                    }
                }
            }
            return getSlow(i);
        }
        return Py.obj(o).getAttr(name);
    }

    /// `obj.name` for a miss probe (see [Py#MISS]): [Py#MISS] when the read
    /// would raise AttributeError without running any code -- remembered per
    /// class and shape like a hit -- and a caught AttributeError from code
    /// that did run comes back as a miss carrying it.
    public Object getOrMiss(Object o) {
        if (o instanceof PyInstance) {
            PyInstance i = (PyInstance) o;
            PyType cls = i.cls;
            Shape shape = i.shape;
            if (missType == cls && missShape == shape && missVersion == PyType.version) { //NOPMD CompareObjectsWithEquals - identity is meant
                return Py.MISS;
            }
            int k = findGet(cls, shape);
            if (k >= 0 && getKinds[k] == MISSING) {
                missType = cls;
                missShape = shape;
                missVersion = PyType.version;
                return Py.MISS;
            }
            // A dunder (`__dict__`, `__class__`) is answered by the object
            // itself, never from the shape or the class: it cannot be proved
            // missing here.
            if (k < 0 && !name.startsWith("__") && !cls.hasCustomGetattribute() && cls.lookup("__getattr__") == null
                    && i.fieldIndex(name) < 0 && (shape == null || shape.indexOf(name) < 0)
                    && cls.lookup(name) == null) {
                addGet(cls, shape, MISSING, 0, null);
                missType = cls;
                missShape = shape;
                missVersion = PyType.version;
                return Py.MISS;
            }
        }
        try {
            return get(o);
        } catch (PyException e) {
            if (e.matches(Py.ATTRIBUTE_ERROR)) {
                return new Py.Miss(e);
            }
            throw e;
        }
    }

    /// The class [#classLacks] last found without this attribute.
    private PyType lackType;
    private int lackVersion;

    /// Whether an instance of `cls` with no attribute of its own would miss
    /// this one: the class, its bases and `__getattr__` all lack it. The
    /// typed code asks this for an object it never made, whose fields it
    /// knows; a class given the attribute later answers false from then on.
    public boolean classLacks(PyType cls) {
        if (lackType == cls && lackVersion == PyType.version) { //NOPMD CompareObjectsWithEquals - identity is meant
            return true;
        }
        if (name.startsWith("__") || cls.hasCustomGetattribute() || cls.lookup("__getattr__") != null
                || cls.lookup(name) != null) {
            return false;
        }
        lackType = cls;
        lackVersion = PyType.version;
        return true;
    }

    private Object getSlow(PyInstance i) {
        int k = findGet(i.cls, i.shape);
        if (k >= 0) {
            int kind = getKinds[k];
            if (kind == SLOT) {
                return i.slots[getSlots[k]];
            }
            if (kind == CLASS_ATTR) {
                return Py.bindAttr(getValues[k], i);
            }
            if (kind == FIELD) {
                Object v = i.getField(getSlots[k]);
                if (v != null) {
                    return v;
                }
            }
            return i.getAttr(name);
        }
        return fillGet(i);
    }

    private Object fillGet(PyInstance i) {
        Shape shape = i.shape;
        Object result = i.getAttr(name);
        if (getVersion != PyType.version || i.shape != shape) { //NOPMD CompareObjectsWithEquals - identity is meant
            // The read ran code (a __getattr__, a property) that changed what
            // the cache would key on; remember nothing.
            return result;
        }
        PyType cls = i.cls;
        int kind = NONE;
        int slot = 0;
        Object value = null;
        Object clsAttr = cls.lookup(name);
        // A compiled field is remembered by its index, not its value: a read
        // from code that cannot name the Java class -- another module -- finds
        // it set, and one that finds it unset takes the generic read.
        int field = i.fieldIndex(name);
        if (clsAttr instanceof PyProperty && !cls.hasCustomGetattribute()) {
            // A property is a data descriptor: it answers before the
            // instance's own attributes, so while the class is unchanged
            // (getVersion) every read is its getter. bindAttr calls it.
            kind = CLASS_ATTR;
            value = clsAttr;
        } else if (field >= 0 && !(clsAttr instanceof PyProperty) && !readsThroughGet(clsAttr) && !cls.hasCustomGetattribute()) {
            kind = FIELD;
            slot = field;
        } else if (!(clsAttr instanceof PyProperty) && !readsThroughGet(clsAttr) && !cls.hasCustomGetattribute()
                && field < 0) {
            slot = shape == null ? -1 : shape.indexOf(name);
            if (slot >= 0) {
                kind = SLOT;
            } else if (clsAttr != null) {
                kind = CLASS_ATTR;
                value = clsAttr;
            }
        }
        addGet(cls, shape, kind, slot, value);
        return result;
    }

    /// A class attribute that is an instance of a class with `__get__` -- a
    /// descriptor written in Python -- answers every read by calling it, so
    /// neither the instance's own value nor the attribute itself may be
    /// remembered in its place.
    private static boolean readsThroughGet(Object clsAttr) {
        return clsAttr instanceof PyInstance && ((PyInstance) clsAttr).cls.lookup("__get__") != null;
    }

    // ---- calls ---------------------------------------------------------------

    public Object call0(Object o) {
        if (intrinsic == I_CLEAR && o != null) {
            // Exact classes: a subclass may override clear.
            if (o.getClass() == PyDict.class) {
                ((PyDict) o).clear();
                return Py.NONE;
            }
            if (o instanceof PyList) {
                ((PyList) o).clear();
                return Py.NONE;
            }
        }
        if (intrinsic != I_NONE && o instanceof String) {
            String v = ((String) o);
            if (intrinsic == I_UPPER) {
                return Py.str(TypeMethods.mapCase(v, 1));
            }
            if (intrinsic == I_LOWER) {
                return Py.str(TypeMethods.mapCase(v, 0));
            }
            if (intrinsic == I_SPLIT) {
                return TypeMethods.split(v, null, -1);
            }
            if (intrinsic == I_STRIP || intrinsic == I_LSTRIP || intrinsic == I_RSTRIP) {
                return Py.str(TypeMethods.strip(v, null, intrinsic != I_RSTRIP, intrinsic != I_LSTRIP));
            }
        }
        Object f = method(o);
        return f != null ? Py.obj(f).call1(o) : Py.obj(Py.obj(o).getAttr(name)).call0();
    }

    public Object call1(Object o, Object a) {
        // instanceof rather than getClass(): both classes are final, and
        // getClass() is a call on ParparVM.
        if (intrinsic != I_NONE && o != null) {
            if (intrinsic == I_APPEND && o instanceof PyList) {
                ((PyList) o).append(a);
                return Py.NONE;
            }
            if (o instanceof String) {
                if (intrinsic == I_SPLIT) {
                    return TypeMethods.split(((String) o), a, -1);
                }
                if (intrinsic == I_JOIN) {
                    return TypeMethods.join(((String) o), a);
                }
                if (intrinsic == I_STARTSWITH && a instanceof String) {
                    return Py.bool(((String) o).startsWith((String) a));
                }
                if (intrinsic == I_ENDSWITH && a instanceof String) {
                    return Py.bool(((String) o).endsWith((String) a));
                }
                if ((intrinsic == I_FIND || intrinsic == I_RFIND) && a instanceof String) {
                    return PyInt.of(TypeMethods.findWhole((String) o, (String) a, intrinsic == I_RFIND));
                }
            }
            if (intrinsic == I_GET && o instanceof PyDict && o.getClass() == PyDict.class) {
                Object v = ((PyDict) o).getOrNull(a);
                return v != null ? v : Py.NONE;
            }
        }
        Object f = method(o);
        return f != null ? Py.obj(f).call2(o, a) : Py.obj(Py.obj(o).getAttr(name)).call1(a);
    }

    public Object call2(Object o, Object a, Object b) {
        if (intrinsic == I_REPLACE && o instanceof String && a instanceof String && b instanceof String
                && ((String) a).length() > 0) {
            return Py.str(((String) o).replace((String) a, (String) b));
        }
        if (intrinsic == I_GET && o != null && o.getClass() == PyDict.class) {
            Object v = ((PyDict) o).getOrNull(a);
            return v != null ? v : b;
        }
        Object f = method(o);
        return f != null ? Py.obj(f).call3(o, a, b) : Py.obj(Py.obj(o).getAttr(name)).call2(a, b);
    }

    public Object call3(Object o, Object a, Object b, Object c) {
        Object f = method(o);
        return f != null ? Py.obj(f).call4(o, a, b, c) : Py.obj(Py.obj(o).getAttr(name)).call3(a, b, c);
    }

    public Object call4(Object o, Object a, Object b, Object c, Object d) {
        Object f = method(o);
        return f != null ? Py.obj(f).call5(o, a, b, c, d) : Py.obj(Py.obj(o).getAttr(name)).call4(a, b, c, d);
    }

    public Object call5(Object o, Object a, Object b, Object c, Object d, Object e) {
        Object f = method(o);
        return f != null ? Py.obj(f).call6(o, a, b, c, d, e)
                : Py.obj(Py.obj(o).getAttr(name)).call5(a, b, c, d, e);
    }

    public Object call6(Object o, Object a, Object b, Object c, Object d, Object e, Object f) {
        Object fm = method(o);
        return fm != null ? Py.obj(fm).call7(o, a, b, c, d, e, f) : Py.obj(Py.obj(o).getAttr(name)).call6(a, b, c, d, e, f);
    }

    public Object call7(Object o, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
        Object fm = method(o);
        return fm != null ? Py.obj(fm).call8(o, a, b, c, d, e, f, g) : Py.obj(Py.obj(o).getAttr(name)).call7(a, b, c, d, e, f, g);
    }

    public Object call8(Object o, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
        Object fm = method(o);
        return fm != null ? Py.obj(fm).call9(o, a, b, c, d, e, f, g, h) : Py.obj(Py.obj(o).getAttr(name)).call8(a, b, c, d, e, f, g, h);
    }

    public Object call9(Object o, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
        Object fm = method(o);
        return fm != null ? Py.obj(fm).call10(o, a, b, c, d, e, f, g, h, i) : Py.obj(Py.obj(o).getAttr(name)).call9(a, b, c, d, e, f, g, h, i);
    }

    public Object call10(Object o, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
        Object fm = method(o);
        return fm != null ? Py.obj(fm).call11(o, a, b, c, d, e, f, g, h, i, j) : Py.obj(Py.obj(o).getAttr(name)).call10(a, b, c, d, e, f, g, h, i, j);
    }

    public Object call11(Object o, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
        Object fm = method(o);
        return fm != null ? Py.obj(fm).call12(o, a, b, c, d, e, f, g, h, i, j, k) : Py.obj(Py.obj(o).getAttr(name)).call11(a, b, c, d, e, f, g, h, i, j, k);
    }

    public Object call12(Object o, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
        Object fm = method(o);
        return fm != null ? Py.obj(fm).call13(o, a, b, c, d, e, f, g, h, i, j, k, l) : Py.obj(Py.obj(o).getAttr(name)).call12(a, b, c, d, e, f, g, h, i, j, k, l);
    }

    public Object call13(Object o, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
        Object fm = method(o);
        return fm != null ? Py.obj(fm).call14(o, a, b, c, d, e, f, g, h, i, j, k, l, m) : Py.obj(Py.obj(o).getAttr(name)).call13(a, b, c, d, e, f, g, h, i, j, k, l, m);
    }

    /// `o.name(...)` with seven or more arguments. `args[0]` is a slot the
    /// call site leaves empty for the receiver, so calling the function
    /// with `o` prepended writes it there instead of copying the array.
    public Object callN(Object o, Object[] args) {
        Object f = method(o);
        if (f != null) {
            args[0] = o;
            return Py.obj(f).call(args, Py.NO_KW);
        }
        Object[] rest = new Object[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return Py.obj(Py.obj(o).getAttr(name)).call(rest, Py.NO_KW);
    }

    /// `o.name(..., key=value)`: [#callN] with keyword names. A method of the
    /// receiver's class is called with the receiver written into the empty
    /// first slot, so the call makes neither a bound method nor a second
    /// argument array.
    public Object callKw(Object o, Object[] args, String[] kwNames) {
        Object f = method(o);
        if (f != null) {
            args[0] = o;
            return Py.obj(f).call(args, kwNames);
        }
        Object[] rest = new Object[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return Py.obj(get(o)).call(rest, kwNames);
    }

    /// [#callKw] of a fixed arity: the receiver plus at most thirteen arguments,
    /// with no argument array at the call site.
    public Object callKwF(Object o, String[] kwNames, Object a) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a);
        }
        return Py.obj(get(o)).callKwF(kwNames, a);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e, f);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e, f);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e, f, g);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e, f, g);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e, f, g, h);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e, f, g, h);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e, f, g, h, i);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i, j);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e, f, g, h, i, j);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i, j, k);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e, f, g, h, i, j, k);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i, j, k, l);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e, f, g, h, i, j, k, l);
    }

    public Object callKwF(Object o, String[] kwNames, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
        Object fn = method(o);
        if (fn != null) {
            return Py.obj(fn).callKwF(kwNames, o, a, b, c, d, e, f, g, h, i, j, k, l, m);
        }
        return Py.obj(get(o)).callKwF(kwNames, a, b, c, d, e, f, g, h, i, j, k, l, m);
    }

    /// The function to call with the receiver prepended, when the call needs
    /// no bound method; otherwise null and the caller goes the generic way.
    private Object method(Object o) {
        if (o instanceof PyInstance) {
            PyInstance i = (PyInstance) o;
            if (getVersion == PyType.version) {
                PyType t = i.cls;
                Shape s = i.shape;
                if (gs0 == s && gt0 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return gf0;
                }
                if (gs1 == s && gt1 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return gf1;
                }
                if (gs2 == s && gt2 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return gf2;
                }
                if (gs3 == s && gt3 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return gf3;
                }
            }
            int k = findGet(i.cls, i.shape);
            if (k >= 0) {
                return getKinds[k] == CLASS_ATTR && getValues[k] instanceof PyFunction ? getValues[k] : null;
            }
            Object clsAttr = i.cls.lookup(name);
            if (i.cls.hasCustomGetattribute() || clsAttr instanceof PyProperty || readsThroughGet(clsAttr)
                    || i.fieldIndex(name) >= 0) {
                addGet(i.cls, i.shape, NONE, 0, null);
                return null;
            }
            int slot = i.shape().indexOf(name);
            if (slot >= 0) {
                addGet(i.cls, i.shape, SLOT, slot, null);
                return null;
            }
            if (clsAttr != null) {
                addGet(i.cls, i.shape, CLASS_ATTR, 0, clsAttr);
                return clsAttr instanceof PyFunction ? clsAttr : null;
            }
            addGet(i.cls, i.shape, NONE, 0, null);
            return null;
        }
        if (o instanceof PyType || o instanceof PyModule || o instanceof PySuper) {
            return null;
        }
        PyType t = Py.typeOf(o);
        int k = findGet(t, null);
        if (k >= 0) {
            return getKinds[k] == BUILTIN ? getValues[k] : null;
        }
        Object m = t.lookup(name);
        int kind = m instanceof PyBuiltin ? BUILTIN : NONE;
        addGet(t, null, kind, 0, m);
        return kind == BUILTIN ? m : null;
    }
}
