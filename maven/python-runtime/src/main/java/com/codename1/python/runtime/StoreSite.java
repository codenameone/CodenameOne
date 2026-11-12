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

/// An inline cache for one attribute store, `obj.name = v`, in generated
/// code: what [AttrSite] is for a read. A store has a site of its own, never
/// one it shares with a read, so neither carries the other's entries.
///
/// It remembers up to sixteen receiver layouts: the slot when the
/// instance already has the attribute, or the shape transition when the
/// store adds it. Everything it does not model -- a property, a descriptor
/// with `__set__`, a `__setattr__` written in Python -- goes the generic
/// way.
public final class StoreSite {
    /// How many layouts a site remembers. More than a read site does, because a store
    /// that adds an attribute takes two of them -- the transition, and the slot on the
    /// shape it leads to -- and the stores that add attributes are those of an
    /// `__init__`, which a base class shares with every subclass: at four, the
    /// constructor of a class with three subclasses forgot one of them at every call
    /// and stored by name instead. The first four are also held in fields, for the
    /// test [#set] makes inline; the rest are found by the scan in [#setSlow].
    private static final int WAYS = 16;
    private static final int INLINE = 4;

    private final String name;

    // Stores: a slot hit when the instance already has the attribute, or a
    // shape transition when the store adds it.
    private int setVersion = -1;
    private int setUsed;
    private PyType[] setTypes;
    private Shape[] setShapes;
    private int[] setSlots;
    private Shape[] setTos;

    /// Once the sixteen are taken: every further layout, by a hash of its
    /// class and shape, one entry a slot. A store in an `__init__` that a base
    /// class shares with hundreds of subclasses (python-pptx's element class per
    /// XML tag) evicted round robin and missed on every call, paying two class
    /// lookups by name and a generic store each time.
    private static final int HASHED = 256;
    private PyType[] hTypes;
    private Shape[] hShapes;
    private int[] hSlots;
    private Shape[] hTos;

    private static int hash(PyType t, Shape s) {
        return (t.id * 0x9E3779B1 + (s == null ? 0 : s.id) * 0x85EBCA6B) >>> 24;
    }

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

    /// A site for each name of `names`, one a line: the sites of a module,
    /// which generated code holds as one table.
    public static StoreSite[] table(String names) {
        String[] each = Py.names(names);
        StoreSite[] out = new StoreSite[each.length];
        for (int i = 0; i < each.length; i++) {
            out[i] = new StoreSite(each[i]);
        }
        return out;
    }

    public StoreSite(String name) {
        this.name = name;
    }

    /// A data descriptor written in Python: every store goes through its
    /// `__set__` (or `__delete__`), never straight into the instance.
    private static boolean storesThroughSet(Object clsAttr) {
        if (!(clsAttr instanceof PyInstance)) {
            return false;
        }
        PyType t = ((PyInstance) clsAttr).cls;
        return t.lookup("__set__") != null || t.lookup("__delete__") != null;
    }


    public void set(Object o, Object v) {
        if (o instanceof PyInstance) {
            PyInstance i = (PyInstance) o;
            if (setVersion == PyType.version) {
                PyType t = i.cls;
                Shape s = i.shape;
                if (ss0 == s && st0 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (sto0 == null) {
                        i.slots[si0] = v;
                    } else {
                        // A new attribute: the transition is cached too.
                        i.append(sto0, v);
                    }
                    return;
                }
                if (ss1 == s && st1 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (sto1 == null) {
                        i.slots[si1] = v;
                    } else {
                        // A new attribute: the transition is cached too.
                        i.append(sto1, v);
                    }
                    return;
                }
                if (ss2 == s && st2 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (sto2 == null) {
                        i.slots[si2] = v;
                    } else {
                        // A new attribute: the transition is cached too.
                        i.append(sto2, v);
                    }
                    return;
                }
                if (ss3 == s && st3 == t) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (sto3 == null) {
                        i.slots[si3] = v;
                    } else {
                        // A new attribute: the transition is cached too.
                        i.append(sto3, v);
                    }
                    return;
                }
            }
            if (fieldSetType == i.cls && fieldSetVersion == PyType.version //NOPMD CompareObjectsWithEquals - identity is meant
                    && (!i.extra || i.getField(fieldSetIndex) != null)) {
                i.setField(fieldSetIndex, v);
                return;
            }
            setSlow(i, v);
            return;
        }
        Py.obj(o).setAttr(name, v);
    }

    /// The one class whose compiled field this site last stored, as
    /// `PyInstance.store` would store it: no property, descriptor or
    /// `__setattr__` in the way, and the field not displaced into the shape.
    private PyType fieldSetType;
    private int fieldSetIndex;
    private int fieldSetVersion = -1;

    private void setSlow(PyInstance i, Object v) {
        if (setVersion == PyType.version) {
            PyType t = i.cls;
            Shape s = i.shape;
            if (hTypes != null) {
                int h = hash(t, s);
                if (hTypes[h] == t && hShapes[h] == s) { //NOPMD CompareObjectsWithEquals - identity is meant
                    Shape to = hTos[h];
                    if (to == null) {
                        i.slots[hSlots[h]] = v;
                    } else {
                        i.append(to, v);
                    }
                    return;
                }
            }
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
            st0 = st1 = st2 = st3 = null;
            hTypes = null;
        }
        fillSet(i, v);
    }

    private void addSet(PyType t, Shape shape, int slot, Shape to) {
        int k;
        if (setUsed < WAYS) {
            k = setUsed++;
        } else {
            if (hTypes == null) {
                hTypes = new PyType[HASHED];
                hShapes = new Shape[HASHED];
                hSlots = new int[HASHED];
                hTos = new Shape[HASHED];
            }
            int h = hash(t, shape);
            hTypes[h] = t;
            hShapes[h] = shape;
            hSlots[h] = slot;
            hTos[h] = to;
            return;
        }
        if (setTypes == null) {
            setTypes = new PyType[INLINE];
            setShapes = new Shape[INLINE];
            setSlots = new int[INLINE];
            setTos = new Shape[INLINE];
        } else if (k == setTypes.length) {
            // Nearly every site sees one class or two; only one that has filled the
            // small table pays for the large one.
            PyType[] types = new PyType[WAYS];
            Shape[] shapes = new Shape[WAYS];
            int[] slots = new int[WAYS];
            Shape[] tos = new Shape[WAYS];
            System.arraycopy(setTypes, 0, types, 0, k);
            System.arraycopy(setShapes, 0, shapes, 0, k);
            System.arraycopy(setSlots, 0, slots, 0, k);
            System.arraycopy(setTos, 0, tos, 0, k);
            setTypes = types;
            setShapes = shapes;
            setSlots = slots;
            setTos = tos;
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
        } else if (k == 3) {
            st3 = t; ss3 = shape; si3 = slot; sto3 = to;
        }
    }

    private void fillSet(PyInstance i, Object v) {
        PyType cls = i.cls;
        Object clsAttr = cls.lookup(name);
        Object sa = cls.lookup("__setattr__");
        int field = i.fieldIndex(name);
        boolean plain = !(clsAttr instanceof PyProperty) && !storesThroughSet(clsAttr)
                && (sa == null || sa instanceof PyBuiltin)
                && field < 0;
        Shape before = i.shape;
        int version = PyType.version;
        i.setAttr(name, v);
        if (field >= 0 && !(clsAttr instanceof PyProperty) && !storesThroughSet(clsAttr)
                && (sa == null || sa instanceof PyBuiltin) && version == PyType.version) {
            fieldSetType = cls;
            fieldSetIndex = field;
            fieldSetVersion = version;
            return;
        }
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
}
