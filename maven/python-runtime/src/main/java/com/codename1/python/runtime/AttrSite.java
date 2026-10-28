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

    private static final int NONE = 0;
    private static final int SLOT = 1;
    private static final int CLASS_ATTR = 2;
    private static final int BUILTIN = 3;

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

    // Reads and calls. An entry with a null shape is a non-instance receiver
    // type (BUILTIN or NONE). All entries are valid only while getVersion is
    // the current class version.
    private int getVersion = -1;
    private int getUsed;
    private int getNext;
    private final PyType[] getTypes = new PyType[WAYS];
    private final Shape[] getShapes = new Shape[WAYS];
    private final int[] getKinds = new int[WAYS];
    private final int[] getSlots = new int[WAYS];
    private final Object[] getValues = new Object[WAYS];

    // Stores: a slot hit when the instance already has the attribute, or a
    // shape transition when the store adds it.
    private int setVersion = -1;
    private int setUsed;
    private int setNext;
    private final PyType[] setTypes = new PyType[WAYS];
    private final Shape[] setShapes = new Shape[WAYS];
    private final int[] setSlots = new int[WAYS];
    private final Shape[] setTos = new Shape[WAYS];

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
    private PyType st0;
    private PyType st1;
    private PyType st2;
    private PyType st3;
    private Shape ss0;
    private Shape ss1;
    private Shape ss2;
    private Shape ss3;
    private int si0;
    private int si1;
    private int si2;
    private int si3;
    private Shape sto0;
    private Shape sto1;
    private Shape sto2;
    private Shape sto3;

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
            default:
                return I_NONE;
        }
    }

    /// The read entry for this receiver layout, or -1.
    private int findGet(PyType t, Shape shape) {
        if (getVersion != PyType.version) {
            getVersion = PyType.version;
            getUsed = 0;
            getNext = 0;
            gt0 = gt1 = gt2 = gt3 = null;
            return -1;
        }
        for (int k = 0; k < getUsed; k++) {
            if (getTypes[k] == t && getShapes[k] == shape) { //NOPMD CompareObjectsWithEquals - identity is meant
                return k;
            }
        }
        return -1;
    }

    private int addGet(PyType t, Shape shape, int kind, int slot, Object value) {
        int k;
        if (getUsed < WAYS) {
            k = getUsed++;
        } else {
            k = getNext;
            getNext = (getNext + 1) % WAYS;
        }
        getTypes[k] = t;
        getShapes[k] = shape;
        getKinds[k] = kind;
        getSlots[k] = slot;
        getValues[k] = value;
        int fast = kind == SLOT ? SLOT : NONE;
        Object f = kind == CLASS_ATTR && value instanceof PyFunction ? value : null;
        if (k == 0) {
            gt0 = t; gs0 = shape; gk0 = fast; gi0 = slot; gf0 = f;
        } else if (k == 1) {
            gt1 = t; gs1 = shape; gk1 = fast; gi1 = slot; gf1 = f;
        } else if (k == 2) {
            gt2 = t; gs2 = shape; gk2 = fast; gi2 = slot; gf2 = f;
        } else {
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
                if (gs0 == s && gt0 == t && gk0 == SLOT) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return i.slots[gi0];
                }
                if (gs1 == s && gt1 == t && gk1 == SLOT) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return i.slots[gi1];
                }
                if (gs2 == s && gt2 == t && gk2 == SLOT) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return i.slots[gi2];
                }
                if (gs3 == s && gt3 == t && gk3 == SLOT) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return i.slots[gi3];
                }
            }
            return getSlow(i);
        }
        return Py.obj(o).getAttr(name);
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
        // A compiled field is read by the generated code itself; this site
        // only sees it unset, and must not remember that.
        if (!(clsAttr instanceof PyProperty) && !cls.hasCustomGetattribute() && i.fieldIndex(name) < 0) {
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

    // ---- stores --------------------------------------------------------------

    public void set(Object o, Object v) {
        if (o instanceof PyInstance) {
            PyInstance i = (PyInstance) o;
            if (setVersion == PyType.version) {
                PyType t = i.cls;
                Shape s = i.shape;
                if (ss0 == s && st0 == t && sto0 == null) { //NOPMD CompareObjectsWithEquals - identity is meant
                    i.slots[si0] = v;
                    return;
                }
                if (ss1 == s && st1 == t && sto1 == null) { //NOPMD CompareObjectsWithEquals - identity is meant
                    i.slots[si1] = v;
                    return;
                }
                if (ss2 == s && st2 == t && sto2 == null) { //NOPMD CompareObjectsWithEquals - identity is meant
                    i.slots[si2] = v;
                    return;
                }
                if (ss3 == s && st3 == t && sto3 == null) { //NOPMD CompareObjectsWithEquals - identity is meant
                    i.slots[si3] = v;
                    return;
                }
            }
            setSlow(i, v);
            return;
        }
        Py.obj(o).setAttr(name, v);
    }

    private void setSlow(PyInstance i, Object v) {
        if (setVersion == PyType.version) {
            PyType t = i.cls;
            Shape s = i.shape;
            for (int k = 0; k < setUsed; k++) {
                if (setTypes[k] == t && setShapes[k] == s) { //NOPMD CompareObjectsWithEquals - identity is meant
                    Shape to = setTos[k];
                    if (to == null) {
                        i.slots[setSlots[k]] = v;
                    } else {
                        i.append(to, v);
                    }
                    return;
                }
            }
        } else {
            setVersion = PyType.version;
            setUsed = 0;
            setNext = 0;
            st0 = st1 = st2 = st3 = null;
        }
        fillSet(i, v);
    }

    private void addSet(PyType t, Shape shape, int slot, Shape to) {
        int k;
        if (setUsed < WAYS) {
            k = setUsed++;
        } else {
            k = setNext;
            setNext = (setNext + 1) % WAYS;
        }
        setTypes[k] = t;
        setShapes[k] = shape;
        setSlots[k] = slot;
        setTos[k] = to;
        if (k == 0) {
            st0 = t; ss0 = shape; si0 = slot; sto0 = to;
        } else if (k == 1) {
            st1 = t; ss1 = shape; si1 = slot; sto1 = to;
        } else if (k == 2) {
            st2 = t; ss2 = shape; si2 = slot; sto2 = to;
        } else {
            st3 = t; ss3 = shape; si3 = slot; sto3 = to;
        }
    }

    private void fillSet(PyInstance i, Object v) {
        PyType cls = i.cls;
        Object clsAttr = cls.lookup(name);
        Object sa = cls.lookup("__setattr__");
        boolean plain = !(clsAttr instanceof PyProperty) && (sa == null || sa instanceof PyBuiltin)
                && i.fieldIndex(name) < 0;
        Shape before = i.shape;
        i.setAttr(name, v);
        if (!plain || setVersion != PyType.version) {
            return;
        }
        Shape after = i.shape;
        if (before == after) { //NOPMD CompareObjectsWithEquals - identity is meant
            addSet(cls, before, after == null ? -1 : after.indexOf(name), null);
        } else {
            // Adding the attribute moved the instance to a new shape. Remember
            // both the transition (the next new instance) and the slot on the
            // new shape (the next store to an instance already initialised).
            addSet(cls, before, 0, after);
            addSet(cls, after, after.indexOf(name), null);
        }
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

    public Object callN(Object o, Object[] args) {
        Object f = method(o);
        return f != null ? Py.obj(f).call(Py.prepend(o, args), Py.NO_KW) : Py.obj(Py.obj(o).getAttr(name)).call(args, Py.NO_KW);
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
            if (i.cls.hasCustomGetattribute() || clsAttr instanceof PyProperty || i.fieldIndex(name) >= 0) {
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
