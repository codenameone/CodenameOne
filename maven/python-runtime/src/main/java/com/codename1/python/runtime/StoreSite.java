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
/// It remembers up to [AttrSite#WAYS] receiver layouts: the slot when the
/// instance already has the attribute, or the shape transition when the
/// store adds it. Everything it does not model -- a property, a descriptor
/// with `__set__`, a `__setattr__` written in Python -- goes the generic
/// way.
public final class StoreSite {
    private static final int WAYS = AttrSite.WAYS;

    private final String name;

    // Stores: a slot hit when the instance already has the attribute, or a
    // shape transition when the store adds it.
    private int setVersion = -1;
    private int setUsed;
    private int setNext;
    private PyType[] setTypes;
    private Shape[] setShapes;
    private int[] setSlots;
    private Shape[] setTos;

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
        if (setTypes == null) {
            setTypes = new PyType[WAYS];
            setShapes = new Shape[WAYS];
            setSlots = new int[WAYS];
            setTos = new Shape[WAYS];
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
